-- Add missing audit columns to draw_result_aud so Envers can write the full audit row
-- Ensure columns that come from BaseTenantEntity / AuditableEntity and entity fields exist

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS tenant_id uuid NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS occurred_at timestamptz NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS created_at timestamptz NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS created_by uuid NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS updated_by uuid NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS deleted_at timestamptz NULL;

ALTER TABLE public.draw_result_aud
  ADD COLUMN IF NOT EXISTS version bigint NULL;

-- Ensure ownership/permissions match other audit tables (no-op if already set)
ALTER TABLE public.draw_result_aud OWNER TO app_user;
GRANT ALL ON TABLE public.draw_result_aud TO app_user;

