# API Error Contract

## ADDED Requirements

### Requirement: Canonical blocking error payload

Application JSON endpoint blocking failures SHALL use `ProblemDetail`
(`application/problem+json`) with a stable `code`, closed `category`, canonical `retryPolicy`,
derived `retryable`, typed public `params`, and available `requestId`, `traceId`, `spanId`, and
generated `errorId`.

The guarantee applies to application JSON endpoints. Proxy/CORS failures, binary downloads,
SSE/WebSocket, and empty/HEAD responses SHALL use documented transport adapters rather than promise
a JSON body they cannot safely carry.

`title` and `detail` remain wire-compatible during migration but SHALL be static, non-displayable
text. Clients SHALL never render or interpolate them. Redacted operational correlation remains
available through server-side observability using stable codes and correlation identifiers.

The closed server category vocabulary is `auth_required`, `access_denied`, `validation`,
`not_found`, `conflict`, `business_rule`, `rate_limited`, `service_unavailable`, and
`unexpected`. Client-only categories may additionally include `network_unavailable`.

#### Scenario: Blocking business failure

- **WHEN** a sale request reaches a draw whose cutoff has passed
- **THEN** the server returns a `409 ProblemDetail` with
  `code = sales.draw.cutoff_passed`, `category = conflict`, and an approved recovery policy
- **AND** public `params` contain only descriptor-approved display-safe values
- **AND** the body includes no tenant-foreign or internal draw identifier

#### Scenario: Unknown technical failure

- **WHEN** an unexpected exception reaches the catch-all handler
- **THEN** the server returns `500 ProblemDetail` with `code = internal.unexpected` and
  `category = unexpected`
- **AND** the body contains no exception message, class name, stack element, provider prose, or
  interpolated internal identifier

### Requirement: Owner-defined descriptors validate codes, parameters, and recovery

Every externally visible server code SHALL be declared as an `ErrorDescriptor` in its owning package.
The descriptor SHALL define code, category, expected HTTP status, retry policy, client audiences,
and typed public parameter specifications. A common collector SHALL validate uniqueness, lowercase
dotted syntax (`^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$`), category membership, and descriptor
metadata at startup or test time. Concrete business codes SHALL not be collected in one global
business enum.

Framework authentication and authorization failures that occur before controller advice SHALL use
the same descriptor-backed `application/problem+json` writer. They SHALL return generic codes and
must not expose token, provider, account, tenant, or policy diagnostics.

#### Scenario: Filter-chain authentication failure

- **WHEN** a JWT is missing, invalid, or fails sensitive identity verification before a controller
- **THEN** the response is a `401 ProblemDetail` with
  `code = access.authentication_required`, `category = auth_required`, and
  `retryPolicy = AFTER_REAUTH`
- **AND** it contains no bearer-token, provider, account, or seller-terminal diagnostic

#### Scenario: Filter-chain authorization failure

- **WHEN** Spring Security rejects an authenticated request before controller advice
- **THEN** the response is a `403 ProblemDetail` with `code = access.denied`
- **AND** it contains no tenant or access-policy diagnostic

Each public parameter specification SHALL declare its name, primitive or display-safe type, and
permitted audience. A parameter name alone SHALL NOT authorize an internal identifier.

#### Scenario: Duplicate descriptor code

- **WHEN** two descriptors declare the same code
- **THEN** descriptor validation fails and names both declaration sites

#### Scenario: Non-conforming code

- **WHEN** a descriptor declares `APPROVAL_REQUIRED` or `NotFound`
- **THEN** descriptor validation fails with the expected lowercase dotted syntax

#### Scenario: Unsafe parameter

- **WHEN** a producer supplies an unknown parameter, object payload, PIN, token, credential,
  provider/SQL payload, or non-public identifier
- **THEN** construction fails in development/test or drops and logs that parameter in production
- **AND** the value never reaches the serialized payload

#### Scenario: Meaningful recovery policy

- **WHEN** a descriptor represents a recoverable failure
- **THEN** its policy distinguishes `AFTER_USER_ACTION`, `AFTER_REAUTH`, `AFTER_DELAY`, or
  `RETRY_SAME_INTENT` from `NEVER`
- **AND** clients do not automatically retry a non-idempotent sale mutation

### Requirement: Producers are code-first

`ProblemRest` SHALL expose a code-first production factory taking an `ErrorDescriptor` and approved
public parameters. Legacy message-first factories are deprecated migration bridges. An
ArchUnit/static rule SHALL fail any new message-first producer while a baseline allowlist only
shrinks.

Producers SHALL not introduce free-text detail, inline external codes, or hybrid `code: prose`
strings. Inventoried legacy producers SHALL be classified as stable code, business prose,
hybrid/dynamic, or framework/technical before migration.

#### Scenario: New legacy call site

- **WHEN** a developer adds `ProblemRest.conflict("Draw is not open for sales")`
- **THEN** the architecture suite fails identifying the call site

#### Scenario: Existing hybrid producer migrates

- **WHEN** a producer previously emits `sales.tenant_disabled: <status>`
- **THEN** it uses a registered stable descriptor
- **AND** any product-visible value is a typed approved public parameter
- **AND** operational correlation remains available only through redacted server-side observability

### Requirement: Validation failures are structured and safe

Bean Validation, framework binding, and domain validation failures SHALL serialize a `violations`
array. Each violation SHALL contain a translatable per-constraint code, nested/array-capable field
path, and typed approved public parameters. Bean-validation prose SHALL never be a client
translation contract.

Legacy `errors` maps and per-violation message/target fields SHALL be removed only after their
clients have migrated.

#### Scenario: Bean Validation failure

- **WHEN** `name` violates `@NotBlank` and `lines[2].amount` violates `@Max(500)`
- **THEN** the response has `code = validation.failed`
- **AND** violations contain `validation.not_blank` for `name` and `validation.max` for
  `lines[2].amount` with approved `{ max: 500 }`
- **AND** no `defaultMessage` text appears in the payload

#### Scenario: Unreadable request body

- **WHEN** a request body contains malformed JSON
- **THEN** the response has `code = request.not_readable`
- **AND** contains no Jackson cause message, payload excerpt, or Java class name

### Requirement: Error and notice payloads are redacted

Serialized errors, notices, capabilities, and support references SHALL NOT contain exception
messages or class names, stack traces, nested provider/HTTP-client prose, credentials, tokens, PINs,
SQL/provider payloads, internal service/component identifiers, or security/identity/tenant
enumeration signals. A representative redaction regression suite SHALL run in CI before endpoint
migration proceeds.

Server logs SHALL use the stable error code and correlation identifiers. They SHALL NOT emit raw
exception messages, request payloads, or unredacted stack traces from an error boundary.

#### Scenario: Security response does not enumerate

- **WHEN** an authentication or tenant-scoping failure concerns an existing or non-existing
  principal/tenant
- **THEN** the responses are semantically identical apart from generated correlation identifiers
- **AND** neither response reveals the existence of the principal or tenant

### Requirement: Non-blocking notices are typed and UI-agnostic

`ApiNotice` SHALL contain stable code, `kind` (`business`, `degradation`, or `information`),
severity, domain, optional stable functional target, retry policy, typed public params, and a
structured correlation block. `target` identifies a feature or slice, never an internal service,
class, framework component, or visual placement.

Free-form `meta` and `message` are migration-only and SHALL not be client display contracts. They
are removed when the migration ledger reaches zero.

#### Scenario: Business warning

- **WHEN** a sale succeeds while approaching a seller limit
- **THEN** it emits a `business` warning with `code = sales.limit.approaching`
- **AND** it contains only approved public params such as remaining amount
- **AND** the response status is `SUCCESS_WITH_WARNINGS`

#### Scenario: Optional slice fails

- **WHEN** a BFF catches a non-blocking stats failure
- **THEN** it emits exactly one `degradation` notice whose target is `stats`
- **AND** no internal service name, exception prose, or UI placement reaches the client

### Requirement: Optional BFF sections have explicit availability state

BFF orchestration SHALL classify dependencies as blocking, non-blocking, or background before
response mapping. Independently recoverable optional sections SHALL represent availability as
`AVAILABLE`, `EMPTY`, or `UNAVAILABLE`. A nullable business value alone SHALL NOT encode a failed
slice because null may be a legitimate domain value.

Only BFF orchestration decides whether a dependency blocks the response; a downstream HTTP status
alone does not decide it. A required technical failure SHALL use the canonical error contract and
never flow through a legacy `IllegalStateException -> 422` bridge.

#### Scenario: Dashboard has one unavailable section

- **WHEN** required summary succeeds and optional stats times out
- **THEN** the response is `200 PARTIAL` with stats state `UNAVAILABLE`
- **AND** exactly one targeted degradation notice describes stats
- **AND** a local retry is offered only when its descriptor policy permits it

#### Scenario: Optional query is empty

- **WHEN** recent tickets succeeds with zero rows
- **THEN** its state is `EMPTY`
- **AND** no degradation notice is emitted

### Requirement: Capability health is distinct from response completeness

`ServiceStatus` SHALL describe cross-cutting functional capability health only. It SHALL use a
functional capability name, `UP`, `DEGRADED`, or `DOWN`, stable code, retry policy, and optional
retry-after duration. It SHALL not contain a message or internal service/class name, and duplicate
capability states SHALL be collapsed within a response.

`PARTIAL` SHALL mean expected response data is unavailable. A business warning or degraded
capability with complete response data SHALL result in `SUCCESS_WITH_WARNINGS`. `PENDING` is
explicit handler intent and SHALL never be inferred by response advice from a sentinel code.

An unusable requested primary resource SHALL be a `404 ProblemDetail`; no `ApiResponse.notFound`
factory may represent it as `SUCCESS` plus an error notice.

#### Scenario: Printing capability is degraded

- **WHEN** printing is degraded but the response has all expected data
- **THEN** it returns `SUCCESS_WITH_WARNINGS`
- **AND** contains one capability state for `printing`

### Requirement: Clients normalize contracts without transport prose

Web and mobile normalized errors SHALL carry code, category, origin, status, retry policy, owner,
target, field, correlation identifiers, and deterministic dedupe key. Known category and recovery
metadata SHALL come from the payload descriptor, not substring or status heuristics.

Production client state, support references, and telemetry SHALL contain only redacted structured
context and correlation identifiers. They SHALL not retain raw server title/detail, notice message,
HTTP response text, or exception text. Client-originated failures SHALL use registered `client.*`
codes and translated catalogs; hardcoded normalizer copy is prohibited.

#### Scenario: Missing exact translation

- **WHEN** a known code has no exact catalog entry in the active locale
- **THEN** the client renders translated category copy and then generic copy
- **AND** it never displays the raw code, title, or detail

#### Scenario: Correlation headers remain distinct

- **WHEN** a response has `X-Request-Id` but no trace header
- **THEN** the client sets request ID and leaves trace ID absent
- **AND** it never assigns request ID to a trace field

### Requirement: Product-visible codes have audience-complete catalogs

Every product-visible code SHALL declare its receiving client audiences. Each receiving client SHALL
ship exact-code, category, and generic copy in Haitian Creole, French, and English. Lookup order is
exact code, category, then generic. CI SHALL fail for duplicate/orphan/missing applicable keys or
invalid interpolation against descriptor parameter specifications.

#### Scenario: Applicable locale is missing

- **WHEN** an admin-visible registered code lacks Haitian Creole or English copy in the admin portal
- **THEN** catalog CI fails naming the code and missing locales

### Requirement: Shared fixtures prove cross-client conformance

Versioned JSON fixtures SHALL define blocking errors, validation failures, success with warnings,
partial BFF results with unavailable sections, capability degradation, explicit pending results,
malformed envelopes, and void responses. Java, TypeScript, and Dart tests SHALL consume the same
fixtures directly; runtime implementation remains independent.

#### Scenario: Partial dashboard fixture

- **WHEN** Java, Angular, and Flutter tests consume the partial-dashboard fixture
- **THEN** each asserts the same response status, section state, notice routing, and correlation
  semantics
