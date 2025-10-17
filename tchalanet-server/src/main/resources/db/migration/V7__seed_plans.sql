INSERT INTO plans (code, name, description, price_amount, currency, billing_frequency, public_plan, features)
VALUES ('BASIC', 'Basic', 'Basic plan', 0.00, 'EUR', 'MONTH', TRUE,
        '["plans.feat.public_pages","plans.feat.theming"]'::jsonb),
       ('PRO', 'Pro', 'Pro plan', 19.00, 'EUR', 'MONTH', TRUE,
        '["plans.feat.public_pages","plans.feat.theming","plans.feat.analytics"]'::jsonb),
       ('ENTERPRISE', 'Enterprise', 'Enterprise plan', 49.00, 'EUR', 'MONTH', TRUE,
        '["plans.feat.public_pages","plans.feat.theming","plans.feat.analytics","plans.feat.sso"]'::jsonb) ON CONFLICT (code) DO NOTHING;
