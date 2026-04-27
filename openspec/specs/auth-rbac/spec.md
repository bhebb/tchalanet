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

An ArchUnit rule SHALL verify at build time that every `@RestController` whose base `@RequestMapping` begins with `/admin/`, `/platform/`, or `/_sdr/` carries a `@PreAuthorize` annotation either at class level or on every public handler method. The rule SHALL cause the build to fail if any violation is detected.

#### Scenario: Controller without @PreAuthorize on protected path fails build

- **WHEN** a new `@RestController` is added with `@RequestMapping("/admin/new-feature")` but no `@PreAuthorize` is declared
- **THEN** the ArchUnit test `SecurityArchTest` fails and the build is rejected

#### Scenario: Controller with class-level @PreAuthorize passes ArchUnit

- **WHEN** a `@RestController` mapped to `/platform/ops/foo` carries `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` at class level
- **THEN** the `SecurityArchTest` passes for that controller

#### Scenario: Controller that must be public uses explicit permitAll whitelist

- **WHEN** an endpoint under `/platform/` is intentionally public, it SHALL be declared with `@PreAuthorize("permitAll()")` (not simply left without annotation)
- **THEN** the `SecurityArchTest` treats it as explicitly whitelisted and passes

### Requirement: No residual TODO security comments

The codebase SHALL contain no commented-out `@PreAuthorize` annotations with `//todo remove testing` or similar deferrals. All security annotations MUST be active.

#### Scenario: Search for todo remove testing yields no results

- **WHEN** the codebase is scanned for the string `todo remove testing`
- **THEN** zero occurrences are found
