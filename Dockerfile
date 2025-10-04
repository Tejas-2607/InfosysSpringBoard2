# Use Java 21 base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven-built JAR (assuming built with mvn package)
COPY target/test-framework-api-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]