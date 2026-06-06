# Tasks

## 1. API boundary

- [x] Create `libs/api` with a documented public API.
- [x] Move common API contracts, response helpers, error mapping, and interceptors.
- [x] Update active app imports and remove migrated app sources.

## 2. Shared config boundary

- [x] Create `libs/shared-config` with a documented public API.
- [x] Move runtime settings, settings API/store/mapping, feature flags, directive, and guard.
- [x] Update active app imports and remove migrated app sources.

## 3. Follow-up boundaries

- [x] Document PageModel, widgets, dashboards, and Web extraction ownership/order.
- [x] Remove only matching migrated backup API/config sources. No backup-only API/config source
      matched the active implementations closely enough to remove in this slice.

## 4. Validation

- [ ] Run focused lint/tests and app build. Lint and tests pass; app build remains blocked by the
      reproducible esbuild deadlock already recorded by the UI foundation change.
- [x] Validate OpenSpec and diff hygiene.
