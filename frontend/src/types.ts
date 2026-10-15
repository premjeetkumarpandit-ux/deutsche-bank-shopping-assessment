export type Product = {
  id: number
  name: string
  description: string
  category: string
  price: number
  compareAtPrice: number
  imageUrl: string
  stock: number
  popularity: number
  isNew: boolean
  createdAt: string
}

export type Voucher = {
  code: string
  title: string
  description: string
  discountLabel: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  minimumSpend: number
  validUntil: string
}

export type CartLine = {
  id: number
  product: Product
  quantity: number
  expiresAt: string
}

export type Cart = {
  items: CartLine[]
  total: number
  expiresAt: string | null
}