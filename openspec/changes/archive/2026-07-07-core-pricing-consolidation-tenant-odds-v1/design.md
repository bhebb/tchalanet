# Design: core-pricing-consolidation-tenant-odds-v1

## Décisions figées

- **D1 — Clé pricing** : `pricing_odds` keyé par `pricing_variant_code`. Unicité fonctionnelle
  `(tenant_id, game_code, pricing_variant_code)`. `bet_type` + `commercial_option_code` gardés
  **descriptifs** (grouping + affichage admin), pas la clé.
- **D2 — Ownership** : `core.pricing` = owner logique du pricing tenant + overrides seller-terminal.
  L'ancien `catalog.pricing` est retiré du runtime cible. **Java d'abord, pas de rename SQL au 1er slice.**
- **D3 — Pas de référentiel pricing plateforme V0** : les odds ne sont pas un référentiel global
  Tchalanet. Elles sont runtime tenant, puis éventuellement override seller-terminal. `/platform/pricing`
  est retiré du parcours cible.
- **Terme canonique** : `PricingVariantCode` (remplace/absorbe `SettlementVariant` comme clé d'odds).

## 1. Ownership cible

```text
catalog.game    = structure déclarative : jeux, betTypes, BetOption possibles, labels, contraintes.
core.pricing    = PricingVariantCode, odds tenant par défaut, overrides seller-terminal, résolution.
core.sales      = préparation ticket, résolution variante(s), snapshot odds, settlement.
```

### Inventaire `catalog.pricing` à migrer

| Classe | Rôle actuel | Cible |
| --- | --- | --- |
| `PricingOddsEntity`, `PricingOddsJpaRepository` | Persistance `pricing_odds` tenant default | Supprimé du catalog ; persistance déplacée sous `core.pricing.internal.infra.persistence` |
| `PricingEntityMapper`, `PricingWebMapper` | Mapping entity/API/web | Supprimé ; remplacé par mapper core + DTO tenant/admin |
| `PricingCatalog`, `PricingCatalogImpl` | Read API tenant odds + stats | Supprimé ; le read runtime passe par `core.pricing` |
| `PricingAdminService`, `PricingAdminController`, web models create/update/view | CRUD platform/admin sur odds | Supprimé ; pas de référentiel pricing plateforme V0 |
| `PricingProvisioningApi`, `PricingProvisioningService` | Seed tenant default odds au provisioning | Migré vers `core.pricing.api.command.EnsureDefaultHaitiLotteryOddsCommand` + handler |
| `PricingCacheNames` | Cache tenant odds/stats | Migré côté core pricing (`core:pricing:tenant_odds_*`) |

Consommateurs basculés:

- `core.pricing.internal.application.query.ResolveSellerTerminalOddsQueryHandler` lit les defaults
  via `TenantPricingOddsReaderPort`.
- `features.tenantadmin.setup.TenantGamesPricingService` lit les odds pour la page admin jeux/pricing.
- `features.platformadmin.overview.PlatformAdminOverviewOrchestrator` ne lit plus de stats
  `catalog.pricing` ; les odds tenant ne sont pas un référentiel global.
- `features.platformadmin.tenantonboarding.TenantProvisioningOrchestrator` appelle
  `EnsureDefaultHaitiLotteryOddsCommand`.
- `core.pricing.internal.infra.web.admin.PricingOverrideAdminController` affiche les defaults pour
  contextualiser les overrides seller-terminal.

## 2. `PricingVariantCode` (promotion du concept variante)

`SettlementVariant` (ancien nom, auparavant `core.sales.internal.domain.model.result`) n'était pas
accessible à `core.pricing`. On le **promeut** en enum stable :

```java
// core.pricing.api.model.PricingVariantCode
MATCH_1_2D, MATCH_2_2D, MATCH_3_2D,
MARRIAGE_EXACT_ORDER, MARRIAGE_REVERSE_ALLOWED,
LOTTO3_STRAIGHT, LOTTO3_BOX_3_WAY, LOTTO3_BOX_6_WAY,
LOTTO4_STRAIGHT, LOTTO4_BOX_4_WAY, LOTTO4_BOX_6_WAY, LOTTO4_BOX_12_WAY, LOTTO4_BOX_24_WAY,
LOTTO4_FRONT_PAIR, LOTTO4_BACK_PAIR,
LOTTO5_LOT1_LOT2, LOTTO5_LOT1_LOT3, LOTTO5_MIXED_1_2_3
```

**Transition** : `SettlementVariant` = ancien nom, `PricingVariantCode` = nom cible. Pas de second
enum parallèle ; on déplace/renomme l'existant ; `core.sales` peut avoir des adapters temporaires,
mais le contrat inter-domaines cible utilise `PricingVariantCode`.

Les labels admin actuels (`adminLabel` FR dans l'enum) deviennent, à terme, des **clés i18n**
(`pricing.variant.lotto3.box6`) — non bloquant V0 (usage admin/support), à noter.

**Split de responsabilité (résolution)** :
- `core.sales` résout **la ou les `PricingVariantCode` applicables** à partir de la ligne commerciale
  (la résolution dépend de la sélection → proche des règles sales/result).
- `core.pricing` résout ensuite **les odds effectifs** pour ces variants
  (`seller-terminal override → tenant default → error`).

## 3. Schéma cible `pricing_odds`

```text
pricing_odds
- id
- tenant_id
- game_code
- pricing_variant_code          ← clé métier
- odds
- active
- created_at / updated_at
- bet_type              (descriptif, grouping/UI)
- commercial_option_code (descriptif, grouping/UI)
UNIQUE (tenant_id, game_code, pricing_variant_code)
```

Le runtime fait le lookup `pricingVariantCode → odds`, jamais `(betType, betOption) → odds`.
`pricing_profile_id` (multi-profils) = futur, pas maintenant.

### Reconciliation seed V38 (pré-go-live)

- Cas 1:1 (ex. `MATCH_1_2D`) → backfill direct.
- Cas non-1:1 (`LOTTO3_BOX`, `LOTTO4_BOX`) → **plusieurs lignes** (une par way), valeurs métier
  requises (ex. 4-way=1200x, 6-way=800x, 12-way=400x, 24-way=200x). Sans les valeurs métier, pas de
  backfill correct → **migration à valider pré-go-live**. En bridge, lecture tolérante.

## 4. Écriture tenant (3 couches)

```
web console-pricing-form
  → admin-games-pricing-api.service (write)
    → features.tenantadmin  PUT/DELETE /admin/pricing/odds  (perm game-pricing.update, RLS, @AuditLog)
      → core.pricing.api  UpsertTenantOddsCommand(gameCode, pricingVariantCode, odds) / Delete…
        → core.pricing internal write (repo pricing_odds)
```

Tenant depuis `TchRequestContext`. Audit : gameCode, pricingVariantCode, old→new, acteur, tenant.

Cache runtime :
- `TenantPricingOddsJpaAdapter.findActiveByTenant` et `findByNaturalKey` sont cachés côté
  `core.pricing`.
- `save` évict les régions tenant odds.
- Les caches sont exposés au groupe ops `pricing`.

## 5. Préparation de vente (single-variant ici)

```
seller choisit BetOption + saisit numéro
→ core.sales résout la PricingVariantCode applicable (dépend de la sélection)
→ core.pricing résout l'odds effective de cette variante : seller-terminal override → tenant default → error
→ core.sales snapshote oddsSnapshot sur la ligne
```

> Note : le resolver renvoie **une** variante pour Exact / Permuté simple. Les cas multi-couverture
> (Exact+Permuté, tous-les-lots, implicite) et le passage du resolver à un `CoverageResolution`
> (liste) sont l'objet de `combined-implicit-bet-coverage-v1`.

## 6. Admin UX — Barème groupé

Groupé par option commerciale (`BetOption`), variantes (`PricingVariantCode`) éditables dessous :

```text
HT_LOTO4 · Permuté
  4-way  → 1200x   [éditable]
  6-way  →  800x   [éditable]
  12-way →  400x   [éditable]
  24-way →  200x   [éditable]
```

Réutilise `console-pricing-table`/`-form`, `tch-status-badge`, tokens `--tch-*`, mobile-first.
