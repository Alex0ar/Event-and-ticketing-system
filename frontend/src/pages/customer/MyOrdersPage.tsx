import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ordersApi } from '@/api/orders'
import { errorMessage } from '@/api/client'
import type { OrderResponse } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { StatusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatDate, formatDateTime, formatMoney } from '@/lib/format'

const DAY_MS = 24 * 60 * 60 * 1000

/** Mirrors the server rule; the server still makes the final decision. */
function cancellable(o: OrderResponse) {
  return o.status === 'PAID' && !!o.eventStartsAt && new Date(o.eventStartsAt).getTime() - Date.now() > DAY_MS
}

export function MyOrdersPage() {
  const queryClient = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['orders', 'mine'], queryFn: ordersApi.mine })

  const cancel = useMutation({
    mutationFn: ordersApi.cancel,
    onSuccess: () => {
      toast.success('Order refunded — the seats are back on sale')
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  if (isLoading) return <PageSpinner />
  return (
    <Container className="max-w-5xl">
      <PageHeader title="Orders" description="Paid orders can be cancelled and refunded until 24 hours before the event." />
      {error ? <ErrorAlert error={error} /> : !data?.length ? (
        <EmptyState title="No orders yet" action={<Link to="/events"><Button>Find an event</Button></Link>} />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">Order</th>
                  <th className="px-5 py-3 font-semibold">Event</th>
                  <th className="px-5 py-3 font-semibold">Placed</th>
                  <th className="px-5 py-3 font-semibold">Total</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((o) => (
                  <tr key={o.id}>
                    <td className="px-5 py-4 font-mono text-slate-500">#{o.id}</td>
                    <td className="px-5 py-4">
                      <div className="font-semibold">{o.eventTitle ?? '—'}</div>
                      {o.eventStartsAt && <div className="text-xs text-slate-500">{formatDateTime(o.eventStartsAt)}</div>}
                    </td>
                    <td className="px-5 py-4 text-slate-600">{formatDate(o.createdAt)}</td>
                    <td className="px-5 py-4 font-semibold">{formatMoney(o.total, o.currency)}</td>
                    <td className="px-5 py-4"><StatusBadge status={o.status} /></td>
                    <td className="px-5 py-4 text-right">
                      {o.status === 'PAID' && (
                        <Button size="sm" variant="secondary" disabled={!cancellable(o)}
                          title={cancellable(o) ? undefined : 'Less than 24h before the event'}
                          loading={cancel.isPending && cancel.variables === o.id}
                          onClick={() => window.confirm(`Cancel order #${o.id} and refund ${formatMoney(o.total, o.currency)}?`) && cancel.mutate(o.id)}>
                          Cancel &amp; refund
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </Container>
  )
}
