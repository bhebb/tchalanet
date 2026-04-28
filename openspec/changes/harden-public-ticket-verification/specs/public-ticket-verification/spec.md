## ADDED Requirements

### Requirement: Public verification DTO MUST NOT contain internal UUIDs

`TicketVerificationResult` (the response of `GET /public/tickets/verify/{publicCode}`) SHALL NOT expose any internal UUID, including but not limited to: `ticketId`, `drawId`, `outletAddress.id`, `outletAddress.tenantId`, `outletAddress.addressId`, `terminalId`. The DTO SHALL identify the draw via business fields (`drawDate: LocalDate`, `drawChannelCode: String`) and the terminal via its human-readable label (`terminalLabel: String`).

#### Scenario: JSON response contains no ticketId field

- **WHEN** `GET /public/tickets/verify/{publicCode}` returns a visible ticket
- **THEN** the JSON body has no `ticketId` field at any level

#### Scenario: JSON response contains no drawId field

- **WHEN** `GET /public/tickets/verify/{publicCode}` returns a visible ticket
- **THEN** the JSON body has no `drawId` field
- **AND** instead exposes `drawDate` (ISO date) and `drawChannelCode` (string)

#### Scenario: outletAddress contains only city and country

- **WHEN** the response includes `outletAddress`
- **THEN** the only fields present are `city` and `country`
- **AND** no `id`, `tenantId`, `line1`, `line2`, `region`, `postalCode`, `normalizedKey` field is present

#### Scenario: terminalLabel is the human-readable terminal label

- **WHEN** the response includes `terminalLabel`
- **THEN** its value is the `Terminal.label()` business field (e.g., "POS-001")
- **AND** never a UUID prefix or any portion of the internal `terminalId`

### Requirement: payoutStatus is derived from real ticket state

`TicketVerificationResult.payoutStatus` SHALL be computed from the ticket's actual state — `saleStatus`, `resultStatus`, `settlementStatus`, and `winningAmount` — not from `potentialPayout`. The set of valid values SHALL be:

- `PENDING_DRAW` — `resultStatus == NOT_RESULTED`
- `WON_UNCLAIMED` — `resultStatus IN (WON, OVERRIDDEN)` AND `settlementStatus == UNSETTLED`
- `WON_PAID` — `resultStatus IN (WON, OVERRIDDEN)` AND `settlementStatus == SETTLED`
- `LOST` — `resultStatus == LOST`
- `VOID` — `saleStatus IN (VOID, REJECTED)`
- `EXPIRED` — ticket past the configured visibility window

The values `POTENTIAL_WIN` and `NO_PAYOUT` SHALL NOT be emitted by this version.

#### Scenario: Ticket SOLD + NOT_RESULTED → PENDING_DRAW

- **GIVEN** `Ticket(saleStatus=SOLD, resultStatus=NOT_RESULTED, settlementStatus=UNSETTLED)`
- **WHEN** verify is called
- **THEN** `payoutStatus = "PENDING_DRAW"`

#### Scenario: Ticket SOLD + WON + UNSETTLED → WON_UNCLAIMED

- **GIVEN** `Ticket(saleStatus=SOLD, resultStatus=WON, settlementStatus=UNSETTLED, winningAmount=100)`
- **WHEN** verify is called
- **THEN** `payoutStatus = "WON_UNCLAIMED"`

#### Scenario: Ticket SOLD + WON + SETTLED → WON_PAID

- **GIVEN** `Ticket(saleStatus=SOLD, resultStatus=WON, settlementStatus=SETTLED, winningAmount=100)`
- **WHEN** verify is called
- **THEN** `payoutStatus = "WON_PAID"`

#### Scenario: Ticket SOLD + LOST → LOST

- **GIVEN** `Ticket(saleStatus=SOLD, resultStatus=LOST)`
- **WHEN** verify is called
- **THEN** `payoutStatus = "LOST"`

#### Scenario: Ticket VOID → VOID

- **GIVEN** `Ticket(saleStatus=VOID)`
- **WHEN** verify is called
- **THEN** `payoutStatus = "VOID"`

#### Scenario: Ticket beyond visibility window → EXPIRED

- **GIVEN** a ticket created 30 days ago, with `public_visibility_days = 14`
- **WHEN** verify is called
- **THEN** the response is the minimal expired payload with `payoutStatus = "EXPIRED"`
- **AND** sensitive fields (`saleStatus`, `resultStatus`, `settlementStatus`, `totalAmount`, `outletName`, `outletAddress`, `lines`) are `null` or empty

### Requirement: Visibility window fallback MUST be logged and metered

`VerifyPublicTicketQueryHandler.resolveVisibilityDays(tenantId)` SHALL fetch `public_visibility_days` from `SettingsCatalog`. If the lookup fails or the setting is missing, the handler SHALL fall back to `14` AND emit a `WARN` log entry referencing the tenant AND increment the Prometheus counter `tch_sales_verify_visibility_fallback_total{tenant=...}`. Catching SHALL be limited to expected exceptions (`RestClientException`, `IllegalStateException`); other exceptions SHALL propagate.

#### Scenario: Settings lookup succeeds with explicit value

- **GIVEN** `SettingsCatalog.resolve(...)` returns `[{settingKey: "public_visibility_days", settingValue: "30"}]`
- **WHEN** `resolveVisibilityDays(tenantId)` is called
- **THEN** the method returns `30`
- **AND** no fallback log/metric is emitted

#### Scenario: Settings lookup throws — fallback to 14 with WARN

- **GIVEN** `SettingsCatalog.resolve(...)` throws `RestClientException`
- **WHEN** `resolveVisibilityDays(tenantId)` is called
- **THEN** the method returns `14`
- **AND** a WARN log entry is emitted with the tenantId and exception message
- **AND** `tch_sales_verify_visibility_fallback_total{tenant=<tenantId>}` is incremented by 1

### Requirement: Outlet enrichment failures MUST be logged and metered

The best-effort outlet/address lookup in `toVisibleResult` SHALL catch only `DataAccessException` and `IllegalStateException`. On caught exception, the handler SHALL emit a `WARN` log AND increment `tch_sales_verify_outlet_enrichment_failure_total{tenant=...}`. Other exceptions SHALL propagate to the controller layer (resulting in a normal 500 response — preferred over silent partial data).

#### Scenario: Outlet lookup throws DataAccessException — log + metric + partial response

- **GIVEN** `outletReader.findById(...)` throws a `DataAccessException`
- **WHEN** verify is called
- **THEN** the response still returns 200 with `outletName=null` and `outletAddress=null`
- **AND** a WARN log entry contains the publicCode and the exception message
- **AND** the metric `tch_sales_verify_outlet_enrichment_failure_total` is incremented

#### Scenario: Unexpected exception propagates to 500

- **GIVEN** `outletReader.findById(...)` throws `NullPointerException`
- **WHEN** verify is called
- **THEN** the exception is NOT caught by the enrichment block
- **AND** the response is HTTP 500 (handled by global exception handler)

### Requirement: Draw context exposed via business fields

`TicketVerificationResult` SHALL include `drawDate: LocalDate` and `drawChannelCode: String` instead of `drawId`. The handler resolves these from the ticket's `drawId` via `DrawLookupPort` (or via a composed read on `TicketReaderPort` — implementation choice). If the draw cannot be loaded, both fields SHALL be `null` and a WARN log SHALL be emitted (treated as enrichment failure).

#### Scenario: Draw exists — drawDate and drawChannelCode populated

- **GIVEN** the ticket points to a `Draw(drawDate=2026-04-26, drawChannel.code="HT_BOLET_MID")`
- **WHEN** verify is called
- **THEN** the response contains `"drawDate": "2026-04-26"` and `"drawChannelCode": "HT_BOLET_MID"`

#### Scenario: Draw lookup fails — fields null + WARN

- **GIVEN** `drawLookupPort.findById(drawId)` returns empty
- **WHEN** verify is called
- **THEN** `drawDate` and `drawChannelCode` are `null`
- **AND** a WARN log is emitted
