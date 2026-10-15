# Implementation Plan: Kanban Todo Board

**Branch**: `001-kanban-todo-board` | **Date**: 2026-10-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-kanban-todo-board/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

A single-deployable Spring Boot monolith serving one shared kanban board of task cards across four
fixed columns, with full create/read/edit/move/delete, a three-state light/dark appearance, and a
REST surface over the same application core for later consumption.

The design is hexagonal: a framework-free domain and application core, with four adapters around
it — Thymeleaf HTML in, REST in, JPA/H2 out. Every user action is a plain form submission followed
by a redirect, because the constitution requires the board to work with client-side scripting
unavailable; that constraint, not a framework preference, drives most of the interaction design.
Validation runs twice on purpose: Bean Validation at the adapter boundary, and value-object
invariants inside the domain.

Phase 0 resolved both of the spec's open clarifications — one card entity with an optional note
field, and one shared board on a local-only deployment — and corrected a contradiction in the
appearance requirements. See [research.md](./research.md).

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.5.x — `web`, `thymeleaf`, `data-jpa`, `validation`,
`security`, `actuator` (health only); H2. Test scope: JUnit 5, AssertJ, Spring Boot Test/MockMvc,
ArchUnit, Jsoup. Exact patch versions are pinned from Spring Initializr when the build file is
first written (research R15).

**Storage**: H2, embedded. In-memory for the default and test profiles; a `durable` profile
switches the same adapter to file mode (research R3). Access through JPA behind an outbound port.

**Testing**: `mvn test` — unit tests on the domain with no Spring context, slice tests on adapters
(MockMvc + Jsoup for HTML, MockMvc for REST), integration tests over a real H2 instance, and
ArchUnit tests that fail the build on a dependency-direction violation (research R9).

**Target Platform**: JVM 21 on the developer's own machine, bound to localhost. Browser targets are
current evergreen versions, and every core flow also works with JavaScript disabled.

**Project Type**: Server-rendered web monolith — one Maven module, one artifact.

**Performance Goals**: a 100-card board ready to use in under 2 seconds on a mid-range phone over a
3G-class connection (SC-010); a move reflected within one second of confirmation (SC-002).

**Constraints**: no client-side scripting required for any core action (FR-030); usable at 320 CSS
px with no horizontal page scrolling (FR-027); 44×44 px touch targets (FR-028); 4.5:1 contrast in
both themes (FR-021); no authentication (FR-025); single shared board, localhost-bound (FR-026a).

**Scale/Scope**: one board, capped at 500 cards; one concurrent user expected, correctness under
two browser tabs required; 9 HTML routes, 7 REST operations, 5 user stories.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Phase 0

| Principle | Gate | Verdict |
|---|---|---|
| I. Security-First | Spec states who may act and what is exposed | **PASS after correction** — the spec originally left the exposure model open; FR-026a/FR-026b now state it, and research R2 records the accepted consequence |
| II. Hexagonal | Core free of framework types, I/O via ports, direction automated | **PASS** — layout and ArchUnit rules defined (research R4, R9) |
| III. Validate Every Input | Two layers, allowlist-based, rejection tested | **PASS after correction** — the spec specified only length and required-ness; research R5 defines the allowlist policy |
| IV. Mobile-First | Smallest-viewport-first, 320 px, 44 px, works without JS | **PASS** — research R7 and R13 define move controls and column stacking without scripting |
| V. Clean Code | Intention-revealing names, one responsibility, tests per use case | **PASS** — nothing in this design requires an exception |

### Architecture & Technology Constraints

| Constraint | Verdict |
|---|---|
| One modular monolith, single artifact | **PASS** — one Maven module |
| Java + Spring Boot, template engine, no SPA, no JS build pipeline | **PASS** — Thymeleaf, hand-written CSS, no bundler |
| Build fails when the core imports framework or adapter code | **PASS** — ArchUnit in the test phase |
| Persistence behind an outbound port; no SQL/ORM/transactions in the domain | **PASS** — `CardJpaEntity` confined to its package (research R4) |
| Externalized, profile-based config; no secrets in source | **PASS** — no secrets exist at this stage |
| Each new dependency justified in the plan | **PASS** — see Dependency Justification below |

### Post-Phase 1 re-check

| Principle | Verdict after design |
|---|---|
| I. Security-First | **PASS with one documented deviation** — CSRF exempt on `/api/**` (see Complexity Tracking). Otherwise strengthened beyond the spec: frame denial, CSP, allowlist validation, content-free security logging (R6, R12), card cap and write rate limit (R11) |
| II. Hexagonal | **PASS** — two inbound adapters share one core; `revision` is modelled as a domain concept because refusing a stale write is a business rule, with JPA's `@Version` mapping onto it rather than leaking in (data-model.md) |
| III. Validate Every Input | **PASS** — adapter DTOs carry Bean Validation; value objects enforce invariants with plain guard clauses, keeping `jakarta.validation` out of the domain |
| IV. Mobile-First | **PASS** — `<details>`-based stacking below 768 px, POST-redirect-GET moves, server-rendered theme with no flash |
| V. Clean Code | **PASS** — one use case per inbound port; the mapper is pure |
| Quality gates 1–7 | **PASS** — gate 2 is the ArchUnit suite, gate 3 is covered by rejection tests per input path, gate 4 by the security checklist, gate 5 by quickstart scenario 9 |

## Project Structure

### Documentation (this feature)

```text
specs/001-kanban-todo-board/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── spec.md              # Feature specification (/speckit-specify)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── rest-api.yaml    # OpenAPI 3.1 for the machine surface
│   └── html-routes.md   # The no-scripting user interface contract
├── checklists/
│   ├── requirements.md  # Spec quality (all items pass)
│   ├── ux.md            # Reviewer-owned UX requirements quality
│   └── security.md      # Reviewer-owned security requirements quality
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
pom.xml

src/main/java/com/todoproject/
├── TodoApplication.java
├── domain/
│   ├── model/
│   │   ├── TaskCard.java             # Aggregate root. No annotations
│   │   ├── CardId.java
│   │   ├── CardTitle.java            # Value object: normalization + allowlist
│   │   ├── CardNote.java
│   │   └── BoardStage.java           # Fixed four-value enum
│   └── error/
│       ├── InvalidCardContentException.java
│       ├── CardNotFoundException.java
│       ├── StaleCardException.java
│       └── BoardFullException.java
├── application/
│   ├── port/in/
│   │   ├── ViewBoardQuery.java
│   │   ├── CreateCardUseCase.java
│   │   ├── EditCardUseCase.java
│   │   ├── MoveCardUseCase.java
│   │   └── DeleteCardUseCase.java
│   ├── port/out/
│   │   ├── CardRepository.java
│   │   ├── ClockPort.java            # Injected time, so tests are deterministic
│   │   └── WriteRateLimiterPort.java
│   └── service/
│       ├── ViewBoardService.java
│       ├── CreateCardService.java
│       ├── EditCardService.java
│       ├── MoveCardService.java
│       └── DeleteCardService.java
└── adapter/
    ├── in/web/
    │   ├── BoardController.java
    │   ├── CardController.java
    │   ├── ThemeController.java
    │   ├── WebExceptionHandler.java
    │   ├── dto/                      # Form DTOs with Bean Validation
    │   └── view/                     # View models: labels, counts, preview clamping
    ├── in/rest/
    │   ├── BoardRestController.java
    │   ├── CardRestController.java
    │   ├── RestExceptionHandler.java # RFC 9457 problem details
    │   └── dto/
    └── out/
        ├── persistence/
        │   ├── CardJpaEntity.java    # Every JPA annotation lives here
        │   ├── CardJpaRepository.java
        │   ├── CardMapper.java
        │   └── CardPersistenceAdapter.java
        ├── time/SystemClockAdapter.java
        └── limit/InMemoryWriteRateLimiter.java

src/main/resources/
├── application.yml                   # default profile: H2 in memory, localhost
├── application-durable.yml           # file-mode H2 (research R3)
├── messages_pt_BR.properties         # Column labels, validation and error messages
├── schema.sql
├── templates/
│   ├── layout.html                   # data-theme, headers, appearance control
│   ├── board.html
│   ├── card-form.html
│   ├── delete-confirm.html
│   ├── fragments/card.html
│   └── error/{403,404,500}.html
└── static/css/app.css                # Mobile-first, token-based, both themes

src/main/java/com/todoproject/config/
├── SecurityConfig.java               # CSRF, headers, permitAll, no login
└── BoardProperties.java              # todo.board.max-cards, todo.limits.*

src/test/java/com/todoproject/
├── architecture/HexagonalArchitectureTest.java   # Constitution II, build gate
├── domain/                           # Value objects, invariants, transitions. No Spring
├── application/                      # Use cases against test doubles
└── adapter/
    ├── web/                          # MockMvc + Jsoup: escaping, labels, no-JS flows
    ├── rest/                         # MockMvc: contract conformance, problem+json
    └── persistence/                  # Mapper round-trip, optimistic locking
```

**Structure Decision**: one Maven module, packaged by role (`domain`, `application`, `adapter`)
rather than by technical type, as the constitution's Architecture section requires. The two inbound
adapters sit as siblings under `adapter/in` so neither can reach the other, and
`adapter/out/persistence` is the only package that compiles against JPA. `config` holds Spring
wiring only — no business rules — which keeps the core free of framework types while still letting
Spring construct it.

## Dependency Justification

The constitution requires each third-party dependency be justified here.

| Dependency | Why | Why not without it |
|---|---|---|
| `spring-boot-starter-security` | CSRF protection and security headers, with authentication left off entirely | Constitution I mandates CSRF on state-changing requests; hand-rolling token generation and header defaults reimplements a solved problem badly (research R6) |
| `thymeleaf-extras-springsecurity6` | Auto-injects the CSRF hidden input into every `th:action` form | Discovered necessary during implementation: `spring-boot-starter-thymeleaf` alone does not wire the CSRF token into rendered forms — without this, every HTML write would be refused by the CSRF filter it is supposed to satisfy |
| `spring-boot-starter-validation` | Bean Validation at the adapter boundary | Adapter-level syntactic validation would otherwise be hand-written per field, and principle III requires it to exist independently of the domain layer |
| `archunit-junit5` (test) | Makes the dependency direction a build failure | Principle II requires automated verification, not convention |
| `jsoup` (test) | Asserting rendered HTML — escaping, labels, touch-target markup | String matching on HTML is unreadable and brittle |
| `spring-boot-starter-actuator` | Health endpoint only, all other endpoints disabled | Minor; could be dropped if the surface is unwanted |

No runtime dependency is added for rate limiting or charting — both are in-house or absent.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| CSRF disabled on `/api/**`, against constitution I's "every state-changing request" | The REST surface is stateless, cookie-less, and carries no ambient credential, so there is no cross-site request for a token to defend against; requiring one would make the API painful to consume for no gain (research R6) | Keeping CSRF on for `/api/**` would force every consumer to fetch and replay a token against an endpoint that has no session to hijack. Dropping the REST surface entirely was also considered, but it was explicitly requested |
| In-memory H2 by default: the board is empty after an application restart, which is thinner than FR-026's intent even though FR-026 and SC-008 speak of browser restarts | The stack was specified explicitly, and a disposable database genuinely suits a local development stage — no migration state, no stale data between runs | File-mode H2 as the default would satisfy FR-026 more fully and is **one property away** (`durable` profile ships it). Left as a deliberate user decision rather than silently overridden |
| REST adapter is surface the spec does not require | Explicitly requested so the board can be consumed later. It adds no behaviour — only serialization over the same ports (research R8) | Omitting it would ignore the request. Building it as a separate service would violate the single-monolith constraint |
| `revision` appears on the domain aggregate, which looks like a persistence concern | Refusing a stale write is a business rule the use case applies (research R14); the domain must be able to express it | Keeping the version only in the JPA entity would push conflict detection into the persistence adapter, where the application layer could not reason about it, and would leak a persistence outcome upward as an infrastructure exception |

## Phase Status

- [x] Phase 0 — research complete, both `[NEEDS CLARIFICATION]` markers resolved, spec corrected
- [x] Phase 1 — data model, both contracts, and quickstart generated
- [x] Constitution Check re-evaluated after design
- [ ] Phase 2 — `tasks.md` (run `/speckit-tasks`; not produced by this command)
