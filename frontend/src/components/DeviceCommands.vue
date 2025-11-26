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
  { value: 'DISPLAY_MESSAGE', label: 'Display Message', paramsHint: 'message text' }
]

function clampColor(val) {
  const num = Number(val)
  if (Number.isNaN(num)) return 0
  return Math.min(255, Math.max(0, Math.floor(num)))
}

async function send() {
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
    const params = commandType.value === 'SET_LED_COLOR'
      ? `${clampColor(ledR.value)},${clampColor(ledG.value)},${clampColor(ledB.value)}`
      : (parameters.value || null)
    await sendDeviceCommand(deviceKey.value, commandType.value, params)
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
      <div class="field" v-else>
        <label>Parameters</label>
        <input v-model="parameters" :placeholder="commandTypes.find(c => c.value === commandType)?.paramsHint || 'parameters'" />
      </div>
      <button class="btn primary" @click="send" :disabled="loading">
        {{ loading ? 'Sending...' : 'Send Command' }}
      </button>
    </div>

    <p v-if="error" style="color:#b91c1c;margin-top:8px">{{ error }}</p>
    <p v-if="success" style="color:#059669;margin-top:8px">{{ success }}</p>

    <div class="panel" style="margin-top:16px">
      <div class="panel-title">Pending Commands</div>
      <div v-if="commands.length === 0" class="muted">No commands sent</div>
      <div v-else class="table">
        <div class="thead grid4">
          <div>Type</div>
          <div>Parameters</div>
          <div>Status</div>
          <div>Time</div>
        </div>
        <div class="tbody">
          <div class="row grid4" v-for="cmd in commands.slice(0, 10)" :key="cmd.id">
            <div>{{ getCommandLabel(cmd.commandType) }}</div>
            <div>{{ cmd.parameters || '—' }}</div>
            <div>
              <span :class="cmd.executed ? 'badge success' : 'badge pending'">
                {{ cmd.executed ? 'Executed' : 'Pending' }}
              </span>
            </div>
            <div class="small">{{ new Date(cmd.createdAt).toLocaleString() }}</div>
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
.small {
  font-size: 12px;
  color: #6b7280;
}
</style>

