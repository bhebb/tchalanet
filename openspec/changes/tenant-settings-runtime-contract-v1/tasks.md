# Tasks

- [x] Inspect current backend tenant settings ownership, JSON fragments, validator, and
      tenant-admin controller placement.
- [x] Inspect admin web settings tab, setup settings card, and platform config summary.
- [x] Inspect mobile runtime/settings consumption at a high level.
- [x] Inspect tenant/provider calendar implementation and document current usage gaps.
- [x] Inventory each tenant config block against its actual runtime/print/calendar/fee consumer.
- [x] Document the difference between tenant runtime columns and JSON tenant policy blocks.
- [x] Check existing OpenSpec context for tenant locale source of truth.
- [x] Backend: remove or constrain JSON `locale.defaultLanguage/defaultLocale` as legacy/mirror
      fields; canonical defaults belong to `tenant.default_language/default_locale`.
- [x] Backend: update locale validation so extended JSON locale policy does not require duplicate
      default language/default locale fields as an independent source.
- [x] Backend: expose tenant-supported language values for admin/user/seller-terminal language
      pickers instead of requiring each client to hardcode `fr/en/ht`.
- [x] Backend: expose tenant currency or supported currency values for send-fee/money forms
      instead of requiring each client to hardcode currency choices.
- [x] Backend: move tenant display name to tenant identity/brand contract, initialized from tenant
      code during provisioning and editable by the tenant after provisioning.
- [x] Backend: keep `document.receipt.displayName` only as a migration/compatibility alias, or
      remove it after read/write paths use canonical tenant display name.
- [x] Web admin: move tenant default language/default locale edits to the tenant identity/runtime
      column contract, or hide them until that contract exists.
- [x] Web admin: reorder settings sections by operational usage: document/print, send options,
      business calendar, then defaults/allowed values.
- [x] Web admin: replace hardcoded language and currency option lists with backend-provided
      tenant/platform allowed values.
- [x] Backend: fix receipt print read view timezone source to use the tenant timezone column,
      not a non-contract top-level JSON `timezone`.
- [x] Backend: decide whether receipt print locale stays hardcoded, uses tenant default locale,
      or uses user/runtime locale.
- [x] Backend: prove or wire consumers for `showSellerName` and `showPotentialPayout`, or hide
      them from active tenant-admin UI.
- [x] Backend: add or adjust section-scoped update commands so web saves cannot drop hidden
      required fields.
- [x] Backend: add a small tenant settings readiness service with stable per-section reason codes.
- [x] Backend: wire the V1 settings readiness service into the setup card, including
      per-section missing reasons.
- [x] Backend: encode required/optional/default semantics for identity, defaults, print, send
      options, and business calendar readiness.
- [x] Backend: ensure receipt header/footer are nullable optional text and do not block readiness.
- [x] Backend: ensure print structural options either have explicit server defaults or return
      validation/readiness errors when missing.
- [x] Backend: remove/hide `holidaySalesAllowed` from active V1 behavior and replace it with
      explicit tenant holiday rules.
- [x] Backend: add tenant holiday rule model for recurring month/day holidays without a year,
      plus labels and open/closed intent.
- [x] Backend: add Haiti V0 holiday templates for common selectable holidays, without auto-closing
      tenants unless the tenant selects them.
- [x] Backend: define and test calendar precedence: one-off override, recurring holiday,
      closed weekday, then `defaultOpen`.
- [x] Backend/docs: replace legacy outlet calendar wording with tenant/seller-terminal language.
- [x] Backend: remove or quarantine unreachable legacy outlet-level calendar reader paths from
      active contracts while preserving database compatibility.
- [x] Backend: resolve duplicate `TenantLocaleApi` component ownership if both implementations
      are active in the same Spring context.
- [x] Backend: add regression tests for locale policy and receipt updates preserving
      `supportedLanguages`, fallback chain, and `defaultTemplateKey`.
- [x] Web admin: align form options with backend canonical values and remove obsolete/legacy
      outlet fields from visible UI.
- [x] Web admin: add a real tenant calendar tab for `rules.businessCalendar` and link or embed
      tenant-level business-day exceptions from `/admin/business-days`.
- [x] Web admin: add tenant holiday picker with Haiti V0 holiday suggestions and custom recurring
      no-year holidays; keep one-off dated exceptions separate.
- [x] Web admin: keep `holidaySalesAllowed` out of editable UI.
- [x] Web admin: update settings save flow to use section-scoped updates or preserve hidden
      canonical fields explicitly.
- [x] Web setup: ensure the settings card uses backend readiness and routes to the exact tab or
      section that fixes the readiness gap.
- [x] Web setup/navigation: add return links from setup destination/detail pages and keep
      parent sidenav items active for setup-owned routes plus tenant/draw/draw-result details.
- [x] Web dashboard: persist dashboard-returned tenant settings into the global runtime settings
      store for downstream tenant-scoped forms.
- [x] Web platform: update tenant config summary to display only canonical active fields.
- [x] Mobile: document settings ownership in `mobile-runtime-settings-v1` and keep read-only
      runtime consumption/fallback tests.
- [x] Run focused backend, web, and mobile validation for touched slices.
