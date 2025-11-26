import { ref } from 'vue'

export const currentRoute = ref(location.hash.slice(1) || '/')

window.addEventListener('hashchange', () => {
  currentRoute.value = location.hash.slice(1) || '/'
})

export function navigate(path) {
  if (!path.startsWith('/')) path = '/' + path
  if (location.hash.slice(1) !== path) {
    location.hash = path
  } else {
    currentRoute.value = path
  }
}

export function getAuth() {
  try {
    return JSON.parse(localStorage.getItem('auth') || 'null')
  } catch {
    return null
  }
}

export function requireSignedIn() {
  const auth = getAuth()
  if (!auth) {
    navigate('/login')
    return false
  }
  return true
}

export function requireRole(role) {
  const auth = getAuth()
  if (!auth) {
    navigate('/login')
    return false
  }
  if (role && auth.role !== role) {
    navigate(auth.role === 'ADMIN' ? '/admin' : '/user')
    return false
  }
  return true
}

