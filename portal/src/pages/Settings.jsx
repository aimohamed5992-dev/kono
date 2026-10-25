import { useEffect, useState } from 'react'
import { Star, Clock, MapPin, Building2, ListChecks, UserRound } from 'lucide-react'
import { useStore, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Card, Button, Input, Spinner } from '../components/ui'
import { clsx } from '../components/clsx'

export default function Settings() {
  const { data: store, isLoading } = useStore()
  const toast = useToast()

  const save = useApiMutation((body) => api.patch('/merchant/store', body), ['store'])
  const status = useApiMutation((body) => api.post('/merchant/status', body), ['store'])

  const [f, setF] = useState(null)
  const [prep, setPrep] = useState(15)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (store && !f) {
      setF({
        name: store.name, cuisineAr: store.cuisineAr, addressAr: store.addressAr,
        openingHoursAr: store.openingHoursAr, ibanLast4: store.ibanLast4,
      })
      setPrep(store.prepMinutes)
      setBusy(store.isBusy)
    }
  }, [store, f])

  if (isLoading || !store || !f)
    return <div className="grid h-64 place-items-center"><Spinner className="size-8" /></div>

  const set = (k, v) => setF((p) => ({ ...p, [k]: v }))

  return (
    <div className="max-w-3xl">
      <PageHeader title="إعدادات المتجر" subtitle={store.name} />

      <Card className="mb-5">
        <div className="flex items-center gap-4">
          <div className="grid size-16 place-items-center overflow-hidden rounded-2xl bg-brand text-2xl font-black text-brown">
            {store.logoUrl ? <img src={store.logoUrl} alt="" className="size-full object-cover" /> : store.name?.[0]}
          </div>
          <div>
            <div className="text-lg font-extrabold text-espresso">{store.name}</div>
            <div className="flex items-center gap-2 text-sm text-ink-soft">
              <Star className="size-4 text-rating" fill="currentColor" />
              {store.rating.toFixed(1)} ({store.ratingCount}) · {store.menuItemCount} صنف
            </div>
            {store.onboardingStatus === 'under_review' && (
              <span className="mt-1 inline-block rounded-full bg-caramel/15 px-2 py-0.5 text-xs font-bold text-caramel">
                طلب التسجيل قيد المراجعة
              </span>
            )}
          </div>
        </div>
      </Card>

      <Card className="mb-5 space-y-4">
        <h2 className="font-extrabold text-espresso">بيانات المتجر</h2>
        <Field icon={Building2} label="اسم المتجر">
          <Input value={f.name} onChange={(e) => set('name', e.target.value)} />
        </Field>
        <Field icon={ListChecks} label="التصنيف">
          <Input value={f.cuisineAr} onChange={(e) => set('cuisineAr', e.target.value)} />
        </Field>
        <Field icon={MapPin} label="العنوان">
          <Input value={f.addressAr} onChange={(e) => set('addressAr', e.target.value)} />
        </Field>
        <Field icon={Clock} label="ساعات العمل">
          <Input value={f.openingHoursAr} onChange={(e) => set('openingHoursAr', e.target.value)} />
        </Field>
        <Field icon={UserRound} label="آخر ٤ أرقام من الآيبان">
          <Input
            dir="ltr"
            value={f.ibanLast4}
            onChange={(e) => set('ibanLast4', e.target.value.replace(/\D/g, '').slice(0, 4))}
            placeholder="4821"
          />
        </Field>
        <div className="flex justify-end">
          <Button
            disabled={save.isPending}
            onClick={() =>
              save.mutateAsync(f).then(() => toast.ok('تم حفظ البيانات')).catch((e) => toast.error(apiError(e)))
            }
          >
            حفظ البيانات
          </Button>
        </div>
      </Card>

      <Card className="space-y-4">
        <h2 className="font-extrabold text-espresso">التشغيل</h2>

        <div>
          <div className="mb-1 flex justify-between text-sm">
            <span className="font-semibold text-ink">متوسط وقت التجهيز</span>
            <span className="font-bold text-brown">{prep} دقيقة</span>
          </div>
          <input
            type="range" min="5" max="45" step="5" value={prep}
            onChange={(e) => setPrep(+e.target.value)}
            onMouseUp={() => status.mutate({ isOnline: null, isBusy: null, prepMinutes: prep })}
            onTouchEnd={() => status.mutate({ isOnline: null, isBusy: null, prepMinutes: prep })}
            className="w-full accent-brown"
          />
        </div>

        <label className="flex items-center justify-between rounded-xl bg-canvas px-3 py-3">
          <div>
            <div className="text-sm font-bold text-ink">وضع الازدحام</div>
            <div className="text-xs text-ink-soft">يضيف وقتاً إضافياً للتجهيز مؤقتاً</div>
          </div>
          <button
            onClick={() => {
              const v = !busy
              setBusy(v)
              status.mutate({ isOnline: null, isBusy: v, prepMinutes: null })
            }}
            className={clsx('h-6 w-11 rounded-full p-0.5 transition', busy ? 'bg-caramel' : 'bg-line')}
          >
            <span className={clsx('block size-5 rounded-full bg-white transition', busy && 'translate-x-5')} />
          </button>
        </label>
      </Card>
    </div>
  )
}

function Field({ icon: Icon, label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 flex items-center gap-1.5 text-sm font-bold text-ink-soft">
        <Icon className="size-4" /> {label}
      </span>
      {children}
    </label>
  )
}
