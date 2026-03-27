<?php
/**
 * Parent API Actions
 * Related to Children oversight, Fee payment monitoring, and Academic progress.
 */
require_once 'api_common.php';

if ($action === 'GET_DASHBOARD' || $action === 'SWITCH_AND_GET_DASHBOARD') {
    $target_student_id = (int)($_REQUEST['student_id'] ?? 0);
    $parent_cnic = $_SESSION['parent_cnic'] ?? $_SESSION['cnic'] ?? $_SESSION['user_cnic'] ?? '';
    
    // If we're verifying ownership but session doesn't have CNIC, fetch it
    if ($target_student_id > 0 && empty($parent_cnic) && $edu_user_id > 0) {
        $parent_q = $conn->query("SELECT cnic FROM edu_users WHERE id = $edu_user_id LIMIT 1");
        if ($parent_q && $parent_q->num_rows > 0) $parent_cnic = $parent_q->fetch_object()->cnic;
    }

    if ($target_student_id > 0 && !empty($parent_cnic)) {
        // Verify that this child belongs to the parent
        $verify = $conn->query("SELECT id FROM edu_users WHERE id = $target_student_id AND (father_cnic = '$parent_cnic' OR parent_cnic_no = '$parent_cnic') LIMIT 1");
        if ($verify && $verify->num_rows > 0) {
            // Success: Switch context to student for this specific view
            // Note: We don't overwrite the actual session role, just the response role.
            send_dashboard_response('student', $target_student_id, $inst_id, $conn);
            exit;
        }
    }
    
    // Default: Send parent's own dashboard stats/modules
    send_dashboard_response($role, $edu_user_id, $inst_id, $conn);
    exit;
} elseif ($action === 'GET_PARENT_DASHBOARD') {
    ob_clean(); 
    try {
        $parent_id = $edu_user_id;
        
        if ($role_lower !== 'parent') {
            throw new Exception('Access Denied: Parent Role Required');
        }

        // Robust identity fetch
        $parent_cnic = $_SESSION['parent_cnic'] ?? $_SESSION['cnic'] ?? $_SESSION['user_cnic'] ?? '';
        
        if (empty($parent_cnic) && $parent_id > 0) {
            $parent_q = $conn->query("SELECT cnic FROM edu_users WHERE id = $parent_id LIMIT 1");
            if ($parent_q && $parent_q->num_rows > 0) {
                $parent_cnic = $parent_q->fetch_object()->cnic;
            }
        }

        if (empty($parent_cnic)) {
            throw new Exception('Parent identity (CNIC) not found in session.');
        }

        // Fetch parent name from their record or child's father_name
        $parent_name = $_SESSION['user_fullname'] ?? $_SESSION['edu_name'] ?? '';
        if (empty($parent_name)) {
            $name_q = $conn->query("SELECT father_name FROM edu_users WHERE father_cnic = '$parent_cnic' OR parent_cnic_no = '$parent_cnic' LIMIT 1");
            if ($name_q && $name_q->num_rows > 0) {
                $parent_name = $name_q->fetch_object()->father_name;
            }
        }
        if (empty($parent_name)) $parent_name = "Parent (" . substr($parent_cnic, -4) . ")";

        $ongoing_year = date('Y');
        $children_query = "
            SELECT 
                u.id as student_id, u.full_name, u.profile_pic,
                se.class_id, se.section_id, se.academic_year,
                c.name as class_name, s.name as section_name,
                i.id as institution_id, i.name as school_name, i.type as school_type
            FROM edu_users u
            JOIN edu_student_enrollment se ON u.id = se.student_id
            JOIN edu_classes c ON se.class_id = c.id
            JOIN edu_sections s ON se.section_id = s.id
            JOIN edu_institutions i ON c.institution_id = i.id
            WHERE (u.father_cnic = '$parent_cnic' OR u.parent_cnic_no = '$parent_cnic')
              AND u.role = 'student' 
              AND (se.academic_year LIKE '%$ongoing_year%' OR se.status = 'active' OR se.status = 'Active')
            ORDER BY u.full_name ASC
        ";
        
        $children_res = $conn->query($children_query);
        if (!$children_res) throw new Exception("Database Query Error: " . $conn->error);

        $children = [];
        $schools_seen = [];
        $total_dues = 0;

        while ($child = $children_res->fetch_assoc()) {
            $student_id = (int)$child['student_id'];
            $inst_id = (int)$child['institution_id'];
            $schools_seen[$inst_id] = true;

            // Attendance Percentage
            $att_res = $conn->query("SELECT status FROM edu_attendance WHERE student_id = $student_id LIMIT 100");
            $present = 0; $total_marked = 0;
            if ($att_res) {
                while($r = $att_res->fetch_assoc()) {
                    $total_marked++;
                    if($r['status'] == 'P' || $r['status'] == 'Present') $present++;
                }
            }
            $att_pct = ($total_marked > 0) ? round(($present / $total_marked) * 100) : 0;

            // Fee 
            $fee_status = "Paid";
            $child_dues = 0;
            if (class_exists('FeeManager')) {
                try {
                    $fm = new FeeManager($conn, $inst_id);
                    $child_dues = $fm->getProjectedBalance($student_id);
                    if ($child_dues > 0) {
                        $fee_status = "Unpaid";
                        $total_dues += $child_dues;
                    }
                } catch (Throwable $e) {}
            }

            // Pending Work
            $pw_q = $conn->query("SELECT COUNT(*) FROM edu_assignments WHERE class_id = {$child['class_id']} AND section_id = {$child['section_id']} AND due_date >= CURDATE()");
            $pw_count = ($pw_q) ? $pw_q->fetch_row()[0] : 0;

            $children[] = [
                'student_id' => $student_id,
                'full_name' => strtoupper($child['full_name']),
                'school_name' => $child['school_name'],
                'class_name' => $child['class_name'],
                'section_name' => $child['section_name'],
                'att_pct' => $att_pct,
                'fee_status' => $fee_status,
                'dues' => (float)$child_dues,
                'performance' => 'Good', 
                'pending_work' => (string)$pw_count,
                'profile_pic' => $child['profile_pic'] ? $child['profile_pic'] : null,
                'institution_id' => $inst_id
            ];
        }

        $schools_list = implode(',', array_keys($schools_seen)) ?: '0';
        $notice_q = $conn->query("SELECT COUNT(*) FROM edu_notices WHERE institution_id IN ($schools_list) AND (expiry_date >= CURDATE() OR expiry_date = '0000-00-00')");
        $total_notices = ($notice_q) ? $notice_q->fetch_row()[0] : 0;

        echo json_encode([
            'status' => 'success',
            'stats' => [
                'children' => count($children),
                'schools' => count($schools_seen),
                'dues' => number_format($total_dues, 0, '.', ','),
                'notices' => (string)$total_notices,
                'parent_name' => $parent_name
            ],
            'children' => $children
        ]);

    } catch (Throwable $e) {
        echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
    }
    exit;
}
