# platform.archive Spec

## ADDED Requirements

### Requirement: Ticket purge supports business retention classes

`platform.archive` SHALL support ticket hot-table purge based on ticket retention eligibility, not
only on calendar periods.

#### Scenario: Losing no-payout ticket becomes a short-retention candidate

- **GIVEN** an approved ticket has `result_status=LOST`
- **AND** the ticket has `settlement_status=NO_PAYOUT`
- **AND** the ticket has `winning_amount=0`
- **AND** the ticket is older than the configured losing-ticket retention cutoff
- **AND** matching verified archive and lookup metadata exists
- **WHEN** a SUPER_ADMIN requests a ticket purge dry-run
- **THEN** the ticket is reported as purge-eligible
- **AND** its charges, lines and header are counted in the dry-run plan

#### Scenario: Winning and payout tickets are kept online longer

- **GIVEN** a ticket has `result_status=WON`
- **OR** the ticket has `settlement_status=PAYOUT_PENDING`
- **OR** the ticket has `settlement_status=PAID`
- **OR** the ticket has `settlement_status=REVERSED`
- **WHEN** the losing-ticket retention cutoff is evaluated
- **THEN** the ticket is not selected by the short-retention purge
- **AND** the ticket remains available in hot storage until the longer winning/dispute retention policy allows purge

#### Scenario: Unresolved tickets are not purged

- **GIVEN** a ticket has `result_status=NOT_RESULTED` or `result_status=PENDING`
- **OR** the ticket has `settlement_status=NOT_SETTLED`
- **WHEN** archive cleanup evaluates ticket candidates
- **THEN** the ticket is not purge-eligible

#### Scenario: Candidate purge remains verified and audited

- **GIVEN** ticket purge candidates were selected by ticket ID
- **WHEN** DELETE mode is requested
- **THEN** purge is refused unless every candidate is covered by verified archive metadata
- **AND** purge is refused when a matching legal hold exists
- **AND** a non-empty reason and requester are recorded
- **AND** deletion removes `sales_ticket_charge`, then `sales_ticket_line`, then `sales_ticket`
  in bounded batches

### Requirement: Draw and result cleanup respects ticket verification dependencies

`platform.archive` SHALL keep draw and result data available long enough for ticket verification,
payout, correction and public result history flows.

#### Scenario: Draw purge is blocked by hot ticket references

- **GIVEN** a draw is older than the draw retention cutoff
- **AND** verified draw archive metadata exists
- **AND** a hot ticket still references the draw
- **WHEN** a SUPER_ADMIN requests draw purge
- **THEN** purge is refused
- **AND** the dry-run plan reports the blocking ticket references

#### Scenario: Draw-result purge is blocked by draw references

- **GIVEN** a draw result is older than the draw-result retention cutoff
- **AND** verified draw-result archive metadata exists
- **AND** a hot draw still references the draw result
- **WHEN** a SUPER_ADMIN requests draw-result purge
- **THEN** purge is refused
- **AND** the dry-run plan reports the blocking draw references

#### Scenario: Draw channels are retained as reference data

- **GIVEN** a draw channel has historical draws or tickets
- **WHEN** archive cleanup evaluates domain purge
- **THEN** `draw_channel` is not treated as a purge dataset
- **AND** operators use deactivate, versioning, or configuration history instead of deleting the row

### Requirement: Verification remains explicit after hot purge

Ticket verification SHALL return an explicit outcome when a ticket has been purged from hot storage
after verified archive, instead of silently behaving like an unknown ticket.

#### Scenario: Public code lookup finds archive metadata

- **GIVEN** a losing ticket was purged from hot storage after verified archive
- **WHEN** a public user or cashier verifies the ticket by public code or QR URL
- **THEN** the service searches hot storage first
- **AND** then searches archive lookup metadata
- **AND** returns an explicit archived or expired verification outcome
- **AND** does not expose archive object storage locations
