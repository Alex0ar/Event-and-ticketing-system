import { http } from './client'
import type { ReservationResponse } from './types'

export const reservationsApi = {
  reserve: (eventId: number, items: { tierId: number; quantity: number }[]) =>
    http.post<ReservationResponse>('/reservations', { eventId, items }).then((r) => r.data),
  /** null when the caller has no active hold (204). */
  current: () =>
    http.get<ReservationResponse | ''>('/reservations/current').then((r) => (r.status === 204 || !r.data ? null : r.data)),
  release: (id: number) => http.delete(`/reservations/${id}`),
}
