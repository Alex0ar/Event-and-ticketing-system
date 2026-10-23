import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarDays, Clock, MapPin, Minus, Plus, Ticket } from 'lucide-react'
import { toast } from 'sonner'
import { eventsApi } from '@/api/events'
import { reservationsApi } from '@/api/reservations'
import { apiError } from '@/api/client'
import type { TierResponse } from '@/api/types'
import { useAuth } from '@/auth/useAuth'
import { Container } from '@/components/layout/AppLayout'
import { EventBanner } from '@/components/EventBanner'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatDateTime, formatMoney, multiplyMoney, titleCase } from '@/lib/format'

function Stepper({ value, max, onChange, disabled }: { value: number; max: number; onChange: (n: number) => void; disabled?: boolean }) {
  return (
    <div className="inline-flex items-center rounded-lg ring-1 ring-slate-300">
      <button type="button" aria-label="Fewer" disabled={disabled || value <= 0} onClick={() => onChange(value - 1)}
        className="grid size-9 place-items-center text-slate-600 disabled:opacity-30"><Minus className="size-4" /></button>
      <span className="w-8 text-center font-semibold tabular-nums" aria-live="polite">{value}</span>
      <button type="button" aria-label="More" disabled={disabled || value >= max} onClick={() => onChange(value + 1)}
        className="grid size-9 place-items-center text-slate-600 disabled:opacity-30"><Plus className="size-4" /></button>
    </div>
  )
}

function TierRow({ tier, qty, setQty }: { tier: TierResponse; qty: number; setQty: (n: number) => void }) {
  const soldOut = tier.available === 0
  const max = Math.min(tier.maxPerOrder, tier.available)
  return (
    <div className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center">
      <div className="flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-semibold">{tier.name}</span>
          {soldOut ? <Badge tone="danger">Sold out</Badge>
            : tier.available <= 10 ? <Badge tone="warning">Only {tier.available} left</Badge>
            : <span className="text-xs text-slate-500">{tier.available} available</span>}
        </div>
        {tier.description && <p className="text-sm text-slate-600">{tier.description}</p>}
        <p className="text-xs text-slate-500">Max {tier.maxPerOrder} per order</p>
      </div>
      <div className="flex items-center justify-between gap-4 sm:justify-end">
        <span className="w-24 text-right font-bold">{formatMoney(tier.priceAmount, tier.priceCurrency)}</span>
        <Stepper value={qty} max={max} onChange={setQty} disabled={soldOut} />
      </div>
    </div>
  )
}

export function EventDetailPage() {
  const id = Number(useParams().id)
  const { status, hasRole } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [quantities, setQuantities] = useState<Record<number, number>>({})
  const [openedAt] = useState(() => Date.now())

  const { data: event, isLoading, error } = useQuery({ queryKey: ['event', id], queryFn: () => eventsApi.get(id) })

  const reserve = useMutation({
    mutationFn: () => reservationsApi.reserve(id, Object.entries(quantities)
      .filter(([, q]) => q > 0).map(([tierId, quantity]) => ({ tierId: Number(tierId), quantity }))),
    onSuccess: (reservation) => {
      queryClient.setQueryData(['reservation', 'current'], reservation)
      queryClient.invalidateQueries({ queryKey: ['event', id] })
      toast.success('Seats held for 10 minutes')
      navigate('/checkout')
    },
  })

  if (isLoading) return <PageSpinner />
  if (error || !event) {
    return <EmptyState title="Event not found" description="It may have been cancelled or isn't on sale yet."
      action={<Link to="/events"><Button variant="secondary">Back to events</Button></Link>} />
  }

  const lines = event.tiers.map((t) => ({ tier: t, qty: quantities[t.id] ?? 0 })).filter((l) => l.qty > 0)
  const count = lines.reduce((s, l) => s + l.qty, 0)
  const subtotal = lines.reduce((s, l) => s + multiplyMoney(l.tier.priceAmount, l.qty), 0)
  const currency = event.tiers[0]?.priceCurrency ?? 'EUR'
  const salesOpen = Date.parse(event.salesStartsAt) <= openedAt && openedAt <= Date.parse(event.salesEndsAt)
  const pendingConflict = apiError(reserve.error)?.message.toLowerCase().includes('pending reservation')

  const onReserve = () => {
    if (status !== 'authenticated') {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    reserve.mutate()
  }

  return (
    <>
      <EventBanner category={event.category} bannerUrl={event.bannerUrl} className="h-56 sm:h-72" iconClass="size-24" />
      <Container className="-mt-20 relative">
        <div className="grid gap-8 lg:grid-cols-[1fr_380px]">
          <div className="space-y-6">
            <Card className="p-6 sm:p-8">
              <p className="text-sm font-semibold uppercase tracking-wide text-brand-600">{titleCase(event.category)}</p>
              <h1 className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">{event.title}</h1>
              <div className="mt-5 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                <span className="flex items-center gap-2"><CalendarDays className="size-4 text-slate-400" /> {formatDateTime(event.startsAt)}</span>
                {event.venue && <span className="flex items-center gap-2"><MapPin className="size-4 text-slate-400" /> {event.venue.name}, {event.venue.city}, {event.venue.country}</span>}
                <span className="flex items-center gap-2"><Clock className="size-4 text-slate-400" /> Sales close {formatDateTime(event.salesEndsAt)}</span>
              </div>
              {event.description && <p className="mt-6 leading-relaxed text-slate-700">{event.description}</p>}
            </Card>

            <Card className="p-6 sm:p-8">
              <h2 className="text-lg font-bold">Tickets</h2>
              <div className="divide-y divide-slate-100">
                {event.tiers.map((t) => (
                  <TierRow key={t.id} tier={t} qty={quantities[t.id] ?? 0}
                    setQty={(n) => setQuantities((q) => ({ ...q, [t.id]: n }))} />
                ))}
              </div>
            </Card>
          </div>

          <aside className="lg:sticky lg:top-24 lg:self-start">
            <Card className="p-6">
              <h2 className="flex items-center gap-2 font-bold"><Ticket className="size-5 text-brand-600" /> Your selection</h2>
              {lines.length === 0 ? (
                <p className="mt-3 text-sm text-slate-500">Choose tickets to continue.</p>
              ) : (
                <ul className="mt-4 space-y-2 text-sm">
                  {lines.map((l) => (
                    <li key={l.tier.id} className="flex justify-between">
                      <span>{l.qty} × {l.tier.name}</span>
                      <span className="font-medium">{formatMoney(multiplyMoney(l.tier.priceAmount, l.qty), currency)}</span>
                    </li>
                  ))}
                </ul>
              )}
              <div className="mt-4 flex justify-between border-t border-slate-100 pt-4 font-bold">
                <span>Total</span><span>{formatMoney(subtotal, currency)}</span>
              </div>
              <p className="mt-1 text-xs text-slate-500">The server recalculates the total — client prices are never trusted.</p>

              {status === 'authenticated' && !hasRole('CUSTOMER') ? (
                <p className="mt-5 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">Only customer accounts can buy tickets. Sign in as the demo customer to try checkout.</p>
              ) : (
                <Button className="mt-5 w-full" size="lg" disabled={count === 0 || !salesOpen} loading={reserve.isPending} onClick={onReserve}>
                  {!salesOpen ? 'Sales are closed' : status === 'authenticated' ? `Hold ${count || ''} seat${count === 1 ? '' : 's'} for 10 min` : 'Sign in to reserve'}
                </Button>
              )}
              {reserve.error && (
                <div className="mt-4 space-y-2">
                  <ErrorAlert error={reserve.error} />
                  {pendingConflict && <Link to="/checkout" className="block text-sm font-semibold text-brand-600">Go to your active reservation →</Link>}
                </div>
              )}
            </Card>
          </aside>
        </div>
      </Container>
    </>
  )
}
