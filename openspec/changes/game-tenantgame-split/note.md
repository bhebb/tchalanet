# game-tenantgame-split — Notes & Handoff

> **Change-id**: game-tenantgame-split  
> **Status**: APPROVED  
> **Scope**: `catalog/game` + `core/tenantgame`

## What changed

We split “global game definitions” from “tenant game configuration”:

- `catalog/game` → reference data only (read-mostly, cacheable, no events)
- `core/tenantgame` → tenant-scoped enablement + policies + effective resolution

## What is correct now

- Core validates game existence via `GameCatalog` (API only)
- No catalog emits events or manages tenant lifecycle
- Tenant policies live only in `core/tenantgame` (RLS)

## What to refactor

If an existing `game` module mixes:

- enable/disable logic
- commissions/limits
- seller autonomy validation rules
- tenant-scoped rows
  …then move that logic to `core/tenantgame` and leave only global definitions in `catalog/game`.

## Copilot do/don’t

### DO

- Use `GameCatalog.findByCode()` for validation
- Store tenant policies in `tenant_game` (jsonb or explicit)
- Implement idempotent commands
- Publish events after commit (if needed)

### DO NOT

- Put tenant policies in `catalog/game`
- Emit domain events from catalog
- Access `catalog/game/internal/**` from core

## Checklist

- [ ] catalog structure conforms to 75-catalog-rules
- [ ] core/tenantgame has commands + query + ports
- [ ] RLS enabled on `tenant_game`
- [ ] ArchUnit guards pass
- [ ] basic tests added (unit + integration)
