# Tasks: platform-tenant-v1

## Jackson 3 rule

- [x] Tous les champs JSON (`config`, `token_overrides`, `flags`, `settings`) utilisent **Jackson 3** (`tools.jackson.*`) — jamais `com.fasterxml.jackson.*`.
  - Imports : `tools.jackson.databind.JsonNode`, `tools.jackson.databind.node.ObjectNode`, etc.
  - Sérialisation : passer par `JsonUtils` (common) ou `JsonMapper` injecté.
  - Colonnes JSONB : `@JdbcTypeCode(SqlTypes.JSON)` + type Java `JsonNode` ou `Map<String, String>`.
  - Validator : `TenantConfigValidator` reçoit `JsonNode` et appelle `jsonUtils.treeToValue(node, TypedConfig.class)`.

## Schema / persistence

- [x] Confirm `tenant` table remains the single table (no schema changes needed - fresh DB strategy).
- [x] Move `TenantJpaEntity` to `platform.tenant.internal.persistence` (package rename done).
- [x] Move repository/adapter/mapper to `platform.tenant.internal.persistence`.
- [x] Audit `config` usage and document allowed keys or create explicit fields.
- [x] **Config initialization and validation**:
  - [x] Conserver `getTenantInternalSettings()` dans `TenantConfigService` — charge et merge les 4 JSON resources au create.
  - [x] Créer `TenantConfigValidator` (classe dédiée, appelée par `TenantConfigService`) avec validation pour les 4 sections :
    - [x] `validateRulesConfig()` — `rules.businessCalendar`: defaultOpen (boolean), closedWeekdays (array jours valides), holidaySalesAllowed (boolean).
    - [x] `validateLocaleConfig()` — locale: defaultLanguage non vide, supportedLanguages non vide, fallbackLanguage dans supportedLanguages.
    - [x] `validateCommunicationConfig()` — déplacé de `TenantConfigService` vers `TenantConfigValidator`.
    - [x] `validateDocumentConfig()` — déplacé de `TenantConfigService` vers `TenantConfigValidator`.
  - [ ] Évaluer `json-schema-validator` (v3.0.3, déjà dans `pom.xml`, pas encore utilisé) — décision: validation manuelle par section suffisante pour V1, json-schema-validator non nécessaire.
  - [x] `TenantConfigService.updateTenantInternalSettings()` appelle `TenantConfigValidator.validateAll()` avant persist.
  - [x] `TenantConfigService.createTenant()` appelle `TenantConfigValidator.validateAll()` après merge, avant persist.
  - [x] Note: `TenantProvisioningService` sera probablement le point d'entrée final — logique dans `TenantConfigService` extractible facilement.
  - [x] Note: `fees_config.json` contient les frais d'envoi de messages (SMS/WhatsApp/email — amount, currency, paidBy) — nom correct, ne pas renommer.
- [x] Fix legacy raw tenant registry queries to consistently filter `deleted_at IS NULL` (JdbcTenantRegistryReader uses `WHERE deleted_at IS NULL`).
- [x] Include `default_language` and `default_locale` in bootstrap lookup projections (`TenantRegistryView` updated, mapper updated).

## Resolver

- [x] Create `TenantRegistryReader` (interface, `platform.tenant.internal.resolver`).
- [x] Create `JdbcTenantRegistryReader` using `rawDataSource` (package-private implementation).
- [x] Create `TenantContextLookupService` (replaces `TenantConfigContextLookup`, no catalog.tenant dependency).
- [x] Document RLS bypass allowed paths (Javadoc on `TenantRegistryReader`).
- [x] Ensure reader is read-only (no write methods on interface or implementation).

## API / adapter

- [x] `TenantApiAdapter` created under `internal/adapter` (replaces `DefaultTenantConfigApi`).
- [x] `DefaultTenantConfigApi` deleted.
- [x] `TenantAdminView` and `TenantRuntimeView` added to `api/model/view/`.
- [x] `TchPageRequest` / `TchPage` already used (no Pageable in public API).

## Services/controllers

- [x] `TenantConfigValidator` created.
- [x] `TenantRuntimeController` added with `/public/tenant/runtime` and `/tenant/runtime`.
- [ ] Create `TenantAdminService`, `TenantProvisioningService`, `TenantRuntimeService`, `TenantReadinessService`, `TenantSettingsService` (service split — deferred to follow-up PR).
- [x] `/platform/tenants/**` controller exists (`TenantAdminController`).
- [x] Use `ApiResponse<T>` — done.
- [x] Permission annotations — existing controller uses `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`.

## Access-control / entitlement

- [ ] Add tenant permissions to permission registry (tenant.read, tenant.create, etc.) — deferred.
- [ ] Map SUPER_ADMIN and TENANT_ADMIN permissions — deferred.
- [ ] Return safe entitlement summary in connected bootstrap — deferred.

## Tests

- [x] `TenantConfigContextLookupTest` updated to use `TenantContextLookupService` + `TenantRegistryReader`.
- [ ] Tenant lookup before context resolution — integration test deferred.
- [ ] RLS bypass reader cannot write — by design (interface has no write methods).
- [ ] Tenant admin cannot update another tenant — deferred.
- [ ] Super admin can create tenant and first tenant admin — deferred.
- [x] Tenant runtime view does not expose internal config (`TenantRuntimeView` has no config field).
- [ ] **Config initialization and validation tests** — unit tests deferred (next PR).
