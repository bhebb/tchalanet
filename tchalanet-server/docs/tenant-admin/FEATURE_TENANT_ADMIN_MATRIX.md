# FEATURE_TENANT_ADMIN — Matrix (Menu → Slice → Owning Core → Core Handlers → BFF Models)

> **Scope**: Backend (`tchalanet-server`)  
> **Status**: NORMATIVE  
> **Goal**: prevent duplication by explicitly mapping each UI area to its **owning core domain(s)**  
> **Rule**: Feature = composition only; truth lives in core/catalog

---

## 0) Legend

- **Owning core**: the only place allowed to implement invariants + persistence writes.
- **Core handlers**: commands/queries that must exist in the owning core.
- **BFF models**: feature-level UI models assembled for screens (no truth).
- **Feature slice**: `com.tchalanet.server.features.tenantadmin.<slice>`

> If a core handler is missing → it MUST be added to the owning core (not to the feature).

---

## 1) Tenant Admin Portal — menu matrix

| Menu area (UI)                   | Feature slice                          | Owning core(s)                                            | Core handlers (examples)                                     | BFF payloads (examples)                 |
| -------------------------------- | -------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------ | --------------------------------------- |
| Bootstrap (startup)              | `tenantadmin/users` _(or portal root)_ | `core.tenantuser` (+ optional other cores for enrichment) | `GetMeQuery`, `GetBootstrapQuery`                            | `TenantAdminBootstrapView`              |
| Current user (`/me`)             | `tenantadmin/users`                    | `core.tenantuser`                                         | `GetMeQuery`                                                 | `CurrentUserView` (may extend)          |
| Users list                       | `tenantadmin/users`                    | `core.tenantuser` (+ `core.user` for identity details)    | `ListTenantUsersQuery`, optional `GetUsersByIdsQuery`        | `TenantUsersListView`                   |
| User details                     | `tenantadmin/users`                    | `core.user` (+ `core.tenantuser` membership)              | `GetUserQuery`, `GetTenantUserQuery`                         | `TenantUserDetailsView`                 |
| Create user                      | `tenantadmin/users`                    | `core.user`                                               | `CreateUserCommand`                                          | `CreateUserResponse` / `TenantUserView` |
| Update user                      | `tenantadmin/users`                    | `core.user`                                               | `UpdateUserCommand`                                          | `UpdateUserResponse`                    |
| User preferences                 | `tenantadmin/users`                    | `core.user`                                               | `UpdateUserPreferencesCommand`, `GetUserPreferencesQuery`    | `UserPreferencesView`                   |
| Assign user to tenant            | `tenantadmin/users`                    | `core.tenantuser`                                         | `AssignUserToTenantCommand`                                  | `TenantUserAssignmentView`              |
| Unassign user                    | `tenantadmin/users`                    | `core.tenantuser`                                         | `UnassignUserFromTenantCommand`                              | `TenantUserAssignmentView`              |
| Tenant config overview           | `tenantadmin/tenantconfig`             | `core.tenant` and/or policy cores + catalog defaults      | `GetTenantConfigQuery`, `UpdateTenantConfigCommand`          | `TenantConfigOverviewView`              |
| Branding/theme                   | `tenantadmin/tenantconfig`             | owning core (tenant/theme) + catalog presets              | `GetThemeQuery`, `UpdateThemeCommand`                        | `TenantBrandingView`                    |
| Feature exposure (flags surface) | `tenantadmin/tenantconfig`             | owning feature-flag core/service                          | `GetFeatureFlagsQuery`, `UpdateFeatureFlagsCommand`          | `TenantFeaturesView`                    |
| Outlets list                     | `tenantadmin/outlets`                  | `core.outlet` (or existing)                               | `ListOutletsQuery`                                           | `OutletsListView`                       |
| Outlet create/update             | `tenantadmin/outlets`                  | `core.outlet`                                             | `CreateOutletCommand`, `UpdateOutletCommand`                 | `OutletDetailsView`                     |
| Outlet details/dashboard         | `tenantadmin/outlets`                  | `core.outlet` (+ optional other cores for metrics)        | `GetOutletQuery`, `GetOutletStatsQuery`                      | `OutletDashboardView`                   |
| Terminals list                   | `tenantadmin/terminals`                | `core.terminal` (or existing)                             | `ListTerminalsQuery`                                         | `TerminalsListView`                     |
| Register terminal                | `tenantadmin/terminals`                | `core.terminal`                                           | `RegisterTerminalCommand`                                    | `TerminalSetupView`                     |
| Pair/activate terminal           | `tenantadmin/terminals`                | `core.terminal`                                           | `PairTerminalCommand`, `ActivateTerminalCommand`             | `TerminalPairingView`                   |
| Revoke/reset terminal            | `tenantadmin/terminals`                | `core.terminal`                                           | `RevokeTerminalCommand`, `ResetTerminalCommand`              | `TerminalDetailsView`                   |
| Draw channels list               | `tenantadmin/draws`                    | `core.draw`                                               | `ListDrawChannelsQuery`                                      | `TenantDrawChannelsView`                |
| Configure draw channel           | `tenantadmin/draws`                    | `core.draw`                                               | `CreateDrawChannelCommand`, `UpdateDrawChannelCommand`       | `DrawChannelDetailsView`                |
| Activate/deactivate channel      | `tenantadmin/draws`                    | `core.draw`                                               | `ActivateDrawChannelCommand`, `DeactivateDrawChannelCommand` | `DrawChannelStatusView`                 |
| Draw windows/schedule            | `tenantadmin/draws`                    | `core.draw`                                               | `UpdateDrawWindowCommand`, `ListDrawWindowsQuery`            | `DrawScheduleView`                      |

---

## 2) Required invariants ownership reminders (anti-duplication)

- User identity + preferences → `core.user`
- Membership + tenant-scoped user listing + `/me` + `/bootstrap` → `core.tenantuser`
- Draw config (tenant draw channels/windows) → `core.draw`
- Outlets → owning core (`core.outlet` or equivalent)
- Terminals/pairing/secrets → owning core (`core.terminal` or equivalent)
- Operational policies (limits/commissions/workflow) → dedicated policy cores

---

## 3) Review checklist (must pass)

When adding a new Tenant Admin screen:

1. Identify **owning core** (single source of truth).
2. Add/extend **core handlers** needed for that screen.
3. In `tenantadmin/<slice>`, orchestrate via CommandBus/QueryBus.
4. Assemble **BFF model** for the screen.
5. Verify ArchUnit rules: no repo/JPA access from feature, no UUID usage in feature.
