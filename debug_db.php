<?php
require_once 'api_common.php';
$username = $_SESSION['username'] ?? $_SESSION['user_username'] ?? $_SESSION['user'] ?? '';
$uid = (int)($_SESSION['user_id'] ?? 0);

echo "DEBUG INFO:\n";
echo "USERNAME (Session): $username\n";
echo "USER ID (Session): $uid\n";

if ($uid > 0 || $username) {
    if (!$uid) {
        $uq = $conn->query("SELECT id, user_type FROM users WHERE username = '$username' LIMIT 1");
        if ($uq && $row = $uq->fetch_assoc()) {
            $uid = (int)$row['id'];
            echo "RESOLVED USER ID: $uid (Type: {$row['user_type']})\n";
        }
    }

    echo "\nLINKED INSTITUTIONS (Owner):\n";
    $q1 = $conn->query("SELECT id, name, owner_id FROM edu_institutions WHERE owner_id = $uid");
    while($row = $q1->fetch_assoc()) echo "[#{$row['id']}] {$row['name']} (Owner: {$row['owner_id']})\n";

    echo "\nASSOCIATED INSTITUTIONS (edu_users):\n";
    $q2 = $conn->query("SELECT DISTINCT i.id, i.name, u.role, u.username FROM edu_institutions i JOIN edu_users u ON i.id = u.institution_id WHERE u.username = '$username' AND u.role IN ('super_admin', 'super admin', 'super', 'developer')");
    while($row = $q2->fetch_assoc()) echo "[#{$row['id']}] {$row['name']} (Role: {$row['role']} for Username: {$row['username']})\n";

    echo "\nALL INSTITUTIONS IN DB:\n";
    $q3 = $conn->query("SELECT id, name, owner_id FROM edu_institutions LIMIT 10");
    while($row = $q3->fetch_assoc()) echo "[#{$row['id']}] {$row['name']} (Owner: {$row['owner_id']})\n";
    
    echo "\nUSER TBL DATA (for role super_admin):\n";
    $q4 = $conn->query("SELECT id, username, user_type FROM users WHERE user_type LIKE '%super%' OR user_type = 'developer' LIMIT 10");
    while($row = $q4->fetch_assoc()) echo "[#{$row['id']}] User: {$row['username']} (Type: {$row['user_type']})\n";
} else {
    echo "NO USER FOUND IN SESSION.\n";
}
?>
