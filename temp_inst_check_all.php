<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');
$res = $conn->query("SELECT id, name FROM edu_institutions");
while($row = $res->fetch_assoc()) {
    print_r($row);
}
?>
