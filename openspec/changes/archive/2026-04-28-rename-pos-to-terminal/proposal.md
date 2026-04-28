# OpenSpec Change — `rename-pos-to-terminal`

> **Status**: Proposed
> **Order**: ⓵ — must be merged before `pos-v0-foundation` > **Type**: refactor / rename only (no behavior change)
> **Risk**: low — cosmetic + import paths

## Why

The package `core.pos` was named after the historical concept "Point of Sale" but actually models the **terminal entity** (PHYSICAL or VIRTUAL device used to place sales). The name causes confusion because:

- "POS" can mean the app, the terminal, or the cashier flow.
- The upcoming `features.pos` (BFF layer for the Flutter POS app) collides with `core.pos`.
- The class name `PosSession` in `core.session` mixes a tech word (POS) with a business concept (session of sales).

We rename for clarity, before adding any new feature on top.

## What Changes

- Package `core.pos.**` renamed to `core.terminal.**` (all sub-packages preserved).
- 8 classes in `core.session` renamed from `PosSession*` / `JpaPosSession*` to `SalesSession*` / `JpaSalesSession*`.
- Database table `pos_session` renamed to `sales_session` in-place (ALTER TABLE RENAME). All indexes, triggers, policies and constraints renamed accordingly.
- All imports across the codebase updated (search-and-replace).
- Test classes renamed to follow production class map.
- No field names, HTTP paths, typed IDs, or audit action names change.
- Existing migration files modified in-place (DB recreated from scratch — no additive migration file).

## Capabilities

### New Capabilities

- `terminal-core`: The `core.terminal` package becomes the canonical home for the terminal domain — replaces `core.pos`.

### Modified Capabilities

- `core.session`: Class names for session domain objects change (`PosSession*` → `SalesSession*`); no requirement-level behavior change.

## Impact

- **Java sources**: every file under `core.pos.**` moves; every reference to `PosSession*` renamed.
- **SQL migrations**: the migration file(s) creating `pos_session` are edited in-place; no new migration file added.
- **Tests**: test classes and fixture files renamed to match production classes.
- **OpenAPI**: DTO names exposed in `SalesSessionResponse` change (was `PosSessionResponse`).
- **Other domains**: any domain importing `core.pos.*` ports needs import updates — compile errors will catch any miss.
