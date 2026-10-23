import { createContext } from 'react'
import type { Role, UserResponse } from '@/api/types'

export type Status = 'loading' | 'authenticated' | 'anonymous'

export interface AuthContextValue {
  user: UserResponse | null
  status: Status
  login: (email: string, password: string) => Promise<UserResponse>
  logout: () => Promise<void>
  hasRole: (role: Role) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)
