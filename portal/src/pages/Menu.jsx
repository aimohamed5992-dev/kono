import { useState } from 'react'
import { Plus } from 'lucide-react'
import { useMenu, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Button, Input, Spinner, currency } from '../components/ui'
import { Drawer } from '../components/Modal'
import { clsx } from '../components/clsx'

export default function Menu() {
  const { data: items, isLoading } = useMenu()
  const [edit, setEdit] = useState(undefined) // undefined closed | null new | obj edit
  const toast = useToast()

  const stock = useApiMutation(
    ({ id, inStock }) => api.put(`/merchant/menu/${id}`, { inStock }),
    ['menu']
  )

  const byCat = {}
  for (const it of items ?? []) (byCat[it.categoryAr] ??= []).push(it)

  return (
    <div>
      <PageHeader
        title="القائمة"
        subtitle={items ? `${items.length} صنف` : ''}
        action={
          <Button variant="brand" onClick={() => setEdit(null)}>
            <Plus className="size-4" /> صنف جديد
          </Button>
        }
      />

      {isLoading ? (
        <div className="grid h-40 place-items-center"><Spinner className="size-7" /></div>
      ) : (
        <div className="space-y-6">
          {Object.entries(byCat).map(([cat, list]) => (
            <div key={cat}>
              <h2 className="mb-2 font-extrabold text-espresso">{cat}</h2>
              <div className="grid gap-3 md:grid-cols-2">
                {list.map((it) => (
                  <div key={it.productId} className="card flex items-center gap-3 p-3">
                    <img src={it.imageUrl} alt="" className="size-14 rounded-lg object-cover" />
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-bold text-ink">{it.nameAr}</div>
                      <div className="truncate text-xs text-ink-soft">{it.descAr}</div>
                      <div className="mt-0.5 text-sm font-bold text-brown">{currency(it.price)}</div>
                    </div>
                    <div className="flex flex-col items-end gap-1.5">
                      <button
                        onClick={() => stock.mutate({ id: it.productId, inStock: !it.inStock })}
                        className={clsx(
                          'rounded-full px-2.5 py-1 text-xs font-bold',
                          it.inStock ? 'bg-ok/12 text-ok' : 'bg-danger/12 text-danger'
                        )}
                      >
                        {it.inStock ? 'متوفّر' : 'غير متوفّر'}
                      </button>
                      <Button size="sm" variant="ghost" onClick={() => setEdit(it)}>تعديل</Button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {edit !== undefined && <ItemDrawer item={edit} onClose={() => setEdit(undefined)} />}
    </div>
  )
}

function ItemDrawer({ item, onClose }) {
  const isNew = !item
  const toast = useToast()
  const save = useApiMutation(
    (body) => (isNew ? api.post('/merchant/menu', body) : api.put(`/merchant/menu/${item.productId}`, body)),
    ['menu', 'reports']
  )
  const [m, setM] = useState(
    isNew
      ? { nameAr: '', descAr: '', price: 0, inStock: true, imageUrl: '', categoryAr: 'أصناف' }
      : {
          nameAr: item.nameAr, descAr: item.descAr, price: item.price,
          inStock: item.inStock, imageUrl: strip(item.imageUrl), categoryAr: item.categoryAr,
        }
  )
  const set = (k, v) => setM((p) => ({ ...p, [k]: v }))

  return (
    <Drawer
      open
      onClose={onClose}
      title={isNew ? 'صنف جديد' : `تعديل: ${item.nameAr}`}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>إلغاء</Button>
          <Button
            disabled={save.isPending}
            onClick={() =>
              save
                .mutateAsync({ ...m, price: +m.price })
                .then(() => { toast.ok('تم الحفظ'); onClose() })
                .catch((e) => toast.error(apiError(e)))
            }
          >
            حفظ
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Field label="الاسم"><Input value={m.nameAr} onChange={(e) => set('nameAr', e.target.value)} /></Field>
        <Field label="الوصف"><Input value={m.descAr} onChange={(e) => set('descAr', e.target.value)} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="السعر (ر.س)">
            <Input type="number" step="0.5" value={m.price} onChange={(e) => set('price', e.target.value)} />
          </Field>
          <Field label="التصنيف">
            <Input value={m.categoryAr} onChange={(e) => set('categoryAr', e.target.value)} />
          </Field>
        </div>
        <Field label="رابط الصورة"><Input dir="ltr" value={m.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} /></Field>
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={m.inStock} onChange={(e) => set('inStock', e.target.checked)} />
          متوفّر للطلب
        </label>
      </div>
    </Drawer>
  )
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-bold text-ink-soft">{label}</span>
      {children}
    </label>
  )
}
function strip(url) {
  const i = (url || '').indexOf('/img/')
  return i >= 0 ? url.slice(i) : url || ''
}
