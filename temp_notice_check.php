<?php
require_once 'C:/xampp/htdocs/WANTUCH/includes/db.php';
$res = $conn->query("SELECT * FROM edu_notices WHERE title LIKE '%Eid Ul Adha%'");
$rows = [];
while ($row = $res->fetch_assoc()) $rows[] = $row;
echo json_encode($rows);
