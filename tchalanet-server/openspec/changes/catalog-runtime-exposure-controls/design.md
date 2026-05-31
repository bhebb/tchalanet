# Design — Catalog Runtime Exposure Controls

## Schema changes (V100, edit in place)

### i18n_override

Add column:

```sql
surface varchar(50) NOT NULL DEFAULT 'INTERNAL'
```

Add CHECK constraint listing all valid `I18nSurface` values.

Update unique indexes to include `surface` — the same key can exist in different surfaces without conflict:

```sql
CREATE UNIQUE INDEX uq_i18n_override_global
ON i18n_override (surface, locale, i18n_key)
WHERE tenant_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_i18n_override_tenant
ON i18n_override (tenant_id, surface, locale, i18n_key)
WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;
```

Add runtime lookup index:

```sql
CREATE INDEX idx_i18n_override_runtime_lookup
ON i18n_override (tenant_id, surface, locale, active)
WHERE deleted_at IS NULL;
```

### app_setting

Add column:

```sql
exposure varchar(50) NOT NULL DEFAULT 'INTERNAL'
```

Add CHECK constraint listing all valid `SettingExposure` values.

Add runtime lookup index:

```sql
CREATE INDEX idx_app_setting_runtime_lookup
ON app_setting (tenant_id, exposure, active, namespace, setting_key)
WHERE deleted_at IS NULL;
```

Note: `GLOBAL` in the existing `app_setting` level CHECK is inconsistent with `BaseTenantEntity` (non-null `tenant_id`). Remove `GLOBAL` from the CHECK in this task since no seed data uses it.

## Enum packages

```
catalog.i18n.api.model.I18nSurface
catalog.settings.api.model.SettingExposure
```

## Public allowlist policy

Both policy helpers use a `Set` constant — not a chain of `==` comparisons:

```java
public final class PublicI18nSurfacePolicy {
    private static final Set<I18nSurface> PUBLIC_SURFACES = Set.of(
        I18nSurface.PUBLIC_HOME,
        I18nSurface.PUBLIC_RESULTS,
        I18nSurface.PUBLIC_TICKET_CHECK,
        I18nSurface.COMMON_PUBLIC_ERROR
    );

    public static boolean isPublic(I18nSurface surface) {
        return PUBLIC_SURFACES.contains(surface);
    }

    public static Set<I18nSurface> publicSurfaces() {
        return PUBLIC_SURFACES;
    }
}
```

`publicSurfaces()` is available so callers can validate a `Set<I18nSurface>` in bulk:

```java
if (!PublicI18nSurfacePolicy.publicSurfaces().containsAll(requested)) {
    throw ProblemRest.badRequest("invalid_public_surface");
}
```

## i18n criteria — multi-surface

`SearchI18nOverridesCriteria` (and any runtime-specific criteria) uses `Set<I18nSurface>`, not a single surface field:

```java
public record SearchI18nOverridesCriteria(
    String locale,
    Set<I18nSurface> surfaces,
    I18nOverrideLevel level,
    Boolean active
) {}
```

Repository query uses `surface IN (:surfaces)`.

## i18n runtime response — I18nBundleView

The runtime read path returns a grouped bundle, distinct from the admin CRUD `I18nOverrideView`:

```java
public record I18nBundleView(
    String locale,
    Map<I18nSurface, Map<String, String>> surfaces
) {}
```

The admin CRUD path continues using `I18nOverrideView` (per-row shape).

## Public runtime controllers (in scope)

Minimal public runtime endpoints are shipped in this change. They are catalog-only reads — they do not aggregate PageModel, theme, shell, or public content.

Controller placement:
- `catalog.i18n.internal.web.PublicI18nRuntimeController`
- `catalog.settings.internal.web.PublicSettingsRuntimeController`

These live inside `catalog.*` because they are mono-catalog reads. A future `/public/bootstrap` that aggregates across catalogs belongs in a `features/` or BFF slice.

### GET /public/i18n

```
locale   required
surface  repeated, required — 400 if missing or empty
```

`surface` is **required**. Do not silently default to all surfaces. If a default is ever needed, use `PUBLIC_HOME + COMMON_PUBLIC_ERROR` — but prefer requiring it explicitly.

### GET /public/settings

```
namespace  optional
```

Exposure is **not** a query parameter. The endpoint always filters by `PUBLIC_RUNTIME` server-side. Clients cannot request other exposure levels.

Both endpoints use the public tenant resolved from server context. Do not accept `tenantId` UUID from the client.

## Controller binding — repeated query params

Spring MVC binds repeated `surface=` params to `List<I18nSurface>` automatically when the parameter type is a list/set of enums. No CSV parsing.

```java
@GetMapping("/public/i18n")
public I18nBundleView getPublicBundle(
    @RequestParam String locale,
    @RequestParam List<I18nSurface> surface
) { ... }
```

## Security invariants

```
/public/**  → only PUBLIC_SURFACES; any private surface → 400 invalid_public_surface
/public/**  → only PUBLIC_RUNTIME settings; INTERNAL/TENANT_RUNTIME/ADMIN_RUNTIME never returned
/tenant/**  → PUBLIC_RUNTIME + TENANT_RUNTIME (depending on use case)
/admin/**   → PUBLIC_RUNTIME + TENANT_RUNTIME + ADMIN_RUNTIME (depending on permissions)
INTERNAL    → never leaves the backend
```

Public tenant selection (future) must use a public code/slug, never a tenant UUID.

## Defaults

| Context | i18n default | settings default |
|---|---|---|
| Admin creates override without explicit surface | `INTERNAL` | — |
| Admin creates setting without explicit exposure | — | `INTERNAL` |
| Existing rows after migration | `INTERNAL` (column DEFAULT) | `INTERNAL` (column DEFAULT) |

Never default to `PUBLIC_HOME` or `PUBLIC_RUNTIME` on create.

## SettingKeyDef — canonical exposure (resolved)

**Decision: add `defaultExposure` + `exposureOverridable` to `SettingKeyDef` in this change.**

Exposure is security-sensitive. Known system keys must not have their exposure set arbitrarily by an admin.

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

Rules:
- Known system keys: `defaultExposure` comes from `SettingKeyDef`.
- `exposureOverridable = false` → admin cannot change exposure.
- `exposureOverridable = true` → admin may choose among allowed exposures.
- Unknown/custom settings → default to `INTERNAL`.

If `SettingKeyDef` already exists and is easy to update, add it now. Otherwise create a follow-up task — but admin-created settings must always default to `INTERNAL`.

## SettingsBatchGateFlagStore — exposure filtering (resolved)

**Decision: no exposure filtering in `SettingsBatchGateFlagStore`.**

It is a backend-internal reader for operational flags. Batch gates are internal and should be seeded with `exposure = INTERNAL`. Public exposure filtering applies only to public runtime readers, not to internal infrastructure reads.

Do not apply exposure filtering globally at the repository level. Add it only in specific public/runtime catalog methods.
