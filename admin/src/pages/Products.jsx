import { useState } from 'react'
import { Plus } from 'lucide-react'
import { useAdminStores, useStoreMenu, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Card, Badge, Button, Input, Select, currency } from '../components/ui'
import { Drawer } from '../components/Modal'
import { Spinner } from '../components/ui'

export default function Products() {
  const { data: stores } = useAdminStores({ pageSize: 100 })
  const [storeId, setStoreId] = useState(null)
  const list = stores?.items ?? []
  const active = storeId ?? list[0]?.id ?? null

  return (
    <div>
      <PageHeader title="المنتجات" subtitle="إدارة قوائم المتاجر" />
      <div className="mb-4 max-w-xs">
        <Select value={active ?? ''} onChange={(e) => setStoreId(+e.target.value)}>
          {list.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </Select>
      </div>
      {active && <StoreMenu storeId={active} />}
    </div>
  )
}

function StoreMenu({ storeId }) {
  const { data: menu, isLoading } = useStoreMenu(storeId)
  const [edit, setEdit] = useState(undefined) // undefined closed, {} new, product edit
  const toast = useToast()

  const stock = useApiMutation(
    ({ id, inStock }) => api.patch(`/admin/products/${id}/stock`, { inStock }),
    ['admin-menu']
  )
  const remove = useApiMutation((id) => api.delete(`/admin/products/${id}`), ['admin-menu'])

  if (isLoading)
    return (
      <div className="grid h-40 place-items-center">
        <Spinner className="size-7" />
      </div>
    )

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <Button variant="brand" onClick={() => setEdit({})}>
          <Plus className="size-4" />
          صنف جديد
        </Button>
      </div>

      {(menu ?? []).map((cat) => (
        <div key={cat.id}>
          <h3 className="mb-2 font-extrabold text-espresso">{cat.nameAr}</h3>
          <Card className="p-0">
            <ul className="divide-y divide-line">
              {cat.items.map((p) => (
                <li key={p.id} className="flex items-center gap-3 p-3">
                  <img src={p.imageUrl} alt="" className="size-12 rounded-lg object-cover" />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-ink">{p.nameAr}</span>
                      {p.isPopular && <Badge tone="warn">الأكثر طلباً</Badge>}
                      {!p.inStock && <Badge tone="danger">غير متاح</Badge>}
                    </div>
                    <div className="truncate text-xs text-ink-soft">{p.descAr}</div>
                  </div>
                  <div className="text-left">
                    <div className="font-bold text-ink">{currency(p.price)}</div>
                    {p.oldPrice > 0 && (
                      <div className="text-xs text-ink-soft line-through">{currency(p.oldPrice)}</div>
                    )}
                  </div>
                  <div className="flex gap-1.5">
                    <Button size="sm" variant="ghost" onClick={() => setEdit(p)}>
                      تعديل
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => stock.mutateAsync({ id: p.id, inStock: !p.inStock })}
                    >
                      {p.inStock ? 'إيقاف' : 'تفعيل'}
                    </Button>
                    <Button
                      size="sm"
                      variant="danger"
                      onClick={() => {
                        if (confirm(`حذف «${p.nameAr}»؟`))
                          remove.mutateAsync(p.id).then(() => toast.ok('تم الحذف'))
                      }}
                    >
                      حذف
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      ))}

      {edit !== undefined && (
        <ProductModal
          storeId={storeId}
          product={edit.id ? edit : null}
          categories={menu ?? []}
          onClose={() => setEdit(undefined)}
        />
      )}
    </div>
  )
}

function ProductModal({ storeId, product, categories, onClose }) {
  const isNew = !product
  const toast = useToast()
  const save = useApiMutation(
    (body) =>
      isNew
        ? api.post(`/admin/stores/${storeId}/products`, body)
        : api.put(`/admin/products/${product.id}`, body),
    ['admin-menu']
  )

  const [m, setM] = useState(
    isNew
      ? { nameAr: '', descAr: '', price: 0, oldPrice: '', imageUrl: '', isPopular: false, calories: '', unitAr: '', inStock: true, menuCategoryId: categories[0]?.id ?? 0, newCategoryNameAr: '' }
      : {
          nameAr: product.nameAr, descAr: product.descAr, price: product.price,
          oldPrice: product.oldPrice ?? '', imageUrl: strip(product.imageUrl), isPopular: product.isPopular,
          calories: product.calories ?? '', unitAr: product.unitAr ?? '', inStock: product.inStock,
          menuCategoryId: product.menuCategoryId, newCategoryNameAr: '',
        }
  )
  const set = (k, v) => setM({ ...m, [k]: v })

  async function submit(e) {
    e.preventDefault()
    try {
      await save.mutateAsync({
        ...m,
        price: +m.price,
        oldPrice: m.oldPrice === '' ? null : +m.oldPrice,
        calories: m.calories === '' ? null : +m.calories,
        menuCategoryId: +m.menuCategoryId,
      })
      toast.ok(isNew ? 'أُضيف الصنف' : 'تم الحفظ')
      onClose()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  return (
    <Drawer
      open
      onClose={onClose}
      title={isNew ? 'صنف جديد' : `تعديل: ${m.nameAr}`}
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            إلغاء
          </Button>
          <Button form="product-form" type="submit" disabled={save.isPending}>
            {save.isPending ? '…' : 'حفظ'}
          </Button>
        </>
      }
    >
      <form id="product-form" onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Input label="الاسم" value={m.nameAr} onChange={(e) => set('nameAr', e.target.value)} required />
        <Input label="السعر" type="number" step="0.5" value={m.price} onChange={(e) => set('price', e.target.value)} required />
        <Input label="الوصف" className="sm:col-span-2" value={m.descAr} onChange={(e) => set('descAr', e.target.value)} />
        <Input label="السعر قبل الخصم" type="number" step="0.5" value={m.oldPrice} onChange={(e) => set('oldPrice', e.target.value)} />
        <Input label="السعرات" type="number" value={m.calories} onChange={(e) => set('calories', e.target.value)} />
        <Input label="الوحدة (١ كجم…)" value={m.unitAr} onChange={(e) => set('unitAr', e.target.value)} />
        <Input label="رابط الصورة" dir="ltr" value={m.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
        <Select label="التصنيف" value={m.menuCategoryId} onChange={(e) => set('menuCategoryId', e.target.value)}>
          <option value={0}>— تصنيف جديد —</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nameAr}
            </option>
          ))}
        </Select>
        {String(m.menuCategoryId) === '0' && (
          <Input label="اسم التصنيف الجديد" value={m.newCategoryNameAr} onChange={(e) => set('newCategoryNameAr', e.target.value)} />
        )}
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={m.isPopular} onChange={(e) => set('isPopular', e.target.checked)} />
          الأكثر طلباً
        </label>
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={m.inStock} onChange={(e) => set('inStock', e.target.checked)} />
          متاح
        </label>
      </form>
    </Drawer>
  )
}

function strip(url) {
  const i = (url || '').indexOf('/img/')
  return i >= 0 ? url.slice(i) : url || ''
}
