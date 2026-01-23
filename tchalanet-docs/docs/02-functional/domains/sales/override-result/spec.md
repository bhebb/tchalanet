# SALES / Override Result — Deltas (v1)

References:

- ./00-index.md
- ./01-backend.md
- ../../context/10-non-negotiables.md
- ../../context/20-backend-rules.md
- ../../context/71-domain-sales.md

## ADDED Requirements

### Requirement: SALES-OVR-HTTP-001 — Override endpoint exists

The system MUST expose `POST /api/v1/tenant/tickets/{ticket_id}/override-result`.

#### Scenario: Override to WON returns 200 and sets OVERRIDDEN

Given a ticket exists in tenant scope  
And `sale_status != VOID`  
And `settlement_status = UNSETTLED`  
When an authorized actor overrides the result with `outcome=WON` and valid reason  
Then the server responds `200`  
And `result_status = OVERRIDDEN`  
And `override_outcome = WON`

### Requirement: SALES-OVR-REQ-001 — Override requires reason_code

Override requests MUST include `reason_code`.

#### Scenario: Missing reason_code returns 422

Given a ticket exists in tenant scope  
When an authorized actor overrides without `reason_code`  
Then the server responds `422`  
And ProblemDetail.code is `sales.override_result.invalid`

### Requirement: SALES-OVR-STATE-001 — Override is UNSETTLED-only

Settled tickets MUST NOT be overridden.

#### Scenario: Override SETTLED returns 409

Given a ticket exists in tenant scope  
And `settlement_status = SETTLED`  
When an authorized actor calls override  
Then the server responds `409`  
And ProblemDetail.code is `sales.override_result.state_conflict`

### Requirement: SALES-OVR-AUTH-001 — Override requires permission

Actors without permission MUST receive 403.

#### Scenario: Forbidden override returns 403

Given a ticket exists in tenant scope  
When an unauthorized actor calls override  
Then the server responds `403`  
And ProblemDetail.code is `sales.override_result.forbidden`

### Requirement: SALES-OVR-NOTFOUND-001 — Missing ticket returns 404

If ticket does not exist in tenant scope, server MUST return 404.

#### Scenario: Ticket not found returns 404

Given no ticket exists for the given id in tenant scope  
When an authorized actor calls override  
Then the server responds `404`  
And ProblemDetail.code is `sales.override_result.not_found`
