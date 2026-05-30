# Spec — core.offlinesync

## ADDED Requirements

### Requirement: Offline grant request requires terminal device proof

`core.offlinesync` SHALL request device proof validation from `core.terminal` before issuing an offline grant.

#### Scenario: Valid grant request
- GIVEN a POS signed request with purpose `OFFLINE_GRANT_REQUEST`
- WHEN the backend validates terminal proof, session, seller, outlet, and offline eligibility
- THEN an offline grant MAY be issued.

#### Scenario: Invalid device proof
- GIVEN an invalid signature or replayed nonce
- WHEN a POS requests an offline grant
- THEN the grant SHALL be rejected
- AND no grant SHALL be persisted as usable.

### Requirement: Offline grant response is signed by backend

The backend SHALL sign offline grants using a backend private signing key, not the POS public key.

#### Scenario: Signed grant returned
- WHEN an offline grant is issued
- THEN the response SHALL include the grant payload, signature, signature algorithm, and keyId.

#### Scenario: POS verifies offline
- GIVEN a signed offline grant and a cached backend public key
- WHEN the POS goes offline
- THEN it SHALL verify the grant signature before using the grant.

### Requirement: Offline grants are scoped and bounded

Each grant SHALL include tenantId, terminalId, bindingId, outletId, sellerId, sessionId, businessDate, validFrom, validUntil, allowed purposes, and limits.

#### Scenario: Wrong terminal local usage
- GIVEN a grant for terminal A
- WHEN terminal B attempts to use it offline
- THEN the POS SHALL reject it locally
- AND backend SHALL reject it during sync if submitted.
