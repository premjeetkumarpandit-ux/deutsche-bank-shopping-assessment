import type { Cart, Product, Voucher } from './types'

const userId = 'demo'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`/api${path}`, {
        ...init,
        headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
    if (!response.ok) {
        const detail = await response.text()
        throw new Error(detail || `Request failed (${response.status})`)
    }
    return response.json() as Promise<T>
}

export const api = {
    products: (section: 'liked' | 'popular' | 'new') =>
        request<Product[]>(`/dashboard/${section}?userId=${userId}`),
    vouchers: () => request<Voucher[]>('/dashboard/vouchers?location=London'),
    favorite: (productId: number) => request<{ saved: boolean }>(`/favorites/${productId}?userId=${userId}`, { method: 'PUT' }),
    unfavorite: (productId: number) => request<{ saved: boolean }>(`/favorites/${productId}?userId=${userId}`, { method: 'DELETE' }),
    cart: () => request<Cart>(`/cart?userId=${userId}`),
    add: (productId: number) => request<Cart>(`/cart/items?userId=${userId}`, {
        method: 'POST', body: JSON.stringify({ productId, quantity: 1 }),
    }),
    update: (productId: number, quantity: number) => request<Cart>(`/cart/items/${productId}?userId=${userId}`, {
        method: 'PUT', body: JSON.stringify({ quantity }),
    }),
    remove: (productId: number) => request<Cart>(`/cart/items/${productId}?userId=${userId}`, {
        method: 'DELETE',
    }),
}