# Tasks: harden-core-sales-sell-print

## 1. Ownership cleanup

- [ ] 1.1 Confirm this change owns sell, ticket lifecycle cleanup required for sell, print internals, publicCode generation/retry, `TicketWriterPort` rename, and sales event publishing cleanup.
- [ ] 1.2 Confirm this change does not own public verify DTO shape.
- [ ] 1.3 Confirm this change does not own POS BFF endpoints.
- [ ] 1.4 Confirm this change does not introduce `TicketEntry`, `PlaceTicketCommand`, or multi-entry DB changes.

## 2. Rename writer port

- [ ] 2.1 Rename `TicketWritterPort` to `TicketWriterPort`.
- [ ] 2.2 Rename file, imports, constructor fields, tests.
- [ ] 2.3 Verify no `TicketWritterPort` remains.
- [ ] 2.4 Update `DOMAIN_SALES.md`.

## 3. TicketLinePreparationService hardening

- [ ] 3.1 Add canonical method `prepare(TenantId tenantId, List<LineCommand> lines)`.
- [ ] 3.2 Validate input list is non-null and non-empty.
- [ ] 3.3 Ensure `prepare` calls normalize → mergeDuplicates → toTicketLines.
- [ ] 3.4 Make `requireStake` enforce `scale <= 2`.
- [ ] 3.5 Make `requireStake` return stake at scale 2.
- [ ] 3.6 Add explicit error when `PricingCatalog.oddsFor(...)` returns null.
- [ ] 3.7 Decide odds scale behavior: `UNNECESSARY` preferred if config must be clean.
- [ ] 3.8 Add unit tests for normalization, duplicate merge, stake scale, null odds, invalid betOption.

## 4. SellTicketCommandHandler cleanup

- [ ] 4.1 Enforce session required for new sales.
- [ ] 4.2 Ensure `TicketSalePolicy.prepareSale(...)` returns session or throws.
- [ ] 4.3 Move mixed game validation before `ticketWriter.save(...)`.
- [ ] 4.4 Generate `approvalRequestId` before pending ticket creation.
- [ ] 4.5 Persist `approvalRequestId` in `Ticket.pendingApproval` / `Ticket.requestApproval`.
- [ ] 4.6 Replace direct `UUID.randomUUID()` with `IdGenerator`.
- [ ] 4.7 Replace direct `EventId.of(UUID.randomUUID())` with `IdGenerator`.
- [ ] 4.8 Remove `ApiResponseContext` mutation from handler if possible.
- [ ] 4.9 Return approval/warnings via `SellTicketResult`.
- [ ] 4.10 Add tests for SOLD, WARN, BLOCK/PENDING_APPROVAL, session missing, mixed game fail-before-save.

## 5. Ticket publicCode invariant

- [ ] 5.1 Change `Ticket.publicCode` from nullable to required.
- [ ] 5.2 Constructor uses `requireNonBlank(publicCode, "publicCode")`.
- [ ] 5.3 Factories `sell`, `pendingApproval`, `requestApproval` require publicCode.
- [ ] 5.4 Rehydrate keeps publicCode required unless explicitly handling legacy migration.
- [ ] 5.5 Update `TicketEntity.publicCode` to `nullable = false`.
- [ ] 5.6 Verify Flyway schema and JPA mapping are aligned.
- [ ] 5.7 Add tests that ticket creation fails without publicCode.

## 6. Ticket code collision retry

- [ ] 6.1 Keep `TimeBasedTicketNumberGenerator`.
- [ ] 6.2 Keep `CrockfordPublicCodeGenerator`.
- [ ] 6.3 Create `TicketCodeGenerationException`.
- [ ] 6.4 Map `TicketCodeGenerationException` to HTTP 503.
- [ ] 6.5 Implement retry max 3 on unique constraint violations:
  - `uq_ticket_tenant_code`
  - `uq_ticket_public_code`
- [ ] 6.6 Ensure retry regenerates both `ticketCode` and `publicCode`.
- [ ] 6.7 Add integration test forcing collision and verifying retry.
- [ ] 6.8 Add integration test exceeding max retries and receiving `TicketCodeGenerationException`.

## 7. TicketPrintViewAssembler

- [ ] 7.1 Create `core.sales.application.service.TicketPrintViewAssembler`.
- [ ] 7.2 Move draw/session/outlet/terminal lookup logic out of `JpaTicketRepositoryAdapter`.
- [ ] 7.3 Assembler loads draw via `DrawLookupPort`.
- [ ] 7.4 Assembler loads session via `SalesSessionReaderPort` when `ticket.sessionId != null`.
- [ ] 7.5 Assembler loads outlet via session outlet id.
- [ ] 7.6 Assembler loads terminal via `TerminalReaderPort`.
- [ ] 7.7 Assembler resolves `terminalLabel`; no UUID fallback.
- [ ] 7.8 Assembler passes `Locale` into `TicketPrintViewMapper`.
- [ ] 7.9 Add tests with full data, missing session, missing outlet, missing terminal label.

## 8. TicketReaderPort cleanup

- [ ] 8.1 Remove `TicketReaderPort.getTicketPrintView(...)`.
- [ ] 8.2 Keep `TicketReaderPort.findWithLinesById(...)`.
- [ ] 8.3 Update `GetTicketPrintPdfQueryHandler`.
- [ ] 8.4 Update `GetTicketPrintEscPosQueryHandler`.
- [ ] 8.5 Verify `JpaTicketRepositoryAdapter` no longer injects:
  - `DrawLookupPort`
  - `OutletReaderPort`
  - `SalesSessionReaderPort`
  - `TicketPrintViewMapper`
- [ ] 8.6 Delete unused `contextResolver.currentOrNull()` in `findWithLinesById`.

## 9. TicketPrintView model

- [ ] 9.1 Add `terminalLabel`.
- [ ] 9.2 Add explicit `ZoneId zoneId` or equivalent print timezone.
- [ ] 9.3 Remove terminal UUID from formatter usage.
- [ ] 9.4 Keep internal ids out of rendered ticket.
- [ ] 9.5 Ensure `publicCode` is always printed.
- [ ] 9.6 Ensure QR payload uses `publicCode`.

## 10. Receipt formatter

- [ ] 10.1 Replace `ZoneId.systemDefault()` with `t.zoneId()`.
- [ ] 10.2 Group lines by `gameCode + betType + betOption`.
- [ ] 10.3 For 58mm/ESC-POS layout, support compact two-column rendering.
- [ ] 10.4 For PDF/80mm layout, allow stake + potential payout.
- [ ] 10.5 Replace `maskUuid(t.terminalId())` with `t.terminalLabel()`.
- [ ] 10.6 Ensure ESC/POS formatter sanitizes accents if ASCII-only.
- [ ] 10.7 Add formatter tests for grouping and terminal label.

## 11. PDF builder

- [ ] 11.1 Compute page height dynamically from line count.
- [ ] 11.2 Support null/empty QR bytes.
- [ ] 11.3 Do not silently cut lines when content exceeds old fixed height.
- [ ] 11.4 Add PDF generation tests for small and large tickets.
- [ ] 11.5 Keep monospace font for body alignment.

## 12. Controller print endpoints

- [ ] 12.1 Keep `/tenant/tickets/{ticketId}/print.pdf`.
- [ ] 12.2 Keep `/tenant/tickets/{ticketId}/print.escpos`.
- [ ] 12.3 Deprecate or remove `/tenant/tickets/{ticketId}/print` base64 `text/plain`.
- [ ] 12.4 Ensure all print endpoints set `Cache-Control: no-store`.
- [ ] 12.5 Ensure filenames do not expose raw UUID if avoidable; prefer ticketCode.

## 13. Sales event publishing cleanup

- [ ] 13.1 Verify `TicketPlacedEvent` publication uses `AfterCommit.run(...)`.
- [ ] 13.2 Replace direct UUID event id generation with `IdGenerator`.
- [ ] 13.3 Remove `TicketEventPublisherPort` if unused.
- [ ] 13.4 Verify no event is published before ticket save commit.
- [ ] 13.5 Add tests that event is scheduled after successful save only.

## 14. Cross-domain isolation checks

- [ ] 14.1 Ensure `core.sales.infra.persistence.adapter.*` contains no SQL referencing:
  - `draw_result`
  - `result_slot`
  - `draw`
  - `draw_channel`
  - `outlet`
  - `terminal`
  - `address`
- [ ] 14.2 Add or update `SalesIsolationArchTest`.
- [ ] 14.3 Ensure `DrawResultViewPortJdbcAdapter` removal is owned by this change only if not already handled elsewhere.
- [ ] 14.4 If `DrawResultViewPortJdbcAdapter` remains in this change, replace with `DrawResultProjectionCatalog`.

## 15. Documentation

- [ ] 15.1 Update `DOMAIN_SALES.md` sell flow.
- [ ] 15.2 Update `DOMAIN_SALES.md` print architecture.
- [ ] 15.3 Update `DOMAIN_SALES.md` ports list.
- [ ] 15.4 Update `sell-ticket.md` request body notes.
- [ ] 15.5 Update OpenAPI docs for removed/deprecated `/print` base64 endpoint.
- [ ] 15.6 Add CHANGELOG entries:
  - `TicketWritterPort` rename internal
  - `SellTicketRequest` body cleanup if included
  - `/print` base64 deprecation/removal if included

## 16. Verification

- [ ] 16.1 Run `./mvnw clean verify`.
- [ ] 16.2 Run print PDF manual smoke test with 1-line ticket.
- [ ] 16.3 Run print PDF manual smoke test with 20+ lines.
- [ ] 16.4 Run ESC/POS text smoke test.
- [ ] 16.5 Verify sold ticket has `ticketCode`, `publicCode`, QR URL.
- [ ] 16.6 Verify pending ticket persists `approvalRequestId`.
