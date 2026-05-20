# TODO 07 — Domain/Application cleanup sales

## Ticket domain

- [ ] `Ticket.publicCode` obligatoire, non null en domain + DB.
- [ ] `PENDING_APPROVAL` nécessite `approvalRequestId` persistent.
- [ ] Remplacer raw `UUID approvalRequestId` par typed `ApprovalRequestId` si possible.
- [ ] `Ticket.forceResult` n’accepte explicitement que `WON` ou `LOST`.
- [ ] Override audité/event, pas forcément statut public `OVERRIDDEN`.
- [ ] Ajouter snapshots si besoin : `outletId`, `cashierId`, `drawChannelId`, `terminalLabel`.

## TicketLine

- [ ] Message `externalGameCode is required` -> `gameCode is required`.
- [ ] `oddsSnapshot` strict scale 4 si pricing propre.
- [ ] Documenter invariant `potentialPayout = stake * oddsSnapshot` ou relaxer si fees/caps/rounding plus tard.

## Selection canonicalization

- [ ] `SelectionKeyCanonicalizer` reste dans `common.selection`.
- [ ] Source unique de canonicalisation :
  - 2D = `NN`
  - paires = `NN-NN`
  - 3x2D = `NN-NN-NN`
  - LOTTO3 = `NNN`
  - LOTTO4/5 digits strict tant que wildcard matching non implémenté.
- [ ] `MARRIAGE_2D2D` commutatif : `34-12` -> `12-34`.
- [ ] Décider explicitement si `12-12` est autorisé.
- [ ] Aligner `TicketWinningCalculator` avec canonical format.

## TicketWinningCalculator

- [ ] Utiliser une vue de résultat sales-friendly unique.
- [ ] Renommer `lot4()` utilisé comme pick3 vers `pick3()`/`lotto3()`.
- [ ] Ne jamais throw `UnsupportedOperationException` pendant settlement.
- [ ] Unsupported bet option doit être bloquée à la vente ou retourner false proprement.

## Application services

Déplacer hors domain service :

```text
TicketSalePolicy -> core.sales.application.service.TicketSalePolicyService
TicketSaleFactory -> core.sales.application.factory.TicketSaleFactory
```

- [ ] `TicketSalePolicyService` retourne `ALLOW`, `WARN`, `REQUIRE_APPROVAL`, `BLOCK`.
- [ ] Ne plus utiliser `BreachOutcome.BLOCK` comme approval.
- [ ] `TicketSaleFactory` utilise `DrawSaleContext` stable.
- [ ] Retry collision code autour du save, pas caché dans factory seulement.
