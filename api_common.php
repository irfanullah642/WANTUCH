<?php
ob_start();
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Robust Dependencies Include
if (!function_exists('include_core_file')) {
    function include_core_file($path_from_root) {
        global $conn, $db, $servername, $username, $password, $dbname;
        $potential_paths = [
            __DIR__ . '/' . $path_from_root,
            __DIR__ . '/../../' . $path_from_root,
            'C:/xampp/htdocs/WANTUCH/' . $path_from_root
        ];
        foreach ($potential_paths as $p) {
            if (file_exists($p)) {
                require_once $p;
                return true;
            }
        }
        return false;
    }
}

// HARD INCLUDE FOR DB TO ENSURE GLOBAL SCOPE
$potential_db_paths = [
    __DIR__ . '/includes/db.php',
    __DIR__ . '/db.php',
    __DIR__ . '/../includes/db.php',
    __DIR__ . '/../../includes/db.php',
    'C:/xampp/htdocs/WANTUCH/includes/db.php',
    '/home/customer/www/wantuch.pk/public_html/includes/db.php'
];
$db_found = false;
foreach ($potential_db_paths as $p) {
    if (file_exists($p)) {
        require_once $p;
        $db_found = true;
        break;
    }
}
if (!$db_found) {
    die(json_encode(['status' => 'error', 'message' => 'Database configuration file not found.']));
}

include_core_file('includes/FeeManager.php');
include_core_file('includes/notification_helper.php');

// Define global context variables
global $action, $edu_user_id, $inst_id, $role_lower, $is_mgmt, $is_staff, $is_student;

$action = strtoupper($_REQUEST['action'] ?? '');
$get_uid = (int)($_REQUEST['user_id'] ?? $_REQUEST['student_id'] ?? 0);
$edu_user_id = ($get_uid > 0) ? $get_uid : ($_SESSION['edu_user_id'] ?? $_SESSION['user_id'] ?? 0);
$role = $_REQUEST['role'] ?? $_SESSION['edu_role'] ?? $_SESSION['user_type'] ?? '';
$inst_id = (int)($_REQUEST['institution_id'] ?: $_REQUEST['inst_id'] ?: $_SESSION['edu_institution_id'] ?: 0);
$role_lower = strtolower($role ?? '');
$user_param = mysqli_real_escape_string($conn, $_REQUEST['username'] ?? ($_REQUEST['user'] ?? ''));

// HIGH RELIABILITY IDENTITY RECOVERY
$username_current = (!empty($user_param)) ? $user_param : ($_SESSION['username'] ?? $_SESSION['user_username'] ?? $_SESSION['user'] ?? '');
if (!empty($username_current) || $edu_user_id > 0) {
    if (empty($role_lower) || $edu_user_id == 0) {
        $where_clause = $edu_user_id > 0 ? "id = $edu_user_id" : "username = '$username_current'";
        $uq = $conn->query("SELECT id, username, user_type FROM users WHERE $where_clause LIMIT 1");
        if ($uq && $row_u = $uq->fetch_assoc()) {
            if (empty($username_current)) $username_current = $row_u['username'];
            if ($edu_user_id == 0) $edu_user_id = (int)$row_u['id'];
            if (empty($role_lower)) $role_lower = strtolower($row_u['user_type']);
        }
    }
}

// GLOBAL CONTEXT RECOVERY: If $inst_id is missing or role is missing but we have a User ID, try to find their details
if ($edu_user_id > 0) {
    if (!$inst_id || empty($role_lower)) {
        $user_ctx_q = $conn->query("SELECT institution_id, role FROM edu_users WHERE id = $edu_user_id LIMIT 1");
        if ($user_ctx_q && $u_row = $user_ctx_q->fetch_assoc()) {
            if (!$inst_id && (int)$u_row['institution_id'] > 0) {
                $inst_id = (int)$u_row['institution_id'];
                $_SESSION['edu_institution_id'] = $inst_id;
            }
            if (empty($role_lower)) {
                $role = $u_row['role'] ?: ($u_row['user_type'] ?: '');
                $role_lower = strtolower($role);
                $_SESSION['edu_role'] = $role;
                $_SESSION['user_type'] = $role;
            }
        }
    }
    
    // Ownership fallback if still no institution context
    if ($inst_id === 0 && (strpos($role_lower, 'super') !== false || strpos($role_lower, 'admin') !== false)) {
        $ctx_q = $conn->query("SELECT id FROM edu_institutions WHERE owner_id = $edu_user_id LIMIT 1");
        if ($ctx_q && $ctx_row = $ctx_q->fetch_assoc()) {
            $inst_id = (int)$ctx_row['id'];
            $_SESSION['edu_institution_id'] = $inst_id;
        }
    }
}
$is_mgmt = in_array($role_lower, ['admin', 'super_admin', 'super admin', 'developer', 'management', 'principal', 'coordinator', 'vice principal', 'vice-principal', 'school_admin']);
$is_staff = ($is_mgmt || strpos($role_lower, 'staff') !== false || strpos($role_lower, 'teacher') !== false);
$is_student = (strpos($role_lower, 'student') !== false || strpos($role_lower, 'parent') !== false);
if ($is_mgmt) $is_staff = true;
// Special case for role match - avoid partial matches for super_admin
if ($is_mgmt && (strpos($role_lower, 'super') !== false || strpos($role_lower, 'admin') !== false)) {
    $is_student = false;
}

// Common JSON headers
if (!headers_sent()) {
    header('Content-Type: application/json');
    header('Access-Control-Allow-Origin: *');
}

// Shared utility functions
if (!function_exists('has_mobile_access')) {
    // ... existing has_mobile_access ...
    function has_mobile_access($eid, $role, $uid, $inst_id, $conn) {
        $role = strtolower($role);
        if (in_array($role, ['developer', 'super_admin'])) return true;
        
        $eid = mysqli_real_escape_string($conn, $eid);
        $role = mysqli_real_escape_string($conn, $role);
        
        if ($uid > 0) {
            $res = $conn->query("SELECT access_level FROM edu_element_access WHERE institution_id = $inst_id AND element_id = '$eid' AND target_user_id = $uid LIMIT 1");
            if ($res && $res->num_rows > 0) {
                $lvl = strtolower($res->fetch_assoc()['access_level']);
                return ($lvl !== 'deny' && in_array($lvl, ['allow', 'read_only', 'full', 'readonly']));
            }
        }
        
        $res = $conn->query("SELECT access_level FROM edu_element_access WHERE institution_id = $inst_id AND element_id = '$eid' AND target_role = '$role' AND target_user_id = 0 LIMIT 1");
        if ($res && $res->num_rows > 0) {
            $lvl = strtolower($res->fetch_assoc()['access_level']);
            return ($lvl !== 'deny' && in_array($lvl, ['allow', 'read_only', 'full', 'readonly']));
        }
        
        $res = $conn->query("SELECT access_level FROM edu_element_access WHERE institution_id = $inst_id AND element_id = '$eid' AND (target_role = 'all_users' OR target_role = 'all' OR target_role = '*') LIMIT 1");
        if ($res && $res->num_rows > 0) {
            $lvl = strtolower($res->fetch_assoc()['access_level']);
            return ($lvl !== 'deny' && in_array($lvl, ['allow', 'read_only', 'full', 'readonly']));
        }
        
        if (in_array($role, ['admin', 'management', 'school_admin'])) return true;
        
        if (in_array($role, ['staff', 'teacher'])) {
            return in_array($eid, ['dash_profile', 'dash_notices', 'dash_timetable', 'dash_syllabus', 'dash_homework', 'dash_attendance', 'dash_students', 'dash_exams', 'dash_smart_id', 'dash_classes', 'dash_subjects', 'dash_reports']);
        }
        if ($role == 'student') {
            return in_array($eid, ['dash_profile', 'dash_notices', 'dash_subjects', 'dash_exams', 'dash_timetable', 'dash_syllabus', 'dash_homework', 'dash_smart_id']);
        }
        return false;
    }
}

if (!function_exists('get_notices_response')) {
    function get_notices_response($inst_id, $role, $uid, $conn) {
        $today = date('Y-m-d');
        $filter = "";
        $is_student = (strpos($role, 'student') !== false);
        $is_parent = (strpos($role, 'parent') !== false);
        $is_staff = (strpos($role, 'staff') !== false || strpos($role, 'teacher') !== false);

        if ($is_student) {
            $filter = " AND (target_students = 1 OR target_students IS NULL OR target_students = 0)"; 
        } elseif ($is_parent) {
            $filter = " AND (target_parents = 1)";
        } elseif ($is_staff) {
            $filter = " AND (target_staff = 1)";
        }

        // Only show notices that haven't expired
        $res = $conn->query("SELECT * FROM edu_notices WHERE institution_id = $inst_id AND (expiry_date >= CURDATE() OR expiry_date = '0000-00-00' OR expiry_date IS NULL) $filter ORDER BY created_at DESC LIMIT 50");
        $notices = []; 
        while($row = $res->fetch_assoc()) {
            $notices[] = [
                'id' => (int)$row['id'],
                'title' => (string)$row['title'],
                'detail' => (string)$row['detail'],
                'expiry_date' => (string)$row['expiry_date'],
                'created_at' => (string)$row['created_at']
            ];
        }
        echo json_encode(['status' => 'success', 'notices' => $notices]);
        exit;
    }
}

if (!function_exists('get_structure_response')) {
    function get_structure_response($inst_id, $conn) {
        $classes = [];
        $res_c = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY id ASC");
        while ($c = $res_c->fetch_assoc()) {
            $sections = [];
            $res_s = $conn->query("SELECT id, name FROM edu_sections WHERE class_id = {$c['id']} ORDER BY name ASC");
            while ($s = $res_s->fetch_assoc()) $sections[] = $s;
            $c['sections'] = $sections; $classes[] = $c;
        }
        echo json_encode(['status' => 'success', 'classes' => $classes]);
        exit;
    }
}

if (!function_exists('get_students_list_response')) {
    function get_students_list_response($inst_id, $class_id, $section_id, $conn, $staff_id = 0) {
        $where = "u.institution_id = $inst_id AND u.role = 'student'";
        if ($class_id > 0) $where .= " AND e.class_id = $class_id";
        if ($section_id > 0) $where .= " AND e.section_id = $section_id";
        
        if ($staff_id > 0) {
            $staff_classes = [];
            $tt = $conn->query("SELECT DISTINCT class_id, section_id FROM edu_timetable WHERE staff_id = $staff_id AND institution_id = $inst_id");
            while ($t_row = $tt->fetch_assoc()) $staff_classes[] = "(e.class_id = {$t_row['class_id']} AND e.section_id = {$t_row['section_id']})";
            if (!empty($staff_classes)) $where .= " AND (" . implode(' OR ', $staff_classes) . ")";
            else $where .= " AND 1=0";
        }
        
        $res = $conn->query("SELECT u.id, u.full_name, e.class_no, c.name as class_name, s.name as section_name FROM edu_users u JOIN edu_student_enrollment e ON u.id = e.student_id LEFT JOIN edu_classes c ON e.class_id = c.id LEFT JOIN edu_sections s ON e.section_id = s.id WHERE $where AND e.status='active' ORDER BY u.full_name ASC");
        $students = []; if ($res) while($row = $res->fetch_assoc()) $students[] = $row;
        echo json_encode(['status' => 'success', 'students' => $students]);
        exit;
    }
}

if (!function_exists('send_dashboard_response')) {
    function send_dashboard_response($role, $edu_user_id, $inst_id, $conn) {
        $role_lower = strtolower($role);
        $today = date('Y-m-d');

        // Robust context for Super Admins
        if (!$inst_id && (in_array($role_lower, ['super_admin', 'super admin', 'developer']))) {
            $main_uid = $_SESSION['user_id'] ?? 0;
            $fallback = $conn->query("SELECT id FROM edu_institutions WHERE owner_id = $main_uid LIMIT 1")->fetch_assoc();
            if ($fallback) $inst_id = (int)$fallback['id'];
        }

        $inst_q = $conn->query("SELECT name, logo_path FROM edu_institutions WHERE id = $inst_id");
        $inst = ($inst_q && $inst_q->num_rows > 0) ? $inst_q->fetch_assoc() : ['name' => 'WANTUCH Academy', 'logo_path' => ''];

        $stats = ['staff' => 0, 'students' => 0, 'fee_today' => 0];
        $is_mgmt = in_array($role_lower, ['admin', 'super_admin', 'super admin', 'developer', 'management', 'school_admin']);
        
        if ($inst_id > 0) {
            $stats['staff'] = $conn->query("SELECT COUNT(*) FROM edu_users WHERE institution_id = $inst_id AND role NOT IN ('student','parent')")->fetch_row()[0];
            $stats['students'] = $conn->query("SELECT COUNT(*) FROM edu_users u JOIN edu_student_enrollment e ON u.id = e.student_id WHERE u.institution_id = $inst_id AND e.status='active'")->fetch_row()[0];
            $stats['fee_today'] = $conn->query("SELECT COALESCE(SUM(amount),0) FROM edu_fee_management WHERE institute_id=$inst_id AND Status = 'Paid' AND DATE(updated_at)='$today'")->fetch_row()[0];
        }

        $all_modules = [
            ['id' => 'profile', 'label' => 'My Profile', 'icon' => 'person', 'perm' => 'dash_profile'],
            ['id' => 'notices', 'label' => 'Notices', 'icon' => 'campaign', 'perm' => 'dash_notices'],
            ['id' => 'timetable', 'label' => 'Timetable', 'icon' => 'schedule', 'perm' => 'dash_timetable'],
            ['id' => 'syllabus', 'label' => 'Syllabus', 'icon' => 'book', 'perm' => 'dash_syllabus'],
            ['id' => 'homework', 'label' => 'Homework', 'icon' => 'tasks', 'perm' => 'dash_homework'],
            ['id' => 'admission', 'label' => 'Adm / Wdl', 'icon' => 'person_add', 'perm' => 'dash_admissions'],
            ['id' => 'staff', 'label' => 'Staff / Faculty', 'icon' => 'people', 'perm' => 'dash_staff'],
            ['id' => 'attendance', 'label' => 'Attendance', 'icon' => 'how_to_reg', 'perm' => 'dash_attendance']
        ];
        
        $modules = [];
        foreach($all_modules as $mod) if (has_mobile_access($mod['perm'], $role, $edu_user_id, $inst_id, $conn)) $modules[] = $mod;

        if (ob_get_length()) ob_clean();
        header('Content-Type: application/json');
        echo json_encode([
            'status' => 'success',
            'institution_name' => $inst['name'],
            'institution_logo' => $inst['logo_path'] ?? '',
            'full_name' => $_SESSION['user_fullname'] ?? $_SESSION['edu_name'] ?? 'Administrator',
            'user_id' => (int)$edu_user_id,
            'role' => $role,
            'stats' => $stats,
            'modules' => $modules,
            'permissions' => ['is_management' => $is_mgmt]
        ]);
        exit;
    }
}

if (!function_exists('get_staff_list_response')) {
    function get_staff_list_response($inst_id, $conn) {
        $stats = ['total' => 0, 'present' => 0, 'absent' => 0, 'leave' => 0];
        $today = date('Y-m-d');
        $r_role = strtolower($_REQUEST['role'] ?? '');
        $r_uid = (int)($_REQUEST['user_id'] ?: $_REQUEST['edu_user_id'] ?: $_SESSION['user_id'] ?: 0);

        // Hyper-perm inst_id recovery: Use provided uid to find their first school
        if (!$inst_id && $r_uid > 0) {
            $inst_id = (int)($_SESSION['edu_institution_id'] ?? 0);
            if (!$inst_id) {
                // If super admin, find first owned school, always works if they have one
                $check = $conn->query("SELECT id FROM edu_institutions WHERE owner_id = $r_uid LIMIT 1")->fetch_assoc();
                if ($check) $inst_id = (int)$check['id'];
                if (!$inst_id) {
                     $u_check = $conn->query("SELECT institution_id FROM edu_users WHERE id = $r_uid LIMIT 1")->fetch_assoc();
                     if ($u_check) $inst_id = (int)$u_check['institution_id'];
                }
            }
        }
        
        $teaching_staff = [];
        $non_teaching_staff = [];
        
        if (!$inst_id) {
            header('Content-Type: application/json');
            echo json_encode(['status' => 'success', 'stats' => $stats, 'teaching_staff' => [], 'non_teaching_staff' => [], 'message' => 'INSTITUTION_REQUIRED']);
            exit;
        }

        $res = $conn->query("SELECT * FROM edu_users WHERE institution_id = $inst_id AND role NOT IN ('student', 'parent') ORDER BY full_name ASC");
        if ($res) {
            while ($row = $res->fetch_assoc()) {
                $stats['total']++;
                $sid = (int)$row['id'];
                
                $marked = 'Not Marked';
                $att_q = $conn->query("SELECT status FROM edu_staff_attendance WHERE staff_id = $sid AND date = '$today' LIMIT 1");
                if ($att_q && $att_row = $att_q->fetch_assoc()) {
                    $st = strtoupper(trim($att_row['status']));
                    if (strpos($st, 'P') !== false) { $stats['present']++; $marked = 'Present'; }
                    elseif (strpos($st, 'A') !== false) { $stats['absent']++; $marked = 'Absent'; }
                    elseif (strpos($st, 'L') !== false || strpos($st, 'S') !== false) { $stats['leave']++; $marked = 'Leave'; }
                }
                
                $m_name = (string)($row['full_name'] ?: ($row['username'] ?: 'Staff MEMBER'));
                $m_parts = @explode(' ', $m_name);
                $m_initials = strtoupper(substr($m_name, 0, 1) . substr($m_parts[1]??'', 0, 1));

                $item = [
                    'id' => $sid,
                    'name' => $m_name,
                    'role' => (string)($row['role'] ?: 'Member'),
                    'initials' => $m_initials,
                    'profile_pic' => (string)($row['profile_pic'] ?: ''),
                    'marked' => $marked,
                    'bps' => (string)($row['bps'] ?: ''),
                    'paid' => (float)($row['paid_salary'] ?? 0),
                    'balance' => (float)($row['salary'] ?? 0)
                ];
                
                // Categorization: Put EVERYTHING in Teaching unless explicitly non-academic staff roles
                $l_role = strtolower($row['role'] ?? '');
                $l_type = strtolower($row['user_type'] ?? '');
                $is_explicit_nt = (
                    strpos($l_role, 'peon') !== false || 
                    strpos($l_role, 'guard') !== false || 
                    strpos($l_role, 'clerk') !== false || 
                    strpos($l_role, 'driver') !== false || 
                    strpos($l_role, 'cleaner') !== false ||
                    $l_type === 'non-teaching'
                );

                if ($is_explicit_nt && strpos($l_role, 'teacher') === false) {
                    $non_teaching_staff[] = $item;
                } else {
                    $teaching_staff[] = $item;
                }
            }
        }
        
        foreach($stats as $k => $v) $stats[$k] = (int)$v;
        header('Content-Type: application/json');
        echo json_encode(['status' => 'success', 'stats' => $stats, 'teaching_staff' => $teaching_staff, 'non_teaching_staff' => $non_teaching_staff, 'msg' => "Loaded data for inst $inst_id"]);
        exit;
    }
}
