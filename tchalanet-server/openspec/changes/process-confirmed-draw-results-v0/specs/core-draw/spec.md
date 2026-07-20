## ADDED Requirements

### Requirement: Only actionable global results may apply to tenant draws

The system SHALL attach and settle a global result for a closed tenant draw only when its status is
`CONFIRMED` or `OVERRIDDEN`.

#### Scenario: Provisional result exists

- **WHEN** a global result is `PROVISIONAL`
- **THEN** the apply candidate and application handler leave closed tenant draws unchanged
- **AND** no `DrawResultAppliedEvent` is emitted.

### Requirement: Draw settlement waits for terminal ticket processing

The system SHALL settle a resulted draw only when all tickets eligible for that draw have terminal
result and settlement state.

#### Scenario: One ticket remains pending after a processing failure

- **WHEN** a ticket cannot be calculated or persisted
- **THEN** the draw remains `RESULTED`
- **AND** a later recovery attempt may process the pending ticket.
