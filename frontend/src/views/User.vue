<script setup>
import { onMounted } from 'vue'
import { navigate, requireRole } from '../router'
import Dashboard from '../components/Dashboard.vue'
import History from '../components/History.vue'
import MiniCharts from '../components/MiniCharts.vue'

onMounted(() => {
  requireRole('USER')
})

function logout() {
  localStorage.removeItem('auth')
  navigate('/login')
}
</script>

<template>
  <div>
    <div class="bar">
      <div class="title">User</div>
      <div class="spacer"></div>
      <button class="btn" @click="logout">Logout</button>
    </div>
    <Dashboard mode="user" />
    <MiniCharts />
    <History />
  </div>
</template>

<style scoped>
.bar { display:flex; align-items:center; gap:8px; margin-bottom:8px; }
.title { font-weight:700; }
.spacer { flex:1; }
.btn { padding:6px 10px; border:1px solid #e5e7eb; background:#fff; border-radius:8px; cursor:pointer; }
</style>


