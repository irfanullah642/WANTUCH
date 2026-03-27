<?php
require_once 'C:/xampp/htdocs/WANTUCH/includes/db.php';
$res = $conn->query("SELECT * FROM edu_institutions WHERE name LIKE '%GMS PEOCHAR%'");
$rows = [];
if ($res) while ($row = $res->fetch_assoc()) $rows[] = $row;
echo json_encode($rows);
