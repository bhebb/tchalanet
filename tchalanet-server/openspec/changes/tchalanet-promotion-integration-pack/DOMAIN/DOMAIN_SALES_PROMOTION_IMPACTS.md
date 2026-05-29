# DOMAIN — Sales promotion impacts

## Décision

Une promotion qui touche le jeu ou le gain ne crée pas un cycle de vie parallèle.
Elle devient soit:

1. une mutation de `TicketMoneyBreakdown`, ou
2. une `TicketLine`, ou
3. un pricing/payout snapshot sur une `TicketLine` existante.

## Cas terrain

### SMS offert

```text
PromotionEffectType.WAIVE_CHARGE
appliesTo = BUYER_SMS
```

Impact: charges/money only. Pas de TicketLine.

### Maryaj gratuit

```text
paidTotal >= 500  -> quantity=1
paidTotal >= 1000 -> quantity=2
paidTotal >= 2000 -> quantity=3
```

Impact: N `TicketLine` avec:

```text
gameCode = MARYAJ_GRATUIT
origin = PROMOTION
pricingSource = PROMOTION
selectionSource = CUSTOMER_SELECTED ou AUTO_GENERATED
stakeAmount = 0
payoutBaseAmount = config/pricing snapshot
oddsSnapshot = odds tenant snapshot
promotionDecisionId = decisionId
```

### 5 boules achetées + 2 gratuites

Impact: N `TicketLine` promotionnelles. Utiliser `BOULE_GRATUIT` si variante stable; sinon `gameCode=BOULE` + `origin=PROMOTION` + `pricingSource=PROMOTION`.

### Boost odds

Impact: modifier les lignes éligibles avant persistence:

```text
pricingSource = PROMOTION
oddsSnapshot = boosted odds
potentialPayoutAmount = payoutBaseAmount * oddsSnapshot
promotionDecisionId = decisionId
```

## Règles non négociables

- Settlement ne réévalue pas la config promotion.
- Payout ne recalcule pas les promotions.
- Reprint doit pouvoir s'appuyer sur les snapshots persistés.
- Ledger/stats consomment events et dimensions (`origin`, `pricingSource`, `promotionDecisionId`).

## Modèle minimal TicketLine

Ajouter:

```text
origin
pricing_source
selection_source
payout_base_amount
promotion_decision_id nullable
```

Garder:

```text
stake_amount       = payé par le client
payout_base_amount = base utilisée pour calculer le gain
odds_snapshot      = odds final snapshot
potential_payout   = exposition max de la ligne
```
