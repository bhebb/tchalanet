# SALES / Sell Ticket — Deltas (v1)

References:

- ./00-index.md
- ./01-backend.md
- ./02-frontend.md
- ./03-terminal.md
- ../../context/25-idempotency.md
- ../../context/26-ticket-codes.md

## ADDED Requirements

### Requirement: SALES-SELL-HTTP-001 — Sell ticket endpoint exists

The system MUST expose the tenant endpoint `POST /api/v1/tenant/tickets` to sell tickets.

#### Scenario: Happy path creates SOLD ticket (201)

Given a valid sell draft with at least one line  
And a valid terminal session exists  
And the request includes a valid `Idempotency-Key` header  
When the client calls `POST /api/v1/tenant/tickets`  
Then the server responds `201`  
And the response includes `ticket_id`, `ticket_code`, and `public_code`  
And `sale_status = SOLD`  
And `result_status = NOT_RESULTED`  
And `settlement_status = UNSETTLED`

### Requirement: SALES-SELL-IDEMP-001 — Missing idempotency key is rejected

The server MUST reject sell requests that omit the `Idempotency-Key` header.

#### Scenario: Missing Idempotency-Key returns 400

Given a valid sell draft  
When the client calls `POST /api/v1/tenant/tickets` without `Idempotency-Key`  
Then the server responds `400`  
And ProblemDetail.code is `idempotency.missing`
