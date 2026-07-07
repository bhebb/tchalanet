# Proposal: core-pricing-consolidation-tenant-odds-v1

## Summary

Résoudre la confusion « deux pricing » et ouvrir la modification tenant des odds. Aujourd'hui
`catalog.pricing` porte les odds tenant par défaut comme s'il s'agissait d'un référentiel catalog,
alors que `core.pricing` porte déjà les overrides seller-terminal + la résolution runtime. Un tenant
doit pouvoir **modifier son barème** (config métier, impact ventes futures + snapshot) → c'est du
**runtime**, pas du catalog. On **consolide l'ownership du pricing tenant dans `core.pricing`** et on
expose l'édition tenant.

Deuxième correction de fond : les odds sont aujourd'hui keyées par `(betType, betOption)`, donc
« Permuté » a un seul odds quel que soit le box-way. Or `LOTTO3_BOX` → 3-way/6-way et `LOTTO4_BOX`
→ 4/6/12/24-way doivent payer différemment. `BetOption` (commercial) n'est donc **pas assez précis**
pour le pricing. On **keye les odds par la variante technique**, promue en type canonique stable
`PricingVariantCode` (voir design), et on snapshote à la vente l'odds de la variante résolue.

## Réutilisation + terme canonique

- Le concept variante existe déjà (`SettlementVariant` + `SettlementVariantResolver`, `SETTLEMENT_VARIANTS.md`).
- **Règle de transition (stricte, anti-prolifération)** :
  - `SettlementVariant` = **ancien nom** ; `PricingVariantCode` (`core.pricing.api.model`) = **nom cible**.
  - **Aucun nouveau code n'introduit un second enum parallèle** (`PricingVariant`, `SettlementVariantCode`, …).
  - On **déplace/renomme progressivement** l'enum existant vers `PricingVariantCode`.
  - `core.sales` peut garder des **adapters temporaires**, mais le contrat inter-domaines **cible**
    utilise `PricingVariantCode`.
- `BetOption` (`catalog.game`) = option commerciale (choix vendeur), **descriptive** pour l'admin,
  pas la clé de pricing.

## Goals

1. Ownership du pricing tenant **dans `core.pricing`** ; l'ancien package `catalog.pricing` est
   retiré du runtime cible (migration Java d'abord, **pas de rename SQL** au premier slice).
2. Odds keyées par **`PricingVariantCode`** ; unicité fonctionnelle
   `(tenant_id, game_code, pricing_variant_code)`. `bet_type` / `commercial_option_code` stockés
   **descriptifs** (grouping/UI admin), jamais la clé.
3. Édition tenant des odds par défaut : commande `core.pricing.api` tenant-scopée (RLS) + endpoint
   `features.tenantadmin` (`/admin/pricing/odds`), permission `game-pricing.update`, `@AuditLog`.
4. Résolution runtime **inchangée** : `seller-terminal override → tenant default → error`.
5. Snapshot à la vente = odds de la variante résolue (préparation de vente appelle le resolver).
6. Non-rétroactivité préservée : tickets vendus gardent `oddsSnapshot`.

## Non-Goals

- Options combinées (`EXACT_PLUS_PERMUTE`) et implicite → `combined-implicit-bet-coverage-v1`.
- Config des options offertes / selection policy → `tenant-game-bet-option-config-v1`.
- Niveau « global Tchalanet » de pricing / référentiel pricing plateforme (hors périmètre V0).
  Les odds sont une configuration runtime tenant, puis éventuellement seller-terminal override.
- Refonte settlement/payout.
- Nouvelle permission (réutilise `game-pricing.update`).
- Renommage SQL sans validation pré-go-live (le keying variant peut nécessiter une reconciliation de
  `pricing_odds` — voir design).

## Impact

- **Backend** : migration d'ownership tenant `catalog.pricing → core.pricing` ; commande d'écriture
  tenant + query ; `features.tenantadmin` endpoint ; préparation de vente qui snapshote l'odds de
  la variante. `/platform/pricing` est retiré du parcours cible : pas de référentiel pricing
  plateforme V0.
- **Web** : `admin-games-pricing-api` gagne les writes ; barème éditable, groupé par option
  commerciale (`BetOption`) avec variantes (`SettlementVariant`) dessous.
- **Data** : `pricing_odds` doit distinguer les variantes (reconciliation du seed V38 — voir design).
