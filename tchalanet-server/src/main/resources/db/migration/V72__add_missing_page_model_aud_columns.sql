-- V72: ensure page_model_aud and page_model_template_aud contain columns expected by entities
-- This migration fixes existing DBs where V70 previously created audit tables without all columns.

-- Add missing columns to page_model_aud
ALTER TABLE public.page_model_aud
  ADD COLUMN IF NOT EXISTS code varchar(128) NULL,
  ADD COLUMN IF NOT EXISTS name varchar(255) NULL,
  ADD COLUMN IF NOT EXISTS schema jsonb NULL,
  ADD COLUMN IF NOT EXISTS version bigint NULL,
  ADD COLUMN IF NOT EXISTS active boolean NULL,
  ADD COLUMN IF NOT EXISTS tenant_id uuid NULL,
  ADD COLUMN IF NOT EXISTS created_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS created_by uuid NULL,
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS updated_by uuid NULL,
  ADD COLUMN IF NOT EXISTS deleted_at timestamptz NULL;

-- Add missing columns to page_model_template_aud
ALTER TABLE public.page_model_template_aud
  ADD COLUMN IF NOT EXISTS code varchar(128) NULL,
  ADD COLUMN IF NOT EXISTS name varchar(255) NULL,
  ADD COLUMN IF NOT EXISTS schema jsonb NULL,
  ADD COLUMN IF NOT EXISTS version bigint NULL,
  ADD COLUMN IF NOT EXISTS created_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS created_by uuid NULL,
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS updated_by uuid NULL,
  ADD COLUMN IF NOT EXISTS deleted_at timestamptz NULL;

-- Optionally add indexes later if needed

