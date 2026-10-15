# Phase 0 Research: Kanban Todo Board

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-14

**Purpose**: resolve the two open `[NEEDS CLARIFICATION]` markers in the spec, settle the
technology decisions supplied with `/speckit-plan`, and close the requirement-quality findings
raised in [checklists/security.md](./checklists/security.md) and
[checklists/ux.md](./checklists/ux.md) that need a design answer.

---

## R1 — Task versus note: one entity or two

**Resolves**: spec FR-005 `[NEEDS CLARIFICATION]`, ux CHK034, ux CHK001

**Decision**: one entity. A **Task Card** carries a required `title` and an optional `note`.
There is no separate note object and no nesting.

**Rationale**: the request uses "tarefas" and "anotações" interchangeably — "remover, editar ou
adicionar novas tarefas ou anotações" treats them as one list of things, and "essas anotações
devem ser movidas pelo kanban" makes the note the thing that moves, which is the card. A single
entity means one form, one validation path, one repository, and one set of REST endpoints. If
dated progress entries are wanted later, they can be added as a child collection without
reshaping the card.

**Alternatives considered**:

- *Notes as a child collection of a task* — rejected for this stage: adds a card-detail screen,
  nested CRUD, and nested REST resources for value the request does not clearly ask for.
- *Tasks and standalone notes as sibling card types* — rejected: forces a visible type
  distinction in the UI and a discriminator in the model, for a "post-it without a task" use case
  that was never stated.

**Consequence for the card face** (closes ux CHK001): the card shows the full title and, when a
note exists, a two-line clamped preview plus a marker that more text exists. The full note is
visible on the edit screen.

---

## R2 — Board scope with no login

**Resolves**: spec FR-026 `[NEEDS CLARIFICATION]`, security CHK001, ux CHK035

**Decision**: **one single shared board**, with the application treated as a
**local / single-user deployment** for this stage. No visitor identifier, no cookie-scoped
boards, no per-visitor isolation.

**Rationale**: the stack chosen for this stage settles it. An in-memory H2 database and no
authentication describe an application run on the developer's own machine, not a hosted
multi-visitor service. Introducing an anonymous visitor identifier would add a cookie, a
retention policy, and a consent question (security CHK019–CHK023) to buy isolation that nobody
needs while the application is single-user.

**The exposure this accepts, stated explicitly** (constitution principle I requires the feature
to declare what it exposes): every visitor who can reach the application sees and can modify or
delete every card. There is no privacy boundary inside the application. This is acceptable
**only** while the deployment is local. Publishing this application to a reachable network
without first adding accounts would expose one person's task text to anyone with the URL, and
MUST be treated as a new feature with its own spec, not a deployment detail.

**Consequences**:

- `application.yml` binds to `localhost` by default. Making it reachable requires an explicit,
  deliberate change.
- The README and quickstart state the single-shared-board limitation in plain words.
- security CHK019, CHK021, CHK022, CHK023 (retention, identifier lifetime, consent, guessable
  identifiers) are **not applicable at this stage** and are recorded as such rather than silently
  dropped. Card identifiers are still random UUIDs, so nothing is enumerable if the scope changes.

**Alternatives considered**:

- *Per-visitor board via anonymous cookie* — the better answer for a hosted deployment, rejected
  now as unjustified complexity for a local app; it is the natural first step when accounts arrive.
- *Browser-only storage, no server persistence* — rejected: it breaks constitution principle IV
  (core flows work without client-side scripting) and principle II (persistence behind an
  outbound port), and would require a constitution amendment.

---

## R3 — H2 in memory versus H2 in a file

**Decision**: use **H2 in in-memory mode** as requested for the default profile, and ship a
one-property switch to file mode. Tests always use a fresh in-memory database.

**The tension, stated plainly**: in-memory H2 keeps data only while the application process
lives. Spec FR-026 and SC-008 require cards to survive "page reloads and browser restarts" —
which in-memory H2 does satisfy, because the server keeps running — but the board is **empty
after every application restart**. For a todo application that is a surprising loss, so it is
recorded here as an accepted, visible limitation rather than discovered later.

**Rationale**: the stack was specified explicitly, and for a local development stage a disposable
database is genuinely convenient — no migration state, no stale data between runs. The cost of
reversing the decision is one line.

**The switch**: change the datasource URL from `jdbc:h2:mem:todo` to
`jdbc:h2:file:./data/todo;AUTO_SERVER=TRUE`. No code changes, because persistence sits behind an
outbound port. A `durable` Spring profile ships pre-configured with exactly this.

**Alternatives considered**:

- *File mode as the default* — the stronger match for FR-026's spirit; not chosen because the
  request named in-memory explicitly. Flagged for the user to overrule in one step.
- *A second real database* — rejected outright: no need at this scale, and it contradicts the
  single-artifact constraint's spirit of keeping the stage small.

---

## R4 — JPA without putting JPA in the domain

**Decision**: two separate types with an explicit mapper. `TaskCard` is a plain domain class with
no annotations. `CardJpaEntity` lives in `adapter.out.persistence`, carries every JPA annotation,
and never leaves that package. `CardMapper` converts between them in both directions.

**Rationale**: constitution principle II forbids persistence annotations in the domain and makes
the build fail on violation. Annotating the domain aggregate directly is the common Spring
shortcut and is exactly what the principle exists to prevent: it couples the aggregate's shape to
the table's shape and drags a JPA runtime into every domain unit test.

**What this costs**: one extra class and a mapper per aggregate — about 40 lines for this feature,
and the duplication is visible rather than hidden. Accepted knowingly.

**Alternatives considered**:

- *Annotate the domain aggregate* — rejected: direct constitution violation, and the ArchUnit test
  would fail the build.
- *Hibernate XML mapping to keep the domain clean without a second class* — rejected: trades a
  readable mapper for an unfamiliar XML dialect that most reviewers cannot audit.

---

## R5 — Where validation lives, and what "allowlist" means here

**Resolves**: security CHK005, CHK006, CHK007, CHK011

**Decision**: validation runs at two independent layers, as constitution principle III requires.

1. **Adapter layer (syntactic)** — Bean Validation annotations on web form DTOs and REST request
   DTOs: required-ness, length bounds, and the character policy below. Rejection happens before
   any use case is invoked, and produces field-level messages.
2. **Domain layer (invariants)** — `CardTitle` and `CardNote` are value objects whose constructors
   reject anything invalid. A `TaskCard` cannot be constructed in a half-valid state. The domain
   uses plain guard clauses, **not** Bean Validation annotations, because `jakarta.validation` is
   a framework dependency the domain must not carry (principle II).

**The character policy** (this is the concrete "allowlist, deny by default" principle III demands,
which the spec left unstated):

- Input is normalized to Unicode NFC first, then trimmed.
- **Permitted** Unicode categories: letters (L\*), marks (M\*), numbers (N\*), punctuation (P\*),
  symbols (S\*), and the space separator (Zs). Notes additionally permit the line feed `\n`.
- **Rejected** categories: control characters (Cc), format characters (Cf — this is what blocks
  zero-width characters and bidirectional overrides), surrogates (Cs), private use (Co), and
  unassigned code points (Cn).
- Titles collapse internal whitespace runs to a single space. Notes preserve line structure but
  normalize `\r\n` to `\n`.
- A title that is empty after normalization and trimming is rejected as empty — this is what makes
  FR-007's whitespace-only rule concrete.

**Rationale**: a length-plus-required check is not an allowlist, and the spec only had that. A
category allowlist is testable, locale-safe for Portuguese (accented letters are L\*), and blocks
the invisible-character class the checklist flagged.

**Alternatives considered**:

- *Regex denylist of known-bad characters* — rejected: principle III mandates deny-by-default, and
  denylists miss new cases.
- *Accept any text and rely only on output escaping* — rejected: escaping protects the page, but
  invisible control characters would still corrupt stored data and display.

---

## R6 — CSRF protection with no authentication

**Resolves**: security CHK015, security CHK014

**Decision**: include `spring-boot-starter-security` **for its protections only, with no
authentication**. Every route is `permitAll()`. There is no login page, no user store, no
`UserDetailsService`.

- **CSRF is enabled for the HTML form routes.** Thymeleaf injects the token into every form
  automatically, so the non-scripted flows keep working unchanged.
- **CSRF is disabled for `/api/**`.** The REST surface is stateless, consumes and returns JSON,
  and carries no cookie or ambient credential, so there is nothing for a cross-site request to
  ride on. This exemption is recorded here as a deliberate decision, not an oversight.
- **Frame embedding is denied** (`X-Frame-Options: DENY`) because every action is unauthenticated
  and the board must not be wrapped by another site (security CHK014).
- A minimal Content-Security-Policy is set: `default-src 'self'`, no inline script. The
  no-JavaScript baseline means the policy costs nothing to satisfy.

**Rationale**: constitution principle I requires CSRF on every state-changing request. "No
authentication for now" removes logins, not protections — a request forged by another site would
otherwise be able to delete the board.

**Alternatives considered**:

- *Skip Spring Security entirely and hand-roll a CSRF token* — rejected: reimplements a solved
  problem and loses the security header defaults.
- *Leave CSRF enabled for `/api/**` too* — rejected: it would force every API consumer to fetch a
  token for no benefit on a cookie-less, credential-less endpoint, and the request explicitly asks
  for an API that is easy to consume later.

---

## R7 — Moving cards with no client-side scripting

**Resolves**: spec FR-011, FR-030; ux CHK016, CHK020

**Decision**: every move is a plain `POST` from a form containing the target stage. Each card
renders a compact set of move controls — one per stage other than its own — as submit buttons
inside that form. The response is a `303 See Other` redirect back to the board, anchored to the
moved card.

**Drag-and-drop is out of scope for this stage.** The spec's "MAY be offered" left it untestable;
this closes it. The markup is built so that a later enhancement layer can attach drag behaviour to
the same forms without changing the server contract.

**Rationale**: the post-redirect-get pattern is the only move mechanism that works with scripting
unavailable (principle IV), and a single mechanism avoids two divergent code paths.

**Alternatives considered**:

- *`PUT`/`DELETE` via hidden `_method` override for HTML forms* — rejected: adds a filter and
  gains nothing, since the REST adapter already exposes the correct verbs for machine consumers.
- *Drag-and-drop with a non-scripted fallback* — rejected for this stage: doubles the interaction
  surface before the basics are proven.

---

## R8 — Two inbound adapters over one application core

**Decision**: the HTML adapter (`adapter.in.web`) and the REST adapter (`adapter.in.rest`) are
siblings. Both depend only on the inbound ports. Neither calls the other, and no use case knows
which one invoked it.

**Rationale**: this is what the hexagonal structure is for, and it is the cheapest way to honour
"endpoints RESTful para as funções" without a second implementation of the rules. The REST surface
adds a serialization layer and its own DTOs, not new behaviour.

**Stage identifiers differ by adapter on purpose**: the REST API uses stable English codes
(`BACKLOG`, `IN_PROGRESS`, `PAUSED`, `DONE`) so a consumer's integration does not break if a UI
label changes; the HTML UI renders the Portuguese labels from a message bundle. The domain enum is
the single source of truth both read from.

**What the REST surface adds to the risk picture**: it is a second unauthenticated write path. The
card cap and write rate limit in R11 therefore apply at the application core, not in one adapter,
so both surfaces are covered by one rule.

---

## R9 — Enforcing the dependency direction

**Decision**: ArchUnit tests in `src/test/java/.../architecture/`, run as part of the normal test
phase so a violation fails the build.

Rules asserted:

1. No class in `domain..` or `application..` may depend on `org.springframework..`,
   `jakarta.persistence..`, `jakarta.validation..`, `jakarta.servlet..`, or `adapter..`.
2. `adapter.out.persistence..` may not be accessed from `adapter.in..`.
3. `domain..` may not depend on `application..`.
4. Layer order: `domain` ← `application` ← `adapter`, with no cycles.
5. `CardJpaEntity` is accessible only from within its own package.

**Rationale**: constitution principle II requires the direction be "verified by an automated
architecture test, not by convention". Without this the layering decays in weeks.

---

## R10 — Theme as three states, server-rendered

**Resolves**: ux CHK018 (a real contradiction in the spec), CHK013, CHK014, CHK019

**Decision**: the stored preference has **three** values — `SYSTEM` (default), `LIGHT`, `DARK` —
and the control cycles through all three, so a person can return to following their device. The
spec's Key Entities already implied `follow-device`; FR-018/FR-020 described only a two-way switch.
The spec is being corrected to the three-state model.

**Mechanism, scripting-free**:

- No preference stored → no `data-theme` attribute is rendered, and CSS `prefers-color-scheme`
  decides. This is how FR-019's device default works with no JavaScript.
- An explicit choice → `<html data-theme="light|dark">` is rendered server-side from a cookie.
  CSS tokens are defined on `:root`, overridden under `@media (prefers-color-scheme: dark)` guarded
  by `:root:not([data-theme="light"])`, and again under `:root[data-theme="dark"]`, so the explicit
  choice wins in both directions.
- The control is a `POST /theme` form in the page header, present on every screen, with an
  accessible name that states the current mode and the next one.
- The cookie is strictly functional (`SameSite=Lax`, `HttpOnly`, 1 year, no identifier of any kind),
  which is why no consent prompt is required (security CHK022).

**Alternatives considered**:

- *Two-state toggle as literally written* — rejected: it strands anyone who taps the control once
  from ever following their device again.
- *Theme in `localStorage` applied by a script* — rejected: violates principle IV and produces a
  flash of the wrong theme.

---

## R11 — Limits on an unauthenticated write surface

**Resolves**: security CHK016, CHK017, CHK018

**Decision**: two limits, enforced in the application core so both adapters inherit them.

- **Board capacity**: 500 cards. A create beyond the cap is rejected with a plain message telling
  the person the board is full and that they can delete cards to continue. Configurable via
  `todo.board.max-cards`.
- **Write rate limit**: 60 write actions per minute per client address, fixed window, in-memory.
  Exceeding it yields a "too many changes, try again in a moment" message. Configurable via
  `todo.limits.writes-per-minute`.

**Rationale**: creation is unauthenticated on two surfaces. Without a cap, one loop fills the heap;
the spec limited field length but never the number of cards. These are deliberately simple — an
in-memory counter, no external dependency — because the deployment is single-user and the point is
to have a defined behaviour at the boundary rather than an unbounded one.

**Alternatives considered**:

- *No limits because it is local-only* — rejected: it leaves the behaviour at the boundary
  undefined, which is what the checklist flagged.
- *A rate-limiting library or gateway* — rejected as disproportionate; a new dependency would need
  justification under the constitution's dependency rule.

---

## R12 — What gets logged

**Resolves**: security CHK025, CHK026

**Decision**: log security-relevant events at `INFO` with a structured, content-free shape:
event type, card id, stage (for moves), outcome, and client address. Logged events are validation
rejection, card-not-found, stale-write conflict, capacity rejection, and rate-limit rejection.

**Card titles and notes are never logged, at any level, including on exception paths.** Exception
messages crossing into the log carry identifiers, not content.

**Rationale**: constitution principle I requires security-relevant events be logged and personal
data not be. The spec specified neither. With a shared board the log is the only record of what
happened, and the card text is exactly the content that must stay out of it.

---

## R13 — Four columns at 320 px

**Resolves**: ux CHK012, CHK028

**Decision**: the four columns **stack vertically** in document order below 768 px, each as a
collapsible section with its name and count in the header, Backlog expanded by default. At 768 px
and above they become a four-column grid. Within a column, the card list scrolls after a
max-height rather than pushing the next column off-screen.

**Rationale**: stacking is the only option that needs no scripting (principle IV) and keeps the
page free of horizontal scrolling at 320 px (FR-027). Horizontal swiping and tabs both need
JavaScript to be usable; an accordion built from `<details>` elements does not.

**Alternatives considered**:

- *Horizontal scroll of four narrow columns* — rejected: FR-027 forbids horizontal page scrolling,
  and 4 × 280 px cannot fit 320 px.
- *Tabs, one column at a time* — rejected: hides the board's state, which is the point of a kanban,
  and needs scripting or four page loads.

---

## R14 — Concurrent edits from two tabs

**Resolves**: spec Edge Cases, security CHK029

**Decision**: optimistic locking. The card carries a `revision` number; every edit, move, and
delete form submits the revision it was rendered from. A mismatch means someone changed the card
first: the write is refused, nothing is modified, and the person sees "this card changed in
another tab — reload to see the current version". A write to a card that no longer exists yields a
not-found message and **never recreates it**.

**Rationale**: the spec required that a stale action must not "silently resurrect" a card's earlier
state but left it as an edge case with no requirement behind it. Optimistic locking makes refusal
the default rather than something each handler must remember.

**Alternatives considered**:

- *Last-write-wins* — rejected: it is exactly the silent resurrection the spec forbids.
- *Pessimistic locking* — rejected as absurd for a single-user board.

---

## R15 — Stack versions

**Decision**: Java 21 (LTS), Spring Boot 3.5.x, Maven. Dependencies: `web`, `thymeleaf`,
`data-jpa`, `validation`, `security`, `h2`, `actuator` (health only), plus ArchUnit, AssertJ, and
Jsoup for tests.

**Pin exact versions at project creation** from Spring Initializr rather than from this document —
treat the minor version above as the line to target, and verify the current patch release when the
build file is first written. Newer Spring Boot majors exist; adopting one is a deliberate choice,
not a default, and nothing in this design depends on it.

**Rationale**: Java 21 is a widely available LTS with records and pattern matching, which suit
value objects and the mapper. Maven is chosen over Gradle for a flatter learning curve and
reproducible builds without a daemon; nothing in the design depends on the choice.

---

## Resolved requirement-quality findings

Findings from the two custom checklists that this research answers, for the reviewer's trace:

| Finding | Where answered |
|---|---|
| ux CHK001 card face contents | R1 |
| ux CHK012 four columns at 320 px | R13 |
| ux CHK013, CHK014 theme control placement and name | R10 |
| ux CHK016, CHK020 drag-and-drop status | R7 |
| ux CHK018, CHK019 theme state contradiction | R10 |
| ux CHK028 large column presentation | R13 |
| ux CHK034, spec FR-005 | R1 |
| ux CHK035, security CHK001, spec FR-026 | R2 |
| security CHK005, CHK006, CHK007, CHK011 validation policy | R5 |
| security CHK014, CHK015 embedding and cross-site requests | R6 |
| security CHK016, CHK017, CHK018 abuse limits | R11 |
| security CHK019, CHK021, CHK022, CHK023 privacy | R2 (not applicable at this stage) and R10 (cookie) |
| security CHK025, CHK026 logging | R12 |
| security CHK029 stale writes | R14 |

Findings **still open for the reviewer**, not resolvable by design alone: ux CHK011 (what "modern
and pleasant" means as reviewable criteria), CHK022 (SC-003's testing method), CHK032 (confirming
the 120/2000 limits), CHK033 (whether moving out of "Concluído" needs confirmation), and security
CHK031 (transport security, which only matters once the deployment stops being local).
