# offlinesync — Spec Delta

> **Version 2** — incorpore Ed25519 device crypto, séparation `validUntil`/`syncAcceptedUntil`, idempotence stricte avec hash, `promotionAttemptId`, cycle de vie codes refondu.

## ADDED Requirements

### Requirement: Grant emission with device public key

The system SHALL emit an `OfflineGrant` to authorize a tuple `(seller, terminal, device)` to perform offline sales during a bounded period.

A Grant SHALL be emitted only when:
- The tenant's plan enables offline sales (BUSINESS or PREMIUM)
- The POS context is valid (validated by `core.session`)
- No active Grant exists for the same trio
- The tenant's quota allows emission
- The device provides a valid Ed25519 public key

A Grant SHALL contain:
- The device's Ed25519 public key (server stores no signing secret)
- A server signature (Ed25519) of the grant payload, verifiable by the device
- Validity bounds (`validFrom`, `validUntil`)
- A separate sync acceptance window (`syncAcceptedUntil`)
- Quantitative limits (`maxTicketCount`, `maxTotalAmount`)
- Reference to an `OfflineCodeBatch` containing pre-allocated codes

The server SHALL NEVER store any secret capable of forging device submissions.

#### Scenario: Successful grant emission for BUSINESS tenant

- **GIVEN** a tenant on BUSINESS plan with offline enabled
- **AND** a valid POS context
- **AND** no active grant for the trio
- **AND** the device provides a valid Ed25519 public key
- **WHEN** the seller requests an offline grant
- **THEN** a new `OfflineGrant` is created with status ACTIVE
- **AND** an `OfflineCodeBatch` of 100 codes is created
- **AND** validUntil is set to now + 8 hours
- **AND** syncAcceptedUntil is set to validUntil + 7 days
- **AND** the response contains grantSignature, server-side metadata, and the list of codes
- **AND** no signing secret is stored server-side

#### Scenario: Grant emission denied for BASIC tenant

- **GIVEN** a tenant on BASIC plan
- **WHEN** the seller requests an offline grant
- **THEN** the system returns error `OFFLINE_NOT_ENABLED` with HTTP 403

#### Scenario: Grant emission denied with invalid device public key

- **GIVEN** a request with malformed or missing devicePublicKey
- **WHEN** the seller requests an offline grant
- **THEN** the system returns error `DEVICE_PUBLIC_KEY_INVALID` with HTTP 400

### Requirement: Grant renewal with supersession

The system SHALL allow renewal of a Grant before its expiration. Renewal SHALL:
- Mark the previous grant as `SUPERSEDED` with `supersededAt` and `supersededByGrantId`
- Set unused codes from the previous batch to EXPIRED
- Issue a new Grant with a new code batch
- Preserve the previous grant's ability to accept syncs for sales made within its validity window, up to its `syncAcceptedUntil`

#### Scenario: Successful renewal preserves previous grant for sync

- **GIVEN** an active grant approaching expiration
- **WHEN** the device requests a renewal
- **THEN** a new grant is issued
- **AND** the previous grant is marked SUPERSEDED
- **AND** sales made during the previous grant's validity can still sync until its syncAcceptedUntil

### Requirement: Distinct validity and sync acceptance windows

The system SHALL maintain two distinct temporal bounds on a Grant:

- `validUntil`: the latest moment a device may create a new offline sale referencing this grant
- `syncAcceptedUntil`: the latest moment the server will accept a submission referencing this grant

The default duration between `validUntil` and `syncAcceptedUntil` SHALL be 7 days, configurable per tenant via `core.limitpolicy`.

A submission SHALL be accepted technically when:
- `clientSoldAt ∈ [validFrom - clock_skew_tolerance, validUntil + grace_period]`
- `receivedAt <= syncAcceptedUntil`

#### Scenario: Sale made during validity, synced after expiration

- **GIVEN** a grant with validUntil = T and syncAcceptedUntil = T + 7 days
- **AND** a submission with clientSoldAt = T - 1 hour
- **AND** the submission is received at T + 5 days
- **WHEN** sync processes the submission
- **THEN** the technical validation succeeds (within syncAcceptedUntil)
- **AND** the submission proceeds to business validation

#### Scenario: Sale attempted after grant validity

- **GIVEN** a grant with validUntil = T
- **AND** a submission with clientSoldAt = T + 1 hour (outside grace period)
- **WHEN** sync processes the submission
- **THEN** the technical validation rejects with code `CLIENT_SOLD_AT_AFTER_GRANT`

#### Scenario: Sync after sync acceptance window closed

- **GIVEN** a grant with syncAcceptedUntil = T + 7 days
- **AND** a submission with clientSoldAt within validity
- **AND** the submission is received at T + 8 days
- **WHEN** sync processes the submission
- **THEN** the technical validation rejects with code `GRANT_EXPIRED`

### Requirement: Grant revocation

The system SHALL allow administrators to revoke an active Grant with a documented reason.

Revocation SHALL:
- Set grant status to REVOKED with `revokedAt`, `revokedBy`, `revokedReason`
- Set all AVAILABLE codes in the associated batch to VOIDED
- Not invalidate sales already made offline with this grant before `revokedAt`
- Reject sales attempted after `revokedAt`

#### Scenario: Admin revokes an active grant

- **GIVEN** an active grant
- **WHEN** an admin calls revoke with a reason
- **THEN** the grant status becomes REVOKED
- **AND** all AVAILABLE codes in the batch become VOIDED
- **AND** submissions with clientSoldAt < revokedAt can still sync
- **AND** submissions with clientSoldAt >= revokedAt are rejected with `GRANT_INACTIVE`

### Requirement: Offline code lifecycle without return to AVAILABLE

The system SHALL never return an offline code to `AVAILABLE` status once a submission referencing it has been received.

Allowed transitions:
```
AVAILABLE → RESERVED         (during technical validation)
AVAILABLE → EXPIRED          (batch expired without use)
AVAILABLE → VOIDED           (grant revoked before use)
RESERVED  → CONSUMED_PROMOTED  (TECH_VALIDATED + BUSINESS_ACCEPTED)
RESERVED  → CONSUMED_REJECTED  (TECH_REJECTED or BUSINESS_REJECTED)
```

Forbidden transitions:
```
RESERVED → AVAILABLE
CONSUMED_* → any state
EXPIRED → any state
VOIDED → any state
```

#### Scenario: Technical rejection consumes the code

- **GIVEN** a submission with an invalid signature
- **WHEN** technical validation rejects it
- **THEN** the code transitions to CONSUMED_REJECTED (not back to AVAILABLE)

#### Scenario: Orphaned reservation recovery

- **GIVEN** a code in RESERVED status for more than 10 minutes
- **AND** no associated submission has been fully processed
- **WHEN** the `OrphanedCodeReservationJob` runs
- **THEN** the code transitions to CONSUMED_REJECTED with reason "ORPHANED_RESERVATION"

### Requirement: Offline code uniqueness and format

The system SHALL allocate offline codes that are:
- 9 characters in 3-3-3 groups (e.g. `A7K-3FH-92Q`)
- From an anti-confusion alphabet (no `O`, `0`, `I`, `1`, `L`)
- Globally unique (PRIMARY KEY constraint)
- Pre-allocated by the server only
- Cryptographically random within the alphabet

### Requirement: Strict idempotency with payload hashes

The system SHALL implement strict idempotency at two levels using content hashes.

**Batch level:**
- `(tenantId, clientBatchId)` is UNIQUE
- Each batch carries a `batchPayloadHash`
- Retry with matching hash → return previous result
- Retry with mismatched hash → error `BATCH_IDEMPOTENCY_CONFLICT`

**Submission level:**
- `(tenantId, clientSubmissionId)` is UNIQUE
- Each submission carries a `payloadHash`
- Retry with matching hash → API result `DUPLICATE` with pointer to original (no row created)
- Retry with mismatched hash → error `SUBMISSION_IDEMPOTENCY_CONFLICT`

`DUPLICATE` SHALL be an API result, not a persistent submission state.

#### Scenario: Batch retry with same content

- **GIVEN** a batch was processed with clientBatchId B1 and batchPayloadHash H1
- **WHEN** the device retries with same B1 and H1
- **THEN** the server returns the original results
- **AND** no new processing occurs

#### Scenario: Batch retry with different content

- **GIVEN** a batch was processed with clientBatchId B1 and batchPayloadHash H1
- **WHEN** another request arrives with same B1 but batchPayloadHash H2 ≠ H1
- **THEN** the server returns error `BATCH_IDEMPOTENCY_CONFLICT`
- **AND** no submissions from H2 are processed

#### Scenario: Duplicate submission with same payload

- **GIVEN** a submission with clientSubmissionId S1 and payloadHash P1 is already persisted
- **WHEN** another submission arrives with same S1 and P1
- **THEN** the API returns `{"serverStatus": "DUPLICATE", "originalSubmissionId": "..."}`
- **AND** no new row is created in `offline_submission`

#### Scenario: Duplicate submission with different payload

- **GIVEN** a submission with clientSubmissionId S1 and payloadHash P1 is persisted
- **WHEN** another submission arrives with same S1 but payloadHash P2 ≠ P1
- **THEN** the system returns error `SUBMISSION_IDEMPOTENCY_CONFLICT`

### Requirement: Ed25519 cryptographic signatures

The system SHALL use Ed25519 signatures with the following properties:

- The device generates its keypair in Android Keystore (private key non-exportable)
- The device transmits its public key to the server during grant emission
- The server stores only the device public key, never a signing secret
- Each submission is signed by the device: `signature = Ed25519.sign(devicePrivateKey, payloadHash)`
- The server verifies: `Ed25519.verify(devicePublicKey, signature, payloadHash)`
- The server signs grants with its own private key (KMS-managed)
- The device verifies grant signatures with the server public key embedded in the app

The payload signed SHALL include all of: `tenantId, grantId, codeBatchId, offlineCode, clientSubmissionId, clientBatchId, sellerUserId, terminalId, outletId, deviceId, clientSoldAt, lines, totalStakeAmount, lineCount, schemaVersion, signatureAlgorithm, canonicalizationVersion, keyId`.

#### Scenario: Backend compromise does not enable forgery

- **GIVEN** an attacker has full read access to the backend database
- **WHEN** the attacker attempts to forge a submission signature
- **THEN** the attacker cannot produce a valid signature
- **AND** the attack is detected by `SIGNATURE_INVALID` rejection

#### Scenario: Forged signature detected

- **GIVEN** a submission with a tampered payload
- **WHEN** sync processes it
- **THEN** technical validation rejects with `SIGNATURE_INVALID`
- **AND** the code transitions to CONSUMED_REJECTED

### Requirement: Submission technical validation

The system SHALL technically validate every submission in 15 steps (short-circuit on first failure):

1. Idempotency check on `clientSubmissionId` (with payloadHash comparison)
2. Tenant coherence (grant and code belong to request tenant)
3. Grant existence
4. Grant usable status (ACTIVE or SUPERSEDED)
5. Sale creation window (`clientSoldAt` within validity bounds)
6. Sync acceptance window (`receivedAt <= syncAcceptedUntil`)
7. Context coherence (terminal/seller/device match grant)
8. Code existence in grant's batch
9. Code usable status (AVAILABLE)
10. Supported schema versions
11. Payload hash recomputed matches received hash
12. Ed25519 signature verifies
13. Denormalization coherence (`totalStakeAmount`, `lineCount`)
14. Lines validity (non-empty, `stakeAmount > 0`)
15. Grant quotas not exceeded

Failed submissions SHALL be persisted with `technicalStatus = REJECTED` and a normalized `rejectionCode`.

### Requirement: Self-contained promotion events

The `OfflineSubmissionTechValidatedEvent` SHALL contain all data needed by `core.sales` to create the ticket, with no need for queries back to `offlinesync`.

The event payload SHALL include:
- `eventId`, `promotionAttemptId`
- `submissionId`, `tenantId`, `clientSubmissionId`
- `grantId`, `offlineCode`
- `sellerUserId`, `terminalId`, `outletId`, `deviceId`
- `clientSoldAt`, `receivedAt`
- Full `lines` array
- `totalStakeAmount`, `lineCount`
- `emittedAt`

### Requirement: Strictly idempotent promotion via events

The system SHALL guarantee that promotion via events is strictly idempotent:

- Each promotion attempt SHALL be identified by a unique `promotionAttemptId`
- The `OfflineSubmissionProcessedEvent` SHALL cite this `promotionAttemptId`
- `core.sales` SHALL maintain a UNIQUE constraint on `(tenant_id, offline_submission_id)` on its ticket table
- The `OfflineSubmissionProcessedEventListener` in `offlinesync` SHALL implement the following idempotency logic:

```
If event.promotionAttemptId != submission.promotionAttemptId:
  → obsolete event, ignore (log warning)

If event.id == submission.lastPromotionEventId:
  → already processed, no-op

If submission.status == PROMOTED_TO_TICKET:
  If outcome == PROMOTED AND event.ticketId == submission.createdTicketId:
    → no-op (normal replay)
  If outcome == PROMOTED AND event.ticketId != submission.createdTicketId:
    → CRITICAL INCIDENT
  If outcome == BUSINESS_REJECTED:
    → CRITICAL INCIDENT

Else:
  → apply outcome, persist event.id as lastPromotionEventId
```

Critical incidents SHALL trigger an on-call page via `platform.communication`.

#### Scenario: Duplicate processed event is no-op

- **GIVEN** a submission already in PROMOTED_TO_TICKET with ticketId T1
- **WHEN** a `ProcessedEvent(outcome=PROMOTED, ticketId=T1)` is received again
- **THEN** the listener detects the matching state and performs no operation

#### Scenario: Conflicting ticket IDs trigger incident

- **GIVEN** a submission in PROMOTED_TO_TICKET with ticketId T1
- **WHEN** a `ProcessedEvent(outcome=PROMOTED, ticketId=T2 ≠ T1)` is received
- **THEN** a critical incident is logged
- **AND** an on-call alert is sent
- **AND** the submission state is NOT modified

#### Scenario: UNIQUE constraint prevents double ticket creation

- **GIVEN** a ticket already exists for `offline_submission_id = S1`
- **WHEN** the sales listener tries to insert another ticket for S1
- **THEN** the database UNIQUE constraint rejects the insert
- **AND** the listener catches the error and publishes `ProcessedEvent` matching the existing ticket

### Requirement: Single grant per batch

All submissions within an `OfflineSyncBatch` SHALL share:
- `grantId`
- `codeBatchId`
- `sellerUserId`
- `terminalId`
- `outletId`
- `deviceId`

If a batch contains a submission with a divergent context, the entire batch SHALL be rejected with error `BATCH_CONTEXT_MISMATCH`.

#### Scenario: Mixed-grant batch rejected

- **GIVEN** a batch with two submissions referencing different grantIds
- **WHEN** sync processes the batch
- **THEN** the entire batch is rejected with `BATCH_CONTEXT_MISMATCH`

### Requirement: Admin approval workflow

The system SHALL allow administrators to approve a NEEDS_REVIEW submission. Approval SHALL:
- Require a non-empty `reason`
- Create an `OfflineSubmissionDecision(decision=APPROVE, dryRun=false)`
- Generate a new `promotionAttemptId`
- Publish a self-contained `OfflineSubmissionAdminApprovedEvent`

### Requirement: Admin rejection workflow

Identical to v1, with status transition to BUSINESS_REJECTED and code → CONSUMED_REJECTED.

### Requirement: Replay in dry-run mode with audit trace

The system SHALL allow administrators to replay a rejected submission in dry-run mode. Dry-run replay SHALL:
- Re-execute technical validation and promotion simulation in memory
- Return a detailed report
- Make no metier changes (no status updates, no events, no code changes)
- Create an `OfflineSubmissionDecision(decision=REPLAY, dryRun=true, reportJson=...)` for audit
- Set `previousStatus == newStatus` (allowed exclusively in dry-run)

#### Scenario: Dry-run replay creates audit trace without changing state

- **GIVEN** a submission in TECH_REJECTED status
- **WHEN** an admin runs replay in dry-run mode
- **THEN** an `OfflineSubmissionDecision(decision=REPLAY, dryRun=true)` is created
- **AND** the submission status remains TECH_REJECTED
- **AND** no events are published
- **AND** no codes are modified

### Requirement: Auditability

All offlinesync entities SHALL be audited via Hibernate Envers, with the following `@NotAudited` fields (volume reasons):
- `OfflineSyncBatch.rawManifest`
- `OfflineSubmission.rawPayload`
- `OfflineSubmission.signature`
- `OfflineGrant.grantSignature`

The `OfflineGrant.devicePublicKey` SHALL be audited (important security event when changed).

### Requirement: Atomic offline sale with persisted signature

The Flutter app SHALL guarantee atomic local persistence of all critical sale data **before** printing:

```
Within a single SQLite transaction:
  1. Reserve next AVAILABLE code
  2. Create submission with payloadHash AND signature already computed
  3. Create lines
  4. Mark code CONSUMED locally
  5. Increment grant counters locally
Commit.
Then (outside transaction):
  6. Print ticket
  7. Mark localStatus = PRINTED_PENDING_SYNC
```

If a crash occurs after commit but before successful print, the app SHALL detect unprinted submissions at startup and offer reprint.

The signature SHALL be persisted in the same transaction as the code consumption. This guarantees that even if Android Keystore later becomes unavailable, the submission remains syncable.

#### Scenario: Crash between persist and print, signature already saved

- **GIVEN** an offline sale where the transaction commits successfully with signature persisted
- **AND** the app crashes before printer confirmation
- **WHEN** the app restarts
- **THEN** the unprinted submission is detected with intact signature
- **AND** the submission can be reprinted and synced normally

### Requirement: Persistent client batch ID before HTTP call

The Flutter app SHALL persist `clientBatchId` to local storage BEFORE making the sync HTTP request. On retry, the same `clientBatchId` SHALL be reused.

#### Scenario: Retry after timeout reuses clientBatchId

- **GIVEN** a sync request was sent with clientBatchId B1
- **AND** the HTTP request timed out
- **WHEN** the app retries the sync
- **THEN** the same clientBatchId B1 is used
- **AND** the server returns the result via idempotency if B1 was processed

### Requirement: SYNCING zombie recovery (Flutter)

If a submission stays in `SYNCING` status locally for more than 15 minutes without transition, the Flutter app SHALL transition it back to `PRINTED_PENDING_SYNC` to allow retry.

### Requirement: Local status enrichment (Flutter)

The Flutter app SHALL track submissions through 9 distinct local statuses:
`DRAFT, COMMITTED_NOT_PRINTED, PRINTED_PENDING_SYNC, SYNCING, TECH_ACCEPTED, PROMOTED, REJECTED, SYNC_FAILED_RETRYABLE, SYNC_FAILED_FINAL`

### Requirement: Forced offline mode (Flutter)

Identical to v1: user-toggleable, persistent visual indicator, immediate sync on deactivation.

### Requirement: Duplicate ticket reprint (Flutter)

Identical to v1: 7-day window, DUPLICATA marker, local reprint count.

### Requirement: Local data retention (Flutter)

Identical to v1: PENDING_SYNC kept indefinitely, SYNCED kept 7 days, consumed codes kept 7 days post-batch-expiration.

### Requirement: UI protection against data loss

The Flutter app SHALL prevent accidental loss of unsynced submissions:
- Display a persistent badge showing pending sync count
- Block logout / clear data flows if pending submissions exist (with admin-coded override)
- Send a system notification if pending sync persists more than 24 hours

### Requirement: Server-side parameters from limitpolicy

The system SHALL fetch offline parameters from `core.limitpolicy` via `GetOfflineLimitPolicyQuery`. Hardcoded plan-specific values in `core.offlinesync` are forbidden.

The policy SHALL provide:
- `offlineEnabled: boolean`
- `batchSize: int`
- `validityDuration: Duration`
- `syncAcceptedExtension: Duration` (new in v2, default 7 days)
- `maxTicketCount: int`
- `maxTotalAmount: BigDecimal`

### Requirement: Extended standardized error codes

The system SHALL return errors with normalized codes covering categories:

**Grant emission:** `OFFLINE_NOT_ENABLED`, `EXISTING_GRANT_ACTIVE`, `SESSION_INVALID`, `TENANT_QUOTA_EXCEEDED`, `DEVICE_NOT_REGISTERED`, `DEVICE_PUBLIC_KEY_INVALID`, `SELLER_BANNED_OFFLINE`

**Batch sync:** `BATCH_DUPLICATE`, `BATCH_EMPTY`, `BATCH_TOO_LARGE`, `BATCH_CONTEXT_MISMATCH`, `BATCH_PAYLOAD_INVALID`, `BATCH_SCHEMA_VERSION_UNSUPPORTED`, `BATCH_IDEMPOTENCY_CONFLICT`

**Submission technical:** `SUBMISSION_IDEMPOTENCY_CONFLICT`, `OFFLINE_CODE_REQUIRED`, `GRANT_UNKNOWN`, `GRANT_INACTIVE`, `GRANT_EXPIRED`, `GRANT_QUOTA_EXCEEDED`, `GRANT_TENANT_MISMATCH`, `CONTEXT_MISMATCH`, `CODE_INVALID`, `CODE_ALREADY_RESERVED`, `CODE_VOIDED`, `CODE_EXPIRED`, `CODE_TENANT_MISMATCH`, `CODE_BATCH_EXPIRED`, `CODE_BATCH_REVOKED`, `PAYLOAD_HASH_MISMATCH`, `CANONICALIZATION_FAILED`, `SIGNATURE_INVALID`, `SIGNATURE_ALGORITHM_UNSUPPORTED`, `CLIENT_SOLD_AT_IN_FUTURE`, `CLIENT_SOLD_AT_BEFORE_GRANT`, `CLIENT_SOLD_AT_AFTER_GRANT`, `DEVICE_CLOCK_UNTRUSTED`, `LINES_INVALID`, `LINE_COUNT_MISMATCH`, `TOTAL_AMOUNT_MISMATCH`, `RAW_PAYLOAD_TOO_LARGE`

**Business (from sales):** `DRAW_CLOSED`, `LIMIT_EXCEEDED`, `GAME_UNKNOWN`, `BET_INVALID`, `STAKE_OUT_OF_RANGE`

**Promotion:** `PROMOTION_TIMEOUT`, `SALES_UNAVAILABLE`, `PROMOTION_DUPLICATE_TICKET`, `PROMOTION_CONFLICT`, `TICKET_ALREADY_EXISTS_FOR_SUBMISSION`, `ADMIN_APPROVAL_REQUIRED`, `ADMIN_APPROVAL_FAILED`

### Requirement: Observability with critical incident metrics

The system SHALL expose metrics via Micrometer including:
- All v1 metrics
- `offlinesync_idempotency_conflicts_total`
- `offlinesync_orphaned_codes_total`
- `offlinesync_double_ticket_incidents_total`
- `offlinesync_grants_superseded_total`

Critical alerts SHALL include:
- `DoubleTicketIncident` (≥ 1, page on-call critical)
- `IdempotencyConflicts` (> 10/hour)
- `OrphanedCodes` (> 5/hour)
- `StuckSubmissions` (> 0 for > 10 minutes)
- `NeedsReviewBacklog` (> 50 open > 24 hours per tenant)

### Requirement: Modulith boundaries

The system SHALL respect Spring Modulith isolation rules:
- No access to other modules' `internal` packages or tables
- Synchronous calls only via `api.command` / `api.query` public interfaces
- Asynchronous workflows only via `api.event` events

`core.offlinesync` MAY consume:
- `core.session.ResolvePosOperationContextQuery`
- `core.limitpolicy.GetOfflineLimitPolicyQuery`

`core.sales` SHALL NOT call queries back to `core.offlinesync` — events are self-contained.
