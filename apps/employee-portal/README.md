# LimitFlow Employee Portal

The internal review console for the transfer-limit increase journey described in the
[repository root README](../../README.md). Next.js (App Router), TypeScript, Tailwind CSS,
hand-rolled shadcn-style UI primitives.

This portal is **not customer-facing** — it's what support agents and managers use to review
the requests the [mobile app](../mobile-app)'s risk engine routes to manual review.

## Run

The backend must be running first (see [`apps/backend-api`](../backend-api)).

```bash
cp .env.local.example .env.local   # defaults to http://localhost:8080/api
npm install
npm run dev
```

Open http://localhost:3000 — you'll land on `/login`.

Demo accounts (password `Password123!` for both):

| Role | Email | Sees |
|---|---|---|
| Support agent | `support@limitflow.demo` | MEDIUM-risk queue |
| Manager | `manager@limitflow.demo` | HIGH-risk queue |

The backend scopes the review queue by role automatically — there's a single `/queue` route
in this app, and the backend decides whether a support agent or manager is looking at it.

## Pages

- `/login`
- `/dashboard` — pending/approved/rejected counts and average resolution time, computed
  client-side from the queue and audit endpoints (there's no dedicated metrics endpoint)
- `/queue` — the role-scoped review queue
- `/queue/[id]` — request detail: timeline, risk, reason, support notes, and the
  approve/reject/request-verification actions
- `/audit` — the full audit trail

## Architecture

```
src/
├── app/               # routes (App Router)
│   ├── login/
│   └── (portal)/      # authenticated shell: sidebar + topbar, route-guarded client-side
│       ├── dashboard/
│       ├── queue/
│       │   └── [id]/
│       └── audit/
├── components/
│   ├── ui/             # shadcn-style primitives (button, card, table, dialog, ...)
│   ├── layout/          # sidebar, topbar
│   ├── dashboard/
│   └── queue/           # queue table, status/risk badges, timeline, review actions, notes
├── hooks/              # TanStack Query hooks per resource
└── lib/                # axios client, auth context, shared types, utils
```

Auth is a JWT held in a (non-httpOnly) cookie, attached via an Axios interceptor; a client-side
`AuthProvider`/`useAuth` context guards the `(portal)` route group and redirects to `/login`
on a 401 from the backend. There's no Next.js `proxy`/middleware-based guard — for a small
internal tool with client-fetched data, the client-side guard is simpler and sufficient.

**Note on shadcn/ui:** the `components/ui/*` primitives are hand-written in the shadcn
pattern (Radix primitives + `class-variance-authority` + Tailwind), not generated via the
`shadcn` CLI. Swapping in CLI-managed components later is a drop-in replacement.

## One backend addition made alongside this app

The queue endpoint (`GET /support/requests`) only returns summary fields (customer name,
amounts, risk, status) — it doesn't include the reason or timeline needed for a request
detail view, and there was no way to look up a single request by ID as staff. This app adds
`GET /support/requests/{id}` to `SupportController`/`SupportReviewService` on the backend,
reusing the existing `LimitRequestResponse` DTO. It wasn't verified with `mvn test` in the
environment this was authored in (Java 21/Maven weren't available) — run the backend test
suite before relying on it.
