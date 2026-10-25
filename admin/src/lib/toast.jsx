import { createContext, useCallback, useContext, useState } from 'react'
import { CheckCircle2, XCircle, Info } from 'lucide-react'

const ToastCtx = createContext(null)

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])

  const push = useCallback((message, tone = 'ok') => {
    const id = Math.random().toString(36).slice(2)
    setItems((x) => [...x, { id, message, tone }])
    setTimeout(() => setItems((x) => x.filter((t) => t.id !== id)), 3800)
  }, [])

  const api = {
    ok: (m) => push(m, 'ok'),
    error: (m) => push(m, 'error'),
    info: (m) => push(m, 'info'),
  }

  return (
    <ToastCtx.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed bottom-5 left-1/2 z-50 flex -translate-x-1/2 flex-col gap-2">
        {items.map((t) => {
          const Icon = t.tone === 'ok' ? CheckCircle2 : t.tone === 'error' ? XCircle : Info
          const color =
            t.tone === 'ok' ? 'text-ok' : t.tone === 'error' ? 'text-danger' : 'text-info'
          return (
            <div
              key={t.id}
              className="pointer-events-auto flex items-center gap-2.5 rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-semibold shadow-lg"
            >
              <Icon className={`size-4 ${color}`} />
              {t.message}
            </div>
          )
        })}
      </div>
    </ToastCtx.Provider>
  )
}

export const useToast = () => useContext(ToastCtx)
