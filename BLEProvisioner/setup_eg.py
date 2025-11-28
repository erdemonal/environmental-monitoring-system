import asyncio, json
from bleak import BleakScanner, BleakClient
from bleak.exc import BleakError

TARGET_NAME = "EG-SETUP"
CREDS_CHAR_UUID = "12345678-1234-5678-1234-56789abc0002"

CREDS = {
    "ssid": "x",
    "password": "x",
    "backend_ip": "x",
    "device_key": "demo-device-key"
}

SCAN_TIMEOUT = 8

async def ensure_services(client: BleakClient):
    if hasattr(client, "get_services"):
        try:
            return await client.get_services()
        except TypeError:
            pass
    if hasattr(client, "fetch_services"):
        await client.fetch_services()
        return client.services
    return client.services

async def main():
    print(f"Scanning for {TARGET_NAME} ...")
    devices = await BleakScanner.discover(timeout=SCAN_TIMEOUT)
    target = next((d for d in devices if d.name == TARGET_NAME), None)
    if not target:
        raise SystemExit("EG-SETUP not found")

    payload = json.dumps(CREDS).encode("utf-8")

    print("\nConnecting:", target.address)
    async with BleakClient(target.address) as client:
        print("Connected")

        services = await ensure_services(client)
        if services is None:
            raise BleakError("Service discovery failed")

        creds_char = None
        print("\nDiscovered services/characteristics:")
        for s in services:
            print("SERVICE", s.uuid)
            for c in s.characteristics:
                print("  CHAR", c.uuid, c.properties)
                if str(c.uuid).lower() == CREDS_CHAR_UUID.lower():
                    creds_char = c

        if creds_char is None:
            raise SystemExit("Provisioning characteristic not found")

        print("\nWriting credentials...")
        await client.write_gatt_char(creds_char, payload, response=False)
        print("Credentials sent")

if __name__ == "__main__":
    asyncio.run(main())
