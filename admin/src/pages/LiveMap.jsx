import { useEffect, useMemo, useState } from 'react'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet'
import { RefreshCw, Package, Bike, MapPin, Store as StoreIcon } from 'lucide-react'
import { useLiveBoard } from '../lib/hooks'
import { PageHeader, Badge, Spinner, currency } from '../components/ui'
import { clsx } from '../components/clsx'

const pin = (bg, glyph) =>
  L.divIcon({
    className: 'kono-pin',
    html: `<div style="background:${bg};width:30px;height:30px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);
      display:grid;place-items:center;box-shadow:0 2px 6px rgba(0,0,0,.35);border:2px solid #fff">
      <span style="transform:rotate(45deg);font-size:15px;line-height:1">${glyph}</span></div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 30],
    popupAnchor: [0, -28],
  })

const ICONS = {
  store: pin('#fdd003', '🏪'),
  drop: pin('#c8811e', '📍'),
  courier: pin('#582808', '🛵'),
}

const STAGE_TONE = {
  'بانتظار التأكيد': 'warn',
  'مؤكَّد': 'info',
  'قيد التجهيز': 'info',
  'جاهز للاستلام': 'brand',
  'أُسند لمندوب': 'brand',
  'استلمه المندوب': 'ok',
  'في الطريق': 'ok',
  'وصل للعميل': 'ok',
}

function FitBounds({ points }) {
  const map = useMap()
  useEffect(() => {
    if (!points.length) return
    const b = L.latLngBounds(points)
    map.fitBounds(b, { padding: [60, 60], maxZoom: 15 })
  }, [points, map])
  return null
}

export default function LiveMap() {
  const { data, isLoading, isFetching, dataUpdatedAt } = useLiveBoard()
  const [selected, setSelected] = useState(null)

  const orders = data?.orders ?? []
  const active = useMemo(() => orders.find((o) => o.id === selected) ?? null, [orders, selected])

  const focusPoints = useMemo(() => {
    if (!active) return []
    const p = [[active.storeLat, active.storeLng], [active.dropoffLat, active.dropoffLng]]
    if (active.courierLat) p.push([active.courierLat, active.courierLng])
    return p.filter(([a, b]) => a && b)
  }, [active])

  const routeLine = useMemo(() => {
    if (!active) return []
    const pts = [[active.storeLat, active.storeLng]]
    if (active.courierLat) pts.push([active.courierLat, active.courierLng])
    pts.push([active.dropoffLat, active.dropoffLng])
    return pts.filter(([a, b]) => a && b)
  }, [active])

  const center = data ? [data.centerLat, data.centerLng] : [24.7136, 46.6753]

  return (
    <div className="flex h-[calc(100vh-7rem)] flex-col">
      <PageHeader
        title="الخريطة الحيّة"
        subtitle="مواقع المتاجر والمناديب والطلبات الجارية"
        action={
          <div className="flex items-center gap-2 text-xs text-ink-soft">
            <RefreshCw className={clsx('size-3.5', isFetching && 'animate-spin')} />
            يتحدّث تلقائياً كل ١٠ ثوانٍ
          </div>
        }
      />

      <div className="grid flex-1 gap-4 overflow-hidden lg:grid-cols-[340px_1fr]">
        {/* order list */}
        <div className="flex flex-col overflow-hidden rounded-xl border border-line bg-white">
          <div className="grid grid-cols-2 gap-2 border-b border-line p-3">
            <Metric icon={Package} label="طلبات جارية" value={data?.activeCount ?? 0} tone="bg-brown/8 text-brown" />
            <Metric icon={Bike} label="بلا مندوب" value={data?.unassignedCount ?? 0} tone="bg-danger/10 text-danger" />
          </div>
          <div className="flex-1 overflow-y-auto">
            {isLoading ? (
              <div className="grid h-40 place-items-center"><Spinner className="size-6" /></div>
            ) : orders.length === 0 ? (
              <p className="p-6 text-center text-sm text-ink-soft">لا توجد طلبات جارية الآن</p>
            ) : (
              orders.map((o) => (
                <button
                  key={o.id}
                  onClick={() => setSelected(o.id)}
                  className={clsx(
                    'block w-full border-b border-line px-4 py-3 text-right transition hover:bg-cream',
                    selected === o.id && 'bg-brand/10'
                  )}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-extrabold text-espresso" dir="ltr">{o.shortCode}</span>
                    <Badge tone={STAGE_TONE[o.stageAr] ?? 'neutral'}>{o.stageAr}</Badge>
                  </div>
                  <div className="mt-1 flex items-center gap-1.5 text-xs text-ink-soft">
                    <StoreIcon className="size-3.5" /> {o.storeNameAr}
                  </div>
                  <div className="mt-0.5 flex items-center gap-1.5 text-xs text-ink-soft">
                    <MapPin className="size-3.5" /> {o.dropoffAddressAr}
                  </div>
                  <div className="mt-1.5 flex items-center justify-between text-xs">
                    <span className="text-ink-soft">
                      {o.courierNameAr ? `🛵 ${o.courierNameAr}` : 'لم يُسند بعد'}
                    </span>
                    <span className="font-bold text-ink">{currency(o.total)}</span>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        {/* map */}
        <div className="overflow-hidden rounded-xl border border-line">
          <MapContainer center={center} zoom={12} className="size-full" style={{ minHeight: 400 }}>
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; OpenStreetMap'
            />
            {focusPoints.length > 0 && <FitBounds points={focusPoints} />}

            {orders.map((o) => (
              <Marker
                key={`s${o.id}`}
                position={[o.storeLat, o.storeLng]}
                icon={ICONS.store}
                eventHandlers={{ click: () => setSelected(o.id) }}
              >
                <Popup>
                  <b>{o.storeNameAr}</b>
                  <br />
                  {o.shortCode} · {o.stageAr}
                </Popup>
              </Marker>
            ))}

            {orders
              .filter((o) => o.dropoffLat)
              .map((o) => (
                <Marker key={`d${o.id}`} position={[o.dropoffLat, o.dropoffLng]} icon={ICONS.drop}
                  eventHandlers={{ click: () => setSelected(o.id) }}>
                  <Popup>
                    عنوان التوصيل — {o.customerNameAr}
                    <br />
                    {o.dropoffAddressAr}
                  </Popup>
                </Marker>
              ))}

            {orders
              .filter((o) => o.courierLat)
              .map((o) => (
                <Marker key={`c${o.id}`} position={[o.courierLat, o.courierLng]} icon={ICONS.courier}
                  eventHandlers={{ click: () => setSelected(o.id) }}>
                  <Popup>
                    🛵 {o.courierNameAr}
                    <br />
                    {o.shortCode}
                  </Popup>
                </Marker>
              ))}

            {routeLine.length >= 2 && (
              <Polyline positions={routeLine} pathOptions={{ color: '#582808', weight: 3, dashArray: '6 8' }} />
            )}
          </MapContainer>
        </div>
      </div>
    </div>
  )
}

function Metric({ icon: Icon, label, value, tone }) {
  return (
    <div className="flex items-center gap-2.5 rounded-lg bg-canvas px-3 py-2">
      <div className={clsx('grid size-8 place-items-center rounded-lg', tone)}>
        <Icon className="size-4" strokeWidth={2.4} />
      </div>
      <div>
        <div className="text-lg font-extrabold leading-none text-espresso">{value}</div>
        <div className="mt-0.5 text-[11px] font-semibold text-ink-soft">{label}</div>
      </div>
    </div>
  )
}
