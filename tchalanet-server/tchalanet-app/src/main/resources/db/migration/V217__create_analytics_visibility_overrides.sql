CREATE TABLE analytics_visibility_override (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type VARCHAR(32) NOT NULL,
    tenant_id UUID,
    seller_terminal_id UUID,
    draw_id UUID,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_by UUID NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_analytics_visibility_override_dates CHECK (from_date <= to_date),
    CONSTRAINT chk_analytics_visibility_override_reason CHECK (length(trim(reason)) >= 10)
);

CREATE INDEX ix_analytics_visibility_override_scope
    ON analytics_visibility_override
        (scope_type, tenant_id, seller_terminal_id, draw_id, from_date, to_date);

GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_visibility_override TO app_user;
