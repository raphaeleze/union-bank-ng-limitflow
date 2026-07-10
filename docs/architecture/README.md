# Architecture

## System overview

```
                 Flutter Mobile App (customer)
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

One backend, two frontends. The mobile app is the primary product — it's what a customer
uses to request a limit increase. The employee portal is the internal tool support agents
and managers use when the backend's risk engine routes a request to manual review. Both
talk to the same REST API; neither talks to the database directly.

## Why a monorepo

The three apps ship and evolve together: a backend DTO change affects both frontends in the
same commit, and reviewing "the limit-increase feature" as one pull request (rather than
three separate repos with pinned SHAs pointing at each other) matches how this project is
actually developed. There's no shared code between the Flutter and TypeScript/JS apps to
justify a package-based monorepo tool (Nx, Turborepo) — each `apps/*` directory is a
self-contained project with its own dependency manager, and the only thing tying them
together is the folder they live in and the API contract they share.

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

## Mobile app: Clean Architecture per feature

```
core/         theme, network client, storage (secure token + Hive cache), session, router,
              shared widgets — cross-cutting, no feature-specific logic
features/*/
  domain/       plain Dart data classes (no codegen — see note below)
  application/  repositories (talk to the backend) + Riverpod providers
  presentation/ screens and feature-local widgets
```

Each feature (`authentication`, `dashboard`, `limit_request`, `notifications`, `support`,
`profile`) is structured the same way, so the increase-limit wizard and the dashboard follow
an identical pattern despite doing very different things. State management is plain
Riverpod and navigation is GoRouter; Freezed/json_serializable/riverpod_generator were
deliberately skipped because they need `build_runner` codegen, which wasn't available in
the environment this was authored in (no local Flutter SDK) — see
[`apps/mobile-app/README.md`](../../apps/mobile-app/README.md).

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
should know about it, a `Notification` row. Both the mobile app's notification center and
the portal's audit page read from these — there's no separate "activity feed" concept
layered on top; the audit log *is* the activity feed.
