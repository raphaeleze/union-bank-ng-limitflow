# Twilio OTP delivery (feature-flagged, log fallback)

## Context

`OtpService` is an explicit demo mock today (see its class comment): it generates a code and
hands it to `NotificationService`, which persists it as an in-app notification with the raw
code embedded in the message. The README lists "real SMS/push delivery" as a named, deliberate
scope gap. This closes that gap for SMS specifically, without touching the rest of the
demo-mode posture (no push, no email).

Users have no phone number today — `User` (`domain/user/User.java`) has none, and there is no
self-registration flow; users are pre-seeded via `V2__seed.sql`. There is also no profile-edit
endpoint, only `GET /api/customer/me` (`CustomerController`).

## Approach

One small additive component, not a generic multi-channel abstraction. There is exactly one
real delivery channel (Twilio SMS); building a pluggable `NotificationChannel` system for a
single implementation is speculative. The existing in-app notification path is untouched — this
sits alongside it.

## Components

**Migration** — `V3__add_user_phone.sql`: nullable `phone VARCHAR(20)` on `users`. A follow-up
seed statement (or amend within the same migration) sets a real-format demo number
(`+2348012345678` style) for the seeded customer (`customer@limitflow.demo`). `V1`/`V2` are not
edited — Flyway migrations already applied are immutable.

**`User.java`** — add `phone` field (nullable, no validation at the entity level — matches
existing fields).

**`UserSummary` DTO** — add `phone`, update the `from()` factory so it's visible via
`GET /api/customer/me`.

**`pom.xml`** — add `com.twilio.sdk:twilio`.

**`application.yml`** — new block, same convention as `limitflow.security.jwt.*`:

```yaml
limitflow:
  twilio:
    enabled: ${TWILIO_ENABLED:false}
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_FROM_NUMBER:}
```

`docker-compose.yml` gets the matching env vars on `backend-api`, defaulted off, alongside the
existing `JWT_SECRET` pattern.

**`OtpDeliveryService`** (new, `application/otp/`) — constructor-injected via `@Value`, matching
`JwtTokenService`'s existing pattern (no `@ConditionalOnProperty` anywhere in this codebase; stay
consistent). Initializes the Twilio SDK (`Twilio.init(accountSid, authToken)`) once, lazily, only
when `enabled=true` — so the demo doesn't need dummy credentials by default.

```java
public void deliver(User customer, String code) {
    if (shouldSendViaTwilio(customer)) {
        try {
            Message.creator(new PhoneNumber(customer.getPhone()),
                             new PhoneNumber(fromNumber),
                             "Your LimitFlow verification code is " + code).create();
            log.info("OTP sent via Twilio to {}", mask(customer.getPhone()));
            return;
        } catch (Exception e) {
            log.warn("Twilio send failed, falling back to log", e);
        }
    }
    log.info("OTP code (Twilio disabled or unavailable) for request: {}", code);
}

boolean shouldSendViaTwilio(User customer) {
    return enabled && customer.getPhone() != null && !customer.getPhone().isBlank();
}
```

`shouldSendViaTwilio` is package-private specifically so it's unit-testable without mocking the
Twilio SDK's static call.

**Wiring** — `LimitRequestService.sendOtp()` keeps its existing `notificationService.send(...)`
call unchanged (in-app visibility stays exactly as it is today) and adds one call to
`otpDeliveryService.deliver(customer, code)`.

## Error handling

Twilio failures (bad number, outage, bad credentials) never block the OTP flow: caught, logged
as a warning, falls through to the same log line used when Twilio is disabled. The limit request
flow has no path where a delivery failure prevents the user from later entering the code (the
code is already persisted by `OtpService.issue()` before delivery is attempted).

## Testing

The Twilio SDK's static `Message.creator(...)` isn't mockable without extra tooling, so:

- Unit test `shouldSendViaTwilio`: true when enabled + phone present, false when disabled, false
  when phone is null/blank.
- Unit test `deliver()`'s fallback path: with `enabled=false`, confirm no exception and a log
  fallback occurs (via a fake/no-op path — no real network call).

No integration test against real Twilio — out of scope for a demo project with no CI Twilio
account.

## Out of scope

- Signup/profile-edit UI for phone numbers (no such flow exists at all today; not created for
  this feature).
- Email or push delivery — README already scopes those out.
- Retrying failed Twilio sends.
