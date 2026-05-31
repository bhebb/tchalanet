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

### 5. Wiring for runtime public endpoint (plumbing only)

If a `/public/i18n` endpoint exists or is created in this task:

- Accept `@RequestParam List<I18nSurface> surface` (repeated params).
- Validate: `PublicI18nSurfacePolicy.publicSurfaces().containsAll(requested)` — if not, throw `400 invalid_public_surface`.
- Delegate to `I18nOverridesCatalog.loadBundle(locale, Set.copyOf(surface))`.
- Return `I18nBundleView`.

If no public endpoint exists yet, document the contract in a `follow-up-public-bootstrap.md` note and leave the plumbing in place.

## Acceptance criteria

- Create/update DTOs include `surface`.
- Admin/platform controller wires `surface` on create/update.
- Public endpoint (if created) validates all surfaces against `PublicI18nSurfacePolicy.publicSurfaces()` before reading.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
