import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { ClipboardList, UtensilsCrossed, BarChart3, MessagesSquare, Settings, LogOut } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { useI18n } from '../lib/i18n'
import { useStore, useThreads, useApiMutation } from '../lib/hooks'
import { api } from '../lib/api'
import { clsx } from './clsx'

const nav = [
  { to: '/', label: 'الطلبات', icon: ClipboardList, end: true },
  { to: '/menu', label: 'القائمة', icon: UtensilsCrossed },
  { to: '/reports', label: 'التقارير', icon: BarChart3 },
  { to: '/chat', label: 'المحادثات', icon: MessagesSquare },
  { to: '/settings', label: 'إعدادات المتجر', icon: Settings },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const { lang, toggle } = useI18n()
  const navigate = useNavigate()
  const { data: store } = useStore()
  const { data: threads } = useThreads()
  const unread = (threads ?? []).reduce((a, t) => a + (t.unreadCount || 0), 0)

  const setStatus = useApiMutation((body) => api.post('/merchant/status', body), ['store'])
  const online = store ? !store.isClosed : true

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-64 shrink-0 flex-col border-l border-line bg-white/70 px-3 py-5 md:flex">
        <div className="mb-5 flex items-center gap-2.5 px-2">
          <div className="grid size-10 place-items-center overflow-hidden rounded-xl bg-brand font-black text-brown">
            {store?.logoUrl ? (
              <img src={store.logoUrl} alt="" className="size-full object-cover" />
            ) : (
              (store?.name?.[0] ?? 'ك')
            )}
          </div>
          <div className="min-w-0">
            <div className="truncate font-extrabold text-espresso">{store?.name ?? 'متجري'}</div>
            <div className="text-xs text-ink-soft">كونو للأعمال</div>
          </div>
        </div>

        <button
          onClick={() => setStatus.mutate({ isOnline: !online, isBusy: null, prepMinutes: null })}
          className={clsx(
            'mb-4 flex items-center justify-between rounded-xl px-3 py-2.5 text-sm font-bold transition',
            online ? 'bg-ok/12 text-ok' : 'bg-danger/12 text-danger'
          )}
        >
          <span>{online ? 'المتجر مفتوح' : 'المتجر مغلق'}</span>
          <span className={clsx('h-4 w-8 rounded-full p-0.5 transition', online ? 'bg-ok' : 'bg-danger/40')}>
            <span className={clsx('block size-3 rounded-full bg-white transition', online && 'translate-x-4')} />
          </span>
        </button>

        <nav className="flex flex-1 flex-col gap-1">
          {nav.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition',
                  isActive ? 'bg-brown text-cream shadow-sm' : 'text-brown-soft hover:bg-cream hover:text-brown'
                )
              }
            >
              <n.icon className="size-[18px]" strokeWidth={2.2} />
              <span className="flex-1">{n.label}</span>
              {n.to === '/chat' && unread > 0 && (
                <span className="rounded-full bg-danger px-1.5 text-xs font-black text-white">{unread}</span>
              )}
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

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-line bg-canvas/80 px-6 backdrop-blur">
          <div className="flex items-center gap-2 md:hidden">
            <span className="font-extrabold text-espresso">{store?.name ?? 'كونو للأعمال'}</span>
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
            <div className="text-sm font-bold text-ink">{user?.name}</div>
          </div>
        </header>
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
