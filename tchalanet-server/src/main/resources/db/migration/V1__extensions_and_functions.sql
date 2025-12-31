-- V1: extensions and common functions
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Function: set_updated_at()
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ------------------------------------------------------------
-- RLS session helpers (public.* only)
-- ------------------------------------------------------------

-- Setter tenant (explicit schema)
CREATE OR REPLACE FUNCTION public.set_current_tenant(p_tenant uuid) RETURNS void AS $$
BEGIN
  PERFORM set_config('app.current_tenant', p_tenant::text, false);
END;
$$ LANGUAGE plpgsql;

-- Safe getter
CREATE OR REPLACE FUNCTION public.current_tenant() RETURNS uuid AS $$
DECLARE v text;
BEGIN
  v := current_setting('app.current_tenant', true);
  IF v IS NULL OR v = '' THEN
    RETURN NULL;
END IF;

RETURN v::uuid;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Deleted visibility setter
CREATE OR REPLACE FUNCTION public.set_deleted_visibility(p_visibility text) RETURNS void AS $$
DECLARE v text;
BEGIN
  v := lower(trim(COALESCE(p_visibility, 'active')));
  IF v NOT IN ('active','deleted','all') THEN
    v := 'active';
END IF;

  PERFORM set_config('app.deleted_visibility', v, false);
END;
$$ LANGUAGE plpgsql;

-- Deleted visibility getter
CREATE OR REPLACE FUNCTION public.deleted_visibility() RETURNS text AS $$
DECLARE v text;
BEGIN
  v := lower(trim(COALESCE(current_setting('app.deleted_visibility', true), 'active')));
  IF v NOT IN ('active','deleted','all') THEN
    v := 'active';
END IF;

RETURN v;
END;
$$ LANGUAGE plpgsql;

-- Reset helper (optional if Java uses set_config directly)
CREATE OR REPLACE FUNCTION public.reset_rls_context() RETURNS void AS $$
BEGIN
  PERFORM set_config('app.current_tenant', '', false);
  PERFORM set_config('app.deleted_visibility', 'active', false);
END;
$$ LANGUAGE plpgsql;
