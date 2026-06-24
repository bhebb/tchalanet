-- Baseline: indexes (consolidated)
--
-- Includes:
--   - Foundational lookups for core tables
--   - Partial unique indexes (active-state guards) absorbed from former V211
--   - Audit event indexes absorbed from former V210
--   - Revinfo lookups absorbed from former V210
--   - Draw result lookups absorbed from former V213
--   - Canonical operational unique guards for active V0 tables.

-- ─── Identity & access ──────────────────────────────────────────────
CREATE UNIQUE INDEX ux_app_user__email ON app_user (email) WHERE email IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_user__phone ON app_user (phone) WHERE phone IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_app_user_external_identity__app_user ON app_user_external_identity (app_user_id);
CREATE INDEX ix_app_role__tenant ON app_role (tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_role__system_code ON app_role (code) WHERE tenant_id IS NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_role__tenant_code ON app_role (tenant_id, code) WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_tenant_user__tenant ON tenant_user (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_tenant_user__user ON tenant_user (user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_address__tenant ON address (tenant_id);
CREATE INDEX idx_seller_terminal_tenant_status ON seller_terminal (tenant_id, status);
CREATE INDEX idx_seller_terminal_tenant_code ON seller_terminal (tenant_id, terminal_code);
CREATE INDEX idx_seller_terminal_tenant_name ON seller_terminal (tenant_id, display_name);
CREATE INDEX idx_seller_terminal_ext_terminal ON seller_terminal_external_identity (seller_terminal_id);

-- ─── Settings, theming, i18n ────────────────────────────────────────
CREATE INDEX ix_app_setting__tenant ON app_setting (tenant_id, namespace, setting_key);
CREATE INDEX ix_app_setting__runtime ON app_setting (tenant_id, exposure, active, namespace, setting_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_i18n_override__tenant_lookup ON i18n_override (tenant_id, locale, i18n_key);
CREATE INDEX ix_i18n_override__runtime ON i18n_override (tenant_id, surface, locale, active) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_i18n_override__global ON i18n_override (surface, locale, i18n_key) WHERE tenant_id IS NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_i18n_override__tenant ON i18n_override (tenant_id, surface, locale, i18n_key) WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_tenant_theme__tenant_preset ON tenant_theme (tenant_id, preset_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_tenant_game__tenant_enabled ON tenant_game (tenant_id, enabled) WHERE deleted_at IS NULL;

-- ─── Game / draw catalog ────────────────────────────────────────────
CREATE INDEX ix_result_slot__active_sort ON result_slot (active, sort_order);
CREATE INDEX ix_draw_channel__tenant_active ON draw_channel (tenant_id, active) WHERE deleted_at IS NULL;
CREATE INDEX ix_dcg_tenant_tenant_game ON draw_channel_game (tenant_id, tenant_game_id);
CREATE INDEX ix_draw_channel_game__tenant_channel ON draw_channel_game (tenant_id, draw_channel_id) WHERE deleted_at IS NULL;

-- ─── Draw lifecycle ─────────────────────────────────────────────────
CREATE UNIQUE INDEX uq_draw__tenant_channel_date_active
  ON draw (tenant_id, draw_channel_id, draw_date)
  WHERE deleted_at IS NULL;
CREATE INDEX ix_draw__tenant_date ON draw (tenant_id, draw_date);
CREATE INDEX ix_draw__tenant_scheduled ON draw (tenant_id, scheduled_at);
CREATE INDEX ix_draw__draw_result_id ON draw (draw_result_id);

-- ─── Draw results ───────────────────────────────────────────────────
CREATE UNIQUE INDEX uq_draw_result_slot_occurred ON draw_result (result_slot_id, occurred_at);
CREATE UNIQUE INDEX uq_draw_result_slot_result_date ON draw_result (result_slot_id, result_date);
CREATE INDEX ix_draw_result__source_hash ON draw_result (source_hash);


-- ─── Sales zone ──────────────────────────────────────────────────────
CREATE INDEX ix_sales_zone__tenant  ON sales_zone (tenant_id, active)    WHERE deleted_at IS NULL;
CREATE INDEX ix_sales_zone__parent  ON sales_zone (tenant_id, parent_id) WHERE parent_id IS NOT NULL AND deleted_at IS NULL;

-- ─── Limit policy ───────────────────────────────────────────────────

CREATE UNIQUE INDEX IF NOT EXISTS uq_limit_assignment_active_scope_rule
    ON limit_assignment (tenant_id, rule_key, scope_type, scope_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_limit_assignment_scope
    ON limit_assignment (tenant_id, scope_type, scope_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_limit_assignment_rule
    ON limit_assignment (tenant_id, rule_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_limit_assignment_enabled_window
    ON limit_assignment (tenant_id, enabled, starts_at, ends_at)
    WHERE deleted_at IS NULL;

-- ─── Reporting / exposure ───────────────────────────────────────────

CREATE UNIQUE INDEX IF NOT EXISTS uq_draw_exposure_key
    ON draw_exposure (
    tenant_id,
    draw_id,
    scope_type,
    scope_id,
    bet_type,
    selection_key
    )
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_draw_exposure_draw_scope
    ON draw_exposure (
    tenant_id,
    draw_id,
    scope_type,
    scope_id
    )
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_draw_exposure_selection
    ON draw_exposure (
    tenant_id,
    draw_id,
    bet_type,
    selection_key
    )
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_draw_exposure_top_stake
    ON draw_exposure (
    tenant_id,
    draw_id,
    scope_type,
    scope_id,
    stake_total DESC
    )
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_draw_exposure_top_payout
    ON draw_exposure (
    tenant_id,
    draw_id,
    scope_type,
    scope_id,
    potential_payout_total DESC
    )
    WHERE deleted_at IS NULL;
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
CREATE INDEX idx_notification_template_lookup
  ON notification_template (tenant_id, template_key, locale)
  WHERE active = true AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_outbound_message_correlation
  ON outbound_message (COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid), correlation_key)
  WHERE correlation_key IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_outbound_message_pending
  ON outbound_message (status, next_attempt_at, priority);
CREATE INDEX idx_message_delivery_attempt_message
  ON message_delivery_attempt (message_id, attempted_at DESC);

-- ─── Technical: processed events / idempotency / stats ──────────────
CREATE INDEX ix_processed_event__lookup ON processed_event (tenant_id, handler_key, event_id);
CREATE INDEX ix_idempotency_record__lookup ON idempotency_record (tenant_id, scope, idem_key);
CREATE INDEX ix_stats_draw__tenant_scheduled ON stats_draw (tenant_id, scheduled_at);
-- Single full-row unique index (NULLS NOT DISTINCT so the null-dimension case still collapses).
-- Required so the upsert in StatsDailyCustomRepositoryImpl can target
--   ON CONFLICT (dimension_type, dimension_id, ref_date)
-- without having to specify a partial WHERE predicate.
CREATE UNIQUE INDEX ux_stats_daily__dimension_date
  ON stats_daily (dimension_type, dimension_id, ref_date) NULLS NOT DISTINCT;

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

-- ─── Sales ticket ────────────────────────────────────────────────────
CREATE INDEX idx_sales_ticket__tenant ON sales_ticket (tenant_id);
CREATE INDEX idx_sales_ticket__tenant_draw ON sales_ticket (tenant_id, draw_id);
CREATE INDEX idx_sales_ticket__seller_terminal ON sales_ticket (tenant_id, seller_terminal_id) WHERE seller_terminal_id IS NOT NULL;
CREATE INDEX idx_sales_ticket__tenant_sale_status ON sales_ticket (tenant_id, sale_status);
CREATE INDEX idx_sales_ticket__tenant_result_status ON sales_ticket (tenant_id, result_status);
CREATE INDEX idx_sales_ticket__tenant_settlement_status ON sales_ticket (tenant_id, settlement_status);
CREATE INDEX idx_sales_ticket__sold_at_desc ON sales_ticket (tenant_id, sold_at DESC);
CREATE INDEX idx_sales_ticket_line__tenant ON sales_ticket_line (tenant_id);
CREATE INDEX idx_sales_ticket_line__ticket ON sales_ticket_line (ticket_id);
CREATE INDEX idx_sales_ticket_line__tenant_draw_game ON sales_ticket_line (tenant_id, draw_id, game_code);
CREATE INDEX idx_sales_ticket_line__result_status ON sales_ticket_line (tenant_id, result_status);
CREATE INDEX idx_sales_ticket_charge__tenant ON sales_ticket_charge (tenant_id);
CREATE INDEX idx_sales_ticket_charge__ticket ON sales_ticket_charge (sales_ticket_id);
CREATE INDEX idx_sales_ticket_charge__type ON sales_ticket_charge (tenant_id, charge_type);

-- ─── Promotion ──────────────────────────────────────────────────────
CREATE INDEX idx_promotion_campaign_active ON promotion_campaign (tenant_id, status, starts_at, ends_at);
CREATE INDEX idx_promotion_rule_campaign ON promotion_rule (tenant_id, campaign_id, priority, rule_key);
CREATE INDEX idx_promotion_rule_effect_rule ON promotion_rule_effect (tenant_id, rule_id, effect_type);
CREATE INDEX idx_promotion_rule_eligibility_line_rule ON promotion_rule_eligibility_line (tenant_id, rule_id, game_code);
CREATE INDEX idx_promotion_decision_lookup ON promotion_decision (tenant_id, context_hash, evaluation_phase);
CREATE INDEX idx_applied_promotion_ticket ON applied_promotion_snapshot (tenant_id, ticket_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- business_day_override index
-- ─────────────────────────────────────────────────────────────────────────────

-- One override per tenant/date.
CREATE UNIQUE INDEX uq_business_day_override_scope_date
ON business_day_override (
    tenant_id,
    business_date
)
WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- result_slot_calendar_override unique indexes (one per XOR shape)
-- These also serve the generator's per-slot lookups; the table is tiny so no
-- extra date-scan index is needed.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX uq_result_slot_calendar_override__specific
ON result_slot_calendar_override (result_slot_id, slot_local_date)
WHERE slot_local_date IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_result_slot_calendar_override__recurring
ON result_slot_calendar_override (result_slot_id, recurring_md)
WHERE recurring_md IS NOT NULL AND deleted_at IS NULL;
