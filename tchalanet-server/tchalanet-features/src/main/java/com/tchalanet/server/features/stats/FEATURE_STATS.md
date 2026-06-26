# Feature Stats (LEGACY BFF + read models)

> **Status**: LEGACY for new dashboard work. Do not use `features.stats` for new PageModel
> dashboards. New KPI/charts must use `core.analytics.api`.

> BFF pour exposer des statistiques agrégées (tenant/admin) sur ventes, payouts, draws. Le module possède aussi des read models statistiques persistés, documentés comme exception bornée parce qu'ils ne portent pas d'invariants métier.

## 0. Legacy boundary

`features.stats` predates `core.analytics`. It may remain temporarily for existing endpoints, but new
dashboard and reporting work must not add dependencies to it.

Migration target:

```text
core.analytics.api
  -> features.pagemodel dynamic providers
  -> features.reporting
```

New financial KPI definitions live in `core.analytics`, not here:

- gross sales = stake amount of approved tickets, excluding buyer pass-through fees;
- winnings/payouts come from ticket settlement lifecycle events;
- seller commissions are sale-time amount snapshots;
- charges are grouped by payer (`BUYER`, `SELLER`, `TENANT`) plus waived amount;
- net revenue subtracts winnings/payouts, seller commissions and tenant-paid charges.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/stats.md`

---

## 1. Rôle & objectifs

- Fournir des métriques et séries temporelles.
- Agréger côté BFF en appelant les domaines.
- Maintenir des projections statistiques dénormalisées (`stats_daily`, `stats_draw`) à partir d'événements core.

Ces objectifs ne s'appliquent qu'aux endpoints legacy existants. Pour tout nouveau dashboard,
chart, KPI PageModel ou rapport financier, utiliser `core.analytics.api`.

---

## 2. Endpoints

- GET `/tenant/stats/sales` — métriques ventes.
- GET `/tenant/stats/payouts` — métriques payouts.
- GET `/tenant/stats/draws` — métriques tirages.

Retour: `ApiResponse<StatsResponse>` ou `ApiResponse<TchPage<StatPointResponse>>`.

---

## 3. Services appelés & agrégation

- Services BFF locaux par dashboard.
- Critères d'entrée nommés `*Criteria`, modèles de sortie `*Response` / `*View` / `*Item`.
- Readers read-only pour les projections et métadonnées nécessaires aux dashboards.

---

## 4. Pagination & cache

- Pagination pour séries longues.
- Cache L1/L2 selon usage (TTL court).

---

## 5. Sécurité & permissions

- `@Secured` selon rôle; `@PreAuthorize` si permissions fines.
- Context via `@CurrentContext`.

---

## 6. Notes techniques

- UI contract suffixes; wrappers ID.
- Pas de logique métier.

---

## 7. Écart documenté : projections persistées

`features.stats.aggregates` contient des entités/repositories JPA et un listener d'événements core.
Cet écart est accepté seulement comme read model de reporting :

- les tables `stats_daily`, `stats_draw`, `stats_event_log` sont des projections dérivées;
- les listeners consomment des événements after-commit et ne modifient pas les aggregates sources;
- les updaters incrémentent des compteurs dénormalisés, sans décider de validité ticket/draw/payout;
- les dashboards lisent ces projections comme optimisation, comme `features.reporting` lit des projections cross-domain.

Limites obligatoires :

- aucun `CommandHandler`, `VoidCommandHandler` ou `QueryHandler` dans `features.stats`;
- aucune règle métier de vente, tirage, paiement ou session dans `features.stats`;
- aucune écriture dans les tables core propriétaires;
- aucune nouvelle définition KPI dashboard/rapport;
- aucun calcul de commission, odds, payout, net revenue ou frais depuis les tables legacy `stats_*`;
- si les statistiques deviennent source de vérité ou déclenchent des décisions métier, créer un domaine `core.stats` et y déplacer le write side.
