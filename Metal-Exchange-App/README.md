# 🥇 Gold & Silver Exchange Rate App
### Angular 18 + Spring Boot Microservices + PostgreSQL + MetalPriceAPI

A complete real-time gold and silver rate dashboard for Indian users.
Live INR rates fetched from MetalPriceAPI, stored in Neon.tech PostgreSQL,
displayed in an Angular 18 dashboard with Chart.js line charts.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          FRONTEND (port 4200)                       │
│    Angular 18 · Signals · Chart.js · Standalone Components         │
│    Dashboard · Rate Cards · History Table · Line Chart             │
└────────────────────────┬────────────────────────────────────────────┘
                         │ HTTP /api/*
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    RATE FETCHER SERVICE (port 8081)                 │
│    Spring Boot · JPA · @Scheduled · WebClient                     │
│    Fetches from MetalPriceAPI daily · Stores to PostgreSQL         │
│    Endpoints: /api/rates/today  /gold/latest  /history  /all       │
└────────────────────────┬────────────────────────────────────────────┘
                         │ JPA / JDBC
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   PostgreSQL (Neon.tech — free)                     │
│    Table: metal_rates                                               │
│    Rows:  one per metal per day (GOLD | SILVER)                    │
│    Persists forever · 500 MB free · No install needed              │
└─────────────────────────────────────────────────────────────────────┘
                         ▲
┌─────────────────────────────────────────────────────────────────────┐
│                   MetalPriceAPI (external)                         │
│    https://api.metalpriceapi.com/v1/latest                        │
│    Returns: XAU (gold ounce INR), XAG (silver ounce INR)          │
│    Free tier: 100 requests/day · No credit card needed             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start — 3 steps

### Step 1 — Get your free MetalPriceAPI key

1. Go to **https://metalpriceapi.com**
2. Click "Get Free API Key"
3. Sign up with email (no credit card)
4. Copy your API key from the dashboard

### Step 2 — Set up free PostgreSQL on Neon.tech

1. Go to **https://neon.tech**
2. Sign up free (GitHub login works)
3. Create a new project → "goldexchange"
4. Click "Connection Details" → copy:
   - Host: `ep-xxx.us-east-2.aws.neon.tech`
   - Database: `neondb`
   - Username: your username
   - Password: your password

### Step 3 — Configure and run

**Edit** `rate-fetcher-service/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require
    username: YOUR_NEON_USERNAME
    password: YOUR_NEON_PASSWORD

metal-price-api:
  api-key: YOUR_METALPRICEAPI_KEY
```

**Run the backend:**
```bash
cd rate-fetcher-service
mvn spring-boot:run
```

**Run the frontend:**
```bash
cd frontend
npm install
npm start
# Opens at http://localhost:4200
```

---

## What it does

### Dashboard page (`/dashboard`)
- **Live rate cards** — Gold and Silver current price per gram and per 10g in INR
- **Trend badges** — ▲ green / ▼ red showing daily change with percentage
- **Day high/low range bar** — visual indicator of where current price sits
- **Gold/Silver ratio** — live computed ratio
- **Interactive line chart** — dual-axis Chart.js showing both metals
- **Time range selector** — 7D / 30D / 90D / 1Y
- **Recent rates table** — last 10 days with change columns
- **Auto-refresh** — live rates poll every 5 minutes

### History page (`/history`)
- **Complete data table** — all stored rates with sorting
- **Filter by metal** — ALL / GOLD / SILVER
- **Sortable columns** — click any column header to sort
- **Change analysis** — daily INR and % change per row

### Backend auto-scheduling
- **10:00 AM IST daily** — main rate fetch
- **Every 4 hours (Mon-Fri)** — intraday update  
- **On startup** — immediate fetch so app has data from day 1

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/rates/today` | Today's gold + silver (JSON) |
| GET | `/api/rates/gold/latest` | Latest gold rate |
| GET | `/api/rates/silver/latest` | Latest silver rate |
| GET | `/api/rates/gold/history?days=30` | Gold history |
| GET | `/api/rates/silver/history?days=30` | Silver history |
| GET | `/api/rates/all?days=30` | Both metals, all days |
| POST | `/api/rates/fetch` | Manual trigger (testing) |

---

## Project Structure

```
gold-exchange/
├── rate-fetcher-service/           ← Spring Boot backend
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/goldexchange/fetcher/
│       │   ├── RateFetcherApplication.java
│       │   ├── model/MetalRate.java        ← JPA entity
│       │   ├── repository/MetalRateRepository.java
│       │   ├── service/RateFetcherService.java ← core logic
│       │   ├── scheduler/RateScheduler.java    ← cron jobs
│       │   ├── controller/RateController.java  ← REST API
│       │   ├── dto/MetalPriceApiResponse.java
│       │   └── config/WebClientConfig.java
│       └── resources/application.yml
│
└── frontend/                       ← Angular 18 frontend
    ├── package.json
    ├── proxy.conf.json              ← dev proxy to backend
    └── src/app/
        ├── app.component.ts
        ├── app.config.ts
        ├── app.routes.ts
        ├── models/metal-rate.model.ts
        ├── services/rate.service.ts     ← Signals + HTTP
        └── components/
            ├── dashboard/dashboard.component.ts  ← main page
            ├── rate-card/rate-card.component.ts  ← price card
            ├── header/header.component.ts         ← nav + ticker
            └── history/history.component.ts       ← data table
```

---

## Technical Highlights

### Angular 18 features used
- **Signals** — `signal()`, `computed()`, `effect()` for reactive state
- **`toSignal()`** — converts HTTP Observable to Signal seamlessly
- **`@for` with track** — performant list rendering
- **`@if / @else`** — new control flow syntax
- **`@defer`** — lazy-loaded route components
- **Standalone components** — no NgModule, self-contained imports
- **`inject()`** — modern dependency injection

### Spring Boot features used
- **`@Scheduled`** — cron-based daily rate fetching
- **Spring Data JPA** — repository pattern with custom JPQL queries
- **WebClient** — reactive HTTP client for API calls
- **`@Transactional`** — idempotent save (skips if today's rate exists)
- **`@CrossOrigin`** — CORS for Angular dev server

### Data model
- One row per metal per date — `(rateDate, metal)` unique constraint
- Prices stored: per gram, per 10g, per ounce (all INR)
- Change vs previous day calculated on every insert
- `1 troy ounce = 31.1035 grams` conversion constant

---

## Free Tier Summary

| Service | Free Limit | Link |
|---------|-----------|------|
| MetalPriceAPI | 100 requests/day | metalpriceapi.com |
| Neon.tech PostgreSQL | 500 MB, unlimited time | neon.tech |
| Angular (local) | Free forever | angular.io |
| Spring Boot (local) | Free forever | spring.io |

100 requests/day is plenty — the scheduler only calls the API 3 times/day.

---

## Troubleshooting

**"Fetching live rates..." never goes away**
→ Check your MetalPriceAPI key in `application.yml`
→ Try: `curl http://localhost:8081/api/rates/today`
→ Check logs: did the startup fetch succeed?

**Database connection error**
→ Verify Neon.tech connection string includes `?sslmode=require`
→ Make sure username/password are correct

**CORS error in browser**
→ Make sure you're using `npm start` (which uses `proxy.conf.json`)
→ Do not call `localhost:8081` directly from Angular — use `/api/`

**No historical data for chart**
→ App needs a few days of running to build up history
→ For testing, POST to `/api/rates/fetch` to trigger a manual fetch

---

*Built with Angular 18 · Spring Boot 3.2 · Chart.js 4 · PostgreSQL (Neon.tech) · MetalPriceAPI*
