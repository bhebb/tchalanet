# Copilot – Coding Rules for Tchalanet Backend

> **Mandatory**  
> Copilot MUST follow these rules when generating backend code for this repository.

---

## 1. Architecture Rules

- Use **Hexagonal Architecture + CQRS** exactly as described in `ARCHITECTURE.md`.
- `domain` = pure business model (no Spring, no JPA, no WebClient).
- `application` = use cases (commands/queries + ports).
- `infra` = adapters (JPA, Web, WebClient, batch, config).

**Never put ports in `domain`.**  
Ports MUST live in `application.port.in` and `application.port.out`.

---

## 2. CQRS Rules

### Commands

- Must be Java `record`s.
- Names: `CreateDrawCommand`, `RecordAuditEventCommand`, …
- Handlers: Direct implementations of `CommandHandler<CreateDrawCommand, UUID>` or `VoidCommandHandler<ArchiveDrawCommand>` for void.
- Implementations: `CreateDrawCommandHandler`, `RecordAuditEventCommandHandler`.

### Queries

- Must be Java `record`s.
- Must have no side effects.
- Handlers: Direct implementations of `QueryHandler<ListUpcomingDrawsQuery, List<UpcomingDrawDto>>`.
- Implementations: `ListUpcomingDrawsHandler`.

---

## 3. Ports

Place ports in:

```text
<bc>/application/port/out
```

Do NOT generate:

- `domain.ports.in`
- `domain.ports.out`

---

## 4. Domain Model

- No annotations in `domain.model`.
- No Spring / JPA imports.
- Entities must enforce invariants internally.
- Domain exceptions in `domain.exception`.

---

## 5. Infra

- JPA entities + repositories in `infra.persistence`.
- Adapters implementing ports in `infra.persistence` / `infra.external`.
- Controllers in `infra.web`, kept thin:
  - Map HTTP → Commands/Queries → DTOs.
  - No business logic.
  - Inject concrete handler classes directly.

---

## 6. Multi-Tenancy

- Always include `tenantId` in commands/queries where relevant.
- All persistence adapters must filter by `tenant_id`.
- Assume PostgreSQL RLS is enabled.

---

## 7. Logging

- Use SLF4J logging (`log.info`, `log.warn`, etc.).
- Do NOT use `System.out.println` or `e.printStackTrace()`.

---

## 8. References for Architecture

When in doubt, follow these references and the existing code in `accesscontrol`, `audit`, `draw`, `ticket`:

- Hexagonal Architecture — Alistair Cockburn  
  https://alistair.cockburn.us/hexagonal-architecture/

- Clean Architecture — Robert C. Martin  
  https://www.oreilly.com/library/view/clean-architecture/9780134494272/

- CQRS Documents — Greg Young  
  https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf

- Domain-Driven Design — Eric Evans  
  https://domainlanguage.com/

---

## 9. Violations

If generated code violates this document or `ARCHITECTURE.md` or `CODE_STYLE.md`,  
it MUST be corrected to follow:

1. `ARCHITECTURE.md`
2. `CODE_STYLE.md`
3. This `.copilot/README.md`
