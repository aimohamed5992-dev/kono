import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api, loadTokens, setTokens, setAuthLostHandler } from './api'

const AuthCtx = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    setAuthLostHandler(() => setUser(null))
    const t = loadTokens()
    if (!t) {
      setReady(true)
      return
    }
    api
      .get('/auth/me')
      .then((r) => {
        if (r.data.role === 'Merchant' || r.data.role === 'Admin') setUser(r.data)
        else setTokens(null)
      })
      .catch(() => setTokens(null))
      .finally(() => setReady(true))
  }, [])

  async function login(phone, password) {
    const { data } = await api.post('/auth/login', { phone, password })
    if (data.user.role !== 'Merchant' && data.user.role !== 'Admin') {
      throw new Error('هذا الحساب غير مسجّل كمتجر على كونو.')
    }
    setTokens(data)
    setUser(data.user)
    return data.user
  }

  async function logout() {
    const t = loadTokens()
    try {
      if (t?.refreshToken) await api.post('/auth/logout', { refreshToken: t.refreshToken })
    } catch {
      /* ignore */
    }
    setTokens(null)
    setUser(null)
  }

  const value = useMemo(() => ({ user, ready, login, logout }), [user, ready])
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>
}

export const useAuth = () => useContext(AuthCtx)
