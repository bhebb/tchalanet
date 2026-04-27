# Feature Stats (BFF)

> BFF pour exposer des statistiques agrégées (tenant/admin) sur ventes, payouts, draws.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/stats.md`

---

## 1. Rôle & objectifs

- Fournir des métriques et séries temporelles.
- Agréger côté BFF en appelant les domaines.

---

## 2. Endpoints

- GET `/tenant/stats/sales` — métriques ventes.
- GET `/tenant/stats/payouts` — métriques payouts.
- GET `/tenant/stats/draws` — métriques tirages.

Retour: `ApiResponse<StatsResponse>` ou `ApiResponse<TchPage<StatPointResponse>>`.

---

## 3. Handlers appelés & agrégation

- Queries des domaines correspondants.
- Agrégation: `StatsResponse`.

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

- DTO suffixes; wrappers ID.
- Pas de logique métier.
