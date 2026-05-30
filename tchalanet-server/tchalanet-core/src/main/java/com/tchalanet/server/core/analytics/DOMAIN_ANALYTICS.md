# Domain — Analytics

## Responsabilité

`core.analytics` owns les projections analytiques dérivées : KPIs, agrégats, dashboards, rapports.

Il n'est **pas** la source de vérité financière.  
Il **dérive** ses métriques depuis les domaines source : `core.sales`, `core.settlement`, `core.payout`, `core.session`.

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
| `analytics_daily` | Agrégats journaliers (ventes, montants, tickets) par tenant/outlet/draw |
| `analytics_draw` | Métriques par tirage |
| `analytics_session` | Métriques par session POS |
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
| `GetTenantDashboardStatsQuery` | KPIs tenant (ventes, tickets, payout) |
| `GetCashierDashboardStatsQuery` | Stats session cashier POS |
| `GetPlatformDashboardStatsQuery` | Vue plateforme agrégée |
| `GetTenantKpisQuery` | KPIs détaillés tenant |
| `GetSalesReportQuery` | Rapport ventes |
| `GetOutletReportQuery` | Rapport par outlet |

---

## Événements consommés

| Événement source | Effet |
|---|---|
| `TicketSoldEvent` | Incrémente `analytics_daily` et `analytics_session` |
| `DrawResultAppliedEvent` | Projette `analytics_draw` |
| `TicketSettledEvent` | Enrichit les agrégats settlement |
| `SalesSessionClosedEvent` | Ferme les métriques de session |

Les projectors utilisent `ProcessedEventPort` pour l'idempotence.

---

## Règles

- Ne pas appeler `core.analytics` depuis `core.sales`, `core.payout` ou `core.settlement` — dépendance inverse interdite.
- Les dashboards/reporting passent toujours par `core.analytics.api`, jamais par SQL direct sur les tables analytics.
- Le recompute est déclenché par batch/scheduler — pas par une action utilisateur directe.
- Voir conventions : `docs/conventions/event_model.md` · `docs/conventions/idempotency.md`
