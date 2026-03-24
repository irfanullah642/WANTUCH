<?php
if (session_status() == PHP_SESSION_NONE) {
    session_start();
}
require_once '../../includes/db.php';
require_once '../../includes/access_helper.php';

if (!isset($_SESSION['edu_user_id']) || !has_granular_access('dash_attendance', true)) {
    header("Location: dashboard.php");
    exit;
}

$inst_id = $_SESSION['edu_institution_id'];

// Fetch all classes and sections
$is_staff = ($_SESSION['edu_role'] === 'staff');
$assigned_cid = 0;
$assigned_sid = 0;

if ($is_staff) {
    $staff_id = $_SESSION['edu_user_id'];
    $u_res = $conn->query("SELECT assigned_class_id, assigned_section_id FROM edu_users WHERE id = $staff_id");
    if ($u_res && $u_res->num_rows > 0) {
        $u = $u_res->fetch_assoc();
        $assigned_cid = (int)$u['assigned_class_id'];
        $assigned_sid = (int)$u['assigned_section_id'];
    }
}

$inst_q = $conn->query("SELECT * FROM edu_institutions WHERE id = $inst_id");
$inst = $inst_q->fetch_assoc();

$class_filter = ($is_staff && $assigned_cid > 0) ? " AND id = $assigned_cid" : ($is_staff ? " AND id = 0" : "");
$classes_q = $conn->query("SELECT * FROM edu_classes WHERE institution_id = $inst_id $class_filter ORDER BY name");

$all_classes = [];
while ($c = $classes_q->fetch_assoc()) {
    $section_filter = ($is_staff && $assigned_sid > 0) ? " AND id = $assigned_sid" : "";
    $sections = [];
    $sq = $conn->query("SELECT * FROM edu_sections WHERE class_id = {$c['id']} $section_filter");
    while ($s = $sq->fetch_assoc())
        $sections[] = $s;
    $c['sections'] = $sections;
    $all_classes[] = $c;
}

$pending_leaves_count = $conn->query("SELECT COUNT(l.id) FROM edu_leaves l JOIN edu_users u ON l.user_id = u.id WHERE l.status IN ('pending', 'forwarded_to_parent') AND u.institution_id = $inst_id")->fetch_row()[0];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#020617">
    <title>Attendance Intelligence</title>
    <link rel="stylesheet" href="../../assets/css/style.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&family=Inter:wght@400;700;900&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="../../assets/vendor/fontawesome/all.min.css" crossorigin="anonymous">
    <script src="../../assets/vendor/tailwind/tailwind.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    <script src="../../assets/vendor/html2pdf.bundle.min.js"></script>
    <style>
        :root {
            --primary:    #6366f1; /* Indigo */
            --primary-dark: #4f46e5;
            --secondary:  #8b5cf6; /* Violet */
            --success:    #10b981;
            --warning:    #f59e0b;
            --danger:     #ef4444;
            --bg:         #020617;
            --card:       rgba(15, 23, 42, 0.6);
            --border:     rgba(255, 255, 255, 0.08);
            --text-main:  #f8fafc;
            --text-dim:   #94a3b8;
            --glass:      rgba(255, 255, 255, 0.03);
            --glass-border: rgba(255, 255, 255, 0.1);
            --neon-blue:  #38bdf8;
            --neon-purple: #a855f7;
            --smart-font: 'Plus Jakarta Sans', sans-serif;
        }

        .swal2-container {
            z-index: 2000000 !important;
        }

        @keyframes scanAnim { 0% { top: 0; } 100% { top: 100%; } }
        @keyframes pulse-glow { 0% { box-shadow: 0 0 5px var(--primary); } 50% { box-shadow: 0 0 20px var(--primary); } 100% { box-shadow: 0 0 5px var(--primary); } }
        @keyframes slideInRight { from { opacity: 0; transform: translateX(30px); } to { opacity: 1; transform: translateX(0); } }
        @keyframes fadeInScale { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }

        body { 
            background: var(--bg) url('../../assets/images/textures/carbon-fibre.png');
            color: var(--text-main);
            font-family: var(--smart-font);
            margin: 0;
            transition: all 0.4s ease;
            overflow-x: hidden;
        }

        /* PREMIUM GLASS CARDS */
        .premium-card {
            background: var(--card);
            backdrop-filter: blur(12px);
            border: 1px solid var(--border);
            border-radius: 24px;
            box-shadow: 0 20px 50px rgba(0,0,0,0.3);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .premium-card:hover {
            border-color: rgba(255, 255, 255, 0.2);
            transform: translateY(-2px);
        }

        /* UNIFIED INPUT SYSTEM */
        select, input[type="date"], input[type="text"], .policy-input {
            background: rgba(15, 23, 42, 0.8) !important;
            border: 1px solid var(--border) !important;
            color: var(--text-main) !important;
            border-radius: 14px !important;
            padding: 0 16px !important;
            height: 52px !important;
            font-size: 0.95rem !important;
            font-weight: 600 !important;
            transition: all 0.3s ease !important;
            outline: none !important;
        }

        select:focus, input:focus {
            border-color: var(--primary) !important;
            box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2) !important;
        }

        /* PREMIUM MODAL SYSTEM */
        .premium-modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.85);
            backdrop-filter: blur(8px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 999999;
            animation: fadeIn 0.3s ease;
        }
        .premium-modal {
            background: #0f172a;
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 24px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            width: 95%;
            overflow: hidden;
            animation: fadeInScale 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            color: #ffffff;
        }
        .modal-header {
            padding: 20px 25px;
            border-bottom: 1px solid rgba(255,255,255,0.05);
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: rgba(255,255,255,0.02);
        }
        .modal-body {
            padding: 25px;
        }
        .modal-footer {
            padding: 20px 25px;
            border-top: 1px solid rgba(255,255,255,0.05);
            background: rgba(255,255,255,0.02);
        }

        /* FACE DETECTION SIDEBAR LIST */
        .attendance-toast {
            position: fixed;
            bottom: 30px;
            right: 30px;
            background: rgba(15, 23, 42, 0.95);
            backdrop-filter: blur(15px);
            border: 1px solid rgba(255,255,255,0.15);
            border-radius: 16px;
            padding: 16px;
            display: flex;
            align-items: center;
            gap: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.5);
            z-index: 20000;
            min-width: 300px;
            transform: translateX(120%);
            transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            animation: slideInRight 0.5s forwards;
        }
        .attendance-toast.hide {
            transform: translateX(120%);
        }
        .toast-img { width: 50px; height: 50px; border-radius: 12px; object-fit: cover; border: 2px solid var(--primary); }
        .toast-msg { flex: 1; }
        .toast-name { font-weight: 900; font-size: 15px; color: #fff; margin-bottom: 2px; }
        .toast-status { font-size: 12px; font-weight: 600; padding: 2px 8px; border-radius: 6px; display: inline-block; }

        .detection-item {
            background: linear-gradient(90deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.01) 100%);
            border: 1px solid rgba(255,255,255,0.05);
            border-radius: 16px;
            padding: 12px;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 14px;
            transition: all 0.3s ease;
            animation: slideInLeft 0.5s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
        }

        .detection-item::before {
            content: '';
            position: absolute;
            left: 0; top: 0; bottom: 0; width: 3px;
            background: var(--primary);
            opacity: 0.5;
        }

        .detection-item:hover {
            background: rgba(255,255,255,0.06);
            border-color: rgba(255,255,255,0.1);
            transform: translateX(5px);
        }

        .detection-avatar {
            width: 48px; height: 48px; border-radius: 12px;
            background: #1e293b;
            display: flex; align-items: center; justify-content: center;
            border: 1px solid rgba(255,255,255,0.1);
            font-size: 20px;
        }

        .detection-info { flex: 1; }
        .detection-name { font-size: 14px; font-weight: 700; color: #fff; letter-spacing: 0.3px; }
        .detection-time { font-size: 10px; color: var(--text-dim); margin-top: 2px; }

        .detection-status {
            padding: 4px 10px;
            border-radius: 10px;
            font-size: 9px;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        /* SCANNER UI CORE */
        .scan-focus-box {
            position: absolute;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            width: 260px; height: 260px;
            border-radius: 40px;
            pointer-events: none;
            overflow: hidden;
        }

        .scan-corner {
            position: absolute; width: 40px; height: 40px;
            border: 4px solid var(--neon-blue);
            filter: drop-shadow(0 0 8px var(--neon-blue));
        }
        .corner-tl { top: -2px; left: -2px; border-right: none; border-bottom: none; border-top-left-radius: 30px; }
        .corner-tr { top: -2px; right: -2px; border-left: none; border-bottom: none; border-top-right-radius: 30px; }
        .corner-bl { bottom: -2px; left: -2px; border-right: none; border-top: none; border-bottom-left-radius: 30px; }
        .corner-br { bottom: -2px; right: -2px; border-left: none; border-top: none; border-bottom-right-radius: 30px; }

        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

        /* PREMIUM TOGGLE SWITCHES */
        .switch-premium {
            position: relative;
            display: inline-block;
            width: 46px;
            height: 24px;
        }

        .switch-premium input { opacity: 0; width: 0; height: 0; }

        .slider-premium {
            position: absolute;
            cursor: pointer;
            top: 0; left: 0; right: 0; bottom: 0;
            background-color: rgba(255,255,255,0.05);
            transition: .4s;
            border-radius: 34px;
            border: 1px solid var(--border);
        }

        .slider-premium:before {
            position: absolute;
            content: "";
            height: 16px; width: 16px;
            left: 3px; bottom: 3px;
            background-color: #fff;
            transition: .4s;
            border-radius: 50%;
            box-shadow: 0 2px 5px rgba(0,0,0,0.3);
        }

        input:checked + .slider-premium {
            background-color: var(--primary);
            border-color: var(--primary);
        }

        input:checked + .slider-premium:before {
            transform: translateX(22px);
        }


        /* INPUT STYLING - UNIFIED & JUSTIFIED */
        select, input[type="date"], input[type="number"], input[type="text"], .policy-input {
            border-radius: 12px !important;
            padding: 0 15px !important;
            height: 50px !important;
            font-size: 1rem !important;
            width: 100%;
            box-sizing: border-box;
            box-shadow: inset 0 2px 4px rgba(0,0,0,0.05) !important;
            transition: all 0.3s ease;
        }
        ::placeholder { color: #ffffff !important; opacity: 1 !important; font-weight: 600; }
        select option { background: inherit; color: inherit; }
        select:focus, input:focus, .policy-input:focus {
            outline: none;
            transition: border-color 0.3s ease;
        }

        select:focus, input:focus {
            border-color: var(--primary) !important;
        }

        .tab-btn {
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            color: #000;
            padding: 12px 20px !important;
            font-size: 0.85rem !important;
            border-radius: 50px; /* Pill shape for main nav */
            flex: 1;
            justify-content: center;
            min-width: 130px;
            font-weight: 800 !important;
            letter-spacing: 0.5px;
            box-shadow: 0 4px 10px rgba(0,0,0,0.1);
            color: var(--text-main, #fff) !important;
        }

        .tab-btn.active {
            background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
            border-color: rgba(255,255,255,0.2);
            color: white !important;
            box-shadow: 0 8px 25px rgba(155, 89, 182, 0.4);
        }

        .premium-header {
            background: linear-gradient(135deg, #9b59b6, #8e44ad) !important;
            padding: 15px 20px;
            border-bottom: 1px solid var(--crystal-border);
            backdrop-filter: blur(10px);
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
        }

        .header-title {
            font-size: 1.25rem;
            font-weight: 800;
            display: flex;
            align-items: center;
            gap: 12px;
            color: var(--text-main);
        }
        
        .header-title i {
            font-size: 1.5rem;
        }

        .back-btn {
            width: 40px;
            height: 40px;
            border-radius: 12px;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--crystal-border);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            color: var(--text-main);
            transition: 0.3s;
        }
        .back-btn:hover { background: var(--primary); color: white; box-shadow: 0 0 15px var(--primary); }

        .tabs-container {
            padding: 12px 10px;
            background: rgba(0,0,0,0.2) !important;
            position: sticky;
            top: 56px; /* Offset by nav height */
            z-index: 50;
            backdrop-filter: blur(15px);
            border-bottom: 1px solid var(--crystal-border);
            width: 100%;
        }

        .tabs {
            display: flex;
            gap: 10px;
            flex-wrap: nowrap;
            overflow-x: auto;
            padding: 5px;
            background: rgba(255, 255, 255, 0.03);
            border-radius: 60px;
            border: 1px solid rgba(255,255,255,0.05);
        }
        .tabs::-webkit-scrollbar { display: none; }

        /* SMART GLASS STUDENT ROW (Sync with Student Manage) */
        .student-row { 
            background: rgba(255,255,255,0.03) !important; 
            padding: 10px 15px; 
            margin-bottom: 8px; 
            display: flex; 
            align-items: center; 
            gap: 15px;
            border-radius: 12px;
            border: 1px solid var(--crystal-border) !important;
            box-shadow: 0 4px 12px rgba(0,0,0,0.25);
            cursor: pointer;
            transition: all 0.2s ease;
            position: relative;
            overflow: hidden;
            backdrop-filter: blur(5px);
        }
        .student-row:hover { 
            background: rgba(255,255,255,0.08) !important; 
            border: 1px solid rgba(255,255,255,0.3) !important;
        }

        .student-info { display: flex; align-items: center; gap: 15px; flex: 1; min-width: 0; }
        .student-name { font-weight: 700; color: white; font-size: 14px; flex: 1; }
        
        .roll-badge {
            background: rgba(155, 89, 182, 0.2);
            padding: 2px 8px;
            border-radius: 8px;
            font-size: 10px;
            font-weight: bold;
            color: #9b59b6;
            border: 1px solid rgba(155, 89, 182, 0.3);
        }

        .status-pill { padding: 4px 12px; border-radius: 20px; font-size: 10px; font-weight: 800; text-transform: uppercase; border: 1px solid rgba(255,255,255,0.1); }
        .bg-present { background: rgba(46, 204, 113, 0.2) !important; color: #2ecc71 !important; border-color: rgba(46, 204, 113, 0.4) !important; }
        .bg-absent { background: rgba(231, 76, 60, 0.2) !important; color: #e74c3c !important; border-color: rgba(231, 76, 60, 0.4) !important; }
        .bg-leave { background: rgba(52, 152, 219, 0.2) !important; color: #3498db !important; border-color: rgba(52, 152, 219, 0.4) !important; }
        .bg-holiday { background: rgba(239, 68, 68, 0.2) !important; color: #ef4444 !important; border-color: rgba(239, 68, 68, 0.4) !important; }
        .bg-unknown { background: rgba(255,255,255,0.05) !important; color: var(--text-dim) !important; }

        .content { padding: 0 30px 40px; }
        .content-sec { display: none; width: 100%; border-radius: 20px; animation: fadeIn 0.4s ease-out; }
        .content-sec.active { display: block; }
        
        #daily-view {
            background: var(--view-bg) !important;
            border-radius: 30px !important;
            padding: 20px !important;
            border: 1px solid var(--modal-border);
            box-shadow: 0 15px 35px rgba(0,0,0,0.1);
            position: relative;
            z-index: 1;
            color: var(--text-main) !important;
        }

        .policy-sec { display: none; }
        .policy-sec.active { display: block; animation: fadeIn 0.3s ease; }
        
        .smart-panel { display: none; }
        .smart-panel.active { display: block; animation: fadeIn 0.3s ease; }
        
        .sub-tab-btn {
            flex: 1; /* Stretch to fill split screen */
            justify-content: center; /* Center text */
            padding: 12px 20px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 800;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            color: rgba(241, 196, 15, 0.6);
            background: rgba(241, 196, 15, 0.05);
            border: 1px solid rgba(241, 196, 15, 0.1);
            display: flex;
            align-items: center;
            gap: 8px;
            text-transform: uppercase;
            letter-spacing: 1px;
            position: relative;
        }
        
        .sub-tab-btn.active {
            background: #f1c40f;
            color: #020617; /* Dark text for yellow bg */
            border: 1px solid rgba(255,255,255,0.2);
            box-shadow: 0 4px 15px rgba(241, 196, 15, 0.3);
        }

        /* Inner Sub-Tabs for Salary */
        .inner-sub-tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            background: rgba(0, 0, 0, 0.2);
            padding: 5px;
            border-radius: 12px;
        }

        .inner-sub-tab-btn {
            flex: 1;
            padding: 10px;
            text-align: center;
            font-size: 0.75rem;
            font-weight: 700;
            border-radius: 8px;
            cursor: pointer;
            color: var(--text-dim);
            transition: all 0.3s ease;
        }

        .inner-sub-tab-btn.active {
            background: rgba(255, 255, 255, 0.05);
            color: white;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        }

        .inner-sec { display: none; }
        .inner-sec.active { display: block; animation: fadeIn 0.3s ease; }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(5px); }
            to { opacity: 1; transform: translateY(0); }
        }

        /* Premium Cards */
        .filter-card {
            background: rgba(30, 41, 59, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.05);
            border-radius: 20px;
            padding: 18px; /* Reduced from 25 */
            margin-bottom: 20px; /* Reduced from 30 */
            backdrop-filter: blur(5px);
            max-width: 100% !important;
            position: relative;
            z-index: 50;
            overflow: visible !important;
        }

        /* Stats Cards */
        .avg-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 20px;
            margin-top: 25px;
        }

        .avg-card {
            background: rgba(30, 41, 59, 0.8);
            border-radius: 20px;
            padding: 25px;
            text-align: left;
            position: relative;
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.05);
            transition: transform 0.3s ease;
        }
        
        .avg-card:hover { 
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            border-color: rgba(255,255,255,0.1);
        }

        .avg-label {
            font-size: 0.8rem;
            text-transform: uppercase;
            letter-spacing: 1px;
            color: var(--text-secondary);
            margin-bottom: 10px;
        }

        .avg-value {
            font-size: 2rem;
            background: linear-gradient(to right, #fff, #cbd5e1);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        /* Controls */
        select, input[type="date"] {
            background: var(--input-bg, rgba(15, 23, 42, 0.6)) !important;
            border: 1px solid rgba(255, 255, 255, 0.1) !important;
            color: var(--text-main) !important;
            padding: 12px 20px !important;
            border-radius: 12px !important;
            font-size: 0.95rem;
            outline: none;
            width: 100%;
        }

        /* FIXED NAVIGATION BUTTONS */
        .nav-fixed-btn {
            position: fixed;
            top: 20px;
            z-index: 5000;
            width: 45px;
            height: 45px;
            border-radius: 50%;
            border: none;
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.2s;
            box-shadow: 0 8px 15px rgba(0, 0, 0, 0.3);
        }
        .nav-fixed-btn:hover {
            transform: scale(1.1);
        }
        .nav-fixed-btn:active {
            transform: scale(0.95);
        }
        .nav-fixed-btn.back-btn {
            left: 20px;
            background: linear-gradient(135deg, #3498db, #2980b9);
            box-shadow: 0 8px 15px rgba(52, 152, 219, 0.4);
        }
        .nav-fixed-btn.theme-btn {
            right: 20px;
            background: linear-gradient(135deg, #9b59b6, #8e44ad);
            box-shadow: 0 8px 15px rgba(155, 89, 182, 0.4);
        }
        @media screen and (max-width: 768px) {
            .nav-fixed-btn { display: none !important; }
        }

        .btn {
            background: var(--accent-primary);
            color: white;
            border: none;
            padding: 12px 25px;
            border-radius: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4);
        }

        /* Premium Action Icons Styling */
        .action-icon-btn {
            width: 48px;
            height: 48px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            border: 1px solid rgba(255, 255, 255, 0.15);
            position: relative;
            overflow: hidden;
            box-shadow: 0 8px 15px rgba(0,0,0,0.2);
            font-size: 20px;
        }

        .action-icon-btn::before {
            content: '';
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            background: linear-gradient(180deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 50%, rgba(0,0,0,0.05) 100%);
            pointer-events: none;
        }

        .action-icon-btn:hover {
            transform: translateY(-4px) scale(1.1);
            box-shadow: 0 12px 20px rgba(0,0,0,0.3);
            filter: brightness(1.2);
        }

        .action-icon-btn:active {
            transform: translateY(-1px) scale(0.95);
        }

        .icon-confirm {
            background: linear-gradient(135deg, #00b09b 0%, #96c93d 100%);
            color: #fff !important;
        }

        .icon-all {
            background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
            color: #fff !important;
        }

        .icon-reset {
            background: linear-gradient(135deg, #e0e0e0 0%, #bdbdbd 100%);
        }

        .icon-print {
            background: linear-gradient(135deg, #FF512F 0%, #DD2476 100%);
            color: #fff !important;
        }

        .icon-whatsapp {
            background: linear-gradient(135deg, #25D366 0%, #128C7E 100%);
            color: #fff !important;
        }


        .action-icon-btn i {
            text-shadow: 0 1px 2px rgba(0,0,0,0.2);
        }

        /* Smart Table Styling */
        .smart-table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0 10px;
        }

        .smart-table th {
            color: var(--text-secondary);
            font-weight: 600;
            padding: 15px 20px;
            font-size: 0.85rem;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .smart-table tbody tr {
            background: rgba(30, 41, 59, 0.4);
            transition: all 0.2s ease;
        }
        
        .smart-table tbody tr td {
            padding: 15px 20px;
            border-top: 1px solid rgba(255,255,255,0.05);
            border-bottom: 1px solid rgba(255,255,255,0.05);
            color: #e2e8f0;
        }
        
        .smart-table tbody tr td:first-child { 
            border-left: 1px solid rgba(255,255,255,0.05); 
            border-top-left-radius: 12px; 
            border-bottom-left-radius: 12px; 
        }
        
        .smart-table tbody tr td:last-child { 
            border-right: 1px solid rgba(255,255,255,0.05); 
            border-top-right-radius: 12px; 
            border-bottom-right-radius: 12px; 
        }

        .smart-table tbody tr:hover {
            transform: scale(1.01);
            background: rgba(51, 65, 85, 0.6);
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        }

        /* SMART LEDGER SYSTEM */
        .smart-ledger-container {
            width: 100%;
            overflow-x: auto;
            border-radius: 20px;
            background: var(--card-bg);
            border: 1px solid var(--modal-border);
            box-shadow: 0 10px 40px rgba(0,0,0,0.1);
            padding: 5px;
            margin-top: 15px;
        }

        .ledger-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        }

        .ledger-table th {
            padding: 18px 15px;
            text-align: center;
            font-weight: 900;
            text-transform: uppercase;
            letter-spacing: 1px;
            
            border-bottom: 2px solid var(--modal-border);
            white-space: nowrap;
        }

        .ledger-table td {
            padding: 15px;
            text-align: center;
            border-bottom: 1px solid var(--modal-border);
            
            transition: all 0.2s ease;
            font-weight: 700;
        }

        .ledger-table tr:hover td {
            background: rgba(147, 51, 234, 0.03);
            cursor: default;
        }

        .ledger-pill {
            padding: 6px 12px;
            border-radius: 10px;
            font-size: 11px;
            font-weight: 900;
            display: inline-block;
            min-width: 45px;
            text-align: center;
            letter-spacing: 0.5px;
            
        }
        .pill-blue { background: rgba(52, 152, 219, 0.15); border: 1px solid rgba(52, 152, 219, 0.3); }
        .pill-purple { background: rgba(155, 89, 182, 0.15); border: 1px solid rgba(155, 89, 182, 0.3); }
        .pill-green { background: rgba(46, 204, 113, 0.15); border: 1px solid rgba(46, 204, 113, 0.3); }
        .pill-red { background: rgba(231, 76, 60, 0.15); border: 1px solid rgba(231, 76, 60, 0.3); }
        .pill-orange { background: rgba(243, 156, 18, 0.15); border: 1px solid rgba(243, 156, 18, 0.3); }

        @media screen and (max-width: 768px) {
            .smart-ledger-container { background: transparent; border: none; box-shadow: none; padding: 0; }
            .ledger-table thead { display: none; }
        /* HIDE TIME PICKER ICONS FOR LABEL LOOK */
        input[type="time"]::-webkit-calendar-picker-indicator {
            display: none !important;
            -webkit-appearance: none;
        }
        
        input[type="time"] {
            -webkit-appearance: none;
            -moz-appearance: textfield;
            appearance: none;
        }

        .ledger-table, .ledger-table tbody, .ledger-table tr, .ledger-table td {
                display: block;
                width: 100%;
                box-sizing: border-box;
            }
            .ledger-table tr {
                margin-bottom: 20px;
                border: 1px solid var(--modal-border);
                border-radius: 20px;
                background: var(--card-bg);
                overflow: hidden;
                box-shadow: 0 10px 25px rgba(0,0,0,0.08);
                position: relative;
                padding-top: 10px;
            }
            .ledger-table td {
                display: flex;
                justify-content: space-between;
                align-items: center;
                text-align: right;
                padding: 12px 20px;
                border-bottom: 1px solid rgba(0,0,0,0.03);
                font-size: 14px;
            }
            .ledger-table td:last-child { border-bottom: none; }
            .ledger-table td::before {
                content: attr(data-label);
                font-weight: 800;
                text-transform: uppercase;
                font-size: 10px;
                color: var(--text-dim);
                float: left;
                text-align: left;
                letter-spacing: 0.8px;
            }
            .ledger-table td[data-label="NAME"], .ledger-table td[data-label="STAFF NAME"] {
                background: rgba(0,0,0,0.05);
                
                justify-content: center;
                font-weight: 900;
                font-size: 16px;
                padding: 18px;
                margin-top: -10px;
                margin-bottom: 10px;
                border-bottom: 2px solid var(--modal-border);
            }
            .ledger-table td[data-label="NAME"]::before { display: none; }
            .ledger-table td[data-label="C.No"] {
                position: absolute;
                top: 0; left: 0;
                width: auto;
                background: rgba(255,255,255,0.25);
                backdrop-filter: blur(5px);
                border-radius: 0 0 15px 0;
                padding: 6px 15px;
                z-index: 10;
                font-size: 12px;
                
                border: 1px solid rgba(0,0,0,0.1);
                font-weight: 900;
            }
            .ledger-table td[data-label="C.No"]::before { display: none; }
            .ledger-pill { transform: scale(1.1); }
        }

        /* Staff Attendance Cards */
        .att-card {
            background: linear-gradient(145deg, rgba(30, 41, 59, 0.4), rgba(15, 23, 42, 0.4));
            border: 1px solid rgba(255,255,255,0.05);
            border-radius: 16px;
            padding: 15px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
            transition: 0.3s;
            box-shadow: inset 2px 2px 5px rgba(255,255,255,0.05), 
                        inset -2px -2px 5px rgba(0,0,0,0.3);
        }
        
        .att-opt {
            width: 36px !important;
            height: 36px !important;
            font-weight: 700;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
            border-radius: 50% !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            cursor: pointer;
            font-size: 11px !important;
            border: 1px solid rgba(255,255,255,0.1) !important;
            box-shadow: 2px 2px 5px rgba(0,0,0,0.2), 
                        -2px -2px 5px rgba(255,255,255,0.02);
            color: rgba(255,255,255,0.5);
        }
        
        .att-opt:hover { transform: scale(1.1); }
        .att-opt.active { 
            transform: scale(1.15); 
            box-shadow: inset 2px 2px 4px rgba(0,0,0,0.3), 
                        0 0 15px rgba(255,255,255,0.1); 
            border-color: rgba(255,255,255,0.3) !important;
        }

        .opt-P.active { background: #4ade80 !important; color: #064e3b !important; }
        .opt-A.active { background: #991b1b !important; color: white !important; }
        .opt-L.active { background: #fbbf24 !important; color: #78350f !important; }

        /* Futuristic Badges */
        .badge-engagement {
            font-size: 10px;
            padding: 2px 8px;
            border-radius: 50px;
            font-weight: 700;
            text-transform: uppercase;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .badge-fire { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3); box-shadow: 0 0 10px rgba(239, 68, 68, 0.2); }
        .badge-shield { background: rgba(59, 130, 246, 0.2); color: #93c5fd; border: 1px solid rgba(59, 130, 246, 0.3); }

        /* Sub-Tabs (Policy Center) */
        .sub-tabs { display: flex; gap: 15px; margin-bottom: 25px; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 10px; }
        .sub-tab-btn { color: var(--text-secondary); font-size: 0.85rem; font-weight: 600; cursor: pointer; padding: 5px 10px; border-radius: 8px; transition: 0.3s; }
        .sub-tab-btn:hover { background: rgba(255,255,255,0.05); color: white; }
        .sub-tab-btn.active { color: var(--accent-primary); background: rgba(99, 102, 241, 0.1); }
        .policy-sec { display: none; }
        .policy-sec.active { display: block; animation: fadeIn 0.3s ease; }
        .policy-input { width:100%; padding:12px; background:rgba(15,23,42,0.6); border:1px solid rgba(255,255,255,0.1); border-radius:12px; color:white; font-weight:700; outline:none; }
        .policy-input:focus { border-color: var(--primary); }

        /* Premium Edit Modal - Neumorphic Style */
        .premium-modal-overlay {
            position: fixed;
            top: 0; left: 0; width: 100%; height: 100%;
            background: var(--modal-overlay-bg, rgba(15, 23, 42, 0.85));
            backdrop-filter: blur(10px);
            z-index: 10000;
            display: flex;
            align-items: flex-start; /* Ensure top is visible even if tall */
            justify-content: center;
            padding: 20px;
            overflow-y: auto; /* Allow scrolling if modal is taller than screen */
        }
        .premium-modal {
            background: var(--modal-bg) !important;
            border: 1px solid var(--modal-border);
            border-radius: 28px;
            width: 100%;
            max-width: 400px;
            box-shadow: 0 20px 50px rgba(0,0,0,0.5);
            margin-top: 40px; /* Space from top */
            margin-bottom: 40px; /* Space at bottom for scrolling */
            animation: modalPop 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
            position: relative;
            color: var(--modal-text) !important;
        }
        @keyframes modalPop {
            from { transform: scale(0.9) translateY(40px); opacity: 0; }
            to { transform: scale(1) translateY(0); opacity: 1; }
        }
        .modal-header {
            padding: 25px;
            background: rgba(255,255,255,0.02);
            border-bottom: 1px solid rgba(255,255,255,0.05);
            display:flex; justify-content: space-between; align-items: center;
        }
        .modal-body { padding: 30px; }
        .modal-footer {
            padding: 20px 25px;
            background: var(--modal-sub-bg, rgba(0,0,0,0.2));
            border-top: 1px solid var(--modal-border);
            display: flex; justify-content: flex-end; gap: 12px;
        }
        .premium-modal h4, .premium-modal div, .premium-modal label, .premium-modal span, .premium-modal i { color: inherit !important; }
        .premium-modal .att-opt {  }
        .premium-modal .policy-input { background: rgba(0,0,0,0.05) !important; border: 1px solid rgba(0,0,0,0.1) !important; color: var(--modal-text) !important; }

        /* Sticky Action Bar */
        .sticky-action-bar {
            position: fixed;
            bottom: 30px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 999;
            background: var(--modal-bg);
            backdrop-filter: blur(15px);
            padding: 12px 30px;
            border-radius: 50px;
            border: 1px solid rgba(255,255,255,0.1);
            box-shadow: 0 15px 35px rgba(0,0,0,0.4), inset 0 1px 1px rgba(255,255,255,0.1);
            display: flex;
            gap: 25px;
            animation: slideUp 0.5s ease;
        }

        .smart-mobile-title {
            font-family: var(--smart-font);
            font-weight: 900;
            font-size: 14px;
            letter-spacing: -0.2px;
            text-transform: uppercase;
            font-style: italic;
            background: linear-gradient(135deg, #fff 0%, rgba(255,255,255,0.7) 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            flex: 1;
            text-align: center;
            padding: 0 10px;
        }
        @keyframes slideUp {
            from { bottom: -100px; opacity: 0; }
            to { bottom: 30px; opacity: 1; }
        }

        #student-marking-container, #staff-result {
            padding-bottom: 120px !important;
        }

        .sticky-action-bar {
            box-shadow: 0 20px 40px rgba(0,0,0,0.6), 
                        inset 0 1px 0 rgba(255,255,255,0.1),
                        10px 10px 20px rgba(0,0,0,0.3);
        }
        body.force-mobile {
            max-width: 440px !important;
            margin: 0 auto !important;
            border-left: 1px solid rgba(155, 89, 182, 0.2);
            border-right: 1px solid rgba(155, 89, 182, 0.2);
            box-shadow: 0 0 60px rgba(0,0,0,0.8);
            background: #020617 !important;
            overflow-x: hidden !important;
        }
        
        body.force-mobile * {
            max-width: 100% !important;
        }
        
        body.force-mobile .max-w-7xl { 
            max-width: 100% !important; 
            padding-left: 12px !important;
            padding-right: 12px !important;
        }
        
        body.force-mobile .grid { 
            grid-template-columns: 1fr !important; 
        }

        /* Fix container widths */
        body.force-mobile .w-full,
        body.force-mobile .px-4,
        body.force-mobile .sm\\:px-6,
        body.force-mobile .lg\\:px-8 {
            width: 100% !important;
            padding-left: 12px !important;
            padding-right: 12px !important;
        }

        /* Fix tabs container */
        body.force-mobile .tabs-container {
            width: 100% !important;
            overflow-x: auto !important;
            padding: 0 12px !important;
        }

        body.force-mobile .tabs {
            display: flex !important;
            flex-wrap: nowrap !important;
            overflow-x: auto !important;
            gap: 8px !important;
            padding-bottom: 8px !important;
            -webkit-overflow-scrolling: touch;
        }

        body.force-mobile .tab-btn {
            flex: 0 0 auto !important;
            min-width: 120px !important;
            white-space: nowrap !important;
            font-size: 11px !important;
            padding: 10px 15px !important;
        }

        /* Fix content areas */
        body.force-mobile .content-sec {
            width: 100% !important;
            padding: 0 !important;
        }

        body.force-mobile .filter-card {
            width: 100% !important;
            margin-left: 0 !important;
            margin-right: 0 !important;
            padding: 12px !important;
        }


        /* Real Device Media Query */
        
        /* Calendar Dropdown Styles */
        .calendar-wrapper { position: relative; display: block; margin-top: 15px; }
        .calendar-trigger {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #f7f7f2 !important;
            border: 1px solid rgba(0, 0, 0, 0.1);
            padding: 12px 15px;
            border-radius: 12px;
            
            cursor: pointer;
            transition: all 0.3s ease;
        }
        .calendar-trigger:hover { background: #ffffff !important; border-color: #000; }
        
        .calendar-dropdown {
            display: none;
            position: absolute;
            top: calc(100% + 5px);
            left: 0;
            width: 320px;
            background: #ffffff !important;
            border-radius: 15px;
            padding: 20px;
            box-shadow: 0 20px 50px rgba(0,0,0,0.3);
            z-index: 99999 !important;
            border: 1px solid rgba(0,0,0,0.1);
            animation: slideDown 0.3s ease-out;
            color: #000 !important; /* Force black text for calendar by default */
        }
        @keyframes slideDown {
            from { opacity: 0; transform: translateY(-10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        
        /* Show dropdown on hover */
        .calendar-wrapper.open .calendar-dropdown { display: block; animation: fadeIn 0.3s ease; }
        
        .date-grid {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
            gap: 5px;
        }

        .month-year-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 10px;
            padding: 10px 0;
        }

        .select-item {
            padding: 8px;
            text-align: center;
            background: #f1f5f9;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 700;
            color: #000;
            cursor: pointer;
            transition: all 0.2s;
        }
        .summary-grid { grid-template-columns: repeat(2, 1fr) !important; }
        @media (max-width: 768px) {
            .summary-grid { grid-template-columns: 1fr !important; }
        }
        .select-item:hover { background: #e2e8f0; color: #3498db; }
        .select-item.active { background: #3498db; color: #fff; }

        .date-cell {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
            gap: 5px;
        }
        .date-cell {
            aspect-ratio: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            font-weight: 700;
            color: #334155;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s;
            background: #f8fafc;
        }
        .date-cell:hover {
            background: #3498db;
            color: white;
            box-shadow: 0 4px 10px rgba(52, 152, 219, 0.3);
        }
        .date-cell.today {
            border: 2px solid #3498db;
            color: #3498db;
        }
        
        /* Theme Overrides for Calendar */
        body.theme-tactile .calendar-dropdown { background: #fdfdfb; border: 1px solid rgba(241, 196, 15, 0.2); }

        @media (max-width: 768px) {
            .main-board {
                padding: 10px !important;
                margin: 0 !important;
                max-width: 100% !important;
                border-radius: 0 !important;
                border: none !important;
                overflow-x: hidden;
            }
            .section-card, .filter-card, .avg-card, .att-card {
                padding: 15px 12px !important;
                margin-left: 0 !important;
                margin-right: 0 !important;
                margin-bottom: 15px !important;
                width: 100% !important;
                border-radius: 16px !important;
                border-left: none !important;
                border-right: none !important;
                backdrop-filter: none !important;
            }
            .main-tabs-grid, .sub-tabs, .tabs, .inner-sub-tabs {
                display: flex !important;
                flex-direction: row !important;
                flex-wrap: nowrap !important;
                overflow-x: auto !important;
                padding: 10px 5px 20px !important;
                -webkit-overflow-scrolling: touch;
                gap: 12px !important;
                scrollbar-width: none;
            }
            .main-tabs-grid::-webkit-scrollbar, .sub-tabs::-webkit-scrollbar { display: none; }
            
            .tab-btn, .sub-tab-btn {
                flex: 0 0 160px !important;
                min-width: 160px !important;
                margin: 0 !important;
                font-size: 11px !important;
                padding: 12px 10px !important;
            }
            .student-row {
                padding: 12px 18px !important;
                margin-left: 0 !important;
                margin-right: 0 !important;
                width: 100% !important;
                border-radius: 12px !important;
                border-left: none !important;
                border-right: none !important;
                display: flex !important;
                flex-direction: column !important;
            }
            .student-info {
                gap: 8px !important;
            }
            .roll-badge {
                margin: 0 !important;
                min-width: 28px;
                height: 28px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 50% !important;
                font-size: 11px !important;
            }
            .student-name {
                font-size: 13px !important;
                text-align: left !important;
                font-weight: 700 !important;
            }
            
            /* Action Icons toolbar on Mobile */
            .action-btn-row {
                justify-content: center !important;
                gap: 25px !important;
                padding: 10px 0 !important;
            }
            .action-icon-btn {
                width: 44px !important;
                height: 44px !important;
                font-size: 18px !important;
            }

            .att-options {
                width: auto !important;
                justify-content: flex-end !important;
            }
            
            /* Date and Filters on mobile */
            .filter-card > div[style*="display:flex"] {
                flex-direction: column !important;
                align-items: stretch !important;
                gap: 12px !important;
            }
            .calendar-wrapper, .filter-card select, .filter-card input, .filter-card > div[style*="display:flex"] > div {
                width: 100% !important;
                margin: 0 !important;
                flex: none !important;
            }
            .calendar-trigger { width: 100% !important; }
        }
        
        /* Force Black Text for Attendance Options (P, A, L, S, R) */
        .att-opt {  font-weight: 900 !important; }

        /* ========== LAYOUT CLASSES (Staff Manage Style) ========== */
        .main-board {
            max-width: 1400px;
            margin: 0 auto;
            padding: 20px;
        }

        .section-card {
            background: rgba(30, 41, 59, 0.6);
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255,255,255,0.08);
            border-radius: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            padding: 20px;
            margin-bottom: 20px;
        }

        .spongy-card {
            background: rgba(30, 41, 59, 0.8);
            border: 1px solid rgba(255, 213, 161, 0.3);
            border-radius: 16px;
            box-shadow: 0 20px 45px rgba(0, 0, 0, 0.4), 0 8px 12px rgba(0, 0, 0, 0.2), inset 0 1px 2px rgba(255, 255, 255, 0.1);
            padding: 15px;
            transition: all 0.3s ease;
        }

        .spongy-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5), 0 10px 15px rgba(0, 0, 0, 0.25), inset 0 1px 2px rgba(255, 255, 255, 0.15);
        }

        .tab-btn.active {
            background: linear-gradient(135deg, #3498db, #2980b9) !important;
            color: white !important;
            box-shadow: 0 15px 35px rgba(52, 152, 219, 0.5), 0 8px 12px rgba(52, 152, 219, 0.3), inset 0 1px 2px rgba(255, 255, 255, 0.2) !important;
            transform: translateY(-5px) !important;
        }

        .mobile-only {
            display: none !important;
        }
        .desktop-only {
            display: flex !important;
        }

        @media (max-width: 768px) {
            .mobile-only {
                display: flex !important;
            }
            .desktop-only {
                display: none !important;
            }
            .main-board {
                padding: 10px;
            }
        }
        
        /* Hardware Tubes with Holes */
        .hardware-tube {
            position: absolute;
            top: 10%;
            bottom: 10%;
            width: 8px;
            border-radius: 4px;
            background: linear-gradient(90deg, #555, #eee, #444);
            box-shadow: 0 2px 4px rgba(0,0,0,0.5), inset 0 -2px 4px rgba(0,0,0,0.3);
            z-index: 10;
        }
        .hardware-tube.left { left: 5px; }
        .hardware-tube.right { right: 5px; }
        
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

        /* Unified Active Tab Style */
        .sub-tab-btn.active {
            background: linear-gradient(135deg, #3498db, #2980b9) !important;
            color: white !important;
            box-shadow: 0 15px 35px rgba(52, 152, 219, 0.5), 0 8px 12px rgba(52, 152, 219, 0.3), inset 0 1px 2px rgba(255, 255, 255, 0.2) !important;
            transform: translateY(-5px) !important;
            border: none !important;
        }

        /* Theme 2: INDIGO (Professional Dark) */
        body.theme-indigo {
            background-color: #020617 !important;
            color: #f8fafc;
            --text-main: #f8fafc;
            --text-dim: #94a3b8;
            --view-bg: #020617;
            --modal-bg: #1e293b;
            --modal-text: #f8fafc;
            --modal-border: rgba(255,255,255,0.1);
            --modal-sub-bg: rgba(255,255,255,0.05);
            --modal-overlay-bg: rgba(2, 6, 23, 0.85);
        }
        body.theme-indigo .section-card, body.theme-indigo .filter-card, body.theme-indigo .avg-card, body.theme-indigo .att-card {
            background: rgba(30, 41, 59, 0.4) !important;
        }
        body.theme-indigo label, body.theme-indigo h3, body.theme-indigo h4, body.theme-indigo p, 
        body.theme-indigo th, body.theme-indigo td, body.theme-indigo select, body.theme-indigo input,
        body.theme-indigo span:not(.ledger-pill):not(.roll-badge):not(.att-opt),
        body.theme-indigo .student-name, body.theme-indigo .header-info-content strong {
            color: #f8fafc !important;
        }
        body.theme-indigo .ledger-pill, body.theme-indigo .roll-badge, body.theme-indigo .att-opt {
            
        }
        body.theme-indigo .spongy-card { background: rgba(30, 41, 59, 0.8) !important; color: #f8fafc !important; }
        body.theme-indigo .tab-btn.active, body.theme-indigo .sub-tab-btn.active { color: #fff !important; }
        body.theme-indigo .smart-mobile-title { color: #f8fafc !important; }
        body.theme-indigo .tab-btn, body.theme-indigo .sub-tab-btn { color: #f8fafc !important; }
        body.theme-indigo select option { background: #1e293b; color: #fff; }

        /* Camera Placeholder Theme Fix */
        #faceVideoPlaceholder {
            background: rgba(255, 255, 255, 0.95) !important;
            color: #000 !important;
        }
        body.theme-indigo #faceVideoPlaceholder {
            background: rgba(2, 6, 23, 0.98) !important;
            color: #fff !important;
        }
        body.theme-indigo #faceVideoPlaceholder h3,
        body.theme-indigo #faceVideoPlaceholder p {
            color: #fff !important;
        }
        body.theme-indigo #faceVideoPlaceholder .spinner-track {
            border-color: rgba(255,255,255,0.1) !important;
        }

        /* Theme 1: TACTILE (Corporate White with 3D Pop-out) */
        html.theme-tactile, body.theme-tactile, 
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

        /* CRITICAL: Override global dark containers */
        body.theme-tactile .mobile-container {
            background: transparent !important;
            background-image: none !important;
            box-shadow: none !important;
            border: none !important;
            backdrop-filter: none !important;
            -webkit-backdrop-filter: none !important;
        }

        body.theme-tactile .filter-card,
        body.theme-tactile .avg-card,
        body.theme-tactile .att-card,
        body.theme-tactile .section-card,
        body.theme-tactile .spongy-card,
        body.theme-tactile .attendance-panel {
            background: #ffffff !important;
            border: 1px solid rgba(0, 0, 0, 0.1) !important;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05) !important;
            color: #000000 !important;
        }
        
        /* Force black labels and spans in Tactile/White */
        body.theme-tactile label, 
        body.theme-tactile span,
        body.theme-tactile .sub-tab-btn span,
        body.theme-tactile #display_date_text,
        body.theme-tactile #staff_display_date_text {
            color: #000000 !important;
            text-shadow: none !important;
        }

        /* FIX: Student Row Visibility in White Theme */
        body.theme-tactile .student-row,
        body.theme-tactile .student-row .student-name,
        body.theme-tactile .student-row .roll-badge,
        body.theme-tactile .student-row .att-opt:not(.active),
        body.theme-tactile .student-row span:not(.status-pill):not([style*="color"]) {
            color: #000000 !important;
            text-shadow: none !important;
        }
        
        body.theme-tactile .student-row {
            background: #ffffff !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05) !important;
        }

        body.theme-tactile .att-opt:not(.active) {
            background: rgba(0,0,0,0.05) !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            color: #000000 !important;
        }

        /* POLICY / RULES SECTION SPECIFIC ENFORCEMENT */
        body.theme-tactile #policy-view *:not(button):not(.btn):not(.btn *),
        body.theme-tactile #policy-view h3,
        body.theme-tactile #policy-view label,
        body.theme-tactile #policy-view p,
        body.theme-tactile #policy-view input,
        body.theme-tactile #policy-view span:not(.btn *),
        body.theme-tactile #policy-view b:not(.btn *) {
            color: #000000 !important;
            opacity: 1 !important;
            -webkit-text-fill-color: #000000 !important;
            font-weight: 800 !important;
        }

        body.theme-tactile #policy-view .policy-input {
            background: #ffffff !important;
            border: 2px solid #000000 !important;
            color: #000000 !important;
        }

        body.theme-tactile #policy-view .btn {
            background: #000000 !important;
            color: #ffffff !important;
            -webkit-text-fill-color: #ffffff !important;
            box-shadow: 0 15px 35px rgba(0,0,0,0.3) !important;
        }
        body.theme-tactile #policy-view .btn i { 
            color: #ffffff !important; 
            -webkit-text-fill-color: #ffffff !important;
        }

        /* MONTHLY ANALYSIS SECTION SPECIFIC ENFORCEMENT */
        body.theme-tactile #analysis-view,
        body.theme-tactile #analysis-view h4,
        body.theme-tactile #analysis-view label,
        body.theme-tactile #analysis-view td,
        body.theme-tactile #analysis-view th,
        body.theme-tactile #analysis-view span,
        body.theme-tactile #analysis-view strong,
        body.theme-tactile #analysis-view .avg-label,
        body.theme-tactile #analysis-view .avg-value {
            color: #000000 !important;
            opacity: 1 !important;
            -webkit-text-fill-color: #000000 !important;
        }

        body.theme-tactile .smart-table tbody tr td,
        body.theme-tactile .report-table tbody tr td {
            color: #000000 !important;
            font-weight: 700 !important;
        }

        /* Exception for status-specific labels if they need to be readable */
        body.theme-tactile .row-present { color: #15803d !important; }
        body.theme-tactile .row-absent { color: #b91c1c !important; }
        body.theme-tactile .row-leave { color: #1d4ed8 !important; }
        body.theme-tactile #analysis-view {
            background: #EAE0D5 !important;
        }

        /* FORCE THEME FONTS FOR ALL TABS */
        .tab-btn, .tab-btn *, .sub-tab-btn, .sub-tab-btn *, .tab-btn.active, .sub-tab-btn.active {
            color: inherit !important;
            font-weight: 900 !important;
        }
        
        /* CALENDAR HEADER COLOR FIX */
        .calendar-dropdown h4, .calendar-dropdown h4 span {
            color: #000;
        }

        /* --- GLOBAL THEME FONT COLOR ENFORCEMENT --- */
        
        /* --- GLOBAL THEME FONT COLOR ENFORCEMENT (AGGRESSIVE) --- */
        
        /* Dark Theme (Indigo) -> White Text (entire module) */
        body.theme-indigo, 
        body.theme-indigo *, 
        body.theme-indigo .avg-card, 
        body.theme-indigo .filter-card,
        body.theme-indigo .student-row, 
        body.theme-indigo .smart-table,
        body.theme-indigo .ledger-table,
        body.theme-indigo .modal-header,
        body.theme-indigo .status-pill:not(.bg-present):not(.bg-absent):not(.bg-leave) {
            color: #ffffff !important;
            border-color: rgba(255,255,255,0.15) !important;
        }

        /* Light Theme (Tactile / White) -> Black Text (entire module) */
        body.theme-tactile, body.theme-tactile *,
        body.theme-tactile .avg-card, body.theme-tactile .filter-card, body.theme-tactile .spongy-card,
        body.theme-tactile .student-row,
        body.theme-tactile .ledger-table {
            color: #000000 !important;
            border-color: rgba(0,0,0,0.15) !important;
        }
        
        /* Re-enforce Light-on-Dark exceptions for Light Theme (Actions/Primary) */
        body.theme-tactile .btn, body.theme-tactile .btn *, 
        body.theme-tactile .capsule-3d, body.theme-tactile .capsule-3d i,
        body.theme-tactile .action-icon-btn, body.theme-tactile .action-icon-btn i {
            color: #ffffff !important;
        }
        
        /* Specific contrast for statistics text in light theme */
        body.theme-tactile .avg-value {
            background: none !important;
            -webkit-text-fill-color: #000000 !important;
            color: #000000 !important;
            font-weight: 900 !important;
        }

        /* Global Input Reset - Theme Specific overrides below */
        select, input[type="date"], input[type="number"], input[type="text"], .policy-input {
            border: 1px solid rgba(0,0,0,0.1) !important;
        }

        body.theme-indigo select, 
        body.theme-indigo input[type="date"], 
        body.theme-indigo input[type="number"], 
        body.theme-indigo input[type="text"], 
        body.theme-indigo input[type="password"],
        body.theme-indigo textarea,
        body.theme-indigo .policy-input,
        body.theme-indigo .calendar-trigger,
        body.theme-indigo .calendar-dropdown,
        body.theme-indigo .calendar-dropdown *,
        body.theme-indigo .date-cell,
        body.theme-indigo .select-item {
            background-color: #000000 !important; /* Inverted: Black Bg */
            color: #ffffff !important;           /* Pure White Text */
            -webkit-text-fill-color: #ffffff !important;
            border: 1px solid rgba(255,255,255,0.2) !important;
        }
        body.theme-indigo .calendar-dropdown { border: 1px solid #cbd5e1 !important; }
        body.theme-indigo .date-cell:hover, body.theme-indigo .select-item:hover { background-color: #3498db !important; color: white !important; }
        body.theme-indigo .date-cell.active, body.theme-indigo .select-item.active { background-color: #3498db !important; color: white !important; }
        
        body.theme-indigo .calendar-trigger .capsule-3d,
        body.theme-indigo .calendar-trigger .capsule-3d i {
            background-color: transparent !important;
            color: #ffffff !important;
            box-shadow: none !important;
        }
        
        body.theme-indigo select option {
            background-color: #000000 !important;
            color: #ffffff !important;
        }
        body.theme-indigo ::placeholder { color: rgba(255,255,255,0.4) !important; }

        /* --- CORPORATE MODE (TACTILE) DROPDOWN: BLACK BG + WHITE TEXT --- */
        body.theme-tactile select, 
        body.theme-tactile input[type="date"], 
        body.theme-tactile input[type="number"], 
        body.theme-tactile input[type="text"], 
        body.theme-tactile input[type="password"],
        body.theme-tactile textarea,
        body.theme-tactile .policy-input,
        body.theme-tactile .calendar-trigger,
        body.theme-tactile .calendar-dropdown,
        body.theme-tactile .calendar-dropdown *,
        body.theme-tactile .date-cell,
        body.theme-tactile .select-item {
            background-color: #ffffff !important; /* White Background */
            color: #000000 !important;           /* Black Text */
            -webkit-text-fill-color: #000000 !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            font-weight: 800 !important;
        }
        body.theme-tactile .calendar-trigger .capsule-3d { background: transparent !important; box-shadow: none !important; }
        body.theme-tactile .date-cell:hover, body.theme-tactile .select-item:hover { background-color: #3498db !important; color: white !important; }
        body.theme-tactile .calendar-dropdown h4, body.theme-tactile .calendar-dropdown h4 span { color: #000000 !important; border-bottom-color: rgba(0,0,0,0.1) !important; }
        
        body.theme-tactile .calendar-trigger * {
            color: #000000 !important;
        }
        body.theme-tactile select option {
            background-color: #ffffff !important;
            color: #000000 !important;
        }
        body.theme-tactile ::placeholder { color: rgba(0,0,0,0.5) !important; }

        /* --- TACTILE THEME SPECIFIC OVERRIDE --- */
        body.theme-tactile select, 
        body.theme-tactile input[type="date"], 
        body.theme-tactile input[type="number"], 
        body.theme-tactile input[type="text"], 
        body.theme-tactile input[type="password"],
        body.theme-tactile textarea,
        body.theme-tactile .policy-input,
        body.theme-tactile .calendar-trigger,
        body.theme-tactile .calendar-trigger *,
        body.theme-tactile .calendar-dropdown,
        body.theme-tactile .calendar-dropdown *,
        body.theme-tactile .date-cell,
        body.theme-tactile .select-item {
            background-color: #ffffff !important; 
            color: #000000 !important;           
            -webkit-text-fill-color: #000000 !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            font-weight: 800 !important;
        }
        body.theme-tactile .calendar-dropdown h4, 
        body.theme-tactile .calendar-dropdown h4 span {
            color: #000000 !important;
            border-bottom-color: rgba(0,0,0,0.1) !important;
        }
        body.theme-tactile select option {
            background-color: #ffffff !important;
            color: #000000 !important;
        }

        /* Force calendar icon to be black in light theme */
        body.theme-tactile input[type="date"]::-webkit-calendar-picker-indicator {
            filter: brightness(0) !important;
            opacity: 1 !important;
            cursor: pointer;
        }

        /* EXCEPTIONS for status colors to remain visible */
        .status-pill.bg-present, .opt-P.active { color: #15803d !important; }
        .status-pill.bg-absent, .opt-A.active { color: white !important; }
        .status-pill.bg-leave, .opt-L.active { color: #1d4ed8 !important; }
        .status-pill.bg-holiday, .opt-H.active, .opt-PH.active { color: white !important; background: #ef4444 !important; border-color: #ef4444 !important; }
        .btn, .btn * { color: white !important; }
        .roll-badge, .ledger-pill { color: inherit !important; }

        /* Manual Filters Grid Responsive */
        .manual-filters-grid {
            display: grid;
            grid-template-columns: 1fr;
            gap: 15px;
            max-width: 600px;
            margin: 0 auto;
        }
        @media (min-width: 769px) {
            .manual-filters-grid {
                grid-template-columns: 1fr 1fr auto;
                max-width: 100%;
                align-items: flex-end;
            }
            .manual-filters-grid > button {
                height: 50px !important;
                margin-top: 0 !important;
            }
        }
        
        .manual-item.selected {
            background: var(--primary) !important;
            color: white !important;
            border-color: rgba(255,255,255,0.4) !important;
            box-shadow: 0 5px 15px rgba(155, 89, 182, 0.4);
            transform: scale(0.95) !important;
        }
        .manual-item.selected * { color: white !important; }
        @media (min-width: 769px) {
            .manual-tab-btn {
                flex: 1 !important;
                margin-top: 0 !important;
            }
        }
        @media (max-width: 768px) {
            .manual-tab-btn {
                flex: 1 1 100% !important;
                margin-top: 10px;
            }
            .sub-tab-btn-compact {
                flex: 1 !important;
                min-width: auto !important;
                padding: 12px 10px !important;
                font-size: 11px !important;
            }
        }
        body.theme-tactile .status-pill.bg-present { color: #15803d !important; }
        body.theme-tactile .status-pill.bg-absent { color: #b91c1c !important; }
        body.theme-tactile .status-pill.bg-leave { color: #1d4ed8 !important; }
        
        .theme-print-btn { color: #fff !important; border-color: rgba(255,255,255,0.2) !important; }
        body.theme-tactile .theme-print-btn { 
            color: #000 !important; 
            border-color: rgba(0,0,0,0.1) !important;
        }

        /* Reporting Styles */
        .report-modal-body { max-height: 70vh; overflow-y: auto; padding: 20px; }
        .report-table { width: 100%; border-collapse: separate; border-spacing: 0 8px; }
        .report-table th { font-size: 10px; text-transform: uppercase; color: var(--text-dim); text-align: center; padding: 10px; width: 25%; }
        .report-table th:first-child { text-align: left; width: 40%; }
        .report-row { background: rgba(255,255,255,0.03); border-radius: 12px; transition: 0.2s; cursor: pointer; }
        .report-row:hover { background: rgba(255,255,255,0.08); transform: scale(1.02); }
        .report-row td { padding: 15px 10px; text-align: center; font-weight: 700; border-top: 1px solid rgba(255,255,255,0.05); border-bottom: 1px solid rgba(255,255,255,0.05); }
        .report-row td:first-child { border-left: 1px solid rgba(255,255,255,0.05); border-top-left-radius: 12px; border-bottom-left-radius: 12px; text-align: left; }
        .report-row td:last-child { border-right: 1px solid rgba(255,255,255,0.05); border-top-right-radius: 12px; border-bottom-right-radius: 12px; }
        
        .row-total { color: #fff; }
        .row-present { color: #2ecc71; }
        .row-leave { color: #3498db; }
        .row-absent { color: #e74c3c; }

        .name-chip-container { display: flex; flex-wrap: wrap; gap: 8px; padding: 10px; }
        .name-chip { padding: 6px 15px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 30px; font-size: 12px; font-weight: 700; color: #fff; }
        .name-chip.Present { border-color: #2ecc71; color: #2ecc71; background: rgba(46, 204, 113, 0.1); }
        .name-chip.Absent { border-color: #e74c3c; color: #e74c3c; background: rgba(231, 76, 60, 0.1); }
        .name-chip.Leave { border-color: #3498db; color: #3498db; background: rgba(52, 152, 219, 0.1); }
        .name-chip .cno { font-size: 10px; opacity: 0.6; margin-left: 5px; }

        body.theme-tactile .report-row { background: #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
        body.theme-tactile .name-chip { background: #f1f5f9; color: #1e293b; border-color: #e2e8f0; }
        body.theme-tactile .row-total { color: #1e293b; }
        /* ==========================================================
           SMART ATTENDANCE UI ENHANCEMENTS (FUTURISTIC & MODERN)
           ========================================================== */
        
        /* 1. Modern Premium Toggles (Pure CSS) */
        .switch-premium {
            position: relative;
            display: inline-block;
            width: 44px;
            height: 24px;
        }
        .switch-premium input { opacity: 0; width: 0; height: 0; }
        .slider-premium {
            position: absolute;
            cursor: pointer;
            top: 0; left: 0; right: 0; bottom: 0;
            background-color: rgba(255,255,255,0.05);
            transition: .4s cubic-bezier(0.4, 0, 0.2, 1);
            border-radius: 34px;
            border: 1px solid rgba(255,255,255,0.1);
        }
        .slider-premium:before {
            position: absolute;
            content: "";
            height: 18px;
            width: 18px;
            left: 2px;
            bottom: 2px;
            background-color: #fff;
            transition: .4s cubic-bezier(0.4, 0, 0.2, 1);
            border-radius: 50%;
            box-shadow: 0 2px 10px rgba(0,0,0,0.4);
        }
        .switch-premium input:checked + .slider-premium { background-color: var(--primary); }
        .switch-premium input:checked + .slider-premium:before { transform: translateX(20px); }
        
        /* Light Theme Toggles */
        body.theme-tactile .slider-premium {
            background-color: rgba(0,0,0,0.05);
            border-color: rgba(0,0,0,0.1);
        }
        body.theme-tactile .switch-premium input:checked + .slider-premium {
            background-color: #000;
        }

        /* 2. Face Detection List Enhancements */
        .detection-item {
            background: rgba(255,255,255,0.03);
            border: 1px solid rgba(255,255,255,0.05);
            border-radius: 12px;
            padding: 10px;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 12px;
            transition: all 0.3s ease;
            animation: slideInLeft 0.4s ease-out;
            position: relative;
            overflow: hidden;
        }
        .detection-item:hover {
            background: rgba(46, 204, 113, 0.1);
            border-color: rgba(46, 204, 113, 0.3);
            transform: scale(1.02);
        }

        /* 3. Fingerprint Management Card Styles */
        .fp-item-row {
            background: rgba(15, 23, 42, 0.6) !important;
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255, 255, 255, 0.08);
            padding: 16px 20px;
            border-radius: 18px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            margin-bottom: 2px;
        }
        .fp-item-row:hover {
            background: rgba(15, 23, 42, 0.8) !important;
            border-color: rgba(99, 102, 241, 0.3);
            transform: translateX(5px);
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        }
        .fp-user-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
        .fp-name-wrapper { display: flex; align-items: center; gap: 10px; }
        .fp-name { font-weight: 800; font-size: 14px; color: #fff; letter-spacing: 0.5px; }
        .role-badge { 
            font-size: 9px; 
            font-weight: 900; 
            padding: 3px 10px; 
            border-radius: 6px; 
            letter-spacing: 0.5px; 
            display: flex;
            align-items: center;
            gap: 5px;
        }
        .role-badge.student { background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.3); }
        .role-badge.staff { background: rgba(168, 85, 247, 0.15); color: #a855f7; border: 1px solid rgba(168, 85, 247, 0.3); }
        .fp-meta { display: flex; align-items: center; gap: 15px; margin-top: 2px; }
        .fp-meta-item { font-size: 10px; color: var(--text-dim); font-weight: 600; display: flex; align-items: center; gap: 6px; }
        .fp-meta-item i { font-size: 11px; opacity: 0.7; }
        
        .fp-delete-btn {
            background: rgba(239, 68, 68, 0.1) !important;
            border: 1px solid rgba(239, 68, 68, 0.2) !important;
            color: #ef4444 !important;
            padding: 10px 18px !important;
            border-radius: 12px !important;
            font-size: 11px !important;
            font-weight: 900 !important;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
            transition: all 0.3s ease;
            white-space: nowrap;
        }
        .fp-delete-btn:hover {
            background: rgba(239, 68, 68, 0.25) !important;
            border-color: #ef4444 !important;
            transform: scale(1.05);
            box-shadow: 0 5px 15px rgba(239, 68, 68, 0.2);
        }
        .fp-delete-btn i { font-size: 12px; }

        .fp-purge-btn {
            background: rgba(239, 68, 68, 0.15) !important;
            border: 1px solid rgba(239, 68, 68, 0.3) !important;
            color: #ef4444 !important;
            padding: 8px 16px !important;
            border-radius: 10px !important;
            font-size: 11px !important;
            font-weight: 900 !important;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
            transition: all 0.3s ease;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .fp-purge-btn:hover {
            background: rgba(239, 68, 68, 0.25) !important;
            box-shadow: 0 0 20px rgba(239, 68, 68, 0.2);
            transform: translateY(-1px);
        }
        .detection-avatar {
            width: 40px; height: 40px; border-radius: 10px;
            background: #1e293b; object-fit: cover;
            border: 1px solid rgba(255,255,255,0.1);
        }
        .detection-info { flex: 1; min-width: 0; }
        .detection-name { font-size: 12px; font-weight: 800; color: #fff; text-transform: uppercase; }
        .detection-time { font-size: 9px; opacity: 0.5; font-weight: 600; }
        .detection-status { 
            font-size: 8px; font-weight: 900; padding: 2px 6px; 
            border-radius: 4px; background: rgba(46, 204, 113, 0.2); color: #2ecc71;
            text-transform: uppercase;
        }

        /* 3. Futuristic Scanning Overlay */
        .scan-focus-box {
            position: absolute;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            width: 250px; height: 250px;
            border: 2px solid rgba(52, 152, 219, 0.3);
            border-radius: 40px;
            pointer-events: none;
            box-shadow: 0 0 0 1000px rgba(0,0,0,0.4);
        }
        .scan-corner {
            position: absolute; width: 30px; height: 30px;
            border: 4px solid #3498db;
        }
        .corner-tl { top: -2px; left: -2px; border-right: none; border-bottom: none; border-top-left-radius: 20px; }
        .corner-tr { top: -2px; right: -2px; border-left: none; border-bottom: none; border-top-right-radius: 20px; }
        .corner-bl { bottom: -2px; left: -2px; border-right: none; border-top: none; border-bottom-left-radius: 20px; }
        .corner-br { bottom: -2px; right: -2px; border-left: none; border-top: none; border-bottom-right-radius: 20px; }

        .laser-line {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 2px;
            background: linear-gradient(90deg, transparent, #3498db, transparent);
            box-shadow: 0 0 15px #3498db;
            animation: laserMove 2.5s infinite linear;
        }
        @keyframes laserMove { 
            0% { top: 10%; opacity: 0; }
            40% { opacity: 1; }
            60% { opacity: 1; }
            100% { top: 90%; opacity: 0; }
        }

        /* 4. Fingerprint Scanner Enhancements */
        .fingerprint-core {
            position: relative;
            z-index: 5;
            transition: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }
        .scanner-glow {
            position: absolute;
            inset: -20px;
            background: radial-gradient(circle, rgba(155, 89, 182, 0.2) 0%, transparent 70%);
            border-radius: 50%;
            animation: pulseGlow 2s infinite;
        }
        @keyframes pulseGlow {
            0% { transform: scale(0.8); opacity: 0.3; }
            50% { transform: scale(1.2); opacity: 0.6; }
            100% { transform: scale(0.8); opacity: 0.3; }
        }

        /* 5. Smart Settings Grid */
        .settings-card {
            background: rgba(255,255,255,0.02);
            border: 1px solid rgba(255,255,255,0.05);
            border-radius: 15px;
            padding: 15px;
            transition: 0.3s;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 15px;
        }
        .settings-card:hover { 
            background: rgba(255,255,255,0.05); 
            border-color: rgba(255,255,255,0.1);
        }
        .settings-icon {
            width: 40px; height: 40px; border-radius: 10px;
            background: rgba(255,255,255,0.05);
            display: flex; align-items: center; justify-content: center;
            font-size: 16px; color: var(--primary);
        }
        .settings-content { flex: 1; }
        .settings-title { font-size: 13px; font-weight: 800; color: #fff; margin-bottom: 2px; }
        .settings-desc { font-size: 10px; opacity: 0.5; font-weight: 600; }

        /* Theme Support for Cards */
        body.theme-tactile .settings-title { color: #000; }
        body.theme-tactile .settings-card {
            background: #f8fafc;
            border-color: rgba(0,0,0,0.05);
        }
        body.theme-tactile .detection-item {
            background: #f8fafc;
            border-color: rgba(0,0,0,0.05);
        }
        body.theme-tactile .detection-name { color: #000; }

        /* --- SMART ATTENDANCE LIGHT THEME FIXES --- */
        .smart-tabs {
            background: rgba(0,0,0,0.2);
            display: flex; gap: 15px; margin-bottom: 25px; padding: 5px; border-radius: 16px;
        }
        .smart-tabs .tab-btn {
            flex: 1; background: transparent; border: none; padding: 12px; border-radius: 12px; 
            color: rgba(255,255,255,0.6); font-weight: 800; font-size: 12px; letter-spacing: 0.5px; 
            transition: all 0.3s; display: flex; align-items: center; justify-content: center; gap: 8px;
        }
        
        body.theme-tactile .smart-tabs {
            background: #ffffff !important;
            border: 1px solid #000000 !important;
            box-shadow: 0 5px 15px rgba(0,0,0,0.05);
        }
        body.theme-tactile .smart-tabs .tab-btn {
            color: #000000 !important;
            opacity: 0.6;
        }
        body.theme-tactile .smart-tabs .tab-btn.active {
            opacity: 1;
            background: var(--primary) !important;
            color: #fff !important;
            box-shadow: 0 5px 15px rgba(52, 152, 219, 0.4);
        }

        /* Premium Card Light Theme */
        body.theme-tactile .premium-card {
            background: #ffffff !important;
            border: 1px solid #000000 !important;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1) !important;
        }
        body.theme-tactile .premium-card h2, body.theme-tactile .premium-card h2,
        body.theme-tactile .premium-card h3, body.theme-tactile .premium-card h3,
        body.theme-tactile .premium-card .text-dim, body.theme-tactile .premium-card .text-dim,
        body.theme-tactile .premium-card p, body.theme-tactile .premium-card p,
        body.theme-tactile .premium-card .biometric-title {
            color: #000000 !important;
        }
        body.theme-tactile .biometric-box {
            background: #f8fafc !important;
            border-color: rgba(0,0,0,0.1) !important;
        }
        body.theme-tactile .biometric-stat-label {
            color: #64748b !important;
        }
        body.theme-tactile .biometric-box {
            background: #f8fafc !important;
            border-color: rgba(0,0,0,0.1) !important;
        }
        body.theme-tactile .biometric-stat-label {
            color: #64748b !important;
        }
        body.theme-tactile .biometric-stat-value {
            color: #000 !important;
        }

        /* Settings Panel Styling */
        .settings-box {
            background: rgba(0,0,0,0.2);
            border-radius: 24px;
            border: 1px solid var(--border);
            padding: 25px;
        }
        body.theme-tactile .settings-box {
            background: #f8fafc !important;
            border-color: rgba(0,0,0,0.1) !important;
        }
        body.theme-tactile .text-dynamic {
            color: #1e293b !important;
        }
        body.theme-tactile .text-dynamic-dim {
            color: #64748b !important;
        }
        body.theme-tactile input[type="text"], body.theme-tactile input[type="password"], body.theme-tactile select, body.theme-tactile input[type="time"],
        body.theme-tactile textarea {
            color: #333 !important;
            background: #fff !important;
            border-color: #cbd5e1 !important;
        }

        /* Light Theme Button Text Overrides */
        body.theme-tactile .smart-panel .btn {
            color: #000 !important;
        }
        body.theme-tactile .smart-panel .btn i {
            color: inherit !important;
            opacity: 1 !important;
        }
        /* Ensure transparent buttons are visible in light mode */
        body.theme-tactile .smart-panel .btn:not(.theme-btn-3d):not(.force-active-btn) {
            background: rgba(0,0,0,0.05) !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
        }
        
        /* Smart Action Buttons Theme Logic */
        .smart-action-btn {
            background: rgba(255,255,255,0.04);
            transition: all 0.3s ease;
        }
        body.theme-tactile .smart-action-btn {
            background: #ffffff !important;
            border-color: rgba(0,0,0,0.1) !important;
            color: #000 !important;
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
        }
        body.theme-tactile .smart-action-btn span {
            color: #000 !important;
        }
        body.theme-tactile .smart-action-btn i {
            /* Keep original icon color if set, or inherit black */
        }
        
        /* Force specific button text overrides */
        body.theme-tactile .text-black-force, body.theme-tactile .text-black-force * {
            color: #000 !important; 
        }

        /* Glass Sidebar Light Theme */
        .glass-sidebar {
            background: rgba(0,0,0,0.2);
            border: 1px solid var(--border);
        }
        body.theme-tactile .glass-sidebar {
            background: #ffffff !important;
            border-color: rgba(0,0,0,0.1) !important;
        }
        .glass-header {
            background: rgba(255,255,255,0.02);
            border-bottom: 1px solid var(--border);
        }
        body.theme-tactile .glass-header {
            background: #f8fafc !important;
            border-bottom-color: rgba(0,0,0,0.1) !important;
        }
        body.theme-tactile .glass-sidebar .title-main {
            color: #000 !important;
        }

        /* Scan Mode Pill Styling */
        .mode-pill {
            transition: all 0.3s ease;
            cursor: pointer;
            border: 1px solid transparent;
        }
        .mode-pill:hover {
            background: rgba(255,255,255,0.1);
        }

        /* Wizard Button Styling */
        .btn-wizard {
            background: rgba(10, 20, 35, 0.8) !important;
            color: #ffffff !important;
            border: 1px solid rgba(255,255,255,0.1) !important;
            box-shadow: 0 4px 15px rgba(0,0,0,0.3) !important;
            transition: all 0.3s ease !important;
        }
        
        body.theme-tactile .btn-wizard, body.theme-white .btn-wizard {
            background: #ffffff !important;
            color: #000000 !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            box-shadow: 0 4px 12px rgba(0,0,0,0.05) !important;
        }

        .btn-wizard:hover {
            transform: translateY(-2px) !important;
            background: rgba(10, 20, 35, 1) !important;
            box-shadow: 0 8px 25px rgba(0,0,0,0.4) !important;
        }
        
        body.theme-tactile .btn-wizard:hover {
            background: #f8fafc !important;
            box-shadow: 0 8px 20px rgba(0,0,0,0.1) !important;
        }
    </style>
    <script>
        // Theme Toggle System
        const themes = ['theme-tactile', 'theme-indigo'];

        const INSTITUTION_NAME = <?php echo json_encode($inst['name']); ?>;
        const INSTITUTION_LOGO = <?php echo json_encode($inst['logo'] ? '../../assets/images/logos/'.$inst['logo'] : '../../assets/images/logo-placeholder.png'); ?>;
        const INSTITUTION_ADDRESS = <?php echo json_encode($inst['address'] ?? 'Official Academic Archive'); ?>;

        function toggleTheme() {
            const body = document.body;
            let currentIdx = themes.findIndex(t => body.classList.contains(t));
            if (currentIdx !== -1) body.classList.remove(themes[currentIdx]);
            
            let nextIdx = (currentIdx + 1) % themes.length;
            let nextTheme = themes[nextIdx];
            body.classList.add(nextTheme);
            localStorage.setItem('dashboard-theme', nextTheme);
            
            // Only target the specific theme toggle buttons
            const icons = document.querySelectorAll('#theme-toggle-btn i, #mobile-theme-btn i');
            icons.forEach(icon => {
                if (nextTheme === 'theme-tactile') icon.className = 'fas fa-palette';
                else if (nextTheme === 'theme-indigo') icon.className = 'fas fa-moon';
                icon.style.color = '#fff';
            });
            updateThemeColors(nextTheme);
        }

        function updateThemeColors(theme) {
            const metaThemeColor = document.querySelector('meta[name="theme-color"]');
            let color = '#f6f5f0';
            if (theme === 'theme-tactile') color = '#f6f5f0';
            else if (theme === 'theme-indigo') color = '#020617';
            if(metaThemeColor) metaThemeColor.setAttribute('content', color);

            // Specific adjustment for mobile banner items
            const mobileBanner = document.getElementById('mobile-top-banner');
            if(mobileBanner) {
                if(theme === 'theme-tactile') {
                    mobileBanner.style.background = '#f6f5f0';
                    mobileBanner.style.color = '#000';
                } else {
                    mobileBanner.style.background = 'rgba(2, 6, 23, 0.8)';
                    mobileBanner.style.color = '#fff';
                }
            }
        }

        document.addEventListener('DOMContentLoaded', () => {
            const savedTheme = localStorage.getItem('dashboard-theme') || 'theme-indigo';
            document.body.classList.add(savedTheme);
            
            const icons = document.querySelectorAll('#theme-toggle-btn i, #mobile-theme-btn i');
            icons.forEach(icon => {
                if (savedTheme === 'theme-tactile') icon.className = 'fas fa-palette';
                else if (savedTheme === 'theme-indigo') icon.className = 'fas fa-moon';
                icon.style.color = '#fff';
            });
            updateThemeColors(savedTheme);

            // Initialize Granular Access Control
            if (typeof AccessControl !== 'undefined') {
                const isAdmin = <?php echo json_encode(in_array($_SESSION['edu_role'] ?? '', ['admin', 'developer', 'super_admin'])); ?>;
                const isGuest = <?php echo json_encode($_SESSION['edu_role'] === 'guest'); ?>;
                const isSuper = <?php echo json_encode(in_array($_SESSION['edu_role'] ?? '', ['developer', 'super_admin'])); ?>;
                AccessControl.init(isAdmin, isGuest, isSuper);
            }
        });
    </script>
    <script src="../../assets/js/access_granular.js"></script>

</head>
<body>

    <?php include 'madrasa_lang_script_only.php'; ?>

     
    <!-- TOPMOST MOBILE BANNER (Mobile Only) -->
    <div id="mobile-top-banner" class="mobile-only" style="margin: 0 !important; width: 100% !important; left: 0; right: 0; padding: 10px 15px; display: flex; justify-content: space-between; align-items: center; border-radius: 0 0 24px 24px; border-top: none; position: fixed; top: 0; z-index: 99999; transition: all 0.3s ease; background: rgba(255, 255, 255, 0.8) !important; backdrop-filter: blur(15px); box-shadow: 0 10px 30px rgba(0,0,0,0.1); border-bottom: 1px solid rgba(0,0,0,0.1);">
        <!-- Mobile Back Button -->
        <button onclick="window.location.href='dashboard.php'" style="background: rgba(0, 0, 0, 0.05);  border: 1px solid rgba(0,0,0,0.1); width: 40px; height: 40px; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center;">
            <i class="fas fa-arrow-left" style="font-size: 16px; color: #334155 !important;"></i>
        </button>

        <!-- Smart Title -->
        <div class="smart-mobile-title" style="padding: 0; margin: 0; font-size: 15px; font-weight: 900; letter-spacing: 1px;">
            <span style="background: linear-gradient(135deg, #334155 0%, #64748b 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">SMART ATTENDANCE</span> <i class="fas fa-lock ml-1 access-control-icon" style="color:#f1c40f; font-size:12px;"></i>
        </div>

        <!-- Mobile Theme Button (Added ID) -->
        <button id="mobile-theme-btn" onclick="toggleTheme()" style="background: rgba(0, 0, 0, 0.05);  border: 1px solid rgba(0,0,0,0.1); width: 40px; height: 40px; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center;">
            <i class="fas fa-moon" style="font-size: 16px; color: #334155 !important;"></i>
        </button>
    </div>



    <!-- Spacer for fixed banner tracking -->
    <div class="mobile-only" style="height: 75px;"></div>
    
    <div class="main-board">
        <!-- TABS SECTION WITH 3D SPONGY DESIGN -->
        <div class="section-card" style="margin-bottom: 0 !important; padding: 15px;">
            <div class="main-tabs-grid" style="display: flex; gap: 12px; flex-wrap: wrap; align-items: stretch;">
                <!-- Integrated Back Button -->
                <div class="spongy-card desktop-only" onclick="window.location.href='dashboard.php'" style="width: 50px; display: flex; align-items: center; justify-content: center; cursor: pointer; background: rgba(255,255,255,0.05); position: relative;">
                    <i class="fas fa-arrow-left" style="font-size: 16px; opacity: 0.8;"></i>
                </div>

                <!-- Integrated Theme Toggle -->
                <div id="theme-toggle-btn" class="spongy-card desktop-only" onclick="toggleTheme()" style="width: 50px; display: flex; align-items: center; justify-content: center; cursor: pointer; background: rgba(255,255,255,0.05); position: relative;">
                    <i class="fas fa-palette" style="font-size: 16px; opacity: 0.8;"></i>
                </div>

                <div class="desktop-only" style="width: 1px; background: rgba(255,255,255,0.1); margin: 5px 0;"></div>

                
                <?php if (has_granular_access('att_tab_daily')): ?>
                <!-- Tab 1: Attendance -->
                <div class="spongy-card tab-btn active" data-access-id="att_tab_daily" onclick="switchTab('daily-view', this)" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                    <!-- Left Tube -->
                    <div class="hardware-tube left"></div>
                    <!-- Right Tube -->
                    <div class="hardware-tube right"></div>
                    
                    <i class="fas fa-user-edit" style="font-size: 16px;"></i>
                    <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Attendance</span>
                    <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                </div>
                <?php
endif; ?>



                <?php if (has_granular_access('att_tab_reports')): ?>
                <!-- Tab 2: Monthly Analysis -->
                <div class="spongy-card tab-btn" data-access-id="att_tab_reports" onclick="switchTab('analysis-view', this)" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                    <!-- Left Tube -->
                    <div class="hardware-tube left"></div>
                    <!-- Right Tube -->
                    <div class="hardware-tube right"></div>
                    
                    <i class="fas fa-chart-pie" style="font-size: 16px;"></i>
                    <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Monthly</span>
                    <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                </div>
                <?php
endif; ?>

                <?php if (in_array($_SESSION['edu_role'] ?? '', ['admin', 'developer', 'super_admin', 'guest'])): ?>
                <!-- Tab 3: School Rules -->
                <div class="spongy-card tab-btn" data-access-id="att_tab_policy" onclick="switchTab('policy-view', this)" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                    <!-- Left Tube -->
                    <div class="hardware-tube left"></div>
                    <!-- Right Tube -->
                    <div class="hardware-tube right"></div>
                    
                    <i class="fas fa-gavel" style="font-size: 16px;"></i>
                    <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;"> Rules</span>
                    <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                </div>

                <!-- Tab 4: Leave Appeals -->
                <div class="spongy-card tab-btn" onclick="switchTab('leave-appeals-view', this); loadLeaveAppeals();" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-envelope-open-text" style="font-size: 16px;"></i>
                    <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px; position:relative;"> L-appeals 
                        <?php if($pending_leaves_count > 0): ?>
                            <i class="fas fa-bell text-red-500 animate-bounce" style="margin-left:5px; margin-right:2px; font-size:12px;" title="<?php echo $pending_leaves_count; ?> pending requests"></i>
                            <span style="background:#e74c3c; color:white; padding:2px 6px; border-radius:10px; font-size:9px; font-weight:900; vertical-align:middle; box-shadow: 0 2px 4px rgba(231, 76, 60, 0.4);"><?php echo $pending_leaves_count; ?></span>
                        <?php endif; ?>
                    </span>
                </div>

                <?php else: ?>
                <!-- Tab: My Leaves (For Staff/Teacher) -->
                <div class="spongy-card tab-btn" onclick="switchTab('my-leaves-view', this); loadMyLeaves();" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-history" style="font-size: 16px;"></i>
                    <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;"> My Leaves</span>
                </div>
                <!-- Request Leave Button for Staff -->
                <div class="spongy-card tab-btn" onclick="document.getElementById('leaveModal').classList.remove('hidden')" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease; border: 1px solid rgba(255, 255, 255, 0.2) !important; background: rgba(52, 152, 219, 0.1) !important;">
                    <div class="hardware-tube left"></div>
                    <div class="hardware-tube right"></div>
                    <i class="fas fa-calendar-plus" style="font-size: 16px; color: #3498db;"></i>
                    <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px; color: #3498db;"> Request Leave</span>
                </div>


                <?php endif; ?>

            </div>
        </div>
        <!-- CONTENT SECTIONS -->
        <div class="w-full" style="margin-top: 0 !important;">
            <!-- 1. Attendance View -->
            <div id="daily-view" class="content-sec active">
                <div class="section-card triple-tab-row" style="display:flex; gap:12px; margin-top: 0 !important; margin-bottom: 20px; width:100%; flex-wrap: wrap; padding: 15px;">
                    <style>
                        @media (max-width: 768px) {
                            .w-full-mobile { width: 100% !important; flex: 1 1 100% !important; min-width: 100% !important; }
                        }
                    </style>
                    <div class="spongy-card sub-tab-btn sub-tab-btn-compact active w-full-mobile" data-access-id="att_sub_students" onclick="switchAttendanceSubTab('students-att-panel', this)" style="flex: 1; min-width: 140px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                         <!-- Left Tube -->
                        <div class="hardware-tube left"></div>
                        <!-- Right Tube -->
                        <div class="hardware-tube right"></div>
                        
                        <i class="fas fa-user-graduate" style="font-size: 16px;"></i> 
                        <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Students</span>
                        <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                    </div>
                    <div class="spongy-card sub-tab-btn sub-tab-btn-compact w-full-mobile" data-access-id="att_sub_staff" onclick="switchAttendanceSubTab('staff-att-panel', this)" style="flex: 1; min-width: 140px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                        <!-- Left Tube -->
                        <div class="hardware-tube left"></div>
                        <!-- Right Tube -->
                        <div class="hardware-tube right"></div>
                        
                        <i class="fas fa-user-tie" style="font-size: 16px;"></i> 
                        <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;"><?php echo $is_staff ? 'My Attendance' : 'Staff'; ?></span>
                        <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                    </div>

                    <!-- Manual Marking (Responsive width) -->
                    <div class="spongy-card sub-tab-btn manual-tab-btn w-full-mobile" data-access-id="att_sub_manual" onclick="switchAttendanceSubTab('manual-att-panel', this)" style="min-width: 140px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                        <!-- Left Tube -->
                        <div class="hardware-tube left"></div>
                        <!-- Right Tube -->
                        <div class="hardware-tube right"></div>
                        
                        <i class="fas fa-edit" style="font-size: 16px;"></i> 
                        <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Manual</span>
                        <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                    </div>

                    <!-- Smart Attendance -->
                    <div class="spongy-card sub-tab-btn manual-tab-btn w-full-mobile" data-access-id="att_sub_smart" onclick="switchAttendanceSubTab('smart-att-panel', this)" style="min-width: 140px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                        <!-- Left Tube -->
                        <div class="hardware-tube left"></div>
                        <!-- Right Tube -->
                        <div class="hardware-tube right"></div>
                        
                        <i class="fas fa-fingerprint" style="font-size: 16px;"></i> 
                        <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Smart Attendance</span>
                        <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                    </div>
                </div>

                <!-- Section A: Students -->
                <div id="students-att-panel" class="attendance-panel active">
                    <div class="filter-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                            <label style="font-size:0.75rem; color:var(--primary); display:block; margin-bottom:0px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">Students Attendance</label>
                            <div style="display: flex; gap: 10px;">
                                <button onclick="openPHModal()" type="button" style="padding: 6px 12px; font-size: 10px; background: rgba(155, 89, 182, 0.1); border: 1px solid #9b59b6; color: #9b59b6 !important; border-radius: 8px; font-weight: 800; display: flex; align-items: center; gap: 5px; cursor: pointer;">
                                    <i class="fas fa-calendar-day"></i> PUBLIC HOLIDAY
                                </button>
                                <?php if (has_granular_access('att_submission_status')): ?>
                                <a href="attendance_submission_status.php" data-access-id="att_submission_status" style="padding: 6px 12px; font-size: 10px; background: rgba(16, 185, 129, 0.1); border: 1px solid #10b981; color: #10b981 !important; border-radius: 8px; text-decoration: none; font-weight: 800; display: flex; align-items: center; gap: 5px;">
                                    <i class="fas fa-tasks"></i> SUBMISSION STATUS
                                    <i class="fas fa-lock access-control-icon" style="margin-left: 5px; font-size: 10px; color: #f1c40f;"></i>
                                </a>
                                <?php endif; ?>
                            </div>
                        </div>
                        <div style="display:flex; align-items:center; gap:15px; flex-wrap: nowrap;">
                            <!-- 1. Date Picker (First) -->
                            <div class="calendar-wrapper" style="flex:0 0 auto; margin-top: 0 !important;">
                                <input type="hidden" id="daily_date" value="<?php echo date('Y-m-d'); ?>">
                                <div class="calendar-trigger" style="height: 50px !important; display: flex; align-items: center; background: rgba(0,0,0,0.03) !important; border: 1px solid rgba(0,0,0,0.1) !important; color: var(--text-main) !important;">
                                    <div style="display:flex; align-items:center; gap:10px;">
                                        <div class="capsule-3d" style="width:30px; height:30px; padding:0; background:transparent; color:var(--text-main); box-shadow: none;"> <i class="fas fa-calendar-alt" style="font-size:18px;"></i></div>
                                        <div>
                                            <span style="font-size:10px; color:var(--text-dim); display:block; line-height:1; font-weight:700; text-transform:uppercase;">Selected Date</span>
                                            <span id="display_date_text" style="font-size:14px; font-weight:800; color:var(--text-main); display:block; margin-top:3px;"><?php echo date('d M Y'); ?></span>
                                        </div>
                                    </div>
                                    <i class="fas fa-chevron-down" style="font-size:12px; opacity:0.5; margin-left: auto; color: var(--text-main) !important;"></i>
                                </div>
                                
                                <div class="calendar-dropdown" id="student_cal_dropdown" data-month="<?php echo date('n'); ?>" data-year="<?php echo date('Y'); ?>">
                                    <h4 style="margin:0 0 10px; text-align:center; font-weight:900;  text-transform:uppercase; letter-spacing:1px; font-size:14px; padding-bottom:10px; border-bottom:1px solid #e2e8f0;">
                                        <span class="cal-month" style="cursor:pointer;" onclick="toggleCalendarView('student_cal_dropdown', 'month')"><?php echo date('F'); ?></span> 
                                        <span class="cal-year" style="cursor:pointer;" onclick="toggleCalendarView('student_cal_dropdown', 'year')"><?php echo date('Y'); ?></span>
                                    </h4>
                                    
                                    <div class="date-view">
                                        <div class="date-grid">
                                            <?php
$days = date('t');
$today = date('j');
for ($d = 1; $d <= $days; $d++) {
    $isToday = ($d == $today) ? 'today' : '';
    echo "<div class='date-cell $isToday' onclick=\"selectAttendanceDate($d)\">$d</div>";
}
?>
                                        </div>
                                    </div>

                                    <div class="month-view" style="display:none;">
                                        <div class="month-year-grid">
                                            <?php
$months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
foreach ($months as $idx => $m) {
    $m_num = $idx + 1;
    $active = ($m_num == date('n')) ? 'active' : '';
    echo "<div class='select-item $active' onclick=\"changeCalMonth('student_cal_dropdown', $m_num, '$m')\">$m</div>";
}
?>
                                        </div>
                                    </div>

                                    <div class="year-view" style="display:none;">
                                        <div class="month-year-grid">
                                            <?php
$curYear = date('Y');
for ($y = $curYear - 2; $y <= $curYear + 2; $y++) {
    $active = ($y == $curYear) ? 'active' : '';
    echo "<div class='select-item $active' onclick=\"changeCalYear('student_cal_dropdown', $y)\">$y</div>";
}
?>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- 2. Selection Dropdown (Second) -->
                            <div style="flex:1;">
                                <select id="daily_class_sel" style="width:100%; height:50px; border:1px solid rgba(0,0,0,0.1) !important; font-weight: 700; box-shadow: inset 0 2px 5px rgba(0,0,0,0.05) !important;" onchange="loadAdminDailyStudents()">
                                    <option value="">Choose Class...</option>
                                    <?php
$opt_count = 0;
$last_val = "";
foreach ($all_classes as $c): ?>
                                        <?php foreach ($c['sections'] as $s):
        $val = $c['id'] . '|' . $s['id'];
        $last_val = $val;
        $opt_count++;
?>
                                            <option value="<?php echo $val; ?>"><?php echo $c['name'] . ' ' . $s['name']; ?></option>
                                        <?php
    endforeach; ?>
                                    <?php
endforeach; ?>
                                </select>
                            </div>

                            <!-- 3. Search Box (Third) -->
                            <div style="flex:1; display:flex; align-items:center; gap:10px;">
                                <input type="text" id="student_search" style="flex:1; height:50px; font-size:14px; padding:0 15px; border:1px solid rgba(0,0,0,0.1);  border-radius:12px; font-weight: 700; box-shadow: inset 0 2px 5px rgba(0,0,0,0.05) !important;" placeholder="Search Student..." oninput="filterStudents()">
                            </div>

                            <!-- 4. Time In/Out Bulk "Banner" Controls -->
                            <div style="flex:1.5; display:flex; align-items:center;">
                                <div class="premium-card" style="width:100%; display:flex; gap:0; padding:4px; background:rgba(255,255,255,0.03); border-radius:14px; border:1px solid rgba(255,255,255,0.08); overflow:hidden;">
                                    <div style="flex:1; padding:5px 12px; border-right:1px solid rgba(255,255,255,0.05);">
                                        <div style="display:flex; align-items:center; gap:6px; margin-bottom:1px;">
                                            <input type="radio" id="mode_std_bulk_in" name="std_bulk_mode" checked onclick="syncBulkMode('student', 'in')" style="width:12px; height:12px; cursor:pointer; accent-color:var(--primary);">
                                            <label for="mode_std_bulk_in" style="font-size:8px; font-weight:900; opacity:0.5; color:var(--primary); letter-spacing:0.5px; cursor:pointer;">DEFAULT IN</label>
                                        </div>
                                        <input type="time" id="bulk_student_time_in" value="08:00" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                    </div>
                                    <div style="flex:1; padding:5px 12px;">
                                        <div style="display:flex; align-items:center; gap:6px; margin-bottom:1px;">
                                            <input type="radio" id="mode_std_bulk_out" name="std_bulk_mode" onclick="syncBulkMode('student', 'out')" style="width:12px; height:12px; cursor:pointer; accent-color:var(--danger);">
                                            <label for="mode_std_bulk_out" style="font-size:8px; font-weight:900; opacity:0.5; color:var(--danger); letter-spacing:0.5px; cursor:pointer;">DEFAULT OUT</label>
                                        </div>
                                        <input type="time" id="bulk_student_time_out" value="13:30" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                    </div>
                                </div>
                            </div>
                        </div>


                        <script>
                            // Auto-fill bulk times to individual rows
                            document.addEventListener('change', function(e) {
                                if (e.target.id === 'bulk_student_time_in') {
                                    document.querySelectorAll('#student-marking-container .time-in-input').forEach(input => {
                                        if(!input.value) input.value = e.target.value;
                                    });
                                } else if (e.target.id === 'bulk_student_time_out') {
                                    document.querySelectorAll('#student-marking-container .time-out-input').forEach(input => {
                                        if(!input.value) input.value = e.target.value;
                                    });
                                } else if (e.target.id === 'bulk_staff_time_in') {
                                    document.querySelectorAll('#staff-result .time-in-input').forEach(input => {
                                        if(!input.value) input.value = e.target.value;
                                    });
                                } else if (e.target.id === 'bulk_staff_time_out') {
                                    document.querySelectorAll('#staff-result .time-out-input').forEach(input => {
                                        if(!input.value) input.value = e.target.value;
                                    });
                                }
                            });
                        </script>

                    </div>

                    <div id="student-marking-container"></div>
                </div>

                <!-- Section B: Staff -->
                <div id="staff-att-panel" class="attendance-panel" style="display:none;">
                    <!-- Staff Filter Card -->
                    <div class="filter-card">
                        <label style="font-size:0.75rem; color:var(--primary); display:block; margin-bottom:12px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">Staff Attendance</label>
                        
                        <div style="display:flex; align-items:center; gap:15px; flex-wrap: nowrap;">
                            <!-- 1. Date Picker (First) -->
                            <div class="calendar-wrapper" style="flex:0 0 auto; margin-top: 0 !important;">
                                <input type="hidden" id="staff_date" value="<?php echo date('Y-m-d'); ?>">
                                <div class="calendar-trigger" style="height: 50px !important; display: flex; align-items: center; background: rgba(0,0,0,0.03) !important; border: 1px solid rgba(0,0,0,0.1) !important; color: var(--text-main) !important;">
                                    <div style="display:flex; align-items:center; gap:10px;">
                                        <div class="capsule-3d" style="width:30px; height:30px; padding:0; background:transparent; color:var(--text-main); box-shadow: none;"> <i class="fas fa-calendar-alt" style="font-size:18px;"></i></div>
                                        <div>
                                            <span style="font-size:10px; color:var(--text-dim); display:block; line-height:1; font-weight:700; text-transform:uppercase;">Selected Date</span>
                                            <span id="staff_display_date_text" style="font-size:14px; font-weight:800; color:var(--text-main); display:block; margin-top:3px;"><?php echo date('d M Y'); ?></span>
                                        </div>
                                    </div>
                                    <i class="fas fa-chevron-down" style="font-size:12px; opacity:0.5; margin-left: auto; color: var(--text-main) !important;"></i>
                                </div>
                                
                                <div class="calendar-dropdown" id="staff_cal_dropdown" data-month="<?php echo date('n'); ?>" data-year="<?php echo date('Y'); ?>">
                                    <h4 style="margin:0 0 10px; text-align:center; font-weight:900;  text-transform:uppercase; letter-spacing:1px; font-size:14px; padding-bottom:10px; border-bottom:1px solid #e2e8f0;">
                                        <span class="cal-month" style="cursor:pointer;" onclick="toggleCalendarView('staff_cal_dropdown', 'month')"><?php echo date('F'); ?></span> 
                                        <span class="cal-year" style="cursor:pointer;" onclick="toggleCalendarView('staff_cal_dropdown', 'year')"><?php echo date('Y'); ?></span>
                                    </h4>
                                    
                                    <div class="date-view">
                                        <div class="date-grid">
                                            <?php
$days = date('t');
$today = date('j');
for ($d = 1; $d <= $days; $d++) {
    $isToday = ($d == $today) ? 'today' : '';
    echo "<div class='date-cell $isToday' onclick=\"selectStaffDate($d)\">$d</div>";
}
?>
                                        </div>
                                    </div>

                                    <div class="month-view" style="display:none;">
                                        <div class="month-year-grid">
                                            <?php
foreach ($months as $idx => $m) {
    $m_num = $idx + 1;
    $active = ($m_num == date('n')) ? 'active' : '';
    echo "<div class='select-item $active' onclick=\"changeCalMonth('staff_cal_dropdown', $m_num, '$m')\">$m</div>";
}
?>
                                        </div>
                                    </div>

                                    <div class="year-view" style="display:none;">
                                        <div class="month-year-grid">
                                            <?php
for ($y = $curYear - 2; $y <= $curYear + 2; $y++) {
    $active = ($y == $curYear) ? 'active' : '';
    echo "<div class='select-item $active' onclick=\"changeCalYear('staff_cal_dropdown', $y)\">$y</div>";
}
?>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <?php if (!$is_staff): ?>
                            <!-- 2. Selection Dropdown (Second) -->
                            <div style="flex:1;">
                                <select id="staff_category_filter" style="width:100%; height:50px; border:1px solid rgba(0,0,0,0.1) !important; font-weight: 700; box-shadow: inset 0 2px 5px rgba(0,0,0,0.05) !important;" onchange="filterStaff()">
                                    <option value="">All Staff</option>
                                    <option value="teaching">Teaching Staff</option>
                                    <option value="non_teaching">Non-Teaching Staff</option>
                                    <option value="visiting">Visiting Staff</option>
                                </select>
                            </div>

                            <div style="position:relative; flex:1;">
                                <input type="text" id="staff_search" style="width:100%; height:50px; font-size:14px; padding:0 15px; border:1px solid rgba(0,0,0,0.1);  border-radius:12px; font-weight: 700; box-shadow: inset 0 2px 5px rgba(0,0,0,0.05) !important;" placeholder="Search Staff..." oninput="filterStaff()">
                            </div>
                            <?php
else: ?>
                            <div style="flex:1; display:flex; align-items:center; gap:10px; background:rgba(0,0,0,0.05); padding:10px 20px; border-radius:12px; border:1px solid rgba(0,0,0,0.1);">
                                <i class="fas fa-user-circle" style="font-size:20px; opacity:0.7;"></i>
                                <span style="font-weight:900; font-size:15px;"><?php echo $_SESSION['edu_name']; ?></span>
                                <span style="margin-left:auto; font-size:10px; font-weight:900; background:rgba(255,255,255,0.1); padding:4px 8px; border-radius:10px; text-transform:uppercase; letter-spacing:1px; opacity:0.6;">Personal Ledger</span>
                            </div>
                            <?php
endif; ?>
                            <!-- 4. Time In/Out Bulk "Banner" Controls -->
                            <div style="flex:1.5; display:flex; align-items:center;">
                                <div class="premium-card" style="width:100%; display:flex; gap:0; padding:4px; background:rgba(255,255,255,0.03); border-radius:14px; border:1px solid rgba(255,255,255,0.08); overflow:hidden;">
                                    <div style="flex:1; padding:5px 12px; border-right:1px solid rgba(255,255,255,0.05);">
                                        <div style="display:flex; align-items:center; gap:6px; margin-bottom:1px;">
                                            <input type="radio" id="mode_staff_bulk_in" name="staff_bulk_mode" checked onclick="syncBulkMode('staff', 'in')" style="width:12px; height:12px; cursor:pointer; accent-color:var(--primary);">
                                            <label for="mode_staff_bulk_in" style="font-size:8px; font-weight:900; opacity:0.5; color:var(--primary); letter-spacing:0.5px; cursor:pointer;">DEFAULT IN</label>
                                        </div>
                                        <input type="time" id="bulk_staff_time_in" value="08:00" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                    </div>
                                    <div style="flex:1; padding:5px 12px;">
                                        <div style="display:flex; align-items:center; gap:6px; margin-bottom:1px;">
                                            <input type="radio" id="mode_staff_bulk_out" name="staff_bulk_mode" onclick="syncBulkMode('staff', 'out')" style="width:12px; height:12px; cursor:pointer; accent-color:var(--danger);">
                                            <label for="mode_staff_bulk_out" style="font-size:8px; font-weight:900; opacity:0.5; color:var(--danger); letter-spacing:0.5px; cursor:pointer;">DEFAULT OUT</label>
                                        </div>
                                        <input type="time" id="bulk_staff_time_out" value="13:30" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div id="staff-result-container" style="margin-top:20px;">
                        <div id="staff-result"></div>
                    </div>
                </div>
                
                <!-- 3.5 Manual Attendance View (Moved inside Daily View to keep tabs visible) -->
                <div id="manual-att-panel" class="attendance-panel" style="display:none;">
                     <!-- Sub Tabs for Manual -->
                     <div class="section-card sub-tabs" style="display:flex; gap:12px; margin-top: 0 !important; margin-bottom: 20px; width:100%; flex-wrap: wrap; padding: 15px;">
                        <div class="spongy-card sub-tab-btn active" data-access-id="att_manual_students" onclick="switchManualSubTab('manual-student-panel', this, 'att_manual_students')" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                             <div class="hardware-tube left"></div>
                            <div class="hardware-tube right"></div>
                            <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                            <i class="fas fa-user-graduate" style="font-size: 16px;"></i> 
                            <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Students</span>
                        </div>
                        <div class="spongy-card sub-tab-btn" data-access-id="att_manual_staff" onclick="switchManualSubTab('manual-staff-panel', this, 'att_manual_staff')" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                            <div class="hardware-tube left"></div>
                            <div class="hardware-tube right"></div>
                            <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                            <i class="fas fa-user-tie" style="font-size: 16px;"></i> 
                            <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;"><?php echo $is_staff ? 'My Attendance' : 'Staff'; ?></span>
                        </div>
                        <div class="spongy-card sub-tab-btn" data-access-id="att_manual_bulk" onclick="switchManualSubTab('manual-bulk-panel', this, 'att_manual_bulk')" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                            <div class="hardware-tube left"></div>
                            <div class="hardware-tube right"></div>
                            <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                            <i class="fas fa-cubes" style="font-size: 16px;"></i> 
                            <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Bulk</span>
                        </div>
                    </div>

                    <div id="manual-student-panel" class="manual-panel active">
                        <div class="filter-card">
                            <label style="font-size:0.75rem; color:var(--text-main); display:block; margin-bottom:12px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">MANUAL STUDENT ATTENDANCE</label>
                            <div class="manual-filters-grid">
                                <!-- Date Input -->
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">DATE</label>
                                    <input type="date" id="manual_std_date" value="<?php echo date('Y-m-d'); ?>" style="width:100%; height:50px; padding:0 15px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1);">
                                </div>
                                
                                <!-- Class Select -->
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">CLASS</label>
                                    <select id="manual_class_sel" style="width:100%; height:50px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1); padding:0 15px;">
                                        <option value="">Choose Class...</option>
                                        <?php
foreach ($all_classes as $c):
    foreach ($c['sections'] as $s):
        $val = $c['id'] . '|' . $s['id'];
?>
                                                <option value="<?php echo $val; ?>"><?php echo $c['name'] . ' ' . $s['name']; ?></option>
                                            <?php
    endforeach;
endforeach; ?>
                                    </select>
                                </div>
                                <!-- Premium Banner for Manual Time -->
                                <div style="grid-column: span 2;">
                                    <div class="premium-card" style="display:flex; gap:0; padding:4px; background:rgba(255,255,255,0.03); border-radius:14px; border:1px solid rgba(255,255,255,0.08); overflow:hidden;">
                                        <div style="flex:1; padding:5px 12px; border-right:1px solid rgba(255,255,255,0.05);">
                                            <label style="font-size:8px; font-weight:900; display:block; margin-bottom:1px; opacity:0.5; color:var(--primary); letter-spacing:0.5px;">MANUAL IN</label>
                                            <input type="time" id="manual_std_time_in" value="08:00" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                        </div>
                                        <div style="flex:1; padding:5px 12px;">
                                            <label style="font-size:8px; font-weight:900; display:block; margin-bottom:1px; opacity:0.5; color:var(--danger); letter-spacing:0.5px;">MANUAL OUT</label>
                                            <input type="time" id="manual_std_time_out" value="13:30" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                        </div>
                                    </div>
                                </div>

                                <button class="btn" onclick="openManualStudentModal()" style="font-weight:900; color: #000 !important;">LOAD RECORDS</button>
                            </div>
                        </div>
                    </div>

                    <!-- B: Manual Staff -->
                    <div id="manual-staff-panel" class="manual-panel" style="display:none;">
                        <div class="filter-card">
                             <label style="font-size:0.75rem; color:var(--text-main); display:block; margin-bottom:12px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">MANUAL STAFF ATTENDANCE</label>
                             <div class="manual-filters-grid">
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">DATE</label>
                                    <input type="date" id="manual_staff_date" value="<?php echo date('Y-m-d'); ?>" style="width:100%; height:50px; padding:0 15px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1);">
                                </div>
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">STAFF TYPE</label>
                                    <select id="manual_staff_type" style="width:100%; height:50px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1); padding:0 15px;">
                                        <option value="all">All Staff</option>
                                        <option value="teaching">Teaching Staff</option>
                                        <option value="non-teaching">Non-Teaching Staff</option>
                                        <option value="visiting">Visiting Staff</option>
                                    </select>
                                </div>
                                <!-- Premium Banner for Manual Staff Time -->
                                <div style="grid-column: span 2;">
                                    <div class="premium-card" style="display:flex; gap:0; padding:4px; background:rgba(255,255,255,0.03); border-radius:14px; border:1px solid rgba(255,255,255,0.08); overflow:hidden;">
                                        <div style="flex:1; padding:5px 12px; border-right:1px solid rgba(255,255,255,0.05);">
                                            <label style="font-size:8px; font-weight:900; display:block; margin-bottom:1px; opacity:0.5; color:var(--primary); letter-spacing:0.5px;">DEFAULT staff IN</label>
                                            <input type="time" id="manual_staff_time_in" value="08:00" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                        </div>
                                        <div style="flex:1; padding:5px 12px;">
                                            <label style="font-size:8px; font-weight:900; display:block; margin-bottom:1px; opacity:0.5; color:var(--danger); letter-spacing:0.5px;">DEFAULT staff OUT</label>
                                            <input type="time" id="manual_staff_time_out" value="13:30" style="width:100%; height:26px !important; background:transparent !important; border:none !important; border-radius: 0 !important; font-weight:900; padding:0 !important; font-size:14px !important; color:inherit !important; outline:none !important; box-shadow:none !important;">
                                        </div>
                                    </div>
                                </div>

                                <button class="btn" onclick="openManualStaffModal()" style="font-weight:900; color: #000 !important;">LOAD RECORDS</button>
                             </div>
                        </div>
                    </div>

                    <!-- C: Manual Bulk -->
                    <div id="manual-bulk-panel" class="manual-panel" style="display:none;">
                        <div class="filter-card">
                             <label style="font-size:0.75rem; color:var(--text-main); display:block; margin-bottom:12px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">BULK ATTENDANCE PROCESSING</label>
                             <div class="manual-filters-grid" style="grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end;">
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">FROM DATE</label>
                                    <input type="date" id="manual_bulk_from" value="<?php echo date('Y-m-d'); ?>" style="width:100%; height:50px; padding:0 15px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1);">
                                </div>
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">TO DATE</label>
                                    <input type="date" id="manual_bulk_to" value="<?php echo date('Y-m-d'); ?>" style="width:100%; height:50px; padding:0 15px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1);">
                                </div>
                                <div style="position:relative;">
                                    <label style="font-size:10px; font-weight:700; margin-bottom:5px; display:block;">TARGET CLASS</label>
                                    <select id="manual_bulk_class" style="width:100%; height:50px; font-weight:700; border-radius:12px; border:1px solid rgba(0,0,0,0.1); padding:0 15px;">
                                        <option value="all">All Classes</option>
                                        <?php
foreach ($all_classes as $c):
    foreach ($c['sections'] as $s):
        $val = $c['id'] . '|' . $s['id'];
?>
                                                <option value="<?php echo $val; ?>"><?php echo $c['name'] . ' ' . $s['name']; ?></option>
                                            <?php
    endforeach;
endforeach; ?>
                                    </select>
                                </div>
                                <div style="display: flex; gap: 8px;">
                                    <button class="btn btn-wizard" data-access-id="att_manual_bulk_wizard" onclick="openAttendanceBulkModal()" title="Export/Import Bulk" style="flex: 1; height: 50px; display: flex; align-items: center; justify-content: center; border-radius: 12px; cursor: pointer;">
                                        <i class="fas fa-lock access-control-icon" style="font-size: 10px; margin-right: 8px; opacity: 0.5;"></i>
                                        <i class="fas fa-file-excel" style="margin-right: 10px; color: inherit !important;"></i> EXCEL ATTENDANCE WIZARD
                                    </button>
                                </div>
                             </div>
                        </div>
                    </div>
                </div>

                <!-- Smart Attendance Panel -->
                <div id="smart-att-panel" class="attendance-panel" style="display:none;">
                    <!-- Sub Tabs for Smart Attendance -->
                    <div class="section-card sub-tabs" style="display:flex; gap:12px; margin-top: 0 !important; margin-bottom: 20px; width:100%; flex-wrap: wrap; padding: 15px;">
                        <div class="spongy-card sub-tab-btn active" data-access-id="att_smart_face" onclick="switchSmartSubTab('face-recognition-panel', this, 'att_smart_face')" style="flex: 1; min-width: 180px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                            <div class="hardware-tube left"></div>
                            <div class="hardware-tube right"></div>
                            <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                            <i class="fas fa-smile" style="font-size: 16px;"></i> 
                            <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Face Recognition</span>
                        </div>
                        <div class="spongy-card sub-tab-btn" data-access-id="att_smart_finger" onclick="switchSmartSubTab('fingerprint-panel', this, 'att_smart_finger')" style="flex: 1; min-width: 180px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                            <div class="hardware-tube left"></div>
                            <div class="hardware-tube right"></div>
                            <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                            <i class="fas fa-fingerprint" style="font-size: 16px;"></i> 
                            <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Fingerprint</span>
                        </div>
                        <?php if (!$is_staff): ?>
                        <div class="spongy-card sub-tab-btn" data-access-id="att_smart_settings" onclick="switchSmartSubTab('smart-settings-panel', this, 'att_smart_settings')" style="flex: 1; min-width: 180px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                            <div class="hardware-tube left"></div>
                            <div class="hardware-tube right"></div>
                            <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                            <i class="fas fa-cog" style="font-size: 16px;"></i> 
                            <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Settings</span>
                        </div>
                        <?php
endif; ?>
                    </div>

                    <!-- A: Face Recognition Panel -->
                    <div id="face-recognition-panel" class="smart-panel active">
                        <div class="premium-card" style="padding: 20px !important; margin-bottom: 20px;">
                            <div style="display: flex; gap: 30px; min-height: 550px; flex-wrap: wrap;">
                                
                                <!-- LEFT COLUMN: Detected Personnel (Glass List) -->
                                <div class="glass-sidebar" style="flex: 0 0 350px; display: flex; flex-direction: column; border-radius: 24px; overflow: hidden;">
                                    <div class="glass-header" style="padding: 20px; display: flex; justify-content: space-between; align-items: center;">
                                        <div>
                                        </div>
                                        <div style="display: flex; align-items: center; gap: 10px;">
                                            <div id="browser-ai-toggle" onclick="toggleBrowserAI()" style="cursor: pointer; background: rgba(255,255,255,0.05); padding: 6px 12px; border-radius: 12px; font-size: 10px; font-weight: 800; border: 1px solid rgba(255,255,255,0.1); color: #fff; display: flex; align-items: center; gap: 6px;">
                                                <i class="fas fa-microchip"></i>
                                                BROWSER AI: <span id="browser-ai-status">ON</span>
                                            </div>
                                            <a href="../../AttendanceHub_Desktop.tar" download style="text-decoration: none; background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%); color: #fff; padding: 6px 12px; border-radius: 12px; font-size: 10px; font-weight: 800; display: flex; align-items: center; gap: 6px; box-shadow: 0 4px 10px rgba(99,102,241,0.3);">
                                                <i class="fas fa-desktop"></i>
                                                DESKTOP HUB
                                            </a>
                                            <div id="face-live-status" style="background: rgba(239, 68, 68, 0.2); color: #f87171; padding: 6px 12px; border-radius: 12px; font-size: 10px; font-weight: 800; border: 1px solid rgba(239, 68, 68, 0.3); display: flex; align-items: center; gap: 6px;">
                                                <div style="width: 8px; height: 8px; background: #ef4444; border-radius: 50%; box-shadow: 0 0 10px #ef4444;"></div>
                                                OFFLINE
                                            </div>
                                        </div>
                                    </div>

                                    <div style="flex: 1; overflow-y: auto; padding: 20px; scrollbar-width: thin; scrollbar-color: var(--primary) transparent;">
                                        <div id="knownFacesContainer">
                                            <div style="text-align: center; padding: 40px 20px; opacity: 0.5;">
                                                <i class="fas fa-radar" style="font-size: 30px; margin-bottom: 15px; display: block;"></i>
                                                <p style="font-size: 12px; font-weight: 600;">System standby. Awaiting hardware boot...</p>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <div style="padding: 15px; background: rgba(99, 102, 241, 0.05); border-top: 1px solid var(--border); text-align: center;">
                                        <span style="font-size: 10px; font-weight: 700; color: var(--primary); letter-spacing: 0.5px;">SECURED BY AI INTELLIGENCE V4.0</span>
                                    </div>
                                </div>

                                <!-- RIGHT COLUMN: Vision Core & Controls -->
                                <div style="flex: 1; min-width: 450px; display: flex; flex-direction: column; gap: 25px;">
                                    
                                    <!-- Vision Container - Clean View -->
                                    <div style="position: relative; background: transparent; border-radius: 20px; overflow: hidden; height: 420px; width: 100%; border: 1px solid var(--border);">
                                        <!-- Video Stream -->
                                        <img id="faceVideoDisplay" src="" style="width: 100%; height: 100%; object-fit: cover; display: none; border-radius: 20px;" alt="Vision Feed">
                                        
                                        <!-- JS Canvas for Detections (Hidden if unwanted, but kept for logic) -->
                                        <canvas id="faceOverlay" style="position: absolute; top:0; left:0; width: 100%; height: 100%; pointer-events: none; z-index: 5;"></canvas>

                                        <!-- System Load UI -->
                                         <div id="faceVideoPlaceholder" style="position: absolute; inset:0; display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 10; border-radius: 20px;">
                                            <div style="position: relative; width: 60px; height: 60px;">
                                                <div class="spinner-track" style="position: absolute; inset: 0; border: 4px solid rgba(0,0,0,0.05); border-radius: 50%;"></div>
                                                <div style="position: absolute; inset: 0; border: 4px solid var(--primary); border-radius: 50%; border-top-color: transparent; animation: spin 1s linear infinite;"></div>
                                            </div>
                                            <h3 style="margin-top: 20px; font-size: 13px; font-weight: 800; letter-spacing: 1px;">CAMERA STARTING...</h3>
                                            <p id="faceLoaderMsg" style="font-size: 11px; font-weight: 600; opacity: 0.6; margin-top: 5px;">Initializing video feed...</p>
                                        </div>
                                    </div>

                                    <!-- Action Dashboard -->
                                    <div style="display: grid; grid-template-columns: 1fr; gap: 15px;">
                                        <!-- Unified Controls for Face Recognition -->
                                        <div style="background: rgba(0,0,0,0.02); border-radius: 12px; padding: 15px; border: 1px solid var(--border);">
                                            <!-- System Master Switch -->
                                            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:15px; padding-bottom:10px; border-bottom:1px solid rgba(0,0,0,0.05);">
                                                <div>
                                                    <div style="font-size:11px; font-weight:900; text-transform:uppercase; color:var(--text-main); letter-spacing:0.5px;">System Power</div>
                                                    <div style="font-size:9px; color:#e74c3c;" id="system_power_status_face">Disabled</div>
                                                </div>
                                                <label class="switch-premium" style="transform: scale(0.8);">
                                                    <input type="checkbox" id="master_system_switch_face" onchange="toggleSystemMaster(this)">
                                                    <span class="slider-premium"></span>
                                                </label>
                                            </div>

                                            <!-- Scan Mode Selector -->
                                            <div style="font-size:10px; font-weight:800; text-transform:uppercase; color:var(--text-dim); margin-bottom:8px;">Scan Mode</div>
                                            <div class="mode-selector" style="display:flex; gap:5px; background:rgba(0,0,0,0.05); padding:4px; border-radius:30px; border: 1px solid rgba(0,0,0,0.05);">
                                                <div id="mode_auto_face" onclick="setScanMode('auto')" class="mode-pill" style="flex:1; text-align:center; padding:6px 10px; border-radius:20px; font-size:10px; font-weight:900; background:var(--primary); color:#fff;">AUTO</div>
                                                <div id="mode_in_face" onclick="setScanMode('check_in')" class="mode-pill" style="flex:1; text-align:center; padding:6px 10px; border-radius:20px; font-size:10px; font-weight:900; color:var(--text-dim);">IN</div>
                                                <div id="mode_out_face" onclick="setScanMode('check_out')" class="mode-pill" style="flex:1; text-align:center; padding:6px 10px; border-radius:20px; font-size:10px; font-weight:900; color:var(--text-dim);">OUT</div>
                                            </div>
                                        </div>
                                    </div>

                                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 15px;">
                                        
                                        <!-- ENROLL FACE: Only visible in AUTO mode -->
                                        <div id="faceEnrollBtnWrapper">
                                            <button class="btn theme-btn-3d smart-action-btn" id="faceEnrollBtn" onclick="openFaceEnrollModal()" style="width:100%; border: 1px solid var(--primary); padding: 20px 10px; border-radius: 20px; display: flex; flex-direction: column; align-items: center; gap: 10px; box-shadow: 0 10px 20px rgba(99,102,241,0.2);">
                                                <i class="fas fa-user-plus" style="font-size: 24px;"></i>
                                                <span style="font-size: 11px; font-weight: 900; letter-spacing: 0.5px;">ENROLL FACE</span>
                                            </button>
                                        </div>
                                        
                                        <button class="btn smart-action-btn" data-access-id="att_smart_logs" onclick="openAttendanceSummaryModal()" style="border: 1px solid var(--border); padding: 20px 10px; border-radius: 20px; display: flex; flex-direction: column; align-items: center; gap: 10px;">
                                            <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-bottom: 5px; opacity: 0.5;"></i>
                                            <i class="fas fa-chart-line" style="font-size: 24px; color: var(--neon-blue);"></i>
                                            <span style="font-size: 11px; font-weight: 900; letter-spacing: 0.5px;">SESSIONS LOG</span>
                                        </button>
                                    </div>

                                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px;">
                                        <button class="btn theme-btn-3d" onclick="finishAttendance('check_in')" style="padding: 15px; background: #e67e22; border-radius: 15px; font-weight: 900; font-size: 11px; border: none; box-shadow: 0 5px 15px rgba(230,126,34,0.2); transition: 0.3s;">
                                            <i class="fas fa-sign-in-alt" style="margin-right: 5px;"></i> FINISH CHECK IN
                                        </button>
                                        <button class="btn theme-btn-3d" onclick="finishAttendance('check_out')" style="padding: 15px; background: #c0392b; border-radius: 15px; font-weight: 900; font-size: 11px; border: none; box-shadow: 0 5px 15px rgba(192,57,43,0.2); transition: 0.3s;">
                                            <i class="fas fa-sign-out-alt" style="margin-right: 5px;"></i> FINISH CHECK OUT
                                        </button>
                                    </div>

                                    <!-- Security Banner -->
                                    <div style="background: rgba(245, 158, 11, 0.05); padding: 15px 20px; border-radius: 16px; border: 1px solid rgba(245, 158, 11, 0.1); display: flex; align-items: center; gap: 15px;">
                                        <i class="fas fa-shield-alt" style="color: #f59e0b; font-size: 20px;"></i>
                                        <div style="font-size: 11px; font-weight: 700; color: rgba(255,255,255,0.7); line-height: 1.5;">
                                            <strong style="color: #f59e0b;">AI ACTIVE:</strong> Attendance is automatically verified upon 95% match confidence. All detections are end-to-end encrypted.
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Face Enrollment Modal (New) -->
                    <!-- Face Enrollment Modal (New) -->
                    <div id="faceEnrollModal" class="premium-modal-overlay" style="display:none; z-index: 9999;">
                        <input type="hidden" id="enroll_modal_current_mode" value="face">
                        <div class="premium-modal" style="max-width: 500px;">
                            <div class="modal-header" style="display: flex; justify-content: space-between; align-items: center;">
                                <div style="display: flex; align-items: center; gap: 15px;">
                                    <i class="fas fa-arrow-left" id="enrollModalBack" style="cursor:pointer; display:none; font-size: 16px; opacity: 0.7;" onclick="goBackToEnrollStep1()"></i>
                                    <h4 style="margin:0; font-size:14px; font-weight:900; letter-spacing:1px;" id="enrollModalTitle"><i class="fas fa-id-card-alt" style="margin-right: 10px;"></i> FACE ENROLLMENT</h4>
                                </div>
                                <div style="display: flex; align-items: center; gap: 15px;">
                                    <i class="fas fa-sync-alt" style="cursor: pointer; opacity: 0.5; font-size: 14px;" onclick="refreshEnrollmentSlots()" title="Refresh Registered Slots"></i>
                                    <i class="fas fa-times" style="cursor:pointer; opacity: 0.5;" onclick="closeFaceEnrollModal()"></i>
                                </div>
                            </div>
                            <div class="modal-body">
                                <div id="enroll-step-1">
                                    <label class="policy-label" style="font-size:11px; margin-bottom:10px;">USER CATEGORY</label>
                                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin-bottom: 25px;">
                                        <div class="spongy-card enroll-type-btn active" id="enroll-type-student" onclick="setEnrollType('student', this)" style="padding: 15px; text-align: center; cursor: pointer; border: 2px solid #3498db;">
                                            <i class="fas fa-user-graduate" style="font-size: 20px; color: #3498db; margin-bottom: 8px;"></i>
                                            <div style="font-size: 11px; font-weight: 900;">STUDENT</div>
                                        </div>
                                        <div class="spongy-card enroll-type-btn" id="enroll-type-staff" onclick="setEnrollType('staff', this)" style="padding: 15px; text-align: center; cursor: pointer; border: 2px solid rgba(255,255,255,0.05);">
                                            <i class="fas fa-user-tie" style="font-size: 20px; opacity: 0.5; margin-bottom: 8px;"></i>
                                            <div style="font-size: 11px; font-weight: 900; opacity: 0.5;">STAFF</div>
                                        </div>
                                        <div class="spongy-card enroll-type-btn" id="enroll-type-bulk" onclick="setEnrollType('bulk', this)" style="padding: 15px; text-align: center; cursor: pointer; border: 2px solid rgba(255,255,255,0.05);">
                                            <i class="fas fa-folder-open" style="font-size: 20px; opacity: 0.5; margin-bottom: 8px;"></i>
                                            <div style="font-size: 11px; font-weight: 900; opacity: 0.5;">BULK</div>
                                        </div>
                                    </div>

                                    <div id="student-enroll-fields">
                                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                                            <div>
                                                <label class="policy-label">CLASS</label>
                                                <select id="enroll_class_sel" class="policy-input" onchange="fetchEnrollSections(this.value)">
                                                    <option value="">SELECT CLASS</option>
                                                    <?php
$classes = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY name");
while ($c = $classes->fetch_assoc())
    echo "<option value='" . $c['id'] . "'>" . $c['name'] . "</option>";
?>
                                                </select>
                                            </div>
                                            <div>
                                                <label class="policy-label">SECTION</label>
                                                <select id="enroll_section_sel" class="policy-input" onchange="fetchEnrollUsers()">
                                                    <option value="">SELECT SECTION</option>
                                                </select>
                                            </div>
                                        </div>
                                    </div>

                                    <div id="staff-enroll-fields" style="display:none; margin-bottom: 20px;">
                                        <label class="policy-label">STAFF CATEGORY</label>
                                        <select id="enroll_staff_cat_sel" class="policy-input" onchange="fetchEnrollUsers()">
                                            <option value="">ALL STAFF</option>
                                            <option value="Teaching">TEACHING</option>
                                            <option value="Non-Teaching">NON-TEACHING</option>
                                        </select>
                                    </div>

                                    <div id="bulk-enroll-fields" style="display:none; margin-bottom: 25px;">
                                        <div style="background: rgba(52, 152, 219, 0.05); padding: 25px; border-radius: 20px; border: 2px dashed rgba(52, 152, 219, 0.3); text-align: center;">
                                            <i class="fas fa-images" style="font-size: 30px; color: #3498db; margin-bottom: 15px; opacity: 0.8;"></i>
                                            <h5 style="font-size: 13px; font-weight: 900; margin-bottom: 8px;">ADVANCED BULK UPLOAD</h5>
                                            <p style="font-size: 11px; opacity: 0.7; margin-bottom: 20px; line-height: 1.4; color: var(--text-main) !important;">
                                                Select multiple photos of students.<br>
                                                Files must be named by <b>Class Number</b> (e.g., 1.jpg, 2.jpg).<br>
                                                <a href="face_enrollment_bulk.php" target="_blank" style="color: #3498db; font-weight: 900; text-decoration: underline;">Open Advanced Page</a>
                                            </p>
                                            <input type="file" id="bulk_folder_input" multiple accept="image/*" style="display:none;" onchange="handleBulkFolderSelect(this)">
                                            
                                            <div id="bulk_selection_status" style="display:none; margin-bottom: 15px; background: rgba(46, 204, 113, 0.1); padding: 10px; border-radius: 10px;">
                                                <i class="fas fa-check-circle" style="color: #2ecc71; margin-right: 5px;"></i>
                                                <span id="bulk_file_count_display" style="font-size: 12px; font-weight: 800; color: #2ecc71;">0 PHOTOS READY</span>
                                            </div>

                                            <button class="btn" onclick="document.getElementById('bulk_folder_input').click()" style="width: auto !important; padding: 8px 15px !important; background: #ffffff !important; color: #000000 !important; border: 1px solid #000000 !important; font-weight: 900; font-size: 11px !important; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.1) !important;">
                                                <i class="fas fa-images" style="margin-right: 8px; color: #000000 !important;"></i> SELECT PHOTOS
                                            </button>
                                        </div>
                                    </div>

                                    <div style="margin-bottom: 25px;">
                                        <label class="policy-label">SELECT PERSONNEL</label>
                                        <select id="enroll_user_sel" class="policy-input" style="height: 55px; font-size: 15px; border-color: #3498db;" onchange="updateUserBiometricInfo()">
                                            <option value="">WAITING FOR SELECTION...</option>
                                        </select>
                                    </div>

                                    <!-- NEW: Biometric Management Info -->
                                    <div id="user_biometric_info" style="display:none; background: rgba(0,0,0,0.2); border-radius: 15px; padding: 15px; border: 1px solid rgba(255,255,255,0.05); margin-bottom: 25px; animation: fadeIn 0.3s ease;">
                                        <div style="font-size: 10px; font-weight: 800; color: var(--text-dim); text-transform: uppercase; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;">
                                            <i class="fas fa-fingerprint"></i> CURRENT BIOMETRICS
                                        </div>
                                        <div id="biometric_info_list" style="display: flex; flex-direction: column; gap: 8px;">
                                            <!-- Dynamic items here -->
                                        </div>
                                        <button class="btn" data-access-id="att_smart_advanced" style="width:100%; margin-top: 15px; background: rgba(155, 89, 182, 0.2); border: 1px solid rgba(155, 89, 182, 0.4); font-size: 11px; font-weight: 800; color: #9b59b6; padding: 12px; border-radius: 10px; transition: 0.3s;" onclick="openBiometricManagerBySelection()">
                                            <i class="fas fa-lock access-control-icon" style="font-size: 10px; margin-right: 5px; opacity: 0.5;"></i>
                                            <i class="fas fa-cogs mr-2"></i> ADVANCED MANAGEMENT
                                        </button>
                                    </div>

                                    <div id="fingerprint-enroll-fields" style="display:none; margin-bottom: 25px;">
                                        <label class="policy-label">SELECT FINGER</label>
                                        <select id="enroll_finger_sel" class="policy-input">
                                            <option value="Left Thumb">Left Thumb</option>
                                            <option value="Left Index">Left Index</option>
                                            <option value="Left Middle">Left Middle</option>
                                            <option value="Left Ring">Left Ring</option>
                                            <option value="Left Little">Left Little</option>
                                            <option value="Right Thumb">Right Thumb</option>
                                            <option value="Right Index" selected>Right Index</option>
                                            <option value="Right Middle">Right Middle</option>
                                            <option value="Right Ring">Right Ring</option>
                                            <option value="Right Little">Right Little</option>
                                        </select>
                                    </div>

                                    <input type="file" id="enroll_single_file_input" accept="image/*" style="display:none;" onchange="handleSingleFileUpload(this)">
                                    <button class="btn text-black-force" data-access-id="att_enroll_photo" id="enrollExistingBtn" onclick="document.getElementById('enroll_single_file_input').click()" style="width: 100%; padding: 18px; font-weight: 900; background: #fff; color: #000; letter-spacing: 1px; margin-bottom: 20px; border: 2px solid #000;">
                                        <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-right: 8px; opacity: 0.5;"></i>
                                        <i class="fas fa-upload" style="margin-right: 10px;"></i> UPLOAD & ENROLL PHOTO
                                    </button>

                                    <div id="enroll-tip-box" style="background: rgba(241, 196, 15, 0.1); padding: 15px; border-radius: 12px; border: 1px solid rgba(241, 196, 15, 0.2); margin-bottom: 25px;">
                                        <div style="display: flex; gap: 10px; align-items: flex-start;">
                                            <i class="fas fa-exclamation-triangle" style="color: #f1c40f; margin-top: 3px;"></i>
                                            <p id="enroll-tip-text" style="font-size: 11px; margin: 0; line-height: 1.5; color: rgba(255,255,255,0.7);">
                                                <b>NEXT STEP:</b> You will take 3 high-definition snapshots of the selected person to train the AI model. Ensure clear lighting.
                                            </p>
                                        </div>
                                    </div>

                                    <button class="btn text-black-force" data-access-id="att_enroll_proceed" id="enrollProceedBtn" onclick="proceedToEnrollStep2()" style="width: 100%; padding: 18px; font-weight: 900; background: #fff; color: #000; letter-spacing: 1px; border: 2px solid #000;">
                                        <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-right: 8px; opacity: 0.5;"></i>
                                        PROCEED TO CAPTURE <i class="fas fa-arrow-right" style="margin-left: 10px;"></i>
                                    </button>
                                </div>

                                <div id="enroll-step-2" style="display:none; text-align: center;">
                                    <div id="face-capture-ui">
                                        <div id="capture-video-container" style="position: relative; width: 100%; height: 300px; background: #000; border-radius: 16px; overflow: hidden; margin-bottom: 20px;">
                                            <img id="enrollCaptureFeed" src="" style="width: 100%; height: 100%; object-fit: cover;">
                                            <div id="captureOverlay" style="position: absolute; inset: 0; border: 40px solid rgba(0,0,0,0.5); pointer-events: none;">
                                                <div style="position: absolute; inset: 0; border: 2px dashed #2ecc71; border-radius: 50%;"></div>
                                            </div>
                                        </div>
                                        <div style="display: flex; justify-content: center; gap: 10px; margin-bottom: 20px;">
                                            <div class="capture-pill" id="shot-1">1</div>
                                            <div class="capture-pill" id="shot-2">2</div>
                                            <div class="capture-pill" id="shot-3">3</div>
                                        </div>
                                        <p id="capture-status" style="font-size: 13px; font-weight: 700; color: #2ecc71;">POSITION FACE IN THE CIRCLE</p>
                                        <button class="btn text-black-force" data-access-id="att_capture_snapshot" id="faceCaptureBtn" onclick="takeFaceSnapshot()" style="width: 100%; padding: 18px; font-weight: 900; background: #fff; color: #000; border: 2px solid #000;">
                                            <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-right: 8px; opacity: 0.5;"></i>
                                            <i class="fas fa-camera" style="margin-right: 10px;"></i> CAPTURE SNAPSHOT
                                        </button>
                                    </div>
                                    
                                    <div id="fingerprint-capture-ui" style="display:none;">
                                        <div style="background: rgba(0,0,0,0.2); width: 150px; height: 180px; margin: 0 auto 20px; border-radius: 15px; border: 2px solid rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden;">
                                            <div id="fingerprintImgPreview" style="display:none; width: 100%; height: 100%;">
                                                <img id="fingerprintPreviewImg" src="" style="width: 100%; height: 100%; object-fit: contain;">
                                            </div>
                                            <div id="fingerprintIconPlaceholder">
                                                <i class="fas fa-fingerprint" style="font-size: 60px; opacity: 0.2;"></i>
                                            </div>
                                            <div id="fingerScanLine" class="scan-line" style="display:none;"></div>
                                        </div>
                                        <p id="fingerprint-capture-status" style="font-size: 13px; font-weight: 700; opacity: 0.7;">READY FOR SCAN...</p>
                                        <button class="btn text-black-force" data-access-id="att_capture_fingerprint" id="fingerCaptureBtn" onclick="captureFingerprintEnroll()" style="width: 100%; padding: 18px; font-weight: 900; background: #8e44ad; color: #fff !important; border: 2px solid rgba(255,255,255,0.1); border-radius: 12px; box-shadow: 0 8px 15px rgba(142,68,173,0.3);">
                                            <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-right: 8px; opacity: 0.5;"></i>
                                            <i class="fas fa-fingerprint" style="margin-right: 10px; color: #fff !important;"></i> CAPTURE FINGERPRINT
                                        </button>

                                        <!-- New Review Controls -->
                                        <div id="fingerprint-enroll-controls" style="display:none; gap: 10px; margin-top: 15px;">
                                            <button class="btn" onclick="saveFingerprintEnrollment()" style="flex: 2; padding: 18px; background: #2ecc71; color: #fff; font-weight: 900; border: none; border-radius: 12px; font-size: 14px; letter-spacing: 1px;">
                                                <i class="fas fa-check-circle" style="margin-right: 8px;"></i> SAVE ENROLLMENT
                                            </button>
                                            <button class="btn" onclick="resetFingerprintCapture()" style="flex: 1; padding: 18px; background: rgba(231, 76, 60, 0.1); color: #e74c3c; font-weight: 900; border: 1px solid rgba(231, 76, 60, 0.3); border-radius: 12px; font-size: 14px;">
                                                <i class="fas fa-redo"></i> RETRY
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- B: Fingerprint Panel -->
                    <div id="fingerprint-panel" class="smart-panel" style="display:none;">
                        <div class="premium-card" style="padding: 30px !important;">
                            <div style="display: flex; gap: 40px; flex-wrap: wrap; align-items: start;">
                                
                                <!-- LEFT: Biometric Scanner Module -->
                                <div class="biometric-box" style="flex: 1; min-width: 350px; display: flex; flex-direction: column; align-items: center; gap: 30px; padding: 20px; border-radius: 30px; border: 1px solid var(--border);">
                                    <div style="text-align: center;">
                                        <div style="font-size: 11px; font-weight: 900; color: var(--primary); letter-spacing: 2px; text-transform: uppercase; margin-bottom: 10px;">IDentify Mode</div>
                                        <div class="biometric-title" style="font-size: 20px; font-weight: 800; color: #fff;">Biometric Core</div>
                                    </div>

                                    <div style="position: relative; width: 180px; height: 230px; background: radial-gradient(circle, rgba(99,102,241,0.1) 0%, rgba(2,6,23,0.8) 100%); border-radius: 40px; border: 2px solid var(--glass-border); display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 0 40px rgba(0,0,0,0.5), inset 0 0 30px rgba(99,102,241,0.05);">
                                        <!-- Glowing Ring -->
                                        <div style="position: absolute; width: 150px; height: 150px; border: 2px dashed rgba(99,102,241,0.2); border-radius: 50%; animation: spin 10s linear infinite;"></div>
                                        
                                        <div id="scannerStatusIcon" style="z-index: 5;">
                                            <i class="fas fa-fingerprint" style="font-size: 80px; color: var(--primary); filter: drop-shadow(0 0 15px rgba(99,102,241,0.4)); transition: 0.5s;"></i>
                                        </div>

                                        <!-- Laser Line -->
                                        <div id="scannerScanLine" class="laser-line" style="display:none; width: 100%; position: absolute; z-index: 6;"></div>

                                        <!-- Success Overlay -->
                                        <div id="scannerSuccessOverlay" style="position: absolute; inset: 0; background: rgba(16, 185, 129, 0.1); backdrop-filter: blur(4px); display: none; align-items: center; justify-content: center; z-index: 10;">
                                            <div style="width: 70px; height: 70px; background: var(--success); border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 30px var(--success);">
                                                <i class="fas fa-check" style="color: #fff; font-size: 30px;"></i>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Identification Preview -->
                                    <div id="verifiedUserResult" style="width: 100%; display: none; transform-origin: top; animation: fadeInScale 0.4s ease-out;">
                                        <div style="background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(16, 185, 129, 0.05) 100%); border: 1px solid rgba(16, 185, 129, 0.2); border-radius: 20px; padding: 15px; display: flex; align-items: center; gap: 15px;">
                                            <div id="vUserPic" style="width: 55px; height: 55px; border-radius: 12px; background: #000; overflow: hidden; border: 2px solid rgba(255,255,255,0.1); flex-shrink: 0;"></div>
                                            <div>
                                                <div id="vUserName" style="font-weight: 800; font-size: 14px; color: #fff; text-transform: uppercase;">User Name</div>
                                                <div id="vUserStatus" style="font-size: 10px; color: var(--success); font-weight: 800; letter-spacing: 0.5px; margin-top: 2px;">MATCH FOUND • ATTENDANCE LOGGED</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <!-- RIGHT: System Control Console -->
                                <div style="flex: 1.2; min-width: 350px; display: flex; flex-direction: column; gap: 30px;">
                                    <div class="biometric-box" style="padding: 30px; border-radius: 30px; border: 1px solid var(--border);">
                                        <div style="margin-bottom: 30px; text-align: left;">
                                             <h3 id="fingerprintMainStatus" style="font-size: 24px; font-weight: 900; color: #fff; margin-bottom: 10px; letter-spacing: -0.5px;">Ready to Scan</h3>
                                             <div id="biometricStatsBadge" style="display: inline-block; padding: 4px 10px; background: rgba(99,102,241,0.1); border: 1px solid rgba(99,102,241,0.2); border-radius: 8px; font-size: 10px; font-weight: 700; color: var(--primary); margin-bottom: 15px; cursor: pointer;" onclick="openFingerprintManager()">
                                                DEVICE STANDBY • LOADING DATA...
                                             </div>
                                             <p id="fingerprintSubStatus" style="font-size: 13px; color: var(--text-dim); line-height: 1.6;">
                                                 Please place your registered finger on the biometric sensor.
                                             </p>
                                        </div>

                                        <!-- Smart Navigation -->

                                        <!-- Smart Navigation -->
                                        <div style="background: rgba(0,0,0,0.2); border-radius: 12px; padding: 15px; border: 1px solid var(--border); margin-bottom: 15px;">
                                            <!-- System Master Switch -->
                                            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:15px; padding-bottom:10px; border-bottom:1px solid rgba(255,255,255,0.05);">
                                                <div>
                                                    <div style="font-size:11px; font-weight:900; text-transform:uppercase; color:var(--text-main); letter-spacing:0.5px;">System Power</div>
                                                    <div style="font-size:9px; color:var(--text-dim);" id="system_power_status">Active</div>
                                                </div>
                                                <label class="switch-premium" style="transform: scale(0.8);">
                                                    <input type="checkbox" id="master_system_switch" onchange="toggleSystemMaster(this)" checked>
                                                    <span class="slider-premium"></span>
                                                </label>
                                            </div>

                                            <!-- Scan Mode Selector -->
                                            <div style="font-size:10px; font-weight:800; text-transform:uppercase; color:var(--text-dim); margin-bottom:8px;">Scan Mode</div>
                                            <div class="mode-selector" style="display:flex; gap:5px; background:rgba(0,0,0,0.2); padding:4px; border-radius:30px; border: 1px solid rgba(255,255,255,0.05);">
                                                <div id="mode_auto" onclick="setScanMode('auto')" class="mode-pill" style="flex:1; text-align:center; padding:6px 10px; border-radius:20px; font-size:10px; font-weight:900; background:var(--primary); color:#fff;">AUTO</div>
                                                <div id="mode_in" onclick="setScanMode('check_in')" class="mode-pill" style="flex:1; text-align:center; padding:6px 10px; border-radius:20px; font-size:10px; font-weight:900; color:var(--text-dim);">IN</div>
                                                <div id="mode_out" onclick="setScanMode('check_out')" class="mode-pill" style="flex:1; text-align:center; padding:6px 10px; border-radius:20px; font-size:10px; font-weight:900; color:var(--text-dim);">OUT</div>
                                            </div>
                                        </div>

                                        <div style="display: grid; gap: 15px;">
                                            <!-- System Manual Override Button -->
                                            <button class="btn force-active-btn" data-access-id="att_smart_activate" onclick="toggleForceActive()" style="background: var(--primary); color: #fff; padding: 15px; border-radius: 12px; font-weight: 800; font-size: 13px; letter-spacing: 0.5px; border: none; box-shadow: 0 5px 15px rgba(99,102,241,0.2); transition: 0.3s;">
                                                <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-right: 8px; opacity: 0.5;"></i>
                                                <i class="fas fa-play-circle" style="margin-right: 8px;"></i> ACTIVATE NOW
                                            </button>

                                            <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                                                <button class="btn theme-btn-3d" data-access-id="att_smart_finish" onclick="finishAttendance('check_in')" style="flex: 1; min-width: 150px; background: #e67e22; padding: 12px 5px; border-radius: 12px; font-weight: 800; font-size: 11px; letter-spacing: 0.5px; border: none;">
                                                    <i class="fas fa-lock access-control-icon" style="font-size: 10px; margin-right: 5px; opacity: 0.5;"></i>
                                                    <i class="fas fa-sign-in-alt" style="margin-right: 8px;"></i> FINISH CHECK IN
                                                </button>
                                                <button class="btn theme-btn-3d" data-access-id="att_smart_finish" onclick="finishAttendance('check_out')" style="flex: 1; min-width: 150px; background: #c0392b; padding: 12px 5px; border-radius: 12px; font-weight: 800; font-size: 11px; letter-spacing: 0.5px; border: none;">
                                                    <i class="fas fa-lock access-control-icon" style="font-size: 10px; margin-right: 5px; opacity: 0.5;"></i>
                                                    <i class="fas fa-sign-out-alt" style="margin-right: 8px;"></i> FINISH CHECK OUT
                                                </button>
                                            </div>

                                            <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                                                <button class="btn theme-btn-3d" data-access-id="att_smart_new_reg" onclick="openFingerEnrollModal()" style="flex: 1; min-width: 150px; background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%); padding: 12px 5px; border-radius: 12px; font-weight: 800; font-size: 11px; letter-spacing: 0.5px; border: none; box-shadow: 0 5px 15px rgba(99,102,241,0.1);">
                                                    <i class="fas fa-lock access-control-icon" style="font-size: 10px; margin-right: 5px; opacity: 0.5;"></i>
                                                    <i class="fas fa-plus-circle" style="margin-right: 8px;"></i> NEW REGISTRATION
                                                </button>
                                                
                                                <button class="btn" onclick="openAttendanceSummaryModal()" style="flex: 1; min-width: 100px; background: rgba(255,255,255,0.05); padding: 12px 5px; border-radius: 12px; font-weight: 800; font-size: 11px; letter-spacing: 0.5px; border: 1px solid var(--border); transition: 0.3s; color:var(--text-dim);">
                                                    <i class="fas fa-history" style="margin-right: 8px;"></i> LOGS
                                                </button>
                                                
                                                <a href="../../AttendanceHub_Desktop.tar" download style="flex: 1; min-width: 160px; text-decoration: none; text-align: center; background: rgba(99, 102, 241, 0.1); border: 1px solid rgba(99, 102, 241, 0.3); padding: 12px 5px; border-radius: 12px; font-weight: 800; font-size: 11px; letter-spacing: 0.5px; color: var(--primary); display: flex; align-items: center; justify-content: center; transition: 0.3s;">
                                                    <i class="fas fa-download" style="margin-right: 8px;"></i> DESKTOP HUB
                                                </a>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Hardware Diagnostics -->
                                    <div style="display: flex; justify-content: space-between; padding: 0 10px;">
                                        <div style="display: flex; gap: 20px;">
                                            <div style="text-align: center;">
                                                <div style="font-size: 9px; font-weight: 800; color: var(--text-dim); text-transform: uppercase;">Signal</div>
                                                <div style="font-size: 12px; font-weight: 900; color: var(--success);">100%</div>
                                            </div>
                                            <div style="text-align: center;">
                                                <div style="font-size: 9px; font-weight: 800; color: var(--text-dim); text-transform: uppercase;">Lat.</div>
                                                <div style="font-size: 12px; font-weight: 900; color: #fff;">12ms</div>
                                            </div>
                                        </div>
                                        <div style="text-align: right;">
                                            <div style="font-size: 9px; font-weight: 800; color: var(--text-dim); text-transform: uppercase;">Engine</div>
                                            <div style="font-size: 12px; font-weight: 900; color: #fff; opacity: 0.6;">HAMSTER PRO 20</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- C: Settings Panel -->
                    <div id="smart-settings-panel" class="smart-panel" style="display:none;">
                        <div class="premium-card" style="padding: 30px !important;">
                            <div style="margin-bottom: 35px; display: flex; align-items: center; gap: 15px;">
                                <div style="width: 50px; height: 50px; background: rgba(99, 102, 241, 0.1); border-radius: 15px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--border);">
                                    <i class="fas fa-microchip" style="font-size: 22px; color: var(--primary);"></i>
                                </div>
                                <div>
                                    <h2 class="text-dynamic" style="font-size: 22px; font-weight: 800; color: #fff; margin: 0;">Smart Configuration</h2>
                                    <p class="text-dynamic-dim" style="font-size: 13px; color: var(--text-dim); margin: 0;">Hardware & AI system parameters</p>
                                </div>
                            </div>
                            
                            <!-- Master Switches -->
                            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; margin-bottom: 40px;">
                                <div class="settings-card premium-card" style="padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                                    <div style="display: flex; align-items: center; gap: 15px;">
                                        <i class="fas fa-smile" style="font-size: 20px; color: var(--primary);"></i>
                                        <div>
                                            <div class="biometric-title" style="font-size: 14px; font-weight: 800; color: #fff;">Vision Engine</div>
                                            <div class="text-dim" style="font-size: 11px; color: var(--text-dim);">Face Recognition AI</div>
                                        </div>
                                    </div>
                                    <label class="switch-premium">
                                        <input type="checkbox" id="setting_face_active" checked>
                                        <span class="slider-premium"></span>
                                    </label>
                                </div>

                                <div class="settings-card premium-card" style="padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                                    <div style="display: flex; align-items: center; gap: 15px;">
                                        <i class="fas fa-fingerprint" style="font-size: 20px; color: var(--secondary);"></i>
                                        <div>
                                            <div class="biometric-title" style="font-size: 14px; font-weight: 800; color: #fff;">Pulse Engine</div>
                                            <div class="text-dim" style="font-size: 11px; color: var(--text-dim);">Biometric Scanner</div>
                                        </div>
                                    </div>
                                    <label class="switch-premium">
                                        <input type="checkbox" id="setting_finger_active" checked>
                                        <span class="slider-premium"></span>
                                    </label>
                                </div>

                                <div class="settings-card premium-card" style="padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                                    <div style="display: flex; align-items: center; gap: 15px;">
                                        <i class="fas fa-bell" style="font-size: 20px; color: var(--warning);"></i>
                                        <div>
                                            <div class="biometric-title" style="font-size: 14px; font-weight: 800; color: #fff;">Smart Alerts</div>
                                            <div class="text-dim" style="font-size: 11px; color: var(--text-dim);">SMS Notifications</div>
                                        </div>
                                    </div>
                                    <label class="switch-premium">
                                        <input type="checkbox" id="setting_smart_sms" checked>
                                        <span class="slider-premium"></span>
                                    </label>
                                </div>

                                <!-- HIDDEN FIELDS FOR COMPATIBILITY -->
                                <input type="checkbox" id="setting_auto_mark" checked style="display:none;">
                                <input type="checkbox" id="setting_sound" checked style="display:none;">
                            </div>

                            <!-- Detailed Configuration Sections -->
                            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 30px;">
                                
                                <!-- Hardware: Network Camera -->
                                <div class="settings-box">
                                    <h4 style="font-size: 13px; font-weight: 900; color: var(--primary); letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 25px; display: flex; align-items: center; gap: 10px;">
                                        <i class="fas fa-video"></i> Vision Hardware
                                    </h4>
                                    
                                    <div style="display: grid; gap: 20px;">
                                        <div>
                                            <label class="text-dynamic-dim" style="font-size: 10px; font-weight: 800; color: var(--text-dim); text-transform: uppercase; margin-bottom: 8px; display: block;">IP Stream Address</label>
                                            <div style="position: relative;">
                                                <i class="fas fa-link" style="position: absolute; left: 15px; top: 18px; color: var(--primary); opacity: 0.5;"></i>
                                                <input type="text" id="setting_face_ip" placeholder="192.168.1.108" style="padding-left: 45px !important; width: 100%;">
                                            </div>
                                            <div id="camera_diag_container" style="margin-top: 10px;">
                                                <button type="button" onclick="runCameraDiagnostic()" class="btn-premium" style="width: 100%; height: 35px; font-size: 11px; background: rgba(52, 152, 219, 0.1); border: 1px dashed rgba(52, 152, 219, 0.3); color: var(--primary); display: flex; align-items: center; justify-content: center; gap: 8px;">
                                                    <i class="fas fa-stethoscope"></i> RUN CONNECTIVITY WIZARD
                                                </button>
                                                <div id="camera_diag_result" style="display:none; margin-top: 10px; padding: 12px; border-radius: 10px; font-size: 11px; line-height: 1.5; border: 1px solid transparent;"></div>
                                            </div>
                                        </div>
                                        
                                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                                            <div>
                                                <label class="text-dynamic-dim" style="font-size: 10px; font-weight: 800; color: var(--text-dim); text-transform: uppercase; margin-bottom: 8px; display: block;">Auth User</label>
                                                <input type="text" id="setting_face_user" placeholder="admin" style="width: 100%;">
                                            </div>
                                            <div>
                                                <label class="text-dynamic-dim" style="font-size: 10px; font-weight: 800; color: var(--text-dim); text-transform: uppercase; margin-bottom: 8px; display: block;">Auth Pass</label>
                                                <div style="position: relative;">
                                                    <input type="password" id="setting_face_pass" style="width: 100%; height: 52px; background: rgba(15, 23, 42, 0.8); border: 1px solid var(--border); border-radius: 14px; padding: 0 45px 0 16px; color: white; outline: none;">
                                                    <i class="fas fa-eye" id="toggle_pass_icon" onclick="togglePasswordVisibility()" style="position: absolute; right: 15px; top: 18px; color: var(--text-dim); cursor: pointer; opacity: 0.7; z-index: 5;"></i>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <!-- Logic: Attendance Rules -->
                                <div class="settings-box">
                                    <h4 style="font-size: 13px; font-weight: 900; color: var(--secondary); letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 25px; display: flex; align-items: center; justify-content: space-between; gap: 10px;">
                                        <span><i class="fas fa-clock"></i> Operation Logic</span>
                                        <span id="system_time_display_settings" style="font-size: 10px; color: var(--primary); background: rgba(52, 152, 219, 0.1); padding: 4px 8px; border-radius: 6px;">
                                            Wait...
                                        </span>
                                    </h4>
                                    
                                    <div style="display: grid; gap: 20px;">
                                        <div>
                                            <label class="text-dynamic-dim" style="font-size: 10px; font-weight: 800; color: var(--text-dim); text-transform: uppercase; margin-bottom: 8px; display: block;">Attendance Window</label>
                                            <select id="setting_att_mode" style="width: 100%;" onchange="toggleCheckOutSettings()">
                                                <option value="check_in_only">CHECK-IN ONLY (Default)</option>
                                                <option value="check_in_out">SHIFT: IN & OUT TRACKING</option>
                                            </select>
                                        </div>

                                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                                            <div id="checkin_window">
                                                <label style="font-size: 10px; font-weight: 800; color: var(--success); text-transform: uppercase; margin-bottom: 8px; display: block;">Check-In Time</label>
                                                <div style="display: flex; gap: 8px;">
                                                    <input type="time" id="setting_in_start" style="width: 100%; font-size: 12px !important;">
                                                    <input type="time" id="setting_in_end" style="width: 100%; font-size: 12px !important;">
                                                </div>
                                            </div>
                                            <div id="checkout_settings_container" style="display: none;">
                                                <label style="font-size: 10px; font-weight: 800; color: var(--danger); text-transform: uppercase; margin-bottom: 8px; display: block;">Check-Out Time</label>
                                                <div style="display: flex; gap: 8px;">
                                                    <input type="time" id="setting_out_start" style="width: 100%; font-size: 12px !important;">
                                                    <input type="time" id="setting_out_end" style="width: 100%; font-size: 12px !important;">
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Footer Actions -->
                            <div style="margin-top: 40px; padding-top: 30px; border-top: 1px solid var(--border); display: flex; justify-content: flex-end; gap: 15px;">
                                <button class="btn" data-access-id="att_smart_reset" style="background: rgba(239, 68, 68, 0.1); color: #ef4444 !important; border: 1px solid rgba(239, 68, 68, 0.2); padding: 15px 30px; border-radius: 16px; font-weight: 800;">
                                    <i class="fas fa-lock access-control-icon" style="font-size: 10px; margin-right: 8px; opacity: 0.5;"></i>
                                    RESET TO DEFAULTS
                                </button>
                                <button class="btn theme-btn-3d" data-access-id="att_smart_deploy" onclick="saveSmartSettings()" style="background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%); padding: 15px 40px; border-radius: 16px; font-weight: 900; border: none; box-shadow: 0 10px 25px rgba(99, 102, 241, 0.3);">
                                    <i class="fas fa-lock access-control-icon" style="font-size: 12px; margin-right: 8px; opacity: 0.5;"></i>
                                    DEPLOY CONFIGURATION <i class="fas fa-rocket" style="margin-left: 10px;"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
                </div>
                </div>


            <!-- 3. Monthly Analysis (Merged) -->
            <div id="analysis-view" class="content-sec section-card" style="margin-top: 0 !important;">
                <div class="section-card sub-tabs" style="display:flex; gap:12px; margin-top: 0 !important; margin-bottom: 20px; width:100%; flex-wrap: wrap; padding: 15px;">
                    <div class="spongy-card sub-tab-btn active" data-access-id="att_analysis_students" onclick="switchAnalysisSubTab('students-analysis-panel', this, 'att_analysis_students')" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                         <!-- Left Tube -->
                        <div class="hardware-tube left"></div>
                        <!-- Right Tube -->
                        <div class="hardware-tube right"></div>
                        <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                        <i class="fas fa-user-graduate" style="font-size: 16px;"></i> 
                        <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;">Students</span>
                    </div>
                    <div class="spongy-card sub-tab-btn" data-access-id="att_analysis_staff" onclick="switchAnalysisSubTab('staff-analysis-panel', this, 'att_analysis_staff')" style="flex: 1; min-width: 200px; padding: 15px 20px !important; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; position: relative; overflow: visible; transition: all 0.3s ease;">
                        <!-- Left Tube -->
                        <div class="hardware-tube left"></div>
                        <!-- Right Tube -->
                        <div class="hardware-tube right"></div>
                        <i class="fas fa-lock access-control-icon" style="font-size: 14px; opacity: 0.5;"></i>
                        <i class="fas fa-user-tie" style="font-size: 16px;"></i> 
                        <span style="font-weight: 800; font-size: 13px; letter-spacing: 0.5px;"><?php echo $is_staff ? 'My Summary' : 'Staff'; ?></span>
                    </div>
                </div>

                <!-- A: Students Monthly Analysis -->
                <div id="students-analysis-panel" class="analysis-panel active">
                    <!-- Admission & Institutional Stats -->
                    <div class="filter-card" style="border-style: dashed; border-color: rgba(255,255,255,0.15);">
                        <label style="font-size:0.75rem; color:var(--text-main); display:block; margin-bottom:12px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">STUDENT PERFORMANCE</label>
                        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;">
                            <select id="avg_class_sel" style="width:100%;" onchange="loadConsolidatedAnalysis()">
                                <option value="">Choose Class...</option>
                                <?php
foreach ($all_classes as $c):
    foreach ($c['sections'] as $s):
        $val = $c['id'] . '|' . $s['id'];
?>
                                        <option value="<?php echo $val; ?>"><?php echo $c['name'] . ' ' . $s['name']; ?></option>
                                    <?php
    endforeach;
endforeach; ?>
                            </select>
                            <script>
                                if(<?php echo $opt_count; ?> === 1) {
                                    document.getElementById('avg_class_sel').value = "<?php echo $last_val; ?>";
                                }
                            </script>
                            <select id="avg_month" style="width:100%;  font-weight:900;" onchange="loadConsolidatedAnalysis()">
                                <?php for ($i = 1; $i <= 12; $i++)
    echo "<option value='" . str_pad($i, 2, '0', STR_PAD_LEFT) . "' " . ($i == date('m') ? 'selected' : '') . " style=' font-weight:900;'>" . date('F', mktime(0, 0, 0, $i, 1)) . "</option>"; ?>
                            </select>
                        </div>
                        <div style="height:10px;"></div>
                    </div>

                    <!-- Combined Result Area -->
                    <div id="analysis-result-container" style="display: none;">
                        <!-- Full Records Card -->
                        <div class="filter-card">
                            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; width: 100%; gap: 15px;">
                                <h4 style="margin:0; font-size:18px;  font-weight: 900; letter-spacing: 1px; white-space: nowrap;">STUDENT ATTENDANCE LEDGER</h4>
                                <div style="position:relative; flex:1; max-width:300px;">
                                     <style>
                                         #ledgerSearchInput::placeholder { color: #000 !important; opacity: 0.7 !important; }
                                         #ledgerSearchInput { color: #000 !important; font-weight: 800 !important; }
                                     </style>
                                     <i class="fas fa-search" style="position:absolute; left:12px; top:50%; transform:translateY(-50%); font-size:13px; opacity:0.7; color: #000 !important; z-index: 5;"></i>
                                     <input type="text" id="ledgerSearchInput" placeholder="Quick Search Name..." onkeyup="filterLedger()"
                                            style="width:100%; height:40px !important; background: #fff !important; border:1px solid rgba(0,0,0,0.2) !important; border-radius:12px !important; padding-left:35px !important; font-size:14px !important; font-weight:800; color: #000 !important; outline: none !important; position: relative; z-index: 1;">
                                </div>
                                <div style="display:flex; gap: 8px;">
                                     <button class="btn no-print theme-print-btn" onclick="printMonthlyLedger('report-result', 'Student Attendance Ledger')" style="width: auto !important; padding: 8px 15px; border: 1px solid currentColor; font-weight: 900; background: transparent;">
                                        <i class="fas fa-print"></i>
                                    </button>
                                    <button class="btn no-print" onclick="shareLedgerWhatsApp()" style="width: auto !important; padding: 8px 15px; border: 1px solid #25D366; color: #25D366; font-weight: 900; background: transparent; border-radius: 8px;">
                                        <i class="fa-brands fa-whatsapp"></i>
                                    </button>
                                    <button class="btn no-print" onclick="closeLedger()" style="width: auto !important; padding: 8px 15px; border: 1px solid #ff7675; color: #ff7675; font-weight: 900; background: transparent; border-radius: 8px;">
                                        <i class="fas fa-times"></i>
                                    </button>
                                </div>
                            </div>
                            <div id="report-result">
                                <div style="text-align:center; padding:30px; opacity:0.3;">Choose parameters to load records</div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- B: Staff Monthly Analysis -->
                <div id="staff-analysis-panel" class="analysis-panel" style="display:none;">
                    <div class="filter-card" style="border-style: dashed; border-color: rgba(255,255,255,0.15);">
                        <label style="font-size:0.75rem; color:var(--text-main); display:block; margin-bottom:12px; font-weight:800; letter-spacing:1px; text-transform:uppercase;">Staff Monthly performance</label>
                        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;">
                            <select id="staff_analysis_month" style="width:100%;  font-weight:900;" onchange="loadStaffMonthlyAnalysis()">
                                <?php for ($i = 1; $i <= 12; $i++)
    echo "<option value='" . str_pad($i, 2, '0', STR_PAD_LEFT) . "' " . ($i == date('m') ? 'selected' : '') . " style=' font-weight:900;'>" . date('F', mktime(0, 0, 0, $i, 1)) . "</option>"; ?>
                            </select>
                            <select id="staff_analysis_type" style="width:100%;  font-weight:900;" onchange="loadStaffMonthlyAnalysis()">
                                <?php if ($is_staff): ?>
                                    <option value="personal" style=" font-weight:900;"><?php echo $_SESSION['edu_name']; ?></option>
                                <?php
else: ?>
                                    <option value="all" style=" font-weight:900;">All Staff</option>
                                    <option value="teaching" style=" font-weight:900;">Teaching Staff</option>
                                    <option value="non-teaching" style=" font-weight:900;">Non-Teaching Staff</option>
                                    <option value="visiting" style=" font-weight:900;">Visiting Staff</option>
                                <?php
endif; ?>
                            </select>
                        </div>
                        <div style="height:10px;"></div>
                    </div>
                </div>
                <!-- Combined Result Area for Staff -->
                <div id="staff-analysis-result-container" style="display: none;">
                    <div class="filter-card">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; width: 100%; gap: 15px;">
                            <h4 style="margin:0; font-size:18px;  font-weight: 900; letter-spacing: 1px; white-space: nowrap;">STAFF PERFORMANCE LEDGER</h4>
                            <div style="display:flex; gap: 8px;">
                                 <button class="btn no-print theme-print-btn" onclick="printMonthlyLedger('staff-report-result', 'Staff Performance Ledger')" style="width: auto !important; padding: 8px 15px; border: 1px solid currentColor; font-weight: 900; background: transparent;">
                                    <i class="fas fa-print"></i>
                                </button>
                                <button class="btn no-print" onclick="shareStaffLedgerWhatsApp()" style="width: auto !important; padding: 8px 15px; border: 1px solid #25D366; color: #25D366; font-weight: 900; background: transparent; border-radius: 8px;">
                                    <i class="fa-brands fa-whatsapp"></i>
                                </button>
                                <button class="btn no-print" onclick="document.getElementById('staff-report-result').innerHTML = '<div style=\'text-align:center; padding:30px; opacity:0.3;\'>Choose parameters to load records</div>'" style="width: auto !important; padding: 8px 15px; border: 1px solid #ff7675; color: #ff7675; font-weight: 900; background: transparent; border-radius: 8px;">
                                    <i class="fas fa-times"></i>
                                </button>
                            </div>
                        </div>
                        <div id="staff-report-result">
                            <div style="text-align:center; padding:30px; opacity:0.3;">Choose parameters to load records</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 4. School Rules (Policy) -->
            <div id="policy-view" class="content-sec" style="width:100%;">
                <div class="filter-card" style="box-shadow: 0 10px 30px rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1);">
                    <h3 style=" margin-bottom:25px; font-size:1.4rem; display:flex; align-items:center; gap:12px; font-weight:900;">
                        <i class="fas fa-university" style=""></i> 
                        Institutional Standards & Payout Rules
                    </h3>
                    
                    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap:25px;">
                        <!-- Financial Controls -->
                        <div style="background:rgba(255,255,255,0.02); padding:15px; border-radius:12px; border:1px solid rgba(255,255,255,0.05);">
                            <label style="font-size:12px;  font-weight:900; display:block; margin-bottom:15px; letter-spacing:1px;">ABSENCE & DEDUCTION</label>
                            
                            <div style="margin-bottom:15px;">
                                <label style="font-size:10px; opacity:0.6;  font-weight:900;">MONTHLY ABSENCE LIMIT</label>
                                <input type="number" id="p_absent_limit" class="policy-input" placeholder="3" style="margin-top:5px; height:45px; font-size:16px; font-weight:bold; ">
                            </div>
                            
                            <div>
                                <label style="font-size:10px; opacity:0.6;  font-weight:900;">FIXED PENALTY PER ABSENCE (RS)</label>
                                <input type="number" id="p_deduction_rate" class="policy-input" placeholder="0 = Auto-Calculate" style="margin-top:5px; height:45px; font-size:16px; font-weight:bold; ">
                                <p style="font-size:9px; opacity:0.8; margin-top:5px;  font-weight:700;">* Automatic Logic: Beyond limit, system deducts 1 full day's pay (Salary / 30).</p>
                            </div>
                        </div>

                        <!-- Class Bonus Structure -->
                        <div style="background:rgba(255,255,255,0.02); padding:15px; border-radius:12px; border:1px solid rgba(255,255,255,0.05);">
                            <label style="font-size:12px;  font-weight:900; display:block; margin-bottom:15px; letter-spacing:1px;">TEACHING BONUS (RS PER EXTRA CLASS)</label>
                            
                            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:15px;">
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">PRIMARY</label>
                                    <input type="number" id="p_bonus_primary" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">MIDDLE</label>
                                    <input type="number" id="p_bonus_middle" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">HIGH</label>
                                    <input type="number" id="p_bonus_high" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">SECONDARY</label>
                                    <input type="number" id="p_bonus_secondary" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                            </div>
                        </div>

                        <!-- Behavior Fines Structure -->
                        <div style="background:rgba(255,255,255,0.02); padding:15px; border-radius:12px; border:1px solid rgba(255,255,255,0.05);">
                            <label style="font-size:12px;  font-weight:900; display:block; margin-bottom:15px; letter-spacing:1px;">DISCIPLINARY FINES (RS)</label>
                            
                            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:15px;">
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">HAIR CUT</label>
                                    <input type="number" id="p_fine_hair" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">REGISTER/BOOKS</label>
                                    <input type="number" id="p_fine_reg" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">RULES BREAK</label>
                                    <input type="number" id="p_fine_rules" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                                <div>
                                    <label style="font-size:9px; opacity:0.6;  font-weight:900;">ATTENDANCE</label>
                                    <input type="number" id="p_fine_att" class="policy-input" style="height:40px; font-size:14px; font-weight:bold; ">
                                </div>
                            </div>
                        </div>
                    </div>

                    <button class="btn" style="margin-top:30px; width:100%; height:65px; font-size:1.2rem; font-weight:900; background: rgba(0,0,0,0.05);  border:1px solid rgba(0,0,0,0.1); border-radius:15px; box-shadow: 0 10px 20px rgba(0,0,0,0.05);" onclick="saveSchoolPolicy()">
                        <i class="fas fa-sync-alt"></i> SYNCHRONIZE INSTITUTIONAL RULES
                    </button>
                    <p style="text-align:center; font-size:10px; opacity:0.4; margin-top:10px;  font-weight:700;">Rule changes apply immediately to all active payroll calculations.</p>
                </div>
            </div>
            </div>
    </div>

    <!-- Leave Appeals View -->
    <div id="leave-appeals-view" class="content-sec section-card" style="margin-top: 0 !important; width:100%;">
        <div style="padding: 20px; text-align: left;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 2px solid rgba(0,0,0,0.05); padding-bottom: 15px;">
                <h2 style="font-size: 1.4rem; font-weight: 900; color: var(--modal-text); letter-spacing: -0.5px; display: flex; align-items: center; gap: 10px;">
                    <i class="fas fa-envelope-open-text text-blue-500"></i> PENDING LEAVE APPEALS
                </h2>
                <div style="display: flex; gap: 5px; background: rgba(0,0,0,0.06); padding: 5px; border-radius: 12px; border: 1px solid rgba(0,0,0,0.08); width: max-content;">
                    <button id="tab-staff-appeal" onclick="switchAppealTab('staff')" style="width: 130px; padding: 10px 0; border-radius: 9px; border: none; font-weight: 800; font-size: 10px; cursor: pointer; transition: all 0.4s ease; text-align: center; background: #3498db; color: white; box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);">STAFF REQUESTS</button>
                    <button id="tab-student-appeal" onclick="switchAppealTab('student')" style="width: 130px; padding: 10px 0; border-radius: 9px; border: none; font-weight: 800; font-size: 10px; cursor: pointer; transition: all 0.4s ease; text-align: center; background: transparent; color: var(--modal-text); opacity: 0.6;">STUDENT REQUESTS</button>
                </div>
            </div>
            <div id="leave-appeals-container">
                <!-- Data will load here via JS -->
            </div>
        </div>
    </div>

    <!-- My Leaves History View (For Staff/Teacher) -->
    <div id="my-leaves-view" class="content-sec section-card" style="margin-top: 0 !important; width:100%;">
        <div style="padding: 20px; text-align: left;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 2px solid rgba(0,0,0,0.05); padding-bottom: 15px;">
                <h2 style="font-size: 1.4rem; font-weight: 900; color: var(--modal-text); letter-spacing: -0.5px; display: flex; align-items: center; gap: 10px;">
                    <i class="fas fa-history text-blue-500"></i> MY LEAVE REQUESTS
                </h2>
                <button onclick="document.getElementById('leaveModal').classList.remove('hidden')" class="btn" style="background:#3498db; color:white; font-weight:900; font-size:11px; padding:8px 15px; border-radius:10px;">
                    <i class="fas fa-plus"></i> NEW REQUEST
                </button>
            </div>
            <div id="my-leaves-container">
                <!-- Data will load here via JS -->
            </div>
        </div>
    </div>
    
    <style>
        .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: var(--modal-overlay-bg, rgba(0,0,0,0.8)); display: none; align-items: center; justify-content: center; z-index: 10000; backdrop-filter: blur(8px); }
        .modal-content { background: var(--modal-bg, #ffffff) !important; padding: 0; border-radius: 20px; width: 95%; max-width: 500px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 1px solid var(--modal-border) !important; color: var(--modal-text, #000) !important; overflow: hidden; }
        .premium-modal { background: var(--modal-bg, #ffffff) !important; color: var(--modal-text, #000) !important; border: 1px solid var(--modal-border) !important; }
        /* ========== THEME SYSTEM: TACTILE & WHITE OVERRIDES ========== */
        /* Theme 1: TACTILE (Perl/Beige with 3D Spongy Effects) */
        body.theme-tactile {
            background-color: #f6f5f0 !important;
            color: #4a4a4a;
        }
        body.theme-tactile .filter-card, body.theme-tactile .avg-card, body.theme-tactile .att-card {
            background: #ffffff !important;
            border: 1px solid rgba(255, 213, 161, 0.6) !important;
            box-shadow: 0 20px 45px rgba(0, 0, 0, 0.35), 0 8px 12px rgba(0, 0, 0, 0.15), inset 0 1px 1px rgba(255, 255, 255, 1) !important;
            
        }
        body.theme-tactile select, body.theme-tactile input[type="date"], body.theme-tactile input[type="number"], body.theme-tactile .policy-input {
            background: rgba(0,0,0,0.03) !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            
        }
        body.theme-tactile .tab-btn {
            background: rgba(255,255,255,0.5) !important;
            color: rgba(0,0,0,0.6) !important;
        }
        body.theme-tactile .tab-btn.active {
            background: #3498db !important;
            color: white !important;
        }
        body.theme-tactile .tabs-container {
            background: rgba(0,0,0,0.05) !important;
            border-bottom: 1px solid rgba(0,0,0,0.05) !important;
        }
        body.theme-tactile .student-row {
            background: #fdfdfb !important;
            border: 1px solid rgba(255, 213, 161, 0.6) !important;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.35), 0 8px 12px rgba(0, 0, 0, 0.15), inset 0 1px 1px rgba(255, 255, 255, 1) !important;
        }
        body.theme-tactile .student-row:hover {
            background: #f8f8f6 !important;
            transform: translateX(5px) translateY(-2px) !important;
        }
        body.theme-tactile .student-name {  }
        body.theme-tactile .smart-table tbody tr {
            background: rgba(255,255,255,0.8) !important;
        }
        body.theme-tactile .smart-table tbody tr td {
            
            border-color: rgba(0,0,0,0.05) !important;
        }
        body.theme-tactile .smart-table th {
            color: #64748b !important;
        }
        body.theme-tactile .sub-tab-btn {
            color: rgba(241, 196, 15, 0.8);
            background: rgba(241, 196, 15, 0.1);
        }
        body.theme-tactile .sub-tab-btn.active {
            background: #3498db !important;
            color: white !important;
            box-shadow: 0 8px 15px rgba(52, 152, 219, 0.3) !important;
        }

        /* End of duplicate check */

        #student-marking-container input[type="date"] { display: none !important; }

        /* Mobile Layout for Main Tabs */
        @media (max-width: 768px) {
            .main-tabs-grid {
                display: grid !important;
                grid-template-columns: repeat(2, 1fr) !important;
                gap: 12px !important;
            }
            .main-tabs-grid .tab-btn {
                flex: none !important;
                min-width: 0 !important;
                width: 100% !important;
                margin: 0 !important;
            }
            /* Explicitly span the 3rd tab if we have 3 */
            .main-tabs-grid .tab-btn:nth-child(3) {
                grid-column: span 2 !important;
            }
            
            /* Consistent Sub-tabs */
            .sub-tabs {
                display: grid !important;
                grid-template-columns: 1fr 1fr !important;
                gap: 10px !important;
            }
            .sub-tabs .sub-tab-btn {
                flex: none !important;
                min-width: 0 !important;
                margin: 0 !important;
            }
        }

        /* Calendar Component Styles (Restored) */
        .calendar-wrapper { position: relative; z-index: 1000; }
        .calendar-trigger {
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 12px;
            padding: 8px 12px;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: all 0.3s ease;
            min-width: 180px;
        }
        .calendar-trigger:hover {
            background: rgba(255,255,255,0.1);
            border-color: rgba(255,255,255,0.3);
        }
        .calendar-dropdown {
            display: none;
            position: absolute;
            top: 100%;
            right: 0;
            width: 320px;
            background: #ffffff;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            padding: 15px;
            margin-top: 10px;
            border: 1px solid rgba(0,0,0,0.1);
            z-index: 1001;
        }
        .calendar-wrapper.open .calendar-dropdown {
            display: block;
            animation: fadeIn 0.2s ease-out;
        }
        .date-grid {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
            gap: 5px;
        }
        .date-cell {
            aspect-ratio: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            cursor: pointer;
            transition: all 0.2s;
        }
        .date-cell:hover {
            background: #e2e8f0;
            color: #0f172a;
        }
        .date-cell.today {
            background: #3498db;
            color: white;
            box-shadow: 0 4px 10px rgba(52, 152, 219, 0.3);
        }
        /* Theme overrides for Calendar */
        body.theme-tactile .calendar-trigger {
            background: #ffffff !important;
            border: 1px solid rgba(0,0,0,0.1) !important;
            color: #4a4a4a;
        }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }
    </style>

    <script src="../../assets/js/access_granular.js?v=<?php echo time(); ?>"></script>
    <script>
        const userRole = '<?php echo strtolower($_SESSION['edu_role'] ?? ''); ?>';
        const isAdmin = ['admin', 'developer', 'super_admin', 'guest', 'useradmin'].includes(userRole);
        const isGuest = userRole === 'guest';
        const isSuper = ['developer', 'super_admin'].includes(userRole);
        const INSTITUTION_NAME = <?php echo json_encode($inst['name']); ?>;
        const INSTITUTION_LOGO = <?php echo json_encode($inst['logo'] ? '../../assets/images/logos/'.$inst['logo'] : '../../assets/images/logo-placeholder.png'); ?>;
        const INSTITUTION_ADDRESS = <?php echo json_encode($inst['address'] ?? 'Official Academic Archive'); ?>;

        document.addEventListener('DOMContentLoaded', () => {
            if (typeof AccessControl !== 'undefined') {
                AccessControl.init(isAdmin, isGuest, isSuper);
            }

            // URL Parameter Handling for Auto-selection
            const urlParams = new URLSearchParams(window.location.search);
            const classId = urlParams.get('class_id');
            const sectionId = urlParams.get('section_id');
            const date = urlParams.get('date');

            if (classId && sectionId) {
                const sel = document.getElementById("daily_class_sel");
                if (sel) {
                    sel.value = classId + '|' + sectionId;
                    if (date) {
                        const dateInput = document.getElementById("daily_date");
                        if (dateInput) dateInput.value = date;
                    }
                    // Trigger load after a short delay to ensure components are ready
                    setTimeout(() => { if (typeof loadAdminDailyStudents === 'function') loadAdminDailyStudents(); }, 500);
                }
            }
        });
    </script>
    <script>
        console.log("Attendance Admin Script Loaded - 2026-01-26 Debug");

        document.addEventListener('DOMContentLoaded', () => {
            
            // Debug: Check dropdown existence
            const sel = document.getElementById("daily_class_sel");
            if (sel) {
                sel.addEventListener("change", (e) => {
                    console.log("Class dropdown changed -> value:", e.target.value);
                });
            }

            // NEW: Calendar View Toggle (Month/Year Selection)
            window.toggleCalendarView = function(dropdownId, view) {
                const dropdown = document.getElementById(dropdownId);
                dropdown.querySelector('.date-view').style.display = 'none';
                dropdown.querySelector('.month-view').style.display = 'none';
                dropdown.querySelector('.year-view').style.display = 'none';
                dropdown.querySelector('.' + view + '-view').style.display = 'block';
            };

            window.changeCalMonth = function(dropdownId, mNum, mName) {
                const dropdown = document.getElementById(dropdownId);
                dropdown.dataset.month = mNum;
                dropdown.querySelector('.cal-month').innerText = mName;
                refreshDateGrid(dropdownId);
                toggleCalendarView(dropdownId, 'date');
            };

            window.changeCalYear = function(dropdownId, year) {
                const dropdown = document.getElementById(dropdownId);
                dropdown.dataset.year = year;
                dropdown.querySelector('.cal-year').innerText = year;
                refreshDateGrid(dropdownId);
                toggleCalendarView(dropdownId, 'date');
            };

            function refreshDateGrid(dropdownId) {
                const dropdown = document.getElementById(dropdownId);
                const m = parseInt(dropdown.dataset.month);
                const y = parseInt(dropdown.dataset.year);
                const grid = dropdown.querySelector('.date-grid');
                
                // Calculate days in selected month/year
                const daysInMonth = new Date(y, m, 0).getDate();
                
                let html = '';
                const today = new Date();
                const isTodayMonth = today.getMonth() + 1 === m && today.getFullYear() === y;
                
                for(let d=1; d<=daysInMonth; d++) {
                    const isToday = (isTodayMonth && d === today.getDate()) ? 'today' : '';
                    const callback = dropdownId === 'student_cal_dropdown' ? 'selectAttendanceDate' : 'selectStaffDate';
                    html += `<div class='date-cell ${isToday}' onclick="${callback}(${d})">${d}</div>`;
                }
                grid.innerHTML = html;
            }

            // Click-to-Open Calendar Dropdown
            document.querySelectorAll('.calendar-trigger').forEach(trigger => {
                trigger.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const wrapper = trigger.closest('.calendar-wrapper');
                    const isOpen = wrapper.classList.contains('open');
                    
                    // Close others
                    document.querySelectorAll('.calendar-wrapper').forEach(w => w.classList.remove('open'));
                    
                    if (!isOpen) wrapper.classList.add('open');
                });
            });

            // Close on outside click
            window.addEventListener('click', () => {
                document.querySelectorAll('.calendar-wrapper').forEach(w => w.classList.remove('open'));
            });

            // Prevent closing when clicking inside dropdown
            document.querySelectorAll('.calendar-dropdown').forEach(dropdown => {
                dropdown.addEventListener('click', (e) => e.stopPropagation());
            });

            // EXPOSE GLOBAL SELECTORS
            window.selectAttendanceDate = function(day) {
                const dropdown = document.getElementById('student_cal_dropdown');
                const m = String(dropdown.dataset.month).padStart(2, '0');
                const y = dropdown.dataset.year;
                const dd = String(day).padStart(2, '0');
                const isoDate = `${y}-${m}-${dd}`;
                
                const input = document.getElementById('daily_date');
                if(input) input.value = isoDate;
                
                const display = document.getElementById('display_date_text');
                if(display) {
                    const dateObj = new Date(isoDate);
                    display.innerText = dateObj.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
                }
                loadAdminDailyStudents();
                document.getElementById('student_cal_dropdown').parentElement.classList.remove('open');
            };

            window.selectStaffDate = function(day) {
                const dropdown = document.getElementById('staff_cal_dropdown');
                const m = String(dropdown.dataset.month).padStart(2, '0');
                const y = dropdown.dataset.year;
                const dd = String(day).padStart(2, '0');
                const isoDate = `${y}-${m}-${dd}`;

                const input = document.getElementById('staff_date');
                if(input) input.value = isoDate;

                const display = document.getElementById('staff_display_date_text');
                if(display) {
                    const dateObj = new Date(isoDate);
                    display.innerText = dateObj.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
                }
                loadStaffAttendance();
                document.getElementById('staff_cal_dropdown').parentElement.classList.remove('open');
            };
        });

        let currentTabId = 'daily-view';

        function refreshAttendanceLogs() {
            console.log("Logs refresh requested...");
        }

        async function finishAttendance(mode) {
            const titleStr = mode === 'check_in' ? 'FINISH CHECK IN?' : 'FINISH CHECK OUT?';
            const textStr = mode === 'check_in' ? 
                "All users who have not scanned in today will be marked as ABSENT (Except those with pending/approved Leave Requests/Appeals)." :
                "Finish Check Out procedure will finalize today's attendance records. Any remaining unmarked users will be appropriately handled based on leave status.";
            
            const result = await Swal.fire({
                title: titleStr,
                text: textStr,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#e74c3c',
                cancelButtonColor: '#34495e',
                confirmButtonText: 'YES, PROCEED',
                background: '#1a1d21',
                color: '#fff'
            });

            if(result.isConfirmed) {
                try {
                    Swal.fire({
                        title: 'PROCESSING...',
                        text: 'Absenting unmarked users & processing leaves...',
                        allowOutsideClick: false,
                        didOpen: () => { Swal.showLoading(); },
                        background: '#1a1d21', color: '#fff'
                    });

                    const resp = await fetch(`attendance_api.php?action=finish_attendance_scan`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: `mode=${mode}`
                    });
                    const data = await resp.json();

                    if(data.success) {
                        Swal.fire({
                            title: 'SUCCESS',
                            text: data.message || 'Users processed successfully!',
                            icon: 'success',
                            background: '#1a1d21', color: '#fff'
                        });
                    } else {
                        Swal.fire({ title: 'ERROR', text: data.error || 'Failed', icon: 'error', background: '#1a1d21', color: '#fff' });
                    }
                } catch(e) {
                    Swal.fire({ title: 'ERROR', text: 'Network connection failed.', icon: 'error', background: '#1a1d21', color: '#fff' });
                }
            }
        }
        function switchTab(id, btn, access_id = null) {
            if (typeof GranularAccessControl !== 'undefined' && access_id) {
                if (!GranularAccessControl.checkAccess(access_id)) return;
            }
            console.log("Switching tab to:", id, "AccessID:", access_id);
            document.querySelectorAll('.content-sec').forEach(s => s.classList.remove('active'));
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            
            const target = document.getElementById(id);
            if(target) target.classList.add('active');
            if(btn) btn.classList.add('active');
            
            currentTabId = id;

            if (id === 'policy-view') {
                loadPolicyData();
            } else if (id === 'daily-view') {
                // If Daily View is opened, check which sub-tab is active
                if (document.getElementById('staff-att-panel').style.display === 'block') {
                    loadStaffAttendance();
                } else {
                    loadAdminDailyStudents();
                }
            }
        }


        function switchAttendanceSubTab(panelId, btn, access_id = null) {
            if (typeof GranularAccessControl !== 'undefined' && access_id) {
                if (!GranularAccessControl.checkAccess(access_id)) return;
            }
            document.querySelectorAll('#daily-view .attendance-panel').forEach(p => p.style.display = 'none');
            document.querySelectorAll('#daily-view .sub-tab-row .sub-tab-btn, #daily-view .triple-tab-row .sub-tab-btn').forEach(b => b.classList.remove('active'));
            
            const target = document.getElementById(panelId);
            if(target) target.style.display = 'block';
            if(btn) btn.classList.add('active');
            
            if(panelId === 'staff-att-panel') {
                if(typeof loadStaffAttendance === 'function') loadStaffAttendance();
            } else if(panelId === 'students-att-panel') {
                if(typeof loadAdminDailyStudents === 'function') loadAdminDailyStudents();
            }
        }

        function switchAnalysisSubTab(panelId, btn, access_id = null) {
            if (typeof GranularAccessControl !== 'undefined' && access_id) {
                if (!GranularAccessControl.checkAccess(access_id)) return;
            }
            document.querySelectorAll('#analysis-view .analysis-panel').forEach(p => p.style.display = 'none');
            document.querySelectorAll('#analysis-view .sub-tab-btn').forEach(b => b.classList.remove('active'));
            
            const target = document.getElementById(panelId);
            if(target) target.style.display = 'block';
            if(btn) btn.classList.add('active');
            
            if(panelId === 'staff-analysis-panel') {
                if(typeof loadStaffMonthlyAnalysis === 'function') loadStaffMonthlyAnalysis();
            } else {
                // Students panel doesn't auto-load for safety
            }
        }

        function switchSmartSubTab(panelId, btn, access_id = null) {
            if (typeof GranularAccessControl !== 'undefined' && access_id) {
                if (!GranularAccessControl.checkAccess(access_id)) return;
            }
            document.querySelectorAll('#smart-att-panel .smart-panel').forEach(p => p.style.display = 'none');
            document.querySelectorAll('#smart-att-panel .sub-tab-btn').forEach(b => b.classList.remove('active'));
            
            const target = document.getElementById(panelId);
            if(target) target.style.display = 'block';
            if(btn) btn.classList.add('active');
        }

        async function loadStaffMonthlyAnalysis() {
            const container = document.getElementById('staff-analysis-result');
            const month = document.getElementById('staff_analysis_month').value;
            const type = document.getElementById('staff_analysis_type').value;
            const year = new Date().getFullYear();
            
            container.innerHTML = '<div style="text-align:center; padding:30px; opacity:0.4; font-weight:700;"><i class="fas fa-sync fa-spin"></i> SYNCING STAFF METRICS...</div>';
            
            try {
                const res = await fetch(`attendance_api.php?action=get_staff_summary&month=${month}&year=${year}&staff_type=${type}`);
                const data = await res.json();
                
                if (data.success && data.staff) {
                    let html = `<div class="no-print" style="display:flex; justify-content:flex-end; margin-bottom:15px;">
                        <button class="btn theme-print-btn" onclick="printMonthlyLedger('staff-analysis-result', 'Staff Attendance Ledger')" style="width: auto !important; padding: 8px 15px; border: 1px solid currentColor; font-weight: 900; background: transparent;">
                            <i class="fas fa-print"></i>
                        </button>
                    </div>`;
                    html += '<div class="smart-ledger-container"><table class="ledger-table">';
                    html += `<thead><tr>
                        <th>S.No</th>
                        <th style="text-align:left;">NAME</th>
                        <th>POST</th>
                        <th>L.BRT</th>
                        <th>L.MTH</th>
                        <th>L.TOT</th>
                        <th>A.BRT</th>
                        <th>A.MTH</th>
                        <th>A.TOT</th>
                    </tr></thead><tbody>`;

                    data.staff.forEach(s => {
                        html += `<tr>
                            <td data-label="S.No">${s.sno}</td>
                            <td data-label="NAME">
                                <div style="text-align:left; font-weight:900;">${s.name}</div>
                            </td>
                            <td data-label="POST">
                                <span style="font-size:10px; opacity:0.8; font-weight:900; text-transform:uppercase;">${s.designation}</span>
                            </td>
                            <td data-label="L.BRT">
                                <span class="ledger-pill pill-blue">${s.l_brought}</span>
                            </td>
                            <td data-label="L.MTH">
                                <span class="ledger-pill pill-purple">${s.l_this}</span>
                            </td>
                            <td data-label="L.TOT">
                                <span class="ledger-pill pill-green" style="font-size:14px;">${s.l_total}</span>
                            </td>
                            <td data-label="A.BRT">
                                <span class="ledger-pill pill-orange">${s.a_brought}</span>
                            </td>
                            <td data-label="A.MTH">
                                <span class="ledger-pill pill-orange" style="background:rgba(231,76,60,0.1);">${s.a_this}</span>
                            </td>
                            <td data-label="A.TOT">
                                <span class="ledger-pill pill-red" style="font-size:14px; box-shadow: 0 4px 10px rgba(231,76,60,0.15);">${s.a_total}</span>
                            </td>
                        </tr>`;
                    });

                    html += '</tbody></table></div>';
                    container.innerHTML = html;
                    
                } else {
                    throw new Error(data.error || 'Failed to parse staff data');
                }
            } catch (e) {
                console.error(e);
                container.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171; font-weight:900;">
                    <i class="fas fa-exclamation-triangle"></i> FAILED TO SYNCHRONIZE STAFF LEDGER: ${e.message}
                </div>`;
            }
        }


        // --- ATTENDANCE INTELLIGENCE & STAFF ---




        function resetRecord(type, id) {
            const row = document.querySelector(`[data-sid="${id}"]${type === 'student' ? '.student-row' : '.staff-row-item'}`);
            if (row) {
                const input = row.querySelector(type === 'student' ? '.student-status-input' : '.staff-status-input');
                if (input) {
                    input.value = '';
                    row.querySelectorAll('.att-opt').forEach(opt => opt.classList.remove('active'));
                    const pill = row.querySelector('.status-pill');
                    if(pill) {
                        pill.className = 'status-pill bg-unknown';
                        pill.innerText = 'NOT MARKED';
                    }
                }
            }
        }

        async function deleteAttendance(type, id, name) {
            if (!confirm(`Are you sure you want to completely clear the attendance record for ${name}?`)) return;
            const date = (type === 'student') ? document.getElementById('daily_date').value : document.getElementById('staff_date').value;
            try {
                const res = await fetch(`attendance_api.php?action=delete_attendance&type=${type}&id=${id}&date=${date}`);
                const r = await res.json();
                if (r.success) {
                    alert('Attendance record removed successfully.');
                    if (type === 'student') loadAdminDailyStudents();
                    else loadStaffAttendance();
                } else {
                    alert('Error: ' + r.error);
                }
            } catch(e) {
                console.error(e);
                alert('Connection failed while deleting record.');
            }
        }

        function editAttendance(type, id) {
            let currentDate = type === 'student' ? document.getElementById('daily_date').value : document.getElementById('staff_date').value;
            openEditModal(type, id, currentDate);
        }

        async function openEditModal(type, id, date) {
            const res = await fetch(`attendance_api.php?action=get_single_entry&type=${type}&id=${id}&date=${date}`);
            const data = await res.json();
            
            const overlay = document.createElement('div');
            overlay.className = 'premium-modal-overlay';
            overlay.innerHTML = `
                <div class="premium-modal">
                    <div class="modal-header">
                        <h4 style="margin:0; font-size:14px; font-weight:900;">EDIT ${type.toUpperCase()} ATTENDANCE</h4>
                        <i class="fas fa-times" style="cursor:pointer;" onclick="this.closest('.premium-modal-overlay').remove()"></i>
                    </div>
                    <div class="modal-body">
                        <div style="text-align:center; margin-bottom:20px;">
                            <div style="font-size:18px; font-weight:800;">${data.name}</div>
                            <div style="font-size:11px; margin-top:4px; opacity: 0.7;">Select a date to view or change status</div>
                        </div>

                         <div style="background:rgba(0,0,0,0.03); padding:15px; border-radius:16px; border:1px solid rgba(0,0,0,0.05); margin-bottom:20px;">
                            <label style="display:block; font-size:10px; font-weight:800; margin-bottom:8px; text-transform:uppercase;">Attendance Date</label>
                            <input type="date" id="edit_modal_date" value="${date}" class="policy-input" style="height:44px;" onchange="refreshModalData('${type}', ${id}, this.value)">
                        </div>

                        <div style="display:flex; gap:15px; margin-bottom:20px;">
                            <div style="flex:1; background:rgba(0,0,0,0.03); padding:12px; border-radius:12px; border:1px solid rgba(0,0,0,0.05);">
                                <label style="display:block; font-size:9px; font-weight:800; margin-bottom:5px; opacity:0.6;">TIME IN</label>
                                <input type="time" id="edit_modal_time_in" value="${data.time_in ? data.time_in.substring(0,5) : ''}" style="width:100%; height:36px; border:none !important; border-radius: 0 !important; font-weight:700; padding:0 8px; background:transparent !important; color:inherit; box-shadow:none !important;">
                            </div>
                            <div style="flex:1; background:rgba(0,0,0,0.03); padding:12px; border-radius:12px; border:1px solid rgba(0,0,0,0.05);">
                                <label style="display:block; font-size:9px; font-weight:800; margin-bottom:5px; opacity:0.6;">TIME OUT</label>
                                <input type="time" id="edit_modal_time_out" value="${data.time_out ? data.time_out.substring(0,5) : ''}" style="width:100%; height:36px; border:none !important; border-radius: 0 !important; font-weight:700; padding:0 8px; background:transparent !important; color:inherit; box-shadow:none !important;">
                            </div>
                        </div>

                        <div style="display:flex; justify-content:center; gap:15px; padding:10px;">
                            <div class="att-opt opt-P ${data.status==='Present'?'active':''}" onclick="setModalStatus(this, 'Present')" title="Present" style="width:40px !important; height:40px !important; font-size:12px !important;">P</div>
                            <div class="att-opt opt-A ${data.status==='Absent'?'active':''}" onclick="setModalStatus(this, 'Absent')" title="Absent" style="width:40px !important; height:40px !important; font-size:12px !important;">A</div>
                            <div class="att-opt opt-L ${data.status==='Leave'?'active':''}" onclick="setModalStatus(this, 'Leave')" title="Leave" style="width:40px !important; height:40px !important; font-size:12px !important;">L</div>
                            <div class="att-opt opt-H ${data.status==='Holiday'?'active':''}" onclick="setModalStatus(this, 'Holiday')" title="Holiday" style="width:40px !important; height:40px !important; font-size:12px !important; background:rgba(241, 196, 15, 0.2); border-color:rgba(241, 196, 15, 0.4);">H</div>
                            <div class="att-opt opt-PH ${data.status==='PH'?'active':''}" onclick="setModalStatus(this, 'PH')" title="PH (Public Holiday)" style="width:40px !important; height:40px !important; font-size:12px !important; background:rgba(155, 89, 182, 0.2); border-color:rgba(155, 89, 182, 0.4);">PH</div>
                            ${type === 'student' ? `<div class="att-opt opt-S ${data.status==='Struck Off'?'active':''}" onclick="setModalStatus(this, 'Struck Off')" title="Struck Off" style="width:40px !important; height:40px !important; font-size:12px !important; background:rgba(239, 68, 68, 0.2); border-color:rgba(239, 68, 68, 0.4);">S</div>` : ''}
                        </div>
                        <input type="hidden" id="edit_modal_status" value="${data.status || ''}">
                    </div>
                    <div class="modal-footer">
                        <button class="btn" style="background:rgba(0,0,0,0.1); color:inherit; border:none; padding:10px 20px; font-size:12px;" onclick="this.closest('.premium-modal-overlay').remove()">Cancel</button>
                        <button class="btn" style="background:linear-gradient(135deg, #00b09b 0%, #96c93d 100%); border:none; padding:10px 20px; font-size:12px; color:white;" onclick="saveModalAttendance('${type}', ${id})">Update</button>
                    </div>
                </div>
            `;
            document.body.appendChild(overlay);
        }

        async function refreshModalData(type, id, date) {
            const res = await fetch(`attendance_api.php?action=get_single_entry&type=${type}&id=${id}&date=${date}`);
            const data = await res.json();
            const modalBody = document.querySelector('.premium-modal .modal-body');
            
            modalBody.querySelectorAll('.att-opt').forEach(opt => opt.classList.remove('active'));
            if(data.status) {
                const target = modalBody.querySelector(`.opt-${data.status}`);
                if(target) target.classList.add('active');
                else {
                    const sChar = data.status.charAt(0);
                    const targetChar = modalBody.querySelector(`.opt-${sChar}`);
                    if(targetChar) targetChar.classList.add('active');
                }
            }
            document.getElementById('edit_modal_status').value = data.status || '';
            document.getElementById('edit_modal_time_in').value = data.time_in ? data.time_in.substring(0,5) : '';
            document.getElementById('edit_modal_time_out').value = data.time_out ? data.time_out.substring(0,5) : '';
        }

        function setModalStatus(el, status) {
            el.parentElement.querySelectorAll('.att-opt').forEach(opt => opt.classList.remove('active'));
            el.classList.add('active');
            document.getElementById('edit_modal_status').value = status;
        }

        async function saveModalAttendance(type, id) {
            const date = document.getElementById('edit_modal_date').value;
            const status = document.getElementById('edit_modal_status').value;
            const time_in = document.getElementById('edit_modal_time_in').value;
            const time_out = document.getElementById('edit_modal_time_out').value;
            if(!status) { alert('Please select a status'); return; }

            const res = await fetch('attendance_api.php?action=save_single_entry', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ type, id, date, status, time_in, time_out })
            });
            const r = await res.json();
            if(r.success) {
                document.querySelector('.premium-modal-overlay').remove();
                if(type === 'student') loadAdminDailyStudents();
                else loadStaffAttendance();
            } else {
                alert('Update failed: ' + (r.error || 'Unknown error'));
            }
        }

        async function loadStaffAttendance() {
            const dateInput = document.getElementById('staff_date');
            const date = dateInput ? dateInput.value : '<?php echo date('Y-m-d'); ?>';
            const container = document.getElementById('staff-result');
            container.innerHTML = '<div style="text-align:center; padding:30px; letter-spacing: 1px; font-weight: 700;">SYNCING STAFF NEURAL PROFILES...</div>';
            
            try {
                const res = await fetch(`attendance_api.php?action=get_staff_attendance&date=${date}`);
                const data = await res.json();
                
                if (data.error) {
                    let msg = data.error;
                    if (data.details) msg += "\n\nDetails: " + data.details;
                    throw new Error(msg);
                }
                
                // Sort: Present on top, then others
                data.sort((a, b) => {
                    const stA = a.status || '';
                    const stB = b.status || '';
                    if (stA === 'Present' && stB !== 'Present') return -1;
                    if (stB === 'Present' && stA !== 'Present') return 1;
                    return 0;
                });

                let html = '';
                if (data.length === 0) {
                     html = '<div style="text-align:center; padding:20px; opacity:0.6;">No staff records found for this date.</div>';
                } else {
                    html += `<div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap:15px;">`;
                    data.forEach(s => {
                        let cur = s.status || ''; 
                        let statusClass = 'bg-unknown';
                        let statusLabel = 'NOT MARKED';
                        if(cur === 'Present') { statusClass = 'bg-present'; statusLabel = 'Present'; }
                        else if(cur === 'Absent') { statusClass = 'bg-absent'; statusLabel = 'Absent'; }
                        else if(cur === 'Leave') { statusClass = 'bg-leave'; statusLabel = 'Leave'; }
                        else if(cur === 'Holiday') { statusClass = 'bg-holiday'; statusLabel = 'Sunday (H)'; }
                        else if(cur === 'PH') { statusClass = 'bg-holiday'; statusLabel = 'PH (Public Holiday)'; }
                        else if(cur === 'Struck Off') { statusClass = 'bg-absent'; statusLabel = 'Struck Off'; }
                        let role = (s.role || s.edu_role || '').toLowerCase();
                        let desig = (s.designation || '').toLowerCase();

                        // Day Color Logic (Sunday=Red, Friday=Green for personal view)
                        let rowStyle = '';
                        if(s.is_personal && s.full_date) {
                            const dateObj = new Date(s.full_date);
                            const day = dateObj.getDay(); // 0=Sun, 5=Fri
                            if(day === 0) rowStyle = 'background: rgba(239, 68, 68, 0.15) !important; border-color: rgba(239, 68, 68, 0.3) !important;';
                            if(day === 5) rowStyle = 'background: #2c52a0 !important; color: #ffffff !important; border-color: #2c52a0 !important;';
                        }

                        // Time Display Logic
                        let timeInfo = '';
                        if(s.is_personal) {
                             if(s.time_in) timeInfo += `<span style="font-size:10px; font-weight:800; opacity:0.8; margin-right:8px;"><i class="fas fa-sign-in-alt text-success"></i> ${s.time_in}</span>`;
                             if(s.time_out) timeInfo += `<span style="font-size:10px; font-weight:800; opacity:0.8;"><i class="fas fa-sign-out-alt text-danger"></i> ${s.time_out}</span>`;
                        } else {
                             if(s.time_in) timeInfo = `<span style="font-size:8px; opacity:0.5; margin-left:5px;">${s.time_in}</span>`;
                        }

                        html += `
                            <div class="student-row staff-row-item" data-sid="${s.id}" data-role="${role}" data-desig="${desig}" data-type="${(s.user_type || '').toLowerCase()}" style="padding: 12px 18px !important; margin-bottom: 8px !important; display: flex; flex-direction: column; gap: 8px; position: relative; overflow: visible; ${rowStyle}">
                                <div class="hardware-tube left"></div>
                                <div class="hardware-tube right"></div>
                                
                                <!-- Line 1: Name and P/A/L Toggles -->
                                 <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                                    <div style="display:flex; align-items:center; gap:8px; flex: 1; overflow: hidden;">
                                        <div class="roll-badge" style="flex-shrink:0; background:rgba(255,255,255,0.05); font-size:9px;"><i class="fas fa-user-tie" style="font-size:10px; opacity:0.6;"></i></div>
                                        ${s.profile_pic ? 
                                            `<img src="../../assets/uploads/${s.profile_pic}" style="width:22px; height:22px; border-radius:50%; object-fit:cover; flex-shrink:0; border:1px solid rgba(0,0,0,0.1);" onerror="this.style.display='none'">` : ''
                                        }
                                        <div class="student-name" style="font-size:13px; font-weight:700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${s.name}</div>
                                    </div>
                                    <div class="att-options" style="display:flex; gap:4px; flex-shrink: 0; ${s.is_personal ? 'display:none;' : ''}">
                                        <input type="hidden" class="staff-status-input" data-sid="${s.id}" value="${cur}">
                                        <div class="att-opt opt-P ${cur=='Present'?'active':''}" title="Present" onclick="setStaffStatus(this, 'Present')" style="width:28px !important; height:28px !important; font-size:10px !important;">P</div>
                                        <div class="att-opt opt-A ${cur=='Absent'?'active':''}" title="Absent" onclick="setStaffStatus(this, 'Absent')" style="width:28px !important; height:28px !important; font-size:10px !important;">A</div>
                                        <div class="att-opt opt-L ${cur=='Leave'?'active':''}" title="Leave" onclick="setStaffStatus(this, 'Leave')" style="width:28px !important; height:28px !important; font-size:10px !important;">L</div>
                                        <div class="att-opt opt-H ${cur=='Holiday'?'active':''}" title="Holiday" onclick="setStaffStatus(this, 'Holiday')" style="width:28px !important; height:28px !important; font-size:10px !important; background:rgba(241, 196, 15, 0.2); border-color:rgba(241, 196, 15, 0.4);">H</div>
                                        <div class="att-opt opt-PH ${cur=='PH'?'active':''}" title="PH (Public Holiday)" onclick="setStaffStatus(this, 'PH')" style="width:28px !important; height:28px !important; font-size:10px !important; background:rgba(155, 89, 182, 0.2); border-color:rgba(155, 89, 182, 0.4);">PH</div>
                                        <div class="att-opt opt-S ${cur=='Struck Off'?'active':''}" title="Struck Off" onclick="setStaffStatus(this, 'Struck Off')" style="width:28px !important; height:28px !important; font-size:10px !important; background:rgba(239, 68, 68, 0.2); border-color:rgba(239, 68, 68, 0.4);">S</div>
                                    </div>
                                </div>
                                
                                <!-- Line 2: Stats Or Time Info -->
                                <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                                    
                                    <div style="display:flex; gap:8px; font-size:9px; font-weight:700; opacity:0.8; align-items: center; flex:1;">
                                        ${s.is_personal ? `
                                            <div style="display:flex; align-items:center; width:100%;">
                                                <span class="status-pill ${statusClass}" style="transform:scale(0.85); transform-origin:left; margin-right:10px;">${statusLabel}</span>
                                                ${timeInfo}
                                            </div>
                                        ` : `
                                            <div style="display:flex; align-items:center; gap:10px; width:100%;">
                                                <div style="display:flex; gap:4px;">
                                                    <span style="color:#2ecc71;">${s.m_p}P</span>
                                                    <span style="color:#e74c3c;">${s.m_a}A</span>
                                                    <span style="color:#f1c40f;">${s.m_l}L</span>
                                                </div>
                                                <span class="status-pill ${statusClass}" style="transform:scale(0.85); transform-origin:left;">${statusLabel}</span>
                                                
                                                <div style="display:flex; gap:8px; align-items:center;">
                                                    ${s.time_in ? `
                                                        <div class="status-pill bg-unknown" style="padding:0 12px; height:26px; display:flex; align-items:center; border-radius:50px !important; font-size:10px; font-weight:900;">
                                                            <i class="fas fa-clock" style="margin-right:5px; font-size:9px; opacity:0.5;"></i>
                                                            ${s.time_in.substring(0,5)}
                                                        </div>
                                                    ` : ''}
                                                    ${s.time_out ? `
                                                        <div class="status-pill bg-unknown" style="padding:0 12px; height:26px; display:flex; align-items:center; border-radius:50px !important; font-size:10px; font-weight:900;">
                                                            <i class="fas fa-history" style="margin-right:5px; font-size:9px; opacity:0.5;"></i>
                                                            ${s.time_out.substring(0,5)}
                                                        </div>
                                                    ` : ''}
                                                    <input type="hidden" class="time-in-input staff-time-in" value="${s.time_in ? s.time_in.substring(0,5) : ''}">
                                                    <input type="hidden" class="time-out-input staff-time-out" value="${s.time_out ? s.time_out.substring(0,5) : ''}">
                                                </div>
                                            </div>
                                        `}
                                    </div>

                                    <div style="display:flex; gap:15px; align-items:center; padding-right:25px; ${s.is_personal ? 'display:none;' : ''}">
                                        <i class="fas fa-undo" style="color:var(--text-secondary); cursor:pointer; font-size:12px; opacity:0.6;" onclick="resetRecord('staff', ${s.id})" title="Reset Today"></i>
                                        <i class="fas fa-edit" style="color:var(--accent); cursor:pointer; font-size:12px; opacity:0.6;" onclick="editAttendance('staff', ${s.id})" title="Edit Details"></i>
                                        <i class="fas fa-trash-alt" style="color:var(--danger); cursor:pointer; font-size:12px; opacity:0.6;" onclick="deleteAttendance('staff', ${s.id}, '${s.name.replace(/'/g, "\\'")}')" title="Delete Attendance"></i>
                                    </div>
                                </div>
                            </div>
                        `;
                    });
                    html += `</div>`;
                    // Floating Action Bar only for admins
                    if (!data[0].is_personal) {
                        html += `
                                 <div class="sticky-action-bar no-print">
                                    <div class="action-icon-btn icon-confirm" onclick="saveStaffAttendance()" title="Confirm Data">
                                        <i class="fas fa-check-circle"></i>
                                    </div>
                                    <div class="action-icon-btn icon-all" onclick="markAllPresent('staff-result')" title="Mark All Present">
                                        <i class="fas fa-check-double"></i>
                                    </div>
                                    <div class="action-icon-btn icon-whatsapp" onclick="shareAttendanceWhatsApp('staff')" title="Share to WhatsApp">
                                        <i class="fa-brands fa-whatsapp"></i>
                                    </div>
                                    <div class="action-icon-btn icon-print" onclick="printReport('staff-result', 'STAFF ATTENDANCE')" title="Print List">
                                        <i class="fas fa-print"></i>
                                    </div>
                                    <div class="action-icon-btn icon-reset" onclick="unmarkAll('staff-result')" title="Reset All">
                                        <i class="fas fa-undo"></i>
                                    </div>
                                </div>

                        `;
                    }
                }
                container.innerHTML = html;
                const saveBtn = document.getElementById('save_staff_btn_top');
                if(saveBtn) saveBtn.style.display = (data.length && !data[0].is_personal) ? 'block' : 'none';
                filterStaff();
            } catch (error) {
                console.error('Fetch error:', error);
                container.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171;">
                    <i class="fas fa-exclamation-triangle" style="margin-right:8px;"></i>
                    Error loading staff data: ${error.message}
                </div>`;
            }
        }

        function filterStaff() {
            const searchInput = document.getElementById('staff_search');
            const categorySel = document.getElementById('staff_category_filter');
            
            const term = searchInput ? searchInput.value.toLowerCase() : '';
            const filter = categorySel ? categorySel.value : '';
            
            const rows = document.querySelectorAll('#staff-result .staff-row-item');
            let foundCount = 0;

            rows.forEach(r => {
                const name = r.querySelector('.student-name').innerText.toLowerCase();
                const role = (r.dataset.role || '').toLowerCase();
                const desig = (r.dataset.desig || '').toLowerCase();
                const combined = role + ' ' + desig;
                
                // 1. Category Filter Logic
                let catShow = true;
                const utype = (r.dataset.type || '').toLowerCase();
                const isTeaching = utype === 'teaching' || 
                                 combined.includes('teacher') || 
                                 combined.includes('faculty') || 
                                 combined.includes('head') || 
                                 combined.includes('principal') || 
                                 combined.includes('lecturer') ||
                                 combined.includes('instructor') ||
                                 role.includes('teacher');

                if(filter === 'teaching') {
                    catShow = isTeaching;
                } else if(filter === 'visiting') {
                    catShow = combined.includes('visiting');
                } else if(filter === 'non_teaching') {
                    catShow = !isTeaching && !combined.includes('visiting');
                }
                
                // 2. Search Term Logic
                let nameShow = name.includes(term);
                
                // Apply combined visibility
                const show = catShow && nameShow;
                
                if (show) {
                    if (document.body.classList.contains('force-mobile')) {
                        r.style.setProperty('display', 'flex', 'important');
                    } else {
                        r.style.display = 'flex';
                    }
                    foundCount++;
                } else {
                    r.style.setProperty('display', 'none', 'important');
                }
            });
            console.log(`Filtered staff: ${foundCount} matches for "${term}" with category "${filter}"`);
        }

        function setStaffStatus(el, status) {
            const container = el.parentElement;
            container.querySelector('.staff-status-input').value = status;
            container.querySelectorAll('.att-opt').forEach(opt => {
                opt.classList.remove('active');
                opt.style.background = '';
                opt.style.color = '';
            });
            el.classList.add('active');
            
            if(status === 'Struck Off') {
                el.style.background = '#e11d48';
                el.style.color = 'white';
            } else if(status === 'Holiday') {
                el.style.background = '#9b59b6';
                el.style.color = 'white';
            }
        }


        async function saveStaffAttendance() {
            const date = document.getElementById('staff_date').value;
            const attendances = {};
            const bulk_in = document.getElementById('bulk_staff_time_in').value;
            const bulk_out = document.getElementById('bulk_staff_time_out').value;

            document.querySelectorAll('#staff-result .staff-row-item').forEach(row => { 
                const input = row.querySelector('.staff-status-input');
                if(!input) return;
                const sid = input.dataset.sid;
                const status = input.value;
                
                const isModeIn = document.getElementById('mode_staff_bulk_in') ? document.getElementById('mode_staff_bulk_in').checked : true;
                let t_in = row.querySelector('.time-in-input').value;
                let t_out = row.querySelector('.time-out-input').value;
                
                // Only fill/update the column that is currently 'active' in UI mode
                if (isModeIn) {
                    if(!t_in && bulk_in) t_in = bulk_in;
                } else {
                    if(!t_out && bulk_out) t_out = bulk_out;
                }

                attendances[sid] = {
                    status: status,
                    time_in: t_in,
                    time_out: t_out
                };
            });

            try {
                const res = await fetch('attendance_api.php?action=save_staff_attendance', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ date, attendances })
                });
                const r = await res.json();
                
                if (r.success) {
                    if (r.request_pending) {
                        Swal.fire({
                            title: 'REQUEST SENT',
                            text: r.message || 'Staff attendance already submitted. Request sent for approval.',
                            icon: 'info',
                            confirmButtonColor: '#3498db',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                    } else {
                        Swal.fire({
                            title: 'SUCCESS',
                            text: 'Staff Attendance Confirmed successfully!',
                            icon: 'success',
                            confirmButtonColor: '#2ecc71',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                    }
                    loadStaffAttendance(); 
                } else {
                    Swal.fire({
                        title: 'ERROR',
                        text: r.error || 'Unknown error',
                        icon: 'error',
                        confirmButtonColor: '#e74c3c',
                        background: '#1a1d21',
                        color: '#fff'
                    });
                }
            } catch (e) {
                console.error(e);
                Swal.fire({
                    title: 'NETWORK ERROR',
                    text: 'Failed to connect to server.',
                    icon: 'error',
                    background: '#1a1d21',
                    color: '#fff'
                });
            }
        }




        async function loadAdminDailyStudents() {
            const val = document.getElementById('daily_class_sel').value;
            const dateInput = document.getElementById('daily_date');
            const date = dateInput ? dateInput.value : '<?php echo date('Y-m-d'); ?>';
            if(!val) return;
            const [cid, sid] = val.split('|');

            document.getElementById('student-marking-container').innerHTML = '<div style="text-align:center; padding:30px; letter-spacing: 1px; font-weight: 700;">MAPPING STUDENT BIOMETRICS...</div>';

            try {
                const res = await fetch(`attendance_api.php?action=get_students&class_id=${cid}&section_id=${sid}&date=${date}`);
                const data = await res.json();
                
                if (data.error) {
                    let msg = data.error;
                    if (data.details) msg += "\n\nDetails: " + data.details;
                    if (data.file) msg += "\nFile: " + data.file;
                    if (data.line) msg += "\nLine: " + data.line;
                    if (data.db_error) msg += "\n\nDB Detail: " + data.db_error;
                    
                    if (msg.includes("Unknown column 'e.status'")) {
                        msg = "Critical: Database columns are missing.\n\nPlease visit this URL to fix:\nhttp://192.168.52.196/WANTUCH/modules/education/migrate_attendance_v2.php";
                    }
                    throw new Error(msg);
                }
                
                // Sort: Present on top, then others
                data.sort((a, b) => {
                    const stA = a.status || '';
                    const stB = b.status || '';
                    if (stA === 'Present' && stB !== 'Present') return -1;
                    if (stB === 'Present' && stA !== 'Present') return 1;
                    return 0;
                });

                let html = '';
                if (data.length === 0) {
                     html = '<div style="text-align:center; padding:20px; opacity:0.6;">No students found in this class.</div>';
                } else {
                    html += `<div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap:15px;">`;
                    data.forEach(s => {
                        let cur = s.status || ''; // By default all will uncheck if no status
                        let h_active = parseInt(s.haircut_fine) ? 'active' : '';
                        let b_active = parseInt(s.rules_break_fine) ? 'active' : '';
                        
                        let statusClass = 'bg-unknown';
                        let statusLabel = 'NOT MARKED';
                        if(cur === 'Present') { statusClass = 'bg-present'; statusLabel = 'Present'; }
                        else if(cur === 'Absent') { statusClass = 'bg-absent'; statusLabel = 'Absent'; }
                        else if(cur === 'Leave') { statusClass = 'bg-leave'; statusLabel = 'Leave'; }
                        else if(cur === 'Holiday') { statusClass = 'bg-holiday'; statusLabel = 'Sunday (H)'; }
                        else if(cur === 'PH') { statusClass = 'bg-holiday'; statusLabel = 'PH (Public Holiday)'; }
                        else if(cur === 'Struck Off') { statusClass = 'bg-absent'; statusLabel = 'Struck Off'; }

                        html += `
                            <div class="student-row" data-sid="${s.id}" style="padding: 12px 18px !important; margin-bottom: 8px !important; display: flex; flex-direction: column; gap: 8px; position: relative; overflow: visible;">
                                <div class="hardware-tube left"></div>
                                <div class="hardware-tube right"></div>

                                <!-- Line 1: Roll/Name and P/A/L/S Toggles -->
                                <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                                    <div style="display:flex; align-items:center; gap:8px; flex: 1; overflow: hidden;">
                                        <div class="roll-badge" style="flex-shrink:0;">${s.class_no || s.roll || '-'}</div>
                                        ${s.profile_pic ? 
                                            `<img src="../../assets/uploads/${s.profile_pic}" style="width:22px; height:22px; border-radius:50%; object-fit:cover; flex-shrink:0; border:1px solid rgba(0,0,0,0.1);" onerror="this.style.display='none'">` : ''
                                        }
                                        <div class="student-name" style="font-size:13px; font-weight:700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${s.name}</div>
                                    </div>
                                    <div class="att-options" style="display:flex; gap:4px; flex-shrink: 0;">
                                        <input type="hidden" class="student-status-input" data-sid="${s.id}" value="${cur}">
                                        <div class="att-opt opt-P ${cur=='Present'?'active':''}" onclick="setAdminAttStatus(this, 'Present')" style="width:28px !important; height:28px !important; font-size:10px !important;">P</div>
                                        <div class="att-opt opt-A ${cur=='Absent'?'active':''}" onclick="setAdminAttStatus(this, 'Absent')" style="width:28px !important; height:28px !important; font-size:10px !important;">A</div>
                                        <div class="att-opt opt-L ${cur=='Leave'?'active':''}" onclick="setAdminAttStatus(this, 'Leave')" style="width:28px !important; height:28px !important; font-size:10px !important;">L</div>
                                        <div class="att-opt opt-H ${cur=='Holiday'?'active':''}" onclick="setAdminAttStatus(this, 'Holiday')" style="width:28px !important; height:28px !important; font-size:10px !important; background:rgba(241, 196, 15, 0.2); border-color:rgba(241, 196, 15, 0.4);" title="Holiday">H</div>
                                        <div class="att-opt opt-PH ${cur=='PH'?'active':''}" onclick="setAdminAttStatus(this, 'PH')" style="width:28px !important; height:28px !important; font-size:10px !important; background:rgba(155, 89, 182, 0.2); border-color:rgba(155, 89, 182, 0.4);" title="PH (Public Holiday)">PH</div>
                                        <div class="att-opt opt-S ${cur=='Struck Off'?'active':''}" onclick="setAdminAttStatus(this, 'Struck Off')" style="width:28px !important; height:28px !important; font-size:10px !important; background:rgba(239, 68, 68, 0.2); border-color:rgba(239, 68, 68, 0.4);" title="Struck Off">S</div>
                                    </div>
                                </div>

                                <!-- Line 2: Stats and Action Icons -->
                                <div class="no-print" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                                    <div style="display:flex; gap:8px; font-size:9px; font-weight:800; opacity:0.8; align-items: center;">
                                        <span style="color:#2ecc71;">${s.m_p}P</span>
                                        <span style="color:#e74c3c;">${s.m_a}A</span>
                                        <span style="color:#f1c40f;">${s.m_l}L</span>
                                         <span class="status-pill ${statusClass}" style="transform:scale(0.85); transform-origin:left; margin-left:4px;">${statusLabel}</span>
                                    </div>
                                    
                                     <!-- Time In/Out Labels (Pill style like 'Not marked') -->
                                     <div style="display:flex; gap:8px; align-items:center;">
                                         ${s.time_in ? `
                                            <div class="status-pill bg-unknown" style="padding:0 12px; height:26px; display:flex; align-items:center; border-radius:50px !important; font-size:10px; font-weight:900;">
                                                <i class="fas fa-clock" style="margin-right:5px; font-size:9px; opacity:0.5;"></i>
                                                ${s.time_in.substring(0,5)}
                                            </div>
                                         ` : ''}
                                         ${s.time_out ? `
                                            <div class="status-pill bg-unknown" style="padding:0 12px; height:26px; display:flex; align-items:center; border-radius:50px !important; font-size:10px; font-weight:900;">
                                                <i class="fas fa-history" style="margin-right:5px; font-size:9px; opacity:0.5;"></i>
                                                ${s.time_out.substring(0,5)}
                                            </div>
                                         ` : ''}
                                         <input type="hidden" class="time-in-input" value="${s.time_in ? s.time_in.substring(0,5) : ''}">
                                         <input type="hidden" class="time-out-input" value="${s.time_out ? s.time_out.substring(0,5) : ''}">
                                     </div>

                                    <div style="display:flex; gap:15px; align-items:center; padding-right:25px;">
                                        <i class="fas fa-undo" style="color:var(--text-secondary); cursor:pointer; font-size:12px; opacity:0.6;" onclick="resetRecord('student', ${s.id})" title="Reset Today"></i>
                                        <i class="fas fa-edit" style="color:var(--accent); cursor:pointer; font-size:12px; opacity:0.6;" onclick="editAttendance('student', ${s.id})" title="Edit Details"></i>
                                        <i class="fas fa-trash-alt" style="color:var(--danger); cursor:pointer; font-size:12px; opacity:0.6;" onclick="deleteAttendance('student', ${s.id}, '${s.name.replace(/'/g, "\\'")}')" title="Delete Attendance"></i>
                                    </div>
                                </div>
                            </div>
                        `;
                    });
                    html += '</div>';
                    // Floating Action Bar at the bottom
                    html += `
                        <div class="sticky-action-bar no-print">
                            <div class="action-icon-btn icon-confirm" onclick="saveAdminStudentAttendance()" title="Confirm Data">
                                <i class="fas fa-check-circle"></i>
                            </div>
                            <div class="action-icon-btn icon-all" onclick="markAllPresent('student-marking-container')" title="Mark All Present">
                                <i class="fas fa-check-double"></i>
                            </div>
                            <div class="action-icon-btn icon-whatsapp" onclick="shareAttendanceWhatsApp('student')" title="Share to WhatsApp">
                                <i class="fa-brands fa-whatsapp"></i>
                            </div>
                            <div class="action-icon-btn icon-print" onclick="printReport('student-marking-container', 'STUDENT ATTENDANCE')" title="Print List">
                                <i class="fas fa-print"></i>
                            </div>
                            <div class="action-icon-btn icon-reset" onclick="unmarkAll('student-marking-container')" title="Reset All">
                                <i class="fas fa-undo"></i>
                            </div>
                        </div>

                    `;
                }
                document.getElementById('student-marking-container').innerHTML = html;
                filterStudents(); // Apply filter after loading students
            } catch (error) {
                console.error('Fetch error:', error);
                alert("Server Error:\n" + error.message);
                document.getElementById('student-marking-container').innerHTML = '<div style="text-align:center; padding:20px; color:#f87171;">Server Error: ' + error.message + '</div>';
            }
        }

        function setAdminAttStatus(el, status) {
            const container = el.parentElement;
            container.querySelector('.student-status-input').value = status;
            container.querySelectorAll('.att-opt').forEach(opt => {
                opt.classList.remove('active');
                opt.style.transform = 'scale(1)';
                opt.style.boxShadow = 'none';
            });
            el.classList.add('active');
            el.style.transform = 'scale(1.2)';
            el.style.boxShadow = '0 0 15px rgba(255,255,255,0.1)';
            
            if(status === 'Struck Off') {
                el.style.background = '#e11d48';
                el.style.color = 'white';
            } else if(status === 'Holiday') {
                el.style.background = '#9b59b6';
                el.style.color = 'white';
            }
        }
        
        async function shareAttendanceWhatsApp(type) {
            const containerId = type === 'student' ? 'student-marking-container' : 'staff-result';
            const container = document.getElementById(containerId);
            const rows = container.querySelectorAll(type === 'student' ? '.student-row' : '.staff-row-item');
            
            let total = 0;
            let present = 0;
            let absent = 0;
            let leave = 0;
            let holiday = 0;
            let unmarked = 0;
            
            let recordData = [];

            rows.forEach(row => {
                const input = row.querySelector(type === 'student' ? '.student-status-input' : '.staff-status-input');
                if(!input) return;
                
                total++;
                const status = input.value || 'Not Marked';
                
                let nameEl = row.querySelector(type === 'student' ? '.student-name' : 'strong');
                let name = nameEl ? nameEl.innerText.trim() : 'Unknown';
                
                let rollEl = row.querySelector('.roll-badge');
                let roll = rollEl ? rollEl.innerText.trim() : '-';

                if(status === 'Present') present++;
                else if(status === 'Absent' || status === 'Struck Off') absent++;
                else if(status === 'Leave') leave++;
                else if(status === 'Holiday') holiday++;
                else unmarked++;
                
                recordData.push({ roll: type === 'student' ? roll : '-', name: name, status: status });
            });

            if(total === 0) {
                alert('No records to share.');
                return;
            }

            const dateVal = document.getElementById(type === 'student' ? 'daily_date' : 'staff_date').value;
            const dateStr = new Date(dateVal).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
            
            let classHeader = '';
            let pdfTitle = '';
            
            if(type === 'student') {
                const sel = document.getElementById('daily_class_sel');
                classHeader = `*Class:* ${sel.options[sel.selectedIndex].text}\n`;
                pdfTitle = `STUDENT ATTENDANCE - ${sel.options[sel.selectedIndex].text}`;
            } else {
                pdfTitle = `STAFF DAILY ATTENDANCE`;
            }

            // PDF Generation Logic
            const wrapper = document.createElement('div');
            wrapper.style.width = '21cm'; // A4 Portrait for PDF
            wrapper.style.padding = "40px";
            wrapper.style.background = "#fff";
            wrapper.style.color = '#1e293b';
            wrapper.style.fontFamily = "'Inter', sans-serif";
            
            let tableRowsHtml = '';
            recordData.forEach(r => {
                tableRowsHtml += `<tr>
                    <td style="text-align:center; padding: 8px;">${r.roll}</td>
                    <td style="text-align:left; padding: 8px;">${r.name}</td>
                    <td style="text-align:center; padding: 8px; font-weight: bold;">${r.status}</td>
                </tr>`;
            });

            const wpInstName = "<?php echo addslashes($inst['name'] ?? 'Institution'); ?>";
            const wpInstLogo = "../../<?php echo addslashes($inst['logo_path'] ?? ''); ?>";
            const wpLink = document.createElement('a');
            wpLink.href = wpInstLogo;
            const absoluteLogo = wpLink.href;
            
            wrapper.innerHTML = `
                <style>
                    #pdf-report-container * {
                        color: inherit !important;
                        background-color: transparent !important;
                        border-color: inherit !important;
                    }
                    #pdf-report-container {
                        background-color: #ffffff !important;
                        color: #1e293b !important;
                    }
                    #pdf-report-container h1 { color: #2c52a0 !important; }
                    #pdf-report-container h2 { color: #1e293b !important; }
                    #pdf-report-container p { color: #64748b !important; }
                    #pdf-report-container .stat-tot { background-color: #e2e8f0 !important; color: #1e293b !important; }
                    #pdf-report-container .stat-pre { background-color: #dcfce7 !important; color: #166534 !important; }
                    #pdf-report-container .stat-abs { background-color: #fee2e2 !important; color: #991b1b !important; }
                    #pdf-report-container .stat-lev { background-color: #e0f2fe !important; color: #075985 !important; }
                    #pdf-report-container .stat-hol { background-color: rgba(155, 89, 182, 0.1) !important; color: #9b59b6 !important; }
                    #pdf-report-container table { border: 1px solid #cbd5e1 !important; table-layout: fixed; }
                    #pdf-report-container th { background-color: #f1f5f9 !important; color: #1e293b !important; border: 1px solid #cbd5e1 !important; }
                    #pdf-report-container td { color: #1e293b !important; border: 1px solid #cbd5e1 !important; }
                </style>
                <div id="pdf-report-container">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #2c52a0; padding-bottom: 15px; margin-bottom: 20px;">
                        <div>
                            <h1 style="margin: 0; font-size: 22px; font-weight: 900; text-transform: uppercase;">${wpInstName}</h1>
                            <p style="margin: 5px 0 0; font-size: 12px; font-weight: bold;">DAILY ATTENDANCE LOG</p>
                        </div>
                        <img src="${absoluteLogo}" style="height: 50px; object-fit: contain;" onerror="this.onerror=null; this.style.display='none';">
                    </div>
                    
                    <div style="margin-bottom: 20px;">
                        <h2 style="margin: 0; font-size: 16px; font-weight: bold; text-transform: uppercase;">${pdfTitle}</h2>
                        <p style="margin: 5px 0; font-size: 12px;">Date: ${dateStr}</p>
                    </div>

                    <div style="display: flex; gap: 15px; margin-bottom: 20px; font-size: 12px; font-weight: 800;">
                        <div class="stat-tot" style="padding: 6px 12px; border-radius: 4px;">TOTAL: ${total}</div>
                        <div class="stat-pre" style="padding: 6px 12px; border-radius: 4px;">PRESENT: ${present}</div>
                        <div class="stat-abs" style="padding: 6px 12px; border-radius: 4px;">ABSENT: ${absent}</div>
                        <div class="stat-lev" style="padding: 6px 12px; border-radius: 4px;">LEAVE: ${leave}</div>
                        <div class="stat-hol" style="padding: 6px 12px; border-radius: 4px;">HOLIDAY: ${holiday}</div>
                    </div>

                    <table style="width: 100%; border-collapse: collapse; font-size: 12px;">
                        <thead>
                            <tr>
                                <th style="width: 15%; padding: 10px; text-align: center;">ID/Roll</th>
                                <th style="width: 60%; padding: 10px; text-align: left;">Name</th>
                                <th style="width: 25%; padding: 10px; text-align: center;">Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRowsHtml}
                        </tbody>
                    </table>
                    
                    <div style="margin-top: 30px; font-size: 10px; text-align: center; border-top: 1px solid #e2e8f0; padding-top: 10px;">
                        Generated via Attendance Intelligence Platform
                    </div>
                </div>
            `;

            let shareableUrl = '';
            
            try {
                // Show Loading
                Swal.fire({
                    title: 'PREPARING PDF...',
                    text: 'Compiling register into secure document...',
                    allowOutsideClick: false,
                    didOpen: () => { Swal.showLoading(); },
                    background: '#1a1d21', color: '#fff'
                });

                document.body.appendChild(wrapper);

                const options = {
                    margin: 0.5,
                    filename: `Att_Report_${dateStr.replace(/ /g, '_')}.pdf`,
                    image: { type: 'jpeg', quality: 0.98 },
                    html2canvas: { scale: 2, useCORS: true, scrollY: 0, backgroundColor: '#ffffff' },
                    jsPDF: { unit: 'in', format: 'a4', orientation: 'portrait' }
                };

                const originalScrollY = window.scrollY;
                window.scrollTo(0, 0);

                const pdfBase64 = await html2pdf().set(options).from(wrapper).output('datauristring');
                
                window.scrollTo(0, originalScrollY);
                document.body.removeChild(wrapper);

                // Local Download
                const link = document.createElement('a');
                link.href = pdfBase64;
                link.download = options.filename;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

                // Save to server
                const base64Content = pdfBase64.split(',')[1];
                const resp = await fetch('attendance_api.php?action=save_summary_pdf', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        pdf_data: base64Content,
                        filename: options.filename
                    })
                });
                
                const res = await resp.json();
                Swal.close();

                if (res.success) {
                    shareableUrl = res.url;
                } else {
                    console.error('Server failed to save PDF:', res.error);
                }
            } catch (err) {
                Swal.close();
                console.error("PDF generation failed:", err);
                if(wrapper.parentNode) document.body.removeChild(wrapper);
            }

            let message = `*📢 ${type.toUpperCase()} ATTENDANCE SUMMARY*\n`;
            message += `*Date:* ${dateStr}\n`;
            message += classHeader;
            message += `----------------------------\n`;
            message += `✅ *Present:* ${present}\n`;
            message += `❌ *Absent:* ${absent}\n`;
            message += `📝 *Leave:* ${leave}\n`;
            message += `🎉 *Holiday:* ${holiday}\n`;
            if(unmarked > 0) message += `⏳ *Not Marked:* ${unmarked}\n`;
            message += `📊 *Total:* ${total}\n`;
            message += `----------------------------\n`;
            if (shareableUrl) {
                message += `📄 *Download Full Report:* ${shareableUrl}\n`;
                message += `----------------------------\n`;
            }
            message += `_Sent via Attendance Intelligence_`;

            const encodedMsg = encodeURIComponent(message);
            window.open(`https://api.whatsapp.com/send?text=${encodedMsg}`, '_blank');
        }
 
        async function shareSummaryPDFWhatsApp() {
            const isStudent = document.getElementById('report-student-panel').style.display !== 'none';
            const tableId = isStudent ? 'student-report-print-area' : 'staff-report-print-area';
            const title = isStudent ? 'STUDENT ATTENDANCE SUMMARY' : 'STAFF ATTENDANCE SUMMARY';
            const date = new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
            const qr_url = "https://quickchart.io/qr?text=" + encodeURIComponent(window.location.href) + "&size=100";
            
            const tableElement = document.getElementById(tableId);
            if(!tableElement) {
                alert('Table data not found.');
                return;
            }

            // Construct the PDF Content HTML (Similar to printAttendanceSummary but for html2pdf)
            const wrapper = document.createElement('div');
            wrapper.style.padding = "20px";
            wrapper.style.backgroundColor = "#fff";
            wrapper.innerHTML = `
                <div style="font-family: 'Inter', sans-serif; padding: 20px; border: 1px solid #ddd;">
                    <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #2c52a0; padding-bottom: 10px; margin-bottom: 20px;">
                        <div>
                            <h1 style="margin: 0; color: #2c52a0; font-size: 24px;">${INSTITUTION_NAME}</h1>
                            <p style="margin: 5px 0 0; font-size: 12px; color: #64748b;">OFFICIAL ACADEMIC ARCHIVE</p>
                        </div>
                        <img src="${INSTITUTION_LOGO}" style="height: 60px;" onerror="this.style.display='none'">
                    </div>
                    
                    <div style="margin-bottom: 20px;">
                        <h2 style="margin: 0; font-size: 18px; color: #1e293b;">${title}</h2>
                        <p style="margin: 5px 0; font-size: 12px; color: #64748b;">Generated on: ${date}</p>
                    </div>

                    <style>
                        table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 10px; }
                        th { background: #f1f5f9; color: #1e293b; padding: 8px 4px; border: 1px solid #e2e8f0; font-weight: 800; text-align: center; }
                        td { padding: 8px 4px; border: 1px solid #e2e8f0; text-align: center; color: #334155; }
                        tfoot td { background: #f8fafc; font-weight: 900; border-top: 2px solid #2c52a0; }
                    </style>

                    ${tableElement.outerHTML}

                    <div style="margin-top: 30px; display: flex; justify-content: space-between; align-items: flex-end;">
                        <div style="text-align: center;">
                            <div style="width: 150px; border-top: 1px dashed #000; margin-bottom: 5px;"></div>
                            <p style="margin: 0; font-size: 10px; font-weight: 800;">ADMINISTRATOR</p>
                        </div>
                        <img src="${qr_url}" style="width: 60px; height: 60px; border: 1px solid #eee;">
                    </div>
                </div>
            `;

            // Use html2pdf to generate Base64
            const options = {
                margin: 0.5,
                filename: `Attendance_Summary_${date.replace(/ /g, '_')}.pdf`,
                image: { type: 'jpeg', quality: 0.98 },
                html2canvas: { scale: 2, useCORS: true },
                jsPDF: { unit: 'in', format: 'letter', orientation: 'portrait' }
            };

            // Show Loading
            Swal.fire({
                title: 'GENERATING PDF...',
                text: 'Preparing official document with neural fonts...',
                allowOutsideClick: false,
                didOpen: () => { Swal.showLoading(); },
                background: '#1a1d21', color: '#fff'
            });

            document.body.appendChild(wrapper);
            wrapper.style.position = 'absolute';
            wrapper.style.left = '-9999px';

            try {
                // Generate PDF as blob or base64
                const pdfBase64 = await html2pdf().set(options).from(wrapper).output('datauristring');
                document.body.removeChild(wrapper);

                // Trigger local download
                const link = document.createElement('a');
                link.href = pdfBase64;
                link.download = options.filename;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

                const base64Content = pdfBase64.split(',')[1];

                // Send to server to save
                const resp = await fetch('attendance_api.php?action=save_summary_pdf', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        pdf_data: base64Content,
                        filename: `summary_${Date.now()}.pdf`
                    })
                });
                
                const res = await resp.json();
                Swal.close();

                if (res.success) {
                    const pdfUrl = res.url;
                    const message = `*📢 ATTENDANCE SUMMARY REPORT*\n` +
                                    `*Type:* ${title}\n` +
                                    `*Date:* ${date}\n` +
                                    `----------------------------\n` +
                                    `📄 *Download PDF:* ${pdfUrl}\n` +
                                    `----------------------------\n` +
                                    `_Sent via Attendance Intelligence_`;
                    
                    const encodedMsg = encodeURIComponent(message);
                    window.open(`https://api.whatsapp.com/send?text=${encodedMsg}`, '_blank');
                } else {
                    alert('Server failed to save PDF: ' + res.error);
                }
            } catch (err) {
                Swal.close();
                console.error(err);
                alert('PDF Generation failed: ' + err.message);
            }
        }

        function shareLedgerWhatsApp() {
            const container = document.getElementById('report-result');
            const summaryGrid = container.querySelector('.summary-grid');
            if(!summaryGrid) {
                alert('No summary data found to share.');
                return;
            }

            const sel = document.getElementById('avg_class_sel');
            const monthSel = document.getElementById('avg_month');
            const className = sel.options[sel.selectedIndex].text;
            const monthName = monthSel.options[monthSel.selectedIndex].text;

            // Extract data from summary grid
            const divs = summaryGrid.querySelectorAll('div[style*="background"]');
            let data = {};
            divs.forEach(div => {
                const labelDiv = div.querySelector('div[style*="font-size:10px"]');
                const valDiv = div.querySelector('div[style*="font-size:22px"], div[style*="font-size:24px"], div[style*="font-size:32px"]');
                if(labelDiv && valDiv) {
                    data[labelDiv.innerText.trim()] = valDiv.innerText.trim();
                }
            });

            let message = `*📊 MONTHLY ATTENDANCE SUMMARY*\n`;
            message += `*Class:* ${className}\n`;
            message += `*Month:* ${monthName}\n`;
            message += `----------------------------\n`;
            for(let key in data) {
                message += `*${key}:* ${data[key]}\n`;
            }
            message += `----------------------------\n`;
            message += `_Sent via Attendance Intelligence_`;

            const encodedMsg = encodeURIComponent(message);
            window.open(`https://api.whatsapp.com/send?text=${encodedMsg}`, '_blank');
        }


        function toggleViolation(el, type) {
            el.classList.toggle('active');
        }

        function markAllPresent(containerId) {
            const container = document.getElementById(containerId);
            const rows = container.querySelectorAll('.student-row');
            rows.forEach(row => {
                const btnP = row.querySelector('.opt-P');
                if (btnP && !btnP.classList.contains('active')) {
                    btnP.click();
                }
            });
        }

        function unmarkAll(containerId) {
            const container = document.getElementById(containerId);
            const rows = container.querySelectorAll('.student-row');
            rows.forEach(row => {
                const input = row.querySelector('.student-status-input') || row.querySelector('.staff-status-input');
                if (input) input.value = '';
                row.querySelectorAll('.att-opt').forEach(opt => {
                    opt.classList.remove('active');
                    opt.style.transform = 'scale(1)';
                    opt.style.boxShadow = 'none';
                    // Reset custom colors if any (like Struck Off)
                    if (opt.classList.contains('opt-S')) {
                         opt.style.background = 'rgba(239, 68, 68, 0.2)';
                         opt.style.color = '';
                    } else if (opt.classList.contains('opt-H')) {
                         opt.style.background = 'rgba(155, 89, 182, 0.2)';
                         opt.style.color = '';
                    }
                });
                // Reset status pills for students
                const pill = row.querySelector('.status-pill');
                if (pill) {
                    pill.className = 'status-pill bg-unknown';
                    pill.innerText = 'NOT MARKED';
                }
            });
        }



        function syncBulkMode(type, mode) {
            // No longer syncing row radios as they are removed for a cleaner label-only view.
            // Row data will update on save/load.
        }


        async function saveAdminStudentAttendance() {
            const val = document.getElementById('daily_class_sel').value;
            const date = document.getElementById('daily_date').value;
            if(!val) { alert('Select class first'); return; }
            const [cid, sid] = val.split('|');

            const status_data = {};
            document.querySelectorAll('.student-row').forEach(row => {
                const s_input = row.querySelector('.student-status-input');
                if (s_input) {
                    const sid = s_input.dataset.sid;
                    const status = s_input.value;
                    const register = 0;
                    
                    const isModeIn = document.getElementById('mode_std_bulk_in') ? document.getElementById('mode_std_bulk_in').checked : true;
                    let t_in = row.querySelector('.time-in-input').value;
                    let t_out = row.querySelector('.time-out-input').value;
                    
                    const bulk_in = document.getElementById('bulk_student_time_in').value;
                    const bulk_out = document.getElementById('bulk_student_time_out').value;
                    
                    if (isModeIn) {
                        if(!t_in && bulk_in) t_in = bulk_in;
                    } else {
                        if(!t_out && bulk_out) t_out = bulk_out;
                    }

                    status_data[sid] = {
                        status: status,
                        haircut: 0,
                        register: register,
                        rules: 0,
                        time_in: t_in,
                        time_out: t_out
                    };
                }
            });

            if(Object.keys(status_data).length === 0) return;

            try {
                const res = await fetch(`attendance_api.php?action=save_student_attendance_bulk`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        class_id: cid,
                        section_id: sid,
                        date: date,
                        status: status_data
                    })
                });
                const r = await res.json();
                
                if (r.success) {
                    if (r.request_pending) {
                        Swal.fire({
                            title: 'REQUEST SENT',
                            text: r.message || 'Attendance already submitted. Request sent to admin.',
                            icon: 'info',
                            confirmButtonColor: '#3498db',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                    } else {
                        Swal.fire({
                            title: 'SUCCESS',
                            text: 'Attendance Confirmed successfully!',
                            icon: 'success',
                            confirmButtonColor: '#2ecc71',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                    }
                    loadAdminDailyStudents();
                } else {
                    Swal.fire({
                        title: 'ERROR',
                        text: (r.error || 'Unknown error') + (r.details ? '\n' + r.details : ''),
                        icon: 'error',
                        confirmButtonColor: '#e74c3c',
                        background: '#1a1d21',
                        color: '#fff'
                    });
                }
            } catch(e) {
                console.error(e);
                Swal.fire({
                    title: 'NETWORK ERROR',
                    text: 'Failed to connect to server: ' + e.message,
                    icon: 'error',
                    background: '#1a1d21',
                    color: '#fff'
                });
            }
        }

        async function loadConsolidatedAnalysis() {
            const val = document.getElementById('avg_class_sel').value;
            if(!val) { alert('Please choose a class to analyze.'); return; }
            const [cid, sid] = val.split('|');
            const month = document.getElementById('avg_month').value;
            const year = new Date().getFullYear();

            loadAdminReport(cid, sid, month, year);
        }

        async function loadAdminReport(cid, sid, m, y) {
            // Reset search input on new load
            const sInput = document.getElementById('ledgerSearchInput');
            if(sInput) sInput.value = '';

            const container = document.getElementById('report-result');
            container.innerHTML = '<div style="text-align:center; padding:30px; opacity:0.4; font-weight:700;"><i class="fas fa-sync fa-spin"></i> GENERATING MONTHLY ANALYTICS...</div>';
            
            try {
                const res = await fetch(`attendance_api.php?action=get_summary&class_id=${cid}&section_id=${sid}&month=${m}&year=${y}`);
                const data = await res.json();

                if (!data.success || !data.students) {
                    let msg = data.error || "Execution failed or unexpected format";
                    container.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171; font-weight:800;">${msg}</div>`;
                    return;
                }

                let html = '<div class="smart-ledger-container"><table class="ledger-table">';
                html += `<thead><tr>
                        <th>C.No</th>
                        <th style="text-align:left;">NAME</th>
                        <th>ATT. BROUGHT</th>
                        <th>THIS MONTH</th>
                        <th>TOTAL ATT.</th>
                        <th>ABSENTEES</th>
                        <th>FINE</th>
                    </tr></thead><tbody>`;
                
                data.students.forEach(s => {
                    html += `<tr>
                        <td data-label="C.No">${s.roll}</td>
                        <td data-label="NAME">
                            <div style="text-align:left; font-weight:900;">${s.name}</div>
                        </td>
                        <td data-label="ATT. BROUGHT"><span class="ledger-pill pill-blue">${s.brought}</span></td>
                        <td data-label="THIS MONTH"><span class="ledger-pill pill-purple">${s.this_month}</span></td>
                        <td data-label="TOTAL ATT."><span class="ledger-pill pill-green" style="font-size:14px;">${s.total}</span></td>
                        <td data-label="ABSENTEES"><span class="ledger-pill pill-red">${s.absentees}</span></td>
                        <td data-label="FINE"><span class="ledger-pill pill-orange">RS ${Math.round(s.total_fine)}</span></td>
                    </tr>`;
                });
                html += '</tbody></table></div>';

                // --- PERFORMANCE SUMMARY GRID ---
                const st = data.stats;
                html += `
                <div style="margin-top:40px; border-top: 2px dashed rgba(255,255,255,0.1); padding-top:30px; font-weight:900;">
                    <div style="display:flex; justify-content:center; align-items:center; margin-bottom:30px;">
                        <h4 style="margin:0; font-size:16px; font-weight: 900; letter-spacing: 1.5px; text-transform:uppercase;">INSTITUTIONAL PERFORMANCE SUMMARY</h4>
                    </div>
                    
                    <div class="summary-grid" style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; width: 100%;">
                        <!-- Opening Balance -->
                        <div style="background:rgba(255,255,255,0.03); padding:15px; border-radius:12px; border:1px solid rgba(255,255,255,0.05); display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-size:10px; opacity:0.6; text-transform:uppercase;">Opening Balance</div>
                                <div style="font-size:11px; font-weight:700;">Count at month start</div>
                            </div>
                            <div style="font-size:22px; font-weight:900;">${st.start_count}</div>
                        </div>

                        <!-- New Admissions -->
                        <div style="background:rgba(46, 204, 113, 0.05); padding:15px; border-radius:12px; border:1px solid rgba(46, 204, 113, 0.1); display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-size:10px; opacity:0.6; text-transform:uppercase;">New Admissions</div>
                                <div style="font-size:11px; font-weight:700;">Admitted during period</div>
                            </div>
                            <div style="font-size:22px; font-weight:900; color:#2ecc71 !important;">+${st.new_admitted}</div>
                        </div>

                        <!-- Struck Off -->
                        <div style="background:rgba(231, 76, 60, 0.05); padding:15px; border-radius:12px; border:1px solid rgba(231, 76, 60, 0.1); display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-size:10px; opacity:0.6; text-transform:uppercase;">Students Struck Off</div>
                                <div style="font-size:11px; font-weight:700;">Attrition / Withdrawn</div>
                            </div>
                            <div style="font-size:22px; font-weight:900; color:#e74c3c !important;">-${st.struck_off}</div>
                        </div>

                        <!-- Closing Balance -->
                        <div style="background:rgba(255,255,255,0.07); padding:15px; border-radius:12px; border:2px solid var(--primary); display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-size:10px; opacity:0.7; text-transform:uppercase;">Closing Balance</div>
                                <div style="font-size:11px; font-weight:800;">Total active now</div>
                            </div>
                            <div style="font-size:24px; font-weight:900;">${st.end_count}</div>
                        </div>

                        <div style="background:rgba(52, 152, 219, 0.05); padding:15px; border-radius:12px; border:1px solid rgba(52, 152, 219, 0.1); display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-size:10px; opacity:0.6; text-transform:uppercase;">Target Capacity</div>
                                <div style="font-size:11px; font-weight:700;">Credits based on days</div>
                            </div>
                            <div style="font-size:22px; font-weight:900; color:#3498db;">${st.total_possible_days}</div>
                        </div>

                        <!-- Overall Efficiency -->
                        <div style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(16, 185, 129, 0.05) 100%); padding:20px; border-radius:15px; border:2px solid #10b981; display:flex; justify-content:space-between; align-items:center; margin-top:5px;">
                            <div>
                                <div style="font-size:12px; font-weight:900; text-transform:uppercase; color:#10b981; letter-spacing:1px;">OVERALL EFFICIENCY</div>
                                <div style="font-size:11px; font-weight:700;">Combined Credits / Potential Ratio</div>
                            </div>
                            <div style="font-size:32px; font-weight:900; color:#10b981; text-shadow: 0 0 10px rgba(16, 185, 129, 0.2);">${st.average_percentage}</div>
                        </div>
                    </div>
                </div>`;

                container.innerHTML = html;
            } catch (e) {
                console.error(e);
                container.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171; font-weight:800;">Execution Error: ${e.message}</div>`;
            }
        }

        function printReport(containerId, title) {
            const content = document.getElementById(containerId).innerHTML;
            const printWindow = window.open('', '_blank');
            const instName = "<?php echo addslashes($inst['name']); ?>";
            const instAddr = "<?php echo addslashes($inst['address'] ?? 'Secure Institutional Manifest'); ?>";
            const instLogo = "../../<?php echo $inst['logo_path']; ?>";
            const today = "<?php echo date('F d, Y'); ?>";

            let absoluteLogo = instLogo;
            try {
                const link = document.createElement('a');
                link.href = instLogo;
                absoluteLogo = link.href;
            } catch(e) {}

            // Read selected class name and date for the print header
            const classSel = document.getElementById('daily_class_sel');
            const className = classSel ? classSel.options[classSel.selectedIndex]?.text || '' : '';
            const dateEl = document.getElementById('daily_date');
            const attDate = dateEl ? dateEl.value : '';
            // Format date nicely e.g. "March 07, 2026"
            let attDateFormatted = attDate;
            try { if(attDate) attDateFormatted = new Date(attDate).toLocaleDateString('en-US', {year:'numeric', month:'long', day:'2-digit'}); } catch(e){}

            printWindow.document.write(`
                <html>
                <head>
                    <title>${title}</title>
                    <link rel="stylesheet" href="../../assets/vendor/fontawesome/all.min.css">
                    <style>
                        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700;900&family=Outfit:wght@300;500;700;900&display=swap');
                        :root {
                            --royal-blue: #2c52a0;
                            --cream-soft: #fbf9f1;
                            --text-main: #1e293b;
                        }
                        body { 
                            font-family: 'Inter', sans-serif; 
                            background-color: var(--cream-soft); 
                            margin: 0; 
                            padding: 0;
                            color: var(--text-main);
                        }
                        .page-container {
                            width: 29.7cm; /* A4 Landscape width */
                            min-height: 21cm;
                            margin: 0 auto;
                            background-color: var(--cream-soft);
                            position: relative;
                            display: flex;
                            flex-direction: column;
                            padding-bottom: 40px;
                        }
                        .header-modern {
                            height: 110px; 
                            display: flex;
                            position: relative;
                            z-index: 10;
                            overflow: hidden;
                            border-bottom: 5px solid var(--royal-blue);
                            background: white;
                        }
                        .header-blue-zone {
                            flex: 6;
                            background: var(--royal-blue);
                            color: white;
                            padding: 20px 40px;
                            position: relative;
                            clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                        }
                        .header-blue-zone h1 { font-size: 24px; margin: 0; font-weight: 900; text-transform: uppercase; }
                        .header-blue-zone p { font-size: 10px; margin: 5px 0 0; opacity: 0.8; letter-spacing: 2px; }
                        
                        .header-white-zone {
                            flex: 4;
                            padding: 10px 30px;
                            text-align: right;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                            align-items: flex-end;
                            position: relative;
                        }
                        .school-logo-img { width: 70px; height: 70px; object-fit: contain; position: absolute; top: 15px; right: 30px; z-index: 20; }
                        .header-info-content { z-index: 10; margin-right: 90px; text-align: right; }
                        .header-info-content strong { font-size: 18px; color: var(--royal-blue); text-transform: uppercase; display: block; font-weight: 900; }
                        .header-info-content p { font-size: 10px; margin: 2px 0 0; color: #64748b; font-weight: 600; }
                        
                        .header-blue-tip {
                            position: absolute; bottom: 0; right: 0; width: 100px; height: 40px;
                            background: var(--royal-blue); clip-path: polygon(100% 0, 100% 100%, 0 100%); z-index: 1;
                        }

                        .ledger-table { width: 100%; border-collapse: collapse; background: white; margin-top: 20px; font-size: 9px; table-layout: fixed; }
                        .ledger-table td { border: 1px solid #cbd5e1; padding: 4px 1px; text-align: center; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
                        .ledger-table th { background: var(--royal-blue); color: white !important; text-transform: uppercase; border: 1px solid #cbd5e1; padding: 6px 1px; text-align: center; font-weight: 900; font-size: 8px; }
                        
                        /* Optimized for 31 days + Summary Columns */
                        .ledger-table th:nth-child(2), .ledger-table td:nth-child(2) { text-align: left; padding-left: 5px; width: 120px !important; overflow: hidden; }
                        .ledger-table th:nth-child(1), .ledger-table td:nth-child(1) { width: 30px !important; }
                        
                        /* Fix for Summary Columns missing on the right */
                        .ledger-table th:nth-last-child(1), .ledger-table td:nth-last-child(1) { width: 35px !important; } /* % */
                        .ledger-table th:nth-last-child(2), .ledger-table td:nth-last-child(2),
                        .ledger-table th:nth-last-child(3), .ledger-table td:nth-last-child(3),
                        .ledger-table th:nth-last-child(4), .ledger-table td:nth-last-child(4),
                        .ledger-table th:nth-last-child(5), .ledger-table td:nth-last-child(5) { width: 25px !important; } /* P, A, L, H */

                        .ledger-pill { border: 1px solid #000; padding: 2px 8px; border-radius: 4px; display: inline-block; }
                        
                        .page-container::before {
                            content: ""; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
                            width: 400px; height: 400px; background-image: url('${instLogo}');
                            background-repeat: no-repeat; background-position: center; background-size: contain;
                            opacity: 0.04; z-index: 0; pointer-events: none;
                        }

                        .footer-combined {
                            padding: 20px 40px; display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 40px;
                            margin-top: auto; border-top: 1px solid #f1f5f9; position: relative; z-index: 10;
                        }
                        .sig-box { border-top: 1.5px dashed #64748b; padding-top: 8px; text-align: center; font-size: 10px; font-weight: 800; color: #1e293b; text-transform: uppercase; }

                        .no-print, .hardware-tube { display: none !important; }
                        
                        .summary-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-top: 30px; }
                        .summary-grid > div { border: 1px solid #000; padding: 15px; background: rgba(0,0,0,0.02); border-radius: 8px; }

                        /* Custom styles for Detailed Monthly Sheet */
                        .smart-ledger-container { overflow: visible !important; width: 100% !important; }
                        .smart-ledger-container table { width: 100% !important; border: 1px solid #cbd5e1 !important; table-layout: fixed !important; }
                        .smart-ledger-container th { 
                            background: #f1f5f9 !important; 
                            color: #1e293b !important; 
                            border: 1px solid #cbd5e1 !important; 
                            font-size: 7.5px !important; 
                            padding: 4px 0 !important;
                            height: 35px !important;
                            vertical-align: middle !important;
                        }
                        .smart-ledger-container th div { opacity: 1 !important; line-height: 1.1 !important; }
                        .smart-ledger-container td { border: 1px solid #cbd5e1 !important; font-size: 8px !important; padding: 3px 0 !important; }

                        .smart-ledger-container th.ledger-day { background: rgba(255,255,255,0.07); color: #fff; }
                        .smart-ledger-container th.ledger-sun { background: rgba(239, 68, 68, 0.2); color: #ef4444; }
                        .smart-ledger-container th.ledger-fri { background: #2c52a0 !important; color: #ffffff !important; }
                        
                        /* Print Specific Headings Override */
                        .smart-ledger-container th.ledger-day div.day-name { color: rgba(255,255,255,0.8); }
                        .smart-ledger-container th.ledger-day div.date-num { color: #fff; }

                        @media print {
                            .smart-ledger-container th.ledger-day { background: #f1f5f9 !important; color: #1e293b !important; }
                            .smart-ledger-container th.ledger-day div.day-name { color: #64748b !important; }
                            .smart-ledger-container th.ledger-day div.date-num { color: #1e293b !important; }
                            .smart-ledger-container th.ledger-sun { background: #fee2e2 !important; color: #ef4444 !important; }
                            .smart-ledger-container th.ledger-fri { background: #2c52a0 !important; color: #ffffff !important; }

                            /* Force Friday column background in print */
                            @media print {
                                .smart-ledger-container td.fri-cell { background: #2c52a0 !important; color: #ffffff !important; }
                            }

                            /* Summary Cells Print Optimization */
                            .summary-cell { border-color: #cbd5e1 !important; }
                            .p-cell { background: #dcfce7 !important; color: #166534 !important; }
                            .a-cell { background: #fee2e2 !important; color: #991b1b !important; }
                            .l-cell { background: #e0f2fe !important; color: #0369a1 !important; }
                            .h-cell { background: #fee2e2 !important; color: #991b1b !important; }
                            .pct-cell { background: #f8fafc !important; color: #1e293b !important; }

                            /* Repeat adjustments */
                            thead { display: table-header-group; }
                            tfoot { display: table-footer-group; }
                        }

                        /* ── Attendance Circle Buttons ── */
                        .att-opt {
                            width: 28px !important;
                            height: 28px !important;
                            font-weight: 900;
                            border-radius: 50% !important;
                            display: inline-flex !important;
                            align-items: center !important;
                            justify-content: center !important;
                            font-size: 10px !important;
                            border: 1.5px solid #cbd5e1 !important;
                            color: #94a3b8;
                            background: #f8fafc;
                        }
                        /* Active states matching on-screen colors */
                        .opt-P.active { background: #4ade80 !important; color: #064e3b !important; border-color: #16a34a !important; }
                        .opt-A.active { background: #991b1b !important; color: #ffffff !important; border-color: #7f1d1d !important; }
                        .opt-L.active { background: #fbbf24 !important; color: #78350f !important; border-color: #d97706 !important; }
                        .opt-S.active { background: #e11d48 !important; color: #ffffff !important; border-color: #9f1239 !important; }
                        .opt-H.active { background: #9b59b6 !important; color: #ffffff !important; border-color: #8e44ad !important; }

                        /* Status Pills */
                        .status-pill {
                            display: inline-flex;
                            align-items: center;
                            padding: 2px 8px;
                            border-radius: 50px;
                            font-size: 9px;
                            font-weight: 900;
                            text-transform: uppercase;
                            letter-spacing: 0.5px;
                            border: 1px solid #cbd5e1;
                        }
                        .bg-present   { background: #dcfce7; color: #166534; border-color: #86efac; }
                        .bg-absent    { background: #fee2e2; color: #991b1b; border-color: #fca5a5; }
                        .bg-leave     { background: #fef9c3; color: #854d0e; border-color: #fde047; }
                        .bg-struckoff { background: #ffe4e6; color: #9f1239; border-color: #fda4af; }
                        .bg-unknown   { background: #f1f5f9; color: #475569; border-color: #cbd5e1; }

                        /* Roll Number Badge */
                        .roll-badge {
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            width: 22px; height: 22px;
                            border-radius: 50%;
                            background: #2c52a0;
                            color: white;
                            font-size: 9px;
                            font-weight: 900;
                        }

                        /* Student & Staff Row Layout */
                        .students-grid {
                            display: grid;
                            grid-template-columns: repeat(2, 1fr);
                            gap: 8px;
                            margin-top: 15px;
                        }
                        .student-row {
                            border: 1px solid #e2e8f0;
                            border-radius: 8px;
                            padding: 8px 12px;
                            display: flex;
                            flex-direction: column;
                            gap: 5px;
                            background: white;
                        }
                        .att-options { display: flex; gap: 4px; align-items: center; }
                        .student-name { font-weight: 800; font-size: 12px; color: #1e293b; }
                        /* Hide hidden inputs in print */
                        .time-in-input, .time-out-input,
                        .student-status-input, .staff-status-input { display: none !important; }

                        @media print {
                            body { background: white !important; }
                            .page-container { width: 100% !important; margin: 0 !important; box-shadow: none !important; }
                            @page { size: landscape; margin: 0; }
                            /* Force browsers to print background colors (critical for attendance circles) */
                            * {
                                -webkit-print-color-adjust: exact !important;
                                print-color-adjust: exact !important;
                                color-adjust: exact !important;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="page-container">
                        <table style="width: 100%; border-collapse: collapse; position: relative; z-index: 10;">
                            <thead>
                                <tr>
                                    <td>
                                        <div class="header-modern">
                                            <div class="header-blue-zone">
                                                <h1>${instName}</h1>
                                                <p>OFFICIAL ATTENDANCE ARCHIVE</p>
                                            </div>
                                            <div class="header-white-zone">
                                                <img src="${absoluteLogo}" class="school-logo-img" onerror="this.onerror=null; this.style.display='none';">
                                                <div class="header-info-content">
                                                    <strong>${title.toUpperCase()}</strong>
                                                    ${className ? `<p style="font-size:13px; font-weight:900; color:#2c52a0; margin-top:4px;"><i style="margin-right:4px;">&#x1F4DA;</i> ${className}</p>` : ''}
                                                    <p>DATE: ${attDateFormatted || today}</p>
                                                </div>
                                                <div class="header-blue-tip"></div>
                                            </div>
                                        </div>
                                        <div style="height: 20px;"></div>
                                    </td>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td style="padding: 0 10px;">
                                        <div style="position: relative; z-index: 5;">
                                            ${content}
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                            <tfoot>
                                <tr>
                                    <td>
                                        <div class="footer-combined">
                                            <div class="sig-box">Class Incharge / Supervisor</div>
                                            <div class="sig-box">Audit & Examination Dept.</div>
                                            <div class="sig-box">Principal / Headmaster Signature</div>
                                        </div>
                                    </td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                    <script>setTimeout(() => { window.print(); window.close(); }, 500);<` + `/script>
                </body>
                </html>
            `);
            printWindow.document.close();
        }

        async function loadPolicyData() {
            console.log("Policy Intel Requested - 2026-01-26 Log");
            if(!document.getElementById('p_absent_limit')) return;
            try {
                const res = await fetch('attendance_api.php?action=get_policy_center');
                if (!res.ok) throw new Error("Network response was not ok: " + res.status);
                
                const d = await res.json();
                if (!d) throw new Error("Received empty response from Policy API");
                
                if (d.error) {
                    console.error("Policy API Internal Error (2026-01-26):", d.error);
                    let msg = d.error;
                    if (d.details) msg += "\n\nDetails: " + d.details;
                    
                    if (msg.includes("columns missing")) {
                        alert("Critical: Database columns are missing.\n\nPlease visit this URL to fix:\nhttp://192.168.52.196/WANTUCH/modules/education/migrate_attendance_v2.php");
                    } else {
                        alert("Policy Sync Exception: " + msg);
                    }
                    return;
                }
                
                // Safe property access with optional chaining if supported, otherwise standard checks
                if(d.financial) {
                    document.getElementById('p_absent_limit').value = d.financial.absence_limit || 3;
                    document.getElementById('p_deduction_rate').value = d.financial.deduction_rate || 0;
                    document.getElementById('p_bonus_primary').value = d.financial.bonus_primary || 0;
                    document.getElementById('p_bonus_middle').value = d.financial.bonus_middle || 0;
                    document.getElementById('p_bonus_high').value = d.financial.bonus_high || 0;
                    document.getElementById('p_bonus_secondary').value = d.financial.bonus_secondary || 0;
                    
                    // Behavior Fines
                    if(d.fines) {
                        document.getElementById('p_fine_hair').value = d.fines.hair_cut || 0;
                        document.getElementById('p_fine_reg').value = d.fines.register || 0;
                        document.getElementById('p_fine_rules').value = d.fines.rules_break || 0;
                        document.getElementById('p_fine_att').value = d.fines.attendance || 0;
                    }
                    console.log("School Rules Synchronized Successfully - 2026-01-26");
                } else {
                    throw new Error("Invalid policy data structure received");
                }
            } catch (e) {
                console.error("Strict Policy Fetch Exception (2026-01-26):", e.message);
                // Fail gracefully without crashing the whole script
            }
        }

        async function saveSchoolPolicy() {
            const data = {
                absence_limit: document.getElementById('p_absent_limit').value,
                deduction_rate: document.getElementById('p_deduction_rate').value,
                bonus_primary: document.getElementById('p_bonus_primary').value,
                bonus_middle: document.getElementById('p_bonus_middle').value,
                bonus_high: document.getElementById('p_bonus_high').value,
                bonus_secondary: document.getElementById('p_bonus_secondary').value,
                fine_hair_cut: document.getElementById('p_fine_hair').value,
                fine_register: document.getElementById('p_fine_reg').value,
                fine_rules_break: document.getElementById('p_fine_rules').value,
                fine_attendance: document.getElementById('p_fine_att').value
            };

            try {
                const res = await fetch('attendance_api.php?action=save_school_policy', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                const r = await res.json();
                if(r.success) {
                    alert('Institutional Policy Synchronized!');
                } else {
                    let msg = r.error || 'Unknown';
                    if(r.details) msg += '\n\nDetails: ' + r.details;
                    
                    if(r.migration_url) {
                        if(confirm(msg + '\n\nWould you like to visit the migration page now to fix this?')) {
                            window.location.href = r.migration_url;
                        }
                    } else {
                        alert('Error: ' + msg);
                    }
                }
            } catch(e) {
                console.error("Policy Save Error", e);
                alert("Failed to save policy. Check console.");
            }
        }


        async function syncRollNumbers(cid, sid) {
            if(!confirm("Are you sure you want to re-order and shift roll numbers for all active students in this class? This will fill any gaps from struck-off students.")) return;
            const res = await fetch(`attendance_api.php?action=sync_roll_numbers&class_id=${cid}&section_id=${sid}`);
            const r = await res.json();
            if(r.success) {
                alert(`Successfully synchronized ${r.processed} roll numbers!`);
                loadAdminDailyStudents();
            } else {
                alert('Sync Error: ' + r.error);
            }
        }

        function filterStudents() {
            const term = document.getElementById('student_search').value.toLowerCase();
            const rows = document.querySelectorAll('#student-marking-container .student-row');
            let foundCount = 0;
            rows.forEach(row => {
                const name = row.querySelector('.student-name').innerText.toLowerCase();
                const roll = row.querySelector('.roll-badge').innerText.toLowerCase();
                if (name.includes(term) || roll.includes(term)) {
                    // Check if it's mobile view to apply correct display
                    if (document.body.classList.contains('force-mobile')) {
                        row.style.setProperty('display', 'flex', 'important');
                    } else {
                        row.style.display = 'flex';
                    }
                    foundCount++;
                } else {
                    row.style.setProperty('display', 'none', 'important');
                }
            });
            console.log(`Filtered students: ${foundCount} matches for "${term}"`);
        }


    </script>

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
        async function syncDatabaseToOnline() {
            if (!confirm('Are you sure you want to SYNC all local school data to the ONLINE server? Existing rows will be updated and new records will be added.')) return;
            
            // Show Loading State
            const btn = document.getElementById('sync-db-btn');
            const originalHTML = btn.innerHTML;
            btn.style.pointerEvents = 'none';
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> SYNCING...';
            
            // Create Overlay
            const overlay = document.createElement('div');
            overlay.style.cssText = 'position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(2,6,23,0.9); z-index:9999; display:flex; flex-direction:column; align-items:center; justify-content:center; backdrop-filter:blur(10px); color:white; font-family:Outfit, sans-serif;';
            overlay.innerHTML = `
                <div style="background:rgba(255,255,255,0.05); padding:40px; border-radius:30px; border:1px solid rgba(255,255,255,0.1); text-align:center; max-width:500px; width:90%;">
                    <i class="fas fa-cloud-upload-alt fa-3x" style="color:#10b981; margin-bottom:20px;"></i>
                    <h2 style="font-size:24px; font-weight:900; margin-bottom:10px;">Establishing Neural Link...</h2>
                    <p style="font-size:13px; opacity:0.7; margin-bottom:20px;">Synchronizing local school modules with live cPanel server...</p>
                    <div id="sync-progress" style="text-align:left; font-size:10px; max-height:200px; overflow-y:auto; font-family:monospace; background:black; padding:15px; border-radius:10px; opacity:0.8;">
                        Connecting to remote database...
                    </div>
                </div>
            `;
            document.body.appendChild(overlay);

            try {
                const res = await fetch('../system/sync_api.php');
                const data = await res.json();
                
                const progressBox = document.getElementById('sync-progress');
                
                if (data.success) {
                    progressBox.innerHTML += '<div style="color:#10b981; margin-top:10px;">CONNECTED! Starting data transfer...</div>';
                    if (data.details) {
                        data.details.forEach(detail => {
                            progressBox.innerHTML += `<div style="margin-top:2px;">✔ ${detail}</div>`;
                            progressBox.scrollTop = progressBox.scrollHeight;
                        });
                    }
                    
                    overlay.querySelector('h2').innerText = "Synchronization Complete!";
                    overlay.querySelector('i').className = "fas fa-check-circle fa-3x";
                    overlay.querySelector('i').style.color = "#10b981";
                    
                    setTimeout(() => {
                        overlay.style.opacity = '0';
                        setTimeout(() => overlay.remove(), 500);
                    }, 3000);
                } else {
                    overlay.querySelector('h2').innerText = "Sync Failed";
                    overlay.querySelector('h2').style.color = "#ef4444";
                    overlay.querySelector('i').className = "fas fa-exclamation-triangle fa-3x";
                    overlay.querySelector('i').style.color = "#ef4444";
                    progressBox.innerHTML += `<div style="color:#ef4444; margin-top:10px;">ERROR: ${data.error}</div>`;
                    if(data.advice) progressBox.innerHTML += `<div style="color:#fbbf24; margin-top:5px;">ADVICE: ${data.advice}</div>`;
                    
                    const closeBtn = document.createElement('button');
                    closeBtn.innerText = "Close";
                    closeBtn.className = "btn";
                    closeBtn.style.marginTop = "20px";
                    closeBtn.onclick = () => overlay.remove();
                    overlay.querySelector('div').appendChild(closeBtn);
                }
            } catch (error) {
                console.error('Sync error:', error);
                alert('Connection to local sync API failed. Check console for details.');
                overlay.remove();
            } finally {
                btn.innerHTML = originalHTML;
                btn.style.pointerEvents = 'auto';
            }
        }
    </script>

    <!-- Manual Attendance Modal (Moved Outside) -->
    <div id="manual-att-modal" class="modal-overlay" style="z-index: 99999;">
        <div class="modal-content" style="width: 95%; max-width: 800px; background: var(--modal-bg, #fff); color: var(--modal-text, #000); max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; padding: 0; position: relative; z-index: 100000;">
            
            <!-- Header -->
            <div style="padding: 15px; border-bottom: 1px solid var(--modal-border, rgba(0,0,0,0.1)); display: flex; justify-content: space-between; align-items: center; background: var(--modal-sub-bg, rgba(0,0,0,0.02));">
                <div style="display:flex; align-items:center; gap:12px;">
                    <input type="date" id="manual-modal-date" onchange="refreshManualModalData()" style="background:rgba(0,0,0,0.05); border:1px solid rgba(0,0,0,0.1); border-radius:30px; padding:4px 10px; font-size:11px; font-weight:800; color:inherit; outline:none;">
                    <label style="display:flex; align-items:center; gap:6px; font-size:11px; font-weight:800; background:rgba(0,0,0,0.05); padding:4px 10px; border-radius:30px; cursor:pointer; color:inherit;">
                        <input type="checkbox" id="manual-select-all" onclick="toggleSelectAllManual(this.checked)" style="width:14px; height:14px;"> ALL
                    </label>
                </div>
                <button onclick="document.getElementById('manual-att-modal').style.display='none'" style="background:none; border:none; font-size:24px; color:var(--text-dim, #666); cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; transition:all 0.2s; opacity:0.7;" onmouseover="this.style.opacity='1'" onmouseout="this.style.opacity='0.7'">
                    ×
                </button>
            </div>

            <!-- Content Area (Scrollable) -->
            <div id="manual-modal-body" style="flex: 1; overflow-y: auto; padding: 20px;">
                <div id="manual-loading" style="text-align:center; padding:40px; display:none;">
                    <i class="fas fa-sync fa-spin" style="font-size:24px; opacity:0.5;"></i>
                    <div style="margin-top:10px; font-weight:700; opacity:0.7;">Fetching Records...</div>
                </div>
                
                <div id="manual-list-container"></div>
            </div>

            <!-- Footer Actions -->
            <div style="padding: 15px; border-top: 1px solid var(--modal-border, rgba(0,0,0,0.1)); background: var(--modal-sub-bg, rgba(0,0,0,0.02));">
                <div style="font-size:11px; font-weight:700; margin-bottom:10px; opacity:0.6; text-align:center; text-transform:uppercase;">Select status to apply to checked items</div>
                
                <div style="display:grid; grid-template-columns: repeat(5, 1fr); gap:8px;" id="manual-status-btns">
                    <button onclick="applyManualStatus('Present')" class="btn" style="background:#dcfce7; color:#166534 !important; border:none; font-weight:900; padding:10px 5px; border-radius:12px; font-size:14px; cursor:pointer;">
                        P
                    </button>
                    <button onclick="applyManualStatus('Absent')" class="btn" style="background:#fee2e2; color:#991b1b !important; border:none; font-weight:900; padding:10px 5px; border-radius:12px; font-size:14px; cursor:pointer;">
                        A
                    </button>
                    <button onclick="applyManualStatus('Leave')" class="btn" style="background:#dbeafe; color:#1e40af !important; border:none; font-weight:900; padding:10px 5px; border-radius:12px; font-size:14px; cursor:pointer;">
                        L
                    </button>
                    <button id="btn-status-so" onclick="applyManualStatus('Struck Off')" class="btn" style="background:#f3f4f6; color:#374151 !important; border:none; font-weight:900; padding:10px 5px; border-radius:12px; font-size:14px; cursor:pointer;">
                        SO
                    </button>
                    <button onclick="applyManualStatus('')" class="btn" style="background:#ffffff; color:#000000 !important; border:none; font-weight:900; padding:10px 5px; border-radius:12px; font-size:14px; cursor:pointer; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        UM
                    </button>
                </div>
            </div>

        </div>
    </div>

    <!-- Attendance Bulk Excel Modal -->
    <div id="attendance-bulk-modal" class="modal-overlay" style="z-index: 99999;">
        <div class="modal-content" style="width: 95%; max-width: 1000px; background: var(--modal-bg, #fff); color: var(--modal-text, #000); max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; padding: 0;">
            <div style="padding: 15px 25px; border-bottom: 1px solid var(--modal-border, rgba(0,243,255,0.2)); display: flex; justify-content: space-between; align-items: center; background: var(--modal-sub-bg, rgba(0, 243, 255, 0.05));">
                <h3 style="margin:0; font-size: 18px; font-weight: 800; color: inherit;"><i class="fas fa-file-excel" style="margin-right:10px; color:inherit;"></i> EXCEL ATTENDANCE WIZARD</h3>
                <button onclick="document.getElementById('attendance-bulk-modal').style.display='none'" style="background:none !important; border:none !important; font-size:28px !important; cursor:pointer !important; opacity:0.8 !important; color:inherit !important; width:auto !important; padding:0 !important; box-shadow:none !important;">×</button>
            </div>
            
            <div style="flex: 1; overflow-y: auto; padding: 25px;">
                <!-- Step 1: Download -->
                <div id="bulk-step-1-container" style="background: var(--modal-sub-bg, #f1f5f9); padding: 20px; border-radius: 16px; margin-bottom: 25px; border: 1px dashed var(--modal-border, #cbd5e1);">
                    <div style="display:flex; justify-content: space-between; align-items: center;">
                        <div>
                            <h4 style="margin:0 0 5px 0; font-size: 14px; font-weight: 800; color: inherit !important;">1. Download Template</h4>
                            <p style="margin:0; font-size: 12px; opacity: 0.8; color: inherit !important;">Get the pre-filled horizontal template for your selection.</p>
                        </div>
                        <button class="btn" onclick="downloadAttendanceTemplate()" style="background:#00f3ff !important; color:#000 !important; font-weight:900 !important; padding: 10px 20px !important; border-radius: 10px !important; width:auto !important; border:none !important; font-size: 12px !important; box-shadow: 0 0 15px rgba(0,243,255,0.4) !important;">DOWNLOAD CSV</button>
                    </div>
                </div>

                <!-- Step 2: Upload -->
                <div id="bulk-step-2-container" style="margin-bottom: 25px;">
                    <h4 style="margin:0 0 15px 0; font-size: 14px; font-weight: 800; color: #000 !important;">2. Import & Preview</h4>
                    <div style="border: 2px dashed #e2e8f0; border-radius: 16px; padding: 40px; text-align: center; cursor: pointer; transition: all 0.3s;" onclick="document.getElementById('bulk-excel-input').click()" onmouseover="this.style.borderColor='var(--primary)'" onmouseout="this.style.borderColor='#e2e8f0'">
                        <i class="fas fa-cloud-upload-alt" style="font-size: 40px; color: #64748b; margin-bottom: 15px;"></i>
                        <div style="font-weight: 700; color: #475569;">Click to upload your modified CSV file</div>
                        <div style="font-size: 11px; color: #94a3b8; margin-top: 5px;">Supports .csv files exported from Step 1</div>
                        <input type="file" id="bulk-excel-input" style="display:none;" accept=".csv" onchange="handleBulkCSVUpload(this)">
                    </div>
                </div>

                <!-- Preview Table -->
                <div id="bulk-preview-container" style="display:none;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:15px; border-top: 1px solid var(--modal-border, #eee); padding-top: 25px;">
                        <h4 style="margin:0; font-size: 14px; font-weight: 800; color: inherit !important;">3. Review Changes</h4>
                        <button onclick="openAttendanceBulkModal()" style="background:rgba(0,0,0,0.05) !important; border:1px solid rgba(0,0,0,0.1) !important; color:var(--primary) !important; font-size:9px !important; font-weight:900 !important; cursor:pointer !important; padding: 4px 12px !important; border-radius: 20px !important; letter-spacing: 0.5px !important; width: fit-content !important; display: inline-flex !important; align-items: center !important;">CHANGE FILE</button>
                    </div>
                    <div style="overflow-x: auto; border: 1px solid var(--modal-border, #e2e8f0); border-radius: 12px;">
                        <table style="width:100%; border-collapse: collapse; font-size: 12px;" id="bulk-preview-table">
                            <thead style="background: transparent;">
                                <tr id="bulk-preview-header" style="border-bottom: 2px solid var(--modal-border, rgba(0,0,0,0.1));"></tr>
                            </thead>
                            <tbody id="bulk-preview-body"></tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div style="padding: 20px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; justify-content: flex-end; gap: 12px; background: rgba(0,0,0,0.2);">
                <button class="btn" onclick="document.getElementById('attendance-bulk-modal').style.display='none'" style="background: white !important; border: 1px solid #cbd5e1 !important; color: #475569 !important; font-weight: 800 !important; width:auto !important;">CANCEL</button>
                <button id="save-bulk-btn" class="btn" onclick="saveBulkPreview()" style="background: #22c55e !important; color: #fff !important; font-weight: 900 !important; width:auto !important; display: none; border:none !important; box-shadow: 0 0 20px rgba(34,197,94,0.4) !important;">SAVE ATTENDANCE</button>
            </div>
        </div>
    </div>

    <!-- JAVASCRIPT LOGIC FOR MANUAL ATTENDANCE -->
    <script>
    // Switch Sub-tabs in Manual View
    function switchManualSubTab(tabId, btn, access_id = null) {
        if (typeof GranularAccessControl !== 'undefined' && access_id) {
            if (!GranularAccessControl.checkAccess(access_id)) return;
        }
        document.querySelectorAll('.manual-panel').forEach(p => p.style.display = 'none');
        document.querySelectorAll('#manual-att-panel .sub-tab-btn').forEach(b => b.classList.remove('active'));
        
        const target = document.getElementById(tabId);
        if(target) target.style.display = 'block';
        if(btn) btn.classList.add('active');
    }

    async function processManualBulkAttendance() {
        const dateFrom = document.getElementById('manual_bulk_from').value;
        const dateTo = document.getElementById('manual_bulk_to').value;
        const target = document.getElementById('manual_bulk_class').value;
        
        let targetLabel = target === 'all' ? 'ALL CLASSES' : 'this class';
        let dateLabel = dateFrom === dateTo ? `for ${dateFrom}` : `from ${dateFrom} to ${dateTo}`;
        
        if(!confirm(`Are you sure you want to mark all students in ${targetLabel} as PRESENT ${dateLabel}?`)) return;
        
        const btn = event.target;
        const originalHTML = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-sync fa-spin"></i> PROCESSING...';

        try {
            const res = await fetch(`attendance_api.php?action=bulk_mark_present&class_selection=${target}&date_from=${dateFrom}&date_to=${dateTo}`);
            const data = await res.json();
            if(data.success) {
                alert(`SUCCESS: Marked ${data.processed} students as Present across the selected range.`);
            } else {
                alert("Error: " + data.error);
            }
        } catch(e) {
            alert("Execution failed script-side.");
        } finally {
            btn.disabled = false;
            btn.innerHTML = originalHTML;
        }
    }

    function openAttendanceBulkModal() {
        document.getElementById('attendance-bulk-modal').style.display = 'flex';
        document.getElementById('bulk-step-1-container').style.display = 'block';
        document.getElementById('bulk-step-2-container').style.display = 'block';
        document.getElementById('bulk-preview-container').style.display = 'none';
        document.getElementById('save-bulk-btn').style.display = 'none';
        document.getElementById('bulk-excel-input').value = '';
    }

    function downloadAttendanceTemplate() {
        const from = document.getElementById('manual_bulk_from').value;
        const to = document.getElementById('manual_bulk_to').value;
        const target = document.getElementById('manual_bulk_class').value;
        window.location.href = `attendance_api.php?action=export_attendance_horizontal_csv&class_selection=${target}&date_from=${from}&date_to=${to}`;
    }

    let bulkImportData = null;
    function handleBulkCSVUpload(input) {
        if (!input.files || !input.files[0]) return;
        const file = input.files[0];
        const reader = new FileReader();

        reader.onload = function(e) {
            try {
                const text = e.target.result;
                const rawLines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l !== '');
                if (rawLines.length < 2) {
                    alert("The file seems empty or invalid.");
                    return;
                }

                // Helper to split CSV line correctly (handling quotes)
                const splitCSV = (line) => {
                    const result = [];
                    let cur = '';
                    let inQuotes = false;
                    for (let char of line) {
                        if (char === '"') inQuotes = !inQuotes;
                        else if (char === ',' && !inQuotes) {
                            result.push(cur.trim().replace(/^"|"$/g, ''));
                            cur = '';
                        } else if (char === ';' && !inQuotes && line.includes(';')) { // Semicolon support
                            result.push(cur.trim().replace(/^"|"$/g, ''));
                            cur = '';
                        }
                        else cur += char;
                    }
                    result.push(cur.trim().replace(/^"|"$/g, ''));
                    return result;
                };

                const headers = splitCSV(rawLines[0]);
                const dateColumns = [];
                
                for(let j=3; j < headers.length; j++) {
                    let h = headers[j];
                    if(h.toLowerCase().includes('(in)')) {
                        let date = h.replace(/\s*\(in\)/i, '').trim();
                        dateColumns.push({ date: date, in_idx: j, out_idx: j+1 });
                    }
                }

                if (dateColumns.length === 0) {
                    alert("Could not detect any attendance date columns in the header. Please use the template exported from Step 1.");
                    return;
                }

                const headerRow = document.getElementById('bulk-preview-header');
                let headHtml = `<th style="padding:10px; border:1px solid #eee; background:#f1f5f9;">ID</th>`;
                headHtml += `<th style="padding:10px; border:1px solid #eee; background:#f1f5f9;">Roll</th>`;
                headHtml += `<th style="padding:10px; border:1px solid #eee; background:#f1f5f9;">Name</th>`;
                dateColumns.forEach(dc => {
                    headHtml += `<th colspan="2" style="padding:10px; border:1px solid #eee; background:#f1f5f9; text-align:center;">${dc.date}</th>`;
                });
                headerRow.innerHTML = headHtml;

                const body = document.getElementById('bulk-preview-body');
                body.innerHTML = '';

                const records = [];
                for (let i = 1; i < rawLines.length; i++) {
                    const cols = splitCSV(rawLines[i]);
                    if (cols.length < headers.length) continue;

                    const studentId = cols[0];
                    const attendance = {}; 

                    let rowHtml = `<tr style="border-bottom: 1px solid #f1f5f9;">`;
                    rowHtml += `<td style="padding:8px; border:1px solid #eee;">${cols[0]}</td>`; 
                    rowHtml += `<td style="padding:8px; border:1px solid #eee;">${cols[1]}</td>`; 
                    rowHtml += `<td style="padding:8px; border:1px solid #eee; font-weight:700;">${cols[2]}</td>`; 

                    dateColumns.forEach(dc => {
                        const statusIn = (cols[dc.in_idx] || '').trim();
                        const statusOut = (cols[dc.out_idx] || '').trim();
                        attendance[dc.date] = [statusIn, statusOut];
                        
                        const getStyle = (s) => {
                            const up = s.toUpperCase();
                            if(s.includes(':') || up === 'P' || up.includes('AM') || up.includes('PM')) return { bg: '#dcfce7', color: '#166534' }; 
                            if(up === 'A') return { bg: '#fee2e2', color: '#991b1b' }; 
                            if(up === 'L') return { bg: '#dbeafe', color: '#1e40af' }; 
                            if(up === 'S') return { bg: '#f3f4f6', color: '#374151' }; 
                            return { bg: '#f8fafc', color: '#64748b' };
                        };

                        const styleIn = getStyle(statusIn);
                        const styleOut = getStyle(statusOut);

                        rowHtml += `<td style="padding:8px; border:1px solid #eee; text-align:center; background:${styleIn.bg}; color:${styleIn.color}; font-weight:900; white-space:nowrap; font-size:10px;">${statusIn}</td>`;
                        rowHtml += `<td style="padding:8px; border:1px solid #eee; text-align:center; background:${styleOut.bg}; color:${styleOut.color}; font-weight:900; white-space:nowrap; font-size:10px;">${statusOut}</td>`;
                    });
                    rowHtml += `</tr>`;
                    body.innerHTML += rowHtml;

                    records.push({ student_id: studentId, attendance: attendance });
                }

                bulkImportData = { records: records, dates: dateColumns.map(d => d.date) };
                
                // Hide Steps 1 & 2, show Preview
                document.getElementById('bulk-step-1-container').style.display = 'none';
                document.getElementById('bulk-step-2-container').style.display = 'none';
                document.getElementById('bulk-preview-container').style.display = 'block';
                document.getElementById('save-bulk-btn').style.display = 'block';
                
                console.log("Bulk Preview Rendered:", bulkImportData);
            } catch (err) {
                console.error("CSV Parsing Error:", err);
                alert("Failed to parse the CSV file. Please ensure it follows the correct format.");
            }
        };

        reader.readAsText(file);
    }

    async function saveBulkPreview() {
        if (!bulkImportData) return;
        
        if (!confirm(`Are you sure you want to save attendance for ${bulkImportData.records.length} students across ${bulkImportData.dates.length} days?`)) return;

        const btn = document.getElementById('save-bulk-btn');
        const originalHTML = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> SAVING...';

        try {
            const res = await fetch('attendance_api.php?action=save_bulk_import_json', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bulkImportData)
            });
            const data = await res.json();
            if (data.success) {
                alert(`SUCCESS: Processed ${data.processed} daily attendance entries.`);
                document.getElementById('attendance-bulk-modal').style.display = 'none';
                
                // Switch back to Daily Students tab to show results
                const studentTabBtn = document.querySelector('[onclick*="switchAttendanceSubTab(\'students-att-panel\'"]');
                if(studentTabBtn) {
                    switchAttendanceSubTab('students-att-panel', studentTabBtn);
                }
            } else {
                alert("Error: " + data.error);
            }
        } catch (e) {
            alert("Save failed script-side.");
        } finally {
            btn.disabled = false;
            btn.innerHTML = originalHTML;
        }
    }

    // Global State for Manual Selection
    let manualSelection = new Set();
    let currentManualContext = null; // 'student' or 'staff'
    let currentManualParams = {}; 

    // --- STUDENT HANDLING ---
    async function openManualStudentModal(dateOverride = null) {
        const date = dateOverride || document.getElementById('manual_std_date').value;
        const val = document.getElementById('manual_class_sel').value;
        
        if(!val) { alert("Please select a class first."); return; }
        const [cid, sid] = val.split('|');
        
        currentManualContext = 'student';
        currentManualParams = { cid, sid, date };
        manualSelection.clear();
        document.getElementById('manual-select-all').checked = false;
        document.getElementById('btn-status-so').style.display = 'block';
        document.getElementById('manual-status-btns').style.gridTemplateColumns = 'repeat(5, 1fr)';
        document.getElementById('manual-modal-date').value = date;

        document.getElementById('manual-att-modal').style.display = 'flex';
        document.getElementById('manual-list-container').innerHTML = '';
        document.getElementById('manual-loading').style.display = 'block';

        try {
            const res = await fetch(`attendance_api.php?action=get_students&class_id=${cid}&section_id=${sid}&date=${date}`);
            const data = await res.json();
            document.getElementById('manual-loading').style.display = 'none';
            
            if(Array.isArray(data)) {
                renderManualGrid(data, 'student');
            } else {
                document.getElementById('manual-list-container').innerHTML = `<div style="text-align:center;">Error loading data</div>`;
            }
        } catch(e) {
            console.error(e);
            document.getElementById('manual-loading').style.display = 'none';
            document.getElementById('manual-list-container').innerHTML = `<div style="text-align:center;">Connection Failed</div>`;
        }
    }

    // --- STAFF HANDLING ---
    async function openManualStaffModal(dateOverride = null) {
        const date = dateOverride || document.getElementById('manual_staff_date').value;
        const type = document.getElementById('manual_staff_type').value; 
        
        currentManualContext = 'staff';
        currentManualParams = { type, date };
        manualSelection.clear();
        document.getElementById('manual-select-all').checked = false;
        document.getElementById('btn-status-so').style.display = 'none';
        document.getElementById('manual-status-btns').style.gridTemplateColumns = 'repeat(4, 1fr)';
        document.getElementById('manual-modal-date').value = date;

        document.getElementById('manual-att-modal').style.display = 'flex';
        document.getElementById('manual-list-container').innerHTML = '';
        document.getElementById('manual-loading').style.display = 'block';

        try {
            const res = await fetch(`attendance_api.php?action=get_staff_attendance&date=${date}&type=${type}`); 
            const data = await res.json();
            document.getElementById('manual-loading').style.display = 'none';
            
            if(Array.isArray(data)) {
                // Client-side filter if API doesn't support 'type' param strictly on that endpoint yet
                let filtered = data;
                if(type === 'teaching') filtered = data.filter(s => s.role === 'teacher');
                if(type === 'non-teaching') filtered = data.filter(s => ['staff','admin','principal','head'].includes(s.role));
                
                renderManualGrid(filtered, 'staff');
            } else {
                document.getElementById('manual-list-container').innerHTML = `<div style="text-align:center;">Error loading data</div>`;
            }
        } catch(e) {
            console.error(e);
            document.getElementById('manual-loading').style.display = 'none';
            document.getElementById('manual-list-container').innerHTML = `<div style="text-align:center;">Connection Failed</div>`;
        }
    }

    function refreshManualModalData() {
        const newDate = document.getElementById('manual-modal-date').value;
        if (currentManualContext === 'student') openManualStudentModal(newDate);
        else openManualStaffModal(newDate);
    }

    function renderManualGrid(items, type) {
        const container = document.getElementById('manual-list-container');
        let html = '<div style="display:flex; flex-wrap:wrap; gap:10px; justify-content:center;">';
        
        items.forEach(item => {
            let labelLabel = type === 'student' ? `CN: ${item.class_no || '?'}` : (item.designation || item.role);
            
            let status = item.status || '';
            let cardStyle = 'background: rgba(0,0,0,0.02); border: 2px solid rgba(0,0,0,0.05); color: inherit;';
            let statusText = 'Pending';
            let statusDot = 'background: #94a3b8;';
            
            if(status === 'Present') { 
                cardStyle = 'background: rgba(22, 163, 74, 0.08); border: 2px solid rgba(22, 163, 74, 0.2); color: #166534;'; 
                statusText = 'PRESENT';
                statusDot = 'background: #22c55e; box-shadow: 0 0 8px rgba(34, 197, 94, 0.5);';
            } else if(status === 'Absent') {
                cardStyle = 'background: rgba(220, 38, 38, 0.08); border: 2px solid rgba(220, 38, 38, 0.2); color: #991b1b;';
                statusText = 'ABSENT';
                statusDot = 'background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);';
            } else if(status === 'Leave') {
                cardStyle = 'background: rgba(37, 99, 235, 0.08); border: 2px solid rgba(37, 99, 235, 0.2); color: #1e40af;';
                statusText = 'LEAVE';
                statusDot = 'background: #3b82f6; box-shadow: 0 0 8px rgba(59, 130, 246, 0.5);';
            } else if(status === 'Struck Off') {
                cardStyle = 'background: rgba(107, 114, 128, 0.08); border: 2px solid rgba(107, 114, 128, 0.2); color: #374151;';
                statusText = 'STRUCK OFF';
                statusDot = 'background: #6b7280; box-shadow: 0 0 8px rgba(107, 114, 128, 0.5);';
            }

            // Photo Logic - handle both full paths and filename-only formats
            let photoUrl;
            if (item.profile_pic) {
                // Check if it's already a full path (contains /) or just a filename
                if (item.profile_pic.includes('/')) {
                    photoUrl = `../../${item.profile_pic}`;
                } else {
                    // Just a filename, assume it's in assets/uploads/
                    photoUrl = `../../assets/uploads/${item.profile_pic}`;
                }
            } else {
                photoUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(item.name)}&background=random&color=fff`;
            }
            
            html += `
            <div class="manual-item" 
                 onclick="toggleManualItem(this, '${item.id}')"
                 style="
                    flex: 1 0 130px; 
                    max-width: 160px;
                    ${cardStyle}
                    border-radius: 16px; 
                    padding: 12px; 
                    cursor: pointer; 
                    user-select: none;
                    text-align: center;
                    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
                    box-shadow: 0 2px 4px rgba(0,0,0,0.02);
                    position: relative;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 8px;
                 ">
                
                <!-- Photo Container -->
                <div style="width: 50px; height: 50px; border-radius: 50%; overflow: hidden; border: 2px solid white; box-shadow: 0 4px 10px rgba(0,0,0,0.1); background: #eee;">
                    <img src="${photoUrl}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(item.name)}&background=6366f1&color=fff'">
                </div>

                <div style="flex: 1;">
                    <div style="font-size: 11px; font-weight: 800; color: inherit; line-height: 1.2; margin-bottom: 2px;">${item.name}</div>
                    <div style="font-size: 9px; opacity: 0.6; font-weight: 700;">${labelLabel}</div>
                </div>

                <div style="display: flex; align-items: center; gap: 5px; margin-top: 4px; flex-direction: column;">
                    <div style="display: flex; align-items: center; gap: 5px;">
                        <span style="width: 6px; height: 6px; border-radius: 50%; ${statusDot}"></span>
                        <span style="font-size: 8px; font-weight: 900; letter-spacing: 0.5px; text-transform:uppercase; opacity: 0.8;">${statusText}</span>
                    </div>
                    ${item.time_in || item.time_out ? `
                        <div style="font-size: 8px; font-weight: 700; opacity: 0.6; margin-top: 2px;">
                            ${item.time_in ? item.time_in.substring(0,5) : ''}${item.time_out ? ' - ' + item.time_out.substring(0,5) : ''}
                        </div>
                    ` : ''}
                </div>

                <!-- Selection Checkmark (Visible when selected) -->
                <div class="selection-indicator" style="position: absolute; top: 8px; right: 8px; width: 18px; height: 18px; border-radius: 50%; background: #10b981; color: white; display: none; align-items: center; justify-content: center; font-size: 10px; border: 2px solid white; box-shadow: 0 2px 5px rgba(16,185,129,0.3);">
                    <i class="fas fa-check"></i>
                </div>
            </div>`;
        });
        
        html += '</div>';
        container.innerHTML = html;
    }

    function toggleManualItem(el, id) {
        if (manualSelection.has(id)) {
            manualSelection.delete(id);
            el.classList.remove('selected');
        } else {
            manualSelection.add(id);
            el.classList.add('selected');
        }
    }

    function toggleSelectAllManual(isSelected) {
        const items = document.querySelectorAll('.manual-item');
        items.forEach(el => {
            const id = el.getAttribute('onclick').match(/'([^']+)'/)[1];
            if (isSelected) {
                if (!manualSelection.has(id)) {
                    manualSelection.add(id);
                    el.classList.add('selected');
                }
            } else {
                manualSelection.delete(id);
                el.classList.remove('selected');
            }
        });
    }

    async function applyManualStatus(status) {
        if (manualSelection.size === 0) {
            alert("Please select at least one person.");
            return;
        }

        const selectedIds = Array.from(manualSelection);
        
        if (currentManualContext === 'student') {
            const bulk_in = document.getElementById('manual_std_time_in').value;
            const bulk_out = document.getElementById('manual_std_time_out').value;

            let statusMap = {};
            selectedIds.forEach(id => {
                statusMap[id] = { 
                    status: status,
                    time_in: bulk_in,
                    time_out: bulk_out
                }; 
            });

            const { cid, sid, date } = currentManualParams;
            
            try {
                const res = await fetch(`attendance_api.php?action=save_student_attendance_bulk`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        class_id: cid,
                        section_id: sid,
                        date: date,
                        status: statusMap
                    })
                });
                await res.json();
                alert("Saved Successfully!");
                manualSelection.clear();
                refreshManualModalData();
            } catch(e) {
                alert("Save Failed");
            }

        } else if (currentManualContext === 'staff') {
            const bulk_in = document.getElementById('manual_staff_time_in').value;
            const bulk_out = document.getElementById('manual_staff_time_out').value;

             let attendanceMap = {};
            selectedIds.forEach(id => {
                attendanceMap[id] = {
                    status: status,
                    time_in: bulk_in,
                    time_out: bulk_out
                };
            });

            const { date } = currentManualParams;
            
            try {
                const res = await fetch(`attendance_api.php?action=save_staff_attendance`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        date: date,
                        attendances: attendanceMap
                    })
                });
                await res.json();
                alert("Saved Successfully!");
                manualSelection.clear();
                refreshManualModalData();
            } catch(e) {
                alert("Save Failed");
            }
        }
    }
    </script>

    <!-- Device Manager Service (Persistent Session) -->
    <script src="../../assets/js/device-manager-service.js"></script>

    <!-- Face-API.js Library for AI Face Recognition -->
    <script src="https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/dist/face-api.min.js"></script>

    <script>
        // ========== FACE RECOGNITION SYSTEM (REFRESHED) ==========
        let faceVideoFeed = null;
        let faceVideoDisplay = null; // New visible feed
        let faceOverlay = null;
        let faceLiveStatus = null;
        let faceLoaderMsg = null;
        let faceVideoPlaceholder = null;
        let faceScanLine = null;
        let knownFacesContainer = null;
        let unknownFacesContainer = null;
        
        let faceMatcher = null;
        let isFaceSystemStarted = false;
        let isFaceProcessing = false;
        let lastFaceDetections = new Set();
        let forceAttendanceActive = false; // System-wide manual override flag

        // Initialize Face Recognition when Smart Attendance tab is opened
        function initFaceRecognition() {
            if (isFaceSystemStarted) return; 
            
            faceVideoFeed = document.getElementById('faceVideoDisplay'); // USE VISIBLE FEED For AI (Performance Boost)
            faceVideoDisplay = document.getElementById('faceVideoDisplay'); // Visible display feed
            faceOverlay = document.getElementById('faceOverlay');
            faceLiveStatus = document.getElementById('face-live-status');
            faceLoaderMsg = document.getElementById('faceLoaderMsg');
            faceVideoPlaceholder = document.getElementById('faceVideoPlaceholder');
            faceScanLine = document.getElementById('faceScanLine');
            knownFacesContainer = document.getElementById('knownFacesContainer');
            unknownFacesContainer = document.getElementById('unknownFacesContainer');
            
            if (!faceVideoFeed || !faceVideoDisplay) return;
            
            // Respect Master System Switch
            if (!systemMasterState) {
                faceLoaderMsg.innerHTML = '<span style="color:#000000 !important; font-weight:900;">SYSTEM OFF</span>';
                if(faceVideoPlaceholder) faceVideoPlaceholder.style.display = 'flex';
                if(faceVideoDisplay) {
                     faceVideoDisplay.src = "";
                     faceVideoDisplay.style.display = 'none';
                }
                return;
            }

            // Respect Settings
            if (forceAttendanceActive || smartSettings.faceActive) {
                startFaceDetectionSystem();
            } else {
                faceLoaderMsg.innerHTML = '<span style="color:#e74c3c; font-weight:800;">VISION ENGINE DISABLED</span><br><span style="font-size:10px; opacity:0.7;">Enable in Settings or click "Activate Now"</span>';
                if(faceVideoPlaceholder) faceVideoPlaceholder.style.display = 'flex';
                if(faceVideoDisplay) faceVideoDisplay.style.display = 'none';
            }
        }

        let isFingerprintSDKInitialized = false;
        function initFingerprintSystem() {
            // DeviceManager: Skip full re-init if already done this session
            if (typeof DeviceManager !== 'undefined' && DeviceManager.isReady('fingerprint')) {
                console.log('[FINGERPRINT] Session-cached — skipping SDK re-initialization');
                isFingerprintSDKInitialized = true;
                loadFingerprintTemplates();
                return;
            }

            loadFingerprintTemplates();
            if (!isFingerprintSDKInitialized) {
                try {
                    if (typeof window.fingerprintSDK === 'undefined') {
                        console.error('[FINGERPRINT] SecuGen SDK not loaded. Service may not be running.');
                        const mainStatus = document.getElementById('fingerprintMainStatus');
                        const subStatus = document.getElementById('fingerprintSubStatus');
                        if(mainStatus) mainStatus.textContent = "SERVICE ERROR";
                        if(subStatus) subStatus.innerHTML = `SecuGen Biometric Service not detected.<br>Please restart the service from Windows Services (services.msc).`;
                        return;
                    }
                    window.fingerprintSDK.initialize();
                    isFingerprintSDKInitialized = true;
                    if (typeof DeviceManager !== 'undefined') DeviceManager.markReady('fingerprint');
                    console.log('[FINGERPRINT] SDK initialized successfully');
                } catch(e) {
                    console.error('[FINGERPRINT] SDK initialization failed:', e);
                    const mainStatus = document.getElementById('fingerprintMainStatus');
                    const subStatus = document.getElementById('fingerprintSubStatus');
                    if(mainStatus) mainStatus.textContent = "INITIALIZATION FAILED";
                    if(subStatus) subStatus.innerHTML = `Error: ${e.message}<br>Check if SecuGen service is running.`;
                }
            }
        }

        async function startFaceDetectionSystem() {
            try {
                if (typeof faceapi === 'undefined') {
                    throw new Error("AI Library (face-api.js) not loaded. Check internet/CDN.");
                }

                isFaceSystemStarted = true;
                faceLoaderMsg.textContent = "AI BRAIN BOOTING...";
                
                const MODEL_URL = '../../FaceDetection/assets/models';
                
                // DeviceManager: Skip model download if cached in session
                const modelsAlreadyCached = (typeof DeviceManager !== 'undefined' && DeviceManager.isReady('faceModels'));
                
                if (modelsAlreadyCached) {
                    console.log('[FACE] AI models cached from previous load — skipping download');
                    faceLoaderMsg.textContent = "RESTORING AI ENGINE...";
                } else {
                    // Show progress in loader message
                    faceLoaderMsg.textContent = "LOADING AI MODELS...";
                }
                
                // face-api.js uses browser cache internally, but we still call loadFromUri.
                // On second load, it completes almost instantly from browser cache.
                await Promise.all([
                    faceapi.nets.ssdMobilenetv1.loadFromUri(MODEL_URL),
                    faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
                    faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL)
                ]);
                
                if (typeof DeviceManager !== 'undefined') DeviceManager.markFaceModelsCached();
                
                faceLoaderMsg.textContent = "INDEXING PERSONNEL...";
                await loadRegisteredFacesData();
                
                faceLoaderMsg.textContent = "HANDSHAKING CAMERA...";
                
                // Add timestamp to prevent caching / force new stream
                const streamUrl = "../../FaceDetection/camera_proxy.php?t=" + new Date().getTime();
                console.log("[FACE] Attempting to load stream from:", streamUrl);
                
                // Set both feeds - one for AI, one for Display
                // Try to find the correct element (handling potential ID mismatches)
                const videoEl = document.getElementById('faceVideoDisplay') || document.getElementById('faceVideoFeed');
                if(!videoEl) throw new Error("Video Element Not Found");

                // Update global references if needed
                if(!faceVideoDisplay) faceVideoDisplay = videoEl;
                if(!faceVideoFeed) faceVideoFeed = videoEl;

                videoEl.src = streamUrl;
                
                // Set a timeout to warn about MJPEG/Codec issues if it takes too long
                const connectionTimeout = setTimeout(() => {
                    if (faceLoaderMsg.textContent.includes("HANDSHAKING")) {
                        faceLoaderMsg.innerHTML = `
                            <span style="color:#e74c3c">STREAM TIMEOUT</span><br>
                            <span style="font-size:10px; opacity:0.7">URL: ${streamUrl}</span><br>
                            <span style="font-size:10px; opacity:0.7">1. Check User/Pass in Settings</span><br>
                            <span style="font-size:10px; opacity:0.7">2. Ensure Camera Encode is <b>MJPEG</b></span>
                        `;
                    }
                }, 15000);

                videoEl.onload = function() {
                    clearTimeout(connectionTimeout);
                    console.log("[FACE] ✓ Video stream loaded successfully");
                    if(faceVideoPlaceholder) faceVideoPlaceholder.style.display = "none";
                    videoEl.style.display = "block";
                    // Scan line removed
                };
                
                videoEl.onerror = function(e) {
                    console.error("[FACE] ✗ Stream error:", e);
                    faceLoaderMsg.textContent = "CAMERA ERROR - CHECK CONSOLE";
                    if(faceVideoPlaceholder) faceVideoPlaceholder.style.display = "flex";
                    videoEl.style.display = "none";
                    setTimeout(() => {
                        if(!systemMasterState || !isFaceSystemStarted) return;
                        console.log("[FACE] Retrying connection...");
                        const newUrl = "../../FaceDetection/camera_proxy.php?t=" + new Date().getTime();
                        videoEl.src = newUrl;
                    }, 3000);
                };
                
                console.log("[FACE] Video element configured");
                
                if(faceLiveStatus) {
                    faceLiveStatus.textContent = "LIVE";
                    faceLiveStatus.style.background = "#2ecc71";
                }
                
                // Optimized frequency for smooth streaming (1 detection per second)
                setInterval(runFaceDetection, 1000);
            } catch (error) {
                console.error("AI System failure:", error);
                isFaceSystemStarted = false; // Allow retry
                faceLoaderMsg.innerHTML = `<span style="color:#e74c3c;">SYSTEM ERROR</span><br><small style="font-size:9px; opacity:0.5; display:block; margin-top:5px;">${error.message}</small><br><button onclick="location.reload()" style="background:#e74c3c; border:none; color:white; padding:5px 10px; border-radius:5px; font-size:10px; margin-top:10px; cursor:pointer;">RELOAD SYSTEM</button>`;
                if(faceLiveStatus) faceLiveStatus.textContent = "FAULT";
            }
        }
        async function loadRegisteredFacesData() {
            try {
                const resp = await fetch('../../FaceDetection/get_registered_faces.php');
                const data = await resp.json();
                
                const labeledDescriptors = await Promise.all(
                    data.map(async person => {
                        try {
                            const img = await faceapi.fetchImage('../../FaceDetection/' + person.image_path);
                            const detections = await faceapi.detectSingleFace(img, new faceapi.SsdMobilenetv1Options()).withFaceLandmarks().withFaceDescriptor();
                            if (detections) return new faceapi.LabeledFaceDescriptors(person.user_id + "::" + person.name, [detections.descriptor]);
                        } catch (e) {}
                        return null;
                    })
                );
                
                const filtered = labeledDescriptors.filter(ld => ld !== null);
                if (filtered.length > 0) faceMatcher = new faceapi.FaceMatcher(filtered, 0.45); // Stricter threshold for accuracy
            } catch (e) { console.error("Data refresh failed:", e); }
        }


        let browserAIEnabled = true;
        function toggleBrowserAI() {
            browserAIEnabled = !browserAIEnabled;
            const statusEl = document.getElementById('browser-ai-status');
            const toggleEl = document.getElementById('browser-ai-toggle');
            if (browserAIEnabled) {
                statusEl.textContent = 'ON';
                toggleEl.style.borderColor = 'var(--primary)';
                toggleEl.style.background = 'rgba(99, 102, 241, 0.1)';
            } else {
                statusEl.textContent = 'PAUSED';
                toggleEl.style.borderColor = 'rgba(239, 68, 68, 0.3)';
                toggleEl.style.background = 'rgba(239, 68, 68, 0.1)';
                // Clear overlay
                const ctx = faceOverlay.getContext('2d');
                ctx.clearRect(0, 0, faceOverlay.width, faceOverlay.height);
            }
        }

        async function runFaceDetection() {
            if (!browserAIEnabled) return; // SKIP Browser AI if paused

            // Respect Settings Dynamic Check
            if (!forceAttendanceActive && !smartSettings.faceActive) {
                if(isFaceSystemStarted) {
                     console.log("Stopping Face Engine (Settings Changed)");
                     isFaceSystemStarted = false;
                     if(faceLoaderMsg) faceLoaderMsg.innerHTML = '<span style="color:#e74c3c">DISABLED</span>';
                }
                return;
            }

            if (!isFaceSystemStarted || isFaceProcessing) return;
            // Check if video is actually loaded, has dimensions, and isn't buffering
            if (!faceVideoFeed || faceVideoFeed.naturalHeight === 0 || faceVideoFeed.naturalWidth === 0) return;
            
            isFaceProcessing = true;
            if (faceOverlay.width !== faceVideoFeed.width) {
                faceOverlay.width = faceVideoFeed.width;
                faceOverlay.height = faceVideoFeed.height;
            }
            
            try {
                // Quality Settings: Higher inputSize (512 or 608) = Better detection but slower. 
                // 416 is default. 512 is a good balance for HD streams.
                const options = new faceapi.SsdMobilenetv1Options({ 
                    minConfidence: 0.5, // Lower slightly to extend range
                    maxResults: 5 // Limit processing
                });
                // We let face-api handle resizing internally based on the input image
                
                const detections = await faceapi.detectAllFaces(faceVideoFeed, options)
                    .withFaceLandmarks()
                    .withFaceDescriptors();
                
                const displaySize = { width: faceVideoFeed.width, height: faceVideoFeed.height };
                const resizedDetections = faceapi.resizeResults(detections, displaySize);
                const ctx = faceOverlay.getContext('2d');
                ctx.clearRect(0, 0, faceOverlay.width, faceOverlay.height);
                
                resizedDetections.forEach(detection => {
                    let fullLabel = "Unknown";
                    let displayName = "Unknown";
                    let color = "#e74c3c";
                    
                    let bestMatch = null;
                    if (faceMatcher) {
                         bestMatch = faceMatcher.findBestMatch(detection.descriptor);
                        if (bestMatch.label !== 'unknown') {
                            fullLabel = bestMatch.label;
                            displayName = fullLabel.split("::")[1] || fullLabel;
                            color = "#2ecc71";
                        }
                    }
                    
                const box = detection.detection.box;
                ctx.strokeStyle = color;
                ctx.lineWidth = 4;
                ctx.strokeRect(box.x, box.y, box.width, box.height);
                
                // Show Name and Distance (Confidence)
                const distStr = bestMatch ? ` (${(1 - bestMatch.distance).toFixed(2)})` : "";
                
                ctx.fillStyle = color;
                ctx.fillRect(box.x, box.y - 25, box.width, 25);
                ctx.fillStyle = "#fff";
                ctx.font = "bold 13px Arial";
                ctx.fillText(displayName + distStr, box.x + 5, box.y - 8);
                
                handleFaceDetectionUI(fullLabel);
            });
        } catch (e) {}
            isFaceProcessing = false;
        }

        let markedUsersSession = new Set();
        let displayedUiFacesSession = new Set();

        async function handleFaceDetectionUI(fullLabel) {
            const parts = fullLabel.split("::");
            const userId = parts[0];
            const name = parts[1] || "Unknown";
            const isUnknown = (fullLabel === 'Unknown');

            // --- OPTIMIZATION START ---
            if (isUnknown) return;
            if (displayedUiFacesSession.has(userId)) return;
            displayedUiFacesSession.add(userId);
            if (markedUsersSession.has(userId)) return;
            // --- OPTIMIZATION END ---

            const time = new Date().toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit' });
            const listKey = fullLabel + "_" + Math.floor(Date.now() / 10000); // 10s cooldown
            if (lastFaceDetections.has(listKey)) return;
            lastFaceDetections.add(listKey);
            
            let statusText = isUnknown ? 'DENIED' : 'MATCH';
            let statusStyle = isUnknown 
                ? 'background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2);' 
                : 'background: rgba(16, 185, 129, 0.1); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.2);';
            
            if (!isUnknown && smartSettings.autoMark) {
                if (isAttendanceTimeValid()) {
                    markedUsersSession.add(userId);

                    const faceAttPayload = { 
                        user_id: userId, 
                        type: 'face', 
                        score: 0.9,
                        mode: determineAttendanceAction() 
                    };

                    try {
                        const resp = await fetch('attendance_api.php?action=mark_smart_attendance', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(faceAttPayload)
                        });
                        const res = await resp.json();
                        
                        // Show Toast Notification
                        if (res.success) {
                            showAttendanceToast(res.user, res.mode, res.time);
                        }
                        
                        let voiceMsg = name + " verified";
                        if (res.already_marked) {
                            statusText = 'ALREADY MARKED';
                            statusStyle = 'background: rgba(243, 156, 18, 0.1); color: #f39c12; border: 1px solid rgba(243, 156, 18, 0.2);';
                            voiceMsg = name + " already marked";
                        } else {
                            statusText = 'PRESENT';
                            statusStyle = 'background: rgba(52, 152, 219, 0.1); color: #3498db; border: 1px solid rgba(52, 152, 219, 0.2);';
                            voiceMsg = name + " verified";
                        }

                        if (smartSettings.sound) {
                            const synth = window.speechSynthesis;
                            if (synth) {
                                const utter = new SpeechSynthesisUtterance(voiceMsg);
                                utter.rate = 1.1;
                                synth.speak(utter);
                            }
                        }
                    } catch(e) {
                        console.error("Face Marking Error:", e);
                        // OFFLINE FALLBACK: Queue for later sync
                        if (typeof DeviceManager !== 'undefined') {
                            DeviceManager.queueOffline(faceAttPayload);
                            console.log('[DEVICE-MANAGER] Face attendance queued offline');
                        }
                    }
                } else {
                    statusText = 'OFF SCHEDULE';
                    statusStyle = 'background: rgba(231, 76, 60, 0.1); color: #e74c3c; border: 1px solid rgba(231, 76, 60, 0.2);';
                }
            }

            const card = document.createElement('div');
            card.className = 'detection-item';
            
            card.innerHTML = `
                <div class="detection-avatar" style="background: ${isUnknown ? 'rgba(239, 68, 68, 0.1)' : 'rgba(99, 102, 241, 0.1)'}; color: ${isUnknown ? '#ef4444' : '#6366f1'}; border: none;">
                    <i class="fas ${isUnknown ? 'fa-user-secret' : 'fa-user-check'}"></i>
                </div>
                <div class="detection-info">
                    <div class="detection-name">${name}</div>
                    <div class="detection-time">${time} • HW_ID: ${userId || '000'} • SECURE_LOG</div>
                </div>
                <div class="detection-status" style="${statusStyle}">
                    ${statusText}
                </div>
            `;
            
            const container = isUnknown ? unknownFacesContainer : knownFacesContainer;
            if (container.querySelector('div[style*="opacity: 0.5"]')) container.innerHTML = ''; 
            container.prepend(card);
            if (container.children.length > 20) container.lastChild.remove();
        }


        // ---------- ENROLLMENT MODAL LOGIC ----------
        function openFaceEnrollModal() { 
            document.getElementById('enroll_modal_current_mode').value = 'face';
            document.getElementById('enrollModalTitle').innerHTML = '<i class="fas fa-id-card-alt" style="margin-right: 10px;"></i> FACE ENROLLMENT';
            document.getElementById('enroll-tip-text').innerHTML = '<b>NEXT STEP:</b> You will take 3 high-definition snapshots of the selected person to train the AI model. Ensure clear lighting.';
            document.getElementById('fingerprint-enroll-fields').style.display = 'none';
            document.querySelectorAll('.enroll-type-btn').forEach(btn => btn.style.display = 'block');
            document.getElementById('enrollExistingBtn').style.display = (currentEnrollType === 'bulk' ? 'none' : 'block');
            
            // Reset button state
            const exBtn = document.getElementById('enrollExistingBtn');
            exBtn.innerHTML = '<i class="fas fa-upload" style="margin-right: 10px;"></i> UPLOAD & ENROLL PHOTO';
            exBtn.style.background = '#fff';
            exBtn.style.color = '#000';
            exBtn.style.border = '2px solid #000';
            exBtn.disabled = false;

            document.getElementById('faceEnrollModal').style.display = 'flex'; 
            updateUserBiometricInfo(); // Refresh info if user is already selected
        }

        function openFingerEnrollModal() {
            initFingerprintSystem(); // Ensure SDK is ready
            document.getElementById('enroll_modal_current_mode').value = 'fingerprint';
            document.getElementById('enrollModalTitle').innerHTML = '<i class="fas fa-fingerprint" style="margin-right: 10px;"></i> FINGERPRINT ENROLLMENT';
            document.getElementById('enroll-tip-text').innerHTML = '<b>NEXT STEP:</b> You will place your finger on the scanner to capture your unique fingerprint template.';
            document.getElementById('fingerprint-enroll-fields').style.display = 'block';
            document.getElementById('enroll-type-bulk').style.display = 'none';
            document.getElementById('enrollExistingBtn').style.display = 'none';
            if(currentEnrollType === 'bulk') setEnrollType('student', document.getElementById('enroll-type-student'));
            document.getElementById('faceEnrollModal').style.display = 'flex';
            updateUserBiometricInfo(); // Refresh info if user is already selected
        }

        function closeFaceEnrollModal() { 
            document.getElementById('faceEnrollModal').style.display = 'none'; 
            document.getElementById('user_biometric_info').style.display = 'none'; // Hide info
            goBackToEnrollStep1();
            if(document.getElementById('bulk_selection_status')) document.getElementById('bulk_selection_status').style.display = 'none';
            bulkFiles = [];
        }
        
        let currentEnrollType = 'student';
        let bulkFiles = [];
        function setEnrollType(type, btn) {
            currentEnrollType = type;
            document.querySelectorAll('.enroll-type-btn').forEach(b => {
                b.style.borderColor = 'rgba(255,255,255,0.05)';
                b.querySelector('i').style.opacity = '0.5';
                b.querySelector('div').style.opacity = '0.5';
            });
            btn.style.borderColor = '#3498db';
            btn.querySelector('i').style.opacity = '1';
            btn.querySelector('div').style.opacity = '1';
            
            document.getElementById('student-enroll-fields').style.display = (type === 'student' ? 'block' : 'none');
            document.getElementById('staff-enroll-fields').style.display = (type === 'staff' ? 'block' : 'none');
            document.getElementById('bulk-enroll-fields').style.display = (type === 'bulk' ? 'block' : 'none');
            
            const personnelParent = document.getElementById('enroll_user_sel').parentElement;
            personnelParent.style.display = (type === 'bulk' ? 'none' : 'block');
            
            const isFaceMode = document.getElementById('enroll_modal_current_mode').value === 'face';
            document.getElementById('enrollExistingBtn').style.display = (type === 'bulk' || !isFaceMode ? 'none' : 'block');
            
            const tipBox = document.querySelector('#enroll-step-1 div[style*="background: rgba(241, 196, 15, 0.1)"]');
            if (type === 'bulk') {
                if(tipBox) tipBox.style.display = 'none';
                document.getElementById('enrollProceedBtn').innerHTML = 'START BULK ENROLLMENT <i class="fas fa-upload" style="margin-left: 10px;"></i>';
                document.getElementById('enrollProceedBtn').style.background = '#fff';
                document.getElementById('enrollProceedBtn').style.color = '#000';
                document.getElementById('enrollProceedBtn').style.border = '2px solid #000';
            } else {
                if(tipBox) tipBox.style.display = 'block';
                document.getElementById('enrollProceedBtn').innerHTML = 'PROCEED TO CAPTURE <i class="fas fa-arrow-right" style="margin-left: 10px;"></i>';
                document.getElementById('enrollProceedBtn').style.background = '#fff';
                document.getElementById('enrollProceedBtn').style.color = '#000';
                document.getElementById('enrollProceedBtn').style.border = '2px solid #000';
            }

            if (type !== 'bulk') fetchEnrollUsers();
        }

        function handleBulkFolderSelect(input) {
            const files = Array.from(input.files);
            bulkFiles = files.filter(f => f.type.startsWith('image/'));
            
            const status = document.getElementById('bulk_selection_status');
            const countDisplay = document.getElementById('bulk_file_count_display');
            if (bulkFiles.length > 0) {
                status.style.display = 'block';
                countDisplay.textContent = `${bulkFiles.length} PHOTOS READY`;
            } else {
                status.style.display = 'none';
                alert("No valid images selected. Please choose image files.");
            }
        }

        async function fetchEnrollSections(classId) {
            const sectionSel = document.getElementById('enroll_section_sel');
            if (!classId) {
                sectionSel.innerHTML = '<option value="">SELECT SECTION</option>';
                fetchEnrollUsers();
                return;
            }
            try {
                console.log(`[ENROLL] Fetching sections for class ID: ${classId}`);
                const resp = await fetch(`attendance_api.php?action=get_sections&class=${encodeURIComponent(classId)}`);
                const sections = await resp.json();
                console.log(`[ENROLL] Sections received:`, sections);
                
                let html = '<option value="">SELECT SECTION</option>';
                if(Array.isArray(sections)) {
                    if (sections.length === 0) {
                        html = '<option value="">NO SECTIONS FOUND</option>';
                    } else {
                        sections.forEach(s => html += `<option value="${s.id}">${s.section}</option>`);
                    }
                } else if (sections.error) {
                    console.error("[ENROLL] API Error:", sections.error);
                    html = `<option value="">ERROR: ${sections.error}</option>`;
                }
                sectionSel.innerHTML = html;
            } catch (e) {
                console.error("[ENROLL] Network/Parse Error:", e);
                sectionSel.innerHTML = '<option value="">NETWORK ERROR</option>';
            }
            fetchEnrollUsers();
        }

        async function fetchEnrollUsers() {
            const userSel = document.getElementById('enroll_user_sel');
            if(!userSel) return;
            userSel.innerHTML = '<option value="">SEARCHING...</option>';
            
            let url = `attendance_api.php?action=get_enroll_users&type=${currentEnrollType}`;
            if (currentEnrollType === 'student') {
                url += `&class=${encodeURIComponent(document.getElementById('enroll_class_sel').value)}&section=${encodeURIComponent(document.getElementById('enroll_section_sel').value)}`;
            } else {
                url += `&category=${encodeURIComponent(document.getElementById('enroll_staff_cat_sel').value)}`;
            }
            
            const resp = await fetch(url);
            const users = await resp.json();
            let html = '<option value="">SELECT PERSONNEL</option>';
            users.forEach(u => html += `<option value="${u.id}">${u.name} [ID:${u.id}]</option>`);
            userSel.innerHTML = (users.length ? html : '<option value="">NO USERS FOUND</option>');
        }

        function startFaceCapture() {
            document.getElementById('enrollCaptureFeed').src = "../../FaceDetection/camera_proxy.php?t=" + new Date().getTime();
        }

        async function proceedToEnrollStep2() {
            const mode = document.getElementById('enroll_modal_current_mode').value;
            if (currentEnrollType === 'bulk' && mode === 'face') {
                if (bulkFiles.length === 0) { alert("Please select a folder with valid student photos."); return; }
                if (!document.getElementById('enroll_class_sel').value) { alert("Please select a class."); return; }
                startBulkEnrollment();
                return;
            }

            if (!document.getElementById('enroll_user_sel').value) { alert("Please select a person."); return; }
            
            document.getElementById('enroll-step-1').style.display = 'none';
            document.getElementById('enroll-step-2').style.display = 'block';
            document.getElementById('enrollModalBack').style.display = 'block';

            if (mode === 'face') {
                document.getElementById('face-capture-ui').style.display = 'block';
                document.getElementById('fingerprint-capture-ui').style.display = 'none';
                startFaceCapture();
            } else {
                document.getElementById('face-capture-ui').style.display = 'none';
                document.getElementById('fingerprint-capture-ui').style.display = 'block';
            }
        }

        async function startBulkEnrollment() {
            const classId = document.getElementById('enroll_class_sel').value;
            const sectionId = document.getElementById('enroll_section_sel').value;
            
            if (!classId || !sectionId) {
                alert('Please select both class and section.');
                return;
            }

            if (bulkFiles.length === 0) {
                alert('Please select at least one photo.');
                return;
            }

            if (!confirm(`Are you sure you want to upload and enroll ${bulkFiles.length} photos?`)) return;

            const btn = document.getElementById('enrollProceedBtn');
            btn.disabled = true;
            const originalHTML = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> UPLOADING BATCH...';

            const formData = new FormData();
            formData.append('action', 'upload_bulk_photos');
            formData.append('class_id', classId);
            formData.append('section_id', sectionId);
            
            bulkFiles.forEach(file => {
                formData.append('photos[]', file);
            });

            try {
                const resp = await fetch('bulk_photo_upload_api.php', {
                    method: 'POST',
                    body: formData
                });
                const result = await resp.json();

                if (result.success) {
                    let msg = `Successfully enrolled ${result.uploaded} students!`;
                    if (result.skipped > 0) msg += `\nSkipped ${result.skipped} (matches not found).`;
                    alert(msg);
                    closeFaceEnrollModal();
                    if(typeof loadRegisteredFacesData === 'function') loadRegisteredFacesData();
                } else {
                    alert('Error: ' + (result.error || 'Unknown error'));
                }
            } catch (e) {
                console.error(e);
                alert('Upload failed: ' + e.message);
            } finally {
                btn.disabled = false;
                btn.innerHTML = originalHTML;
            }
        }

        const toBase64 = file => new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = () => resolve(reader.result);
            reader.onerror = error => reject(error);
        });

        function goBackToEnrollStep1() {
            document.getElementById('enroll-step-2').style.display = 'none';
            document.getElementById('enroll-step-1').style.display = 'block';
            document.getElementById('enrollModalBack').style.display = 'none';
            document.getElementById('enrollCaptureFeed').src = "";
            const mainFeed = document.getElementById('faceVideoFeed');
            if(mainFeed && systemMasterState && document.getElementById('face-recognition-panel').classList.contains('active')) {
                mainFeed.src = "../../FaceDetection/camera_proxy.php?t=" + new Date().getTime();
            }
        }
// ... [Retaining existing code in between] ...
        function resetFaceEnrollment(skipInnerRefresh = false) {
            captureCount = 0;
            document.querySelectorAll('.capture-pill').forEach(p => { 
                p.style.background = 'rgba(255,255,255,0.05)'; 
                p.style.color = 'inherit'; 
            });
            const captureStatus = document.getElementById('capture-status');
            if (captureStatus) { captureStatus.textContent = "POSITION FACE IN THE CIRCLE"; captureStatus.style.color = "#2ecc71"; }
            const faceCaptureBtn = document.getElementById('faceCaptureBtn');
            if (faceCaptureBtn) { faceCaptureBtn.disabled = false; faceCaptureBtn.innerHTML = '<i class="fas fa-camera" style="margin-right: 10px;"></i> CAPTURE SNAPSHOT'; faceCaptureBtn.style.background = '#fff'; faceCaptureBtn.style.color = '#000'; }
            
            // Restart feed to ensure freshness
            if (!skipInnerRefresh && typeof startFaceCapture === 'function') startFaceCapture();
            
            // Also refresh the biometric slots
            if (!skipInnerRefresh) refreshEnrollmentSlots();


            console.log("[ENROLL] Enrollment state reset.");
        }

        function resetFingerprintCaptureUI() {
            const statusText = document.getElementById('fingerprint-capture-status');
            const previewImg = document.getElementById('fingerprintPreviewImg');
            const previewContainer = document.getElementById('fingerprintImgPreview');
            const placeholder = document.getElementById('fingerprintIconPlaceholder');
            const btn = document.getElementById('fingerCaptureBtn');
            
            if (statusText) { statusText.textContent = "READY FOR SCAN..."; statusText.style.color = ""; }
            if (btn) {
                btn.disabled = false;
                btn.style.display = 'block';
            }
            const controls = document.getElementById('fingerprint-enroll-controls');
            if (controls) controls.style.display = 'none';
        }


        // Dedicated refresh: re-fetches registered slots from DB for the currently selected user
        async function refreshEnrollmentSlots() {
            const userId = document.getElementById('enroll_user_sel') ? document.getElementById('enroll_user_sel').value : '';
            const list = document.getElementById('biometric_info_list');
            const container = document.getElementById('user_biometric_info');

            // Spin the refresh icon as visual feedback
            const syncIcon = document.querySelector('[onclick="refreshEnrollmentSlots()"]');
            if (syncIcon) { syncIcon.classList.add('fa-spin'); }

            // Reset Capture UIs
            if (typeof resetFaceEnrollment === 'function') resetFaceEnrollment(true);
            if (typeof resetFingerprintCaptureUI === 'function') resetFingerprintCaptureUI();

            // Always reload the global templates cache from DB
            if (typeof loadFingerprintTemplates === 'function') await loadFingerprintTemplates();
            
            // If in face mode, also refresh face recognition system
            const enrollMode = document.getElementById('enroll_modal_current_mode') ? document.getElementById('enroll_modal_current_mode').value : 'face';
            if (enrollMode === 'face' && typeof loadRegisteredFacesData === 'function') {
                loadRegisteredFacesData(); // Run without await to make UI refresh quick
            }

            if (userId) {
                // Fetch per-user biometric info from API
                if (typeof updateUserBiometricInfo === 'function') await updateUserBiometricInfo();
            } else {
                // No user selected — just clear the list
                if (list) list.innerHTML = '<div style="font-size:11px; opacity:0.5; padding:5px; text-align:center;">SELECT A USER TO VIEW REGISTERED SLOTS</div>';
                if (container) container.style.display = 'block';
            }

            // Stop spin
            if (syncIcon) { syncIcon.classList.remove('fa-spin'); }
            console.log('[ENROLL] Finger slots refreshed for user:', userId || 'none selected');
        }

        let captureCount = 0;
        async function takeFaceSnapshot() {
            const userId = document.getElementById('enroll_user_sel').value;
            if(!userId) return;
            
            captureCount++;
            if(captureCount <= 3) {
                const pill = document.getElementById(`shot-${captureCount}`);
                pill.style.background = '#2ecc71';
                pill.style.color = '#fff';
            }
            
            // Capture current frame from the enrollment feed
            const video = document.getElementById('enrollCaptureFeed');
            const captureCanvas = document.createElement('canvas');
            captureCanvas.width = video.naturalWidth || 640;
            captureCanvas.height = video.naturalHeight || 480;
            const ctx = captureCanvas.getContext('2d');
            ctx.drawImage(video, 0, 0, captureCanvas.width, captureCanvas.height);
            const imageData = captureCanvas.toDataURL('image/jpeg');

            if(captureCount === 3) {
                document.getElementById('capture-status').textContent = "UPLOADING BIOMETRICS...";
                document.getElementById('faceCaptureBtn').disabled = true;
                
                try {
                    const resp = await fetch(`attendance_api.php?action=save_face_enrollment`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            user_id: userId,
                            image: imageData
                        })
                    });
                    const res = await resp.json();
                    
                    if(res.success) {
                        document.getElementById('capture-status').textContent = "SUCCESS! ENROLLMENT COMPLETE.";
                        document.getElementById('capture-status').style.color = "#2ecc71";
                        await new Promise(r => setTimeout(r, 1500));
                        
                        closeFaceEnrollModal();
                        loadRegisteredFacesData(); // Refresh the AI system's memory
                        updateUserBiometricInfo();
                    } else {
                        document.getElementById('capture-status').textContent = "ERROR: " + res.error;
                        document.getElementById('capture-status').style.color = "#e74c3c";
                        alert("Error: " + res.error);
                        
                        // Reset if error
                        captureCount = 0;
                        document.querySelectorAll('.capture-pill').forEach(p => { p.style.background = 'rgba(255,255,255,0.05)'; p.style.color = 'inherit'; });
                        document.getElementById('faceCaptureBtn').disabled = false;
                    }
                } catch (e) {
                    alert("System Link Failure during Upload.");
                    captureCount = 0;
                    document.getElementById('faceCaptureBtn').disabled = false;
                } finally {
                     if(captureCount !== 3) { // Only reset if we didn't succeed
                        document.getElementById('capture-status').textContent = "POSITION FACE IN CIRCLE";
                        document.getElementById('faceCaptureBtn').style.background = '#fff';
                        document.getElementById('faceCaptureBtn').style.color = '#000';
                        document.getElementById('faceCaptureBtn').style.border = '2px solid #000';
                     }
                }
            }
        }


        async function handleSingleFileUpload(input) {
            if (!input.files || !input.files[0]) return;
            const file = input.files[0];
            const userId = document.getElementById('enroll_user_sel').value;
            
            // Reset input value immediately so the same file selection triggers change event next time
            input.value = '';

            if(!userId) { 
                alert("Please select a person first."); 
                return; 
            }

            const btn = document.getElementById('enrollExistingBtn');
            const originalText = '<i class="fas fa-upload" style="margin-right: 10px;"></i> UPLOAD & ENROLL PHOTO';
            
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> UPLOADING...';
            btn.disabled = true;

            try {
                const base64 = await toBase64(file);
                const resp = await fetch(`attendance_api.php?action=save_face_enrollment`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ user_id: userId, image: base64 })
                });
                const res = await resp.json();

                if (res.success) {
                    btn.innerHTML = '<i class="fas fa-check"></i> SUCCESS!';
                    btn.style.background = '#2ecc71';
                    btn.disabled = false; // Re-enable immediately so it can be clicked again
                    
                    // Refresh AI system in background
                    loadRegisteredFacesData();
                    
                    alert("Profile Picture Updated & Face Enrolled Successfully!");
                    
                    // Reset appearance
                    btn.innerHTML = originalText;
                    btn.style.background = '#fff';
                    btn.style.color = '#000';
                    btn.style.border = '2px solid #000';
                } else {
                    alert("Error: " + (res.error || "Failed to upload image."));
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                }
            } catch (e) {
                alert("System Error: " + e.message);
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        }

        async function enrollExistingProfilePic() {
            // Keep this for manual use if needed elsewhere, but mainly use handleSingleFileUpload
            const userId = document.getElementById('enroll_user_sel').value;
            if(!userId) { alert("Please select a person first."); return; }

            if (!confirm("Use the existing profile picture for Face Recognition?")) return;

            const btn = document.getElementById('enrollExistingBtn');
            const originalText = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> VERIFYING...';
            btn.disabled = true;

            try {
                const resp = await fetch(`attendance_api.php?action=enroll_existing_profile`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ user_id: userId })
                });
                const res = await resp.json();

                if (res.success) {
                    btn.innerHTML = '<i class="fas fa-check"></i> SUCCESS!';
                    btn.style.background = '#2ecc71';
                    
                    // Refresh AI system
                    await loadRegisteredFacesData();
                    
                    setTimeout(() => {
                        closeFaceEnrollModal();
                        btn.innerHTML = originalText;
                        btn.style.background = '#000';
                        btn.style.color = '#fff';
                        btn.disabled = false;
                        alert("Face Enrollment Updated Successfully!");
                    }, 1500);
                } else {
                    alert("Error: " + (res.error || "Profile picture not found or invalid."));
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                }
            } catch (e) {
                alert("System Error: " + e.message);
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        }

        // ========== FINGERPRINT AUTOMATION ENGINE ==========
        let isFingerprintLoopRunning = false;
        let fingerprintTemplates = [];
        let isScannerReady = false;
        // let forceAttendanceActive = false; // Note: This is already declared above or handled globally

        async function loadFingerprintTemplates() {
            try {
                const badge = document.getElementById('biometricStatsBadge');
                
                // DeviceManager: Try session-cached templates first for instant load
                if (typeof DeviceManager !== 'undefined' && DeviceManager.isReady('templates')) {
                    const cached = DeviceManager.getCachedTemplates();
                    if (cached && cached.length > 0) {
                        fingerprintTemplates = cached;
                        if(badge) {
                            badge.textContent = `${fingerprintTemplates.length} TEMPLATES SECURED (CACHED)`;
                            badge.style.background = 'rgba(16,185,129,0.1)';
                            badge.style.color = '#10b981';
                            badge.style.borderColor = 'rgba(16,185,129,0.2)';
                        }
                        console.log(`[BIOMETRIC] Using ${fingerprintTemplates.length} session-cached templates`);
                    }
                }
                
                const resp = await fetch('attendance_api.php?action=get_fingerprint_templates');
                const freshData = await resp.json();
                
                if (Array.isArray(freshData)) {
                    fingerprintTemplates = freshData;
                    if(badge) {
                        badge.textContent = `${fingerprintTemplates.length} TEMPLATES SECURED`;
                        badge.style.background = 'rgba(16,185,129,0.1)';
                        badge.style.color = '#10b981';
                        badge.style.borderColor = 'rgba(16,185,129,0.2)';
                    }
                    console.log(`[BIOMETRIC] Loaded ${fingerprintTemplates.length} templates`);
                    // DeviceManager: Cache for session persistence
                    if (typeof DeviceManager !== 'undefined') DeviceManager.cacheTemplates(freshData);
                } else {
                    throw new Error("Invalid format from API");
                }
            } catch (e) {
                console.error("Biometric Fetch Error:", e);
                const badge = document.getElementById('biometricStatsBadge');
                if(badge) badge.textContent = "DATABASE LINK FAILURE";
            }
        }
        // --- Fingerprint Management Logic ---
        let _fpCurrentTab = 'staff'; // 'staff' or 'student'

        function openFingerprintManager() {
            document.getElementById('fingerprintManagerModal').style.display = 'flex';
            const filterInput = document.getElementById('fingerprintFilter');
            if(filterInput) { filterInput.value = ''; }
            // Reset to default tab
            switchFpTab('staff');
        }

        function closeFingerprintManager() {
            document.getElementById('fingerprintManagerModal').style.display = 'none';
        }

        function switchFpTab(tab) {
            _fpCurrentTab = tab;

            const staffTab    = document.getElementById('fp-tab-staff');
            const studentTab  = document.getElementById('fp-tab-student');
            const staffFilter = document.getElementById('fp-filter-staff');
            const stuFilter   = document.getElementById('fp-filter-student');

            if (tab === 'staff') {
                staffTab.style.background   = 'var(--primary)';
                staffTab.style.color        = '#fff';
                staffTab.style.boxShadow    = '0 4px 12px rgba(99,102,241,0.3)';
                studentTab.style.background = 'rgba(255,255,255,0.05)';
                studentTab.style.color      = 'var(--text-dim)';
                studentTab.style.boxShadow  = 'none';
                staffFilter.style.display   = 'block';
                stuFilter.style.display     = 'none';
            } else {
                studentTab.style.background = 'var(--primary)';
                studentTab.style.color      = '#fff';
                studentTab.style.boxShadow  = '0 4px 12px rgba(99,102,241,0.3)';
                staffTab.style.background   = 'rgba(255,255,255,0.05)';
                staffTab.style.color        = 'var(--text-dim)';
                staffTab.style.boxShadow    = 'none';
                stuFilter.style.display     = 'flex';
                staffFilter.style.display   = 'none';
            }

            // Clear search
            const f = document.getElementById('fingerprintFilter');
            if(f) f.value = '';
            loadFingerprintManagementList();
        }

        function onFpClassChange() {
            const classId = document.getElementById('fp-student-class').value;
            const secSel  = document.getElementById('fp-student-section');
            secSel.innerHTML = '<option value="">All Sections</option>';

            if (!classId) { loadFingerprintManagementList(); return; }

            // Populate sections from PHP data embedded in JS
            const classData = <?php
                $classJson = [];
                foreach ($all_classes as $c) {
                    $classJson[$c['id']] = array_map(function($s) {
                        return ['id' => $s['id'], 'name' => $s['name']];
                    }, $c['sections']);
                }
                echo json_encode($classJson);
            ?>;

            const sections = classData[classId] || [];
            sections.forEach(s => {
                const opt = document.createElement('option');
                opt.value = s.id;
                opt.textContent = s.name;
                secSel.appendChild(opt);
            });

            loadFingerprintManagementList();
        }

        function filterFingerprints() {
            let filter = document.getElementById('fingerprintFilter') ? document.getElementById('fingerprintFilter').value.toUpperCase() : '';
            let list = document.getElementById('fingerprint-manage-list');
            if(!list) return;
            let items = list.getElementsByClassName('fp-item-row');
            
            for (let i = 0; i < items.length; i++) {
                let nameStr = items[i].querySelector('.fp-name').textContent || items[i].querySelector('.fp-name').innerText;
                let uidStr = items[i].querySelector('.fp-uid').textContent || items[i].querySelector('.fp-uid').innerText;
                
                if (nameStr.toUpperCase().indexOf(filter) > -1 || uidStr.toUpperCase().indexOf(filter) > -1) {
                    items[i].style.display = "flex";
                } else {
                    items[i].style.display = "none";
                }
            }
        }

        async function loadFingerprintManagementList() {
            const list = document.getElementById('fingerprint-manage-list');
            list.innerHTML = '<div style="text-align:center; padding:50px; opacity:0.5;"><i class="fas fa-circle-notch fa-spin"></i> SYNCING VAULT...</div>';
            
            try {
                let url = `attendance_api.php?action=get_fingerprint_templates&fp_role=${_fpCurrentTab}`;

                if (_fpCurrentTab === 'staff') {
                    const staffType = document.getElementById('fp-staff-type')?.value || '';
                    if (staffType) url += `&fp_staff_type=${encodeURIComponent(staffType)}`;
                } else {
                    const classId = document.getElementById('fp-student-class')?.value || '';
                    const sectionId = document.getElementById('fp-student-section')?.value || '';
                    if (classId)   url += `&fp_class_id=${classId}`;
                    if (sectionId) url += `&fp_section_id=${sectionId}`;
                }

                const resp = await fetch(url);
                const data = await resp.json();
                
                if (Array.isArray(data)) {
                    let html = '';
                    data.forEach(tpl => {
                        const isStudent = (tpl.role === 'student' || tpl.role === 'Student');
                        const roleLabel = isStudent
                            ? `<span class="role-badge student"><i class="fas fa-graduation-cap"></i> STUDENT</span>`
                            : `<span class="role-badge staff"><i class="fas fa-user-tie"></i> STAFF</span>`;
                        
                        html += `
                            <div class="fp-item-row">
                                <div class="fp-user-info">
                                    <div class="fp-name-wrapper">
                                        <div class="fp-name">${tpl.name.toUpperCase()}</div>
                                        ${roleLabel}
                                    </div>
                                    <div class="fp-meta">
                                        <span class="fp-meta-item"><i class="fas fa-id-badge"></i> UID: ${tpl.user_id}</span>
                                        <span class="fp-meta-item"><i class="fas fa-fingerprint"></i> ${tpl.finger_position}</span>
                                    </div>
                                </div>
                                <button class="fp-delete-btn" onclick="deleteFingerprint(${tpl.user_id}, '${tpl.finger_position}', '${tpl.name.replace(/'/g, "\\'")}')" title="Revoke Access">
                                    <i class="fas fa-trash-alt"></i> <span>DELETE</span>
                                </button>
                            </div>
                        `;
                    });
                    list.innerHTML = html || `
                        <div style="text-align:center; padding:60px 20px; opacity:0.3;">
                            <i class="fas fa-shield-alt" style="font-size:40px; margin-bottom:15px; display:block;"></i>
                            <div style="font-size:14px; font-weight:900; letter-spacing:1px;">SECURE VAULT EMPTY</div>
                            <div style="font-size:10px; margin-top:5px; font-weight:600;">No biometric templates found for this criteria.</div>
                        </div>
                    `;
                }
            } catch (e) {
                console.error("Management List Error:", e);
                list.innerHTML = `<div style="text-align:center; padding:50px; color:#e74c3c;"><i class="fas fa-exclamation-triangle"></i> SECURE VAULT UNREACHABLE</div>`;
            }
        }

        async function deleteFingerprint(userId, pos, name) {
            console.log("Delete Request:", {userId, pos, name});
            const result = await Swal.fire({
                title: 'Confirm Removal?',
                text: `Removing ${name.toUpperCase()}'s fingerprint will revoke biometric access instantly.`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#e74c3c',
                cancelButtonColor: '#34495e',
                confirmButtonText: 'YES, REMOVE',
                background: '#1a1d21',
                color: '#fff'
            });

            if (result.isConfirmed) {
                try {
                    const encodedPos = encodeURIComponent(pos);
                    const resp = await fetch(`attendance_api.php?action=delete_fingerprint&user_id=${userId}&finger_position=${encodedPos}`);
                    const data = await resp.json();
                    
                    if (data.success) {
                        Swal.fire({
                            title: 'REVOKED',
                            text: 'Template successfully removed from the secure vault.',
                            icon: 'success',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                        loadFingerprintManagementList();
                        loadFingerprintTemplates(); // Refresh the main count badge
                    } else {
                        Swal.fire({
                            title: 'ERROR',
                            text: data.error || 'Operation failed',
                            icon: 'error',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                    }
                } catch (e) {
                    console.error("Delete Error:", e);
                    Swal.fire({
                        title: 'ERROR',
                        text: 'Secure Connection Failure',
                        icon: 'error',
                        background: '#1a1d21',
                        color: '#fff'
                    });
                }
            }
        }

        async function deleteAllFingerprints() {
            const result = await Swal.fire({
                title: 'WIPE ENTIRE VAULT?',
                text: `CRITICAL: This will permanently delete ALL biometric templates for this institution. This action cannot be undone!`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#e74c3c',
                cancelButtonColor: '#34495e',
                confirmButtonText: 'YES, PURGE EVERYTHING',
                background: '#1a1d21',
                color: '#fff'
            });

            if (result.isConfirmed) {
                try {
                    const resp = await fetch('attendance_api.php?action=delete_all_fingerprints');
                    const data = await resp.json();
                    
                    if (data.success) {
                        Swal.fire({
                            title: 'VAULT PURGED',
                            text: 'All biometric templates have been securely removed.',
                            icon: 'success',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                        loadFingerprintManagementList();
                        if (typeof loadFingerprintTemplates === 'function') loadFingerprintTemplates();
                    } else {
                        Swal.fire({
                            title: 'PURGE FAILED',
                            text: data.error || 'Operation failed',
                            icon: 'error',
                            background: '#1a1d21',
                            color: '#fff'
                        });
                    }
                } catch (e) {
                    console.error("Purge Error:", e);
                    Swal.fire({ title: 'PROCESS ERROR', text: 'Secure terminal connection interrupted.', icon: 'error', background: '#1a1d21', color: '#fff' });
                }
            }
        }

        async function updateUserBiometricInfo() {
            const userEl = document.getElementById('enroll_user_sel');
            if (!userEl) return;
            const userId = userEl.value;
            const container = document.getElementById('user_biometric_info');
            const list = document.getElementById('biometric_info_list');
            
            if (!userId) {
                container.style.display = 'none';
                return;
            }

            try {
                const resp = await fetch(`attendance_api.php?action=get_user_biometric_info&user_id=${userId}`);
                const data = await resp.json();
                
                if (data.success) {
                    list.innerHTML = '';
                    let count = 0;

                    // Face Info
                    if (data.has_face) {
                        count++;
                        list.innerHTML += `
                            <div style="display:flex; justify-content:space-between; align-items:center; background:rgba(255,255,255,0.03); padding:8px 12px; border-radius:10px;">
                                <div style="display:flex; align-items:center; gap:10px;">
                                    <i class="fas fa-smile" style="color:#3498db; width:15px;"></i>
                                    <span style="font-size:11px; font-weight:700;">FACE REGISTERED</span>
                                </div>
                                <i class="fas fa-trash-alt" style="color:#e74c3c; cursor:pointer; font-size:12px; opacity:0.6;" onclick="deleteBiometricData('face')" title="Remove Face Data"></i>
                            </div>
                        `;
                    }

                    // Fingerprint Info
                    data.fingerprints.forEach(f => {
                        count++;
                        list.innerHTML += `
                            <div style="display:flex; justify-content:space-between; align-items:center; background:rgba(255,255,255,0.03); padding:8px 12px; border-radius:10px;">
                                <div style="display:flex; align-items:center; gap:10px;">
                                    <i class="fas fa-fingerprint" style="color:#2ecc71; width:15px;"></i>
                                    <span style="font-size:11px; font-weight:700;">${f.finger_position.toUpperCase()}</span>
                                    <span style="font-size:9px; opacity:0.5;">Q: ${f.quality_score}%</span>
                                </div>
                                <i class="fas fa-trash-alt" style="color:#e74c3c; cursor:pointer; font-size:12px; opacity:0.6;" onclick="deleteBiometricData('finger', '${f.finger_position}')" title="Remove Fingerprint"></i>
                            </div>
                        `;
                    });

                    if (count === 0) {
                        list.innerHTML = `<div style="font-size:11px; opacity:0.5; padding:5px; text-align:center;">NO BIOMETRICS REGISTERED</div>`;
                    }
                    
                    container.style.display = 'block';
                }
            } catch (e) { console.error("Failed to load biometric info:", e); }
        }

        async function deleteBiometricData(type, position = '') {
            const userId = document.getElementById('enroll_user_sel').value;
            const label = type === 'face' ? 'Face Recognition Data' : (position || 'Fingerprint');
            
            if (!confirm(`Are you sure you want to PERMANENTLY remove ${label} for this user?`)) return;

            try {
                const resp = await fetch('attendance_api.php?action=delete_biometric', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ user_id: userId, type, position })
                });
                const res = await resp.json();
                if (res.success) {
                    await updateUserBiometricInfo();
                    await loadFingerprintTemplates(); // Global refresh
                    alert("Biometric Data Removed Successfully");
                } else {
                    alert("Error: " + res.error);
                }
            } catch (e) { alert("Link Failure: " + e.message); }
        }

        let lastFingerprintCapture = null;

        function resetFingerprintCapture() {
            lastFingerprintCapture = null;
            document.getElementById('fingerprintPreviewImg').src = "";
            document.getElementById('fingerprintImgPreview').style.display = 'none';
            document.getElementById('fingerprintIconPlaceholder').style.display = 'block';
            document.getElementById('fingerprint-capture-status').textContent = "WAITING FOR FINGER ON SCANNER...";
            document.getElementById('fingerCaptureBtn').style.display = 'block';
            document.getElementById('fingerCaptureBtn').disabled = false;
            document.getElementById('fingerprint-enroll-controls').style.display = 'none';
            
            // Re-enable save button UI
            const saveBtn = document.querySelector('#fingerprint-enroll-controls button:first-child');
            if(saveBtn) {
                saveBtn.disabled = false;
                saveBtn.innerHTML = '<i class="fas fa-check-circle" style="margin-right: 8px;"></i> SAVE ENROLLMENT';
            }
        }

        async function saveFingerprintEnrollment() {
            if (!lastFingerprintCapture) {
                alert("No capture data found. Please recapture.");
                return;
            }
            
            const userId = document.getElementById('enroll_user_sel').value;
            const finger = document.getElementById('enroll_finger_sel').value;
            const statusText = document.getElementById('fingerprint-capture-status');
            const saveBtn = document.querySelector('#fingerprint-enroll-controls button:first-child');
            
            if (!userId) { alert("User session lost. Please re-select user."); return; }

            saveBtn.disabled = true;
            saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> SAVING TO DATABASE...';

            try {
                const resp = await fetch('attendance_api.php?action=save_fingerprint_enrollment', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        user_id: userId,
                        template: lastFingerprintCapture.template,
                        position: finger,
                        quality: lastFingerprintCapture.quality
                    })
                });
                const res = await resp.json();
                
                if (res.success) {
                    statusText.innerHTML = `<span style="color:#2ecc71; font-weight:900;"><i class="fas fa-check-circle"></i> ENROLLED SUCCESSFULLY!</span>`;
                    await refreshEnrollmentSlots();
                    setTimeout(() => {
                        resetFingerprintCapture();
                        goBackToEnrollStep1();
                    }, 1200);
                } else {
                    if (res.error && res.error.includes("already registered")) {
                        statusText.innerHTML = `<span style="color: #f1c40f; font-weight: 800;"><i class="fas fa-exclamation-circle"></i> ${res.error.toUpperCase()}</span>`;
                        alert(res.error);
                    } else {
                        alert("Enrollment Failed: " + res.error);
                    }
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = '<i class="fas fa-check-circle" style="margin-right: 8px;"></i> SAVE ENROLLMENT';
                }
            } catch (e) {
                console.error(e);
                alert("Network Error: Could not save enrollment.");
                saveBtn.disabled = false;
                saveBtn.innerHTML = '<i class="fas fa-check-circle" style="margin-right: 8px;"></i> SAVE ENROLLMENT';
            }
        }

        async function captureFingerprintEnroll() {
            const userId = document.getElementById('enroll_user_sel').value;
            const statusText = document.getElementById('fingerprint-capture-status');
            const previewImg = document.getElementById('fingerprintPreviewImg');
            const previewContainer = document.getElementById('fingerprintImgPreview');
            const placeholder = document.getElementById('fingerprintIconPlaceholder');
            const btn = document.getElementById('fingerCaptureBtn');
            const controls = document.getElementById('fingerprint-enroll-controls');

            statusText.textContent = "CAPTURE IN PROGRESS... PLACE FINGER";
            btn.disabled = true;

            const wasLoopRunning = (typeof isFingerprintLoopRunning !== 'undefined') && isFingerprintLoopRunning;
            if (wasLoopRunning) {
                isFingerprintLoopRunning = false;
                await new Promise(r => setTimeout(r, 300));
            }

            try {
                const result = await window.fingerprintSDK.captureFingerprint();
                console.log("[DEBUG] Capture Result:", result);
                
                if (result.success) {
                    lastFingerprintCapture = result;
                    previewImg.src = result.image;
                    previewContainer.style.display = 'block';
                    placeholder.style.display = 'none';
                    statusText.innerHTML = `<span style="color: #2ecc71; font-weight: 900;">CAPTURE SUCCESSFUL (SCORE: ${result.quality}%)</span><br><small style="color:var(--text-dim)">Review image below then Save or Retry</small>`;
                    alert("SUCCESS: Please review the fingerprint image then click SAVE or RETRY.");

                    // Success: Show controls
                    console.log("[DEBUG] Showing save/retry controls");
                    btn.style.setProperty('display', 'none', 'important');
                    if (controls) {
                        controls.style.setProperty('display', 'flex', 'important');
                    } else {
                        console.error("[DEBUG] Controls element NOT FOUND");
                        alert("UI Error: Controls container missing.");
                    }

                } else {
                    alert("Capture Failed: " + (result.message || "Unknown error"));
                    btn.disabled = false;
                }
            } catch (e) {
                console.error("[DEBUG] Capture Exception:", e);
                alert("Scanner error: " + e.message);
                btn.disabled = false;
            } finally {
                if (!lastFingerprintCapture) btn.disabled = false;
                if (wasLoopRunning && typeof startFingerprintAutoLoop === 'function') {
                    isFingerprintLoopRunning = false;
                    startFingerprintAutoLoop();
                }
            }
        }

        // Automatic Verification Loop
        async function startFingerprintAutoLoop() {
            if (isFingerprintLoopRunning) return;
            
            // Respect Settings
            if (!forceAttendanceActive && !smartSettings.fingerActive) {
                console.log("[BIOMETRIC] Fingerprint Engine Disabled in Settings");
                const mStatus = document.getElementById('fingerprintMainStatus');
                if(mStatus) {
                    mStatus.textContent = "ENGINE DISABLED";
                    mStatus.style.color = "#bdc3c7";
                }
                document.getElementById('fingerprintSubStatus').innerText = "Biometric Verification is disabled. Use Manual or Force Active.";
                return;
            }

            isFingerprintLoopRunning = true;
            console.log("[BIOMETRIC] Automatic Scanner Loop: ENGAGED");
            
            // NEW: Background Scanner Status Monitor
            const monitorInterval = setInterval(async () => {
                if (!isFingerprintLoopRunning) {
                    clearInterval(monitorInterval);
                    return;
                }
                
                const mainStatus = document.getElementById('fingerprintMainStatus');
                const subStatus = document.getElementById('fingerprintSubStatus');
                if (!mainStatus || !subStatus || isVerifyingStep) return;

                // Respect Schedule - If Out of Schedule, don't show device status errors
                if (!isAttendanceTimeValid()) {
                    if (mainStatus.textContent !== "OUT OF SCHEDULE" && mainStatus.textContent !== "SYSTEM OFF") {
                        mainStatus.textContent = "OUT OF SCHEDULE";
                        mainStatus.style.color = "#bdc3c7";
                        subStatus.innerHTML = `System Time: <b>${getCurrentPKT()} (PKT)</b>. Schedule not matched.<br>Click 'ACTIVATE NOW' to override.`;
                    }
                    return;
                }

                const status = await window.fingerprintSDK.getDeviceStatus();
                if (status.connected) {
                    // Only update if not already ready and not currently verifying
                    if (mainStatus.textContent !== "READY TO SCAN" && !isVerifyingStep) {
                         mainStatus.textContent = "READY TO SCAN";
                         mainStatus.style.color = "#fff";
                         subStatus.textContent = "System active. Place your finger on the sensor.";
                    }
                } else {
                    // Only update if message changed
                    if (mainStatus.textContent !== status.message.toUpperCase()) {
                        mainStatus.textContent = status.message.toUpperCase();
                        mainStatus.style.color = "#e74c3c";
                        let subHtml = `<i class="fas fa-plug"></i> ${status.message}.`;
                        if (status.message.includes("Service Not Running") || status.message.includes("Unreachable")) {
                             const fixUrl = window.fingerprintSDK.getServiceBaseUrl();
                             subHtml += ` <a href="${fixUrl}" target="_blank" style="color: #3498db; text-decoration: underline; font-weight: 800;">CLICK HERE TO FIX SSL</a>`;
                        }
                        subStatus.innerHTML = subHtml;
                    }
                }
            }, 3000);
            
            // Automatic loop for verification

            while (isFingerprintLoopRunning) {
                // Verification stop condition: global flag (Respect settings/power)
                if (!isFingerprintLoopRunning) break;

                try {
                    if (isAttendanceTimeValid()) {
                        await runFingerprintVerificationStep();
                    } else {
                        const mStatus = document.getElementById('fingerprintMainStatus');
                        const sStatus = document.getElementById('fingerprintSubStatus');
                        if (mStatus && mStatus.textContent !== "OUT OF SCHEDULE" && mStatus.textContent !== "SYSTEM OFF") {
                            mStatus.textContent = "OUT OF SCHEDULE";
                            mStatus.style.color = "#bdc3c7";
                        }
                        if (sStatus && !sStatus.innerHTML.includes("Schedule not matched") && (mStatus && mStatus.textContent !== "SYSTEM OFF")) {
                            sStatus.innerHTML = `System Time: <b>${getCurrentPKT()} (PKT)</b>. Schedule not matched.<br>Click 'ACTIVATE NOW' to override.`;
                        }
                        await new Promise(r => setTimeout(r, 5000));
                    }
                } catch (e) {
                    console.error("Biometric Verification Cluster Error:", e);
                    await new Promise(r => setTimeout(r, 2000));
                }
                
                // Pulse interval to keep system responsive and prevent hardware jams
                // Reduced from 1500ms to 250ms for faster scanning
                await new Promise(r => setTimeout(r, 250));
            }
            
            console.log("[BIOMETRIC] Automatic Scanner Loop: DISENGAGED");
        }

        function getCurrentPKT() {
            try {
                // Robust way to get PKT HH:MM
                const now = new Date();
                const formatter = new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Karachi', hour: '2-digit', minute: '2-digit', hour12: false });
                const parts = formatter.formatToParts(now);
                const hour = parts.find(p => p.type === 'hour').value;
                const minute = parts.find(p => p.type === 'minute').value;
                return `${hour}:${minute}`;
            } catch (e) {
                console.error("Timezone Error:", e);
                // Fallback
                const n = new Date();
                return n.getHours().toString().padStart(2, '0') + ':' + n.getMinutes().toString().padStart(2, '0');
            }
        }

        let scanMode = 'auto'; // auto, check_in, check_out
        // DeviceManager: Auto-restore system state from session if previously ON
        let systemMasterState = (typeof DeviceManager !== 'undefined' && DeviceManager.wasSystemOn()) ? true : false;

        function manualActivateVision() {
            forceAttendanceActive = true; 
            systemMasterState = true;
            initFingerprintSystem(); // Initialize hardware
            toggleSystemMaster(null); 
            
            Swal.fire({
                title: 'SYSTEM ACTIVATED',
                text: 'Smart Vision Engine has been manually initialized.',
                icon: 'success',
                timer: 2000,
                showConfirmButton: false,
                background: 'rgba(15, 23, 42, 0.95)',
                color: '#fff'
            });
        }

        function toggleSystemMaster(source) {
            // IF called from UI event, update state. If null, just refresh UI.
            if (source) {
                systemMasterState = source.checked;
            }

            // Sync All Switches
            const switches = [document.getElementById('master_system_switch'), document.getElementById('master_system_switch_face')];
            switches.forEach(sw => { if(sw) sw.checked = systemMasterState; });

            const statusTxts = [document.getElementById('system_power_status'), document.getElementById('system_power_status_face')];
            
            if(!systemMasterState) {
                // STOP EVERYTHING
                if (typeof DeviceManager !== 'undefined') DeviceManager.stop();
                statusTxts.forEach(t => { if(t) { t.textContent = "Disabled"; t.style.color = "#e74c3c"; } });
                isFaceSystemStarted = false;
                isFingerprintLoopRunning = false;
                
                // Visual Feedback
                if(document.getElementById('faceLoaderMsg')) document.getElementById('faceLoaderMsg').innerHTML = '<span style="color:#000000 !important; font-weight:900;">SYSTEM OFF</span>';
                if(document.getElementById('fingerprintMainStatus')) {
                    document.getElementById('fingerprintMainStatus').textContent = "SYSTEM OFF";
                    document.getElementById('fingerprintMainStatus').style.color = "#e74c3c";
                }

                // Stop Camera Feed
                const videoEl = document.getElementById('faceVideoDisplay');
                if(videoEl) {
                    videoEl.src = ""; // Stop network stream
                    videoEl.style.display = 'none';
                }
                const placeholder = document.getElementById('faceVideoPlaceholder');
                if(placeholder) {
                    placeholder.style.display = 'flex';
                }
            } else {
                // RESTART
                statusTxts.forEach(t => { if(t) { t.textContent = "Active"; t.style.color = "var(--success)"; } });
                if (typeof DeviceManager !== 'undefined') DeviceManager.start();
                initFaceRecognition();
                startFingerprintAutoLoop();
            }
        }

        function setScanMode(mode) {
            scanMode = mode;
            if (typeof displayedUiFacesSession !== 'undefined') displayedUiFacesSession.clear();
            if (typeof markedUsersSession !== 'undefined') markedUsersSession.clear();

            // Update UI Pills (Fingerprint)
            ['mode_auto', 'mode_in', 'mode_out'].forEach(id => {
                const el = document.getElementById(id);
                if(el) {
                    el.style.background = 'transparent';
                    el.style.color = 'var(--text-dim)';
                }
            });
            const activeEl = document.getElementById('mode_' + (mode === 'check_in' ? 'in' : (mode === 'check_out' ? 'out' : 'auto')));
            if(activeEl) {
                activeEl.style.background = 'var(--primary)';
                activeEl.style.color = '#fff';
            }

            // Update UI Pills (Face)
            ['mode_auto_face', 'mode_in_face', 'mode_out_face'].forEach(id => {
                const el = document.getElementById(id);
                if(el) {
                    el.style.background = 'transparent';
                    el.style.color = 'var(--text-dim)';
                }
            });
            const activeFaceEl = document.getElementById('mode_' + (mode === 'check_in' ? 'in_face' : (mode === 'check_out' ? 'out_face' : 'auto_face')));
            if(activeFaceEl) {
                activeFaceEl.style.background = 'var(--primary)';
                activeFaceEl.style.color = '#fff';
            }

            // Show/Hide ENROLL FACE button (only in AUTO mode)
            const enrollWrapper = document.getElementById('faceEnrollBtnWrapper');
            if (enrollWrapper) {
                enrollWrapper.style.display = (mode === 'auto') ? '' : 'none';
            }

            // Auto-activate system when IN or OUT is selected
            if (mode === 'check_in' || mode === 'check_out') {
                if (!systemMasterState) {
                    systemMasterState = true;
                    forceAttendanceActive = true;
                    initFingerprintSystem();
                    toggleSystemMaster(null);
                }
                const modeLabel = (mode === 'check_in') ? 'Check-IN' : 'Check-OUT';
                const modeIcon = (mode === 'check_in') ? '🟢' : '🔴';
                const modeBg   = (mode === 'check_in') ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)';
                const modeBorder= (mode === 'check_in') ? '#10b981' : '#ef4444';
                Swal.fire({
                    position: 'center',
                    icon: 'success',
                    title: modeIcon + ' ' + modeLabel + ' Mode Activated',
                    text: 'System is now scanning for ' + modeLabel.toLowerCase() + ' attendance.',
                    showConfirmButton: false,
                    timer: 2500,
                    timerProgressBar: true,
                    background: 'rgba(15, 23, 42, 0.97)',
                    color: '#fff',
                    customClass: { popup: 'swal-scan-mode-popup' },
                    didOpen: (popup) => {
                        popup.style.border = '1px solid ' + modeBorder;
                        popup.style.boxShadow = '0 0 40px ' + modeBorder + '55';
                        popup.style.borderRadius = '20px';
                    }
                });
            }

            console.log("Scan Mode set to:", scanMode);
        }

        function isAttendanceTimeValid() {
            // 1. Master Switch Overrides All
            if (!systemMasterState) return false;

            // 2. Manual Override Overrides Schedule
            if (scanMode !== 'auto') return true; 
            
            // 3. Force Active (Legacy Button) Overrides Schedule
            if (forceAttendanceActive) return true;
            
            const current = getCurrentPKT();
            
            // Allow if within Check-In window
            if (current >= smartSettings.inStart && current <= smartSettings.inEnd) return true;
            
            // Allow if Check-Out is active and within window
            if (smartSettings.attMode === 'check_in_out') {
                if (current >= smartSettings.outStart && current <= smartSettings.outEnd) return true;
            }
            
            if (smartSettings.attMode === 'check_in_out') {
                if (current >= smartSettings.outStart && current <= smartSettings.outEnd) return true;
            }
            
            return false;
        }

        function determineAttendanceAction() {
            if (scanMode === 'check_in') return 'check_in';
            if (scanMode === 'check_out') return 'check_out';
            
            // Auto Mode: Resolve based on time
            const current = getCurrentPKT();
            
            if (smartSettings.attMode === 'check_in_out') {
                if (current >= smartSettings.outStart && current <= smartSettings.outEnd) return 'check_out';
            }
            
            // Default to Check-In (covers check_in_only and check_in window of shift mode)
            return 'check_in';
        }

        async function toggleForceActive() {
            forceAttendanceActive = !forceAttendanceActive;
            
            // 1. Update ALL Buttons with this class
            const btns = document.querySelectorAll('.force-active-btn');
            btns.forEach(btn => {
                if(forceAttendanceActive) {
                    btn.innerHTML = '<i class="fas fa-stop-circle" style="margin-right:8px;"></i> DEACTIVATE NOW';
                    btn.style.background = '#e74c3c';
                    btn.style.boxShadow = '0 0 15px rgba(231, 76, 60, 0.4)';
                } else {
                    btn.innerHTML = '<i class="fas fa-power-off" style="margin-right:8px;"></i> ACTIVATE NOW';
                    btn.style.background = '#34495e';
                    btn.style.boxShadow = 'none';
                }
            });
            
            // 2. Update Face Specific UI
            const faceStatus = document.getElementById('face-live-status');
            const faceToggle = document.getElementById('setting_face_active');
            const videoDisplay = document.getElementById('faceVideoDisplay');
            const placeholder = document.getElementById('faceVideoPlaceholder');

            if (forceAttendanceActive) {
                if(faceStatus) {
                    faceStatus.innerHTML = 'ONLINE';
                    faceStatus.style.background = '#2ecc71';
                }
                if(faceToggle) faceToggle.checked = true;
                
                const facePanel = document.getElementById('face-recognition-panel');
                if (facePanel && facePanel.classList.contains('active')) {
                   initFaceRecognition(); // Re-check settings (now forced) and start
                }
            } else {
                if(faceStatus) {
                    faceStatus.innerHTML = 'OFFLINE';
                    faceStatus.style.background = '#dc3545';
                }
                if(faceToggle) faceToggle.checked = false;
                isFaceSystemStarted = false;
                if(videoDisplay) videoDisplay.style.display = 'none';
                if(placeholder) placeholder.style.display = 'flex';
            }

            // 3. Update Fingerprint Specific UI
            const fingerStatus = document.getElementById('fingerprintMainStatus');
            const fingerSubStatus = document.getElementById('fingerprintSubStatus');
            
            if (forceAttendanceActive) {
                if(fingerStatus) {
                    fingerStatus.textContent = "READY TO SCAN";
                    fingerStatus.style.color = "#fff";
                }
                if(fingerSubStatus) fingerSubStatus.textContent = "Manual activation engaged. Place your registered finger on the sensor.";
                
                // Stop existing loop and restart to ensure fresh start (regardless of tab visibility)
                isFingerprintLoopRunning = false;
                console.log('[FINGERPRINT] Restarting verification loop after activation...');
                setTimeout(() => {
                    initFingerprintSystem();
                    startFingerprintAutoLoop();
                }, 500);
            } else {
                if(fingerStatus) fingerStatus.textContent = "Ready to Scan";
                // Stop the loop when deactivating
                isFingerprintLoopRunning = false;
            }

            // 4. Save to database
            try {
                const stateVal = forceAttendanceActive ? 1 : 0;
                await fetch('attendance_api.php?action=save_smart_settings', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ face_active: stateVal, finger_active: stateVal })
                });
            } catch(e) { console.error("Failed to save state:", e); }
        }

        let isVerifyingStep = false;
        async function runFingerprintVerificationStep() {
            if (isVerifyingStep) return;
            isVerifyingStep = true;
            
            console.log('[FINGERPRINT] Starting verification step. Templates loaded:', fingerprintTemplates.length);
            
            const mainStatus = document.getElementById('fingerprintMainStatus');
            const subStatus = document.getElementById('fingerprintSubStatus');
            const scanLine = document.getElementById('scannerScanLine');

            if (fingerprintTemplates.length === 0) {
                console.log('[FINGERPRINT] No templates loaded, fetching from database...');
                await loadFingerprintTemplates();
                console.log('[FINGERPRINT] Templates loaded:', fingerprintTemplates.length);
            }

            mainStatus.textContent = "WAITING FOR FINGER";
            mainStatus.style.color = "var(--primary)";
            scanLine.style.display = 'block';

            try {
                // Check SDK availability before attempting capture
                if (typeof window.fingerprintSDK === 'undefined') {
                    throw new Error('SecuGen SDK not loaded. Service may not be running.');
                }
                
                // Trigger scanner hardware
                console.log('[FINGERPRINT] Attempting to capture fingerprint from scanner...');
                const captureResult = await window.fingerprintSDK.captureFingerprint();
                console.log('[FINGERPRINT] Capture result:', captureResult ? 'Success' : 'Failed', captureResult);
                
                if (captureResult && captureResult.aborted) {
                    // Capture was deliberately aborted — just return cleanly.
                    // The while() loop in startFingerprintAutoLoop will restart us naturally.
                    console.log('[FINGERPRINT] Capture aborted, returning to main loop.');
                    // fall through to finally (isVerifyingStep = false, scanLine hidden)
                } else if (captureResult && captureResult.success) {
                    mainStatus.textContent = "MATCHING BIOMETRICS...";
                    mainStatus.style.color = "#f1c40f";
                    
                    // Group scores by user_id to pick the BEST matching USER
                    // (a user may have multiple fingers registered — we want the best score PER USER)
                    const userScores = {}; // { user_id: { score, template } }
                    let bestScore = 0;

                    for (let template of fingerprintTemplates) {
                        try {
                            const t1 = captureResult.template;
                            const t2 = template.fingerprint_template;
                            
                            console.log(`[BIOMETRIC] Comparing to ${template.name} (${template.finger_position || 'Unknown Pos'})`);

                            const matchResult = await window.fingerprintSDK.matchTemplates(t1, t2);
                            const score = parseInt(matchResult.score || 0);

                            if (score > bestScore) bestScore = score;

                            // Keep best score per user (not per finger template)
                            if (!userScores[template.user_id] || score > userScores[template.user_id].score) {
                                userScores[template.user_id] = { score, template };
                            }
                        } catch(matchErr) {
                            console.error("[FINGERPRINT] Match error for " + (template ? template.name : 'Unknown'), matchErr);
                        }
                    }

                    // Now find the user with the highest score that meets threshold (>=50)
                    let matchedUser = null;
                    for (const uid in userScores) {
                        const entry = userScores[uid];
                        if (entry.score >= 50 && (!matchedUser || entry.score > matchedUser.lastScore)) {
                            matchedUser = { ...entry.template, lastScore: entry.score };
                        }
                    }

                    console.log(`[BIOMETRIC] Verification Complete. Best Score: ${bestScore}`);

                    if (matchedUser) {
                        // Mark Attendance via API (with offline fallback)
                        const attPayload = { 
                            user_id: matchedUser.user_id, 
                            type: 'fingerprint', 
                            score: matchedUser.lastScore,
                            mode: determineAttendanceAction()
                        };
                        
                        try {
                            const attResp = await fetch('attendance_api.php?action=mark_smart_attendance', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify(attPayload)
                            });
                            const attRes = await attResp.json();

                            if (attRes.success) {
                                showAttendanceToast(attRes.user, attRes.mode, attRes.time);
                                await showMatchSuccess(matchedUser, attRes.already_marked);
                            }
                        } catch(netErr) {
                            // OFFLINE FALLBACK: Queue attendance for later sync
                            console.warn('[DEVICE-MANAGER] Network error — queuing attendance offline');
                            if (typeof DeviceManager !== 'undefined') {
                                DeviceManager.queueOffline(attPayload);
                            }
                            await showMatchSuccess(matchedUser, false);
                        }
                    } else {
                        mainStatus.textContent = "ACCESS DENIED";
                        mainStatus.style.color = "#e74c3c";
                        
                        if (fingerprintTemplates.length === 0) {
                            subStatus.innerHTML = "<b>NO BIOMETRIC DATA FOUND</b><br>Try clicking the 'SYNC' badge to reload data.";
                        } else {
                            subStatus.innerHTML = `No matching fingerprint found.<br><small style="opacity:0.6">Best match score was: ${bestScore} / 100</small>`;
                        }
                        await new Promise(r => setTimeout(r, 4000));
                        subStatus.textContent = "Please place your registered finger on the scanner sensor.";
                    }
                } else {
                    // Capture failed or returned no data
                    console.warn('[FINGERPRINT] Capture returned no data or failed');
                    mainStatus.textContent = "CAPTURE FAILED";
                    mainStatus.style.color = "#e74c3c";
                    subStatus.innerHTML = "Scanner not responding.<br>Ensure finger is properly placed or restart the SecuGen service.";
                    await new Promise(r => setTimeout(r, 3000));
                }
            } catch (e) {
                console.error("Biometric Capture Exception:", e);
                mainStatus.textContent = "SCANNER ERROR";
                mainStatus.style.color = "#e74c3c";
                if(e.message && e.message.includes('SDK not loaded')) {
                    subStatus.innerHTML = `<b>SecuGen Service Not Running</b><br>Please restart 'SecuGen Biometric Service' from Windows Services.`;
                } else {
                    subStatus.innerHTML = `Hardware Error: ${e.message || 'Unknown error'}<br>Try restarting the scanner or your computer.`;
                }
                await new Promise(r => setTimeout(r, 4000));
            } finally {
                isVerifyingStep = false;
                scanLine.style.display = 'none';
            }
        }

        function showAttendanceToast(user, mode, time) {
            // Remove existing toast if any
            const oldToast = document.querySelector('.attendance-toast');
            if (oldToast) oldToast.remove();

            const toast = document.createElement('div');
            toast.className = 'attendance-toast';
            
            const statusColor = mode === 'check_in' ? 'rgba(46, 204, 113, 0.15)' : 'rgba(52, 152, 219, 0.15)';
            const statusTextColor = mode === 'check_in' ? '#2ecc71' : '#3498db';
            const statusLabel = mode === 'check_in' ? 'CHECKED IN' : 'CHECKED OUT';
            const photoUrl = user.photo && user.photo !== 'default.png' ? '../../assets/uploads/' + user.photo : '../../assets/images/default-user.png';

            toast.innerHTML = `
                <img src="${photoUrl}" class="toast-img" onerror="this.src='../../assets/images/default-user.png'">
                <div class="toast-msg">
                    <div class="toast-name">${user.name}</div>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <span class="toast-status" style="background:${statusColor}; color:${statusTextColor}">${statusLabel}</span>
                        <span style="font-size:11px; opacity:0.6; font-weight:700;">${time}</span>
                    </div>
                </div>
                <div style="font-size:24px; color:${statusTextColor};"><i class="fas fa-check-circle"></i></div>
            `;

            document.body.appendChild(toast);

            // Auto-hide after 5 seconds
            setTimeout(() => {
                toast.classList.add('hide');
                setTimeout(() => toast.remove(), 500);
            }, 5000);
        }

        // Original button-triggered function repurposed as manual override
        async function startFingerprintVerification() {
            if (isFingerprintLoopRunning) {
                alert("Automatic scanning is already engaged. Simply place your finger on the sensor.");
                return;
            }
            startFingerprintAutoLoop();
        }

        async function showMatchSuccess(user, alreadyMarked = false) {
            const mainStatus = document.getElementById('fingerprintMainStatus');
            const successOverlay = document.getElementById('scannerSuccessOverlay');
            const resultPanel = document.getElementById('verifiedUserResult');
            const scannerIcon = document.querySelector('#scannerStatusIcon i');
            const scannerGlow = document.querySelector('.scanner-glow');
            
            mainStatus.textContent = alreadyMarked ? "ALREADY MARKED" : "ACCESS GRANTED";
            mainStatus.style.color = alreadyMarked ? "#f39c12" : "#2ecc71";
            if(successOverlay) {
                successOverlay.style.display = 'flex';
                successOverlay.style.animation = 'fadeIn 0.3s ease';
            }
            
            if(scannerGlow) {
                scannerGlow.style.background = alreadyMarked ? 'radial-gradient(circle, rgba(243, 156, 18, 0.4) 0%, transparent 70%)' : 'radial-gradient(circle, rgba(46, 204, 113, 0.4) 0%, transparent 70%)';
            }

            if(scannerIcon) {
                scannerIcon.style.color = alreadyMarked ? '#f39c12' : '#2ecc71';
                scannerIcon.style.filter = alreadyMarked ? 'drop-shadow(0 0 25px #f39c12)' : 'drop-shadow(0 0 25px #2ecc71)';
                scannerIcon.classList.add('fa-beat');
            }
            
            document.getElementById('vUserName').textContent = user.name.toUpperCase();
            document.getElementById('vUserPic').innerHTML = `<i class="fas ${alreadyMarked ? 'fa-info-circle' : 'fa-check'}" style="font-size: 25px; color: ${alreadyMarked ? '#f39c12' : '#2ecc71'}; margin: 10px;"></i>`;
            resultPanel.style.display = 'block';

            // Voice Feedback
            if (smartSettings.sound) {
                const synth = window.speechSynthesis;
                if (synth) {
                    const voiceMsg = alreadyMarked ? (user.name + " already marked") : (user.name + " identified");
                    const utter = new SpeechSynthesisUtterance(voiceMsg);
                    utter.rate = 1.0;
                    synth.speak(utter);
                }
            }
            
            await new Promise(r => setTimeout(r, 4000));
            
            if(successOverlay) successOverlay.style.display = 'none';
            if(scannerGlow) {
                scannerGlow.style.background = 'radial-gradient(circle, rgba(155, 89, 182, 0.2) 0%, transparent 70%)';
            }
            if(scannerIcon) {
                scannerIcon.style.color = 'var(--primary)';
                scannerIcon.style.filter = 'drop-shadow(0 0 15px rgba(155, 89, 182, 0.4))';
                scannerIcon.classList.remove('fa-beat');
            }
            mainStatus.style.color = "var(--primary)";
            mainStatus.textContent = `READY FOR SCAN (${getCurrentPKT()})`;
            resultPanel.style.display = 'none';
        }

        let smartSettings = { 
            autoMark: true, 
            sound: true, 
            faceActive: true,
            fingerActive: true,
            attMode: 'check_in_only', 
            inStart: '07:00', 
            inEnd: '09:00', 
            outStart: '13:00', 
            outEnd: '15:00' 
        };

        async function loadSmartSettings() {
            try {
                const resp = await fetch('attendance_api.php?action=get_smart_settings');
                const settings = await resp.json();
                
                if (settings) {
                    document.getElementById('setting_face_active').checked = parseInt(settings.att_smart_face_active);
                    document.getElementById('setting_finger_active').checked = parseInt(settings.att_smart_finger_active);
                    
                    document.getElementById('setting_face_ip').value = settings.att_smart_face_ip || "192.168.1.108";
                    document.getElementById('setting_face_user').value = settings.att_smart_face_user || "admin";
                    document.getElementById('setting_face_pass').value = settings.att_smart_face_pass || "pakistan123";
                    
                    if(document.getElementById('setting_smart_sms')) {
                        document.getElementById('setting_smart_sms').checked = parseInt(settings.att_smart_auto_mark);
                    }
                    if(document.getElementById('setting_auto_mark')) {
                        document.getElementById('setting_auto_mark').checked = parseInt(settings.att_smart_auto_mark);
                    }
                    if(document.getElementById('setting_sound')) {
                        document.getElementById('setting_sound').checked = parseInt(settings.att_smart_sound);
                    }
                    
                    // Populate New Schedule Fields
                    if(settings.att_smart_mode) {
                        document.getElementById('setting_att_mode').value = settings.att_smart_mode;
                        smartSettings.attMode = settings.att_smart_mode;
                    }
                    if(settings.att_smart_in_start) document.getElementById('setting_in_start').value = settings.att_smart_in_start;
                    if(settings.att_smart_in_end) document.getElementById('setting_in_end').value = settings.att_smart_in_end;
                    if(settings.att_smart_out_start) document.getElementById('setting_out_start').value = settings.att_smart_out_start;
                    if(settings.att_smart_out_end) document.getElementById('setting_out_end').value = settings.att_smart_out_end;
                    
                    smartSettings.inStart = settings.att_smart_in_start || '07:00';
                    smartSettings.inEnd = settings.att_smart_in_end || '09:00';
                    smartSettings.outStart = settings.att_smart_out_start || '13:00';
                    smartSettings.outEnd = settings.att_smart_out_end || '15:00';

                    toggleCheckOutSettings(); // Sync UI
                    
                    // Display Current System Time in Settings for verification
                    const timeDisp = document.getElementById('system_time_display');
                    if(timeDisp) timeDisp.innerHTML = `SYSTEM TIME: <b>${getCurrentPKT()}</b> (PKT)`;
                    else {
                        // Inject if not exists
                        const header = document.querySelector('.operation-logic-header');
                        if(header) {
                             const span = document.createElement('span');
                             span.id = 'system_time_display';
                             span.style.float = 'right';
                             span.style.fontSize = '11px';
                             span.style.color = 'var(--primary)';
                             span.innerHTML = `SYSTEM TIME: <b>${getCurrentPKT()}</b> (PKT)`;
                             header.appendChild(span);
                        }
                    }

                    smartSettings.autoMark = parseInt(settings.att_smart_auto_mark) === 1;
                    smartSettings.sound = parseInt(settings.att_smart_sound) === 1;
                    smartSettings.faceActive = parseInt(settings.att_smart_face_active) === 1;
                    smartSettings.fingerActive = parseInt(settings.att_smart_finger_active) === 1;
                }
            } catch (e) {
                console.error("Failed to load smart settings", e);
            }
        }

        async function runCameraDiagnostic() {
            const ip = document.getElementById('setting_face_ip').value;
            const resDiv = document.getElementById('camera_diag_result');
            resDiv.style.display = 'block';
            resDiv.style.background = 'rgba(255, 255, 255, 0.05)';
            resDiv.style.borderColor = 'rgba(255, 255, 255, 0.1)';
            resDiv.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Analyzing network configuration...';

            try {
                const resp = await fetch('attendance_diag_api.php?ip=' + encodeURIComponent(ip));
                const data = await resp.json();

                let html = `<div style="font-weight: 800; margin-bottom: 8px; text-transform: uppercase; font-size: 10px; letter-spacing: 1px;">Diagnostic Results</div>`;
                
                if (data.reachable) {
                    resDiv.style.background = 'rgba(46, 204, 113, 0.1)';
                    resDiv.style.borderColor = 'rgba(46, 204, 113, 0.2)';
                    resDiv.style.color = '#2ecc71';
                    html += `<div><i class="fas fa-check-circle"></i> Camera at <b>${data.cam_ip}</b> is REACHABLE!</div>`;
                    html += `<div style="margin-top: 5px; color: var(--text-dim); opacity: 0.8;">HTTP Code: ${data.http_code}. Your connection is healthy.</div>`;
                } else {
                    resDiv.style.background = 'rgba(231, 76, 60, 0.1)';
                    resDiv.style.borderColor = 'rgba(231, 76, 60, 0.2)';
                    resDiv.style.color = '#e74c3c';
                    html += `<div><i class="fas fa-times-circle"></i> Camera at <b>${data.cam_ip}</b> is UNREACHABLE.</div>`;
                    
                    if (!data.subnet_match) {
                        html += `<div style="margin-top: 10px; color: #fff; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 6px;">`;
                        html += `<div style="color: var(--warning); font-weight: 800;"><i class="fas fa-exclamation-triangle"></i> SUBNET MISMATCH DETECTED</div>`;
                        html += `<div style="margin-top: 5px;">Your laptop IP(s): <b>${data.server_ips.join(', ')}</b></div>`;
                        html += `<div style="margin-top: 5px;">Camera IP: <b>${data.cam_ip}</b></div>`;
                        html += `<div style="margin-top: 10px; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 10px;"><b>FIX (Ethernet Cable):</b> Set your Laptop Ethernet IP to <b>192.168.1.50</b>.</div>`;
                        html += `</div>`;
                    } else {
                        html += `<div style="margin-top: 5px; color: var(--text-dim);">cURL: ${data.curl_error || 'Timeout'}. OS Ping: ${data.ping_ok ? 'PASS' : 'FAIL'}.</div>`;
                    }

                    // Raw Debug Info
                    if (data.raw_ip) {
                        html += `<div style="margin-top: 15px; border: 1px dashed rgba(255,255,255,0.3); padding: 10px; border-radius: 6px;">`;
                        html += `<div style="color: var(--warning); font-weight: 800; font-size: 11px; margin-bottom: 5px;">DEBUG TOOLS (FOR SUPPORT)</div>`;
                        
                        html += `<div style="margin-bottom: 10px;">
                                    <button onclick="togglePortabilityGuide()" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: #fff; padding: 5px 10px; border-radius: 4px; font-size: 10px; cursor: pointer;">
                                        <i class="fas fa-question-circle"></i> View End-User Portability Guide
                                    </button>
                                 </div>`;

                        html += `<div id="portabilityGuide" style="display:none; background: rgba(0,0,0,0.3); padding: 10px; border-radius: 4px; margin-bottom: 10px; font-size: 10px; color: #eee; line-height: 1.4; border-left: 3px solid var(--accent);">
                                    <b>How to fix for a New Client:</b><br>
                                    1. <b>Reset Camera:</b> Press the physical reset button on the camera for 10s.<br>
                                    2. <b>Search Tool:</b> Download "Dahua ConfigTool" on the laptop.<br>
                                    3. <b>Initialize:</b> Open ConfigTool, find the camera, and set its IP to match the laptop subnet (e.g., if laptop is 192.168.100.X, set camera to 192.168.100.108).<br>
                                    4. <b>Update Settings:</b> Type the new IP into the "Face IP" setting above.
                                 </div>`;

                        html += `<div id="rawIpConfig" style="background: #000; color: #0f0; font-family: monospace; font-size: 9px; padding: 10px; border-radius: 4px; max-height: 150px; overflow: auto; text-align: left; user-select: all; border: 1px solid #333;">`;
                        html += data.raw_ip.replace(/\n/g, '<br>');
                        html += `</div></div>`;
                    }
                }
                resDiv.innerHTML = html;
            } catch (e) {
                resDiv.innerHTML = '<span style="color: #e74c3c;">Diagnostic failed to run. Check server connection.</span>';
            }
        }

        async function saveSmartSettings() {
            // Safely get elements
            const getVal = (id) => document.getElementById(id) ? document.getElementById(id).value : '';
            const getCheck = (id) => document.getElementById(id) ? (document.getElementById(id).checked ? 1 : 0) : 0;

            const data = {
                face_active: getCheck('setting_face_active'),
                finger_active: getCheck('setting_finger_active'),
                face_ip: getVal('setting_face_ip') || "192.168.1.108",
                face_user: getVal('setting_face_user') || "admin",
                face_pass: getVal('setting_face_pass') || "pakistan123",
                auto_mark: document.getElementById('setting_smart_sms') ? getCheck('setting_smart_sms') : getCheck('setting_auto_mark'), 
                sound: getCheck('setting_sound'),
                
                // New Schedule Config
                att_mode: document.getElementById('setting_att_mode').value,
                in_time_start: document.getElementById('setting_in_start').value,
                in_time_end: document.getElementById('setting_in_end').value,
                out_time_start: document.getElementById('setting_out_start').value,
                out_time_end: document.getElementById('setting_out_end').value
            };

            smartSettings.inStart = data.in_time_start;
            smartSettings.inEnd = data.in_time_end;
            smartSettings.outStart = data.out_time_start;
            smartSettings.outEnd = data.out_time_end;
            smartSettings.attMode = data.att_mode;
            smartSettings.faceActive = data.face_active === 1;
            smartSettings.fingerActive = data.finger_active === 1;
            smartSettings.autoMark = data.auto_mark === 1;
            smartSettings.sound = data.sound === 1;

            try {
                const resp = await fetch('attendance_api.php?action=save_smart_settings', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                const res = await resp.json();
                if (res.success) {
                    alert("Configuration Saved Successfully!");
                } else {
                    alert("Save Error: " + res.error);
                }
            } catch (e) {
                alert("Connection failed during save.");
            }
            
            // Immediate Effect: Stop Engines if disabled
            if (!smartSettings.faceActive && !forceAttendanceActive) {
                isFaceSystemStarted = false;
                if(document.getElementById('faceVideoPlaceholder')) {
                     document.getElementById('faceVideoPlaceholder').style.display = 'flex';
                     document.getElementById('faceLoaderMsg').innerHTML = '<span style="color:#e74c3c; font-weight:800;">VISION ENGINE DISABLED</span>';
                }
                if(document.getElementById('faceVideoDisplay')) document.getElementById('faceVideoDisplay').style.display = 'none';
            }
            
            if (!smartSettings.fingerActive && !forceAttendanceActive) {
                isFingerprintLoopRunning = false;
                const mStatus = document.getElementById('fingerprintMainStatus');
                if(mStatus) {
                    mStatus.textContent = "ENGINE DISABLED";
                    mStatus.style.color = "#bdc3c7";
                }
            }
        }

        function toggleCheckOutSettings() {
            const mode = document.getElementById('setting_att_mode').value;
            const container = document.getElementById('checkout_settings_container');
            const checkinLabel = document.querySelector('#checkin_window label');
            
            if (mode === 'check_in_out') {
                container.style.display = 'block';
                if(checkinLabel) checkinLabel.textContent = "SHIFT START (IN)";
            } else {
                container.style.display = 'none';
                if(checkinLabel) checkinLabel.textContent = "CHECK-IN TIME";
            }
        }

        function togglePasswordVisibility() {
            const input = document.getElementById('setting_face_pass');
            const icon = document.getElementById('toggle_pass_icon');
            if (input.type === "password") {
                input.type = "text";
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
                icon.style.color = "var(--primary)";
            } else {
                input.type = "password";
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
                icon.style.color = "var(--text-dim)";
            }
        }

        async function openAttendanceSummaryModal() {
            document.getElementById('attendanceSummaryModal').style.display = 'flex';

            loadAttendanceReportSummary();
        }

        function closeAttendanceSummaryModal() {
            document.getElementById('attendanceSummaryModal').style.display = 'none';
        }

        async function loadAttendanceReportSummary() {
            const tbodyStd = document.getElementById('student-report-tbody');
            const tfootStd = document.getElementById('student-report-tfoot');
            const tbodyStaff = document.getElementById('staff-report-tbody');
            const tfootStaff = document.getElementById('staff-report-tfoot');
            
            const loading = '<tr><td colspan="13" style="text-align:center; padding:50px; opacity:0.5;"><i class="fas fa-circle-notch fa-spin"></i> GENERATING STATISTICAL REPORT...</td></tr>';
            tbodyStd.innerHTML = loading;
            tbodyStaff.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:50px; opacity:0.5;"><i class="fas fa-circle-notch fa-spin"></i> GENERATING REPORT...</td></tr>';

            try {
                const resp = await fetch('attendance_api.php?action=get_attendance_report_summary');
                const data = await resp.json();
                
                if (data.success === false) throw new Error(data.error || "Server Error");
                
                // 1. Populate Students with Totals
                let stdHtml = '';
                let gTotal = {tm:0, tf:0, pm:0, pf:0, lm:0, lf:0, am:0, af:0};

                data.students.forEach(g => {
                    const rowSumT = g.total_m + g.total_f;
                    const rowSumP = g.present_m + g.present_f;
                    const rowSumL = g.leave_m + g.leave_f;
                    const rowSumA = g.absent_m + g.absent_f;

                    stdHtml += `
                        <tr class="report-row" onclick="openGroupDetailModal('student', ${g.class_id}, ${g.section_id}, '${g.class_name} ${g.section_name}')">
                            <td style="text-align:left; font-weight:800;">${g.class_name} ${g.section_name}</td>
                            <td>${g.total_m}</td><td>${g.total_f}</td><td style="background:rgba(52,152,219,0.1); font-weight:900;">${rowSumT}</td>
                            <td>${g.present_m}</td><td>${g.present_f}</td><td style="background:rgba(46,204,113,0.1); font-weight:900;">${rowSumP}</td>
                            <td>${g.leave_m}</td><td>${g.leave_f}</td><td style="background:rgba(241,194,15,0.1); font-weight:900;">${rowSumL}</td>
                            <td>${g.absent_m}</td><td>${g.absent_f}</td><td style="background:rgba(231,76,60,0.1); font-weight:900;">${rowSumA}</td>
                        </tr>
                    `;
                    gTotal.tm += g.total_m; gTotal.tf += g.total_f;
                    gTotal.pm += g.present_m; gTotal.pf += g.present_f;
                    gTotal.lm += g.leave_m; gTotal.lf += g.leave_f;
                    gTotal.am += g.absent_m; gTotal.af += g.absent_f;
                });
                
                tbodyStd.innerHTML = stdHtml || '<tr><td colspan="13" style="text-align:center;">No student records found.</td></tr>';
                tfootStd.innerHTML = `
                    <tr>
                        <td>GRAND TOTAL</td>
                        <td>${gTotal.tm}</td><td>${gTotal.tf}</td><td>${gTotal.tm+gTotal.tf}</td>
                        <td>${gTotal.pm}</td><td>${gTotal.pf}</td><td>${gTotal.pm+gTotal.pf}</td>
                        <td>${gTotal.lm}</td><td>${gTotal.lf}</td><td>${gTotal.lm+gTotal.lf}</td>
                        <td>${gTotal.am}</td><td>${gTotal.af}</td><td>${gTotal.am+gTotal.af}</td>
                    </tr>
                `;

                // 2. Populate Staff
                let staffHtml = '';
                let sTotal = {t:0, p:0, l:0, a:0};
                data.staff.forEach(g => {
                    staffHtml += `
                        <tr class="report-row" onclick="openGroupDetailModal('staff', null, null, '${g.label}', '${g.type}')">
                            <td>${g.label}</td>
                            <td>${g.total}</td>
                            <td>${g.present}</td>
                            <td>${g.leave}</td>
                            <td>${g.absent}</td>
                        </tr>
                    `;
                    sTotal.t += g.total; sTotal.p += g.present; sTotal.l += g.leave; sTotal.a += g.absent;
                });
                tbodyStaff.innerHTML = staffHtml || '<tr><td colspan="5" style="text-align:center;">No staff records found.</td></tr>';
                tfootStaff.innerHTML = `
                    <tr>
                        <td>GRAND TOTAL</td>
                        <td>${sTotal.t}</td>
                        <td>${sTotal.p}</td>
                        <td>${sTotal.l}</td>
                        <td>${sTotal.a}</td>
                    </tr>
                `;

            } catch (e) {
                console.error("Load Summary Error:", e);
                const errorHtml = `<tr><td colspan="13" style="text-align:center; color:red; font-weight:800;">Link Error: ${e.message}</td></tr>`;
                tbodyStd.innerHTML = errorHtml;
                tbodyStaff.innerHTML = errorHtml.replace('colspan="13"', 'colspan="5"');
            }
        }

        function printAttendanceSummary() {
            const isStudent = document.getElementById('report-student-panel').style.display !== 'none';
            const tableId = isStudent ? 'student-report-print-area' : 'staff-report-print-area';
            const title = isStudent ? 'STUDENT ATTENDANCE SUMMARY' : 'STAFF ATTENDANCE SUMMARY';
            const date = new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
            const qr_url = "https://quickchart.io/qr?text=" + encodeURIComponent(window.location.href) + "&size=100";
            
            const printContent = document.getElementById(tableId).outerHTML;
            const win = window.open('', '', 'height=800,width=1100');
            
            win.document.write(`
                <html>
                <head>
                    <title>Print Report - ${title}</title>
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800;900&display=swap" rel="stylesheet">
                    <style>
                        :root {
                            --royal-blue: #2c52a0;
                            --cream-soft: #fbf9f1;
                        }
                        body { 
                            font-family: 'Inter', sans-serif; 
                            margin: 0; padding: 0; 
                            background-color: #cbd5e1;
                        }
                        .page-container {
                            width: 100%;
                            max-width: 28cm;
                            margin: 20px auto;
                            background-color: var(--cream-soft);
                            position: relative;
                            display: flex;
                            flex-direction: column;
                            border: 1px solid #ddd;
                        }
                        @media print {
                            body { background: white !important; margin: 0; padding: 0; }
                            .page-container { 
                                width: 100% !important; max-width: none !important; margin: 0 !important; border: none !important; 
                                zoom: 0.8; /* Slightly smaller zoom to allow larger, clearer internal text */
                            }
                            @page { size: landscape; margin: 0; }
                        }

                        /* Header Styles */
                        .header-modern {
                            height: 100px; 
                            display: flex;
                            border-bottom: 4px solid var(--royal-blue);
                            position: relative;
                            overflow: hidden;
                        }
                        .header-blue-zone {
                            flex: 6;
                            background: var(--royal-blue);
                            color: white;
                            padding: 10px 45px;
                            clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                            z-index: 10;
                        }
                        .header-blue-zone h1 {
                            font-size: 20px;
                            font-weight: 700;
                            margin: 0;
                            text-transform: uppercase;
                        }
                        .header-blue-zone p {
                            font-size: 11px;
                            margin: 2px 0 0;
                            opacity: 0.9;
                            font-weight: 600;
                        }
                        .header-white-zone {
                            flex: 4;
                            padding: 5px 30px;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                            align-items: flex-end;
                            background: transparent;
                            position: relative;
                        }
                        .school-logo-img {
                            width: 70px; height: 70px;
                            object-fit: contain;
                            position: absolute;
                            top: 10px; right: 30px;
                            z-index: 20;
                        }
                        .header-info-content {
                            margin-right: 110px;
                            text-align: right;
                            z-index: 10;
                        }
                        .header-info-content strong {
                            font-size: 24px;
                            color: var(--royal-blue);
                            text-transform: uppercase;
                            display: block;
                            font-weight: 900;
                            line-height: 1;
                        }
                        .header-info-content p {
                            font-size: 10px;
                            margin: 2px 0 0;
                            color: #000;
                            font-weight: 700;
                        }
                        .header-blue-tip {
                            position: absolute; bottom: 0; right: 0;
                            width: 100px; height: 40px;
                            background: var(--royal-blue);
                            clip-path: polygon(100% 0, 100% 100%, 0 100%);
                            z-index: 1;
                        }

                        /* Table Styles */
                        .print-body { padding: 10px 40px; flex: 1; position: relative; z-index: 5; }
                        table { 
                            width: 100%; border-collapse: collapse; 
                            background: white; font-size: 11.5px; 
                            border: 1px solid #cbd5e1;
                        }
                        th { 
                            background: var(--royal-blue) !important; color: white !important; 
                            padding: 10px 5px; font-weight: 800; text-transform: uppercase;
                            border: 1px solid rgba(255,255,255,0.2); 
                        }
                        td { 
                            padding: 8px 5px; text-align: center; border: 1px solid #cbd5e1; 
                            color: #000; font-weight: 900;
                        }
                        tfoot td { background: #f1f5f9 !important; font-weight: 950; color: var(--royal-blue); font-size: 13px; padding: 10px 5px; }
                        
                        /* Watermark */
                        .page-container::before {
                            content: "";
                            position: absolute; top: 50%; left: 50%;
                            transform: translate(-50%, -50%);
                            width: 400px; height: 400px;
                            background-image: url('${INSTITUTION_LOGO}');
                            background-repeat: no-repeat; background-position: center; background-size: contain;
                            opacity: 0.035; z-index: 0; pointer-events: none;
                        }

                        /* Footer Styles */
                        .footer-combined {
                            padding: 10px 40px;
                            display: grid;
                            grid-template-columns: 1fr 1fr 1fr 100px;
                            gap: 30px;
                            align-items: flex-end;
                            border-top: 1px solid #f1f5f9;
                            margin-top: 10px;
                        }
                        .sig-box {
                            border-top: 1.5px dashed #000;
                            padding-top: 8px;
                            text-align: center;
                            font-size: 11px;
                            font-weight: 800;
                            color: #1e293b;
                            text-transform: uppercase;
                        }
                        .qr-footer-box {
                            display: flex; flex-direction: column; align-items: center;
                            background: white; padding: 3px; border: 1px solid #e2e8e0;
                            width: fit-content; margin-left: auto;
                        }
                        .qr-footer-box img { width: 45px; height: 45px; }
                        .qr-footer-label { font-size: 6px; font-weight: 800; color: var(--royal-blue); text-transform: uppercase; margin-top: 2px; }
                    </style>
                </head>
                <body>
                    <div class="page-container">
                        <div class="header-modern">
                            <div class="header-blue-zone">
                                <h1>${INSTITUTION_NAME}</h1>
                                <p>OFFICIAL ACADEMIC ARCHIVE</p>
                            </div>
                            <div class="header-white-zone">
                                <img src="${INSTITUTION_LOGO}" class="school-logo-img">
                                <div class="header-info-content">
                                    <strong>${title}</strong>
                                    <p style="font-size: 10px; margin: 2px 0; opacity: 0.8;">123</p>
                                    <p>${date} | Global Attendance Report (MASTER)</p>
                                </div>
                                <div class="header-blue-tip"></div>
                            </div>
                        </div>
                        
                        <div class="print-body">
                            ${printContent}
                        </div>

                        <div class="footer-combined">
                            <div class="sig-box">Head of Institution</div>
                            <div class="sig-box">Attendance Coordinator</div>
                            <div class="sig-box">Academic Council</div>
                            <div class="qr-footer-box">
                                <img src="${qr_url}">
                                <span class="qr-footer-label">SECURE SCHEDULE</span>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `);
            
            win.document.close();
            win.focus();
            setTimeout(() => { win.print(); win.close(); }, 800);
        }

        async function shareSummaryPDFWhatsApp() {
            const isStudent = document.getElementById('report-student-panel').style.display !== 'none';
            const tableId = isStudent ? 'student-report-print-area' : 'staff-report-print-area';
            const title = isStudent ? 'STUDENT_ATTENDANCE_SUMMARY' : 'STAFF_ATTENDANCE_SUMMARY';
            const headerTitle = isStudent ? 'STUDENT ATTENDANCE SUMMARY' : 'STAFF ATTENDANCE SUMMARY';
            
            const element = document.createElement('div');
            element.className = 'page-container';
            element.style.width = '21cm'; // A4 Portrait for PDF
            element.style.padding = '40px';
            element.style.background = '#fbf9f1';
            element.style.color = '#1e293b';
            element.style.fontFamily = "'Inter', sans-serif";

            const date = new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
            
            element.innerHTML = `
                <style>
                    #pdf-summary-container * {
                        color: inherit !important;
                        background-color: transparent !important;
                        border-color: inherit !important;
                    }
                    #pdf-summary-container {
                        background-color: #fbf9f1 !important;
                        color: #1e293b !important;
                    }
                    #pdf-summary-container h1 { color: #2c52a0 !important; }
                    #pdf-summary-container p { color: #1e293b !important; }
                    #pdf-summary-container .print-table-container { color: #1e293b !important; }
                    #pdf-summary-container table { width: 100%; border-collapse: collapse; font-size: 11px; margin-top: 20px; border: 1px solid #ddd !important; }
                    #pdf-summary-container th { background-color: #2c52a0 !important; color: white !important; padding: 10px; border: 1px solid #ddd !important; text-align: center; }
                    #pdf-summary-container td { color: #1e293b !important; padding: 8px; border: 1px solid #ddd !important; text-align: center; font-weight: 700; }
                    #pdf-summary-container .stats-table th:first-child, #pdf-summary-container .stats-table td:first-child { width: 150px !important; text-align: left !important; }
                </style>
                <div id="pdf-summary-container">
                    <div style="border-bottom: 4px solid #2c52a0; padding-bottom: 20px; margin-bottom: 30px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <h1 style="font-size: 24px; margin: 0; text-transform: uppercase; font-weight: 900;">${INSTITUTION_NAME}</h1>
                            <p style="font-size: 14px; margin: 5px 0 0; opacity: 0.8; font-weight: 700;">${headerTitle}</p>
                        </div>
                        <img src="${INSTITUTION_LOGO}" style="height: 60px; object-fit: contain;">
                    </div>
                    <div style="font-size: 12px; margin-bottom: 10px; font-weight: 700;">DATE: ${date}</div>
                    <div class="print-table-container">
                        ${document.getElementById(tableId).outerHTML}
                    </div>
                    <div style="margin-top: 40px; border-top: 1px solid #cbd5e1; padding-top: 10px; font-size: 10px; text-align: center; opacity: 0.6;">
                        Generated via Attendance Intelligence Platform | ${new Date().toLocaleString()}
                    </div>
                </div>
            `;

            document.body.appendChild(element);

            const opt = {
                margin: 0.5,
                filename: `${title}_${date}.pdf`,
                image: { type: 'jpeg', quality: 0.98 },
                html2canvas: { scale: 2, useCORS: true },
                jsPDF: { unit: 'in', format: 'a4', orientation: 'portrait' }
            };

            try {
                Swal.fire({
                    title: 'Generating PDF...',
                    html: 'Preparing your summary for sharing.',
                    allowOutsideClick: false,
                    didOpen: () => { Swal.showLoading(); }
                });

                // Temporarily scroll to top so html2canvas doesn't capture blank background offset
                const originalScrollY = window.scrollY;
                window.scrollTo(0, 0);

                const pdfBase64 = await html2pdf().set(opt).from(element).output('datauristring');
                
                // Restore scroll
                window.scrollTo(0, originalScrollY);
                document.body.removeChild(element);

                // Trigger local download
                const link = document.createElement('a');
                link.href = pdfBase64;
                link.download = opt.filename;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

                // Save to server
                const saveResp = await fetch('attendance_api.php?action=save_summary_pdf', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        pdf_data: pdfBase64,
                        filename: `${title}_${date.replace(/ /g, '_')}.pdf`
                    })
                });

                const saveData = await saveResp.json();
                Swal.close();

                if (saveData.success) {
                    const message = `*📊 ${headerTitle}*\nDate: ${date}\nInstitution: ${INSTITUTION_NAME}\n\nDownload Report: ${saveData.url}`;
                    window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(message)}`, '_blank');
                } else {
                    throw new Error(saveData.error || 'Failed to save PDF');
                }

            } catch (err) {
                console.error(err);
                if(element.parentNode) document.body.removeChild(element);
                Swal.fire('Error', 'PDF Generation failed: ' + err.message, 'error');
            }
        }

        function switchReportTab(type) {
            // Update Tab Headings
            document.querySelectorAll('.report-tab').forEach(t => t.classList.remove('active'));
            const tabEl = document.getElementById('report-tab-' + type);
            if(tabEl) tabEl.classList.add('active');
            
            // Toggle Panels inside Modal
            const stdPanel = document.getElementById('report-student-panel');
            const staffPanel = document.getElementById('report-staff-panel');
            if(stdPanel) stdPanel.style.display = (type === 'student' ? 'block' : 'none');
            if(staffPanel) staffPanel.style.display = (type === 'staff' ? 'block' : 'none');
            
            console.log(`Global Summary Tab Switched: ${type.toUpperCase()}`);
        }

        async function openGroupDetailModal(type, cid, sid, title, userType = '') {
            const modal = document.getElementById('groupDetailsModal');
            const container = document.getElementById('details-names-container');
            document.getElementById('detailsModalTitle').innerText = title + ' - PERSONNEL LIST';
            
            container.innerHTML = '<div style="width:100%; text-align:center; padding:30px; opacity:0.5;"><i class="fas fa-spinner fa-spin"></i> FETCHING NAMES...</div>';
            modal.style.display = 'flex';

            try {
                let url = `attendance_api.php?action=get_group_attendance_details&type=${type}`;
                if (type === 'student') url += `&class_id=${cid}&section_id=${sid}`;
                else url += `&user_type=${userType}`;

                const resp = await fetch(url);
                const list = await resp.json();
                
                let html = '';
                list.forEach(p => {
                    const statusClass = p.status || 'Unknown';
                    const classNoLabel = p.class_no ? `<span class="cno">#${p.class_no}</span>` : '';
                    html += `<div class="name-chip ${statusClass}">${p.name}${classNoLabel}</div>`;
                });
                container.innerHTML = html || '<div style="width:100%; text-align:center; opacity:0.5;">No personnel found in this group.</div>';
            } catch (e) {
                container.innerHTML = '<div style="width:100%; text-align:center; color:red;">Link Failure.</div>';
            }
        }

        function closeGroupDetailsModal() {
            document.getElementById('groupDetailsModal').style.display = 'none';
        }

        async function simulateFingerprintMatch() {
            if (fingerprintTemplates.length === 0) {
                alert("No templates loaded to simulate. Enroll someone first!");
                return;
            }
            // Pick a random template from the loaded list
            const randomIndex = Math.floor(Math.random() * fingerprintTemplates.length);
            const user = fingerprintTemplates[randomIndex];
            
            if (confirm(`SIMULATION MODE: Match found for ${user.name}. Mark attendance?`)) {
                try {
                    // Actually mark it in DB
                    const resp = await fetch('attendance_api.php?action=mark_smart_attendance', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ user_id: user.user_id, type: 'fingerprint_sim', score: 99.9 })
                    });
                    const res = await resp.json();
                    
                    showMatchSuccess(user, res.already_marked);
                } catch(e) {
                    console.error("Simulation Mark Error:", e);
                    showMatchSuccess(user);
                }
            }
        }


        // ============ BULLETPROOF TAB PERSISTENCE & AUTO-RESTART ============
        const DM_TAB_KEY = 'dm_active_tabs';

        function _dmSaveTabState(mainTab, subTab) {
            try {
                const state = JSON.parse(sessionStorage.getItem(DM_TAB_KEY) || '{}');
                if (mainTab) state.mainTab = mainTab;
                if (subTab) state.subTab = subTab;
                sessionStorage.setItem(DM_TAB_KEY, JSON.stringify(state));
            } catch(e) {}
        }

        function _dmGetTabState() {
            try {
                const s = JSON.parse(sessionStorage.getItem(DM_TAB_KEY) || '{}');
                return { 
                    mainTab: s.mainTab || 'students-att-panel', 
                    subTab: s.subTab || 'face-recognition-panel' 
                };
            } catch(e) { return { mainTab: 'students-att-panel', subTab: 'face-recognition-panel' }; }
        }

        /**
         * Direct State Injection: Restores the UI tab styles without simulating clicks
         */
        function _dmRestoreUI(targetMain, targetSub) {
            console.log(`[DEVICE-MANAGER] Force-restoring UI to: ${targetMain} > ${targetSub}`);
            
            // 1. Reset all main panels
            document.querySelectorAll('.attendance-panel').forEach(p => p.style.display = 'none');
            document.querySelectorAll('.sub-tab-btn').forEach(b => b.classList.remove('active'));
            
            // 2. Show Target Main Panel
            const mainEl = document.getElementById(targetMain);
            if (mainEl) {
                mainEl.style.display = 'block';
                // Find and activate the button
                const mainBtn = document.querySelector(`[onclick*="switchAttendanceSubTab('${targetMain}'"]`);
                if (mainBtn) mainBtn.classList.add('active');
            }

            // 3. Show Target sub-panel (if inside Smart Attendance)
            if (targetMain === 'smart-att-panel') {
                document.querySelectorAll('#smart-att-panel .smart-panel').forEach(p => p.style.display = 'none');
                const subEl = document.getElementById(targetSub);
                if (subEl) {
                    subEl.style.display = 'block';
                    subEl.classList.add('active');
                    const subBtn = document.querySelector(`[onclick*="switchSmartSubTab('${targetSub}'"]`);
                    if (subBtn) subBtn.classList.add('active');
                }
            }
        }

        // Auto-initialize observers
        const originalSwitchSmartSubTab = switchSmartSubTab;
        switchSmartSubTab = function(pId, btn) { 
            if(typeof originalSwitchSmartSubTab === 'function') originalSwitchSmartSubTab(pId, btn); 
            _dmSaveTabState(null, pId);
            if (pId !== 'fingerprint-panel' && !forceAttendanceActive) isFingerprintLoopRunning = false;
            if(pId === 'face-recognition-panel') setTimeout(initFaceRecognition, 300); 
            if(pId === 'fingerprint-panel') { initFingerprintSystem(); setTimeout(startFingerprintAutoLoop, 600); }
        };
        
        const originalSwitchAttendanceSubTab = switchAttendanceSubTab;
        switchAttendanceSubTab = function(pId, btn) { 
            if(typeof originalSwitchAttendanceSubTab === 'function') originalSwitchAttendanceSubTab(pId, btn); 
            _dmSaveTabState(pId, null);
            if (pId !== 'smart-att-panel' && !forceAttendanceActive) {
                isFingerprintLoopRunning = false;
                if (window.fingerprintSDK) window.fingerprintSDK.stopCapture();
            }
        };

        // Logic to run once we are sure devices should be on
        function _dmAutoStartDevices() {
            if (!systemMasterState) return;
            
            console.log('[DEVICE-MANAGER] Auto-starting hardware devices...');
            
            // Sync UI switches
            const switches = [document.getElementById('master_system_switch'), document.getElementById('master_system_switch_face')];
            switches.forEach(sw => { if(sw) sw.checked = true; });
            
            // Trigger Master Logic
            toggleSystemMaster(null); 
        }

        // Fast Restoration
        document.addEventListener('DOMContentLoaded', () => {
            if (typeof DeviceManager !== 'undefined' && DeviceManager.wasSystemOn()) {
                const tabs = _dmGetTabState();
                // If it was ON, we MUST be in Smart Attendance
                _dmRestoreUI('smart-att-panel', tabs.subTab);
                systemMasterState = true;
            }
        });

        // Safety Restoration (in case DOMContentLoaded was too fast)
        window.addEventListener('load', () => {
            // Load saved Smart Configuration settings from DB on every page load
            if (typeof loadSmartSettings === 'function') loadSmartSettings();

            if (typeof DeviceManager !== 'undefined') DeviceManager.init();
            
            if (systemMasterState) {
                const tabs = _dmGetTabState();
                _dmRestoreUI('smart-att-panel', tabs.subTab);
                
                // Wait for all SDKs to settle
                setTimeout(_dmAutoStartDevices, 1000);
            }
            
            if (typeof DeviceManager !== 'undefined') DeviceManager.syncNow();
        });

        // MASTER HEARTBEAT: Ensure system stays in expected state
        setInterval(() => {
            if (systemMasterState && document.visibilityState === 'visible') {
                const activeMain = document.getElementById('smart-att-panel');
                if (activeMain && activeMain.style.display !== 'none') {
                    // We are in smart attendance and system is ON.
                    // If camera or fingerprint aren't running, poke them.
                    const faceActive = document.getElementById('face-recognition-panel');
                    if (faceActive && faceActive.style.display !== 'none' && !isFaceSystemStarted) {
                        console.log('[DEVICE-MANAGER] Heartbeat: Camera should be on but isn\'t. Restarting...');
                        initFaceRecognition();
                    }
                }
            }
        }, 10000); // 10s check
        
        // Sleep/Wake detection
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible' && systemMasterState) {
                console.log('[DEVICE-MANAGER] Global Wake detected');
                if (typeof DeviceManager !== 'undefined') DeviceManager.onWake();
                
                // Re-verify stream
                const videoEl = document.getElementById('faceVideoDisplay');
                if (videoEl && isFaceSystemStarted && (!videoEl.src || videoEl.src === '')) {
                    videoEl.src = '../../FaceDetection/camera_proxy.php?t=' + Date.now();
                }
            }
        });






    </script>
    <div id="attendanceSummaryModal" class="premium-modal-overlay" style="display:none; z-index: 11000;">
        <div class="premium-modal" style="max-width: 800px;">
            <div class="modal-header">
                <div style="display:flex; align-items:center; gap:12px;">
                    <div class="capsule-3d" style="background:#3498db;"><i class="fas fa-chart-bar" style="color:white;"></i></div>
                    <h4 style="margin:0; font-size:14px; font-weight:900; letter-spacing:1px;">ATTENDANCE SUMMARY (TODAY)</h4>
                </div>
                <div style="display:flex; align-items:center; gap:15px;">
                    <button onclick="printAttendanceSummary()" style="background: rgba(52, 152, 219, 0.1); border: 1px solid rgba(52, 152, 219, 0.2); color: #3498db; padding: 6px 15px; border-radius: 6px; font-size: 10px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 8px;">
                        <i class="fas fa-print"></i> PRINT REPORT
                    </button>
                    <button onclick="shareSummaryPDFWhatsApp()" style="background: rgba(37, 211, 102, 0.1); border: 1px solid rgba(37, 211, 102, 0.2); color: #25d366; padding: 6px 15px; border-radius: 6px; font-size: 10px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 8px;">
                        <i class="fab fa-whatsapp"></i> SHARE PDF
                    </button>
                    <i class="fas fa-times" style="cursor:pointer; opacity: 0.5;" onclick="closeAttendanceSummaryModal()"></i>
                </div>
            </div>
            <div class="modal-body" style="padding:0;">
                <!-- Tab Switching for Reports -->
                <div style="display:flex; background:rgba(0,0,0,0.1); border-bottom:1px solid rgba(255,255,255,0.05);">
                    <div class="report-tab active" id="report-tab-student" onclick="switchReportTab('student')" style="flex:1; padding:15px; text-align:center; cursor:pointer; font-weight:900; font-size:11px; letter-spacing:1px;">STUDENTS</div>
                    <div class="report-tab" id="report-tab-staff" onclick="switchReportTab('staff')" style="flex:1; padding:15px; text-align:center; cursor:pointer; font-weight:900; font-size:11px; letter-spacing:1px;">STAFF</div>
                </div>
                
                <style>
                    .report-tab { opacity:0.5; border-bottom:3px solid transparent; transition:0.3s; }
                    .report-tab.active { opacity:1; border-bottom-color:#3498db; background:rgba(52, 152, 219, 0.05); }
                </style>

                <div class="report-modal-body">
                    <!-- Student Summary Content -->
                    <div id="report-student-panel">
                        <table class="report-table stats-table" id="student-report-print-area">
                            <thead>
                                <tr>
                                    <th rowspan="2" style="background: rgba(255,255,255,0.03);">CLASS & SECTION</th>
                                    <th colspan="3" style="text-align:center; background: rgba(52, 152, 219, 0.05);">TOTAL</th>
                                    <th colspan="3" style="text-align:center; background: rgba(46, 204, 113, 0.05);">PRESENT</th>
                                    <th colspan="3" style="text-align:center; background: rgba(241, 194, 15, 0.05);">LEAVE</th>
                                    <th colspan="3" style="text-align:center; background: rgba(231, 76, 60, 0.05);">ABSENT</th>
                                </tr>
                                <tr class="sub-headers">
                                    <th title="Male">M</th><th title="Female">F</th><th title="Sum">Σ</th>
                                    <th title="Male">M</th><th title="Female">F</th><th title="Sum">Σ</th>
                                    <th title="Male">M</th><th title="Female">F</th><th title="Sum">Σ</th>
                                    <th title="Male">M</th><th title="Female">F</th><th title="Sum">Σ</th>
                                </tr>
                            </thead>
                            <tbody id="student-report-tbody">
                                <!-- Dynamic Load -->
                            </tbody>
                            <tfoot id="student-report-tfoot" style="background: rgba(255,255,255,0.05); font-weight: 800;">
                                <!-- Totals calculate via JS -->
                            </tfoot>
                        </table>
                    </div>

                    <!-- Staff Summary Content -->
                    <div id="report-staff-panel" style="display:none;">
                         <table class="report-table" id="staff-report-print-area">
                            <thead>
                                <tr>
                                    <th>CATEGORY</th>
                                    <th>TOTAL</th>
                                    <th>PRESENT</th>
                                    <th>LEAVE</th>
                                    <th>ABSENT</th>
                                </tr>
                            </thead>
                            <tbody id="staff-report-tbody">
                                <!-- Dynamic Load -->
                            </tbody>
                            <tfoot id="staff-report-tfoot" style="background: rgba(255,255,255,0.05); font-weight: 800;">
                            </tfoot>
                        </table>
                    </div>
                </div>

                <style>
                    .stats-table { width: 100%; border-collapse: separate; border-spacing: 0; table-layout: fixed; min-width: 650px; }
                    .stats-table th { font-size: 8px !important; padding: 8px 2px !important; text-align: center; border: 1px solid rgba(255,255,255,0.05); word-wrap: break-word; }
                    .stats-table td { font-size: 10px !important; text-align: center; padding: 10px 4px !important; border: 1px solid rgba(255,255,255,0.03); }
                    
                    /* Force first column width */
                    .stats-table th:first-child, .stats-table td:first-child { 
                        width: 140px !important; 
                        text-align: left !important; 
                        padding-left: 12px !important; 
                        background: rgba(255,255,255,0.02); 
                        white-space: nowrap; 
                        overflow: hidden; 
                        text-overflow: ellipsis; 
                    }
                    
                    /* All numeric columns (12 total) should be equal */
                    .stats-table th:not(:first-child), 
                    .stats-table td:not(:first-child) { 
                        width: 42px !important; 
                    }

                    .sub-headers th { font-size: 7px !important; color: var(--text-dim); background: #f8fafc !important; font-weight: 500; }
                    body.dark-mode .sub-headers th { background: #1e293b !important; }

                    .report-table tfoot td { border-top: 2px solid var(--primary) !important; color: #fff; background: rgba(52, 152, 219, 0.15) !important; font-weight: 900; }
                    .report-row:hover td { background: rgba(52, 152, 219, 0.05); }
                    #attendanceSummaryModal .modal-body { overflow-x: auto; max-height: calc(100vh - 200px); scroll-behavior: smooth; }
                    
                    /* STICKY HEADER SYSTEM */
                    .stats-table thead th { 
                        position: sticky !important; 
                        z-index: 100 !important; 
                        background-color: var(--modal-bg, #fff) !important;
                        box-shadow: inset 0 -1px 0 rgba(0,0,0,0.1);
                    }
                    
                    /* Vertical Alignment */
                    .stats-table thead tr:nth-child(1) th { top: 0; }
                    .stats-table thead tr:nth-child(2) th { top: 33px; } /* Exact height of first row headers */
                    
                    /* Column-specific Tints (Maintenance of UI colors while scrolling) */
                    .stats-table thead tr:first-child th:nth-child(2) { background-color: #f1f7fc !important; } /* TOTAL blue tint */
                    .stats-table thead tr:first-child th:nth-child(3) { background-color: #f0faf3 !important; } /* PRESENT green tint */
                    .stats-table thead tr:first-child th:nth-child(4) { background-color: #fcf9f0 !important; } /* LEAVE yellow tint */
                    .stats-table thead tr:first-child th:nth-child(5) { background-color: #fcf1f0 !important; } /* ABSENT red tint */
                    
                    /* Theme Compatibility for Tints (Dark Mode Support) */
                    body.dark-mode .stats-table thead tr:first-child th:nth-child(2) { background-color: #1e293b !important; }
                    body.dark-mode .stats-table thead tr:first-child th:nth-child(3) { background-color: #064e3b !important; }
                    body.dark-mode .stats-table thead tr:first-child th:nth-child(4) { background-color: #451a03 !important; }
                    body.dark-mode .stats-table thead tr:first-child th:nth-child(5) { background-color: #450a0a !important; }

                    .premium-modal { margin: auto; }
                    .row-sum { background: rgba(255,255,255,0.03); font-weight: 700; }
                </style>
            </div>
        </div>
    </div>

    <!-- Secondary Detail Modal -->
    <div id="groupDetailsModal" class="premium-modal-overlay" style="display:none; z-index: 12000;">
        <div class="premium-modal" style="max-width: 600px;">
            <div class="modal-header">
                <div style="display:flex; align-items:center; gap:12px;">
                    <i class="fas fa-users" style="color:#3498db;"></i>
                    <h4 style="margin:0; font-size:13px; font-weight:900; letter-spacing:0.5px;" id="detailsModalTitle">GROUP DETAILS</h4>
                </div>
                <i class="fas fa-times" style="cursor:pointer;" onclick="closeGroupDetailsModal()"></i>
            </div>
            <div class="modal-body">
                <div class="name-chip-container" id="details-names-container">
                    <!-- Dynamic chips -->
                </div>
            </div>
        </div>
    </div>

    <!-- Fingerprint Management Modal -->
    <div id="fingerprintManagerModal" class="premium-modal-overlay" style="display:none; z-index: 11001;">
        <div class="premium-modal" style="max-width: 600px;">
            <div class="modal-header" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                <h4 style="margin:0;"><i class="fas fa-fingerprint" style="margin-right: 10px; color: var(--primary);"></i> SECURED TEMPLATES</h4>
                <div style="display: flex; align-items: center; gap: 15px;">
                    <button class="fp-purge-btn" onclick="deleteAllFingerprints()">
                        <i class="fas fa-trash-alt"></i> DELETE ALL
                    </button>
                    <i class="fas fa-times" style="cursor:pointer; opacity: 0.5;" onclick="closeFingerprintManager()"></i>
                </div>
            </div>
            <div class="modal-body">
                <div style="font-size: 11px; color: var(--text-dim); margin-bottom: 15px; font-weight: 600;">
                    Manage biometric signatures stored in the central database. Removing a template will prevent the user from matching via fingerprint.
                </div>

                <!-- Staff / Student Tab Switcher -->
                <div style="display: flex; gap: 8px; margin-bottom: 14px; background: rgba(0,0,0,0.2); padding: 5px; border-radius: 12px;">
                    <button id="fp-tab-staff" onclick="switchFpTab('staff')" style="flex:1; padding:9px 0; border-radius:9px; border:none; font-size:12px; font-weight:800; cursor:pointer; text-transform:uppercase; letter-spacing:0.5px; transition:0.3s; background:var(--primary); color:#fff; box-shadow:0 4px 12px rgba(99,102,241,0.3);">
                        <i class="fas fa-user-tie" style="margin-right:6px;"></i>Staff
                    </button>
                    <button id="fp-tab-student" onclick="switchFpTab('student')" style="flex:1; padding:9px 0; border-radius:9px; border:none; font-size:12px; font-weight:800; cursor:pointer; text-transform:uppercase; letter-spacing:0.5px; transition:0.3s; background:rgba(255,255,255,0.05); color:var(--text-dim);">
                        <i class="fas fa-user-graduate" style="margin-right:6px;"></i>Student
                    </button>
                </div>

                <!-- Staff Filters -->
                <div id="fp-filter-staff" style="margin-bottom:12px;">
                    <select id="fp-staff-type" onchange="loadFingerprintManagementList()" style="width:100%; height:42px !important; border-radius:10px !important; padding:0 12px !important; font-size:12px !important; font-weight:700 !important; background:rgba(0,0,0,0.3) !important; border:1px solid var(--border) !important; color:var(--text-main) !important; outline:none;">
                        <option value="">All Staff</option>
                        <option value="Teaching">Teaching</option>
                        <option value="Non-Teaching">Non-Teaching</option>
                    </select>
                </div>

                <!-- Student Filters -->
                <div id="fp-filter-student" style="margin-bottom:12px; display:none; display:flex; gap:8px; display:none;">
                    <select id="fp-student-class" onchange="onFpClassChange()" style="flex:1; height:42px !important; border-radius:10px !important; padding:0 12px !important; font-size:12px !important; font-weight:700 !important; background:rgba(0,0,0,0.3) !important; border:1px solid var(--border) !important; color:var(--text-main) !important; outline:none;">
                        <option value="">All Classes</option>
                        <?php foreach ($all_classes as $c): ?>
                        <option value="<?= $c['id'] ?>"><?= htmlspecialchars($c['name']) ?></option>
                        <?php endforeach; ?>
                    </select>
                    <select id="fp-student-section" onchange="loadFingerprintManagementList()" style="flex:1; height:42px !important; border-radius:10px !important; padding:0 12px !important; font-size:12px !important; font-weight:700 !important; background:rgba(0,0,0,0.3) !important; border:1px solid var(--border) !important; color:var(--text-main) !important; outline:none;">
                        <option value="">All Sections</option>
                    </select>
                </div>

                <!-- Search -->
                <div style="margin-bottom: 12px; position: relative;">
                    <i class="fas fa-search" style="position: absolute; left: 13px; top: 13px; color: var(--text-dim); opacity: 0.6; font-size: 13px;"></i>
                    <input type="text" id="fingerprintFilter" placeholder="Search by name or UID..." onkeyup="filterFingerprints()" style="width: 100%; padding: 11px 15px 11px 38px; border-radius: 10px; border: 1px solid var(--modal-border); background: var(--card-bg, rgba(0,0,0,0.02)); color: var(--modal-text); font-size: 13px; font-weight: 600; outline: none; transition: 0.3s; box-sizing: border-box;" onfocus="this.style.boxShadow='0 0 0 2px var(--primary)'" onblur="this.style.boxShadow='none'">
                </div>

                <div id="fingerprint-manage-list" style="display: grid; gap: 10px; max-height: 380px; overflow-y: auto; padding-right: 5px;">
                    <!-- Templates loaded via JS -->
                </div>
            </div>
        </div>
    </div>

    <script src="../../assets/js/fingerprint-sdk.js"></script>

    <script>
        // --- RESTORED ANALYSIS FUNCTIONS ---

        function switchAnalysisSubTab(panelId, btnElement) {
            // Hide all analysis panels
            document.querySelectorAll('.analysis-panel').forEach(p => p.style.display = 'none');
            
            // Deactivate buttons
            const container = document.getElementById('analysis-view');
            if(container) {
                container.querySelectorAll('.sub-tab-btn').forEach(b => b.classList.remove('active'));
            }

            // Show selected panel
            const panel = document.getElementById(panelId);
            if(panel) {
                panel.style.display = 'block';
                panel.classList.add('active');
            }

            if(btnElement) btnElement.classList.add('active');

            // --- DATA LOADING HOOKS ---
            if(panelId === 'students-analysis-panel') {
                 // Return to student view
            }
            if(panelId === 'staff-analysis-panel') {
                loadStaffMonthlyAnalysis();
            }
        }

        async function loadStaffMonthlyAnalysis() {
            const m = document.getElementById('staff_analysis_month').value;
            const type = document.getElementById('staff_analysis_type').value; 
            const y = new Date().getFullYear();
            
            let resultContainer = document.getElementById('staff-report-result');
            if(!resultContainer) {
                resultContainer = document.createElement('div');
                resultContainer.id = 'staff-report-result';
                resultContainer.className = 'filter-card';
                resultContainer.style.marginTop = '20px';
                document.getElementById('staff-analysis-panel').appendChild(resultContainer);
            }
            
            try {
                const resCont = document.getElementById('staff-analysis-result-container');
                if(resCont) resCont.style.display = 'block';

                resultContainer.innerHTML = '<div style="text-align:center; padding:30px; opacity:0.6; font-weight:700;"><i class="fas fa-sync fa-spin"></i> GENERATING STAFF PERFORMANCE...</div>';
                
                let apiUrl = '';
                if(type === 'personal') {
                    apiUrl = `attendance_api.php?action=get_staff_attendance&month=${m}&year=${y}`;
                } else {
                    apiUrl = `attendance_api.php?action=get_staff_summary&month=${m}&year=${y}&staff_type=${type}`;
                }

                const res = await fetch(apiUrl);
                const data = await res.json();
                
                // 2. Render Based on Type
                if(type === 'personal') {
                     // PERSONAL VIEW (Daily list)
                     const monthName = document.getElementById('staff_analysis_month').options[document.getElementById('staff_analysis_month').selectedIndex].text;
                     let html = `<h4 style="margin:0 0 20px 0; font-size:16px; font-weight:900; text-transform:uppercase;">MY ATTENDANCE: ${monthName} ${y}</h4>`;
                     
                     if(Array.isArray(data)) {
                         html += `<div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap:10px;">`;
                         data.forEach(d => {
                             const dateObj = d.full_date ? new Date(d.full_date) : null;
                             const isFri = dateObj && dateObj.getDay() === 5;
                             const isSun = dateObj && dateObj.getDay() === 0;

                             let color = isFri ? '#2c52a0' : 'rgba(255,255,255,0.05)';
                             let text = isFri ? '#fff' : 'var(--text-main)';
                             let status = d.status || 'N/A';
                             
                             if(status === 'Present') { if(!isFri) { color='rgba(46, 204, 113, 0.1)'; text='#2ecc71'; } }
                             else if(status === 'Absent') { if(!isFri) { color='rgba(231, 76, 60, 0.1)'; text='#e74c3c'; } }
                             else if(status === 'Leave') { if(!isFri) { color='rgba(52, 152, 219, 0.1)'; text='#3498db'; } }
                             else if(status === 'Holiday' || status === 'PH' || isSun) { if(!isFri) { color='rgba(239, 68, 68, 0.1)'; text='#ef4444'; } }

                             html += `
                                <div class="name-chip ${status}" style="padding:10px; border-radius:8px; text-align:center; flex:1; min-width:120px; background:${color}; color:${text}; border:1px solid rgba(255,255,255,0.05);">
                                    <div style="font-size:10px; opacity:0.7; font-weight:700;">${d.name}</div>
                                    <div style="font-size:12px; font-weight:900; margin-top:4px;">${status}</div>
                                    ${d.time_in ? `<div style="font-size:9px; margin-top:4px; opacity:0.8;">${d.time_in}${d.time_out ? ' - '+d.time_out : ''}</div>` : ''}
                                </div>
                             `;
                         });
                         html += `</div>`;
                         
                         // Summary Stats
                         const p = data.filter(x=>x.status==='Present').length;
                         const a = data.filter(x=>x.status==='Absent').length;
                         const l = data.filter(x=>x.status==='Leave').length;
                         html += `
                            <div style="display:flex; gap:20px; margin-top:20px; justify-content:center; background:rgba(0,0,0,0.2); padding:15px; border-radius:12px;">
                                <div style="text-align:center;"><div style="font-size:20px; font-weight:900; color:#2ecc71;">${p}</div><div style="font-size:10px; font-weight:700; opacity:0.6;">PRESENT</div></div>
                                <div style="text-align:center;"><div style="font-size:20px; font-weight:900; color:#e74c3c;">${a}</div><div style="font-size:10px; font-weight:700; opacity:0.6;">ABSENT</div></div>
                                <div style="text-align:center;"><div style="font-size:20px; font-weight:900; color:#3498db;">${l}</div><div style="font-size:10px; font-weight:700; opacity:0.6;">LEAVE</div></div>
                            </div>
                         `;
                     } else {
                        html += `<div style="text-align:center; padding:20px;">No data available.</div>`;
                     }
                     resultContainer.innerHTML = html;

                } else {
                     // ADMIN SUMMARY VIEW (Table)
                     if(data.success && data.staff) {
                         let html = `
                            <div style="overflow-x:auto;">
                            <table class="report-table" style="width:100%; border-collapse:collapse;">
                                <thead>
                                    <tr style="text-align:left; background:#2c52a0; color:#fff;">
                                                                                 <th style="padding:10px; color:#fff !important;">NAME</th>
                                                                                 <th style="padding:10px; color:#fff !important;">ROLE</th>
                                                                                 <th style="padding:10px; text-align:center; color:#fff !important;">LEAVE (M)</th>
                                                                                 <th style="padding:10px; text-align:center; color:#fff !important;">ABSENT (M)</th>
                                        <th style="padding:10px; text-align:center; background:rgba(0,0,0,0.1); color:#fff;">TOTAL (YTD)</th>
                                    </tr>
                                </thead>
                                <tbody>
                         `;
                         
                          data.staff.forEach(s => {
                             html += `
                                <tr class="report-row">
                                    <td style="padding:12px; font-weight:800;">${s.name}</td>
                                    <td style="padding:12px; opacity:0.7; font-size:0.85em;">${s.designation || 'Staff'}</td>
                                    <td class="row-leave" style="padding:12px; text-align:center; font-weight:bold; color:#3498db; background:rgba(52,152,219,0.05);">${s.l_this}</td>
                                    <td class="row-absent" style="padding:12px; text-align:center; font-weight:bold; color:#ef4444; background:rgba(239, 68, 68, 0.05);">${s.a_this}</td>
                                    <td style="padding:12px; text-align:center; font-weight:900; background:rgba(44, 82, 160, 0.1); color:#fff;">L:${s.l_total} / A:${s.a_total}</td>
                                </tr>
                             `;
                          });
                         html += `</tbody></table></div>`;
                         resultContainer.innerHTML = html;
                     } else {
                         resultContainer.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171;">${data.error || 'No records returned.'}</div>`;
                     }
                }

            } catch (e) {
                console.error(e);
                resultContainer.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171;">Error: ${e.message}</div>`;
            }
        }

        function shareStaffLedgerWhatsApp() {
            const container = document.getElementById('staff-report-result');
            if(!container) return;
            const table = container.querySelector('table');
            if(!table) { alert('No table found to share.'); return; }

            const monthSel = document.getElementById('staff_analysis_month');
            const monthName = monthSel.options[monthSel.selectedIndex].text;

            let message = `*📊 STAFF PERFORMANCE LEDGER*\n`;
            message += `*Period:* ${monthName}\n`;
            message += `----------------------------\n`;
            
            const rows = table.querySelectorAll('tbody tr');
            rows.forEach(row => {
                const cols = row.querySelectorAll('td');
                if(cols.length >= 5) {
                    const name = cols[0].innerText.trim();
                    const leaves = cols[2].innerText.trim();
                    const absents = cols[3].innerText.trim();
                    message += `• *${name}*: L:${leaves}, A:${absents}\n`;
                }
            });
            
            message += `----------------------------\n`;
            message += `_Sent via Attendance Intelligence_`;

            const encodedMsg = encodeURIComponent(message);
            window.open(`https://api.whatsapp.com/send?text=${encodedMsg}`, '_blank');
        }
        function openBiometricManagerBySelection() {
            const userId = document.getElementById('enroll_user_sel').value;
            const userName = document.getElementById('enroll_user_sel').options[document.getElementById('enroll_user_sel').selectedIndex].text;
            if(!userId || userId === "") {
                alert('Please select a personnel first to manage their biometrics.');
                return;
            }
            openBiometricManager(userId, userName);
        }
    </script>
    <?php include 'biometric_manager.php'; ?>
    
    <!-- Request Leave Modal -->
    <div id="leaveModal" class="fixed inset-0 bg-black/50 hidden z-50 flex items-center justify-center backdrop-blur-sm" style="z-index: 10001;">
        <div class="bg-white dark:bg-slate-800 w-full max-w-md rounded-2xl p-6 shadow-2xl premium-modal">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-xl font-black text-gray-900 dark:text-white">Request Leave</h3>
                <button onclick="document.getElementById('leaveModal').classList.add('hidden')" class="text-gray-500 hover:text-red-500"><i class="fas fa-times"></i></button>
            </div>
            <form id="leaveForm" onsubmit="submitLeave(event)">
                <input type="hidden" name="action" value="request_leave">
                <!-- Dropdown optional if we wanted to select who we request for. For staff or student self, we don't need target_user_id dropdown right now. -->
                <div class="mb-4">
                    <label class="block text-sm font-bold mb-1 opacity-70 text-gray-800 dark:text-gray-200">Start Date</label>
                    <input type="date" name="start_date" required class="w-full p-3 rounded-xl border border-gray-200 dark:border-slate-600 focus:outline-none focus:border-blue-500 bg-white dark:bg-slate-700 text-gray-900 dark:text-white">
                </div>
                <div class="mb-4">
                    <label class="block text-sm font-bold mb-1 opacity-70 text-gray-800 dark:text-gray-200">End Date</label>
                    <input type="date" name="end_date" required class="w-full p-3 rounded-xl border border-gray-200 dark:border-slate-600 focus:outline-none focus:border-blue-500 bg-white dark:bg-slate-700 text-gray-900 dark:text-white">
                </div>
                <div class="mb-4">
                    <label class="block text-sm font-bold mb-1 opacity-70 text-gray-800 dark:text-gray-200">Reason</label>
                    <textarea name="reason" rows="3" required class="w-full p-3 rounded-xl border border-gray-200 dark:border-slate-600 focus:outline-none focus:border-blue-500 bg-white dark:bg-slate-700 text-gray-900 dark:text-white"></textarea>
                </div>
                <button type="submit" class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 rounded-xl transition-colors">
                    Submit Request
                </button>
            </form>
        </div>
    </div>
    
    <script>
    async function submitLeave(e) {
        e.preventDefault();
        const fd = new FormData(document.getElementById('leaveForm'));
        if(window.editingLeaveId) {
            fd.append('action', 'update_my_leave');
            fd.append('leave_id', window.editingLeaveId);
        } else {
            fd.append('action', 'request_leave');
        }
        try {
            const res = await fetch('api_leave_request.php', { method: 'POST', body: fd });
            const data = await res.json();
            if (data.success) {
                alert(window.editingLeaveId ? 'Leave request updated successfully.' : 'Leave request submitted successfully.');
                document.getElementById('leaveModal').classList.add('hidden');
                document.getElementById('leaveForm').reset();
                window.editingLeaveId = null;
                
                let subBtn = document.querySelector('#leaveModal button[type="submit"]');
                if(subBtn) {
                    subBtn.innerHTML = 'Submit Request';
                    subBtn.classList.add('bg-blue-600', 'hover:bg-blue-700');
                    subBtn.classList.remove('bg-emerald-600', 'hover:bg-emerald-700');
                    let modalH3 = document.querySelector('#leaveModal h3');
                    if(modalH3) modalH3.innerText = 'Request Leave';
                }

                if(typeof loadMyLeaves === 'function') loadMyLeaves();
            } else {
                alert(data.message || 'Error processing request.');
            }
        } catch (error) {
            console.error(error);
            alert('Cloud synchronization failed.');
        }
    }

    async function deleteMyLeave(id) {
        if(!confirm('Are you sure you want to delete this pending leave request?')) return;
        const fd = new FormData();
        fd.append('action', 'delete_my_leave');
        fd.append('leave_id', id);
        try {
            const res = await fetch('api_leave_request.php', { method: 'POST', body: fd });
            const data = await res.json();
            if(data.success) {
                loadMyLeaves();
            } else {
                alert(data.message || 'Error deleting request');
            }
        } catch(e) {
            alert('Connection error');
        }
    }

    window.editingLeaveId = null;
    function editMyLeave(id, start_date, end_date, reason) {
        window.editingLeaveId = id;
        document.getElementById('leaveModal').classList.remove('hidden');
        document.querySelector('#leaveModal h3').innerText = 'Edit Leave Request';
        document.querySelector('#leaveModal input[name="start_date"]').value = start_date;
        document.querySelector('#leaveModal input[name="end_date"]').value = end_date;
        document.querySelector('#leaveModal textarea[name="reason"]').value = reason;
        
        let subBtn = document.querySelector('#leaveModal button[type="submit"]');
        if(subBtn) {
            subBtn.innerHTML = '<i class="fas fa-save mr-1"></i> Update Request';
            subBtn.classList.remove('bg-blue-600', 'hover:bg-blue-700');
            subBtn.classList.add('bg-emerald-600', 'hover:bg-emerald-700');
        }
    }

    async function loadMyLeaves() {
        const container = document.getElementById('my-leaves-container');
        if(!container) return;
        container.innerHTML = '<div style="text-align:center; padding:50px; opacity:0.6;"><i class="fas fa-spinner fa-spin fa-2x"></i><p style="margin-top:10px; font-weight:800; font-size:12px;">LOADING YOUR REQUESTS...</p></div>';
        
        try {
            const fd = new FormData();
            fd.append('action', 'get_my_leaves');
            const res = await fetch('api_leave_request.php', { method: 'POST', body: fd });
            const data = await res.json();
            
            if(!data.success || !data.leaves || data.leaves.length === 0) {
                container.innerHTML = `<div style="text-align:center; padding:60px; opacity:0.3;">
                    <i class="fas fa-calendar-times fa-3x mb-3"></i>
                    <p style="font-weight:900; font-size:14px; text-transform:uppercase;">No Leave Requests Found</p>
                </div>`;
                return;
            }

            let html = '<div style="display:flex; flex-direction:column; gap:12px;">';
            data.leaves.forEach(l => {
                let statusColor = '#f39c12'; // pending
                if(l.status === 'approved') statusColor = '#2ecc71';
                else if(l.status === 'rejected') statusColor = '#e74c3c';
                else if(l.status === 'forwarded_to_parent') statusColor = '#3498db';

                html += `
                    <div class="spongy-card" style="padding: 15px 20px; border-radius: 16px; background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.05); display: flex; align-items: center; justify-content: space-between; gap: 20px;">
                        <div style="display: flex; align-items: center; gap: 20px; flex: 1;">
                            <div style="background:${statusColor}22; color:${statusColor}; width:45px; height:45px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-size:18px;">
                                <i class="fas ${l.status==='approved'?'fa-check-circle':(l.status==='rejected'?'fa-times-circle':'fa-clock')}"></i>
                            </div>
                            <div>
                                <div style="font-weight: 900; font-size: 14px; color: var(--modal-text); text-transform:uppercase;">
                                    ${l.status}
                                </div>
                                <div style="font-size: 11px; font-weight: 700; opacity: 0.6;">
                                    ${l.start_date} <i class="fas fa-arrow-right mx-1" style="font-size:9px;"></i> ${l.end_date}
                                </div>
                            </div>
                            <div style="flex:1; font-size: 13px; font-weight: 600; opacity: 0.8; padding-left:20px; border-left:1px solid rgba(0,0,0,0.05);">
                                ${l.reason}
                            </div>
                        </div>
                        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:10px;">
                            ${(l.status === 'pending' || l.status === 'forwarded_to_parent') ? `
                                <div style="display:flex; gap:8px;">
                                    <button onclick="editMyLeave(${l.id}, '${l.start_date}', '${l.end_date}', \`${l.reason.replace(/"/g, '&quot;').replace(/'/g, "\\'")}\`)" style="background:transparent; border:1px solid rgba(52, 152, 219, 0.4); color:#3498db; border-radius:6px; padding:4px 12px; font-size:9px; font-weight:900; cursor:pointer;" title="Edit Request"><i class="fas fa-edit"></i> EDIT</button>
                                    <button onclick="deleteMyLeave(${l.id})" style="background:transparent; border:1px solid rgba(231, 76, 60, 0.4); color:#e74c3c; border-radius:6px; padding:4px 12px; font-size:9px; font-weight:900; cursor:pointer;" title="Delete Request"><i class="fas fa-trash"></i> DELETE</button>
                                </div>
                            ` : ''}
                            <div style="font-size: 10px; font-weight: 900; opacity: 0.4; text-transform: uppercase;">
                                Submitted: ${new Date(l.created_at).toLocaleDateString()}
                            </div>
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            container.innerHTML = html;
        } catch (error) {
            console.error(error);
            container.innerHTML = '<div style="text-align:center; padding:30px; color:#e74c3c; font-weight:bold;">Connection error.</div>';
        }
    }

    window.allPendingAppeals = [];
    window.currentAppealTab = 'staff';

    function switchAppealTab(tab) {
        window.currentAppealTab = tab;
        const sBtn = document.getElementById('tab-staff-appeal');
        const uBtn = document.getElementById('tab-student-appeal');
        
        if(tab === 'staff') {
            sBtn.style.background = '#3498db';
            sBtn.style.color = 'white';
            sBtn.style.boxShadow = '0 4px 12px rgba(52, 152, 219, 0.4)';
            sBtn.style.opacity = '1';
            
            uBtn.style.background = 'transparent';
            uBtn.style.color = 'inherit';
            uBtn.style.boxShadow = 'none';
            uBtn.style.opacity = '0.6';
        } else {
            uBtn.style.background = '#3498db';
            uBtn.style.color = 'white';
            uBtn.style.boxShadow = '0 4px 12px rgba(52, 152, 219, 0.4)';
            uBtn.style.opacity = '1';
            
            sBtn.style.background = 'transparent';
            sBtn.style.color = 'inherit';
            sBtn.style.boxShadow = 'none';
            sBtn.style.opacity = '0.6';
        }
        renderAppeals();
    }

    async function loadLeaveAppeals() {
        const container = document.getElementById('leave-appeals-container');
        if(!container) return;
        container.innerHTML = '<div style="text-align:center; padding:50px; opacity:0.6;"><i class="fas fa-spinner fa-spin fa-2x"></i><p style="margin-top:10px; font-weight:800; font-size:12px;">FETCHING APPEALS...</p></div>';
        
        try {
            const fd = new FormData();
            fd.append('action', 'get_pending_leaves');
            const res = await fetch('api_leave_request.php', { method: 'POST', body: fd });
            const data = await res.json();
            window.allPendingAppeals = (data.success && data.leaves) ? data.leaves : [];
            renderAppeals();
        } catch (error) {
            console.error(error);
            container.innerHTML = '<div style="text-align:center; padding:30px; color:#e74c3c; font-weight:bold;">Connection error.</div>';
        }
    }

    function renderAppeals() {
        const container = document.getElementById('leave-appeals-container');
        if(!container) return;

        const filtered = window.allPendingAppeals.filter(l => {
            if(window.currentAppealTab === 'student') return l.role === 'student';
            return l.role !== 'student';
        });
        
        if(filtered.length === 0) {
            container.innerHTML = `<div style="text-align:center; padding:60px; opacity:0.3;">
                <i class="fas fa-inbox fa-3x mb-3"></i>
                <p style="font-weight:900; font-size:14px; text-transform:uppercase;">No Pending ${window.currentAppealTab} Appeals</p>
            </div>`;
            return;
        }

        let html = '<div style="display:flex; flex-direction:column; gap:10px;">';
        filtered.forEach(l => {
            let statusBadge = '';
            if(l.status === 'forwarded_to_parent') {
                statusBadge = `<span style="font-size: 9px; font-weight: 900; background: #f39c12; color: #fff; padding: 3px 10px; border-radius: 20px; text-transform:uppercase;"><i class="fas fa-hourglass-half"></i> Waiting Parent</span>`;
            }
            
            let fwdButton = (l.role === 'student' && l.status !== 'forwarded_to_parent') ? 
                `<button onclick="handleLeaveAppeal(${l.id}, 'forward')" title="Forward to Parent" style="background:rgba(243, 156, 18, 0.1) !important; color:#f39c12 !important; border:1px solid rgba(243, 156, 18, 0.2) !important; padding:8px 12px !important; border-radius:10px !important; font-weight:900 !important; cursor:pointer !important; font-size: 11px !important;"><i class="fas fa-share-alt"></i></button>` : '';

            html += `
                <div class="spongy-card" style="padding: 12px 20px; border-radius: 16px; background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.05); display: flex; align-items: center; justify-content: space-between; gap: 20px; transition: transform 0.2s;">
                    <div style="display: flex; align-items: center; gap: 20px; flex: 1; overflow: hidden;">
                        <div style="min-width: 140px;">
                            <div style="font-weight: 900; font-size: 14px; color: var(--modal-text);">${l.user_name || 'User ' + l.user_id}</div>
                            <div style="font-size: 10px; color: #3498db; font-weight: 800; text-transform: uppercase;">${l.role}</div>
                        </div>
                        <div style="font-size: 12px; font-weight: 700; opacity: 0.7; min-width: 170px;">
                            <i class="far fa-calendar-alt text-blue-500 mr-1"></i> ${l.start_date} <i class="fas fa-long-arrow-alt-right mx-1 opacity-30"></i> ${l.end_date}
                        </div>
                        <div style="font-size: 13px; font-weight: 600; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 400px; opacity: 0.8;" title="${l.reason}">
                            ${l.reason}
                        </div>
                        ${statusBadge}
                    </div>
                    <div style="display:flex; gap:8px; flex-shrink: 0;">
                        <button onclick="handleLeaveAppeal(${l.id}, 'approve')" style="background:#2ecc71 !important; color:white !important; border:none !important; padding:8px 18px !important; border-radius:10px !important; font-weight:900 !important; cursor:pointer !important; font-size: 11px !important; text-transform: uppercase; box-shadow: 0 4px 10px rgba(46, 204, 113, 0.2) !important;"><i class="fas fa-check mr-1"></i> Approve</button>
                        <button onclick="handleLeaveAppeal(${l.id}, 'reject')" style="background:#e74c3c !important; color:white !important; border:none !important; padding:8px 18px !important; border-radius:10px !important; font-weight:900 !important; cursor:pointer !important; font-size: 11px !important; text-transform: uppercase; box-shadow: 0 4px 10px rgba(231, 76, 60, 0.2) !important;"><i class="fas fa-times mr-1"></i> Reject</button>
                        ${fwdButton}
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    }

    async function handleLeaveAppeal(id, actionCode) {
        if (!confirm(`Are you sure you want to ${actionCode} this leave request?`)) return;
        try {
            const fd = new FormData();
            fd.append('action', 'admin_action');
            fd.append('leave_id', id);
            fd.append('status_action', actionCode);
            const res = await fetch('api_leave_request.php', { method: 'POST', body: fd });
            const data = await res.json();
            if (data.success) {
                alert('Action synchronized successfully!');
                loadLeaveAppeals();
            } else {
                alert(data.message || 'Error processing request.');
            }
        } catch (error) {
            console.error(error);
            alert('Cloud synchronization failed.');
        }
    }

    async function loadDetailedSheet(context = 'student') {
        let apiUrl = '';
        let container = null;
        let month = '';
        const year = new Date().getFullYear();

        if (context === 'staff') {
            month = document.getElementById('staff_analysis_month').value;
            const staffType = document.getElementById('staff_analysis_type').value;
            apiUrl = `attendance_api.php?action=get_monthly_detailed_sheet&type=staff&staff_type=${staffType}&month=${month}&year=${year}`;
            container = document.getElementById('staff-report-result');
        } else {
            const val = document.getElementById('avg_class_sel').value;
            if(!val) { Swal.fire('Error', 'Please choose a class to analyze.', 'error'); return; }
            const [cid, sid] = val.split('|');
            month = document.getElementById('avg_month').value;
            apiUrl = `attendance_api.php?action=get_monthly_detailed_sheet&class_id=${cid}&section_id=${sid}&month=${month}&year=${year}`;
            container = document.getElementById('report-result');
        }

        if (context === 'staff') {
            const resCont = document.getElementById('staff-analysis-result-container');
            if(resCont) resCont.style.display = 'block';
        } else {
            const resCont = document.getElementById('analysis-result-container');
            if(resCont) resCont.style.display = 'block';
        }

        container.innerHTML = '<div style="text-align:center; padding:30px; opacity:0.4; font-weight:700;"><i class="fas fa-sync fa-spin"></i> GENERATING DETAILED CALENDAR SHEET...</div>';
        
        try {
            const res = await fetch(apiUrl);
            const data = await res.json();
            
            if (!data.success) throw new Error(data.error || 'Server error');

            let html = '<div class="smart-ledger-container" style="overflow-x:auto; padding: 0 !important;"><table class="ledger-table" style="font-size: 10px; margin:0; border-radius:0; width:100%; table-layout: fixed;">';
            html += '<thead><tr style="background:#2c52a0; color:#fff;"><th style="font-size:9px; padding: 10px 2px !important; width:40px; text-align:center; color:#fff !important;">R.NO</th><th style="text-align:left; width: 140px; padding: 10px 5px !important; color:#fff !important;">NAME</th>';
            
            // Header for Days/Names
            for(let i=1; i<=data.days; i++) {
                const dateObj = new Date(year, parseInt(month)-1, i);
                const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' }).charAt(0);
                const isSunday = dateObj.getDay() === 0;
                const isFriday = dateObj.getDay() === 5;
                
                let bg = 'rgba(255,255,255,0.05)';
                let color = '#fff';
                if(isFriday) bg = '#2ecc71';
                if(isSunday) bg = '#e74c3c';
                
                html += `<th style="width: 24px; padding: 8px 0 !important; font-size: 9px; text-align:center; border-left: 1px solid rgba(255,255,255,0.1); background:${bg}; color:${color};">
                            <div style="font-weight: 800; font-size: 7px; opacity:0.8;">${dayName}</div>
                            <div style="font-weight: 900; font-size: 10px;">${i}</div>
                         </th>`;
            }

            // Summary Header Columns
            const summaryHeaderStyle = "width: 32px; text-align:center; font-size:9px; font-weight:900; padding: 10px 0 !important; border-left:1px solid rgba(255,255,255,0.1); word-wrap: break-word; white-space: normal;";
            
            if (context === 'student') {
                html += `<th style="${summaryHeaderStyle} background:#3498db; color:#fff;">ATT BRT</th>
                         <th style="${summaryHeaderStyle} background:#2ecc71; color:#fff;">TM ATT</th>
                         <th style="${summaryHeaderStyle} background:#2c3e50; color:#fff;">TTL ATT</th>
                         <th style="${summaryHeaderStyle} background:#e67e22; color:#fff;">ABS</th>
                         <th style="${summaryHeaderStyle} background:#8e44ad; color:#fff; width:36px;">FINE</th>`;
            } else {
                html += `<th style="${summaryHeaderStyle} background:rgba(46, 204, 113, 0.2); color:#2ecc71;">P</th>
                         <th style="${summaryHeaderStyle} background:rgba(239, 68, 68, 0.2); color:#ef4444;">A</th>
                         <th style="${summaryHeaderStyle} background:rgba(52, 152, 219, 0.2); color:#3498db;">L</th>
                         <th style="${summaryHeaderStyle} background:rgba(239, 68, 68, 0.2); color:#ef4444;">H</th>
                         <th style="${summaryHeaderStyle} background:rgba(255, 255, 255, 0.1); color:#fff; width:35px;">%</th>`;
            }

            html += '</tr></thead><tbody>';

            const personnel = (context === 'staff' ? data.staff : data.students) || [];
            window.lastPersonnelData = data; // Cache data for individual editor

            personnel.forEach((s, sIdx) => {
                const rno = context === 'staff' ? (s.sno || 'Staff') : (s.class_no || '-');
                html += `<tr>
                    <td style="font-size:9px; padding: 3px 2px !important; border-bottom: 1px solid rgba(0,0,0,0.05); text-align:center;">${rno}</td>
                    <td onclick="openAttendanceEditor(${s.id}, '${s.name.replace(/'/g, "\\'")}', '${month}', '${year}', '${context}')" 
                        style="text-align:left; font-weight: 700; font-size:10px; padding: 3px 5px !important; border-bottom: 1px solid rgba(0,0,0,0.05); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor:pointer; color:var(--primary);">
                        ${s.name}
                    </td>`;
                
                let tm_score = 0;
                let abs_count = 0;
                let p_count = 0, a_count = 0, l_count = 0, h_count = 0;

                for(let i=1; i<=data.days; i++) {
                    const status = (s.attendance && s.attendance[i]) ? s.attendance[i] : '';
                    const dateObj = new Date(year, parseInt(month)-1, i);
                    const isSunday = dateObj.getDay() === 0;
                    const isFriday = dateObj.getDay() === 5;
                    const dateStr = `${year}-${month.toString().padStart(2,'0')}-${i.toString().padStart(2,'0')}`;

                    // Update Counters for Staff-centric view
                    if(status === 'Present') p_count++;
                    else if(status === 'Absent') a_count++;
                    else if(status === 'Leave') l_count++;
                    else if(status === 'Holiday' || status === 'PH' || isSunday) h_count++;

                    // Public Holiday Merging Logic (Full-Table Rowspan)
                    let publicHoliday = (data.holidays || []).find(h => h.from_date <= dateStr && h.to_date >= dateStr);
                    if (publicHoliday) {
                    // Check if we already handled this holiday in this table
                    const holidayKey = `${publicHoliday.id || publicHoliday.name}_${publicHoliday.from_date}`;
                    
                    // IF FIRST ROW: Create the big cell spanning ALL rows
                    if (sIdx === 0) {
                        let h_colspan = 1;
                        let nextDay = i + 1;
                        while(nextDay <= data.days) {
                            let nextDateStr = `${year}-${month.toString().padStart(2,'0')}-${nextDay.toString().padStart(2,'0')}`;
                            // Peek at anyone's attendance for this day to see if it's holiday
                            let nextIsHoliday = publicHoliday.to_date >= nextDateStr; 
                            if (nextIsHoliday) {
                                h_colspan++;
                                nextDay++;
                            } else break;
                        }

                        let phName = publicHoliday.name.toUpperCase();
                        // Repeat phName if the list is long
                        let repeatCount = Math.max(1, Math.floor(personnel.length / 10));
                        let displayPh = phName;
                        for(let r=1; r<repeatCount; r++) displayPh += " &nbsp;&nbsp; " + phName;

                        html += `<td rowspan="${personnel.length}" colspan="${h_colspan}" 
                                    onclick="openEditPHModal(${publicHoliday.id}, '${publicHoliday.name.replace(/'/g, "\\'")}', '${publicHoliday.from_date}', '${publicHoliday.to_date}', '${publicHoliday.class_ids || ''}')"
                                    style="background:rgba(231, 76, 60, 0.1); color:#e74c3c; font-size:16px; font-weight:900; text-align:center; border:1px solid rgba(0,0,0,0.03); vertical-align:middle; cursor:pointer;" title="Click to Edit/Delete Holiday">
                                    <div style="writing-mode: vertical-rl; transform: rotate(180deg); margin:auto; white-space:nowrap; padding: 10px 0; letter-spacing: 2px;">${displayPh}</div>
                                 </td>`;
                        i += (h_colspan - 1);
                        continue;
                    } else {
                        // SUBSEQUENT ROWS: Skip the space taken by the rowspan
                        let h_colspan = 1;
                        let nextDay = i + 1;
                        while(nextDay <= data.days) {
                            let nextDateStr = `${year}-${month.toString().padStart(2,'0')}-${nextDay.toString().padStart(2,'0')}`;
                            if (publicHoliday.to_date >= nextDateStr) {
                                h_colspan++;
                                nextDay++;
                            } else break;
                        }
                        i += (h_colspan - 1);
                        continue;
                    }
                }

                    // Score Logic: Present counts 2 except Friday (counts 1)
                    if(status === 'Present') {
                        tm_score += (isFriday ? 1 : 2);
                    }
                    if(status === 'Absent') abs_count++;

                    // Coloring cell based on Day
                    let bg = '';
                    let color = 'inherit';
                    if(isFriday) bg = 'rgba(46, 204, 113, 0.15)'; 
                    if(isSunday) bg = 'rgba(231, 76, 60, 0.15)';

                    let symbol = '';
                    if(status === 'Present') { symbol = 'P'; color = '#27ae60'; }
                    else if(status === 'Absent') { symbol = 'A'; color = '#c0392b'; }
                    else if(status === 'Leave') { symbol = 'L'; color = '#2980b9'; }
                    else if(status === 'Struck Off') { symbol = 'SO'; color = '#000'; bg = 'rgba(0,0,0,0.1)'; }
                    else if(status === 'Holiday' || status === 'PH') { 
                        symbol = 'H'; 
                        color = '#e67e22'; 
                        bg = 'rgba(231, 76, 60, 0.15)'; // Match print view holiday color
                    }
                    else if(isSunday) { symbol = 'S'; color = '#c0392b'; }

                    html += `<td style="color: ${color}; font-weight: 800; background: ${bg}; border-right: 1px solid rgba(0,0,0,0.02); border-bottom: 1px solid rgba(0,0,0,0.05); padding: 3px 0 !important; text-align:center;">${symbol}</td>`;
                }

                const summaryCellStyle = "font-size:10px; font-weight:900; text-align:center; border-left:1px solid rgba(0,0,0,0.05); border-bottom: 1px solid rgba(0,0,0,0.05); padding: 3px 2px !important;";

                if (context === 'student') {
                    const att_brt = s.brought_attendance || 0;
                    const ttl_att = att_brt + tm_score;
                    const fine_rate = parseFloat(s.absent_fine_rate || data.fine_rate || 0);
                    const fine = Math.round(abs_count * fine_rate);
                    html += `<td style="${summaryCellStyle}">${att_brt}</td>
                             <td style="${summaryCellStyle}">${tm_score}</td>
                             <td style="${summaryCellStyle}">${ttl_att}</td>
                             <td style="${summaryCellStyle} color:#c0392b;">${abs_count}</td>
                             <td style="${summaryCellStyle} color:#8e44ad;">${fine}</td>`;
                } else {
                    const totalWorking = p_count + a_count + l_count;
                    const percentage = totalWorking > 0 ? Math.round((p_count / totalWorking) * 100) : 0;
                    html += `<td style="${summaryCellStyle} background:rgba(46, 204, 113, 0.05); color:#2ecc71;">${p_count}</td>
                             <td style="${summaryCellStyle} background:rgba(239, 68, 68, 0.05); color:#ef4444;">${a_count}</td>
                             <td style="${summaryCellStyle} background:rgba(52, 152, 219, 0.05); color:#3498db;">${l_count}</td>
                             <td style="${summaryCellStyle} background:rgba(239, 68, 68, 0.05); color:#ef4444;">${h_count}</td>
                             <td style="${summaryCellStyle} background:rgba(255, 255, 255, 0.05); font-weight:900;">${percentage}%</td>`;
                }

                html += '</tr>';
            });

            html += '</tbody></table></div>';
            container.innerHTML = html;

        } catch(e) {
            console.error(e);
            container.innerHTML = `<div style="text-align:center; padding:20px; color:#f87171; font-weight:800;">ERROR: ${e.message}</div>`;
        }
    }

    // Auto-update when dropdowns change
    function loadConsolidatedAnalysis() {
        loadDetailedSheet();
    }


    function closeLedger() {
        const container = document.getElementById('report-result');
        if(container) {
            container.innerHTML = '<div style="text-align:center; padding:40px; opacity:0.3; font-weight:700;">Choose parameters to load records</div>';
        }
        const sInput = document.getElementById('ledgerSearchInput');
        if(sInput) sInput.value = '';
    }

    function filterLedger() {
        const input = document.getElementById('ledgerSearchInput');
        if(!input) return;
        const filter = input.value.toUpperCase();
        const container = document.getElementById('report-result');
        if (!container) return;
        const table = container.querySelector('table');
        if (!table) return;
        const tr = table.getElementsByTagName('tr');

        for (let i = 1; i < tr.length; i++) {
            // NAME column is the second column (index 1)
            const nameTd = tr[i].getElementsByTagName('td')[1];
            if (nameTd) {
                const txtValue = nameTd.textContent || nameTd.innerText;
                if (txtValue.toUpperCase().indexOf(filter) > -1) {
                    tr[i].style.display = "";
                } else {
                    tr[i].style.display = "none";
                }
            }
        }
    }

    window.printMonthlyLedger = function(divId, title) {
        const container = document.getElementById(divId);
        const table = container.querySelector('table');
        if (!table) {
            alert('Record data is not loaded yet. Please wait or reload.');
            return;
        }

        const rows = Array.from(table.querySelectorAll('tbody tr'));
        const headers = Array.from(table.querySelectorAll('thead th')).map(th => th.textContent.trim().toUpperCase());
        
        // Robust identification indexes
        let tmAttIdx = -1;
        let attBrtIdx = -1;
        headers.forEach((h, idx) => {
            const clean = h.replace(/\s+/g, ' ').trim();
            if (clean.includes('TM ATT')) tmAttIdx = idx;
            if (clean.includes('ATT BRT') || clean.includes('ATT.BRT')) attBrtIdx = idx;
        });

        const day1Idx = 2; 
        const summaryStartIdx = headers.findIndex(h => (h.includes('ATT BRT') || h.includes('TM ATT') || h === 'P'));
        const lastDayIdx = (summaryStartIdx !== -1 && summaryStartIdx > day1Idx) ? summaryStartIdx - 1 : headers.length - 1;

        let startOfMonth = 0;
        let newAdmissions = 0;
        let struckOff = 0;
        let sumTmAtt = 0;
        let struckOffNos = [];

        rows.forEach(row => {
            const cells = row.cells;
            if (cells && cells.length > day1Idx) {
                const day1 = cells[day1Idx].textContent.trim();
                let hasAnyAttendance = false;
                let isSO = false;
                
                for(let i = day1Idx; i <= lastDayIdx; i++) {
                    if(!cells[i]) continue;
                    const txt = cells[i].textContent.trim().toUpperCase();
                    if (txt === 'SO') isSO = true;
                    if (txt === 'P' || txt === 'A' || txt === 'L') hasAnyAttendance = true;
                }
                
                let hasHistory = false;
                if (attBrtIdx !== -1 && cells[attBrtIdx]) {
                   const val = parseFloat(cells[attBrtIdx].textContent.trim()) || 0;
                   if (val > 0) hasHistory = true;
                }

                if (isSO) {
                    struckOff++;
                    struckOffNos.push(cells[0].textContent.trim());
                    if (hasHistory || day1 !== '') startOfMonth++; else if(hasAnyAttendance) newAdmissions++;
                } else if (hasHistory || (day1 !== '' && row.rowIndex <= 14)) {
                    startOfMonth++;
                } else if (hasAnyAttendance) {
                    newAdmissions++;
                }

                if (tmAttIdx !== -1 && cells[tmAttIdx]) {
                    const rowTmAtt = parseFloat(cells[tmAttIdx].innerText || cells[tmAttIdx].textContent) || 0;
                    sumTmAtt += rowTmAtt;
                }
            }
        });

        const totalActive = startOfMonth + newAdmissions;
        // Current month working days (estimated from grid)
        const workingDays = (lastDayIdx - day1Idx + 1) || 25;
        // User requested: Sum / Total Active Students
        const meanRatio = totalActive > 0 ? (sumTmAtt / totalActive).toFixed(1) : 0;
        const struckOffText = struckOffNos.length > 0 ? `(${struckOffNos.join(', ')})` : '';

        const content = container.innerHTML;
        const instName = "<?php echo addslashes($inst['name'] ?? 'Academic Institution'); ?>";
        const instAddr = "<?php echo addslashes($inst['address'] ?? 'Official Academic Archive'); ?>";
        const instLogo = "../../<?php echo addslashes($inst['logo_path'] ?? 'assets/images/logo-placeholder.png'); ?>";

        // Resolve Logo Path to Absolute - using parent window context
        let absoluteLogo = instLogo;
        try {
            const link = document.createElement('a');
            link.href = instLogo;
            absoluteLogo = link.href;
        } catch(e) {}

        const win = window.open('', '_blank');
        try {
            win.document.write(`
                <html>
                <head>
                    <title>PRINT REPORT - ${title}</title>
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700;800;900&display=swap" rel="stylesheet">
                    <style>
                        body { font-family: 'Inter', sans-serif; margin: 0; padding: 0.1in; background: white; color: #000; }
                        * { print-color-adjust: exact !important; -webkit-print-color-adjust: exact !important; }
                        
                        .report-header { 
                            display:flex; justify-content:space-between; align-items:flex-start; 
                            margin-bottom:10px; position:relative; min-height: 100px; 
                            border-bottom: 2px solid #2c52a0; overflow: hidden;
                        }
                        
                        .print-container { width: 100%; overflow: visible; }
                        .ledger-table { width: 100%; border-collapse: collapse; font-size: 8px; table-layout: fixed; }
                        .ledger-table th, .ledger-table td { border: 1px solid #34495e; padding: 2px 0; text-align: center; vertical-align: middle; word-wrap: break-word; font-weight:700 !important; }
                        .ledger-table th { color: white !important; font-weight: 900 !important; font-size: 7px; background-color: #2c52a0 !important; }
                        .ledger-table td:nth-child(2) { text-align: left; padding-left: 5px; font-weight: 800; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
                        
                        @media print {
                            @page { size: landscape; margin: 0.1in; }
                            .no-print { display: none !important; }
                            .print-footer {
                                margin-top: 15px;
                                width: 100%;
                                background: white;
                                padding: 10px 0;
                                border-top: 1px dashed #eee;
                                page-break-inside: avoid;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="report-header">
                        <div style="position:absolute; top:0; left:0; width:65%; height:110px; background:#2c52a0; clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%); z-index:1;"></div>
                        <div style="position:relative; z-index:10; padding:20px 0 0 30px; color:white; flex:1.5;">
                            <h1 style="margin:0; font-size:22px; font-weight:900; text-transform:uppercase;">${instName}</h1>
                            <p style="margin:4px 0 0; font-size:10px; font-weight:400; opacity:0.8; letter-spacing:1.5px;">OFFICIAL ACADEMIC ARCHIVE</p>
                        </div>
                        <div style="position:relative; z-index:10; padding:15px 30px 0 0; text-align:right; flex:2;">
                            <h2 style="margin:0; font-size:18px; font-weight:900; color:#2c52a0; text-transform:uppercase; line-height:1.1; display:inline-block;">${title.replace('Attendance', '<br>Attendance')}</h2>
                            <p style="margin:2px 0 0; font-size:8px; color:#64748b; font-weight:700;">${instAddr}</p>
                        </div>
                        <div style="position:relative; z-index:10; padding:10px 20px 0 0;">
                            <img src="${absoluteLogo}" style="height:70px; object-fit:contain;" onerror="this.onerror=null; this.style.display='none';">
                        </div>
                    </div>

                    <div class="print-container">${content.replace(/no-print/g, 'hidden')}</div>

                    <div class="print-footer">
                        <div style="font-size:9px; font-weight:800; border:1px solid #2c52a0; padding:6px; border-radius:6px; background:#f8fbff; text-transform:uppercase; margin-bottom:15px; display:flex; justify-content:space-around; align-items:center;">
                            <span>START OF MONTH: <b>${startOfMonth}</b></span>
                            <span>NEW ADMISSIONS: <b>${newAdmissions}</b></span>
                            <span>STRUCK OFF: <b>${struckOff} ${struckOffText}</b></span>
                            <span>ACTIVE NOW: <b>${totalActive}</b></span>
                            <span>SUM OF TM ATT: <b>${sumTmAtt}</b></span>
                            <span>RATIO: <b>${meanRatio}</b></span>
                        </div>
                        <div style="display:flex; justify-content:space-between; padding: 0 50px;">
                            <div style="text-align:center; width:150px; border-top:1px solid #000; padding-top:5px; font-weight:900; font-size:10px;">FORM MASTER</div>
                            <div style="text-align:center; width:150px; border-top:1px solid #000; padding-top:5px; font-weight:900; font-size:10px;">CHECKER</div>
                            <div style="text-align:center; width:150px; border-top:1px solid #000; padding-top:5px; font-weight:900; font-size:10px;">PRINCIPAL</div>
                        </div>
                    </div>
                    <script>
                        setTimeout(() => { window.print(); }, 1000);
                    <\/script>
                </body>
                </html>
            `);
            win.document.close();
            win.focus();
        } catch(e) {
            console.error(e);
            win.document.write("Print Error: " + e.message);
            win.document.close();
        }
    };
    const ALL_CLASSES = <?php echo json_encode($all_classes); ?>;

    function togglePHClasses(el) {
        const checkboxes = document.querySelectorAll('.ph-class-check');
        checkboxes.forEach(cb => {
            cb.checked = el.checked;
            cb.disabled = el.checked;
        });
    }

    // Public Holiday JS Modal functions
    function openPHModal() {
        if(document.getElementById('ph_modal_overlay')) {
            document.getElementById('ph_modal_overlay').style.display = 'flex';
        } else {
            const overlay = document.createElement('div');
            overlay.id = 'ph_modal_overlay';
            overlay.className = 'premium-modal-overlay';
            overlay.style.display = 'flex';
            
            let html = `
                <div class="premium-modal" style="max-width:450px;">
                    <div class="modal-header">
                        <h4 style="margin:0; font-size:14px; font-weight:900;"><i class="fas fa-calendar-day" style="color:#e67e22; margin-right:8px;"></i> MARK PUBLIC HOLIDAY (PH)</h4>
                        <i class="fas fa-times" style="cursor:pointer;" onclick="this.closest('.premium-modal-overlay').style.display='none'"></i>
                    </div>
                    <div class="modal-body">
                        <div style="background:rgba(230, 126, 34, 0.1); padding:10px 15px; border-radius:12px; margin-bottom:15px; border:1px solid rgba(230, 126, 34, 0.2);">
                            <div style="font-size:11px; font-weight:700; color:#e67e22;">
                                All dates within your selected range will be marked as "PH (Public Holiday)" for selected classes.
                            </div>
                        </div>

                        <div style="margin-bottom:15px;">
                            <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase; color:var(--text-main);">Applicable Classes</label>
                            <div style="background:rgba(255,255,255,0.03); border:1px solid rgba(255,255,255,0.1); border-radius:12px; padding:10px; max-height:160px; overflow-y:auto;">
                                <label style="display:flex; align-items:center; gap:10px; padding:8px; border-bottom:1px solid rgba(255,255,255,0.05); cursor:pointer;">
                                    <input type="checkbox" id="ph_all_classes" checked onchange="togglePHClasses(this)" style="width:16px; height:16px; accent-color:#e67e22;">
                                    <span style="font-size:12px; font-weight:800; color:#e67e22;">SELECT ALL CLASSES</span>
                                </label>
                                <div id="ph_class_list_container" style="padding-top:5px;">
                                    ${ALL_CLASSES.map(c => `
                                        <label style="display:flex; align-items:center; gap:10px; padding:6px 8px; cursor:pointer; opacity:0.8;">
                                            <input type="checkbox" class="ph-class-check" value="${c.id}" disabled checked style="width:15px; height:15px; accent-color:#e67e22;">
                                            <span style="font-size:11px; font-weight:600;">${c.name}</span>
                                        </label>
                                    `).join('')}
                                </div>
                            </div>
                        </div>

                        <div style="margin-bottom:15px;">
                            <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase; color:var(--text-main);">Holiday Name</label>
                            <input type="text" id="ph_name" class="policy-input" style="height:44px;" placeholder="e.g. Eid Holiday, Summer Vacation">
                        </div>

                        <div style="display:flex; gap:15px; margin-bottom:15px;">
                            <div style="flex:1;">
                                <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase; color:var(--text-main);">From Date</label>
                                <input type="date" id="ph_start" class="policy-input" style="height:44px;">
                            </div>
                            <div style="flex:1;">
                                <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase; color:var(--text-main);">To Date</label>
                                <input type="date" id="ph_end" class="policy-input" style="height:44px;">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer" style="display:flex; justify-content:space-between; align-items:center;">
                        <button class="btn" onclick="openPHManageModal()" style="font-weight:900; background:rgba(0,0,0,0.05); color:var(--text-main); font-size:11px; border:none; padding:12px 20px;"><i class="fas fa-list"></i> Manage PH</button>
                        <button class="btn" style="background:linear-gradient(135deg, #e67e22 0%, #d35400 100%); border:none; padding:12px 20px; font-size:12px; color:white; font-weight:900;" onclick="savePublicHoliday()"><i class="fas fa-save"></i> Save PH</button>
                    </div>
                </div>
            `;
            overlay.innerHTML = html;
            document.body.appendChild(overlay);
        }
    }

    async function savePublicHoliday() {
        const name = document.getElementById('ph_name').value.trim();
        const start = document.getElementById('ph_start').value;
        const end = document.getElementById('ph_end').value;

        let class_ids = 'all';
        if(!document.getElementById('ph_all_classes').checked) {
            const selected = Array.from(document.querySelectorAll('.ph-class-check:checked')).map(cb => cb.value);
            if(selected.length === 0) {
                Swal.fire({title: 'Required', text: 'Please select at least one class or choose "All Classes".', icon: 'warning'});
                return;
            }
            class_ids = selected.join(',');
        }

        if (!name || !start || !end) {
            Swal.fire({title: 'Required', text: 'Please define holiday name and dates.', icon: 'warning'});
            return;
        }

        const res = await fetch('attendance_api.php?action=save_public_holiday', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ name: name, start: start, end: end, class_ids: class_ids })
        });
        const data = await res.json();
        
        if (data.success) {
            Swal.fire({title: 'Success!', text: 'Public Holiday marked successfully.', icon: 'success'});
            document.getElementById('ph_modal_overlay').style.display = 'none';
            // Refresh views
            refreshAttendanceViews();
        } else {
            Swal.fire({title: 'Error', text: data.error, icon: 'error'});
        }
    }

    function openEditPHModal(id, name, from, to, classIds) {
        const overlayId = 'ph_edit_modal_overlay';
        if(document.getElementById(overlayId)) document.getElementById(overlayId).remove();

        const overlay = document.createElement('div');
        overlay.id = overlayId;
        overlay.className = 'premium-modal-overlay';
        overlay.style.display = 'flex';
        
        const isAll = classIds === 'all' || classIds === '' || !classIds;

        let html = `
            <div class="premium-modal" style="max-width:450px;">
                <div class="modal-header">
                    <h4 style="margin:0; font-size:14px; font-weight:900;"><i class="fas fa-edit" style="color:#3498db; margin-right:8px;"></i> EDIT PUBLIC HOLIDAY</h4>
                    <i class="fas fa-times" style="cursor:pointer;" onclick="document.getElementById('${overlayId}').remove()"></i>
                </div>
                <div class="modal-body">
                    <div style="margin-bottom:15px;">
                        <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase;">Holiday Name</label>
                        <input type="text" id="edit_ph_name" class="policy-input" style="height:44px;" value="${name}">
                    </div>
                    <div style="display:flex; gap:15px; margin-bottom:15px;">
                        <div style="flex:1;">
                            <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase;">From Date</label>
                            <input type="date" id="edit_ph_start" class="policy-input" style="height:44px;" value="${from}">
                        </div>
                        <div style="flex:1;">
                            <label style="display:block; font-size:10px; font-weight:800; margin-bottom:5px; text-transform:uppercase;">To Date</label>
                            <input type="date" id="edit_ph_end" class="policy-input" style="height:44px;" value="${to}">
                        </div>
                    </div>
                </div>
                <div class="modal-footer" style="display:flex; justify-content:space-between; align-items:center;">
                    <button class="btn" style="background:#e74c3c; color:white; font-weight:900; padding:10px 20px; font-size:11px;" onclick="confirmDeletePH(${id})"><i class="fas fa-trash"></i> Delete</button>
                    <button class="btn" style="background:#3498db; color:white; font-weight:900; padding:10px 20px; font-size:11px;" onclick="updateEditPH(${id})"><i class="fas fa-check"></i> Update Changes</button>
                </div>
            </div>
        `;
        overlay.innerHTML = html;
        document.body.appendChild(overlay);
    }

    async function updateEditPH(id) {
        const name = document.getElementById('edit_ph_name').value.trim();
        const start = document.getElementById('edit_ph_start').value;
        const end = document.getElementById('edit_ph_end').value;

        if (!name || !start || !end) return Swal.fire('Error', 'Please fill all fields', 'warning');

        const res = await fetch('attendance_api.php?action=update_public_holiday', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ id: id, name: name, start: start, end: end })
        });
        const data = await res.json();
        if(data.success) {
            Swal.fire('Updated', 'Holiday details updated successfully.', 'success');
            document.getElementById('ph_edit_modal_overlay').remove();
            refreshAttendanceViews();
        } else Swal.fire('Error', data.error, 'error');
    }

    function confirmDeletePH(id) {
        Swal.fire({
            title: 'Are you sure?',
            text: "This will remove the holiday and all associated 'PH' marks from the ledger.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#e74c3c',
            confirmButtonText: 'Yes, Delete it!'
        }).then(async (result) => {
            if (result.isConfirmed) {
                const res = await fetch('attendance_api.php?action=delete_public_holiday', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ id: id })
                });
                const data = await res.json();
                if(data.success) {
                    Swal.fire('Deleted', 'Holiday has been removed.', 'success');
                    if(document.getElementById('ph_edit_modal_overlay')) document.getElementById('ph_edit_modal_overlay').remove();
                    refreshAttendanceViews();
                } else Swal.fire('Error', data.error, 'error');
            }
        });
    }

    function refreshAttendanceViews() {
        if(typeof loadAdminDailyStudents === 'function') loadAdminDailyStudents();
        if(typeof loadStaffAttendance === 'function') loadStaffAttendance();
        if(typeof loadAdminMonthlySummary === 'function') loadAdminMonthlySummary();
        if(typeof loadDetailedSheet === 'function') {
            const reportResult = document.getElementById('report-result');
            if(reportResult && reportResult.innerHTML.includes('ledger-table')) {
                loadDetailedSheet();
            }
        }
    }

    async function openPHManageModal() {
        if (document.getElementById('ph_modal_overlay')) {
            document.getElementById('ph_modal_overlay').style.display = 'none';
        }
        if (document.getElementById('ph_manage_overlay')) {
            document.getElementById('ph_manage_overlay').remove();
        }
        
        const overlay = document.createElement('div');
        overlay.id = 'ph_manage_overlay';
        overlay.className = 'premium-modal-overlay';
        overlay.style.display = 'flex';
        
        let html = `
            <div class="premium-modal" style="max-width:550px; width:95%;">
                <div class="modal-header">
                    <h4 style="margin:0; font-size:14px; font-weight:900;"><i class="fas fa-clock" style="color:#e67e22; margin-right:8px;"></i> MANAGE PUBLIC HOLIDAYS</h4>
                    <i class="fas fa-times" style="cursor:pointer;" onclick="this.closest('.premium-modal-overlay').remove()"></i>
                </div>
                <div class="modal-body" id="ph_management_list" style="max-height:60vh; overflow-y:auto; padding-top:10px;">
                    <div style="text-align:center; padding:20px;">Loading holidays...</div>
                </div>
            </div>
        `;
        overlay.innerHTML = html;
        document.body.appendChild(overlay);
        
        try {
            const res = await fetch('attendance_api.php?action=get_public_holidays');
            const data = await res.json();
            
            let listHtml = '';
            if(!data || data.length === 0) {
                listHtml = '<div style="text-align:center; padding:20px; opacity:0.6; font-weight:700;">No Public Holidays configured yet.</div>';
            } else {
                listHtml += '<div style="display:flex; flex-direction:column; gap:10px;">';
                data.forEach(h => {
                    let scopeText = "GLOBAL (ALL CLASSES)";
                    if(h.class_ids && h.class_ids !== 'all') {
                        const count = h.class_ids.split(',').length;
                        scopeText = `${count} SPECIFIC CLASSES`;
                    }
                    listHtml += `
                        <div style="background:rgba(0,0,0,0.03); border:1px solid rgba(0,0,0,0.05); padding:15px; border-radius:12px; display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-weight:900; font-size:13px; color:var(--text-main);">${h.name}</div>
                                <div style="font-size:10px; font-weight:700; opacity:0.6; margin-top:3px; color:var(--text-main);"><i class="fas fa-calendar-alt"></i> ${h.from_date} <i class="fas fa-arrow-right" style="margin:0 5px; font-size:8px;"></i> ${h.to_date}</div>
                                <div style="font-size:9px; font-weight:900; background:rgba(230, 126, 34, 0.1); color:#e67e22; padding:2px 8px; border-radius:6px; display:inline-block; margin-top:5px; text-transform:uppercase;">${scopeText}</div>
                            </div>
                            <button class="btn" style="background:rgba(231, 76, 60, 0.1) !important; color:#e74c3c !important; border:none !important; padding:8px 12px !important; font-size:10px !important; border-radius:8px !important; font-weight:900 !important;" onclick="deletePublicHoliday(${h.id})"><i class="fas fa-trash"></i></button>
                        </div>
                    `;
                });
                listHtml += '</div>';
            }
            document.getElementById('ph_management_list').innerHTML = listHtml;
        } catch(e) {
            document.getElementById('ph_management_list').innerHTML = '<div style="color:red; font-weight:bold; text-align:center;">Failed to load holidays</div>';
        }
    }

    async function deletePublicHoliday(id) {
        if(!confirm("Are you sure you want to delete this public holiday period?")) return;
        
        const res = await fetch('attendance_api.php?action=delete_public_holiday', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ id: id })
        });
        const data = await res.json();
        if(data.success) {
            document.getElementById('ph_manage_overlay').remove();
            openPHManageModal();
            // Refresh ALL possible views
            if(typeof loadAdminDailyStudents === 'function') loadAdminDailyStudents();
            if(typeof loadStaffAttendance === 'function') loadStaffAttendance();
            if(typeof loadAdminMonthlySummary === 'function') loadAdminMonthlySummary();
            if(typeof loadDetailedSheet === 'function') {
                const reportResult = document.getElementById('report-result');
                if(reportResult && reportResult.innerHTML.includes('ledger-table')) {
                    loadDetailedSheet();
                }
            }
        } else {
            alert('Error deleting: ' + (data.error || 'Unknown Error'));
        }
    }
    </script>
    <style>
        /* FINAL FORCE: Ensure Dark Theme for all SweetAlerts */
        .swal2-popup, div.swal2-popup.swal2-modal {
            background: #0f172a !important;
            color: #ffffff !important;
            border-radius: 1.5rem !important;
            border: 1px solid rgba(255,255,255,0.1) !important;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7) !important;
        }
        .swal2-title, .swal2-html-container, .swal2-content, .swal2-popup h2, .swal2-popup div:not(.swal2-icon):not(.swal2-actions) {
            color: #ffffff !important;
            opacity: 1 !important;
            text-shadow: none !important;
        }
        .swal2-confirm, .swal2-cancel, .swal2-styled.swal2-confirm {
            background: #6366f1 !important;
            color: #ffffff !important;
            font-weight: 700 !important;
            border-radius: 0.75rem !important;
        }
        .swal2-success-circular-line-left, .swal2-success-circular-line-right, .swal2-success-fix {
            background-color: transparent !important;
        }

        /* Fix Close Editor Button in White Theme */
        body.theme-white .close-editor-btn {
            background: #e2e8f0 !important;
            color: #0f172a !important;
            border: 1px solid #cbd5e1 !important;
        }
    </style>
<script>
    async function openAttendanceEditor(personId, personName, month, year, context) {
        const overlayId = 'attendance_editor_overlay';
        if(document.getElementById(overlayId)) document.getElementById(overlayId).remove();

        const overlay = document.createElement('div');
        overlay.id = overlayId;
        overlay.className = 'premium-modal-overlay';
        overlay.style.cssText = 'display:flex; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); z-index:200000; align-items:center; justify-content:center; backdrop-filter:blur(5px);';
        
        const monthNum = parseInt(month);
        const yearNum = parseInt(year);
        const firstDay = new Date(yearNum, monthNum - 1, 1).getDay(); // Sunday=0
        const daysInMonth = new Date(yearNum, monthNum, 0).getDate();
        const monthName = new Date(yearNum, monthNum - 1, 1).toLocaleString('default', { month: 'long' });

        // Find existing attendance data from the already loaded state if possible
        let personnelRes = (context === 'staff' ? window.lastPersonnelData?.staff : window.lastPersonnelData?.students) || [];
        let person = personnelRes.find(p => p.id == personId);
        let attendance = person ? person.attendance : {};

        let gridHtml = '';
        // Paddings for first week
        for(let p=0; p<firstDay; p++) {
            gridHtml += '<div style="aspect-ratio:1; border:1px solid rgba(255,255,255,0.03);"></div>';
        }

        for(let d=1; d<=daysInMonth; d++) {
            const dateStr = `${year}-${month.toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}`;
            let status = attendance[d] || '';
            const dayOfWeek = new Date(yearNum, monthNum - 1, d).getDay();
            
            // AUTO-FIX: Sundays should always be blank. If data exists, we silently clean it.
            if(dayOfWeek === 0 && status !== '') {
                fetch(`attendance_api.php?action=delete_attendance&type=${context}&id=${personId}&date=${dateStr}`);
                status = '';
            }

            let bg = 'rgba(255,255,255,0.05)';
            let color = 'white';
            
            // Color sequence: Status > Day Type (Sunday/Friday) > Default
            if(status === 'Present') bg = '#27ae60';
            else if(status === 'Absent') bg = '#c0392b';
            else if(status === 'Leave') bg = '#2980b9';
            else if(status === 'Struck Off') bg = '#000';
            else if(status === 'Holiday' || status === 'PH') bg = '#f39c12'; // Bright orange for PH
            else if(dayOfWeek === 0) bg = 'rgba(231, 76, 60, 0.3)'; // Sunday Light Red
            else if(dayOfWeek === 5) bg = 'rgba(46, 204, 113, 0.3)'; // Friday Light Green

            // Show 'PH' for public holidays instead of just 'P'
            const displayText = (status === 'PH') ? 'PH' : (status ? status.charAt(0) : '');

            gridHtml += `
                <div onclick="toggleDayAttendance(${personId}, '${dateStr}', '${context}', '${status}')" 
                     style="aspect-ratio:1; border:1px solid rgba(255,255,255,0.1); display:flex; flex-direction:column; align-items:center; justify-content:center; cursor:pointer; background:${bg}; transition:all 0.2s; position:relative; border-radius:12px; margin:2px;">
                    <span style="font-size:11px; font-weight:800; opacity:0.6; position:absolute; top:4px; left:6px; color:${color};">${d}</span>
                    <span style="font-size:15px; font-weight:900; color:${color};">${displayText}</span>
                </div>`;
        }

        let picHtml = '';
        if (person && person.profile_pic) {
            picHtml = `<img src="../../assets/uploads/${person.profile_pic}" style="width:40px; height:40px; border-radius:50%; object-fit:cover; margin-right:12px; border:2px solid rgba(255,255,255,0.2);" onerror="this.outerHTML='<div style=\\\'width:40px; height:40px; border-radius:50%; background:rgba(155,89,182,0.2); display:flex; align-items:center; justify-content:center; margin-right:12px; font-size:14px; color:#9b59b6;\\\'><i class=\\\'fas fa-user\\\'></i></div>'">`;
        } else {
            picHtml = `<div style="width:40px; height:40px; border-radius:50%; background:rgba(155,89,182,0.2); display:flex; align-items:center; justify-content:center; margin-right:12px; font-size:14px; color:#9b59b6;"><i class="fas fa-user"></i></div>`;
        }

        let html = `
            <div class="premium-modal" style="width:90%; max-width:480px; background:#0f172a; border:1px solid rgba(255,255,255,0.1); border-radius:20px; box-shadow:0 25px 50px -12px rgba(0,0,0,0.5); overflow:hidden;">
                <div class="modal-header" style="padding:20px; border-bottom:1px solid rgba(255,255,255,0.05); display:flex; justify-content:space-between; align-items:center;">
                    <div style="display:flex; align-items:center;">
                        ${picHtml}
                        <div>
                            <h4 style="margin:0; font-size:16px; font-weight:900; color:white;" class="theme-text-white-fix">${context === 'staff' ? 'STAFF' : 'STUDENT'} ATTENDANCE EDITOR</h4>
                            <p style="margin:2px 0 0; font-size:11px; color:#94a3b8; font-weight:600;">Editing record for: ${personName} (${monthName} ${year})</p>
                        </div>
                    </div>
                    <i class="fas fa-times" style="font-size:18px; color:#64748b; cursor:pointer;" onclick="document.getElementById('${overlayId}').remove()"></i>
                </div>
                <div class="modal-body" style="padding:20px;">
                    <div style="display:grid; grid-template-columns: repeat(7, 1fr); gap:2px; background:rgba(0,0,0,0.2); padding:10px; border-radius:12px;">
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">SUN</div>
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">MON</div>
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">TUE</div>
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">WED</div>
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">THU</div>
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">FRI</div>
                        <div style="text-align:center; font-size:9px; font-weight:900; color:#64748b; padding-bottom:10px;">SAT</div>
                        ${gridHtml}
                    </div>
                    
                    <div style="margin-top:20px; padding:15px; background:rgba(255,255,255,0.02); border-radius:12px; border:1px solid rgba(255,255,255,0.05);">
                        <div style="display:flex; justify-content:space-between; align-items:center;">
                            <p style="margin:0; font-size:10px; font-weight:800; color:#f87171; text-transform:uppercase;">Administrative Actions</p>
                            <button onclick="deleteMonthlyRecords(${personId}, '${month}', '${year}', '${context}', '${personName}')" 
                                    style="background:#f87171; color:white; border:none; padding:8px 12px; border-radius:8px; font-size:10px; font-weight:900; cursor:pointer;">
                                <i class="fas fa-trash-alt"></i> DELETE MONTHLY RECORDS
                            </button>
                        </div>
                    </div>
                </div>
                <div class="modal-footer" style="padding:20px; background:white; display:flex; justify-content:center;">
                     <button class="btn close-editor-btn" style="background:#2c3e50; color:white; font-weight:900; padding:12px 30px; border-radius:12px;" onclick="document.getElementById('${overlayId}').remove()">CLOSE EDITOR</button>
                </div>
            </div>
        `;
        overlay.innerHTML = html;
        document.body.appendChild(overlay);
    }

    async function toggleDayAttendance(id, date, type, currentStatus) {
        const statuses = ['', 'Present', 'Absent', 'Leave', 'Struck Off'];
        let nextIdx = (statuses.indexOf(currentStatus) + 1) % statuses.length;
        let nextStatus = statuses[nextIdx];
        
        if (nextStatus === '') {
            // Delete record
            const res = await fetch(`attendance_api.php?action=delete_attendance&type=${type}&id=${id}&date=${date}`);
            const data = await res.json();
            if(data.success) {
                // Refresh modal and table
                loadDetailedSheet();
                setTimeout(() => {
                    const month = date.split('-')[1];
                    const year = date.split('-')[0];
                    const name = document.querySelector(`td[onclick*="${id}"]`).textContent.trim();
                    openAttendanceEditor(id, name, month, year, type);
                }, 500);
            }
        } else {
            // Save record
            const res = await fetch(`attendance_api.php?action=save_single_entry`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ type: type, id: id, date: date, status: nextStatus })
            });
            const data = await res.json();
            if(data.success) {
                loadDetailedSheet();
                setTimeout(() => {
                    const month = date.split('-')[1];
                    const year = date.split('-')[0];
                    const name = document.querySelector(`td[onclick*="${id}"]`).textContent.trim();
                    openAttendanceEditor(id, name, month, year, type);
                }, 500);
            }
        }
    }

    async function deleteMonthlyRecords(id, month, year, type, name) {
        const confirmed = await Swal.fire({
            title: 'Delete Records?',
            text: `Are you sure you want to delete ALL attendance records for ${name} in ${month}/${year}? This action cannot be undone.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ef4444',
            confirmButtonText: 'Yes, Delete All'
        });

        if (confirmed.isConfirmed) {
            Swal.fire({title: 'Processing...', allowOutsideClick: false, didOpen: () => { Swal.showLoading(); }});
            for(let d=1; d<=31; d++) {
                const date = `${year}-${month.toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}`;
                await fetch(`attendance_api.php?action=delete_attendance&type=${type}&id=${id}&date=${date}`);
            }
            Swal.close();
            Swal.fire('Deleted!', 'All records for this month have been cleared.', 'success');
            document.getElementById('attendance_editor_overlay').remove();
            loadDetailedSheet();
        }
    }
</script>
</body>
</html>





 