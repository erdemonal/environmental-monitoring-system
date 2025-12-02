<script setup>
import { onMounted, ref, watch } from 'vue'
import { getThresholds, updateThreshold } from '../api'

const props = defineProps({
  adminMode: { type: Boolean, default: false }
})

const thresholds = ref([])
const loading = ref(true)
const error = ref('')
const saveState = ref({})

function labelFor(metric) {
  switch (metric) {
    case 'TEMP': return 'Temperature'
    case 'HUMIDITY': return 'Humidity'
    case 'CO2': return 'CO₂'
    case 'LIGHT': return 'Light'
    default: return metric
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    thresholds.value = await getThresholds(props.adminMode ? 'admin' : 'user')
  } catch (e) {
    error.value = e?.message || 'Failed to load thresholds'
  } finally {
    loading.value = false
  }
}

async function saveRow(t) {
  try {
    const min = parseFloat(t.minValue)
    const max = parseFloat(t.maxValue)
    if (Number.isNaN(min) || Number.isNaN(max)) {
      alert('Min/Max must be numbers')
      return
    }
    if (min < 0 || max < 0) {
      alert('Values cannot be negative')
      return
    }
    if (max < min) {
      alert('Maximum must be greater than or equal to Minimum')
      return
    }
    if (t.metricType === 'HUMIDITY' && max > 100) {
      alert('Humidity maximum cannot be above 100%')
      return
    }
    saveState.value[t.id] = 'saving'
    await updateThreshold(t.id, { 
      minValue: min, 
      maxValue: max
    })
    saveState.value[t.id] = 'saved'
    setTimeout(() => {
      if (saveState.value[t.id] === 'saved') {
        saveState.value[t.id] = ''
      }
    }, 2000)
  } catch (e) {
    saveState.value[t.id] = 'error'
    alert(e?.message || 'Save failed')
  }
}


onMounted(load)
watch(() => props.adminMode, load)
</script>

<template>
  <div>
    <h2>Thresholds</h2>
    <p class="muted" v-if="!props.adminMode">Read-only</p>
    <p v-if="loading">Loading...</p>
    <p v-if="error" style="color:#b91c1c">{{ error }}</p>
    <div v-if="thresholds?.length" class="table">
      <div class="thead" :class="props.adminMode ? 'grid4' : 'grid3'">
        <div>Metric</div>
        <div>Min</div>
        <div>Max</div>
        <div v-if="props.adminMode">Actions</div>
      </div>
      <div class="tbody">
        <div class="row" :class="props.adminMode ? 'grid4' : 'grid3'" v-for="t in thresholds" :key="t.id">
          <div>{{ labelFor(t.metricType) }}</div>
          <div v-if="props.adminMode"><input type="number" step="any" v-model="t.minValue" class="input" /></div>
          <div v-else>{{ t.minValue }}</div>
          <div v-if="props.adminMode"><input type="number" step="any" v-model="t.maxValue" class="input" /></div>
          <div v-else>{{ t.maxValue }}</div>
          <div v-if="props.adminMode" class="actions">
            <button class="btn" @click="saveRow(t)" :disabled="saveState[t.id] === 'saving'">
              {{ saveState[t.id] === 'saving' ? 'Saving...' : 'Save' }}
            </button>
            <span v-if="saveState[t.id] === 'saved'" class="status success">Saved!</span>
            <span v-else-if="saveState[t.id] === 'error'" class="status error">Failed</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!loading" class="muted">No thresholds found</div>
  </div>
</template>

<style scoped>
h2 {
  margin: 16px 0 12px 0;
  font-size: 20px;
  font-weight: 700;
}
.muted { color: #6b7280; margin-top: -4px; margin-bottom: 12px; }
.table {
  display: grid;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.thead, .row { display: grid; gap: 8px; }
.grid4 { grid-template-columns: 2fr 1fr 1fr 1fr; }
.grid3 { grid-template-columns: 2fr 1fr 1fr; }
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
.input { border:1px solid #e5e7eb; border-radius:8px; padding:6px 8px; width: 100%; }
.actions { display:flex; gap:8px; align-items: center; flex-wrap: wrap; }
.btn { padding:6px 10px; border:1px solid #e5e7eb; background:#fff; border-radius:8px; cursor:pointer; }
.btn.danger { border-color:#fecaca; background:#fee2e2; }
.status { font-size: 12px; }
.status.success { color: #059669; }
.status.error { color: #b91c1c; }
.create { margin-top: 12px; }
</style>


