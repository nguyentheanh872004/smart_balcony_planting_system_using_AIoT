<?php
header("Content-Type: application/json");

// ===== KẾT NỐI DB =====
// $conn = new mysqli("localhost", "root", "", "final_nongnghiep");

$conn = new mysqli("44.210.85.81", "root", "12345678", "group5");
//$conn = new mysqli("47.128.252.157", "final_nongnghiep", "WttcdVrDxVQdhIPi9U020zXnmB5z8UWwkLNL9WVMwdc1XkUPvW", "final_nongnghiep");
if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "DB connection failed"]);
    exit;
}

// ===== ĐỌC JSON RAW =====
$raw = file_get_contents("php://input");
$data = json_decode($raw, true);

if (!$data) {
    http_response_code(400);
    echo json_encode(["error" => "Invalid JSON"]);
    exit;
}

// ===== MAP DỮ LIỆU =====
$time = date("Y-m-d H:i:s");

$temp     = $data["temp"]     ?? null;
$humi     = $data["humi"]     ?? null;
$pressure = $data["pressure"] ?? null;
$light    = $data["light"]    ?? null;
$rain     = $data["rain"]     ?? null;
$pm25     = $data["pm25"]     ?? null;
$soil     = $data["soil"]     ?? null;
$pump     = $data["pump"]     ?? null;
$mist     = $data["mist"]     ?? null;
$mode     = $data["mode"]     ?? "1";

// ===== INSERT =====
$stmt = $conn->prepare("
INSERT INTO iot_data
(time, temp, humi, pressure, light, rain, pm25, soil, pump, mist, mode)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
");

$stmt->bind_param(
    "sdddiiiiiss",
    $time,
    $temp,
    $humi,
    $pressure,
    $light,
    $rain,
    $pm25,
    $soil,
    $pump,
    $mist,
    $mode
);

if ($stmt->execute()) {
    echo json_encode(["status" => "OK"]);
} else {
    http_response_code(500);
    echo json_encode(["error" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>