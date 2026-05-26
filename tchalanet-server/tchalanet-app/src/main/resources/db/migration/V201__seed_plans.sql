-- V201: seed billing_plan (STARTER, STANDARD, PRO, DEMO)
-- Conformance: catalog/plan spec (P1-P5)
-- Table: billing_plan (renamed from 'plan' for clarity)

DO $$ BEGIN
  RAISE NOTICE 'V201__seed_plans: seeding billing_plan (STARTER/STANDARD/PRO/DEMO)';
END $$;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'STARTER',
  'Starter',
  'Petit tenant avec opérations de base.',
  0,
  'USD',
  'MONTHLY',
  '{
    "tenant.profile.basic": true,
    "auth.login.basic": true,
    "user.self.profile": true,
    "sales.ticket.sell": true,
    "sales.ticket.lookup": true,
    "sales.ticket.reprint": true,
    "draw.active.list": true,
    "drawresult.public.view": true,
    "payout.basic": true,
    "document.receipt.basic": true,
    "pos.web.basic": true,
    "reporting.daily.basic": true,
    "security.role.basic": true,
    "audit.basic": true,
    "user.management.basic": true,
    "outlet.management.basic": true,
    "terminal.management.basic": true,
    "session.cashier.basic": true,
    "sales.ticket.cancel.basic": true,
    "payout.session.basic": true,
    "reporting.sales.summary": true,
    "reporting.payout.summary": true,
    "tenant.theme.logo": true,
    "document.receipt.logo": true
  }'::jsonb,
  '{
    "limits.users.max": 5,
    "limits.outlets.max": 1,
    "limits.terminals.max": 2,
    "limits.mobile_devices.max": 0,
    "limits.promotion_rules.max": 0,
    "limits.offline_days.max": 0,
    "limits.exports.rows.max": 1000
  }'::jsonb,
  true,
  true,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'STANDARD',
  'Standard',
  'Tenant structuré avec multi-outlet léger et terminaux.',
  0,
  'USD',
  'MONTHLY',
  '{
    "tenant.profile.basic": true,
    "auth.login.basic": true,
    "user.self.profile": true,
    "sales.ticket.sell": true,
    "sales.ticket.lookup": true,
    "sales.ticket.reprint": true,
    "draw.active.list": true,
    "drawresult.public.view": true,
    "payout.basic": true,
    "document.receipt.basic": true,
    "pos.web.basic": true,
    "reporting.daily.basic": true,
    "security.role.basic": true,
    "audit.basic": true,

    "user.management.basic": true,
    "outlet.management.basic": true,
    "terminal.management.basic": true,
    "session.cashier.basic": true,
    "sales.ticket.cancel.basic": true,
    "payout.session.basic": true,
    "reporting.sales.summary": true,
    "reporting.payout.summary": true,
    "tenant.theme.logo": true,
    "document.receipt.logo": true,

    "user.management.standard": true,
    "user.role.assignment.basic": true,
    "outlet.management.multi": true,
    "terminal.licensing": true,
    "terminal.device.binding": true,
    "sales.phone.sell": true,
    "session.supervision": true,
    "sales.ticket.void.admin": true,
    "payout.admin.review": true,
    "reporting.dashboard.standard": true,
    "reporting.export.csv": true,
    "document.receipt.pdf": true,
    "notification.in_app": true,
    "tenant.theme.basic_branding": true,
    "limitpolicy.basic": true
  }'::jsonb,
  '{
    "limits.users.max": 15,
    "limits.outlets.max": 3,
    "limits.terminals.max": 10,
    "limits.mobile_devices.max": 2,
    "limits.promotion_rules.max": 0,
    "limits.offline_days.max": 0,
    "limits.exports.rows.max": 10000
  }'::jsonb,
  true,
  false,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'PRO',
  'Pro',
  'Mobile, offline, promotions simples et limites avancées.',
  0,
  'USD',
  'MONTHLY',
  '{
    "tenant.profile.basic": true,
    "auth.login.basic": true,
    "user.self.profile": true,
    "sales.ticket.sell": true,
    "sales.ticket.lookup": true,
    "sales.ticket.reprint": true,
    "draw.active.list": true,
    "drawresult.public.view": true,
    "payout.basic": true,
    "document.receipt.basic": true,
    "pos.web.basic": true,
    "reporting.daily.basic": true,
    "security.role.basic": true,
    "audit.basic": true,

    "user.management.basic": true,
    "outlet.management.basic": true,
    "terminal.management.basic": true,
    "session.cashier.basic": true,
    "sales.ticket.cancel.basic": true,
    "payout.session.basic": true,
    "reporting.sales.summary": true,
    "reporting.payout.summary": true,
    "tenant.theme.logo": true,
    "document.receipt.logo": true,

    "user.management.standard": true,
    "user.role.assignment.basic": true,
    "outlet.management.multi": true,
    "terminal.licensing": true,
    "terminal.device.binding": true,
    "sales.phone.sell": true,
    "session.supervision": true,
    "sales.ticket.void.admin": true,
    "payout.admin.review": true,
    "reporting.dashboard.standard": true,
    "reporting.export.csv": true,
    "document.receipt.pdf": true,
    "notification.in_app": true,
    "tenant.theme.basic_branding": true,
    "limitpolicy.basic": true,

    "mobile.pos.basic": true,
    "mobile.device.management": true,
    "offline.sales.basic": true,
    "offline.sync.review": true,
    "offline.grant.basic": true,
    "promotionDecision.rules.basic": true,
    "promotionDecision.free_game": true,
    "promotionDecision.prize_multiplier": true,
    "limitpolicy.advanced": true,
    "payout.approval.workflow": true,
    "reporting.dashboard.pro": true,
    "reporting.export.excel": true,
    "document.receipt.custom_template.basic": true,
    "notification.email": true,
    "audit.viewer": true
  }'::jsonb,
  '{
    "limits.users.max": 50,
    "limits.outlets.max": 10,
    "limits.terminals.max": 30,
    "limits.mobile_devices.max": 20,
    "limits.promotion_rules.max": 10,
    "limits.offline_days.max": 2,
    "limits.offline_tickets_per_device.max": 500,
    "limits.exports.rows.max": 100000
  }'::jsonb,
  true,
  false,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing_plan (
  id,
  code,
  name,
  description,
  price_amount,
  currency,
  billing_period,
  features_json,
  limits_json,
  active,
  is_default,
  created_at,
  updated_at
)
VALUES (
  gen_random_uuid(),
  'DEMO',
  'Demo / Trial',
  'Accès complet à toutes les fonctionnalités pour évaluation',
  0.00,
  'USD',
  'MONTHLY',
  '{
      "tenant.profile.basic": true,
      "auth.login.basic": true,
      "user.self.profile": true,
      "sales.ticket.sell": true,
      "sales.ticket.lookup": true,
      "sales.ticket.reprint": true,
      "draw.active.list": true,
      "drawresult.public.view": true,
      "payout.basic": true,
      "document.receipt.basic": true,
      "pos.web.basic": true,
      "reporting.daily.basic": true,
      "security.role.basic": true,
      "audit.basic": true,

      "user.management.basic": true,
      "outlet.management.basic": true,
      "terminal.management.basic": true,
      "session.cashier.basic": true,
      "sales.ticket.cancel.basic": true,
      "payout.session.basic": true,
      "reporting.sales.summary": true,
      "reporting.payout.summary": true,
      "tenant.theme.logo": true,
      "document.receipt.logo": true,

      "user.management.standard": true,
      "user.role.assignment.basic": true,
      "outlet.management.multi": true,
      "terminal.licensing": true,
      "terminal.device.binding": true,
      "sales.phone.sell": true,
      "session.supervision": true,
      "sales.ticket.void.admin": true,
      "payout.admin.review": true,
      "reporting.dashboard.standard": true,
      "reporting.export.csv": true,
      "document.receipt.pdf": true,
      "notification.in_app": true,
      "tenant.theme.basic_branding": true,
      "limitpolicy.basic": true,

      "mobile.pos.basic": true,
      "mobile.device.management": true,
      "offline.sales.basic": true,
      "offline.sync.review": true,
      "offline.grant.basic": true,
      "promotionDecision.rules.basic": true,
      "promotionDecision.free_game": true,
      "promotionDecision.prize_multiplier": true,
      "limitpolicy.advanced": true,
      "payout.approval.workflow": true,
      "reporting.dashboard.pro": true,
      "reporting.export.excel": true,
      "document.receipt.custom_template.basic": true,
      "notification.email": true,
      "audit.viewer": true,

      "demo.full_access": true,
      "demo.seed_data": true,
      "demo.external_delivery.mock": true,
      "demo.expires": true
  }'::jsonb,
  '{
      "limits.users.max": 9999,
      "limits.outlets.max": 999,
      "limits.terminals.max": 9999,
      "limits.mobile_devices.max": 9999,
      "limits.promotion_rules.max": 999,
      "limits.offline_days.max": 999,
      "limits.offline_tickets_per_device.max": 999999,
      "limits.exports.rows.max": 9999999
  }'::jsonb,
  true,
  false,
  now(),
  now()
)
ON CONFLICT (code) DO NOTHING;

-- Sanity check: ensure all 4 plans exist and exactly one is default
DO $$
DECLARE
  plan_count int;
  default_count int;
BEGIN
  SELECT count(*) INTO plan_count
  FROM billing_plan
  WHERE code IN ('STARTER', 'STANDARD', 'PRO', 'DEMO')
    AND deleted_at IS NULL;

  SELECT count(*) INTO default_count
  FROM billing_plan
  WHERE is_default = true
    AND active = true
    AND deleted_at IS NULL;

  IF plan_count < 4 THEN
    RAISE EXCEPTION 'V201__seed_plans sanity check failed: expected 4 plans, found %', plan_count;
  END IF;

  IF default_count != 1 THEN
    RAISE EXCEPTION 'V201__seed_plans sanity check failed: expected exactly 1 default plan, found %', default_count;
  END IF;

  RAISE NOTICE 'V201__seed_plans sanity check OK: % plans present, % default plan', plan_count, default_count;
END $$;
