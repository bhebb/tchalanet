SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

WITH public_home_hero_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'home.hero.eyebrow', 'TCHALANET'),
    ('fr', 'home.hero.tagline', 'TCHALANET'),
    ('fr', 'home.hero.title', 'Une plateforme pour tout l’écosystème de la loterie.'),
    ('fr', 'home.hero.subtitle', 'Vente, gestion, tirages, résultats et vérification…'),
    ('fr', 'home.hero.cta', 'Vérifier un ticket'),
    ('fr', 'home.hero.secondary_cta', 'Découvrir Tchalanet'),
    ('en', 'home.hero.eyebrow', 'TCHALANET'),
    ('en', 'home.hero.tagline', 'TCHALANET'),
    ('en', 'home.hero.title', 'One platform for the whole lottery ecosystem.'),
    ('en', 'home.hero.subtitle', 'Sales, management, draws, results and verification…'),
    ('en', 'home.hero.cta', 'Verify a ticket'),
    ('en', 'home.hero.secondary_cta', 'Discover Tchalanet'),
    ('ht', 'home.hero.eyebrow', 'TCHALANET'),
    ('ht', 'home.hero.tagline', 'TCHALANET'),
    ('ht', 'home.hero.title', 'Yon sèl platfòm pou tout ekosistèm lotri a.'),
    ('ht', 'home.hero.subtitle', 'Vant, jesyon, tiraj, rezilta ak verifikasyon…'),
    ('ht', 'home.hero.cta', 'Verifye yon tikè'),
    ('ht', 'home.hero.secondary_cta', 'Dekouvri Tchalanet')
)
INSERT INTO i18n_override (level, tenant_id, surface, locale, i18n_key, i18n_value, active)
SELECT 'GLOBAL', NULL, 'PUBLIC_HOME', seed.locale, seed.i18n_key, seed.i18n_value, true
FROM public_home_hero_i18n seed
WHERE NOT EXISTS (
  SELECT 1
  FROM i18n_override existing
  WHERE existing.level = 'GLOBAL'
    AND existing.tenant_id IS NULL
    AND existing.surface = 'PUBLIC_HOME'
    AND existing.locale = seed.locale
    AND existing.i18n_key = seed.i18n_key
    AND existing.deleted_at IS NULL
);

WITH public_home_hero_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'home.hero.eyebrow', 'TCHALANET'),
    ('fr', 'home.hero.tagline', 'TCHALANET'),
    ('fr', 'home.hero.title', 'Une plateforme pour tout l’écosystème de la loterie.'),
    ('fr', 'home.hero.subtitle', 'Vente, gestion, tirages, résultats et vérification…'),
    ('fr', 'home.hero.cta', 'Vérifier un ticket'),
    ('fr', 'home.hero.secondary_cta', 'Découvrir Tchalanet'),
    ('en', 'home.hero.eyebrow', 'TCHALANET'),
    ('en', 'home.hero.tagline', 'TCHALANET'),
    ('en', 'home.hero.title', 'One platform for the whole lottery ecosystem.'),
    ('en', 'home.hero.subtitle', 'Sales, management, draws, results and verification…'),
    ('en', 'home.hero.cta', 'Verify a ticket'),
    ('en', 'home.hero.secondary_cta', 'Discover Tchalanet'),
    ('ht', 'home.hero.eyebrow', 'TCHALANET'),
    ('ht', 'home.hero.tagline', 'TCHALANET'),
    ('ht', 'home.hero.title', 'Yon sèl platfòm pou tout ekosistèm lotri a.'),
    ('ht', 'home.hero.subtitle', 'Vant, jesyon, tiraj, rezilta ak verifikasyon…'),
    ('ht', 'home.hero.cta', 'Verifye yon tikè'),
    ('ht', 'home.hero.secondary_cta', 'Dekouvri Tchalanet')
)
UPDATE i18n_override existing
SET i18n_value = seed.i18n_value
FROM public_home_hero_i18n seed
WHERE existing.level = 'GLOBAL'
  AND existing.tenant_id IS NULL
  AND existing.surface = 'PUBLIC_HOME'
  AND existing.locale = seed.locale
  AND existing.i18n_key = seed.i18n_key
  AND existing.deleted_at IS NULL
  AND existing.i18n_value IS DISTINCT FROM seed.i18n_value;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
