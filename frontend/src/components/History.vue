<script setup>
import { onMounted, ref, watch, computed } from 'vue'
import { getSensorRange } from '../api'

const pad = v => String(v).padStart(2, '0')
function formatLocal(date) {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + 'T' + [pad(date.getHours()), pad(date.getMinutes()), pad(date.getSeconds())].join(':')
}
// normalizes datetime local input to ISO format
// handles browser inconsistencies: some return "YYYY-MM-DDTHH:mm" (16 chars),
// others return full ISO with seconds. We standardize to 19 chars.
function normalizeInput(value) {
  if (!value) return ''
  if (value.length === 16) return value + ':00'  // add missing seconds
  if (value.length > 19) return value.slice(0, 19)  // trim excess precision
  return value
}

const now = new Date()
const endIso = ref(formatLocal(now))
const startIso = ref(formatLocal(new Date(now.getTime() - 24 * 60 * 60 * 1000)))
const loading = ref(true)
const error = ref('')
const data = ref([])
const sortKey = ref('timestamp')
const sortDir = ref('desc')

function fmt(dt) {
  return new Date(dt).toLocaleString()
}

// generates SVG path string for line charts
// maps timestamp/value pairs to SVG coordinates with scaling
function linePath(points, width, height, padding = 16) {
  if (!points.length) return ''
  const xs = points.map(p => new Date(p.timestamp).getTime())
  const ys = points.map(p => p.value)
  const minX = Math.min(...xs)
  const maxX = Math.max(...xs)
  const minY = Math.min(...ys)
  const maxY = Math.max(...ys)
  // prevent division by zero if all values are the same
  const spanX = Math.max(1, maxX - minX)
  const spanY = Math.max(1, maxY - minY)
  // linear mapping: data range to SVG coordinate range
  const mapX = x => padding + ((x - minX) / spanX) * (width - padding * 2)
  const mapY = y => height - padding - ((y - minY) / spanY) * (height - padding * 2)
  // M = move to first point, L = line to subsequent points
  return points.map((p, i) => `${i ? 'L' : 'M'}${mapX(new Date(p.timestamp).getTime())},${mapY(p.value)}`).join(' ')
}


async function load() {
  loading.value = true
  error.value = ''
  data.value = []
  try {
    const res = await getSensorRange(normalizeInput(startIso.value), normalizeInput(endIso.value))
    data.value = Array.isArray(res) ? res : []
  } catch (e) {
    error.value = e?.message || 'Failed to load history'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch([startIso, endIso], load)

const sortedData = computed(() => {
  const copy = [...data.value]
  const key = sortKey.value
  const dir = sortDir.value === 'asc' ? 1 : -1
  return copy.sort((a, b) => {
    const va = valueForSort(a, key)
    const vb = valueForSort(b, key)
    if (va === vb) return 0
    return va > vb ? dir : -dir
  })
})

function valueForSort(row, key) {
  if (key === 'timestamp') return new Date(row.timestamp).getTime()
  return row[key] ?? -Infinity
}

function sortBy(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
}

function downloadFile(name, content, type) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  URL.revokeObjectURL(url)
}

function exportJson() {
  const generatedAt = new Date().toISOString()
  const payload = {
    generatedBy: 'EcoGuard',
    generatedAt,
    rows: sortedData.value
  }
  downloadFile(`ecoguard-data-${generatedAt}.json`, JSON.stringify(payload, null, 2), 'application/json')
}

function exportXml() {
  const generatedAt = new Date().toISOString()
  const rows = sortedData.value.map(row => {
    return [
      '  <row>',
      `    <timestamp>${row.timestamp}</timestamp>`,
      `    <temperature>${row.temperature}</temperature>`,
      `    <humidity>${row.humidity}</humidity>`,
      `    <co2>${row.co2 ?? row.co2Level}</co2>`,
      `    <light>${row.lightLevel}</light>`,
      '  </row>'
    ].join('\n')
  }).join('\n')
  const xml = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    `<ecoguardExport generatedBy="EcoGuard" generatedAt="${generatedAt}">`,
    rows,
    '</ecoguardExport>'
  ].join('\n')
  downloadFile(`ecoguard-data-${generatedAt}.xml`, xml, 'application/xml')
}
</script>

<template>
  <div>
    <h2>History (Last 24h by default)</h2>
    <div class="controls">
      <div class="field">
        <label>Start</label>
        <input type="datetime-local" step="1" v-model="startIso" />
      </div>
      <div class="field">
        <label>End</label>
        <input type="datetime-local" step="1" v-model="endIso" />
      </div>
      <button class="btn" @click="load">Refresh</button>
    </div>
    <p v-if="loading">Loading...</p>
    <p v-if="error" style="color:#b91c1c">{{ error }}</p>

    <div v-if="data.length" class="charts">
      <Chart title="Temperature (°C)" :points="data.map(d => ({ timestamp: d.timestamp, value: d.temperature }))" color="#ef4444" />
      <Chart title="Humidity (%)" :points="data.map(d => ({ timestamp: d.timestamp, value: d.humidity }))" color="#3b82f6" />
      <Chart title="CO₂ (ppm)" :points="data.map(d => ({ timestamp: d.timestamp, value: d.co2 ?? d.co2Level }))" color="#10b981" />
      <Chart title="Light (lux)" :points="data.map(d => ({ timestamp: d.timestamp, value: d.lightLevel }))" color="#f59e0b" />
    </div>
    <div v-else-if="!loading" class="muted">No data</div>

    <details class="panel">
      <summary>Raw data</summary>
      <div class="export-bar">
        <button class="btn" @click="exportJson">Export JSON</button>
        <button class="btn" @click="exportXml">Export XML</button>
      </div>
      <table class="raw">
        <thead>
          <tr>
            <th @click="sortBy('timestamp')">Timestamp</th>
            <th @click="sortBy('temperature')">Temp</th>
            <th @click="sortBy('humidity')">Humidity</th>
            <th @click="sortBy('co2Level')">CO₂</th>
            <th @click="sortBy('lightLevel')">Light</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in sortedData" :key="row.id">
            <td>{{ fmt(row.timestamp) }}</td>
            <td>{{ row.temperature }}</td>
            <td>{{ row.humidity }}</td>
            <td>{{ row.co2 ?? row.co2Level }}</td>
            <td>{{ row.lightLevel }}</td>
          </tr>
        </tbody>
      </table>
    </details>
  </div>
</template>

<script>
export default {
  components: {
    Chart: {
      props: ['title', 'points', 'color'],
      computed: {
        d() {
          const pts = (this.points || []).filter(p => p.value !== null && p.value !== undefined)
          return linePath(pts, 500, 160)
        }
      },
      template: `
        <div class="chart">
          <div class="chart-title">{{ title }}</div>
          <svg width="100%" viewBox="0 0 500 160" preserveAspectRatio="none">
            <path :d="d" fill="none" :stroke="color" stroke-width="2" />
          </svg>
        </div>
      `
    }
  }
}
</script>

<style scoped>
h2 {
  margin: 16px 0 12px 0;
  font-size: 20px;
  font-weight: 700;
}
.controls {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.field { display: flex; flex-direction: column; gap: 4px; }
input {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 6px 10px;
  min-width: 280px;
}
.btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
}
.charts {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
.chart {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
}
.chart-title { font-weight: 600; margin-bottom: 6px; }
.panel {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
}
.raw { width: 100%; border-collapse: collapse; }
.raw th, .raw td { border: 1px solid #e5e7eb; padding: 6px 8px; text-align: left; }
.raw th { cursor: pointer; }
.muted { color: #6b7280; }
.export-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.muted.small { font-size: 12px; }
</style>


