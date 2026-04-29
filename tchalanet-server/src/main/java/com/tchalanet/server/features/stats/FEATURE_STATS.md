# Feature Stats (BFF + read models)

> BFF pour exposer des statistiques agrégées (tenant/admin) sur ventes, payouts, draws. Le module possède aussi des read models statistiques persistés, documentés comme exception bornée parce qu'ils ne portent pas d'invariants métier.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/stats.md`

---

## 1. Rôle & objectifs

- Fournir des métriques et séries temporelles.
- Agréger côté BFF en appelant les domaines.
- Maintenir des projections statistiques dénormalisées (`stats_daily`, `stats_draw`) à partir d'événements core.

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
- si les statistiques deviennent source de vérité ou déclenchent des décisions métier, créer un domaine `core.stats` et y déplacer le write side.
