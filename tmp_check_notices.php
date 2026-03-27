<?php
require_once 'api_common.php';
$inst_id = 9; // Let's guess an ID or check what they are
$role = 'student';
$uid = 0;

$conn = new mysqli($host, $user, $pass, $dbname);

$res = $conn->query("SELECT * FROM edu_notices WHERE institution_id = $inst_id AND (expiry_date >= CURDATE() OR expiry_date = '0000-00-00') ORDER BY created_at DESC LIMIT 5");
echo "ALL NOTICES FOR INST $inst_id:\n";
while($row = $res->fetch_assoc()) {
    echo "ID: " . $row['id'] . " | Title: " . $row['title'] . " | Students: " . $row['target_students'] . " | Parents: " . $row['target_parents'] . " | Staff: " . $row['target_staff'] . "\n";
}
