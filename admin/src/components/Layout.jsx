import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  Map,
  ShoppingBag,
  Store,
  Package,
  Bike,
  Users,
  Grid3x3,
  Megaphone,
  LogOut,
} from 'lucide-react'
import { useAuth } from '../lib/auth'
import { useI18n } from '../lib/i18n'
import { clsx } from './clsx'

const nav = [
  { to: '/', label: 'لوحة التحكم', icon: LayoutDashboard, end: true },
  { to: '/live', label: 'الخريطة الحيّة', icon: Map },
  { to: '/orders', label: 'الطلبات', icon: ShoppingBag },
  { to: '/stores', label: 'المتاجر', icon: Store },
  { to: '/products', label: 'المنتجات', icon: Package },
  { to: '/couriers', label: 'المناديب', icon: Bike },
  { to: '/customers', label: 'العملاء', icon: Users },
  { to: '/verticals', label: 'الأقسام', icon: Grid3x3 },
  { to: '/promotions', label: 'العروض', icon: Megaphone },
]

function Mark() {
  return (
    <div className="grid size-9 place-items-center rounded-xl bg-brand font-black text-brown">
      k
    </div>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const { lang, toggle } = useI18n()
  const navigate = useNavigate()

  return (
    <div className="flex min-h-screen">
      {/* sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col border-l border-line bg-white/70 px-3 py-5 md:flex">
        <div className="mb-6 flex items-center gap-2.5 px-2">
          <Mark />
          <div>
            <div className="font-extrabold text-espresso">كونو</div>
            <div className="text-xs text-ink-soft">لوحة الإدارة</div>
          </div>
        </div>

        <nav className="flex flex-1 flex-col gap-1">
          {nav.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition',
                  isActive
                    ? 'bg-brown text-cream shadow-sm'
                    : 'text-brown-soft hover:bg-cream hover:text-brown'
                )
              }
            >
              <n.icon className="size-[18px]" strokeWidth={2.2} />
              {n.label}
            </NavLink>
          ))}
        </nav>

        <button
          onClick={async () => {
            await logout()
            navigate('/login')
          }}
          className="mt-2 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-danger hover:bg-danger/10"
        >
          <LogOut className="size-[18px]" strokeWidth={2.2} />
          تسجيل الخروج
        </button>
      </aside>

      {/* main */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-line bg-canvas/80 px-6 backdrop-blur">
          <div className="flex items-center gap-2 md:hidden">
            <Mark />
            <span className="font-extrabold text-espresso">كونو</span>
          </div>
          <div className="hidden text-sm text-ink-soft md:block">
            {new Date().toLocaleDateString('ar-SA', { weekday: 'long', day: 'numeric', month: 'long' })}
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={toggle}
              className="rounded-lg border border-line px-2.5 py-1 text-xs font-bold text-brown-soft hover:bg-cream"
            >
              {lang === 'ar' ? 'EN' : 'ع'}
            </button>
            <div className="text-right">
              <div className="text-sm font-bold text-ink">{user?.name}</div>
              <div className="text-xs text-ink-soft">مدير</div>
            </div>
            <div className="grid size-9 place-items-center rounded-full bg-brand text-sm font-black text-brown">
              {user?.name?.[0] ?? 'م'}
            </div>
          </div>
        </header>

        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
