<?php
chdir('C:/xampp/htdocs/WANTUCH/modules/education');
ini_set('display_errors', 1);
error_reporting(E_ALL);
$_SESSION['user_id'] = 1;
$_SESSION['edu_user_id'] = 1;
$_REQUEST['action'] = 'GET_FEE_DASHBOARD';
$_REQUEST['institution_id'] = 64;
$_POST['institution_id'] = 64;
require 'api_mobile_dashboard.php';
?>
