# Change: Add PageModel Template Update Workflow

## Summary

Replace the current automatic PageModel mutation on `PageModelTemplateUpdatedEvent` with a controlled workflow:

1. Template update event is published after commit.
2. Listener creates actionable notifications for affected tenant admins.
3. Tenant admin reviews the update.
4. Admin chooses merge, create draft, replace completely, ignore, or migrate.
5. PageModels continue to work until an explicit action is applied.

## Motivation

The current listener directly calls `PageModelWritePort.applyTemplateUpdate(...)` after the template update event. This can silently mutate unpublished pages and create drafts for published pages without tenant-admin approval.

That behavior is risky because PageModels are tenant-facing UI contracts. Template changes can be safe, minor, or breaking. Tenant admins should be notified and allowed to accept, ignore, merge, or replace when a published/customized PageModel is involved.

## Goals

- Stop silent mutation of published PageModels.
- Convert template updates into actionable notifications.
- Add PageModel review/apply services.
- Support merge and replace workflows.
- Support draft creation for published pages.
- Support breaking-change handling through compatibility levels.
- Audit admin decisions.
- Keep existing PageModels functional until explicitly changed.

## Non-Goals

- Do not build a full visual diff editor in this change.
- Do not create a `page_template_update_proposal` table for MVP unless the workflow needs durable multi-step assignment/history.
- Do not force all tenants to accept template updates.
- Do not break current PageModel rendering contracts.

## Current Behavior

`PageModelTemplateUpdatedListener` receives `PageModelTemplateUpdatedEvent` and calls:

```java
writePort.applyTemplateUpdate(
    event.templateId(),
    event.logicalId(),
    event.newModel(),
    event.newSchemaVersion(),
    event.actorId());
```

This should be replaced.

## Proposed Behavior

### Event listener

The listener should stay thin:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateUpdatedListener {

  private final CommandBus commandBus;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void on(PageModelTemplateUpdatedEvent event) {
    commandBus.send(new CreatePageTemplateUpdateNotificationsCommand(...));
  }
}
```

The command/service should:

- find affected tenants/page models,
- classify compatibility,
- create deduped notifications,
- include action payload for review/apply.

### Review endpoints

Recommended endpoints:

- `GET /api/v1/admin/page-model-template-updates`
- `GET /api/v1/admin/page-model-template-updates/{logicalId}/preview`
- `POST /api/v1/admin/page-model-template-updates/{logicalId}/merge`
- `POST /api/v1/admin/page-model-template-updates/{logicalId}/draft`
- `POST /api/v1/admin/page-model-template-updates/{logicalId}/replace`
- `POST /api/v1/admin/page-model-template-updates/{logicalId}/ignore`

The backend should recalculate current state at action time instead of trusting stale notification payloads.

## Compatibility Levels

Template changes should be classified as:

- `PATCH`: safe structural/content update.
- `MINOR`: compatible but needs review.
- `MAJOR`: breaking change; requires migration or explicit replace.

## Merge Modes

- `MERGE_SAFE`: no conflicts detected.
- `MERGE_WITH_CONFLICTS`: possible merge, but admin review required.
- `CREATE_DRAFT`: create draft from merged result.
- `REPLACE_ALL`: replace existing model with template model.
- `IGNORE`: do nothing and mark notification/action handled.
- `REQUIRES_MIGRATION`: cannot apply automatically.

## Conflict Policy

A conflict exists when:

- a tenant customized the same section/widget/key changed by the template,
- schema version changes incompatibly,
- widget type removed or changed in a breaking way,
- required layout/content keys are missing,
- protected tenant overrides would be lost.

## Rollback/Safety

Before replace/merge on a published model, the system should:

- create a draft or backup snapshot,
- keep previous published model available,
- audit the decision,
- never make published pages unusable.

## Risks

- Recomputing state at action time must handle template changing again.
- Notification payload can become stale; backend must validate before applying.
- Over-notification if idempotency/dedupe is not enforced.

## Rollout

1. Introduce notifications for template updates.
2. Deprecate direct `applyTemplateUpdate` from event listener.
3. Add preview/diff service.
4. Add merge/draft/replace/ignore commands.
5. Add audit logs.
6. Later, add durable proposal table only if needed.
