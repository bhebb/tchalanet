# Domaine `core.offlinesync` — Ventes offline

> Permet à un POS hors-réseau de vendre, puis de synchroniser les ventes au retour de connectivité. Les ventes sont signées Ed25519 côté device et validées techniquement côté serveur avant promotion vers `core.sales`.

> Spec source de vérité : `tchalanet-server/openspec/changes/add-offlinesync-module/`
> Module sales aval : `tchalanet-core/.../core/sales/DOMAIN_SALES.md`
> Cycle draw amont : `tchalanet-core/.../core/draw/`

---

## 1. Rôle du domaine

- **Émettre des grants** signés serveur qui autorisent un device POS à produire N ventes offline pendant une fenêtre `[validFrom, validUntil)`.
- **Stocker N codes** dans un code batch (status `AVAILABLE → RESERVED → CONSUMED_*`) — un code par vente, jamais retour à `AVAILABLE`.
- **Recevoir des batches de submissions** signés device au retour online, appliquer les 15 checks ordonnés d'`OfflineSubmissionTechnicalPolicy`.
- **Publier les events de promotion** self-contained vers `core.sales` via une outbox (at-least-once).
- **Recevoir la réponse** de sales (ticket créé ou business rejected) et appliquer l'outcome si l'attempt courant matche.
- **Recovery ops** : grants expirés, submissions stuck, codes orphelins, fenêtre sync fermée.

**Ce que le domaine ne fait pas**

- Construction du ticket sales — `core.sales.OfflineSubmissionToTicketMapper`.
- Résolution du draw — le device pin le `drawId` au moment de la vente ; sales fait `GetDrawByIdQuery(drawId)`.
- Policy tenant — `core.limitpolicy.GetOfflineLimitPolicyQuery` + table `tenant_offline_policy`.
- Validation du contexte POS trusted — `TchRequestContext.operationalContext().trustedForSensitiveOperation()`.

---

## 2. Modèle & invariants

### Agrégat `OfflineGrant`

Composé : `GrantIdentity + GrantDevice + GrantValidityWindow + GrantQuota + GrantLifecycle`.

**Champs clés** :
- Identité : `id`, `tenantId`, `sellerUserId`, `terminalId`, `outletId`, `salesSessionId`
- Device : `deviceId (UUID)`, `devicePublicKey (X.509 SPKI base64, @NotAudited)`, `keyId`
- Window : `validFrom`, `validUntil`, `syncAcceptedUntil` (extension de grâce pour late uploads)
- Quota : `maxTicketCount`, `maxTotalAmount (Money)`, `consumedTicketCount`, `consumedTotalAmount`
- Lifecycle : `status (ACTIVE|EXPIRED|REVOKED)`, `issuedAt`, `revokedAt`, `revokedReason`

**Invariants** :
- `validFrom < validUntil ≤ syncAcceptedUntil` (CHECK SQL + record constraint)
- `consumedTicketCount ≤ maxTicketCount` (vérifié dans `canAccept(stake)`)
- `consumedTotalAmount + stake ≤ maxTotalAmount`
- `recordValidatedTicket(stake)` exige `status == ACTIVE` + quota libre
- `revoke(reason, now)` exige `status == ACTIVE` ET `reason` non-blank

### Agrégat `OfflineCodeBatch`

Composé : `CodeBatchIdentity + CodeBatchCounts + CodeBatchLifecycle`.

- `allocatedCount` (size de la batch), `consumedCount` (incremente à chaque code passé en `RESERVED`)
- Status : `ACTIVE | EXPIRED`
- `incrementConsumed()` refuse si batch `EXPIRED` ou allocation épuisée

### Agrégat `OfflineCode` ⭐

Composé : `CodeIdentity + CodeLifecycle`.

**Invariant clé spec v2.1** : **un code submis (passé en `RESERVED`) ne revient JAMAIS à `AVAILABLE`**.

| Transition | Status final | Méthode |
|---|---|---|
| `AVAILABLE → RESERVED` | `RESERVED` | `reserve(submissionId, now)` — refuse si déjà reservé ou expiré |
| `RESERVED → CONSUMED_PROMOTED` | `CONSUMED_PROMOTED` | `markConsumedPromoted(ticketId, now)` |
| `RESERVED → CONSUMED_REJECTED` | `CONSUMED_REJECTED` | `markConsumedRejected(now)` — sur tech rejection ou recovery orphan |
| `AVAILABLE → EXPIRED` | `EXPIRED` | `expire()` (no-op si pas AVAILABLE) |
| `AVAILABLE → VOIDED` | `VOIDED` | `voidUnused()` |

Pessimistic lock via `OfflineCodeWriterPort.lockForReservation(codeId)` avant `reserve(...)` pour serializer.

### Agrégat `OfflineSyncBatch`

Composé : `SyncBatchIdentity + SyncBatchContext + SyncBatchCounters + SyncBatchLifecycle`.

- Idempotence : `(tenant_id, grant_id, client_batch_id)` UNIQUE → replay silencieux.
- Status calculé depuis les counters : `RECEIVED → PARTIALLY_ACCEPTED → ACCEPTED|REJECTED → COMPLETED`.
- `withCounters(techReject, salesAccept, salesReject, review, processedAt)` compute le status terminal.

### Agrégat `OfflineSubmission`

Composé : `SubmissionIdentity + SubmissionContext + SubmissionPayload + SubmissionLifecycle + SubmissionPromotionTrace`.

**Champs payload** (persistés pour recovery) : `drawId` (obligatoire — pinné par device), `clientSoldAt`, `totalStakeAmount`, `lineCount`, `payloadHash`, `signature`.

**Status state machine** :
```
RECEIVED ─────────┬─→ TECH_VALIDATED ─→ PROMOTION_REQUESTED ─→ PROMOTED
                  │                                          ╲
                  │                                           ╲→ BUSINESS_REJECTED ─→ NEEDS_ADMIN_REVIEW
                  │                                                                        ↓
                  │                                                           ADMIN_APPROVED|ADMIN_REJECTED
                  └─→ TECH_REJECTED
                  └─→ SYNC_FAILED (orphan recovery, sync window closed)
```

`DUPLICATE` n'est PAS un status persistant — réponse API uniquement.

**`SubmissionPromotionTrace`** porte le `promotionAttemptId` courant ; un retour avec un attempt obsolète est ignoré par `OfflineSyncPromotionPolicy.evaluateReturn`.

### Sous-aggrégats / value records exposés (`api/model/<concept>/`)

- `GrantValidityWindow.containsForSale(clientSoldAt)` / `.acceptsSyncAt(receivedAt)`
- `GrantQuota.canAccept(stake)` / `.recordValidated(stake)`
- `CodeLifecycle.reserved(submissionId, now)` / `.consumedPromoted(ticketId, now)` / etc.
- `SubmissionLifecycle.transitionTo(...)` / `.reject(status, code, reason, now)`
- `SubmissionPromotionTrace.withAttempt(...)` / `.withCreatedTicket(...)` / `.isCurrentAttempt(...)`

---

## 3. Policies pures (`internal/domain/service/`)

### `OfflineGrantPolicy.evaluateIssue(Inputs, now) → Decision`
Vérifie : tenant offline enabled, batch size > 0, validity duration > 0. Retourne `Accept(window, quota)` ou `Reject(code, reason)`.

### `OfflineSubmissionTechnicalPolicy.evaluate(Inputs) → Decision` ⭐
**Les 15 checks ordonnés du spec v2.1**, dans cet ordre :

1. Tenant feature flag `offlinesync.enabled` ON
2. Plan tenant autorise offline
3. Trusted operational context présent (`ctx.operationalContext().trustedForSensitiveOperation()`)
4. POS context validé (`ResolvePosOperationContextQuery` — TODO câbler proprement)
5. Grant existe
6. Grant non REVOKED
7. `clientSoldAt ∈ [validFrom, validUntil)`
8. `receivedAt ≤ syncAcceptedUntil`
9. `deviceId` + `devicePublicKey` matchent le grant
10. Signature Ed25519 valide (`OfflineCryptoPort.verifySubmission`)
11. Code existe dans le batch
12. Code `AVAILABLE` (puis lock pessimiste vers `RESERVED`)
13. Code batch non expiré (`receivedAt < batch.expiresAt`)
14. `declaredPayloadHash == recomputedPayloadHash` (`OfflineSubmissionPayloadHasher`)
15. Quota grant non dépassé (`grant.canAccept(stake)`)

Codes d'erreur stables : `offlinesync.disabled`, `offlinesync.plan_forbidden`, `offlinesync.context_untrusted`, `offlinesync.pos_context_invalid`, `offlinesync.grant.not_found`, `offlinesync.grant.revoked`, `offlinesync.grant.outside_validity`, `offlinesync.grant.sync_window_closed`, `offlinesync.device_mismatch`, `offlinesync.signature.invalid`, `offlinesync.code.not_found`, `offlinesync.code.batch_mismatch`, `offlinesync.code.not_available`, `offlinesync.code.batch_expired`, `offlinesync.submission.payload_mismatch`, `offlinesync.grant.quota_exceeded`.

### `OfflineCodeTransitionPolicy.canTransition(from, to) → boolean`
Table de transitions autorisées. Invariant central : `RESERVED → AVAILABLE` toujours `false`.

### `OfflineSyncPromotionPolicy.evaluateReturn(submission, incomingAttempt) → Outcome`
Apply ou Ignore selon le matching `promotionAttemptId` + status courant.

---

## 4. Events publiés

| Event | Émis par | Consommé par | Payload self-contained |
|---|---|---|---|
| `OfflineSubmissionTechValidatedEvent` | `SyncOfflineSalesCommandHandler` + `RecoverStuckOfflineSubmissionCommandHandler` (via outbox) | `core.sales.OfflineSubmissionPromotionEventListener` | grantId, codeId, offlineCode, promotionAttemptId, `OfflineSubmissionTicketDraft(drawId, sellerUser, terminal, outlet, session, device, soldAt, stake, lines[])` |
| `OfflineSubmissionAdminApprovedEvent` | `ApproveOfflineSubmissionCommandHandler` (TODO outbox) | `core.sales` (idem) | Idem + `approvedBy`, `approvalReason` |
| `OfflineSubmissionProcessedEvent` | `core.sales` après création ticket OU rejet métier | `OfflineSubmissionProcessedEventListener` (offlinesync) | outcome (`PROMOTED`/`BUSINESS_REJECTED`/`DUPLICATE`), ticketId, rejectionCode/reason, promotionAttemptId |

**Outbox pattern** : les 2 events offlinesync sont écrits dans `offline_event_outbox` dans la même tx que les writes métier ; `OfflineEventOutboxDrainerScheduler` (ShedLock, 5s) draine + publie. Garantit at-least-once après crash pod.

---

## 5. Tables SQL

| Table | Rôle | Notes |
|---|---|---|
| `offline_grant` | 1 row par grant émis | `currency`, `consumed_*_count`, `key_id`, `device_public_key (NotAudited)`, CHECK windows |
| `offline_code_batch` | 1 row par batch de codes | `grant_id FK`, `allocated_count`/`consumed_count` |
| `offline_code` | 1 row par code (avant rename : `offline_code_reservation`) | `UNIQUE (tenant_id, code)`, FK `grant_id`/`code_batch_id`/`offline_submission_id`/`ticket_id` |
| `offline_sync_batch` | 1 row par upload batch device | `UNIQUE (tenant_id, grant_id, client_batch_id)` |
| `offline_submission` | 1 row par submission reçue | `draw_id NOT NULL FK draw`, `promotion_attempt_id`, `last_promotion_event_id`, `created_ticket_id`, `UNIQUE (tenant_id, client_submission_id)` |
| `offline_submission_line` | N rows par submission | Persistées pour permettre recovery (rebuild draft) |
| `offline_submission_ticket_link` | Audit promotion ↔ ticket | `UNIQUE (tenant_id, submission_id) WHERE link_type='CREATED'` |
| `offline_submission_decision` | Trace admin approve/reject/replay | `decided_by`, `dry_run`, `report_json` |
| `offline_event_outbox` | Outbox events publication | `UNIQUE (tenant_id, event_id)`, index partiel `pending` |
| `tenant_offline_policy` | Override per-tenant de la policy globale | `UNIQUE (tenant_id)` ; fallback config si absent |

Côté `core.sales` : colonne `ticket.offline_submission_id` + index unique partiel = protection finale contre double promotion.

---

## 6. Crypto Ed25519

- **Server keypair** : config `tch.offlinesync.crypto.server-{private,public}-key` (PKCS#8 / X.509 SPKI base64). Ephemeral keypair generated at boot si absent — interdit en profile `prod/staging`.
- **Grant signature canonique v1** : `OfflineGrantPayloadSigner` ; format `v1|grantId|tenantId|deviceId|keyId|validFrom|validUntil|syncAcceptedUntil|maxTicketCount|maxTotalAmount.value:currency`.
- **Submission verification** : `OfflineCryptoPort.verifySubmission(payloadBytes, sigB64, devicePublicKeyB64)` via JDK 17+ `Signature.getInstance("Ed25519")`.
- **Submission payload hash v1** : `OfflineSubmissionPayloadHasher` ; SHA-256 sur format canonique versionné incluant `clientSubmissionId|offlineCode|clientSoldAt|stake|lineCount|lines[]` triées par `lineNo`.

---

## 7. Concurrence & idempotence

- **Grant lock pessimiste** : `OfflineGrantWriterPort.lockForUpdate(grantId)` au début de `SyncOfflineSalesCommandHandler.handle` — serializes les batches concurrent sur le même grant pour la cohérence des compteurs quota.
- **Code lock pessimiste** : `OfflineCodeWriterPort.lockForReservation(codeId)` avant chaque `code.reserve(...)`.
- **Batch idempotence** : `findByClientBatchId` au début → si présent, `DUPLICATE` outcomes returned sans re-processing.
- **Submission idempotence** : `findByClientSubmissionId` → si même payloadHash : `DUPLICATE` ; si différent : conflit `payload_mismatch`.
- **Promotion idempotence** : `ProcessedEventPort` côté sales (key `sales.offline-promotion`) + côté offlinesync sur le retour (key `offlinesync.sales-offline-promotion-return`).
- **Outbox at-least-once** : le drainer peut republier ; le consumer est idempotent via `ProcessedEventPort`.

---

## 8. Risques connus & TODO

- `OfflineSubmissionAdminApprovedEvent` n'est pas encore publié par `ApproveOfflineSubmissionCommandHandler` (TODO).
- `OfflineSubmissionProcessedEvent` côté sales utilise encore `AfterCommit.run` (pas d'outbox sales-side).
- `DrawCutoffRule` check pas appliqué dans le mapper offline (vérifier `clientSoldAt ≤ draw.cutoffAt`).
- Pas de retention/cleanup auto sur `offline_event_outbox` ; à ajouter quand le volume monte.
- `OfflineDashboardQueryHandler` partiel (seul `pending_review` count).

Voir `tchalanet-server/openspec/changes/add-offlinesync-module/docs/OFFLINESYNC_DECISIONS.md` pour les décisions architecturales détaillées et `OFFLINESYNC_IMPLEMENTATION_GUIDE.md` pour les flows complets.
