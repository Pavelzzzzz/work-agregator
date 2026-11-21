FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY settings.gradle.kts ./
COPY gradlew ./
COPY gradle ./gradle
COPY backend/build.gradle.kts backend/
COPY backend/src backend/src
RUN mkdir -p frontend && touch frontend/build.gradle.kts && \
    ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/backend/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
