<?php
header('Content-Type: application/json');

$conn = new mysqli("44.210.85.81", "root", "12345678", "group51");
if ($conn->connect_error) {
    echo json_encode(["pump"=>0,"mist"=>0]);
    exit;
}

$sql = "SELECT pump, mist FROM iot_control ORDER BY id DESC LIMIT 1";
$result = $conn->query($sql);

if ($row = $result->fetch_assoc()) {
    echo json_encode([
        "pump" => intval($row["pump"]),
        "mist" => intval($row["mist"])
    ]);
} else {
    echo json_encode(["pump"=>0,"mist"=>0]);
}
