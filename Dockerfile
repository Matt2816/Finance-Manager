# Backend build stage
FROM maven:3.8.5-openjdk-17-slim AS backend-builder

WORKDIR /app/backend
COPY pom.xml .

# Download project dependencies
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Frontend build stage (Next.js 16 requires Node >= 20.9)
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
RUN npm run build

# Final stage: keep Node on its own Alpine image and add Java for Spring Boot
FROM node:20-alpine

RUN apk add --no-cache openjdk17-jre-headless

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

# Copy backend JAR
COPY --from=backend-builder /app/backend/target/*.jar /app/application.jar

# Copy frontend build artifacts and static public assets (manifest, icons)
COPY --from=frontend-builder /app/frontend/.next /app/frontend/.next
COPY --from=frontend-builder /app/frontend/public /app/frontend/public
COPY --from=frontend-builder /app/frontend/package*.json /app/frontend/

WORKDIR /app/frontend
RUN npm ci --omit=dev

WORKDIR /app

EXPOSE 8080 3000

CMD ["sh", "-c", "java -jar /app/application.jar & cd /app/frontend && npm start"]
