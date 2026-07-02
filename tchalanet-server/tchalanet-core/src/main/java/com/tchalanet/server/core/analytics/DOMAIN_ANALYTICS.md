# Domain — Analytics

## Responsabilité

`core.analytics` owns les projections analytiques dérivées : KPIs, agrégats, dashboards, rapports.

Il n'est **pas** la source de vérité financière.  
Il **dérive** ses métriques depuis les domaines source : `core.sales`, `core.draw`, `core.session`.

```
core.analytics owns derived read truth.
It does not invent financial truth.
```

---

## Principe fondamental

```
core.analytics
  ├── api/          ← contrat public (commands, queries, models)
  └── internal/     ← projectors, application services, infra
```

Les features (`features.reporting`, `features.pagemodel`, dashboards) consomment uniquement `core.analytics.api`.  
Elles ne touchent pas aux tables analytics directement.

---

## Invariants

- Les projections sont **recomputables** depuis les événements source.
- Les projectors sont **idempotents** — même événement rejoué = même résultat.
- Les projections peuvent être en retard mais jamais fausses sur le passé.
- `core.analytics` ne publie pas d'événements domaine — il consomme.

---

## États et tables

| Table | Contenu |
|---|---|
| `analytics_daily` | Agrégats journaliers par dimension (`PLATFORM`, `TENANT`, `SELLER_TERMINAL`, etc.) |
| `analytics_draw` | Métriques financières par tirage |
| `analytics_seller_terminal_draw` | Métriques financières exactes par seller terminal et tirage |
| `analytics_session` | Métriques par session POS |
| `analytics_selection` | Agrégats line-level par jeu, bet type, option et sélection |
| `analytics_recompute_run` | Traçabilité des recomputes (idempotence) |

---

## Commandes

| Commande | Rôle |
|---|---|
| `RecomputeAnalyticsDailyCommand` | Recompute les agrégats journaliers depuis les sources |
| `PurgeAnalyticsCommand` | Purge les projections selon politique de rétention |

---

## Queries

| Query | Vue produite |
|---|---|
| `GetTenantDashboardStatsQuery` | KPIs tenant (ventes, tickets, winnings, payouts) |
| `GetCashierDashboardStatsQuery` | Stats session cashier POS |
| `GetPlatformDashboardStatsQuery` | Vue plateforme agrégée |
| `GetTenantKpisQuery` | KPIs détaillés tenant |
| `GetTenantFinancialBreakdownQuery` | Drilldowns tenant par jour, tirage et seller-terminal/jour |
| `GetSalesReportQuery` | Rapport ventes |
| `GetOutletReportQuery` | Rapport par outlet |

---

## Événements consommés

| Événement source | Effet |
|---|---|
| `TicketPlacedEvent` | Incrémente les ventes officielles quand le ticket est APPROVED |
| `TicketCancelledEvent` | Décrémente le nombre de tickets vendus et incrémente les annulations |
| `DrawResultAppliedEvent` | Assure/enrichit la ligne `analytics_draw` |
| `TicketWinningSettlementCreatedEvent` | Incrémente `winningsCalculated` |
| `TicketPayoutPaidEvent` | Incrémente `payoutsPaid` |
| `TicketPayoutReversedEvent` | Décrémente `payoutsPaid` en V1 |
| `SalesSessionClosedEvent` | Ferme les métriques de session |

Les projectors utilisent `ProcessedEventPort` pour l'idempotence.

---

## Règles

- Ne pas appeler `core.analytics` depuis `core.sales`, `core.payout` ou `core.settlement` — dépendance inverse interdite.
- Les dashboards/reporting passent toujours par `core.analytics.api`, jamais par SQL direct sur les tables analytics.
- Le recompute est déclenché par batch/scheduler — pas par une action utilisateur directe.
- Voir conventions : `docs/conventions/event_model.md` · `docs/conventions/idempotency.md`

## Money semantics

| Métrique | Sens |
|---|---|
| `grossSales` | Montant des mises des tickets officiellement approuvés. Les frais buyer restent séparés. |
| `winningsCalculated` | Gains calculés après résultat officiel |
| `payoutsPaid` | Gains réellement marqués payés dans le lifecycle ticket |
| `sellerCommission` | Montant de commission seller-terminal snapshoté à la vente |
| `buyerCharges` | Frais non waived payés par le client. Pass-through, pas du chiffre de jeu. |
| `sellerCharges` | Frais non waived absorbés par le seller terminal |
| `tenantCharges` | Frais non waived absorbés par le tenant |
| `waivedCharges` | Montant original des frais offerts par promotion |
| `promotionLines` | Nombre de lignes créées par promotion (`origin=PROMOTION`) |
| `promotionPricedLines` | Nombre de lignes dont pricing/odds vient d'une promotion (`pricingSource=PROMOTION`) |
| `promotionPayoutBase` | Base de payout exposée par les lignes promotionnelles |
| `promotionPotentialPayout` | Payout potentiel exposé par les lignes promotionnelles |
| `netRevenueEstimated` | `grossSales - winningsCalculated - sellerCommission - tenantCharges` |
| `netRevenuePaidBasis` | `grossSales - payoutsPaid - sellerCommission - tenantCharges` |

Il n'y a pas de table payout source-of-truth en V1. Le paiement est dérivé de la transition du
ticket gagnant vers `TicketSettlementStatus.PAID`.

## Dimensions financières

- `PLATFORM` : agrégat global, réservé super-admin.
- `TENANT` : agrégat tenant, utilisé par dashboard tenant et rapports.
- `SELLER_TERMINAL` : drilldown POS/seller terminal, utilisé pour commission et performance seller.
- `GAME` / `analytics_selection` : breakdown par jeu/selection. Pour les widgets de jeu,
  préférer `analytics_selection` qui porte `game_code`, `bet_type`, `bet_option` et `selection_key`.
- `analytics_draw` : vérité dérivée par tirage pour ventes, winnings, payouts, commissions et frais.
- `analytics_seller_terminal_draw` : vérité dérivée croisée par terminal vendeur et tirage pour
  commissions, frais, promotions, winnings/payouts et net revenue.

La commission est un pourcentage dans la configuration seller terminal, mais analytics somme le
montant snapshoté dans `TicketPlacedEvent.context.sellerCommissionAmount`. Ne jamais recalculer une
commission historique depuis le taux courant.

Les métriques promotion mesurent l'exposition et l'usage. Elles ne sont pas soustraites directement
du net revenue: le coût réel d'une ligne promotionnelle gagnante arrive via `winningsCalculated` et
`payoutsPaid`.

## Consommation par PageModel

`features.pagemodel` consomme les KPI via `core.analytics.api` seulement :

- tenant admin : `GetTenantDashboardStatsQuery` pour ventes, trend, game breakdown, commissions et frais;
- tenant admin financials : `GetTenantFinancialBreakdownQuery` pour listes jour/tirage/seller-terminal;
- platform admin : `GetPlatformDashboardStatsQuery` pour agrégats plateforme, trend, top tenants et game breakdown;
- cashier/POS : endpoints BFF POS dédiés quand la surface est opérationnelle, pas PageModel.

Les dynamic providers PageModel peuvent renommer/formatter les champs pour les widgets, mais ils ne
calculent pas les KPI financiers et ne lisent pas les tables analytics directement.

Exception opérationnelle V0 : le dashboard tenant admin peut compléter les métriques du jour avec une
lecture live de `core.sales.api` afin d'afficher immédiatement les tickets vendus, même si la
projection `analytics_daily` est en retard. Cette lecture ne remplace pas les rapports historiques :
elle sert uniquement à éviter un tableau de bord vide pendant l'exploitation courante.
