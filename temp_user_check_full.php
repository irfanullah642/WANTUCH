<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');
$res = $conn->query("SHOW COLUMNS FROM edu_users");
while($row = $res->fetch_assoc()) {
    echo $row['Field'] . ", ";
}
echo "\n---\n";
$res = $conn->query("SELECT * FROM edu_users WHERE username = 'irfanullah' LIMIT 1");
if ($row = $res->fetch_assoc()) {
    print_r($row);
}
?>
