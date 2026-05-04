# Claude — tchalanet-server

Scope:

- Backend only.

Read first:

- This file.
- The closest slice `CLAUDE.md`.
- The files explicitly listed in the task.

Do not:

- Scan all backend packages.
- Modify web, mobile, infra, or edge-service unless explicitly requested.
- Load all convention docs.
- Create new abstractions when local patterns exist.

Architecture:

- `common`: technical only, no business logic.
- `catalog`: reference/read-mostly, no lifecycle, no business events.
- `core`: business domains, CQRS/hexagonal.
- `features`: UI/BFF orchestration, no business truth.

Backend rules:

- Java 25 / Spring Boot 4.x / Maven.
- Constructor injection only.
- Controllers are thin.
- Use CommandBus / QueryBus.
- Commands and queries are records.
- Write handlers use `@TchTx`.
- Side-effects use `AfterCommit.run(...)`.
- Use typed IDs outside persistence.
- Raw UUID only in JPA/JDBC/Flyway.
- Persistence contains no business logic.
- RLS handles tenant isolation.
- Do not filter tenant manually in read-side queries.

Testing:

- JUnit 5 + AssertJ only.
- Prefer in-memory ports/fakes over heavy mocks.
- Add focused tests near changed code.

Useful conventions, load only when relevant:

- Batch/scheduler: `docs/conventions/batch.md`, `spring_batch_6.md`
- RLS/context: `docs/conventions/rls.md`, `request_context_usage.md`
- Cache: `docs/conventions/cache.md`
- Events: `docs/conventions/event_model.md`
- Web API: `docs/conventions/web_api.md`, `api_response.md`
- Persistence: `docs/conventions/persistence.md`, `typed_ids.md`
