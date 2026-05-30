# Feature Reporting

> BFF de rapports agrégés pour les administrateurs tenant.  
> Lecture seule — consolide sales, payouts, ledger sans logique métier.

---

## Rôle

Produit des vues paginées et des agrégats à partir des domaines core.  
Pas de logique métier : filtre, pagine, projette, retourne.

---

## Endpoints

```http
GET /tenant/reports/sales-by-period-and-game
```
Rapport ventes par période et jeu.  
Service : `SalesReportService` · Critères : `SalesReportCriteria` · Sortie : `SalesReportResponse`.

```http
GET /tenant/reports/outlet-performance
```
Performance comparative des points de vente.  
Service : `OutletPerformanceReportService` · Sortie : `OutletPerformanceReportResponse`.

```http
GET /tenant/reports/outlet/{id}/export
GET /tenant/reports/outlet/{id}/download
```
Export et téléchargement du rapport d'un outlet spécifique.

```http
GET /tenant/reports/tenant-kpis
```
KPIs synthétiques du tenant (tickets vendus, montants, sessions, payouts).  
Service : `GetTenantKpisService` · Sortie : `TenantKpisResponse`.

---

## Conventions

- Entrée : critères nommés `*Criteria` (période, filtres, pagination)
- Sortie : `ApiResponse<TchPage<*Response>>` pour listes, `ApiResponse<*Response>` pour agrégats
- Projections SQL read-only encapsulées dans des `*Reader`
- Cache optionnel sur agrégats courants (TTL court)
- `@TchPaging TchPageRequest` pour la pagination
- `@Secured` / `@PreAuthorize` selon rôle, `TchPermissionEvaluator` pour permissions fines

---

## Frontières

- Pas de `CommandHandler` ni d'écriture
- Pas de logique métier (calcul de gains, validation vente, etc.)
- Les données sources restent dans leurs domaines propriétaires (`core.sales`, `core.payout`, `core.ledger`)

---

## Références

- Sales : `core/sales/DOMAIN_SALES.md`
- Payout : `core/payout/DOMAIN_PAYOUT.md`
- Ledger : `core/ledger/DOMAIN_LEDGER.md`
