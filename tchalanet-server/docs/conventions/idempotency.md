# Idempotency

## Goal

Prevent duplicate side effects (double ticket sales) when clients retry requests (timeouts, offline sync, double-click).

## Headers

- `X-Request-Id` (optional; server generates if missing; echoed in logs)
- `Idempotency-Key` (required on selected endpoints)

## When it is REQUIRED

- `POST /api/v1/tenant/tickets` (SellTicket) -> REQUIRED
- Other endpoints: optional (state transitions already protected by state machine)

## Core rules

- One client intent -> one server resource.
- Same `(tenant_id, scope, key)` + same `request_hash` -> replay same result (same ticket).
- Same `(tenant_id, scope, key)` + different `request_hash` -> `409 idempotency.payload_mismatch`.
- Missing key when required -> `400 idempotency.missing`.
- In progress -> `409 idempotency.in_progress` (client should retry with backoff).

## Scope

`IdempotencyScope` is an enum (stored as STRING in DB). Example: `SALES_SELL_TICKET`.

## Request hash

- `request_hash = sha256(normalized_json(body))`
- Normalization:
  - sort object keys recursively
  - keep array order (arrays are business-significant)

## Storage

Table: `idempotency_record` (tenant-scoped, RLS)

- Unique: `UNIQUE(tenant_id, scope, idem_key)`
- Fields: scope, idem_key, request_hash, status, resource_id, response_json(optional), expires_at, audit fields

## Lifecycle

- `begin(...)` inserts `IN_PROGRESS`
- On success: `complete(...)` -> `COMPLETED` + `resource_id` (+ `response_json` optional)
- Cleanup: purge expired rows via daily batch job

## HTTP pipeline integration

- `TchContextFilter` extracts `Idempotency-Key` into `TchRequestContext`
- `@RequireIdempotency(scope=...)` aspect applies begin/decision logic
- Application handler completes the record after creating the resource

## Notes

- Client MUST reuse the same `Idempotency-Key` for retries of the same request.
- Client MUST generate a new key if it changes the request payload.

---

# Event idempotence & projectors (new)

This project uses a lightweight, common pattern to guarantee idempotence of projectors / consumers (events processed AFTER_COMMIT or via outbox). Add the following rules to your handlers and adapters.

## 1) HTTP idempotency (summary)

- Used by write endpoints (e.g. SellTicket).
- Key: tuple `(tenant, scope, idem_key)` (tenant comes from context / RLS, scope = IdempotencyScope enum, idem_key = client-provided key).
- Server computes `request_hash` (sha256 of normalized payload) and stores it.
- On replay with same key and same hash → return same response.
- On replay with same key and different hash → return `409 idempotency.payload_mismatch`.
- `TTL` for idempotency records: required (expires_at must be set).
- Errors use the `idempotency.*` error codes (e.g. `idempotency.missing`, `idempotency.in_progress`, `idempotency.payload_mismatch`).

## 2) Event idempotence (projectors / consumers)

- Used by all projectors / event consumers that apply read-model updates (exposure, stats, projections triggered AFTER_COMMIT / outbox).
- Key: tuple `(tenant, handler_key, event_id)`.
  - `handler_key` is a stable constant in the handler code identifying the projector (format convention below).
  - `event_id` is the event unique id (UUID) from the Domain Event.
- Behavior:
  - `alreadyProcessed(handler_key, eventId)` → if true: skip (no-op), do not treat as error.
  - Otherwise: apply projector logic and then `markProcessed(handler_key, eventId)`.
- No client errors should be raised for duplicate events — handlers must be idempotent and return silently when event already processed.
- `TTL` for processed_event is optional (you may keep records for audit or purge old ones via a scheduled job).

### DDL suggestion (Flyway)

Create a migration, e.g. `V16__processed_event.sql`:

```sql
CREATE TABLE processed_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  handler_key varchar(96) NOT NULL,
  event_id uuid NOT NULL,
  processed_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  CONSTRAINT uq_processed_event UNIQUE (tenant_id, handler_key, event_id)
);

CREATE INDEX idx_processed_event_lookup
  ON processed_event (handler_key, event_id);
```

> Note: RLS must enforce tenant isolation; do NOT add tenant filter to queries in Java code.

## 3) RLS rule (non negotiable)

- NEVER add `WHERE tenant_id = ?` in application/read queries.
- Tenant scoping is enforced by DB RLS policies.
- For writes/inserts the `tenant_id` **must** be obtained from `TchContextResolver.currentOrThrow().tenantId()` and inserted into the row so the unique constraint and auditing work.

---

# Conventions

## Handler key convention

- `handler_key` is a stable string constant in the projector/handler code.
- Recommended format: `"<domain>.<projection>"` or `"<domain>.<feature>.<projection>"`.
  - Example: `limitpolicy.exposure`.
- The handler MUST NOT accept handler_key from clients.

## Port (common)

Add a small common port to encapsulate processed-event checks:

```java
package com.tchalanet.server.common.application.port.out;

import java.util.UUID;

public interface ProcessedEventPort {
  boolean alreadyProcessed(String handlerKey, UUID eventId);
  void markProcessed(String handlerKey, UUID eventId);
}
```

## Adapter (JDBC, RLS-correct)

A reference adapter implementation (in `common`) should be:

```java
@Component
@RequiredArgsConstructor
public class ProcessedEventJdbcAdapter implements ProcessedEventPort {
  private final JdbcTemplate jdbc;
  private final TchContextResolver ctxResolver;

  public boolean alreadyProcessed(String handlerKey, UUID eventId) {
    Integer found = jdbc.queryForObject("""
      SELECT 1 FROM processed_event WHERE handler_key = ? AND event_id = ? LIMIT 1
    """, Integer.class, handlerKey, eventId);
    return found != null;
  }

  public void markProcessed(String handlerKey, UUID eventId) {
    var ctx = ctxResolver.currentOrThrow();
    var tenantId = ctx.tenantId().value();
    UUID createdBy = ctx.userUuid();
    jdbc.update("""
      INSERT INTO processed_event (tenant_id, handler_key, event_id, created_by)
      VALUES (?, ?, ?, ?)
      ON CONFLICT (tenant_id, handler_key, event_id) DO NOTHING
    """, tenantId, handlerKey, eventId, createdBy);
  }
}
```

Rules:

- `alreadyProcessed`: do NOT pass tenant_id in the WHERE clause (RLS will prevent cross-tenant reads).
- `markProcessed`: tenant_id must be set from context (not passed from caller).

---

# Minimal change in `core.limitpolicy` (apply exposure)

To adopt the new pattern with minimal changes:

- Inject `ProcessedEventPort` (common) into `ApplyTicketExposureCommandHandler`.
- Define a stable handler key constant:

```java
private static final String HANDLER_KEY = "limitpolicy.exposure";
```

- Handler flow:
  1. `UUID eventId = event.eventId().value();`
  2. `if (processedEvent.alreadyProcessed(HANDLER_KEY, eventId)) return;`
  3. `projector.applyTicketPlaced(event);`
  4. `processedEvent.markProcessed(HANDLER_KEY, eventId);`

This guarantees idempotence and respects RLS (no tenant filters in reads).

---

# Quick checklist for implementers

- [ ] Create Flyway migration `V16__processed_event.sql` and apply to dev/staging
- [ ] Add `ProcessedEventPort` to `common.application.port.out`
- [ ] Implement `ProcessedEventJdbcAdapter` in `common.infra.persistence`
- [ ] Replace dedicated per-domain ledger ports with `ProcessedEventPort` where appropriate
- [ ] Ensure all projectors use HANDLER_KEY constants and follow check→apply→mark
- [ ] Document handler_key strings in domain docs (stable and unique)

---

# FAQ

**Q**: Why not include tenant_id in the `alreadyProcessed` WHERE?
**A**: Because RLS must be the single source of truth for tenant isolation. Adding tenant_id in queries risks bypassing RLS assumptions and leads to duplicated logic.

**Q**: Do we need to keep old ledgers (e.g. `limit_exposure_ledger`)?
**A**: You can migrate to `processed_event` and eventually remove old tables or keep them for audit, but the new common table is recommended.
