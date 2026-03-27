<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');
$res = $conn->query("SELECT owner_id FROM edu_institutions WHERE id = 84");
if ($row = $res->fetch_assoc()) {
    print_r($row);
}
?>
