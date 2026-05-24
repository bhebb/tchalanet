# Tasks — add-core-agent

## 1. Documentation and decisions

- [ ] Add `docs/DOMAIN/DOMAIN_AGENT.md` with decisions: User vs Agent, hierarchy depth, zones, mandates, outlet/user/terminal relationship.
- [ ] Document default Haiti zones and tenant onboarding behavior.
- [ ] Document V1 depth limit and what is intentionally deferred.

## 2. Common IDs

- [ ] Add typed IDs if missing: `AgentId`, `AgentZoneId`, `AgentMandateId`, `AgentUserAssignmentId`.
- [ ] Add Spring converters and MapStruct mapping helpers according to typed ID policy.

## 3. Persistence

- [ ] Create Flyway migration for `agent_zone`, `agent`, `agent_zone_mandate`, `agent_user_assignment`.
- [ ] Add all standard audit columns required by `persistence.md`.
- [ ] Add tenant RLS policies.
- [ ] Add indexes for tenant-scoped lookups and hierarchy traversal.
- [ ] Add JPA entities only under `core.agent.internal.infra.persistence`.

## 4. Domain layer

- [ ] Implement pure domain model: `Agent`, `AgentZone`, `AgentZoneMandate`, `AgentUserAssignment`.
- [ ] Implement pure policies: `AgentHierarchyPolicy`, `AgentMandatePolicy`, `AgentZonePolicy`.
- [ ] Enforce V1 max depth.
- [ ] Enforce parent/child zone delegation rules.

## 5. Application layer

- [ ] Add commands and handlers for zone seed/create/update, agent create/status, user assignment, mandate grant/revoke.
- [ ] Add queries and handlers for agent/zone list and operational scope resolution.
- [ ] Use ports for persistence; no infra dependencies in application.
- [ ] Publish domain events after commit.

## 6. Web/admin

- [ ] Add tenant admin controllers under `core.agent.internal.infra.web.admin`.
- [ ] Use `/admin/agents` and `/admin/agent-zones` logical paths.
- [ ] Use `@PreAuthorize` permission gates and audit annotations.
- [ ] Do not include `/api/v1` in `@RequestMapping` if servlet path already provides it.

## 7. Integration changes

- [ ] Update tenant onboarding to seed default zones and create internal tenant agent.
- [ ] Update `core.outlet` to require/resolve `agentId` and validate outlet zone against agent mandate.
- [ ] Update `core.terminal` and session context resolution to expose agent and zone scope via outlet/agent.
- [ ] Update `core.sales` to snapshot `agentId`, `zoneId`, and seller user on sale.
- [ ] Update `core.limitpolicy` target model to support agent and zone targets.
- [ ] Update reporting/BFF views to filter and aggregate by agent and zone.

## 8. Tests

- [ ] Unit-test hierarchy depth policy.
- [ ] Unit-test parent zone delegation.
- [ ] Unit-test user-agent assignment rules.
- [ ] Integration-test RLS isolation.
- [ ] Integration-test operational scope resolution.
