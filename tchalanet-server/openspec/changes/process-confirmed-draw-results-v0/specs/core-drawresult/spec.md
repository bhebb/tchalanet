## ADDED Requirements

### Requirement: Provisional results are operational but not financial

The system SHALL make provisional global results available to platform operations without using
them to calculate ticket winnings or payouts.

#### Scenario: Provisional result remains unconfirmed

- **WHEN** a provisional result exceeds the configured operations threshold
- **THEN** the system creates at most one action-required platform notification through the
  existing notification and Slack bridge
- **AND** no tenant draw or ticket financial state changes.
