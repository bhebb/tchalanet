-- V1: extensions and common functions
-- Extensions
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Function: set_updated_at()
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Tenant context helpers
CREATE OR REPLACE FUNCTION set_current_tenant(p_tenant uuid) RETURNS void AS $$
BEGIN
  PERFORM set_config('app.current_tenant', p_tenant::text, true);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION current_tenant() RETURNS uuid AS $$
BEGIN
  RETURN current_setting('app.current_tenant', true)::uuid;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Deleted visibility helper (active|deleted|all)
CREATE OR REPLACE FUNCTION set_deleted_visibility(p_visibility text) RETURNS void AS $$
BEGIN
  PERFORM set_config('app.deleted_visibility', COALESCE(p_visibility,'active'), true);
END;
$$ LANGUAGE plpgsql;

