import { http } from './client'
import type { CheckInResponse, TicketResponse } from './types'

export const ticketsApi = {
  mine: () => http.get<TicketResponse[]>('/tickets/listMyTickets').then((r) => r.data),
  /** The QR endpoint needs the bearer token, so fetch it as a blob rather than using <img src>. */
  qrBlob: (code: string) => http.get<Blob>(`/tickets/${code}/qr`, { responseType: 'blob' }).then((r) => r.data),
  checkIn: (ticketCode: string) =>
    http.post<CheckInResponse>('/checkin', { ticketCode }).then((r) => r.data),
}
