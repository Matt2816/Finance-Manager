# Finance Manager — Frontend

Next.js dashboard for the Finance Manager monorepo. It shows transaction summaries, charts, and a filterable data table. Data is loaded from the Spring Boot API via a small Next.js API route (BFF).

## Prerequisites

- Node.js **20.9+**
- A running backend (see the [root README](../README.MD))

## Quick start

```bash
cd frontend
npm install
```

Create `.env.local` in this directory:

```env
BACKEND_API_URL=http://127.0.0.1:8080/api
```

Start the dev server:

```bash
npm run dev
```

Open http://localhost:3000.

When using **Docker** from the repo root, the root `docker-compose.yml` sets `BACKEND_API_URL` on the app container; you do not need a local `.env.local` in that setup.

## Scripts

| Command        | Description                    |
| -------------- | ------------------------------ |
| `npm run dev`  | Development server (port 3000) |
| `npm run build`| Production build               |
| `npm run start`| Serve production build         |
| `npm run lint` | ESLint                         |

## How data reaches the UI

```text
Browser  →  GET /api/transactions  →  fetchBackend("/transaction")
                                              ↓
                                    Spring GET /api/transaction
```

- **Browser**: `useTransactions` (SWR) calls `/api/transactions`.
- **Server**: `src/pages/api/transactions.ts` calls the backend using `getBackendApiUrl()` from `src/lib/backend-api.ts`.
- **Backend base URL**: `BACKEND_API_URL`, then `NEXT_PUBLIC_API_URL`, then `http://127.0.0.1:8080/api`.

Only server-side code should call the Spring API directly; the browser talks to Next.js routes under `/api/*`.

## Project structure

```text
src/
├── app/              # App Router — layout, home page, global styles
├── pages/api/        # API routes (BFF to Spring Boot)
├── components/       # Dashboard, charts, table, shadcn/ui primitives
├── hooks/            # useTransactions (SWR)
├── lib/              # backend-api, utils
└── types/            # Shared TypeScript types
```

Main entry points:

- `src/app/page.tsx` — home page with the financial dashboard
- `src/components/transaction-dashboard.tsx` — summary, charts, and table
- `src/lib/backend-api.ts` — server-side fetch helper and API base URL

## UI libraries

- [Next.js](https://nextjs.org) 16 (App Router + Pages API routes)
- [Tailwind CSS](https://tailwindcss.com)
- [shadcn/ui](https://ui.shadcn.com) (Radix primitives under `src/components/ui/`)
- [TanStack Table](https://tanstack.com/table), [Recharts](https://recharts.org), [SWR](https://swr.vercel.app)

## Production build

```bash
npm run build
npm run start
```

The root `Dockerfile` builds this app in a multi-stage image and runs `npm start` alongside the Spring Boot JAR. See the [root README](../README.MD) for Docker instructions.

## Monorepo

Backend code lives in `/src` at the repository root. For database setup, Docker, and API details, use the [root README](../README.MD).
