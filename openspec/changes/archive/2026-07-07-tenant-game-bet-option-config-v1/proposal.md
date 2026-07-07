# Proposal: tenant-game-bet-option-config-v1

## Summary

Ajouter, **avant le barème**, la configuration tenant des **options de vente** par jeu/betType :
quelles options commerciales sont offertes, et **comment elles sont choisies à la vente**
(politique de sélection). Réutilise `BetOption` (option commerciale) et `SettlementVariant` +
`SettlementVariantResolver` (calcul) — **aucun nouveau modèle**.

Point clé : la politique de sélection pilote **l'affichage / le choix**, pas le calcul. Même en
mode implicite (le client ne choisit pas d'option, il saisit juste un numéro), le backend **calcule
quand même** la variante gagnante et paie via `SettlementVariantResolver`.

## Portée de réutilisation (garder cette spec petite)

- **Réutilise `BetOption`** (option commerciale). Pas de `CommercialBetOption` parallèle.
- **`PricingVariantCode` n'est utilisé ici que pour l'affichage / l'admin / la readiness** (grouper
  les variantes sous une option) — cette spec **ne modifie pas** le modèle de couverture ni
  `SettlementVariantResolver`.
- Elle répond à « quelles options le tenant offre / comment le vendeur les choisit », **pas** à
  « combien ça paie » ni « comment settlement paie plusieurs couvertures ».

## Goals

1. Config tenant par `(gameCode, betType)` : options offertes, ordre, visible POS, option par défaut.
2. Politique de sélection stockable : `EXPLICIT_ONLY`, `EXPLICIT_WITH_AUTO_OPTION`,
   `IMPLICIT_BEST_MATCH`.
3. **Verrou V0** : seul **`EXPLICIT_ONLY`** est **actif/exposé au POS**.
   `EXPLICIT_WITH_AUTO_OPTION` peut être stocké/configuré mais **pas exposé au POS** tant que
   `combined-implicit-bet-coverage-v1` n'est pas terminé. `IMPLICIT_BEST_MATCH` = feature-flag/off.
4. **Implicite = calcul quand même** (quand activé) : le resolver tourne, le paiement suit ; la policy
   pilote l'affichage/choix, pas le calcul.
5. UX admin : section « Options de vente » avant « Barème ».
6. POS/vendeur : n'affiche que les options commerciales offertes ; jamais les variantes techniques.

## Non-Goals

- Odds / barème (édition) → `core-pricing-consolidation-tenant-odds-v1`.
- Option combinée `EXACT_PLUS_PERMUTE` multi-couverture + implicite complet + `TicketLineCoverage`
  → `combined-implicit-bet-coverage-v1` (y compris l'ajout des `BetOption` combinées au catalog).
- En V0 on n'offre que les `BetOption` **simples** existantes (`EXPLICIT_ONLY`).
- Matrice `canal × jeu × option` (pas en V0).
- Nouvelle permission (réutilise `game-pricing.update` en V0 ; split `game-options.update` documenté
  comme évolution possible).
- SQL sans validation pré-go-live.

## Impact

- **Backend** : config `tenant_game_bet_config` / `tenant_game_bet_option_config` (ownership à
  trancher : `core.pricing` si impacte couverture, sinon `tenant_game`) ; endpoint tenant-admin
  read/write ; audit.
- **Web (admin)** : section « Options de vente » dans la carte jeu (avant Barème).
- **Web (POS)** : liste des options offertes uniquement.
