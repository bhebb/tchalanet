# Claude — core.sales

Scope:

- Ticket lifecycle (placement, approval, results, settlement)
- Critical money domain (multi-tenant, RLS, Envers audit)
- Events: TicketPlaced, TicketApproved, TicketCancelled, TicketResulted, TicketOverridden

Out of scope:

- Draw lifecycle (core.draw responsibility)
- Result ingestion (core.drawresult responsibility)
- Ledger accounting (core.ledger responsibility)
- Payout execution (core.payout responsibility)

Rules:

- **Typed IDs everywhere**: TenantId, TicketId, TerminalId, DrawId, SessionId, OutletId, AgentId
- **@TchTx on all write handlers**: SellTicket, Approve, Reject, Cancel, RecordDrawResult, Override
- **AfterCommit.run() for events**: TicketPlaced, TicketApproved, TicketCancelled, TicketResulted, TicketOverridden
- **RLS filters by tenant_id**: No raw UUID in queries; DB enforces row-level filtering
- **Soft-delete compatible**: Ticket supports is_deleted flag for archival
- **Envers audit active**: Full history tracked for Ticket and TicketLine entities
- **Invariants enforced**: Ticket status state machine, line validations, winning calculations

Key flows:

1. **Sell**: prepareSale → check limits → newSoldTicket → publish TicketPlacedEvent
2. **Approve/Reject**: load ticket (PENDING_APPROVAL) → transition → TicketPaymentPendingEvent
3. **Record Results**: triggered by DrawResultAppliedEvent → calculate winning → TicketResultedEvent
4. **Override**: admin force result change → TicketResultOverriddenEvent
5. **Cancel**: reverse ticket via CancelSaleCommand → TicketCancelledEvent

Before editing:

- Load DOMAIN_SALES.md for full domain model and invariants
- Load core/draw for draw lifecycle integration
- Load core/drawresult for result matching patterns
- Load core/limitpolicy for limit evaluation contracts
- Load docs/conventions/typed_ids.md for ID wrapper rules
- Load docs/conventions/persistence.md for soft-delete patterns

Output:

1. Files inspected
2. Files changed
3. Tests (if any, prefer integration tests for money flows)
4. Risks (data consistency, limit bypass scenarios)
5. Compact handoff
