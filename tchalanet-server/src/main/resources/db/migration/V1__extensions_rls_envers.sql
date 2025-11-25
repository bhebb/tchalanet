-- V1__extensions_rls_envers.sql
-- Extensions de base, helpers RLS et métadonnées Envers

-- Extensions utiles
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- Helper générique pour mettre à jour updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

-- ==============================
-- Helpers RLS : tenant & soft delete
-- ==============================

-- Tenant courant (UUID)
CREATE OR REPLACE FUNCTION set_current_tenant(p uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  -- Use transaction-local setting (is_local = true) to avoid leaking settings across sessions
  PERFORM set_config('app.current_tenant', p::text, true);
END $$;

CREATE OR REPLACE FUNCTION current_tenant()
RETURNS uuid LANGUAGE sql STABLE AS
$$
SELECT current_setting('app.current_tenant', true)::uuid;
$$;

-- Visibilité des données soft-deleted : 'active' | 'deleted' | 'all'
CREATE OR REPLACE FUNCTION set_deleted_visibility(p text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  IF p IS NULL OR p NOT IN ('active','deleted','all') THEN
    PERFORM set_config('app.deleted_visibility', 'active', true);
  ELSE
    PERFORM set_config('app.deleted_visibility', p, true);
  END IF;
END $$;

-- ==============================
-- Envers : table de révision + séquence
-- ==============================

CREATE TABLE IF NOT EXISTS revinfo (
  rev           INTEGER PRIMARY KEY,
  rev_timestamp BIGINT NOT NULL,
  tenant_id     UUID,
  user_id       UUID
);

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence
  START WITH 1 INCREMENT BY 1;
