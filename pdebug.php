<?php
require_once 'api_common.php';
$username = $_SESSION['username'] ?? $_SESSION['user_username'] ?? $_SESSION['user'] ?? '';
$uid = (int)($_SESSION['user_id'] ?? 0);

$file = 'portfolio_debug.log';
$log = "DEBUG PORTFOLIO REQUEST\n";
$log .= "URL: " . $_SERVER['REQUEST_URI'] . "\n";
$log .= "USER ID: $uid\n";
$log .= "USERNAME: $username\n";

if ($uid > 0 || $username) {
    if (!$uid) {
        $uq = $conn->query("SELECT id FROM users WHERE username = '$username' LIMIT 1");
        $uid = (int)($uq ? $uq->fetch_assoc()['id'] : 0);
    }
    
    $q = $conn->query("SELECT id, name, owner_id FROM edu_institutions WHERE owner_id = $uid OR id IN (SELECT institution_id FROM edu_users WHERE username = '$username')");
    $log .= "FOUND SCHOOLS:\n";
    while($row = $q->fetch_assoc()) {
        $log .= " [#{$row['id']}] {$row['name']} (Owner: {$row['owner_id']})\n";
    }

    $all = $conn->query("SELECT id, name, owner_id FROM edu_institutions LIMIT 5");
    $log .= "\nSAMPLES FROM ALL SCHOOLS:\n";
    while($row = $all->fetch_assoc()) {
        $log .= " [#{$row['id']}] {$row['name']} (Owner: {$row['owner_id']})\n";
    }
}
file_put_contents($file, $log);
echo "Logged to $file";
?>
