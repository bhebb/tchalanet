# Task 01 — i18n Java core (enum, entity, mapper, policy)

## Goal

Add `I18nSurface`, update `I18nOverrideEntity`, update `I18nOverrideMapper`, create `PublicI18nSurfacePolicy`.

## Steps

### 1. Create `I18nSurface` enum

Package: `com.tchalanet.server.catalog.i18n.api.model`

```java
public enum I18nSurface {
    PUBLIC_HOME,
    PUBLIC_RESULTS,
    PUBLIC_TICKET_CHECK,
    COMMON_PUBLIC_ERROR,
    AUTH,
    CASHIER,
    TENANT_ADMIN,
    PLATFORM_ADMIN,
    COMMON_PRIVATE_ERROR,
    INTERNAL
}
```

### 2. Create `PublicI18nSurfacePolicy`

Package: `com.tchalanet.server.catalog.i18n.api.model` (or `catalog.i18n.api`)

```java
public final class PublicI18nSurfacePolicy {

    private static final Set<I18nSurface> PUBLIC_SURFACES = Set.of(
        I18nSurface.PUBLIC_HOME,
        I18nSurface.PUBLIC_RESULTS,
        I18nSurface.PUBLIC_TICKET_CHECK,
        I18nSurface.COMMON_PUBLIC_ERROR
    );

    private PublicI18nSurfacePolicy() {}

    public static boolean isPublic(I18nSurface surface) {
        return PUBLIC_SURFACES.contains(surface);
    }

    public static Set<I18nSurface> publicSurfaces() {
        return PUBLIC_SURFACES;
    }
}
```

### 3. Update `I18nOverrideEntity`

Add field:

```java
@Enumerated(EnumType.STRING)
@Column(name = "surface", nullable = false, length = 50)
private I18nSurface surface = I18nSurface.INTERNAL;
```

Update `fullKey()` if it exists to include surface: `surface + ":" + locale + ":" + i18nKey`.

### 4. Update `I18nOverrideMapper`

Map `surface` in all `entity → view` and `request → entity/command` conversions.

## Acceptance criteria

- `I18nSurface` enum compiles with all 10 values.
- `PublicI18nSurfacePolicy.publicSurfaces()` returns the 4 public surfaces.
- `I18nOverrideEntity.surface` defaults to `INTERNAL`.
- Mapper compiles and maps `surface` bidirectionally.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
