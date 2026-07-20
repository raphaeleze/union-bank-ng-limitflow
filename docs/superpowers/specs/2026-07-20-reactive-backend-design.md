# Reactive backend rewrite (WebFlux + R2DBC)

## Context

The backend (`apps/backend-api`) is currently Spring MVC (`spring-boot-starter-web`) with blocking
JPA/Hibernate over Postgres. This rewrites it to Spring WebFlux with R2DBC, end to end — every
repository, service, and controller becomes non-blocking, returning `Mono`/`Flux` rather than
`Optional`/`List`/bare values.

The controller/API pattern is modeled on a sibling project, `recipeX-springboot-app`
(`GAZPACHO-TECH-SL/recipeX-springboot-app`), which already does this successfully: an `XxxApi`
interface carries the routing annotations (`@GetMapping` etc.), the OpenAPI `@Operation`/
`@ApiResponse` docs, and the `Mono`/`Flux` signatures; the concrete `XxxController implements
XxxApi` is pure delegation to a service, no logic in the controller at all. That project got
"reactive" close to free because it's backed by reactive MongoDB. This backend is Postgres via
JPA, so going truly reactive means a full swap to R2DBC — there is no reactive JPA. That's the
one asterisk on directly copying recipeX's pattern, and it's the reason this is the largest of
the three subprojects (Twilio OTP and the UI redesign, both already shipped/planned separately).

## Scope

One cohesive subsystem, not split into independent specs — every layer here is coupled through
the same request pipeline (a repository's return type change forces its service's signature to
change, which forces its controller's signature to change). Unlike Twilio/UI, there's no
meaningful way to ship a slice of this independently. It does get a long, ordered implementation
plan (this is an execution/task-granularity decision, not a spec-splitting one).

## What changes, layer by layer

**Dependencies (`pom.xml`)** — remove `spring-boot-starter-web`, `spring-boot-starter-data-jpa`.
Add `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `io.r2dbc:r2dbc-postgresql`.
Keep `org.postgresql:postgresql` (the JDBC driver) — Flyway has no reactive driver; migrations
still run once at blocking startup, which is normal even in fully reactive Spring Boot apps and
not a compromise worth avoiding. `springdoc-openapi-starter-webmvc-ui` →
`springdoc-openapi-starter-webflux-ui`. Test-side: `spring-security-test` stays; `reactor-test`
(`StepVerifier`) is added.

**Persistence — entities.** Every `@Entity`/`@Table`/Hibernate-managed class becomes a plain
Spring Data R2DBC class (`@Table`, `@Id`, `@Column` from `org.springframework.data.*`, not
`jakarta.persistence.*`). The single behavior-changing consequence: **R2DBC has no relationship
mapping at all** — no `@ManyToOne`, no lazy proxies, no cascades. Every current object
association becomes a raw FK column, and every current object-graph read becomes an explicit
second query, composed with `flatMap`/`zip`. This is not cosmetic — it changes control flow in
every service that touches these fields:

| Entity | Current JPA relation | Becomes |
|---|---|---|
| `Account` | `user` (`@ManyToOne`, eager) | `userId` (`UUID`) + explicit fetch |
| `LimitRequest` | `account` (`@ManyToOne`, eager) | `accountId` (`UUID`) + explicit fetch |
| `LimitRequest` | `resolvedBy` (`@ManyToOne`, lazy, nullable) | `resolvedByUserId` (`UUID`, nullable) + explicit fetch |
| `Notification` | `user` (`@ManyToOne`, lazy) | `userId` (`UUID`) + explicit fetch |
| `AuditLog` | `actor` (`@ManyToOne`, eager) | `actorUserId` (`UUID`) + explicit fetch |
| `SupportNote` | `limitRequest` (`@ManyToOne`, lazy) | `limitRequestId` (`UUID`) + explicit fetch |
| `SupportNote` | `author` (`@ManyToOne`, eager) | `authorUserId` (`UUID`) + explicit fetch |
| `OtpCode` | `limitRequest` (`@ManyToOne`, lazy) | `limitRequestId` (`UUID`) + explicit fetch |

Current eager/lazy distinctions become irrelevant — R2DBC has no proxies, so every relation is
"explicit" regardless of what its JPA fetch type used to be.

**Persistence — repositories.** Each `XxxJpaRepository extends JpaRepository<...>` becomes an
`XxxR2dbcRepository extends R2dbcRepository<...>` (or `ReactiveCrudRepository`), and the domain
port interfaces (`AccountRepository`, `LimitRequestRepository`, etc.) change their method
signatures from `Optional<T>`/`List<T>`/`T` to `Mono<T>`/`Flux<T>`. Derived query methods
(`findByAccountIdOrderByCreatedAtDesc`, `existsByAccountIdAndStatusIn`, etc.) carry over —
Spring Data R2DBC supports the same derived-query-method conventions as JPA for these shapes.

**Services.** Every `application/*Service` method returns `Mono`/`Flux` and composes reactively.
`@Transactional` (used today on `LimitRequestService.verifyBiometric`) becomes reactive
transactionality via Spring's `ReactiveTransactionManager` (auto-configured for R2DBC) — the
annotation usage is unchanged, but it now wraps a reactive chain instead of a thread-bound
transaction.

**Controllers.** All 7 controllers (`Auth`, `Customer`, `Account`, `LimitRequest`,
`Notification`, `Support`, `Audit`) split into an `XxxApi` interface (routing + OpenAPI docs +
`Mono`/`Flux` signatures) and an `XxxController implements XxxApi` (pure delegation), per
recipeX's pattern.

**Security.** `SecurityConfig`: `@EnableWebSecurity`/`HttpSecurity`/`SecurityFilterChain` →
`@EnableWebFluxSecurity`/`ServerHttpSecurity`/`SecurityWebFilterChain`. `@EnableMethodSecurity` →
`@EnableReactiveMethodSecurity` — the three existing class-level `@PreAuthorize` annotations
(`LimitRequestController` → `CUSTOMER`, `SupportController`/`AuditController` →
`SUPPORT_AGENT`/`MANAGER`) carry over unchanged; reactive method security supports `Mono`/`Flux`-
returning methods natively. `JwtAuthFilter` (currently `OncePerRequestFilter`) becomes a
`WebFilter`: resolves the user via a reactive `userRepository.findById(...)` and publishes the
`Authentication` via `ReactiveSecurityContextHolder.withAuthentication(...)` through
`.contextWrite(...)`, replacing the thread-local `SecurityContextHolder` write. `BCryptPasswordEncoder`
stays as-is — it's pure CPU, no I/O, and not worth wrapping in `Schedulers.boundedElastic()` for
this demo's request volume. *(Deliberate ceiling, not an oversight: revisit only if profiling
ever shows bcrypt hashing measurably contending with the Netty event loop.)*

**Testing.** `AuthControllerIntegrationTest` and `LimitRequestFlowIntegrationTest` move from
`TestRestTemplate` to `WebTestClient`, matching recipeX's own integration-test convention
(`DefaultSpringBootTest` + `webTestClient`). Unit tests (`LimitRequestServiceTest`,
`OtpDeliveryServiceTest`, `AmountThresholdRiskRule` tests, etc.) change mocked return values from
bare values to `Mono.just(...)`/`Flux.just(...)`, and assertions move from direct value checks to
`StepVerifier`.

## Out of scope

- No change to the Postgres schema itself (column names, types, Flyway migrations) — this is a
  data-access-layer rewrite, not a schema change. `V1`–`V3` stay as they are.
- No change to `customer-portal`/`employee-portal` (Next.js) — the REST contract (paths, request/
  response JSON shapes) stays identical; this is purely a backend implementation swap.
- No change to `docker-compose.yml` beyond what the new dependencies require (no new env vars).
- Not attempting connection-pool tuning or reactive-specific performance work beyond correctness
  — this is a demo project, not a system under real load.

## Risks / things the implementation plan must get right

- **`open-in-view=false`'s R2DBC equivalent**: R2DBC has no "view" concept to disable in the
  first place (no lazy proxies to leak past a request boundary), so this JPA-specific config key
  simply disappears — not a risk, just noting it has no replacement to add.
- **N+1-shaped code**: replacing `account.getUser()` with an explicit fetch means it's easy to
  accidentally write a `Flux` that fetches a related user once per item in a loop. The plan
  should call out batching (`Flux.collectList()` + a single `findAllById(...)`) wherever a list
  of related IDs is fetched, rather than fetching one at a time.
- **Reactive transaction boundaries**: `verifyBiometric`'s current `@Transactional` covers a
  read-modify-write across `LimitRequest` and `Account`. The reactive equivalent must be verified
  to actually wrap both writes in one reactive transaction, not just compile.
