from machine import Pin, I2C, ADC
import time
import network
import json
import uasyncio as asyncio
import gc
from scd4x import SCD4X
from ssd1306 import SSD1306_I2C
import neopixel
import ubluetooth

WIFI_SSID = None
WIFI_PASSWORD = None
BACKEND_IP = None
DEVICE_KEY = None
WEBHOOK_URL = None
CONFIG_FILE = "eg_config.json"
PROV_DEVICE_NAME = "EG-SETUP"
PROV_SERVICE_UUID = ubluetooth.UUID("12345678-1234-5678-1234-56789abc0001")
PROV_CHAR_UUID = ubluetooth.UUID("12345678-1234-5678-1234-56789abc0002")
_IRQ_CENTRAL_CONNECT = 1
_IRQ_CENTRAL_DISCONNECT = 2
_IRQ_GATTS_WRITE = 3
LIGHT_SENSOR_PIN = 34
RGB_PIN = 5
RGB_LED_COUNT = 1
SEND_INTERVAL = 5
THRESHOLD_REFRESH_INTERVAL = 10
SCD41_READ_INTERVAL = 10
BUTTON_A_PIN = 15
BUTTON_B_PIN = 32
BUTTON_C_PIN = 14
BUTTON_DEBOUNCE_MS = 80
BASE_URL = None
THRESHOLD_URL = None
COMMAND_URL = None
BACKEND_URL = None
COMMAND_CHECK_INTERVAL = 10
BLE_COMMAND_CHECK_INTERVAL = 15
SCD41_REINIT_TIMEOUT = 30
THRESHOLD_ALERT_COOLDOWN = 300

i2c = I2C(0, scl=Pin(22, Pin.PULL_UP), sda=Pin(23, Pin.PULL_UP), freq=50000)
time.sleep(1)

scd4x = None
oled = None
light_sensor = None
np = None
last_light_color = (0, 0, 0)
ble = None
ble_broadcasting = False
BLE_NAME = "EG"
last_ble_update = 0
BLE_UPDATE_INTERVAL = 2
last_ble_data = None
wifi_ok = False
thresholds = {}
last_printed_thresholds = None
last_threshold_fetch = 0
current_ip = None
last_admin_message = "No admin message"
oled_override_until = 0
DEFAULT_LED_COLOR = (0, 255, 0)
last_command_check = 0
last_threshold_alert_time = {}

sensor_data = {
    'co2': None,
    'temp': None,
    'hum': None,
    'light': None,
    'breaches': []
}

button_a = None
button_b = None
button_c = None
last_button_press = {"A": 0, "B": 0, "C": 0}
last_send = 0
last_sensor_poll = 0
not_ready_seconds = 0

def load_stored_credentials():
    try:
        with open(CONFIG_FILE, "r") as fp:
            data = fp.read()
            if not data:
                return None
            return json.loads(data)
    except Exception:
        return None

def save_stored_credentials(creds):
    try:
        with open(CONFIG_FILE, "w") as fp:
            fp.write(json.dumps(creds))
        print("Credentials saved")
        return True
    except Exception as e:
        print("Save error:", e)
        return False

def apply_credentials(creds):
    global WIFI_SSID, WIFI_PASSWORD, BACKEND_IP, DEVICE_KEY
    global BASE_URL, THRESHOLD_URL, COMMAND_URL, BACKEND_URL
    WIFI_SSID = creds.get("ssid") or ""
    WIFI_PASSWORD = creds.get("password") or ""
    BACKEND_IP = creds.get("backend_ip") or ""
    DEVICE_KEY = creds.get("device_key") or ""
    BASE_URL = "http://{}:8080".format(BACKEND_IP)
    THRESHOLD_URL = BASE_URL + "/api/device/thresholds"
    COMMAND_URL = BASE_URL + "/api/device/commands"
    BACKEND_URL = BASE_URL + "/api/device/sensor-data"

def _prov_adv_payload(name):
    name_bytes = name.encode("utf-8")
    return bytes(bytearray([len(name_bytes) + 1, 0x09]) + name_bytes)

def provision_via_ble():
    print("BLE provisioning mode...")
    prov_ble = ubluetooth.BLE()
    prov_ble.active(True)
    try:
        prov_ble.config(gap_name=PROV_DEVICE_NAME)
    except Exception:
        pass
    creds_char = (PROV_CHAR_UUID, ubluetooth.FLAG_WRITE | ubluetooth.FLAG_WRITE_NO_RESPONSE | ubluetooth.FLAG_READ)
    prov_service = (PROV_SERVICE_UUID, (creds_char,))
    ((creds_handle,),) = prov_ble.gatts_register_services((prov_service,))
    try:
        prov_ble.gatts_set_buffer(creds_handle, 256, True)
    except Exception:
        pass
    state = {"data": None}
    connected = {"active": False}
    if oled is not None:
        oled.fill(0)
        oled.text("BLE Setup", 0, 0)
        oled.text("Use app", 0, 10)
        oled.show()
    def _irq(event, data):
        if event == _IRQ_GATTS_WRITE:
            conn_handle, attr_handle = data
            if attr_handle != creds_handle:
                return
            try:
                raw = prov_ble.gatts_read(creds_handle)
                payload = raw.decode().strip()
                creds = json.loads(payload)
                required = ("ssid", "password", "backend_ip", "device_key")
                if all(creds.get(k) for k in required):
                    state["data"] = creds
                    print("Credentials received")
            except Exception as e:
                print("Parse error:", e)
        elif event == _IRQ_CENTRAL_CONNECT:
            connected["active"] = True
            print("Client connected")
        elif event == _IRQ_CENTRAL_DISCONNECT:
            connected["active"] = False
            if state["data"] is None:
                try:
                    prov_ble.gap_advertise(250000, adv_data=adv, connectable=True)
                except Exception:
                    pass
    prov_ble.irq(_irq)
    adv = _prov_adv_payload(PROV_DEVICE_NAME)
    prov_ble.gap_advertise(250000, adv_data=adv, connectable=True)
    timeout = 300
    start = time.time()
    while state["data"] is None:
        if time.time() - start > timeout:
            raise TimeoutError("Provisioning timeout")
        time.sleep(0.2)
        gc.collect()
        if not connected["active"]:
            try:
                prov_ble.gap_advertise(250000, adv_data=adv, connectable=True)
            except OSError:
                pass
    prov_ble.gap_advertise(None)
    prov_ble.active(False)
    if oled is not None:
        oled.fill(0)
        oled.text("BLE OK", 0, 0)
        oled.show()
    return state["data"]

def ensure_credentials():
    creds = load_stored_credentials()
    if creds:
        apply_credentials(creds)
        return True
    while True:
        try:
            new_creds = provision_via_ble()
            if new_creds and save_stored_credentials(new_creds):
                apply_credentials(new_creds)
                return True
        except Exception as e:
            print("Provisioning error:", e)
        time.sleep(1)

def set_led(color):
    global last_light_color
    if np is None:
        return
    try:
        np[0] = color
        np.write()
        last_light_color = color
    except Exception:
        pass

async def init_scd41():
    global scd4x
    try:
        devices = i2c.scan()
        if 0x62 in devices:
            scd4x = SCD4X(i2c)
            await asyncio.sleep(2)
            try:
                scd4x.stop_periodic_measurement()
                await asyncio.sleep(1)
            except Exception:
                pass
            scd4x.start_periodic_measurement()
            await asyncio.sleep(3)
            print("SCD41 OK")
        else:
            print("SCD41 not found")
    except Exception as e:
        print("Sensor init error:", e)
        scd4x = None

async def init_oled():
    global oled
    try:
        oled = SSD1306_I2C(128, 32, i2c, addr=0x3C)
        oled.fill(0)
        oled.text("EcoGuard", 0, 0)
        oled.show()
        await asyncio.sleep(1)
        print("OLED OK")
    except Exception as e:
        print("OLED error:", e)
        oled = None

async def init_light_sensor():
    global light_sensor
    try:
        light_sensor = ADC(Pin(LIGHT_SENSOR_PIN))
        light_sensor.atten(ADC.ATTN_11DB)
        light_sensor.width(ADC.WIDTH_12BIT)
        print("Light sensor OK")
    except Exception as e:
        print("Light sensor error:", e)
        light_sensor = None

async def init_rgb_led():
    global np
    try:
        np = neopixel.NeoPixel(Pin(RGB_PIN, Pin.OUT), RGB_LED_COUNT)
        set_led((0, 255, 0))
        print("RGB LED OK")
    except Exception as e:
        print("RGB LED error:", e)
        np = None

def init_button(pin_number):
    if pin_number is None:
        return None
    try:
        return Pin(pin_number, Pin.IN, Pin.PULL_UP)
    except Exception as e:
        print("Button init error:", e)
        return None

async def init_buttons():
    global button_a, button_b, button_c
    button_a = init_button(BUTTON_A_PIN)
    button_b = init_button(BUTTON_B_PIN)
    button_c = init_button(BUTTON_C_PIN)
    await asyncio.sleep(0.1)

async def init_all_sensors():
    print("Init sensors...")
    await asyncio.gather(
        init_scd41(),
        init_oled(),
        init_light_sensor(),
        init_rgb_led(),
        init_buttons()
    )

def init_ble():
    global ble
    try:
        gc.collect()
        time.sleep(0.1)
        ble = ubluetooth.BLE()
        ble.active(True)
        gc.collect()
        time.sleep(0.2)
        print("BLE OK")
        return True
    except Exception as e:
        print("BLE error:", e)
        return False

def ble_adv_payload(name, co2, temp, hum, light):
    try:
        name_bytes = name.encode('utf-8')
        name_len = len(name_bytes)
        co2_val = int(co2) if co2 is not None else 0
        temp_val = round(temp, 1) if temp is not None else 0.0
        hum_val = round(hum, 1) if hum is not None else 0.0
        light_val = int(light) if light is not None else 0
        data_str = "c{}t{}h{}l{}".format(co2_val, temp_val, hum_val, light_val)
        data_bytes = data_str.encode('utf-8')
        data_len = len(data_bytes)
        name_overhead = name_len + 2
        payload = bytearray([name_len + 1, 0x09]) + name_bytes
        manufacturer_overhead = 4
        remaining = 31 - name_overhead
        if data_len + manufacturer_overhead <= remaining:
            payload += bytearray([data_len + 3, 0xFF, 0xFF, 0xFF]) + data_bytes
        return bytes(payload)
    except Exception as e:
        print("BLE payload error:", e)
        return None

def ble_start_broadcast():
    global ble_broadcasting, ble
    if ble is None:
        if not init_ble():
            return False
    gc.collect()
    time.sleep(0.1)
    try:
        try:
            ble.active(True)
        except:
            pass
        ble_broadcasting = True
        payload = ble_adv_payload(BLE_NAME, 0, 0.0, 0.0, 0)
        if payload:
            ble.gap_advertise(500, adv_data=payload, connectable=False)
        gc.collect()
        print("BLE broadcast started")
        return True
    except Exception as e:
        print("BLE start error:", e)
        ble_broadcasting = False
        return False

def ble_stop_broadcast():
    global ble_broadcasting, ble
    try:
        if ble is not None:
            ble.gap_advertise(None)
        ble_broadcasting = False
        gc.collect()
        time.sleep(0.1)
        print("BLE broadcast stopped")
        return True
    except Exception as e:
        print("BLE stop error:", e)
        gc.collect()
        return False

def ble_update_metrics(co2, temp, hum, light):
    global ble, ble_broadcasting, last_ble_update, last_ble_data
    if not ble_broadcasting or ble is None:
        return
    current_time = time.time()
    if (current_time - last_ble_update) < BLE_UPDATE_INTERVAL:
        return
    try:
        gc.collect()
        current_data = (
            int(co2) if co2 is not None else 0,
            round(temp, 1) if temp is not None else 0.0,
            round(hum, 1) if hum is not None else 0.0,
            int(light) if light is not None else 0
        )
        if last_ble_data == current_data:
            return
        payload = ble_adv_payload(BLE_NAME, co2, temp, hum, light)
        if payload:
            ble.gap_advertise(500, adv_data=payload, connectable=False)
            last_ble_update = current_time
            last_ble_data = current_data
            gc.collect()
    except Exception as e:
        print("BLE update error:", e)
        gc.collect()

def format_thresholds(data):
    parts = []
    for metric, values in data.items():
        parts.append("{}(min={}, max={})".format(metric, values.get("min", "-"), values.get("max", "-")))
    return ", ".join(parts) if parts else "None"

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

async def reinit_scd41():
    global scd4x
    if scd4x is None:
        return
    try:
        print("Reinit SCD41...")
        scd4x.stop_periodic_measurement()
        await asyncio.sleep(1)
    except Exception:
        pass
    try:
        scd4x.start_periodic_measurement()
        await asyncio.sleep(3)
        print("SCD41 reinit OK")
    except Exception as e:
        print("SCD41 reinit failed:", e)

def connect_wifi():
    global current_ip
    current_ip = None
    wlan = network.WLAN(network.STA_IF)
    wlan.active(False)
    time.sleep(0.5)
    wlan.active(True)
    time.sleep(1)
    if not wlan.isconnected():
        print("Connecting WiFi:", WIFI_SSID)
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
        if wlan.isconnected():
            ip = wlan.ifconfig()[0]
            current_ip = ip
            print("WiFi OK:", ip)
            if oled is not None:
                oled.fill(0)
                oled.text("WiFi OK", 0, 0)
                oled.text("IP:" + ip[-9:], 0, 10)
                oled.show()
            time.sleep(2)
            return True
        else:
            current_ip = None
            print("WiFi failed")
            if oled is not None:
                oled.fill(0)
                oled.text("WiFi ERROR", 0, 10)
                oled.show()
            return False
    else:
        current_ip = wlan.ifconfig()[0]
        print("WiFi connected:", current_ip)
        return True

def ensure_wifi():
    global wifi_ok, current_ip
    wlan = network.WLAN(network.STA_IF)
    if not wlan.isconnected():
        print("WiFi reconnecting...")
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
        payload = {"alertType": alert_type, "message": message, "timestamp": time.time()}
        if data:
            payload.update(data)
        json_data = json.dumps(payload)
        headers = {"Content-Type": "application/json"}
        response = urequests.post(WEBHOOK_URL, data=json_data, headers=headers, timeout=2)
        response.close()
        print("Webhook sent")
        return True
    except Exception as e:
        print("Webhook error:", e)
        return False

def send_to_backend(co2, temp, hum, light_level=None):
    if ble_broadcasting:
        return False
    response = None
    gc.collect()
    try:
        import urequests
        payload = {"co2Level": int(co2), "temperature": float(temp), "humidity": float(hum)}
        if light_level is not None:
            payload["lightLevel"] = int(light_level)
        json_data = json.dumps(payload)
        headers = {"Content-Type": "application/json", "X-Device-Key": DEVICE_KEY}
        response = urequests.post(BACKEND_URL, data=json_data, headers=headers, timeout=3)
        if response.status_code == 200:
            result = response.json()
            print("Sent OK")
            return True
        else:
            print("Send error:", response.status_code)
            return False
    except Exception as e:
        print("Send error:", e)
        gc.collect()
        return False
    finally:
        if response:
            try:
                response.close()
            except Exception:
                pass
        gc.collect()

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
    log_line = "CO2:{} T:{:.1f} H:{:.1f} L:{}".format(co2, temp, hum, light_level)
    if breach_metrics:
        log_line += " | Breach:{}".format(",".join(breach_metrics))
    print(log_line)

def read_light():
    if light_sensor is None:
        return None
    return light_sensor.read()

def fetch_thresholds():
    if ble_broadcasting:
        return
    global thresholds, last_threshold_fetch
    response = None
    gc.collect()
    try:
        import urequests
        response = urequests.get(THRESHOLD_URL, headers={"X-Device-Key": DEVICE_KEY}, timeout=3)
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
            thresholds = updated
            last_threshold_fetch = time.time()
    except Exception as e:
        print("Threshold fetch error:", e)
        gc.collect()
    finally:
        if response:
            try:
                response.close()
            except Exception:
                pass
        gc.collect()

def get_cmd_id(cmd):
    return cmd.get("id") or cmd.get("commandId") or cmd.get("commandID")

def ack_command(cmd_id):
    try:
        import urequests
        gc.collect()
        base = COMMAND_URL + "/" + str(cmd_id)
        tries = [("PUT", base + "/ack"), ("POST", base + "/ack")]
        for method, url in tries:
            try:
                gc.collect()
                if method == "PUT":
                    r = urequests.put(url, headers={"X-Device-Key": DEVICE_KEY}, timeout=2)
                else:
                    r = urequests.post(url, headers={"X-Device-Key": DEVICE_KEY}, timeout=2)
                r.close()
                if r.status_code in (200, 204):
                    gc.collect()
                    return True
            except Exception:
                gc.collect()
        return False
    except Exception as e:
        print("ACK error:", e)
        gc.collect()
        return False

def fetch_ble_commands_only():
    global last_command_check, ble_broadcasting, last_ble_data
    if not wifi_ok:
        return
    try:
        if ble is not None:
            try:
                ble.gap_advertise(None)
                ble.active(False)
            except:
                pass
        gc.collect()
        time.sleep(0.3)
    except:
        pass
    response = None
    try:
        import urequests
        gc.collect()
        response = urequests.get(COMMAND_URL, headers={"X-Device-Key": DEVICE_KEY}, timeout=3)
        if response.status_code != 200:
            return
        commands = response.json()
        for cmd in commands:
            if cmd.get("commandType") != "BLE_BROADCAST":
                continue
            cmd_id = get_cmd_id(cmd)
            if cmd_id:
                ack_command(cmd_id)
            execute_command(cmd)
            gc.collect()
        last_command_check = time.time()
    except Exception as e:
        print("BLE cmd fetch error:", e)
        gc.collect()
    finally:
        if response:
            try:
                response.close()
            except:
                pass
        gc.collect()
        try:
            if ble is not None and ble_broadcasting:
                try:
                    ble.active(True)
                except:
                    pass
                if last_ble_data:
                    co2, temp, hum, light = last_ble_data
                else:
                    co2, temp, hum, light = 0, 0.0, 0.0, 0
                payload = ble_adv_payload(BLE_NAME, co2, temp, hum, light)
                if payload:
                    ble.gap_advertise(500, adv_data=payload, connectable=False)
            gc.collect()
        except:
            pass

def fetch_and_execute_commands():
    if ble_broadcasting:
        return
    global last_command_check
    response = None
    gc.collect()
    try:
        import urequests
        response = urequests.get(COMMAND_URL, headers={"X-Device-Key": DEVICE_KEY}, timeout=3)
        if response.status_code == 200:
            commands = response.json()
            for cmd in commands:
                cmd_type = cmd.get("commandType")
                cmd_id = get_cmd_id(cmd)
                pre_acked = False
                if cmd_type == "BLE_BROADCAST" and cmd_id:
                    pre_acked = ack_command(cmd_id)
                execute_command(cmd)
                gc.collect()
                if cmd_id and not pre_acked:
                    ack_command(cmd_id)
        last_command_check = time.time()
    except Exception as e:
        print("Command fetch error:", e)
        gc.collect()
    finally:
        if response:
            try:
                response.close()
            except:
                pass
        gc.collect()

def execute_command(cmd):
    cmd_type = cmd.get("commandType")
    params = cmd.get("parameters")
    print("Exec cmd:", cmd_type, params)
    gc.collect()
    if ble_broadcasting and cmd_type != "BLE_BROADCAST":
        return
    if cmd_type == "SET_LED_COLOR":
        try:
            if params:
                parts = params.split(",")
                if len(parts) == 3:
                    r = max(0, min(int(parts[0].strip()), 255))
                    g = max(0, min(int(parts[1].strip()), 255))
                    b = max(0, min(int(parts[2].strip()), 255))
                    flash_led((r, g, b))
        except Exception as e:
            print("LED cmd error:", e)
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
            except Exception as e:
                print("Display error:", e)
    elif cmd_type == "REFRESH_CONFIG":
        if not ble_broadcasting and wifi_ok:
            fetch_thresholds()
    elif cmd_type == "BLE_BROADCAST":
        if params:
            params_lower = params.lower().strip()
            if params_lower == "start":
                ble_start_broadcast()
            elif params_lower == "stop":
                ble_stop_broadcast()
            elif params_lower == "toggle":
                if ble_broadcasting:
                    ble_stop_broadcast()
                else:
                    ble_start_broadcast()
        else:
            if ble_broadcasting:
                ble_stop_broadcast()
            else:
                ble_start_broadcast()
        gc.collect()
        time.sleep(0.3)

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

def evaluate_thresholds(temp, hum, co2, light_level):
    global last_threshold_alert_time
    breaches = []
    current_time = time.time()
    if is_metric_outside("TEMP", temp):
        breaches.append("TEMP")
        if "TEMP" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["TEMP"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification("THRESHOLD", "Temperature threshold breached", {"metric": "TEMP", "value": temp, "unit": "C"})
            last_threshold_alert_time["TEMP"] = current_time
    if is_metric_outside("HUMIDITY", hum):
        breaches.append("HUMIDITY")
        if "HUMIDITY" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["HUMIDITY"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification("THRESHOLD", "Humidity threshold breached", {"metric": "HUMIDITY", "value": hum, "unit": "%"})
            last_threshold_alert_time["HUMIDITY"] = current_time
    if is_metric_outside("CO2", co2):
        breaches.append("CO2")
        if "CO2" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["CO2"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification("THRESHOLD", "CO2 threshold breached", {"metric": "CO2", "value": co2, "unit": "ppm"})
            last_threshold_alert_time["CO2"] = current_time
    if is_metric_outside("LIGHT", light_level):
        breaches.append("LIGHT")
        if "LIGHT" not in last_threshold_alert_time or (current_time - last_threshold_alert_time["LIGHT"]) >= THRESHOLD_ALERT_COOLDOWN:
            send_webhook_notification("THRESHOLD", "Light threshold breached", {"metric": "LIGHT", "value": light_level, "unit": "lux"})
            last_threshold_alert_time["LIGHT"] = current_time
    return breaches

async def sensor_reading_task():
    not_ready_seconds = 0
    while True:
        try:
            if scd4x is None:
                await asyncio.sleep(5)
                continue
            if scd4x.data_ready:
                co2, temp, hum = scd4x.measurement
                light_level = read_light()
                sensor_data['co2'] = co2
                sensor_data['temp'] = temp
                sensor_data['hum'] = hum
                sensor_data['light'] = light_level
                sensor_data['breaches'] = evaluate_thresholds(temp, hum, co2, light_level)
                not_ready_seconds = 0
            else:
                not_ready_seconds += 1
                if not_ready_seconds >= 3:
                    await reinit_scd41()
                    not_ready_seconds = 0
        except Exception as e:
            print("Sensor task error:", e)
            await reinit_scd41()
            not_ready_seconds = 0
        await asyncio.sleep(SCD41_READ_INTERVAL)

async def backend_send_task():
    while True:
        try:
            if ble_broadcasting or not wifi_ok:
                await asyncio.sleep(SEND_INTERVAL)
                continue
            co2 = sensor_data.get('co2')
            temp = sensor_data.get('temp')
            hum = sensor_data.get('hum')
            light = sensor_data.get('light')
            if co2 is not None:
                send_to_backend(co2, temp, hum, light)
        except Exception as e:
            print("Backend task error:", e)
        await asyncio.sleep(SEND_INTERVAL)

async def threshold_fetch_task():
    while True:
        try:
            if not ble_broadcasting and wifi_ok:
                fetch_thresholds()
        except Exception as e:
            print("Threshold task error:", e)
        await asyncio.sleep(THRESHOLD_REFRESH_INTERVAL)

async def command_check_task():
    global last_command_check
    while True:
        try:
            if not wifi_ok:
                await asyncio.sleep(5)
                continue
            interval = BLE_COMMAND_CHECK_INTERVAL if ble_broadcasting else COMMAND_CHECK_INTERVAL
            now = time.time()
            if (now - last_command_check) >= interval:
                if ble_broadcasting:
                    fetch_ble_commands_only()
                else:
                    fetch_and_execute_commands()
        except Exception as e:
            print("Command task error:", e)
        await asyncio.sleep(5)

async def ble_update_task():
    while True:
        try:
            if ble_broadcasting:
                co2 = sensor_data.get('co2', 0)
                temp = sensor_data.get('temp', 0.0)
                hum = sensor_data.get('hum', 0.0)
                light = sensor_data.get('light', 0)
                ble_update_metrics(co2, temp, hum, light)
        except Exception as e:
            print("BLE task error:", e)
        await asyncio.sleep(BLE_UPDATE_INTERVAL)

async def display_task():
    while True:
        try:
            handle_buttons()
            if oled_override_until <= time.time():
                co2 = sensor_data.get('co2')
                temp = sensor_data.get('temp')
                hum = sensor_data.get('hum')
                light = sensor_data.get('light')
                breaches = sensor_data.get('breaches', [])
                if co2 is not None and oled is not None:
                    readings(co2, temp, hum, light, breaches)
                    if breaches:
                        set_led((255, 0, 0))
                    else:
                        set_led(DEFAULT_LED_COLOR)
        except Exception as e:
            print("Display task error:", e)
        await asyncio.sleep(0.5)

async def wifi_monitor_task():
    while True:
        try:
            ensure_wifi()
        except Exception as e:
            print("WiFi task error:", e)
        await asyncio.sleep(30)

async def main():
    print("EcoGuard async starting...")
    await init_all_sensors()
    ensure_wifi()
    if wifi_ok:
        fetch_thresholds()
    await asyncio.gather(
        sensor_reading_task(),
        backend_send_task(),
        threshold_fetch_task(),
        command_check_task(),
        ble_update_task(),
        display_task(),
        wifi_monitor_task()
    )

print("Starting EcoGuard...")
ensure_credentials()
wifi_ok = connect_wifi()
if not wifi_ok:
    print("WARNING: No WiFi!")
else:
    fetch_thresholds()

try:
    asyncio.run(main())
except KeyboardInterrupt:
    print("Stopped")
except Exception as e:
    print("Fatal error:", e)
    