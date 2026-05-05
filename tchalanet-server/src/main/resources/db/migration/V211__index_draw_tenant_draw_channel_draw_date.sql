CREATE UNIQUE INDEX uq_draw_tenant_channel_date_active
    ON draw (tenant_id, draw_channel_id, draw_date)
    WHERE deleted_at IS NULL;
