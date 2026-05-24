# Specification: core.agent

## ADDED Requirements

### Requirement: User and Agent must be separate concepts

Tchalanet SHALL model user identity separately from commercial affiliate responsibility.

#### Scenario: User sells under an agent

- **GIVEN** user `Marie` is assigned as seller to agent `Chez Tata`
- **WHEN** Marie sells a ticket from an outlet operated by `Chez Tata`
- **THEN** the sale records `sellerUserId=Marie`
- **AND** the sale records `agentId=Chez Tata`
- **AND** reports can attribute the sale to both Marie and Chez Tata.

#### Scenario: Agent has multiple users

- **GIVEN** agent `Chez Tata` has users `Tata`, `Marie`, and `Samuel`
- **WHEN** Marie and Samuel sell tickets
- **THEN** both sales roll up under `Chez Tata`
- **AND** disabling Marie does not remove or rename the agent history.

### Requirement: Agent hierarchy must be depth-limited in V1

The system SHALL allow at most two agent levels below a tenant in V1.

#### Scenario: Tenant creates level 1 agent

- **GIVEN** tenant admin creates `Chez Toto Sud`
- **WHEN** no parent agent is specified
- **THEN** the agent is created at level 1.

#### Scenario: Level 1 agent creates level 2 agent

- **GIVEN** agent `Chez Toto Sud` is level 1
- **AND** has a mandate allowing child agent creation in zone `Sud`
- **WHEN** `Chez Tata Camp-Perrin` is created under `Chez Toto Sud`
- **THEN** `Chez Tata Camp-Perrin` is level 2.

#### Scenario: Level 2 agent cannot create child agent in V1

- **GIVEN** agent `Chez Tata Camp-Perrin` is level 2
- **WHEN** a user attempts to create a child agent under it
- **THEN** the operation is rejected with a domain error such as `agent.hierarchy.max_depth_exceeded`.

### Requirement: Tenant onboarding must seed default commercial zones

The system SHALL seed default Haiti commercial zones during tenant onboarding unless disabled.

#### Scenario: Tenant receives default zones

- **GIVEN** a new tenant is onboarded
- **WHEN** default zone seeding runs
- **THEN** zones such as `Zone Métropolitaine`, `Ouest`, `Sud`, `Nord`, `Grand’Anse`, `Nippes` are created for the tenant
- **AND** the tenant can rename, disable, or add sub-zones later.

### Requirement: Agent mandates must control delegated zones

An agent SHALL only create or manage child commercial activity in zones included in its mandate.

#### Scenario: Parent agent creates child in allowed zone

- **GIVEN** agent `Chez Toto` has `canCreateSubAgents=true` for zone `Sud`
- **WHEN** a child agent is created in zone `Sud`
- **THEN** the creation is allowed.

#### Scenario: Parent agent creates child outside allowed zone

- **GIVEN** agent `Chez Toto` has mandate only for zone `Sud`
- **WHEN** it attempts to create an agent in zone `Nord`
- **THEN** the operation is rejected with `agent.zone.not_allowed`.

### Requirement: Outlets must be linked to tenant, agent, and zone

Every outlet SHALL remain tenant-scoped and SHALL be assigned to a responsible agent and operating zone.

#### Scenario: Affiliate outlet creation

- **GIVEN** agent `Chez Tata` has a mandate for `Camp-Perrin`
- **WHEN** tenant admin creates outlet `Boutique Tata Centre` in `Camp-Perrin`
- **THEN** the outlet is assigned to `Chez Tata`
- **AND** the outlet zone is accepted.

#### Scenario: Outlet zone outside agent mandate

- **GIVEN** agent `Chez Tata` has a mandate for `Camp-Perrin`
- **WHEN** an outlet is created for `Chez Tata` in `Nord`
- **THEN** the operation is rejected.

### Requirement: Agent operational scope must be resolvable for sensitive operations

The system SHALL expose a stable query to resolve operational commercial scope from user, outlet, terminal, or session context.

#### Scenario: Sales resolves agent and zone

- **GIVEN** a trusted operational context with terminal and sales session
- **WHEN** sales asks `core.agent` to resolve operational scope
- **THEN** it receives `tenantId`, `agentId`, `agentPath`, `zoneId`, `zonePath`, and `sellerUserId`
- **AND** sales can snapshot these values on the ticket.

### Requirement: Agent suspension must block descendants

If an agent is blocked or suspended, descendant agents and outlets must not be eligible for new sensitive operations.

#### Scenario: Parent blocked

- **GIVEN** parent agent `Chez Toto` is `BLOCKED`
- **AND** child agent `Chez Tata` is `ACTIVE`
- **WHEN** a seller under `Chez Tata` tries to sell
- **THEN** agent validation rejects the sale because a parent is blocked.
