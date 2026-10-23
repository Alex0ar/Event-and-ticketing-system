import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { BarChart3, CalendarPlus, Settings2 } from 'lucide-react'
import { organizerApi } from '@/api/organizer'
import type { EventResponse } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { EventBanner } from '@/components/EventBanner'
import { StatusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatDateTime } from '@/lib/format'

function capacityOf(e: EventResponse) {
  const total = e.tiers.reduce((s, t) => s + t.totalQuantity, 0)
  const taken = total - e.tiers.reduce((s, t) => s + t.available, 0)
  return { total, taken, pct: total ? Math.round((taken / total) * 100) : 0 }
}

export function DashboardPage() {
  const { data, isLoading, error } = useQuery({ queryKey: ['organizer', 'events'], queryFn: organizerApi.myEvents })

  if (isLoading) return <PageSpinner />
  const events = [...(data ?? [])].sort((a, b) => a.startsAt.localeCompare(b.startsAt))
  const count = (s: string) => events.filter((e) => e.status === s).length

  return (
    <Container>
      <PageHeader eyebrow="Organizer" title="Your events"
        description="Create drafts, add ticket tiers, publish when ready and follow sales live."
        actions={<>
          <Link to="/organizer/venues"><Button variant="secondary">Venues</Button></Link>
          <Link to="/organizer/events/new"><Button><CalendarPlus className="size-4" /> New event</Button></Link>
        </>} />

      <div className="mb-8 grid gap-4 sm:grid-cols-4">
        {[['Total', events.length], ['Published', count('PUBLISHED')], ['Drafts', count('DRAFT')], ['Cancelled', count('CANCELLED')]].map(([label, n]) => (
          <Card key={label} className="p-5">
            <p className="text-sm text-slate-500">{label}</p>
            <p className="mt-1 text-3xl font-bold">{n}</p>
          </Card>
        ))}
      </div>

      {error ? <ErrorAlert error={error} /> : events.length === 0 ? (
        <EmptyState title="No events yet" description="Start by adding a venue, then create your first event."
          action={<Link to="/organizer/events/new"><Button>Create an event</Button></Link>} />
      ) : (
        <div className="space-y-3">
          {events.map((e) => {
            const cap = capacityOf(e)
            return (
              <Card key={e.id} className="flex flex-col overflow-hidden sm:flex-row">
                <EventBanner category={e.category} className="h-24 sm:h-auto sm:w-40" iconClass="size-10" />
                <div className="flex flex-1 flex-col gap-4 p-5 lg:flex-row lg:items-center">
                  <div className="flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-bold">{e.title}</h3>
                      <StatusBadge status={e.status} />
                    </div>
                    <p className="mt-1 text-sm text-slate-600">{formatDateTime(e.startsAt)} · {e.venue?.name}, {e.venue?.city}</p>
                  </div>
                  <div className="w-full lg:w-56">
                    <div className="flex justify-between text-xs text-slate-500"><span>{cap.taken} / {cap.total} sold or held</span><span>{cap.pct}%</span></div>
                    <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-slate-100">
                      <div className="h-full rounded-full bg-brand-600" style={{ width: `${cap.pct}%` }} />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Link to={`/organizer/events/${e.id}`}><Button variant="secondary" size="sm"><Settings2 className="size-4" /> Manage</Button></Link>
                    {e.status !== 'DRAFT' && <Link to={`/organizer/events/${e.id}/stats`}><Button variant="secondary" size="sm"><BarChart3 className="size-4" /> Stats</Button></Link>}
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      )}
    </Container>
  )
}
