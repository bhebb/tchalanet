# Spec: Tenant Provisioning / Onboarding V1

## ADDED Requirements

### Requirement: Provisioning is separate from dashboard and overview

Tenant provisioning SHALL be a platform admin flow, not a PageModel dashboard provider.

Package:

```text
features.platformadmin.tenantonboarding
```

Routes:

```http
POST /platform/tenant-onboarding/preview
POST /platform/tenant-onboarding/provision
```

### Requirement: Provisioning V1 uses controlled profiles

V1 SHALL support controlled profiles:

- `MINIMAL`
- `DEFAULT_HAITI_LOTTERY`
- `DEMO`

`CUSTOM_FROM_TENANT` is out of scope for V1.

### Requirement: Preview is read-only

#### Scenario: Super admin previews provisioning

- **WHEN** super admin submits preview
- **THEN** the system returns included domains, warnings, not-copied data, and expected readiness
- **AND** no data is written.

### Requirement: Provisioning calls owning domains

The provisioning orchestrator SHALL NOT insert directly into all domain tables.

It MUST call owning domain/catalog/platform APIs or commands.

Provisionable domains V1:

| Domain | Owner |
|---|---|
| Tenant base | platform tenant owner / tenantconfig |
| Initial admin user | platform identity |
| PageModels | core.pagemodel |
| Theme/appearance | catalog.theme / platform.tenanttheme |
| Settings defaults | catalog.settings / platform.tenantconfig |
| I18n overrides | catalog.i18n |
| Games enabled | catalog.game / platform tenant game owner |
| Pricing defaults | pricing owner |
| Draw channels | draw channel owner |
| Promotion templates | core.promotion |
| Limit policy templates | core.limitpolicy |

### Requirement: Provisioning never copies transactional data

Provisioning SHALL NOT copy:

- tickets;
- sales;
- sessions;
- payouts;
- terminal bindings;
- users except initial admin/demo users;
- audit;
- notifications;
- stats;
- ledger;
- offline submissions.

### Requirement: Provisioning result includes readiness

Provisioning result SHALL return readiness/next steps.

#### Scenario: Tenant provisioned with default profile

- **WHEN** provisioning completes
- **THEN** the result includes per-domain status
- **AND** readiness may still require outlet, terminal and seller setup.
