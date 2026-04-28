## ADDED Requirements

### Requirement: Tenant tickets cancel and print endpoints require role

`TicketController` (`/tenant/tickets`) endpoints `PATCH /{ticketId}/cancel`, `GET /{ticketId}/print`, `GET /{ticketId}/print.escpos`, `GET /{ticketId}/print.pdf` SHALL be annotated with `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})`. No anonymous or low-privilege authenticated principal SHALL be able to cancel a ticket or fetch a print copy (PDF/ESC-POS).

#### Scenario: Unauthenticated cancel returns 401

- **WHEN** `PATCH /tenant/tickets/{ticketId}/cancel` is called without an Authorization header
- **THEN** the server returns HTTP 401

#### Scenario: Authenticated user without role cannot cancel

- **WHEN** `PATCH /tenant/tickets/{ticketId}/cancel` is called with a valid token whose authorities do not include any of `ROLE_CASHIER, ROLE_ADMIN, ROLE_SUPER_ADMIN`
- **THEN** the server returns HTTP 403

#### Scenario: Cashier can cancel ticket

- **WHEN** `PATCH /tenant/tickets/{ticketId}/cancel` is called with a valid token carrying `ROLE_CASHIER`
- **THEN** the server processes the cancel command and returns 200 with the cancel response

#### Scenario: Authenticated user without role cannot fetch print PDF

- **WHEN** `GET /tenant/tickets/{ticketId}/print.pdf` is called with a token whose authorities do not include any role from {`ROLE_CASHIER`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`}
- **THEN** the server returns HTTP 403

#### Scenario: Cashier can fetch print PDF

- **WHEN** `GET /tenant/tickets/{ticketId}/print.pdf` is called with a token carrying `ROLE_CASHIER`
- **THEN** the server returns 200 with `Content-Type: application/pdf` and the PDF body

#### Scenario: Authenticated user without role cannot fetch ESC/POS

- **WHEN** `GET /tenant/tickets/{ticketId}/print.escpos` is called with a token without any sales role
- **THEN** the server returns HTTP 403

### Requirement: ArchUnit covers `/tenant/tickets/**` for authorization

The `SecurityArchTest` ArchUnit rule SHALL include `/tenant/tickets/` in its set of protected path prefixes, in addition to the existing `/admin/`, `/platform/`, `/_sdr/`. Every `@RestController` mapped under `/tenant/tickets/` SHALL declare `@Secured` or `@PreAuthorize` either at class level or on every public handler method. Build SHALL fail on any violation.

#### Scenario: New tenant ticket endpoint without @Secured fails build

- **WHEN** a new `@PostMapping` is added to `TicketController` without `@Secured` and without a class-level `@Secured`
- **THEN** `SecurityArchTest` fails and the build is rejected

#### Scenario: Endpoint with @Secured passes build

- **WHEN** a new `@PostMapping` carries `@Secured({"ROLE_ADMIN"})`
- **THEN** `SecurityArchTest` passes for that endpoint

### Requirement: Public ticket endpoints SHALL be rate-limited per IP

`PublicTicketController` (`/public/tickets`) SHALL be protected by an IP-based rate-limit filter. The filter SHALL bucket requests by client IP for the path prefix `/public/tickets/**` (covering both `verify` and `qr`). Default configuration: 10 requests per second per IP, burst 30. When the bucket is empty, the server SHALL return HTTP 429 Too Many Requests with a `Retry-After` header (in seconds).

#### Scenario: 11th request in 1 second from same IP returns 429

- **GIVEN** the rate-limit configuration is `requests-per-second: 10, burst: 30, enabled: true`
- **WHEN** 31 successive `GET /public/tickets/verify/{publicCode}` requests arrive from the same IP within 1 second
- **THEN** the first 30 requests are processed normally and the 31st returns HTTP 429 with a `Retry-After` header

#### Scenario: Rate-limit can be disabled via configuration

- **GIVEN** `tch.public.tickets.rate-limit.enabled: false`
- **WHEN** 1000 requests arrive from the same IP in 1 second
- **THEN** none of them are rejected by the rate-limit filter

#### Scenario: 429 events are logged at WARN level

- **WHEN** the rate-limit filter rejects a request
- **THEN** a `WARN` log entry contains the source IP and the request path

### Requirement: Public verify endpoint MUST return ApiResponse<T>

`PublicTicketController.verify` SHALL return `ResponseEntity<ApiResponse<TicketVerificationResult>>`. The 200 response SHALL wrap the verification result inside `ApiResponse.success(...)`. The 404 response SHALL be `ApiResponse.notFound(...)` (not `ResponseEntity.status(404).build()`). The headers `X-Robots-Tag: noindex, nofollow` and `Cache-Control: no-store` SHALL remain on every response.

#### Scenario: Verify returns ApiResponse envelope

- **WHEN** `GET /public/tickets/verify/{publicCode}` resolves a visible ticket
- **THEN** the body matches `{ "data": { ... TicketVerificationResult ... }, "errors": [], "notices": [], ... }` (the `ApiResponse<T>` envelope)
- **AND** headers include `X-Robots-Tag: noindex, nofollow` and `Cache-Control: no-store`

#### Scenario: Unknown publicCode returns ApiResponse 404

- **WHEN** `GET /public/tickets/verify/UNKNOWN_CODE` is called
- **THEN** the response is HTTP 404 with body `ApiResponse` carrying an error entry
