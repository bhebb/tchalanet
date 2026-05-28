# sales-sell Specification

## Purpose
TBD - created by archiving change harden-critical-ticket-flows-v1. Update Purpose after archive.
## Requirements
### Requirement: Sell must materialize final ticket truth

`core.sales` SHALL materialize the final ticket truth during `SellTicketCommand` handling.

The final persisted ticket SHALL include:

- final ticket lines;
- final money breakdown;
- final charges;
- final potential payout;
- applied promotion effects;
- promotion identifiers and display snapshots where applicable.

#### Scenario: Normal paid ticket

Given a valid POS context and no active promotion
When a seller sells a ticket
Then the persisted ticket contains only paid lines
And no applied promotion snapshot is created
And the returned result contains a backup built from canonical sales receipt content.

#### Scenario: Maryaj gratuit promotion

Given an active FREE_GAME_LINE campaign
And the buyer qualifies for Maryaj gratuit
When a seller sells an eligible ticket
Then the persisted ticket includes the promotional line or promotional line marker
And the promotional line has origin `PROMOTION`
And the promotion decision id is snapshotted
And the payout base amount is snapshotted
And the canonical receipt can display `Maryaj gratuit` without re-evaluating promotion.

#### Scenario: Boost odds promotion

Given an active BOOST_ODDS campaign
When a seller sells an eligible ticket
Then the affected ticket line stores the boosted odds snapshot
And the pricing source indicates promotion effect
And the final potential payout uses the boosted odds snapshot.

#### Scenario: Waived charge promotion

Given an active WAIVE_CHARGE campaign
And the buyer requests a chargeable delivery channel
When a seller sells an eligible ticket
Then the waived charge is reflected in final money breakdown
And the buyer total excludes the waived charge.

### Requirement: Sell must evaluate exposure using final risk

The sale policy/autonomy/limit evaluation SHALL use final promotion-adjusted lines and money whenever promotions change payout risk.

Promotion eligibility conditions MAY use the initial paid basis.

#### Scenario: Promotion increases exposure above autonomy

Given a sale that is under the seller limit before promotion
And a promotion adds a free line or boosts odds
And final exposure exceeds seller autonomy
When the seller attempts to sell
Then the sale is rejected or pending approval according to policy
And the decision is based on final promotion-adjusted risk.

### Requirement: Sell must publish complete ticket placement event

`TicketPlacedEvent` SHALL include line-level promotion snapshots.

Each `TicketLinePlacedItem` SHALL include enough facts for downstream consumers to avoid recalculating promotion:

- origin;
- pricing source;
- selection source;
- payout base amount;
- promotion decision id;
- promotion label;
- promotion effect type.

#### Scenario: Ticket placed with promotion

Given a ticket sold with Maryaj gratuit
When `TicketPlacedEvent` is published after commit
Then the event includes the promotional line
And the promotional line includes promotion id, label, effect type, origin, and payout base.

### Requirement: Sell side effects must happen after commit

Ticket placement events and communication dispatch SHALL happen after the sale transaction commits.

Communication dispatch SHALL receive explicit tenant/user/correlation context and SHALL NOT rely on implicit `TchContext` inside the after-commit callback.

#### Scenario: Sale transaction rolls back

Given a sale transaction fails after ticket creation but before commit
When the transaction is rolled back
Then no `TicketPlacedEvent` is published
And no receipt communication is enqueued.

### Requirement: Applied promotion snapshots must explain materialized effects

Applied promotion snapshots SHALL capture materialized effects, not only the global decision.

Snapshots SHOULD include:

- ticket id;
- promotion decision id;
- campaign id/code;
- effect type;
- target line id or charge type;
- original and final stake/odds/charge where applicable;
- payout base amount;
- display label;
- applied timestamp.

#### Scenario: Admin reviews promotion costs

Given tickets sold with promotions
When tenant admin later reviews promotion cost/usage
Then sales snapshots are sufficient to explain what was applied and paid
And no call to promotion rule evaluation is needed.

