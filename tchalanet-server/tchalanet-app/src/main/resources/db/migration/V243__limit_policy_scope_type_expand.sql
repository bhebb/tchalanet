-- Expand scope_type constraints to include SELLER_TERMINAL
-- Previously only TENANT, DRAW_CHANNEL, AGENT were allowed.
-- LimitPolicyAdminController supports SELLER_TERMINAL but INSERTs were failing at DB level.

ALTER TABLE limit_assignment
  DROP CONSTRAINT IF EXISTS ck_limit_assignment_scope_type;

ALTER TABLE limit_assignment
  ADD CONSTRAINT ck_limit_assignment_scope_type
  CHECK (scope_type IN ('TENANT', 'DRAW_CHANNEL', 'AGENT', 'SELLER_TERMINAL'));

ALTER TABLE draw_exposure
  DROP CONSTRAINT IF EXISTS ck_draw_exposure_scope_type;

ALTER TABLE draw_exposure
  ADD CONSTRAINT ck_draw_exposure_scope_type
  CHECK (scope_type IN ('TENANT', 'DRAW_CHANNEL', 'AGENT', 'SELLER_TERMINAL'));
