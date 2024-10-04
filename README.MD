# Financial App

This project is a full-stack financial application with a Spring Boot backend and a Next.js frontend.

## Prerequisites

- [Docker](https://www.docker.com/get-started) and [Docker Compose](https://docs.docker.com/compose/install/)
- [Java 17](https://www.oracle.com/java/technologies/downloads/#java17) (for local development)
- [Node.js 18+](https://nodejs.org/en/download/) (for local development)
- [Maven](https://maven.apache.org/download.cgi) (for local development)

## Repository Structure

- `/src`: Spring Boot backend code
- `/frontend`: Next.js frontend code
- `Dockerfile`: Multi-stage build for production
- `Dockerfile.dev`: Development setup for frontend
- `docker-compose.yml`: Docker Compose configuration

## Environment Setup

1. Clone the repository:

   ```
   git clone https://github.com/Matt2816/financial-app.git
   cd financial-app
   ```

2. Create a `.env` file in the root directory with the following variables:
   ```
   POSTGRES_USER=
   POSTGRES_PASSWORD=
   POSTGRES_DB=
   SPRING_DATASOURCE_URL=jdbc:postgresql://financial-db:5432/
   SPRING_DATASOURCE_USERNAME=
   SPRING_DATASOURCE_PASSWORD=
   ```

## Development

To run the application in development mode:

```
docker-compose up --build
```

This will:

- Start the PostgreSQL database
- Run the Spring Boot backend on port 8080
- Run the Next.js frontend in development mode on port 3000

Access the frontend at `http://localhost:3000` and the backend API at `http://localhost:8080/api`.

## Production

To run the application in production mode:

```
docker-compose --profile prod up --build
```

This will:

- Build the Spring Boot backend
- Build the Next.js frontend
- Run both in a single container, with the backend serving the frontend

Access the application at `http://localhost:3000`.

## Backend Development

For local backend development:

1. Navigate to the root directory
2. Run `./mvnw spring-boot:run`

The backend will be available at `http://localhost:8080`.

## Frontend Development

For local frontend development:

1. Navigate to the `/frontend` directory
2. Install dependencies: `npm install`
3. Run the development server: `npm run dev`

The frontend will be available at `http://localhost:3000`.

## Testing

- Backend tests: `./mvnw test`
- Frontend tests: `cd frontend && npm test`

## Additional Information

- The backend uses Spring Boot 3.3.3 with Java 17
- The frontend uses Next.js 14.2.14 with TypeScript
- Database: PostgreSQL
- ORM: Spring Data JPA
- API Documentation: _Work in progress_
