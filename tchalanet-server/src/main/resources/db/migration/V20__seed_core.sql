-- V20: seed core (tenants, games, plans, themes, roles/permissions)

-- Tenants (demo, tchalanet)
INSERT INTO tenant (id, code, name, timezone, currency, status, type)
VALUES
  ('00000000-0000-0000-0000-000000000002', 'demo', 'Demo Tenant', 'America/Toronto', 'USD', 'ACTIVE', 'BORLETTE'),
  ('00000000-0000-0000-0000-000000000003', 'tchalanet', 'Tchalanet', 'America/Toronto', 'USD', 'ACTIVE', 'BORLETTE')
ON CONFLICT (code) DO NOTHING;

-- Games (sample)
INSERT INTO game (id, code, name, category, min_digits, max_digits, combination, active, sort_order)
VALUES
  ('00000000-0000-0000-0000-000000000101', 'US_LOTTERY', 'US Lottery', 'LOTTO', 1, 6, 'STRAIGHT', true, 1),
  ('00000000-0000-0000-0000-000000000102', 'BORLETTE_3', 'Borlette 3', 'BORLETTE', 3, 3, 'STRAIGHT', true, 2)
ON CONFLICT (code) DO NOTHING;

-- Plans
INSERT INTO plan (id, code, name, description, price, currency, period, active)
VALUES
  ('00000000-0000-0000-0000-000000000201', 'PLAN_DEMO', 'Demo Plan', 'Plan de démonstration', 0, 'USD', 'MONTHLY', true),
  ('00000000-0000-0000-0000-000000000202', 'PLAN_STD', 'Standard', 'Abonnement standard', 199, 'USD', 'MONTHLY', true)
ON CONFLICT (code) DO NOTHING;

-- Themes
INSERT INTO theme (id, tenant_id, code, name, definition, active)
VALUES
  ('00000000-0000-0000-0000-000000000301', NULL, 'DEFAULT_LIGHT', 'Default Light', '{}'::jsonb, true),
  ('00000000-0000-0000-0000-000000000302', NULL, 'DEFAULT_DARK', 'Default Dark', '{}'::jsonb, true)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- Roles & Permissions (ex-V5 seed regroupé ici)
-- Roles
INSERT INTO app_role (code, name, description, tenant_id, is_system)
VALUES
    ('SUPER_ADMIN', 'Super administrateur plateforme', 'Accès complet à la plateforme', NULL, true),
    ('TENANT_ADMIN', 'Admin opérateur', 'Administrateur d’un tenant / opérateur', NULL, true),
    ('OPERATOR', 'Opérateur / chef de PDV', 'Gère un point de vente', NULL, true),
    ('CASHIER', 'Caissier', 'Vente de tickets', NULL, true)
    ON CONFLICT (tenant_id, code) DO NOTHING;

-- Permissions
INSERT INTO permission (code, name, description)
VALUES
    ('ticket.create', 'Créer un ticket', 'Création de tickets de jeu'),
    ('ticket.print',  'Imprimer un ticket', 'Impression ou export de ticket'),
    ('ticket.view',   'Voir / lister les tickets', 'Consultation des tickets (caissier / opérateur)'),
    ('ticket.void',   'Annuler un ticket', 'Annulation avant tirage ou sous conditions'),
    ('ticket.pay',    'Payer un ticket gagnant', 'Validation du paiement des tickets gagnants'),
    ('draw.view',        'Voir les tirages', 'Consultation des tirages'),
    ('draw.override',    'Modifier les résultats d’un tirage', 'Override manuel des résultats'),
    ('uslottery.refresh', 'Forcer le refresh des résultats US Lottery', 'Rafraîchissement manuel des résultats externes'),
    ('session.open',        'Ouvrir une session POS', 'Ouverture de session caissier'),
    ('session.close',       'Fermer sa session', 'Clôture de sa propre session'),
    ('session.force_close', 'Forcer la fermeture de sessions', 'Fermeture forcée des sessions d’un autre utilisateur'),
    ('limits.manage', 'Gérer les limites et odds', 'Configuration des limites et des cotes'),
    ('tenant.manage', 'Gérer les tenants', 'Gestion des opérateurs / tenants (plateforme)'),
    ('roles.manage',  'Gérer les rôles & permissions', 'Gestion des rôles applicatifs et de leurs permissions'),
    ('users.manage',  'Gérer les utilisateurs', 'Gestion des utilisateurs (assignation de rôles, etc.)'),
    ('pages.manage',  'Gérer les modèles de page & contenu dynamique', 'Gestion du PageModel et contenu dynamique')
    ON CONFLICT (code) DO NOTHING;

-- Role -> Permissions mapping
INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code FROM app_role r, permission p
WHERE r.code = 'CASHIER' AND r.tenant_id IS NULL AND p.code IN ('ticket.create','ticket.print','ticket.view','session.open','session.close')
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code FROM app_role r, permission p
WHERE r.code = 'OPERATOR' AND r.tenant_id IS NULL AND p.code IN ('ticket.view','ticket.void','ticket.pay','draw.view','session.force_close')
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code FROM app_role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND r.tenant_id IS NULL AND p.code IN ('ticket.view','ticket.void','ticket.pay','draw.view','draw.override','limits.manage','roles.manage','users.manage','pages.manage')
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code FROM app_role r, permission p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id IS NULL
ON CONFLICT DO NOTHING;

