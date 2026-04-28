# Tasks: Add PageModel Template Update Workflow

## 1. Listener correction

- [ ] Replace direct call to `PageModelWritePort.applyTemplateUpdate(...)` in `PageModelTemplateUpdatedListener`.
- [ ] Add command/service to create notifications for affected tenants/admins.
- [ ] Ensure listener remains thin and idempotent.
- [ ] Ensure event handling remains after-commit.

## 2. Affected page discovery

- [ ] Add query/reader to find PageModels by template id/logical id.
- [ ] Include tenant, publication status, schema version, and customization metadata.
- [ ] Respect RLS or use controlled platform/admin scope where needed.

## 3. Notification creation

- [ ] Create `ACTION_REQUIRED` notification for tenant admins.
- [ ] Include payload:
  - [ ] `templateId`
  - [ ] `logicalId`
  - [ ] `schemaVersion`
  - [ ] `compatibility`
  - [ ] `recommendedAction`
- [ ] Use dedupe key per tenant/template/logicalId/schemaVersion.

## 4. Preview and diff

- [ ] Add `PreviewTemplateUpdateQuery`.
- [ ] Add diff model for frontend:
  - [ ] added sections/widgets/keys
  - [ ] removed sections/widgets/keys
  - [ ] changed sections/widgets/keys
  - [ ] conflicts
  - [ ] recommended action
- [ ] Do not rely only on stale notification payload; recalculate preview from current state.

## 5. Apply commands

- [ ] Add `MergePageModelWithTemplateCommand`.
- [ ] Add `CreateDraftFromTemplateUpdateCommand`.
- [ ] Add `ReplacePageModelFromTemplateCommand`.
- [ ] Add `IgnoreTemplateUpdateCommand`.
- [ ] Add migration-required handling for `MAJOR` changes.
- [ ] Ensure commands use typed IDs and `@TchTx`.

## 6. PageModel writer services

- [ ] Implement merge service.
- [ ] Implement replace service.
- [ ] Implement draft creation service.
- [ ] Preserve published model unless explicit replace is accepted.
- [ ] Create backup/draft before destructive replace.

## 7. API endpoints

- [ ] Add tenant-admin review/list endpoint.
- [ ] Add preview endpoint.
- [ ] Add merge endpoint.
- [ ] Add draft endpoint.
- [ ] Add replace endpoint.
- [ ] Add ignore endpoint.
- [ ] Use `ApiResponse<T>` and `ProblemDetail` conventions.

## 8. Audit

- [ ] Audit merge decision.
- [ ] Audit draft creation decision.
- [ ] Audit replace decision.
- [ ] Audit ignore decision.
- [ ] Audit migration-required decision if admin acknowledges it.

## 9. Tests

- [ ] Template update creates notification, not direct mutation.
- [ ] Published PageModel is not changed without admin action.
- [ ] Merge safe applies correctly.
- [ ] Conflict requires explicit action.
- [ ] Replace creates backup/draft snapshot first.
- [ ] Duplicate event does not create duplicate notifications.
