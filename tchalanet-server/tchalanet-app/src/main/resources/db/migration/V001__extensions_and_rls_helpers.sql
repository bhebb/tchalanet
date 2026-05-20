-- Baseline: extensions and RLS helpers
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

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
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION public.deleted_visibility() RETURNS text AS $$
DECLARE v text;
BEGIN
  v := lower(trim(coalesce(current_setting('app.deleted_visibility', true), 'active')));
  IF v NOT IN ('active', 'deleted', 'all') THEN
    v := 'active';
  END IF;
  RETURN v;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION public.api_scope() RETURNS text AS $$
  SELECT coalesce(current_setting('app.api_scope', true), '');
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.is_super_admin() RETURNS boolean AS $$
  SELECT coalesce(current_setting('app.is_super_admin', true), '') = 'true';
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.allow_platform_cross_tenant_select() RETURNS boolean AS $$
  SELECT public.is_super_admin() AND public.api_scope() = 'platform';
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.reset_rls_context() RETURNS void AS $$
BEGIN
  PERFORM set_config('app.current_tenant', '', false);
  PERFORM set_config('app.deleted_visibility', 'active', false);
  PERFORM set_config('app.api_scope', '', false);
  PERFORM set_config('app.is_super_admin', 'false', false);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

