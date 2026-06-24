# =============================================================================
# Multi-stage Dockerfile for spring-boot-performence-test
# Runtime: Eclipse Temurin JRE 21 (Alpine) – container-aware JVM flags applied.
# =============================================================================

# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn -B -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime

# Run as non-root for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

# Expose application and actuator port
EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport          – honour cgroup CPU/memory limits
#   -XX:MaxRAMPercentage=75.0         – use 75% of container RAM for heap
#   -XX:InitialRAMPercentage=50.0     – start with 50% to avoid over-commitment
#   -XX:+UseG1GC                      – G1GC (default for JDK 11+, explicit here)
#   -XX:+ExitOnOutOfMemoryError       – fast-fail on OOM instead of hanging
#   -XX:+HeapDumpOnOutOfMemoryError   – capture heap dump on OOM
#   -XX:HeapDumpPath=/tmp/heapdump.hprof
#   -Djava.security.egd=file:/dev/./urandom – faster SecureRandom in containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
