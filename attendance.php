<?php
require_once '../../includes/session_config.php';
require_once '../../includes/db.php';
require_once '../../includes/access_helper.php';

// Auto-create table for leave requests if not exists
$sql_create_leave = "CREATE TABLE IF NOT EXISTS `edu_leaves` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `role` varchar(50) NOT NULL,
  `requested_by` varchar(50) NOT NULL DEFAULT 'self',
  `requester_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `reason` text NOT NULL,
  `status` enum('pending','approved','rejected','forwarded_to_parent') NOT NULL DEFAULT 'pending',
  `parent_status` enum('pending','confirmed','rejected') NOT NULL DEFAULT 'pending',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
$conn->query($sql_create_leave);

if (!isset($_SESSION['edu_user_id']) || ($_SESSION['edu_role'] !== 'student' && !has_granular_access('dash_attendance', true))) {
    header("Location: dashboard.php");
    exit;
}

$role = $_SESSION['edu_role'];

if (in_array($role, ['admin', 'super_admin', 'developer', 'guest', 'staff'])) {
    include 'attendance_admin.php';
} elseif ($role == 'student') {
    include 'attendance_student.php';
} else {
    header("Location: dashboard.php");
    exit;
}
?>




