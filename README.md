# todo-project — Kanban Todo Board

A security-first, hexagonal Spring Boot monolith: one shared kanban board, four fixed columns
(Backlog, Em andamento, Pausado, Concluído), server-rendered with Thymeleaf, with a REST API over
the same application core.

See [specs/001-kanban-todo-board/](specs/001-kanban-todo-board/) for the full specification, plan,
research decisions, data model and API contracts. This file covers only what you need to run it.

## Before you run it — read this

**There is one board and no login.** Anyone who can reach this application's port sees and can
modify or delete every card. That is an explicit, accepted decision for this stage (see
[spec.md FR-026a/FR-026b](specs/001-kanban-todo-board/spec.md)), not an oversight — the application
binds to `127.0.0.1` by default for exactly this reason. **Do not change `server.address` or
otherwise expose this port on a network without first adding accounts.**

**The board is empty after every restart by default.** The datasource is H2 in memory
(`jdbc:h2:mem:todo`). Run with the `durable` profile for a board that survives restarts — see
below.

## Running it

Requires JDK 21 and Maven.

```bash
mvn spring-boot:run
```

Open <http://localhost:8080>.

For a board that survives an application restart:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=durable
```

This only changes where H2 stores its data (`./data/todo`, excluded from version control) — no
code differs between the two profiles, because persistence sits behind an outbound port.

## Testing

```bash
mvn test
```

The `HexagonalArchitectureTest` suite fails the build if the domain or application layer ever
comes to depend on Spring, JPA, Bean Validation, the servlet API, or an adapter — that gate is
what keeps the architecture honest over time, not a convention.

## REST API

A machine-consumable surface over the same use cases the HTML board uses — see
[contracts/rest-api.yaml](specs/001-kanban-todo-board/contracts/rest-api.yaml). No authentication,
same single shared board, no CSRF token required (that surface is stateless and cookie-less).
Every write to an existing card requires an `If-Match` header carrying the revision you last read.

```bash
curl -s localhost:8080/api/board | jq
curl -si -X POST localhost:8080/api/cards -H 'Content-Type: application/json' -d '{"title":"Comprar pão"}'
```

## Architecture

Ports and adapters (hexagonal), per [constitution.md](.specify/memory/constitution.md):

```
domain/        — framework-free aggregate, value objects, the allowlist text policy
application/   — use cases (inbound ports) and their implementations, outbound ports
adapter/in/    — web (Thymeleaf, no-scripting-required) and rest (JSON) — siblings, same ports
adapter/out/   — persistence (JPA, confined to its own package), time, rate limiting
config/        — Spring wiring only; no business rules live here
```

Full rationale for each design decision lives in
[research.md](specs/001-kanban-todo-board/research.md) (R1–R15).

## Known gaps in this build

This implementation was written and reviewed without a local JDK/Maven available to compile or
run it — see the session's final report for the full list of what that means in practice. Before
relying on this code, run `mvn test` yourself and treat any failure as expected first-pass friction
to fix, not a sign the design is wrong.
