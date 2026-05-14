# Specification: core-offlinesync

## ADDED Requirements

### Requirement: Offline grant uses validated POS context

The system SHALL require `RequestOfflineGrantCommandHandler` to resolve POS context through `core.session` before issuing a grant.

#### Scenario: Grant request resolves POS frame

- **GIVEN** a grant request with POS headers
- **WHEN** the handler executes
- **THEN** it asks `ResolvePosOperationContextQuery` with action `REQUEST_OFFLINE_GRANT`
- **AND** uses the returned `ValidatedPosOperationContext` as the frame for the grant

### Requirement: Offline grant requires device proof

The system SHALL reject offline grants that rely only on weak client claims.

#### Scenario: Weak claim without device proof is rejected

- **GIVEN** a request with POS headers derived as `CLIENT_CLAIM/WEAK`
- **AND** no valid device binding or equivalent proof
- **WHEN** `RequestOfflineGrantCommandHandler` runs
- **THEN** no `OfflineGrant` is issued
- **AND** the handler returns a problem such as `offlinesync.device_proof_required`

### Requirement: Offline grant is not a ticket

The system SHALL treat an offline grant as a revocable authorization, not a sale.

#### Scenario: Grant issued without ticket creation

- **GIVEN** a valid offline grant request
- **WHEN** the grant is issued
- **THEN** an `offline_grant` record is persisted
- **AND** no ticket is created

### Requirement: Offline grant retrieval is tenant scoped

The system SHALL expose offline grant retrieval without leaking grants across tenants.

#### Scenario: Tenant retrieves an existing grant

- **GIVEN** an existing offline grant for the effective tenant
- **WHEN** the tenant calls the grant retrieval endpoint
- **THEN** the system returns the grant identifiers, status and validity window

#### Scenario: Tenant attempts to retrieve another tenant grant

- **GIVEN** an existing offline grant owned by another tenant
- **WHEN** the tenant calls the grant retrieval endpoint
- **THEN** the grant is not returned
- **AND** the response is equivalent to not found

### Requirement: Offline submission is not a ticket

The system SHALL store offline sales as submissions before creating official tickets.

#### Scenario: Submission accepted creates ticket later

- **GIVEN** an offline submission with valid grant, device proof, and signature
- **WHEN** technical and business validations pass
- **THEN** the system creates the real ticket through `core.sales`
- **AND** marks the submission accepted

#### Scenario: Submission rejected creates no ticket

- **GIVEN** an offline submission with invalid technical or business checks
- **WHEN** sync is processed
- **THEN** the submission is marked rejected or review-required
- **AND** no ticket is created
