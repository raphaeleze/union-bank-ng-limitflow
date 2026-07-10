# LimitFlow Backend API

Spring Boot 3 / Java 21 backend for the transfer-limit increase journey described in the
[repository root README](../../README.md). Implements Clean Architecture (domain →
application → infrastructure → presentation), JWT auth, a strategy-pattern risk engine, and
full audit logging.

## Stack

Java 21 · Spring Boot 3 · Spring Security · Spring Data JPA · PostgreSQL · Flyway · JWT
(jjwt) · Lombok · springdoc-openapi (Swagger UI)

## Run locally

```bash
cd docker
docker compose up --build
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

Or run against your own Postgres instance:

```bash
cd apps/backend-api
DB_URL=jdbc:postgresql://localhost:5432/limitflow \
DB_USERNAME=limitflow \
DB_PASSWORD=limitflow \
./mvnw spring-boot:run
```

Flyway applies the schema and seed data automatically on startup.

## Seed accounts

All seeded users share the password `Password123!`.

| Role | Email |
|---|---|
| Customer | `customer@limitflow.demo` |
| Support agent | `support@limitflow.demo` |
| Manager | `manager@limitflow.demo` |

The seeded customer starts with a ₦200,000 daily limit, ₦180,000 used today, and an
active ₦500,000 increase request already sitting in the support queue (MEDIUM risk).

## Tests

```bash
./mvnw test
```

- Unit tests (`RiskEngineTest`, `LimitRequestServiceTest`) cover the risk engine and the
  core request lifecycle in isolation with Mockito.
- Integration tests (`AuthControllerIntegrationTest`, `LimitRequestFlowIntegrationTest`)
  boot the full Spring context against a real, disposable Postgres instance
  (`io.zonky.test:embedded-postgres` — a real `postgres` binary run as a subprocess, no
  Docker required) and drive the HTTP API end to end, including the OTP → biometric →
  risk assessment → approval flow.

## API surface

All endpoints are under `/api`. See Swagger UI for the full contract with request/response
schemas.

| Endpoint | Notes |
|---|---|
| `POST /auth/login` | Public |
| `GET /customer/me` | Customer |
| `GET /accounts` | Customer |
| `GET /limits/current` | Customer — dashboard summary + active request |
| `POST /limits/request` | Customer — starts the OTP step |
| `POST /limits/{id}/otp/verify` | Customer |
| `POST /limits/{id}/biometric/verify` | Customer — triggers risk assessment |
| `GET /limits/history` / `GET /limits/{id}` | Customer |
| `GET /support/requests` | Support agent (MEDIUM queue) / Manager (HIGH queue) |
| `POST /support/requests/{id}/approve\|reject\|request-verification` | Support/Manager |
| `POST /support/requests/{id}/notes` | Support/Manager |
| `GET /notifications` | Any authenticated user |
| `GET /audit` | Support/Manager |

## Risk engine

`RiskEngine` (application/limitrequest/risk) runs a set of independent `RiskRule` strategies
and takes the highest-severity result:

- `AmountThresholdRiskRule` — above the configured ceiling (`limitflow.risk.high-threshold`,
  default ₦1,000,000) → HIGH
- `MultiplierRiskRule` — requesting ≥2x the current limit → MEDIUM
- `DeviceTrustRiskRule` — unrecognized device → MEDIUM
- `SuspiciousActivityRiskRule` — suspicious activity flag → HIGH

LOW auto-approves; MEDIUM/HIGH route to the support/manager review queue respectively.

## What's intentionally out of scope

Refresh tokens, real SMS/push delivery (OTPs are surfaced through the in-app notification
feed instead), real biometric hardware integration, and any card/loan/statement/payment
functionality — this backend exists to demonstrate the limit-increase journey, not to be a
full banking platform.
