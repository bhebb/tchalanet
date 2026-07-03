WITH maryaj_campaigns AS (
    SELECT id, tenant_id
    FROM promotion_campaign
    WHERE code = 'DEFAULT_MARYAJ_GRATIS'
      AND deleted_at IS NULL
),
inserted_rules AS (
    INSERT INTO promotion_rule (
        tenant_id,
        campaign_id,
        rule_key,
        priority,
        min_paid_total,
        created_at,
        version
    )
    SELECT
        campaign.tenant_id,
        campaign.id,
        'maryaj-gratis-default',
        100,
        1,
        now(),
        0
    FROM maryaj_campaigns campaign
    WHERE NOT EXISTS (
        SELECT 1
        FROM promotion_rule rule
        WHERE rule.tenant_id = campaign.tenant_id
          AND rule.campaign_id = campaign.id
          AND rule.rule_key = 'maryaj-gratis-default'
          AND rule.deleted_at IS NULL
    )
    RETURNING id, tenant_id, campaign_id
),
maryaj_rules AS (
    SELECT id, tenant_id, campaign_id
    FROM inserted_rules
    UNION
    SELECT rule.id, rule.tenant_id, rule.campaign_id
    FROM promotion_rule rule
    JOIN maryaj_campaigns campaign
      ON campaign.id = rule.campaign_id
     AND campaign.tenant_id = rule.tenant_id
    WHERE rule.rule_key = 'maryaj-gratis-default'
      AND rule.deleted_at IS NULL
)
INSERT INTO promotion_rule_effect (
    tenant_id,
    rule_id,
    effect_type,
    game_code,
    payout_base_amount,
    quantity,
    quantity_mode,
    max_quantity,
    quantity_tiers,
    choice_mode,
    generation_strategy,
    regenerable_before_confirm,
    max_regenerations_before_confirm,
    created_at,
    version
)
SELECT
    rule.tenant_id,
    rule.id,
    'FREE_GAME_LINE',
    'HT_MARYAJ_GRATUIT',
    50,
    1,
    'TIERED_PAID_AMOUNT',
    3,
    '[
        {"minPaidAmount": "100", "maxPaidAmount": "199", "quantity": 1},
        {"minPaidAmount": "200", "maxPaidAmount": "499", "quantity": 2},
        {"minPaidAmount": "500", "quantity": 3}
    ]'::jsonb,
    'AUTO_GENERATE',
    'RANDOM',
    true,
    3,
    now(),
    0
FROM maryaj_rules rule
WHERE NOT EXISTS (
    SELECT 1
    FROM promotion_rule_effect effect
    WHERE effect.tenant_id = rule.tenant_id
      AND effect.rule_id = rule.id
      AND effect.effect_type = 'FREE_GAME_LINE'
      AND effect.game_code = 'HT_MARYAJ_GRATUIT'
      AND effect.deleted_at IS NULL
);
