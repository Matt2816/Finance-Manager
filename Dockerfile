# First stage: Build the Spring Boot JAR
FROM maven:3.8.5-openjdk-17-slim AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .

# Download project dependencies
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Second stage: Build the final Docker image
FROM amazoncorretto:17-alpine

# Set the JAR file name as an argument
ARG JAR_FILE=target/*.jar

# Copy the JAR file from the first stage
COPY --from=builder /app/target/*.jar application.jar

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/application.jar"]
