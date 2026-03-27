<?php
/**
 * Staff API Actions
 * Related to Teachers, Staff members, Attendance marking, and Teaching tasks.
 */
require_once 'api_common.php';

if ($action === 'GET_DASHBOARD' || $action === 'SWITCH_AND_GET_DASHBOARD') {
    send_dashboard_response($role, $edu_user_id, $inst_id, $conn);
} elseif ($action === 'GET_NOTICES') {
    get_notices_response($inst_id, $role, $edu_user_id, $conn);
} elseif ($action === 'GET_STRUCTURE') {
    get_structure_response($inst_id, $conn);
} elseif ($action === 'GET_STAFF') {
    get_staff_list_response($inst_id, $conn);
} elseif ($action === 'GET_STAFF_PROFILE') {
    $staff_id = (int)($_REQUEST['id'] ?: ($_REQUEST['staff_id'] ?? $edu_user_id));
    $res = $conn->query("SELECT * FROM edu_users WHERE id = $staff_id AND role NOT IN ('student','parent')");
    if ($row = $res->fetch_assoc()) {
        echo json_encode(['status' => 'success', 'profile' => $row]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Staff profile not found']);
    }
    exit;

} elseif ($action === 'MARK_ATTENDANCE') {
    // ... mark attendance code ...
    $student_id = (int)$_REQUEST['student_id'];
    $status = $_REQUEST['status'] ?? '';
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

} elseif ($action === 'GET_SUBJECTS') {
    // ... get subjects code ...
    $subjects = [];
    $query = "SELECT DISTINCT s.* 
              FROM edu_subjects s
              JOIN edu_timetable t ON s.id = t.subject_id
              WHERE t.staff_id = $edu_user_id 
              AND s.institution_id = $inst_id
              ORDER BY s.name";
    $res = $conn->query($query);
    if ($res) {
        while ($row = $res->fetch_assoc()) {
            $sid = (int)$row['id'];
            $count_q = $conn->query("SELECT COUNT(DISTINCT class_id) as cnt FROM edu_timetable WHERE staff_id = $edu_user_id AND subject_id = $sid");
            $ccount = $count_q ? (int)$count_q->fetch_assoc()['cnt'] : 0;
            $subjects[] = ['id' => $sid, 'name' => $row['name'], 'classesCount' => $ccount];
        }
    }
    echo json_encode(['status' => 'success', 'subjects' => $subjects]);
    exit;

} elseif ($action === 'GET_STUDENTS') {
    get_students_list_response($inst_id, (int)($_REQUEST['class_id']??0), (int)($_REQUEST['section_id']??0), $conn, $edu_user_id);

} elseif ($action === 'CREATE_ASSIGNMENT') {
    $cid = (int)$_POST['class_id'];
    $sid = (int)$_POST['section_id'];
    $subid = (int)$_POST['subject_id'];
    $title = $conn->real_escape_string($_POST['title']);
    $desc = $conn->real_escape_string($_POST['description'] ?? '');
    $due = $conn->real_escape_string($_POST['due_date']);

    $q = "INSERT INTO edu_assignments (institution_id, class_id, section_id, subject_id, teacher_id, title, description, due_date) VALUES ($inst_id, $cid, $sid, $subid, $edu_user_id, '$title', '$desc', '$due')";
    if($conn->query($q)) echo json_encode(['status' => 'success']); else echo json_encode(['status' => 'error', 'message' => $conn->error]);
    exit;
}
