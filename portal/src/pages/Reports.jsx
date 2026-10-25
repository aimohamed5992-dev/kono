import { Banknote, ShoppingBag, TrendingUp, Star } from 'lucide-react'
import { useReports } from '../lib/hooks'
import { Card, PageHeader, Spinner, currency } from '../components/ui'
import { Chart, KONO } from '../components/Chart'
import { clsx } from '../components/clsx'

export default function Reports() {
  const { data, isLoading } = useReports()
  if (isLoading || !data)
    return <div className="grid h-64 place-items-center"><Spinner className="size-8" /></div>

  const days = data.days.map((d) => d.weekdayAr)
  const amounts = data.days.map((d) => Number(d.amount))
  const orders = data.days.map((d) => d.trips)

  return (
    <div>
      <PageHeader title="التقارير" subtitle="أداء متجرك خلال آخر ٧ أيام" />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi icon={Banknote} tone="ok" label="إجمالي المبيعات" value={currency(data.totalSales)} />
        <Kpi icon={ShoppingBag} tone="brand" label="عدد الطلبات" value={data.orderCount} />
        <Kpi icon={TrendingUp} tone="caramel" label="متوسط الطلب" value={currency(data.avgOrderValue)} />
        <Kpi icon={Star} tone="info" label="التقييم" value={data.rating.toFixed(1)} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="mb-2 font-extrabold text-espresso">المبيعات اليومية</h2>
          <Chart
            type="area"
            height={300}
            series={[{ name: 'المبيعات', data: amounts }]}
            options={{
              colors: [KONO.brandDeep],
              fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 90] } },
              xaxis: {
                categories: days,
                labels: { style: { colors: KONO.inkSoft, fontFamily: 'Cairo, sans-serif' } },
                axisBorder: { show: false }, axisTicks: { show: false },
              },
              yaxis: { labels: { style: { colors: KONO.inkSoft }, formatter: (v) => Math.round(v) } },
              tooltip: { y: { formatter: (v) => currency(v) } },
              markers: { size: 0, hover: { size: 5 } },
            }}
          />
        </Card>

        <Card>
          <h2 className="mb-2 font-extrabold text-espresso">الطلبات يومياً</h2>
          <Chart
            type="bar"
            height={300}
            series={[{ name: 'الطلبات', data: orders }]}
            options={{
              colors: [KONO.brand],
              plotOptions: { bar: { borderRadius: 6, columnWidth: '50%' } },
              xaxis: {
                categories: days,
                labels: { style: { colors: KONO.inkSoft, fontFamily: 'Cairo, sans-serif' } },
                axisBorder: { show: false }, axisTicks: { show: false },
              },
              yaxis: { labels: { style: { colors: KONO.inkSoft } } },
            }}
          />
        </Card>
      </div>

      <Card className="mt-6">
        <h2 className="mb-3 font-extrabold text-espresso">الأصناف الأكثر مبيعاً</h2>
        {data.topItems.length === 0 ? (
          <p className="py-6 text-center text-sm text-ink-soft">لا توجد مبيعات بعد</p>
        ) : (
          <ul className="divide-y divide-line">
            {data.topItems.map((it, i) => (
              <li key={i} className="flex items-center gap-3 py-2.5">
                <span className="w-5 text-center font-extrabold text-brown">{i + 1}</span>
                <span className="flex-1 font-semibold text-ink">{it.nameAr}</span>
                <span className="text-sm text-ink-soft">{it.count} مبيع</span>
                <span className="w-24 text-left text-sm font-bold text-ink">{currency(it.revenue)}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}

const TONES = {
  ok: 'bg-ok/12 text-ok',
  brand: 'bg-brand/25 text-brown',
  caramel: 'bg-caramel/15 text-caramel',
  info: 'bg-info/12 text-info',
}
function Kpi({ icon: Icon, label, value, tone }) {
  return (
    <div className="card p-5">
      <div className={clsx('grid size-11 place-items-center rounded-xl', TONES[tone])}>
        <Icon className="size-5" strokeWidth={2.3} />
      </div>
      <div className="mt-3 text-2xl font-extrabold text-espresso">{value}</div>
      <div className="text-xs font-semibold text-ink-soft">{label}</div>
    </div>
  )
}
