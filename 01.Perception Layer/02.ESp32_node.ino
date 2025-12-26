#include <DHT.h>
#include <DHT_U.h>

#include <WiFi.h>
#include <ArduinoJson.h>
#include <Adafruit_BMP085.h>
// ===== WIFI =====
const char* ssid = "Nhom6";
const char* password = "12345678";
IPAddress server_ip(192,168,8,1);
#define PORT 5000

WiFiClient client;
#define DHTPIN 27
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);
// ===== PIN =====
#define NODE_ID "node1"
#define LIGHT_PIN     32
#define RAIN_PIN      33
// #define PM25_PIN      34
#define RELAY_MIST    26
Adafruit_BMP085 bmp;
unsigned long lastSend = 0;
int mistState = 0;

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  // pinMode(35, INPUT);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
  dht.begin();
  bmp.begin();
}

void loop() {
  if (!client.connected()) {
    client.connect(server_ip, PORT);
  }

  if (millis() - lastSend >= 2000) {
    lastSend = millis();

    // ===== READ SENSOR =====
    int light = 4095 - analogRead(LIGHT_PIN);
    int rain_a  = analogRead(RAIN_PIN);
    float rain = 100-map(rain_a, 1200, 4095, 0, 100);
    float humi = dht.readHumidity();
    float temp = dht.readTemperature();
    // float pressure = 1011;   // demo
    float pressure = bmp.readPressure();

  // ===== RECEIVE FROM GATEWAY =====
  // if (client.available()) {
  //   String msg = client.readStringUntil('\n');
  //   Serial.println("GW -> " + msg);
  //   handleGatewayCommand(msg);
  // }
        // ===== RECEIVE FROM GATEWAY =====
  if (client.available()) {
    String msg = client.readStringUntil('\n');
    Serial.println("GW -> " + msg);
    handleGatewayCommand(msg);
  }

    // ===== JSON =====
    StaticJsonDocument<256> doc;
    doc["node_id"]  = "node1";
    doc["temp"]     = temp;
    doc["humi"]     = humi;
    doc["light"]    = light;
    doc["pressure"] = pressure;
    doc["rain"]     = rain;
    // doc["pm25"]     = pm25;
    // doc["mist"]     = mistState;

    String json;
    serializeJson(doc, json);
    client.println(json);

    Serial.println("node1 sent: " + json);
  }
}
// void handleGatewayCommand(String payload) {
//   StaticJsonDocument<128> doc;
//   DeserializationError err = deserializeJson(doc, payload);

//   if (err) {
//     Serial.println("JSON parse failed");
//     return;
//   }

//   // Ví dụ: {"mist":1}
//   if (doc.containsKey("mist")) {
//     // mistFromGW = doc["mist"];
//     // Serial.print("Gateway set mist = ");
//     // Serial.println(mistFromGW);
//   }
// }
void handleGatewayCommand(String payload) {
  5

  // // Lệnh đúng node
  // if (doc.containsKey("mist")) {
  //   mistFromGW = doc["mist"];
  //   Serial.print("Command for ");
  //   Serial.print(NODE_ID);
  //   Serial.print(": mist = ");
  //   Serial.println(mistFromGW);
  //   digitalWrite(D5, !mistFromGW);
  // }
  // if (doc.containsKey("pump")) {
  //   pumpFromGW = doc["pump"];
  //   Serial.print("Command for ");
  //   Serial.print(NODE_ID);
  //   Serial.print(": pump = ");
  //   Serial.println(pumpFromGW);
  //   digitalWrite(D0, !pumpFromGW);
  // }
}