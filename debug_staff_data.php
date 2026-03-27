<?php
require_once 'api_common.php';
$res = $conn->query("SELECT id, full_name, role, user_type FROM edu_users WHERE institution_id = $inst_id AND role NOT IN ('student', 'parent') ORDER BY full_name ASC LIMIT 20");
$data = [];
while($row = $res->fetch_assoc()) {
    $data[] = $row;
}
echo json_encode($data, JSON_PRETTY_PRINT);
