# Domaine core.audit — Audit applicatif & révisions

> Enregistre des audits applicatifs (actions), et s’appuie sur Envers pour l’historique des entités JPA.

---

## 1. Rôle du domaine

- Publier des `AuditEvent` (tenant, actor, operationType, entityRef, outcome, metadata).
- Exposer lecture paginée du journal audit.

**Ne fait pas**

- Décision d’autorisation.
- Historisation manuelle hors Envers.

---

## 2. Modèle & invariants

- `AuditEvent`: immutable; publié après commit.
- Invariants:
  - Audit log read-only; filtrage par tenant.

---

## 3. Use Cases

- `PublishAuditEventHandler`
- `ListAuditEventsQueryHandler`

---

## 4. Ports (out)

- `AuditEventRepoPort`

---

## 5. Intégrations

- `common.tx.AfterCommit` pour publication.
- Envers: `_aud` tables pour révisions entity.

---

## 6. Notes techniques

- Multi-tenant & RLS (si audit tenant-scoped).
- DTOs MapStruct; wrappers ID.

---

## 7. Incohérences / TODO

- Confirmer la politique de rétention et les filtres admin.

---

## 8. Stratégie d’audit (Envers + audit_event)

Tchalanet utilise deux mécanismes complémentaires :

- Envers: historique complet des entités (reconstitution d’état passé).
- audit_event: log métier lisible ("qui a fait quoi ?"), stocké en `jsonb`.

### 8.1 Objectifs

- Historiser les versions d’entités via Envers.
- Auditer les actions métier importantes via `audit_event`.

### 8.2 Envers

- Entités de base annotées `@Audited` (sur `BaseEntity` / `BaseTenantEntity`).
- Paramètres Envers configurés dans `application.yaml`.
- `TchRevisionListener` enrichit `revinfo` (tenantId, userId, requestId).
- Le `RevisionListener` doit être autonome (pas d’injection Spring par constructeur) et lire le contexte via `RequestContextHolder.get()`.

Revision table:

```sql
CREATE TABLE revinfo (
  rev INT PRIMARY KEY,
  rev_timestamp BIGINT NOT NULL,
  tenant_id UUID,
  user_id   UUID
);
```

Listener (exemple):

```java
public class TchRevisionListener implements RevisionListener {
  public void newRevision(Object revisionEntity) {
     var ctx = RequestContextHolder.get();
     rev.setTenantId(ctx.tenantId());
     rev.setUserId(ctx.userId());
  }
}
```

### 8.3 Table `audit_event`

- Stocke événements métier lisibles: actor_type, actor_id, entity_type, entity_id, action, details (jsonb), ip, user_agent.

Schema (exemple):

```sql
CREATE TABLE audit_event (
  id bigserial PRIMARY KEY,
  ts timestamptz NOT NULL DEFAULT now(),
  tenant_id uuid NOT NULL,
  actor_type text NOT NULL,
  actor_id text NOT NULL,
  entity_type text NOT NULL,
  entity_id text NOT NULL,
  action text NOT NULL,
  details jsonb NOT NULL,
  ip inet,
  user_agent text
);
```

### 8.4 AuditEventFactory & enregistrement

- `AuditEventFactory` construit les événements à partir du `TchRequestContext` et d’un payload `details`.
- Persistance recommandée AFTER COMMIT (TransactionSynchronization) pour ne refléter que les transactions commitées.

Exemple factory:

```java
public AuditEvent event(String entity, String id, String action, Object details) {
   return new AuditEvent(...);
}
```

### 8.5 Implémentation afterCommit

- Collecter des `AuditEvent` (ThreadLocal ou collection).
- `TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { repo.saveAll(events); } });`

### 8.6 AuditorAware

- `AuditorAware<UUID>` (ex: `RequestContextAuditorAware`) pour remplir `@CreatedBy` / `@LastModifiedBy`.
- `getCurrentAuditor()` retourne `Optional<UUID>` depuis `TchRequestContext.userId`.

### 8.7 Bonnes pratiques

- Envers pour historique complet; `audit_event` pour lecture métier.
- Stocker les détails métier dans `details` (jsonb) via `AuditEventFactory`.
- Gérer la volumétrie (archiver/synthétiser si besoin).

### 8.8 Tests

- Envers: vérifier `_aud` après modifications.
- `audit_event` afterCommit: s’assurer que rollback n’enregistre pas l’audit.

---

## 9. Visualisation

- `_aud`: interne/technique.
- `audit_event`: exploitable par front admin / BI.
