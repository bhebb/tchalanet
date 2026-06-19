-- Baseline: row-level security
ALTER TABLE address ENABLE ROW LEVEL SECURITY;
ALTER TABLE address FORCE ROW LEVEL SECURITY;
CREATE POLICY address_rls_all ON address
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY address_rls_select ON address
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE app_setting ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_setting FORCE ROW LEVEL SECURITY;
CREATE POLICY app_setting_rls_all ON app_setting
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY app_setting_rls_select ON app_setting
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE tenant_theme ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_theme FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_theme_rls_all ON tenant_theme
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY tenant_theme_rls_select ON tenant_theme
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE tenant_game ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_game FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_game_rls_all ON tenant_game
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY tenant_game_rls_select ON tenant_game
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE draw_channel ENABLE ROW LEVEL SECURITY;
ALTER TABLE draw_channel FORCE ROW LEVEL SECURITY;
CREATE POLICY draw_channel_rls_all ON draw_channel
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY draw_channel_rls_select ON draw_channel
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE draw_channel_game ENABLE ROW LEVEL SECURITY;
ALTER TABLE draw_channel_game FORCE ROW LEVEL SECURITY;
CREATE POLICY draw_channel_game_rls_all ON draw_channel_game
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY draw_channel_game_rls_select ON draw_channel_game
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE draw ENABLE ROW LEVEL SECURITY;
ALTER TABLE draw FORCE ROW LEVEL SECURITY;
CREATE POLICY draw_rls_all ON draw
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY draw_rls_select ON draw
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE sales_zone ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_zone FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_zone_rls_all ON sales_zone FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY sales_zone_rls_select ON sales_zone FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE pricing_odds ENABLE ROW LEVEL SECURITY;
ALTER TABLE pricing_odds FORCE ROW LEVEL SECURITY;
CREATE POLICY pricing_odds_rls_all ON pricing_odds
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY pricing_odds_rls_select ON pricing_odds
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));


ALTER TABLE tenant_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_user FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_user_rls_all ON tenant_user
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY tenant_user_rls_select ON tenant_user
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE page_model ENABLE ROW LEVEL SECURITY;
ALTER TABLE page_model FORCE ROW LEVEL SECURITY;
CREATE POLICY page_model_rls_all ON page_model
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY page_model_rls_select ON page_model
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

-- audit_event: tenant-scoped OR platform/global (tenant_id IS NULL) for system audits.
ALTER TABLE audit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_event_rls_all ON audit_event
  FOR ALL
  USING (
    (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );
CREATE POLICY audit_event_rls_select ON audit_event
  FOR SELECT
  USING (
    public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );

ALTER TABLE limit_assignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE limit_assignment FORCE ROW LEVEL SECURITY;
CREATE POLICY limit_assignment_rls_all ON limit_assignment
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY limit_assignment_rls_select ON limit_assignment
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));


ALTER TABLE draw_exposure ENABLE ROW LEVEL SECURITY;
ALTER TABLE draw_exposure FORCE ROW LEVEL SECURITY;
CREATE POLICY draw_exposure_rls_all ON draw_exposure
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY draw_exposure_rls_select ON draw_exposure
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE notification ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_rls_all ON notification
  FOR ALL
  USING (
    (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );

ALTER TABLE notification_delivery ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_delivery FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_delivery_rls_all ON notification_delivery
  FOR ALL
  USING (
    (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );

ALTER TABLE notification_preference ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preference FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_preference_rls_all ON notification_preference
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());

ALTER TABLE notification_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_template FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_template_rls_all ON notification_template
  FOR ALL
  USING (
    (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );

ALTER TABLE outbound_message ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbound_message FORCE ROW LEVEL SECURITY;
CREATE POLICY outbound_message_rls_all ON outbound_message
  FOR ALL
  USING (
    (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );

ALTER TABLE message_delivery_attempt ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_delivery_attempt FORCE ROW LEVEL SECURITY;
CREATE POLICY message_delivery_attempt_rls_all ON message_delivery_attempt
  FOR ALL
  USING (public.allow_platform_cross_tenant_select())
  WITH CHECK (public.allow_platform_cross_tenant_select());

ALTER TABLE message_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_template FORCE ROW LEVEL SECURITY;
CREATE POLICY message_template_rls_all ON message_template
  FOR ALL
  USING (
    (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );

ALTER TABLE tenant_communication_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_communication_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_communication_settings_rls_all ON tenant_communication_settings
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());

ALTER TABLE idempotency_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_record FORCE ROW LEVEL SECURITY;
CREATE POLICY idempotency_record_rls_all ON idempotency_record
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY idempotency_record_rls_select ON idempotency_record
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE processed_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE processed_event FORCE ROW LEVEL SECURITY;
CREATE POLICY processed_event_rls_all ON processed_event
  FOR ALL
  USING (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY processed_event_rls_select ON processed_event
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE stats_draw ENABLE ROW LEVEL SECURITY;
ALTER TABLE stats_draw FORCE ROW LEVEL SECURITY;
CREATE POLICY stats_draw_rls_all ON stats_draw
  FOR ALL
  USING (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY stats_draw_rls_select ON stats_draw
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE sales_ticket ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_ticket FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_ticket_rls_all ON sales_ticket
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY sales_ticket_rls_select ON sales_ticket
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE sales_ticket_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_ticket_line FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_ticket_line_rls_all ON sales_ticket_line
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY sales_ticket_line_rls_select ON sales_ticket_line
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE sales_ticket_charge ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_ticket_charge FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_ticket_charge_rls_all ON sales_ticket_charge
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY sales_ticket_charge_rls_select ON sales_ticket_charge
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

-- ─── Promotion domain ────────────────────────────────────────────────
ALTER TABLE promotion_campaign ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_campaign FORCE ROW LEVEL SECURITY;
CREATE POLICY promotion_campaign_rls_all ON promotion_campaign
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY promotion_campaign_rls_select ON promotion_campaign
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE promotion_rule ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_rule FORCE ROW LEVEL SECURITY;
CREATE POLICY promotion_rule_rls_all ON promotion_rule
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY promotion_rule_rls_select ON promotion_rule
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE promotion_rule_effect ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_rule_effect FORCE ROW LEVEL SECURITY;
CREATE POLICY promotion_rule_effect_rls_all ON promotion_rule_effect
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY promotion_rule_effect_rls_select ON promotion_rule_effect
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE promotion_rule_eligibility_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_rule_eligibility_line FORCE ROW LEVEL SECURITY;
CREATE POLICY promotion_rule_eligibility_line_rls_all ON promotion_rule_eligibility_line
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY promotion_rule_eligibility_line_rls_select ON promotion_rule_eligibility_line
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE promotion_decision ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_decision FORCE ROW LEVEL SECURITY;
CREATE POLICY promotion_decision_rls_all ON promotion_decision
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY promotion_decision_rls_select ON promotion_decision
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE applied_promotion_snapshot ENABLE ROW LEVEL SECURITY;
ALTER TABLE applied_promotion_snapshot FORCE ROW LEVEL SECURITY;
CREATE POLICY applied_promotion_snapshot_rls_all ON applied_promotion_snapshot
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY applied_promotion_snapshot_rls_select ON applied_promotion_snapshot
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

-- ─────────────────────────────────────────────────────────────────────────────
-- business_day_override (tenant-scoped)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE business_day_override ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_day_override FORCE ROW LEVEL SECURITY;
CREATE POLICY business_day_override_rls_all ON business_day_override
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY business_day_override_rls_select ON business_day_override
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

-- ─────────────────────────────────────────────────────────────────────────────
-- result_slot_calendar_override is global (no tenant_id) — same RLS posture
-- as result_slot itself (no RLS). Writes are gated by the SUPER_ADMIN
-- controller layer.
-- ─────────────────────────────────────────────────────────────────────────────
