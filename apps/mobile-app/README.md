# LimitFlow Mobile

The customer-facing app for the transfer-limit increase journey described in the
[repository root README](../../README.md). Flutter, Clean Architecture, Riverpod, GoRouter.

## One-time setup

This app was authored without a local Flutter SDK available, so the native `android/` and
`ios/` platform folders aren't checked in — generating them by hand (Gradle files, Info.plist,
etc.) without being able to build/verify them would be far more likely to ship broken than to
help. Instead, generate them with Flutter's own tooling, which does this deterministically:

```bash
cd apps/mobile-app
flutter create --org com.limitflow --project-name limitflow_mobile .
flutter pub get
```

This backfills `android/` and `ios/` without touching anything under `lib/`.

### Enable biometrics on each platform

`local_auth` needs a small amount of native wiring that `flutter create` doesn't add on its own:

**Android** — in `android/app/src/main/AndroidManifest.xml`, add:
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC"/>
```
and change `MainActivity` to extend `FlutterFragmentActivity` instead of `FlutterActivity`
(required by `local_auth` on Android).

**iOS** — in `ios/Runner/Info.plist`, add:
```xml
<key>NSFaceIDUsageDescription</key>
<string>LimitFlow uses Face ID to confirm sensitive account changes.</string>
```

If biometrics aren't available/enrolled on the device or emulator, the app treats that as
"no biometric hardware" and lets the demo continue rather than blocking the flow.

## Run

The backend must be running first (see [`apps/backend-api`](../backend-api)).

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api   # Android emulator
flutter run --dart-define=API_BASE_URL=http://localhost:8080/api  # iOS simulator
```

Without `--dart-define`, the app defaults to `10.0.2.2` on Android and `localhost` everywhere
else, which covers the common emulator/simulator case.

Demo login: `customer@limitflow.demo` / `Password123!` (see the backend README for the
support/manager accounts too).

## Architecture

```
lib/
├── core/            # cross-cutting: theme, network client, storage, session, router, widgets
├── features/
│   ├── authentication/   # splash, login, mock biometric
│   ├── dashboard/        # limit/used/remaining summary
│   ├── limit_request/    # increase-limit wizard, request history, status timeline
│   ├── notifications/    # notification center
│   ├── support/          # mock support chat
│   ├── profile/
│   └── shell/            # bottom navigation
└── main.dart
```

Each feature follows `domain/` (plain data classes) → `application/` (repositories + Riverpod
providers) → `presentation/` (screens/widgets), talking to the backend over `Dio` and keeping
the JWT in `flutter_secure_storage`. The dashboard falls back to the last cached response
(via Hive) if the network call fails, so it's still useful offline.

**Note on codegen:** this app intentionally uses plain Dart classes and plain Riverpod
providers instead of Freezed/json_serializable/riverpod_generator. Those require running
`build_runner`, which needs a working Flutter/Dart toolchain — unavailable in the environment
this was authored in. Swapping them in later is a mechanical, low-risk refactor once you have
a local Flutter setup to verify the generated code against.

## What's mocked

- **OTP delivery**: no SMS gateway exists. The backend surfaces the code through the
  Notifications feed instead — the OTP step in the wizard tells you to check there.
- **Biometrics**: a real `local_auth` prompt, but with no backend-side liveness/anti-spoof
  check behind it — it demonstrates the UX step, not a production auth system.
- **Support chat**: static, canned replies. There's no chat backend.
