-- V999: normalize pos_session / pos_session_aud status values and constraints
-- Fixes mismatch between Java enum mapping and audit table CHECK constraint

-- 1) normalize existing values (in case older migrations used OPENED)
UPDATE public.pos_session SET status = 'OPEN' WHERE status = 'OPENED';
UPDATE public.pos_session SET status = 'CLOSED' WHERE status = 'CLOSED' AND status <> 'CLOSED'; -- noop (kept for symmetry)
UPDATE public.pos_session_aud SET status = 'OPEN' WHERE status = 'OPENED';

-- 2) ensure main table constraint allows SETTLED as well
ALTER TABLE public.pos_session DROP CONSTRAINT IF EXISTS pos_session_status_check;
ALTER TABLE public.pos_session
  ADD CONSTRAINT pos_session_status_check CHECK (status IN ('OPEN','CLOSED','SETTLED'));

-- 3) fix audit table constraint to match runtime values
ALTER TABLE public.pos_session_aud DROP CONSTRAINT IF EXISTS pos_session_aud_status_check;
ALTER TABLE public.pos_session_aud
  ADD CONSTRAINT pos_session_aud_status_check CHECK (status IN ('OPEN','CLOSED','SETTLED'));

-- 4) Optionally, refresh permissions (keeps consistency with other migrations)
ALTER TABLE public.pos_session OWNER TO app_user;
GRANT ALL ON TABLE public.pos_session TO app_user;
ALTER TABLE public.pos_session_aud OWNER TO app_user;
GRANT ALL ON TABLE public.pos_session_aud TO app_user;

