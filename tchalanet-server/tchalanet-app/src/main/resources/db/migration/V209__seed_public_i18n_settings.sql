SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

-- ============================================================
-- Public i18n — GLOBAL rows for public surfaces
-- These are the baseline translations served by GET /public/i18n.
-- Tenant admins may override per-key per-surface with TENANT rows.
-- ============================================================

WITH public_i18n(surface, locale, i18n_key, i18n_value) AS (VALUES

  -- PUBLIC_HOME
  ('PUBLIC_HOME', 'fr', 'home.hero.title',       'Bienvenue'),
  ('PUBLIC_HOME', 'fr', 'home.hero.subtitle',    'Jouez. Vérifiez. Gagnez.'),
  ('PUBLIC_HOME', 'fr', 'home.hero.cta',         'Vérifier un ticket'),
  ('PUBLIC_HOME', 'fr', 'home.nav.check_ticket', 'Vérifier'),
  ('PUBLIC_HOME', 'fr', 'home.nav.results',      'Résultats'),

  ('PUBLIC_HOME', 'en', 'home.hero.title',       'Welcome'),
  ('PUBLIC_HOME', 'en', 'home.hero.subtitle',    'Play. Check. Win.'),
  ('PUBLIC_HOME', 'en', 'home.hero.cta',         'Check a ticket'),
  ('PUBLIC_HOME', 'en', 'home.nav.check_ticket', 'Check'),
  ('PUBLIC_HOME', 'en', 'home.nav.results',      'Results'),

  ('PUBLIC_HOME', 'ht', 'home.hero.title',       'Byenveni'),
  ('PUBLIC_HOME', 'ht', 'home.hero.subtitle',    'Jwe. Verifye. Genyen.'),
  ('PUBLIC_HOME', 'ht', 'home.hero.cta',         'Verifye yon tikè'),
  ('PUBLIC_HOME', 'ht', 'home.nav.check_ticket', 'Verifye'),
  ('PUBLIC_HOME', 'ht', 'home.nav.results',      'Rezilta'),

  -- PUBLIC_TICKET_CHECK
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.title',               'Vérifier un ticket'),
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.input.placeholder',   'Entrez votre code public'),
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.action.verify',       'Vérifier'),
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.result.won',          'Ticket gagnant'),
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.result.lost',         'Ticket non gagnant'),
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.result.pending',      'Résultat en attente'),
  ('PUBLIC_TICKET_CHECK', 'fr', 'ticket_check.result.not_found',    'Ticket introuvable'),

  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.title',               'Check a ticket'),
  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.input.placeholder',   'Enter your public code'),
  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.action.verify',       'Check'),
  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.result.won',          'Winning ticket'),
  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.result.lost',         'Non-winning ticket'),
  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.result.pending',      'Result pending'),
  ('PUBLIC_TICKET_CHECK', 'en', 'ticket_check.result.not_found',    'Ticket not found'),

  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.title',               'Verifye yon tikè'),
  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.input.placeholder',   'Antre kòd piblik ou'),
  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.action.verify',       'Verifye'),
  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.result.won',          'Tikè genyen'),
  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.result.lost',         'Tikè pa genyen'),
  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.result.pending',      'Rezilta poko pare'),
  ('PUBLIC_TICKET_CHECK', 'ht', 'ticket_check.result.not_found',    'Tikè pa jwenn'),

  -- PUBLIC_RESULTS
  ('PUBLIC_RESULTS', 'fr', 'results.title',          'Résultats des tirages'),
  ('PUBLIC_RESULTS', 'fr', 'results.empty',          'Aucun résultat disponible'),
  ('PUBLIC_RESULTS', 'fr', 'results.draw.date',      'Date'),
  ('PUBLIC_RESULTS', 'fr', 'results.draw.numbers',   'Numéros'),

  ('PUBLIC_RESULTS', 'en', 'results.title',          'Draw results'),
  ('PUBLIC_RESULTS', 'en', 'results.empty',          'No results available'),
  ('PUBLIC_RESULTS', 'en', 'results.draw.date',      'Date'),
  ('PUBLIC_RESULTS', 'en', 'results.draw.numbers',   'Numbers'),

  ('PUBLIC_RESULTS', 'ht', 'results.title',          'Rezilta tiraj'),
  ('PUBLIC_RESULTS', 'ht', 'results.empty',          'Pa gen rezilta disponib'),
  ('PUBLIC_RESULTS', 'ht', 'results.draw.date',      'Dat'),
  ('PUBLIC_RESULTS', 'ht', 'results.draw.numbers',   'Nimewo'),

  -- COMMON_PUBLIC_ERROR
  ('COMMON_PUBLIC_ERROR', 'fr', 'error.not_found',       'Page introuvable'),
  ('COMMON_PUBLIC_ERROR', 'fr', 'error.generic',         'Une erreur est survenue. Réessayez.'),
  ('COMMON_PUBLIC_ERROR', 'fr', 'error.network',         'Vérifiez votre connexion.'),

  ('COMMON_PUBLIC_ERROR', 'en', 'error.not_found',       'Page not found'),
  ('COMMON_PUBLIC_ERROR', 'en', 'error.generic',         'An error occurred. Please try again.'),
  ('COMMON_PUBLIC_ERROR', 'en', 'error.network',         'Check your connection.'),

  ('COMMON_PUBLIC_ERROR', 'ht', 'error.not_found',       'Paj pa jwenn'),
  ('COMMON_PUBLIC_ERROR', 'ht', 'error.generic',         'Gen yon erè. Eseye ankò.'),
  ('COMMON_PUBLIC_ERROR', 'ht', 'error.network',         'Verifye koneksyon ou.')

)
INSERT INTO i18n_override (level, tenant_id, surface, locale, i18n_key, i18n_value, active)
SELECT 'GLOBAL', NULL, seed.surface, seed.locale, seed.i18n_key, seed.i18n_value, true
FROM public_i18n seed
WHERE NOT EXISTS (
  SELECT 1
  FROM i18n_override existing
  WHERE existing.level = 'GLOBAL'
    AND existing.tenant_id IS NULL
    AND existing.surface = seed.surface
    AND existing.locale = seed.locale
    AND existing.i18n_key = seed.i18n_key
    AND existing.deleted_at IS NULL
);

-- ============================================================
-- Public runtime settings — GLOBAL rows, exposure = PUBLIC_RUNTIME
-- These are served by GET /public/settings.
-- ============================================================

INSERT INTO app_setting (
    level, namespace, setting_key, value_type, setting_value, active, exposure
)
VALUES
    ('GLOBAL', 'ui.i18n',       'default_locale',    'STRING',  'fr',       true, 'PUBLIC_RUNTIME'),
    ('GLOBAL', 'ui.i18n',       'supported_locales', 'STRING',  'fr,en,ht', true, 'PUBLIC_RUNTIME'),
    ('GLOBAL', 'ui.public_home','variant',            'STRING',  'v1',       true, 'PUBLIC_RUNTIME')
ON CONFLICT DO NOTHING;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
