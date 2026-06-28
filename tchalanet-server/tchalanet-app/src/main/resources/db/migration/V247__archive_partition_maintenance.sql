-- V247 — archive partition maintenance helpers.
--
-- This migration intentionally does not rewrite sales_ticket/draw/draw_result as
-- partitioned tables. PostgreSQL requires every UNIQUE/PRIMARY KEY constraint on
-- a RANGE-partitioned table to include the partition key. These tables are
-- referenced by id-only foreign keys and also have id-only primary keys.
-- Rewriting them safely requires a dedicated schema redesign, not a blind DDL
-- conversion.
--
-- The helpers below are safe for already-partitioned tables such as audit_log
-- and for future tables that are created as RANGE partitions by month.

CREATE OR REPLACE FUNCTION public.archive_month_partition_name(
  p_parent_table text,
  p_month date
)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT lower(regexp_replace(p_parent_table, '[^a-zA-Z0-9_]', '_', 'g'))
         || '_' || to_char(date_trunc('month', p_month)::date, 'YYYY_MM')
$$;

CREATE OR REPLACE FUNCTION public.archive_ensure_month_partition(
  p_parent_table regclass,
  p_month date
)
RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
  v_parent_name text := p_parent_table::text;
  v_partition_name text;
  v_from date := date_trunc('month', p_month)::date;
  v_to date := (date_trunc('month', p_month)::date + INTERVAL '1 month')::date;
  v_relkind "char";
BEGIN
  SELECT c.relkind
    INTO v_relkind
    FROM pg_class c
   WHERE c.oid = p_parent_table;

  IF v_relkind IS DISTINCT FROM 'p' THEN
    RAISE EXCEPTION 'archive partition helper refused: % is not a partitioned table', v_parent_name;
  END IF;

  v_partition_name := public.archive_month_partition_name(v_parent_name, v_from);

  IF to_regclass(v_partition_name) IS NULL THEN
    EXECUTE format(
      'CREATE TABLE %I PARTITION OF %s FOR VALUES FROM (%L) TO (%L)',
      v_partition_name,
      p_parent_table,
      v_from,
      v_to
    );
  END IF;

  RETURN v_partition_name;
END;
$$;

CREATE OR REPLACE FUNCTION public.archive_ensure_month_partitions(
  p_parent_table regclass,
  p_start_month date,
  p_month_count integer
)
RETURNS TABLE(partition_name text)
LANGUAGE plpgsql
AS $$
DECLARE
  i integer;
BEGIN
  IF p_month_count < 1 OR p_month_count > 120 THEN
    RAISE EXCEPTION 'archive partition helper refused: p_month_count must be between 1 and 120';
  END IF;

  FOR i IN 0..(p_month_count - 1) LOOP
    partition_name := public.archive_ensure_month_partition(
      p_parent_table,
      (date_trunc('month', p_start_month)::date + (i || ' months')::interval)::date
    );
    RETURN NEXT;
  END LOOP;
END;
$$;

-- Keep audit_log partition coverage ahead of the current month.
SELECT public.archive_ensure_month_partitions(
  'audit_log'::regclass,
  date_trunc('month', CURRENT_DATE)::date,
  13
);
