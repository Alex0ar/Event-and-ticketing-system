import { Link } from 'react-router'
import { API_URL } from '@/api/client'
import { Container } from '@/components/layout/AppLayout'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'

const stack = [
  ['Backend', 'Java 21, Spring Boot 3.5, Spring Security (JWT access + rotating refresh tokens), Spring Data JPA'],
  ['Database', 'PostgreSQL 16, schema owned by Flyway migrations, CHECK constraints on inventory'],
  ['Frontend', 'React 19, TypeScript, Vite, TanStack Query, Tailwind CSS, ZXing camera scanning'],
  ['Testing', 'JUnit 5 + Testcontainers against real Postgres, including 200-thread concurrency tests'],
  ['Hosting', 'Static frontend on Vercel, Dockerized API on Render, Postgres on Neon'],
]

const decisions = [
  {
    title: 'Overselling is impossible, not just unlikely',
    body: (
      <>
        <p>Reserving seats is one indivisible SQL statement — the availability check and the increment happen together:</p>
        <pre className="my-3 overflow-x-auto rounded-lg bg-slate-900 p-4 text-xs text-slate-100">{`UPDATE ticket_tiers
   SET reserved_quantity = reserved_quantity + :qty
 WHERE id = :id
   AND total_quantity - reserved_quantity - sold_quantity >= :qty`}</pre>
        <p>The number of updated rows is the answer: <code>1</code> = seats held, <code>0</code> = not enough left. A naive
          read-check-write lets two buyers read the same “1 seat left” before either writes; here there is no gap to race through.
          A test fires 200 parallel reservations at 10 seats and asserts exactly 10 succeed. A database <code>CHECK</code> constraint
          backs it up so an oversold row can't even be stored.</p>
      </>
    ),
  },
  {
    title: 'Holds expire on their own',
    body: <p>A reservation holds inventory for 10 minutes. A scheduled job releases unpaid holds using row locking so it's safe with several app instances running.</p>,
  },
  {
    title: 'Payments are idempotent',
    body: <p>Each payment attempt carries an <code>Idempotency-Key</code>, stored with a unique constraint. A retried or double-clicked request returns the first outcome instead of charging again — including declines. Paying moves seats from reserved to sold and issues tickets in one transaction.</p>,
  },
  {
    title: 'One scan per ticket',
    body: <p>Check-in flips a ticket from <code>VALID</code> to <code>USED</code> with the same conditional-update technique, so two gates scanning the same code at once can't both admit it. Rejections carry distinct codes (used, refunded, void) the scanner shows to staff.</p>,
  },
  {
    title: 'Ownership is checked in the service layer',
    body: <p>Roles gate endpoints with <code>@PreAuthorize</code>; ownership (“is this <em>your</em> event / order / ticket?”) is checked in services, so an organizer can't read another organizer's stats even with a valid token.</p>,
  },
  {
    title: 'No N+1 queries on lists',
    body: <p>Catalog pages load events, then all their tiers and venues in two batched queries. A test counts SQL statements with Hibernate statistics to keep it that way. Organizer stats are aggregated in SQL, not summed in Java.</p>,
  },
]

export function AboutPage() {
  return (
    <Container className="max-w-4xl">
      <p className="text-sm font-semibold text-brand-600">How it works</p>
      <h1 className="mt-1 text-3xl font-extrabold tracking-tight sm:text-4xl">The engineering behind TicketFlow</h1>
      <p className="mt-4 text-lg text-slate-600">
        The domain is ticketing because it forces the problem most CRUD demos skip: many people competing for the same
        scarce thing at the same moment. Everything below is running in this demo.
      </p>

      <div className="mt-10 space-y-5">
        {decisions.map((d, i) => (
          <Card key={d.title} className="p-6">
            <h2 className="flex items-center gap-3 text-lg font-bold">
              <span className="grid size-7 place-items-center rounded-full bg-brand-600 text-sm text-white">{i + 1}</span>{d.title}
            </h2>
            <div className="mt-3 space-y-2 text-slate-700">{d.body}</div>
          </Card>
        ))}
      </div>

      <h2 className="mt-12 text-xl font-bold">Stack</h2>
      <Card className="mt-4 divide-y divide-slate-100">
        {stack.map(([k, v]) => (
          <div key={k} className="grid gap-1 p-4 sm:grid-cols-[140px_1fr]">
            <span className="font-semibold">{k}</span><span className="text-slate-700">{v}</span>
          </div>
        ))}
      </Card>

      <div className="mt-10 flex flex-wrap gap-3">
        {import.meta.env.VITE_GITHUB_URL && <a href={import.meta.env.VITE_GITHUB_URL} target="_blank" rel="noreferrer"><Button>Source on GitHub</Button></a>}
        <a href={`${API_URL}/swagger-ui.html`} target="_blank" rel="noreferrer"><Button variant="secondary">API docs (Swagger)</Button></a>
        <Link to="/login"><Button variant="secondary">Try the demo accounts</Button></Link>
      </div>
    </Container>
  )
}
