# DOMAIN_AGENT — OpenSpec notes

This file summarizes the intended domain decisions captured in `openspec/changes/add-core-agent`.

## Decisions

- `User != Agent`.
- `Agent` is tenant-scoped commercial responsibility, not a mini-tenant.
- V1 max hierarchy: tenant -> level 1 agent -> level 2 agent -> users/outlets/terminals.
- Zones are tenant-scoped commercial zones, seeded at onboarding and customizable.
- Parent agent can delegate only within commercial allowed zones.
- Outlet belongs to tenant and is operated by one agent in V1.
- Sale history snapshots `agentId` and `zoneId`.

## Main integration impacts

- Tenant onboarding: seed default zones and internal agent.
- Outlet: require/validate agent + zone.
- Terminal/session: resolve agent through outlet/session.
- Sales: snapshot agent/zone/seller.
- LimitPolicy: add zone and agent targets.
- Promotion: use agent/zone targeting.
- Reporting: add rollups by agent and zone.
- Future wallet/commission: attach to agent, not user.
