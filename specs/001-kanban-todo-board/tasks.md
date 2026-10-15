---

description: "Task list for Kanban Todo Board implementation"
---

# Tasks: Kanban Todo Board

**Input**: Design documents from `/specs/001-kanban-todo-board/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

**Tests**: Test tasks are **included and mandatory**. This is not the template's optional case — the
constitution requires them: principle V ("every use case MUST have an automated test"), principle
III ("every new or changed input path MUST ship with at least one test proving invalid input is
rejected"), and principle II (dependency direction "verified by an automated architecture test").
Merge gates 1–3 in plan.md depend on them.

**Organization**: Tasks are grouped by user story so each story can be implemented, tested, and
demonstrated on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single Maven module, packaged by role per plan.md. Java sources under `src/main/java/com/todoproject/`,
tests under `src/test/java/com/todoproject/`, templates and CSS under `src/main/resources/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and configuration

- [X] T001 Create Maven project in `pom.xml` with Java 21 and Spring Boot 3.5.x: starters `web`, `thymeleaf`, `data-jpa`, `validation`, `security`, `actuator`; runtime `com.h2database:h2`; test scope `spring-boot-starter-test`, `archunit-junit5`, `org.jsoup:jsoup`. Pin exact versions from Spring Initializr per research R15
- [X] T002 Create the package skeleton and `src/main/java/com/todoproject/TodoApplication.java` with packages `domain.model`, `domain.error`, `application.port.in`, `application.port.out`, `application.service`, `adapter.in.web`, `adapter.in.rest`, `adapter.out.persistence`, `adapter.out.time`, `adapter.out.limit`, `config`
- [X] T003 [P] Configure `src/main/resources/application.yml`: datasource `jdbc:h2:mem:todo`, `server.address: 127.0.0.1` (FR-026a localhost binding), JPA `ddl-auto: none`, actuator exposing health only, and properties `todo.board.max-cards: 500` and `todo.limits.writes-per-minute: 60`
- [X] T004 [P] Create `src/main/resources/application-durable.yml` overriding the datasource to `jdbc:h2:file:./data/todo;AUTO_SERVER=TRUE` per research R3, changing nothing else
- [X] T005 [P] Create `src/main/resources/messages_pt_BR.properties` with column labels keyed by enum name (`stage.BACKLOG=Backlog`, `stage.IN_PROGRESS=Em andamento`, `stage.PAUSED=Pausado`, `stage.DONE=Concluído`) and the message strings listed in `contracts/html-routes.md`
- [X] T006 [P] Add Spotless (or equivalent formatter) and `-Werror`-style compiler warning configuration to `pom.xml` so merge gate 6 ("formatter and linter clean; no new warnings") is enforceable by `mvn verify`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The framework-free core, the persistence adapter behind its port, and the shared
security and layout scaffolding. Nothing in this phase is story-specific; everything in Phase 3+
depends on it.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Write `src/test/java/com/todoproject/architecture/HexagonalArchitectureTest.java` with the five ArchUnit rules from research R9: (1) no class in `domain..` or `application..` depends on `org.springframework..`, `jakarta.persistence..`, `jakarta.validation..`, `jakarta.servlet..`, or `adapter..`; (2) `adapter.out.persistence..` is not accessed from `adapter.in..`; (3) `domain..` does not depend on `application..`; (4) layer order `domain` ← `application` ← `adapter` with no cycles; (5) `CardJpaEntity` is accessed only from its own package. This test must run in the normal test phase so a violation fails the build
- [X] T008 [P] Create `src/main/java/com/todoproject/domain/model/BoardStage.java` as an enum with exactly four constants in declaration order `BACKLOG, IN_PROGRESS, PAUSED, DONE` (FR-001, FR-002). Declaration order is the display order; no label strings in this file
- [X] T009 [P] Create `src/main/java/com/todoproject/domain/model/CardId.java` wrapping a `UUID`, with a `newId()` factory using random UUID v4 and a `parse(String)` that signals "not found" rather than throwing a raw parse error
- [X] T010 [P] Create `src/main/java/com/todoproject/domain/model/TextPolicy.java` implementing the allowlist from research R5: normalize to Unicode NFC, permit only categories L*, M*, N*, P*, S*, Zs (plus `\n` when the caller allows it), and reject Cc, Cf, Cs, Co, Cn. Plain Java only — no `jakarta.validation`
- [X] T011 Create `src/main/java/com/todoproject/domain/model/CardTitle.java` (depends on T010): required, NFC-normalized, trimmed, internal whitespace runs collapsed to one space, minimum 1 character after normalization, **maximum 120 characters** measured after normalization, rejecting the forbidden categories (FR-007, FR-008)
- [X] T012 Create `src/main/java/com/todoproject/domain/model/CardNote.java` (depends on T010): optional and modelled as `Optional.empty()` when absent, **maximum 2000 characters**, `\r\n` and `\r` normalized to `\n` with line structure preserved, whitespace-only stored as absent rather than blank
- [X] T013 [P] Create `src/main/java/com/todoproject/domain/error/` with `InvalidCardContentException` (carrying field name and violation code, never the offending content per research R12), `CardNotFoundException`, `StaleCardException`, and `BoardFullException`
- [X] T014 Create `src/main/java/com/todoproject/domain/model/TaskCard.java` (depends on T008–T013) as the aggregate root with fields `id`, `title`, `note`, `stage`, `createdAt`, `updatedAt`, `revision` and behaviour `moveTo(BoardStage, Instant)`, `rename(CardTitle, Instant)`, `changeNote(CardNote, Instant)`. Enforce the invariants in data-model.md: nothing null, `updatedAt` never before `createdAt`, `revision` never negative, `createdAt` and `id` unmodifiable, every mutator advancing `updatedAt` and incrementing `revision`. No mutator may change `stage` except `moveTo` (FR-015). Zero annotations in this file
- [X] T015 [P] Write `src/test/java/com/todoproject/domain/CardTitleTest.java` and `CardNoteTest.java` covering: empty rejected, whitespace-only rejected, 120 accepted and 121 rejected, 2000 accepted and 2001 rejected, accented Portuguese letters accepted, zero-width space rejected, bidirectional override rejected, control character rejected, `\r\n` normalized in notes, whitespace-only note stored as absent
- [X] T016 [P] Write `src/test/java/com/todoproject/domain/TaskCardTest.java` covering: all twelve ordered stage pairs legal plus self-move, `createdAt` and content preserved across a move (FR-010), `revision` incremented and `updatedAt` advanced by every mutator including a self-move, and no path that mutates `stage` during a rename or note change
- [X] T017 [P] Create `src/main/java/com/todoproject/application/port/out/CardRepository.java` declaring load-by-id, load-all-ordered-by-updated-at-descending, count, save, and delete, expressed in domain types only
- [X] T018 [P] Create `src/main/java/com/todoproject/application/port/out/ClockPort.java` returning the current `Instant`, so use case tests are deterministic
- [X] T019 [P] Create `src/main/java/com/todoproject/application/port/out/WriteRateLimiterPort.java` declaring a check keyed by client address
- [X] T020 [P] Create `src/main/java/com/todoproject/config/BoardProperties.java` binding `todo.board.max-cards` (default 500) and `todo.limits.writes-per-minute` (default 60) per research R11
- [X] T021 Create `src/main/java/com/todoproject/adapter/out/persistence/CardJpaEntity.java` with the column mapping from data-model.md: `id` UUID primary key, `title VARCHAR(120) NOT NULL`, `note VARCHAR(2000)` nullable, `stage VARCHAR(20) NOT NULL` using `@Enumerated(EnumType.STRING)` (**`EnumType.ORDINAL` is forbidden**), `created_at` and `updated_at` as `TIMESTAMP(6) WITH TIME ZONE NOT NULL`, and `revision BIGINT NOT NULL` annotated `@Version`. Package-private where the language allows
- [X] T022 Create `src/main/resources/schema.sql` with the `card` table matching T021 and the index `idx_card_stage_updated_at` on `(stage, updated_at DESC)`
- [X] T023 Create `src/main/java/com/todoproject/adapter/out/persistence/CardJpaRepository.java` as the Spring Data interface over `CardJpaEntity`
- [X] T024 Create `src/main/java/com/todoproject/adapter/out/persistence/CardMapper.java` converting both directions, pure with no repository access or clock. Reconstitution uses a factory that trusts already-validated stored data rather than re-running normalization, and surfaces an invariant failure loudly as corruption (data-model.md)
- [X] T025 [P] Write `src/test/java/com/todoproject/adapter/persistence/CardMapperTest.java` asserting a round trip preserves every field including `revision`, that an absent note maps to SQL NULL and back to `Optional.empty()`, and that stage is stored as the enum name
- [X] T026 Create `src/main/java/com/todoproject/adapter/out/persistence/CardPersistenceAdapter.java` implementing `CardRepository` via `CardJpaRepository` and `CardMapper`, translating a JPA optimistic-lock failure into `StaleCardException`
- [X] T027 [P] Write `src/test/java/com/todoproject/adapter/persistence/CardPersistenceAdapterTest.java` as a `@DataJpaTest` covering save/load/delete round trips, ordering by `updated_at` descending, and a concurrent-update attempt raising `StaleCardException`
- [X] T028 [P] Create `src/main/java/com/todoproject/adapter/out/time/SystemClockAdapter.java` implementing `ClockPort`
- [X] T029 Create `src/main/java/com/todoproject/adapter/out/limit/InMemoryWriteRateLimiter.java` implementing `WriteRateLimiterPort` as a fixed-window counter per client address (research R11), plus `src/test/java/com/todoproject/adapter/limit/InMemoryWriteRateLimiterTest.java` covering the boundary and window reset
- [X] T030 Create `src/main/java/com/todoproject/config/SecurityConfig.java` per research R6: `permitAll()` on every route with no login page and no user store; CSRF **enabled** for HTML routes and **disabled for `/api/**`** only; `X-Frame-Options: DENY`; Content-Security-Policy `default-src 'self'` with no inline script
- [X] T031 [P] Write `src/test/java/com/todoproject/config/SecurityConfigTest.java` asserting a form POST without a CSRF token is refused with 403, the same POST with a token succeeds, `/api/**` writes succeed without a token, and both headers are present on a board response
- [X] T032 Create `src/main/resources/templates/layout.html` and `src/main/resources/static/css/app.css`: the shared page shell plus mobile-first CSS authored smallest-viewport-first with `min-width` queries only, design tokens on `:root`, the light palette, 44×44 px minimum control sizing, and the four-column grid above 768 px with `<details>` stacking below it (research R13). Dark palette and the appearance control arrive in US5
- [X] T033 Create `src/main/java/com/todoproject/adapter/in/web/WebExceptionHandler.java` plus `src/main/resources/templates/error/403.html`, `404.html` and `500.html`, mapping each outcome in data-model.md to its status and message with no stack trace, SQL, or identifier exposed (FR-024)
- [X] T034 Create `src/main/java/com/todoproject/adapter/in/rest/RestExceptionHandler.java` and the `Problem` representation from `contracts/rest-api.yaml`, emitting `application/problem+json` with `type`, `title`, `status`, `detail` and a field-level `errors` array, and the status mapping 400/404/409/412/428/429
- [X] T035 [P] Create `src/test/java/com/todoproject/TaskCardFixtures.java` providing builders for cards in each stage and a fixed `ClockPort` test double

**Checkpoint**: Core, persistence, security and layout ready — user story implementation can begin

---

## Phase 3: User Story 1 - Capture a task on the board (Priority: P1) 🎯 MVP

**Goal**: A person opens the board, adds a card with a title and optional note, and finds it still
there after a reload. Delivers a working personal task list on its own.

**Independent Test**: Open the application, add three cards (one with a note), reload, and confirm
all three are in Backlog with content intact — quickstart scenarios 1–3.

### Tests for User Story 1

- [X] T036 [P] [US1] Write `src/test/java/com/todoproject/application/CreateCardServiceTest.java` covering a valid create landing in the requested stage, default `BACKLOG` when no stage is given (FR-006), rejection of an invalid title with nothing persisted (FR-022), refusal at the 500-card cap, and refusal when the rate limiter denies the write
- [X] T037 [P] [US1] Write `src/test/java/com/todoproject/application/ViewBoardServiceTest.java` asserting all four columns are always returned in declaration order including empty ones, each with its count, cards ordered most-recently-changed first
- [X] T038 [P] [US1] Write `src/test/java/com/todoproject/adapter/web/BoardControllerTest.java` using MockMvc and Jsoup: `GET /` returns 200 with four columns in order, a count per column, and an empty-state message in each empty column (FR-001, FR-003, FR-004)
- [X] T039 [P] [US1] Write `src/test/java/com/todoproject/adapter/web/CreateCardFlowTest.java`: `POST /cards` with a valid title answers 303 to `/#card-{id}`; a blank title answers 422, re-renders the form with a message beside the title field, and preserves the typed note (FR-007); a whitespace-only title is rejected; a 121-character title is rejected with the limit stated and nothing truncated (FR-008); a title containing a zero-width space is rejected
- [X] T040 [P] [US1] Write `src/test/java/com/todoproject/adapter/web/EscapingTest.java` asserting a card titled `<script>alert(1)</script>` and a note containing `<img src=x onerror=alert(1)>` render as literal text, with Jsoup confirming no element was created from the input (FR-023, SC-009)
- [X] T041 [P] [US1] Write `src/test/java/com/todoproject/adapter/rest/CardCreateReadRestTest.java` against `contracts/rest-api.yaml`: `POST /api/cards` returns 201 with `Location` and `ETag`; a blank title returns 400 `application/problem+json` with a `title`/`REQUIRED` entry in `errors`; the cap returns 409; `GET /api/board` always returns four columns with `totalCards` and `maxCards`; `GET /api/cards?stage=` filters; `GET /api/cards/{id}` returns 200 with `ETag` and 404 for an unknown id

### Implementation for User Story 1

- [X] T042 [P] [US1] Create `src/main/java/com/todoproject/application/port/in/ViewBoardQuery.java` returning a board view of four columns with counts
- [X] T043 [P] [US1] Create `src/main/java/com/todoproject/application/port/in/CreateCardUseCase.java` with a command carrying raw title, optional note, optional stage, and client address
- [X] T044 [US1] Implement `src/main/java/com/todoproject/application/service/ViewBoardService.java` (depends on T042, T017) always emitting all four stages in declaration order, including empty ones
- [X] T045 [US1] Implement `src/main/java/com/todoproject/application/service/CreateCardService.java` (depends on T043, T014, T017–T020): check the rate limit, check the 500-card cap, build the `TaskCard` through its value objects, stamp times from `ClockPort`, save, and log a content-free rejection event on each refusal path (research R11, R12)
- [X] T046 [P] [US1] Create `src/main/java/com/todoproject/adapter/in/web/dto/CreateCardForm.java` with Bean Validation for `title` (required, max 120) and `note` (max 2000) per `contracts/html-routes.md`
- [X] T047 [P] [US1] Create `src/main/java/com/todoproject/adapter/in/web/view/` board view models resolving stage labels from `messages_pt_BR.properties` and clamping the note preview to two lines with a more-text indicator (FR-005a)
- [X] T048 [US1] Implement `src/main/java/com/todoproject/adapter/in/web/BoardController.java` serving `GET /`
- [X] T049 [US1] Implement `GET /cards/new` and `POST /cards` in `src/main/java/com/todoproject/adapter/in/web/CardController.java`, answering 303 on success and 422 with the form re-rendered on rejection
- [X] T050 [P] [US1] Create `src/main/resources/templates/board.html` and `src/main/resources/templates/fragments/card.html`: four columns with name, count and empty-state message; card face showing full title plus the clamped note preview; escaping text operator only, `th:utext` forbidden; a non-field notice region at the top of the board (ux CHK004)
- [X] T051 [P] [US1] Create `src/main/resources/templates/card-form.html` with a labelled title input, a note textarea, the CSRF token, and field-level error slots
- [X] T052 [US1] Implement `src/main/java/com/todoproject/adapter/in/rest/BoardRestController.java` (`GET /api/board`, `GET /api/cards`) and the create/read half of `src/main/java/com/todoproject/adapter/in/rest/CardRestController.java` (`POST /api/cards`, `GET /api/cards/{cardId}`) with `ETag` emission per the contract

**Checkpoint**: US1 is fully functional — a usable persistent task list on both surfaces

---

## Phase 4: User Story 2 - Move a task across the kanban columns (Priority: P2)

**Goal**: Cards move between Backlog, Em andamento, Pausado and Concluído in any direction, with
no scripting required.

**Independent Test**: With cards in Backlog, move one through all four columns and back, reloading
after each move — quickstart scenario 4.

### Tests for User Story 2

- [X] T053 [P] [US2] Write `src/test/java/com/todoproject/application/MoveCardServiceTest.java` covering every one of the twelve ordered stage pairs plus a self-move, title/note/`createdAt` preserved with only `stage` and `updatedAt` changing (FR-010), a stale revision refused with nothing written, and a move to a missing card refused without recreating it (FR-012)
- [ ] T054 [P] [US2] Write `src/test/java/com/todoproject/adapter/web/MoveCardFlowTest.java`: `POST /cards/{id}/move` answers 303 and the new column persists across a reload; both affected counts update; a stale `revision` answers 409 with the "alterada em outra aba" notice and leaves the card in its current column; an unknown stage value answers 422
- [ ] T055 [P] [US2] Write `src/test/java/com/todoproject/adapter/rest/MoveCardRestTest.java`: `PUT /api/cards/{id}/stage` with a matching `If-Match` returns 200 and a new `ETag`; a mismatched `If-Match` returns 412; an absent `If-Match` returns 428; an invalid stage returns 400; an unknown card returns 404

### Implementation for User Story 2

- [X] T056 [P] [US2] Create `src/main/java/com/todoproject/application/port/in/MoveCardUseCase.java` with a command carrying card id, target stage, expected revision, and client address
- [X] T057 [US2] Implement `src/main/java/com/todoproject/application/service/MoveCardService.java` (depends on T056): rate-limit check, load, compare the expected revision and refuse a stale write (research R14), delegate to `TaskCard.moveTo`, save, and log a content-free event
- [X] T058 [US2] Implement `POST /cards/{id}/move` in `src/main/java/com/todoproject/adapter/in/web/CardController.java`
- [X] T059 [US2] Add the move form to `src/main/resources/templates/fragments/card.html`: one form per card containing three submit buttons (one per stage other than the card's own), each at least 44×44 px, with hidden `revision` and the CSRF token (research R7, FR-011, FR-028)
- [X] T060 [US2] Implement `PUT /api/cards/{cardId}/stage` in `src/main/java/com/todoproject/adapter/in/rest/CardRestController.java` with `If-Match` handling per the contract
- [X] T061 [US2] Render the stale-write and not-found notices in the board's notice region in `src/main/resources/templates/board.html`, using the wording in `contracts/html-routes.md`
- [X] T062 [P] [US2] Extend `src/main/resources/static/css/app.css` so the move controls fit a 320 px viewport with no horizontal page scrolling and remain reachable in the stacked layout (FR-027, quickstart scenario 9)

**Checkpoint**: US1 and US2 both work independently — the board is a functioning kanban

---

## Phase 5: User Story 3 - Edit an existing task (Priority: P3)

**Goal**: A card's title and note can be corrected, with cancel leaving it untouched and the
column never changing.

**Independent Test**: Edit a title and confirm it after reload; start another edit and cancel it —
quickstart scenario 5.

### Tests for User Story 3

- [X] T063 [P] [US3] Write `src/test/java/com/todoproject/application/EditCardServiceTest.java` covering a title change, a note change, clearing a note, `stage` unchanged by every path (FR-015), invalid input rejected with the card untouched, and a stale revision refused
- [ ] T064 [P] [US3] Write `src/test/java/com/todoproject/adapter/web/EditCardFlowTest.java`: `GET /cards/{id}/edit` prefills both fields and shows the full note (FR-005a); `POST /cards/{id}` answers 303 and keeps the card in its column; a cleared title answers 422 with the card unmodified; the cancel link returns to the board without writing (FR-014)
- [ ] T065 [P] [US3] Write `src/test/java/com/todoproject/adapter/rest/EditCardRestTest.java`: `PATCH /api/cards/{id}` with only `title` leaves the note alone; `note: null` clears it; an empty body returns 400 (`minProperties: 1`); a mismatched `If-Match` returns 412

### Implementation for User Story 3

- [X] T066 [P] [US3] Create `src/main/java/com/todoproject/application/port/in/EditCardUseCase.java` with a command carrying card id, optional new title, optional new note, expected revision, and client address
- [X] T067 [US3] Implement `src/main/java/com/todoproject/application/service/EditCardService.java` (depends on T066) applying `rename` and `changeNote` only, never touching `stage`
- [X] T068 [US3] Implement `GET /cards/{id}/edit` and `POST /cards/{id}` in `src/main/java/com/todoproject/adapter/in/web/CardController.java`, plus `src/main/java/com/todoproject/adapter/in/web/dto/EditCardForm.java` with the same Bean Validation rules and a required `revision`
- [X] T069 [US3] Extend `src/main/resources/templates/card-form.html` for edit mode: prefilled fields, the full note, a hidden `revision`, and a cancel link back to the board
- [X] T070 [US3] Implement `PATCH /api/cards/{cardId}` in `src/main/java/com/todoproject/adapter/in/rest/CardRestController.java`

**Checkpoint**: US1–US3 independently functional

---

## Phase 6: User Story 4 - Remove a task (Priority: P4)

**Goal**: A card can be deleted permanently, but never by accident.

**Independent Test**: Delete a card and confirm; then start a deletion and decline it —
quickstart scenario 6.

### Tests for User Story 4

- [X] T071 [P] [US4] Write `src/test/java/com/todoproject/application/DeleteCardServiceTest.java` covering a successful hard delete with no tombstone remaining, a stale revision refused, and a delete of an already-missing card reporting not-found without recreating it (FR-012, FR-017)
- [ ] T072 [P] [US4] Write `src/test/java/com/todoproject/adapter/web/DeleteCardFlowTest.java`: `GET /cards/{id}/delete` renders a confirmation naming the card (ux CHK003); declining leaves the card present; `POST /cards/{id}/delete` answers 303 and the card is absent after a reload; deleting the last card in a column leaves that column visible with its empty-state message
- [ ] T073 [P] [US4] Write `src/test/java/com/todoproject/adapter/rest/DeleteCardRestTest.java`: `DELETE /api/cards/{id}` with a matching `If-Match` returns 204; a repeat returns 404; a mismatched `If-Match` returns 412; an absent `If-Match` returns 428

### Implementation for User Story 4

- [X] T074 [P] [US4] Create `src/main/java/com/todoproject/application/port/in/DeleteCardUseCase.java` with a command carrying card id, expected revision, and client address
- [X] T075 [US4] Implement `src/main/java/com/todoproject/application/service/DeleteCardService.java` (depends on T074) performing a permanent delete with no soft-delete or archive, and logging a content-free event
- [X] T076 [US4] Implement `GET /cards/{id}/delete` and `POST /cards/{id}/delete` in `src/main/java/com/todoproject/adapter/in/web/CardController.java`
- [X] T077 [US4] Create `src/main/resources/templates/delete-confirm.html` naming the card, with a confirm form carrying `revision` and the CSRF token and a decline link back to the board
- [X] T078 [US4] Implement `DELETE /api/cards/{cardId}` in `src/main/java/com/todoproject/adapter/in/rest/CardRestController.java`

**Checkpoint**: All four CRUD stories independently functional

---

## Phase 7: User Story 5 - Switch between light and dark appearance (Priority: P5)

**Goal**: Three appearance states — follow-device, light, dark — all reachable, remembered between
visits, with no scripting and no flash of the wrong theme.

**Independent Test**: Load with the device set to dark and confirm dark applies unprompted; switch
to light and confirm it survives a browser restart; cycle back to follow-device — quickstart
scenario 7.

### Tests for User Story 5

- [X] T079 [P] [US5] Write `src/test/java/com/todoproject/adapter/web/ThemeControllerTest.java`: `POST /theme` accepts `SYSTEM`, `LIGHT` and `DARK` and rejects anything else; the cookie is set with `SameSite=Lax`, `HttpOnly` and a one-year age and carries no identifier (research R10); a `redirectTo` outside this application is ignored in favour of `/`
- [X] T080 [P] [US5] Write `src/test/java/com/todoproject/adapter/web/ThemeRenderingTest.java` using Jsoup: no cookie renders **no** `data-theme` attribute so `prefers-color-scheme` governs (FR-019); `LIGHT` and `DARK` render `data-theme="light"` and `"dark"` on `<html>`; the control's accessible name states the current state and the next (FR-020a)

### Implementation for User Story 5

- [X] T081 [P] [US5] Create `src/main/java/com/todoproject/adapter/in/web/AppearancePreference.java` as an enum with exactly `SYSTEM`, `LIGHT`, `DARK` (FR-019)
- [X] T082 [US5] Implement `POST /theme` in `src/main/java/com/todoproject/adapter/in/web/ThemeController.java`, reading and writing the functional cookie and validating `redirectTo` as an internal path only
- [X] T083 [US5] Add the appearance control to `src/main/resources/templates/layout.html` as a `POST /theme` form present on every screen, cycling through all three states so follow-device is reachable again after an explicit choice (FR-020)
- [X] T084 [US5] Add the dark palette to `src/main/resources/static/css/app.css` with the guarded selectors from research R10: tokens on bare `:root`, overridden under `@media (prefers-color-scheme: dark)` guarded by `:root:not([data-theme="light"])`, and again under `:root[data-theme="dark"]`, so an explicit choice wins in both directions
- [ ] T085 [P] [US5] Verify 4.5:1 contrast for every text and control colour in both palettes across the board, forms, empty states, notices and error pages, recording the measured ratios in `specs/001-kanban-todo-board/quickstart.md` scenario 7 (FR-021, SC-007)
- [ ] T086 [P] [US5] Confirm both themes cover every screen and state listed in FR-018 across `src/main/resources/templates/` — board, card form, delete confirmation, empty states, notices and `templates/error/{403,404,500}.html` from T033

**Checkpoint**: All five user stories independently functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: The acceptance bar that spans stories, plus the merge gates in plan.md

- [ ] T087 [P] Run every core flow with JavaScript disabled per scenario 8 of `specs/001-kanban-todo-board/quickstart.md` and record the result there; this is the acceptance bar from FR-030 and constitution principle IV, not a degraded mode
- [ ] T088 [P] Audit the board at 320×568 and at 200% zoom per scenario 9 of `specs/001-kanban-todo-board/quickstart.md`, fixing findings in `src/main/resources/static/css/app.css`: no horizontal page scrolling, no clipped controls, every target at least 44×44 px, a 120-character title and a long unbroken URL both wrapping or clamping without widening the page
- [ ] T089 [P] Walk through the two-tab concurrency cases in scenario 10 of `specs/001-kanban-todo-board/quickstart.md` and confirm no stale write resurrects a card's earlier column and no write recreates a deleted card
- [ ] T090 [P] Walk through the limits in scenario 11 of `specs/001-kanban-todo-board/quickstart.md`: the card cap and the write rate limit each produce a clear message rather than a server error, on both the HTML and REST surfaces
- [X] T091 [P] Review every log statement across `src/main/java/` and confirm no card title or note reaches the log at any level, including exception paths (research R12, security CHK026)
- [ ] T092 [P] Measure a 100-card board against SC-010 (ready to use in under 2 seconds on a mid-range phone over a 3G-class connection) and record the figure in `specs/001-kanban-todo-board/quickstart.md`
- [X] T093 Write `README.md` at the repository root stating in plain words that there is one shared board with no login, that the port is bound to localhost and must not be exposed without accounts (FR-026a, FR-026b), and that the default profile loses the board on restart with `durable` as the alternative
- [ ] T094 Run `mvn verify` and clear every formatter finding and compiler warning so merge gate 6 passes with no new warnings
- [ ] T095 Execute quickstart scenarios 1–14 end to end in `specs/001-kanban-todo-board/quickstart.md` and confirm each expected outcome
- [ ] T096 Review the reviewer-owned items still open in `specs/001-kanban-todo-board/checklists/ux.md` and `security.md` — ux CHK011, CHK022, CHK032, CHK033 and security CHK031 — and either satisfy them or record the decision in the checklist notes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **User Stories (Phases 3–7)**: All depend on Phase 2 completion, then proceed in priority order
  or in parallel if staffed
- **Polish (Phase 8)**: Depends on every story you intend to ship

### User Story Dependencies

- **US1 (P1)**: After Phase 2. No dependency on another story — this is the MVP
- **US2 (P2)**: After Phase 2. Needs cards to exist to be demonstrable, which US1 provides, but
  its code touches no US1 file except `fragments/card.html` (T059) and `board.html` (T061)
- **US3 (P3)**: After Phase 2. Shares `card-form.html` (T069) and `CardController` (T068) with US1
- **US4 (P4)**: After Phase 2. Shares `CardController` (T076) with US1–US3
- **US5 (P5)**: After Phase 2. Shares `layout.html` (T083) and `app.css` (T084) with Phase 2's T032.
  Fully independent of US1–US4 — the appearance control works on an empty board

### Within Each User Story

- Tests are written before the implementation they cover and must fail first
- Ports before services, services before controllers, controllers before templates
- Both inbound adapters (HTML and REST) for a story land in the same phase, so the story is
  complete on both surfaces before the next one starts

### Shared-File Serialization

These tasks touch the same file and must not run concurrently:

- `CardController.java` — T049 (US1), T058 (US2), T068 (US3), T076 (US4)
- `CardRestController.java` — T052 (US1), T060 (US2), T070 (US3), T078 (US4)
- `fragments/card.html` — T050 (US1), T059 (US2)
- `card-form.html` — T051 (US1), T069 (US3)
- `app.css` — T032 (Phase 2), T062 (US2), T084 (US5)
- `board.html` — T050 (US1), T061 (US2)

### Parallel Opportunities

- Setup: T003–T006 in parallel after T002
- Foundational: T008, T009, T010, T013, T017, T018, T019, T020 in parallel; then T015, T016, T025,
  T027, T028, T031, T035 in parallel once their subjects exist
- Every story's test tasks run in parallel with each other
- Across stories: US5 is genuinely parallel to US1–US4 — different files, no shared controller
- Polish: T087–T092 in parallel

---

## Parallel Example: User Story 1

```bash
# All US1 tests together (they must fail before implementation):
Task: "CreateCardServiceTest in src/test/java/com/todoproject/application/CreateCardServiceTest.java"
Task: "ViewBoardServiceTest in src/test/java/com/todoproject/application/ViewBoardServiceTest.java"
Task: "BoardControllerTest in src/test/java/com/todoproject/adapter/web/BoardControllerTest.java"
Task: "CreateCardFlowTest in src/test/java/com/todoproject/adapter/web/CreateCardFlowTest.java"
Task: "EscapingTest in src/test/java/com/todoproject/adapter/web/EscapingTest.java"
Task: "CardCreateReadRestTest in src/test/java/com/todoproject/adapter/rest/CardCreateReadRestTest.java"

# Then the independent US1 pieces:
Task: "ViewBoardQuery port in src/main/java/com/todoproject/application/port/in/ViewBoardQuery.java"
Task: "CreateCardUseCase port in src/main/java/com/todoproject/application/port/in/CreateCardUseCase.java"
Task: "CreateCardForm DTO in src/main/java/com/todoproject/adapter/in/web/dto/CreateCardForm.java"
Task: "Board view models in src/main/java/com/todoproject/adapter/in/web/view/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup (T001–T006)
2. Phase 2: Foundational (T007–T035) — the largest phase by design: a framework-free core plus a
   persistence adapter behind its port is genuinely prerequisite work, and T007 must land early so
   the architecture gate is live while the core is being written
3. Phase 3: User Story 1 (T036–T052)
4. **STOP and VALIDATE**: quickstart scenarios 1–3, plus scenario 8 with JavaScript disabled
5. At this point the application is a usable persistent task list with a REST surface

### Incremental Delivery

1. Setup + Foundational → core ready, architecture gate live
2. US1 → validate → **MVP**
3. US2 → validate → a real kanban
4. US3 → validate → corrections possible
5. US4 → validate → full CRUD
6. US5 → validate → light and dark
7. Phase 8 → the cross-cutting acceptance bar

### Parallel Team Strategy

With more than one developer, after Phase 2:

- Developer A: US1 then US2 (they share `card.html` and `board.html`)
- Developer B: US5 immediately — no file overlap with US1–US4 beyond `layout.html`/`app.css`,
  which Phase 2 has already created
- Developer C: US3 then US4, coordinating on `CardController.java` with Developer A

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- Field constraints are quoted verbatim from [data-model.md](./data-model.md) so no limit is left to
  implementation-time discretion
- T007 is deliberately the first task of Phase 2: the dependency-direction gate should be failing
  the build before the core exists, not added after it

## Implementation status (2026-10-14)

80 of 96 tasks are checked off. All production code for all five user stories exists, and the
mandatory test gates (ArchUnit, domain, every application service) are in place. No JDK/Maven was
available in the environment that wrote this code, so **nothing here has been compiled or run** —
see the session's final report for what that means and what to check first.

The 16 tasks left unchecked split into two kinds:

**Test coverage not yet written** (the production code they would exercise already exists):
T054, T055 (US2 move — web flow and REST), T064, T065 (US3 edit — web flow and REST), T072, T073
(US4 delete — web flow and REST), T085, T086 (US5 — numeric contrast measurement and a full
per-screen theme sweep).

**Needs a running application, so it could not be done in this environment**: T087–T090, T092,
T094, T095 (every quickstart scenario, the JS-disabled walkthrough, the 320px/zoom audit, the
two-tab and limits walkthroughs, the 100-card performance measurement, and `mvn verify` itself).
T096 (review the open `ux.md`/`security.md` checklist items) was explicitly deferred earlier in
this session at the user's instruction to proceed without blocking on it.
