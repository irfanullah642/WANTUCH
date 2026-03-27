<?php
require_once 'C:/xampp/htdocs/WANTUCH/includes/db.php';
$res = $conn->query("SELECT id, name, owner_id FROM edu_institutions WHERE name LIKE '%RANA College%'");
$rows = [];
while ($row = $res->fetch_assoc()) $rows[] = $row;
echo json_encode($rows);
