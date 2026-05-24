# Design — core.agent

## Architectural placement

`core.agent` is a core domain because agent hierarchy, commercial delegation, limits, wallet/prepaid, commissions, reporting, and sale responsibility can affect money, risk, and disputes.

Package shape:

```text
core/agent/api/
  command/
  query/
  event/
  model/

core/agent/internal/
  domain/
  application/
    command/handler/
    query/handler/
    port/out/
    service/
  infra/
    persistence/
    web/admin/
    event/
    config/
```

Other modules may only depend on `core.agent.api.*`.

## Business model

### Tenant vs Agent vs User

```text
Tenant = operator / owner of the commercial system.
Agent  = tenant-scoped affiliate/commercial responsibility.
User   = actor identity who logs in and performs actions.
```

A sale is done **by a user** but **under an agent**.

Required sale attribution:

```text
tenantId
agentId
zoneId
sellerUserId
outletId
terminalId
salesSessionId
```

### V1 hierarchy

V1 supports two levels of agents under the tenant:

```text
Tenant
  -> level 1 agent: regional/main affiliate
      -> level 2 agent: local affiliate
          -> users / sellers / outlets / terminals
```

V1 rejects deeper agent creation.

Recommended names:

```text
AgentType.INTERNAL
AgentType.AFFILIATE_PARENT
AgentType.AFFILIATE
AgentType.INDIVIDUAL
```

Depth rules:

```text
INTERNAL          may be created for tenant-owned direct operations.
AFFILIATE_PARENT may create level 2 agents when mandate allows it.
AFFILIATE        may manage sellers/outlets/terminals, not create child agents in V1 unless explicitly allowed by future plan.
INDIVIDUAL       represents one seller-style agent, no child agents.
```

### Commercial zones

Zones are tenant-scoped commercial territories. They are not necessarily exact administrative boundaries.

Onboarding may seed default Haiti zones:

```text
Zone Métropolitaine
Ouest
Centre
Artibonite
Nord
Nord-Est
Nord-Ouest
Sud
Sud-Est
Grand’Anse
Nippes
```

Tenants can activate, rename, disable, or create sub-zones such as:

```text
Sud
  -> Les Cayes
      -> Camp-Perrin
          -> Section communale X
```

Zone tree and agent tree are separate. The link is the agent commercial mandate.

### Commercial mandate / allowed zones

An agent has a commercial mandate defining what it can do in zones:

```text
agentId
zoneId
canSell
canCreateSubAgents
canCreateSellers
canCreateOutlets
canManageTerminals
canViewReports
maxChildAgents optional
maxSellers optional
maxTerminals optional
```

A parent agent may only create a child agent in a zone covered by its mandate with `canCreateSubAgents=true`.

### Outlet ownership

Outlets remain tenant-scoped. An outlet is operated/responsible under exactly one agent in V1.

```text
Outlet.tenantId = owner scope
Outlet.agentId  = commercial responsibility
Outlet.zoneId   = operating territory
```

Recommendation: create an internal tenant agent for direct tenant outlets so all sales always have an `agentId`.

### User assignment

Users are assigned to agents with a relationship:

```text
OWNER
MANAGER
SELLER
SUPERVISOR
```

This lets an affiliate have multiple users and sellers without confusing user identity with commercial responsibility.

## Persistence requirements

All tables must follow `docs/conventions/persistence.md`:

- `tenant_id` on every tenant-scoped row;
- standard audit columns, for example `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`, `version` according to project convention;
- RLS policies on all tenant-scoped tables;
- no hard delete for business records unless explicitly allowed;
- indexes for tenant + functional lookup;
- typed IDs outside persistence.

Suggested tables:

```text
agent_zone
agent
agent_zone_mandate
agent_user_assignment
```

Potential future tables:

```text
agent_account
agent_account_movement
agent_commission_profile_assignment
```

## API surface intent

Commands:

```text
SeedDefaultAgentZonesCommand
CreateAgentZoneCommand
UpdateAgentZoneCommand
CreateAgentCommand
UpdateAgentStatusCommand
AssignUserToAgentCommand
GrantAgentZoneMandateCommand
RevokeAgentZoneMandateCommand
```

Queries:

```text
ListAgentZonesQuery
GetAgentQuery
ListAgentsQuery
ResolveAgentOperationalScopeQuery
ValidateAgentForOperationQuery
ListAgentHierarchyQuery
```

Views:

```text
AgentZoneView
AgentView
AgentMandateView
AgentOperationalScopeView
AgentHierarchyNodeView
```

Events:

```text
AgentCreatedEvent
AgentStatusChangedEvent
AgentZoneCreatedEvent
AgentMandateChangedEvent
UserAssignedToAgentEvent
```

Events are after-commit and consumers must be idempotent.

## Integration principle

`core.agent` owns the commercial responsibility model. Other domains consume stable views/queries and snapshot relevant IDs at write time. They must not import `core.agent.internal.*`.
