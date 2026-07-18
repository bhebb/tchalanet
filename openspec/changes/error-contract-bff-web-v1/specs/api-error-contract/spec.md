# API Error Contract

## ADDED Requirements

### Requirement: BFF slice failures are classified before response mapping

BFF orchestration SHALL classify each downstream or domain slice dependency as blocking,
non-blocking, or background before mapping the response.

The existing HTTP response semantics SHALL be preserved: successful/partial responses use
`ApiResponse<T>`, and blocking HTTP failures use `ProblemDetail`.

#### Scenario: Required slice fails

- **WHEN** a required slice fails and the BFF cannot produce a valid user-facing result
- **THEN** the BFF fails the request
- **AND** the server returns `ProblemDetail`
- **AND** the `ProblemDetail` includes a stable `code` and available trace identifiers

#### Scenario: Optional slice fails

- **WHEN** an optional slice fails and the BFF can still produce useful primary data
- **THEN** the BFF returns a successful `ApiResponse`
- **AND** the response includes an `ApiNotice` and/or `ServiceStatus`
- **AND** the notice includes a stable code, severity, source/domain, and support correlation metadata

### Requirement: Blocking errors use ProblemDetail with stable codes

Blocking backend errors SHALL be represented as `ProblemDetail` and SHALL include stable machine
codes that the web app can translate without parsing human-readable text.

#### Scenario: Application exception is handled globally

- **WHEN** `GlobalErrorHandler` handles a `TchException` or known request exception
- **THEN** the emitted `ProblemDetail` includes `code`
- **AND** includes `requestId`, `traceId`, `spanId`, or `errorId` when available
- **AND** backend `title/detail` are not required to be suitable for direct public UI copy

### Requirement: Non-blocking errors travel in successful response notices

Non-blocking slice failures SHALL travel inside `ApiResponse.notices` or service health metadata,
not as thrown HTTP errors.

#### Scenario: BFF returns partial dashboard data

- **WHEN** a dashboard BFF returns core data but one optional stats slice fails
- **THEN** the HTTP response remains successful
- **AND** `ApiResponse.status` indicates warnings or partial data
- **AND** an `ApiNotice` describes the optional failure with code, severity, domain/source, and trace
  metadata
- **AND** clients do not treat the notice as a blocking HTTP error

### Requirement: Notice metadata uses reserved generic keys

Non-blocking failure notices SHALL use a small reserved metadata vocabulary for support correlation
and source identification.

#### Scenario: Optional slice failure is converted to a notice

- **WHEN** a BFF catches a non-blocking slice failure
- **THEN** the notice metadata uses reserved keys such as `source`, `service`, `operation`,
  `requestId`, `traceId`, `spanId`, and `errorId`
- **AND** frontend behavior does not depend on many feature-specific metadata keys
- **AND** domain-specific metadata remains optional and diagnostic-only

### Requirement: Notice creation is centralized through a helper

Backend application code SHALL have a small helper/factory for adding response notices with standard
metadata.

#### Scenario: BFF emits a warning notice

- **WHEN** BFF code emits a non-blocking warning
- **THEN** it can call a helper with code, message, domain, severity/source information, and optional
  exception
- **AND** the helper attaches trace identifiers and generated `errorId`
- **AND** the helper stores the notice in `ApiResponseContext`
- **AND** controllers/services do not manually recreate the same metadata map each time

### Requirement: Error codes are centralized and namespaced

Backend error and notice codes SHALL be centralized by owner and SHALL use stable namespaced strings.
Centralization SHALL be owner-based rather than one large global enum.

#### Scenario: Feature emits an identity activation failure notice

- **WHEN** platform identity activation cannot complete as part of an optional BFF slice
- **THEN** the emitted notice uses a stable code such as `platform.identity.activation.error`
- **AND** controllers/services do not invent alternate strings for the same condition

#### Scenario: Blocking exception carries a stable code

- **WHEN** application code throws a `TchException` with an owner-defined stable code
- **THEN** `GlobalErrorHandler` emits that code in `ProblemDetail`
- **AND** adds support correlation fields through the normal error decoration path
- **AND** callers do not manually assemble the final HTTP error body

#### Scenario: Non-blocking notice carries a stable code

- **WHEN** BFF code records a non-blocking warning or error through the notice helper
- **THEN** `ApiResponseContext` stores the notice
- **AND** `ApiResponseBodyAdvice` emits it in the successful `ApiResponse`
- **AND** callers do not manually assemble the final `ApiResponse` envelope

### Requirement: Web translation prefers exact codes over fallback

The web app SHALL translate backend errors and notices by exact stable code before using category or
generic fallback messages.

#### Scenario: Known code is received

- **WHEN** the web app receives a `ProblemDetail` or `ApiNotice` with a known code
- **THEN** it renders the localized copy for that exact code
- **AND** avoids showing raw backend `message`, `title`, or `detail` as the primary user message

#### Scenario: Raw backend diagnostic text is present

- **WHEN** backend diagnostic text, provider messages, SQL messages, or stack traces are available
- **THEN** public/minimal web views do not render those values
- **AND** support diagnostics preserve only safe correlation fields

#### Scenario: Unknown code is received

- **WHEN** the web app receives an unknown code
- **THEN** it falls back to category/severity copy
- **AND** retains the original code and trace identifiers in support-safe diagnostics

### Requirement: Product-visible error codes have verified client copy

Every product-visible backend error or notice code SHALL have title and message copy in the shipped
Haitian Creole, French, and English bundles for every client family that can receive it. Backend
human-readable text SHALL NOT be the normal visible fallback.

#### Scenario: Known backend code reaches a client

- **WHEN** a public, admin, platform, or mobile client receives a known product-visible code
- **THEN** it renders its local exact-code translation
- **AND** a CI parity test proves that the locale bundle contains title and message copy
- **AND** it does not display `ProblemDetail.title`, `ProblemDetail.detail`, or `ApiNotice.message`
  as primary product copy

#### Scenario: Client receives an unknown code

- **WHEN** a client receives a code absent from its local catalog
- **THEN** it renders a localized category or generic fallback
- **AND** it preserves the code and support-safe correlation reference
- **AND** telemetry/diagnostics make the missing catalog entry discoverable

### Requirement: BFF presentation notices and domain notices have different owners

The contract SHALL distinguish BFF presentation degradation from feature-owned domain notices.

#### Scenario: Optional dashboard slice fails

- **WHEN** a BFF can return the page while one optional slice fails
- **THEN** its notice MAY include a stable functional `target` such as `recentTickets`
- **AND** the client request context and screen model decide page versus section ownership
- **AND** the server does not name an Angular/Flutter component or visual placement
- **AND** the matching page or section renders the warning without converting it into a blocking error

#### Scenario: Sales domain emits a limit or promotion notice

- **WHEN** core sales emits a notice about a limit, price, promotion, fee, or approval
- **THEN** the notice remains UI-agnostic
- **AND** the sales feature owns its placement
- **AND** core sales does not contain a web or mobile component target

### Requirement: Clients retain and route response feedback explicitly

Public, admin, platform, and mobile clients SHALL retain the successful response envelope until its
notices have been routed to an explicit owner.

#### Scenario: BFF response carries a section warning

- **WHEN** a client receives `ApiResponse<T>` with a targeted BFF notice
- **THEN** it does not discard the notice while unwrapping `data`
- **AND** it routes the notice to the declared section or page owner
- **AND** it does not show a duplicate shell notification

### Requirement: Feedback recovery is accessible and mobile-first

Blocking page failures and mutation outcomes SHALL use an owner-aware recovery model in web and
mobile clients.

#### Scenario: Read resource fails on a phone

- **WHEN** a blocking page resource fails on a narrow viewport
- **THEN** the recovery surface presents localized copy, an owner-declared retry when safe, a back
  action, and a copyable safe support reference without requiring hover or horizontal scrolling

#### Scenario: Form validation fails

- **WHEN** a backend validation response identifies invalid fields
- **THEN** the client renders the error in the form/field owner
- **AND** focus moves to the first invalid field
- **AND** a generic shell error is not shown in duplicate

### Requirement: Support references exist for blocking and non-blocking failures

Both blocking errors and non-blocking notices SHALL preserve support correlation identifiers whenever
they are available.

#### Scenario: User copies support reference for a warning

- **WHEN** a non-blocking warning is shown to an authenticated user
- **THEN** the copied support reference includes code, severity, source/domain, route, and available
  trace/request/error identifiers
- **AND** excludes stack traces, tokens, request bodies, response bodies, and personal data
