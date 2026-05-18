# Add offlinesync module — Tasks

> **Version 2** — intègre tâches crypto Ed25519, idempotence stricte, jobs de récupération.

Checklist d'implémentation. Cocher au fur et à mesure.

---

## 1. Préparation et fondations

- [ ] 1.1 Créer la structure de package `core.offlinesync` selon convention
- [ ] 1.2 Configurer Spring Modulith pour reconnaître le nouveau module
- [ ] 1.3 Ajouter le module à la suite de tests de vérification modulith
- [ ] 1.4 Setup des migrations Flyway dans le bon ordre
- [ ] 1.5 Configurer Hibernate Envers pour les nouvelles tables
- [ ] 1.6 **Setup KMS pour les clés serveur Ed25519** (génération initiale, accès via service)

## 2. Domaine — Entités

- [ ] 2.1 `OfflineGrant` avec `devicePublicKey`, `syncAcceptedUntil`, statut `SUPERSEDED`
- [ ] 2.2 `OfflineCodeBatch`
- [ ] 2.3 `OfflineCode` avec **6 états** (AVAILABLE, RESERVED, CONSUMED_PROMOTED, CONSUMED_REJECTED, VOIDED, EXPIRED)
- [ ] 2.4 `OfflineSyncBatch` avec `batchPayloadHash`
- [ ] 2.5 `OfflineSubmission` avec `totalStakeAmount`, `lineCount`, `promotionAttemptId`, `lastPromotionEventId`
- [ ] 2.6 `OfflineSubmissionLine` avec `potentialPayout >= 0`
- [ ] 2.7 `OfflineSubmissionTicketLink`
- [ ] 2.8 `OfflineSubmissionDecision` avec `dryRun` et `reportJson`
- [ ] 2.9 Enums avec nouvelles valeurs
- [ ] 2.10 Tests unitaires sur les invariants de chaque entité (notamment cohérence multi-status)

## 3. Domaine — Policies

- [ ] 3.1 `OfflineGrantPolicy` (avec gestion `syncAcceptedExtension`)
- [ ] 3.2 `OfflineSubmissionTechnicalPolicy` avec les **15 étapes** ordonnées
- [ ] 3.3 `OfflineSyncPromotionPolicy`
- [ ] 3.4 Tests unitaires exhaustifs (couverture branches > 90%)

## 4. Domaine — Services crypto (REFONTE v2)

- [ ] 4.1 `OfflineCodeGenerator` avec alphabet anti-confusion + tests d'unicité
- [ ] 4.2 `OfflineGrantSigner` (Ed25519, **clé serveur via KMS**)
- [ ] 4.3 `OfflineSubmissionVerifier` (Ed25519, **vérifie avec devicePublicKey du grant**)
- [ ] 4.4 Service de sérialisation canonique JSON avec versioning
- [ ] 4.5 Tests crypto :
  - Signature serveur des grants vérifiable
  - Signature device des submissions vérifiable
  - Détection altération payload
  - Détection signature forgée
  - Test que le backend ne peut PAS forger une submission valide

## 5. Application — Commands

- [ ] 5.1 `RequestOfflineGrantCommand` + handler (avec réception `devicePublicKey`)
- [ ] 5.2 `RenewOfflineGrantCommand` + handler (avec transition `SUPERSEDED`)
- [ ] 5.3 `RevokeOfflineGrantCommand` + handler
- [ ] 5.4 `SyncOfflineSalesCommand` + handler (avec idempotence stricte hash-based)
- [ ] 5.5 `ApproveOfflineSubmissionCommand` + handler (génère nouveau `promotionAttemptId`)
- [ ] 5.6 `RejectOfflineSubmissionCommand` + handler
- [ ] 5.7 `ReplayOfflineSubmissionCommand` + handler (dry-run only, avec `reportJson`)
- [ ] 5.8 Tests d'intégration pour chaque command

## 6. Application — Queries

- [ ] 6.1 `GetOfflineGrantQuery` + handler
- [ ] 6.2 `GetCurrentOfflineGrantQuery` + handler
- [ ] 6.3 `GetOfflineSubmissionQuery` + handler
- [ ] 6.4 `ListOfflineSubmissionsQuery` + handler
- [ ] 6.5 `GetOfflineSyncBatchQuery` + handler
- [ ] 6.6 `GetOfflineDashboardQuery` + handler
- [ ] 6.7 Tests pour les queries

## 7. Events Modulith (avec idempotence stricte)

- [ ] 7.1 Définir `OfflineSubmissionTechValidatedEvent` **self-contained** dans api.event
- [ ] 7.2 Définir `OfflineSubmissionAdminApprovedEvent` self-contained
- [ ] 7.3 Définir `OfflineSubmissionProcessedEvent` avec `promotionAttemptId`
- [ ] 7.4 Publication de l'event dans `SyncOfflineSalesCommandHandler` (avec génération `promotionAttemptId`)
- [ ] 7.5 Publication dans `ApproveOfflineSubmissionCommandHandler`
- [ ] 7.6 Implémenter `OfflineSubmissionProcessedEventListener` **strictement idempotent** (toutes les règles de §5.5 du domaine)
- [ ] 7.7 Configurer event publication registry (Spring Modulith)
- [ ] 7.8 Détecter et alerter sur incidents critiques (deux tickets pour une submission, BUSINESS_REJECTED après PROMOTED)
- [ ] 7.9 Tests : events publiés et persistés
- [ ] 7.10 Tests : rejeu après crash simulé
- [ ] 7.11 Tests : idempotence event dupliqué
- [ ] 7.12 Tests : event obsolète (promotionAttemptId obsolète) ignoré

## 8. Modifications dans core.sales

- [ ] 8.1 **Ajouter colonne `offline_submission_id`** sur table ticket avec UNIQUE constraint `(tenant_id, offline_submission_id)`
- [ ] 8.2 Créer le listener `@ApplicationModuleListener` pour `OfflineSubmissionTechValidatedEvent`
- [ ] 8.3 Créer le listener pour `OfflineSubmissionAdminApprovedEvent`
- [ ] 8.4 Logique de promotion **idempotente** : si UNIQUE constraint catch → publier ProcessedEvent matching ticket existant
- [ ] 8.5 Logique métier : pricing, validation draw, limites, création ticket
- [ ] 8.6 Publication `OfflineSubmissionProcessedEvent` avec `promotionAttemptId` cité
- [ ] 8.7 **Transaction outbox** : création ticket + publication event dans la même transaction
- [ ] 8.8 Tests cross-module (offlinesync → sales → offlinesync)
- [ ] 8.9 Tests : double création ticket impossible (UNIQUE constraint)

## 9. Modifications dans core.limitpolicy

- [ ] 9.1 Définir le modèle `OfflineLimitPolicy` avec `syncAcceptedExtension`
- [ ] 9.2 Implémenter `GetOfflineLimitPolicyQuery` exposée
- [ ] 9.3 Configuration par défaut par plan (BUSINESS, PREMIUM) avec extension 7j
- [ ] 9.4 Persistance des overrides par tenant
- [ ] 9.5 Tests

## 10. Infrastructure — REST

### POS (vendeur)
- [ ] 10.1 `POST /tenant/offline/grants` (réception `devicePublicKey`)
- [ ] 10.2 `GET /tenant/offline/grants/current`
- [ ] 10.3 `POST /tenant/offline/sync` (réception `batchPayloadHash`)
- [ ] 10.4 `GET /tenant/offline/submissions/my`
- [ ] 10.5 `GET /tenant/offline/submissions/{id}/status` (polling pour outcome business)

### Admin
- [ ] 10.6 à 10.15 (identique v1)
- [ ] 10.16 OpenAPI / Swagger
- [ ] 10.17 Codes d'erreur étendus documentés
- [ ] 10.18 Sécurité par endpoint

## 11. Jobs scheduled (NOUVEAU v2)

- [ ] 11.1 `StuckSubmissionRecoveryJob` : détecter TECH_VALIDATED > 10 min sans résolution, alerter
- [ ] 11.2 `OrphanedCodeReservationJob` : détecter codes RESERVED > 10 min, les passer en CONSUMED_REJECTED
- [ ] 11.3 `GrantExpirationJob` : passer grants en EXPIRED quand validUntil dépassé
- [ ] 11.4 Tests d'intégration des jobs

## 12. Observabilité

- [ ] 12.1 Métriques Micrometer (avec nouvelles : idempotency_conflicts, orphaned_codes, double_ticket_incidents, grants_superseded)
- [ ] 12.2 Histogrammes (validation duration, promotion lag)
- [ ] 12.3 Métriques business
- [ ] 12.4 Logs structurés avec `promotionAttemptId` propagé
- [ ] 12.5 Configuration des alertes Prometheus
- [ ] 12.6 Dashboard Grafana initial avec section "Critical incidents"

## 13. Frontend Flutter — Foundation

- [ ] 13.1 Module `features/offline/` avec structure
- [ ] 13.2 Setup Drift : schema, build_runner
- [ ] 13.3 Setup `flutter_secure_storage` pour métadonnées
- [ ] 13.4 **Setup `cryptography` + `cryptography_flutter`** pour Ed25519 avec Android Keystore backend
- [ ] 13.5 Setup HTTP client pour l'API
- [ ] 13.6 Setup connectivity_plus avec ping API custom
- [ ] 13.7 Setup workmanager

## 14. Frontend Flutter — Domain layer (crypto Ed25519)

- [ ] 14.1 Modèles métier purs
- [ ] 14.2 **`CryptoService`** :
  - Génération keypair Ed25519 dans Android Keystore
  - Vérification signature grant avec clé publique serveur embarquée
  - Calcul `payloadHash` (canonical JSON + SHA-256)
  - Signature submission avec clé privée Keystore
- [ ] 14.3 `GrantService` : récupération, validation locale, renouvellement
- [ ] 14.4 **`OfflineSaleService`** : flow atomique Drift, signature INCLUSE dans la transaction
- [ ] 14.5 `SyncService` avec :
  - `clientBatchId` persisté avant HTTP
  - `batchPayloadHash` calculé
  - Retry avec même clientBatchId
  - Gestion des conflits d'idempotence
- [ ] 14.6 Tests unitaires des services

## 15. Frontend Flutter — Data layer

- [ ] 15.1 Schéma Drift avec **9 statuts locaux**
- [ ] 15.2 DAOs
- [ ] 15.3 Table `local_pending_syncs` pour idempotence `clientBatchId`
- [ ] 15.4 Repositories
- [ ] 15.5 `OfflineApi` (client HTTP)
- [ ] 15.6 DTOs
- [ ] 15.7 Tests d'intégration

## 16. Frontend Flutter — Presentation

- [ ] 16.1 Indicateur "mode hors ligne" persistant
- [ ] 16.2 Badge "X submissions à syncer" cliquable
- [ ] 16.3 Écran de détail sync
- [ ] 16.4 Toggle "Mode offline forcé"
- [ ] 16.5 Dialog de recovery au démarrage (COMMITTED_NOT_PRINTED)
- [ ] 16.6 Réimpression DUPLICATA
- [ ] 16.7 Historique 7 jours
- [ ] 16.8 Adapter écran de vente : routage online/offline
- [ ] 16.9 Messages d'erreur user-friendly par code
- [ ] 16.10 **Blocage logout/clear data si pending sync** (avec override admin)
- [ ] 16.11 **Notification système si pending > 24h**

## 17. Frontend Flutter — Background

- [ ] 17.1 Sync périodique foreground (2 min)
- [ ] 17.2 Background workmanager (15 min)
- [ ] 17.3 Listener connectivity → sync immédiat
- [ ] 17.4 Renouvellement auto du grant
- [ ] 17.5 Job nettoyage local (rétention 7j)
- [ ] 17.6 **Recovery zombie SYNCING (> 15 min) → PRINTED_PENDING_SYNC**
- [ ] 17.7 Détection submissions stuck → alerte locale

## 18. Tests bout en bout (étendus v2)

- [ ] 18.1 Scénario nominal : grant → vente offline → sync → ticket
- [ ] 18.2 Retry de sync après timeout (idempotence hash-based)
- [ ] 18.3 Grant expiré pendant offline
- [ ] 18.4 Crash device entre persist et impression
- [ ] 18.5 Sales down pendant sync (replay events au retour)
- [ ] 18.6 Admin approve un NEEDS_REVIEW
- [ ] 18.7 Admin rejette un NEEDS_REVIEW
- [ ] 18.8 Double-utilisation code (rejeté CODE_ALREADY_RESERVED)
- [ ] 18.9 Signature invalide (rejeté)
- [ ] 18.10 Grant révoqué pendant offline
- [ ] 18.11 Mode offline forcé
- [ ] 18.12 Réimpression DUPLICATA
- [ ] 18.13 **NOUVEAU** : Vente faite pendant validité, sync après expiration validUntil mais avant syncAcceptedUntil → acceptée
- [ ] 18.14 **NOUVEAU** : Vente faite pendant validité, sync après syncAcceptedUntil → rejetée
- [ ] 18.15 **NOUVEAU** : Conflit d'idempotence batch (même clientBatchId, payload différent) → BATCH_IDEMPOTENCY_CONFLICT
- [ ] 18.16 **NOUVEAU** : Conflit d'idempotence submission (même clientSubmissionId, payload différent) → SUBMISSION_IDEMPOTENCY_CONFLICT
- [ ] 18.17 **NOUVEAU** : Duplicate submission (même clientSubmissionId, même payload) → résultat API DUPLICATE
- [ ] 18.18 **NOUVEAU** : Grant SUPERSEDED accepte encore les ventes faites pendant sa validité
- [ ] 18.19 **NOUVEAU** : Batch multi-grants → BATCH_CONTEXT_MISMATCH
- [ ] 18.20 **NOUVEAU** : Backend compromis ne peut pas forger de submission (test crypto)
- [ ] 18.21 **NOUVEAU** : Event ProcessedEvent obsolète (promotionAttemptId mismatch) → ignoré
- [ ] 18.22 **NOUVEAU** : UNIQUE constraint sales empêche double ticket
- [ ] 18.23 **NOUVEAU** : OrphanedCodeReservationJob récupère code RESERVED orphelin

## 19. Documentation

- [ ] 19.1 README du module
- [ ] 19.2 Diagrammes de séquence (5 flows principaux + flow crypto)
- [ ] 19.3 Documentation des codes d'erreur (étendus)
- [ ] 19.4 Guide admin
- [ ] 19.5 Runbook ops avec alertes critiques (notamment `DoubleTicketIncident`)
- [ ] 19.6 Mise à jour docs Flutter (README, ADRs sur crypto Ed25519)
- [ ] 19.7 **Documentation de la procédure de rotation des clés KMS (manuelle v1)**

## 20. Release et déploiement

- [ ] 20.1 Feature flag `offlinesync.enabled` (rollout progressif par tenant)
- [ ] 20.2 Migration Flyway testée sur snapshot prod
- [ ] 20.3 Plan de rollback documenté
- [ ] 20.4 **Audit crypto externe avant mise en prod**
- [ ] 20.5 Communication client / commercial
- [ ] 20.6 Formation support sur le workflow admin
- [ ] 20.7 Beta sur un tenant pilote (1 mois)
- [ ] 20.8 Monitoring renforcé pendant la beta (focus métriques incidents)
- [ ] 20.9 Rollout général

## 21. Post-release

- [ ] 21.1 Monitoring métriques business 30 jours
- [ ] 21.2 Feedback vendeurs et admin
- [ ] 21.3 Backlog v2 priorisé (refunds, iOS, rotation clés, métriques risque)
- [ ] 21.4 Spec OpenSpec pour v2 lancée
