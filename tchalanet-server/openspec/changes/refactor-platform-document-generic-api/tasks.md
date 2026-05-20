# Tasks

## 1. API contract

- [x] Replace `DocumentApi` specialized methods with `RenderedDocument render(DocumentRenderRequest request)`.
- [x] Add `DocumentRenderRequest`.
- [x] Add `RenderedDocument`.
- [x] Add `DocumentKind` and `DocumentFormat`.
- [x] Add `DocumentContent` sealed interface.
- [x] Add `ReceiptDocumentContent`.
- [x] Add `ReportDocumentContent`.
- [x] Add `GenericDocumentContent`.
- [x] Add shared primitives: `DocumentSection`, `DocumentLine`, `DocumentTable`, `DocumentAsset`, options and enums.

## 2. Internal implementation

- [x] Refactor `DefaultDocumentApi` to dispatch by `DocumentFormat`.
- [x] Move PDF rendering behind `PdfDocumentRenderer`.
- [x] Move ESC/POS rendering behind `EscPosDocumentRenderer`.
- [x] Move QR PNG rendering behind `QrPngDocumentRenderer`.
- [x] Keep PDFBox, ZXing and ESC/POS builder classes internal.

## 3. Migration

- [x] Replace direct `renderReceiptPdf(...)` calls.
- [x] Replace direct `renderReceiptEscPos(...)` calls.
- [x] Replace direct `renderQrPng(...)` calls.
- [x] Replace direct `renderQrEscPos(...)` calls.
- [x] Add a `SalesDocumentRequestFactory` or equivalent where sales needs receipts. *(implemented as `TicketReceiptDocumentRequestFactory` in `features.receipt.app`, used by both `ReceiptService` and `CashierService`.)*

## 4. Tests

- [x] Unit test `DefaultDocumentApi` dispatch.
- [x] Unit test PDF render with receipt content.
- [x] Unit test ESC/POS render with receipt content.
- [x] Unit test QR PNG render.
- [x] Unit test unsupported format/content combinations.

## 5. Documentation

- [x] Add `docs/platform/PLATFORM_DOCUMENT.md`. *(updated in-package at `tchalanet-platform/.../platform/document/PLATFORM_DOCUMENT.md`.)*
- [x] Document sales request construction example.
- [x] Document forbidden dependencies and naming rules.
