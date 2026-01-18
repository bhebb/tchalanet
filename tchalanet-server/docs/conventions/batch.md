# Backend Batch Conventions (Jobs/Schedulers)

Dernière mise à jour: 2026-01-17

Conventions pour **ajouter/modifier** des batchs (jobs, schedulers) dans un domaine backend.

---

## Principes

- **Hexagonal + CQRS**: un batch orchestre des handlers (commands/queries), pas de logique métier directe.
- **Contexte & RLS**: fixer `TchRequestContext` avant toute opération DB ; utiliser `RlsAwareDataSource`.
- **Idempotence**: un job doit être re‐jouable sans effets indésirables ; utiliser clés d’idempotence si applicable.
- **AfterCommit**: publier les events métier **après commit**.

## Ajout d’un batch (Checklist)

1. Définir l’**objectif** et la **fréquence** (cron/fixed delay).
2. Construire `TchRequestContext` (tenant, user technique, roles) pour l’exécution.
3. Orchestrer via `CommandBus` / `QueryBus` (handlers existants).
4. Encadrer transactionnellement (`@TchTx`) les writes ; lecture sans transaction possible.
5. Gérer **erreurs** et **retries** ; journaliser structuré.
6. Tests unitaires (scénarios happy path / erreurs / edge cases).
7. Documentation (ce fichier + `DOMAIN.md` si métier impacté).

## Modification d’un batch (Checklist)

1. Évaluer l’impact (fréquence, charge, collisions, windows).
2. Valider idempotence et transactions (pas de double‐post côté ledger, etc.).
3. Mettre à jour contexte (tenant override si SUPER_ADMIN job global).
4. Mettre à jour les tests.
5. Mettre à jour la doc.

## Contexte & RLS (rappel)

- `TchContextFilter` construit le contexte HTTP ; pour les jobs, construire un `TchRequestContext` manuellement.
- Avant DB:

```java
try {
  TchContext.set(ctx);
  // opérations DB via RlsAwareDataSource
} finally {
  TchContext.clear();
}
```

## Idempotence (rappel)

- Utiliser `Idempotency-Key` pour les writes critiques (si via endpoints).
- Unicité côté DB (unique constraints) pour ledger/posting si consommateur d’événements.

## Logs & Monitoring

- Logs structurés (tenant, jobId, requestId, timestamps).
- Metrics (durée, taux réussite/échec) ; alerte si dépassements.

## Liens techniques

- Contexte/RLS: `tchalanet-server/docs/rls.md`
- Architecture: `tchalanet-server/docs/ARCHITECTURE.md`
- Playbook (procédure): `tchalanet-server/docs/PLAYBOOK.md`
