import { useState } from 'react'
import { useAdminOrder, useAdminOrders, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Badge, Button, currency, dateAr } from '../components/ui'
import { DataTable, Pagination } from '../components/table'
import { Drawer } from '../components/Modal'

const TABS = [
  { key: 'all', label: 'الكل' },
  { key: 'active', label: 'نشطة' },
  { key: 'completed', label: 'مكتملة' },
  { key: 'cancelled', label: 'ملغاة' },
]

const STAGES = [
  'Placed', 'Confirmed', 'Preparing', 'ReadyForPickup', 'CourierAssigned',
  'PickedUp', 'OnTheWay', 'Arrived', 'Delivered', 'Cancelled',
]
const STAGE_AR = {
  Placed: 'مستلَم', Confirmed: 'مؤكَّد', Preparing: 'قيد التجهيز', ReadyForPickup: 'جاهز',
  CourierAssigned: 'أُسند لمندوب', PickedUp: 'استلمه المندوب', OnTheWay: 'في الطريق',
  Arrived: 'وصل', Delivered: 'تم التوصيل', Cancelled: 'ملغى',
}
const toneFor = (s) =>
  s === 'Delivered' ? 'ok' : s === 'Cancelled' ? 'danger' : s === 'Placed' ? 'warn' : 'info'

export default function Orders() {
  const [tab, setTab] = useState('all')
  const [page, setPage] = useState(1)
  const [openId, setOpenId] = useState(null)
  const { data, isLoading } = useAdminOrders({ status: tab, page, pageSize: 12 })

  const columns = [
    { key: 'shortCode', header: 'الرقم', render: (r) => <span className="font-bold text-ink">{r.shortCode}</span> },
    { key: 'customerNameAr', header: 'العميل' },
    { key: 'storeName', header: 'المتجر' },
    { key: 'itemCount', header: 'أصناف', render: (r) => `${r.itemCount}` },
    { key: 'total', header: 'الإجمالي', render: (r) => currency(r.total) },
    {
      key: 'stage',
      header: 'الحالة',
      render: (r) => <Badge tone={toneFor(r.stage)}>{STAGE_AR[r.stage] ?? r.stage}</Badge>,
    },
    { key: 'courierNameAr', header: 'المندوب', render: (r) => r.courierNameAr || '—' },
    { key: 'placedAt', header: 'التاريخ', render: (r) => <span className="text-xs text-ink-soft">{dateAr(r.placedAt)}</span> },
  ]

  return (
    <div>
      <PageHeader title="الطلبات" subtitle={data ? `${data.total} طلب` : ''} />

      <div className="mb-4 flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => {
              setTab(t.key)
              setPage(1)
            }}
            className={
              'rounded-full px-4 py-1.5 text-sm font-bold transition ' +
              (tab === t.key ? 'bg-brown text-cream' : 'bg-white text-brown-soft hover:bg-cream')
            }
          >
            {t.label}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        rows={data?.items ?? []}
        loading={isLoading}
        onRowClick={(r) => setOpenId(r.id)}
        empty="لا توجد طلبات"
      />
      <Pagination page={page} totalPages={data?.totalPages ?? 1} onChange={setPage} />

      <OrderDrawer id={openId} onClose={() => setOpenId(null)} />
    </div>
  )
}

function OrderDrawer({ id, onClose }) {
  const { data: order, isLoading } = useAdminOrder(id)
  const toast = useToast()
  const setStage = useApiMutation(
    (stage) => api.post(`/admin/orders/${id}/stage`, { stage }),
    ['admin-orders', 'admin-order', 'stats']
  )

  return (
    <Drawer open={!!id} onClose={onClose} title={order ? order.shortCode : 'الطلب'} size="lg">
      {isLoading || !order ? (
        <div className="py-10 text-center text-ink-soft">جارٍ التحميل…</div>
      ) : (
        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <Badge tone={toneFor(order.stage)}>{order.stageAr ?? STAGE_AR[order.stage] ?? order.stage}</Badge>
            <span className="text-sm text-ink-soft">{dateAr(order.placedAt)}</span>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="card p-4 text-sm">
              <h3 className="mb-2 font-bold text-ink">العميل</h3>
              <Row label="الاسم" value={order.customerNameAr} />
              <Row label="الجوال" value={<span dir="ltr">{order.customerPhone}</span>} />
              <Row label="نوع الطلب" value={order.isDelivery ? 'توصيل' : 'استلام من الفرع'} />
              <Row label="العنوان" value={`${order.addressLabelAr} — ${order.addressLineAr}`} />
            </div>
            <div className="card p-4 text-sm">
              <h3 className="mb-2 font-bold text-ink">المتجر</h3>
              <Row label="الاسم" value={order.storeNameAr} />
              {order.storeCuisineAr && <Row label="التصنيف" value={order.storeCuisineAr} />}
              {order.storeAddressAr && <Row label="العنوان" value={order.storeAddressAr} />}
              {order.storePhone && <Row label="جوال المتجر" value={<span dir="ltr">{order.storePhone}</span>} />}
              <Row label="وقت التجهيز" value={`${order.storeEtaMinutes} دقيقة`} />
            </div>
          </div>

          <div className="card p-4 text-sm">
            <h3 className="mb-2 font-bold text-ink">المندوب والدفع</h3>
            {order.courier ? (
              <>
                <Row label="المندوب" value={`${order.courier.name} · ⭐ ${order.courier.rating.toFixed(1)}`} />
                <Row label="المركبة" value={`${order.courier.vehicleAr} · ${order.courier.plateAr}`} />
                <Row label="أجرة المندوب" value={currency(order.courierPayout)} />
                <Row label="مسافة التوصيل" value={`${order.distanceKm} كم`} />
              </>
            ) : (
              <Row label="المندوب" value="لم يُسند بعد" />
            )}
            <Row label="طريقة الدفع" value={order.paymentLabelAr} />
            {order.scheduledForAr && <Row label="مجدول لـ" value={order.scheduledForAr} />}
          </div>

          {order.noteAr && (
            <div className="rounded-xl bg-warn/10 px-4 py-3 text-sm text-[#8a5a00]">
              <span className="font-bold">ملاحظة العميل: </span>{order.noteAr}
            </div>
          )}

          <div>
            <h3 className="mb-2 font-bold text-ink">الأصناف</h3>
            <ul className="divide-y divide-line rounded-xl border border-line">
              {order.items.map((i) => (
                <li key={i.id} className="flex items-center gap-3 p-3">
                  <img src={i.imageUrl} alt="" className="size-10 rounded-lg object-cover" />
                  <div className="flex-1">
                    <div className="text-sm font-semibold text-ink">
                      {i.quantity}× {i.nameAr}
                    </div>
                    {i.selectedOptionsAr && (
                      <div className="text-xs text-ink-soft">{i.selectedOptionsAr}</div>
                    )}
                  </div>
                  <span className="text-sm font-bold">{currency(i.lineTotal)}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="card space-y-1.5 p-4 text-sm">
            <Row label="المجموع الفرعي" value={currency(order.subtotal)} />
            <Row label="التوصيل" value={currency(order.deliveryFee)} />
            <Row label="الخدمة" value={currency(order.serviceFee)} />
            {order.discount > 0 && <Row label="الخصم" value={`- ${currency(order.discount)}`} />}
            {order.tip > 0 && <Row label="الإكرامية" value={currency(order.tip)} />}
            <div className="mt-2 flex justify-between border-t border-line pt-2 text-base font-extrabold">
              <span>الإجمالي</span>
              <span>{currency(order.total)}</span>
            </div>
          </div>

          <div>
            <h3 className="mb-2 font-bold text-ink">سجل المراحل</h3>
            <ol className="space-y-2">
              {order.events.map((e, i) => (
                <li key={i} className="flex items-center gap-3 text-sm">
                  <span className="size-2 rounded-full bg-brown" />
                  <span className="font-semibold text-ink">{STAGE_AR[e.stage] ?? e.stage}</span>
                  <span className="text-xs text-ink-soft">{dateAr(e.at)}</span>
                  {e.note && <span className="text-xs text-ink-soft">· {e.note}</span>}
                </li>
              ))}
            </ol>
          </div>

          <div>
            <h3 className="mb-2 font-bold text-ink">تغيير المرحلة (إداري)</h3>
            <div className="flex flex-wrap gap-2">
              {STAGES.map((s) => (
                <button
                  key={s}
                  disabled={setStage.isPending || s === order.stage}
                  onClick={() =>
                    setStage
                      .mutateAsync(s)
                      .then(() => toast.ok('تم تحديث المرحلة'))
                      .catch((e) => toast.error(apiError(e)))
                  }
                  className={
                    'rounded-lg px-2.5 py-1 text-xs font-bold transition ' +
                    (s === order.stage
                      ? 'bg-brown text-cream'
                      : 'bg-white text-brown-soft hover:bg-cream disabled:opacity-40')
                  }
                >
                  {STAGE_AR[s]}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </Drawer>
  )
}

function Row({ label, value, hide }) {
  if (hide) return null
  return (
    <div className="flex justify-between py-0.5">
      <span className="text-ink-soft">{label}</span>
      <span className="font-semibold text-ink">{value}</span>
    </div>
  )
}
