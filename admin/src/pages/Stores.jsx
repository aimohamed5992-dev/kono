import { useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { useAdminStore, useAdminStores, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Badge, Button, Input, Select, currency } from '../components/ui'
import { DataTable, Pagination } from '../components/table'
import { Drawer } from '../components/Modal'

const KINDS = [
  ['Restaurants', 'مطاعم'],
  ['Groceries', 'بقالة'],
  ['Pharmacy', 'صيدليات'],
  ['Sweets', 'حلويات'],
  ['Flowers', 'ورود'],
  ['Water', 'مياه'],
]
const kindAr = (k) => KINDS.find((x) => x[0] === k)?.[1] ?? k

export default function Stores() {
  const [page, setPage] = useState(1)
  const [search, setSearch] = useState('')
  const [kind, setKind] = useState('')
  const [editId, setEditId] = useState(undefined) // undefined = closed, null = new, number = edit
  const toast = useToast()

  const { data, isLoading } = useAdminStores({ search, kind, page, pageSize: 12 })

  const setStatus = useApiMutation(
    ({ id, body }) => api.patch(`/admin/stores/${id}/status`, body),
    ['admin-stores']
  )
  const remove = useApiMutation((id) => api.delete(`/admin/stores/${id}`), ['admin-stores'])

  const columns = [
    {
      key: 'name',
      header: 'المتجر',
      render: (r) => (
        <div className="flex items-center gap-3">
          <img src={r.heroImageUrl} alt="" className="size-10 rounded-lg object-cover" />
          <div>
            <div className="font-bold text-ink">{r.name}</div>
            <div className="text-xs text-ink-soft">{r.cuisineAr}</div>
          </div>
        </div>
      ),
    },
    { key: 'kind', header: 'القسم', render: (r) => <Badge tone="brand">{kindAr(r.kind)}</Badge> },
    { key: 'rating', header: 'التقييم', render: (r) => `⭐ ${r.rating.toFixed(1)}` },
    { key: 'etaMinutes', header: 'الوقت', render: (r) => `${r.etaMinutes} د` },
    { key: 'deliveryFee', header: 'التوصيل', render: (r) => (r.deliveryFee === 0 ? 'مجاني' : currency(r.deliveryFee)) },
    { key: 'productCount', header: 'أصناف' },
    {
      key: 'status',
      header: 'الحالة',
      render: (r) => (
        <div className="flex gap-1.5">
          <Badge tone={r.isActive ? 'ok' : 'danger'}>{r.isActive ? 'نشط' : 'موقوف'}</Badge>
          {r.isClosed && <Badge tone="warn">مغلق</Badge>}
        </div>
      ),
    },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <div className="flex justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
          <Button size="sm" variant="ghost" onClick={() => setEditId(r.id)}>
            تعديل
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() =>
              setStatus.mutateAsync({ id: r.id, body: { isActive: r.isActive, isClosed: !r.isClosed } })
            }
          >
            {r.isClosed ? 'فتح' : 'إغلاق'}
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => {
              if (confirm(`إيقاف متجر «${r.name}»؟`))
                remove.mutateAsync(r.id).then(() => toast.ok('تم الإيقاف'))
            }}
          >
            حذف
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="المتاجر"
        subtitle={data ? `${data.total} متجر` : ''}
        action={
          <Button variant="brand" onClick={() => setEditId(null)}>
            <Plus className="size-4" />
            متجر جديد
          </Button>
        }
      />

      <div className="mb-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute right-3 top-1/2 size-4 -translate-y-1/2 text-ink-soft" />
          <Input
            placeholder="ابحث باسم المتجر…"
            className="pr-9"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(1)
            }}
          />
        </div>
        <Select value={kind} onChange={(e) => { setKind(e.target.value); setPage(1) }} className="w-40">
          <option value="">كل الأقسام</option>
          {KINDS.map(([k, ar]) => (
            <option key={k} value={k}>
              {ar}
            </option>
          ))}
        </Select>
      </div>

      <DataTable columns={columns} rows={data?.items ?? []} loading={isLoading} empty="لا توجد متاجر" />
      <Pagination page={page} totalPages={data?.totalPages ?? 1} onChange={setPage} />

      {editId !== undefined && (
        <StoreFormModal id={editId} onClose={() => setEditId(undefined)} />
      )}
    </div>
  )
}

function StoreFormModal({ id, onClose }) {
  const isNew = id === null
  const { data: store } = useAdminStore(isNew ? null : id)
  const toast = useToast()
  const save = useApiMutation(
    (body) => (isNew ? api.post('/admin/stores', body) : api.put(`/admin/stores/${id}`, body)),
    ['admin-stores', 'admin-store']
  )

  const [form, setForm] = useState(null)
  const model = form ?? (isNew ? blankStore() : store ? fromStore(store) : null)

  if (!model) return null
  const set = (k, v) => setForm({ ...model, [k]: v })

  async function submit(e) {
    e.preventDefault()
    try {
      await save.mutateAsync({ ...model, tags: (model.tagsText || '').split(',').map((t) => t.trim()).filter(Boolean) })
      toast.ok(isNew ? 'تم إنشاء المتجر' : 'تم الحفظ')
      onClose()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  return (
    <Drawer
      open
      onClose={onClose}
      title={isNew ? 'متجر جديد' : `تعديل: ${model.name}`}
      subtitle={isNew ? 'أضف متجراً جديداً إلى المنصّة' : 'تعديل بيانات المتجر'}
      size="xl"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            إلغاء
          </Button>
          <Button form="store-form" type="submit" disabled={save.isPending}>
            {save.isPending ? 'جارٍ الحفظ…' : 'حفظ'}
          </Button>
        </>
      }
    >
      <form id="store-form" onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Input label="الاسم" value={model.name} onChange={(e) => set('name', e.target.value)} required />
        <Select label="القسم" value={model.kind} onChange={(e) => set('kind', e.target.value)}>
          {KINDS.map(([k, ar]) => (
            <option key={k} value={k}>
              {ar}
            </option>
          ))}
        </Select>
        <Input label="الوصف" value={model.cuisineAr} onChange={(e) => set('cuisineAr', e.target.value)} />
        <Input label="وسم عرض (اختياري)" value={model.promoTagAr ?? ''} onChange={(e) => set('promoTagAr', e.target.value)} />
        <Input label="التقييم" type="number" step="0.1" value={model.rating} onChange={(e) => set('rating', +e.target.value)} />
        <Input label="عدد التقييمات" type="number" value={model.ratingCount} onChange={(e) => set('ratingCount', +e.target.value)} />
        <Input label="وقت التوصيل (دقيقة)" type="number" value={model.etaMinutes} onChange={(e) => set('etaMinutes', +e.target.value)} />
        <Input label="رسوم التوصيل" type="number" step="0.5" value={model.deliveryFee} onChange={(e) => set('deliveryFee', +e.target.value)} />
        <Input label="الحد الأدنى للطلب" type="number" value={model.minOrder} onChange={(e) => set('minOrder', +e.target.value)} />
        <Input label="المسافة (كم)" type="number" step="0.1" value={model.distanceKm} onChange={(e) => set('distanceKm', +e.target.value)} />
        <Input label="رابط صورة الغلاف" dir="ltr" value={model.heroImageUrl} onChange={(e) => set('heroImageUrl', e.target.value)} />
        <Input label="مدة التجهيز (دقيقة)" type="number" value={model.prepMinutes} onChange={(e) => set('prepMinutes', +e.target.value)} />
        <Input label="العنوان" value={model.addressAr} onChange={(e) => set('addressAr', e.target.value)} />
        <Input label="الوسوم (مفصولة بفاصلة)" value={model.tagsText} onChange={(e) => set('tagsText', e.target.value)} />
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={model.isActive} onChange={(e) => set('isActive', e.target.checked)} />
          نشط
        </label>
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={model.isClosed} onChange={(e) => set('isClosed', e.target.checked)} />
          مغلق مؤقتاً
        </label>
      </form>
    </Drawer>
  )
}

function blankStore() {
  return {
    name: '', kind: 'Restaurants', cuisineAr: '', promoTagAr: '', rating: 4.5, ratingCount: 0,
    etaMinutes: 30, deliveryFee: 0, minOrder: 0, distanceKm: 2, heroImageUrl: '', logoImageUrl: '',
    priceLevel: 2, prepMinutes: 15, isActive: true, isClosed: false, latitude: 24.71, longitude: 46.67,
    addressAr: 'الرياض', cityAr: 'الرياض', tagsText: '',
  }
}
function fromStore(s) {
  return {
    name: s.name, kind: s.kind, cuisineAr: s.cuisineAr, promoTagAr: s.promoTagAr ?? '',
    rating: s.rating, ratingCount: s.ratingCount, etaMinutes: s.etaMinutes, deliveryFee: s.deliveryFee,
    minOrder: s.minOrder, distanceKm: s.distanceKm, heroImageUrl: stripBase(s.heroImageUrl),
    logoImageUrl: stripBase(s.logoImageUrl), priceLevel: s.priceLevel, prepMinutes: s.prepMinutes,
    isActive: s.isActive, isClosed: s.isClosed, latitude: s.latitude, longitude: s.longitude,
    addressAr: s.addressAr, cityAr: s.cityAr, tagsText: (s.tags || []).join(', '),
  }
}
function stripBase(url) {
  if (!url) return ''
  const i = url.indexOf('/img/')
  return i >= 0 ? url.slice(i) : url
}
