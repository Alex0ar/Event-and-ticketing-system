import { useState } from 'react'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import type { Role } from '@/api/types'
import { DEMO_ACCOUNTS, DEMO_PASSWORD } from '@/lib/demoAccounts'
import { errorMessage } from '@/api/client'
import { useAuth } from '@/auth/useAuth'
import { cn } from '@/lib/cn'

export function DemoLoginButtons({ compact = false }: { compact?: boolean }) {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [busy, setBusy] = useState<Role | null>(null)

  const signIn = async (account: (typeof DEMO_ACCOUNTS)[number]) => {
    setBusy(account.role)
    try {
      await login(account.email, DEMO_PASSWORD)
      toast.success(`Signed in as demo ${account.label.toLowerCase()}`)
      navigate(account.home)
    } catch (err) {
      toast.error(errorMessage(err, 'Demo login failed'))
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className={cn('grid gap-3', compact ? 'grid-cols-2' : 'sm:grid-cols-2')}>
      {DEMO_ACCOUNTS.map((a) => (
        <button
          key={a.role}
          type="button"
          onClick={() => signIn(a)}
          disabled={busy !== null}
          className="group flex items-start gap-3 rounded-xl bg-white p-4 text-left ring-1 ring-slate-200 transition hover:ring-brand-400 hover:shadow-md disabled:opacity-60"
        >
          <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-brand-50 text-brand-600 group-hover:bg-brand-600 group-hover:text-white">
            <a.icon className={cn('size-5', busy === a.role && 'animate-pulse')} />
          </span>
          <span>
            <span className="block font-semibold text-slate-900">{a.label}</span>
            {!compact && <span className="mt-0.5 block text-sm text-slate-600">{a.hint}</span>}
          </span>
        </button>
      ))}
    </div>
  )
}
