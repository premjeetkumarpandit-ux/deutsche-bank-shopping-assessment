# London Market

A focused online shopping application with a React storefront and a Spring Boot REST API. The demo is scoped to London and includes personalized picks, active vouchers, popular and new products, and a stock-reserving cart with a 15-minute hold.

## Run locally

Requirements: Node.js 20+, Java 17+, and Maven 3.9+.

Start the API from `backend`:

```powershell
mvn spring-boot:run
```

Start the UI from `frontend` in another terminal:

```powershell
npm install
npm run dev
```

Open http://localhost:5173. The API is served at http://localhost:8080/api. H2 is used for local development; the console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:shop`, user `sa`, blank password).

## Test

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm install
npm test
```

See [docs/DESIGN.md](docs/DESIGN.md) for the database, transaction, API, expiry, and distribution design.