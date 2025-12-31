-- V41: seed plans (FREE / BASIC / ENTERPRISE)
DO $$ BEGIN
  RAISE NOTICE 'V41__seed_plans: seeding plans (FREE/BASIC/ENTERPRISE)';
END $$;

INSERT INTO plan (id, code, name, description, price_amount, currency, billing_frequency, public_plan, features)
VALUES
    ('00000000-0000-0000-0000-000000000201','FREE','Free',
     'Découverte: pages publiques + résultats + vérification de ticket',
     0.00,'USD','MONTHLY',true,
     '{
       "limits":{"outlets":1,"cashiers":2},
       "features":[
         "Page publique (Home + news + résultats)",
         "Vérifier un ticket (/verifier + /ticket/:code)",
         "Résultats US (NY/FL) en lecture",
         "Support de base (email)"
       ]
     }'::jsonb
    ),
    ('00000000-0000-0000-0000-000000000202','BASIC','Basic',
     'Pour un opérateur: dashboards + POS web + rapports simples',
     99.00,'USD','MONTHLY',true,
     '{
       "limits":{"outlets":5,"cashiers":25},
       "features":[
         "Dashboard privé (vendeur/opérateur/admin)",
         "POS Web (vente + sessions) - v1",
         "Gestion jeux par tenant (activer/désactiver canaux)",
         "Rapports simples (ventes/jour, par PDV, par caissier)",
         "Support prioritaire"
       ]
     }'::jsonb
    ),
    ('00000000-0000-0000-0000-000000000203','ENTERPRISE','Enterprise',
     'Multi-PDV, conformité, options avancées (sur devis)',
     299.00,'USD','MONTHLY',true,
     '{
       "limits":{"outlets":999,"cashiers":9999},
       "features":[
         "Multi-PDV + workflows (autonomy policy)",
         "Audit & conformité renforcés",
         "Exports & intégrations (API/BI) - v2",
         "SLA + support dédié"
       ]
     }'::jsonb
    )
    ON CONFLICT (code) DO UPDATE
      SET name = EXCLUDED.name,
          description = EXCLUDED.description,
          price_amount = EXCLUDED.price_amount,
          currency = EXCLUDED.currency,
          billing_frequency = EXCLUDED.billing_frequency,
          public_plan = EXCLUDED.public_plan,
          features = EXCLUDED.features;

-- Sanity check
DO $$
DECLARE cnt int;
BEGIN
  SELECT count(*) INTO cnt FROM plan WHERE code IN ('FREE','BASIC','ENTERPRISE');
  IF cnt < 3 THEN
    RAISE EXCEPTION 'V41__seed_plans sanity check failed: expected 3 plans, found %', cnt;
  ELSE
    RAISE NOTICE 'V41__seed_plans sanity check OK: % plans present', cnt;
  END IF;
END $$;

