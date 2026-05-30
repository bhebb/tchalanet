
# Platform Capability `platform.audit`


## Rôle

`platform.audit` gère l’audit fonctionnel : historique des actions métier/utilisateur.

**Ce module fait** :
- Enregistrement d’événements d’audit métier (succès, échec, refus)
- Exposition d’une API pour loguer, lister, purger les événements d’audit
- Annotation `@AuditLog` pour audit automatique sur endpoints critiques

**Ce module ne fait pas** :
- Audit technique de révision (Envers)
- Audit d’infrastructure technique


## Surface API

- `AuditApi` (Java) :
  - `logAuditEvent(LogAuditEventRequest)`
  - `listAuditEvents(ListAuditEventsRequest)`
  - `purgeOldAuditEvents(PurgeOldAuditEventsRequest)`
- Annotation `@AuditLog` (méthode/type) : audit automatique


## Intégration

- Les endpoints critiques utilisent `@AuditLog` pour générer automatiquement des entrées d’audit
- Les modules platform/core peuvent loguer explicitement via `AuditApi`
- Les événements d’audit sont paginés et consultables (TchPage)


## Règles et limitations

- L’audit fonctionnel ne remplace pas l’audit technique (Envers)
- Les actions refusées ou échouées doivent aussi être auditées
- L’audit doit être émis après commit de la transaction principale (succès)
- Les échecs d’infrastructure d’audit doivent être logués/observés
- Audit infrastructure failure must not turn an already committed business operation into a client
  failure.

## Public Surface

Consumers outside the capability use only:

```text
platform/audit/api/
```

Implementation stays private:

```text
platform/audit/internal/
```

Core and features inject `AuditApi`, never internal services/repositories.

## HTTP API

Audit endpoints are read-only search/read endpoints. Public HTTP writes to audit are forbidden.

Tenant-scoped reads must respect RLS/effective tenant. Platform override must be explicit and
auditable.

## Guardrails

- Functional audit persistence belongs to `platform.audit`, not `common.persistence`.
- Do not duplicate the same audit action in controller and handler unless they represent different
  actions.
- Envers is not a substitute for functional audit.
- Audit failures do not rollback successful business operations.
