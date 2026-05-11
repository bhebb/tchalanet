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

ALTER TABLE outlet ENABLE ROW LEVEL SECURITY;
ALTER TABLE outlet FORCE ROW LEVEL SECURITY;
CREATE POLICY outlet_rls_all ON outlet
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY outlet_rls_select ON outlet
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE terminal ENABLE ROW LEVEL SECURITY;
ALTER TABLE terminal FORCE ROW LEVEL SECURITY;
CREATE POLICY terminal_rls_all ON terminal
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY terminal_rls_select ON terminal
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE sales_session ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_session FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_session_rls_all ON sales_session
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY sales_session_rls_select ON sales_session
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE sales_session_totals ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales_session_totals FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_session_totals_rls_all ON sales_session_totals
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY sales_session_totals_rls_select ON sales_session_totals
  FOR SELECT
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

ALTER TABLE ticket ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket FORCE ROW LEVEL SECURITY;
CREATE POLICY ticket_rls_all ON ticket
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY ticket_rls_select ON ticket
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE payout ENABLE ROW LEVEL SECURITY;
ALTER TABLE payout FORCE ROW LEVEL SECURITY;
CREATE POLICY payout_rls_all ON payout
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY payout_rls_select ON payout
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

ALTER TABLE autonomy_policy_rule ENABLE ROW LEVEL SECURITY;
ALTER TABLE autonomy_policy_rule FORCE ROW LEVEL SECURITY;
CREATE POLICY autonomy_policy_rule_rls_all ON autonomy_policy_rule
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY autonomy_policy_rule_rls_select ON autonomy_policy_rule
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

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

ALTER TABLE approval_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_request FORCE ROW LEVEL SECURITY;
CREATE POLICY approval_request_rls_all ON approval_request
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY approval_request_rls_select ON approval_request
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

ALTER TABLE ledger_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE ledger_entry FORCE ROW LEVEL SECURITY;
CREATE POLICY ledger_entry_rls_all ON ledger_entry
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY ledger_entry_rls_select ON ledger_entry
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
