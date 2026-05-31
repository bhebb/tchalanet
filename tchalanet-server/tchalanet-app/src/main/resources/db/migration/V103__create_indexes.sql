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

-- ─── Sales zone ──────────────────────────────────────────────────────
CREATE INDEX ix_sales_zone__tenant  ON sales_zone (tenant_id, active)    WHERE deleted_at IS NULL;
CREATE INDEX ix_sales_zone__parent  ON sales_zone (tenant_id, parent_id) WHERE parent_id IS NOT NULL AND deleted_at IS NULL;

-- ─── Outlet, terminal, sales session (canonical guards) ─────────────
CREATE UNIQUE INDEX ux_outlet__tenant_slug
  ON outlet (tenant_id, slug) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_outlet__tenant_partner_ref
  ON outlet (tenant_id, partner_ref) WHERE partner_ref IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_outlet__kind        ON outlet (tenant_id, kind)    WHERE deleted_at IS NULL;
CREATE INDEX ix_outlet__zone        ON outlet (tenant_id, zone_id) WHERE zone_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_outlet__tenant_active
  ON outlet (tenant_id, status, outlet_blocked, day_closed) WHERE deleted_at IS NULL;
CREATE INDEX ix_outlet__auto_session_open
  ON outlet (tenant_id, session_open_time)
  WHERE auto_session_open_enabled = true AND deleted_at IS NULL;
CREATE INDEX ix_outlet__auto_session_close
  ON outlet (tenant_id, session_close_time)
  WHERE auto_session_close_enabled = true AND deleted_at IS NULL;
CREATE INDEX ix_tenant_user__tenant_outlet
  ON tenant_user (tenant_id, outlet_id) WHERE outlet_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_terminal__tenant_outlet
  ON terminal (tenant_id, outlet_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_terminal__assigned_user
  ON terminal (tenant_id, assigned_user_id)
  WHERE assigned_user_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_terminal__one_active_per_user
  ON terminal (tenant_id, assigned_user_id)
  WHERE assigned_user_id IS NOT NULL AND auto_session_enabled = true AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_terminal__auto_session_eligible
  ON terminal (tenant_id, outlet_id, assigned_user_id)
  WHERE assigned_user_id IS NOT NULL
    AND auto_session_enabled = true
    AND state = 'ACTIVE'
    AND deleted_at IS NULL;
CREATE INDEX ix_terminal_capability__terminal
  ON terminal_capability (tenant_id, terminal_id)
  WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_terminal_assignment__active_terminal_user
  ON terminal_assignment (tenant_id, terminal_id, user_id)
  WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX ix_terminal_assignment__user
  ON terminal_assignment (tenant_id, user_id, status)
  WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_terminal_binding__active_terminal_type
  ON terminal_binding (tenant_id, terminal_id, binding_type)
  WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX ix_terminal_binding__terminal_status
  ON terminal_binding (tenant_id, terminal_id, status)
  WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_terminal_challenge__pending_terminal_user_type
  ON terminal_challenge (tenant_id, terminal_id, user_id, challenge_type)
  WHERE status = 'PENDING' AND deleted_at IS NULL;
CREATE INDEX ix_terminal_challenge__expires
  ON terminal_challenge (tenant_id, status, expires_at)
  WHERE deleted_at IS NULL;
CREATE INDEX ix_sales_session__tenant_outlet_status
  ON sales_session (tenant_id, outlet_id, status) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_sales_session__one_open_per_user
  ON sales_session (tenant_id, opened_by)
  WHERE status = 'OPEN' AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_sales_session__user_business_day
  ON sales_session (tenant_id, outlet_id, opened_by, business_date)
  WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_sales_session__open_terminal
  ON sales_session (tenant_id, terminal_id)
  WHERE status = 'OPEN' AND deleted_at IS NULL;
CREATE INDEX ix_sales_session__tenant_opened_by_date
  ON sales_session (tenant_id, opened_by, business_date)
  WHERE deleted_at IS NULL;


-- ─── Payout claim (1 claim max per ticket — enforced by uq_payout_tenant_ticket) ──
-- idx_payout_tenant_ticket is intentionally omitted: the unique constraint already
-- creates an index on (tenant_id, ticket_id).

create index idx_payout_tenant_status_opened
    on payout (tenant_id, status, opened_at desc);

create index idx_payout_tenant_draw_status
    on payout (tenant_id, draw_id, status)
    where draw_id is not null;

create index idx_payout_tenant_paying_session
    on payout (tenant_id, paying_session_id);

create index idx_payout_tenant_selling_session
    on payout (tenant_id, selling_session_id);

CREATE INDEX ix_payout__paying_session_status
  ON payout (tenant_id, paying_session_id, status)
  WHERE paying_session_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX ix_payout__status_opened_at
  ON payout (tenant_id, status, opened_at)
  WHERE deleted_at IS NULL;

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

-- ─── Autonomy policy ────────────────────────────────────────────────
CREATE INDEX ix_autonomy_policy_rule__tenant_target
  ON autonomy_policy_rule (tenant_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_autonomy_policy_rule__natural
  ON autonomy_policy_rule (tenant_id, target_type, COALESCE(target_id, '00000000-0000-0000-0000-000000000000'::uuid))
  WHERE deleted_at IS NULL;


-- ─── Reporting / exposure / ledger ──────────────────────────────────

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

CREATE INDEX ix_ledger_entry_tenant_occurred  ON ledger_entry (tenant_id, occurred_at DESC);

CREATE INDEX ix_ledger_entry_tenant_ref  ON ledger_entry (tenant_id, ref_type, ref_id);

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
CREATE INDEX idx_sales_ticket__tenant_session ON sales_ticket (tenant_id, sales_session_id);
CREATE INDEX idx_sales_ticket__tenant_seller ON sales_ticket (tenant_id, seller_user_id);
CREATE INDEX idx_sales_ticket__tenant_payout ON sales_ticket (tenant_id, payout_id);
CREATE INDEX idx_sales_ticket__tenant_outlet ON sales_ticket (tenant_id, outlet_id);
CREATE INDEX idx_sales_ticket__tenant_terminal ON sales_ticket (tenant_id, terminal_id);
CREATE INDEX idx_sales_ticket__tenant_sale_status ON sales_ticket (tenant_id, sale_status);
CREATE INDEX idx_sales_ticket__tenant_result_status ON sales_ticket (tenant_id, result_status);
CREATE INDEX idx_sales_ticket__tenant_settlement_status ON sales_ticket (tenant_id, settlement_status);
CREATE INDEX idx_sales_ticket__offline_submission ON sales_ticket (tenant_id, offline_submission_id);
CREATE UNIQUE INDEX uk_sales_ticket__offline_submission_not_null
  ON sales_ticket (tenant_id, offline_submission_id)
  WHERE offline_submission_id IS NOT NULL;
CREATE INDEX idx_sales_ticket__sold_at_desc ON sales_ticket (tenant_id, sold_at DESC);
CREATE INDEX idx_sales_ticket_line__tenant ON sales_ticket_line (tenant_id);
CREATE INDEX idx_sales_ticket_line__ticket ON sales_ticket_line (ticket_id);
CREATE INDEX idx_sales_ticket_line__tenant_draw_game ON sales_ticket_line (tenant_id, draw_id, game_code);
CREATE INDEX idx_sales_ticket_line__result_status ON sales_ticket_line (tenant_id, result_status);
CREATE INDEX idx_sales_ticket_charge__tenant ON sales_ticket_charge (tenant_id);
CREATE INDEX idx_sales_ticket_charge__ticket ON sales_ticket_charge (sales_ticket_id);
CREATE INDEX idx_sales_ticket_charge__type ON sales_ticket_charge (tenant_id, charge_type);

-- ─── Offline sync ────────────────────────────────────────────────────
CREATE INDEX idx_offline_grant__tenant ON offline_grant (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_grant__terminal ON offline_grant (tenant_id, terminal_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_grant__seller ON offline_grant (tenant_id, seller_user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_sync_batch__tenant ON offline_sync_batch (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_sync_batch__grant ON offline_sync_batch (tenant_id, grant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_code_batch__tenant ON offline_code_batch (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_code_batch__terminal ON offline_code_batch (tenant_id, terminal_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_submission__tenant ON offline_submission (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_submission__grant ON offline_submission (tenant_id, grant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_submission__batch ON offline_submission (tenant_id, sync_batch_id) WHERE sync_batch_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_offline_submission__dispatch
  ON offline_submission (tenant_id, status, processed_at)
  WHERE status IN ('TECH_VALIDATED','PROMOTION_REQUESTED','SYNC_FAILED') AND deleted_at IS NULL;
CREATE INDEX idx_offline_submission__promotion_attempt
  ON offline_submission (tenant_id, promotion_attempt_id)
  WHERE promotion_attempt_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_offline_submission_line__submission ON offline_submission_line (submission_id);
CREATE INDEX idx_offline_code__batch ON offline_code (tenant_id, code_batch_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_code__code ON offline_code (tenant_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_code__status ON offline_code (tenant_id, code_batch_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_offline_submission_ticket_link__submission ON offline_submission_ticket_link (submission_id);
CREATE INDEX idx_offline_submission_ticket_link__ticket ON offline_submission_ticket_link (ticket_id);
CREATE UNIQUE INDEX uq_offline_submission_ticket_link__created
  ON offline_submission_ticket_link (tenant_id, submission_id)
  WHERE link_type = 'CREATED' AND deleted_at IS NULL;
CREATE INDEX idx_offline_submission_decision__submission ON offline_submission_decision (tenant_id, submission_id) WHERE deleted_at IS NULL;
-- Outbox: cheap scan over pending rows; the drainer scheduler reads this hot path every few seconds.
CREATE INDEX idx_offline_event_outbox__pending
  ON offline_event_outbox (tenant_id, next_attempt_at)
  WHERE published_at IS NULL;
-- tenant_offline_policy is keyed by tenant_id (unique constraint already covers the lookup),
-- so no extra index is needed beyond the implicit one on the PRIMARY KEY + UNIQUE.

-- ─── Promotion ──────────────────────────────────────────────────────
CREATE INDEX idx_promotion_campaign_active ON promotion_campaign (tenant_id, status, starts_at, ends_at);
CREATE INDEX idx_promotion_rule_campaign ON promotion_rule (tenant_id, campaign_id, priority, rule_key);
CREATE INDEX idx_promotion_rule_effect_rule ON promotion_rule_effect (tenant_id, rule_id, effect_type);
CREATE INDEX idx_promotion_rule_eligibility_line_rule ON promotion_rule_eligibility_line (tenant_id, rule_id, game_code);
CREATE INDEX idx_promotion_decision_lookup ON promotion_decision (tenant_id, context_hash, evaluation_phase);
CREATE INDEX idx_applied_promotion_ticket ON applied_promotion_snapshot (tenant_id, ticket_id);

-- ─── Seller ─────────────────────────────────────────────────────────
CREATE INDEX idx_seller__tenant_status ON seller (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_seller__tenant_user   ON seller (tenant_id, user_id) WHERE user_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_seller_assignment__seller ON seller_outlet_assignment (tenant_id, seller_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_seller_assignment__outlet ON seller_outlet_assignment (tenant_id, outlet_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_seller_commission__seller ON seller_commission_policy (tenant_id, seller_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_sales_ticket__seller     ON sales_ticket (tenant_id, seller_id)            WHERE seller_id IS NOT NULL;
CREATE INDEX idx_sales_ticket__assignment ON sales_ticket (tenant_id, seller_assignment_id) WHERE seller_assignment_id IS NOT NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- sales_session constraints V1 update
-- ─────────────────────────────────────────────────────────────────────────────

-- Drop the old constraint that prevents CLOSED+OPEN on the same businessDate.
-- V1 rule: a CLOSED session does NOT block opening a new session on the same
-- business day. Only one OPEN session per operational context is enforced.
DROP INDEX IF EXISTS ux_sales_session__user_business_day;

-- New: one OPEN session per (tenant, outlet, terminal, seller).
CREATE UNIQUE INDEX uq_sales_session_one_open_per_operational_context
ON sales_session (tenant_id, outlet_id, terminal_id, opened_by)
WHERE status = 'OPEN' AND deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- business_day_override index
-- ─────────────────────────────────────────────────────────────────────────────

-- One override per (tenant, outlet-or-null, date).
CREATE UNIQUE INDEX uq_business_day_override_scope_date
ON business_day_override (
    tenant_id,
    COALESCE(outlet_id, '00000000-0000-0000-0000-000000000000'::uuid),
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
