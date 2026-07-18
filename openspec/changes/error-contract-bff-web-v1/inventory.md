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

## Observed BFF / aggregated read behaviour

| Surface / entry point | Primary result | Secondary slices observed | Current failure behaviour | Initial classification |
|---|---|---|---|---|
| PageModel dynamic runtime (`PageModelDynamicResolver`) | Page payload / widgets | Each dynamic provider/widget | Catches provider errors, adds `WidgetDynamicError` and `ApiNotice`. Current notice includes web placement metadata. | Non-blocking per widget; migrate target to stable functional ID and let client choose placement. |
| Tenant admin dashboard (`TenantAdminDashboardPayloadAssembler`) | Tenant dashboard payload | registry, seller terminal counts, games/channels/limits, KPI/analytics/live sales, draws, notifications, commission, public content | Many `RuntimeException`s become `0`, empty list, or `null`, with log only. | Most are likely non-blocking sections, but currently silent and indistinguishable from real zero/empty data. |
| Platform admin dashboard (`PlatformAdminDashboardPayloadAssembler`) | Platform dashboard payload | tenant catalog, analytics, subscriptions, onboarding, public content | Catches failures and returns zero/empty section values with log only. | Non-blocking section candidates; need explicit unavailable state and degradation code. |
| Cashier dashboard (`PosDashboardPayloadAssembler`) | Cashier dashboard payload | overview, next draws, recent tickets, analytics | Analytics failures become `CashierStatsPayload.unavailable()`; other slices remain blocking. | Analytics is explicit unavailable state but lacks response notice/correlation; classify and render locally. |
| Tenant admin overview (`TenantAdminOverviewService`) | Tenant overview | address, registry, readiness | Address/registry errors become `null` silently. | Optional header fields; need explicit degradation or intentionally documented absence. |
| Platform overview (`PlatformAdminOverviewOrchestrator`) | Platform overview payload | tenant, catalog, subscription reads | No local catch; any failure fails whole response. | Provisional blocking until the slice matrix decides otherwise. |

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
