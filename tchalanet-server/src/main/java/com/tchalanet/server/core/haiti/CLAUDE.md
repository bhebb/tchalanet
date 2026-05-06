# Claude — core.haiti

Scope:

- Two sub-domains: Tchala (dream-to-number mapping) and Lottery (result normalization)
- Tenant-scoped: TchalaEntry approvals, submissions are per-tenant
- Global: HaitiProjectionService (result normalization, no persistence)

Out of scope:

- Provider HTTP fetching (drawresult / uslottery responsibility)
- Ticket result calculation (sales responsibility)
- Frontend/mobile

Rules:

- **Tchala sub-domain**: Entry imports, approvals, rejections, merges, deletes

  - Use typed IDs: TenantId, TchalaEntryId
  - Status flow: PENDING_APPROVAL → APPROVED → (soft-delete if REJECTED)
  - Events: Emit if domain rule violations detected, otherwise silent

- **Lottery sub-domain**: Pure normalization service

  - HaitiProjectionService: `project(resultSlot, date, externalResult) → HaitiResult`
  - No persistence; called during FetchExternalResultsWindowCommand
  - Stateless, testable, deterministic

- Never read external result directly; always go through drawresult.ExternalResultFetcher
- Typed IDs everywhere (domain/application/dtos)
- RLS filters by tenant_id on Tchala queries

Before editing:

- Inspect only files listed by the task
- Load HaitiProjectionService if projection logic changes
- Load core/sales if ticket matching changes
- Load core/drawresult if integration points change
- Load docs/conventions/typed_ids.md for ID wrapper rules

Output:

1. Files inspected
2. Files changed
3. Tests (if any)
4. Risks
5. Compact handoff
