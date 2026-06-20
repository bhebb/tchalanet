-- V240 — Seller terminal PIN reset
-- Adds must_change_pin / pin_reset_at columns and seeds the new permission.

-- ── 1. Schema ──────────────────────────────────────────────────────────────────

ALTER TABLE seller_terminal
    ADD COLUMN IF NOT EXISTS must_change_pin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pin_reset_at    TIMESTAMPTZ;

-- ── 2. Permission seed ────────────────────────────────────────────────────────

INSERT INTO permission (code, name, category, system, active)
VALUES
    ('seller_terminal.manage', 'Manage seller terminals',    'seller_terminal', true, true),
    ('seller_terminal.block',  'Block/unblock seller terminals', 'seller_terminal', true, true),
    ('seller_terminal.pin.reset', 'Reset seller terminal PIN', 'seller_terminal', true, true)
ON CONFLICT (code) DO NOTHING;

-- ── 3. Grant to TENANT_ADMIN ──────────────────────────────────────────────────

INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000302'::uuid, unnest(ARRAY[
    'seller_terminal.manage',
    'seller_terminal.block',
    'seller_terminal.pin.reset'
]) ON CONFLICT DO NOTHING;
