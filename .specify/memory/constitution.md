<!--
SYNC IMPACT REPORT (scratch material for amendment review - remove before commit)

Version change: (unfilled template) -> 1.0.0
Rationale: Initial ratification. All placeholders replaced with concrete, project-specific
governance. No prior ratified version existed, so this is 1.0.0 rather than a bump.

Principles defined (all new):
  - I. Security-First (NON-NEGOTIABLE)
  - II. Hexagonal Architecture (Ports & Adapters) (NON-NEGOTIABLE)
  - III. Validate Every Input (NON-NEGOTIABLE)
  - IV. Mobile-First Front-End
  - V. Clean Code

Renamed principles: none (no prior named principles).

Added sections:
  - Architecture & Technology Constraints (was [SECTION_2_NAME])
  - Development Workflow & Quality Gates (was [SECTION_3_NAME])
  - Governance (filled)

Removed sections: none. Template comment hints removed after substitution.

Deferred items / TODOs: none. Ratification date set to the adoption date (2026-10-14).

Note: this document is written in English to stay consistent with the Spec Kit templates and
downstream artifacts (spec.md, plan.md, tasks.md). Switching the whole set to Portuguese would
be a MINOR amendment if the team prefers it.
-->

# todo-project Constitution

## Core Principles

### I. Security-First (NON-NEGOTIABLE)

Security is a design input, not a hardening pass. Every feature spec MUST state who may perform
the action and what data it exposes before implementation starts.

- Authentication and authorization MUST be enforced inside the application core (use case
  boundary). UI-level hiding or adapter-level checks alone are insufficient.
- Templates MUST escape all rendered user-supplied content; auto-escaping MUST NOT be disabled
  without a recorded justification in the feature plan.
- CSRF protection MUST be enabled for every state-changing request.
- Secrets (credentials, keys, tokens) MUST NOT appear in the repository, templates, logs, or
  error messages. Configuration comes from externalized sources.
- Error responses MUST NOT leak stack traces, SQL, framework internals, or persistence
  identifiers to the user.
- Security-relevant events (login success/failure, authorization denial, validation rejection
  bursts) MUST be logged; secrets and plaintext personal data MUST NOT be.
- A dependency with a known critical or high severity vulnerability MUST NOT be merged.

Rationale: this is a multi-user application handling personal task data. A breach is not
recoverable by a patch, so the cost of prevention always beats the cost of repair.

### II. Hexagonal Architecture (Ports & Adapters) (NON-NEGOTIABLE)

The system is structured as a domain core surrounded by adapters. Dependencies point inward,
always.

- The domain and application layers MUST be free of framework types: no Spring annotations, no
  persistence/ORM annotations, no HTTP or servlet types, no template-engine types.
- Every interaction with the outside world MUST cross a port interface declared by the
  application core and implemented by an adapter.
- Inbound adapters (web controllers, view rendering) and outbound adapters (persistence, mail,
  external services) MUST depend on the core; the core MUST NOT depend on them.
- Dependency direction MUST be verified by an automated architecture test, not by convention.
- Packages are organized by role (`domain`, `application`, `adapter.in`, `adapter.out`), never
  by technical type (`controllers`, `services`, `dtos` as top-level buckets).

Rationale: the core carries the business rules worth protecting. Keeping it framework-free keeps
it testable without a container and replaceable independently of Spring, the template engine, or
the database.

### III. Validate Every Input (NON-NEGOTIABLE)

No externally-supplied value reaches domain logic unvalidated. "External" includes form fields,
query and path parameters, headers, cookies, uploaded files, and responses from external
services.

- Adapters MUST perform syntactic validation (presence, type, length, format, range, allowed
  values) and reject malformed requests before invoking a use case.
- The domain MUST enforce its own invariants independently. A domain object MUST reject invalid
  construction; partially-valid entities MUST NOT exist.
- Validation MUST be allowlist-based (deny by default), never denylist-based.
- Rejected input MUST produce a structured, field-level error for the user and MUST NOT trigger
  any persistence or side effect.
- Client-side validation is a usability affordance only and MUST NEVER be the sole check.
- Every new or changed input path MUST ship with at least one test proving invalid input is
  rejected.

Rationale: unvalidated input is the shared root of injection, data corruption, and broken
invariants. Validating at both the boundary and the core means neither layer has to trust the
other.

### IV. Mobile-First Front-End

The front-end is server-rendered through the Spring Boot template engine and is designed for the
smallest screen first.

- Styles MUST be authored for the smallest viewport first; responsive overrides MUST use
  `min-width` media queries only.
- Every screen MUST be usable at 320 CSS px wide with no horizontal scrolling.
- Interactive touch targets MUST be at least 44x44 CSS px.
- Core task flows (create, list, view, edit, complete, delete) MUST work with JavaScript
  disabled. JavaScript is progressive enhancement, never a prerequisite.
- Markup MUST be semantic, and every form control MUST have an associated accessible label.

Rationale: a todo application is used on a phone, in a hurry, on poor connectivity. Designing up
from the constrained case yields a smaller, faster, more accessible result than retrofitting a
desktop layout downward.

### V. Clean Code

Readability is a delivery requirement, enforced at review.

- Names MUST reveal intent. Abbreviations and single-letter identifiers are not acceptable
  outside loop counters.
- Each function MUST do one thing at one level of abstraction. Each class MUST have one reason
  to change.
- Commented-out code and dead code MUST be deleted, not left behind. A `TODO` MUST reference a
  tracked issue or it MUST NOT be committed.
- Duplicated logic MUST be extracted on its third occurrence.
- Magic numbers and magic strings MUST be named constants or configuration.
- Every use case MUST have an automated test. Every bug fix MUST add a regression test that
  fails before the fix.
- Formatter and linter MUST pass with no new warnings before merge.

Rationale: the security and architecture rules above are only enforceable in code a reviewer can
actually follow. Clean code is what makes the other four principles auditable.

## Architecture & Technology Constraints

- **Deployment shape**: one modular monolith. A single Spring Boot application producing a single
  deployable artifact. Splitting into services or adding a separate front-end deployment
  requires a MAJOR constitution amendment.
- **Language and framework**: Java with Spring Boot. Front-end HTML is produced by the Spring
  Boot template engine. No SPA framework and no separate JavaScript build pipeline.
- **Layering**: hexagonal as defined in Principle II. The build MUST fail when the core imports
  framework or adapter code.
- **Persistence**: reached only through an outbound port. Schema migrations are versioned and
  checked in. The domain contains no SQL, no ORM mappings, and no transaction annotations.
- **Configuration**: externalized and profile-based. No environment-specific values compiled
  into source. Secrets are injected at runtime.
- **Dependencies**: each new third-party dependency MUST be justified in the feature plan,
  including what it replaces and why in-house code is insufficient.

## Development Workflow & Quality Gates

Work follows the Spec Kit flow: `/speckit-specify` -> `/speckit-plan` -> `/speckit-tasks` ->
`/speckit-implement`. The Constitution Check section of `plan.md` MUST be completed before task
generation begins.

The following gates MUST pass before any change is merged:

1. Build succeeds and the full test suite passes.
2. Architecture tests pass: core free of framework imports, dependency direction inward only.
3. Every new or changed input path has a validation test, including a rejection case.
4. Security review of the change covers authentication, authorization at the use case boundary,
   input validation, output escaping, CSRF on state-changing requests, error-message leakage,
   and log hygiene.
5. New or changed UI is verified at 320 px width, with labeled controls and keyboard operability.
6. Formatter and linter are clean; no new warnings introduced.
7. At least one reviewer other than the author has approved.

Any deviation from a principle MUST be recorded in the plan's Complexity Tracking section with
the simpler alternative considered and the reason it was rejected. An undocumented deviation is
a review rejection, not a discussion.

## Governance

This constitution supersedes all other development practices, conventions, and habits for
todo-project. Where guidance conflicts, this document wins.

**Amendment procedure**: amendments are proposed as a change to this file, accompanied by the
rationale, the version bump, and a note on which templates or commands are affected. An
amendment takes effect once merged. Amendments that remove or redefine a NON-NEGOTIABLE
principle require explicit maintainer approval and a migration plan for existing code.

**Versioning policy**: semantic versioning of governance.

- MAJOR: a principle is removed or redefined in a backward-incompatible way.
- MINOR: a principle or section is added, or existing guidance is materially expanded.
- PATCH: clarifications, wording, and non-semantic refinements.

**Compliance review**: every feature plan MUST include a Constitution Check against the
principles above. Reviewers MUST reject changes that violate a principle without a documented
justification. When a principle proves genuinely unworkable, the correct response is to amend
this constitution, not to quietly ignore it. The full document MUST be reviewed at least every
six months, and whenever the technology stack changes materially.

**Runtime guidance**: agent and tooling guidance files (for example `CLAUDE.md`) complement this
constitution with day-to-day operational detail. They MUST NOT contradict it; where they do,
this constitution governs and the guidance file MUST be corrected.

**Version**: 1.0.0 | **Ratified**: 2026-10-14 | **Last Amended**: 2026-10-14
