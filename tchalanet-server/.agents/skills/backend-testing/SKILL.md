# Skill — Backend Testing for Tchalanet Server

Use this skill only for `tchalanet-server` test tasks.

For specialized concerns, load extras:

- `idempotency.md` — idempotent endpoint testing
- `pos-context.md` — POS operational context rules
- `events.md` — event publishing and projector testing

## Non-negotiable Tchalanet rules

Backend tests must respect:

- controllers are thin: validation, context, mapping, security/audit metadata, bus dispatch, response mapping;
- no business logic in controllers;
- writes use CommandBus and handlers;
- reads use QueryBus and handlers;
- typed IDs outside persistence;
- tenant comes from TchRequestContext/RLS, not client body;
- 2xx JSON returns ApiResponse<T>;
- 4xx/5xx errors return ProblemDetail;
- writes that are sensitive are audited;
- events and side-effects are after-commit;
- projectors/listeners are idempotent;
- POS sell/payout/offline operations require trusted operational context.

## Unit tests

Prefer direct handler/domain tests for business rules.

Good targets:

- domain policies: cutoff, payout eligibility, limit exposure, receipt formatting decisions;
- command handlers with fake ports;
- query handlers with fake readers;
- application orchestrators and assemblers;
- mappers when the mapping is contract-sensitive.

Do not use Spring unless the test is about Spring wiring.

Checklist:

- JUnit 5;
- AssertJ;
- `@Nested` for scenarios;
- fake ports/builders instead of real repositories;
- deterministic Clock when time matters;
- no raw UUID outside persistence fixtures.

## Controller tests

Use targeted MVC/security tests when checking web behavior.

Verify:

- route and HTTP method;
- request validation;
- `@PreAuthorize`/security deny;
- `@AuditLog` exists for sensitive writes, if inspectable;
- typed ID conversion;
- mapping request to command/query;
- success is ApiResponse;
- errors are ProblemDetail;
- pagination uses TchPageRequest/TchPage when list endpoint.

Do not verify domain rules here. Mock bus or use a minimal test bus.

## Persistence/RLS integration tests

Use Testcontainers only when the DB behavior is the point.

Verify:

- Flyway migration applies;
- JPA/JDBC adapter maps UUID <-> typed IDs;
- soft delete and indexes when relevant;
- RLS blocks cross-tenant reads;
- tenant_id is inserted from context;
- no application-side tenant filter is introduced where RLS is the rule.

## Bus integration tests

Use real CommandBus/QueryBus for:

- handler registration;
- duplicate/missing handler behavior;
- execute/ask names, not legacy send/handle;
- one message type => exactly one handler.

## Maven commands

Targeted module:

```bash
./mvnw -pl tchalanet-core -am test
./mvnw -pl tchalanet-app -am test
```

Full PR verification:

```bash
./mvnw clean verify
```

Never claim tests pass without naming the command executed.

## Do NOT test

- Handlers by bypassing CommandBus (use `bus.execute()` instead).
- Repositories directly when RLS is the point (use integration test with Testcontainers).
- POS flows without operational context (it's a non-negotiable).
- Backend business rules duplicated in controller tests (mock the bus).
- Framework auto-configuration or Spring internals.
