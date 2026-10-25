import { ChevronLeft, ChevronRight } from 'lucide-react'
import { clsx } from './clsx'
import { Spinner } from './ui'

export function DataTable({ columns, rows, loading, onRowClick, empty = 'لا توجد بيانات', keyField = 'id' }) {
  return (
    <div className="card overflow-hidden p-0">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line bg-cream/40 text-right">
              {columns.map((c) => (
                <th
                  key={c.key}
                  className={clsx('px-4 py-3 font-bold text-brown-soft', c.className)}
                  style={c.width ? { width: c.width } : undefined}
                >
                  {c.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={columns.length} className="py-14">
                  <div className="flex justify-center">
                    <Spinner className="size-6" />
                  </div>
                </td>
              </tr>
            )}
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={columns.length} className="py-14 text-center text-ink-soft">
                  {empty}
                </td>
              </tr>
            )}
            {!loading &&
              rows.map((row) => (
                <tr
                  key={row[keyField]}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  className={clsx(
                    'border-b border-line/70 transition last:border-0',
                    onRowClick && 'cursor-pointer hover:bg-cream/50'
                  )}
                >
                  {columns.map((c) => (
                    <td key={c.key} className={clsx('px-4 py-3', c.cellClassName)}>
                      {c.render ? c.render(row) : row[c.key]}
                    </td>
                  ))}
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-4 flex items-center justify-center gap-2">
      <button
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
        className="grid size-9 place-items-center rounded-lg border border-line bg-white disabled:opacity-40"
      >
        <ChevronRight className="size-4" />
      </button>
      <span className="text-sm font-semibold text-ink-soft">
        صفحة {page} من {totalPages}
      </span>
      <button
        disabled={page >= totalPages}
        onClick={() => onChange(page + 1)}
        className="grid size-9 place-items-center rounded-lg border border-line bg-white disabled:opacity-40"
      >
        <ChevronLeft className="size-4" />
      </button>
    </div>
  )
}
