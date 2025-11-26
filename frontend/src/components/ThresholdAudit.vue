<script setup>
import { onMounted, ref } from 'vue'
import { getThresholdAudits } from '../api'

const audits = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    audits.value = await getThresholdAudits() || []
  } catch (e) {
    error.value = e?.message || 'Failed to load changes'
  } finally {
    loading.value = false
  }
}

function fmt(ts) {
  return ts ? new Date(ts).toLocaleString() : '—'
}

onMounted(load)
</script>

<template>
  <div class="audit">
    <div class="header">
      <h2>Recent Threshold Changes</h2>
      <button class="btn" @click="load" :disabled="loading">
        {{ loading ? 'Refreshing...' : 'Refresh' }}
      </button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="table" v-if="audits.length">
      <div class="thead">
        <div>Metric</div>
        <div>Range</div>
        <div>Updated by</div>
        <div>Time</div>
      </div>
      <div class="tbody">
        <div class="row" v-for="item in audits" :key="item.id">
          <div>{{ item.metricType }}</div>
          <div>{{ item.minValue }} – {{ item.maxValue }}</div>
          <div>{{ item.updatedBy || 'admin' }}</div>
          <div>{{ fmt(item.updatedAt) }}</div>
        </div>
      </div>
    </div>
    <p v-else-if="!loading" class="muted">No edits yet.</p>
  </div>
</template>

<style scoped>
.audit {
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.btn {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.table {
  display: grid;
}
.thead, .row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1.2fr;
  gap: 8px;
  padding: 8px 0;
}
.thead {
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
}
.row {
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
}
.row:last-child {
  border-bottom: none;
}
.error {
  color: #b91c1c;
}
.muted {
  color: #6b7280;
  font-size: 14px;
}
</style>

