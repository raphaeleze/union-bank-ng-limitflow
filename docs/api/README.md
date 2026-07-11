# API Reference

Documented directly from the controller source
(`apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller`), not
generated from a running instance's OpenAPI export — this environment didn't have a
Java 21/Maven toolchain available to run the backend and produce one. When you *can* run
it, prefer the interactive, always-in-sync version at `/swagger-ui.html`; treat this file
as a quick-reference that mirrors it.

All paths are relative to `/api`. All request/response bodies are JSON. Authenticated
endpoints expect `Authorization: Bearer <token>` from `POST /auth/login`.

## Auth

| Method | Path | Auth | Body → Response |
|---|---|---|---|
| POST | `/auth/login` | none | `{email, password}` → `{token, user}` |

## Customer

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/customer/me` | any authenticated user | `UserSummary` |
| GET | `/accounts` | any authenticated user | `AccountResponse[]` — the caller's own accounts |

## Transfer limit requests (customer-facing)

Role required: `CUSTOMER` on every endpoint in this group.

| Method | Path | Body → Response |
|---|---|---|
| GET | `/limits/current` | → `{accountId, dailyLimit, usedToday, remaining, activeRequest}` |
| POST | `/limits/request` | `{accountId, requestedLimit, reason, knownDevice}` → `LimitRequestResponse` (status `OTP_PENDING`) |
| POST | `/limits/{id}/otp/verify` | `{code}` → `LimitRequestResponse` (status `BIOMETRIC_PENDING`) |
| POST | `/limits/{id}/biometric/verify` | `{success}` → `LimitRequestResponse` (status `APPROVED` or `UNDER_REVIEW`, risk assessed) |
| GET | `/limits/history` | → `LimitRequestResponse[]` for the caller's account |
| GET | `/limits/{id}` | → `LimitRequestResponse` (must be the caller's own request) |

`LimitRequestResponse` includes a `timeline` array of `{label, status}` steps
(`COMPLETE`/`CURRENT`/`PENDING`) driving the customer portal's progress screen.

## Support review (staff-facing)

Role required: `SUPPORT_AGENT` or `MANAGER` on every endpoint in this group.
`GET /support/requests` is **role-scoped automatically** — support agents see MEDIUM-risk
requests, managers see HIGH-risk requests, from the same endpoint.

| Method | Path | Body → Response |
|---|---|---|
| GET | `/support/requests` | → `SupportQueueItemResponse[]` |
| GET | `/support/requests/{id}` | → `LimitRequestResponse` (full detail, any status — added for the employee portal, see its README) |
| POST | `/support/requests/{id}/approve` | `{note?}` → `LimitRequestResponse` (must be `UNDER_REVIEW`) |
| POST | `/support/requests/{id}/reject` | `{note?}` → `LimitRequestResponse` (must be `UNDER_REVIEW`) |
| POST | `/support/requests/{id}/request-verification` | `{note?}` → `LimitRequestResponse` (sends it back to `OTP_PENDING`) |
| POST | `/support/requests/{id}/notes` | `{note}` (required) → `SupportNoteResponse` |
| GET | `/support/requests/{id}/notes` | → `SupportNoteResponse[]` |

## Notifications

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/notifications` | any authenticated user | `NotificationResponse[]` for the caller |

## Audit

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/audit` | `SUPPORT_AGENT` or `MANAGER` | `AuditLogResponse[]`, newest first |

## Errors

Every error response is a single shape (`GlobalExceptionHandler` → `ApiError`):

```json
{
  "timestamp": "2026-07-10T13:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requested limit must be greater than the current limit",
  "path": "/api/limits/request"
}
```

`message` is always written to be shown to an end user as-is — controllers and services
never leak stack traces or raw exception text.
