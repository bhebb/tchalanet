# Proposal — Catalog Runtime Exposure Controls (i18n + settings)

## Why

`i18n_override` and `app_setting` can identify *where* a value is defined (`GLOBAL` / `TENANT` / `OUTLET` / `TERMINAL`) but cannot safely answer:

- Can this value be returned by `/public/**`?
- Can this value be returned by `/tenant/**` runtime?
- Is this value admin-only?
- Is this value internal/backend-only?

This means any public runtime bootstrap would either have to leak all values or apply ad-hoc filtering in controller code. Neither is acceptable.

**Critical invariant:**

```
GLOBAL does not mean PUBLIC.
TENANT does not mean PRIVATE.
PUBLIC / PUBLIC_RUNTIME must be explicit.
```

## What

Add a **surface** dimension to `i18n_override` and an **exposure** dimension to `app_setting` so each row carries its own runtime visibility.

### catalog.i18n — `I18nSurface`

A surface identifies which UI area or app section a translation belongs to, and whether it is public-safe.

Public-safe surfaces (may be served by `/public/**`):

```
PUBLIC_HOME
PUBLIC_RESULTS
PUBLIC_TICKET_CHECK
COMMON_PUBLIC_ERROR
```

Private surfaces (never served by `/public/**`):

```
AUTH
CASHIER
TENANT_ADMIN
PLATFORM_ADMIN
COMMON_PRIVATE_ERROR
INTERNAL
```

Default for new overrides: `INTERNAL`.

### catalog.settings — `SettingExposure`

An exposure label identifies who may read a setting at runtime.

```
INTERNAL       — backend/application only, never returned by runtime endpoints
PUBLIC_RUNTIME — safe for anonymous/public bootstrap
TENANT_RUNTIME — safe for authenticated cashier / private app bootstrap
ADMIN_RUNTIME  — safe for tenant admin management views
```

Default for new settings: `INTERNAL`.

### Multi-surface i18n runtime reads (addendum)

The runtime read endpoint must accept multiple surfaces in one request to avoid one HTTP call per page during public bootstrap.

```http
GET /public/i18n?locale=fr&surface=PUBLIC_HOME&surface=PUBLIC_RESULTS&surface=PUBLIC_TICKET_CHECK
```

Response is grouped by surface:

```json
{
  "locale": "fr",
  "surfaces": {
    "PUBLIC_HOME": { "home.hero.title": "Bienvenue" },
    "PUBLIC_RESULTS": { "results.title": "Résultats" }
  }
}
```

## Non-goals

- Do not create a full `/public/bootstrap` endpoint (PageModel, theme, shell, public content aggregation).
- Minimal public runtime endpoints for i18n and settings **are in scope** — see design.md.
- Do not implement tenant domain/subdomain resolution.
- Do not implement public tenant selection by tenantCode/publicSlug (future).
- Do not create `ALTER TABLE` migrations — edit V100 in place (pre-go-live).
- Do not redesign the catalog module.
- Do not expose JPA entities.
- Do not implement full HSM/KMS or key rotation for settings encryption.
