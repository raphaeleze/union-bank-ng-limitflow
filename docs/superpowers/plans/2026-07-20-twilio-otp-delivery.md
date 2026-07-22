# Twilio OTP Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver OTP codes via real Twilio SMS when a feature flag is enabled and the customer has a phone number on file, falling back to the existing structured log line otherwise — never blocking the limit-increase flow.

**Architecture:** A new `OtpDeliveryService` (backend-api, `application/otp` package) is called once from `LimitRequestService.sendOtp()`, alongside the existing (unchanged) in-app notification. It owns the enabled/phone-present decision and the Twilio SDK call; failures are caught and logged, never thrown.

**Tech Stack:** Spring Boot 3.5 (constructor `@Value` injection, matching `JwtTokenService`), Lombok `@Slf4j` (matching `GlobalExceptionHandler`), Twilio Java SDK (`com.twilio.sdk:twilio:12.1.1` — [Maven Central](https://mvnrepository.com/artifact/com.twilio.sdk/twilio)), Flyway migration, JUnit 5 + Mockito + AssertJ (matching `LimitRequestServiceTest`).

## Global Constraints

- All new Spring config values live under the `limitflow.*` prefix, `${ENV_VAR:default}` style, exactly like `limitflow.security.jwt.*` in `application.yml`.
- Config injection is constructor `@Value`, not `@ConditionalOnProperty` — nothing in this codebase uses the latter.
- Twilio SDK init (`Twilio.init(...)`) must not run, and must not require credentials, when the feature flag is off — the demo must work with zero Twilio account by default.
- A Twilio send failure must never throw out of `OtpDeliveryService.deliver()` — it must log and fall through.
- Migrations already applied (`V1__init.sql`, `V2__seed.sql`) are not edited; new schema changes go in a new `V3__` file.

---

### Task 1: Add phone number to User (migration, entity, DTO)

**Files:**
- Create: `apps/backend-api/src/main/resources/db/migration/V3__add_user_phone.sql`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/user/User.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/UserSummary.java`
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/integration/AuthControllerIntegrationTest.java` (existing — verify it still passes; no new test needed, see note below)

**Interfaces:**
- Produces: `User.getPhone()` returns `String` (nullable). `UserSummary.phone()` returns `String` (nullable), included in `GET /api/customer/me` JSON as `"phone"`.

There's no unit-testable branch here — this is a schema/DTO addition. Its correctness is proven by the existing `LimitRequestFlowIntegrationTest` and `AuthControllerIntegrationTest` continuing to pass against the real (embedded Postgres) Flyway migration chain, plus Task 3's `OtpDeliveryServiceTest` exercising `getPhone()` directly. No placeholder test is written for this task per YAGNI — a getter/DTO field has no logic to assert beyond "the migration runs and the field round-trips," which the integration suite already covers.

- [ ] **Step 1: Write the migration**

Create `apps/backend-api/src/main/resources/db/migration/V3__add_user_phone.sql`:

```sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

UPDATE users SET phone = '+2348012345678' WHERE email = 'customer@limitflow.demo';
```

- [ ] **Step 2: Add the field to `User.java`**

In `apps/backend-api/src/main/java/com/limitflow/backend/domain/user/User.java`, add after the `role` field (line 36-37):

```java
    @Column(name = "phone")
    private String phone;
```

- [ ] **Step 3: Add `phone` to `UserSummary`**

Replace the full contents of `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/UserSummary.java`:

```java
package com.limitflow.backend.presentation.dto;

import com.limitflow.backend.domain.user.User;

import java.util.UUID;

public record UserSummary(UUID id, String firstName, String lastName, String email, String role, String phone) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), user.getPhone());
    }
}
```

- [ ] **Step 4: Run the existing integration tests to confirm the migration and DTO change don't break anything**

Run: `./mvnw test -Dtest=AuthControllerIntegrationTest,LimitRequestFlowIntegrationTest` (from `apps/backend-api`)
Expected: PASS (these hit a real embedded Postgres and run all Flyway migrations, including the new `V3__`)

- [ ] **Step 5: Commit**

```bash
git add apps/backend-api/src/main/resources/db/migration/V3__add_user_phone.sql apps/backend-api/src/main/java/com/limitflow/backend/domain/user/User.java apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/UserSummary.java
git commit -m "Add phone number to User, seeded for the demo customer"
```

---

### Task 2: Add Twilio dependency and configuration

**Files:**
- Modify: `apps/backend-api/pom.xml`
- Modify: `apps/backend-api/src/main/resources/application.yml`
- Modify: `docker/docker-compose.yml`

**Interfaces:**
- Produces: properties `limitflow.twilio.enabled` (boolean, default `false`), `limitflow.twilio.account-sid`, `limitflow.twilio.auth-token`, `limitflow.twilio.from-number` (all `String`, default empty) — consumed by Task 3's `OtpDeliveryService` constructor.

This task has no logic to test — it's dependency/config wiring, proven by the app starting successfully in Task 3's test run.

- [ ] **Step 1: Add the Twilio dependency**

In `apps/backend-api/pom.xml`, add inside `<dependencies>`, after the `postgresql` dependency block (after line 58's closing `</dependency>`):

```xml
        <dependency>
            <groupId>com.twilio.sdk</groupId>
            <artifactId>twilio</artifactId>
            <version>12.1.1</version>
        </dependency>
```

- [ ] **Step 2: Add the config block to `application.yml`**

In `apps/backend-api/src/main/resources/application.yml`, add a `twilio` block under the existing `limitflow:` key, after the `security.jwt` block (after `expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}`, before `risk:`):

```yaml
  twilio:
    enabled: ${TWILIO_ENABLED:false}
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_FROM_NUMBER:}
```

- [ ] **Step 3: Add matching env vars to `docker-compose.yml`**

In `docker/docker-compose.yml`, add to the `backend-api` service's `environment:` block (after `JWT_SECRET: local-development-secret-change-me-before-any-real-deployment`):

```yaml
      TWILIO_ENABLED: "false"
      TWILIO_ACCOUNT_SID: ""
      TWILIO_AUTH_TOKEN: ""
      TWILIO_FROM_NUMBER: ""
```

- [ ] **Step 4: Verify the app still builds and boots**

Run: `./mvnw spring-boot:run` (from `apps/backend-api`), wait for "Started BackendApiApplication", then Ctrl+C
Expected: starts cleanly with no Twilio-related errors (the SDK isn't initialized yet — that's Task 3)

- [ ] **Step 5: Commit**

```bash
git add apps/backend-api/pom.xml apps/backend-api/src/main/resources/application.yml docker/docker-compose.yml
git commit -m "Add Twilio SDK dependency and feature-flagged config"
```

---

### Task 3: Build `OtpDeliveryService`

**Files:**
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/application/otp/OtpDeliveryService.java`
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/application/otp/OtpDeliveryServiceTest.java`

**Interfaces:**
- Consumes: `User.getPhone()` (`String`, nullable) from Task 1.
- Produces: `public void deliver(User customer, String code)` and package-private `boolean shouldSendViaTwilio(User customer)` — consumed by Task 4's `LimitRequestService`.

- [ ] **Step 1: Write the failing tests**

Create `apps/backend-api/src/test/java/com/limitflow/backend/application/otp/OtpDeliveryServiceTest.java`:

```java
package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpDeliveryServiceTest {

    @Test
    void shouldSendViaTwilioIsFalseWhenDisabled() {
        OtpDeliveryService service = new OtpDeliveryService(false, "", "", "");
        User customer = newCustomer("+2348012345678");

        assertThat(service.shouldSendViaTwilio(customer)).isFalse();
    }

    @Test
    void shouldSendViaTwilioIsFalseWhenPhoneIsMissing() {
        OtpDeliveryService service = new OtpDeliveryService(true, "sid", "token", "+15550000000");
        User customer = newCustomer(null);

        assertThat(service.shouldSendViaTwilio(customer)).isFalse();
    }

    @Test
    void shouldSendViaTwilioIsFalseWhenPhoneIsBlank() {
        OtpDeliveryService service = new OtpDeliveryService(true, "sid", "token", "+15550000000");
        User customer = newCustomer("   ");

        assertThat(service.shouldSendViaTwilio(customer)).isFalse();
    }

    @Test
    void shouldSendViaTwilioIsTrueWhenEnabledAndPhonePresent() {
        OtpDeliveryService service = new OtpDeliveryService(true, "sid", "token", "+15550000000");
        User customer = newCustomer("+2348012345678");

        assertThat(service.shouldSendViaTwilio(customer)).isTrue();
    }

    @Test
    void deliverFallsBackToLoggingWithoutThrowingWhenDisabled() {
        OtpDeliveryService service = new OtpDeliveryService(false, "", "", "");
        User customer = newCustomer(null);

        assertThatCode(() -> service.deliver(customer, "123456")).doesNotThrowAnyException();
    }

    private User newCustomer(String phone) {
        User user = mock(User.class);
        lenient().when(user.getPhone()).thenReturn(phone);
        return user;
    }
}
```

- [ ] **Step 2: Run the test to verify it fails (compilation failure — `OtpDeliveryService` doesn't exist yet)**

Run: `./mvnw test -Dtest=OtpDeliveryServiceTest` (from `apps/backend-api`)
Expected: FAIL — compilation error, `OtpDeliveryService` cannot be found

- [ ] **Step 3: Write the implementation**

Create `apps/backend-api/src/main/java/com/limitflow/backend/application/otp/OtpDeliveryService.java`:

```java
package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.user.User;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends the OTP via Twilio SMS when {@code limitflow.twilio.enabled=true} and the customer has
 * a phone number on file. Otherwise — and on any Twilio failure — falls back to a log line, the
 * same demo-mode behavior this codebase already had before real SMS delivery existed. Never
 * throws: a delivery failure must not block the limit-increase flow, since the code is already
 * persisted by {@link OtpService#issue}.
 */
@Slf4j
@Service
public class OtpDeliveryService {

    private final boolean enabled;
    private final String fromNumber;

    public OtpDeliveryService(
            @Value("${limitflow.twilio.enabled}") boolean enabled,
            @Value("${limitflow.twilio.account-sid}") String accountSid,
            @Value("${limitflow.twilio.auth-token}") String authToken,
            @Value("${limitflow.twilio.from-number}") String fromNumber) {
        this.enabled = enabled;
        this.fromNumber = fromNumber;
        if (enabled) {
            Twilio.init(accountSid, authToken);
        }
    }

    public void deliver(User customer, String code) {
        if (shouldSendViaTwilio(customer)) {
            try {
                Message.creator(new PhoneNumber(customer.getPhone()), new PhoneNumber(fromNumber),
                                "Your LimitFlow verification code is " + code)
                        .create();
                log.info("OTP sent via Twilio to {}", mask(customer.getPhone()));
                return;
            } catch (Exception e) {
                log.warn("Twilio send failed, falling back to log", e);
            }
        }
        log.info("OTP code (Twilio disabled or unavailable): {}", code);
    }

    boolean shouldSendViaTwilio(User customer) {
        return enabled && customer.getPhone() != null && !customer.getPhone().isBlank();
    }

    private String mask(String phone) {
        return phone.length() <= 4 ? phone : "***" + phone.substring(phone.length() - 4);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw test -Dtest=OtpDeliveryServiceTest` (from `apps/backend-api`)
Expected: PASS — 5 tests

- [ ] **Step 5: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/application/otp/OtpDeliveryService.java apps/backend-api/src/test/java/com/limitflow/backend/application/otp/OtpDeliveryServiceTest.java
git commit -m "Add OtpDeliveryService: Twilio SMS when enabled, log fallback otherwise"
```

---

### Task 4: Wire `OtpDeliveryService` into `LimitRequestService`

**Files:**
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/limitrequest/LimitRequestService.java`
- Modify: `apps/backend-api/src/test/java/com/limitflow/backend/application/limitrequest/LimitRequestServiceTest.java`

**Interfaces:**
- Consumes: `OtpDeliveryService.deliver(User, String)` from Task 3.

- [ ] **Step 1: Write the failing test**

In `apps/backend-api/src/test/java/com/limitflow/backend/application/limitrequest/LimitRequestServiceTest.java`:

Add the import (alongside the other `com.limitflow.backend.application.otp.OtpService` import on line 6):

```java
import com.limitflow.backend.application.otp.OtpDeliveryService;
```

Add the mock field (after `otpService` on line 40):

```java
    @Mock
    private OtpDeliveryService otpDeliveryService;
```

Update the `service = new LimitRequestService(...)` call in `setUp()` (lines 54-55) to pass the new mock in the same position as the field order below:

```java
        service = new LimitRequestService(limitRequestRepository, accountRepository, otpService,
                otpDeliveryService, riskEngine, notificationService, auditService);
```

Add a new test (anywhere alongside the other `@Test` methods, e.g. after `submitRequestRejectsWhenAnActiveRequestAlreadyExists`):

```java
    @Test
    void submitRequestDeliversTheOtpThroughOtpDeliveryService() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(otpService.issue(any())).thenReturn("654321");

        service.submitRequest(customer, account.getId(), BigDecimal.valueOf(250_000), "reason", true);

        verify(otpDeliveryService).deliver(customer, "654321");
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw test -Dtest=LimitRequestServiceTest` (from `apps/backend-api`)
Expected: FAIL — compilation error (`LimitRequestService` has no matching 7-arg constructor yet)

- [ ] **Step 3: Wire the dependency into `LimitRequestService`**

In `apps/backend-api/src/main/java/com/limitflow/backend/application/limitrequest/LimitRequestService.java`:

Add the import (after line 7, `import com.limitflow.backend.application.otp.OtpService;`):

```java
import com.limitflow.backend.application.otp.OtpDeliveryService;
```

Add the field (after line 35, `private final OtpService otpService;`):

```java
    private final OtpDeliveryService otpDeliveryService;
```

Update `sendOtp` (lines 145-150) to also call delivery:

```java
    private void sendOtp(User customer, LimitRequest limitRequest) {
        String code = otpService.issue(limitRequest);
        otpDeliveryService.deliver(customer, code);
        notificationService.send(customer, NotificationType.OTP_SENT, "OTP sent",
                "Your verification code is " + code + ". It expires in 5 minutes. "
                        + "(Demo mode: shown here instead of SMS.)");
    }
```

(`@RequiredArgsConstructor` regenerates the constructor from field declaration order automatically — no manual constructor to edit.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw test -Dtest=LimitRequestServiceTest` (from `apps/backend-api`)
Expected: PASS — all existing tests plus the new one

- [ ] **Step 5: Run the full suite, including integration tests**

Run: `./mvnw test` (from `apps/backend-api`)
Expected: PASS — `LimitRequestFlowIntegrationTest` exercises `submitRequest` end-to-end through a real Spring context; with `TWILIO_ENABLED` defaulting to `false`, `OtpDeliveryService` takes the log-fallback path, so no real Twilio call happens and no test changes are needed there

- [ ] **Step 6: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/application/limitrequest/LimitRequestService.java apps/backend-api/src/test/java/com/limitflow/backend/application/limitrequest/LimitRequestServiceTest.java
git commit -m "Wire OtpDeliveryService into LimitRequestService.sendOtp"
```

---

## Manual verification (after all tasks)

1. `cd docker && docker compose up --build`
2. Log in as `customer@limitflow.demo` / `Password123!`, submit a limit increase request.
3. Check `backend-api` container logs — expect `OTP code (Twilio disabled or unavailable): <code>` (Twilio stays off by default in docker-compose).
4. Optionally, to prove the Twilio path works: set real `TWILIO_ENABLED=true`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` (a Twilio trial account's verified number) in `docker-compose.yml`, rebuild, repeat step 2, confirm an SMS arrives at `+2348012345678` (or update the seed migration to a real test number you control first).
