# LimitFlow Mobile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a customer-facing Expo (React Native) mobile app covering the same single
journey `customer-portal` demonstrates, with real device biometrics and real push
notifications — the two capabilities the README names but a browser demo can't fake.

**Architecture:** A new `apps/mobile` Expo Router app consumes the existing Spring Boot REST
API exactly like `customer-portal` does. The backend gains one additive vertical slice (push
token registration + an Expo push delivery channel wired through the existing
`NotificationService`) — zero changes to `LimitRequestService` or `SupportReviewService`.

**Tech Stack:** Expo (React Native + TypeScript), Expo Router, NativeWind (Tailwind for React
Native), `@tanstack/react-query`, `axios`, `expo-secure-store`, `expo-local-authentication`,
`expo-notifications`, `expo-linear-gradient`, `lucide-react-native`, `date-fns`,
`react-hook-form` + `zod`. Backend: Spring WebFlux `WebClient`, Spring Data R2DBC (same
`Persistable<UUID>` pattern as every other entity).

## Global Constraints

- Full design-spec reference: `docs/superpowers/specs/2026-07-21-mobile-app-design.md`.
- No changes to `customer-portal`, `employee-portal`, or any existing endpoint's
  request/response shape. The mobile app is a new consumer of the API exactly as it exists
  today, plus one new endpoint pair (`POST`/`DELETE /api/devices/push-token`).
- Backend: reactive end-to-end (`Mono`/`Flux`), Clean Architecture layering (domain interface +
  infrastructure R2DBC adapter), the `Persistable<UUID>` + `@PersistenceCreator` pattern for
  every entity with a client-assigned id, exact `<AccessLevel.PROTECTED>` no-args constructor
  convention — copy `Notification.java`'s shape, not just its intent.
  All new backend Mono chains follow this project's existing reactive style (no blocking calls
  outside `Schedulers.boundedElastic()`, matching `OtpDeliveryService`).
- Biometrics are a **local device gate only** — the OS challenge (Face ID/fingerprint/passcode)
  gates whether the app calls the existing `POST /limits/{id}/biometric/verify` with
  `success: true`. No cryptographic proof, no backend changes for the biometric step itself
  (see spec's "Approach" for why this matches the project's stated scope).
  Same for app-unlock: purely a local gate in front of an already-valid stored token, no new
  backend round-trip.
- Push send failures never block anything: swallowed with `onErrorResume`, exactly the way
  `OtpDeliveryService.deliver` already swallows Twilio failures.
- Mobile screens visually match `customer-portal`'s existing design tokens (colors from
  `apps/customer-portal/src/app/globals.css`) — this is a port, not a redesign.
- No automated test suite for the mobile app — matches `customer-portal`'s and
  `employee-portal`'s existing precedent (neither has one; this is a demonstration project
  verified manually, not shipped software). Every mobile task's verification step is
  `npx tsc --noEmit` (must be clean) plus a manual smoke check in Expo Go / a simulator.
  Backend tasks keep this project's existing TDD convention (JUnit 5 + Mockito + StepVerifier).
- Out of scope (do not build): App Store/Play Store publishing or EAS build/signing, a
  cryptographic passkey-style biometric flow, offline mode/local caching beyond react-query's
  defaults, a shared `packages/` workspace for the types/currency files duplicated between
  `customer-portal` and `mobile`, employee/support flows on mobile, and any FCM/APNs
  integration beyond Expo's own push service.

---

### Task 1: Push token persistence

**Files:**
- Create: `apps/backend-api/src/main/resources/db/migration/V4__add_push_tokens.sql`
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/domain/push/PushToken.java`
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/domain/push/PushTokenRepository.java`
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/persistence/PushTokenR2dbcRepository.java`
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/domain/push/PushTokenTest.java`

**Interfaces:**
- Produces: `PushToken` (fields: `id: UUID`, `userId: UUID`, `expoPushToken: String`,
  `platform: String`, `createdAt: Instant`), constructor
  `PushToken(UUID userId, String expoPushToken, String platform)`.
- Produces: `PushTokenRepository.save(S)`, `findByUserId(UUID): Flux<PushToken>`,
  `deleteByUserIdAndExpoPushToken(UUID, String): Mono<Void>`.

- [ ] **Step 1: Write the migration**

```sql
CREATE TABLE push_tokens (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users(id),
    expo_push_token    VARCHAR(255) NOT NULL,
    platform           VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, expo_push_token)
);
```

- [ ] **Step 2: Write the domain entity**

`apps/backend-api/src/main/java/com/limitflow/backend/domain/push/PushToken.java`:

```java
package com.limitflow.backend.domain.push;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code id} is client-assigned (not DB-generated), so Spring Data R2DBC can't tell new
 * from existing rows just by checking for a null id — see {@link Persistable} on every other
 * entity in this codebase for why an explicit {@code isNew} flag is needed instead.
 */
@Table("push_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("user_id")
    private UUID userId;

    @Column("expo_push_token")
    private String expoPushToken;

    private String platform;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Transient
    private boolean isNew = true;

    public PushToken(UUID userId, String expoPushToken, String platform) {
        this.userId = userId;
        this.expoPushToken = expoPushToken;
        this.platform = platform;
    }

    @PersistenceCreator
    PushToken(UUID id, UUID userId, String expoPushToken, String platform, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.expoPushToken = expoPushToken;
        this.platform = platform;
        this.createdAt = createdAt;
        this.isNew = false;
    }
}
```

- [ ] **Step 3: Write the domain repository interface**

`apps/backend-api/src/main/java/com/limitflow/backend/domain/push/PushTokenRepository.java`:

```java
package com.limitflow.backend.domain.push;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PushTokenRepository {

    <S extends PushToken> Mono<S> save(S pushToken);

    Flux<PushToken> findByUserId(UUID userId);

    Mono<Void> deleteByUserIdAndExpoPushToken(UUID userId, String expoPushToken);
}
```

- [ ] **Step 4: Write the R2DBC adapter**

`apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/persistence/PushTokenR2dbcRepository.java`:

```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PushTokenR2dbcRepository extends R2dbcRepository<PushToken, UUID>, PushTokenRepository {

    @Override
    Mono<Void> deleteByUserIdAndExpoPushToken(UUID userId, String expoPushToken);
}
```

- [ ] **Step 5: Write a test confirming the entity's new-vs-existing behavior**

`apps/backend-api/src/test/java/com/limitflow/backend/domain/push/PushTokenTest.java`:

```java
package com.limitflow.backend.domain.push;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PushTokenTest {

    @Test
    void businessConstructorMarksTheTokenAsNew() {
        PushToken token = new PushToken(UUID.randomUUID(), "ExponentPushToken[abc]", "ios");

        assertThat(token.isNew()).isTrue();
    }
}
```

- [ ] **Step 6: Run the test**

Run: `./mvnw -q -B -Dtest=PushTokenTest test` (or via the project's Docker-based Maven
invocation if no local JDK 21 is available — see the reactive-backend plan's Task 1 for the
`docker run eclipse-temurin:21-jdk-alpine` pattern already established in this repo).
Expected: PASS.

- [ ] **Step 7: Compile the whole module**

Run: `./mvnw -q -B -DskipTests package`.
Expected: BUILD SUCCESS, no errors from the new migration/entity/repository.

- [ ] **Step 8: Commit**

```bash
git add apps/backend-api/src/main/resources/db/migration/V4__add_push_tokens.sql \
        apps/backend-api/src/main/java/com/limitflow/backend/domain/push/ \
        apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/persistence/PushTokenR2dbcRepository.java \
        apps/backend-api/src/test/java/com/limitflow/backend/domain/push/
git commit -m "Add push_tokens table and PushToken domain entity"
```

---

### Task 2: Expo push delivery service

**Files:**
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/config/WebClientConfig.java`
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/application/notification/PushNotificationService.java`
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/application/notification/PushNotificationServiceTest.java`

**Interfaces:**
- Consumes: `PushTokenRepository.findByUserId(UUID)` (Task 1).
- Produces: `PushNotificationService.push(UUID userId, String title, String body): Mono<Void>`
  — used by Task 3.

- [ ] **Step 1: Write the failing test**

`apps/backend-api/src/test/java/com/limitflow/backend/application/notification/PushNotificationServiceTest.java`:

```java
package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Test
    void pushSendsToEveryRegisteredTokenForTheUser() {
        UUID userId = UUID.randomUUID();
        PushToken token = new PushToken(userId, "ExponentPushToken[abc]", "ios");
        when(pushTokenRepository.findByUserId(userId)).thenReturn(Flux.just(token));
        RecordingExchangeFunction exchangeFunction = new RecordingExchangeFunction();
        PushNotificationService service = newService(exchangeFunction);

        StepVerifier.create(service.push(userId, "Title", "Body")).verifyComplete();

        assertThat(exchangeFunction.requestCount()).isEqualTo(1);
    }

    @Test
    void pushIsANoOpWhenTheUserHasNoRegisteredToken() {
        UUID userId = UUID.randomUUID();
        when(pushTokenRepository.findByUserId(userId)).thenReturn(Flux.empty());
        RecordingExchangeFunction exchangeFunction = new RecordingExchangeFunction();
        PushNotificationService service = newService(exchangeFunction);

        StepVerifier.create(service.push(userId, "Title", "Body")).verifyComplete();

        assertThat(exchangeFunction.requestCount()).isZero();
    }

    private PushNotificationService newService(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new PushNotificationService(pushTokenRepository, webClient);
    }

    /** A hand-rolled {@link ExchangeFunction} test double — real HTTP mocking libraries
     * aren't a dependency of this project and would be a disproportionate addition for one
     * test class. */
    private static class RecordingExchangeFunction implements ExchangeFunction {

        private int requests = 0;

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            requests++;
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }

        int requestCount() {
            return requests;
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -q -B -Dtest=PushNotificationServiceTest test`
Expected: COMPILE FAILURE — `PushNotificationService` doesn't exist yet.

- [ ] **Step 3: Write the WebClient bean**

`apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/config/WebClientConfig.java`:

```java
package com.limitflow.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient expoPushClient() {
        return WebClient.builder()
                .baseUrl("https://exp.host/--/api/v2/push/send")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
```

- [ ] **Step 4: Write the service**

`apps/backend-api/src/main/java/com/limitflow/backend/application/notification/PushNotificationService.java`:

```java
package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.push.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * The Expo push delivery channel. Every send is fire-and-forget from the caller's
 * perspective — see {@link NotificationService#send} for why failures here must never
 * propagate.
 */
@Service
public class PushNotificationService {

    private final PushTokenRepository pushTokenRepository;
    private final WebClient expoPushClient;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
            @Qualifier("expoPushClient") WebClient expoPushClient) {
        this.pushTokenRepository = pushTokenRepository;
        this.expoPushClient = expoPushClient;
    }

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

One explicit constructor, not `@RequiredArgsConstructor` — the `@Qualifier` disambiguates
which `WebClient` bean to inject (there's only one today, but being explicit costs nothing and
avoids an implicit dependency on bean-count), and an explicit constructor is what the test
above calls directly with its own hand-built `WebClient`, bypassing Spring's DI entirely.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -q -B -Dtest=PushNotificationServiceTest test`
Expected: PASS (2/2).

- [ ] **Step 6: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/config/WebClientConfig.java \
        apps/backend-api/src/main/java/com/limitflow/backend/application/notification/PushNotificationService.java \
        apps/backend-api/src/test/java/com/limitflow/backend/application/notification/PushNotificationServiceTest.java
git commit -m "Add Expo push delivery channel"
```

---

### Task 3: Device registration endpoint + wire push into NotificationService

**Files:**
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/device/PushTokenRequest.java`
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/DeviceApi.java`
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/DeviceController.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/notification/NotificationService.java`
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/application/notification/NotificationServiceTest.java`

**Interfaces:**
- Consumes: `PushNotificationService.push` (Task 2), `PushTokenRepository.save`/
  `deleteByUserIdAndExpoPushToken` (Task 1).
- Produces: `POST /api/devices/push-token`, `DELETE /api/devices/push-token` — used by mobile
  Task 16.

- [ ] **Step 1: Write the DTO**

`apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/device/PushTokenRequest.java`:

```java
package com.limitflow.backend.presentation.dto.device;

import jakarta.validation.constraints.NotBlank;

public record PushTokenRequest(
        @NotBlank String expoPushToken,
        @NotBlank String platform
) {
}
```

- [ ] **Step 2: Write the API interface**

`apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/DeviceApi.java`:

```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.device.PushTokenRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/api/devices")
@Tag(name = "Devices")
public interface DeviceApi {

    @PostMapping("/push-token")
    Mono<Void> register(@AuthenticationPrincipal User user, @Valid @RequestBody PushTokenRequest request);

    @DeleteMapping("/push-token")
    Mono<Void> unregister(@AuthenticationPrincipal User user, @Valid @RequestBody PushTokenRequest request);
}
```

No `@PreAuthorize` — any authenticated role can register a device, matching
`NotificationController`'s pattern (no role restriction there either).

- [ ] **Step 3: Write the controller**

`apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/DeviceController.java`:

```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.device.PushTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceApi {

    private final PushTokenRepository pushTokenRepository;

    @Override
    public Mono<Void> register(User user, PushTokenRequest request) {
        return pushTokenRepository
                .save(new PushToken(user.getId(), request.expoPushToken(), request.platform()))
                .then();
    }

    @Override
    public Mono<Void> unregister(User user, PushTokenRequest request) {
        return pushTokenRepository.deleteByUserIdAndExpoPushToken(user.getId(), request.expoPushToken());
    }
}
```

- [ ] **Step 4: Write the failing NotificationService test**

`apps/backend-api/src/test/java/com/limitflow/backend/application/notification/NotificationServiceTest.java`:

```java
package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import com.limitflow.backend.domain.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationService pushNotificationService;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, pushNotificationService);
    }

    @Test
    void sendStillPersistsTheNotificationWhenThePushCallFails() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pushNotificationService.push(any(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Expo is down")));

        StepVerifier.create(service.send(userId, NotificationType.OTP_SENT, "Title", "Body"))
                .expectNextCount(1)
                .verifyComplete();

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendPushesAfterPersisting() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pushNotificationService.push(any(), anyString(), anyString())).thenReturn(Mono.empty());

        service.send(userId, NotificationType.OTP_SENT, "Title", "Body").block();

        verify(pushNotificationService).push(userId, "Title", "Body");
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./mvnw -q -B -Dtest=NotificationServiceTest test`
Expected: COMPILE FAILURE — `NotificationService`'s constructor doesn't take a
`PushNotificationService` yet.

- [ ] **Step 6: Wire push into NotificationService**

Modify `apps/backend-api/src/main/java/com/limitflow/backend/application/notification/NotificationService.java`
to its full new contents:

```java
package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import com.limitflow.backend.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    /** Persists the in-app notification, then best-effort pushes it to any registered mobile
     * device. A push failure (no token, Expo outage, bad token) must never fail this call —
     * every existing caller (OTP codes, status updates) already treats {@code send} as
     * unconditionally successful once the in-app notification exists. */
    public Mono<Notification> send(UUID userId, NotificationType type, String title, String message) {
        return notificationRepository.save(new Notification(userId, type, title, message))
                .flatMap(saved -> pushNotificationService.push(userId, title, message)
                        .onErrorResume(e -> Mono.empty())
                        .thenReturn(saved));
    }

    public Flux<Notification> findForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./mvnw -q -B -Dtest=NotificationServiceTest,PushNotificationServiceTest test`
Expected: PASS (4/4 across both classes).

- [ ] **Step 8: Run the full backend test suite and compile**

Run: `./mvnw -q -B test` then `./mvnw -q -B -DskipTests package`.
Expected: all existing tests still pass (no other class constructs `NotificationService`
directly other than through Spring DI, so this is a additive-only constructor change) and the
module builds.

- [ ] **Step 9: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/device/ \
        apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/DeviceApi.java \
        apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/DeviceController.java \
        apps/backend-api/src/main/java/com/limitflow/backend/application/notification/NotificationService.java \
        apps/backend-api/src/test/java/com/limitflow/backend/application/notification/NotificationServiceTest.java
git commit -m "Add device push-token registration endpoint, wire push into NotificationService"
```

This is the last backend task. Everything from here on is `apps/mobile`.

---

### Task 4: Expo app scaffold, NativeWind, design tokens

**Files:**
- Create: `apps/mobile/` (via `create-expo-app`, then modified)
- Modify: `apps/mobile/package.json`
- Create: `apps/mobile/tailwind.config.js`
- Create: `apps/mobile/global.css`
- Create: `apps/mobile/babel.config.js`
- Create: `apps/mobile/metro.config.js`
- Modify: `apps/mobile/tsconfig.json`
- Create: `apps/mobile/app.config.ts`
- Create: `apps/mobile/src/app/_layout.tsx` (placeholder root layout — real content in Task 6)
- Modify: `docker-compose.yml` is **not** touched — the mobile app doesn't run in Docker
  (see spec: "try it" is Expo Go / a simulator, not `docker compose up`).

**Interfaces:**
- Produces: the `apps/mobile` project shell every later task adds files into.

- [ ] **Step 1: Scaffold the app**

Run from the repo root:

```bash
npx create-expo-app@latest apps/mobile --template default
```

This gives a current, working Expo Router + TypeScript project — pinning exact dependency
versions by hand here would drift from whatever the real Expo SDK looks like by the time this
plan runs; letting the official CLI resolve current compatible versions is the same reasoning
`customer-portal`'s own `AGENTS.md` gives for tracking bleeding-edge tooling rather than
freezing to what's already known.

- [ ] **Step 2: Install the extra dependencies this app needs**

```bash
cd apps/mobile
npx expo install nativewind tailwindcss@^3 react-native-reanimated react-native-safe-area-context
npx expo install expo-secure-store expo-local-authentication expo-notifications expo-linear-gradient
npm install @tanstack/react-query axios date-fns react-hook-form zod @hookform/resolvers lucide-react-native clsx tailwind-merge
```

`expo install` (not plain `npm install`) for anything with native code, so Expo resolves the
version compatible with the scaffolded SDK. NativeWind pins to Tailwind v3 (its stable major at
time of writing) rather than the v4 `customer-portal` uses — NativeWind's CSS-in-JS transform
doesn't yet track Tailwind v4's `@theme inline` config format, so tokens here are expressed as
a classic `tailwind.config.js`, translated by value from `globals.css` rather than shared by
file. This is a real, deliberate per-app tooling divergence, not an oversight.

- [ ] **Step 3: Write the design tokens as a Tailwind config**

`apps/mobile/tailwind.config.js` — same color names as `customer-portal`, both light and
dark values (NativeWind's `dark:` variant follows the OS color scheme automatically, matching
the web app's `@media (prefers-color-scheme: dark)` behavior):

```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./app/**/*.{js,jsx,ts,tsx}", "./src/**/*.{js,jsx,ts,tsx}"],
  presets: [require("nativewind/preset")],
  darkMode: "media",
  theme: {
    extend: {
      colors: {
        ink: { DEFAULT: "#151222", dark: "#f2f0fa" },
        "ink-muted": { DEFAULT: "#6b6580", dark: "#a199c2" },
        surface: { DEFAULT: "#faf9fe", dark: "#0f0b22" },
        card: { DEFAULT: "#ffffff", dark: "#1b1533" },
        border: { DEFAULT: "#ece8f6", dark: "#2a2350" },
        accent: { DEFAULT: "#5b3df5", dark: "#8b7cff" },
        "accent-deep": { DEFAULT: "#241653", dark: "#150c33" },
        "accent-soft": { DEFAULT: "#efebff", dark: "#241c4a" },
        success: { DEFAULT: "#12b76a", dark: "#3dd68c" },
        "success-soft": { DEFAULT: "#e7f9f0", dark: "#123326" },
        "success-strong": { DEFAULT: "#0a7a4a", dark: "#3dd68c" },
        danger: { DEFAULT: "#e5484d", dark: "#ff6369" },
        "danger-soft": { DEFAULT: "#fdedee", dark: "#3a1518" },
        "danger-strong": { DEFAULT: "#b3231e", dark: "#ff6369" },
        warning: { DEFAULT: "#f5a524", dark: "#ffc24d" },
        "warning-soft": { DEFAULT: "#fef3e2", dark: "#3a2a10" },
        "warning-strong": { DEFAULT: "#8a5a10", dark: "#ffc24d" },
      },
    },
  },
  plugins: [],
};
```

NativeWind resolves `bg-ink` to the `DEFAULT` value and `dark:bg-ink` to `.dark` automatically
via its own dark-mode color object convention — reference each token as `text-ink dark:text-ink-dark`
(NativeWind's per-color dark suffix, not Tailwind's `dark:` prefix on the same class) in every
component built in later tasks. This is the one real ergonomic cost of the version split from
`customer-portal`'s CSS-variable approach — every dark-mode pairing is spelled out per class
instead of swapping a `:root` variable, so later tasks write `className="text-ink dark:text-ink-dark"`
consistently rather than `className="text-ink"` alone.

- [ ] **Step 4: Wire up NativeWind's Babel/Metro/global CSS plumbing**

`apps/mobile/global.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

`apps/mobile/babel.config.js`:

```js
module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      ["babel-preset-expo", { jsxImportSource: "nativewind" }],
      "nativewind/babel",
    ],
  };
};
```

`apps/mobile/metro.config.js`:

```js
const { getDefaultConfig } = require("expo/metro-config");
const { withNativeWind } = require("nativewind/metro");

const config = getDefaultConfig(__dirname);

module.exports = withNativeWind(config, { input: "./global.css" });
```

- [ ] **Step 5: Add the `@/` path alias**

Modify `apps/mobile/tsconfig.json` to include:

```json
{
  "extends": "expo/tsconfig.base",
  "compilerOptions": {
    "strict": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["**/*.ts", "**/*.tsx", ".expo/types/**/*.ts", "expo-env.d.ts"]
}
```

- [ ] **Step 6: Set the API base URL**

`apps/mobile/app.config.ts`:

```ts
import type { ExpoConfig } from "expo/config";

const config: ExpoConfig = {
  name: "LimitFlow",
  slug: "limitflow-mobile",
  scheme: "limitflow",
  extra: {
    apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  },
};

export default config;
```

Referenced later (Task 5) via `Constants.expoConfig?.extra?.apiBaseUrl`.

- [ ] **Step 7: Write a placeholder root layout so the app boots**

`apps/mobile/src/app/_layout.tsx` (replaced with real content in Task 6). Note the two levels
up in the CSS import — Expo Router's real root for this SDK is `src/app/`, not `app/`, so a
file here sits two directories below the mobile package root where `global.css` lives (see
Task 4's report for the confirmed live behavior this reflects):

```tsx
import "../../global.css";
import { Stack } from "expo-router";

export default function RootLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
```

- [ ] **Step 8: Verify the scaffold boots**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.

Run: `npx expo start` and open in Expo Go / a simulator.
Expected: a blank white/dark screen with no red error overlay — this task only proves the
toolchain works, not that there's a real app yet.

- [ ] **Step 9: Commit**

```bash
git add apps/mobile
git commit -m "Scaffold apps/mobile: Expo Router + NativeWind + ported design tokens"
```

---

### Task 5: Shared lib port (types, currency, API client)

**Files:**
- Create: `apps/mobile/src/lib/types.ts`
- Create: `apps/mobile/src/lib/currency.ts`
- Create: `apps/mobile/src/lib/utils.ts`
- Create: `apps/mobile/src/lib/api-client.ts`

**Interfaces:**
- Produces: `apiClient` (axios instance), `ApiError`, `formatCurrency`, `cn`, every type from
  `customer-portal/src/lib/types.ts` — consumed by every screen task from here on.

- [ ] **Step 1: Port the shared types verbatim**

`apps/mobile/src/lib/types.ts` — byte-for-byte identical to
`apps/customer-portal/src/lib/types.ts` (copy it; see Global Constraints on why this isn't
extracted into a shared package yet):

```ts
export type Role = "CUSTOMER" | "SUPPORT_AGENT" | "MANAGER";

export type RequestStatus =
  | "PENDING"
  | "OTP_PENDING"
  | "BIOMETRIC_PENDING"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";

export type TimelineStepStatus = "COMPLETE" | "CURRENT" | "PENDING";

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface LoginResponse {
  token: string;
  user: UserSummary;
}

export interface TimelineStep {
  label: string;
  status: TimelineStepStatus;
}

export interface LimitRequest {
  id: string;
  accountId: string;
  currentLimit: number;
  requestedLimit: number;
  reason: string;
  status: RequestStatus;
  riskLevel: RiskLevel | null;
  createdAt: string;
  updatedAt: string;
  timeline: TimelineStep[];
}

export interface AccountSummary {
  id: string;
  accountNumber: string;
  dailyLimit: number;
  usedToday: number;
  remaining: number;
  status: "ACTIVE" | "SUSPENDED";
}

export interface CurrentLimitResponse {
  accountId: string;
  dailyLimit: number;
  usedToday: number;
  remaining: number;
  activeRequest: LimitRequest | null;
}

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

const ACTIVE_STATUSES: RequestStatus[] = ["PENDING", "OTP_PENDING", "BIOMETRIC_PENDING", "UNDER_REVIEW"];

export function isActiveStatus(status: RequestStatus): boolean {
  return ACTIVE_STATUSES.includes(status);
}
```

- [ ] **Step 2: Port currency formatting verbatim**

`apps/mobile/src/lib/currency.ts` — identical to `apps/customer-portal/src/lib/currency.ts`:

```ts
const formatter = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  maximumFractionDigits: 0,
});

const THIN_SPACE = " ";

export function formatCurrency(amount: number): string {
  // en-NG glues the ₦ symbol directly to the first digit with no space, which reads as
  // overlapping glyphs in the tabular-mono figures used throughout the app. A thin space
  // gives the symbol breathing room without visibly widening the figure.
  return formatter
    .formatToParts(amount)
    .map((part) => (part.type === "currency" ? part.value + THIN_SPACE : part.value))
    .join("");
}
```

- [ ] **Step 3: Port the className helper**

`apps/mobile/src/lib/utils.ts`:

```ts
import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 4: Write the API client (secure-store instead of cookies)**

`apps/mobile/src/lib/api-client.ts`:

```ts
import axios, { AxiosError } from "axios";
import Constants from "expo-constants";
import * as SecureStore from "expo-secure-store";

import type { ApiErrorBody } from "./types";

export const AUTH_TOKEN_KEY = "limitflow_customer_token";
export const AUTH_USER_KEY = "limitflow_customer_user";

export class ApiError extends Error {
  status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.status = status;
  }
}

export const apiClient = axios.create({
  baseURL: (Constants.expoConfig?.extra?.apiBaseUrl as string) ?? "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorBody>) => {
    if (error.response?.status === 401) {
      await SecureStore.deleteItemAsync(AUTH_TOKEN_KEY);
      await SecureStore.deleteItemAsync(AUTH_USER_KEY);
      // Task 6's auth context listens for this via its own 401 handling on each query,
      // matching the web app's redirect-to-login-on-401 behavior without a global router
      // reference here (there isn't a `window.location` equivalent to reach for).
    }

    const message =
      error.response?.data?.message ??
      (error.code === "ECONNABORTED" || (error.message ?? "").includes("Network")
        ? "We couldn't reach LimitFlow. Check your connection and try again."
        : "Something went wrong. Please try again.");

    return Promise.reject(new ApiError(message, error.response?.status));
  },
);
```

Also run `npx expo install expo-constants` if it wasn't already pulled in transitively by the
Expo template (recent templates include it by default — check `package.json` first).

- [ ] **Step 5: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add apps/mobile/src/lib
git commit -m "Port shared types, currency formatting, and API client to mobile"
```

---

### Task 6: Auth (login, secure token storage, biometric app-unlock)

**Files:**
- Create: `apps/mobile/src/lib/auth.tsx`
- Create: `apps/mobile/src/app/(auth)/login.tsx`
- Modify: `apps/mobile/src/app/_layout.tsx`
- Create: `apps/mobile/src/app/(app)/_layout.tsx` (auth+unlock gate; tab bar itself is Task 10)

**Interfaces:**
- Consumes: `apiClient`, `ApiError`, `LoginResponse`, `UserSummary` (Task 5).
- Produces: `AuthProvider`, `useAuth()` returning
  `{ user, isReady, isUnlocked, login, logout, unlock }` — consumed by every screen task.

- [ ] **Step 1: Write the auth context**

`apps/mobile/src/lib/auth.tsx`:

```tsx
import * as LocalAuthentication from "expo-local-authentication";
import * as SecureStore from "expo-secure-store";
import { useRouter } from "expo-router";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

import { apiClient, AUTH_TOKEN_KEY, AUTH_USER_KEY } from "./api-client";
import type { LoginResponse, UserSummary } from "./types";

interface AuthContextValue {
  user: UserSummary | null;
  isReady: boolean;
  isUnlocked: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  unlock: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isReady, setIsReady] = useState(false);
  const [isUnlocked, setIsUnlocked] = useState(false);

  useEffect(() => {
    (async () => {
      const raw = await SecureStore.getItemAsync(AUTH_USER_KEY);
      if (raw) {
        try {
          setUser(JSON.parse(raw) as UserSummary);
        } catch {
          setUser(null);
        }
      }
      setIsReady(true);
    })();
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<LoginResponse>("/auth/login", { email, password });
    const { token, user: loggedInUser } = response.data;

    if (loggedInUser.role !== "CUSTOMER") {
      throw new Error("This app is for customer accounts only.");
    }

    await SecureStore.setItemAsync(AUTH_TOKEN_KEY, token);
    await SecureStore.setItemAsync(AUTH_USER_KEY, JSON.stringify(loggedInUser));
    setUser(loggedInUser);
    setIsUnlocked(true);
  }, []);

  const logout = useCallback(async () => {
    await SecureStore.deleteItemAsync(AUTH_TOKEN_KEY);
    await SecureStore.deleteItemAsync(AUTH_USER_KEY);
    setUser(null);
    setIsUnlocked(false);
    router.replace("/login");
  }, [router]);

  /** Returns whether the app is now unlocked. Falls back to "unlocked" if the device has no
   * enrolled biometrics/passcode at all — that's a real device state, not an edge case, and
   * there is nothing left to gate with in that case (see the design spec). */
  const unlock = useCallback(async () => {
    const hasHardware = await LocalAuthentication.hasHardwareAsync();
    const isEnrolled = await LocalAuthentication.isEnrolledAsync();
    if (!hasHardware || !isEnrolled) {
      setIsUnlocked(true);
      return true;
    }
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: "Unlock LimitFlow",
      });
      setIsUnlocked(result.success);
      return result.success;
    } catch {
      // A native-level throw is rare but must resolve to "not unlocked" rather than reject —
      // the caller (the (app) gate layout) has no catch of its own and needs a boolean it can
      // react to, the same as an ordinary failed/cancelled prompt.
      setIsUnlocked(false);
      return false;
    }
  }, []);

  const value = useMemo(
    () => ({ user, isReady, isUnlocked, login, logout, unlock }),
    [user, isReady, isUnlocked, login, logout, unlock],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
```

- [ ] **Step 2: Write the login screen**

`apps/mobile/src/app/(auth)/login.tsx`:

```tsx
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "expo-router";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { Text, View } from "react-native";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Enter your password"),
});

type LoginValues = z.infer<typeof loginSchema>;

export default function LoginScreen() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "customer@limitflow.demo", password: "" },
  });

  const onSubmit = async (values: LoginValues) => {
    setServerError(null);
    try {
      await login(values.email, values.password);
      router.replace("/(app)");
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : (error as Error).message);
    }
  };

  return (
    <View className="flex-1 justify-center bg-surface px-4 dark:bg-surface-dark">
      <View className="mb-8 items-center gap-2">
        <View className="h-12 w-12 items-center justify-center rounded-2xl bg-accent dark:bg-accent-dark">
          <Text className="text-xl font-bold text-white">LF</Text>
        </View>
        <Text className="text-2xl font-semibold text-ink dark:text-ink-dark">LimitFlow</Text>
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">
          Sign in to manage your transfer limit.
        </Text>
      </View>

      <View className="gap-4">
        <View className="gap-1.5">
          <Label>Email</Label>
          <Controller
            control={control}
            name="email"
            render={({ field }) => (
              <Input
                value={field.value}
                onChangeText={field.onChange}
                autoCapitalize="none"
                keyboardType="email-address"
                accessibilityLabel="Email"
              />
            )}
          />
          {errors.email && <Text className="text-xs text-danger dark:text-danger-dark">{errors.email.message}</Text>}
        </View>

        <View className="gap-1.5">
          <Label>Password</Label>
          <Controller
            control={control}
            name="password"
            render={({ field }) => (
              <Input
                value={field.value}
                onChangeText={field.onChange}
                secureTextEntry
                accessibilityLabel="Password"
              />
            )}
          />
          {errors.password && (
            <Text className="text-xs text-danger dark:text-danger-dark">{errors.password.message}</Text>
          )}
        </View>

        {serverError && <Text className="text-sm text-danger dark:text-danger-dark">{serverError}</Text>}

        <Button loading={isSubmitting} onPress={handleSubmit(onSubmit)}>
          {isSubmitting ? "Signing in…" : "Sign in"}
        </Button>
      </View>

      <Text className="mt-6 text-center text-xs text-ink-muted dark:text-ink-muted-dark">
        Demo account: customer@limitflow.demo — password Password123!
      </Text>
    </View>
  );
}
```

(Uses `Button`, `Input`, `Label` from Tasks 7–8 — write this screen's file now, but its
`tsc --noEmit` check won't be clean until those exist. That's fine: Step 5 below is deferred
to the end of Task 8, noted there.)

- [ ] **Step 3: Write the root layout (providers) and the authenticated-app gate**

`apps/mobile/src/app/_layout.tsx`:

```tsx
import "../../global.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";

import { AuthProvider } from "@/lib/auth";
import { ToastProvider } from "@/components/ui/toast";

const queryClient = new QueryClient();

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <Stack screenOptions={{ headerShown: false }} />
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
```

`apps/mobile/src/app/(app)/_layout.tsx` — the login-required + biometric-unlock gate, mirroring
`customer-portal/src/app/(portal)/layout.tsx`'s redirect-when-unauthenticated logic, plus the
mobile-only unlock step:

```tsx
import { Redirect, Slot } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, Text, View } from "react-native";

import { useAuth } from "@/lib/auth";

export default function AppGateLayout() {
  const { user, isReady, isUnlocked, unlock } = useAuth();
  const [unlockFailed, setUnlockFailed] = useState(false);

  async function attemptUnlock() {
    setUnlockFailed(false);
    const success = await unlock();
    if (!success) {
      setUnlockFailed(true);
    }
  }

  useEffect(() => {
    if (isReady && user && !isUnlocked) {
      attemptUnlock();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- attemptUnlock intentionally
    // omitted: it's re-created every render, and including it would re-run this effect (and
    // re-trigger the biometric prompt) on every unrelated re-render of this component.
  }, [isReady, user, isUnlocked]);

  if (!isReady) {
    return (
      <View className="flex-1 items-center justify-center bg-surface dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Loading…</Text>
      </View>
    );
  }

  if (!user) {
    return <Redirect href="/login" />;
  }

  if (!isUnlocked) {
    return (
      <View className="flex-1 items-center justify-center gap-4 bg-surface px-4 dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">
          Unlock LimitFlow to continue.
        </Text>
        {unlockFailed && (
          <Pressable onPress={attemptUnlock}>
            <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
          </Pressable>
        )}
      </View>
    );
  }

  // A layout's default export renders its matched nested route via <Slot />, not a `children`
  // prop — Expo Router (React Navigation underneath) never passes one; that's a Next.js
  // App Router convention, not this one. Task 10 replaces this whole function with a real
  // <Tabs> navigator, which renders its matched screen itself and won't need <Slot /> either.
  return <Slot />;
}
```

`unlock()` (from `apps/mobile/src/lib/auth.tsx`, Step 1 above) must wrap its call to
`LocalAuthentication.authenticateAsync` in a `try/catch`, resolving to `false` on a thrown
error exactly like a failed/cancelled prompt — a native-level throw here must never leave this
component's `attemptUnlock` awaiting a rejected promise with no caught state to react to.

Note: this file's real tab-bar content (`Tabs` from `expo-router`) is written in Task 10 — this
step only establishes the gate itself. Task 10 replaces the final `<Slot />` branch with a real
`<Tabs>` navigator (which renders its own matched screen directly, the same way `<Slot />` does
here), keeping the loading/redirect/unlock-prompt branches above it unchanged.

- [ ] **Step 4: Move the login route**

Confirm `apps/mobile/src/app/(auth)/login.tsx` exists (Step 2) and there's a route group boundary —
Expo Router treats `(auth)` and `(app)` as parallel unauthenticated/authenticated route groups,
matching the Next.js `(portal)` route-group convention `customer-portal` already uses for the
same purpose.

- [ ] **Step 5: Verify (after Task 8 completes UI primitives)**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: clean once `Button`/`Input`/`Label` exist (Tasks 7–8). Flag this task's reviewer
package with a note: "Step 5 verification intentionally deferred — rerun `tsc --noEmit` after
Task 8 lands and confirm this task's files introduce no errors at that point."

- [ ] **Step 6: Commit**

```bash
git add apps/mobile/src/lib/auth.tsx "apps/mobile/src/app/(auth)" "apps/mobile/src/app/(app)/_layout.tsx" apps/mobile/src/app/_layout.tsx
git commit -m "Add mobile auth: login, secure token storage, biometric app-unlock gate"
```

---

### Task 7: UI primitives — Button, Card, Label, Badge

**Files:**
- Create: `apps/mobile/src/components/ui/button.tsx`
- Create: `apps/mobile/src/components/ui/card.tsx`
- Create: `apps/mobile/src/components/ui/label.tsx`
- Create: `apps/mobile/src/components/ui/badge.tsx`

**Interfaces:**
- Produces: `Button`, `Card`/`CardHeader`/`CardTitle`/`CardContent`, `Label`, `Badge` — every
  later screen task depends on these.

- [ ] **Step 1: Button**

`apps/mobile/src/components/ui/button.tsx` — React Native has no native `disabled`-aware
hover/focus states or `asChild`/`Slot` composition, so this drops both (nothing in this app
composes a `Button` into a `Link` the way `customer-portal`'s dashboard CTA does — Task 11
handles that case directly with `Pressable` wrapped in a `Link`, not through `Button asChild`):

```tsx
import { ActivityIndicator, Pressable, Text, type PressableProps } from "react-native";

import { cn } from "@/lib/utils";

type Variant = "default" | "outline" | "ghost" | "destructive";

const VARIANT_CLASSES: Record<Variant, string> = {
  default: "bg-accent dark:bg-accent-dark",
  outline: "border border-border bg-card dark:border-border-dark dark:bg-card-dark",
  ghost: "bg-transparent",
  destructive: "bg-danger dark:bg-danger-dark",
};

const VARIANT_TEXT_CLASSES: Record<Variant, string> = {
  default: "text-white",
  outline: "text-ink dark:text-ink-dark",
  ghost: "text-ink-muted dark:text-ink-muted-dark",
  destructive: "text-white",
};

export interface ButtonProps extends Omit<PressableProps, "children"> {
  variant?: Variant;
  loading?: boolean;
  children: React.ReactNode;
  className?: string;
}

export function Button({
  variant = "default",
  loading = false,
  disabled,
  children,
  className,
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      className={cn(
        "h-10 flex-row items-center justify-center gap-2 rounded-lg px-4",
        VARIANT_CLASSES[variant],
        isDisabled && "opacity-50",
        className,
      )}
      {...props}
    >
      {loading && <ActivityIndicator size="small" color={variant === "outline" || variant === "ghost" ? "#5b3df5" : "#ffffff"} />}
      {typeof children === "string" ? (
        <Text className={cn("text-sm font-medium", VARIANT_TEXT_CLASSES[variant])}>{children}</Text>
      ) : (
        children
      )}
    </Pressable>
  );
}
```

- [ ] **Step 2: Card**

`apps/mobile/src/components/ui/card.tsx`:

```tsx
import { Text, View, type TextProps, type ViewProps } from "react-native";

import { cn } from "@/lib/utils";

export function Card({ className, ...props }: ViewProps & { className?: string }) {
  return (
    <View
      className={cn("rounded-2xl border border-border bg-card dark:border-border-dark dark:bg-card-dark", className)}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("gap-1 p-6 pb-0", className)} {...props} />;
}

export function CardTitle({ className, ...props }: TextProps & { className?: string }) {
  return <Text className={cn("text-lg font-semibold text-ink dark:text-ink-dark", className)} {...props} />;
}

export function CardContent({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("p-6", className)} {...props} />;
}
```

- [ ] **Step 3: Label**

`apps/mobile/src/components/ui/label.tsx` — React Native has no `htmlFor`/`id` association;
`accessibilityLabel` on the paired `Input` (already used throughout Task 6/11's screens) is
the mobile equivalent, so this is just styled text:

```tsx
import { Text, type TextProps } from "react-native";

import { cn } from "@/lib/utils";

export function Label({ className, ...props }: TextProps & { className?: string }) {
  return <Text className={cn("text-sm font-medium text-ink dark:text-ink-dark", className)} {...props} />;
}
```

- [ ] **Step 4: Badge**

`apps/mobile/src/components/ui/badge.tsx`:

```tsx
import { Text, View } from "react-native";

import { cn } from "@/lib/utils";

type Variant = "neutral" | "blue" | "green" | "orange" | "red";

const VARIANT_CLASSES: Record<Variant, string> = {
  neutral: "bg-border dark:bg-border-dark",
  blue: "bg-accent-soft dark:bg-accent-soft-dark",
  green: "bg-success-soft dark:bg-success-soft-dark",
  orange: "bg-warning-soft dark:bg-warning-soft-dark",
  red: "bg-danger-soft dark:bg-danger-soft-dark",
};

const VARIANT_TEXT_CLASSES: Record<Variant, string> = {
  neutral: "text-ink-muted dark:text-ink-muted-dark",
  blue: "text-accent dark:text-accent-dark",
  green: "text-success-strong dark:text-success-strong-dark",
  orange: "text-warning-strong dark:text-warning-strong-dark",
  red: "text-danger-strong dark:text-danger-strong-dark",
};

export function Badge({ variant = "neutral", children }: { variant?: Variant; children: React.ReactNode }) {
  return (
    <View className={cn("rounded-full px-2.5 py-0.5", VARIANT_CLASSES[variant])}>
      <Text className={cn("text-xs font-semibold", VARIANT_TEXT_CLASSES[variant])}>{children}</Text>
    </View>
  );
}
```

- [ ] **Step 5: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no new errors from these four files (the login screen from Task 6 may still show
errors for `Input`/`Textarea`/`Checkbox`, resolved in Task 8).

- [ ] **Step 6: Commit**

```bash
git add apps/mobile/src/components/ui/button.tsx apps/mobile/src/components/ui/card.tsx \
        apps/mobile/src/components/ui/label.tsx apps/mobile/src/components/ui/badge.tsx
git commit -m "Add mobile UI primitives: Button, Card, Label, Badge"
```

---

### Task 8: UI primitives — Input, Textarea, Checkbox, Skeleton

**Files:**
- Create: `apps/mobile/src/components/ui/input.tsx`
- Create: `apps/mobile/src/components/ui/textarea.tsx`
- Create: `apps/mobile/src/components/ui/checkbox.tsx`
- Create: `apps/mobile/src/components/ui/skeleton.tsx`

**Interfaces:**
- Produces: `Input`, `Textarea`, `Checkbox`, `Skeleton`.

- [ ] **Step 1: Input**

`apps/mobile/src/components/ui/input.tsx` — React Native's `TextInput` typings don't
guarantee an `aria-invalid` prop the way a DOM `<input>` does, so this takes an explicit
`invalid` prop instead of gambling on an uncertain passthrough attribute. There's no
`aria-describedby` equivalent either; the adjacent error text (already written next to every
`Input` usage) is the accessibility link, read naturally in document order by both VoiceOver
and TalkBack:

```tsx
import { TextInput, type TextInputProps } from "react-native";

import { cn } from "@/lib/utils";

export interface InputProps extends TextInputProps {
  className?: string;
  invalid?: boolean;
}

export function Input({ className, invalid, ...props }: InputProps) {
  return (
    <TextInput
      className={cn(
        "h-10 rounded-lg border border-border bg-card px-3 text-sm text-ink dark:border-border-dark dark:bg-card-dark dark:text-ink-dark",
        invalid && "border-danger dark:border-danger-dark",
        className,
      )}
      placeholderTextColor="#6b6580"
      {...props}
    />
  );
}
```

- [ ] **Step 2: Textarea**

`apps/mobile/src/components/ui/textarea.tsx`:

```tsx
import { TextInput, type TextInputProps } from "react-native";

import { cn } from "@/lib/utils";

export function Textarea({ className, ...props }: TextInputProps & { className?: string }) {
  return (
    <TextInput
      multiline
      textAlignVertical="top"
      className={cn(
        "min-h-[90px] rounded-lg border border-border bg-card px-3 py-2 text-sm text-ink dark:border-border-dark dark:bg-card-dark dark:text-ink-dark",
        className,
      )}
      placeholderTextColor="#6b6580"
      {...props}
    />
  );
}
```

- [ ] **Step 3: Checkbox**

`apps/mobile/src/components/ui/checkbox.tsx` — no native RN checkbox; `Pressable` + a
controlled `checked`/`onValueChange` pair (the RN-idiomatic naming, cleaner than porting the
DOM `onChange={(e) => e.target.checked}` shape):

```tsx
import { Check } from "lucide-react-native";
import { Pressable, View } from "react-native";

import { cn } from "@/lib/utils";

export function Checkbox({
  checked,
  onValueChange,
}: {
  checked: boolean;
  onValueChange: (value: boolean) => void;
}) {
  return (
    <Pressable
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      onPress={() => onValueChange(!checked)}
      className={cn(
        "h-4 w-4 items-center justify-center rounded border border-border bg-card dark:border-border-dark dark:bg-card-dark",
        checked && "border-accent bg-accent dark:border-accent-dark dark:bg-accent-dark",
      )}
    >
      {checked && <Check size={12} color="#ffffff" />}
    </Pressable>
  );
}
```

- [ ] **Step 4: Skeleton**

`apps/mobile/src/components/ui/skeleton.tsx`:

```tsx
import { View, type ViewProps } from "react-native";

import { cn } from "@/lib/utils";

export function Skeleton({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("rounded-md bg-border opacity-70 dark:bg-border-dark", className)} {...props} />;
}
```

(React Native has no CSS `animate-pulse`; a static muted block is the honest simplification —
`react-native-reanimated` could drive a real pulse loop, but that's meaningfully more code for a
loading placeholder nobody in this demo stares at for long. Note this as a deliberate
simplification, not an oversight, same spirit as the dropped blur-glow in Task 11.)

- [ ] **Step 5: Verify — this closes out Task 6's deferred check too**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: clean, including the Task 6 login screen now that `Input`/`Label`/`Button` all
exist.

- [ ] **Step 6: Commit**

```bash
git add apps/mobile/src/components/ui/input.tsx apps/mobile/src/components/ui/textarea.tsx \
        apps/mobile/src/components/ui/checkbox.tsx apps/mobile/src/components/ui/skeleton.tsx
git commit -m "Add mobile UI primitives: Input, Textarea, Checkbox, Skeleton"
```

---

### Task 9: UI primitives — Dialog, Toast

**Files:**
- Create: `apps/mobile/src/components/ui/dialog.tsx`
- Create: `apps/mobile/src/components/ui/toast.tsx`

**Interfaces:**
- Produces: `Dialog`/`DialogContent`/`DialogHeader`/`DialogTitle`/`DialogDescription`/`DialogFooter`,
  `ToastProvider`/`useToast` — consumed by Task 12 (cancel-confirmation) and every mutation
  across the app.

- [ ] **Step 1: Dialog**

`apps/mobile/src/components/ui/dialog.tsx` — RN's built-in `Modal` component is the direct
equivalent of a Radix `Dialog` portal + overlay:

```tsx
import { Modal, Pressable, Text, View, type ViewProps } from "react-native";

import { cn } from "@/lib/utils";

export function Dialog({
  open,
  onOpenChange,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
}) {
  return (
    <Modal visible={open} transparent animationType="fade" onRequestClose={() => onOpenChange(false)}>
      <Pressable
        className="flex-1 items-center justify-center bg-ink/40 px-4"
        onPress={() => onOpenChange(false)}
      >
        {/* An empty onPress (not stopPropagation, which GestureResponderEvent doesn't
            reliably expose) makes this Pressable claim the touch responder for taps inside
            the dialog card, so they don't fall through to the scrim's dismiss handler. */}
        <Pressable onPress={() => {}} className="w-full max-w-md">
          {children}
        </Pressable>
      </Pressable>
    </Modal>
  );
}

export function DialogContent({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("rounded-2xl bg-card p-6 dark:bg-card-dark", className)} {...props} />;
}

export function DialogHeader({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("mb-4 gap-1", className)} {...props} />;
}

export function DialogTitle({ children }: { children: React.ReactNode }) {
  return <Text className="text-lg font-semibold text-ink dark:text-ink-dark">{children}</Text>;
}

export function DialogDescription({ children }: { children: React.ReactNode }) {
  return <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{children}</Text>;
}

export function DialogFooter({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("mt-6 flex-row justify-end gap-2", className)} {...props} />;
}
```

No separate close (×) button — tapping the scrim dismisses it, and every dialog in this app
(cancel-confirmation, Task 12) also has an explicit "Keep request" button, so a redundant close
icon isn't needed the way the web version's standalone dialog sometimes is.

- [ ] **Step 2: Toast**

`apps/mobile/src/components/ui/toast.tsx` — RN's accessibility announcement equivalent of
`role="alert"` vs `role="status"` is `AccessibilityInfo.announceForAccessibility`, fired
immediately for error toasts (assertive) and left to be discovered visually for success ones
(polite, matching the web behavior exactly):

```tsx
import { AccessibilityInfo, Pressable, Text, View } from "react-native";
import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { X } from "lucide-react-native";

import { cn } from "@/lib/utils";

interface Toast {
  id: number;
  title: string;
  variant: "success" | "error";
}

interface ToastContextValue {
  toast: (title: string, variant?: Toast["variant"]) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback(
    (title: string, variant: Toast["variant"] = "success") => {
      const id = nextId++;
      setToasts((current) => [...current, { id, title, variant }]);
      if (variant === "error") {
        AccessibilityInfo.announceForAccessibility(title);
      }
      setTimeout(() => dismiss(id), 4000);
    },
    [dismiss],
  );

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <View pointerEvents="box-none" className="absolute inset-x-4 bottom-24 gap-2">
        {toasts.map((t) => (
          <View
            key={t.id}
            className={cn(
              "flex-row items-center gap-2 rounded-lg border px-4 py-3",
              t.variant === "success"
                ? "border-success/30 bg-success-soft dark:bg-success-soft-dark"
                : "border-danger/30 bg-danger-soft dark:bg-danger-soft-dark",
            )}
          >
            <Text
              className={cn(
                "flex-1 text-sm",
                t.variant === "success"
                  ? "text-success-strong dark:text-success-strong-dark"
                  : "text-danger-strong dark:text-danger-strong-dark",
              )}
            >
              {t.title}
            </Text>
            <Pressable onPress={() => dismiss(t.id)} accessibilityLabel="Dismiss">
              <X size={14} color="#6b6580" />
            </Pressable>
          </View>
        ))}
      </View>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
```

- [ ] **Step 3: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: clean.

- [ ] **Step 4: Commit**

```bash
git add apps/mobile/src/components/ui/dialog.tsx apps/mobile/src/components/ui/toast.tsx
git commit -m "Add mobile UI primitives: Dialog, Toast"
```

---

### Task 10: Tab navigation shell

**Files:**
- Modify: `apps/mobile/src/app/(app)/_layout.tsx`
- Create: `apps/mobile/src/components/layout/header.tsx`

**Interfaces:**
- Produces: the five-tab shell (Home / Increase / Requests / Alerts / Profile) every screen
  task renders inside.

- [ ] **Step 1: Write the header**

`apps/mobile/src/components/layout/header.tsx` — mirrors `topbar.tsx`'s "Hi, {name}" + logout
icon:

```tsx
import { LogOut } from "lucide-react-native";
import { Pressable, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { useAuth } from "@/lib/auth";

export function Header() {
  const { user, logout } = useAuth();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={{ paddingTop: insets.top }}
      className="flex-row items-center justify-between border-b border-border bg-card px-4 pb-3 dark:border-border-dark dark:bg-card-dark"
    >
      <View className="flex-row items-center gap-2">
        <View className="h-8 w-8 items-center justify-center rounded-xl bg-accent dark:bg-accent-dark">
          <Text className="text-xs font-bold text-white">LF</Text>
        </View>
        <Text className="text-sm font-medium text-ink dark:text-ink-dark">Hi, {user?.firstName}</Text>
      </View>
      <Pressable accessibilityLabel="Log out" onPress={logout}>
        <LogOut size={18} color="#6b6580" />
      </Pressable>
    </View>
  );
}
```

- [ ] **Step 2: Rewrite the app-group layout with the real tab bar**

Full new contents of `apps/mobile/src/app/(app)/_layout.tsx`:

```tsx
import { Bell, ClipboardList, LayoutDashboard, ArrowUpCircle, User } from "lucide-react-native";
import { Redirect, Tabs } from "expo-router";
import { useEffect } from "react";
import { Text, View } from "react-native";

import { Header } from "@/components/layout/header";
import { useAuth } from "@/lib/auth";

export default function AppGateLayout() {
  const { user, isReady, isUnlocked, unlock } = useAuth();

  useEffect(() => {
    if (isReady && user && !isUnlocked) {
      unlock();
    }
  }, [isReady, user, isUnlocked, unlock]);

  if (!isReady) {
    return (
      <View className="flex-1 items-center justify-center bg-surface dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Loading…</Text>
      </View>
    );
  }

  if (!user) {
    return <Redirect href="/login" />;
  }

  if (!isUnlocked) {
    return (
      <View className="flex-1 items-center justify-center gap-4 bg-surface px-4 dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Unlock LimitFlow to continue.</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-surface dark:bg-surface-dark">
      <Header />
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarActiveTintColor: "#5b3df5",
          tabBarInactiveTintColor: "#6b6580",
        }}
      >
        <Tabs.Screen name="index" options={{ title: "Home", tabBarIcon: ({ color, size }) => <LayoutDashboard color={color} size={size} /> }} />
        <Tabs.Screen name="increase-limit" options={{ title: "Increase", tabBarIcon: ({ color, size }) => <ArrowUpCircle color={color} size={size} /> }} />
        <Tabs.Screen name="requests" options={{ title: "Requests", tabBarIcon: ({ color, size }) => <ClipboardList color={color} size={size} /> }} />
        <Tabs.Screen name="notifications" options={{ title: "Alerts", tabBarIcon: ({ color, size }) => <Bell color={color} size={size} /> }} />
        <Tabs.Screen name="profile" options={{ title: "Profile", tabBarIcon: ({ color, size }) => <User color={color} size={size} /> }} />
        <Tabs.Screen name="support" options={{ href: null }} />
        <Tabs.Screen name="requests/[id]" options={{ href: null }} />
      </Tabs>
    </View>
  );
}
```

`support` and `requests/[id]` are registered with `href: null` so Expo Router's file-based
routing finds them (they exist as files under `(app)/`) without adding a sixth/seventh visible
tab — reachable via `Link`/`router.push`, exactly like `customer-portal`'s `/support` (linked
from Profile, not in the bottom nav) and `/requests/[id]` (linked from the list, not a tab
itself).

- [ ] **Step 3: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: errors here are expected and fine until Tasks 11–15 create the referenced screen
files (`index.tsx`, `increase-limit.tsx`, `requests/index.tsx`, etc.) — Expo Router itself
doesn't type-error on a missing route file, but this task's own new code (`Header`, the layout)
must show zero errors in isolation. Confirm that specifically.

- [ ] **Step 4: Commit**

```bash
git add "apps/mobile/src/app/(app)/_layout.tsx" apps/mobile/src/components/layout/header.tsx
git commit -m "Add mobile tab navigation shell"
```

---

### Task 11: Dashboard screen

**Files:**
- Create: `apps/mobile/src/hooks/use-dashboard.ts`
- Create: `apps/mobile/src/components/dashboard/limit-summary-card.tsx`
- Create: `apps/mobile/src/components/dashboard/active-request-banner.tsx`
- Create: `apps/mobile/src/components/requests/status-badge.tsx`
- Create: `apps/mobile/src/app/(app)/index.tsx`

**Interfaces:**
- Consumes: `apiClient`, `formatCurrency`, `Badge`, `Card`, `CardContent`, `Skeleton`,
  `useAuth` (Tasks 5–8).
- Produces: `useCurrentLimitQuery` — reused by Task 12's wizard.

- [ ] **Step 1: Port the query hook verbatim**

`apps/mobile/src/hooks/use-dashboard.ts` — identical logic to
`customer-portal/src/hooks/use-dashboard.ts`:

```ts
import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { CurrentLimitResponse } from "@/lib/types";

export function useCurrentLimitQuery() {
  return useQuery({
    queryKey: ["limits", "current"],
    queryFn: async () => {
      const response = await apiClient.get<CurrentLimitResponse>("/limits/current");
      return response.data;
    },
  });
}
```

- [ ] **Step 2: Status badge**

`apps/mobile/src/components/requests/status-badge.tsx`:

```tsx
import { Badge } from "@/components/ui/badge";
import type { RequestStatus } from "@/lib/types";

const LABELS: Record<RequestStatus, string> = {
  PENDING: "Pending",
  OTP_PENDING: "Verifying code",
  BIOMETRIC_PENDING: "Verifying identity",
  UNDER_REVIEW: "Under review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

const VARIANTS: Record<RequestStatus, "neutral" | "blue" | "green" | "orange" | "red"> = {
  PENDING: "blue",
  OTP_PENDING: "blue",
  BIOMETRIC_PENDING: "blue",
  UNDER_REVIEW: "orange",
  APPROVED: "green",
  REJECTED: "red",
  CANCELLED: "neutral",
};

export function StatusBadge({ status }: { status: RequestStatus }) {
  return <Badge variant={VARIANTS[status]}>{LABELS[status]}</Badge>;
}
```

- [ ] **Step 3: Limit summary card, with a real gradient**

`apps/mobile/src/components/dashboard/limit-summary-card.tsx` — the blurred ambient glow
decoration from the web version is dropped (React Native has no CSS `blur-filter`; approximating
it would need `expo-blur`'s `<BlurView>`, which blurs *content behind it* as a frosted panel —
a different, heavier effect than a soft glow, not worth adding a dependency for a purely
decorative flourish). The gradient itself is the one thing worth a real native equivalent:

```tsx
import { LinearGradient } from "expo-linear-gradient";
import { Text, View } from "react-native";

import { formatCurrency } from "@/lib/currency";

export function LimitSummaryCard({
  dailyLimit,
  usedToday,
  remaining,
}: {
  dailyLimit: number;
  usedToday: number;
  remaining: number;
}) {
  const usedPct = dailyLimit > 0 ? Math.min(100, Math.round((usedToday / dailyLimit) * 100)) : 0;

  return (
    <LinearGradient
      colors={["#5b3df5", "#241653"]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={{ borderRadius: 16, padding: 20 }}
    >
      <Text className="text-sm text-white/70">Daily transfer limit</Text>
      <Text className="mt-1 text-3xl font-semibold text-white">{formatCurrency(dailyLimit)}</Text>

      <View className="mt-4 h-2 w-full overflow-hidden rounded-full bg-white/20">
        <View className="h-full rounded-full bg-white" style={{ width: `${usedPct}%` }} />
      </View>
      <View className="mt-2 flex-row justify-between">
        <Text className="text-xs text-white/70">{formatCurrency(usedToday)} used today</Text>
        <Text className="text-xs text-white/70">{formatCurrency(remaining)} remaining</Text>
      </View>
    </LinearGradient>
  );
}
```

- [ ] **Step 4: Active request banner**

`apps/mobile/src/components/dashboard/active-request-banner.tsx`:

```tsx
import { Link } from "expo-router";
import { Pressable, Text, View } from "react-native";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function ActiveRequestBanner({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`} asChild>
      <Pressable className="flex-row items-center justify-between gap-3 rounded-2xl border border-accent/20 bg-accent-soft p-4 dark:bg-accent-soft-dark">
        <View>
          <Text className="text-sm font-medium text-ink dark:text-ink-dark">
            Request for {formatCurrency(request.requestedLimit)}
          </Text>
          <Text className="text-xs text-ink-muted dark:text-ink-muted-dark">Tap to see progress</Text>
        </View>
        <StatusBadge status={request.status} />
      </Pressable>
    </Link>
  );
}
```

- [ ] **Step 5: Dashboard screen**

`apps/mobile/src/app/(app)/index.tsx`:

```tsx
import { ArrowUpCircle } from "lucide-react-native";
import { Link } from "expo-router";
import { Pressable, ScrollView, Text, View } from "react-native";

import { ActiveRequestBanner } from "@/components/dashboard/active-request-banner";
import { LimitSummaryCard } from "@/components/dashboard/limit-summary-card";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import { useAuth } from "@/lib/auth";

export default function DashboardScreen() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useCurrentLimitQuery();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <View>
        <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Hi, {user?.firstName}</Text>
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Here's your account at a glance.</Text>
      </View>

      {isLoading ? (
        <View className="gap-4">
          <Skeleton className="h-32" />
          <Skeleton className="h-16" />
        </View>
      ) : isError || !data ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load your account.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : (
        <>
          <LimitSummaryCard dailyLimit={data.dailyLimit} usedToday={data.usedToday} remaining={data.remaining} />

          {data.activeRequest ? (
            <ActiveRequestBanner request={data.activeRequest} />
          ) : (
            <Link href="/increase-limit" asChild>
              <Pressable className="h-11 flex-row items-center justify-center gap-2 rounded-lg bg-accent dark:bg-accent-dark">
                <ArrowUpCircle color="#ffffff" size={16} />
                <Text className="text-sm font-medium text-white">Increase my limit</Text>
              </Pressable>
            </Link>
          )}
        </>
      )}
    </ScrollView>
  );
}
```

- [ ] **Step 6: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors from these five files.

- [ ] **Step 7: Manual smoke check**

Run: `npx expo start`, log in as `customer@limitflow.demo` / `Password123!` against a running
backend (`cd docker && docker compose up -d postgres backend-api`, `EXPO_PUBLIC_API_BASE_URL`
pointed at the machine's LAN IP if testing on a physical device rather than a simulator — a
simulator on the same machine can use `localhost`).
Expected: dashboard renders the daily-limit card and either the "Increase my limit" button or
the active-request banner, matching what `customer-portal`'s dashboard shows for the same
account.

- [ ] **Step 8: Commit**

```bash
git add apps/mobile/src/hooks/use-dashboard.ts apps/mobile/src/components/dashboard \
        apps/mobile/src/components/requests/status-badge.tsx "apps/mobile/src/app/(app)/index.tsx"
git commit -m "Add mobile dashboard screen"
```

---

### Task 12: Increase-limit wizard (with native biometric step)

**Files:**
- Create: `apps/mobile/src/hooks/use-limit-request.ts`
- Create: `apps/mobile/src/app/(app)/increase-limit.tsx`

**Interfaces:**
- Consumes: everything from Tasks 5–11, plus `expo-local-authentication`.
- Produces: the full 4-step wizard with the resume/cancel behavior already shipped on web.

- [ ] **Step 1: Port the mutation hooks verbatim**

`apps/mobile/src/hooks/use-limit-request.ts` — identical logic to
`customer-portal/src/hooks/use-limit-request.ts`:

```ts
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest } from "@/lib/types";

interface SubmitPayload {
  accountId: string;
  requestedLimit: number;
  reason: string;
  knownDevice: boolean;
}

function invalidateLimits(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: ["limits"] });
}

export function useSubmitLimitRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: SubmitPayload) => {
      const response = await apiClient.post<LimitRequest>("/limits/request", payload);
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}

export function useVerifyOtpMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ requestId, code }: { requestId: string; code: string }) => {
      const response = await apiClient.post<LimitRequest>(`/limits/${requestId}/otp/verify`, { code });
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}

export function useVerifyBiometricMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ requestId, success }: { requestId: string; success: boolean }) => {
      const response = await apiClient.post<LimitRequest>(`/limits/${requestId}/biometric/verify`, {
        success,
      });
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}

export function useCancelLimitRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (requestId: string) => {
      const response = await apiClient.post<LimitRequest>(`/limits/${requestId}/cancel`, {});
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}
```

- [ ] **Step 2: Write the wizard screen**

`apps/mobile/src/app/(app)/increase-limit.tsx` — same state machine as
`customer-portal/src/app/(portal)/increase-limit/page.tsx` (step tracking, `effectiveRequest`/
`effectiveStep` resume logic, cancel confirmation), with the biometric step calling
`expo-local-authentication` instead of just flipping state, and native components throughout:

```tsx
import * as LocalAuthentication from "expo-local-authentication";
import { useRouter } from "expo-router";
import { CheckCircle2, Fingerprint, ShieldCheck } from "lucide-react-native";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import {
  useCancelLimitRequestMutation,
  useSubmitLimitRequestMutation,
  useVerifyBiometricMutation,
  useVerifyOtpMutation,
} from "@/hooks/use-limit-request";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";
import { cn } from "@/lib/utils";

type Step = "amount" | "review" | "otp" | "biometric" | "done";

const RESUMABLE_STEP: Partial<Record<LimitRequest["status"], Step>> = {
  OTP_PENDING: "otp",
  BIOMETRIC_PENDING: "biometric",
};

const WIZARD_STEPS: Step[] = ["amount", "review", "otp", "biometric"];

function WizardProgress({ step }: { step: Step }) {
  const currentIndex = WIZARD_STEPS.indexOf(step);
  if (currentIndex === -1) return null;

  return (
    <View className="gap-1.5">
      <Text className="text-xs font-medium text-ink-muted dark:text-ink-muted-dark">
        Step {currentIndex + 1} of {WIZARD_STEPS.length}
      </Text>
      <View className="flex-row gap-1.5">
        {WIZARD_STEPS.map((s, index) => (
          <View
            key={s}
            className={cn(
              "h-1 flex-1 rounded-full",
              index <= currentIndex ? "bg-accent dark:bg-accent-dark" : "bg-border dark:bg-border-dark",
            )}
          />
        ))}
      </View>
    </View>
  );
}

export default function IncreaseLimitScreen() {
  const router = useRouter();
  const { toast } = useToast();
  const { data: current, isLoading } = useCurrentLimitQuery();

  const [step, setStep] = useState<Step>("amount");
  const [requestedLimit, setRequestedLimit] = useState("");
  const [reason, setReason] = useState("");
  const [newDevice, setNewDevice] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [request, setRequest] = useState<LimitRequest | null>(null);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const [biometricError, setBiometricError] = useState<string | null>(null);

  const submitMutation = useSubmitLimitRequestMutation();
  const otpMutation = useVerifyOtpMutation();
  const biometricMutation = useVerifyBiometricMutation();
  const cancelMutation = useCancelLimitRequestMutation();

  const activeRequest = current?.activeRequest ?? null;
  const resumableStep = activeRequest ? RESUMABLE_STEP[activeRequest.status] : undefined;
  const shouldRedirectToDetail = Boolean(activeRequest) && !resumableStep && !request;

  const effectiveRequest = request ?? (resumableStep ? activeRequest : null);
  const effectiveStep: Step = request ? step : (resumableStep ?? step);

  useEffect(() => {
    if (shouldRedirectToDetail) {
      router.replace(`/requests/${activeRequest!.id}`);
    }
  }, [shouldRedirectToDetail, activeRequest, router]);

  async function handleCancel() {
    if (!effectiveRequest) return;
    try {
      await cancelMutation.mutateAsync(effectiveRequest.id);
      setCancelConfirmOpen(false);
      setRequest(null);
      setOtpCode("");
      setStep("amount");
      toast("Request cancelled.");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't cancel this request.", "error");
    }
  }

  if (isLoading || !current || shouldRedirectToDetail) {
    return (
      <View className="flex-1 items-center justify-center bg-surface dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Loading…</Text>
      </View>
    );
  }

  const amount = Number(requestedLimit);
  const amountValid = amount > current.dailyLimit;

  async function handleSubmit() {
    try {
      const result = await submitMutation.mutateAsync({
        accountId: current!.accountId,
        requestedLimit: amount,
        reason,
        knownDevice: !newDevice,
      });
      setRequest(result);
      setStep("otp");
      toast("We sent a verification code. Check Notifications for the demo code.");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't submit your request.", "error");
    }
  }

  async function handleOtpVerify() {
    if (!effectiveRequest) return;
    try {
      const result = await otpMutation.mutateAsync({ requestId: effectiveRequest.id, code: otpCode });
      setRequest(result);
      setStep("biometric");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "That code didn't work.", "error");
    }
  }

  async function handleBiometricConfirm() {
    if (!effectiveRequest) return;
    setBiometricError(null);
    const authResult = await LocalAuthentication.authenticateAsync({
      promptMessage: "Confirm it's you",
      fallbackLabel: "Use passcode",
    });
    if (!authResult.success) {
      setBiometricError("Biometric confirmation was cancelled or failed.");
      return;
    }
    try {
      const result = await biometricMutation.mutateAsync({ requestId: effectiveRequest.id, success: true });
      setRequest(result);
      setStep("done");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Biometric verification failed.", "error");
    }
  }

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Increase your limit</Text>

      <WizardProgress step={effectiveStep} />

      {effectiveStep === "amount" && (
        <Card>
          <CardContent className="gap-4">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">
              Current daily limit: <Text className="font-medium text-ink dark:text-ink-dark">{formatCurrency(current.dailyLimit)}</Text>
            </Text>

            <View className="gap-1.5">
              <Label>New daily limit (₦)</Label>
              <Input
                keyboardType="numeric"
                value={requestedLimit}
                onChangeText={setRequestedLimit}
                placeholder={`More than ${current.dailyLimit}`}
                invalid={Boolean(requestedLimit) && !amountValid}
              />
              {requestedLimit.length > 0 && !amountValid && (
                <Text className="text-xs text-danger dark:text-danger-dark">Must be more than your current limit.</Text>
              )}
            </View>

            <View className="gap-1.5">
              <Label>Reason for increase</Label>
              <Textarea value={reason} onChangeText={setReason} placeholder="e.g. Paying a supplier for a large order" />
            </View>

            <Pressable className="flex-row items-center gap-2" onPress={() => setNewDevice((v) => !v)}>
              <Checkbox checked={newDevice} onValueChange={setNewDevice} />
              <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">I'm on a new or unrecognized device</Text>
            </Pressable>

            <Button disabled={!amountValid || reason.trim().length === 0} onPress={() => setStep("review")}>
              Continue
            </Button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "review" && (
        <Card>
          <CardContent className="gap-4">
            <View className="flex-row items-center justify-between">
              <Text className="text-sm font-medium text-ink dark:text-ink-dark">Review your request</Text>
              <Pressable onPress={() => setStep("amount")}>
                <Text className="text-sm font-medium text-accent dark:text-accent-dark">Edit</Text>
              </Pressable>
            </View>
            <View className="gap-2">
              <View className="flex-row justify-between">
                <Text className="text-ink-muted dark:text-ink-muted-dark">New limit</Text>
                <Text className="font-medium text-ink dark:text-ink-dark">{formatCurrency(amount)}</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-ink-muted dark:text-ink-muted-dark">Reason</Text>
                <Text className="max-w-[60%] text-right font-medium text-ink dark:text-ink-dark">{reason}</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-ink-muted dark:text-ink-muted-dark">Device</Text>
                <Text className="font-medium text-ink dark:text-ink-dark">{newDevice ? "New device" : "Trusted device"}</Text>
              </View>
            </View>
            <Button loading={submitMutation.isPending} onPress={handleSubmit}>
              {submitMutation.isPending ? "Submitting…" : "Confirm and submit"}
            </Button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "otp" && (
        <Card>
          <CardContent className="items-center gap-4">
            <ShieldCheck color="#5b3df5" size={40} />
            <View className="items-center">
              <Text className="font-medium text-ink dark:text-ink-dark">Enter the verification code</Text>
              <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
                Sent to your registered number. Check Notifications for the demo code.
              </Text>
            </View>
            <Input
              value={otpCode}
              onChangeText={setOtpCode}
              placeholder="6-digit code"
              keyboardType="numeric"
              className="w-full text-center text-lg tracking-widest"
            />
            <Button disabled={otpCode.length === 0} loading={otpMutation.isPending} onPress={handleOtpVerify}>
              {otpMutation.isPending ? "Verifying…" : "Verify code"}
            </Button>
            <Pressable onPress={() => setCancelConfirmOpen(true)}>
              <Text className="text-sm font-medium text-danger dark:text-danger-dark">Cancel request</Text>
            </Pressable>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "biometric" && (
        <Card>
          <CardContent className="items-center gap-4">
            <Fingerprint color="#5b3df5" size={40} />
            <View className="items-center">
              <Text className="font-medium text-ink dark:text-ink-dark">Confirm it's you</Text>
              <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
                Use your fingerprint or face to finish verifying this request.
              </Text>
            </View>
            {biometricError && <Text className="text-xs text-danger dark:text-danger-dark">{biometricError}</Text>}
            <Button loading={biometricMutation.isPending} onPress={handleBiometricConfirm}>
              {biometricMutation.isPending ? "Confirming…" : "Confirm biometric"}
            </Button>
            <Pressable onPress={() => setCancelConfirmOpen(true)}>
              <Text className="text-sm font-medium text-danger dark:text-danger-dark">Cancel request</Text>
            </Pressable>
          </CardContent>
        </Card>
      )}

      <Dialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel this request?</DialogTitle>
            <DialogDescription>
              You'll need to start a new request from scratch if you change your mind. This can't be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onPress={() => setCancelConfirmOpen(false)}>
              Keep request
            </Button>
            <Button variant="destructive" loading={cancelMutation.isPending} onPress={handleCancel}>
              {cancelMutation.isPending ? "Cancelling…" : "Cancel request"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {step === "done" && request && (
        <Card>
          <CardContent className="items-center gap-4">
            <CheckCircle2 color="#12b76a" size={40} />
            <View className="items-center">
              <Text className="font-medium text-ink dark:text-ink-dark">
                {request.status === "APPROVED" ? "Limit increased" : "Request submitted for review"}
              </Text>
              <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
                {request.status === "APPROVED"
                  ? `Your new daily limit is ${formatCurrency(request.requestedLimit)}.`
                  : "We'll notify you once a review is complete."}
              </Text>
            </View>
            <Button onPress={() => router.replace(`/requests/${request.id}`)}>View request status</Button>
          </CardContent>
        </Card>
      )}
    </ScrollView>
  );
}
```

- [ ] **Step 3: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Manual smoke check — the actual point of this whole feature**

With a running backend, log in on a physical device or simulator with biometrics enrolled
(iOS Simulator: Features → Face ID → Enrolled, then Features → Face ID → Matching Face during
the prompt) and drive the full flow: amount → review → submit → read the OTP from
Notifications → verify → **the biometric step must show a real Face ID/fingerprint system
prompt, not just a button** → confirm the request resolves (approved or under review) exactly
as `customer-portal` does for the same inputs.
Also verify: cancel mid-flow works, and backgrounding/reopening the app on the OTP or
biometric step resumes at the correct step (same resume logic as web, already proven there).

- [ ] **Step 5: Commit**

```bash
git add apps/mobile/src/hooks/use-limit-request.ts "apps/mobile/src/app/(app)/increase-limit.tsx"
git commit -m "Add mobile increase-limit wizard with native biometric verification"
```

---

### Task 13: Requests list + detail

**Files:**
- Create: `apps/mobile/src/hooks/use-history.ts`
- Create: `apps/mobile/src/hooks/use-request-detail.ts`
- Create: `apps/mobile/src/components/requests/timeline.tsx`
- Create: `apps/mobile/src/components/requests/request-list-item.tsx`
- Create: `apps/mobile/src/app/(app)/requests/index.tsx`
- Create: `apps/mobile/src/app/(app)/requests/[id].tsx`

**Interfaces:**
- Consumes: `Card`, `Skeleton`, `Button`, `Dialog`, `StatusBadge`, `useCancelLimitRequestMutation`
  (Tasks 7–12).
- Produces: requests list + detail screens with the same resume/cancel affordances as web.

- [ ] **Step 1: Port the query hooks verbatim**

`apps/mobile/src/hooks/use-history.ts`:

```ts
import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest } from "@/lib/types";

export function useHistoryQuery() {
  return useQuery({
    queryKey: ["limits", "history"],
    queryFn: async () => {
      const response = await apiClient.get<LimitRequest[]>("/limits/history");
      return response.data;
    },
  });
}
```

`apps/mobile/src/hooks/use-request-detail.ts`:

```ts
import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest } from "@/lib/types";

export function useRequestDetailQuery(requestId: string) {
  return useQuery({
    queryKey: ["limits", requestId],
    queryFn: async () => {
      const response = await apiClient.get<LimitRequest>(`/limits/${requestId}`);
      return response.data;
    },
  });
}
```

- [ ] **Step 2: Timeline**

`apps/mobile/src/components/requests/timeline.tsx` — same current-step spinner shipped on web:

```tsx
import { Check, Loader2 } from "lucide-react-native";
import { Text, View } from "react-native";

import { cn } from "@/lib/utils";
import type { TimelineStepStatus } from "@/lib/types";

export function Timeline({ steps }: { steps: { label: string; status: TimelineStepStatus }[] }) {
  return (
    <View>
      {steps.map((step, index) => {
        const isComplete = step.status === "COMPLETE";
        const isCurrent = step.status === "CURRENT";
        const isLast = index === steps.length - 1;

        return (
          <View key={step.label} className="flex-row gap-3">
            <View className="items-center">
              <View
                className={cn(
                  "h-6 w-6 items-center justify-center rounded-full border-2",
                  isComplete && "border-success bg-success dark:border-success-dark dark:bg-success-dark",
                  isCurrent && "border-accent bg-accent dark:border-accent-dark dark:bg-accent-dark",
                  !isComplete && !isCurrent && "border-border bg-card dark:border-border-dark dark:bg-card-dark",
                )}
              >
                {isComplete && <Check size={14} color="#ffffff" />}
                {isCurrent && <Loader2 size={14} color="#ffffff" />}
              </View>
              {!isLast && (
                <View className={cn("w-0.5 flex-1", isComplete ? "bg-success dark:bg-success-dark" : "bg-border dark:bg-border-dark")} />
              )}
            </View>
            <Text
              className={cn(
                "pb-6 text-sm",
                isCurrent ? "font-semibold text-ink dark:text-ink-dark" : "text-ink-muted dark:text-ink-muted-dark",
              )}
            >
              {step.label}
            </Text>
          </View>
        );
      })}
    </View>
  );
}
```

`Loader2` has no `animate-spin` NativeWind equivalent driving continuous rotation without
`react-native-reanimated` wiring — the icon renders static here rather than spinning. This is a
real, visible gap from the web version (called out honestly, not hidden): if it matters,
follow up with a `Reanimated.useAnimatedStyle` rotation loop; not added here as it would be
this task's only piece of animation code in an otherwise straightforward port.

- [ ] **Step 3: Request list item**

`apps/mobile/src/components/requests/request-list-item.tsx`:

```tsx
import { formatDistanceToNow } from "date-fns";
import { Link } from "expo-router";
import { Pressable, Text, View } from "react-native";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function RequestListItem({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`} asChild>
      <Pressable className="flex-row items-center justify-between gap-3 py-3">
        <View>
          <Text className="text-sm font-medium text-ink dark:text-ink-dark">{formatCurrency(request.requestedLimit)}</Text>
          <Text className="text-xs text-ink-muted dark:text-ink-muted-dark">
            {formatDistanceToNow(new Date(request.createdAt), { addSuffix: true })}
          </Text>
        </View>
        <StatusBadge status={request.status} />
      </Pressable>
    </Link>
  );
}
```

- [ ] **Step 4: Requests list screen**

`apps/mobile/src/app/(app)/requests/index.tsx`:

```tsx
import { Pressable, ScrollView, Text, View } from "react-native";

import { RequestListItem } from "@/components/requests/request-list-item";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useHistoryQuery } from "@/hooks/use-history";

export default function RequestsScreen() {
  const { data, isLoading, isError, refetch } = useHistoryQuery();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Your requests</Text>

      {isLoading ? (
        <View className="gap-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </View>
      ) : isError ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load your requests.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent>
            <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
              You haven't requested a limit increase yet.
            </Text>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="divide-y divide-border dark:divide-border-dark">
            {data.map((request) => (
              <RequestListItem key={request.id} request={request} />
            ))}
          </CardContent>
        </Card>
      )}
    </ScrollView>
  );
}
```

(NativeWind supports `divide-y`/`divide-border` the same way Tailwind does; if the installed
NativeWind version doesn't resolve it correctly, fall back to wrapping each `RequestListItem`
in a `View` with a `border-b border-border` and dropping the border on the last item — check
this visually in Step 6 and adjust if the divider doesn't render.)

- [ ] **Step 5: Request detail screen**

`apps/mobile/src/app/(app)/requests/[id].tsx` — same resume/cancel affordances as
`customer-portal/src/app/(portal)/requests/[id]/request-detail-client.tsx`:

```tsx
import { useLocalSearchParams, useRouter } from "expo-router";
import { ArrowLeft } from "lucide-react-native";
import { useState } from "react";
import { Pressable, ScrollView, Text, View } from "react-native";

import { StatusBadge } from "@/components/requests/status-badge";
import { Timeline } from "@/components/requests/timeline";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { useCancelLimitRequestMutation } from "@/hooks/use-limit-request";
import { useRequestDetailQuery } from "@/hooks/use-request-detail";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/currency";
import { isActiveStatus, type RequestStatus } from "@/lib/types";

const RESUMABLE_STATUSES: RequestStatus[] = ["OTP_PENDING", "BIOMETRIC_PENDING"];

export default function RequestDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const { data: request, isLoading, isError, refetch } = useRequestDetailQuery(id);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const cancelMutation = useCancelLimitRequestMutation();

  async function handleCancel() {
    try {
      await cancelMutation.mutateAsync(id);
      setCancelConfirmOpen(false);
      toast("Request cancelled.");
      refetch();
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't cancel this request.", "error");
    }
  }

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Pressable className="flex-row items-center gap-1" onPress={() => router.back()}>
        <ArrowLeft size={16} color="#6b6580" />
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Back to requests</Text>
      </Pressable>

      {isLoading ? (
        <View className="gap-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-48" />
        </View>
      ) : isError || !request ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load this request.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent>
              <View className="flex-row items-start justify-between gap-4">
                <View>
                  <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Requested limit</Text>
                  <Text className="text-2xl font-semibold text-ink dark:text-ink-dark">{formatCurrency(request.requestedLimit)}</Text>
                  <Text className="mt-1 text-sm text-ink-muted dark:text-ink-muted-dark">
                    Current limit: {formatCurrency(request.currentLimit)}
                  </Text>
                </View>
                <StatusBadge status={request.status} />
              </View>
              <View className="mt-4 border-t border-border pt-4 dark:border-border-dark">
                <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Reason given</Text>
                <Text className="mt-1 text-sm text-ink dark:text-ink-dark">{request.reason}</Text>
              </View>

              {RESUMABLE_STATUSES.includes(request.status) && (
                <View className="mt-4 border-t border-border pt-4 dark:border-border-dark">
                  <Button onPress={() => router.push("/increase-limit")}>Resume verification</Button>
                </View>
              )}

              {isActiveStatus(request.status) && (
                <View className="mt-2 items-center">
                  <Pressable onPress={() => setCancelConfirmOpen(true)}>
                    <Text className="text-sm font-medium text-danger dark:text-danger-dark">Cancel request</Text>
                  </Pressable>
                </View>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Progress</CardTitle>
            </CardHeader>
            <CardContent>
              <Timeline steps={request.timeline} />
            </CardContent>
          </Card>
        </>
      )}

      <Dialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel this request?</DialogTitle>
            <DialogDescription>
              You'll need to start a new request from scratch if you change your mind. This can't be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onPress={() => setCancelConfirmOpen(false)}>
              Keep request
            </Button>
            <Button variant="destructive" loading={cancelMutation.isPending} onPress={handleCancel}>
              {cancelMutation.isPending ? "Cancelling…" : "Cancel request"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ScrollView>
  );
}
```

- [ ] **Step 6: Verify and manually check the divider fallback from Step 4**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.
Then run the app and open Requests — confirm dividers between list items render; if not,
apply the Step 4 fallback.

- [ ] **Step 7: Commit**

```bash
git add apps/mobile/src/hooks/use-history.ts apps/mobile/src/hooks/use-request-detail.ts \
        apps/mobile/src/components/requests "apps/mobile/src/app/(app)/requests"
git commit -m "Add mobile requests list and detail screens"
```

---

### Task 14: Notifications screen

**Files:**
- Create: `apps/mobile/src/hooks/use-notifications.ts`
- Create: `apps/mobile/src/app/(app)/notifications.tsx`

**Interfaces:**
- Consumes: `Card`, `Skeleton` (Tasks 7–8).

- [ ] **Step 1: Port the query hook verbatim**

`apps/mobile/src/hooks/use-notifications.ts`:

```ts
import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { NotificationItem } from "@/lib/types";

export function useNotificationsQuery() {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const response = await apiClient.get<NotificationItem[]>("/notifications");
      return response.data;
    },
    refetchInterval: 15_000,
  });
}
```

- [ ] **Step 2: Notifications screen**

`apps/mobile/src/app/(app)/notifications.tsx`:

```tsx
import { formatDistanceToNow } from "date-fns";
import { Bell, CheckCircle2, MessageCircle, ShieldCheck, XCircle, type LucideIcon } from "lucide-react-native";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useNotificationsQuery } from "@/hooks/use-notifications";
import { cn } from "@/lib/utils";

const ICONS: Record<string, LucideIcon> = {
  OTP_SENT: ShieldCheck,
  VERIFICATION_COMPLETED: ShieldCheck,
  VERIFICATION_REQUESTED: ShieldCheck,
  LIMIT_APPROVED: CheckCircle2,
  LIMIT_REJECTED: XCircle,
  SUPPORT_COMMENT: MessageCircle,
};

export default function NotificationsScreen() {
  const { data, isLoading, isError, refetch } = useNotificationsQuery();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Notifications</Text>

      {isLoading ? (
        <View className="gap-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-14" />
          ))}
        </View>
      ) : isError ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load your notifications.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="items-center gap-2">
            <Bell size={32} color="#ece8f6" />
            <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">Nothing here yet.</Text>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="gap-0">
            {data.map((item) => {
              const Icon = ICONS[item.type] ?? Bell;
              return (
                <View key={item.id} className="flex-row gap-3 py-3">
                  <View
                    className={cn(
                      "h-9 w-9 items-center justify-center rounded-full",
                      item.read ? "bg-border dark:bg-border-dark" : "bg-accent-soft dark:bg-accent-soft-dark",
                    )}
                  >
                    <Icon size={16} color={item.read ? "#6b6580" : "#5b3df5"} />
                  </View>
                  <View className="flex-1">
                    <Text className="text-sm font-medium text-ink dark:text-ink-dark">{item.title}</Text>
                    <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{item.message}</Text>
                    <Text className="mt-1 text-xs text-ink-muted dark:text-ink-muted-dark">
                      {formatDistanceToNow(new Date(item.createdAt), { addSuffix: true })}
                    </Text>
                  </View>
                </View>
              );
            })}
          </CardContent>
        </Card>
      )}
    </ScrollView>
  );
}
```

- [ ] **Step 3: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add apps/mobile/src/hooks/use-notifications.ts "apps/mobile/src/app/(app)/notifications.tsx"
git commit -m "Add mobile notifications screen"
```

---

### Task 15: Profile + Support screens

**Files:**
- Create: `apps/mobile/src/app/(app)/profile.tsx`
- Create: `apps/mobile/src/app/(app)/support.tsx`

**Interfaces:**
- Consumes: `useAuth`, `Card`, `Button` (Tasks 6–7).

- [ ] **Step 1: Profile screen**

`apps/mobile/src/app/(app)/profile.tsx`:

```tsx
import { Link } from "expo-router";
import { LogOut, Mail, User as UserIcon } from "lucide-react-native";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/lib/auth";

export default function ProfileScreen() {
  const { user, logout } = useAuth();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Profile</Text>

      <Card>
        <CardContent className="flex-row items-center gap-4">
          <View className="h-14 w-14 items-center justify-center rounded-full bg-accent dark:bg-accent-dark">
            <Text className="text-lg font-semibold text-white">
              {user?.firstName?.[0]}
              {user?.lastName?.[0]}
            </Text>
          </View>
          <View>
            <Text className="text-base font-medium text-ink dark:text-ink-dark">
              {user?.firstName} {user?.lastName}
            </Text>
            <View className="flex-row items-center gap-1">
              <Mail size={14} color="#6b6580" />
              <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{user?.email}</Text>
            </View>
          </View>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Link href="/support" asChild>
            <Pressable className="flex-row items-center gap-3">
              <UserIcon size={16} color="#6b6580" />
              <Text className="text-sm font-medium text-ink dark:text-ink-dark">Contact support</Text>
            </Pressable>
          </Link>
        </CardContent>
      </Card>

      <Button variant="outline" onPress={logout}>
        <View className="flex-row items-center gap-2">
          <LogOut size={16} color="#151222" />
          <Text className="text-sm font-medium text-ink dark:text-ink-dark">Log out</Text>
        </View>
      </Button>
    </ScrollView>
  );
}
```

- [ ] **Step 2: Support screen**

`apps/mobile/src/app/(app)/support.tsx`:

```tsx
import * as Linking from "expo-linking";
import { Mail, MessageCircle, Phone } from "lucide-react-native";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Card, CardContent } from "@/components/ui/card";

const CHANNELS = [
  { icon: Phone, label: "Call us", value: "0700-LIMITFLOW", href: "tel:0700546483569" },
  { icon: Mail, label: "Email us", value: "support@limitflow.demo", href: "mailto:support@limitflow.demo" },
];

export default function SupportScreen() {
  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <View>
        <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Support</Text>
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Need help with a request? Reach out any time.</Text>
      </View>

      <Card>
        <CardContent className="gap-0">
          {CHANNELS.map((channel) => (
            <Pressable key={channel.label} className="flex-row items-center gap-3 py-3" onPress={() => Linking.openURL(channel.href)}>
              <View className="h-9 w-9 items-center justify-center rounded-full bg-accent-soft dark:bg-accent-soft-dark">
                <channel.icon size={16} color="#5b3df5" />
              </View>
              <View>
                <Text className="text-sm font-medium text-ink dark:text-ink-dark">{channel.label}</Text>
                <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{channel.value}</Text>
              </View>
            </Pressable>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex-row items-start gap-3">
          <MessageCircle size={20} color="#6b6580" />
          <Text className="flex-1 text-sm text-ink-muted dark:text-ink-muted-dark">
            If a request lands in review, our team follows up using the details on your profile — no need to
            call in just to check on it. Track progress any time from the Requests tab.
          </Text>
        </CardContent>
      </Card>
    </ScrollView>
  );
}
```

- [ ] **Step 3: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add "apps/mobile/src/app/(app)/profile.tsx" "apps/mobile/src/app/(app)/support.tsx"
git commit -m "Add mobile profile and support screens"
```

---

### Task 16: Push notification registration (mobile side)

**Files:**
- Create: `apps/mobile/src/lib/push.ts`
- Modify: `apps/mobile/src/lib/auth.tsx`
- Modify: `apps/mobile/app.config.ts`

**Interfaces:**
- Consumes: `apiClient` (Task 5), the `POST`/`DELETE /api/devices/push-token` endpoints
  (backend Task 3).
- Produces: automatic push-token registration on login, unregistration on logout — the last
  piece the design spec's "OTP via push" data flow depends on.

- [ ] **Step 1: Add the notifications plugin/permission config**

Modify `apps/mobile/app.config.ts` to add the `expo-notifications` plugin (needed for the
notification icon/behavior on Android):

```ts
import type { ExpoConfig } from "expo/config";

const config: ExpoConfig = {
  name: "LimitFlow",
  slug: "limitflow-mobile",
  scheme: "limitflow",
  plugins: ["expo-notifications"],
  extra: {
    apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  },
};

export default config;
```

- [ ] **Step 2: Write the push registration helper**

`apps/mobile/src/lib/push.ts`:

```ts
import * as Device from "expo-device";
import * as Notifications from "expo-notifications";
import { Platform } from "react-native";

import { apiClient } from "./api-client";

/** Best-effort: a denied permission, a simulator with no push capability, or a network
 * failure here must never block login — this mirrors the backend's own
 * "push failures are swallowed" rule from NotificationService. */
export async function registerForPushNotifications(): Promise<void> {
  try {
    if (!Device.isDevice) {
      return; // simulators/emulators can't receive real push tokens
    }
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    if (existingStatus !== "granted") {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    if (finalStatus !== "granted") {
      return;
    }

    const { data: expoPushToken } = await Notifications.getExpoPushTokenAsync();
    await apiClient.post("/devices/push-token", {
      expoPushToken,
      platform: Platform.OS,
    });
  } catch {
    // Deliberately swallowed — see the function comment above.
  }
}

export async function unregisterForPushNotifications(): Promise<void> {
  try {
    if (!Device.isDevice) {
      return;
    }
    const { data: expoPushToken } = await Notifications.getExpoPushTokenAsync();
    await apiClient.delete("/devices/push-token", { data: { expoPushToken, platform: Platform.OS } });
  } catch {
    // Same reasoning as above — logout must always succeed locally regardless.
  }
}
```

Run `npx expo install expo-device` if not already present.

- [ ] **Step 3: Wire registration into login, unregistration into logout**

Modify `apps/mobile/src/lib/auth.tsx`: add the import and two call sites.

```ts
import { registerForPushNotifications, unregisterForPushNotifications } from "./push";
```

In `login`, after `setUser(loggedInUser)`:

```ts
setUser(loggedInUser);
setIsUnlocked(true);
void registerForPushNotifications();
```

In `logout`, before `setUser(null)`:

```ts
await unregisterForPushNotifications();
setUser(null);
```

(`login`'s registration call is fire-and-forget — `void registerForPushNotifications()`, not
awaited — so a slow or denied permission prompt never delays getting the user into the app.
`logout`'s unregistration is awaited since it must complete before the token that authorizes it
is cleared.)

- [ ] **Step 4: Verify**

Run: `cd apps/mobile && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 5: Manual end-to-end check — the full loop this whole plan was for**

With the backend running (including Task 3's endpoint) and a physical device (push tokens
don't work in most simulators): log in, grant the notification permission prompt, then from
another session submit and drive a limit-increase request for this same account through OTP.
Expected: **a real push notification arrives on the device** with the OTP code, matching the
in-app notification exactly (same title/body) — this is the design spec's "OTP via push" data
flow, now real. Log out and confirm no further pushes arrive for that account.

- [ ] **Step 6: Commit**

```bash
git add apps/mobile/src/lib/push.ts apps/mobile/src/lib/auth.tsx apps/mobile/app.config.ts
git commit -m "Wire push-token registration into mobile login/logout"
```

---

## Final integration check (not a task — run after Task 16)

1. Full clean run: `cd apps/mobile && npx tsc --noEmit` — clean across the whole app.
2. Full backend suite: `cd apps/backend-api && ./mvnw -q -B test` — all tests pass, including
   the four new push-related test classes from Tasks 1–3.
3. One complete manual walkthrough on a physical device with biometrics enrolled: login →
   biometric app-unlock on relaunch → dashboard → increase-limit (real biometric prompt) →
   push notification received for the OTP → requests list/detail → cancel a request → resume a
   stranded one → notifications → profile → support → logout.
4. Confirm `customer-portal` and `employee-portal` are both unaffected: re-run their existing
   manual verification (or, if time allows, the same increase-limit end-to-end check already
   used earlier in this project) to confirm the additive `NotificationService` change didn't
   alter their behavior.

Then use **superpowers:finishing-a-development-branch** to wrap up.
