# SALES / Reject Ticket — Deltas (v1)

References:

- ./00-index.md
- ./01-backend.md
- ../../context/10-non-negotiables.md
- ../../context/20-backend-rules.md
- ../../context/71-domain-sales.md

## ADDED Requirements

### Requirement: SALES-REJECT-HTTP-001 — Reject endpoint exists

The system MUST expose `POST /api/v1/tenant/tickets/{ticket_id}/reject`.

#### Scenario: Reject pending ticket becomes REJECTED (200)

Given a ticket exists in tenant scope  
And `sale_status = PENDING_APPROVAL`  
And `settlement_status = UNSETTLED`  
When an authorized actor calls the reject endpoint with a valid reason  
Then the server responds `200`  
And `sale_status = REJECTED`

### Requirement: SALES-REJECT-REQ-001 — Reject requires reason_code

Reject requests MUST include a `reason_code` (and MAY include `reason_note`).

#### Scenario: Missing reason_code returns 422

Given a ticket exists in tenant scope  
When an authorized actor calls reject without `reason_code`  
Then the server responds `422`  
And ProblemDetail.code is `sales.reject_ticket.invalid`

### Requirement: SALES-REJECT-STATE-001 — Reject enforces state constraints

The server MUST reject attempts to reject tickets whose `sale_status` is not `PENDING_APPROVAL`.

#### Scenario: Rejecting SOLD returns 409

Given a ticket exists in tenant scope  
And `sale_status = SOLD`  
When an authorized actor calls reject  
Then the server responds `409`  
And ProblemDetail.code is `sales.reject_ticket.state_conflict`

### Requirement: SALES-REJECT-AUTH-001 — Reject requires permission

Actors without the appropriate permission MUST be rejected.

#### Scenario: Forbidden reject returns 403

Given a ticket exists in tenant scope  
When an unauthorized actor calls reject  
Then the server responds `403`  
And ProblemDetail.code is `sales.reject_ticket.forbidden`

### Requirement: SALES-REJECT-NOTFOUND-001 — Missing ticket returns 404

If the ticket does not exist in tenant scope, server MUST return 404.

#### Scenario: Ticket not found returns 404

Given no ticket exists for the given id in tenant scope  
When an authorized actor calls reject  
Then the server responds `404`  
And ProblemDetail.code is `sales.reject_ticket.not_found`
