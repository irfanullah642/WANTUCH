<?php
require 'C:/xampp/htdocs/WANTUCH/includes/db.php';
header('Content-Type: text/plain');
echo "--- Recent fee entries ---\n";
$res = $conn->query("SELECT * FROM edu_fee_management ORDER BY id DESC LIMIT 5");
if($res) {
    while($r = $res->fetch_assoc()) {
        print_r($r);
    }
}
?>
