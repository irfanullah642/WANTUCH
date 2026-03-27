<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');
$res = $conn->query("SELECT id, username, role, institution_id, full_name FROM edu_users WHERE username LIKE '%irfan%' LIMIT 10");
while($row = $res->fetch_assoc()) {
    print_r($row);
}
?>
