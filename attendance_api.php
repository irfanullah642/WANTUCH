<?php
ob_start(); // 1. Start buffering immediately
error_reporting(E_ALL); // Report all errors internally
ini_set('display_errors', 0); // Do NOT display them to the user (prevents HTML leakage)
ini_set('log_errors', 1); // Log them instead

// 2. Shutdown Handler to catch Fatal Errors and return JSON
register_shutdown_function(function() {
    $error = error_get_last();
    // Catch fatal errors that stop script execution
    if ($error && in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR])) {
        if (ob_get_length()) ob_end_clean(); // WIPE any HTML leaked so far
        header('Content-Type: application/json');
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'error' => 'Fatal Server Error', 
            'details' => $error['message'],
            'file' => basename($error['file']),
            'line' => $error['line']
        ]);
        exit;
    }
});

require_once '../../includes/session_config.php';
require_once '../../includes/db.php';
require_once '../../includes/guest_access_helper.php';

// Block guest users from write operations
block_guest_write();

// 3. Helper to send clean JSON
function json_response($data) {
    if (ob_get_length()) ob_end_clean(); // Discard any prior buffer/warnings
    header('Content-Type: application/json');
    echo json_encode($data);
    exit;
}

// 4. Authentication Check (Session or System Token)
$is_authenticated = isset($_SESSION['edu_user_id']);
$system_token = $_SERVER['HTTP_X_ERP_AUTH'] ?? $_GET['sys_token'] ?? '';
$is_system_call = ($system_token === 'WANTUCH_PYTHON_SECURE_TOKEN_2024');

if (!$is_authenticated && !$is_system_call) {
    http_response_code(403);
    json_response(['error' => 'Unauthorized']);
}

// 5. Context Resolution
$inst_id = 0;
if (isset($_SESSION['edu_institution_id'])) {
    $inst_id = (int)$_SESSION['edu_institution_id'];
} elseif ($is_system_call) {
    // For system calls, we can pass the institution_id in the request or use a default
    // Fetching from request if provided, otherwise default to first institution
    $inputData = json_decode(file_get_contents('php://input'), true);
    $inst_id = (int)($inputData['institution_id'] ?? $_GET['inst_id'] ?? 0);
    
    if ($inst_id <= 0) {
        $first_inst = $conn->query("SELECT id FROM edu_institutions LIMIT 1");
        if ($first_inst && $first_inst->num_rows > 0) {
            $inst_id = (int)$first_inst->fetch_assoc()['id'];
        }
    }
}
$role = strtolower($_SESSION['edu_role'] ?? ($is_system_call ? 'admin' : ''));
$action = $_GET['action'] ?? '';
$allowed_roles = ['admin', 'developer', 'super_admin', 'useradmin']; // Centralized allowed roles

if ($inst_id <= 0) {
    http_response_code(400);
    json_response(['success' => false, 'error' => 'Invalid or missing Institution ID. Please re-login.']);
}

// Global Admin Check
$is_admin = in_array($role, ['admin', 'developer', 'super_admin', 'useradmin']);

// LIVE DEBUG LOG
if ($action !== 'get_reedit_requests' && $action !== 'get_submission_status') {
    error_log("Attendance API Debug: Action=$action, Role=$role, IsAdmin=".($is_admin?'Yes':'No').", UserID=".($_SESSION['edu_user_id']??0));
}

try {
// --- STAFF VALIDATION (Assignment Enforcement) ---
// --- STAFF VALIDATION (Assignment Enforcement) ---
if ($role === 'staff') {
    $staff_id = $_SESSION['edu_user_id'];
    
    // Actions that require class/section authorization
    $restricted_actions = ['get_students', 'get_summary', 'save_student_attendance_bulk', 'get_average_summary'];
    
    if (in_array($action, $restricted_actions)) {
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            $postData = json_decode(file_get_contents('php://input'), true);
            $req_cid = (int)($postData['class_id'] ?? 0);
            $req_sid = (int)($postData['section_id'] ?? 0);
        } else {
            $req_cid = (int)($_GET['class_id'] ?? 0);
            $req_sid = (int)($_GET['section_id'] ?? 0);
        }

        // 1. Check if Formmaster (Primary Class Teacher)
        $fm_check = $conn->query("SELECT 1 FROM edu_users WHERE id = $staff_id AND assigned_class_id = $req_cid AND assigned_section_id = $req_sid LIMIT 1");
        $is_formmaster = ($fm_check && $fm_check->num_rows > 0);

        // 2. Check Subject/Period Assignments
        $assign_check = $conn->query("SELECT 1 FROM edu_staff_assignments WHERE staff_id = $staff_id AND class_id = $req_cid AND section_id = $req_sid LIMIT 1");
        $is_assigned = ($assign_check && $assign_check->num_rows > 0);
        
        if (!$is_formmaster && !$is_assigned) {
            json_response(['success' => false, 'error' => 'Permission Denied: You are not assigned to this class/section.']);
        }
    }
}
// -------------------------------------------------

if ($action == 'get_students') {
    $cid = (int)$_GET['class_id'];
    $sid = (int)$_GET['section_id'];
    $date = $_GET['date'];

    // Defensive check for status column
    $check_col = $conn->query("SHOW COLUMNS FROM edu_student_enrollment LIKE 'status'");
    $insert_status_filter = ($check_col && $check_col->num_rows > 0) ? " AND status = 'active'" : "";
    $status_filter = ($check_col && $check_col->num_rows > 0) ? " AND e.status = 'active'" : "";

    $is_sunday = (date('w', strtotime($date)) == 0);
    
    // Auto-create table if missing (Defensive)
    $conn->query("CREATE TABLE IF NOT EXISTS `edu_public_holidays` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `institution_id` int(11) NOT NULL,
      `name` varchar(255) NOT NULL,
      `from_date` date NOT NULL,
      `to_date` date NOT NULL,
      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

    $is_ph = $conn->query("SELECT 1 FROM edu_public_holidays WHERE institution_id = $inst_id AND '$date' BETWEEN from_date AND to_date LIMIT 1")->num_rows > 0;
    $default_status = $is_ph ? 'PH' : 'Holiday';

    if ($is_sunday || $is_ph) {
        $check_exists = $conn->query("SELECT 1 FROM edu_attendance WHERE class_id = $cid AND section_id = $sid AND date = '$date' LIMIT 1");
        if ($check_exists && $check_exists->num_rows == 0) {
            $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status) 
                          SELECT $inst_id, student_id, class_id, section_id, '$date', '$default_status' 
                          FROM edu_student_enrollment 
                          WHERE class_id = $cid AND section_id = $sid $insert_status_filter");
        }
    }

    $m_start = date('Y-m-01', strtotime($date));
    $m_end = date('Y-m-t', strtotime($date));

    $students = $conn->query("
        SELECT u.id, u.full_name as name, u.profile_pic, e.class_no, e.roll_number as roll, TRIM(a.status) as status, 
               a.haircut_fine, a.register_fine, a.rules_break_fine, a.time_in, a.time_out,
               (SELECT COUNT(*) FROM edu_attendance WHERE student_id = u.id AND status = 'Present' AND date BETWEEN '$m_start' AND '$m_end') as m_p,
               (SELECT COUNT(*) FROM edu_attendance WHERE student_id = u.id AND status = 'Absent' AND date BETWEEN '$m_start' AND '$m_end') as m_a,
               (SELECT COUNT(*) FROM edu_attendance WHERE student_id = u.id AND status = 'Leave' AND date BETWEEN '$m_start' AND '$m_end') as m_l,
               (SELECT COUNT(*) FROM edu_attendance WHERE student_id = u.id AND (status = 'Holiday' OR status = 'PH') AND date BETWEEN '$m_start' AND '$m_end') as m_h
        FROM edu_student_enrollment e
        JOIN edu_users u ON e.student_id = u.id
        LEFT JOIN edu_attendance a ON u.id = a.student_id AND DATE(a.date) = '$date'
        WHERE e.class_id = $cid AND e.section_id = $sid $status_filter
        ORDER BY CAST(e.class_no AS UNSIGNED) ASC, e.class_no ASC
    ");

    if (!$students) {
        json_response(['error' => 'Database Query Failed (get_students)', 'db_error' => $conn->error]);
    }

    $res = [];
    $is_guest = (isset($_SESSION['edu_role']) && $_SESSION['edu_role'] === 'guest');
    while($row = $students->fetch_assoc()) {
        if ($is_guest) {
            $row['name'] = '?';
            $row['roll'] = '?';
            $row['class_no'] = '?';
        }
        $res[] = $row;
    }
    json_response($res);

} elseif ($action == 'get_summary') {
    $cid = isset($_GET['class_id']) ? (int)$_GET['class_id'] : 0;
    $sid = isset($_GET['section_id']) ? (int)$_GET['section_id'] : 0;
    $month = isset($_GET['month']) ? $_GET['month'] : date('m');
    $year = isset($_GET['year']) ? $_GET['year'] : date('Y');
    
    if ($cid == 0 || $sid == 0) {
        json_response(['success' => false, 'error' => 'Missing Class or Section ID']);
    }

    $start_date = "$year-$month-01";
    $end_date = date("Y-m-t", strtotime($start_date));
    
    // Check for fine columns
    $check_f = $conn->query("SHOW COLUMNS FROM edu_student_enrollment LIKE 'absent_fine_rate'");
    $has_fine_cols = ($check_f && $check_f->num_rows > 0);
    $fine_select = $has_fine_cols ? ", e.absent_fine_rate, e.special_fine" : "";

    $students = $conn->query("
        SELECT u.id, u.full_name as name, e.roll_number as roll, e.class_no, e.brought_attendance $fine_select
        FROM edu_student_enrollment e
        JOIN edu_users u ON e.student_id = u.id
        WHERE e.class_id = $cid AND e.section_id = $sid AND e.status = 'active'
        ORDER BY CAST(e.class_no AS UNSIGNED) ASC, e.class_no ASC
    ");
    
    if (!$students) {
        json_response(['error' => 'Database Query Failed (get_summary: students)', 'db_error' => $conn->error]);
    }
    
    $students_list = [];
    $is_guest = (isset($_SESSION['edu_role']) && $_SESSION['edu_role'] === 'guest');
    $sum_this_month = 0;
    
    while($s = $students->fetch_assoc()) {
        $sid_user = $s['id'];
        
        $manual_base = (int)($s['brought_attendance'] ?? 0);
        
        // AutoBrought (Historical Present before current month with Friday logic)
        $h_q = $conn->query("SELECT SUM(IF(DAYOFWEEK(date) = 6, 1, 2)) as score FROM edu_attendance WHERE student_id = $sid_user AND status = 'Present' AND date < '$start_date'");
        if (!$h_q) { json_response(['error' => "Query Failed (History): " . $conn->error]); }
        $auto_brought = (int)($h_q->fetch_assoc()['score'] ?? 0);
        $brought = $manual_base + $auto_brought;

        // This Month (Current month Present with Friday logic: Friday=1, Others=2)
        $m_q = $conn->query("SELECT SUM(IF(DAYOFWEEK(date) = 6, 1, 2)) as score FROM edu_attendance WHERE student_id = $sid_user AND status = 'Present' AND date BETWEEN '$start_date' AND '$end_date'");
        if (!$m_q) { json_response(['error' => "Query Failed (Current): " . $conn->error]); }
        $this_month = (int)($m_q->fetch_assoc()['score'] ?? 0);
        $sum_this_month += $this_month;
        
        // Absentees this month
        $a_q = $conn->query("SELECT COUNT(*) as cnt FROM edu_attendance WHERE student_id = $sid_user AND status = 'Absent' AND date BETWEEN '$start_date' AND '$end_date'");
        if (!$a_q) { json_response(['error' => "Query Failed (Absent): " . $conn->error]); }
        $absents = (int)$a_q->fetch_assoc()['cnt'];
        
        // Disciplinary Fines counting
        $d_q = $conn->query("
            SELECT SUM(haircut_fine) as h_cnt, SUM(register_fine) as r_cnt, SUM(rules_break_fine) as rb_cnt 
            FROM edu_attendance WHERE student_id = $sid_user AND date BETWEEN '$start_date' AND '$end_date'
        ");
        if (!$d_q) { json_response(['error' => "Query Failed (Fines): " . $conn->error]); }
        $d_fines = $d_q->fetch_assoc();
        
        // Fetch institution policy for these fine rates
        $inst_q = $conn->query("SELECT fine_hair_cut, fine_register, fine_rules_break, fine_attendance FROM edu_institutions WHERE id = $inst_id");
        $inst_rates = ($inst_q) ? $inst_q->fetch_assoc() : [];
        
        $h_fine_total = (int)($d_fines['h_cnt'] ?? 0) * (float)($inst_rates['fine_hair_cut'] ?? 0);
        $r_fine_total = (int)($d_fines['r_cnt'] ?? 0) * (float)($inst_rates['fine_register'] ?? 0);
        $rb_fine_total = (int)($d_fines['rb_cnt'] ?? 0) * (float)($inst_rates['fine_rules_break'] ?? 0);
        $abs_fine_rate = (float)($inst_rates['fine_attendance'] ?? 10.00);

        $spec_fine = (float)($s['special_fine'] ?? 0.00);
        $absent_fine_total = $absents * $abs_fine_rate;
        $disc_fine_total = $h_fine_total + $r_fine_total + $rb_fine_total;
        $total_fine = $absent_fine_total + $spec_fine + $disc_fine_total;

        $row_data = [
            'roll' => $s['class_no'] ?: $s['roll'],
            'name' => $s['name'],
            'brought' => $brought,
            'this_month' => $this_month,
            'total' => $brought + $this_month,
            'absentees' => $absents,
            'total_fine' => $total_fine
        ];

        if ($is_guest) {
            $row_data['name'] = '?'; $row_data['roll'] = '?'; $row_data['brought'] = '?'; $row_data['this_month'] = '?'; $row_data['total'] = '?'; $row_data['absentees'] = '?'; $row_data['total_fine'] = '?';
        }
        $students_list[] = $row_data;
    }

    // --- SECONDARY KPIS ---
    $start_q = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE e.class_id = $cid AND e.section_id = $sid AND u.date_of_admission < '$start_date' AND (u.status = 'active' OR (u.status = 'struck_off' AND u.struck_off_date >= '$start_date'))");
    $start_cnt = ($start_q) ? (int)$start_q->fetch_row()[0] : 0;

    $new_q = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE e.class_id = $cid AND e.section_id = $sid AND u.date_of_admission BETWEEN '$start_date' AND '$end_date'");
    $new_cnt = ($new_q) ? (int)$new_q->fetch_row()[0] : 0;

    $struck_q = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE e.class_id = $cid AND e.section_id = $sid AND u.status = 'struck_off' AND u.struck_off_date BETWEEN '$start_date' AND '$end_date'");
    $struck_cnt = ($struck_q) ? (int)$struck_q->fetch_row()[0] : 0;

    // Calendar Calculations
    $possible_per_student = 0;
    $iter = strtotime($start_date);
    $iter_end = strtotime($end_date);
    while($iter <= $iter_end) {
        $w = date('w', $iter); // 0=Sun, 5=Fri
        if ($w != 0) $possible_per_student += ($w == 5) ? 1 : 2;
        $iter = strtotime("+1 day", $iter);
    }

    $end_cnt = count($students_list);
    $total_possible = $end_cnt * $possible_per_student;
    $avg = ($total_possible > 0) ? round(($sum_this_month / $total_possible) * 100, 2) : 0;

    json_response([
        'success' => true,
        'students' => $students_list,
        'stats' => [
            'start_count' => $start_cnt,
            'new_admitted' => $new_cnt,
            'struck_off' => $struck_cnt,
            'end_count' => $end_cnt,
            'sum_attendance' => $sum_this_month,
            'total_possible_days' => $total_possible,
            'average_percentage' => $avg . '%'
        ]
    ]);
} elseif ($action == 'get_monthly_detailed_sheet') {
    $type = $_GET['type'] ?? 'student'; // 'student' or 'staff'
    $cid = (int)($_GET['class_id'] ?? 0);
    $sid = (int)($_GET['section_id'] ?? 0);
    $month = $_GET['month'] ?? date('m');
    $year = $_GET['year'] ?? date('Y');
    
    $start_date = "$year-$month-01";
    $end_date = date("Y-m-t", strtotime($start_date));
    $days_in_month = (int)date('t', strtotime($start_date));
    
    if ($type === 'staff') {
        $staff_type = $_GET['staff_type'] ?? 'all';
        $role_filter = "u.role IN ('staff', 'teacher', 'admin', 'principal', 'head', 'worker')";
        
        // Defensive check: Does user_type column exist online?
        $has_user_type = false;
        $check_col = $conn->query("SHOW COLUMNS FROM edu_users LIKE 'user_type'");
        if ($check_col && $check_col->num_rows > 0) $has_user_type = true;
        
        $ut_cond_t = $has_user_type ? "OR u.user_type = 'teaching'" : "";
        $ut_cond_nt = $has_user_type ? "AND u.user_type != 'teaching'" : "";

        if ($staff_type === 'teaching') {
            $role_filter = "(u.role = 'teacher' $ut_cond_t OR u.designation LIKE '%Teacher%' OR u.designation LIKE '%Professor%' OR u.designation LIKE '%Lecturer%' OR u.designation LIKE '%Principal%' OR u.designation LIKE '%Head%' OR u.designation LIKE '%Instructor%')";
        } else if ($staff_type === 'non-teaching') {
            $role_filter = "u.role IN ('staff', 'admin', 'principal', 'head') $ut_cond_nt AND (u.designation NOT LIKE '%Teacher%' AND u.designation NOT LIKE '%Professor%' AND u.designation NOT LIKE '%Lecturer%' AND u.designation NOT LIKE '%Principal%' AND u.designation NOT LIKE '%Head%' AND u.designation NOT LIKE '%Instructor%')";
        } else if ($staff_type === 'visiting') {
            $role_filter = "u.role = 'visiting'";
        }

        $people_q = $conn->query("
            SELECT u.id, u.full_name as name, u.profile_pic, 'Staff' as class_no
            FROM edu_users u
            WHERE u.institution_id = $inst_id AND ($role_filter) AND u.status = 'active'
            ORDER BY u.full_name ASC
        ");
        $table_name = "edu_staff_attendance";

        $id_col = "staff_id";
    } else {
        $check_f = $conn->query("SHOW COLUMNS FROM edu_student_enrollment LIKE 'absent_fine_rate'");
        $fine_rate_select = ($check_f && $check_f->num_rows > 0) ? ", e.absent_fine_rate" : "";

        $people_q = $conn->query("
            SELECT u.id, u.full_name as name, u.profile_pic, e.class_no, e.status $fine_rate_select
            FROM edu_student_enrollment e
            JOIN edu_users u ON e.student_id = u.id
            WHERE e.class_id = $cid AND e.section_id = $sid 
            AND (e.status = 'active' OR e.student_id IN (SELECT DISTINCT student_id FROM edu_attendance WHERE class_id = $cid AND section_id = $sid AND date BETWEEN '$start_date' AND '$end_date'))
            ORDER BY CAST(e.class_no AS UNSIGNED) ASC, e.class_no ASC
        ");
        $table_name = "edu_attendance";
        $id_col = "student_id";
    }
    
    $res = [];
    if($people_q) {
        while($s = $people_q->fetch_assoc()) {
            $sid_user = $s['id'];
            $att_q = $conn->query("SELECT DAY(date) as day, status FROM $table_name WHERE $id_col = $sid_user AND date BETWEEN '$start_date' AND '$end_date'");
            $attendance = [];
            if($att_q) {
                while($a = $att_q->fetch_assoc()) {
                    $attendance[$a['day']] = $a['status'];
                }
            }
            $s['attendance'] = (object)$attendance;
            $res[] = $s;
        }
    }
    
    // Fetch Public Holidays
    $hol_q = $conn->query("SELECT id, name, from_date, to_date, class_ids FROM edu_public_holidays WHERE institution_id = $inst_id AND (from_date <= '$end_date' AND to_date >= '$start_date')");
    $holidays_meta = [];
    if($hol_q) {
        while($h = $hol_q->fetch_assoc()) $holidays_meta[] = $h;
    }

    // Fetch Global Fine Rate
    $inst_q = $conn->query("SELECT fine_attendance FROM edu_institutions WHERE id = $inst_id");
    $global_fine_rate = ($inst_q && $inst_q->num_rows > 0) ? (float)$inst_q->fetch_assoc()['fine_attendance'] : 10.0;
    
    json_response([
        'success' => true,
        'days' => $days_in_month,
        'students' => $res,
        'holidays' => $holidays_meta,
        'fine_rate' => $global_fine_rate,
        'month_name' => date('F', strtotime($start_date)),
        'year' => $year
    ]);
} elseif ($action == 'get_average_summary') {
    $cid = (int)$_GET['class_id'];
    $sid = (int)$_GET['section_id'];
    $month = $_GET['month'];
    $year = $_GET['year'];
    
    $month_start = "$year-$month-01";
    $month_end = date("Y-m-t", strtotime($month_start));
    
    // 1. Students at start of month (Enrolled before this month AND not struck off before this month)
    // Note: We use edu_users.created_at for admission date.
    $start_q = $conn->query("
        SELECT COUNT(*) as cnt 
        FROM edu_student_enrollment e
        JOIN edu_users u ON e.student_id = u.id
        WHERE e.class_id = $cid AND e.section_id = $sid 
        AND u.created_at < '$month_start'
        AND u.id NOT IN (SELECT student_id FROM edu_attendance WHERE status = 'Struck Off' AND date < '$month_start')
    ");
    $start_count = $start_q->fetch_assoc()['cnt'];

    // 2. Struck Off in this month
    $struck_off_q = $conn->query("
        SELECT COUNT(DISTINCT student_id) as cnt 
        FROM edu_attendance 
        WHERE class_id = $cid AND section_id = $sid 
        AND status = 'Struck Off' 
        AND date BETWEEN '$month_start' AND '$month_end'
    ");
    $struck_off_count = $struck_off_q->fetch_assoc()['cnt'];

    // 3. New Admitted in this month
    $new_q = $conn->query("
        SELECT COUNT(*) as cnt 
        FROM edu_student_enrollment e
        JOIN edu_users u ON e.student_id = u.id
        WHERE e.class_id = $cid AND e.section_id = $sid 
        AND u.created_at BETWEEN '$month_start 00:00:00' AND '$month_end 23:59:59'
    ");
    $new_count = $new_q->fetch_assoc()['cnt'];

    // 4. Total Students at end of month
    $end_count = $start_count + $new_count - $struck_off_count;

    // 5. Total Attendance (Sum of this month attendance for all students)
    // "This Month Attendance" = Count(Present) * 2
    $total_att_q = $conn->query("
        SELECT COUNT(*) as present_days 
        FROM edu_attendance 
        WHERE class_id = $cid AND section_id = $sid 
        AND status = 'Present' 
        AND date BETWEEN '$month_start' AND '$month_end'
    ");
    $total_present_days = $total_att_q->fetch_assoc()['present_days'];
    $total_attendance_value = $total_present_days * 2;

    // 6. Average Attendance
    $avg_att = ($end_count > 0) ? round($total_attendance_value / $end_count, 2) : 0;

    json_response([
        'start_count' => $start_count,
        'struck_off' => $struck_off_count,
        'new_admitted' => $new_count,
        'end_count' => $end_count,
        'total_attendance' => $total_attendance_value,
        'average_attendance' => $avg_att
    ]);
} elseif ($action == 'get_staff_attendance') {
    $date = $_GET['date'];

    // Check for extra_classes column
    $check_extra = $conn->query("SHOW COLUMNS FROM edu_staff_attendance LIKE 'extra_classes'");
    $extra_col = ($check_extra && $check_extra->num_rows > 0) ? ", a.extra_classes" : "";

    $m_start = date('Y-m-01', strtotime($date));
    $m_end = date('Y-m-t', strtotime($date));

    // If role is staff, return personal records for the month instead of all staff for the day
    if ($role === 'staff') {
        $staff_id = $_SESSION['edu_user_id'];
        
        // Granular column checks
        $check_cols = $conn->query("SHOW COLUMNS FROM edu_staff_attendance");
        $existing_cols = [];
        while($c = $check_cols->fetch_assoc()) {
            $existing_cols[] = $c['Field'];
        }

        $time_select = "";
        if(in_array('time_in', $existing_cols)) $time_select .= ", time_in";
        if(in_array('time_out', $existing_cols)) $time_select .= ", time_out";
        if(in_array('source', $existing_cols)) $time_select .= ", source";

        // Return all days for the selected month
        $res = [];
        $temp_date = $m_start;
        while ($temp_date <= $m_end) {
            $staff_q = $conn->query("
                SELECT status $time_select
                FROM edu_staff_attendance 
                WHERE staff_id = $staff_id AND date = '$temp_date'
            ");
            
            if ($staff_q && $staff_q->num_rows > 0) {
                $row = $staff_q->fetch_assoc();
            } else {
                $row = ['status' => ''];
                if(in_array('time_in', $existing_cols)) $row['time_in'] = null;
                if(in_array('time_out', $existing_cols)) $row['time_out'] = null;
                if(in_array('source', $existing_cols)) $row['source'] = null;
            }
            
            $row['id'] = $staff_id;
            $row['name'] = date('d M (D)', strtotime($temp_date));
            $row['full_date'] = $temp_date;
            $row['is_personal'] = true;
            
            // Monthly totals (repeated for each row to match standard structure, but JS can just use the first one)
            $row['m_p'] = 0; $row['m_a'] = 0; $row['m_l'] = 0; 
            
            $res[] = $row;
            $temp_date = date('Y-m-d', strtotime($temp_date . ' +1 day'));
        }
        json_response($res);
    }


    $is_sunday = (date('w', strtotime($date)) == 0);
    $is_ph = $conn->query("SELECT 1 FROM edu_public_holidays WHERE institution_id = $inst_id AND '$date' BETWEEN from_date AND to_date LIMIT 1")->num_rows > 0;
    $default_status = $is_ph ? 'PH' : 'Holiday';

    if ($is_sunday || $is_ph) {
        $check_exists = $conn->query("SELECT 1 FROM edu_staff_attendance WHERE institution_id = $inst_id AND date = '$date' LIMIT 1");
        if ($check_exists && $check_exists->num_rows == 0) {
            $conn->query("INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status)
                          SELECT $inst_id, id, '$date', '$default_status'
                          FROM edu_users
                          WHERE institution_id = $inst_id AND role IN ('staff', 'teacher', 'admin', 'principal', 'head')");
        }
    }

    $staff = $conn->query("
        SELECT u.id, u.full_name as name, u.profile_pic, u.role, u.role as edu_role, u.designation, u.user_type, a.status, a.time_in, a.time_out $extra_col,
               (SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id = u.id AND status = 'Present' AND date BETWEEN '$m_start' AND '$m_end') as m_p,
               (SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id = u.id AND status = 'Absent' AND date BETWEEN '$m_start' AND '$m_end') as m_a,
               (SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id = u.id AND status = 'Leave' AND date BETWEEN '$m_start' AND '$m_end') as m_l,
               (SELECT COUNT(*) FROM edu_staff_attendance WHERE staff_id = u.id AND (status = 'Holiday' OR status = 'PH') AND date BETWEEN '$m_start' AND '$m_end') as m_h
        FROM edu_users u
        LEFT JOIN edu_staff_attendance a ON u.id = a.staff_id AND DATE(a.date) = '$date'
        WHERE u.institution_id = $inst_id AND u.role IN ('staff', 'teacher', 'admin', 'principal', 'head')
        ORDER BY u.full_name ASC
    ");
    
    if (!$staff) {
        json_response(['error' => 'Database Query Failed (get_staff_attendance)', 'sql_error' => $conn->error]);
    }

    $res = [];
    $is_guest = (isset($_SESSION['edu_role']) && $_SESSION['edu_role'] === 'guest');
    while($row = $staff->fetch_assoc()) {
        if ($is_guest) $row['name'] = '?';
        $res[] = $row;
    }
    json_response($res);

} elseif ($action == 'save_staff_attendance') {
    if (!in_array($role, $allowed_roles)) { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    $date = $data['date'];
    $attendances = $data['attendances']; // Array [staff_id => {status, time_in, time_out}]
    
    $conn->begin_transaction();
    try {
        // Safe check for extra_classes column
        $check_ex = $conn->query("SHOW COLUMNS FROM edu_staff_attendance LIKE 'extra_classes'");
        $has_extra = ($check_ex && $check_ex->num_rows > 0);

        $extra_classes = $data['extra_classes'] ?? [];
        foreach($attendances as $sid => $entry) {
            $sid = (int)$sid;
            if(is_array($entry)) {
                $status = $conn->real_escape_string($entry['status'] ?? '');
                $t_in = $entry['time_in'] ? "'".$conn->real_escape_string($entry['time_in'])."'" : "NULL";
                $t_out = $entry['time_out'] ? "'".$conn->real_escape_string($entry['time_out'])."'" : "NULL";
            } else {
                $status = $conn->real_escape_string($entry);
                $t_in = "NULL";
                $t_out = "NULL";
            }
            $ex_cnt = (int)($extra_classes[$sid] ?? 0);
            
            if ($has_extra) {
                $conn->query("INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status, time_in, time_out, extra_classes) 
                              VALUES ($inst_id, $sid, '$date', '$status', $t_in, $t_out, $ex_cnt)
                              ON DUPLICATE KEY UPDATE status = '$status', time_in = $t_in, time_out = $t_out, extra_classes = $ex_cnt");
            } else {
                $conn->query("INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status, time_in, time_out) 
                              VALUES ($inst_id, $sid, '$date', '$status', $t_in, $t_out)
                              ON DUPLICATE KEY UPDATE status = '$status', time_in = $t_in, time_out = $t_out");
            }
        }
        $conn->commit();
        json_response(['success' => true]);
    } catch (Exception $e) {
        $conn->rollback();
        json_response(['success' => false, 'error' => $e->getMessage()]);
    }

} elseif ($action == 'get_staff_summary') {
    // Allow admin, developer, super_admin, AND guest (read-only masked)
    if (!in_array($role, $allowed_roles) && $role != 'guest') { http_response_code(403); exit; }
    
    $month = isset($_GET['month']) ? $_GET['month'] : date('m');
    $year = isset($_GET['year']) ? $_GET['year'] : date('Y');
    
    $start_date = "$year-$month-01";
    $end_date = date("Y-m-t", strtotime($start_date));

    $type = $_GET['staff_type'] ?? 'all';
    $role_filter = "u.role IN ('staff', 'teacher', 'admin', 'principal', 'head')";
    
    if ($role === 'staff' || $type === 'personal') {
        $staff_user_id = $_SESSION['edu_user_id'];
        $role_filter = "u.id = $staff_user_id";
    } else if ($type === 'teaching') {
        $role_filter = "(u.role = 'teacher' OR u.user_type = 'teaching' OR u.designation LIKE '%Teacher%' OR u.designation LIKE '%Professor%' OR u.designation LIKE '%Lecturer%' OR u.designation LIKE '%Principal%' OR u.designation LIKE '%Head%' OR u.designation LIKE '%Instructor%')";
    } else if ($type === 'non-teaching') {
        $role_filter = "u.role IN ('staff', 'admin', 'principal', 'head') AND (u.user_type != 'teaching' AND u.designation NOT LIKE '%Teacher%' AND u.designation NOT LIKE '%Professor%' AND u.designation NOT LIKE '%Lecturer%' AND u.designation NOT LIKE '%Principal%' AND u.designation NOT LIKE '%Head%' AND u.designation NOT LIKE '%Instructor%')";
    } else if ($type === 'visiting') {
        $role_filter = "u.role = 'visiting'";
    }

    // Check availability of designation column to avoid crash
    $check_desig = $conn->query("SHOW COLUMNS FROM edu_users LIKE 'designation'");
    $has_desig = ($check_desig && $check_desig->num_rows > 0);
    $desig_col = $has_desig ? ", u.designation" : "";

    $staff_q = $conn->query("
        SELECT u.id, u.full_name $desig_col
        FROM edu_users u
        WHERE u.institution_id = $inst_id AND $role_filter
    ");
    
    if (!$staff_q) {
        json_response(['success' => false, 'error' => 'Staff Query Failed', 'db_error' => $conn->error]);
    }
    
    $staff_list = [];
    $idx = 1;

    while($s = $staff_q->fetch_assoc()) {
        $sid = $s['id'];
        
        // Historical Metrics (Before current month)
        $hist_q = $conn->query("
            SELECT 
                SUM(CASE WHEN status = 'Leave' THEN 1 ELSE 0 END) as leaves,
                SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as absents
            FROM edu_staff_attendance
            WHERE staff_id = $sid AND date < '$start_date'
        ");
        if (!$hist_q) continue; // Skip if query fails (or handle error)
        
        $hist = $hist_q->fetch_assoc();
        $l_brought = (int)($hist['leaves'] ?? 0);
        $a_brought = (int)($hist['absents'] ?? 0);

        // Current Month Metrics
        $curr_q = $conn->query("
            SELECT 
                SUM(CASE WHEN status = 'Leave' THEN 1 ELSE 0 END) as leaves,
                SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as absents
            FROM edu_staff_attendance
            WHERE staff_id = $sid AND date BETWEEN '$start_date' AND '$end_date'
        ");
        if (!$curr_q) continue;

        $curr = $curr_q->fetch_assoc();
        $l_this = (int)($curr['leaves'] ?? 0);
        $a_this = (int)($curr['absents'] ?? 0);

        $designation_val = isset($s['designation']) ? ($s['designation'] ?: 'N/A') : 'N/A';

        $staff_list[] = [
            'sno' => $idx++,
            'name' => $s['full_name'],
            'designation' => $designation_val,
            'l_brought' => $l_brought,
            'l_this' => $l_this,
            'l_total' => $l_brought + $l_this,
            'a_brought' => $a_brought,
            'a_this' => $a_this,
            'a_total' => $a_brought + $a_this
        ];
    }

    json_response([
        'success' => true,
        'staff' => $staff_list
    ]);
} elseif ($action == 'get_all_staff_salaries') {
    if (!in_array($role, $allowed_roles) && $role != 'guest') { http_response_code(403); exit; }
    $staff = $conn->query("SELECT id, full_name, salary FROM edu_users WHERE institution_id = $inst_id AND role IN ('staff', 'teacher', 'admin') ORDER BY full_name ASC");
    $data = [];
    while($s = $staff->fetch_assoc()) $data[] = $s;
    json_response($data);
} elseif ($action == 'set_brought_attendance') {
    if ($role != 'admin' && $role != 'staff') { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    $cid = (int)$data['class_id'];
    $sid = (int)$data['section_id'];
    $text = $data['batch_text'];

    // Parsing logic for "1 Irfan 120, 2 Imran 111" or "1 Irfan 120\n2 Imran 111"
    // We look for [Roll] [Name (multiple words)] [Value]
    preg_match_all('/(\w+)\s+(.+?)\s+(\d+)(?:,|$|\n)/', $text, $matches, PREG_SET_ORDER);
    
    $processed = 0;
    $errors = [];
    
    foreach ($matches as $match) {
        $c_no = $conn->real_escape_string(trim($match[1]));
        $val = (int)$match[3];
        
        $sql = "UPDATE edu_student_enrollment SET brought_attendance = $val 
                WHERE class_id = $cid AND section_id = $sid AND class_no = '$c_no'";
        if ($conn->query($sql)) {
            if ($conn->affected_rows > 0) $processed++;
        } else {
            $errors[] = $conn->error;
        }
    }
    
    
    json_response(['success' => true, 'processed' => $processed, 'errors' => $errors]);
} elseif ($action == 'save_student_attendance_bulk') {
    $allowed_bulk_roles = ['admin', 'developer', 'super_admin', 'useradmin', 'staff', 'teacher', 'useruser'];
    if (!in_array($role, $allowed_bulk_roles)) { 
        error_log("Bulk Save Denied: Role '$role' not in " . implode(',', $allowed_bulk_roles));
        http_response_code(403); 
        exit; 
    }
    $data = json_decode(file_get_contents('php://input'), true);
    $cid = (int)$data['class_id'];
    $sid = (int)$data['section_id'];
    $date = $conn->real_escape_string($data['date']);
    $status_data = $data['status']; // [student_id => status]

    // Check if already submitted
    $check_q = $conn->query("SELECT id FROM edu_attendance_submission WHERE institution_id = $inst_id AND class_id = $cid AND section_id = $sid AND date = '$date' LIMIT 1");
    $already_submitted = ($check_q && $check_q->num_rows > 0);

    // If already submitted and NOT an admin, create a request instead of direct save
    if ($already_submitted && !$is_admin) {
        $create_q = "CREATE TABLE IF NOT EXISTS `edu_attendance_reedit_requests` (
            `id` int(11) NOT NULL AUTO_INCREMENT,
            `institution_id` int(11) NOT NULL,
            `class_id` int(11) NOT NULL,
            `section_id` int(11) NOT NULL,
            `date` date NOT NULL,
            `requested_by` int(11) NOT NULL,
            `teacher_name` varchar(255) DEFAULT NULL,
            `proposed_data` LONGTEXT NOT NULL,
            `status` enum('pending','approved','rejected') DEFAULT 'pending',
            `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        
        if (!$conn->query($create_q)) {
            error_log("Failed to create edu_attendance_reedit_requests: " . $conn->error);
            json_response(['success' => false, 'error' => 'Database error: Could not initialize requests table.']);
        }

        $user_id_s = (int)($_SESSION['edu_user_id'] ?? 0);
        $tname_q   = $conn->query("SELECT full_name FROM edu_users WHERE id = $user_id_s LIMIT 1");
        $tname_assoc = ($tname_q && $tname_q->num_rows > 0) ? $tname_q->fetch_assoc() : null;
        $tname_esc = $tname_assoc ? $conn->real_escape_string($tname_assoc['full_name']) : 'Teacher';
        
        // Include more context in the proposed data if needed, but for now just the JSON
        $json_data = $conn->real_escape_string(json_encode($data));

        $insert_q = "INSERT INTO edu_attendance_reedit_requests (institution_id, class_id, section_id, date, requested_by, teacher_name, proposed_data)
                     VALUES ($inst_id, $cid, $sid, '$date', $user_id_s, '$tname_esc', '$json_data')";
        
        if ($conn->query($insert_q)) {
            json_response([
                'success' => true, 
                'request_pending' => true, 
                'message' => 'Attendance already submitted. Your change request has been sent to Admin for approval.'
            ]);
        } else {
            error_log("Failed to insert reedit request: " . $conn->error);
            json_response(['success' => false, 'error' => 'Failed to log your request: ' . $conn->error]);
        }
    }

    $conn->begin_transaction();
    try {
        require_once '../../includes/FeeManager.php';
        $fm = new FeeManager($conn, $inst_id);
        $m_name = date('F', strtotime($date));
        $y_val = (int)date('Y', strtotime($date));

        foreach($status_data as $std_id => $entry) {
            $std_id = (int)$std_id;
            if (is_array($entry)) {
                $stat = $conn->real_escape_string($entry['status'] ?? 'Present');
                $h_f = (int)($entry['haircut'] ?? 0);
                $r_f = (int)($entry['register'] ?? 0);
                $rb_f = (int)($entry['rules'] ?? 0);
                $t_in = !empty($entry['time_in']) ? "'".$conn->real_escape_string($entry['time_in'])."'" : "NULL";
                $t_out = !empty($entry['time_out']) ? "'".$conn->real_escape_string($entry['time_out'])."'" : "NULL";
            } else {
                $stat = $conn->real_escape_string($entry);
                $h_f = $r_f = $rb_f = 0;
                $t_in = $t_out = "NULL";
            }
            
            $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status, haircut_fine, register_fine, rules_break_fine, time_in, time_out) 
                          VALUES ($inst_id, $std_id, $cid, $sid, '$date', '$stat', $h_f, $r_f, $rb_f, $t_in, $t_out)
                          ON DUPLICATE KEY UPDATE status = '$stat', haircut_fine = $h_f, register_fine = $r_f, rules_break_fine = $rb_f, time_in = $t_in, time_out = $t_out");
            
            if ($stat === 'Struck Off') {
                $conn->query("UPDATE edu_student_enrollment SET status = 'struck_off' WHERE student_id = $std_id");
            }
            $fm->syncAttendanceFines($std_id, $m_name, $y_val);
        }
        $conn->commit();

        // Mark as Done
        $conn->query("CREATE TABLE IF NOT EXISTS `edu_attendance_submission` (
            `id` int(11) NOT NULL AUTO_INCREMENT,
            `institution_id` int(11) NOT NULL,
            `class_id` int(11) NOT NULL,
            `section_id` int(11) NOT NULL,
            `date` date NOT NULL,
            `submitted_by` int(11) DEFAULT NULL,
            `submitted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            `teacher_name` varchar(255) DEFAULT NULL,
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_submission` (`institution_id`,`class_id`,`section_id`,`date`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        $user_id_s = (int)($_SESSION['edu_user_id'] ?? 0);
        $tname_q   = $conn->query("SELECT full_name FROM edu_users WHERE id = $user_id_s LIMIT 1");
        $tname_esc = ($tname_q && $tname_q->num_rows > 0) ? $conn->real_escape_string($tname_q->fetch_assoc()['full_name']) : 'Teacher';
        $conn->query("INSERT INTO edu_attendance_submission (institution_id, class_id, section_id, date, submitted_by, teacher_name)
                      VALUES ($inst_id, $cid, $sid, '$date', $user_id_s, '$tname_esc')
                      ON DUPLICATE KEY UPDATE submitted_by = $user_id_s, teacher_name = '$tname_esc', submitted_at = NOW()");

        json_response(['success' => true]);
    } catch (Exception $e) {
        $conn->rollback();
        json_response(['success' => false, 'error' => $e->getMessage()]);
    }

} elseif ($action == 'get_reedit_requests') {
    if (!$is_admin) json_response(['success' => false, 'error' => 'Admin only']);
    
    // Check if table exists first to avoid crash
    $table_check = $conn->query("SHOW TABLES LIKE 'edu_attendance_reedit_requests'");
    if (!$table_check || $table_check->num_rows == 0) {
        json_response(['success' => true, 'requests' => []]);
    }

    $reqs = $conn->query("
        SELECT r.*, c.name as class_name, s.name as section_name 
        FROM edu_attendance_reedit_requests r
        JOIN edu_classes c ON r.class_id = c.id
        JOIN edu_sections s ON r.section_id = s.id
        WHERE r.institution_id = $inst_id AND r.status = 'pending'
        ORDER BY r.created_at DESC
    ");
    $list = [];
    if ($reqs) {
        while($row = $reqs->fetch_assoc()) $list[] = $row;
    }
    json_response(['success' => true, 'requests' => $list]);

} elseif ($action == 'approve_reedit_request') {
    if (!$is_admin) json_response(['success' => false, 'error' => 'Admin only']);
    $id = (int)($_GET['id'] ?? 0);
    
    $req_q = $conn->query("SELECT * FROM edu_attendance_reedit_requests WHERE id = $id AND institution_id = $inst_id LIMIT 1");
    if (!$req_q || $req_q->num_rows == 0) json_response(['success' => false, 'error' => 'Request not found']);
    
    $req = $req_q->fetch_assoc();
    $proposed = json_decode($req['proposed_data'], true);
    
    // Logic to apply the proposed data (same as save_student_attendance_bulk)
    $cid  = (int)$proposed['class_id'];
    $sid  = (int)$proposed['section_id'];
    $date = $conn->real_escape_string($proposed['date']);
    $status_data = $proposed['status'];

    $conn->begin_transaction();
    try {
        require_once '../../includes/FeeManager.php';
        $fm = new FeeManager($conn, $inst_id);
        $m_name = date('F', strtotime($date));
        $y_val = (int)date('Y', strtotime($date));

        foreach($status_data as $std_id => $entry) {
            $std_id = (int)$std_id;
            if (is_array($entry)) {
                $stat = $conn->real_escape_string($entry['status'] ?? 'Present');
                $h_f = (int)($entry['haircut'] ?? 0);
                $r_f = (int)($entry['register'] ?? 0);
                $rb_f = (int)($entry['rules'] ?? 0);
                $t_in = !empty($entry['time_in']) ? "'".$conn->real_escape_string($entry['time_in'])."'" : "NULL";
                $t_out = !empty($entry['time_out']) ? "'".$conn->real_escape_string($entry['time_out'])."'" : "NULL";
            } else {
                $stat = $conn->real_escape_string($entry);
                $h_f = $r_f = $rb_f = 0;
                $t_in = $t_out = "NULL";
            }
            $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status, haircut_fine, register_fine, rules_break_fine, time_in, time_out) 
                          VALUES ($inst_id, $std_id, $cid, $sid, '$date', '$stat', $h_f, $r_f, $rb_f, $t_in, $t_out)
                          ON DUPLICATE KEY UPDATE status = '$stat', haircut_fine = $h_f, register_fine = $r_f, rules_break_fine = $rb_f, time_in = $t_in, time_out = $t_out");
            $fm->syncAttendanceFines($std_id, $m_name, $y_val);
        }
        
        // Update submission info
        $conn->query("UPDATE edu_attendance_submission SET submitted_at = NOW(), teacher_name = '".$conn->real_escape_string($req['teacher_name'])."'
                      WHERE institution_id = $inst_id AND class_id = $cid AND section_id = $sid AND date = '$date'");
        
        // Mark request as approved
        $conn->query("UPDATE edu_attendance_reedit_requests SET status = 'approved' WHERE id = $id");
        
        $conn->commit();
        json_response(['success' => true, 'message' => 'Request approved and attendance updated.']);
    } catch (Exception $e) {
        $conn->rollback();
        json_response(['success' => false, 'error' => $e->getMessage()]);
    }

} elseif ($action == 'reject_reedit_request') {
    if (!$is_admin) json_response(['success' => false, 'error' => 'Admin only']);
    $id = (int)($_GET['id'] ?? 0);
    $conn->query("UPDATE edu_attendance_reedit_requests SET status = 'rejected' WHERE id = $id AND institution_id = $inst_id");
    json_response(['success' => true, 'message' => 'Request rejected. Original attendance remains.']);

} elseif ($action == 'set_student_fines') {
    if ($role != 'admin' && $role != 'developer' && $role != 'super_admin') { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    $cid = (int)$data['class_id'];
    $sid = (int)$data['section_id'];
    $text = $data['batch_text'];

    // Format: Roll AbsentFine SpecialFine
    preg_match_all('/(\w+)\s+(\d+)\s+(\d+)(?:,|$|\n)/', $text, $matches, PREG_SET_ORDER);
    
    $cnt = 0;
    foreach ($matches as $m) {
        $c_no = $conn->real_escape_string(trim($m[1]));
        $afine = (int)$m[2];
        $sfine = (int)$m[3];
        
        $sql = "UPDATE edu_student_enrollment SET absent_fine_rate = $afine, special_fine = $sfine 
                WHERE class_id = $cid AND section_id = $sid AND class_no = '$c_no'";
        if ($conn->query($sql) && $conn->affected_rows > 0) $cnt++;
    }
    json_response(['success' => true, 'processed' => $cnt]);

} elseif ($action == 'get_leaderboard') {
    // Top 5 Students
    $res = $conn->query("
        SELECT u.full_name as name, 
               (SELECT COUNT(*) FROM edu_attendance WHERE student_id = u.id AND status = 'Present') * 100.0 / 
               (SELECT COUNT(*) FROM edu_attendance WHERE student_id = u.id) as att_score
        FROM edu_users u
        WHERE u.institution_id = $inst_id AND u.role = 'student'
        HAVING att_score IS NOT NULL
        ORDER BY att_score DESC
        LIMIT 5
    ");
    
    $leaderboard = [];
    while($row = $res->fetch_assoc()) $leaderboard[] = $row;
    json_response($leaderboard);

} elseif ($action == 'get_attendance_trends') {
    // 6-Month Trend Data
    $trends = [];
    for($i=5; $i>=0; $i--) {
        $m = date('m', strtotime("-$i months"));
        $y = date('Y', strtotime("-$i months"));
        $q = $conn->query("SELECT COUNT(*) as cnt FROM edu_attendance WHERE institution_id = $inst_id AND status = 'Present' AND month(date) = $m AND year(date) = $y");
        $trends[] = [
            'label' => date('M', mktime(0,0,0,$m,1)),
            'value' => (int)$q->fetch_assoc()['cnt']
        ];
    }
    json_response($trends);

} elseif ($action == 'get_daily_marking_gaps') {
    if ($role != 'admin' && $role != 'developer' && $role != 'super_admin') { http_response_code(403); exit; }
    
    $today = date('Y-m-d');
    $day_name = date('l'); 

    // 1. Get absent staff for today
    $absent_q = $conn->query("SELECT staff_id FROM edu_staff_attendance WHERE date = '$today' AND status = 'Absent' AND institution_id = $inst_id");
    $absent_ids = [];
    while($row = $absent_q->fetch_assoc()) $absent_ids[] = (int)$row['staff_id'];

    if (empty($absent_ids)) {
        json_response(['slots' => [], 'staff' => []]);
    }

    $id_list = implode(',', $absent_ids);
    
    // 2. Map absences to timetable slots
    $sql = "
        SELECT t.id as slot_id, t.period_number, t.start_time, t.end_time, t.subject_id,
               c.name as class_name, s.name as section_name, sub.name as subject_name,
               u.full_name as original_teacher
        FROM edu_timetable t
        JOIN edu_classes c ON t.class_id = c.id
        JOIN edu_sections s ON t.section_id = s.id
        JOIN edu_subjects sub ON t.subject_id = sub.id
        JOIN edu_users u ON t.staff_id = u.id
        WHERE t.institution_id = $inst_id 
          AND t.day_of_week = '$day_name' 
          AND t.staff_id IN ($id_list)
          AND t.activity_type = 'class'
        ORDER BY t.period_number, t.start_time
    ";
    
    $res = $conn->query($sql);
    $slots = [];
    while($row = $res->fetch_assoc()) $slots[] = $row;
    
    // 3. Get present staff pool
    $present_q = $conn->query("SELECT id, full_name FROM edu_users WHERE institution_id = $inst_id AND role IN ('staff', 'teacher', 'admin') AND id NOT IN ($id_list) ORDER BY full_name");
    $staff_pool = [];
    while($row = $present_q->fetch_assoc()) $staff_pool[] = $row;

    // 4. Fetch competencies (Staff Subject mapping)
    $comp_res = $conn->query("SELECT staff_id, subject_id FROM edu_staff_competencies WHERE institution_id = $inst_id");
    $competencies = [];
    $comp_check = $conn->query("SHOW TABLES LIKE 'edu_staff_competencies'");
    if ($comp_check && $comp_check->num_rows > 0) {
        $comp_res = $conn->query("SELECT staff_id, subject_id FROM edu_staff_competencies WHERE institution_id = $inst_id");
        while($c = $comp_res->fetch_assoc()) {
            $competencies[$c['staff_id']][] = (int)$c['subject_id'];
        }
    }

    json_response([
        'slots' => $slots, 
        'staff' => $staff_pool,
        'competencies' => $competencies
    ]);

} elseif ($action == 'mark_proxy_performance') {
    if ($role != 'admin' && $role != 'developer' && $role != 'super_admin') { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    
    $replacement_staff_id = (int)$data['staff_id'];
    $slot_id = (int)$data['slot_id'];
    $level = $data['level'] ?? 'class'; // 'low', 'normal', 'high', or 'class'
    $today = date('Y-m-d');

    // 1. Resolve payout amount
    $payout = 0;
    if ($level === 'class') {
        $slot_q = $conn->query("SELECT c.bonus_amount FROM edu_timetable t JOIN edu_classes c ON t.class_id = c.id WHERE t.id = $slot_id LIMIT 1");
        $payout = ($slot_q && $slot_q->num_rows > 0) ? (float)$slot_q->fetch_assoc()['bonus_amount'] : 0;
    } else {
        $inst_res = $conn->query("SELECT extra_rate_low, extra_rate_normal, extra_rate_high FROM edu_institutions WHERE id = $inst_id");
        if ($inst_res && $inst_res->num_rows > 0) {
            $inst = $inst_res->fetch_assoc();
            if ($level === 'low') $payout = (float)($inst['extra_rate_low'] ?? 0);
            else if ($level === 'normal') $payout = (float)($inst['extra_rate_normal'] ?? 0);
            else if ($level === 'high') $payout = (float)($inst['extra_rate_high'] ?? 0);
        }
    }

    // 2. Automatically credit merit bonus
    $check = $conn->query("SELECT id, extra_classes, proxy_bonus_amount FROM edu_staff_attendance WHERE staff_id = $replacement_staff_id AND date = '$today'");
    
    if ($check->num_rows > 0) {
        $row = $check->fetch_assoc();
        $new_cnt = (int)$row['extra_classes'] + 1;
        $new_proxy_bal = (float)$row['proxy_bonus_amount'] + $payout;
        $conn->query("UPDATE edu_staff_attendance SET extra_classes = $new_cnt, proxy_bonus_amount = $new_proxy_bal, status = 'Present' WHERE id = {$row['id']}");
    } else {
        $conn->query("INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status, extra_classes, proxy_bonus_amount) 
                      VALUES ($inst_id, $replacement_staff_id, '$today', 'Present', 1, $payout)");
    }

    json_response(['success' => true]);

} elseif ($action == 'get_attendance_intel') {
    $allowed_intel_roles = ['admin', 'developer', 'super_admin', 'principal', 'useradmin'];
    if (!in_array($role, $allowed_intel_roles)) { http_response_code(403); exit; }
    $today = date('Y-m-d');
    
    $q_total = $conn->query("SELECT COUNT(*) FROM edu_users WHERE institution_id = $inst_id AND role IN ('staff', 'teacher', 'admin')");
    $total_staff = ($q_total) ? (int)$q_total->fetch_row()[0] : 0;
    
    $q_present = $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE institution_id = $inst_id AND date = '$today' AND status = 'Present'");
    $present_staff = ($q_present) ? (int)$q_present->fetch_row()[0] : 0;
    
    $q_absent = $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE institution_id = $inst_id AND date = '$today' AND status = 'Absent'");
    $absent_staff = ($q_absent) ? (int)$q_absent->fetch_row()[0] : 0;

    $q_leave = $conn->query("SELECT COUNT(*) FROM edu_staff_attendance WHERE institution_id = $inst_id AND date = '$today' AND status = 'Leave'");
    $leave_staff = ($q_leave) ? (int)$q_leave->fetch_row()[0] : 0;
    
    // Link to Institutional Absence Limit
    $inst_q = $conn->query("SELECT absence_limit FROM edu_institutions WHERE id = $inst_id");
    $limit = ($inst_q && $inst_q->num_rows > 0) ? (int)$inst_q->fetch_assoc()['absence_limit'] : 2;

    // Monthly Red-Zone (Exceeding the limit)
    $start_month = date('Y-m-01');
    $end_month = date('Y-m-t');
    $red_zone_res = $conn->query("
        SELECT u.full_name, COUNT(a.id) as abs_count
        FROM edu_users u
        JOIN edu_staff_attendance a ON u.id = a.staff_id
        WHERE u.institution_id = $inst_id AND u.role IN ('staff', 'teacher', 'admin')
          AND a.status = 'Absent' AND a.date BETWEEN '$start_month' AND '$end_month'
        GROUP BY u.id
        HAVING abs_count > $limit
        ORDER BY abs_count DESC
    ");
    $red_zone = [];
    if($red_zone_res) while($row = $red_zone_res->fetch_assoc()) $red_zone[] = $row;

    json_response([
        'stats' => [
            'total' => $total_staff,
            'present' => $present_staff,
            'absent' => $absent_staff,
            'leave' => $leave_staff,
            'unmarked' => max(0, $total_staff - ($present_staff + $absent_staff + $leave_staff)),
            'limit' => $limit
        ],
        'red_zone' => $red_zone
    ]);

} elseif ($action == 'save_school_policy') {
    $allowed_policy_roles = ['admin', 'super_admin', 'developer', 'principal', 'useradmin'];
    if (!in_array($role, $allowed_policy_roles)) json_response(['success' => false, 'error' => 'Permission Denied']);

    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data)) json_response(['success' => false, 'error' => 'Invalid JSON data received']);

    $limit = (int)($data['absence_limit'] ?? 0);
    $rate = (float)($data['deduction_rate'] ?? 0);
    $b_p = (float)($data['bonus_primary'] ?? 0);
    $b_m = (float)($data['bonus_middle'] ?? 0);
    $b_h = (float)($data['bonus_high'] ?? 0);
    $b_s = (float)($data['bonus_secondary'] ?? 0);
    $f_h = (float)($data['fine_hair_cut'] ?? 0);
    $f_r = (float)($data['fine_register'] ?? 0);
    $f_rb = (float)($data['fine_rules_break'] ?? 0);
    $f_a = (float)($data['fine_attendance'] ?? 0);

    $sql = "UPDATE edu_institutions SET 
            absence_limit = $limit, 
            absent_deduction_rate = $rate, 
            bonus_primary = $b_p, 
            bonus_middle = $b_m, 
            bonus_high = $b_h, 
            bonus_secondary = $b_s,
            fine_hair_cut = $f_h,
            fine_register = $f_r,
            fine_rules_break = $f_rb,
            fine_attendance = $f_a,
            staff_absent_allowance = $limit
            WHERE id = $inst_id";
    
    $res = $conn->query($sql);
    if ($res) {
        json_response(['success' => true]);
    } else {
        $err = $conn->error;
        if (strpos($err, "Unknown column") !== false) {
             json_response([
                'success' => false, 
                'error' => 'Database Schema Outdated', 
                'details' => 'One or more required columns are missing in your database. Please run the migration script.',
                'migration_url' => 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/migrate_attendance_v2.php'
            ]);
        }
        json_response(['success' => false, 'error' => $err]);
    }

} elseif ($action == 'get_policy_center') {
    // Check for column presence to decide which settings to return
    $check_i = $conn->query("SHOW COLUMNS FROM edu_institutions LIKE 'absence_limit'");
    if ($check_i && $check_i->num_rows > 0) {
        // Fetch all required columns, using fallback for those that might still be missing
        $cols = "absence_limit, absent_deduction_rate, bonus_primary, bonus_middle, bonus_high, bonus_secondary, fine_hair_cut, fine_register, fine_rules_break, fine_attendance";
        
        // Add staff_absent_allowance if it exists
        $check_staff = $conn->query("SHOW COLUMNS FROM edu_institutions LIKE 'staff_absent_allowance'");
        if ($check_staff && $check_staff->num_rows > 0) $cols .= ", staff_absent_allowance";
        
        $res_inst = $conn->query("SELECT $cols FROM edu_institutions WHERE id = $inst_id");
        $inst = ($res_inst && $res_inst->num_rows > 0) ? $res_inst->fetch_assoc() : [];
    } else {
        $inst = [
            'absence_limit' => 3, 'absent_deduction_rate' => 0, 
            'bonus_primary' => 5, 'bonus_middle' => 7, 'bonus_high' => 10, 'bonus_secondary' => 12, 
            'fine_hair_cut' => 0, 'fine_register' => 0, 'fine_rules_break' => 0, 'fine_attendance' => 0,
            'staff_absent_allowance' => 3
        ];
    }
    
    $check_c = $conn->query("SHOW COLUMNS FROM edu_classes LIKE 'bonus_amount'");
    $bonus_cols = ($check_c && $check_c->num_rows > 0) ? ", bonus_amount, is_bonus_active, level" : "";

    $classes_res = $conn->query("SELECT id, name $bonus_cols FROM edu_classes WHERE institution_id = $inst_id ORDER BY id ASC");
    $classes = [];
    if($classes_res && $classes_res->num_rows > 0) {
        while($row = $classes_res->fetch_assoc()) $classes[] = $row;
    }

    json_response([
        'financial' => [
            'absence_limit' => (int)(isset($inst['absence_limit']) ? $inst['absence_limit'] : 3),
            'deduction_rate' => (float)(isset($inst['absent_deduction_rate']) ? $inst['absent_deduction_rate'] : 0),
            'bonus_primary' => (float)(isset($inst['bonus_primary']) ? $inst['bonus_primary'] : 0),
            'bonus_middle' => (float)(isset($inst['bonus_middle']) ? $inst['bonus_middle'] : 0),
            'bonus_high' => (float)(isset($inst['bonus_high']) ? $inst['bonus_high'] : 0),
            'bonus_secondary' => (float)(isset($inst['bonus_secondary']) ? $inst['bonus_secondary'] : 0)
        ],
        'fines' => [
            'hair_cut' => (float)(isset($inst['fine_hair_cut']) ? $inst['fine_hair_cut'] : 0),
            'register' => (float)(isset($inst['fine_register']) ? $inst['fine_register'] : 0),
            'rules_break' => (float)(isset($inst['fine_rules_break']) ? $inst['fine_rules_break'] : 0),
            'attendance' => (float)(isset($inst['fine_attendance']) ? $inst['fine_attendance'] : 0)
        ],
        'classes' => $classes
    ]);

} elseif ($action == 'sync_roll_numbers') {
    if (!in_array($role, $allowed_roles)) json_response(['success' => false, 'error' => 'Permission Denied']);
    $cid = (int)$_GET['class_id'];
    $sid = (int)$_GET['section_id'];

    // Fetch active students ordered by their current Class No or Roll Number
    $res = $conn->query("SELECT id FROM edu_student_enrollment WHERE class_id = $cid AND section_id = $sid AND status = 'active' ORDER BY CAST(class_no AS UNSIGNED) ASC, roll_number ASC");
    
    $i = 1;
    $updated = 0;
    while($row = $res->fetch_assoc()) {
        $conn->query("UPDATE edu_student_enrollment SET class_no = '$i' WHERE id = " . $row['id']);
        $updated++;
        $i++;
    }
    json_response(['success' => true, 'processed' => $updated]);

} elseif ($action == 'get_staff_no_salary') {
    if (!in_array($role, $allowed_roles)) json_response(['success' => false, 'error' => 'Permission Denied']);
    
    // Fetch staff members with no salary or salary = 0
    $query = "SELECT id, full_name, role FROM edu_users 
              WHERE institution_id = $inst_id 
              AND role IN ('staff', 'teacher') 
              AND (salary IS NULL OR salary = 0 OR salary = '')
              ORDER BY full_name ASC";
    
    $res = $conn->query($query);
    $staff = [];
    if($res) {
        while($row = $res->fetch_assoc()) {
            $staff[] = $row;
        }
    }
    json_response($staff);

} elseif ($action == 'set_staff_salary') {
    if (!in_array($role, $allowed_roles)) json_response(['success' => false, 'error' => 'Permission Denied']);
    
    $data = json_decode(file_get_contents('php://input'), true);
    $staff_id = (int)$data['staff_id'];
    $salary = (float)$data['salary'];
    
    if($staff_id <= 0 || $salary <= 0) {
        json_response(['success' => false, 'error' => 'Invalid staff ID or salary amount']);
    }
    
    // Verify staff belongs to this institution
    $check = $conn->query("SELECT id FROM edu_users WHERE id = $staff_id AND institution_id = $inst_id");
    if(!$check || $check->num_rows == 0) {
        json_response(['success' => false, 'error' => 'Staff member not found']);
    }
    
    // Update salary
    $update = $conn->query("UPDATE edu_users SET salary = $salary WHERE id = $staff_id");
    
    if($update) {
        json_response(['success' => true, 'message' => 'Salary updated successfully']);
    } else {
        json_response(['success' => false, 'error' => 'Database error: ' . $conn->error]);
    }
} elseif ($action == 'delete_attendance') {
    if (!in_array($role, $allowed_roles) && $role !== 'staff') { http_response_code(403); exit; }
    $type = $_GET['type'] ?? 'student'; // 'student' or 'staff'
    $id = (int)($_GET['id'] ?? 0);
    $date = $_GET['date'] ?? '';

    if ($id <= 0 || empty($date)) {
        json_response(['success' => false, 'error' => 'Invalid parameters']);
    }

    if ($type === 'student') {
        $sql = "DELETE FROM edu_attendance WHERE student_id = $id AND date = '$date'";
    } else {
        $sql = "DELETE FROM edu_staff_attendance WHERE staff_id = $id AND date = '$date'";
    }

    if ($conn->query($sql)) {
        json_response(['success' => true]);
    } else {
        json_response(['success' => false, 'error' => $conn->error]);
    }

} elseif ($action == 'get_single_entry') {
    $type = $_GET['type'] ?? '';
    $id = (int)($_GET['id'] ?? 0);
    $date = $_GET['date'] ?? date('Y-m-d');
    
    if (!$id) json_response(['error' => 'Invalid ID']);
    
    if ($type === 'student') {
        $res = $conn->query("SELECT status, time_in, time_out FROM edu_attendance WHERE student_id = $id AND date = '$date'")->fetch_assoc();
    } else {
        $res = $conn->query("SELECT status, time_in, time_out FROM edu_staff_attendance WHERE staff_id = $id AND date = '$date'")->fetch_assoc();
    }
    $user = $conn->query("SELECT full_name as name FROM edu_users WHERE id = $id")->fetch_assoc();
    
    json_response([
        'success' => true,
        'name' => $user['name'] ?? 'Unknown',
        'status' => $res['status'] ?? '',
        'time_in' => $res['time_in'] ?? '',
        'time_out' => $res['time_out'] ?? ''
    ]);

} elseif ($action == 'save_single_entry') {
    if (!in_array($role, $allowed_roles)) json_response(['error' => 'Unauthorized']);
    
    $data = json_decode(file_get_contents('php://input'), true);
    $type = $data['type'] ?? '';
    $id = (int)($data['id'] ?? 0);
    $date = $data['date'] ?? '';
    $status = $conn->real_escape_string($data['status'] ?? '');
    $t_in = !empty($data['time_in']) ? "'".$conn->real_escape_string($data['time_in'])."'" : "NULL";
    $t_out = !empty($data['time_out']) ? "'".$conn->real_escape_string($data['time_out'])."'" : "NULL";
    
    if (!$id || !$date || !$status) json_response(['error' => 'Missing required fields']);

    if ($type === 'student') {
        $enroll = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $id")->fetch_assoc();
        $cid = (int)($enroll['class_id'] ?? 0);
        $sid = (int)($enroll['section_id'] ?? 0);
        
        $sql = "INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status, time_in, time_out) 
                VALUES ($inst_id, $id, $cid, $sid, '$date', '$status', $t_in, $t_out)
                ON DUPLICATE KEY UPDATE status = '$status', time_in = $t_in, time_out = $t_out";
    } else {
        $sql = "INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status, time_in, time_out) 
                VALUES ($inst_id, $id, '$date', '$status', $t_in, $t_out)
                ON DUPLICATE KEY UPDATE status = '$status', time_in = $t_in, time_out = $t_out";
    }
    
    if ($conn->query($sql)) {
        if ($type === 'student') {
            require_once '../../includes/FeeManager.php';
            $fm = new FeeManager($conn, $inst_id);
            $fm->syncAttendanceFines($id, date('F', strtotime($date)), (int)date('Y', strtotime($date)));
        }
        json_response(['success' => true]);
    } else {
        json_response(['success' => false, 'error' => $conn->error]);
    }

} elseif ($action == 'get_classes') {
    $res = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY name");
    $data = [];
    while($r = $res->fetch_assoc()) $data[] = $r;
    json_response($data);

} elseif ($action == 'get_sections') {
    $class_id = (int)($_GET['class'] ?? 0);
    if (!$class_id) json_response([]);
    
    $res = $conn->query("SELECT id, name as section FROM edu_sections WHERE class_id = $class_id ORDER BY name");
    if (!$res) {
        // Log SQL error if needed or return error object
        json_response(['error' => 'Database Query Failed', 'sql_error' => $conn->error]);
    }
    
    $data = [];
    while($r = $res->fetch_assoc()) $data[] = $r;
    json_response($data);

} elseif ($action == 'get_enroll_users') {
    $type = $_GET['type'] ?? 'student';
    $inst_id = (int)$inst_id;

    if ($type === 'student') {
        $class_id = (int)($_GET['class'] ?? 0);
        $section_id = (int)($_GET['section'] ?? 0);
        
        if (!$class_id) json_response([]);

        $sql = "SELECT DISTINCT u.id, u.full_name as name 
                FROM edu_users u
                JOIN edu_student_enrollment e ON u.id = e.student_id
                WHERE u.institution_id = $inst_id 
                  AND (u.role = 'student' OR u.role = 'Student')
                  AND e.class_id = $class_id
                  AND e.status = 'active'";
        if ($section_id) $sql .= " AND e.section_id = $section_id";
        $sql .= " ORDER BY u.full_name";
    } else {
        $cat = $conn->real_escape_string($_GET['category'] ?? '');
        $sql = "SELECT id, full_name as name 
                FROM edu_users 
                WHERE institution_id = $inst_id 
                  AND (role = 'staff' OR role = 'Staff' OR role = 'driver' OR role = 'teacher')";
        
        if ($cat === 'Teaching') $sql .= " AND (designation LIKE '%Teacher%' OR designation LIKE '%Professor%' OR designation LIKE '%Lecturer%')";
        if ($cat === 'Non-Teaching') $sql .= " AND (designation NOT LIKE '%Teacher%' AND designation NOT LIKE '%Professor%' AND designation NOT LIKE '%Lecturer%')";
        
        $sql .= " ORDER BY full_name";
    }

    $res = $conn->query($sql);
    $data = [];
    if ($res) {
        while($r = $res->fetch_assoc()) $data[] = $r;
    }
    json_response($data);

} elseif ($action == 'bulk_face_enroll_single') {
    $data = json_decode(file_get_contents('php://input'), true);
    $class_id = (int)($data['class_id'] ?? 0);
    $section_id = (int)($data['section_id'] ?? 0);
    $class_no = $conn->real_escape_string($data['class_no'] ?? '');
    $image_data = $data['image'] ?? '';

    if (!$class_id || $class_no === '' || !$image_data) {
        json_response(['success' => false, 'error' => 'Incomplete data for Class No: ' . $class_no]);
    }

    // Resolve class_no to student_id
    $where = "class_id = $class_id AND (class_no = '$class_no' OR class_no = '".(int)$class_no."') AND status = 'active'";
    if ($section_id > 0) $where .= " AND section_id = $section_id";
    
    $sql = "SELECT student_id FROM edu_student_enrollment WHERE $where LIMIT 1";
    $q = $conn->query($sql);
    
    if (!$q || $q->num_rows === 0) {
        json_response(['success' => false, 'error' => "Student not found for Class No $class_no"]);
    }

    $user_id = $q->fetch_assoc()['student_id'];

    // Process Base64 image
    $image_parts = explode(";base64,", $image_data);
    if (count($image_parts) < 2) json_response(['success' => false, 'error' => 'Invalid image format']);
    
    $image_type_aux = explode("image/", $image_parts[0]);
    $image_type = isset($image_type_aux[1]) ? $image_type_aux[1] : 'jpg';
    $image_base64 = base64_decode($image_parts[1]);

    $dir = '../../assets/uploads/';
    if (!file_exists($dir)) @mkdir($dir, 0777, true);

    $filename = 'user_' . $user_id . '_' . time() . '.' . $image_type;
    $file_path = $dir . $filename;

    if (file_put_contents($file_path, $image_base64)) {
        $sql = "UPDATE edu_users SET profile_pic = '$filename' WHERE id = $user_id";
        if ($conn->query($sql)) {
            // Sync to global users as well if applicable
            $u = $conn->query("SELECT username FROM edu_users WHERE id = $user_id")->fetch_assoc();
            if ($u) {
                $uname = $conn->real_escape_string($u['username']);
                $conn->query("UPDATE users SET profile_pic = '$filename' WHERE username = '$uname'");
            }
            json_response(['success' => true, 'user_id' => $user_id]);
        } else {
            @unlink($file_path);
            json_response(['success' => false, 'error' => 'Database update failed']);
        }
    } else {
        json_response(['success' => false, 'error' => 'File save failed']);
    }

} elseif ($action == 'save_face_enrollment') {
    $data = json_decode(file_get_contents('php://input'), true);
    $user_id = (int)($data['user_id'] ?? 0);
    $image_data = $data['image'] ?? ''; 

    if (!$user_id || !$image_data) json_response(['success' => false, 'error' => 'Incomplete data']);

    $image_parts = explode(";base64,", $image_data);
    $image_type_aux = explode("image/", $image_parts[0]);
    $image_type = isset($image_type_aux[1]) ? $image_type_aux[1] : 'jpg';
    $image_base64 = base64_decode($image_parts[1]);

    // Use unified assets/uploads for compatibility with profile.php and fixed get_registered_faces.php
    $dir = '../../assets/uploads/';
    if (!file_exists($dir)) @mkdir($dir, 0777, true);

    $filename = 'user_' . $user_id . '_' . time() . '.' . $image_type;
    $file_path = $dir . $filename;

    if (file_put_contents($file_path, $image_base64)) {
        $sql = "UPDATE edu_users SET profile_pic = '$filename' WHERE id = $user_id";
        if ($conn->query($sql)) {
            // Sync to global users as well if applicable
            $u = $conn->query("SELECT username FROM edu_users WHERE id = $user_id")->fetch_assoc();
            if ($u) {
                $uname = $conn->real_escape_string($u['username']);
                $conn->query("UPDATE users SET profile_pic = '$filename' WHERE username = '$uname'");
            }
            json_response(['success' => true, 'filename' => $filename]);
        } else {
            @unlink($file_path);
            json_response(['success' => false, 'error' => 'Database update failed']);
        }
    } else {
        json_response(['success' => false, 'error' => 'Failed to save image file']);
    }

} elseif ($action == 'enroll_existing_profile') {
    $data = json_decode(file_get_contents('php://input'), true);
    $user_id = (int)($data['user_id'] ?? 0);
    
    if (!$user_id) json_response(['success' => false, 'error' => 'No user selected']);

    $q = $conn->query("SELECT profile_pic FROM edu_users WHERE id = $user_id");
    if (!$q || $q->num_rows === 0) json_response(['success' => false, 'error' => 'User not found']);
    
    $row = $q->fetch_assoc();
    $pic = $row['profile_pic'];

    if (empty($pic)) json_response(['success' => false, 'error' => 'No existing profile picture found']);
    if ($pic === 'default.png') json_response(['success' => false, 'error' => 'User has default placeholder image']);

    // Check multiple potential locations for the profile picture
    $possible_paths = [
        '../../assets/uploads/' . $pic,
        '../../uploads/profiles/' . $pic,
        '../../assets/' . $pic,
        '../../' . $pic
    ];
    
    // Also handle cases where 'uploads/' prefix is already in DB
    if (strpos($pic, 'uploads/') === 0) {
        $possible_paths[] = '../../assets/' . $pic;
        $possible_paths[] = '../../' . $pic;
    }

    $final_path = '';
    foreach ($possible_paths as $p) {
        if (file_exists($p)) {
            $final_path = $p;
            break;
        }
    }

    if (empty($final_path)) {
        json_response(['success' => false, 'error' => 'Image file missing from server storage. Filename: ' . $pic]);
    }

    // Success - user has a valid profile picture that get_registered_faces.php can now read
    json_response(['success' => true]);

} elseif ($action == 'get_fingerprint_templates') {
    // Fetch all fingerprint templates associated with active users for the current institution
    $sql = "SELECT f.user_id, f.fingerprint_template, f.finger_position, u.full_name as name,
                   u.role, COALESCE(u.designation, '') as designation,
                   COALESCE(u.assigned_class_id, 0) as class_id,
                   COALESCE(u.assigned_section_id, 0) as section_id
            FROM edu_fingerprints f
            JOIN edu_users u ON f.user_id = u.id
            WHERE u.institution_id = $inst_id";
    
    // Optional role filter
    $fp_role = $_GET['fp_role'] ?? '';
    $fp_staff_type = $_GET['fp_staff_type'] ?? '';
    $fp_class_id = (int)($_GET['fp_class_id'] ?? 0);
    $fp_section_id = (int)($_GET['fp_section_id'] ?? 0);

    if ($fp_role === 'student') {
        $sql .= " AND (u.role = 'student' OR u.role = 'Student')";
        if ($fp_class_id > 0) {
            $sql .= " AND EXISTS (SELECT 1 FROM edu_student_enrollment e WHERE e.student_id = u.id AND e.class_id = $fp_class_id AND e.status = 'active'";
            if ($fp_section_id > 0) $sql .= " AND e.section_id = $fp_section_id";
            $sql .= ")";
        }
    } elseif ($fp_role === 'staff') {
        $sql .= " AND u.role IN ('staff', 'teacher', 'admin', 'principal', 'head')";
        if ($fp_staff_type === 'Teaching') {
            $sql .= " AND (u.user_type = 'teaching' OR u.designation LIKE '%Teacher%' OR u.designation LIKE '%Professor%' OR u.designation LIKE '%Lecturer%' OR u.designation LIKE '%Principal%' OR u.designation LIKE '%Head%' OR u.designation LIKE '%Instructor%')";
        } elseif ($fp_staff_type === 'Non-Teaching') {
            $sql .= " AND (u.user_type = 'non-teaching' OR (u.user_type != 'teaching' AND u.designation NOT LIKE '%Teacher%' AND u.designation NOT LIKE '%Professor%' AND u.designation NOT LIKE '%Lecturer%' AND u.designation NOT LIKE '%Principal%' AND u.designation NOT LIKE '%Head%' AND u.designation NOT LIKE '%Instructor%'))";
        }
    }

    $res = $conn->query($sql);
    $data = [];
    if ($res) {
        while($r = $res->fetch_assoc()) {
            unset($r['fingerprint_template']); // Don't send raw template data unnecessarily
            $data[] = $r;
        }
    }
    // Re-run with template for SDK use (if needed by original callers not passing fp_role)
    if ($fp_role === '') {
        $res2 = $conn->query("SELECT f.user_id, f.fingerprint_template, f.finger_position, u.full_name as name, u.role, COALESCE(u.designation,'') as designation FROM edu_fingerprints f JOIN edu_users u ON f.user_id = u.id WHERE u.institution_id = $inst_id");
        $data = [];
        if ($res2) { while($r = $res2->fetch_assoc()) $data[] = $r; }
    }
    json_response($data);

} elseif ($action == 'save_fingerprint_enrollment') {
    $data = json_decode(file_get_contents('php://input'), true);
    $user_id = (int)($data['user_id'] ?? 0);
    $template = $conn->real_escape_string($data['template'] ?? '');
    $position = $conn->real_escape_string($data['position'] ?? 'Right Index');
    $quality = (int)($data['quality'] ?? 0);

    if (!$user_id || !$template) {
        json_response(['success' => false, 'error' => 'Incomplete data']);
    }

    // Upsert fingerprint template
    $check = $conn->query("SELECT id FROM edu_fingerprints WHERE user_id = $user_id AND finger_position = '$position'");
    if ($check && $check->num_rows > 0) {
        $id = $check->fetch_assoc()['id'];
        $sql = "UPDATE edu_fingerprints SET fingerprint_template = '$template', quality_score = $quality WHERE id = $id";
    } else {
        $sql = "INSERT INTO edu_fingerprints (user_id, finger_position, fingerprint_template, quality_score) 
                VALUES ($user_id, '$position', '$template', $quality)";
    }

    if ($conn->query($sql)) {
        json_response(['success' => true]);
    } else {
        json_response(['success' => false, 'error' => 'Database update failed: ' . $conn->error]);
    }
} elseif ($action == 'mark_smart_attendance') {
    $data = json_decode(file_get_contents('php://input'), true);
    $user_id = (int)($data['user_id'] ?? 0);
    $type = $conn->real_escape_string($data['type'] ?? 'face'); // face or fingerprint
    $score = (float)($data['score'] ?? 0);
    $mode = $conn->real_escape_string($data['mode'] ?? 'check_in'); // check_in, check_out, or auto (client resolved)

    // RELEASE SESSION LOCK (Crucial for performance/preventing 'hangs')
    if (session_status() === PHP_SESSION_ACTIVE) {
        session_write_close();
    }

    
    $date = date('Y-m-d');
    $time = date('H:i:s');

    if (!$user_id) json_response(['success' => false, 'error' => 'Invalid user ID']);

    // Check Columns Existence (Migration)
    $tbls = ['edu_attendance', 'edu_staff_attendance'];
    foreach($tbls as $tbl) {
        $cols = ['time_in', 'time_out', 'source'];
        foreach($cols as $col) {
            $check = $conn->query("SHOW COLUMNS FROM $tbl LIKE '$col'");
            if(!$check || $check->num_rows == 0) {
                $type_def = ($col == 'source') ? "VARCHAR(50)" : "TIME";
                $conn->query("ALTER TABLE $tbl ADD COLUMN $col $type_def NULL");
            }
        }
    }

    // Fetch user details and role
    $user_q = $conn->query("SELECT id, role, institution_id, full_name, profile_pic FROM edu_users WHERE id = $user_id");
    if (!$user_q || $user_q->num_rows == 0) json_response(['success' => false, 'error' => 'User not found']);
    
    $user = $user_q->fetch_assoc();
    $u_role = $user['role'];
    $u_name = $user['full_name'];
    $u_pic = $user['profile_pic'];

    $success = false;
    
    // Logic for Update
    // If Mode is Check-In: Set time_in if null (or update?). Standard is First In.
    // If Mode is Check-Out: Set time_out.
    
    $update_logic = "";
    $insert_cols = "time_in, source"; 
    $insert_vals = "'$time', '$type'";
    
    if ($mode === 'check_out') {
        // If checking out, we ideally want to UPDATE existing record for today.
        // If no record exists, insert with time_out (and null time_in?) or treat as time_in?
        // Let's assume Insert if not exists, but set time_out.
        $insert_cols = "time_out, source";
        $insert_vals = "'$time', '$type'";
        $update_logic = "time_out = '$time'";
    } else {
        // Check In
        $update_logic = "time_in = IF(time_in IS NULL, '$time', time_in)";
    }

    // Explicit DB Operations
    if ($u_role == 'student') {
        // Find enrollment
        $en_q = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $user_id AND status = 'active' LIMIT 1");
        if ($en_q && $en_q->num_rows > 0) {
            $en = $en_q->fetch_assoc();
            $cid = $en['class_id'];
            $sid = $en['section_id'];
            
            $sql = "INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status, $insert_cols) 
                    VALUES ($inst_id, $user_id, $cid, $sid, '$date', 'Present', $insert_vals)
                    ON DUPLICATE KEY UPDATE status = 'Present', $update_logic";
            $success = $conn->query($sql);
        }
    } else {
        // Staff/Admin
        $sql = "INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status, $insert_cols) 
                VALUES ($inst_id, $user_id, '$date', 'Present', $insert_vals)
                ON DUPLICATE KEY UPDATE status = 'Present', $update_logic";
        $success = $conn->query($sql);
    }

    if ($success) {
        if ($u_role == 'student') {
            require_once '../../includes/FeeManager.php';
            $fm = new FeeManager($conn, $inst_id);
            $fm->syncAttendanceFines($user_id, date('F', strtotime($date)), (int)date('Y', strtotime($date)));
        }
        // Log the verification attempt for audit
        $conn->query("INSERT INTO edu_fingerprints_logs (institution_id, user_id, status, confidence_score, method, ip_address) 
                      VALUES ($inst_id, $user_id, 'Success', $score, '$type', '" . $_SERVER['REMOTE_ADDR'] . "')");
        
        json_response([
            'success' => true, 
            'user' => ['name' => $u_name, 'role' => $u_role, 'photo' => $u_pic],
            'mode' => $mode,
            'time' => date('h:i A')
        ]);
    } else {
        json_response(['success' => false, 'error' => 'Database update failed: ' . $conn->error]);
    }

} elseif ($action == 'get_smart_settings') {
    // Check for new columns existence to avoid breaking on older DBs
    $cols = "att_smart_face_active, att_smart_finger_active, att_smart_face_ip, 
             att_smart_face_user, att_smart_face_pass, att_smart_auto_mark, att_smart_sound";
    
    $check = $conn->query("SHOW COLUMNS FROM edu_institutions LIKE 'att_smart_mode'");
    if ($check && $check->num_rows > 0) {
        $cols .= ", att_smart_mode, att_smart_in_start, att_smart_in_end, att_smart_out_start, att_smart_out_end";
    }

    $sql = "SELECT $cols FROM edu_institutions WHERE id = $inst_id";
    $res = $conn->query($sql);
    if ($res && $res->num_rows > 0) {
        json_response($res->fetch_assoc());
    } else {
        json_response(['error' => 'Settings not found']);
    }

} elseif ($action == 'get_attendance_report_summary') {
    try {
        $date = $_GET['date'] ?? date('Y-m-d');
        
        // Defensive Check: Ensure 'status' column exists before using it
    $check_col = $conn->query("SHOW COLUMNS FROM edu_student_enrollment LIKE 'status'");
    $has_status = ($check_col && $check_col->num_rows > 0);
    $status_sql = $has_status ? " AND e.status = 'active'" : "";

    // 1. Students Summary Grouped by Class/Section
    $student_summary = [];
    $classes_res = $conn->query("SELECT c.id as cid, c.name as cname, s.id as sid, s.name as sname 
                                 FROM edu_classes c 
                                 JOIN edu_sections s ON c.id = s.class_id 
                                 WHERE c.institution_id = $inst_id ORDER BY c.name, s.name");
    
    if ($classes_res) {
        while ($row = $classes_res->fetch_assoc()) {
            $cid = (int)$row['cid'];
            $sid = (int)$row['sid'];
            
            // Total Students breakdown by gender
            $total_m_q = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE e.class_id = $cid AND e.section_id = $sid $status_sql AND u.gender = 'male'");
            $total_m = ($total_m_q) ? (int)$total_m_q->fetch_row()[0] : 0;
            $total_f_q = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE e.class_id = $cid AND e.section_id = $sid $status_sql AND u.gender = 'female'");
            $total_f = ($total_f_q) ? (int)$total_f_q->fetch_row()[0] : 0;
            $total = $total_m + $total_f;
            
            // Stats breakdown by gender
            $stats_q = $conn->query("SELECT 
                                        SUM(IF(a.status = 'Present' AND u.gender = 'male', 1, 0)) as p_m,
                                        SUM(IF(a.status = 'Present' AND u.gender = 'female', 1, 0)) as p_f,
                                        SUM(IF(a.status = 'Leave' AND u.gender = 'male', 1, 0)) as l_m,
                                        SUM(IF(a.status = 'Leave' AND u.gender = 'female', 1, 0)) as l_f,
                                        SUM(IF(a.status = 'Absent' AND u.gender = 'male', 1, 0)) as a_m,
                                        SUM(IF(a.status = 'Absent' AND u.gender = 'female', 1, 0)) as a_f
                                     FROM edu_attendance a
                                     JOIN edu_users u ON a.student_id = u.id
                                     WHERE a.class_id = $cid AND a.section_id = $sid AND a.date = '$date'");
            $stats = ($stats_q) ? $stats_q->fetch_assoc() : [];
            
            $student_summary[] = [
                'class_id' => $cid,
                'section_id' => $sid,
                'class_name' => $row['cname'],
                'section_name' => $row['sname'],
                'total_m' => $total_m,
                'total_f' => $total_f,
                'present_m' => (int)($stats['p_m'] ?? 0),
                'present_f' => (int)($stats['p_f'] ?? 0),
                'leave_m' => (int)($stats['l_m'] ?? 0),
                'leave_f' => (int)($stats['l_f'] ?? 0),
                'absent_m' => (int)($stats['a_m'] ?? 0),
                'absent_f' => (int)($stats['a_f'] ?? 0)
            ];
        }
    }
    
    // 2. Staff Summary Grouped by User Type
    $staff_summary = [];
    $types = ['teaching' => 'Teaching Staff', 'non-teaching' => 'Non-Teaching', 'visiting' => 'Visiting Staff'];
    
    // Check user status column
    $check_u = $conn->query("SHOW COLUMNS FROM edu_users LIKE 'status'");
    $has_u_status = ($check_u && $check_u->num_rows > 0);
    $u_status_sql = $has_u_status ? " AND u.status = 'active'" : "";

    // Check staff attendance status (assuming it exists as it's core table)
    
    foreach ($types as $key => $label) {
        // Total Staff
        $total_q = $conn->query("SELECT COUNT(*) FROM edu_users u WHERE u.institution_id = $inst_id AND u.role IN ('staff', 'teacher', 'admin', 'principal', 'head') AND u.user_type = '$key' $u_status_sql");
        $total = ($total_q) ? $total_q->fetch_row()[0] : 0;
        
        // Stats
        $stats_q = $conn->query("SELECT 
                                    SUM(IF(sa.status = 'Present', 1, 0)) as p,
                                    SUM(IF(sa.status = 'Leave', 1, 0)) as l,
                                    SUM(IF(sa.status = 'Absent', 1, 0)) as a
                                 FROM edu_users u
                                 LEFT JOIN edu_staff_attendance sa ON u.id = sa.staff_id AND sa.date = '$date'
                                 WHERE u.institution_id = $inst_id AND u.role IN ('staff', 'teacher', 'admin', 'principal', 'head') AND u.user_type = '$key' $u_status_sql");
        $stats = ($stats_q) ? $stats_q->fetch_assoc() : [];
        if(!$stats) $stats = ['p' => 0, 'l' => 0, 'a' => 0]; // Ensure array structure
        
        $p = (int)($stats['p'] ?? 0);
        $l = (int)($stats['l'] ?? 0);
        $a = (int)($stats['a'] ?? 0);
        
        $staff_summary[] = [
            'type' => $key,
            'label' => $label,
            'total' => $total,
            'present' => $p,
            'leave' => $l,
            'absent' => $a
        ];
    }
    
        
        json_response(['students' => $student_summary, 'staff' => $staff_summary]);

    } catch (Exception $e) {
        json_response(['success' => false, 'error' => $e->getMessage()]);
    }

} elseif ($action == 'get_staff_summary') {
    $month = (int)($_GET['month'] ?? date('m'));
    $year = (int)($_GET['year'] ?? date('Y'));
    $staff_type = $conn->real_escape_string($_GET['staff_type'] ?? 'all');
    
    $where = "u.institution_id = $inst_id AND u.role IN ('staff', 'teacher', 'admin', 'principal', 'head')";
    if($staff_type !== 'all' && $staff_type !== 'personal') {
        $where .= " AND u.user_type = '$staff_type'";
    }

    $q = $conn->query("SELECT u.id, u.full_name as name, u.user_type as designation 
                       FROM edu_users u 
                       WHERE $where AND u.status = 'active'
                       ORDER BY u.full_name ASC");
    
    $staff_list = [];
    $sno = 1;
    while($row = $q->fetch_assoc()) {
        $sid = $row['id'];
        
        // Month Stats
        $stats = $conn->query("SELECT 
                                SUM(IF(status = 'Present', 1, 0)) as p_this,
                                SUM(IF(status = 'Leave', 1, 0)) as l_this,
                                SUM(IF(status = 'Absent', 1, 0)) as a_this
                               FROM edu_staff_attendance 
                               WHERE staff_id = $sid AND MONTH(date) = $month AND YEAR(date) = $year")->fetch_assoc();
        
        // YTD Stats
        $totals = $conn->query("SELECT 
                                SUM(IF(status = 'Leave', 1, 0)) as l_total,
                                SUM(IF(status = 'Absent', 1, 0)) as a_total
                               FROM edu_staff_attendance 
                               WHERE staff_id = $sid AND YEAR(date) = $year")->fetch_assoc();
                               
        $staff_list[] = [
            'sno' => $sno++,
            'name' => $row['name'],
            'designation' => $row['designation'] ?: 'Staff',
            'l_this' => (int)($stats['l_this'] ?? 0),
            'a_this' => (int)($stats['a_this'] ?? 0),
            'l_total' => (int)($totals['l_total'] ?? 0),
            'a_total' => (int)($totals['a_total'] ?? 0)
        ];
    }
    json_response(['success' => true, 'staff' => $staff_list]);

} elseif ($action == 'get_staff_attendance') {
    // Personal View
    $month = (int)($_GET['month'] ?? date('m'));
    $year = (int)($_GET['year'] ?? date('Y'));
    $uid = (int)($_SESSION['edu_user_id'] ?? 0);
    
    // For admin to view others, they can pass uid
    if(in_array($role, $allowed_roles) && isset($_GET['user_id'])) $uid = (int)$_GET['user_id'];

    if($uid <= 0) json_response(['success' => false, 'error' => 'Invalid User ID']);

    $q = $conn->query("SELECT date, status, time_in, time_out 
                       FROM edu_staff_attendance 
                       WHERE staff_id = $uid AND MONTH(date) = $month AND YEAR(date) = $year
                       ORDER BY date ASC");
    
    $records = [];
    while($row = $q->fetch_assoc()) {
        $row['name'] = date('D, d M', strtotime($row['date'])); // Using date as name for the chip
        $records[] = $row;
    }
    json_response($records);

} elseif ($action == 'delete_fingerprint') {
    $user_id = (int)($_GET['user_id'] ?? 0);
    $finger_pos = $conn->real_escape_string($_GET['finger_position'] ?? '');
    
    if (!$user_id) json_response(['success' => false, 'error' => 'Invalid User ID']);
    
    $res = $conn->query("DELETE FROM edu_fingerprints WHERE user_id = $user_id AND finger_position = '$finger_pos'");
    if ($res) {
        json_response(['success' => true, 'message' => 'Fingerprint template removed successfully']);
    } else {
        json_response(['success' => false, 'error' => $conn->error]);
    }

} elseif ($action == 'delete_all_fingerprints') {
    if (!in_array($role, $allowed_roles)) { http_response_code(403); exit; }
    
    // Delete all fingerprints for the current institution
    $sql = "DELETE f FROM edu_fingerprints f
            JOIN edu_users u ON f.user_id = u.id
            WHERE u.institution_id = $inst_id";
            
    if ($conn->query($sql)) {
        json_response(['success' => true, 'message' => 'All fingerprint templates for this institution have been removed.']);
    } else {
        json_response(['success' => false, 'error' => $conn->error]);
    }

} elseif ($action == 'delete_biometric') {
    $data = json_decode(file_get_contents('php://input'), true);
    $user_id = (int)($data['user_id'] ?? 0);
    $type = $conn->real_escape_string($data['type'] ?? '');
    $pos = $conn->real_escape_string($data['position'] ?? '');

    if (!$user_id || !$type) json_response(['success' => false, 'error' => 'Invalid Request']);

    if ($type === 'face') {
        $sql = "UPDATE edu_users SET profile_pic = NULL WHERE id = $user_id";
        $res = $conn->query($sql);
    } else {
        $where = "user_id = $user_id";
        if (!empty($pos)) $where .= " AND finger_position = '$pos'";
        $sql = "DELETE FROM edu_fingerprints WHERE $where";
        $res = $conn->query($sql);
    }

    if ($res) {
        json_response(['success' => true, 'message' => 'Biometric data removed']);
    } else {
        json_response(['success' => false, 'error' => $conn->error]);
    }

} elseif ($action == 'get_user_biometric_info') {
    $user_id = (int)($_GET['user_id'] ?? 0);
    if (!$user_id) json_response(['success' => false, 'error' => 'Invalid User ID']);

    // 1. Check Face (Profile Pic)
    $u_q = $conn->query("SELECT profile_pic FROM edu_users WHERE id = $user_id");
    $u = $u_q->fetch_assoc();
    $has_face = (!empty($u['profile_pic']) && $u['profile_pic'] !== 'default.png');

    // 2. Check Fingerprints
    $f_q = $conn->query("SELECT finger_position, quality_score FROM edu_fingerprints WHERE user_id = $user_id");
    $fingerprints = [];
    if($f_q) {
        while($f = $f_q->fetch_assoc()) $fingerprints[] = $f;
    }

    json_response([
        'success' => true,
        'has_face' => $has_face,
        'fingerprints' => $fingerprints
    ]);

} elseif ($action == 'get_group_attendance_details') {
    $date = $_GET['date'] ?? date('Y-m-d');
    $type = $_GET['type']; // 'student' or 'staff'
    $res = [];
    
    if ($type === 'student') {
        $cid = (int)$_GET['class_id'];
        $sid = (int)$_GET['section_id'];
        $q = $conn->query("SELECT u.full_name as name, e.class_no, a.status 
                           FROM edu_student_enrollment e
                           JOIN edu_users u ON e.student_id = u.id
                           LEFT JOIN edu_attendance a ON u.id = a.student_id AND a.date = '$date'
                           WHERE e.class_id = $cid AND e.section_id = $sid AND e.status = 'active'
                           ORDER BY CAST(e.class_no AS UNSIGNED) ASC, e.class_no ASC");
        while ($row = $q->fetch_assoc()) $res[] = $row;
    } else {
        $user_type = $conn->real_escape_string($_GET['user_type']);
        $q = $conn->query("SELECT u.full_name as name, sa.status 
                           FROM edu_users u
                           LEFT JOIN edu_staff_attendance sa ON u.id = sa.staff_id AND sa.date = '$date'
                           WHERE u.institution_id = $inst_id AND u.role IN ('staff', 'teacher', 'admin', 'principal', 'head') AND u.user_type = '$user_type' AND u.status = 'active'
                           ORDER BY u.full_name ASC");
        while ($row = $q->fetch_assoc()) $res[] = $row;
    }
    
    json_response($res);

} elseif ($action == 'save_smart_settings') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    // Whitelist allowed columns to prevent SQL injection or accidents
    $allowed_cols = [
        'face_ip' => 'att_smart_face_ip',
        'face_user' => 'att_smart_face_user',
        'face_pass' => 'att_smart_face_pass',
        'auto_mark' => 'att_smart_auto_mark',
        'sound' => 'att_smart_sound',
        'face_active' => 'att_smart_face_active',
        'finger_active' => 'att_smart_finger_active',
        'att_mode' => 'att_smart_mode',
        'in_time_start' => 'att_smart_in_start',
        'in_time_end' => 'att_smart_in_end',
        'out_time_start' => 'att_smart_out_start',
        'out_time_end' => 'att_smart_out_end'
    ];

    $updates = [];
    foreach ($allowed_cols as $json_key => $db_col) {
        if (isset($data[$json_key])) {
            $val = $conn->real_escape_string($data[$json_key]);
            $updates[] = "$db_col = '$val'";
        }
    }

    if (empty($updates)) {
        json_response(['success' => true]); // Nothing to update
    }

    // Handle DB Schema Updates on the fly if needed
    $check = $conn->query("SHOW COLUMNS FROM edu_institutions LIKE 'att_smart_mode'");
    if (!$check || $check->num_rows == 0) {
        $conn->query("ALTER TABLE edu_institutions 
                      ADD COLUMN att_smart_mode VARCHAR(20) DEFAULT 'check_in_only',
                      ADD COLUMN att_smart_in_start VARCHAR(10) DEFAULT '07:00',
                      ADD COLUMN att_smart_in_end VARCHAR(10) DEFAULT '09:00',
                      ADD COLUMN att_smart_out_start VARCHAR(10) DEFAULT '13:00',
                      ADD COLUMN att_smart_out_end VARCHAR(10) DEFAULT '15:00',
                      ADD COLUMN att_smart_face_active TINYINT(1) DEFAULT 1,
                      ADD COLUMN att_smart_finger_active TINYINT(1) DEFAULT 1,
                      ADD COLUMN att_smart_auto_mark TINYINT(1) DEFAULT 1,
                      ADD COLUMN att_smart_sound TINYINT(1) DEFAULT 1,
                      ADD COLUMN att_smart_face_ip VARCHAR(100) DEFAULT '',
                      ADD COLUMN att_smart_face_user VARCHAR(100) DEFAULT '',
                      ADD COLUMN att_smart_face_pass VARCHAR(100) DEFAULT ''");
    } else {
        // Double check for individual columns that might have been missed in partial migrations
        $cols_to_check = ['att_smart_face_active', 'att_smart_finger_active', 'att_smart_auto_mark', 'att_smart_sound'];
        foreach($cols_to_check as $col) {
            $c = $conn->query("SHOW COLUMNS FROM edu_institutions LIKE '$col'");
            if(!$c || $c->num_rows == 0) {
                $conn->query("ALTER TABLE edu_institutions ADD COLUMN $col TINYINT(1) DEFAULT 1");
            }
        }
    }
 
    $sql = "UPDATE edu_institutions SET " . implode(', ', $updates) . " WHERE id = $inst_id";
    
    if ($conn->query($sql)) {
        json_response(['success' => true]);
    } else {
        json_response(['success' => false, 'error' => $conn->error]);
    }

} elseif ($action == 'get_attendance_summary_students') {
    $date = $_GET['date'] ?? date('Y-m-d');
    
    // Get all classes and sections with attendance counts
    $classes_q = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY name");
    $summary = [];
    
    while ($class = $classes_q->fetch_assoc()) {
        $class_id = $class['id'];
        $sections_q = $conn->query("SELECT id, name FROM edu_sections WHERE class_id = $class_id ORDER BY name");
        
        while ($section = $sections_q->fetch_assoc()) {
            $section_id = $section['id'];
            
            // Get total students
            $total_q = $conn->query("SELECT COUNT(*) as cnt FROM edu_student_enrollment WHERE class_id = $class_id AND section_id = $section_id AND status = 'active'");
            $total = $total_q->fetch_assoc()['cnt'];
            
            if ($total == 0) continue; // Skip empty classes
            
            // Get present count
            $present_q = $conn->query("SELECT COUNT(*) as cnt FROM edu_attendance WHERE class_id = $class_id AND section_id = $section_id AND date = '$date' AND status = 'Present'");
            $present = $present_q->fetch_assoc()['cnt'];
            
            // Get leave count
            $leave_q = $conn->query("SELECT COUNT(*) as cnt FROM edu_attendance WHERE class_id = $class_id AND section_id = $section_id AND date = '$date' AND status = 'Leave'");
            $leave = $leave_q->fetch_assoc()['cnt'];
            
            // Get absent count
            $absent_q = $conn->query("SELECT COUNT(*) as cnt FROM edu_attendance WHERE class_id = $class_id AND section_id = $section_id AND date = '$date' AND status = 'Absent'");
            $absent = $absent_q->fetch_assoc()['cnt'];
            
            $summary[] = [
                'class_id' => $class_id,
                'section_id' => $section_id,
                'class_name' => $class['name'],
                'section_name' => $section['name'],
                'total' => $total,
                'present' => $present,
                'leave' => $leave,
                'absent' => $absent
            ];
        }
    }
    
    json_response(['success' => true, 'data' => $summary]);

} elseif ($action == 'get_attendance_summary_staff') {
    $date = $_GET['date'] ?? date('Y-m-d');
    
    // Define staff categories
    $categories = [
        'Teaching' => ['teacher'],
        'Non-Teaching' => ['staff', 'admin', 'principal', 'head'],
        'Visiting' => ['visiting']
    ];
    
    $summary = [];
    
    foreach ($categories as $category_name => $roles) {
        $roles_str = "'" . implode("','", $roles) . "'";
        
        // Get total staff in this category
        $total_q = $conn->query("SELECT COUNT(*) as cnt FROM edu_users WHERE institution_id = $inst_id AND role IN ($roles_str)");
        $total = $total_q->fetch_assoc()['cnt'];
        
        if ($total == 0) continue; // Skip empty categories
        
        // Get present count
        $present_q = $conn->query("SELECT COUNT(DISTINCT sa.staff_id) as cnt FROM edu_staff_attendance sa JOIN edu_users u ON sa.staff_id = u.id WHERE sa.institution_id = $inst_id AND sa.date = '$date' AND sa.status = 'Present' AND u.role IN ($roles_str)");
        $present = $present_q->fetch_assoc()['cnt'];
        
        // Get leave count
        $leave_q = $conn->query("SELECT COUNT(DISTINCT sa.staff_id) as cnt FROM edu_staff_attendance sa JOIN edu_users u ON sa.staff_id = u.id WHERE sa.institution_id = $inst_id AND sa.date = '$date' AND sa.status = 'Leave' AND u.role IN ($roles_str)");
        $leave = $leave_q->fetch_assoc()['cnt'];
        
        // Get absent count
        $absent_q = $conn->query("SELECT COUNT(DISTINCT sa.staff_id) as cnt FROM edu_staff_attendance sa JOIN edu_users u ON sa.staff_id = u.id WHERE sa.institution_id = $inst_id AND sa.date = '$date' AND sa.status = 'Absent' AND u.role IN ($roles_str)");
        $absent = $absent_q->fetch_assoc()['cnt'];
        
        $summary[] = [
            'category' => $category_name,
            'roles' => $roles,
            'total' => $total,
            'present' => $present,
            'leave' => $leave,
            'absent' => $absent
        ];
    }
    
    json_response(['success' => true, 'data' => $summary]);

} elseif ($action == 'get_group_details') {
    $date = $_GET['date'] ?? date('Y-m-d');
    $type = $_GET['type'] ?? 'student'; // student or staff
    
    if ($type == 'student') {
        $class_id = (int)$_GET['class_id'];
        $section_id = (int)$_GET['section_id'];
        
        // Get all students with their attendance status
        $q = $conn->query("SELECT u.full_name as name, e.class_no, a.status 
                           FROM edu_student_enrollment e
                           JOIN edu_users u ON e.student_id = u.id
                           LEFT JOIN edu_attendance a ON u.id = a.student_id AND a.date = '$date'
                           WHERE e.class_id = $class_id AND e.section_id = $section_id AND e.status = 'active'
                           ORDER BY CAST(e.class_no AS UNSIGNED) ASC, e.class_no ASC");
        
        $students = [];
        while ($row = $q->fetch_assoc()) {
            $students[] = [
                'name' => $row['name'],
                'class_no' => $row['class_no'],
                'status' => $row['status'] ?? 'Not Marked'
            ];
        }
        
        json_response(['success' => true, 'data' => $students]);
        
    } else {
        $category = $_GET['category'] ?? 'Teaching';
        $roles_map = [
            'Teaching' => ['teacher'],
            'Non-Teaching' => ['staff', 'admin', 'principal', 'head'],
            'Visiting' => ['visiting']
        ];
        
        $roles = $roles_map[$category] ?? ['teacher'];
        $roles_str = "'" . implode("','", $roles) . "'";
        
        // Get all staff with their attendance status
        $q = $conn->query("SELECT u.full_name as name, sa.status 
                           FROM edu_users u
                           LEFT JOIN edu_staff_attendance sa ON u.id = sa.staff_id AND sa.date = '$date'
                           WHERE u.institution_id = $inst_id AND u.role IN ($roles_str)
                           ORDER BY u.full_name ASC");
        
        $staff = [];
        while ($row = $q->fetch_assoc()) {
            $staff[] = [
                'name' => $row['name'],
                'status' => $row['status'] ?? 'Not Marked'
            ];
        }
        
        json_response(['success' => true, 'data' => $staff]);
    }
} elseif ($action == 'get_personal_staff_records') {
    $month = date('m');
    $year = date('Y');
    
    $q = $conn->query("SELECT date, status, extra_classes 
                       FROM edu_staff_attendance 
                       WHERE staff_id = $user_id 
                       AND MONTH(date) = $month AND YEAR(date) = $year
                       ORDER BY date DESC");
                       
    $records = [];
    while($row = $q->fetch_assoc()) {
        $row['display_date'] = date('d M, Y', strtotime($row['date']));
        $records[] = $row;
    }
    json_response($records);
} elseif ($action == 'bulk_mark_present') {
    if (!in_array($role, $allowed_roles)) { http_response_code(403); exit; }
    $date_from = $conn->real_escape_string($_GET['date_from']);
    $date_to = $conn->real_escape_string($_GET['date_to']);
    $selection = $conn->real_escape_string($_GET['class_selection']); // 'all' or 'cid|sid'
    
    $where = "e.status = 'active' AND u.institution_id = $inst_id";
    if ($selection !== 'all') {
        $parts = explode('|', $selection);
        if (count($parts) === 2) {
            $cid = (int)$parts[0]; $sid = (int)$parts[1];
            $where .= " AND e.class_id = $cid AND e.section_id = $sid";
        }
    }

    $students = $conn->query("SELECT e.student_id, e.class_id, e.section_id FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE $where");
    $processed = 0;
    
    if ($students && $students->num_rows > 0) {
        $conn->begin_transaction();
        try {
            $student_data = [];
            while($s = $students->fetch_assoc()) $student_data[] = $s;

            $current_date = $date_from;
            while (strtotime($current_date) <= strtotime($date_to)) {
                if (date('w', strtotime($current_date)) != 0) { // Skip Sundays
                    foreach ($student_data as $s) {
                        $std_id = $s['student_id'];
                        $cid = $s['class_id'];
                        $sid = $s['section_id'];
                        $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status) 
                                      VALUES ($inst_id, $std_id, $cid, $sid, '$current_date', 'Present')
                                      ON DUPLICATE KEY UPDATE status = 'Present'");
                        $processed++;
                    }
                }
                $current_date = date('Y-m-d', strtotime($current_date . ' +1 day'));
            }
            $conn->commit();
            json_response(['success' => true, 'processed' => $processed]);
        } catch (Exception $e) {
            $conn->rollback();
            json_response(['success' => false, 'error' => $e->getMessage()]);
        }
    } else {
        json_response(['success' => false, 'error' => 'No active students found in selected scope']);
    }
} elseif ($action === 'export_attendance_horizontal_csv') {
    if (!in_array($role, $allowed_roles)) { http_response_code(403); exit; }
    $from = $_GET['date_from'] ?? date('Y-m-d');
    $to = $_GET['date_to'] ?? date('Y-m-d');
    $selection = $_GET['class_selection'] ?? 'all';

    $filename_suffix = "all_classes";
    $where = "e.status = 'active' AND u.institution_id = $inst_id";
    if ($selection !== 'all') {
        $parts = explode('|', $selection);
        if (count($parts) === 2) {
            $cid = (int)$parts[0]; $sid = (int)$parts[1];
            $where .= " AND e.class_id = $cid AND e.section_id = $sid";
            
            // Get Name for Filename
            $c_info = $conn->query("SELECT c.name as cname, s.name as sname FROM edu_classes c JOIN edu_sections s ON c.id = $cid AND s.id = $sid")->fetch_assoc();
            if ($c_info) {
                $filename_suffix = str_replace(' ', '_', $c_info['cname'] . '_' . $c_info['sname']);
            }
        }
    }

    $dates = [];
    $curr = $from;
    while(strtotime($curr) <= strtotime($to)) {
        if(date('w', strtotime($curr)) != 0) $dates[] = $curr;
        $curr = date('Y-m-d', strtotime($curr . ' +1 day'));
    }

    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="attendance_' . $filename_suffix . '_' . $from . '_to_' . $to . '.csv"');
    $output = fopen('php://output', 'w');
    
    // Header: Student ID, Roll No, Name, Date1(In), Date1(Out), Date2(In), Date2(Out)...
    $header = ['Student ID', 'Roll No', 'Name'];
    foreach($dates as $d) {
        $header[] = $d . ' (In)';
        $header[] = $d . ' (Out)';
    }
    fputcsv($output, $header);

    $where = "e.status = 'active' AND u.institution_id = $inst_id";
    if ($selection !== 'all') {
        $parts = explode('|', $selection);
        if (count($parts) === 2) {
            $cid = (int)$parts[0]; $sid = (int)$parts[1];
            $where .= " AND e.class_id = $cid AND e.section_id = $sid";
        }
    }

    $students = $conn->query("SELECT e.student_id, e.class_id, e.section_id, e.class_no, u.full_name 
                             FROM edu_student_enrollment e 
                             JOIN edu_users u ON e.student_id = u.id 
                             WHERE $where 
                             ORDER BY e.class_id, e.section_id, CAST(e.class_no AS UNSIGNED)");

    if ($students) {
        while ($s = $students->fetch_assoc()) {
            $row = [$s['student_id'], $s['class_no'], $s['full_name']];
            
            // Get current status for these dates if any
            foreach($dates as $d) {
                $att_q = $conn->query("SELECT status, time_in, time_out FROM edu_attendance WHERE student_id = {$s['student_id']} AND date = '$d'");
                $att = ($att_q && $att_q->num_rows > 0) ? $att_q->fetch_assoc() : null;
                
                if ($att) {
                    if ($att['status'] === 'Present') {
                        $row[] = $att['time_in'] ? date('h:i:s A', strtotime($att['time_in'])) : '08:00:00 AM';
                        $row[] = $att['time_out'] ? date('h:i:s A', strtotime($att['time_out'])) : '01:30:00 PM';
                    } elseif ($att['status'] === 'Absent') {
                        $row[] = 'A'; $row[] = 'A';
                    } elseif ($att['status'] === 'Leave') {
                        $row[] = 'L'; $row[] = 'L';
                    } elseif ($att['status'] === 'Struck Off') {
                        $row[] = 'S'; $row[] = 'S';
                    } else {
                        $row[] = '08:00:00 AM'; $row[] = '01:30:00 PM';
                    }
                } else {
                    $row[] = '08:00:00 AM';
                    $row[] = '01:30:00 PM';
                }
            }
            fputcsv($output, $row);
        }
    }
    fclose($output);
    exit;

} elseif ($action === 'save_bulk_import_json') {
    if (!in_array($role, $allowed_roles)) { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    
    if (empty($data['records']) || empty($data['dates'])) {
        json_response(['success' => false, 'error' => 'No data to save']);
    }

    $records = $data['records']; 
    $dates = $data['dates']; // These are the raw dates (not with In/Out tags)
    $processed = 0;

    require_once '../../includes/FeeManager.php';
    $fm = new FeeManager($conn, $inst_id);

    $conn->begin_transaction();
    try {
        foreach ($records as $rec) {
            $std_id = (int)$rec['student_id'];
            $enroll = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $std_id")->fetch_assoc();
            if (!$enroll) continue;
            
            $cid = (int)$enroll['class_id'];
            $sid = (int)$enroll['section_id'];

            foreach ($rec['attendance'] as $d => $statuses) {
                $in_val = trim(is_array($statuses) ? ($statuses[0] ?? '') : $statuses);
                $out_val = trim(is_array($statuses) ? ($statuses[1] ?? '') : '');

                $final_status = '';
                $ti = 'NULL';
                $to = 'NULL';

                // Check if it's a time (contains :)
                $is_in_time = strpos($in_val, ':') !== false;
                $is_out_time = strpos($out_val, ':') !== false;

                if ($is_in_time || $is_out_time || strtoupper($in_val) == 'P' || strtoupper($out_val) == 'P') {
                    $final_status = 'Present';
                    if ($is_in_time) $ti = "'" . date('H:i:s', strtotime($in_val)) . "'";
                    else $ti = "'08:00:00'";

                    if ($is_out_time) $to = "'" . date('H:i:s', strtotime($out_val)) . "'";
                    else $to = "'13:30:00'";
                } else {
                    $in_up = strtoupper($in_val);
                    $out_up = strtoupper($out_val);
                    if ($in_up == 'A' || $out_up == 'A') $final_status = 'Absent';
                    elseif ($in_up == 'L' || $out_up == 'L') $final_status = 'Leave';
                    elseif ($in_up == 'S' || $out_up == 'S') $final_status = 'Struck Off';
                }
                
                if (empty($final_status)) continue;

                $d_safe = $conn->real_escape_string($d);
                $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status, time_in, time_out) 
                            VALUES ($inst_id, $std_id, $cid, $sid, '$d_safe', '$final_status', $ti, $to)
                            ON DUPLICATE KEY UPDATE status = '$final_status', time_in = $ti, time_out = $to");
                
                // If Struck Off, update enrollment status
                if ($final_status === 'Struck Off') {
                    $conn->query("UPDATE edu_users SET status = 'struck_off' WHERE id = $std_id");
                }
                
                $processed++;
            }
            // After processing all dates for this student, sync their fines
            // We need to sync for all unique months in the import, but for simplicity, let's sync for the months of the dates we just processed
            $processed_months = [];
            foreach($rec['attendance'] as $d => $notused) {
                $m = date('F', strtotime($d));
                $y = (int)date('Y', strtotime($d));
                $processed_months[$m.'_'.$y] = ['m'=>$m, 'y'=>$y];
            }
            foreach($processed_months as $pm) {
                $fm->syncAttendanceFines($std_id, $pm['m'], $pm['y']);
            }
        }
        $conn->commit();
        json_response(['success' => true, 'processed' => $processed]);
    } catch (Exception $e) {
        $conn->rollback();
        json_response(['success' => false, 'error' => $e->getMessage()]);
    }

} elseif ($action == 'finish_attendance_scan') {
    $mode = $_POST['mode'] ?? 'check_in';
    $today = date('Y-m-d');
    
    // We want to "absent all users who have not scanned finger yet"
    // AND "take care of those users who have sent leave request/apeal".
    // Treat any leave request (approved/pending/forwarded) spanning today as a reason to mark 'Leave' instead of 'Absent'.

    $processed = 0;

    // 1. Process Students
    $res_students = $conn->query("SELECT u.id, e.class_id, e.section_id 
                                  FROM edu_users u 
                                  JOIN edu_student_enrollment e ON u.id = e.student_id 
                                  WHERE u.institution_id = $inst_id AND u.status = 'active'");
    
    require_once '../../includes/FeeManager.php';
    $fm = new FeeManager($conn, $inst_id);
    $cur_month = date('F', strtotime($today));
    $cur_year = (int)date('Y', strtotime($today));

    while ($s = $res_students->fetch_assoc()) {
        $uid = $s['id'];
        $cid = $s['class_id'];
        $sid = $s['section_id'];

        $chk = $conn->query("SELECT id FROM edu_attendance WHERE student_id = $uid AND date = '$today'");
        if ($chk->num_rows == 0) {
            // Check for leave spanning today
            $lv = $conn->query("SELECT id FROM edu_leaves WHERE user_id = $uid AND start_date <= '$today' AND end_date >= '$today'");
            $status = ($lv->num_rows > 0) ? 'Leave' : 'Absent';
            
            $conn->query("INSERT INTO edu_attendance (student_id, class_id, section_id, date, status, time_in, time_out, institution_id, marked_by) 
                          VALUES ($uid, $cid, $sid, '$today', '$status', NULL, NULL, $inst_id, 0)");
            
            // Recalculate and update fines for this month
            $fm->syncAttendanceFines($uid, $cur_month, $cur_year);

            $processed++;
        }
    }

    // 2. Process Staff
    $res_staff = $conn->query("SELECT id FROM edu_users 
                               WHERE institution_id = $inst_id 
                               AND role IN ('staff', 'teacher', 'admin', 'principal', 'head') 
                               AND status = 'active'");
                               
    while ($st = $res_staff->fetch_assoc()) {
        $uid = $st['id'];

        $chk = $conn->query("SELECT id FROM edu_staff_attendance WHERE staff_id = $uid AND date = '$today'");
        if ($chk->num_rows == 0) {
            // Check for leave spanning today
            $lv = $conn->query("SELECT id FROM edu_leaves WHERE user_id = $uid AND start_date <= '$today' AND end_date >= '$today'");
            $status = ($lv->num_rows > 0) ? 'Leave' : 'Absent';
            
            $conn->query("INSERT INTO edu_staff_attendance (staff_id, date, status, time_in, time_out, institution_id, marked_by) 
                          VALUES ($uid, '$today', '$status', NULL, NULL, $inst_id, 0)");
            $processed++;
        }
    }

    json_response(['success' => true, 'message' => "Attendance finalized. $processed unmarked users have been marked as Absent/Leave."]);


} elseif ($action == 'save_summary_pdf') {
    if (!in_array($role, $allowed_roles) && $role !== 'principal' && $role !== 'staff') { http_response_code(403); exit; }
    
    $data = json_decode(file_get_contents('php://input'), true);
    $pdf_base64 = $data['pdf_data'] ?? $data['pdf'] ?? '';
    // Strip header if present
    if (strpos($pdf_base64, 'base64,') !== false) {
        $pdf_base64 = explode('base64,', $pdf_base64)[1];
    }
    
    $filename = $data['filename'] ?? 'attendance_summary_' . date('Y-m-d') . '.pdf';

    if (empty($pdf_base64)) {
        json_response(['success' => false, 'error' => 'No PDF data received']);
    }

    $pdf_content = base64_decode($pdf_base64);
    $dir = '../../uploads/attendance_reports/';
    if (!file_exists($dir)) @mkdir($dir, 0777, true);

    $file_path = $dir . $filename;
    
    if (file_put_contents($file_path, $pdf_content)) {
        $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off' || $_SERVER['SERVER_PORT'] == 443) ? "https://" : "http://";
        $host = $_SERVER['HTTP_HOST'];
        
        // Normalize slashes for Windows compatibility
        $base_dir = str_replace('\\', '/', dirname(dirname(dirname($_SERVER['PHP_SELF']))));
        // Ensure NO trailing slash in base dir
        $base_dir = rtrim($base_dir, '/');
        
        $base_url = $protocol . $host . $base_dir;
        $public_url = $base_url . '/uploads/attendance_reports/' . $filename;
        
        json_response(['success' => true, 'url' => $public_url]);
    } else {
        json_response(['success' => false, 'error' => 'Failed to save PDF file']);
    }
} elseif ($action == 'save_public_holiday') {
    if (!in_array($role, $allowed_roles) && $role !== 'principal' && $role !== 'admin' && $role !== 'super_admin') { http_response_code(403); exit; }
    
    $data = json_decode(file_get_contents('php://input'), true);
    $name = $conn->real_escape_string($data['name']);
    $start = $conn->real_escape_string($data['start']);
    $end = $conn->real_escape_string($data['end']);
    
    // Create holidays table if not exist
    $conn->query("CREATE TABLE IF NOT EXISTS `edu_public_holidays` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `institution_id` int(11) NOT NULL,
      `name` varchar(255) NOT NULL,
      `from_date` date NOT NULL,
      `to_date` date NOT NULL,
      `class_ids` text DEFAULT NULL,
      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

    // Defensive: Add column if table existed but was old
    $check_col = $conn->query("SHOW COLUMNS FROM edu_public_holidays LIKE 'class_ids'");
    if($check_col && $check_col->num_rows == 0) {
        $conn->query("ALTER TABLE edu_public_holidays ADD COLUMN class_ids TEXT AFTER to_date");
    }
    
    $class_ids = !empty($data['class_ids']) ? $conn->real_escape_string($data['class_ids']) : '';

    $conn->query("INSERT INTO edu_public_holidays (institution_id, name, from_date, to_date, class_ids) VALUES ($inst_id, '$name', '$start', '$end', '$class_ids')");
    
    // Auto-mark appropriate attendees for the entire date range
    try {
        $conn->begin_transaction();
        
        // Scope for classes
        $class_filter = "";
        if(!empty($class_ids) && $class_ids !== 'all') {
            $ids = array_map('intval', explode(',', $class_ids));
            $ids_str = implode(',', $ids);
            if(!empty($ids_str)) {
                $class_filter = " AND e.class_id IN ($ids_str)";
            }
        }
        
        $current = strtotime($start);
        $last = strtotime($end);
        
        while($current <= $last) {
            $current_date = date('Y-m-d', $current);
            // 1. Students (Institution Safe + FK existence check)
            $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status) 
                          SELECT $inst_id, e.student_id, e.class_id, e.section_id, '$current_date', 'PH' 
                          FROM edu_student_enrollment e
                          JOIN edu_users u ON e.student_id = u.id
                          JOIN edu_sections sec ON e.section_id = sec.id
                          JOIN edu_classes cls ON e.class_id = cls.id
                          WHERE u.institution_id = $inst_id AND e.status = 'active' $class_filter
                          ON DUPLICATE KEY UPDATE status = 'PH'");
            
            // 2. Staff (Only if all classes or specific flag, but for now mark all staff if it's a PH)
            // If the user wants specific classes only, it usually means student holiday. 
            // However, typical behavior is that PH applies to all staff.
            if(empty($class_filter)) {
                $conn->query("INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status)
                              SELECT $inst_id, id, '$current_date', 'PH'
                              FROM edu_users
                              WHERE institution_id = $inst_id AND role IN ('staff','teacher','admin','principal','head') AND status = 'active'
                              ON DUPLICATE KEY UPDATE status = 'PH'");
            }
                          
            $current = strtotime('+1 day', $current);
        }
        $conn->commit();
        json_response(['success' => true]);
    } catch (Exception $e) {
        $conn->rollback();
        json_response(['success' => false, 'error' => $e->getMessage()]);
    }

} elseif ($action == 'update_public_holiday') {
    if (!in_array($role, $allowed_roles) && $role !== 'principal' && $role !== 'admin' && $role !== 'super_admin') { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    $id = (int)$data['id'];
    $name = $conn->real_escape_string($data['name']);
    $start = $conn->real_escape_string($data['start']);
    $end = $conn->real_escape_string($data['end']);
    $class_ids = !empty($data['class_ids']) ? $conn->real_escape_string($data['class_ids']) : '';

    // 1. Fetch old range to cleanup
    $old = $conn->query("SELECT from_date, to_date FROM edu_public_holidays WHERE id = $id AND institution_id = $inst_id")->fetch_assoc();
    
    // 2. Clear old PH marks (only those that were PH)
    if($old) {
        $old_from = $old['from_date'];
        $old_to = $old['to_date'];
        $conn->query("DELETE FROM edu_attendance WHERE institution_id = $inst_id AND status = 'PH' AND date BETWEEN '$old_from' AND '$old_to'");
        $conn->query("DELETE FROM edu_staff_attendance WHERE institution_id = $inst_id AND status = 'PH' AND date BETWEEN '$old_from' AND '$old_to'");
    }

    // 3. Update the holiday record
    $conn->query("UPDATE edu_public_holidays SET name = '$name', from_date = '$start', to_date = '$end', class_ids = '$class_ids' 
                  WHERE id = $id AND institution_id = $inst_id");

    // 4. Apply new PH marks
    $current = strtotime($start);
    $last = strtotime($end);
    $class_filter = "";
    if(!empty($class_ids) && $class_ids !== 'all') {
        $ids = array_map('intval', explode(',', $class_ids));
        $ids_str = implode(',', $ids);
        if(!empty($ids_str)) $class_filter = " AND e.class_id IN ($ids_str)";
    }

    while($current <= $last) {
        $curr_date = date('Y-m-d', $current);
        $conn->query("INSERT INTO edu_attendance (institution_id, student_id, class_id, section_id, date, status) 
                      SELECT $inst_id, e.student_id, e.class_id, e.section_id, '$curr_date', 'PH' FROM edu_student_enrollment e JOIN edu_users u ON e.student_id = u.id WHERE u.institution_id = $inst_id AND e.status = 'active' $class_filter
                      ON DUPLICATE KEY UPDATE status = 'PH'");
        if(empty($class_filter)) {
            $conn->query("INSERT INTO edu_staff_attendance (institution_id, staff_id, date, status)
                          SELECT $inst_id, id, '$curr_date', 'PH' FROM edu_users WHERE institution_id = $inst_id AND role IN ('staff','teacher','admin','principal','head') AND status = 'active'
                          ON DUPLICATE KEY UPDATE status = 'PH'");
        }
        $current = strtotime('+1 day', $current);
    }

    json_response(['success' => true]);

} elseif ($action == 'get_public_holidays') {
    $check = $conn->query("SHOW TABLES LIKE 'edu_public_holidays'");
    if(!$check || $check->num_rows == 0) {
        json_response([]);
        exit;
    }
    $res = $conn->query("SELECT * FROM edu_public_holidays WHERE institution_id = $inst_id ORDER BY from_date DESC");
    $holidays = [];
    while($row = $res->fetch_assoc()) {
        $holidays[] = $row;
    }
    json_response($holidays);

} elseif ($action == 'delete_public_holiday') {
    if (!in_array($role, $allowed_roles) && $role !== 'principal' && $role !== 'admin' && $role !== 'super_admin') { http_response_code(403); exit; }
    $data = json_decode(file_get_contents('php://input'), true);
    $id = (int)$data['id'];
    
    // Fetch range before deleting
    $holiday = $conn->query("SELECT from_date, to_date FROM edu_public_holidays WHERE id = $id AND institution_id = $inst_id")->fetch_assoc();
    if($holiday) {
        $from = $holiday['from_date'];
        $to = $holiday['to_date'];
        
        // Revert Sundays to 'Holiday' (H) instead of deleting them
        // In MySQL DAYOFWEEK, 1 is Sunday.
        $conn->query("UPDATE edu_attendance SET status = 'Holiday' 
                      WHERE institution_id = $inst_id AND status = 'PH' 
                      AND date BETWEEN '$from' AND '$to' AND DAYOFWEEK(date) = 1");
        $conn->query("UPDATE edu_staff_attendance SET status = 'Holiday' 
                      WHERE institution_id = $inst_id AND status = 'PH' 
                      AND date BETWEEN '$from' AND '$to' AND DAYOFWEEK(date) = 1");

        // Delete remaining 'PH' records for this range
        $conn->query("DELETE FROM edu_attendance WHERE institution_id = $inst_id AND status = 'PH' AND date BETWEEN '$from' AND '$to'");
        $conn->query("DELETE FROM edu_staff_attendance WHERE institution_id = $inst_id AND status = 'PH' AND date BETWEEN '$from' AND '$to'");
    }

    $conn->query("DELETE FROM edu_public_holidays WHERE id = $id AND institution_id = $inst_id");
    json_response(['success' => true]);

} elseif ($action == 'get_submission_status') {
    // Ensure table and columns exist
    $conn->query("CREATE TABLE IF NOT EXISTS `edu_attendance_submission` (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `institution_id` int(11) NOT NULL,
        `class_id` int(11) NOT NULL,
        `section_id` int(11) NOT NULL,
        `date` date NOT NULL,
        `submitted_by` int(11) DEFAULT NULL,
        `submitted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `teacher_name` varchar(255) DEFAULT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uniq_submission` (`institution_id`,`class_id`,`section_id`,`date`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    
    $check_tname = $conn->query("SHOW COLUMNS FROM edu_attendance_submission LIKE 'teacher_name'");
    if ($check_tname->num_rows == 0) $conn->query("ALTER TABLE edu_attendance_submission ADD COLUMN teacher_name varchar(255) DEFAULT NULL");

    $date = $conn->real_escape_string($_GET['date'] ?? date('Y-m-d'));

    $classes_q = $conn->query("
        SELECT c.id as class_id, c.name as class_name, s.id as section_id, s.name as section_name,
               sub.id as sub_id, sub.submitted_at, sub.teacher_name
        FROM edu_classes c
        LEFT JOIN edu_sections s ON s.class_id = c.id
        LEFT JOIN edu_attendance_submission sub
            ON sub.class_id = c.id AND sub.section_id = s.id
            AND sub.date = '$date' AND sub.institution_id = $inst_id
        WHERE c.institution_id = $inst_id
        ORDER BY c.name ASC, s.name ASC
    ");

    $result = [];
    if ($classes_q) {
        while ($row = $classes_q->fetch_assoc()) {
            $result[] = [
                'class_id'     => (int)$row['class_id'],
                'class_name'   => $row['class_name'],
                'section_id'   => (int)$row['section_id'],
                'section_name' => $row['section_name'],
                'submitted'    => !empty($row['sub_id']),
                'submitted_at' => $row['submitted_at'],
                'teacher_name' => $row['teacher_name'],
            ];
        }
    }
    json_response(['success' => true, 'date' => $date, 'classes' => $result]);

} elseif ($action == 'mark_attendance_submitted') {
    if (!$is_admin) json_response(['success' => false, 'error' => 'Permission Denied. Only admins can manually override submission status.']);
    
    $conn->query("CREATE TABLE IF NOT EXISTS `edu_attendance_submission` (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `institution_id` int(11) NOT NULL,
        `class_id` int(11) NOT NULL,
        `section_id` int(11) NOT NULL,
        `date` date NOT NULL,
        `submitted_by` int(11) DEFAULT NULL,
        `submitted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        `teacher_name` varchar(255) DEFAULT NULL,
        PRIMARY KEY (`id`),
        UNIQUE KEY `uniq_submission` (`institution_id`,`class_id`,`section_id`,`date`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

    $data = json_decode(file_get_contents('php://input'), true);
    $class_id   = (int)($data['class_id'] ?? 0);
    $section_id = (int)($data['section_id'] ?? 0);
    $date       = $conn->real_escape_string($data['date'] ?? date('Y-m-d'));

    if (!$class_id || !$section_id) {
        json_response(['success' => false, 'error' => 'Missing class_id or section_id']);
    }

    $user_id   = (int)($_SESSION['edu_user_id'] ?? 0);
    $tname_q   = $conn->query("SELECT full_name FROM edu_users WHERE id = $user_id LIMIT 1");
    $tname_esc = ($tname_q && $tname_q->num_rows > 0) ? $conn->real_escape_string($tname_q->fetch_assoc()['full_name']) : 'Unknown';

    $conn->query("INSERT INTO edu_attendance_submission (institution_id, class_id, section_id, date, submitted_by, teacher_name)
                  VALUES ($inst_id, $class_id, $section_id, '$date', $user_id, '$tname_esc')
                  ON DUPLICATE KEY UPDATE submitted_by = $user_id, teacher_name = '$tname_esc', submitted_at = NOW()");

    json_response(['success' => true, 'message' => 'Attendance marked as submitted.']);

} elseif ($action == 'clear_attendance_submission') {
    if (!$is_admin) json_response(['success' => false, 'error' => 'Permission Denied. Only admins can clear attendance submissions.']);
    
    $data = json_decode(file_get_contents('php://input'), true);
    $class_id   = (int)($data['class_id'] ?? 0);
    $section_id = (int)($data['section_id'] ?? 0);
    $date       = $conn->real_escape_string($data['date'] ?? date('Y-m-d'));

    if (!$class_id || !$section_id) {
        json_response(['success' => false, 'error' => 'Missing class_id or section_id']);
    }

    // 1. Clear the submission status
    $conn->query("DELETE FROM edu_attendance_submission 
                  WHERE institution_id = $inst_id 
                  AND class_id = $class_id 
                  AND section_id = $section_id 
                  AND date = '$date'");

    // 2. Clear the actual attendance records (present/absent marks)
    $conn->query("DELETE FROM edu_attendance 
                  WHERE institution_id = $inst_id 
                  AND class_id = $class_id 
                  AND section_id = $section_id 
                  AND date = '$date'");

    json_response(['success' => true, 'message' => 'Attendance data and submission status cleared.']);
}

} catch (Throwable $e) {
    file_put_contents('api_error.log', date('[Y-m-d H:i:s] ') . $e->getMessage() . " in " . $e->getFile() . ":" . $e->getLine() . "\n", FILE_APPEND);
    json_response([
        'success' => false,
        'error' => 'Server Exception', 
        'details' => $e->getMessage() . " (File: " . basename($e->getFile()) . " Line: " . $e->getLine() . ")",
    ]);
}
?>





