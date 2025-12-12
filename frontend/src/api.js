const BASE = import.meta?.env?.VITE_API_BASE || '';

function getToken() {
  try {
    const raw = localStorage.getItem('auth')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.token || null
  } catch {
    return null
  }
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const config = { ...options, headers }
  const res = await fetch(`${BASE}${path}`, config);
  if (res.status === 401) {
    localStorage.removeItem('auth')
    window.location.hash = '#/login'
  }
  if (res.status === 403) {
    throw new Error('Forbidden')
  }
  if (res.status === 404) {
    return null
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Request failed ${res.status}: ${text}`);
  }
  return res.status === 204 ? null : res.json();
}

function rolePrefix(role = 'user') {
  return role === 'admin' ? '/api/admin' : '/api/user'
}

export function getLatestSensor(role = 'user') {
  return request(`${rolePrefix(role)}/sensor-data/latest`);
}

export function getAllSensor(role = 'user') {
  return request(`${rolePrefix(role)}/sensor-data`);
}

export function getSensorRange(startIso, endIso, role = 'user') {
  const params = new URLSearchParams({ start: startIso, end: endIso });
  return request(`${rolePrefix(role)}/sensor-data/range?${params.toString()}`);
}

export function getThresholds(role = 'user') {
  return request(`${rolePrefix(role)}/thresholds`);
}

export function getAlerts(role = 'user') {
  return request(`${rolePrefix(role)}/alerts`);
}

export function login(username, password) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function updateThreshold(id, patch) {
  return request(`/api/admin/thresholds/${id}`, {
    method: 'PUT',
    body: JSON.stringify(patch),
  })
}

export function getThresholdAudits() {
  return request('/api/admin/thresholds/audit')
}

export function sendDeviceCommand(deviceKey, commandType, parameters) {
  return request('/api/admin/device/commands', {
    method: 'POST',
    body: JSON.stringify({ deviceKey, commandType, parameters }),
  })
}

export function getDeviceCommands(deviceKey) {
  return request(`/api/admin/device/commands/by-device/${deviceKey}/history`)
}

export function getDeviceStatus() {
  return request('/api/admin/device/status')
}

export function getHealth() {
  return request('/api/health')
}



