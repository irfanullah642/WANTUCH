<?php
session_start();
require_once '../../includes/db.php';
require_once '../../includes/FeeManager.php';
require_once '../../includes/notification_helper.php';

if (!function_exists('applySurplusWithIds')) {
    $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
    $baseUrl = "$protocol://{$_SERVER['HTTP_HOST']}" . str_replace('api_mobile_dashboard.php', '', $_SERVER['SCRIPT_NAME']);
    $baseUrl = str_replace('modules/education/', '', $baseUrl);

    function applySurplusWithIds(float $surplus, int $student_id, string $start_month, int $start_year, int $inst_id, $conn, int $u_id, string $method, string $trans) : array {
        $affected_ids = [];
        if ($surplus <= 0) return ['remaining' => 0.0, 'ids' => []];
        $months_order = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
        $start_idx = array_search($start_month, $months_order);
        if ($start_idx === false) return ['remaining' => $surplus, 'ids' => []];
        $info_q = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $student_id LIMIT 1");
        $info = ($info_q && $info_q->num_rows > 0) ? $info_q->fetch_assoc() : ['class_id' => 0, 'section_id' => 0];
        $cid = (int)($info['class_id'] ?? 0);
        $scid = (int)($info['section_id'] ?? 0);
        $cur_y = $start_year;
        for ($i = 0; $i < 12; $i++) {
            $m_idx = ($start_idx + $i) % 12;
            if ($m_idx == 0 && $i > 0) $cur_y++;
            $m_name = $months_order[$m_idx];
            $res = $conn->query("SELECT id, amount, fee_type_id FROM edu_fee_management WHERE student_id = $student_id AND fee_month = '$m_name' AND fee_year = $cur_y AND institute_id = $inst_id AND Status = 'Unpaid' ORDER BY id ASC");
            while ($surplus > 0.001 && $row = $res->fetch_object()) {
                $row_amt = (float)$row->amount;
                if ($surplus >= $row_amt) {
                    $conn->query("UPDATE edu_fee_management SET Status = 'Paid', amount = $row_amt, payment_method = '$method', transaction_id = '$trans', remarks = 'Auto-paid from surplus', fee_collected_by = $u_id WHERE id = $row->id");
                    $affected_ids[] = (int)$row->id;
                    $surplus -= $row_amt;
                } else {
                    $rem = $row_amt - $surplus;
                    $conn->query("UPDATE edu_fee_management SET Status = 'Paid', amount = $surplus, payment_method = '$method', transaction_id = '$trans', remarks = 'Part-paid from surplus', fee_collected_by = $u_id WHERE id = $row->id");
                    $affected_ids[] = (int)$row->id;
                    $conn->query("INSERT INTO edu_fee_management (institute_id, student_id, class_id, section_id, fee_month, fee_year, fee_type_id, amount, Status, remarks) VALUES ($inst_id, $student_id, $cid, $scid, '$m_name', $cur_y, $row->fee_type_id, $rem, 'Unpaid', 'Remaining after surplus applied')");
                    $surplus = 0;
                }
            }
        }
        return ['remaining' => $surplus, 'ids' => $affected_ids];
    }
}

$action = $_REQUEST['action'] ?? '';

if (!isset($_SESSION['user_id']) && !isset($_SESSION['edu_user_id']) && 
    !in_array($action, ['GET_PORTFOLIO', 'GET_STAFF', 'GET_STAFF_PROFILE', 'GET_STUDENTS', 'SAVE_STUDENT', 'BULK_SAVE_STUDENTS', 
                       'GET_FEE_DASHBOARD', 'GET_FEE_INTELLIGENCE', 'GET_STRUCTURE', 'GET_STUDENT_FEE_LEDGER', 'GET_PAYMENT_METADATA'])) {
    echo json_encode(['status' => 'error', 'message' => 'Unauthorized']);
    exit;
}

if ($action === 'GET_PORTFOLIO') {
    $main_user_id = $_SESSION['user_id'] ?? 0;
    $role = $_SESSION['global_role'] ?? $_SESSION['edu_role'] ?? 'user';
    $ongoing_year = date('Y');

    $stats = ['schools' => 0, 'students' => 0, 'staff' => 0];
    
    // Portfolio Stats
    if ($role === 'developer' || $role === 'super_admin') {
        if ($role === 'developer') {
            $stats['schools'] = $conn->query("SELECT COUNT(*) FROM edu_institutions")->fetch_row()[0];
            $stats['students'] = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment WHERE academic_year = '$ongoing_year' AND status = 'active'")->fetch_row()[0];
            $stats['staff'] = $conn->query("SELECT COUNT(*) FROM edu_users WHERE role IN ('staff','teacher','admin','driver','super_admin')")->fetch_row()[0];
        } else {
            $stats['schools'] = $conn->query("SELECT COUNT(*) FROM edu_institutions WHERE owner_id = $main_user_id")->fetch_row()[0];
            $stats['students'] = $conn->query("SELECT COUNT(*) FROM edu_users u JOIN edu_student_enrollment e ON u.id = e.student_id JOIN edu_institutions i ON u.institution_id = i.id WHERE i.owner_id = $main_user_id AND e.academic_year = '$ongoing_year' AND e.status = 'active'")->fetch_row()[0];
            $stats['staff'] = $conn->query("SELECT COUNT(*) FROM edu_users WHERE role IN ('staff','teacher','admin','driver','super_admin') AND institution_id IN (SELECT id FROM edu_institutions WHERE owner_id=$main_user_id)")->fetch_row()[0];
        }
    }

    // Institutions List
    $institutions = [];
    if ($role === 'developer') {
        $q = $conn->query("SELECT id, name, type, logo_path FROM edu_institutions ORDER BY created_at DESC");
    } elseif ($role === 'super_admin') {
        $q = $conn->query("SELECT id, name, type, logo_path FROM edu_institutions WHERE owner_id = $main_user_id ORDER BY created_at DESC");
    } else {
        $q = $conn->prepare("SELECT i.id, i.name, i.type, i.logo_path FROM edu_institutions i JOIN edu_users u ON i.id = u.institution_id WHERE u.username = (SELECT username FROM users WHERE id = ?) ORDER BY i.created_at DESC");
        $q->bind_param("i", $main_user_id);
        $q->execute();
        $q = $q->get_result();
    }

    while ($row = $q->fetch_assoc()) {
        $institutions[] = [
            'id' => $row['id'],
            'name' => $row['name'],
            'type' => $row['type'],
            'logo' => $row['logo_path'] ? $row['logo_path'] : ''
        ];
    }

    echo json_encode([
        'status' => 'success',
        'stats' => $stats,
        'institutions' => $institutions
    ]);

} elseif ($action === 'SWITCH_AND_GET_DASHBOARD') {
    $inst_id = (int)($_GET['institution_id'] ?? 0);
    $main_user_id = $_SESSION['user_id'] ?? 0;
    
    // Switch context
    $stmt = $conn->prepare("SELECT id, name, type FROM edu_institutions WHERE id = ?");
    $stmt->bind_param("i", $inst_id);
    $stmt->execute();
    $res = $stmt->get_result();
    
        if ($res->num_rows > 0) {
            $inst = $res->fetch_assoc();
            $_SESSION['edu_institution_id'] = $inst['id'];
            $_SESSION['edu_institution_name'] = $inst['name'];
            
            // Resolve local identity in the new institution
            if ($main_user_id > 0) {
                $stmt_u = $conn->prepare("SELECT username FROM users WHERE id = ?");
                $stmt_u->bind_param("i", $main_user_id);
                $stmt_u->execute();
                $res_u = $stmt_u->get_result();
                if ($res_u->num_rows > 0) {
                    $username = $res_u->fetch_assoc()['username'];
                    $stmt_loc = $conn->prepare("SELECT id, role, full_name FROM edu_users WHERE username = ? AND institution_id = ? LIMIT 1");
                    $stmt_loc->bind_param("si", $username, $inst_id);
                    $stmt_loc->execute();
                    $res_loc = $stmt_loc->get_result();
                    if ($res_loc->num_rows > 0) {
                        $loc_row = $res_loc->fetch_assoc();
                        $_SESSION['edu_user_id'] = $loc_row['id'];
                        $_SESSION['edu_role'] = $loc_row['role'];
                        $_SESSION['edu_name'] = $loc_row['full_name'];
                    } else {
                        // Fallback: If no local record exists for a high-level admin, 
                        // reset local ID to avoid mismatch, but keep the global role.
                        $_SESSION['edu_user_id'] = 0; 
                        $_SESSION['edu_role'] = $_SESSION['global_role'] ?? 'super_admin';
                    }
                }
            }
            
            $role = $_SESSION['edu_role'] ?? 'user';
            $edu_user_id = $_SESSION['edu_user_id'] ?? 0;
            $ongoing_year = date('Y');
            $today = date('Y-m-d');

        // Stats
        $stats = [];
        $stats['staff'] = $conn->query("SELECT COUNT(*) FROM edu_users WHERE institution_id = $inst_id AND role NOT IN ('student','parent')")->fetch_row()[0];
        $stats['students'] = $conn->query("SELECT COUNT(*) FROM edu_users u JOIN edu_student_enrollment e ON u.id = e.student_id WHERE u.institution_id = $inst_id AND e.academic_year = '$ongoing_year' AND e.status = 'active'")->fetch_row()[0];
        $stats['fee_today'] = $conn->query("SELECT COALESCE(SUM(amount),0) FROM edu_fee_management WHERE institute_id=$inst_id AND Status = 'Paid' AND DATE(updated_at)='$today'")->fetch_row()[0];
        
        // Attendance Holiday Check
        $is_holiday = false;
        if (date('N') == 7) $is_holiday = true;
        // (Simplified holiday check for mobile speed)

        // Modules
        $modules = [
            ['id' => 'profile', 'label' => 'My Profile', 'icon' => 'person'],
            ['id' => 'inst_profile', 'label' => 'Inst. Profile', 'icon' => 'apartment'],
            ['id' => 'quick_scan', 'label' => 'Quick Scan', 'icon' => 'bolt'],
            ['id' => 'notices', 'label' => 'Notices', 'icon' => 'campaign'],
            ['id' => 'classes', 'label' => 'Classes', 'icon' => 'class'],
            ['id' => 'subjects', 'label' => 'Subjects', 'icon' => 'menu_book'],
            ['id' => 'exams', 'label' => 'Exams', 'icon' => 'assignment'],
            ['id' => 'timetable', 'label' => 'Timetable', 'icon' => 'schedule'],
            ['id' => 'syllabus', 'label' => 'Syllabus', 'icon' => 'book'],
            ['id' => 'homework', 'label' => 'Homework', 'icon' => 'tasks'],
            ['id' => 'attendance', 'label' => 'Attendance', 'icon' => 'how_to_reg'], // Added from current list
            ['id' => 'transport', 'label' => 'Transport', 'icon' => 'bus'],
            ['id' => 'smart_id', 'label' => 'Smart ID', 'icon' => 'badge'],
            ['id' => 'admission', 'label' => 'Adm / Wdl', 'icon' => 'person_add'],
            ['id' => 'promotion', 'label' => 'Promotion', 'icon' => 'school'],
            ['id' => 'fee', 'label' => 'Fee Management', 'icon' => 'payments'], // Added from current list
            ['id' => 'staff', 'label' => 'Staff', 'icon' => 'person_search'],
            ['id' => 'proxies', 'label' => 'Proxies', 'icon' => 'swap_horiz'],
            ['id' => 'study_plan', 'label' => 'Study Plan', 'icon' => 'event_available'],
            ['id' => 'reports', 'label' => 'Reports', 'icon' => 'analytics'],
            ['id' => 'database', 'label' => 'Database', 'icon' => 'database']
        ];

        echo json_encode([
            'status' => 'success',
            'institution_name' => $inst['name'],
            'full_name' => $_SESSION['edu_name'] ?? '',
            'user_id' => (int)($edu_user_id ?: $main_user_id),
            'role' => $role,
            'stats' => $stats,
            'is_holiday' => $is_holiday,
            'modules' => $modules
        ]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Institution not found']);
    }

} elseif ($action === 'GET_STAFF') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_REQUEST['inst_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    $today = date('Y-m-d');
    $stats = ['total' => 0, 'present' => 0, 'absent' => 0, 'leave' => 0];
    
    // Stats calculation (Fixed column names)
    $res_t = $conn->query("SELECT COUNT(*) FROM edu_users WHERE institution_id = $inst_id AND role NOT IN ('student','parent')");
    if ($res_t && $row_t = $res_t->fetch_row()) $stats['total'] = (int)$row_t[0];

    $res_p = $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE institution_id=$inst_id AND date='$today' AND status='P'");
    if ($res_p && $row_p = $res_p->fetch_row()) $stats['present'] = (int)$row_p[0];

    $res_a = $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE institution_id=$inst_id AND date='$today' AND status='A'");
    if ($res_a && $row_a = $res_a->fetch_row()) $stats['absent'] = (int)$row_a[0];

    $res_l = $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE institution_id=$inst_id AND date='$today' AND status='L'");
    if ($res_l && $row_l = $res_l->fetch_row()) $stats['leave'] = (int)$row_l[0];

    // Base URL
    $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
    $baseUrl = "$protocol://{$_SERVER['HTTP_HOST']}" . str_replace('modules/education/api_mobile_dashboard.php', '', $_SERVER['SCRIPT_NAME']);

    $teaching_staff = [];
    $non_teaching_staff = [];
    
    // FETCH (Removed status filter to ensure everyone shows up)
    $res = $conn->query("SELECT * FROM edu_users WHERE institution_id = $inst_id AND role NOT IN ('student','parent') ORDER BY full_name ASC");
    if ($res) {
        while ($row = $res->fetch_assoc()) {
            $staff_id = (int)$row['id'];
            $full_name = $row['full_name'] ?: 'UNKNOWN';
            
            $pic = $row['profile_pic'] ?? $row['photo'] ?? '';
            if ($pic && (strpos($pic, 'http') !== 0)) {
                $pic = $baseUrl . "assets/uploads/" . $pic;
            }

            $st_res = $conn->query("SELECT status FROM edu_staff_attendance WHERE staff_id = $staff_id AND date = '$today' LIMIT 1");
            $m_code = ($st_res && $s_row = $st_res->fetch_assoc()) ? $s_row['status'] : '';
            $m_label = 'Not Marked';
            switch($m_code) {
                case 'P': $m_label = 'Present'; break;
                case 'A': $m_label = 'Absent'; break;
                case 'L': $m_label = 'Leave'; break;
            }

            $member = [
                'id' => $staff_id,
                'name' => strtoupper($full_name),
                'initials' => strtoupper(substr($full_name, 0, 2)),
                'role' => $row['role'],
                'bps' => (string)($row['bps'] ?? ''),
                'marked' => $m_label,
                'paid' => 0.0,
                'balance' => (float)($row['salary'] ?? 0.0),
                'profile_pic' => $pic
            ];

            if (strtolower($row['user_type'] ?? '') === 'non-teaching') {
                $non_teaching_staff[] = $member;
            } else {
                $teaching_staff[] = $member;
            }
        }
    }
    
    header('Content-Type: application/json');
    echo json_encode(['status' => 'success', 'stats' => $stats, 'teaching_staff' => $teaching_staff, 'non_teaching_staff' => $non_teaching_staff]);
    exit;

} elseif ($action === 'GET_STAFF_PROFILE') {
    $staff_id = (int)($_REQUEST['staff_id'] ?? 0);
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    
    // Basic Info
    // 1. Try to fetch from local education users within the current institution
    $basic = null;
    if ($inst_id > 0) {
        $res = $conn->query("SELECT * FROM edu_users WHERE id = $staff_id AND institution_id = $inst_id");
        $basic = $res ? $res->fetch_assoc() : null;
    }
    
    // 2. Fallback to global users table if not found locally
    if (!$basic) {
        $res = $conn->query("SELECT *, user_type as role FROM users WHERE id = $staff_id");
        $basic = $res ? $res->fetch_assoc() : null;
        
        // If it's a global user, try to find a plain-text password from any linked edu account
        if ($basic && isset($basic['username'])) {
            $u_name = $conn->real_escape_string($basic['username']);
            $res_p = $conn->query("SELECT password FROM edu_users WHERE username = '$u_name' LIMIT 1");
            if ($res_p && $res_p->num_rows > 0) {
                $basic['password'] = $res_p->fetch_assoc()['password'];
            }
        }
    }
    
    if ($basic) {
        $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
        $baseUrl = "$protocol://{$_SERVER['HTTP_HOST']}" . str_replace('modules/education/api_mobile_dashboard.php', '', $_SERVER['SCRIPT_NAME']);
        if ($basic['profile_pic'] && strpos($basic['profile_pic'], 'http') !== 0) {
            $basic['profile_pic'] = $baseUrl . "assets/uploads/" . $basic['profile_pic'];
        }

        // We keep password/password_hash for editing as requested by user
        
        // Helper to fetch all rows safely
        $fetchAll = function($query) use ($conn) {
            $result = $conn->query($query);
            $data = [];
            if ($result) {
                while($row = $result->fetch_assoc()) {
                    $data[] = $row;
                }
            }
            return $data;
        };

        // Contacts
        $contacts = $fetchAll("SELECT * FROM edu_user_contacts WHERE user_id = $staff_id ORDER BY id DESC");
        
        // Education (Academics)
        $academics = $fetchAll("SELECT * FROM edu_user_academics WHERE user_id = $staff_id ORDER BY id DESC");
        
        // Experience
        $experience = $fetchAll("SELECT * FROM edu_user_experience WHERE user_id = $staff_id ORDER BY from_date DESC");
        
        // Bank
        $bank = $fetchAll("SELECT * FROM edu_user_bank WHERE user_id = $staff_id");
        
        // Institution Info (Institution Node)
        $inst_res = $conn->query("SELECT * FROM edu_institutions WHERE id = $inst_id");
        $inst_data = $inst_res ? $inst_res->fetch_assoc() : null;

        // Attendance & Salary Stats
        $month_start = date('Y-m-01');
        $attendance_data = [];
        $res_att = $conn->query("SELECT DAY(date) as day, status FROM edu_staff_attendance WHERE staff_id = $staff_id AND date >= '$month_start' ORDER BY date ASC");
        if ($res_att) {
            while($r = $res_att->fetch_assoc()) {
                $status_code = $r['status'];
                $status_label = $status_code;
                switch($status_code) {
                    case 'P': $status_label = 'Present'; break;
                    case 'A': $status_label = 'Absent'; break;
                    case 'L': $status_label = 'Leave'; break;
                    case 'S': $status_label = 'Short'; break;
                    case 'PH': $status_label = 'Public Holiday'; break;
                }
                $attendance_data[(int)$r['day']] = $status_label;
            }
        }
        
        // Financials
        $pay_table_check = $conn->query("SHOW TABLES LIKE 'edu_staff_payments_v2'");
        $pay_table = ($pay_table_check && $pay_table_check->num_rows > 0) ? 'edu_staff_payments_v2' : 'edu_staff_payments';
        $total_salary = (float)($basic['salary'] ?: 0.0);
        $paid_this_month = (float)($conn->query("SELECT SUM(amount) FROM $pay_table WHERE staff_id=$staff_id AND institution_id=$inst_id AND payment_date >= '$month_start'")->fetch_row()[0] ?: 0.0);
        
        $stats = [
            'calendar' => $attendance_data,
            'attendance' => $attendance_data,
            'month' => [
                'present' => $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id=$staff_id AND status='P' AND date >= '$month_start'")->fetch_row()[0] ?: 0,
                'absent'  => $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id=$staff_id AND status='A' AND date >= '$month_start'")->fetch_row()[0] ?: 0,
                'leave'   => $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id=$staff_id AND status='L' AND date >= '$month_start'")->fetch_row()[0] ?: 0
            ],
            'salary' => [
                'total' => $total_salary,
                'paid' => $paid_this_month,
                'balance' => max(0, $total_salary - $paid_this_month)
            ]
        ];
        
        // --- Institution Advanced Data ---
        $fetchAll = function($query) use ($conn) {
            $result = $conn->query($query);
            $data = [];
            if ($result) {
                while($row = $result->fetch_assoc()) {
                    $data[] = $row;
                }
            }
            return $data;
        };

        $inst_posts = $fetchAll("SELECT * FROM edu_institution_posts WHERE inst_id = $inst_id ORDER BY id DESC");
        $inst_bank = $fetchAll("SELECT * FROM edu_institution_bank WHERE inst_id = $inst_id ORDER BY id DESC");
        $inst_assets = $fetchAll("SELECT * FROM edu_institution_assets WHERE inst_id = $inst_id ORDER BY id DESC");
        $inst_funds = $fetchAll("SELECT * FROM edu_institution_funds WHERE inst_id = $inst_id ORDER BY id DESC");
        $inst_timetable = $fetchAll("
            SELECT t.*, c.name as class_name, s.name as section_name 
            FROM edu_timetable t
            LEFT JOIN edu_classes c ON t.class_id = c.id
            LEFT JOIN edu_sections s ON t.section_id = s.id
            WHERE t.institution_id = $inst_id 
            ORDER BY t.id DESC
        ");
        
        echo json_encode([
            'status' => 'success',
            'basic' => $basic,
            'stats' => $stats,
            'contacts' => $contacts,
            'academics' => $academics,
            'experience' => $experience,
            'bank' => $bank,
            'institution' => $inst_data,
            'inst_posts' => $inst_posts,
            'inst_bank' => $inst_bank,
            'inst_assets' => $inst_assets,
            'inst_funds' => $inst_funds,
            'inst_timetable' => $inst_timetable
        ]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'User not found']);
    }
    exit;

} elseif ($action === 'UPDATE_INSTITUTION_PROFILE') {
    $inst_id = (int)($_POST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    if (!$inst_id) { echo json_encode(['status' => 'error', 'message' => 'Invalid institution ID']); exit; }

    $allowed_fields = [
        'name', 'type', 'category', 'sector', 'edu_level', 'reg_no', 'address', 'contact_no', 'email',
        'whatsapp_enabled', 'wp_provider', 'wp_token', 'wp_instance_id'
    ];
    $sets = [];
    $params = [];
    $types = "";

    foreach ($allowed_fields as $key) {
        if (isset($_POST[$key])) {
            $sets[] = "$key = ?";
            $params[] = $_POST[$key];
            $types .= "s";
        }
    }

    if (!empty($sets)) {
        $sql = "UPDATE edu_institutions SET " . implode(", ", $sets) . " WHERE id = ?";
        $params[] = $inst_id;
        $types .= "i";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param($types, ...$params);
        if ($stmt->execute()) echo json_encode(['status' => 'success', 'message' => 'Institution updated']);
        else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'No fields to update']);
    }
    exit;

} elseif ($action === 'UPDATE_STAFF_PROFILE') {
    $staff_id = (int)($_POST['staff_id'] ?? 0);
    $inst_id = (int)($_POST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    
    if ($staff_id <= 0) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid staff ID']);
        exit;
    }

    $allowed_fields = [
        'username', 'password', 'role', 'user_type', 'full_name', 'father_name', 'gender', 'cnic', 
        'address', 'designation', 'bps', 'salary', 'status', 'dob', 'tribe', 'religion', 
        'disability', 'permenent_disease', 'facebook_link', 'tiktok_link', 'whatsapp_no', 
        'is_transport_active', 'transport_location', 'transport_charges', 'special_bonus', 'created_at'
    ];

    $sets = [];
    $params = [];
    $types = "";

    // Global Target Check
    $is_global_target = false;
    $check_local = $conn->query("SELECT id FROM edu_users WHERE id = $staff_id");
    if ($check_local->num_rows === 0) {
        $check_global = $conn->query("SELECT id FROM users WHERE id = $staff_id");
        if ($check_global->num_rows > 0) $is_global_target = true;
    }

    $table = $is_global_target ? "users" : "edu_users";
    
    // Check columns
    $target_cols = [];
    $res_cols = $conn->query("DESCRIBE $table");
    while($c = $res_cols->fetch_assoc()) $target_cols[] = $c['Field'];

    foreach ($allowed_fields as $key) {
        if (isset($_POST[$key])) {
            $val = $_POST[$key];
            $db_key = $key;
            
            // Special handling for passwords
            if ($key === 'password' && !empty($val)) {
                if ($is_global_target) {
                    $db_key = 'password_hash';
                    $val = password_hash($val, PASSWORD_DEFAULT);
                }
                // Also update local record if we're global, or vice versa
                $u_name = $conn->query("SELECT username FROM $table WHERE id = $staff_id")->fetch_assoc()['username'] ?? '';
                if ($u_name) {
                    $other_table = $is_global_target ? "edu_users" : "users";
                    $other_col = $is_global_target ? "password" : "password_hash";
                    $other_val = $is_global_target ? $_POST['password'] : password_hash($_POST['password'], PASSWORD_DEFAULT);
                    
                    // Simple sync query
                    $conn->query("UPDATE $other_table SET $other_col = '".$conn->real_escape_string($other_val)."' WHERE username = '".$conn->real_escape_string($u_name)."'");
                }
            }

            if ($is_global_target && $key === 'role') {
                $db_key = 'user_type';
            }
            
            if (in_array($db_key, $target_cols)) {
                $sets[] = "$db_key = ?";
                $params[] = $val;
                $types .= "s";

                if ($key === 'role' && !$is_global_target && in_array('user_type', $target_cols)) {
                    $sets[] = "user_type = ?";
                    $params[] = $val;
                    $types .= "s";
                }
            }
        }
    }

    if (!empty($sets)) {
        $sql = "UPDATE $table SET " . implode(", ", $sets) . " WHERE id = ?";
        $params[] = $staff_id;
        $types .= "i";
        
        $stmt = $conn->prepare($sql);
        if ($stmt) {
            $stmt->bind_param($types, ...$params);
            if ($stmt->execute()) {
                echo json_encode(['status' => 'success', 'message' => 'Profile updated successfully']);
            } else {
                echo json_encode(['status' => 'error', 'message' => $stmt->error]);
            }
        } else {
            echo json_encode(['status' => 'error', 'message' => $conn->error]);
        }
        exit;
    }

} elseif ($action === 'SAVE_CONTACT') {
    $staff_id = (int)$_POST['staff_id'];
    $id = (int)($_POST['id'] ?? 0);
    $type = $_POST['type'] ?? '';
    $val = $_POST['value'] ?? '';
    $det = $_POST['detail'] ?? '';

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_user_contacts SET type=?, value=?, detail=? WHERE id=? AND user_id=?");
        $stmt->bind_param("sssii", $type, $val, $det, $id, $staff_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_user_contacts (user_id, type, value, detail) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("isss", $staff_id, $type, $val, $det);
    }
    
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_CONTACT') {
    $id = (int)$_POST['id'];
    $staff_id = (int)$_POST['staff_id'];
    $conn->query("DELETE FROM edu_user_contacts WHERE id = $id AND user_id = $staff_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'SAVE_ACADEMIC') {
    $staff_id = (int)$_POST['staff_id'];
    $id = (int)($_POST['id'] ?? 0);
    $degree = $_POST['degree_title'] ?? '';
    // $inst = $_POST['institute_name'] ?? ''; // Remove as not in table
    $subject = $_POST['core_subject'] ?? '';
    $det = $_POST['detail'] ?? '';

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_user_academics SET degree_title=?, core_subject=?, detail=? WHERE id=? AND user_id=?");
        $stmt->bind_param("sssii", $degree, $subject, $det, $id, $staff_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_user_academics (user_id, degree_title, core_subject, detail) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("isss", $staff_id, $degree, $subject, $det);
    }
    
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_ACADEMIC') {
    $id = (int)$_POST['id'];
    $staff_id = (int)$_POST['staff_id'];
    $conn->query("DELETE FROM edu_user_academics WHERE id = $id AND user_id = $staff_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'SAVE_EXPERIENCE') {
    $staff_id = (int)$_POST['staff_id'];
    $id = (int)($_POST['id'] ?? 0);
    $title = $_POST['field_title'] ?? '';
    $from = $_POST['from_date'] ?? '';
    $to = $_POST['to_date'] ?? 'Present';
    $det = $_POST['detail'] ?? '';

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_user_experience SET field_title=?, from_date=?, to_date=?, detail=? WHERE id=? AND user_id=?");
        $stmt->bind_param("ssssii", $title, $from, $to, $det, $id, $staff_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_user_experience (user_id, field_title, from_date, to_date, detail) VALUES (?, ?, ?, ?, ?)");
        $stmt->bind_param("issss", $staff_id, $title, $from, $to, $det);
    }
    
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_EXPERIENCE') {
    $id = (int)$_POST['id'];
    $staff_id = (int)$_POST['staff_id'];
    $conn->query("DELETE FROM edu_user_experience WHERE id = $id AND user_id = $staff_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'SAVE_BANK') {
    $staff_id = (int)$_POST['staff_id'];
    $id = (int)($_POST['id'] ?? 0);
    $bank = $_POST['bank_name'] ?? '';
    $acc = $_POST['account_no'] ?? '';
    $branch = $_POST['branch_name'] ?? '';
    $title = $_POST['account_title'] ?? '';

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_user_bank SET bank_name=?, account_no=?, branch_name=?, account_title=? WHERE id=? AND user_id=?");
        $stmt->bind_param("ssssii", $bank, $acc, $branch, $title, $id, $staff_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_user_bank (user_id, bank_name, account_no, branch_name, account_title) VALUES (?, ?, ?, ?, ?)");
        $stmt->bind_param("issss", $staff_id, $bank, $acc, $branch, $title);
    }
    
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_BANK') {
    $id = (int)$_POST['id'];
    $staff_id = (int)$_POST['staff_id'];
    $conn->query("DELETE FROM edu_user_bank WHERE id = $id AND user_id = $staff_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'SAVE_INST_POST') {
    $inst_id = (int)$_POST['institution_id'];
    $id = (int)($_POST['id'] ?? 0);
    $desig = $_POST['designation'] ?? '';
    $bps = $_POST['bps'] ?? '';
    $total = (int)($_POST['total_posts'] ?? 0);
    $filled = (int)($_POST['filled_posts'] ?? 0);
    $vacant = $total - $filled;

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_institution_posts SET designation=?, bps=?, total_posts=?, filled_posts=?, vacant_posts=? WHERE id=? AND inst_id=?");
        $stmt->bind_param("ssiiiii", $desig, $bps, $total, $filled, $vacant, $id, $inst_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_institution_posts (inst_id, designation, bps, total_posts, filled_posts, vacant_posts) VALUES (?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("issiii", $inst_id, $desig, $bps, $total, $filled, $vacant);
    }
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_INST_POST') {
    $id = (int)$_POST['id'];
    $inst_id = (int)$_POST['institution_id'];
    $conn->query("DELETE FROM edu_institution_posts WHERE id = $id AND inst_id = $inst_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'SAVE_INST_BANK') {
    $inst_id = (int)$_POST['institution_id'];
    $id = (int)($_POST['id'] ?? 0);
    $bank = $_POST['bank_name'] ?? '';
    $br_name = $_POST['branch_name'] ?? '';
    $br_code = $_POST['branch_code'] ?? '';
    $acc = $_POST['account_no'] ?? '';
    $det = $_POST['detail'] ?? '';

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_institution_bank SET bank_name=?, branch_name=?, branch_code=?, account_no=?, detail=? WHERE id=? AND inst_id=?");
        $stmt->bind_param("sssssii", $bank, $br_name, $br_code, $acc, $det, $id, $inst_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_institution_bank (inst_id, bank_name, branch_name, branch_code, account_no, detail) VALUES (?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("isssss", $inst_id, $bank, $br_name, $br_code, $acc, $det);
    }
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_INST_BANK') {
    $id = (int)$_POST['id'];
    $inst_id = (int)$_POST['institution_id'];
    $conn->query("DELETE FROM edu_institution_bank WHERE id = $id AND inst_id = $inst_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'SAVE_INST_ASSET') {
    $inst_id = (int)$_POST['institution_id'];
    $id = (int)($_POST['id'] ?? 0);
    $name = $_POST['asset_name'] ?? '';
    $type = $_POST['asset_type'] ?? '';
    $fqty = (int)($_POST['functional_qty'] ?? 0);
    $nqty = (int)($_POST['non_functional_qty'] ?? 0);

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_institution_assets SET asset_name=?, asset_type=?, functional_qty=?, non_functional_qty=? WHERE id=? AND inst_id=?");
        $stmt->bind_param("ssiiii", $name, $type, $fqty, $nqty, $id, $inst_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_institution_assets (inst_id, asset_name, asset_type, functional_qty, non_functional_qty) VALUES (?, ?, ?, ?, ?)");
        $stmt->bind_param("issii", $inst_id, $name, $type, $fqty, $nqty);
    }
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_INST_ASSET') {
    $id = (int)$_POST['id'];
    $inst_id = (int)$_POST['institution_id'];
    $conn->query("DELETE FROM edu_institution_assets WHERE id = $id AND inst_id = $inst_id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'GET_STUDENTS') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    $class_id = (int)($_REQUEST['class_id'] ?? 0);
    $section_id = (int)($_REQUEST['section_id'] ?? 0);
    $status = $_REQUEST['status'] ?? 'active';
    
    // Auto-detect latest available year if not provided
    $year = $_REQUEST['year'] ?? '';
    if (!$year) {
        $year_q = $conn->query("SELECT e.academic_year FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE u.institution_id = $inst_id ORDER BY e.academic_year DESC LIMIT 1");
        $year = ($year_q && $y_row = $year_q->fetch_assoc()) ? $y_row['academic_year'] : date('Y');
    }
    
    if (!$inst_id) {
        echo json_encode(['status' => 'error', 'message' => 'No institution selected']);
        exit;
    }
    
    $today = date('Y-m-d');
    
    // Shared Where Clause
    $where = "u.institution_id = $inst_id AND u.role = 'student'";
    if($status) $where .= " AND e.status = '$status'";
    if($class_id > 0) $where .= " AND e.class_id = $class_id";
    if($section_id > 0) $where .= " AND e.section_id = $section_id";
    
    // Year filter logic: 
    // 1. Try with the detected/provided year
    // 2. If no students found, drop the year filter (fallback)
    $year_filter = "";
    if (!empty($year)) {
        $check_q = "SELECT 1 FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id 
                    WHERE $where AND e.academic_year = '$year' LIMIT 1";
        if ($conn->query($check_q)->num_rows > 0) {
            $year_filter = " AND e.academic_year = '$year'";
        }
    }
    $final_where = $where . $year_filter;

    // Stats
    $stats = ['total' => 0, 'present' => 0, 'absent' => 0, 'leave' => 0];
    $stats_q = $conn->query("
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN (SELECT status FROM edu_attendance WHERE student_id = u.id AND date = '$today' LIMIT 1) = 'Present' THEN 1 ELSE 0 END) as present,
            SUM(CASE WHEN (SELECT status FROM edu_attendance WHERE student_id = u.id AND date = '$today' LIMIT 1) = 'Absent' THEN 1 ELSE 0 END) as absent,
            SUM(CASE WHEN (SELECT status FROM edu_attendance WHERE student_id = u.id AND date = '$today' LIMIT 1) = 'Leave' THEN 1 ELSE 0 END) as `leave`
        FROM edu_users u
        JOIN edu_student_enrollment e ON u.id = e.student_id
        WHERE $final_where
    ");
    if($sq = $stats_q->fetch_assoc()) $stats = array_map('intval', $sq);

    // Students List
    $students = [];
    $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
    $host = $_SERVER['HTTP_HOST'];
    $baseUrl = "$protocol://$host" . str_replace('modules/education/api_mobile_dashboard.php', '', $_SERVER['SCRIPT_NAME']);

    $q = $conn->query("
        SELECT u.id, u.full_name, u.username, u.gender, u.father_name, u.profile_pic, e.class_no, c.name as class_name, s.name as section_name,
        (SELECT status FROM edu_attendance WHERE student_id = u.id AND date = '$today' LIMIT 1) as today_status
        FROM edu_users u
        JOIN edu_student_enrollment e ON u.id = e.student_id
        LEFT JOIN edu_classes c ON e.class_id = c.id
        LEFT JOIN edu_sections s ON e.section_id = s.id
        WHERE $final_where
        ORDER BY CAST(e.class_no AS UNSIGNED) ASC, u.full_name ASC
    ");
    
    while ($row = $q->fetch_assoc()) {
        $pic = $row['profile_pic'];
        if ($pic && (strpos($pic, 'http') !== 0)) $pic = $baseUrl . "assets/uploads/" . $pic;
        
        $students[] = [
            'id' => (int)$row['id'],
            'name' => strtoupper($row['full_name'] ?: 'UNKNOWN'),
            'username' => $row['username'],
            'initials' => strtoupper(substr($row['full_name'] ?: 'UN', 0, 2)),
            'class_no' => $row['class_no'] ?: '',
            'class_section' => $row['class_name'] . ' - ' . $row['section_name'],
            'gender' => $row['gender'] ?: '',
            'father_name' => $row['father_name'] ?: '',
            'marked' => $row['today_status'] ?: 'Not Marked',
            'profile_pic' => $pic
        ];
    }
    
    echo json_encode(['status' => 'success', 'stats' => $stats, 'students' => $students]);

} elseif ($action === 'GET_STUDENT_PROFILE') {
    $student_id = (int)$_GET['student_id'];
    $inst_id = (int)($_GET['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    
    // Detailed Profile
    $q = $conn->query("
        SELECT u.*, e.status as enrollment_status, e.class_no, e.class_id, e.section_id, e.academic_year, e.roll_number,
               c.name as class_name, s.name as section_name
        FROM edu_users u
        JOIN edu_student_enrollment e ON u.id = e.student_id
        LEFT JOIN edu_classes c ON e.class_id = c.id
        LEFT JOIN edu_sections s ON e.section_id = s.id
        WHERE u.id = $student_id AND u.institution_id = $inst_id
    ");
    $basic = $q ? $q->fetch_assoc() : null;
    
    if ($basic) {
        $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
        $baseUrl = "$protocol://{$_SERVER['HTTP_HOST']}" . str_replace('modules/education/api_mobile_dashboard.php', '', $_SERVER['SCRIPT_NAME']);
        if ($basic['profile_pic'] && strpos($basic['profile_pic'], 'http') !== 0) {
            $basic['profile_pic'] = $baseUrl . "assets/uploads/" . $basic['profile_pic'];
        }

        // Monthly Stats (Current Month)
        $month_start = date('Y-m-01');
        $m_stats_q = $conn->query("
            SELECT 
                SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as p,
                SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as a,
                SUM(CASE WHEN status = 'Leave' THEN 1 ELSE 0 END) as l,
                SUM(CASE WHEN status = 'Short' THEN 1 ELSE 0 END) as s,
                SUM(CASE WHEN status = 'Public Holiday' THEN 1 ELSE 0 END) as h
            FROM edu_attendance WHERE student_id = $student_id AND date >= '$month_start'
        ");
        $ms = $m_stats_q->fetch_assoc();

        // Calendar: per-day attendance for current month
        $calendar = [];
        $cal_q = $conn->query("
            SELECT DAY(date) as day, status FROM edu_attendance
            WHERE student_id = $student_id AND date >= '$month_start' AND date <= '$today'
            ORDER BY date ASC
        ");
        if ($cal_q) {
            while ($cal_row = $cal_q->fetch_assoc()) {
                $calendar[(int)$cal_row['day']] = $cal_row['status'];
            }
        }
        // Days in current month and first weekday (0=Sun)
        $days_in_month = (int)date('t');
        $first_weekday = (int)date('w', strtotime($month_start)); // 0=Sun, 6=Sat
        $month_year_label = date('F Y');

        // Academic Year Stats
        $year_start = date('Y-01-01');
        $y_stats_q = $conn->query("
            SELECT 
                SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as p,
                SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as a,
                SUM(CASE WHEN status = 'Leave' THEN 1 ELSE 0 END) as l,
                SUM(CASE WHEN status = 'Short' THEN 1 ELSE 0 END) as s,
                SUM(CASE WHEN status = 'Public Holiday' THEN 1 ELSE 0 END) as h,
                COUNT(*) as total
            FROM edu_attendance WHERE student_id = $student_id AND date >= '$year_start'
        ");
        $ys = $y_stats_q->fetch_assoc();

        // Other Stats (Today, Exam, Fee)
        $today = date('Y-m-d');
        $today_q = $conn->query("SELECT status FROM edu_attendance WHERE student_id = $student_id AND date = '$today'");
        $today_status = ($today_q->fetch_row()[0]) ?? 'Not Marked';

        $last_exam_q = $conn->query("
            SELECT m.obtain_marks, e.total_marks, s.name as sub_name, e.exam_type
            FROM edu_exam_marks m
            JOIN edu_exams e ON m.exam_id = e.id
            JOIN edu_subjects s ON e.subject_id = s.id
            WHERE m.student_id = $student_id
            ORDER BY e.exam_date DESC LIMIT 1
        ");
        $last_result = $last_exam_q->fetch_assoc();
        
        $avg_q = $conn->query("SELECT AVG((obtain_marks / total_marks) * 100) FROM edu_exam_marks m JOIN edu_exams e ON m.exam_id = e.id WHERE m.student_id = $student_id AND e.total_marks > 0");
        $avg_pct = round((float)($avg_q->fetch_row()[0] ?? 0), 1);

        $paid_q = $conn->query("SELECT SUM(amount_paid) FROM edu_fee_payments_log WHERE student_id = $student_id AND payment_month = '".date('F')."' AND payment_year = '".date('Y')."'");
        $paid_this_month = (float)($paid_q->fetch_row()[0] ?? 0);
        
        $tuition = (float)($basic['tuition_fee'] ?? 0);
        
        $balance = 0;
        if (file_exists('../../includes/FeeManager.php')) {
            require_once '../../includes/FeeManager.php';
            $fm = new FeeManager($conn, $inst_id);
            $balance = $fm->getStudentBalance($student_id);
        }

        echo json_encode([
            'status' => 'success',
            'basic' => $basic,
            'stats' => [
                'today' => $today_status,
                'paid_this_month' => $paid_this_month,
                'tuition_fee' => $tuition,
                'outstanding_balance' => $balance,
                'exam_avg' => $avg_pct,
                'last_exam' => $last_result ? $last_result['sub_name'] . ' (' . round(($last_result['obtain_marks']/$last_result['total_marks'])*100, 1) . '%)' : 'No Record',
                'month' => [
                    'present' => (int)($ms['p'] ?? 0),
                    'absent' => (int)($ms['a'] ?? 0),
                    'leave' => (int)($ms['l'] ?? 0),
                    'short' => (int)($ms['s'] ?? 0),
                    'holiday' => (int)($ms['h'] ?? 0)
                ],
                'calendar' => $calendar,
                'days_in_month' => $days_in_month,
                'first_weekday' => $first_weekday,
                'month_year' => $month_year_label,
                'year' => [
                    'present' => (int)($ys['p'] ?? 0),
                    'absent' => (int)($ys['a'] ?? 0),
                    'leave' => (int)($ys['l'] ?? 0),
                    'short' => (int)($ys['s'] ?? 0),
                    'holiday' => (int)($ys['h'] ?? 0),
                    'total' => (int)($ys['total'] ?? 0)
                ]
            ]
        ]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Student not found']);
    }

} elseif ($action === 'GET_STRUCTURE') {
    $inst_id = (int)($_GET['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    $classes = [];
    $res_c = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY id ASC");
    while ($c = $res_c->fetch_assoc()) {
        $sections = [];
        $res_s = $conn->query("SELECT id, name FROM edu_sections WHERE class_id = {$c['id']} ORDER BY name ASC");
        while ($s = $res_s->fetch_assoc()) $sections[] = $s;
        $c['sections'] = $sections;
        $classes[] = $c;
    }
    echo json_encode(['status' => 'success', 'classes' => $classes]);

} elseif ($action === 'SAVE_STUDENT') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    $id = (int)($_REQUEST['id'] ?? 0);
    $full_name = mysqli_real_escape_string($conn, $_REQUEST['full_name'] ?? '');
    $username = mysqli_real_escape_string($conn, $_REQUEST['username'] ?? '');
    $pass = $_REQUEST['password_plain'] ?? '';
    $father_name = mysqli_real_escape_string($conn, $_REQUEST['father_name'] ?? '');
    $gender = mysqli_real_escape_string($conn, $_REQUEST['gender'] ?? 'Male');
    $adm_no = mysqli_real_escape_string($conn, $_REQUEST['adm_no'] ?? '');
    $class_no = mysqli_real_escape_string($conn, $_REQUEST['class_no'] ?? '');
    $status = mysqli_real_escape_string($conn, $_REQUEST['status'] ?? 'active');
    $whatsapp = mysqli_real_escape_string($conn, $_REQUEST['whatsapp_no'] ?? '');
    $address = mysqli_real_escape_string($conn, $_REQUEST['address'] ?? '');

    if ($id > 0) {
        // Update Existing with more fields
        $stmt = $conn->prepare("UPDATE edu_users SET full_name=?, username=?, father_name=?, gender=?, whatsapp_no=?, address=? WHERE id=? AND institution_id=?");
        $stmt->bind_param("ssssssii", $full_name, $username, $father_name, $gender, $whatsapp, $address, $id, $inst_id);
        
        if($stmt->execute()) {
            if(!empty($pass)) {
                $hash = password_hash($pass, PASSWORD_BCRYPT);
                $conn->query("UPDATE edu_users SET password_plain='$pass', password_hash='$hash' WHERE id=$id");
                $conn->query("UPDATE users SET password='$pass', password_hash='$hash' WHERE username='$username'");
            }
            
            // Enrollment Updates
            $adm_date = $_REQUEST['date_of_admission'] ?? '';
            $status = mysqli_real_escape_string($conn, $_REQUEST['status'] ?? 'active');
            
            $stmt_e = $conn->prepare("UPDATE edu_student_enrollment SET adm_no=?, class_no=?, status=?, date_of_admission=? WHERE student_id=?");
            $stmt_e->bind_param("ssssi", $adm_no, $class_no, $status, $adm_date, $id);
            $stmt_e->execute();
            
            echo json_encode(['status' => 'success', 'message' => 'Student updated successfully']);
        } else {
            echo json_encode(['status' => 'error', 'message' => $stmt->error]);
        }
    } else {
        // Add New
        $hash = password_hash($pass, PASSWORD_BCRYPT);
        $conn->query("INSERT INTO edu_users (institution_id, full_name, username, password_plain, password_hash, role, father_name, gender, whatsapp_no, address) 
                     VALUES ($inst_id, '$full_name', '$username', '$pass', '$hash', 'student', '$father_name', '$gender', '$whatsapp', '$address')");
        $new_id = $conn->insert_id;
        if ($new_id) {
            $year = date('Y');
            $c_id = (int)($_POST['class_id'] ?? 0);
            $s_id = (int)($_POST['section_id'] ?? 0);
            $conn->query("INSERT INTO edu_student_enrollment (student_id, academic_year, adm_no, class_no, status, class_id, section_id) 
                         VALUES ($new_id, '$year', '$adm_no', '$class_no', '$status', $c_id, $s_id)");
            
            // Sync with global users
            $conn->query("INSERT INTO users (username, password, password_hash, role) VALUES ('$username', '$pass', '$hash', 'student')");
            
            echo json_encode(['status' => 'success', 'message' => 'Student added successfully', 'id' => $new_id]);
        } else {
            echo json_encode(['status' => 'error', 'message' => $conn->error]);
        }
    }

} elseif ($action === 'BULK_SAVE_STUDENTS') {
    $inst_id = (int)$_POST['institution_id'];
    $class_id = (int)$_POST['class_id'];
    $section_id = (int)$_POST['section_id'];
    $gender = $_POST['gender'] ?? 'Male';
    $names_text = $_POST['names_text'] ?? '';
    
    $lines = explode("\n", $names_text);
    $added = 0;
    $year = date('Y');
    
    foreach ($lines as $line) {
        $name = trim(preg_replace('/^\d+[\.\)\s]+/', '', $line));
        if (empty($name)) continue;
        
        $username = strtolower(str_replace(' ', '', $name)) . rand(100, 999);
        $pass = "123456";
        $hash = password_hash($pass, PASSWORD_BCRYPT);
        
        $q = "INSERT INTO edu_users (institution_id, full_name, username, password_plain, password_hash, role, gender) 
              VALUES ($inst_id, '$name', '$username', '$pass', '$hash', 'student', '$gender')";
        if ($conn->query($q)) {
            $sid = $conn->insert_id;
            $conn->query("INSERT INTO edu_student_enrollment (student_id, academic_year, status, class_id, section_id) 
                         VALUES ($sid, '$year', 'active', $class_id, $section_id)");
            $conn->query("INSERT INTO users (username, password, password_hash, role) VALUES ('$username', '$pass', '$hash', 'student')");
            $added++;
        }
    }
    echo json_encode(['status' => 'success', 'message' => "$added students enrolled successfully"]);

} elseif ($action === 'MARK_ATTENDANCE') {
    $student_id = (int)$_REQUEST['student_id'];
    $status = $_REQUEST['status'] ?? '';
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    // Use provided date if valid, otherwise default to today
    $raw_date = $_REQUEST['date'] ?? '';
    $date = ($raw_date && preg_match('/^\d{4}-\d{2}-\d{2}$/', $raw_date)) ? $raw_date : date('Y-m-d');

    if ($status === 'Delete') {
        $conn->query("DELETE FROM edu_attendance WHERE student_id = $student_id AND date = '$date'");
        echo json_encode(['status' => 'success', 'message' => 'Attendance removed']);
        exit;
    }

    $existing = $conn->query("SELECT id FROM edu_attendance WHERE student_id = $student_id AND date = '$date'")->fetch_assoc();
    if ($existing) {
        $stmt = $conn->prepare("UPDATE edu_attendance SET status = ? WHERE id = ?");
        $stmt->bind_param("si", $status, $existing['id']);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_attendance (institution_id, student_id, date, status) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("iiss", $inst_id, $student_id, $date, $status);
    }

    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_STUDENT') {
    $student_id = (int)$_POST['student_id'];
    $inst_id = (int)($_POST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    
    // To delete properly:
    // 1. Get username for global deletion
    $res = $conn->query("SELECT username FROM edu_users WHERE id = $student_id AND institution_id = $inst_id");
    if ($row = $res->fetch_assoc()) {
        $username = $row['username'];
        // 2. Delete from enrollment
        $conn->query("DELETE FROM edu_student_enrollment WHERE student_id = $student_id");
        // 3. Delete from edu_users
        $conn->query("DELETE FROM edu_users WHERE id = $student_id");
        // 4. Delete from global users (optional? process_add_student suggests they are synced)
        // Usually we keep global users if they might belong to other schools, but here we'll follow standard cleanup
        // $conn->query("DELETE FROM users WHERE username = '$username'"); 
    }
    echo json_encode(['status' => 'success']);

} elseif ($action === 'SAVE_INST_FUND') {
    $inst_id = (int)$_POST['institution_id'];
    $id = (int)($_POST['id'] ?? 0);
    $name = $_POST['name'] ?? '';
    $type = $_POST['type'] ?? '';
    $acc_no = $_POST['account_number'] ?? '';
    $amt = (float)($_POST['amount'] ?? 0);

    if ($id > 0) {
        $stmt = $conn->prepare("UPDATE edu_institution_funds SET name=?, type=?, account_number=?, amount=? WHERE id=? AND inst_id=?");
        $stmt->bind_param("sssdii", $name, $type, $acc_no, $amt, $id, $inst_id);
    } else {
        $stmt = $conn->prepare("INSERT INTO edu_institution_funds (inst_id, name, type, account_number, amount) VALUES (?, ?, ?, ?, ?)");
        $stmt->bind_param("isssd", $inst_id, $name, $type, $acc_no, $amt);
    }
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'COLLECT_FEE') {
    $inst_id = (int)($_POST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    $student_id = (int)$_POST['student_id'];
    $amount = (float)$_POST['amount'];
    $mode = $conn->real_escape_string($_POST['mode'] ?? 'Cash');
    $category = $conn->real_escape_string($_POST['category'] ?? 'Fee');
    $month = $conn->real_escape_string($_POST['month'] ?? date('F'));
    $year = $conn->real_escape_string($_POST['year'] ?? date('Y'));
    $date = date('Y-m-d');

    // Insert into fee collection table (assuming edu_fee_collection)
    $stmt = $conn->prepare("INSERT INTO edu_fee_collection (institution_id, student_id, amount, payment_mode, category, month, year, collection_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
    $stmt->bind_param("iidsssss", $inst_id, $student_id, $amount, $mode, $category, $month, $year, $date);
    
    if ($stmt->execute()) {
        // Also update the tuition balance if applicable
        $conn->query("UPDATE edu_student_enrollment SET tuition_fee_paid = tuition_fee_paid + $amount WHERE student_id = $student_id");
        echo json_encode(['status' => 'success', 'message' => "Fee of Rs. $amount collected successfully"]);
    } else {
        echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    }
    exit;

} elseif ($action === 'UPDATE_STUDENT_ROLE') {
    $student_id = (int)$_POST['student_id'];
    $role = $conn->real_escape_string($_POST['role'] ?? 'Regular Student');
    
    $stmt = $conn->prepare("UPDATE edu_student_enrollment SET student_role = ? WHERE student_id = ?");
    $stmt->bind_param("si", $role, $student_id);
    if ($stmt->execute()) echo json_encode(['status' => 'success', 'message' => "Student role updated to $role"]);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'CHANGE_STUDENT_STATUS') {
    $student_id = (int)$_POST['student_id'];
    $status = $conn->real_escape_string($_POST['status'] ?? 'active');
    
    $stmt = $conn->prepare("UPDATE edu_student_enrollment SET status = ? WHERE student_id = ?");
    $stmt->bind_param("si", $status, $student_id);
    if ($stmt->execute()) {
        $conn->query("UPDATE edu_users SET status = '$status' WHERE id = $student_id");
        echo json_encode(['status' => 'success', 'message' => "Student status changed to $status"]);
    } else {
        echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    }
    exit;

} elseif ($action === 'MARK_STAFF_ATTENDANCE') {
    $inst_id = (int)($_POST['institution_id'] ?? 0);
    $staff_id = (int)$_POST['staff_id'];
    $status = $conn->real_escape_string($_POST['status'] ?? 'P');
    $date = date('Y-m-d', strtotime($_POST['date'] ?: date('Y-m-d')));
    
    // Check if attendance exists
    $check = $conn->query("SELECT id FROM edu_staff_attendance WHERE staff_id = $staff_id AND date = '$date'");
    if ($check && $check->num_rows > 0) {
        $conn->query("UPDATE edu_staff_attendance SET status = '$status' WHERE staff_id = $staff_id AND date = '$date'");
    } else {
        $conn->query("INSERT INTO edu_staff_attendance (inst_id, staff_id, status, date) VALUES ($inst_id, $staff_id, '$status', '$date')");
    }
    echo json_encode(['status' => 'success', 'message' => "Attendance marked as $status"]);
    exit;

} elseif ($action === 'RECORD_SALARY') {
    $inst_id = (int)($_POST['institution_id'] ?? 0);
    $staff_id = (int)$_POST['staff_id'];
    $amount = (float)$_POST['amount'];
    $month = $conn->real_escape_string($_POST['month'] ?? date('F'));
    $year = $conn->real_escape_string($_POST['year'] ?? date('Y'));
    $mode = $conn->real_escape_string($_POST['mode'] ?? 'Cash');
    
    $stmt = $conn->prepare("INSERT INTO edu_staff_salaries (inst_id, staff_id, amount, month, year, payment_mode, payment_date) VALUES (?, ?, ?, ?, ?, ?, ?)");
    $date = date('Y-m-d');
    $stmt->bind_param("iidssss", $inst_id, $staff_id, $amount, $month, $year, $mode, $date);
    
    if ($stmt->execute()) echo json_encode(['status' => 'success', 'message' => "Salary of Rs. $amount recorded"]);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'UPDATE_STAFF_STATUS') {
    $staff_id = (int)$_POST['staff_id'];
    $status = $conn->real_escape_string($_POST['status'] ?? 'active');
    
    $stmt = $conn->prepare("UPDATE edu_users SET status = ? WHERE id = ?");
    $stmt->bind_param("si", $status, $staff_id);
    if ($stmt->execute()) echo json_encode(['status' => 'success', 'message' => "Staff status updated to $status"]);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'GET_FEE_DASHBOARD') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_REQUEST['inst_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    if (!$inst_id) { echo json_encode(['status' => 'error', 'message' => 'Missing institution Context', 'received_id' => $_REQUEST]); exit; }
    $today = date('Y-m-d');
    $month = date('F');
    $year = date('Y');

    // Stats
    $stats = [
        'cumulative_charges' => 0.0,
        'total_collected' => 0.0,
        'total_outstanding' => 0.0,
        'attendance_fine' => 0.0,
        'projected_month' => 0.0
    ];

    // Cumulative Charges (Total debits in fee_management for this inst)
    $res = $conn->query("SELECT SUM(amount) FROM edu_fee_management WHERE institute_id = $inst_id AND Status != 'Paid'");
    $stats['total_outstanding'] = (float)($res->fetch_row()[0] ?? 0);

    $res = $conn->query("SELECT SUM(amount) FROM edu_fee_management WHERE institute_id = $inst_id AND Status = 'Paid'");
    $stats['total_collected'] = (float)($res->fetch_row()[0] ?? 0);
    
    $stats['cumulative_charges'] = $stats['total_collected'] + $stats['total_outstanding'];

    // Attendance Fine (Special calc if needed, or just sum from fee_management where type is fine)
    $res = $conn->query("SELECT SUM(amount) FROM edu_fee_management WHERE institute_id = $inst_id AND fee_type_id IN (SELECT id FROM edu_fee_types WHERE type_name LIKE '%Fine%' AND institution_id = $inst_id)");
    $stats['attendance_fine'] = (float)($res->fetch_row()[0] ?? 0);

    // Projected (Month) - Expected fees for current month
    $res = $conn->query("SELECT SUM(tuition_fee + transport_charges) FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE u.institution_id = $inst_id AND e.status = 'active'");
    $stats['projected_month'] = (float)($res->fetch_row()[0] ?? 0);

    // Data for Tabs
    $classes = [];
    $res_c = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY sort_order ASC, name ASC");
    if ($res_c) {
        while ($c = $res_c->fetch_assoc()) $classes[] = $c;
    }

    $fee_types = [];
    $res_f = $conn->query("SELECT id, type_name FROM edu_fee_types WHERE institution_id = $inst_id AND status = 'Active' ORDER BY type_name ASC");
    while ($f = $res_f->fetch_assoc()) $fee_types[] = $f;

    // Approvals (Pending payments)
    $approvals = [];
    $res_a = $conn->query("SELECT f.*, u.full_name as student_name FROM edu_fee_management f JOIN edu_users u ON f.student_id = u.id WHERE f.institute_id = $inst_id AND f.payment_status = 'Pending' ORDER BY f.created_at DESC");
    while ($a = $res_a->fetch_assoc()) $approvals[] = $a;

    // Transport Pricing Setup
    $trans_locations = [];
    $res_t = $conn->query("SELECT * FROM edu_transport_charges WHERE institute_id = $inst_id AND status = 'Active'");
    while ($t = $res_t->fetch_assoc()) $trans_locations[] = $t;

    $sections = [];
    $res_s = $conn->query("SELECT s.id, s.name FROM edu_sections s JOIN edu_classes c ON s.class_id = c.id WHERE c.institution_id = $inst_id GROUP BY s.id ORDER BY s.id ASC");
    if ($res_s) {
        while ($s = $res_s->fetch_assoc()) $sections[] = $s;
    }

    // NEW: Automation & WhatsApp Settings from edu_institutions
    $res_inst = $conn->query("SELECT fee_automation_status, fee_automation_day, fee_automation_target, 
                                     whatsapp_enabled, whatsapp_api_provider, whatsapp_instance_id, whatsapp_api_key, 
                                     whatsapp_reminder_day_1, whatsapp_reminder_day_2, whatsapp_msg_reminder, whatsapp_msg_payment, whatsapp_msg_overdue 
                              FROM edu_institutions WHERE id = $inst_id");
    $inst_settings = ($res_inst && $res_inst->num_rows > 0) ? $res_inst->fetch_assoc() : (object)[];

    // NEW: Detailed Fee Structure (Class-wise amounts for all types)
    $fee_structure = [];
    $res_struct = $conn->query("SELECT * FROM edu_fee_structure WHERE institute_id = $inst_id");
    while ($rs = $res_struct->fetch_assoc()) $fee_structure[] = $rs;

    // NEW: Transport Types
    $trans_types = [];
    $res_tt = $conn->query("SELECT * FROM edu_transport_types WHERE institute_id = $inst_id AND status = 'Active'");
    if ($res_tt) {
        while ($tt = $res_tt->fetch_assoc()) $trans_types[] = $tt;
    }

    // NEW: Staff List for Transport
    $staff_list = [];
    $staff_q = $conn->query("
        SELECT id, full_name as name, role, profile_pic, transport_charges, transport_location_id
        FROM edu_users 
        WHERE institution_id = $inst_id AND role NOT IN ('student', 'parent') AND role != ''
        ORDER BY full_name ASC
    ");
    while ($s = $staff_q->fetch_assoc()) {
        $pic = $s['profile_pic'];
        if ($pic && (strpos($pic, 'http') !== 0)) $pic = $baseUrl . "assets/uploads/" . $pic;
        $staff_list[] = [
            'id' => (int)$s['id'],
            'name' => strtoupper($s['name']),
            'role' => strtoupper($s['role']),
            'profile_pic' => $pic,
            'transport_charges' => (float)$s['transport_charges'],
            'transport_location_id' => (int)$s['transport_location_id']
        ];
    }

    echo json_encode([
        'status' => 'success',
        'stats' => $stats,
        'classes' => $classes,
        'sections' => $sections,
        'fee_types' => $fee_types,
        'approvals' => $approvals,
        'trans_locations' => $trans_locations,
        'settings' => $inst_settings,
        'fee_structure' => $fee_structure,
        'trans_types' => $trans_types,
        'staff_list' => $staff_list,
        'months' => ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
        'years' => array_reverse(range(date('Y') - 5, date('Y') + 3))
    ]);

} elseif ($action === 'GET_FEE_INTELLIGENCE') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_REQUEST['inst_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    if (!$inst_id) { echo json_encode(['status' => 'error', 'message' => 'Missing institution Context']); exit; }
    $month = $_REQUEST['month'] ?? date('F');
    $year = (int)($_REQUEST['year'] ?? date('Y'));
    $class_id = (int)($_REQUEST['class_id'] ?? 0);
    $section_id = (int)($_REQUEST['section_id'] ?? 0);
    $search = $_REQUEST['search'] ?? '';

    $where = "u.institution_id = $inst_id AND u.role = 'student'";
    if ($class_id > 0) $where .= " AND e.class_id = $class_id";
    if ($section_id > 0) $where .= " AND e.section_id = $section_id";
    if (!empty($search)) {
        $search = $conn->real_escape_string($search);
        $where .= " AND (u.full_name LIKE '%$search%' OR u.username LIKE '%$search%')";
    }

    $q = $conn->query("
        SELECT u.id, u.full_name, u.username, u.father_name, u.profile_pic, e.class_no, c.name as class_name, s.name as section_name,
               e.tuition_fee, e.tuition_type, e.transport_charges, e.transport_location_id, 
               e.class_id, e.section_id,
               (SELECT SUM(amount) FROM edu_fee_management WHERE student_id = u.id AND fee_month = '$month' AND fee_year = $year AND Status = 'Unpaid') as payable,
               (SELECT SUM(amount) FROM edu_fee_management WHERE student_id = u.id AND fee_month = '$month' AND fee_year = $year AND Status = 'Paid') as paid
        FROM edu_users u
        JOIN edu_student_enrollment e ON u.id = e.student_id
        LEFT JOIN edu_classes c ON e.class_id = c.id
        LEFT JOIN edu_sections s ON e.section_id = s.id
        WHERE $where
        ORDER BY CAST(e.class_no AS UNSIGNED) ASC, u.full_name ASC
    ");

    $students = [];

    while ($row = $q->fetch_assoc()) {
        $pic = $row['profile_pic'];
        if ($pic && (strpos($pic, 'http') !== 0)) $pic = $baseUrl . "assets/uploads/" . $pic;
        
        $payable = (float)($row['payable'] ?: 0);
        $paid = (float)($row['paid'] ?: 0);
        $status = 'PENDING';
        if ($payable <= 0 && $paid > 0) $status = 'PAID';
        elseif ($payable > 0 && $paid > 0) $status = 'PARTIAL';

        $students[] = [
            'id' => (int)$row['id'],
            'name' => strtoupper($row['full_name'] ?: 'UNKNOWN'),
            'full_name' => strtoupper($row['full_name'] ?: 'UNKNOWN'),
            'username' => $row['username'],
            'father_name' => $row['father_name'] ?: '',
            'class_section' => $row['class_name'] . ' - ' . $row['section_name'],
            'class_id' => $row['class_id'],
            'section_id' => $row['section_id'],
            'class_name' => $row['class_name'],
            'section_name' => $row['section_name'],
            'payable' => $payable,
            'paid' => $paid,
            'status' => $status,
            'profile_pic' => $pic,
            'tuition_fee' => (float)$row['tuition_fee'],
            'tuition_type' => $row['tuition_type'],
            'transport_charges' => (float)$row['transport_charges'],
            'transport_location_id' => $row['transport_location_id'],
            'tuition_enabled' => ((float)$row['tuition_fee'] > 0 ? "1" : "0"),
            'transport_enabled' => ((float)$row['transport_charges'] > 0 ? "1" : "0"),
            'student_role' => 'Regular Student'
        ];
    }
    echo json_encode(['status' => 'success', 'students' => $students]);
    exit;

} elseif ($action === 'GET_STUDENT_FEE_LEDGER') {
    $student_id = (int)$_REQUEST['student_id'];
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    
    $month = $_REQUEST['month'] ?? date('F');
    $year = (int)($_REQUEST['year'] ?? date('Y'));
    
    $fm = new FeeManager($conn, $inst_id);
    $fm->syncMonthlyBills($student_id, $month, $year);

    $ledger = [];
    $res = $conn->query("SELECT f.*, t.type_name as fee_type FROM edu_fee_management f LEFT JOIN edu_fee_types t ON f.fee_type_id = t.id WHERE f.student_id = $student_id AND f.institute_id = $inst_id ORDER BY f.fee_year DESC, f.id DESC");
    while($r = $res->fetch_assoc()) {
        $ledger[] = [
            'id' => $r['id'],
            'fee_type' => $r['fee_type'] ?: 'Unknown',
            'amount' => (float)$r['amount'],
            'fee_month' => $r['fee_month'],
            'fee_year' => $r['fee_year'],
            'Status' => $r['Status']
        ];
    }
    
    $student_info = $conn->query("SELECT full_name, profile_pic FROM edu_users WHERE id = $student_id")->fetch_assoc();
    if ($student_info) {
        $pic = $student_info['profile_pic'] ?? '';
        $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
        $baseUrl = "$protocol://{$_SERVER['HTTP_HOST']}" . str_replace('api_mobile_dashboard.php', '', $_SERVER['SCRIPT_NAME']);
        $baseUrl = str_replace('modules/education/', '', $baseUrl);
        if ($pic && (strpos($pic, 'http') !== 0)) $student_info['profile_pic'] = $baseUrl . "assets/uploads/" . $pic;
    }

    $fee_types = [];
    $res_f = $conn->query("SELECT id, type_name FROM edu_fee_types WHERE institution_id = $inst_id AND status = 'Active' ORDER BY type_name ASC");
    if ($res_f) {
        while ($f = $res_f->fetch_assoc()) $fee_types[] = $f;
    }

    echo json_encode(['status' => 'success', 'ledger' => $ledger, 'student' => $student_info, 'fee_types' => $fee_types]);
    exit;

} elseif ($action === 'GET_PAYMENT_METADATA') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? 0);
    $methods = [];
    $res = $conn->query("SELECT name FROM edu_payment_methods WHERE institution_id = $inst_id AND status = 'Active'");
    while($r = $res->fetch_assoc()) $methods[] = $r['name'];
    $banks = [];
    $res = $conn->query("SELECT name FROM edu_bank_accounts WHERE institution_id = $inst_id AND status = 'Active'");
    while($r = $res->fetch_assoc()) $banks[] = $r['name'];
    echo json_encode(['status' => 'success', 'methods' => $methods, 'banks' => $banks]);
    exit;

} elseif ($action === 'GET_STRUCTURE') {
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    $classes = [];
    $res_c = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY id ASC");
    while ($c = $res_c->fetch_assoc()) {
        $sections = [];
        $class_id = $c['id'];
        $res_s = $conn->query("SELECT id, name FROM edu_sections WHERE class_id = $class_id ORDER BY name ASC");
        while ($s = $res_s->fetch_assoc()) $sections[] = $s;
        $c['sections'] = $sections;
        $classes[] = $c;
    }
    echo json_encode(['status' => 'success', 'classes' => $classes]);
    exit;

} elseif ($action === 'COLLECT_FEE') {
    $id = (int)$_POST['id'];
    $amt = (float)$_POST['amount']; // The amount the user is actually paying
    $method = $conn->real_escape_string($_POST['payment_method'] ?? 'Cash');
    $bank = $conn->real_escape_string($_POST['bank_account'] ?? '');
    $trans = $conn->real_escape_string($_POST['transaction_id'] ?? '');
    $inst_id = (int)($_POST['institution_id'] ?? 0);
    $u_id = (int)($_SESSION['edu_user_id'] ?? $_SESSION['user_id'] ?? 0);

    $fm = new FeeManager($conn, $inst_id);
    $chk = $conn->query("SELECT f.*, t.type_name FROM edu_fee_management f LEFT JOIN edu_fee_types t ON f.fee_type_id = t.id WHERE f.id = $id AND f.institute_id = $inst_id");
    if ($chk && $row = $chk->fetch_object()) {
        $student_id = $row->student_id;
        $rowAmount = (float)$row->amount;
        $rem = "Payment via $method";
        if ($bank || $trans) $rem .= " [$bank | ID: $trans]";
        $affected_ids = [$id];

        if ($amt >= $rowAmount) {
            // Full or Overpayment
            $conn->query("UPDATE edu_fee_management SET Status = 'Paid', payment_method = '$method', transaction_id = '$trans', remarks = '$rem', fee_collected_by = $u_id WHERE id = $id");
            $surplus = $amt - $rowAmount;
            if ($surplus > 0.001) {
                $res_surplus = applySurplusWithIds($surplus, $student_id, $row->fee_month, (int)$row->fee_year, $inst_id, $conn, $u_id, $method, $trans);
                $affected_ids = array_merge($affected_ids, $res_surplus['ids']);
            }
        } else {
            // Partial Payment
            $deficit = $rowAmount - $amt;
            $conn->query("UPDATE edu_fee_management SET Status = 'Paid', amount = $amt, payment_method = '$method', transaction_id = '$trans', remarks = '$rem (Part-payment)', fee_collected_by = $u_id WHERE id = $id");
            
            // Carry deficit to next month as Dues
            $months_list = ['January','February','March','April','May','June','July','August','September','October','November','December'];
            $cur_m_idx = array_search($row->fee_month, $months_list);
            $next_m_idx = ($cur_m_idx + 1) % 12;
            $next_month = $months_list[$next_m_idx];
            $next_year = ($cur_m_idx == 11) ? ((int)$row->fee_year + 1) : (int)$row->fee_year;
            
            $dues_tid = $fm->getFeeTypeId('Dues', true);
            $sid_class = (int)$row->class_id;
            $sid_sect = (int)$row->section_id;
            $conn->query("INSERT INTO edu_fee_management (institute_id, student_id, class_id, section_id, fee_month, fee_year, fee_type_id, amount, Status, remarks) VALUES ($inst_id, $student_id, $sid_class, $sid_sect, '$next_month', $next_year, $dues_tid, $deficit, 'Unpaid', 'Dues carried from part-payment $row->fee_month $row->fee_year')");
        }
        echo json_encode(['status' => 'success', 'paid_amount' => $amt, 'ids' => $affected_ids]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Record not found']);
    }
    exit;

} elseif ($action === 'COLLECT_BULK_FEE') {
    $student_id = (int)$_POST['student_id'];
    $amt = (float)$_POST['amount'];
    $method = $conn->real_escape_string($_POST['payment_method'] ?? 'Cash');
    $bank = $conn->real_escape_string($_POST['bank_account'] ?? '');
    $trans = $conn->real_escape_string($_POST['transaction_id'] ?? '');
    $inst_id = (int)($_POST['institution_id'] ?? 0);
    $u_id = (int)($_SESSION['edu_user_id'] ?? $_SESSION['user_id'] ?? 0);

    // Find the earliest unpaid month to start cascade
    $first_q = $conn->query("SELECT fee_month, fee_year FROM edu_fee_management WHERE student_id = $student_id AND institute_id = $inst_id AND Status = 'Unpaid' ORDER BY fee_year ASC, FIELD(fee_month, 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December') ASC LIMIT 1");
    if ($first_q && $row = $first_q->fetch_object()) {
        $start_month = $row->fee_month;
        $start_year = (int)$row->fee_year;
        $res_surplus = applySurplusWithIds($amt, $student_id, $start_month, $start_year, $inst_id, $conn, $u_id, $method, $trans);
        echo json_encode(['status' => 'success', 'paid_amount' => $amt, 'remaining' => $res_surplus['remaining'], 'ids' => $res_surplus['ids']]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'No unpaid records found']);
    }
    exit;

} elseif ($action === 'ADD_STUDENT_FEE') {
    $student_id = (int)$_POST['student_id'];
    $inst_id = (int)$_POST['institution_id'];
    $type_id = (int)$_POST['fee_type_id'];
    $amount = (float)$_POST['amount'];
    $month = $_POST['month'];
    $year = (int)$_POST['year'];
    
    $info = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $student_id")->fetch_assoc();
    $cid = $info['class_id'] ?? 0;
    $sid = $info['section_id'] ?? 0;

    $stmt = $conn->prepare("INSERT INTO edu_fee_management (institute_id, student_id, class_id, section_id, fee_month, fee_year, fee_type_id, amount, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Unpaid')");
    $stmt->bind_param("iiiiisid", $inst_id, $student_id, $cid, $sid, $month, $year, $type_id, $amount);
    
    if ($stmt->execute()) echo json_encode(['status' => 'success']);
    else echo json_encode(['status' => 'error', 'message' => $stmt->error]);
    exit;

} elseif ($action === 'DELETE_STUDENT_FEE') {
    $id = (int)$_POST['id'];
    $conn->query("DELETE FROM edu_fee_management WHERE id = $id");
    echo json_encode(['status' => 'success']);
    exit;

} elseif ($action === 'DELETE_INST_FUND') {
    $id = (int)$_POST['id'];
    $inst_id = (int)$_POST['institution_id'];
    $conn->query("DELETE FROM edu_institution_funds WHERE id = $id AND inst_id = $inst_id");
    echo json_encode(['status' => 'success']);
    exit;

} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid Action or No valid fields provided']);
}
?>
