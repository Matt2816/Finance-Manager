# Finance Manager

A full-stack personal finance tracker with a Spring Boot backend and Next.js frontend.

## Cursor Cloud specific instructions

### Services

| Service | Port | How to start |
|---------|------|-------------|
| Spring Boot API | 8080 | `./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.docker.compose.enabled=false"` |
| Next.js Frontend | 3000 | `cd frontend && npm run dev` |

### Important caveats

- **Docker Compose auto-start**: The project includes `spring-boot-docker-compose` dependency which auto-detects `docker-compose.yml` and tries to start Docker. Since Docker is not available in this environment, you must pass `--spring.docker.compose.enabled=false` when running the backend locally. Without this flag, the backend will crash on startup.
- **H2 in-memory database**: Local dev uses H2 (configured in `src/main/resources/application.properties`). No external database is needed.
- **Frontend env**: The frontend needs `frontend/.env.local` with `BACKEND_API_URL=http://127.0.0.1:8080/api` to proxy API calls to the backend.

### Commands reference

- **Backend tests**: `./mvnw test` (6 tests, uses H2)
- **Frontend lint**: `cd frontend && npm run lint` (ESLint, 0 errors expected, 1 TanStack warning is known)
- **Health check**: `curl http://localhost:8080/api/health`
- **API docs**: See `README.MD` for full endpoint listing under `/api`
