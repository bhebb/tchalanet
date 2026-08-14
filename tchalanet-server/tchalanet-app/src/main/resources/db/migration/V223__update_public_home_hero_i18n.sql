SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

WITH public_home_hero_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'home.hero.eyebrow', 'Tchalanet'),
    ('fr', 'home.hero.tagline', 'Tchalanet'),
    ('fr', 'home.hero.title', 'Une plateforme. Tout l’écosystème de la loterie.'),
    ('fr', 'home.hero.subtitle', 'Vente, gestion, tirages, résultats et vérification des tickets réunis dans une même plateforme.'),
    ('fr', 'home.hero.cta', 'Découvrir Tchalanet'),
    ('fr', 'home.hero.secondary_cta', 'Vérifier un ticket'),
    ('en', 'home.hero.eyebrow', 'Tchalanet'),
    ('en', 'home.hero.tagline', 'Tchalanet'),
    ('en', 'home.hero.title', 'One platform. The whole lottery ecosystem.'),
    ('en', 'home.hero.subtitle', 'Sales, management, draws, results and ticket verification brought together in one platform.'),
    ('en', 'home.hero.cta', 'Discover Tchalanet'),
    ('en', 'home.hero.secondary_cta', 'Verify a ticket'),
    ('ht', 'home.hero.eyebrow', 'Tchalanet'),
    ('ht', 'home.hero.tagline', 'Tchalanet'),
    ('ht', 'home.hero.title', 'Yon sèl platfòm. Tout ekosistèm lotri a.'),
    ('ht', 'home.hero.subtitle', 'Vant, jesyon, tiraj, rezilta ak verifikasyon tikè yo reyini nan yon sèl platfòm.'),
    ('ht', 'home.hero.cta', 'Dekouvri Tchalanet'),
    ('ht', 'home.hero.secondary_cta', 'Verifye yon tikè')
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
    ('fr', 'home.hero.eyebrow', 'Tchalanet'),
    ('fr', 'home.hero.tagline', 'Tchalanet'),
    ('fr', 'home.hero.title', 'Une plateforme. Tout l’écosystème de la loterie.'),
    ('fr', 'home.hero.subtitle', 'Vente, gestion, tirages, résultats et vérification des tickets réunis dans une même plateforme.'),
    ('fr', 'home.hero.cta', 'Découvrir Tchalanet'),
    ('fr', 'home.hero.secondary_cta', 'Vérifier un ticket'),
    ('en', 'home.hero.eyebrow', 'Tchalanet'),
    ('en', 'home.hero.tagline', 'Tchalanet'),
    ('en', 'home.hero.title', 'One platform. The whole lottery ecosystem.'),
    ('en', 'home.hero.subtitle', 'Sales, management, draws, results and ticket verification brought together in one platform.'),
    ('en', 'home.hero.cta', 'Discover Tchalanet'),
    ('en', 'home.hero.secondary_cta', 'Verify a ticket'),
    ('ht', 'home.hero.eyebrow', 'Tchalanet'),
    ('ht', 'home.hero.tagline', 'Tchalanet'),
    ('ht', 'home.hero.title', 'Yon sèl platfòm. Tout ekosistèm lotri a.'),
    ('ht', 'home.hero.subtitle', 'Vant, jesyon, tiraj, rezilta ak verifikasyon tikè yo reyini nan yon sèl platfòm.'),
    ('ht', 'home.hero.cta', 'Dekouvri Tchalanet'),
    ('ht', 'home.hero.secondary_cta', 'Verifye yon tikè')
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
