# Specification: core.agent integrations

## ADDED Requirements

### Requirement: Tenant onboarding must integrate with core.agent

Tenant onboarding SHALL seed zones and create an internal direct-sales agent.

#### Scenario: Direct tenant outlet has internal agent

- **GIVEN** tenant `ParyajPam` is created
- **WHEN** onboarding completes
- **THEN** an internal agent such as `ParyajPam Direct` exists
- **AND** tenant-owned outlets can be assigned to that internal agent.

### Requirement: core.outlet must validate agent and zone assignments

`core.outlet` SHALL use `core.agent` API to validate that an outlet's agent can operate in the selected zone.

#### Scenario: Create outlet for agent

- **GIVEN** tenant admin requests outlet creation with `agentId` and `zoneId`
- **WHEN** `core.outlet` validates the request
- **THEN** it asks `core.agent` whether the agent can operate in that zone
- **AND** it rejects invalid assignments before persisting.

### Requirement: core.terminal must resolve agent via outlet

Terminals SHALL not carry independent agent truth. They SHALL resolve commercial scope through their outlet.

#### Scenario: Terminal sale context

- **GIVEN** terminal `POS-001` belongs to outlet `O1`
- **AND** outlet `O1` belongs to agent `A1`
- **WHEN** terminal context is validated
- **THEN** the resolved operational scope includes `agentId=A1`.

### Requirement: core.session must keep user-agent consistency

Sales sessions SHALL validate that the seller user is allowed to operate under the resolved agent/outlet.

#### Scenario: Seller not assigned to agent

- **GIVEN** user `Marie` is not assigned to agent `Chez Tata`
- **WHEN** Marie starts or uses a sales session in a `Chez Tata` outlet
- **THEN** the session validation rejects the operation.

### Requirement: core.sales must snapshot agent and zone

Ticket sales SHALL snapshot commercial responsibility at sale confirmation.

#### Scenario: Outlet later transferred

- **GIVEN** ticket `T1` was sold under agent `Chez Tata`
- **WHEN** the outlet is later reassigned to another agent
- **THEN** ticket `T1` still reports under original `Chez Tata` for historical reporting.

### Requirement: core.limitpolicy must support zone and agent targets

Limit policy SHALL be able to target tenant, zone, agent, outlet, terminal, seller, game, draw channel, and number/boule scopes.

#### Scenario: Agent level 2 sale consumes parent limits

- **GIVEN** agent hierarchy `Toto -> Tata`
- **AND** Tata sells a ticket
- **WHEN** limit exposure is evaluated
- **THEN** exposure may be checked against Tata, Toto, zone, and tenant limits.

### Requirement: core.promotion must use agent and zone targeting

Promotion evaluation SHALL accept `agentId`, `agentPath`, `zoneId`, and `zonePath` in its context.

#### Scenario: Promotion by zone

- **GIVEN** a Maryaj promotion is active for zone `Sud`
- **WHEN** a sale occurs in sub-zone `Camp-Perrin`
- **THEN** the promotion can match via zone path.

### Requirement: Reporting/BFF must support agent and zone rollups

Reporting views SHALL support tenant, zone, level 1 agent, level 2 agent, outlet, terminal, and seller rollups.

#### Scenario: Parent agent report

- **GIVEN** `Chez Toto` has child agent `Chez Tata`
- **WHEN** a report is requested for `Chez Toto`
- **THEN** the report includes sales from `Chez Toto` and its descendant agents according to user permissions.

### Requirement: Future wallet/prepaid must use agent identity

Future wallet/prepaid functionality SHALL debit or reserve against an agent account, not a user account.

#### Scenario: Seller under affiliate sells ticket

- **GIVEN** seller Marie sells under agent `Chez Tata`
- **WHEN** prepaid debit is enabled in the future
- **THEN** the debit applies to the configured agent account for `Chez Tata`, not Marie's user identity.
