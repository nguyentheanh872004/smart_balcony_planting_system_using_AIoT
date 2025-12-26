import json
import requests
import paho.mqtt.client as mqtt

PHP_API_URL = "http://localhost/iot_final/receive.php"
MQTT_BROKER = "10.250.203.21"
MQTT_TOPIC  = "gateway_to_server"
# MQTT_TOPIC  = "gateway_to_server"
def on_message(client, userdata, msg):
    try:
        payload = json.loads(msg.payload.decode())
        print("MQTT:", payload)

        r = requests.post(PHP_API_URL, json=payload, timeout=3)
        print("PHP:", r.text)

    except Exception as e:
        print("ERROR:", e)

client = mqtt.Client(
    client_id="AI_SERVER",
    protocol=mqtt.MQTTv311,
    transport="tcp"
)

client.connect(MQTT_BROKER, 1883, 60)
client.subscribe(MQTT_TOPIC)
client.publish("server_to_gateway", "Hello from AI_SERVER")
client.on_message = on_message
client.loop_forever()
