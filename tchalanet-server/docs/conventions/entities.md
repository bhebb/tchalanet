# JPA Entities: which base class?

## Decision

Choose base class based on **data ownership** (tenant-scoping), not based on endpoint scope.

### Use `BaseTenantEntity` when:

- Row belongs to exactly one tenant (tenant-scoped)
- Table has `tenant_id NOT NULL`
- RLS policies apply
- Examples: `ticket`, `sale`, `payout`, `ledger_entry`, `draw` (tenant draw), `limit_policy` (tenant rules)

### Use `BaseEntity` when:

- Row is global/platform or shared reference data
- Table does NOT have `tenant_id`
- RLS does not apply (or uses a different model)
- Examples: `result_slot` (global), `draw_result` (global), provider cache tables, platform config

## Anti-patterns (forbidden)

- ❌ A tenant-scoped table without `tenant_id`
- ❌ A tenant-scoped entity extending `BaseEntity`
- ❌ Duplicating audit fields inside an entity that already extends base classes
