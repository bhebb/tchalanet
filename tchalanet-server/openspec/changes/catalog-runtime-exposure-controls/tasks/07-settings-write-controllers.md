# Task 07 — settings write path + controllers

## Goal

Add `exposure` to create/update request DTOs and wire it through both platform and tenant controllers.

## Steps

### 1. Update `CreateSettingAdminRequest` (admin/platform model)

Add:

```java
@NotNull SettingExposure exposure
```

Default: `INTERNAL`. Never default to `PUBLIC_RUNTIME`.

### 2. Update `CreateSettingRequest` (tenant web model)

Add `SettingExposure exposure`. Make mandatory or nullable — follow the existing convention for this DTO. Default to `INTERNAL` if omitted.

### 3. Update `UpdateSettingRequest`

Add `SettingExposure exposure`. Nullable patch semantics or mandatory — follow existing convention.

### 4. Update `PlatformSettingsController`

- Accept `exposure` in create/update endpoints.
- Allow filtering by `exposure` on list/search endpoints if they exist.

### 5. Update `TenantSettingsController`

- Accept `exposure` in create/update if tenant admins may set it.
- If tenants must not set `PUBLIC_RUNTIME` themselves (platform-only privilege), validate and reject with `403` or `400`.
- Allow filtering by `exposure` on list endpoints.

### 6. Public endpoint (plumbing only)

If a `/public/settings` endpoint exists or is created here:

- Filter strictly by `PUBLIC_RUNTIME` before reading — never pass `INTERNAL`, `TENANT_RUNTIME`, or `ADMIN_RUNTIME`.
- Use `PublicSettingExposurePolicy.publicExposures()` for the allowlist check.

## Acceptance criteria

- Create/update DTOs include `exposure`.
- Platform controller wires `exposure` on create/update.
- Tenant controller wires `exposure`; validate if tenant write of `PUBLIC_RUNTIME` is restricted.
- `./mvnw compile -pl tchalanet-catalog -am -q` clean.
