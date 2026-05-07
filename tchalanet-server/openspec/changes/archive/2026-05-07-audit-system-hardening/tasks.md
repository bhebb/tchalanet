# Tasks — Audit System Hardening

## 1. Audit applicatif — unifier le chemin canonique

- [x] Rechercher tous les usages de `RecordAuditEventCommand`.
- [x] Migrer les usages vers `LogAuditEventCommand`.
- [x] Supprimer `RecordAuditEventCommandHandler`.
- [x] Supprimer `RecordAuditEventCommand` si plus utilisé.
- [x] Garder `AuditLoggingCommandHandler` comme seul handler d'écriture applicative.
- [x] Vérifier que `AuditEventFactory` est utilisé pour construire le modèle domaine.

## 2. Corriger `AuditLogAspect`

- [x] Retirer tout `return` dans le `finally`.
- [x] Remplacer `if (cmd == null) return null;` par un simple `if (cmd != null) { ... }`.
- [x] Vérifier que success appelle `AfterCommit.run(() -> safeSend(cmd))`.
- [x] Vérifier que error appelle `safeSend(cmd)` immédiatement.
- [x] Vérifier que `safeSend` utilise `CommandBus`.
- [x] Vérifier qu'aucun handler n'est appelé directement.
- [x] Catch/log toutes les erreurs d'audit.
- [x] Ajouter support annotation méthode.
- [x] Ajouter support annotation classe si nécessaire.
- [x] Tester `#result`.
- [x] Tester `#error`.
- [x] Tester paramètres de méthode SpEL.

## 3. Normaliser `entityId`

- [x] Confirmer que `LogAuditEventCommand.entityId` est `String`.
- [x] Supprimer tout `UUID.fromString(command.entityId())` dans le chemin applicatif.
- [x] Vérifier que `AuditEvent.domain.entityId` peut rester `String`.
- [x] Vérifier que `AuditEventJpaEntity.entityId` est `String`.
- [x] Ajouter test avec entityId non-UUID.
- [x] Ajouter exemples :
  - ticket code ;
  - public code ;
  - job key ;
  - force marker.

## 4. Normaliser `details`

- [x] Convertir `detailsExpression` Map en `Map<String,Object>`.
- [x] Convertir objet simple via `JsonUtils`/`ObjectMapper`.
- [x] Éviter `toString()` sauf fallback.
- [x] Ajouter `outcome=SUCCESS` si retenu.
- [x] Ajouter `outcome=FAIL` pour error.
- [x] Ajouter `error` et `errorMessage` pour error.
- [x] Ajouter `requestId` si disponible.
- [x] Vérifier que `details` est JSONB-safe.

## 5. Corriger persistence audit

- [x] Ajouter `e.setOccurredAt(event.occurredAt())` dans `toEntity`.
- [x] Vérifier `actorId` nullable ou non selon modèle.
- [x] Vérifier `createdBy`/`actorId` SYSTEM sans user.
- [x] Décider si `audit_event` étend `BaseEntity` ou `BaseTenantEntity`.
- [x] Recommandé : `BaseEntity` + `tenant_id nullable`.
- [x] Ajouter ou adapter Flyway `audit_event`.
- [x] Ajouter index :
  - [x] `(tenant_id, occurred_at desc)`
  - [x] `(entity_type, entity_id)`
  - [x] `(action, occurred_at desc)`
  - [x] `(actor_id, occurred_at desc)`
- [x] Vérifier RLS/policies tenant/platform.
- [x] Vérifier consultation audit tenant vs platform.

## 6. Supprimer `AuditedForceCommandAspect`

- [x] Supprimer `common.command.audit.AuditedForceCommand`.
- [x] Supprimer `common.command.audit.infra.AuditedForceCommandAspect`.
- [x] Vérifier qu'aucun package `common` ne dépend de `core.audit`.
- [x] Rechercher toutes les commandes `force`.
- [x] Pour chaque commande `force=true`, décider si audit obligatoire.
- [x] Ajouter audit explicite dans le handler propriétaire.
- [x] Rendre `reason` obligatoire pour force manuel.
- [ ] Ajouter tests par handler concerné.
  - Note: les opérations Ops HTTP force sont couvertes par `@AuditLog` controller-level selon le plan d'implémentation retenu; les tests handler restent à ajouter pour les flows non-HTTP.

## 7. Définir liste canonique des actions auditables

- [x] Mettre à jour `AuditAction`.
- [x] Mettre à jour `AuditEntityType` si nécessaire.
- [x] Couvrir Sales :
  - [x] `SELL_TICKET`
  - [x] `CANCEL_TICKET`
  - [x] `OVERRIDE_RESULT`
  - [x] `PRINT_TICKET`
- [x] Couvrir Payout :
  - [x] `PAYOUT_REQUEST`
  - [x] `PAYOUT_APPROVE`
  - [x] `PAYOUT_REJECT`
  - [x] `PAYOUT_EXECUTE`
- [x] Couvrir Draw :
  - [x] `DRAW_GENERATE`
  - [x] `DRAW_OPEN`
  - [x] `DRAW_CLOSE`
- [x] Couvrir DrawResult :
  - [x] `DRAW_RESULT_FETCH`
  - [x] `DRAW_RESULT_APPLY`
  - [x] `DRAW_RESULT_OVERRIDE`
- [x] Couvrir configuration :
  - [x] `OUTLET_CREATE`
  - [x] `OUTLET_UPDATE`
  - [x] `OUTLET_DELETE`
  - [x] `TERMINAL_CREATE`
  - [x] `TERMINAL_UPDATE`
  - [x] `TERMINAL_DELETE`
  - [x] `USER_CREATE`
  - [x] `USER_UPDATE`
  - [x] `USER_ROLE_CHANGE`
  - [x] `LIMIT_UPDATE`
  - [x] `COMMISSION_UPDATE`
  - [x] `TENANT_THEME_UPDATE`
  - [x] `FEATURE_FLAG_UPDATE`
- [x] Couvrir sécurité / plateforme :
  - [x] `SUPER_ADMIN_OVERRIDE`
  - [x] `FORCE_OPERATION`
  - [x] `BATCH_JOB_START`
  - [x] `CACHE_CLEAR`
  - [x] `TENANT_CREATE`
  - [x] `TENANT_UPDATE`
  - [x] `TENANT_DISABLE`

## 8. Créer matrice de couverture audit

- [x] Créer une table interne action -> endpoint/use-case.
- [x] Colonnes recommandées :
  - action ;
  - entity ;
  - endpoint ou command ;
  - trigger (`@AuditLog` ou handler) ;
  - idExpression ;
  - details ;
  - success/error ;
  - tests.
- [x] Vérifier chaque controller sensible.
- [ ] Vérifier chaque command handler sensible non-HTTP.
- [ ] Éviter double log controller + handler pour la même action.
- [x] Documenter les cas où handler-level audit est obligatoire.

## 9. Purge audit

- [x] Injecter `Clock` dans `PurgeOldAuditEventsCommandHandler`.
- [x] Remplacer `Instant.now()` par `Instant.now(clock)`.
- [x] Utiliser `tch.audit.retention-days`.
- [x] Supprimer par `occurred_at < threshold`.
- [x] Logger `deleted` et `retentionDays`.
- [x] Exposer purge via batch/Ops contrôlé si nécessaire.
- [x] Auditer l'action de purge si déclenchée manuellement.
- [x] Ajouter tests :
  - [ ] vieux événements supprimés ;
  - [ ] récents conservés ;
  - [x] seuil déterministe ;
  - [x] `retentionDays` respecté.

## 10. Envers — revinfo

- [x] Vérifier table `revinfo`.
- [x] Vérifier séquence `tch_revinfo_seq`.
- [x] Ajouter `tenant_id`.
- [x] Ajouter `user_id`.
- [x] Ajouter `request_id`.
- [x] Ajouter `actor_type`.
- [x] Optionnel : ajouter `api_scope`.
- [x] Optionnel : ajouter `tenant_overridden`.
- [x] Adapter `TchRevisionEntity`.
- [x] Adapter `TchRevisionListener`.
- [x] Utiliser `TchContextResolver`.
- [x] Catch/log debug sans throw.
- [x] Ajouter test revision avec contexte.
- [x] Ajouter test revision sans contexte.

## 11. Envers — entités auditées

- [x] Décider si `@Audited` reste sur `AuditableEntity`.
- [x] Recommandé : éviter audit global par défaut si trop large.
- [x] Lister entités critiques à auditer explicitement.
- [x] Ajouter tables `_AUD` via Flyway.
- [x] Vérifier colonnes `_AUD` alignées.
- [x] Vérifier `ddl-auto=validate`.
- [x] Ajouter règle PR : toute migration table auditée met aussi à jour `_AUD`.

## 12. TenantEntityListener

- [x] Supprimer usage `RequestContextHolder`.
- [x] Utiliser `TchContext` ou `TchContextResolver`.
- [x] Garder respect tenantId déjà set pour batch/import.
- [x] Fail fast si tenant absent au persist et tenantId non set.
- [x] Vérifier tenant mismatch au update.
- [x] Ajouter tests persist/update.
- [x] Vérifier que batch pose correctement `TchContext`.

## 13. Controllers audit query

- [x] Remplacer placeholder `AuditEventRestController`.
- [x] Déplacer vers `/platform/audit` si SUPER_ADMIN platform.
- [x] Retourner `ApiResponse<TchPage<AuditEventResponse>>`.
- [x] Utiliser `@TchPaging`.
- [x] Ajouter filtres :
  - tenant ;
  - entityType ;
  - entityId ;
  - action ;
  - actor ;
  - date range.
- [x] Ne pas exposer JPA/domain brut.
- [x] Appliquer sécurité `SUPER_ADMIN` / tenant admin selon scope.

## 14. Tests globaux

- [x] Aspect success after commit.
- [x] Aspect rollback no success log.
- [x] Aspect exception immediate fail log.
- [x] Audit writer exception does not break flow.
- [x] Details JSONB safe.
- [x] Non-UUID entityId accepted.
- [ ] Force handler audit.
- [x] Purge retention.
- [x] Envers revision context.
- [x] Tenant listener canonical context.
- [ ] Flyway + ddl-auto validate.

## 15. Documentation

- [x] Créer/mettre à jour `DOMAIN_AUDIT.md`.
- [x] Documenter audit applicatif vs Envers.
- [x] Documenter action coverage matrix.
- [x] Documenter purge.
- [x] Documenter PR checklist audit.
