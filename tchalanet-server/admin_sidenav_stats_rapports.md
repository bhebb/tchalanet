# Tchalanet — Admin V0 : Sidenav compacte, Dashboard KPI, Stats & Rapports

## Statut

Proposé — ajusté après retour client.

## Objectif

Simplifier l'espace admin pour un opérateur qui veut surtout contrôler son réseau de vente :

- seller terminals visibles dans l'interface sous le label métier **Vendeurs** ;
- limites, odds et primes ;
- promotions, surtout Maryaj gratis ;
- rapports simples ;
- configuration tenant et espace opérationnel.

## Décision de vocabulaire

Le concept métier et technique unifié est `seller_terminal`.

Dans l'interface admin, on peut garder le label court **Vendeurs**, mais il désigne toujours un `seller_terminal`. On ne crée pas de pages séparées pour `seller`, `terminal` ou `vendeur`.

```text
Vendeurs = seller_terminal
Ajouter vendeur = créer un seller_terminal
Vendeurs actifs = seller_terminals actifs
Odds / limites / primes vendeur = règles appliquées au seller_terminal
```

La sidenav doit rester utilisable sur portable **sans scroll**. Les sous-menus existent et sont accessibles depuis la sidenav, mais ils ne sont pas dépliés automatiquement. Les détails analytiques comme **par seller terminal**, **par tirage**, **par date** restent dans la page Rapports sous forme de filtres, cartes, tabs ou tableaux.

---

# 1. Principe de navigation

## 1.1 Sidenav compacte visible

```text
Accueil
Vendeurs
Contrôles
Promotions
Rapports
Plus
```

## 1.2 Sous-menus accessibles mais non dépliés

Le shell peut recevoir des `children`, mais l'UI doit afficher uniquement les 6 entrées principales par défaut.

Sur desktop : un chevron, un hover menu, un menu contextuel ou un panneau secondaire peut afficher les enfants.

Sur mobile : un tap sur le parent ouvre la page parent ; les enfants peuvent aussi être disponibles via un bouton secondaire ou une action rapide, mais la sidenav ne doit pas devenir longue.

---

# 2. Sous-menus validés

## 2.1 Vendeurs

Dans l'UX admin, **Vendeurs** représente **seller_terminal**. On ne sépare plus vendeur, seller et terminal dans l'interface admin : un vendeur opérationnel = un `seller_terminal`.

Sous-menus visibles/accessibles :

```text
Vendeurs
  - Ajouter vendeur
  - Vendeurs actifs
```

Ce qu'on ne met pas dans la sidenav :

```text
- Par seller terminal
- Bloqués
- Sans activité
- Par succursale
```

Ces éléments restent dans la page Vendeurs comme filtres, cartes ou tabs.

## 2.2 Contrôles

Sous-menus :

```text
Contrôles
  - Limites
  - Odds
  - Primes
```

Notes :

- `Odds` doit gérer les odds par défaut et les overrides par seller terminal.
- `Limites` doit gérer les limites tenant, seller terminal, jeu, tirage et sélection.
- `Primes` concerne les commissions / bonus / incentives des seller terminals.

## 2.3 Promotions

Sous-menus :

```text
Promotions
  - Maryaj gratis
  - Promotions actives
```

Ce qu'on ne met pas dans la sidenav :

```text
- Promotions programmées
- Promotions expirées
- Historique détaillé
```

Ces éléments restent dans la page Promotions.

## 2.4 Rapports

Sous-menus :

```text
Rapports
  - Rapport du jour
  - Export / impression
```

Ce qu'on ne met pas dans la sidenav :

```text
- Par seller terminal
- Par tirage
- Par date
- Par jeu
- Seller terminals actifs
- Commissions / primes
```

Ces axes doivent être dans la page Rapports via filtres et sections internes.

## 2.5 Plus

Sous-menus :

```text
Plus
  - Configuration générale
  - Mon espace
  - Mon compte
  - Support
```

`Configuration générale` contient la configuration tenant, dont la configuration des tirages.

`Mon espace` contient l'organisation opérationnelle : succursales, points de vente, utilisateurs admin. Les seller terminals restent gérés dans **Vendeurs**.

---

# 3. JSON sidenav cible

```json
{
  "navigation": [
    {
      "code": "dashboard",
      "label": "Accueil",
      "icon": "dashboard",
      "route": "/app/admin/dashboard"
    },
    {
      "code": "seller_terminals",
      "label": "Vendeurs",
      "icon": "point_of_sale",
      "route": "/app/admin/sellers",
      "collapsedByDefault": true,
      "children": [
        {
          "code": "seller_terminal_add",
          "label": "Ajouter vendeur",
          "icon": "person_add",
          "route": "/app/admin/sellers/new"
        },
        {
          "code": "seller_terminal_active",
          "label": "Vendeurs actifs",
          "icon": "check_circle",
          "route": "/app/admin/sellers?status=active"
        }
      ]
    },
    {
      "code": "controls",
      "label": "Contrôles",
      "icon": "tune",
      "route": "/app/admin/controls",
      "collapsedByDefault": true,
      "children": [
        {
          "code": "limits",
          "label": "Limites",
          "icon": "shield",
          "route": "/app/admin/controls/limits"
        },
        {
          "code": "odds",
          "label": "Odds",
          "icon": "percent",
          "route": "/app/admin/controls/odds"
        },
        {
          "code": "bonuses",
          "label": "Primes",
          "icon": "payments",
          "route": "/app/admin/controls/bonuses"
        }
      ]
    },
    {
      "code": "promotions",
      "label": "Promotions",
      "icon": "campaign",
      "route": "/app/admin/promotions",
      "collapsedByDefault": true,
      "children": [
        {
          "code": "maryaj_gratis",
          "label": "Maryaj gratis",
          "icon": "redeem",
          "route": "/app/admin/promotions/maryaj-gratis"
        },
        {
          "code": "promotions_active",
          "label": "Promotions actives",
          "icon": "campaign",
          "route": "/app/admin/promotions?status=active"
        }
      ]
    },
    {
      "code": "reports",
      "label": "Rapports",
      "icon": "bar_chart",
      "route": "/app/admin/reports",
      "collapsedByDefault": true,
      "children": [
        {
          "code": "daily_report",
          "label": "Rapport du jour",
          "icon": "today",
          "route": "/app/admin/reports/daily"
        },
        {
          "code": "reports_export",
          "label": "Export / impression",
          "icon": "print",
          "route": "/app/admin/reports/export"
        }
      ]
    },
    {
      "code": "more",
      "label": "Plus",
      "icon": "more_horiz",
      "route": "/app/admin/more",
      "collapsedByDefault": true,
      "children": [
        {
          "code": "configuration",
          "label": "Configuration générale",
          "icon": "settings",
          "route": "/app/admin/more/configuration"
        },
        {
          "code": "workspace",
          "label": "Mon espace",
          "icon": "storefront",
          "route": "/app/admin/more/workspace"
        },
        {
          "code": "account",
          "label": "Mon compte",
          "icon": "account_circle",
          "route": "/app/admin/more/account"
        },
        {
          "code": "support",
          "label": "Support",
          "icon": "support_agent",
          "route": "/app/admin/more/support"
        }
      ]
    }
  ]
}
```

---

# 4. Dashboard admin

Route :

```text
/app/admin/dashboard
```

Le dashboard doit afficher les KPI qui répondent aux questions client :

```text
Est-ce que mes vendeurs travaillent ?
Quels seller terminals vendent ?
Combien j'ai vendu aujourd'hui ?
Combien je dois payer ?
Est-ce qu'il y a des alertes limites / odds / promotions ?
```

## 4.1 KPI principaux

```text
Ventes aujourd'hui
- Montant total vendu aujourd'hui
- Nombre de transactions / tickets
- Panier moyen

Seller terminals / vendeurs
- Seller terminals actifs aujourd'hui
- Seller terminals inactifs aujourd'hui
- Seller terminals bloqués
- Dernière activité
- Top seller terminal du jour
- Seller terminal avec alerte

Paiement / risque
- Montant gagnant estimé
- Montant à payer
- Montant payé
- Reste à payer
- Alertes limites
```

## 4.2 Widgets dashboard V0

```text
1. KPI cards
   - Ventes aujourd'hui
   - Montant à payer
   - Seller terminals actifs
   - Alertes limites

2. Activité seller terminals
   - Top 5 seller terminals actifs
   - Seller terminals sans activité

3. Alertes rapides
   - Seller terminal bloqué
   - Seller terminal sans activité
   - Limite proche / dépassée
   - Maryaj gratis inactif ou expiré

4. Raccourcis
   - Ajouter vendeur
   - Voir vendeurs actifs
   - Gérer limites
   - Gérer odds
   - Maryaj gratis
   - Rapport du jour
```

## 4.3 Exemple Dashboard PageModel

```json
{
  "code": "private.admin.dashboard",
  "scope": "private",
  "slug": "dashboard",
  "context": "tenant_admin",
  "schemaVersion": 2,
  "title": "Accueil",
  "widgets": [
    {
      "code": "sales_today",
      "type": "kpi-card",
      "title": "Ventes aujourd'hui",
      "dataEndpoint": "/api/v1/admin/dashboard/kpis/sales-today"
    },
    {
      "code": "active_seller_terminals",
      "type": "kpi-card",
      "title": "Vendeurs actifs",
      "dataEndpoint": "/api/v1/admin/dashboard/kpis/active-seller-terminals"
    },
    {
      "code": "payout_due",
      "type": "kpi-card",
      "title": "À payer",
      "dataEndpoint": "/api/v1/admin/dashboard/kpis/payout-due"
    },
    {
      "code": "seller_terminal_activity",
      "type": "table-card",
      "title": "Activité vendeurs",
      "dataEndpoint": "/api/v1/admin/dashboard/seller-terminal-activity"
    },
    {
      "code": "alerts",
      "type": "alert-list",
      "title": "Alertes",
      "dataEndpoint": "/api/v1/admin/dashboard/alerts"
    }
  ],
  "actions": [
    { "code": "add_seller_terminal", "label": "Ajouter vendeur", "route": "/app/admin/sellers/new" },
    { "code": "active_seller_terminals", "label": "Vendeurs actifs", "route": "/app/admin/sellers?status=active" },
    { "code": "daily_report", "label": "Rapport du jour", "route": "/app/admin/reports/daily" }
  ]
}
```

---

# 5. Page Vendeurs

Route parent :

```text
/app/admin/sellers
```

Dans l'interface, on garde le label **Vendeurs**. Techniquement, la page manipule uniquement le concept `seller_terminal`.

```text
Vendeur affiché = seller_terminal
```

Un `seller_terminal` porte l'identité opérationnelle utile à l'admin : nom/code vendeur, statut, point de vente, activité, odds/limites/primes et capacité de vente. On ne présente pas un objet `seller` séparé d'un objet `terminal`.

## 5.1 Sous-menus sidenav

```text
/app/admin/sellers/new
/app/admin/sellers?status=active
```

## 5.2 Contenu de page

Les détails restent dans la page :

```text
Filtres :
- Statut seller terminal
- Point de vente
- Dernière activité
- Période

Tabs ou cartes :
- Tous
- Actifs
- Bloqués
- Sans activité
- Non assignés
```

## 5.3 Table V0

```text
Colonnes :
- Vendeur / seller terminal
- Point de vente
- Statut
- Ventes aujourd'hui
- Dernière activité
- Odds override
- Limite override
- Actions
```

## 5.4 Actions rapides

```text
- Ajouter vendeur
- Activer / bloquer
- Voir détails
- Voir rapport seller terminal
- Modifier odds
- Modifier limites
```

---

# 6. Page Contrôles

Route parent :

```text
/app/admin/controls
```

## 6.1 Sous-menus sidenav

```text
/app/admin/controls/limits
/app/admin/controls/odds
/app/admin/controls/bonuses
```

## 6.2 Contrôles / Limites

La page Limites doit permettre :

```text
- Limite globale tenant
- Limite par seller terminal
- Limite par jeu
- Limite par tirage
- Limite par numéro / sélection
```

## 6.3 Contrôles / Odds

La page Odds doit permettre :

```text
- Odds par défaut
- Odds par seller terminal
- Exceptions actives
- Historique des changements
- Réinitialisation d'une exception
```

Hiérarchie recommandée :

```text
Tenant default
  -> SellerTerminal override
```

## 6.4 Contrôles / Primes

La page Primes doit permettre :

```text
- Commission standard
- Prime de volume
- Bonus temporaire
- Règles actives / inactives
- Seller terminals éligibles
```

---

# 7. Page Promotions

Route parent :

```text
/app/admin/promotions
```

## 7.1 Sous-menus sidenav

```text
/app/admin/promotions/maryaj-gratis
/app/admin/promotions?status=active
```

## 7.2 Maryaj gratis

Maryaj gratis est une promotion client, pas une prime vendeur.

Fonctions V0 :

```text
- Activer / désactiver
- Définir les conditions d'éligibilité
- Définir le montant minimum
- Définir le nombre de lignes gratuites
- Définir la génération automatique
- Définir la régénération avant confirmation
- Voir statut : active / inactive / programmée / expirée
```

## 7.3 Promotions actives

La page Promotions actives doit montrer :

```text
- Promotion
- Statut
- Date début / fin
- Jeu concerné
- Conditions
- Effet
- Actions
```

---

# 8. Page Rapports

Route parent :

```text
/app/admin/reports
```

## 8.1 Sous-menus sidenav

```text
/app/admin/reports/daily
/app/admin/reports/export
```

La sidenav ne contient pas les axes analytiques. Ils vivent dans la page.

## 8.2 Axes dans la page

```text
Rapports
- Par seller terminal
- Par tirage
- Par date / période
- Par jeu
- Par point de vente
- Commissions / primes
- Seller terminals actifs
```

## 8.3 Filtres globaux de la page

```text
- Aujourd'hui
- Hier
- Cette semaine
- Ce mois
- Période personnalisée
- Seller terminal
- Point de vente
- Tirage
- Jeu
```

## 8.4 KPI Rapports

```text
- Total ventes
- Nombre de transactions
- Montant moyen
- Montant gagnant estimé
- Montant payé
- Reste à payer
- Commission estimée
- Seller terminals actifs
- Vendeurs actifs
```

## 8.5 Rapport du jour

Le rapport du jour est un raccourci important.

Il doit afficher :

```text
- Ventes totales du jour
- Ventes par heure
- Ventes par seller terminal
- Ventes par tirage
- Montant à payer
- Montant payé
- Alertes
```

## 8.6 Export / impression

La page Export / impression doit permettre :

```text
- Choisir type de rapport
- Choisir période
- Choisir format : PDF / CSV plus tard
- Imprimer
- Télécharger
```

---

# 9. Configuration générale vs Mon espace

## 9.1 Configuration générale

Route :

```text
/app/admin/more/configuration
```

Contient :

```text
- Informations tenant
- Devise
- Langue
- Fuseau horaire
- Configuration des tirages
- Jeux actifs globaux
- Paramètres d'impression
- Thème / logo
```

Important : la **configuration des tirages** reste ici, pas dans Rapports et pas dans Odds.

## 9.2 Mon espace

Route :

```text
/app/admin/more/workspace
```

Contient :

```text
- Succursales
- Points de vente
- Utilisateurs admin
- Profil business
```

Règle :

```text
Configuration générale configure le tenant.
Mon espace gère l'organisation physique/opérationnelle.
Rapports observe.
Contrôles ajuste les règles métier.
```

---

# 10. Bootstrap privé admin

Endpoint :

```http
GET /api/v1/tenant/runtime/bootstrap
```

Le bootstrap charge uniquement le shell privé : contexte, acteur, tenant, thème, navigation principale, permissions, feature flags et petites alertes globales.

Il ne charge pas les rapports, les tableaux de seller terminals ou les grosses listes.

```json
{
  "context": {
    "scope": "ADMIN",
    "area": "tenant_admin",
    "timezone": "America/Port-au-Prince",
    "locale": "fr"
  },
  "actor": {
    "displayName": "Admin",
    "roles": ["TENANT_ADMIN"],
    "permissions": [
      "SELLER_TERMINAL_READ",
      "SELLER_TERMINAL_ADMIN",
      "LIMITS_ADMIN",
      "ODDS_ADMIN",
      "PROMOTION_ADMIN",
      "REPORT_READ",
      "REPORT_EXPORT",
      "TENANT_CONFIG_ADMIN"
    ]
  },
  "tenant": {
    "name": "Client Borlette",
    "code": "client-borlette",
    "currency": "HTG",
    "status": "ACTIVE"
  },
  "shell": {
    "defaultRoute": "/app/admin/dashboard",
    "navigation": []
  },
  "features": {
    "maryajGratis": true,
    "sellerTerminalOdds": true,
    "drawConfiguration": true,
    "reportsExport": true,
    "sellerTerminalStats": true,
    "drawStats": true
  },
  "alerts": {
    "count": 0,
    "highestSeverity": null
  }
}
```

La vraie `navigation` du bootstrap reprend le JSON de la section 3.

---

# 11. Private PageModels

Les PageModels privés retournent le modèle d'une page précise : layout, sections, widgets, actions, filtres, tables, empty states et routes enfants.

Ils ne retournent pas les données lourdes.

## 11.1 PageModel admin reports

```http
GET /api/v1/tenant/private/page-models/private.admin.reports
```

```json
{
  "code": "private.admin.reports",
  "scope": "private",
  "slug": "reports",
  "context": "tenant_admin",
  "schemaVersion": 2,
  "title": "Rapports",
  "layout": {
    "type": "admin-page",
    "density": "comfortable"
  },
  "sections": [
    {
      "code": "daily",
      "title": "Rapport du jour",
      "description": "Voir les ventes, paiements et alertes du jour.",
      "icon": "today",
      "route": "/app/admin/reports/daily",
      "requiredPermission": "REPORT_READ"
    },
    {
      "code": "analytics",
      "title": "Analyse",
      "description": "Analyser par seller terminal, tirage, date ou jeu.",
      "icon": "monitoring",
      "route": "/app/admin/reports",
      "requiredPermission": "REPORT_READ"
    },
    {
      "code": "export",
      "title": "Export / impression",
      "description": "Exporter ou imprimer les rapports.",
      "icon": "print",
      "route": "/app/admin/reports/export",
      "requiredPermission": "REPORT_EXPORT"
    }
  ],
  "filters": [
    { "code": "period", "type": "date-range", "default": "today" },
    { "code": "sellerTerminal", "type": "seller-terminal-select", "optional": true },
    { "code": "draw", "type": "draw-select", "optional": true },
    { "code": "game", "type": "game-select", "optional": true }
  ],
  "widgets": [
    { "code": "sales_total", "type": "kpi-card", "title": "Total ventes" },
    { "code": "active_seller_terminals", "type": "kpi-card", "title": "Vendeurs actifs" },
    { "code": "payout_due", "type": "kpi-card", "title": "À payer" },
    { "code": "analytics_table", "type": "table", "title": "Analyse détaillée" }
  ]
}
```

## 11.2 PageModel admin seller terminals

```http
GET /api/v1/tenant/private/page-models/private.admin.seller-terminals
```

```json
{
  "code": "private.admin.seller-terminals",
  "scope": "private",
  "slug": "seller-terminals",
  "context": "tenant_admin",
  "schemaVersion": 2,
  "title": "Vendeurs",
  "subtitle": "Contrôle des seller terminals",
  "sections": [
    {
      "code": "add_seller_terminal",
      "title": "Ajouter vendeur",
      "route": "/app/admin/sellers/new",
      "requiredPermission": "SELLER_TERMINAL_ADMIN"
    },
    {
      "code": "active_seller_terminals",
      "title": "Vendeurs actifs",
      "route": "/app/admin/sellers?status=active",
      "requiredPermission": "SELLER_TERMINAL_READ"
    }
  ],
  "table": {
    "columns": [
      "sellerTerminal",
      "outlet",
      "status",
      "salesToday",
      "lastActivityAt",
      "oddsOverride",
      "limitOverride",
      "actions"
    ]
  },
  "filters": [
    { "code": "status", "type": "enum", "optional": true },
    { "code": "sellerTerminal", "type": "seller-terminal-select", "optional": true },
    { "code": "outlet", "type": "outlet-select", "optional": true },
    { "code": "activity", "type": "enum", "optional": true }
  ]
}
```

---

# 12. Data endpoints V0

## 12.1 Dashboard

```http
GET /api/v1/admin/dashboard/summary
GET /api/v1/admin/dashboard/kpis/sales-today
GET /api/v1/admin/dashboard/kpis/active-seller-terminals
GET /api/v1/admin/dashboard/kpis/payout-due
GET /api/v1/admin/dashboard/seller-terminal-activity
GET /api/v1/admin/dashboard/alerts
```

## 12.2 Vendeurs

```http
GET  /api/v1/admin/seller-terminals
POST /api/v1/admin/seller-terminals
GET  /api/v1/admin/seller-terminals/active
GET  /api/v1/admin/seller-terminals/{sellerTerminalId}
POST /api/v1/admin/seller-terminals/{sellerTerminalId}/activate
POST /api/v1/admin/seller-terminals/{sellerTerminalId}/block
```

## 12.3 Contrôles

```http
GET /api/v1/admin/controls/limits
PUT /api/v1/admin/controls/limits/{limitId}

GET /api/v1/admin/controls/odds
PUT /api/v1/admin/controls/odds/default
PUT /api/v1/admin/controls/odds/seller-terminals/{sellerTerminalId}
DELETE /api/v1/admin/controls/odds/seller-terminals/{sellerTerminalId}/override

GET /api/v1/admin/controls/bonuses
POST /api/v1/admin/controls/bonuses
PUT /api/v1/admin/controls/bonuses/{bonusId}
```

## 12.4 Promotions

```http
GET  /api/v1/admin/promotions
GET  /api/v1/admin/promotions/active
GET  /api/v1/admin/promotions/maryaj-gratis
PUT  /api/v1/admin/promotions/maryaj-gratis
POST /api/v1/admin/promotions/maryaj-gratis/activate
POST /api/v1/admin/promotions/maryaj-gratis/deactivate
```

## 12.5 Rapports

```http
GET /api/v1/admin/reports/summary
GET /api/v1/admin/reports/daily
GET /api/v1/admin/reports/analytics
GET /api/v1/admin/reports/export/options
POST /api/v1/admin/reports/export
```

`/reports/analytics` accepte des filtres pour :

```text
- période
- seller terminal
- tirage
- jeu
- point de vente
```

Exemples :

```http
GET /api/v1/admin/reports/analytics?period=today&groupBy=sellerTerminal
GET /api/v1/admin/reports/analytics?period=today&groupBy=draw
GET /api/v1/admin/reports/analytics?from=2026-06-01&to=2026-06-15&groupBy=date
```

---

# 13. Backend placement

## Features

Les écrans admin, PageModels et agrégations UI vivent dans :

```text
features.tenantadmin.dashboard
features.tenantadmin.sellerterminals
features.tenantadmin.controls
features.tenantadmin.promotions
features.tenantadmin.reports
features.tenantadmin.more
```

## Domaines sources

```text
core.sales
  - ventes
  - tickets agrégés
  - volumes par période

core.sellerterminal
  - seller terminals
  - identité opérationnelle vendeur/terminal unifiée
  - statut opérationnel côté vente
  - point de vente associé
  - dernière activité

core.outlet
  - points de vente / succursales

core.draw
  - tirages tenant
  - statut tirage

core.drawresult
  - résultats appliqués

core.payout
  - payé / à payer

core.limitpolicy
  - alertes limites / exposition

core.promotion
  - promotions / Maryaj gratis

catalog.game
  - jeux disponibles

catalog.resultslot / catalog.drawchannel
  - définitions de tirage et slots

platform.tenantconfig
  - configuration générale tenant

platform.tenanttheme
  - thème / branding
```

Règle :

```text
features agrège.
core décide.
catalog décrit.
platform configure.
```

---

# 14. Permissions V0

```text
SELLER_TERMINAL_READ
SELLER_TERMINAL_ADMIN
LIMITS_ADMIN
ODDS_ADMIN
BONUS_ADMIN
PROMOTION_READ
PROMOTION_ADMIN
REPORT_READ
REPORT_EXPORT
TENANT_CONFIG_READ
TENANT_CONFIG_ADMIN
WORKSPACE_READ
WORKSPACE_ADMIN
```

---

# 15. Critères d'acceptation

## Navigation

- [ ] La sidenav affiche seulement 6 entrées principales par défaut : Accueil, Vendeurs, Contrôles, Promotions, Rapports, Plus.
- [ ] Aucun scroll vertical n'est nécessaire sur portable pour voir les entrées principales.
- [ ] Les sous-menus existent dans le payload `children`.
- [ ] Les enfants ne sont pas dépliés automatiquement.
- [ ] Vendeurs expose seulement `Ajouter vendeur` et `Vendeurs actifs` en sous-menu.
- [ ] Contrôles expose seulement `Limites`, `Odds`, `Primes` en sous-menu.
- [ ] Promotions expose seulement `Maryaj gratis`, `Promotions actives` en sous-menu.
- [ ] Rapports expose seulement `Rapport du jour`, `Export / impression` en sous-menu.

## Dashboard

- [ ] Le dashboard affiche des KPI de ventes et seller terminals.
- [ ] Le dashboard affiche au minimum : ventes aujourd'hui, à payer, seller terminals actifs, alertes.
- [ ] Les KPI ne chargent pas les grosses listes.
- [ ] Les raccourcis principaux sont visibles : Ajouter vendeur, Vendeurs actifs, Rapport du jour.

## Rapports

- [ ] Les axes `par seller terminal`, `par tirage`, `par date` sont dans la page Rapports, pas dans la sidenav.
- [ ] La page Rapports permet de filtrer par période, seller terminal, tirage et jeu.
- [ ] Le rapport du jour est accessible directement depuis la sidenav.
- [ ] Export / impression est accessible directement depuis la sidenav.

## Backend

- [ ] Le bootstrap privé retourne la navigation principale et les enfants, mais pas les données lourdes.
- [ ] Les PageModels privés retournent les layouts, widgets, filtres et actions.
- [ ] Les données de dashboard et rapports viennent d'endpoints data dédiés.
- [ ] Les listes sont paginées si la cardinalité peut grossir.
- [ ] Les controllers restent minces : validation, contexte, sécurité, dispatch CommandBus / QueryBus.
