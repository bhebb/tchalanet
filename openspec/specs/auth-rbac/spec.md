# Spec — Auth RBAC (Authorization on Protected Scopes)

## Status

**NORMATIVE**

---

## Requirements

### Requirement: Admin endpoints require SUPER_ADMIN authority

Every `@RestController` mapped to a path starting with `/admin/`, `/platform/`, or `/_sdr/` SHALL enforce Spring Security authorization via `@PreAuthorize` (declared at class level or on every method). No endpoint in these scopes SHALL be reachable without a valid `SUPER_ADMIN` authority.

#### Scenario: Unauthenticated request to admin draw endpoint is rejected

- **WHEN** an HTTP request is sent to `/admin/draws` without an Authorization header
- **THEN** the server returns HTTP 401 Unauthorized

#### Scenario: Unauthorized request to admin draw endpoint is rejected

- **WHEN** an HTTP request is sent to `/admin/draws` with a valid token that does not carry `SUPER_ADMIN` authority
- **THEN** the server returns HTTP 403 Forbidden

#### Scenario: Authorized request to admin draw endpoint is accepted

- **WHEN** an HTTP request is sent to `/admin/draws` with a valid token carrying `SUPER_ADMIN` authority
- **THEN** the server processes the request and returns the appropriate 2xx response

### Requirement: Ops draw calendar endpoints require SUPER_ADMIN authority

`DrawCalendarOpsController` (`/platform/ops/draws`) SHALL enforce `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` at the class level. Endpoints generate, open, close, and apply draw results — operations with irreversible financial consequences.

#### Scenario: Unauthenticated request to ops generate is rejected

- **WHEN** a POST request is sent to `/platform/ops/draws/generate` without authentication
- **THEN** the server returns HTTP 401

#### Scenario: Unauthenticated request to ops close-due is rejected

- **WHEN** a POST request is sent to `/platform/ops/draws/close-due` without authentication
- **THEN** the server returns HTTP 401

### Requirement: Ops draw results endpoints require SUPER_ADMIN authority

`DrawResultsOpsController` (`/platform/ops/draw-results`) SHALL enforce `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` at the class level. Endpoints can insert, override, and record manual draw results.

#### Scenario: Unauthenticated request to ops draw results override is rejected

- **WHEN** a POST request is sent to `/platform/ops/draw-results/override` without authentication
- **THEN** the server returns HTTP 401

#### Scenario: Unauthenticated request to ops manual result is rejected

- **WHEN** a POST request is sent to `/platform/ops/draw-results/manual` without authentication
- **THEN** the server returns HTTP 401

### Requirement: ArchUnit enforces authorization convention on protected scopes

An ArchUnit rule SHALL verify at build time that every `@RestController` whose base `@RequestMapping` begins with `/admin/`, `/platform/`, `/_sdr/`, or `/tenant/tickets/` carries a `@PreAuthorize` or `@Secured` annotation either at class level or on every public handler method. The rule SHALL cause the build to fail if any violation is detected.

#### Scenario: Controller without @PreAuthorize on protected path fails build

- **WHEN** a new `@RestController` is added with `@RequestMapping("/admin/new-feature")` but no `@PreAuthorize` is declared
- **THEN** the ArchUnit test `SecurityArchTest` fails and the build is rejected

#### Scenario: Controller with class-level @PreAuthorize passes ArchUnit

- **WHEN** a `@RestController` mapped to `/platform/ops/foo` carries `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` at class level
- **THEN** the `SecurityArchTest` passes for that controller

#### Scenario: Controller that must be public uses explicit permitAll whitelist

- **WHEN** an endpoint under `/platform/` is intentionally public, it SHALL be declared with `@PreAuthorize("permitAll()")` (not simply left without annotation)
- **THEN** the `SecurityArchTest` treats it as explicitly whitelisted and passes

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

### Requirement: Public ticket endpoints SHALL be rate-limited per IP

`PublicTicketController` (`/public/tickets`) SHALL be protected by an IP-based rate-limit filter. The filter SHALL bucket requests by client IP for the path prefix `/public/tickets/**` (covering both `verify` and `qr`). Default configuration: 10 requests per second per IP, burst 30. When the bucket is empty, the server SHALL return HTTP 429 Too Many Requests with a `Retry-After` header (in seconds).

#### Scenario: Burst exhausted returns 429

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

### Requirement: No residual TODO security comments

The codebase SHALL contain no commented-out `@PreAuthorize` annotations with `//todo remove testing` or similar deferrals. All security annotations MUST be active.

#### Scenario: Search for todo remove testing yields no results

- **WHEN** the codebase is scanned for the string `todo remove testing`
- **THEN** zero occurrences are found
