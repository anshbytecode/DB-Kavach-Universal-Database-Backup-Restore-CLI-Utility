# Multi-stage Docker build for DB-Kavach Banking Management Platform

# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY frontend ./frontend
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/db-backup-cli-1.0.0.jar app.jar

EXPOSE 8080

# Use JVM system property for dynamic PORT binding without breaking Picocli CLI argument parsing
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
