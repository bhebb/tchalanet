# Spec — Migrate core.audit to platform.audit

## Goal

Move audit from `core.audit` to `platform.audit` because audit is a transversal application/compliance service, not a lottery core domain.

## Old package

```text
com.tchalanet.server.core.audit.*
```

## New package

```text
com.tchalanet.server.platform.audit.api.*
com.tchalanet.server.platform.audit.internal.*
```

## Public API

```text
platform.audit.api.AuditApi
platform.audit.api.model.AuditAction
platform.audit.api.model.AuditEntityType
platform.audit.api.model.LogAuditEventRequest
platform.audit.api.model.AuditEventView
```

## Internal implementation

```text
internal/app/AuditLoggingService
internal/persistence/AuditEventJpaEntity
internal/persistence/AuditEventRepository
internal/persistence/envers/*
internal/web/AuditOpsController or AuditAdminController
internal/event/*
```

## Keep in common

Only pure annotations or technical hooks may remain in common if they do not depend on platform data.

Example:

```text
@AuditLog may remain common.stereotype/audit if used by web aspects broadly,
but the implementation/aspect that writes audit should call platform.audit.api.AuditApi.
```

## Rename rules

```text
core.audit.infra.persistence.envers.TchRevisionListener
  -> platform.audit.internal.persistence.envers.TchRevisionListener

core.audit.application.command.model.LogAuditEventCommand
  -> platform.audit.api.model.LogAuditEventRequest OR internal command if bus retained

core.audit.application.command.handler.LogAuditEventHandler
  -> platform.audit.internal.app.AuditLoggingService
```

## Migration tasks

- [ ] Create `AuditApi`.
- [ ] Move audit persistence and Envers listener.
- [ ] Move audit logging aspect/service wiring to platform.
- [ ] Preserve after-commit success logging semantics.
- [ ] Preserve failure audit in independent transaction if currently implemented.
- [ ] Update all imports.
- [ ] Remove legacy core.audit package.

## Verification

- [ ] Write endpoints still create audit records.
- [ ] Envers revision listener still enriches tenant/user/request metadata.
- [ ] Audit failure does not break main operation.
- [ ] No import of `platform.audit.internal` outside platform.audit.
