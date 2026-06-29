# TODO — web-apps-split-v1

## 1. Apps cibles

- [x] Créer/valider `apps/public-portal`.
- [x] Créer/valider `apps/admin-portal`.
- [x] Créer/valider `apps/platform-portal`.
- [x] Ne pas créer `apps/pos-portal` en V0.
- [x] Garder `pos-portal` comme extraction future si la vente POS grossit.
- [x] Permettre à `admin-portal` d'être build/deploy seul, sans `public-portal` ni `platform-portal`.
- [x] Permettre à `platform-portal` d'être build/deploy seul.
- [x] Permettre à `public-portal` d'être build/deploy seul.

## 2. Libs cibles

### 2.1 `libs/api`

- [x] Garder `libs/api` comme lib technique HTTP + contrats transverses.
- [x] Ajouter/valider le dossier `libs/api/src/lib/contracts`.
- [x] Ajouter/valider le dossier `libs/api/src/lib/http`.
- [x] Ajouter/valider le dossier `libs/api/src/lib/backend-client`.
- [x] Ne pas créer `libs/api/clients` comme registre central de tous les clients métier.
- [x] Placer `ApiResponse<T>`, `ProblemDetail`, `TchPage<T>`, `ApiNotice`, `ServiceStatus`,
  `ActionItem`, `NavigationDestination` dans `libs/api/src/lib/contracts`.
- [x] Placer interceptors, constantes headers, helpers query params/pagination et normalisation
  `ProblemDetail` dans `libs/api/src/lib/http`.
- [x] Placer `BackendClient`, unwrap `ApiResponse<T>`, mapping central erreurs, pagination et
  options headers dans `libs/api/src/lib/backend-client`.

### 2.2 `libs/core`

- [x] Créer/valider `libs/core/auth`.
- [x] Créer/valider `libs/core/i18n`.
- [x] Placer `I18nFacade`, store/effects i18n, loader/merger ngx-translate et language switcher
  runtime dans `libs/core/i18n`.
- [x] Placer login partagé, session store, guards, permissions, login/logout et état auth dans
  `libs/core/auth`.
- [x] Déplacer `AuthSessionService`, guards, bearer interceptor, provider Firebase, bootstrap privé
  et support access runtime dans `libs/core/auth`.
- [x] Vérifier que `libs/core/auth` ne contient aucun client métier admin/platform/POS ni registre
  central d'endpoints backend.

### 2.3 `libs/ui`

- [x] Créer/valider `libs/ui/theme`.
- [x] Créer/valider `libs/ui/styles`.
- [x] Créer/valider `libs/ui/components`.
- [ ] Vérifier que `libs/ui/theme` contient runtime theme, `ThemeStore`, `ThemeDomApplier`, preset
  registry et mapping backend tokens vers `--tch-*`.
- [ ] Vérifier que `libs/ui/styles` contient SCSS primitives, breakpoints, mixins, typography et
  Material overrides globaux.
- [ ] Vérifier que `libs/ui/components` contient des composants UI réutilisables/stateless sans appel
  HTTP direct.

### 2.4 `libs/web`

- [x] Créer/valider `libs/web/errors`.
- [x] Créer/valider `libs/web/shell`.
- [x] Ne pas créer `libs/web/navigation` en V0; garder navigation dans `libs/web/shell` tant qu'elle
  appartient au shell.
- [x] Ne pas créer une lib séparée `libs/shell`.
- [x] Déplacer les primitives publiques réutilisables `PublicHeader`/`PublicFooter`/`PublicBottomNav`
  dans `libs/web/shell`.
- [x] Déplacer `PublicShellService`, `PublicFallbackBundleService` et le modèle bootstrap public dans
  `libs/web/shell`.
- [x] Déplacer le feedback shell réutilisable (`ShellFeedbackStore`, outlet, banner, copy support
  reference) dans `libs/web/shell`.
- [x] Déplacer les presets/modèles de navigation shell privés dans `libs/web/shell`.
- [x] Extraire le layout visuel du shell privé dans `libs/web/shell`, avec projection pour les
  utilitaires et le contenu spécifiques à l'app.
- [x] Extraire le layout visuel du shell public dans `libs/web/shell`, avec projection du contenu
  routé et wiring i18n/login conservé dans l'app.
- [ ] Placer public shell, private/admin shell, platform shell et layout shell utilities dans
  `libs/web/shell`.
- [x] Placer page error, section error, API error presenter et mapping UI depuis `ProblemDetail`
  dans `libs/web/errors`.

### 2.5 Autres libs

- [x] Valider `libs/page-model` sans le déplacer si la lib est déjà bien placée.
- [x] Valider `libs/shared-config` sans le déplacer si la lib est déjà bien placée.
- [ ] Vérifier que `libs/page-model` contient contrats runtime PageModel, renderer, widget host
  abstrait et helpers de rendu.
- [ ] Vérifier que `libs/page-model` ne contient aucun shell, thème runtime ou client métier.
- [ ] Vérifier que `libs/shared-config` contient runtime config, feature flags et config
  proxy/app metadata si nécessaire.

## 3. Structure standard des features

- [ ] Organiser les features par surface puis feature : `features/admin`, `features/platform`,
  `features/public`, `features/pos`.
- [ ] Organiser chaque feature par page/flow avec `list`, `new`, `edit`, `components`,
  `data-access` selon besoin réel.
- [ ] Mettre chaque page routée dans son propre dossier.
- [ ] Séparer chaque page en `*.page.ts`, `*.page.html`, `*.page.scss`.
- [ ] Mettre `*.page.store.ts` avec la page quand la page a de l'état.
- [ ] Mettre les composants page-only dans `<page-folder>/components/`.
- [ ] Mettre les composants feature-shared dans `<feature>/components/`.
- [ ] Mettre les composants globaux dans `libs/ui/components/`.
- [ ] Mettre les clients API métier dans `<feature>/data-access/`.
- [ ] Mettre les types/mappers/query builders API de feature dans `<feature>/data-access/`.
- [ ] Garder les stores page-specific avec leur page, pas dans `data-access/`.
- [ ] Ne pas créer de dossier `shared` dans une feature sans usage réel.

## 4. Exemple cible — seller terminals

- [ ] Aligner `features/admin/seller-terminals/list` avec page, html, scss, store et composants
  table/filter/empty-state.
- [ ] Aligner `features/admin/seller-terminals/new` avec page, html, scss, store et form component.
- [ ] Prévoir `features/admin/seller-terminals/edit` avec page, html, scss, store et edit form.
- [ ] Garder status badge et pin reset dialog dans `features/admin/seller-terminals/components/`
  tant qu'ils sont feature-shared.
- [ ] Garder `seller-terminals-api.service.ts`, types, mappers et query params dans
  `features/admin/seller-terminals/data-access/`.

## 5. Règle d'extraction des composants

- [ ] Extraire si `*.page.ts`, `*.page.html` ou `*.page.scss` dépasse environ 100 lignes.
- [ ] Extraire en priorité table, filter bar, form, summary card, dialog, empty state, error state,
  toolbar locale et section complexe.
- [ ] Garder un composant extrait dans `<page-folder>/components/` s'il est page-only.
- [ ] Promouvoir vers `<feature>/components/` seulement si plusieurs pages de la feature l'utilisent.
- [ ] Promouvoir vers `libs/ui/components/` seulement si réutilisable hors feature.
- [ ] Ne pas promouvoir trop tôt vers shared/global.

## 6. Placement des clients API

- [ ] Garder `libs/api` pour contrats + HTTP technique + `BackendClient` générique.
- [ ] Garder les clients API métier + types locaux + mappers + query builders dans
  `features/*/*/data-access`.
- [ ] Ne pas mettre les clients métier dans `libs/api`.
- [ ] Ne pas créer de registre central `libs/api/clients`.
- [ ] Chaque slice possède son propre `XxxApiService` dans `data-access/`.
- [ ] Ne sortir un client métier de sa slice que lorsqu'il a au moins deux consommateurs réels.
- [ ] Interdire `HttpClient` direct dans les composants UI.
- [ ] Interdire l'injection directe de clients API métier dans les composants UI.
- [ ] Faire orchestrer les appels API par les pages/stores via `data-access`.

## 7. Admin portal

- [x] Brancher `admin-portal` sur le login partagé depuis `libs/core/auth`.
- [ ] Mettre les routes tenant admin dans `admin-portal`.
- [x] Ajouter une route lazy pour la vente POS admin.
- [x] Garder la vente POS lazy-loaded dans `admin-portal` en V0.
- [ ] Masquer/désactiver la vente pour les admins qui ne veulent pas cette fonction.
- [ ] Garder les features POS dans `features/pos/home` et `features/pos/sale`.
- [ ] Préparer l'extraction future vers `pos-portal` sans refonte majeure.

## 8. Platform portal

- [x] Brancher `platform-portal` sur le login partagé depuis `libs/core/auth`.
- [ ] Mettre les routes superadmin/platform dans `platform-portal`.
- [ ] Garder les features platform sous `features/platform`.
- [ ] Ne pas mélanger les écrans platform avec les écrans tenant admin.
- [ ] Garder les opérations platform séparées des features admin tenant.

## 9. Public portal

- [x] Mettre les routes publiques dans `public-portal`.
- [x] Garder les features publiques sous `features/public`.
- [x] Brancher `public-portal` sur `libs/page-model` si la page est rendue par PageModel.
- [ ] Ne pas mettre le shell public dans PageModel.
- [ ] Ne pas mettre le thème runtime dans PageModel.
- [x] Prioriser SSR/SSG pour `public-portal`.

## 10. Proxy / sous-routes locales

- [x] Définir les sous-routes locales `/public/**`, `/admin/**`, `/platform/**`, `/pos/**` futur,
  `/api/v1/**`.
- [x] Utiliser des URLs API relatives `/api/v1/...`.
- [x] Ne pas hardcoder les hosts backend dans les features.
- [x] Garder la config proxy compatible avec déploiement indépendant des apps.

## 11. Angular moderne

- [x] Utiliser standalone components.
- [x] Utiliser `bootstrapApplication`.
- [ ] Utiliser lazy routes par surface/feature/page.
- [ ] Utiliser Signals pour état local.
- [ ] Utiliser stores explicites pour pages complexes.
- [x] Ne pas introduire NgRx par défaut.
- [ ] Utiliser `@defer` pour les zones lourdes, notamment vente POS.
- [ ] Utiliser control flow moderne Angular.
- [ ] Garder les composants UI purs : inputs/outputs, pas d'appel HTTP direct.
- [ ] Garder les appels HTTP dans les API services/stores.
- [x] Utiliser `ChangeDetectionStrategy.OnPush` comme convention finale.
- [x] Utiliser signal forms pour les nouveaux formulaires.
- [ ] Utiliser `httpResource`/`resource` pour les lectures réactives quand adapté.

## 12. SSR / performance

- [x] Garder `admin-portal` CSR optimisé en V0.
- [x] Rendre `admin-portal` SSR/hydration-ready pour plus tard.
- [x] Éviter les accès directs non protégés à `window`.
- [x] Éviter les accès directs non protégés à `document`.
- [ ] Garder le shell compatible SSR.
- [ ] Garder la runtime config compatible SSR.
- [ ] Garder les routes lazy propres.
- [ ] Préparer une stratégie hybride par route.
- [x] Prioriser SSR/SSG pour `public-portal`.

## 13. Style / UI

- [x] Utiliser les tokens `--tch-*`.
- [ ] Utiliser les variables locales `--comp-*` dans les composants réutilisables.
- [ ] Ne pas hardcoder les couleurs de marque dans les features.
- [x] Respecter BEM-like naming.
- [x] Utiliser les fichiers `.scss` séparés pour pages/composants.
- [ ] Garder les Material overrides globaux dans `libs/ui/styles` ou `libs/ui/theme`.
- [ ] Ne pas utiliser `::ng-deep` sauf exception temporaire documentée.
- [ ] Garder les composants globaux stateless et réutilisables.

## 14. Critères d'acceptation

- [x] `admin-portal` build et démarre seul.
- [x] `public-portal` build et démarre seul.
- [x] `platform-portal` build et démarre seul.
- [x] La page login est partagée depuis `libs/core/auth`.
- [x] La vente POS est lazy-loaded dans `admin-portal`.
- [x] Aucune app ne dépend directement d'une autre app.
- [x] Aucun client métier n'est centralisé dans `libs/api`.
- [ ] Les clients métier vivent dans `features/**/data-access`.
- [ ] Chaque page routée vit dans son propre dossier.
- [ ] Les pages utilisent des fichiers `.ts`, `.html`, `.scss` séparés.
- [ ] Les stores page-specific vivent avec leur page.
- [ ] Les composants page-only vivent dans `<page>/components`.
- [ ] Les composants feature-shared vivent dans `<feature>/components`.
- [ ] Les composants globaux vivent dans `libs/ui/components`.
- [ ] Aucun composant UI pur n'injecte `HttpClient`.
- [x] Les contrats `ApiResponse`, `ProblemDetail`, `TchPage` vivent dans `libs/api/src/lib/contracts`.
- [x] Les shells vivent dans `libs/web/shell`.
- [x] Le proxy local supporte les sous-routes des apps.
- [x] Les styles respectent `ui/theme`, `ui/styles`, `ui/components`.
