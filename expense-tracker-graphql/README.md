# Expense Tracker — Spring Boot GraphQL

A complete, production-ready GraphQL API for expense tracking built with Spring Boot 3, Spring GraphQL, JPA, and H2.

---

## Quick Start

```bash
# 1. Clone and run
mvn spring-boot:run

# 2. Open GraphiQL IDE in your browser
open http://localhost:8080/graphiql

# 3. Open H2 Console (to inspect the database)
open http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:expensedb   User: sa   Password: (empty)
```

---

## Project Structure

```
src/main/
├── java/com/expensetracker/
│   ├── ExpenseTrackerApplication.java     ← Main entry point
│   │
│   ├── model/
│   │   ├── Expense.java                   ← JPA Entity (@Entity)
│   │   ├── Category.java                  ← Enum (FOOD, TRANSPORT…)
│   │   └── Dtos.java                      ← All Input + Response records
│   │
│   ├── repository/
│   │   ├── ExpenseRepository.java         ← JPA Repository + custom @Query methods
│   │   └── ExpenseSpecification.java      ← Dynamic filter (JPA Criteria API)
│   │
│   ├── service/
│   │   ├── ExpenseService.java            ← All business logic
│   │   └── SubscriptionService.java       ← Reactive Flux sinks for subscriptions
│   │
│   ├── controller/
│   │   └── ExpenseController.java         ← @QueryMapping @MutationMapping @SubscriptionMapping
│   │
│   ├── exception/
│   │   ├── ExpenseNotFoundException.java  ← Custom exception
│   │   └── GlobalExceptionHandler.java    ← Converts exceptions → GraphQL errors
│   │
│   └── config/
│       ├── GraphQLConfig.java             ← CORS, custom scalars, DataLoader hooks
│       └── DataSeeder.java                ← Seeds 30 sample expenses on startup
│
└── resources/
    ├── application.properties             ← Server, H2, GraphQL, JPA config
    └── graphql/
        └── schema.graphqls                ← Full GraphQL schema definition
```

---

## Copy-Paste Queries for GraphiQL

### Queries

```graphql
# Fetch all expenses (paginated, default page 0 size 10)
query {
  expenses {
    totalElements totalPages currentPage
    content { id title amount category date description }
  }
}

# Fetch with filter + custom pagination
query {
  expenses(
    filter: { category: FOOD, minAmount: 10, maxAmount: 100 }
    pagination: { page: 0, size: 5, sortBy: AMOUNT, direction: DESC }
  ) {
    totalElements
    content { id title amount category date }
  }
}

# Single expense by ID
query {
  expense(id: "1") { id title amount category date description createdAt }
}

# Expenses by category
query {
  expensesByCategory(category: FOOD) { id title amount date }
}

# Monthly summary (bar chart data)
query {
  monthlySummaries(year: 2025) { month total count avgPerDay }
}

# Category breakdown (pie chart data)
query {
  categorySummaries(year: 2025) { category total count percentage }
}

# Total spent in a date range
query {
  totalSpent(fromDate: "2025-01-01", toDate: "2025-03-31")
}

# Predict next month's spending
query {
  predictNextMonth
}
```

### Mutations

```graphql
# Create a new expense
mutation {
  createExpense(input: {
    title:       "Team lunch"
    amount:      48.50
    category:    FOOD
    date:        "2025-03-20"
    description: "Monthly team lunch"
  }) {
    id title amount category date createdAt
  }
}

# Update specific fields only (other fields stay unchanged)
mutation {
  updateExpense(id: "1", input: { amount: 55.00 }) {
    id title amount updatedAt
  }
}

# Delete a single expense
mutation {
  deleteExpense(id: "1") { success message deletedId }
}

# Delete all expenses in a category
mutation {
  deleteByCategory(category: OTHER)
}
```

### Subscriptions (run in a second GraphiQL tab)

```graphql
# Real-time: receive every new expense as it is created
subscription {
  expenseCreated { id title amount category date }
}

# Real-time: receive every updated expense
subscription {
  expenseUpdated { id title amount category }
}

# Real-time: receive the ID of every deleted expense
subscription {
  expenseDeleted
}
```

---

## How the GraphQL → Java mapping works

| Schema element          | Java annotation         | Purpose                                       |
|-------------------------|-------------------------|-----------------------------------------------|
| `type Query { x }`     | `@QueryMapping`         | Method name must match field name             |
| `type Mutation { x }`  | `@MutationMapping`      | Method name must match field name             |
| `type Subscription { x }` | `@SubscriptionMapping` | Returns `Flux<T>` — Spring streams it over WS |
| `input Foo { ... }`    | Java record / class     | Auto-mapped from `@Argument Foo input`        |
| Field argument          | `@Argument`             | Maps a single GQL argument to a Java param    |

---

## Running Tests

```bash
mvn test
```

Tests use `HttpGraphQlTester` — a real Spring Boot integration test that fires actual GraphQL requests.

---

## Swap H2 for PostgreSQL in Production

1. Replace the H2 dependency in `pom.xml` with:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/expensedb
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

3. Remove or profile-guard `DataSeeder.java`.
