#include <ESP8266WiFi.h>
#include <ArduinoJson.h>
#include <SoftwareSerial.h>
#include "PMS7003-SOLDERED.h"
const char* ssid = "Nhom6";
const char* password = "12345678";
IPAddress server_ip(192,168,8,1);
#define PORT 5000

WiFiClient client;
#define RELAY_MIST D6
#define SOIL_PIN A0
#define RELAY_PUMP D8
#define NODE_ID "node2"   // 

unsigned long lastSend = 0;
// int pumpState = 1;
// int mistState =1;
int pumpFromGW =1;
int mistFromGW =1;
int PMS_RX = D1; 
int PMS_TX = D2;
PMS7003 pms(PMS_RX, PMS_TX);
// ===== THRESHOLD =====
#define PM25_THRESHOLD 150
void setup() {
  Serial.begin(115200);
  pms.begin();
  pinMode(RELAY_MIST, OUTPUT);
  digitalWrite(RELAY_MIST, LOW); // OFF (relay active LOW)
  pinMode(RELAY_PUMP, OUTPUT);
  digitalWrite(RELAY_PUMP, LOW); // OFF
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void loop() {
  if (!client.connected()) {
    client.connect(server_ip, PORT);
  }
    // ===== RECEIVE FROM GATEWAY =====
  if (client.available()) {
    String msg = client.readStringUntil('\n');
    Serial.println("GW -> " + msg);
    handleGatewayCommand(msg);
  }


  // ===== GỬI DỮ LIỆU =====
  if (millis() - lastSend >= 2000) {
    lastSend = millis();
    
    int soil = analogRead(SOIL_PIN);
    float soil_moisture =100-map(soil, 280, 1023, 0, 100);
    int pm25 = readPm25();
    StaticJsonDocument<200> doc;
    doc["node_id"] = "node2";
    doc["soil"]    = soil_moisture;
    doc["pm25"]    = pm25;
    String json;
    serializeJson(doc, json);
    client.println(json);

    Serial.println("Node2 sent: " + json);
  }
}
void handleGatewayCommand(String payload) {
  StaticJsonDocument<128> doc;
  if (deserializeJson(doc, payload)) return;

  // CHECK TARGET
  if (!doc.containsKey("target")) return;
  const char* target = doc["target"];

  if (strcmp(target, NODE_ID) != 0) {
    // Không phải lệnh cho mình
    Serial.println("123--");
    return;
  }
  // Lệnh đúng node
  if (doc.containsKey("mist")) {
    mistFromGW = (int)doc["mist"];
    Serial.print("Command for ");
    Serial.print(NODE_ID);
    Serial.print(": mist = ");
    Serial.println(mistFromGW);
    digitalWrite(D6, mistFromGW);
  }
  if (doc.containsKey("pump")) {
    pumpFromGW = (int)doc["pump"];
    Serial.print("Command for ");
    Serial.print(NODE_ID);
    Serial.print(": pump = ");
    Serial.println(pumpFromGW);
    digitalWrite(RELAY_PUMP, pumpFromGW);
  }
}
int readPm25()
{
  pms.read();

    if (pms) {
        // Đọc thành công, in các giá trị bụi chính
        Serial.println(F("---------------------------------------"));
        // Serial.print(F("PM 1.0: "));
        // Serial.print(pms.pm01);
        // Serial.println(F(" ug/m3"));

        // Serial.print(F("PM 2.5: "));
        // Serial.print(pms.pm25);
        // Serial.println(F(" ug/m3"));

        // Serial.print(F("PM 10 : "));
        // Serial.print(pms.pm10);
        // Serial.println(F(" ug/m3"));
    }
    else {
        // Kiểm tra lỗi nếu không đọc được
        if (pms.status == pms.ERROR_TIMEOUT) {
            Serial.println(F("Loi: Timeout - Kiem tra day va chan SET!"));
        } else {
            Serial.print(F("Loi sensor, ma loi: "));
            Serial.println(pms.status);
        }
    }
    return pms.pm25;
}