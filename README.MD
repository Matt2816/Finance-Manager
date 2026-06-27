# Finance Manager

A full-stack personal finance tracker: a **Spring Boot** API for transactions, income, expenses, and statement uploads, plus a **Next.js** dashboard for charts and tables.

## Stack

| Layer    | Technology                                      |
| -------- | ----------------------------------------------- |
| Backend  | Java 17, Spring Boot 3.3.3, Spring Data JPA     |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS  |
| Database | PostgreSQL (Docker) · H2 in-memory (local `mvnw`) |

## Prerequisites

Choose one path:

**Run everything with Docker**

- [Docker](https://www.docker.com/get-started) and Docker Compose v2

**Run locally (backend + frontend in separate terminals)**

- Java 17
- Node.js **20.9+** (required by Next.js 16)
- Maven is optional — the repo includes `./mvnw`

## Repository layout

```
├── src/                 # Spring Boot backend
├── frontend/            # Next.js app (see frontend/README.md)
├── Dockerfile           # Builds backend + frontend into one image
├── docker-compose.yml   # App + PostgreSQL
├── pom.xml
└── .env                 # Required for Docker (create from template below)
```

## Environment variables

Create a `.env` file in the project root before running Docker Compose. Example (change passwords for anything non-local):

```env
POSTGRES_USER=finance
POSTGRES_PASSWORD=finance
POSTGRES_DB=finance

SPRING_DATASOURCE_URL=jdbc:postgresql://financial-db:5432/finance
SPRING_DATASOURCE_USERNAME=finance
SPRING_DATASOURCE_PASSWORD=finance

# Required once auth is implemented (see docs/auth-plan.md)
JWT_SECRET=change-me-to-a-random-string-at-least-32-characters
JWT_EXPIRATION_MS=86400000
```

| Variable | Required | Default | Purpose |
| -------- | -------- | ------- | ------- |
| `JWT_SECRET` | Yes (with auth) | — | HS256 signing key for JWT tokens (≥32 characters) |
| `JWT_EXPIRATION_MS` | No | `86400000` (24h) | Access token lifetime in milliseconds |

`financial-db` is the PostgreSQL service hostname **inside** the Docker network. The app container also receives `BACKEND_API_URL=http://127.0.0.1:8080/api` so the Next.js server can reach the API in the same container.

## Run with Docker (recommended)

From the repository root:

```bash
docker compose up --build
```

This starts:

1. **PostgreSQL** on port `5432`
2. **financial-app** — Spring Boot on `8080` and Next.js (`npm start`) on `3000`

| URL | Purpose |
| --- | ------- |
| http://localhost:3000 | Dashboard UI |
| http://localhost:8080/api | Spring Boot REST API |

Stop with `Ctrl+C`, or run detached with `docker compose up --build -d`.

## Local development

Use two terminals when you want hot reload on the frontend and a JVM-backed API without rebuilding the Docker image.

### 1. Backend

From the repository root:

```bash
./mvnw spring-boot:run
```

Default config in `src/main/resources/application.properties` uses an **H2 in-memory** database and enables the H2 console. The API is at http://localhost:8080/api.

### 2. Frontend

See [frontend/README.md](frontend/README.md) for setup. In short:

```bash
cd frontend
npm install
```

Create `frontend/.env.local`:

```env
BACKEND_API_URL=http://127.0.0.1:8080/api
```

Then:

```bash
npm run dev
```

Open http://localhost:3000. The UI loads data through Next.js route `/api/transactions`, which proxies to the Spring endpoint `GET /api/transaction`.

## API overview

All backend routes are under `/api`:

| Path | Description |
| ---- | ----------- |
| `GET/POST /api/transaction` | List and create transactions |
| `POST /api/transaction/import/wallet-notes` | Import Apple Wallet notes (`testData.txt` format) |
| `POST /api/transaction/import/wallet-note` | Import one Wallet note (plain text, for Shortcuts) |
| `POST /api/transaction/import/wallet-note-json` | Import one Wallet note as JSON (`name`, `merchant`, `amount`, `date`, `location`) |
| `POST /api/transaction/import/wallet-notes-batch` | Batch import a JSON array of wallet-note items (drains PWA/Shortcuts offline queues; returns per-item status). Optional `categoryId` per item. |
| `GET /api/health` | Simple API health check (browser-friendly) |
| `GET/POST /api/transaction/test` | Test connectivity from your phone (POST does not save unless `?save=true`) |
| `GET/POST /api/income` | Recurring income |
| `GET/POST /api/expense` | Recurring expenses |
| `POST /api/statement/upload` | Parse uploaded statements (CSV, Excel, PDF) |
| `GET /api/insights` | Cached lifestyle spending insights |
| `GET /api/insights/trends` | Cached trend analysis (moving averages, seasonality) |
| `GET /api/forecasts` | Cached spending forecasts (`scope`, `horizon`) |
| `GET /api/forecasts/category/{slug}` | Forecasts for a spending category |
| `GET /api/analytics/status` | Last analytics refresh job status |
| `POST /api/analytics/refresh` | Trigger analytics refresh (async) |
| `GET /api/analytics/income-summary?from=&to=` | Income, expenses, net savings, savings rate |
| `DELETE /api/categories/{id}` | Delete category (re-assigns tx to Uncategorized) |
| `GET /api/recurring` | List active recurring transactions |
| `GET /api/recurring?direction=CREDIT|DEBIT` | Filter recurring by direction |
| `GET /api/recurring/{id}` | Get recurring transaction by ID |
| `POST /api/recurring` | Create recurring transaction (auto-backfills) |
| `PUT /api/recurring/{id}` | Update recurring transaction |
| `DELETE /api/recurring/{id}` | Soft delete (pause) recurring transaction |
| `POST /api/recurring/{id}/resume` | Resume paused recurring transaction |
| `POST /api/recurring/{id}/generate-now` | Force generate child transactions now |

## Import Apple Wallet notes

If you save Wallet transactions to a text file (see `testData.txt` in the repo root), import them while the backend is running:

```bash
curl -X POST http://localhost:8080/api/transaction/import/wallet-notes \
  -F "file=@testData.txt"
```

The response reports how many rows were imported, skipped (no amount), or skipped as duplicates. Re-running the same file is safe — existing hashes are not duplicated.

## Apple Shortcut (Tailscale)

Forward each purchase to the API with the **same text** your Shortcut already builds for Notes (one block: Name, Merchant, Amount, Date, Location).

**Endpoint:** `POST /api/transaction/import/wallet-note`  
**Content-Type:** `text/plain`  
**Body:** the note text (not JSON)

Replace `YOUR_TAILSCALE_IP` with your Mac or server’s Tailscale address (e.g. `100.x.x.x`):

```text
http://YOUR_TAILSCALE_IP:8080/api/transaction/import/wallet-note
```

**Use `http://`, not `https://`.** This API does not speak TLS on port 8080. If your Shortcut uses `https://`, iOS fails with a TLS / secure connection error because nothing is offering a certificate on that port.

Find your Tailscale IP on the Mac: `tailscale ip -4`

### Shortcuts setup

1. In your existing Wallet Shortcut, after you build the note text variable, add **Get Contents of URL**.
2. **URL:** `http://YOUR_TAILSCALE_IP:8080/api/transaction/import/wallet-note`
3. **Method:** POST
4. **Headers:** `Content-Type` = `text/plain`
5. **Request Body:** Text → select your note text variable.
6. Keep your “append to Notes” step if you want a backup.

**Responses**

| HTTP | `status`   | Meaning                          |
| ---- | ---------- | -------------------------------- |
| 201  | `created`  | Saved to the database            |
| 409  | `duplicate`| Same transaction hash already exists |
| 422  | `skipped`  | No valid amount (e.g. card tap only) |

Example with curl over Tailscale:

```bash
curl -X POST "http://YOUR_TAILSCALE_IP:8080/api/transaction/import/wallet-note" \
  -H "Content-Type: text/plain" \
  --data-binary $'Name: Wendys 6758\nMerchant: Wendys 6758\nAmount: $18.06\nDate: June 27, 2025 at 8:37:02 PM EDT\nLocation: 370 King St W\nToronto ON M5V 1J9\nCanada'
```

Docker must expose port `8080` on the Tailscale host. Rebuild after pulling this change: `docker compose up --build -d`.

### Test from your phone

**1. Browser (GET)** — confirm Tailscale reachability:

```text
http://YOUR_TAILSCALE_IP:8080/api/health
```

**2. Shortcuts (POST)** — same setup as the real import, but dry-run (nothing saved):

```text
http://YOUR_TAILSCALE_IP:8080/api/transaction/test
```

Method POST, header `Content-Type: text/plain`, body = any text (or a real Wallet note). Response JSON includes `bodyLength`, `parseable`, and a `preview` if parsing worked.

To save one real test row: use the same URL with `?save=true`.

**3. Real import** — when tests pass:

```text
http://YOUR_TAILSCALE_IP:8080/api/transaction/import/wallet-note
```

### TLS / “secure connection failed” from Shortcuts

| Mistake | Fix |
| ------- | --- |
| URL starts with `https://` | Use `http://100.x.x.x:8080/...` |
| Using port `3000` | That’s the Next.js UI — use port **8080** for the API |
| Using MagicDNS without a port | Add `:8080` and path `/api/transaction/import/wallet-note` |

Test from the phone (Safari or a one-off Shortcut step): open  
`http://YOUR_TAILSCALE_IP:8080/api/transaction` — you should see JSON (a list of transactions), not a certificate warning.

### Optional: HTTPS via Tailscale Serve

If you prefer `https://` (valid Tailscale certificate, no browser warnings), on the **machine running Docker**:

```bash
tailscale serve --bg --https=443 http://127.0.0.1:8080
```

Then in Shortcuts use your machine’s Tailscale HTTPS URL (from `tailscale serve status`), e.g.:

```text
https://your-mac.tailXXXX.ts.net/api/transaction/import/wallet-note
```

No `:8080` in the URL when using Serve on 443 — traffic is proxied to local port 8080.

## Tests

Backend only:

```bash
./mvnw test
```

The frontend has no test script configured yet.

## Clone

```bash
git clone https://github.com/Matt2816/Finance-Manager.git
cd Finance-Manager
```

## Further reading

- [frontend/README.md](frontend/README.md) — UI structure, scripts, and environment variables
- [docs/SPENDING_INSIGHTS_ARCHITECTURE.md](docs/SPENDING_INSIGHTS_ARCHITECTURE.md) — Spending insights, trends, and prediction backend
