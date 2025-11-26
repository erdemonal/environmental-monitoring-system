<script setup>
import { ref } from 'vue'
import { login } from '../api'
import { navigate } from '../router'

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await login(username.value, password.value)
    localStorage.setItem('auth', JSON.stringify(res))
    navigate(res.role === 'ADMIN' ? '/admin' : '/user')
  } catch (e) {
    error.value = e?.message || 'Login failed'
  } finally {
    loading.value = false
  }
}

function handleKeyPress(e) {
  if (e.key === 'Enter') submit()
}
</script>

<template>
  <div class="page">
    <div class="center">
      <div class="login-card">
      <div class="logo">
        <div class="logo-icon">
          <span class="logo-mark">EG</span>
        </div>
      </div>
      <p class="subtitle">EcoGuard</p>

      <div v-if="error" class="error">{{ error }}</div>

      <div class="form">
        <div class="field">
          <input 
            v-model="username" 
            placeholder="Username" 
            @keypress="handleKeyPress"
            :disabled="loading"
          />
        </div>
        <div class="field">
          <input 
            type="password" 
            v-model="password" 
            placeholder="Password" 
            @keypress="handleKeyPress"
            :disabled="loading"
          />
        </div>
        <button class="btn-primary" :disabled="loading || !username || !password" @click="submit">
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </div>

      <div class="login-footer">
        made with ❤️ in Saint-Étienne
      </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  width: 100%;
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #eef2f3 0%, #dfe9f3 100%);
}

.center {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px 0;
}

.login-card {
  width: min(400px, 100%);
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.logo {
  text-align: center;
  margin-bottom: 8px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  margin: 0 auto 8px;
  background: radial-gradient(circle at 30% 20%, #bbf7d0, #16a34a);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 18px rgba(22, 163, 74, 0.35);
}

.logo-mark {
  font-family: 'Fira Code', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 20px;
  font-weight: 700;
  color: #f9fafb;
  letter-spacing: 2px;
}

h1 {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.subtitle {
  text-align: center;
  color: #374151;
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 24px 0;
}

.error {
  background: #fee2e2;
  color: #b91c1c;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 20px;
  text-align: center;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.field input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.field input:disabled {
  background: #f3f4f6;
  cursor: not-allowed;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  margin-top: 8px;
}

.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-primary:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 11px;
  color: #111827;
}
</style>


