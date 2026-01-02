-- V5: core game & draw (catalogue + draw planning/results)

-- GAME
CREATE TABLE IF NOT EXISTS game (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(32) NOT NULL,
    name varchar(128) NOT NULL,
    category varchar(32) NOT NULL,
    min_digits integer NOT NULL,
    max_digits integer NOT NULL,
    combination varchar(32) NOT NULL,
    description text,
    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    UNIQUE (code)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_game_updated_at') THEN
    CREATE TRIGGER trg_game_updated_at
      BEFORE UPDATE ON game
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- TENANT_GAME
CREATE TABLE IF NOT EXISTS tenant_game (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,
    tenant_id uuid NOT NULL REFERENCES tenant(id),
    game_id uuid NOT NULL REFERENCES game(id),
    enabled boolean NOT NULL DEFAULT true,
    display_name varchar(128),
    min_stake numeric(12, 2),
    max_stake numeric(12, 2),
    flags jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    UNIQUE (tenant_id, game_id)
);

CREATE INDEX IF NOT EXISTS ix_tenant_game_tenant_enabled ON tenant_game (tenant_id, enabled);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_tenant_game_updated_at') THEN
    CREATE TRIGGER trg_tenant_game_updated_at
      BEFORE UPDATE ON tenant_game
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- DRAW_CHANNEL
CREATE TABLE IF NOT EXISTS draw_channel (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,
    tenant_id uuid NOT NULL REFERENCES tenant(id),
    tenant_game_id uuid NOT NULL REFERENCES tenant_game(id),
    code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    timezone varchar(64) NOT NULL,
    draw_time time NOT NULL,
    cutoff_sec int NOT NULL DEFAULT 120,
    days_of_week varchar(32) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,
    external_provider varchar(32),
    external_game_key varchar(64),
    external_channel_code varchar(32),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS ix_draw_channel_tenant_active ON draw_channel (tenant_id, active, sort_order);
CREATE INDEX IF NOT EXISTS ix_draw_channel_external_slot
    ON draw_channel (external_provider, external_game_key, external_channel_code)
    WHERE active = true AND deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_channel_updated_at') THEN
    CREATE TRIGGER trg_draw_channel_updated_at
      BEFORE UPDATE ON draw_channel
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;


-- DRAW_RESULT
CREATE TABLE draw_result (
                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             version bigint NOT NULL DEFAULT 0,

                             channel_code varchar(64) NOT NULL,
                             draw_date date NOT NULL,

                             occurred_at timestamptz,
                             numbers_main jsonb NOT NULL,
                             numbers_extra jsonb,

                             quality varchar(16) NOT NULL,
                             status varchar(16) NOT NULL DEFAULT 'VALID',
                             source varchar(32) NOT NULL,

                             source_hash varchar(64),
                             raw_payload jsonb,

                             override_reason text,

                             fetched_at timestamptz NOT NULL DEFAULT now(),
                             created_at timestamptz NOT NULL DEFAULT now(),
                             created_by uuid,
                             updated_at timestamptz,
                             updated_by uuid,
                             deleted_at timestamptz,
                             CONSTRAINT uq_draw_result_channel_date UNIQUE (channel_code, draw_date)
);

CREATE INDEX ix_draw_result_channel_date ON draw_result(channel_code, draw_date);
CREATE INDEX ix_draw_result_status ON draw_result(status);
CREATE INDEX ix_draw_result_quality ON draw_result(quality);

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_result_updated_at') THEN
CREATE TRIGGER trg_draw_result_updated_at
    BEFORE UPDATE ON draw_result
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;


-- DRAW
C-- DRAW
CREATE TABLE IF NOT EXISTS draw (
                                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),

    draw_channel_id uuid NOT NULL REFERENCES draw_channel(id),
    -- FK vers résultat canonique (non-tenant) - null tant que pas RESULTED
    draw_result_id uuid REFERENCES draw_result(id),

    -- clé métier stable (jour local du channel)
    draw_date date NOT NULL,

    -- timestamps de planification
    scheduled_at timestamptz NOT NULL,
    cutoff_sec int NOT NULL DEFAULT 120,
    cutoff_at timestamptz NOT NULL,

    -- lifecycle timestamps (audit)
    opened_at timestamptz,
    closed_at timestamptz,
    resulted_at timestamptz,
    settled_at timestamptz,
    canceled_at timestamptz,
    cancel_reason text,

    status varchar(16) NOT NULL CHECK (status IN ('SCHEDULED','OPEN','CLOSED','RESULTED','SETTLED','CANCELED','ARCHIVED')),
    draw_source varchar(32),

    system_generated boolean NOT NULL DEFAULT true,
    locked boolean NOT NULL DEFAULT false,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

-- Unicité stable: 1 draw par (tenant, channel, date)
CREATE UNIQUE INDEX IF NOT EXISTS uq_draw_unique_day
    ON draw (tenant_id, draw_channel_id, draw_date)
    WHERE deleted_at IS NULL;

-- Accélère les listings / ops
CREATE INDEX IF NOT EXISTS ix_draw_tenant_date
    ON draw (tenant_id, draw_date DESC);

CREATE INDEX IF NOT EXISTS ix_draw_tenant_scheduled
    ON draw (tenant_id, scheduled_at);

-- OpenDue: status=SCHEDULED + window sur scheduled_at
CREATE INDEX IF NOT EXISTS ix_draw_status_scheduled_at
    ON draw (status, scheduled_at)
    WHERE deleted_at IS NULL AND locked = false;

-- CloseDue: status=OPEN + cutoff_at <= now
CREATE INDEX IF NOT EXISTS ix_draw_status_cutoff_at
    ON draw (status, cutoff_at)
    WHERE deleted_at IS NULL AND locked = false;

-- Attach/Apply: status=CLOSED + draw_result_id IS NULL (optionnel mais utile)
CREATE INDEX IF NOT EXISTS ix_draw_closed_missing_result
    ON draw (tenant_id, draw_channel_id, draw_date)
    WHERE deleted_at IS NULL AND status = 'CLOSED' AND draw_result_id IS NULL;

-- Settle: status=RESULTED + not locked
CREATE INDEX IF NOT EXISTS ix_draw_resulted_to_settle
    ON draw (tenant_id, draw_date)
    WHERE deleted_at IS NULL AND status = 'RESULTED' AND locked = false;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_draw_updated_at') THEN
CREATE TRIGGER trg_draw_updated_at
    BEFORE UPDATE ON draw
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

