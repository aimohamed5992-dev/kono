import axios from 'axios'

const BASE = import.meta.env.VITE_API_URL || '' // '' → same origin (vite proxy)

export const api = axios.create({ baseURL: BASE + '/api' })

let accessToken = null
let refreshToken = null
let onAuthLost = () => {}

export function setTokens(t) {
  accessToken = t?.accessToken ?? null
  refreshToken = t?.refreshToken ?? null
  if (t) localStorage.setItem('kono.portal.auth', JSON.stringify(t))
  else localStorage.removeItem('kono.portal.auth')
}

export function loadTokens() {
  try {
    const raw = localStorage.getItem('kono.portal.auth')
    if (!raw) return null
    const t = JSON.parse(raw)
    accessToken = t.accessToken
    refreshToken = t.refreshToken
    return t
  } catch {
    return null
  }
}

export function setAuthLostHandler(fn) {
  onAuthLost = fn
}

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

let refreshing = null

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    if (status === 401 && refreshToken && !original._retry) {
      original._retry = true
      try {
        refreshing =
          refreshing ||
          axios
            .post(BASE + '/api/auth/refresh', { refreshToken })
            .then((res) => {
              setTokens(res.data)
              return res.data.accessToken
            })
            .finally(() => {
              refreshing = null
            })
        const newToken = await refreshing
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      } catch {
        setTokens(null)
        onAuthLost()
        return Promise.reject(error)
      }
    }
    return Promise.reject(error)
  }
)

export function apiError(error) {
  return (
    error?.response?.data?.title ||
    error?.response?.data?.detail ||
    error?.message ||
    'حدث خطأ غير متوقع'
  )
}
