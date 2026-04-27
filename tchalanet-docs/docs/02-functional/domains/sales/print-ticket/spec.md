# SALES / Print Ticket — Deltas (v1)

References:

- ./00-index.md
- ./01-backend.md
- ../../context/10-non-negotiables.md
- ../../context/20-backend-rules.md
- ../../context/71-domain-sales.md

## ADDED Requirements

### Requirement: SALES-PRINT-PDF-001 — Print PDF endpoint returns a PDF

The system MUST expose `GET /api/v1/tenant/tickets/{ticket_id}/print.pdf` producing `application/pdf`.

#### Scenario: Print PDF returns 200 with no-store

Given a ticket exists in tenant scope  
And the actor is authorized  
When the actor calls the PDF print endpoint  
Then the server responds `200`  
And the response Content-Type is `application/pdf`  
And the response includes header `Cache-Control: no-store`

### Requirement: SALES-PRINT-ESCPOS-001 — Print ESC/POS endpoint returns bytes

The system MUST expose `GET /api/v1/tenant/tickets/{ticket_id}/print.escpos` producing `application/octet-stream`.

#### Scenario: Print ESC/POS returns 200 with no-store

Given a ticket exists in tenant scope  
And the actor is authorized  
When the actor calls the ESC/POS print endpoint  
Then the server responds `200`  
And the response Content-Type is `application/octet-stream`  
And the response includes header `Cache-Control: no-store`

### Requirement: SALES-PRINT-AUTH-001 — Print requires permission

Actors without permission MUST be rejected.

#### Scenario: Forbidden print returns 403

Given a ticket exists in tenant scope  
When an unauthorized actor calls a print endpoint  
Then the server responds `403`  
And ProblemDetail.code is `sales.print_ticket.forbidden`

### Requirement: SALES-PRINT-NOTFOUND-001 — Missing ticket returns 404

If ticket does not exist in tenant scope, server MUST return 404.

#### Scenario: Ticket not found returns 404

Given no ticket exists for the given id in tenant scope  
When an authorized actor calls a print endpoint  
Then the server responds `404`  
And ProblemDetail.code is `sales.print_ticket.not_found`
