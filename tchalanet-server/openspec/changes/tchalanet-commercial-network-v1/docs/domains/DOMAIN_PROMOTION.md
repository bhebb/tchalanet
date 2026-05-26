# DOMAIN_PROMOTION — Promotion V1

## Rappel décision

Promotion V1 n'est pas un moteur de règles complet. C'est une base extensible pour trois effets concrets afin que les effets promotionnels deviennent des snapshots sales fiables sans cycle parallèle pour settlement/payout.

## Effets V1

```text
FREE_GAME_LINE  Maryaj gratuit / ligne de jeu gratuite.
BOOST_ODDS      odds boostées sur lignes existantes.
WAIVE_CHARGE    frais/charge offert, par exemple SMS offert.
```

## Hors scope V1

Moteur de règles complet, multi-stacking avancé, quotas premiers avancés, commission seller/agent, bonus payout complexe, ledger/stats avancés, zones/agents profonds, listener sales event promotion, partner compensation.

## Promotion configure, Sales matérialise

Promotion configure campagne/rules/eligibility/effects. Sales évalue au moment de la vente, applique, snapshotte sur TicketLine / MoneyBreakdown / ticket snapshots. Settlement/Payout ne recalculent jamais les promotions.

## Conditions simples V1

`startsAt / endsAt`, `minPaidTotal`, `paidLineCount by gameCode`, `beforeLocalTime`.

## TicketLine impacts

`TicketLine` doit supporter : origin, pricingSource, selectionSource, payoutBaseAmount, promotionDecisionId nullable.

## FREE_GAME_LINE

Maryaj gratuit : ajouter game code `HT_MARYAJ_GRATUIT`; chaque tenant définit ses odds dans `pricing_odds`; payoutBaseAmount vient de PromotionEffect; ligne gratuite ajoutée par Sales; snapshot sur TicketLine.

## BOOST_ODDS

oddsOverride depuis PromotionEffect; snapshot sur TicketLine; ne modifie pas `pricing_odds`.

## WAIVE_CHARGE

Impact `MoneyBreakdown` / charges; pas d'impact pricing_odds; peut supprimer ou mettre à zéro une charge buyer.

## Runtime

Évaluer uniquement campagnes ACTIVE; cache runtime par tenant recommandé; invalidation sur activate/pause/update; éviter gros joins et parsing JSON dans le hot path.
