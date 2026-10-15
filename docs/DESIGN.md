# Shopping System Design

## Scope and assumptions

The first release serves one location, London, and a demo user (`demo`). All prices are GBP. Product discovery is read-heavy, especially the popular and new feeds. Cart additions reserve inventory immediately; a reservation expires 15 minutes after the most recent add or quantity update. Checkout/payment is outside this assignment.

## Data model

- `products`: identity, descriptive fields, GBP price, stock available to reserve, popularity score, and creation date. `category` and `created_at` support browse and new-item ordering; `popularity` supports ranking.
- `favorites`: `(user_id, product_id)` unique pair. Index by `user_id` for the personalized dashboard.
- `cart_items`: user, product, quantity, and `expires_at`. Index `(user_id, expires_at)` for cart reads/expiry and `(product_id)` for inventory reconciliation. A line per user/product keeps add and update idempotent at the domain level.
- `vouchers`: production representation should include code, London eligibility, validity window, activation status, and offer terms. The demo keeps a small in-memory list to make the live-applicable behavior explicit.

Use PostgreSQL (or another transactional relational database) in production. Add foreign keys, unique constraints, and indexes listed above. Store money as `DECIMAL`, never floating point. The local H2 schema is generated from the JPA entities.

## Transactions and cart expiry

Adding a line locks its product row (`SELECT ... FOR UPDATE`), checks available stock, decrements stock, and writes/extends the cart line in one transaction. Quantity updates lock the same product and apply only the stock delta. Removing or expiring a line returns its reserved quantity to stock. An API read also expires stale lines before returning the cart, so correctness does not depend on a scheduler running on time; a periodic worker performs cleanup for abandoned carts. Every expiry operation is transactional.

For multiple service instances, use database time consistently for expiry comparisons and keep the row lock as the inventory authority. Add idempotency keys to cart mutations if clients need retry guarantees. Checkout must atomically convert held inventory into an order, verify the hold remains active, and release reservations on payment failure. A payment provider call must not be held inside a database transaction; use an order state machine/outbox for that boundary.

## HTTP API

All endpoints are under `/api`; the only supported location is `London`.

| Method | Route | Behavior |
| --- | --- | --- |
| GET | `/dashboard/liked?userId=demo` | Products favorited by the user |
| PUT / DELETE | `/favorites/{productId}?userId=demo` | Save or remove a product from favorites |
| GET | `/dashboard/vouchers?location=London` | Vouchers active now and eligible in London |
| GET | `/dashboard/popular` | Highest popularity-ranked products (up to 12) |
| GET | `/dashboard/new` | Newest products marked new (up to 12) |
| GET | `/cart?userId=demo` | Cart lines, total, and earliest expiry |
| POST | `/cart/items?userId=demo` | Body `{ "productId": 1, "quantity": 1 }`; reserve stock |
| PUT | `/cart/items/{productId}?userId=demo` | Body `{ "quantity": 2 }`; set quantity and refresh hold |
| DELETE | `/cart/items/{productId}?userId=demo` | Remove line and release stock |

Voucher responses include `discountType`, `discountValue`, and `minimumSpend`; the cart previews eligible savings from those terms. A production checkout must revalidate the code, user eligibility, validity window, and final order total on the server before accepting payment.

Invalid quantities return 400, missing products/lines return 404, and insufficient stock returns 409. Discovery responses include short-lived public cache headers; personalized responses are not shared-cacheable.

## Distribution and high-load feeds

Keep API instances stateless behind a load balancer and use a shared PostgreSQL cluster. Popular/new feeds are deterministic read models: index their ranking/order columns and cache responses briefly in a shared Redis cache or CDN. Invalidate or refresh those keys when catalog/ranking data changes. If popularity is event-driven, ingest view/purchase events asynchronously and periodically publish a precomputed top-N list; do not increment a hot product row for every view. Keep London as an explicit eligibility/filter dimension so a future expansion cannot silently leak other regions.

Cart and inventory mutations bypass discovery caches and go to the transactional database. Use connection pooling, bounded timeouts, metrics for lock waits/stock conflicts/expired holds, and horizontally scale the expiry worker with a lease or partitioned queue to prevent duplicate cleanup work. Back up the database and test restore and inventory reconciliation procedures.

## Verification

Backend tests cover cart service rules and HTTP integration for dashboard and cart mutations using an isolated H2 database. Frontend tests cover API request construction and dashboard selection. Production readiness still requires PostgreSQL concurrency/load tests, authentication/authorization, payment/checkout, observability, and deployment-specific cache/worker configuration.