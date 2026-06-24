# Gaps backend — admin-tenant-sidenav-v0

Ces endpoints n'existent pas encore côté backend. Ils sont hors scope V0 web.
Les pages qui en ont besoin affichent un état placeholder ou désactivent l'action concernée.

## Accueil / Dashboard

```http
GET /admin/dashboard
```
BFF page d'accueil. Regroupe todaySales, openDraws, recentDraws, missingResults, topSellers, limitAlerts.
**Contournement V0** : appels parallèles vers les endpoints existants (tenant-kpis, draws/today, commission/overview).

## Tirages — détail agrégé

```http
GET /admin/draws/{drawId}/dashboard
GET /admin/draws/{drawId}/sales-summary
GET /admin/draws/{drawId}/seller-sales
GET /admin/draws/{drawId}/selection-exposure
```
Données managériales du tirage : résumé KPIs, ventes par vendeur, exposition des sélections.
**Contournement V0** : onglets Vendeurs et Sélections affichent un placeholder "Données non disponibles".

## Vendeurs

```http
POST /admin/seller-terminals/{sellerTerminalId}/pin-reset
PATCH /admin/seller-terminals/{sellerTerminalId}/status
PUT  /admin/commission/sellers/{sellerTerminalId}
GET  /admin/seller-terminals/{sellerTerminalId}/performance
```
Reset PIN, blocage/activation, commission individuelle, performance vendeur.
**Contournement V0** : actions désactivées dans le menu contextuel (non rendues).

## Rapports

```http
GET /admin/reports/seller-performance
GET /admin/reports/draw-sales
```
Rapports agrégés pour les pages Rapports > Vendeurs et Rapports > Tirages.
**Contournement V0** : pages avec placeholder "Rapport disponible prochainement" + données partielles depuis les endpoints existants.

## Mon entreprise

```http
PUT /admin/tenant-config/communication
```
Modification des infos de contact/support.
**Contournement V0** : section lecture seule avec mention "modification prochainement disponible".

## Odds par vendeur

```http
GET /admin/controls/odds/default
PUT /admin/controls/odds/default
GET /admin/controls/odds/seller-overrides
PUT /admin/controls/odds/sellers/{sellerTerminalId}
DELETE /admin/controls/odds/sellers/{sellerTerminalId}
```
Gestion des gains à payer avec exceptions par vendeur.
**Contournement V0** : affichage du barème global uniquement (`GET /admin/controls/odds`), sans gestion des exceptions.
