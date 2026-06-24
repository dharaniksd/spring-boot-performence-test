# spring-boot-performence-test

A production-ready Spring Boot microservice (Java 21) with performance-tuned defaults and benchmark test APIs suitable for load testing and JVM profiling.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Profile Overview](#profile-overview)
3. [Endpoints](#endpoints)
4. [Actuator & Metrics](#actuator--metrics)
5. [Running with Docker](#running-with-docker)
6. [Load Testing](#load-testing)
7. [Tuning Knobs](#tuning-knobs)

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+

### Run locally (dev profile)

```bash
cd spring-boot-performence-test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Application starts on **http://localhost:8080**

### Run tests

```bash
mvn test
```

---

## Profile Overview

| Profile | Purpose | Tomcat max-threads | HikariCP pool |
|---------|---------|-------------------|---------------|
| `dev`   | Local development, verbose logs | 20 | 5 |
| `perf`  | Load/performance testing | 400 | 50 |
| `prod`  | Production, minimal logs | 300 | 30 |

Activate a profile:
```bash
# Maven
mvn spring-boot:run -Dspring-boot.run.profiles=perf

# JAR
java -Dspring.profiles.active=perf -jar target/spring-boot-performence-test-1.0.0-SNAPSHOT.jar

# Environment variable
SPRING_PROFILES_ACTIVE=prod java -jar target/*.jar
```

---

## Endpoints

Base URL: `http://localhost:8080`

### Test / Benchmark APIs

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/test/ping` | Fast ping – measures baseline round-trip latency |
| GET | `/api/test/cpu?iterations=100000` | CPU-bound prime-counting loop |
| GET | `/api/test/latency?ms=200` | Artificial sleep (simulates I/O latency) |
| GET | `/api/test/memory?mb=50` | Allocate & release a memory block |
| GET | `/api/test/cache/{key}` | Caffeine cache hit/miss demo |

#### Parameter limits (guardrails)

| Endpoint | Parameter | Min | Max | Default |
|----------|-----------|-----|-----|---------|
| `/cpu` | `iterations` | 1 | 10,000,000 | 100,000 |
| `/latency` | `ms` | 0 | 30,000 | 100 |
| `/memory` | `mb` | 1 | 512 | 10 |
| `/cache` | `key` (path) | 1 char | 64 chars | – |

#### Example requests

```bash
# Ping
curl -s http://localhost:8080/api/test/ping | jq

# CPU load (1M iterations)
curl -s "http://localhost:8080/api/test/cpu?iterations=1000000" | jq

# Artificial latency (500ms)
curl -s "http://localhost:8080/api/test/latency?ms=500" | jq

# Memory allocation (100 MB)
curl -s "http://localhost:8080/api/test/memory?mb=100" | jq

# Cache (first call = miss, second = hit)
curl -s http://localhost:8080/api/test/cache/mykey | jq
curl -s http://localhost:8080/api/test/cache/mykey | jq
```

---

## Actuator & Metrics

| URL | Description |
|-----|-------------|
| `GET /actuator/health` | Liveness/readiness |
| `GET /actuator/info` | App metadata |
| `GET /actuator/metrics` | Metrics list |
| `GET /actuator/metrics/http.server.requests` | HTTP latency percentiles |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |
| `GET /actuator/threaddump` | JVM thread dump |
| `GET /actuator/heapdump` | Heap dump (binary) |
| `GET /actuator/env` | Environment properties (**restrict in prod**) |
| `GET /actuator/configprops` | Config properties (**restrict in prod**) |

> ⚠️ In `prod` profile, `/actuator/env` and `/actuator/configprops` are **not exposed** by default. Review `application-prod.yml` before enabling them.

---

## Running with Docker

```bash
# Build
docker build -t spring-boot-performence-test:latest .

# Run (dev profile, 512MB container)
docker run -p 8080:8080 -m 512m \
  -e SPRING_PROFILES_ACTIVE=dev \
  spring-boot-performence-test:latest

# Run (perf profile, 2GB container)
docker run -p 8080:8080 -m 2g \
  -e SPRING_PROFILES_ACTIVE=perf \
  spring-boot-performence-test:latest
```

The JVM is configured with container-aware memory settings (`-XX:MaxRAMPercentage=75.0`), so it will automatically use ~75% of the container's RAM for heap.

---

## Load Testing

### Quick curl loop

```bash
# 100 requests to /ping, print status codes
for i in $(seq 1 100); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/test/ping
done
```

### Concurrent curl (background jobs)

```bash
for i in $(seq 1 50); do
  curl -s "http://localhost:8080/api/test/cpu?iterations=500000" > /dev/null &
done
wait
echo "All done"
```

### k6 snippet

```javascript
// k6 load test – save as load-test.js, run with: k6 run load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 50,
  duration: '30s',
};

export default function () {
  // Ping
  let r1 = http.get('http://localhost:8080/api/test/ping');
  check(r1, { 'ping 200': (r) => r.status === 200 });

  // CPU
  let r2 = http.get('http://localhost:8080/api/test/cpu?iterations=100000');
  check(r2, { 'cpu 200': (r) => r.status === 200 });

  // Latency
  let r3 = http.get('http://localhost:8080/api/test/latency?ms=50');
  check(r3, { 'latency 200': (r) => r.status === 200 });

  sleep(0.1);
}
```

Run k6:
```bash
k6 run --out influxdb=http://localhost:8086/k6 load-test.js
# or plain output:
k6 run load-test.js
```

---

## Tuning Knobs

### Tomcat Thread Pool

```yaml
server:
  tomcat:
    threads:
      max: 200          # max worker threads (increase for high concurrency)
      min-spare: 10     # always-alive threads (lower = less idle memory)
    accept-count: 100   # queue depth when all threads busy
```

### HikariCP (if a DB datasource is added)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 5000   # ms – fail fast if pool exhausted
      idle-timeout: 600000       # retire idle connections after 10 min
      max-lifetime: 1800000      # recycle connections after 30 min
```

### JVM Heap (Docker)

Adjust `MaxRAMPercentage` in `Dockerfile` or pass via environment:
```bash
docker run -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0" ...
```

### Compression

Enabled by default for JSON/text responses ≥ 1 KB:
```yaml
server:
  compression:
    enabled: true
    min-response-size: 1024   # bytes
```

### Cache TTL

Default TTL is 5 minutes. To change, modify `CacheConfig.java`:
```java
Caffeine.newBuilder()
    .maximumSize(1_000)
    .expireAfterWrite(10, TimeUnit.MINUTES)  // ← adjust here
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/dharaniksd/performence/
│   │   ├── PerformenceTestApplication.java   # Entry point
│   │   ├── controller/
│   │   │   └── PerformenceTestController.java
│   │   ├── service/
│   │   │   └── PerformenceTestService.java
│   │   ├── dto/
│   │   │   ├── ApiResponse.java
│   │   │   ├── PingResponse.java
│   │   │   ├── CpuResponse.java
│   │   │   ├── LatencyResponse.java
│   │   │   ├── MemoryResponse.java
│   │   │   ├── CacheResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── config/
│   │   │   ├── CacheConfig.java
│   │   │   └── WebConfig.java
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-perf.yml
│       └── application-prod.yml
└── test/
    └── java/com/dharaniksd/performence/
        ├── PerformenceTestServiceTest.java   # Unit tests
        └── PingIntegrationTest.java          # Integration test
```
