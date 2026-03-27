<?php
require_once __DIR__ . '/notification_helper.php';

class FeeManager {
    private $conn;
    private $inst_id;

    public function __construct($database_connection, $institution_id) {
        $this->conn = $database_connection;
        $this->inst_id = (int)$institution_id;
    }

    public function getFeeTypeId($name, $create_if_missing = false) {
        $name = $this->conn->real_escape_string($name);
        $res = $this->conn->query("SELECT id FROM edu_fee_types WHERE institution_id = {$this->inst_id} AND type_name = '$name' ORDER BY id ASC LIMIT 1");
        $row = $res->fetch_assoc();
        
        if (!$row && $create_if_missing) {
            $this->conn->query("INSERT INTO edu_fee_types (institution_id, type_name, status) VALUES ({$this->inst_id}, '$name', 'Active')");
            return (int)$this->conn->insert_id;
        }

        return $row ? (int)$row['id'] : 0;
    }

    public function getFeeTypes() {
        $res = $this->conn->query("SELECT id, type_name FROM edu_fee_types WHERE institution_id = {$this->inst_id} AND status = 'Active' ORDER BY type_name ASC");
        $types = [];
        if ($res) {
            while($r = $res->fetch_assoc()) $types[] = $r;
        }
        return $types;
    }

    public function getFeeRecord($student_id, $month, $year, $type_id) {
        $sid = (int)$student_id;
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        $tid = (int)$type_id;
        
        $res = $this->conn->query("SELECT * FROM edu_fee_management 
                                  WHERE student_id = $sid AND institute_id = {$this->inst_id} 
                                  AND fee_month = '$m' AND fee_year = $y AND fee_type_id = $tid
                                  LIMIT 1");
        return $res->fetch_assoc();
    }

    public function updateFeeRecord($student_id, $month, $year, $type_id, $amount = null, $status = null, $remarks = null, $collected_by = null) {
        // Validation: Never save records with empty or invalid months
        if (empty($month) || $month === 'All' || strlen($month) < 3) return false;
        
        $sid = (int)$student_id;
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        $tid = (int)$type_id;

        $curr = $this->getFeeRecord($sid, $m, $y, $tid);
        
        // Use existing values if not provided
        $amt = ($amount !== null) ? (float)$amount : ($curr ? (float)$curr['amount'] : 0);
        $stat = ($status !== null) ? $this->conn->real_escape_string($status) : ($curr ? $curr['Status'] : 'Unpaid');
        $rem = ($remarks !== null) ? $this->conn->real_escape_string($remarks) : ($curr ? $curr['remarks'] : '');
        $cby_val = ($collected_by !== null) ? (int)$collected_by : ($curr ? (int)($curr['fee_collected_by'] ?? 0) : 0);
        $cby_sql = $cby_val > 0 ? (int)$cby_val : "NULL";

        if ($curr) {
            $sql = "UPDATE edu_fee_management SET 
                    amount = $amt,
                    Status = '$stat',
                    remarks = '$rem',
                    fee_collected_by = $cby_sql
                    WHERE id = {$curr['id']}";
        } else {
            $info = $this->conn->query("SELECT class_id, section_id FROM edu_student_enrollment WHERE student_id = $sid LIMIT 1")->fetch_assoc();
            $cid = (int)($info['class_id'] ?? 0);
            $scid = (int)($info['section_id'] ?? 0);

            $sql = "INSERT INTO edu_fee_management 
                    (institute_id, student_id, class_id, section_id, fee_month, fee_year, fee_type_id, amount, Status, remarks, fee_collected_by)
                    VALUES ({$this->inst_id}, $sid, $cid, $scid, '$m', $y, $tid, $amt, '$stat', '$rem', $cby_sql)";
        }

        $res = $this->conn->query($sql);

        // 4. Log the payment if it was marked as paid
        if ($res && $stat === 'Paid') {
            // Get type name for log compatibility (or we should update log table too? log table already has fee_type varchar)
            $type_name_q = $this->conn->query("SELECT type_name FROM edu_fee_types WHERE id = $tid")->fetch_assoc();
            $tname = $this->conn->real_escape_string($type_name_q['type_name'] ?? 'General Fee');
            
            $this->conn->query("INSERT INTO edu_fee_payments_log 
                               (student_id, institution_id, fee_type, amount_paid, payment_month, payment_year, remarks)
                               VALUES ($sid, {$this->inst_id}, '$tname', $amt, '$m', $y, '$rem')");
        }

        return $res;
    }

    public function syncMonthlyBills($student_id, $month, $year) {
        $sid = (int)$student_id;
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        
        // Fetch fees from enrollment table instead of users
        $res = $this->conn->query("
            SELECT se.tuition_fee, se.transport_charges, u.is_transport_active 
            FROM edu_student_enrollment se
            JOIN edu_users u ON se.student_id = u.id
            WHERE se.student_id = $sid LIMIT 1
        ");
        $user = $res->fetch_assoc();
        if (!$user) return;

        $tid_tui = $this->getFeeTypeId('Tuition Fee');
        $tid_trans = $this->getFeeTypeId('TC (Transport Charges)');

        // Tuition Sync
        $tui_amt = (float)($user['tuition_fee'] ?? 0);
        if ($tid_tui) {
            $curr = $this->getFeeRecord($sid, $m, $y, $tid_tui);
            if ($tui_amt > 0) {
                if (!$curr || $curr['Status'] === 'Unpaid') {
                    $this->updateFeeRecord($sid, $m, $y, $tid_tui, $tui_amt);
                    if ($curr) {
                        $this->conn->query("DELETE FROM edu_fee_management WHERE student_id = $sid AND fee_month = '$m' AND fee_year = $y AND fee_type_id = $tid_tui AND id != {$curr['id']} AND Status = 'Unpaid'");
                    }
                }
            } else {
                // Enrollment says 0, so remove any Unpaid dues
                $this->conn->query("DELETE FROM edu_fee_management WHERE student_id = $sid AND fee_month = '$m' AND fee_year = $y AND fee_type_id = $tid_tui AND Status = 'Unpaid'");
            }
        }
        
        // Transport Sync
        $trans_amt = (float)($user['transport_charges'] ?? 0);
        if ($tid_trans) {
            $curr = $this->getFeeRecord($sid, $m, $y, $tid_trans);
            if ($trans_amt > 0) {
                if (!$curr || $curr['Status'] === 'Unpaid') {
                    $this->updateFeeRecord($sid, $m, $y, $tid_trans, $trans_amt);
                    if ($curr) {
                        $this->conn->query("DELETE FROM edu_fee_management WHERE student_id = $sid AND fee_month = '$m' AND fee_year = $y AND fee_type_id = $tid_trans AND id != {$curr['id']} AND Status = 'Unpaid'");
                    }
                }
            } else {
                // Enrollment says 0, so remove any Unpaid dues
                $this->conn->query("DELETE FROM edu_fee_management WHERE student_id = $sid AND fee_month = '$m' AND fee_year = $y AND fee_type_id = $tid_trans AND Status = 'Unpaid'");
            }
        }

        // Sync Attendance & Disciplinary Fines
        $this->syncAttendanceFines($sid, $m, $y);
    }

    public function syncAttendanceFines($student_id, $month, $year) {
        $sid = (int)$student_id;
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        
        // Convert month name to number for MySQL
        $month_num = date('m', strtotime("$month $year"));
        $start_date = "$year-$month_num-01";
        $end_date = date('Y-m-t', strtotime($start_date));

        // 1. Fetch Disciplinary Flags from Attendance
        $att_q = $this->conn->query("
            SELECT 
                SUM(haircut_fine) as h_cnt, 
                SUM(register_fine) as r_cnt, 
                SUM(rules_break_fine) as rb_cnt,
                SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as abs_cnt
            FROM edu_attendance 
            WHERE student_id = $sid AND date BETWEEN '$start_date' AND '$end_date'
        ");
        $counts = $att_q->fetch_assoc();
        
        // 2. Fetch Rates
        $inst_q = $this->conn->query("SELECT fine_hair_cut, fine_register, fine_rules_break, fine_attendance FROM edu_institutions WHERE id = {$this->inst_id}");
        $rates = $inst_q->fetch_assoc();
        
        $enrol_q = $this->conn->query("SELECT absent_fine_rate, special_fine FROM edu_student_enrollment WHERE student_id = $sid LIMIT 1");
        $enrol = $enrol_q->fetch_assoc();
        
        $h_total = (int)($counts['h_cnt'] ?? 0) * (float)($rates['fine_hair_cut'] ?? 0);
        $r_total = (int)($counts['r_cnt'] ?? 0) * (float)($rates['fine_register'] ?? 0);
        $rb_total = (int)($counts['rb_cnt'] ?? 0) * (float)($rates['fine_rules_break'] ?? 0);
        
        // Priority: Institutional Global fine_attendance -> Enrollment specific rate
        $abs_rate = (float)($rates['fine_attendance'] ?? 0);
        if ($abs_rate <= 0) {
            $abs_rate = (float)($enrol['absent_fine_rate'] ?? 10.00); // Default to 10 if none set as fallback
        }
        
        $abs_total = (int)($counts['abs_cnt'] ?? 0) * $abs_rate;
        $spec_fine = (float)($enrol['special_fine'] ?? 0);

        $disc_total = $h_total + $r_total + $rb_total + $spec_fine;
        
        // 3. Update Ledger
        if ($disc_total > 0) {
            $tid = $this->getFeeTypeId('Disciplinary Fine', true);
            if ($tid) {
                $remarks = [];
                if($h_total > 0) $remarks[] = "Haircut: $h_total";
                if($r_total > 0) $remarks[] = "Register: $r_total";
                if($rb_total > 0) $remarks[] = "Rules: $rb_total";
                if($spec_fine > 0) $remarks[] = "Special: $spec_fine";
                
                if ($this->updateFeeRecord($sid, $m, $y, $tid, $disc_total, null, implode(", ", $remarks))) {
                    notify_student_and_parents($this->conn, $sid, "Disciplinary Fine Added", "A disciplinary fine of " . number_format($disc_total) . " has been added to your ledger for $m $y.", 'warning');
                }
            }
        }

        if ($abs_total >= 0) { // Keep sync for 0 to reflect cleared absents/manual removal
            $tid = $this->getFeeTypeId('Attendance Fine', true);
            if ($tid) {
                if ($this->updateFeeRecord($sid, $m, $y, $tid, $abs_total, null, "Absents: " . $counts['abs_cnt'])) {
                    if ($abs_total > 0) {
                         notify_student_and_parents($this->conn, $sid, "Attendance Fine Added", "An attendance fine of " . number_format($abs_total) . " has been added for " . $counts['abs_cnt'] . " absents in $m $y.", 'warning');
                    }
                }
            }
        }
    }

    public function getStudentBalance($student_id, $up_to_month = null, $up_to_year = null) {
        $sid = (int)$student_id;
        $m = $up_to_month ?: date('F');
        $y = (int)($up_to_year ?: date('Y'));

        // UNIFIED CALCULATION FOR NEW RECORD-STATUS MODEL
        // Debt: All rows with Status='Unpaid' (excluding 'Paid' type rows which shouldn't exist as debt)
        // Credits: Legacy rows with type 'Paid' or 'Advance'
        
        $month_clause = "AND (f.fee_year < $y OR (f.fee_year = $y AND FIELD(f.fee_month, 'January','February','March','April','May','June','July','August','September','October','November','December') <= FIELD('$m', 'January','February','March','April','May','June','July','August','September','October','November','December')))";

        // 1. Sum Unpaid Debt (The real balance in the new system)
        $charges_q = $this->conn->query("
            SELECT SUM(f.amount) as total 
            FROM edu_fee_management f
            JOIN edu_fee_types t ON f.fee_type_id = t.id
            WHERE f.student_id = $sid AND f.institute_id = {$this->inst_id} 
            AND f.Status = 'Unpaid'
            AND t.type_name != 'Paid'
            AND f.remarks NOT LIKE '%summary%'
            AND NOT (t.type_name IN ('Dues', 'Balance', 'Arrears', 'Total Dues') AND f.remarks LIKE 'Arrears from%')
            $month_clause
        ");
        $total_unpaid = ($charges_q) ? (float)($charges_q->fetch_assoc()['total'] ?? 0) : 0;

        // 2. Sum Legacy Credits (Rows with type 'Paid' that haven't been 'consumed' or settled)
        $payments_q = $this->conn->query("
            SELECT SUM(f.amount) as total 
            FROM edu_fee_management f
            JOIN edu_fee_types t ON f.fee_type_id = t.id
            WHERE f.student_id = $sid AND f.institute_id = {$this->inst_id} 
            AND (t.type_name = 'Paid' OR t.type_name = 'Advance Payment')
            AND f.payment_status != 'Rejected'
            $month_clause
        ");
        $total_legacy_credits = ($payments_q) ? (float)($payments_q->fetch_assoc()['total'] ?? 0) : 0;

        return $total_unpaid - $total_legacy_credits;
    }

    public function countHistoryRecords($student_id, $month, $year) {
        $sid = (int)$student_id;
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        
        $month_clause = "AND (f.fee_year < $y OR (f.fee_year = $y AND FIELD(f.fee_month, 'January','February','March','April','May','June','July','August','September','October','November','December') <= FIELD('$m', 'January','February','March','April','May','June','July','August','September','October','November','December')))";
        
        $res = $this->conn->query("
            SELECT COUNT(*) as cnt 
            FROM edu_fee_management f
            JOIN edu_fee_types t ON f.fee_type_id = t.id
            WHERE f.student_id = $sid AND f.institute_id = {$this->inst_id}
            AND t.type_name != 'Paid'
            AND f.remarks NOT LIKE '%summary%'
            $month_clause
        ");
        return ($res) ? (int)($res->fetch_assoc()['cnt'] ?? 0) : 0;
    }

    public function getHistoryBalance($student_id, $month, $year) {
        return $this->getStudentBalance($student_id, $month, $year);
    }

    public function getMonthBreakdown($student_id, $month, $year) {
        $sid = (int)$student_id;
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        // Join with fee_types to get names for UI
        $res = $this->conn->query("SELECT f.*, t.type_name as fee_type 
                                  FROM edu_fee_management f 
                                  LEFT JOIN edu_fee_types t ON f.fee_type_id = t.id 
                                  WHERE f.student_id = $sid AND f.institute_id = {$this->inst_id} 
                                  AND f.fee_month = '$m' AND f.fee_year = $y");
        $data = [];
        if ($res) {
            while($row = $res->fetch_assoc()) $data[] = $row;
        }
        return $data;
    }
    public function getBulkBalances($student_ids, $month, $year) {
        if (empty($student_ids)) return [];
        
        $ids_str = implode(',', array_map('intval', $student_ids));
        $m = $this->conn->real_escape_string($month);
        $y = (int)$year;
        
        $month_clause = "AND (f.fee_year < $y OR (f.fee_year = $y AND FIELD(f.fee_month, 'January','February','March','April','May','June','July','August','September','October','November','December') <= FIELD('$m', 'January','February','March','April','May','June','July','August','September','October','November','December')))";

        // Bulk Fetch Charges
        $charges_q = $this->conn->query("
            SELECT f.student_id, SUM(f.amount) as total 
            FROM edu_fee_management f
            JOIN edu_fee_types t ON f.fee_type_id = t.id
            WHERE f.student_id IN ($ids_str) AND f.institute_id = {$this->inst_id} 
            AND t.type_name != 'Paid'
            AND f.remarks NOT LIKE '%summary%'
            AND NOT (t.type_name IN ('Dues', 'Balance', 'Arrears', 'Total Dues') AND f.remarks LIKE 'Arrears from%')
            AND f.Status != 'Paid'
            $month_clause
            GROUP BY f.student_id
        ");
        
        $charges = [];
        if ($charges_q) {
            while($row = $charges_q->fetch_assoc()) {
                $charges[$row['student_id']] = (float)$row['total'];
            }
        }

        // Bulk Fetch Payments
        $payments_q = $this->conn->query("
            SELECT f.student_id, SUM(f.amount) as total 
            FROM edu_fee_management f
            JOIN edu_fee_types t ON f.fee_type_id = t.id
            WHERE f.student_id IN ($ids_str) AND f.institute_id = {$this->inst_id} 
            AND t.type_name = 'Paid'
            AND f.payment_status != 'Rejected'
            $month_clause
            GROUP BY f.student_id
        ");
        
        $payments = [];
        if ($payments_q) {
            while($row = $payments_q->fetch_assoc()) {
                $payments[$row['student_id']] = (float)$row['total'];
            }
        }
        
        // Calculate Balances
        $balances = [];
        foreach($student_ids as $sid) {
            $c = $charges[$sid] ?? 0;
            $p = $payments[$sid] ?? 0;
            $balances[$sid] = $c - $p;
        }
        
        return $balances;
    }

    public function getLedgerStartDate($student_id) {
        $sid = (int)$student_id;
        $res = $this->conn->query("SELECT admission_date FROM edu_student_enrollment WHERE student_id = $sid LIMIT 1");
        $enroll = $res->fetch_assoc();
        $admit_time = !empty($enroll['admission_date']) ? strtotime($enroll['admission_date']) : 0;
        
        $min_q = $this->conn->query("SELECT fee_month, fee_year FROM edu_fee_management WHERE student_id = $sid AND institute_id = {$this->inst_id} ORDER BY fee_year ASC, FIELD(fee_month, 'January','February','March','April','May','June','July','August','September','October','November','December') ASC LIMIT 1");
        $min_row = $min_q->fetch_assoc();
        $first_bill_time = $min_row ? strtotime("1 {$min_row['fee_month']} {$min_row['fee_year']}") : 0;
        
        if ($admit_time > 0 && $first_bill_time > 0) return min($admit_time, $first_bill_time);
        return $admit_time ?: $first_bill_time ?: strtotime(date('1 F Y'));
    }

    public function getProjectedBalance($student_id, $up_to_month = null, $up_to_year = null) {
        $sid = (int)$student_id;
        $target_m = $up_to_month ?: date('F');
        $target_y = (int)($up_to_year ?: date('Y'));
        
        // 1. Get real debt from DB
        $db_bal = $this->getStudentBalance($sid, $target_m, $target_y);

        // 2. Fetch enrollment info
        $res = $this->conn->query("SELECT admission_date, tuition_fee, transport_charges FROM edu_student_enrollment WHERE student_id = $sid LIMIT 1");
        $enroll = $res->fetch_assoc();
        if (!$enroll) return $db_bal;

        $t_fee = (float)$enroll['tuition_fee'];
        $tr_fee = (float)$enroll['transport_charges'];
        
        // Determine Logical Start Date to prevent projecting into the "pre-history"
        $start_time = $this->getLedgerStartDate($sid);

        // 3. Projection: Look backwards from target Month/Year
        $months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
        $projected_extra = 0;
        
        $curr_m_idx = array_search($target_m, $months);
        $curr_y = $target_y;

        $t_tid = (int)$this->getFeeTypeId('Tuition Fee');
        $tr_tid = (int)$this->getFeeTypeId('TC (Transport Charges)');

        // Safety: Limit projection to 12 months backwards
        for ($i = 0; $i < 12; $i++) {
            $month_name = $months[$curr_m_idx];
            $check_time = strtotime("1 $month_name $curr_y");
            if ($check_time < $start_time) break;

            // Fetch what actually exists for this month
            $existing_q = $this->conn->query("SELECT fee_type_id FROM edu_fee_management WHERE student_id = $sid AND fee_month = '$month_name' AND fee_year = $curr_y AND institute_id = {$this->inst_id}");
            $found_types = [];
            while($r = $existing_q->fetch_assoc()) $found_types[] = (int)$r['fee_type_id'];

            $has_any = (count($found_types) > 0);

            // Add missing Tuition if expected
            if ($t_fee > 0 && !in_array($t_tid, $found_types)) {
                $projected_extra += $t_fee;
            }
            // Add missing Transport if expected
            if ($tr_fee > 0 && !in_array($tr_tid, $found_types)) {
                $projected_extra += $tr_fee;
            }

            // ANCHOR LOGIC: If the month has ANY records, we assume it's the "start" of our billed history.
            // We only continue projecting backwards if the month is COMPLETELY empty (unbilled).
            // However, we always process the FIRST iteration (target month) fully.
            if ($has_any && $i > 0) break;

            // Move to previous month
            $curr_m_idx--;
            if ($curr_m_idx < 0) {
                $curr_m_idx = 11;
                $curr_y--;
            }
        }

        return $db_bal + $projected_extra;
    }

    /**
     * Mobile-Specific: Get the fee ledger for a specific student with optional period filtering and auto-sync.
     */
    public function getStudentLedger($student_id, $f_month = 'All', $f_year = 0) {
        $student_id = (int)$student_id;
        $inst_id = $this->inst_id;
        $response = [
            'status' => 'success',
            'ledger' => [],
            'student_info' => null,
            'institution_name' => 'WANTUCH OFFICIAL RECEIPT',
            'fee_types' => []
        ];

        // Ensure monthly bill synced for requested period (Reuse original syncMonthlyBills)
        if ($f_month !== 'All' && $f_month !== 'Unpaid' && (int)$f_year > 0) {
            $this->syncMonthlyBills($student_id, $f_month, (int)$f_year);
        }

        // 1. Fetch Student Info (Model compatibility for mobile)
        $stu_q = $this->conn->query("
            SELECT u.id as student_id, u.full_name, u.father_name, u.profile_pic, u.adm_no,
                   e.class_id, e.section_id, c.name as class_name, s.name as section_name
            FROM edu_users u
            LEFT JOIN edu_student_enrollment e ON u.id = e.student_id
            LEFT JOIN edu_classes c ON e.class_id = c.id
            LEFT JOIN edu_sections s ON e.section_id = s.id
            WHERE u.id = $student_id AND u.institution_id = $inst_id
            LIMIT 1
        ");
        if ($stu_q && $stu = $stu_q->fetch_assoc()) {
            $stu['class_section'] = ($stu['class_name'] ?? '') . ' - ' . ($stu['section_name'] ?? '');
            $response['student_info'] = $stu;
        }

        // 2. Fetch Institution Name
        $inst_q = $this->conn->query("SELECT name FROM edu_institutions WHERE id = $inst_id");
        if ($inst_q && $inst_row = $inst_q->fetch_assoc()) {
            $response['institution_name'] = $inst_row['name'];
        }

        // 3. Fetch Fee Types (Include global ones if any)
        $types_q = $this->conn->query("SELECT id, type_name FROM edu_fee_types WHERE institution_id = $inst_id OR institution_id = 0");
        while ($t = $types_q->fetch_assoc()) {
            $response['fee_types'][] = $t;
        }

        // 4. Fetch Ledger Data with Filtering
        $where = "f.student_id = $student_id AND f.institute_id = $inst_id";
        if ($f_month !== 'All' && $f_month !== 'Unpaid' && (int)$f_year > 0) {
            $f_month_esc = $this->conn->real_escape_string($f_month);
            $where .= " AND ( (f.fee_month = '$f_month_esc' AND f.fee_year = " . (int)$f_year . ") OR (f.Status != 'Paid') )";
        } elseif ($f_month === 'Unpaid') {
            $where .= " AND f.Status != 'Paid' AND f.Status != 'Rejected'";
        }

        $ledger_q = $this->conn->query("
            SELECT f.id, f.amount, f.fee_month, f.fee_year, f.fee_type_id, f.Status as status, f.payment_method, f.transaction_id, f.remarks, t.type_name as fee_type 
            FROM edu_fee_management f 
            LEFT JOIN edu_fee_types t ON f.fee_type_id = t.id 
            WHERE $where
            ORDER BY f.fee_year DESC, 
                     FIELD(f.fee_month, 'December','November','October','September','August','July','June','May','April','March','February','January') DESC, 
                     f.id DESC
        ");
        if ($ledger_q) {
            while ($row = $ledger_q->fetch_assoc()) {
                $row['Status'] = $row['status']; // App expects uppercase S specifically
                $row['amount'] = (float)$row['amount'];
                $response['ledger'][] = $row;
            }
        }
        return $response;
    }
}
