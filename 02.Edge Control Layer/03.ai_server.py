import json
import joblib
import requests
import numpy as np
import paho.mqtt.client as mqtt
from datetime import datetime

# ================= CONFIG =================
MQTT_BROKER = "192.168.102.40"
MQTT_PORT   = 1883
SUB_TOPIC   = "gateway_to_server"
PUB_TOPIC   = "server_to_gateway"

PHP_SENSOR_URL   = "http://localhost/iot_final/receive.php"
PHP_MODE_URL     = "http://localhost/iot_final/get_mode.php"
PHP_CONTROL_URL  = "http://localhost/iot_final/get_control.php"
PHP_RAIN_INPUT   = "http://localhost/iot_final/get_rain_input.php"

MODE_MANUAL = 0
MODE_AUTO   = 1

# ================= LOAD MODELS =================
rain_model = joblib.load("./model/random_forest_model_rain.joblib")
pump_model = joblib.load("./model/xgboost_model_pump.joblib")
print("AI models loaded")

# ================= PHP HELPERS =================
def send_to_php(data):
    try:
        requests.post(PHP_SENSOR_URL, json=data, timeout=2)
    except:
        pass

def get_mode():
    try:
        r = requests.get(PHP_MODE_URL, timeout=2)
        return int(r.json()["mode"])
    except:
        return MODE_AUTO

def get_manual_control():
    try:
        r = requests.get(PHP_CONTROL_URL, timeout=2)
        return r.json()
    except:
        return {"pump": 0, "mist": 0}

# ================= AI FUNCTIONS =================
def predict_rain():
    """
    Lấy delta 5 phút từ PHP:
    now(id) - past(id-150)
    """
    try:
        r = requests.get(PHP_RAIN_INPUT, timeout=2)
        data = r.json()

        if data["status"] != "ok":
            return 0

        d = data["delta"]
        X = np.array([[
            d["d_pressure"],
            d["d_humi"],
            d["d_temp"],
            d["d_rain"],
            d["d_pm25"]
        ]])

        return int(rain_model.predict(X)[0])
    except:
        return 0

def predict_pump(sensor, rain_future):
    if rain_future == 1:
        return 0

    X = np.array([[
        sensor["soil"],
        sensor["light"],
        sensor["temp"],
        sensor["humi"],
        rain_future
    ]])

    return int(pump_model.predict(X)[0])

def decide_mist(sensor):
    return 1 if sensor["pm25"] > 150 else 0

# ================= MQTT CALLBACK =================
def on_message(client, userdata, msg):
    payload = json.loads(msg.payload.decode())
    print("SENSOR:", payload)

    # 1️⃣ Lưu sensor vào DB
    send_to_php(payload)

    # 2️⃣ Đọc mode
    mode = get_mode()

    # ================= MANUAL =================
    if mode == MODE_MANUAL:
        control = get_manual_control()

        control_msg = {
            "type": "control",
            "mode": MODE_MANUAL,
            "pump": int(control["pump"]),
            "mist": int(control["mist"]),
            "timestamp": datetime.now().isoformat()
        }

        client.publish(PUB_TOPIC, json.dumps(control_msg))
        send_to_php(control_msg)

        print("MANUAL ", control_msg)
        return

    # ================= AUTO (AI) =================
    rain_future = predict_rain()
    pump = predict_pump(payload, rain_future)
    mist = decide_mist(payload)

    control_msg = {
        "type": "control",
        "mode": MODE_AUTO,
        "pump": pump,
        "mist": mist,
        "rain_future": rain_future,
        "timestamp": datetime.now().isoformat()
    }

    client.publish(PUB_TOPIC, json.dumps(control_msg))
    send_to_php(control_msg)

    print("AUTO ", control_msg)

# ================= MAIN =================
client = mqtt.Client(
    client_id="AI_SERVER",
    protocol=mqtt.MQTTv311,
    callback_api_version=mqtt.CallbackAPIVersion.VERSION1
)

client.on_message = on_message
client.connect(MQTT_BROKER, MQTT_PORT, 60)
client.subscribe(SUB_TOPIC)

print("AI SERVER RUNNING...")
client.loop_forever()
