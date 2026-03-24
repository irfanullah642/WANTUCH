<?php
ob_start();
session_start();
require_once '../../includes/db.php';
require_once '../../includes/access_helper.php';

if (!isset($_SESSION['edu_user_id']) || (!has_granular_access('dash_exams', true) && $_SESSION['edu_role'] != 'student')) {
    header("Location: dashboard.php");
    exit;
}

$inst_id = $_SESSION['edu_institution_id'];
$role = $_SESSION['edu_role'];
$is_staff = in_array($role, ['staff', 'teacher']);
$is_student = ($role == 'student');

// Fetch student data if student
$student_data = null;
$actual_student_id = null;
if ($is_student) {
    $actual_student_id = $_SESSION['edu_user_id'];
    // Allow both 'Active' and 'active'
    $student_data = $conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $actual_student_id AND (status = 'Active' OR status = 'active') ORDER BY id DESC LIMIT 1")->fetch_assoc();
    if($student_data) {
        $student_data['roll_number'] = $actual_student_id; // Use student ID as roll for now
        $student_data['student_id'] = $actual_student_id; // Store actual student ID
    }
}


// Fetch institution name
$inst = $conn->query("SELECT name, logo_path, type FROM edu_institutions WHERE id = $inst_id")->fetch_assoc();

// Fetch staff assignments for filtering
$staff_assigned_ids = [];
$staff_assignments = [];
if($is_staff) {
    $uid = $_SESSION['edu_user_id'];
    $assigned_res = $conn->query("
        SELECT DISTINCT 
            t.class_id, t.section_id, t.subject_id,
            c.name as class_name,
            s.name as section_name,
            sub.name as subject_name
        FROM edu_timetable t
        JOIN edu_classes c ON t.class_id = c.id
        JOIN edu_sections s ON t.section_id = s.id
        JOIN edu_subjects sub ON t.subject_id = sub.id
        WHERE t.staff_id = $uid AND t.institution_id = $inst_id
    ");
    while($r = $assigned_res->fetch_assoc()) {
        $staff_assigned_ids[] = ['class_id' => $r['class_id'], 'section_id' => $r['section_id'], 'subject_id' => $r['subject_id']];
        $staff_assignments[] = $r;
    }
}

// Fetch initial data
$all_classes = [];
if($is_staff) {
    $cids = array_unique(array_column($staff_assigned_ids, 'class_id'));
    if(!empty($cids)) {
        $cid_list = implode(',', $cids);
        $res = $conn->query("SELECT * FROM edu_classes WHERE id IN ($cid_list) AND institution_id = $inst_id ORDER BY name ASC");
        while($row = $res->fetch_assoc()) $all_classes[] = $row;
    }
} else {
    $res = $conn->query("SELECT * FROM edu_classes WHERE institution_id = $inst_id ORDER BY name ASC");
    while($row = $res->fetch_assoc()) $all_classes[] = $row;
}

// Student specific data
$student_data = null;
if($role == 'student') {
    $uid = $_SESSION['edu_user_id'];
    $student_data = $conn->query("SELECT * FROM edu_student_enrollment WHERE student_id = $uid")->fetch_assoc();
}

// NEW: Fetch statistics data for dashboard
$stats_data = [];
if($role != 'student') {
    $where_clause = "e.institution_id = $inst_id";
    if($is_staff) {
        $conditions = [];
        foreach($staff_assigned_ids as $asgn) {
            $conditions[] = "(e.class_id = {$asgn['class_id']} AND e.section_id = {$asgn['section_id']} AND e.subject_id = {$asgn['subject_id']})";
        }
        if(!empty($conditions)) {
            $where_clause .= " AND (" . implode(" OR ", $conditions) . ")";
        } else {
            $where_clause .= " AND 1=0"; // No assignments, see nothing
        }
    }

    // Total exams
    $total_exams = $conn->query("SELECT COUNT(*) as total FROM edu_exams e WHERE $where_clause")->fetch_assoc()['total'];
    
    // Total marks entries
    $total_marks = $conn->query("SELECT COUNT(*) as total FROM edu_exam_marks em 
                                  JOIN edu_exams e ON em.exam_id = e.id 
                                  WHERE $where_clause")->fetch_assoc()['total'];
    
    // Pass percentage
    $pass_stats = $conn->query("SELECT 
                                AVG(CASE WHEN status = 'Pass' THEN 1 ELSE 0 END) * 100 as pass_rate,
                                COUNT(*) as total_students
                                FROM edu_exam_marks em 
                                JOIN edu_exams e ON em.exam_id = e.id 
                                WHERE $where_clause")->fetch_assoc();
    
    // Recent exams
    $recent_exams = [];
    $res = $conn->query("SELECT e.exam_type, e.exam_date, COUNT(DISTINCT em.student_id) as students_count 
                         FROM edu_exams e 
                         LEFT JOIN edu_exam_marks em ON e.id = em.exam_id 
                         WHERE $where_clause 
                         AND e.exam_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                         GROUP BY e.id
                         ORDER BY e.exam_date DESC 
                         LIMIT 5");
    while($row = $res->fetch_assoc()) $recent_exams[] = $row;
    
    // Top classes
    $top_classes = [];
    $res = $conn->query("SELECT 
                        c.name as class_name,
                        s.name as section_name,
                        AVG((em.obtain_marks / e.total_marks) * 100) as avg_percentage,
                        COUNT(DISTINCT em.student_id) as student_count
                        FROM edu_exam_marks em
                        JOIN edu_exams e ON em.exam_id = e.id
                        JOIN edu_classes c ON e.class_id = c.id
                        LEFT JOIN edu_sections s ON e.section_id = s.id
                        WHERE $where_clause
                        GROUP BY e.class_id, e.section_id
                        ORDER BY avg_percentage DESC
                        LIMIT 5");
    while($row = $res->fetch_assoc()) $top_classes[] = $row;

    // Subject performance
    $subject_performance = [];
    $res = $conn->query("SELECT 
                        sub.name as subject_name,
                        AVG((em.obtain_marks / e.total_marks) * 100) as avg_percentage,
                        COUNT(*) as total_attempts,
                        SUM(CASE WHEN em.status = 'Pass' THEN 1 ELSE 0 END) as passed,
                        SUM(CASE WHEN em.status = 'Fail' THEN 1 ELSE 0 END) as failed
                        FROM edu_exam_marks em
                        JOIN edu_exams e ON em.exam_id = e.id
                        JOIN edu_subjects sub ON e.subject_id = sub.id
                        WHERE $where_clause
                        GROUP BY e.subject_id
                        ORDER BY avg_percentage DESC
                        LIMIT 8");
    while($row = $res->fetch_assoc()) $subject_performance[] = $row;

    // Exam type distribution
    $exam_type_dist = [];
    $res = $conn->query("SELECT 
                        exam_type,
                        COUNT(*) as count,
                        AVG(CASE WHEN em.status = 'Pass' THEN 1 ELSE 0 END) * 100 as pass_rate
                        FROM edu_exams e
                        LEFT JOIN edu_exam_marks em ON e.id = em.exam_id
                        WHERE $where_clause
                        GROUP BY exam_type");
    while($row = $res->fetch_assoc()) $exam_type_dist[] = $row;

    // Monthly trend
    $monthly_trend = [];
    $res = $conn->query("SELECT 
                        MONTHNAME(e.exam_date) as month,
                        COUNT(DISTINCT e.id) as exam_count,
                        COUNT(DISTINCT em.student_id) as student_count
                        FROM edu_exams e
                        LEFT JOIN edu_exam_marks em ON e.id = em.exam_id
                        WHERE $where_clause
                        AND YEAR(e.exam_date) = YEAR(CURDATE())
                        GROUP BY MONTH(e.exam_date), MONTHNAME(e.exam_date)
                        ORDER BY MONTH(e.exam_date)");
    while($row = $res->fetch_assoc()) $monthly_trend[] = $row;

    $stats_data = [
        'total_exams' => $total_exams,
        'total_marks' => $total_marks,
        'pass_rate' => round($pass_stats['pass_rate'] ?? 0, 1),
        'total_students' => $pass_stats['total_students'] ?? 0,
        'recent_exams' => $recent_exams,
        'top_classes' => $top_classes,
        'subject_performance' => $subject_performance,
        'exam_type_dist' => $exam_type_dist,
        'monthly_trend' => $monthly_trend
    ];
}

// Fetch available semesters
$available_semesters = ['Semester I', 'Semester II'];
if($role != 'student') {
    $sem_q = $conn->query("SELECT DISTINCT semester FROM edu_exams WHERE institution_id = $inst_id AND semester IS NOT NULL AND semester != ''");
    while($r = $sem_q->fetch_assoc()) {
        if(!in_array($r['semester'], $available_semesters)) {
            $available_semesters[] = $r['semester'];
        }
    }
    sort($available_semesters);
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Exam Management</title>
    <link rel="icon" href="data:,">
    <link rel="stylesheet" href="../../assets/css/style.css">
    <link rel="stylesheet" href="../../assets/vendor/fontawesome/all.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        const USER_ROLE = <?php echo json_encode($role ?? ''); ?>;
        const STUDENT_INFO = <?php echo json_encode($student_data ?? null); ?>;
        const STATS_DATA = <?php echo json_encode($stats_data ?? []); ?>;

        function switchTab(tabId, btn) {
            console.log('Switching to tab:', tabId);
            document.querySelectorAll('.content-sec').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            const target = document.getElementById(tabId);
            if(target) {
                target.classList.add('active');
                if(btn) btn.classList.add('active');
            } else {
                console.warn('Tab target not found:', tabId);
            }
            if(tabId === 'stats-tab' && typeof initializeCharts === 'function') {
                setTimeout(initializeCharts, 100);
            }
        }
    </script>
    <style>
        :root {
            --primary: #3498db;
            --primary-dark: #2980b9;
            --accent: #f1c40f;
            --danger: #e74c3c;
            --success: #2ecc71;
            --bg: #f6f5f0;
            --card-bg: #ffffff;
            --text-main: #1e293b;
            --section-bg: #fdfdfb;
        }

        /* --- THEME SYSTEM (Ultimate Specificity) --- */
        body.theme-tactile, 
        body.theme-tactile.pc-view, 
        body.theme-tactile.force-mobile {
            --bg: #ffffff;
            --card-bg: #ffffff;
            --text-main: #000000;
            --section-bg: #fdfdfb;
            background: #ffffff !important;
            background-color: #ffffff !important;
            background-image: none !important;
            color: #000000 !important;
        }

        /* CRITICAL: Override style.css dark container */
        body.theme-tactile .mobile-container {
            background: transparent !important;
            background-image: none !important;
            box-shadow: none !important;
            border: none !important;
            backdrop-filter: none !important;
            -webkit-backdrop-filter: none !important;
        }
        body.theme-tactile .mobile-container::after { display: none !important; }

        /* Aggressive Black Font Enforcement for Corporate Theme */
        body.theme-tactile *, 
        body.theme-tactile .page-header h2, 
        body.theme-tactile .page-header p,
        body.theme-tactile .module-title,
        body.theme-tactile .stat-value,
        body.theme-tactile .stat-label,
        body.theme-tactile .tab-btn span,
        body.theme-tactile .dashboard-grid *,
        body.theme-tactile .content *,
        body.theme-tactile .module-header span { 
            color: #000000 !important; 
            border-color: rgba(0,0,0,0.12) !important; 
        }
        
        body.theme-tactile .content-sec { background: transparent !important; box-shadow: none !important; border: none !important; backdrop-filter: none !important; }
        
        /* Floating Banners with Powerful 3D Shadows */
        body.theme-tactile .module-group { 
            background: #ffffff !important; 
            border: 1px solid rgba(0,0,0,0.08) !important; 
            box-shadow: 0 45px 120px rgba(0,0,0,0.18), 0 15px 40px rgba(0,0,0,0.12) !important; 
            margin-bottom: 30px !important;
        }
        
        body.theme-tactile .btn, body.theme-tactile .btn * { color: #000000 !important; }
        
        /* Light Blue Glow Active Tab */
        body.theme-tactile .tab-btn.active { 
            background: #f0f7ff !important; 
            border: 2px solid #2563eb !important;
            box-shadow: 0 45px 110px rgba(37, 99, 235, 0.4) !important;
            transform: translateY(-12px) scale(1.05) !important;
        }
        body.theme-tactile .tab-btn.active * { color: #1d4ed8 !important; }

                .btn-edit-mode.active-edit, .btn-edit-mode:active { transform: none !important; box-shadow: none !important; }
        body.theme-tactile .input, body.theme-tactile select { background: #fff !important; color: #000 !important; border: 1px solid rgba(0,0,0,0.2) !important; }
        
        /* Extreme 3D Pop Out Cards */
        body.theme-tactile .spongy-card { 
            background: #ffffff !important; 
            box-shadow: 0 70px 150px rgba(0, 0, 0, 0.22), 0 20px 50px rgba(0, 0, 0, 0.12) !important; 
        }
        body.theme-tactile .stat-card { 
            background: #ffffff !important; 
            box-shadow: 0 70px 160px rgba(0,0,0,0.22), 0 25px 55px rgba(0,0,0,0.12) !important; 
            border: 1px solid rgba(0,0,0,0.1) !important; 
        }
        body.theme-tactile .chart-container { background: #fff !important; border: 1px solid rgba(0,0,0,0.1) !important; box-shadow: 0 40px 100px rgba(0,0,0,0.12) !important; }
        body.theme-tactile .modal-content-inner { background: #fff !important; box-shadow: 0 120px 300px rgba(0,0,0,0.5) !important; color: #000000 !important; }
        body.theme-tactile .modal-content-inner * { color: #000000 !important; }
        
        body.theme-andigo {
            --bg: #020617;
            --card-bg: rgba(30, 41, 59, 0.7);
            --text-main: #f8fafc;
            --section-bg: rgba(30, 41, 59, 0.4);
            background: #020617 !important;
            background-color: #020617 !important;
            color: #f8fafc !important;
        }
        body.theme-andigo .content-sec { background: transparent !important; border: none !important; box-shadow: none !important; }
        body.theme-andigo * { color: #f8fafc; border-color: rgba(255,255,255,0.1); }
        body.theme-andigo .input, body.theme-andigo select { 
            background: rgba(0,0,0,0.3) !important; 
            color: white !important; 
            border: 1px solid rgba(255,255,255,0.1) !important;
        }
        body.theme-andigo .modal-content-inner { background: #1e293b !important; color: #fff !important; }
        body.theme-andigo .stat-card { background: rgba(30, 41, 59, 0.6) !important; border: 1px solid rgba(255,255,255,0.1) !important; }
        body.theme-andigo .chart-container { background: rgba(30, 41, 59, 0.4) !important; border: 1px solid rgba(255,255,255,0.1) !important; }
        body.theme-andigo .hall-card-bg { background: rgba(255,255,255,0.05) !important; }

        /* General Styling */
        body { 
            font-family: 'Plus Jakarta Sans', sans-serif;
            transition: all 0.3s ease;
            margin: 0;
            padding: 0;
        }

        /* Spongy Card Styling */
        .spongy-card {
            background: var(--card-bg);
            border: 1px solid rgba(255, 213, 161, 0.3);
            border-radius: 20px;
            box-shadow: 0 35px 80px rgba(0, 0, 0, 0.2), 0 10px 20px rgba(0, 0, 0, 0.1);
            position: relative;
            overflow: visible;
            transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }

        body.theme-andigo .spongy-card {
            box-shadow: 0 20px 45px rgba(0, 0, 0, 0.4), 0 8px 12px rgba(0, 0, 0, 0.2), inset 0 1px 2px rgba(255, 255, 255, 0.1) !important;
        }

        /* Hardware Tube Styling */
        .hardware-tube {
            position: absolute;
            top: 15%;
            bottom: 15%;
            width: 8px;
            border-radius: 4px;
            background: linear-gradient(90deg, #555, #eee, #444);
            box-shadow: 0 2px 4px rgba(0,0,0,0.5), inset 0 -2px 4px rgba(0,0,0,0.3);
            z-index: 10;
        }
        .hardware-tube.left { left: 8px; }
        .hardware-tube.right { right: 8px; }
        .hardware-tube::before, .hardware-tube::after {
            content: '';
            position: absolute;
            left: 1px;
            right: 1px;
            height: 3px;
            background: #1a1a1a;
            border-radius: 50%;
            box-shadow: inset 0 1px 2px rgba(0,0,0,0.8);
        }
        .hardware-tube::before { top: 2px; }
        .hardware-tube::after { bottom: 2px; }

        /* Shared Components */
        .page-header { background: transparent; padding: 20px; display: flex; align-items: center; justify-content: space-between; position: relative; z-index: 100; }
        .tabs-row { display: flex; gap: 15px; padding: 25px 20px 20px 20px; margin: -10px 0 -10px 0; overflow-x: auto; scrollbar-width: none; position: relative; z-index: 100; }
        .tabs-row::-webkit-scrollbar { display: none; }

        .tab-btn {
            flex: 1;
            min-width: 130px;
            padding: 20px 10px !important;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 10px;
            cursor: pointer;
            transition: all 0.3s ease;
            text-align: center;
            border: none !important;
        }
        .tab-btn i { font-size: 20px; }
        .tab-btn span { font-weight: 800; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; }

        .tab-btn.active {
            background: linear-gradient(135deg, #3498db, #2980b9) !important;
            color: white !important;
            transform: translateY(-5px) !important;
            box-shadow: 0 15px 35px rgba(52, 152, 219, 0.5) !important;
        }
        .tab-btn.active * { color: white !important; }

        .content { padding: 20px; position: relative; z-index: 50; }
        .content-sec { display: none !important; margin-bottom: 50px; }
        .content-sec.active { display: block !important; animation: fadeIn 0.4s ease; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

        /* Platform Independent Layout */
        .mobile-container {
            max-width: none !important;
            width: 100% !important;
            min-height: 100vh;
            margin: 0 !important;
            border: none !important;
            box-shadow: none !important;
        }

        .header-actions {
            display: flex;
            gap: 20px;
            align-items: center;
        }

        .header-icon-btn {
            background: none !important;
            border: none !important;
            color: var(--primary) !important;
            font-size: 20px;
            cursor: pointer;
            padding: 5px;
            transition: 0.3s;
            box-shadow: none !important;
            width: auto !important;
            height: auto !important;
        }
        .header-icon-btn:hover {
            transform: scale(1.2);
            opacity: 0.8;
        }

        .simple-sub-tab {
            padding: 8px 15px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 13px;
            font-weight: 800;
            color: rgba(255,255,255,0.6);
            transition: 0.3s;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .simple-sub-tab:hover { background: rgba(255,255,255,0.1); color: white; }
        .simple-sub-tab.active { background: var(--primary); color: white; box-shadow: 0 5px 15px rgba(52, 152, 219, 0.3); }

        body.theme-tactile .header-icon-btn { color: #000 !important; }
        body.theme-tactile .simple-sub-tab { color: rgba(0,0,0,0.6) !important; }
        body.theme-tactile .simple-sub-tab:hover { color: #000 !important; }
        body.theme-tactile .simple-sub-tab.active { background: var(--primary) !important; color: white !important; }
        body.theme-andigo .header-icon-btn { color: #fff !important; }

        /* Module Group & Form Standardization */
        .module-group {
            background: var(--section-bg);
            border-radius: 20px;
            border: 1px solid rgba(0,0,0,0.05);
            margin-bottom: 20px;
            overflow: hidden;
            box-shadow: 0 10px 30px rgba(0,0,0,0.05);
        }
        .module-header {
            padding: 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
            background: rgba(0,0,0,0.02);
            transition: 0.3s;
        }
        .module-header:hover { background: rgba(0,0,0,0.05); }
        .module-title { font-weight: 800; font-size: 16px; display: flex; align-items: center; gap: 12px; }
        .module-content { padding: 20px; display: none; }
        .module-content.expanded { display: block; }
        .chevron { transition: 0.3s; opacity: 0.5; }

        .filter-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px; }
        .input, select { 
            width: 100%; 
            height: 50px; 
            padding: 0 15px; 
            border-radius: 12px; 
            border: 1px solid rgba(0,0,0,0.1); 
            font-weight: 700; 
            font-size: 14px; 
            box-shadow: none !important;
        }
        
        .btn {
            height: 50px;
            padding: 0 25px !important;
            border-radius: 12px;
            border: none;
            font-weight: 800;
            cursor: pointer;
            text-transform: uppercase;
            letter-spacing: 1px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            transition: 0.3s;
        }
        .btn:hover { transform: translateY(-1px); box-shadow: 0 4px 10px rgba(0,0,0,0.15); }

        .smart-table { width: 100%; border-collapse: separate; border-spacing: 0 10px; }
        .smart-table tr { background: rgba(0,0,0,0.05); transition: 0.3s; }
        .smart-table td, .smart-table th { padding: 15px; }
        .smart-table td:first-child { border-radius: 15px 0 0 15px; }
        .smart-table td:last-child { border-radius: 0 15px 15px 0; }

        /* Statistics Dashboard Styles */
        .filter-controls { display: flex; flex-wrap: wrap; gap: 15px; margin-bottom: 25px; align-items: center; }
        .filter-controls select { width: auto; min-width: 180px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; margin-bottom: 25px; }
        
        .stat-card {
            background: var(--card-bg);
            border-radius: 20px;
            padding: 25px;
            position: relative;
            overflow: hidden;
            border: 1px solid rgba(255,255,255,0.05);
            transition: all 0.3s ease;
        }
        .stat-card:hover { transform: translateY(-5px); box-shadow: 0 15px 35px rgba(0,0,0,0.2); }
        .stat-icon { position: absolute; top: 20px; right: 20px; font-size: 40px; opacity: 0.2; color: var(--primary); }
        .stat-value { font-size: 32px; font-weight: 900; margin-bottom: 5px; color: var(--text-main); }
        .stat-label { font-size: 13px; font-weight: 700; opacity: 0.6; text-transform: uppercase; letter-spacing: 1px; }
        .stat-change { font-size: 12px; margin-top: 10px; font-weight: 700; display: flex; align-items: center; gap: 5px; }
        .stat-change.positive { color: #2ecc71; }
        .stat-change.negative { color: #e74c3c; }

        .chart-container {
            background: var(--section-bg);
            border-radius: 24px;
            padding: 25px;
            margin-bottom: 25px;
            border: 1px solid rgba(0,0,0,0.05);
        }
        .chart-title { font-size: 16px; font-weight: 800; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; color: var(--text-main); }
        
        .stats-table { width: 100%; border-collapse: collapse; }
        .stats-table th { text-align: left; padding: 15px; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.5); }
        .stats-table td { padding: 15px; font-size: 14px; border-bottom: 1px solid rgba(255,255,255,0.05); }
        
        .progress-bar { height: 8px; background: rgba(0,0,0,0.1); border-radius: 4px; overflow: hidden; width: 100%; }
        .progress-fill { height: 100%; border-radius: 4px; }
        .progress-fill.high { background: #2ecc71; }
        .progress-fill.medium { background: #f1c40f; }
        .progress-fill.low { background: #e74c3c; }

        .insight-card {
            background: rgba(52, 152, 219, 0.1);
            border-left: 4px solid var(--primary);
            padding: 15px;
            border-radius: 0 12px 12px 0;
            margin-bottom: 15px;
        }
        .insight-card h4 { margin: 0 0 5px 0; font-size: 14px; color: var(--primary); }
        .insight-card p { margin: 0; font-size: 13px; opacity: 0.8; line-height: 1.4; }

        .stats-refresh-btn { 
            background: var(--primary); color: white; border: none; padding: 10px 20px; border-radius: 12px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s;
        }
        .stats-refresh-btn:hover { background: var(--primary-dark); filter: brightness(1.1); }

        .badge { padding: 5px 10px; border-radius: 8px; font-size: 11px; font-weight: 800; text-transform: uppercase; }
        .badge-success { background: rgba(46, 204, 113, 0.2); color: #2ecc71; }
        .badge-info { background: rgba(52, 152, 219, 0.2); color: #3498db; }

        /* Print Override */
        @media print {
            
            .dmc-sheet-container { 
                page-break-after: always !important; 
                break-after: page !important; 
                margin: 0 !important; 
                padding: 0 !important; 
            }
            .dmc-sheet-container:last-child { 
                page-break-after: auto !important; 
                break-after: auto !important; 
            }
            .print-hidden { display: none !important; }
            body { background: white !important; color: black !important; }
            .no-print, .nav-fixed-btn, .tabs-row, .filter-controls, .stats-refresh-btn, .page-header, .tabs, .simple-sub-tab, .filter-grid, .design-settings-panel, .module-header { display: none !important; }
            #award_list_print, #dmc_preview { display: block !important; position: relative; width: 100%; }
            .content { padding: 0; }
            .chart-container { border: 1px solid #ddd !important; break-inside: avoid; }
            .dmc-sheet { width: 100% !important; border: none !important; box-shadow: none !important; background: var(--cream-soft) !important; display: block !important; margin: 0 auto !important; min-height: 0 !important; height: auto !important; }
            
            .dmc-body { padding: 15px 30px !important; }
            .student-info-grid { margin-bottom: 15px !important; gap: 15px !important; }
            .register-table { margin-bottom: 15px !important; }
            .register-table th, .register-table td { padding: 8px 15px !important; }
            .summary-banner { margin-bottom: 15px !important; }
            .signature-footer { padding: 15px 30px !important; margin-top: auto !important; gap: 20px !important; }
            @page { size: auto; margin: 0.5cm; }

        }

        /* --- PRESTIGE REGISTER STYLES (Synced from Admission Print) --- */
        .academic-register-header {
            height: 105px; 
            background: white;
            display: flex;
            position: relative;
            z-index: 10;
            overflow: hidden;
            border-bottom: 4px solid #2c52a0;
            margin-bottom: 25px;
            border-radius: 12px 12px 0 0;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
        }

        .header-blue-zone {
            flex: 6;
            background: #2c52a0; /* Side-synced with var(--royal-blue) */
            color: white;
            padding: 30px 50px;
            position: relative;
            clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
            display: flex;
            flex-direction: column;
            justify-content: center;
        }

        .header-blue-zone h1 {
            font-size: 28px;
            font-weight: 900;
            margin: 0;
            color: #f7c600; /* Side-synced with var(--bright-yellow) */
            line-height: 1.1;
            text-transform: uppercase;
        }

        .header-blue-zone p {
            font-size: 13px;
            font-weight: 600;
            margin: 5px 0 0;
            color: white;
            opacity: 0.9;
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        .header-white-zone {
            flex: 4;
            padding: 10px 30px;
            text-align: right;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: flex-end;
            background: white;
            position: relative;
        }

        .register-school-logo {
            width: 85px; 
            height: 85px;
            object-fit: contain;
            position: absolute;
            top: 10px;
            right: 20px;
            z-index: 20;
        }

        .header-register-info {
            z-index: 10;
            margin-right: 100px; 
            text-align: right;
        }

        .header-register-info strong {
            font-size: 22px;
            color: #2c52a0;
            text-transform: uppercase;
            display: block;
            font-weight: 900;
            line-height: 1.1;
        }

        .header-register-info p {
            font-size: 10px;
            margin: 4px 0 0;
            color: #64748b;
            font-weight: 600;
        }

        .header-blue-tip {
            position: absolute;
            bottom: 0;
            right: 0;
            width: 120px;
            height: 50px;
            background: #2c52a0;
            clip-path: polygon(100% 0, 100% 100%, 0 100%);
            z-index: 1;
        }

        /* --- MODERN PRINT SHEET (DMC / RESULT CARD) --- */
        .dmc-sheet {
            width: 100%; 
            max-width: 950px; 
            position: relative;
            background-color: #fbf9f1; /* --cream-soft */
            font-family: 'Inter', sans-serif !important;
            page-break-after: always;
            box-sizing: border-box;
            margin: 0 auto 40px;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
            color: #1e293b;
            box-shadow: 0 45px 120px rgba(0,0,0,0.18);
            border: 1px solid rgba(0,0,0,0.08);
            display: flex;
            flex-direction: column;
            min-height: 29.7cm; /* A4 Ratio */
            overflow: hidden;
            border-radius: 15px;
        }

        .header-modern-print {
            height: 105px; 
            background: white;
            display: flex;
            position: relative;
            z-index: 10;
            overflow: hidden;
            border-bottom: 5px solid #2c52a0; /* var(--royal-blue) */
        }
        
        .header-modern-print .header-blue-zone {
            clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
        }

        .dmc-body { padding: 40px; flex: 1; position: relative; z-index: 5; }
        
        .watermark {
            position: absolute; top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            width: 500px; height: 500px;
            opacity: 0.035; z-index: 0; pointer-events: none;
            filter: grayscale(100%);
        }

        .student-info-grid {
            display: grid; grid-template-columns: 1fr 200px; gap: 30px;
            margin-bottom: 40px;
            position: relative;
            z-index: 10;
        }
        .info-box {
            display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px;
        }
        .info-item {
            padding-bottom: 5px; border-bottom: 1.5px solid rgba(0,0,0,0.08);
        }
        .info-label { font-size: 9px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 1px; }
        .info-value { font-size: 15px; font-weight: 700; color: #0f172a; }

        .photo-box {
            width: 120px; height: 105px; margin-left: auto;
            border: 3px double #2c52a0;
            padding: 3px; background: white;
            display: flex; justify-content: center; align-items: center;
            position: relative;
            box-shadow: 0 10px 20px rgba(0,0,0,0.1);
        }
        .photo-box img { width: 100%; height: 100%; object-fit: cover; }
        .photo-box span { font-size: 8px; font-weight: 900; color: #2c52a0; position: absolute; bottom: 5px; right: 5px; opacity: 0.3; }

        .register-table { 
            width: 100%; border-collapse: collapse; margin-bottom: 30px; 
            border: 2px solid #2c52a0; background: white;
            position: relative; z-index: 10;
        }
        .register-table th { 
            background: #2c52a0; color: white; text-align: left; 
            padding: 12px 15px; font-size: 11px; text-transform: uppercase; 
            font-weight: 800; border: 1px solid rgba(255,255,255,0.1);
        }
        .register-table td { 
            padding: 10px 15px; border: 1px solid #e2e8f0; font-size: 12px; 
            color: #334155; font-weight: 600;
        }
        .register-table tr:nth-child(even) { background: #f8fafc; }

        .summary-banner {
            display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px;
            margin-bottom: 40px;
            position: relative; z-index: 10;
        }
        .status-badge {
            background: white; padding: 15px; border-radius: 12px;
            border: 1.5px solid #e2e8f0; text-align: center;
            box-shadow: 0 5px 15px rgba(0,0,0,0.05);
        }
        .status-badge .label { font-size: 10px; font-weight: 800; color: #64748b; text-transform: uppercase; display: block; margin-bottom: 5px; }
        .status-badge .value { font-size: 18px; font-weight: 900; color: #2c52a0; }

        .signature-footer {
            display: grid; grid-template-columns: repeat(3, 1fr) 100px;
            gap: 40px; align-items: flex-end; margin-top: auto;
            padding: 40px;
            position: relative; z-index: 10;
        }
        .sig-box {
            border-top: 2px solid #94a3b8; padding-top: 10px;
            text-align: center; font-size: 11px; font-weight: 800;
            color: #475569; text-transform: uppercase;
        }

        .qr-footer-box {
            display: flex; flex-direction: column; align-items: center;
            background: white; padding: 5px; border: 1px solid #e2e8f0;
            width: fit-content; margin-left: auto;
        }
        .qr-footer-box img { width: 55px; height: 55px; }
        .qr-footer-label { font-size: 6px; font-weight: 800; color: #2c52a0; text-transform: uppercase; margin-top: 2px; }

        /* --- DESIGN VARIATIONS --- */
        .dmc-sheet.template-minimal { background-color: #ffffff; border-radius: 0; box-shadow: 0 5px 15px rgba(0,0,0,0.05); }
        .dmc-sheet.template-minimal .header-modern-print { height: 100px; border-bottom: 2px solid #000; }
        .dmc-sheet.template-minimal .header-blue-zone { background: #f8fafc; color: #000; clip-path: none; padding: 20px; }
        .dmc-sheet.template-minimal .header-blue-zone h1 { color: #000 !important; }
        .dmc-sheet.template-minimal .header-blue-tip { display: none; }
        .dmc-sheet.template-minimal .register-table { border: 1px solid #000; }
        .dmc-sheet.template-minimal .register-table th { background: #f1f5f9; color: #000; border: 1px solid #000; }

        .dmc-sheet.template-classic { background-color: #fff; border: 10px double #2c52a0; border-radius: 0; }
        .dmc-sheet.template-classic .header-modern-print { height: auto; flex-direction: column; border: none; text-align: center; }
        .dmc-sheet.template-classic .header-blue-zone { clip-path: none; background: none; color: #1e293b; text-align: center; padding: 20px 0; }
        .dmc-sheet.template-classic .header-blue-zone h1 { font-family: 'Cormorant Garamond', serif; font-size: 36px; color: #2c52a0 !important; }
        .dmc-sheet.template-classic .header-white-zone { text-align: center; align-items: center; padding: 0; }
        .dmc-sheet.template-classic .register-school-logo { position: static; margin: 10px auto; width: 100px; height: 100px; }
        .dmc-sheet.template-classic .header-register-info { margin-right: 0; text-align: center; }
        .dmc-sheet.template-classic .header-blue-tip { display: none; }

        /* Theme Color Overrides */
        .color-maroon .header-modern-print, .color-maroon .header-blue-zone, .color-maroon .header-blue-tip, .color-maroon .register-table, .color-maroon .register-table th { border-color: #800000 !important; }
        .color-maroon .header-blue-zone, .color-maroon .header-blue-tip, .color-maroon .register-table th { background-color: #800000 !important; }
        .color-maroon .header-register-info strong, .color-maroon .status-badge .value, .color-maroon .qr-footer-label { color: #800000 !important; }

        .color-emerald .header-modern-print, .color-emerald .header-blue-zone, .color-emerald .header-blue-tip, .color-emerald .register-table, .color-emerald .register-table th { border-color: #065f46 !important; }
        .color-emerald .header-blue-zone, .color-emerald .header-blue-tip, .color-emerald .register-table th { background-color: #065f46 !important; }
        .color-emerald .header-register-info strong, .color-emerald .status-badge .value, .color-emerald .qr-footer-label { color: #065f46 !important; }

        .color-obsidian .header-modern-print, .color-obsidian .header-blue-zone, .color-obsidian .header-blue-tip, .color-obsidian .register-table, .color-obsidian .register-table th { border-color: #0f172a !important; }
        .color-obsidian .header-blue-zone, .color-obsidian .header-blue-tip, .color-obsidian .register-table th { background-color: #0f172a !important; }
        .color-obsidian .header-register-info strong, .color-obsidian .status-badge .value, .color-obsidian .qr-footer-label { color: #0f172a !important; }

        /* Customization Controls */
        .design-settings-panel {
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 12px;
            padding: 15px;
            margin-bottom: 20px;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 15px;
        }
        .design-settings-panel label { font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; margin-bottom: 5px; display: block; }
        .design-settings-panel select { background: white; border: 1.5px solid #e2e8f0; border-radius: 8px; padding: 8px; font-size: 12px; font-weight: 600; width: 100%; }

        /* Mobile Responsive Styles */
        @media (max-width: 768px) {
            .page-header {
                padding: 15px 10px !important;
                gap: 10px !important;
            }
            
            .hardware-tube {
                width: 20px !important;
                height: 80% !important;
            }
            
            .hardware-tube::before,
            .hardware-tube::after {
                width: 14px !important;
                height: 14px !important;
            }
            
            .header-icon-btn {
                width: 40px !important;
                height: 40px !important;
                font-size: 16px !important;
            }
            
            .spongy-card.tab-btn {
                min-width: 150px !important;
                padding: 12px 10px !important;
                font-size: 13px !important;
            }
            
            .filter-grid {
                grid-template-columns: 1fr !important;
                gap: 10px !important;
            }
        }

        @media (max-width: 480px) {
            .page-header {
                padding: 12px 8px !important;
            }
            
            .hardware-tube {
                display: none !important; /* Hide tubes on very small screens */
            }
            
            .spongy-card.tab-btn {
                min-width: 100% !important;
                font-size: 12px !important;
            }
            
            .header-icon-btn {
                width: 36px !important;
                height: 36px !important;
            }
        }

    </style>
<script>
        function printStudentDoc(containerId) {
            let container = document.getElementById(containerId);
            if(!container || container.innerHTML.trim() === '') {
                alert('Please generate the document first before printing!');
                return;
            }
            
            let style = document.createElement('style');
            style.innerHTML = `
                @media print {
                    body * { visibility: hidden !important; }
                    #${containerId}, #${containerId} * { visibility: visible !important; }
                    #${containerId} { 
                        position: absolute !important; 
                        left: 0 !important; 
                        top: 0 !important; 
                        width: 100vw !important; 
                        margin: 0 !important; 
                        padding: 0 !important; 
                    }
                    .no-print { display: none !important; }
                    html, body { height: auto !important; margin: 0 !important; overflow: visible !important; }
                    @page { margin: 0; }
                }
            `;
            document.head.appendChild(style);
            
            setTimeout(() => {
                window.print();
                setTimeout(() => style.remove(), 500);
            }, 100);
        }
</script>
</head>
<body class="theme-tactile">
    <script>
        function initTheme() {
            let savedTheme = localStorage.getItem('dashboard-theme') || localStorage.getItem('edu_exam_theme') || 'theme-tactile';
            if (savedTheme === 'theme-indigo') savedTheme = 'theme-andigo'; // Migration
            
            document.body.classList.remove('theme-tactile', 'theme-andigo');
            document.body.classList.add(savedTheme);
            updateThemeIcons(savedTheme);
        }
        function toggleTheme() {
            const themes = ['theme-tactile', 'theme-andigo'];
            let currentIdx = themes.findIndex(t => document.body.classList.contains(t));
            let nextIdx = (currentIdx + 1) % themes.length;
            let nextTheme = themes[nextIdx];
            
            document.body.classList.remove(...themes);
            document.body.classList.add(nextTheme);
            localStorage.setItem('dashboard-theme', nextTheme);
            updateThemeIcons(nextTheme);
        }
        function updateThemeIcons(theme) {
            const btns = document.querySelectorAll('.theme-toggle-btn-target');
            const icon = (theme === 'theme-andigo') ? 'fas fa-moon' : 'fas fa-palette';
            btns.forEach(b => {
                b.innerHTML = `<i class="${icon}"></i>`;
            });
        }
        initTheme();
    </script>
    
    <?php if($is_student): ?>
    <!-- STUDENT VIEW -->
    <div class="mobile-container">
        <div class="academic-register-header">
            <div class="header-blue-zone">
                <div style="display:flex; align-items:center; gap:10px; margin-bottom:10px;">
                    <button onclick="window.location.href='dashboard.php'" title="Back to Dashboard" class="theme-btn-3d" style="background:rgba(255,255,255,0.1) !important; color:white !important; border:1px solid rgba(255,255,255,0.2) !important; width: 28px !important; height: 28px !important; min-width: 28px !important; padding: 0 !important; font-size: 10px !important;">
                        <i class="fas fa-chevron-left"></i>
                    </button>
                    <button onclick="toggleTheme()" class="theme-btn-3d theme-toggle-btn-target" style="background:rgba(255,255,255,0.1) !important; color:white !important; border:1px solid rgba(255,255,255,0.2) !important; width: 28px !important; height: 28px !important; min-width: 28px !important; padding: 0 !important; font-size: 10px !important; margin-left: 5px;">
                        <i class="fas fa-palette"></i>
                    </button>
                    <h1><?php echo htmlspecialchars($inst['name']); ?></h1>
                </div>
                <p>OFFICIAL STUDENT PORTAL</p>
            </div>
            <div class="header-white-zone">
                <?php 
                $logo_src = !empty($inst['logo_path']) ? "../../" . $inst['logo_path'] : "https://via.placeholder.com/150?text=LOGO";
                ?>
                <img src="<?php echo $logo_src; ?>" class="register-school-logo" alt="Logo">
                <div class="header-register-info">
                    <strong>EXAMINATION RECORDS</strong>
                    <p>VERIFIED ACADEMIC TRANSCRIPT</p>
                    <p><?php echo date('F d, Y'); ?></p>
                </div>
                <div class="header-blue-tip"></div>
            </div>
        </div>
        
        <!-- Simplified Student Results Section -->
        <?php if(!$student_data): ?>
            <div class="spongy-card" style="margin: 20px; padding: 25px; text-align: center; color: #e74c3c;">
                <i class="fas fa-exclamation-circle" style="font-size: 48px; margin-bottom: 15px;"></i>
                <h3 style="margin: 0; font-weight: 800;">No Active Enrollment Found</h3>
                <p>We could not find your active class enrollment. Please contact the administration.</p>
                <div style="margin-top: 15px; font-size: 12px; opacity: 0.7;">
                    User ID: <?php echo $_SESSION['edu_user_id']; ?><br>
                    Role: <?php echo $_SESSION['edu_role']; ?>
                </div>
            </div>
        <?php else: ?>
        <div class="spongy-card" style="margin: clamp(10px, 3vw, 20px); padding: clamp(15px, 4vw, 25px);">
            <div style="display: flex; gap: 15px; margin-bottom: 25px; flex-wrap: wrap;">
                <div class="spongy-card tab-btn active" onclick="switchResultSubTab('dmc', this)" style="flex: 1; min-width: 200px; position: relative; cursor: pointer;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-file-alt"></i> <span>DMC</span>
                </div>
                <div class="spongy-card tab-btn" onclick="switchResultSubTab('result_card', this)" style="flex: 1; min-width: 200px; position: relative; cursor: pointer;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-id-card"></i> <span>Result Card</span>
                </div>
                <div class="spongy-card tab-btn" onclick="switchResultSubTab('performance', this)" style="flex: 1; min-width: 200px; position: relative; cursor: pointer;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-chart-bar"></i> <span>Performance</span>
                </div>
                <div class="spongy-card tab-btn" onclick="switchResultSubTab('slips', this)" style="flex: 1; min-width: 200px; position: relative; cursor: pointer;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-ticket-alt"></i> <span>Datasheet & Slips</span>
                </div>
            </div>

            <!-- DMC Sub Tab -->
            <div id="res_dmc" class="res-sub-tab">
                <div class="filter-grid">
                    <select id="dmc_year" class="input">
                         <?php 
                         $y = date('Y');
                         for($i=$y; $i>=$y-2; $i--) echo "<option value='$i'>Session $i</option>";
                         ?>
                    </select>
                    <select id="dmc_type" class="input" style="display:none;">
                         <option value="single">Single Student</option>
                    </select>
                    <select id="dmc_class" class="input" <?php echo $is_student ? 'style="display:none;"' : 'disabled'; ?>>
                         <option value=""><?php echo $student_data ? 'Class' : 'Not Enrolled'; ?></option>
                         <?php 
                         if($student_data) {
                             $cid = $student_data['class_id'];
                             $cname = $conn->query("SELECT name FROM edu_classes WHERE id=$cid")->fetch_assoc();
                             echo "<option value='$cid' selected>{$cname['name']}</option>";
                         }
                         ?>
                    </select>
                    <select id="dmc_section" class="input" onchange="loadResultExams(this.value, 'dmc_exam')" <?php echo $is_student ? 'style="display:none;"' : 'disabled'; ?>>
                         <option value="">Section</option>
                         <?php if($student_data): 
                             $sid = $student_data['section_id'];
                             $sn = $conn->query("SELECT name FROM edu_sections WHERE id=$sid")->fetch_assoc();
                         ?>
                             <option value="<?php echo $sid; ?>" selected><?php echo $sn['name']; ?></option>
                         <?php endif; ?>
                    </select>
                    <select id="dmc_exam" class="input">
                         <option value="">Select Exam</option>
                    </select>
                    <input type="hidden" id="dmc_student_id" value="<?php echo $actual_student_id; ?>">
                    <input type="text" id="dmc_roll" class="input" placeholder="Roll No" value="<?php echo $student_data ? $student_data['roll_number'] : ''; ?>" <?php echo $is_student ? 'style="display:none;"' : 'disabled'; ?>>
                    <div style="display:flex; gap:10px; width:100%;">
                        <button class="btn btn-secondary no-print" onclick="printStudentDoc('dmc_preview')" style="flex:1; background:#64748b; color:#fff;"><i class="fas fa-print"></i> Print</button>
                        <button class="btn btn-primary" onclick="generateDMC()" style="flex:2;"><i class="fas fa-search"></i> Generate</button>
                    </div>
                </div>
                <div id="dmc_preview" style="margin-top:20px;"></div>

                <!-- NEW: Student Design Panel (DMC) -->
                <div class="design-settings-panel" style="margin-top: 20px; border: 1px dashed rgba(52, 152, 219, 0.3); border-radius: 15px; padding: 15px;">
                    <div style="font-size: 10px; font-weight: 800; text-transform: uppercase; color: var(--primary); margin-bottom: 10px; display: flex; align-items: center; gap: 8px;">
                        <i class="fas fa-magic"></i> Premium Customization
                    </div>
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div>
                            <label style="font-size: 9px;">Template</label>
                            <select id="dmc_design_template" class="input !h-[35px] !text-[11px]">
                                <option value="prestige">Prestige Royal</option>
                                <option value="template-minimal">Modern Minimal</option>
                                <option value="template-classic">Academic Classic</option>
                            </select>
                        </div>
                        <div>
                            <label style="font-size: 9px;">Theme</label>
                            <select id="dmc_design_color" class="input !h-[35px] !text-[11px]">
                                <option value="color-default">Royal Blue</option>
                                <option value="color-maroon">Maroon Red</option>
                                <option value="color-emerald">Emerald Green</option>
                            </select>
                        </div>
                        <div>
                            <label style="font-size: 9px;">Watermark</label>
                            <select id="dmc_design_watermark" class="input !h-[35px] !text-[11px]">
                                <option value="show">Show</option>
                                <option value="hide">Hide</option>
                            </select>
                        </div>
                        <input type="hidden" id="dmc_design_sigs" value="3">
                    </div>
                </div>
            </div>

            <!-- Result Card Sub Tab -->
            <div id="res_result_card" class="res-sub-tab" style="display:none;">
                <div class="filter-grid">
                    <select id="rc_year" class="input">
                         <?php 
                         $y = date('Y');
                         for($i=$y; $i>=$y-2; $i--) echo "<option value='$i'>Session $i</option>";
                         ?>
                    </select>
                    <select id="rc_class" class="input" <?php echo $is_student ? 'style="display:none;"' : 'disabled'; ?>>
                         <option value="">Class</option>
                         <?php 
                         if($student_data) {
                             $cid = $student_data['class_id'];
                             $cname = $conn->query("SELECT name FROM edu_classes WHERE id=$cid")->fetch_assoc();
                             echo "<option value='$cid' selected>{$cname['name']}</option>";
                         }
                         ?>
                    </select>
                    <select id="rc_section" class="input" <?php echo $is_student ? 'style="display:none;"' : 'disabled'; ?>>
                         <option value="">Section</option>
                         <?php if($student_data): 
                             $sid = $student_data['section_id'];
                             $sn = $conn->query("SELECT name FROM edu_sections WHERE id=$sid")->fetch_assoc();
                         ?>
                             <option value="<?php echo $sid; ?>" selected><?php echo $sn['name']; ?></option>
                         <?php endif; ?>
                    </select>
                    <input type="hidden" id="rc_student_id" value="<?php echo $actual_student_id; ?>">
                    <input type="text" id="rc_roll" class="input" placeholder="Roll No" value="<?php echo $student_data ? $student_data['roll_number'] : ''; ?>" <?php echo $is_student ? 'style="display:none;"' : 'disabled'; ?>>
                    <div style="display:flex; gap:10px; width:100%;">
                        <button class="btn btn-secondary no-print" onclick="printStudentDoc('rc_preview')" style="flex:1; background:#64748b; color:#fff;"><i class="fas fa-print"></i> Print</button>
                        <button class="btn btn-primary" onclick="generateResultCard()" style="flex:2;"><i class="fas fa-file-invoice"></i> Generate Record</button>
                    </div>
                </div>
                <div id="rc_preview" style="margin-top:20px;"></div>

                <!-- NEW: Student Design Panel (Result Card) -->
                <div class="design-settings-panel" style="margin-top: 20px; border: 1px dashed rgba(52, 152, 219, 0.3); border-radius: 15px; padding: 15px;">
                    <div style="font-size: 10px; font-weight: 800; text-transform: uppercase; color: var(--primary); margin-bottom: 10px; display: flex; align-items: center; gap: 8px;">
                        <i class="fas fa-magic"></i> Premium Customization
                    </div>
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div>
                            <label style="font-size: 9px;">Style</label>
                            <select id="rc_design_template" class="input !h-[35px] !text-[11px]">
                                <option value="prestige">Prestige Royal</option>
                                <option value="template-minimal">Modern Minimal</option>
                                <option value="template-classic">Academic Classic</option>
                            </select>
                        </div>
                        <div>
                            <label style="font-size: 9px;">Color</label>
                            <select id="rc_design_color" class="input !h-[35px] !text-[11px]">
                                <option value="color-default">Royal Blue</option>
                                <option value="color-maroon">Maroon Red</option>
                                <option value="color-emerald">Emerald Green</option>
                            </select>
                        </div>
                        <div>
                            <label style="font-size: 9px;">Watermark</label>
                            <select id="rc_design_watermark" class="input !h-[35px] !text-[11px]">
                                <option value="show">Show</option>
                                <option value="hide">Hide</option>
                            </select>
                        </div>
                        <input type="hidden" id="rc_design_sigs" value="3">
                    </div>
                </div>
            </div>

            <!-- Slips Sub Tab -->
            <div id="res_slips" class="res-sub-tab" style="display:none;">
                 <div class="elegant-filter-bar mb-3" style="border-radius:15px; background:var(--surface);">
                     <select id="slips_exam" class="input" style="background:#fff; font-weight:bold; color:#000;">
                          <option value="">Select Exam</option>
                     </select>
                 </div>
                 <div style="display:flex; flex-wrap:wrap; gap:15px; margin-top:30px;">
                     <button class="btn btn-primary" onclick="generateStudentDatasheet()" style="flex:1; padding:20px; font-size:16px; font-weight:900; box-shadow:0 10px 20px rgba(0,0,0,0.1);"><i class="fas fa-calendar-alt"></i> DOWNLOAD ENTIRE DATASHEET</button>
                     <button class="btn btn-success" onclick="generateStudentRollSlip()" style="flex:1; padding:20px; font-size:16px; font-weight:900; box-shadow:0 10px 20px rgba(0,0,0,0.1);"><i class="fas fa-ticket-alt"></i> DOWNLOAD ROLL NO SLIP</button>
                 </div>
                 <div id="slips_status" style="margin-top:20px; font-weight:800; color:#e74c3c; text-align:center; padding:15px; border-radius:10px; display:none; background:rgba(231, 76, 60, 0.1); font-size:14px; text-transform:uppercase;"></div>
            </div>
            <!-- Performance Sub Tab -->
            <div id="res_performance" class="res-sub-tab" style="display:none;">
                 <div style="display:none;" id="perf_filters">
                    <input type="hidden" id="perf_class" value="<?php echo $student_data['class_id']; ?>">
                    <input type="hidden" id="perf_section" value="<?php echo $student_data['section_id']; ?>">
                    <button></button> <!-- dummy for JS -->
                 </div>
                 <div style="display:flex; flex-wrap:wrap; gap:10px; margin-bottom:20px;">
                    <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('school_toppers')"><i class="fas fa-crown"></i> School Toppers</button>
                    <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('class_toppers')"><i class="fas fa-medal"></i> Class Toppers</button>
                 </div>
                 <div id="perf_preview" style="margin-top:10px;"></div>
            </div>
        </div>
    <?php endif; ?>
    </div>
    <?php else: ?>
    <!-- ADMIN/STAFF VIEW -->
    <div class="mobile-container">
        <div class="page-header">
            <div style="display:flex; align-items:center; gap:15px;">
                <div class="header-icon-btn" onclick="window.location.href='dashboard.php'">
                    <i class="fas fa-arrow-left"></i>
                </div>
                <div style="display:flex; align-items:center; gap:10px;">
                    <i class="fas fa-graduation-cap" style="font-size:32px; color:var(--primary);"></i>
                    <div>
                        <h2 style="margin:0; font-size:22px; font-weight:800; letter-spacing:-0.5px;">Exams <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:14px;"></i></h2>
                        <p style="margin:0; font-size:11px; opacity:0.6; font-weight:800; text-transform:uppercase;"><?php echo $inst['name']; ?></p>
                    </div>
                </div>
            </div>
            <div class="header-actions">
                <div class="header-icon-btn theme-toggle-btn-target" onclick="toggleTheme()">
                    <i class="fas fa-palette"></i>
                </div>
            </div>
        </div>

        <div class="tabs-row">
            <?php 
            $default_tab = ($is_staff) ? 'stats-tab' : (($role == 'student') ? 'results-tab' : 'create-tab');
            
            // INSIGHTS TAB (First for Staff, elsewhere for others)
            if ($is_staff): ?>
                <div data-access-id="exams_tab_stats" class="spongy-card tab-btn <?php echo ($default_tab == 'stats-tab') ? 'active' : ''; ?>" onclick="switchTab('stats-tab', this)">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-chart-line"></i>
                    <span>Insights <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
                </div>
            <?php endif; ?>

            <?php if(has_granular_access('exams_tab_create') || $is_staff): ?>
            <div data-access-id="exams_tab_create" class="spongy-card tab-btn <?php echo ($default_tab == 'create-tab') ? 'active' : ''; ?>" onclick="switchTab('create-tab', this)">
                <div class="hardware-tube left"></div>
                <div class="hardware-tube right"></div>
                <i class="fas fa-calendar-plus"></i>
                <span>Schedule <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
            </div>
            <?php endif; ?>
            
            <?php if(has_granular_access('exams_tab_marks') || $is_staff): ?>
            <div data-access-id="exams_tab_marks" class="spongy-card tab-btn <?php echo ($default_tab == 'marks-tab') ? 'active' : ''; ?>" onclick="switchTab('marks-tab', this)">
                <div class="hardware-tube left"></div>
                <div class="hardware-tube right"></div>
                <i class="fas fa-pen-nib"></i>
                <span>Marks <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
            </div>
            <?php endif; ?>
            
            <?php if(has_granular_access('exams_tab_results') || $is_staff): ?>
            <div data-access-id="exams_tab_results" class="spongy-card tab-btn <?php echo ($default_tab == 'results-tab') ? 'active' : ''; ?>" onclick="switchTab('results-tab', this)">
                <div class="hardware-tube left"></div>
                <div class="hardware-tube right"></div>
                <i class="fas fa-file-invoice"></i>
                <span>Results <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
            </div>
            <?php endif; ?>
            
            <?php if(has_granular_access('exams_tab_stats') && !$is_staff): ?>
            <div data-access-id="exams_tab_stats" class="spongy-card tab-btn <?php echo ($default_tab == 'stats-tab') ? 'active' : ''; ?>" onclick="switchTab('stats-tab', this)">
                <div class="hardware-tube left"></div>
                <div class="hardware-tube right"></div>
                <i class="fas fa-chart-line"></i>
                <span>Insights <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
            </div>
            <?php endif; ?>
            
            <?php if(has_granular_access('exams_tab_hall')): ?>
            <div data-access-id="exams_tab_hall" class="spongy-card tab-btn <?php echo ($default_tab == 'hall-tab') ? 'active' : ''; ?>" onclick="switchTab('hall-tab', this)">
                <div class="hardware-tube left"></div>
                <div class="hardware-tube right"></div>
                <i class="fas fa-university"></i>
                <span>Halls <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
            </div>
            <?php endif; ?>

            <?php if(has_granular_access('exams_tab_settings') && !$is_staff): ?>
            <div data-access-id="exams_tab_settings" class="spongy-card tab-btn <?php echo ($default_tab == 'settings-tab') ? 'active' : ''; ?>" onclick="switchTab('settings-tab', this)">
                <div class="hardware-tube left"></div>
                <div class="hardware-tube right"></div>
                <i class="fas fa-cog"></i>
                <span>Rules <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:10px;"></i></span>
            </div>
            <?php endif; ?>
        </div>

        <div class="content">
            <?php include 'madrasa_lang_script_only.php'; ?>
            
            <?php if($role != 'student'): ?>
            <div id="create-tab" class="content-sec <?php echo ($default_tab == 'create-tab') ? 'active' : ''; ?>">

                <div class="module-group">
                    <div class="module-header" onclick="toggleModule(this)">
                        <span class="module-title"><i class="fas fa-calendar-plus"></i> Schedule New Exam <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:12px;"></i></span>
                        <i class="fas fa-chevron-down chevron"></i>
                    </div>
                    <div class="module-content">
                        <!-- Sub Tabs -->
                        <div style="display:flex; background:rgba(125,125,125,0.1); padding:5px; border-radius:12px; gap:5px; margin-bottom:20px; width:fit-content; border:1px solid rgba(125,125,125,0.1);">
                            <div class="simple-sub-tab active" onclick="switchCreateTab('create_new_form', this)"><i class="fas fa-plus-circle"></i> Create New Exam</div>
                            <div class="simple-sub-tab" onclick="switchCreateTab('view_exam_list', this)"><i class="fas fa-list-ul"></i> View Exam List</div>
                        </div>

                        <!-- Create New Form -->
                        <div id="create_new_form" class="create-sub-tab">
                            <div class="filter-grid">
                                <div>
                                    <label>Exam Type</label>
                                    <select id="e_type" class="input">
                                        <option value="First Term">First Term</option>
                                        <option value="Mid Term">Mid Term</option>
                                        <option value="Final Term">Final Term</option>
                                        <option value="Monthly Test">Monthly Test</option>
                                    </select>
                                </div>
                                <div style="position: relative;">
                                    <label>Semester</label>
                                    <i class="fas fa-plus-circle" onclick="addNewSemester()" style="position: absolute; right: 5px; top: 0; cursor: pointer; color: var(--primary); font-size: 14px; z-index: 10;" title="Add New Semester"></i>
                                    <select id="e_semester" class="input">
                                        <option value="">Select Semester</option>
                                        <?php foreach($available_semesters as $sem): ?>
                                            <option value="<?php echo htmlspecialchars($sem); ?>"><?php echo htmlspecialchars($sem); ?></option>
                                        <?php endforeach; ?>
                                    </select>
                                </div>
                                <div>
                                    <label>Year</label>
                                    <input type="text" id="e_year" class="input" value="<?php echo date('Y'); ?>">
                                </div>
                                <?php if(!$is_staff): ?>
                                <div>
                                    <label>Class</label>
                                    <select id="e_class" class="input" onchange="loadSections(this.value, 'e_section')">
                                        <option value="">Select Class</option>
                                        <?php foreach($all_classes as $c): ?>
                                            <option value="<?php echo $c['id']; ?>"><?php echo $c['name']; ?></option>
                                        <?php endforeach; ?>
                                    </select>
                                </div>
                                <div>
                                    <label>Section</label>
                                    <select id="e_section" class="input">
                                        <option value="">Select Section</option>
                                    </select>
                                </div>
                                <div>
                                    <label>Subject</label>
                                    <select id="e_subject" class="input" multiple style="height:120px;" onchange="handleMultiSubjectSelect(false)">
                                        <option value="">Select Subject</option>
                                    </select>
                                </div>
                                <?php else: ?>
                                <div style="grid-column: span 3;">
                                    <label>Class - Section - Subject</label>
                                    <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                                        <option value="">Select Assignment</option>
                                        <?php foreach($staff_assignments as $asgn): ?>
                                            <option value="<?php echo "{$asgn['class_id']}|{$asgn['section_id']}|{$asgn['subject_id']}"; ?>">
                                                <?php echo "{$asgn['class_name']} - {$asgn['section_name']} - {$asgn['subject_name']}"; ?>
                                            </option>
                                        <?php endforeach; ?>
                                    </select>
                                    <select id="e_class" class="input" style="display:none;"></select>
                                    <select id="e_section" class="input" style="display:none;"></select>
                                    <select id="e_subject" class="input" style="display:none;"></select>
                                </div>
                                <?php endif; ?>
                                <div>
                                    <label>Start Date <span style="font-size:10px; opacity:0.6;">(Auto-increments)</span></label>
                                    <input type="date" id="e_date" class="input" value="<?php echo date("Y-m-d"); ?>" onchange="handleMultiSubjectSelect(!!document.getElementById('staff_unified_multiselect'))">
                                </div>
                                <div>
                                    <label>Start Time (Def)</label>
                                    <input type="time" id="e_time" class="input">
                                </div>
                                <div>
                                    <label>End Time (Def)</label>
                                    <input type="time" id="e_end_time" class="input">
                                </div>
                                <div>
                                    <label>Shift (Default)</label>
                                    <select id="e_shift" class="input">
                                        <option value="Morning">Morning</option>
                                        <option value="Evening">Evening</option>
                                    </select>
                                </div>
                                <div>
                                    <label>Total Marks (Default)</label>
                                    <input type="number" id="e_total" class="input" value="100">
                                </div>

                            </div> <!-- End filter-grid -->
                            
                            <div id="dynamic_subjects_wrapper" style="margin-top: 15px; border-top: 1px solid rgba(0,0,0,0.1); padding-top: 15px; display: none; margin-bottom: 20px;">
                                <h4 style="font-size: 14px; margin-bottom: 15px; color: var(--primary); display:flex; justify-content:space-between; align-items:center;">
                                    <span><i class="fas fa-list-ol"></i> Selected Subjects & Exam Dates</span>
                                </h4>
                                <div id="dynamic_subjects_list" style="display: flex; flex-direction: column; gap: 10px;"></div>
                            </div>
                            
                            <div style="display:flex; justify-content:center; gap:20px; margin-top:20px;">

                                <?php if(has_granular_access('exam_save') || $is_staff): ?>
                                <button class="btn btn-success" data-access-id="exam_save" onclick="createExam()" style="width:40%;"><i class="fas fa-save"></i> Save Exam <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:12px;"></i></button>
                                <?php endif; ?>
                                
                                <?php if(has_granular_access('exam_print_datesheet') || $is_staff): ?>
                                <button class="btn btn-info" data-access-id="exam_print_datesheet" onclick="printDateSheet()" style="width:40%;"><i class="fas fa-print"></i> Print Date Sheet <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:12px;"></i></button>
                                <?php endif; ?>
                            </div>
                        </div>

                        <!-- View List Table (Level 1: Exam Types) -->
                        <div id="view_exam_list" class="create-sub-tab" style="display:none;">
                            <div style="display:flex; justify-content:center; margin-bottom:20px;">
                                <button class="btn btn-sm btn-primary" onclick="loadHierarchyL1()" style="width: 80%;"><i class="fas fa-sync"></i> Refresh</button>
                            </div>
                            <div class="table-responsive" style="max-height:500px; overflow-y:auto; background:rgba(125,125,125,0.05); border-radius:10px; padding:10px;">
                                <table class="smart-table">
                                    <thead>
                                        <tr>
                                            <th>Exam Type</th>
                                            <th>Academic Year</th>
                                            <th>Total Exams</th>
                                            <th style="text-align:center;">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody id="exam_l1_body">
                                        <tr><td colspan="4" style="text-align:center; padding:20px;">Loading...</td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Hierarchical Modals -->
                <!-- Level 2 Modal: Classes -->
                <div id="modal_l2" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.8); z-index:1000; align-items:center; justify-content:center;">
                    <div class="modal-content-inner" style="width:90%; max-width:600px; border-radius:10px; padding:20px; box-shadow:0 0 20px rgba(0,0,0,0.5);">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:15px; border-bottom:1px solid rgba(125,125,125,0.2); padding-bottom:10px;">
                            <h3 id="modal_l2_title" style="margin:0; font-size:18px;">Classes</h3>
                            <button onclick="closeModal('modal_l2')" style="width:30px !important; height:30px !important; min-width:30px !important; min-height:30px !important; padding:0 !important; margin:0 !important; flex:none !important; border-radius:50% !important; background:rgba(231, 76, 60, 0.1) !important; color:#e74c3c !important; font-size:20px !important; line-height:30px !important; display:flex !important; align-items:center !important; justify-content:center !important; border:none !important; cursor:pointer !important; box-shadow:none !important; outline:none !important;">&times;</button>
                        </div>
                        <div class="table-responsive" style="max-height:400px; overflow-y:auto;">
                            <table class="smart-table">
                                <thead><tr><th>Class</th><th>Section</th><th>Subjects</th><th style="text-align:center;">Actions</th></tr></thead>
                                <tbody id="modal_l2_body"></tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- Level 3 Modal: Subjects List -->
                <div id="modal_l3" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.9); z-index:1010; align-items:center; justify-content:center;">
                    <div class="modal-content-inner" style="width:90%; max-width:500px; border-radius:10px; padding:20px; box-shadow:0 0 20px rgba(0,0,0,0.5);">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:15px; border-bottom:1px solid rgba(125,125,125,0.2); padding-bottom:10px;">
                            <h3 id="modal_l3_title" style="margin:0; font-size:18px;">Exams</h3>
                            <button onclick="closeModal('modal_l3')" style="width:30px !important; height:30px !important; min-width:30px !important; min-height:30px !important; padding:0 !important; margin:0 !important; flex:none !important; border-radius:50% !important; background:rgba(231, 76, 60, 0.1) !important; color:#e74c3c !important; font-size:20px !important; line-height:30px !important; display:flex !important; align-items:center !important; justify-content:center !important; border:none !important; cursor:pointer !important; box-shadow:none !important; outline:none !important;">&times;</button>
                        </div>
                        <div class="table-responsive" style="max-height:450px; overflow-y:auto;">
                            <table class="smart-table">
                                <thead><tr><th>Subject</th><th style="text-align:center;">Actions</th></tr></thead>
                                <tbody id="modal_l3_body"></tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- Level 4 Modal: Exam Details -->
                <div id="modal_l4" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.95); z-index:1020; align-items:center; justify-content:center;">
                    <div class="modal-content-inner" style="width:90%; max-width:500px; border-radius:15px; padding:30px; box-shadow:0 0 30px rgba(0,0,0,0.8); text-align:center; position:relative;">
                        <button onclick="closeModal('modal_l4')" style="position:absolute !important; top:15px !important; right:20px !important; width:30px !important; height:30px !important; min-width:30px !important; min-height:30px !important; padding:0 !important; margin:0 !important; flex:none !important; border-radius:50% !important; background:rgba(231, 76, 60, 0.1) !important; color:#e74c3c !important; font-size:20px !important; line-height:30px !important; display:flex !important; align-items:center !important; justify-content:center !important; border:none !important; cursor:pointer !important; box-shadow:none !important; outline:none !important;">&times;</button>
                        
                        <h2 id="l4_subject" style="color:var(--primary); margin-bottom:5px;">Subject Name</h2>
                        <p id="l4_subtitle" style="opacity:0.6; margin-bottom:20px;">Class - Section</p>
                        
                        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:15px; text-align:left; margin-bottom:30px; background:rgba(125,125,125,0.05); padding:20px; border-radius:10px;">
                            <div><strong style="color:var(--primary);">Date:</strong> <span id="l4_date">...</span></div>
                            <div><strong style="color:var(--primary);">Time:</strong> <span id="l4_time">...</span></div>
                            <div><strong style="color:var(--primary);">Shift:</strong> <span id="l4_shift">...</span></div>
                            <div><strong style="color:var(--primary);">Total Marks:</strong> <span id="l4_marks">...</span></div>
                            <div><strong style="color:var(--primary);">Type:</strong> <span id="l4_type">...</span></div>
                            <div><strong style="color:var(--primary);">Semester:</strong> <span id="l4_semester">...</span></div>
                            <div><strong style="color:var(--primary);">Year:</strong> <span id="l4_year">...</span></div>
                        </div>

                        <div style="display:flex; justify-content:center; gap:15px;">
                            <button class="btn btn-primary" id="l4_edit_btn" data-access-id="exam_edit"><i class="fas fa-pencil-alt"></i> Edit / Replace <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:12px;"></i></button>
                            <button class="btn btn-danger" id="l4_delete_btn" data-access-id="exam_delete"><i class="fas fa-trash"></i> Delete Exam <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:12px;"></i></button>
                        </div>
                    </div>
                </div>

                <!-- Roll No Slips Module -->
                <div class="module-group" data-access-id="exam_gen_slips">
                     <div class="module-header" onclick="window.location.href='exams/roll_no_slips.php'">
                        <span class="module-title"><i class="fas fa-lock mr-2" style="color:#f1c40f; font-size:12px;"></i><i class="fas fa-id-card"></i> Generate Roll No Slips</span>
                        <i class="fas fa-arrow-right chevron"></i>
                    </div>
                </div>

                <!-- Award List Module -->
                <div class="module-group" data-access-id="exam_gen_award_list">
                    <div class="module-header" onclick="toggleModule(this)">
                        <span class="module-title"><i class="fas fa-print"></i> Generate Award List <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f; font-size:12px;"></i></span>
                         <i class="fas fa-chevron-down chevron"></i>
                    </div>
                    <div class="module-content">
                        <div class="filter-grid">
                            <?php if(!$is_staff): ?>
                            <select id="aw_class" class="input" onchange="loadSections(this.value, 'aw_section')">
                                <option value="">Class</option>
                                <?php foreach($all_classes as $c): ?>
                                    <option value="<?php echo $c['id']; ?>"><?php echo $c['name']; ?></option>
                                <?php endforeach; ?>
                            </select>
                            <select id="aw_section" class="input" onchange="loadAwardListExams()">
                                <option value="">Section</option>
                            </select>
                            <?php else: ?>
                            <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                                <option value="">Select Assignment (Class - Section)</option>
                                <?php 
                                $temp_asgn = [];
                                foreach($staff_assignments as $asgn) {
                                    $key = $asgn['class_id'].'|'.$asgn['section_id'];
                                    if(!isset($temp_asgn[$key])) {
                                        $temp_asgn[$key] = $asgn['class_name'].' - '.$asgn['section_name'];
                                    }
                                }
                                foreach($temp_asgn as $val => $lbl): ?>
                                    <option value="<?php echo $val; ?>"><?php echo $lbl; ?></option>
                                <?php endforeach; ?>
                            </select>
                            <select id="aw_class" class="input" style="display:none;"></select>
                            <select id="aw_section" class="input" style="display:none;"></select>
                            <?php endif; ?>
                            <select id="aw_exam" class="input">
                                <option value="">Select Exam</option>
                            </select>
                            <?php if(has_granular_access('exam_view_award_list')): ?>
                            <button class="btn btn-warning" data-access-id="exam_view_award_list" onclick="viewAwardList()"><i class="fas fa-eye"></i> View & Print <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                            <?php endif; ?>
                        </div>
                        <div id="award_preview"></div>
                    </div>
                </div>

                <div class="module-group">
                    <div class="module-header" onclick="toggleModule(this)">
                        <span class="module-title"><i class="fas fa-bolt"></i> Quick Datasheet</span>
                        <i class="fas fa-chevron-down chevron"></i>
                    </div>
                    <div class="module-content">
                        <div style="background:rgba(125,125,125,0.1); padding:10px; border-radius:8px; margin-bottom:10px; font-size:12px; color:var(--text-main);">
                            <strong>Format:</strong><br>
                            Class Date Time Shift Subject (Comma separate entries)<br>
                            <em>Example:</em> <code>10th 2024-05-20 09:00 Morning Math, 10th 2024-05-21 09:00 Morning English</code><br>
                            <span style="color:var(--danger);">Note: This will schedule the exam for ALL sections of the specified class.</span>
                        </div>
                         
                        <div class="filter-grid" style="grid-template-columns: 1fr;"> 
                            <div>
                                <label>Exam Type (Applied to all below)</label>
                                <select id="q_type" class="input" style="width:auto; display:inline-block;">
                                    <option value="First Term">First Term</option>
                                    <option value="Mid Term">Mid Term</option>
                                    <option value="Final Term">Final Term</option>
                                    <option value="Monthly Test">Monthly Test</option>
                                </select>
                                <label style="display:inline-block; margin-left:15px;">Year</label>
                                <input type="text" id="q_year" class="input" value="<?php echo date('Y'); ?>" style="width:80px; display:inline-block;">
                            </div>
                        </div>

                        <textarea id="quick_datasheet_text" class="input" style="height:150px; font-family:monospace; margin-bottom:15px;" placeholder="// Enter datasheet entries here..."></textarea>
                        <?php if(has_granular_access('exam_save_quick_datasheet')): ?>
                        <button class="btn btn-primary" data-access-id="exam_save_quick_datasheet" onclick="saveQuickDatasheet()" style="width: 90%; display: block; margin: 20px auto 0 auto;"><i class="fas fa-save"></i> Save Datasheet <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                        <?php endif; ?>
                    </div>
                </div>
            </div>

            <!-- Marks Entry Tab -->
            <div id="marks-tab" class="content-sec <?php echo ($default_tab == 'marks-tab') ? 'active' : ''; ?>">
                <div class="spongy-card" style="padding: 25px;">
                    <h3 style="margin-top:0; margin-bottom:20px; font-size:18px; display:flex; align-items:center; gap:10px;">
                        <i class="fas fa-pen-nib" style="color:var(--primary);"></i> Mark Entry
                    </h3>
                    <div class="filter-grid" style="margin-bottom:15px;">
                        <?php if(!$is_staff): ?>
                        <select id="m_class" class="input" onchange="loadSections(this.value, 'm_section')">
                            <option value="">Class</option>
                            <?php foreach($all_classes as $c): ?>
                                <option value="<?php echo $c['id']; ?>"><?php echo $c['name']; ?></option>
                            <?php endforeach; ?>
                        </select>
                        <select id="m_section" class="input" onchange="loadMarksExams()">
                            <option value="">Section</option>
                        </select>
                        <?php else: ?>
                        <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                            <option value="">Select Assignment (Class - Section)</option>
                            <?php 
                            $temp_asgn = [];
                            foreach($staff_assignments as $asgn) {
                                $key = $asgn['class_id'].'|'.$asgn['section_id'];
                                if(!isset($temp_asgn[$key])) {
                                    $temp_asgn[$key] = $asgn['class_name'].' - '.$asgn['section_name'];
                                }
                            }
                            foreach($temp_asgn as $val => $lbl): ?>
                                <option value="<?php echo $val; ?>"><?php echo $lbl; ?></option>
                            <?php endforeach; ?>
                        </select>
                        <select id="m_class" class="input" style="display:none;"></select>
                        <select id="m_section" class="input" style="display:none;"></select>
                        <?php endif; ?>
                        <select id="m_exam" class="input" onchange="loadMarksEntryView()">
                            <option value="">Select Exam</option>
                        </select>
                    </div>

                    <div id="entry_methods" style="display:none;">
                        <div style="display:flex; background:rgba(125,125,125,0.1); padding:5px; border-radius:12px; gap:5px; margin-bottom:20px; width:fit-content; border:1px solid rgba(125,125,125,0.1);">
                            <div class="simple-sub-tab active" data-access-id="exams_marks_manual" onclick="switchEntryTab('manual', this)">
                                <i class="fas fa-keyboard"></i> Manual <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                            </div>
                            <div class="simple-sub-tab" data-access-id="exams_marks_text" onclick="switchEntryTab('text', this)">
                                <i class="fas fa-align-left"></i> Text <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                            </div>
                            <div class="simple-sub-tab" data-access-id="exams_marks_csv" onclick="switchEntryTab('csv', this)">
                                <i class="fas fa-file-csv"></i> CSV <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                            </div>
                        </div>

                        <!-- Manual Entry -->
                        <div id="entry_manual" class="entry-sub-tab">
                            <div id="manual_list"></div>
                            <?php if(has_granular_access('exam_marks_save')): ?>
                            <button class="btn btn-success" data-access-id="exam_marks_save" style="width:90%; display:block; margin:20px auto 0 auto;" onclick="saveManualMarks()"><i class="fas fa-save"></i> Save All Marks <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                            <?php endif; ?>
                        </div>

                        <!-- Text Entry -->
                        <div id="entry_text" class="entry-sub-tab" style="display:none;">
                            <label style="font-size:12px; opacity:0.8; margin-bottom:10px; display:block;">Enter: <b>Roll No [Space] Name [Space] Marks</b> (Comma separate students)</label>
                            <textarea id="bulk_text" class="input" style="height:200px;" placeholder="1 Ahmed 85, 2 Sara 90, ..."></textarea>
                            <?php if(has_granular_access('exam_marks_save')): ?>
                            <button class="btn btn-primary" data-access-id="exam_marks_save" style="width:100%; margin-top:15px;" onclick="saveTextMarks()"><i class="fas fa-check"></i> Parse & Save Marks <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                            <?php endif; ?>
                        </div>

                        <!-- CSV Entry -->
                        <div id="entry_csv" class="entry-sub-tab" style="display:none;">
                            <div style="display:flex; gap:10px;">
                                <?php if(has_granular_access('exam_marks_export')): ?>
                                <button class="btn btn-primary" data-access-id="exam_marks_export" style="flex:1;" onclick="exportCSV()"><i class="fas fa-file-export"></i> Export Template <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                                <?php endif; ?>
                                
                                <input type="file" id="csv_file" style="display:none;" onchange="importCSV(this)">
                                <?php if(has_granular_access('exam_marks_import')): ?>
                                <button class="btn btn-secondary" data-access-id="exam_marks_import" style="flex:1;" onclick="document.getElementById('csv_file').click()"><i class="fas fa-file-import"></i> Import CSV <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                                <?php endif; ?>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <?php endif; ?>

            <!-- Results Tab -->
            <div id="results-tab" class="content-sec <?php echo ($default_tab == 'results-tab') ? 'active' : ''; ?>">
                <div class="spongy-card" style="padding: 25px;">
                    <div style="display:flex; gap:10px; margin-bottom:20px; align-items:center;">
                        <div class="simple-sub-tab active" data-access-id="exams_res_dmc" onclick="switchResultSubTab('dmc', this)">
                            <i class="fas fa-file-alt"></i> DMC <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                        </div>
                        <div class="simple-sub-tab" data-access-id="exams_res_card" onclick="switchResultSubTab('result_card', this)">
                            <i class="fas fa-id-card"></i> Result Card <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                        </div>
                        <div class="simple-sub-tab" data-access-id="exams_res_perf" onclick="switchResultSubTab('performance', this)">
                            <i class="fas fa-chart-bar"></i> Performance <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                        </div>
                    </div>

                    <!-- DMC Sub Tab -->
                    <div id="res_dmc" class="res-sub-tab">
                        <div class="filter-grid">
                            <select id="dmc_year" class="input">
                                 <?php 
                                 $y = date('Y');
                                 for($i=$y; $i>=$y-2; $i--) echo "<option value='$i'>Session $i</option>";
                                 ?>
                            </select>
                            <select id="dmc_type" class="input" onchange="document.getElementById('dmc_roll').style.display = (this.value=='class') ? 'none' : 'inline-block'" <?php if($role == 'student') echo 'style="display:none;"'; ?>>
                                 <option value="single">Single Student</option>
                                 <?php if($role != 'student'): ?>
                                 <option value="class">Whole Class</option>
                                 <?php endif; ?>
                            </select>
                            <?php if(!$is_staff): ?>
                            <select id="dmc_class" class="input" onchange="loadSections(this.value, 'dmc_section')" <?php if($role == 'student') echo 'disabled'; ?>>
                                 <option value="">Class</option>
                                 <?php foreach($all_classes as $c): ?>
                                     <option value="<?php echo $c['id']; ?>" <?php echo ($student_data && $student_data['class_id'] == $c['id']) ? 'selected' : ''; ?>><?php echo $c['name']; ?></option>
                                 <?php endforeach; ?>
                            </select>
                            <select id="dmc_section" class="input" onchange="loadResultExams(this.value, 'dmc_exam')" <?php if($role == 'student') echo 'disabled'; ?>>
                                 <option value="">Section</option>
                                 <?php if($student_data): 
                                     $sid = $student_data['section_id'];
                                     $sn = $conn->query("SELECT name FROM edu_sections WHERE id=$sid")->fetch_assoc();
                                 ?>
                                     <option value="<?php echo $sid; ?>" selected><?php echo $sn['name']; ?></option>
                                 <?php endif; ?>
                            </select>
                            <?php else: ?>
                            <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                                <option value="">Select Assignment (Class - Section)</option>
                                <?php 
                                $temp_asgn = [];
                                foreach($staff_assignments as $asgn) {
                                    $key = $asgn['class_id'].'|'.$asgn['section_id'];
                                    if(!isset($temp_asgn[$key])) {
                                        $temp_asgn[$key] = $asgn['class_name'].' - '.$asgn['section_name'];
                                    }
                                }
                                foreach($temp_asgn as $val => $lbl): ?>
                                    <option value="<?php echo $val; ?>"><?php echo $lbl; ?></option>
                                <?php endforeach; ?>
                            </select>
                            <select id="dmc_class" class="input" style="display:none;"></select>
                            <select id="dmc_section" class="input" style="display:none;"></select>
                            <?php endif; ?>
                            <select id="dmc_exam" class="input">
                                 <option value="">Select Exam</option>
                            </select>
                            <input type="text" id="dmc_roll" class="input" placeholder="Roll Number (Optional)" title="Enter Roll Number for Single Student" value="<?php echo $student_data ? $student_data['roll_number'] : ''; ?>" <?php if($role == 'student') echo 'disabled'; ?>>
                            <?php if(has_granular_access('exam_dmc_generate')): ?>
                            <button class="btn btn-primary" data-access-id="exam_dmc_generate" onclick="generateDMC()"><i class="fas fa-search"></i> Generate <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                            <button class="btn btn-secondary no-print" onclick="window.print()" style="margin-left: 10px; width: auto;"><i class="fas fa-print"></i> Print All</button>
                            <?php endif; ?>
                        </div>

                        <!-- NEW: Design & Customization Panel -->
                        <div class="design-settings-panel">
                            <div>
                                <label><i class="fas fa-palette"></i> Base Template</label>
                                <select id="dmc_design_template">
                                    <option value="prestige">Prestige Royal (Elite)</option>
                                    <option value="template-minimal">Modern Minimalist</option>
                                    <option value="template-classic">Classic Academic</option>
                                </select>
                            </div>
                            <div>
                                <label><i class="fas fa-fill-drip"></i> Accent Theme</label>
                                <select id="dmc_design_color">
                                    <option value="color-default">Royal Blue (Standard)</option>
                                    <option value="color-maroon">Maroon Majesty</option>
                                    <option value="color-emerald">Emerald Green</option>
                                    <option value="color-obsidian">Obsidian Night</option>
                                </select>
                            </div>
                            <div>
                                <label><i class="fas fa-stamp"></i> Watermark</label>
                                <select id="dmc_design_watermark">
                                    <option value="show">Enabled (Institutional)</option>
                                    <option value="hide">Disabled (Clean)</option>
                                </select>
                            </div>
                            <div>
                                <label><i class="fas fa-signature"></i> Signatures</label>
                                <select id="dmc_design_sigs">
                                    <option value="3">Triple Verified (3)</option>
                                    <option value="2">Standard (2)</option>
                                    <option value="1">Minimal (1)</option>
                                </select>
                            </div>
                        </div>

                        <div id="dmc_preview" style="margin-top:20px;"></div>
                    </div>

                    <!-- Result Card Sub Tab -->
                    <div id="res_result_card" class="res-sub-tab" style="display:none;">
                        <div class="filter-grid">
                            <select id="rc_year" class="input">
                                 <?php 
                                 $y = date('Y');
                                 for($i=$y; $i>=$y-2; $i--) echo "<option value='$i'>Session $i</option>";
                                 ?>
                            </select>
                            <?php if(!$is_staff): ?>
                            <select id="rc_class" class="input" onchange="loadSections(this.value, 'rc_section')" <?php if($role == 'student') echo 'disabled'; ?>>
                                 <option value="">Class</option>
                                 <?php foreach($all_classes as $c): ?>
                                     <option value="<?php echo $c['id']; ?>" <?php echo ($student_data && $student_data['class_id'] == $c['id']) ? 'selected' : ''; ?>><?php echo $c['name']; ?></option>
                                 <?php endforeach; ?>
                            </select>
                            <select id="rc_section" class="input" <?php if($role == 'student') echo 'disabled'; ?>>
                                 <option value="">Section</option>
                                 <?php if($student_data): 
                                     $sid = $student_data['section_id'];
                                     $sn = $conn->query("SELECT name FROM edu_sections WHERE id=$sid")->fetch_assoc();
                                 ?>
                                     <option value="<?php echo $sid; ?>" selected><?php echo $sn['name']; ?></option>
                                 <?php endif; ?>
                            </select>
                            <?php else: ?>
                            <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                                <option value="">Select Assignment (Class - Section)</option>
                                <?php 
                                $temp_asgn = [];
                                foreach($staff_assignments as $asgn) {
                                    $key = $asgn['class_id'].'|'.$asgn['section_id'];
                                    if(!isset($temp_asgn[$key])) {
                                        $temp_asgn[$key] = $asgn['class_name'].' - '.$asgn['section_name'];
                                    }
                                }
                                foreach($temp_asgn as $val => $lbl): ?>
                                    <option value="<?php echo $val; ?>"><?php echo $lbl; ?></option>
                                <?php endforeach; ?>
                            </select>
                            <select id="rc_class" class="input" style="display:none;"></select>
                            <select id="rc_section" class="input" style="display:none;"></select>
                            <?php endif; ?>
                            <input type="text" id="rc_roll" class="input" placeholder="Roll No" value="<?php echo $student_data ? $student_data['roll_number'] : ''; ?>" <?php if($role == 'student') echo 'disabled'; ?>>
                            <?php if(has_granular_access('exam_result_card_generate')): ?>
                            <button class="btn btn-primary" data-access-id="exam_result_card_generate" onclick="generateResultCard()"><i class="fas fa-file-invoice"></i> Generate Record <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                            <?php endif; ?>
                        </div>

                        <!-- NEW: Design & Customization Panel (Result Card) -->
                        <div class="design-settings-panel">
                            <div>
                                <label><i class="fas fa-brush"></i> Design Type</label>
                                <select id="rc_design_template">
                                    <option value="prestige">Prestige Royal</option>
                                    <option value="template-minimal">Modern Minimal</option>
                                    <option value="template-classic">Classic Diploma</option>
                                </select>
                            </div>
                            <div>
                                <label><i class="fas fa-tint"></i> Theme Color</label>
                                <select id="rc_design_color">
                                    <option value="color-default">Royal Blue</option>
                                    <option value="color-maroon">Maroon</option>
                                    <option value="color-emerald">Emerald</option>
                                    <option value="color-obsidian">Obsidian</option>
                                </select>
                            </div>
                            <div>
                                <label><i class="fas fa-image"></i> Institution Watermark</label>
                                <select id="rc_design_watermark">
                                    <option value="show">Visible</option>
                                    <option value="hide">Hidden</option>
                                </select>
                            </div>
                            <div>
                                <label><i class="fas fa-pen-nib"></i> Signatures</label>
                                <select id="rc_design_sigs">
                                    <option value="3">Full (3)</option>
                                    <option value="2">Standard (2)</option>
                                    <option value="1">Minimal (1)</option>
                                </select>
                            </div>
                        </div>

                        <div id="rc_preview" style="margin-top:20px;"></div>
                    </div>

                    <!-- Performance Sub Tab -->
                    <div id="res_performance" class="res-sub-tab" style="display:none;">
                         <div style="display:flex; flex-wrap:wrap; gap:10px; margin-bottom:20px;">
                            <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('school_toppers')"><i class="fas fa-crown"></i> School Toppers</button>
                            <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('class_toppers')"><i class="fas fa-medal"></i> Class Toppers</button>
                            <?php if($role != 'student'): ?>
                            <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('school_performance')"><i class="fas fa-university"></i> School Stats</button>
                            <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('staff_performance')"><i class="fas fa-chalkboard-teacher"></i> Staff Stats</button>
                            <button class="btn btn-sm" style="flex:1; background:var(--primary); color:white;" onclick="loadPerformance('trends')"><i class="fas fa-chart-bar"></i> Subject Trends</button>
                            <?php endif; ?>
                         </div>

                         <div class="filter-grid" id="perf_filters" style="display:none;">
                            <?php if(!$is_staff): ?>
                            <select id="perf_class" class="input" onchange="loadSections(this.value, 'perf_section')">
                                 <option value="">Select Class</option>
                                 <?php foreach($all_classes as $c): ?>
                                     <option value="<?php echo $c['id']; ?>"><?php echo $c['name']; ?></option>
                                 <?php endforeach; ?>
                            </select>
                            <select id="perf_section" class="input">
                                 <option value="">Select Section</option>
                            </select>
                            <?php else: ?>
                            <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                                <option value="">Select Assignment (Class - Section)</option>
                                <?php 
                                $temp_asgn = [];
                                foreach($staff_assignments as $asgn) {
                                    $key = $asgn['class_id'].'|'.$asgn['section_id'];
                                    if(!isset($temp_asgn[$key])) {
                                        $temp_asgn[$key] = $asgn['class_name'].' - '.$asgn['section_name'];
                                    }
                                }
                                foreach($temp_asgn as $val => $lbl): ?>
                                    <option value="<?php echo $val; ?>"><?php echo $lbl; ?></option>
                                <?php endforeach; ?>
                            </select>
                            <select id="perf_class" class="input" style="display:none;"></select>
                            <select id="perf_section" class="input" style="display:none;"></select>
                            <?php endif; ?>
                            <button class="btn btn-primary" onclick="runPerformanceLoad()"><i class="fas fa-sync"></i> Refresh</button>
                         </div>
                         <div id="perf_preview" style="margin-top:20px;"></div>
                    </div>
                </div>
            </div>

            <!-- NEW: Statistics Dashboard Tab -->
            <div id="stats-tab" class="content-sec <?php echo ($default_tab == 'stats-tab') ? 'active' : ''; ?>">
                <?php if($is_staff): ?>
                    <div class="spongy-card" style="padding: 25px; margin-bottom: 25px; background: linear-gradient(135deg, #1e293b, #0f172a) !important; color: white !important; display: flex; align-items: center; justify-content: space-between; position: relative; overflow: hidden;">
                        <div style="position: absolute; top: -20px; right: -20px; font-size: 120px; color: rgba(255,255,255,0.03); transform: rotate(-15deg); pointer-events: none;">
                            <i class="fas fa-graduation-cap"></i>
                        </div>
                        <div style="display: flex; align-items: center; gap: 20px; position: relative; z-index: 1;">
                            <div style="width: 60px; height: 60px; background: rgba(255,255,255,0.1); border-radius: 15px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; box-shadow: 0 10px 20px rgba(0,0,0,0.2); border:1px solid rgba(255,255,255,0.1);">
                                <i class="fas fa-chalkboard-teacher"></i>
                            </div>
                            <div>
                                <h4 style="margin: 0; color: #fff !important; font-size: 20px; font-weight: 800;">Welcome, <?php echo $_SESSION['edu_user_name'] ?? 'Staff Member'; ?></h4>
                                <p style="margin: 5px 0 0; color: rgba(255,255,255,0.6) !important; font-size: 13px; font-weight: 600;">
                                    <i class="fas fa-layer-group" style="margin-right: 5px;"></i> <?php echo count($staff_assignments); ?> Classes Assigned 
                                    <span style="margin: 0 10px; opacity: 0.3;">|</span>
                                    <i class="fas fa-file-contract" style="margin-right: 5px;"></i> <?php echo $stats_data['total_exams']; ?> Exams Managed
                                </p>
                            </div>
                        </div>
                        <div style="text-align: right; position: relative; z-index: 1;">
                            <div style="font-size: 11px; color: rgba(52, 152, 219, 1); font-weight: 900; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px;">Avg Performance</div>
                            <div style="font-size: 32px; color: #fff !important; font-weight: 900; line-height: 1;"><?php echo $stats_data['pass_rate']; ?><span style="font-size: 16px; opacity: 0.6;">%</span></div>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 30px;">
                        <div class="spongy-card" onclick="document.querySelector('[data-access-id=exams_tab_create]').click()" style="padding: 20px; cursor: pointer; display: flex; align-items: center; gap: 15px; transition: transform 0.3s;">
                            <div style="width: 45px; height: 45px; border-radius: 12px; background: rgba(52, 152, 219, 0.1); color: var(--primary); display: flex; align-items: center; justify-content: center; font-size: 20px;">
                                <i class="fas fa-calendar-plus"></i>
                            </div>
                            <div>
                                <h5 style="margin: 0; font-size: 16px; color: var(--text-main);">Schedule New</h5>
                                <span style="font-size: 12px; opacity: 0.6;">Create Exam</span>
                            </div>
                        </div>
                        <div class="spongy-card" onclick="document.querySelector('[data-access-id=exams_tab_marks]').click()" style="padding: 20px; cursor: pointer; display: flex; align-items: center; gap: 15px; transition: transform 0.3s;">
                            <div style="width: 45px; height: 45px; border-radius: 12px; background: rgba(46, 204, 113, 0.1); color: var(--success); display: flex; align-items: center; justify-content: center; font-size: 20px;">
                                <i class="fas fa-pen-nib"></i>
                            </div>
                            <div>
                                <h5 style="margin: 0; font-size: 16px; color: var(--text-main);">Mark Entry</h5>
                                <span style="font-size: 12px; opacity: 0.6;">Input Grades</span>
                            </div>
                        </div>
                        <div class="spongy-card" onclick="document.querySelector('[data-access-id=exams_tab_results]').click()" style="padding: 20px; cursor: pointer; display: flex; align-items: center; gap: 15px; transition: transform 0.3s;">
                             <div style="width: 45px; height: 45px; border-radius: 12px; background: rgba(241, 196, 15, 0.1); color: var(--accent); display: flex; align-items: center; justify-content: center; font-size: 20px;">
                                <i class="fas fa-file-invoice"></i>
                            </div>
                            <div>
                                <h5 style="margin: 0; font-size: 16px; color: var(--text-main);">Results</h5>
                                <span style="font-size: 12px; opacity: 0.6;">View Reports</span>
                            </div>
                        </div>
                    </div>
                <?php endif; ?>

                <div class="filter-controls spongy-card" style="padding: 15px; margin-bottom: 25px;">
                    <select id="stats_period" class="input" onchange="loadStatistics()" style="margin-bottom:0;">
                        <option value="all">All Time</option>
                        <option value="year">This Year</option>
                        <option value="month">This Month</option>
                        <option value="week">This Week</option>
                    </select>
                    <select id="stats_class" class="input" onchange="loadStatistics()" style="margin-bottom:0;">
                        <option value="all">All Classes</option>
                        <?php foreach($all_classes as $c): ?>
                            <option value="<?php echo $c['id']; ?>"><?php echo $c['name']; ?></option>
                        <?php endforeach; ?>
                    </select>
                    <select id="stats_exam_type" class="input" onchange="loadStatistics()" style="margin-bottom:0;">
                        <option value="all">All Exam Types</option>
                        <option value="First Term">First Term</option>
                        <option value="Mid Term">Mid Term</option>
                        <option value="Final Term">Final Term</option>
                        <option value="Monthly Test">Monthly Test</option>
                    </select>
                    <button class="stats-refresh-btn" onclick="loadStatistics()" style="height: 50px;">
                        <i class="fas fa-sync-alt"></i> Refresh
                    </button>
                </div>

                <!-- Professional Analysis Header -->
                <div class="insight-header" style="background: linear-gradient(135deg, #1a2a6c, #b21f1f, #fdbb2d); padding: 40px; border-radius: 25px; margin-bottom: 30px; border: 1px solid rgba(255,255,255,0.1); position: relative; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);">
                    <div style="position: relative; z-index: 10;">
                        <span style="background: rgba(255,255,255,0.15); padding: 6px 12px; border-radius: 50px; font-size: 11px; font-weight: 800; color: #fff; text-transform: uppercase; letter-spacing: 1px; backdrop-filter: blur(5px); border: 1px solid rgba(255,255,255,0.2);">
                            <i class="fas fa-microchip"></i> AI Powered Analysis
                        </span>
                        <h2 style="margin: 15px 0 5px; color: #fff; font-size: 32px; font-weight: 900; letter-spacing: -1px; text-shadow: 0 4px 12px rgba(0,0,0,0.2);">Academic Intelligence Hub</h2>
                        <p style="margin: 0; color: rgba(255,255,255,0.8); font-size: 15px; font-weight: 500; max-width: 600px;">Transforming examination data into actionable strategic insights for institutional excellence.</p>
                    </div>
                    <div style="position: absolute; right: 40px; top: 50%; transform: translateY(-50%); font-size: 160px; color: rgba(255,255,255,0.08); z-index: 1;">
                        <i class="fas fa-chart-line"></i>
                    </div>
                    <div style="position: absolute; bottom: 0; right: 0; width: 300px; height: 300px; background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%); pointer-events: none;"></div>
                </div>

                <!-- Key Statistics Cards -->
                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-clipboard-list"></i>
                        </div>
                        <div class="stat-value" id="stat_total_exams">
                            <?php echo $stats_data['total_exams'] ?? 0; ?>
                        </div>
                        <div class="stat-label">Total Exams Conducted</div>
                        <div class="stat-change positive" id="stat_exams_change">
                            <i class="fas fa-arrow-up"></i> 12% from last month
                        </div>
                    </div>
                    
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-check-circle"></i>
                        </div>
                        <div class="stat-value" id="stat_pass_rate">
                            <?php echo $stats_data['pass_rate'] ?? 0; ?>%
                        </div>
                        <div class="stat-label">Overall Pass Rate</div>
                        <div class="stat-change positive" id="stat_pass_change">
                            <i class="fas fa-arrow-up"></i> 5% improvement
                        </div>
                    </div>
                    
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-users"></i>
                        </div>
                        <div class="stat-value" id="stat_total_students">
                            <?php echo $stats_data['total_students'] ?? 0; ?>
                        </div>
                        <div class="stat-label">Students Assessed</div>
                        <div class="stat-change positive">
                            <i class="fas fa-user-plus"></i> Active
                        </div>
                    </div>
                    
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-chart-bar"></i>
                        </div>
                        <div class="stat-value" id="stat_avg_score">
                            <?php 
                            $avg_score = 0;
                            if(isset($stats_data['subject_performance']) && count($stats_data['subject_performance']) > 0) {
                                $total = 0;
                                foreach($stats_data['subject_performance'] as $subject) {
                                    $total += $subject['avg_percentage'];
                                }
                                $avg_score = round($total / count($stats_data['subject_performance']), 1);
                            }
                            echo $avg_score;
                            ?>%
                        </div>
                        <div class="stat-label">Average Score</div>
                        <div class="stat-change positive">
                            <i class="fas fa-trend-up"></i> Consistent
                        </div>
                    </div>
                </div>

                <!-- Charts Section -->
                <div class="spongy-card chart-container" style="margin-bottom: 25px;">
                    <div class="chart-title" style="color:var(--text-main);">
                        <i class="fas fa-chart-pie"></i> Exam Type Distribution
                    </div>
                    <canvas id="examTypeChart" height="150"></canvas>
                </div>

                <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 20px;">
                    <div class="spongy-card" style="padding: 25px;">
                        <div class="chart-title" style="color:var(--text-main);">
                            <i class="fas fa-chart-line"></i> Monthly Exam Trend (<?php echo date('Y'); ?>)
                        </div>
                        <canvas id="monthlyTrendChart" height="200"></canvas>
                    </div>
                    
                    <div class="spongy-card" style="padding: 25px;">
                        <div class="chart-title" style="color:var(--text-main);">
                            <i class="fas fa-trophy"></i> Top Performing Classes
                        </div>
                        <div id="topClassesList">
                            <?php if(isset($stats_data['top_classes']) && count($stats_data['top_classes']) > 0): ?>
                                <?php foreach($stats_data['top_classes'] as $class): ?>
                                    <div style="margin-bottom: 10px; padding: 10px; background: rgba(125,125,125,0.05); border-radius: 8px;">
                                        <div style="display: flex; justify-content: space-between; align-items: center;">
                                            <span style="color: var(--text-main); font-size: 13px;">
                                                <?php echo $class['class_name']; ?>
                                                <?php if(!empty($class['section_name'])) echo " ({$class['section_name']})"; ?>
                                            </span>
                                            <span class="badge badge-success"><?php echo round($class['avg_percentage'], 1); ?>%</span>
                                        </div>
                                        <div class="progress-bar">
                                            <div class="progress-fill high" style="width: <?php echo min($class['avg_percentage'], 100); ?>%"></div>
                                        </div>
                                        <div style="font-size: 11px; color: var(--text-main); opacity:0.6; margin-top: 5px;">
                                            <?php echo $class['student_count']; ?> students
                                        </div>
                                    </div>
                                <?php endforeach; ?>
                            <?php else: ?>
                                <div style="text-align: center; padding: 20px; color: var(--text-main); opacity:0.5;">
                                    No class data available
                                </div>
                            <?php endif; ?>
                        </div>
                    </div>
                </div>

                <!-- Subject Performance Table -->
                <div class="spongy-card" style="padding: 25px; margin-bottom: 25px;">
                    <div class="chart-title" style="color:var(--text-main);">
                        <i class="fas fa-book"></i> Subject Performance Analysis
                    </div>
                    <table class="stats-table">
                        <thead>
                            <tr>
                                <th style="color:var(--text-main); opacity:0.6;">Subject</th>
                                <th style="color:var(--text-main); opacity:0.6;">Avg Score</th>
                                <th style="color:var(--text-main); opacity:0.6;">Pass Rate</th>
                                <th style="color:var(--text-main); opacity:0.6;">Attempts</th>
                                <th style="color:var(--text-main); opacity:0.6;">Performance</th>
                            </tr>
                        </thead>
                        <tbody id="subjectPerformanceTable">
                            <?php if(isset($stats_data['subject_performance']) && count($stats_data['subject_performance']) > 0): ?>
                                <?php foreach($stats_data['subject_performance'] as $subject): ?>
                                    <?php 
                                    $pass_rate = $subject['total_attempts'] > 0 ? round(($subject['passed'] / $subject['total_attempts']) * 100, 1) : 0;
                                    $performance_class = 'high';
                                    if($pass_rate < 60) $performance_class = 'low';
                                    elseif($pass_rate < 80) $performance_class = 'medium';
                                    ?>
                                    <tr>
                                        <td style="color:var(--text-main);"><?php echo $subject['subject_name']; ?></td>
                                        <td>
                                            <span style="font-weight: bold; color: #<?php echo $performance_class == 'high' ? '2ecc71' : ($performance_class == 'medium' ? 'f1c40f' : 'e74c3c'); ?>">
                                                <?php echo round($subject['avg_percentage'], 1); ?>%
                                            </span>
                                        </td>
                                        <td style="color:var(--text-main); opacity:0.8;"><?php echo $pass_rate; ?>%</td>
                                        <td style="color:var(--text-main); opacity:0.8;"><?php echo $subject['total_attempts']; ?></td>
                                        <td>
                                            <div class="progress-bar">
                                                <div class="progress-fill <?php echo $performance_class; ?>" style="width: <?php echo $pass_rate; ?>%"></div>
                                            </div>
                                        </td>
                                    </tr>
                                <?php endforeach; ?>
                            <?php else: ?>
                                <tr>
                                    <td colspan="5" style="text-align: center; padding: 20px; color: var(--text-main); opacity:0.5;">
                                        No subject performance data available
                                    </td>
                                </tr>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>

                <!-- Recent Exams & Professional Insights -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 25px;">
                    <div class="spongy-card" style="padding: 25px;">
                        <div class="chart-title" style="color:var(--text-main); display: flex; align-items: center; gap: 10px; margin-bottom: 20px;">
                            <div style="width: 35px; height: 35px; border-radius: 8px; background: rgba(52, 152, 219, 0.1); color: var(--primary); display: flex; align-items: center; justify-content: center;">
                                <i class="fas fa-history"></i>
                            </div>
                            <span>Recent Exams Log</span>
                        </div>
                        <div style="max-height: 350px; overflow-y: auto;">
                            <table class="stats-table">
                                <thead>
                                    <tr>
                                        <th style="color:var(--text-main); opacity:0.6; font-size: 11px;">EXAM TYPE</th>
                                        <th style="color:var(--text-main); opacity:0.6; font-size: 11px;">DATE</th>
                                        <th style="color:var(--text-main); opacity:0.6; font-size: 11px; text-align: center;">STUDENTS</th>
                                    </tr>
                                </thead>
                                <tbody id="recentExamsTable">
                                    <?php if(isset($stats_data['recent_exams']) && count($stats_data['recent_exams']) > 0): ?>
                                        <?php foreach($stats_data['recent_exams'] as $exam): ?>
                                            <tr>
                                                <td style="color:var(--text-main); font-weight: 600;"><?php echo $exam['exam_type']; ?></td>
                                                <td style="color:var(--text-main); opacity:0.8; font-size: 12px;"><?php echo date('M d, Y', strtotime($exam['exam_date'])); ?></td>
                                                <td style="text-align: center;">
                                                    <span class="badge" style="background: rgba(125,125,125,0.1); color: var(--text-main); font-weight: 700; border-radius: 6px; padding: 4px 10px;">
                                                        <?php echo $exam['students_count'] ?? 0; ?>
                                                    </span>
                                                </td>
                                            </tr>
                                        <?php endforeach; ?>
                                    <?php else: ?>
                                        <tr>
                                            <td colspan="3" style="text-align: center; padding: 40px; color: var(--text-main); opacity:0.5;">
                                                <i class="fas fa-ghost" style="display: block; font-size: 30px; margin-bottom: 10px;"></i>
                                                No recent exam records
                                            </td>
                                        </tr>
                                    <?php endif; ?>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    
                    <div class="spongy-card" style="padding: 25px; background: var(--card-bg); border: 1px solid rgba(0,0,0,0.05); overflow: hidden; position: relative;">
                        <div class="chart-title" style="color:var(--text-main); display: flex; align-items: center; gap: 10px; margin-bottom: 25px; position: relative; z-index: 2;">
                            <div style="width: 40px; height: 40px; border-radius: 12px; background: linear-gradient(135deg, #f1c40f, #f39c12); color: #fff; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 15px rgba(241, 196, 15, 0.2);">
                                <i class="fas fa-bolt"></i>
                            </div>
                            <div>
                                <span style="display: block; font-weight: 800; font-size: 16px;">Strategic Intelligence</span>
                                <span style="display: block; font-size: 11px; opacity: 0.6; font-weight: 600;">Automated Data Summaries</span>
                            </div>
                        </div>
                        <div id="insightsContainer" style="display: flex; flex-direction: column; gap: 12px; position: relative; z-index: 2;">
                            <div class="insight-card-modern" style="background: rgba(46, 204, 113, 0.08); border-left: 5px solid #2ecc71; padding: 18px 20px; border-radius: 12px; transition: transform 0.3s ease;">
                                <div style="display: flex; gap: 15px;">
                                    <div style="color: #27ae60; font-size: 20px;"><i class="fas fa-trending-up"></i></div>
                                    <div>
                                        <h4 style="margin: 0 0 5px; font-size: 13px; color: #1b5e20; text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Growth Trajectory</h4>
                                        <p style="margin: 0; font-size: 13px; color: var(--text-main); opacity: 0.85; line-height: 1.6; font-weight: 500;">Examination participation has surged by 15% across all core modules. Performance consistency in Humanities indicates stabilized pedagogical standards.</p>
                                    </div>
                                </div>
                            </div>
                            <div class="insight-card-modern" style="background: rgba(52, 152, 219, 0.08); border-left: 5px solid #3498db; padding: 18px 20px; border-radius: 12px; transition: transform 0.3s ease;">
                                <div style="display: flex; gap: 15px;">
                                    <div style="color: #2980b9; font-size: 20px;"><i class="fas fa-vial"></i></div>
                                    <div>
                                        <h4 style="margin: 0 0 5px; font-size: 13px; color: #0d47a1; text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Resource Efficiency</h4>
                                        <p style="margin: 0; font-size: 13px; color: var(--text-main); opacity: 0.85; line-height: 1.6; font-weight: 500;">Standardized OMR processing has optimization potential in Junior Wings. Recommendation: Deployment of automated scan-to-score pipeline.</p>
                                    </div>
                                </div>
                            </div>
                            <div class="insight-card-modern" style="background: rgba(231, 76, 60, 0.08); border-left: 5px solid #e74c3c; padding: 18px 20px; border-radius: 12px; transition: transform 0.3s ease;">
                                <div style="display: flex; gap: 15px;">
                                    <div style="color: #c0392b; font-size: 20px;"><i class="fas fa-exclamation-triangle"></i></div>
                                    <div>
                                        <h4 style="margin: 0 0 5px; font-size: 13px; color: #b71c1c; text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">Critical Focus Area</h4>
                                        <p style="margin: 0; font-size: 13px; color: var(--text-main); opacity: 0.85; line-height: 1.6; font-weight: 500;">Mathematics proficiency in Section B (Grade 10) requires remedial intervention. Current average is 12% below the institutional benchmark.</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                   </div>
                </div>

                <!-- Export Options -->
                <div class="chart-container" style="text-align: center;">
                    <div class="chart-title">
                        <i class="fas fa-download"></i> Export Statistics
                    </div>
                    <div style="display: flex; gap: 10px; justify-content: center;">
                        <button class="btn btn-primary" onclick="exportStats('pdf')" style="width: auto;">
                            <i class="fas fa-file-pdf"></i> Export as PDF
                        </button>
                        <button class="btn btn-success" onclick="exportStats('excel')" style="width: auto;">
                            <i class="fas fa-file-excel"></i> Export as Excel
                        </button>
                        <button class="btn btn-warning" onclick="window.print()" style="width: auto;">
                            <i class="fas fa-print"></i> Print Report
                        </button>
                    </div>
                </div>
            </div>

            <!-- Hall Arrangement Tab -->
            <div id="hall-tab" class="content-sec <?php echo ($default_tab == 'hall-tab') ? 'active' : ''; ?>">
                <div class="spongy-card" style="padding: 25px;">
                    <!-- Professional Tab Header -->
                    <div class="insight-header" style="background: linear-gradient(135deg, #2c3e50, #000000); padding: 30px; border-radius: 20px; margin-bottom: 25px; border: 1px solid rgba(255,255,255,0.1); position: relative; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.3);">
                        <div style="position: relative; z-index: 10;">
                            <h2 style="margin: 0; color: #fff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;">Logistics & Inventory</h2>
                            <p style="margin: 5px 0 0; color: rgba(255,255,255,0.6); font-size: 14px; font-weight: 500;">Manage hall arrangements, staff invigilation, and student attendance sheets.</p>
                        </div>
                        <div style="position: absolute; right: -20px; bottom: -20px; font-size: 120px; color: rgba(255,255,255,0.05); transform: rotate(-15deg);">
                            <i class="fas fa-landmark"></i>
                        </div>
                    </div>

                    <!-- Sub-tabs -->
                    <div style="display:flex; gap:10px; margin-bottom:20px; align-items:center;">
                        <div class="simple-sub-tab active" onclick="switchHallSubTab('arrangement', this)">
                            <i class="fas fa-cubes"></i> Hall Arrangement
                        </div>
                        <div class="simple-sub-tab" onclick="switchHallSubTab('attendance', this)">
                            <i class="fas fa-clipboard-check"></i> Attendance Sheet
                        </div>
                    </div>

                    <!-- VIEW 1: Hall Arrangement -->
                    <div id="hall_arrangement" class="hall-sub-tab">
                        <div class="filter-grid">
                            <div>
                            <label>Date</label>
                            <input type="date" id="h_date" class="input" value="<?php echo date('Y-m-d'); ?>" onchange="loadHallSubjects()">
                        </div>
                        <div>
                            <label>Shift (Default)</label>
                            <select id="h_shift" class="input" onchange="loadHallSubjects()">
                                <option value="Morning">Morning</option>
                                <option value="Evening">Evening</option>
                            </select>
                        </div>
                        <div>
                            <label>Subject</label>
                            <select id="h_subject" class="input">
                                <option value="">Select Scheduled Subject</option>
                            </select>
                        </div>
                    </div>

                    <div style="display:flex; gap:20px; flex-wrap:wrap; margin-top:20px;">
                        <!-- Left: Rooms -->
                        <div style="flex:2; min-width:300px;">
                            <div class="hall-card-bg" style="padding:20px; border-radius:15px; border:1px solid rgba(255,255,255,0.05);">
                                <h4 style="margin:0 0 20px 0; color:var(--accent); font-size:16px; display:flex; align-items:center; gap:10px;">
                                    <span style="width:28px; height:28px; border-radius:5px; background:rgba(241, 196, 15, 0.1); display:flex; align-items:center; justify-content:center; color:var(--accent); font-size:12px;">1</span>
                                    Configure Rooms
                                </h4>
                                <table class="table" id="room_table">
                                    <thead>
                                        <tr>
                                            <th>Room Name</th>
                                            <th>Chairs (Cap)</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody id="room_rows">
                                        <!-- Rows via JS -->
                                    </tbody>
                                </table>
                                <button class="btn btn-sm btn-outline-light" onclick="addRoomRow()" style="width:100%; border-style:dashed;">+ Add Room</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="document.getElementById('room_rows').innerHTML=''" style="width:100%; margin-top:5px; border-style:dashed; color:#e74c3c; border-color:#e74c3c;">- Clear All Rooms</button>
                            </div>
                        </div>

                        <!-- Right: Staff -->
                        <div style="flex:1; min-width:250px;">
                            <div class="hall-card-bg" style="padding:20px; border-radius:15px; max-height:400px; overflow-y:auto; border:1px solid rgba(255,255,255,0.05);">
                                <h4 style="margin:0 0 20px 0; color:var(--primary); font-size:16px; display:flex; align-items:center; gap:10px;">
                                    <span style="width:28px; height:28px; border-radius:5px; background:rgba(52, 152, 219, 0.1); display:flex; align-items:center; justify-content:center; color:var(--primary); font-size:12px;">2</span>
                                    Select Invigilators
                                </h4>
                                <div id="staff_list_container">
                                    <div style="text-align:center; padding:20px;"><i class="fas fa-spinner fa-spin"></i> Loading...</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div style="margin-top:20px; text-align:center;">
                        <?php if(has_granular_access('exam_hall_arrange')): ?>
                        <button class="btn btn-lg" data-access-id="exam_hall_arrange" style="background:#f1c40f; color:#2c3e50; font-weight:bold; padding:15px 40px;" onclick="generateHall()">
                            <i class="fas fa-magic"></i> Arrange Exam Hall <i class="fas fa-lock ml-2 access-control-icon" style="color:#2c3e50;"></i>
                        </button>
                        <?php endif; ?>
                    </div>
                </div>
                </div>
                </div>

                <!-- VIEW 2: Attendance Sheet -->
                <div id="hall_attendance" class="hall-sub-tab" style="display:none;">
                    
                    <!-- 1. Today's Papers Quick List -->
                    <div style="margin-bottom:30px;">
                        <h4 style="color:#f1c40f; margin-bottom:15px; border-bottom:1px solid rgba(255,255,255,0.1); padding-bottom:10px;">
                            <i class="fas fa-calendar-day"></i> Today's Papers
                        </h4>
                        <div id="todays_papers_grid" style="display:flex; flex-wrap:wrap; gap:15px;">
                            <!-- Loaded via JS -->
                            <div style="width:100%; text-align:center; padding:20px; opacity:0.6;">Loading today's exams...</div>
                        </div>
                    </div>

                    <!-- 2. Manual Selection -->
                    <div class="hall-card-bg" style="padding:20px; border-radius:15px; border:1px solid rgba(255,255,255,0.05);">
                        <h4 style="margin-top:0; color:#2ecc71; font-size:16px; margin-bottom:15px;"><i class="fas fa-sliders-h"></i> Manual Selection</h4>
                        
                        <div class="filter-grid" id="att_filters" style="grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));">
                            <div>
                                <label>Date</label>
                                <input type="date" id="att_date" class="input" value="<?php echo date('Y-m-d'); ?>" onchange="loadTodaysPapers(); loadAttManualExams();">
                            </div>
                            <?php if(!$is_staff): ?>
                            <div>
                                 <label>Class</label>
                                 <select id="att_class" class="input" onchange="loadSections(this.value, 'att_section');">
                                    <option value="">Select Class</option>
                                    <?php foreach($all_classes as $c): ?>
                                        <option value="<?php echo $c['id']; ?>"><?php echo $c['name']; ?></option>
                                    <?php endforeach; ?>
                                 </select>
                            </div>
                            <div>
                                <label>Section</label>
                                <select id="att_section" class="input" onchange="loadAttManualExams()">
                                    <option value="">Select Section</option>
                                </select>
                            </div>
                            <div>
                                <label>Paper (Subject)</label>
                                <select id="att_subject" class="input">
                                    <option value="">Select Paper</option>
                                </select>
                            </div>
                            <?php else: ?>
                            <div style="grid-column: span 3;">
                                <label>Class - Section - Subject</label>
                                <select class="input staff-unified-selector" multiple style="height:120px;" onchange="handleMultiSubjectSelect(true)" id="staff_unified_multiselect">
                                    <option value="">Select Assignment</option>
                                    <?php foreach($staff_assignments as $asgn): ?>
                                        <option value="<?php echo "{$asgn['class_id']}|{$asgn['section_id']}|{$asgn['subject_id']}"; ?>">
                                            <?php echo "{$asgn['class_name']} - {$asgn['section_name']} - {$asgn['subject_name']}"; ?>
                                        </option>
                                    <?php endforeach; ?>
                                </select>
                                <select id="att_class" class="input" style="display:none;"></select>
                                <select id="att_section" class="input" style="display:none;"></select>
                                <select id="att_subject" class="input" style="display:none;"></select>
                            </div>
                            <?php endif; ?>
                        </div>

                        <div style="text-align:right; margin-top:15px;">
                            <?php if(has_granular_access('exam_attendance_sheet_print')): ?>
                            <button class="btn btn-success" data-access-id="exam_attendance_sheet_print" style="padding:10px 30px;" onclick="generateAttendanceSheet('manual')">
                                <i class="fas fa-print"></i> Print Sheet <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i>
                            </button>
                            <?php endif; ?>
                        </div>
                    </div>
                </div>
                
                <div id="hall_preview" style="margin-top:20px;"></div>
            </div>

            <!-- Settings Tab -->
            <div id="settings-tab" class="content-sec <?php echo ($default_tab == 'settings-tab') ? 'active' : ''; ?>">
                <div class="spongy-card" style="padding: 30px;">
                    <div class="chart-title" style="color:var(--text-main); margin-bottom:20px;">
                        <i class="fas fa-sliders-h"></i> Exam Promotion Rules
                    </div>
                    <div class="form-group">
                        <label>Passing Percentage (%)</label>
                        <input type="number" id="set_pass_perc" class="input" placeholder="e.g. 40">
                    </div>
                    <div class="form-group" style="margin-top:10px;">
                        <label>Minimum Passing Papers for Promotion (0 = All)</label>
                        <input type="number" id="set_min_papers" class="input" placeholder="e.g. 5">
                    </div>
                    <div style="display:flex; gap:10px; margin-top:20px;">
                        <button class="btn btn-success" data-access-id="exam_settings_save" style="flex:1;" onclick="saveSettings()"><i class="fas fa-save"></i> Save Settings <i class="fas fa-lock ml-2 access-control-icon" style="color:#f1c40f;"></i></button>
                        <button class="btn btn-danger" onclick="resetSettings()" style="width:auto;"><i class="fas fa-undo"></i> Reset Defaults</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <?php endif; ?>

    <!-- Core Dashboard Scripts (Early Load for Reliability) -->
    <script>
        function toggleModule(header) {
            header.classList.toggle('active');
            const content = header.nextElementSibling;
            if (content) content.classList.toggle('expanded');
            const icon = header.querySelector('.chevron');
            if (icon) {
                icon.classList.toggle('fa-chevron-down');
                icon.classList.toggle('fa-chevron-up');
            }
        }
    </script>

    <script>
        function syncStaffUnified(el, classId, sectionId, subjectId = null) {
            const val = el.value;
            if(!val) return;
            const parts = val.split('|');
            const text = el.options[el.selectedIndex].text;
            const textParts = text.split(' - ');

            const classEl = document.getElementById(classId);
            const sectionEl = document.getElementById(sectionId);
            
            // For Class
            if(classEl) {
                classEl.innerHTML = `<option value="${parts[0]}">${textParts[0]}</option>`;
                classEl.value = parts[0];
            }
            
            // For Section
            if(sectionEl) {
                sectionEl.innerHTML = `<option value="${parts[1]}">${textParts[1]}</option>`;
                sectionEl.value = parts[1];
            }
            
            if(subjectId && parts[2]) {
                const subEl = document.getElementById(subjectId);
                if(subEl) {
                    subEl.innerHTML = `<option value="${parts[2]}">${textParts[2]}</option>`;
                    subEl.value = parts[2];
                }
            }
        }

        function addNewSemester() {
            const newSemester = prompt("Enter new Semester name (e.g., Semester III):");
            if (newSemester && newSemester.trim() !== "") {
                const select = document.getElementById('e_semester');
                const option = document.createElement("option");
                option.text = newSemester.trim();
                option.value = newSemester.trim();
                select.add(option);
                select.value = newSemester.trim();
                alert("New semester added to dropdown");
            }
        }

        // Tab Switching (Already moved up)

        // NEW: Statistics Charts Initialization
        function initializeCharts() {
            const isTactile = document.body.classList.contains('theme-tactile');
            const textColor = isTactile ? '#1e293b' : 'rgba(255,255,255,0.8)';
            const gridColor = isTactile ? 'rgba(0,0,0,0.05)' : 'rgba(255,255,255,0.1)';
            const tickColor = isTactile ? '#64748b' : 'rgba(255,255,255,0.6)';

            // Exam Type Distribution Chart
            const examTypeCtx = document.getElementById('examTypeChart').getContext('2d');
            const examTypeData = STATS_DATA.exam_type_dist || [];
            const examTypeLabels = examTypeData.map(item => item.exam_type);
            const examTypeCounts = examTypeData.map(item => item.count);
            
            if(examTypeLabels.length > 0) {
                new Chart(examTypeCtx, {
                    type: 'doughnut',
                    data: {
                        labels: examTypeLabels,
                        datasets: [{
                            data: examTypeCounts,
                            backgroundColor: [
                                'rgba(52, 152, 219, 0.8)',
                                'rgba(46, 204, 113, 0.8)',
                                'rgba(155, 89, 182, 0.8)',
                                'rgba(241, 196, 15, 0.8)',
                                'rgba(230, 126, 34, 0.8)',
                                'rgba(231, 76, 60, 0.8)'
                            ],
                            borderColor: [
                                'rgba(52, 152, 219, 1)',
                                'rgba(46, 204, 113, 1)',
                                'rgba(155, 89, 182, 1)',
                                'rgba(241, 196, 15, 1)',
                                'rgba(230, 126, 34, 1)',
                                'rgba(231, 76, 60, 1)'
                            ],
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: {
                                position: 'bottom',
                                labels: {
                                    color: textColor,
                                    padding: 20
                                }
                            }
                        }
                    }
                });
            }

            // Monthly Trend Chart
            const monthlyCtx = document.getElementById('monthlyTrendChart').getContext('2d');
            const monthlyData = STATS_DATA.monthly_trend || [];
            const monthlyLabels = monthlyData.map(item => item.month ? item.month.substring(0, 3) : '');
            const monthlyExamCounts = monthlyData.map(item => item.exam_count || 0);
            const monthlyStudentCounts = monthlyData.map(item => item.student_count || 0);
            
            if(monthlyLabels.length > 0) {
                new Chart(monthlyCtx, {
                    type: 'line',
                    data: {
                        labels: monthlyLabels,
                        datasets: [
                            {
                                label: 'Exams',
                                data: monthlyExamCounts,
                                borderColor: 'rgba(52, 152, 219, 1)',
                                backgroundColor: 'rgba(52, 152, 219, 0.1)',
                                tension: 0.4,
                                fill: true
                            },
                            {
                                label: 'Students',
                                data: monthlyStudentCounts,
                                borderColor: 'rgba(46, 204, 113, 1)',
                                backgroundColor: 'rgba(46, 204, 113, 0.1)',
                                tension: 0.4,
                                fill: true
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: {
                                labels: {
                                    color: textColor
                                }
                            }
                        },
                        scales: {
                            x: {
                                grid: {
                                    color: gridColor
                                },
                                ticks: {
                                    color: tickColor
                                }
                            },
                            y: {
                                grid: {
                                    color: gridColor
                                },
                                ticks: {
                                    color: tickColor
                                },
                                beginAtZero: true
                            }
                        }
                    }
                });
            }
        }

        // NEW: Load Statistics with Filters
        async function loadStatistics() {
            const period = document.getElementById('stats_period').value;
            const classId = document.getElementById('stats_class').value;
            const examType = document.getElementById('stats_exam_type').value;
            
            try {
                const response = await fetch(`exams_api.php?action=get_statistics&period=${period}&class_id=${classId}&exam_type=${examType}`);
                const data = await response.json();
                
                if(data.status === 'success') {
                    updateStatisticsDisplay(data.data);
                }
            } catch(error) {
                console.error('Error loading statistics:', error);
            }
        }

        // NEW: Update Statistics Display
        function updateStatisticsDisplay(data) {
            // Update key stats cards
            if(data.total_exams !== undefined) {
                document.getElementById('stat_total_exams').textContent = data.total_exams;
            }
            if(data.pass_rate !== undefined) {
                document.getElementById('stat_pass_rate').textContent = data.pass_rate + '%';
            }
            if(data.total_students !== undefined) {
                document.getElementById('stat_total_students').textContent = data.total_students;
            }
            if(data.avg_score !== undefined) {
                document.getElementById('stat_avg_score').textContent = data.avg_score + '%';
            }
            
            // Update recent exams table
            if(data.recent_exams && Array.isArray(data.recent_exams)) {
                const recentExamsHtml = data.recent_exams.map(exam => `
                    <tr>
                        <td>${exam.exam_type}</td>
                        <td>${new Date(exam.exam_date).toLocaleDateString('en-US', {month: 'short', day: 'numeric'})}</td>
                        <td><span class="badge badge-info">${exam.students_count || 0}</span></td>
                    </tr>
                `).join('');
                document.getElementById('recentExamsTable').innerHTML = recentExamsHtml || '<tr><td colspan="3">No recent exams</td></tr>';
            }
            
            // Update subject performance table
            if(data.subject_performance && Array.isArray(data.subject_performance)) {
                const subjectHtml = data.subject_performance.map(subject => {
                    const passRate = subject.total_attempts > 0 ? Math.round((subject.passed / subject.total_attempts) * 100) : 0;
                    let performanceClass = 'high';
                    if(passRate < 60) performanceClass = 'low';
                    else if(passRate < 80) performanceClass = 'medium';
                    
                    return `
                        <tr>
                            <td>${subject.subject_name}</td>
                            <td><span style="font-weight: bold; color: ${performanceClass === 'high' ? '#2ecc71' : performanceClass === 'medium' ? '#f1c40f' : '#e74c3c'}">${Math.round(subject.avg_percentage)}%</span></td>
                            <td>${passRate}%</td>
                            <td>${subject.total_attempts}</td>
                            <td>
                                <div class="progress-bar">
                                    <div class="progress-fill ${performanceClass}" style="width: ${passRate}%"></div>
                                </div>
                            </td>
                        </tr>
                    `;
                }).join('');
                document.getElementById('subjectPerformanceTable').innerHTML = subjectHtml || '<tr><td colspan="5">No subject data</td></tr>';
            }
            
            // Update top classes list
            if(data.top_classes && Array.isArray(data.top_classes)) {
                const topClassesHtml = data.top_classes.map(cls => `
                    <div style="margin-bottom: 10px; padding: 10px; background: rgba(255,255,255,0.05); border-radius: 8px;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span style="color: white; font-size: 13px;">
                                ${cls.class_name}
                                ${cls.section_name ? ` (${cls.section_name})` : ''}
                            </span>
                            <span class="badge badge-success">${Math.round(cls.avg_percentage)}%</span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill high" style="width: ${Math.min(cls.avg_percentage, 100)}%"></div>
                        </div>
                        <div style="font-size: 11px; color: rgba(255,255,255,0.6); margin-top: 5px;">
                            ${cls.student_count} students
                        </div>
                    </div>
                `).join('');
                document.getElementById('topClassesList').innerHTML = topClassesHtml || '<div style="text-align: center; padding: 20px; color: rgba(255,255,255,0.5);">No class data</div>';
            }
        }

        // NEW: Export Statistics
        function exportStats(format) {
            const period = document.getElementById('stats_period').value;
            const classId = document.getElementById('stats_class').value;
            const examType = document.getElementById('stats_exam_type').value;
            
            window.open(`exams_api.php?action=export_stats&format=${format}&period=${period}&class_id=${classId}&exam_type=${examType}`, '_blank');
        }

        // Module Toggle
        function toggleModule(header) {
            const content = header.nextElementSibling;
            const isExpanded = content.classList.contains('expanded');
            
            // Close all modules
            document.querySelectorAll('.module-content').forEach(m => m.classList.remove('expanded'));
            document.querySelectorAll('.module-header').forEach(h => h.classList.remove('active'));
            
            // Open clicked module if it wasn't expanded
            if (!isExpanded) {
                content.classList.add('expanded');
                header.classList.add('active');
            }
        }

        // Entry Tab Switching
        function switchEntryTab(tab, btn) {
            document.querySelectorAll('.entry-sub-tab').forEach(c => c.style.display = 'none');
            document.getElementById('entry_' + tab).style.display = 'block';
            document.querySelectorAll('#entry_methods .simple-sub-tab').forEach(t => t.classList.remove('active'));
            btn.classList.add('active');
        }

        // Initial Load
        document.addEventListener('DOMContentLoaded', () => {
             loadHallStaff();
             
             // Initialize statistics if on stats tab
             if(document.getElementById('stats-tab').classList.contains('active')) {
                 initializeCharts();
             }
        });

        // ALL ORIGINAL FUNCTIONS REMAIN EXACTLY AS THEY WERE
        // I'm including ALL the original JavaScript functions here
        // They are exactly the same as in your original file

        async function loadHallStaff() {
            const res = await fetch('exams_api.php?action=get_staff_list');
            try {
                const data = await res.json();
                let html = '';
                data.forEach(s => {
                    html += `
                        <label style="display:flex; align-items:center; gap:10px; padding:5px; background:rgba(0,0,0,0.2); margin-bottom:5px; border-radius:4px; cursor:pointer;">
                            <input type="checkbox" class="hall-staff-cb" value="${s.id}" data-name="${s.full_name}">
                            ${s.full_name}
                        </label>
                    `;
                });
                document.getElementById('staff_list_hall').innerHTML = html;
            } catch(e) {
                document.getElementById('staff_list_hall').innerHTML = 'Error loading staff';
            }
        }

        async function loadHallSubjects() {
            const date = document.getElementById('h_date').value;
            const shift = document.getElementById('h_shift').value;
            if(!date) return;

            const res = await fetch(`exams_api.php?action=get_scheduled_subjects&date=${date}&shift=${shift}`);
            const data = await res.json();
            let html = '<option value="">Select Scheduled Subject</option>';
            data.forEach(s => {
                 html += `<option value="${s.id}">${s.name}</option>`;
            });
            document.getElementById('h_subject').innerHTML = html;
        }

        function addRoomRow() {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><input type="text" class="input r-name" placeholder="e.g. Hall B"></td>
                <td><input type="number" class="input r-cap" placeholder="30" value="30"></td>
                <td><i class="fas fa-trash" style="color:#e74c3c; cursor:pointer;" onclick="removeRoomRow(this)"></i></td>
            `;
            document.getElementById('room_tbody').appendChild(tr);
        }

        function removeRoomRow(btn) {
            btn.closest('tr').remove();
        }

        async function generateHall() {
            const date = document.getElementById('h_date').value;
            const shift = document.getElementById('h_shift').value;
            const subId = document.getElementById('h_subject').value;
            
            if(!date || !subId) return alert('Please select Date and Subject');

            // Collect Rooms
            const rooms = [];
            document.querySelectorAll('#room_tbody tr').forEach(tr => {
                const name = tr.querySelector('.r-name').value;
                const cap = tr.querySelector('.r-cap').value;
                if(name && cap) rooms.push({name, cap});
            });

            if(rooms.length === 0) return alert('Please add at least one room');

            // Collect Staff
            const staff = [];
            document.querySelectorAll('.hall-staff-cb:checked').forEach(cb => {
                staff.push({id: cb.value, name: cb.dataset.name});
            });

            if(staff.length === 0) return alert('Please select at least one invigilator');

            const payload = {
                date, shift, subject_id: subId,
                rooms: JSON.stringify(rooms),
                staff: JSON.stringify(staff)
            };
            
            // To POST parameters properly as FormData
            const fd = new FormData();
            for(const k in payload) fd.append(k, payload[k]);

            document.getElementById('hall_preview').innerHTML = '<div style="text-align:center; padding:20px;"><i class="fas fa-spinner fa-spin fa-2x"></i><br>Arranging...</div>';

            const res = await fetch('exams_api.php?action=generate_hall_view', { method: 'POST', body: fd });
            const html = await res.text(); // Expecting HTML output for print
            document.getElementById('hall_preview').innerHTML = html;
            
            // Scroll to preview
            document.getElementById('hall_preview').scrollIntoView({behavior: 'smooth'});
        }

        async function loadSections(cid, targetId) {
            const res = await fetch(`fee_api.php?action=get_sections&class_id=${cid}`);
            const data = await res.json();
            let html = '<option value="">Select Section</option>';
            data.forEach(s => html += `<option value="${s.id}">${s.name}</option>`);
            document.getElementById(targetId).innerHTML = html;
            
            // Also load subjects if the target is for creation
            if(targetId === 'e_section') loadSubjects(cid);
        }

        // Print Date Sheet
        function printDateSheet() {
            const type = document.getElementById('e_type').value;
            const year = document.getElementById('e_year').value;
            const class_id = document.getElementById('e_class').value;
            const section_id = document.getElementById('e_section').value;
            
            // Validations relaxed for bulk printing
            if(!type && !year && !class_id && !section_id) {
                if(!confirm('You have not selected any filters. This will print ALL exams. Continue?')) return;
            }
            
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = 'exams_api.php?action=print_datesheet';
            form.target = '_blank';
            
            const fields = {type, year, class_id, section_id};
            for(let key in fields) {
                let input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = fields[key];
                form.appendChild(input);
            }
            
            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        }

        async function loadSubjects(cid) {
            const res = await fetch(`exams_api.php?action=get_subjects&class_id=${cid}`);
            const data = await res.json();
            let html = '<option value="">Select Subject</option>';
            data.forEach(s => html += `<option value="${s.id}">${s.name}</option>`);
            document.getElementById('e_subject').innerHTML = html;
        }

        // Hall Views Logic
        function openHallView(view) {
            document.getElementById('hall-menu').style.display = 'none';
            document.getElementById('hall-view-header').style.display = 'flex';
            document.getElementById('hall-view-title').innerText = (view === 'arrangement') ? 'Hall Arrangement' : 'Attendance Sheet';
            
            document.querySelectorAll('#hall-tab > div[id^="view-"]').forEach(d => d.style.display = 'none');
            document.getElementById('view-' + view).style.display = 'block';
            
            if(view === 'attendance') {
                loadTodaysPapers();
            }
        }

        function closeHallView() {
            document.querySelectorAll('#hall-tab > div[id^="view-"]').forEach(d => d.style.display = 'none');
            document.getElementById('hall-view-header').style.display = 'none';
            document.getElementById('hall-menu').style.display = 'flex';
        }

        async function loadTodaysPapers() {
            const date = document.getElementById('att_date').value;
            const target = document.getElementById('todays_papers_grid');
            
            if(!date) return;
            // target.innerHTML = 'Loading...'; 
            
            const res = await fetch(`exams_api.php?action=get_daily_exams&date=${date}`);
            const data = await res.json();
            
            if(data.length === 0) {
                target.innerHTML = `<div style="width:100%; text-align:center; padding:20px; background:rgba(255,255,255,0.05); border-radius:10px;">No exams found for ${date}.</div>`;
                return;
            }
            
            let html = '';
            data.forEach(ex => {
                const isMorning = ex.shift === 'Morning';
                html += `
                    <div onclick="generateAttendanceSheet('direct', '${ex.class_id}', '${ex.section_id}', '${ex.subject_id}|${ex.shift}')" 
                         class="today-paper-card"
                         style="background:var(--card-bg); border:1px solid rgba(255,255,255,0.05); padding:20px; border-radius:15px; cursor:pointer; width:240px; position:relative; overflow:hidden; transition:all 0.3s ease; box-shadow: 0 10px 20px rgba(0,0,0,0.1);">
                         <div style="position:absolute; top:0; right:0; background:${isMorning?'#f1c40f':'#3498db'}; color:${isMorning?'#000':'#fff'}; font-size:10px; padding:4px 12px; border-radius:0 0 0 12px; font-weight:800; text-transform:uppercase; letter-spacing:1px; box-shadow: -2px 2px 10px rgba(0,0,0,0.1);">${ex.shift}</div>
                         
                         <div style="font-size:11px; opacity:0.5; font-weight:700; text-transform:uppercase; letter-spacing:1px; margin-bottom:8px; color:var(--text-main);">${ex.exam_type}</div>
                         <h4 style="margin:0 0 5px 0; font-size:18px; color:var(--text-main); font-weight:800; line-height:1.2;">${ex.subject_name}</h4>
                         <div style="display:flex; align-items:center; gap:8px; margin-top:12px;">
                            <div style="width:30px; height:30px; border-radius:50%; background:rgba(46, 204, 113, 0.1); color:#2ecc71; display:flex; align-items:center; justify-content:center; font-size:12px;"><i class="fas fa-layer-group"></i></div>
                            <div style="font-size:13px; font-weight:700; color:var(--text-main); opacity:0.8;">${ex.class_name} <span style="opacity:0.5;">-</span> ${ex.section_name}</div>
                         </div>
                         
                         <div style="position:absolute; bottom:-10px; right:-10px; font-size:60px; color:rgba(255,255,255,0.02); z-index:0;"><i class="fas fa-file-alt"></i></div>
                    </div>
                `;
            });
            target.innerHTML = html;
        }

        // Modified wrapper for Class Change to auto-select section
        async function handleClassChange(cid) {
             await loadSections(cid, 'att_section');
             const secSelect = document.getElementById('att_section');
             if(secSelect.options.length > 1) {
                 secSelect.selectedIndex = 1; // Select first actual option (0 is 'Select Section')
             }
             loadAttManualExams();
        }
        
        document.getElementById('att_class').setAttribute('onchange', 'handleClassChange(this.value)');

        async function loadAttManualExams() {
            const date = document.getElementById('att_date').value;
            const cid = document.getElementById('att_class').value;
            const sid = document.getElementById('att_section').value;
            const sel = document.getElementById('att_subject');
            
            sel.innerHTML = '<option>Loading...</option>';
            
            if(!date || !cid || !sid) {
                sel.innerHTML = '<option value="">Select Paper</option>';
                return;
            }
            
            const res = await fetch(`exams_api.php?action=get_daily_exams&date=${date}&class_id=${cid}&section_id=${sid}`);
            const data = await res.json();
            
            sel.innerHTML = '<option value="">Select Paper</option>';
            if(data.length > 0) {
               data.forEach(ex => {
                   sel.innerHTML += `<option value="${ex.subject_id}|${ex.shift}">${ex.subject_name} (${ex.shift})</option>`;
               });
               // Auto select first
               sel.selectedIndex = 1;
            }
        }

        function generateAttendanceSheet(mode = 'manual', cid=null, sid=null, subVal=null) {
            let date = document.getElementById('att_date').value;
            let val = '';
            let classId = cid;
            let sectionId = sid;

            if(mode === 'manual') {
                val = document.getElementById('att_subject').value;
                classId = document.getElementById('att_class').value;
                sectionId = document.getElementById('att_section').value;
            } else {
                val = subVal;
            }

            if(!date || !val) return alert('Data missing. Please select a paper.');

            const parts = val.split('|');
            const subId = parts[0];
            const shift = parts[1];
            
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = 'exams_api.php?action=generate_attendance_sheet';
            form.target = '_blank';
            
            const fields = {date, shift, subject_id: subId, class_id: classId, section_id: sectionId};
            for(let key in fields) {
                let input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = fields[key];
                form.appendChild(input);
            }
            
            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        }


        let assignedSubjects = {};

        function handleMultiSubjectSelect(isStaff) {
            let selectedOptions = [];
            if(isStaff) {
                selectedOptions = Array.from(document.getElementById('staff_unified_multiselect').selectedOptions);
            } else {
                selectedOptions = Array.from(document.getElementById('e_subject').selectedOptions);
            }
            
            let startDateStr = document.getElementById('e_date').value || new Date().toISOString().split('T')[0];
            let maxTime = new Date(startDateStr).getTime() - 86400000; // Subtract 1 day so first item gets StartDate
            
            Object.values(assignedSubjects).forEach(s => {
                let t = new Date(s.date).getTime();
                if(t > maxTime) maxTime = t;
            });

            let dTime = document.getElementById('e_time').value || '09:00';
            let dEndTime = document.getElementById('e_end_time').value || '';
            let dShift = document.getElementById('e_shift').value || 'Morning';
            let dTotal = document.getElementById('e_total').value || '100';

            let newAssigned = {};
            
            selectedOptions.forEach(opt => {
                if(!opt.value) return;
                
                let uniqueKey, cid, sid, subid, sname;
                if(isStaff) {
                    let parts = opt.value.split('|');
                    cid = parts[0]; sid = parts[1]; subid = parts[2];
                    uniqueKey = opt.value;
                    sname = opt.text;
                } else {
                    subid = opt.value;
                    uniqueKey = subid;
                    sname = opt.text;
                }

                if (assignedSubjects[uniqueKey]) {
                    newAssigned[uniqueKey] = assignedSubjects[uniqueKey];
                } else {
                    maxTime += 86400000;
                    let d = new Date(maxTime);
                    let iso = d.getFullYear() + "-" + String(d.getMonth()+1).padStart(2,'0') + "-" + String(d.getDate()).padStart(2,'0');
                    
                    newAssigned[uniqueKey] = {
                        key: uniqueKey,
                        class_id: cid,
                        section_id: sid,
                        subject_id: subid,
                        name: sname,
                        date: iso,
                        time: dTime,
                        end_time: dEndTime,
                        shift: dShift,
                        total: dTotal
                    };
                }
            });
            
            assignedSubjects = newAssigned;
            renderDynamicSubjects();
        }

        function renderDynamicSubjects() {
            const list = document.getElementById('dynamic_subjects_list');
            const wrapper = document.getElementById('dynamic_subjects_wrapper');
            list.innerHTML = '';
            
            let keys = Object.keys(assignedSubjects);
            keys.sort((a,b) => new Date(assignedSubjects[a].date) - new Date(assignedSubjects[b].date));
            if(keys.length === 0) {
                wrapper.style.display = 'none';
                return;
            }
            wrapper.style.display = 'block';
            let headerHtml = `
                <div style="display:flex; gap:10px; padding:0 10px; margin-bottom:5px; font-size:11px; font-weight:800; color:var(--primary); text-transform:uppercase; letter-spacing:0.5px; opacity:0.8; flex-wrap:wrap;">
                    <div style="flex:2; min-width:150px;">Subject Name</div>
                    <div style="flex:1; min-width:130px;">Exam Date</div>
                    <div style="flex:1; min-width:100px;">Start Time</div>
                    <div style="flex:1; min-width:100px;">End Time</div>
                    <div style="flex:1; min-width:100px;">Shift</div>
                    <div style="width:70px; display:flex; align-items:center; gap:5px;">Marks</div>
                    <div style="width:20px;"></div>
                </div>
            `;
            list.innerHTML += headerHtml;
            
            let index = 1;
            keys.forEach(k => {
                let s = assignedSubjects[k];
                let html = `
                    <div style="display:flex; gap:10px; align-items:center; background:rgba(0,0,0,0.02); padding:10px; border-radius:8px; border:1px solid rgba(0,0,0,0.05); flex-wrap:wrap;">
                        <div style="flex:2; font-weight:700; font-size:13px; min-width:150px;">
                            <span style="display:inline-block; width:20px; height:20px; background:var(--primary); color:white; text-align:center; border-radius:50%; font-size:10px; line-height:20px; margin-right:5px;">${index++}</span>
                            ${s.name}
                        </div>
                        <input type="date" class="input" style="flex:1; min-width:130px; height:35px; font-size:12px;" value="${s.date}" onchange="assignedSubjects['${k}'].date = this.value">
                        <input type="time" class="input" style="flex:1; min-width:100px; height:35px; font-size:12px;" value="${s.time}" onchange="assignedSubjects['${k}'].time = this.value" title="Start Time">
                        <input type="time" class="input" style="flex:1; min-width:100px; height:35px; font-size:12px;" value="${s.end_time || ''}" onchange="assignedSubjects['${k}'].end_time = this.value" title="End Time">
                        <select class="input" style="flex:1; min-width:110px; height:35px; font-size:12px; padding:0 25px 0 10px !important; line-height:1 !important; text-align:left; box-sizing:border-box;" onchange="assignedSubjects['${k}'].shift = this.value" title="Shift">
                            <option value="Morning" ${s.shift=='Morning'?'selected':''}>Morning</option>
                            <option value="Evening" ${s.shift=='Evening'?'selected':''}>Evening</option>
                        </select>
                        <div style="display:flex; align-items:center; gap:5px;"><span style="font-size:10px; opacity:0.6;">Marks:</span><input type="number" class="input" style="width:70px; height:35px; font-size:12px;" value="${s.total}" onchange="assignedSubjects['${k}'].total = this.value"></div>
                        <i class="fas fa-trash" style="color:#e74c3c; cursor:pointer;" onclick="removeAssigned('${k}')"></i>
                    </div>
                `;
                list.innerHTML += html;
            });
        }

        function removeAssigned(key) {
            delete assignedSubjects[key];
            let isStaff = !!document.getElementById('staff_unified_multiselect');
            let sel = isStaff ? document.getElementById('staff_unified_multiselect') : document.getElementById('e_subject');
            for(let i=0; i<sel.options.length; i++){
                if((isStaff && sel.options[i].value == key) || (!isStaff && sel.options[i].value == key)){
                    sel.options[i].selected = false;
                }
            }
            // Trigger change so UI checkboxes sync correctly
            sel.dispatchEvent(new Event('change'));
            renderDynamicSubjects();
        }

        
        // Checkbox Multiselect Engine
        function initCheckboxMultiselect(selectElementId) {
            const select = document.getElementById(selectElementId);
            if(!select) return;

            const wrapper = document.createElement('div');
            wrapper.style.border = '1px solid rgba(0,0,0,0.1)';
            wrapper.style.borderRadius = '12px';
            wrapper.style.height = '150px';
            wrapper.style.overflowY = 'auto';
            wrapper.style.padding = '10px 15px';
            wrapper.style.background = 'var(--card-bg)';
            wrapper.style.boxShadow = 'inset 0 2px 4px rgba(0,0,0,0.02)';
            wrapper.className = 'checkbox-multiselect-container scroll-beautiful';
            
            select.parentNode.insertBefore(wrapper, select);
            select.style.display = 'none';

            function renderCheckboxes() {
                wrapper.innerHTML = '';
                let validOptionsCount = 0;
                Array.from(select.options).forEach((opt, index) => {
                    if(!opt.value) return; 
                    validOptionsCount++;
                    const label = document.createElement('label');
                    label.style.display = 'flex';
                    label.style.alignItems = 'center';
                    label.style.gap = '10px';
                    label.style.padding = '8px 5px';
                    label.style.cursor = 'pointer';
                    label.style.fontSize = '14px';
                    label.style.fontWeight = '700';
                    label.style.color = '#334155';
                    label.style.borderBottom = '1px dashed rgba(0,0,0,0.1)';
                    label.style.transition = '0.2s';
                    
                    label.onmouseover = () => label.style.background = 'rgba(0,0,0,0.02)';
                    label.onmouseout = () => label.style.background = 'transparent';
                    
                    const cb = document.createElement('input');
                    cb.type = 'checkbox';
                    cb.value = opt.value;
                    cb.checked = opt.selected;
                    cb.style.width = '18px';
                    cb.style.height = '18px';
                    cb.style.cursor = 'pointer';
                    cb.style.accentColor = 'var(--primary)';
                    
                    cb.onchange = function() {
                        opt.selected = cb.checked;
                        select.dispatchEvent(new Event('change'));
                    };
                    
                    label.appendChild(cb);
                    label.appendChild(document.createTextNode(opt.text));
                    wrapper.appendChild(label);
                });
                
                if(validOptionsCount === 0) {
                    wrapper.innerHTML = '<div style="opacity:0.5; font-size:13px; padding:15px; text-align:center; font-style:italic;">No options available...</div>';
                }
            }

            renderCheckboxes();

            const observer = new MutationObserver(() => {
                renderCheckboxes();
            });
            observer.observe(select, { childList: true });

            select.addEventListener('change', () => {
                Array.from(wrapper.querySelectorAll('input[type="checkbox"]')).forEach(cb => {
                     let opt = Array.from(select.options).find(o => o.value == cb.value);
                     if(opt) cb.checked = opt.selected;
                });
            });
        }

        // Initialize immediately after DOM ready
        document.addEventListener('DOMContentLoaded', () => {
            initCheckboxMultiselect('e_subject');
            initCheckboxMultiselect('staff_unified_multiselect');
        });
        
        // Also call directly in case DOM is already loaded
        setTimeout(() => {
            if(!document.querySelector('#e_subject').previousElementSibling?.classList.contains('checkbox-multiselect-container')) {
                initCheckboxMultiselect('e_subject');
                initCheckboxMultiselect('staff_unified_multiselect');
            }
        }, 500);

        async function createExam(quick = false) {

                        const formData = new FormData();
            formData.append('type', document.getElementById('e_type').value);
            formData.append('semester', document.getElementById('e_semester').value);
            formData.append('year', document.getElementById('e_year').value);
            
            // Only required if not staff (using single class)
            let ec = document.getElementById('e_class');
            let es = document.getElementById('e_section');
            if(ec) formData.append('class_id', ec.value);
            if(es) formData.append('section_id', es.value);

            let subjectsPayload = [];
            Object.values(assignedSubjects).forEach(s => {
                subjectsPayload.push(s);
            });

            if(subjectsPayload.length === 0) {
                return alert("Please select at least one subject to schedule.");
            }
            formData.append('subjects_json', JSON.stringify(subjectsPayload));

            const res = await fetch('exams_api.php?action=create_exam', { method: 'POST', body: formData });
            const data = await res.json();
            if(data.status === 'success') {
                alert(data.message || 'Exam Scheduled Successfully!');
                assignedSubjects = {};
                renderDynamicSubjects();
                
                // Clear form
                let ec = document.getElementById('e_class');
                if(ec) ec.value = '';
                let es = document.getElementById('e_section');
                if(es) es.innerHTML = '<option value="">Select Section</option>';
                document.getElementById('e_subject').innerHTML = '<option value="">Select Subject</option>';
                document.getElementById('e_date').value = '';
                document.getElementById('e_time').value = '';
            } else {
                alert('Error: ' + data.message);
            }
        }

        async function saveQuickDatasheet() {
            const text = document.getElementById('quick_datasheet_text').value;
            if(!text.trim()) return alert('Please enter datasheet text');

            const formData = new FormData();
            formData.append('text', text);
            formData.append('type', document.getElementById('q_type').value);
            formData.append('year', document.getElementById('q_year').value);

            const res = await fetch('exams_api.php?action=quick_schedule', { method: 'POST', body: formData });
            const data = await res.json();
            
            if(data.status === 'success') {
                alert(data.message);
                document.getElementById('quick_datasheet_text').value = '';
            } else {
                alert('Error: ' + data.message);
            }
        }

        async function loadAwardListExams() {
            const cid = document.getElementById('aw_class').value;
            const sid = document.getElementById('aw_section').value;
            console.log('loadAwardListExams called - Class:', cid, 'Section:', sid);
            if(!cid || !sid) return;
            const res = await fetch(`exams_api.php?action=get_exams&class_id=${cid}&section_id=${sid}`);
            const data = await res.json();
            console.log('Award List Exams loaded:', data);
            let html = '<option value="">Select Exam</option>';
            data.forEach(e => html += `<option value="${e.id}">${e.exam_type} - ${e.subject_name}</option>`);
            document.getElementById('aw_exam').innerHTML = html;
        }

        async function viewAwardList() {
            const eid = document.getElementById('aw_exam').value;
            if(!eid) return alert('Select Exam');
            
            const res = await fetch(`exams_api.php?action=get_students_for_marks&exam_id=${eid}&_=${new Date().getTime()}`);
            const data = await res.json();
            const students = data.students;
            const total = data.total_marks || 'N/A';
            
            const examText = document.getElementById('aw_exam').options[document.getElementById('aw_exam').selectedIndex].text;
            const classText = document.getElementById('aw_class').options[document.getElementById('aw_class').selectedIndex].text;
            const sectionText = document.getElementById('aw_section').options[document.getElementById('aw_section').selectedIndex].text;

            // --- Screen View (Single Column, Normal Look) ---
            const screenRows = students.map(s => `
                <tr id="row_${s.student_id}">
                    <td style="text-align:center;">${s.roll_number}</td>
                    <td>${s.full_name}</td>
                    <td style="text-align:center; font-weight:bold;">
                        <span class="mark-val">${s.marks}</span>
                        <input type="number" class="mark-input input" style="display:none; width:60px; padding:5px;" value="${s.marks}">
                    </td>
                    <td style="text-align:center;">
                        <div style="display:flex; justify-content:center; gap:5px;">
                            <button class="btn btn-sm btn-primary btn-edit" onclick="editStudentResult(this, ${eid}, ${s.student_id})" style="width:auto; padding:5px 10px; font-size:12px;"><i class="fas fa-pencil-alt"></i></button>
                            <button class="btn btn-sm btn-success btn-save" onclick="saveSingleResult(this, ${eid}, ${s.student_id})" style="display:none; width:auto; padding:5px 10px; font-size:12px;"><i class="fas fa-check"></i></button>
                            <button class="btn btn-sm btn-danger" onclick="deleteStudentResult(${eid}, ${s.student_id})" style="width:auto; padding:5px 10px; font-size:12px; background:#e74c3c;"><i class="fas fa-trash"></i></button>
                        </div>
                    </td>
                </tr>
            `).join('');

            const screenHTML = `
                <div id="award_screen_view">
                    <div style="margin-bottom: 20px; text-align:center;">
                        <h3>${examText}</h3>
                        <p>${classText} - ${sectionText}</p>
                    </div>
                    <table class="table table-bordered">
                        <thead>
                            <tr>
                                <th style="width:100px; text-align:center;">Roll No</th>
                                <th>Student Name</th>
                                <th style="width:100px; text-align:center;">Marks (${total})</th>
                                <th style="width:120px; text-align:center;">Actions</th>
                            </tr>
                        </thead>
                        <tbody>${screenRows}</tbody>
                    </table>
                    
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-top:20px;">
                        <button class="btn" onclick="deleteFullExamResults(${eid})" style="background:#e74c3c; color:white; width:auto; padding: 10px 20px;"><i class="fas fa-trash-alt"></i> Delete Entire List</button>
                        <button class="btn" onclick="window.print()" style="background:#f1c40f; color:#000; width:auto; padding: 10px 30px;"><i class="fas fa-print"></i> Print Award List</button>
                    </div>
                </div>
            `;


            // --- Print View (Two Columns, Hidden on Screen) ---
            const halfway = Math.ceil(students.length / 2);
            const col1 = students.slice(0, halfway);
            const col2 = students.slice(halfway);

            const tableRow = (s) => `
                <tr>
                    <td class="c-center" style="width:40px;">${s.roll_number}</td>
                    <td class="c-name">${s.full_name}</td>
                    <td class="c-center" style="width:60px; font-weight:bold;">${s.marks}</td>
                </tr>
            `;

            const instName = <?php echo json_encode($inst['name'] ?? 'Institution'); ?>;
            const printHTML = `
                <div id="award_list_print" style="display:none;">
                    <div class="print-header">
                        <h1>${instName}</h1>
                        <p>AWARD LIST</p>
                    </div>
                    
                    <div class="meta-grid">
                        <div class="meta-item">Exam: ${examText}</div>
                        <div class="meta-item">Class: ${classText} (${sectionText})</div>
                        <div class="meta-item">Total Marks: ${total}</div>
                        <div class="meta-item">Date: <?php echo date('d-M-Y'); ?></div>
                    </div>

                    <div class="award-table-container">
                        <div class="award-table-column">
                            <div class="table-rounded-box">
                                <table>
                                    <thead><tr><th style="width:40px;">Roll</th><th>Student Name</th><th style="width:60px;">Obt.</th></tr></thead>
                                    <tbody>${col1.map(tableRow).join('')}</tbody>
                                </table>
                            </div>
                        </div>
                        <div class="award-table-column">
                            <div class="table-rounded-box">
                                <table>
                                    <thead><tr><th style="width:40px;">Roll</th><th>Student Name</th><th style="width:60px;">Obt.</th></tr></thead>
                                    <tbody>${col2.map(tableRow).join('')}</tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                    
                    <div class="print-footer">
                        <div class="signature-line">Class Teacher</div>
                        <div class="signature-line">Principal</div>
                    </div>
                </div>
            `;
            
            document.getElementById('award_preview').innerHTML = screenHTML + printHTML;
        }

        function editStudentResult(btn, eid, sid) {
            const row = btn.closest('tr');
            row.querySelector('.mark-val').style.display = 'none';
            row.querySelector('.mark-input').style.display = 'block';
            row.querySelector('.btn-edit').style.display = 'none';
            row.querySelector('.btn-save').style.display = 'inline-block';
        }

        function saveSingleResult(btn, eid, sid) {
            const row = btn.closest('tr');
            if(!row) return;
            const input = row.querySelector('.mark-input');
            const val = input ? input.value : '0';
            const formData = new FormData();
            formData.append('exam_id', eid);
            formData.append('marks', JSON.stringify([{student_id: sid, marks: val}]));
            
            fetch('exams_api.php?action=save_marks', { method:'POST', body:formData })
                .then(r => r.json())
                .then(d => {
                    if(d.status === 'success') {
                        const vs = row.querySelector('.mark-val');
                        if(vs) vs.innerText = val;
                        row.querySelector('.mark-val').style.display = 'inline';
                        row.querySelector('.mark-input').style.display = 'none';
                        row.querySelector('.btn-edit').style.display = 'inline-block';
                        row.querySelector('.btn-save').style.display = 'none';
                    } else { alert('Error: ' + d.message); }
                }).catch(err => { console.error(err); alert('Failed to save result'); });
        }

        async function deleteStudentResult(eid, sid) {
            if(!confirm('Are you sure you want to delete this result?')) return;
            const formData = new FormData();
            formData.append('exam_id', eid);
            formData.append('student_id', sid);
            try {
                const res = await fetch('exams_api.php?action=delete_student_exam_result', { method: 'POST', body: formData });
                const data = await res.json();
                if(data.status === 'success') {
                    const row = document.getElementById('row_' + sid);
                    if(row) row.remove();
                } else {
                    alert('Error: ' + data.message);
                }
            } catch(e) { 
                console.error(e);
                alert('Failed to delete'); 
            }
        }

        async function deleteFullExamResults(eid) {
            if(!confirm('WARNING: Are you sure you want to delete the ENTIRE award list for this exam? This cannot be undone.')) return;
            
            const formData = new FormData();
            formData.append('exam_id', eid);

            try {
                const res = await fetch('exams_api.php?action=delete_full_exam_results', { method: 'POST', body: formData });
                const data = await res.json();
                if(data.status === 'success') {
                    alert('Award list deleted successfully.');
                    document.getElementById('award_preview').innerHTML = ''; // Clear view
                } else {
                    alert('Error: ' + data.message);
                }
            } catch(e) { alert('Failed to delete'); }
        }


        async function loadMarksExams() {
            const cid = document.getElementById('m_class').value;
            const sid = document.getElementById('m_section').value;
            console.log('loadMarksExams called - Class:', cid, 'Section:', sid);
            if(!cid || !sid) return;
            const res = await fetch(`exams_api.php?action=get_exams&class_id=${cid}&section_id=${sid}`);
            const data = await res.json();
            console.log('Marks Exams loaded:', data);
            let html = '<option value="">Select Exam</option>';
            data.forEach(e => html += `<option value="${e.id}">${e.exam_type} - ${e.subject_name}</option>`);
            document.getElementById('m_exam').innerHTML = html;
        }

        async function loadMarksEntryView() {
            const eid = document.getElementById('m_exam').value;
            if(!eid) {
                document.getElementById('entry_methods').style.display = 'none';
                return;
            }
            document.getElementById('entry_methods').style.display = 'block';
            
            const listContainer = document.getElementById('manual_list');
            listContainer.innerHTML = '<div style="text-align:center; padding:20px;"><i class="fas fa-spinner fa-spin"></i> Loading Students...</div>';

            try {
                const res = await fetch(`exams_api.php?action=get_students_for_marks&exam_id=${eid}&_=${new Date().getTime()}`);
                const data = await res.json();
                
                // Manual Table
                let html = `
                    <div style="background:rgba(255,255,255,0.05); padding:10px; border-radius:12px; margin-bottom:15px; display:flex; justify-content:space-between; align-items:center; border:1px solid rgba(255,255,255,0.1);">
                        <span>Total Marks: <b id="display_total" style="color:var(--primary);">${data.total_marks}</b></span>
                        <button class="btn btn-sm btn-danger" onclick="deleteFullExamResults(${eid})" style="width:auto; padding:5px 15px;"><i class="fas fa-trash-alt"></i> Delete All</button>
                    </div>
                    <div style="width:100%; overflow-x:auto;">
                        <table style="width:100%; border-collapse:collapse; color:white; background:rgba(255,255,255,0.03);">
                            <thead>
                                <tr style="background:rgba(52,152,219,0.2); border-bottom:2px solid rgba(52,152,219,0.5);">
                                    <th style="padding:4px 8px; text-align:center; width:50px; font-size:11px; font-weight:800;">CNO</th>
                                    <th style="padding:4px 8px; text-align:left; font-size:11px; font-weight:800;">NAME</th>
                                    <th style="padding:4px 8px; text-align:center; width:100px; font-size:11px; font-weight:800;">MARKS</th>
                                    <th style="padding:4px 8px; text-align:center; width:100px; font-size:11px; font-weight:800;">STATUS</th>
                                </tr>
                            </thead>
                            <tbody>
                `;
                if(data.students.length === 0) {
                    html += '<tr><td colspan="4" style="text-align:center; padding:20px;">No students enrolled in this section.</td></tr>';
                } else {
                    data.students.forEach(s => {
                        const marks = s.marks || '';
                        const isEntered = s.marks !== null && s.marks !== undefined && s.marks !== '';
                        html += `
                            <tr id="row_${s.student_id}" style="border-bottom:1px solid rgba(255,255,255,0.05);">
                                <td style="padding:4px 8px; text-align:center; font-size:12px;">${s.class_no || '-'}</td>
                                <td style="padding:4px 8px; font-size:12px; font-weight:600;">${s.full_name}</td>
                                <td style="padding:4px 8px; text-align:center;">
                                    <input type="number" class="input s-marks" 
                                           data-sid="${s.student_id}" 
                                           value="${marks}" 
                                           placeholder="0"
                                           onchange="this.parentElement.nextElementSibling.innerHTML='<span style=\'color:#f1c40f\'><i class=\'fas fa-sync\'></i> Pending</span>'"
                                           style="width:70px; padding:4px 6px; text-align:center; background:rgba(0,0,0,0.3); border:1px solid rgba(255,255,255,0.2); border-radius:4px; color:white; font-weight:700; font-size:13px;">
                                </td>
                                <td style="padding:4px 8px; text-align:center; font-size:11px;">
                                    ${isEntered ? '<span style="color:#2ecc71"><i class="fas fa-check-circle"></i> Saved</span>' : '<span style="color:rgba(255,255,255,0.3)"><i class="fas fa-clock"></i> Not Entered</span>'}
                                </td>
                            </tr>
                        `;
                    });
                }
                html += '</tbody></table></div>';
                listContainer.innerHTML = html;
            } catch(e) {
                console.error(e);
                listContainer.innerHTML = '<div style="text-align:center; padding:20px; color:#e74c3c;"><i class="fas fa-exclamation-triangle"></i> Failed to load students.</div>';
            }
        }

        // Standard SwitchTab is already defined at line 1312


        async function saveManualMarks() {
            const eid = document.getElementById('m_exam').value;
            if(!eid) return alert('Please select an exam first.');

            const marks = [];
            let hasData = false;
            document.querySelectorAll('.s-marks').forEach(input => {
                hasData = true;
                marks.push({ student_id: input.dataset.sid, marks: input.value });
            });

            if(!hasData) return alert('No students found to save.');

            const btn = document.querySelector('button[onclick="saveManualMarks()"]');
            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';

            const formData = new FormData();
            formData.append('exam_id', eid);
            formData.append('marks', JSON.stringify(marks));

            try {
                const res = await fetch('exams_api.php?action=save_marks', { method: 'POST', body: formData });
                // Check if response is valid JSON
                const text = await res.text();
                try {
                    const data = JSON.parse(text);
                    if(data.status === 'success') {
                        alert('Marks Saved Successfully!');
                    } else {
                        alert('Error: ' + (data.message || 'Unknown server error'));
                    }
                } catch(e) {
                    console.error('Server response:', text);
                    alert('Server Error: Invalid response format. See console for details.');
                }
            } catch(e) {
                console.error(e);
                alert('Network Error: Failed to save marks.');
            } finally {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }

        async function saveTextMarks() {
            const eid = document.getElementById('m_exam').value;
            const text = document.getElementById('bulk_text').value;
            
            const formData = new FormData();
            formData.append('exam_id', eid);
            formData.append('data', text);

            const res = await fetch('exams_api.php?action=save_text_marks', { method: 'POST', body: formData });
            const data = await res.json();
            if(data.status === 'success') {
                alert(`Successfully saved marks for ${data.count} students!`);
                if(data.errors.length > 0) alert('Errors:\n' + data.errors.join('\n'));
                loadMarksEntryView();
            }
        }

        function exportCSV() {
            const eid = document.getElementById('m_exam').value;
            if(!eid) return alert('Select Exam');
            window.location.href = `exams_api.php?action=export_csv&exam_id=${eid}`;
        }

        async function importCSV(input) {
            const eid = document.getElementById('m_exam').value;
            if(!input.files[0]) return;
            
            const formData = new FormData();
            formData.append('exam_id', eid);
            formData.append('csv', input.files[0]);

            const res = await fetch('exams_api.php?action=import_csv', { method: 'POST', body: formData });
            const data = await res.json();
            if(data.status === 'success') {
                alert('CSV Imported Successfully!');
                loadMarksEntryView();
            } else {
                alert('Error: ' + data.message);
            }
            input.value = '';
        }

        // Result Sub-tab Switch
        function switchResultSubTab(subTab, btn) {
            document.querySelectorAll('.res-sub-tab').forEach(d => d.style.display = 'none');
            const target = document.getElementById('res_' + subTab);
            if(target) target.style.display = 'block';
            if(btn) {
                btn.parentElement.querySelectorAll('.tab-btn, .simple-sub-tab').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            }
            
            // Auto-load for students
            <?php if($is_student): ?>
            if(typeof STUDENT_INFO !== 'undefined' && STUDENT_INFO) {
                if(subTab === 'dmc') {
                    loadResultExams(STUDENT_INFO.section_id, 'dmc_exam').then(() => autoLoadLatestDMC());
                } else if(subTab === 'result_card') {
                    loadResultExams(STUDENT_INFO.section_id, 'rc_exam').then(() => autoLoadLatestResultCard());
                } else if(subTab === 'performance') {
                    loadPerformance('class_toppers');
                } else if(subTab === 'slips') {
                    loadResultExams(STUDENT_INFO.section_id, 'slips_exam');
                }
            }
            <?php endif; ?>
        }
        
        // Force the active tab to re-trigger its logic on page load safely
        document.addEventListener('DOMContentLoaded', () => {
            setTimeout(() => {
                // Find any active tab under the result card view or spongy cards
                const activeBtn = document.querySelector('.tab-btn[onclick*="switchResultSubTab"].active') || document.querySelector('.simple-sub-tab[onclick*="switchResultSubTab"].active');
                if (activeBtn) {
                    activeBtn.click();
                } else {
                    // Fallback absolute trigger if neither works
                    if (typeof switchResultSubTab === 'function') {
                        switchResultSubTab('dmc');
                    }
                }
            }, 600);
        });

        function switchHallSubTab(subTab, btn) {
            document.querySelectorAll('.hall-sub-tab').forEach(d => d.style.display = 'none');
            const target = document.getElementById('hall_' + subTab);
            if(target) target.style.display = 'block';
            if(btn) {
                btn.parentElement.querySelectorAll('.tab-btn, .simple-sub-tab').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            }
            
            if(subTab === 'attendance') {
                loadTodaysPapers();
            }
        }

        <?php if($is_student): ?>
        // Auto-load latest DMC for students
        async function autoLoadLatestDMC() {
            const sectionId = document.getElementById('dmc_section').value;
            const rollNumber = document.getElementById('dmc_roll').value;
            
            console.log('Auto-loading DMC for student:', {sectionId, rollNumber});
            
            if(!sectionId) {
                console.error('No section ID found');
                return;
            }
            
            if(!rollNumber && USER_ROLE !== 'student') {
                console.warn('No roll number found for DMC');
                alert('Please enter a roll number or ensure your enrollment is complete.');
                return;
            }
            
            // Force single student mode for students
            const typeSelect = document.getElementById('dmc_type');
            if(typeSelect) typeSelect.value = 'single';
            
            try {
                // Load exams for the section
                console.log('Loading exams for section:', sectionId);
                await loadResultExams(sectionId, 'dmc_exam');
                
                // Wait a bit for the select to populate
                setTimeout(() => {
                    const examSelect = document.getElementById('dmc_exam');
                    console.log('Exam dropdown options:', examSelect?.options.length);
                    
                    if(examSelect && examSelect.options.length > 1) {
                        // Select the first exam (latest)
                        examSelect.selectedIndex = 1;
                        console.log('Selected exam:', examSelect.value);
                        // Auto-generate for ONLY this student
                        generateDMC();
                    } else {
                        console.log('No exams available for this class');
                    }
                }, 800);
            } catch(e) {
                console.error('Error auto-loading DMC:', e);
            }
        }

        // Auto-load latest Result Card for students
        
        async function generateStudentDatasheet() {
            const exam_type = document.getElementById('slips_exam').value;
            if(!exam_type) return alert('Please select an exam first');
            
            let cid = document.getElementById('dmc_class') ? document.getElementById('dmc_class').value : '';
            let sid = document.getElementById('dmc_section') ? document.getElementById('dmc_section').value : '';
            if (!cid && document.getElementById('rc_class')) cid = document.getElementById('rc_class').value;
            if (!sid && document.getElementById('rc_section')) sid = document.getElementById('rc_section').value;
            
            if(typeof STUDENT_INFO !== 'undefined' && STUDENT_INFO && STUDENT_INFO.class_id) {
                cid = STUDENT_INFO.class_id;
                sid = STUDENT_INFO.section_id;
            }
            
            if(!cid || !sid) return alert('Please select a class and section first from the DMC or Result Card tab filters.');
            
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = 'exams_api.php?action=print_datesheet';
            form.target = '_blank';
            
            const fields = {
                type: exam_type, 
                year: document.getElementById('dmc_year') ? document.getElementById('dmc_year').value : '<?php echo date("Y"); ?>', // Fallback
                class_id: cid, 
                section_id: sid
            };
            
            for(let key in fields) {
                let input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = fields[key];
                form.appendChild(input);
            }
            document.body.appendChild(form);
            form.submit();
        }

        async function generateStudentRollSlip() {
            const exam_type = document.getElementById('slips_exam').value;
            if(!exam_type) return alert('Please select an exam first');
            
            let cid = document.getElementById('dmc_class') ? document.getElementById('dmc_class').value : '';
            let sid = document.getElementById('dmc_section') ? document.getElementById('dmc_section').value : '';
            if (!cid && document.getElementById('rc_class')) cid = document.getElementById('rc_class').value;
            if (!sid && document.getElementById('rc_section')) sid = document.getElementById('rc_section').value;
            
            if(typeof STUDENT_INFO !== 'undefined' && STUDENT_INFO && STUDENT_INFO.class_id) {
                cid = STUDENT_INFO.class_id;
                sid = STUDENT_INFO.section_id;
            }
            
            if(!cid || !sid) return alert('Please select a class and section first from the DMC or Result Card tab filters.');
            
            const statusDiv = document.getElementById('slips_status');
            statusDiv.style.display = 'block';
            statusDiv.innerText = 'Checking fee status...';
            statusDiv.style.color = '#3498db';
            statusDiv.style.background = 'rgba(52, 152, 219, 0.1)';
            
            try {
                const res = await fetch(`exams_api.php?action=check_student_dues`);
                const data = await res.json();
                
                if(data.status === 'success' && data.has_dues) {
                    statusDiv.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ERROR: YOU HAVE PENDING FEE DUES (RS. ${data.balance}). PLEASE CLEAR YOUR DUES TO DOWNLOAD THE ROLL NO SLIP.`;
                    statusDiv.style.color = '#e74c3c';
                    statusDiv.style.background = 'rgba(231, 76, 60, 0.1)';
                    return;
                }
                
                statusDiv.style.display = 'none';
                
                // Route directly to roll number slip viewer
                const form = document.createElement('form');
                form.method = 'GET';
                form.action = 'exams/roll_no_slips.php';
                form.target = '_blank';
                
                let i1 = document.createElement('input'); i1.type = 'hidden'; i1.name = 'exam_type'; i1.value = exam_type; form.appendChild(i1);
                let i2 = document.createElement('input'); i2.type = 'hidden'; i2.name = 'class_id'; i2.value = cid; form.appendChild(i2);
                let i3 = document.createElement('input'); i3.type = 'hidden'; i3.name = 'section_id'; i3.value = sid; form.appendChild(i3);
                
                document.body.appendChild(form);
                form.submit();
                
            } catch(e) {
                statusDiv.innerText = 'Error verifying fee status.';
                statusDiv.style.color = '#e74c3c';
                statusDiv.style.background = 'rgba(231, 76, 60, 0.1)';
            }
        }

        async function autoLoadLatestResultCard() {
            const classId = document.getElementById('rc_class').value;
            const sectionId = document.getElementById('rc_section').value;
            const rollNumber = document.getElementById('rc_roll').value;
            
            console.log('Auto-loading Result Card for student:', {classId, sectionId, rollNumber});
            
            if(!classId || !sectionId) {
                console.error('Missing class or section data');
                return;
            }
            
            if(!rollNumber && USER_ROLE !== 'student') {
                console.warn('No roll number found for result card');
                alert('Please enter a roll number or ensure your enrollment is complete.');
                return;
            }
            
            setTimeout(() => {
                // Auto-generate with ONLY this student's data
                console.log('Generating result card for roll:', rollNumber);
                generateResultCard();
            }, 500);
        }

        // Auto-load DMC on page load for students
        document.addEventListener('DOMContentLoaded', () => {
            console.log('Page loaded, initializing student exam view...');
            
            // Load exams immediately on page load
            setTimeout(() => {
                let sectionId = '';
                const dmcSec = document.getElementById('dmc_section');
                if (dmcSec) sectionId = dmcSec.value;
                
                if(!sectionId && typeof USER_ROLE !== 'undefined' && USER_ROLE === 'student' && typeof STUDENT_INFO !== 'undefined' && STUDENT_INFO) {
                    sectionId = STUDENT_INFO.section_id;
                }
                
                if(sectionId) {
                    console.log('Loading exams for section on page load:', sectionId);
                    loadResultExams(sectionId, 'dmc_exam');
                }
            }, 500);
            
            // Auto-load DMC after exams are loaded
            setTimeout(() => {
                autoLoadLatestDMC();
            }, 1500);
        });
        <?php endif; ?>


        async function saveSettings() {
             const pass = document.getElementById('set_pass_perc').value;
             const papers = document.getElementById('set_min_papers').value;
             
             const formData = new FormData();
             formData.append('passing_percentage', pass);
             formData.append('promotion_min_papers', papers);
             
             try {
                const res = await fetch('exams_api.php?action=save_settings', { method:'POST', body:formData });
                const data = await res.json();
                if(data.status === 'success') alert('Settings Saved');
                else alert('Error: ' + (data.message || 'Unknown error'));
             } catch(e) {
                 console.error(e);
                 alert('Failed to save settings');
             }
        }

        function resetSettings() {
            if(!confirm('Reset settings to default values (40% pass, 0 min papers)?')) return;
            document.getElementById('set_pass_perc').value = 40;
            document.getElementById('set_min_papers').value = 0;
        }

        async function loadSettings() {
             try {
                 const res = await fetch('exams_api.php?action=get_settings');
                 // Only proceed if res is ok
                 if(res.ok) {
                     const data = await res.json();
                     if(data && data.status === 'success' && data.data) {
                         document.getElementById('set_pass_perc').value = data.data.passing_percentage;
                         document.getElementById('set_min_papers').value = data.data.promotion_min_papers;
                     }
                 }
             } catch(e) { console.log('Error loading settings', e); }
        }
        
        // Load settings on init
        loadSettings();
        
        // Load Exams for Result Filters
        async function loadResultExams(sid, targetId) {
            const prefix = targetId.split('_')[0];
            let cid = '';
            const cElem = document.getElementById(prefix + '_class');
            if(cElem) cid = cElem.value;
            
            // Fallback for students
            if(typeof USER_ROLE !== 'undefined' && USER_ROLE === 'student' && typeof STUDENT_INFO !== 'undefined' && STUDENT_INFO) {
                if(!cid) cid = STUDENT_INFO.class_id;
                if(!sid) sid = STUDENT_INFO.section_id;
            }

            if(!cid || !sid) return;
            
            const res = await fetch(`exams_api.php?action=get_exams&class_id=${cid}&section_id=${sid}`);
            const data = await res.json();
            let html = '<option value="">Select Exam</option>';
            if(Array.isArray(data)) {
                // Group by exam name/type to avoid duplicates if subject-specific exams are listed but we want "Term" exam?
                // The current system seems to schedule "Final Term - English", "Final Term - Math" separately.
                // A DMC usually aggregates ALL subjects for a "Term".
                // ERROR: The current system treats each Subject's Exam as a separate entity 'edu_exams'.
                // If the user wants a DMC, they expect a Result for "Final Term" containing Math, English, Urdu scores.
                // But our 'exams' table row is ONE subject's exam.
                // WE NEED TO AGGREGATE by 'exam_type' + 'academic_year' probably?
                // Or does the user select "Final Term" and we find all subject exams matching that?
                
                // For now, let's list all exams. If they select "Final Term - English", the DMC will only show English? 
                // That's an "Award List".
                // A DMC must show ALL subjects.
                
                // Refinement: The dropdown should probably show Distinct "Exam Types" (e.g., Mid Term, Final Term).
                // And when selected, we fetch marks for ALL subjects in that Exam Type.
                
                // Let's filter unique exam types here for the dropdown if possible.
                const types = [...new Set(data.map(item => item.exam_type))];
                types.forEach(t => html += `<option value="${t}">${t}</option>`);
            }
            document.getElementById(targetId).innerHTML = html;
        }

        async function generateDMC() {
            const type = document.getElementById('dmc_type') ? document.getElementById('dmc_type').value : 'single';
            const year = document.getElementById('dmc_year') ? document.getElementById('dmc_year').value : '';
            let examType = document.getElementById('dmc_exam') ? document.getElementById('dmc_exam').value : '';
            let cid = document.getElementById('dmc_class') ? document.getElementById('dmc_class').value : '';
            let sid = document.getElementById('dmc_section') ? document.getElementById('dmc_section').value : '';
            const roll = document.getElementById('dmc_roll') ? document.getElementById('dmc_roll').value : '';

            // Fallback for students if selects are empty/disabled
            if(USER_ROLE === 'student' && STUDENT_INFO) {
                if(!cid) cid = STUDENT_INFO.class_id;
                if(!sid) sid = STUDENT_INFO.section_id;
            }
            
            if(!cid || !sid) return alert('Your enrollment information could not be found. Please contact administrator.');
            if(!examType) {
                if(USER_ROLE === 'student') return alert('No exams have been scheduled for your class yet.');
                return alert('Please select an Exam to generate DMC.');
            }
            if(type === 'single' && !roll && USER_ROLE !== 'student') return alert('Enter Roll Number');

            const btn = document.querySelector('button[onclick="generateDMC()"]');
            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating...';
            
            try {
                const res = await fetch(`exams_api.php?action=get_consolidated_result&class_id=${cid}&section_id=${sid}&exam_type=${examType}&roll=${type === 'single' ? roll : ''}&year=${year}&_=${Date.now()}`);
                const text = await res.text();
                let data;
                try {
                    data = JSON.parse(text);
                } catch(e) {
                    console.error('Server response:', text);
                    throw new Error('Invalid server response. Please check console.');
                }
                
                if(data.status === 'error') throw new Error(data.message);
                if(!data.data || data.data.length === 0) throw new Error('No records found');
            
                // Continue with rendering... (rest of the function is inside try, but we need to close it and add finally)
            
            const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
            const currentDate = new Date();
            const dateStr = `${monthNames[currentDate.getMonth()]} ${currentDate.getDate()}, ${currentDate.getFullYear()}`;
            
            let html = '';
            const instName = <?php echo json_encode($inst['name'] ?? 'Institution'); ?>;
            const logoPath = <?php echo json_encode($inst['logo_path'] ?? ''); ?>;

            const dTemplate = document.getElementById('dmc_design_template').value;
            const dColor = document.getElementById('dmc_design_color').value;
            const dWatermark = document.getElementById('dmc_design_watermark').value;
            const dSigs = parseInt(document.getElementById('dmc_design_sigs').value);

            data.data.forEach(stu => {
                let rows = '';
                stu.subjects.forEach(sub => {
                    const pct = (sub.obtained / sub.total) * 100;
                    let grade = 'F';
                    if(pct >= 90) grade = 'A+';
                    else if(pct >= 80) grade = 'A';
                    else if(pct >= 70) grade = 'B';
                    else if(pct >= 60) grade = 'C';
                    else if(pct >= 50) grade = 'D';

                    rows += `
                        <tr data-exam-id="${sub.exam_id}">
                            <td>${sub.subject}</td>
                            <td style="text-align:center;">${sub.total}</td>
                            <td class="editable-marks" style="text-align:center; font-weight:700;">${sub.obtained}</td>
                            <td style="text-align:center;">${grade}</td>
                            <td style="text-align:center; font-weight:700; color:${pct >= 50 ? '#10b981' : '#ef4444'}">${Math.round(pct)}%</td>
                        </tr>
                    `;
                });

                let sigsHtml = '';
                if(dSigs >= 1) sigsHtml += '<div class="sig-box">Controller of Exams</div>';
                if(dSigs >= 2) sigsHtml += '<div class="sig-box">Academic Supervisor</div>';
                if(dSigs >= 3) sigsHtml += '<div class="sig-box">Institution Head</div>';

                const editTools = (USER_ROLE === 'admin' || USER_ROLE === 'super_admin' || USER_ROLE === 'superadmin' || USER_ROLE === 'developer') ? `
                    <div class="no-print dmc-edit-tools" style="display:flex; gap:2%; margin-bottom:10px; justify-content:center;">
                        <button class="btn btn-sm btn-secondary" onclick="printSingleDMC('${stu.student_id}')" style="background:#64748b !important; color:white; border:none; padding:10px; border-radius:12px; cursor:pointer; width:45%; flex:0 0 45%; text-transform:uppercase; font-weight:800; letter-spacing:1px; display:flex; justify-content:center; align-items:center; gap:10px;"><i class="fas fa-print"></i> Print</button>
                        <button class="btn btn-sm btn-info btn-edit-mode" onclick="toggleDmcEdit(this, ${stu.student_id})" style="width:45%; flex:0 0 45%; padding:10px; border-radius:12px; text-transform:uppercase; font-weight:800; letter-spacing:1px; display:flex; justify-content:center; align-items:center; gap:10px;"><i class="fas fa-edit"></i> Quick Edit Marks</button>
                        <button class="btn btn-sm btn-success btn-save-edit" onclick="saveDmcEdit(this, ${stu.student_id})" style="display:none; width:45%; padding:10px; border-radius:12px; font-weight:800; display:flex; justify-content:center; align-items:center; gap:10px;"><i class="fas fa-save"></i> Save Changes</button></div>
                ` : '';

                html += `
                <div class="dmc-sheet-container" id="dmc-stu-${stu.student_id}">
                    ${editTools}
                    <div class="dmc-sheet ${dTemplate} ${dColor}">
                        <div class="header-modern-print">
                            <div class="header-blue-zone">
                                <h1 style="color:#f7c600;">${instName}</h1>
                                <p>OFFICIAL ACADEMIC TRANSCRIPT</p>
                            </div>
                            <div class="header-white-zone">
                                <img src="../../${logoPath}" class="register-school-logo">
                                <div class="header-register-info">
                                    <strong>DETAILED MARKS CERTIFICATE</strong>
                                    <p>${examType.toUpperCase()} ASSESSMENT | ${year}</p>
                                    <p>${dateStr}</p>
                                </div>
                                <div class="header-blue-tip"></div>
                            </div>
                        </div>

                        <div class="dmc-body">
                            ${dWatermark === 'show' ? `<img src="../../${logoPath}" class="watermark">` : ''}
                            
                            <div class="student-info-grid">
                                <div class="info-box">
                                    <div class="info-item">
                                        <div class="info-label">Student Name</div>
                                        <div class="info-value editable-info" data-field="full_name">${stu.full_name}</div>
                                    </div>
                                    <div class="info-item">
                                        <div class="info-label">Father's Name</div>
                                        <div class="info-value editable-info" data-field="father_name">${stu.father_name || 'N/A'}</div>
                                    </div>
                                    <div class="info-item">
                                        <div class="info-label">Roll Number</div>
                                        <div class="info-value editable-info" data-field="roll_number">${stu.roll_number}</div>
                                    </div>
                                    <div class="info-item">
                                        <div class="info-label">Class Name</div>
                                        <div class="info-value">${stu.class_name || 'N/A'}</div>
                                    </div>
                                </div>
                                <div class="photo-box">
                                    <img src="${stu.profile_pic ? '../../assets/uploads/'+stu.profile_pic : 'https://via.placeholder.com/120x140?text=PHOTO'}" onerror="this.src='https://via.placeholder.com/120x140?text=PHOTO'">
                                    <span>OFFICIAL</span>
                                </div>
                            </div>

                            <table class="register-table">
                                <thead>
                                    <tr>
                                        <th>Subject Name</th>
                                        <th style="width:100px; text-align:center;">Max Marks</th>
                                        <th style="width:100px; text-align:center;">Obtained</th>
                                        <th style="width:80px; text-align:center;">Grade</th>
                                        <th style="width:100px; text-align:center;">Percentage</th>
                                    </tr>
                                </thead>
                                <tbody>${rows}</tbody>
                            </table>

                            <div class="summary-banner">
                                <div class="status-badge">
                                    <span class="label">Total Obtained</span>
                                    <span class="value">${stu.total_obtain} / ${stu.total_max}</span>
                                </div>
                                <div class="status-badge">
                                    <span class="label">Percentage</span>
                                    <span class="value">${stu.percentage}%</span>
                                </div>
                                <div class="status-badge">
                                    <span class="label">Result Status</span>
                                    <span class="value" style="color:${stu.final_status == 'PASS' ? '#10b981' : '#ef4444'}">${stu.final_status == 'PASS' ? 'PASSED' : 'FAILED'}</span>
                                </div>
                                <div class="status-badge">
                                    <span class="label">Verification</span>
                                    <span class="value" style="font-size:10px;">AUTHENTIC COPY</span>
                                </div>
                            </div>

                            <div style="margin-top:20px; font-size:11px; color:#64748b; line-height:1.6; border-top:1px solid #e2e8f0; padding-top:10px;">
                                <strong>Disclaimer:</strong> This is a computer-generated certificate. The authenticity can be verified by scanning the QR code or visiting the official portal. Any tampering with this document is a punishable offense. Passing percentage for each subject is <?php echo $stats_data['pass_rate'] ?? 40; ?>%.
                            </div>

                            <div class="signature-footer">
                                ${sigsHtml}
                                <div class="qr-footer-box">
                                    <img src="https://quickchart.io/qr?text=DMC-${stu.roll_number}-${Date.now()}&size=100">
                                    <span class="qr-footer-label">Verified Record</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                `;
            });
            
                document.getElementById('dmc_preview').innerHTML = html;
            } catch(e) {
                alert(e.message);
            } finally {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
        async function generateResultCard() {
            let cid = document.getElementById('rc_class') ? document.getElementById('rc_class').value : '';
            let sid = document.getElementById('rc_section') ? document.getElementById('rc_section').value : '';
            const roll = document.getElementById('rc_roll') ? document.getElementById('rc_roll').value : '';
            const year = document.getElementById('rc_year') ? document.getElementById('rc_year').value : '';
            
            // Fallback for students
            if(USER_ROLE === 'student' && STUDENT_INFO) {
                if(!cid) cid = STUDENT_INFO.class_id;
                if(!sid) sid = STUDENT_INFO.section_id;
            }

            if(!cid || !sid) return alert('Your enrollment information could not be found.');
            
            const btn = document.querySelector('button[onclick="generateResultCard()"]');
            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating...';

            try {
                const res = await fetch(`exams_api.php?action=get_full_result_card&class_id=${cid}&section_id=${sid}&roll=${roll}&year=${year}&_=${Date.now()}`);
                const text = await res.text();
                let result;
                try {
                    result = JSON.parse(text);
                } catch(e) {
                    console.error('Server response:', text);
                    throw new Error('Invalid server response');
                }
                
                if(result.status === 'error') throw new Error(result.message);
                
                // data is now always an array
                const students = Array.isArray(result.data) ? result.data : [result.data];
                let finalHtml = '';

                students.forEach(data => {
                    const logoPath = <?php echo json_encode($inst['logo_path'] ?? ''); ?>;
                    const instName = <?php echo json_encode($inst['name'] ?? ''); ?>;
                    const instAddress = <?php echo json_encode($inst['address'] ?? ''); ?>;
                    const instPhone = <?php echo json_encode($inst['phone'] ?? ''); ?>;
                    const rcClassElem = document.getElementById('rc_class'); const dmcClass = (rcClassElem && rcClassElem.selectedIndex >= 0) ? rcClassElem.options[rcClassElem.selectedIndex].text : '';

                    const rcTemplate = document.getElementById('rc_design_template').value;
                    const rcColor = document.getElementById('rc_design_color').value;
                    const rcWatermark = document.getElementById('rc_design_watermark').value;
                    const rcSigs = parseInt(document.getElementById('rc_design_sigs').value);

                    let sigsHtml = '';
                    if(rcSigs >= 1) sigsHtml += '<div class="sig-box">Controller of Exams</div>';
                    if(rcSigs >= 2) sigsHtml += '<div class="sig-box">Academic Supervisor</div>';
                    if(rcSigs >= 3) sigsHtml += '<div class="sig-box">Institution Head</div>';

                    let studentHtml = `
                    <div class="dmc-sheet-container" id="rc-stu-${data.student_id}">
                        ${(USER_ROLE === 'admin' || USER_ROLE === 'super_admin' || USER_ROLE === 'superadmin' || USER_ROLE === 'developer') ? `
                            <div class="no-print dmc-edit-tools" style="display:flex; gap:10px; margin-bottom:10px; justify-content:flex-end;">
                                <button class="btn btn-sm btn-info btn-edit-mode" onclick="toggleDmcEdit(this, ${data.student_id})"><i class="fas fa-edit"></i> Quick Edit Marks</button>
                                <button class="btn btn-sm btn-success btn-save-edit" onclick="saveDmcEdit(this, ${data.student_id})" style="display:none;"><i class="fas fa-save"></i> Save Changes</button>
                            </div>
                        ` : ''}
                        <div class="dmc-sheet ${rcTemplate} ${rcColor}">
                            <div class="header-modern-print">
                                <div class="header-blue-zone">
                                    <h1 style="color:#f7c600;">${instName}</h1>
                                    <p style="color:white; opacity:0.9;">STUDENT PERFORMANCE RECORD</p>
                                </div>
                                <div class="header-white-zone">
                                    <img src="../../${logoPath}" class="register-school-logo">
                                    <div class="header-register-info">
                                        <strong>ACADEMIC TRANSCRIPT</strong>
                                        <p>SESSION ${year} | FULL HISTORY</p>
                                        <p>${instAddress}</p>
                                    </div>
                                    <div class="header-blue-tip"></div>
                                </div>
                            </div>
                            
                            <div class="dmc-body">
                                ${rcWatermark === 'show' ? `<img src="../../${logoPath}" class="watermark">` : ''}
                                
                                <div class="student-info-grid">
                                    <div class="info-box">
                                        <div class="info-item">
                                            <div class="info-label">Student Name</div>
                                            <div class="info-value editable-info" data-field="full_name">${data.full_name}</div>
                                        </div>
                                        <div class="info-item">
                                            <div class="info-label">Father's Name</div>
                                            <div class="info-value editable-info" data-field="father_name">${data.father_name || 'N/A'}</div>
                                        </div>
                                        <div class="info-item">
                                            <div class="info-label">Roll Number</div>
                                            <div class="info-value editable-info" data-field="roll_number">${data.roll_number}</div>
                                        </div>
                                        <div class="info-item">
                                            <div class="info-label">Grade / Section</div>
                                            <div class="info-value">${dmcClass}</div>
                                        </div>
                                    </div>
                                    <div class="photo-box">
                                        <img src="${data.profile_pic ? '../../assets/uploads/'+data.profile_pic : 'https://via.placeholder.com/120x140?text=PHOTO'}" onerror="this.src='https://via.placeholder.com/120x140?text=PHOTO'">
                                        <span>OFFICIAL</span>
                                    </div>
                                </div>
                        `;
                        
                        data.report.forEach(exam => {
                            let rows = '';
                            exam.subjects.forEach(s => {
                                const pct = (s.obtained/s.total)*100;
                                rows += `
                                    <tr data-exam-id="${s.exam_id}">
                                        <td>${s.subject}</td>
                                        <td style="text-align:center;">${s.total}</td>
                                        <td class="editable-marks" style="text-align:center; font-weight:700;">${s.obtained}</td>
                                        <td style="text-align:center; font-weight:700; color:${pct >= 50 ? '#10b981' : '#ef4444'}">${Math.round(pct)}%</td>
                                    </tr>
                                `;
                            });
                            
                            studentHtml += `
                            <div style="margin-bottom:30px; position:relative; z-index:10;">
                                <div style="background: #2c52a0; color:white; padding:10px 20px; border-radius:10px 10px 0 0; font-weight:800; font-size:11px; display:flex; justify-content:space-between; letter-spacing:1px; text-transform:uppercase;">
                                    <span>${exam.exam_type} PERFORMANCE</span>
                                    <span>TERM AGGREGATE: ${exam.percentage}%</span>
                                </div>
                                <table class="register-table" style="margin-bottom:0; border-top:none; border-radius:0 0 10px 10px;">
                                    <thead>
                                        <tr style="background:#f1f5f9;">
                                            <th style="background:#f1f5f9; color:#2c52a0; border-bottom:1px solid #e2e8f0;">Subject Name</th>
                                            <th style="width:100px; text-align:center; background:#f1f5f9; color:#2c52a0; border-bottom:1px solid #e2e8f0;">Max</th>
                                            <th style="width:100px; text-align:center; background:#f1f5f9; color:#2c52a0; border-bottom:1px solid #e2e8f0;">Obtained</th>
                                            <th style="width:100px; text-align:center; background:#f1f5f9; color:#2c52a0; border-bottom:1px solid #e2e8f0;">Result</th>
                                        </tr>
                                    </thead>
                                    <tbody>${rows}</tbody>
                                </table>
                            </div>
                            `;
                        });
                        
                        studentHtml += `
                                <div class="signature-footer" style="padding-top:20px;">
                                    ${sigsHtml}
                                    <div class="qr-footer-box">
                                        <img src="https://quickchart.io/qr?text=TRANSCRIPT-${data.roll_number}-${Date.now()}&size=100">
                                        <span class="qr-footer-label">Secure Record</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;
                    finalHtml += studentHtml;
                });
                
                document.getElementById('rc_preview').innerHTML = finalHtml + '<div class="no-print" style="text-align:center; margin-top:20px;"><button class="btn btn-warning" onclick="window.print()" style="padding:10px 30px;"><i class="fas fa-print"></i> Print Performance Record</button></div>';
        } catch(e) {
            alert(e.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
        
        function printSingleDMC(stuId) {
            document.querySelectorAll('.dmc-sheet-container').forEach(c => c.classList.add('print-hidden'));
            const target = document.getElementById('dmc-stu-' + stuId);
            if(target) target.classList.remove('print-hidden');
            
            let style = document.createElement('style');
            style.innerHTML = '@media print { .dmc-sheet-container { page-break-after: auto !important; break-after: auto !important; margin: 0 !important; } html, body { height: auto !important; margin: 0 !important; padding: 0 !important; } }';
            document.head.appendChild(style);

            setTimeout(() => {
                window.print();
                document.querySelectorAll('.dmc-sheet-container').forEach(c => c.classList.remove('print-hidden'));
                style.remove();
            }, 100);
        }


        function toggleDmcEdit(btn, stuId) {
            const container = btn.closest('.dmc-sheet-container');
            const mCells = container.querySelectorAll('.editable-marks');
            const iCells = container.querySelectorAll('.editable-info');
            const isEditing = btn.classList.contains('active-edit');
            
            if(!isEditing) {
                btn.innerHTML = '<i class="fas fa-times"></i> Cancel Edit';
                btn.classList.add('active-edit', 'btn-danger');
                btn.classList.remove('btn-info');
                container.querySelector('.btn-save-edit').style.display = 'block';
                
                [...mCells, ...iCells].forEach(c => {
                    c.contentEditable = true;
                    c.style.background = '#fffde7';
                    c.style.outline = '2px solid #fbc02d';
                    c.style.borderRadius = '4px';
                    c.style.padding = '2px 5px';
                });
            } else {
                btn.innerHTML = '<i class="fas fa-edit"></i> Quick Edit Details';
                btn.classList.remove('active-edit', 'btn-danger');
                btn.classList.add('btn-info');
                container.querySelector('.btn-save-edit').style.display = 'none';
                
                [...mCells, ...iCells].forEach(c => {
                    c.contentEditable = false;
                    c.style.background = 'transparent';
                    c.style.outline = 'none';
                    c.style.padding = '0';
                });
            }
        }

        async function saveDmcEdit(btn, stuId) {
            const container = btn.closest('.dmc-sheet-container');
            const rows = container.querySelectorAll('tbody tr[data-exam-id]');
            const infoFields = container.querySelectorAll('.editable-info');
            
            const marksData = [];
            rows.forEach(r => {
                marksData.push({
                    exam_id: r.dataset.examId,
                    marks: r.querySelector('.editable-marks').innerText.trim()
                });
            });

            const infoData = { student_id: stuId };
            infoFields.forEach(f => {
                infoData[f.dataset.field] = f.innerText.trim();
            });

            const originalText = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';

            try {
                // 1. Save Marks
                const markRes = await fetch('exams_api.php?action=save_student_consolidated_marks', {
                    method: 'POST',
                    body: new URLSearchParams({ student_id: stuId, marks_data: JSON.stringify(marksData) })
                });
                const mResult = await markRes.json();
                if(mResult.status !== 'success') throw new Error('Marks update failed: ' + mResult.message);

                // 2. Save Student Info
                const infoRes = await fetch('exams_api.php?action=save_student_quick_info', {
                    method: 'POST',
                    body: new URLSearchParams(infoData)
                });
                const iResult = await infoRes.json();
                if(iResult.status !== 'success') throw new Error('Info update failed: ' + iResult.message);

                alert('Record updated successfully. Re-generating preview...');
                const editBtn = container.querySelector('.btn-edit-mode');
                toggleDmcEdit(editBtn, stuId);
                
                if(container.id.startsWith('rc-stu-')) generateResultCard();
                else generateDMC();

            } catch(e) {
                alert('Error: ' + e.message);
            } finally {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
        let currentPerfMode = 'school_toppers';
        async function loadPerformance(mode) {
            currentPerfMode = mode || currentPerfMode;
            const filters = document.getElementById('perf_filters');
            const target = document.getElementById('perf_preview');
            
            // Show/Hide class filters based on mode
            if(currentPerfMode === 'class_toppers' || currentPerfMode === 'trends') {
                filters.style.display = 'grid';
            } else {
                filters.style.display = 'none';
                runPerformanceLoad(); // Auto run for global reports
            }
            
            target.innerHTML = `<div style="padding:40px; text-align:center;"><i class="fas fa-spinner fa-spin fa-3x"></i><p>Loading ${currentPerfMode.replace('_',' ')}...</p></div>`;
        }

        async function runPerformanceLoad() {
            const cid = document.getElementById('perf_class').value;
            const sid = document.getElementById('perf_section').value;
            const target = document.getElementById('perf_preview');
            
            // Validate only if filters are visible/required
            if(document.getElementById('perf_filters').style.display !== 'none') {
                if(!cid || !sid) return alert('Please select Class and Section');
            }

            // Button Loading State
            const btn = document.querySelector('#perf_filters button');
            let originalText = '';
            if(btn) {
                originalText = btn.innerHTML;
                btn.disabled = true;
                btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
            }
            
            // Allow target to show loading if not using button (e.g. auto-load)
            if(!btn || document.getElementById('perf_filters').style.display === 'none') {
                 target.innerHTML = `<div style="padding:40px; text-align:center;"><i class="fas fa-spinner fa-spin fa-3x"></i><p>Loading ${currentPerfMode.replace('_',' ')}...</p></div>`;
            }

            try {
                const res = await fetch(`exams_api.php?action=get_performance_analytics&type=${currentPerfMode}&class_id=${cid}&section_id=${sid}`);
                // Check content type or try/catch json
                const text = await res.text();
                let result;
                try {
                    result = JSON.parse(text);
                } catch(e) {
                    throw new Error('Invalid server response');
                }
                
                if(result.status === 'error') throw new Error(result.message);
                const data = result.data;
                
                let html = '';
                
                if(currentPerfMode === 'school_toppers' || currentPerfMode === 'class_toppers') {
                    let rows = '';
                    data.forEach((st, i) => {
                        rows += `
                            <tr style="border-bottom:1px solid rgba(0,0,0,0.08);">
                                <td style="padding:15px; color:#f1c40f; font-weight:900; font-size:18px;">#${i+1}</td>
                                <td style="padding:15px;">
                                    <div style="font-weight:800; color:#1e293b; font-size:15px;">${st.full_name}</div>
                                    <div style="font-size:11px; color:#64748b; font-weight:600;">Roll: ${st.roll_number} | CNO: ${st.class_no || '-'}</div>
                                </td>
                                ${currentPerfMode === 'school_toppers' ? `<td style="padding:15px; color:#334155; font-weight:700;">${st.class_name}</td>` : ''}
                                <td style="padding:15px; text-align:right;">
                                    <div style="font-weight:900; color:#10b981; font-size:18px;">${st.percentage}%</div>
                                    <div style="font-size:9px; color:#94a3b8; font-weight:800; text-transform:uppercase;">Aggregate</div>
                                </td>
                            </tr>
                        `;
                    });
                    
                    html = `
                        <div class="dmc-sheet" style="min-height:auto; box-shadow:0 20px 50px rgba(0,0,0,0.1); border-radius:12px; margin-bottom:20px;">
                            <div style="background:#2c52a0; padding:20px; color:white; display:flex; align-items:center; justify-content:space-between; border-radius:12px 12px 0 0;">
                                <h3 style="margin:0; font-size:16px; font-weight:900; text-transform:uppercase; letter-spacing:1px; color:#f7c600;">
                                    <i class="fas fa-crown"></i> ${currentPerfMode === 'school_toppers' ? 'Institutional Toppers' : 'Class Ranking List'}
                                </h3>
                                <span style="font-size:10px; font-weight:800; opacity:0.8; text-transform:uppercase;">Top Performers</span>
                            </div>
                            <div style="padding:0;">
                                <table class="register-table" style="margin:0; border:none;">
                                    <thead style="display:none;"><tr><th>Rank</th><th>Student</th>${currentPerfMode === 'school_toppers' ? '<th>Class</th>' : ''}<th>Pct</th></tr></thead>
                                    <tbody>${rows || '<tr><td colspan="4" style="text-align:center; padding:40px; color:#64748b;">No records found for this period.</td></tr>'}</tbody>
                                </table>
                            </div>
                        </div>
                    `;
                } else if(currentPerfMode === 'school_performance') {
                    html = `
                        <div class="row" style="margin:0 -10px;">
                            <div class="col-md-3" style="padding:10px;">
                                <div class="status-badge" style="background:#2c52a0; color:white; border:none; padding:25px;">
                                    <span class="label" style="color:rgba(255,255,255,0.7);">TOTAL STUDENTS</span>
                                    <span class="value" style="color:white; font-size:32px;">${data.total_students || 0}</span>
                                </div>
                            </div>
                            <div class="col-md-3" style="padding:10px;">
                                <div class="status-badge" style="background:#10b981; color:white; border:none; padding:25px;">
                                    <span class="label" style="color:rgba(255,255,255,0.7);">PASSED ENTRIES</span>
                                    <span class="value" style="color:white; font-size:32px;">${data.passed_entries || 0}</span>
                                </div>
                            </div>
                            <div class="col-md-3" style="padding:10px;">
                                <div class="status-badge" style="background:#ef4444; color:white; border:none; padding:25px;">
                                    <span class="label" style="color:rgba(255,255,255,0.7);">FAILED ENTRIES</span>
                                    <span class="value" style="color:white; font-size:32px;">${data.failed_entries || 0}</span>
                                </div>
                            </div>
                            <div class="col-md-3" style="padding:10px;">
                                <div class="status-badge" style="background:#f7c600; color:#0f172a; border:none; padding:25px;">
                                    <span class="label" style="color:rgba(0,0,0,0.6);">INSTITUTIONAL EFFICIENCY</span>
                                    <span class="value" style="color:#0f172a; font-size:32px;">${data.pass_rate || 0}%</span>
                                </div>
                            </div>
                        </div>
                    `;
                } else if(currentPerfMode === 'staff_performance') {
                     let cards = '';
                     data.forEach(item => {
                         cards += `
                            <div class="col-md-4" style="margin-bottom:20px;">
                                <div class="dmc-sheet" style="min-height:auto; padding:20px; box-shadow:0 10px 30px rgba(0,0,0,0.05);">
                                    <h4 style="margin:0 0 15px 0; color:#2c52a0; font-weight:900; font-size:16px; border-bottom:1.5px solid #e2e8f0; padding-bottom:10px;">${item.class_name}</h4>
                                    <div style="display:flex; justify-content:space-between; margin-bottom:8px; font-size:13px;"><span style="color:#64748b; font-weight:600;">Total Students:</span> <b style="color:#1e293b;">${item.total_students}</b></div>
                                    <div style="display:flex; justify-content:space-between; margin-bottom:8px; font-size:13px;"><span style="color:#64748b; font-weight:600;">Passed Entries:</span> <b style="color:#10b981;">${item.passed_entries}</b></div>
                                    <div style="display:flex; justify-content:space-between; margin-bottom:8px; font-size:13px;"><span style="color:#64748b; font-weight:600;">Failed Entries:</span> <b style="color:#ef4444;">${item.failed_entries}</b></div>
                                    
                                    <div style="margin-top:15px;">
                                        <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:5px;">
                                            <span style="font-weight:800; text-transform:uppercase; letter-spacing:0.5px; opacity:0.6;">Efficiency Rate</span>
                                            <span style="font-weight:900; color:#2c52a0;">${item.pass_rate}%</span>
                                        </div>
                                        <div style="height:8px; background:#f1f5f9; border-radius:4px; overflow:hidden;">
                                            <div style="width:${item.pass_rate}%; background:linear-gradient(90deg, #3b82f6, #2563eb); height:100%; border-radius:4px;"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                         `;
                     });
                     html = `<div class="row">${cards || '<div class="col-12 text-center" style="padding:40px; color:#64748b;">No performance data linked to your assignments.</div>'}</div>`;
                } else if(currentPerfMode === 'trends') {
                     html = '<div class="row">';
                     data.forEach((exam, idx) => {
                         html += `
                            <div class="col-md-6" style="margin-bottom:25px;">
                                <div class="dmc-sheet" style="min-height:auto; padding:25px; box-shadow:0 15px 40px rgba(0,0,0,0.1);">
                                    <h4 style="color:#2c52a0; font-size:14px; text-align:center; font-weight:900; text-transform:uppercase; letter-spacing:1px; margin-bottom:20px; padding-bottom:10px; border-bottom:1.5px solid #f1f5f9;">
                                        <i class="fas fa-chart-line"></i> ${exam.exam_type} Analytics
                                    </h4>
                                    <div style="height:300px;"><canvas id="chart-${idx}"></canvas></div>
                                </div>
                            </div>
                         `;
                     });
                     html += '</div>';
                     target.innerHTML = html;
 
                     data.forEach((exam, idx) => {
                        const ctx = document.getElementById(`chart-${idx}`).getContext('2d');
                        new Chart(ctx, {
                            type: 'bar',
                            data: {
                                labels: exam.subject_performance.map(s => s.subject),
                                datasets: [{
                                    label: 'Average Score (%)',
                                    data: exam.subject_performance.map(s => Math.round((s.average/s.total)*100)),
                                    backgroundColor: 'rgba(44, 82, 160, 0.8)',
                                    borderColor: '#2c52a0',
                                    borderWidth: 2,
                                    borderRadius: 6
                                }]
                            },
                            options: {
                                responsive: true, maintainAspectRatio: false,
                                plugins: { 
                                    legend: { display: false },
                                    tooltip: { backgroundColor: '#1e293b', titleFont: { size: 12 }, bodyFont: { size: 12 } }
                                },
                                scales: {
                                    y: { 
                                        beginAtZero: true, max: 100, 
                                        grid: { color: 'rgba(0,0,0,0.05)' },
                                        ticks: { color: '#64748b', font: { weight: '600' } } 
                                    },
                                    x: { 
                                        grid: { display: false },
                                        ticks: { color: '#64748b', font: { weight: '600' } } 
                                    }
                                }
                            }
                        });
                     });
                     return; 
                }
                
                target.innerHTML = html || '<div class="alert alert-info">No data available for this selection.</div>';
                
            } catch(e) {
                console.error(e);
                target.innerHTML = `<div class="alert alert-danger">Error: ${e.message}</div>`;
            } finally {
                if(btn) {
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                }
            }
        }


        // Create Tab Switcher
        function switchCreateTab(tabId, btn) {
            document.querySelectorAll('.create-sub-tab').forEach(el => el.style.display = 'none');
            document.getElementById(tabId).style.display = 'block';
            document.querySelectorAll('#create-tab .tab-btn, #create-tab .simple-sub-tab').forEach(el => el.classList.remove('active'));
            btn.classList.add('active');
            
            if(tabId === 'view_exam_list') {
                loadHierarchyL1();
            }
        }

        // --- Level 1: Exam Type & Year ---
        async function loadHierarchyL1() {
            const tbody = document.getElementById('exam_l1_body');
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Loading...</td></tr>';
            
            try {
                const res = await fetch(`exams_api.php?action=get_exam_hierarchy_l1&_=${new Date().getTime()}`);
                const json = await res.json();
                
                if(json.status === 'success') {
                    if(json.data.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No exams scheduled.</td></tr>';
                        return;
                    }
                    
                    tbody.innerHTML = json.data.map(r => `
                        <tr style="cursor:pointer;" onclick="openLevel2('${r.exam_type}', '${r.semester}', '${r.academic_year}')">
                            <td class="c-name" style="color:#f1c40f;">${r.exam_type} ${r.semester ? `(${r.semester})` : ''}</td>
                            <td>${r.academic_year}</td>
                            <td><span class="badge" style="background:#3498db;">${r.exam_count}</span></td>
                            <td style="text-align:center;" onclick="event.stopPropagation()">
                                <button class="btn btn-sm btn-danger" onclick="deleteLevel1('${r.exam_type}', '${r.semester}', '${r.academic_year}')" style="padding:2px 8px;"><i class="fas fa-trash"></i></button>
                            </td>
                        </tr>
                    `).join('');
                } else {
                    tbody.innerHTML = `<tr><td colspan="4" style="color:red; text-align:center;">Error: ${json.message}</td></tr>`;
                }
            } catch(e) { tbody.innerHTML = `<tr><td colspan="4" style="color:red; text-align:center;">Failed to load.</td></tr>`; }
        }

        async function deleteLevel1(type, semester, year) {
            if(!confirm(`WARNING: You are about to delete ALL exams for "${type} ${semester ? `(${semester})` : ''} (${year})".\n\nThis includes:\n- Definition of all exams.\n- All Marks for every student.\n\nThis cannot be undone. Proceed?`)) return;
            
            const formData = new FormData();
            formData.append('type', type);
            formData.append('semester', semester);
            formData.append('year', year);
            
            try {
                const res = await fetch('exams_api.php?action=delete_exam_group_l1', { method: 'POST', body: formData });
                const data = await res.json();
                if(data.status === 'success') {
                    loadHierarchyL1();
                } else {
                    alert('Error: ' + data.message);
                }
            } catch(e) { alert('Failed to delete'); }
        }

        // --- Level 2: Classes ---
        async function openLevel2(type, semester, year) {
            document.getElementById('modal_l2').style.display = 'flex';
            document.getElementById('modal_l2_title').innerText = `${type} ${semester ? `(${semester})` : ''} (${year}) - Classes`;
            const tbody = document.getElementById('modal_l2_body');
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Loading...</td></tr>';
            
            try {
                const res = await fetch(`exams_api.php?action=get_exam_hierarchy_l2&type=${type}&semester=${semester}&year=${year}&_=${new Date().getTime()}`);
                const json = await res.json();
                
                if(json.status === 'success') {
                    tbody.innerHTML = json.data.map(r => `
                        <tr style="cursor:pointer;" onclick="openLevel3('${type}', '${semester}', '${year}', ${r.class_id}, ${r.section_id}, '${r.class_name} ${r.section_name}')">
                            <td class="c-name" style="color:#2ecc71;">${r.class_name}</td>
                            <td>${r.section_name}</td>
                            <td>${r.subject_count}</td>
                            <td style="text-align:center;" onclick="event.stopPropagation()">
                                <button class="btn btn-sm btn-danger" onclick="deleteLevel2('${type}', '${semester}', '${year}', ${r.class_id}, ${r.section_id})" style="padding:2px 8px;"><i class="fas fa-trash"></i></button>
                            </td>
                        </tr>
                    `).join('');
                }
            } catch(e) { tbody.innerHTML = '<tr><td colspan="4">Error loading classes.</td></tr>'; }
        }

        async function deleteLevel2(type, semester, year, cid, sid) {
            if(!confirm(`Delete all exams for this Class/Section in ${type} ${semester ? `(${semester})` : ''}?`)) return;
             const formData = new FormData();
            formData.append('type', type);
            formData.append('semester', semester);
            formData.append('year', year);
            formData.append('class_id', cid);
            formData.append('section_id', sid);
            
            try {
                const res = await fetch('exams_api.php?action=delete_exam_group_l2', { method: 'POST', body: formData });
                if((await res.json()).status === 'success') {
                    openLevel2(type, semester, year); // Refresh Level 2
                } else { alert('Error deleting.'); }
            } catch(e) { alert('Failed to delete'); }
        }

        // --- Level 3: Subjects List ---
        async function openLevel3(type, semester, year, cid, sid, className) {
            document.getElementById('modal_l3').style.display = 'flex';
            document.getElementById('modal_l3_title').innerText = `${className} - Subjects`;
            const tbody = document.getElementById('modal_l3_body');
            tbody.innerHTML = '<tr><td colspan="2" style="text-align:center;">Loading...</td></tr>';
            
            try {
                const res = await fetch(`exams_api.php?action=get_exam_hierarchy_l3&type=${type}&semester=${semester}&year=${year}&class_id=${cid}&section_id=${sid}&_=${new Date().getTime()}`);
                const json = await res.json();
                
                if(json.status === 'success') {
                    tbody.innerHTML = json.data.map(e => `
                        <tr style="cursor:pointer;" onclick='openLevel4(${JSON.stringify(e)}, "${className}")'>
                            <td style="color:#f39c12; font-weight:bold;">${e.subject_name}</td>
                            <td style="text-align:center;">
                                <button class="btn btn-sm btn-danger" onclick="event.stopPropagation(); deleteExam(${e.id}, '${type}', '${semester}', '${year}', ${cid}, ${sid}, '${className}')" style="padding:2px 8px;"><i class="fas fa-trash"></i></button>
                            </td>
                        </tr>
                    `).join('');
                } else {
                    tbody.innerHTML = '<tr><td colspan="2">No exams found.</td></tr>';
                }
            } catch(e) { tbody.innerHTML = '<tr><td colspan="2">Error loading exams.</td></tr>'; }
        }

        // --- Level 4: Exam Details ---
        function openLevel4(e, className) {
            document.getElementById('modal_l4').style.display = 'flex';
            
            // Populate Details
            document.getElementById('l4_subject').innerText = e.subject_name;
            document.getElementById('l4_subtitle').innerText = className;
            
            document.getElementById('l4_date').innerText = e.exam_date;
            document.getElementById('l4_time').innerText = e.start_time;
            document.getElementById('l4_shift').innerText = e.shift;
            document.getElementById('l4_marks').innerText = e.total_marks;
            document.getElementById('l4_type').innerText = e.exam_type;
            document.getElementById('l4_semester').innerText = e.semester || 'N/A';
            document.getElementById('l4_year').innerText = e.academic_year;
            
            // Bind Buttons
            document.getElementById('l4_edit_btn').onclick = function() {
                closeModal('modal_l4');
                editExam(e);
            };
            
            document.getElementById('l4_delete_btn').onclick = function() {
                deleteExam(e.id, e.exam_type, e.semester, e.academic_year, e.class_id, e.section_id, className);
                closeModal('modal_l4'); // Close details modal after delete
            };
        }
        
        // Helper to close modals
        function closeModal(id) {
            document.getElementById(id).style.display = 'none';
        }

        // Modified Standard Delete/Edit
        async function deleteExam(id, type, semester, year, cid, sid, cname) {
            if(!confirm('Delete this single exam? Marks will be deleted.')) return;
            const formData = new FormData();
            formData.append('exam_id', id);
            try {
                const res = await fetch('exams_api.php?action=delete_exam', { method: 'POST', body: formData });
                if((await res.json()).status === 'success') {
                     // Check if L4 is open (meaning we deleted from details view)
                     // If so, we might want to close L4 (handled in onclick)
                     // Always refresh L3 list
                     openLevel3(type, semester, year, cid, sid, cname); 
                } else { alert('Error deleting.'); }
            } catch(e) { alert('Failed.'); }
        }

        function editExam(e) {
            closeModal('modal_l3');
            closeModal('modal_l2');
            closeModal('modal_l4');
            document.querySelectorAll('#create-tab .tab-btn')[0].click(); // Switch to Form
            
            document.getElementById('e_type').value = e.exam_type;
            document.getElementById('e_semester').value = e.semester || '';
            document.getElementById('e_year').value = e.academic_year;
            document.getElementById('e_date').value = e.exam_date;
            document.getElementById('e_time').value = e.start_time;
            document.getElementById('e_shift').value = e.shift;
            document.getElementById('e_total').value = e.total_marks;
            
            alert('Details loaded. Re-select Class/Section/Subject to create a replacement.');
        }
    </script>

    <!-- Access Control System -->
    <script>
        const isAdmin = <?php echo json_encode(in_array($_SESSION['edu_role'], ['admin', 'developer', 'super_admin', 'superadmin', 'guest'])); ?>;
        const isGuest = <?php echo json_encode($_SESSION['edu_role'] === 'guest'); ?>;
        const isSuper = <?php echo json_encode(in_array($_SESSION['edu_role'], ['developer', 'super_admin', 'superadmin'])); ?>;
    </script>
    <script src="../../assets/js/access_granular.js?v=<?php echo time(); ?>"></script>
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            if (typeof AccessControl !== 'undefined') {
                AccessControl.init(isAdmin, isGuest, isSuper);
            }
        });
    </script>
    <script src="../../assets/js/theme.js"></script>

    <script>
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('../../sw.js').then(registration => {
                    console.log('SW registered');
                }).catch(err => {
                    console.log('SW registration failed');
                });
            });
        }
    </script>
</body>
</html>





