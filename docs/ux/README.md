# UX Flows

## The problem this project responds to

See the [root README](../../README.md) for the full story. In short: increasing a transfer
limit at a real bank meant phone queues, WhatsApp queues, a video call, and a PDF form
asking for information the bank already had — spread across days. Every flow below exists
to answer one question: *what would this look like if it just... didn't require any of
that?*

## Customer journey (mobile app)

```
Splash
  └─► Login (password, or biometric shortcut if already logged in once)
        └─► Dashboard
              │  shows: current limit, used today, remaining, active request (if any)
              └─► "Increase transfer limit"
                    └─► Step 1: Choose limit      (slider + quick-select chips)
                    └─► Step 2: Reason            (suggestion chips + free text)
                    └─► Step 3: Review            (device-trust toggle, submit)
                    └─► Step 4: OTP               (6-digit code, delivered via
                                                    the Notifications tab in this demo)
                    └─► Step 5: Biometric         (mock local_auth prompt)
                    └─► Result:
                          ├─► LOW risk    → "Limit increased!" — done, ~instant
                          └─► MEDIUM/HIGH → "Under review" — timeline screen,
                                             notified when support/manager decides
```

Every step explains *why* it's happening (the OTP step literally says where to find the
code, since there's no real SMS gateway behind this demo) — per the project's UX
principle of never leaving the customer wondering what's going on or why.

The whole thing, LOW-risk path, is designed to take under two minutes and under six taps —
the explicit budget from the original brief, and the entire point of the exercise: the same
outcome the real-world version took multiple days and five support channels to reach.

## Support/manager journey (employee portal)

```
Login (support or manager account)
  └─► Dashboard — pending count, approved/rejected today, avg. resolution time, recent activity
  └─► Review queue — role-scoped automatically (MEDIUM → support, HIGH → manager)
        └─► Request detail
              ├─ Timeline (same steps the customer sees)
              ├─ Risk badge + reason given
              ├─ Support notes (visible to the customer as a notification when added)
              └─ Actions: Approve · Reject · Request additional verification
                    (each opens a confirm dialog with an optional note)
  └─► Audit log — every action, by anyone, across the whole system
```

There's deliberately no separate "Manager Dashboard" page distinct from the support
dashboard — the backend already scopes the queue by role, so building two near-identical
pages would be duplicating a decision the API already makes. The one thing genuinely
manager-only (per the brief) is that HIGH-risk items only ever appear in a manager's queue
in the first place; approve/reject/request-verification work identically for both roles
once a request is in front of them.

## Current vs. proposed, restated per-flow

| Step (real-world) | This demo's equivalent |
|---|---|
| Call customer care, wait 11+ minutes | Nothing to wait for — the flow is entirely in-app |
| WhatsApp queue position ~300, hours of silence | If manual review is needed, a push-style notification the moment it's decided |
| "You're not the account owner" dead end | Not modeled here — see root README's future-improvements note on delegated access |
| Video verification call | OTP + biometric, both inside the same five-step flow |
| PDF form asking for data the bank already has | Nothing re-asked — the reason field is the only new input, everything else is pulled from the account |
| Multiple disconnected support channels | One queue, one detail page, one place staff make the call |
