import { useState } from 'react'
import { Search, Crown } from 'lucide-react'
import { useCustomers, useApiMutation } from '../lib/hooks'
import { api } from '../lib/api'
import { useToast } from '../lib/toast'
import { PageHeader, Badge, Button, Input, currency, dateAr } from '../components/ui'
import { DataTable, Pagination } from '../components/table'

export default function Customers() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const { data, isLoading } = useCustomers({ search, page, pageSize: 15 })
  const toast = useToast()
  const setActive = useApiMutation(
    ({ id, isActive }) => api.patch(`/admin/users/${id}/status`, { isActive }),
    ['customers']
  )

  const columns = [
    {
      key: 'nameAr',
      header: 'العميل',
      render: (r) => (
        <div className="flex items-center gap-2">
          <span className="font-bold text-ink">{r.nameAr}</span>
          {r.konoPlus && <Crown className="size-3.5 text-caramel" />}
        </div>
      ),
    },
    { key: 'phone', header: 'الجوال', render: (r) => <span dir="ltr">{r.phone}</span> },
    { key: 'email', header: 'البريد', render: (r) => r.email || '—' },
    { key: 'walletBalance', header: 'المحفظة', render: (r) => currency(r.walletBalance) },
    { key: 'points', header: 'النقاط' },
    { key: 'orderCount', header: 'الطلبات' },
    { key: 'lifetimeSpend', header: 'إجمالي الإنفاق', render: (r) => currency(r.lifetimeSpend) },
    { key: 'createdAt', header: 'مسجّل منذ', render: (r) => <span className="text-xs text-ink-soft">{dateAr(r.createdAt)}</span> },
    {
      key: 'isActive',
      header: 'الحالة',
      render: (r) => <Badge tone={r.isActive ? 'ok' : 'danger'}>{r.isActive ? 'نشط' : 'موقوف'}</Badge>,
    },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <Button
          size="sm"
          variant={r.isActive ? 'danger' : 'outline'}
          onClick={() =>
            setActive
              .mutateAsync({ id: r.id, isActive: !r.isActive })
              .then(() => toast.ok(r.isActive ? 'تم الإيقاف' : 'تم التفعيل'))
          }
        >
          {r.isActive ? 'إيقاف' : 'تفعيل'}
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="العملاء" subtitle={data ? `${data.total} عميل` : ''} />
      <div className="relative mb-4 max-w-sm">
        <Search className="absolute right-3 top-1/2 size-4 -translate-y-1/2 text-ink-soft" />
        <Input
          placeholder="ابحث بالاسم أو الجوال…"
          className="pr-9"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
        />
      </div>
      <DataTable columns={columns} rows={data?.items ?? []} loading={isLoading} empty="لا يوجد عملاء" />
      <Pagination page={page} totalPages={data?.totalPages ?? 1} onChange={setPage} />
    </div>
  )
}
