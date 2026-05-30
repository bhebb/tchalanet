# Task 00 — Cleanup terminal controllers

## Goal

Remove duplicated/incoherent terminal routes and make each controller responsibility obvious.

## Steps

1. Keep `TerminalAdminLifecycleController` as owner of:
   - `POST /admin/terminals`
   - `DELETE /admin/terminals/{terminalId}`
   - `PATCH /admin/terminals/{terminalId}/lock`
   - `PATCH /admin/terminals/{terminalId}/unlock`

2. Keep `TerminalAdminAssignmentController` as owner of:
   - `POST /admin/terminals/{terminalId}/assign-outlet`
   - `POST /admin/terminals/{terminalId}/assign-user`
   - `POST /admin/terminals/{terminalId}/activate-for-user`

3. Rename typo:
   - `TerminalAdminMetadatatController` -> `TerminalAdminMetadataController`

4. Rework `AdminTerminalOperationalControlsController`:
   - remove register/unregister/lock/unlock from it;
   - keep only `PATCH /admin/terminals/{terminalId}/operational-controls/{control}`;
   - ensure command includes tenantId or documented RLS/context reason.

5. Ensure all write endpoints have:
   - security annotation;
   - `@AuditLog`;
   - `@Valid` request;
   - typed IDs in path variables;
   - no business logic.

## Acceptance

- No endpoint contains two terminal IDs accidentally.
- Create terminal is only `POST /admin/terminals`.
- Operational control controller contains only operational-control-specific operation.
- Controller methods only map + dispatch through bus.
