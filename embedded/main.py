from machine import Pin, I2C, ADC
import time
import network
import json
from scd4x import SCD4X
from ssd1306 import SSD1306_I2C
import neopixel

WIFI_SSID = "erdem"
WIFI_PASSWORD = "erdem124"
BACKEND_IP = "172.20.10.2"
DEVICE_KEY = "demo-device-key"
WEBHOOK_URL = None
LIGHT_SENSOR_PIN = 34
RGB_PIN = 5
RGB_LED_COUNT = 1
SEND_INTERVAL = 5
THRESHOLD_REFRESH_INTERVAL = 10
LOOP_DELAY = 0.05  
SCD41_READ_INTERVAL = 10
BUTTON_A_PIN = 15
BUTTON_B_PIN = 32
BUTTON_C_PIN = 14
BUTTON_DEBOUNCE_MS = 80
BASE_URL = "http://" + BACKEND_IP + ":8080"
THRESHOLD_URL = BASE_URL + "/api/device/thresholds"
COMMAND_URL = BASE_URL + "/api/device/commands"
COMMAND_CHECK_INTERVAL = 10
SCD41_REINIT_TIMEOUT = 30
i2c = I2C(0, scl=Pin(22, Pin.PULL_UP), sda=Pin(23, Pin.PULL_UP), freq=50000)
time.sleep(1)

print("Initializing sensors...")
scd4x = None
try:
    devices = i2c.scan()
    print("I2C devices:", [hex(d) for d in devices])

    if 0x62 in devices:
        scd4x = SCD4X(i2c)
        time.sleep(2)
        try:
            scd4x.stop_periodic_measurement()
            time.sleep(1)
        except Exception:
            pass
        scd4x.start_periodic_measurement()
        time.sleep(3)
        print("SCD41 initialized")
    else:
        print("ERROR: SCD41 not found on I2C bus!")
except Exception as e:
    print("Sensor init error:", e)
    print("Continuing without sensor...")
    scd4x = None

try:
    oled = SSD1306_I2C(128, 32, i2c, addr=0x3C)
    oled.fill(0)
    oled.text("EcoGuard", 0, 0)
    oled.show()
    time.sleep(1)
    print("OLED initialized")
except Exception as e:
    print("OLED init error:", e)
    oled = None

try:
    light_sensor = ADC(Pin(LIGHT_SENSOR_PIN))
    light_sensor.atten(ADC.ATTN_11DB)
    light_sensor.width(ADC.WIDTH_12BIT)
    print("Light sensor initialized")
except Exception as e:
    print("Light sensor error:", e)
    light_sensor = None

try:
    np = neopixel.NeoPixel(Pin(RGB_PIN, Pin.OUT), RGB_LED_COUNT)

    def set_led(color):
        global last_light_color
        np[0] = color
        np.write()
        last_light_color = color

    set_led((0, 5, 0))
    print("RGB LED initialized")
except Exception as e:
    print("RGB LED init error:", e)
    np = None

    def set_led(color):
        return

    last_light_color = (0, 0, 0)

wifi_ok = False
thresholds = {}
last_printed_thresholds = None
last_threshold_fetch = 0
current_ip = None
last_admin_message = "No admin message"
oled_override_until = 0
DEFAULT_LED_COLOR = (0, 5, 0)

def format_thresholds(data):
    parts = []
    for metric, values in data.items():
        parts.append("{}(min={}, max={})".format(
            metric,
            values.get("min", "-"),
            values.get("max", "-")
        ))
    return ", ".join(parts) if parts else "None"

def light_to_color(light_value):
    if light_value is None:
        return (0, 0, 40)
    bucket = max(0, min(4000, int(light_value)))
    step = bucket // 100  
    ratio = step / 40
    ratio = max(0.0, min(1.0, ratio))
    if ratio <= 0.5:
        blend = ratio / 0.5
        r = 0
        g = int(80 + 150 * blend)
        b = int(220 - 140 * blend)
    else:
        blend = (ratio - 0.5) / 0.5
        r = int(60 + 195 * blend)
        g = int(230 - 180 * blend)
        b = 0
    return (min(max(r, 0), 255), min(max(g, 0), 255), min(max(b, 0), 255))

def flash_led(color, flashes=3, delay=0.18):
    global last_light_color
    if np is None:
        return
    try:
        for _ in range(flashes):
            set_led(color)
            time.sleep(delay)
            set_led(last_light_color)
            time.sleep(delay)
    except Exception as e:
        print("Flash LED error:", e)
    finally:
        # Flash bittikten sonra her zaman default renge dön
        try:
            set_led(DEFAULT_LED_COLOR)
        except Exception:
            pass

def set_oled_override(renderer, duration=5):
    global oled_override_until
    if oled is None:
        return
    renderer()
    oled_override_until = time.time() + duration

def display_network_info():
    if oled is None:
        return
    ip = current_ip or "No IP"
    oled.fill(0)
    oled.text("WiFi", 0, 0)
    oled.text(WIFI_SSID[:16], 0, 10)
    oled.text("IP:{}".format(ip[-12:]), 0, 20)
    oled.show()

def display_thresholds_info():
    if oled is None:
        return
    oled.fill(0)
    oled.text("THR", 0, 0)

    def fmt_range(metric):
        info = thresholds.get(metric)
        if not info:
            return "-"
        min_v = "-" if info.get("min") is None else str(int(info["min"]))
        max_v = "-" if info.get("max") is None else str(int(info["max"]))
        return "{}-{}".format(min_v, max_v)

    t_txt = fmt_range("TEMP")
    h_txt = fmt_range("HUMIDITY")
    c_txt = fmt_range("CO2")
    l_txt = fmt_range("LIGHT")

    oled.text("T:{} H:{}".format(t_txt, h_txt)[:16], 0, 10)
    oled.text("C:{} L:{}".format(c_txt, l_txt)[:16], 0, 20)
    oled.show()

def display_admin_message():
    if oled is None:
        return
    msg = last_admin_message or "No admin msg"
    oled.fill(0)
    oled.text("Admin", 0, 0)
    oled.text(msg[:16], 0, 10)
    if len(msg) > 16:
        oled.text(msg[16:32], 0, 20)
    oled.show()

def init_button(pin_number):
    if pin_number is None:
        return None
    try:
        return Pin(pin_number, Pin.IN, Pin.PULL_UP)
    except Exception as e:
        print("Button init error on pin {}: {}".format(pin_number, e))
        return None

button_a = init_button(BUTTON_A_PIN)
button_b = init_button(BUTTON_B_PIN)
button_c = init_button(BUTTON_C_PIN)
last_button_press = {"A": 0, "B": 0, "C": 0}

def button_pressed(btn):
    return btn is not None and btn.value() == 0

def handle_buttons():
    now = time.ticks_ms()
    if button_pressed(button_a):
        if time.ticks_diff(now, last_button_press["A"]) > BUTTON_DEBOUNCE_MS:
            last_button_press["A"] = now
            set_oled_override(display_network_info, duration=3)
    if button_pressed(button_b):
        if time.ticks_diff(now, last_button_press["B"]) > BUTTON_DEBOUNCE_MS:
            last_button_press["B"] = now
            set_oled_override(display_thresholds_info, duration=3)
    if button_pressed(button_c):
        if time.ticks_diff(now, last_button_press["C"]) > BUTTON_DEBOUNCE_MS:
            last_button_press["C"] = now
            set_oled_override(display_admin_message, duration=4)
def reinit_scd41():
    global scd4x
    if scd4x is None:
        return
    try:
        print("Reinitializing SCD41...")
        scd4x.stop_periodic_measurement()
        time.sleep(1)
    except Exception:
        pass
    try:
        scd4x.start_periodic_measurement()
        time.sleep(3)
        print("SCD41 reinit complete")
    except Exception as e:
        print("SCD41 reinit failed:", e)

BACKEND_URL = BASE_URL + "/api/device/sensor-data"
def connect_wifi():
    global current_ip
    current_ip = None
    wlan = network.WLAN(network.STA_IF)
    wlan.active(False)
    time.sleep(0.5)
    wlan.active(True)
    time.sleep(1)
    
    if not wlan.isconnected():
        print("Connecting to WiFi: " + WIFI_SSID)
        if oled is not None:
            oled.fill(0)
            oled.text("WiFi...", 0, 0)
            oled.show()
        
        try:
            wlan.disconnect()
            time.sleep(0.5)
        except:
            pass
        
        wlan.connect(WIFI_SSID, WIFI_PASSWORD)
        
        for i in range(20):
            if wlan.isconnected():
                break
            time.sleep(1)
            print(".", end="")
        
        if wlan.isconnected():
            ip = wlan.ifconfig()[0]
            current_ip = ip
            print("\nWiFi connected! IP: " + ip)
            if oled is not None:
                oled.fill(0)
                oled.text("WiFi OK", 0, 0)
                oled.text("IP: " + ip[-9:], 0, 10)
                oled.show()
            time.sleep(2)
            return True
        else:
            current_ip = None
            print("\nWiFi connection failed!")
            if oled is not None:
                oled.fill(0)
                oled.text("WiFi ERROR", 0, 10)
                oled.show()
            return False
    else:
        current_ip = wlan.ifconfig()[0]
        print("WiFi already connected. IP: " + current_ip)
        return True

def ensure_wifi():
    global wifi_ok, current_ip
    wlan = network.WLAN(network.STA_IF)
    if not wlan.isconnected():
        print("WiFi lost, reconnecting...")
        wifi_ok = connect_wifi()
    else:
        wifi_ok = True
        current_ip = wlan.ifconfig()[0]
    return wifi_ok

def send_webhook_notification(alert_type, message, data=None):
    if WEBHOOK_URL is None:
        return False
    try:
        import urequests
        payload = {
            "alertType": alert_type,
            "message": message,
            "timestamp": time.time()
        }
        if data:
            payload.update(data)
        json_data = json.dumps(payload)
        headers = {"Content-Type": "application/json"}
        response = urequests.post(WEBHOOK_URL, data=json_data, headers=headers, timeout=3)
        response.close()
        print("Webhook sent: " + alert_type)
        return True
    except Exception as e:
        print("Webhook error: " + str(e))
        return False

def send_to_backend(co2, temp, hum, light_level=None):
    response = None
    try:
        import urequests
        
        payload = {
            "co2Level": int(co2),
            "temperature": float(temp),
            "humidity": float(hum)
        }
        if light_level is not None:
            payload["lightLevel"] = int(light_level)
        
        json_data = json.dumps(payload)
        headers = {
            "Content-Type": "application/json",
            "X-Device-Key": DEVICE_KEY
        }
        
        print("Sending to backend...")
        response = urequests.post(BACKEND_URL, data=json_data, headers=headers)
        
        if response.status_code == 200:
            result = response.json()
            print("Success! ID: " + str(result.get("sensorDataId", "?")))
            return True
        else:
            print("Error! Code: " + str(response.status_code))
            return False
            
    except ImportError:
        print("urequests not installed! Install: upip install micropython-urequests")
        return False
    except Exception as e:
        print("Error: " + str(e))
        return False
    finally:
        if response:
            try:
                response.close()
            except Exception:
                pass

def readings(co2, temp, hum, light_level, breach_metrics=None):
    override_active = oled_override_until > time.time()
    if oled is not None and not override_active:
        oled.fill(0)
        status_label = "OK"
        if breach_metrics:
            status_label = "ALRT {}".format(",".join(breach_metrics)[:6])
        oled.text(status_label[:12], 0, 0)
        oled.text("T{} H{}".format(int(temp), int(hum))[:16], 0, 10)
        oled.text("C{} L{}".format(int(co2), light_level if light_level is not None else "-")[:16], 0, 20)
        oled.show()
    log_line = "CO2: {} ppm | Temp: {:.1f}C | Hum: {:.1f}% | Light: {}".format(
        co2, temp, hum, light_level)
    if breach_metrics:
        log_line = "{} | Breach: {}".format(log_line, ",".join(breach_metrics))
    print(log_line)

def read_light():
    if light_sensor is None:
        return None
    return light_sensor.read()

def fetch_thresholds():
    global thresholds, last_threshold_fetch
    response = None
    try:
        import urequests
        response = urequests.get(THRESHOLD_URL, headers={"X-Device-Key": DEVICE_KEY})
        if response.status_code == 200:
            data = response.json()
            updated = {}
            for item in data:
                metric = item.get("metricType")
                if not metric:
                    continue
                min_value = item.get("minValue")
                max_value = item.get("maxValue")
                updated[metric.upper()] = {
                    "min": float(min_value) if min_value is not None else None,
                    "max": float(max_value) if max_value is not None else None
                }
            if thresholds != updated:
                print("Threshold change detected. New thresholds:", format_thresholds(updated))
                thresholds = updated
            last_threshold_fetch = time.time()
        else:
            print("Failed to fetch thresholds:", response.status_code)
    except Exception as e:
        print("Threshold fetch error:", e)
    finally:
        if response:
            try:
                response.close()
            except Exception:
                pass

last_command_check = 0

def fetch_and_execute_commands():
    global last_command_check
    response = None
    try:
        import urequests
        response = urequests.get(COMMAND_URL, headers={"X-Device-Key": DEVICE_KEY})
        if response.status_code == 200:
            commands = response.json()
            for cmd in commands:
                execute_command(cmd)
                cmd_id = cmd.get("id")
                if cmd_id:
                    ack_response = None
                    try:
                        ack_response = urequests.put(
                            COMMAND_URL + "/" + str(cmd_id) + "/ack",
                            headers={"X-Device-Key": DEVICE_KEY}
                        )
                    except Exception:
                        pass
                    finally:
                        if ack_response:
                            try:
                                ack_response.close()
                            except Exception:
                                pass
        last_command_check = time.time()
    except Exception as e:
        print("Command fetch error:", e)
    finally:
        if response:
            try:
                response.close()
            except Exception:
                pass

def execute_command(cmd):
    cmd_type = cmd.get("commandType")
    params = cmd.get("parameters")
    print("Executing command:", cmd_type, params)
    
    if cmd_type == "SET_LED_COLOR":
        try:
            if params:
                parts = params.split(",")
                if len(parts) == 3:
                    r = int(parts[0].strip())
                    g = int(parts[1].strip())
                    b = int(parts[2].strip())
                    r = max(0, min(r, 255))
                    g = max(0, min(g, 255))
                    b = max(0, min(b, 255))
                    # Admin rengi sadece kısa bir flash olarak kullan
                    flash_led((r, g, b))
                    print("LED flash color:", r, g, b)
        except Exception as e:
            print("LED command error:", e)
    
    elif cmd_type == "DISPLAY_MESSAGE":
        global last_admin_message
        if params:
            last_admin_message = params
        if params and oled is not None:
            try:
                oled.fill(0)
                oled.text(params[:16], 0, 0)
                if len(params) > 16:
                    oled.text(params[16:32], 0, 10)
                oled.show()
                set_oled_override(display_admin_message, duration=4)
                print("Display message:", params)
            except Exception as e:
                print("Display error:", e)

def is_metric_outside(metric, value):
    info = thresholds.get(metric)
    if not info or value is None:
        return False
    min_value = info.get("min")
    max_value = info.get("max")
    if min_value is not None and value < min_value:
        return True
    if max_value is not None and value > max_value:
        return True
    return False

last_threshold_alert_time = {}
THRESHOLD_ALERT_COOLDOWN = 300

def evaluate_thresholds(temp, hum, co2, light_level):
    global last_threshold_alert_time
    breaches = []
    current_time = time.time()
    
    if is_metric_outside("TEMP", temp):
        breaches.append("TEMP")
        if "TEMP" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["TEMP"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification(
                "THRESHOLD",
                "Temperature threshold breached",
                {"metric": "TEMP", "value": temp, "unit": "C"}
            )
            last_threshold_alert_time["TEMP"] = current_time
    
    if is_metric_outside("HUMIDITY", hum):
        breaches.append("HUMIDITY")
        if "HUMIDITY" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["HUMIDITY"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification(
                "THRESHOLD",
                "Humidity threshold breached",
                {"metric": "HUMIDITY", "value": hum, "unit": "%"}
            )
            last_threshold_alert_time["HUMIDITY"] = current_time
    
    if is_metric_outside("CO2", co2):
        breaches.append("CO2")
        if "CO2" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["CO2"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification(
                "THRESHOLD",
                "CO2 threshold breached",
                {"metric": "CO2", "value": co2, "unit": "ppm"}
            )
            last_threshold_alert_time["CO2"] = current_time

    if is_metric_outside("LIGHT", light_level):
        breaches.append("LIGHT")
        if "LIGHT" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["LIGHT"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification(
                "THRESHOLD",
                "Light threshold breached",
                {"metric": "LIGHT", "value": light_level, "unit": "lux"}
            )
            last_threshold_alert_time["LIGHT"] = current_time
    
    return breaches


print("Starting EcoGuard...")

wifi_ok = connect_wifi()
if not wifi_ok:
    print("WARNING: No WiFi, data will not be sent to backend!")
else:
    fetch_thresholds()

print("Reading SCD41...")
last_send = 0
last_sensor_poll = 0
not_ready_seconds = 0

if scd4x is None:
    print("ERROR: Sensor not initialized!")

while True:
    try:
        handle_buttons()
        ensure_wifi()
        if wifi_ok and (time.time() - last_command_check) >= COMMAND_CHECK_INTERVAL:
            fetch_and_execute_commands()
        if scd4x is None:
            if oled is not None:
                oled.fill(0)
                oled.text("Sensor ERROR", 0, 10)
                oled.show()
            time.sleep(5)
            continue
            
        current_time = time.time()
        if (current_time - last_sensor_poll) >= SCD41_READ_INTERVAL:
            last_sensor_poll = current_time

            if scd4x.data_ready:
                if wifi_ok and (current_time - last_threshold_fetch) >= THRESHOLD_REFRESH_INTERVAL:
                    fetch_thresholds()
                if thresholds and thresholds != last_printed_thresholds:
                    print("Thresholds changed:", format_thresholds(thresholds))
                    last_printed_thresholds = dict(thresholds)

                co2, temp, hum = scd4x.measurement
                light_level = read_light()
                threshold_breaches = evaluate_thresholds(temp, hum, co2, light_level)
                not_ready_seconds = 0

                if threshold_breaches:
                    set_led((5, 0, 0))
                else:
                    set_led(DEFAULT_LED_COLOR)

                readings(co2, temp, hum, light_level, threshold_breaches)

                measurement_time = time.time()
                if wifi_ok and (measurement_time - last_send) >= SEND_INTERVAL:
                    success = send_to_backend(co2, temp, hum, light_level)
                    last_send = measurement_time
                    if oled is not None and oled_override_until <= time.time():
                        oled.fill(0)
                        status_line = "OK" if success else "SEND ERR"
                        if threshold_breaches:
                            status_line = "ALRT {}".format(",".join(threshold_breaches)[:6])
                        oled.text(status_line[:12], 0, 0)
                        oled.text("T{} H{}".format(int(temp), int(hum))[:16], 0, 10)
                        oled.text("C{} L{}".format(int(co2), light_level if light_level is not None else "-")[:16], 0, 20)
                        oled.show()
            else:
                if oled is not None:
                    oled.fill(0)
                    oled.text("Waiting...", 0, 10)
                    oled.show()
                print("SCD41 not ready...")
                not_ready_seconds += SCD41_READ_INTERVAL
                if not_ready_seconds >= SCD41_REINIT_TIMEOUT:
                    reinit_scd41()
                    not_ready_seconds = 0
    except Exception as e:
        if oled is not None:
            oled.fill(0)
            oled.text("Sensor ERROR", 0, 10)
            oled.show()
        print("Sensor error:", e)
        time.sleep(5)
        reinit_scd41()
        not_ready_seconds = 0

    time.sleep(LOOP_DELAY)