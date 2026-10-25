import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider, useAuth } from './lib/auth'
import { I18nProvider } from './lib/i18n'
import { ToastProvider } from './lib/toast'
import { Spinner } from './components/ui'
import Layout from './components/Layout'
import Login from './pages/Login'
import Orders from './pages/Orders'
import Menu from './pages/Menu'
import Reports from './pages/Reports'
import Settings from './pages/Settings'
import Chat from './pages/Chat'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 10_000 } },
})

function Protected({ children }) {
  const { user, ready } = useAuth()
  if (!ready)
    return (
      <div className="grid min-h-screen place-items-center">
        <Spinner className="size-8" />
      </div>
    )
  return user ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter><I18nProvider>
        <AuthProvider>
          <ToastProvider>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route
                element={
                  <Protected>
                    <Layout />
                  </Protected>
                }
              >
                <Route path="/" element={<Orders />} />
                <Route path="/menu" element={<Menu />} />
                <Route path="/reports" element={<Reports />} />
                <Route path="/chat" element={<Chat />} />
                <Route path="/settings" element={<Settings />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </ToastProvider>
        </AuthProvider>
      </I18nProvider></BrowserRouter>
    </QueryClientProvider>
  )
}
