# SALES / Void Ticket — Deltas (v1)

References:

- ./00-index.md
- ./01-backend.md
- ../../context/10-non-negotiables.md
- ../../context/20-backend-rules.md
- ../../context/71-domain-sales.md

## ADDED Requirements

### Requirement: SALES-VOID-HTTP-001 — Void endpoint exists

The system MUST expose `POST /api/v1/tenant/tickets/{ticket_id}/void`.

#### Scenario: Void SOLD ticket becomes VOID (200)

Given a ticket exists in tenant scope  
And `sale_status = SOLD`  
And `settlement_status = UNSETTLED`  
And the ticket is within the configured void window  
When an authorized actor calls the void endpoint  
Then the server responds `200`  
And `sale_status = VOID`

### Requirement: SALES-VOID-WINDOW-001 — Void window is enforced

The server MUST enforce a configured void window (tenant/outlet policy).

#### Scenario: Void outside window returns 409

Given a ticket exists in tenant scope  
And `sale_status = SOLD`  
And the ticket is outside the configured void window  
When an authorized actor calls void  
Then the server responds `409`  
And ProblemDetail.code is `sales.void_ticket.state_conflict`

### Requirement: SALES-VOID-STATE-001 — Void requires UNSETTLED

Settled tickets MUST NOT be voided.

#### Scenario: Void SETTLED returns 409

Given a ticket exists in tenant scope  
And `settlement_status = SETTLED`  
When an authorized actor calls void  
Then the server responds `409`  
And ProblemDetail.code is `sales.void_ticket.state_conflict`

### Requirement: SALES-VOID-AUTH-001 — Void requires permission

Actors without permission MUST receive 403.

#### Scenario: Forbidden void returns 403

Given a ticket exists in tenant scope  
When an unauthorized actor calls void  
Then the server responds `403`  
And ProblemDetail.code is `sales.void_ticket.forbidden`

### Requirement: SALES-VOID-NOTFOUND-001 — Missing ticket returns 404

If ticket does not exist in tenant scope, server MUST return 404.

#### Scenario: Ticket not found returns 404

Given no ticket exists for the given id in tenant scope  
When an authorized actor calls void  
Then the server responds `404`  
And ProblemDetail.code is `sales.void_ticket.not_found`
