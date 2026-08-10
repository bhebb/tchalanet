## ADDED Requirements

### Requirement: Post-sale delivery is independent from ticket sale

Delivery SHALL operate only on an existing ticket and SHALL never re-run sale
preparation or confirmation.

#### Scenario: Ticket is sold before delivery

- **GIVEN** a ticket has a confirmed ticket id
- **WHEN** the seller chooses SMS, WhatsApp, or email
- **THEN** mobile starts a delivery operation for that ticket
- **AND** a delivery failure does not change the ticket's sold state
- **AND** mobile does not call `prepare` or `confirm` again.

#### Scenario: Seller sends SMS or email without printing

- **GIVEN** a ticket has been confirmed
- **AND** automatic printing is disabled or no printer is available
- **WHEN** the seller chooses SMS or email from the completion screen
- **THEN** mobile sends only the selected delivery request
- **AND** mobile does not call the printer service
- **AND** mobile does not open the system PDF print UI automatically.

### Requirement: Tenant policy controls available channels

Mobile SHALL show only channels enabled by the tenant and available to the
active terminal/session.

#### Scenario: Channel is disabled

- **GIVEN** a tenant has disabled a delivery channel
- **WHEN** the seller views ticket actions
- **THEN** that channel is unavailable or clearly disabled
- **AND** mobile does not enqueue a request for it.

### Requirement: Delivery retry is idempotent

Retries SHALL not send an accidental duplicate for the same ticket, channel,
recipient, and delivery intent.

#### Scenario: Provider timeout

- **GIVEN** a delivery request times out
- **WHEN** the seller retries
- **THEN** the retry uses the same delivery intent key
- **AND** the result is shown as sent, queued, or failed without reselling the
  ticket.
