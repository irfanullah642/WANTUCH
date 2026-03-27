<?php
/**
 * Student API Actions
 */
require_once 'api_common.php';

if ($action === 'GET_DASHBOARD' || $action === 'SWITCH_AND_GET_DASHBOARD') {
    send_dashboard_response($role, $edu_user_id, $inst_id, $conn);
} elseif ($action === 'GET_NOTICES') {
    get_notices_response($inst_id, $role, $edu_user_id, $conn);
} elseif ($action === 'GET_STUDENT_PROFILE') {
    $student_id = (int)($_REQUEST['id'] ?: ($_REQUEST['student_id'] ?? $edu_user_id));
    $raw = $conn->query("SELECT u.id, u.full_name, u.username, u.gender, u.father_name, u.profile_pic, u.address, u.cnic, u.dob, u.whatsapp_no, u.father_cnic, u.adm_no, u.date_of_admission, u.class_no, e.roll_number, e.admission_date, c.name as class_name, s.name as section_name FROM edu_users u LEFT JOIN edu_student_enrollment e ON u.id = e.student_id LEFT JOIN edu_classes c ON e.class_id = c.id LEFT JOIN edu_sections s ON e.section_id = s.id WHERE u.id = $student_id")->fetch_assoc();
    if ($raw) {
        $basic = []; foreach($raw as $k => $v) $basic[$k] = (string)($v ?? "");
        echo json_encode(['status' => 'success', 'profile' => $basic]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Profile not found']);
    }
    exit;

} elseif ($action === 'GET_SUBJECTS') {
    $subjects = [];
    $query = "SELECT DISTINCT s.id, s.name, s.type FROM edu_subjects s JOIN edu_timetable t ON s.id = t.subject_id JOIN edu_student_enrollment e ON t.class_id = e.class_id WHERE e.student_id = $edu_user_id AND s.institution_id = $inst_id ORDER BY s.name";
    $res = $conn->query($query);
    while ($row = $res->fetch_assoc()) {
        $subjects[] = ['id' => (int)$row['id'], 'name' => $row['name'], 'type' => $row['type'] ?? 'Compulsory', 'classesCount' => 1];
    }
    echo json_encode(['status' => 'success', 'subjects' => $subjects]);
    exit;

} elseif ($action === 'GET_STUDENT_FEE_LEDGER') {
    if (class_exists('FeeManager')) {
        $f_month = $_REQUEST['month'] ?? 'All';
        $f_year = (int)($_REQUEST['year'] ?? 0);
        $fm = new FeeManager($conn, $inst_id);
        echo json_encode($fm->getStudentLedger($edu_user_id, $f_month, $f_year));
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Fee logic not loaded']);
    }
    exit;

} elseif ($action === 'GET_TIMETABLE') {
    $enroll = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $edu_user_id AND status = 'Active' LIMIT 1")->fetch_assoc();
    $class_id = (int)($enroll['class_id'] ?? 0);
    $section_id = (int)($enroll['section_id'] ?? 0);
    $q = "SELECT t.*, s.name as subject_name, u.full_name as staff_name FROM edu_timetable t JOIN edu_subjects s ON t.subject_id = s.id LEFT JOIN edu_users u ON t.staff_id = u.id WHERE t.class_id = $class_id AND t.section_id = $section_id AND t.institution_id = $inst_id ORDER BY t.day_of_week";
    $res = $conn->query($q);
    $timetable = []; while($row = $res->fetch_assoc()) $timetable[] = $row;
    echo json_encode(['status' => 'success', 'timetable' => $timetable]);
    exit;

} elseif ($action === 'GET_SYLLABUS') {
    $enroll = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $edu_user_id AND status = 'Active' LIMIT 1")->fetch_assoc();
    $class_id = (int)($enroll['class_id'] ?? 0); $section_id = (int)($enroll['section_id'] ?? 0);
    $items = [];
    $query = "SELECT DISTINCT t.class_id, t.section_id, t.subject_id, c.name as class_name, sec.name as section_name, sub.name as subject_name FROM edu_timetable t JOIN edu_classes c ON t.class_id = c.id JOIN edu_sections sec ON t.section_id = sec.id JOIN edu_subjects sub ON t.subject_id = sub.id WHERE t.institution_id = $inst_id AND t.class_id = $class_id AND t.section_id = $section_id";
    $res = $conn->query($query);
    while ($row = $res->fetch_assoc()) {
        $subid = (int)$row['subject_id']; $chapters = [];
        $ch_q = $conn->query("SELECT * FROM edu_syllabus WHERE class_id = $class_id AND subject_id = $subid AND parent_id = 0 ORDER BY sort_order ASC");
        while ($ch = $ch_q->fetch_assoc()) {
            $chid = (int)$ch['id']; $topics = [];
            $tp_q = $conn->query("SELECT * FROM edu_syllabus WHERE parent_id = $chid ORDER BY sort_order ASC");
            while($tp = $tp_q->fetch_assoc()) $topics[] = ['id' => (int)$tp['id'], 'title' => $tp['topic_name'], 'status' => $tp['status']];
            $chapters[] = ['id' => $chid, 'title' => $ch['topic_name'], 'topics' => $topics];
        }
        $items[] = ['class_name' => $row['class_name'] . ' ' . $row['section_name'], 'subject_name' => $row['subject_name'], 'chapters' => $chapters];
    }
    echo json_encode(['status' => 'success', 'items' => $items]);
    exit;

} elseif ($action === 'GET_MONTHLY_STUDENT_LEDGER') {
    $month_name = $_REQUEST['month'] ?? date('F');
    $month_num = (int)date('m', strtotime($month_name));
    $year = (int)($_REQUEST['year'] ?? date('Y'));
    $days_in_month = (int)date('t', strtotime("$year-$month_num-01"));

    // Fetch Public Holidays
    $holidays = [];
    $res_h = $conn->query("SELECT id, name, from_date, to_date, class_ids FROM edu_public_holidays WHERE institution_id = $inst_id AND (MONTH(from_date) = $month_num OR MONTH(to_date) = $month_num) AND (YEAR(from_date) = $year OR YEAR(to_date) = $year)");
    if ($res_h) { while($h = $res_h->fetch_assoc()) { $holidays[] = $h; } }

    // Robust Enrollment Check: filter by inst_id and Active/OnRoll status
    $s_query = "SELECT u.id, u.full_name as name, e.class_no, e.class_id, e.section_id 
                FROM edu_users u 
                JOIN edu_student_enrollment e ON u.id = e.student_id 
                WHERE u.id = $edu_user_id AND (e.status = 'Active' OR e.status = 'active' OR e.status = 'OnRoll' OR e.status = 'onroll') 
                LIMIT 1";
    $student = $conn->query($s_query)->fetch_assoc();
    
    if ($student) {
        $attendance = [];
        // Permit finding attendance even if institution_id is missing or slightly different in logs
        $att_q = $conn->query("SELECT DAY(date) as d, status FROM edu_attendance WHERE student_id = $edu_user_id AND MONTH(date) = $month_num AND YEAR(date) = $year");
        while($a = $att_q->fetch_assoc()) { $attendance[(int)$a['d']] = strtoupper(trim($a['status'] ?? '')); }
        $student['attendance'] = $attendance;
        
        // Stats
        $p=0; $a=0; $l=0; $ph=0;
        foreach($attendance as $st) {
            if($st=='P' || $st=='PRESENT') $p++;
            elseif($st=='A' || $st=='ABSENT') $a++;
            elseif($st=='L' || $st=='LEAVE') $l++;
            elseif($st=='PH' || $st=='H' || $st=='HOLIDAY') $ph++;
        }
        $student['stats'] = ['tm_att'=>$p, 'p'=>$p, 'a'=>$a, 'l'=>$l, 'ph'=>$ph, 'absent'=>$a, 'present'=>$p, 'leave'=>$l];
        
        // YTD Stats
        $y_start = $year . "-01-01"; $y_end = date('Y-m-d');
        $y_res = $conn->query("SELECT status FROM edu_attendance WHERE student_id = $edu_user_id AND date >= '$y_start' AND date <= '$y_end'");
        $yp=0; $ya=0; $yl=0; $ys=0; $yph=0; $yt=0;
        while($yr = $y_res->fetch_assoc()) {
            $st = strtoupper(trim($yr['status']));
            if($st=='P' || $st=='PRESENT') $yp++;
            elseif($st=='A' || $st=='ABSENT') $ya++;
            elseif($st=='L' || $st=='LEAVE') $yl++;
            elseif($st=='S' || $st=='SUNDAY') $ys++;
            elseif($st=='PH' || $st=='H' || $st=='HOLIDAY') $yph++;
            $yt++;
        }
        $student['stats']['y_p'] = $yp; 
        $student['stats']['y_a'] = $ya; 
        $student['stats']['y_l'] = $yl; 
        $student['stats']['y_s'] = $ys; 
        $student['stats']['y_ph'] = $yph; 
        $student['stats']['y_total'] = $yt;
        $student['stats']['present_total'] = $yp;
        $student['stats']['absent_total'] = $ya;
        $student['stats']['leave_total'] = $yl;
    }

    echo json_encode(['status' => 'success', 'students' => $student ? [$student] : [], 'holidays' => $holidays, 'days_in_month' => $days_in_month]);
    exit;

} elseif ($action === 'GET_LEAVE_APPEALS') {
    $where = "WHERE l.institution_id = $inst_id AND l.user_id = $edu_user_id";
    $q = "SELECT l.*, u.full_name as user_name FROM edu_leaves l JOIN edu_users u ON l.user_id = u.id $where ORDER BY l.created_at DESC";
    $res = $conn->query($q);
    $appeals = [];
    if($res) { while($row = $res->fetch_assoc()) $appeals[] = $row; }
    echo json_encode(['status' => 'success', 'appeals' => $appeals]);
    exit;

} elseif ($action === 'SAVE_LEAVE_APPEAL') {
    $from = $conn->real_escape_string($_REQUEST['from_date'] ?? '');
    $to = $conn->real_escape_string($_REQUEST['to_date'] ?? '');
    $type = $conn->real_escape_string($_REQUEST['leave_type'] ?? '');
    $reason = $conn->real_escape_string($_REQUEST['reason'] ?? '');
    
    // Auto-Create Table if not exists
    $conn->query("CREATE TABLE IF NOT EXISTS `edu_leaves` (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `institution_id` int(11) NOT NULL,
        `user_id` int(11) NOT NULL,
        `role` varchar(50) NOT NULL,
        `requested_by` varchar(50) NOT NULL DEFAULT 'self',
        `requester_id` int(11) NOT NULL,
        `start_date` date NOT NULL,
        `end_date` date NOT NULL,
        `leave_type` varchar(255) DEFAULT NULL,
        `reason` text NOT NULL,
        `status` enum('pending','approved','rejected','forwarded_to_parent') NOT NULL DEFAULT 'pending',
        `parent_status` enum('pending','confirmed','rejected') NOT NULL DEFAULT 'pending',
        `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
        PRIMARY KEY (`id`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

    // Fix: Explicitly handle the requester_id and requested_by for students
    $requester_id = (int)$edu_user_id; 
    $user_role = strpos($role_lower, 'student') !== false ? 'student' : (strpos($role_lower, 'parent') !== false ? 'parent' : 'staff');
    $requested_by = (strpos($role_lower, 'parent') !== false) ? 'parent' : 'self';

    $stmt = $conn->prepare("INSERT INTO edu_leaves (institution_id, user_id, role, requested_by, requester_id, start_date, end_date, leave_type, reason, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')");
    // Types: iis sissss (9 params: inst:i, user:i, role:s, by:s, reqid:i, from:s, to:s, type:s, reason:s)
    $stmt->bind_param("iississss", $inst_id, $edu_user_id, $user_role, $requested_by, $requester_id, $from, $to, $type, $reason);
    
    if ($stmt->execute()) {
        echo json_encode(['status' => 'success', 'message' => 'Leave appeal submitted successfully']);
    } else {
        echo json_encode(['status' => 'error', 'message' => $conn->error]);
    }
    exit;

} elseif ($action === 'DELETE_LEAVE_APPEAL') {
    $id = (int)$_REQUEST['id'];
    // Students can only delete their own pending appeals
    $where = ($role_lower === 'student') ? "AND user_id = $edu_user_id AND status = 'pending'" : "";
    $conn->query("DELETE FROM edu_leaves WHERE id = $id $where");
    echo json_encode(['status' => 'success', 'message' => 'Appeal deleted']);
    exit;

} elseif ($action === 'GET_ASSIGNMENTS') {
    $enroll = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $edu_user_id AND status='Active' LIMIT 1")->fetch_assoc();
    $cid = (int)($enroll['class_id'] ?? 0); $sid = (int)($enroll['section_id'] ?? 0);
    $history_asn = [];
    $res = $conn->query("SELECT a.*, s.name as subname FROM edu_assignments a JOIN edu_subjects s ON a.subject_id=s.id WHERE a.class_id=$cid AND a.section_id=$sid ORDER BY a.due_date DESC");
    while($r = $res->fetch_assoc()) {
        $check_sub = $conn->query("SELECT status, feedback, submitted_at FROM edu_assignment_submissions WHERE assignment_id={$r['id']} AND student_id=$edu_user_id")->fetch_assoc();
        $r['submission_status'] = $check_sub['status'] ?? 'Pending';
        $r['submission_feedback'] = $check_sub['feedback'] ?? '';
        $r['submitted_at'] = $check_sub['submitted_at'] ?? null;
        $history_asn[] = $r;
    }
    echo json_encode(['status' => 'success', 'history' => $history_asn]);
    exit;
}
