import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Store, ArrowLeft } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { Button } from '../components/ui'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [phone, setPhone] = useState('+966500000001')
  const [password, setPassword] = useState('Kono@12345')
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
    <div className="flex min-h-screen">
      <aside className="relative hidden w-1/2 overflow-hidden bg-gradient-to-br from-espresso via-brown to-caramel lg:block">
        <div className="relative flex h-full flex-col justify-between p-12 text-cream">
          <div className="flex items-center gap-3">
            <div className="grid size-12 place-items-center rounded-2xl bg-brand text-2xl font-black text-brown">ك</div>
            <div>
              <p className="text-lg font-extrabold leading-none">كونو للأعمال</p>
              <p className="text-xs text-cream/70">بوابة المتاجر الشريكة</p>
            </div>
          </div>
          <div>
            <h2 className="text-3xl font-black leading-snug">
              أدِر متجرك على كونو
              <br /> من المتصفّح
            </h2>
            <ul className="mt-6 space-y-2.5 text-sm text-cream/80">
              <li>• استقبل الطلبات وأكّدها لحظياً</li>
              <li>• عدّل القائمة والأسعار والتوفّر</li>
              <li>• تابع المبيعات والتقييمات</li>
              <li>• راسل العملاء والمناديب مباشرة</li>
            </ul>
          </div>
          <p className="text-xs text-cream/50">© {new Date().getFullYear()} كونو</p>
        </div>
      </aside>

      <main className="flex flex-1 items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-6 grid size-14 place-items-center rounded-2xl bg-brown text-cream lg:hidden">
            <Store className="size-6" />
          </div>
          <h1 className="text-2xl font-black text-espresso">تسجيل دخول المتجر</h1>
          <p className="mt-1.5 text-sm text-ink-soft">ادخل ببيانات حساب متجرك على كونو.</p>

          <form onSubmit={submit} className="mt-8 space-y-4">
            <label className="block">
              <span className="mb-1.5 block text-sm font-bold text-ink-soft">رقم جوال المتجر</span>
              <input
                dir="ltr"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="h-12 w-full rounded-xl border border-line bg-white px-4 text-left text-sm outline-none focus:border-brown focus:ring-2 focus:ring-brand/30"
              />
            </label>
            <label className="block">
              <span className="mb-1.5 block text-sm font-bold text-ink-soft">كلمة المرور</span>
              <input
                dir="ltr"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="h-12 w-full rounded-xl border border-line bg-white px-4 text-left text-sm outline-none focus:border-brown focus:ring-2 focus:ring-brand/30"
              />
            </label>
            {error && (
              <div className="rounded-xl bg-danger/10 px-3.5 py-2.5 text-sm font-semibold text-danger">{error}</div>
            )}
            <Button type="submit" size="lg" className="w-full gap-2" disabled={loading}>
              {loading ? 'جارٍ الدخول…' : <>دخول <ArrowLeft className="size-4" /></>}
            </Button>
          </form>

          <div className="mt-6 rounded-xl border border-dashed border-line bg-white/60 px-3.5 py-3 text-xs text-ink-soft">
            <span className="font-bold text-brown-soft">حساب تجريبي:</span>{' '}
            <span dir="ltr">+966500000001 / Kono@12345</span>
          </div>
        </div>
      </main>
    </div>
  )
}
