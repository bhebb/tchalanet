# Convention — Structure des features Angular `tch-portal`

## Objectif

Cette convention définit l’organisation des features Angular dans l’application Nx :

```text
apps/tch-portal/src/app
```

L’objectif est de garder une structure claire, maintenable et progressive, sans tout migrer d’un coup.

---

# 1. Arborescence générale `features/`

Structure cible :

```text
features/
  auth/
  public/
  private/
    shell/
    shared/
    account/
    platform/
    admin/
    seller-terminal/
  dev/
```

## Rôle des dossiers

| Dossier                    | Rôle                                                                                                          |
| -------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `auth/`                    | Écrans d’authentification avant connexion : login, forgot password, reset password.                           |
| `public/`                  | Pages publiques : home, résultats publics, règles, contact, aide.                                             |
| `private/`                 | Tout l’espace connecté.                                                                                       |
| `private/shell/`           | Shell privé : topbar, sidenav, page-model host, layout connecté.                                              |
| `private/shared/`          | Composants privés réutilisables entre `platform`, `admin`, `seller-terminal`. Jamais de logique métier forte. |
| `private/account/`         | Compte utilisateur connecté : activation, profil, sécurité, préférences.                                      |
| `private/platform/`        | Espace superadmin / plateforme.                                                                               |
| `private/admin/`           | Espace tenant admin.                                                                                          |
| `private/seller-terminal/` | Espace POS seller-terminal.                                                                                   |
| `dev/`                     | Pages de développement : theme sandbox, debug UI, playground.                                                 |

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
features/private/platform/tenants
features/private/admin/setup
features/private/account/activation
features/public/home
features/auth/login
```

---

# 3. Structure cible d’une feature significative

Toute feature importante doit suivre cette structure :

```text
feature-name/
  pages/
  components/
  data-access/
  feature-name.routes.ts
```

| Dossier        | Contenu                                                     |
| -------------- | ----------------------------------------------------------- |
| `pages/`       | Composants routés uniquement. Une page = une route Angular. |
| `components/`  | Composants UI propres à la feature. Pas de routing.         |
| `data-access/` | API services, models, stores locaux. Pas de composants UI.  |
| `*.routes.ts`  | Routes internes de la feature.                              |

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

## Pages

```text
pages/
  xxx/
    xxx.page.ts
    xxx.page.html
    xxx.page.scss
```

## Components

```text
components/
  xxx/
    xxx.component.ts
    xxx.component.html
    xxx.component.scss
```

## Data access

```text
data-access/
  xxx-api.service.ts
  xxx.models.ts
  xxx.store.ts
```

Les composants dans `components/` sont spécifiques à leur feature.

S’ils deviennent vraiment génériques et réutilisés dans plusieurs features, ils peuvent être déplacés plus tard vers :

```text
features/private/shared/
```

ou :

```text
libs/ui/components/
```

selon leur niveau de généricité.

---

# 7. `private/shared/`

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

# 8. `private/shell/`

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
      import('./pages/admins/platform-tenant-admins.page').then(
        m => m.PlatformTenantAdminsPage,
      ),
  },
  {
    path: ':tenantId',
    loadComponent: () =>
      import('./pages/detail/platform-tenant-detail.page').then(
        m => m.PlatformTenantDetailPage,
      ),
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
/app/admin/setup
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

# 13. Exemple futur : `admin/onboarding/`

Si l’on crée une vraie page `AdminOnboardingPage` complète, elle doit suivre cette structure :

```text
admin/
  onboarding/
    pages/
      overview/
        admin-onboarding.page.ts
        admin-onboarding.page.html
        admin-onboarding.page.scss

    components/
      onboarding-checklist/
        onboarding-checklist.component.ts
        onboarding-checklist.component.html
        onboarding-checklist.component.scss

      onboarding-step-card/
        onboarding-step-card.component.ts
        onboarding-step-card.component.html
        onboarding-step-card.component.scss

      onboarding-readiness-card/
        onboarding-readiness-card.component.ts
        onboarding-readiness-card.component.html
        onboarding-readiness-card.component.scss

    data-access/
      admin-onboarding-api.service.ts
      admin-onboarding.models.ts
      admin-onboarding.store.ts

    admin-onboarding.routes.ts
```

Mais pour V0, `admin/setup` peut rester la route canonique si l’objectif est surtout de compléter la configuration tenant.

---

# 14. Exemple futur : `admin/seller-terminals/`

```text
admin/
  seller-terminals/
    pages/
      list/
        admin-seller-terminals.page.ts
        admin-seller-terminals.page.html
        admin-seller-terminals.page.scss

      create/
        admin-seller-terminal-create.page.ts
        admin-seller-terminal-create.page.html
        admin-seller-terminal-create.page.scss

      detail/
        admin-seller-terminal-detail.page.ts
        admin-seller-terminal-detail.page.html
        admin-seller-terminal-detail.page.scss

    components/
      seller-terminal-filter-bar/
      seller-terminal-table/
      seller-terminal-status-card/
      seller-terminal-pin-reset-dialog/

    data-access/
      seller-terminals-api.service.ts
      seller-terminals.models.ts

    seller-terminals.routes.ts
```

Route canonique :

```text
/app/admin/seller-terminals
```

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
apps/tch-portal/src/app/
```

Quand une page est déplacée plus profondément, ajouter un `../` supplémentaire par niveau de dossier ajouté.

Exemple :

```scss
@use '../../../../../../libs/ui/styles/...' as ...;
```

À vérifier après chaque déplacement.

---

# 18. Ordre de découpage recommandé

## Déjà fait / à valider

```text
1. platform/tenants — fait ou en cours
2. account — à structurer si activation/profile grossissent
3. admin/setup — temporaire, utile pour config tenant
```

## À faire progressivement

```text
4. admin/onboarding — seulement si une vraie page onboarding admin complète est créée
5. admin/seller-terminals — au moment du CRUD seller-terminal
6. platform/operations — au moment de travailler sur ops
7. platform/references — au moment de travailler sur référentiels
8. platform/communication — au moment de travailler notifications/news/contact
9. platform/access-rights — au moment de travailler permissions/rôles
10. platform/reports — au moment de travailler rapports
```

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

La priorité produit reste :

```text
1. tenant provisioning/list/detail
2. account activation
3. admin setup/config
4. seller-terminals admin
5. seller-terminal POS
```
