<?php
/* =====================================================
   DATABASE CONFIG
===================================================== */
$DB_HOST = "18.210.37.33";
$DB_NAME = "group51";
$DB_USER = "root";
$DB_PASS = "12345678";

try {
    $pdo = new PDO(
        "mysql:host=$DB_HOST;dbname=$DB_NAME;charset=utf8",
        $DB_USER,
        $DB_PASS
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    die("Lỗi kết nối database: " . $e->getMessage());
}

/* =====================================================
   API: Lấy dữ liệu sensor mới nhất (cho AJAX)
===================================================== */
if(isset($_GET['ajax'])) {
    header('Content-Type: application/json');
    
    try {
        // Lấy dữ liệu sensor mới nhất
        $sql = "SELECT * FROM iot_data ORDER BY time DESC LIMIT 1";
        $stmt = $pdo->query($sql);
        $data = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Lấy trạng thái điều khiển
        $sql2 = "SELECT * FROM iot_control ORDER BY id DESC LIMIT 1";
        $stmt2 = $pdo->query($sql2);
        $control = $stmt2->fetch(PDO::FETCH_ASSOC);
        
        if($data && $control) {
            $data['pump'] = (int)$control['pump'];
            $data['mist'] = (int)$control['mist'];
            $data['mode'] = (int)$control['mode'];
        } elseif($data) {
            $data['pump'] = 0;
            $data['mist'] = 0;
            $data['mode'] = 0;
        }
        
        echo json_encode($data);
    } catch (PDOException $e) {
        echo json_encode(['error' => $e->getMessage()]);
    }
    exit;
}

/* =====================================================
   API: ESP32 đọc lệnh điều khiển
===================================================== */
if(isset($_GET['get_control'])) {
    header('Content-Type: application/json');
    
    try {
        $sql = "SELECT * FROM iot_control ORDER BY id DESC LIMIT 1";
        $stmt = $pdo->query($sql);
        $control = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if($control) {
            echo json_encode($control);
        } else {
            echo json_encode(['pump' => 0, 'mist' => 0, 'mode' => 0]);
        }
    } catch (PDOException $e) {
        echo json_encode(['error' => $e->getMessage()]);
    }
    exit;
}

/* =====================================================
   Xử lý POST: Cập nhật điều khiển
===================================================== */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // Lấy trạng thái hiện tại
        $current = $pdo->query("SELECT * FROM iot_control ORDER BY id DESC LIMIT 1")->fetch(PDO::FETCH_ASSOC);
        
        // Nếu chưa có dữ liệu, tạo giá trị mặc định
        if(!$current) {
            $pump = 0;
            $mist = 0;
            $mode = 0;
            $sensorID = 1;
        } else {
            // Mặc định giữ nguyên giá trị hiện tại
            $pump = $current['pump'];
            $mist = $current['mist'];
            $mode = $current['mode'];
            $sensorID = $current['id'];
        }
        
        // Kiểm tra xem người dùng bấm nút gì
        if(isset($_POST['ai_mode'])) {
            // Toggle AI mode: 0 -> 1 hoặc 1 -> 0
            $mode = (int)$_POST['ai_mode'];
        }
        
        if(isset($_POST['pump'])) {
            // Bật bơm: gửi 1, Tắt bơm: gửi 0
            $pump = (int)$_POST['pump'];
        }
        
        if(isset($_POST['mist'])) {
            // Bật phun sương: gửi 1, Tắt phun sương: gửi 0
            $mist = (int)$_POST['mist'];
        }
        
        // INSERT hoặc UPDATE vào database
        if(!$current) {
            // Chưa có dữ liệu -> INSERT
            $sql = "INSERT INTO iot_control (id, pump, mist, mode) VALUES (?, ?, ?, ?)";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$sensorID, $pump, $mist, $mode]);
        } else {
            // Đã có dữ liệu -> UPDATE
            $sql = "UPDATE iot_control SET pump = ?, mist = ?, mode = ? WHERE id = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$pump, $mist, $mode, $sensorID]);
        }
        
        header("Location: " . $_SERVER['PHP_SELF']);
        exit;
    } catch (PDOException $e) {
        die("Lỗi cập nhật: " . $e->getMessage());
    }
}

/* =====================================================
   Lấy dữ liệu để hiển thị
===================================================== */
try {
    // Dữ liệu sensor
    $sql = "SELECT * FROM iot_data ORDER BY time DESC LIMIT 1";
    $stmt = $pdo->query($sql);
    $sensor = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Dữ liệu điều khiển
    $sql2 = "SELECT * FROM iot_control ORDER BY id DESC LIMIT 1";
    $stmt2 = $pdo->query($sql2);
    $control = $stmt2->fetch(PDO::FETCH_ASSOC);
    
    // Gán giá trị mặc định
    $temp = $sensor ? $sensor['temp'] : 0;
    $humi = $sensor ? $sensor['humi'] : 0;
    $pressure = $sensor ? $sensor['pressure'] : 0;
    $light = $sensor ? $sensor['light'] : 0;
    $pm25 = $sensor ? $sensor['pm25'] : 0;
    $soil = $sensor ? $sensor['soil'] : 0;
    $rain = $sensor ? $sensor['rain'] : 0;
    
    $pump = $control ? $control['pump'] : 0;
    $mist = $control ? $control['mist'] : 0;
    $mode = $control ? $control['mode'] : 0;
    
} catch (PDOException $e) {
    die("Lỗi đọc dữ liệu: " . $e->getMessage());
}

$status = $mode ? "🤖 AI MODE (AUTO)" : "🧑‍🌾 MANUAL MODE";
?>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Smart IoT Dashboard</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
<style>
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    background: linear-gradient(135deg, #141e30, #243b55);
    color: #fff;
    min-height: 100vh;
    padding: 20px;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    padding: 30px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

h1 {
    text-align: center;
    margin-bottom: 10px;
    font-size: 2rem;
}

.status {
    text-align: center;
    font-size: 1.3rem;
    font-weight: bold;
    margin-bottom: 30px;
    color: #4ade80;
}

.info-box {
    background: rgba(59, 130, 246, 0.2);
    border-left: 4px solid #3b82f6;
    padding: 15px;
    margin-bottom: 30px;
    border-radius: 8px;
}

.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 20px;
    margin-bottom: 40px;
}

.card {
    background: rgba(255, 255, 255, 0.15);
    border-radius: 15px;
    padding: 25px;
    text-align: center;
    transition: all 0.3s ease;
    border: 1px solid rgba(255, 255, 255, 0.1);
}

.card:hover {
    transform: translateY(-5px);
    background: rgba(255, 255, 255, 0.2);
}

.card i {
    font-size: 2.5rem;
    margin-bottom: 10px;
    display: block;
}

.card-label {
    font-size: 0.9rem;
    opacity: 0.8;
    margin-bottom: 8px;
}

.card-value {
    font-size: 1.8rem;
    font-weight: bold;
    color: #4ade80;
}

.controls {
    text-align: center;
    margin-top: 40px;
}

.toggle-container {
    display: inline-block;
    margin-bottom: 10px;
}

.toggle {
    width: 70px;
    height: 35px;
    background: #64748b;
    border-radius: 35px;
    position: relative;
    cursor: pointer;
    transition: all 0.3s ease;
    display: inline-block;
}

.toggle.on {
    background: #22c55e;
}

.toggle::after {
    content: "";
    width: 27px;
    height: 27px;
    background: white;
    border-radius: 50%;
    position: absolute;
    top: 4px;
    left: 4px;
    transition: all 0.3s ease;
    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.toggle.on::after {
    left: 39px;
}

.toggle-label {
    margin-top: 10px;
    font-size: 1.1rem;
}

.manual-controls {
    margin-top: 30px;
}

.btn {
    padding: 14px 28px;
    border: none;
    border-radius: 10px;
    margin: 8px;
    cursor: pointer;
    font-size: 15px;
    font-weight: bold;
    transition: all 0.3s ease;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.2);
}

.btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);
}

.btn-on {
    background: #22c55e;
    color: white;
}

.btn-off {
    background: #ef4444;
    color: white;
}

.btn:active {
    transform: translateY(0);
}

footer {
    text-align: center;
    margin-top: 40px;
    opacity: 0.7;
    font-size: 0.9rem;
}

.update-indicator {
    display: inline-block;
    width: 12px;
    height: 12px;
    background: #22c55e;
    border-radius: 50%;
    margin-left: 10px;
    animation: pulse 2s infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(1.1); }
}

.status-indicator {
    display: inline-block;
    padding: 8px 16px;
    margin: 10px;
    border-radius: 8px;
    font-size: 0.9rem;
    background: rgba(255, 255, 255, 0.1);
}

.status-indicator.active {
    background: rgba(34, 197, 94, 0.3);
    border: 1px solid #22c55e;
}

@media (max-width: 768px) {
    .container {
        padding: 20px;
    }
    
    h1 {
        font-size: 1.5rem;
    }
    
    .grid {
        grid-template-columns: repeat(2, 1fr);
    }
}
</style>
</head>
<body>

<div class="container">
    <h1>🌱 Smart IoT Dashboard <span class="update-indicator"></span></h1>
    <div class="status" id="status"><?php echo $status; ?></div>
    
    <div class="info-box">
        📌 <strong>Hướng dẫn:</strong> Bấm nút để gửi lệnh (1=BẬT, 0=TẮT) vào database
        <br>
        <div style="margin-top: 10px;">
            <span class="status-indicator <?php echo $pump ? 'active' : ''; ?>">
                🚿 Bơm: <?php echo $pump ? 'ON (1)' : 'OFF (0)'; ?>
            </span>
            <span class="status-indicator <?php echo $mist ? 'active' : ''; ?>">
                💨 Sương: <?php echo $mist ? 'ON (1)' : 'OFF (0)'; ?>
            </span>
            <span class="status-indicator <?php echo $mode ? 'active' : ''; ?>">
                🤖 AI: <?php echo $mode ? 'ON (1)' : 'OFF (0)'; ?>
            </span>
        </div>
    </div>
    
    <div class="grid">
        <div class="card">
            <i class="fa fa-temperature-half" style="color: #f59e0b;"></i>
            <div class="card-label">Nhiệt độ</div>
            <div class="card-value" id="temp"><?php echo $temp; ?>°C</div>
        </div>
        
        <div class="card">
            <i class="fa fa-droplet" style="color: #3b82f6;"></i>
            <div class="card-label">Độ ẩm</div>
            <div class="card-value" id="humi"><?php echo $humi; ?>%</div>
        </div>
        
        <div class="card">
            <i class="fa fa-sun" style="color: #fbbf24;"></i>
            <div class="card-label">Ánh sáng</div>
            <div class="card-value" id="light"><?php echo $light; ?> </div>
        </div>
        
        <div class="card">
            <i class="fa fa-gauge-high" style="color: #8b5cf6;"></i>
            <div class="card-label">Áp suất</div>
            <div class="card-value" id="pressure"><?php echo $pressure; ?> hPa</div>
        </div>
        
        <div class="card">
            <i class="fa fa-smog" style="color: #ef4444;"></i>
            <div class="card-label">PM2.5</div>
            <div class="card-value" id="pm25"><?php echo $pm25; ?></div>
        </div>

        <div class="card">
            <i class="fa fa-gauge-high" style="color: #8b5cf6;"></i>
            <div class="card-label">Luong mua</div>
            <div class="card-value" id="pressure"><?php echo $rain; ?> %</div>
        </div>

        <div class="card">
            <i class="fa fa-seedling" style="color: #22c55e;"></i>
            <div class="card-label">Độ ẩm đất</div>
            <div class="card-value" id="soil"><?php echo $soil; ?>%</div>
        </div>
    </div>
    
    <div class="controls">
        <form method="POST" id="controlForm">
            
            <div class="toggle-container">
                <div class="toggle <?php echo $mode ? 'on' : ''; ?>" id="toggleBtn" onclick="toggleAI()"></div>
                <div class="toggle-label">AI Mode (<?php echo $mode ? '1' : '0'; ?>)</div>
            </div>
            
            <div class="manual-controls" id="manualControls" style="<?php echo $mode ? 'display:none' : ''; ?>">
                <div>
                    <button type="submit" class="btn btn-on" name="pump" value="1">🚿 Bật bơm (1)</button>
                    <button type="submit" class="btn btn-off" name="pump" value="0">⛔ Tắt bơm (0)</button>
                </div>
                <div>
                    <button type="submit" class="btn btn-on" name="mist" value="1">💨 Bật sương (1)</button>
                    <button type="submit" class="btn btn-off" name="mist" value="0">❌ Tắt sương (0)</button>
                </div>
            </div>
        </form>
    </div>
    
</div>

<script>
function toggleAI() {
    // Toggle AI mode: 0 -> 1 hoặc 1 -> 0
    let currentMode = document.getElementById('toggleBtn').classList.contains('on') ? 1 : 0;
    let newMode = currentMode ? 0 : 1;
    
    // Tạo form data và submit
    let form = document.getElementById('controlForm');
    let input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'ai_mode';
    input.value = newMode;
    form.appendChild(input);
    form.submit();
}

function updateData() {
    fetch('?ajax=1')
        .then(response => response.json())
        .then(data => {
            if(data && !data.error) {
                // Cập nhật giá trị sensor
                if(data.temp !== undefined) document.getElementById('temp').textContent = data.temp + '°C';
                if(data.humi !== undefined) document.getElementById('humi').textContent = data.humi + '%';
                if(data.light !== undefined) document.getElementById('light').textContent = data.light + ' lux';
                if(data.pressure !== undefined) document.getElementById('pressure').textContent = data.pressure + ' hPa';
                if(data.pm25 !== undefined) document.getElementById('pm25').textContent = data.pm25;
                if(data.soil !== undefined) document.getElementById('soil').textContent = data.soil + '%';
                
                // Cập nhật trạng thái mode
                if(data.mode !== undefined) {
                    let aiMode = parseInt(data.mode);
                    let status = aiMode ? '🤖 AI MODE (AUTO)' : '🧑‍🌾 MANUAL MODE';
                    document.getElementById('status').textContent = status;
                    
                    let toggle = document.getElementById('toggleBtn');
                    let manual = document.getElementById('manualControls');
                    
                    if(aiMode) {
                        toggle.classList.add('on');
                        manual.style.display = 'none';
                    } else {
                        toggle.classList.remove('on');
                        manual.style.display = 'block';
                    }
                }
            }
        })
        .catch(error => {
            console.error('Lỗi:', error);
        });
}

// Tự động cập nhật mỗi 2 giây
setInterval(updateData, 2000);

// Cập nhật lần đầu sau 500ms
setTimeout(updateData, 500);
</script>

</body>
</html>
