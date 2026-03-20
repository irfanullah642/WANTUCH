<?php
require 'C:/xampp/htdocs/WANTUCH/includes/db.php';
$inst_id = 64;
$res = $conn->query("SELECT * FROM edu_transport_charges WHERE institute_id = $inst_id AND status = 'Active'");
if (!$res) { echo "edu_transport_charges Error: " . $conn->error . "\n"; }

$res_f = $conn->query("SELECT id, type_name FROM edu_fee_types WHERE institution_id = $inst_id AND status = 'Active' ORDER BY type_name ASC");
if (!$res_f) { echo "edu_fee_types Error: " . $conn->error . "\n"; }

$res_c = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY id ASC");
if (!$res_c) { echo "edu_classes Error: " . $conn->error . "\n"; }

echo "DONE.";
?>
