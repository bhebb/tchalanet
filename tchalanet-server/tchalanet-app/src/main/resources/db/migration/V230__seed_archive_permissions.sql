-- V230 — seed archive permission codes (data-lifecycle-archive-v1 Phase 7D)
--
-- These permissions gate archive lookup (admin/tenant scope) and archive
-- management operations (SUPER_ADMIN scope).

INSERT INTO permission (code, name, category, description, system, active)
VALUES
    ('archive.read',
     'Read archived entities',
     'archive',
     'View archived tickets, payouts and audit records via the archive lookup index.',
     true, true),
    ('archive.run',
     'Trigger archive run',
     'archive',
     'Initiate a platform-level archive run (SUPER_ADMIN scope only).',
     true, true),
    ('archive.restore',
     'Restore from archive',
     'archive',
     'Copy archived rows into temporary restore tables for investigation (SUPER_ADMIN scope only).',
     true, true),
    ('archive.objects.list',
     'List archive objects',
     'archive',
     'List archive run metadata and archive objects (SUPER_ADMIN scope only).',
     true, true)
ON CONFLICT (code) DO NOTHING;
