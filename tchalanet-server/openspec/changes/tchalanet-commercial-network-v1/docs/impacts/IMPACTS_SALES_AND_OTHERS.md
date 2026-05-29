# Impacts — Sales, LimitPolicy, Cashier, Settlement, Payout, Stats

## Sales

`core.sales` est le point de matérialisation.

Au moment de la vente, Sales doit :

1. valider operational context ;
2. résoudre seller ;
3. revalider seller / assignment avant write ;
4. vérifier limitpolicy scope SELLER ;
5. calculer lignes/pricing ;
6. évaluer promotion ;
7. appliquer charges / waive charges ;
8. snapshotter seller, assignment, commission policy, promotion, charges ;
9. créer ticket ;
10. publier events after-commit.

## Ticket impacts

Ajouter ou confirmer :

```text
sold_by_user_id
seller_id nullable ou required selon policy
seller_assignment_id nullable
seller_commission_snapshot jsonb nullable
```

## TicketLine impacts

```text
origin
pricingSource
selectionSource
payoutBaseAmount
promotionDecisionId nullable
oddsOverride nullable
```

## MoneyBreakdown impacts

Doit représenter : paid lines total, free lines total, charges original, charges applied, charges waived, promotional adjustments, grand total.

## LimitPolicy

Le prepaid/limite vendeur V1 est traité comme `scope = SELLER`, `scopeRef = sellerId`. `core.seller` ne connaît pas les plafonds.

## Cashier

`features.cashier` charge home/dashboard, affiche seller profile, outlet/session, warnings ApiNotice, ne porte pas logique métier, dispatch via bus.

## Settlement

Settlement lit uniquement les snapshots : ticket lines, promotion effects matérialisés, payoutBaseAmount, odds snapshot, seller snapshot si stats nécessaires.

Settlement ne recalcule pas promotions, commissions, seller policy.

## Payout

Payout joueur reste séparé. Payout ne paie pas seller en V1.

## Stats / Reporting

Stats peuvent consommer events sales et snapshots : sales by seller, sales by outlet, sales by outlet kind, promotion usage, waived charges, seller commission estimated. Stats ne recalculent pas les règles métier.

## Notification

Notifications in-app seulement pour low limit/prepaid threshold si implémenté comme warning. Ne pas confondre avec `platform.communication` SMS/email/WhatsApp.
