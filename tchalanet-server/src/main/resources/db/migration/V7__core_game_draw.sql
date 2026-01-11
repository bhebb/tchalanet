-- ============================================
-- Tchalanet • Core Draw Pipeline (From Scratch)
-- Tables:
--   1) game (global)
--   2) tenant_game (tenant-scoped)  [aka game_tenant]
--   3) result_slot (global)
--   4) draw_channel (tenant-scoped)  -> FK result_slot
--   5) draw_channel_game (tenant-scoped)
--   6) draw_result (global)  [aka sraw_draw_result]
-- Seed:
--   - Haiti games
--   - result slots (NY/FL/GA/TX + TN inactive optional)
--   - default tenant channels (HT_*) linked to result_slot
--   - link all games to all channels
-- ============================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- 1) game (global)
-- =========================
CREATE TABLE IF NOT EXISTS game (
                                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    code varchar(32) NOT NULL UNIQUE,     -- HT_BOLET, HT_MARYAJ, ...
    name varchar(128) NOT NULL,

    category varchar(32) NOT NULL,        -- ex: HAITI
    combination varchar(32) NOT NULL,     -- SINGLE, PAIR_UNORDERED, EXACT, ...
    min_digits int NOT NULL,
    max_digits int NOT NULL,

    description text,
    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_game_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_game_updated_at BEFORE UPDATE ON game FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_game_active_sort
    ON game (active, sort_order, code)
    WHERE deleted_at IS NULL;

-- =========================
-- 2) tenant_game (tenant-scoped)
-- =========================
CREATE TABLE IF NOT EXISTS tenant_game (
                                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    game_id uuid NOT NULL REFERENCES game(id),

    enabled boolean NOT NULL DEFAULT true,
    display_name varchar(128),

    min_stake numeric(12,2),
    max_stake numeric(12,2),

    flags jsonb NOT NULL DEFAULT '{}'::jsonb, -- odds/multipliers/etc

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT uq_tenant_game UNIQUE (tenant_id, game_id)
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_tenant_game_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_tenant_game_updated_at BEFORE UPDATE ON tenant_game FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_tenant_game_tenant_enabled
    ON tenant_game (tenant_id, enabled)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tenant_game_game
    ON tenant_game (game_id)
    WHERE deleted_at IS NULL;

-- =========================
-- 3) result_slot (global) = "un résultat attendu"
-- =========================
CREATE TABLE IF NOT EXISTS result_slot (
                                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    slot_key varchar(32) NOT NULL UNIQUE,          -- NY_MID, TX_2212, ...
    provider varchar(16) NOT NULL,            -- NY/FL/GA/TN/TX
    timezone varchar(64) NOT NULL,            -- America/New_York, America/Chicago, ...
    draw_time time NOT NULL,                  -- heure locale du slot
    days_of_week varchar(32) NOT NULL DEFAULT 'MON-SUN', -- convention simple MVP

    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,

    -- config source (optionnel mais utile pour debug/uniformiser)
    -- ex: {"pick3":{"game_code":"US_NY_NUM3_MID","external_key":"NUMBERS"}, "pick4":{...}}
    source_cfg jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- projection Haïti (global, par slot) - MVP
    projection_cfg jsonb NOT NULL DEFAULT '{}'::jsonb,

    notes text,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT ck_result_slot_provider CHECK (provider IN ('NY','FL','GA','TN','TX'))
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_result_slot_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_result_slot_updated_at BEFORE UPDATE ON result_slot FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_result_slot_active_sort
    ON result_slot (active, sort_order, slot_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_result_slot_provider_time
    ON result_slot (provider, draw_time)
    WHERE deleted_at IS NULL;

-- =========================
-- 4) draw_channel (tenant-scoped) = calendrier + vente
--     -> référence result_slot (pivot global)
-- =========================
CREATE TABLE IF NOT EXISTS draw_channel (
                                            id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),

    code varchar(64) NOT NULL,                -- HT_NY_MID, ...
    name varchar(128) NOT NULL,

    result_slot_id uuid NOT NULL REFERENCES result_slot(id),

    -- calendrier tenant (peut diverger du slot si besoin, mais MVP: aligné)
    timezone varchar(64) NOT NULL,            -- souvent = slot.timezone
    draw_time time NOT NULL,                  -- souvent = slot.draw_time
    cutoff_sec int NOT NULL DEFAULT 120,
    days_of_week varchar(32) NOT NULL,        -- MON-SUN / CSV

    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,

    flags jsonb NOT NULL DEFAULT '{}'::jsonb,
    notes text,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT uq_draw_channel_tenant_code UNIQUE (tenant_id, code)
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_channel_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_draw_channel_updated_at BEFORE UPDATE ON draw_channel FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_draw_channel_tenant_active
    ON draw_channel (tenant_id, active, sort_order)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_draw_channel_tenant_slot
    ON draw_channel (tenant_id, result_slot_id)
    WHERE deleted_at IS NULL;

-- =========================
-- 5) draw_channel_game (tenant-scoped)
-- =========================
CREATE TABLE IF NOT EXISTS draw_channel_game (
                                                 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    draw_channel_id uuid NOT NULL REFERENCES draw_channel(id),
    game_id uuid NOT NULL REFERENCES game(id),

    enabled boolean NOT NULL DEFAULT true,
    flags jsonb NOT NULL DEFAULT '{}'::jsonb,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT uq_draw_channel_game UNIQUE (tenant_id, draw_channel_id, game_id)
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_channel_game_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_draw_channel_game_updated_at BEFORE UPDATE ON draw_channel_game FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_dcg_tenant_channel
    ON draw_channel_game (tenant_id, draw_channel_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_dcg_tenant_game
    ON draw_channel_game (tenant_id, game_id)
    WHERE deleted_at IS NULL;

-- =========================
-- 6) draw_result (global) = vérité externe + projection HA
--     (aka sraw_draw_result)
-- =========================
CREATE TABLE IF NOT EXISTS draw_result (
                                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    result_slot_id uuid NOT NULL REFERENCES result_slot(id),
    occurred_at timestamptz NOT NULL,           -- instant source

    source_result jsonb NOT NULL,               -- payload normalisé (pick3/pick4)
    haiti_result jsonb NOT NULL,                -- lots HA (lot1..lot4)
    raw_payload jsonb,
    flags jsonb NOT NULL DEFAULT '{}'::jsonb,

    status varchar(16) NOT NULL DEFAULT 'PROVISIONAL', -- PROVISIONAL/FINAL/ERROR
    quality varchar(16),
    source varchar(32),                         -- API/MANUAL/IMPORT
    source_hash varchar(64),

    fetched_at timestamptz NOT NULL DEFAULT now(),

    override_reason text,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT ck_draw_result_status CHECK (status IN ('PROVISIONAL','FINAL','ERROR'))
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_result_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_draw_result_updated_at BEFORE UPDATE ON draw_result FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_draw_result_slot_time
    ON draw_result(result_slot_id, occurred_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_draw_result_slot_time
    ON draw_result(result_slot_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_draw_result_status
    ON draw_result(status)
    WHERE deleted_at IS NULL;

ALTER TABLE draw_result
    ADD CONSTRAINT chk_draw_result_haiti_lots
        CHECK (
            haiti_result ? 'lot1' AND
    haiti_result ? 'lot2' AND
    haiti_result ? 'lot3' AND
    haiti_result ? 'lot4'
    );

-- =========================
-- 7) draw (tenant-scoped aggregate)
-- =========================
CREATE TABLE IF NOT EXISTS draw (
                                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    draw_channel_id uuid NOT NULL REFERENCES draw_channel(id),

    -- date "locale" du channel (ZonedDateTime(slot.timezone).toLocalDate())
    draw_date date NOT NULL,

    scheduled_at timestamptz NOT NULL,
    cutoff_at timestamptz NOT NULL,

    opened_at timestamptz,
    closed_at timestamptz,
    resulted_at timestamptz,
    settled_at timestamptz,
    canceled_at timestamptz,
    cancel_reason text,

    status varchar(16) NOT NULL
    CHECK (status IN ('SCHEDULED','OPEN','CLOSED','RESULTED','SETTLED','CANCELED', 'ARCHIVED')),

    -- le draw référence le résultat externe (global) qu'il applique
    draw_result_id uuid REFERENCES draw_result(id),

    system_generated boolean NOT NULL DEFAULT true,
    locked boolean NOT NULL DEFAULT false,

    -- Ops override / audit léger
    result_source varchar(16),               -- AUTO/OPS
    result_override_reason text,
    result_overridden_at timestamptz,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    CONSTRAINT uq_draw_tenant_channel_date UNIQUE (tenant_id, draw_channel_id, draw_date)
    );

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_updated_at') THEN
    EXECUTE 'CREATE TRIGGER trg_draw_updated_at BEFORE UPDATE ON draw FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
END IF;
END$$;

-- =========================
-- Indexes (ops + schedulers)
-- =========================

-- Listings / ops
CREATE INDEX IF NOT EXISTS ix_draw_tenant_date
    ON draw (tenant_id, draw_date DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_draw_tenant_scheduled
    ON draw (tenant_id, scheduled_at)
    WHERE deleted_at IS NULL;

-- Due windows (open/close)
CREATE INDEX IF NOT EXISTS ix_draw_open_due
    ON draw (scheduled_at)
    WHERE deleted_at IS NULL
    AND locked = false
    AND status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS ix_draw_close_due
    ON draw (cutoff_at)
    WHERE deleted_at IS NULL
    AND locked = false
    AND status = 'OPEN';

-- Processing states
CREATE INDEX IF NOT EXISTS ix_draw_status_scheduled_at
    ON draw (status, scheduled_at)
    WHERE deleted_at IS NULL
    AND locked = false;

CREATE INDEX IF NOT EXISTS ix_draw_status_cutoff_at
    ON draw (status, cutoff_at)
    WHERE deleted_at IS NULL
    AND locked = false;

-- Apply results: draws closed but missing result (fast join slotKey)
CREATE INDEX IF NOT EXISTS ix_draw_closed_missing_result
    ON draw (tenant_id, draw_channel_id, draw_date)
    WHERE deleted_at IS NULL
    AND status = 'CLOSED'
    AND draw_result_id IS NULL;

-- Settlement: resulted -> settle
CREATE INDEX IF NOT EXISTS ix_draw_resulted_to_settle
    ON draw (tenant_id, draw_date)
    WHERE deleted_at IS NULL
    AND status = 'RESULTED'
    AND locked = false;

-- ============================================
-- SEED BASE (moved)
-- The seed data (games, tenant_game, result_slot, draw_channel, draw_channel_game)
-- was extracted and moved to V46__seed_core_game_draw.sql to separate structure and data migrations.
-- Apply V46__seed_core_game_draw.sql after V7 to populate seed data.
-- ============================================
