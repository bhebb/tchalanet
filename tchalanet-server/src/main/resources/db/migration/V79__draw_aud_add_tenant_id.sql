-- Add tenant_id to draw_aud so Envers insert has the expected column
ALTER TABLE public.draw_aud
  ADD COLUMN IF NOT EXISTS tenant_id uuid NULL;

-- Ensure ownership/permissions match other audit tables (no-op if already set)
ALTER TABLE public.draw_aud OWNER TO app_user;
GRANT ALL ON TABLE public.draw_aud TO app_user;

