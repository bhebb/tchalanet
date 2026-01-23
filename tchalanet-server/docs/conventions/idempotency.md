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
