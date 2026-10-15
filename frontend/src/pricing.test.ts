import { describe, expect, it } from 'vitest'
import { calculateVoucherDiscount } from './pricing'
import type { Voucher } from './types'

const percentOffer: Voucher = {
  code: 'WELCOME10',
  title: 'Welcome',
  description: '10% off orders over £30',
  discountLabel: '10% off',
  discountType: 'PERCENT',
  discountValue: 10,
  minimumSpend: 30,
  validUntil: '2030-01-01T00:00:00Z',
}

describe('voucher discount calculation', () => {
  it('applies percentage savings only after the minimum spend', () => {
    expect(calculateVoucherDiscount(percentOffer, 29.99)).toBe(0)
    expect(calculateVoucherDiscount(percentOffer, 39.99)).toBe(4)
  })

  it('caps fixed savings at the cart subtotal', () => {
    const fixedOffer = { ...percentOffer, discountType: 'FIXED' as const, discountValue: 5, minimumSpend: 25 }
    expect(calculateVoucherDiscount(fixedOffer, 30)).toBe(5)
    expect(calculateVoucherDiscount(fixedOffer, 26)).toBe(5)
  })
})