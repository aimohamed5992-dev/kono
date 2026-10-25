import { useState } from 'react'
import { Plus } from 'lucide-react'
import { useCouriers, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Badge, Button, Input, Select, currency } from '../components/ui'
import { DataTable } from '../components/table'
import { Drawer } from '../components/Modal'

const VEHICLES = ['دباب', 'دراجة نارية', 'سيارة', 'دراجة هوائية', 'سيراً على الأقدام']

export default function Couriers() {
  const { data: rows, isLoading } = useCouriers()
  const [edit, setEdit] = useState(null)
  const [creating, setCreating] = useState(false)

  const columns = [
    {
      key: 'name',
      header: 'المندوب',
      render: (r) => (
        <div>
          <div className="font-bold text-ink">{r.name}</div>
          <div className="text-xs text-ink-soft" dir="ltr">
            {r.phone}
          </div>
        </div>
      ),
    },
    { key: 'rating', header: 'التقييم', render: (r) => `⭐ ${r.rating.toFixed(1)}` },
    { key: 'trips', header: 'الرحلات', render: (r) => r.trips.toLocaleString('en-US') },
    { key: 'vehicleAr', header: 'المركبة' },
    { key: 'plateAr', header: 'اللوحة' },
    { key: 'acceptanceRate', header: 'نسبة القبول', render: (r) => `${r.acceptanceRate}٪` },
    { key: 'todayEarnings', header: 'أرباح اليوم', render: (r) => currency(r.todayEarnings) },
    { key: 'todayTrips', header: 'رحلات اليوم' },
    {
      key: 'isOnline',
      header: 'الحالة',
      render: (r) => <Badge tone={r.isOnline ? 'ok' : 'neutral'}>{r.isOnline ? 'متصل' : 'غير متصل'}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <Button size="sm" variant="ghost" onClick={() => setEdit(r)}>
          تعديل
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="المناديب"
        subtitle={rows ? `${rows.length} مندوب` : ''}
        action={
          <Button variant="brand" onClick={() => setCreating(true)}>
            <Plus className="size-4" />
            مندوب جديد
          </Button>
        }
      />
      <DataTable columns={columns} rows={rows ?? []} loading={isLoading} empty="لا يوجد مناديب" />

      {edit && <CourierDrawer courier={edit} onClose={() => setEdit(null)} />}
      {creating && <CourierDrawer onClose={() => setCreating(false)} />}
    </div>
  )
}

function CourierDrawer({ courier, onClose }) {
  const isNew = !courier
  const toast = useToast()
  const save = useApiMutation(
    (body) => (isNew ? api.post('/admin/couriers', body) : api.put(`/admin/couriers/${courier.id}`, body)),
    ['couriers']
  )
  const [m, setM] = useState({
    name: courier?.name ?? '',
    phone: courier?.phone ?? '+9665',
    vehicleAr: courier?.vehicleAr ?? 'دباب',
    plateAr: courier?.plateAr ?? '',
    acceptanceRate: courier?.acceptanceRate ?? 100,
    isOnline: courier?.isOnline ?? false,
  })
  const set = (k, v) => setM((p) => ({ ...p, [k]: v }))

  async function submit() {
    if (isNew && (!m.name.trim() || m.phone.replace(/\D/g, '').length < 12)) {
      toast.error('أدخل الاسم ورقم جوال صحيح')
      return
    }
    try {
      const body = { ...m, acceptanceRate: +m.acceptanceRate }
      if (!isNew) delete body.phone
      await save.mutateAsync(body)
      toast.ok(isNew ? 'تمت إضافة المندوب' : 'تم الحفظ')
      onClose()
    } catch (e) {
      toast.error(apiError(e))
    }
  }

  return (
    <Drawer
      open
      onClose={onClose}
      title={isNew ? 'مندوب جديد' : `تعديل: ${courier.name}`}
      subtitle={isNew ? 'إنشاء حساب مندوب توصيل' : courier.phone}
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            إلغاء
          </Button>
          <Button disabled={save.isPending} onClick={submit}>
            {save.isPending ? 'جارٍ الحفظ…' : isNew ? 'إضافة' : 'حفظ'}
          </Button>
        </>
      }
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="الاسم" value={m.name} onChange={(e) => set('name', e.target.value)} />
        {isNew && (
          <Input
            label="رقم الجوال"
            dir="ltr"
            value={m.phone}
            onChange={(e) => set('phone', e.target.value)}
            placeholder="+9665XXXXXXXX"
          />
        )}
        <Select label="المركبة" value={m.vehicleAr} onChange={(e) => set('vehicleAr', e.target.value)}>
          {VEHICLES.map((v) => (
            <option key={v} value={v}>
              {v}
            </option>
          ))}
        </Select>
        <Input label="رقم اللوحة" value={m.plateAr} onChange={(e) => set('plateAr', e.target.value)} />
        <Input
          label="نسبة القبول ٪"
          type="number"
          value={m.acceptanceRate}
          onChange={(e) => set('acceptanceRate', e.target.value)}
        />
        <label className="flex items-center gap-2 self-end pb-2.5 text-sm font-semibold">
          <input type="checkbox" checked={m.isOnline} onChange={(e) => set('isOnline', e.target.checked)} />
          متصل الآن
        </label>
      </div>

      {isNew && (
        <p className="mt-4 rounded-xl bg-info/8 px-3.5 py-2.5 text-xs text-info">
          يُنشأ حساب المندوب فوراً ويستطيع الدخول للتطبيق برقم جواله عبر رمز التحقق. تُستكمل
          مستنداته (الهوية، الرخصة، تصريح التوصيل) من شاشة تسجيل المندوب في التطبيق.
        </p>
      )}
    </Drawer>
  )
}
