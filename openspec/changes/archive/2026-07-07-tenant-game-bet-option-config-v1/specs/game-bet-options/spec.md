# Tenant Game Bet Option Config

## ADDED Requirements

### Requirement: Tenant configures offered commercial options and selection policy

A tenant SHALL configure, per `(gameCode, betType)`, which commercial options (`BetOption`) are
offered, their order, POS visibility, the default option, and a selection policy
(`EXPLICIT_ONLY`, `EXPLICIT_WITH_AUTO_OPTION`, or `IMPLICIT_BEST_MATCH`). The configuration SHALL
reuse the existing `BetOption` model and SHALL NOT introduce a parallel commercial-option type. The
seller terminal SHALL show only offered commercial options and SHALL NOT show technical settlement
variants.

#### Scenario: Disabled option is hidden from POS

- **WHEN** a tenant disables a commercial option for a bet type
- **THEN** the seller terminal does not list that option
- **AND** admin/support surfaces may still show its technical variants

### Requirement: POS exposes only coverage-ready selection policies

`EXPLICIT_ONLY` SHALL always be eligible for POS exposure when the tenant enables the option.
`EXPLICIT_WITH_AUTO_OPTION` MAY be exposed at the POS only for commercial options whose coverage
runtime is complete and accepted (`TicketLineCoverage` snapshots, settlement, receipt/reprint,
verification, refund/cancel and minimal reporting). `IMPLICIT_BEST_MATCH` SHALL remain behind a
feature flag and off until its product behavior is explicitly decided. This prevents a UI that offers
combined options before the money-path can represent and settle them safely.

#### Scenario: Combined option exposed only after coverage exists

- **WHEN** a tenant configures `EXPLICIT_WITH_AUTO_OPTION` while coverage support for that option is not complete
- **THEN** the POS does not expose the combined option
- **AND** only single-variant explicit options are sellable

#### Scenario: Exact plus box can be exposed after coverage acceptance

- **WHEN** `LOTTO3_EXACT_PLUS_BOX` or `LOTTO4_EXACT_PLUS_BOX` coverage is complete and accepted
- **THEN** the POS may expose that combined option according to tenant configuration
- **AND** sale preparation snapshots every technical coverage before selling

### Requirement: Settlement calculation is independent of selection policy

The settlement variant calculation via `SettlementVariantResolver` SHALL run regardless of the
selection policy. Under an implicit policy, the customer plays only a number without explicitly
choosing an option, and the backend SHALL still resolve the winning variant and settle accordingly.

#### Scenario: Implicit policy still calculates

- **WHEN** the policy is `IMPLICIT_BEST_MATCH` and a customer plays a number without choosing an option
- **THEN** the backend resolves the applicable settlement variant from the number
- **AND** the ticket is settled from the resolved variant, not from an explicit option choice
