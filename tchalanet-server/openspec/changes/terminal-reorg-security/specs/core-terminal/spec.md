# Spec — core.terminal

## ADDED Requirements

### Requirement: Terminal controllers are separated by responsibility

`core.terminal` SHALL expose separate controllers for lifecycle, assignment, metadata, operational controls, query, activation, and runtime operations.

#### Scenario: Create terminal endpoint
- WHEN a tenant admin creates a terminal
- THEN the request SHALL be handled by `TerminalAdminLifecycleController`
- AND the route SHALL be `POST /admin/terminals`
- AND the controller SHALL dispatch `RegisterTerminalCommand` through `CommandBus`.

#### Scenario: Operational control endpoint
- WHEN an admin blocks or unblocks a terminal operational control
- THEN the route SHALL be `PATCH /admin/terminals/{terminalId}/operational-controls/{control}`
- AND the controller SHALL NOT contain create/retire/lock/unlock actions.

### Requirement: Terminal activation challenges are temporary and server-side

`core.terminal` SHALL generate terminal activation challenges with a short-lived code hash, max attempts, expiry, and consumed timestamp.

#### Scenario: POS pairing challenge
- WHEN a POS activation challenge is requested
- THEN the challenge type SHALL be `POS_PAIRING`
- AND default delivery mode SHALL be `E2E` outside production unless configured otherwise
- AND the resulting binding type on successful verification SHALL be `POS_DEVICE`.

#### Scenario: Mobile OTP challenge
- WHEN a phone activation challenge is requested
- THEN the challenge type SHALL be `MOBILE_OTP`
- AND default delivery mode SHALL be `LIVE`
- AND the resulting binding type on successful verification SHALL be `MOBILE_APP`.

### Requirement: Verify challenge validates terminal URL identity

The verify command SHALL include `tenantId`, `terminalId`, and `challengeId`.

#### Scenario: Terminal mismatch
- GIVEN a challenge created for terminal A
- WHEN a client verifies it using terminal B in the URL
- THEN verification SHALL fail with `activation_challenge.terminal_mismatch`
- AND no binding SHALL be created.

### Requirement: Terminal binding stores POS/mobile public key material

`terminal_binding` SHALL store the POS/mobile public key metadata when provided.

Required fields for signature-ready binding:

- `binding_public_key`
- `public_key_algorithm`
- `public_key_hash`
- `credential_hash`
- `device_fingerprint_hash`
- `state`

#### Scenario: Public key provided
- WHEN a challenge is verified with a `bindingPublicKey`
- THEN backend SHALL compute `publicKeyHash`
- AND store the public key, algorithm, and hash on the binding.

### Requirement: Device proof validation is owned by core.terminal

`core.terminal` SHALL expose a stable query/handler for validating device proof signatures.

#### Scenario: Valid signed POS request
- GIVEN a terminal binding with active public key
- WHEN a request contains terminal id, binding id, purpose, timestamp, nonce, body hash, and signature
- THEN `core.terminal` SHALL verify the signature and return `Trusted`.

#### Scenario: Replay detected
- GIVEN a nonce already used for a binding
- WHEN the same nonce is used again
- THEN validation SHALL fail with `terminal.device_replay_detected`.

#### Scenario: Wrong purpose
- GIVEN a signature for `SELL_TICKET`
- WHEN it is submitted to a `PAYOUT_CONFIRM` endpoint
- THEN validation SHALL fail with `terminal.device_purpose_mismatch`.

### Requirement: Runtime sync-state persists request body

`POST /tenant/terminals/{terminalId}/sync-state` SHALL not only update heartbeat. It SHALL persist or update sync status fields from the request body.

#### Scenario: Sync pending report
- WHEN POS reports pending sync count and last synced timestamp
- THEN terminal runtime status SHALL reflect sync pending state
- AND lastSeen SHALL also be updated.
