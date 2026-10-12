# Multi-stage Docker build for SplitFlow AI (Java 21 + Spring Boot 3.3.4)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN apk add --no-cache maven && mvn clean package -DskipTests

# Runtime stage (Minimal JRE image for ultra-low RAM footprint)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

# JVM options optimized for free tier instances (512MB RAM)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
