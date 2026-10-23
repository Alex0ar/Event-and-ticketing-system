import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

export type Tone = 'neutral' | 'brand' | 'success' | 'warning' | 'danger' | 'info'

const tones: Record<Tone, string> = {
  neutral: 'bg-slate-100 text-slate-700 ring-slate-200',
  brand: 'bg-brand-50 text-brand-700 ring-brand-100',
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  warning: 'bg-amber-50 text-amber-800 ring-amber-200',
  danger: 'bg-rose-50 text-rose-700 ring-rose-200',
  info: 'bg-sky-50 text-sky-700 ring-sky-200',
}

export function Badge({ tone = 'neutral', children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset', tones[tone], className)}>
      {children}
    </span>
  )
}

const statusTones: Record<string, Tone> = {
  PUBLISHED: 'success', VALID: 'success', PAID: 'success', CONFIRMED: 'success',
  DRAFT: 'neutral', PENDING: 'warning', PENDING_PAYMENT: 'warning', SOLD_OUT: 'info',
  USED: 'info', COMPLETED: 'info',
  CANCELLED: 'danger', VOID: 'danger', REFUNDED: 'danger', FAILED: 'danger', EXPIRED: 'danger',
}

export function StatusBadge({ status }: { status: string }) {
  return <Badge tone={statusTones[status] ?? 'neutral'}>{status.replace(/_/g, ' ').toLowerCase()}</Badge>
}
