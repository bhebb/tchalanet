-- Add missing audit columns to draw_aud so Envers can write the full audit row
-- This is required because the entity base class defines created_at/created_by/updated_at/updated_by/deleted_at/version

ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS created_at timestamptz NULL;

ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS created_by uuid NULL;

ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NULL;

ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS updated_by uuid NULL;

ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS deleted_at timestamptz NULL;

ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS version bigint NULL;

-- Ensure ownership/permissions match other audit tables (no-op if already set)
ALTER TABLE public.draw_aud OWNER TO app_user;
GRANT ALL ON TABLE public.draw_aud TO app_user;

