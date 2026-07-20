# Reactive Backend (WebFlux + R2DBC) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite `apps/backend-api` from Spring MVC + JPA (blocking) to Spring WebFlux + R2DBC (reactive), end to end, with controllers following the `XxxApi` interface + thin `XxxController` pattern from `recipeX-springboot-app`.

**Architecture:** A clean cutover (per explicit decision — see Global Constraints), not a staged coexistence migration: dependencies and config flip first, then persistence, then services, then security, then controllers, in that order, since Java's static typing means each layer's callers must convert together with what they call. Several early tasks in this plan will **not** compile until a later task lands — this is expected and intentional, not a defect; see Global Constraints.

**Tech Stack:** Spring Boot 3.5 WebFlux, Spring Data R2DBC, `r2dbc-postgresql`, Project Reactor (`Mono`/`Flux`), `reactor-test` (`StepVerifier`), `WebTestClient`, Flyway (unchanged, JDBC-based).

## Global Constraints

- **This is a clean cutover, chosen explicitly over staged coexistence.** Tasks 1–4 will leave the module *not compiling* — this is expected. Do not attempt to make an individual task's `mvnw test` pass if the task's own brief says compilation isn't expected to succeed yet; verify only what that task's brief asks you to verify. The module compiles and all tests pass again starting at Task 5.
- Entities use Spring Data R2DBC annotations (`org.springframework.data.annotation.Id`, `org.springframework.data.relational.core.mapping.Table`/`Column`), never `jakarta.persistence.*`.
- **No JPA-style relationships survive.** Every `@ManyToOne` becomes a raw FK `UUID` column on the owning side, with explicit fetches at the call site — R2DBC has no relationship mapping, no lazy proxies, no cascades.
- Enums persist as their `.name()` string by default in Spring Data R2DBC (confirmed: "Spring Data converts enum values to String for maximum portability" — [Spring Data Relational R2DBC mapping docs](https://docs.spring.io/spring-data/relational/reference/r2dbc/mapping.html)) — this matches the existing `VARCHAR` columns exactly. No custom converter is added unless Task 2's own round-trip test proves one is needed.
- Flyway keeps using the JDBC driver (`org.postgresql:postgresql`) via `spring.datasource.*` (kept unchanged, alongside the new `spring.r2dbc.*`) — not moved to standalone `spring.flyway.url/user/password` properties. A lightweight `spring-boot-starter-jdbc` dependency is added specifically to give `DataSourceAutoConfiguration` something to build, since removing `spring-boot-starter-data-jpa` removes JDBC support entirely otherwise. Production code never queries through this `DataSource` — only Flyway does.
- The test-side embedded Postgres (`io.zonky.test:embedded-database-spring-test`) has no R2DBC support and assigns a random port per run ([zonkyio/embedded-database-spring-test#121](https://github.com/zonkyio/embedded-database-spring-test/issues/121)). Every test that hits real data access needs the `EmbeddedR2dbcConfig` bridge from Task 2 imported, or R2DBC will try to connect to a Postgres that isn't running.
- Any currently-fire-and-forget `void` service call (`auditService.record(...)`, `notificationService.send(...)`) becomes a `Mono` that **must be chained** into the caller's reactive pipeline (`.then(...)`, `.flatMap(...)`). An un-subscribed `Mono` never executes — Reactor is lazy. This is the single easiest mistake to make in this rewrite; every task below is explicit about where chaining is required.
- `OtpDeliveryService.deliver(...)` calls Twilio's Java SDK, which is a **blocking** synchronous HTTP call. It must run on `Schedulers.boundedElastic()`, not the Reactor event-loop thread. `PasswordEncoder` (BCrypt) and `TokenService` (JWT sign/verify) are pure CPU, no I/O — they stay direct, unwrapped calls inside `.map()`/`.filter()`, per the design spec's explicit, named ceiling (revisit only if profiling ever shows contention).
- `@PreAuthorize` class-level annotations (`LimitRequestController` → `hasRole('CUSTOMER')`, `SupportController`/`AuditController` → `hasAnyRole('SUPPORT_AGENT','MANAGER')`) are preserved verbatim — reactive method security supports `Mono`/`Flux`-returning methods natively once `@EnableReactiveMethodSecurity` replaces `@EnableMethodSecurity`.
- No schema changes. `V1`–`V3` migrations are untouched.
- No change to REST paths or JSON response shapes — `customer-portal`/`employee-portal` need zero changes.

---

### Task 1: Dependencies and configuration cutover

**Files:**
- Modify: `apps/backend-api/pom.xml`
- Modify: `apps/backend-api/src/main/resources/application.yml`
- Modify: `docker/docker-compose.yml`

**Interfaces:** None yet — this task only changes what's on the classpath and how the app is configured. Nothing in this task is consumed by name; later tasks rely on the dependencies existing and the properties below resolving.

**Expected outcome: the module will NOT compile after this task.** Every `jakarta.persistence.*` import, every `JpaRepository`, every `@EnableWebSecurity`/`HttpSecurity` reference breaks. This is fully expected — do not attempt to fix it. Task 2 begins repairing compilation; full green returns at Task 5.

- [ ] **Step 1: Update `pom.xml`**

Remove these three `<dependency>` blocks entirely:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
```
```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
```

Add these in their place (webflux where `starter-web` was, r2dbc where `data-jpa` was, springdoc-webflux where `springdoc-webmvc` was), plus `spring-boot-starter-jdbc` for Flyway's `DataSource`, `r2dbc-postgresql`, and `reactor-test`:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>r2dbc-postgresql</artifactId>
        </dependency>
```
```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
```
Add to the `<dependencies>` block (test scope, alongside `spring-boot-starter-test`):
```xml
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
```

Leave `org.postgresql:postgresql` (JDBC driver, runtime scope), `flyway-core`, `flyway-database-postgresql`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, Lombok, `jjwt-*`, `io.zonky.test:*`, `h2` (test), `httpclient5` (test), `spring-security-test` (test) all exactly as they are — none of these are JPA/MVC-specific.

- [ ] **Step 2: Update `application.yml`**

Replace the full `spring:` block with:
```yaml
spring:
  application:
    name: limitflow-backend-api
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/limitflow}
    username: ${DB_USERNAME:limitflow}
    password: ${DB_PASSWORD:limitflow}
  r2dbc:
    url: ${R2DBC_URL:r2dbc:postgresql://localhost:5432/limitflow}
    username: ${DB_USERNAME:limitflow}
    password: ${DB_PASSWORD:limitflow}
  flyway:
    enabled: true
    locations: classpath:db/migration
```
`spring.datasource.*` is **kept, unchanged from today** — this is deliberate, not an oversight. Flyway still auto-detects and reuses this JDBC `DataSource` bean exactly as it already does today (no explicit `spring.flyway.url/user/password` needed). Just as importantly, keeping a real `DataSource` bean is what lets the test-side embedded Postgres provisioning work at all: `io.zonky.test:embedded-database-spring-test`'s `@AutoConfigureEmbeddedDatabase` works by intercepting and replacing an *existing* `DataSource` bean definition with one pointed at a freshly-started embedded instance (random port per run) — if there were no `DataSource` bean, it would have nothing to attach to. `spring.jpa.*` is gone (no JPA to configure). Only `spring.datasource.*` (JDBC, for Flyway) and the new `spring.r2dbc.*` (for the app's actual reactive data access) coexist.

**Known gap this creates, resolved in Task 2:** in tests, zonky assigns the embedded instance a *random* port, so the static `spring.r2dbc.url` default (`localhost:5432`) is wrong in the test context — R2DBC would try to talk to a Postgres that isn't running. There's no built-in bridge for this (Spring Data R2DBC has no equivalent of zonky's `@AutoConfigureEmbeddedDatabase` — see [zonkyio/embedded-database-spring-test#121](https://github.com/zonkyio/embedded-database-spring-test/issues/121), open and unresolved as of this writing). Task 2 adds a small test-only bridge that reads the actual port zonky assigned to the JDBC `DataSource` and builds a matching R2DBC `ConnectionFactory` from it.

Leave everything below `spring:` (`server:`, `limitflow:`, `springdoc:`, `management:`, `logging:`) exactly as-is — none of it is JPA/MVC-specific.

- [ ] **Step 3: Update `docker/docker-compose.yml`**

In the `backend-api` service's `environment:` block, add `R2DBC_URL` alongside the existing `DB_URL` (keep `DB_URL` — Flyway still needs it):
```yaml
      R2DBC_URL: r2dbc:postgresql://postgres:5432/limitflow
```

- [ ] **Step 4: Commit**

```bash
git add apps/backend-api/pom.xml apps/backend-api/src/main/resources/application.yml docker/docker-compose.yml
git commit -m "Cut over to WebFlux + R2DBC dependencies and config"
```

Do not run `mvnw test` or `mvnw compile` as a gate for this task — failure is expected. Proceed to Task 2.

---

### Task 2: Persistence layer — entities and repositories (all 7 domains)

**Files:**
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/user/User.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/user/UserRepository.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/account/Account.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/account/AccountRepository.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/limitrequest/LimitRequest.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/limitrequest/LimitRequestRepository.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/notification/Notification.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/notification/NotificationRepository.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/audit/AuditLog.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/audit/AuditLogRepository.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/support/SupportNote.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/support/SupportNoteRepository.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/otp/OtpCode.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/otp/OtpCodeRepository.java`
- Rename+modify (delete old, create new): `apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/persistence/UserJpaRepository.java` → `UserR2dbcRepository.java` (same for `AccountJpaRepository`→`AccountR2dbcRepository`, `LimitRequestJpaRepository`→`LimitRequestR2dbcRepository`, `NotificationJpaRepository`→`NotificationR2dbcRepository`, `AuditLogJpaRepository`→`AuditLogR2dbcRepository`, `SupportNoteJpaRepository`→`SupportNoteR2dbcRepository`, `OtpCodeJpaRepository`→`OtpCodeR2dbcRepository`)
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/EmbeddedR2dbcConfig.java` (new — bridges zonky's embedded JDBC DataSource to a matching R2DBC ConnectionFactory for tests; reused by Task 6)
- Test: `apps/backend-api/src/test/java/com/limitflow/backend/domain/user/UserR2dbcRepositoryTest.java` (new — the enum round-trip proof)

**Interfaces:**
- Produces: every repository port's methods now return `Mono<T>`/`Flux<T>` (exact signatures below) — consumed by Task 3's services.
- Produces: every entity's relationship field is now a raw `UUID` FK column (exact field names below) — consumed by Task 3's services, which must explicitly fetch related rows.

**Expected outcome: still will NOT compile** — every service in `application/*` still calls these repositories with the old blocking signatures. Task 3 fixes that.

- [ ] **Step 1: Convert `User`**

```java
package com.limitflow.backend.domain.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    private Role role;

    private String phone;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public User(String firstName, String lastName, String email, String passwordHash, Role role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
```

- [ ] **Step 2: Convert `UserRepository`**

```java
package com.limitflow.backend.domain.user;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository {

    Mono<User> save(User user);

    Mono<User> findById(UUID id);

    Mono<User> findByEmail(String email);
}
```

- [ ] **Step 3: Convert `Account`**

```java
package com.limitflow.backend.domain.account;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("user_id")
    private UUID userId;

    @Column("account_number")
    private String accountNumber;

    @Column("daily_limit")
    private BigDecimal dailyLimit;

    @Column("used_today")
    private BigDecimal usedToday = BigDecimal.ZERO;

    private AccountStatus status = AccountStatus.ACTIVE;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public Account(UUID userId, String accountNumber, BigDecimal dailyLimit, BigDecimal usedToday) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
    }

    public BigDecimal remaining() {
        BigDecimal remaining = dailyLimit.subtract(usedToday);
        return remaining.max(BigDecimal.ZERO);
    }

    public void applyNewLimit(BigDecimal newLimit) {
        this.dailyLimit = newLimit;
    }
}
```

- [ ] **Step 4: Convert `AccountRepository`**

```java
package com.limitflow.backend.domain.account;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountRepository {

    Mono<Account> save(Account account);

    Mono<Account> findById(UUID id);

    Flux<Account> findByUserId(UUID userId);
}
```

- [ ] **Step 5: Convert `LimitRequest`**

```java
package com.limitflow.backend.domain.limitrequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("limit_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LimitRequest {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("account_id")
    private UUID accountId;

    @Column("current_limit")
    private BigDecimal currentLimit;

    @Column("requested_limit")
    private BigDecimal requestedLimit;

    private String reason;

    private RequestStatus status = RequestStatus.PENDING;

    @Column("risk_level")
    private RiskLevel riskLevel;

    @Column("known_device")
    private boolean knownDevice = true;

    @Column("otp_verified_at")
    private Instant otpVerifiedAt;

    @Column("biometric_verified_at")
    private Instant biometricVerifiedAt;

    @Column("resolved_by")
    private UUID resolvedByUserId;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Column("updated_at")
    private Instant updatedAt = Instant.now();

    public LimitRequest(UUID accountId, BigDecimal currentLimit, BigDecimal requestedLimit,
                         String reason, boolean knownDevice) {
        this.accountId = accountId;
        this.currentLimit = currentLimit;
        this.requestedLimit = requestedLimit;
        this.reason = reason;
        this.knownDevice = knownDevice;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void transitionTo(RequestStatus newStatus) {
        this.status = newStatus;
        touch();
    }

    public boolean isFullyVerified() {
        return otpVerifiedAt != null && biometricVerifiedAt != null;
    }
}
```

- [ ] **Step 6: Convert `LimitRequestRepository`**

```java
package com.limitflow.backend.domain.limitrequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LimitRequestRepository {

    Mono<LimitRequest> save(LimitRequest limitRequest);

    Mono<LimitRequest> findById(UUID id);

    Flux<LimitRequest> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Flux<LimitRequest> findByStatusInOrderByCreatedAtAsc(List<RequestStatus> statuses);

    Flux<LimitRequest> findByRiskLevelAndStatusInOrderByCreatedAtAsc(RiskLevel riskLevel, List<RequestStatus> statuses);

    Mono<Boolean> existsByAccountIdAndStatusIn(UUID accountId, Collection<RequestStatus> statuses);

    Mono<Long> countByAccountIdAndCreatedAtAfter(UUID accountId, Instant createdAt);
}
```

- [ ] **Step 7: Convert `Notification`**

```java
package com.limitflow.backend.domain.notification;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("user_id")
    private UUID userId;

    private NotificationType type;

    private String title;

    private String message;

    @Column("read_at")
    private Instant readAt;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public Notification(UUID userId, NotificationType type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public void markRead() {
        this.readAt = Instant.now();
    }
}
```

- [ ] **Step 8: Convert `NotificationRepository`**

```java
package com.limitflow.backend.domain.notification;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NotificationRepository {

    Mono<Notification> save(Notification notification);

    Flux<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
```

- [ ] **Step 9: Convert `AuditLog`**

```java
package com.limitflow.backend.domain.audit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("audit_logs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("actor_user_id")
    private UUID actorUserId;

    private String action;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private String entityId;

    private String metadata;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public AuditLog(UUID actorUserId, String action, String entityType, String entityId, String metadata) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
    }
}
```

- [ ] **Step 10: Convert `AuditLogRepository`**

```java
package com.limitflow.backend.domain.audit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuditLogRepository {

    Mono<AuditLog> save(AuditLog auditLog);

    Flux<AuditLog> findAllByOrderByCreatedAtDesc();
}
```

- [ ] **Step 11: Convert `SupportNote`**

```java
package com.limitflow.backend.domain.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("support_notes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportNote {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("limit_request_id")
    private UUID limitRequestId;

    @Column("author_user_id")
    private UUID authorUserId;

    private String note;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public SupportNote(UUID limitRequestId, UUID authorUserId, String note) {
        this.limitRequestId = limitRequestId;
        this.authorUserId = authorUserId;
        this.note = note;
    }
}
```

- [ ] **Step 12: Convert `SupportNoteRepository`**

```java
package com.limitflow.backend.domain.support;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SupportNoteRepository {

    Mono<SupportNote> save(SupportNote supportNote);

    Flux<SupportNote> findByLimitRequestIdOrderByCreatedAtAsc(UUID limitRequestId);
}
```

- [ ] **Step 13: Convert `OtpCode`**

```java
package com.limitflow.backend.domain.otp;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("otp_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpCode {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("limit_request_id")
    private UUID limitRequestId;

    @Column("code_hash")
    private String codeHash;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("verified_at")
    private Instant verifiedAt;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    public OtpCode(UUID limitRequestId, String codeHash, Instant expiresAt) {
        this.limitRequestId = limitRequestId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}
```

- [ ] **Step 14: Convert `OtpCodeRepository`**

```java
package com.limitflow.backend.domain.otp;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OtpCodeRepository {

    Mono<OtpCode> save(OtpCode otpCode);

    Mono<OtpCode> findTopByLimitRequestIdOrderByCreatedAtDesc(UUID limitRequestId);
}
```

- [ ] **Step 15: Replace all 7 JPA repository adapter interfaces with R2DBC ones**

Delete these 7 files: `UserJpaRepository.java`, `AccountJpaRepository.java`, `LimitRequestJpaRepository.java`, `NotificationJpaRepository.java`, `AuditLogJpaRepository.java`, `SupportNoteJpaRepository.java`, `OtpCodeJpaRepository.java` (all in `apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/persistence/`).

Create these 7 in their place, following the same "one interface extends both the Spring Data base and the domain port" pattern the JPA versions used — `R2dbcRepository`'s own `findById`/`save` signatures already return `Mono`, so they merge cleanly with the domain ports above:

`UserR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface UserR2dbcRepository extends R2dbcRepository<User, UUID>, UserRepository {
}
```

`AccountR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface AccountR2dbcRepository extends R2dbcRepository<Account, UUID>, AccountRepository {
}
```

`LimitRequestR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface LimitRequestR2dbcRepository extends R2dbcRepository<LimitRequest, UUID>, LimitRequestRepository {
}
```

`NotificationR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.notification.Notification;
import com.limitflow.backend.domain.notification.NotificationRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface NotificationR2dbcRepository extends R2dbcRepository<Notification, UUID>, NotificationRepository {
}
```

`AuditLogR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.audit.AuditLog;
import com.limitflow.backend.domain.audit.AuditLogRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface AuditLogR2dbcRepository extends R2dbcRepository<AuditLog, UUID>, AuditLogRepository {
}
```

`SupportNoteR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.support.SupportNoteRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface SupportNoteR2dbcRepository extends R2dbcRepository<SupportNote, UUID>, SupportNoteRepository {
}
```

`OtpCodeR2dbcRepository.java`:
```java
package com.limitflow.backend.infrastructure.persistence;

import com.limitflow.backend.domain.otp.OtpCode;
import com.limitflow.backend.domain.otp.OtpCodeRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface OtpCodeR2dbcRepository extends R2dbcRepository<OtpCode, UUID>, OtpCodeRepository {
}
```

- [ ] **Step 16: Add the zonky-to-R2DBC test bridge**

Create `apps/backend-api/src/test/java/com/limitflow/backend/EmbeddedR2dbcConfig.java` — every test that needs real reactive data access against the embedded Postgres imports this, since `spring.r2dbc.url`'s static default points at a port nothing is listening on during tests:

```java
package com.limitflow.backend;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridges zonky's embedded-database-spring-test (JDBC-only — no R2DBC support, see
 * https://github.com/zonkyio/embedded-database-spring-test/issues/121 — and a random port per
 * test run) to R2DBC: reads the actual host/port/database/user the embedded instance's JDBC
 * DataSource ended up with, and builds a matching R2DBC ConnectionFactory from it, overriding the
 * static {@code spring.r2dbc.url} default that only resolves outside tests.
 */
@TestConfiguration
public class EmbeddedR2dbcConfig {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    @Bean
    @Primary
    public ConnectionFactory testConnectionFactory(DataSource dataSource) throws Exception {
        String jdbcUrl;
        String username;
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
            username = connection.getMetaData().getUserName();
        }
        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unexpected embedded JDBC URL shape: " + jdbcUrl);
        }

        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, matcher.group(1))
                .option(ConnectionFactoryOptions.PORT, Integer.parseInt(matcher.group(2)))
                .option(ConnectionFactoryOptions.DATABASE, matcher.group(3))
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, "")
                .build());
    }
}
```

(The embedded instance uses trust-style local authentication — zonky's own JDBC URLs already connect with no real password, e.g. `jdbc:postgresql://localhost:65014/xxxx?user=postgres` — so an empty R2DBC password matches that, not a real credential being discarded.)

- [ ] **Step 17: Write and run the enum round-trip proof test**

This is the one thing in this task that genuinely needs a real test — confirming the "no custom converter needed" assumption in Global Constraints holds against the real embedded Postgres before every later task quietly depends on it, and proving the bridge from Step 16 actually works.

Create `apps/backend-api/src/test/java/com/limitflow/backend/domain/user/UserR2dbcRepositoryTest.java`:
```java
package com.limitflow.backend.domain.user;

import com.limitflow.backend.EmbeddedR2dbcConfig;
import com.limitflow.backend.infrastructure.persistence.UserR2dbcRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY, refresh = BEFORE_EACH_TEST_METHOD)
@Import(EmbeddedR2dbcConfig.class)
class UserR2dbcRepositoryTest {

    @Autowired
    private UserR2dbcRepository userR2dbcRepository;

    @Test
    void roleEnumRoundTripsAsItsNameString() {
        User user = new User("Test", "User", "role-roundtrip@limitflow.test", "hash", Role.MANAGER);

        StepVerifier.create(
                        userR2dbcRepository.save(user)
                                .flatMap(saved -> userR2dbcRepository.findById(saved.getId())))
                .assertNext(found -> {
                    if (found.getRole() != Role.MANAGER) {
                        throw new AssertionError("Expected role MANAGER, got " + found.getRole());
                    }
                })
                .verifyComplete();
    }
}
```

Run: `./mvnw test -Dtest=UserR2dbcRepositoryTest` (from `apps/backend-api`; needs `JAVA_HOME` set, e.g. to the Temurin 21 JDK if not already on `PATH`)

Expected: **This will fail to compile** alongside the rest of the still-broken module (Task 3 hasn't run yet) — that's fine for now; **do not** try to get this specific test green in isolation yet. Instead, verify this file's *code* is correct by reading it against Step 15's `UserR2dbcRepository` and the Global Constraints' enum-mapping claim. The actual green run of this test happens for real once Task 3 restores overall compilation — re-run it there and confirm PASS at that point (Task 3's own steps say so explicitly).

- [ ] **Step 18: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/domain apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/persistence apps/backend-api/src/test/java/com/limitflow/backend/EmbeddedR2dbcConfig.java apps/backend-api/src/test/java/com/limitflow/backend/domain/user/UserR2dbcRepositoryTest.java
git commit -m "Convert entities and repositories to R2DBC (all 7 domains)"
```

---

### Task 3: Application services (all 8 services)

**Files:**
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/user/UserRepository.java` (add one batch-fetch method)
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/domain/account/AccountRepository.java` (add one batch-fetch method, used by `SupportReviewService.queueFor`)
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/audit/AuditService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/notification/NotificationService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/otp/OtpService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/otp/OtpDeliveryService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/customer/CustomerService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/auth/AuthService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/limitrequest/LimitRequestService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/application/support/SupportReviewService.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/limitrequest/LimitRequestResponse.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/audit/AuditLogResponse.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/support/SupportNoteResponse.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/support/SupportQueueItemResponse.java`

**Interfaces:**
- Consumes: Task 2's repository ports (`Mono`/`Flux`-returning), entity FK fields (`Account.userId`, `LimitRequest.accountId`, `LimitRequest.resolvedByUserId`, `Notification.userId`, `AuditLog.actorUserId`, `SupportNote.limitRequestId`/`authorUserId`, `OtpCode.limitRequestId`).
- Produces: every public service method now returns `Mono<T>`/`Flux<T>` (exact signatures in the code below) — consumed by Task 5's controllers. `AuditService.findAll()`, `SupportReviewService.notesFor(...)`/`addStaffNote(...)`/`queueFor(...)` now return **response DTOs directly** (`Flux<AuditLogResponse>`, `Flux<SupportNoteResponse>`, `Mono<SupportNoteResponse>`, `Flux<SupportQueueItemResponse>`), not raw entities — see the note below on why.

**Why three services now return DTOs instead of entities:** `AuditLogResponse.from(...)`, `SupportNoteResponse.from(...)`, and `SupportQueueItemResponse.from(...)` each need a related row's display name (actor, author, or customer) that used to come for free through a JPA association (`auditLog.getActor().fullName()` etc.). That's now an explicit async fetch. A controller's `.map(XxxResponse::from)` can only do *synchronous* transformations — if the mapping needs an async fetch, that fetch has to happen inside a `.flatMap()`, which means real logic in the controller, breaking the "controller is pure delegation" rule this whole rewrite is modeling on recipeX. So for exactly these three cases, the DTO assembly moves into the service, where the reactive fetch orchestration already lives. `AccountResponse`, `NotificationResponse`, `UserSummary`, and (with one field simplified) `LimitRequestResponse` need no extra fetch and keep their existing controller-side `.map(...)` untouched in Task 5.

**Expected outcome: still will NOT compile** — the 7 controllers (Task 5) and the security layer (Task 4) still call these services with pre-reactive expectations. Full green returns at Task 5.

- [ ] **Step 1: Add a batch-fetch method to `UserRepository`**

`R2dbcRepository` already provides `findAllById(Iterable<ID>)`, but the domain port doesn't expose it yet — services depend on the port, not the infrastructure type, so add it explicitly:

```java
package com.limitflow.backend.domain.user;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository {

    Mono<User> save(User user);

    Mono<User> findById(UUID id);

    Mono<User> findByEmail(String email);

    Flux<User> findAllById(Iterable<UUID> ids);
}
```

(`UserR2dbcRepository` from Task 2 already satisfies this — `R2dbcRepository.findAllById` matches this signature exactly, no change needed there.)

- [ ] **Step 2: Convert `AuditService`**

```java
package com.limitflow.backend.application.audit;

import com.limitflow.backend.domain.audit.AuditLog;
import com.limitflow.backend.domain.audit.AuditLogRepository;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public Mono<AuditLog> record(User actor, String action, String entityType, String entityId) {
        return record(actor, action, entityType, entityId, null);
    }

    public Mono<AuditLog> record(User actor, String action, String entityType, String entityId, String metadata) {
        return auditLogRepository.save(new AuditLog(actor.getId(), action, entityType, entityId, metadata));
    }

    public Flux<AuditLogResponse> findAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .collectList()
                .flatMapMany(logs -> {
                    var actorIds = logs.stream().map(AuditLog::getActorUserId).distinct().toList();
                    return userRepository.findAllById(actorIds)
                            .collectMap(User::getId, User::fullName)
                            .flatMapMany(namesById -> Flux.fromIterable(logs)
                                    .map(log -> AuditLogResponse.from(log, namesById.get(log.getActorUserId()))));
                });
    }
}
```

- [ ] **Step 3: Convert `NotificationService`**

The `send` parameter changes from `User user` to `UUID userId` — every call site that used to pass a full `User` only ever needed its ID, and after Task 2 several callers (`SupportReviewService`) only have a FK `UUID` in hand, not a `User` object, so this avoids forcing an unnecessary extra fetch just to unwrap an ID.

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

    public Mono<Notification> send(UUID userId, NotificationType type, String title, String message) {
        return notificationRepository.save(new Notification(userId, type, title, message));
    }

    public Flux<Notification> findForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
```

- [ ] **Step 4: Convert `OtpService`**

```java
package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.otp.OtpCode;
import com.limitflow.backend.domain.otp.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public Mono<String> issue(LimitRequest limitRequest) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpCode otpCode = new OtpCode(limitRequest.getId(), passwordEncoder.encode(code), Instant.now().plus(TTL));
        return otpCodeRepository.save(otpCode).thenReturn(code);
    }

    public Mono<Boolean> verify(LimitRequest limitRequest, String code) {
        return otpCodeRepository.findTopByLimitRequestIdOrderByCreatedAtDesc(limitRequest.getId())
                .filter(otp -> !otp.isExpired())
                .filter(otp -> passwordEncoder.matches(code, otp.getCodeHash()))
                .flatMap(this::markVerified)
                .map(otp -> true)
                .defaultIfEmpty(false);
    }

    private Mono<OtpCode> markVerified(OtpCode otpCode) {
        otpCode.setVerifiedAt(Instant.now());
        return otpCodeRepository.save(otpCode);
    }
}
```

- [ ] **Step 5: Convert `OtpDeliveryService`**

The enabled/phone-present decision (`shouldSendViaTwilio`) is pure and unaffected. The Twilio SDK call itself is a **blocking** synchronous HTTP call, so it moves onto `Schedulers.boundedElastic()` rather than running on the Reactor event loop:

```java
package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.user.User;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public Mono<Void> deliver(User customer, String code) {
        return Mono.fromRunnable(() -> deliverBlocking(customer, code))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void deliverBlocking(User customer, String code) {
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

`shouldSendViaTwilio`'s package-private visibility and behavior are byte-for-byte unchanged, so the existing `OtpDeliveryServiceTest`'s tests of it need no changes — only its `deliver(...)`-calling tests change (see Task 6).

- [ ] **Step 6: Convert `CustomerService`**

```java
package com.limitflow.backend.application.customer;

import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final AccountRepository accountRepository;

    public Flux<Account> accountsFor(User customer) {
        return accountRepository.findByUserId(customer.getId());
    }

    public Mono<Account> primaryAccount(User customer) {
        return accountsFor(customer)
                .next()
                .switchIfEmpty(Mono.error(new NotFoundException("No account found for this customer")));
    }
}
```

- [ ] **Step 7: Convert `AuthService`**

```java
package com.limitflow.backend.application.auth;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.domain.exception.InvalidCredentialsException;
import com.limitflow.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditService auditService;

    public Mono<AuthResult> login(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid email or password")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                        return Mono.error(new InvalidCredentialsException("Invalid email or password"));
                    }
                    String token = tokenService.generateToken(user);
                    return auditService.record(user, "LOGGED_IN", "User", user.getId().toString())
                            .thenReturn(new AuthResult(token, user));
                });
    }
}
```

- [ ] **Step 8: Convert `LimitRequestService`**

```java
package com.limitflow.backend.application.limitrequest;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.limitrequest.risk.RiskContext;
import com.limitflow.backend.application.limitrequest.risk.RiskEngine;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpDeliveryService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.ForbiddenException;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.exception.ValidationException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.notification.NotificationType;
import com.limitflow.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LimitRequestService {

    private final LimitRequestRepository limitRequestRepository;
    private final AccountRepository accountRepository;
    private final OtpService otpService;
    private final OtpDeliveryService otpDeliveryService;
    private final RiskEngine riskEngine;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public Mono<LimitRequest> submitRequest(User customer, UUID accountId, BigDecimal requestedLimit,
                                             String reason, boolean knownDevice) {
        return ownedAccount(customer, accountId)
                .flatMap(account -> {
                    if (requestedLimit.compareTo(account.getDailyLimit()) <= 0) {
                        return Mono.<LimitRequest>error(
                                new ValidationException("Requested limit must be greater than the current limit"));
                    }
                    return limitRequestRepository.existsByAccountIdAndStatusIn(account.getId(), RequestStatus.ACTIVE)
                            .flatMap(hasActive -> {
                                if (hasActive) {
                                    return Mono.<LimitRequest>error(new ValidationException(
                                            "You already have a limit increase request in progress"));
                                }
                                LimitRequest limitRequest = new LimitRequest(account.getId(), account.getDailyLimit(),
                                        requestedLimit, reason, knownDevice);
                                limitRequest.transitionTo(RequestStatus.OTP_PENDING);
                                return limitRequestRepository.save(limitRequest);
                            });
                })
                .flatMap(limitRequest -> auditService.record(customer, "LIMIT_REQUESTED", "LimitRequest",
                                limitRequest.getId().toString())
                        .then(sendOtp(customer, limitRequest))
                        .thenReturn(limitRequest));
    }

    public Mono<LimitRequest> verifyOtp(User customer, UUID requestId, String code) {
        return ownedRequest(customer, requestId)
                .flatMap(limitRequest -> {
                    requireStatus(limitRequest, RequestStatus.OTP_PENDING);
                    return otpService.verify(limitRequest, code)
                            .flatMap(verified -> {
                                if (!verified) {
                                    return Mono.<LimitRequest>error(
                                            new ValidationException("Invalid or expired verification code"));
                                }
                                limitRequest.setOtpVerifiedAt(Instant.now());
                                limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);
                                return limitRequestRepository.save(limitRequest);
                            });
                })
                .flatMap(limitRequest -> auditService.record(customer, "OTP_VERIFIED", "LimitRequest",
                                limitRequest.getId().toString())
                        .thenReturn(limitRequest));
    }

    @Transactional
    public Mono<LimitRequest> verifyBiometric(User customer, UUID requestId, boolean success) {
        return ownedRequest(customer, requestId)
                .flatMap(limitRequest -> {
                    requireStatus(limitRequest, RequestStatus.BIOMETRIC_PENDING);
                    if (!success) {
                        return Mono.<LimitRequest>error(new ValidationException("Biometric confirmation failed"));
                    }
                    limitRequest.setBiometricVerifiedAt(Instant.now());
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(limitRequest -> auditService.record(customer, "BIOMETRIC_VERIFIED", "LimitRequest",
                                limitRequest.getId().toString())
                        .then(notificationService.send(customer.getId(), NotificationType.VERIFICATION_COMPLETED,
                                "Verification complete", "Identity verification complete. We're assessing your request now."))
                        .then(assessRisk(customer, limitRequest)));
    }

    public Flux<LimitRequest> history(User customer, UUID accountId) {
        return ownedAccount(customer, accountId)
                .flatMapMany(account -> limitRequestRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()));
    }

    public Mono<LimitRequest> get(User requester, UUID requestId) {
        // Reached only via GET /api/limits/{id}, which is customer-only (see
        // LimitRequestController's class-level @PreAuthorize) — staff use the separate
        // GET /api/support/requests/{id}, which has no ownership check of its own.
        return ownedRequest(requester, requestId);
    }

    private Mono<LimitRequest> assessRisk(User customer, LimitRequest limitRequest) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        return limitRequestRepository.countByAccountIdAndCreatedAtAfter(limitRequest.getAccountId(), since)
                .flatMap(recentRequests -> {
                    boolean suspiciousActivity = recentRequests > 2;
                    RiskContext context = new RiskContext(
                            limitRequest.getCurrentLimit(),
                            limitRequest.getRequestedLimit(),
                            limitRequest.isKnownDevice(),
                            suspiciousActivity);
                    RiskLevel risk = riskEngine.assess(context);
                    limitRequest.setRiskLevel(risk);

                    Mono<LimitRequest> afterAudit = auditService.record(customer, "RISK_ASSESSED", "LimitRequest",
                                    limitRequest.getId().toString(), risk.name())
                            .thenReturn(limitRequest);

                    if (risk == RiskLevel.LOW) {
                        return afterAudit.flatMap(lr -> approveAutomatically(customer, lr));
                    }
                    return afterAudit.flatMap(lr -> {
                        lr.transitionTo(RequestStatus.UNDER_REVIEW);
                        return limitRequestRepository.save(lr)
                                .flatMap(saved -> notificationService.send(customer.getId(),
                                                NotificationType.VERIFICATION_COMPLETED, "Under review",
                                                "Your request needs a quick manual review. We'll notify you as soon as it's decided.")
                                        .thenReturn(saved));
                    });
                });
    }

    private Mono<LimitRequest> approveAutomatically(User customer, LimitRequest limitRequest) {
        return accountRepository.findById(limitRequest.getAccountId())
                .flatMap(account -> {
                    account.applyNewLimit(limitRequest.getRequestedLimit());
                    return accountRepository.save(account);
                })
                .then(Mono.defer(() -> {
                    limitRequest.transitionTo(RequestStatus.APPROVED);
                    return limitRequestRepository.save(limitRequest);
                }))
                .flatMap(saved -> auditService.record(customer, "LIMIT_APPROVED", "LimitRequest", saved.getId().toString())
                        .then(notificationService.send(customer.getId(), NotificationType.LIMIT_APPROVED, "Limit increased",
                                "Your daily transfer limit is now " + saved.getRequestedLimit()))
                        .thenReturn(saved));
    }

    private Mono<Void> sendOtp(User customer, LimitRequest limitRequest) {
        return otpService.issue(limitRequest)
                .flatMap(code -> otpDeliveryService.deliver(customer, code)
                        .then(notificationService.send(customer.getId(), NotificationType.OTP_SENT, "OTP sent",
                                "Your verification code is " + code + ". It expires in 5 minutes. "
                                        + "(Demo mode: shown here instead of SMS.)")))
                .then();
    }

    private Mono<Account> ownedAccount(User customer, UUID accountId) {
        return accountRepository.findById(accountId)
                .filter(account -> account.getUserId().equals(customer.getId()))
                .switchIfEmpty(Mono.error(new NotFoundException("Account not found")));
    }

    private Mono<LimitRequest> ownedRequest(User customer, UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new NotFoundException("Limit request not found")))
                .flatMap(limitRequest -> accountRepository.findById(limitRequest.getAccountId())
                        .flatMap(account -> {
                            if (!account.getUserId().equals(customer.getId())) {
                                return Mono.<LimitRequest>error(new ForbiddenException("You cannot act on this request"));
                            }
                            return Mono.just(limitRequest);
                        }));
    }

    private void requireStatus(LimitRequest limitRequest, RequestStatus expected) {
        if (limitRequest.getStatus() != expected) {
            throw new ValidationException("Request is not awaiting " + expected);
        }
    }
}
```

- [ ] **Step 9: Convert `SupportReviewService`**

```java
package com.limitflow.backend.application.support;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.ForbiddenException;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.exception.ValidationException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.notification.NotificationType;
import com.limitflow.backend.domain.support.SupportNote;
import com.limitflow.backend.domain.support.SupportNoteRepository;
import com.limitflow.backend.domain.user.Role;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.domain.user.UserRepository;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportReviewService {

    private static final List<RequestStatus> REVIEWABLE = List.of(RequestStatus.UNDER_REVIEW);

    private final LimitRequestRepository limitRequestRepository;
    private final AccountRepository accountRepository;
    private final SupportNoteRepository supportNoteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OtpService otpService;

    /** Support agents see MEDIUM-risk requests; managers see the HIGH-risk escalations. */
    public Flux<SupportQueueItemResponse> queueFor(Role role) {
        return limitRequestRepository.findByRiskLevelAndStatusInOrderByCreatedAtAsc(scopeFor(role), REVIEWABLE)
                .collectList()
                .flatMapMany(requests -> accountRepository.findAllById(
                                requests.stream().map(LimitRequest::getAccountId).distinct().toList())
                        .collectMap(Account::getId, Account::getUserId)
                        .flatMapMany(ownerIdByAccountId -> userRepository.findAllById(
                                        ownerIdByAccountId.values().stream().distinct().toList())
                                .collectMap(User::getId, User::fullName)
                                .flatMapMany(nameByUserId -> Flux.fromIterable(requests)
                                        .map(request -> SupportQueueItemResponse.from(request,
                                                nameByUserId.get(ownerIdByAccountId.get(request.getAccountId())))))));
    }

    /** Unlike {@link #reviewable}, this isn't limited to UNDER_REVIEW — staff can look
     * up a request's detail regardless of whether it's already been decided. */
    public Mono<LimitRequest> getForReview(UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new NotFoundException("Limit request not found")));
    }

    @Transactional
    public Mono<LimitRequest> approve(User staff, UUID requestId, String note) {
        return reviewable(staff, requestId)
                .flatMap(limitRequest -> accountRepository.findById(limitRequest.getAccountId())
                        .flatMap(account -> {
                            account.applyNewLimit(limitRequest.getRequestedLimit());
                            return accountRepository.save(account);
                        })
                        .flatMap(account -> {
                            limitRequest.setResolvedByUserId(staff.getId());
                            limitRequest.transitionTo(RequestStatus.APPROVED);
                            return limitRequestRepository.save(limitRequest)
                                    .flatMap(saved -> recordNote(staff, saved, account.getUserId(), note)
                                            .then(auditService.record(staff, "MANUAL_APPROVED", "LimitRequest",
                                                    saved.getId().toString()))
                                            .then(notificationService.send(account.getUserId(),
                                                    NotificationType.LIMIT_APPROVED, "Limit increased",
                                                    "Your daily transfer limit is now " + saved.getRequestedLimit()))
                                            .thenReturn(saved));
                        }));
    }

    public Mono<LimitRequest> reject(User staff, UUID requestId, String note) {
        return reviewable(staff, requestId)
                .flatMap(limitRequest -> {
                    limitRequest.setResolvedByUserId(staff.getId());
                    limitRequest.transitionTo(RequestStatus.REJECTED);
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(saved -> accountOwnerIdOf(saved)
                        .flatMap(ownerId -> recordNote(staff, saved, ownerId, note)
                                .then(auditService.record(staff, "MANUAL_REJECTED", "LimitRequest", saved.getId().toString()))
                                .then(notificationService.send(ownerId, NotificationType.LIMIT_REJECTED, "Request declined",
                                        note != null && !note.isBlank() ? note : "Your limit increase request was declined."))
                                .thenReturn(saved)));
    }

    public Mono<LimitRequest> requestAdditionalVerification(User staff, UUID requestId, String note) {
        return reviewable(staff, requestId)
                .flatMap(limitRequest -> {
                    limitRequest.transitionTo(RequestStatus.OTP_PENDING);
                    return limitRequestRepository.save(limitRequest);
                })
                .flatMap(saved -> accountOwnerIdOf(saved)
                        .flatMap(ownerId -> recordNote(staff, saved, ownerId, note)
                                .then(auditService.record(staff, "VERIFICATION_REQUESTED", "LimitRequest",
                                        saved.getId().toString()))
                                .then(otpService.issue(saved))
                                .flatMap(code -> notificationService.send(ownerId, NotificationType.VERIFICATION_REQUESTED,
                                        "Additional verification needed",
                                        "We need to re-verify this request. Your new code is " + code + "."))
                                .thenReturn(saved)));
    }

    public Mono<SupportNoteResponse> addStaffNote(User staff, UUID requestId, String note) {
        return inScope(staff, requestId)
                .flatMap(limitRequest -> accountOwnerIdOf(limitRequest)
                        .flatMap(ownerId -> recordNote(staff, limitRequest, ownerId, note)))
                .switchIfEmpty(Mono.error(new ValidationException("Note text is required")));
    }

    public Flux<SupportNoteResponse> notesFor(User staff, UUID requestId) {
        return inScope(staff, requestId)
                .flatMapMany(limitRequest -> supportNoteRepository.findByLimitRequestIdOrderByCreatedAtAsc(requestId)
                        .collectList()
                        .flatMapMany(notes -> userRepository.findAllById(
                                        notes.stream().map(SupportNote::getAuthorUserId).distinct().toList())
                                .collectMap(User::getId, User::fullName)
                                .flatMapMany(nameByUserId -> Flux.fromIterable(notes)
                                        .map(supportNote -> SupportNoteResponse.from(supportNote,
                                                nameByUserId.get(supportNote.getAuthorUserId()))))));
    }

    /** Note assembly returns the persisted note as a response DTO — {@code Mono.empty()} when
     * there's no note text, matching the previous "return null" no-op behavior. */
    private Mono<SupportNoteResponse> recordNote(User staff, LimitRequest limitRequest, UUID accountOwnerId, String note) {
        if (note == null || note.isBlank()) {
            return Mono.empty();
        }
        return supportNoteRepository.save(new SupportNote(limitRequest.getId(), staff.getId(), note))
                .flatMap(supportNote -> notificationService.send(accountOwnerId, NotificationType.SUPPORT_COMMENT,
                                "New message from support", note)
                        .thenReturn(SupportNoteResponse.from(supportNote, staff.fullName())));
    }

    private Mono<UUID> accountOwnerIdOf(LimitRequest limitRequest) {
        return accountRepository.findById(limitRequest.getAccountId()).map(Account::getUserId);
    }

    private Mono<LimitRequest> reviewable(User staff, UUID requestId) {
        return inScope(staff, requestId)
                .flatMap(limitRequest -> {
                    if (limitRequest.getStatus() != RequestStatus.UNDER_REVIEW) {
                        return Mono.<LimitRequest>error(new ValidationException("Request is not awaiting review"));
                    }
                    return Mono.just(limitRequest);
                });
    }

    /** Support agents only act on MEDIUM-risk requests; managers only on HIGH — mirrors {@link #queueFor}. */
    private Mono<LimitRequest> inScope(User staff, UUID requestId) {
        return limitRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new NotFoundException("Limit request not found")))
                .flatMap(limitRequest -> {
                    RiskLevel riskLevel = limitRequest.getRiskLevel();
                    if (riskLevel != null && riskLevel != scopeFor(staff.getRole())) {
                        return Mono.<LimitRequest>error(new ForbiddenException("This request is outside your review scope"));
                    }
                    return Mono.just(limitRequest);
                });
    }

    private RiskLevel scopeFor(Role role) {
        return role == Role.MANAGER ? RiskLevel.HIGH : RiskLevel.MEDIUM;
    }
}
```

Add the same `findAllById` method to `AccountRepository` used by `queueFor` above:

```java
package com.limitflow.backend.domain.account;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountRepository {

    Mono<Account> save(Account account);

    Mono<Account> findById(UUID id);

    Flux<Account> findByUserId(UUID userId);

    Flux<Account> findAllById(Iterable<UUID> ids);
}
```

- [ ] **Step 10: Fix the 3 DTOs that need an extra display name, and simplify the 1 that no longer needs a fetch**

`LimitRequestResponse.java` — simplify (no signature change, `request.getAccount().getId()` → `request.getAccountId()`):
```java
    public static LimitRequestResponse from(LimitRequest request) {
        return new LimitRequestResponse(
                request.getId(),
                request.getAccountId(),
                request.getCurrentLimit(),
                request.getRequestedLimit(),
                request.getReason(),
                request.getStatus().name(),
                request.getRiskLevel() != null ? request.getRiskLevel().name() : null,
                request.getCreatedAt(),
                request.getUpdatedAt(),
                buildTimeline(request));
    }
```
(`buildTimeline` and everything else in this file is unchanged.)

`AuditLogResponse.java` — add the resolved actor name as a parameter:
```java
    public static AuditLogResponse from(AuditLog auditLog, String actorName) {
        return new AuditLogResponse(
                auditLog.getId(),
                actorName,
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt());
    }
```

`SupportNoteResponse.java` — same shape:
```java
    public static SupportNoteResponse from(SupportNote supportNote, String authorName) {
        return new SupportNoteResponse(
                supportNote.getId(),
                authorName,
                supportNote.getNote(),
                supportNote.getCreatedAt());
    }
```

`SupportQueueItemResponse.java` — same shape:
```java
    public static SupportQueueItemResponse from(LimitRequest request, String customerName) {
        return new SupportQueueItemResponse(
                request.getId(),
                customerName,
                request.getCurrentLimit(),
                request.getRequestedLimit(),
                request.getRiskLevel() != null ? request.getRiskLevel().name() : null,
                request.getStatus().name(),
                request.getCreatedAt());
    }
```

- [ ] **Step 11: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/domain/user/UserRepository.java apps/backend-api/src/main/java/com/limitflow/backend/domain/account/AccountRepository.java apps/backend-api/src/main/java/com/limitflow/backend/application apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/limitrequest/LimitRequestResponse.java apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/audit/AuditLogResponse.java apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/support/SupportNoteResponse.java apps/backend-api/src/main/java/com/limitflow/backend/presentation/dto/support/SupportQueueItemResponse.java
git commit -m "Convert application services to Mono/Flux (all 8 services)"
```

Compilation is still not expected to succeed — Task 4 (security) and Task 5 (controllers) still call these with pre-reactive expectations.

---

### Task 4: Security layer

**Files:**
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/security/SecurityConfig.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/security/JwtAuthFilter.java`
- Modify: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: `TokenService.extractUserId(String)` (unchanged, pure/synchronous), `UserRepository.findById(UUID)` (now `Mono<User>`, from Task 2/3).
- Produces: the three `@PreAuthorize` class-level annotations on `LimitRequestController`, `SupportController`, `AuditController` (Task 5) keep working unchanged against `@EnableReactiveMethodSecurity`.

**Expected outcome: still will NOT compile** — the 7 controllers (Task 5) still use servlet-based request types in a few places and the old MVC security annotations elsewhere. Full green at Task 5.

- [ ] **Step 1: Convert `SecurityConfig`**

```java
package com.limitflow.backend.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .authorizeExchange(auth -> auth
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
```

(There's no reactive equivalent of `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` to carry over — WebFlux security has no `HttpSession`-based authentication in the first place, so there's nothing to disable.)

- [ ] **Step 2: Convert `JwtAuthFilter`**

```java
package com.limitflow.backend.infrastructure.security;

import com.limitflow.backend.application.auth.TokenService;
import com.limitflow.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Optional<UUID> userId = tokenService.extractUserId(header.substring(7));
            if (userId.isPresent()) {
                return userRepository.findById(userId.get())
                        .flatMap(user -> {
                            List<SimpleGrantedAuthority> authorities =
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(user, null, authorities);
                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
                        })
                        .switchIfEmpty(chain.filter(exchange));
            }
        }
        return chain.filter(exchange);
    }
}
```

(The original's `SecurityContextHolder.getContext().getAuthentication() == null` guard doesn't need translating: the reactive security context is freshly established per request via `contextWrite`, not a mutable thread-local, so there's no "already authenticated" state to guard against in the first place.)

- [ ] **Step 3: Convert `GlobalExceptionHandler`**

`jakarta.servlet.http.HttpServletRequest` doesn't exist once `spring-boot-starter-web` is gone — it becomes `ServerWebExchange`, and the bean-validation exception type changes from Spring MVC's `MethodArgumentNotValidException` to WebFlux's `WebExchangeBindException` (which exposes the same `.getBindingResult()`):

```java
package com.limitflow.backend.presentation.exception;

import com.limitflow.backend.domain.exception.ForbiddenException;
import com.limitflow.backend.domain.exception.InvalidCredentialsException;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage(), exchange);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", exchange);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleBeanValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respond(HttpStatus.BAD_REQUEST, message.isBlank() ? "Invalid request" : message, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception on {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", exchange);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message, ServerWebExchange exchange) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                exchange.getRequest().getPath().value());
        return ResponseEntity.status(status).body(body);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/infrastructure/security apps/backend-api/src/main/java/com/limitflow/backend/presentation/exception/GlobalExceptionHandler.java
git commit -m "Convert security layer and exception handler to WebFlux"
```

Compilation is still not expected to succeed — the 7 controllers (Task 5) haven't changed yet.

---

### Task 5: Controllers (all 7 domains) — module compiles again

**Files:**
- Delete: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/AuthController.java` (recreated below)
- Create: `apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller/AuthApi.java`
- Create+modify: `AuthController.java`, and the same Api+Controller pair for `Customer`, `Account`, `LimitRequest`, `Notification`, `Support`, `Audit` (14 files total; the 7 `XxxController.java` files already exist and are rewritten in place, the 7 `XxxApi.java` files are new)

**Interfaces:**
- Consumes: every service method from Task 3 (exact `Mono`/`Flux` signatures already established).
- Produces: nothing further downstream — this is the outermost layer.

**Expected outcome: the module compiles, and `mvnw compile` succeeds for the first time in this plan.** `mvnw test` will still show failures — the existing test files (`AuthControllerIntegrationTest`, `LimitRequestFlowIntegrationTest`, `LimitRequestServiceTest`, `OtpDeliveryServiceTest`) still call the pre-reactive APIs at the test level. That's Task 6.

Pattern, shown once in full for `Auth`, then applied identically for the remaining 6 (full code given for every file — this is mechanical, not "repeat the pattern"):

- [ ] **Step 1: `Auth`**

Create `presentation/controller/AuthApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.presentation.dto.auth.LoginRequest;
import com.limitflow.backend.presentation.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public interface AuthApi {

    @PostMapping("/login")
    Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request);
}
```

Replace `presentation/controller/AuthController.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.auth.AuthService;
import com.limitflow.backend.presentation.dto.auth.LoginRequest;
import com.limitflow.backend.presentation.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        return authService.login(request.email(), request.password()).map(LoginResponse::from);
    }
}
```

- [ ] **Step 2: `Customer`**

Create `presentation/controller/CustomerApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.UserSummary;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/api/customer")
@Tag(name = "Customer")
public interface CustomerApi {

    @GetMapping("/me")
    Mono<UserSummary> me(@AuthenticationPrincipal User user);
}
```

Replace `presentation/controller/CustomerController.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.UserSummary;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CustomerController implements CustomerApi {

    @Override
    public Mono<UserSummary> me(User user) {
        return Mono.just(UserSummary.from(user));
    }
}
```
(`@AuthenticationPrincipal` already resolves the `User` synchronously by the time the handler runs — there was never a repository call in this controller, so it just wraps the existing value; no new fetch introduced.)

- [ ] **Step 3: `Account`**

Create `presentation/controller/AccountApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.account.AccountResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
public interface AccountApi {

    @GetMapping
    Flux<AccountResponse> accounts(@AuthenticationPrincipal User user);
}
```

Replace `presentation/controller/AccountController.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.customer.CustomerService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.account.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final CustomerService customerService;

    @Override
    public Flux<AccountResponse> accounts(User user) {
        return customerService.accountsFor(user).map(AccountResponse::from);
    }
}
```

- [ ] **Step 4: `Notification`**

Create `presentation/controller/NotificationApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.notification.NotificationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public interface NotificationApi {

    @GetMapping
    Flux<NotificationResponse> notifications(@AuthenticationPrincipal User user);
}
```

Replace `presentation/controller/NotificationController.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    public Flux<NotificationResponse> notifications(User user) {
        return notificationService.findForUser(user.getId()).map(NotificationResponse::from);
    }
}
```

- [ ] **Step 5: `LimitRequest`**

Create `presentation/controller/LimitRequestApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.BiometricVerifyRequest;
import com.limitflow.backend.presentation.dto.limitrequest.CurrentLimitResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestSubmitRequest;
import com.limitflow.backend.presentation.dto.limitrequest.OtpVerifyRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequestMapping("/api/limits")
@Tag(name = "Transfer Limit Requests")
public interface LimitRequestApi {

    @GetMapping("/current")
    Mono<CurrentLimitResponse> current(@AuthenticationPrincipal User user);

    @PostMapping("/request")
    Mono<LimitRequestResponse> submit(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody LimitRequestSubmitRequest request);

    @PostMapping("/{id}/otp/verify")
    Mono<LimitRequestResponse> verifyOtp(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                         @Valid @RequestBody OtpVerifyRequest request);

    @PostMapping("/{id}/biometric/verify")
    Mono<LimitRequestResponse> verifyBiometric(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                                @Valid @RequestBody BiometricVerifyRequest request);

    @GetMapping("/history")
    Flux<LimitRequestResponse> history(@AuthenticationPrincipal User user);

    @GetMapping("/{id}")
    Mono<LimitRequestResponse> get(@AuthenticationPrincipal User user, @PathVariable UUID id);
}
```

Replace `presentation/controller/LimitRequestController.java` (the class-level `@PreAuthorize("hasRole('CUSTOMER')")` stays on the concrete controller, not the interface):
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.customer.CustomerService;
import com.limitflow.backend.application.limitrequest.LimitRequestService;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.BiometricVerifyRequest;
import com.limitflow.backend.presentation.dto.limitrequest.CurrentLimitResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestSubmitRequest;
import com.limitflow.backend.presentation.dto.limitrequest.OtpVerifyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class LimitRequestController implements LimitRequestApi {

    private final LimitRequestService limitRequestService;
    private final CustomerService customerService;

    @Override
    public Mono<CurrentLimitResponse> current(User user) {
        return customerService.primaryAccount(user)
                .flatMap(account -> limitRequestService.history(user, account.getId())
                        .filter(r -> RequestStatus.ACTIVE.contains(r.getStatus()))
                        .next()
                        .map(LimitRequestResponse::from)
                        .map(activeRequest -> CurrentLimitResponse.from(account, activeRequest))
                        .switchIfEmpty(Mono.fromSupplier(() -> CurrentLimitResponse.from(account, null))));
    }

    @Override
    public Mono<LimitRequestResponse> submit(User user, LimitRequestSubmitRequest request) {
        return limitRequestService.submitRequest(user, request.accountId(), request.requestedLimit(),
                        request.reason(), request.knownDevice())
                .map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> verifyOtp(User user, UUID id, OtpVerifyRequest request) {
        return limitRequestService.verifyOtp(user, id, request.code()).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> verifyBiometric(User user, UUID id, BiometricVerifyRequest request) {
        return limitRequestService.verifyBiometric(user, id, request.success()).map(LimitRequestResponse::from);
    }

    @Override
    public Flux<LimitRequestResponse> history(User user) {
        return customerService.primaryAccount(user)
                .flatMapMany(account -> limitRequestService.history(user, account.getId()))
                .map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> get(User user, UUID id) {
        return limitRequestService.get(user, id).map(LimitRequestResponse::from);
    }
}
```

- [ ] **Step 6: `Support`**

Create `presentation/controller/SupportApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.support.AddNoteRequest;
import com.limitflow.backend.presentation.dto.support.ReviewActionRequest;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequestMapping("/api/support/requests")
@Tag(name = "Support Review")
public interface SupportApi {

    @GetMapping
    Flux<SupportQueueItemResponse> queue(@AuthenticationPrincipal User user);

    @GetMapping("/{id}")
    Mono<LimitRequestResponse> get(@PathVariable UUID id);

    @PostMapping("/{id}/approve")
    Mono<LimitRequestResponse> approve(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                        @RequestBody(required = false) ReviewActionRequest request);

    @PostMapping("/{id}/reject")
    Mono<LimitRequestResponse> reject(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                       @RequestBody(required = false) ReviewActionRequest request);

    @PostMapping("/{id}/request-verification")
    Mono<LimitRequestResponse> requestVerification(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                                     @RequestBody(required = false) ReviewActionRequest request);

    @PostMapping("/{id}/notes")
    Mono<SupportNoteResponse> addNote(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                       @Valid @RequestBody AddNoteRequest request);

    @GetMapping("/{id}/notes")
    Flux<SupportNoteResponse> notes(@AuthenticationPrincipal User user, @PathVariable UUID id);
}
```

Replace `presentation/controller/SupportController.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.support.SupportReviewService;
import com.limitflow.backend.domain.user.User;
import com.limitflow.backend.presentation.dto.limitrequest.LimitRequestResponse;
import com.limitflow.backend.presentation.dto.support.AddNoteRequest;
import com.limitflow.backend.presentation.dto.support.ReviewActionRequest;
import com.limitflow.backend.presentation.dto.support.SupportNoteResponse;
import com.limitflow.backend.presentation.dto.support.SupportQueueItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER')")
public class SupportController implements SupportApi {

    private final SupportReviewService supportReviewService;

    @Override
    public Flux<SupportQueueItemResponse> queue(User user) {
        return supportReviewService.queueFor(user.getRole());
    }

    @Override
    public Mono<LimitRequestResponse> get(UUID id) {
        return supportReviewService.getForReview(id).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> approve(User user, UUID id, ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return supportReviewService.approve(user, id, note).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> reject(User user, UUID id, ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return supportReviewService.reject(user, id, note).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<LimitRequestResponse> requestVerification(User user, UUID id, ReviewActionRequest request) {
        String note = request != null ? request.note() : null;
        return supportReviewService.requestAdditionalVerification(user, id, note).map(LimitRequestResponse::from);
    }

    @Override
    public Mono<SupportNoteResponse> addNote(User user, UUID id, AddNoteRequest request) {
        return supportReviewService.addStaffNote(user, id, request.note());
    }

    @Override
    public Flux<SupportNoteResponse> notes(User user, UUID id) {
        return supportReviewService.notesFor(user, id);
    }
}
```

- [ ] **Step 7: `Audit`**

Create `presentation/controller/AuditApi.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@RequestMapping("/api/audit")
@Tag(name = "Audit")
public interface AuditApi {

    @GetMapping
    Flux<AuditLogResponse> auditLog();
}
```

Replace `presentation/controller/AuditController.java`:
```java
package com.limitflow.backend.presentation.controller;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.presentation.dto.audit.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER')")
public class AuditController implements AuditApi {

    private final AuditService auditService;

    @Override
    public Flux<AuditLogResponse> auditLog() {
        return auditService.findAll();
    }
}
```

- [ ] **Step 8: Compile and confirm the module builds for the first time**

Run: `./mvnw compile` (from `apps/backend-api`; set `JAVA_HOME` first if needed)
Expected: **BUILD SUCCESS.** If it fails, fix the reported errors in this task's files (and, if the root cause traces back to Tasks 1–4, fix it there) before moving on — this is the first point in the plan where a clean compile is the actual bar to clear.

- [ ] **Step 9: Commit**

```bash
git add apps/backend-api/src/main/java/com/limitflow/backend/presentation/controller
git commit -m "Convert controllers to Api-interface + thin-Controller pattern (all 7 domains)"
```

---

### Task 6: Convert tests to reactive equivalents — full suite green

**Files:**
- Modify: `apps/backend-api/src/test/java/com/limitflow/backend/integration/AuthControllerIntegrationTest.java`
- Modify: `apps/backend-api/src/test/java/com/limitflow/backend/integration/LimitRequestFlowIntegrationTest.java`
- Modify: `apps/backend-api/src/test/java/com/limitflow/backend/application/limitrequest/LimitRequestServiceTest.java`
- Modify: `apps/backend-api/src/test/java/com/limitflow/backend/application/otp/OtpDeliveryServiceTest.java`

**Interfaces:** None new — this task only updates test code to call the already-reactive production APIs from Tasks 1–5 correctly.

**Important Mockito trap:** an unstubbed mock method that returns a reference type (`Mono<T>`/`Flux<T>`) returns **`null`** by default, not an empty `Mono`/`Flux`. Chaining `.flatMap(...)` off a `null` reference throws `NullPointerException` immediately — this is a real behavior difference from the old blocking mocks, where an unstubbed primitive-returning method (e.g. `long count(...)`) safely defaulted to `0`. Every reactive-returning mock method a test path actually reaches must be explicitly stubbed.

- [ ] **Step 1: Convert `OtpDeliveryServiceTest`**

Only the `deliver(...)`-calling test changes — `deliver` now returns `Mono<Void>`, and building a `Mono` never executes it or throws; only *subscribing* does. The old `assertThatCode(...).doesNotThrowAnyException()` would now trivially pass without testing anything, since it never subscribes. Replace with `StepVerifier`:

```java
package com.limitflow.backend.application.otp;

import com.limitflow.backend.domain.user.User;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
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

        StepVerifier.create(service.deliver(customer, "123456")).verifyComplete();
    }

    private User newCustomer(String phone) {
        User user = mock(User.class);
        lenient().when(user.getPhone()).thenReturn(phone);
        return user;
    }
}
```

- [ ] **Step 2: Convert `LimitRequestServiceTest`**

```java
package com.limitflow.backend.application.limitrequest;

import com.limitflow.backend.application.audit.AuditService;
import com.limitflow.backend.application.limitrequest.risk.RiskEngine;
import com.limitflow.backend.application.notification.NotificationService;
import com.limitflow.backend.application.otp.OtpDeliveryService;
import com.limitflow.backend.application.otp.OtpService;
import com.limitflow.backend.domain.account.Account;
import com.limitflow.backend.domain.account.AccountRepository;
import com.limitflow.backend.domain.exception.ValidationException;
import com.limitflow.backend.domain.limitrequest.LimitRequest;
import com.limitflow.backend.domain.limitrequest.LimitRequestRepository;
import com.limitflow.backend.domain.limitrequest.RequestStatus;
import com.limitflow.backend.domain.limitrequest.RiskLevel;
import com.limitflow.backend.domain.user.Role;
import com.limitflow.backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitRequestServiceTest {

    @Mock
    private LimitRequestRepository limitRequestRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private OtpDeliveryService otpDeliveryService;
    @Mock
    private RiskEngine riskEngine;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;

    private LimitRequestService service;
    private User customer;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new LimitRequestService(limitRequestRepository, accountRepository, otpService,
                otpDeliveryService, riskEngine, notificationService, auditService);

        customer = newUser(UUID.randomUUID(), Role.CUSTOMER);
        account = new Account(customer.getId(), "0123456789", BigDecimal.valueOf(200_000), BigDecimal.valueOf(180_000));
        setId(account, UUID.randomUUID());

        lenient().when(limitRequestRepository.save(any(LimitRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        lenient().when(auditService.record(any(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        lenient().when(auditService.record(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.empty());
        lenient().when(notificationService.send(any(UUID.class), any(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        lenient().when(otpDeliveryService.deliver(any(), anyString())).thenReturn(Mono.empty());
    }

    @Test
    void submitRequestRejectsAnAmountNotGreaterThanTheCurrentLimit() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));

        StepVerifier.create(service.submitRequest(customer, account.getId(),
                        BigDecimal.valueOf(200_000), "reason", true))
                .verifyError(ValidationException.class);
    }

    @Test
    void submitRequestRejectsWhenAnActiveRequestAlreadyExists() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.submitRequest(customer, account.getId(),
                        BigDecimal.valueOf(300_000), "reason", true))
                .verifyError(ValidationException.class);

        verify(limitRequestRepository, never()).save(any());
    }

    @Test
    void submitRequestDeliversTheOtpThroughOtpDeliveryService() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(Mono.just(false));
        when(otpService.issue(any())).thenReturn(Mono.just("654321"));

        StepVerifier.create(service.submitRequest(customer, account.getId(),
                        BigDecimal.valueOf(250_000), "reason", true))
                .expectNextCount(1)
                .verifyComplete();

        verify(otpDeliveryService).deliver(customer, "654321");
    }

    @Test
    void submitRequestStartsTheOtpStepAndSendsACode() {
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(limitRequestRepository.existsByAccountIdAndStatusIn(eq(account.getId()), eq(RequestStatus.ACTIVE)))
                .thenReturn(Mono.just(false));
        when(otpService.issue(any(LimitRequest.class))).thenReturn(Mono.just("123456"));

        LimitRequest result = service.submitRequest(customer, account.getId(),
                BigDecimal.valueOf(300_000), "vacation", true).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.OTP_PENDING);
        verify(otpService).issue(any(LimitRequest.class));
        verify(notificationService).send(eq(customer.getId()), any(), any(), contains("123456"));
    }

    @Test
    void lowRiskRequestAutoApprovesAndRaisesTheAccountLimit() {
        LimitRequest limitRequest = new LimitRequest(account.getId(), BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(250_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Mono.just(limitRequest));
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(riskEngine.assess(any())).thenReturn(RiskLevel.LOW);
        when(limitRequestRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(Mono.just(0L));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        LimitRequest result = service.verifyBiometric(customer, limitRequest.getId(), true).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(account.getDailyLimit()).isEqualByComparingTo(BigDecimal.valueOf(250_000));
        verify(accountRepository).save(account);
    }

    @Test
    void mediumRiskRequestGoesToManualReviewInstead() {
        LimitRequest limitRequest = new LimitRequest(account.getId(), BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000), "reason", true);
        setId(limitRequest, UUID.randomUUID());
        limitRequest.transitionTo(RequestStatus.BIOMETRIC_PENDING);

        when(limitRequestRepository.findById(limitRequest.getId())).thenReturn(Mono.just(limitRequest));
        when(accountRepository.findById(account.getId())).thenReturn(Mono.just(account));
        when(riskEngine.assess(any())).thenReturn(RiskLevel.MEDIUM);
        when(limitRequestRepository.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(Mono.just(0L));

        LimitRequest result = service.verifyBiometric(customer, limitRequest.getId(), true).block();

        assertThat(result.getStatus()).isEqualTo(RequestStatus.UNDER_REVIEW);
        verify(accountRepository, never()).save(any());
    }

    private User newUser(UUID id, Role role) {
        User user = mock(User.class, withSettings().lenient());
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 3: Convert `AuthControllerIntegrationTest` to `WebTestClient`**

```java
package com.limitflow.backend.integration;

import com.limitflow.backend.EmbeddedR2dbcConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.reactive.server.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
@Import(EmbeddedR2dbcConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void seededCustomerCanLogIn() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("email", "customer@limitflow.demo", "password", "Password123!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.token").exists();
    }

    @Test
    void wrongPasswordIsRejected() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("email", "customer@limitflow.demo", "password", "wrong-password"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

- [ ] **Step 4: Convert `LimitRequestFlowIntegrationTest` to `WebTestClient`**

```java
package com.limitflow.backend.integration;

import com.limitflow.backend.EmbeddedR2dbcConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.reactive.server.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY, refresh = BEFORE_EACH_TEST_METHOD)
@Import(EmbeddedR2dbcConfig.class)
class LimitRequestFlowIntegrationTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("code is (\\d{6})");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void seededDashboardShowsTheActiveMediumRiskRequest() {
        String token = loginAs("customer@limitflow.demo");

        Map<String, Object> body = webTestClient.get().uri("/api/limits/current")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(body.get("dailyLimit")).isEqualTo(200000.0);
        assertThat(body.get("usedToday")).isEqualTo(180000.0);
        Map<String, Object> activeRequest = (Map<String, Object>) body.get("activeRequest");
        assertThat(activeRequest.get("status")).isEqualTo("UNDER_REVIEW");
        assertThat(activeRequest.get("riskLevel")).isEqualTo("MEDIUM");
    }

    @Test
    void lowRiskRequestWalksThroughOtpAndBiometricToAutomaticApproval() {
        resolveSeededRequest();

        String token = loginAs("customer@limitflow.demo");

        Map<String, Object> account = firstAccount(token);
        String accountId = (String) account.get("id");

        Map<String, Object> submitBody = Map.of(
                "accountId", accountId,
                "requestedLimit", 600000,
                "reason", "Paying a contractor",
                "knownDevice", true);
        Map<String, Object> submitResponse = webTestClient.post().uri("/api/limits/request")
                .header("Authorization", "Bearer " + token)
                .bodyValue(submitBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        String requestId = (String) submitResponse.get("id");
        assertThat(submitResponse.get("status")).isEqualTo("OTP_PENDING");

        String otpCode = latestOtpCode(token);

        Map<String, Object> otpResponse = webTestClient.post().uri("/api/limits/" + requestId + "/otp/verify")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("code", otpCode))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(otpResponse.get("status")).isEqualTo("BIOMETRIC_PENDING");

        Map<String, Object> biometricResponse = webTestClient.post().uri("/api/limits/" + requestId + "/biometric/verify")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("success", true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(biometricResponse.get("status")).isEqualTo("APPROVED");
        assertThat(biometricResponse.get("riskLevel")).isEqualTo("LOW");

        List<Map<String, Object>> accounts = webTestClient.get().uri("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(accounts.get(0).get("dailyLimit")).isEqualTo(600000.0);
    }

    private void resolveSeededRequest() {
        String supportToken = loginAs("support@limitflow.demo");
        webTestClient.post().uri("/api/support/requests/55555555-5555-5555-5555-555555555555/approve")
                .header("Authorization", "Bearer " + supportToken)
                .bodyValue(Map.of())
                .exchange();
    }

    private Map<String, Object> firstAccount(String token) {
        List<Map<String, Object>> accounts = webTestClient.get().uri("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();
        return accounts.get(0);
    }

    private String latestOtpCode(String token) {
        List<Map<String, Object>> notifications = webTestClient.get().uri("/api/notifications")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();
        String message = notifications.stream()
                .filter(n -> "OTP_SENT".equals(n.get("type")))
                .findFirst()
                .map(n -> (String) n.get("message"))
                .orElseThrow();
        Matcher matcher = CODE_PATTERN.matcher(message);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String loginAs(String email) {
        Map<String, Object> body = webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("email", email, "password", "Password123!"))
                .exchange()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        return (String) body.get("token");
    }
}
```

- [ ] **Step 5: Re-run `UserR2dbcRepositoryTest` from Task 2 and confirm it now passes for real**

Run: `./mvnw test -Dtest=UserR2dbcRepositoryTest` (from `apps/backend-api`)
Expected: PASS — 1/1. If it fails, that's the enum-mapping assumption from Global Constraints not holding; add a custom `Converter<Role, String>`/`Converter<String, Role>` pair registered via an `AbstractR2dbcConfiguration.r2dbcCustomConversions()` override before proceeding.

- [ ] **Step 6: Run the full suite**

Run: `./mvnw test` (from `apps/backend-api`)
Expected: **BUILD SUCCESS**, all tests passing. Fix any remaining failures in the file where they originate before moving on — this is the point where the whole rewrite must actually be green, not just compiling.

- [ ] **Step 7: Commit**

```bash
git add apps/backend-api/src/test
git commit -m "Convert tests to reactive equivalents (WebTestClient, Mono/Flux mocks, StepVerifier)"
```

---

### Task 7: Final verification

**Files:** None modified — this task only verifies.

- [ ] **Step 1: Full backend test suite, one more time from a clean state**

Run: `./mvnw clean test` (from `apps/backend-api`)
Expected: BUILD SUCCESS.

- [ ] **Step 2: Docker Compose smoke test**

```bash
cd docker
docker compose up --build
```
Expected: `backend-api` reaches healthy (its healthcheck hits `/actuator/health`). Log in via `customer-portal` (`http://localhost:3001`) as `customer@limitflow.demo` / `Password123!`, view the dashboard, submit a limit increase, verify OTP (check backend logs for the fallback-logged code, same as the Twilio feature's existing behavior), complete biometric verification. Confirm the request resolves exactly as it did before this rewrite — no visible behavior change for either portal.

- [ ] **Step 3: Confirm no dead JPA/MVC references remain**

Run: `grep -rl "jakarta.persistence\|JpaRepository\|HttpServletRequest\|MethodArgumentNotValidException" apps/backend-api/src` (from the repo root)
Expected: no matches. If any remain, they're leftover artifacts from the cutover and should be removed.

- [ ] **Step 4: Commit** (only if Step 3 found and fixed something; otherwise this task produces no diff)

```bash
git add -A
git commit -m "Final cleanup pass after reactive backend rewrite"
```
