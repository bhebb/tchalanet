# Tasks

## 1. Package structure

- [ ] `features.tenantadmin.overview`
- [ ] `features.tenantadmin.onboarding`
- [ ] Keep existing `features.tenantadmin.config.identity` only if it truly orchestrates tenant config.
- [ ] Do not create CRUD duplicates for outlet/terminal/policies.

## 2. Overview endpoint

- [ ] `GET /admin/tenantadmin/overview` or project-approved route.
- [ ] Aggregates:
  - tenant identity summary
  - outlet count / blocked count
  - terminal count / offline count / sync pending count
  - users count / active sellers
  - games enabled count
  - draw channel configured count
  - policy configured yes/no
  - autonomy configured yes/no
  - open sessions count
  - today sales summary
- [ ] Use QueryBus and catalog APIs only.
- [ ] No business decisions.

## 3. Onboarding endpoints

- [ ] `GET /admin/onboarding/status`
- [ ] `POST /admin/onboarding/steps/default-outlet`
- [ ] `POST /admin/onboarding/steps/default-terminal`
- [ ] `POST /admin/onboarding/steps/attach-users`
- [ ] `POST /admin/onboarding/steps/enable-games`
- [ ] `POST /admin/onboarding/steps/configure-draw-channels`
- [ ] `POST /admin/onboarding/steps/configure-policies`
- [ ] `POST /admin/onboarding/complete`
- [ ] Each step orchestrates core/catalog commands.
- [ ] Each step must be idempotent.

## 4. Existing config overview cleanup

- [ ] Fix routes: avoid bare `/config`; use `/admin/...` logical path.
- [ ] Do not pass `tenantId.value()` raw UUID to autonomy query; introduce typed target.
- [ ] Keep `TenantAdminConfigOverviewOrchestrator` as orchestration only.
- [ ] Do not swallow missing tenant config with `orElseThrow()` without ProblemDetail mapping.

## 5. I18n admin

- [ ] If i18n override belongs to tenant admin feature, keep it here.
- [ ] Add `@TchPaging` for search endpoint.
- [ ] Ensure `TchPageRequest` is resolved by annotation, not raw unvalidated argument.
- [ ] Use core/catalog service behind the feature if available.

## 6. Policies overview

- [ ] `GET /admin/policies/overview` may remain feature or core read endpoint.
- [ ] It must aggregate core.limitpolicy + core.autonomy.
- [ ] It must not perform mutations.
- [ ] Mutations remain in core policy controllers.

## 7. Security and response

- [ ] Use `@PreAuthorize` with project-standard permissions.
- [ ] Use `ApiResponse<T>`.
- [ ] Use `@CurrentContext`.
- [ ] No direct repository/JPA access.

## 8. Tests

- [ ] Overview returns correct setup status from fake queries.
- [ ] Onboarding step is idempotent.
- [ ] Feature does not depend on JPA entities.
