# Architecture

## System overview

```
                 Next.js Customer Portal
                         │
                   REST API (HTTPS/JSON)
                         │
                 Spring Boot Backend
                         │
      ┌──────────────────┼──────────────────┐
      │                  │                  │
 PostgreSQL       Notification feed    Audit log
 (Flyway-managed)  (in-app, mocked      (every state
                    OTP/SMS delivery)    transition)
      │
 Next.js Employee Portal (support / manager)
```

One backend, two frontends. The customer portal is the primary product — it's what a
customer uses to request a limit increase. The employee portal is the internal tool support
agents and managers use when the backend's risk engine routes a request to manual review.
Both talk to the same REST API; neither talks to the database directly.

The customer-facing app was originally built as a Flutter mobile app, but was rebuilt as a
Next.js web app — same journey, same backend contract, but reusing the employee portal's
already-proven Next.js/TypeScript toolchain instead of maintaining a second, unrelated stack
(Dart/Flutter) for a demo project. The original Flutter implementation is still visible in
this repository's git history.

## Why a monorepo

The three apps ship and evolve together: a backend DTO change affects both frontends in the
same commit, and reviewing "the limit-increase feature" as one pull request (rather than
three separate repos with pinned SHAs pointing at each other) matches how this project is
actually developed. Each `apps/*` directory is a self-contained project with its own
dependency manager, and the only thing tying them together is the folder they live in and
the API contract they share.

## Backend: Clean Architecture

```
domain            entities + repository ports (User, Account, LimitRequest, RiskLevel, ...)
                  no framework dependencies beyond JPA annotations
application       use-case services (LimitRequestService, RiskEngine, SupportReviewService, ...)
                  orchestration only — no HTTP, no JPA specifics
infrastructure    Spring Data JPA repositories, JWT security, OpenAPI/CORS config
presentation      REST controllers, request/response DTOs, global exception handling
```

Dependencies point inward: `presentation` → `application` → `domain`, with `infrastructure`
implementing ports declared by `domain` (repository interfaces) and `application`
(`TokenService`). Domain entities are also the JPA entities — a separate persistence model
would be pure ceremony at this project's scale, so that's the one deliberate simplification
against "textbook" Clean Architecture. See
[`apps/backend-api/README.md`](../../apps/backend-api/README.md) for the risk engine's
strategy-pattern design and the full endpoint list.

## Customer portal: feature-folder Next.js

```
app/            routes (App Router) — login, and an authenticated (portal) route group
                wrapping dashboard/increase-limit/requests/notifications/support/profile
                behind a client-side auth guard, with a bottom nav for the mobile-first
                journey this app was originally designed as
components/ui/  shadcn-style primitives, hand-written (not CLI-generated)
components/*    feature components (dashboard cards, wizard steps, request timeline, ...)
hooks/          one TanStack Query hook per backend resource
lib/            axios client, auth context, shared types
```

Structurally identical to the employee portal below — same App Router conventions, same
hand-rolled UI primitives, same TanStack Query + axios data layer — just with customer-only
routes (`CUSTOMER` role) and a five-step increase-limit wizard (choose limit → reason →
review → OTP → biometric) instead of a review queue. See
[`apps/customer-portal/README.md`](../../apps/customer-portal/README.md).

## Employee portal: feature-folder Next.js

```
app/            routes (App Router) — login, and an authenticated (portal) route group
                wrapping dashboard/queue/audit behind a client-side auth guard
components/ui/  shadcn-style primitives, hand-written (not CLI-generated)
components/*    feature components (queue table, timeline, review actions, ...)
hooks/          one TanStack Query hook per backend resource
lib/            axios client, auth context, shared types
```

No server-side rendering of backend data — every page is a client component that fetches
through TanStack Query, because the data genuinely lives behind a separate API the browser
talks to directly, not something Next.js's own server needs to broker. See
[`apps/employee-portal/README.md`](../../apps/employee-portal/README.md) for the one small
backend addition (`GET /support/requests/{id}`) this app needed.

## Cross-cutting: audit and notifications

Every state-changing action (login, OTP verified, biometric verified, risk assessed,
approved, rejected, manual note added) writes an `AuditLog` row and, where the customer
should know about it, a `Notification` row. Both the customer portal's notification center
and the employee portal's audit page read from these — there's no separate "activity feed"
concept layered on top; the audit log *is* the activity feed.
