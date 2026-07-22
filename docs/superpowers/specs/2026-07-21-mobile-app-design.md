# LimitFlow Mobile (customer-facing Expo app)

## Context

The README's origin story starts with "I opened the mobile app expecting to increase the
limit." Every other part of LimitFlow now exists as a working web app except the one that
started the story. This adds a fourth app to the monorepo: `apps/mobile`, an Expo (React
Native + TypeScript) client that covers the same single customer journey `customer-portal`
already demonstrates — login, dashboard, the increase-limit wizard, request tracking,
notifications, profile, support — talking to the existing Spring Boot backend over the same
JWT REST API. Support/manager review stays web-only (`employee-portal`); that's a desktop-shaped
back-office workflow nobody in the story ever needed on a phone.

Two things a phone can do that a browser demo can't fake convincingly:

- **Biometric authentication** — the web wizard's "Confirm biometric" step is just a button.
  A phone has a real Face ID / fingerprint prompt.
- **Push notifications** — the web app's OTP delivery falls back to an in-app notification
  ("Demo mode: shown here instead of SMS"). A phone can receive an actual push.

Both are named explicitly in the README's "Current Experience vs Proposed Experience" table.
This design closes both gaps for the mobile client specifically.

## Approach

One new Expo app, reusing the backend as-is except one small additive piece (a push delivery
channel, following the exact pattern already established by `OtpDeliveryService`'s Twilio/log
strategy — see `docs/superpowers/specs/2026-07-20-twilio-otp-design.md`). No changes to
`customer-portal`, `employee-portal`, or any existing endpoint's request/response shape.

Biometrics stay a **local device gate**, not a cryptographic proof: the phone's OS
authentication challenge (Face ID / fingerprint / passcode fallback) gates whether the app is
allowed to call the existing `POST /limits/{id}/biometric/verify` with `success: true` — the
same trust model the web button already uses, just backed by something real instead of a
click. A passkey-style flow (device keypair, backend-verified signature) would be more
"real-world bank," but the README already names "real biometric hardware integration" as an
explicit, deliberate scope gap for this project; a local gate is the honest scope match.

## Components

### New app: `apps/mobile`

```
apps/mobile/
├── app/                          Expo Router (file-based, mirrors the Next.js App Router
│   ├── (auth)/
│   │   └── login.tsx             Email/password login (react-hook-form + zod, same schema
│   │                             shape as customer-portal/src/app/login/page.tsx)
│   ├── (app)/
│   │   ├── _layout.tsx           Tab bar: Home / Increase / Requests / Alerts / Profile
│   │   ├── index.tsx             Dashboard
│   │   ├── increase-limit.tsx    The 4-step wizard
│   │   ├── requests/
│   │   │   ├── index.tsx
│   │   │   └── [id].tsx
│   │   ├── notifications.tsx
│   │   ├── profile.tsx
│   │   └── support.tsx
│   └── _layout.tsx               Root: app-unlock gate (see below), providers
├── src/
│   ├── lib/
│   │   ├── api-client.ts         Same ApiError class and interceptor shape as the web
│   │   │                         client, token read from expo-secure-store instead of a cookie
│   │   ├── currency.ts           Identical formatCurrency (pure TS, no DOM dependency —
│   │   │                         copied as-is, not abstracted into a shared package; see
│   │   │                         "Out of scope")
│   │   ├── types.ts              Identical shared types, copied from customer-portal for the
│   │   │                         same reason
│   │   ├── auth.ts                Secure-store token read/write, biometric unlock check
│   │   └── push.ts               expo-notifications permission + token registration
│   ├── hooks/                    Same react-query hook shape as customer-portal
│   │   (use-dashboard.ts, use-limit-request.ts, use-request-detail.ts, ...)
│   └── components/ui/            Button, Card, Badge, Input, Checkbox, Dialog, Toast —
│                                  React Native ports of the same primitives, styled with
│                                  NativeWind against the same token names (accent, ink,
│                                  success/success-soft/success-strong, danger/warning
│                                  equivalents, danger, etc.) pulled from
│                                  customer-portal/src/app/globals.css
├── app.config.ts                 App name/icon/scheme; NEXT_PUBLIC_API_BASE_URL equivalent
│                                  as an Expo public env var
├── package.json
└── tsconfig.json
```

Styling is NativeWind (Tailwind for React Native) specifically so the existing design tokens
port with a find-and-replace rather than a redesign — the whole point of matching is that this
should look and feel like the same product as `customer-portal`, not a reskin.

### Auth & app-unlock

- Login stores the JWT in `expo-secure-store` (Keychain on iOS, Keystore-backed on Android) —
  never `AsyncStorage`, which is unencrypted.
- Root layout (`app/_layout.tsx`) checks for a stored token on launch/foreground. If present,
  it calls `expo-local-authentication`'s `authenticateAsync()` before rendering the tab
  navigator. Success → straight to Dashboard. Failure or cancel → "Try again" / "Use password
  instead" (clears the stored token, routes to `(auth)/login`).
- If the device has no enrolled biometrics/passcode (`hasHardwareAsync()` /
  `isEnrolledAsync()` both false), the unlock gate is skipped entirely and password login is
  the only path — this is a real device state, not an edge case to paper over.

### Increase-limit wizard — biometric step

Same 4-step flow, state, and resume/cancel behavor as the web wizard shipped in
`apps/customer-portal/src/app/(portal)/increase-limit/page.tsx` (step tracking, resuming an
in-flight `OTP_PENDING`/`BIOMETRIC_PENDING` request instead of stranding it, cancel with
confirmation). The one different step:

```tsx
async function handleBiometricConfirm() {
  const result = await LocalAuthentication.authenticateAsync({
    promptMessage: "Confirm it's you",
    fallbackLabel: "Use passcode",
  });
  if (!result.success) {
    toast("Biometric confirmation was cancelled or failed.", "error");
    return;
  }
  // same call the web app already makes
  const updated = await biometricMutation.mutateAsync({ requestId: effectiveRequest.id, success: true });
  ...
}
```

No new backend call — `success: true` is only sent once the OS challenge itself succeeded, so
the existing `LimitRequestService.verifyBiometric` needs no changes.

### Push notifications

**Mobile side** (`src/lib/push.ts`): after login, request notification permission
(`Notifications.requestPermissionsAsync()`), obtain an Expo push token
(`Notifications.getExpoPushTokenAsync()`), and register it with the backend. On logout,
unregister it.

**Backend side** — one new small vertical slice, same shape as every other feature in this
codebase:

- **Migration** `V4__add_push_tokens.sql`:

  ```sql
  CREATE TABLE push_tokens (
      id UUID PRIMARY KEY,
      user_id UUID NOT NULL REFERENCES users(id),
      expo_push_token VARCHAR(255) NOT NULL,
      platform VARCHAR(20) NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      UNIQUE (user_id, expo_push_token)
  );
  ```

- **`PushToken.java`** (`domain/push/`) — R2DBC entity, same `Persistable<UUID>` +
  `@PersistenceCreator` pattern every other client-assigned-id entity in this codebase uses
  (see `LimitRequest.java`'s class comment for why).

- **`PushTokenRepository`** — `ReactiveCrudRepository`, plus
  `findByUserId(UUID)` / `deleteByUserIdAndExpoPushToken(UUID, String)`.

- **`DeviceApi` / `DeviceController`** (`presentation/controller/`), same interface+impl split
  every other controller in this codebase uses:

  ```java
  @PostMapping("/api/devices/push-token")
  Mono<Void> register(@AuthenticationPrincipal User user, @Valid @RequestBody PushTokenRequest request);

  @DeleteMapping("/api/devices/push-token")
  Mono<Void> unregister(@AuthenticationPrincipal User user, @Valid @RequestBody PushTokenRequest request);
  ```

  `PushTokenRequest(String expoPushToken, String platform)`.

- **`PushNotificationService`** (`application/notification/`) — the new delivery channel:

  ```java
  @Service
  @RequiredArgsConstructor
  public class PushNotificationService {

      private final PushTokenRepository pushTokenRepository;
      private final WebClient expoPushClient; // baseUrl: https://exp.host/--/api/v2/push/send

      public Mono<Void> push(UUID userId, String title, String body) {
          return pushTokenRepository.findByUserId(userId)
                  .flatMap(token -> expoPushClient.post()
                          .bodyValue(Map.of("to", token.getExpoPushToken(), "title", title, "body", body))
                          .retrieve()
                          .toBodilessEntity())
                  .then();
      }
  }
  ```

- **Wiring** — `NotificationService.send(...)` (the one place every in-app notification
  already flows through — OTP codes, "Under review," "Limit increased," everything) adds one
  fire-and-forget call:

  ```java
  public Mono<Notification> send(UUID userId, NotificationType type, String title, String message) {
      return notificationRepository.save(new Notification(userId, type, title, message))
              .flatMap(saved -> pushNotificationService.push(userId, title, message)
                      .onErrorResume(e -> Mono.empty())
                      .thenReturn(saved));
  }
  ```

  This is the whole point of routing push through `NotificationService` instead of touching
  `LimitRequestService`/`SupportReviewService` directly: **zero changes** to either service.
  Every notification they already send — OTP codes, "Under review," "Limit increased,"
  rejection reasons — starts arriving as a real push the moment a user has a registered
  device token, with no new call sites anywhere.

## Data flow examples

**Returning-session unlock:** app foreground → secure-store has a token → `authenticateAsync()`
→ success → Dashboard renders with the stored token, exactly as if the user had just logged in
(react-query fetches `/limits/current` etc. the same way). No new backend round-trip for the
unlock itself — it's purely a local gate in front of an already-valid token.

**OTP via push:** customer submits a limit increase → `LimitRequestService.submitRequest` calls
`sendOtp` (unchanged) → `otpDeliveryService.deliver(...)` (Twilio/log, unchanged) **and**
`notificationService.send(...)` (unchanged call site) → inside that now-updated method, the
in-app notification is saved *and* `pushNotificationService.push(...)` fires → if this user has
a registered mobile device, a real push lands: "OTP sent — Your verification code is 482913."

## Error handling

- Push send failures never block anything: `onErrorResume` swallows them exactly the way
  `OtpDeliveryService.deliver` already swallows Twilio failures and falls back to a log line —
  same established pattern, applied consistently.
- Biometric prompt cancellation/failure (either at the wizard step or at app-unlock) always
  falls back to a manual path (retry, or password login) — never a dead end.
- No registered push token for a user is the default, expected state (web-only users, or a
  mobile user who denied the permission prompt) — `findByUserId` returning empty is normal,
  not an error.
- Standard mobile network-loss handling (react-query's existing retry/error states, same
  `ApiError` messages the web app already shows) — no bespoke offline mode.

## Testing

Matches this project's existing split: automated tests on the backend, manual verification on
the frontend (`customer-portal` and `employee-portal` have no test suites either — this is a
demonstration project, not shipped software).

- **Backend**, mirroring `OtpDeliveryServiceTest`'s style exactly:
  - `PushNotificationServiceTest`: `push()` with a registered token calls the Expo client once
    with the expected body; with no registered token, `push()` completes without calling the
    client; a client failure is swallowed (via `onErrorResume` at the `NotificationService`
    call site) and never surfaces to the caller.
  - `NotificationServiceTest` (new — none exists today): `send()` still persists the
    notification even when the push call fails.
- **Mobile**: manual verification via Expo Go / a simulator, the same "drive it in a real
  browser" standard already used for the two Next.js apps throughout this project — biometric
  APIs in particular are not meaningfully unit-testable without real hardware.

## Out of scope

- **App Store / Play Store publishing, EAS build/signing pipeline** — "try it" means Expo Go
  or a simulator, matching the other three apps' `docker compose up` bar for entry. A
  demonstration project doesn't need a distribution pipeline.
- **Cryptographic (passkey-style) biometrics** — see "Approach"; the README already scopes out
  real biometric hardware integration.
- **Offline mode / local caching beyond react-query's defaults.**
- **A shared `packages/shared` workspace for the types/currency code duplicated between
  `customer-portal` and `mobile`.** Two consumers with a handful of small, stable, pure-TS
  files is not yet a real duplication problem; extracting a shared package is a reasonable
  follow-up once (if) a third consumer or real drift shows up, not a day-one abstraction.
- **Employee/support flows on mobile** — desktop-shaped review work, stays on
  `employee-portal`.
- **Android/iOS platform-specific push provider setup beyond Expo's own push service**
  (i.e., no direct FCM/APNs integration) — Expo's push service is the standard, zero-config
  path for an Expo-managed app and is what this design assumes throughout.
