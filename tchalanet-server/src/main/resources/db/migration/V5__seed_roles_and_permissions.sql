-- V5__seed_roles_and_permissions.sql
-- Seed initial des rôles applicatifs, permissions et mapping rôle -> permissions.

-- ===================
-- 1. APP ROLES (rôles applicatifs globaux, is_system = true)
-- ===================

INSERT INTO app_role (code, name, description, tenant_id, is_system)
VALUES
    ('SUPER_ADMIN', 'Super administrateur plateforme', 'Accès complet à la plateforme', NULL, true),
    ('TENANT_ADMIN', 'Admin opérateur', 'Administrateur d’un tenant / opérateur', NULL, true),
    ('OPERATOR', 'Opérateur / chef de PDV', 'Gère un point de vente', NULL, true),
    ('CASHIER', 'Caissier', 'Vente de tickets', NULL, true)
    ON CONFLICT (tenant_id, code) DO NOTHING;


-- ===================
-- 2. PERMISSIONS (globales)
-- ===================

INSERT INTO permission (code, name, description)
VALUES
    -- Tickets
    ('ticket.create', 'Créer un ticket', 'Création de tickets de jeu'),
    ('ticket.print',  'Imprimer un ticket', 'Impression ou export de ticket'),
    ('ticket.view',   'Voir / lister les tickets', 'Consultation des tickets (caissier / opérateur)'),
    ('ticket.void',   'Annuler un ticket', 'Annulation avant tirage ou sous conditions'),
    ('ticket.pay',    'Payer un ticket gagnant', 'Validation du paiement des tickets gagnants'),

    -- Tirages
    ('draw.view',        'Voir les tirages', 'Consultation des tirages'),
    ('draw.override',    'Modifier les résultats d’un tirage', 'Override manuel des résultats'),

    -- US Lottery / intégrations externes
    ('uslottery.refresh', 'Forcer le refresh des résultats US Lottery', 'Rafraîchissement manuel des résultats externes'),

    -- Sessions POS
    ('session.open',        'Ouvrir une session POS', 'Ouverture de session caissier'),
    ('session.close',       'Fermer sa session', 'Clôture de sa propre session'),
    ('session.force_close', 'Forcer la fermeture de sessions', 'Fermeture forcée des sessions d’un autre utilisateur'),

    -- Limites & configuration risques
    ('limits.manage', 'Gérer les limites et odds', 'Configuration des limites et des cotes'),

    -- Administration / plateforme
    ('tenant.manage', 'Gérer les tenants', 'Gestion des opérateurs / tenants (plateforme)'),
    ('roles.manage',  'Gérer les rôles & permissions', 'Gestion des rôles applicatifs et de leurs permissions'),
    ('users.manage',  'Gérer les utilisateurs', 'Gestion des utilisateurs (assignation de rôles, etc.)'),
    ('pages.manage',  'Gérer les modèles de page & contenu dynamique', 'Gestion du PageModel et contenu dynamique')
    ON CONFLICT (code) DO NOTHING;


-- ===================
-- 3. MAPPING ROLE -> PERMISSIONS
-- ===================
-- NB: on utilise la table role_permission (role_id, permission_code)

-- CASHIER
INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM app_role r, permission p
WHERE r.code = 'CASHIER'
  AND r.tenant_id IS NULL
  AND p.code IN (
                 'ticket.create',
                 'ticket.print',
                 'ticket.view',
                 'session.open',
                 'session.close'
    )
    ON CONFLICT DO NOTHING;


-- OPERATOR
INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM app_role r, permission p
WHERE r.code = 'OPERATOR'
  AND r.tenant_id IS NULL
  AND p.code IN (
                 'ticket.view',
                 'ticket.void',
                 'ticket.pay',
                 'draw.view',
                 'session.force_close'
    )
    ON CONFLICT DO NOTHING;


-- TENANT_ADMIN
INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM app_role r, permission p
WHERE r.code = 'TENANT_ADMIN'
  AND r.tenant_id IS NULL
  AND p.code IN (
                 'ticket.view',
                 'ticket.void',
                 'ticket.pay',
                 'draw.view',
                 'draw.override',
                 'limits.manage',
                 'roles.manage',
                 'users.manage',
                 'pages.manage'
    )
    ON CONFLICT DO NOTHING;


-- SUPER_ADMIN : toutes les permissions
INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM app_role r, permission p
WHERE r.code = 'SUPER_ADMIN'
  AND r.tenant_id IS NULL
    ON CONFLICT DO NOTHING;
