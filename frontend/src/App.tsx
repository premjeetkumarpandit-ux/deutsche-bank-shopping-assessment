import { useEffect, useState } from 'react'
import { ArrowDownRight, ArrowRight, Check, Clock3, Heart, MapPin, Minus, Plus, Search, ShoppingBag, Sparkles, X } from 'lucide-react'
import { api } from './api'
import { calculateVoucherDiscount } from './pricing'
import type { Cart, Product, Voucher } from './types'

type Section = 'shop' | 'vouchers'
type ProductFeed = 'liked' | 'popular' | 'new'
const navigation: { id: Section; label: string }[] = [
  { id: 'shop', label: 'Shop' },
  { id: 'vouchers', label: 'Offers' },
]
const productFeeds: { id: ProductFeed; label: string; eyebrow: string; number: string }[] = [
  { id: 'liked', label: 'For you', eyebrow: 'YOUR PERSONAL EDIT', number: '01' },
  { id: 'popular', label: 'Most loved', eyebrow: 'THE CROWD FAVOURITES', number: '02' },
  { id: 'new', label: 'Just in', eyebrow: 'FRESH OFF THE SHELF', number: '03' },
]
const emptyFeeds: Record<ProductFeed, Product[]> = { liked: [], popular: [], new: [] }
const emptyCart: Cart = { items: [], total: 0, expiresAt: null }

function money(amount: number) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' }).format(amount)
}

function App() {
  const [section, setSection] = useState<Section>('shop')
  const [feeds, setFeeds] = useState(emptyFeeds)
  const [vouchers, setVouchers] = useState<Voucher[]>([])
  const [cart, setCart] = useState<Cart>(emptyCart)
  const [favoriteIds, setFavoriteIds] = useState<number[]>([])
  const [selectedVoucherCode, setSelectedVoucherCode] = useState('')
  const [appliedVoucherCode, setAppliedVoucherCode] = useState('')
  const [voucherMessage, setVoucherMessage] = useState('')
  const [cartOpen, setCartOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [loading, setLoading] = useState(true)
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    const load = section === 'vouchers'
      ? api.vouchers().then((result) => {
          if (active) setVouchers(result)
        })
      : Promise.all([
          api.products('liked'),
          api.products('popular'),
          api.products('new'),
        ]).then(([liked, popular, newest]) => {
          if (!active) return
          setFeeds({ liked, popular, new: newest })
          setFavoriteIds(liked.map((product) => product.id))
        })
    load.catch((cause: Error) => active && setError(cause.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [section])

  useEffect(() => {
    api.cart().then(setCart).catch((cause: Error) => setError(cause.message))
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    if (!cartOpen) return
    let active = true
    api.vouchers().then((available) => active && setVouchers(available))
      .catch(() => active && setVoucherMessage('Offers are unavailable right now.'))
    return () => { active = false }
  }, [cartOpen])

  useEffect(() => {
    const applied = vouchers.find((voucher) => voucher.code === appliedVoucherCode)
    if (!applied) return
    if (new Date(applied.validUntil).getTime() <= now) {
      setAppliedVoucherCode('')
      setVoucherMessage(`${applied.code} expired and was removed.`)
    } else if (cart.total < applied.minimumSpend) {
      setAppliedVoucherCode('')
      setVoucherMessage(`${applied.code} was removed because the cart no longer meets its minimum spend.`)
    }
  }, [appliedVoucherCode, cart.total, now, vouchers])

  async function refreshCart(action: Promise<Cart>, message: string) {
    try {
      const updatedCart = await action
      setCart(updatedCart)
      if (updatedCart.items.length === 0) {
        setSelectedVoucherCode('')
        setAppliedVoucherCode('')
        setVoucherMessage('')
      }
      setToast(message)
      window.setTimeout(() => setToast(''), 2400)
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'That did not work. Please try again.')
    }
  }

  async function toggleFavorite(product: Product) {
    const isFavorite = favoriteIds.includes(product.id)
    try {
      if (isFavorite) await api.unfavorite(product.id)
      else await api.favorite(product.id)
      setFavoriteIds((current) => isFavorite
        ? current.filter((id) => id !== product.id)
        : [...current, product.id])
      if (isFavorite && section === 'shop') {
        setFeeds((current) => ({
          ...current,
          liked: current.liked.filter((item) => item.id !== product.id),
        }))
      }
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not update saved items.')
    }
  }

  const cartCount = cart.items.reduce((sum, line) => sum + line.quantity, 0)
  const selectedVoucher = vouchers.find((voucher) => voucher.code === selectedVoucherCode)
  const appliedVoucher = vouchers.find((voucher) => voucher.code === appliedVoucherCode)
  const selectedVoucherExpired = selectedVoucher ? new Date(selectedVoucher.validUntil).getTime() <= now : false
  const selectedVoucherEligible = Boolean(selectedVoucher && !selectedVoucherExpired && cart.total >= selectedVoucher.minimumSpend)
  const voucherDiscount = appliedVoucher && new Date(appliedVoucher.validUntil).getTime() > now
    ? calculateVoucherDiscount(appliedVoucher, cart.total)
    : 0
  const totalAfterVoucher = Math.max(0, cart.total - voucherDiscount)

  function applySelectedVoucher() {
    if (!selectedVoucher || !selectedVoucherEligible) return
    setAppliedVoucherCode(selectedVoucher.code)
    setVoucherMessage(`${selectedVoucher.code} applied.`)
  }

  return (
    <div className="shop-shell">
      <div className="announcement"><Sparkles size={14} /> A good day for finding something lovely <span>Free London delivery over £45</span></div>
      <header className="site-header">
        <a className="wordmark" href="#top" aria-label="Market Lane home">market<span>lane</span><i>.</i></a>
        <div className="location"><MapPin size={16} /><span>Delivering to</span><strong>London</strong></div>
        <label className="search-box"><Search size={17} /><input aria-label="Search the collection" placeholder="Search the collection" value={query} onChange={(event) => setQuery(event.target.value)} /><kbd>↵</kbd></label>
        <button className="bag-button" aria-label={`Open bag, ${cartCount} items`} onClick={() => setCartOpen(true)}><ShoppingBag size={19} /><span>Bag</span><b>{cartCount}</b></button>
      </header>

      <nav className="main-nav" aria-label="Shop dashboards">
        {navigation.map((item) => <button key={item.id} className={section === item.id ? 'nav-link active' : 'nav-link'} onClick={() => setSection(item.id)}>{item.label}</button>)}
        <span className="nav-note">Curated close to home</span>
      </nav>

      <main id="top">
        <section className="hero-band">
          <div className="hero-copy">
            <p className="eyebrow"><span className="eyebrow-line" /> THE LONDON EDIT · NO. 04</p>
            <h1>Good things,<br /><em>close to home.</em></h1>
            <p className="hero-description">Thoughtful pieces from independent makers, picked for everyday living in the city.</p>
            <button className="hero-link" onClick={() => { setSection('shop'); document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth' }) }}>Explore what’s loved <ArrowRight size={16} /></button>
          </div>
          <div className="hero-image" role="img" aria-label="A sunlit still life of flowers and homewares">
            <div className="image-caption"><span>01 / 04</span><span>Small finds, big feeling</span><ArrowDownRight size={17} /></div>
          </div>
          <div className="hero-side-note">Made with care<br />in and around London</div>
        </section>

        <section className="collection-section" id="collection">
          <div className="section-heading">
            <div><p className="eyebrow">A LITTLE BIT OF GOOD</p><h2>{section === 'vouchers' ? 'A treat, on us.' : 'Picked around your day.'}</h2></div>
            <p className="collection-intro">Independent labels, everyday favourites<br />and the things worth passing on.</p>
          </div>
          {error && <div className="status-banner" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss error"><X size={16} /></button></div>}
          {section === 'vouchers' ? (
            <div className="voucher-grid">{loading ? <p className="loading-state">Finding your offers…</p> : vouchers.map((voucher) => <article className="voucher-ticket" key={voucher.code}><div className="ticket-top"><span>MARKET LANE · LONDON</span><Sparkles size={18} /></div><div className="ticket-discount">{voucher.discountLabel}</div><h3>{voucher.title}</h3><p>{voucher.description}</p><div className="ticket-code"><span>USE CODE</span><strong>{voucher.code}</strong><span>UNTIL {new Date(voucher.validUntil).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}</span></div></article>)}</div>
          ) : (
            <div className="dashboard-feed-grid">
              {productFeeds.map((feed) => {
                const feedProducts = feeds[feed.id].filter((product) =>
                  `${product.name} ${product.category}`.toLowerCase().includes(query.toLowerCase()))
                return (
                  <section className={`dashboard-feed dashboard-feed-${feed.id}`} key={feed.id} aria-labelledby={`feed-${feed.id}`}>
                    <div className="dashboard-feed-heading">
                      <span className="feed-number">{feed.number}</span>
                      <div><p className="eyebrow">{feed.eyebrow}</p><h3 id={`feed-${feed.id}`}>{feed.label}</h3></div>
                    </div>
                    {loading ? <p className="feed-loading">Finding your picks…</p> : feedProducts.length === 0
                      ? <p className="feed-loading">No finds here yet.</p>
                      : feed.id === 'new'
                        ? <div className="new-arrivals-grid">{feedProducts.slice(0, 12).map((product) => <NewArrivalCard key={product.id} product={product} isFavorite={favoriteIds.includes(product.id)} onFavorite={() => toggleFavorite(product)} onAdd={() => refreshCart(api.add(product.id), 'Added to your bag')} />)}</div>
                        : <div className="dashboard-feed-items">{feedProducts.slice(0, 4).map((product) => <DashboardProduct key={product.id} product={product} isFavorite={favoriteIds.includes(product.id)} onFavorite={() => toggleFavorite(product)} onAdd={() => refreshCart(api.add(product.id), 'Added to your bag')} />)}</div>}
                  </section>
                )
              })}
            </div>
          )}
        </section>

        <section className="local-note"><span className="local-number">L / 01</span><p>Good things travel better<br /><em>when they don’t travel far.</em></p><span className="local-detail">A London-only edit<br />of people doing it well.</span></section>
      </main>

      <footer><a className="wordmark footer-mark" href="#top">market<span>lane</span><i>.</i></a><span>Independent finds, delivered across London.</span><span>© 2025 Market Lane</span></footer>

      {cartOpen && <div className="drawer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setCartOpen(false)}><aside className="cart-drawer" aria-label="Your shopping bag"><div className="drawer-header"><div><p className="eyebrow">YOUR GOOD FINDS</p><h2>Your bag <span>({cartCount})</span></h2></div><button className="icon-button" onClick={() => setCartOpen(false)} aria-label="Close bag"><X size={20} /></button></div>{cart.items.length === 0 ? <div className="empty-bag"><ShoppingBag size={28} /><p>Your bag is taking a little breather.</p><button className="text-link" onClick={() => setCartOpen(false)}>Find something lovely <ArrowRight size={16} /></button></div> : <><div className="hold-note"><Clock3 size={16} /><span>Items held for</span><strong>{formatTime(cart.expiresAt, now)}</strong></div><div className="cart-lines">{cart.items.map((line) => <article className="cart-line" key={line.id}><img src={line.product.imageUrl} alt="" /><div className="cart-line-info"><strong>{line.product.name}</strong><span>{money(line.product.price)}</span><div className="quantity-controls"><button onClick={() => line.quantity === 1 ? refreshCart(api.remove(line.product.id), 'Removed from your bag') : refreshCart(api.update(line.product.id, line.quantity - 1), 'Bag updated')} aria-label="Decrease quantity"><Minus size={13} /></button><span>{line.quantity}</span><button onClick={() => refreshCart(api.update(line.product.id, line.quantity + 1), 'Bag updated')} aria-label="Increase quantity"><Plus size={13} /></button></div></div><button className="remove-line" onClick={() => refreshCart(api.remove(line.product.id), 'Removed from your bag')} aria-label={`Remove ${line.product.name}`}><X size={16} /></button></article>)}</div><div className="voucher-apply"><label htmlFor="cart-voucher">Use an offer</label><div className="voucher-controls"><select id="cart-voucher" aria-label="Select a voucher" value={selectedVoucherCode} onChange={(event) => { setSelectedVoucherCode(event.target.value); setAppliedVoucherCode(''); setVoucherMessage('') }}><option value="">Select an available voucher</option>{vouchers.map((voucher) => <option key={voucher.code} value={voucher.code}>{voucher.code} · {voucher.discountLabel} · min {money(voucher.minimumSpend)}</option>)}</select>{appliedVoucher ? <button className="remove-voucher" onClick={() => { setAppliedVoucherCode(''); setVoucherMessage('Offer removed.') }}>Remove</button> : <button className="apply-voucher" disabled={!selectedVoucherEligible} onClick={applySelectedVoucher}>Apply</button>}</div><p className="voucher-guidance" aria-live="polite">{selectedVoucherExpired ? 'This voucher has expired.' : selectedVoucher && cart.total < selectedVoucher.minimumSpend ? `Add ${money(selectedVoucher.minimumSpend - cart.total)} to use this offer.` : appliedVoucher ? `${appliedVoucher.code} applied. You save ${money(voucherDiscount)}.` : selectedVoucher?.description ?? 'Choose a London offer to see its minimum spend.'}</p>{voucherMessage && <p className="voucher-status" role="status">{voucherMessage}</p>}</div><div className="drawer-total"><div className="subtotal-row"><span>Subtotal</span><strong>{money(cart.total)}</strong></div>{voucherDiscount > 0 && <div className="discount-row"><span>{appliedVoucher?.code} savings</span><strong>-{money(voucherDiscount)}</strong></div>}<div className="grand-total-row"><span>Total</span><strong>{money(totalAfterVoucher)}</strong></div><p>Delivery calculated at checkout</p><button className="checkout-button" disabled>Continue to checkout <ArrowRight size={17} /></button><span className="checkout-caption">Checkout is coming soon</span></div></>}</aside></div>}
      {toast && <div className="toast"><Check size={16} />{toast}</div>}
    </div>
  )
}

function formatTime(expiresAt: string | null, now: number) {
  if (!expiresAt) return '15:00'
  const seconds = Math.max(0, Math.floor((new Date(expiresAt).getTime() - now) / 1000))
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

function DashboardProduct({ product, isFavorite, onFavorite, onAdd }: { product: Product; isFavorite: boolean; onFavorite: () => void; onAdd: () => void }) {
  const discount = product.compareAtPrice > product.price
  return (
    <article className="dashboard-product">
      <img src={product.imageUrl} alt={product.name} loading="lazy" />
      <div className="dashboard-product-info">
        <span className="product-category">{product.category}</span>
        <h4>{product.name}</h4>
        <div className="dashboard-product-price"><strong>{money(product.price)}</strong>{discount && <del>{money(product.compareAtPrice)}</del>}</div>
      </div>
      <div className="dashboard-product-actions">
        <button className={isFavorite ? 'dashboard-icon-button is-favorite' : 'dashboard-icon-button'} onClick={onFavorite} aria-label={isFavorite ? `Remove ${product.name} from liked items` : `Save ${product.name}`} title={isFavorite ? 'Remove from saved items' : 'Save for later'}><Heart size={15} fill={isFavorite ? 'currentColor' : 'none'} /></button>
        <button className="dashboard-icon-button add-to-bag" onClick={onAdd} aria-label={`Add ${product.name} to bag`} title="Add to bag"><Plus size={16} /></button>
      </div>
    </article>
  )
}

function NewArrivalCard({ product, isFavorite, onFavorite, onAdd }: { product: Product; isFavorite: boolean; onFavorite: () => void; onAdd: () => void }) {
  const discount = product.compareAtPrice > product.price
  return (
    <article className="new-arrival-card">
      <div className="new-arrival-image">
        <img src={product.imageUrl} alt={product.name} loading="lazy" />
        <span className="new-arrival-tag">JUST IN</span>
        <button className={isFavorite ? 'new-arrival-favorite is-favorite' : 'new-arrival-favorite'} onClick={onFavorite} aria-label={isFavorite ? `Remove ${product.name} from liked items` : `Save ${product.name}`} title={isFavorite ? 'Remove from saved items' : 'Save for later'}><Heart size={17} fill={isFavorite ? 'currentColor' : 'none'} /></button>
      </div>
      <div className="new-arrival-details">
        <span className="product-category">{product.category}</span>
        <h4>{product.name}</h4>
        <p>{product.description}</p>
        <div className="new-arrival-price"><strong>{money(product.price)}</strong>{discount && <del>{money(product.compareAtPrice)}</del>}</div>
        <button className="new-arrival-add" onClick={onAdd}><Plus size={16} /> Add to bag</button>
      </div>
    </article>
  )
}

export default App