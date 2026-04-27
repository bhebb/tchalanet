# SALES / Public Verify Ticket — Deltas (v1)

References:

- ./00-index.md
- ./01-backend.md
- ../../context/10-non-negotiables.md
- ../../context/50-edge-service-rules.md
- ../../context/71-domain-sales.md
- ../../context/26-ticket-codes.md

## ADDED Requirements

### Requirement: SALES-PUBV-SINGLE-001 — Single verify endpoint exists

The system MUST expose `GET /api/v1/public/tickets/verify/{public_code}` without authentication.

#### Scenario: Existing code returns 200 with a public snapshot

Given a ticket exists for the given `public_code`  
When the client calls single verify  
Then the server responds `200`  
And the response includes a masked public snapshot  
And the response does NOT include internal identifiers

### Requirement: SALES-PUBV-SINGLE-002 — Visibility window returns EXPIRED, not 404

If a ticket exists but is outside visibility window, the server MUST return `200` with `public_state=EXPIRED`.

#### Scenario: Expired ticket returns 200 with EXPIRED

Given a ticket exists for the given `public_code`  
And the ticket is outside visibility window  
When the client calls single verify  
Then the server responds `200`  
And `ticket.public_state = EXPIRED`

### Requirement: SALES-PUBV-SINGLE-003 — Unknown code returns 404

Unknown or invalid codes MUST return 404 with canonical error code.

#### Scenario: Unknown code returns 404 not_found

Given no ticket exists for the given `public_code`  
When the client calls single verify  
Then the server responds `404`  
And ProblemDetail.code is `sales.public_verify_ticket.not_found`

### Requirement: SALES-PUBV-BATCH-001 — Batch verify endpoint exists

The system MUST expose `POST /api/v1/public/tickets/verify` for batch verification without authentication.

#### Scenario: Batch returns 200 with per-item results

Given a request with multiple `public_codes`  
When the client calls batch verify  
Then the server responds `200`  
And the response includes `items[]`  
And each item includes `public_code` and `status`

### Requirement: SALES-PUBV-BATCH-002 — Invalid batch request returns 400

Empty or oversized batches MUST return 400.

#### Scenario: Empty batch returns 400 invalid_request

Given a request with `public_codes=[]`  
When the client calls batch verify  
Then the server responds `400`  
And ProblemDetail.code is `sales.public_verify_ticket.batch.invalid_request`

### Requirement: SALES-PUBV-BATCH-003 — Per-item INVALID and NOT_FOUND are supported

Batch processing MUST not fail the whole response for invalid/unknown codes.

#### Scenario: Mixed codes return mixed statuses

Given a batch with one valid code, one invalid code format, and one unknown code  
When the client calls batch verify  
Then the response is `200`  
And one item has `status=OK`  
And one item has `status=INVALID`  
And one item has `status=NOT_FOUND`
