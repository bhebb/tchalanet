# Spec — Migrate core.tenantuser to platform.identity

## Goal

Rename and move tenant user/application user capability to `platform.identity`.

## Old package

```text
com.tchalanet.server.core.tenantuser.*
```

## New package

```text
com.tchalanet.server.platform.identity.api.*
com.tchalanet.server.platform.identity.internal.*
```

## Naming decision

Use `identity`, not `usercontext` or `tenantuser`, because:

- The capability covers app user identity, profile, preferences, locale/timezone, tenant association, and bootstrap context.
- `usercontext` conflicts with the low-level `platform.context` primitive (request/execution context).
- `identity` aligns with the API class name `IdentityApi` already decided in `migration/naming-renames.md`.

## Public API

```text
IdentityApi
AppUserView
UserIdentityView
ResolveCurrentIdentityRequest
UserPreferenceView
```

## Internal implementation

```text
internal/app/IdentityResolver
internal/app/AppUserBootstrapService
internal/persistence/AppUserJpaEntity
internal/persistence/UserPreferenceJpaEntity
internal/web/IdentityAdminController
internal/event/*
```

## Migration tasks

- [ ] Create `IdentityApi`.
- [ ] Rename Java packages/classes from tenantuser to identity where appropriate.
- [ ] Move app user/profile/preference persistence.
- [ ] Update `UserBootstrapFilter` or actor enrichment to call `platform.identity.api` through allowed boundary.
- [ ] Keep low-level request context primitives in `common.context`.
- [ ] Update tenant admin features to call `platform.identity.api`.
- [ ] Remove legacy `core.tenantuser` package.

## Verification

- [ ] Login/bootstrap still resolves app user.
- [ ] Tenant admin users UI still works.
- [ ] Request context semantics unchanged.
- [ ] `common.context` does not depend on `platform`.
