import {
  Banknote,
  ShoppingBag,
  Store,
  Bike,
  Users,
  TrendingUp,
  Activity,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-react'
import { useStats } from '../lib/hooks'
import { Card, PageHeader, Spinner, currency } from '../components/ui'
import { clsx } from '../components/clsx'
import { Chart, Sparkline, KONO, VERTICAL_PALETTE } from '../components/Chart'

const STAGE_AR = {
  Placed: 'مستلَم',
  Confirmed: 'مؤكَّد',
  Preparing: 'قيد التجهيز',
  ReadyForPickup: 'جاهز',
  CourierAssigned: 'أُسند لمندوب',
  PickedUp: 'استلمه المندوب',
  OnTheWay: 'في الطريق',
  Arrived: 'وصل',
  Delivered: 'تم التوصيل',
  Cancelled: 'ملغى',
}

const VERTICAL_AR = {
  Restaurants: 'مطاعم',
  Groceries: 'بقالة',
  Pharmacy: 'صيدليات',
  Sweets: 'حلويات',
  Flowers: 'ورود',
  Water: 'مياه',
}

const DAY_AR = { Sun: 'الأحد', Mon: 'الإثنين', Tue: 'الثلاثاء', Wed: 'الأربعاء', Thu: 'الخميس', Fri: 'الجمعة', Sat: 'السبت' }

export default function Dashboard() {
  const { data, isLoading } = useStats()

  if (isLoading || !data)
    return (
      <div className="grid h-64 place-items-center">
        <Spinner className="size-8" />
      </div>
    )

  const days = data.revenueByDay.map((p) => DAY_AR[p.day] ?? p.day)
  const revSeries = data.revenueByDay.map((p) => p.revenue)
  const orderSeries = data.revenueByDay.map((p) => p.orders)

  // day-over-day deltas
  const revYesterday = revSeries[revSeries.length - 2] ?? 0
  const revDelta = revYesterday ? Math.round(((data.revenueToday - revYesterday) / revYesterday) * 100) : null
  const ordYesterday = orderSeries[orderSeries.length - 2] ?? 0
  const ordDelta = ordYesterday ? Math.round(((data.ordersToday - ordYesterday) / ordYesterday) * 100) : null

  const vert = data.salesByVertical ?? []
  const stageTotal = data.ordersByStage.reduce((a, b) => a + b.count, 0) || 1

  return (
    <div>
      <PageHeader title="لوحة التحكم" subtitle="نظرة عامة على أداء المنصّة اليوم" />

      {/* ── KPI cards ─────────────────────────────────────────── */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi
          icon={Banknote}
          tone="ok"
          label="إيرادات اليوم"
          value={currency(data.revenueToday)}
          delta={revDelta}
          spark={revSeries}
          sparkColor={KONO.ok}
        />
        <Kpi
          icon={ShoppingBag}
          tone="brand"
          label="طلبات اليوم"
          value={data.ordersToday}
          delta={ordDelta}
          spark={orderSeries}
          sparkColor={KONO.brown}
        />
        <Kpi
          icon={TrendingUp}
          tone="caramel"
          label="متوسط قيمة الطلب"
          value={currency(data.avgOrderValue)}
          hint={`إيراد ٧ أيام: ${currency(data.revenue7d)}`}
        />
        <Kpi
          icon={Bike}
          tone="info"
          label="مناديب متصلون"
          value={`${data.couriersOnline} / ${data.couriersTotal}`}
          hint={`${data.ordersActive} طلب نشط الآن`}
        />
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-3">
        <MiniStat icon={Store} label="متاجر نشطة" value={data.storesActive} />
        <MiniStat icon={Users} label="إجمالي العملاء" value={data.customersTotal} />
        <MiniStat icon={Activity} label="إجمالي الطلبات" value={data.ordersTotal} />
      </div>

      {/* ── revenue + vertical mix ────────────────────────────── */}
      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <div className="mb-2 flex items-baseline justify-between">
            <h2 className="font-extrabold text-espresso">الإيراد اليومي</h2>
            <span className="text-sm text-ink-soft">آخر ٧ أيام</span>
          </div>
          <Chart
            type="area"
            height={300}
            series={[
              { name: 'الإيراد', type: 'area', data: revSeries },
              { name: 'الطلبات', type: 'line', data: orderSeries },
            ]}
            options={{
              colors: [KONO.brandDeep, KONO.brown],
              fill: {
                type: ['gradient', 'solid'],
                gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 90] },
              },
              stroke: { width: [3, 2.5], curve: 'smooth', dashArray: [0, 4] },
              xaxis: {
                categories: days,
                labels: { style: { colors: KONO.inkSoft, fontFamily: 'Cairo, sans-serif' } },
                axisBorder: { show: false },
                axisTicks: { show: false },
              },
              yaxis: [
                { labels: { style: { colors: KONO.inkSoft }, formatter: (v) => `${Math.round(v)}` } },
                { opposite: true, labels: { style: { colors: KONO.inkSoft }, formatter: (v) => `${Math.round(v)}` } },
              ],
              tooltip: {
                y: { formatter: (v, { seriesIndex }) => (seriesIndex === 0 ? currency(v) : `${v} طلب`) },
              },
              markers: { size: 0, hover: { size: 5 } },
            }}
          />
        </Card>

        <Card>
          <h2 className="mb-2 font-extrabold text-espresso">المبيعات حسب القسم</h2>
          {vert.length ? (
            <Chart
              type="donut"
              height={300}
              series={vert.map((v) => Number(v.revenue))}
              options={{
                labels: vert.map((v) => VERTICAL_AR[v.kind] ?? v.kind),
                colors: VERTICAL_PALETTE,
                stroke: { width: 2, colors: ['#fff'] },
                legend: { position: 'bottom' },
                plotOptions: {
                  pie: {
                    donut: {
                      size: '68%',
                      labels: {
                        show: true,
                        total: {
                          show: true,
                          label: 'الإجمالي',
                          fontFamily: 'Cairo, sans-serif',
                          color: KONO.inkSoft,
                          formatter: () => currency(vert.reduce((a, b) => a + Number(b.revenue), 0)),
                        },
                      },
                    },
                  },
                },
                tooltip: { y: { formatter: (v) => currency(v) } },
                dataLabels: { enabled: true, formatter: (p) => `${Math.round(p)}%` },
              }}
            />
          ) : (
            <p className="py-16 text-center text-sm text-ink-soft">لا توجد مبيعات بعد</p>
          )}
        </Card>
      </div>

      {/* ── orders/day + stages ───────────────────────────────── */}
      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="mb-2 font-extrabold text-espresso">عدد الطلبات يومياً</h2>
          <Chart
            type="bar"
            height={280}
            series={[{ name: 'الطلبات', data: orderSeries }]}
            options={{
              colors: [KONO.brand],
              plotOptions: { bar: { borderRadius: 6, columnWidth: '45%', distributed: false } },
              xaxis: {
                categories: days,
                labels: { style: { colors: KONO.inkSoft, fontFamily: 'Cairo, sans-serif' } },
                axisBorder: { show: false },
                axisTicks: { show: false },
              },
              yaxis: { labels: { style: { colors: KONO.inkSoft } } },
              tooltip: { y: { formatter: (v) => `${v} طلب` } },
            }}
          />
        </Card>

        <Card>
          <h2 className="mb-1 font-extrabold text-espresso">الطلبات حسب الحالة</h2>
          {(() => {
            const delivered = data.ordersByStage.find((s) => s.label === 'Delivered')?.count ?? 0
            const pct = Math.round((delivered / stageTotal) * 100)
            return (
              <Chart
                type="radialBar"
                height={200}
                series={[pct]}
                options={{
                  colors: [KONO.ok],
                  labels: ['مكتملة'],
                  plotOptions: {
                    radialBar: {
                      hollow: { size: '58%' },
                      dataLabels: {
                        name: { fontFamily: 'Cairo, sans-serif', color: KONO.inkSoft, fontSize: '13px' },
                        value: { fontFamily: 'Cairo, sans-serif', color: KONO.ink, fontSize: '22px', fontWeight: 700 },
                      },
                      track: { background: KONO.line },
                    },
                  },
                }}
              />
            )
          })()}
          <div className="space-y-3">
            {data.ordersByStage.map((s) => (
              <div key={s.label}>
                <div className="flex justify-between text-sm">
                  <span className="font-semibold text-ink">{STAGE_AR[s.label] ?? s.label}</span>
                  <span className="text-ink-soft">{s.count}</span>
                </div>
                <div className="mt-1 h-2 overflow-hidden rounded-full bg-cream">
                  <div
                    className="h-full rounded-full bg-caramel transition-all"
                    style={{ width: `${(s.count / stageTotal) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* ── top lists ─────────────────────────────────────────── */}
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-3 font-extrabold text-espresso">الأصناف الأكثر مبيعاً</h2>
          <ul className="divide-y divide-line">
            {data.topProducts.map((p, i) => (
              <li key={p.id} className="flex items-center gap-3 py-2.5">
                <span className="w-5 text-center font-extrabold text-brown">{i + 1}</span>
                <img src={p.imageUrl} alt="" className="size-9 rounded-lg object-cover" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-semibold text-ink">{p.nameAr}</div>
                  <div className="text-xs text-ink-soft">{p.storeName}</div>
                </div>
                <div className="text-left">
                  <div className="text-sm font-bold text-ink">{p.sold} مبيع</div>
                  <div className="text-xs text-ink-soft">{currency(p.revenue)}</div>
                </div>
              </li>
            ))}
          </ul>
        </Card>

        <Card>
          <h2 className="mb-3 font-extrabold text-espresso">أفضل المتاجر</h2>
          <ul className="divide-y divide-line">
            {data.topStores.map((s, i) => (
              <li key={s.id} className="flex items-center gap-3 py-2.5">
                <span className="w-5 text-center font-extrabold text-brown">{i + 1}</span>
                <img src={s.imageUrl} alt="" className="size-9 rounded-lg object-cover" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-semibold text-ink">{s.name}</div>
                  <div className="text-xs text-ink-soft">
                    ⭐ {s.rating.toFixed(1)} · {s.orders} طلب
                  </div>
                </div>
                <div className="text-sm font-bold text-ink">{currency(s.revenue)}</div>
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  )
}

const TONES = {
  brown: 'bg-brown/10 text-brown',
  brand: 'bg-brand/25 text-brown',
  ok: 'bg-ok/12 text-ok',
  caramel: 'bg-caramel/15 text-caramel',
  info: 'bg-info/12 text-info',
}

function Kpi({ icon: Icon, label, value, hint, delta, spark, sparkColor, tone = 'brown' }) {
  return (
    <div className="card flex h-[168px] flex-col overflow-hidden p-5">
      <div className="flex items-start justify-between">
        <div className={clsx('grid size-11 place-items-center rounded-xl', TONES[tone])}>
          <Icon className="size-5" strokeWidth={2.3} />
        </div>
        {delta != null && (
          <span
            className={clsx(
              'inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-xs font-bold',
              delta >= 0 ? 'bg-ok/12 text-ok' : 'bg-danger/12 text-danger'
            )}
          >
            {delta >= 0 ? <ArrowUpRight className="size-3.5" /> : <ArrowDownRight className="size-3.5" />}
            {Math.abs(delta)}٪
          </span>
        )}
      </div>
      <div className="mt-3 text-2xl font-extrabold text-espresso">{value}</div>
      <div className="text-xs font-semibold text-ink-soft">{label}</div>
      <div className="mt-auto">
        {hint && <div className="text-[11px] text-ink-soft/80">{hint}</div>}
        {spark && (
          <div className="-mx-2 -mb-3 h-11">
            <Sparkline data={spark} color={sparkColor} height={44} />
          </div>
        )}
      </div>
    </div>
  )
}

function MiniStat({ icon: Icon, label, value }) {
  return (
    <Card className="flex items-center gap-3 py-4">
      <div className="grid size-10 place-items-center rounded-xl bg-brown/8 text-brown-soft">
        <Icon className="size-[18px]" strokeWidth={2.3} />
      </div>
      <div>
        <div className="text-lg font-extrabold text-espresso">{value}</div>
        <div className="text-xs font-semibold text-ink-soft">{label}</div>
      </div>
    </Card>
  )
}
