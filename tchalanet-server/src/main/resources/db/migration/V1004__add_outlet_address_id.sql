-- Add optional foreign key to address for physical outlets
-- Adds address_id uuid column referencing address(id)

ALTER TABLE outlet
  ADD COLUMN IF NOT EXISTS address_id uuid;

-- Add foreign key constraint if not exists
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'outlet' AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'address_id') THEN
    ALTER TABLE outlet
      ADD CONSTRAINT fk_outlet_address_id FOREIGN KEY (address_id) REFERENCES address(id);
  END IF;
END$$;

-- Index for faster lookups
CREATE INDEX IF NOT EXISTS ix_outlet_address_id ON outlet(address_id);

-- Also update the audit table to carry the address_id for historical records
ALTER TABLE public.outlet_aud
  ADD COLUMN IF NOT EXISTS address_id uuid;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'outlet_aud' AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'address_id') THEN
    ALTER TABLE public.outlet_aud
      ADD CONSTRAINT fk_outlet_aud_address_id FOREIGN KEY (address_id) REFERENCES public.address(id);
  END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_outlet_aud_address_id ON public.outlet_aud(address_id);
