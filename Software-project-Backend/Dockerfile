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

# Back4app's free container has 256 MB RAM. These conservative defaults keep
# the JVM within that limit and can still be overridden by the platform.
ENV JAVA_TOOL_OPTIONS="-Xms16m -Xmx64m -XX:MaxMetaspaceSize=84m -XX:CompressedClassSpaceSize=16m -XX:ReservedCodeCacheSize=10m -XX:MaxDirectMemorySize=4m -Xss320k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
