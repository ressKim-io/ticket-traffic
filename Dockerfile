FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle settings.gradle ./
COPY common/ common/

ARG SERVICE_NAME
COPY ${SERVICE_NAME}/ ${SERVICE_NAME}/

RUN chmod +x gradlew && \
    ./gradlew :${SERVICE_NAME}:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG SERVICE_NAME
COPY --from=builder /app/${SERVICE_NAME}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
