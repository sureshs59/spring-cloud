# Spring Boot WebClient Multiple Internal API Calls Demo

This is a simple runnable Spring Boot application that demonstrates two common `WebClient` patterns:

1. **Parallel internal API calls** using `Mono.zip()`
2. **Dependent internal API calls** using `flatMap()`

To keep the project easy to run, the same application exposes mock internal APIs under `/mock/*` and then calls those endpoints from the aggregation service using `WebClient`.

## Tech Stack
- Java 17
- Spring Boot 3
- Spring WebFlux
- Maven

## Project Flow

### Parallel Example
Endpoint:
`GET /api/dashboard-parallel/{userId}`

The service calls these internal APIs in parallel:
- `/mock/user/{id}`
- `/mock/orders/{userId}`
- `/mock/profile/{userId}`

Then combines the result into one JSON response.

### Dependent Example
Endpoint:
`GET /api/employee-salary-dependent/{employeeId}`

Flow:
1. Call `/mock/employee/{employeeId}`
2. Read `employeeCode` from that response
3. Call `/mock/salary/{employeeCode}`
4. Combine both into one response

## How to Run

### 1. Extract the zip
### 2. Open terminal in project folder
### 3. Run the app

```bash
mvn spring-boot:run
```

Or build a jar:

```bash
mvn clean package
java -jar target/webclient-multiple-api-demo-1.0.0.jar
```

## Test the APIs

### Parallel call example
```bash
curl http://localhost:8080/api/dashboard-parallel/1
```

Expected response:
```json
{
  "user": {
    "id": 1,
    "name": "Suresh",
    "email": "suresh@test.com"
  },
  "orders": [
    {
      "orderId": 101,
      "productName": "Laptop",
      "amount": 75000.0
    }
  ],
  "profile": {
    "userId": 1,
    "city": "Detroit",
    "membership": "Gold"
  }
}
```

### Dependent call example
```bash
curl http://localhost:8080/api/employee-salary-dependent/1001
```

Expected response:
```json
{
  "employee": {
    "employeeId": 1001,
    "employeeCode": "EMP-1001",
    "department": "Engineering"
  },
  "salary": {
    "employeeId": 9001,
    "baseSalary": 120000.0,
    "bonus": 15000.0
  }
}
```

## Key Learning Points

### Parallel calls
Use `Mono.zip()` when multiple internal APIs can run at the same time.

Example:
```java
Mono.zip(userMono, ordersMono, profileMono)
```

### Dependent calls
Use `flatMap()` when the second API depends on the first API response.

Example:
```java
getEmployee(employeeId)
    .flatMap(employee -> getSalary(employee.employeeCode()))
```

## Where to Look in the Code
- `MockInternalApiController` -> fake internal services
- `AggregationService` -> WebClient logic
- `AggregationController` -> public endpoints
- `WebClientConfig` -> WebClient bean

## Notes
- This is a learning/demo project
- In real microservices, these internal APIs would usually be separate services
- You can easily extend this by adding timeout, retry, fallback, and circuit breaker with Resilience4j
