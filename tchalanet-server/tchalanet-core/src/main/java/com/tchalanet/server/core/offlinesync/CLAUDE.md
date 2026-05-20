# Claude — core.offlinesync

Scope:

- Émission de grants offline signés Ed25519 (POS + N codes uniques + draws upcoming).
- Réception et validation technique des ventes offline (15 checks ordonnés, spec v2.1).
- Promotion vers `core.sales` via events self-contained, sans callback inverse.
- Cycle de vie code offline : `AVAILABLE → RESERVED → CONSUMED_PROMOTED|CONSUMED_REJECTED|EXPIRED|VOIDED` (jamais retour à AVAILABLE).
- Recovery ops : grants expirés, submissions bloquées, codes orphelins, fenêtre de sync fermée.

Out of scope:

- Création du ticket sales — `core.sales.OfflineSubmissionPromotionEventListener` consomme l'event et construit le `Ticket` (mapper `OfflineSubmissionToTicketMapper`).
- Cycle draw — `core.draw` (résolution `drawId` via `GetDrawByIdQuery`).
- Policy tenant — `core.limitpolicy` (`GetOfflineLimitPolicyQuery` + table `tenant_offline_policy`).
- POS context trusted — `core.session` / `TchRequestContext.operationalContext()`.
- Flutter device — hors backend (l'event embarque tout via `OfflineSubmissionTicketDraft`).

Rules:

- **Self-contained events** : `OfflineSubmissionTechValidatedEvent` et `AdminApprovedEvent` portent un `OfflineSubmissionTicketDraft` complet ; sales ne re-query JAMAIS offlinesync.
- **Outbox obligatoire** : tout publish d'event passe par `OfflineEventOutboxPort.record(...)` dans la même tx ; le drainer scheduler le publie ensuite. Pas de `AfterCommit.run(eventPublisher::publish)`.
- **Locks pessimistes** : `grantWriter.lockForUpdate(grantId)` au début du sync handler ; `codeWriter.lockForReservation(codeId)` avant `code.reserve(...)`.
- **Crypto** : grant signé via `OfflineGrantPayloadSigner` (format canonique v1). Submission verifiée via `OfflineCryptoPort.verifySubmission(payload, sig, devicePublicKey)`. Fail-fast en profile `prod/staging` si pas de clé serveur configurée.
- **Payload hash recomputé serveur** : `OfflineSubmissionPayloadHasher.hash(submission)` doit matcher le hash annoncé par le device (check #14 spec).
- **Domain pur** : pas de Spring/JPA dans `internal/domain/`. Records immutables, transitions retournant nouvelle instance, invariants au constructeur.
- **Typed IDs partout** : `OfflineGrantId`, `OfflineSubmissionId`, `OfflineCodeId`, `OfflineCodeBatchId`, `OfflineSyncBatchId`, `PromotionAttemptId` (jamais raw UUID hors persistence).
- **Sous-packages par concept** dans `internal/domain/model/{grant,code,codebatch,submission,syncbatch}/` ; idem `api/model/<concept>/` pour les sub-records exposés (status, lifecycle, quota, etc.).
- **`getRequired(...)`** sur les reader ports — pas de `findById(...).orElseThrow(NoSuchElement::new)` dans les handlers. `ProblemRest.notFound(code, id)` → 404 RFC 7807.
- **RLS tenant** : toutes les tables `offline_*` sont tenant-scoped, pas de filtre tenant_id explicite en Java sauf pour les jobs platform-scoped.
- **Idempotence** :
  - Sync batch : `(tenant_id, grant_id, client_batch_id)` UNIQUE → replay silencieux.
  - Submission : `(tenant_id, client_submission_id)` UNIQUE + check payload_hash → `DUPLICATE` ou `payload_mismatch`.
  - Ticket : `(tenant_id, offline_submission_id)` UNIQUE sur `sales_ticket` → protection finale contre double promotion.
  - Promotion return : `promotionAttemptId` matching + `ProcessedEventPort` (handler key `"offlinesync.sales-offline-promotion-return"`).
- **Schedulers** : `@SchedulerLock` (ShedLock) + `JobContextBinder.bindTenant(...)` autour de la query/dispatch, MDC `job=offlinesync:*`.

Key flows:

1. **Request grant** : POS → `RequestOfflineGrantCommand` → `OfflineGrantPolicy.evaluateIssue(policy)` → persist grant + code batch + N codes → `OfflineGrantPayloadSigner.sign(...)` → `ListUpcomingDrawsTenantWideQuery` → réponse `(grantSignature, serverPublicKey, codes[], upcomingDraws[])`.
2. **Sync sales** : POS → `SyncOfflineSalesCommand(submissions[])` → lock grant pessimiste → pour chaque submission : duplicate/hash check → lock code → `OfflineSubmissionTechnicalPolicy.evaluate(15 checks)` → si OK : `code.reserve`, save submission `TECH_VALIDATED`, save lines, bump grant quota, outbox `OfflineSubmissionTechValidatedEvent` → drainer publie → sales.
3. **Promotion retour** : sales émet `OfflineSubmissionProcessedEvent(outcome, ticketId, promotionAttemptId)` → `OfflineSubmissionProcessedEventListener` (offlinesync) → stale-check via `OfflineSyncPromotionPolicy` → `submission.markPromoted` / `markBusinessRejected`.
4. **Admin approve/reject** : `ApproveOfflineSubmissionCommand(decidedBy, reason)` → `submission.markAdminApproved` + persist `offline_submission_decision`. TODO Phase suivante : publier `OfflineSubmissionAdminApprovedEvent` via outbox pour retry promotion.
5. **Recover stuck** : `StuckSubmissionRecoveryScheduler` → `RecoverStuckOfflineSubmissionCommand` → bump `promotionAttemptId` → rebuild draft depuis `offline_submission_line` + `submission.payload().drawId()` → outbox event → sales retry.
6. **Expire grant** / **Orphaned code** / **Sync window close** : schedulers thin déléguant à des commands dédiées.

Before editing:

- Charger `DOMAIN_OFFLINESYNC.md` (ce dossier) pour le modèle d'agrégats + invariants.
- Charger `tchalanet-server/openspec/changes/add-offlinesync-module/specs/offlinesync/spec.md` pour les scénarios spec v2.1.
- Charger `docs/OFFLINESYNC_IMPLEMENTATION_GUIDE.md` (openspec) pour les flows détaillés et les ports.
- Si tu touches au mapper Ticket : charger aussi `core/sales/CLAUDE.md` + `DOMAIN_SALES.md`.
- Si tu touches à la policy : charger `core/limitpolicy/` pour `OfflineLimitPolicy`, `GetOfflineLimitPolicyQuery`, `tenant_offline_policy` table.

Output:

1. Files inspected
2. Files changed
3. Tests run / verification path
4. Risks (concurrency, RLS scope, idempotence, crypto)
5. Compact handoff
