import { clsx } from './clsx'

export function Button({ variant = 'primary', size = 'md', className, ...props }) {
  const base =
    'inline-flex items-center justify-center gap-2 font-semibold rounded-xl transition active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none whitespace-nowrap'
  const variants = {
    primary: 'bg-brown text-cream hover:bg-espresso',
    brand: 'bg-brand text-brown hover:bg-brand-deep',
    ghost: 'text-brown hover:bg-brown/8',
    outline: 'border border-line text-ink hover:bg-cream',
    danger: 'bg-danger/12 text-danger hover:bg-danger/20',
  }
  const sizes = { sm: 'h-8 px-3 text-sm', md: 'h-10 px-4 text-sm', lg: 'h-12 px-5' }
  return <button className={clsx(base, variants[variant], sizes[size], className)} {...props} />
}

export function Card({ className, children }) {
  return <div className={clsx('card p-5', className)}>{children}</div>
}

export function Badge({ tone = 'neutral', children }) {
  const tones = {
    neutral: 'bg-brown/8 text-brown-soft',
    ok: 'bg-ok/12 text-ok',
    warn: 'bg-warn/15 text-[#8a5a00]',
    danger: 'bg-danger/12 text-danger',
    info: 'bg-info/12 text-info',
    brand: 'bg-brand/25 text-brown',
  }
  return (
    <span className={clsx('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold', tones[tone])}>
      {children}
    </span>
  )
}

export function Input({ className, label, ...props }) {
  return (
    <label className="block">
      {label && <span className="mb-1.5 block text-sm font-semibold text-ink-soft">{label}</span>}
      <input
        className={clsx(
          'h-11 w-full rounded-xl border border-line bg-white px-3.5 text-sm outline-none',
          'focus:border-brown focus:ring-2 focus:ring-brand/30',
          className
        )}
        {...props}
      />
    </label>
  )
}

export function Select({ className, label, children, ...props }) {
  return (
    <label className="block">
      {label && <span className="mb-1.5 block text-sm font-semibold text-ink-soft">{label}</span>}
      <select
        className={clsx(
          'h-11 w-full rounded-xl border border-line bg-white px-3 text-sm outline-none focus:border-brown',
          className
        )}
        {...props}
      >
        {children}
      </select>
    </label>
  )
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-extrabold text-espresso">{title}</h1>
        {subtitle && <p className="mt-0.5 text-sm text-ink-soft">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Spinner({ className }) {
  return (
    <div
      className={clsx(
        'size-5 animate-spin rounded-full border-2 border-brown/25 border-t-brown',
        className
      )}
    />
  )
}

export function EmptyState({ icon: Icon, title, message }) {
  return (
    <div className="flex flex-col items-center gap-2 py-16 text-center">
      {Icon && <Icon className="size-10 text-caramel/50" strokeWidth={1.5} />}
      <p className="font-bold text-ink">{title}</p>
      {message && <p className="text-sm text-ink-soft">{message}</p>}
    </div>
  )
}

export function currency(n) {
  return `${Number(n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ر.س`
}

export function dateAr(d) {
  return new Date(d).toLocaleString('ar-SA', { dateStyle: 'medium', timeStyle: 'short' })
}
