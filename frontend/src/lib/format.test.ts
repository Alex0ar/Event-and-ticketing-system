import { formatMoney, multiplyMoney } from './format'

describe('money helpers', () => {
  it('multiplies in integer cents, avoiding floating-point drift', () => {
    expect(0.1 * 3).not.toBe(0.3)
    expect(multiplyMoney(0.1, 3)).toBe(0.3)
    expect(multiplyMoney(49.99, 3)).toBe(149.97)
  })

  it('formats euros', () => {
    expect(formatMoney(1234.5, 'EUR')).toBe('€1,234.50')
  })
})
