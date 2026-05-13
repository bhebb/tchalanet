# Platform Capability `platform.audit` — Audit Trail

> Archetype : Application Service Module. Migré depuis `core.audit`.

## 1. Rôle

Enregistrer les traces d'audit des opérations critiques (writes métier, admin actions) et les exposer en lecture.

**Ce module fait** :
- Persister des entrées d'audit (`AuditEntry`) avec contexte tenant/actor/action/entity.
- Exposer une API de lecture pour les tableaux de bord audit.
- Publier via `@AuditLog` AOP ou appel direct à `AuditApi`.

**Ce module ne fait pas** :
- Évaluation de permissions (→ `platform.accesscontrol`).
- Audit de sécurité Keycloak (→ infrastructure externe).

## 2. Structure

```text
platform/audit/
  api/
    AuditApi.java             ← record(AuditEntry) + query methods
    model/
      AuditEntry.java         ← record immuable
      AuditRow.java           ← read model
  internal/
    service/                  ← AuditService (implémente AuditApi)
    persistence/              ← AuditJpaEntity, AuditRepository
    web/                      ← AuditAdminController (/api/v1/platform/audit)
    config/
```

## 3. Transaction

Peut utiliser `REQUIRES_NEW` pour garantir l'enregistrement même si la transaction principale roll-back (audit de failure).

## 4. Règles

- RLS actif sur la table `audit_entry` (`tenant_id` géré par TchContext).
- `core` ne doit pas écouter les events de ce module.
- Core et features injectent `AuditApi` — jamais `AuditService`.
