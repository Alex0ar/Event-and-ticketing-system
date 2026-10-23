import { discardPaymentKey, paymentKeyFor } from './idempotency'

describe('payment idempotency keys', () => {
  beforeEach(() => sessionStorage.clear())

  it('reuses the same key for retries of one order', () => {
    expect(paymentKeyFor(7)).toBe(paymentKeyFor(7))
  })

  it('uses different keys per order', () => {
    expect(paymentKeyFor(7)).not.toBe(paymentKeyFor(8))
  })

  it('issues a fresh key after the attempt is discarded', () => {
    const first = paymentKeyFor(7)
    discardPaymentKey(7)
    expect(paymentKeyFor(7)).not.toBe(first)
  })
})
