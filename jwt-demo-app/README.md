# JWT Authentication Demo
## Spring Boot + Angular 18 — Learn JWT Programmatically

A complete, working JWT authentication application designed for learning. Every line of code is commented to explain **what** is happening and **why**.

---

## What You Will Learn

### 1. JWT Structure
Every JWT has exactly 3 parts separated by dots:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9    ← HEADER
.eyJzdWIiOiJzdXJlc2giLCJyb2xlcyI6WyJ   ← PAYLOAD
.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_   ← SIGNATURE
```

- **Header** (Base64): `{ "alg": "HS256", "typ": "JWT" }`
- **Payload** (Base64): `{ "sub": "suresh", "roles": ["ROLE_USER"], "iat": 1234, "exp": 5678 }`
- **Signature** (HMAC-SHA256): Proves the token was issued by our server and not tampered with

### 2. The Full Authentication Flow
```
Client                              Server
  │                                   │
  │  POST /api/auth/login              │
  │  { username, password }  ─────────▶ AuthenticationManager.authenticate()
  │                                   │   → UserDetailsService.loadByUsername()
  │                                   │   → BCrypt.matches(password, hash)
  │                                   │
  │◀ ─────────────────────────────────  JwtService.generateAccessToken()
  │  { accessToken, refreshToken }    │   → Build claims (sub, roles, iat, exp, jti)
  │                                   │   → Sign with HMAC-SHA256(secret)
  │                                   │   → Compact to header.payload.signature
  │
  │  Store JWT in localStorage
  │
  │  GET /api/dashboard               │
  │  Authorization: Bearer <JWT> ─────▶ JwtAuthFilter.doFilterInternal()
  │                                   │   → Extract JWT from "Bearer " header
  │                                   │   → JwtService.extractUsername(jwt)
  │                                   │   → JwtService.isTokenValid(jwt, user)
  │                                   │   →   verifyWith(signingKey)     ← checks signature
  │                                   │   →   parseSignedClaims(jwt)     ← decodes payload
  │                                   │   →   check expiration           ← checks exp claim
  │                                   │   → SecurityContextHolder.setAuthentication()
  │                                   │
  │◀ ─────────────────────────────────  Controller returns protected data
  │  { "message": "Welcome suresh!" } │
```

### 3. Why JWT is Stateless
Traditional session authentication:
- Server stores session in memory/database
- Client sends session ID cookie
- Server looks up session on every request
- Doesn't scale horizontally (sessions on Server A not on Server B)

JWT authentication:
- Server stores NOTHING
- JWT contains everything needed (username, roles, expiry)
- Server only needs the signing key to verify
- Scales horizontally — any server can validate any JWT

### 4. Access + Refresh Token Pattern
| | Access Token | Refresh Token |
|--|--|--|
| **Lifetime** | 15 minutes | 24 hours |
| **Sent on** | Every protected request | Only when refreshing |
| **Stored in** | localStorage (or memory) | localStorage (or httpOnly cookie) |
| **Purpose** | Authenticate API calls | Get new access token without re-login |

### 5. Claims in Our Token
| Claim | Value | Purpose |
|-------|-------|---------|
| `sub` | "suresh" | Subject — who this token represents |
| `roles` | ["ROLE_USER"] | User's authorities |
| `type` | "ACCESS" | Distinguishes ACCESS from REFRESH |
| `iat` | 1714556400 | Issued At — Unix timestamp |
| `exp` | 1714557300 | Expires At — 15 min after iat |
| `jti` | "uuid-here" | JWT ID — unique ID per token |

---

## Project Structure

```
jwt-demo/
├── backend/                          ← Spring Boot
│   ├── pom.xml
│   └── src/main/java/com/jwtdemo/
│       ├── JwtDemoApplication.java   ← Entry point
│       ├── config/
│       │   └── SecurityConfig.java   ← Spring Security + JWT filter chain
│       ├── security/
│       │   ├── JwtService.java       ← Generate, validate, decode tokens
│       │   └── JwtAuthFilter.java    ← Intercepts every request
│       ├── controller/
│       │   ├── AuthController.java   ← /api/auth/* endpoints
│       │   └── ApiController.java    ← Protected endpoints
│       └── model/
│           ├── LoginRequest.java
│           └── AuthResponse.java
│
└── frontend/                         ← Angular 18
    └── src/app/
        ├── app.component.ts          ← Full JWT learning dashboard
        └── services/
            └── auth.service.ts       ← JWT storage + API calls
```

---

## Quick Start

### Step 1 — Run the Spring Boot backend
```bash
cd backend
mvn spring-boot:run

# Backend starts at http://localhost:8080
```

### Step 2 — Run the Angular frontend
```bash
cd frontend
npm install
npm start

# Frontend starts at http://localhost:4200
```

### Step 3 — Open the app
Go to **http://localhost:4200** and:
1. Click a user tag (e.g. `suresh / password`) to fill the form
2. Click "Login & Get JWT"
3. See the JWT split into its 3 decoded parts
4. Watch the 15-minute countdown timer
5. Click the API buttons to see protected endpoints in action
6. Try the admin endpoint with a non-admin user — see 403 Forbidden
7. Click "Refresh access token" before it expires

---

## API Endpoints

### Public (no JWT needed)
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"suresh","password":"password"}'

# Inspect any JWT
curl -X POST http://localhost:8080/api/auth/inspect \
  -H "Content-Type: application/json" \
  -d '{"token":"<paste-jwt-here>"}'

# Refresh access token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<paste-refresh-token>"}'
```

### Protected (requires JWT)
```bash
# Set your token first
TOKEN="<your-access-token-here>"

# Dashboard (any authenticated user)
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer $TOKEN"

# Profile (any authenticated user)
curl http://localhost:8080/api/profile \
  -H "Authorization: Bearer $TOKEN"

# Admin only (403 for non-admin users)
curl http://localhost:8080/api/admin/data \
  -H "Authorization: Bearer $TOKEN"
```

### What happens with no token?
```bash
# Without Authorization header → 401 Unauthorized
curl http://localhost:8080/api/dashboard
# {"status":401,"error":"Unauthorized"}

# With expired/tampered token → 403 Forbidden
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer invalid.token.here"
```

---

## Key Code Explanations

### How JwtService builds a token
```java
// In JwtService.buildToken():
String token = Jwts.builder()
    .claims(extraClaims)            // add roles, type, jti to PAYLOAD
    .subject(userDetails.getUsername()) // "sub" claim → PAYLOAD
    .issuedAt(new Date())           // "iat" claim → PAYLOAD
    .expiration(new Date(now + ms)) // "exp" claim → PAYLOAD
    .signWith(getSigningKey())       // HMAC-SHA256(header.payload, secret) → SIGNATURE
    .compact();                     // output: header.payload.signature
```

### How JwtAuthFilter validates a request
```java
// In JwtAuthFilter.doFilterInternal():
String jwt        = authHeader.substring(7); // strip "Bearer "
String username   = jwtService.extractUsername(jwt); // read "sub" claim
UserDetails user  = userDetailsService.loadUserByUsername(username);
boolean isValid   = jwtService.isTokenValid(jwt, user);
// isValid checks:
//   1. username matches
//   2. token is not expired (exp > now)
//   3. signature is valid (verifyWith(signingKey))
if (isValid) SecurityContextHolder.getContext().setAuthentication(authToken);
```

### How the Angular interceptor adds the token
```typescript
// In jwtInterceptor:
const token = localStorage.getItem('accessToken');
if (token) {
  req = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}
// Every HTTP request from Angular automatically gets the JWT header
```

---

## Security Notes (Production Checklist)

- [ ] Store secret in AWS Secrets Manager or Vault — never in code or config files
- [ ] Use HTTPS only — JWT in HTTP is vulnerable to interception
- [ ] Store refresh token in httpOnly cookie — not localStorage (XSS protection)
- [ ] Implement token denylist in Redis for logout invalidation
- [ ] Add rate limiting on /api/auth/login (prevent brute force)
- [ ] Rotate signing key periodically
- [ ] Log all failed authentication attempts for monitoring

---

*Built with Spring Boot 3.2 · Java 17 · JJWT 0.12.3 · Angular 18 · Spring Security*
