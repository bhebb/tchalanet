# Platform Modules Registry

> Living reference. This is intentionally not the ADR itself.

`platform/` is the Java/backend layer for transversal application service modules. It is distinct from `/api/v1/platform/**`, which is the HTTP platform-admin scope.

## Current target modules

| Module | Status | Purpose | Notes |
|---|---:|---|---|
| `platform.audit` | target | Audit trail, audit records, audit commands/listeners | May use specific transaction rules for failure audit. |
| `platform.accesscontrol` | target | Roles, permissions, effective permission checks | Security glue remains in `common.security`; decisions live here. |
| `platform.identity` | target | App user, user profile, tenant membership, IdP mapping, user bootstrap | Replaces former `core.user` / `core.tenantuser`; do not use `usercontext`. |
| `platform.tenantconfig` | target | Tenant lifecycle/config base, effective tenant config | Keep separate from tenant theme/game. |
| `platform.tenanttheme` | target | Tenant branding/theme overrides and resolution | Theme presets may live in `catalog.theme`. |
| `platform.tenantgame` | target | Tenant game enablement/policy resolution | Game catalog data remains in `catalog`. |
| `platform.document` | target | Document generation/storage/metadata/workflows | Pure PDF/QR primitives may remain in `common` if stateless. |
| `platform.communication` | target | Outbound channels, providers, message delivery | Sends email/SMS/Slack/etc. |
| `platform.notification` | target | Notification intent, inbox/preferences/batch lifecycle notifications | Uses `platform.communication` to deliver. |
| `platform.idempotence` | target | Persistent idempotency records, replay protection, cleanup jobs | Annotations/interfaces may remain in `common.idempotence`. |

## Explicit naming decision

Use `platform.identity`, not `platform.usercontext`.

Reason:
- `usercontext` conflicts with `TchRequestContext`, `RequestContext`, `ActorContext`, and `OperationalContext` terminology.
- `identity` better represents persisted user/profile/membership/identity-provider mapping.
- Runtime request context remains in `common.context`.
