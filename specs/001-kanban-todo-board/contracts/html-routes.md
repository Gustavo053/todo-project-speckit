# Contract: HTML Routes (no-scripting interface)

**Feature**: [spec.md](../spec.md) | **Research**: [research.md](../research.md)

This is the contract for the user-facing interface. It is a **first-class contract, not a
fallback**: constitution principle IV and spec FR-030 require every core action to work with
client-side scripting unavailable, so each action below is a full page navigation or a form
submission followed by a redirect.

Every state-changing route is `POST` and carries a CSRF token, which Thymeleaf injects into each
form automatically (research R6). Every successful write answers `303 See Other` so a reload never
repeats the action.

---

## Routes

| Method | Path | Purpose | Success | Requirement |
|---|---|---|---|---|
| `GET` | `/` | The board: four columns in display order, each with name, count, cards, and an empty-state message when empty | `200` | FR-001, FR-003, FR-004 |
| `GET` | `/cards/new` | Add form. Optional `?stage=` preselects the target column, default `BACKLOG` | `200` | FR-006 |
| `POST` | `/cards` | Create a card | `303` → `/#card-{id}` | FR-005, FR-007, FR-008 |
| `GET` | `/cards/{id}/edit` | Edit form, prefilled, with the full note | `200` | FR-013, FR-005a |
| `POST` | `/cards/{id}` | Save title and note | `303` → `/#card-{id}` | FR-013, FR-015 |
| `POST` | `/cards/{id}/move` | Move to the submitted stage | `303` → `/#card-{id}` | FR-009, FR-010, FR-011 |
| `GET` | `/cards/{id}/delete` | Confirmation page naming the card about to be deleted | `200` | FR-017, ux CHK003 |
| `POST` | `/cards/{id}/delete` | Delete permanently | `303` → `/` | FR-016, FR-017 |
| `POST` | `/theme` | Set the appearance preference, then return to the submitting page | `303` → referring path | FR-019, FR-020 |

There is no `GET /cards/{id}` detail page in this stage — the card face plus the edit form cover
everything the spec asks for.

## Form fields

### `POST /cards`

| Field | Required | Rules |
|---|---|---|
| `title` | yes | 1–120 characters after normalization; whitespace-only rejected (FR-007) |
| `note` | no | up to 2000 characters; blank is stored as absent |
| `stage` | no | one of the four stage codes; defaults to `BACKLOG` |
| `_csrf` | yes | injected by the template |

### `POST /cards/{id}` and `POST /cards/{id}/delete`

| Field | Required | Rules |
|---|---|---|
| `title` | yes (edit only) | as above |
| `note` | no (edit only) | as above |
| `revision` | yes | the revision the form was rendered from; a mismatch refuses the write (research R14) |
| `_csrf` | yes | injected by the template |

### `POST /cards/{id}/move`

| Field | Required | Rules |
|---|---|---|
| `stage` | yes | one of the four stage codes; anything else is invalid input |
| `revision` | yes | as above |
| `_csrf` | yes | injected by the template |

Each card renders one move form containing three submit buttons — one per stage other than its
own — so a move is a single interaction with no scripting and no menu (research R7, spec SC-002).

### `POST /theme`

| Field | Required | Rules |
|---|---|---|
| `preference` | yes | `SYSTEM`, `LIGHT` or `DARK` — all three reachable (FR-019, FR-020) |
| `redirectTo` | no | a path within this application; anything else is ignored and `/` is used |
| `_csrf` | yes | injected by the template |

Stored in a functional cookie: `SameSite=Lax`, `HttpOnly`, one year, no identifier of any kind
(research R10).

---

## Failure behaviour

Rejected writes re-render the originating page with status `422` (`200` is wrong — nothing was
created) and never redirect, so the person keeps what they typed.

| Situation | Response | Shown to the person | Requirement |
|---|---|---|---|
| Validation failed | `422` + the form again | Field-level message beside each offending field, all typed input preserved | FR-007, FR-022 |
| Card no longer exists | `404` + the board | "Esta tarefa não existe mais." The card is never recreated | FR-012 |
| Revision stale | `409` + the board | "Esta tarefa foi alterada em outra aba. Recarregue para ver a versão atual." | research R14 |
| Board full | `409` + the form again | "O quadro está cheio. Exclua uma tarefa para adicionar outra." | research R11 |
| Rate limit exceeded | `429` + the board | "Muitas alterações de uma vez. Tente novamente em instantes." | research R11 |
| Missing or invalid CSRF token | `403` + a plain page | A short message and a link back to the board | research R6 |
| Unexpected failure | `500` + a plain page | A short apology and a link back. No stack trace, no SQL, no identifiers | FR-024, security CHK027 |

Non-field messages appear in one consistent region at the top of the board, distinct from
field-level messages (ux CHK004).

---

## Rendering guarantees

These are contract-level because they are what the spec's safety and accessibility requirements
depend on:

1. **Escaping** — all card content renders through Thymeleaf's escaping text operator. `th:utext`
   is forbidden anywhere card content is involved, so markup typed by a person is shown literally
   (FR-023, SC-009).
2. **Labels** — every input has a `<label for>`; the appearance control's accessible name states
   the current state and the next (FR-029, FR-020a).
3. **Focus** — after a redirect the moved or saved card is the fragment target, so the browser
   returns focus into the right region without scripting (ux CHK007).
4. **Touch targets** — every button and link in a card renders at 44×44 CSS px minimum (FR-028).
5. **Layout** — columns stack vertically below 768 px as `<details>` sections and form a
   four-column grid above it; no horizontal page scrolling at 320 px (FR-027, research R13).
6. **Theme** — no preference renders no `data-theme` attribute, leaving `prefers-color-scheme` in
   charge; an explicit choice renders `data-theme="light|dark"` on `<html>` (research R10).
7. **Headers** — `X-Frame-Options: DENY` and a `default-src 'self'` Content-Security-Policy with no
   inline script on every response (research R6).
