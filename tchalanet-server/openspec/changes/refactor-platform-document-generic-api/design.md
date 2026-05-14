# Design: PLATFORM_DOCUMENT generic request model

## Decision

Use one generic `DocumentApi.render(DocumentRenderRequest)` method.

Use a typed content model instead of only `List<DocumentBlock>`.

```text
DocumentRenderRequest
  kind
  format
  title
  content: DocumentContent
  assets
  options
  locale
  metadata
```

`DocumentContent` is a sealed interface with compact variants:

```text
ReceiptDocumentContent
ReportDocumentContent
GenericDocumentContent
```

## Rationale

A raw block list is flexible but can become long and repetitive. Sales and payout would have to manually assemble many rendering primitives for common receipt structures.

The typed content model keeps the API generic while reducing boilerplate:

- receipt callers provide header, sections, totals, footer;
- report callers provide summary, tables, footer;
- uncommon layouts use generic sections.

The renderer can normalize all content into an internal render tree before producing PDF, ESC/POS, or PNG.

## Boundary

`platform.document` must not know `Ticket`, `Payout`, `Draw`, `SalesSession`, `Outlet` domain models, or any use case-specific rules.

Domain modules may create small factories/assemblers such as:

```text
core.sales.internal.application.service.SalesDocumentRequestFactory
features.pos.receipt.app.PosReceiptDocumentFactory
core.payout.internal.application.service.PayoutDocumentRequestFactory
```

These factories are consumers of `platform.document.api.model`, not part of platform.

## Internal rendering flow

```text
DefaultDocumentApi.render(request)
  -> validate request
  -> dispatch by DocumentFormat
  -> renderer normalizes DocumentContent
  -> renderer writes bytes
  -> return RenderedDocument
```

## Error handling

Unsupported combinations must fail explicitly:

- `DocumentFormat.PNG` with full report content may be unsupported.
- `ESC_POS` with wide tables may be unsupported or degraded.
- Missing QR asset for QR-only render should return a controlled problem.

The exact exception type can reuse the project `ProblemRest` pattern in web flows, but the API itself should remain Java-level and not expose Spring MVC types.
