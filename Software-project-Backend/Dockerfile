# Multi-stage build for Spring Boot application
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Security: Run as non-root user
RUN groupadd --gid 1001 app && useradd --uid 1001 --gid app --create-home app

COPY --from=builder --chown=app:app /app/target/*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", \
    "-Dspring.profiles.active=prod", \
    "-Dserver.port=8080"]
