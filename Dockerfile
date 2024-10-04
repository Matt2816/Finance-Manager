# Backend build stage
FROM maven:3.8.5-openjdk-17-slim AS backend-builder

WORKDIR /app/backend
COPY pom.xml .

# Download project dependencies
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Frontend build stage
FROM node:18-alpine AS frontend-builder

WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
RUN npm run build

# Final stage
FROM amazoncorretto:17-alpine

# Copy backend JAR
COPY --from=backend-builder /app/backend/target/*.jar /app/application.jar

# Copy frontend build
COPY --from=frontend-builder /app/frontend/.next /app/frontend/.next
COPY --from=frontend-builder /app/frontend/public /app/frontend/public
COPY --from=frontend-builder /app/frontend/package*.json /app/frontend/

# Install Node.js and npm
RUN apk add --update nodejs npm

# Set working directory
WORKDIR /app

# Install production dependencies for frontend
WORKDIR /app/frontend
RUN npm ci --only=production

# Set working directory back to /app
WORKDIR /app

# Expose ports for both backend and frontend
EXPOSE 8080 3000

# Start both applications
CMD ["sh", "-c", "java -jar /app/application.jar & cd frontend && npm start"]