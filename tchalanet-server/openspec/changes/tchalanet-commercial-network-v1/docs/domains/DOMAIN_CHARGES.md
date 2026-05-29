# DOMAIN_CHARGES — Ticket Charges V1

## Contexte

Liste actuelle discutée :

```java
public enum TicketChargeType {
    BUYER_SMS,
    BUYER_WHATSAPP,
    EMAIL_NOTIFICATION,
    BUYER_EMAIL,

    // future:
    // INSURANCE,
    // WEEKEND_SURCHARGE,
    // ADMIN_ADJUSTMENT_FEE,
}
```

## Décision V1

Les charges sont des éléments de `MoneyBreakdown`. Elles ne sont pas des lignes de jeu et ne doivent pas modifier les odds.

## Charges supportées V1

- BUYER_SMS
- BUYER_WHATSAPP
- EMAIL_NOTIFICATION
- BUYER_EMAIL

## Charges futures

- INSURANCE
- WEEKEND_SURCHARGE
- ADMIN_ADJUSTMENT_FEE

## Interaction Promotion

Promotion V1 effet `WAIVE_CHARGE` peut annuler une charge.

Exemple : TicketCharge(BUYER_SMS, amount=10) + PromotionEffect(WAIVE_CHARGE, chargeType=BUYER_SMS) => charge BUYER_SMS waived/snapshot amount 0.

## Snapshot recommandé

```text
ticket_charge_snapshot
  charge_type
  original_amount
  applied_amount
  waived
  promotion_decision_id nullable
  reason_code nullable
```

Sales calcule les charges, applique promotions `WAIVE_CHARGE`, puis snapshotte le breakdown final. Settlement et payout n'interprètent pas les charges.
