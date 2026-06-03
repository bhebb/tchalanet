# Change: Init Mobile POS Foundation

## Status

Proposed

## Context

The Flutter mobile/POS application has a different runtime flow from Web. It should not blindly copy the public PageModel Web flow. The mobile/POS foundation must prove:

- Keycloak/auth session works on device;
- tokens are stored securely;
- roles are detected;
- cashier/admin surfaces can be protected if included;
- terminal/outlet/session operational context can be displayed and validated later;
- local i18n merges with backend overrides at runtime;
- runtime theme/settings are applied;
- POS-ready UI primitives exist without building the whole sales flow first.

## Goals

- Create Dart contracts aligned with backend/frontend Web concepts where useful.
- Configure Keycloak/OIDC auth flow appropriate for mobile.
- Store tokens securely.
- Add login/logout test screen.
- Prove protected dashboard access and role display.
- Add runtime settings as V1 feature-toggle/config mechanism.
- Merge local mobile translations with backend overrides; backend wins on duplicate keys.
- Add runtime theme support with Tchalanet default theme.
- Add operational context contracts for terminal/outlet/session.
- Add dashboard POS skeleton showing session, tenant, role, terminal/outlet/session placeholder state.
- Add minimal POS UI primitives: notice/error/loading/status/operational context/session/sync indicators.

## Non-goals

- No full sell flow in this change.
- No full offline sync engine in this change.
- No forced Web PageModel runtime for mobile V1.
- No custom theme builder.
- No Unleash integration in V1.
- No platform superadmin experience on mobile unless explicitly required later.

## Critical decisions

### Mobile flow is not Web PageModel flow

Mobile/POS initially prioritizes auth, session, operational context, settings, i18n, and theme. PageModel mobile is optional and out of V1 unless a specific dynamic mobile screen requirement appears.

### i18n rule is shared with Web

Local translations are fallback. Backend overrides are merged at runtime and win on duplicate keys.

### settings are needed for V1 flags

Runtime settings are used for feature toggles/config in V1 and may be replaced or supplemented by Unleash later.

### POS operational context is first-class

The POS dashboard must make it visible whether the app knows:

- who is logged in;
- which tenant is active;
- which terminal is bound;
- which outlet is attached;
- which sales session is active.

