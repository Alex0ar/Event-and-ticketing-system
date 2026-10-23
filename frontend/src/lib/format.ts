import type { Role } from '@/api/types'

const moneyFormatters = new Map<string, Intl.NumberFormat>()

/** Formats a BigDecimal-as-number for display. Never used for arithmetic on totals. */
export function formatMoney(amount: number, currency = 'EUR') {
  let f = moneyFormatters.get(currency)
  if (!f) {
    f = new Intl.NumberFormat('en-IE', { style: 'currency', currency })
    moneyFormatters.set(currency, f)
  }
  return f.format(amount)
}

/** Price × quantity preview, computed in integer cents to avoid floating-point drift. */
export function multiplyMoney(price: number, quantity: number) {
  return (Math.round(price * 100) * quantity) / 100
}

export function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('en-GB', {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function dateParts(iso: string) {
  const d = new Date(iso)
  return {
    day: d.toLocaleDateString('en-GB', { day: '2-digit' }),
    month: d.toLocaleDateString('en-GB', { month: 'short' }).toUpperCase(),
    weekday: d.toLocaleDateString('en-GB', { weekday: 'short' }),
    time: d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
  }
}

export const roleLabel: Record<Role, string> = {
  CUSTOMER: 'Customer',
  ORGANIZER: 'Organizer',
  GATE_STAFF: 'Gate staff',
  ADMIN: 'Admin',
}

export function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ')
}
