# Design — Harden Critical Ticket Flows V1

## 1. Canonical ownership

`core.sales` owns all canonical ticket evidence. A ticket receipt is not a feature-specific UI layout; it is a sales proof. Therefore, `core.sales` must expose stable query models for receipt/print/message/verify.

## 2. Target class responsibilities

### `SellTicketCommandHandler`

Responsibilities:

- get current request context;
- call `SaleAcceptanceEvaluator.evaluateFinal(...)`;
- persist ticket aggregate;
- persist applied promotion snapshots;
- build backup from canonical receipt model;
- publish events and enqueue communication after commit;
- return `SellTicketResult`.

Must not:

- decide promotion rules;
- decide limit/autonomy rules;
- format receipt lines directly;
- call communication after commit using implicit `TchContext`.

### `SalePreparationOrchestrator`

Responsibilities:

- validate command;
- resolve POS frame;
- apply draw cutoff;
- prepare paid ticket lines;
- compute initial money;
- evaluate promotion conditions;
- apply promotion effects;
- compute final money;
- evaluate limits/autonomy on final risk where applicable;
- collect notices;
- produce `PreparedSale`.

### `TicketReceiptAssembler`

Responsibilities:

- convert `TicketPrintView` into canonical `TicketReceiptView`;
- carry sale snapshot labels and promotion info;
- normalize public/display code;
- expose draw/outlet/seller/terminal/ticket facts.

### `TicketReceiptPrintFormatter`

Responsibilities:

- build header lines;
- build draw section;
- build game sections and line rows;
- display Maryaj gratuit / promotion labels;
- build totals;
- build outlet and tenant footers;
- build verification block.

### `TicketPrintDocumentMapper`

Responsibilities:

- map sales receipt content to platform document content;
- select template key;
- select document format;
- map receipt line style to document line style;
- map QR payload to document asset;
- set paper/document options.

Must not:

- query catalogs;
- format money;
- decide labels;
- decide promotions;
- decide line grouping;
- decide tenant/outlet header/footer content.

### `TicketVerifyController`

Responsibilities:

- accept public request;
- validate public code shape;
- apply rate limit;
- call `TicketVerifyService`;
- add no-store/noindex headers;
- return `ApiResponse<TicketVerifyResponse>`.

### `VerifyTicketByPublicCodeQueryHandler`

Responsibilities:

- verify public code + verification code;
- apply visibility policy;
- resolve customer status;
- decide whether winning amount is visible;
- return safe public view.

## 3. Receipt model direction

Use sales-neutral receipt model types, not platform document types, in `core.sales.api`.

Example:

```java
public record TicketReceiptPrintContent(
    String title,
    List<TicketReceiptTextLine> headerLines,
    List<TicketReceiptSectionView> sections,
    List<TicketReceiptTextLine> totals,
    List<TicketReceiptTextLine> footerLines,
    TicketReceiptQrView qr,
    Locale locale,
    ZoneId timezone,
    Map<String, String> metadata
) {}

public record TicketReceiptTextLine(
    String text,
    TicketReceiptLineStyle style
) {}

public enum TicketReceiptLineStyle {
    NORMAL,
    BOLD,
    SMALL
}
```

## 4. Public verification model direction

Public verification must not expose internal status fields. It returns customer-safe facts:

```text
publicCode
displayCode
customerStatus
totalAmount
winningAmount when visible
draw info
outlet info
line info
```

Line info must include enough to display promotions:

```text
lineNumber
gameLabel
betTypeLabel
optionLabel
displaySelection
stake
potentialPayout
promotional
promotionLabel
```

## 5. Security and context

- Tenant-scoped POS actions use `TchRequestContext` and trusted operational context.
- Print/reprint/send are operational actions and must not bypass terminal/outlet/session validation.
- Public verification is `permitAll`, rate-limited, no-store, and noindex.

## 6. Eventing

`TicketPlacedEvent` must remain after-commit. It must carry enough line-level facts for downstream consumers to avoid recalculating promotions.

Consumers must be idempotent.

## 7. Production notes

- The current in-memory public rate limiter is acceptable for single-node V1 only.
- Multi-instance production requires Redis-backed or gateway-level distributed rate limiting.
- `X-Forwarded-For` is trusted only behind configured reverse proxy.
