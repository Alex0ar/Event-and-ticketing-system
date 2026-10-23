import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { organizerApi } from '@/api/organizer'
import { Container } from '@/components/layout/AppLayout'
import { StatusBadge } from '@/components/ui/Badge'
import { Card } from '@/components/ui/Card'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatMoney } from '@/lib/format'

function Tile({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <Card className="p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-bold tabular-nums">{value}</p>
      {sub && <p className="mt-0.5 text-xs text-slate-500">{sub}</p>}
    </Card>
  )
}

export function EventStatsPage() {
  const id = Number(useParams().id)
  const { data: s, isLoading, error } = useQuery({
    queryKey: ['organizer', 'stats', id],
    queryFn: () => organizerApi.stats(id),
    refetchInterval: 15_000, // near-live while the page is open
  })

  if (isLoading) return <PageSpinner />
  if (error || !s) return <Container><ErrorAlert error={error} /></Container>

  const pct = (n: number, d: number) => (d ? `${Math.round((n / d) * 100)}%` : '—')
  const tierChart = s.tiers.map((t) => ({ name: t.name, Sold: t.sold, Held: t.reserved, Available: t.total - t.sold - t.reserved }))
  const daily = s.dailySales.map((d) => ({ day: d.day.slice(5), Tickets: d.tickets }))

  return (
    <Container>
      <Link to={`/organizer/events/${id}`} className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-slate-600 hover:text-slate-900">
        <ArrowLeft className="size-4" /> Back to event
      </Link>
      <div className="mb-8 flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold sm:text-3xl">{s.title}</h1>
        <StatusBadge status={s.status} />
      </div>
      <p className="-mt-6 mb-8 text-sm text-slate-500">Aggregated in PostgreSQL (JPQL projection + date_trunc) — refreshes every 15 s.</p>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Tile label="Revenue" value={formatMoney(s.revenue, s.currency)} />
        <Tile label="Tickets sold" value={s.sold.toLocaleString()} sub={`${pct(s.sold, s.capacity)} of ${s.capacity.toLocaleString()} seats`} />
        <Tile label="Held right now" value={s.reserved.toLocaleString()} sub="in 10-minute reservations" />
        <Tile label="Checked in" value={s.checkedIn.toLocaleString()} sub={`${pct(s.checkedIn, s.sold)} of sold tickets`} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="mb-4 font-bold">Inventory by tier</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={tierChart} layout="vertical" margin={{ left: 24 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} />
              <XAxis type="number" />
              <YAxis type="category" dataKey="name" width={100} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Legend />
              <Bar dataKey="Sold" stackId="a" fill="#4f46e5" />
              <Bar dataKey="Held" stackId="a" fill="#f59e0b" />
              <Bar dataKey="Available" stackId="a" fill="#cbd5e1" />
            </BarChart>
          </ResponsiveContainer>
        </Card>
        <Card className="p-6">
          <h2 className="mb-4 font-bold">Tickets sold per day</h2>
          {daily.length === 0 ? <p className="py-20 text-center text-sm text-slate-500">No sales yet.</p> : (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={daily}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="day" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="Tickets" fill="#6366f1" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>
      </div>

      <Card className="mt-6 overflow-x-auto p-6">
        <h2 className="mb-4 font-bold">Tier breakdown</h2>
        <table className="w-full text-left text-sm">
          <thead className="text-xs uppercase tracking-wide text-slate-500">
            <tr><th className="py-2">Tier</th><th>Price</th><th>Sold</th><th>Held</th><th>Capacity</th><th>Checked in</th><th className="text-right">Revenue</th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {s.tiers.map((t) => (
              <tr key={t.tierId}>
                <td className="py-3 font-semibold">{t.name}</td>
                <td>{formatMoney(t.price, t.currency)}</td>
                <td>{t.sold}</td><td>{t.reserved}</td><td>{t.total}</td><td>{t.checkedIn}</td>
                <td className="text-right font-semibold">{formatMoney(t.revenue, t.currency)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </Container>
  )
}
