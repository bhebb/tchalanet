# Tasks

## 1. Contract checkpoint

- [x] Confirm this change supersedes the `doc/pageModel + dynamic` runtime shape described by
      `tchalanet-web/openspec/changes/extend-pagemodel-runtime-role-dashboards`.
- [x] Confirm the surface-oriented runtime routes: `/public/page`, `/tenant/dashboard`, and
      `/platform/dashboard`.
- [x] Freeze representative public-shell and private-shell `PageRuntimeResponse` JSON fixtures.
- [x] Define the forbidden runtime key list, including `binding`, `fileKey`, provider/source
      details, snake_case contract fields, and template/admin metadata.
- [x] Confirm runtime `meta` contains only `logicalId`, `scope`, `slug`, and `schemaVersion`.

## 2. Backend runtime contract

- [x] Add `features.pagemodel` runtime DTOs for meta, optional theme hint, typed shell, content,
      widget config, dynamic data, navigation destinations, and actions.
- [x] Map template/internal metadata explicitly and exclude `code`, `name`, `label`, `schema`,
      `isDefault`, `level`, tenant id, timestamps, and persistence ids from runtime.
- [x] Keep internal PageDefinition/PageModelDoc types owned by catalog/core and prevent runtime
      controllers from serializing them directly.
- [x] Implement a dedicated PageRuntime assembler from the effective internal definition.
- [x] Map every frontend runtime field to camelCase.
- [x] Align template/widget/navigation UI ids to camelCase while preserving logical ids and i18n keys.
- [x] Add a contract guard/test that fails when forbidden internal keys leak into runtime JSON.

## 3. Backend shell and fragment resolution

- [x] Resolve public header/footer fragments into `shell.header` and `shell.footer`.
- [x] Resolve private shell fragments into `shell.topAppBar` and `shell.navigationDrawer`.
- [x] Ensure `shell.type` is exactly `public` or `private`.
- [x] Remove shell delivery through dynamic widget ids such as `shell.header` and `shell.footer`.
- [x] Resolve fragment-backed widget props before mapping to runtime.
- [x] Ensure runtime output never contains `fileKey`, raw resource paths, or fragment registry keys.

## 4. Backend widget data and actions

- [x] Keep render config in `content.widgets[widgetId]` as `type + props` only.
- [x] Keep volatile provider payloads in `dynamic.widgets[widgetId]`.
- [x] Keep request-level notices and services only in the `ApiResponse` envelope.
- [x] Project `home.draws` to the lightweight public fields required by the homepage.
- [x] Return sanitized widget-local errors without provider/storage internals.
- [x] Normalize all navigable actions to `destination.kind/value`.
- [x] Preserve route authorization and tenant/scope resolution from `TchRequestContext`.
- [x] Keep layout V1 limited to rows, columns, span, widgets, and labelKey.

## 5. Backend endpoints and tests

- [x] Add `GET /api/v1/public/page`, resolving the single public page server-side without accepting
      a logical id.
- [x] Add `GET /api/v1/tenant/dashboard`, resolving the dashboard from `TchRequestContext`.
- [x] Add `GET /api/v1/platform/dashboard`, resolving the platform dashboard server-side.
- [x] Make all three runtime endpoints return the same `PageRuntimeResponse` contract inside the
      existing API envelope.
- [x] Remove the legacy `/public/page-models/{logicalId}`, `/tenant/page-models`, and
      `/platform/page-models` runtime routes after the web migration.
- [x] Verify public home returns header, footer, layout, widget config, and widget data in one call.
- [ ] Verify each supported private dashboard returns top app bar and navigation drawer in one call.
- [x] Verify runtime JSON is camelCase and contains no `binding` or `fileKey`.
- [x] Verify provider/fragment failures are contained and sanitized.
- [x] Verify all PageModel template `fileKey` values resolve through the fragment registry.
- [x] Verify every template layout references exactly its declared widget definitions.
- [x] Verify template, fragment, provider dispatch, and E2E fixture widget ids use aligned camelCase
      runtime ids.
- [ ] Verify unauthorized private PageModel access invokes no sensitive provider.

## 6. Web runtime types and API

- [x] Replace the direct mirror of backend `PageModelDoc` with `PageRuntimeResponse` types.
- [x] Remove frontend `WidgetBinding`, `ShellSectionConfig`, snake_case runtime fields, and
      `fileKey` handling.
- [x] Update `PageModelApi` to use `/public/page`, `/tenant/dashboard`, and `/platform/dashboard`
      and return the unified runtime type.
- [x] Add web contract tests using the frozen backend runtime fixtures.

## 7. Web shell and page rendering

- [x] Make the page container load only the PageModel runtime call for page composition.
- [x] Select `PublicShell` or `PrivateShell` only from `shell.type`.
- [x] Render public header/footer directly from the public shell runtime.
- [x] Render private top app bar/navigation drawer directly from the private shell runtime.
- [x] Remove reads of `dynamic.widgets['shell.header']` and `dynamic.widgets['shell.footer']`.
- [x] Ensure public page loading/error states and private dashboard shell remain usable.

## 8. Web widgets and navigation

- [x] Make `WidgetHost` join `content.widgets[widgetId]` with `dynamic.widgets[widgetId]`.
- [x] Keep unsupported widget failures contained.
- [x] Map route destinations to Angular `routerLink`.
- [x] Map URL destinations to external `href`.
- [x] Remove support for parallel action fields such as `path`, raw `href`, and internal/external
      destination kinds.

## 9. Documentation and OpenSpec alignment

- [x] Update backend `FEATURE_PAGEMODEL.md` with the internal-definition/runtime-response boundary.
- [x] Update web PageModel convention with the simplified runtime contract.
- [ ] Update or supersede canonical PageModel specs that require snake_case or direct PageModelDoc
      runtime serialization.
- [ ] Reconcile the runtime-shape sections of existing active PageModel OpenSpec changes.

## 10. Validation and delivery

- [x] Run focused backend tests for runtime mapping, serialization, shell resolution, provider
      isolation, draw projection, camelCase props, and dashboard provider dispatch.
- [ ] Run focused backend access-control tests.
- [x] Run focused web PageModel/API/shell/widget tests through Nx.
- [x] Validate all PageModel template and fragment JSON files.
- [x] Validate OpenSpec strictly and run `git diff --check`.
- [ ] Run a public-home integration check proving one PageModel composition call.
- [ ] Run tenant-admin and platform-admin integration checks proving navigation drawers remain.
- [x] Inspect representative runtime JSON and confirm all acceptance criteria.
