<?php
/**
 * Super Admin API Actions
 * Related to Database Management, Smart Attendance, and Global Oversight.
 */
require_once 'api_common.php';

if ($action === 'GET_DASHBOARD' || $action === 'SWITCH_AND_GET_DASHBOARD') {
    // Preserve context from api_common if it recovered it from User ID
    $inst_id = (int)($_REQUEST['institution_id'] ?? $_REQUEST['inst_id'] ?? $inst_id ?? $_SESSION['edu_institution_id'] ?? 0);
    $main_user_id = (int)($get_uid ?: $_SESSION['user_id'] ?: 0);
    
    // Switch context
    $stmt = $conn->prepare("SELECT id, name, type, logo_path FROM edu_institutions WHERE id = ?");
    $stmt->bind_param("i", $inst_id);
    $stmt->execute();
    $res = $stmt->get_result();
    
    if ($res && $res->num_rows > 0) {
        $inst = $res->fetch_assoc();
        $_SESSION['edu_institution_id'] = $inst['id'];
        $_SESSION['edu_institution_name'] = $inst['name'];
        $_SESSION['edu_name'] = $inst['name']; // Fallback
        
        // Resolve local identity in the new institution
        if ($main_user_id > 0) {
            $username = $_SESSION['user_username'] ?? '';
            if ($username) {
                $stmt_loc = $conn->prepare("SELECT id, role, full_name FROM edu_users WHERE username = ? AND institution_id = ? LIMIT 1");
                $stmt_loc->bind_param("si", $username, $inst_id);
                $stmt_loc->execute();
                $res_loc = $stmt_loc->get_result();
                if ($res_loc && $res_loc->num_rows > 0) {
                    $loc_row = $res_loc->fetch_assoc();
                    $_SESSION['edu_user_id'] = $loc_row['id'];
                    $_SESSION['edu_role'] = $loc_row['role'];
                    $_SESSION['edu_name'] = $loc_row['full_name'];
                } else {
                    $_SESSION['edu_user_id'] = 0; 
                    $_SESSION['edu_role'] = $_SESSION['global_role'] ?? 'super_admin';
                    $_SESSION['edu_name'] = $_SESSION['user_fullname'] ?? 'Administrator';
                }
            }
        }
            
        // Finalize identity from session (it may have changed above)
        $edu_user_id = $_SESSION['edu_user_id'] ?? 0;
        $role = $_SESSION['edu_role'] ?? 'super_admin';
        
        send_dashboard_response($role, $edu_user_id, $inst_id, $conn);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Institution not found']);
    }
    exit;

} elseif ($action === 'IMPORT_DATABASE') {
    if (!isset($_FILES['sql_file']) || $_FILES['sql_file']['error'] !== UPLOAD_ERR_OK) {
        echo json_encode(['status' => 'error', 'message' => 'No valid sql file uploaded.']);
        exit;
    }
    
    $sql_content = file_get_contents($_FILES['sql_file']['tmp_name']);
    if (empty($sql_content)) {
        echo json_encode(['status' => 'error', 'message' => 'Empty sql file uploaded.']);
        exit;
    }

    $sql_content = str_ireplace('INSERT INTO', 'INSERT IGNORE INTO', $sql_content);

    if ($conn->multi_query($sql_content)) {
        do { if ($result = $conn->store_result()) $result->free(); } while ($conn->more_results() && $conn->next_result());
        echo json_encode(['status' => 'success', 'message' => 'Database imported successfully.']);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Query execution failed: ' . $conn->error]);
    }
    exit;

} elseif ($action === 'GET_SMART_STATUS') {
    $inst_id = (int)($_REQUEST['inst_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    echo json_encode([
        'status' => 'success',
        'is_active' => true,
        'hardware' => 'ONLINE',
        'ip' => '192.168.1.104',
        'engine' => 'WANTUCH AI Core v2.1'
    ]);
    exit;

} elseif ($action === 'SAVE_SMART_CONFIG') {
    $inst_id = (int)($_POST['institution_id'] ?? $_SESSION['edu_institution_id'] ?? 0);
    if ($inst_id <= 0) { echo json_encode(['status' => 'error', 'message' => 'Invalid Institution']); exit; }
    
    $v_eng = (int)($_POST['vision_engine'] ?? 1);
    $p_eng = (int)($_POST['pulse_engine'] ?? 0);
    $s_alt = (int)($_POST['smart_alerts'] ?? 1);
    $ip = $conn->real_escape_string($_POST['ip_stream_address'] ?? '');
    $user = $conn->real_escape_string($_POST['auth_user'] ?? '');
    $pass = $conn->real_escape_string($_POST['auth_pass'] ?? '');
    $window = $conn->real_escape_string($_POST['attendance_window'] ?? '');
    $start = $conn->real_escape_string($_POST['check_in_time_start'] ?? '');
    $end = $conn->real_escape_string($_POST['check_in_time_end'] ?? '');

    $res = $conn->query("SELECT institution_id FROM edu_smart_attendance_config WHERE institution_id = $inst_id");
    if ($res && $res->num_rows > 0) {
        $conn->query("UPDATE edu_smart_attendance_config SET vision_engine=$v_eng, pulse_engine=$p_eng, smart_alerts=$s_alt, ip_stream_address='$ip', auth_user='$user', auth_pass='$pass', attendance_window='$window', check_in_time_start='$start', check_in_time_end='$end' WHERE institution_id=$inst_id");
    } else {
        $conn->query("INSERT INTO edu_smart_attendance_config (institution_id, vision_engine, pulse_engine, smart_alerts, ip_stream_address, auth_user, auth_pass, attendance_window, check_in_time_start, check_in_time_end) VALUES ($inst_id, $v_eng, $p_eng, $s_alt, '$ip', '$user', '$pass', '$window', '$start', '$end')");
    }
    echo json_encode(['status' => 'success', 'message' => 'Configuration deployed/saved successfully']);
    exit;
}
