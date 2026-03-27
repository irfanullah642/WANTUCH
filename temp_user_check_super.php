<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');
$res = $conn->query("SELECT id, username, role, institution_id FROM edu_users WHERE role LIKE '%super%'");
while($row = $res->fetch_assoc()) {
    print_r($row);
}
?>
