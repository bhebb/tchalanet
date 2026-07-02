# Convention — Structure des features Angular `tchalanet-web`

## Objectif

Cette convention définit l’organisation des features Angular dans les applications Nx :

```text
apps/<portal>/src/app
apps/public-portal/src/app
apps/admin-portal/src/app
apps/platform-portal/src/app
```

L’objectif est de garder une structure claire, maintenable et progressive.

> Pour le **contenu** d'un écran console (liste, détail, création, édition, drawer), voir
> [`feature-playbook.md`](./feature-playbook.md). Ce document couvre uniquement l'organisation
> des dossiers/fichiers.

---

# 1. Arborescence générale `features/`

Structure active pour les apps séparées :

```text
apps/public-portal/src/app/features/
  public/

apps/admin-portal/src/app/features/
  admin/
  pos/

apps/platform-portal/src/app/features/
  platform/
```

`pos-portal` n'existe pas en V0. Les features POS restent sous `admin-portal/src/app/features/pos`
et doivent être lazy-loadées pour préparer une extraction future.

## Rôle des dossiers

| Dossier app / lib                           | Rôle                                                                                 |
| ------------------------------------------- | ------------------------------------------------------------------------------------ |
| `apps/public-portal/src/app/features/public` | Pages publiques : home, résultats publics, règles, contact, aide.                    |
| `apps/admin-portal/src/app/features/admin`   | Espace tenant admin.                                                                 |
| `apps/admin-portal/src/app/features/pos`     | Espace POS seller-terminal V0, lazy-loadé et extractible plus tard.                  |
| `apps/platform-portal/src/app/features/platform` | Espace superadmin / plateforme.                                                  |
| `libs/core/auth`                             | Login partagé, forgot password, guards, access/entitlements.                         |
| `libs/core/i18n`                             | Runtime i18n partagé.                                                                |
| `libs/web/shell`                             | Shells publics/privés/platform réutilisables.                                        |
| `libs/web/sandbox`                           | Pages/outils de développement : theme sandbox, debug UI, playground.                 |

---

# 2. Règle `core`, `shared`, `features`

## `core/`

`core` contient l’infrastructure globale de l’application.

Accepté dans `core` :

```text
auth
http
runtime
i18n
guards
interceptors
initializers
global stores
app bootstrap
```

Dans les nouvelles apps, éviter de recréer durablement `core/auth` ou `core/i18n` si la logique est
réutilisable. Utiliser les libs :

```text
libs/core/auth
libs/core/i18n
```

`apps/<portal>/src/app/core` reste acceptable pour le wiring propre à l'app : bootstrap, providers,
configuration de routes, initializers spécifiques.

À éviter dans `core` :

```text
pages routées
composants métier
tenant-list
tenant-detail
seller-terminal-list
workflows métier complets
```

---

## `shared/`

`shared` contient des briques UI ou helpers réutilisables.

Accepté dans `shared` :

```text
feedback components
layout helpers
small UI helpers
pipes
directives
copy helpers
format helpers
```

À éviter dans `shared` :

```text
services API métier
stores métier
pages routées
workflows métier
feature-specific services
```

---

## `features/`

`features` contient les surfaces produit et workflows visibles par l’utilisateur.

Exemples :

```text
apps/platform-portal/src/app/features/platform/tenants
apps/admin-portal/src/app/features/admin/setup
apps/admin-portal/src/app/features/pos/sale
apps/public-portal/src/app/features/public/home
```

---

# 3. Structure cible d’une feature significative

Toute feature importante doit suivre cette structure :

```text
feature-name/
  list/
  new/
  edit/
  components/
  data-access/
  feature-name.routes.ts
```

| Dossier                 | Contenu                                                                        |
| ----------------------- | ------------------------------------------------------------------------------ |
| `list/`, `new`, `edit/` | Page/flow routé quand la feature suit un CRUD clair.                           |
| `pages/`                | Accepté pour les features legacy ou hétérogènes. Une page = une route Angular. |
| `components/`           | Composants UI propres à la feature. Pas de routing.                            |
| `data-access/`          | API services, models, stores locaux. Pas de composants UI.                     |
| `*.routes.ts`           | Routes internes de la feature.                                                 |

## Cas particulier `features/public`

`features/public` suit la même règle pour les pages publiques routées :

```text
features/public/
  <public-feature>/
    pages/        # cible quand la feature grossit
    components/
    data-access/
    <public-feature>.routes.ts
```

Pendant la migration, les anciennes pages publiques peuvent encore être à plat dans
`features/public/<page>/`, mais toute nouvelle feature publique significative doit converger vers
`pages/components/data-access`.

Les widgets PageModel rendus sur les pages publiques ne vont pas dans `features/public/components`.
Ils restent dans `libs/widgets`, car ils appartiennent au registre PageModel et doivent rester
portables entre pages/surfaces.

---

# 4. Règle `.ts / .html / .scss`

Les pages de taille significative utilisent des fichiers séparés :

```text
xxx.page.ts
xxx.page.html
xxx.page.scss
```

Inline autorisé seulement pour des composants UI très simples :

```text
template très court
pas ou peu de SCSS
moins de ~50 lignes de template
pas de responsive complexe
```

Pour les grands écrans admin, CRUD, détail, onboarding, setup :

```text
.ts + .html + .scss séparés
```

Chaque page routée nouvelle doit vivre dans son propre dossier :

```text
seller-terminals/
  list/
    seller-terminal-list.page.ts
    seller-terminal-list.page.html
    seller-terminal-list.page.scss
    seller-terminal-list.store.ts
```

Les stores page-specific restent avec la page. Les clients métier restent dans `data-access/`.

---

# 5. Séparer une page en composants

Quand une page devient grosse, la page routée doit rester un **orchestrateur**. Les blocs UI internes doivent aller dans `components/`.

## Règle

```text
pages/
= composants routés uniquement

components/
= sous-composants UI utilisés par les pages de la feature
```

Une page ne devrait pas contenir toute la UI si elle commence à mélanger :

```text
plusieurs cards
plusieurs sections de formulaire
une table
des filtres
un right rail
des états loading/error/empty
des actions complexes
du responsive important
```

Dans ce cas, on extrait des composants spécifiques à la feature.

---

## Ce qui reste dans la page

La page garde l’orchestration :

```text
lecture des route params / query params
chargement API
signals loading / error / ready
submit principal
navigation
coordination des composants
actions globales
```

Exemple :

```text
platform-tenant-detail.page.ts
```

peut gérer :

```text
tenantId depuis la route
loadTenant()
refresh()
openAdminList()
archiveTenant()
```

---

## Ce qui va dans `components/`

Les composants de feature gèrent une partie UI précise :

```text
afficher un résumé
afficher une section de formulaire
afficher une table
afficher une checklist
afficher un right rail
émettre un événement au parent
```

Exemple :

```text
tenant-filter-bar.component
```

Inputs :

```text
q
status
type
```

Outputs :

```text
filtersChange
reset
```

Il ne doit pas appeler directement l’API.

---

## Ce qui ne va pas dans `components/`

Éviter dans les composants UI :

```text
appels API métier
lecture directe du router si non nécessaire
navigation principale
stores globaux
décisions de permission complexes
```

Ces responsabilités restent dans la page ou dans `data-access/`.

---

## Quand extraire un composant

Extraire quand :

```text
le template de la page dépasse ~150 lignes
une section devient réutilisée
une table ou un filtre grossit
le right rail devient complexe
le SCSS devient difficile à lire
la page mélange trop d’états et de sections
```

Ne pas extraire trop tôt si le composant n’est utilisé qu’une fois et reste simple.

---

# 6. Convention de nommage

Le nommage (pages, components, dialogs, data-access, préfixes de surface) est défini dans
[`naming.md`](./naming.md) — ne pas le dupliquer ici.

Rappel minimal : un composant utilisé par une seule feature reste dans
`<feature>/components/` ; il ne monte vers `libs/ui/components` (ou `libs/ui/console` pour le
scaffolding console) que quand il devient réellement générique et multi-features.

---

# 7. `private/shared/` et équivalents multi-app

`private/shared/` contient uniquement des composants/helpers réutilisables dans l’espace connecté.

Exemples :

```text
private/shared/
  admin-ui/
    admin-page-shell
    admin-section-card
    admin-detail-layout
    identity-card
    next-steps-card
    empty-state
```

Dans les apps séparées, éviter de recréer un gros `shared/` par réflexe. Préférer :

```text
apps/admin-portal/src/app/features/admin/<feature>/components
apps/platform-portal/src/app/features/platform/<feature>/components
libs/ui/components                       # si vraiment global et stateless
libs/web/shell                           # si shell/runtime web
libs/web/errors                          # si erreur web
libs/core/auth                           # si auth
libs/core/i18n                           # si i18n
```

Règle stricte :

```text
pas de services API métier
pas de stores métier
pas de pages routées
pas de workflow produit complet
```

Un composant qui n’est utilisé que par une seule feature reste dans :

```text
feature-name/components/
```

---

# 8. Shells

`private/shell/` contient les éléments structurels de l’espace connecté.

Exemples :

```text
private/shell/
  private-shell.component.ts/.html/.scss
  private-topbar.component.ts/.html/.scss
  private-sidenav.component.ts/.html/.scss
  pages/
    page-model-host/
      private-page-model-host.page.ts/.html/.scss
```

Dans les nouvelles apps, les primitives réutilisables de shell vivent dans :

```text
libs/web/shell
```

La composition de route, les providers et le bootstrap restent dans :

```text
apps/admin-portal/src/app
apps/platform-portal/src/app
apps/public-portal/src/app
```

## PageModel host

Le composant qui affiche :

```text
PrivateShellService.page$
<tch-page-model>
```

n’est pas un vrai dashboard métier.

Il doit être nommé et placé comme host runtime :

```text
features/private/shell/pages/page-model-host/
  private-page-model-host.page.ts
  private-page-model-host.page.html
  private-page-model-host.page.scss
```

Nom recommandé :

```text
PrivatePageModelHostPage
```

À éviter :

```text
PrivateDashboardPage
```

car ce composant ne contient pas de logique métier dashboard.

Les vrais dashboards métier iront plus tard dans :

```text
features/private/platform/dashboard/
features/private/admin/dashboard/
features/private/seller-terminal/dashboard/
```

---

# 9. Exemple : `platform/tenants/`

## Structure

```text
platform/
  platform.routes.ts
  tenants/
    pages/
      list/
        platform-tenants.page.ts
        platform-tenants.page.html
        platform-tenants.page.scss

      onboarding/
        platform-tenant-provisioning.page.ts
        platform-tenant-provisioning.page.html
        platform-tenant-provisioning.page.scss

      detail/
        platform-tenant-detail.page.ts
        platform-tenant-detail.page.html
        platform-tenant-detail.page.scss

      admins/
        platform-tenant-admins.page.ts
        platform-tenant-admins.page.html
        platform-tenant-admins.page.scss

      admin-create/
        platform-tenant-admin-create.page.ts
        platform-tenant-admin-create.page.html
        platform-tenant-admin-create.page.scss

    components/
      tenant-filter-bar/
        tenant-filter-bar.component.ts
        tenant-filter-bar.component.html
        tenant-filter-bar.component.scss

      tenant-table/
        tenant-table.component.ts
        tenant-table.component.html
        tenant-table.component.scss

      tenant-detail-overview/
        tenant-detail-overview.component.ts
        tenant-detail-overview.component.html
        tenant-detail-overview.component.scss

      tenant-commercial-summary/
        tenant-commercial-summary.component.ts
        tenant-commercial-summary.component.html
        tenant-commercial-summary.component.scss

      tenant-readiness-card/
        tenant-readiness-card.component.ts
        tenant-readiness-card.component.html
        tenant-readiness-card.component.scss

    data-access/
      platform-tenants-api.service.ts
      platform-provisioning-api.service.ts
      platform-admin-api.service.ts
      platform-tenants.models.ts

    platform-tenants.routes.ts

  pages/
    ← pages non encore découpées : ops, access, audit, placeholders, etc.
```

## `platform.routes.ts`

`platform.routes.ts` doit devenir un fichier d’assemblage.

Il délègue `tenants` via `loadChildren` :

```ts
{
  path: 'tenants',
  loadChildren: () =>
    import('./tenants/platform-tenants.routes').then(m => m.platformTenantRoutes),
}
```

## `platform-tenants.routes.ts`

```ts
import { Route } from '@angular/router';

export const platformTenantRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/list/platform-tenants.page').then(m => m.PlatformTenantsPage),
  },
  {
    path: 'onboarding',
    loadComponent: () =>
      import('./pages/onboarding/platform-tenant-provisioning.page').then(
        m => m.PlatformTenantProvisioningPage,
      ),
  },
  {
    path: ':tenantId/admins/new',
    loadComponent: () =>
      import('./pages/admin-create/platform-tenant-admin-create.page').then(
        m => m.PlatformTenantAdminCreatePage,
      ),
  },
  {
    path: ':tenantId/admins',
    loadComponent: () =>
      import('./pages/admins/platform-tenant-admins.page').then(m => m.PlatformTenantAdminsPage),
  },
  {
    path: ':tenantId',
    loadComponent: () =>
      import('./pages/detail/platform-tenant-detail.page').then(m => m.PlatformTenantDetailPage),
  },

  // Legacy
  { path: 'new', redirectTo: 'onboarding', pathMatch: 'full' },
];
```

---

# 10. Routes legacy

Lors d’un déplacement de routes, conserver les redirects legacy dans le fichier d’assemblage concerné.

Exemple dans `platform.routes.ts` :

```ts
{ path: 'tenant-provisioning', redirectTo: 'tenants/onboarding', pathMatch: 'full' },
{ path: 'tenant-onboarding', redirectTo: 'tenants/onboarding', pathMatch: 'full' },
```

Exemple dans `platform-tenants.routes.ts` :

```ts
{ path: 'new', redirectTo: 'onboarding', pathMatch: 'full' },
```

Objectif :

```text
ne pas casser les anciens liens
ne pas casser les bookmarks
ne pas casser la navigation existante
```

---

# 11. Exemple : `account/`

`account` est une feature privée transversale pour le compte utilisateur connecté.

Elle remplace l’idée d’un dossier `profile/` isolé.

## Structure

```text
account/
  pages/
    activation/
      account-activation.page.ts
      account-activation.page.html
      account-activation.page.scss

    profile/
      account-profile.page.ts
      account-profile.page.html
      account-profile.page.scss

    security/
      account-security.page.ts
      account-security.page.html
      account-security.page.scss

    preferences/
      account-preferences.page.ts
      account-preferences.page.html
      account-preferences.page.scss

  components/

  data-access/
    account-api.service.ts
    account.models.ts

  account.routes.ts
```

## Routes possibles

```text
/app/account/activation
/app/account/profile
/app/account/security
/app/account/preferences
```

Redirect optionnel :

```text
/app/profile → /app/account/profile
```

## Placement

À utiliser pour :

```text
activation du compte
complétion du profil
sécurité du compte
changement mot de passe
préférences utilisateur
```

À ne pas mettre dans :

```text
shared
core
platform
admin
```

---

# 12. Exemple : `admin/setup/`

`admin/setup` est le regroupement temporaire des routes de configuration tenant côté `TENANT_ADMIN`.

## Structure

```text
admin/
  admin.routes.ts

  setup/
    pages/
      complete-config/
        admin-complete-tenant-config.page.ts
        admin-complete-tenant-config.page.html
        admin-complete-tenant-config.page.scss

      settings/
        admin-settings.page.ts
        admin-settings.page.html
        admin-settings.page.scss

      config/
        admin-config.page.ts
        admin-config.page.html
        admin-config.page.scss

      runtime/
        admin-runtime.page.ts
        admin-runtime.page.html
        admin-runtime.page.scss

    components/
      tenant-config-form/
        tenant-config-form.component.ts
        tenant-config-form.component.html
        tenant-config-form.component.scss

      receipt-config-section/
        receipt-config-section.component.ts
        receipt-config-section.component.html
        receipt-config-section.component.scss

      business-calendar-section/
        business-calendar-section.component.ts
        business-calendar-section.component.html
        business-calendar-section.component.scss

      setup-readiness-card/
        setup-readiness-card.component.ts
        setup-readiness-card.component.html
        setup-readiness-card.component.scss

    data-access/
      tenant-config-api.service.ts
      runtime-api.service.ts
      admin-setup.models.ts

    admin-setup.routes.ts

  pages/
    ← reste des pages admin non encore découpées
```

## Route canonique

```text
/admin/setup
```

## Redirects legacy dans `admin.routes.ts`

```text
complete-config → setup
onboarding → setup
appearance → settings
more/space → setup
```

Quand une feature admin grossit, elle suit le même pattern.

Exemples futurs :

```text
admin/seller-terminals/
admin/reports/
admin/draw-results/
admin/tickets/
```

---

# 13. Autres exemples réels

Les « exemples futurs » historiques (onboarding, seller-terminals) sont devenus du code réel —
la référence est le code, pas ce document :

```text
apps/admin-portal/src/app/features/admin/seller-terminals/   (pages/list, pages/new, pages/activation)
apps/admin-portal/src/app/features/admin/setup/
apps/platform-portal/src/app/features/platform/tenants/
```

Pour le contenu de ces écrans, voir [`feature-playbook.md`](./feature-playbook.md).

---

# 15. Découpage progressif de `platform.routes.ts`

`platform.routes.ts` ne doit pas continuer à grossir.

Structure cible progressive :

```text
platform/
  platform.routes.ts

  tenants/
    platform-tenants.routes.ts

  operations/
    platform-operations.routes.ts

  references/
    platform-references.routes.ts

  communication/
    platform-communication.routes.ts

  access-rights/
    platform-access-rights.routes.ts

  reports/
    platform-reports.routes.ts
```

Mais on ne migre pas tout d’un coup.

Priorité :

```text
1. tenants
2. operations quand on travaille sur ops
3. references quand on travaille sur référentiels
4. communication quand on travaille sur notifications/news/contact
5. access-rights quand on travaille sur permissions/rôles
6. reports quand on travaille sur rapports
```

---

# 16. Découpage progressif de `admin.routes.ts`

`admin.routes.ts` est actuellement très gros.

Structure cible progressive :

```text
admin/
  admin.routes.ts

  setup/
    admin-setup.routes.ts

  seller-terminals/
    seller-terminals.routes.ts

  settings/
    admin-settings.routes.ts

  controls/
    admin-controls.routes.ts

  reports/
    admin-reports.routes.ts

  tickets/
    admin-tickets.routes.ts
```

On découpe seulement quand on travaille activement sur la feature.

---

# 17. Note SCSS — chemins relatifs vers libs

Les fichiers `.page.scss` qui utilisent `@use` vers `libs/ui/styles` ont un chemin relatif dépendant de leur profondeur dans :

```text
apps/<portal>/src/app/
```

Quand une page est déplacée plus profondément, ajouter un `../` supplémentaire par niveau de dossier ajouté.

Exemple :

```scss
@use '../../../../../../libs/ui/styles/...' as...;
```

À vérifier après chaque déplacement.

---

# 18. Ordre de découpage

Le découpage se fait feature par feature, **quand on travaille activement dessus**.
La roadmap vivante (chantiers C1–C6, priorités) est dans
[`../ARCHITECTURE.md`](../ARCHITECTURE.md) §2 — ne pas la dupliquer ici.

---

# 19. Règle finale

Ne pas tout migrer d’un coup.

Appliquer cette convention :

```text
aux nouvelles features
aux features actives
aux pages qui deviennent trop grosses
```

Ne pas déplacer une feature stable uniquement pour “faire propre” si elle n’est pas en cours de développement.
