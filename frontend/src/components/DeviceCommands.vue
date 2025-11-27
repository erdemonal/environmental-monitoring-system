<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { sendDeviceCommand, getDeviceCommands } from '../api'

const deviceKey = ref('demo-device-key')
const commandType = ref('SET_LED_COLOR')
const parameters = ref('')
const ledR = ref(0)
const ledG = ref(0)
const ledB = ref(0)
const loading = ref(false)
const error = ref('')
const success = ref('')
const commands = ref([])

const commandTypes = [
  { value: 'SET_LED_COLOR', label: 'Flash Light', paramsHint: 'r,g,b (e.g., 255,0,0)' },
  { value: 'DISPLAY_MESSAGE', label: 'Display Message', paramsHint: 'message text' },
  { value: 'BLE_BROADCAST', label: 'BLE Broadcast', paramsHint: 'start / stop / toggle' }
]

function clampColor(val) {
  const num = Number(val)
  if (Number.isNaN(num)) return 0
  return Math.min(255, Math.max(0, Math.floor(num)))
}

async function send(params = null) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    if (commandType.value === 'SET_LED_COLOR') {
      if ([ledR.value, ledG.value, ledB.value].some(v => Number(v) < 0)) {
        alert('LED color values cannot be negative')
        return
      }
    }
    const finalParams = commandType.value === 'SET_LED_COLOR'
      ? `${clampColor(ledR.value)},${clampColor(ledG.value)},${clampColor(ledB.value)}`
      : (params !== null ? params : (parameters.value || null))
    await sendDeviceCommand(deviceKey.value, commandType.value, finalParams)
    success.value = 'Command sent successfully!'
    if (commandType.value !== 'SET_LED_COLOR') {
      parameters.value = ''
    }
    await loadCommands()
  } catch (e) {
    error.value = e?.message || 'Failed to send command'
  } finally {
    loading.value = false
  }
}

async function sendBleCommand(action) {
  await send(action)
}

async function loadCommands() {
  try {
    commands.value = await getDeviceCommands(deviceKey.value) || []
  } catch (e) {
    console.error('Failed to load commands:', e)
  }
}

function getCommandLabel(type) {
  return commandTypes.find(c => c.value === type)?.label || type
}

function formatDate(ts) {
  return ts ? new Date(ts).toLocaleString() : '—'
}

function statusHint(cmd) {
  if (cmd.executed) {
    return cmd.executedAt ? `Ack ${new Date(cmd.executedAt).toLocaleTimeString()}` : 'Acked'
  }
  if (!cmd.createdAt) return 'Waiting'
  const seconds = Math.max(0, Math.round((Date.now() - new Date(cmd.createdAt).getTime()) / 1000))
  if (seconds < 60) return `Waiting ${seconds}s`
  const minutes = Math.floor(seconds / 60)
  return `Waiting ${minutes}m`
}

let refreshInterval = null
onMounted(() => {
  loadCommands()
  refreshInterval = setInterval(loadCommands, 5000)
})
onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})
</script>

<template>
  <div>
    <h2>Device Commands</h2>
    <p class="muted">Send commands to embedded device</p>
    
    <div class="form">
      <div class="field">
        <label>Device Key</label>
        <input v-model="deviceKey" placeholder="demo-device-key" />
      </div>
      <div class="field">
        <label>Command Type</label>
        <select v-model="commandType">
          <option v-for="cmd in commandTypes" :key="cmd.value" :value="cmd.value">
            {{ cmd.label }}
          </option>
        </select>
      </div>
      <div class="field" v-if="commandType === 'SET_LED_COLOR'">
        <label>Parameters</label>
        <div class="led-inputs">
          <input type="number" min="0" max="255" v-model.number="ledR" />
          <span>,</span>
          <input type="number" min="0" max="255" v-model.number="ledG" />
          <span>,</span>
          <input type="number" min="0" max="255" v-model.number="ledB" />
        </div>
      </div>
      <div class="field" v-else-if="commandType === 'BLE_BROADCAST'">
        <label>BLE Broadcast Control</label>
        <div class="ble-buttons">
          <button class="btn ble-start" @click="sendBleCommand('start')" :disabled="loading">
            Start
          </button>
          <button class="btn ble-toggle" @click="sendBleCommand('toggle')" :disabled="loading">
            Toggle
          </button>
          <button class="btn ble-stop" @click="sendBleCommand('stop')" :disabled="loading">
            Stop
          </button>
        </div>
        <div class="cheat-sheet">
          <strong>Cheat Sheet:</strong>
          <ul>
            <li><strong>Start:</strong> Starts BLE broadcast. Sensor data is transmitted via Bluetooth.</li>
            <li><strong>Toggle:</strong> Turns BLE off if on, turns on if off.</li>
            <li><strong>Stop:</strong> Stops BLE broadcast. WiFi operations resume.</li>
          </ul>
        </div>
      </div>
      <div class="field" v-else>
        <label>Parameters</label>
        <input v-model="parameters" :placeholder="commandTypes.find(c => c.value === commandType)?.paramsHint || 'parameters'" />
      </div>
      <button class="btn primary" @click="send" :disabled="loading" v-if="commandType !== 'BLE_BROADCAST'">
        {{ loading ? 'Sending...' : 'Send Command' }}
      </button>
    </div>

    <p v-if="error" style="color:#b91c1c;margin-top:8px">{{ error }}</p>
    <p v-if="success" style="color:#059669;margin-top:8px">{{ success }}</p>

    <div class="panel" style="margin-top:16px">
      <div class="panel-title">Recent Commands</div>
      <div v-if="commands.length === 0" class="muted">No commands yet</div>
      <div v-else class="table">
        <div class="thead grid4">
          <div>Type</div>
          <div>Parameters</div>
          <div>Status</div>
          <div>Time</div>
        </div>
        <div class="tbody">
          <div class="row grid4" v-for="cmd in commands" :key="cmd.id">
            <div>{{ getCommandLabel(cmd.commandType) }}</div>
            <div>{{ cmd.parameters || '—' }}</div>
            <div>
              <div class="status-cell">
                <span :class="cmd.executed ? 'badge success' : 'badge pending'">
                  {{ cmd.executed ? 'Delivered' : 'Pending' }}
                </span>
                <small>{{ statusHint(cmd) }}</small>
              </div>
            </div>
            <div class="small">
              <div>Sent {{ formatDate(cmd.createdAt) }}</div>
              <div v-if="cmd.executedAt">Ack {{ formatDate(cmd.executedAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  -moz-appearance: textfield;
  appearance: textfield;
}

.muted { color: #6b7280; margin-top: -4px; margin-bottom: 12px; }
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}
input, select {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
}
.btn.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
  padding: 10px 16px;
  width: auto;
  align-self: flex-start;
}
.btn.primary:hover {
  background: #1d4ed8;
}
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.led-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}
.led-inputs input {
  width: 80px;
  text-align: center;
}
.led-inputs span {
  user-select: none;
  font-weight: 600;
}
.table {
  display: grid;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.thead, .row {
  display: grid;
  gap: 8px;
}
.grid4 {
  grid-template-columns: 2fr 2fr 1fr 1.5fr;
}
.thead {
  background: #f3f4f6;
  padding: 10px 12px;
  font-weight: 600;
}
.tbody .row {
  padding: 10px 12px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}
.badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #f3f4f6;
  color: #374151;
}
.badge.success {
  background: #d1fae5;
  border-color: #a7f3d0;
  color: #065f46;
}
.badge.pending {
  background: #fef3c7;
  border-color: #fde68a;
  color: #92400e;
}
.status-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.status-cell small {
  font-size: 11px;
  color: #6b7280;
}
.small {
  font-size: 12px;
  color: #6b7280;
}
.ble-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.ble-buttons .btn {
  flex: 1;
  min-width: 80px;
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.ble-buttons .btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ble-buttons .btn.ble-start {
  background: #10b981;
  color: #fff;
  border-color: #10b981;
}
.ble-buttons .btn.ble-start:hover:not(:disabled) {
  background: #059669;
  border-color: #059669;
}
.ble-buttons .btn.ble-toggle {
  background: #f59e0b;
  color: #fff;
  border-color: #f59e0b;
}
.ble-buttons .btn.ble-toggle:hover:not(:disabled) {
  background: #d97706;
  border-color: #d97706;
}
.ble-buttons .btn.ble-stop {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}
.ble-buttons .btn.ble-stop:hover:not(:disabled) {
  background: #dc2626;
  border-color: #dc2626;
}
.cheat-sheet {
  margin-top: 12px;
  padding: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
}
.cheat-sheet strong {
  color: #111827;
  display: block;
  margin-bottom: 8px;
}
.cheat-sheet ul {
  margin: 0;
  padding-left: 20px;
  list-style-type: disc;
}
.cheat-sheet li {
  margin: 4px 0;
  line-height: 1.5;
}
.cheat-sheet li strong {
  display: inline;
  color: #2563eb;
}
</style>

