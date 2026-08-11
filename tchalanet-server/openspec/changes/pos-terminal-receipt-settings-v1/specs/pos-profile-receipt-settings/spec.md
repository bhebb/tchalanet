# POS profile receipt settings

## ADDED Requirements

### Requirement: Terminal receipt policy is persisted
The system SHALL persist quick-sale and printer policy per `(tenant_id, seller_terminal_id)` with defaults of quick sale enabled, printer mode `POS_DIRECT`, and paper size `RECEIPT_58MM`.

#### Scenario: Existing terminal settings receive safe defaults
- **WHEN** the migration is applied to an existing `seller_terminal_settings` row
- **THEN** `quick_sale` is `true`, `receipt_printer_mode` is `POS_DIRECT`, and `receipt_paper_size` is `RECEIPT_58MM`

### Requirement: POS profile exposes and updates printer policy
The POS profile settings contract SHALL expose and partially update `quickSale`, `printerMode`, `paperSize`, and `adapterPreference` under `settings.receipt`.

#### Scenario: Seller updates direct POS printing
- **WHEN** the seller terminal updates `receiptPrinterMode` to `POS_DIRECT`
- **THEN** the profile returns `settings.receipt.printerMode` as `POS_DIRECT`
- **AND** no physical Bluetooth or Sunmi pairing address is persisted

### Requirement: Invalid printer policy is rejected
The system SHALL reject printer modes outside `AUTO`, `POS_DIRECT`, and `SYSTEM_PDF`, and paper sizes outside `RECEIPT_58MM` and `RECEIPT_80MM`.

#### Scenario: Unsupported paper size is submitted
- **WHEN** a settings update submits `receiptPaperSize` as `A4`
- **THEN** the update is rejected and the existing terminal policy is unchanged
