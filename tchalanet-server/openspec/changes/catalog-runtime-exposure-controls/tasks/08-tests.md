# Task 08 — Tests

## i18n tests

```
entity:
  - I18nOverrideEntity.surface defaults to INTERNAL when not set
  - fullKey() includes surface prefix

mapper:
  - entity → view maps surface correctly
  - create request → entity maps surface correctly

policy:
  - PublicI18nSurfacePolicy.isPublic returns true for all 4 public surfaces
  - PublicI18nSurfacePolicy.isPublic returns false for CASHIER, TENANT_ADMIN, PLATFORM_ADMIN, INTERNAL, AUTH
  - publicSurfaces() returns exactly the 4 public surfaces

catalog / read:
  - loadBundle returns keys grouped by surface
  - loadBundle with [PUBLIC_HOME, PUBLIC_RESULTS] returns two surface groups
  - SearchI18nOverridesCriteria with surfaces filters by surface IN (...)
  - GLOBAL public rows are returned when no tenant override exists
  - TENANT public rows override GLOBAL public rows for same key and surface
  - same i18n key can exist in PUBLIC_HOME and TENANT_ADMIN without unique constraint conflict

security / public endpoint (if implemented in task 03):
  - request with PUBLIC_HOME + PUBLIC_RESULTS → 200, grouped bundle
  - request with PUBLIC_HOME + TENANT_ADMIN → 400 invalid_public_surface
  - TENANT_ADMIN rows never appear in public bundle even if tenant has them
```

## settings tests

```
entity:
  - SettingEntity.exposure defaults to INTERNAL when not set

mapper:
  - entity → view maps exposure correctly
  - create request → entity maps exposure correctly

policy:
  - PublicSettingExposurePolicy.isPublic returns true for PUBLIC_RUNTIME only
  - PublicSettingExposurePolicy.isPublic returns false for INTERNAL, TENANT_RUNTIME, ADMIN_RUNTIME
  - publicExposures() returns exactly {PUBLIC_RUNTIME}

catalog / read:
  - listActiveByExposure(PUBLIC_RUNTIME) returns only PUBLIC_RUNTIME settings
  - listActiveByExposure(PUBLIC_RUNTIME) does not return INTERNAL settings
  - listActiveByExposure(PUBLIC_RUNTIME) does not return TENANT_RUNTIME or ADMIN_RUNTIME settings
  - criteria with exposure=null returns all settings (no filter)

security / public endpoint (if implemented in task 07):
  - /public/settings returns only PUBLIC_RUNTIME settings
  - INTERNAL settings are never in the response
```

## Schema / migration tests

```
  - Flyway migration parses cleanly (compile + test-compile on tchalanet-app)
  - i18n_override.surface column exists and is NOT NULL
  - app_setting.exposure column exists and is NOT NULL
  - Hibernate ddl-auto=validate passes (schema matches entity declarations)
```

## Admin visibility tests

```
i18n — super admin:
  - search with no surface filter returns all surfaces (PUBLIC_HOME, CASHIER, INTERNAL, etc.)
  - search with no surface filter returns rows from all tenants
  - surface filter is null/empty → no WHERE surface IN clause in query

i18n — tenant admin:
  - search returns all surfaces for own tenant (PUBLIC_HOME, CASHIER, TENANT_ADMIN, INTERNAL)
  - search returns GLOBAL rows for all surfaces
  - does not return rows for other tenants (RLS enforced)
  - surface filter is null/empty → no surface constraint applied

settings — super admin:
  - search with no exposure filter returns all exposures (PUBLIC_RUNTIME, INTERNAL, TENANT_RUNTIME, ADMIN_RUNTIME)
  - search with no exposure filter returns settings from all tenants
  - exposure filter is null → no WHERE exposure = clause in query

settings — tenant admin:
  - search returns all exposures for own tenant (PUBLIC_RUNTIME, TENANT_RUNTIME, ADMIN_RUNTIME, INTERNAL)
  - does not return settings for other tenants (RLS enforced)
  - exposure filter is null → no exposure constraint applied
```

## Public endpoint tests

```
GET /public/i18n:
  - multiple public surfaces → 200, grouped bundle
  - single public surface → 200, bundle with one surface key
  - TENANT_ADMIN surface among request → 400 invalid_public_surface
  - all private surfaces → 400 invalid_public_surface
  - missing surface param → 400
  - empty surface list → 400
  - tenant_admin surface never appears in response
  - CASHIER, PLATFORM_ADMIN, INTERNAL never appear in response

GET /public/settings:
  - returns only PUBLIC_RUNTIME settings
  - INTERNAL settings never in response
  - TENANT_RUNTIME never in response
  - ADMIN_RUNTIME never in response
  - namespace filter returns only settings in that namespace
  - does not accept tenantId as query parameter

SettingKeyDef:
  - exposureOverridable=false → write attempt to change exposure rejected
  - exposureOverridable=true → exposure change accepted if valid value
  - unknown key creation defaults to INTERNAL
```

## Run

```
./mvnw test -pl tchalanet-catalog -am -q
./mvnw compile -pl tchalanet-app -am -q
```
