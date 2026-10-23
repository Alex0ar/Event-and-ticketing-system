import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { Role } from '@/api/types'
import { PageSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { useAuth } from './useAuth'

/** Route guard: anonymous visitors go to /login, signed-in users without the role see a notice. */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { status, hasRole } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <PageSpinner />
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (!hasRole(role)) {
    return (
      <EmptyState
        title="This area needs a different role"
        description={`Sign in with a ${role.replace('_', ' ').toLowerCase()} account to continue — the demo buttons on the login page switch roles in one click.`}
      />
    )
  }
  return <>{children}</>
}
