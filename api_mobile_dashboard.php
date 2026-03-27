<?php
/**
 * Main Router for Mobile API
 * Dispatches requests to role-specific modules.
 */
require_once 'api_common.php';

// Check if Action is in Whitelist or requires Institution context
// If we have a user_id, api_common.php will try to resolve the institution automatically
if (!in_array($action, ['GET_PORTFOLIO', 'LOGIN', 'GET_INSTITUTIONS'])) {
    if (!$inst_id && empty($edu_user_id)) {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Unauthorized: Institution context required.']);
        exit;
    }
}

// 1. Shared / Universal Actions
if ($action === 'LOGIN') {
    // Forward to legacy for now as login has complex logic
    include 'api_dashboard_legacy.php';
    exit;
} elseif ($action === 'GET_PORTFOLIO') {
    include 'api_dashboard_legacy.php';
    exit;
}

// 2. Role-Based Routing
if ($is_mgmt) {
    if (in_array($role_lower, ['super_admin', 'super admin', 'developer'])) {
        include 'api_super_admin.php';
    }
    include 'api_admin.php';
} elseif (strpos($role_lower, 'staff') !== false || strpos($role_lower, 'teacher') !== false) {
    require_once 'api_staff.php';
} elseif (strpos($role_lower, 'student') !== false) {
    require_once 'api_student.php';
} elseif (strpos($role_lower, 'parent') !== false) {
    require_once 'api_parent.php';
} else {
    // If no role, fallback to legacy to prevent breakage
    if (empty($role_lower)) {
        include 'api_dashboard_legacy.php';
    } else {
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => "Access Denied: Role '$role_lower' not recognized."]);
        exit;
    }
}

// 3. Final Fallback: If the role file didn't exit, try legacy
// This ensures that any action not yet migrated to role files still works.
include 'api_dashboard_legacy.php';
