# Specification Delta — features.cashier

## ADDED Requirements

### Requirement: Cashier package is organized by POS journey slices

`features.cashier` SHALL be organized into the following vertical slices:

```text
session
draws
tickets
offline
operationalcontext
```

Each slice SHOULD use `web/`, `app/`, `model/`, and optionally `mapper/`.

#### Scenario: Cashier tickets slice owns POS ticket operations

- **GIVEN** a seller is operating the POS
- **WHEN** the seller previews, sells, lists, gets, cancels, prints, or sends a ticket
- **THEN** the endpoint SHALL live under `features.cashier.tickets`

### Requirement: Cashier endpoints use trusted operational context

Cashier preview, sell, cancel, print, and send operations SHALL require a trusted operational context containing terminal, outlet, and sales session.

#### Scenario: Missing trusted context blocks preview

- **GIVEN** an authenticated cashier without trusted POS operational context
- **WHEN** they call `POST /tenant/cashier/tickets/preview`
- **THEN** the request SHALL fail with an operational context error

#### Scenario: Missing trusted context blocks sell

- **GIVEN** an authenticated cashier without trusted POS operational context
- **WHEN** they call `POST /tenant/cashier/tickets/sell`
- **THEN** the request SHALL fail before creating any ticket

### Requirement: Cashier preview is best-effort and read-only

`POST /tenant/cashier/tickets/preview` SHALL be read-only and SHALL NOT reserve exposure or lock exposure rows.

#### Scenario: Acceptable preview includes warning

- **GIVEN** a basket that is acceptable at preview time
- **WHEN** the seller previews the basket
- **THEN** the response SHALL include `decision = ACCEPTABLE`
- **AND** a warning that concurrent sales can modify available limits

### Requirement: Cashier sell requires idempotency

`POST /tenant/cashier/tickets/sell` SHALL require an `Idempotency-Key` header.

#### Scenario: Missing idempotency key fails sell

- **GIVEN** a valid basket
- **WHEN** the seller calls sell without `Idempotency-Key`
- **THEN** the request SHALL fail with `idempotency.missing`
- **AND** no ticket SHALL be created

#### Scenario: Same key and payload replays same ticket

- **GIVEN** a successful sell request with an idempotency key
- **WHEN** the same request is retried with the same key and same payload
- **THEN** the same ticket result SHALL be replayed

#### Scenario: Same key and different payload conflicts

- **GIVEN** a successful sell request with an idempotency key
- **WHEN** a different payload is submitted with the same key
- **THEN** the request SHALL fail with `idempotency.payload_mismatch`

### Requirement: Accepted sell response includes backup proof

Every accepted cashier sell response SHALL include backup proof content usable even if print/send fails.

#### Scenario: Accepted sale returns backup

- **GIVEN** a basket that sells successfully
- **WHEN** the backend returns `outcome = ACCEPTED`
- **THEN** the response SHALL include `backup.displayCode`
- **AND** `backup.verificationShortUrl`
- **AND** `backup.shareableText`
- **AND** seller instructions

### Requirement: Cashier POS does not expose pending approval

Cashier POS SHALL NOT expose `PENDING_APPROVAL` to the seller.

#### Scenario: Approval required appears as requires changes

- **GIVEN** a basket that would require approval in a non-POS channel
- **WHEN** a cashier previews or sells the basket
- **THEN** the response SHALL surface `APPROVAL_REQUIRED` as a change-required issue
- **AND** the seller SHALL be instructed to reduce the basket or contact admin
- **AND** no customer proof SHALL be issued

### Requirement: Rejected sell disables delivery actions

A rejected sale SHALL NOT expose backup content and SHALL disable print/send/copy actions.

#### Scenario: Rejected sale has no backup

- **GIVEN** a basket that is rejected
- **WHEN** sell returns `outcome = REJECTED`
- **THEN** `backup` SHALL be null
- **AND** `canPrint`, `canSendSms`, `canSendWhatsapp`, `canSendEmail`, and `canCopy` SHALL be false

### Requirement: Print returns binary response

`POST /tenant/cashier/tickets/{ticketId}/print` SHALL return binary content and SHALL NOT wrap the response in `ApiResponse`.

#### Scenario: Print accepted ticket

- **GIVEN** an accepted printable ticket
- **WHEN** the seller prints the ticket
- **THEN** the backend SHALL return binary content
- **AND** `Cache-Control: no-store`
- **AND** a `Content-Disposition` filename based on the display code

### Requirement: Send uses text-only ticket proof in V1

`POST /tenant/cashier/tickets/{ticketId}/send` SHALL enqueue a text-only receipt message in V1.

#### Scenario: WhatsApp send is text-only

- **GIVEN** an accepted sendable ticket
- **WHEN** the seller sends by WhatsApp
- **THEN** the communication request SHALL contain text body only
- **AND** no PDF attachment SHALL be generated or stored

### Requirement: Send dedup is surfaced

Send responses SHALL indicate whether the request was newly queued or deduplicated.

#### Scenario: Duplicate send within dedup window

- **GIVEN** a ticket was sent to a recipient over a channel
- **WHEN** the same ticket/channel/recipient is submitted again within 60 seconds
- **THEN** the response SHALL include `deduplicated = true`

### Requirement: Cashier cancel is time-windowed

Cashier cancel SHALL be allowed only inside the configured post-sale cancellation window.

#### Scenario: Cancel within window succeeds

- **GIVEN** a ticket sold less than the cancel window ago
- **WHEN** cashier cancels it
- **THEN** the response SHALL indicate cancellation success

#### Scenario: Cancel after window fails

- **GIVEN** a ticket sold outside the cancel window
- **WHEN** cashier cancels it
- **THEN** the request SHALL fail with `CANCEL_WINDOW_EXPIRED`
