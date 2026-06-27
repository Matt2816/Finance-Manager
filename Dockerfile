# Backend build stage
FROM maven:3.8.5-openjdk-17-slim AS backend-builder

WORKDIR /app/backend
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget \
    && addgroup -S app \
    && adduser -S app -G app

COPY --from=backend-builder /app/backend/target/*.jar /app/application.jar
RUN chown app:app /app/application.jar

USER app
WORKDIR /app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/api/health || exit 1

CMD ["java", "-jar", "/app/application.jar"]
