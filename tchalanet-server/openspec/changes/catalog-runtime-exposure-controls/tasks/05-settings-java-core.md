# Task 05 — settings Java core (enum, entity, mapper, policy)

## Goal

Add `SettingExposure`, update `SettingEntity`, update `SettingMapper`, create `PublicSettingExposurePolicy`.

## Steps

### 1. Create `SettingExposure` enum

Package: `com.tchalanet.server.catalog.settings.api.model`

```java
public enum SettingExposure {
    INTERNAL,
    PUBLIC_RUNTIME,
    TENANT_RUNTIME,
    ADMIN_RUNTIME
}
```

### 2. Create `PublicSettingExposurePolicy`

Package: `com.tchalanet.server.catalog.settings.api.model` (or `catalog.settings.api`)

```java
public final class PublicSettingExposurePolicy {

    private static final Set<SettingExposure> PUBLIC_EXPOSURES = Set.of(
        SettingExposure.PUBLIC_RUNTIME
    );

    private PublicSettingExposurePolicy() {}

    public static boolean isPublic(SettingExposure exposure) {
        return PUBLIC_EXPOSURES.contains(exposure);
    }

    public static Set<SettingExposure> publicExposures() {
        return PUBLIC_EXPOSURES;
    }
}
```

Uses `Set` constant for consistency with `PublicI18nSurfacePolicy`, even though only one value is public today.

### 3. Update `SettingEntity`

Add field:

```java
@Enumerated(EnumType.STRING)
@Column(name = "exposure", nullable = false, length = 50)
private SettingExposure exposure = SettingExposure.INTERNAL;
```

### 4. Update `SettingMapper`

Map `exposure` in all `entity → view` and `request → entity/command` conversions.

### 5. Consider `SettingKeyDef` (registry)

If `SettingKeyDef` should carry a default exposure per key (enforced on write-path validation), add:

```java
SettingExposure defaultExposure()
```

Resolve the open question from `design.md` before implementing. If the registry stays exposure-agnostic, document the decision here.

## Acceptance criteria

- `SettingExposure` enum compiles with 4 values.
- `PublicSettingExposurePolicy.publicExposures()` returns `{PUBLIC_RUNTIME}`.
- `SettingEntity.exposure` defaults to `INTERNAL`.
- Mapper compiles and maps `exposure` bidirectionally.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
