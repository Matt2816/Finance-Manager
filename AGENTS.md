# AGENTS.md

## Cursor Cloud specific instructions

### Architecture

Full-stack financial transactions tracker: Spring Boot 3.3.3 (Java 17+) backend + Next.js 14 frontend.

### Running services locally (without Docker)

The backend defaults to an **H2 in-memory database** (see `application.properties`), so no external database is needed for local development.

**Backend** (port 8080):
```
cd /workspace && chmod +x ./mvnw && ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.docker.compose.enabled=false"
```
The `-Dspring.docker.compose.enabled=false` flag prevents Spring Boot from trying to auto-start Docker Compose.

**Frontend** (port 3000):
```
cd /workspace/frontend && NEXT_PUBLIC_API_URL=http://localhost:8080/api npm run dev
```
`NEXT_PUBLIC_API_URL` must point to the backend; the frontend's Next.js API route (`/api/transactions`) proxies to it.

### Lint / Test / Build

| Service  | Lint | Test | Build |
|----------|------|------|-------|
| Backend  | N/A  | `./mvnw test` | `./mvnw package -DskipTests` |
| Frontend | `cd frontend && npm run lint` | N/A (no test script configured) | `cd frontend && npm run build` |

### Gotchas

- The Maven wrapper (`mvnw`) may lack execute permission after a fresh clone; run `chmod +x ./mvnw` before using it.
- The frontend Zod schema (`frontend/src/components/data/schema.ts`) requires `label` and `address` fields on transactions, but the backend does not return them. This causes a Zod validation error on the dashboard when transactions are loaded. To work around it during development, you can make those fields optional in the schema.
- H2 is in-memory; data is lost when the backend restarts.
