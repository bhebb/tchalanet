# Task 02 — i18n read API (criteria, view, catalog, bundle view)

## Goal

Update `I18nOverrideView`, upgrade `SearchI18nOverridesCriteria` to `Set<I18nSurface>`, update repository and catalog impl, add `I18nBundleView` for runtime reads.

## Steps

### 1. Update `I18nOverrideView`

Add `I18nSurface surface` field to the existing view record. Adjust field order to match entity declaration order.

### 2. Upgrade `SearchI18nOverridesCriteria`

Replace single `surface` field (if present) with `Set<I18nSurface> surfaces`. Null or empty set means "no surface filter" (return all).

```java
public record SearchI18nOverridesCriteria(
    String locale,
    Set<I18nSurface> surfaces,
    I18nOverrideLevel level,
    Boolean active
) {}
```

Update all callers of this criteria.

### 3. Update `I18nOverrideRepository`

Add or update query method to filter by `surface IN (:surfaces)`:

```java
List<I18nOverrideEntity> findBySurfaces(
    UUID tenantId, Set<I18nSurface> surfaces, String locale, Boolean active
);
```

Adapt to the project's JPA/query pattern (Spring Data derived query or `@Query`).

### 4. Update `I18nOverridesCatalogImpl`

Wire the new repository method. When `criteria.surfaces()` is non-empty, apply `surface IN` filter. When empty/null, skip the filter.

### 5. Create `I18nBundleView`

Package: `com.tchalanet.server.catalog.i18n.api.model`

```java
public record I18nBundleView(
    String locale,
    Map<I18nSurface, Map<String, String>> surfaces
) {}
```

This is the runtime read shape (multi-surface grouped). The admin CRUD path keeps using `I18nOverrideView`.

### 6. Add runtime read method to `I18nOverridesCatalog`

```java
I18nBundleView loadBundle(String locale, Set<I18nSurface> surfaces);
```

Implement in `I18nOverridesCatalogImpl` with the GLOBAL → TENANT merge/fallback logic:

```
1. load GLOBAL rows for (surfaces ∩ requested) + locale
2. load TENANT rows for (surfaces ∩ requested) + locale
3. merge tenant over global per surface
4. return I18nBundleView grouped by surface
```

For V1, tenant must be resolved by the caller before invoking this method.

## Acceptance criteria

- `SearchI18nOverridesCriteria.surfaces` is `Set<I18nSurface>`.
- Repository filters by `surface IN (...)`.
- `I18nBundleView` record exists and is distinct from `I18nOverrideView`.
- `loadBundle()` exists on the catalog interface and implementation.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
