# Design — Audit System Hardening

## 1. Séparation conceptuelle

Tchalanet utilise deux audits distincts.

| Audit            | But                                                         | Déclencheur                           | Stockage            |
| ---------------- | ----------------------------------------------------------- | ------------------------------------- | ------------------- |
| Audit applicatif | Tracer une action métier sensible : qui, quoi, où, pourquoi | Controller annoté ou handler use-case | `audit_event`       |
| Envers           | Historiser les modifications de lignes DB                   | Hibernate/JPA                         | `*_AUD` + `revinfo` |

Règle importante :

- Une action métier peut produire un audit applicatif.
- La même action peut aussi modifier une entité historisée par Envers.
- Ce ne sont pas des doublons : ils répondent à deux questions différentes.

Exemple :

- Audit applicatif : “SUPER_ADMIN a forcé l'application d'un résultat, reason=X”.
- Envers : “la colonne `status` de `draw_result` est passée de PROVISIONAL à OVERRIDDEN”.

## 2. Audit applicatif — flow canonique

Le flow retenu est :

```text
@AuditLog / handler explicit
  ↓
AuditLogAspect ou handler propriétaire
  ↓
CommandBus.send(LogAuditEventCommand)
  ↓
AuditLoggingCommandHandler (@Transactional(REQUIRES_NEW))
  ↓
AuditEventFactory
  ↓
AuditEventWriterPort
  ↓
audit_event
```

### Success

Pour un succès métier :

```java
AfterCommit.run(() -> commandBus.send(cmd));
```

Raison :

- si la transaction métier rollback, on ne veut pas écrire un audit de succès.

### Error

Pour une erreur métier :

```java
commandBus.send(cmd);
```

Raison :

- il n'y aura pas de commit ;
- on veut quand même tracer l'échec ;
- le handler est en `REQUIRES_NEW`.

### No crash

Toutes les erreurs dans l'audit doivent être catch/log.

L'audit ne doit jamais :

- masquer l'erreur métier originale ;
- remplacer le résultat normal ;
- provoquer un rollback métier.

## 3. Nettoyage du double chemin

Le chemin `RecordAuditEventCommand` doit être supprimé ou migré.

Raisons :

- il concurrence `LogAuditEventCommand` ;
- il reconstruit manuellement `AuditEvent` ;
- il parse `entityId` en UUID ;
- il utilise `Instant.now()` ;
- il contourne `AuditEventFactory`.

Décision :

- `LogAuditEventCommand` devient le modèle canonique.
- `AuditLoggingCommandHandler` devient le seul handler d'écriture applicative.
- `AuditEventFactory` est le seul endroit qui construit le modèle domaine `AuditEvent`.

## 4. AuditLogAspect

### Responsabilités

- Lire `@AuditLog`.
- Évaluer `idExpression`.
- Évaluer `detailsExpression`.
- Injecter `#result` et `#error` dans SpEL.
- Construire `LogAuditEventCommand`.
- Router success/error selon la règle transactionnelle.
- Ne jamais casser le flux principal.

### Correction critique

Le `finally` ne doit jamais contenir :

```java
return null;
```

Un `return` dans `finally` peut écraser le retour normal du controller.

Pattern attendu :

```java
finally {
  try {
    var cmd = buildCommandOrNull(pjp, result, error);
    if (cmd != null) {
      if (error == null) {
        AfterCommit.run(() -> safeSend(cmd));
      } else {
        safeSend(cmd);
      }
    }
  } catch (Exception e) {
    log.warn("Audit logging failed", e);
  }
}
```

## 5. Entity ID policy

`entityId` est toujours un `String` stable.

Exemples valides :

- UUID string : `550e8400-e29b-41d4-a716-446655440000`
- ticket code : `TCK-260113-214501-9K3W2H-7`
- public code : `9Q2H7M4K1PZX`
- job key : `RESULTS_EXTERNAL_REFRESH`
- force bypass marker : `FORCE_BYPASS`

Interdit :

- forcer `UUID.fromString(entityId)` dans le flow applicatif ;
- stocker uniquement UUID si l'entité auditée peut être identifiée autrement.

## 6. Details policy

`details` doit être JSONB-safe.

Règles :

- Si SpEL retourne un `Map`, convertir vers `Map<String,Object>`.
- Si SpEL retourne un objet simple, convertir via `JsonUtils` ou `ObjectMapper`.
- Éviter `toString()` sauf fallback minimal.
- En cas d'erreur, enrichir avec :
  - `outcome = FAIL` ou équivalent ;
  - `error`;
  - `errorMessage`.
- En cas de succès, enrichir avec :
  - `outcome = SUCCESS` si utile.
- Inclure `requestId` lorsque disponible.

## 7. Audit event persistence

### occurredAt

`AuditEventRepositoryAdapter.toEntity()` doit renseigner :

```java
e.setOccurredAt(event.occurredAt());
```

### Tenant/platform model

`audit_event` doit représenter trois cas :

1. audit tenant-scoped ;
2. audit platform/global ;
3. audit système/batch.

Décision recommandée :

- `audit_event` utilise `BaseEntity`, pas `BaseTenantEntity`;
- `tenant_id` est une colonne nullable manuelle ;
- RLS/policies permettent :
  - tenant admin voit son tenant ;
  - super admin/platform voit selon scope ;
  - système écrit avec tenant null si global.

Si le projet conserve `BaseTenantEntity`, il faut documenter explicitement que les audits platform sans tenant ne sont pas supportés ou nécessitent un tenant système.

### Index recommandés

- `(tenant_id, occurred_at desc)`
- `(entity_type, entity_id)`
- `(action, occurred_at desc)`
- `(actor_id, occurred_at desc)`

## 8. Force operations

`AuditedForceCommandAspect` est rejeté.

Raisons :

- `common` ne doit pas dépendre de `core.audit`;
- interception globale de `CommandBus.send(..)` trop large ;
- ne connaît pas le succès ou l'échec ;
- risque de récursion quand il envoie lui-même un audit ;
- réflexion sur champs `force`/`reason` fragile.

Pattern retenu :

- le handler qui possède l'opération `force=true` audite explicitement ;
- `reason` doit être obligatoire pour les forces manuelles ;
- success audit après commit ;
- error audit immédiat si on veut tracer l'échec.

Exemple :

```java
if (cmd.force()) {
  var details = Map.of(
      "force", true,
      "reason", cmd.reason(),
      "command", cmd.getClass().getSimpleName()
  );

  AfterCommit.run(() ->
      commandBus.send(new LogAuditEventCommand(
          AuditEntityType.DRAW_RESULT,
          drawResultId.value().toString(),
          AuditAction.DRAW_RESULT_OVERRIDE,
          details
      ))
  );
}
```

## 9. Liste canonique des actions

La liste canonique doit vivre dans `AuditAction`.

Chaque action sensible doit être mappée à :

- endpoint ou handler ;
- entity type ;
- idExpression ;
- details ;
- déclenchement controller ou handler.

Le projet doit maintenir une couverture lisible, par exemple dans une doc `DOMAIN_AUDIT.md` ou dans la spec.

## 10. Purge

La purge doit utiliser :

- un `Clock` injecté ;
- `tch.audit.retention-days`;
- `occurred_at` comme colonne de référence ;
- un log du nombre d'éléments supprimés.

Pseudo-code :

```java
var threshold = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
int deleted = writer.deleteBefore(threshold);
log.info("Purged {} audit events older than {} days", deleted, retentionDays);
```

La purge doit être disponible via :

- job batch contrôlé ; ou
- endpoint Ops/SUPER_ADMIN audité.

## 11. Envers — revinfo

`revinfo` doit être enrichi.

Champs minimaux :

- `rev`
- `rev_timestamp`
- `tenant_id`
- `user_id`
- `request_id`
- `actor_type`

Champs optionnels utiles :

- `api_scope`
- `tenant_overridden`

`TchRevisionListener` doit :

- lire `TchContextResolver`;
- remplir les champs disponibles ;
- catch/log debug si impossible ;
- ne jamais throw.

## 12. Envers — @Audited policy

Ne pas auditer tout le système par défaut.

Règles :

- seules les entités critiques sont `@Audited`;
- chaque table auditée a une table `_AUD`;
- chaque migration Flyway qui modifie la table principale modifie aussi `_AUD`;
- CI valide via `flyway migrate` + `ddl-auto=validate`.

Entités candidates à décider :

- tenant config ;
- tenant users/roles ;
- outlets ;
- terminals ;
- limit policies ;
- commissions/pricing config ;
- payout ;
- ticket si nécessaire ;
- draw/draw_result si nécessaire.

## 13. TenantEntityListener

Le listener tenant doit utiliser le contexte canonique :

- `TchContext` ou `TchContextResolver`.

Il ne doit pas utiliser :

- `RequestContextHolder`.

Règles :

- `@PrePersist` :
  - si tenantId déjà set, respecter la valeur (batch/import) ;
  - sinon injecter depuis contexte ;
  - si absent, fail fast.
- `@PreUpdate` :
  - si contexte tenant disponible, vérifier mismatch ;
  - si mismatch, throw.

## 14. Tests

Tests audit applicatif :

- success écrit après commit ;
- rollback n'écrit pas success ;
- exception écrit FAIL immédiat ;
- erreur audit ne casse pas flux ;
- details JSONB-safe ;
- entityId non-UUID accepté ;
- force=true audité dans handler.

Tests purge :

- seuil calculé avec `Clock`;
- vieux événements supprimés ;
- récents conservés.

Tests Envers :

- revision contient tenant/user/request si contexte disponible ;
- revision n'échoue pas sans contexte ;
- `_AUD` aligné avec Flyway.

Tests contexte :

- tenant injecté au persist ;
- tenant mismatch au update ;
- pas de `RequestContextHolder`.
