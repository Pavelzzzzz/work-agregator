# Stage 1: build
FROM gradle:8.5-jdk21 AS builder
WORKDIR /workspace/backend
COPY backend/build.gradle.kts ./
COPY backend/settings.gradle.kts ./
COPY backend/gradle ./gradle
COPY backend/src ./src
- Build the app
RUN gradle bootJar --no-daemon

# Stage 2: run
FROM openjdk:21-jre-slim
WORKDIR /app
COPY --from=builder /workspace/backend/backend/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
