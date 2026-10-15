# Phase 1 Data Model: Kanban Todo Board

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md)

Two representations, deliberately separate (research R4): a framework-free **domain model** that
owns the rules, and a **persistence model** that owns the table. A mapper in
`adapter.out.persistence` is the only place that knows both.

---

## Domain model

### `TaskCard` (aggregate root)

Plain Java class in `domain.model`. No annotations of any kind.

| Field | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `CardId` | no | Random UUID v4, assigned at creation, never reused |
| `title` | `CardTitle` | no | Value object, validated on construction |
| `note` | `CardNote` | yes | Value object; absent is modelled as `Optional.empty()`, never as `""` |
| `stage` | `BoardStage` | no | Enum; the column the card sits in |
| `createdAt` | `Instant` | no | Set once, never modified by any operation |
| `updatedAt` | `Instant` | no | Updated by every content change and every move |
| `revision` | `long` | no | Starts at 0, increments on each successful write; used to refuse stale writes (research R14) |

**Invariants enforced in the constructor and every mutator** — a `TaskCard` cannot exist in a
half-valid state (constitution principle III):

1. `id`, `title`, `stage`, `createdAt`, `updatedAt` are never null.
2. `updatedAt` is never earlier than `createdAt`.
3. `revision` is never negative.
4. Mutators advance `updatedAt`; `createdAt`, `id` and `revision` are not reachable for
   modification.

**`revision` has exactly one writer.** The aggregate carries the revision it was *loaded* at and
exposes `requireCurrentRevision(expected)`, which throws `StaleCardException` on a mismatch — so
refusing a stale write stays a domain rule. Advancing the number belongs to the persistence
adapter, where JPA's `@Version` already does it on flush. Incrementing it in the domain as well
would give one value two writers and drift the two layers apart by one on every save.

**Behaviour** (the domain owns these, not a service):

- `moveTo(BoardStage target, Instant now)` — changes `stage`. Any target is legal, including the
  current one (FR-009; a no-op move still advances `revision`, which keeps stale-write detection
  honest). Title, note and `createdAt` are untouched (FR-010).
- `rename(CardTitle newTitle, Instant now)` — replaces the title (FR-013).
- `changeNote(CardNote or empty, Instant now)` — replaces or clears the note (FR-013).
- Editing never changes `stage` (FR-015) — there is no mutator that could.

### `CardTitle` (value object)

| Rule | Value | Source |
|---|---|---|
| Required | yes — empty or whitespace-only is rejected | FR-007 |
| Normalization | Unicode NFC, trimmed, internal whitespace runs collapsed to one space | research R5 |
| Minimum length | 1 character after normalization | FR-007 |
| Maximum length | 120 characters, measured after normalization | FR-008 |
| Permitted characters | Unicode categories L\*, M\*, N\*, P\*, S\*, Zs | research R5 |
| Rejected characters | Cc, Cf, Cs, Co, Cn — this is what blocks zero-width and bidirectional override characters | research R5, security CHK007 |

Rejection throws `InvalidCardContentException` carrying the field name and a violation code, never
the offending content (research R12).

### `CardNote` (value object)

Same policy as `CardTitle` with three differences:

| Rule | Value |
|---|---|
| Required | no — absence is `Optional.empty()` |
| Maximum length | 2000 characters |
| Line breaks | `\n` is permitted; `\r\n` and `\r` are normalized to `\n`; line structure is preserved rather than collapsed |

A note that is whitespace-only after normalization is stored as absent, not as blank text.

### `BoardStage` (enum)

Fixed, closed set — not user-editable (FR-002).

| Constant | Order | REST code | UI label (pt-BR) |
|---|---|---|---|
| `BACKLOG` | 1 | `BACKLOG` | Backlog |
| `IN_PROGRESS` | 2 | `IN_PROGRESS` | Em andamento |
| `PAUSED` | 3 | `PAUSED` | Pausado |
| `DONE` | 4 | `DONE` | Concluído |

The enum constant is the single source of truth. The REST adapter serializes the constant name so
consumers are unaffected by label changes; the HTML adapter resolves labels from a message bundle
keyed by constant name (research R8). The display order is the declaration order — the UI never
hardcodes a sequence.

### `CardId` (value object)

Wraps a `UUID`. Random v4, so identifiers are not enumerable even though nothing depends on that
at this stage (research R2, security CHK023). Parsing a malformed identifier from an adapter
yields a not-found outcome, not a 500.

---

## State transitions

Every stage reaches every other stage directly, in both directions. There are no forbidden
transitions and no one-way columns (FR-009, US2 scenario 2).

```text
        ┌──────────────────────────────────────────┐
        │                                          │
        ▼                                          │
    BACKLOG ◄──────► IN_PROGRESS ◄──────► PAUSED ◄─┼──────► DONE
        ▲                  ▲                 ▲     │          ▲
        └──────────────────┴─────────────────┴─────┴──────────┘
              (all 12 ordered pairs are legal, plus self-moves)
```

A move is refused only for reasons unrelated to the transition itself: the card does not exist
(FR-012), the submitted revision is stale (research R14), the submitted stage is not one of the
four (research R5), or the write rate limit is exceeded (research R11).

**Lifecycle**: created in the stage the add action started from, defaulting to `BACKLOG` (FR-006);
deleted permanently with no soft-delete, archive, or tombstone (FR-017, security CHK024).

---

## Persistence model

### `CardJpaEntity`

Lives in `adapter.out.persistence` and is package-private where the language allows. Carries every
JPA annotation; nothing outside its package may reference it (enforced by ArchUnit, research R9).

| Column | Type | Constraints | Maps from |
|---|---|---|---|
| `id` | `UUID` | primary key | `CardId.value()` |
| `title` | `VARCHAR(120)` | `NOT NULL` | `CardTitle.value()` |
| `note` | `VARCHAR(2000)` | nullable | `CardNote.value()` or `NULL` |
| `stage` | `VARCHAR(20)` | `NOT NULL`, stored as enum **name** via `@Enumerated(EnumType.STRING)` | `BoardStage.name()` |
| `created_at` | `TIMESTAMP(6) WITH TIME ZONE` | `NOT NULL` | `createdAt` |
| `updated_at` | `TIMESTAMP(6) WITH TIME ZONE` | `NOT NULL` | `updatedAt` |
| `revision` | `BIGINT` | `NOT NULL`, JPA `@Version` | `revision` |

`EnumType.ORDINAL` is forbidden — reordering the enum would silently relabel every stored card.

**Indexes**: `idx_card_stage_updated_at` on `(stage, updated_at DESC)` — the board query reads all
cards grouped by stage and ordered most-recently-changed first (spec Assumptions, ux CHK015), and
this is the only read pattern that matters at this scale.

**Why `revision` is both a domain field and `@Version`**: the domain needs it because refusing a
stale write is a business rule the use case applies (research R14); JPA needs it to detect the
conflict at flush time. The mapper carries the same number across the boundary. The domain does not
know Hibernate manages it.

### Mapping

`CardMapper` converts both directions and is pure — no repository access, no clock, no side
effects. Reconstituting a `TaskCard` from a row goes through a dedicated factory that trusts
already-validated data rather than re-running normalization, so a stored value can never be
silently rewritten on read; a row that fails the invariants is a data corruption error, surfaced
loudly rather than repaired.

---

## Board-level rules

Not properties of a single card, enforced in the application core so both inbound adapters inherit
them (research R8, R11):

| Rule | Value | Behaviour when exceeded |
|---|---|---|
| Maximum cards on the board | 500 (`todo.board.max-cards`) | Create is refused with a message saying the board is full; nothing is stored |
| Write actions per minute, per client address | 60 (`todo.limits.writes-per-minute`) | Write is refused with a try-again message; nothing is stored |

Both limits apply to creates, edits, moves and deletes from **either** the HTML or the REST
surface, counted together.

---

## Error outcomes

Each outcome maps to exactly one user-facing message and one REST status. No outcome exposes a
stack trace, SQL, or internal identifier (FR-024, security CHK028).

| Outcome | Cause | HTML result | REST status |
|---|---|---|---|
| `InvalidCardContent` | Validation failed (FR-007, FR-008, R5 character policy) | Re-render the form with field-level messages and the typed input preserved | `400` |
| `CardNotFound` | Unknown or malformed card id (FR-012) | Board with a "this card no longer exists" notice; never recreated | `404` |
| `StaleCard` | Submitted revision does not match stored revision (R14) | "Changed in another tab — reload to see the current version" | `412` (`If-Match` mismatch); `428` when the header is absent |
| `BoardFull` | Card cap reached (R11) | "The board is full — delete a card to add another" | `409` |
| `RateLimited` | Write rate limit exceeded (R11) | "Too many changes — try again in a moment" | `429` |
| `InvalidStage` | Submitted stage is not one of the four (R5) | Treated as invalid input | `400` |

---

## Traceability

| Spec requirement | Model element |
|---|---|
| FR-001, FR-002 | `BoardStage` — closed enum, declaration order is display order |
| FR-003 | Board query returns per-stage counts |
| FR-005, FR-005a | `TaskCard.title` + optional `TaskCard.note` |
| FR-006 | Creation takes a target stage, defaulting to `BACKLOG` |
| FR-007, FR-008 | `CardTitle` / `CardNote` construction rules |
| FR-009, FR-010 | `moveTo` — all transitions legal; content and `createdAt` preserved |
| FR-012 | `CardNotFound` outcome |
| FR-013, FR-015 | `rename`, `changeNote`; no mutator touches `stage` |
| FR-017 | Hard delete; no soft-delete column exists |
| FR-019, FR-020 | `AppearancePreference` — three states, held in a cookie, not in this database |
| FR-022 | Two validation layers: adapter DTOs and value objects |
| FR-026 | Single shared table, no visitor or owner column |
| Edge case: two tabs | `revision` + `StaleCard` outcome |
