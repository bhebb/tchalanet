# Add offlinesync module — Tasks v2.1

> Live status — coche par item. Cf. [`tchalanet-core/.../core/offlinesync/ROADMAP.md`](../../../tchalanet-core/src/main/java/com/tchalanet/server/core/offlinesync/ROADMAP.md) pour le suivi détaillé et les risques résiduels.

## 1. Fondations

- [x] Créer `core.offlinesync` selon structure core.
- [x] Ajouter typed IDs : `OfflineGrantId`, `OfflineCodeBatchId`, `OfflineCodeId`, `OfflineSyncBatchId`, `OfflineSubmissionId`, `PromotionAttemptId`.
- [x] Ajouter enums de statuts (5 enums dans sous-packages `api/model/<concept>/`).
- [x] Ajouter feature flag `tch.offlinesync.enabled`.

## 2. Migrations

- [x] Créer tables `offline_grant`, `offline_code_batch`, `offline_code`, `offline_sync_batch`, `offline_submission`, `offline_submission_line`, `offline_submission_ticket_link`, `offline_submission_decision` (+ `offline_event_outbox` outbox + `tenant_offline_policy` per-tenant override).
- [x] Ajouter indexes et constraints idempotence.
- [x] Ajouter RLS strict + `allow_platform_cross_tenant_select` pour les jobs ops.
- [x] Ajouter `ticket.offline_submission_id`.
- [x] Ajouter unique partiel `(tenant_id, offline_submission_id) WHERE offline_submission_id IS NOT NULL` sur `ticket`.
- [x] Ajouter colonne `draw_id NOT NULL REFERENCES draw(id)` sur `offline_submission` (Phase #10/11 — persister le draw pinné device pour recovery).

## 3. Domaine

- [x] Implémenter `OfflineGrant` (composé : `GrantIdentity + GrantDevice + GrantValidityWindow + GrantQuota + GrantLifecycle`).
- [x] Implémenter `OfflineCodeBatch`.
- [x] Implémenter `OfflineCode` sans transition `RESERVED -> AVAILABLE`.
- [x] Implémenter `OfflineSyncBatch`.
- [x] Implémenter `OfflineSubmission` (avec `drawId` dans `SubmissionPayload`).
- [x] Implémenter `OfflineGrantPolicy`.
- [x] Implémenter `OfflineSubmissionTechnicalPolicy` avec les 15 checks ordonnés.
- [x] Implémenter `OfflineSyncPromotionPolicy`.
- [x] Implémenter `OfflineCodeTransitionPolicy`.

## 4. Crypto

- [x] `OfflineGrantPayloadSigner` côté serveur (format canonique versionné `v1`).
- [x] `OfflineCryptoPort` + `Ed25519OfflineCryptoAdapter` (JDK 17+ natif).
- [x] `OfflineSubmissionPayloadHasher` (canonique versionné `v1`, recompute serveur — check #14).
- [x] Fail-fast en profile `prod`/`staging` si pas de keypair configurée.
- [x] Tests signature valide/invalide/key mismatch/malformed.

## 5. Commands offlinesync

- [x] `RequestOfflineGrantCommand` + handler (avec `upcomingDraws` embarqués).
- [x] `RenewOfflineGrantCommand` + handler (revoke + dispatch `RequestOfflineGrantCommand`).
- [x] `RevokeOfflineGrantCommand` + handler.
- [x] `SyncOfflineSalesCommand` + handler (avec `drawId` + `trustedOperationalContext`, lock pessimiste grant + code, lignes persistées, outbox).
- [x] `ApproveOfflineSubmissionCommand` + handler (avec `decidedBy`, persist decision).
- [x] `RejectOfflineSubmissionCommand` + handler (avec `decidedBy`).
- [ ] `ReplayOfflineSubmissionCommand` + handler dry-run only (squelette compile, ré-évaluation policy à implémenter — cf. ROADMAP R7).
- [x] `RecoverStuckOfflineSubmissionCommand` + handler (rebuild draft depuis lignes persistées + outbox event).
- [x] `ExpireOfflineGrantCommand` + handler.
- [x] `ReleaseOrphanedReservedCodeCommand` + handler (ajout post-spec pour Phase H).

## 6. Queries offlinesync

- [x] `GetOfflineGrantQuery` (via `getRequired`).
- [x] `GetCurrentOfflineGrantQuery`.
- [x] `GetOfflineSubmissionQuery` (via `getRequired`).
- [x] `ListOfflineSubmissionsQuery`.
- [x] `GetOfflineSyncBatchQuery`.
- [ ] `GetOfflineDashboardQuery` (partiel — seul `pendingReview` count câblé ; cf. ROADMAP R8).

## 7. Events

- [x] `OfflineSubmissionTechValidatedEvent` dans `api.event` (self-contained avec `OfflineSubmissionTicketDraft` + `drawId`).
- [x] `OfflineSubmissionAdminApprovedEvent` dans `api.event` (DTO créé).
- [x] `OfflineSubmissionProcessedEvent` dans `core.sales.api.event`.
- [x] Events self-contained avec payload complet (`OfflineSubmissionTicketDraft` + `OfflineSubmissionLineSnapshot[]`).
- [x] Publication via outbox `offline_event_outbox` (au lieu de `AfterCommit.run` direct) — at-least-once.
- [x] Drainer scheduler ShedLock — toutes les 5s configurable.
- [x] Listener retour offlinesync idempotent (`ProcessedEventPort` handler key `offlinesync.sales-offline-promotion-return`).
- [x] Ignorer event retour avec `promotionAttemptId` obsolète (`OfflineSyncPromotionPolicy.evaluateReturn`).
- [ ] `ApproveOfflineSubmissionCommandHandler` publie `OfflineSubmissionAdminApprovedEvent` via outbox (cf. ROADMAP R3).

## 8. Sales

- [x] Listener `OfflineSubmissionTechValidatedEvent` (`OfflineSubmissionPromotionEventListener`).
- [x] Listener `OfflineSubmissionAdminApprovedEvent` (idem, route vers le même handler).
- [x] Command `CreateTicketFromOfflineSubmissionCommand` + handler.
- [x] Mapping `OfflineSubmissionTicketDraft → Ticket` (`OfflineSubmissionToTicketMapper`) — utilise `GetDrawByIdQuery` pour résoudre `drawChannelId`, génère `TicketCodes`, construit `TicketLine[]` avec `Selection`, appelle `Ticket.place(POS_OFFLINE_SYNCED)`.
- [x] Protection DB unique `(tenant_id, offline_submission_id)` → handler catch `DataIntegrityViolationException` → DUPLICATE.
- [x] Publication `OfflineSubmissionProcessedEvent` (côté sales — actuellement via `AfterCommit.run`, sales-side outbox à venir cf. ROADMAP R2).
- [ ] Tests double listener / retry / duplicate (cf. ROADMAP Phase I).

## 9. LimitPolicy

- [x] Modèle `OfflineLimitPolicy` dans `core.limitpolicy.api.model.offline`.
- [x] Query `GetOfflineLimitPolicyQuery` + handler avec lookup tenant + fallback global.
- [x] Config `OfflineLimitPolicyProperties` (`tch.limitpolicy.offline.*`).
- [x] Per-tenant override : table `tenant_offline_policy` + adapter + `UpsertOfflineLimitPolicyCommand` + endpoint `PUT /admin/policies/limits/offline`.

## 10. REST

- [x] `POST /tenant/offline/grants` — réponse contient signature + codes + `upcomingDraws[]`.
- [x] `GET /tenant/offline/grants/current`.
- [x] `POST /tenant/offline/sync` (`drawId` requis dans chaque submission).
- [x] `GET /tenant/offline/submissions/my`.
- [x] `GET /tenant/offline/submissions/{id}/status`.
- [x] `GET /admin/offline/submissions`.
- [x] `POST /admin/offline/submissions/{id}/approve` (`decidedBy` propagé depuis ctx).
- [x] `POST /admin/offline/submissions/{id}/reject`.
- [x] `POST /admin/offline/submissions/{id}/replay-dry-run` (squelette compile, impl partielle).
- [x] `GET /admin/offline/dashboard`.
- [x] `GET|PUT /admin/policies/limits/offline` (per-tenant policy).
- [ ] Audit logging admin endpoints (`@AuditLog` ou aspect — cf. ROADMAP R10).

## 11. Jobs

- [x] `StuckSubmissionRecoveryScheduler` (toutes les 10 min, threshold 15 min).
- [x] `OrphanedCodeReservationScheduler` (toutes les 15 min, threshold 30 min, dispatch `ReleaseOrphanedReservedCodeCommand`).
- [x] `GrantExpirationScheduler` (toutes les 5 min).
- [x] `SyncAcceptedWindowCloseScheduler` (toutes les heures, impl JPQL join).
- [x] `OfflineEventOutboxDrainerScheduler` (toutes les 5s, ShedLock, batch 50, backoff exponentiel).
- [x] Tous : `@SchedulerLock` ShedLock + `JobContextBinder.bindTenant(...)` + MDC.

## 12. Flutter

> Plan mobile séparé — hors backend openspec.

- [ ] Module `features/offline`.
- [ ] Drift schema local.
- [ ] Grant cache + code cache + upcoming draws cache.
- [ ] Ed25519 signing device.
- [ ] Sale offline atomique.
- [ ] Sync outbox device.
- [ ] UI mode offline.
- [ ] Réimpression duplicata.

## 13. Tests E2E

> Phase I — cf. [`ROADMAP.md`](../../../tchalanet-core/src/main/java/com/tchalanet/server/core/offlinesync/ROADMAP.md#4-tests-poussés) pour le plan détaillé.

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
- [ ] outbox crash & resume.
- [ ] recover stuck submission.
- [ ] admin approve flow.
- [ ] per-tenant policy override.

## 14. Hardening (post-spec, livré)

- [x] Locks pessimistes (grant `lockForUpdate`, code `lockForReservation`).
- [x] Payload canonical signatures versionnées (`v1`).
- [x] Fail-fast crypto en profile prod/staging.
- [x] `getRequired` partout via `ProblemRest.notFound` (404 RFC 7807).
- [x] Outbox at-least-once + drainer.
- [x] ShedLock + tenant binding sur les 5 schedulers.
- [x] `decidedBy` audit (admin commands + table `offline_submission_decision`).
- [x] Per-tenant policy override.
- [x] Sous-packages par concept dans `api/model/` et `internal/domain/model/`.
- [ ] DrawCutoffRule check dans mapper offline (cf. ROADMAP R4).
- [ ] Sales-side outbox pour `OfflineSubmissionProcessedEvent` (cf. ROADMAP R2).
- [ ] Outbox cleanup retention job (cf. ROADMAP R6).
- [ ] Métriques Prometheus (cf. ROADMAP R9).
