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

## Run

```
./mvnw test -pl tchalanet-catalog -am -q
./mvnw compile -pl tchalanet-app -am -q
```
