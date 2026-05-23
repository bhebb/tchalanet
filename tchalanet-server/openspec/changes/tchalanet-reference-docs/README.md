# Tchalanet — Reference Docs Pack

Ce pack consolide les documents de référence qui doivent devenir la base normative de Tchalanet.

## Documents inclus

```text
INDEX.md
reference/SECURITY_REFERENCE.md
reference/PERSISTENCE_REFERENCE.md
reference/BATCH_SCHEDULER_REFERENCE.md
reference/TRANSACTION_SECURITY_TEST_PLAN.md
openspec/security-platform-hardening/specs/security-platform-hardening.md
```

## Intention

Ces documents remplacent les notes éparpillées. Ils fixent :

- les règles de sécurité transactionnelle ;
- les règles persistence/RLS/audit/idempotence ;
- les règles batch/scheduler ;
- les flows Web/Mobile/POS ;
- les tests minimaux pour affirmer la crédibilité de la plateforme.

## Règle de gouvernance

En cas de conflit :

1. `docs/ARCHITECTURE.md`
2. `docs/PLAYBOOK.md`
3. ces documents de référence
4. OpenSpec de mise en œuvre
5. commentaires de PR / tickets

Aucune PR liée à sécurité, persistence, batch ou scheduler ne doit être mergée si elle contredit ces documents sans ADR explicite.
