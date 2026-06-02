# Design: tenantgame-runtime-v1

## Boundary

```text
catalog.game
→ global canonical game definition

platform.tenantgame
→ tenant-specific game offer/configuration and runtime view

pricing_odds
→ tenant odds/pricing

limit_policy
→ tenant limits/exposure

core.sales
→ sell-time validation using runtime views and domain gates
```

## catalog.game

Global game catalog fields:

```text
code
name
category
combination
minDigits
maxDigits
description
active
sortOrder
```

Global game writes are platform-only. SUPER_ADMIN can create/update/deactivate games through platform catalog endpoints. TENANT_ADMIN cannot create or update global games.

Structural fields are protected:

```text
code
category
combination
minDigits
maxDigits
```

If a game is referenced by tenant configuration, pricing, draws, tickets, sales, settlement, or payout, structural changes must be blocked or handled through explicit migration/versioning.

## Target package structure

```text
platform/tenantgame/
  api/
    TenantGameApi.java
    model/request/
    model/result/
    model/view/
    model/event/

  internal/
    adapter/TenantGameApiAdapter.java
    model/TenantGame.java
    service/
      TenantGameAdminService.java
      TenantGameRuntimeService.java
      TenantGameCatalogProjectionService.java
      TenantGameConfigValidator.java
      TenantGameProvisioningService.java
    persistence/
      entity/TenantGameJpaEntity.java
      repository/
      adapter/
      mapper/
    web/admin/TenantGameAdminController.java
    web/runtime/TenantGameRuntimeController.java
```

## tenant_game table V1

Recommended columns:

```text
id
tenant_id
game_id
game_code
enabled
visible_in_pos
display_name
display_order
min_stake
max_stake
availability_enabled
availability_days
start_local_time
end_local_time
created_at
updated_at
deleted_at
```

Recommended entity:

```java
@Entity
@Table(name = "tenant_game")
@Getter
@Setter
public class TenantGameJpaEntity extends BaseTenantEntity {

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "game_code", nullable = false, length = 32)
  private String gameCode;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "visible_in_pos", nullable = false)
  private boolean visibleInPos = true;

  @Column(name = "display_name", length = 128)
  private String displayName;

  @Column(name = "display_order", nullable = false)
  private int displayOrder = 0;

  @Column(name = "min_stake", precision = 12, scale = 2)
  private BigDecimal minStake;

  @Column(name = "max_stake", precision = 12, scale = 2)
  private BigDecimal maxStake;

  @Column(name = "availability_enabled", nullable = false)
  private boolean availabilityEnabled = false;

  @Column(name = "availability_days", length = 64)
  private String availabilityDays;

  @Column(name = "start_local_time")
  private LocalTime startLocalTime;

  @Column(name = "end_local_time")
  private LocalTime endLocalTime;
}
```

No `tenant_game_availability` table in V1.

A dedicated availability table is deferred until the product needs multiple windows, holiday exceptions, effective dating, reporting, or SQL-indexed scheduling queries.

## Column consumers

Every column must have a V1 consumer.

| Column | Written by | Read by | Context |
|---|---|---|---|
| `game_id` | provisioning/admin enable | tenantgame | catalog link |
| `game_code` | provisioning/admin enable | sales/events/POS/E2E | functional key |
| `enabled` | tenant admin | POS/sales | allow/block game |
| `visible_in_pos` | tenant admin | POS | hide prepared/unready game |
| `display_name` | tenant admin | POS/receipt/admin | local label |
| `display_order` | tenant admin | POS/admin | sorting |
| `min_stake` | tenant admin | POS/sales | simple stake bound |
| `max_stake` | tenant admin | POS/sales | simple stake bound |
| `availability_enabled` | tenant admin | sales/POS | activate schedule restriction |
| `availability_days` | tenant admin | sales/POS | allowed days |
| `start_local_time` | tenant admin | sales/POS | sale window start |
| `end_local_time` | tenant admin | sales/POS | sale window end |

Not allowed in tenant_game: global combination, minDigits/maxDigits, payout algorithm, pricing odds, exposure limits, promotion rules, or settlement rules.

## Admin endpoints

```text
GET    /admin/games
GET    /admin/games/catalog
POST   /admin/games/{gameCode}/enable
POST   /admin/games/{gameCode}/disable
PATCH  /admin/games/{gameCode}/settings
```

Replace `/tenant/games`, `PUT /{gameCode}/policy`, and `UpdateTenantGamePolicyRequest` with `/admin/games`, `PATCH /{gameCode}/settings`, and `UpdateTenantGameSettingsRequest`.

## Runtime endpoints / bootstrap

Runtime may be provided through connected bootstrap or an explicit endpoint:

```text
GET /tenant/games/runtime
```

POS and `core.sales` consume `TenantGameRuntimeView`, not admin views and not raw tenant_game rows.

## Views

### TenantGameAdminView

```text
gameCode
catalogName
displayName
category
enabled
visibleInPos
displayOrder
minStake
maxStake
availability
pricingConfigured
readyForSale
```

### TenantGameCatalogItemView

For tenant admin catalog tab:

```text
gameCode
name
category
catalogActive
enabledForTenant
canEnable
disabledReason
```

### TenantGameRuntimeView

```text
gameCode
label
category
saleEnabled
visibleInPos
minStake
maxStake
availability
```

## Permissions

Add:

```text
catalog.game.read
catalog.game.manage

tenantgame.read
tenantgame.manage
```

SUPER_ADMIN gets catalog.game read/manage and tenantgame read/manage. TENANT_ADMIN gets tenantgame read/manage. OPERATOR may get tenantgame.read for operational dashboard. CASHIER receives runtime games through bootstrap/runtime and does not need tenantgame.manage.

## Entitlements / quotas

Feature keys:

```text
tenantgame.management
tenantgame.availability
tenantgame.advanced_settings
tenantgame.premium_games
```

Plan limits:

```text
tenant_games.max_enabled
```

Usage example:

```java
@PostMapping("/{gameCode}/enable")
@PreAuthorize("hasPermission('tenantgame.manage')")
@RequiredFeature(FeatureKeys.TENANTGAME_MANAGEMENT)
@RequiredQuota(limit = PlanLimitKeys.TENANT_GAMES_MAX, usage = UsageKeys.TENANT_GAMES_ENABLED)
```

If feature depends on `gameCode`, check dynamically in `TenantGameAdminService`:

```java
entitlementApi.requireFeature(tenantId, featureRequiredForGame(gameCode));
```

## Controller validation

Controllers use `/admin/games`, `ApiResponse<T>`, `@CurrentContext`, no tenantId in tenant admin request, `@PreAuthorize("hasPermission('tenantgame.read/manage')")`, Bean Validation, service validators, audit on writes, and notices only after successful service calls.

## TenantGameConfigValidator

Validates game existence through `GameCatalog`, global active status when enabling, displayName length, displayOrder bounds, positive stake values, minStake <= maxStake, valid availability days/times, and no attempt to update global game fields.

## Sell-time use

When cashier sells:

```text
1. sales receives gameCode
2. sales gets TenantGameRuntimeView
3. enabled/saleEnabled must be true
4. availability passes if enabled
5. min/max stake passes
6. draw cutoff passes
7. pricing odds configured
8. limits OK
9. promotions applied if applicable
```

`tenantgame` is an availability/configuration gate, not the complete rules engine.
