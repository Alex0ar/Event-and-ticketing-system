import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import type { EventResponse, TierResponse } from '@/api/types'
import { EventCard } from './EventCard'

const tier = (over: Partial<TierResponse>): TierResponse => ({
  id: 1, name: 'General', description: null, priceAmount: 40, priceCurrency: 'EUR',
  totalQuantity: 100, available: 50, maxPerOrder: 4, ...over,
})

const event = (tiers: TierResponse[]): EventResponse => ({
  id: 1, venueId: 1, venue: { id: 1, name: 'Arena Nord', city: 'Berlin', country: 'Germany' },
  title: 'Neon Nights', description: null, category: 'CONCERT',
  startsAt: '2026-11-06T19:00:00Z', endsAt: '2026-11-06T23:00:00Z',
  salesStartsAt: '2026-10-01T00:00:00Z', salesEndsAt: '2026-11-06T18:00:00Z',
  status: 'PUBLISHED', bannerUrl: null, tiers,
})

const renderCard = (e: EventResponse) => render(<MemoryRouter><EventCard event={e} /></MemoryRouter>)

describe('EventCard', () => {
  it('shows the cheapest tier price and venue', () => {
    renderCard(event([tier({ id: 1, priceAmount: 95 }), tier({ id: 2, priceAmount: 45 })]))
    expect(screen.getByText('€45.00')).toBeInTheDocument()
    expect(screen.getByText(/Arena Nord · Berlin/)).toBeInTheDocument()
    expect(screen.getByRole('link')).toHaveAttribute('href', '/events/1')
  })

  it('flags sold-out events', () => {
    renderCard(event([tier({ available: 0 })]))
    expect(screen.getByText('Sold out')).toBeInTheDocument()
  })

  it('flags events with few seats left', () => {
    renderCard(event([tier({ available: 5 })]))
    expect(screen.getByText('Few left')).toBeInTheDocument()
  })
})
