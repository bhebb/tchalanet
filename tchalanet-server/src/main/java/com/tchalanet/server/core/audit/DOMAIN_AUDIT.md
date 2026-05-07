# Domaine Audit — `core.audit`

> Type: Functional Domain Specification  
> Scope: `com.tchalanet.server.core.audit`  
> Criticalité: HIGH (Conformité / Sécurité / Traçabilité)

---

## 1. But du domaine

Le domaine **Audit** assure la traçabilité applicative des actions métier importantes.

Il fournit :

- un journal lisible métier ("qui a fait quoi ?");
- une API de lecture paginée et filtrée (admin / superadmin);
- une intégration technique avec Hibernate Envers pour l'historique technique des entités.

### 1.1 Ce que le domaine fait

- Publie des `AuditEvent` applicatifs _après commit_.
- Persiste les audits métier dans la table `audit_event`.
- Expose des queries de lecture (filtres tenant/date/actor/entity/action).
- Centralise les conventions et la factory d'audit.

### 1.2 Ce que le domaine ne fait PAS

- Authentification / login (Keycloak).
- Historisation manuelle d'entités (usage d'Envers pour les révisions techniques).
- Log technique ou debug applicatif.

---

## 2. Modèle & invariants

### 2.1 `AuditEvent` (concept)

- Objet immuable.
- Construit à partir du contexte applicatif (`TchRequestContext`) et d'un payload métier.
- Persisté uniquement _après commit_ (garantie transactionnelle).

Champs logiques recommandés :

- `tenantId`
- `actorType`
- `actorId`
- `entityType`
- `entityId`
- `action`
- `details` (JSON)
- `occurredAt` (timestamp)

### 2.2 Invariants

- Le journal d'audit est _read-only_ (aucune modification) ;
- Aucun audit ne doit être écrit si la transaction rollback ;
- Le filtrage tenant est obligatoire (RLS ou équivalent) pour toute lecture/exposition.

---

## 3. Cas d'utilisation (application layer)

**Emplacement** :

- `core.audit.application.command`
- `core.audit.application.query`

### 3.1 Commands

- `PublishAuditEventCommand` : utilisé par les autres domaines via la factory, exécuté _after commit_.

### 3.2 Queries

- `ListAuditEventsQuery` : filtres (tenant, date, actor, entity, action) + pagination obligatoire.

---

## 4. Ports (hexagonal)

**Emplacement** : `core.audit.application.port.out`

Ports requis :

- `AuditEventWriterPort`
- `AuditEventReaderPort`

Remarque : les autres domaines n'interagissent qu'avec la factory/event publisher, pas directement avec les ports.

---

## 5. Intégrations techniques

### 5.1 Publication after-commit

- Utiliser `common.tx.AfterCommit` (ou TransactionSynchronizationManager) pour garantir :
  - pas d'écriture d'audit si rollback ;
  - cohérence entre état DB et events persistés.

Exemple :

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
  @Override
  public void afterCommit() {
    repo.saveAll(events);
  }
});
```

### 5.2 Envers (historique technique)

- Hibernate Envers est utilisé pour les révisions techniques (`_aud` tables).
- Envers n'est PAS un substitut à l'audit métier : usage complémentaire.

#### Exemple de table de révision (revinfo)

```sql
CREATE TABLE revinfo (
  rev               INT PRIMARY KEY,
  rev_timestamp     BIGINT NOT NULL,
  tenant_id         UUID,
  user_id           UUID,
  request_id        VARCHAR(128),
  actor_type        VARCHAR(32),
  api_scope         VARCHAR(32),
  tenant_overridden BOOLEAN NOT NULL DEFAULT false
);
```

> `TchRevisionListener` enrichit `revinfo` avec le contexte canonique via
> `TchContextResolver`. Il ne dépend pas de `RequestContextHolder` et ne doit jamais
> empêcher la révision Envers d'être créée si le contexte est absent.

### 5.3 Stratégie des entités historisées

- `@Audited` ne vit pas sur `AuditableEntity`, `BaseEntity` ou `BaseTenantEntity`.
- Chaque entité historisée doit porter `@Audited` explicitement.
- Toute nouvelle entité `@Audited` doit ajouter la table `_aud` correspondante via Flyway.
- `FlywayAuditAlignmentArchTest` vérifie qu'une entité JPA `@Audited` possède une table `_aud`.
- Les entités historisées prioritaires sont les agrégats et référentiels sensibles :
  IAM, tenant/user, settings, draw/drawresult, sales tickets, payout, pricing,
  limits, page models, notifications.

---

## 6. Schéma métier : `audit_event`

Stocke des événements lisibles métier.

```sql
CREATE TABLE audit_event (
  id          UUID PRIMARY KEY,
  occurred_at TIMESTAMPTZ NOT NULL,
  tenant_id   UUID,
  actor_type  TEXT NOT NULL,
  actor_id    TEXT,
  entity_type TEXT NOT NULL,
  entity_id   TEXT NOT NULL,
  action      TEXT NOT NULL,
  details     JSONB,
  ip          INET,
  user_agent  TEXT
);
```

`tenant_id` est nullable pour les audits platform/global et système. Les indexes
canoniques sont :

- `(tenant_id, occurred_at desc)`
- `(entity_type, entity_id)`
- `(action, occurred_at desc)`
- `(actor_id, occurred_at desc)`

---

## 7. Composants clés

- `AuditEventFactory` : construit les `AuditEvent` à partir du `TchRequestContext` + payload métier.
- `AuditEventWriterPort` / `AuditEventReaderPort` : ports persistants.
- `AfterCommit` collector : collecte les events pendant la transaction et les persiste après commit.
- `AuditorAware<UUID>` : alimente `@CreatedBy` / `@LastModifiedBy`.

---

## 8. RLS & multi-tenant

- `audit_event` accepte les lignes tenant-scoped et platform/global.
- Les tenants voient leur propre `tenant_id`.
- Les super-admin platform peuvent consulter les audits cross-tenant et platform selon RLS.
- Les écritures platform/global peuvent avoir `tenant_id = null`.

---

## 9. Performance & volumétrie

- Les événements d'audit peuvent croître rapidement. Pratiques recommandées :
  - indexation ciblée (`tenant_id`, `ts`, `entity_type`) ;
  - archivage / purge planifiée (batch) ;
  - agrégations/exports pour BI.

## 10. Couverture applicative canonique

Cette matrice est la référence de couverture pour les actions sensibles. Elle doit
être mise à jour dans la même PR que tout nouveau endpoint ou handler sensible.

| Action                        | Entity        | Endpoint / use-case                          | Trigger     | Entity id                | Details                | Tests            |
| ----------------------------- | ------------- | -------------------------------------------- | ----------- | ------------------------ | ---------------------- | ---------------- |
| `DRAW_GENERATE`               | `DRAW`        | `POST /platform/ops/draws/generate`          | `@AuditLog` | `tenantId`               | request + outcome      | aspect + compile |
| `DRAW_OPEN`                   | `DRAW`        | `POST /platform/ops/draws/open-due`          | `@AuditLog` | `draw-open-due`          | request + outcome      | aspect + compile |
| `DRAW_OPEN`                   | `DRAW`        | `POST /platform/ops/draws/open-today`        | `@AuditLog` | `draw-open-today`        | request + outcome      | aspect + compile |
| `DRAW_CLOSE`                  | `DRAW`        | `POST /platform/ops/draws/close-due`         | `@AuditLog` | `draw-close-due`         | request + outcome      | aspect + compile |
| `DRAW_RESULT_FETCH`           | `DRAW_RESULT` | `POST /platform/ops/draw-results/fetch`      | `@AuditLog` | slot keys or `all-slots` | request + outcome      | aspect + compile |
| `DRAW_RESULT_REFRESH`         | `DRAW_RESULT` | `POST /platform/ops/draw-results/refresh`    | `@AuditLog` | slot keys or `all-slots` | request + outcome      | aspect + compile |
| `DRAW_RESULT_APPLY`           | `DRAW_RESULT` | `POST /platform/ops/draws/apply`             | `@AuditLog` | slot keys or `all-slots` | request + outcome      | aspect + compile |
| `DRAW_RESULT_OVERRIDE`        | `DRAW_RESULT` | `POST /platform/ops/draw-results/override`   | `@AuditLog` | `slotKey:drawDate`       | request + outcome      | aspect + compile |
| `DRAW_RESULT_MANUAL`          | `DRAW_RESULT` | `POST /platform/ops/draw-results/manual`     | `@AuditLog` | `slotKey:drawDate`       | request + outcome      | aspect + compile |
| `DRAW_CORRECT_APPLIED_RESULT` | `DRAW`        | `POST /admin/draws/{drawId}/results/correct` | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `DRAW_CANCEL`                 | `DRAW`        | `POST /admin/draws/{drawId}/cancel`          | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `DRAW_RESCHEDULE`             | `DRAW`        | `POST /admin/draws/{drawId}/reschedule`      | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `DRAW_LOCK`                   | `DRAW`        | `POST /admin/draws/{drawId}/lock`            | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `DRAW_UNLOCK`                 | `DRAW`        | `POST /admin/draws/{drawId}/unlock`          | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `DRAW_ARCHIVE`                | `DRAW`        | `POST /admin/draws/{drawId}/archive`         | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `DRAW_SETTLE`                 | `DRAW`        | `POST /admin/draws/{drawId}/settle`          | `@AuditLog` | `drawId`                 | request + outcome      | aspect + compile |
| `AUDIT_PURGE`                 | `SYSTEM`      | `POST /platform/audit/purge`                 | `@AuditLog` | `audit_event`            | purge result + outcome | handler test     |

### Trigger rule

- Prefer `@AuditLog` on thin Ops/admin controllers when the audited operation is
  HTTP-only and the controller has stable request/result objects.
- Prefer handler-level explicit audit for non-HTTP force operations, batch-only
  operations, or flows where the final entity id is discovered inside the use case.
- Do not audit the same action in both controller and handler unless the two
  events answer different business questions.

## 11. Purge policy

Application audit purge is controlled by `tch.audit.retention-days` and deletes
rows where `audit_event.occurred_at < threshold`.

The canonical manual operation is:

```text
POST /platform/audit/purge
```

Rules:

- `SUPER_ADMIN` only.
- Uses injected `Clock`.
- Returns `deleted`, `retentionDays`, and `threshold`.
- Audited with `AUDIT_PURGE`.
- Purge removes only application audit rows; Envers `_AUD` history is governed by
  its own retention/archive policy.

## 12. PR checklist

For any sensitive endpoint or command handler:

- [ ] Pick one canonical `AuditAction`.
- [ ] Pick one `AuditEntityType`.
- [ ] Define a stable `entityId` string; do not assume UUID.
- [ ] Include JSONB-safe details with reason/force when relevant.
- [ ] Choose exactly one trigger: `@AuditLog` or handler-level audit.
- [ ] Add or update this coverage matrix.
- [ ] Add focused tests when the audit is handler-level, force-related, or has
      custom details/id logic.

---

## 10. Modes de défaillance

- Transaction rollback → aucun audit écrit.
- Contexte absent → audit applicatif global/système possible si le use-case le permet ;
  Envers crée une révision avec `actor_type=SYSTEM`.
- Volume excessif → prévoir throttling / archivage (hors V1).

---

## 11. Tests — Definition of Done

- Audit métier écrit _après commit_ ;
- Aucun audit écrit si rollback ;
- Filtres tenant corrects dans les queries ;
- Envers génère des révisions `_aud` après update ;
- `revinfo` enrichi avec `tenantId`, `userId`, `requestId`, `actorType`, `apiScope`.

---

## 12. Mini-checklist

- [ ] Aucun audit écrit avant commit
- [ ] Envers réservé à l'historique technique
- [ ] `audit_event` pour lecture métier
- [ ] Factory centralisée (AuditEventFactory)
- [ ] Aucun audit depuis controllers (utiliser la factory)
- [ ] RLS actif pour lectures

---

_Document rédigé selon les conventions Tchalanet — voir `docs/conventions` pour la structure attendue._
