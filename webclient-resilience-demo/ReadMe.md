# Spring Boot WebClient Resilience Demo

> A Spring Boot 3 project demonstrating **non-blocking HTTP communication** with WebClient, parallel and dependent API calls, and production-grade resilience patterns using Resilience4j.

Designed for learning, interview preparation, and GitHub portfolio use.

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.x |
| Spring WebFlux | included |
| WebClient | included |
| Resilience4j | latest |
| Build Tool | Maven |

---

## Features

### 1. Parallel API calls

Calls multiple independent APIs simultaneously and combines results into a single response.

```
Endpoints called in parallel:
  GET /mock/user/{id}
  GET /mock/orders/{id}
  GET /mock/profile/{id}
```

**Key operator:** `Mono.zip()` — fires all calls at the same time, waits for all to complete.

---

### 2. Dependent API calls

Calls one API first, extracts data from the response, then uses it to trigger further calls.

**Key operators:** `flatMap()` → `Mono.zip()`

---

### 3. Resilience patterns

Handles downstream failures gracefully using Resilience4j:

| Pattern | What it does |
|---|---|
| **Retry** | Automatically retries failed requests |
| **Circuit Breaker** | Stops calling an unhealthy service after repeated failures |
| **Timeout** | Fails fast if the downstream takes too long |
| **Fallback** | Returns a safe default response when all else fails |

---

## Request Flows

### Parallel flow — `GET /api/parallel/1`

```
Client
  └── /api/parallel/1
        ├── GET /mock/user/{id}     ─┐
        ├── GET /mock/orders/{id}   ─┤ fired simultaneously via Mono.zip()
        └── GET /mock/profile/{id} ─┘
              └── combined into a single DashboardResponse
```

### Dependent flow — `GET /api/dependent/1`

```
Client
  └── /api/dependent/1
        └── GET /mock/user/{id}          (step 1)
              └── extract user.id
                    ├── GET /mock/orders/{id}   ─┐ fired in parallel
                    └── GET /mock/profile/{id}  ─┘ via Mono.zip()
                          └── combined into DashboardResponse
```

### Fallback flow — `GET /api/profile-fallback/1`

```
Client
  └── /api/profile-fallback/1
        └── GET /mock/profile-fail/{id}
              ├── retry (automatic)
              ├── circuit breaker check
              └── fallback response returned
```

---

## Project Structure

```
src/main/java/com/example/webclientresiliencedemo
├── config
│   └── WebClientConfig.java          # WebClient bean configuration
├── controller
│   ├── MainController.java           # /api/* endpoints
│   └── MockController.java           # /mock/* endpoints (internal mocks)
├── model
│   ├── DashboardResponse.java        # aggregated response model
│   ├── Order.java
│   ├── Profile.java
│   └── User.java
├── service
│   └── AggregationService.java       # core orchestration logic
└── WebclientResilienceDemoApplication.java
```

---

## Endpoints

### Main API endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/parallel/{id}` | Parallel call — user + orders + profile |
| `GET` | `/api/dependent/{id}` | Dependent call — user → orders + profile |
| `GET` | `/api/profile-fallback/{id}` | Resilience demo with fallback |

### Mock API endpoints (internal)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/mock/user/{id}` | Returns a mock user |
| `GET` | `/mock/orders/{id}` | Returns mock orders |
| `GET` | `/mock/profile/{id}` | Returns a mock profile |
| `GET` | `/mock/profile-fail/{id}` | Always fails (for resilience testing) |

---

## Sample Responses

### `GET /api/parallel/1`

```json
{
  "user": {
    "id": 1,
    "name": "Suresh"
  },
  "orders": [
    { "orderId": 1, "product": "Laptop" },
    { "orderId": 2, "product": "Mouse" }
  ],
  "profile": {
    "city": "Detroit"
  }
}
```

### `GET /api/profile-fallback/1`

```json
{
  "city": "Default City"
}
```

---

## Key Concepts

| Concept | Purpose |
|---|---|
| `WebClient` | Non-blocking HTTP client for internal service communication |
| `Mono.zip()` | Fires multiple independent calls in parallel, combines results |
| `flatMap()` | Chains calls where the next call depends on the previous response |
| `Retry` | Retries failed downstream requests with configurable attempts |
| `Circuit Breaker` | Prevents repeated calls to an unhealthy service |
| `Timeout` | Fails fast if a downstream call exceeds the time limit |
| `Fallback` | Returns a safe default instead of propagating the failure |

---

## Architecture

```
Client
  │
  ▼
MainController  (/api/*)
  │
  ▼
AggregationService
  │
  ├──── WebClient ──── User API       (/mock/user)
  ├──── WebClient ──── Orders API     (/mock/orders)
  ├──── WebClient ──── Profile API    (/mock/profile)
  └──── WebClient ──── Failing API    (/mock/profile-fail)
                            │
                      Resilience4j
                      ├── Retry
                      ├── Circuit Breaker
                      ├── Timeout
                      └── Fallback
```

---

## Getting Started

```bash
# Clone the repository
git clone https://github.com/your-username/webclient-resilience-demo.git

# Navigate to project directory
cd webclient-resilience-demo

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## Author

**Suresh Sunuguri**  
Senior Software Engineer | Java · Spring Boot · Microservices

---

> If you found this project useful, please consider giving it a star on GitHub!
