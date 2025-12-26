#include <ESP8266WiFi.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>
// ===== WIFI =====
const char* ssid_sta = "Luckycat";
const char* pass_sta = "67722999";

const char* ssid_ap = "Nhom6";
const char* pass_ap = "12345678";
const char* mqtt_server = "192.168.102.40";
// ===== TCP SERVER =====
WiFiServer server(5000);
WiFiClient client;   // 🔥 CLIENT TOÀN CỤC
WiFiClient node1Client;
WiFiClient node2Client;

WiFiClient mqttWiFi;      // cho MQTT
PubSubClient mqttClient(mqttWiFi);
unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE	(50)
char msg[MSG_BUFFER_SIZE];
// ===== IP AP =====
IPAddress ap_ip(192,168,8,1);
IPAddress ap_gw(192,168,8,1);
IPAddress ap_subnet(255,255,255,0);

unsigned long node1_ts = 0;
unsigned long node2_ts = 0;
bool node1_updated = false;
bool node2_updated = false;

unsigned long last_publish_ts = 0;
const unsigned long PUBLISH_INTERVAL = 2000; // 2s

int systemMode = 0;   // auto | manual

struct {
  float temp, humi, pressure, rain, soil;
  int light, pm25, mist;
  int pump;
} data;

void readNode(WiFiClient &c) {
  if (!c || !c.connected()) return;

  while (c.available()) {
    String json = c.readStringUntil('\n');

    StaticJsonDocument<256> doc;
    if (deserializeJson(doc, json)) return;

    String id = doc["node_id"];

    if (id == "node1") {
      node1_ts = millis();
      node1_updated = true;
      data.temp = doc["temp"];
      data.humi = doc["humi"];
      data.light = doc["light"];
      data.pressure = doc["pressure"];
      data.rain = doc["rain"];

    }

    if (id == "node2") {
      node2_ts = millis();
      node2_updated = true;
      data.soil = doc["soil"];
      data.pm25 = doc["pm25"];
      // data.pump = doc["pump"];
      // data.mist = doc["mist"];
    }
  }
}


void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) Serial.print((char)payload[i]);
  Serial.println();

  StaticJsonDocument<256> doc;
  if (deserializeJson(doc, payload, length)) return;

  const char* type = doc["type"];
  const char* mode = doc["mode"];
  // // ===== CHỈ LƯU MODE (KHÔNG DÙNG ĐIỀU KHIỂN) =====
  // if (strcmp(mode, "0") == 0) {
  //   // systemMode = doc["mode"].as<String>();
  //   systemMode = doc["mode"];
  //   Serial.print("MODE UPDATED: ");
  //   Serial.println(systemMode);
  //   return;
  // }
  // else
  // {
  //   systemMode = doc["mode"];
  //   Serial.print("MODE UPDATED: ");
  //   Serial.println(systemMode);
  //   return;
  // }

  // ===== NHẬN LỆNH ĐIỀU KHIỂN (LUÔN NHẬN) =====
  if (strcmp(type, "control") == 0) {

    StaticJsonDocument<256> cmd;

    if (doc.containsKey("pump")) {
      data.pump = doc["pump"];
      cmd["pump"] = data.pump;
    }

    if (doc.containsKey("mist")) {
      data.mist = doc["mist"];
      cmd["mist"] = data.mist;
    }
      
    // 🔥 GỬI LUÔN XUỐNG NODE2
    if (node2Client && node2Client.connected()) {
      String out;
      cmd["target"] = "node2";
      serializeJson(cmd, out);
      out += "\n";
      node2Client.print(out);

      Serial.print("Sent to node2: ");
      Serial.println(out);
    } else {
      Serial.println("Node2 not connected!");
    }
  }
}


void reconnect() {
  // Loop until we're reconnected
  while (!mqttClient.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (mqttClient.connect(clientId.c_str())) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      mqttClient.publish("gateway_to_server", "hello IoT Gateway...");
      // ... and resubscribe
      mqttClient.subscribe("server_to_gateway");
    } else {
      Serial.print("failed, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  mqttClient.setServer(mqtt_server, 1883);
  mqttClient.setCallback(callback);
  
  WiFi.mode(WIFI_AP_STA);

  // ===== STA =====
  WiFi.begin(ssid_sta, pass_sta);
  Serial.print("Connecting to home WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nSTA connected, IP: " + WiFi.localIP().toString());

  // ===== AP =====
  WiFi.softAPConfig(ap_ip, ap_gw, ap_subnet);
  WiFi.softAP(ssid_ap, pass_ap);
  Serial.println("AP started, IP: " + WiFi.softAPIP().toString());

  server.begin();
  Serial.println("TCP Server started");
}

void loop() {
  if (!mqttClient.connected()) reconnect();
  mqttClient.loop();

  WiFiClient newClient = server.available();
  if (newClient) {
    if (!node1Client || !node1Client.connected()) node1Client = newClient;
    else if (!node2Client || !node2Client.connected()) node2Client = newClient;
  }

  readNode(node1Client);
  readNode(node2Client);

  unsigned long now = millis();

unsigned long diff = (node1_ts > node2_ts) ?
                     (node1_ts - node2_ts) :
                     (node2_ts - node1_ts);

if ( node1_updated &&
     node2_updated &&
     diff < 3000 &&
     (now - last_publish_ts >= PUBLISH_INTERVAL) ) {

  DynamicJsonDocument outDoc(512);

  outDoc["temp"]     = data.temp;
  outDoc["humi"]     = data.humi;
  outDoc["light"]    = data.light;
  outDoc["pressure"] = data.pressure;
  outDoc["rain"]     = data.rain;
  outDoc["pm25"]     = data.pm25;
  // outDoc["mist"]     = data.mist;
  outDoc["soil"]     = data.soil;
  // outDoc["pump"]     = data.pump;
  // outDoc["mode"] = systemMode;


  char jsonStr[256];
  serializeJson(outDoc, jsonStr);

  mqttClient.publish("gateway_to_server", jsonStr);
  Serial.println("MQTT SENT:");
  Serial.println(jsonStr);

  // 🔒 RESET FLAG sau khi gửi
  node1_updated = false;
  node2_updated = false;
  last_publish_ts = now;
}

  }
