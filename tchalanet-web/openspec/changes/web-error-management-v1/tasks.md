# Tasks — web-error-management-v1

## 1. Contract inventory

- [x] Review existing shell feedback and `ProblemDetail` mapping behavior.
- [x] Review backend `GlobalErrorHandler` and `ApiResponseBodyAdvice` behavior.
- [x] Add cross-project API error-contract OpenSpec for backend/web BFF aggregation.
- [x] Inventory current duplicated error surfaces and support/trace actions in `apps/tch-portal`.
      Current local owners include public page-model fallbacks/pages, admin page-level `TchErrorPanel`
      states, and platform operations partial-data cards.
- [x] Identify feature flows that intentionally handle errors locally and must suppress shell feedback.
      Representative migrated flows: public home page-model load and private admin barèmes load.
- [x] Confirm `web-error-management-v1` remains a web consumption change and does not redefine the
      backend `ApiResponse`/`ProblemDetail` contract.

## 2. Error model and mapping

- [x] Add a canonical `WebAppError` model in the appropriate web/API boundary.
- [x] Add a deterministic normalizer from `ProblemDetail`, `ApiResponse.notices`, HTTP status,
      network failures, and frontend runtime errors.
- [x] Define stable category and severity mapping tests.
- [x] Define deterministic `dedupeKey` generation before changing shell feedback grouping.
- [x] Preserve backend correlation fields for both blocking and non-blocking failures:
      `traceId`, `requestId`, `errorId`, `spanId` when available.
- [x] Keep raw `HttpErrorResponse` inspection inside API clients/normalizers, not presentation
      components.
- [x] Add local routing metadata for page/section/field errors:
      `surface`, `placement`, `target`, and `field`.

## 3. Shell feedback behavior

- [x] Change shell feedback storage from simple stacking to deduplicated groups with repeat counts.
- [x] Cap visible shell feedback and render an overflow summary instead of five independent banners.
- [x] Ensure locally handled page/section/form errors do not also create shell banners.
- [x] Add reusable field and section error presentation primitives without moving API logic into
      reusable UI components.
- [x] Migrate one representative private form to consume server field validation locally.
      The business profile commission form maps `ProblemDetail.violations` to the `rate` control.
      The business profile address form maps server field violations to address controls.
      Setup locale and receipt forms map server field violations to their owning controls.
- [x] Keep public shell feedback minimal and private shell diagnostics role-aware.
- [x] Avoid introducing a generic global error store; keep shell feedback limited to shell-owned
      cross-cutting feedback.
- [x] Keep ownership/rendering logic outside reusable `ui/components`.

## 4. Support and navigation actions

- [x] Remove or rename the ambiguous "send trace to support" action unless a real submission endpoint
      exists.
- [x] Provide a non-navigation "copy support reference" action for private standard/verbose views.
- [x] Ensure feedback actions preserve the current shell: public users stay public, authenticated users
      stay private.
- [x] Add retry actions only where the owning operation is explicitly retryable.
- [x] Do not add global retry actions.

## 5. User copy and i18n

- [x] Add localized category messages for public-safe errors.
- [x] Add known backend-code-specific messages only for stable backend codes.
- [x] Add exact-code translation support for notice codes such as
      `platform.identity.activation.error`.
- [x] Prevent raw technical titles/details from being shown to public/minimal users.
- [x] Prevent raw exception/provider/SQL/stack messages from public/minimal UI copy.

## 6. Validation

- [x] Run focused unit tests for error normalization, deduplication, and redaction.
- [x] Test local handling suppression and shell-preserving
      action routing.
- [x] Test repeat counts and category/severity mapping.
- [x] Run focused Nx lint/test targets for touched web projects.
- [x] Migrate one representative public flow and one authenticated private flow before broad
      migration. PageModel dashboards now map section-targeted `ApiResponse.notices` into
      widget-local `dynamic.errors`; backend PageModel provider failures now emit matching
      section-targeted notices. The authenticated business profile page now routes the non-blocking
      commission overview failure to the commercial section card via `tchSectionErrorTarget`, keeps
      blocking overview failures at page level, and maps identity/region/commission/address submit
      failures to the owning form or field.
      Tenant provisioning readiness preview failures now stay local to the health card and render
      through `tch-section-error`. The platform operations overview now routes partial slice
      failures to the owning dashboard cards for results, draws, jobs, and cache. The Setup
      complete-config page now renders section-targeted `/admin/overview` notices on the owning
      setup cards while preserving page-level failure for blocking load errors. The games-pricing
      overview now keeps blocking load failures at page level and renders activate/disable action
      failures on the owning game card. The draw-channels overview now keeps blocking load
      failures at page level and renders provider source failures on the owning provider card.
      The draw-sales-matrix page now keeps blocking load failures at page level and renders
      offer/toggle/remove action failures on the affected channel/game tile. Business-days and
      limits now keep blocking load failures at page level while rendering add/delete/save action
      failures in the owning page section or dialog. Draw list tabs now own their section load
      failures, and draw detail manual-result failures render in the result-entry section.
      Seller-terminal creation now suppresses shell feedback, maps backend field violations to
      form controls, and keeps one page-local fallback error for blocking create failures.
      Seller-terminal list load/action failures now suppress shell feedback and render once in the
      owning page or dialog for unblock, disable, block, reset-PIN, and seller-terminal limits
      actions; create/unblock/disable success paths no longer create snackbars. Seller-terminal POS
      now treats terminal lookup as blocking, while draws, games, and
      activity failures render on their owning sections and sale failures stay beside the sale
      action. Admin support sell now suppresses shell feedback for selector, preview, and sale calls,
      rendering selector failures above the form and action failures near the submit actions.
      Commission admin now suppresses shell feedback and renders overview/seller/default-rate
      failures through the owning page or section card. Games admin now suppresses shell feedback
      and renders enabled/catalog tab failures in-place, with settings failures owned by the dialog.
      Subscription admin now suppresses shell feedback and renders load/action failures through the
      owning page or actions card. Admin tickets now suppresses shell feedback and renders list
      failures through the page error. Admin notifications now suppresses shell feedback and keeps
      list, composer, and row/bulk action feedback inside the owning page surface. Admin business
      days now suppresses shell feedback and renders month load, add, and delete feedback inside
      the owning page surface without snackbars. Admin draw-sales matrix now suppresses shell
      feedback and renders matrix load failures at page level while offer/toggle/remove feedback
      stays on the owning game card. Admin generated-draws now suppresses shell feedback and
      renders list and lifecycle action feedback in the owning page surface without snackbars.
      Admin limits now suppresses shell feedback and renders rules/assignment load failures at page
      level, upsert failures in the dialog, and delete feedback in the owning page surface. Admin
      draw-results now suppresses shell feedback and renders load/filter failures at page level.
      Admin setup settings now suppresses shell feedback and renders config load failures at page
      level, while locale and receipt submit failures stay in their owning forms. Admin setup
      runtime now suppresses shell feedback and renders load/reload feedback in the owning page
      surface without snackbars. Admin draw-channels now renders blocking load failures at page
      level and keeps the unavailable configure action as local page information instead of a
      snackbar. Admin pricing now suppresses shell feedback and renders default-odds load failures
      as one safe page-level error with message copy. Admin onboarding/users/placeholders/settings
      now use external templates/styles instead of inline component definitions. Admin payouts now
      suppresses shell feedback, renders list/action failures locally, removes snackbars, and uses
      external dialog template/styles. Platform super-admins list/detail now suppress shell
      feedback, render failures locally, and keep temporary-password/assignment feedback as
      page-local notices instead of snackbars. Platform tenant-admins list/detail now suppress
      shell feedback, render failures locally, and keep temporary-password feedback as a page-local
      notice instead of a snackbar. Platform contact-requests now suppresses shell feedback and
      renders list/detail/action failures locally with status/notes success feedback as page-local
      notices instead of snackbars. Platform news now suppresses shell feedback and renders
      list/save/status/hide/RSS-refresh failures locally with success feedback as page-local
      notices instead of snackbars. Platform notifications now suppresses shell feedback and renders
      list/composer/row/lifecycle failures locally with success feedback as page-local notices
      instead of snackbars. Platform placeholder/support-tenant/ops-cache now remove
      inline/raw/snackbar feedback, suppress shell feedback for owned loads/actions, and render
      cache dialog failures locally. Platform communication list/outbox/tests now suppress shell
      feedback and render load/dispatch/provider-test feedback locally without snackbars. Platform
      tenant-admin support access now renders start-session failures locally with normalized copy.
      Platform ops draw-results now suppresses shell feedback and renders list/confirm feedback
      locally without snackbars.
- [ ] Manually verify one public error and one authenticated private error do not stack duplicate
      banners and do not route to the wrong shell.
