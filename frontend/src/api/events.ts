import { http } from './client'
import type { EventResponse, EventSearchParams, PageResponse } from './types'

export const eventsApi = {
  search: (params: EventSearchParams) =>
    http.get<PageResponse<EventResponse>>('/events', { params }).then((r) => r.data),
  get: (id: number) => http.get<EventResponse>(`/events/${id}`).then((r) => r.data),
}
