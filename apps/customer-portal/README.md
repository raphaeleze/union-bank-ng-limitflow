# LimitFlow Customer Portal

The customer-facing app for the transfer-limit increase journey described in the
[repository root README](../../README.md). Next.js (App Router), TypeScript, Tailwind CSS,
hand-rolled shadcn-style UI primitives — the same stack and conventions as the
[employee portal](../employee-portal), reused deliberately (see the root README for why
this replaced an earlier Flutter mobile app).

## Run

The backend must be running first (see [`apps/backend-api`](../backend-api)).

```bash
cp .env.local.example .env.local   # defaults to http://localhost:8080/api
npm install
npm run dev
```

Open http://localhost:3000 — you'll land on `/login`.

Demo account: `customer@limitflow.demo` / `Password123!`. Starts at a ₦200,000 daily limit
with ₦180,000 used today, and already has a ₦500,000 request sitting in the support queue.

## Pages

- `/login`
- `/dashboard` — current limit, usage, and the active request (if any)
- `/increase-limit` — the five-step wizard: choose limit → reason → review → OTP → biometric
- `/requests` — request history
- `/requests/[id]` — a single request's status timeline
- `/notifications` — the in-app notification feed
- `/support` — contact channels
- `/profile` — account info and logout

## Architecture

```
src/
├── app/               # routes (App Router)
│   ├── login/
│   └── (portal)/      # authenticated shell: topbar + bottom nav, route-guarded client-side
│       ├── dashboard/
│       ├── increase-limit/
│       ├── requests/
│       │   └── [id]/
│       ├── notifications/
│       ├── support/
│       └── profile/
├── components/
│   ├── ui/             # shadcn-style primitives (button, card, dialog, ...)
│   ├── layout/          # topbar, bottom nav
│   ├── dashboard/        # limit summary card, active-request banner
│   └── requests/         # status badge, timeline, request list item
├── hooks/              # TanStack Query hooks per resource
└── lib/                # axios client, auth context, shared types, utils
```

Auth is a JWT held in a (non-httpOnly) cookie, attached via an Axios interceptor; a
client-side `AuthProvider`/`useAuth` context guards the `(portal)` route group and redirects
to `/login` on a 401 from the backend. Same pattern as the employee portal, just gated to
the `CUSTOMER` role instead of `SUPPORT_AGENT`/`MANAGER` — and different cookie names
(`limitflow_customer_token`/`limitflow_customer_user`) so both portals can run side by side
without clobbering each other's session if ever pointed at the same domain.

The bottom nav (rather than the employee portal's sidebar) reflects this app's origin as a
mobile-first customer journey — it reads naturally full-width on a phone browser while still
working fine on desktop.

**Note on shadcn/ui:** the `components/ui/*` primitives are hand-written in the shadcn
pattern (Radix primitives + `class-variance-authority` + Tailwind), not generated via the
`shadcn` CLI, and are copied verbatim from the employee portal — they have no
customer/employee-specific logic.

## What's mocked or out of scope

- **Support is a contact card, not live chat.** The backend has no customer-facing
  messaging endpoint (`SupportController` is staff-only, for reviewing requests — not for
  customer conversations), so `/support` shows contact channels rather than faking a chat
  UI backed by nothing.
- **Profile is read-only.** There's no backend endpoint to edit customer details.
- **OTP code delivery is via the notification feed**, not real SMS — the demo code shows up
  as a `Notifications` entry after submitting a request, same as the backend's actual mocked
  delivery mechanism.
