SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

WITH receipt_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'receipt.ticket', 'Ticket'),
    ('fr', 'receipt.public_code', 'Code public'),
    ('fr', 'receipt.sale_timestamp', 'Vente'),
    ('fr', 'receipt.terminal', 'Terminal'),
    ('fr', 'receipt.seller', 'Vendeur'),
    ('fr', 'receipt.section.draw', 'Tirage'),
    ('fr', 'receipt.draw.full_prefix', 'Tirage du'),
    ('fr', 'receipt.draw_time', 'Heure'),
    ('fr', 'receipt.line.header.no', 'No'),
    ('fr', 'receipt.line.header.stake', 'Mise'),
    ('fr', 'receipt.line.header.payout', 'Gain'),
    ('fr', 'receipt.total.stake', 'Mise'),
    ('fr', 'receipt.total.amount', 'TOTAL'),
    ('fr', 'receipt.total.max_payout', 'Gain max'),
    ('fr', 'receipt.verification', 'Verification'),
    ('fr', 'receipt.qr', 'QR'),
    ('fr', 'receipt.promotion', 'Promotion'),
    ('fr', 'receipt.promotion.free_game_line', 'Maryaj gratuit'),
    ('fr', 'receipt.promotion.boost_odds', 'Cote boostee'),
    ('fr', 'receipt.message.valid_ticket', 'Ticket Tchalanet valide'),
    ('fr', 'receipt.message.code', 'Code'),
    ('fr', 'receipt.message.games', 'Jeux'),
    ('fr', 'receipt.message.game', 'Jeu'),
    ('fr', 'receipt.message.amount', 'Montant'),
    ('fr', 'receipt.charge.sms', 'Frais SMS'),
    ('fr', 'receipt.charge.whatsapp', 'Frais WhatsApp'),
    ('fr', 'receipt.charge.email', 'Frais email'),

    ('en', 'receipt.ticket', 'Ticket'),
    ('en', 'receipt.public_code', 'Public code'),
    ('en', 'receipt.sale_timestamp', 'Sale'),
    ('en', 'receipt.terminal', 'Terminal'),
    ('en', 'receipt.seller', 'Seller'),
    ('en', 'receipt.section.draw', 'Draw'),
    ('en', 'receipt.draw.full_prefix', 'Draw for'),
    ('en', 'receipt.draw_time', 'Time'),
    ('en', 'receipt.line.header.no', 'No'),
    ('en', 'receipt.line.header.stake', 'Stake'),
    ('en', 'receipt.line.header.payout', 'Payout'),
    ('en', 'receipt.total.stake', 'Stake'),
    ('en', 'receipt.total.amount', 'TOTAL'),
    ('en', 'receipt.total.max_payout', 'Max payout'),
    ('en', 'receipt.verification', 'Verification'),
    ('en', 'receipt.qr', 'QR'),
    ('en', 'receipt.promotion', 'Promotion'),
    ('en', 'receipt.promotion.free_game_line', 'Free Maryaj'),
    ('en', 'receipt.promotion.boost_odds', 'Boosted odds'),
    ('en', 'receipt.message.valid_ticket', 'Valid Tchalanet ticket'),
    ('en', 'receipt.message.code', 'Code'),
    ('en', 'receipt.message.games', 'Games'),
    ('en', 'receipt.message.game', 'Game'),
    ('en', 'receipt.message.amount', 'Amount'),
    ('en', 'receipt.charge.sms', 'SMS fee'),
    ('en', 'receipt.charge.whatsapp', 'WhatsApp fee'),
    ('en', 'receipt.charge.email', 'Email fee'),

    ('ht', 'receipt.ticket', 'Tikè'),
    ('ht', 'receipt.public_code', 'Kòd piblik'),
    ('ht', 'receipt.sale_timestamp', 'Vant'),
    ('ht', 'receipt.terminal', 'Terminal'),
    ('ht', 'receipt.seller', 'Vandè'),
    ('ht', 'receipt.section.draw', 'Tiraj'),
    ('ht', 'receipt.draw.full_prefix', 'Tiraj pou'),
    ('ht', 'receipt.draw_time', 'Lè'),
    ('ht', 'receipt.line.header.no', 'No'),
    ('ht', 'receipt.line.header.stake', 'Miz'),
    ('ht', 'receipt.line.header.payout', 'Peman'),
    ('ht', 'receipt.total.stake', 'Miz'),
    ('ht', 'receipt.total.amount', 'TOTAL'),
    ('ht', 'receipt.total.max_payout', 'Pi gwo peman'),
    ('ht', 'receipt.verification', 'Verifikasyon'),
    ('ht', 'receipt.qr', 'QR'),
    ('ht', 'receipt.promotion', 'Pwomosyon'),
    ('ht', 'receipt.promotion.free_game_line', 'Maryaj gratis'),
    ('ht', 'receipt.promotion.boost_odds', 'Kot ogmante'),
    ('ht', 'receipt.message.valid_ticket', 'Tikè Tchalanet valid'),
    ('ht', 'receipt.message.code', 'Kod'),
    ('ht', 'receipt.message.games', 'Jwet'),
    ('ht', 'receipt.message.game', 'Jwet'),
    ('ht', 'receipt.message.amount', 'Montan'),
    ('ht', 'receipt.charge.sms', 'Fre SMS'),
    ('ht', 'receipt.charge.whatsapp', 'Fre WhatsApp'),
    ('ht', 'receipt.charge.email', 'Fre email')
)
INSERT INTO i18n_override (level, tenant_id, locale, i18n_key, i18n_value, active)
SELECT 'GLOBAL', NULL, seed.locale, seed.i18n_key, seed.i18n_value, true
FROM receipt_i18n seed
WHERE NOT EXISTS (
  SELECT 1
  FROM i18n_override existing
  WHERE existing.level = 'GLOBAL'
    AND existing.tenant_id IS NULL
    AND existing.locale = seed.locale
    AND existing.i18n_key = seed.i18n_key
    AND existing.deleted_at IS NULL
);

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
