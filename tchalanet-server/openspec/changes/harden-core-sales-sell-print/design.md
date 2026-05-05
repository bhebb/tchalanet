# Design: harden-core-sales-sell-print

## 1. Ownership

`core.sales` owns the ticket sale lifecycle and ticket print preparation.

`features.pos` may orchestrate UI requests, but must consume `core.sales` commands and queries.

Canonical command:

```text
SellTicketCommand
```

POS-facing DTOs may be named:

```text
PlaceSaleRequest
PlaceSaleResponse
```

But no `PlaceTicketCommand` is introduced in this change.

## 2. Sell target flow

```text
SellTicketCommandHandler
  -> TicketSalePolicy.prepareSale(command)
      -> resolve session
      -> enforce session OPEN
      -> resolve draw
      -> enforce draw OPEN / before cutoff
      -> TicketLinePreparationService.prepare(...)
      -> evaluate limits/autonomy
  -> validate single game MVP rule before save
  -> TicketSaleFactory.newSoldTicket(...) OR newPendingApprovalTicket(...)
  -> TicketWriterPort.save(...)
  -> publish TicketPlacedEvent AFTER_COMMIT when SOLD
  -> return SellTicketResult
```

## 3. Session rule

For MVP, `session` is mandatory.

Reason:

- `TicketPlacedEvent` requires `session.id()`, `session.outletId()`, `session.userId()`.
- Print needs session/outlet context.
- POS flow is session-based.

If legacy backfills require null sessions, that remains a rehydrate/read concern, not a new sale concern.

## 4. Approval rule

The approval id must be generated before creating the pending ticket.

Bad pattern:

```java
Ticket pending = ticketFactory.newPendingApprovalTicket(...);
var approvalRequestId = UUID.randomUUID();
return new SellTicketResult(saved, PENDING_APPROVAL, approvalRequestId);
```

Target pattern:

```java
var approvalRequestId = ApprovalRequestId.of(idGenerator.newUuid());

Ticket pending = ticketFactory.newPendingApprovalTicket(
    ...,
    approvalRequestId
);

var saved = ticketWriter.save(pending);

return SellTicketResult.pending(saved, approvalRequestId, notices);
```

If a dedicated approval domain does not exist yet, the id remains a persisted placeholder on `Ticket`.

## 5. TicketLinePreparationService

Expose a single canonical method:

```java
List<TicketLine> prepare(TenantId tenantId, List<SellTicketCommand.LineCommand> lines)
```

Internal pipeline:

```text
validate non-empty
normalize selections
validate bet options
canonicalize stake
merge duplicates
resolve odds
compute potential payout
build TicketLine
```

`requireStake` rules:

- not null
- > 0
- scale <= 2
- returned value scale = 2

Odds rules:

- `PricingCatalog.oddsFor(...)` must not return null.
- odds snapshot stored scale = 4.
- Prefer `RoundingMode.UNNECESSARY` if config is expected clean.
- Use explicit exception if odds are missing or invalid.

## 6. Ticket code rules

Two codes exist:

```text
ticketCode = internal tenant-scoped support/search code
publicCode = globally unique public verification code
```

Both are generated during ticket creation.

`publicCode` is mandatory in MVP.

Retry collision behavior:

```text
attempt save
if unique violation uq_ticket_tenant_code or uq_ticket_public_code:
  regenerate ticketCode/publicCode
  retry
max 3 attempts
after max -> TicketCodeGenerationException
```

The retry must be implemented outside pure domain logic.

Allowed locations:

- application service wrapping `TicketWriterPort`
- writer adapter only if carefully isolated and documented

The preferred design is an application-level service, because generators are application/infra concerns.

## 7. Print architecture

Current anti-pattern:

```text
JpaTicketRepositoryAdapter
  -> ticket DB
  -> draw lookup
  -> session lookup
  -> outlet lookup
  -> print mapping
```

Target:

```text
GetTicketPrintPdfQueryHandler
  -> TicketReaderPort.findWithLinesById(ticketId)
  -> TicketPrintViewAssembler.assemble(ticket, locale)
  -> TicketReceiptFormatter.formatModel(view, verifyUrl)
  -> TicketPdfBuilder.buildReceiptPdf(model, qrBytes)
```

```text
GetTicketPrintEscPosQueryHandler
  -> TicketReaderPort.findWithLinesById(ticketId)
  -> TicketPrintViewAssembler.assemble(ticket, locale)
  -> TicketReceiptFormatter.formatModel(view, verifyUrl)
  -> EscPosReceiptBuilder.build(model, qrBytes?)
```

`JpaTicketRepositoryAdapter` keeps only persistence operations.

## 8. TicketPrintViewAssembler

Responsibilities:

- Load draw summary/labels.
- Load session.
- Load outlet.
- Load terminal.
- Resolve terminal label.
- Resolve locale.
- Resolve print zone.
- Call `TicketPrintViewMapper`.

Allowed dependencies:

```text
TicketReaderPort is used by query handler, not assembler if ticket is already loaded.
DrawLookupPort
SalesSessionReaderPort
OutletReaderPort
TerminalReaderPort
TicketPrintViewMapper
```

## 9. Print DTO target

`TicketPrintView` should avoid internal UUID display.

Target shape:

```java
public record TicketPrintView(
    String ticketCode,
    String publicCode,
    String terminalLabel,
    String outletName,
    String drawChannelCode,
    String drawChannelLabel,
    String drawWhenLabel,
    Instant createdAt,
    ZoneId zoneId,
    BigDecimal totalAmount,
    List<TicketPrintLine> lines
) {}
```

`TicketPrintLine` may keep:

```java
public record TicketPrintLine(
    String gameCode,
    BetType betType,
    Short betOption,
    String selection,
    BigDecimal stake,
    BigDecimal potentialPayout
) {}
```

## 10. Print formatting rules

Receipt lines must group by:

```text
gameCode + betType + betOption
```

For 58mm ESC/POS:

```text
NUMERO              MISE
21                 75.00
25                 75.00
```

For PDF / 80mm:

```text
JEU        NUMEROS      MISE   GAIN
```

A ticket must never display:

- ticket UUID
- draw UUID
- terminal UUID
- tenant UUID
- address UUID

Use human labels only.

## 11. PDF builder

PDF height must be dynamic.

Required behavior:

- Height grows with `model.lines().size()`.
- QR block included only if QR bytes exist.
- Do not cut content silently.
- Keep 80mm width default.
- Use monospace font for body rows.

## 12. Deprecated print endpoint

`GET /tenant/tickets/{ticketId}/print` returning base64 PDF as `text/plain` should be deprecated or removed.

Keep:

```text
GET /tenant/tickets/{ticketId}/print.pdf
GET /tenant/tickets/{ticketId}/print.escpos
```

## 13. Event publishing

`TicketPlacedEvent` is published only after commit.

Event id generation should use `IdGenerator`, not direct `UUID.randomUUID()`.

`TicketEventPublisherPort` is removed if unused.

## 14. Cross-domain boundaries

This change removes cross-domain calls from persistence adapters.

Allowed:

- application services and query handlers orchestrate read-only cross-domain ports.
- persistence adapters implement local persistence only.

Forbidden:

- `core.sales.infra.persistence.*` SQL JOIN on `draw_result`, `result_slot`, `draw`, `draw_channel`, `outlet`, `terminal`, `address`.
