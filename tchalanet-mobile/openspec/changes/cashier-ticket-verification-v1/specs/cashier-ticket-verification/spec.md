## ADDED Requirements

### Requirement: Verify without a payout workflow

The mobile POS MUST display the ticket-verification decision returned by
`features.pos`. It MUST NOT offer a payment command or local fake payment
confirmation.

#### Scenario: Winning ticket

- **WHEN** `features.pos` returns `PAYABLE` and a calculated amount
- **THEN** the POS displays the localized winning result and amount
- **AND** it does not display a payout action

### Requirement: Open verified ticket details

The POS MUST enable ticket details only when the verification response includes
a resolved ticket identifier.

#### Scenario: Found ticket

- **WHEN** the verification response includes `ticketId`
- **THEN** the seller can navigate to that ticket's detail screen

#### Scenario: Unknown ticket

- **WHEN** the verification response has no `ticketId`
- **THEN** ticket detail navigation remains unavailable
