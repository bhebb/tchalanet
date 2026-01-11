-- V1002: Add id PK to pos_session_totals and keep session_id as FK
-- This migration adds a standard UUID primary slotKey `id` to pos_session_totals,
-- populates it for existing rows, sets a default generator, drops the old
-- PK on session_id and creates a unique index on session_id to preserve 1:1 semantics.
-- NOTE: test this migration on a staging DB before applying to production.

BEGIN;

-- 1) add id column if missing
ALTER TABLE pos_session_totals ADD COLUMN IF NOT EXISTS id uuid;

-- 2) populate id for existing rows
UPDATE pos_session_totals SET id = gen_random_uuid() WHERE id IS NULL;

-- 3) ensure default for new rows
ALTER TABLE pos_session_totals ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- 4) drop existing PK on session_id (name used by default when created as PRIMARY KEY)
ALTER TABLE pos_session_totals DROP CONSTRAINT IF EXISTS pos_session_totals_pkey;

-- 5) ensure id not null
ALTER TABLE pos_session_totals ALTER COLUMN id SET NOT NULL;

-- 6) add primary slotKey on id
ALTER TABLE pos_session_totals ADD CONSTRAINT pos_session_totals_pkey PRIMARY KEY (id);

-- 7) keep uniqueness on session_id to maintain 1:1 relation with pos_session
CREATE UNIQUE INDEX IF NOT EXISTS ux_pos_session_totals_session_id ON pos_session_totals(session_id);

COMMIT;

