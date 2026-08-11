-- Default POS terminals to the field-tested 58mm direct-print flow.
-- V218 is already versioned/applied in some environments; keep the default
-- change in a new migration to avoid Flyway checksum drift.
ALTER TABLE seller_terminal_settings
  ALTER COLUMN quick_sale SET DEFAULT true,
  ALTER COLUMN receipt_printer_mode SET DEFAULT 'POS_DIRECT',
  ALTER COLUMN receipt_paper_size SET DEFAULT 'RECEIPT_58MM';

UPDATE seller_terminal_settings
SET quick_sale = true,
    receipt_printer_mode = 'POS_DIRECT',
    receipt_paper_size = 'RECEIPT_58MM'
WHERE quick_sale = false
  AND receipt_printer_mode = 'AUTO'
  AND receipt_paper_size = 'RECEIPT_80MM';
