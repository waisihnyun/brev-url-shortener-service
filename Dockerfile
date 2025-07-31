# Multi-stage Dockerfile for Spring Boot application

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install required packages
RUN apk add --no-cache curl

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn/ .mvn/
COPY pom.xml .

# Make Maven wrapper executable and verify it works
RUN chmod +x ./mvnw && \
    ./mvnw --version

# Download dependencies with retry logic
RUN ./mvnw dependency:resolve dependency:resolve-sources -B || \
    ./mvnw dependency:resolve dependency:resolve-sources -B || \
    ./mvnw dependency:resolve dependency:resolve-sources -B

# Copy source code
COPY src/ src/

# Build the application (skip tests for faster builds in production)
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create a non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001 -G appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
