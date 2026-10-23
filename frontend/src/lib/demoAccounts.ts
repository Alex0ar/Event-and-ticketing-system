import { ScanLine, ShieldCheck, Ticket, Users } from 'lucide-react'
import type { Role } from '@/api/types'

export const DEMO_PASSWORD = 'demo1234'

export const DEMO_ACCOUNTS: { role: Role; email: string; label: string; hint: string; home: string; icon: typeof Ticket }[] = [
  { role: 'CUSTOMER', email: 'customer@demo.ticketflow', label: 'Customer', icon: Ticket, home: '/events',
    hint: 'Browse, reserve with a 10-minute hold, pay, get QR tickets' },
  { role: 'ORGANIZER', email: 'organizer@demo.ticketflow', label: 'Organizer', icon: Users, home: '/organizer',
    hint: 'Publish the draft gala, add tiers, watch sales stats' },
  { role: 'GATE_STAFF', email: 'gate@demo.ticketflow', label: 'Gate staff', icon: ScanLine, home: '/gate',
    hint: 'Scan tickets at the door — each works exactly once' },
  { role: 'ADMIN', email: 'admin@demo.ticketflow', label: 'Admin', icon: ShieldCheck, home: '/admin',
    hint: 'Grant roles to users' },
]
