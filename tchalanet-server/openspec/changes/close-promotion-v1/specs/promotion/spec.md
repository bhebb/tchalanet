# Specification — Promotion V1

## ADDED Requirements

### Requirement: Promotion module purpose

The promotion module SHALL own promotion configuration, campaign lifecycle, rule validation, and runtime decision resolution.

The promotion module SHALL NOT own tickets, settlement, payout, ledger, or stats.

#### Scenario: Promotion decides but Sales materializes
- **GIVEN** a sale preparation context
- **WHEN** Promotion evaluates active campaigns
- **THEN** Promotion returns a decision
- **AND** Sales materializes the decision on TicketLine and MoneyBreakdown
- **AND** Settlement and Payout do not call Promotion again

### Requirement: Campaign lifecycle

Campaign lifecycle SHALL be controlled by a state machine.

Allowed transitions SHALL be:

```text
DRAFT    -> ACTIVE | INACTIVE | ARCHIVED
INACTIVE -> ACTIVE | ARCHIVED
ACTIVE   -> PAUSED
PAUSED   -> ACTIVE | INACTIVE | ARCHIVED
ARCHIVED -> none
```

#### Scenario: Activate draft campaign
- **GIVEN** a campaign in `DRAFT`
- **WHEN** the admin activates it
- **THEN** the state becomes `ACTIVE`

#### Scenario: Archive active campaign directly
- **GIVEN** a campaign in `ACTIVE`
- **WHEN** the admin archives it
- **THEN** the operation is rejected with `promotion.campaign.pause_before_archive`

### Requirement: Rule configuration

A promotion rule SHALL contain rule key, priority, scalar eligibility columns, line-count eligibility rows, and typed effect rows.

Promotion rule configuration SHALL NOT store `eligibility_json`, `effects_json`, `evaluation_phase`, rule status, quota key, or max uses.

#### Scenario: Store multiple effects
- **GIVEN** a rule with two effects
- **WHEN** the rule is persisted
- **THEN** each effect is stored as a row in `promotion_rule_effect`
- **AND** every effect row has a typed `effect_type`

#### Scenario: Store paid line count eligibility
- **GIVEN** a rule requiring at least 3 paid lines for a game
- **WHEN** the rule is persisted
- **THEN** the condition is stored as a row in `promotion_rule_eligibility_line`
- **AND** the row is tenant-scoped for RLS

### Requirement: V1 eligibility

Promotion V1 SHALL support only `MIN_PAID_TOTAL`, `PAID_LINE_COUNT`, and `BEFORE_LOCAL_TIME`.

#### Scenario: Unsupported eligibility type
- **GIVEN** an eligibility item with unsupported type
- **WHEN** the rule is validated
- **THEN** validation is rejected

### Requirement: V1 effects

Promotion V1 SHALL support only `FREE_GAME_LINE`, `BOOST_ODDS`, and `WAIVE_CHARGE`.

#### Scenario: Free game line effect
- **GIVEN** a valid `FREE_GAME_LINE` effect
- **WHEN** Promotion resolves a decision
- **THEN** the decision includes a promotional line instruction

#### Scenario: Boost odds effect
- **GIVEN** a valid `BOOST_ODDS` effect
- **WHEN** Promotion resolves a decision
- **THEN** the decision includes an odds override instruction
- **AND** pricing odds are not modified

#### Scenario: Waive charge effect
- **GIVEN** a valid `WAIVE_CHARGE` effect
- **WHEN** Promotion resolves a decision
- **THEN** the decision includes a charge waiver instruction

### Requirement: Campaign activation verification

Activation SHALL verify campaign consistency.

#### Scenario: Activate invalid campaign
- **GIVEN** a campaign with no rule
- **WHEN** the admin activates it
- **THEN** activation is rejected

### Requirement: Campaign view

`PromotionCampaignView` SHALL return rules with eligibility and effects for detail views.

#### Scenario: Get campaign detail
- **GIVEN** a campaign with rules
- **WHEN** the admin requests campaign detail
- **THEN** the response includes ordered rules
- **AND** each rule includes eligibility items
- **AND** each rule includes effect items

### Requirement: Cache

Promotion SHALL use cache only as infrastructure optimization.

Runtime active campaign cache SHALL be keyed by tenant id.

Campaign detail cache SHALL be keyed by tenant id and campaign id.

Admin list cache SHALL be keyed by tenant id, page number, page size, and sort.

#### Scenario: Rule mutation
- **GIVEN** a rule is updated
- **WHEN** the transaction commits
- **THEN** runtime tenant cache is evicted
- **AND** campaign detail cache is evicted
- **AND** admin list cache is cleared

### Requirement: Runtime decision resolution

Promotion runtime SHALL evaluate only ACTIVE campaigns.

Promotion runtime SHALL return a decision and SHALL NOT mutate Sales.

#### Scenario: Resolve promotion decision
- **GIVEN** active campaigns exist for the tenant
- **WHEN** Sales asks Promotion for a decision during sale preparation
- **THEN** Promotion returns matching effects
- **AND** Sales remains responsible for applying them
