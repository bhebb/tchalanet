# platform-document Specification

## ADDED Requirements

### Requirement: Generic document rendering API

`platform.document` SHALL expose a single Java API method for document rendering.

```java
RenderedDocument render(DocumentRenderRequest request);
```

#### Scenario: Render PDF receipt

- **GIVEN** a `DocumentRenderRequest` with `format = PDF`
- **AND** `kind = RECEIPT`
- **AND** `content` is `ReceiptDocumentContent`
- **WHEN** `DocumentApi.render(request)` is called
- **THEN** the PDF renderer SHALL produce a `RenderedDocument`
- **AND** `RenderedDocument.contentType` SHALL be `application/pdf`
- **AND** the returned bytes SHALL not be empty

#### Scenario: Render ESC/POS receipt

- **GIVEN** a `DocumentRenderRequest` with `format = ESC_POS`
- **AND** `kind = RECEIPT`
- **AND** `content` is `ReceiptDocumentContent`
- **WHEN** `DocumentApi.render(request)` is called
- **THEN** the ESC/POS renderer SHALL produce a `RenderedDocument`
- **AND** `RenderedDocument.contentType` SHALL be `application/octet-stream`
- **AND** the returned bytes SHALL include printer initialization commands

#### Scenario: Render QR PNG

- **GIVEN** a `DocumentRenderRequest` with `format = PNG`
- **AND** a QR asset is present
- **WHEN** `DocumentApi.render(request)` is called
- **THEN** the QR renderer SHALL produce a `RenderedDocument`
- **AND** `RenderedDocument.contentType` SHALL be `image/png`

### Requirement: Domain-agnostic content model

`DocumentRenderRequest` SHALL use platform-owned model classes only.

The content model SHALL NOT reference `Ticket`, `Payout`, `Draw`, `SalesSession`, `Outlet`, or any core/feature internal type.

#### Scenario: Sales builds a document request

- **GIVEN** sales has a `SellTicketResult`
- **WHEN** sales needs a printable receipt
- **THEN** sales SHALL map its result into a `DocumentRenderRequest`
- **AND** pass it to `DocumentApi.render(request)`
- **AND** `platform.document` SHALL not import sales classes

### Requirement: Typed content variants

`DocumentRenderRequest` SHALL carry a `DocumentContent` payload.

`DocumentContent` SHALL support at least:

- `ReceiptDocumentContent`
- `ReportDocumentContent`
- `GenericDocumentContent`

#### Scenario: Avoid low-level block explosion

- **GIVEN** a sales receipt with header, sections, totals and footer
- **WHEN** sales builds the request
- **THEN** sales MAY use `ReceiptDocumentContent`
- **AND** sales SHALL NOT be required to create a long flat list of low-level blocks

### Requirement: Internal renderer isolation

PDF, ESC/POS and QR implementation classes SHALL live under `platform.document.internal`.

#### Scenario: Consumer imports platform document

- **GIVEN** a core or feature module needs document rendering
- **WHEN** it imports document types
- **THEN** it SHALL import only `platform.document.api` or `platform.document.api.model`
- **AND** it SHALL NOT import `platform.document.internal`

## MODIFIED Requirements

### Requirement: Existing specialized rendering methods are removed

The old specialized methods SHALL be replaced by `render(DocumentRenderRequest)`:

- `renderReceiptPdf(...)`
- `renderReceiptEscPos(...)`
- `renderQrPng(...)`
- `renderQrEscPos(...)`

#### Scenario: Old caller migration

- **GIVEN** a caller uses an old specialized method
- **WHEN** this change is implemented
- **THEN** the caller SHALL create a `DocumentRenderRequest`
- **AND** call `DocumentApi.render(request)` instead

## REMOVED Requirements

### Requirement: Raw byte array API response

`DocumentApi` SHALL NOT return raw `byte[]` directly.

It SHALL return `RenderedDocument` so consumers receive content bytes plus content type, filename and format.
