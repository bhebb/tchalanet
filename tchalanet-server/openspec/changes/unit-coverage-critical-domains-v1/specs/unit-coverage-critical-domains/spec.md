# unit-coverage-critical-domains Spec Delta

## ADDED Requirements

### Requirement: Unit Test Boundary For Business Logic

The server SHALL cover business-critical sell-flow logic with fast, Spring-free
unit tests, distinct from Spring integration and Python E2E.

#### Scenario: Logic carriers are unit tested

- **WHEN** a class carries branching business logic (evaluator, applier,
  calculator, resolver, state machine)
- **THEN** it SHALL have JUnit unit tests asserting each branch
- **AND** the tests SHALL run without a Spring context or Testcontainers
- **AND** plumbing classes (commands, handlers, requests, responses, JPA
  entities/adapters, controllers) SHALL NOT be unit tested unless they carry
  branching logic.

#### Scenario: Combinatorial matrix lives at the unit level

- **WHEN** the sell decision depends on
  `{maryaj gratis ON/OFF} × {limit configured ON/OFF} × {terminal override ON/OFF}`
- **THEN** the permutations SHALL be asserted in unit tests
- **AND** Spring integration SHALL assert only the wired composition
- **AND** E2E SHALL assert only a single full-stack happy path
- **AND** the same permutation SHALL NOT be duplicated across layers.

### Requirement: Limit Evaluation Unit Coverage

The limit evaluation engine and its evaluators SHALL be unit tested for
below-limit, at-limit, above-limit, and limit-absent cases.

#### Scenario: A stake exposure limit is evaluated

- **WHEN** a sale is evaluated against a configured per-selection stake exposure
  limit
- **THEN** a stake below the limit SHALL pass
- **AND** a stake above the limit SHALL produce a limit breach with a clear reason
- **AND** the at-limit boundary SHALL follow the documented inclusive/exclusive rule.

#### Scenario: No limit is configured

- **WHEN** no limit rule applies to a sale
- **THEN** the engine SHALL return no breach
- **AND** the sale SHALL not be blocked by the limit layer.

### Requirement: Sell-Time Promotion Effect Unit Coverage

Sell-time promotion effect appliers SHALL be unit tested independently of
campaign configuration and state.

#### Scenario: Maryaj gratis is active

- **WHEN** an eligible sale is processed with an active maryaj gratis campaign
- **THEN** the promotion effect applier SHALL materialize the promotional
  effect (waived charge / free line / boosted odds as configured)
- **AND** the applied effect SHALL be captured in the sale promotion snapshot.

#### Scenario: No campaign is configured

- **WHEN** a sale is processed with no eligible campaign
- **THEN** the promotion appliers SHALL be a no-op
- **AND** base charge and base odds SHALL remain unchanged.

### Requirement: Terminal Odds Override Unit Coverage

Seller-terminal odds override rules SHALL be unit tested with and without an
active override.

#### Scenario: An active override is present

- **WHEN** a sale runs on a terminal with an active odds override
- **THEN** the override SHALL apply to the resolved odds
- **AND** the interaction with a promotion odds boost SHALL follow one
  documented precedence with no double application.

#### Scenario: No override is present

- **WHEN** a sale runs on a terminal with no active override
- **THEN** the resolved odds SHALL be the base/pricing odds.

### Requirement: Draw Result And Settlement Unit Coverage

Draw-result scheduling and ticket settlement lifecycle logic SHALL be unit
tested for legal and illegal transitions.

#### Scenario: Settlement lifecycle transitions

- **WHEN** a ticket settlement moves through its lifecycle
- **THEN** legal transitions SHALL be accepted
- **AND** illegal transitions SHALL be rejected
- **AND** the derived settlement status SHALL match the lifecycle state.

### Requirement: Print And Receipt Policy Unit Coverage

Print policy and receipt label/i18n resolution SHALL be unit tested.

#### Scenario: Print policy decides eligibility

- **WHEN** a print is requested for a ticket
- **THEN** the print policy SHALL allow or block per the documented rule
- **AND** the receipt label/i18n resolvers SHALL return the expected labels for
  the requested locale.
