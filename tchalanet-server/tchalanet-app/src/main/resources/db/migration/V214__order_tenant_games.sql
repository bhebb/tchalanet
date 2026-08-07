-- Give the POS game chips a deliberate order.
--
-- PosGamesService already sorts by tenant_game.display_order, but every row
-- carried 0 — both the V204 seed and TenantGameProvisioningService leave it
-- unset. Sorting a column that is uniformly zero is a no-op, and since Java's
-- sort is stable the chips came out in whatever order the query returned:
-- Maryaj, Loto 4, Bòlèt, Loto 5, Loto 3. Bòlèt is the game sold most and it
-- sat third.
--
-- game.sort_order already holds the intended sequence (10/30/35/40/50/60), it
-- was simply never carried across. This copies it.
--
-- Maryaj gratis is pushed to the end rather than kept at 35. The POS filters it
-- out entirely — it is generated automatically — so this only matters to any
-- other surface that lists games, where it belongs after the games a seller
-- actually picks.

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

UPDATE tenant_game tg
SET display_order = CASE
      WHEN g.code = 'HT_MARYAJ_GRATIS' THEN 999
      ELSE g.sort_order
    END,
    updated_at = now()
FROM game g
WHERE g.id = tg.game_id
  AND tg.deleted_at IS NULL
  AND tg.display_order IS DISTINCT FROM (
        CASE WHEN g.code = 'HT_MARYAJ_GRATIS' THEN 999 ELSE g.sort_order END
      );
