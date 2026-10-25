import { useState } from 'react'
import { useVerticals, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Card, Button, Input, Spinner } from '../components/ui'

export default function Verticals() {
  const { data, isLoading } = useVerticals()
  if (isLoading || !data)
    return (
      <div className="grid h-40 place-items-center">
        <Spinner className="size-7" />
      </div>
    )
  return (
    <div>
      <PageHeader title="الأقسام" subtitle="أقسام الشاشة الرئيسية في التطبيق" />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {data.map((v) => (
          <VerticalCard key={v.id} v={v} />
        ))}
      </div>
    </div>
  )
}

function VerticalCard({ v }) {
  const toast = useToast()
  const save = useApiMutation((body) => api.put(`/admin/verticals/${v.id}`, body), ['a-verticals', 'a-promotions'])
  const [m, setM] = useState({
    titleAr: v.titleAr,
    subtitleAr: v.subtitleAr,
    icon: v.icon,
    imageUrl: strip(v.imageUrl),
    sortOrder: v.sortOrder,
    isActive: true,
  })
  const set = (k, val) => setM({ ...m, [k]: val })

  return (
    <Card>
      <div className="mb-3 flex items-center gap-3">
        <img src={v.imageUrl} alt="" className="size-14 rounded-xl object-cover" />
        <div>
          <div className="font-extrabold text-espresso">{v.kind}</div>
          <div className="text-xs text-ink-soft">{v.icon}</div>
        </div>
      </div>
      <div className="space-y-3">
        <Input label="العنوان" value={m.titleAr} onChange={(e) => set('titleAr', e.target.value)} />
        <Input label="الوصف" value={m.subtitleAr} onChange={(e) => set('subtitleAr', e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="الأيقونة (SF Symbol)" dir="ltr" value={m.icon} onChange={(e) => set('icon', e.target.value)} />
          <Input label="الترتيب" type="number" value={m.sortOrder} onChange={(e) => set('sortOrder', +e.target.value)} />
        </div>
        <Input label="رابط الصورة" dir="ltr" value={m.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={m.isActive} onChange={(e) => set('isActive', e.target.checked)} />
          مفعّل
        </label>
        <Button
          className="w-full"
          disabled={save.isPending}
          onClick={() =>
            save
              .mutateAsync(m)
              .then(() => toast.ok('تم الحفظ'))
              .catch((e) => toast.error(apiError(e)))
          }
        >
          {save.isPending ? '…' : 'حفظ'}
        </Button>
      </div>
    </Card>
  )
}

function strip(url) {
  const i = (url || '').indexOf('/img/')
  return i >= 0 ? url.slice(i) : url || ''
}
