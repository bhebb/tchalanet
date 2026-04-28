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
- **Feature slice**: `com.tchalanet.server.features.tenantadmin.<slice>` when the screen is a composite BFF.
- **Admin controller**: `core.<bc>.infra.web.admin.*Controller` when the screen is mono-domain CRUD.

> If a core handler is missing → it MUST be added to the owning core (not to the feature).

---

## 1) Tenant Admin Portal — menu matrix

| Menu area (UI)         | Feature slice                      | Owning core(s)                                            | Core handlers (examples)                                                     | BFF payloads (examples)   |
| ---------------------- | ---------------------------------- | --------------------------------------------------------- | ---------------------------------------------------------------------------- | ------------------------- |
| Bootstrap (startup)    | `core.tenantuser.infra.web.admin`  | `core.tenantuser` (+ optional other cores for enrichment) | `GetCurrentUserQuery`                                                        | `TenantUserAdminResponse` |
| Current user (`/me`)   | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `GetCurrentUserQuery`                                                        | `TenantUserAdminResponse` |
| Users list             | `core.tenantuser.infra.web.admin`  | `core.tenantuser` (+ `core.user` for identity details)    | `PagedListTenantUsersQuery`                                                  | `TenantUserAdminResponse` |
| User details           | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `GetCurrentUserQuery`                                                        | `TenantUserAdminResponse` |
| Create user            | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `CreateUserCommand`, `AssignUserToTenantCommand`, `SetTenantUserRoleCommand` | `TenantUserAdminResponse` |
| Update user            | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `UpdateUserProfileCommand`                                                   | `TenantUserAdminResponse` |
| User preferences       | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `UpdateUserPreferencesCommand`                                               | `TenantUserAdminResponse` |
| Assign user to tenant  | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `AssignUserToTenantCommand`                                                  | `TenantUserAdminResponse` |
| Unassign user          | `core.tenantuser.infra.web.admin`  | `core.tenantuser`                                         | `UnassignUserFromTenantCommand`                                              | `ApiResponse<Void>`       |
| Tenant config overview | `tenantadmin/config`               | `core.tenantconfig`, `core.tenanttheme`, catalogs         | Core queries + catalog reads                                                 | `AdminConfigOverviewView` |
| Branding/theme         | `tenantadmin/config`               | `core.tenanttheme` + `catalog.theme`                      | `ResolveTenantThemeQuery`, theme commands                                    | `ThemeSummaryView`        |
| Tenant settings        | `tenantadmin/config/settings`      | `catalog.settings`                                        | Settings catalog/admin service calls                                         | `AdminSettingRow`         |
| Tenant i18n            | `tenantadmin/config/i18n`          | `catalog.i18n`                                            | I18n catalog/admin service calls                                             | `AdminI18nRow`            |
| Outlets list           | `core.outlet.infra.web.admin`      | `core.outlet`                                             | `ListOutletsByTenantQuery`                                                   | `OutletSummary`           |
| Outlet create/update   | `core.outlet.infra.web.admin`      | `core.outlet`                                             | `CreateOutletCommand`, `UpdateOutletConfigCommand`                           | `OutletId` / `OutletView` |
| Terminals list         | `core.terminal.infra.web.admin`    | `core.terminal`                                           | `ListTerminalsQuery`                                                         | `TerminalResponse`        |
| Register terminal      | `core.terminal.infra.web.admin`    | `core.terminal`                                           | `RegisterTerminalCommand`                                                    | `TerminalId`              |
| Lock/unlock terminal   | `core.terminal.infra.web.admin`    | `core.terminal`                                           | `LockTerminalCommand`, `UnlockTerminalCommand`                               | `ApiResponse<Void>`       |
| Limits CRUD            | `core.limitpolicy.infra.web.admin` | `core.limitpolicy`                                        | Limit definition/assignment commands and queries                             | Core limit policy views   |
| Autonomy CRUD          | `core.autonomy.infra.web.admin`    | `core.autonomy`                                           | `GetAutonomyOverviewQuery`, `UpsertAutonomyPolicyRuleCommand`                | Core autonomy views       |
| Policies overview      | `tenantadmin/policies`             | `core.limitpolicy` + `core.autonomy`                      | Limit policy queries + autonomy query                                        | `PoliciesOverviewView`    |

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
3. If the screen is mono-domain CRUD, expose it from `core/<bc>/infra/web/admin`.
4. If the screen is composite, orchestrate in `tenantadmin/<slice>` via CommandBus/QueryBus.
5. Assemble **BFF model** only for composite screens.
6. Verify ArchUnit rules: no repo/JPA access from feature, no UUID usage in feature.
