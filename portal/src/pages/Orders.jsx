import { useState } from 'react'
import { Clock, ChefHat, PackageCheck, Bike, CheckCircle2, MapPin, Phone } from 'lucide-react'
import { useTickets, useTicketDetail, useApiMutation } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Badge, Button, Spinner, currency, dateAr } from '../components/ui'
import { Drawer } from '../components/Modal'
import { clsx } from '../components/clsx'

const STAGE_AR = {
  Placed: 'استُلم الطلب', Confirmed: 'أكّدت الطلب', Preparing: 'بدأ التجهيز',
  ReadyForPickup: 'جاهز للاستلام', CourierAssigned: 'أُسند لمندوب', PickedUp: 'استلمه المندوب',
  OnTheWay: 'في الطريق', Arrived: 'وصل للعميل', Delivered: 'تم التوصيل', Cancelled: 'أُلغي',
}

const TABS = [
  { key: 'incoming', label: 'جديدة', icon: Clock, tone: 'danger' },
  { key: 'preparing', label: 'قيد التجهيز', icon: ChefHat, tone: 'info' },
  { key: 'ready', label: 'جاهزة', icon: PackageCheck, tone: 'brand' },
  { key: 'handedOver', label: 'مع المندوب', icon: Bike, tone: 'ok' },
  { key: 'completed', label: 'مكتملة', icon: CheckCircle2, tone: 'neutral' },
]

export default function Orders() {
  const [tab, setTab] = useState('incoming')
  const [touched, setTouched] = useState(false)
  const [openId, setOpenId] = useState(null)
  const { data: all, isLoading } = useTickets()
  const toast = useToast()

  // on first load, land on the first tab that actually has orders
  if (all && !touched) {
    const counts0 = {}
    for (const t of all) counts0[t.state] = (counts0[t.state] || 0) + 1
    const first = TABS.find((x) => counts0[x.key] > 0)
    if (first && first.key !== tab) setTab(first.key)
    setTouched(true)
  }
  const pick = (k) => { setTouched(true); setTab(k) }

  const act = useApiMutation(
    ({ id, action }) => api.post(`/merchant/tickets/${id}/${action}`),
    ['tickets', 'ticket', 'reports']
  )
  const run = (id, action) =>
    act.mutateAsync({ id, action }).then(() => toast.ok('تم')).catch((e) => toast.error(apiError(e)))

  const counts = {}
  for (const t of all ?? []) counts[t.state] = (counts[t.state] || 0) + 1
  const list = (all ?? []).filter((t) => t.state === tab)

  return (
    <div>
      <PageHeader title="الطلبات" subtitle={all ? `${all.length} طلب` : ''} />

      <div className="mb-5 flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => pick(t.key)}
            className={clsx(
              'flex items-center gap-2 rounded-xl px-3.5 py-2 text-sm font-bold transition',
              tab === t.key ? 'bg-brown text-cream' : 'bg-white text-brown-soft hover:bg-cream'
            )}
          >
            <t.icon className="size-4" />
            {t.label}
            {counts[t.key] > 0 && (
              <span
                className={clsx(
                  'rounded-full px-1.5 text-xs font-black',
                  tab === t.key ? 'bg-cream/25' : 'bg-brand/30 text-brown'
                )}
              >
                {counts[t.key]}
              </span>
            )}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="grid h-40 place-items-center"><Spinner className="size-7" /></div>
      ) : list.length === 0 ? (
        <div className="card grid place-items-center py-16 text-center text-ink-soft">
          لا توجد طلبات في هذه الحالة
        </div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {list.map((t) => (
            <TicketCard key={t.id} t={t} onOpen={() => setOpenId(t.id)} onAct={run} busy={act.isPending} />
          ))}
        </div>
      )}

      {openId && <OrderDrawer id={openId} onClose={() => setOpenId(null)} onAct={run} />}
    </div>
  )
}

function TicketCard({ t, onOpen, onAct, busy }) {
  return (
    <div className="card flex flex-col gap-3 p-4">
      <button onClick={onOpen} className="text-right">
        <div className="flex items-center justify-between">
          <span className="font-extrabold text-espresso" dir="ltr">{t.shortCode}</span>
          <span className="text-xs text-ink-soft">{dateAr(t.placedAt)}</span>
        </div>
        <div className="mt-1 text-sm text-ink-soft">{t.customerNameAr} · {t.itemCount} صنف</div>
        <ul className="mt-2 space-y-0.5 text-sm text-ink">
          {t.items.slice(0, 3).map((i) => (
            <li key={i.id} className="truncate">{i.quantity}× {i.nameAr}</li>
          ))}
          {t.items.length > 3 && <li className="text-xs text-ink-soft">+ {t.items.length - 3} أخرى</li>}
        </ul>
        {t.noteAr && <div className="mt-2 rounded-lg bg-warn/10 px-2 py-1 text-xs text-[#8a5a00]">📝 {t.noteAr}</div>}
      </button>

      <div className="flex items-center justify-between border-t border-line pt-3">
        <span className="font-extrabold text-ink">{currency(t.total)}</span>
        <div className="flex gap-2">
          {t.state === 'incoming' && (
            <>
              <Button size="sm" variant="danger" disabled={busy} onClick={() => onAct(t.id, 'reject')}>رفض</Button>
              <Button size="sm" disabled={busy} onClick={() => onAct(t.id, 'accept')}>قبول</Button>
            </>
          )}
          {t.state === 'preparing' && (
            <Button size="sm" variant="brand" disabled={busy} onClick={() => onAct(t.id, 'advance')}>
              الطلب جاهز
            </Button>
          )}
          {t.state === 'ready' && (
            <Button size="sm" disabled={busy} onClick={() => onAct(t.id, 'advance')}>
              {t.isDelivery ? 'تسليم للمندوب' : 'تم الاستلام'}
            </Button>
          )}
          {(t.state === 'handedOver' || t.state === 'completed') && (
            <Badge tone={t.state === 'completed' ? 'ok' : 'info'}>
              {t.state === 'completed' ? 'مكتمل' : t.courierNameAr ? `مع ${t.courierNameAr}` : 'مع المندوب'}
            </Badge>
          )}
        </div>
      </div>
    </div>
  )
}

function OrderDrawer({ id, onClose, onAct }) {
  const { data: d, isLoading } = useTicketDetail(id)

  return (
    <Drawer open onClose={onClose} title={d ? d.shortCode : 'الطلب'} subtitle={d?.stageAr} size="lg">
      {isLoading || !d ? (
        <div className="py-10 text-center text-ink-soft">جارٍ التحميل…</div>
      ) : (
        <div className="space-y-5">
          <div className="card p-4 text-sm">
            <h3 className="mb-2 font-bold text-ink">العميل</h3>
            <Row label="الاسم" value={d.customerNameAr} />
            <Row label="الجوال" value={<a href={`tel:${d.customerPhone}`} dir="ltr" className="text-info">{d.customerPhone}</a>} />
            <Row label="نوع الطلب" value={d.isDelivery ? 'توصيل عبر مندوب كونو' : 'استلام من الفرع'} />
            <div className="mt-2 flex items-start gap-2 rounded-lg bg-canvas p-2.5">
              <MapPin className="mt-0.5 size-4 shrink-0 text-caramel" />
              <div>
                <div className="font-semibold text-ink">{d.addressLabelAr}</div>
                <div className="text-xs text-ink-soft">{d.addressLineAr}</div>
                <a
                  className="mt-1 inline-block text-xs font-bold text-info"
                  href={`https://maps.google.com/?q=${d.addressLat},${d.addressLng}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  فتح على الخريطة
                </a>
              </div>
            </div>
          </div>

          {d.courier && (
            <div className="card p-4 text-sm">
              <h3 className="mb-2 font-bold text-ink">المندوب</h3>
              <Row label="الاسم" value={`${d.courier.name} · ⭐ ${d.courier.rating.toFixed(1)}`} />
              <Row label="المركبة" value={`${d.courier.vehicleAr} · ${d.courier.plateAr}`} />
            </div>
          )}

          <div>
            <h3 className="mb-2 font-bold text-ink">الأصناف ({d.itemCount})</h3>
            <ul className="divide-y divide-line rounded-xl border border-line">
              {d.items.map((i) => (
                <li key={i.id} className="flex items-start gap-3 p-3">
                  <img src={i.imageUrl} alt="" className="size-10 rounded-lg object-cover" />
                  <div className="flex-1">
                    <div className="text-sm font-semibold text-ink">{i.quantity}× {i.nameAr}</div>
                    {i.selectedOptionsAr && <div className="text-xs text-ink-soft">{i.selectedOptionsAr}</div>}
                    {i.noteAr && <div className="text-xs text-caramel">ملاحظة: {i.noteAr}</div>}
                  </div>
                  <span className="text-sm font-bold">{currency(i.lineTotal)}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="card space-y-1.5 p-4 text-sm">
            <Row label="المجموع الفرعي" value={currency(d.subtotal)} />
            <Row label="التوصيل" value={currency(d.deliveryFee)} />
            <Row label="الخدمة" value={currency(d.serviceFee)} />
            {d.discount > 0 && <Row label="الخصم" value={`- ${currency(d.discount)}`} />}
            {d.tip > 0 && <Row label="بقشيش" value={currency(d.tip)} />}
            <div className="mt-2 flex justify-between border-t border-line pt-2 text-base font-extrabold">
              <span>الإجمالي</span>
              <span>{currency(d.total)}</span>
            </div>
            <Row label="طريقة الدفع" value={d.paymentLabelAr} />
          </div>

          <div>
            <h3 className="mb-2 font-bold text-ink">سجل الطلب</h3>
            <ol className="space-y-2">
              {d.events.map((e, i) => (
                <li key={i} className="flex items-center gap-3 text-sm">
                  <span className="size-2 rounded-full bg-brown" />
                  <span className="font-semibold text-ink">{STAGE_AR[e.stage] ?? e.stage}</span>
                  <span className="text-xs text-ink-soft">{dateAr(e.at)}</span>
                </li>
              ))}
            </ol>
          </div>

          {['Placed', 'Confirmed', 'Preparing', 'ReadyForPickup'].includes(d.stage) && (
            <div className="flex gap-2">
              {d.stage === 'Placed' && (
                <>
                  <Button variant="danger" onClick={() => { onAct(d.id, 'reject'); onClose() }}>رفض الطلب</Button>
                  <Button onClick={() => onAct(d.id, 'accept')}>قبول الطلب</Button>
                </>
              )}
              {d.stage !== 'Placed' && <Button onClick={() => onAct(d.id, 'advance')}>المرحلة التالية</Button>}
            </div>
          )}
        </div>
      )}
    </Drawer>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between py-0.5">
      <span className="text-ink-soft">{label}</span>
      <span className="font-semibold text-ink">{value}</span>
    </div>
  )
}
