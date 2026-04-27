# CHANGELOG — tchalanet-server

## [Unreleased]

### Security

- **FIXED** `TicketController` cancel and print endpoints now require ROLE_CASHIER/ROLE_ADMIN/ROLE_SUPER_ADMIN — previously accessible by any authenticated user. `@EnableMethodSecurity(securedEnabled = true)` activated in `SecurityConfig`. ([secure-sales-ticket-endpoints])
- **ADDED** IP-based rate-limiting on `/public/tickets/**` (Bucket4j in-memory, default 10 req/s, burst 30). Returns HTTP 429 + `Retry-After` when bucket empty. Configurable via `tch.public.tickets.rate-limit.*`. ([secure-sales-ticket-endpoints])
- **ADDED** `SecurityArchTest` now covers `/tenant/tickets/` prefix — build fails if any controller under this path lacks `@Secured` or `@PreAuthorize`. ([secure-sales-ticket-endpoints])

### BREAKING

- **`GET /public/tickets/verify/{publicCode}`** — Response body format changed from raw object to `ApiResponse<TicketVerificationResult>` envelope. Clients must read `.data` to access the verification result. 404 responses now have a body (`ApiResponse` with NOT_FOUND notice) instead of an empty body. Coordinate with frontend/POS before deploying. ([secure-sales-ticket-endpoints])

### Added

- `NoticeSeverity.ERROR` enum value added for error-level API notices.
- `ApiNotice.error(code, message)`, `ApiNotice.warn(code, message)`, `ApiNotice.info(code, message)` factory methods.
- `ApiResponse.notFound(message)` factory method — returns HTTP-safe 404-compatible `ApiResponse`.
- `PublicTicketsRateLimitFilter` — Spring `OncePerRequestFilter`, bucket by IP, path `/public/tickets/**`.
- `PublicTicketsRateLimitProperties` — `@ConfigurationProperties(prefix = "tch.public.tickets.rate-limit")`.
