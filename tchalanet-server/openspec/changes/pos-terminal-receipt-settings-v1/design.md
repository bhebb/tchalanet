# Design

The pre-go-live `V100__create_core_tables.sql` definition of
`seller_terminal_settings` receives:

- `quick_sale`, default `false`;
- `receipt_printer_mode`, one of `AUTO`, `POS_DIRECT`, `SYSTEM_PDF`, default `AUTO`;
- `receipt_paper_size`, one of `RECEIPT_58MM`, `RECEIPT_80MM`, default `RECEIPT_80MM`;
- nullable `receipt_adapter_preference` for a stable adapter identifier, not a Bluetooth address.

The POS profile API returns these fields under `settings.receipt` and accepts partial updates. The mobile app uses `AUTO` without opening a PDF automatically: direct adapters are attempted first and PDF is an explicit/manual fallback. Physical pairing remains local to `(tenant, seller terminal, app installation)`.
