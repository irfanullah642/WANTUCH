<?php
$conn = new mysqli('localhost', 'root', '', 'wantuch');
if ($conn->connect_error) die('err');

$username = 'irfanullah';
echo "User: irfanullah\n";
$res1 = $conn->query("SELECT id FROM users WHERE username = '$username'");
$user = $res1->fetch_assoc();
echo "User ID: " . $user['id'] . "\n";

echo "Institutions owned by " . $user['id'] . ":\n";
$res2 = $conn->query("SELECT id, name, owner_id FROM edu_institutions WHERE owner_id = " . $user['id']);
while($row = $res2->fetch_assoc()) {
    print_r($row);
}

echo "\nInstitutions where " . $username . " is staff:\n";
$res3 = $conn->query("SELECT DISTINCT institution_id FROM edu_users WHERE username = '$username'");
while($row = $res3->fetch_assoc()) {
    print_r($row);
}
?>
