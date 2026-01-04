-- V1001: add event_id and processed_at to stats_event_log to match JPA entity
ALTER TABLE public.stats_event_log
  ADD COLUMN IF NOT EXISTS event_id uuid;

ALTER TABLE public.stats_event_log
  ADD COLUMN IF NOT EXISTS processed_at timestamptz;

-- Backfill event_id from existing id where present
UPDATE public.stats_event_log SET event_id = id WHERE event_id IS NULL;
-- Backfill processed_at from occurred_at for historical rows
UPDATE public.stats_event_log SET processed_at = occurred_at WHERE processed_at IS NULL;

-- Make event_id not null and add unique index for idempotence checks
ALTER TABLE public.stats_event_log ALTER COLUMN event_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_stats_event_log_event_id ON public.stats_event_log(event_id);

-- Optional: owner/permissions
ALTER TABLE public.stats_event_log OWNER TO app_user;
GRANT ALL ON TABLE public.stats_event_log TO app_user;

