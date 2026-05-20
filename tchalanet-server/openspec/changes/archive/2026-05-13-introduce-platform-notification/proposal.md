# Change: introduce-platform-notification

## Why

Tchalanet needs a transversal notification center for in-app notifications without coupling core domains to UI notification persistence.

## What changes

- Add `platform.notification` capability.
- Add notification persistence, preferences and templates.
- Add event listeners/rules that convert domain/system events to `NotificationIntent`.
- Add tenant/admin/platform notification endpoints.
- Keep external delivery out of this capability.

## Impact

- New platform module package.
- New Flyway migration.
- New ArchUnit rules preventing direct notification persistence outside platform.notification.

---

## Archive Information

**Archived:** 2026-05-13
**Outcome:** Implemented

### Files Modified

- `tchalanet-platform/src/main/java/com/tchalanet/server/platform/notification/**`
- `tchalanet-platform/src/test/java/com/tchalanet/server/platform/notification/**`
- `tchalanet-app/src/main/resources/db/migration/V100__create_core_tables.sql`
- `tchalanet-app/src/main/resources/db/migration/V101__create_audit_tables.sql`
- `tchalanet-app/src/main/resources/db/migration/V103__create_indexes.sql`
- `tchalanet-app/src/main/resources/db/migration/V104__create_triggers.sql`
- `tchalanet-app/src/main/resources/db/migration/V105__configure_rls.sql`
- `tchalanet-app/src/test/java/com/tchalanet/server/architecture/PlatformLayerGatesTest.java`

### Specs Updated

- `openspec/specs/platform-notification/spec.md`
