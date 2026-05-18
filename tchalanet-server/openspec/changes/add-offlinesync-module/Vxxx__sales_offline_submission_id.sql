-- Adds physical protection against creating more than one ticket for a single offline submission.
-- Replace table/column names if your current sales ticket table differs.

ALTER TABLE ticket
  ADD COLUMN offline_submission_id uuid NULL;

CREATE UNIQUE INDEX uq_ticket_tenant_offline_submission
  ON ticket (tenant_id, offline_submission_id)
  WHERE offline_submission_id IS NOT NULL;

CREATE INDEX idx_ticket_offline_submission
  ON ticket (offline_submission_id)
  WHERE offline_submission_id IS NOT NULL;
