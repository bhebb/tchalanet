# Tchalanet Web — Architecture Frontend TODO

## 1. Objectif

Stabiliser l’architecture Angular Nx de `tchalanet-web` avant de développer massivement les pages public, admin, cashier/POS et superadmin.

Le but est de réduire le nombre de libs, clarifier les responsabilités, éviter les abstractions prématurées et poser une base cohérente avec l’architecture backend Tchalanet.

---

## 2. Décision de base

État actif après la fondation UI :

```text
libs/
  api/
  shared-config/
  ui/
    components/
    styles/
    theme/
```

Architecture cible par extraction :

```text
libs/
  api/
  shared-auth/
  shared-i18n/
  shared-config/
  ui/
  page-model/
  widgets/
  web/
```

Une lib Nx doit exister seulement si elle porte une frontière claire, stable et utile. Ne jamais
créer une coquille vide pour faire correspondre le workspace au diagramme cible.

---

## 3. Rôle des libs

### `libs/api`

Responsabilité :

* contrats backend/frontend
* modèles transverses
* clients HTTP
* interceptors

Contenu cible :

```text
api/
  contracts/
    api-response.model.ts
    problem-detail.model.ts
    api-notice.model.ts
    service-status.model.ts
    action-item.model.ts
    navigation-destination.model.ts
    page-model.model.ts
    widget.model.ts
    session.model.ts

  client/
    api-client.ts
    page.api.ts
    me.api.ts
    i18n.api.ts

  interceptors/
    api-base.interceptor.ts
    auth.interceptor.ts
    meta-headers.interceptor.ts
```

Règle :

* `api` ne contient pas de composants UI.
* `api` ne contient pas de logique d’écran.
* `api` définit le langage commun entre backend, Angular et Flutter.

---

### `libs/shared-auth`

Responsabilité :

* OIDC / Keycloak
* login
* callback
* guards
* refresh token
* secure storage
* wiring auth runtime

Contenu cible :

```text
shared-auth/
  components/
    auth-login.component.ts
    auth-callback.component.ts

  services/
    auth.service.ts
    local-storage-security.service.ts

  guards/
    auth.guard.ts
    role.guard.ts

  providers/
    provide-tch-auth.ts
```

Règle :

* aucune logique métier Tchalanet critique ici.
* cette lib protège l’accès et expose l’état d’authentification.

---

### `libs/shared-i18n`

Responsabilité :

* traduction Angular
* merge assets + backend overrides
* langue courante
* language switcher
* state i18n si nécessaire

Contenu cible :

```text
shared-i18n/
  loader/
  services/
  state/
  components/
    language-switcher.component.ts
```

---

### `libs/shared-config`

Responsabilité :

* environment
* feature flags
* settings runtime
* constantes globales non métier

Contenu cible :

```text
shared-config/
  environment/
  feature-flags/
  constants/
  settings/
```

---

### `libs/ui`

Responsabilité :

* design system Angular
* composants visuels réutilisables
* thème
* layout générique
* feedback
* actions
* formulaires
* status badges

Contenu actif :

```text
ui/
  theme/
  styles/
  components/
```

Composants prioritaires :

```text
feedback/
  TchNotice
  TchNoticeList
  TchErrorPanel
  TchToast
  TchServiceStatusBanner

actions/
  TchActionButton
  TchActionList
  TchConfirmDialog
  TchEmptyState

status/
  TchStatusBadge
  TchSeverityBadge

layout/
  PageHeader
  Skeleton
  LoadingOverlay
  Grid primitives

forms/
  AmountInput
  PhoneInput
  SearchInput
  DateRangePicker
```

Règle :

* pas de HttpClient.
* pas de store ni de service applicatif injecté.
* composants présentational/stateless autant que possible.
* reçoit des inputs, émet des outputs.

---

### `libs/page-model`

Responsabilité :

* moteur PageModel frontend
* chargement PageModel
* state PageModel
* services et state PageModel
* rendu layout/widgets

Contenu cible :

```text
page-model/
  model/
  state/
  services/
  rendering/
    page-renderer.component.ts
    grid-layout.component.ts
    widget-renderer.component.ts
```

Règle :

* `page-model` sait interpréter le PageModel backend.
* `page-model` ne contient pas les widgets concrets.
* `page-model` appelle le registry de widgets.

---

### `libs/widgets`

Responsabilité :

* registry de widgets
* widgets dynamiques affichés par PageModel
* widgets public/private/cashier/admin

Contenu cible :

```text
widgets/
  registry/
    widget.token.ts
    widget.registry.ts
    provide-builtin-widgets.ts

  common/
  public/
  private/
  cashier/
  tenant-admin/
  platform-admin/
```

Règle :

* un widget reçoit des props.
* un widget ne doit pas devenir un mini-container sauf exception documentée.
* un widget peut utiliser des composants `ui`.

---

### `libs/web`

Responsabilité :

* routes Angular
* pages routées
* shells
* containers de surfaces
* assemblage écran

Contenu cible :

```text
web/
  public/
    shell/
    pages/

  private/
    shell/
    pages/

  cashier/
    pages/
    containers/
    components/

  tenant-admin/
    pages/
    containers/

  platform-admin/
    pages/
    containers/
```

Règle :

* les pages vivent ici.
* les containers spécifiques à une page/surface vivent ici.
* pas de `HttpClient` direct dans les pages; elles orchestrent des services applicatifs/state dédiés.

---

## 4. Convention Page / Container / Component / Widget

### Règle officielle

```text
Route -> Page -> Container(s) -> Component(s)
```

### Page

* composant routé
* suffixe `*.page.ts`
* toutes les routes Angular pointent vers une Page
* responsable du layout principal d’écran
* peut injecter services applicatifs/store/router si nécessaire

### Container

* composant interne à une Page
* suffixe `*.container.ts`
* jamais routé directement
* peut injecter services applicatifs/store/router
* orchestre une sous-zone logique d’écran

### Component

* composant visuel
* suffixe `*.component.ts`
* reçoit `input()`
* émet `output()`
* pas de HttpClient
* pas de store/service applicatif sauf exception documentée
* stateless/presentational autant que possible

### Widget

* composant rendu dynamiquement par PageModel
* suffixe `*.widget.ts`
* reçoit des props
* enregistré dans `widgets/registry`

### Shell

* structure globale d’une surface
* suffixe `*.shell.ts`
* exemples : public shell, private shell, cashier shell

---

## 5. Migration depuis l’existant

Mapping prévu :

```text
libs/shared/api
  -> libs/api

libs/shared/types
  -> libs/api/contracts

libs/shared/auth
  -> libs/shared-auth

libs/shared/data-access/page
  -> libs/page-model/state

anciennes abstractions d'orchestration génériques
  -> décomposer vers les services/state propriétaires; ne pas recréer une couche générique

libs/shared/feature
  -> libs/shared-config/feature-flags

libs/web/public-pages
  -> libs/web/public

libs/web/private-pages
  -> libs/web/private

libs/web/feature-home-public
  -> libs/web/public/pages/home

libs/web/widgets
  -> libs/widgets
```

Migration progressive : ne pas tout déplacer en une seule fois.

---

## 6. Priorités de refonte

### Phase 0 — Backup

* faire un backup du workspace actuel
* créer une branche dédiée
* noter les imports publics actuels
* vérifier que l’app build avant refonte

### Phase 1 — Stabiliser les contrats (terminée pour le transport transverse)

* `libs/api` contient les contrats HTTP transverses, helpers et interceptors communs
* les clients spécifiques restent avec leur domaine propriétaire
* éviter `@tchl/types` trop vague

### Phase 2 — Stabiliser Auth / I18n / Config

* isoler auth dans `shared-auth`
* isoler i18n dans `shared-i18n`
* feature flags et settings runtime sont isolés dans `shared-config`
* l'environnement de build reste à extraire lorsqu'un contrat runtime concret le justifie

### Phase 3 — Stabiliser UI (terminée pour la fondation)

* `ui/theme` porte le runtime thème et les presets
* `ui/styles` porte les primitives SCSS
* `ui/components` porte les composants réutilisables
* ne pas recréer de libs parallèles `ui/feedback`, `ui/actions` ou `ui/layout`

### Phase 4 — Stabiliser PageModel

* créer `page-model` seulement avec le déplacement du renderer, contrats propres et tests
* déplacer puis décomposer les responsabilités PageModel existantes
* déplacer `PageModelApi`, le contrat PageModel, `PageModelComponent` et `WidgetHostComponent`
* clarifier la frontière avec `widgets`

### Phase 5 — Stabiliser Widgets

* créer `widgets` seulement avec le déplacement du registry et des widgets concrets
* déplacer widgets actuels
* remplacer `TchlLink` par `ActionItem` / `NavigationDestination`
* garder les widgets petits et props-driven

### Phase 6 — Stabiliser Web

* routes -> pages uniquement
* regrouper public/private/cashier/admin dans `web`
* déplacer les dashboards tenant admin, superadmin et cashier dans leurs surfaces `web`
* déplacer les shells public/private et pages publiques après stabilisation PageModel/widgets
* éviter les routes vers containers/components
* garder les pages simples et lisibles

---

## 7. Règles non négociables

* Toutes les routes pointent vers une `Page`.
* Aucun container n’est routé directement.
* Les composants UI ne font pas d’appel HTTP.
* Les composants UI ne dépendent pas de NgRx ni de services applicatifs.
* Les widgets reçoivent des props et sont rendus par PageModel.
* Les contrats backend/frontend vivent dans `api/contracts`.
* Les pages orchestrent des services applicatifs/state dédiés, sans appeler directement `HttpClient`.
* Pas de nouvelle lib sans frontière claire.

---

## 8. Definition of Done initiale

La base est considérée propre quand :

* les 8 libs cibles existent ou sont clairement planifiées
* `api/contracts` contient les modèles transverses
* auth/i18n/config ne sont plus mélangés avec des abstractions ou types génériques
* PageModel a sa propre lib
* widgets a son registry
* les routes principales pointent vers des pages
* `ui` contient au moins feedback/actions/status/theme/layout
* le build Angular passe
* les imports publics sont cohérents
