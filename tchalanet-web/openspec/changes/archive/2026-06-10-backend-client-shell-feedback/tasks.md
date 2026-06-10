# Tasks — backend-client-shell-feedback

## 1. TchBackendClient

- [x] Créer `libs/api/src/lib/http/api-feedback-context.ts` : `SUPPRESS_SHELL_FEEDBACK` token.
- [x] Créer `libs/api/src/lib/http/tch-backend-client.ts` avec méthodes `get/post/put/patch/delete`,
      `getApiResponse/postApiResponse`, raw downloads, multipart, override tenant.
- [x] Exporter depuis `libs/api/src/index.ts`.

## 2. ApiFeedbackInterceptor

- [x] Créer `src/app/shared/api/api-feedback.interceptor.ts`.
- [x] Observer notices ApiResponse et services DOWN/DEGRADED → ShellFeedbackStore.
- [x] Observer erreurs ProblemDetail → ShellFeedbackStore (ne pas avaler).
- [x] Respecter `SUPPRESS_SHELL_FEEDBACK` dans le contexte HTTP.
- [x] Filtrer sur `APPLICATION_API_URL_PATTERN` pour ne pas intercepter les appels hors backend.
- [x] Placer avant `problemDetailInterceptor` dans `app.config.ts`.

## 3. ShellFeedbackStore et modèles

- [x] Créer `src/app/shared/feedback/shell-feedback.model.ts` :
      `ShellFeedbackSeverity`, `ShellFeedbackVerbosity`, `ShellFeedbackItem`, `AddShellFeedbackInput`.
- [x] Créer `src/app/shared/feedback/shell-feedback.store.ts` (signal store) :
      `items`, `hasItems`, `add`, `dismiss`, `clear`, limite max 5.
- [x] Créer `src/app/shared/feedback/copy-error-details.ts` : `buildCopyText`, `copyToClipboard`.

## 4. ShellFeedbackOutletComponent et bannière

- [x] Créer `src/app/shared/feedback/shell-feedback-banner.component.ts` avec affichage conditionnel
      trace id (standard/verbose), status+source (verbose), bouton copier (standard/verbose).
- [x] Créer `src/app/shared/feedback/shell-feedback-outlet.component.ts` avec `[verbosity]` Input.
- [x] Ajouter `<tch-shell-feedback-outlet verbosity="minimal" />` dans le shell public.
- [x] Ajouter `<tch-shell-feedback-outlet [verbosity]="feedbackVerbosity()" />` dans le shell privé
      (SUPER_ADMIN → verbose, TENANT_ADMIN → standard).

## 5. Migration services existants

- [x] Migrer `PageModelApi` (`libs/page-model`) vers `TchBackendClient`.
- [x] Migrer `SettingsApi` (`libs/shared-config`) vers `TchBackendClient` (paths logiques sans /api/v1).
- [x] Migrer `TenantAdminApi` (`apps/tch-portal/features/admin`) vers `TchBackendClient`.
- [x] Migrer `PlatformAdminApi` (`apps/tch-portal/features/platform`) vers `TchBackendClient`.
- [x] Migrer `PublicDrawResultsService` vers `TchBackendClient`.
- [x] Migrer `PublicTchalaService` vers `TchBackendClient`.
- [x] Migrer `PublicTicketVerificationApi` vers `TchBackendClient` + `suppressShellFeedback: true`
      (gère ses erreurs localement).

## 6. Norme

- [x] Mettre à jour `libs/api/README.md` avec la norme complète `TchBackendClient`.
- [x] Mettre à jour `tchalanet-web/AGENTS.md` avec la règle de layering HTTP.

## 7. Validation

- [x] `pnpm nx run-many -t lint --projects=api,page-model,shared-config` — zéro erreur.
- [x] Lint `tch-portal` : zéro erreur sur les fichiers touchés (2 erreurs pré-existantes, hors scope).
- [ ] Vérifier manuellement l'affichage de la bannière feedback sur une erreur 5xx simulée.
- [ ] Vérifier le bouton "Copier les détails" sur le shell tenant admin.
