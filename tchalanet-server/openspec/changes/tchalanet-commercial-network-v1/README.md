# Tchalanet — Commercial Network V1

## But du ZIP

Ce ZIP cadre les changements discutés autour de :

- `core.outlet` : outlet comme lieu / institution / partenaire / canal de vente.
- `core.seller` : remplacement de l'ancien concept flou `agent` par `seller` / `machann`.
- `core.promotion` : Promotions V1 avec les 3 effets déjà décidés.
- Charges de ticket : liste de charges supportées et impact `MoneyBreakdown`.
- Impacts `sales`, `limitpolicy`, `cashier`, `settlement`, `payout`, `stats`, `notification`.

## Décision clé

Tchalanet modélise un réseau de vente.

```text
User s'authentifie.
Seller vend.
Cashier est l'écran / flow POS.
Outlet est le canal, lieu, institution ou partenaire.
Sales crée le ticket et snapshotte la vérité commerciale.
LimitPolicy encadre ce que le seller peut vendre.
Promotion configure des effets commerciaux. Sales les matérialise en snapshots.
```

## Hors scope V1

- Pas de `core.compensation` générique.
- Pas de `core.partner` séparé, sauf extraction future.
- Pas de prepaid ledger financier.
- Pas de paiement seller/agent séparé.
- Pas de multi-level commission.
- Pas de partner settlement avancé.

## Lecture recommandée

1. `docs/decisions/ADR-SELLER-OUTLET-PROMOTION-V1.md`
2. `docs/domains/DOMAIN_SELLER.md`
3. `docs/domains/DOMAIN_OUTLET.md`
4. `docs/domains/DOMAIN_PROMOTION.md`
5. `docs/impacts/IMPACTS_SALES_AND_OTHERS.md`
6. `openspec/changes/commercial-network-v1/proposal.md`
