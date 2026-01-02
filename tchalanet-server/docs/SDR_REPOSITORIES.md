# SDR Repository Report

This file lists all `@RepositoryRestResource` occurrences found in the codebase and gives a quick review (path, exported flag and suggested action).

Columns:
- File: repository source file
- Interface: Java interface name
- path: value of `path` attribute if present (single segment required)
- exported: value of `exported` attribute if present (true/false) or `not set`
- Recommendation: suggested action to align with `/api/v1/_sdr` strategy

| File | Interface | path | exported | Recommendation |
|---|---:|---|---:|---|
| src/main/java/com/tchalanet/server/core/billing/infra/persistence/SubscriptionRestRepository.java | SubscriptionRestRepository | (not set) | true | Keep exported=true only if you want SDR CRUD; otherwise set exported=false. Path is single-segment (default). |
| src/main/java/com/tchalanet/server/core/payout/infra/rest/payout/PayoutRestRepository.java | PayoutRestRepository | (not set) | true | Keep or set exported=true explicitly (will be served under `/_sdr/payoutRestRepository` or default name). Consider adding explicit `path` in kebab-case if you expose it. |
| src/main/java/com/tchalanet/server/common/persistence/AppSettingRepository.java | AppSettingRepository | app-settings | false | Good: exported=false (platform APIs should be controllers). Path is single-segment. |
| src/main/java/com/tchalanet/server/common/persistence/I18nOverrideRepository.java | I18nOverrideRepository | i18n-overrides | false | Good: exported=false for admin-facing tenant data. |
| src/main/java/com/tchalanet/server/core/theme/infra/persistence/ThemeRestRepository.java | ThemeRestRepository | themes | false | Good: exported=false (platform themes via controller). |
| src/main/java/com/tchalanet/server/core/theme/infra/persistence/JpaThemeRepository.java | JpaThemeRepository | (not set) | false | Good: not exported. |
| src/main/java/com/tchalanet/server/core/outlet/infra/rest/outlet/OutletRestRepository.java | OutletRestRepository | (not set) | true | Currently exported=true — decide if you want outlets via SDR (admin) or via explicit controller; if SDR, consider adding explicit `path = "outlets"`. |
| src/main/java/com/tchalanet/server/core/game/infra/persistence/GameJpaRepository.java | GameJpaRepository | games | (not set) | Annotated and will be considered by detection-strategy=annotated. If you want to expose it via SDR, set `exported=true` explicitly; otherwise set `exported=false`. |
| src/main/java/com/tchalanet/server/core/game/infra/persistence/TenantGameJpaRepository.java | TenantGameJpaRepository | tenant-games | (not set) | Path is single-segment. Decide exported true/false. Prefer exported=false for platform data unless you explicitly want SDR. |
| src/main/java/com/tchalanet/server/core/accesscontrol/infra/persistence/TenantUserRepository.java | TenantUserRepository | tenant-users | false | Good: exported=false. |
| src/main/java/com/tchalanet/server/core/user/infra/persistence/UserPreferenceRestRepository.java | UserPreferenceRestRepository | (not set) | true | exported=true. Confirm this is intended (admin user prefs SDR). |
| src/main/java/com/tchalanet/server/features/pagemodel/shared/PageModelRepository.java | PageModelRepository | page-models | false | Good: exported=false (page models controlled by controllers). |


## Notes & next steps

- `detection-strategy: annotated` means only repositories annotated with `@RepositoryRestResource` will be considered. For safety, prefer explicit `exported = true` on repos you want to expose and `exported = false` on all others.
- Ensure that any `path` values are a single segment in kebab-case (no `/`). If you find multi-segment path values, change them to single segment (I've already fixed many occurrences).
- Recommended visible SDR list (examples): `payouts`, `subscriptions`, `outlets`, `user-preferences` — but review and confirm. If you prefer SDR minimal, set `exported = false` on most repos and only export a small approved list.

## Quick command to re-generate this report locally

To scan the codebase yourself and produce the same table, you can run (requires ripgrep `rg`):

```bash
rg "@RepositoryRestResource" -n src/main/java | sed -E 's/.*src\/(main\/java\/.*\.java):.*/\1/' | sort -u
# then inspect each file for annotation values
```

If you want, I can:
- automatically update the annotations to explicitly `exported = false` on all currently `not set` repos (safer), or
- create a Git patch that sets `exported = true` only for a curated list you confirm.

---

Generated on: 2025-12-31
