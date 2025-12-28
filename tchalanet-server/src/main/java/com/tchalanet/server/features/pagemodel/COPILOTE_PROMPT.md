# PROMPT POUR COPILOT – Pages privées & données dynamiques (Tchalanet)

## Contexte

Le backend Tchalanet est organisé en deux espaces :

- `core/` : domaines critiques (architecture hexagonale / Clean Architecture)
- `features/` : vertical slices orientées UI

Les pages (publiques et privées) sont décrites par un `PageModel` JSON stocké en base. Le BFF renvoie un `PageModelResponse` contenant :

- la langue courante
- le `PageModel` normalisé
- un bloc `dynamic` avec les données métiers pour les widgets dynamiques

## Objectif

Mettre en place la partie dynamique des pages privées (dashboards) en s’appuyant sur :

- le domaine `pagemodel` (PageModel + templates)
- les features `reporting/*`
- les domaines `draw`, `session`, `autonomy`, `audit`, etc.

Cibles principales :

- un service unique `PrivateDashboardDynamicDataService` qui construit les données dynamiques pour les dashboards privés selon le rôle `TchRole`.
- des services par rôle : `SuperadminDashboardService`, `TenantAdminDashboardService`, `CashierDashboardService`.
- des blocs Java (records) pour véhiculer la data dynamique (exposés dans `dynamic` de la réponse).

> Important : respecter l’architecture verticale ; consommer les features de reporting existantes sans les déplacer.

## Structure & packages recommandés

- Partagé pagemodel :

  - `com.tchalanet.server.features.pagemodel.shared`
  - `com.tchalanet.server.features.pagemodel.shared.dynamic`
  - `com.tchalanet.server.features.pagemodel.shared.block`

- Dashboards privés :

  - `com.tchalanet.server.features.privatedashboard.block`
  - `com.tchalanet.server.features.privatedashboard.dynamic`

- Reporting existant (à consommer) :

  - `com.tchalanet.server.features.reporting.salesreport`
  - `...outletperformance`
  - `...tenantkpis`
  - `...tenant_dashboard`

- Domaines utilisés : `com.tchalanet.server.core.draw.*`, `...core.session.*`, `...core.autonomy.*`, `...core.audit.*`.

---

## Étape 1 – Payload dynamique commun

Créer `PrivateDashboardDynamicPayload` (record) dans `com.tchalanet.server.features.privatedashboard.block` :

Champs recommandés (sous‑blocs) :

- `SuperadminOverviewBlock superadminOverview`
- `TenantAdminOverviewBlock tenantAdminOverview`
- `CashierOverviewBlock cashierOverview`
- `KpiBlock kpiGlobal`
- `KpiBlock kpiDraws`
- `KpiBlock kpiSales`
- `AlertsBlock alerts`
- `ActivityFeedBlock recentActivity`
- `ValidationsBlock validations`
- `SessionBlock session`
- `TicketsBlock recentTickets`
- `QuickSalePreloadBlock quickSale`

Ajouter une méthode statique `empty()` qui retourne un payload avec tous les champs à `null` (ou des blocs `empty()` si préférable).

Créer des records (avec `empty()` si nécessaire) pour :

- `SuperadminOverviewBlock`, `TenantAdminOverviewBlock`, `CashierOverviewBlock`
- `KpiBlock` (+ `KpiItem`)
- `AlertsBlock` (+ `AlertItem`)
- `ActivityFeedBlock` (+ `ActivityItem`)
- `ValidationsBlock` (+ `ValidationItem`)
- `SessionBlock`
- `TicketsBlock` (+ `TicketItem`)
- `QuickSalePreloadBlock` (+ types internes si nécessaire)

Préférence : utiliser des `record` immuables (Java 17+) et retourner des blocs `empty()` plutôt que `null` quand pertinent.

---

## Étape 2 – Service d’orchestration par rôle

Créer `PrivateDashboardDynamicDataService` dans `com.tchalanet.server.features.privatedashboard.dynamic` :

- Annoter `@Service` + `@RequiredArgsConstructor`.
- Injecter : `SuperadminDashboardService`, `TenantAdminDashboardService`, `CashierDashboardService`.

Méthode :

```
public PrivateDashboardDynamicPayload buildDynamicData(
    UUID tenantId,
    UUID userId,
    TchRole role,
    String currentLang,
    PageModel pageModel
)
```

Implémenter un switch sur `role` :

- `SUPER_ADMIN` → `superadminDashboardService.build(...)`
- `TENANT_ADMIN` → `tenantAdminDashboardService.build(...)`
- `CASHIER` → `cashierDashboardService.build(...)`
- `OPERATOR` → `PrivateDashboardDynamicPayload.empty()` (pour l’instant)

Ce service est appelé par l’adapter REST/BFF pour remplir `dynamic` des pages privées.

---

## Étape 3 – `TenantAdminDashboardService`

Créer `TenantAdminDashboardService` (@Service, @RequiredArgsConstructor) et injecter handlers existants (si présents) :

- `GetTenantKpisHandler` (`features.reporting.tenantkpis`)
- `TenantDashboardStatsService` / `TenantDashboardStatsUseCase` (`features.reporting.tenant_dashboard`)
- `GetOutletPerformanceReportHandler` (`features.reporting.outletperformance`)
- `ListPendingValidationsHandler` (autonomy)
- `ListTenantRecentActivityHandler` (audit / activity)

Méthode :

```
public PrivateDashboardDynamicPayload build(
    UUID tenantId,
    UUID userId,
    String currentLang,
    PageModel pageModel
)
```

Implémenter méthodes privées (TODO si handlers manquants) :

- `TenantAdminOverviewBlock buildOverview(...)`
- `KpiBlock buildGlobalKpis(...)`
- `KpiBlock buildDrawKpis(...)`
- `KpiBlock buildSalesKpis(...)`
- `ValidationsBlock buildValidations(...)`
- `ActivityFeedBlock buildActivity(...)`

Retourner un `PrivateDashboardDynamicPayload` avec : `tenantAdminOverview`, `kpiGlobal`, `kpiDraws`, `kpiSales`, `validations`, `recentActivity`.

---

## Étape 4 – `SuperadminDashboardService`

Créer `SuperadminDashboardService` (@Service, @RequiredArgsConstructor). Injecter handlers de reporting global :

- `GetPlatformOverviewHandler` (créer si nécessaire)
- `GetPlatformSalesKpiHandler` ou réutiliser `salesreport` avec `tenantId = null`
- `ListPlatformAlertsHandler`
- `ListPlatformRecentActivityHandler`

Méthode `build(UUID tenantId, UUID userId, String currentLang, PageModel pageModel)` :

- Remplir `SuperadminOverviewBlock` (nb tenants, nb actifs, nb users, santé services)
- Remplir `KpiBlock kpiGlobal` (ventes globales 30 jours)
- Remplir `AlertsBlock` (alertes plateforme)
- Remplir `ActivityFeedBlock` (activité globale)
- Retourner `PrivateDashboardDynamicPayload`.

---

## Étape 5 – `CashierDashboardService`

Créer `CashierDashboardService` (@Service, @RequiredArgsConstructor). Injecter handlers :

- `GetCashierSalesReportHandler`
- `GetCashierSessionSummaryHandler`
- `ListRecentTicketsForCashierHandler`
- `GetQuickSaleOptionsHandler`
- `GetOutletPerformanceReportHandler`
- `ListCashierNotificationsHandler`
- `ListCashierRecentActivityHandler`

Méthode `build(UUID tenantId, UUID userId, String currentLang, PageModel pageModel)` :

- Construire `CashierOverviewBlock` (ventes/payout/tickets du jour)
- `SessionBlock` (session courante)
- `TicketsBlock` (tickets récents)
- `QuickSalePreloadBlock` (options vente rapide)
- Retourner `PrivateDashboardDynamicPayload`.

---

## Étape 6 – Connexion avec `PageModelResponse`

Dans l’adapter REST/BFF qui sert les pages privées :

- Résoudre `TchRole` effectif de l’utilisateur
- Récupérer `PageModel` (domaine pagemodel)
- Obtenir `tenantId`, `userId`, `currentLang`
- Appeler `privateDashboardDynamicDataService.buildDynamicData(...)`
- Construire la réponse JSON où `dynamic.private_dashboard` contient le payload.

Mapping Java → JSON : exposer `PrivateDashboardDynamicPayload` tel quel ou via un DTO BFF.

---

## Contraintes générales

- Ne pas introduire de logique métier critique dans `features.private_dashboard`.
- Orchestrer handlers de reporting / domain existants.
- Préférer `record` immuables pour les blocs (Java 17+).
- Toujours renvoyer un bloc "vide" (`empty()`) plutôt que `null` quand pertinent.
- Respecter le multi‑tenant : filtrer par `tenantId`.

---

## Notes opérationnelles

- Cette feuille de route sert de guide pour l’implémentation progressive.
- Commencer par définir les `record` (payload & sous-blocs), puis l’orchestrateur `PrivateDashboardDynamicDataService`, enfin les services par rôle.
