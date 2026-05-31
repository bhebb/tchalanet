# Task 06 — settings read API (criteria, view, catalog)

## Goal

Update `SettingView`, add `exposure` to all criteria objects, update repository and catalog impl.

## Steps

### 1. Update `SettingView`

Add `SettingExposure exposure` field to the existing view record.

### 2. Update `SearchSettingsAdminCriteria`

Add `SettingExposure exposure` (nullable — null means no filter).

### 3. Update `SearchSettingsCriteria` (web model)

Add `SettingExposure exposure` (nullable).

### 4. Update `ResolveSettingsCriteria`

Add `SettingExposure exposure` if the resolver should filter by exposure. For public bootstrap resolvers, this must be `PUBLIC_RUNTIME`.

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
