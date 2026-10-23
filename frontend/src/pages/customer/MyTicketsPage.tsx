import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { CalendarDays, Copy, MapPin, Ticket } from 'lucide-react'
import { toast } from 'sonner'
import { ticketsApi } from '@/api/tickets'
import type { TicketResponse } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { QrImage } from '@/components/QrImage'
import { StatusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatDateTime } from '@/lib/format'
import { cn } from '@/lib/cn'

function TicketCard({ ticket }: { ticket: TicketResponse }) {
  const valid = ticket.status === 'VALID'
  const copy = async () => {
    await navigator.clipboard.writeText(ticket.uuidCode)
    toast.success('Ticket code copied — paste it on the gate scanner')
  }
  return (
    <div className={cn('ticket-notch flex flex-col overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 sm:flex-row', !valid && 'opacity-70')}>
      <div className="flex-1 p-6">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">{ticket.tierName}</p>
            <h3 className="mt-1 text-lg font-bold leading-snug">{ticket.eventTitle}</h3>
          </div>
          <StatusBadge status={ticket.status} />
        </div>
        <div className="mt-4 space-y-1.5 text-sm text-slate-600">
          {ticket.eventStartsAt && <p className="flex items-center gap-2"><CalendarDays className="size-4" /> {formatDateTime(ticket.eventStartsAt)}</p>}
          {ticket.venueName && <p className="flex items-center gap-2"><MapPin className="size-4" /> {ticket.venueName}, {ticket.venueCity}</p>}
          <p className="flex items-center gap-2"><Ticket className="size-4" /> {ticket.ownerName}</p>
          {ticket.checkedInAt && <p className="text-xs text-slate-500">Scanned {formatDateTime(ticket.checkedInAt)}</p>}
        </div>
        <button onClick={copy} className="mt-4 inline-flex max-w-full items-center gap-1.5 rounded-md bg-slate-100 px-2 py-1 font-mono text-[11px] text-slate-600 hover:bg-slate-200" title="Copy ticket code">
          <span className="truncate">{ticket.uuidCode}</span> <Copy className="size-3 shrink-0" />
        </button>
      </div>
      <div className="flex items-center justify-center border-t border-dashed border-slate-300 bg-slate-50 p-5 sm:border-l sm:border-t-0">
        {valid ? <QrImage code={ticket.uuidCode} className="size-36" />
          : <div className="grid size-36 place-items-center text-center text-sm font-semibold text-slate-500">{ticket.status === 'USED' ? 'Already scanned' : 'Not valid'}</div>}
      </div>
    </div>
  )
}

export function MyTicketsPage() {
  const { data, isLoading, error } = useQuery({ queryKey: ['tickets', 'mine'], queryFn: ticketsApi.mine })

  if (isLoading) return <PageSpinner />
  return (
    <Container className="max-w-5xl">
      <PageHeader title="My tickets" description="Show the QR at the entrance. Each code is accepted exactly once." />
      {error ? <ErrorAlert error={error} /> : !data?.length ? (
        <EmptyState title="No tickets yet" description="Tickets appear here as soon as a payment goes through."
          action={<Link to="/events"><Button>Find an event</Button></Link>} />
      ) : (
        <div className="grid gap-5 lg:grid-cols-2">
          {data.map((t) => <TicketCard key={t.uuidCode} ticket={t} />)}
        </div>
      )}
    </Container>
  )
}
