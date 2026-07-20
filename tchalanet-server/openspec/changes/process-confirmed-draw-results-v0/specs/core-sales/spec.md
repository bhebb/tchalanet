## ADDED Requirements

### Requirement: Ticket result processing is replay-safe

The system SHALL process only pending tickets in a bounded command invocation. Replaying the same
draw SHALL not create duplicate ticket-result or payout events for a terminal ticket.

#### Scenario: A ticket processing attempt fails

- **WHEN** ticket result calculation or persistence fails for one ticket
- **THEN** the ticket remains eligible for retry
- **AND** the processing outcome reports unresolved work
- **AND** the draw is not considered ready to settle.
