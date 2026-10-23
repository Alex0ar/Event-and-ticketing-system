import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { onSessionExpired, refreshAccessToken } from '@/api/client'
import type { UserResponse } from '@/api/types'
import { AuthContext, type AuthContextValue, type Status } from './authContext'
import { tokenStore } from './tokenStore'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const queryClient = useQueryClient()

  const reset = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    setStatus('anonymous')
    queryClient.clear()
  }, [queryClient])

  // On load, a stored refresh token silently restores the session.
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const token = tokenStore.getRefresh() ? await refreshAccessToken() : null
      if (!token) {
        if (!cancelled) setStatus('anonymous')
        return
      }
      try {
        const me = await authApi.me()
        if (!cancelled) {
          setUser(me)
          setStatus('authenticated')
        }
      } catch {
        if (!cancelled) reset()
      }
    })()
    return () => {
      cancelled = true
    }
  }, [reset])

  useEffect(() => onSessionExpired(reset), [reset])

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password)
    tokenStore.set(tokens.accessToken, tokens.refreshToken)
    const me = await authApi.me()
    queryClient.clear()
    setUser(me)
    setStatus('authenticated')
    return me
  }, [queryClient])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // revoking server-side is best-effort; the local session ends regardless
    }
    reset()
  }, [reset])

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, logout, hasRole: (role) => !!user?.roles.includes(role) }),
    [user, status, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
