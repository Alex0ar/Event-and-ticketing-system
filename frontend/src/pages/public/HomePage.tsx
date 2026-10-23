import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Clock, QrCode, ShieldCheck, Zap } from 'lucide-react'
import { eventsApi } from '@/api/events'
import { useAuth } from '@/auth/useAuth'
import { Container } from '@/components/layout/AppLayout'
import { EventCard } from '@/components/EventCard'
import { DemoLoginButtons } from '@/components/DemoLoginButtons'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'

const features = [
  { icon: ShieldCheck, title: 'Never oversells', text: 'Seat holds are a single atomic SQL update — 200 simultaneous buyers for 10 seats means exactly 10 winners.' },
  { icon: Clock, title: '10-minute holds', text: 'Reserving locks your seats while you pay. Abandoned holds are released automatically by a scheduled job.' },
  { icon: Zap, title: 'Idempotent payments', text: 'Every payment carries an Idempotency-Key. A retried request can never charge twice or issue duplicate tickets.' },
  { icon: QrCode, title: 'QR check-in', text: 'Tickets are scanned at the gate with the phone camera. Each code works exactly once — even under concurrent scans.' },
]

export function HomePage() {
  const { status } = useAuth()
  const { data, isLoading } = useQuery({
    queryKey: ['events', 'featured'],
    queryFn: () => eventsApi.search({ size: 3, sort: 'startsAt' }),
  })

  return (
    <>
      <section className="relative overflow-hidden bg-slate-950 text-white">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,rgba(99,102,241,0.45),transparent_55%),radial-gradient(ellipse_at_bottom_left,rgba(236,72,153,0.3),transparent_50%)]" />
        <div className="relative mx-auto grid max-w-7xl gap-12 px-4 py-20 sm:px-6 lg:grid-cols-2 lg:py-28">
          <div>
            <p className="mb-4 inline-flex rounded-full bg-white/10 px-3 py-1 text-sm font-medium text-indigo-200 ring-1 ring-white/15">
              Spring Boot · PostgreSQL · React
            </p>
            <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl">
              Ticketing that holds up when everyone clicks <span className="bg-gradient-to-r from-indigo-300 to-pink-300 bg-clip-text text-transparent">“buy”</span> at once.
            </h1>
            <p className="mt-6 max-w-xl text-lg text-slate-300">
              Browse events, hold seats for ten minutes, pay, and get QR tickets that gate staff scan at the door.
              Built to show concurrency-safe inventory, idempotent payments and role-based access end to end.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/events"><Button size="lg">Browse events <ArrowRight className="size-4" /></Button></Link>
              <Link to="/about"><Button size="lg" variant="secondary" className="bg-white/10 text-white ring-white/20 hover:bg-white/20">How it works</Button></Link>
            </div>
          </div>
          {status !== 'authenticated' && (
            <div className="rounded-2xl bg-white/5 p-6 ring-1 ring-white/10 backdrop-blur">
              <h2 className="font-semibold">Try it in one click</h2>
              <p className="mb-4 mt-1 text-sm text-slate-300">Demo accounts for every role — no sign-up needed.</p>
              <div className="text-slate-900"><DemoLoginButtons /></div>
            </div>
          )}
        </div>
      </section>

      <Container>
        <div className="mb-6 flex items-end justify-between">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Coming up</h2>
            <p className="text-slate-600">Live availability from the database.</p>
          </div>
          <Link to="/events" className="text-sm font-semibold text-brand-600 hover:text-brand-700">All events →</Link>
        </div>
        {isLoading ? <div className="py-12 text-center"><Spinner className="size-7" /></div> : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {data?.content.map((e) => <EventCard key={e.id} event={e} />)}
          </div>
        )}

        <div className="mt-16 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f) => (
            <div key={f.title} className="rounded-2xl bg-white p-6 ring-1 ring-slate-200/80">
              <f.icon className="size-6 text-brand-600" />
              <h3 className="mt-3 font-semibold">{f.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{f.text}</p>
            </div>
          ))}
        </div>
      </Container>
    </>
  )
}
