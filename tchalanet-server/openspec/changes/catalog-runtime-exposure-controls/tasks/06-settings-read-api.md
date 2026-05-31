# Task 06 — settings read API (criteria, view, catalog)

## Goal

Update `SettingView`, add `exposure` to all criteria objects, update repository and catalog impl.

## Steps

### 1. Update `SettingView`

Add `SettingExposure exposure` field to the existing view record.

### 2. Update `SearchSettingsAdminCriteria`

Add `SettingExposure exposure` (nullable — null means **no exposure filter**, all exposures returned).

**Caller rules:**
- Super admin search → passes `null` (no filter — sees all exposures, all tenants).
- Tenant admin search → passes `null` (no filter — sees all exposures scoped to their tenant by RLS).
- Public runtime endpoint → always passes `PUBLIC_RUNTIME` (never null).

### 3. Update `SearchSettingsCriteria` (web model)

Add `SettingExposure exposure` (nullable — null means no filter for admin callers).

### 4. Update `ResolveSettingsCriteria`

Add `SettingExposure exposure`. For public bootstrap resolvers this must be `PUBLIC_RUNTIME`. For internal resolvers and admin resolvers, pass `null`.

### 5. Update `SettingRepository`

Add or update query method to filter by exposure:

```java
List<SettingEntity> findByTenantIdAndExposureAndActiveTrue(UUID tenantId, SettingExposure exposure);
```

Or with namespace:

```java
List<SettingEntity> findByTenantIdAndExposureAndNamespaceAndActiveTrue(
    UUID tenantId, SettingExposure exposure, String namespace
);
```

Adapt to the project's JPA/query pattern.

### 6. Update `SettingsCatalogImpl`

Wire the new repository methods. When `criteria.exposure()` is non-null, filter by it. When null, skip the filter (admin all-access).

Add a dedicated runtime read method to `SettingsCatalog` if needed:

```java
List<SettingView> listActiveByExposure(UUID tenantId, SettingExposure exposure);
```

## Acceptance criteria

- `SettingView` includes `exposure`.
- All criteria objects accept `exposure` as a nullable filter.
- Repository supports `exposure`-based filtering.
- Catalog impl applies `exposure` filter when set.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
