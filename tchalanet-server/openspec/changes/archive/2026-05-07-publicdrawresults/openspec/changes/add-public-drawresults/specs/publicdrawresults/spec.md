# Spec — features.publicdrawresults

## ADDED Requirements

### Requirement: Public draw results feature package

The backend SHALL provide a vertical feature package named `features.publicdrawresults` for public result display.

#### Scenario: Public result feature uses drawresult core

- **GIVEN** a public client requests draw result information
- **WHEN** `features.publicdrawresults` needs result data
- **THEN** it SHALL call `core.drawresult` through `QueryBus`
- **AND** it SHALL NOT access `core.draw` persistence directly
- **AND** it SHALL NOT access `draw_channel` persistence directly
- **AND** it SHALL NOT require a tenant context.

### Requirement: Public slots response supports optional history

The public draw result slots response SHALL represent each active `result_slot` as one item containing slot metadata, latest result, next expected result time/countdown, and optionally history.

#### Scenario: Light public slots response

- **GIVEN** `includeHistory=false`
- **WHEN** public slots are requested
- **THEN** each item SHALL include slot metadata, latest result, and next expected result time/countdown
- **AND** `history` SHALL be an empty list
- **AND** the backend SHALL NOT execute a history lookup.

#### Scenario: Detailed public slots response

- **GIVEN** `includeHistory=true`
- **WHEN** public slots are requested with `historyLimit=5`
- **THEN** each item SHALL include slot metadata, latest result, next expected result time/countdown, and up to 5 recent historical results.

#### Scenario: History limit capped

- **GIVEN** `includeHistory=true`
- **AND** `historyLimit` is greater than 10
- **WHEN** public slots are requested
- **THEN** the server SHALL cap `historyLimit` to 10.

#### Scenario: History limit defaulted

- **GIVEN** `includeHistory=true`
- **AND** `historyLimit` is missing or invalid
- **WHEN** public slots are requested
- **THEN** the server SHALL default `historyLimit` to 5.

### Requirement: Public slots endpoint is optional for PageModel but available for clients

The PageModel provider SHALL call the internal query directly, while public/mobile/terminal clients MAY consume an HTTP endpoint.

#### Scenario: PageModel provider does not call HTTP endpoint

- **GIVEN** the public home PageModel renders draw result widgets
- **WHEN** the PageModel provider loads result data
- **THEN** it SHALL call `QueryBus.send(new ListPublicDrawResultSlotsQuery(...))`
- **AND** it SHALL NOT call `/api/v1/public/draw-results/slots` over HTTP.

#### Scenario: Mobile client requests detailed slots

- **GIVEN** a mobile client requests public draw result details
- **WHEN** it calls `GET /api/v1/public/draw-results/slots?includeHistory=true&historyLimit=5`
- **THEN** the response SHALL include recent history up to 5 items per slot.

### Requirement: Public advanced history search

The feature SHALL provide a paginated advanced history search for past results.

#### Scenario: Search history by date range

- **GIVEN** a public user searches results from `2026-05-01` to `2026-05-05`
- **WHEN** the user calls `/api/v1/public/draw-results/history`
- **THEN** the response SHALL be paginated
- **AND** it SHALL filter by date range and optional slot/provider filters.

#### Scenario: Search range too large

- **GIVEN** a public history search date range exceeds the configured maximum
- **WHEN** the request is processed
- **THEN** the API SHALL reject or clamp the range according to public API policy
- **AND** it SHALL avoid unbounded public queries.

### Requirement: Public DTOs hide internal identifiers

Public response DTOs SHALL NOT expose internal UUIDs by default.

#### Scenario: Public slots response

- **GIVEN** a public slots response
- **WHEN** it is serialized
- **THEN** it SHALL NOT include `drawResultId`, `resultSlotId`, `tenantId`, `drawId`, or `drawChannelId` fields unless explicitly approved in a later change.
