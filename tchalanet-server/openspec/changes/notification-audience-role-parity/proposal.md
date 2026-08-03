# Proposal - notification audience role parity

## Why

The tenant notification audience is interpreted differently by two paths. In-app visibility
includes `TENANT_OWNER` and `TENANT_ADMIN`, while external recipient resolution currently
selects only `TENANT_ADMIN`. A tenant owner can therefore see a notification in the inbox but
cannot receive an external delivery derived from the same notification.

The roles and scope boundaries also need an explicit contract so that adding a recipient does not
accidentally expose tenant notices to an unrelated superadmin.

## What

- Define `TENANT_ADMINS` as all active tenant owners and tenant administrators.
- Keep `PLATFORM_ADMINS` restricted to active superadministrators.
- Require in-app visibility and external recipient resolution to follow the same audience policy.
- Preserve the separation between tenant (`/admin/notifications`) and platform
  (`/platform/notifications`) notification scopes.
- Add focused regression coverage for audience membership and scope isolation.

## Non-goals

- No automatic email, SMS, or Slack policy is added for tenant notifications.
- No schema migration or change to notification retention is required.
- A superadministrator does not become an implicit administrator of every tenant.
