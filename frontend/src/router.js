import { ref } from 'vue'

// using hash based routing instead of Vue Router for simplicity
export const currentRoute = ref(location.hash.slice(1) || '/')

window.addEventListener('hashchange', () => {
  currentRoute.value = location.hash.slice(1) || '/'
})

export function navigate(path) {
  if (!path.startsWith('/')) path = '/' + path
  // only update hash if it is different to avoid unnecessary navigation
  if (location.hash.slice(1) !== path) {
    location.hash = path
  } else {
    // force reactivity update even if hash didn't change
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

