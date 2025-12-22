<?php
require_once __DIR__ . '/lib/fpdf.php';
define('PDF_PRICE',5); // ₹49

session_start();

$downloadType = $_POST['download_type'] ?? 'free';

// default
$showWatermark = true;

// if paid button clicked
if ($downloadType === 'paid' && empty($_SESSION['paid_user'])) {
    $_SESSION['pdf_form_data'] = $_POST;
    header("Location: payment/pay.php");
    exit;
}

// disable watermark if paid
if (!empty($_SESSION['paid_user']) && $_SESSION['paid_user'] === true) {
    $showWatermark = false;
}

/* ---------- JSON BEAUTIFIER ---------- */
function beautifyJson(string $text): string
{
    $decoded = json_decode($text, true);
    if (json_last_error() === JSON_ERROR_NONE) {
        return json_encode($decoded, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    }
    return $text;
}

/* ---------- PDF CLASS ---------- */
class APIDocPDF extends FPDF
{
    public $showWatermark = true;
    protected $angle = 0;

    function Header()
    {
        // if ($this->showWatermark) {
        //     $this->addWatermark();
        // }
        if (empty($_SESSION['paid_user'])) {
            $this->addWatermark();
        }
    }

    function Footer()
    {
        // ONLY page number (keep it clean)
        $this->SetY(-15);
        $this->SetFont('Arial','I',9);
        $this->SetX(-30);
        $this->Cell(30,10,'Page '.$this->PageNo(),0,0,'R');
    }

    /* ---------- WATERMARK ---------- */
    function addWatermark()
    {
        $text = 'Created by CodingWithSonu.com';

        $this->SetFont('Arial','B',30);
        $this->SetTextColor(235,235,235);

        $pageW = $this->GetPageWidth();
        $pageH = $this->GetPageHeight();

        // ADJUST HERE
        $x = $pageW / 2;
        $y = ($pageH / 2) + 70; // ⬅ move watermark DOWN

        $textWidth = $this->GetStringWidth($text);

        // OPTIONAL GUIDE LINES (remove later)
        /*
        $this->SetDrawColor(200,200,200);
        $this->Line(10, $pageH/2, $pageW-10, $pageH/2);
        $this->Line($pageW/2, 10, $pageW/2, $pageH-10);
        */

        $this->Rotate(45, $x, $y);
        $this->Text($x - ($textWidth / 2), $y, $text);
        $this->Rotate(0);

        $this->SetTextColor(0,0,0);
    }


    /* ---------- ROTATE SUPPORT ---------- */
    function Rotate($angle, $x = -1, $y = -1)
    {
        if ($x == -1) $x = $this->x;
        if ($y == -1) $y = $this->y;

        if ($this->angle != 0) {
            $this->_out('Q');
        }

        $this->angle = $angle;

        if ($angle != 0) {
            $angle *= M_PI / 180;
            $c = cos($angle);
            $s = sin($angle);
            $cx = $x * $this->k;
            $cy = ($this->h - $y) * $this->k;

            $this->_out(sprintf(
                'q %.5F %.5F %.5F %.5F %.5F %.5F cm',
                $c, $s, -$s, $c, $cx, $cy
            ));
        }
    }

    function _endpage()
    {
        if ($this->angle != 0) {
            $this->angle = 0;
            $this->_out('Q');
        }
        parent::_endpage();
    }

    /* ---------- CONTENTS LINE ---------- */
    function contentsLine($title, $pageNo, $link=null)
    {
        $this->SetFont('Arial','',12);
        $usable = $this->GetPageWidth() - $this->lMargin - $this->rMargin;
        $tw = $this->GetStringWidth($title);
        $pw = $this->GetStringWidth($pageNo);
        $dotsW = $usable - $tw - $pw - 6;
        $dots = str_repeat('.', max(0, floor($dotsW / $this->GetStringWidth('.'))));

        $this->Cell($tw+2,8,$title,0,0,'L',false,$link);
        $this->Cell($dotsW,8,$dots,0,0);
        $this->Cell($pw+4,8,$pageNo,0,1,'R');
    }
}

/* ---------- FORM SUBMIT ---------- */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {

    $docTitle    = trim($_POST['doc_title']);
    $docSubtitle = trim($_POST['doc_subtitle']);

    $names   = $_POST['api_name'] ?? [];
    $methods = $_POST['api_method'] ?? [];
    $urls    = $_POST['api_url'] ?? [];
    $reqs    = $_POST['api_request'] ?? [];
    $resps   = $_POST['api_response'] ?? [];

    $reqParams    = $_POST['req_param'] ?? [];
    $reqDescs     = $_POST['req_desc'] ?? [];
    $reqMandatory = $_POST['req_mandatory'] ?? [];
    $reqTypes     = $_POST['req_type'] ?? [];
    $reqLengths   = $_POST['req_length'] ?? [];

    $pdf = new APIDocPDF();
    $pdf->showWatermark = $showWatermark;

    /* ---------- COVER PAGE ---------- */
    $pdf->AddPage();

    // IMPORTANT: set color JUST before printing title
    $pdf->SetTextColor(31,79,216); // Blue
    $pdf->SetFont('Arial','B',24);
    $pdf->Ln(60);
    $pdf->Cell(0,15,$docTitle,0,1,'C');

    // Reset color immediately after title
    $pdf->SetTextColor(0,0,0);

    if ($docSubtitle) {
        $pdf->Ln(8);
        $pdf->SetFont('Arial','',13);
        $pdf->MultiCell(0,8,$docSubtitle,0,'C');
    }


    /* ---------- CONTENTS ---------- */
    $pdf->AddPage();
    $pdf->SetFillColor(240,127,46);
    $pdf->SetTextColor(255,255,255);
    $pdf->SetFont('Arial','B',18);
    $pdf->Cell(0,14,'  Contents',0,1,'L',true);
    $pdf->Ln(6);
    $pdf->SetTextColor(0,0,0);

    $toc = [];
    $page = 3;

    foreach ($names as $i => $n) {
        if (trim($n)==='') continue;
        $toc[] = ['title'=>$n,'page'=>$page++,'link'=>$pdf->AddLink()];
    }

    foreach ($toc as $t) {
        $pdf->contentsLine($t['title'],$t['page'],$t['link']);
    }

    /* ---------- API PAGES ---------- */
    $idx = 0;
    foreach ($names as $i => $name) {

        if (trim($name)==='') continue;

        $method   = strtoupper($methods[$i] ?? 'GET');
        $url      = trim($urls[$i]);
        $request  = beautifyJson(trim($reqs[$i] ?? ''));
        $response = beautifyJson(trim($resps[$i] ?? ''));

        $pdf->AddPage();
        $pdf->SetLink($toc[$idx]['link']);

        // API Name
        $pdf->SetFont('Arial','B',18);
        $pdf->SetTextColor(31,79,216);
        $pdf->Cell(0,12,$name,0,1);
        $pdf->Ln(4);
        $pdf->SetTextColor(0,0,0);

        // URL
        $pdf->SetFont('Arial','B',12);
        $pdf->Cell(40,8,'URL:',0,0);
        $pdf->SetFont('Arial','',12);
        $pdf->MultiCell(0,8,$url);
        $pdf->Ln(3);

        // Method label
        $pdf->SetFont('Arial','B',12);
        $pdf->Cell(40,8,'Method:',0,0);

        // Badge color
        if ($method === 'GET') {
            $pdf->SetFillColor(46, 204, 113); // green
        } else {
            $pdf->SetFillColor(52, 152, 219); // blue
        }

        // Badge text
        $pdf->SetTextColor(255,255,255);
        $pdf->SetFont('Arial','B',11);

        // Badge box
        $pdf->Cell(25,8,' '.$method.' ',0,1,'C',true);

        // Reset colors
        $pdf->SetTextColor(0,0,0);
        $pdf->Ln(4);

        if (!empty($reqParams[$idx])) {
            $pdf->Ln(4);
            $pdf->SetFont('Arial','B',12);
            $pdf->Cell(0,8,'Request Field Description :',0,1);
            $pdf->Ln(3);

            // Table header
            $pdf->SetFont('Arial','B',10);
            $pdf->SetFillColor(240,127,46);
            $pdf->SetTextColor(255,255,255);

            $pdf->Cell(40,8,'Parameter',1,0,'C',true);
            $pdf->Cell(65,8,'Description',1,0,'C',true);
            $pdf->Cell(30,8,'Mandatory',1,0,'C',true);
            $pdf->Cell(30,8,'Type',1,0,'C',true);
            $pdf->Cell(25,8,'Length / Value',1,1,'C',true);

            $pdf->SetTextColor(0,0,0);
            $pdf->SetFont('Arial','',10);

            foreach ($reqParams[$idx] as $k => $param) {
                $pdf->Cell(40,8,$param,1);
                $pdf->Cell(65,8,$reqDescs[$idx][$k] ?? '',1);
                $pdf->Cell(30,8,$reqMandatory[$idx][$k] ?? '',1);
                $pdf->Cell(30,8,$reqTypes[$idx][$k] ?? '',1);
                $pdf->Cell(25,8,$reqLengths[$idx][$k] ?? '',1,1);

            }
        }

        if ($method === 'POST' && $request !== '') {
            $pdf->SetFont('Arial','B',12);
            $pdf->Cell(0,8,'Request',0,1);
            $pdf->SetFont('Courier','',10);
            $pdf->MultiCell(0,6,$request);
            $pdf->Ln(4);
        }

        $pdf->SetFont('Arial','B',12);
        $pdf->Cell(0,8,'Response',0,1);
        $pdf->SetFont('Courier','',10);
        $pdf->MultiCell(0,6,$response);

        $idx++;
    }

        /* ---------- ERROR CODES PAGE ---------- */
    $errorCodes = $_POST['error_code'] ?? [];
    $errorDescs = $_POST['error_desc'] ?? [];

    if (!empty(array_filter($errorCodes))) {

        $pdf->AddPage();

        // Header bar
        $pdf->SetFillColor(240,127,46);
        $pdf->SetTextColor(255,255,255);
        $pdf->SetFont('Arial','B',18);
        $pdf->Cell(0,14,'  Error Codes',0,1,'L',true);
        $pdf->Ln(6);

        $pdf->SetTextColor(0,0,0);
        $pdf->SetFont('Arial','B',14);
        $pdf->Cell(0,10,'List of Error Code',0,1);
        $pdf->Ln(3);

        // Table Header
        $pdf->SetFont('Arial','B',12);
        $pdf->SetFillColor(245,245,245);
        $pdf->Cell(40,10,'Error Code',1,0,'C',true);
        $pdf->Cell(0,10,'Error Description',1,1,'C',true);

        // Table Rows
        $pdf->SetFont('Arial','',12);

        foreach ($errorCodes as $i => $code) {
            if (trim($code) === '') continue;

            $desc = trim($errorDescs[$i] ?? '');

            $pdf->Cell(40,10,$code,1,0,'C');
            $pdf->Cell(0,10,$desc,1,1,'L');
        }
    }

    $pdf->Output('D','api-documentation.pdf');
    unset($_SESSION['pdf_form_data']);
    exit;
}
?>


<!DOCTYPE html>
<html>
<head>
<!-- Primary Meta Tags -->
<title>API Documentation Generator Online | Create API Docs PDF - CodingWithSonu</title>

<meta name="description" content="Create professional API documentation PDF online. Add API name, URL, GET/POST method, request & response JSON, error codes and export clean API docs instantly. Free API documentation generator by CodingWithSonu.">

<meta name="keywords" content="API documentation generator, API doc pdf, create api docs online, API documentation tool, REST API documentation, API request response documentation, CodingWithSonu tools">

<meta name="author" content="CodingWithSonu">
<meta name="robots" content="index, follow">

<!-- Open Graph / Facebook -->
<meta property="og:type" content="website">
<meta property="og:title" content="API Documentation Generator | CodingWithSonu">
<meta property="og:description" content="Generate clean and professional API documentation PDFs with request, response, GET/POST method and error codes.">
<meta property="og:url" content="https://codingwithsonu.com/create-api-doc.php">
<meta property="og:site_name" content="CodingWithSonu">
<meta property="og:image" content="https://codingwithsonu.com/assets/images/api-doc-generator.png">

<!-- Twitter Card -->
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="API Documentation Generator Online - CodingWithSonu">
<meta name="twitter:description" content="Create API documentation PDF easily with method, request, response & error codes.">
<meta name="twitter:image" content="https://codingwithsonu.com/assets/images/api-doc-generator.png">

<!-- Canonical -->
<link rel="canonical" href="https://codingwithsonu.com/create-api-doc.php">

<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">

<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap" rel="stylesheet">
<link rel="stylesheet" type="text/css" href="asset/css/style.css">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.6.3/css/font-awesome.min.css">
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>

<style>
body{background:#eef2f7;padding:30px;font-family:Arial}
.box{background:#fff;padding:30px;border-radius:14px;max-width:950px;margin:auto}
.api-card{border:1px solid #e3e6ea;padding:20px;border-radius:12px;margin-bottom:20px;background:#fafbfc;position:relative}
.remove-btn{
    position:absolute;top:10px;right:10px;
    background:#dc3545;color:#fff;border:none;
    border-radius:50%;width:28px;height:28px;
    font-weight:bold;cursor:pointer
}
textarea{font-family:monospace;min-height:120px}
.add-btn{background:#28a745;color:#fff;padding:12px 18px;border-radius:8px;border:none}
</style>
    <?php include_once('google-analytics.php'); ?>
    <?php include_once('ads.php'); ?>
    <?php include_once('track-visitor.php');?>
</head>

<body>
<nav class="navbar navbar-inverse navbar-fixed-top top-navbar">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#myNavbar"> <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button> <a class="navbar-brand my-navbar-brand" href="../">Coding With Sonu</a>
        </div>
        <div class="collapse navbar-collapse" id="myNavbar">
            <ul class="nav navbar-nav mynav">
            <li><a href="html/html-home.php">HTML</a></li>
            <li><a href="css/css-home.php">CSS</a></li>
            <li><a href="javascript/javascript-home.php">Javascript</a></li>
            <li><a href="jquery/jquery-home.php">Jquery</a></li>
            <li><a href="php/php-home.php">PHP</a></li>
            <li><a href="java/java-home.php">JAVA</a></li>
            <li><a href="python/python-home.php">Python</a></li>
            <li><a href="tools.php">Tools</a></li>
            </ul>
        </div>
    </div>
</nav>

<div class="box">
<h2 class="text-center">API Documentation Generator</h2>

<form method="POST">

<label>Document Title</label>
<input type="text" name="doc_title" class="form-control" required>

<label style="margin-top:10px;">Sub Heading / Description</label>
<textarea name="doc_subtitle" class="form-control"></textarea>

<hr>

<div style="display:flex;gap:10px;margin-bottom:15px;">
    <button type="button" id="btnImport" class="btn btn-warning" onclick="switchMode('import')"> Import Collection</button>

    <button type="button" id="btnManual" class="btn btn-primary" onclick="switchMode('manual')"> Manual Entry </button>
</div>

<div id="apiContainer">


<div id="importSection">
    <h3 style="color:#f07f2e;">Import API Collection</h3>

    <label><b>Upload Postman / API Collection (.json)</b></label>
    <input type="file"
           id="collection_file"
           accept=".json"
           class="form-control"
           onchange="loadCollectionFile(this)">

    <p style="margin:10px 0;text-align:center;color:#999">OR</p>

    <label><b>Paste Collection JSON</b></label>
    <textarea id="collection_json"
              class="form-control"
              placeholder="Paste Postman / API collection JSON here"
              style="min-height:200px"></textarea>

    <button type="button"
            class="add-btn"
            style="margin-top:10px"
            onclick="importCollection()">Import Collection 
    </button>

    <hr>
</div>


<!-- DEFAULT API (NON-REMOVABLE) -->
<div id="manualSection">
<div class="api-card">
<label>Method</label>
<select name="api_method[]" class="form-control" onchange="toggleRequest(this)">
    <option value="GET">GET</option>
    <option value="POST">POST</option>
</select>

<label>API Name</label>
<input type="text" name="api_name[]" class="form-control" required>

<label>API URL</label>
<input type="text" name="api_url[]" class="form-control" required>

<label>Request (JSON)</label>
<textarea name="api_request[]" class="form-control request-box" disabled
placeholder="Not required for GET" onblur="generateRequestTable(this)"></textarea>

<h4 class="req-table-title" style="display:none;color:#f07f2e;">
    JSON Request Field Description
</h4>

<table class="table table-bordered req-table" style="display:none;">
    <thead style="background:#f07f2e;color:#fff;">
        <tr>
            <th>Parameter</th>
            <th>Description</th>
            <th>Mandatory / Optional</th>
            <th>Data Type</th>
            <th>Length / Value</th>
        </tr>
    </thead>
    <tbody></tbody>
</table>

<label>Response (JSON)</label>
<textarea name="api_response[]" class="form-control"></textarea>

<input type="hidden" name="download_type" id="download_type" value="free">
</div>

</div>

<button type="button" class="add-btn" onclick="addApi()">+ Add API</button>
<br><br>
</div>
<hr>

<h3 style="color:#f07f2e;">Error Codes</h3>

<div id="errorContainer">

    <div class="api-card">
        <label>Error Code</label>
        <input type="text" name="error_code[]" class="form-control" placeholder="e.g. 200">

        <label>Error Description</label>
        <input type="text" name="error_desc[]" class="form-control" placeholder="e.g. SUCCESSFUL">
    </div>

</div>

<button type="button" class="add-btn" onclick="addError()" style="margin-bottom:10px;">+ Add Error Code</button>
<div class="row">
    <div class="col-md-6">
        <button type="submit"
                class="btn btn-default btn-lg btn-block"
                onclick="setDownloadType('free')">
            Download Free PDF (With Watermark)
        </button>
    </div>

    <div class="col-md-6">
        <button type="submit"
                class="btn btn-success btn-lg btn-block"
                onclick="setDownloadType('paid')">
             Download Without Watermark - ₹<?php echo PDF_PRICE; ?>
        </button>
    </div>
</div>

</form>
</div>

<script>
// default mode (manual)
switchMode('manual');
function addApi(){
    document.getElementById('apiContainer').insertAdjacentHTML('beforeend',`
    <div class="api-card">
        <button type="button" class="remove-btn"
                onclick="this.parentElement.remove()">×</button>

        <label>Method</label>
        <select name="api_method[]" class="form-control"
                onchange="toggleRequest(this)">
            <option value="GET">GET</option>
            <option value="POST">POST</option>
        </select>

        <label>API Name</label>
        <input type="text" name="api_name[]" class="form-control" required>

        <label>API URL</label>
        <input type="text" name="api_url[]" class="form-control" required>

        <label>Request (JSON)</label>
        <textarea name="api_request[]"
                  class="form-control request-box"
                  disabled
                  placeholder="Not required for GET"
                  onblur="generateRequestTable(this)"></textarea>

        <h4 class="req-table-title"
            style="display:none;color:#f07f2e;">
            JSON Request Field Description
        </h4>

        <table class="table table-bordered req-table"
               style="display:none;">
            <thead style="background:#f07f2e;color:#fff;">
                <tr>
                    <th>Parameter</th>
                    <th>Description</th>
                    <th>Mandatory / Optional</th>
                    <th>Data Type</th>
                    <th>Length / Value</th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>

        <label>Response (JSON)</label>
        <textarea name="api_response[]" class="form-control"></textarea>
    </div>`);
}

function toggleRequest(sel){
    const box = sel.closest('.api-card').querySelector('.request-box');
    if(sel.value === 'GET'){
        box.value='';
        box.disabled=true;
        box.placeholder='Not required for GET';
    }else{
        box.disabled=false;
        box.placeholder='';
    }
}

function addError(){
    document.getElementById('errorContainer').insertAdjacentHTML('beforeend',`
    <div class="api-card">
        <button type="button" class="remove-btn" onclick="this.parentElement.remove()">×</button>

        <label>Error Code</label>
        <input type="text" name="error_code[]" class="form-control">

        <label>Error Description</label>
        <input type="text" name="error_desc[]" class="form-control">
    </div>`);
}

function setDownloadType(type) {
    document.getElementById('download_type').value = type;
}

function generateRequestTable(textarea) {

    const card = textarea.closest('.api-card');
    const apiIndex = [...document.querySelectorAll('.api-card')].indexOf(card);

    const table = card.querySelector('.req-table');
    const tbody = table.querySelector('tbody');
    const title = card.querySelector('.req-table-title');

    tbody.innerHTML = '';

    let json;
    try {
        json = JSON.parse(textarea.value);
    } catch (e) {
        table.style.display = 'none';
        title.style.display = 'none';
        return;
    }

    title.style.display = 'block';
    table.style.display = 'table';

    Object.keys(json).forEach(key => {
        const val = json[key];

        let type = typeof val;
        let length = '';

        if (type === 'string') length = val.length;
        else if (type === 'number') length = val.toString().length;
        else if (Array.isArray(val)) { type = 'Array'; length = val.length; }
        else if (type === 'object') { type = 'Object'; length = '-'; }

        const row = `
        <tr>
            <td><input name="req_param[${apiIndex}][]" class="form-control" value="${key}" readonly></td>
            <td><input name="req_desc[${apiIndex}][]" class="form-control"></td>
            <td>
                <select name="req_mandatory[${apiIndex}][]" class="form-control">
                    <option>Mandatory</option>
                    <option>Optional</option>
                </select>
            </td>
            <td><input name="req_type[${apiIndex}][]" class="form-control" value="${type}" readonly></td>
            <td><input name="req_length[${apiIndex}][]" class="form-control" value="${length}"></td>
        </tr>`;
        tbody.insertAdjacentHTML('beforeend', row);
    });
}

function importCollection() {

    let raw = document.getElementById('collection_json').value.trim();
    if (!raw) return alert('Paste collection JSON');

    let data;
    try {
        data = JSON.parse(raw);
    } catch (e) {
        return alert('Invalid JSON');
    }

    if (!data.item || !Array.isArray(data.item)) {
        return alert('Invalid Postman collection');
    }

    // remove only API cards
    clearApiCards();

    // recursively extract APIs
    extractItems(data.item);

    alert('Collection imported successfully');
}

function extractItems(items) {
    items.forEach(item => {

        // 📁 Folder (has nested items)
        if (item.item && Array.isArray(item.item)) {
            extractItems(item.item);
            return;
        }

        // 🚀 Actual API request
        if (!item.request) return;

        const name = item.name || 'Unnamed API';
        const method = item.request.method || 'GET';

        let url = '';
        if (item.request.url) {
            if (item.request.url.raw) {
                url = item.request.url.raw;
            } else {
                const protocol = item.request.url.protocol || 'https';
                const host = (item.request.url.host || []).join('.');
                const path = (item.request.url.path || []).join('/');
                url = protocol + '://' + host + '/' + path;
            }
        }

        let req = item.request.body?.raw || '';
        let res = item.response?.[0]?.body || '';

        const container = document.getElementById('apiContainer');

        container.insertAdjacentHTML(
            'beforeend',
            createApiCard(name, method, url, req, res)
        );

        // ✅ AUTO-GENERATE REQUEST TABLE
        const lastCard = container.querySelector('.api-card:last-child');
        const textarea = lastCard.querySelector('.request-box');

        if (textarea && textarea.value.trim() !== '') {
            generateRequestTable(textarea);
        }

    });
}

function createApiCard(name, method, url, req, res) {
    return `
    <div class="api-card">
        <button type="button" class="remove-btn"
                onclick="this.parentElement.remove()">×</button>

        <label>Method</label>
        <select name="api_method[]" class="form-control"
                onchange="toggleRequest(this)">
            <option value="GET" ${method==='GET'?'selected':''}>GET</option>
            <option value="POST" ${method==='POST'?'selected':''}>POST</option>
        </select>

        <label>API Name</label>
        <input type="text" name="api_name[]" class="form-control"
               value="${name}">

        <label>API URL</label>
        <input type="text" name="api_url[]" class="form-control"
               value="${url}">

        <label>Request (JSON)</label>
        <textarea name="api_request[]"
                  class="form-control request-box"
                  ${method==='GET'?'disabled':''}
                  onblur="generateRequestTable(this)">${req}</textarea>

        <h4 class="req-table-title"
            style="display:none;color:#f07f2e;">
            JSON Request Field Description
        </h4>

        <table class="table table-bordered req-table"
               style="display:none;">
            <thead style="background:#f07f2e;color:#fff;">
                <tr>
                    <th>Parameter</th>
                    <th>Description</th>
                    <th>Mandatory</th>
                    <th>Type</th>
                    <th>Length</th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>

        <label>Response (JSON)</label>
        <textarea name="api_response[]" class="form-control">${res}</textarea>
    </div>`;
}


function loadCollectionFile(input) {
    const file = input.files[0];
    if (!file) return;

    if (!file.name.endsWith('.json')) {
        alert('Please upload a valid JSON file');
        input.value = '';
        return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
        document.getElementById('collection_json').value = e.target.result;
    };
    reader.readAsText(file);
}

function clearApiCards() {
    document.querySelectorAll('#apiContainer .api-card')
        .forEach(el => el.remove());
}

function addDefaultApiIfEmpty() {
    const container = document.getElementById('manualSection');
    const cards = container.querySelectorAll('.api-card');

    if (cards.length === 0) {
        container.insertAdjacentHTML('afterbegin', createApiCard('', 'GET', '', '', ''));
    }
}

function switchMode(mode) {
    const importSection = document.getElementById('importSection');
    const manualSection = document.getElementById('manualSection');

    const importBtn = document.getElementById('btnImport');
    const manualBtn = document.getElementById('btnManual');

    if (mode === 'import') {
        importSection.style.display = 'block';
        manualSection.style.display = 'none';

        importBtn.classList.add('btn-warning');
        importBtn.classList.remove('btn-default');

        manualBtn.classList.add('btn-default');
        manualBtn.classList.remove('btn-primary');
    } 
    else {
        importSection.style.display = 'none';
        manualSection.style.display = 'block';

        manualBtn.classList.add('btn-primary');
        manualBtn.classList.remove('btn-default');

        importBtn.classList.add('btn-default');
        importBtn.classList.remove('btn-warning');

        // ✅ ENSURE one default API always exists
        addDefaultApiIfEmpty();
    }
}


</script>

</body>
</html>
