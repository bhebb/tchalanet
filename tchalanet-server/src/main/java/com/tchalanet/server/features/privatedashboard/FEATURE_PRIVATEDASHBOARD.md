# Feature PrivateDashboard (BFF)

> BFF pour le tableau de bord privé (tenant/admin) avec widgets agrégés (ventes, payouts, ledger, sessions).

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/privatedashboard.md`

---

## 1. Rôle & objectifs

- Exposer un tableau de bord configurable.
- Agréger des widgets (multi-domaines).

---

## 2. Endpoints (tenant/admin)

- GET `/tenant/dashboard` — dashboard tenant.
- GET `/admin/dashboard` — dashboard admin.

Retour: `ApiResponse<DashboardResponse>`.

---

## 3. Handlers appelés & agrégation

- Queries: `GetDashboardQuery` + sous-queries par widget.
- Agrégation: `DashboardResponse`.

---

## 4. Pagination & cache

- Pas de pagination globale; certaines listes internes peuvent être paginées.
- Cache L1/L2 selon widget (TTL court recommandé).

---

## 5. Sécurité & permissions

- `@Secured` / `@PreAuthorize` selon rôle.
- Context via `@CurrentContext`.

---

## 6. Notes techniques

- DTO suffixes; wrappers ID.
- Ne pas mettre de règles critiques; rester orchestrateur.
