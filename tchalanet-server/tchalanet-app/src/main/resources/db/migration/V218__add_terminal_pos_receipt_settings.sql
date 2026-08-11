-- Terminal-scoped POS behavior. Physical printer pairing remains local to the
-- app installation; these values describe the server-visible terminal policy.
ALTER TABLE seller_terminal_settings
  ADD COLUMN quick_sale boolean NOT NULL DEFAULT false,
  ADD COLUMN receipt_printer_mode varchar(32) NOT NULL DEFAULT 'AUTO',
  ADD COLUMN receipt_paper_size varchar(32) NOT NULL DEFAULT 'RECEIPT_80MM',
  ADD COLUMN receipt_adapter_preference varchar(64);

ALTER TABLE seller_terminal_settings
  ADD CONSTRAINT chk_seller_terminal_settings_printer_mode
    CHECK (receipt_printer_mode IN ('AUTO', 'POS_DIRECT', 'SYSTEM_PDF')),
  ADD CONSTRAINT chk_seller_terminal_settings_paper_size
    CHECK (receipt_paper_size IN ('RECEIPT_58MM', 'RECEIPT_80MM'));
