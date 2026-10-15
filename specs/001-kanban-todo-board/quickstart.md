# Quickstart & Validation Guide: Kanban Todo Board

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

How to run the application and prove the feature works end to end. Scenarios map to the spec's
user stories and success criteria, so this doubles as the acceptance walkthrough.

---

## Prerequisites

| Need | Version | Check |
|---|---|---|
| JDK | 21 (LTS) | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Browser | any current version | — |

No database to install — H2 runs inside the application (research R3).

---

## Run

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080>.

The application binds to localhost only. That is deliberate: there is **one shared board and no
login**, so anyone who can reach the port can read and delete every card (spec FR-026a). Do not
expose the port without first specifying accounts (spec FR-026b).

**The board is empty after every restart.** The default profile uses H2 in memory, as requested.
For a board that survives restarts, run with the durable profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=durable
```

That switches the datasource to a file under `./data/` and changes nothing else — persistence sits
behind a port, so no code differs (research R3).

---

## Test

```bash
mvn test                                   # everything
mvn test -Dtest='Arch*Test'                # dependency direction only (constitution II)
mvn test -Dtest='*ValidationTest'          # input validation, including rejection cases
```

The architecture tests are the ones to watch: they fail the build if anything in `domain` or
`application` touches Spring, JPA, Bean Validation, the servlet API, or an adapter (research R9).

---

## Validation scenarios

Each scenario is a manual walkthrough with an explicit expected outcome. The parenthetical
references are the requirement each one proves.

### 1. Capture a card — US1, SC-001

1. Open <http://localhost:8080>. Four columns appear: Backlog, Em andamento, Pausado, Concluído,
   each showing a count of 0 and an empty-state message (FR-001, FR-003, FR-004).
2. Add a card titled `Pagar conta de luz` with the note `Vence dia 20.`
3. **Expect**: the card appears in Backlog, the Backlog count reads 1, and the card shows the full
   title plus a two-line note preview (FR-005, FR-005a, FR-006).
4. Reload. **Expect**: still there, unchanged (FR-026).

### 2. Reject bad input without losing work — US1, FR-007, FR-022

1. Open the add form, leave the title blank, type something in the note, submit.
2. **Expect**: no card created; a message beside the title field; the note text still in the field;
   HTTP status 422 (FR-007).
3. Submit a title of 121 characters. **Expect**: rejected with the limit stated, nothing truncated
   silently (FR-008).
4. Submit a title of `   ` (spaces only). **Expect**: treated as empty and rejected (FR-007).
5. Submit a title containing a zero-width space. **Expect**: rejected as a forbidden character
   (research R5).

### 3. Content is never executed — SC-009, FR-023

1. Create a card titled `<script>alert(1)</script>` and another with the note
   `<img src=x onerror=alert(1)>`.
2. **Expect**: both render as literal text. No dialog, no broken image, nothing executed.
3. View source. **Expect**: the angle brackets appear escaped.

### 4. Move through every column — US2, FR-009, FR-010

1. On a Backlog card, use the move controls to send it to Em andamento, then Pausado, then
   Concluído, then back to Backlog.
2. **Expect**: every move succeeds; counts update on both affected columns; the title, note and
   creation time are unchanged throughout (FR-010); no column is one-way (FR-009).
3. Reload after a move. **Expect**: the new column persisted.

### 5. Edit and cancel — US3, FR-013, FR-014, FR-015

1. Edit a card's title, save. **Expect**: new title on the board, same column (FR-015).
2. Open the edit form again, change the note, cancel. **Expect**: nothing changed (FR-014).
3. Open the edit form, clear the title, save. **Expect**: rejected, card untouched.

### 6. Delete with confirmation — US4, FR-016, FR-017

1. Delete a card. **Expect**: a confirmation page that names the card (ux CHK003).
2. Decline. **Expect**: the card is still on the board.
3. Delete and confirm. **Expect**: gone, count decreased, still gone after reload.
4. Delete the last card in a column. **Expect**: the column remains with its empty-state message.

### 7. Appearance, all three states — US5, FR-019, FR-020

1. Set the operating system to dark, open the application fresh (no cookie).
   **Expect**: dark applied with no action (FR-019).
2. Use the appearance control to choose light. **Expect**: switches immediately; still light after
   a reload and after closing and reopening the browser (FR-020).
3. Continue cycling until the control returns to follow-device. **Expect**: the appearance follows
   the operating system again — this is reachable, not a dead end (FR-019, research R10).
4. Check the control's accessible name in each state (FR-020a).

### 8. Works with scripting disabled — SC-006, FR-030, constitution IV

1. Disable JavaScript in the browser.
2. Run scenarios 1, 4, 5, 6 and 7 again.
3. **Expect**: all of them complete. This is the acceptance bar, not a degraded mode.

### 9. Mobile at 320 px — SC-004, FR-027, FR-028

1. Set the viewport to 320×568.
2. **Expect**: columns stacked vertically as collapsible sections, no horizontal page scrolling,
   every control at least 44×44 px, no clipped text (research R13).
3. Create a card with a 120-character title and one containing a very long unbroken URL.
   **Expect**: both wrap or clamp; neither widens the page.
4. Zoom to 200%. **Expect**: layout still usable (ux CHK010).

### 10. Two tabs — research R14, security CHK029

1. Open the board in two tabs. In tab A, move a card to Concluído.
2. In tab B (still showing the old state), edit that same card and save.
3. **Expect**: refused with "esta tarefa foi alterada em outra aba", status 409. The card stays in
   Concluído — the stale write does not resurrect its old column.
4. In tab A delete a card; in tab B edit it. **Expect**: not found, and the card is **not**
   recreated (FR-012).

### 11. Limits — research R11

1. Create cards until the cap (default 500, or lower `todo.board.max-cards` to test quickly).
   **Expect**: a clear "board is full" message, not a server error.
2. Submit writes faster than 60/minute. **Expect**: a "try again in a moment" message, status 429.

### 12. Errors leak nothing — FR-024, security CHK027

1. Request `/cards/not-a-uuid/edit`. **Expect**: a plain not-found page — no stack trace, no SQL,
   no class names.
2. Submit a form with the CSRF token removed. **Expect**: refused (research R6).

### 13. REST surface — contracts/rest-api.yaml

```bash
# Read the board
curl -s localhost:8080/api/board | jq

# Create, capturing the revision from the ETag
curl -si -X POST localhost:8080/api/cards \
  -H 'Content-Type: application/json' \
  -d '{"title":"Comprar pão"}'

# Move it (If-Match carries the revision you last read)
curl -s -X PUT localhost:8080/api/cards/$ID/stage \
  -H 'Content-Type: application/json' -H 'If-Match: "0"' \
  -d '{"stage":"IN_PROGRESS"}' | jq

# Stale write is refused
curl -si -X DELETE localhost:8080/api/cards/$ID -H 'If-Match: "0"'   # expect 412

# Validation failure returns problem+json, not a stack trace
curl -s -X POST localhost:8080/api/cards \
  -H 'Content-Type: application/json' -d '{"title":"   "}' | jq      # expect 400
```

**Expect**: `GET /api/board` always returns all four columns including empty ones; stage codes are
the English constants; errors arrive as `application/problem+json` with a field-level `errors`
array and no internal detail.

### 14. Same rules on both surfaces — research R8, R11

1. Create a card via the REST API. **Expect**: it appears on the HTML board after a reload.
2. Exhaust the write rate limit through the REST API, then try an HTML form.
   **Expect**: also refused — the limit lives in the application core, not in one adapter.

---

## What "done" looks like

The feature is accepted when scenarios 1–14 pass, `mvn test` is green including the architecture
tests, and the items in [checklists/requirements.md](./checklists/requirements.md) are all checked.
The remaining reviewer-owned items in [checklists/ux.md](./checklists/ux.md) and
[checklists/security.md](./checklists/security.md) are judgement calls for the reviewer, not gates
this guide can settle.
