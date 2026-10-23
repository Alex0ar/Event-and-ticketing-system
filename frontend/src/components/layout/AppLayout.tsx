import { useState, type ReactNode } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { LogOut, Menu, Ticket, X } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { roleLabel } from '@/lib/format'
import { cn } from '@/lib/cn'
import { Button } from '../ui/Button'
import { ServerWakeBanner } from '../ServerWakeBanner'

function useNavLinks() {
  const { hasRole } = useAuth()
  const links = [{ to: '/events', label: 'Events' }]
  if (hasRole('CUSTOMER')) links.push({ to: '/my/tickets', label: 'My tickets' }, { to: '/my/orders', label: 'Orders' })
  if (hasRole('ORGANIZER')) links.push({ to: '/organizer', label: 'Dashboard' }, { to: '/organizer/venues', label: 'Venues' })
  if (hasRole('GATE_STAFF')) links.push({ to: '/gate', label: 'Gate scanner' })
  if (hasRole('ADMIN')) links.push({ to: '/admin', label: 'Admin' })
  links.push({ to: '/about', label: 'How it works' })
  return links
}

export function AppLayout() {
  const { user, status, logout } = useAuth()
  const navigate = useNavigate()
  const links = useNavLinks()
  const [open, setOpen] = useState(false)

  const signOut = async () => {
    await logout()
    navigate('/')
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    cn('rounded-lg px-3 py-2 text-sm font-medium transition-colors',
      isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900')

  return (
    <div className="flex min-h-screen flex-col">
      <ServerWakeBanner />
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/85 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4 sm:px-6">
          <Link to="/" className="flex items-center gap-2 font-extrabold tracking-tight text-slate-900">
            <span className="grid size-8 place-items-center rounded-lg bg-brand-600 text-white"><Ticket className="size-4.5" /></span>
            TicketFlow
          </Link>
          <nav className="hidden flex-1 items-center gap-1 md:flex">
            {links.map((l) => <NavLink key={l.to} to={l.to} end={l.to === '/organizer'} className={linkClass}>{l.label}</NavLink>)}
          </nav>
          <div className="ml-auto hidden items-center gap-3 md:flex">
            {status === 'authenticated' && user ? (
              <>
                <div className="text-right leading-tight">
                  <div className="text-sm font-semibold text-slate-900">{user.fullName}</div>
                  <div className="text-xs text-slate-500">{user.roles.map((r) => roleLabel[r]).join(', ')}</div>
                </div>
                <Button variant="ghost" size="sm" onClick={signOut} aria-label="Sign out"><LogOut className="size-4" /></Button>
              </>
            ) : status === 'anonymous' ? (
              <>
                <Link to="/login" className="text-sm font-semibold text-slate-700 hover:text-slate-900">Sign in</Link>
                <Link to="/register"><Button size="sm">Create account</Button></Link>
              </>
            ) : null}
          </div>
          <button className="ml-auto md:hidden" onClick={() => setOpen((o) => !o)} aria-label="Toggle menu">
            {open ? <X className="size-6" /> : <Menu className="size-6" />}
          </button>
        </div>
        {open && (
          <div className="border-t border-slate-200 bg-white px-4 py-3 md:hidden">
            <nav className="flex flex-col gap-1" onClick={() => setOpen(false)}>
              {links.map((l) => <NavLink key={l.to} to={l.to} end className={linkClass}>{l.label}</NavLink>)}
              {status === 'authenticated'
                ? <button onClick={signOut} className="rounded-lg px-3 py-2 text-left text-sm font-medium text-rose-600">Sign out ({user?.fullName})</button>
                : <NavLink to="/login" className={linkClass}>Sign in</NavLink>}
            </nav>
          </div>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-2 px-4 py-8 text-sm text-slate-500 sm:flex-row sm:justify-between sm:px-6">
          <span>TicketFlow — a portfolio project. Payments are simulated; no real money moves.</span>
          <span className="flex gap-4">
            <Link to="/about" className="hover:text-slate-900">Architecture</Link>
            {import.meta.env.VITE_GITHUB_URL && (
              <a href={import.meta.env.VITE_GITHUB_URL} target="_blank" rel="noreferrer" className="hover:text-slate-900">GitHub</a>
            )}
          </span>
        </div>
      </footer>
    </div>
  )
}

export function Container({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10', className)}>{children}</div>
}
