-- V999__add_terminal_meta.sql
-- Add meta column to terminal and audit table to store device metadata
-- Non-destructive: uses IF NOT EXISTS so it can be re-run safely on existing DBs

ALTER TABLE terminal
  ADD COLUMN IF NOT EXISTS meta text;

-- Envers audit table (if present)
ALTER TABLE terminal_aud
  ADD COLUMN IF NOT EXISTS meta text;

