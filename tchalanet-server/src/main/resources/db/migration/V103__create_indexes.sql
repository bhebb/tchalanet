-- Baseline: indexes (consolidated)
--
-- Includes:
--   - Foundational lookups for core tables
--   - Partial unique indexes (active-state guards) absorbed from former V211
--   - Audit event indexes absorbed from former V210
--   - Revinfo lookups absorbed from former V210
--   - Draw result lookups absorbed from former V213
--   - Canonical operational unique guards (terminal, sales_session, limit_assignment,
--     autonomy_policy_rule, payout) defined alongside the canonical tables.

-- ─── Identity & access ──────────────────────────────────────────────
CREATE UNIQUE INDEX ux_app_user__keycloak_sub ON app_user (keycloak_sub) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_user__email ON app_user (email) WHERE email IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_app_role__tenant ON app_role (tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_role__system_code ON app_role (code) WHERE tenant_id IS NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_role__tenant_code ON app_role (tenant_id, code) WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_tenant_user__tenant ON tenant_user (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_tenant_user__user ON tenant_user (user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_address__tenant ON address (tenant_id);

-- ─── Settings, theming, i18n ────────────────────────────────────────
CREATE INDEX ix_app_setting__tenant ON app_setting (tenant_id, namespace, setting_key);
CREATE INDEX ix_i18n_override__tenant_lookup ON i18n_override (tenant_id, locale, i18n_key);
CREATE UNIQUE INDEX ux_tenant_theme__tenant_preset ON tenant_theme (tenant_id, preset_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_tenant_game__tenant_enabled ON tenant_game (tenant_id, enabled) WHERE deleted_at IS NULL;

-- ─── Game / draw catalog ────────────────────────────────────────────
CREATE INDEX ix_result_slot__active_sort ON result_slot (active, sort_order);
CREATE INDEX ix_draw_channel__tenant_active ON draw_channel (tenant_id, active) WHERE deleted_at IS NULL;
CREATE INDEX ix_draw_channel_game__tenant_channel ON draw_channel_game (tenant_id, draw_channel_id) WHERE deleted_at IS NULL;

-- ─── Draw lifecycle ─────────────────────────────────────────────────
CREATE UNIQUE INDEX uq_draw__tenant_channel_date_active
  ON draw (tenant_id, draw_channel_id, draw_date)
  WHERE deleted_at IS NULL;
CREATE INDEX ix_draw__tenant_date ON draw (tenant_id, draw_date);
CREATE INDEX ix_draw__tenant_scheduled ON draw (tenant_id, scheduled_at);
CREATE INDEX ix_draw__draw_result_id ON draw (draw_result_id);

-- ─── Draw results ───────────────────────────────────────────────────
CREATE INDEX ix_draw_result__slot_occurred ON draw_result (result_slot_id, occurred_at DESC);
CREATE INDEX ix_draw_result__source_hash ON draw_result (source_hash);

-- ─── Outlet, terminal, sales session (canonical guards) ─────────────
CREATE INDEX ix_outlet__tenant_active
  ON outlet (tenant_id, day_closed, sales_blocked) WHERE deleted_at IS NULL;
CREATE INDEX ix_tenant_user__tenant_outlet
  ON tenant_user (tenant_id, outlet_id) WHERE outlet_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_terminal__tenant_outlet
  ON terminal (tenant_id, outlet_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_terminal__one_active_per_user
  ON terminal (tenant_id, assigned_user_id)
  WHERE assigned_user_id IS NOT NULL AND active_for_user = true AND deleted_at IS NULL;
CREATE INDEX ix_sales_session__tenant_user_status
  ON sales_session (tenant_id, user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX ix_sales_session__tenant_outlet_status
  ON sales_session (tenant_id, outlet_id, status) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_sales_session__one_open_per_user
  ON sales_session (tenant_id, user_id)
  WHERE status = 'OPEN' AND deleted_at IS NULL;

-- ─── Tickets & lines ────────────────────────────────────────────────
CREATE INDEX ix_ticket__tenant_draw ON ticket (tenant_id, draw_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_ticket__tenant_session ON ticket (tenant_id, session_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_ticket__tenant_outlet ON ticket (tenant_id, outlet_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_ticket__tenant_user ON ticket (tenant_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_ticket_line__ticket ON ticket_line (ticket_id);

-- ─── Payout (1 payout max per ticket) ───────────────────────────────
CREATE INDEX ix_payout__tenant_ticket ON payout (tenant_id, ticket_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_payout__ticket ON payout (ticket_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_payout__tenant_status ON payout (tenant_id, status) WHERE deleted_at IS NULL;

-- ─── Limit policy ───────────────────────────────────────────────────
CREATE INDEX ix_limit_assignment__tenant_target
  ON limit_assignment (tenant_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_limit_assignment__natural
  ON limit_assignment (tenant_id, limit_definition_id, target_type, COALESCE(target_id, '00000000-0000-0000-0000-000000000000'::uuid))
  WHERE deleted_at IS NULL;

-- ─── Autonomy policy ────────────────────────────────────────────────
CREATE INDEX ix_autonomy_policy_rule__tenant_target
  ON autonomy_policy_rule (tenant_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_autonomy_policy_rule__natural
  ON autonomy_policy_rule (tenant_id, target_type, COALESCE(target_id, '00000000-0000-0000-0000-000000000000'::uuid))
  WHERE deleted_at IS NULL;

-- ─── Approval requests ──────────────────────────────────────────────
CREATE INDEX ix_approval_request__tenant_status
  ON approval_request (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX ix_approval_request__tenant_entity
  ON approval_request (tenant_id, entity_type, entity_id) WHERE deleted_at IS NULL;

-- ─── Reporting / exposure / ledger ──────────────────────────────────
CREATE INDEX ix_draw_exposure__top_stake ON draw_exposure (tenant_id, draw_id, stake_total DESC);
CREATE INDEX ix_ledger_entry__tenant_occurred ON ledger_entry (tenant_id, occurred_at);
CREATE INDEX ix_tchala_entry__status ON tchala_entry (status);

-- ─── Notifications ──────────────────────────────────────────────────
CREATE UNIQUE INDEX uq_notification__tenant_dedupe
  ON notification (COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid), dedupe_key)
  WHERE dedupe_key IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_notification_audience_status_created
  ON notification (tenant_id, audience_type, audience_value, status, created_at DESC);
CREATE INDEX idx_notification_delivery_notification_channel
  ON notification_delivery (notification_id, channel);
CREATE INDEX idx_notification_delivery_status_next
  ON notification_delivery (status, next_attempt_at);

-- ─── Technical: processed events / idempotency / stats ──────────────
CREATE INDEX ix_processed_event__lookup ON processed_event (tenant_id, handler_key, event_id);
CREATE INDEX ix_idempotency_record__lookup ON idempotency_record (tenant_id, scope, idem_key);
CREATE INDEX ix_stats_draw__tenant_scheduled ON stats_draw (tenant_id, scheduled_at);
CREATE INDEX ix_stats_daily__dimension_date ON stats_daily (dimension_type, dimension_id, ref_date);

-- ─── Audit event (formerly V210) ────────────────────────────────────
CREATE INDEX ix_audit_event__tenant_occurred
  ON audit_event (tenant_id, occurred_at DESC);
CREATE INDEX ix_audit_event__entity
  ON audit_event (entity_type, entity_id);
CREATE INDEX ix_audit_event__action_occurred
  ON audit_event (action, occurred_at DESC);
CREATE INDEX ix_audit_event__actor_occurred
  ON audit_event (actor_id, occurred_at DESC);

-- ─── Revinfo (Envers) ───────────────────────────────────────────────
CREATE INDEX ix_revinfo__tenant_id ON revinfo (tenant_id);
CREATE INDEX ix_revinfo__user_id ON revinfo (user_id);
CREATE INDEX ix_revinfo__request_id ON revinfo (request_id);
CREATE INDEX ix_revinfo__api_scope ON revinfo (api_scope);
