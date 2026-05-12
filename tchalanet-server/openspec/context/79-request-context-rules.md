# OpenSpec — Request Context Usage aligned to Modulith (79)

> Status: NORMATIVE

## 1. Canonical producer

HTTP request context is produced by `TchContextFilter`.

No platform/core/catalog/feature service may independently resolve the effective tenant from JWT or request body.

## 2. Tenant policy

Tenant-scoped operations use the effective tenant from context.

Platform APIs that require tenant information receive either:

- typed `TenantId` from the caller's context;
- full `TchRequestContext` when actor/scope/locale/timezone are needed.

## 3. Temporary switching

Temporary context switching is allowed only through explicit scope helpers that restore the previous context.

## 4. Events and async

Events crossing module boundaries carry context metadata if listeners may run outside the original request thread.

## 5. Migration rule

When moving usercontext/accesscontrol/audit/tenantconfig to platform, preserve all existing context semantics before changing behavior.
