<script setup>
import { onMounted, ref } from 'vue'
import { getSensorRange } from '../api'

const pad = v => String(v).padStart(2, '0')
function formatLocal(date) {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + 'T' + [pad(date.getHours()), pad(date.getMinutes()), pad(date.getSeconds())].join(':')
}

const loading = ref(true)
const error = ref('')
const data = ref([])
const detailChart = ref(null)
const detailSvg = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const end = new Date()
    const start = new Date(end.getTime() - 6 * 60 * 60 * 1000) 
    const res = await getSensorRange(formatLocal(start), formatLocal(end), 'user')
    data.value = Array.isArray(res) ? res : []
  } catch (e) {
    error.value = e?.message || 'Failed to load mini charts'
  } finally {
    loading.value = false
  }
}

onMounted(load)

// generates SVG path string for line charts
// maps timestamp/value pairs to SVG coordinates with scaling
function linePath(points, width, height, padding = 10) {
  if (!points.length) return ''
  const xs = points.map(p => new Date(p.timestamp).getTime())
  const ys = points.map(p => p.value)
  const minX = Math.min(...xs), maxX = Math.max(...xs)
  const minY = Math.min(...ys), maxY = Math.max(...ys)
  // prevent division by zero if all values are the same
  const spanX = Math.max(1, maxX - minX), spanY = Math.max(1, maxY - minY)
  // linear mapping: data range to SVG coordinate range
  const mapX = x => padding + ((x - minX) / spanX) * (width - padding * 2)
  const mapY = y => height - padding - ((y - minY) / spanY) * (height - padding * 2)
  // M = move to first point, L = line to subsequent points
  return points.map((p, i) =>
    `${i ? 'L' : 'M'}${mapX(new Date(p.timestamp).getTime())},${mapY(p.value)}`
  ).join(' ')
}

const chartDefs = [
  { key: 'temperature', title: 'Temperature (°C)', color: '#ef4444', accessor: row => row.temperature },
  { key: 'humidity', title: 'Humidity (%)', color: '#3b82f6', accessor: row => row.humidity },
  { key: 'co2', title: 'CO₂ (ppm)', color: '#10b981', accessor: row => (row.co2Level ?? row.co2) },
  { key: 'lightLevel', title: 'Light (lux)', color: '#f59e0b', accessor: row => row.lightLevel }
]

function pointsFor(def) {
  return (data.value || [])
    .map(row => ({ timestamp: row.timestamp, value: def.accessor(row) }))
    .filter(p => p.value !== null && p.value !== undefined)
}

function openDetail(def) {
  const pts = pointsFor(def)
  if (!pts.length) return
  detailChart.value = {
    def,
    points: pts,
    path: linePath(pts, 800, 240, 24)
  }
}

function closeDetail() {
  detailChart.value = null
}

function exportDetail() {
  if (!detailSvg.value || !detailChart.value) return

  const svgEl = detailSvg.value
  try {
    const serializer = new XMLSerializer()
    const svgStr = serializer.serializeToString(svgEl)
    const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0)
      URL.revokeObjectURL(url)

      const mime = 'image/png'
      const dataUrl = canvas.toDataURL(mime)
      const a = document.createElement('a')
      const key = detailChart.value.def?.key || 'chart'
      a.download = `ecoguard-${key}.png`
      a.href = dataUrl
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    }
    img.src = url
  } catch (e) {
    console.error('Export error', e)
  }
}
</script>

<template>
  <div class="mini">
    <div class="head">
      <div class="title">Last 6 hours</div>
      <button class="btn" @click="load" :disabled="loading">{{ loading ? '...' : 'Refresh' }}</button>
    </div>
    <p v-if="error" style="color:#b91c1c;margin:0 0 8px 0">{{ error }}</p>
    <div class="grid">
      <div
        class="card"
        v-for="def in chartDefs"
        :key="def.key"
        @click="openDetail(def)"
      >
        <div class="label">{{ def.title }}</div>
        <svg width="100%" viewBox="0 0 320 100" preserveAspectRatio="none">
          <path :d="linePath(pointsFor(def),320,100)" fill="none" :stroke="def.color" stroke-width="2"/>
        </svg>
        <div class="hint">Click for details</div>
      </div>
    </div>
    <div v-if="detailChart" class="modal" @click.self="closeDetail">
      <div class="modal-card">
        <div class="modal-header">
          <div>
            <div class="modal-title">{{ detailChart.def.title }}</div>
            <div class="modal-subtitle">Last 6 hours • {{ detailChart.points.length }} points</div>
          </div>
          <div class="modal-actions">
            <button class="btn btn-small" @click="exportDetail">Export PNG</button>
            <button class="btn" @click="closeDetail">Close</button>
          </div>
        </div>
        <svg ref="detailSvg" width="100%" viewBox="0 0 800 240" preserveAspectRatio="none">
          <path :d="detailChart.path" fill="none" :stroke="detailChart.def.color" stroke-width="3"/>
        </svg>
        <div class="modal-meta">
          Peak: {{ Math.max(...detailChart.points.map(p => p.value)).toFixed(2) }} |
          Min: {{ Math.min(...detailChart.points.map(p => p.value)).toFixed(2) }}
        </div>
      </div>
    </div>
  </div>
  </template>

<style scoped>
.mini { margin-top: 12px; }
.head { display:flex; align-items:center; justify-content: space-between; margin-bottom:8px; }
.title { font-weight: 600; }
.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.card:hover {
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.label { font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.hint { font-size: 11px; color: #9ca3af; margin-top: 6px; }
@media (max-width: 1200px) {
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .grid { grid-template-columns: 1fr; }
}
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 50;
}
.modal-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  width: min(900px, 100%);
  max-height: 90vh;
  overflow: auto;
  border: 1px solid #e5e7eb;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
}
.modal-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}
.modal-subtitle {
  font-size: 13px;
  color: #6b7280;
}
.modal-meta {
  margin-top: 10px;
  font-size: 13px;
  color: #6b7280;
}
.btn-small {
  font-size: 11px;
  padding: 3px 8px;
}
</style>


