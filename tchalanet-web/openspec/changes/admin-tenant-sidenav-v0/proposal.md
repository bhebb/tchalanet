# Change: Admin Tenant Sidenav V0

## Status

Proposal

## Why

Le menu admin tenant était fragmenté, sans vision produit claire. Les routes existaient mais ne reflétaient pas les besoins d'un gestionnaire métier : suivre les ventes, contrôler les vendeurs, gérer les limites, saisir les résultats, analyser les performances.

Ce change redéfinit la sidenav admin complète, ses routes, et les nouvelles pages associées — en partant d'une vision produit validée : l'admin est un gestionnaire, pas un opérateur technique.

## Périmètre absorbé

Ce change absorbe les **Slices 5 et 6** de `platform-superadmin-and-tenant-admin-pages` (admin API services + admin pages). Ces slices sont marqués `Blocked / superseded by admin-tenant-sidenav-v0` dans ce change.

`platform-superadmin-and-tenant-admin-pages` conserve uniquement la partie platform/superadmin (Slices 1–4).

## Vocabulaire admin (mapping technique → libellé)

| Terme technique | Libellé admin affiché |
|---|---|
| tenant | Mon entreprise / Mon espace |
| seller terminal | Vendeur / Terminal vendeur |
| draw | Tirage |
| draw result | Résultat |
| odds | Gains à payer / Barème de gains |
| commissionRate | Commission |
| limit policy | Limite |
| ticket sell | Vente |
| lock draw | Bloquer la vente |
| unlock draw | Réouvrir la vente |
| manual result | Entrer résultat |

## Sidenav cible

```
Administration

Accueil                         /admin

Configuration générale          /admin/setup

Vendeurs
├── Liste des vendeurs          /admin/sellers
└── Nouveau vendeur             /admin/sellers/new

Tirages
├── Tous les tirages            /admin/draws
├── Tirages en cours            /admin/draws?status=open
├── Tirages passés              /admin/draws?status=past
├── Matrice des tirages         /admin/draws/matrix
└── Configuration des tirages   /admin/draws/channels

Limites
├── Limites système             /admin/limits?scope=system
├── Limite générale             /admin/limits?scope=global
├── Par vendeur                 /admin/limits?scope=seller
├── Par numéro                  /admin/limits?scope=number
├── Par jeu                     /admin/limits?scope=game
└── Par tirage                  /admin/limits?scope=draw

Contrôles de vente
├── Jeux & tarifs               /admin/controls/games
├── Gains à payer               /admin/controls/gains
└── Commissions                 /admin/controls/commissions

Promotions
├── Maryaj gratis               /admin/promotions/maryaj-gratis
└── Autres promotions           /admin/promotions

Rapports
├── Ventes                      /admin/reports/sales
├── Vendeurs                    /admin/reports/sellers
├── Tirages                     /admin/reports/draws
└── Exportations                /admin/reports/exports

Tickets
├── Liste des tickets           /admin/tickets
├── Vendre                      /admin/tickets/sell   [feature flag: adminTicketSale]
└── Vérifier                    /admin/tickets/verify

Mon entreprise
├── Identité                    /admin/company/identity
├── Adresse                     /admin/company/address
├── Apparence                   /admin/company/appearance
├── Paramètres                  /admin/company/settings
└── Support                     /admin/company/support

Aide                            /admin/help
```

## Décisions validées

**Configuration générale** = la page `admin-setup` existante (checklist d'activation). Pas de nouvelle page — renommage du label menu et amélioration des libellés i18n.

**Tirages > Matrice des tirages** = lecture de quels jeux sont vendus sur quels tirages (`draw-sales-matrix` existant).
**Tirages > Configuration des tirages** = canaux et configurations disponibles (`draw-channels` existant). Deux intentions distinctes.

**Accueil** = appels parallèles vers les endpoints existants, widgets indépendants avec skeleton/error par bloc. Le BFF `GET /admin/dashboard` est un gap futur non bloquant.

**Tickets > Vendre** = visible uniquement si `feature.adminTicketSale = true` ou permission `ticket.sell`.

**Gains à payer** = odds globales tenant + exceptions par seller-terminal. Pas de notion de "prime" en V0.

## UX principle

The tenant admin is a business manager, not a technical operator.

Admin pages must prioritize:
- seller control
- sales visibility per draw and per seller
- limits
- commissions
- result entry
- reports

Admin pages must not prioritize:
- provider operations
- batch jobs / cache / technical identifiers
- ticket-by-ticket control as the default view

## Draw detail UX contract

The default draw detail tab is `Résumé`.

It must show:
- total sold amount
- ticket count
- active seller count
- potential payout / exposure when available
- result status and draw status
- seller sales breakdown
- top selections
- available actions (state-driven)

Tickets are a secondary tab, not the main view.

## What changes

### Nouveaux fichiers

**Registry d'actions** (`features/private/admin/shared/`) :
- `domain-action.model.ts` — `DomainActionDefinition<TStatus>`, `DrawAction`, `SellerAction`, `LimitAction`

**Accueil** :
- `admin-home.page.ts`
- `admin-home-api.service.ts`

**Vendeurs** :
- `sellers/admin-sellers.page.ts` — table colonnes-riches, actions contextuelles
- `sellers/admin-seller-new.page.ts` — formulaire création
- `sellers/admin-sellers-api.service.ts`

**Tirages** :
- `draws/admin-draws.page.ts` — table avec filtres et actions bulk
- `draws/admin-draw-detail.page.ts` — 6 onglets (Résumé, Vendeurs, Sélections, Résultat, Tickets, Historique)
- `draws/admin-draws-api.service.ts`

**Limites** :
- `limits/admin-limits.page.ts` — vue unique filtrée par `scope`
- `limits/admin-limits-api.service.ts`

**Contrôles de vente** :
- Rework des pages existantes commission/baremes/games vers les nouvelles routes controls/

**Promotions** :
- `promotions/admin-promotions-maryaj.page.ts`
- `promotions/admin-promotions.page.ts`
- `promotions/admin-promotions-api.service.ts`

**Rapports** :
- `reports/admin-reports-sales.page.ts`
- `reports/admin-reports-sellers.page.ts`
- `reports/admin-reports-draws.page.ts`
- `reports/admin-reports-exports.page.ts`
- `reports/admin-reports-api.service.ts`

**Mon entreprise** (absorbe Slice 5–6 de platform-superadmin-and-tenant-admin-pages) :
- `company/admin-company-identity.page.ts`
- `company/admin-company-address.page.ts`
- `company/admin-company-appearance.page.ts`
- `company/admin-company-settings.page.ts` (absorbe admin-config.page.ts)
- `company/admin-company-support.page.ts`
- `company/admin-company-api.service.ts` (absorbe tenant-config-api.service.ts)
- `company/admin-runtime.page.ts` (absorbe admin-runtime.page.ts)
- `company/admin-games.page.ts` (absorbe admin-games.page.ts)
- `company/admin-business-days.page.ts` (absorbe admin-business-days.page.ts)

### Fichiers modifiés

- `admin.routes.ts` — restructuré complet, redirects existants conservés
- Sidenav component / `NavigationSection[]` admin — à localiser et restructurer

## Impact

- `apps/tch-portal/src/app/features/private/admin/` — refonte majeure
- Aucune lib Nx créée (tout dans l'app)
- `platform-superadmin-and-tenant-admin-pages` : Slices 5–6 superseded

## Non-goals V0

- Pas de BFF `GET /admin/dashboard` (gap backend, implémentation future)
- Pas de `GET /admin/draws/{id}/dashboard` (gap backend — onglet Résumé utilise les données disponibles)
- Pas de gestion des gaps backend listés dans `gaps-backend.md`
- Pas de NgRx (signals + store service)
- Pas de `admin-business-days.page.ts` si la page `setup` existante couvre déjà le cas
- Pas de prime/bonus
