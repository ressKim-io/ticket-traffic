# ==============================================================
# Multi-stage Dockerfile for all Java services
# Usage: docker build --build-arg SERVICE_NAME=auth-service .
# ==============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper & config first (better layer caching)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

# Copy common module (shared by all services except gateway)
COPY common/ common/

# Download dependencies (cached unless build.gradle changes)
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/build.gradle ${SERVICE_NAME}/build.gradle
RUN ./gradlew :${SERVICE_NAME}:dependencies --no-daemon 2>/dev/null || true

# Copy service source and build
COPY ${SERVICE_NAME}/src/ ${SERVICE_NAME}/src/
RUN ./gradlew :${SERVICE_NAME}:build -x test --no-daemon

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

# Security: non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

ARG SERVICE_NAME
COPY --from=builder /app/${SERVICE_NAME}/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

# JVM defaults - override via JAVA_OPTS env var
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
