# Design Notes

## Current Findings

- Tenant creation deep-merges `tenantconfig/*.json` into the tenant JSONB config, then
  validates the merged document in `TenantConfigValidator`.
- After tenant creation, those classpath defaults are no longer read as runtime fallback. The
  persisted tenant JSONB config is the source of truth; changing `tenantconfig/*.json` affects new
  tenants or an explicit migration, not existing tenant reads.
- The active top-level JSON sections are `rules`, `document`, `communication`, and `locale`.
- Admin web reads `GET /admin/tenant-config` and writes `PUT /admin/tenant-config/internal-settings`.
- The write endpoint lives in `features.tenantadmin.tenant.AdminTenantConfigController`, but
  it delegates validation and persistence to `platform.tenant.TenantConfigApi`.
- Admin web edits only `locale` and `document.receipt`; communication and rules are read-only
  or placeholder tabs.
- Hidden-but-required fields exist: `locale.supportedLanguages` and
  `document.receipt.defaultTemplateKey` are required by backend validation when those sections
  are present.
- Existing OpenSpec context `86-draw-calendar-rules.md` says `tenant.default_language` and
  `tenant.default_locale` columns are the canonical source for hot paths. JSON locale may hold
  extended options such as supported languages and fallback chain.
- The setup settings card is READY when receipt display name is customized away from
  `CHEZ Toto`; this is the wrong level because display name is tenant/brand identity.
- Tenant display name is provisioned from tenant code and can be changed later by the tenant.
- Mobile already consumes runtime bootstrap/state settings and should remain read-only for
  tenant configuration.
- `rules.businessCalendar.closedWeekdays` is used by
  `TenantBusinessCalendarApiAdapter.resolveFromTenantRules`.
- `rules.businessCalendar.holidaySalesAllowed` has been removed from the active JSON contract. It
  was too coarse because tenants need to choose actual holiday dates/rules.
- Legacy outlet calendar wording is obsolete. The active operational actor is `SellerTerminal`;
  any non-tenant business-calendar scope must be seller-terminal-owned in `core.sellerterminal`.
- Tenant-level business-day exceptions are implemented at `/admin/business-days`; the admin web
  page exists separately from tenant settings.
- Provider/result-slot calendar is separate and already platform-owned through
  `/platform/result-slots/{resultSlotId}/calendar`; it is not tenant settings. Super admins
  manage it, tenant admins may consult it read-only from catalog/provider calendar screens.
- Tenant rows carry first-class columns for runtime defaults (`timezone`, `currency`,
  `default_language`, `default_locale`) and a JSONB `config` for editable policy blocks. Those
  are not interchangeable.

## Why Each Block Exists

Before adding, removing, or exposing a tenant setting, the contract must identify the runtime
consumer. A field without a consumer is a migration/compatibility field, not an active UI field.

| Block | Why it exists | Current consumers / gaps |
|---|---|---|
| Tenant columns: `timezone`, `currency`, `default_language`, `default_locale` | Canonical runtime defaults available from tenant identity/registry without parsing the editable JSON policy document. | Runtime bootstrap, tenant runtime view, and locale hot paths should use these values. |
| JSON `locale` | Extended locale policy: supported languages and fallback chain. Supported languages should feed tenant-scoped language pickers such as admin creation, seller-terminal setup, print/send language choices, and any flow that must choose a tenant-supported language. | JSON no longer carries duplicate `defaultLanguage/defaultLocale`; tenant columns are canonical. Print receipt SQL currently hardcodes `fr`, so print locale is not yet wired. |
| Tenant currency / supported currencies | Tenant money defaults and allowed currency choices for tenant-scoped money inputs. | Current tenant has canonical `currency`; per-channel send fees also store a currency. Any fee/cost form should choose from tenant-supported/canonical currency values instead of hardcoding. |
| Canonical display name | Tenant/brand display name used across print, reports, setup readiness, and summaries. Provisioned from tenant code, then tenant-editable. | Stored in `tenant.display_name`; receipt JSON no longer owns display identity. |
| JSON `document.receipt.headerMessage/footerMessage` | Non-canonical free text shown on printed ticket receipts, possibly scoped to receipt template/config. | Receipt print header view reads these JSON fields and the ticket receipt assembler/formatter renders them. No report consumer found in the current scan. |
| JSON `document.receipt.defaultTemplateKey/defaultPaperSize/showQrCode` | Controls receipt print document template, paper size, and QR asset rendering. | `TicketPrintDocumentMapper` consumes these fields. Paper size values currently drift between backend defaults and web options. |
| JSON `communication.buyerTicketDelivery` | Tenant policy for buyer ticket delivery fees by channel and cost bearer. | `TenantConfigCommunicationFeePolicy` actively reads it for SMS/WhatsApp/email fee decisions. |
| JSON `rules.businessCalendar.defaultOpen/closedWeekdays` | Tenant-level weekly selling availability when no date-specific or holiday rule exists. | `TenantBusinessCalendarApiAdapter` uses `defaultOpen` and `closedWeekdays`. `defaultOpen` means "is the tenant open on days not otherwise closed by weekday/holiday/exception?" |
| Tenant holiday calendar | Tenant-selected recurring holidays without year, plus optional imported common holidays such as Independence Day or December 25. | Not implemented yet; should replace `holidaySalesAllowed` as active behavior. Rules should support month/day recurrence and labels. |
| `business_day_override` tenant rows | One-off tenant-level open/closed exceptions for a concrete date/year. | `/admin/business-days` lists/upserts/deletes tenant-wide exceptions. Existing legacy nullable `outlet_id` is not an active V1 scope. |
| `result_slot_calendar_override` | Provider/result-slot no-draw calendar. | Draw generation and opening use it to skip/cancel provider unavailable dates. It is platform-owned, not tenant settings. Super admins write it; tenant admins see it read-only. |

## Columns vs JSON

- `tenant.timezone` is the source of truth for local time calculations in runtime and tenant
  identity. A top-level JSON `timezone` field is not part of the current tenant config contract.
- `tenant.currency` is the source of truth for tenant monetary defaults in runtime. Per-channel
  delivery fees still carry their own amount/currency because they are fee policy entries.
- `tenant.default_language` and `tenant.default_locale` are canonical runtime columns. The mapper
  no longer syncs them from JSON.
- JSON `locale.supportedLanguages` and `locale.fallbackLanguage` are richer editable locale policy
  fields; JSON `locale.defaultLanguage/defaultLocale` is removed from the active contract.
- Print SQL uses tenant columns for display name and timezone.

## Config Usage Inventory

| Config | Current use | V1 decision |
|---|---|---|
| `locale.supportedLanguages` | Backend validation/runtime, hidden from admin form | Active allowed-values policy; use for admin/seller-terminal/user language pickers instead of hardcoded lists |
| `locale.fallbackLanguage` | Web edit, backend validation | Active fallback policy, not the same thing as default language |
| Tenant currency / supported currency values | Tenant identity/runtime and communication fee inputs | Use as allowed-values policy for money fields; do not hardcode fee currency choices |
| `document.receipt.headerMessage/footerMessage` | Web edit, receipt print branding | Non-canonical document-print text; active for tickets, report usage not proven |
| `document.receipt.defaultPaperSize` | Web edit, backend validation, print document mapper | Active but values drift (`RECEIPT_80MM` vs `THERMAL_*`) |
| `document.receipt.showQrCode` | Web edit, print document mapper | Active |
| `document.receipt.defaultTemplateKey` | Backend validation, print document mapper, hidden from admin form | Preserve as canonical hidden field |
| `communication.buyerTicketDelivery.*` | Backend validation and communication fee policy | Active fee policy; decide which admin surface may edit it |
| `rules.businessCalendar.defaultOpen` | Backend validation and resolver fallback | Active tenant selling-calendar fallback: open on dates not closed by weekday, holiday, or exception |
| `rules.businessCalendar.closedWeekdays` | Backend validation and resolver | Active recurring weekly closure policy; empty list means no weekly closures |
| `rules.businessCalendar.holidaySalesAllowed` | Legacy JSON only | Removed from active V1 JSON; replace with explicit tenant holiday rules |
| Tenant holiday rules | Not implemented | Add as tenant calendar policy: recurring no-year month/day holidays and optional imported known holiday templates |
| `business_day_override` tenant rows | `/admin/business-days` list/upsert/delete | Active tenant exception calendar; surface from settings tab |
| `result_slot_calendar_override` | Platform catalog + draw generation/opening | Active provider no-draw calendar; keep out of tenant settings |

## Configuration Readiness

Tenant settings readiness is not a blanket non-null check. Each section must declare which fields
are required, which fields are optional nullable/free text, and which fields have safe defaults.

| Section | Required for configured | Nullable / optional | Default behavior |
|---|---|---|---|
| Tenant identity | Canonical display name exists and has been reviewed or accepted by the tenant; timezone, currency, default language, and default locale columns are valid. | Address/details not required by this settings card unless a separate setup step owns them. | Display name is initialized from tenant code at provisioning, then tenant-editable. |
| Defaults / allowed values | `supportedLanguages` is non-empty, fallback language is in supported languages, currency values are valid. | Extra supported values may be omitted if the tenant only supports the canonical default. | Defaults come from tenant columns and backend seed/migration policy, not hardcoded client lists. |
| Document/print | Receipt enabled state, template key, paper size, and structural print options have valid values. | Header/footer are optional free text; empty/null means no custom message. Visibility toggles are optional only if the backend has explicit defaults. | Missing optional header/footer renders as absent. Missing structural print options use server defaults or fail validation if no safe default exists. |
| Send options / fees | Each supported send channel has an explicit enabled/disabled state; enabled fee entries have amount, currency, and payer. | Disabled channels may omit fee amount/payer if backend normalizes them. | Disabled is the safe default for channels not intentionally configured. Currency choices come from tenant/platform allowed values. |
| Business calendar | Default open fallback and recurring closed weekdays are valid. If holidays are enabled, selected Haiti V0 holiday rules are valid. | One-off business-day exceptions are optional; no rows means no date-specific override. Tenant holiday rules are optional and can be selected from Haiti V0 templates or added as custom recurring rules. | Empty `closedWeekdays` means no recurring weekly closure. Empty holiday list means no tenant holiday closure. Missing exceptions fall back to holiday/weekly/default policy. |
| Provider calendar | Not part of tenant settings readiness. | N/A | Provider/result-slot no-draw days are platform-owned. |

## Tenant Holiday Calendar Model

- `defaultOpen` is a fallback, not a holiday switch. It answers: when no date-specific override,
  holiday rule, or closed weekday matches, is the tenant open?
- Weekly closures use `closedWeekdays` such as Sunday or Monday.
- V0 supports Haiti only. Common Haiti holidays should be offered as selectable templates, for
  example Independence Day and December 25. The tenant chooses which ones apply; we should not
  silently close every tenant for a hardcoded list.
- Custom recurring holidays are month/day rules without a year, for dates that repeat every year.
- One-off closures/openings are concrete dates with a year and remain `business_day_override`.
- Precedence should be explicit: one-off business-day override wins, then recurring holiday, then
  closed weekday, then `defaultOpen`.
- `holidaySalesAllowed` should not be an editable V1 field because it cannot represent which
  holidays exist, partial adoption, or tenant-specific choices.

## Ownership

`features.tenantadmin` is the right place for the tenant-admin-facing controller because it is
a BFF surface under `/admin/*`. It should stay thin: current tenant resolution, permission
checks, request/response shape, and delegation only.

`platform.tenant` remains the owner of the JSON schema, validation, merge/persistence, typed
sub-config accessors, and runtime-safe projection. Any new validation rule or canonical field
belongs there.

## Target Contract

- Backend exposes typed section DTOs or patch commands that preserve unknown/future-safe
  canonical fields.
- Backend read APIs expose the persisted tenant settings document, or one persisted section.
  Defaults are materialized during provisioning/migration; reads and updates must not re-merge the
  current classpath defaults because that would make old tenant behavior drift silently.
- Backend update APIs patch the persisted JSON document and validate the resulting persisted
  document. Section saves preserve sibling slices by patching the stored document, not by rebuilding
  from defaults.
- Tenant default language/default locale updates use the tenant identity/runtime column contract.
  JSON locale must not become an independent source of truth for those values.
- Supported languages and tenant currency values are allowed-value policies. Admin, seller-terminal,
  send, print, and user-language forms must load them from tenant/platform contracts instead of
  hardcoding `fr/en/ht` or currency choices locally.
- Tenant display name is canonical tenant/brand identity: it is initialized from tenant code at
  provisioning and can be changed by the tenant. Receipt header/footer remain non-canonical
  document-print text unless another consumer is proven.
- Admin web forms submit section-scoped updates or a server-merged patch, avoiding accidental
  deletion of fields not present in the form.
- Setup readiness derives from backend section validation/completeness and canonical tenant display
  name. It must not depend on `document.receipt.displayName` as the source of truth.
- "Configured" means the backend can return a named readiness status per section. It must not
  require optional texts such as receipt header/footer, and it must distinguish defaults that were
  accepted from values that are missing and unsafe.
- A small backend readiness service in `platform.tenant` should evaluate runtime tenant identity
  plus internal settings and return stable section status/reason codes. The BFF/setup surface should
  consume those codes instead of duplicating field checks.
- Admin web settings should be ordered by operational usage, not by low-level defaults:
  document/print, send options and fees, business calendar, then defaults/allowed values.
- Admin web settings include a real calendar tab for tenant business rules: default open,
  recurring closed weekdays, and a tenant-level exceptions entry point backed by
  `/admin/business-days`. Holiday policy must stay hidden/copy-only until a holiday source exists.
- Seller-terminal-specific planned closures are not part of V1 because the current model has no
  active seller-terminal business-day rows. Seller-terminal operational availability remains owned
  by seller-terminal status/eligibility flows.
- Legacy outlet labels must not appear in tenant-admin settings; user-facing copy should say
  tenant or seller-terminal.
- Platform web summary displays canonical active fields only and hides deprecated/placeholder
  fields unless explicitly useful to platform admins.
- Mobile consumes only runtime bootstrap/state settings. It treats missing or obsolete fields
  as safe fallbacks and never confirms business behavior offline based on local settings alone.

## Future Tenant Config Candidates

These are architectural candidates, not automatic readiness blockers. Each one needs a proven
consumer and should either be optional, warning-only, or represented by its own setup card.

| Candidate | Likely owner | Readiness posture |
|---|---|---|
| Brand assets: logo, colors, receipt/report visual identity | Tenant identity/brand or tenant theme | Optional unless a surface explicitly requires it; theme already has its own active assignment check. |
| Public/support contacts: phone, email, WhatsApp, website | Tenant profile/contact | Optional for launch unless customer support workflows require it. |
| Report branding: report title/footer/legal text | Reporting/document domain | Do not reuse receipt header/footer unless reports actually consume the same copy. |
| Notification preferences: admin alerts, seller-terminal announcements | Platform notification | Optional preferences; missing config falls back to default notification policy. |
| Seller-terminal defaults: default language, commission, receipt behavior | Core sellerterminal / tenant defaults | Not tenant settings readiness unless needed to create the first seller-terminal. |
| Sales limits, odds, exposure controls, promotion defaults | Core pricing/limit/promotion domains | Separate operational setup cards; do not hide under generic tenant config readiness. |
| Payout policy: payout proof, approval thresholds, payment methods | Core sales/payout domain | Separate operational readiness if payouts cannot work safely without it. |
| Data/export preferences: report timezone/locale, CSV format | Reporting/export | Optional defaults; should use tenant runtime columns unless a report-specific override is proven. |
| Compliance text: legal notices, terms, responsible gaming copy | Document/public content domain | Optional until a legal/regulatory requirement exists. |

## Suspected Cleanup Items

- Normalize paper size values between defaults (`RECEIPT_80MM`) and admin web options
  (`THERMAL_58`, `THERMAL_80`, `A4`).
- Decide whether `communication.buyerTicketDelivery` is platform-only or tenant-admin editable.
- Remove or hide admin web fields that are not applied by document generation/runtime.
- Move tenant display name to tenant identity/brand profile: provision from code, tenant-editable,
  with receipt config kept only as a compatibility alias if needed.
- Replace hardcoded language/currency option lists in tenant-scoped forms with backend-provided
  allowed values.
- Fix print SQL timezone source to use the tenant column, and decide whether print locale should
  use tenant/user locale instead of hardcoded `fr`.
- Remove or quarantine legacy outlet calendar readers/columns from active contracts while keeping
  database compatibility where required.
