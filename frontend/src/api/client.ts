import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { tokenStore } from '@/auth/tokenStore'
import type { ApiError, TokenResponse } from './types'

export const API_URL = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? 'http://localhost:8080'

export const http = axios.create({ baseURL: `${API_URL}/api/v1` })

http.interceptors.request.use((config) => {
  const token = tokenStore.getAccess()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** Listeners notified when the session can't be recovered (refresh failed). */
const sessionExpiredListeners = new Set<() => void>()
export const onSessionExpired = (fn: () => void) => {
  sessionExpiredListeners.add(fn)
  return () => {
    sessionExpiredListeners.delete(fn)
  }
}

// Single-flight refresh: concurrent 401s share one refresh request instead of racing,
// which matters because the backend rotates (and invalidates) refresh tokens on use.
let refreshInFlight: Promise<string | null> | null = null

export function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.getRefresh()
  if (!refreshToken) return Promise.resolve(null)
  refreshInFlight ??= axios
    .post<TokenResponse>(`${API_URL}/api/v1/auth/refresh`, { refreshToken })
    .then(({ data }) => {
      tokenStore.set(data.accessToken, data.refreshToken)
      return data.accessToken
    })
    .catch(() => {
      tokenStore.clear()
      return null
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

http.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    const isAuthCall = config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && config && !config._retried && !isAuthCall) {
      config._retried = true
      const token = await refreshAccessToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        return http(config)
      }
      sessionExpiredListeners.forEach((fn) => fn())
    }
    return Promise.reject(error)
  },
)

/** The backend's ApiError body, if the failure carried one. */
export function apiError(err: unknown): ApiError | null {
  if (axios.isAxiosError(err) && err.response?.data && typeof err.response.data === 'object') {
    const data = err.response.data as Partial<ApiError>
    if (typeof data.message === 'string') return data as ApiError
  }
  return null
}

/** Human-readable message for any failure. */
export function errorMessage(err: unknown, fallback = 'Something went wrong'): string {
  const api = apiError(err)
  if (api) return api.message
  if (axios.isAxiosError(err) && !err.response) return 'Cannot reach the server. It may be waking up — try again in a moment.'
  return fallback
}
