import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShieldCheck, MapPinned, BarChart3, Eye, EyeOff, ArrowLeft } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { useI18n } from '../lib/i18n'
import { Button } from '../components/ui'
import { clsx } from '../components/clsx'

const HIGHLIGHTS = [
  { icon: MapPinned, title: 'خريطة الطلبات الحيّة', text: 'تابع كل طلب ومندوب على الخريطة لحظة بلحظة.' },
  { icon: BarChart3, title: 'تحليلات فورية', text: 'إيرادات، طلبات، ومتاجر الأداء الأعلى في لوحة واحدة.' },
  { icon: ShieldCheck, title: 'إدارة كاملة', text: 'المتاجر، المندوبون، العروض والتصنيفات من مكان واحد.' },
]

export default function Login() {
  const { login } = useAuth()
  const { lang, toggle } = useI18n()
  const navigate = useNavigate()
  const [phone, setPhone] = useState('+966500000000')
  const [password, setPassword] = useState('Kono@12345')
  const [showPw, setShowPw] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(phone, password)
      navigate('/')
    } catch (err) {
      setError(err?.response?.data?.title || err.message || 'تعذّر تسجيل الدخول')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen bg-canvas">
      {/* ── brand / imagery pane ─────────────────────────────── */}
      <aside className="relative hidden w-1/2 max-w-[720px] shrink-0 overflow-hidden lg:block">
        <div className="absolute inset-0 bg-gradient-to-br from-espresso via-brown to-caramel" />
        <img
          src="/login-hero.jpg"
          alt=""
          className="absolute inset-0 size-full object-cover"
          onError={(e) => { e.currentTarget.style.display = 'none' }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-espresso/92 via-espresso/40 to-espresso/10" />

        <div className="relative flex h-full flex-col justify-between p-12">
          <div className="flex items-center gap-3">
            <div className="grid size-12 place-items-center rounded-2xl bg-brand text-2xl font-black text-brown shadow-lg">
              k
            </div>
            <div className="text-cream">
              <p className="text-lg font-extrabold leading-none">كونو</p>
              <p className="text-xs font-semibold text-cream/70">منصّة التوصيل متعدّدة الفئات</p>
            </div>
          </div>

          <div>
            <h2 className="text-3xl font-black leading-snug text-cream">
              لوحة تحكّم واحدة
              <br />
              لتشغيل كونو بالكامل
            </h2>
            <div className="mt-8 space-y-5">
              {HIGHLIGHTS.map((h) => (
                <div key={h.title} className="flex items-start gap-3.5">
                  <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-cream/15 text-brand ring-1 ring-cream/20">
                    <h.icon className="size-5" strokeWidth={2} />
                  </div>
                  <div>
                    <p className="font-bold text-cream">{h.title}</p>
                    <p className="text-sm text-cream/70">{h.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <p className="text-xs text-cream/50">© {new Date().getFullYear()} كونو · جميع الحقوق محفوظة</p>
        </div>
      </aside>

      {/* ── form pane ────────────────────────────────────────── */}
      <main className="relative flex flex-1 items-center justify-center p-6">
        <button
          onClick={toggle}
          className="absolute end-6 top-6 rounded-lg border border-line px-2.5 py-1 text-xs font-bold text-brown-soft hover:bg-cream"
        >
          {lang === 'ar' ? 'English' : 'العربية'}
        </button>
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <div className="grid size-14 place-items-center rounded-2xl bg-brown text-2xl font-black text-brand">
              k
            </div>
          </div>

          <h1 className="text-2xl font-black text-espresso">تسجيل الدخول</h1>
          <p className="mt-1.5 text-sm text-ink-soft">
            ادخل ببيانات حساب المدير للوصول إلى لوحة الإدارة.
          </p>

          <form onSubmit={submit} className="mt-8 space-y-4">
            <Field label="رقم الجوال">
              <input
                dir="ltr"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+9665XXXXXXXX"
                className="h-12 w-full rounded-xl border border-line bg-white px-4 text-left text-sm outline-none focus:border-brown focus:ring-2 focus:ring-brand/30"
              />
            </Field>

            <Field label="كلمة المرور">
              <div className="relative">
                <input
                  dir="ltr"
                  type={showPw ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="h-12 w-full rounded-xl border border-line bg-white px-4 pe-11 text-left text-sm outline-none focus:border-brown focus:ring-2 focus:ring-brand/30"
                />
                <button
                  type="button"
                  onClick={() => setShowPw((v) => !v)}
                  className="absolute inset-y-0 end-3 grid place-items-center text-ink-soft hover:text-brown"
                  tabIndex={-1}
                >
                  {showPw ? <EyeOff className="size-[18px]" /> : <Eye className="size-[18px]" />}
                </button>
              </div>
            </Field>

            {error && (
              <div className="rounded-xl bg-danger/10 px-3.5 py-2.5 text-sm font-semibold text-danger">
                {error}
              </div>
            )}

            <Button type="submit" size="lg" className="w-full gap-2" disabled={loading}>
              {loading ? 'جارٍ الدخول…' : <>دخول <ArrowLeft className="size-4" /></>}
            </Button>
          </form>

          <div className="mt-6 rounded-xl border border-dashed border-line bg-white/60 px-3.5 py-3 text-xs text-ink-soft">
            <span className="font-bold text-brown-soft">حساب تجريبي:</span>{' '}
            <span dir="ltr">+966500000000 / Kono@12345</span>
          </div>
        </div>
      </main>
    </div>
  )
}

function Field({ label, children }) {
  return (
    <label className={clsx('block')}>
      <span className="mb-1.5 block text-sm font-bold text-ink-soft">{label}</span>
      {children}
    </label>
  )
}
