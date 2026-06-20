  # Tchalanet Screen Realization Guide

  Status: ACTIVE draft

  ## Purpose

  This reference defines the mandatory workflow for creating, correcting, or replacing a Tchalanet screen.

  A Tchalanet screen is not only an Angular page. It is a complete flow:

  - where the user comes from;
  - what the screen receives;
  - how the screen presents itself;
  - how the screen calls external systems;
  - how the screen renders the response;
  - what happens next.

  The goal is to avoid deviations: wrong shell, wrong routing, isolated components, off-brand styles, invented APIs, wrong request/response contracts, missing loading/error/empty states, missing confirmation/refresh behavior, or a beautiful screen that cannot be integrated.

  ## 1. Documents To Consult

  Frontend:

  - `WEB_ARCHITECTURE.md`
  - `docs/conventions/style.md`
  - `docs/conventions/theme-convention.md`
  - `docs/conventions/web-naming.md`
  - PageModel/runtime shell docs if the screen depends on shell or navigation

  Backend/API:

  - `docs/ARCHITECTURE.md`
  - `docs/PLAYBOOK.md`
  - `docs/modules/features.md`
  - `docs/modules/platform.md`
  - `docs/conventions/web_api.md`
  - `docs/conventions/api_response.md`
  - `docs/conventions/pagination.md`
  - `docs/conventions/request_context_usage.md`
  - `docs/conventions/security_permissions.md`
  - `docs/conventions/typed_ids.md`

  Design reference, when available:

  - screenshot;
  - Figma;
  - HTML prototype;
  - older similar page;
  - target page already implemented;
  - existing admin-ui component.

  ## 2. Golden Flow

  Reason about every screen in this order:

  1. Where we come from: route, shell, navigation, actor.
  2. What we receive: params, query params, state, inputs, permissions.
  3. How we present: layout, style, theme, generic components.
  4. How we call: API service, endpoints, request DTOs.
  5. How we receive: response DTOs, `ApiResponse`, `ProblemDetail`, pagination.
  6. How we render: loading, error, empty, ready, submitting, success.
  7. What happens after: confirmation, navigation, refresh, local invalidation.

  Never start directly with the template or SCSS.

  ## 3. Route, Shell, And Navigation

  For every screen, document:

  - surface: public, platform, admin, cashier, or seller-terminal;
  - route: exact path;
  - parent shell: `PublicShell`, `PrivateShell`, or other;
  - router outlet owner: component containing the `router-outlet`;
  - navigation source: PageModel or local fallback;
  - actor: anonymous, SUPER_ADMIN, TENANT_ADMIN, SELLER_TERMINAL, or CASHIER;
  - expected permission.

  Shell rule:

  - Application shell equals `router-outlet`.
  - Page/layout shell equals `ng-content`.

  Allowed:

  - `PrivateShellComponent` contains `router-outlet`.
  - `PublicShellComponent` contains `router-outlet`.

  Forbidden:

  - `AdminPageShellComponent` with `router-outlet`.
  - `AdminCrudShellComponent` with `router-outlet`.
  - `AdminDetailLayoutComponent` with `router-outlet`.
  - `AdminSectionCardComponent` with `router-outlet`.

  Example:

  ```text
  surface: platform
  route: /app/platform/tenants/provisioning
  parent shell: PrivateShellComponent
  router outlet owner: PrivateShellComponent
  navigation: PrivateShellService -> platform navigation
  actor: SUPER_ADMIN
  permission: platform tenant provisioning
  ```

  ## 4. Screen Inputs

  Identify all screen inputs before coding:

  - route params;
  - query params;
  - router state;
  - auth/session state;
  - tenant context;
  - PageModel runtime;
  - form initial values;
  - backend defaults;
  - local/session storage only if justified.

  Rules:

  - Do not invent `tenantId` in the body for tenant/admin endpoints.
  - Do not read JWT directly in a component.
  - Do not duplicate backend context in the frontend.
  - Route params must be typed and validated at usage.
  - Direct URL refresh must rebuild the screen state.

  Mandatory questions:

  - Does the screen depend on an ID in the URL?
  - Does it depend on current tenant?
  - Does it depend on a role?
  - Does it depend on PageModel?
  - Does it depend on fragile navigation state?
  - Must it support browser refresh?

  ## 5. Structure, Style, Theme, Components

  Classify the screen:

  - list;
  - create;
  - edit;
  - detail;
  - dashboard;
  - wizard;
  - settings;
  - ops;
  - report;
  - placeholder.

  Standard layouts:

  - List/CRUD index: `AdminPageShellComponent`, `AdminCrudShellComponent`, toolbar, content, footer, loading/error/empty/ready, pagination if backend-paged.
  - Create/edit/detail: `AdminPageShellComponent`, `AdminDetailLayoutComponent`, `AdminSectionCardComponent`, `TchIdentityCardComponent`, footer actions.
  - Dashboard: `AdminPageShellComponent`, KPI cards, section cards, charts/tables, summary cards.
  - Placeholder: `AdminPageShellComponent`, `AdminEmptyStateComponent`, linked TODO/story.

  A placeholder must not become a final page.

  ## 6. Tchalanet Theme And Style

  Tchalanet does not need a parallel palette. Components consume runtime tokens.

  - `--tch-color-primary`: deep navy; identity, control, platform structure, important object cards.
  - `--tch-color-accent`: gold; active state, action, commercial value, borlette, badge, progress.
  - `--tch-color-surface`: cards, forms, CRUD surfaces.
  - `--tch-color-background`: page background.
  - `--tch-color-error`: destructive actions and errors.

  Forbidden:

  ```scss
  background: #1A1B4B;
  color: #FECB00;
  ```

  Allowed:

  ```scss
  background: var(--tch-color-primary);
  color: var(--tch-color-on-primary);
  ```

  Reusable components expose local variables:

  ```scss
  :host {
    --comp-identity-bg: var(--tch-color-primary);
    --comp-identity-fg: var(--tch-color-on-primary);
    --comp-identity-accent: var(--tch-color-accent);
  }
  ```

  `--comp-*` variables must always fall back to `--tch-*` tokens. Never consume another component's `--comp-*`.

  ## 7. Generic Components To Search First

  Before creating a component, search for:

  - `AdminPageShellComponent`
  - `AdminCrudShellComponent`
  - `AdminSectionCardComponent`
  - `AdminEmptyStateComponent`
  - `AdminStatusPillComponent`
  - `AdminDetailLayoutComponent`
  - `TchIdentityCardComponent`
  - `TchCard`
  - `TchLoading`
  - `TchErrorPanel`
  - `TchButton` or Material button
  - `TchFilterBar`
  - `TchKpiCard`

  Create a new shared component only if:

  - it is reusable by at least two or three predictable screens;
  - it expresses a stable design-system rule;
  - it is not specific to one page.

  If it is page-specific, keep the style in the page.

  ## 8. Capture / Design Stitch

  If a screenshot or design exists, create a stitch plan before coding.

  Extract:

  - main zones;
  - header;
  - actions;
  - cards;
  - aside;
  - table/form;
  - footer;
  - statuses;
  - spacing;
  - dominant colors;
  - Tchalanet markers.

  Document:

  1. Existing components to reuse.
  2. New components required.
  3. Page-specific layout.
  4. Tokens used.
  5. Material components used.
  6. Accepted V0 deviations.

  Parity criteria:

  - zones are in the right place;
  - primary actions are visible;
  - cards respect hierarchy;
  - Tchalanet identity is visible;
  - rendering does not create a new visual charter;
  - responsive behavior does not break.

  ## 9. API Contract

  An integrated screen must identify its API contract:

  - backend controller/service;
  - HTTP route;
  - HTTP method;
  - scope: public, tenant, admin, or platform;
  - permission;
  - request DTO;
  - response DTO;
  - `ApiResponse` wrapper;
  - `ProblemDetail` errors;
  - pagination if list;
  - audit if sensitive action.

  UI/API matrix:

  | UI action | Endpoint/service | Request | Response | UI state | After success |
  | --- | --- | --- | --- | --- | --- |
  | Load page | GET ... | query/page params | view/page | loading/error/empty/ready | render |
  | Preview | POST .../preview | request DTO | preview view | previewLoading/previewError | update preview |
  | Submit | POST ... | request DTO | result view | submitting/submitError | confirmation/navigation/refresh |
  | Row action | POST .../{id}/... | optional body | action result | actionLoading/error | reload row/list |

  Frontend API rules:

  - Do not invent endpoints.
  - Do not invent DTOs when backend exists.
  - Angular interfaces must reflect backend DTOs.
  - Pure UI fields are derived through mappers or `computed()`.
  - Angular services unwrap `ApiResponse<T>` in one place.
  - Components should not manipulate `ApiResponse<T>` directly.

  Example unwrap:

  ```ts
  this.http
    .post<ApiResponse<TenantProvisioningPreviewView>>(url, request)
    .pipe(map(res => res.data));
  ```

  ProblemDetail helper:

  ```ts
  private errorTitle(err: unknown, fallback: string): string {
    const pd = (err as { error?: { title?: string; detail?: string } })?.error;
    return pd?.title ?? pd?.detail ?? fallback;
  }
  ```

  ## 10. Response And UI States

  Every data-driven screen must explicitly handle states.

  List page:

  - initial;
  - loading;
  - error + retry;
  - empty;
  - ready;
  - pagination;
  - row action loading if applicable.

  Form page:

  - initial;
  - valid/invalid;
  - preview loading if applicable;
  - preview error if applicable;
  - submitting;
  - submit error;
  - success.

  Dashboard:

  - loading;
  - partial data if API is partial;
  - critical error;
  - empty/no activity;
  - ready;
  - refresh.

  An empty state must explain what is missing, why the screen is empty, and what action to take next.

  ## 11. User Inputs

  Business forms use Reactive Forms unless the local project has explicitly adopted another strategy for that class of screen.

  Verify:

  - frontend validators align with backend;
  - submit disabled if invalid;
  - submit disabled if submitting;
  - errors visible;
  - default values explicit;
  - debounced preview if applicable.

  Route/query inputs:

  - absent param;
  - invalid param;
  - browser refresh;
  - direct navigation;
  - back navigation.

  Search/filter:

  - debounce;
  - query params if filters must survive refresh;
  - reset page to 0 when filter changes;
  - backend pagination if list.

  ## 12. Success Behavior

  Every action defines what happens after success:

  - stay and show result;
  - navigate to detail;
  - return to list;
  - open confirmation page;
  - reload list;
  - update one row locally;
  - refresh PageModel/runtime if shell/navigation changed.

  After a successful submit, always provide at least one confirmation mechanism:

  - snackbar/toast;
  - inline message;
  - confirmation page;
  - navigation to detail;
  - visible data refresh.

  For actions that modify navigation, shell, or theme, consider refreshing runtime, PageModel, navigation, or reapplying theme.

  ## 13. i18n

  All user-visible text must be translated through i18n keys before a screen is considered final.

  This includes:

  - page title;
  - page description;
  - section titles;
  - field labels;
  - button labels;
  - empty state;
  - error messages;
  - success messages;
  - status labels;
  - table headers;
  - filter labels;
  - hints/help text;
  - confirmation text;
  - snackbar/toast text;
  - aria labels and accessible names when they contain user-facing wording.

  Hardcoded text is acceptable only as a short transition and must be listed as TODO. Do not leave hardcoded UI text in a final screen.

  ## 14. Accessibility

  Verify:

  - visible focus;
  - form labels;
  - `aria-label` on icon buttons;
  - table headers;
  - sufficient contrast;
  - readable error messages;
  - touch target at least 44px.

  Never remove focus outline without replacement.

  ## 15. Responsive

  Use mobile-first responsive behavior. Start with the compact single-column experience, then progressively enhance for wider viewports.

  Verify:

  - compact: one column;
  - medium: readable spacing;
  - expanded: main + aside when applicable;
  - large: controlled width.

  Do not invent new breakpoints without project precedent.

  ## 16. Angular Material

  Use Material when already adopted:

  - `mat-button`
  - `mat-icon-button`
  - `mat-form-field`
  - `mat-input`
  - `mat-select`
  - `mat-table`
  - `mat-menu`
  - `mat-progress-bar`
  - `mat-chip`
  - `mat-snack-bar`

  Do not style Material internals in feature components. Avoid `::ng-deep` unless clearly justified.

  ## 17. Missing Backend Or Design

  If a backend endpoint is missing:

  - do not fake the action;
  - do not invent an endpoint;
  - disable or hide the action;
  - document the expected backend contract;
  - create a follow-up task.

  If design is missing:

  - use the standard layout for the screen type;
  - use existing admin-ui components;
  - do not invent a new visual direction.

  ## 18. Screen Task Template

  Every screen realization task should follow:

  1. Goal.
  2. Documents to consult.
  3. Where we come from: route, shell, navigation, actor/permission.
  4. Inputs: route params, query params, form defaults, context/session.
  5. Design/capture/stitch.
  6. Target layout.
  7. Components to reuse/create.
  8. Theme/style constraints.
  9. API contract: endpoints, request DTOs, response DTOs, errors, pagination.
  10. UI states.
  11. Submit/action success behavior.
  12. i18n.
  13. Responsive/accessibility.
  14. Implementation steps.
  15. Acceptance criteria.
  16. Non-goals.
  17. Follow-up tasks.

  ## 19. Pre-Code Checklist

  Before coding, answer:

  - Where do we come from?
  - Which shell renders this page?
  - What inputs do we receive?
  - Which design/capture must we respect?
  - Which generic components already exist?
  - Which theme tokens should be used?
  - Which backend/API do we call?
  - Which request/response DTOs?
  - Which UI states?
  - What happens after success?
  - Which documents apply?

  ## 20. Post-Code Checklist

  Before finishing, verify:

  - route/shell correct;
  - inputs handled;
  - design/capture respected or deviation documented;
  - existing components reused;
  - no hardcoded brand colors;
  - BEM-like classes;
  - `--comp-*` for reusable components;
  - real API used;
  - request/response aligned with backend;
  - `ApiResponse` unwrapped in service;
  - `ProblemDetail` handled;
  - loading/error/empty/ready implemented;
  - submit/action loading implemented;
  - confirmation/navigation/refresh defined;
  - all visible UI text translated, or temporary hardcoded text listed as TODO;
  - responsive checked;
  - basic accessibility checked;
  - missing backend/design documented;
  - build/lint/test noted.

  ## 21. Golden Rule

  A Tchalanet screen must answer:

  - Where do I come from?
  - What do I receive?
  - How do I present myself?
  - Who do I call?
  - How do I render the response?
  - What happens after?

  If one question has no answer, the screen is not ready.
