import ReactApexChart from 'react-apexcharts'

/** Kono brand palette for charts. */
export const KONO = {
  brand: '#fdd003',
  brandDeep: '#f3b90a',
  brown: '#582808',
  brownSoft: '#7a4a1e',
  caramel: '#c8811e',
  ink: '#2e1703',
  inkSoft: '#8a6b4a',
  line: '#efe6d4',
  ok: '#2e9e5b',
  info: '#2f7dbe',
  danger: '#d1493b',
}

export const VERTICAL_PALETTE = [
  KONO.brand,
  KONO.brown,
  KONO.caramel,
  KONO.ok,
  KONO.info,
  KONO.brownSoft,
]

const BASE = {
  chart: {
    fontFamily: 'Cairo, sans-serif',
    toolbar: { show: false },
    zoom: { enabled: false },
    animations: { easing: 'easeinout', speed: 500 },
  },
  grid: { borderColor: KONO.line, strokeDashArray: 4, padding: { left: 6, right: 6 } },
  dataLabels: { enabled: false },
  tooltip: { style: { fontFamily: 'Cairo, sans-serif', fontSize: '13px' } },
  legend: { fontFamily: 'Cairo, sans-serif', labels: { colors: KONO.inkSoft } },
  stroke: { curve: 'smooth', width: 3 },
}

function deepMerge(a, b) {
  const out = { ...a }
  for (const k of Object.keys(b || {})) {
    out[k] = b[k] && typeof b[k] === 'object' && !Array.isArray(b[k]) ? deepMerge(a[k] || {}, b[k]) : b[k]
  }
  return out
}

/** ApexCharts with Kono defaults baked in. */
export function Chart({ type = 'area', series, options = {}, height = 300 }) {
  return (
    <ReactApexChart type={type} series={series} options={deepMerge(BASE, options)} height={height} />
  )
}

/** Tiny inline sparkline for KPI cards. */
export function Sparkline({ data, color = KONO.brown, height = 44 }) {
  return (
    <ReactApexChart
      type="area"
      height={height}
      series={[{ data }]}
      options={{
        chart: { sparkline: { enabled: true }, fontFamily: 'Cairo, sans-serif' },
        stroke: { curve: 'smooth', width: 2.5 },
        fill: { type: 'gradient', gradient: { opacityFrom: 0.35, opacityTo: 0, stops: [0, 100] } },
        colors: [color],
        tooltip: { enabled: false },
      }}
    />
  )
}
