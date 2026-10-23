import { useCallback, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CreditCard, Info, ShieldCheck, TimerOff } from 'lucide-react'
import { toast } from 'sonner'
import { reservationsApi } from '@/api/reservations'
import { ordersApi } from '@/api/orders'
import { eventsApi } from '@/api/events'
import { apiError } from '@/api/client'
import { Container } from '@/components/layout/AppLayout'
import { CountdownTimer } from '@/components/CountdownTimer'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatDateTime, formatMoney } from '@/lib/format'
import { discardPaymentKey, paymentKeyFor } from '@/lib/idempotency'

export function CheckoutPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [expired, setExpired] = useState(false)
  const [simulateDecline, setSimulateDecline] = useState(false)

  const { data: reservation, isLoading } = useQuery({
    queryKey: ['reservation', 'current'],
    queryFn: reservationsApi.current,
  })
  const { data: event } = useQuery({
    queryKey: ['event', reservation?.eventId],
    queryFn: () => eventsApi.get(reservation!.eventId),
    enabled: !!reservation,
  })

  const refreshAfterChange = () => {
    queryClient.invalidateQueries({ queryKey: ['reservation'] })
    queryClient.invalidateQueries({ queryKey: ['event'] })
    queryClient.invalidateQueries({ queryKey: ['events'] })
  }

  const release = useMutation({
    mutationFn: () => reservationsApi.release(reservation!.id),
    onSuccess: () => {
      refreshAfterChange()
      toast('Seats released back to the pool')
      navigate(`/events/${reservation!.eventId}`)
    },
  })

  const pay = useMutation({
    mutationFn: async () => {
      const order = await ordersApi.initiate() // returns the existing order on retry
      try {
        return await ordersApi.pay(order.id, paymentKeyFor(order.id), simulateDecline)
      } catch (err) {
        // A definitive answer (e.g. declined) ends this attempt: the next click gets a fresh key.
        // A network failure keeps the key, so a retry can't double-charge.
        if (apiError(err)) discardPaymentKey(order.id)
        throw err
      }
    },
    onSuccess: (order) => {
      discardPaymentKey(order.id)
      queryClient.setQueryData(['reservation', 'current'], null)
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      refreshAfterChange()
      toast.success('Payment successful — your tickets are ready')
      navigate('/my/tickets')
    },
  })

  const onExpire = useCallback(() => setExpired(true), [])

  if (isLoading) return <PageSpinner />
  if (!reservation || expired) {
    return (
      <Container>
        <EmptyState
          icon={expired ? <TimerOff className="size-7" /> : undefined}
          title={expired ? 'Your hold expired' : 'No active reservation'}
          description={expired
            ? 'Seats are only held for 10 minutes. They have been returned to the pool — pick them again if they are still available.'
            : 'Choose seats on an event page and they will be held here for 10 minutes while you pay.'}
          action={<Link to={expired ? `/events/${reservation?.eventId ?? ''}` : '/events'}><Button>Browse events</Button></Link>}
        />
      </Container>
    )
  }

  const declined = apiError(pay.error)?.errorCode === 'PAYMENT_DECLINED'

  return (
    <Container className="max-w-4xl">
      <PageHeader eyebrow="Checkout" title={event?.title ?? 'Your reservation'}
        description={event ? `${formatDateTime(event.startsAt)} · ${event.venue?.name ?? ''}` : undefined} />

      <div className="grid gap-6 md:grid-cols-[1fr_300px]">
        <Card className="p-6">
          <h2 className="font-bold">Held seats</h2>
          <ul className="mt-4 divide-y divide-slate-100">
            {reservation.items.map((line) => (
              <li key={line.tierId} className="flex justify-between py-3 text-sm">
                <span><span className="font-semibold">{line.quantity} ×</span> {line.tierName} <span className="text-slate-500">@ {formatMoney(line.unitPrice, reservation.currency)}</span></span>
                <span className="font-semibold">{formatMoney(line.lineTotal, reservation.currency)}</span>
              </li>
            ))}
          </ul>
          <div className="mt-2 flex justify-between border-t border-slate-200 pt-4 text-lg font-bold">
            <span>Total</span><span>{formatMoney(reservation.total, reservation.currency)}</span>
          </div>

          <label className="mt-6 flex cursor-pointer items-start gap-3 rounded-xl bg-slate-50 p-4 text-sm ring-1 ring-slate-200">
            <input type="checkbox" className="mt-0.5 size-4 accent-brand-600" checked={simulateDecline}
              onChange={(e) => setSimulateDecline(e.target.checked)} />
            <span>
              <span className="font-semibold">Simulate a declined card</span>
              <span className="block text-slate-600">Payments are mocked. Tick this to see a decline — your seats stay held so you can retry.</span>
            </span>
          </label>

          {pay.error && (
            <div className="mt-4">
              <ErrorAlert error={pay.error} title={declined ? 'Card declined' : undefined} />
            </div>
          )}

          <div className="mt-6 flex flex-col gap-3 sm:flex-row">
            <Button size="lg" className="flex-1" loading={pay.isPending} disabled={release.isPending} onClick={() => pay.mutate()}>
              <CreditCard className="size-5" /> {declined ? 'Try again' : `Pay ${formatMoney(reservation.total, reservation.currency)}`}
            </Button>
            <Button size="lg" variant="secondary" loading={release.isPending} disabled={pay.isPending} onClick={() => release.mutate()}>
              Release seats
            </Button>
          </div>
        </Card>

        <div className="space-y-4">
          <Card className="p-6 text-center">
            <p className="mb-3 text-sm font-medium text-slate-600">Seats held for</p>
            <CountdownTimer expiresAt={reservation.expiresAt} onExpire={onExpire} />
          </Card>
          <Card className="space-y-3 p-5 text-sm text-slate-600">
            <p className="flex gap-2"><ShieldCheck className="size-4 shrink-0 text-emerald-600" /><span>These seats were claimed with one atomic SQL update — nobody else can take them while the timer runs.</span></p>
            <p className="flex gap-2"><Info className="size-4 shrink-0 text-brand-600" /><span>Each payment attempt sends an <code className="text-xs">Idempotency-Key</code>; double-clicking or retrying never charges twice.</span></p>
          </Card>
        </div>
      </div>
    </Container>
  )
}
