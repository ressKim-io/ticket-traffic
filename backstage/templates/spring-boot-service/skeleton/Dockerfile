# Multi-stage build for ${{ values.serviceName }}-service
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :${{ values.serviceName }}-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
RUN groupadd -r app && useradd -r -g app -d /app app
WORKDIR /app
COPY --from=builder /build/${{ values.serviceName }}-service/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE ${{ values.port }}
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "-jar", "app.jar"]
