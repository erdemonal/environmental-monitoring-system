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
    health.value = {
      status: 'DOWN',
      time: null,
      db: { status: 'DOWN' }
    }
  } finally {
    loading.value = false
  }
}

async function refreshHealth() {
  try {
    health.value = await getHealth()
  } catch (e) {
    console.error(e)
    health.value = {
      status: 'DOWN',
      time: null,
      db: { status: 'DOWN' }
    }
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

function isOnline() {
  return Boolean(status.value?.online)
}

function lastSeenText() {
  const sec = status.value?.secondsSinceLastSeen
  if (sec == null) return 'Last seen: —'
  if (sec < 60) return `Last seen ${sec}s ago`
  const minutes = Math.floor(sec / 60)
  if (minutes < 60) return `Last seen ${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  return `Last seen ${hours}h ago`
}

onMounted(load)

</script>

<template>
  <div>
    <h2 class="section-title">Device Info</h2>
    <div class="device-info">
      <div class="card">
      <div class="card-title">Device Status</div>
      <p v-if="loading">Loading...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <div v-else>
        <div class="status-line">
          <span :class="['pill', isOnline() ? 'online' : 'offline']">
            {{ isOnline() ? 'Online' : 'Offline' }}
          </span>
          <span class="last-seen">{{ lastSeenText() }}</span>
        </div>
        <div class="row">
          <span>Last reading</span>
          <strong>{{ fmt(status?.lastReadingTime) }}</strong>
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
        <div class="row">
          <span>App Status</span>
          <strong>DOWN</strong>
        </div>
        <div class="row">
          <span>Data Fetch Time</span>
          <strong>N/A</strong>
        </div>
        <div class="row">
          <span>DB Status</span>
          <strong>DOWN</strong>
        </div>
      </div>
      <div v-else>
        <div class="row">
          <span>App Status</span>
          <strong>{{ health.status ?? 'DOWN' }}</strong>
        </div>
        <div class="row">
          <span>Data Fetch Time</span>
          <strong>{{ health.time ? new Date(health.time).toLocaleString() : 'N/A' }}</strong>
        </div>
        <div class="row">
          <span>DB Status</span>
          <strong>{{ health.db?.status ?? 'DOWN' }}</strong>
        </div>
        <div class="row">
          <span>Sensor Rows</span>
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
  </div>
</template>

<style scoped>
.section-title {
  margin: 16px 0 12px 0;
  font-size: 20px;
  font-weight: 700;
}
.device-info {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
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
.status-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.pill {
  padding: 2px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}
.pill.online {
  background: #d1fae5;
  color: #065f46;
  border-color: #a7f3d0;
}
.pill.offline {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fecaca;
}
.last-seen {
  font-size: 12px;
  color: #6b7280;
}
.row span {
  color: #6b7280;
}
.row strong {
  text-align: right;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
}
.field input {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
}
.success {
  color: #065f46;
  margin-top: 6px;
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

