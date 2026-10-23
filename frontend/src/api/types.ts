// TypeScript mirrors of the backend DTOs. Instants arrive as ISO-8601 strings;
// BigDecimal amounts arrive as JSON numbers and are only formatted, never summed, client-side.

export type Role = 'CUSTOMER' | 'ORGANIZER' | 'GATE_STAFF' | 'ADMIN'
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'SOLD_OUT' | 'CANCELLED' | 'COMPLETED'
export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED'
export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'FAILED' | 'REFUNDED'
export type TicketStatus = 'VALID' | 'USED' | 'VOID' | 'REFUNDED'

export interface ApiError {
  timestamp: string
  path: string
  status: number
  errorCode: string
  message: string
  fieldErrors: { field: string; message: string }[]
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export interface UserResponse {
  id: number
  email: string
  fullName: string
  roles: Role[]
}

export interface VenueSummary {
  id: number
  name: string
  city: string
  country: string
}

export interface TierResponse {
  id: number
  name: string
  description: string | null
  priceAmount: number
  priceCurrency: string
  totalQuantity: number
  available: number
  maxPerOrder: number
}

export interface EventResponse {
  id: number
  venueId: number
  venue: VenueSummary | null
  title: string
  description: string | null
  category: string
  startsAt: string
  endsAt: string
  salesStartsAt: string
  salesEndsAt: string
  status: EventStatus
  bannerUrl: string | null
  tiers: TierResponse[]
}

export interface EventSearchParams {
  query?: string
  city?: string
  category?: string
  startsAfter?: string
  startsBefore?: string
  minPrice?: string
  maxPrice?: string
  page?: number
  size?: number
  sort?: string
}

export interface ReservationLine {
  tierId: number
  tierName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface ReservationResponse {
  id: number
  eventId: number
  status: ReservationStatus
  expiresAt: string
  secondsUntilExpiry: number
  total: number
  currency: string
  items: ReservationLine[]
}

export interface OrderResponse {
  id: number
  userId: number
  reservationId: number
  status: OrderStatus
  total: number
  currency: string
  eventId: number | null
  eventTitle: string | null
  eventStartsAt: string | null
  createdAt: string
}

export interface TicketResponse {
  ownerName: string
  status: TicketStatus
  uuidCode: string
  ownerId: number
  tierId: number
  tierName: string | null
  orderId: number
  eventId: number
  eventTitle: string | null
  eventStartsAt: string | null
  venueName: string | null
  venueCity: string | null
  checkedInAt: string | null
}

export interface VenueRequest {
  name: string
  address: string
  city: string
  country: string
  capacity: number
}

export interface VenueResponse extends VenueRequest {
  id: number
}

export interface EventCreateRequest {
  venueId: number
  title: string
  description?: string
  category: string
  startsAt: string
  endsAt: string
  salesStartsAt: string
  salesEndsAt: string
  bannerUrl?: string
}

export interface TierRequest {
  name: string
  description?: string
  priceAmount: number
  priceCurrency: string
  totalQuantity: number
  maxPerOrder: number
}

export interface TierStats {
  tierId: number
  name: string
  price: number
  currency: string
  total: number
  sold: number
  reserved: number
  revenue: number
  checkedIn: number
}

export interface EventStatsResponse {
  eventId: number
  title: string
  status: EventStatus
  currency: string
  capacity: number
  sold: number
  reserved: number
  available: number
  revenue: number
  checkedIn: number
  tiers: TierStats[]
  dailySales: { day: string; tickets: number }[]
}

export interface CheckInResponse {
  ticketCode: string
  holderName: string
  eventTitle: string
  tierName: string
  checkedInAt: string
}
