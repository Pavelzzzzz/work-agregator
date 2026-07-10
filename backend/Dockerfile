FROM gradle:8.5-jdk21 AS builder
WORKDIR /workspace
COPY settings.gradle.kts ./
COPY backend/build.gradle.kts backend/
COPY gradle ./gradle
COPY backend/src backend/src
RUN mkdir -p frontend && touch frontend/build.gradle.kts && \
    gradle :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/backend/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
