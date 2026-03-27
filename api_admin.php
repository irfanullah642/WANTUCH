<?php
/**
 * Admin API Actions
 * Related to Management, Institution Oversight, and Administrative controls.
 */
require_once 'api_common.php';

if ($action === 'GET_DASHBOARD' || $action === 'SWITCH_AND_GET_DASHBOARD') {
    if ($action === 'SWITCH_AND_GET_DASHBOARD') {
        $_SESSION['edu_institution_id'] = $inst_id;
        $inst_q = $conn->query("SELECT name FROM edu_institutions WHERE id = $inst_id");
        if ($inst_q && $inst_row = $inst_q->fetch_assoc()) {
            $_SESSION['edu_institution_name'] = $inst_row['name'];
        }
    }
    send_dashboard_response($role, $edu_user_id, $inst_id, $conn);
} elseif ($action === 'GET_NOTICES') {
    get_notices_response($inst_id, $role, $edu_user_id, $conn);
} elseif ($action === 'GET_STAFF') {
    get_staff_list_response($inst_id, $conn);
} elseif ($action === 'GET_STUDENTS') {
    get_students_list_response($inst_id, (int)($_REQUEST['class_id']??0), (int)($_REQUEST['section_id']??0), $conn);
} elseif ($action === 'GET_STRUCTURE') {
    get_structure_response($inst_id, $conn);
} elseif ($action === 'GET_FEE_DASHBOARD') {
    if (class_exists('FeeManager')) {
        $fm = new FeeManager($conn, $inst_id);
        echo json_encode($fm->getDashboardStats());
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Fee logic not loaded']);
    }
    exit;

} elseif ($action === 'GET_FEE_INTELLIGENCE') {
    if (class_exists('FeeManager')) {
        $fm = new FeeManager($conn, $inst_id);
        echo json_encode($fm->getFeeIntelligence());
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Fee logic not loaded']);
    }
    exit;

} elseif ($action === 'SAVE_STUDENT') {
    $id = (int)($_POST['id'] ?? 0);
    $name = $conn->real_escape_string($_POST['full_name'] ?? '');
    $gender = $conn->real_escape_string($_POST['gender'] ?? 'Male');
    $fname = $conn->real_escape_string($_POST['father_name'] ?? '');
    $cls = (int)($_POST['class_id'] ?? 0);
    $sec = (int)($_POST['section_id'] ?? 0);
    $adm = $conn->real_escape_string($_POST['adm_no'] ?? '');

    if ($id > 0) {
        $q = "UPDATE edu_users SET full_name='$name', gender='$gender', father_name='$fname' WHERE id=$id";
    } else {
        $q = "INSERT INTO edu_users (institution_id, full_name, role, gender, father_name, adm_no) VALUES ($inst_id, '$name', 'student', '$gender', '$fname', '$adm')";
    }
    if($conn->query($q)) echo json_encode(['status' => 'success']); else echo json_encode(['status' => 'error', 'message' => $conn->error]);
    exit;

} elseif ($action === 'SAVE_STAFF') {
    $id = (int)($_POST['id'] ?? 0);
    $name = $conn->real_escape_string($_POST['full_name'] ?? '');
    $role = $conn->real_escape_string($_POST['role'] ?? 'teacher');
    
    if ($id > 0) {
        $q = "UPDATE edu_users SET full_name='$name', role='$role' WHERE id=$id";
    } else {
        $q = "INSERT INTO edu_users (institution_id, full_name, role) VALUES ($inst_id, '$name', '$role')";
    }
    if($conn->query($q)) echo json_encode(['status' => 'success']); else echo json_encode(['status' => 'error', 'message' => $conn->error]);
    exit;
}
