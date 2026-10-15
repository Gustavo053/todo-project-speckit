# Feature Specification: Kanban Todo Board

**Feature Branch**: `001-kanban-todo-board` *(spec directory name; the repository is not yet a git repository, so no branch exists)*

**Created**: 2026-10-14

**Status**: Draft — both clarifications resolved in [research.md](./research.md) (R1, R2)

**Input**: User description: "Crie uma aplicação de TODO para cadastro de tarefas do dia a dia. Essa aplicação deve possobilitar o cadastro de anotações. Essas anotações devem ser movidas pelo kanban, entre as colunas backlog, em andamento, pausado e concluído. O design deve ser moderno e agradável, contendo os temas ligtht e dark. A aplicação deve possibilitar remover, editar ou adicionar novas tarefas ou anotações. Sem login para essa primeira etapa."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Capture a task on the board (Priority: P1)

A person opens the application and immediately sees their board. They type what they need to do,
optionally add a longer note with details, and save. The new card appears in the Backlog column
and is still there when they come back later.

**Why this priority**: Without capture there is no board. This is the smallest slice that
delivers standalone value — a visible, persistent list of what needs doing — and everything else
operates on the cards it produces.

**Independent Test**: Open the application, add three cards with titles (one with a note), reload
the page, and confirm all three are present in Backlog with their content intact. Delivers a
working personal task list even if no other story ships.

**Acceptance Scenarios**:

1. **Given** an empty board, **When** the person submits a card titled "Pagar conta de luz",
   **Then** a card with that title appears in the Backlog column and the Backlog count becomes 1.
2. **Given** the add form is open, **When** the person submits it with the title field blank,
   **Then** no card is created and a message next to the title field states that a title is
   required, with any text already typed in the note field preserved.
3. **Given** a card was created with a note, **When** the person reloads the page, **Then** the
   card and its note are still shown unchanged.
4. **Given** the person types a title containing `<script>alert(1)</script>`, **When** the card is
   displayed, **Then** that text is shown literally as typed and nothing is executed.

---

### User Story 2 - Move a task across the kanban columns (Priority: P2)

As work progresses, the person moves a card from Backlog to Em andamento, parks it in Pausado
when blocked, and drops it into Concluído when finished. They can also move a card backwards when
something turns out not to be done.

**Why this priority**: This is what makes the list a kanban rather than a flat list, and it is the
headline request. It depends on cards existing (P1) but nothing else depends on it.

**Independent Test**: With cards present in Backlog, move one card through all four columns in
order, then move it back to Backlog, reloading the page after each move to confirm the new column
persisted.

**Acceptance Scenarios**:

1. **Given** a card in Backlog, **When** the person moves it to Em andamento, **Then** it appears
   only in Em andamento, the two column counts update, and the change survives a page reload.
2. **Given** a card in Concluído, **When** the person moves it back to Backlog, **Then** the move
   succeeds — no column is one-way.
3. **Given** a card in Pausado, **When** it is moved, **Then** its title, note, and creation time
   are unchanged and only its column and last-updated time differ.
4. **Given** the person is on a 320 px wide screen, **When** they move a card, **Then** the move
   is completable without drag-and-drop and without horizontal page scrolling.

---

### User Story 3 - Edit an existing task (Priority: P3)

The person realises a card's wording is wrong or its note needs more detail. They open the card,
change the title or note, and save. If they change their mind they cancel and nothing is altered.

**Why this priority**: Captured text is frequently wrong or incomplete, but a person can work
around it by deleting and re-adding, so it ranks below capture and movement.

**Independent Test**: Edit a card's title, save, and confirm the board shows the new title after
reload; then start another edit, cancel it, and confirm the card is unchanged.

**Acceptance Scenarios**:

1. **Given** a card titled "Comprar pão", **When** the person edits the title to "Comprar pão e
   leite" and saves, **Then** the board shows the new title and the card stays in its current
   column.
2. **Given** an open edit form, **When** the person clears the title and saves, **Then** the card
   is not modified and a message states that a title is required.
3. **Given** an open edit form with unsaved changes, **When** the person cancels, **Then** the
   card keeps its previous title and note.

---

### User Story 4 - Remove a task (Priority: P4)

The person has a card that is no longer relevant. They delete it and it disappears from the board
for good — but not by accident, because the application asks them to confirm first.

**Why this priority**: Needed to keep the board trustworthy over time, but a stale card is an
annoyance rather than a blocker, and deletion is the most destructive action, so it ships after
the constructive ones.

**Independent Test**: Delete a card, confirm the prompt, verify it is gone after reload; then
start a deletion and decline the confirmation, verifying the card remains.

**Acceptance Scenarios**:

1. **Given** a card on the board, **When** the person deletes it and confirms, **Then** it is
   removed from the board, the column count decreases, and it does not return after a reload.
2. **Given** a delete confirmation is being shown, **When** the person declines it, **Then** the
   card remains on the board unchanged.
3. **Given** the last card in a column is deleted, **When** the board redraws, **Then** the column
   is still shown with its empty-state message.

---

### User Story 5 - Switch between light and dark appearance (Priority: P5)

The person prefers a dark interface at night. The application already matches their device
preference when they first arrive, and they can override it with a visible control; the choice is
remembered on their next visit.

**Why this priority**: Explicitly requested and affects comfort on every use, but the board is
fully functional without it.

**Independent Test**: Load the application with the device set to dark, confirm the dark
appearance; switch to light, reload, and confirm light persisted; verify text remains readable in
both.

**Acceptance Scenarios**:

1. **Given** a visitor whose device requests a dark appearance, **When** they open the application
   for the first time, **Then** the dark theme is applied without any action from them.
2. **Given** the dark theme is active, **When** the person chooses light, **Then** the interface
   switches immediately and is still light after a reload and after closing and reopening the
   browser.
3. **Given** either theme is active, **When** any screen is inspected, **Then** all text meets a
   contrast ratio of at least 4.5:1 against its background.

---

### Edge Cases

- A title that is only whitespace is submitted → treated as empty and rejected with a field-level
  message; nothing is saved.
- A title or note longer than the allowed length is submitted → rejected with a message stating
  the limit and the current length; the typed content is not silently truncated.
- Card text contains markup, script tags, or template syntax → rendered as literal text, never
  interpreted.
- A single unbroken word longer than the card width (e.g. a long URL) → wraps or truncates
  visibly; it never pushes the board into horizontal scrolling at 320 px.
- A column holds 100+ cards → the column scrolls within the layout and remains navigable on a
  phone; the rest of the board stays usable.
- All four columns are empty → each column still shows its heading, a zero count, and an
  empty-state message that explains how to add a card.
- The same board is open in two tabs and a card is moved in one, then edited in the other → the
  second action either applies to the current state or fails with a message telling the person to
  refresh; it MUST NOT silently resurrect the card's earlier column.
- A card is deleted in one tab and then edited in another → the edit fails with a clear "this card
  no longer exists" message rather than recreating it.
- Client-side scripting is unavailable in the browser → adding, moving, editing, deleting, and
  switching theme all still work through ordinary page navigation and form submission.
- An action references a card that does not exist → the person sees a plain "not found" message
  with no internal identifiers, stack traces, or storage details.
- The device requests no appearance preference → the light theme is used as the default.

## Requirements *(mandatory)*

### Functional Requirements

**Board and columns**

- **FR-001**: System MUST present a single kanban board with exactly four columns in this fixed
  order: Backlog, Em andamento, Pausado, Concluído.
- **FR-002**: The four columns MUST be fixed — users MUST NOT be able to add, rename, reorder, or
  remove columns.
- **FR-003**: Each column MUST display its name and the number of cards it currently holds.
- **FR-004**: An empty column MUST display an empty-state message that tells the person how to add
  a card.

**Creating cards**

- **FR-005**: Users MUST be able to create a card consisting of a required short title and an
  optional longer note. A note is a field on the card, not a separate item — there is exactly one
  kind of card, and "tarefa" and "anotação" refer to the same thing (see research R1).
- **FR-005a**: A card's face MUST show its full title and, when a note exists, a preview of the
  note clamped to two lines together with an indication that more text exists. The full note MUST
  be visible on the edit screen.
- **FR-006**: A newly created card MUST be placed in the column from which the add action was
  started, defaulting to Backlog.
- **FR-007**: System MUST reject a card whose title is empty or whitespace only, show a message
  next to the title field, preserve the person's other typed input, and create nothing.
- **FR-008**: System MUST reject a title longer than 120 characters or a note longer than 2000
  characters, stating the limit, and MUST NOT truncate silently.

**Moving cards**

- **FR-009**: Users MUST be able to move any card from its current column to any other column, in
  either direction, with no forbidden transitions.
- **FR-010**: Moving a card MUST preserve its title, note, and creation time, and MUST update its
  last-changed time.
- **FR-011**: Every move MUST be achievable through an explicit, always-visible control. Pointer
  drag-and-drop is out of scope for this stage (see research R7), so the explicit control is the
  only move mechanism.
- **FR-012**: A move that targets a card that no longer exists MUST fail with a plain message and
  MUST NOT recreate the card.

**Editing cards**

- **FR-013**: Users MUST be able to change the title and note of an existing card, subject to the
  same validation rules as creation (FR-007, FR-008).
- **FR-014**: Cancelling an edit MUST leave the card exactly as it was.
- **FR-015**: Editing a card MUST NOT change the column it is in.

**Removing cards**

- **FR-016**: Users MUST be able to delete a card.
- **FR-017**: Deletion MUST require an explicit confirmation step and MUST be permanent — there is
  no undo and no archive in this stage.

**Appearance**

- **FR-018**: System MUST provide a light theme and a dark theme, both covering every screen and
  every state, including error messages and empty states.
- **FR-019**: The appearance preference MUST have exactly three states: follow-device (the
  default), light, and dark. On a first visit, follow-device MUST apply the appearance the
  visitor's device requests, falling back to light when no preference is expressed.
- **FR-020**: Users MUST be able to change the appearance preference at any time through a control
  present on every screen, and MUST be able to reach all three states from it — including
  returning to follow-device after an explicit choice. The chosen state MUST persist across visits.
- **FR-020a**: The appearance control MUST carry an accessible name that states the current state
  and what choosing it will change to.
- **FR-021**: Text and interactive controls in both themes MUST meet a contrast ratio of at least
  4.5:1 against their background.

**Input handling and safety**

- **FR-022**: System MUST validate every user-supplied value before storing it, reject invalid
  input with field-level feedback, and MUST NOT save any part of a rejected submission.
- **FR-023**: System MUST render all card content as literal text so that markup, scripts, or
  template syntax typed by the person are never interpreted or executed.
- **FR-024**: Messages shown to the person MUST NOT reveal internal details such as stack traces,
  query text, or storage identifiers.

**Access and persistence**

- **FR-025**: System MUST NOT require login, registration, or any personal data in this stage.
- **FR-026**: Cards, their column, and the appearance preference MUST survive page reloads and
  browser restarts.
- **FR-026a**: There is exactly one shared board. Every visitor who can reach the application sees
  and may modify or delete every card; there is no privacy boundary inside the application (see
  research R2). This is acceptable only because this stage is a local, single-user deployment, so
  the application MUST bind to the local machine by default.
- **FR-026b**: Making the application reachable beyond the local machine is out of scope for this
  stage and MUST NOT be done without first specifying accounts, because it would expose one
  person's card text to anyone with the address.

**Usability on small screens and assistive technology**

- **FR-027**: The whole board MUST be usable at 320 CSS px wide with no horizontal page scrolling,
  and all four columns MUST be reachable on that width.
- **FR-028**: Every interactive control MUST present a touch target of at least 44x44 CSS px.
- **FR-029**: Adding, moving, editing, deleting, and switching theme MUST all be completable using
  the keyboard alone, and every input MUST have a visible, associated label.
- **FR-030**: All core actions MUST remain completable when client-side scripting is unavailable;
  scripting MAY only improve the experience, never enable it.

### Key Entities *(include if feature involves data)*

- **Task Card**: one thing the person intends to do, and the only kind of item on the board. Holds
  a required short title, an optional longer note, the column it currently sits in, when it was
  created, when it last changed, and a revision marker used to refuse writes based on stale data.
- **Column (board stage)**: one of exactly four fixed stages — Backlog, Em andamento, Pausado,
  Concluído — with a fixed display order. Not created or edited by users. Holds zero or more cards.
- **Appearance Preference**: the chosen appearance — follow-device, light, or dark — remembered
  between visits.
- **Board**: the whole collection of cards across the four columns. There is exactly one board,
  shared by everyone who can reach the application (FR-026a).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can add their first card within 30 seconds of the board
  appearing, with no instructions or onboarding.
- **SC-002**: Moving a card to another column takes no more than two interactions, and the board
  shows the card in its new column within one second of the person confirming the move.
- **SC-003**: In unmoderated testing, at least 90% of first-time users move a card between two
  columns successfully on their first attempt, without help.
- **SC-004**: The entire board is operable on a 320 px wide screen with zero horizontal page
  scrolling and zero clipped or unreachable controls.
- **SC-005**: 100% of invalid submissions produce a visible message attached to the offending
  field, and zero invalid submissions result in stored data or lost already-typed content.
- **SC-006**: All six core actions (add, view, move, edit, delete, switch theme) complete
  successfully with client-side scripting disabled.
- **SC-007**: An accessibility audit of every screen in both themes reports zero contrast
  failures below 4.5:1 and zero unlabelled inputs.
- **SC-008**: 100% of created cards, their columns, and the theme choice are still correct after a
  page reload and after closing and reopening the browser.
- **SC-009**: Zero instances of user-entered markup or script being executed, verified by a test
  suite that submits scripting payloads into every text field.
- **SC-010**: A board holding 100 cards appears ready to use in under 2 seconds on a mid-range
  phone over a 3G-class connection, and remains scrollable without stutter.

## Assumptions

- **Language**: the interface is in Brazilian Portuguese, with the column labels shown as
  "Backlog", "Em andamento", "Pausado" and "Concluído", matching the wording in the request. This
  specification is written in English to stay consistent with the project's other Spec Kit
  artifacts.
- **Scale**: personal day-to-day use — tens to a few hundred cards on one board, not a team
  workload.
- **Field limits**: a 120-character title and a 2000-character note are reasonable defaults chosen
  here; they were not specified in the request.
- **Card order within a column**: cards are listed most-recently-changed first. Manual reordering
  inside a column is not part of this stage.
- **Confirmation**: only deletion is confirmed. Creating, moving, and editing apply immediately.
- **"Modern and pleasant" design** is interpreted as a clean, uncluttered layout with consistent
  spacing, legible type, clear column separation, and both themes meeting the contrast requirement
  in FR-021 — not as adherence to any particular brand or design system.
- **Browsers**: current versions of evergreen browsers. Per the project constitution, the board
  also works without client-side scripting (FR-030).
- **No accounts means no personal data**: the application collects no names, emails, or other
  identifying information in this stage.
- **Dependencies**: none external. No integration with calendars, notification services, or any
  third-party system is required.

## Out of Scope

Deliberately excluded from this first stage, to keep the slice small:

- Login, registration, accounts, and any form of sharing or collaboration.
- Due dates, reminders, notifications, priorities, labels, tags, colours, and attachments.
- Subtasks, checklists inside a card, and card-to-card relationships.
- Search, filtering, sorting controls, and archiving.
- Manual reordering of cards within a column.
- Drag-and-drop card movement (research R7).
- Custom or user-defined columns.
- Undo, trash, history, and audit trails.
- Importing from or exporting to other tools, and offline use.
