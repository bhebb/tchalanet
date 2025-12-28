-- V82: add tenant_id to page_model_template to match JPA entity

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS tenant_id uuid;

-- Add an index to speed lookups by tenant (soft-delete aware)
CREATE INDEX IF NOT EXISTS ix_page_model_template_tenant ON page_model_template (tenant_id) WHERE deleted_at IS NULL;

