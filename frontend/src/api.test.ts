import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'

afterEach(() => vi.unstubAllGlobals())

describe('shopping API client', () => {
  it('requests the selected dashboard for the demo user', async () => {
    const response = [{ id: 7, name: 'Market tote' }]
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => response })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.products('popular')).resolves.toEqual(response)

    expect(fetchMock).toHaveBeenCalledWith('/api/dashboard/popular?userId=demo', expect.objectContaining({
      headers: { 'Content-Type': 'application/json' },
    }))
  })

  it('sends cart additions as a stock reservation request', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ items: [] }) })
    vi.stubGlobal('fetch', fetchMock)

    await api.add(12)

    expect(fetchMock).toHaveBeenCalledWith('/api/cart/items?userId=demo', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ productId: 12, quantity: 1 }),
    }))
  })
})