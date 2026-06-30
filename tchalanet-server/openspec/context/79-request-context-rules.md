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

## 6. Actor types

Two authenticated actor types exist. There is no fallback between them.

- `APP_USER` — tenant/platform admins. Resolved via `AppUserExternalIdentityJpaRepository`.
- `SELLER_TERMINAL` — operational POS sellers. Resolved via `SellerTerminalIdentityLookup`.

A POS request that fails terminal resolution is denied immediately. It does not fall through to AppUser resolution.

## 7. Spring authority format

Tchalanet enriches the Spring `Authentication` object with three authority namespaces:

- `ROLE_*` — AppUser roles (e.g. `ROLE_TENANT_ADMIN`, `ROLE_SUPER_ADMIN`).
- `PERM_*` — effective permissions after role + override resolution (e.g. `PERM_ticket.sell`).
- `ACTOR_*` — actor type marker (e.g. `ACTOR_APP_USER`, `ACTOR_SELLER_TERMINAL`).

Provider claims (Firebase custom claims, Keycloak roles) must not be used directly for authorization. Only Tchalanet-owned authorities derived from the DB are authoritative.

## 8. SUPER_ADMIN tenant override

Tenant override via `X-Tch-Tenant-Override` is explicit and auditable. It must never be inferred from JWT claims. Every use is recorded in the audit log with the override reason.
