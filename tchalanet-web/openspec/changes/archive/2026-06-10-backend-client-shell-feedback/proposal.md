# Change: backend-client-shell-feedback

## Why

Les appels HTTP sont actuellement répétés sans couche partagée : chaque service injecte `HttpClient`
directement, appelle `.pipe(map(unwrapApiResponse))` manuellement, et code le préfixe `/api/v1` en
dur. Il n'existe pas non plus de canal transverse pour afficher les notices backend, les services
dégradés, ou les erreurs techniques dans le shell.

## What changes

- Créer `TchBackendClient` dans `src/app/shared/api/` : passe-plat technique au-dessus de
  `HttpClient` qui standardise le préfixe `/api/v1`, l'unwrapping `ApiResponse<T>`, les téléchargements
  bruts, les uploads multipart, et les headers override tenant pour SUPER_ADMIN.
- Créer `ApiFeedbackInterceptor` : observe les réponses, alimente `ShellFeedbackStore` en notices et
  erreurs transverses sans avaler les erreurs.
- Créer `ShellFeedbackStore` + `ShellFeedbackOutletComponent` + `ShellFeedbackBannerComponent` dans
  `src/app/shared/feedback/` : pipeline d'affichage shell pour notices, services dégradés, erreurs
  techniques avec trace id et copie support-safe.
- Ajouter `<tch-shell-feedback-outlet />` dans les shells public, tenant-admin, et platform.
- Migrer les services API existants pour injecter `TchBackendClient` à la place de `HttpClient`.

## Impact

- Touche uniquement `tchalanet-web` / `apps/tch-portal`.
- Aucun changement backend.
- `libs/api` reste intact : `TchBackendClient` consomme ses exports (`ApiResponse`, `TCH_API_BASE`,
  `ProblemDetail`, `unwrapApiResponse`).
- Les services métier existants (`PageModelApi`, `SettingsApi`, `TenantAdminApi`,
  `PlatformAdminApi`, services publics) migrent vers `TchBackendClient` dans une slice dédiée.

## Non-goals

- Pas de retry global.
- Pas de nouvelle lib Nx.
- Pas de logique métier dans `TchBackendClient` ni dans `ApiFeedbackInterceptor`.
- Pas d'affichage de stacktrace, JWT, ou données sensibles.
- Pas de migration des appels `HttpClient` hors du périmètre API backend (assets, i18n).
