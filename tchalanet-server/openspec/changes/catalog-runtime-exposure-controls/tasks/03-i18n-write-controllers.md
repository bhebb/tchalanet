# Task 03 — i18n write path + controllers

## Goal

Add `surface` to create/update request DTOs and wire it through controllers. Wire `PublicI18nSurfacePolicy` validation on any public endpoint.

## Steps

### 1. Update `CreateI18nOverrideRequest` (tenant web model)

Add:

```java
@NotNull I18nSurface surface
```

Default in spec: `INTERNAL`. If the DTO is strict (all fields required), make it mandatory. Never default to `PUBLIC_HOME`.

### 2. Update `UpdateI18nOverrideRequest` (tenant web model)

Add:

```java
I18nSurface surface
```

Nullable update (patch semantics) or mandatory — follow the existing convention for this DTO.

### 3. Update `CreateI18nOverrideAdminRequest` (admin/platform model)

Add `@NotNull I18nSurface surface` — same rule as above.

### 4. Update `PlatformI18nOverridesController`

- Accept `surface` in create/update endpoints.
- Allow filtering by `surface` on list/search if a list endpoint exists (pass `Set.of(surface)` to criteria).

### 5. Create `PublicI18nRuntimeController`

Package: `catalog.i18n.internal.web`

```java
@RestController
@Tag(name = "Public i18n")
public class PublicI18nRuntimeController {

    @GetMapping("/public/i18n")
    public I18nBundleView getBundle(
        @RequestParam String locale,
        @RequestParam List<I18nSurface> surface
    ) { ... }
}
```

Rules:
- `surface` is **required** — reject with `400` if missing or empty.
- Validate: `PublicI18nSurfacePolicy.publicSurfaces().containsAll(Set.copyOf(surface))` — if not, throw `400 invalid_public_surface`.
- Do not silently ignore private surfaces.
- Delegate to `I18nOverridesCatalog.loadBundle(locale, Set.copyOf(surface))`.
- Return `I18nBundleView`.
- Do not accept `tenantId` from the request. Use server-resolved public tenant context.
- Controller stays thin: validate → policy check → catalog dispatch → return DTO.

## Acceptance criteria

- Create/update DTOs include `surface`.
- Admin/platform controller wires `surface` on create/update.
- `PublicI18nRuntimeController` exists at `GET /public/i18n`.
- Missing or empty `surface` → `400`.
- Any private surface in `surface` list → `400 invalid_public_surface`.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
