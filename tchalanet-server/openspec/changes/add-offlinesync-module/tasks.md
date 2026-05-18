# Add offlinesync module — Tasks v2.1

## 1. Fondations

- [ ] Créer `core.offlinesync` selon structure core.
- [ ] Ajouter typed IDs : `OfflineGrantId`, `OfflineCodeBatchId`, `OfflineCodeId`, `OfflineSyncBatchId`, `OfflineSubmissionId`, `PromotionAttemptId`.
- [ ] Ajouter enums de statuts.
- [ ] Ajouter feature flag `offlinesync.enabled`.

## 2. Migrations

- [ ] Créer tables `offline_grant`, `offline_code_batch`, `offline_code`, `offline_sync_batch`, `offline_submission`, `offline_submission_line`, `offline_submission_ticket_link`, `offline_submission_decision`.
- [ ] Ajouter indexes et constraints idempotence.
- [ ] Ajouter RLS.
- [ ] Ajouter `ticket.offline_submission_id`.
- [ ] Ajouter unique `(tenant_id, offline_submission_id)` sur `ticket`.

## 3. Domaine

- [ ] Implémenter `OfflineGrant`.
- [ ] Implémenter `OfflineCodeBatch`.
- [ ] Implémenter `OfflineCode` sans transition `RESERVED -> AVAILABLE`.
- [ ] Implémenter `OfflineSyncBatch`.
- [ ] Implémenter `OfflineSubmission`.
- [ ] Implémenter `OfflineGrantPolicy`.
- [ ] Implémenter `OfflineSubmissionTechnicalPolicy` avec les 15 checks ordonnés.
- [ ] Implémenter `OfflineSyncPromotionPolicy`.

## 4. Crypto

- [ ] `OfflineGrantSigner` côté serveur.
- [ ] `OfflineSubmissionVerifier` Ed25519.
- [ ] Tests signature valide/invalide/key mismatch.

## 5. Commands offlinesync

- [ ] `RequestOfflineGrantCommand` + handler.
- [ ] `RenewOfflineGrantCommand` + handler.
- [ ] `RevokeOfflineGrantCommand` + handler.
- [ ] `SyncOfflineSalesCommand` + handler.
- [ ] `ApproveOfflineSubmissionCommand` + handler.
- [ ] `RejectOfflineSubmissionCommand` + handler.
- [ ] `ReplayOfflineSubmissionCommand` + handler dry-run only.
- [ ] `RecoverStuckOfflineSubmissionCommand` + handler.
- [ ] `ExpireOfflineGrantCommand` + handler.

## 6. Queries offlinesync

- [ ] `GetOfflineGrantQuery`.
- [ ] `GetCurrentOfflineGrantQuery`.
- [ ] `GetOfflineSubmissionQuery`.
- [ ] `ListOfflineSubmissionsQuery`.
- [ ] `GetOfflineSyncBatchQuery`.
- [ ] `GetOfflineDashboardQuery`.

## 7. Events

- [ ] `OfflineSubmissionTechValidatedEvent` dans `api.event`.
- [ ] `OfflineSubmissionAdminApprovedEvent` dans `api.event`.
- [ ] `OfflineSubmissionProcessedEvent` dans `api.event`.
- [ ] Events self-contained avec payload complet.
- [ ] Publication after-commit.
- [ ] Listener retour offlinesync idempotent.
- [ ] Ignorer event retour avec `promotionAttemptId` obsolète.

## 8. Sales

- [ ] Listener `OfflineSubmissionTechValidatedEvent`.
- [ ] Listener `OfflineSubmissionAdminApprovedEvent`.
- [ ] Command `CreateTicketFromOfflineSubmissionCommand`.
- [ ] Mapping payload offline -> modèle sales.
- [ ] Protection DB unique.
- [ ] Publication `OfflineSubmissionProcessedEvent`.
- [ ] Tests double listener / retry / duplicate.

## 9. LimitPolicy

- [ ] Modèle `OfflineLimitPolicy`.
- [ ] Query `GetOfflineLimitPolicyQuery`.
- [ ] Config `syncAcceptedExtension`.

## 10. REST

- [ ] `POST /tenant/offline/grants`.
- [ ] `GET /tenant/offline/grants/current`.
- [ ] `POST /tenant/offline/sync`.
- [ ] `GET /tenant/offline/submissions/my`.
- [ ] `GET /tenant/offline/submissions/{id}/status`.
- [ ] `GET /admin/offline/submissions`.
- [ ] `POST /admin/offline/submissions/{id}/approve`.
- [ ] `POST /admin/offline/submissions/{id}/reject`.
- [ ] `POST /admin/offline/submissions/{id}/replay-dry-run`.
- [ ] Ops endpoints audités.

## 11. Jobs

- [ ] `StuckSubmissionRecoveryJob`.
- [ ] `OrphanedCodeReservationJob`.
- [ ] `GrantExpirationJob`.
- [ ] `SyncAcceptedWindowCloseJob`.

## 12. Flutter

- [ ] Module `features/offline`.
- [ ] Drift schema local.
- [ ] Grant cache + code cache.
- [ ] Ed25519 signing.
- [ ] Sale offline atomique.
- [ ] Sync outbox.
- [ ] UI mode offline.
- [ ] Réimpression duplicata.

## 13. Tests E2E

- [ ] grant -> vente offline -> sync -> ticket.
- [ ] duplicate same hash.
- [ ] duplicate payload mismatch.
- [ ] sync après `validUntil` mais avant `syncAcceptedUntil`.
- [ ] sync après `syncAcceptedUntil`.
- [ ] grant superseded mais old submission encore acceptée.
- [ ] grant revoked.
- [ ] code jamais disponible après submission.
- [ ] double ticket impossible.
- [ ] event obsolète ignoré.
