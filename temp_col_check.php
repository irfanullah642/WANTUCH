<?php
require_once 'C:/xampp/htdocs/WANTUCH/includes/db.php';
$res = $conn->query("SHOW COLUMNS FROM edu_notices");
$rows = [];
while ($row = $res->fetch_assoc()) $rows[] = $row;
echo json_encode($rows);
