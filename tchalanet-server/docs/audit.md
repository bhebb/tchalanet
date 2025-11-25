# Audit & Envers — Tchalanet

Ce document explique la stratégie d'audit de Tchalanet, la cohabitation Envers + table `audit_event`, et les recommandations d'implémentation.

## Objectifs

- Garder un historique des versions d'entités (en utilisant Hibernate Envers) : utile pour reconstituer l'état passé.
- Garder un log métier (audit_event) pour événements importants (création de ticket, paiement, modification de plan, etc.).

## Envers

- Les entités de base sont annotées `@Audited` (sur `BaseEntity` / `BaseTenantEntity`) et les paramètres Envers sont configurés dans `application.yaml`.
- `TchRevisionListener` est utilisé pour enrichir la table `revinfo` avec des métadonnées (tenantId, userId, requestId).
- IMPORTANT : Envers instancie le `RevisionListener` ; ton listener doit être autonome (ne pas compter sur injection Spring par constructeur). Utilise `RequestContextHolder.get()` pour lire le contexte HTTP.

## Table `audit_event`

- La table `audit_event` (migrations V7) est prévue pour stocker des événements métiers plus lisibles que les enregistrements Envers.
- Type d'information : actor_type, actor_id, entity_type, entity_id, action, details (jsonb), ip, user_agent.

## `AuditEventFactory` & enregistrement

- `AuditEventFactory` construit les événements à partir du `TchRequestContext` et d'un payload `details` fourni par le use case.
- Pour persister les événements `audit_event` :
  - Option A (actuelle) : écrire directement depuis un listener JPA dans la même transaction — l'audit sera rollbacké si la transaction rollbacke.
  - Option B (recommandée) : collecter les événements et les persister AFTER COMMIT (TransactionSynchronization) — l'audit reflète seulement les transactions réellement commitées.

## Implémentation recommandée (afterCommit)

- Dans ton `JpaAuditEntityListener` ou dans les use cases :
  - collecter des `AuditEvent` dans un ThreadLocal (ou une liste dans la synch),
  - `TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() { afterCommit() { auditRepo.saveAll(collected); }})`.

## Auditeur (AuditorAware)

- Implementer `AuditorAware<UUID>` (ex: `RequestContextAuditorAware`) et déclarer bean `@Component("auditorAware")` pour que Spring Data JPA puisse remplir `@CreatedBy` et `@LastModifiedBy`.
- `AuditorAware.getCurrentAuditor()` doit retourner `Optional<UUID>` parsant `TchRequestContext.userId` (JWT sub) en UUID.

## Bonnes pratiques

- Envers pour historique d'entité (besoin de stockage plus lourd mais fiable), `audit_event` pour événements métier lisibles et stockés en jsonb.
- Stocker les détails métier (payload) dans `details` (jsonb) en utilisant `AuditEventFactory`.
- Gérer la volumétrie : archiver ou résumer les events anciens si nécessaire.

## Tests

- Tester Envers : vérifier que les tables `_aud` contiennent les enregistrements attendus après modification d'une entité.
- Tester audit_event persistance `afterCommit` : simuler un échec après création de l'entité pour vérifier que l'audit n'est pas enregistré si la transaction rollback.

---

# Audit — Tchalanet

## Double audit : Envers + audit_event

Tchalanet utilise **2 types d’audit complémentaires** :

| Audit           | Scope                          | Usage                     |
| --------------- | ------------------------------ | ------------------------- |
| **Envers**      | Historique complet de l'entité | Reconstitution état passé |
| **audit_event** | Audit métier lisible           | “Qui a fait quoi ?”       |

---

## 1. Envers

### Revision table

```sql
CREATE TABLE revinfo (
  rev INT PRIMARY KEY,
  rev_timestamp BIGINT NOT NULL,
  tenant_id UUID,
  user_id   UUID
);
```

### Listener

```java
public class TchRevisionListener implements RevisionListener {
  public void newRevision(Object revisionEntity) {
     var ctx = RequestContextHolder.get();
     rev.setTenantId(ctx.tenantId());
     rev.setUserId(ctx.userId());
  }
}
```

---

## 2. Table audit_event

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

---

## 3. AuditEventFactory

```java
public AuditEvent event(String entity, String id, String action, Object details) {
   return new AuditEvent(...);
}
```

---

## 4. AfterCommit Strategy

Les events ne doivent être persistés **qu’après commit** :

```java
TransactionSynchronizationManager.registerSynchronization(
  new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        repo.saveAll(events);
    }
  }
);
```

---

## 5. Quand utiliser quoi ?

| Cas                               | Solution            |
| --------------------------------- | ------------------- |
| Besoin de roll‑up historique      | Envers              |
| Besoin d’un tableau de bord admin | audit_event         |
| Besoin d’audit réglementaire      | double audit        |
| Analyse volumétrique              | audit_event (jsonb) |

---

## 6. Visualisation

- `_aud` → interne, technique
- `audit_event` → exploitable par front admin / BI
