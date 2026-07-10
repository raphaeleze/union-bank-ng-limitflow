# System Design

## Modules

The backend is organized into eight modules, matching the original brief exactly — no
module was added or dropped:

| Module | Backend package(s) |
|---|---|
| Authentication | `application/auth`, `infrastructure/security` |
| Customer | `application/customer` |
| Account | `domain/account` |
| Transfer Limit | `domain/limitrequest`, `application/limitrequest` (incl. the risk engine) |
| Verification | `domain/otp`, `application/otp` |
| Notifications | `domain/notification`, `application/notification` |
| Support Case | `domain/support`, `application/support` |
| Audit | `domain/audit`, `application/audit` |

## Request lifecycle (state machine)

```
PENDING ──► OTP_PENDING ──► BIOMETRIC_PENDING ──► (risk assessment) ──┬──► APPROVED
                                                                       └──► UNDER_REVIEW ──┬──► APPROVED
                                                                                            └──► REJECTED
```

- A request is created already in `OTP_PENDING` — the "choose limit + reason" step happens
  client-side before the first API call, so there's no observable `PENDING` state in
  practice today; the status exists for a request that's been created but not yet had an
  OTP issued, which isn't currently reachable but keeps the state machine honest for future
  flows (e.g. a save-and-resume draft).
- `OTP_PENDING → BIOMETRIC_PENDING` requires a correct, unexpired OTP code.
- `BIOMETRIC_PENDING → (APPROVED | UNDER_REVIEW)` runs the risk engine synchronously —
  there's no queue or async worker, since a single risk assessment is fast enough to do
  inline within the request.
- `UNDER_REVIEW → (APPROVED | REJECTED)` is a human decision (support agent or manager).
  Support can also loop a request **back** to `OTP_PENDING` via "request additional
  verification" — the only backward transition in the whole machine.
- `APPROVED` and `REJECTED` are terminal. There's no reopening a decided request.

Every transition is recorded as an `AuditLog` entry (`LIMIT_REQUESTED`, `OTP_VERIFIED`,
`BIOMETRIC_VERIFIED`, `RISK_ASSESSED`, `LIMIT_APPROVED`, `MANUAL_APPROVED`,
`MANUAL_REJECTED`, `VERIFICATION_REQUESTED`) and, where the customer should be told,
a `Notification`.

## Risk engine

`RiskEngine` (`application/limitrequest/risk`) runs every registered `RiskRule` and takes
the **highest-severity** result — rules don't short-circuit each other, so adding a new
rule never accidentally weakens an existing one.

| Rule | Trigger | Result |
|---|---|---|
| `AmountThresholdRiskRule` | requested limit exceeds a hard ceiling (`limitflow.risk.high-threshold`, default ₦1,000,000) | HIGH |
| `SuspiciousActivityRiskRule` | suspicious-activity flag set on the request | HIGH |
| `MultiplierRiskRule` | requested limit ≥ 2× the current limit | MEDIUM |
| `DeviceTrustRiskRule` | request came from a device not marked as trusted | MEDIUM |
| *(none of the above)* | | LOW |

LOW auto-approves and updates the account's daily limit immediately. MEDIUM routes to the
support queue; HIGH routes to the manager queue — this split isn't a separate concept in
the data model, it's just `GET /support/requests` filtering by risk level based on the
caller's role (`SupportReviewService.queueFor`). A manager and a support agent hit the
exact same endpoint; they just see different rows.

Adding a rule means writing one class implementing `RiskRule` and registering it as a
Spring bean — `RiskEngine` picks it up automatically via constructor-injected
`List<RiskRule>`, no switch statement to extend.

## Why synchronous risk assessment, not a queue

At real-bank scale, risk assessment might call out to fraud-detection services, device
fingerprinting providers, etc., and would reasonably be async. Here, every rule is a pure
function over data already on the request (amount, device-trust flag, a suspicious-activity
flag) — there's nothing to wait on, so a queue would add operational complexity (a broker,
retry logic, an idempotency story) to solve a latency problem that doesn't exist yet. If a
real external risk call were added later, that's the point where this would need to become
async — noted, not built, per the project's "don't build for hypothetical requirements"
principle.
