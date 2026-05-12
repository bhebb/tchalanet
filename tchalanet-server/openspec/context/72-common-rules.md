# OpenSpec — Common Rules (72)

## Status

NORMATIVE.

## 1. Definition

`common` is the Technical Shared Kernel.

It contains technical primitives only. It does not contain application capabilities, workflow persistence, tenant/user policies, or business decisions.

## 2. Allowed packages

Examples:

```text
common.bus
common.types.id
common.context
common.web.api
common.web.paging
common.problem
common.cache
common.tx
common.stereotype
common.validation
common.mapper
```

## 3. Forbidden in common

```text
Audit logging domain/workflow
Access control policies and role assignments
Tenant configuration values
Tenant theme overrides
User profiles / app users
Document management
Communication delivery
Idempotency records/workflows
Security permission evaluation that depends on platform data
```

## 4. De-engraissement rule

If a class in `common` has one of these properties, migrate it to `platform`:

- owns DB table/entity/repository;
- sends external communication;
- stores per-tenant/per-user data;
- performs permission/policy decisions;
- represents an application workflow;
- needs cache invalidation from writes;
- emits application events.

## 5. Allowed abstractions

`common` may keep pure annotations/interfaces used by several modules if they have no dependency on platform/core/catalog/features.

Examples:

- marker annotation
- pure interface
- typed ID wrapper
- exception helper
- generic cache/tx abstraction
