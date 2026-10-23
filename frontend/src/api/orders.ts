import { http } from './client'
import type { OrderResponse } from './types'

export const ordersApi = {
  initiate: () => http.post<OrderResponse>('/orders/initiate').then((r) => r.data),
  pay: (orderId: number, idempotencyKey: string, simulateDecline = false) =>
    http
      .post<OrderResponse>(`/orders/${orderId}/pay`, null, {
        headers: {
          'Idempotency-Key': idempotencyKey,
          ...(simulateDecline ? { 'X-Mock-Payment-Outcome': 'DECLINE' } : {}),
        },
      })
      .then((r) => r.data),
  mine: () => http.get<OrderResponse[]>('/orders/listOwnedOrders').then((r) => r.data),
  cancel: (orderId: number) => http.post<OrderResponse>(`/orders/${orderId}/cancel`).then((r) => r.data),
}
