# Tchalanet — Index des documents de référence

## Objectif

Tchalanet doit pouvoir affirmer une posture sérieuse : vente mobile/POS, multi-tenant, argent réel, contexte opérationnel, offline potentiel, audit, et isolation des tenants.

La crédibilité vient de la discipline suivante :

```text
Un login identifie un utilisateur.
Un terminal trusted autorise un contexte de vente.
Une session opérationnelle valide permet une transaction.
Une transaction critique est idempotente, auditée, isolée par RLS et testée.
```

## Documents canoniques

### 1. `reference/SECURITY_REFERENCE.md`

À utiliser pour :

- login Web/Mobile/POS ;
- Keycloak ;
- JWT ;
- rôles et permissions ;
- `TchContextFilter` ;
- `TchRequestContext` ;
- `OperationalRequestContext` ;
- terminal binding ;
- téléphone/POS ;
- Face ID / biométrie ;
- idempotence transactionnelle ;
- audit de sécurité.

### 2. `reference/PERSISTENCE_REFERENCE.md`

À utiliser pour :

- RLS ;
- migrations Flyway ;
- JPA entities ;
- typed IDs ;
- audit columns ;
- soft-delete ;
- timestamps ;
- optimistic locking ;
- contraintes d’unicité ;
- processed events ;
- idempotency records.

### 3. `reference/BATCH_SCHEDULER_REFERENCE.md`

À utiliser pour :

- batch jobs ;
- schedulers ;
- draw lifecycle ;
- draw processing ;
- gates ;
- forced ops ;
- context binding ;
- retry/replay ;
- idempotence des jobs.

### 4. `reference/TRANSACTION_SECURITY_TEST_PLAN.md`

À utiliser pour :

- tester les flows de login ;
- tester mobile/POS ;
- tester vente téléphone ;
- tester permissions ;
- tester RLS ;
- tester idempotence ;
- tester batch/scheduler ;
- tester concurrence multi-tenant.

## Placement recommandé dans le repo

```text
tchalanet-server/docs/reference/
  SECURITY_REFERENCE.md
  PERSISTENCE_REFERENCE.md
  BATCH_SCHEDULER_REFERENCE.md
  TRANSACTION_SECURITY_TEST_PLAN.md
```

## Definition of Done documentaire

Une PR qui touche aux zones suivantes doit mettre à jour le document concerné :

```text
security/auth/context/permissions/terminal/session/sales/offline/payout
persistence/migrations/RLS/audit/idempotence/processed_event
batch/scheduler/ops/forced flows/retry/replay
```

Checklist :

- [ ] La règle est documentée dans un document de référence.
- [ ] L’OpenSpec dérive du document, il ne l’invente pas.
- [ ] Les tests couvrent le flow nominal et les abus.
- [ ] Les erreurs sont en `ProblemDetail`, pas en `ApiResponse`.
- [ ] Les succès sont en `ApiResponse<T>`.
- [ ] Aucune décision métier critique n’est dans le frontend.
