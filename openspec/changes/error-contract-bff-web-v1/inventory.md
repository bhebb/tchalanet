# Error Contract Inventory

Status: initial evidence collected on 2026-07-17. This is deliberately incomplete until every
feature BFF is classified. It records observed behaviour, not desired behaviour.

## Canonical server entry points

| Producer | Current behaviour | Contract gap / follow-up |
|---|---|---|
| `GlobalErrorHandler` | Handles `ProblemRestException`, typed `TchException`s, JPA, Jakarta/Spring validation, request deserialization, security, and catch-all errors. Adds correlation fields and now acts as a migration firewall: legacy `ProblemRest` prose becomes neutral detail plus a stable code before serialization. | Typed legacy exceptions and framework adapters still need owner descriptors and exact product copy; the firewall is protection, not final classification. |
| Spring Security filter chain | **Migrated baseline:** the OAuth resource-server entry point, access-denied handler, and sensitive identity verification failure share `ProblemDetailSecurityFailureHandler`, backed by `ProblemDetailResponseWriter`. | Running app-level handler tests are temporarily blocked by pre-existing app `testCompile` missing-class failures; retain direct handler tests and restore app test compilation before closing this row. |
| `ProblemRest` / `ProblemRestException` | Common direct producer of stable-ish HTTP error codes. The preferred `ErrorDescriptor` path carries category/retry/approved params. | Legacy overloads remain, but dynamic prose/properties are stripped at the HTTP boundary. Inventory and descriptor registry must still migrate each owner deliberately. |
| `RequireIdempotencyAspect` | Emits `idempotency.missing`, `idempotency.payload_mismatch`, `idempotency.in_progress`, and `idempotency.completed_no_response`. | Good first catalog candidates; verify exact client translation and retry semantics. |
| `ApiResponseContext` + `ApiResponseBodyAdvice` | Collects request-local notices/services and merges them into successful envelopes. | Must be tested for cleanup/async/204 and must not make message fields visible contracts. |
| `ApiResponseNotices` | Adds code/message/domain/severity and trace metadata. | `message` is currently required and remains potentially visible. Define diagnostic-only migration and safe parameters. |
| `BffSlices` | Required slice rethrows; optional slice adds notice and returns fallback. | Exists but needs real adoption audit, semantic kind/status precedence, dedupe, typed unavailable sections, and BFF-level tests. |

## Observed blocking error risks

| Location | Evidence | Required migration direction |
|---|---|---|
| `GlobalErrorHandler.handleNotReadable` | **Migrated:** fixed `Malformed request body` detail and a stable `request.not_readable` code; parser prose is omitted from the response. | Add contract fixtures and client translation coverage. |
| `GlobalErrorHandler.handleMethodArgumentNotValid` / `handleConstraintViolation` | **Migrated baseline:** returns `violations` with only stable `code`, `field`, and `target`; no Bean Validation message, rejected value, or legacy `errors` map. | Add approved safe parameters only where a form needs them, then add exact three-locale client catalog entries. |
| `GlobalErrorHandler.handleTypeMismatch` | No longer appends the most-specific conversion cause. | Normalize its remaining target/detail to the same structured validation path. |
| `GlobalErrorHandler.handleLegacyIllegalState` | **Migrated safety baseline:** returns 500 `internal.unexpected`; exception type/message remains server-side only. | Remove this broad legacy handler after callers use explicit typed business errors. |
| `GlobalErrorHandler.handleJpaNotFound` | **Migrated safety baseline:** returns 404 `resource.not_found` without JPA/RLS detail. | Migrate callers to owner-specific typed not-found codes where product semantics require it. |
| `GlobalErrorHandler.handleProblemRest` | **Migrated safety baseline:** old code-in-detail becomes structured `code`; prose/dynamic parameters are removed and receive a category fallback code. | Convert each producer to an owner descriptor so approved safe params and exact client translations can be restored intentionally. |
| Security `AuthenticationEntryPoint` / `AccessDeniedHandler` | **Migrated:** filter-chain `401`/`403` responses use stable `access.authentication_required` / `access.denied` descriptors and omit exception/protocol detail. | Add exact three-locale code catalogs and a running security-chain test after app `testCompile` is repaired. |
| `PosTicketReceiptService` | **Migrated:** invalid print options use `pos.receipt.print_options_invalid` with a descriptor and private root cause. | Migrate delivery/channel/recipient validation together with the POS delivery form ownership. |

### POS sales contract gap

The POS controllers delegate sale preparation and confirmation to `core.sales`. POS-reachable sales
producers have now migrated from `ProblemRest.*(String)` to `SalesErrorCodes` descriptors: sale
preparation, confirmation, ticket lookup/verification, ticket-list filter validation, and
ticket-print projection lookup all serialize `ProblemDetail.code`. The legacy String overload still
exists elsewhere in the server and remains prohibited for new POS/sales producers.

`SaleIssueFactory` now preserves a code-first `ProblemDetail.code` before using its legacy-detail
bridge. This keeps the original stable code through preparation and confirmation responses; the
admin POS renders the code through i18n and never the legacy `message` or `sellerInstruction` prose.
The seller-terminal sale gate is also code-first as `sales.seller_terminal.cannot_sell`. Ticket
lookup no longer serializes a ticket UUID in `ProblemDetail.detail`.

One POS producer is also invalid as a stable contract:

```text
sales.tenant_disabled:<tenant-status>
```

It makes the apparent code dynamic. This must become a constant code such as
`sales.tenant_disabled`, with the status retained only as server-side diagnostics or an approved
safe parameter if the product needs it.

**Remaining POS backend proof:** add MVC/contract tests asserting the serialized
`ProblemDetail.code` for prepare, confirm, receipt-send, print/reprint, verify, and ticket-detail
failures. Do not expand the web legacy-detail allowlist.

### POS mobile-home copy gap

`PosHomeService` still serializes display prose in `HomeHeader`, `HomeRequiredStep`, and
`HomeAction` (for example the greeting, PIN-change message, and action labels). This is not an
error renderer and is intentionally outside the admin seller-terminal POS slice. Before migrating
the mobile POS home to retained envelopes, replace that transport prose with stable i18n keys and
approved parameters; mobile must not use backend French text as its localization contract.

## Contract producer priority matrix

This is the explicit audit boundary for the first backend pass. Counts are static signals of
`ProblemRest`, direct `ProblemDetail`, and thrown exception use as of 2026-07-18; they are not a
claim that every site is an HTTP producer. Each domain audit must classify the reachable producers,
replace client-relevant legacy strings with owner `ErrorDescriptor`s, and add contract tests for the
HTTP paths named below.

| Priority | Owner | Signal count | Why it is first | Required contract proof |
|---|---|---:|---|---|
| P0 | `core.sales` | 174 | Determines prepare, confirm, ticket lifecycle, print and payment eligibility. | `ProblemDetail.code` for invalid sale input, availability/cutoff, limit block, seller-terminal block, preparation expiry/replay, ticket not found, print/reprint and delivery failures. |
| P0 | `core.sellerterminal` | 47 | Owns seller-terminal admin lifecycle and POS identity. | **Migrated and HTTP-proven:** missing terminal, invalid state transition, commission validation, identity binding/provisioning, and PIN reset use registered code-first descriptors. The admin state-transition adapter serializes the exact code/category/retry policy with neutral detail. |
| P0 | `core.pricing` | 31 | Owns tenant payout rules and seller-terminal overrides used at sale preparation. | **Migrated and HTTP-proven:** pricing validation, missing tenant defaults, override lookup, and payout-rule mismatches use registered code-first descriptors. The admin pricing adapter serializes the exact code/category/retry policy with neutral detail. |
| P0 | `core.limitpolicy` | 35 | Directly decides allow/warn/approval/block for a sale. | Stable code/category/retry policy for every blocking outcome; a rejected sale carries the policy code rather than a generic 403. |
| P0 | `platform.accesscontrol` | 16 | Resolves effective tenant and support/admin actor scope before a POS or admin action. | Non-enumerating codes for missing/forbidden actor, tenant override and ambiguous membership; 401/403 semantics remain deterministic. |
| P0 | `platform.identity` | 79 | Owns login/session/bootstrap/handoff and account activation. | Authentication errors are generic where enumeration matters; session/activation/handoff codes are stable and serialized. |
| P1 | `core.promotion` | 39 | Shapes Maryaj gratis and can reject/alter prepared sales. | Promotion configuration and selection failures return stable field/business codes; no generator/provider prose reaches clients. |
| P1 | `platform.tenant` | 58 | Tenant status, business day and tenant configuration gate every sale. | Stable active/closed/configuration codes, with no dynamic status embedded in the code. |
| P1 | `platform.tenantgame` | 18 | Controls game/bet option visibility and availability in the POS. | Stable disabled/not-configured/not-visible codes for prepare and confirm. |
| P1 | `features.pos` | 18 | HTTP adapters for POS draws, games, tickets, receipt/send and home. | Controllers preserve core `ProblemDetail` unchanged and successful envelopes retain notices/trace. Ticket verification maps only `ticket.not_found` to its business result; every other stable failure propagates. Admin POS maps `SaleIssue` by code only, never server prose. |
| P2 | `features.tenantadmin` | 7 | Tenant configuration BFF/form surface; it must expose useful field failures without leaking IDs. | The overview now distinguishes unavailable address/registry slices from absent business data and emits degradation notices; draw-channel game mutations use owner descriptors. The dashboard slice matrix and remaining forms stay open. |
| P2 | `features.pagemodel` | 20 | Public/private BFF aggregation and optional-widget degradation. | Required failure is `ProblemDetail`; optional failure is typed unavailable section plus a stable degradation notice, never a silent zero. |

### Audit order

1. `core.sales` + `core.limitpolicy` + `platform.tenantgame`: one sale decision contract from
   prepare through confirm.
2. `platform.accesscontrol` + `platform.identity` + `platform.tenant`: actor/session/tenant gates
   and enumeration review.
3. `core.promotion`: Maryaj and promotion-specific selection/validation paths.
4. `features.pos`: controller/adaptor pass with MVC contracts and retained success envelopes.
5. `features.tenantadmin` + `features.pagemodel`: BFF slice matrices, PARTIAL semantics, and
   section-notice contracts.

No web or mobile screen is considered migrated merely because it has a generic fallback. A screen
gets exact feedback only after its owning backend path has a tested stable code.

## Legacy `ProblemRest` contract audit

The legacy `ProblemRest` overloads still name their main argument `detail` and create a
message-first `ProblemDetail`. `GlobalErrorHandler` now neutralizes that shape at the HTTP boundary:
a valid dotted legacy value becomes `code`, arbitrary prose receives a status-safe fallback code,
and legacy properties are dropped. The preferred descriptor factory is the only path that retains
category, retry policy, and approved public parameters.

Initial scan across common/core/platform/catalog/features:

| Measure | Result | Meaning |
|---|---:|---|
| `ProblemRest.*` call sites | 232 | Most must be classified before changing the API. |
| Direct `ProblemDetail` construction sites | 33 | Must be either migrated to descriptor factories or explicitly justified as framework adapters. |
| Clearly unsafe/prose/dynamic `ProblemRest` examples in first pass | 30+ | They cannot form a stable client translation key. |

Representative unsafe examples:

| Category | Example pattern | Why it is unsafe |
|---|---|---|
| Exception prose | `ProblemRest.badRequest(ex.getMessage(), ex)` | A provider/library message becomes client-visible error detail. |
| Human sentence | `ProblemRest.conflict("Draw is not open for sales")` | Translatable only by parsing prose; not a stable API contract. |
| Dynamic value in code/detail | `"ops.sales_simulation.draw_not_in_tenant: " + drawUuid` | Creates unbounded keys and leaks an internal identifier. |
| Entity/id appended by helper | `notFound("Result slot not found", id)` | Adds an ID into client detail and cannot be cataloged. |
| Context prose | `"sales.tenant_disabled:" + tenant.status()` | Turns a variable business state into a dynamic code/detail. |
| Security/identity prose | `"No account found for this email"` | Risks enumeration and cannot be a generic public login response. |

**Migration decision recorded:** Phase 1 keeps `ProblemRest` as a temporary compatibility bridge.
The bridge emits a stable fallback code and neutral response detail; it does not serialize legacy
detail or properties. It may recognize an already-valid dotted legacy code strictly as a migration
bridge, but must never promote arbitrary prose. A baseline/allowlist guard must prevent new
message-first call sites and only shrink as owners migrate.

### First migrated producer

`PosTicketReceiptService` now maps invalid print options to
`pos.receipt.print_options_invalid` with `VALIDATION` / `AFTER_USER_ACTION`. The original
`IllegalArgumentException` remains the exception cause for server-side diagnostics, while the
serialized ProblemDetail contains the stable code rather than its prose. Other receipt validation
codes remain legacy until their form/POS ownership and client translations are migrated together.

## Observed BFF / aggregated read behaviour

| Surface / entry point | Primary result | Secondary slices observed | Current failure behaviour | Initial classification |
|---|---|---|---|---|
| PageModel dynamic runtime (`PageModelDynamicResolver`) | Page payload / widgets | Each dynamic provider/widget | Catches provider errors, adds `WidgetDynamicError` and `ApiNotice`. Direct widget IDs remain only for resolver-generated compatibility errors. | Non-blocking per widget. BFF notices name a stable functional slice; resolved runtime widget configs declare `feedbackTargets`, which the API boundary maps to the renderer. |
| Tenant admin dashboard (`TenantAdminDashboardPayloadAssembler`) | Tenant dashboard payload | registry, seller terminal counts, games/channels/limits, KPI/analytics/live sales, draws, notifications, commission, public content | Each remote/query slice now records `AVAILABLE`, `EMPTY`, or `UNAVAILABLE` in additive `sectionStates`; failures emit one stable degradation notice and return `PARTIAL` while preserving the primary payload. | Vertical migrated with assembler and PageModel tests. BFF notice targets are functional `tenant_admin_dashboard.<slice>` identifiers. |
| Platform admin dashboard (`PlatformAdminDashboardPayloadAssembler`) | Platform dashboard payload | tenant catalog, analytics, subscriptions, onboarding, public content | Each remote/query slice now emits a stable degradation notice and returns `PARTIAL` with a fallback payload. The shared PageModel client owns the notice locally and retries through the platform dashboard reload. | Commercial vertical migrated with functional `platform_admin_dashboard.<slice>` targets. Platform Ops has its own pending slice matrix. |
| Cashier dashboard (`PosDashboardPayloadAssembler`) | Dormant PageModel payload | overview, next draws, recent tickets, analytics | Analytics exceptions and an absent projection now become `CashierStatsPayload.unavailable()` plus `pos.dashboard.analytics_unavailable` and `PARTIAL`; other slices remain blocking. | No current PageModel template, Angular route, or mobile consumer references `cashier_dashboard`. Do not add a renderer under this change; separately decide to expose or remove the projection. `PosHomeService` is the mobile endpoint and has no analytics slice. |
| Tenant admin overview (`TenantAdminOverviewService`) | Tenant overview | address, registry, readiness | Address/registry failures now produce `tenantadmin.overview.address_unavailable` / `tenantadmin.overview.registry_unavailable`, `PARTIAL`, and additive availability flags; a genuine missing address/registry remains business-empty. | Migrated for header slices; the web owner must render the local warning and retry action. |
| Platform overview (`PlatformAdminOverviewOrchestrator`) | Platform overview payload | tenant, catalog, subscription reads | No local catch; any failure fails whole response. | Provisional blocking until the slice matrix decides otherwise. |

## Initial BFF endpoint matrix

The feature module currently has 34 HTTP controllers. A controller is not automatically a BFF: this
matrix starts with endpoints that resolve a page, assemble a multi-domain payload, or contain an
orchestrator. Remaining controllers are to be recorded as either a simple feature API or a BFF in
the next pass.

| Endpoint | Assembler/orchestrator | Primary response | Current error mode | Matrix status |
|---|---|---|---|---|
| `GET /tenant/dashboard` | `DashboardPageModelService` → `PageModelDynamicResolver` | Tenant PageModel runtime | Tenant dashboard optional slices are explicit `PARTIAL` degradations and are rendered/retried locally; tenant settings still catches silently. | Vertical migrated; settings and canonical functional target remain. |
| `GET /platform/dashboard` | `DashboardPageModelService` → `PageModelDynamicResolver` | Platform PageModel runtime | Commercial platform dashboard optional slices are explicit `PARTIAL` degradations and are rendered/retried locally. | Commercial vertical migrated; Ops dashboard and canonical functional target remain. |
| `GET /public/page`, `GET /public/managers` | Public PageModel services → dynamic resolver | Public PageModel runtime | Uses same dynamic-resolution semantics; public disclosure/retry policy still unclassified. | In progress |
| `GET /tenant/cashier/home` | `PosHomeService` / cashier payload assemblers | Compact POS home | Separate home/readiness payload; dashboard analytics has an unavailable state without envelope notice. | In progress |
| `GET /platform/overview` | `PlatformAdminOverviewOrchestrator` | Platform overview | Multi-domain reads currently fail as one blocking request. | In progress |
| `GET /admin/overview` | `TenantAdminOverviewService` | Tenant overview | Optional address/registry failures become null. | In progress |

### PageModel dashboard slice candidates

| Dashboard | Candidate functional slice | Current fallback | Needed decision |
|---|---|---|---|
| Tenant admin | `operations` | counts become zero | Is a zero count valid? If yes, expose `UNAVAILABLE` separately on failure. |
| Tenant admin | `commercial` | empty games/channels and zero limits | Same empty-versus-unavailable distinction. |
| Tenant admin | `analytics`, `liveSales` | `null` or empty values | Section degradation, local retry policy, and report-safe copy. |
| Tenant admin | `openDraws`, `closedDraws`, `notifications`, `commission` | zero/empty | Decide which are expected optional slices and which block operational decisions. |
| Platform admin | `tenantCatalog`, `analytics`, `subscriptions`, `onboarding`, `publicContent` | zero/empty | Explicit `UNAVAILABLE` state and one degradation notice per functional slice. |
| Cashier | `analytics` | `available=false` | Preserve this good typed state, add code/correlation/feature ownership; classify overview/draws/tickets separately. |

## Initial implementation findings that alter the design

- `PageModelDynamicResolver` currently emits `surface=section` and `placement=top` in notice meta.
  The contract will replace these with at most a stable functional `target`; a portal owns visual
  placement.
- `DashboardPageModelService.withTenantSettings` catches all runtime failures and returns the page
  unchanged. It needs explicit optional-slice treatment, otherwise settings absence is invisible.
- `PosDashboardPayloadAssembler`, tenant admin dashboard, and platform dashboard prove that an
  `ApiResponse` success status alone cannot be trusted to mean complete data. Their section states
  and envelopes must become part of the matrix before client migration.

## Immediate implementation guardrails

1. Do not turn all caught exceptions into shell banners. Each migrated slice must declare an owner.
2. Do not change dashboard fallback values before the client can distinguish `EMPTY` from
   `UNAVAILABLE`; otherwise reports may appear to be genuine zeroes.
3. Do not expose server exception text merely because a client translation key is missing.
4. Treat receipt delivery, sale, authentication, and print paths as priority flows once the common
   contract fixtures exist.

## Translation gate status

`pnpm i18n:inventory -- --check` was run on 2026-07-18 after adding the initial shared validation
codes in all three shipped bundles. The new `validation.required`, `validation.invalid_format`, and
`validation.out_of_range` keys are aligned across `ht`, `fr`, and `en`.

The inventory now has zero referenced missing keys. This fixed the POS confirmation/delivery dialog
that rendered raw `admin.sellerTerminal.pos.dialog.*` keys, plus the report and navigation labels.
The inventory was also missing the `errors` bundle entirely, so valid `common.errors.*` entries were
wrongly reported absent; it now reads the same bundle set as the runtime loader.

The check mode blocks missing references, locale parity, and forbidden key placement. It still
prints the pre-existing duplicate and legacy-root diagnostics without blocking CI; those are an
explicit taxonomy-cleanup slice, not an excuse to hide missing user copy.

## Web normalization baseline

`WebAppError` retains stable code/category/retry/params and correlation only. It does not retain
raw `ProblemDetail.title/detail`, `ApiNotice.message`, field violation messages, or
`ServiceStatus.message` in client state. Its legacy title/message placeholders are still an
intermediate presentation bridge; the next client wave replaces them with exact catalog key lookup
and removes hardcoded French fallback copy.

`TchBackendClient` now has a complete retained `*ApiResponse` family, including paged responses,
multipart requests, and a `getApiResponseResource` for reactive BFF/dashboard reads. Existing
data-only methods remain compatible and are documented as unsuitable for endpoints that emit
notices, service metadata, `PARTIAL`, or trace information. The POS preparation/confirmation flow
already follows this pattern; dashboard and form migrations remain an explicit consumer pass.

### First form/dialog consumer vertical: seller terminals

The seller-terminal creation page, list actions, block dialog, reset-PIN dialog, and limits dialog
now normalize both raw `HttpErrorResponse` values and already-normalized `ProblemDetail` values via
`mapHttpErrorToProblemDetail`. The block/create dialogs attach known violations to controls, place
unmapped violations in `tch-form-error-summary`, and clear only server errors on field edits. The
block dialog returns a typed reload/notice result; the list owns the localized success notice and
resource reload. This is the reference migration for the remaining CRUD/dialog features.

The admin POS seller picker, terminal sale, ticket verification, and ticket detail pages use the same
normalizer for their local failures. They retain their existing local ownership and
`suppressShellFeedback` policy. This closes the raw transport-reader pass for the POS sales feature;
the next step is retained-envelope consumption for successful notices and partial responses.

The PageModel boundary sanitizes dynamic widget errors to stable code, severity, widget target, and
support references. It does not carry `ApiNotice.message` or a raw backend widget message into the
render model. A PageModel test covers both inputs.

## Ownership gap

`SUPPRESS_SHELL_FEEDBACK` is currently written by many feature calls but no shell/interceptor reads
it. It therefore does not route or suppress anything today; it is only an intent marker. Do not
assume it prevents duplicate UI feedback. Phase 6 must introduce an explicit request feedback
context (`owner`, `target`, `mode`) and wire it into the eventual shell feedback router before
migrating call sites. Until then, feature-local presenters remain the effective renderer.

The first compatibility step now exists in `@tch/api`: `TCH_FEEDBACK_CONTEXT` carries explicit
`owner`, `mode`, and optional `target`, while legacy `suppressShellFeedback` maps to local mode and
still writes its deprecated token. PageModel uses `{ owner: 'feature', mode: 'local',
target: 'page-model' }`. This is deliberately passive until a tested router consumes it.

The first router now lives in `@tch/web/shell` and is registered in public, admin, and platform HTTP
chains before the transport-only ProblemDetail mapper. It adds a deduplicated shell banner only for
an explicit `feedback: { owner: 'shell', mode: 'inherit' }` request; `local`, `silent`, and ownerless
`inherit` requests cannot create shell feedback. This conservative default avoids duplicate rendering
while page/form/section consumers migrate. A shell-owned error keeps only stable translated copy and
safe correlation identifiers in its copyable support reference. The response path is deliberately
`support-access → auth-bearer → shell-feedback → problem-detail`: a raw 401 can be retried by auth
before an error is normalized or presented. `AuthSessionService` accepts both raw and normalized
401/403 values when it decides whether to clear a stale application session.

## Shared recovery surface baseline

`tch-error-panel` and `tch-page-error` now accept only owner-supplied normalized title/message,
safe support reference, and retry state. They expose retry/copy events but never perform requests,
routing, clipboard access, or translation lookup. Both give their action controls a touch-safe size;
the full-page surface stacks actions on narrow screens. Tests cover support-reference rendering and
the disabled retry state. The owner/router and i18n layers still need to supply localized labels,
copy support references, and move focus after recovery.

`TchFieldError` now translates local Angular validator keys instead of embedding French copy. It
still expects feature code to attach already-resolved presentation copy for server errors; this is
intentional because the UI library does not own backend-code resolution.

`applyServerFieldErrors` now appends mapped `WebAppError` values under the server error key rather
than overwriting an earlier violation on the same control. Unknown fields remain unconsumed so the
form owner can present them in a summary. Nested paths, arrays, and field-change cleanup are still
pending. `TchFormErrorSummary` now renders safe unconsumed violations and focuses
itself when it appears. The tenant business-profile page is the first migrated form consumer: its
identity, region, commission, and address mutations normalize both a direct `ProblemDetail` and a
legacy `HttpErrorResponse` through `mapHttpErrorToProblemDetail` before routing field violations.
It is the reference pattern for the next form migrations.

The seller-terminal create and block dialogs now follow the same boundary. They no longer inspect
`err.error` or collapse the server error list to one message: `TchFieldError` receives the control,
renders each resolved violation, and local validator feedback has one owner rather than competing
with an inline Material error.

`clearServerFieldErrorsOnEdit` recursively subscribes to reactive controls and removes only the
server error associated with the control the user changed. It is active on business-profile and both
seller-terminal dialogs. The seller-terminal creation page has an equivalent Signal Form validator:
it keeps a server error only while the rejected value remains unchanged. Other Signal Form screens
still need the same adoption.

## Next inventory pass

1. Enumerate all `features/**` HTTP BFF/controllers and identify their assembler/orchestrator.
2. Complete the slice matrix for dashboard and PageModel sources first.
3. Inventory direct `ProblemDetail` and `ProblemRest` callers by code/prose/owner.
4. Record client consumers for each migrated endpoint before changing envelope methods.
