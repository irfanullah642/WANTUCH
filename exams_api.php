<?php
// Disable error display to prevent HTML output in JSON responses
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

if (session_status() == PHP_SESSION_NONE) {
    session_start();
}
require_once '../../includes/db.php';
require_once '../../includes/notification_helper.php';
require_once '../../includes/guest_access_helper.php';

// Block guest users from write operations
block_guest_write();

if (!isset($_SESSION['edu_user_id'])) {
    http_response_code(403);
    header('Content-Type: application/json');
    echo json_encode(['error' => 'Unauthorized']);
    exit;
}

$inst_id = $_SESSION['edu_institution_id'];
$action = $_GET['action'] ?? '';
$role = $_SESSION['edu_role'];
$uid = $_SESSION['edu_user_id'];

// Global Role/ID for students to avoid parameters manipulation
$student_enroll = null;
if($role == 'student') {
    $student_enroll = $conn->query("SELECT * FROM edu_student_enrollment WHERE student_id = $uid AND status IN ('active', 'Active') ORDER BY academic_year DESC LIMIT 1")->fetch_assoc();
}

// Set JSON header for all actions except CSV export
if ($action !== 'export_csv') {
    header('Content-Type: application/json');
}

// Action Authorization helper
// Action Authorization helper
function authorize($allowed_roles) {
    global $role;
    // Allow developer and super_admin as admin by default
    if(in_array($role, ['developer', 'super_admin'])) {
        return; // specific check passed implicitly for high priv scenarios
    }
    
    if(!in_array($role, $allowed_roles)) {
        http_response_code(403);
        echo json_encode(['status' => 'error', 'message' => 'Unauthorized for this action']);
        exit;
    }
}

if ($action == 'create_exam') {
    authorize(['admin', 'staff']);
    $type = $conn->real_escape_string($_POST['type']);
    $semester = $conn->real_escape_string($_POST['semester']);
    $year = $conn->real_escape_string($_POST['year']);
    $cid_default = isset($_POST['class_id']) ? (int)$_POST['class_id'] : 0;
    $sid_default = isset($_POST['section_id']) ? (int)$_POST['section_id'] : 0;
    
    if(!isset($_POST['subjects_json'])) {
        echo json_encode(['status' => 'error', 'message' => 'No subjects provided']);
        exit;
    }

    $subjects = json_decode($_POST['subjects_json'], true);
    if(empty($subjects)) {
         echo json_encode(['status' => 'error', 'message' => 'No subjects provided']);
         exit;
    }

    $success_count = 0;
    $errors = [];

    foreach($subjects as $sub) {
        $cid = !empty($sub['class_id']) ? (int)$sub['class_id'] : $cid_default;
        $sid = !empty($sub['section_id']) ? (int)$sub['section_id'] : $sid_default;
        $subid = (int)$sub['subject_id'];
        
        $date = $conn->real_escape_string($sub['date']);
        $time = $conn->real_escape_string($sub['time']);
        $end_time = !empty($sub['end_time']) ? "'" . $conn->real_escape_string($sub['end_time']) . "'" : 'NULL';
        $shift = $conn->real_escape_string($sub['shift']);
        $total = (int)$sub['total'];

        // Check exists
        $check_sql = "SELECT id FROM edu_exams 
                     WHERE institution_id = $inst_id 
                     AND exam_type = '$type' 
                     AND academic_year = '$year' 
                     AND class_id = $cid 
                     AND section_id = $sid 
                     AND subject_id = $subid";
        $check_res = $conn->query($check_sql);
        
        if ($check_res && $check_res->num_rows > 0) {
             $ex_id = $check_res->fetch_assoc()['id'];
             $conn->query("UPDATE edu_exams SET exam_date='$date', start_time='$time', end_time=$end_time, shift='$shift', total_marks=$total WHERE id=$ex_id");
             $success_count++;
        } else {
             $sql = "INSERT INTO edu_exams (institution_id, exam_type, semester, academic_year, class_id, section_id, subject_id, exam_date, start_time, end_time, shift, total_marks) 
                     VALUES ($inst_id, '$type', '$semester', '$year', $cid, $sid, $subid, '$date', '$time', $end_time, '$shift', $total)";
             if ($conn->query($sql)) {
                 $success_count++;
             } else {
                 $errors[] = "Failed for subject $subid: " . $conn->error;
             }
        }
    }

    if(count($errors) > 0) {
        echo json_encode(['status' => 'error', 'message' => implode(', ', $errors)]);
    } else {
        echo json_encode(['status' => 'success', 'message' => "Scheduled $success_count exams successfully!"]);
    }

} elseif ($action == 'quick_schedule') {
    authorize(['admin']);
    $text = trim($_POST['text']);
    $type = $conn->real_escape_string($_POST['type']);
    $year = $conn->real_escape_string($_POST['year']);

    if(empty($text)) {
        echo json_encode(['status' => 'error', 'message' => 'Empty text']);
        exit;
    }

    $entries = explode(',', $text);
    $success_count = 0;
    $errors = [];
    
    // Cache
    $cache_class = [];
    $cache_subject = [];
    
    foreach($entries as $entry) {
        $entry = trim($entry);
        if(empty($entry)) continue;
        
        // Format: Class Date Time Shift Subject
        // Example: 10th 2024-05-20 09:00 Morning Math
        $parts = preg_split('/\s+/', $entry);
        if(count($parts) < 5) {
            $errors[] = "Invalid format: $entry";
            continue;
        }
        
        // "Math" or "Computer Science"? Subject might be multiple words.
        // Usually Class, Date, Time, Shift are single words. Subject is the rest.
        // Let's assume:
        // Index 0: Class
        // Index 1: Date
        // Index 2: Time
        // Index 3: Shift
        // Index 4...: Subject
        
        $c_name = $parts[0];
        $date = $parts[1];
        $time = $parts[2];
        $shift = $parts[3];
        $sub_name = implode(' ', array_slice($parts, 4));
        
        // Resolve Class
        if(!isset($cache_class[$c_name])) {
            $res = $conn->query("SELECT id FROM edu_classes WHERE institution_id=$inst_id AND name='$c_name'");
            if($res->num_rows > 0) $cache_class[$c_name] = $res->fetch_assoc()['id'];
            else $cache_class[$c_name] = null;
        }
        $cid = $cache_class[$c_name];
        if(!$cid) { $errors[] = "Class not found: $c_name"; continue; }
        
        // Resolve Subject
        if(!isset($cache_subject[$sub_name])) {
            $res = $conn->query("SELECT id FROM edu_subjects WHERE institution_id=$inst_id AND name='$sub_name'");
            if($res->num_rows > 0) $cache_subject[$sub_name] = $res->fetch_assoc()['id'];
            else $cache_subject[$sub_name] = null;
        }
        $sub_id = $cache_subject[$sub_name];
        if(!$sub_id) { $errors[] = "Subject not found: $sub_name"; continue; }
        
        // Get Sections for this class (Schedule for ALL sections)
        $sections = [];
        $sec_q = $conn->query("SELECT id FROM edu_sections WHERE class_id=$cid");
        while($s = $sec_q->fetch_assoc()) $sections[] = $s['id'];
        
        if(empty($sections)) { $errors[] = "No sections found for class $c_name"; continue; }
        
        foreach($sections as $sid) {
            // Check existence
            $check = $conn->query("SELECT id FROM edu_exams WHERE institution_id=$inst_id AND class_id=$cid AND section_id=$sid AND subject_id=$sub_id AND exam_type='$type' AND academic_year='$year'");
            
            if($check->num_rows > 0) {
                 // Update
                 $ex_id = $check->fetch_assoc()['id'];
                 $sql = "UPDATE edu_exams SET exam_date='$date', start_time='$time', shift='$shift' WHERE id=$ex_id";
                 $conn->query($sql);
            } else {
                 // Insert
                 // Default total marks = 100 for quick mode? Or make it configurable?
                 // Let's assume 100 for now or fetch existing standard?
                 $total = 100;
                 $sql = "INSERT INTO edu_exams (institution_id, exam_type, academic_year, class_id, section_id, subject_id, exam_date, start_time, shift, total_marks) 
                         VALUES ($inst_id, '$type', '$year', $cid, $sid, $sub_id, '$date', '$time', '$shift', $total)";
                 $conn->query($sql);
            }
        }
        $success_count++;
    }
    
    $msg = "Processed $success_count entries.";
    if(!empty($errors)) $msg .= " Errors: " . implode(", ", array_slice($errors, 0, 3));
    
    echo json_encode(['status' => 'success', 'message' => $msg]);

} elseif ($action == 'get_subjects') {
    $where = "institution_id = " . (int)$inst_id;
    if($is_staff) {
        $res_sub = $conn->query("SELECT DISTINCT subject_id FROM edu_timetable WHERE staff_id = $uid AND institution_id = $inst_id");
        $sub_ids = [];
        while($r = $res_sub->fetch_assoc()) $sub_ids[] = $r['subject_id'];
        
        if(!empty($sub_ids)) {
            $where .= " AND id IN (" . implode(',', $sub_ids) . ")";
        } else {
            echo json_encode([]); exit;
        }
    }
    
    $sql = "SELECT id, name 
            FROM edu_subjects 
            WHERE $where 
            ORDER BY name";
    $res = $conn->query($sql);
    
    if (!$res) {
        echo json_encode(['error' => $conn->error, 'sql' => $sql]);
        exit;
    }
    
    $subjects = [];
    while($row = $res->fetch_assoc()) $subjects[] = $row;
    echo json_encode($subjects);

} elseif ($action == 'get_staff_list') {
    // Fetch active staff for selection
    $res = $conn->query("SELECT id, full_name FROM edu_users WHERE role IN ('staff', 'admin') AND institution_id = $inst_id ORDER BY full_name");
    $data = [];
    while($row = $res->fetch_assoc()) {
        if ($role === 'guest') $row['full_name'] = '?';
        $data[] = $row;
    }
    echo json_encode($data);

} elseif ($action == 'get_daily_exams') {
    // Fetch all exams for a specific date (and optional class/section)
    $date = $conn->real_escape_string($_GET['date']);
    $where = "e.institution_id = $inst_id AND e.exam_date = '$date'";
    
    if($is_staff) {
        $conditions = [];
        $res_asgn = $conn->query("SELECT class_id, section_id, subject_id FROM edu_timetable WHERE staff_id = $uid AND institution_id = $inst_id");
        while($r = $res_asgn->fetch_assoc()) {
            $conditions[] = "(e.class_id = {$r['class_id']} AND e.section_id = {$r['section_id']} AND e.subject_id = {$r['subject_id']})";
        }
        if(!empty($conditions)) {
            $where .= " AND (" . implode(" OR ", $conditions) . ")";
        } else {
            $where .= " AND 1=0";
        }
    }
    
    if(isset($_GET['class_id']) && !empty($_GET['class_id'])) {
        $where .= " AND e.class_id = " . (int)$_GET['class_id'];
    }
    if(isset($_GET['section_id']) && !empty($_GET['section_id'])) {
        $where .= " AND e.section_id = " . (int)$_GET['section_id'];
    }
    
    $sql = "SELECT e.subject_id, e.shift, e.exam_type, e.class_id, e.section_id,
                   s.name as subject_name, c.name as class_name, sec.name as section_name
            FROM edu_exams e
            JOIN edu_subjects s ON e.subject_id = s.id
            JOIN edu_classes c ON e.class_id = c.id
            JOIN edu_sections sec ON e.section_id = sec.id
            WHERE $where
            ORDER BY e.shift, c.name, sec.name";
            
    $res = $conn->query($sql);
    $data = [];
    while($row = $res->fetch_assoc()) $data[] = $row;
    echo json_encode($data);

} elseif ($action == 'get_scheduled_subjects') {
    $date = $conn->real_escape_string($_GET['date']);
    $shift = $conn->real_escape_string($_GET['shift']);
    
    // Fetch subjects that have an exam on this date/shift
    $sql = "SELECT DISTINCT s.id, s.name 
            FROM edu_exams e 
            JOIN edu_subjects s ON e.subject_id = s.id 
            WHERE e.institution_id = $inst_id AND e.exam_date = '$date' AND e.shift = '$shift'
            ORDER BY s.name";
    $res = $conn->query($sql);
    $data = [];
    while($row = $res->fetch_assoc()) $data[] = $row;
    echo json_encode($data);
    exit;

} elseif ($action == 'print_datesheet') {
    // Printable Date Sheet (Consolidated & Styled)
    header('Content-Type: text/html');

    $type = isset($_REQUEST['type']) ? $conn->real_escape_string($_REQUEST['type']) : '';
    $year = isset($_REQUEST['year']) ? $conn->real_escape_string($_REQUEST['year']) : '';
    $semester = isset($_REQUEST['semester']) ? $conn->real_escape_string($_REQUEST['semester']) : '';
    $date = isset($_REQUEST['date']) ? $conn->real_escape_string($_REQUEST['date']) : '';
    $shift = isset($_REQUEST['shift']) ? $conn->real_escape_string($_REQUEST['shift']) : '';
    $cid = isset($_REQUEST['class_id']) ? (int)$_REQUEST['class_id'] : 0;
    $sid = isset($_REQUEST['section_id']) ? (int)$_REQUEST['section_id'] : 0;
    
    // Build Query
    $where = "e.institution_id = $inst_id";
    if(!empty($type)) $where .= " AND e.exam_type = '$type'";
    if(!empty($year)) $where .= " AND e.academic_year = '$year'";
    if(!empty($semester)) $where .= " AND e.semester = '$semester'";
    if(!empty($date)) $where .= " AND e.exam_date = '$date'";
    if(!empty($shift)) $where .= " AND e.shift = '$shift'";
    if($cid > 0) $where .= " AND e.class_id = $cid";
    if($sid > 0) $where .= " AND e.section_id = $sid";
    
    // Fetch filter options for the UI
    $opt_types = $conn->query("SELECT DISTINCT exam_type FROM edu_exams WHERE institution_id = $inst_id AND exam_type != '' ORDER BY exam_type");
    $opt_years = $conn->query("SELECT DISTINCT academic_year FROM edu_exams WHERE institution_id = $inst_id AND academic_year != '' ORDER BY academic_year DESC");
    $opt_semesters = $conn->query("SELECT DISTINCT semester FROM edu_exams WHERE institution_id = $inst_id AND semester != '' ORDER BY semester");
    $opt_classes = $conn->query("SELECT id, name FROM edu_classes WHERE institution_id = $inst_id ORDER BY name");
    $opt_sections = $conn->query("SELECT id, name, class_id FROM edu_sections WHERE class_id IN (SELECT id FROM edu_classes WHERE institution_id = $inst_id) ORDER BY name");
    
    $inst = $conn->query("SELECT * FROM edu_institutions WHERE id = $inst_id")->fetch_assoc();
    
    // Fetch Exams
    $sql = "SELECT e.exam_date, e.start_time, e.end_time, e.shift, e.exam_type, e.academic_year,
                   s.name as subject, c.name as class_name, sec.name as section_name 
            FROM edu_exams e 
            JOIN edu_subjects s ON e.subject_id = s.id 
            JOIN edu_classes c ON e.class_id = c.id
            JOIN edu_sections sec ON e.section_id = sec.id
            WHERE $where
            ORDER BY c.name, sec.name, e.exam_date, e.start_time";
            
    $res = $conn->query($sql);
    $groups = [];
    $first_type = $type;
    $first_year = $year;

    while($row = $res->fetch_assoc()) {
        $key = $row['class_name'] . ' ' . $row['section_name'];
        if(!isset($groups[$key])) $groups[$key] = [];
        $groups[$key][] = $row;
        if(!$first_type) $first_type = $row['exam_type'];
        if(!$first_year) $first_year = $row['academic_year'];
    }
    
    ?>
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Date Sheet</title>
        <link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;600;700&family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="../../assets/vendor/fontawesome/all.min.css">
        <script src="../../assets/vendor/tailwind/tailwind.js"></script>
        <style>
             :root {
                --royal-blue: #2c52a0;
                --bright-yellow: #f7c600;
                --cream-soft: #fbf9f1;
                --text-main: #1e293b;
            }
            body { 
                font-family: 'Inter', sans-serif;
                background: white;
                color: var(--text-main);
                margin: 0;
                padding: 20px; 
            }
            @media print {
                * {
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                    color-adjust: exact !important;
                }
                .no-print { display: none !important; }
                body { padding: 0; }
                table { page-break-inside: auto; }
                tr { page-break-inside: avoid; page-break-after: auto; }
                thead { display: table-header-group; }
                tfoot { display: table-footer-group; }
            }

            /* Header Design */
            .header-modern {
                height: 130px; 
                border-bottom: 5px solid var(--royal-blue);
                display: flex;
                position: relative;
                overflow: hidden;
                margin-bottom: 30px;
            }
            .header-blue-zone {
                flex: 4.5;
                background: var(--royal-blue);
                color: white;
                padding: 25px 40px; /* Reduced padding for fit */
                position: relative;
                clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
                display: flex;
                flex-direction: column;
                justify-content: center;
                z-index: 1;
            }
            .header-white-zone {
                flex: 5.5;
                padding: 10px 20px;
                text-align: right;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: flex-end;
                position: relative;
                z-index: 2;
            }
            .school-logo-img {
                width: 80px; 
                height: 80px;
                object-fit: contain;
                position: absolute;
                top: 15px;
                right: 30px;
                z-index: 20;
            }
            .header-info-content {
                margin-right: 90px; 
                text-align: right;
                z-index: 10;
                position: relative;
            }
            .header-info-content strong {
                font-size: 17px;
                color: var(--royal-blue);
                text-transform: uppercase;
                display: block;
                font-weight: 900;
                line-height: 1.1;
            }
            .header-info-content p {
                font-size: 10px;
                margin: 4px 0 0;
                color: #000;
                font-weight: 700;
            }
            .header-blue-tip {
                position: absolute;
                bottom: 0;
                right: 0;
                width: 120px;
                height: 50px;
                background: var(--royal-blue);
                clip-path: polygon(100% 0, 100% 100%, 0 100%);
                z-index: 1;
            }

            /* Content Tables */
            .main-layout { width: 100%; border-collapse: collapse; }
            .class-block { margin-bottom: 40px; break-inside: avoid; }
            .class-title {
                font-size: 18px;
                font-weight: 800;
                color: var(--royal-blue);
                border-left: 5px solid var(--bright-yellow);
                padding-left: 10px;
                margin-bottom: 10px;
                text-transform: uppercase;
            }
            .ds-table {
                width: 100%;
                border-collapse: collapse;
                font-size: 12px;
                margin-bottom: 10px;
            }
            .ds-table th {
                background: #f1f5f9;
                color: var(--royal-blue);
                padding: 8px;
                text-align: left;
                border: 1px solid #e2e8f0;
                text-transform: uppercase;
                font-weight: 800;
            }
            .ds-table td {
                padding: 8px;
                border: 1px solid #e2e8f0;
                color: #334155;
            }
        </style>
    </head>
    <body>
        <div class="no-print" style="margin-bottom: 30px; padding: 25px; background: #fff; border-bottom: 2px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);">
            <div style="max-width: 1200px; margin: 0 auto; display: flex; flex-wrap: wrap; align-items: flex-end; gap: 15px; justify-content: center;">
                
                <div style="flex: 1; min-width: 150px;">
                    <label style="display: block; font-size: 11px; font-weight: 700; color: #64748b; margin-bottom: 5px; text-transform: uppercase;">Exam Type</label>
                    <select id="filter_type" onchange="applyFilters()" style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; color: #1e293b; background-color: #f8fafc;">
                        <option value="">All Types</option>
                        <?php while($t = $opt_types->fetch_assoc()): ?>
                            <option value="<?php echo htmlspecialchars($t['exam_type']); ?>" <?php if($type == $t['exam_type']) echo 'selected'; ?>>
                                <?php echo htmlspecialchars($t['exam_type']); ?>
                            </option>
                        <?php endwhile; ?>
                    </select>
                </div>

                <div style="flex: 1; min-width: 150px;">
                    <label style="display: block; font-size: 11px; font-weight: 700; color: #64748b; margin-bottom: 5px; text-transform: uppercase;">Academic Year</label>
                    <select id="filter_year" onchange="applyFilters()" style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; color: #1e293b; background-color: #f8fafc;">
                        <option value="">All Years</option>
                        <?php while($y = $opt_years->fetch_assoc()): ?>
                            <option value="<?php echo htmlspecialchars($y['academic_year']); ?>" <?php if($year == $y['academic_year']) echo 'selected'; ?>>
                                <?php echo htmlspecialchars($y['academic_year']); ?>
                            </option>
                        <?php endwhile; ?>
                    </select>
                </div>

                <div style="flex: 1; min-width: 150px;">
                    <label style="display: block; font-size: 11px; font-weight: 700; color: #64748b; margin-bottom: 5px; text-transform: uppercase;">Semester</label>
                    <select id="filter_semester" onchange="applyFilters()" style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; color: #1e293b; background-color: #f8fafc;">
                        <option value="">All Semesters</option>
                        <?php while($s = $opt_semesters->fetch_assoc()): ?>
                            <option value="<?php echo htmlspecialchars($s['semester']); ?>" <?php if($semester == $s['semester']) echo 'selected'; ?>>
                                <?php echo htmlspecialchars($s['semester']); ?>
                            </option>
                        <?php endwhile; ?>
                    </select>
                </div>

                <div style="flex: 1; min-width: 150px;">
                    <label style="display: block; font-size: 11px; font-weight: 700; color: #64748b; margin-bottom: 5px; text-transform: uppercase;">Class</label>
                    <select id="filter_class" onchange="updateSections(); applyFilters();" style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; color: #1e293b; background-color: #f8fafc;">
                        <option value="0">All Classes</option>
                        <?php while($c = $opt_classes->fetch_assoc()): ?>
                            <option value="<?php echo $c['id']; ?>" <?php if($cid == $c['id']) echo 'selected'; ?>>
                                <?php echo htmlspecialchars($c['name']); ?>
                            </option>
                        <?php endwhile; ?>
                    </select>
                </div>

                <div style="flex: 1; min-width: 150px;">
                    <label style="display: block; font-size: 11px; font-weight: 700; color: #64748b; margin-bottom: 5px; text-transform: uppercase;">Section</label>
                    <select id="filter_section" onchange="applyFilters()" style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; color: #1e293b; background-color: #f8fafc;">
                        <option value="0" data-class="0">All Sections</option>
                        <?php while($s = $opt_sections->fetch_assoc()): ?>
                            <option value="<?php echo $s['id']; ?>" data-class="<?php echo $s['class_id']; ?>" <?php if($sid == $s['id']) echo 'selected'; ?> <?php if($cid > 0 && $cid != $s['class_id']) echo 'style="display:none;"'; ?>>
                                <?php echo htmlspecialchars($s['name']); ?>
                            </option>
                        <?php endwhile; ?>
                    </select>
                </div>

                <div style="flex: 1; min-width: 150px;">
                    <label style="display: block; font-size: 11px; font-weight: 700; color: #64748b; margin-bottom: 5px; text-transform: uppercase;">Shift</label>
                    <select id="filter_shift" onchange="applyFilters()" style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; color: #1e293b; background-color: #f8fafc;">
                        <option value="">All Shifts</option>
                        <option value="Morning" <?php if($shift == 'Morning') echo 'selected'; ?>>Morning</option>
                        <option value="Evening" <?php if($shift == 'Evening') echo 'selected'; ?>>Evening</option>
                    </select>
                </div>

                <div style="display: flex; gap: 10px;">
                    <button onclick="window.print()" style="background: var(--royal-blue); color: white; padding: 10px 20px; border-radius: 8px; font-weight: 700; cursor: pointer; border: none; font-size: 13px; transition: all 0.2s; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 6px -1px rgba(44, 82, 160, 0.3);">
                        <i class="fas fa-print"></i> PRINT
                    </button>
                    <button onclick="location.href='?action=print_datesheet'" style="background: #f1f5f9; color: #475569; padding: 10px 15px; border-radius: 8px; font-weight: 600; cursor: pointer; border: 1px solid #e2e8f0; font-size: 13px; transition: all 0.2s;">
                        <i class="fas fa-sync-alt"></i> RESET
                    </button>
                </div>
            </div>
            <?php if(empty($groups)): ?>
                <div style="margin-top: 20px; padding: 12px; background: #fee2e2; border: 1px solid #fecaca; border-radius: 8px; color: #b91c1c; font-size: 14px; font-weight: 500;">
                    <i class="fas fa-exclamation-circle"></i> No exams found for the selected criteria. Try adjusting your filters.
                </div>
            <?php endif; ?>
        </div>

        <script>
            function updateSections() {
                const classId = document.getElementById('filter_class').value;
                const sectionSelect = document.getElementById('filter_section');
                const options = sectionSelect.options;
                
                let foundAny = false;
                for (let i = 0; i < options.length; i++) {
                    const optClass = options[i].getAttribute('data-class');
                    if (classId === '0' || optClass === '0' || optClass === classId) {
                        options[i].style.display = 'block';
                        foundAny = true;
                    } else {
                        options[i].style.display = 'none';
                        if (options[i].selected) {
                            sectionSelect.value = '0';
                        }
                    }
                }
            }

            function applyFilters() {
                const type = document.getElementById('filter_type').value;
                const year = document.getElementById('filter_year').value;
                const semester = document.getElementById('filter_semester').value;
                const classId = document.getElementById('filter_class').value;
                const sectionId = document.getElementById('filter_section').value;
                const shift = document.getElementById('filter_shift').value;
                
                let url = '?action=print_datesheet';
                if(type) url += '&type=' + encodeURIComponent(type);
                if(year) url += '&year=' + encodeURIComponent(year);
                if(semester) url += '&semester=' + encodeURIComponent(semester);
                if(classId && classId != '0') url += '&class_id=' + classId;
                if(sectionId && sectionId != '0') url += '&section_id=' + sectionId;
                if(shift) url += '&shift=' + encodeURIComponent(shift);
                
                window.location.href = url;
            }
        </script>

        <table class="main-layout">
            <thead>
                <tr>
                    <td>
                        <div class="header-modern">
                            <div class="header-blue-zone">
                                <h1 style="font-size: 28px; font-weight: 900; margin: 0; line-height: 1;">DATE SHEET</h1>
                                <p style="margin: 5px 0 0; font-size: 14px; opacity: 0.9; font-weight: 500; text-transform: uppercase;">
                                    <?php echo htmlspecialchars($first_type . ' ' . $first_year); ?>
                                </p>
                            </div>
                            <div class="header-white-zone">
                                <?php if(!empty($inst['logo_path'])): ?>
                                    <img src="../../<?php echo $inst['logo_path']; ?>" class="school-logo-img" alt="Logo">
                                <?php endif; ?>
                                <div class="header-info-content">
                                    <strong><?php echo htmlspecialchars($inst['name']); ?></strong>
                                    <p><?php echo htmlspecialchars($inst['address']); ?></p>
                                    <p><?php echo htmlspecialchars($inst['phone']); ?></p>
                                </div>
                                <div class="header-blue-tip"></div>
                            </div>
                        </div>
                    </td>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td style="padding-top: 20px;">
                        <?php foreach($groups as $className => $exams): ?>
                            <div class="class-block">
                                <div class="class-title"><?php echo htmlspecialchars($className); ?></div>
                                <table class="ds-table">
                                    <thead>
                                        <tr>
                                            <th style="width: 15%;">Date & Day</th>
                                            <th style="width: 30%;">Subject</th>
                                            <th style="width: 20%;">Start Time</th>
                                            <th style="width: 20%;">End Time</th>
                                            <th style="width: 15%;">Shift</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php foreach($exams as $ex): ?>
                                            <tr>
                                                <td style="font-weight: 600;">
                                                    <?php echo date('d M Y', strtotime($ex['exam_date'])); ?><br>
                                                    <span style="font-size: 10px; font-weight: normal; color: #64748b; text-transform: uppercase;"><?php echo date('l', strtotime($ex['exam_date'])); ?></span>
                                                </td>
                                                <td style="font-weight: 700; color: var(--royal-blue);"><?php echo htmlspecialchars($ex['subject']); ?></td>
                                                <td><?php echo date('h:i A', strtotime($ex['start_time'])); ?></td>
                                                <td><?php echo !empty($ex['end_time']) ? date('h:i A', strtotime($ex['end_time'])) : '--:-- --'; ?></td>
                                                <td>
                                                    <span style="padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 700; background: <?php echo $ex['shift']=='Morning'?'#fff7ed':'#eff6ff'; ?>; color: <?php echo $ex['shift']=='Morning'?'#c2410c':'#1e40af'; ?>;">
                                                        <?php echo $ex['shift']; ?>
                                                    </span>
                                                </td>
                                            </tr>
                                        <?php endforeach; ?>
                                    </tbody>
                                </table>
                            </div>
                        <?php endforeach; ?>
                    </td>
                </tr>
            </tbody>
            <tfoot>
                <tr>
                    <td style="padding-top: 50px;">
                        <div style="display: flex; justify-content: space-between; padding: 0 50px;">
                            <div style="text-align: center;">
                                <div style="border-top: 2px solid #000; width: 200px; padding-top: 5px; font-weight: 800; font-size: 12px; text-transform: uppercase;">Controller of Examinations</div>
                            </div>
                            <div style="text-align: center;">
                                <div style="border-top: 2px solid #000; width: 200px; padding-top: 5px; font-weight: 800; font-size: 12px; text-transform: uppercase;">Principal</div>
                            </div>
                        </div>
                        <!-- Footer Repeat Spacer -->
                         <div style="height: 30px;"></div>
                    </td>
                </tr>
            </tfoot>
        </table>
        
        <script>
            // Auto print if bulk mode? No, manual.
        </script>
    </body>
    </html>
    <?php
    exit;

} elseif ($action == 'generate_attendance_sheet') {
    // Printable Attendance Sheet
    header('Content-Type: text/html');
    
    $date = $conn->real_escape_string($_POST['date']);
    $shift = $conn->real_escape_string($_POST['shift']);
    $sub_id = (int)$_POST['subject_id'];
    
    // Get Exam Context & Institution Info
    $inst = $conn->query("SELECT name FROM edu_institutions WHERE id = $inst_id")->fetch_assoc();
    $sub_name = $conn->query("SELECT name FROM edu_subjects WHERE id = $sub_id")->fetch_row()[0];
    
    // Get Students allocated to this exam
    // Filter by Class/Section if provided
    $cid_filter = isset($_POST['class_id']) && !empty($_POST['class_id']) ? "AND e.class_id = ".(int)$_POST['class_id'] : "";
    $sid_filter = isset($_POST['section_id']) && !empty($_POST['section_id']) ? "AND e.section_id = ".(int)$_POST['section_id'] : "";
    
    $e_sql = "SELECT e.class_id, e.section_id, c.name as class_name, s.name as section_name, e.exam_type 
              FROM edu_exams e 
              JOIN edu_classes c ON e.class_id = c.id
              JOIN edu_sections s ON e.section_id = s.id 
              WHERE e.institution_id = $inst_id AND e.exam_date = '$date' AND e.shift = '$shift' AND e.subject_id = $sub_id $cid_filter $sid_filter";
    $exams_q = $conn->query($e_sql);
    
    if($exams_q->num_rows == 0) { echo "No exams found."; exit; }
    
    $exam_type = "";
    $all_students = [];
    
    while($ex = $exams_q->fetch_assoc()) {
        $exam_type = $ex['exam_type'];
        $cid = $ex['class_id'];
        $sid = $ex['section_id'];
        
        // Fetch Students
        $s_sql = "SELECT u.full_name, e.roll_number, e.class_no 
                  FROM edu_student_enrollment e 
                  JOIN edu_users u ON e.student_id = u.id 
                  WHERE e.class_id = $cid AND e.section_id = $sid 
                  ORDER BY e.roll_number";
        $res = $conn->query($s_sql);
        while($stu = $res->fetch_assoc()) {
            if ($role === 'guest') {
                $stu['full_name'] = '?';
                $stu['roll_number'] = '?';
                $stu['class_no'] = '?';
            }
            $stu['class'] = $ex['class_name'] . ' ' . $ex['section_name'];
            $all_students[] = $stu;
        }
    }
    
    // Get Institution Details
    $inst = $conn->query("SELECT * FROM edu_institutions WHERE id = $inst_id")->fetch_assoc();

    // HTML Output
    ?>
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Attendance Sheet - <?php echo $sub_name; ?></title>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="../../assets/vendor/fontawesome/all.min.css">
        <style>
             :root {
                --royal-blue: #2c52a0;
                --bright-yellow: #f7c600;
                --text-main: #1e293b;
            }
            body { 
                font-family: 'Inter', sans-serif;
                background: white;
                color: var(--text-main);
                margin: 0;
                padding: 2.5cm 1cm 1cm; /* Margins for hole punching / binding */
                position: relative;
            }
            @media print {
                .no-print { display: none !important; }
                body { padding: 0; }
                .sheet-page { margin-bottom: 0; page-break-after: always; }
            }

            /* Royal Header */
            .header-modern {
                height: 120px; 
                border-bottom: 5px solid var(--royal-blue);
                display: flex;
                position: relative;
                overflow: hidden;
                margin-bottom: 30px;
            }
            .header-blue-zone {
                flex: 6;
                background: var(--royal-blue);
                color: white;
                padding: 15px 40px;
                clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
                display: flex;
                flex-direction: column;
                justify-content: center;
            }
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
            .school-logo-img {
                width: 70px; height: 70px;
                object-fit: contain;
                position: absolute;
                top: 15px; right: 30px;
                z-index: 20;
            }
            .header-info-content { margin-right: 90px; text-align: right; }
            .header-info-content strong {
                font-size: 20px; color: var(--royal-blue);
                text-transform: uppercase; display: block;
                font-weight: 900; line-height: 1.1;
            }
            .header-info-content p { font-size: 10px; margin: 3px 0; color: #444; font-weight: 600; }

            /* Table Styles */
            .att-table { 
                width: 100%; border-collapse: collapse; 
                border: 2px solid var(--royal-blue);
            }
            .att-table th {
                background: var(--royal-blue);
                color: white;
                padding: 10px 8px;
                text-align: left;
                font-size: 11px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                border: 1px solid rgba(255,255,255,0.1);
            }
            .att-table td {
                padding: 8px;
                border: 1px solid #cbd5e1;
                font-size: 12px;
                font-weight: 600;
                color: #1e293b;
            }
            .att-table tr:nth-child(even) { background: #f8fafc; }

            .meta-strip {
                background: #f1f5f9;
                padding: 10px 15px;
                border-radius: 6px;
                margin-bottom: 20px;
                display: flex;
                justify-content: space-between;
                font-size: 12px;
                font-weight: 800;
                color: var(--royal-blue);
                border: 1.5px solid #e2e8f0;
            }

            .sig-area {
                margin-top: 50px;
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 50px;
            }
            .sig-box {
                border-top: 2px solid #000;
                padding-top: 5px;
                text-align: center;
                font-size: 11px;
                font-weight: 900;
                text-transform: uppercase;
            }
        </style>
    </head>
    <body>
        <div class="no-print" style="position:fixed; top:20px; left:20px; z-index:1000;">
            <button onclick="window.print()" style="background:var(--royal-blue); color:white; padding:12px 25px; border:none; border-radius:8px; font-weight:bold; cursor:pointer; box-shadow:0 10px 20px rgba(0,0,0,0.2);">
                <i class="fas fa-print"></i> PRINT ATTENDANCE SHEET
            </button>
        </div>

        <div class="header-modern">
            <div class="header-blue-zone">
                <h1 style="font-size: 24px; font-weight: 900; margin: 0; line-height: 1;">EXAM ATTENDANCE SHEET</h1>
                <p style="margin: 5px 0 0; font-size: 12px; opacity: 0.9; font-weight: 500; text-transform: uppercase;">
                    Academic Accountability Report • Exam Cycle 2024
                </p>
            </div>
            <div class="header-white-zone">
                <?php if($inst['logo_path']): ?>
                    <img src="../../<?php echo $inst['logo_path']; ?>" class="school-logo-img">
                <?php endif; ?>
                <div class="header-info-content">
                    <strong><?php echo $inst['name']; ?></strong>
                    <p><?php echo $inst['address']; ?></p>
                    <p><?php echo $inst['phone']; ?></p>
                </div>
                <div class="header-blue-tip"></div>
            </div>
        </div>

        <div class="meta-strip">
            <span>EXAM: <?php echo strtoupper($exam_type); ?></span>
            <span>SUBJECT: <?php echo $sub_name; ?></span>
            <span>SHIFT: <?php echo $shift; ?></span>
            <span>DATE: <?php echo date('d-M-Y', strtotime($date)); ?></span>
        </div>
        
        <table class="att-table">
            <thead>
                <tr>
                    <th style="width:40px; text-align:center;">S#</th>
                    <th style="width:80px; text-align:center;">C.NO</th>
                    <th style="width:90px; text-align:center;">ROLL NO</th>
                    <th>STUDENT NAME</th>
                    <th>CLASS & SECTION</th>
                    <th style="width:180px;">SIGNATURE</th>
                </tr>
            </thead>
            <tbody>
                <?php 
                $sn = 1;
                foreach($all_students as $s): 
                ?>
                <tr>
                    <td style="text-align:center;"><?php echo $sn++; ?></td>
                    <td style="text-align:center;"><?php echo htmlspecialchars($s['class_no'] ?? '-'); ?></td>
                    <td style="text-align:center; color:var(--royal-blue); font-weight:800;"><?php echo htmlspecialchars($s['roll_number']); ?></td>
                    <td><?php echo htmlspecialchars($s['full_name']); ?></td>
                    <td><?php echo htmlspecialchars($s['class']); ?></td>
                    <td style="background: white;"></td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
        
        <div class="sig-area">
            <div class="sig-box">Invigilator</div>
            <div class="sig-box">Superintendent</div>
            <div class="sig-box">Controller Exam</div>
        </div>

        <div style="margin-top: 30px; border-top: 1px dotted #ccc; padding-top: 10px; font-size: 9px; color: #666; text-align: center;">
            Document Generated: <?php echo date('d-M-Y H:i'); ?> | System Secure Copy | Page 1 of 1
        </div>
    </body>
    </html>
    <?php
    exit;

} elseif ($action == 'generate_hall_view') {
    // Return HTML output directly
    header('Content-Type: text/html');
    
    $date = $conn->real_escape_string($_POST['date']);
    $shift = $conn->real_escape_string($_POST['shift']);
    $sub_id = (int)$_POST['subject_id'];
    $rooms = json_decode($_POST['rooms'], true);
    $staff = json_decode($_POST['staff'], true);
    
    // 1. Get Exams to identify classes/sections
    $e_sql = "SELECT e.id, e.class_id, e.section_id, c.name as class_name, s.name as subject_name 
              FROM edu_exams e 
              JOIN edu_classes c ON e.class_id = c.id
              JOIN edu_subjects s ON e.subject_id = s.id
              WHERE e.institution_id = $inst_id AND e.exam_date = '$date' AND e.shift = '$shift' AND e.subject_id = $sub_id";
    $exams_q = $conn->query($e_sql);
    
    if($exams_q->num_rows == 0) { echo "<h3>No exams found for this selection.</h3>"; exit; }
    
    $subject_name = '';
    $classes_involved = [];
    $all_students = []; // {name, roll, class}
    
    while($ex = $exams_q->fetch_assoc()) {
        $subject_name = $ex['subject_name'];
         if(!in_array($ex['class_name'], $classes_involved)) $classes_involved[] = $ex['class_name'];
         
         $cid = $ex['class_id'];
         $sid = $ex['section_id'];
         
         // Fetch Students
         $s_sql = "SELECT u.full_name, st.roll_number 
                   FROM edu_student_enrollment st 
                   JOIN edu_users u ON st.student_id = u.id 
                   WHERE st.class_id = $cid AND st.section_id = $sid 
                   ORDER BY st.roll_number";
         $s_res = $conn->query($s_sql);
         $s_res = $conn->query($s_sql);
         while($stu = $s_res->fetch_assoc()) {
             $all_students[] = [
                 'name' => ($role==='guest') ? '?' : $stu['full_name'],
                 'roll' => ($role==='guest') ? '?' : $stu['roll_number'],
                 'class' => $ex['class_name']
             ];
         }
    }
    
    // 2. Allocation Logic
    $total_students = count($all_students);
    $total_capacity = array_reduce($rooms, function($c, $r){ return $c + (int)$r['cap']; }, 0);
    
    // Get Institution Details
    $inst = $conn->query("SELECT * FROM edu_institutions WHERE id = $inst_id")->fetch_assoc();

    ?>
    <style>
         :root {
            --royal-blue: #2c52a0;
            --bright-yellow: #f7c600;
            --cream-soft: #fbf9f1;
            --text-main: #1e293b;
        }
        .royal-print-container {
            font-family: 'Inter', sans-serif;
            background: white;
            color: var(--text-main);
            padding: 20px;
        }
        /* Royal Header */
        .header-modern-inner {
            height: 100px; 
            border-bottom: 4px solid var(--royal-blue);
            display: flex;
            position: relative;
            overflow: hidden;
            margin-bottom: 25px;
        }
        .header-blue-zone-inner {
            flex: 6; background: var(--royal-blue); color: white;
            padding: 10px 30px; clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%);
            display: flex; flex-direction: column; justify-content: center;
        }
        .header-white-zone-inner {
            flex: 4; text-align: right; display: flex; flex-direction: column; justify-content: center;
            align-items: flex-end; position: relative; padding: 10px;
        }
        .logo-inner { width: 60px; height: 60px; object-fit: contain; }

        .room-block {
            border: 2px solid var(--royal-blue);
            border-radius: 12px;
            overflow: hidden;
            margin-bottom: 25px;
            break-inside: avoid;
        }
        .room-header {
            background: var(--royal-blue);
            color: white;
            padding: 10px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-weight: 800;
            font-size: 14px;
            text-transform: uppercase;
        }
        .room-table { width: 100%; border-collapse: collapse; }
        .room-table th { background: #f1f5f9; color: var(--royal-blue); padding: 8px; text-align: left; font-size: 11px; text-transform: uppercase; border-bottom: 2px solid #e2e8f0; }
        .room-table td { padding: 8px; border-bottom: 1px solid #f1f5f9; font-size: 12px; font-weight: 600; }
        
        @media print { .no-print { display: none !important; } }
    </style>

    <div class="royal-print-container">
        <div class="header-modern-inner">
            <div class="header-blue-zone-inner">
                <h1 style="font-size: 20px; font-weight: 900; margin: 0; text-transform: uppercase;">HALL ARRANGEMENT</h1>
                <p style="margin: 3px 0 0; font-size: 11px; opacity: 0.8; font-weight: 500;">SUBJECT: <?php echo strtoupper($subject_name); ?> • <?php echo $date; ?></p>
            </div>
            <div class="header-white-zone-inner">
                <?php if($inst['logo_path']): ?>
                    <img src="../../<?php echo $inst['logo_path']; ?>" class="logo-inner">
                <?php endif; ?>
                <div style="text-align: right; margin-right: 15px;">
                    <strong style="color: var(--royal-blue); font-size: 14px;"><?php echo $inst['name']; ?></strong>
                </div>
            </div>
        </div>

        <?php if($total_capacity < $total_students): ?>
            <div style="background: #fee2e2; border: 1px solid #fecaca; color: #dc2626; padding: 10px; border-radius: 8px; font-size: 12px; font-weight: 700; margin-bottom: 20px;">
                <i class="fas fa-exclamation-triangle"></i> ALERT: Insufficient capacity. Need more desks for students.
            </div>
        <?php endif; ?>

        <?php 
        $student_idx = 0;
        $staff_idx = 0;
        foreach($rooms as $room): 
            if($student_idx >= $total_students) break;
            $r_name = htmlspecialchars($room['name']);
            $r_cap = (int)$room['cap'];
            $assigned_staff = [];
            if($staff_idx < count($staff)) {
                $assigned_staff[] = $staff[$staff_idx];
                $staff_idx = ($staff_idx + 1) % count($staff);
            }
            $s_label = implode(', ', array_map(function($s){ return $s['name']; }, $assigned_staff));
        ?>
            <div class="room-block">
                <div class="room-header">
                    <span>ROOM: <?php echo $r_name; ?></span>
                    <span style="background:rgba(255,255,255,0.2); padding:4px 10px; border-radius:6px; font-size:11px;">INVIGILATOR: <?php echo $s_label; ?></span>
                </div>
                <table class="room-table">
                    <thead>
                        <tr>
                            <th style="width:100px;">ROLL NO</th>
                            <th>STUDENT NAME</th>
                            <th style="width:150px;">CLASS</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php 
                        $count = 0;
                        while($count < $r_cap && $student_idx < $total_students): 
                            $stu = $all_students[$student_idx];
                        ?>
                            <tr>
                                <td style="color:var(--royal-blue); font-weight:800;"><?php echo $stu['roll']; ?></td>
                                <td><?php echo $stu['name']; ?></td>
                                <td style="opacity:0.6; font-size:11px;"><?php echo $stu['class']; ?></td>
                            </tr>
                        <?php 
                            $student_idx++;
                            $count++;
                        endwhile; 
                        ?>
                    </tbody>
                </table>
                <div style="background:#f8fafc; padding:8px 20px; text-align:right; font-size:10px; font-weight:800; color:#64748b;">
                    ALLOCATION: <?php echo $count; ?> STUDENTS
                </div>
            </div>
        <?php endforeach; ?>

        <div class="no-print" style="text-align:center; margin-top:30px;">
            <button onclick="window.print()" style="background:var(--royal-blue); color:white; padding:12px 30px; border:none; border-radius:8px; font-weight:800; cursor:pointer; box-shadow:0 10px 20px rgba(44, 82, 160, 0.2);">
                <i class="fas fa-print"></i> PRINT ARRANGEMENT
            </button>
        </div>
    </div>
    <?php
    exit;
 // Important since we changed header

} elseif ($action == 'get_exams') {
    try {
        $cid = (int)($_GET['class_id'] ?? 0);
        $sid = (int)($_GET['section_id'] ?? 0);
        $type = isset($_GET['exam_type']) ? $conn->real_escape_string($_GET['exam_type']) : '';
        $year = isset($_GET['year']) ? $conn->real_escape_string($_GET['year']) : '';

        $where = "e.institution_id = $inst_id";
        if($is_staff) {
            $conditions = [];
            $res_asgn = $conn->query("SELECT class_id, section_id, subject_id FROM edu_timetable WHERE staff_id = $uid AND institution_id = $inst_id");
            while($r = $res_asgn->fetch_assoc()) {
                $conditions[] = "(e.class_id = {$r['class_id']} AND e.section_id = {$r['section_id']} AND e.subject_id = {$r['subject_id']})";
            }
            if(!empty($conditions)) {
                $where .= " AND (" . implode(" OR ", $conditions) . ")";
            } else {
                $where .= " AND 1=0";
            }
        }

        if($cid > 0) $where .= " AND e.class_id = $cid";
        if($sid > 0) $where .= " AND e.section_id = $sid";
        if(!empty($type)) $where .= " AND e.exam_type = '$type'";
        if(!empty($year)) $where .= " AND e.academic_year = '$year'";

        $sql = "SELECT e.*, s.name as subject_name FROM edu_exams e 
                JOIN edu_subjects s ON e.subject_id = s.id 
                WHERE $where ORDER BY e.exam_date DESC";
        
        $res = $conn->query($sql);
        if (!$res) {
            echo json_encode(['error' => $conn->error, 'sql' => $sql]);
            exit;
        }
        
        $exams = [];
        while($row = $res->fetch_assoc()) $exams[] = $row;
        echo json_encode($exams);
    } catch (Exception $e) {
        echo json_encode(['error' => 'Exception: ' . $e->getMessage()]);
    }

} elseif ($action == 'get_students_for_marks') {
    $eid = (int)$_GET['exam_id'];
    
    // Get exam details first
    $exam = $conn->query("SELECT * FROM edu_exams WHERE id = $eid")->fetch_assoc();
    
    if(!$exam) {
        echo json_encode(['students' => [], 'total_marks' => 0, 'error' => 'Exam not found', 'exam_id' => $eid]);
        exit;
    }
    
    $cid = $exam['class_id'];
    $sid = $exam['section_id'];

    // Join with enrollment to get students of that class/section
    // Load ALL students in this class/section regardless of status or year
    $sql = "SELECT e.student_id, u.full_name, e.roll_number, e.class_no, 
            COALESCE(m.obtain_marks, '') as marks, COALESCE(m.status, 'Present') as status
            FROM edu_student_enrollment e
            JOIN edu_users u ON e.student_id = u.id
            LEFT JOIN edu_exam_marks m ON e.student_id = m.student_id AND m.exam_id = $eid
            WHERE e.class_id = $cid AND e.section_id = $sid
            ORDER BY e.roll_number ASC";
    
    $res = $conn->query($sql);
    
    if(!$res) {
        echo json_encode([
            'students' => [], 
            'total_marks' => $exam['total_marks'],
            'error' => 'Query failed: ' . $conn->error,
            'sql' => $sql,
            'class_id' => $cid,
            'section_id' => $sid
        ]);
        exit;
    }
    
    $students = [];
    while($row = $res->fetch_assoc()) {
        if ($role === 'guest') {
            $row['full_name'] = '?';
            $row['marks'] = '?';
        }
        $students[] = $row;
    }
    
    echo json_encode([
        'students' => $students, 
        'total_marks' => $exam['total_marks'],
        'debug' => [
            'exam_id' => $eid,
            'class_id' => $cid,
            'section_id' => $sid,
            'student_count' => count($students),
            'query' => $sql
        ]
    ]);

} elseif ($action == 'save_marks') {
    // Start output buffering to catch any stray warnings/errors
    ob_start();
    header('Content-Type: application/json');

    try {
        authorize(['admin', 'staff']);
        
        if(!isset($_POST['exam_id']) || !isset($_POST['marks'])) {
            throw new Exception("Missing required parameters");
        }

        $eid = (int)$_POST['exam_id'];
        $marks_data = json_decode($_POST['marks'], true);

        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception("Invalid JSON data for marks");
        }
        
        // Fetch Exam Info once
        $ex_query = $conn->query("SELECT e.exam_type, s.name as sub_name 
                                FROM edu_exams e 
                                JOIN edu_subjects s ON e.subject_id = s.id 
                                WHERE e.id = $eid");
        
        if(!$ex_query) throw new Exception("Database Error: " . $conn->error);
        
        $ex_info = $ex_query->fetch_assoc();
        if(!$ex_info) throw new Exception("Exam not found");

        $sub_name = $ex_info['sub_name'] ?? 'Exam';
        $ex_type = $ex_info['exam_type'] ?? 'Test';

        $conn->begin_transaction();

        foreach($marks_data as $m) {
            $sid = (int)$m['student_id'];
            $val = (float)$m['marks'];
            $status = $conn->real_escape_string($m['status'] ?? 'Present');
            
            // Use INSERT ON DUPLICATE KEY UPDATE
            // Ensure unique constraint on (exam_id, student_id)
            $stmt = $conn->prepare("INSERT INTO edu_exam_marks (exam_id, student_id, obtain_marks, status) 
                          VALUES (?, ?, ?, ?) 
                          ON DUPLICATE KEY UPDATE obtain_marks = VALUES(obtain_marks), status = VALUES(status)");
            if(!$stmt) throw new Exception("Prepare failed: " . $conn->error);

            $stmt->bind_param("iids", $eid, $sid, $val, $status);
            
            if (!$stmt->execute()) {
                throw new Exception("Execute failed for Student ID $sid: " . $stmt->error);
            }
            $stmt->close();
            
            // Notify Student & Parents (Silently ignore notification errors to avoid breaking save)
            try {
                $msg = "Result Alert: Marks for $sub_name ($ex_type) have been recorded. Obtained: $val. Status: $status";
                notify_student_and_parents($conn, $sid, "Exam Result Updated", $msg, 'info');
            } catch(Exception $e_notif) {
                // Ignore notification failure
            }
        }

        $conn->commit();
        
        // Clear buffer and output success
        ob_clean();
        echo json_encode(['status' => 'success']);

    } catch (Exception $e) {
        $conn->rollback();
        // Clear buffer and output error
        ob_clean();
        echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
    }
} elseif ($action == 'save_student_consolidated_marks') {
    ob_start();
    header('Content-Type: application/json');
    try {
        authorize(['admin', 'staff']);
        if(!isset($_POST['student_id']) || !isset($_POST['marks_data'])) {
            throw new Exception("Missing parameters");
        }
        $sid = (int)$_POST['student_id'];
        $marks_received = json_decode($_POST['marks_data'], true);
        if(!$marks_received) throw new Exception("Invalid marks data");

        $conn->begin_transaction();
        $stmt = $conn->prepare("INSERT INTO edu_exam_marks (exam_id, student_id, obtain_marks, status) 
                              VALUES (?, ?, ?, ?) 
                              ON DUPLICATE KEY UPDATE obtain_marks = VALUES(obtain_marks), status = VALUES(status)");
        
        foreach($marks_received as $m) {
            $eid = (int)$m['exam_id'];
            $val = (float)$m['marks'];
            $status = 'Present'; // Default for consolidated edits
            $stmt->bind_param("iids", $eid, $sid, $val, $status);
            if(!$stmt->execute()) throw new Exception("Execute failed: " . $stmt->error);
        }
        $stmt->close();
        $conn->commit();
        ob_clean();
        echo json_encode(['status' => 'success']);
    } catch(Exception $e) {
        if(isset($conn) && $conn->connect_errno == 0 && $conn->query("SELECT 1") && $conn->in_transaction) $conn->rollback();
        ob_clean();
        echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
    }

} elseif ($action == 'save_student_quick_info') {
    ob_start();
    header('Content-Type: application/json');
    try {
        authorize(['admin', 'super_admin']);
        $sid = (int)$_POST['student_id'];
        $name = $conn->real_escape_string($_POST['full_name']);
        $father = $conn->real_escape_string($_POST['father_name']);
        $roll = $conn->real_escape_string($_POST['roll_number']);

        $conn->begin_transaction();
        // Update user
        $conn->query("UPDATE edu_users SET full_name='$name', father_name='$father' WHERE id=$sid");
        // Update enrollment
        $conn->query("UPDATE edu_student_enrollment SET roll_number='$roll' WHERE student_id=$sid AND (status='Active' OR status='active')");
        
        $conn->commit();
        ob_clean();
        echo json_encode(['status' => 'success']);
    } catch(Exception $e) {
        if(isset($conn) && $conn->connect_errno == 0 && $conn->query("SELECT 1") && $conn->in_transaction) $conn->rollback();
        ob_clean();
        echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
    }

} elseif ($action == 'save_text_marks') {
    ob_start();
    header('Content-Type: application/json');

    try {
        authorize(['admin', 'staff']);
        $eid = (int)$_POST['exam_id'];
        $text = $_POST['data']; // Format: "1 Ahmed 85, 2 Sara 90"
        
        // This is more complex because we need to match Roll No to Student ID
        $exam_q = $conn->query("SELECT class_id, section_id FROM edu_exams WHERE id = $eid");
        if(!$exam_q) throw new Exception("DB Error: ".$conn->error);
        
        $exam = $exam_q->fetch_assoc();
        if(!$exam) throw new Exception("Exam not found");

        $cid = $exam['class_id'];
        $sid = $exam['section_id'];
    
        $errors = [];
        $success_count = 0;
    
        $sub_name = $ex_info['sub_name'] ?? 'Exam';
        $ex_type = $ex_info['exam_type'] ?? 'Test';
        $sub_msg_part = "$sub_name ($ex_type)";

        $parts = explode(',', $text);
        
        $conn->begin_transaction();

        // Prepare statements
        $enrol_stmt = $conn->prepare("SELECT student_id FROM edu_student_enrollment WHERE class_id = ? AND section_id = ? AND roll_number = ?");
        $mark_stmt = $conn->prepare("INSERT INTO edu_exam_marks (exam_id, student_id, obtain_marks) 
                                   VALUES (?, ?, ?) 
                                   ON DUPLICATE KEY UPDATE obtain_marks = ?");

        foreach($parts as $p) {
            $p = trim($p);
            if(empty($p)) continue;

            // Split by space. We expect Roll No at start and Marks at end
            $words = preg_split('/\s+/', $p);
            if(count($words) < 2) continue;

            $roll = $words[0];
            $marks = (float)end($words);

            // Find student by roll no in this class/section
            $enrol_stmt->bind_param("iis", $cid, $sid, $roll);
            $enrol_stmt->execute();
            $s_res = $enrol_stmt->get_result();

            if($s_res->num_rows > 0) {
                $student_id = $s_res->fetch_assoc()['student_id'];
                
                $mark_stmt->bind_param("iidd", $eid, $student_id, $marks, $marks);
                if ($mark_stmt->execute()) {
                    // Notify (Silently)
                    try {
                        $msg = "Result Alert: Marks for $sub_msg_part have been updated. Obtained: $marks";
                        notify_student_and_parents($conn, $student_id, "Exam Result Updated", $msg, 'info');
                    } catch(Exception $e) { }
                    
                    $success_count++;
                }
            } else {
                $errors[] = "Roll No $roll not found in this class.";
            }
        }

        $conn->commit();
        
        ob_clean();
        echo json_encode(['status' => 'success', 'count' => $success_count, 'errors' => $errors]);

    } catch (Exception $e) {
        if(isset($conn)) $conn->rollback();
        ob_clean();
        echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
    }

} elseif ($action == 'export_csv') {
    authorize(['admin', 'staff']);
    $eid = (int)$_GET['exam_id'];
    $exam = $conn->query("SELECT e.*, s.name as subject_name, c.name as class_name 
                         FROM edu_exams e 
                         JOIN edu_subjects s ON e.subject_id = s.id 
                         JOIN edu_classes c ON e.class_id = c.id
                         WHERE e.id = $eid")->fetch_assoc();
    
    $filename = "Marks_" . $exam['class_name'] . "_" . $exam['subject_name'] . ".csv";
    
    header('Content-Type: text/csv');
    header('Content-Disposition: attachment; filename="' . $filename . '"');
    
    $output = fopen('php://output', 'w');
    fputcsv($output, ['Roll No', 'Name', 'Marks']);
    
    $sql = "SELECT e.roll_number, u.full_name, COALESCE(m.obtain_marks, '') as marks
            FROM edu_student_enrollment e
            JOIN edu_users u ON e.student_id = u.id
            LEFT JOIN edu_exam_marks m ON e.student_id = m.student_id AND m.exam_id = $eid
            WHERE e.class_id = {$exam['class_id']} AND e.section_id = {$exam['section_id']}
            ORDER BY e.roll_number ASC";
            
    $res = $conn->query($sql);
    while($row = $res->fetch_assoc()) {
        fputcsv($output, [$row['roll_number'], $row['full_name'], $row['marks']]);
    }
    fclose($output);
    exit;

} elseif ($action == 'import_csv') {
    authorize(['admin', 'staff']);
    $eid = (int)$_POST['exam_id'];
    $exam = $conn->query("SELECT class_id, section_id FROM edu_exams WHERE id = $eid")->fetch_assoc();
    $cid = $exam['class_id'];
    $sid = $exam['section_id'];

    if(isset($_FILES['csv']) && $_FILES['csv']['error'] == 0) {
        // Fetch Exam Info once
        $ex_info = $conn->query("SELECT e.exam_type, s.name as sub_name 
                                FROM edu_exams e 
                                JOIN edu_subjects s ON e.subject_id = s.id 
                                WHERE e.id = $eid")->fetch_assoc();
        $sub_msg_part = $ex_info['sub_name'] . " (" . $ex_info['exam_type'] . ")";

        $file = fopen($_FILES['csv']['tmp_name'], 'r');
        fgetcsv($file); // Skip header row
        
        while(($row = fgetcsv($file)) !== FALSE) {
            $roll = $conn->real_escape_string($row[0]);
            $marks = (float)$row[2];
            
            $s_q = $conn->query("SELECT student_id FROM edu_student_enrollment WHERE class_id = $cid AND section_id = $sid AND roll_number = '$roll'");
            if($s_q->num_rows > 0) {
                $student_id = $s_q->fetch_assoc()['student_id'];
                if ($conn->query("INSERT INTO edu_exam_marks (exam_id, student_id, obtain_marks) 
                              VALUES ($eid, $student_id, $marks) 
                              ON DUPLICATE KEY UPDATE obtain_marks = $marks")) {
                    
                    // Notify
                    $msg = "Result Alert: Marks for $sub_msg_part have been imported. Obtained: $marks";
                    notify_student_and_parents($conn, $student_id, "Exam Result Updated", $msg, 'info');
                }
            }
        }
        fclose($file);
        echo json_encode(['status' => 'success']);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'File upload error']);
    }

} elseif ($action == 'save_settings') {
    authorize(['admin']);
    $pass = (float)$_POST['passing_percentage'];
    $min = (int)$_POST['promotion_min_papers'];
    
    $chk = $conn->query("SELECT id FROM edu_exam_settings WHERE institution_id = $inst_id");
    if($chk->num_rows > 0) {
        $sql = "UPDATE edu_exam_settings SET passing_percentage = $pass, promotion_min_papers = $min WHERE institution_id = $inst_id";
    } else {
        $sql = "INSERT INTO edu_exam_settings (institution_id, passing_percentage, promotion_min_papers) VALUES ($inst_id, $pass, $min)";
    }
    
    if($conn->query($sql)) {
        echo json_encode(['status' => 'success']);
    } else {
        echo json_encode(['status' => 'error', 'message' => $conn->error]);
    }

} elseif ($action == 'get_settings') {
    $res = $conn->query("SELECT passing_percentage, promotion_min_papers FROM edu_exam_settings WHERE institution_id = $inst_id");
    if($res && $res->num_rows > 0) {
        echo json_encode(['status' => 'success', 'data' => $res->fetch_assoc()]);
    } else {
        // Return defaults
        echo json_encode(['status' => 'success', 'data' => ['passing_percentage' => 40.00, 'promotion_min_papers' => 0]]);
    }

} elseif ($action == 'get_consolidated_result') {
    try {
        $cid = (int)$_GET['class_id'];
        $sid = (int)$_GET['section_id'];
        $etype = $conn->real_escape_string($_GET['exam_type']);
        $roll = isset($_GET['roll']) ? $conn->real_escape_string($_GET['roll']) : '';

        $year = $conn->real_escape_string($_GET['year'] ?? date('Y'));

        // Safely fetch settings
        $s_query = $conn->query("SELECT * FROM edu_exam_settings WHERE institution_id = $inst_id");
        if (!$s_query) { throw new Exception("Database Error (Settings): " . $conn->error); }
        
        $settings = $s_query->fetch_assoc();
        $pass_pct = ($settings) ? ($settings['passing_percentage'] ?? 40) : 40;
        
        // Fetch Exams
        $e_sql = "SELECT e.id, e.total_marks, s.name as subject 
                  FROM edu_exams e 
                  JOIN edu_subjects s ON e.subject_id = s.id 
                  WHERE e.class_id=$cid AND e.section_id=$sid AND e.exam_type='$etype' AND e.academic_year='$year'";
        $exams_q = $conn->query($e_sql);
        
        if(!$exams_q) { throw new Exception("Database Error (Exams): " . $conn->error); }
        
        $exam_map = [];
        while($row = $exams_q->fetch_assoc()) $exam_map[$row['id']] = $row;
        
        if(empty($exam_map)) { 
             // Not an exception, just client error (not found)
             echo json_encode(['status'=>'error', 'message'=>'No exams found for exam type: ' . $etype]); 
             exit; 
        }
        
        // Fetch Students
        $where_s = "e.class_id = $cid AND e.section_id = $sid AND (TRIM(e.academic_year) = '$year') AND (e.status = 'Active' OR e.status = 'active')";
        
        // STRICT STUDENT FILTER
        if($role == 'student') {
             $where_s .= " AND e.student_id = $uid";
             // Ignore 'roll' parameter for students, rely on ID
        } elseif(!empty($roll)) {
             $where_s .= " AND e.roll_number = '$roll'";
        }
        
        $stu_sql = "SELECT e.student_id, e.roll_number, e.class_no, u.full_name, u.father_name, u.profile_pic 
                    FROM edu_student_enrollment e 
                    JOIN edu_users u ON e.student_id = u.id 
                    WHERE $where_s 
                    ORDER BY CAST(e.roll_number AS UNSIGNED) ASC";
        $students_q = $conn->query($stu_sql);
        
        if(!$students_q) { throw new Exception("Database Error (Students): " . $conn->error); }
        
        $results = [];
        while($stu = $students_q->fetch_assoc()) {
            $st_id = $stu['student_id'];
            
            // Guest Masking early
            if($role === 'guest') {
                $stu['full_name'] = '?';
                $stu['father_name'] = '?';
                $stu['profile_pic'] = null;
            }

            $subjects_data = [];
            $total_obtain = 0;
            $total_max = 0;
            $failed_papers = 0;

            foreach($exam_map as $eid => $ex) {
                $m_q = $conn->query("SELECT obtain_marks FROM edu_exam_marks WHERE exam_id=$eid AND student_id=$st_id");
                $marks_row = ($m_q && $m_q->num_rows > 0) ? $m_q->fetch_assoc() : null;
                $marks = $marks_row ? (float)$marks_row['obtain_marks'] : 0;
                
                $pass_marks = ($ex['total_marks'] * $pass_pct / 100);
                $status = ($marks >= $pass_marks) ? 'Pass' : 'Fail';
                if($status == 'Fail') $failed_papers++;
                
                $total_obtain += $marks;
                $total_max += $ex['total_marks'];
                
                $subjects_data[] = [
                    'exam_id' => $eid,
                    'subject' => $ex['subject'],
                    'total' => $ex['total_marks'],
                    'obtained' => ($role === 'guest') ? '?' : $marks,
                    'pass_marks' => $pass_marks,
                    'status' => ($role === 'guest') ? '?' : $status
                ];
            }
            
            if($role === 'guest') {
                $total_obtain = '?';
            }
            
            $results[] = array_merge($stu, [
                'student_id' => $st_id,
                'subjects' => $subjects_data,
                'total_max' => $total_max,
                'total_obtain' => $total_obtain,
                'percentage' => $total_max > 0 ? round(($total_obtain/$total_max)*100, 2) : 0,
                'final_status' => ($role === 'guest') ? '?' : (($failed_papers == 0) ? 'PASS' : 'FAIL'),
                'failed_papers' => ($role === 'guest') ? '?' : $failed_papers
            ]);
        }
        echo json_encode(['status'=>'success', 'data'=>$results]);
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(['status'=>'error', 'message'=>'Server Error: ' . $e->getMessage()]);
    }
} elseif ($action == 'get_full_result_card') {
    try {
        $cid = (int)$_GET['class_id'];
        $sid = (int)$_GET['section_id'];
        $roll = $conn->real_escape_string($_GET['roll'] ?? '');
        
        // Security: If student, override filters
        if($role == 'student') {
            if(!$student_enroll) throw new Exception("Enrollment record missing");
            $cid = $student_enroll['class_id'];
            $sid = $student_enroll['section_id'];
            $roll = $student_enroll['roll_number'];
        }

        $year = $conn->real_escape_string($_GET['year'] ?? date('Y'));
        
        if(!$cid || !$sid) throw new Exception("Class and Section required");
        
        // Fetch Students
        $where_stu = "e.class_id=$cid AND e.section_id=$sid AND (TRIM(e.academic_year)='$year') AND (e.status = 'Active' OR e.status = 'active')";
        
        // STRICT STUDENT FILTER
        if($role == 'student') {
            $where_stu .= " AND e.student_id = $uid";
        } elseif(!empty($roll)) {
            $where_stu .= " AND e.roll_number='$roll'";
        }
        
        $stu_q = $conn->query("SELECT e.student_id, e.roll_number, e.class_no, u.full_name, u.father_name, u.profile_pic 
                             FROM edu_student_enrollment e 
                             JOIN edu_users u ON e.student_id = u.id 
                             WHERE $where_stu
                             ORDER BY CAST(e.roll_number AS UNSIGNED) ASC");
                             
        $students = [];
        while($s = $stu_q->fetch_assoc()) $students[] = $s;
        
        if(empty($students)) {
            $count_raw = $conn->query("SELECT COUNT(*) FROM edu_student_enrollment e WHERE $where_stu")->fetch_row()[0];
            throw new Exception("No students found (Criteria: CID=$cid, SID=$sid, Year=$year, Roll=$roll). Raw count: $count_raw");
        }
        
        // Get all exams for this class
        $exams_q = $conn->query("SELECT e.*, sub.name as subject_name 
                               FROM edu_exams e 
                               JOIN edu_subjects sub ON e.subject_id = sub.id 
                               WHERE e.class_id=$cid AND e.institution_id=$inst_id AND e.academic_year='$year'
                               ORDER BY e.exam_date ASC");
        $all_exams = [];
        while($ex = $exams_q->fetch_assoc()) $all_exams[] = $ex;
        
        // Group by exam_type and semester
        $groups = [];
        foreach($all_exams as $e) {
            $key = $e['exam_type'] . (isset($e['semester']) ? ' (' . $e['semester'] . ')' : '');
            if(!isset($groups[$key])) $groups[$key] = [];
            $groups[$key][] = $e;
        }

        $all_results = [];
        foreach($students as $stu) {
            $st_id = $stu['student_id'];
            $report = [];
            
            foreach($groups as $label => $type_exams) {
                $subjects = [];
                $t_max = 0; $t_obt = 0;
                foreach($type_exams as $ex) {
                    $eid = $ex['id'];
                    $mq = $conn->query("SELECT obtain_marks FROM edu_exam_marks WHERE exam_id=$eid AND student_id=$st_id");
                    $m = $mq->fetch_assoc();
                    $obt = $m ? (float)$m['obtain_marks'] : 0;
                    $subjects[] = [
                        'exam_id' => $eid,
                        'subject' => $ex['subject_name'], 
                        'total' => $ex['total_marks'], 
                        'obtained' => $obt
                    ];
                    $t_max += $ex['total_marks'];
                    $t_obt += $obt;
                }
                $report[] = [
                    'exam_type' => $label,
                    'subjects' => $subjects,
                    'total_max' => $t_max,
                    'total_obtain' => $t_obt,
                    'percentage' => $t_max > 0 ? round(($t_obt/$t_max)*100, 2) : 0
                ];
            }
            
            if($role === 'guest') {
                $stu['full_name'] = '?';
                $stu['father_name'] = '?';
                $stu['profile_pic'] = null;
                $stu['roll_number'] = '?';
                
                foreach($report as &$r) {
                    foreach($r['subjects'] as &$s) {
                        $s['obtained'] = '?';
                    }
                    $r['total_obtain'] = '?';
                    $r['percentage'] = '?';
                }
            }
            
            $stu['student_id'] = $st_id;
            $stu['report'] = $report;
            $all_results[] = $stu;
        }

        // Maintain backward compatibility for single roll request by returning object?
        // Actually, it's safer to always return array in 'data' if we update frontend.
        // But if I want to minimize frontend changes, I could check if count is 1.
        // The user asked "show result card of the whole student" so returning a list is expected.
        echo json_encode(['status'=>'success', 'data'=>$all_results, 'is_bulk' => empty($roll)]);
        
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(['status'=>'error', 'message'=>$e->getMessage()]);
    }
} elseif ($action == 'get_performance_analytics') {
    try {
        $type = $_GET['type'] ?? 'trends';
        $cid = (int)($_GET['class_id'] ?? 0);
        $sid = (int)($_GET['section_id'] ?? 0);
        
        // Students can only see toppers or overall school stats? 
        // User didn't specify, but usually they can see toppers and trends.
        // Let's restrict based on role if needed.
        if($role == 'student' && in_array($type, ['school_performance', 'staff_performance'])) {
            throw new Exception("Unauthorized access to institutional statistics");
        }
        
        switch($type) {
            case 'school_toppers':
                // Top 10 students across institution based on average percentage
                $sql = "SELECT u.full_name, c.name as class_name, s.roll_number, s.class_no,
                               SUM(m.obtain_marks) as total_obtained, 
                               SUM(e.total_marks) as total_max,
                               ROUND((SUM(m.obtain_marks) / SUM(e.total_marks)) * 100, 2) as percentage
                        FROM edu_exam_marks m
                        JOIN edu_exams e ON m.exam_id = e.id
                        JOIN edu_student_enrollment s ON m.student_id = s.student_id
                        JOIN edu_users u ON s.student_id = u.id
                        JOIN edu_classes c ON s.class_id = c.id
                        WHERE e.institution_id = $inst_id
                        GROUP BY m.student_id
                        ORDER BY percentage DESC LIMIT 10";
                $res = $conn->query($sql);
                $data = [];
                while($row = $res->fetch_assoc()) {
                    if ($role === 'guest') {
                        $row['full_name'] = '?';
                        $row['roll_number'] = '?';
                        $row['total_obtained'] = '?';
                        $row['percentage'] = '?';
                    }
                    $data[] = $row;
                }
                echo json_encode(['status'=>'success', 'data'=>$data]);
                break;

            case 'class_toppers':
                if(!$cid) throw new Exception("Class required for class toppers");
                $where = "e.institution_id = $inst_id AND s.class_id = $cid";
                if($sid) $where .= " AND s.section_id = $sid";
                
                $sql = "SELECT u.full_name, s.roll_number, s.class_no,
                               SUM(m.obtain_marks) as total_obtained, 
                               SUM(e.total_marks) as total_max,
                               ROUND((SUM(m.obtain_marks) / SUM(e.total_marks)) * 100, 2) as percentage
                        FROM edu_exam_marks m
                        JOIN edu_exams e ON m.exam_id = e.id
                        JOIN edu_student_enrollment s ON m.student_id = s.student_id
                        JOIN edu_users u ON s.student_id = u.id
                        WHERE $where
                        GROUP BY m.student_id
                        ORDER BY percentage DESC LIMIT 10";
                $res = $conn->query($sql);
                $data = [];
                while($row = $res->fetch_assoc()) {
                    if ($role === 'guest') {
                        $row['full_name'] = '?';
                        $row['roll_number'] = '?';
                        $row['total_obtained'] = '?';
                        $row['percentage'] = '?';
                    }
                    $data[] = $row;
                }
                echo json_encode(['status'=>'success', 'data'=>$data]);
                break;

            case 'school_performance':
            case 'staff_performance':
                // Aggregate stats: Total Students, Passed, Failed, %
                // Using a simplified logic: A student "passes" if they have 0 failed papers in their last relevant exam session
                // For staff_performance, we might need a join with staff assignments if available, but for now we'll do global and filter if possible.
                
                $pass_pct = 40; // Default or fetch from settings
                
                $sql = "SELECT COUNT(DISTINCT m.student_id) as total_students,
                               SUM(CASE WHEN (m.obtain_marks / e.total_marks * 100) >= $pass_pct THEN 1 ELSE 0 END) as passed_entries,
                               SUM(CASE WHEN (m.obtain_marks / e.total_marks * 100) < $pass_pct THEN 1 ELSE 0 END) as failed_entries
                        FROM edu_exam_marks m
                        JOIN edu_exams e ON m.exam_id = e.id
                        WHERE e.institution_id = $inst_id";
                
                if($type == 'staff_performance') {
                    // This would ideally join with edu_staff_assignments. For now, we'll return a class-wise breakdown as a proxy
                    $sql = "SELECT c.name as class_name, 
                                   COUNT(DISTINCT m.student_id) as total_students,
                                   SUM(CASE WHEN (m.obtain_marks / e.total_marks * 100) >= $pass_pct THEN 1 ELSE 0 END) as passed_entries,
                                   SUM(CASE WHEN (m.obtain_marks / e.total_marks * 100) < $pass_pct THEN 1 ELSE 0 END) as failed_entries
                            FROM edu_exam_marks m
                            JOIN edu_exams e ON m.exam_id = e.id
                            JOIN edu_classes c ON e.class_id = c.id
                            WHERE e.institution_id = $inst_id
                            GROUP BY c.id";
                    $res = $conn->query($sql);
                    $data = [];
                    while($row = $res->fetch_assoc()) {
                         $row['pass_rate'] = $row['total_students'] > 0 ? round(($row['passed_entries'] / ($row['passed_entries']+$row['failed_entries']))*100, 2) : 0;
                         $data[] = $row;
                    }
                } else {
                    $res = $conn->query($sql);
                    $data = $res->fetch_assoc();
                    $data['pass_rate'] = ($data['passed_entries'] + $data['failed_entries']) > 0 ? round(($data['passed_entries'] / ($data['passed_entries']+$data['failed_entries']))*100, 2) : 0;
                }
                
                echo json_encode(['status'=>'success', 'data'=>$data]);
                break;

            default: // Trends (Original Chart logic)
                if(!$cid || ! $sid) throw new Exception("Class and Section required for trends");
                // Get distinct exam types
                $types_q = $conn->query("SELECT DISTINCT exam_type FROM edu_exams WHERE class_id=$cid AND institution_id=$inst_id");
                $analytics = [];
                while($t = $types_q->fetch_assoc()) {
                    $etype = $t['exam_type'];
                    $exams_q = $conn->query("SELECT e.id, sub.name as subject_name, e.total_marks FROM edu_exams e JOIN edu_subjects sub ON e.subject_id = sub.id WHERE e.class_id=$cid AND e.exam_type='$etype'");
                    $sub_avgs = [];
                    while($ex = $exams_q->fetch_assoc()) {
                        $eid = $ex['id'];
                        $avg_q = $conn->query("SELECT AVG(obtain_marks) as avg_marks FROM edu_exam_marks WHERE exam_id=$eid");
                        $avg = $avg_q->fetch_assoc();
                        $sub_avgs[] = ['subject' => $ex['subject_name'], 'average' => $avg['avg_marks'] ? round($avg['avg_marks'], 2) : 0, 'total' => $ex['total_marks']];
                    }
                    $analytics[] = ['exam_type' => $etype, 'subject_performance' => $sub_avgs];
                }
                echo json_encode(['status'=>'success', 'data'=>$analytics]);
                break;
        }
        
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(['status'=>'error', 'message'=>$e->getMessage()]);
    }
// Action: Delete Single Student Result
} elseif($action == 'delete_student_exam_result') {
    authorize(['admin', 'staff', 'developer', 'super_admin']);
    
    $eid = (int)$_POST['exam_id'];
    $sid = (int)$_POST['student_id'];
    
    if($conn->query("DELETE FROM edu_exam_marks WHERE exam_id = $eid AND student_id = $sid")) {
        echo json_encode(['status' => 'success']);
    } else {
        echo json_encode(['status' => 'error', 'message' => $conn->error]);
    }
    exit;
} elseif ($action == 'delete_full_exam_results') {
    authorize(['admin', 'staff']);
    $eid = (int)$_POST['exam_id'];
    
    if($conn->query("DELETE FROM edu_exam_marks WHERE exam_id = $eid")) {
        echo json_encode(['status' => 'success', 'deleted' => $conn->affected_rows]);
    } else {
        echo json_encode(['status' => 'error', 'message' => $conn->error]);
    }

} elseif ($action == 'get_scheduled_exams') {
    // List all scheduled exams for management
    authorize(['admin', 'staff', 'developer', 'super_admin']);
    
    // Optional filters? For now, fetch all, ordered by latest date
    $sql = "SELECT e.id, e.exam_type, e.academic_year, e.exam_date, e.start_time, e.shift, e.total_marks,
                   c.name as class_name, s.name as section_name, sub.name as subject_name 
            FROM edu_exams e 
            JOIN edu_classes c ON e.class_id = c.id 
            JOIN edu_sections s ON e.section_id = s.id 
            JOIN edu_subjects sub ON e.subject_id = sub.id 
            WHERE e.institution_id = $inst_id 
            ORDER BY e.exam_date DESC, e.start_time ASC LIMIT 500";
            
    $res = $conn->query($sql);
    $data = [];
    while($row = $res->fetch_assoc()) $data[] = $row;
    echo json_encode(['status' => 'success', 'data' => $data]);

} elseif ($action == 'delete_exam') {
    authorize(['admin', 'developer', 'super_admin']); 
    $eid = (int)$_POST['exam_id'];
    
    // Delete marks first (Foreign Key might handle it, but explicit is safer if no Cascade)
    $conn->query("DELETE FROM edu_exam_marks WHERE exam_id = $eid");
    
    if($conn->query("DELETE FROM edu_exams WHERE id = $eid")) {
        echo json_encode(['status' => 'success']);
    } else {
        echo json_encode(['status' => 'error', 'message' => $conn->error]);
    }

} elseif ($action == 'get_exam_hierarchy_l1') {
    // Level 1: Distinct Type & Year
    authorize(['admin', 'staff', 'developer', 'super_admin']);
    
    $sql = "SELECT exam_type, semester, academic_year, COUNT(*) as exam_count 
            FROM edu_exams 
            WHERE institution_id = $inst_id 
            GROUP BY exam_type, semester, academic_year 
            ORDER BY academic_year DESC, semester, exam_type";
            
    $res = $conn->query($sql);
    $data = [];
    while($row = $res->fetch_assoc()) $data[] = $row;
    echo json_encode(['status' => 'success', 'data' => $data]);

} elseif ($action == 'get_exam_hierarchy_l2') {
    // Level 2: Classes for Type/Year
    authorize(['admin', 'staff', 'developer', 'super_admin']);
    
    $type = $conn->real_escape_string($_GET['type']);
    $semester = $conn->real_escape_string($_GET['semester'] ?? '');
    $year = $conn->real_escape_string($_GET['year']);
    
    $sql = "SELECT e.class_id, e.section_id, c.name as class_name, s.name as section_name, COUNT(*) as subject_count 
            FROM edu_exams e 
            JOIN edu_classes c ON e.class_id = c.id 
            JOIN edu_sections s ON e.section_id = s.id 
            WHERE e.institution_id = $inst_id AND e.exam_type = '$type' AND e.semester = '$semester' AND e.academic_year = '$year'
            GROUP BY e.class_id, e.section_id
            ORDER BY c.name, s.name";
            
    $res = $conn->query($sql);
    $data = [];
    while($row = $res->fetch_assoc()) $data[] = $row;
    echo json_encode(['status' => 'success', 'data' => $data]);

} elseif ($action == 'get_exam_hierarchy_l3') {
    // Level 3: Specific Exams (Subjects) for Class/Type/Year
    authorize(['admin', 'staff', 'developer', 'super_admin']);
    
    $type = $conn->real_escape_string($_GET['type']);
    $semester = $conn->real_escape_string($_GET['semester'] ?? '');
    $year = $conn->real_escape_string($_GET['year']);
    $cid = (int)$_GET['class_id'];
    $sid = (int)$_GET['section_id'];
    
    $sql = "SELECT e.id, e.exam_date, e.start_time, e.shift, e.total_marks, sub.name as subject_name,
                   e.exam_type, e.semester, e.academic_year, e.class_id, e.section_id 
            FROM edu_exams e 
            JOIN edu_subjects sub ON e.subject_id = sub.id 
            WHERE e.institution_id = $inst_id 
              AND e.exam_type = '$type' 
              AND e.semester = '$semester'
              AND e.academic_year = '$year' 
              AND e.class_id = $cid 
              AND e.section_id = $sid
            ORDER BY e.exam_date, e.start_time";
            
    $res = $conn->query($sql);
    $data = [];
    while($row = $res->fetch_assoc()) $data[] = $row;
    echo json_encode(['status' => 'success', 'data' => $data]);

} elseif ($action == 'delete_exam_group_l1') {
    // Delete All by Type & Year
    authorize(['admin', 'developer', 'super_admin']); 
    $type = $conn->real_escape_string($_POST['type']);
    $semester = $conn->real_escape_string($_POST['semester']);
    $year = $conn->real_escape_string($_POST['year']);
    
    // Find IDs to delete marks first (if needed, or rely on CASCADE)
    // Deleting from edu_exams should be enough if FKs are set, but let's be safe
    // Actually, simple DELETE FROM edu_exam_marks joined? MySQL doesn't support easy delete join syntax always.
    // Let's just delete exams.
    
    // Get IDs first to delete associated marks
    $ids_q = $conn->query("SELECT id FROM edu_exams WHERE institution_id = $inst_id AND exam_type = '$type' AND semester = '$semester' AND academic_year = '$year'");
    $ids = [];
    while($r = $ids_q->fetch_assoc()) $ids[] = $r['id'];
    
    if(!empty($ids)) {
        $id_str = implode(',', $ids);
        $conn->query("DELETE FROM edu_exam_marks WHERE exam_id IN ($id_str)");
        $conn->query("DELETE FROM edu_exams WHERE id IN ($id_str)");
    }
    echo json_encode(['status' => 'success', 'deleted' => count($ids)]);

} elseif ($action == 'delete_exam_group_l2') {
    authorize(['admin', 'developer', 'super_admin']); 
    $type = $conn->real_escape_string($_POST['type']);
    $semester = $conn->real_escape_string($_POST['semester']);
    $year = $conn->real_escape_string($_POST['year']);
    $cid = (int)$_POST['class_id'];
    $sid = (int)$_POST['section_id'];
    
    $ids_q = $conn->query("SELECT id FROM edu_exams WHERE institution_id = $inst_id AND exam_type = '$type' AND semester = '$semester' AND academic_year = '$year' AND class_id = $cid AND section_id = $sid");
    $ids = [];
    while($r = $ids_q->fetch_assoc()) $ids[] = $r['id'];
    
    if(!empty($ids)) {
        $id_str = implode(',', $ids);
        $conn->query("DELETE FROM edu_exam_marks WHERE exam_id IN ($id_str)");
        $conn->query("DELETE FROM edu_exams WHERE id IN ($id_str)");
    }
    echo json_encode(['status' => 'success', 'deleted' => count($ids)]);

} elseif ($action == 'check_student_dues') {
    if ($role !== 'student') {
        echo json_encode(['status' => 'success', 'has_dues' => false, 'balance' => 0]);
        exit;
    }
    require_once '../../includes/FeeManager.php';
    $fm = new FeeManager($conn, $inst_id);
    $student_id = (int)$_SESSION['edu_user_id'];
    $bal = $fm->getStudentBalance($student_id);
    echo json_encode(['status' => 'success', 'has_dues' => ($bal > 0), 'balance' => $bal]);
    exit;
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid action']);
}
?>






