FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy parent POM + all module sources
COPY pom.xml .
COPY pc-core     ./pc-core
COPY pc-parser   ./pc-parser
COPY pc-engine   ./pc-engine
COPY pc-ai       ./pc-ai
COPY pc-batch    ./pc-batch
COPY pc-api      ./pc-api

# Build with Maven (skip tests for Docker build speed; run tests separately)
RUN mvn -B package -pl pc-api -am -DskipTests

# ── Runtime image ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/pc-api/target/pc-api-1.0.0-SNAPSHOT.jar app.jar

# Create lucene index directory
RUN mkdir -p /app/lucene-index

EXPOSE 8080
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
