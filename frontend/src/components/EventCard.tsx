import { Link } from 'react-router'
import { MapPin } from 'lucide-react'
import type { EventResponse } from '@/api/types'
import { dateParts, formatMoney, titleCase } from '@/lib/format'
import { minPrice } from '@/lib/events'
import { Badge } from './ui/Badge'
import { EventBanner } from './EventBanner'

export function EventCard({ event }: { event: EventResponse }) {
  const cheapest = minPrice(event)
  const available = event.tiers.reduce((sum, t) => sum + t.available, 0)
  const total = event.tiers.reduce((sum, t) => sum + t.totalQuantity, 0)
  const soldOut = available === 0
  const fewLeft = !soldOut && total > 0 && available / total < 0.15
  const d = dateParts(event.startsAt)

  return (
    <Link
      to={`/events/${event.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-slate-200/80 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg"
    >
      <div className="relative h-40">
        <EventBanner category={event.category} bannerUrl={event.bannerUrl} className="h-full" />
        <div className="absolute left-4 top-4 rounded-xl bg-white/95 px-3 py-1.5 text-center shadow">
          <div className="text-[11px] font-bold tracking-wider text-brand-600">{d.month}</div>
          <div className="text-xl font-extrabold leading-none text-slate-900">{d.day}</div>
        </div>
        <div className="absolute right-4 top-4 flex gap-1.5">
          {soldOut && <Badge tone="danger">Sold out</Badge>}
          {fewLeft && <Badge tone="warning">Few left</Badge>}
        </div>
      </div>
      <div className="flex flex-1 flex-col p-5">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{titleCase(event.category)}</p>
        <h3 className="mt-1 text-lg font-bold leading-snug text-slate-900 group-hover:text-brand-700">{event.title}</h3>
        {event.venue && (
          <p className="mt-2 flex items-center gap-1.5 text-sm text-slate-600">
            <MapPin className="size-4 shrink-0" /> {event.venue.name} · {event.venue.city}
          </p>
        )}
        <div className="mt-auto flex items-end justify-between pt-4">
          <span className="text-sm text-slate-500">{d.weekday} · {d.time}</span>
          {cheapest && (
            <span className="text-sm text-slate-600">
              from <span className="text-base font-bold text-slate-900">{formatMoney(cheapest.priceAmount, cheapest.priceCurrency)}</span>
            </span>
          )}
        </div>
      </div>
    </Link>
  )
}
