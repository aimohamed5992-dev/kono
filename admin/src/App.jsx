import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider, useAuth } from './lib/auth'
import { I18nProvider } from './lib/i18n'
import { ToastProvider } from './lib/toast'
import { Spinner } from './components/ui'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import LiveMap from './pages/LiveMap'
import Orders from './pages/Orders'
import Stores from './pages/Stores'
import Products from './pages/Products'
import Couriers from './pages/Couriers'
import Customers from './pages/Customers'
import Verticals from './pages/Verticals'
import Promotions from './pages/Promotions'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 15_000 } },
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
      <BrowserRouter>
        <I18nProvider>
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
                <Route path="/" element={<Dashboard />} />
                <Route path="/live" element={<LiveMap />} />
                <Route path="/orders" element={<Orders />} />
                <Route path="/stores" element={<Stores />} />
                <Route path="/products" element={<Products />} />
                <Route path="/couriers" element={<Couriers />} />
                <Route path="/customers" element={<Customers />} />
                <Route path="/verticals" element={<Verticals />} />
                <Route path="/promotions" element={<Promotions />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </ToastProvider>
        </AuthProvider>
        </I18nProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
