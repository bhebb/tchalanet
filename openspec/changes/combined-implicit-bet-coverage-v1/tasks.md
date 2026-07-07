# Tasks: combined-implicit-bet-coverage-v1

## Slice 0 — Décisions figées (cf. design)

- [x] D3 : modèle = `TicketLineCoverage` (Option B).
- [x] D4 : gain potentiel par couverture, `RANGE_ALTERNATIVE` par défaut.
- [x] Auditer `TicketLine` actuel (peut-il porter plusieurs odds ? settlement groupé ?).

## Slice 1 — Modèle couverture

- [x] `ticket_line` (commercial) + `ticket_line_coverage` (`PricingVariantCode`, stakeSnapshot, oddsSnapshot, potentialGainSnapshot, win_mode).
- [x] `TicketLine` résumé : `potentialGainMode`, `min/maxPotentialGain`, `totalPotentialGain` (cumulatif seulement).
- [x] Snapshot par couverture (réutilise `PricingVariantCode` du change 1).

## Slice 2 — Resolver `CoverageResolution`

- [x] Faire évoluer le resolver : `resolve(...) → CoverageResolution` (liste de `CoverageVariant`).
- [x] `LOTTO3_BOX` → 1 couverture ; `EXACT_PLUS_BOX` → 2.
  - [x] `LOTTO3_BOX` → 1 couverture.
  - [x] `LOTTO3_EXACT_PLUS_BOX` → 2 couvertures.
  - [x] `LOTTO4_EXACT_PLUS_BOX` → 2 couvertures.
- [x] Retirer `BOLET_ALL_LOTS` du scope de ce change : futur produit Bòlèt, modèle commercial à définir séparément.
- [x] Ajouter les `BetOption` combinées à `catalog.game` (`LOTTO3_EXACT_PLUS_BOX`, `LOTTO4_EXACT_PLUS_BOX`).
  - [x] `LOTTO3_EXACT_PLUS_BOX`, `LOTTO4_EXACT_PLUS_BOX`.

## Slice 3 — Settlement / payout

- [x] Settlement = par couverture gagnante `stake × oddsSnapshot`, applique `win_mode`.
- [ ] V0 produit : Maryaj/Loto = `BEST_OF`, Bòlèt = `CUMULATIVE`.
- [ ] Tests de règlement (table `SETTLEMENT_VARIANTS.md` étendue) + annulation/refund + reprint.

## Slice 8 — Produits Bòlèt composés futurs

- [x] Ne pas forcer `BOLET_ALL_LOTS` dans un `BetType` Bòlèt inadéquat ; le sortir du scope de ce change.
- [ ] Ouvrir un futur change produit Bòlèt pour « tous les lots » si nécessaire.
- [x] Clarifier `Dekabès` : pas un `BetOption` V0 ; label/signal post-settlement quand le joueur gagne deux fois.
- [ ] Dans ce futur change, adapter settlement Bòlèt composé en `CUMULATIVE`.
- [ ] Dans ce futur change, tester : `10` présent sur plusieurs lots paie la somme des coverages gagnantes.

## Slice 4 — Non-régression toutes surfaces (obligatoire)

- [ ] Vérifier/adapter chaque surface qui affiche ou additionne des lignes : vente, preview, confirmation, ticket detail, reçu/reprint, annulation/refund, settlement, payout, reporting minimal.
  - [x] Preview/prepare sale : expose `PotentialGainMode` typé + min/max/total.
  - [x] Reçu/reprint domaine : print line porte coverages internes, receipt line expose résumé sans variants techniques.
  - [x] Mapper/JPA : round-trip coverages + mutator protège les snapshots immutables.
- [ ] Toutes lisent les snapshots par couverture (pas un odds unique de ligne).

## Slice 5 — Implicite + aperçu/reçu

- [ ] `IMPLICIT_BEST_MATCH` (feature flag) : couvertures gagnantes automatiques, déterministe.
- [ ] Aperçu + reçu : couverture en libellés commerciaux ; gain min–max si alternatives.

## Slice 6 — Activation vente multi-couverture

- [x] Définir la règle de split stake par couverture pour `EXACT_PLUS_BOX`.
- [x] Lever le guard `multi-coverage sale is not enabled yet` dans `TicketLinePreparationService`.
- [x] Résoudre chaque coverage via `ResolveSellerTerminalOddsQuery`, snapshoter odds/gain par coverage.
- [x] Tests vente : exact+box crée 2 coverages, stake total correct, min/max correct.

## Slice 7 — Surfaces restantes hors domaine

- [x] Ticket detail / verification : exposer résumé coverage et ne pas afficher les variants techniques au client.
- [x] Cancellation/refund : vérifier que les montants utilisent stake total et snapshots immuables.
- [x] Reporting minimal / dashboard : conserver compatibilité sur `potential_payout_amount`, ajouter min/max seulement si nécessaire.
- [x] Reprint complet : test via `TicketPrintProjectionReaderAdapter` ou équivalent, pas seulement assembler.
- [x] Archive : exporter `sales_ticket_line_coverage` et inclure le résumé min/max sur `sales_ticket_line`.

## Validation

- [x] `openspec validate combined-implicit-bet-coverage-v1 --strict`.
- [ ] Tests settlement/payout ; non-rétroactivité ; web build.
  - [x] Tests ciblés resolver/préparation/payout : `TicketLinePreparationServiceTest`, `TicketWinningCalculatorTest`, `SettlementVariantResolverTest`.
  - [x] Tests ciblés mapper/reçu : `TicketAggregateMutatorTest`, `TicketReceiptAssemblerTest`.
  - [x] Test public verification : `TicketVerifyControllerTest` vérifie `PotentialGainMode` + min/max dans la réponse.
  - [x] Test reprint complet : `TicketPrintProjectionReaderAdapterTest` vérifie que l'aggregate recharge les coverages.
