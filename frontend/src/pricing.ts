import type { Voucher } from './types'

export function calculateVoucherDiscount(voucher: Voucher, subtotal: number) {
  if (subtotal < voucher.minimumSpend) return 0

  const subtotalCents = Math.round(subtotal * 100)
  const discountCents = voucher.discountType === 'PERCENT'
    ? Math.round(subtotalCents * voucher.discountValue / 100)
    : Math.round(voucher.discountValue * 100)

  return Math.min(subtotalCents, discountCents) / 100
}