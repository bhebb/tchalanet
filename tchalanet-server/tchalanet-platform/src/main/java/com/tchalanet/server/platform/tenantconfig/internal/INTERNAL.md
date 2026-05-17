# TenantConfig Internal Notes

## Scope
Internal notes for `platform.tenantconfig.internal`.

## Internal settings JSON lifecycle
- Source defaults: `classpath*:tenantconfig/*.json`
- Merge strategy: deep merge object fields, concatenate arrays, deterministic by filename sort
- Persistence: stored in `tenant.config` (`jsonb`)
- Read path for required operations: `TenantConfigService -> TenantPersistenceAdapter.getRequiredByIdActive -> TenantJpaRepository.getRequiredByIdActive`

## Validation (current)
- Applied on create defaults and on settings update
- Current validated block: `communication.buyerTicketDelivery`
- Channel checks:
  - `amount >= 0`
  - `currency` required
  - `paidBy` required and in `{BUYER, TENANT}`
- Current validated block: `document.receipt`
  - `defaultTemplateKey` required
  - `defaultPaperSize` required

## Typed access endpoint/service
- `getTenantCommunicationConfig(GetTenantByIdRequest)` returns typed `TenantInternalCommunicationConfig`
- `getTenantDocumentConfig(GetTenantByIdRequest)` returns typed `TenantInternalDocumentConfig`
- If `config` is null/empty, returns `null`

## Follow-up
- Replace partial validation with schema-based validation when schema contract is finalized.

