<?php
ini_set('display_errors', false);
error_reporting(E_ALL);
require_once('sps_mobile_server_error_codes.php');
/*Set Ini variables*/
$ini = parse_ini_file("server.ini");
$ipaddress = $ini['ipAddress'];
$port = $ini['port'];
$servername = $ini['HOST'];
$user = $ini['USER'];
$pass = $ini['PASS'];
$dbname = $ini['DB'];
$img_base_url = $ini['entryImageUrl'];
$storage_image_url = $ini['storageImageUrl']; //US872: New Receipt Layout and Format
$defaultTimeZone = $ini['defaultTimeZone'];
$max_clients = 20;
$baseUrl = $ini['baseUrl'];


date_default_timezone_set($defaultTimeZone);
/*End  Ini variables*/
//print_r($ini); die;
/*Global Variable*/
$response = array();


/* check connection */
$conn = mysqli_connect($servername, $user, $pass, $dbname);
if (mysqli_connect_errno()) {
    echo json_encode(array('err_code' => '10001', 'err_msg' => $errors['10001']));
    $logString = $errors['10001'];
    createLog($logString);
    exit();
}

//US872: New Receipt Layout and Format
$invoice_number_sql = mysqli_query($conn, "SELECT prefix_for_invoice FROM sps_settings");
$invoice_number = mysqli_fetch_assoc($invoice_number_sql);
if (!empty($invoice_number['prefix_for_invoice'])) {
    $invoice_number = $invoice_number['prefix_for_invoice'];
} else {
    $invoice_number = '';
}

/*---- Fetching IP Address against ----*/
$sqlIpAddress = "SELECT ip_id FROM sps_ip_address WHERE ip_address ='" . $_REQUEST['ipaddress'] . "' AND STATUS = '1' AND deleted_at IS NULL";
$sqlQueryforIpAddress = mysqli_query($conn, $sqlIpAddress);
if (!mysqli_num_rows($sqlQueryforIpAddress)) {
    echo json_encode(array('err_code' => '10002', 'err_msg' => $errors['10002']));
    $logString = $sqlIpAddress.PHP_EOL.'err_msg->'.$errors['10002'];
    createLog($logString);
    exit();
}

$data = mysqli_fetch_assoc($sqlQueryforIpAddress);

if ($_REQUEST['cmd'] == 'online')
{
    echo json_encode(array('err_code' => '0'));
    exit;
}

/*Request command switch*/
$logString = json_encode($_REQUEST) . PHP_EOL;


switch ($_REQUEST['cmd']):
    /*---- Case 0002 for Gate Configuration data retrieval ----*/
    case "startUp":
        /*---- Fetching Gate Configuration Data ----*/
        $gateConfig = mysqli_query($conn, "SELECT b.gate_id, b.gate_name, b.gate_type, b.standard_nested,b.gate_ip,b.is_full,b.grace_period,b.discount_additional,b.percentage_flat,b.discount_additional_value FROM sps_ip_address a, sps_gate_configurations b WHERE b.gate_ip = '" . $data['ip_id'] . "'  AND a.ip_id = b.gate_ip AND b.deleted_at IS NULL GROUP BY b.gate_id");
        $GateConfig = mysqli_fetch_assoc($gateConfig);
        if ($GateConfig['gate_type'] != '5') {
            unset($GateConfig['grace_period']);
            unset($GateConfig['discount_additional']);
            unset($GateConfig['percentage_flat']);
            unset($GateConfig['discount_additional_value']);
        }
        $response['GateConfig'] = $GateConfig;
        $bit = 0;

        $GateTypeResult = mysqli_query($conn, "SELECT gt_name FROM sps_gate_type WHERE gt_id ='" . $GateConfig['gate_type'] . "' AND deleted_at IS NULL");
        $GateTypes = mysqli_fetch_assoc($GateTypeResult);
        $GateType = $GateTypes['gt_name'];
        switch ($GateType) {
            case "Man Entry":
                $GateTypeName = "IN";
                break;
            case "Man Exit";
                $GateTypeName = "OUT";
                break;
            case "Manless Entry":
                $GateTypeName = "IN";
                break;
            case "Manless Exit":
                $GateTypeName = "OUT";
                break;
            case "CPS":
                $GateTypeName = "CPS";
                break;
            default:
                $GateTypeName = NULL;
        }

        $response['GateTypeName'] = $GateTypeName;

        /*---- fetching vehicle ids and in next query getting related vehicle type and shortcut keycode ----*/
        $VehicleType = mysqli_query($conn, "SELECT svt.id,svt.type,svt.shortcut_keycode FROM sps_gate_vehicles_mapping AS sgvm LEFT JOIN sps_vehicle_types AS svt ON sgvm.vehicle_id = svt.id WHERE sgvm.gate_id = '" . $GateConfig['gate_id'] . "' AND svt.deleted_at IS NULL");
        if (!$VehicleType) {
            echo json_encode(array('err_code' => '10003', 'err_msg' => $errors['10003']));
            exit();
        }
        while ($VehicleTypeResults = mysqli_fetch_assoc($VehicleType)) {
            $response['VehicleTypeResult'][] = $VehicleTypeResults;
        }


        /*---- fetching all vehicle type and shortcut keycode ----*/
        $allVehicleType = mysqli_query($conn, "SELECT id,type,shortcut_keycode FROM sps_vehicle_types WHERE deleted_at IS NULL");
        if (!$allVehicleType) {
            echo json_encode(array('err_code' => '10003', 'err_msg' => $errors['10003']));
            exit();
        }
        while ($allVehicleTypeResults = mysqli_fetch_assoc($allVehicleType)) {
            $response['allVehicleTypeResult'][] = $allVehicleTypeResults;
        }

        /*---- Payement Mode Query against the gate_id from sps_gate_payments_mapping  ----*/
        $PaymentModes = mysqli_query($conn, "SELECT b.id,b.payment_mode,b.master_id FROM sps_gate_payments_mapping a, sps_payment_modes b WHERE a.gate_id = '" . $GateConfig['gate_id'] . "'  AND a.payment_mode = b.id AND b.deleted_at IS NULL GROUP BY b.id");
        if (!$PaymentModes) {
            echo json_encode(array('err_code' => '10005', 'err_msg' => $errors['10005']));
            exit();
        }
        while ($PaymentModesResult = mysqli_fetch_assoc($PaymentModes)) {
            $response['PaymentModesResult'][] = $PaymentModesResult;
        }

        /*---- Fetching data to check system is full or half ----*/
        //$full_half = mysqli_fetch_assoc(mysqli_query($conn, "SELECT a.is_full FROM sps_gate_configurations a, sps_ip_address b WHERE a.gate_ip = b.ip_id  AND a.gate_ip =  '" . $GateConfig['gate_id'] . "'"));
        $response['is_full'] = $GateConfig['is_full']['is_full'];


        /*---- Tariff calculation query ----*/
        $tariff_calculation_applied_query = mysqli_fetch_assoc(mysqli_query($conn, "SELECT tariff_calculation_applied FROM sps_settings"));
        $tariff_calculation_applied = $tariff_calculation_applied_query['tariff_calculation_applied'];
        $response['tariff_calculation_applied'] = $tariff_calculation_applied_query['tariff_calculation_applied'];

        $response['casualTariff'] = array();
        $response['casualTariffTier'] = array();
        $response['overnightTariff'] = array();
        $response['additionalTariff'] = array();
        $response['specialDay'] = array();
        $response['focReasons'] = array();
        $response['focApprovers'] = array();
        $response['taxes'] = array();
        $response['casual_discounts'] = array();
        $response['f8_reasons'] = array();

        $casualTariffQuery = mysqli_query($conn, "SELECT * FROM sps_tariff WHERE  sps_tariff.status = 1 AND deleted_at IS NULL ");
        while ($result = mysqli_fetch_assoc($casualTariffQuery)) {
            $response['casualTariff'][] = $result;
            $tierTariffQuery = mysqli_query($conn, "SELECT * FROM `sps_tariff_tier` WHERE `tariff_id` = '" . $result['id'] . "' AND deleted_at IS NULL");
            while ($tier = mysqli_fetch_assoc($tierTariffQuery)) {
                $response['casualTariffTier'][] = $tier;
            }
        }

        $overnightTariffQuery = mysqli_query($conn, "SELECT * FROM sps_tariff_overnight WHERE  status=1 AND deleted_at IS NULL ");
        while ($overnightTariff = mysqli_fetch_assoc($overnightTariffQuery)) {
            $response['overnightTariff'][] = $overnightTariff;
        }

        $additionalTariffQuery = mysqli_query($conn, "SELECT sta.*,sst.type as service_type FROM sps_tariff_additionals as sta JOIN sps_service_type as sst ON sta.service_type=sst.id WHERE sta.status=1 AND  sta.deleted_at IS NULL ");
        while ($additionalTariff = mysqli_fetch_assoc($additionalTariffQuery)) {
            $response['additionalTariff'][] = $additionalTariff;
        }

        $gateTypesQuery = mysqli_query($conn, "SELECT * FROM `sps_gate_type`  WHERE  status=1  AND  deleted_at IS NULL");
        while ($gateType = mysqli_fetch_assoc($gateTypesQuery)) {
            $response['gateTypesMaster'][] = $gateType;
        }
        $ticketLayoutQuery = mysqli_query($conn, "SELECT * FROM `sps_ticket_layouts` WHERE  status=1  AND  deleted_at IS NULL");
        $ticketLayoutIds = array();
        while ($ticketLayout = mysqli_fetch_assoc($ticketLayoutQuery)) {
            $ticketLayoutIds[] = $ticketLayout['layout_id'];
            $imagesResult = mysqli_query($conn, "SELECT * FROM sps_images WHERE module = 'ticket' AND module_id = '" . $ticketLayout['layout_id'] . "'");
            //$imgCnt = 0;
            //US872: New Receipt Layout and Format
            while ($image = mysqli_fetch_assoc($imagesResult)) {
                $module_id = $image['module_id'];
                if ($image['file_name'] == $ticketLayout['logo']) {
                    $ticketLayout['logo'] = $storage_image_url . 'ticket/' . md5($module_id) . '/' . $image['file_name'];
                } elseif ($image['file_name'] == $ticketLayout['logo_b']) {
                    $ticketLayout['logo_b'] = $storage_image_url . 'ticket/' . md5($module_id) . '/' . $image['file_name'];
                } elseif ($image['file_name'] == $ticketLayout['logo_c']) {
                    $ticketLayout['logo_c'] = $storage_image_url . 'ticket/' . md5($module_id) . '/' . $image['file_name'];
                }
                //$imgCnt++;
            }
            $response['ticketLayout'][] = $ticketLayout;
        }
        if (count($ticketLayoutIds) >= 1) {
            $ticketLayoutIds = implode(',', $ticketLayoutIds);
            $ticketOptionsQuery = mysqli_query($conn, "SELECT * FROM `sps_ticket_options` WHERE option_value = 1 AND layout_id IN(" . $ticketLayoutIds . ")");
            while ($ticketOptions = mysqli_fetch_assoc($ticketOptionsQuery)) {
                $response['ticketOptions'][] = $ticketOptions;
            }
        }


        $siteSettingsQuery = mysqli_query($conn, "SELECT sps_settings.*,sps_area.area_name,sps_area.area_code AS area_id,sps_location.location_name, sps_location.location_code FROM `sps_settings` LEFT JOIN sps_area ON sps_area.area_id = sps_settings.area_id LEFT JOIN sps_location ON sps_location.location_id = sps_settings.location_id");
        while ($siteSettings = mysqli_fetch_assoc($siteSettingsQuery)) {
            $response['siteSettings'][] = $siteSettings;
        }

        $specialDayQuery = mysqli_query($conn, "SELECT `date` FROM `sps_special_days` WHERE deleted_at IS NULL");
        while ($specialDay = mysqli_fetch_assoc($specialDayQuery)) {
            $response['specialDay'][] = $specialDay['date'];
        }

//        $productsQuery = mysqli_query($conn, "SELECT * FROM sps_tariff_member WHERE deleted_at IS NULL AND status = 1");
//        while ($product = mysqli_fetch_assoc($productsQuery)) {
//            $response['membershipProducts'][] = $product;
//        }

        $focReasonsQuery = mysqli_query($conn, "SELECT id,reason FROM sps_foc_reasons WHERE deleted_at IS NULL");
        while ($focReason = mysqli_fetch_assoc($focReasonsQuery)) {
            $response['focReasons'][] = $focReason;
        }

        $focApproversQuery = mysqli_query($conn, "SELECT sps_foc_approvers.id AS user_id,CONCAT(sps_users.first_name, ' ', sps_users.last_name) AS username FROM sps_foc_approvers LEFT JOIN sps_users ON  sps_foc_approvers.user_id = sps_users.user_id WHERE sps_foc_approvers.deleted_at IS NULL");
        while ($focApprover = mysqli_fetch_assoc($focApproversQuery)) {
            $response['focApprovers'][] = $focApprover;
        }

        $taxQuery = mysqli_query($conn, "SELECT id,label,percentage,calculation_base FROM sps_tax WHERE label !='Base Fare' AND deleted_at IS NULL");
        while ($tax = mysqli_fetch_assoc($taxQuery)) {
            $response['taxes'][] = $tax;
        }

        $discountQuery = mysqli_query($conn, "SELECT id,discount_code,no_of_visits,duration,discount_type,value,start_date,end_date,start_time,end_time,vehicle_type,gate,day_type FROM sps_discounts WHERE deleted_at IS NULL");
        while ($discount = mysqli_fetch_assoc($discountQuery)) {
            $response['casual_discounts'][] = $discount;
        }

        $feightReasonsQuery = mysqli_query($conn, "SELECT id,reason FROM sps_f_eight_reason WHERE deleted_at IS NULL");
        while ($reason = mysqli_fetch_assoc($feightReasonsQuery)) {
            $response['f8_reasons'][] = $reason;
        }

        $response['day_type'][] = array('1' => 'Weekdays', '2' => 'Weekends', '3' => 'Special Days');
        $response['tarrif_unit'][] = array('1' => 'Per Minute', '2' => 'Per Hour');
        $response['tier_type'][] = array('1' => 'Flat', '2' => 'Progressive');
        $response['overnight_tariff_calculation'][] = array('1' => 'Parking Charge + Overnight Charge', '2' => 'Split the Parking Charge + Overnight Charge');
        /* added for cps gate */
        $cps_gate = mysqli_query($conn, "select gate_id,gate_name,grace_period from sps_gate_configurations where gate_type = '5'");
        while ($cps_gateResults = mysqli_fetch_assoc($cps_gate)) {
            $response['cpsList'][] = $cps_gateResults;
        }
        /* end */
        echo json_encode(array('err_code' => '0', 'data' => $response));
        createLog($logString);
        break;

    case "login":

        $username = trim($_REQUEST['username']);
        $password = trim(md5($_REQUEST['password']));
        /*---- Query to fetch user id against the username ----*/
        $user_id_query = "SELECT sps_users.user_id,sps_users.user_role,sps_users.user_name,sps_user_role.role,CONCAT( sps_users.first_name,  ' ', sps_users.last_name ) as name  FROM sps_users LEFT JOIN sps_user_role ON sps_user_role.id = sps_users.user_role  WHERE sps_users.password = '$password' AND sps_users.user_name = '$username' AND sps_users.deleted_at is NULL";
        $logString .= $user_id_query . "\n" . PHP_EOL;
        $user_id_db = mysqli_fetch_assoc(mysqli_query($conn, $user_id_query));
        if (is_null($user_id_db)) {
            echo json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
            $logString .= $user_id_query . "\n" . PHP_EOL . 'err_msg->' . $errors['10006'];
            createLog($logString);
            exit();
        }
        $userid = $user_id_db['user_id'];
        $response['user_id'] = $userid;
        $response['user_role'] = $user_id_db['user_role'];
        $response['role'] = $user_id_db['role'];
        $response['user_name'] = $user_id_db['user_name'];
        $response['name'] = $user_id_db['name'];
        if ($response['user_role'] == 1 || $response['role'] == 'Auditor') {
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        /*---- Query to fetch gate id against user id from sps_shift_gate_mapping table ----*/
        $query_gate_id_user_id = "SELECT a.gate_id FROM sps_shift_gate_mapping a, sps_shift_management b WHERE a.shift_id = b.sr_no AND b.shift_activate_status='1' AND b.user_id ='$userid' AND b.deleted_at IS NULL";
        $logString .= $query_gate_id_user_id . "\n" . PHP_EOL;
        $gate_id_db = mysqli_fetch_assoc(mysqli_query($conn, $query_gate_id_user_id));
        $gate_id = $gate_id_db['gate_id'];

        /*---- Query to fetch gate id againt ip address from sps_gate_configuration table ----*/
        $query_gate_id_ip_address = "SELECT a.gate_id FROM sps_gate_configurations a, sps_ip_address b WHERE a.gate_ip = b.ip_id AND b.ip_address ='" . $_REQUEST['ipaddress'] . "' AND a.deleted_at IS NULL AND b.deleted_at IS NULL";
        $logString .= $query_gate_id_ip_address . "\n" . PHP_EOL;
        $gate_id_ip = mysqli_fetch_assoc(mysqli_query($conn, $query_gate_id_ip_address));
        $gate_id_ipaddress = $gate_id_ip['gate_id'];

        /*---- Query to fetch the shift status of the current user who is trying to login ----*/
        $query_shift_status_current_user = "SELECT a.sr_no , a.shift_activate_status,a.shift_start,a.f_eight_granted,a.f_eight_used FROM sps_shift_management a , sps_users b  where a.user_id = b.user_id AND a.user_id='$userid' AND a.shift_activate_status='1' AND a.end_shift_status = '0'  AND a.deleted_at IS NULL AND b.deleted_at IS NULL";
        $logString .= $query_shift_status_current_user . "\n" . PHP_EOL;
        $shift_id_result = mysqli_fetch_assoc(mysqli_query($conn, $query_shift_status_current_user));
        $shift_id = $shift_id_result['sr_no'];
        $shift_start_time = $shift_id_result['shift_start'];


        /*---- find the shift time from user login ----*/
        $shift_time_start_res = explode(' ', $shift_start_time);
        $shift_time_result = $shift_time_start_res[1];
        $response['shift_id'] = $shift_id;
        $response['f_eight_granted'] = $shift_id_result['f_eight_granted'];
        $response['f_eight_used'] = $shift_id_result['f_eight_used'];

        /*---- Query to fetch shift id and gate id against shift id from sps_shift_gate_mapping ----*/
        $query_shift_id_gate_id = "SELECT ssgm.shift_id ,ssgm.gate_id FROM sps_shift_gate_mapping as ssgm  LEFT JOIN sps_gate_configurations as sgc ON ssgm.gate_id = sgc.gate_id LEFT JOIN sps_ip_address as sip ON sgc.gate_ip = sip.ip_id where shift_id = '$shift_id' AND sip.ip_address = '" . $_REQUEST['ipaddress'] . "'";
        $logString .= $query_shift_id_gate_id . "\n" . PHP_EOL;
        $shiftIdFromMapping = mysqli_fetch_assoc(mysqli_query($conn, $query_shift_id_gate_id));
        $shiftGateId = $shiftIdFromMapping['gate_id'];
        $response['gate_id'] = $shiftIdFromMapping['gate_id'];
        /*---- Fetching username and password from server to validate the user ----*/
        $query_username_password = "SELECT user_name, password FROM sps_users WHERE BINARY user_name = '$username' AND deleted_at IS NULL";
        $logString .= $query_username_password . "\n" . PHP_EOL;
        $password_result = mysqli_fetch_assoc(mysqli_query($conn, $query_username_password));
        $oldPassworddb = $password_result['password'];
        $oldusernamedb = $password_result['user_name'];

        /*---- Validating username and password ----*/
        if ($username == $oldusernamedb and $password == $oldPassworddb) {
            /*---- validating gate_id from sps_shift_mapping table against sps_gate_configuration table ----*/
            if ($shift_id_result > 0) {
                if ($shiftGateId == $gate_id_ipaddress) {
                    //validation of before user login
                    $query_validation_user_login = "SELECT a.sr_no, b.user_id, DATE_FORMAT( a.shift_start,'%Y-%m-%d') , a.shift_activate_status, a.end_shift_status,b.user_name, b.password, b.status, b.deleted_at,  c.gate_id,a.first_login_time FROM sps_shift_management a, sps_users b, sps_shift_gate_mapping c WHERE b.user_id = a.user_id AND b.user_id ='$userid' AND c.shift_id = a.sr_no AND b.user_id ='$userid' AND b.deleted_at IS NULL AND b.status = '1' AND a.shift_activate_status = '1' AND a.end_shift_status = '0' AND a.deleted_at IS NULL AND b.deleted_at IS NULL";
                    $logString .= $query_validation_user_login . "\n" . PHP_EOL;
                    $validate_before_login = mysqli_fetch_array(mysqli_query($conn, $query_validation_user_login)); // 36045 - first_login_time added in query
                    if ($validate_before_login > 0) {
                        /* 36045 */
                        if (is_null($validate_before_login['first_login_time']) || empty($validate_before_login['first_login_time'])) {
                            $now_date = date("Y-m-d H:i:s");
                            $update_first_login = "UPDATE sps_users JOIN sps_shift_management ON sps_users.user_id = sps_shift_management.user_id SET sps_shift_management.first_login_time='$now_date' WHERE sps_users.user_id=sps_shift_management.user_id AND sps_users.user_id='$userid' AND sps_users.deleted_at IS NULL AND sps_shift_management.deleted_at IS NULL AND sps_shift_management.sr_no = $shift_id ";
                            $logString .= $update_first_login . "\n" . PHP_EOL;
                            mysqli_query($conn, $update_first_login);
                        }
                        /* end */
                        $update_after_login = "UPDATE sps_users JOIN sps_shift_management ON sps_users.user_id = sps_shift_management.user_id SET sps_users.login_status='1' , sps_shift_management.shift_login_status='1' WHERE sps_users.user_id=sps_shift_management.user_id AND sps_users.user_id='$userid' AND sps_users.deleted_at IS NULL AND sps_shift_management.deleted_at IS NULL AND sps_shift_management.sr_no = $shift_id ";
                        $logString .= $update_after_login . "\n" . PHP_EOL;
                        /*---- Query to insert data into sps_cashiers_log table ----*/
                        $now_date = date("Y-m-d H:i:s");
                        $querySpsCachiersLog = "insert into sps_cashiers_log(user_id,gate_id,shift_id,login_time,logout_time,location_id,area_id) values ('$userid','$gate_id','$shift_id','$now_date',Null,0,0)";
                        $logString .= $querySpsCachiersLog . "\n" . PHP_EOL;
                        mysqli_query($conn, $querySpsCachiersLog);

                        if (mysqli_query($conn, $update_after_login)) {
                            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                            echo $response_data;
                            $logString .= $response_data . "\n" . PHP_EOL;
                            createLog($logString);
                            exit();
                        } else {
                            $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
                            echo $response_data;
                            $logString .= $response_data . "\n" . PHP_EOL;
                            createLog($logString);
                            exit();
                        }
                    } else {
                        /*---- Error if user is inactive ----*/
                        $response_data = json_encode(array('err_code' => '10008', 'err_msg' => $errors['10008']));
                        echo $response_data;
                        $logString .= $response_data . "\n" . PHP_EOL;
                        createLog($logString);
                        exit();
                    }
                } else {
                    /*---- Error if Gate id does not match ----*/
                    $response_data = json_encode(array('err_code' => '10009', 'err_msg' => $errors['10009']));
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();
                }
            } else {
                /*---- Error if shift is not activated ----*/
                $response_data = json_encode(array('err_code' => '10010', 'err_msg' => $errors['10010']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
        } else {
            /*---- Error if Username or password are incorrect ----*/
            $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;

    /*---- Case 0020 logout of F10 ----*/
    case "logout":

        $get_user_id = trim($_REQUEST['user_id']);

        $query_user_id_user_role = "SELECT a.user_id, a.user_role from sps_users a where a.user_id ='$get_user_id' AND a.deleted_at IS NULL";
        $logString .= $query_user_id_user_role . "\n" . PHP_EOL;
        $user_id_float = mysqli_fetch_assoc(mysqli_query($conn, $query_user_id_user_role));
        $user_id = $user_id_float['user_id'];
        if ($user_id_float['user_role'] == 1) {
            $update_logout = "UPDATE sps_users SET login_status='1' WHERE user_id = '" . $user_id . "' AND deleted_at IS NULL";
            $logString .= $update_logout . "\n" . PHP_EOL;
            $update_logout_query = mysqli_query($conn, $update_logout);
            $response['user_id'] = $user_id;
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        /*---- Query to fetch user id against username from sps_users and sps_shift_management  ---- */
        $query_user_id_username = "SELECT a.user_id, b.user_id,a.user_role from sps_users a,sps_shift_management b where a.user_id = b.user_id AND a.user_id ='$get_user_id' AND a.deleted_at IS NULL AND b.deleted_at IS NULL";
        $logString .= $query_user_id_username . "\n" . PHP_EOL;
        $user_id_float = mysqli_fetch_assoc(mysqli_query($conn, $query_user_id_username));
        $user_id = $user_id_float['user_id'];
        //echo "<pre>";print_r($user_id_float);exit;
        if ($user_id > 0) {


            /*---- update record at F10 logout time in database of shift management table  ----*/
            $update_logout = "UPDATE sps_users JOIN sps_shift_management ON sps_users.user_id = sps_shift_management.user_id SET sps_users.login_status='1' , sps_shift_management.shift_login_status='1'  WHERE sps_users.user_id=sps_shift_management.user_id AND sps_users.user_id='$user_id' AND sps_users.deleted_at IS NULL AND sps_shift_management.deleted_at IS NULL";
            $logString .= $update_logout . "\n" . PHP_EOL;
            $update_logout_query = mysqli_query($conn, $update_logout);

            $now_date = date("Y-m-d H:i:s");
            $update_cashier = "UPDATE sps_cashiers_log a JOIN sps_shift_management b ON a.shift_id = b.sr_no AND a.user_id ='$user_id' SET a.logout_time ='$now_date' WHERE a.user_id ='$user_id'";
            $logString .= $update_cashier . "\n" . PHP_EOL;
            $update_cashier_query = mysqli_query($conn, $update_cashier);

            if ($update_logout_query && $update_cashier_query) {
                $response['user_id'] = $user_id;
                $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();

            } else {
                /*---- Error if Username or password are incorrect ----*/
                $response_data = json_encode(array('err_code' => '10011', 'err_msg' => $errors['10011']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }

        } else {
            /*---- Error if Username or password are incorrect ----*/
            $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;

    case "endshift":
        $user_id = trim($_REQUEST['user_id']);
        $shift_id = trim($_REQUEST['shift_id']);


        /*---- Query to fetch user id against username from sps_users  ---- */
        $query_user_id_against_username = "SELECT user_id FROM sps_users WHERE user_id = '$user_id' AND deleted_at IS NULL";
        $logString .= $query_user_id_against_username . "\n" . PHP_EOL;
        $user_id_db = mysqli_fetch_assoc(mysqli_query($conn, $query_user_id_against_username));
        $userid = $user_id_db['user_id'];
        if ($userid <= 0 || empty($userid)) {
            $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }

        /*---- Query to fetch login status,shift login status ,end shift status shift end from sps_users and sps_shift_management ----*/
        $query_login_status_shift_login_status_end_shift_status = "SELECT  b.sr_no,a.login_status, b.shift_login_status, b.end_shift_status, b.shift_end FROM sps_users a, sps_shift_management b WHERE a.user_id=b.user_id AND b.end_shift_status='0' AND a.user_id='" . $userid . "' AND b.sr_no='" . $shift_id . "' AND a.deleted_at IS NULL AND b.deleted_at IS NULL";
        $logString .= $query_login_status_shift_login_status_end_shift_status . "\n" . PHP_EOL;
        $querystatus = mysqli_query($conn, $query_login_status_shift_login_status_end_shift_status);
        $result = mysqli_fetch_array($querystatus);
        if ($result > 0) {
            /*----   update record at end shift time         ----*/
            $now_date = date("Y-m-d H:i:s");
            $updatestatus = "UPDATE sps_users JOIN sps_shift_management ON sps_users.user_id = sps_shift_management.user_id SET sps_users.login_status='0' , sps_shift_management.shift_login_status='0' , sps_shift_management.end_shift_status='1', sps_shift_management.shift_end='" . $now_date . "' WHERE sps_shift_management.sr_no = '" . $result['sr_no'] . "' AND sps_shift_management.deleted_at IS NULL AND sps_users.deleted_at IS NULL";
            $logString .= $updatestatus . "\n" . PHP_EOL;
            if (mysqli_query($conn, $updatestatus)) {
                $updateCashier = "UPDATE sps_cashiers_log a JOIN sps_shift_management b ON a.shift_id = b.sr_no AND b.sr_no =  '" . $result['sr_no'] . "' SET a.logout_time = NOW( ) WHERE b.sr_no =  '" . $result['sr_no'] . "' AND a.logout_time IS NULL";
                $logString .= $updateCashier . "\n" . PHP_EOL;
                mysqli_query($conn, $updateCashier);
                $response['msg'] = 'Shift end successfully!';
                $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();

            } else {
                $response_data = json_encode(array('err_code' => '10012', 'err_msg' => $errors['10012']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
        } else {
            $response_data = json_encode(array('err_code' => '10013', 'err_msg' => $errors['10013']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;

    /*Case 0006 for Change password*/
    case "changePassword":
        $user_id = trim($_REQUEST['user_id']);
        $oldPassword = trim(md5($_REQUEST['password']));
        $newPassword = trim(md5($_REQUEST['newpassword']));
        /*---- Query to fetch password against username from sps_users ----*/
        $query_password_against_username = "SELECT password FROM sps_users WHERE BINARY user_id = '$user_id' AND password = '" . $oldPassword . "' AND deleted_at IS NULL";
        $logString .= $query_password_against_username . "\n" . PHP_EOL;
        $password_result = mysqli_fetch_assoc(mysqli_query($conn, $query_password_against_username));
        if (!is_null($password_result)) {
            $querychange = "UPDATE sps_users SET password='$newPassword' WHERE user_id='$user_id' AND deleted_at IS NULL";
            $logString .= $querychange . "\n" . PHP_EOL;
            if (mysqli_query($conn, $querychange)) {
                $response['msg'] = 'Password updated successfully';
                $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            } else {
                $response_data = json_encode(array('err_code' => '10014', 'err_msg' => $errors['10014']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
        } else {
            $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;

    /*---- Case 0004 for Entry Gate data insertion ----*/
    case "entry_transaction":
        try {

            /* US793: Dynamic QR Code – Mobile Gate 
               Allowing to set payment reference number in case of master payment mode E wallet
            */
            if ($_REQUEST['in_master_payment_mode'] == 4 && ($_REQUEST['in_payment_mode'] == 5 || $_REQUEST['in_payment_mode'] == 7) && !empty($_REQUEST['in_payment_referance_number'])) {
                $in_payment_reference_number = trim($_REQUEST['in_payment_referance_number']);
            } else {
                $_REQUEST['in_payment_referance_number'] = 0;
                $in_payment_reference_number = 0;
            }
            if (!empty($in_payment_reference_number)) {
                isValidPaymentReferenceNumber($conn, $errors, $logString, $in_payment_reference_number);
            }
            /* US793: Dynamic QR Code – Mobile Gate */

            mysqli_begin_transaction($conn);
            $transactionId = NULL;
            $ingate = trim($_REQUEST['in_gate']);
            $vh_type = trim($_REQUEST['vehicle_type']);
            $in_userid = trim($_REQUEST['in_user_id']);
            $in_time = trim($_REQUEST['in_time']);
            $vh_number = trim($_REQUEST['vehicle_number']);
            $ticket_no = trim($_REQUEST['ticket_no']);
            $barcode = trim($_REQUEST['barcode']);
            $in_type = trim($_REQUEST['in_type']);
            $in_type_id = trim($_REQUEST['in_type_id']);
            $shift_id = trim($_REQUEST['in_shift_id']);
            $imagePos1 = trim($_REQUEST['image1_pos']);
            $imagePos2 = trim($_REQUEST['image2_pos']);
            $in_foc_difference = trim($_REQUEST['in_foc_difference']);
            $foc = trim($_REQUEST['in_foc']);
            $foc_reason = trim($_REQUEST['in_foc_reason']);
            $foc_approval = trim($_REQUEST['in_foc_approval']);
            $foc_note = trim($_REQUEST['in_foc_note']);
            $in_payment_mode = trim($_REQUEST['in_payment_mode']);
            $in_master_payment_mode = trim($_REQUEST['in_master_payment_mode']);
            $in_standard_parking_amount = trim($_REQUEST['in_standard_parking_amount']);
            $in_membership_payment_amount = trim($_REQUEST['in_membership_payment_amount']); // 35688
            $in_payment_amount = trim($_REQUEST['in_payment_amount']);
            $total_payment_amount = trim($_REQUEST['total_payment_amount']);
            $full_gate = trim($_REQUEST['full_gate']);
            $in_day_type = trim($_REQUEST['in_day_type']);
            $location_id = trim($_REQUEST['location_id']);
            $area_id = trim($_REQUEST['area_id']);
            $in_tariff_code = trim($_REQUEST['in_tariff_code']);

            $inData = array();
            if (!empty($_REQUEST['barcode'])) {
                $barcode = $_REQUEST['barcode'];
                $query = "SELECT id,offline_ticket FROM sps_transactions WHERE barcode = '$barcode' AND deleted_at IS NULL";
                $logString .= $query . "\n" . PHP_EOL;
                $result = mysqli_query($conn, $query);
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    $transactionId = $transaction['id'];
                    $offline_bit = NULL;
                    if ($transaction['offline_ticket'] == 3) {
                        $offline_bit = 4;
                    } elseif ($transaction['offline_ticket'] == 12) {
                        $offline_bit = 13;
                    } elseif ($transaction['offline_ticket'] == 17) {
                        $offline_bit = 18;
                    } elseif ($transaction['offline_ticket'] == 15) {
                        $offline_bit = 16;
                    } elseif (is_null($transaction['offline_ticket'])) {
                        $offline_bit = $_REQUEST['offline_ticket'];
                    }
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 1 && !empty($offline_bit)) {
                        $updatetransactions_lost = "UPDATE sps_transactions SET offline_ticket='$offline_bit'  WHERE id= '$transactionId' AND deleted_at IS NULL";
                        $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
                        mysqli_query($conn, $updatetransactions_lost);
                    }
                } else {
                    $inData = array('barcode' => $_REQUEST['barcode']);
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 1) {
                        $inData['offline_ticket'] = 1;
                    }

                    $sps_transaction_query = insert($conn, 'sps_transactions', $inData);
                    $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                    if (mysqli_query($conn, $sps_transaction_query)) {
                        $transactionId = mysqli_insert_id($conn);;
                    }
                }
            } elseif (!empty($_REQUEST['in_type_id']) && !empty($_REQUEST['in_type']) && empty($_REQUEST['manuel_ticket']) && is_null($lost_ticket) && isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 1) {
                $in_type_id = trim($_REQUEST['in_type_id']);
                $in_type = trim($_REQUEST['in_type']);
                $query = "SELECT id,offline_ticket FROM sps_transactions WHERE out_type = '$in_type' AND out_type_id = '$in_type_id' AND deleted_at IS NULL";
                $logString .= $query . "\n" . PHP_EOL;
                $result = mysqli_query($conn, $query);
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    $transactionId = $transaction['id'];
                    $offline_bit = NULL;
                    if ($transaction['offline_ticket'] == 3) {
                        $offline_bit = 4;
                    } elseif ($transaction['offline_ticket'] == 12) {
                        $offline_bit = 13;
                    } elseif ($transaction['offline_ticket'] == 17) {
                        $offline_bit = 18;
                    } elseif ($transaction['offline_ticket'] == 15) {
                        $offline_bit = 16;
                    } elseif (is_null($transaction['offline_ticket'])) {
                        $offline_bit = $_REQUEST['offline_ticket'];
                    }
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 1 && !empty($offline_bit)) {
                        $updatetransactions_lost = "UPDATE sps_transactions SET offline_ticket='$offline_bit'  WHERE id= '$transactionId' AND deleted_at IS NULL";
                        $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
                        mysqli_query($conn, $updatetransactions_lost);
                    }

                } else {
                    $inData = array('in_type' => $_REQUEST['in_type'], 'in_type_id' => $_REQUEST['in_type_id']);
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 1) {
                        $inData['offline_ticket'] = 1;
                    }
                    $sps_transaction_query = insert($conn, 'sps_transactions', $inData);
                    $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                    if (mysqli_query($conn, $sps_transaction_query)) {
                        $transactionId = mysqli_insert_id($conn);
                    }
                }

            }


            $discounts = null;
            if (!empty($_REQUEST['in_discount'])) {
                $discounts = json_decode($_REQUEST['in_discount'], true);
                unset($_REQUEST['in_discount']);

            }
            $additionalCharges = null;
            if (!empty($_REQUEST['in_additional_charges'])) {
                $additionalCharges = json_decode($_REQUEST['in_additional_charges'], true);
                unset($_REQUEST['in_additional_charges']);
            }
            $taxBreakup = null;
            if (!empty($_REQUEST['in_tax_breakup'])) {
                $taxBreakup = json_decode($_REQUEST['in_tax_breakup'], true);
                unset($_REQUEST['in_tax_breakup']);

            }
            if (empty($_REQUEST['in_foc_note'])) {
                $_REQUEST['in_foc_note'] = 0;
            }

            /*---- Query to fetch vehicle number from sps_transactions table ---- */
            //echo "SELECT vehicle_number FROM sps_transactions WHERE vehicle_number ='$vh_number' AND in_gate IS NOT NULL AND out_gate IS NULL";exit;
            //$querytransactions_vehicle = mysqli_query($conn, "SELECT vehicle_number FROM sps_transactions WHERE vehicle_number ='$vh_number' AND in_time != '0000-00-00 00:00:00' AND out_gate IS NULL AND deleted_at IS NULL");
            $query_vehicle_number_sps_transactions = "SELECT vehicle_number FROM sps_transactions WHERE vehicle_number ='$vh_number' AND vehicle_type='$vh_type' AND in_shift_id IS  NOT NULL  AND out_gate IS NULL AND deleted_at IS NULL";
            $logString .= $query_vehicle_number_sps_transactions . "\n" . PHP_EOL;
            $querytransactions_vehicle = mysqli_query($conn, $query_vehicle_number_sps_transactions);
            $data = mysqli_fetch_array($querytransactions_vehicle, MYSQLI_NUM);
            if ($data > 0) {
                $response_data = json_encode(array('err_code' => '10015', 'err_msg' => $errors['10015']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            } else {
                /*---- insert data at sps_transactions table ----*/
                $postData = $_REQUEST;
                unset($postData['cam_type'], $postData['corporate_id'], $postData['cmd'], $postData['image1_pos'], $postData['image2_pos'], $postData['ipaddress'], $postData['full_gate']);
                if (is_null($transactionId)) {
                    $sps_transaction_query = insert($conn, 'sps_transactions', $postData);
                    $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                    mysqli_query($conn, $sps_transaction_query);
                    $sps_transaction_id = mysqli_insert_id($conn);
                } else {
                    $sps_transaction_query = "UPDATE sps_transactions SET in_time='$in_time', in_gate='$ingate',vehicle_type='$vh_type',total_payment_amount= total_payment_amount +'$total_payment_amount',in_payment_amount='$in_payment_amount',f_eight=NULL,in_standard_parking_amount = '$in_standard_parking_amount',in_master_payment_mode = '$in_master_payment_mode',in_payment_mode = '$in_payment_mode',in_foc_note = '$foc_note',in_foc_approval = '$foc_approval',in_foc_reason = '$foc_reason',in_foc = '$foc',in_foc_difference = '$in_foc_difference',in_shift_id = '$shift_id',in_type_id = '$in_type_id',in_type = '$in_type',ticket_no = '$ticket_no',vehicle_number = '$vh_number',in_user_id = '$in_userid',in_day_type = '$in_day_type',in_tariff_code = '$in_tariff_code',in_membership_payment_amount = '$in_membership_payment_amount', in_payment_referance_number = '$in_payment_reference_number' WHERE id= '$transactionId' AND deleted_at IS NULL";
                    $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                    mysqli_query($conn, $sps_transaction_query);
                    $sps_transaction_id = $transactionId;
                }

                if ($sps_transaction_id) {
                    $response['msg'] = 'New transaction created successfully!';
                    $response['transaction_id'] = $sps_transaction_id;
                    //$sps_transaction_id = $response['transaction_id'];
                    $md5_of_transaction_id = md5($sps_transaction_id);

                    $membership = array("2", "5");
                    $query_reservTotalQuery = "SELECT member_resv_total,vip_resv_total,member_resv_left,vip_resv_left FROM sps_corporates WHERE corporate_id  ='$corporate_id'";
                    $logString .= $query_reservTotalQuery . "\n" . PHP_EOL;
                    $reservTotalQuery = mysqli_query($conn, $query_reservTotalQuery);
                    $reservTotalQueryData = mysqli_fetch_array($reservTotalQuery, MYSQLI_NUM);
                    if ($reservTotalQueryData['member_resv_total'] > 0 || $reservTotalQueryData['vip_resv_total'] > 0) {
                        if ($_REQUEST['vip_membership_slot'] == 1 && in_array($_REQUEST['in_type'], $membership)) {
                            $corporate_id = $_REQUEST['corporate_id'];
                            $query_sps_corporates_vip_resv_left = "UPDATE sps_corporates SET vip_resv_left = vip_resv_left + 1 WHERE vip_resv_left < vip_resv_total AND corporate_id  ='$corporate_id'";
                            $logString .= $query_sps_corporates_vip_resv_left . "\n" . PHP_EOL;
                            mysqli_query($conn, $query_sps_corporates_vip_resv_left);

                        } elseif ($_REQUEST['vip_membership_slot'] == 0 && in_array($_REQUEST['in_type'], $membership)) {
                            $corporate_id = $_REQUEST['corporate_id'];
                            $query_sps_corporates_member_resv_left = "UPDATE sps_corporates SET member_resv_left = member_resv_left + 1 WHERE member_resv_left < member_resv_total AND corporate_id ='$corporate_id'";
                            $logString .= $query_sps_corporates_member_resv_left . "\n" . PHP_EOL;
                            mysqli_query($conn, $query_sps_corporates_member_resv_left);
                        }
                    }

                    if (!is_null($additionalCharges)) {
                        foreach ($additionalCharges as $additionalCharge) {
                            $additionalData = array();
                            $additionalData['transaction_id'] = $sps_transaction_id;
                            $additionalData['additional_tariff_id'] = $additionalCharge['additional_tariff_id'];
                            $additionalData['additional_tariff_amount'] = $additionalCharge['additional_tariff_amount'];
                            $additionalData['additional_charges_at'] = $additionalCharge['additional_charges_at'];
                            $additionalData['gate_id'] = $additionalCharge['gate_id'];
                            $transaction_additional_charges = insert($conn, 'sps_transaction_additional_charges', $additionalData);
                            mysqli_query($conn, $transaction_additional_charges);
                            $logString .= $transaction_additional_charges . "\n" . PHP_EOL;
                            $additionalData = array();

                        }
                    }
                    if (!is_null($taxBreakup)) {
                        foreach ($taxBreakup as $tax) {
                            $taxData = array();
                            $taxData['tax_id'] = $tax['tax_id'];
                            $taxData['tax_amount'] = $tax['tax_value'];
                            $taxData['module'] = $tax['module'];
                            $taxData['module_id'] = $sps_transaction_id;
                            $taxData['location_id'] = $location_id;
                            $taxData['area_id'] = $area_id;
                            $transaction_tac_breakup = insert($conn, 'sps_tax_breakup', $taxData);
                            mysqli_query($conn, $transaction_tac_breakup);
                            $logString .= $transaction_tac_breakup . "\n" . PHP_EOL;
                        }
                    }

                    if (!is_null($discounts)) {
                        foreach ($discounts as $discount) {
                            $discountData = array();
                            $discountData['transaction_id'] = $sps_transaction_id;
                            //'discount_type' => array('1'=>'Registered Vouchers','2'=>'Unregistered Vouchers','3'=>'Casual Discount'),
                            $discountData['discount_type'] = $discount['discount_type'];
                            //Registered Vouchers = voucher code, Unregistered Vouchers = category id, Casual Discount = 0;
                            $discountData['discount_type_id'] = $discount['discount_type_id'];
                            //'1'=>'Percentage', '2'=>'Random Value', '3'=>'Full Value'
                            $discountData['discount_value_type'] = $discount['discount_value_type'];
                            $discountData['discount_value'] = $discount['discount_value'];
                            $discountData['discount_amount'] = $discount['discount_amount'];
                            $discountData['gate_id'] = $discount['gate_id'];
                            $discountData['discount_at'] = $discount['discount_at'];
                            $sps_transaction_discount = insert($conn, 'sps_transaction_discount', $discountData);
                            mysqli_query($conn, $sps_transaction_discount);
                            $logString .= $sps_transaction_discount . "\n" . PHP_EOL;
                            $discount_type_id = $discount['discount_type_id'];
                            $increaseCountVoucherEntry = "update sps_vouchers set used_count = used_count + 1 where status='1' AND deleted_at IS NULL AND id='$discount_type_id'";
                            mysqli_query($conn, $increaseCountVoucherEntry);
                            $logString .= $increaseCountVoucherEntry . "\n" . PHP_EOL;
                        }
                    }
                    //echo 'full_gate=>'.$full_gate;
                    if (!empty($full_gate) && $full_gate == '1') {
                        $update_transactions = "UPDATE sps_transactions SET 
                            out_time='" . $in_time . "',
                            out_gate='$ingate',
                            out_user_id='$in_userid',
                            out_shift_id = '$shift_id',                            
                            in_foc_difference = '$in_foc_difference',
                            in_foc ='$foc',
                            in_foc_reason = '$foc_reason',
                            in_foc_approval = '$foc_approval',
                            in_foc_note = '$foc_note',
                            in_standard_parking_amount = '$in_standard_parking_amount',
                            in_membership_payment_amount = '$in_membership_payment_amount',
                            total_time=0,
                            penalty_charge=0,
                            overnight_charges=0,
                            out_tariff_code=0,
                            out_day_type= '$in_day_type',
                            out_type='$in_type',
                            out_type_id='$in_type_id'
                            WHERE id= '$sps_transaction_id'";
                        //echo 'update_transactions=>'.$update_transactions;exit;                        
                        mysqli_query($conn, $update_transactions);
                        $logString .= $update_transactions . "\n" . PHP_EOL;
                    }
                    if ($_FILES) {
                        /*---- creating image name using type of image, timestamp and transaction id ----*/
                        $imageTimestampName = date("YmdHis");
                        $fullImageName1 = $imagePos1 . "_" . $imageTimestampName . "_" . $sps_transaction_id;
                        $fullImageName2 = $imagePos2 . "_" . $imageTimestampName . "_" . $sps_transaction_id;
                        $imageFileType1 = '';
                        $imageFileType2 = '';
                        $dir = "../storage/images/transactions";
                        if (!file_exists($dir)) {
                            $oldmask = umask(0);  // helpful when used in linux server
                            /*---- using mkdir to create a directory if not exists and giving permission ----*/
                            mkdir($dir, 0777);

                            /*---- image path ----*/
                            $imageFileType1 = pathinfo($_FILES["fileToUpload1"]["name"], PATHINFO_EXTENSION);
                            $imageFileType2 = pathinfo($_FILES["fileToUpload2"]["name"], PATHINFO_EXTENSION);
                            $file1 = "../storage/images/transactions/$fullImageName1.$imageFileType1";
                            $file2 = "../storage/images/transactions/$fullImageName2.$imageFileType2";
                            /*---- writing image to a directory ----*/

                        } else {
                            /*---- if directory name already exists then write image directly to that directory ----*/
                            $imageFileType1 = pathinfo($_FILES["fileToUpload1"]["name"], PATHINFO_EXTENSION);
                            $imageFileType2 = pathinfo($_FILES["fileToUpload2"]["name"], PATHINFO_EXTENSION);
                            $file1 = "../storage/images/transactions/$fullImageName1.$imageFileType1";
                            $file2 = "../storage/images/transactions/$fullImageName2.$imageFileType2";
                        }
                        move_uploaded_file($_FILES["fileToUpload1"]["tmp_name"], $file1);
                        move_uploaded_file($_FILES["fileToUpload2"]["tmp_name"], $file2);

                        $queryEntrySpsImages = "insert into sps_images(module,module_id,file_name,created_at)values('Entry','$sps_transaction_id','$fullImageName1.$imageFileType1','$created_at'),('Entry','$sps_transaction_id','$fullImageName2.$imageFileType2','$created_at')";
                        mysqli_query($conn, $queryEntrySpsImages);
                        $logString .= $queryEntrySpsImages . "\n" . PHP_EOL;
                    }
                    //Added changes for balance update from mobile
                    if (isset($_REQUEST['in_type']) && isset($_REQUEST['in_type_id']) && $_REQUEST['in_type'] == 2 && strlen($_REQUEST['in_type_id']) == 16) {
                        $q = "SELECT sm.membership_id FROM sps_membership AS sm WHERE CONCAT( sm.card_no_prefix, sm.card_no ) =  '" . $_REQUEST['in_type_id'] . "' AND (sm.balance_updated_at IS NULL OR sm.balance_updated_at < '" . $in_time . "') AND sm.deleted_at IS NULL";
                        $logString .= $q . "\n" . PHP_EOL;
                        $result = mysqli_query($conn, $q);
                        $rowcount = mysqli_num_rows($result);
                        $card_no = $_REQUEST['in_type_id'];
                        if ($result) {
                            $membership = mysqli_fetch_assoc($result);
                            $update_balance = "UPDATE sps_membership SET balance_updated_at='" . $in_time . "',balance='" . $_REQUEST['remaining_balance'] . "' WHERE membership_id= '" . $membership['membership_id'] . "'";
                            mysqli_query($conn, $update_balance);
                            $logString .= $update_balance . "\n" . PHP_EOL;

                            //inserting balance update in sps_membership_bal_noti table
                            $queryInsertBalanceUpdate = "insert into sps_membership_bal_noti(transaction_id,cps_id,card_no,transaction_at,is_notify)values($sps_transaction_id',NULL,'$card_no','ENTRY','1')";
                            mysqli_query($conn, $queryInsertBalanceUpdate);
                            $logString .= $queryInsertBalanceUpdate . "\n" . PHP_EOL;

                            $lastInsertId = mysqli_insert_id($conn);
                            balance_deduction($lastInsertId, $baseUrl);
                        }
                    }

                    //Update Entry Process Time
                    $update_process_time = "UPDATE sps_transactions SET entry_process_time='" . date("Y-m-d H:i:s") . "',location_id='" . $location_id . "',area_id='" . $area_id . "' WHERE id = $sps_transaction_id";
                    mysqli_query($conn, $update_process_time);
                    $logString .= $update_process_time . "\n" . PHP_EOL;
                    //Update Entry Process Time

                    mysqli_commit($conn);
                    $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();

                } else {
                    $response_data = json_encode(array('err_code' => '10016', 'err_msg' => mysqli_error($conn)));
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();
                }
            }
        } catch (Exception $e) {
            mysqli_rollback($conn);
            echo $e->getMessage();
        }
        createLog($logString);
        break;

    /*---- Case 0029 is related to RFID card at entry ----*/
    case 'rfid_validity':
        /*---- getting card no from client ----*/
        $card_no = trim($_REQUEST['card_no']);
        $date = date('Y-m-d');
        $link_vehicle_to_membership = trim($_REQUEST['link_vehicle_to_membership']);
        $vehicle_no = trim($_REQUEST['vehicle_no']);

        if ($card_no == '') {
            $response_data = json_encode(array('err_code' => '10021', 'err_msg' => $errors['10021']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }

        if ($vehicle_no == '') {
            $response_data = json_encode(array('err_code' => '10029', 'err_msg' => $errors['10029']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }

        $q = "SELECT sm.membership_id,CONCAT( sm.card_no_prefix, sm.card_no ) AS Card_no ,sm.period_from,sm.period_to,sm.extended_period_from,sm.extended_period_to,sm.status,smr.corporate_id,sc.status as corporate_status
                   FROM sps_membership AS sm
                   LEFT JOIN sps_members AS smr
                   ON sm.member_id = smr.member_id
                   LEFT JOIN sps_corporates AS sc 
                   ON smr.corporate_id=sc.corporate_id
                   WHERE CONCAT( sm.card_no_prefix, sm.card_no ) =  '" . $card_no . "' AND sm.deleted_at IS NULL";

        $logString .= $q . "\n" . PHP_EOL;
        $result = mysqli_query($conn, $q);
        $validity_membership = mysqli_fetch_assoc($result);

        if (!isset($validity_membership)) {
            $response_data = json_encode(array('err_code' => '10019', 'err_msg' => $errors['10019']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else if ($validity_membership['corporate_status'] == '0') {
            $response_data = json_encode(array('err_code' => '10037', 'err_msg' => $errors['10037']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            if ($validity_membership['status'] == '1') {
                $response = array();
                $q_receipt = "SELECT sml.membership_log_id
                   FROM sps_membership_log AS sml                   
                   WHERE sml.rfid_membership_id='" . $validity_membership['membership_id'] . "' AND sml.membership_action IN(1,2) AND sml.deleted_at IS NULL ORDER BY sml.membership_log_id DESC LIMIT 1";
                $logString .= $q_receipt . "\n" . PHP_EOL;
                $result_receipt = mysqli_query($conn, $q_receipt);
                if (mysqli_num_rows($result_receipt) > 0) {
                    $receipt_no = mysqli_fetch_assoc($result_receipt);
                    $response['member']['reciept'] = $receipt_no['membership_log_id'];
                }

                $dateBegin = date('Y-m-d', strtotime($validity_membership['period_from']));
                $dateEnd = date('Y-m-d', strtotime($validity_membership['period_to']));
                if (($date >= $dateBegin) && ($date <= $dateEnd)) {
                    if ($link_vehicle_to_membership == 'yes') {
                        $vehicle = "SELECT vehicle_no FROM sps_member_vehicles WHERE vehicle_no = '" . $vehicle_no . "' AND membership_id = '" . $validity_membership['membership_id'] . "' AND membership_type = 'RFID' AND deleted_at IS NULL";
                        $logString .= $vehicle . "\n" . PHP_EOL;
                        $result = mysqli_query($conn, $vehicle);
                        $vehicle_membership = mysqli_fetch_assoc($result);
                        if ($vehicle_membership['vehicle_no'] == $vehicle_no) {
                            $member_resv_left = "SELECT vip_resv_total,member_resv_total,vip_resv_left,member_resv_left FROM sps_corporates WHERE corporate_id = '" . $validity_membership['corporate_id'] . "'";
                            $member_resv_left_query = mysqli_query($conn, $member_resv_left);
                            $member_resv_left_result = mysqli_fetch_assoc($member_resv_left_query);
                            $response['member']['vip_resv_total'] = $member_resv_left_result['vip_resv_total'];
                            $response['member']['member_resv_total'] = $member_resv_left_result['member_resv_total'];
                            $response['member']['member_available_slots'] = $member_resv_left_result['member_resv_left'];
                            $response['member']['vip_available_slots'] = $member_resv_left_result['vip_resv_left'];
                            $response['member']['corporate_id'] = $validity_membership['corporate_id'];
                            $response['validity'] = 'true';
                            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                            echo $response_data;
                            $logString .= $response_data . "\n" . PHP_EOL;
                        } else {
                            $response_data = json_encode(array('err_code' => '10031', 'err_msg' => $errors['10031']));
                            echo $response_data;
                            $logString .= $response_data . "\n" . PHP_EOL;
                            createLog($logString);
                            exit;
                        }
                    } else {
                        $member_resv_left = "SELECT vip_resv_total,member_resv_total,vip_resv_left,member_resv_left FROM sps_corporates WHERE corporate_id = '" . $validity_membership['corporate_id'] . "'";
                        $member_resv_left_query = mysqli_query($conn, $member_resv_left);
                        $member_resv_left_result = mysqli_fetch_assoc($member_resv_left_query);
                        $response['member']['vip_resv_total'] = $member_resv_left_result['vip_resv_total'];
                        $response['member']['member_resv_total'] = $member_resv_left_result['member_resv_total'];
                        $response['member']['corporate_id'] = $validity_membership['corporate_id'];
                        $response['member']['member_available_slots'] = $member_resv_left_result['member_resv_left'];
                        $response['member']['vip_available_slots'] = $member_resv_left_result['vip_resv_left'];
                        $response['validity'] = 'true';
                        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                        echo $response_data;
                        $logString .= $response_data . "\n" . PHP_EOL;
                        createLog($logString);
                        exit();
                    }
                } else {
                    if ($validity_membership['extended_period_from'] != '' && $validity_membership['extended_period_to'] != '') {
                        $extendedDateBegin = date('Y-m-d', strtotime($validity_membership['extended_period_from']));
                        $extendedDateEnd = date('Y-m-d', strtotime($validity_membership['extended_period_to']));
                        if (($date >= $extendedDateBegin) && ($date <= $extendedDateEnd)) {
                            if ($link_vehicle_to_membership == 'yes') {
                                $vehicle = "SELECT vehicle_no FROM sps_member_vehicles WHERE vehicle_no = '" . $vehicle_no . "' AND membership_id = '" . $validity_membership['membership_id'] . "' AND membership_type = 'RFID' AND deleted_at IS NULL";
                                $logString .= $vehicle . "\n" . PHP_EOL;
                                $result = mysqli_query($conn, $vehicle);
                                $vehicle_membership = mysqli_fetch_assoc($result);
                                if ($vehicle_membership['vehicle_no'] == $vehicle_no) {
                                    $member_resv_left = "SELECT vip_resv_total,member_resv_total,vip_resv_left,member_resv_left FROM sps_corporates WHERE corporate_id = '" . $validity_membership['corporate_id'] . "'";
                                    $logString .= $member_resv_left . "\n" . PHP_EOL;
                                    $member_resv_left_query = mysqli_query($conn, $member_resv_left);
                                    $member_resv_left_result = mysqli_fetch_assoc($member_resv_left_query);
                                    $response['member']['vip_resv_total'] = $member_resv_left_result['vip_resv_total'];
                                    $response['member']['member_resv_total'] = $member_resv_left_result['member_resv_total'];
                                    $response['member']['corporate_id'] = $validity_membership['corporate_id'];
                                    $response['member']['member_available_slots'] = $member_resv_left_result['member_resv_left'];
                                    $response['member']['vip_available_slots'] = $member_resv_left_result['vip_resv_left'];
                                    $response['validity'] = true;
                                    $response['extended'] = true;
                                    $response['extended_period_from'] = $validity_membership['extended_period_from'];
                                    $response['extended_period_to'] = $validity_membership['extended_period_to'];
                                    $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                                    echo $response_data;
                                    $logString .= $response_data . "\n" . PHP_EOL;
                                    createLog($logString);
                                    exit();
                                } else {
                                    $response_data = json_encode(array('err_code' => '10031', 'err_msg' => $errors['10031']));
                                    echo $response_data;
                                    $logString .= $response_data . "\n" . PHP_EOL;
                                    createLog($logString);
                                    exit;
                                }
                            } else {
                                $member_resv_left = "SELECT vip_resv_total,member_resv_total,vip_resv_left,member_resv_left FROM sps_corporates WHERE corporate_id = '" . $validity_membership['corporate_id'] . "'";
                                $member_resv_left_query = mysqli_query($conn, $member_resv_left);
                                $member_resv_left_result = mysqli_fetch_assoc($member_resv_left_query);
                                $response['member']['vip_resv_total'] = $member_resv_left_result['vip_resv_total'];
                                $response['member']['member_resv_total'] = $member_resv_left_result['member_resv_total'];
                                $response['member']['corporate_id'] = $validity_membership['corporate_id'];
                                $response['member']['member_available_slots'] = $member_resv_left_result['member_resv_left'];
                                $response['member']['vip_available_slots'] = $member_resv_left_result['vip_resv_left'];
                                $response['validity'] = true;
                                $response['extended'] = true;
                                $response['extended_period_from'] = $validity_membership['extended_period_from'];
                                $response['extended_period_to'] = $validity_membership['extended_period_to'];
                                $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                                echo $response_data;
                                $logString .= $response_data . "\n" . PHP_EOL;
                                createLog($logString);
                                exit();
                            }
                        } else {
                            $response_data = json_encode(array('err_code' => '10032', 'err_msg' => $errors['10032']));
                            echo $response_data;
                            $logString .= $response_data . "\n" . PHP_EOL;
                            createLog($logString);
                            exit;
                        }
                    } else {

                        $response_data = json_encode(array('err_code' => '10032', 'err_msg' => $errors['10032']));
                        echo $response_data;
                        $logString .= $response_data . "\n" . PHP_EOL;
                        createLog($logString);
                        exit;
                    }

                }

            } else {
                $response_data = json_encode(array('err_code' => '10030', 'err_msg' => $errors['10030']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();

            }

        }
        createLog($logString);
        break;


    /*---- Case 0029 is related to RFID card at entry ----*/
    case 'vehicle_visits':
        /*---- vehicle count ----*/
        $vehicle_no = trim($_REQUEST['vehicle_no']);
        $vehicle_type = trim($_REQUEST['vehicle_type']);
        $day_type = trim($_REQUEST['day_type']);
//        $count = trim($_REQUEST['duration']);
        $gate_id = trim($_REQUEST['gate_id']);
        $type = trim($_REQUEST['type']);
//        $startDate = date("Y-m-d", strtotime("-$count days", time()));
//        $endDate = date('Y-m-d');                  
        $time = date('H:i:s');

        if (empty($vehicle_no)) {
            $response_data = json_encode(array('err_code' => '10020', 'err_msg' => $errors['10020']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        if (empty($vehicle_type)) {
            $response_data = json_encode(array('err_code' => '10003', 'err_msg' => $errors['10003']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }


        /* start code for casual discount */
        $vehicle_count['count'] = 0;
        $query = "SELECT * FROM sps_discounts WHERE
            day_type = '$day_type'
            AND vehicle_type = '" . $vehicle_type . "'
            AND ('" . date('Y-m-d') . "' >= start_date AND '" . date('Y-m-d') . "' <= end_date)
            AND ('" . $time . "' >= start_time AND '" . $time . "' <= end_time)
            AND FIND_IN_SET('" . $gate_id . "',gate) > 0 AND status=1          
            AND deleted_at is NULL";
        $logString .= $query . "\n" . PHP_EOL;
        $disc_data = mysqli_fetch_assoc(mysqli_query($conn, $query));
        if (!empty($disc_data)) {
            $disc_startDate = date("Y-m-d", strtotime("-" . ($disc_data['duration'] - 1) . "  days", time()));
            if (strtotime($disc_startDate) < strtotime($disc_data['start_date'])) {
                $disc_startDate = $disc_data['start_date'];
            }
            $disc_endDate = date('Y-m-d');

            $query = "SELECT t.vehicle_number, td.* FROM sps_transaction_discount AS td
                LEFT JOIN sps_transactions AS t ON
                t.id = td.transaction_id
                WHERE td.discount_type = 3
                AND t.vehicle_number = '" . $vehicle_no . "'
                AND t.vehicle_type = '" . $vehicle_type . "'
                ORDER BY td.discount_id
                LIMIT 1";

            $logString .= $query . "\n" . PHP_EOL;
            $last_disc_data = mysqli_fetch_assoc(mysqli_query($conn, $query));
            $last_disc_date = (explode(' ', $last_disc_data['created_at'])[0]);
            if ($last_disc_date > $disc_startDate) {
                $disc_startDate = $last_disc_date;
            }

            $query = "SELECT count(*) AS count FROM sps_transactions AS t
                WHERE t.vehicle_number = '" . $vehicle_no . "'
                AND t.vehicle_type = '" . $vehicle_type . "'
                AND t.in_time >= '" . $disc_startDate . "'
                AND t.out_time >= '" . $disc_endDate . "'";
            $logString .= $query . "\n" . PHP_EOL;
            $vehicle_result = mysqli_fetch_assoc(mysqli_query($conn, $query));
            $trips_count = $vehicle_result['count'] + 1;
            $visit_count = $disc_data['no_of_visits'] + 1;
            if (($trips_count > $disc_data['no_of_visits']) && ($trips_count % $visit_count == 0) && (($time >= $disc_data['start_time']) && ($time <= $disc_data['end_time']))) {
                $discount['discount_id'] = $disc_data['id'];
                $discount['discount_code'] = $disc_data['discount_code'];
                $discount['discount_type'] = $disc_data['discount_type'];
                $discount['value'] = (int)$disc_data['value'];
            }
        }
        /*End code for casual discount */

        /*Start Code for membership validation*/
        $query = "SELECT sv.members_vehicles_id, sv.member_id, sv.membership_id, sv.vehicle_priority, sv.membership_type,
                sm.employee_id, sm.member_name, sm.location_id, sm.area_id, sm.phone_number, sm.driving_license,
                ms.period_from, ms.period_to, tm.tariff_member_code,sm.corporate_id,tm.vehicle_type
                FROM sps_member_vehicles AS sv
                LEFT JOIN sps_members AS sm ON sm.member_id = sv.member_id
                LEFT JOIN sps_membership_sticker AS ms ON ms.sticker_membership_id = sv.membership_id
                LEFT JOIN sps_tariff_member AS tm ON tm.member_tariff_id = ms.membership_product
                WHERE sv.status = 1 AND sm.status = 1 AND sv.deleted_at IS NULL AND sm.deleted_at IS NULL AND ms.deleted_at IS NULL
                AND (ms.period_to >= CURDATE() OR ms.extended_period_to >= CURDATE())";
        // if (!empty($vehicle_type))
        //     $query .= " AND (tm.vehicle_type = '" . $vehicle_type . "')";

        if (!empty($vehicle_no))
            $query .= " AND (sv.vehicle_no = '" . $vehicle_no . "')";

        $logString .= $query . "\n" . PHP_EOL;
        //echo $query; die;
        $result = mysqli_query($conn, $query);
        $rowcount = mysqli_num_rows($result);
        //$vehicle = mysqli_fetch_assoc($result);
        //var_dump($rowcount); die;
        if ($rowcount) {
            $vehicle = mysqli_fetch_assoc($result);     //print_r($vehicle); die;

            $response['member']['employee_id'] = $vehicle['employee_id'];
            $response['member']['member_name'] = $vehicle['member_name'];
            $response['member']['location_id'] = $vehicle['location_id'];
            $response['member']['area_id'] = $vehicle['area_id'];
            $response['member']['phone_number'] = $vehicle['phone_number'];
            $response['member']['driving_license'] = $vehicle['driving_license'];
            $response['membership_id'] = $vehicle['membership_id'];
            $response['member']['period_from'] = $vehicle['period_from'];
            $response['member']['period_to'] = $vehicle['period_to'];
            $response['member']['corporate_id'] = $vehicle['corporate_id'];
            $response['member']['vehicle_type'] = $vehicle['vehicle_type'];

            if ($vehicle['membership_type'] == 'STICKER') {
                $response['type'] = 'STICKER';
                $response['tariff_member_code'] = $vehicle['tariff_member_code'];
                $response['priority'] = $vehicle['vehicle_priority'];
                $is_chargeable = 0;
                $member_resv_left = "SELECT member_resv_total,member_resv_left FROM sps_corporates WHERE corporate_id = '" . $vehicle['corporate_id'] . "'";
                $logString .= $member_resv_left . "\n" . PHP_EOL;
                $member_resv_left_query = mysqli_query($conn, $member_resv_left);
                $member_resv_left_result = mysqli_fetch_assoc($member_resv_left_query);
                $response['member']['member_resv_total'] = $member_resv_left_result['member_resv_total'];
                $response['member']['available_slots'] = $member_resv_left_result['member_resv_left'];

                if ($vehicle['vehicle_priority'] == '2') {
                    $qt = "SELECT sv.vehicle_no
                              FROM sps_member_vehicles AS sv
                              LEFT JOIN sps_members AS sm ON sm.member_id = sv.member_id
                              JOIN sps_transactions ON sv.vehicle_no=sps_transactions.vehicle_number
                              WHERE sv.status = 1 AND sv.deleted_at IS NULL AND sv.membership_id ='" . $vehicle['membership_id'] . "' AND sv.vehicle_priority=1 AND (out_time = '0000-00-00 00:00:00' OR DATE(out_time)='" . date('Y-m-d') . "') AND sps_transactions.deleted_at IS NULL";
                    $logString .= $qt . "\n" . PHP_EOL;
                    $result = mysqli_query($conn, $qt);
                    $vt = mysqli_num_rows($result);
                    if ($vt) {
                        $is_chargeable++;
                    } else {
                        $qt = "SELECT id FROM sps_transactions WHERE vehicle_number='" . $vehicle_no . "' AND deleted_at IS NULL AND DATE(out_time)='" . date('Y-m-d') . "'";
                        $logString .= $qt . "\n" . PHP_EOL;
                        $result = mysqli_query($conn, $qt);
                        $vt = mysqli_num_rows($result);
                        if ($vt) {
                            $is_chargeable++;
                        } else {
                            $query_v = "SELECT sv.vehicle_no
                                  FROM sps_member_vehicles AS sv
                                  LEFT JOIN sps_members AS sm ON sm.member_id = sv.member_id
                                  WHERE sv.status = 1 AND sv.deleted_at IS NULL AND sv.membership_id ='" . $vehicle['membership_id'] . "' AND sv.vehicle_no NOT IN('" . $vehicle_no . "')";
                            // print_r($query_v );
                            $logString .= $query_v . "\n" . PHP_EOL;
                            $result_v = mysqli_query($conn, $query_v);

                            foreach ($result_v as $v) {
                                $vehicles[] = $v['vehicle_no'];
                            }
                            $mv = implode("','", $vehicles);

                            $qt = "SELECT id FROM sps_transactions WHERE vehicle_number IN ('" . $mv . "') AND deleted_at IS NULL AND DATE(out_time)='" . date('Y-m-d') . "'";
                            $logString .= $qt . "\n" . PHP_EOL;
                            $result = mysqli_query($conn, $qt);
                            $vt = mysqli_num_rows($result);
                            if ($vt) {
                                $is_chargeable++;
                            } else {
                                $qt = "SELECT id FROM sps_transactions WHERE vehicle_number IN ('" . $mv . "') AND deleted_at IS NULL AND out_time = '0000-00-00 00:00:00'";
                                $logString .= $qt . "\n" . PHP_EOL;
                                $result = mysqli_query($conn, $qt);
                                $vt = mysqli_num_rows($result);
                                if ($type == 'ENTRY' && $vt) {
                                    $is_chargeable++;
                                } else {
                                    $qt = "SELECT cps_id FROM sps_cps WHERE cps_vehicle_number IN ('" . $mv . "') AND deleted_at IS NULL AND DATE(cps_out_time) = '" . date('Y-m-d') . "'";
                                    $logString .= $qt . "\n" . PHP_EOL;
                                    $result = mysqli_query($conn, $qt);
                                    $vt = mysqli_num_rows($result);
                                    if ($vt) {
                                        $is_chargeable++;
                                    }
                                }
                            }
                        }
                    }
                }

                $response['is_chargeable'] = $is_chargeable;
            } else if ($vehicle['membership_type'] == 'RFID') {
                $response['type'] = 'RFID';
                $response['priority'] = '1';
            }
            $response_data = json_encode(array('err_code' => '0', 'data' => array('count' => $vehicle_count['count'], 'membership' => $response)));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            $response_data = json_encode(array('err_code' => '0', 'data' => array('count' => $vehicle_count['count'], 'discount' => $discount)));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        /*End Code for membership validation*/
        createLog($logString);
        break;

    case "transaction_exit":

        /* US793: Dynamic QR Code – Mobile Gate 
           Allowing to set payment reference number in case of master payment mode E wallet
        */
        if ($_REQUEST['out_master_payment_mode'] == 4 && ($_REQUEST['in_payment_mode'] == 5 || $_REQUEST['in_payment_mode'] == 7) && !empty($_REQUEST['out_payment_reference_number'])) {
            $out_payment_reference_number = trim($_REQUEST['out_payment_reference_number']);
        } else {
            $_REQUEST['out_payment_reference_number'] = 0;
            $out_payment_reference_number = 0;
        }
        if (!empty($out_payment_reference_number)) {
            isValidPaymentReferenceNumber($conn, $errors, $logString, $out_payment_reference_number);
        }
        /* US793: Dynamic QR Code – Mobile Gate */

        // 41048 EOD is not generating at Promenade mall: start
        if (!isset($_REQUEST['vehicle_type']) || empty($_REQUEST['vehicle_type'])) {
            $response_data = json_encode(array('err_code' => '10003', 'err_msg' => $errors['10003']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            $queryVehicleTypes = "SELECT `id` FROM `sps_vehicle_types` WHERE `deleted_at` IS NULL";
            $resultVehicleTypes = mysqli_query($conn, $queryVehicleTypes);
            $logString .= $queryVehicleTypes . "\n" . PHP_EOL;

            while ($row = mysqli_fetch_assoc($resultVehicleTypes)) {
                $vehicleTypeIds[] = $row['id'];
            }

            if (!in_array($_REQUEST['vehicle_type'], $vehicleTypeIds)) {
                $response_data = json_encode(array('err_code' => '10040', 'err_msg' => $errors['10040']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
        }
        // 41048 EOD is not generating at Promenade mall: end

        $ticket_no = trim($_REQUEST['ticket_no']);
        $transactionId = $_REQUEST['transaction_id'];
        $out_time = $_REQUEST['out_time'];

        // Get total time from sps_cps table for transaction_id 
        $query_getSum = "SELECT SUM(cps_total_time) as cps_total_time FROM `sps_cps` WHERE transaction_id='" . $_REQUEST['transaction_id'] . "'";
        $logString .= $query_getSum . "\n" . PHP_EOL;
        $getSum = mysqli_fetch_assoc(mysqli_query($conn, $query_getSum));
        $cps_total_time = $getSum['cps_total_time'];

        // Added cps_total_time to $_REQUEST['total_time']
        $total_time = $_REQUEST['total_time'] + $cps_total_time;

        $outgate = $_REQUEST['out_gate'];
        $out_userid = $_REQUEST['out_user_id'];
        $payment_amount = $_REQUEST['out_payment_amount'];
        $out_payment_mode = $_REQUEST['out_payment_mode'];
        $out_master_payment_mode = $_REQUEST['out_master_payment_mode'];
        $out_shift_id = $_REQUEST['out_shift_id'];
        $out_type = trim($_REQUEST['out_type']);
        $out_type_id = trim($_REQUEST['out_type_id']);
        $imagePos1 = trim($_REQUEST['image1_pos']);
        $imagePos2 = trim($_REQUEST['image2_pos']);
        $total_payment_amount = trim($_REQUEST['total_payment_amount']);
        $out_standard_parking_amount = trim($_REQUEST['out_standard_parking_amount']);
        $out_membership_payment_amount = trim($_REQUEST['out_membership_payment_amount']);
        $outDayType = $_REQUEST['out_day_type'];
        $penalty_charge = trim($_REQUEST['penalty_charge']);
        $overnight_charges = trim($_REQUEST['overnight_charges']);
        $lost_ticket_penalty = trim($_REQUEST['lost_ticket_penalty']);
        $manuel_ticket = empty($_REQUEST['manuel_ticket']) ? NULL : $_REQUEST['manuel_ticket'];
        $f_eight = empty($_REQUEST['f_eight']) ? NULL : $_REQUEST['f_eight'];
        $f_eight_reason = empty($_REQUEST['f_eight_reason']) ? NULL : $_REQUEST['f_eight_reason'];
        $f_eight_notes = empty($_REQUEST['f_eight_notes']) ? NULL : $_REQUEST['f_eight_notes'];
        $foc = empty($_REQUEST['out_foc']) ? NULL : $_REQUEST['out_foc'];
        $foc_difference = empty($_REQUEST['out_foc_difference']) ? NULL : $_REQUEST['out_foc_difference'];
        $foc_reason = empty($_REQUEST['out_foc_reason']) ? NULL : $_REQUEST['out_foc_reason'];
        $foc_approval = empty($_REQUEST['out_foc_approval']) ? NULL : $_REQUEST['out_foc_approval'];
        $foc_note = empty($_REQUEST['out_foc_note']) ? NULL : $_REQUEST['out_foc_note'];
        $lost_ticket = empty($_REQUEST['lost_ticket']) ? NULL : $_REQUEST['lost_ticket'];
        //$offline_ticket = empty($_REQUEST['offline_ticket']) ? NULL : $_REQUEST['offline_ticket'];
        $out_tariff_code = empty($_REQUEST['out_tariff_code']) ? NULL : $_REQUEST['out_tariff_code'];
        $in_type = trim($_REQUEST['in_type']);
        $in_type_id = is_null($_REQUEST['in_type_id']) ? NULL : trim($_REQUEST['in_type_id']);
        $location_id = trim($_REQUEST['location_id']);;
        $area_id = trim($_REQUEST['area_id']);

        /*Start : Sonu Code */
        date_default_timezone_set("Asia/Calcutta");   //India time (GMT+5:30)
        $dbDateTime = date('Y-m-d H:i:s');
        $custName = trim($_REQUEST['custName']);
        $custMobile = trim($_REQUEST['custMobile']);
        $custEmail = trim($_REQUEST['custEmail']);
        $vehicleNumber = trim($_REQUEST['vehicle_number']);

        if (!empty($custName) && !empty($custMobile) && !empty($custEmail)) {
            $queryFetchVehicleData = "SELECT name,mobile,email FROM sps_user_vehicle_contact_details WHERE name = '$custName' and mobile = '$custMobile' and email = '$custEmail' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', '$custMobile', '$location_id', '$area_id', '0', NULL, '$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        } else if (!empty($custName) && !empty($custMobile)) {
            $queryFetchVehicleData = "SELECT name,mobile FROM sps_user_vehicle_contact_details WHERE name = '$custName' and mobile = '$custMobile' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', '$custMobile', '$location_id', '$area_id', '0', NULL, '$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        } else if (!empty($custName) && !empty($custEmail)) {
            $queryFetchVehicleData = "SELECT name,mobile FROM sps_user_vehicle_contact_details WHERE name = '$custName' and email = '$custEmail' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', NULL, '$location_id', '$area_id', '0', NULL, '$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        } else if (!empty($custMobile) && !empty($custEmail)) {
            $queryFetchVehicleData = "SELECT name,mobile FROM sps_user_vehicle_contact_details WHERE mobile = '$custMobile' and email = '$custEmail' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', '$custMobile', '$location_id', '$area_id', '0', NULL, '$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        } else if (!empty($custName)) {
            $queryFetchVehicleData = "SELECT name,mobile FROM sps_user_vehicle_contact_details WHERE name = '$custName' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', NULL, '$location_id', '$area_id', '0', NULL,'$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        } else if (!empty($custMobile)) {
            $queryFetchVehicleData = "SELECT name,mobile FROM sps_user_vehicle_contact_details WHERE mobile = '$custMobile' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', '$mobile', '$location_id', '$area_id', '0', NULL, '$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        } else if (!empty($custEmail)) {
            $queryFetchVehicleData = "SELECT name,mobile FROM sps_user_vehicle_contact_details WHERE email = '$custEmail' and vehicle_number = '$vehicleNumber' LIMIT 1";
            $result = mysqli_query($conn, $queryFetchVehicleData);
            $rowcount = mysqli_num_rows($result);
            if ($rowcount == 0) {
                $queryInsertCustomerDetails = "INSERT INTO `sps_user_vehicle_contact_details` (`id`, `name`, `vehicle_number`, `email`, `mobile`, `location_id`, `area_id`, `superadmin`, `sync_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (NULL, '$custName', '$vehicleNumber', '$custEmail', NULL, '$location_id', '$area_id', '0', NULL, '$out_userid', NULL, '$dbDateTime', '$dbDateTime', NULL)";
                mysqli_query($conn, $queryInsertCustomerDetails);
            }
        }
        /*End : Sonu Code */

        if (empty($transactionId)) {
            $inData = array();
            $gateId = 0;
            if (isset($_REQUEST['entry_gate_name'])) {
                $query_getSearch = "SELECT gate_id FROM sps_gate_configurations WHERE gate_name = '" . $_REQUEST['entry_gate_name'] . "' AND status='1' AND deleted_at IS NULL";
                $logString .= $query_getSearch . "\n" . PHP_EOL;
                $getSearch = mysqli_fetch_assoc(mysqli_query($conn, $query_getSearch));
                $gateId = $getSearch['gate_id'];
            }
            $entry_time = NULL;
            if (isset($_REQUEST['entry_time'])) {
                $entry_time = $_REQUEST['entry_time'];
            }
            if (!empty($_REQUEST['barcode'])) {

                $barcode = $_REQUEST['barcode'];
                $query = "SELECT id FROM sps_transactions WHERE barcode = '$barcode' AND (deleted_at IS NOT NULL OR out_time != '0000-00-00 00:00:00')";
                $result = mysqli_query($conn, $query);
                $logString .= $query . "\n" . PHP_EOL;
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    $response_data = json_encode(array('err_code' => '10033', 'err_msg' => $errors['10033']));
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();
                }

                $query = "SELECT id,offline_ticket,in_gate,in_time FROM sps_transactions WHERE barcode = '$barcode' AND deleted_at IS NULL";
                $result = mysqli_query($conn, $query);
                $logString .= $query . "\n" . PHP_EOL;
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    $transactionId = $transaction['id'];
                    $offline_bit = NULL;
                    if ($transaction['offline_ticket'] == 9) {
                        $offline_bit = 10;
                    } elseif ($transaction['offline_ticket'] == 1) {
                        $offline_bit = 2;
                    } elseif ($transaction['offline_ticket'] == 13) {
                        $offline_bit = 14;
                    } elseif ($transaction['offline_ticket'] == 12) {
                        $offline_bit = 15;
                    } elseif (is_null($transaction['offline_ticket'])) {
                        $offline_bit = $_REQUEST['offline_ticket'];
                    }
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 3 && !empty($offline_bit)) {
                        $updatetransactions_lost = "UPDATE sps_transactions SET offline_ticket='$offline_bit' ";
                        if (empty($transaction['in_gate']) || $transaction['in_gate'] == 0) {
                            $updatetransactions_lost .= ",in_gate = $gateId";
                        }
                        if (is_null($transaction['in_time']) || $transaction['in_time'] == '0000-00-00 00:00:00') {
                            $updatetransactions_lost .= ",in_time = '" . $entry_time . "'";
                        }
                        $updatetransactions_lost .= " WHERE id= '$transactionId' AND deleted_at IS NULL";
                        mysqli_query($conn, $updatetransactions_lost);
                        $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
                    }


                } else {
                    //$inData = array('barcode' => $_REQUEST['barcode'], 'offline_ticket' => 3);                    
                    $inData = array('barcode' => $_REQUEST['barcode'], 'vehicle_type' => $_REQUEST['vehicle_type'], 'vehicle_number' => $_REQUEST['vehicle_number'], 'in_gate' => $gateId, 'in_time' => $entry_time, 'in_time' => $entry_time, 'in_type' => $in_type, 'in_type_id' => $in_type_id);
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 3) {
                        $inData['offline_ticket'] = 3;
                    }
                    $sps_transaction_query = insert($conn, 'sps_transactions', $inData);
                    if (mysqli_query($conn, $sps_transaction_query)) {
                        $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                        $transactionId = mysqli_insert_id($conn);;
                    }
                }
            } elseif (!empty($_REQUEST['out_type_id']) && !empty($_REQUEST['out_type']) && empty($_REQUEST['manuel_ticket']) && is_null($lost_ticket) && isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 3) {
                $out_type_id = trim($_REQUEST['out_type_id']);
                $out_type = trim($_REQUEST['out_type']);
                $query = "SELECT id,offline_ticket,in_time,in_gate FROM sps_transactions WHERE in_type = '$out_type' AND in_type_id = '$out_type_id' AND deleted_at IS NULL";
                $result = mysqli_query($conn, $query);
                $logString .= $query . "\n" . PHP_EOL;
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    $transactionId = $transaction['id'];
                    $offline_bit = NULL;
                    if ($transaction['offline_ticket'] == 9) {
                        $offline_bit = 10;
                    } elseif ($transaction['offline_ticket'] == 1) {
                        $offline_bit = 2;
                    } elseif ($transaction['offline_ticket'] == 13) {
                        $offline_bit = 14;
                    } elseif ($transaction['offline_ticket'] == 12) {
                        $offline_bit = 15;
                    } elseif (is_null($transaction['offline_ticket'])) {
                        $offline_bit = $_REQUEST['offline_ticket'];
                    }
                    if (!empty($offline_bit)) {

                        $updatetransactions_lost = "UPDATE sps_transactions SET offline_ticket='$offline_bit'  ";
                        if (empty($transaction['in_gate']) || $transaction['in_gate'] == 0) {
                            $updatetransactions_lost .= ",in_gate = $gateId";
                        }
                        if (is_null($transaction['in_time']) || $transaction['in_time'] == '0000-00-00 00:00:00') {
                            $updatetransactions_lost .= ",in_time = '" . $entry_time . "'";
                        }
                        $updatetransactions_lost .= " WHERE id= '$transactionId' AND deleted_at IS NULL";
                        mysqli_query($conn, $updatetransactions_lost);
                        $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
                    }
                } else {
                    //$inData = array('out_type' => $_REQUEST['out_type'], 'out_type_id' => $_REQUEST['out_type_id'], 'offline_ticket' => 3);

                    $inData = array('out_type' => $_REQUEST['out_type'], 'out_type_id' => $_REQUEST['out_type_id'], 'barcode' => $_REQUEST['barcode'], 'vehicle_type' => $_REQUEST['vehicle_type'], 'vehicle_number' => $_REQUEST['vehicle_number'], 'in_gate' => $gateId, 'in_time' => $entry_time, 'in_type' => $in_type, 'in_type_id' => $in_type_id);
                    if (isset($_REQUEST['offline_ticket']) && $_REQUEST['offline_ticket'] == 3) {
                        $inData['offline_ticket'] = 3;
                    }

                    $sps_transaction_query = insert($conn, 'sps_transactions', $inData);
                    if (mysqli_query($conn, $sps_transaction_query)) {
                        $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                        $transactionId = mysqli_insert_id($conn);
                    }
                }

            }
        }


        if (!empty($_REQUEST['manuel_ticket']) && !is_null($_REQUEST['manuel_ticket']) && $_REQUEST['manuel_ticket'] > 0) {
            $inData = array();
            $barcode = '';
            if (!empty($_REQUEST['barcode'])) {
                $inData['barcode'] = trim($_REQUEST['barcode']);
                $barcode = $inData['barcode'];
            }


            $inData['in_time'] = trim($_REQUEST['in_time']);
            $inData['vehicle_type'] = trim($_REQUEST['vehicle_type']);
            $inData['vehicle_number'] = trim($_REQUEST['vehicle_number']);
            $inData['in_type'] = trim($_REQUEST['in_type']);
            // $inData['in_type_id'] = trim($_REQUEST['in_type_id']);
            if (isset($_REQUEST['in_type_id']) && $_REQUEST['in_type_id'] != 'null') {
                $inData['in_type_id'] = trim($_REQUEST['in_type_id']);
            }
            //$inData['in_type_id'] = empty($_REQUEST['in_type_id']) ? NULL : $_REQUEST['in_type_id'];
            $inData['in_gate'] = trim($_REQUEST['in_gate']);
            $inData['in_user_id'] = trim($_REQUEST['in_user_id']);
            $inData['in_payment_mode'] = trim($_REQUEST['in_payment_mode']);
            $inData['in_master_payment_mode'] = trim($_REQUEST['in_master_payment_mode']);
            $inData['in_shift_id'] = trim($_REQUEST['in_shift_id']);
            // 35688 -- below two parameter added
            $inData['in_membership_payment_amount'] = empty($_REQUEST['in_membership_payment_amount']) ? NULL : $_REQUEST['in_membership_payment_amount'];
            $inData['out_membership_payment_amount'] = empty($_REQUEST['out_membership_payment_amount']) ? NULL : $_REQUEST['out_membership_payment_amount'];


            $query = "SELECT id FROM sps_transactions WHERE barcode = '$barcode' AND deleted_at IS NULL";
            $result = mysqli_query($conn, $query);
            $logString .= $query . "\n" . PHP_EOL;
            $transaction = mysqli_fetch_assoc($result);
            if (!is_null($transaction)) {
                $transactionId = $transaction['id'];
                $updatetransactions_lost = "UPDATE sps_transactions SET in_time ='" . $inData['in_time'] . "',vehicle_type ='" . $inData['vehicle_type'] . "',vehicle_number ='" . $inData['vehicle_number'] . "',in_type ='" . $inData['in_type'] . "',in_type_id =" . trim($_REQUEST['in_type_id']) . ",in_gate ='" . $inData['in_gate'] . "',in_user_id ='" . $inData['in_user_id'] . "',in_payment_mode ='" . $inData['in_payment_mode'] . "',in_master_payment_mode ='" . $inData['in_master_payment_mode'] . "' WHERE id= '$transactionId' AND deleted_at IS NULL"; //36301 - ,in_shift_id ='" . $inData['in_shift_id'] . "' removed from this update query
                mysqli_query($conn, $updatetransactions_lost);
                $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
            } else {
                $sps_transaction_query = insert($conn, 'sps_transactions', $inData);
                if (mysqli_query($conn, $sps_transaction_query)) {
                    $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                    $response['msg'] = 'New transaction created successfully!';
                    $response['transaction_id'] = mysqli_insert_id($conn);;
                    $transactionId = $response['transaction_id'];
                }

            }

            if (!empty($f_eight) && !is_null($f_eight) && $f_eight > 0) {
                $f_eight_used = "UPDATE sps_shift_management SET f_eight_used = f_eight_used - 1 WHERE sr_no= '" . $out_shift_id . "' AND deleted_at IS NULL";
                mysqli_query($conn, $f_eight_used);
            }
        }

        $discounts = null;
        if (!empty($_REQUEST['out_discount'])) {
            $discounts = json_decode($_REQUEST['out_discount'], true);
            unset($_REQUEST['out_discount']);

        }
        $additionalCharges = null;
        if (!empty($_REQUEST['out_additional_charges'])) {
            $additionalCharges = json_decode($_REQUEST['out_additional_charges'], true);
            unset($_REQUEST['out_additional_charges']);
        }
        $taxBreakup = null;
        if (!empty($_REQUEST['out_tax_breakup'])) {
            $taxBreakup = json_decode($_REQUEST['out_tax_breakup'], true);
            unset($_REQUEST['out_tax_breakup']);

        }


        /*----Query to fetch vehicle_number against vehicle number from sps_transactions ----*/
        /*----Added total_payment_amount in query to added in total_payment_amount fetch from Exit REQUEST------------------------*/
        $query = "SELECT id,offline_ticket,total_payment_amount FROM sps_transactions WHERE id = '$transactionId' AND out_gate IS NULL AND deleted_at IS NULL";
        $result = mysqli_query($conn, $query);
        $logString .= $query . "\n" . PHP_EOL;
        $transaction = mysqli_fetch_assoc($result);
        if (!is_null($transaction)) {
            $vehicle_type = trim($_REQUEST['vehicle_type']);
            $vehicle_number = trim($_REQUEST['vehicle_number']);
            /*-----Added total_payment_amount of entry gate-------------*/
            $total_payment_amount = $total_payment_amount + $transaction['total_payment_amount'];

            /*  if($f_eight > '0'){
                  $updatetransactions_lost = "UPDATE sps_transactions SET  vehicle_number = '$vehicle_number',vehicle_type = '$vehicle_type',out_time='$out_time',total_time = '$total_time', out_gate='$outgate',out_user_id='$outuserid',out_payment_amount='$payment_amount',out_payment_mode='$out_payment_mode',out_master_payment_mode='$out_master_payment_mode',out_shift_id = '$out_shift_id',out_foc = '$foc',out_foc_reason = '$foc_reason',out_foc_difference = '$foc_difference',out_foc_approval = '$foc_approval',out_foc_note = '$foc_note',penalty_charge = '$penalty_charge',overnight_charges = '$overnight_charges',lost_ticket = '$lost_ticket',lost_ticket_penalty = '$lost_ticket_penalty',manuel_ticket = '$manuel_ticket',out_type = '$out_type',out_standard_parking_amount = '$out_standard_parking_amount'  WHERE id= '$transactionId' AND deleted_at IS NULL";

              }else{
                  $updatetransactions_lost = "UPDATE sps_transactions SET  vehicle_number = '$vehicle_number',vehicle_type = '$vehicle_type',out_time='$out_time',total_time = '$total_time', out_gate='$outgate',out_user_id='$outuserid',out_payment_amount='$payment_amount',out_payment_mode='$out_payment_mode',out_master_payment_mode='$out_master_payment_mode',out_shift_id = '$out_shift_id',out_foc = '$foc',out_foc_reason = '$foc_reason',out_foc_difference = '$foc_difference',out_foc_approval = '$foc_approval',out_foc_note = '$foc_note',penalty_charge = '$penalty_charge',overnight_charges = '$overnight_charges',lost_ticket = '$lost_ticket',lost_ticket_penalty = '$lost_ticket_penalty',manuel_ticket = '$manuel_ticket',f_eight = NULL, f_eight_reason = '$f_eight_reason',f_eight_notes = '$f_eight_notes',out_type = '$out_type',out_standard_parking_amount = '$out_standard_parking_amount'  WHERE id= '$transactionId' AND deleted_at IS NULL";

            }*/
            // DE609 - in below query ticket number and out_type_id is added.
            //out_membership_payment_amount and total_payment_amount added in below query
            $updatetransactions_lost = "UPDATE sps_transactions SET ticket_no = '$ticket_no', lost_ticket_penalty = '$lost_ticket_penalty',vehicle_type = '$vehicle_type',out_time='$out_time',total_time = '$total_time', out_gate='$outgate',out_user_id='$out_userid',out_payment_amount='$payment_amount',out_payment_mode='$out_payment_mode',out_master_payment_mode='$out_master_payment_mode',out_shift_id = '$out_shift_id',penalty_charge = '$penalty_charge',overnight_charges = '$overnight_charges',out_type = '$out_type',out_type_id = '$out_type_id',out_standard_parking_amount = '$out_standard_parking_amount', out_day_type= '$outDayType',out_membership_payment_amount='$out_membership_payment_amount',total_payment_amount='$total_payment_amount',";
            if (!is_null($foc))
                $updatetransactions_lost .= " out_foc = '$foc',";
            if (!is_null($foc_difference))
                $updatetransactions_lost .= " out_foc_difference = '$foc_difference',";
            if (!is_null($foc_reason))
                $updatetransactions_lost .= " out_foc_reason = '$foc_reason',";
            if (!is_null($foc_approval))
                $updatetransactions_lost .= " out_foc_approval = '$foc_approval',";
            if (!is_null($foc_note))
                $updatetransactions_lost .= " out_foc_note = '$foc_note',";
            if (!is_null($lost_ticket))
                $updatetransactions_lost .= " lost_ticket = '$lost_ticket',";
            if (!is_null($manuel_ticket))
                $updatetransactions_lost .= " manuel_ticket = '$manuel_ticket',";
            if (!is_null($f_eight))
                $updatetransactions_lost .= " f_eight = '$f_eight',";
            if (!is_null($f_eight_reason))
                $updatetransactions_lost .= " f_eight_reason = '$f_eight_reason',";
            if (!is_null($f_eight_notes))
                $updatetransactions_lost .= " f_eight_notes = '$f_eight_notes',";
            /*if (!is_null($offline_ticket) && $offline_ticket != 3)
                $updatetransactions_lost .= " offline_ticket = '$offline_ticket',";*/
            if (!is_null($out_tariff_code))
                $updatetransactions_lost .= " out_tariff_code = '$out_tariff_code',";

            /* US793: Dynamic QR Code – Mobile Gate */
            if (isset($out_payment_reference_number)) {
                $updatetransactions_lost .= " out_payment_reference_number = '$out_payment_reference_number',";
            }
            /* US793: Dynamic QR Code – Mobile Gate */

            $updatetransactions_lost .= " vehicle_number = '$vehicle_number' WHERE id= '$transactionId' AND deleted_at IS NULL";
            //testComment vijay2991
            if (mysqli_query($conn, $updatetransactions_lost)) {
                $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
                $response['msg'] = 'Transaction updated successfully!';
                $response['transaction_id'] = $transaction['id'];
                $sps_transaction_id = $transaction['id'];
                $md5_of_transaction_id = md5($sps_transaction_id);

                $membership = array("2", "5");
                $reservTotalQuery = "SELECT member_resv_total,vip_resv_total,member_resv_left,vip_resv_left FROM sps_corporates WHERE corporate_id  ='$corporate_id'";
                $reservTotalQueryResult = mysqli_query($conn, $reservTotalQuery);
                $logString .= $reservTotalQuery . "\n" . PHP_EOL;
                $reservTotalQueryData = mysqli_fetch_array($reservTotalQueryResult, MYSQLI_NUM);
                if ($reservTotalQueryData['member_resv_total'] > 0 || $reservTotalQueryData['vip_resv_total'] > 0) {
                    if ($_REQUEST['vip_membership_slot'] == 1 && in_array($_REQUEST['out_type'], $membership)) {
                        $corporate_id = $_REQUEST['corporate_id'];
                        $query_sps_corporates_set_vip_resv_left = "UPDATE sps_corporates SET vip_resv_left = vip_resv_left - 1 WHERE vip_resv_left > 0 AND corporate_id  ='$corporate_id'";
                        mysqli_query($conn, $query_sps_corporates_set_vip_resv_left);
                        $logString .= $query_sps_corporates_set_vip_resv_left . "\n" . PHP_EOL;

                    } elseif ($_REQUEST['vip_membership_slot'] == 0 && in_array($_REQUEST['out_type'], $membership)) {
                        $corporate_id = $_REQUEST['corporate_id'];
                        $query_sps_corporates_set_member_resv_left = "UPDATE sps_corporates SET member_resv_left = member_resv_left - 1 WHERE member_resv_left > 0 AND corporate_id ='$corporate_id'";
                        mysqli_query($conn, $query_sps_corporates_set_member_resv_left);
                        $logString .= $query_sps_corporates_set_member_resv_left . "\n" . PHP_EOL;
                    }
                }
                if (!is_null($additionalCharges)) {
                    foreach ($additionalCharges as $additionalCharge) {
                        $additionalData = array();
                        $additionalData['transaction_id'] = $sps_transaction_id;
                        $additionalData['additional_tariff_id'] = $additionalCharge['additional_tariff_id'];
                        $additionalData['additional_tariff_amount'] = $additionalCharge['additional_tariff_amount'];
                        $additionalData['additional_charges_at'] = $additionalCharge['additional_charges_at'];
                        $additionalData['additional_value_type'] = $additionalCharge['additional_value_type'];
                        $additionalData['gate_id'] = $additionalCharge['gate_id'];
                        $transaction_additional_charges = insert($conn, 'sps_transaction_additional_charges', $additionalData);
                        mysqli_query($conn, $transaction_additional_charges);
                        $logString .= $transaction_additional_charges . "\n" . PHP_EOL;
                    }
                }
                if (!is_null($taxBreakup)) {
                    foreach ($taxBreakup as $tax) {
                        $taxData = array();
                        // $taxData['transaction_id'] = $sps_transaction_id;
                        $taxData['tax_id'] = $tax['tax_id'];
                        $taxData['tax_amount'] = $tax['tax_value'];
                        $taxData['module'] = $tax['module'];
                        $taxData['module_id'] = $sps_transaction_id;
                        $taxData['location_id'] = $location_id;
                        $taxData['area_id'] = $area_id;
                        $transaction_tac_breakup = insert($conn, 'sps_tax_breakup', $taxData);
                        mysqli_query($conn, $transaction_tac_breakup);
                        $logString .= $transaction_tac_breakup . "\n" . PHP_EOL;
                    }
                }

                if (!is_null($discounts)) {
                    foreach ($discounts as $discount) {
                        $discountData = array();
                        $discountData['transaction_id'] = $sps_transaction_id;
                        //'discount_type' => array('1'=>'Registered Vouchers','2'=>'Unregistered Vouchers','3'=>'Casual Discount'),
                        $discountData['discount_type'] = $discount['discount_type'];
                        //Registered Vouchers = voucher code, Unregistered Vouchers = category id, Casual Discount = 0;
                        $discountData['discount_type_id'] = $discount['discount_type_id'];
                        //'1'=>'Percentage', '2'=>'Random Value', '3'=>'Full Value'
                        $discountData['discount_value_type'] = $discount['discount_value_type'];
                        $discountData['discount_value'] = $discount['discount_value'];
                        $discountData['discount_amount'] = $discount['discount_amount'];
                        $discountData['gate_id'] = $discount['gate_id'];
                        $discountData['discount_at'] = $discount['discount_at'];
                        $sps_transaction_discount = insert($conn, 'sps_transaction_discount', $discountData);
                        mysqli_query($conn, $sps_transaction_discount);
                        $logString .= $sps_transaction_discount . "\n" . PHP_EOL;
                        $discount_type_id = $discount['discount_type_id'];
                        if ($discountData['discount_type'] == 1 || $discountData['discount_type'] == 2) {
                            $increaseCountVoucherEntry = "update sps_vouchers set used_count = used_count + 1 where status='1' AND deleted_at IS NULL AND id='$discount_type_id'";
                            mysqli_query($conn, $increaseCountVoucherEntry);
                            $logString .= $increaseCountVoucherEntry . "\n" . PHP_EOL;
                        }
                    }
                }
                if ($_FILES) {
                    /*---- creating image name using type of image, timestamp and transaction id ----*/
                    $imageTimestampName = date("YmdHis", strtotime("+30 seconds"));
                    $dir = "../storage/images/transactions";

                    if (isset($_FILES['fileToUpload1']) || isset($_FILES['fileToUpload2'])) {

                        $fullImageName1 = $imagePos1 . "_" . $imageTimestampName . "_" . $sps_transaction_id;
                        $fullImageName2 = $imagePos2 . "_" . $imageTimestampName . "_" . $sps_transaction_id;
                        $imageFileType1 = '';
                        $imageFileType2 = '';

                        if (!file_exists($dir)) {
                            $oldmask = umask(0);
                            /*---- using mkdir to create a directory if not exists and giving permission ----*/
                            mkdir($dir, 0777);
                            /*---- image path ----*/
                            $imageFileType1 = pathinfo($_FILES["fileToUpload1"]["name"], PATHINFO_EXTENSION);
                            $imageFileType2 = pathinfo($_FILES["fileToUpload2"]["name"], PATHINFO_EXTENSION);
                            $file1 = "../storage/images/transactions/$fullImageName1.$imageFileType1";
                            $file2 = "../storage/images/transactions/$fullImageName2.$imageFileType2";
                            /*---- writing image to a directory ----*/
                        } else {
                            /*---- if directory name already exists then write image directly to that directory ----*/
                            $imageFileType1 = pathinfo($_FILES["fileToUpload1"]["name"], PATHINFO_EXTENSION);
                            $imageFileType2 = pathinfo($_FILES["fileToUpload2"]["name"], PATHINFO_EXTENSION);
                            $file1 = "../storage/images/transactions/$fullImageName1.$imageFileType1";
                            $file2 = "../storage/images/transactions/$fullImageName2.$imageFileType2";
                        }
                        if (isset($_FILES['fileToUpload1'])) {
                            move_uploaded_file($_FILES["fileToUpload1"]["tmp_name"], $file1);
                            $queryEntrySpsImages = "insert into sps_images(module,module_id,file_name,created_at)values('Exit','$sps_transaction_id','$fullImageName1.$imageFileType1','$created_at')";
                            mysqli_query($conn, $queryEntrySpsImages);
                        }
                        if (isset($_FILES['fileToUpload2'])) {
                            move_uploaded_file($_FILES["fileToUpload2"]["tmp_name"], $file2);
                            $queryEntrySpsImages = "insert into sps_images(module,module_id,file_name,created_at)values('Exit','$sps_transaction_id','$fullImageName2.$imageFileType2','$created_at')";
                            mysqli_query($conn, $queryEntrySpsImages);
                        }
                    }

                    $insertReceiptImage = '';
                    if (isset($_FILES['fileToUpload3'])) {
                        $imageTimestampName = date("YmdHis");
                        $imageFileType3 = pathinfo($_FILES["fileToUpload3"]["name"], PATHINFO_EXTENSION);

                        if (!file_exists($dir)) {
                            $oldmask = umask(0);
                            mkdir($dir, 0777);
                        }

                        $sqlGetDriverImage = "SELECT file_name FROM sps_images WHERE module_id ='" . $sps_transaction_id . "' AND module='Entry' AND file_name LIKE 'enf_%'";
                        $sqlGetDriverImageResult = mysqli_query($conn, $sqlGetDriverImage);
                        $sqlGetDriverImageData = mysqli_fetch_assoc($sqlGetDriverImageResult);
                        if ($sqlGetDriverImageData) {
                            $fullImageName3 = $sqlGetDriverImageData['file_name'];
                            $file3 = "../storage/images/transactions/$fullImageName3";
                        } else {
                            $fullImageName3 = "enf_" . $imageTimestampName . "_" . $sps_transaction_id;
                            $file3 = "../storage/images/transactions/$fullImageName3.$imageFileType3";
                        }
                        move_uploaded_file($_FILES["fileToUpload3"]["tmp_name"], $file3);
                        if ($sqlGetDriverImageData['file_name'] == '') {
                            $queryEntrySpsImages = "insert into sps_images(module,module_id,file_name,created_at)values('Entry','$sps_transaction_id','$fullImageName3.$imageFileType3','$created_at')";
                            mysqli_query($conn, $queryEntrySpsImages);
                        }
                    }

                    $logString .= $queryEntrySpsImages . "\n" . PHP_EOL;
                }

                //Added changes for balance update from mobile
                if (isset($_REQUEST['out_type']) && isset($_REQUEST['out_type_id']) && $_REQUEST['out_type'] == 2 && strlen($_REQUEST['out_type_id']) == 16) {
                    $q = "SELECT sm.membership_id FROM sps_membership AS sm WHERE CONCAT( sm.card_no_prefix, sm.card_no ) =  '" . $_REQUEST['out_type_id'] . "' AND (sm.balance_updated_at IS NULL OR sm.balance_updated_at < '" . $out_time . "') AND sm.deleted_at IS NULL";
                    $logString .= $q . "\n" . PHP_EOL;
                    $result = mysqli_query($conn, $q);
                    $rowcount = mysqli_num_rows($result);
                    $card_no = $_REQUEST['out_type_id'];
                    if ($result) {
                        $membership = mysqli_fetch_assoc($result);
                        $update_balance = "UPDATE sps_membership SET balance_updated_at='" . $out_time . "',balance='" . $_REQUEST['remaining_balance'] . "' WHERE membership_id= '" . $membership['membership_id'] . "'";
                        mysqli_query($conn, $update_balance);
                        $logString .= $update_balance . "\n" . PHP_EOL;

                        //inserting balance update in sps_membership_bal_noti table
                        $queryInsertBalanceUpdate = "insert into sps_membership_bal_noti(transaction_id,cps_id,card_no,transaction_at,is_notify)values('$sps_transaction_id',NULL,'$card_no','EXIT','1')";
                        mysqli_query($conn, $queryInsertBalanceUpdate);
                        $logString .= $queryInsertBalanceUpdate . "\n" . PHP_EOL;

                        $lastInsertId = mysqli_insert_id($conn);
                        balance_deduction($lastInsertId, $baseUrl);
                    }
                }

                //Update Exit Process Time
                $update_process_time = "UPDATE sps_transactions SET exit_process_time='" . date("Y-m-d H:i:s") . "',location_id='" . $location_id . "',area_id='" . $area_id . "' WHERE id= '$sps_transaction_id'";
                mysqli_query($conn, $update_process_time);
                $logString .= $update_process_time . "\n" . PHP_EOL;
                //Update Exit Process Time

                mysqli_commit($conn);
                $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            $response_data = json_encode(array('err_code' => '10017', 'err_msg' => $errors['10017']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;

    case 'audit_report':
        $auditor = check_login($conn, $errors);

        $shift_id = $_REQUEST['shift_id'];
        $gate_id = $_REQUEST['gate_id'];
        if (empty($shift_id)) {
            $response_data = json_encode(array('err_code' => '10025', 'err_msg' => $errors['10025']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        if (empty($gate_id)) {
            $response_data = json_encode(array('err_code' => '10026', 'err_msg' => $errors['10026']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }

        $query = "SELECT sgm.*, 
                sm.sr_no, sm.user_id ,sm.shift_start, sm.shift_end, sm.end_shift_status, sm.shift_login_status, sm.shift_activate_status, sm.deleted_at,
                gc.gate_name, gc.gate_type,gc.standard_nested, gc.is_full,
                u.user_name, u.user_role, u.status, u.login_status
                FROM sps_shift_gate_mapping AS sgm
                LEFT JOIN sps_shift_management AS sm ON
                sm.sr_no = sgm.shift_id 
                LEFT JOIN sps_gate_configurations AS gc ON
                gc.gate_id = sgm.gate_id 
                LEFT JOIN sps_users AS u ON
                u.user_id = sm.user_id
                WHERE sgm.shift_id = " . $shift_id . "
                AND gc.gate_id = " . $gate_id . "
                AND u.login_status = 1
                AND u.deleted_at IS NULL
                AND sm.shift_end IS NULL
                AND sm.deleted_at IS NULL";
        //echo $query;exit;
        $logString .= $query . "\n" . PHP_EOL;
        $querystatus = mysqli_query($conn, $query);
        $result = mysqli_fetch_assoc($querystatus);
        $response['auditor_name'] = $auditor['name'];
        if ($result > 0) {
            $in_trans_ids = '0';
            if (preg_match('/1/', $result['gate_type']) == 1) {
                $response['IN'] = array();
                $response['IN']['report_date_time'] = date("d-m-Y H:i:s");
                $response['IN']['shift_start'] = $result['shift_start'];
                $response['IN']['shift_end'] = $result['shift_end'];
                $response['IN']['user_name'] = $result['user_name'];

                $where = ' in_shift_id= ' . $shift_id . ' AND in_gate = ' . $gate_id . ' ';
                $q_trans_count = "SELECT t.id
                    FROM sps_transactions AS t
                    LEFT JOIN sps_payment_modes AS ipm ON
                    ipm.id = t.in_payment_mode
                    WHERE (" . $where . ")";
                $querystatus = get_result_array($conn, $q_trans_count);
                $trans_in_count = count($querystatus);
                foreach ($querystatus as $key => $val) {
                    $in_trans_ids = $in_trans_ids . ',' . $val['id'];
                }
                $response['IN']['total_trans'] = (int)count($querystatus);

                /* LOOP A count */
                $loopa_count_query = "SELECT loop_a AS loopa,loop_b AS loopb
                                FROM sps_barrier_loop_count
                                WHERE shift_id = '" . $shift_id . "'";
                $logString .= $loopa_count_query . "\n" . PHP_EOL;
                $loopa_count_query_status = mysqli_query($conn, $loopa_count_query);
                $loopa_count = mysqli_fetch_assoc($loopa_count_query_status);

                /* BG count */
                $bg_count_query = "SELECT barrier_open, barrier_close
                                    FROM sps_barrier_loop_count
                                    WHERE shift_id = '" . $shift_id . "'";
                $logString .= $bg_count_query . "\n" . PHP_EOL;
                $bg_count_query_status = mysqli_query($conn, $bg_count_query);
                $bg_count = mysqli_fetch_assoc($bg_count_query_status);
                $response['IN']['barrier_open'] = isset($bg_count['barrier_open']) ? (int)$bg_count['barrier_open'] : 0;
                $response['IN']['barrier_close'] = isset($bg_count['barrier_close']) ? (int)$bg_count['barrier_close'] : 0;
                $response['IN']['total_loop_count_a'] = isset($loopa_count['loopa']) ? (int)$loopa_count['loopa'] : 0;
                $response['IN']['total_loop_count_b'] = isset($loopa_count['loopb']) ? (int)$loopa_count['loopb'] : 0;
                //$response['IN']['total_bg_count'] = 0;

                /* IN Payment Total */
                $in_payaments = array();
                if (strpos($result['gate_type'], '1') !== false) {
                    $in_payment_qry = "SELECT spm.id, spm.payment_mode, sum(st.in_payment_amount) AS in_pay
                            FROM sps_payment_modes AS spm
                            LEFT JOIN sps_transactions AS st ON spm.id = st.in_payment_mode AND st.id in ($in_trans_ids)
                            GROUP BY spm.id";
                    $in_payaments = get_result_array($conn, $in_payment_qry);
                }

                /* collection payments */
                if (!empty($in_payaments)) {
                    foreach ($in_payaments as $in_pay) {
                        $collection_pay[$in_pay['payment_mode']] = $in_pay['in_pay'] ? (int)$in_pay['in_pay'] : 0;
                    }
                }
                $response['IN']['collection'] = $collection_pay;

                /* IN F8 count*/
                $query_f8 = "SELECT count(st.f_eight) as f8
                        FROM sps_transactions AS st
                        LEFT JOIN sps_payment_modes AS ipm ON ipm.id = st.in_payment_mode
                        WHERE st.id in ($in_trans_ids)
                        AND st.f_eight=1 aND st.f_eight is not null AND st.in_foc IS NULL";
                $logString .= $query_f8 . "\n" . PHP_EOL;
                $res_query_f8 = mysqli_query($conn, $query_f8);
                $count_f8 = mysqli_fetch_assoc($res_query_f8);
                $response['IN']['transaction_count']['f8'] = (int)$count_f8['f8'];

                /* IN F7 count*/
                $query_f7 = "SELECT count(st.lost_ticket) as f7
                        FROM sps_transactions AS st
                        LEFT JOIN sps_payment_modes AS opm ON opm.id = st.out_payment_mode
                        where st.id in ($in_trans_ids)
                        AND st.lost_ticket = 1";
                $logString .= $query_f7 . "\n" . PHP_EOL;
                $res_query_f7 = mysqli_query($conn, $query_f7);
                $count_f7 = mysqli_fetch_assoc($res_query_f7);
                $response['IN']['transaction_count']['f7'] = (int)$count_f7['f7'];

                /* IN FOC count */
                $in_foc_count['in_foc'] = 0;
                $in_foc_count_query = "SELECT count(t.in_foc) AS in_foc
                                FROM sps_transactions AS t
                                WHERE t.id in ($in_trans_ids)  AND t.in_foc = 1";
                $logString .= $in_foc_count_query . "\n" . PHP_EOL;
                $in_foc_count_query_status = mysqli_query($conn, $in_foc_count_query);
                $in_foc_count = mysqli_fetch_assoc($in_foc_count_query_status);
                $response['IN']['transaction_count']['foc'] = (int)$in_foc_count['in_foc'];

                /* IN membership count */
                $member_count_query = "SELECT count(t.in_type) AS membership
                                FROM sps_transactions AS t
                                WHERE t.id in ($in_trans_ids) AND  t.in_type IN (2,5,6,7)";
                $logString .= $member_count_query . "\n" . PHP_EOL;
                $member_count_query_status = mysqli_query($conn, $member_count_query);
                $member_count = mysqli_fetch_assoc($member_count_query_status);
                $response['IN']['transaction_count']['membership'] = (int)$member_count['membership'];

                /* IN voucher count */
                $voucher_count_query = "SELECT count(t.in_type) AS voucher
                                FROM sps_transactions AS t
                                WHERE t.id in ($in_trans_ids) AND  t.in_type IN (3,4)";
                $logString .= $voucher_count_query . "\n" . PHP_EOL;
                $voucher_count_query_status = mysqli_query($conn, $voucher_count_query);
                $voucher_count = mysqli_fetch_assoc($voucher_count_query_status);
                $response['IN']['transaction_count']['vouchers'] = (int)$voucher_count['voucher'];

                /* IN standard count */
                $std_count_query = "SELECT count(t.in_type) AS standard
                                FROM sps_transactions AS t
                                WHERE t.id in ($in_trans_ids) AND  t.in_type = 1";
                $logString .= $std_count_query . "\n" . PHP_EOL;
                $std_count_query_status = mysqli_query($conn, $std_count_query);
                $std_count = mysqli_fetch_assoc($std_count_query_status);
                $response['IN']['transaction_count']['standard'] = (int)$std_count['standard'];

                /* IN discount voucher count */
                $discount_voucher_count_query = "SELECT count(std.discount_id) AS discount_voucher
                                FROM sps_transaction_discount AS std
                                LEFT JOIN sps_transactions AS t ON std.transaction_id = t.id
                                WHERE t.id in ($in_trans_ids) AND  t.in_type = 1 AND std.discount_at='ENTRY'";
                $logString .= $discount_voucher_count_query . "\n" . PHP_EOL;
                $discount_voucher_count_query_status = mysqli_query($conn, $discount_voucher_count_query);
                $discount_voucher_count = mysqli_fetch_assoc($discount_voucher_count_query_status);
                $response['IN']['transaction_count']['discount_voucher_count'] = (int)$discount_voucher_count['discount_voucher'];
            }

            $out_trans_ids = '0';
            if (preg_match('/2/', $result['gate_type']) == 1) {
                $response['OUT'] = array();
                $response['OUT']['report_date_time'] = date("d-m-Y H:i:s");
                $response['OUT']['shift_start'] = $result['shift_start'];
                $response['OUT']['shift_end'] = $result['shift_end'];
                $response['OUT']['user_name'] = $result['user_name'];

                $where = ' out_shift_id= ' . $shift_id . ' AND out_gate = ' . $gate_id . ' ';
                $q_trans_count = "SELECT t.id
                    FROM sps_transactions AS t
                    LEFT JOIN sps_payment_modes AS opm ON
                    opm.id = t.out_payment_mode
                    WHERE (" . $where . ")";
                $querystatus = get_result_array($conn, $q_trans_count);
                $trans_out_count = count($querystatus);
                foreach ($querystatus as $key => $val) {
                    $out_trans_ids = $out_trans_ids . ',' . $val['id'];
                }
                $response['OUT']['total_trans'] = (int)count($querystatus);

                /* LOOP A count */
                $loopa_count_query = "SELECT loop_a AS loopa,loop_b AS loopb
                                FROM sps_barrier_loop_count
                                WHERE shift_id = '" . $shift_id . "'";
                $logString .= $loopa_count_query . "\n" . PHP_EOL;
                $loopa_count_query_status = mysqli_query($conn, $loopa_count_query);
                $loopa_count = mysqli_fetch_assoc($loopa_count_query_status);

                /* BG count */
                $bg_count_query = "SELECT barrier_open, barrier_close
                                    FROM sps_barrier_loop_count
                                    WHERE shift_id = '" . $shift_id . "'";
                $logString .= $bg_count_query . "\n" . PHP_EOL;
                $bg_count_query_status = mysqli_query($conn, $bg_count_query);
                $bg_count = mysqli_fetch_assoc($bg_count_query_status);
                $response['OUT']['barrier_open'] = isset($bg_count['barrier_open']) ? (int)$bg_count['barrier_open'] : 0;
                $response['OUT']['barrier_close'] = isset($bg_count['barrier_close']) ? (int)$bg_count['barrier_close'] : 0;
                $response['OUT']['total_loop_count_a'] = isset($loopa_count['loopa']) ? (int)$loopa_count['loopa'] : 0;
                $response['OUT']['total_loop_count_b'] = isset($loopa_count['loopb']) ? (int)$loopa_count['loopb'] : 0;
                //$response['OUT']['total_bg_count'] = 0;

                /* OUT Payment Total */
                $out_payaments = array();
                if (strpos($result['gate_type'], '2') !== false) {
                    $out_payment_qry = "SELECT spm.id, spm.payment_mode, sum(st.out_payment_amount) AS out_pay
                            FROM sps_payment_modes AS spm
                            LEFT JOIN sps_transactions AS st ON spm.id = st.out_payment_mode AND st.id in ($out_trans_ids)
                            GROUP BY spm.id";
                    $out_payaments = get_result_array($conn, $out_payment_qry);
                }

                /* collection payments */
                $collection_pay = [];
                if (!empty($out_payaments)) {
                    foreach ($out_payaments as $out_pay) {
                        $collection_pay[$out_pay['payment_mode']] = $out_pay['out_pay'] ? (int)$out_pay['out_pay'] : 0;
                    }
                }
                $response['OUT']['collection'] = $collection_pay;

                /* OUT F8 count*/
                $query_f8 = "SELECT count(st.f_eight) as f8
                        FROM sps_transactions AS st
                        LEFT JOIN sps_payment_modes AS ipm ON ipm.id = st.out_payment_mode
                        WHERE st.id in ($out_trans_ids)
                        AND st.f_eight=1 aND st.f_eight is not null AND st.out_foc IS NULL";
                $logString .= $query_f8 . "\n" . PHP_EOL;
                $res_query_f8 = mysqli_query($conn, $query_f8);
                $count_f8 = mysqli_fetch_assoc($res_query_f8);
                $response['OUT']['transaction_count']['f8'] = (int)$count_f8['f8'];

                /* OUT F7 count*/
                $query_f7 = "SELECT count(st.lost_ticket) as f7
                        FROM sps_transactions AS st
                        LEFT JOIN sps_payment_modes AS opm ON opm.id = st.out_payment_mode
                        where st.id in ($out_trans_ids)
                        AND st.lost_ticket = 1";
                $logString .= $query_f7 . "\n" . PHP_EOL;
                $res_query_f7 = mysqli_query($conn, $query_f7);
                $count_f7 = mysqli_fetch_assoc($res_query_f7);
                $response['OUT']['transaction_count']['f7'] = (int)$count_f7['f7'];

                /* OUT FOC count */
                $out_foc_count['out_foc'] = 0;
                $out_foc_count_query = "SELECT count(t.out_foc) AS out_foc
                                FROM sps_transactions AS t
                                WHERE t.id in ($out_trans_ids)  AND t.out_foc = 1";
                $logString .= $out_foc_count_query . "\n" . PHP_EOL;
                $out_foc_count_query_status = mysqli_query($conn, $out_foc_count_query);
                $out_foc_count = mysqli_fetch_assoc($out_foc_count_query_status);
                $response['OUT']['transaction_count']['foc'] = (int)$out_foc_count['out_foc'];

                /* OUT membership count */
                $member_count_query = "SELECT count(t.out_type) AS membership
                                FROM sps_transactions AS t
                                WHERE t.id in ($out_trans_ids) AND  t.out_type IN (2,5,6,7);";
                $logString .= $member_count_query . "\n" . PHP_EOL;
                $member_count_query_status = mysqli_query($conn, $member_count_query);
                $member_count = mysqli_fetch_assoc($member_count_query_status);
                $response['OUT']['transaction_count']['membership'] = (int)$member_count['membership'];

                /* OUT voucher count */
                $voucher_count_query = "SELECT count(t.out_type) AS voucher
                                FROM sps_transactions AS t
                                WHERE t.id in ($out_trans_ids) AND  t.out_type IN (3,4);";
                $logString .= $voucher_count_query . "\n" . PHP_EOL;
                $voucher_count_query_status = mysqli_query($conn, $voucher_count_query);
                $voucher_count = mysqli_fetch_assoc($voucher_count_query_status);
                $response['OUT']['transaction_count']['vouchers'] = (int)$voucher_count['voucher'];

                /* OUT standard count */
                $std_count_query = "SELECT count(t.out_type) AS standard
                                FROM sps_transactions AS t
                                WHERE t.id in ($out_trans_ids) AND  t.out_type = 1;";
                $logString .= $std_count_query . "\n" . PHP_EOL;
                $std_count_query_status = mysqli_query($conn, $std_count_query);
                $std_count = mysqli_fetch_assoc($std_count_query_status);
                $response['OUT']['transaction_count']['standard'] = (int)$std_count['standard'];

                /* OUT discount voucher count */
                $discount_voucher_count_query = "SELECT count(std.discount_id) AS discount_voucher
                                FROM sps_transaction_discount AS std
                                LEFT JOIN sps_transactions AS t ON std.transaction_id = t.id
                                WHERE t.id in ($out_trans_ids) AND  t.out_type = 1 AND std.discount_at='EXIT'";
                $logString .= $discount_voucher_count_query . "\n" . PHP_EOL;
                $discount_voucher_count_query_status = mysqli_query($conn, $discount_voucher_count_query);
                $discount_voucher_count = mysqli_fetch_assoc($discount_voucher_count_query_status);
                $response['OUT']['transaction_count']['discount_voucher_count'] = (int)$discount_voucher_count['discount_voucher'];
            }

            $cps_trans_ids = '0';
            if (preg_match('/5/', $result['gate_type']) == 1) {
                $response['CPS'] = array();
                $response['CPS']['report_date_time'] = date("d-m-Y H:i:s");
                $response['CPS']['shift_start'] = $result['shift_start'];
                $response['CPS']['shift_end'] = $result['shift_end'];
                $response['CPS']['user_name'] = $result['user_name'];

                $where = ' cps_shift_id= ' . $shift_id . ' AND cps_gate = ' . $gate_id . ' ';
                $q_trans_count = "SELECT t.cps_id
                    FROM sps_cps AS t
                    LEFT JOIN sps_payment_modes AS cpm ON
                    cpm.id = t.cps_payment_mode
                    WHERE (" . $where . ")";
                $querystatus = get_result_array($conn, $q_trans_count);
                $trans_out_count = count($querystatus);
                foreach ($querystatus as $key => $val) {
                    $cps_trans_ids = $cps_trans_ids . ',' . $val['cps_id'];
                }
                $response['CPS']['total_trans'] = (int)count($querystatus);

                /* LOOP A count */
                $loopa_count_query = "SELECT loop_a AS loopa,loop_b AS loopb
                                FROM sps_barrier_loop_count
                                WHERE shift_id = '" . $shift_id . "'";
                $logString .= $loopa_count_query . "\n" . PHP_EOL;
                $loopa_count_query_status = mysqli_query($conn, $loopa_count_query);
                $loopa_count = mysqli_fetch_assoc($loopa_count_query_status);

                /* BG count */
                $bg_count_query = "SELECT barrier_open, barrier_close
                                    FROM sps_barrier_loop_count
                                    WHERE shift_id = '" . $shift_id . "'";
                $logString .= $bg_count_query . "\n" . PHP_EOL;
                $bg_count_query_status = mysqli_query($conn, $bg_count_query);
                $bg_count = mysqli_fetch_assoc($bg_count_query_status);
                $response['CPS']['barrier_open'] = isset($bg_count['barrier_open']) ? (int)$bg_count['barrier_open'] : 0;
                $response['CPS']['barrier_close'] = isset($bg_count['barrier_close']) ? (int)$bg_count['barrier_close'] : 0;
                $response['CPS']['total_loop_count_a'] = isset($loopa_count['loopa']) ? (int)$loopa_count['loopa'] : 0;
                $response['CPS']['total_loop_count_b'] = isset($loopa_count['loopb']) ? (int)$loopa_count['loopb'] : 0;
                //$response['OUT']['total_bg_count'] = 0;

                /* OUT Payment Total */
                $cps_payaments = array();
                if (strpos($result['gate_type'], '5') !== false) {
                    $cps_payment_qry = "SELECT spm.id, spm.payment_mode, sum(sc.cps_payment_amount) AS cps_pay
                            FROM sps_payment_modes AS spm
                            LEFT JOIN sps_cps AS sc ON spm.id = sc.cps_payment_mode AND sc.cps_id in ($cps_trans_ids)
                            GROUP BY spm.id";
                    $cps_payaments = get_result_array($conn, $cps_payment_qry);
                }

                /* collection payments */
                $collection_pay = [];
                if (!empty($cps_payaments)) {
                    foreach ($cps_payaments as $cps_pay) {
                        $collection_pay[$cps_pay['payment_mode']] = $cps_pay['cps_pay'] ? (int)$cps_pay['cps_pay'] : 0;
                    }
                }
                $response['CPS']['collection'] = $collection_pay;


                /* CPS F8 count*/
                $query_f8 = "SELECT count(sc.cps_f_eight) as f8
                        FROM sps_cps AS sc
                        LEFT JOIN sps_payment_modes AS cpm ON cpm.id = sc.cps_payment_mode
                        WHERE sc.cps_id in ($cps_trans_ids)
                        AND sc.cps_f_eight=1 AND sc.cps_f_eight is not null AND st.cps_foc IS NULL";
                $logString .= $query_f8 . "\n" . PHP_EOL;
                $res_query_f8 = mysqli_query($conn, $query_f8);
                $count_f8 = mysqli_fetch_assoc($res_query_f8);
                $response['CPS']['transaction_count']['f8'] = (int)$count_f8['f8'];

                /* CPS F7 count*/
                $query_f7 = "SELECT count(sc.cps_lost_ticket) as f7
                        FROM sps_cps AS sc
                        LEFT JOIN sps_payment_modes AS cpm ON cpm.id = sc.cps_payment_mode
                        where sc.cps_id in ($cps_trans_ids)
                        AND sc.cps_lost_ticket = 1";
                $logString .= $query_f7 . "\n" . PHP_EOL;
                $res_query_f7 = mysqli_query($conn, $query_f7);
                $count_f7 = mysqli_fetch_assoc($res_query_f7);
                $response['CPS']['transaction_count']['f7'] = (int)$count_f7['f7'];

                /* CPS FOC count */
                $cps_foc_count_query = "SELECT count(sc.cps_foc) AS cps_foc
                                FROM sps_cps AS sc
                                WHERE sc.cps_id in ($cps_trans_ids)  AND sc.cps_foc = 1";
                $logString .= $cps_foc_count_query . "\n" . PHP_EOL;
                $cps_foc_count_query_status = mysqli_query($conn, $cps_foc_count_query);
                $cps_foc_count = mysqli_fetch_assoc($cps_foc_count_query_status);
                $response['CPS']['transaction_count']['foc'] = (int)$cps_foc_count['cps_foc'];

                /* CPS membership count */
                $member_count_query = "SELECT count(sc.cps_type) AS membership
                                FROM sps_cps AS sc
                                WHERE sc.cps_id in ($cps_trans_ids) AND  sc.cps_type IN (2,5,6,7);";
                $logString .= $member_count_query . "\n" . PHP_EOL;
                $member_count_query_status = mysqli_query($conn, $member_count_query);
                $member_count = mysqli_fetch_assoc($member_count_query_status);
                $response['CPS']['transaction_count']['membership'] = (int)$member_count['membership'];

                /* CPS voucher count */
                $voucher_count_query = "SELECT count(sc.cps_type) AS voucher
                                FROM sps_cps AS sc
                                WHERE sc.cps_id in ($cps_trans_ids) AND  sc.cps_type IN (3,4);";
                $logString .= $voucher_count_query . "\n" . PHP_EOL;
                $voucher_count_query_status = mysqli_query($conn, $voucher_count_query);
                $voucher_count = mysqli_fetch_assoc($voucher_count_query_status);
                $response['CPS']['transaction_count']['vouchers'] = (int)$voucher_count['voucher'];

                /* CPS standard count */
                $std_count_query = "SELECT count(sc.cps_type) AS standard
                                FROM sps_cps AS sc
                                WHERE sc.cps_id in ($cps_trans_ids) AND  sc.cps_type = 1;";
                $logString .= $std_count_query . "\n" . PHP_EOL;
                $std_count_query_status = mysqli_query($conn, $std_count_query);
                $std_count = mysqli_fetch_assoc($std_count_query_status);
                $response['CPS']['transaction_count']['standard'] = (int)$std_count['standard'];

                /* OUT discount voucher count */
                $discount_voucher_count_query = "SELECT count(std.discount_id) AS discount_voucher
                                FROM sps_transaction_discount AS std
                                LEFT JOIN sps_cps AS sc ON std.transaction_id = sc.transaction_id
                                WHERE sc.cps_id in ($cps_trans_ids) AND  sc.cps_type = 1 AND std.discount_at='CPS'";
                $logString .= $discount_voucher_count_query . "\n" . PHP_EOL;
                $discount_voucher_count_query_status = mysqli_query($conn, $discount_voucher_count_query);
                $discount_voucher_count = mysqli_fetch_assoc($discount_voucher_count_query_status);
                $response['CPS']['transaction_count']['discount_voucher_count'] = (int)$discount_voucher_count['discount_voucher'];
            }

            $response_data = json_encode(array('err_code' => '0', 'data' => $response), JSON_FORCE_OBJECT);
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            $response_data = json_encode(array('err_code' => '10013', 'err_msg' => $errors['10013']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;

    case 'getVouchers':
        $today = date('Y-m-d');
        $NewDate = Date('Y-m-d', strtotime("+5 days"));
        $query_sps_vouchers = "SELECT * FROM sps_vouchers WHERE status = 1 AND deleted_at IS NULL AND DATE(expiry_date) >= '" . date('Y-m-d') . "'";
        $logString .= $query_sps_vouchers . "\n" . PHP_EOL;
        $vouchers = mysqli_query($conn, $query_sps_vouchers);
        if (!$vouchers) {
            $response_data = json_encode(array('err_code' => '10023', 'err_msg' => $errors['10023']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        while ($voucher = mysqli_fetch_assoc($vouchers)) {
            $response['voucherData'][] = $voucher;
        }
        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();

        createLog($logString);
        break;

    case 'getMembershipProducts' :
        $page = $_REQUEST['page'];
        $count = $_REQUEST['count'];
        $timestamp = $_REQUEST['timestamp'];
        $membership_type = $_REQUEST['membership_type'];

        $where = ' 1=1 ';
        if ($timestamp != 0) {
//            $updated_date = (new DateTime("@$timestamp"))->format('Y-m-d H:i:s') ;

        }
        $where .= ' AND updated_at >= "' . $timestamp . '"';
        $where .= ' AND deleted_at IS NULL';
        /*if($membership_type  == 2 ){
            $where .= ' AND type_member IN (4,5) ';
        }*/
        $query_member_tariff = "SELECT COUNT(*) AS tariff_count FROM sps_tariff_member WHERE " . $where . " ";
        $logString .= $query_member_tariff . "\n" . PHP_EOL;
        $res = mysqli_query($conn, $query_member_tariff);
        $tariff = mysqli_fetch_assoc($res);
        $response['total_count'] = $tariff['tariff_count'];

        $query_vehicle_types = "SELECT * FROM sps_tariff_member WHERE " . $where . " LIMIT " . ($page * $count) . "," . $count;
        $logString .= $query_vehicle_types . "\n" . PHP_EOL;
        $res = mysqli_query($conn, $query_vehicle_types);
        $i = 0;
        $response['tariff_member'] = array();
        while ($tariff = mysqli_fetch_assoc($res)) {
            $response['tariff_member'][$i] = $tariff;
            $i++;
        }
        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
        createLog($logString);
        break;

    case 'voucherUsedCount':
        $voucher_id = $_REQUEST['voucher_id'];
        $query_vouchers = "SELECT count,used_count FROM sps_vouchers WHERE status = 1 AND id = '" . $voucher_id . "' AND deleted_at IS NULL";
        $logString .= $query_vouchers . "\n" . PHP_EOL;
        $vouchers = mysqli_query($conn, $query_vouchers);
        if (!$vouchers) {
            $response_data = json_encode(array('err_code' => '10028', 'err_msg' => $errors['10028']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        while ($voucher = mysqli_fetch_assoc($vouchers)) {
            $response['voucherData'][] = $voucher;
        }
        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
        createLog($logString);
        break;

    case 'searchByVehicle':
        $vehicle_number = trim($_REQUEST['vehicle_number']);
        if (!empty($vehicle_number)) {
            $queryblocked = "SELECT id from sps_blocked_vehicles where vehicle_no = '" . $vehicle_number . "' AND deleted_at IS NULL";
            $logString .= $queryblocked . "\n" . PHP_EOL;
            $resultblocked = mysqli_query($conn, $queryblocked);
            if (mysqli_num_rows($resultblocked) > 0) {
                $response_data = json_encode(array('err_code' => '10036', 'err_msg' => $errors['10036']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            } else {
                $query = "SELECT st.id,st.barcode,st.in_time,st.vehicle_type,st.vehicle_number,st.in_shift_id,st.in_type,st.in_type_id,st.in_gate,st.in_user_id,st.in_payment_mode,st.in_master_payment_mode,st.in_membership_payment_amount,st.in_payment_amount,st.penalty_charge,sgc.gate_name AS in_gate_name,su.user_name AS in_username, in_foc_difference,vip_membership_slot
                          FROM sps_transactions AS st
                          LEFT JOIN sps_gate_configurations AS sgc ON st.in_gate = sgc.gate_id
                          LEFT JOIN sps_users AS su ON st.in_user_id = su.user_id
                          WHERE st.deleted_at IS NULL AND st.out_gate IS NULL ";
                $query .= " AND vehicle_number = '" . $vehicle_number . "'";
                $logString .= $query . "\n" . PHP_EOL;
                $result = mysqli_query($conn, $query);
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    $response_data = json_encode(array('err_code' => '10034', 'err_msg' => $errors['10034']));
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();
                } else {
                    $response_data = json_encode(array('err_code' => '0', 'data' => $response, 'invoice_number' => '')); //US872
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();
                }
            }
        }
        createLog($logString);
        break;
    /* added for reprint ticket data */
    case 'reprint_count':
        $barcode = trim($_REQUEST['barcode']);
        $transaction_id = trim($_REQUEST['transaction_id']);
        $rfid_number = trim($_REQUEST['rfid_number']);
        $reprint_type = trim($_REQUEST['reprint_type']);
        $cps_barcode = trim($_REQUEST['cps_barcode']);
        if (empty($barcode) && empty($transaction_id) && empty($rfid_number) && empty($cps_barcode)) {
            $response_data = json_encode(array('err_code' => '10035', 'err_msg' => $errors['10035']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        if (empty($reprint_type)) {
            $response_data = json_encode(array('err_code' => '10035', 'err_msg' => $errors['10035']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }

        if ($reprint_type == 'CPS') {
            $update_at = "cps_barcode = '" . $cps_barcode . "'";
            $query = "select cps_reprint_count from sps_cps where " . $update_at;
            $logString .= $query . "\n" . PHP_EOL;
            $result = mysqli_query($conn, $query);
            $transaction = mysqli_fetch_assoc($result);

            if (!is_null($transaction['cps_reprint_count'])) {
                $reprint_cps = $transaction['cps_reprint_count'] + 1; // added for cps reprint
            } else {
                $reprint_cps = 1; // added for cps reprint
            }
            $query_update = "update sps_cps set cps_reprint_count = '$reprint_cps' where " . $update_at;
        } else {
            if (!empty($barcode)) {
                $update_at = "barcode = '" . $barcode . "'";
            } else if (!empty($transaction_id)) {
                $update_at = "id = " . $transaction_id;
            } else if (!empty($rfid_number)) {
                $rfid_query = "SELECT id FROM `sps_transactions` WHERE `out_type_id` = '$rfid_number' ORDER BY `sps_transactions`.`out_time` DESC limit 1";
                $logString .= $rfid_query . "\n" . PHP_EOL;
                $rfid_result = mysqli_query($conn, $rfid_query);
                $rfid_id = mysqli_fetch_assoc($rfid_result);
                $update_at = "id = " . $rfid_id['id'];
            }
            $query = "select reprint_count from sps_transactions where " . $update_at;
            $logString .= $query . "\n" . PHP_EOL;
            $result = mysqli_query($conn, $query);
            $transaction = mysqli_fetch_assoc($result);
            if (!is_null($transaction['reprint_count'])) {
                $reprint_data = json_decode($transaction['reprint_count'], True);
                $reprint_entry = $reprint_type == 'ENTRY' ? $reprint_data['ENTRY'] + 1 : $reprint_data['ENTRY'];
                $reprint_exit = $reprint_type == 'EXIT' ? $reprint_data['EXIT'] + 1 : $reprint_data['EXIT'];
            } else {
                $reprint_entry = $reprint_type == 'ENTRY' ? 1 : 0;
                $reprint_exit = $reprint_type == 'EXIT' ? 1 : 0;
            }
            $reprint_data = array('ENTRY' => $reprint_entry, 'EXIT' => $reprint_exit);
            $reprint_data = json_encode($reprint_data);

            $query_update = "update sps_transactions set reprint_count = '$reprint_data' where " . $update_at;
        }
        $logString .= $query_update . "\n" . PHP_EOL;
        $result_update = mysqli_query($conn, $query_update);
        $transaction_check = mysqli_fetch_assoc($result_update);
        if ($result_update) {
            $response['msg'] = 'Transaction updated successfully!';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            $response_data = json_encode(array('err_code' => '10035', 'err_msg' => $errors['10035']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;
    /* end */
    /* added for CPS */
    case 'cpsSearch':
        $imageTransId = NULL; //NULL
        $vehicle_number = trim($_REQUEST['vehicle_number']);
        $barcode = trim($_REQUEST['barcode']); // entry barcode and entering in receipt barcode
        $cps_barcode = trim($_REQUEST['cps_barcode']);
        $rfid = trim($_REQUEST['rfid']);
        if (empty($vehicle_number) && empty($barcode) && empty($rfid) && empty($cps_barcode)) {
            $response_data = json_encode(array('err_code' => '10017', 'err_msg' => $errors['10017']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        if (!empty($vehicle_number)) {
            $queryblocked = "SELECT id from sps_blocked_vehicles where vehicle_no = '" . $vehicle_number . "' AND deleted_at IS NULL";
            $logString .= $queryblocked . "\n" . PHP_EOL;
            $resultblocked = mysqli_query($conn, $queryblocked);
            if (mysqli_num_rows($resultblocked) > 0) {
                $response_data = json_encode(array('err_code' => '10036', 'err_msg' => $errors['10036']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
        }
        $cps_query = "SELECT cps.cps_id,cps.cps_barcode,cps.receipt_barcode,cps.transaction_id,cps.cps_out_time,cps.cps_vehicle_type,cps.cps_vehicle_number,cps.cps_shift_id,cps.cps_type,cps.cps_type_id,cps.cps_gate,cps.cps_user_id,cps.cps_payment_mode,cps.cps_master_payment_mode,cps.cps_foc_difference,cps.cps_membership_payment_amount,cps.cps_payment_amount,cps.cps_standard_parking_amount,sgc.gate_name AS in_gate_name,su.user_name AS in_user_name,st.ticket_no
                FROM sps_cps AS cps
                LEFT JOIN sps_transactions AS st ON st.id = cps.transaction_id
                LEFT JOIN sps_gate_configurations AS sgc ON cps.cps_gate = sgc.gate_id
                LEFT JOIN sps_users AS su ON cps.cps_user_id = su.user_id
                WHERE cps.deleted_at IS NULL AND st.out_gate IS NULL";//no change        
        if (!empty($cps_barcode)) {
            $check_query = "select sps.out_time,sps.out_gate from sps_transactions sps left join sps_cps cps on sps.barcode = cps.receipt_barcode where sps.deleted_at is NULL and cps.cps_barcode = '" . $cps_barcode . "'";
            $check_query .= "  ORDER BY id DESC limit 1";
            $logString .= $check_query . "\n" . PHP_EOL;
            $check_row = mysqli_query($conn, $check_query);
            $check_transaction = mysqli_fetch_assoc($check_row);
            if (mysqli_num_rows($check_row) <= 0) {
                $response_data = json_encode(array('err_code' => '10017', 'err_msg' => $errors['10017']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            } elseif (mysqli_num_rows($check_row) > 0 && !is_null($check_transaction['out_gate']) && $check_transaction['out_time'] != '0000-00-00 00:00:00') {
                $response_data = json_encode(array('err_code' => '10033', 'err_msg' => $errors['10033']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }


            $cps_query .= " AND (cps_barcode = '$cps_barcode')";
            $cps_result = mysqli_query($conn, $cps_query);
            $cps_transaction = mysqli_fetch_assoc($cps_result);
            if (!is_null($cps_transaction)) {
                if ($cps_transaction['cps_vehicle_number'] == '0') {
                    $cps_transaction['cps_vehicle_number'] = '';
                }
                $imageTransId = $cps_transaction['transaction_id'];
                $cps_transaction['invoice_number'] = $invoice_number . $cps_transaction['ticket_no'];
                $response['cps_transaction'] = $cps_transaction;
                //$response['cps_transaction']['images'] = getImages($conn, $cps_transaction['transaction_id'], $img_base_url);
            } else {
                $response_data = json_encode(array('err_code' => '10017', 'err_msg' => $errors['10017']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }
        } else {

            $check_query = "select out_time,out_gate from sps_transactions where deleted_at is NULL";
            if (!empty($barcode)) {
                $check_query .= " AND barcode = '$barcode'";
            } elseif (!empty($vehicle_number)) {
                $check_query .= " AND vehicle_number = '" . $vehicle_number . "'";
            } elseif (!empty($rfid)) {
                $check_query .= " AND in_type = 2 and in_type_id = '" . $rfid . "'";
            }

            $check_query .= "  ORDER BY id DESC limit 1";
            $logString .= $check_query . "\n" . PHP_EOL;
            $check_row = mysqli_query($conn, $check_query);
            $check_transaction = mysqli_fetch_assoc($check_row);
            if (mysqli_num_rows($check_row) <= 0) {
                $response_data = json_encode(array('err_code' => '10017', 'err_msg' => $errors['10017']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            } elseif (mysqli_num_rows($check_row) > 0 && !is_null($check_transaction['out_gate']) && $check_transaction['out_time'] != '0000-00-00 00:00:00') {
                $response_data = json_encode(array('err_code' => '10033', 'err_msg' => $errors['10033']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            }

            if (!empty($vehicle_number) && !empty($barcode)) {
                $cps_query .= " AND (cps_vehicle_number LIKE '%$vehicle_number%' OR receipt_barcode = '$barcode')";
            } elseif (!empty($barcode)) {
                $cps_query .= " AND receipt_barcode = '$barcode'";
            } elseif (!empty($vehicle_number)) {
                $cps_query .= " AND cps_vehicle_number = '" . $vehicle_number . "'";
            } elseif (!empty($rfid)) {
                $cps_query .= " AND cps_type = 2 and cps_type_id = '" . $rfid . "'";
            }
            $cps_query .= " ORDER BY cps_id DESC limit 1";

            //echo $cps_query; exit;
            $logString .= $cps_query . "\n" . PHP_EOL;
            $cps_result = mysqli_query($conn, $cps_query);
            $cps_transaction = mysqli_fetch_assoc($cps_result);
            if (!is_null($cps_transaction)) {
                if ($cps_transaction['cps_vehicle_number'] == '0') {
                    $cps_transaction['cps_vehicle_number'] = '';
                }
                $imageTransId = $cps_transaction['transaction_id'];
                $cps_transaction['invoice_number'] = $invoice_number . $cps_transaction['ticket_no'];
                $response['cps_transaction'] = $cps_transaction;
                // $response['cps_transaction']['images'] = getImages($conn, $cps_transaction['transaction_id'], $img_base_url);
            } else {
                $query = "SELECT st.id,st.barcode,st.in_time,st.vehicle_type,st.vehicle_number,st.in_shift_id,st.in_type,st.in_type_id,st.in_gate,st.in_user_id,st.in_payment_mode,st.in_master_payment_mode,st.in_membership_payment_amount,st.in_payment_amount,st.penalty_charge,st.in_standard_parking_amount,st.out_standard_parking_amount,sgc.gate_name AS in_gate_name,su.user_name AS in_username, in_foc_difference,vip_membership_slot,st.in_standard_parking_amount,st.out_standard_parking_amount,st.ticket_no
                  FROM sps_transactions AS st
                  LEFT JOIN sps_gate_configurations AS sgc ON st.in_gate = sgc.gate_id
                  LEFT JOIN sps_users AS su ON st.in_user_id = su.user_id
                  WHERE st.out_gate IS NULL AND st.deleted_at IS NULL";
                if (!empty($vehicle_number) && !empty($barcode)) {
                    $query .= " AND (vehicle_number LIKE '%$vehicle_number%' OR barcode = '$barcode')";
                } elseif (!empty($barcode)) {
                    $query .= " AND barcode = '$barcode'";
                } elseif (!empty($vehicle_number)) {
                    $query .= " AND vehicle_number = '" . $vehicle_number . "'";
                } elseif (!empty($rfid)) {
                    $query .= " AND in_type = 2 and in_type_id = '" . $rfid . "'";
                }
                $logString .= $query . "\n" . PHP_EOL;
                $result = mysqli_query($conn, $query);
                $transaction = mysqli_fetch_assoc($result);
                if (!is_null($transaction)) {
                    if ($transaction['vehicle_number'] == '0') {
                        $transaction['vehicle_number'] = '';
                    }
                    $imageTransId = $transaction['id'];
                    $transaction['invoice_number'] = $invoice_number . $transaction['ticket_no'];
                    $response['transaction'] = $transaction;
                    $query_additionalResult = "SELECT transaction_id,additional_tariff_id,additional_tariff_amount,gate_id,additional_charges_at FROM sps_transaction_additional_charges WHERE additional_charges_at = 'ENTRY' AND transaction_id = '" . $transaction['id'] . "' AND deleted_at IS NULL";
                    $additionalResult = mysqli_query($conn, $query_additionalResult);
                    $logString .= $query_additionalResult . "\n" . PHP_EOL;
                    while ($additional = mysqli_fetch_assoc($additionalResult)) {
                        $response['transaction']['additionalCharges'][] = $additional;
                    }
                    $query_discountResult = "SELECT transaction_id,discount_type,discount_type_id,discount_value_type,discount_value,discount_amount,discount_at FROM sps_transaction_discount WHERE discount_at = 'ENTRY' AND transaction_id = '" . $transaction['id'] . "' AND deleted_at IS NULL";
                    $discountResult = mysqli_query($conn, $query_discountResult);
                    $logString .= $query_discountResult . "\n" . PHP_EOL;
                    while ($discount = mysqli_fetch_assoc($discountResult)) {
                        $response['transaction']['discounts'][] = $discount;
                    }
                    $query_taxBreakupResult = "SELECT module_id as transaction_id,tax_amount as tax_value,tax_id, module as tax_breakup_at,id as gate_id FROM sps_tax_breakup WHERE  module = 'ENTRY' AND module_id = '" . $transaction['id'] . "' AND deleted_at IS NULL";
                    $taxBreakupResult = mysqli_query($conn, $query_taxBreakupResult);
                    $logString .= $query_taxBreakupResult . "\n" . PHP_EOL;
                    while ($tax = mysqli_fetch_assoc($taxBreakupResult)) {
                        $response['transaction']['taxBreakup'][] = $tax;
                    }
                } else {
                    $response_data = json_encode(array('err_code' => '10017', 'err_msg' => $errors['10017']));
                    echo $response_data;
                    $logString .= $response_data . "\n" . PHP_EOL;
                    createLog($logString);
                    exit();
                }
            }
        }
        $query_imagesResult = "SELECT * FROM sps_images WHERE module = 'Entry' AND module_id = '" . $imageTransId . "'";
        $logString .= $query_imagesResult . "\n" . PHP_EOL;
        $imagesResult = mysqli_query($conn, $query_imagesResult);
        $rowcount = mysqli_num_rows($imagesResult);
        if ($rowcount) {
            while ($image = mysqli_fetch_assoc($imagesResult)) {
                $module_id = $image['module_id'];
                $dataArray[] = $img_base_url . '/' . $image['file_name'];
            }
            if (isset($response['transaction']))
                $response['transaction']['images'] = $dataArray;
            else
                $response['cps_transaction']['images'] = $dataArray;
        }

        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
        createLog($logString);
        break;
    /* end */

    /* added for insert CPS */
    case 'cps_transaction':

        /* US793: Dynamic QR Code – Mobile Gate 
           Allowing to set payment reference number in case of master payment mode E wallet
        */
        if ($_REQUEST['cps_master_payment_mode'] == 4 && ($_REQUEST['in_payment_mode'] == 5 || $_REQUEST['in_payment_mode'] == 7) && !empty($_REQUEST['cps_payment_reference_number'])) {
            $cps_payment_reference_number = trim($_REQUEST['cps_payment_reference_number']);
        } else {
            $_REQUEST['cps_payment_reference_number'] = 0;
            $cps_payment_reference_number = 0;
        }
        if (!empty($cps_payment_reference_number)) {
            isValidPaymentReferenceNumber($conn, $errors, $logString, $cps_payment_reference_number);
        }
        /* US793: Dynamic QR Code – Mobile Gate */

        $inData['cps_barcode'] = trim($_REQUEST['cps_barcode']);
        $inData['receipt_barcode'] = trim($_REQUEST['receipt_barcode']);
        $inData['transaction_id'] = trim($_REQUEST['transaction_id']);
        $inData['cps_out_time'] = trim($_REQUEST['cps_out_time']);
        $inData['cps_total_time'] = trim($_REQUEST['cps_total_time']);
        $inData['cps_vehicle_type'] = trim($_REQUEST['cps_vehicle_type']);
        $inData['cps_vehicle_number'] = trim($_REQUEST['cps_vehicle_number']);
        $inData['cps_shift_id'] = trim($_REQUEST['cps_shift_id']);
        $inData['cps_type'] = trim($_REQUEST['cps_type']);
        $inData['cps_type_id'] = trim($_REQUEST['cps_type_id']);
        $inData['cps_gate'] = trim($_REQUEST['cps_gate']);
        $inData['cps_user_id'] = trim($_REQUEST['cps_user_id']);
        $inData['cps_payment_mode'] = trim($_REQUEST['cps_payment_mode']);
        $inData['cps_master_payment_mode'] = trim($_REQUEST['cps_master_payment_mode']);
        $inData['cps_day_type'] = trim($_REQUEST['cps_day_type']);
        $inData['cps_tariff_code'] = trim($_REQUEST['cps_tariff_code']);
        $inData['cps_foc_difference'] = trim($_REQUEST['cps_foc_difference']);
        $inData['cps_membership_payment_amount'] = trim($_REQUEST['cps_membership_payment_amount']);
        $inData['cps_payment_reference_number'] = trim($_REQUEST['cps_payment_reference_number']);
        $inData['cps_payment_amount'] = trim($_REQUEST['cps_payment_amount']);
        $inData['cps_penalty_charge'] = trim($_REQUEST['cps_penalty_charge']);
        $inData['cps_overnight_charges'] = trim($_REQUEST['cps_overnight_charges']);
        $inData['cps_lost_ticket'] = trim($_REQUEST['cps_lost_ticket']);
        $inData['cps_lost_ticket_penalty'] = trim($_REQUEST['cps_lost_ticket_penalty']);
        $inData['cps_manuel_ticket'] = trim($_REQUEST['cps_manuel_ticket']);
        $inData['cps_offline_ticket'] = trim($_REQUEST['cps_offline_ticket']);
        $inData['cps_reprint_count'] = trim($_REQUEST['cps_reprint_count']);
        $inData['cps_f_eight'] = trim($_REQUEST['cps_f_eight']);
        $inData['cps_f_eight_reason'] = trim($_REQUEST['cps_f_eight_reason']);
        $inData['cps_f_eight_notes'] = trim($_REQUEST['cps_f_eight_notes']);
        $inData['cps_total_payment_amount'] = trim($_REQUEST['cps_total_payment_amount']);
        $inData['cps_standard_parking_amount'] = trim($_REQUEST['cps_standard_parking_amount']);
        $inData['cps_foc'] = trim($_REQUEST['cps_foc']);
        $inData['cps_foc_reason'] = trim($_REQUEST['cps_foc_reason']);
        $inData['cps_foc_approval'] = trim($_REQUEST['cps_foc_approval']);
        $inData['cps_foc_note'] = trim($_REQUEST['cps_foc_note']);
        $inData['location_id'] = trim($_REQUEST['location_id']);
        $inData['area_id'] = trim($_REQUEST['area_id']);;
        $inData['created_by'] = trim($_REQUEST['cps_user_id']);
        $cpsGateDiscounts = null;
        $cpsGateAdditionalCharges = null;
        $discounts = null;
        $additionalCharges = null;
        $taxBreakup = null;
        if (empty($_REQUEST['cps_offline_ticket'])) {
            unset($inData['cps_offline_ticket']);
        }
        if (!empty($_REQUEST['out_additional_charges'])) {
            $additionalCharges = json_decode($_REQUEST['out_additional_charges'], true);
            unset($_REQUEST['out_additional_charges']);
        }

        if (!empty($_REQUEST['cps_gate_additional_charges'])) {
            $cpsGateAdditionalCharges = json_decode($_REQUEST['cps_gate_additional_charges'], true);
            unset($_REQUEST['cps_gate_additional_charges']);
        }

        if (!empty($_REQUEST['out_discount'])) {
            $discounts = json_decode($_REQUEST['out_discount'], true);
            unset($_REQUEST['out_discount']);
        }

        if (!empty($_REQUEST['cps_gate_discount'])) {
            $cpsGateDiscounts = json_decode($_REQUEST['cps_gate_discount'], true);
            unset($_REQUEST['cps_gate_discount']);
        }

        if (!empty($_REQUEST['out_tax_breakup'])) {
            $taxBreakup = json_decode($_REQUEST['out_tax_breakup'], true);
            unset($_REQUEST['out_tax_breakup']);
        }
        $gateId = FAlSE;
        if (isset($_REQUEST['entry_gate_name'])) {
            $query_getSearch_gate_id = "SELECT gate_id FROM sps_gate_configurations WHERE gate_name = '" . $_REQUEST['entry_gate_name'] . "' AND status='1' AND deleted_at IS NULL";
            $logString .= $query_getSearch_gate_id . "\n" . PHP_EOL;
            $getSearch = mysqli_fetch_assoc(mysqli_query($conn, $query_getSearch_gate_id));
            $gateId = $getSearch['gate_id'];
        }
        if ((empty($inData['transaction_id']) || $inData['transaction_id'] == 0)) {

            $entry_time = NULL;
            if (isset($_REQUEST['entry_time'])) {
                $entry_time = $_REQUEST['entry_time'];
            }

            if (!empty($inData['receipt_barcode'])) {
                $query = "SELECT id,offline_ticket,in_gate,in_time FROM sps_transactions WHERE barcode = '" . $inData['receipt_barcode'] . "' AND deleted_at IS NULL";
            } elseif (!empty($inData['cps_type']) && !empty($inData['cps_type_id'])) {
                $query = "SELECT id,offline_ticket,in_gate,in_time FROM sps_transactions WHERE in_type = '" . $inData['cps_type'] . "' AND in_type_id = '" . $inData['cps_type_id'] . "' AND deleted_at IS NULL";
            }
            $logString .= $query . "\n" . PHP_EOL;
            $result = mysqli_query($conn, $query);
            $rowcount = mysqli_num_rows($result);

            if ($rowcount == 0) {
                $transactionsData = array('barcode' => $inData['receipt_barcode'], 'vehicle_type' => $inData['cps_vehicle_type'], 'is_cps' => 1, 'vehicle_number' => $inData['cps_vehicle_number'], 'in_time' => $entry_time);
                if ($gateId) {
                    $transactionsData['in_gate'] = $gateId;
                }
                if (!empty($_REQUEST['cps_offline_ticket'])) {
                    $transactionsData['offline_ticket'] = $_REQUEST['cps_offline_ticket'];
                }
                $sps_transaction_query = insert($conn, 'sps_transactions', $transactionsData);
                $logString .= $sps_transaction_query . "\n" . PHP_EOL;
                if (mysqli_query($conn, $sps_transaction_query)) {
                    $transactionId = mysqli_insert_id($conn);;
                    $inData['transaction_id'] = $transactionId;
                }
            } else {
                $transaction = mysqli_fetch_assoc($result);
                $inData['transaction_id'] = $transaction['id'];
                $transactionId = $transaction['id'];
                $offline_bit = NULL;
                if ($transaction['offline_ticket'] == 1) {
                    $offline_bit = 9;
                } elseif ($transaction['offline_ticket'] == 2) {
                    $offline_bit = 11;
                } elseif ($transaction['offline_ticket'] == 3) {
                    $offline_bit = 17;
                } elseif ($transaction['offline_ticket'] == 4) {
                    $offline_bit = 19;
                } elseif (is_null($transaction['offline_ticket'])) {
                    $offline_bit = $_REQUEST['cps_offline_ticket'];
                }
                if (isset($_REQUEST['cps_offline_ticket']) && $_REQUEST['cps_offline_ticket'] == 12 && !empty($offline_bit)) {
                    $updatetransactions_lost = "UPDATE sps_transactions SET offline_ticket='$offline_bit',is_cps = '1' ";
                    if (empty($transaction['in_gate']) || $transaction['in_gate'] == 0) {
                        $updatetransactions_lost .= ",in_gate = $gateId";
                    }
                    if (is_null($transaction['in_time']) || $transaction['in_time'] == '0000-00-00 00:00:00') {
                        $updatetransactions_lost .= ",in_time = '" . $entry_time . "'";
                    }
                    $updatetransactions_lost .= "   WHERE id= '$transactionId' AND deleted_at IS NULL";
                    $logString .= $updatetransactions_lost . "\n" . PHP_EOL;
                    mysqli_query($conn, $updatetransactions_lost);
                }

            }
        }
        if (isset($inData['cps_f_eight']) && $inData['cps_f_eight'] > 0) {
            $f_eight_used = "UPDATE sps_shift_management SET f_eight_used = f_eight_used - 1 WHERE sr_no= '" . $inData['cps_shift_id'] . "' AND deleted_at IS NULL";
            mysqli_query($conn, $f_eight_used);

        }

        $cps_transaction_query = insert($conn, 'sps_cps', $inData);
        $logString .= $cps_transaction_query . "\n" . PHP_EOL;
        if (mysqli_query($conn, $cps_transaction_query)) {


            $transactionId = mysqli_insert_id($conn);
            if (!is_null($additionalCharges)) {
                foreach ($additionalCharges as $additionalCharge) {
                    $additionalData = array();
                    $additionalData['transaction_id'] = $inData['transaction_id'];
                    $additionalData['cps_id'] = $transactionId; //This is cps_id from sps_cps table last record inserted
                    $additionalData['additional_tariff_id'] = $additionalCharge['additional_tariff_id'];
                    $additionalData['additional_tariff_amount'] = $additionalCharge['additional_tariff_amount'];
                    $additionalData['additional_charges_at'] = $additionalCharge['additional_charges_at'];
                    $additionalData['gate_id'] = $additionalCharge['gate_id'];
                    $transaction_additional_charges = insert($conn, 'sps_transaction_additional_charges', $additionalData);
                    mysqli_query($conn, $transaction_additional_charges);
                    $logString .= $transaction_additional_charges . "\n" . PHP_EOL;
                    $additionalData = array();
                }
            }

            if (!is_null($cpsGateAdditionalCharges)) {
                foreach ($cpsGateAdditionalCharges as $additionalCharge) {
                    $additionalData = array();
                    $additionalData['transaction_id'] = $inData['transaction_id'];
                    $additionalData['cps_id'] = $transactionId; //This is cps_id from sps_cps table last record inserted
                    $additionalData['additional_tariff_id'] = $additionalCharge['additional_tariff_id'];
                    $additionalData['additional_tariff_amount'] = $additionalCharge['additional_tariff_amount'];
                    $additionalData['additional_charges_at'] = $additionalCharge['additional_charges_at'];
                    $additionalData['gate_id'] = $additionalCharge['gate_id'];
                    $transaction_additional_charges = insert($conn, 'sps_transaction_additional_charges', $additionalData);
                    mysqli_query($conn, $transaction_additional_charges);
                    $logString .= $transaction_additional_charges . "\n" . PHP_EOL;
                    $additionalData = array();
                }
            }

            if (!is_null($discounts)) {
                foreach ($discounts as $discount) {
                    $discountData = array();
                    $discountData['transaction_id'] = $inData['transaction_id'];
                    $discountData['cps_id'] = $transactionId; //This is cps_id from sps_cps table last record inserted
                    $discountData['discount_type'] = $discount['discount_type'];
                    $discountData['discount_type_id'] = $discount['discount_type_id'];
                    $discountData['discount_value_type'] = $discount['discount_value_type'];
                    $discountData['discount_value'] = $discount['discount_value'];
                    $discountData['discount_amount'] = $discount['discount_amount'];
                    $discountData['gate_id'] = $discount['gate_id'];
                    $discountData['discount_at'] = $discount['discount_at'];
                    $sps_transaction_discount = insert($conn, 'sps_transaction_discount', $discountData);
                    mysqli_query($conn, $sps_transaction_discount);
                    $logString .= $sps_transaction_discount . "\n" . PHP_EOL;
                    $discount_type_id = $discount['discount_type_id'];
                    if ($discountData['discount_type'] == 1 || $discountData['discount_type'] == 2) {
                        $increaseCountVoucherEntry = "update sps_vouchers set used_count = used_count + 1 where status='1' AND deleted_at IS NULL AND id='$discount_type_id'";
                        $logString .= $increaseCountVoucherEntry . "\n" . PHP_EOL;
                        mysqli_query($conn, $increaseCountVoucherEntry);
                    }

                }
            }

            if (!is_null($cpsGateDiscounts)) {
                foreach ($cpsGateDiscounts as $discount) {
                    $discountData = array();
                    $discountData['transaction_id'] = $inData['transaction_id'];
                    $discountData['cps_id'] = $transactionId; //This is cps_id from sps_cps table last record inserted
                    $discountData['discount_type'] = $discount['discount_type'];
                    $discountData['discount_type_id'] = $discount['discount_type_id'];
                    $discountData['discount_value_type'] = $discount['discount_value_type'];
                    $discountData['discount_value'] = $discount['discount_value'];
                    $discountData['discount_amount'] = $discount['discount_amount'];
                    $discountData['gate_id'] = $discount['gate_id'];
                    $discountData['discount_at'] = $discount['discount_at'];
                    $sps_transaction_discount = insert($conn, 'sps_transaction_discount', $discountData);
                    mysqli_query($conn, $sps_transaction_discount);
                    $logString .= $sps_transaction_discount . "\n" . PHP_EOL;
                }
            }

            if (!is_null($taxBreakup)) {
                foreach ($taxBreakup as $tax) {
                    $taxData = array();
                    $taxData['tax_id'] = $tax['tax_id'];
                    $taxData['tax_amount'] = $tax['tax_value'];
                    $taxData['module'] = $tax['module'];
                    $taxData['module_id'] = $transactionId;
                    $taxData['location_id'] = $inData['location_id'];
                    $taxData['area_id'] = $inData['area_id'];
                    $transaction_tac_breakup = insert($conn, 'sps_tax_breakup', $taxData);
                    mysqli_query($conn, $transaction_tac_breakup);
                    $logString .= $transaction_tac_breakup . "\n" . PHP_EOL;
                }
            }
        }

        //Added changes for balance update from mobile
        if (isset($_REQUEST['cps_type']) && isset($_REQUEST['cps_type_id']) && $_REQUEST['cps_type'] == 2 && strlen($_REQUEST['cps_type_id']) == 16) {
            $q = "SELECT sm.membership_id FROM sps_membership AS sm WHERE CONCAT( sm.card_no_prefix, sm.card_no ) =  '" . $_REQUEST['cps_type_id'] . "' AND (sm.balance_updated_at IS NULL OR sm.balance_updated_at < '" . $_REQUEST['cps_out_time'] . "') AND sm.deleted_at IS NULL";
            $logString .= $q . "\n" . PHP_EOL;
            $result = mysqli_query($conn, $q);
            $rowcount = mysqli_num_rows($result);
            $sps_transaction_id = $inData['transaction_id'];
            $sps_cps_id = $transactionId;
            $card_no = $_REQUEST['cps_type_id'];
            if ($result) {
                $membership = mysqli_fetch_assoc($result);
                $update_balance = "UPDATE sps_membership SET balance_updated_at='" . $_REQUEST['cps_out_time'] . "',balance='" . $_REQUEST['remaining_balance'] . "' WHERE membership_id= '" . $membership['membership_id'] . "'";
                mysqli_query($conn, $update_balance);
                $logString .= $update_balance . "\n" . PHP_EOL;

                //inserting balance update in sps_membership_bal_noti table
                $queryInsertBalanceUpdate = "insert into sps_membership_bal_noti(transaction_id,cps_id,card_no,transaction_at,is_notify)values('$sps_transaction_id','$sps_cps_id','$card_no','CPS','1')";
                mysqli_query($conn, $queryInsertBalanceUpdate);
                $logString .= $queryInsertBalanceUpdate . "\n" . PHP_EOL;

                $lastInsertId = mysqli_insert_id($conn);
                balance_deduction($lastInsertId, $baseUrl);
            }
        }

        if ($inData['transaction_id'] != 0) {
            $tranasctionData = array();
            //$in_time = trim($_REQUEST['in_time']);
            $entry_time = trim($_REQUEST['entry_time']);
            //$manuel_ticket = trim($_REQUEST['manuel_ticket']);
            $transactionId = $inData['transaction_id'];
            $updatetransactions = "UPDATE sps_transactions SET  in_time = '" . $entry_time . "',vehicle_type ='" . $inData['cps_vehicle_type'] . "',is_cps = 1,vehicle_number ='" . $inData['cps_vehicle_number'] . "'";
            if ($gateId) {
                $updatetransactions .= ",in_gate=$gateId";
            }
            $updatetransactions .= ",cps_process_time='" . date("Y-m-d H:i:s") . "',location_id='" . $inData['location_id'] . "',area_id='" . $inData['area_id'] . "' WHERE id= '$transactionId' AND deleted_at IS NULL";
            mysqli_query($conn, $updatetransactions);
            $logString .= $updatetransactions . "\n" . PHP_EOL;
        }

        if (!is_null($transactionId)) {
            $response_data = json_encode(array('err_code' => '0', 'data' => 'Success')); // changes successe msg here
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            $response_data = json_encode(array('err_code' => '10034', 'err_msg' => $errors['10034']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        createLog($logString);
        break;
    /* end */

    case 'transaction_by_barcode':
        $barcode = trim($_REQUEST['barcode']);
        $check_query = "select out_time,deleted_at from sps_transactions where barcode = '$barcode'";
        $check_query .= "  ORDER BY id DESC limit 1";
        $logString .= $check_query . "\n" . PHP_EOL;
        $check_row = mysqli_query($conn, $check_query);
        $check_transaction = mysqli_fetch_assoc($check_row);
        if (mysqli_num_rows($check_row) <= 0) {
            $response = array('result' => 'NotProcess');
            $response['out_time'] = '';
            $response['deleted_at'] = '';
        } elseif ($check_transaction['out_time'] != '0000-00-00 00:00:00') {
            $response = array('result' => 'Process');
            $response['out_time'] = $check_transaction['out_time'];
            $response['deleted_at'] = $check_transaction['deleted_at'];
        } elseif (is_null($check_transaction['deleted_at']) && $check_transaction['out_time'] == '0000-00-00 00:00:00') {
            $response = array('result' => 'NotProcess');
            $response['out_time'] = $check_transaction['out_time'];
            $response['deleted_at'] = $check_transaction['deleted_at'];
        } elseif (!is_null($check_transaction['deleted_at'])) {
            $response = array('result' => 'Process');
            $response['out_time'] = $check_transaction['out_time'];
            $response['deleted_at'] = $check_transaction['deleted_at'];
        }

        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit;

        break;
    case 'transaction_by_rfid':
        $cardno = trim($_REQUEST['cardno']);
        $check_query = "select id from sps_transactions where (out_time = '0000-00-00 00:00:00' OR out_time IS NULL) AND deleted_at IS NULL AND sps_transactions.in_type = 2 AND in_type_id = '$cardno'";
        $check_query .= "  ORDER BY id DESC limit 1";
        $logString .= $check_query . "\n" . PHP_EOL;
        $check_row = mysqli_query($conn, $check_query);
        $check_transaction = mysqli_fetch_assoc($check_row);
        if (mysqli_num_rows($check_row) <= 0) {
            $response = array('result' => 'NOTFOUND');

        } else {
            $response = array('result' => 'FOUND');
        }

        $response_data = json_encode(array('err_code' => '0', 'data' => $response));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit;

        break;


    case 'feight_count':
        $shift_id = trim($_REQUEST['shift_id']);
        $query = "select end_shift_status,f_eight_used,f_eight_granted from sps_shift_management where sr_no = '$shift_id' AND deleted_at IS NULL LIMIT 1";
        $logString .= $query . "\n" . PHP_EOL;
        $check_row = mysqli_query($conn, $query);
        $f_eigth_row = mysqli_fetch_assoc($check_row);
        if (mysqli_num_rows($check_row) <= 0) {
            $response_data = json_encode(array('err_code' => '10013', 'err_msg' => $errors['10013']));
            echo $response_data;
            $logString .= $response_data . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        } else {
            if ($f_eigth_row['end_shift_status'] == '1') {
                $response_data = json_encode(array('err_code' => '10038', 'err_msg' => $errors['10038']));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit();
            } else {
                $response = array('f_eight_used' => $f_eigth_row['f_eight_used'], 'f_eight_granted' => $f_eigth_row['f_eight_granted']);
                $response_data = json_encode(array('err_code' => '0', 'data' => $response));
                echo $response_data;
                $logString .= $response_data . "\n" . PHP_EOL;
                createLog($logString);
                exit;
            }
        }
        break;
    /* US793: Dynamic UPI QR Code API */
    case 'validatePaymentRefNumber':
        $paymentRefNumber = trim($_REQUEST['payment_reference_number']);
        $isValid = isValidPaymentReferenceNumber($conn, $errors, $logString, $paymentRefNumber);
        if ($isValid) {
            /* $responseData = array();
            $responseData['msg'] = 'Validation successful!';
            $responseData['payment_reference_number'] = $paymentRefNumber;
            $response = json_encode(array('err_code' => '0', 'data' => $responseData)); */
            $response = json_encode(array('err_code' => '0', 'err_msg' => 'Validation successful!'));
            echo $response;
            $logString .= $response . "\n" . PHP_EOL;
            createLog($logString);
            exit();
        }
        break;
    /* US793: Dynamic UPI QR Code API */

    case 'sonu':   
        $response = json_encode(array('err_code' => '2201', 'err_msg' => 'Validation successful!'));
            echo $response;
            $logString .= $response . "\n" . PHP_EOL;
            createLog($logString);
            exit(); 

    case '':
        createLog($logString);
        break;

endswitch;
/*End Request command switch*/
fclose($fp);
/**
 * @param $table
 * @param $array
 * @return bool|int
 */
function insert($con, $table, $array)
{
    $query = "INSERT INTO " . $table;
    $fis = array();
    $vas = array();
    foreach ($array as $field => $val) {
        $fis[] = "`$field`"; //you must verify keys of array outside of function;
        //unknown keys will cause mysql errors;
        //there is also sql injection risc;
        $vas[] = "'" . mysqli_real_escape_string($con, $val) . "'";
    }
    $query .= " (" . implode(", ", $fis) . ") VALUES (" . implode(", ", $vas) . ")";
    return $query;
}

function update($con, $tablename, $valueSets = array(), $conditionSets = array())
{

    foreach ($valueSets as $key => $value) {
        $valueSets[] = $key . " = '" . mysqli_real_escape_string($con, $value) . "'";
    }

    foreach ($conditionSets as $key => $value) {
        $conditionSets[] = $key . " = '" . $value . "'";
    }

    $query = "UPDATE $tablename SET " . join(",", $valueSets) . " WHERE " . join(" AND ", $conditionSets);

    return $query;
}

function check_member_vehicles($conn, $member_id, $membership_id)
{
    $query_v = "SELECT sv.vehicle_no
                              FROM sps_member_vehicles AS sv
                              LEFT JOIN sps_members AS sm ON sm.member_id = sv.member_id
                              WHERE sv.status = 1 AND sv.deleted_at IS NULL";
    if (isset($member_id) & ($member_id != ''))
        $query_v .= " AND sv.member_id = " . $member_id . "";
    if (isset($membership_id) & ($membership_id != ''))
        $query_v .= " AND sv.membership_id = " . $membership_id . "";
//    echo $query_v ;
    $result_v = mysqli_query($conn, $query_v);

    while ($row = mysqli_fetch_assoc($result_v)) {
        $arr[] = $row;
    }
    return $arr;
}

function flat_array($array, $key)
{
    $flat_array = [];
    foreach ($array as $item) {
        $flat_array[$item[$key]] = $item;
    }
    return $flat_array;
}

function check_login($conn, $errors)
{
    $username = trim($_REQUEST['username']);
    $password = trim(md5($_REQUEST['password']));
    /*---- Query to fetch user id against the username ----*/
    $user_id_db = mysqli_fetch_assoc(mysqli_query($conn, "SELECT sps_users.user_id,sps_users.user_role,sps_users.user_name,sps_user_role.role,CONCAT( sps_users.first_name,  ' ', sps_users.last_name ) as name  FROM sps_users LEFT JOIN sps_user_role ON sps_user_role.id = sps_users.user_role  WHERE sps_users.password = '$password' AND sps_users.user_name = '$username' AND sps_users.deleted_at is NULL"));
    if (is_null($user_id_db)) {
        echo json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
        exit();
    }
    if ($user_id_db['user_role'] == 1 || $user_id_db['role'] == 'Auditor') {
        return $user_id_db;
        exit();
    } else {
        echo json_encode(array('err_code' => '10029', 'err_msg' => $errors['10029']));
        exit();
    }
}

function get_result_array($conn, $query)
{
    $assoc_array = [];
    if ($result = mysqli_query($conn, $query)) {
        /* fetch associative array */
        while ($row = mysqli_fetch_assoc($result)) {
            $assoc_array[] = $row;
        }
        mysqli_free_result($result);
    }
    return $assoc_array;
}


function createLog($logString)
{
    $log = "-----------" . date('Y-m-d H:i:s') . "--------------" . PHP_EOL .
        $logString . PHP_EOL .
        "-------------------------" . PHP_EOL;
    if (!file_exists('./mobile_log')) {
        mkdir('./mobile_log', 0777, true);
    }
    file_put_contents('./mobile_log/log_' . date("j.n.Y") . '.txt', $log, FILE_APPEND);
}

function balance_deduction($id, $baseUrl)
{

    $url = $baseUrl . 'background_processes.php';
    $curl = curl_init();
    $post['cmd'] = 'balDeduction'; // our data todo in received
    $post['id'] = $id;
    curl_setopt($curl, CURLOPT_URL, $url);
    curl_setopt($curl, CURLOPT_POST, TRUE);
    curl_setopt($curl, CURLOPT_POSTFIELDS, $post);
    curl_setopt($curl, CURLOPT_USERAGENT, 'api');
    curl_setopt($curl, CURLOPT_TIMEOUT, 1);
    curl_setopt($curl, CURLOPT_HEADER, 0);
    curl_setopt($curl, CURLOPT_RETURNTRANSFER, false);
    curl_setopt($curl, CURLOPT_FORBID_REUSE, true);
    curl_setopt($curl, CURLOPT_CONNECTTIMEOUT, 1);
    curl_setopt($curl, CURLOPT_DNS_CACHE_TIMEOUT, 10);
    curl_setopt($curl, CURLOPT_FRESH_CONNECT, true);
    curl_exec($curl);
    curl_close($curl);
}

/**
 * US793: Dynamic UPI QR Code API
 * To validate payment reference number
 *
 * @param string $payment_reference_number
 *
 * @return boolean
 */
function isValidPaymentReferenceNumber($conn, $errors, &$logString, $payment_reference_number)
{
    $response_data = null;

    $query = "SELECT id FROM sps_transactions WHERE in_payment_referance_number = '$payment_reference_number' OR out_payment_reference_number = '$payment_reference_number'";
    $result = mysqli_query($conn, $query);
    $logString .= $query . "\n" . PHP_EOL;
    $transaction = mysqli_fetch_assoc($result);

    if (!is_null($transaction)) {
        $response_data = json_encode(array('err_code' => '10039', 'err_msg' => $errors['10039']));
    } else {
        $query = "SELECT cps_id FROM sps_cps WHERE cps_payment_reference_number = '$payment_reference_number'";
        $result = mysqli_query($conn, $query);
        $logString .= $query . "\n" . PHP_EOL;
        $transaction = mysqli_fetch_assoc($result);
        if (!is_null($transaction)) {
            $response_data = json_encode(array('err_code' => '10039', 'err_msg' => $errors['10039']));
        }
    }

    if (!empty($response_data)) {
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }

    return true;
}

function createStickerMembershipData($baseUrl, $logString, $errors)
{
    try {
        $response = curlCall($baseUrl . "/socket/sticker_membership/create", [], "GET");

        if ($response['status']) {
            $response['message'] = 'Sticker Membership Data received successfully';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            $logString .= $response_data . "\n" . PHP_EOL;
            echo $response_data;
            createLog($logString);
            exit();
        }

        throw new Exception($response['message']);

    } catch (Exception $e) {

        $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }
}

function getStickerMemberTariffDate($baseUrl, $logString, $errors)
{
    try {
        $tariffId = $_REQUEST['tariff_id'];
        $tariffCount = $_REQUEST['count'];
        $response = curlCall($baseUrl . "/socket/sticker_membership/tariffdata/" . $tariffId, ['count' => $tariffCount], "GET");
        if ($response['status']) {
            $response['message'] = 'Sticker Tariff Data Listed Successfully';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            $logString .= $response_data . "\n" . PHP_EOL;
            echo $response_data;
            createLog($logString);
            exit();
        }

        throw new Exception($response['message']);

    } catch (Exception $e) {

        $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }
}

function getStickerMemberCalculateDate($baseUrl, $logString, $errors)
{
    try {
        if (isset($_REQUEST['calenderProduct'])) {
            $calenderProduct = $_REQUEST['calenderProduct'];
            $useCurrentDate = $_REQUEST['useCurrentDate'];
            $periodType = $_REQUEST['periodType'];
            $periodLength = $_REQUEST['periodLength'];
            $tolerance = $_REQUEST['tolerance'];
            $requestData = [
                'calenderProduct' => $calenderProduct,
                'useCurrentDate' => $useCurrentDate,
                'periodType' => $periodType,
                'periodLength' => $periodLength,
                'tolerance' => $tolerance,
            ];

        } else {
            $startDate = $_REQUEST['startDate'];
            $periodType = $_REQUEST['periodType'];
            $periodLength = $_REQUEST['periodLength'];
            $tolerance = $_REQUEST['tolerance'];
            $requestData = [
                'startDate' => $startDate,
                'periodType' => $periodType,
                'periodLength' => $periodLength,
                'tolerance' => $tolerance,
            ];
        }

        $response = curlCall($baseUrl . "/socket/sticker_membership/calculate_date", $requestData, "POST");
        if ($response['status']) {
            $response['message'] = 'Sticker Tariff Calculated Date Listed Successfully';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            $logString .= $response_data . "\n" . PHP_EOL;
            echo $response_data;
            createLog($logString);
            exit();
        }

        throw new Exception($response['message']);

    } catch (Exception $e) {

        $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }
}

function getStickerMemberFetchFastagId($baseUrl, $logString, $errors)
{
    try {
        $vehicleNo = $_REQUEST['vehicleNo'];

        $response = curlCall($baseUrl . "/socket/sticker_membership/fetch_fastag_id/" . $vehicleNo, [], "GET");

        if ($response['status']) {
            $response['message'] = 'Sticker Membership Fetch FASTag ID Successfully';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            $logString .= $response_data . "\n" . PHP_EOL;
            echo $response_data;
            createLog($logString);
            exit();
        }

        throw new Exception($response['message']);

    } catch (Exception $e) {

        $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $e->getMessage()));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }
}

function getStickerMemberFetchVehicleNo($baseUrl, $logString, $errors)
{
    try {
        $fastagId = $_REQUEST['fastagId'];

        $response = curlCall($baseUrl . "/socket/sticker_membership/fetch_vehicle_no/" . $fastagId, [], "GET");

        if ($response['status']) {
            $response['message'] = 'Sticker Membership Fetch Vehicle No Successfully';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            $logString .= $response_data . "\n" . PHP_EOL;
            echo $response_data;
            createLog($logString);
            exit();
        }
        throw new Exception($response['message']);

    } catch (Exception $e) {

        $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $errors['10006']));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }
}

function storeStickerMembershipData($baseUrl, $logString, $errors)
{
    /*---- Query to fetch password against username from sps_users ----*/

    try {
        $requestData = [
            "member_id" => $_REQUEST['member_id'],
            "area_id" => $_REQUEST['area_id'],
            "location_id" => $_REQUEST['location_id'],
            "membership_product" => $_REQUEST['membership_product'],
            "hidden_sticker_membership_product_id" => $_REQUEST['hidden_sticker_membership_product_id'],
            "tariff_name" => $_REQUEST['tariff_name'],
            "tolerance" => $_REQUEST['tolerance'],
            "tariff" => $_REQUEST['tariff'],
            "vehicle_charge" => $_REQUEST['vehicle_charge'],
            "new_card_charge" => $_REQUEST['new_card_charge'],
            "extend_charge" => $_REQUEST['extend_charge'],
            "vehicle_change_charge" => $_REQUEST['vehicle_change_charge'],
            "from_time" => $_REQUEST['from_time'],
            "to_time" => $_REQUEST['to_time'],
            "duration" => $_REQUEST['duration'],
            "tariff_charge" => $_REQUEST['tariff_charge'],
            "payment_mode" => $_REQUEST['payment_mode'],
            "membership_no" => $_REQUEST['membership_no'],
            "status" => $_REQUEST['status'],
            "membership_type" => $_REQUEST['membership_type'],
            "reciept_hidden" => $_REQUEST['reciept_hidden'],
            "period_to" => $_REQUEST['period_to'],
            "period_from" => $_REQUEST['period_from'],
            "card_deposite_fee" => $_REQUEST['card_deposite_fee'],
            "vehicle_no" => json_encode($_REQUEST['vehicle_no']),
            "fastag_id" => json_encode($_REQUEST['fastag_id']),
            "vehicle_priority" => json_encode($_REQUEST['vehicle_priority']),
            "created_by" => $_REQUEST['created_by']
        ];

        $response = curlCall($baseUrl . "/socket/sticker_membership", $requestData, "POST");

        if ($response['status']) {
            $response['message'] = 'Sticker Membership Store successfully';
            $response_data = json_encode(array('err_code' => '0', 'data' => $response));
            $logString .= $response_data . "\n" . PHP_EOL;
            echo $response_data;
            createLog($logString);
            exit();
        }

        throw new Exception($response['message']);

    } catch (Exception $e) {

        $response_data = json_encode(array('err_code' => '10006', 'err_msg' => $e->getMessage()));
        echo $response_data;
        $logString .= $response_data . "\n" . PHP_EOL;
        createLog($logString);
        exit();
    }
}


function curlCall($apiURL, $requestParamList, $method = "POST")
{
    try {
        $ch = curl_init($apiURL);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $requestParamList);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        //curl_setopt($ch, CURLOPT_HTTPHEADER, $customHeader);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30);
        curl_setopt($ch, CURLOPT_TIMEOUT, 60);
        curl_setopt($ch, CURLOPT_VERBOSE, true);

        $streamVerboseHandle = fopen('php://temp', 'w+');
        curl_setopt($ch, CURLOPT_STDERR, $streamVerboseHandle);
        $jsonResponse = curl_exec($ch);

        $err = curl_error($ch);  //if you need
        return json_decode($jsonResponse, true);
    } catch (Exception $e) {
        echo $e->getMessage();
        return false;
    }
}