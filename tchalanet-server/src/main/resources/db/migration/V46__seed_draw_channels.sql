WITH t AS (
    SELECT id AS tenant_id
    FROM tenant
    WHERE code = 'tchalanet'
    LIMIT 1
    ),
    tg AS (
SELECT tg.id AS tenant_game_id, g.code AS game_code
FROM tenant_game tg
    JOIN game g ON g.id = tg.game_id
    JOIN t ON t.tenant_id = tg.tenant_id
WHERE tg.deleted_at IS NULL
    ),
    src AS (
SELECT
    gen_random_uuid()                       AS id,
    t.tenant_id                             AS tenant_id,
    tg.tenant_game_id                       AS tenant_game_id,
    v.code                                  AS code,
    v.name                                  AS name,
    v.timezone::varchar                     AS timezone,
    v.draw_time::time                       AS draw_time,
    v.cutoff_sec                            AS cutoff_sec,
    v.days_of_week                          AS days_of_week,
    v.active                                AS active,
    v.sort_order                            AS sort_order,
    v.external_provider                     AS external_provider,
    v.external_game_key                     AS external_game_key,
    v.external_channel_code                 AS external_channel_code
FROM t
    JOIN (
    VALUES
    ('US_NY_NUM3_MID','US_NY_PICK3','NY Pick 3 Midday','America/New_York','14:30',300,'MON-SUN',true,10,'NY','NUMBERS','MID'),
    ('US_NY_NUM3_EVE','US_NY_PICK3','NY Pick 3 Evening','America/New_York','22:30',300,'MON-SUN',true,11,'NY','NUMBERS','EVE'),
    ('US_NY_NUM4_MID','US_NY_PICK4','NY Pick 4 Midday','America/New_York','14:30',300,'MON-SUN',true,20,'NY','WIN4','MID'),
    ('US_NY_NUM4_EVE','US_NY_PICK4','NY Pick 4 Evening','America/New_York','22:30',300,'MON-SUN',true,21,'NY','WIN4','EVE'),
    ('US_NY_TAKE5_EVE','US_NY_TAKE5','NY Take 5 Evening','America/New_York','22:30',300,'MON-SUN',true,30,'NY','TAKE 5','EVE'),
    ('US_FL_PICK3_MID','US_FL_PICK3','FL Pick 3 Midday','America/New_York','13:30',400,'MON-SUN',true,40,'FL','PICK3','MID'),
    ('US_FL_PICK3_EVE','US_FL_PICK3','FL Pick 3 Evening','America/New_York','22:45',500,'MON-SUN',true,41,'FL','PICK3','EVE'),
    ('US_FL_PICK4_MID','US_FL_PICK4','FL Pick 4 Midday','America/New_York','13:30',400,'MON-SUN',true,50,'FL','PICK4','MID'),
    ('US_FL_PICK4_EVE','US_FL_PICK4','FL Pick 4 Evening','America/New_York','22:45',500,'MON-SUN',true,51,'FL','PICK4','EVE'),
    ('US_FL_LOTTO','US_FL_LOTTO','Florida Lotto','America/New_York','23:15',200,'WED-SAT',true,60,'FL','FL_LOTTO','EVE')
    ) AS v(code, game_code, name, timezone, draw_time, cutoff_sec, days_of_week, active, sort_order, external_provider, external_game_key, external_channel_code)
ON TRUE
    JOIN tg ON tg.game_code = v.game_code
    )
INSERT INTO draw_channel (
    id, tenant_id, tenant_game_id,
    code, name, timezone, draw_time, cutoff_sec, days_of_week, active, sort_order,
    external_provider, external_game_key, external_channel_code
)
SELECT
    id, tenant_id, tenant_game_id,
    code, name, timezone, draw_time, cutoff_sec, days_of_week, active, sort_order,
    external_provider, external_game_key, external_channel_code
FROM src
    ON CONFLICT (tenant_id, code) DO UPDATE
                                         SET
                                             tenant_game_id         = EXCLUDED.tenant_game_id,
                                         name                   = EXCLUDED.name,
                                         timezone               = EXCLUDED.timezone,
                                         draw_time              = EXCLUDED.draw_time,
                                         cutoff_sec             = EXCLUDED.cutoff_sec,
                                         days_of_week           = EXCLUDED.days_of_week,
                                         active                 = EXCLUDED.active,
                                         sort_order             = EXCLUDED.sort_order,
                                         external_provider      = EXCLUDED.external_provider,
                                         external_game_key      = EXCLUDED.external_game_key,
                                         external_channel_code  = EXCLUDED.external_channel_code;

-- Sanity check
SELECT t.id AS tenant_id,
       (SELECT count(*) FROM draw_channel dc WHERE dc.tenant_id = t.id AND dc.deleted_at IS NULL) AS draw_channel_count
FROM tenant t
WHERE t.code = 'tchalanet'
    LIMIT 1;
