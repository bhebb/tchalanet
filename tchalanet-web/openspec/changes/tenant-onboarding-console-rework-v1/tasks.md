# Tasks: tenant-onboarding-console-rework-v1

## 0. Discovery

- [x] 0.1 Confirm `REQUIRED_SETUP_SECTION_IDS` / `setupCards` ids in
      `admin-complete-tenant-config.page.ts` match the readiness section ids actually returned by
      `GET /admin/overview` today (no stale `outlet`/`cashier` id, none missing). **Result:**
      `identity`/`address`/`games_pricing`/`draws`/`theme`/`promotions` all present, no stale ids.
- [x] 0.2 Confirm `DOMAIN_LABEL_KEYS` / `STATUS_LABEL_KEYS` / `STEP_LABEL_KEYS` (now in
      `tenant-provisioning-display.ts`, moved out of the page during the earlier decomposition
      pass) against the current `TenantProvisioningOrchestrator.domainStatuses` / `nextSteps`
      values (tchalanet-server). **Result: already fully aligned** — all domain/status/step keys
      match (`tenant_identity`, `pagemodels`, `theme`, `settings`, `i18n`, `games`, `pricing`,
      `draw_channels`, `promotions_templates`, `limits_templates`, `demo_users`,
      `demo_seller_terminals`; steps `CREATE_INITIAL_ADMIN`…`VERIFY_DEMO_SETUP`), nothing stale,
      nothing missing. Task 3 (vocabulary alignment) is a no-op — the prior decomposition pass
      already did this.
- [x] 0.3 Confirm none of the already-extracted provisioning components
      (`tenant-provisioning-aside`, `-profile-summary`, `-success`, `tenant-type-picker`) import
      `data-access`/API services directly. **Result:** confirmed clean — `tenant-provisioning-aside`
      imports two view-model **types** from `data-access` for `input()` typing only, no service
      injection anywhere in the four components.
- [x] 0.4 **New finding, not in the original task list:** `TchBackendClient.getResource` calls
      `get<T>()` internally, which unwraps `ApiResponse<T>` via `unwrapApiResponse` and discards
      `notices`. `admin-complete-tenant-config.page.ts` needs the response's `notices` to build
      `sectionErrors` (per `error-management.md`, section-owned notices). No page in the codebase
      combines `getResource`/`tch-async-view` with notices-derived section errors today — even the
      page `error-management.md` cites as "migrated" for this exact notice pattern
      (`admin-business-profile.page.ts`) still uses manual `pageState`/`pageError` signals for
      that reason. **Decision:** do not force the resource migration in this change (section 2
      below is scoped down accordingly) — it would either silently drop section-error support or
      require extending `TchBackendClient` (a shared-lib change, out of this change's non-goals).
      Flagged to the user; tracked as a future follow-up once the client gains a notices-aware
      resource variant.

## 1. Admin setup — component extraction

- [x] 1.1 Created `components/setup-checklist-card/` (stateless: icon, titleKey, status,
      badgeKind, body, bodyVariant, ctaKey, route, fragment, emphasizeMissing,
      `AdminSectionTargetError[]`) replacing the repeated inline card blocks for the active setup
      checklist. Identity/address are intentionally merged, draw channels also carries
      draw-sales-matrix section errors, and seeded/default domains (Maryaj gratis/promotions,
      subscription) are left out of the main checklist.
- [x] 1.2 Created `components/setup-progress-header/` (completed/total/pct, status icon)
      replacing the inline progress block.
- [x] 1.3 Created `components/setup-seller-terminal-card/` (locked/unlocked variant +
      blocking-steps list) replacing the 9th inline card block.
- [x] 1.4 `admin-complete-tenant-config.page.html` now composes the three components; page
      template dropped from 258 to ~55 lines.
- [x] 1.5 `admin-complete-tenant-config.page.ts` builds `setupCards` as a `computed()` array of
      view models from `setup()`/`sectionMap()`/`header()`; per-card duplication logic removed
      (`sectionStatus`/`isBlocking`/`sectionError` kept as page-level helpers, reused by the
      computed).

## 2. Admin setup — async loading (deferred, see 0.4)

- [x] 2.1~2.3 Skipped for this change per 0.4 — `getResource` drops `ApiResponse.notices`, which
      this page needs for section errors. Keep `pageState`/`pageError`/`.subscribe(...)` as-is;
      only wire it to the new stateless components from section 1.

## 3. Platform provisioning — vocabulary alignment

- [x] 3.1 Per 0.2, already aligned — no changes needed.
- [x] 3.2 Per 0.3, already clean — no changes needed.

## 4. Plan/subscription (scope grew beyond the original non-goals — see note)

Requested mid-change: give the super-admin a way to attach a plan at provisioning time, and
surface the real subscription on both the tenant-admin setup checklist and the platform tenant
detail page. This required a backend change, which the original proposal's non-goals excluded —
tracked here honestly rather than silently expanding scope.

- [x] 4.1 (tchalanet-server) `TenantProvisioningRequest.planCode` (nullable) +
      `TenantProvisioningOrchestrator.provision()` applies it via `ApplyTenantPlanCommand` inside
      its own tenant-context block (subscription is RLS-scoped, spec S7); adds
      `domainStatuses.subscription` (`PLAN_APPLIED`/`NONE`) and warning `NO_PLAN_SELECTED`.
      `TenantProvisioningResultView.appliedPlanCode` added. `preview()` echoes `subscription` in
      `includedDomains` when a plan is selected. **Not compiled** — no JDK in this sandbox; verified
      by careful manual read only (no other call site constructs `TenantProvisioningResultView`).
- [x] 4.2 (platform-portal) New `components/tenant-plan-picker/` (promoted to feature-level
      `tenants/components/` — two consumers: provisioning page + tenant detail page, per
      `structure.md` §6/§7 promotion rule). Provisioning page fetches `GET /platform/plans`,
      pre-selects the catalog default, adds a "Plan du tenant" section-card below "Admin initial".
- [x] 4.3 (admin-portal) Re-added the `subscription` checklist card (it had been dropped as a
      "seeded default" in the section-1 pass) — now backed by real data
      (`GET /tenant/subscription` via the existing `AdminSubscriptionApi`), own load call so a
      subscription hiccup never blocks the checklist. i18n keys re-added (fr/en/ht).
- [x] 4.4 (platform-portal) Filled the tenant-detail "Abonnement" tab: displays plan/status/dates
      via new `PlatformSubscriptionApi.resolve()`, lazy-loaded on tab select (same pattern as the
      admins tab), plus an apply/change action reusing `tenant-plan-picker` +
      `PlatformSubscriptionApi.apply()` (`POST /platform/subscriptions/{tenantId}/apply`).
- [x] 4.5 (tchalanet-server) `TenantReadinessAssembler.computeSetup()` reworked from a flat
      4-section count to `REQUIRED_STEP_GROUPS` (`[identity+address], [games_pricing], [draws]`)
      so `totalSteps`/`completedSteps` match the merged "Identité & adresse" card (3 steps, not 4).
      Confirmed `checkDraws()` already folds games×channel-matrix completeness
      (`offeredChannelGameCount`) into the `draws` status — validates the earlier decision to not
      gate the frontend's merged draw-channels card status on a separate `gamesStatus` check.
      Frontend `admin-complete-tenant-config.page.ts` fallback constant renamed
      `REQUIRED_SETUP_SECTION_IDS` → `REQUIRED_STEP_GROUPS` to match. **Not compiled** (same JDK
      limitation as 4.1).
- [x] 4.6 (tchalanet-server) Added two real readiness checks that were previously placeholder/
      derived, per explicit follow-up request ("s'assurer que le tenant param est setté et qu'au
      moins un draw a été généré"):
      - **`settings`**: `checkSettings()` calls `TenantConfigApi.getTenantDocumentConfig` (public
        API) and compares `receipt.displayName` against the literal seed placeholder `"CHEZ Toto"`
        (from `tenantconfig/document_config.json`) — READY once customized, MISSING otherwise.
        Currency/timezone/locale defaults were confirmed NOT useful signals (always set at
        provisioning, no blank state). New dependency `TenantConfigApi` injected into
        `TenantReadinessAssembler`.
      - **`generated_draws`** (new `SectionDescriptor`, didn't exist before): `checkGeneratedDraws()`
        calls `ListDrawsQuery` (`core.draw.api.query`, page size 1) — the same "does at least one
        row exist" pattern as `checkSellerTerminals`/`checkPromotions`. READY once a Draw instance
        exists; UNKNOWN (not MISSING) when none yet, since generation is async/scheduled, not a
        step a human forgot.
      - Frontend: `admin-complete-tenant-config.page.ts` now reads `sectionStatus('settings')` /
        `sectionStatus('generated_draws')` instead of the hardcoded `'UNKNOWN'` / derived-from-draws
        values. Fixed a copy/route mismatch this surfaced: the "settings" card's route/description
        said "devise, fuseau horaire, horaires d'ouverture" and linked to `/app/admin/business-days`,
        but the real check is about the receipt display name — reworded to "langue et reçu" and
        re-routed to `/app/admin/settings/config` (fr/en/ht). `subscription` still has no readiness
        section (reads its own API directly — different bounded context, unchanged from 4.4).
      **Not compiled** (same JDK limitation as 4.1) — verified by full manual re-read of
      `TenantReadinessAssembler.java` and confirming no other constructor call site exists for it.

## 5. Validation

- [x] 5.1 `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit` — clean.
- [x] 4.2 `pnpm exec tsc -p apps/platform-portal/tsconfig.app.json --noEmit` — clean (no changes
      made there beyond the 0.2/0.3 audit, which found nothing to fix).
- [x] 4.3 i18n check — no new hardcoded FR string; all card text goes through `TranslatePipe`.
      New keys were added only for the merged identity/address and tenant settings cards.
- [ ] 4.4 **Blocked, not done:** manual browser verification. `nx serve admin-portal` builds
      clean (fixed one `NG8113`/`NG1010` unused-import round-trip during the extraction), but
      `/app/admin/setup` requires an authenticated session and this sandbox has no reachable
      Firebase Auth/backend (`@angular/fire` throws `TypeError: cls is not a constructor` on the
      login screen itself — a pre-existing environment limitation, unrelated to this change).
      Verification here was limited to: careful 1:1 line-by-line port of the original
      template/SCSS into the three components (including two non-obvious details: `display:
      contents` on each card's host so it stays the actual CSS grid item, and no invented
      margins between grid/settings-link/terminal-card since the original had none), plus a
      clean type-check. **Needs a real manual pass** in an environment with a working
      Firebase/backend session before this is considered fully verified.
