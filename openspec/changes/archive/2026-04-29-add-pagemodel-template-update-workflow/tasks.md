# Tasks: Add PageModel Template Update Workflow

## 1. Listener correction

- [x] Replace direct call to `PageModelWritePort.applyTemplateUpdate(...)` in `PageModelTemplateUpdatedListener`.
- [x] Add command/service to create notifications for affected tenants/admins.
- [x] Ensure listener remains thin and idempotent.
- [x] Ensure event handling remains after-commit.

## 2. Affected page discovery

- [x] Add query/reader to find PageModels by template id/logical id.
- [x] Include tenant, publication status, schema version, and customization metadata.
- [x] Respect RLS or use controlled platform/admin scope where needed.

## 3. Notification creation

- [x] Create `ACTION_REQUIRED` notification for tenant admins.
- [x] Include payload:
  - [x] `templateId`
  - [x] `logicalId`
  - [x] `schemaVersion`
  - [x] `compatibility`
  - [x] `recommendedAction`
- [x] Use dedupe key per tenant/template/logicalId/schemaVersion.

## 4. Preview and diff

- [x] Add `PreviewTemplateUpdateQuery`.
- [x] Add diff model for frontend:
  - [x] added sections/widgets/keys
  - [x] removed sections/widgets/keys
  - [x] changed sections/widgets/keys
  - [x] conflicts
  - [x] recommended action
- [x] Do not rely only on stale notification payload; recalculate preview from current state.

## 5. Apply commands

- [x] Add `MergePageModelWithTemplateCommand`.
- [x] Add `CreateDraftFromTemplateUpdateCommand`.
- [x] Add `ReplacePageModelFromTemplateCommand`.
- [x] Add `IgnoreTemplateUpdateCommand`.
- [x] Add migration-required handling for `MAJOR` changes.
- [x] Ensure commands use typed IDs and `@TchTx`.

## 6. PageModel writer services

- [x] Implement merge service.
- [x] Implement replace service.
- [x] Implement draft creation service.
- [x] Preserve published model unless explicit replace is accepted.
- [ ] Create backup/draft before destructive replace.

## 7. API endpoints

- [ ] Add tenant-admin review/list endpoint.
- [x] Add preview endpoint.
- [x] Add merge endpoint.
- [x] Add draft endpoint.
- [x] Add replace endpoint.
- [x] Add ignore endpoint.
- [x] Use `ApiResponse<T>` and `ProblemDetail` conventions.

## 8. Audit

- [ ] Audit merge decision.
- [ ] Audit draft creation decision.
- [ ] Audit replace decision.
- [ ] Audit ignore decision.
- [ ] Audit migration-required decision if admin acknowledges it.

## 9. Tests

- [x] Template update creates notification, not direct mutation.
- [x] Published PageModel is not changed without admin action.
- [ ] Merge safe applies correctly.
- [ ] Conflict requires explicit action.
- [ ] Replace creates backup/draft snapshot first.
- [ ] Duplicate event does not create duplicate notifications.
