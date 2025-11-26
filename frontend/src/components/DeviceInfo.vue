<script setup>
import { onMounted, ref } from 'vue'
import { getDeviceStatus, getHealth } from '../api'

const loading = ref(true)
const error = ref('')
const status = ref(null)
const health = ref(null)

const pins = [
  { name: 'Button A', pin: 15, action: 'WiFi info' },
  { name: 'Button B', pin: 32, action: 'Threshold list' },
  { name: 'Button C', pin: 14, action: 'Admin message' },
  { name: 'Light sensor', pin: 34, action: 'TEMT6000 ADC' },
  { name: 'RGB LED', pin: 5, action: 'Status / Alerts' },
  { name: 'OLED I²C', pin: '22 / 23', action: 'Display data' }
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [s, h] = await Promise.all([getDeviceStatus(), getHealth()])
    status.value = s
    health.value = h
  } catch (e) {
    error.value = e?.message || 'Unable to load status'
  } finally {
    loading.value = false
  }
}

async function refreshHealth() {
  try {
    health.value = await getHealth()
  } catch (e) {
    console.error(e)
  }
}

function fmt(ts) {
  return ts ? new Date(ts).toLocaleString() : '—'
}

function alertText() {
  if (!status.value?.lastAlertTime) return '—'
  const base = fmt(status.value.lastAlertTime)
  if (status.value.lastAlertType) {
    return `${base} (${status.value.lastAlertType})`
  }
  return base
}

function adminMessage() {
  return status.value?.lastAdminMessage || '—'
}

onMounted(load)
</script>

<template>
  <div class="device-info">
    <div class="card">
      <div class="card-title">Device Status</div>
      <p v-if="loading">Loading...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <div v-else>
        <div class="row">
          <span>Last reading</span>
          <strong>{{ fmt(status?.lastReadingTime) }}</strong>
        </div>
        <div class="row">
          <span>Device IP</span>
          <strong>{{ status?.lastIpAddress ?? '—' }}</strong>
        </div>
        <div class="row">
          <span>Temperature</span>
          <strong>{{ status?.temperature ?? '—' }} °C</strong>
        </div>
        <div class="row">
          <span>Humidity</span>
          <strong>{{ status?.humidity ?? '—' }} %</strong>
        </div>
        <div class="row">
          <span>CO₂</span>
          <strong>{{ status?.co2 ?? '—' }} ppm</strong>
        </div>
        <div class="row">
          <span>Light</span>
          <strong>{{ status?.lightLevel ?? '—' }} lux</strong>
        </div>
        <div class="row">
          <span>Last alert</span>
          <strong>{{ alertText() }}</strong>
        </div>
        <div class="row">
          <span>Admin msg</span>
          <strong>{{ adminMessage() }}</strong>
        </div>
      </div>
    </div>
    <div class="card">
      <div class="card-title row space-between">
        <span>System Health</span>
        <button class="btn-small" @click="refreshHealth">Get current</button>
      </div>
      <div v-if="!health">
        <p class="muted">Health data not available</p>
      </div>
      <div v-else>
        <div class="row">
          <span>App status</span>
          <strong>{{ health.status }}</strong>
        </div>
        <div class="row">
          <span>Time</span>
          <strong>{{ health.time ? new Date(health.time).toLocaleString() : '—' }}</strong>
        </div>
        <div class="row">
          <span>DB status</span>
          <strong>{{ health.db?.status ?? '—' }}</strong>
        </div>
        <div class="row">
          <span>Sensor rows</span>
          <strong>{{ health.db?.sensorDataCount ?? '—' }}</strong>
        </div>
        <p class="hint">Backend health and DB row count.</p>
      </div>
    </div>
    <div class="card">
      <div class="card-title">Hardware Pins</div>
      <div class="pin-grid">
        <div class="pin-row" v-for="pin in pins" :key="pin.name">
          <div>
            <div class="pin-name">{{ pin.name }}</div>
            <div class="pin-action">{{ pin.action }}</div>
          </div>
          <div class="pin-value">{{ pin.pin }}</div>
        </div>
      </div>
      <p class="hint">Use this as wiring reference when rebuilding the ESP32 circuit.</p>
    </div>
  </div>
</template>

<style scoped>
.device-info {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  margin-top: 12px;
}
.card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
}
.card-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.card-title.row {
  align-items: center;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 14px;
  gap: 8px;
}
.row span {
  color: #6b7280;
}
.row strong {
  text-align: right;
}
.error {
  color: #b91c1c;
}
.pin-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pin-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f3f4f6;
}
.pin-row:last-child {
  border-bottom: none;
}
.pin-name {
  font-weight: 600;
  font-size: 14px;
}
.pin-action {
  font-size: 12px;
  color: #6b7280;
}
.pin-value {
  font-weight: 700;
  font-size: 14px;
}
.btn-small {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  cursor: pointer;
}
.btn-small:hover {
  background: #e5e7eb;
}
.hint {
  margin-top: 6px;
  font-size: 11px;
  color: #6b7280;
}
</style>

