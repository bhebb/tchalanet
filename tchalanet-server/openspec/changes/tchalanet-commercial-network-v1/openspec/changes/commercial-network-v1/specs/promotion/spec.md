# Spec — Promotion V1

## ADDED Requirements

### Requirement: Promotion V1 effects

The system SHALL support exactly these promotion effects in V1: FREE_GAME_LINE, BOOST_ODDS, WAIVE_CHARGE.

### Requirement: Free game line

Sales SHALL materialize FREE_GAME_LINE as a ticket line snapshot.

### Requirement: Boost odds

Sales SHALL snapshot boosted odds on the affected ticket line without modifying pricing_odds.

### Requirement: Waive charge

Sales SHALL materialize WAIVE_CHARGE in MoneyBreakdown or charge snapshots.

### Requirement: Promotion snapshot

Settlement and payout SHALL use promotion snapshots created by Sales.
