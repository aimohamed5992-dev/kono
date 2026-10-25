import { useState } from 'react'
import { Plus } from 'lucide-react'
import { usePromotions, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Card, Button, Input, Spinner } from '../components/ui'
import { Drawer } from '../components/Modal'

export default function Promotions() {
  const { data, isLoading } = usePromotions()
  const [edit, setEdit] = useState(undefined)
  const toast = useToast()
  const remove = useApiMutation((id) => api.delete(`/admin/promotions/${id}`), ['a-promotions'])

  return (
    <div>
      <PageHeader
        title="العروض"
        subtitle="شرائح البانر في الشاشة الرئيسية"
        action={
          <Button variant="brand" onClick={() => setEdit(null)}>
            <Plus className="size-4" />
            عرض جديد
          </Button>
        }
      />

      {isLoading ? (
        <div className="grid h-40 place-items-center">
          <Spinner className="size-7" />
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {(data ?? []).map((p) => (
            <div
              key={p.id}
              className="card overflow-hidden p-0"
              style={{ background: p.backgroundHex }}
            >
              <div className="flex items-center gap-4 p-5">
                <img src={p.imageUrl} alt="" className="size-20 rounded-xl bg-white/40 object-cover" />
                <div className="min-w-0 flex-1">
                  <div className="text-lg font-extrabold text-espresso">{p.titleAr}</div>
                  <div className="text-sm text-brown/80">{p.subtitleAr}</div>
                </div>
              </div>
              <div className="flex gap-2 bg-white/50 px-5 py-3">
                <Button size="sm" variant="ghost" onClick={() => setEdit(p)}>
                  تعديل
                </Button>
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => {
                    if (confirm(`حذف «${p.titleAr}»؟`))
                      remove.mutateAsync(p.id).then(() => toast.ok('تم الحذف'))
                  }}
                >
                  حذف
                </Button>
              </div>
            </div>
          ))}
          {data?.length === 0 && (
            <Card className="text-center text-ink-soft md:col-span-2">لا توجد عروض</Card>
          )}
        </div>
      )}

      {edit !== undefined && <PromoModal promo={edit} onClose={() => setEdit(undefined)} />}
    </div>
  )
}

function PromoModal({ promo, onClose }) {
  const isNew = !promo
  const toast = useToast()
  const save = useApiMutation(
    (body) => (isNew ? api.post('/admin/promotions', body) : api.put(`/admin/promotions/${promo.id}`, body)),
    ['a-promotions']
  )
  const [m, setM] = useState(
    isNew
      ? { titleAr: '', subtitleAr: '', imageUrl: '', backgroundHex: '#FFE79A', sortOrder: 0, isActive: true }
      : {
          titleAr: promo.titleAr,
          subtitleAr: promo.subtitleAr,
          imageUrl: strip(promo.imageUrl),
          backgroundHex: promo.backgroundHex,
          sortOrder: promo.sortOrder,
          isActive: true,
        }
  )
  const set = (k, v) => setM({ ...m, [k]: v })

  return (
    <Drawer
      open
      onClose={onClose}
      title={isNew ? 'عرض جديد' : 'تعديل العرض'}
      subtitle="شريحة بانر في الشاشة الرئيسية للتطبيق"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            إلغاء
          </Button>
          <Button
            disabled={save.isPending}
            onClick={() =>
              save
                .mutateAsync(m)
                .then(() => {
                  toast.ok('تم الحفظ')
                  onClose()
                })
                .catch((e) => toast.error(apiError(e)))
            }
          >
            حفظ
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Input label="العنوان" value={m.titleAr} onChange={(e) => set('titleAr', e.target.value)} />
        <Input label="الوصف" value={m.subtitleAr} onChange={(e) => set('subtitleAr', e.target.value)} />
        <Input label="رابط الصورة" dir="ltr" value={m.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="mb-1.5 block text-sm font-semibold text-ink-soft">لون الخلفية</span>
            <input
              type="color"
              className="h-11 w-full rounded-xl border border-line"
              value={m.backgroundHex}
              onChange={(e) => set('backgroundHex', e.target.value)}
            />
          </label>
          <Input label="الترتيب" type="number" value={m.sortOrder} onChange={(e) => set('sortOrder', +e.target.value)} />
        </div>
        <div className="rounded-xl p-4" style={{ background: m.backgroundHex }}>
          <div className="font-extrabold text-espresso">{m.titleAr || 'معاينة'}</div>
          <div className="text-sm text-brown/80">{m.subtitleAr}</div>
        </div>
      </div>
    </Drawer>
  )
}

function strip(url) {
  const i = (url || '').indexOf('/img/')
  return i >= 0 ? url.slice(i) : url || ''
}
