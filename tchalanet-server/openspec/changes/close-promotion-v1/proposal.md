# OpenSpec Change — close-promotion-v1

## Status

Proposed

## Goal

Clôturer le cadrage Promotion V1 et lister les tâches restantes pour rendre le module exploitable sans créer un cycle parallèle à Sales, Settlement ou Payout.

La décision centrale reste :

```text
Promotion configure et décide.
Sales matérialise sur TicketLine / MoneyBreakdown.
Settlement utilise les snapshots.
Payout paie le résultat settlé.
Ledger / stats consomment les events.
```

## Why

Promotion a été créée pour éviter de disperser les règles promotionnelles dans Sales, Settlement, Payout ou le print.

Sans module Promotion :
- Sales finirait avec des `if Maryaj gratuit`, `if SMS offert`, `if odds boostées`.
- Settlement risquerait de réévaluer des règles passées.
- Payout pourrait dépendre d’une configuration modifiée après la vente.
- Ledger/stats auraient du mal à distinguer ventes payées, lignes promotionnelles et coûts promo.
- Les tenants ne pourraient pas préparer des promotions en draft avant activation.

Promotion V1 n’est pas un moteur de règles complet. Elle pose une base extensible pour trois effets concrets :
- `FREE_GAME_LINE`
- `BOOST_ODDS`
- `WAIVE_CHARGE`

## Scope

### In scope

- Stabiliser les packages `lifecycle`, `rule`, `applied`.
- Finaliser state machine campagne.
- Finaliser rule CRUD : metadata, eligibility typée, effects typés.
- Retourner `PromotionCampaignView` complet avec rules, eligibility et effects.
- Valider les campagnes avant activation.
- Valider les rules/effects/eligibility V1.
- Créer runtime read model/cache pour campagnes ACTIVE.
- Définir `ResolvePromotionDecision` / `ApplyPromotionDecision`.
- Préparer intégration Sales : `TicketLine`, `MoneyBreakdown`, preview, print.
- Définir impacts Settlement, Payout, Ledger, Stats.

### Out of scope V1

- Moteur de règles complet.
- Quotas complexes / “100 premiers” réellement appliqués.
- Commission agent.
- Bonus payout complexe.
- Multi-stacking avancé.
- Zones/agents profonds.
- Promotion listener qui modifie Sales après coup.
- Recalcul promotionnel dans Settlement/Payout.

## Package boundaries

```text
core.promotion
  api
    command
    query
    model
    event

  internal
    application
      lifecycle
      rule
      applied

    domain
      lifecycle
      rule
      applied

    infra
      persistence
        lifecycle
        rule
        applied
      cache
      web
        admin
        runtime
```

## Design decisions

### Campaign lifecycle

Status values:

```java
DRAFT, ACTIVE, PAUSED, INACTIVE, ARCHIVED
```

Official transitions:

```text
DRAFT    -> ACTIVE | INACTIVE | ARCHIVED
INACTIVE -> ACTIVE | ARCHIVED
ACTIVE   -> PAUSED
PAUSED   -> ACTIVE | INACTIVE | ARCHIVED
ARCHIVED -> none
```

`ACTIVE -> ARCHIVED` direct is forbidden. Pause first.

### Rule storage

Rule has typed scalar eligibility:

```text
rule_key
priority
min_paid_total
before_local_time
```

`PAID_LINE_COUNT` eligibility lives in `promotion_rule_eligibility_line`.
Effects live in `promotion_rule_effect`.

Rule lifecycle and validity window are inherited from the campaign.

```text
promotion_rule_effect
  effect_type = FREE_GAME_LINE | BOOST_ODDS | WAIVE_CHARGE
  game_code
  payout_base_amount
  quantity
  odds_override
  charge_type
```

### Cache

Cache names:

```text
core.promotion.runtime.active       key = tenantId
core.promotion.campaign.by_id       key = tenantId:campaignId
core.promotion.campaign.admin_list  key = tenantId:pageNumber:pageSize:sort
```

Eviction:

```text
runtime.active       -> evict tenant
campaign.by_id       -> evict tenant:campaignId
campaign.admin_list  -> clear whole cache
```

Eviction happens after commit only through `PromotionCacheEvictorPort`.

## Risks

- Runtime evaluation can slow Sales if it loads/parses DB every sale.
- Returning stale admin detail if cache key omits campaignId.
- Returning stale list pages if cache key omits page/size/sort.
- Bypassing state machine if adapter directly sets status.
- Settlement/Payout bugs if they re-evaluate Promotion instead of using snapshots.
- Misplaced ownership if applied snapshots are created from promotion listeners instead of Sales.

## Definition of Done

- Campaign state machine implemented and tested.
- Rule add/update/delete implemented.
- Eligibility/effects replace implemented.
- `PromotionCampaignView` returns rules with eligibility/effects.
- Activation validates campaign and rules.
- Runtime active cache implemented.
- Sales no-op applier integrated.
- TicketLine schema supports promotion snapshots.
- Settlement reads snapshots only.
- Payout pays settled amount only.
- Events expose promotion snapshot fields where useful.
