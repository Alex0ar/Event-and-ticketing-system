import type { EventResponse } from '@/api/types'

export function minPrice(event: EventResponse) {
  if (event.tiers.length === 0) return null
  return event.tiers.reduce((min, t) => (t.priceAmount < min.priceAmount ? t : min))
}
