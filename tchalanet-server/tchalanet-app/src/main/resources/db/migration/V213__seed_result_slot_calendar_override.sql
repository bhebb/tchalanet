-- ─────────────────────────────────────────────────────────────────────────────
-- V213 — Bootstrap provider no-draw days (recurring).
--
-- Runtime truth lives in `result_slot_calendar_override`, managed by SUPER_ADMIN
-- through a platform endpoint (not yet shipped). This seed only pre-loads the
-- well-known fixed holidays that US state lotteries observe, so the draw
-- generator skips them out of the box on a fresh DB.
--
-- Conventions:
-- - `recurring_md` is a year-less 'MM-dd' annual rule in the slot's own timezone
--   (see V100 comment). Christmas is every Dec 25 → '12-25', never goes stale.
-- - `slot_local_date` stays NULL here (XOR shape — recurring rule only).
-- - `available = false` marks a no-draw day; `reason_code` is canonical.
-- - Re-runnable: ON CONFLICT infers the PARTIAL unique index by repeating its
--   predicate (recurring_md IS NOT NULL AND deleted_at IS NULL).
--
-- Scope: FIXED-date holidays only (same calendar day every year). Movable feasts
-- (Easter) and one-off provider closures are entered at runtime as specific
-- dated rows via the SUPER_ADMIN endpoint — never hardcoded in an immutable
-- migration (they would go stale and cannot be corrected in place).
-- ─────────────────────────────────────────────────────────────────────────────

WITH closures (slot_key, recurring_md, reason_code, reason_label) AS (
    VALUES
        -- ─── Christmas (Dec 25, every year — no draw on US state lotteries) ──
        ('NY_MID',  '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no NY Numbers midday draw'),
        ('NY_EVE',  '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no NY Numbers evening draw'),
        ('FL_MID',  '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no FL Pick midday draw'),
        ('FL_EVE',  '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no FL Pick evening draw'),
        ('GA_MID',  '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no GA Cash midday draw'),
        ('GA_EVE',  '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no GA Cash evening draw'),
        ('GA_LATE', '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no GA Cash night draw'),
        ('TX_1000', '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no TX Pick3 morning draw'),
        ('TX_1227', '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no TX Pick3 day draw'),
        ('TX_1800', '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no TX Pick3 evening draw'),
        ('TX_2212', '12-25', 'PROVIDER_CLOSED', 'Christmas Day — no TX Pick3 night draw')
)
INSERT INTO result_slot_calendar_override (
    id, result_slot_id, slot_local_date, recurring_md, available, reason_code, reason_label,
    created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    rs.id,
    NULL,
    c.recurring_md,
    false,
    c.reason_code,
    c.reason_label,
    now(),
    now(),
    0
FROM closures c
JOIN result_slot rs
  ON rs.slot_key = c.slot_key
 AND rs.deleted_at IS NULL
-- The unique index is PARTIAL (uq_result_slot_calendar_override__recurring); its
-- predicate must be repeated here for PostgreSQL to infer it for ON CONFLICT.
ON CONFLICT (result_slot_id, recurring_md)
  WHERE recurring_md IS NOT NULL AND deleted_at IS NULL
  DO NOTHING;
