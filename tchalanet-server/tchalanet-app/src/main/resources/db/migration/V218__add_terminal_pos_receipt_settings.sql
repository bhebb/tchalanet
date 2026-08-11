-- Terminal-scoped POS behavior. Physical printer pairing remains local to the
-- app installation; these values describe the server-visible terminal policy.
ALTER TABLE seller_terminal_settings
  ADD COLUMN IF NOT EXISTS quick_sale boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS receipt_printer_mode varchar(32) NOT NULL DEFAULT 'POS_DIRECT',
  ADD COLUMN IF NOT EXISTS receipt_paper_size varchar(32) NOT NULL DEFAULT 'RECEIPT_58MM',
  ADD COLUMN IF NOT EXISTS receipt_adapter_preference varchar(64);

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

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_seller_terminal_settings_printer_mode'
      AND conrelid = 'seller_terminal_settings'::regclass
  ) THEN
    ALTER TABLE seller_terminal_settings
      ADD CONSTRAINT chk_seller_terminal_settings_printer_mode
        CHECK (receipt_printer_mode IN ('AUTO', 'POS_DIRECT', 'SYSTEM_PDF'));
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_seller_terminal_settings_paper_size'
      AND conrelid = 'seller_terminal_settings'::regclass
  ) THEN
    ALTER TABLE seller_terminal_settings
      ADD CONSTRAINT chk_seller_terminal_settings_paper_size
        CHECK (receipt_paper_size IN ('RECEIPT_58MM', 'RECEIPT_80MM'));
  END IF;
END
$$;
