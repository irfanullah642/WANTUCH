<?php
require 'C:/xampp/htdocs/WANTUCH/includes/db.php';
header('Content-Type: text/plain');
$res = $conn->query("DESCRIBE edu_users");
if ($res) {
    echo "EDU_USERS COLUMNS:\n";
    while($r = $res->fetch_assoc()) {
        echo $r['Field'] . " (".$r['Type'].")\n";
    }
} else {
    echo "Error describing edu_users: " . $conn->error . "\n";
}

$res2 = $conn->query("DESCRIBE users");
if ($res2) {
    echo "\nUSERS COLUMNS:\n";
    while($r = $res2->fetch_assoc()) {
        echo $r['Field'] . " (".$r['Type'].")\n";
    }
} else {
    echo "Error describing users: " . $conn->error . "\n";
}
?>
