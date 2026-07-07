# Proposal: combined-implicit-bet-coverage-v1

## Summary

Supporter les cas où **une ligne commerciale couvre plusieurs variantes techniques** :
`EXACT_PLUS_BOX` (Exact + Permuté) et, plus tard, le mode `IMPLICIT_BEST_MATCH` (le client saisit un
numéro, le backend paie les variantes gagnantes selon les règles produit). Ce sont des changements du
**chemin monétaire** (préparation ligne + snapshot multi-odds + settlement/payout).

Réutilise `BetOption`, `PricingVariantCode` et le resolver coverage. Dépend de
`core-pricing-consolidation-tenant-odds-v1` (odds keyées par variante) et de
`tenant-game-bet-option-config-v1` (policy + options offertes).

## Goals

1. Modèle de **couverture multi-variantes** pour une ligne commerciale.
2. `EXACT_PLUS_BOX` : une ligne « 123 · Exact + Permuté » → variantes `STRAIGHT` + `BOX_*`, chacune
   avec son `oddsSnapshot`.
3. `IMPLICIT_BEST_MATCH` : résolution automatique des variantes gagnantes, à garder derrière décision
   explicite.
4. Aperçu/reçu affichent la couverture clairement (anti-litige).
5. Snapshot par variante préservé pour un settlement déterministe.

## Décisions figées (voir design)

- **D3 — Modèle = `TicketLineCoverage`** (Option B) : `TicketLine` = achat client ;
  `ticket_line_coverage` = possibilités de règlement (variant + stake + oddsSnapshot + win_mode).
- **D4 — Gain potentiel par couverture** ; Maryaj/Loto = `BEST_OF`, Bòlèt multi-lots = `CUMULATIVE`.
- **Resolver** renvoie un `CoverageResolution` (liste), plus un seul variant.
- Options commerciales combinées `LOTTO3_EXACT_PLUS_BOX` et `LOTTO4_EXACT_PLUS_BOX` vivent dans
  `catalog.game` (`BetOption`), pas comme variantes.
- Bòlèt « tous les lots » est un futur produit commercial à définir séparément, parce que le modèle
  actuel représente Bòlèt par trois `BetType` distincts (`MATCH_1_2D`, `MATCH_2_2D`, `MATCH_3_2D`).
- `EXACT_PLUS_BOX` peut être actif parce que le modèle + settlement + reçu + annulation/reprint ont
  été adaptés et testés. `IMPLICIT_BEST_MATCH` reste désactivé tant que son comportement produit
  n'est pas explicitement figé.

## Non-Goals

- `EXPLICIT_ONLY` (déjà couvert par les changes 1/2).
- Édition des odds/options (changes 1/2).
- Refonte du moteur de payout au-delà du support multi-couverture.
- SQL sans validation pré-go-live.

## Impact

- **Backend** : `core.sales` (modèle ligne/couverture, préparation, settlement, payout) ; snapshot
  multi-odds ; audit.
- **Web/POS** : aperçu + reçu montrant la couverture ; libellés commerciaux (jamais techniques au
  client).
