import { http } from './client'
import type {
  EventCreateRequest,
  EventResponse,
  EventStatsResponse,
  TierRequest,
  TierResponse,
  VenueRequest,
  VenueResponse,
} from './types'

export const organizerApi = {
  myEvents: () => http.get<EventResponse[]>('/organizer/events').then((r) => r.data),
  event: (id: number) => http.get<EventResponse>(`/organizer/events/${id}`).then((r) => r.data),
  createEvent: (body: EventCreateRequest) => http.post<EventResponse>('/organizer/events', body).then((r) => r.data),
  addTier: (eventId: number, body: TierRequest) =>
    http.post<TierResponse>(`/organizer/events/${eventId}/tiers`, body).then((r) => r.data),
  publish: (eventId: number) => http.post<EventResponse>(`/organizer/events/${eventId}/publish`).then((r) => r.data),
  cancel: (eventId: number) => http.post<EventResponse>(`/organizer/events/${eventId}/cancel`).then((r) => r.data),
  stats: (eventId: number) =>
    http.get<EventStatsResponse>(`/organizer/events/${eventId}/stats`).then((r) => r.data),

  venues: () => http.get<VenueResponse[]>('/venues').then((r) => r.data),
  createVenue: (body: VenueRequest) => http.post<VenueResponse>('/venues', body).then((r) => r.data),
  updateVenue: (id: number, body: VenueRequest) => http.post<VenueResponse>(`/venues/${id}`, body).then((r) => r.data),
  deleteVenue: (id: number) => http.delete(`/venues/${id}`),
}
