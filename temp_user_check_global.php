<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');
$res = $conn->query("SELECT id, username, role, full_name FROM users WHERE username = 'irfanullah' LIMIT 1");
if ($row = $res->fetch_assoc()) {
    print_r($row);
} else {
    echo "Not found in users";
}
?>
