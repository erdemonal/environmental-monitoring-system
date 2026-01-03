<script setup>
import { onMounted, onUnmounted, ref, watch, h, computed } from 'vue'
import { getLatestSensor, getAlerts } from '../api'

const props = defineProps({
  mode: { type: String, default: 'user' }
})

const latest = ref(null)
const alerts = ref([])
const loading = ref(true)
const error = ref('')

const alertFilterMetric = ref('ALL')
const alertFilterSeverity = ref('ALL')
const alertsPage = ref(1)
const pageSize = 5

// decides alert importance based on metric type and value
function alertSeverity(a) {
  const t = (a.alertType || a.type || '').toUpperCase()
  const metric = (a.metricType || '').toUpperCase()
  const valueNum = Number(a.value)

  if (t === 'INTRUDER') return 'CRITICAL'

  if (t === 'THRESHOLD' && !Number.isNaN(valueNum)) {
    if (metric === 'CO2') {
      return valueNum >= 2000 ? 'CRITICAL' : 'WARNING'
    }
    if (metric === 'LIGHT') {
      return valueNum >= 3500 ? 'CRITICAL' : 'WARNING'
    }
    if (metric === 'HUMIDITY') {
      return (valueNum <= 30 || valueNum >= 70) ? 'CRITICAL' : 'WARNING'
    }
    if (metric === 'TEMP') {
      return (valueNum <= 16 || valueNum >= 28) ? 'CRITICAL' : 'WARNING'
    }
    return 'WARNING'
  }

  return 'INFO'
}

const filteredAlerts = computed(() => {
  let list = alerts.value
  if (alertFilterMetric.value !== 'ALL') {
    list = list.filter(a => (a.metricType || '').toUpperCase() === alertFilterMetric.value)
  }
  if (alertFilterSeverity.value !== 'ALL') {
    list = list.filter(a => alertSeverity(a) === alertFilterSeverity.value)
  }
  alertsPage.value = 1
  return list
})

const totalPages = computed(() => {
  return filteredAlerts.value.length === 0
    ? 1
    : Math.ceil(filteredAlerts.value.length / pageSize)
})

const pagedAlerts = computed(() => {
  const start = (alertsPage.value - 1) * pageSize
  return filteredAlerts.value.slice(start, start + pageSize)
})

function MetricCard(props) {
  return h('div', { class: 'card' }, [
    h('div', { class: 'metric' }, props.label),
    h('div', { class: 'value' }, [
      String(props.value ?? '—'),
      props.unit ? h('span', { style: 'font-size:12px;font-weight:500;margin-left:4px' }, props.unit) : null
    ])
  ])
}

function formatNumber(val) {
  if (val === null || val === undefined) return '—'
  const num = Number(val)
  if (Number.isNaN(num)) return val
  return num.toFixed(1)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [l, a] = await Promise.all([getLatestSensor(props.mode), getAlerts(props.mode)])
    latest.value = l || null
    const sortedAlerts = Array.isArray(a)
      ? [...a].sort((x, y) => new Date(y.timestamp) - new Date(x.timestamp))
      : []
    alerts.value = sortedAlerts
    if (!l) {
      error.value = ''
    }
  } catch (e) {
    console.error(e)
    if (e.message && e.message.includes('404')) {
      error.value = ''
      latest.value = null
    } else {
      error.value = 'Failed to load data'
    }
  } finally {
    loading.value = false
  }
}

let refreshInterval = null
onMounted(() => {
  load()
  refreshInterval = setInterval(load, 10000)
})
onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})
watch(() => props.mode, load)
</script>

<template>
  <div>
    <div class="header">
      <h2>Dashboard</h2>
      <p v-if="latest" class="last-ts">
        Last reading: {{ new Date(latest.timestamp).toLocaleString() }}
      </p>
    </div>
    <p v-if="loading">Loading...</p>
    <p v-if="error && !loading" class="error-msg">{{ error }}</p>
    <div v-if="!latest && !loading" class="no-data">
      <p>No sensor data available</p>
      <p class="muted">Waiting for data from embedded device...</p>
    </div>
    <div v-if="latest" class="cards">
      <MetricCard label="Temperature" :value="formatNumber(latest.temperature)" unit="°C" />
      <MetricCard label="Humidity" :value="formatNumber(latest.humidity)" unit="%" />
      <MetricCard label="CO₂" :value="formatNumber(latest.co2Level ?? latest.co2)" unit="ppm" />
      <MetricCard label="Light" :value="formatNumber(latest.lightLevel)" unit="lux" />
    </div>
    <div class="panel">
      <div class="panel-title">Recent Alerts</div>
      <div class="alert-filters">
        <select v-model="alertFilterMetric">
          <option value="ALL">All metrics</option>
          <option value="TEMP">Temp</option>
          <option value="HUMIDITY">Humidity</option>
          <option value="CO2">CO₂</option>
          <option value="LIGHT">Light</option>
        </select>
        <select v-model="alertFilterSeverity">
          <option value="ALL">All severities</option>
          <option value="WARNING">Warning</option>
          <option value="CRITICAL">Critical</option>
        </select>
      </div>
      <div v-if="filteredAlerts.length === 0" class="muted">No alerts</div>
      <ul v-else class="list">
        <li v-for="a in pagedAlerts" :key="a.id" class="li">
          <span class="badge" :class="'badge-' + alertSeverity(a).toLowerCase()">
            {{ a.alertType || a.type }}
          </span>
          <span class="muted">{{ new Date(a.timestamp).toLocaleString() }}</span>
          <span>{{ a.metricType ? a.metricType + ': ' : '' }}{{ a.value }}</span>
        </li>
      </ul>
      <div v-if="filteredAlerts.length > pageSize" class="pager">
        <button class="pager-btn" :disabled="alertsPage === 1" @click="alertsPage--">Prev</button>
        <span class="pager-info">Page {{ alertsPage }} / {{ totalPages }}</span>
        <button class="pager-btn" :disabled="alertsPage === totalPages" @click="alertsPage++">Next</button>
      </div>
    </div>
  </div>
  </template>

<style scoped>
.header {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
h2 {
  margin: 0;
}
.last-ts {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
}
.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
}
.metric { color: #6b7280; font-size: 12px; }
.value { font-size: 20px; font-weight: 700; margin-top: 6px; }
.value.small { font-size: 14px; font-weight: 500; }
.panel {
  margin-top: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
}
.panel-title { font-weight: 600; margin-bottom: 8px; }
.alert-filters {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.alert-filters select {
  font-size: 12px;
  padding: 2px 6px;
}
.muted { color: #6b7280; }
.error-msg { color: #b91c1c; margin: 8px 0; }
.no-data {
  text-align: center;
  padding: 40px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}
.no-data p { margin: 8px 0; }
.list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.li { display: flex; gap: 8px; align-items: center; }
.badge {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 999px;
  border: 1px solid transparent;
}
.badge-warning {
  background: #fef3c7;
  color: #92400e;
  border-color: #fde68a;
}
.badge-critical {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fecaca;
}
.badge-info {
  background: #e0f2fe;
  color: #075985;
  border-color: #bae6fd;
}
.pager {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.pager-btn {
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
}
.pager-btn:disabled {
  opacity: 0.5;
  cursor: default;
}
.pager-info {
  font-size: 12px;
}
@media (max-width: 900px) {
  .cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 520px) {
  .cards { grid-template-columns: 1fr; }
}
</style>

<script>
export default {
  components: {
    MetricCard: {
      props: ['label', 'value', 'unit'],
      template: `
        <div class="card">
          <div class="metric">{{ label }}</div>
          <div class="value">
            {{ value }} <span v-if="unit" style="font-size:12px;font-weight:500">{{ unit }}</span>
          </div>
        </div>
      `
    }
  },
  methods: {
    formatNumber(val) {
      if (val === null || val === undefined) return '—'
      const num = Number(val)
      if (Number.isNaN(num)) return val
      return num.toFixed(1)
    }
  }
}
</script>


