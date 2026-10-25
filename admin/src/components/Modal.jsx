import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { clsx } from './clsx'

const WIDTHS = {
  sm: 'max-w-md',
  md: 'max-w-xl',
  lg: 'max-w-2xl',
  xl: 'max-w-3xl',
}

/**
 * Side drawer. Every editor / detail panel in the dashboard uses this — it slides
 * in from the leading edge opposite the nav rail, never a centred pop-up.
 * Parents mount it conditionally (`{open && <Drawer .../>}`) so it animates in on
 * mount and unmounts instantly on close.
 */
export function Drawer({ open = true, onClose, title, subtitle, children, footer, size = 'md' }) {
  const [shown, setShown] = useState(false)

  useEffect(() => {
    const id = requestAnimationFrame(() => setShown(true))
    const onKey = (e) => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      cancelAnimationFrame(id)
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [onClose])

  const visible = open && shown

  return (
    <div className="fixed inset-0 z-50">
      <div
        className={clsx(
          'absolute inset-0 bg-espresso/45 backdrop-blur-[2px] transition-opacity duration-300',
          visible ? 'opacity-100' : 'opacity-0'
        )}
        onClick={onClose}
      />
      <div
        className={clsx(
          'absolute inset-y-0 left-0 flex w-full flex-col bg-canvas shadow-2xl transition-transform duration-300 ease-out',
          WIDTHS[size] ?? WIDTHS.md,
          visible ? 'translate-x-0' : '-translate-x-full'
        )}
        role="dialog"
        aria-modal="true"
      >
        <div className="flex items-start justify-between gap-3 border-b border-line bg-white/60 px-5 py-4">
          <div className="min-w-0">
            <h2 className="truncate text-lg font-extrabold text-espresso">{title}</h2>
            {subtitle && <p className="mt-0.5 truncate text-sm text-ink-soft">{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            className="-me-1 shrink-0 rounded-lg p-1.5 text-ink-soft transition hover:bg-cream hover:text-brown"
            aria-label="إغلاق"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-5">{children}</div>

        {footer && (
          <div className="flex justify-end gap-2 border-t border-line bg-white/60 px-5 py-4">{footer}</div>
        )}
      </div>
    </div>
  )
}

/** Back-compat alias — the dashboard has no centred modals, everything is a drawer. */
export const Modal = Drawer
