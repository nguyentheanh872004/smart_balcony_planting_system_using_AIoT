<?php
header('Content-Type: application/json');

$conn = new mysqli("44.210.85.81", "root", "12345678", "group51");
if ($conn->connect_error) {
    echo json_encode(["status"=>"error"]);
    exit;
}

$sql_now = "SELECT * FROM iot_data ORDER BY id DESC LIMIT 1";
$sql_past = "SELECT * FROM iot_data ORDER BY id DESC LIMIT 1 OFFSET 150";

$now = $conn->query($sql_now)->fetch_assoc();
$past = $conn->query($sql_past)->fetch_assoc();

if (!$now || !$past) {
    echo json_encode(["status"=>"not_enough_data"]);
    exit;
}

echo json_encode([
    "status" => "ok",
    "delta" => [
        "d_pressure" => $now["pressure"] - $past["pressure"],
        "d_humi"     => $now["humi"]     - $past["humi"],
        "d_temp"     => $now["temp"]     - $past["temp"],
        "d_rain"     => $now["rain"]     - $past["rain"],
        "d_pm25"     => $now["pm25"]     - $past["pm25"]
    ]
]);
