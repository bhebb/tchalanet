## ADDED Requirements

### Requirement: TG1 — Enable/disable tenant game (commands)

System MUST support enabling/disabling games for a tenant:

- `EnableTenantGameCommand(tenantId, gameCode, policy?, idempotencyKey?)`
- `DisableTenantGameCommand(tenantId, gameCode, idempotencyKey?)`

Handlers MUST:

- validate game exists via `GameCatalog.findByCode(gameCode)`
- reject soft-deleted games
- persist tenant-scoped state (`tenant_game`)
- increment version
- publish `TenantGameUpdatedEvent` after commit (if used)

#### Scenario: enable valid game

- **GIVEN** game `HT_BOLET` is active in catalog
- **WHEN** `EnableTenantGameCommand(T, HT_BOLET)` is executed
- **THEN** `tenant_game` has an enabled entry for tenant T and version increments

#### Scenario: enable missing game rejected

- **GIVEN** game `UNKNOWN` does not exist
- **WHEN** `EnableTenantGameCommand(T, UNKNOWN)` is executed
- **THEN** the command is rejected, no write occurs, and no event is published

### Requirement: TG2 — Resolve effective tenant games (query)

System MUST expose:

- `ResolveTenantGamesQuery(tenantId)` returning effective enabled games
  (optionally enriched with `GameView` metadata via `GameCatalog`).

Query MUST be safe for bootstrap and side-effect free.

#### Scenario: Resolve enabled games

- **GIVEN** tenant T has enabled `HT_BOLET`
- **WHEN** `ResolveTenantGamesQuery(T)` is executed
- **THEN** it returns a list containing `HT_BOLET`

### Requirement: TG3 — Policies per tenant game

Tenant-scoped policies MUST be supported.
Policies MAY include:

- limits (per ticket/day/draw)
- commission rules
- validation requirements (autonomy integration)
- allowed channels (web/mobile/pos) if applicable

Policies MUST be stored tenant-scoped and versioned.

#### Scenario: Store policy

- **WHEN** enabling a game with specific commission rules
- **THEN** the rules are stored in the `tenant_game` record

### Requirement: TG4 — Idempotency

Commands MUST be idempotent.
Retries MUST NOT create duplicate final state/events.

#### Scenario: Retry enable command

- **GIVEN** `EnableTenantGameCommand` was successfully processed
- **WHEN** the same command is received again
- **THEN** no new state change occurs and the system remains consistent

### Requirement: TG5 — Boundaries

`core/tenantgame` MUST NOT depend on `catalog/game/internal/**` nor entities.
Validation MUST use `GameCatalog` API only.

#### Scenario: Boundary check

- **WHEN** `core/tenantgame` compiles
- **THEN** it has no imports from `catalog/game/internal`

### Requirement: TG6 — RLS

TenantGame persistence MUST be tenant-scoped and protected by RLS.

Expected table: `tenant_game`:

- `tenant_id`
- `game_code` (or game_id internally)
- `enabled`
- `policy_json`
- `version`
- audit columns

#### Scenario: RLS enforcement

- **GIVEN** RLS is enabled on `tenant_game`
- **WHEN** a query is executed for tenant T
- **THEN** it only returns rows belonging to tenant T
