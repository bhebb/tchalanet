-- Baseline: triggers and technical SQL functions
CREATE TRIGGER trg_tenant__set_updated_at BEFORE UPDATE ON tenant FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_address__set_updated_at BEFORE UPDATE ON address FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_app_user__set_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_user_preference__set_updated_at BEFORE UPDATE ON user_preference FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_permission__set_updated_at BEFORE UPDATE ON permission FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_app_role__set_updated_at BEFORE UPDATE ON app_role FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_tenant_user__set_updated_at BEFORE UPDATE ON tenant_user FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_app_setting__set_updated_at BEFORE UPDATE ON app_setting FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_i18n_override__set_updated_at BEFORE UPDATE ON i18n_override FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_theme_preset__set_updated_at BEFORE UPDATE ON theme_preset FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_tenant_theme__set_updated_at BEFORE UPDATE ON tenant_theme FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_game__set_updated_at BEFORE UPDATE ON game FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_tenant_game__set_updated_at BEFORE UPDATE ON tenant_game FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_result_slot__set_updated_at BEFORE UPDATE ON result_slot FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_draw_channel__set_updated_at BEFORE UPDATE ON draw_channel FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_draw_channel_game__set_updated_at BEFORE UPDATE ON draw_channel_game FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_draw_result__set_updated_at BEFORE UPDATE ON draw_result FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_draw__set_updated_at BEFORE UPDATE ON draw FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_outlet__set_updated_at BEFORE UPDATE ON outlet FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_terminal__set_updated_at BEFORE UPDATE ON terminal FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_sales_session__set_updated_at BEFORE UPDATE ON sales_session FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_sales_session_totals__set_updated_at BEFORE UPDATE ON sales_session_totals FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_pricing_odds__set_updated_at BEFORE UPDATE ON pricing_odds FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_ticket__set_updated_at BEFORE UPDATE ON ticket FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_ticket_line__set_updated_at BEFORE UPDATE ON ticket_line FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_payout__set_updated_at BEFORE UPDATE ON payout FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_billing_plan__set_updated_at BEFORE UPDATE ON billing_plan FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_tenant_subscription__set_updated_at BEFORE UPDATE ON tenant_subscription FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_page_model_template__set_updated_at BEFORE UPDATE ON page_model_template FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_page_model__set_updated_at BEFORE UPDATE ON page_model FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_audit_event__set_updated_at BEFORE UPDATE ON audit_event FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_autonomy_policy_rule__set_updated_at BEFORE UPDATE ON autonomy_policy_rule FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_limit_definition__set_updated_at BEFORE UPDATE ON limit_definition FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_limit_assignment__set_updated_at BEFORE UPDATE ON limit_assignment FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_draw_exposure__set_updated_at BEFORE UPDATE ON draw_exposure FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_ledger_entry__set_updated_at BEFORE UPDATE ON ledger_entry FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_tchala_entry__set_updated_at BEFORE UPDATE ON tchala_entry FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_notification__set_updated_at BEFORE UPDATE ON notification FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_notification_delivery__set_updated_at BEFORE UPDATE ON notification_delivery FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_notification_preference__set_updated_at BEFORE UPDATE ON notification_preference FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_idempotency_record__set_updated_at BEFORE UPDATE ON idempotency_record FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE OR REPLACE FUNCTION public.increment_draw_exposure(
  p_tenant_id uuid,
  p_draw_id uuid,
  p_scope_type varchar,
  p_scope_id uuid,
  p_bet_type varchar,
  p_selection_key varchar,
  p_stake numeric,
  p_potential_payout numeric,
  p_event_id uuid,
  p_event_at timestamptz
) RETURNS void AS $$
BEGIN
  INSERT INTO draw_exposure (
    tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key,
    stake_total, sales_count, potential_payout_total, last_event_id, last_event_at
  ) VALUES (
    p_tenant_id, p_draw_id, p_scope_type, p_scope_id, p_bet_type, p_selection_key,
    p_stake, 1, p_potential_payout, p_event_id, p_event_at
  )
  ON CONFLICT (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)
  DO UPDATE SET
    stake_total = draw_exposure.stake_total + EXCLUDED.stake_total,
    sales_count = draw_exposure.sales_count + 1,
    potential_payout_total = draw_exposure.potential_payout_total + EXCLUDED.potential_payout_total,
    last_event_id = EXCLUDED.last_event_id,
    last_event_at = EXCLUDED.last_event_at,
    updated_at = now();
END;
$$ LANGUAGE plpgsql;
