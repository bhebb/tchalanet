-- V224: make seeded tenant games ready for sale without manual stake setup.

UPDATE tenant_game
SET min_stake = 1,
    max_stake = 1000000,
    availability_enabled = true,
    updated_at = now()
WHERE game_code IN ('HT_BOLET','HT_MARYAJ','HT_MARYAJ_GRATIS','HT_LOTO3','HT_LOTO4','HT_LOTO5')
  AND deleted_at IS NULL;
