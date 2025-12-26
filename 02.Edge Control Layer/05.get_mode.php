<?php
header("Content-Type: application/json");
$conn = new mysqli("44.210.85.81", "root", "12345678", "group51");
if ($conn->connect_error) {
    echo json_encode(["error" => "db_error"]);
    exit;
}

$sql = "SELECT mode FROM iot_control ORDER BY id DESC LIMIT 1";
$res = $conn->query($sql);

if ($row = $res->fetch_assoc()) {
    echo json_encode(["mode" => intval($row["mode"])]);
} else {
    echo json_encode(["mode" => 1]); // mặc định AUTO
}
