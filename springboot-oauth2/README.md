# Spring Boot OAuth2 — Local Demo

A complete OAuth2 + JWT authentication application built with Spring Boot 3, Spring Security, and H2 in-memory database.

**No database install needed. Runs out of the box.**

---

## What this project demonstrates

| Feature | Details |
|---|---|
| Local JWT auth | Register + login with email/password → get JWT |
| Google OAuth2 | Login via Google → get JWT |
| GitHub OAuth2 | Login via GitHub → get JWT |
| JWT filter | Every protected request validated automatically |
| Role-based access | ROLE_USER and ROLE_ADMIN with @PreAuthorize |
| H2 database | In-memory, no install required |
| BCrypt password | Passwords hashed, never stored plain |
| Global error handler | Consistent JSON error responses |

---

## Prerequisites

- Java 17+
- Maven 3.8+

That's it. No database, no Docker, no external services needed for local JWT testing.

---

## Running the application

```bash
# 1. Clone or unzip the project
cd springboot-oauth2

# 2. Run with Maven
mvn spring-boot:run

# Application starts at http://localhost:8080
```

---

## Two modes of operation

### Mode 1 — Local JWT (works immediately, no setup)

Use these endpoints directly with email + password. Two users are seeded automatically:

| User | Email | Password | Role |
|---|---|---|---|
| Admin | admin@example.com | admin123 | ROLE_ADMIN |
| Regular | user@example.com | user123 | ROLE_USER |

### Mode 2 — Google / GitHub OAuth2

Requires setting up OAuth2 credentials. See the OAuth2 Setup section below.

---

## API Testing — Local JWT (no OAuth2 needed)

### Step 1 — Register a new user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name":     "Suresh",
    "email":    "suresh@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType":   "Bearer",
  "expiresIn":   86400000,
  "email":       "suresh@example.com",
  "name":        "Suresh",
  "role":        "ROLE_USER"
}
```

### Step 2 — Login (get JWT)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":    "user@example.com",
    "password": "user123"
  }'
```

### Step 3 — Access protected endpoint

```bash
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Response:
```json
{
  "id":       1,
  "name":     "Test User",
  "email":    "user@example.com",
  "imageUrl": null,
  "provider": "local",
  "role":     "ROLE_USER"
}
```

### Step 4 — Admin endpoint (ROLE_ADMIN required)

```bash
# Login as admin first to get admin JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "admin123"}'

# Use admin JWT
curl http://localhost:8080/api/user/all \
  -H "Authorization: Bearer <admin-jwt-token>"
```

---

## OAuth2 Setup (Google)

### 1. Create Google OAuth2 credentials

1. Go to https://console.cloud.google.com/
2. Create a new project (or use existing)
3. APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID
4. Application type: Web application
5. Add Authorised redirect URI: `http://localhost:8080/oauth2/callback/google`
6. Copy Client ID and Client Secret

### 2. Update application.yml

Replace these values in `src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_ACTUAL_CLIENT_ID_HERE
            client-secret: YOUR_ACTUAL_CLIENT_SECRET_HERE
```

### 3. Test Google OAuth2 login

Open in browser:
```
http://localhost:8080/oauth2/authorize/google
```

This redirects to Google login. After authentication, you are redirected to:
```
http://localhost:8080/oauth2/success?token=eyJhb...
```

Copy the token from the response and use it in the Authorization header.

---

## OAuth2 Setup (GitHub)

1. GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
2. Homepage URL: `http://localhost:8080`
3. Authorization callback URL: `http://localhost:8080/oauth2/callback/github`
4. Copy Client ID and Client Secret into `application.yml`

Test:
```
http://localhost:8080/oauth2/authorize/github
```

---

## H2 Console (view the database)

Open in browser:
```
http://localhost:8080/h2-console
```

Settings:
- JDBC URL: `jdbc:h2:mem:oauth2db`
- Username: `sa`
- Password: (empty)

You can view the USERS table and see all registered users.

---

## Project structure

```
src/main/java/com/example/oauth2/
├── OAuth2Application.java          ← entry point
│
├── config/
│   ├── SecurityConfig.java         ← main security config (JWT + OAuth2)
│   ├── GlobalExceptionHandler.java ← unified error responses
│   └── DataInitializer.java        ← seeds admin + test user on startup
│
├── controller/
│   ├── AuthController.java         ← POST /api/auth/register, /login
│   ├── UserController.java         ← GET /api/user/me, /all
│   └── OAuth2SuccessController.java← GET /oauth2/success
│
├── model/
│   ├── User.java                   ← JPA entity (local + OAuth2 users)
│   ├── AuthProvider.java           ← enum: local, google, github
│   └── Role.java                   ← enum: ROLE_USER, ROLE_ADMIN
│
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   └── UserProfileResponse.java
│
├── security/
│   ├── JwtTokenProvider.java           ← generate + validate JWT
│   ├── JwtAuthenticationFilter.java    ← intercepts requests, validates JWT
│   ├── CustomUserDetailsService.java   ← loads user from DB for JWT auth
│   ├── CustomOAuth2UserService.java    ← handles OAuth2 login, saves user
│   ├── OAuth2AuthenticationSuccessHandler.java ← generates JWT after OAuth2
│   ├── OAuth2UserInfo.java             ← abstract user info from provider
│   └── OAuth2UserInfoProviders.java    ← Google + GitHub implementations
│
└── service/
    ├── AuthService.java            ← register + login business logic
    └── UserRepository.java         ← Spring Data JPA repository
```

---

## Authentication flow

### Local JWT flow

```
Client                    Server
  │                          │
  ├─ POST /api/auth/login ──►│
  │   { email, password }    │
  │                          ├─ validate credentials (BCrypt)
  │                          ├─ generate JWT (signed with secret)
  │◄─ { accessToken, ... } ──┤
  │                          │
  ├─ GET /api/user/me ──────►│
  │   Authorization: Bearer  │
  │                          ├─ JwtAuthenticationFilter validates token
  │                          ├─ loads UserDetails from DB
  │                          ├─ sets SecurityContext
  │◄─ { user profile } ──────┤
```

### OAuth2 flow

```
Browser                   Server                  Google
  │                          │                        │
  ├─ GET /oauth2/authorize/google ──────────────────►│
  │◄── redirect to Google consent screen ────────────┤
  │                          │                        │
  │  user logs in on Google  │                        │
  │◄── redirect to /oauth2/callback/google?code=... ──┤
  │                          │                        │
  │                 Spring Security exchanges code for token
  │                 CustomOAuth2UserService saves user to DB
  │                 OAuth2SuccessHandler generates JWT
  │◄── redirect to /oauth2/success?token=eyJhb... ────┤
```

---

## Postman collection (import this)

Save as `oauth2-demo.postman_collection.json` and import into Postman:

```json
{
  "info": { "name": "Spring Boot OAuth2 Demo" },
  "item": [
    {
      "name": "Register",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/auth/register",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": { "mode": "raw", "raw": "{\"name\":\"Suresh\",\"email\":\"suresh@test.com\",\"password\":\"password123\"}" }
      }
    },
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/auth/login",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": { "mode": "raw", "raw": "{\"email\":\"user@example.com\",\"password\":\"user123\"}" }
      }
    },
    {
      "name": "Get My Profile",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/user/me",
        "header": [{ "key": "Authorization", "value": "Bearer {{token}}" }]
      }
    },
    {
      "name": "Get All Users (Admin)",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/user/all",
        "header": [{ "key": "Authorization", "value": "Bearer {{adminToken}}" }]
      }
    }
  ]
}
```

---

*Built with Java 17 · Spring Boot 3.2 · Spring Security · JJWT · H2*
