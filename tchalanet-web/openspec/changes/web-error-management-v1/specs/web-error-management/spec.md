# Web Error Management

## ADDED Requirements

### Requirement: Web errors are normalized before rendering

The web runtime SHALL normalize backend, network, validation, auth, and frontend runtime failures into
a canonical `WebAppError` presentation model before rendering user-facing error UI.

The web runtime SHALL consume the existing backend contract:

- `2xx` JSON responses use `ApiResponse<T>`;
- `4xx/5xx` failures use `ProblemDetail`.

It SHALL NOT redefine that contract.

#### Scenario: Backend returns a coded ProblemDetail

- **WHEN** an HTTP response fails with a backend `ProblemDetail` containing `code`
- **THEN** the frontend maps the error category from `code`
- **AND** preserves correlation fields such as `traceId`, `requestId`, and `errorId`
- **AND** does not parse `title` or `detail` to decide behavior

#### Scenario: Backend error has no stable code

- **WHEN** an HTTP response fails without a stable backend `code`
- **THEN** the frontend maps the error category from stable `type` when available
- **AND** otherwise falls back to HTTP status
- **AND** renders a generic localized message for unknown cases

#### Scenario: Successful response contains a non-blocking notice

- **WHEN** an `ApiResponse` succeeds with one or more `notices`
- **THEN** each user-visible notice is normalized with the same code/category pipeline as blocking
  errors
- **AND** exact notice-code translation is attempted before category fallback
- **AND** available support correlation metadata is preserved for diagnostics

### Requirement: Error surfaces have single ownership

Each failure SHALL be rendered by one owning surface: page, section/widget/card, field/form, or global
shell feedback.

Shell feedback SHALL NOT be the default renderer for every API failure.

Targeted errors SHALL carry enough placement metadata for deterministic local rendering:

- `surface`: `shell`, `page`, `section`, or `field`;
- `placement`: `top`, `inline`, or `summary`;
- `target`: stable UI target such as `dashboard.commissions` or `profile.email`;
- `field`: form control name when the target is a form field.

#### Scenario: Feature handles an error locally

- **WHEN** a page, section, or form catches an error and renders its own error state
- **THEN** the same failure does not also create a global shell banner
- **AND** the owning feature uses `suppressShellFeedback` or an equivalent boundary mechanism

#### Scenario: Route cannot continue

- **WHEN** the main routed content cannot be loaded or the user cannot access it
- **THEN** the error renders through the page-level error boundary
- **AND** no duplicate section-level or shell-level error is shown for the same failure

#### Scenario: Notice represents partial success

- **WHEN** a successful `ApiResponse` contains a non-blocking notice
- **THEN** the notice is rendered as non-blocking feedback
- **AND** it is not converted into a blocking page failure

#### Scenario: Dashboard slice fails without blocking the page

- **WHEN** a dashboard BFF returns primary data and a warning notice with `surface=section`
- **AND** the notice target is `dashboard.commissions`
- **THEN** the commissions block renders the warning in that block
- **AND** unrelated dashboard blocks remain usable
- **AND** the shell does not show a duplicate banner for the same notice

#### Scenario: Server returns field validation details

- **WHEN** a blocking `ProblemDetail` contains field validation entries
- **THEN** the web maps them to `surface=field` and `placement=inline`
- **AND** the owning form attaches each error to the matching control by `field` or `target`
- **AND** fields that cannot be matched remain available for page or summary fallback

### Requirement: Shell feedback is deduplicated and bounded

The shell feedback store SHALL deduplicate repeated failures and SHALL NOT render multiple visible
banners for the same error group.

The dedupe key SHALL be deterministic before grouping is applied.

#### Scenario: Same backend error repeats

- **WHEN** the same operation produces repeated errors with the same code, status, and source
- **THEN** the shell shows one feedback item
- **AND** increments or otherwise indicates the repeat count
- **AND** does not stack separate banners for each repeat

#### Scenario: Many unrelated errors occur

- **WHEN** more errors exist than the visible shell feedback cap
- **THEN** the shell shows the highest-priority visible items
- **AND** provides a compact overflow summary
- **AND** avoids filling the page with independent error banners

### Requirement: Error state ownership remains bounded

The web app SHALL NOT introduce a generic global error store that becomes a second app-wide state
system.

#### Scenario: Feature owns a local operation error

- **WHEN** a feature operation fails and the feature can render the failure locally
- **THEN** the error state remains owned by that feature or operation store
- **AND** reusable UI components receive normalized input instead of owning API error logic
- **AND** shell feedback is used only when the failure is cross-cutting

### Requirement: Error messages are user-safe and localized

User-visible error titles and messages SHALL come from frontend i18n keys based on category and known
stable backend code.

#### Scenario: Public user sees an error

- **WHEN** a public/minimal view renders an error
- **THEN** the message is localized and action-oriented
- **AND** technical status, source, stack, and backend diagnostic text are hidden
- **AND** raw exception messages, provider messages, SQL messages, and stack traces are not shown

#### Scenario: Privileged user views diagnostics

- **WHEN** a private standard or verbose diagnostic view renders an error
- **THEN** support correlation identifiers may be shown according to role verbosity
- **AND** sensitive request data, tokens, stack traces, and personal data are not shown

#### Scenario: Known non-blocking code is received

- **WHEN** an `ApiNotice` has a known code such as `platform.identity.activation.error`
- **THEN** the web app renders the exact localized title and message for that code
- **AND** uses `severity` to select warning, info, or error presentation
- **AND** does not show the raw backend notice message as the primary copy

#### Scenario: Temporary or unknown backend string is received

- **WHEN** a code is temporary, invented, or not part of a stable backend code catalog
- **THEN** the web app does not add exact-code product translation for it
- **AND** falls back to category or generic safe copy

### Requirement: Support actions do not change shell context unexpectedly

Error actions SHALL preserve the user's current shell context unless the action explicitly navigates
to a role-aware destination.

#### Scenario: Authenticated user uses a support action

- **WHEN** an authenticated user clicks an error support action
- **THEN** the user remains inside the private shell
- **AND** the action does not redirect to a public route
- **AND** the default support action copies a support reference instead of pretending to submit a trace

#### Scenario: Authenticated user opens support

- **WHEN** support navigation is available from a private error surface
- **THEN** it opens a private support/help destination
- **AND** it does not reuse a public support page that exits the private shell

#### Scenario: Public user uses an error action

- **WHEN** a public user clicks an error action
- **THEN** the user remains in the public shell
- **AND** no private diagnostics or privileged identifiers are exposed

#### Scenario: Public user opens support

- **WHEN** support navigation is available from a public error surface
- **THEN** it opens a public support/help destination
- **AND** it exposes only public-safe support reference copy

### Requirement: Diagnostic copy is support-safe

Copied diagnostic details SHALL include only support-safe references and SHALL exclude sensitive
runtime data.

#### Scenario: User copies support reference

- **WHEN** a standard or verbose private user copies diagnostics
- **THEN** the copied text includes timestamp, route, category, code, status, and available correlation
  IDs
- **AND** excludes stack traces, JWTs, request bodies, response bodies, and user-entered personal data

### Requirement: Retry actions are explicit and operation-owned

Retry controls SHALL be shown only when the owning operation marks the error as retryable.

#### Scenario: Non-idempotent operation fails

- **WHEN** a non-idempotent command fails
- **THEN** the error UI does not show a generic retry action
- **AND** the feature may present a domain-specific next step instead

### Requirement: Migration is representative before broad rollout

The implementation SHALL validate the model on one representative public flow and one authenticated
private flow before migrating broadly.

#### Scenario: First migration slice is selected

- **WHEN** implementation starts
- **THEN** it inventories duplicated error surfaces first
- **AND** migrates one public flow and one private authenticated flow
- **AND** broad feature migration waits until normalization, deduplication, redaction, suppression,
  and shell-preserving routing tests pass
