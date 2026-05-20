# Specification: common-context

## ADDED Requirements

### Requirement: POS operational context headers

The system SHALL define a single canonical set of POS operational-context header constants.

#### Scenario: Canonical POS headers are used

- **GIVEN** an HTTP POS request
- **WHEN** operational context is supplied
- **THEN** terminal, outlet, sales session, and operational source are read only from:
  - `X-Tch-Terminal-Id`
  - `X-Tch-Outlet-Id`
  - `X-Tch-Sales-Session-Id`
  - `X-Tch-Operational-Source`

### Requirement: Tenant override headers are separate

The system SHALL treat tenant override headers as tenant/effective-context inputs, not POS operational source inputs.

#### Scenario: Super-admin override does not become POS source

- **GIVEN** a request with `X-Tch-Tenant-Override`
- **AND** `X-Tch-Override-Reason`
- **WHEN** the request context is built
- **THEN** tenant override metadata MAY affect effective tenant after authorization
- **AND** `OperationalContextSource` MUST NOT contain `SUPER_ADMIN_OVERRIDE`

### Requirement: POS IDs are not read from body

The system SHALL reject POS-frame IDs in protected POS request bodies.

#### Scenario: POS IDs in body are rejected

- **GIVEN** a protected POS endpoint
- **WHEN** the request body contains `terminalId`, `outletId`, or `salesSessionId`
- **THEN** the request fails with `400`
- **AND** the problem code/type identifies `operational_context.in_body`

### Requirement: Source and trust are orthogonal

The system SHALL model POS operational source and trust as separate values.

#### Scenario: Source does not imply trust by enum constructor

- **GIVEN** `OperationalContextSource.SIGNED_DEVICE_BINDING`
- **WHEN** no valid binding proof is verified by the server
- **THEN** the request MUST NOT receive `OperationalContextTrust.STRONG`

### Requirement: Trust is server-derived

The system SHALL derive `OperationalContextTrust` only inside the operational context resolver.

#### Scenario: Client cannot declare trust

- **GIVEN** a request with header `X-Tch-Operational-Trust: STRONG`
- **WHEN** context is resolved
- **THEN** the header does not influence the derived trust

### Requirement: TchRequestContext carries only a hint

`TchRequestContext` SHALL carry an optional `OperationalContextHint`, not a validated POS frame.

#### Scenario: Handler resolves validated frame late

- **GIVEN** a handler requiring POS context
- **WHEN** it reads `TchRequestContext`
- **THEN** it can obtain only `OperationalContextHint`
- **AND** it must ask `core.session` for `ValidatedPosOperationContext`
