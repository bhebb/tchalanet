# Specification: settlement-payout-integration

## ADDED Requirements

### Requirement: Settlement uses applied promotion snapshots

Settlement SHALL use ticket/line applied snapshots for payout-impacting effects.

#### Scenario: Promotion changed after sale

- GIVEN a ticket was sold with first prize multiplier 60x
- AND the promotion rule is later changed to 50x
- WHEN settlement calculates winnings for the old ticket
- THEN settlement SHALL use 60x from the ticket snapshot.

### Requirement: Tenant-specific multipliers affect payout calculation

Payout calculation SHALL support tenant-specific multiplier snapshots.

#### Scenario: Matching first prize with applied multiplier

- GIVEN a winning line has `effective_stake_amount = 100 HTG`
- AND a snapshot has `appliedMultiplier = 60`
- WHEN payout is calculated
- THEN final payout SHALL be 6000 HTG for that line.

### Requirement: Free lines can be payable when configured

A free promotional line SHALL be eligible for payout if its snapshot/effective stake says it is payable.

#### Scenario: Free Maryaj wins

- GIVEN a free MARYAJ line has `effective_stake_amount = 50 HTG`
- AND the line wins
- WHEN settlement calculates payout
- THEN payout SHALL be calculated from 50 HTG, not 0 HTG.
