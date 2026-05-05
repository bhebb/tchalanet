# Study Security Bootstrap Status Policy

## Why

While applying `security-auth-accesscontrol-refactor`, several security-flow rules exposed policy
ambiguities that should be decided explicitly before deeper cleanup.

## What

- Clarify whether first-login runtime bootstrap creates `app_user` as `ACTIVE` or
  `PENDING_APPROVAL`.
- Align `DOMAIN_USER.md`, `UserStatus`, and Flyway defaults for the supported user statuses.
- Decide whether method security belongs on controllers only or may remain on command handlers for
  privileged unit actions.
- Define the migration boundary for removing `@RequiresPermission` and its annotation entirely.

## Impact

This is an analysis/change-planning item only. It does not change runtime behavior until a follow-up
implementation is approved.
