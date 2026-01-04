-- V1003: add unique index on ticket.public_code (non-null & not deleted)
-- This migration creates a unique partial index to enforce global uniqueness of public_code
-- for non-deleted rows.

BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS ux_ticket_public_code
    ON ticket (public_code)
    WHERE public_code IS NOT NULL AND deleted_at IS NULL;

COMMIT;

