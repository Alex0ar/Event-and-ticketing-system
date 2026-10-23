// One Idempotency-Key per payment attempt, persisted so that retrying after a network error
// (where we never saw the response) reuses the same key and can't double-charge.
// After a definitive outcome (paid / declined) the key is discarded so a new attempt gets a new key.
const key = (orderId: number) => `ticketflow.idem.${orderId}`

export function paymentKeyFor(orderId: number) {
  let value = sessionStorage.getItem(key(orderId))
  if (!value) {
    value = crypto.randomUUID()
    sessionStorage.setItem(key(orderId), value)
  }
  return value
}

export function discardPaymentKey(orderId: number) {
  sessionStorage.removeItem(key(orderId))
}
