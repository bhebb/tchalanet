# Error Contract Inventory

Status: initial evidence collected on 2026-07-17. This is deliberately incomplete until every
feature BFF is classified. It records observed behaviour, not desired behaviour.

## Canonical server entry points

| Producer | Current behaviour | Contract gap / follow-up |
|---|---|---|
| `GlobalErrorHandler` | Handles `ProblemRestException`, typed `TchException`s, JPA, Jakarta/Spring validation, request deserialization, security, and catch-all errors. Adds correlation fields. | Several handlers derive `detail` from exception/cause text. It must become diagnostic-only; validation needs stable safe violation codes/parameters. |
| `ProblemRest` / `ProblemRestException` | Common direct producer of stable-ish HTTP error codes. | Calls mix codes and human prose. Inventory and descriptor registry must separate safe code from diagnostic detail. |
| `RequireIdempotencyAspect` | Emits `idempotency.missing`, `idempotency.payload_mismatch`, `idempotency.in_progress`, and `idempotency.completed_no_response`. | Good first catalog candidates; verify exact client translation and retry semantics. |
| `ApiResponseContext` + `ApiResponseBodyAdvice` | Collects request-local notices/services and merges them into successful envelopes. | Must be tested for cleanup/async/204 and must not make message fields visible contracts. |
| `ApiResponseNotices` | Adds code/message/domain/severity and trace metadata. | `message` is currently required and remains potentially visible. Define diagnostic-only migration and safe parameters. |
| `BffSlices` | Required slice rethrows; optional slice adds notice and returns fallback. | Exists but needs real adoption audit, semantic kind/status precedence, dedupe, typed unavailable sections, and BFF-level tests. |

## Observed blocking error risks

| Location | Evidence | Required migration direction |
|---|---|---|
| `GlobalErrorHandler.handleNotReadable` | Uses Jackson most-specific cause text as `ProblemDetail.detail`. | Replace visible diagnostic text with stable code/approved parameter; retain redacted cause only in logs. |
| `GlobalErrorHandler.handleTypeMismatch` | Appends the most-specific cause message to detail. | Same: stable violation/code plus safe field parameter only. |
| `GlobalErrorHandler.handleConstraintViolation` | Uses raw constraint exception message. | Convert to structured violations; never use Bean Validation prose for product UI. |
| `GlobalErrorHandler.handleLegacyIllegalState` | Exposes `IllegalStateException` message and maps every instance to 422. | Remove blind legacy mapping after callers use explicit typed business errors. |
| `GlobalErrorHandler.handleJpaNotFound` | Exposes JPA exception message. | Migrate callers to a stable typed not-found code. |
| `PosTicketReceiptService` | Wraps `ex.getMessage()` in `ProblemRest.badRequest`. | High-priority POS path: map exception to stable receipt/delivery code and log root cause privately. |

## Legacy `ProblemRest` contract audit

The current `ProblemRest` API names its main argument `detail` and writes it directly into
`ProblemDetail.detail`. It does not require or add a `code` property. `ProblemRestException` has the
same legacy message-first shape. This is materially different from the desired error contract.

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

**Migration decision recorded:** Phase 1 must introduce a code-first factory/descriptor path while
keeping `ProblemRest` only as a temporary compatibility bridge. The bridge must emit a stable
fallback code plus diagnostic-only legacy detail, and a static rule must prevent new message-first
call sites. It must not silently infer a code by parsing `detail`.

### First migrated producer

`PosTicketReceiptService` now maps invalid print options to
`pos.receipt.print_options_invalid` with `VALIDATION` / `AFTER_USER_ACTION`. The original
`IllegalArgumentException` remains the exception cause for server-side diagnostics, while the
serialized ProblemDetail contains the stable code rather than its prose. Other receipt validation
codes remain legacy until their form/POS ownership and client translations are migrated together.

## Observed BFF / aggregated read behaviour

| Surface / entry point | Primary result | Secondary slices observed | Current failure behaviour | Initial classification |
|---|---|---|---|---|
| PageModel dynamic runtime (`PageModelDynamicResolver`) | Page payload / widgets | Each dynamic provider/widget | Catches provider errors, adds `WidgetDynamicError` and `ApiNotice`. Current notice includes web placement metadata. | Non-blocking per widget; migrate target to stable functional ID and let client choose placement. |
| Tenant admin dashboard (`TenantAdminDashboardPayloadAssembler`) | Tenant dashboard payload | registry, seller terminal counts, games/channels/limits, KPI/analytics/live sales, draws, notifications, commission, public content | Many `RuntimeException`s become `0`, empty list, or `null`, with log only. | Most are likely non-blocking sections, but currently silent and indistinguishable from real zero/empty data. |
| Platform admin dashboard (`PlatformAdminDashboardPayloadAssembler`) | Platform dashboard payload | tenant catalog, analytics, subscriptions, onboarding, public content | Catches failures and returns zero/empty section values with log only. | Non-blocking section candidates; need explicit unavailable state and degradation code. |
| Cashier dashboard (`PosDashboardPayloadAssembler`) | Cashier dashboard payload | overview, next draws, recent tickets, analytics | Analytics failures become `CashierStatsPayload.unavailable()`; other slices remain blocking. | Analytics is explicit unavailable state but lacks response notice/correlation; classify and render locally. |
| Tenant admin overview (`TenantAdminOverviewService`) | Tenant overview | address, registry, readiness | Address/registry errors become `null` silently. | Optional header fields; need explicit degradation or intentionally documented absence. |
| Platform overview (`PlatformAdminOverviewOrchestrator`) | Platform overview payload | tenant, catalog, subscription reads | No local catch; any failure fails whole response. | Provisional blocking until the slice matrix decides otherwise. |

## Initial BFF endpoint matrix

The feature module currently has 34 HTTP controllers. A controller is not automatically a BFF: this
matrix starts with endpoints that resolve a page, assemble a multi-domain payload, or contain an
orchestrator. Remaining controllers are to be recorded as either a simple feature API or a BFF in
the next pass.

| Endpoint | Assembler/orchestrator | Primary response | Current error mode | Matrix status |
|---|---|---|---|---|
| `GET /tenant/dashboard` | `DashboardPageModelService` → `PageModelDynamicResolver` | Tenant PageModel runtime | Blocking access/model failures; per-widget catch and notice; tenant settings catch silently returns original page. | In progress |
| `GET /platform/dashboard` | `DashboardPageModelService` → `PageModelDynamicResolver` | Platform PageModel runtime | Blocking access/model failures; per-widget catch and notice. | In progress |
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

## Next inventory pass

1. Enumerate all `features/**` HTTP BFF/controllers and identify their assembler/orchestrator.
2. Complete the slice matrix for dashboard and PageModel sources first.
3. Inventory direct `ProblemDetail` and `ProblemRest` callers by code/prose/owner.
4. Record client consumers for each migrated endpoint before changing envelope methods.
