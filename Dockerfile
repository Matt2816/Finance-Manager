# Use Amazon Corretto as the base image
FROM openjdk:17-jdk-alpine

# Set the JAR file name as an environment variable
ARG JAR_FILE=target/*.jar

# Copy the JAR file from the build context into the Docker image
COPY ${JAR_FILE} application.jar

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/application.jar"]