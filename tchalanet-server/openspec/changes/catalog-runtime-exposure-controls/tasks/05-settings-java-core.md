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

### 5. Update `SettingKeyDef` (registry)

Add `defaultExposure` and `exposureOverridable` to `SettingKeyDef`:

```java
public record SettingKeyDef(
    String namespace,
    String key,
    SettingValueType valueType,
    SettingLevel allowedLevel,
    SettingExposure defaultExposure,
    boolean exposureOverridable
) {}
```

Update `SettingsRegistry` and `SettingsValidator` to:
- Use `SettingKeyDef.defaultExposure()` as the default when a known key is created without an explicit exposure.
- Reject writes that change exposure on a key where `exposureOverridable = false`.

Update all existing `SettingKeyDef` usages in `SettingsRegistry` to declare `defaultExposure`. Known public runtime keys (e.g. `public.brand_name`, `public.supported_languages`, `public.default_locale`) should be `PUBLIC_RUNTIME, exposureOverridable = false`. Everything else defaults to `INTERNAL`.

## Acceptance criteria

- `SettingExposure` enum compiles with 4 values.
- `PublicSettingExposurePolicy.publicExposures()` returns `{PUBLIC_RUNTIME}`.
- `SettingEntity.exposure` defaults to `INTERNAL`.
- Mapper compiles and maps `exposure` bidirectionally.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
