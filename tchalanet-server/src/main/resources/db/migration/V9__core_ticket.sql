-- V6: core ticket (ticket + ticket_line)

CREATE TABLE IF NOT EXISTS ticket (
                                      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    terminal_id uuid NOT NULL REFERENCES terminal(id),
    draw_id uuid REFERENCES draw(id),
    session_id uuid REFERENCES pos_session(id),

    ticket_code text NOT NULL UNIQUE,
    public_code varchar(32),
    created_at timestamptz NOT NULL DEFAULT now(),
    status varchar(16) NOT NULL,
    total_amount numeric(14, 2) NOT NULL,
    total_payout numeric(14,2) NOT NULL DEFAULT 0,

    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

ALTER TABLE ticket
    ADD CONSTRAINT chk_ticket_status
        CHECK (status IN ('PENDING', 'WON', 'LOST', 'PAID', 'VOID'));

CREATE INDEX IF NOT EXISTS ix_ticket_tenant_created
    ON ticket (tenant_id, created_at);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_ticket_updated_at') THEN
CREATE TRIGGER trg_ticket_updated_at
    BEFORE UPDATE ON ticket
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

CREATE TABLE IF NOT EXISTS ticket_line (
                                           id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id uuid NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    game_code varchar(32) NOT NULL REFERENCES game(code),
    selection text NOT NULL,
    stake numeric(12, 2) NOT NULL,
    odds_snapshot numeric(12, 4) NOT NULL,
    potential_payout numeric(14, 2) NOT NULL,
    bet_type varchar(20) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

CREATE INDEX IF NOT EXISTS ix_ticket_line_ticket ON ticket_line (ticket_id);
