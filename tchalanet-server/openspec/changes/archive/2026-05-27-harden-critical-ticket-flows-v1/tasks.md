# Tasks

## Phase 1 — Sales truth and promotion materialization

- [x] TASK-001: Enrich `TicketPrintLine` / persisted ticket-line snapshots with promotion display fields.
  - Add or expose: `origin`, `pricingSource`, `selectionSource`, `payoutBaseAmount`, `promotionDecisionId`, `promotionLabel`, `promotionEffectType`.
  - Ensure Maryaj gratuit can be displayed without querying promotion again.
  - Ensure BOOST_ODDS can be displayed from line snapshots.

- [x] TASK-002: Harden `SalePreparationOrchestrator` policy input.
  - Introduce an explicit policy input containing paid basis and final basis.
  - Promotion conditions may use paid basis.
  - Limit/exposure/autonomy checks must use final promotion-adjusted lines/money when risk changes.

- [x] TASK-003: Enrich `AppliedPromotionSnapshot`.
  - Capture materialized effects, not only the decision.
  - Include target line/charge, original/final stake/odds/charge, payout base, label, effect type, applied time.

- [x] TASK-004: Enrich `TicketPlacedEvent` and `TicketLinePlacedItem`.
  - Add promotion and origin/pricing/selection snapshot fields per line.
  - Update consumers/tests accordingly.

## Phase 2 — Canonical receipt/print/message content

- [x] TASK-005: Create or evolve rich `TicketReceiptPrintContent` in `core.sales.api.model.receipt`.
  - Include header lines, sections, totals, footer lines, QR info, locale, timezone, metadata.
  - Use sales-neutral line styles, not `platform.document` types.

- [x] TASK-006: Move receipt business layout decisions to `core.sales.internal.application.receipt`.
  - Move header/footer/draw/line/promotion/totals/money formatting out of `TicketPrintDocumentMapper`.
  - Ensure one canonical content path feeds print, backup, and messages where applicable.

- [x] TASK-007: Simplify `TicketPrintDocumentMapper`.
  - Mapper should only convert `TicketReceiptPrintContent` to `DocumentRenderRequest`.
  - It may choose document template, format, paper options, and assets.
  - It must not know game labels, bet options, promotion labels, or money business formatting.

- [x] TASK-008: Ensure `TicketBackupAssembler` uses the same canonical receipt model.
  - Backup returned by sell must match the official receipt content semantics.

## Phase 3 — Cashier print/send hardening

- [x] TASK-009: Validate print/send operational context.
  - Add `SellerOperation.PRINT_TICKET`, `REPRINT_TICKET`, `SEND_RECEIPT`.
  - Replace no-op `validateSellerContextForPrint()`.
  - Enforce trusted operational context for cashier/POS actions.

- [x] TASK-010: Enrich `RecordTicketPrintCommand`.
  - Include actor user, terminal, outlet, sales session, correlation id, format, reprint reason.
  - Store print history sufficient for tenant dispute/audit.

- [x] TASK-011: Make communication dispatch context-explicit.
  - Replace implicit `TchContext` use after commit with explicit dispatch request.
  - Include tenant id, actor user id, ticket id, correlation id, communication options.

- [x] TASK-012: Add dedup/idempotency key for receipt communication.
  - Ensure resend/retry cannot duplicate side effects unexpectedly.

## Phase 4 — Public ticket verification hardening

- [x] TASK-013: Harden `TicketVerifyController`.
  - Add explicit `@PreAuthorize("permitAll()")` if compatible with security config.
  - Keep `ApiResponse.success(...)`.
  - Add headers: `X-Robots-Tag: noindex, nofollow`, `Cache-Control: no-store`, `Pragma: no-cache`, `Expires: 0`.

- [x] TASK-014: Harden public rate-limit behavior.
  - Keep in-memory implementation for single-node V1 if accepted.
  - Add `Retry-After` support for 429 if possible.
  - Document that `X-Forwarded-For` is trusted only behind configured reverse proxy.
  - Add production follow-up for Redis-backed limiter in multi-instance deployments.

- [x] TASK-015: Enrich `TicketVerificationView`.
  - Add `displayCode`, draw channel label, option label, promotion label, promotional flag.
  - Public verify must display Maryaj gratuit and the same draw label as receipt.

- [x] TASK-016: Enrich `TicketVerificationProjection`.
  - Expose sale snapshot labels: game label, bet type label, option label, promotion label.
  - Use `GameCatalog` only as fallback, not primary source for old ticket display.

## Phase 5 — Tests

- [ ] TEST-001: Sell normal paid ticket.
- [ ] TEST-002: Sell with Maryaj gratuit.
- [ ] TEST-003: Sell with BOOST_ODDS.
- [ ] TEST-004: Sell with WAIVE_CHARGE.
- [x] TEST-005: Limit/exposure after promotion.
- [x] TEST-006: Print canonical content with tenant/outlet headers and footers.
- [x] TEST-007: Print/reprint operational context validation.
- [x] TEST-008: Send receipt uses canonical message and context-explicit communication.
- [x] TEST-009: Public verify success with canonical proof.
- [x] TEST-010: Public verify wrong verification code returns same 404 as unknown ticket.
- [x] TEST-011: Public verify rate-limit returns 429 without ticket data.
- [x] TEST-012: Public verify visibility policy returns 404 when hidden.
- [ ] TEST-013: End-to-end flow: sell -> print -> verify for normal, long, and promotional tickets.

## Definition of Done

- [x] No feature layer computes ticket money, promotions, winnings, customer status, or receipt business content.
- [x] `TicketPrintDocumentMapper` is technical only.
- [x] Promotion effects are materialized and snapshotted in sales.
- [x] Limit/exposure checks use final promotion-adjusted risk where applicable.
- [x] `TicketPlacedEvent` includes promotion line snapshots.
- [x] Receipt/print/message/backup use the same canonical sales model.
- [x] Print/send validates operational context.
- [x] Public verify is safe, rate-limited, no-store/noindex, and sales-owned for truth.
- [x] Wrong verification code does not reveal ticket existence.
- [ ] Side effects remain after-commit.
- [x] Communication after commit is context-explicit.
