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
```

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
| `GET/POST /api/income` | Recurring income |
| `GET/POST /api/expense` | Recurring expenses |
| `POST /api/statement/upload` | Parse uploaded statements (CSV, Excel, PDF) |

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
