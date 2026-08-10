## Scope boundary

This specification owns collapsing the `prepare` + `confirm` two-step flow into
a single seller action and triggering direct print immediately after
confirmation creates the ticket.

Post-sale delivery channels are explicitly outside this specification and
outside `pos-fast-sale-direct-print-v1`. Defining post-sale delivery channels
(SMS, WhatsApp, email) according to tenant-enabled channels and terminal
capabilities is the responsibility of the separate
`post-sale-delivery-channels-v1` specification.

## ADDED Requirements

### Requirement: Quick sale must remain prepared-sale safe

Mobile quick sale SHALL provide a one-tap seller experience without switching
to the legacy direct-sell endpoint.

#### Scenario: Accepted sale with no review is auto-confirmed

- **GIVEN** quick-sale mode is enabled
- **AND** the seller has committed at least one line
- **WHEN** the seller taps the primary sale action
- **AND** `POST /tenant/sales/preparations` returns an accepted preparation
  with no promotion lines, notices, or review requirement
- **THEN** mobile confirms that preparation using
  `POST /tenant/sales/preparations/{preparationId}/confirm`
- **AND** it sends the stable `Idempotency-Key` for that sale intent
- **AND** it navigates to the sale-completion surface on sold response.

#### Scenario: Review-required preparation is not auto-confirmed

- **GIVEN** quick-sale mode is enabled
- **WHEN** the preparation response includes promotion lines, notices, or a
  review-required signal
- **THEN** mobile shows the preparation preview
- **AND** the seller must explicitly confirm or return to editing.

#### Scenario: Rejected preparation remains on sale screen

- **GIVEN** quick-sale mode is enabled
- **WHEN** preparation is rejected or the API returns a mapped problem
- **THEN** mobile shows the localized error or rejection inline
- **AND** no confirm request is sent.

#### Scenario: Accepted preparation is confirmed within one seller action

- **GIVEN** the seller taps the quick-sale action once
- **WHEN** `POST /tenant/sales/preparations` succeeds with an accepted
  preparation that has no review requirement
- **THEN** mobile calls confirm using that preparation's id
- **AND** mobile sends the stable idempotency key for the same sale intent
- **AND** mobile does not call the legacy direct-sell endpoint.

#### Scenario: Prepare failure stops the chain

- **GIVEN** the seller taps quick sale
- **WHEN** prepare returns a validation, pricing, promotion, or availability
  error
- **THEN** mobile shows the localized error
- **AND** mobile sends no confirm request
- **AND** mobile does not retry prepare automatically with altered lines.

#### Scenario: Prepare success is not shown as a sold ticket

- **GIVEN** prepare returns an accepted preparation
- **WHEN** confirm has not yet returned a sold response
- **THEN** mobile does not navigate to the success screen
- **AND** it shows a confirmation-in-progress or prepared state
- **AND** it disables duplicate sale submission for the current intent.

#### Scenario: Confirm is rejected by a known business rule

- **GIVEN** prepare succeeded
- **WHEN** confirm returns a known stock, limit, cutoff, validation, or
  expired-preparation problem
- **THEN** mobile does not show `Siksè`
- **AND** mobile shows the localized backend error
- **AND** mobile keeps the ticket as not sold from the client perspective
- **AND** the seller can explicitly edit, discard, or start a new prepare
- **AND** mobile does not retry confirm with a new idempotency key.

#### Scenario: Confirm outcome is unknown after a network failure

- **GIVEN** prepare succeeded and confirm was sent
- **WHEN** the connection times out, drops, or returns an indeterminate 5xx
- **THEN** mobile does not show either sold or definitively failed
- **AND** mobile shows `Vente à vérifier` with retry/recovery actions
- **AND** it keeps the same preparation id and idempotency key
- **AND** a retry calls confirm only, never prepare.

#### Scenario: Confirm succeeds after a timeout on retry

- **GIVEN** the first confirm outcome was unknown
- **WHEN** mobile retries confirm with the same preparation id and key
- **THEN** a positive response navigates to the success screen with the returned
  ticket id
- **AND** an idempotent replay does not create a second ticket
- **AND** automatic printing starts only after this sold response.

#### Scenario: Seller attempts a second sale while confirmation is pending

- **GIVEN** the current intent is confirming or has an unknown outcome
- **WHEN** the seller taps `Nouveau ticket` or submits another sale
- **THEN** mobile blocks the second sale
- **AND** it asks the seller to resolve or explicitly abandon the pending intent
- **AND** it does not create a second preparation or ticket automatically.

#### Scenario: App restarts with a pending confirmation

- **GIVEN** the app closes after prepare or during confirm
- **WHEN** the seller returns to the POS
- **THEN** mobile restores the pending preparation id and idempotency key when
  available
- **AND** it offers the same confirm/recovery action
- **AND** it does not silently call prepare again.

#### Scenario: Confirm retry does not regenerate Maryaj gratis

- **GIVEN** prepare created a stored preparation containing a generated
  `HT_MARYAJ_GRATIS` promotion line
- **AND** confirm times out or returns a retryable error before the client knows
  whether the ticket was created
- **WHEN** the seller retries
- **THEN** mobile calls confirm with the same preparation id
- **AND** mobile uses the same idempotency key
- **AND** mobile does not call prepare again
- **AND** the backend returns the original ticket or the original idempotent
  result without generating a second Maryaj line.

#### Scenario: Maryaj gratis regeneration is explicit and bounded

- **GIVEN** prepare returns a generated Maryaj gratis line and the seller wants
  another generated selection
- **WHEN** the seller explicitly chooses regenerate before confirmation
- **THEN** mobile calls the preparation promotion-line regeneration endpoint
- **AND** mobile updates the same preparation preview
- **AND** mobile displays the remaining regeneration allowance
- **AND** mobile confirms only the latest stored preparation state.

#### Scenario: Confirmed preparation cannot be regenerated

- **GIVEN** a preparation has already been confirmed
- **WHEN** mobile receives a stale regenerate action or retry
- **THEN** backend rejects the regeneration
- **AND** mobile keeps the existing sold ticket
- **AND** mobile does not prepare or sell a replacement ticket.

### Requirement: POS print must not replace Tchalanet with PDF UI

On physical POS direct-print mode, auto-print and manual print SHALL remain
inside the Tchalanet app and SHALL NOT call the system PDF print preview.

#### Scenario: POS direct auto-print

- **GIVEN** the seller is on a supported physical POS terminal
- **AND** receipt printer mode resolves to POS direct
- **WHEN** a sale is confirmed
- **THEN** mobile requests the ticket print endpoint with
  `printOptionsRequest.outputFormat = ESC_POS`
- **AND** `printOptionsRequest.paperSize` is `RECEIPT_58MM` or `RECEIPT_80MM`
- **AND** the returned bytes are sent to the selected POS printer adapter
- **AND** the app remains on the sale-completion screen.

#### Scenario: Unsupported terminal offers manual fallback

- **GIVEN** POS direct mode is preferred
- **AND** no POS printer adapter is available
- **WHEN** auto-print would run
- **THEN** mobile shows print failed or printer unavailable inline
- **AND** it offers manual PDF fallback
- **AND** it does not launch system PDF UI automatically.

### Requirement: Printer implementation must stay vendor-generic

Mobile SHALL hide vendor-specific printing APIs behind a generic printer
adapter interface.

#### Scenario: NETUM external printer uses Bluetooth ESC/POS adapter

- **GIVEN** the seller has selected a paired NETUM-style Bluetooth thermal printer
- **WHEN** POS direct print is requested
- **THEN** `PrinterService` selects the Bluetooth ESC/POS adapter
- **AND** mobile requests `ESC_POS` bytes from backend
- **AND** callers only depend on the generic printer interface.

#### Scenario: Sunmi device uses Sunmi adapter

- **GIVEN** the device capability probe identifies a Sunmi printer service
- **WHEN** POS direct print is requested
- **THEN** `PrinterService` selects the Sunmi adapter
- **AND** callers only depend on the generic printer interface.

#### Scenario: Future device can plug in without sale-flow rewrite

- **GIVEN** a future POS vendor adapter is added
- **WHEN** the adapter reports compatible ESC/POS capabilities
- **THEN** `PrinterService` can select it without changes to sale controller
  code or sale-completion UI code.

### Requirement: Print failure SHALL NOT invalidate the sale

A ticket sale and a receipt print SHALL be represented as separate outcomes in
the mobile UI.

#### Scenario: Print fails after sold response

- **WHEN** a sale has been confirmed and the print adapter fails
- **THEN** mobile continues to show the ticket code and sold state
- **AND** it shows retry and fallback actions for the print only
- **AND** it does not resubmit the sale.

### Requirement: Reprints remain auditable

Seller-initiated reprints SHALL keep the existing reason capture and audit
payload while using the new printer service.

#### Scenario: Seller reprints from success or ticket detail

- **WHEN** the seller requests a reprint
- **THEN** mobile asks for a reprint reason
- **AND** sends the reason to
  `POST /tenant/cashier/tickets/{ticketId}/print`
- **AND** dispatches the returned receipt through the selected printer adapter.

### Requirement: Print configuration SHALL separate tenant policy from device preference

Mobile SHALL resolve print behavior from tenant/admin receipt defaults, then
terminal/device preferences, then live adapter capabilities.

The backend SHALL resolve terminal-scoped settings from the authenticated
seller-terminal context. A mobile request SHALL NOT select or impersonate a
seller terminal by supplying an arbitrary `sellerTerminalId`.

#### Scenario: Tenant default initializes terminal paper size

- **GIVEN** tenant receipt config defaults to `RECEIPT_80MM`
- **AND** the terminal has no explicit paper-size override
- **WHEN** mobile resolves print options
- **THEN** it uses `RECEIPT_80MM` unless the selected adapter reports only a
  different supported receipt width.

#### Scenario: Terminal override does not change tenant policy

- **GIVEN** one terminal is configured for `RECEIPT_58MM`
- **AND** another terminal in the same tenant is configured for `RECEIPT_80MM`
- **WHEN** each terminal prints a ticket
- **THEN** each request uses its own terminal paper-size preference
- **AND** tenant receipt template, header/footer, and QR policy remain shared.

#### Scenario: Unsupported adapter falls back safely

- **GIVEN** tenant policy allows PDF fallback
- **AND** terminal preference is POS direct
- **WHEN** no POS adapter is available
- **THEN** mobile offers system PDF fallback
- **AND** it does not mutate tenant/admin receipt defaults.

#### Scenario: Same phone is used for two seller terminals

- **GIVEN** one phone is used first with seller terminal A and later with
  seller terminal B
- **WHEN** the seller-terminal session changes
- **THEN** mobile loads terminal B's server-side receipt settings
- **AND** it uses a local printer binding scoped to tenant B and terminal B
- **AND** it does not reuse terminal A's Bluetooth printer silently.

#### Scenario: Admin opens a selected seller terminal POS

- **GIVEN** an authorized admin opens seller terminal A from the terminal list
- **WHEN** the POS session is initialized
- **THEN** the client sends the existing
  `X-Tch-Act-As-Terminal: A` bridge context on terminal-scoped POS requests
- **AND** the backend injects terminal A into `TchRequestContext`
- **AND** profile, sale, print, reprint, limits, and statistics resolve for
  terminal A
- **AND** the mobile UI clearly shows that terminal A is active.

#### Scenario: Admin changes the active seller terminal

- **GIVEN** the same phone was operating terminal A
- **WHEN** the admin opens terminal B
- **THEN** mobile replaces the active terminal session with terminal B
- **AND** reloads terminal B's server-side settings
- **AND** uses a local printer binding scoped to terminal B
- **AND** it does not reuse terminal A's Bluetooth printer silently.

#### Scenario: Client cannot select a terminal through a sale request

- **GIVEN** a POS sale or print request is being prepared
- **WHEN** the request contains a seller-terminal identifier in its body
- **THEN** backend uses the authenticated terminal context or admin bridge
  context instead of trusting that body value
- **AND** the sale and print remain attributed to the active server-side
  terminal.

#### Scenario: Admin operates a Sunmi terminal from a regular phone

- **GIVEN** terminal A is normally equipped with an integrated Sunmi printer
- **AND** an admin opens terminal A's POS from a regular phone
- **WHEN** a receipt is printed
- **THEN** mobile probes the capabilities of the current phone, not only the
  saved terminal preference
- **AND** it uses a locally paired compatible printer when available
- **AND** otherwise it reports that direct printing is unavailable and offers
  manual PDF
- **AND** it does not pretend to reach the remote Sunmi printer.

## Test Matrix

The implementation SHALL cover the following phone, terminal, sale, and print
cases. A print failure must never create a second sale.

| ID | Phone and active context | Printer state | Delivery action | Expected result |
| --- | --- | --- | --- | --- |
| TEL-01 | Regular phone, seller-terminal identity | No printer | Sell only | Ticket is sold; completion screen stays in Tchalanet; no PDF opens automatically. |
| TEL-02 | Regular phone, seller-terminal identity | NETUM paired and reachable | Auto print | Backend receives `ESC_POS` with the configured receipt width; NETUM prints; app remains open. |
| TEL-03 | Regular phone, admin bridge for terminal A | NETUM paired locally for terminal A | Auto print | Requests carry `X-Tch-Act-As-Terminal: A`; print uses terminal A settings and the local terminal-A printer binding. |
| TEL-04 | Regular phone, admin bridge for terminal A | Terminal A prefers Sunmi, but no Sunmi service on phone | Auto print | No Sunmi attempt is claimed; show printer unavailable and offer manual PDF. |
| TEL-05 | Sunmi phone/device, seller-terminal identity | Integrated Sunmi printer available | Auto print | Sunmi adapter prints directly; no system PDF UI or app replacement. |
| TEL-06 | Any supported phone and active terminal | Printer available | Manual print | Seller can print from completion or ticket detail; manual print does not resell the ticket. |
| TEL-07 | Any phone and active terminal | No printer | Manual PDF | Seller explicitly chooses PDF; backend returns PDF; system PDF/share UI may open only after this action. |
| TEL-11 | Any phone and active terminal | Printer disconnects during print | Auto print | Sale remains sold; show retry and PDF fallback; retry never confirms the sale again. |
| TEL-13 | Any phone | Terminal A active, then terminal B selected | Local printer configured for A | Reload B settings and use B-scoped local binding; A's printer is not silently reused. |
| TEL-14 | Admin phone | No active terminal bridge | Any POS operation | Backend returns missing-terminal/unauthorized context; mobile does not invent a terminal id. |
| TEL-15 | Any phone and active terminal | Any printer | Double tap on sell or print | One sale and one intended print operation only; stable idempotency keys handle retries. |
| TEL-16 | Any phone and active terminal | Any printer | Reprint | Require a reason, record the reprint, and print without creating a new ticket. |

Automated tests SHALL cover the request payload, context headers, state
transitions, idempotency, and independence of sale/print outcomes. Physical
smoke tests SHALL cover TEL-02 with NETUM, TEL-05 on the ordered Sunmi, and
TEL-04 from a regular phone. SMS, WhatsApp, and email delivery tests belong to
`post-sale-delivery-channels-v1`.
