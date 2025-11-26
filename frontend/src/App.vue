<script setup>
import { computed } from 'vue'
import { currentRoute, navigate, getAuth } from './router'
import Login from './views/Login.vue'
import Admin from './views/Admin.vue'
import User from './views/User.vue'

const View = computed(() => {
  const route = currentRoute.value
  if (route === '/login') return Login
  if (route === '/admin') return Admin
  if (route === '/user') return User
  const auth = getAuth()
  if (!auth) return Login
  return auth.role === 'ADMIN' ? Admin : User
})

const isLogin = computed(() => currentRoute.value === '/login')

function gotoHome() {
  const auth = getAuth()
  navigate(auth?.role === 'ADMIN' ? '/admin' : '/user')
}
</script>

<template>
  <div v-if="isLogin">
    <component :is="View" />
  </div>
  <div v-else class="layout">
    <header class="topbar">
      <div class="brand" @click="gotoHome">EcoGuard</div>
    </header>
    <main class="content">
      <component :is="View" />
    </main>
  </div>
  <div v-if="!isLogin" class="bg"></div>
</template>

<style scoped>
.layout {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}
.brand {
  font-weight: 700;
  font-size: 20px;
}
.tabs {
  display: flex;
  gap: 8px;
}
.content {
  margin-top: 12px;
  background: #ffffffcc;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.bg {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(600px 300px at 10% 10%, #93c5fd33, transparent),
    radial-gradient(600px 300px at 90% 20%, #34d39933, transparent),
    radial-gradient(600px 300px at 50% 80%, #fbbf2433, transparent);
  z-index: -1;
}
</style>
