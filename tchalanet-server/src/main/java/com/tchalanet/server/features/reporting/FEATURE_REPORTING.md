# Feature Reporting (BFF)

> BFF pour dashboards et rapports agrégés (multi-domaines: sales/payout/ledger). Expose vues paginées et agrégations.

---

## 1. Rôle & objectifs

- Produire des vues consolidées (tenant/admin) à partir des domaines core.
- Filtrer/paginer/exporter.

---

## 2. Endpoints

- GET `/admin/reporting/sales` — agrégats ventes.
- GET `/admin/reporting/payouts` — agrégats payouts.
- GET `/admin/reporting/ledger` — écritures.

Retour: `ApiResponse<TchPage<XxxResponse>>` + endpoints d’agrégats.

---

## 3. Handlers appelés & agrégation

- Queries: `ListSalesQuery`, `ListPayoutsQuery`, `ListLedgerEntriesQuery`.
- Agrégation dans `ReportingResponse`.

---

## 4. Pagination & cache

- `@TchPaging TchPageRequest` pour listes.
- Cache optionnel pour agrégats courants (TTL court).

---

## 5. Sécurité & permissions

- `@Secured` / `@PreAuthorize` selon rôle.
- Permissions fines via `TchPermissionEvaluator`.

---

## 6. Notes techniques

- DTO suffixes; wrappers ID.
- Pas de logique métier.

---

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/reporting.md`
