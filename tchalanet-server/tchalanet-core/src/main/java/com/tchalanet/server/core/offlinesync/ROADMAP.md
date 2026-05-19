# Roadmap — `core.offlinesync`

> Document de suivi vivant : ce qui est livré, ce qui reste, les risques connus, et les
> tests poussés à écrire avant pilote / go-live. Mis à jour à chaque PR significative.
>
> Spec source de vérité : `tchalanet-server/openspec/changes/add-offlinesync-module/`
> Domaine focal : [`DOMAIN_OFFLINESYNC.md`](DOMAIN_OFFLINESYNC.md)
> Guide agent : [`CLAUDE.md`](CLAUDE.md)

---

## 1. État actuel

### ✅ Livré (Phases A → H + hardening Bloc 1+2 + #9–12)

| Phase | Livrable |
|---|---|
| **A — Foundations** | 5 typed IDs (`OfflineGrantId`, `OfflineSubmissionId`, `OfflineCodeId`, `OfflineSyncBatchId`, `OfflineCodeBatchId`, `PromotionAttemptId`), 5 enums statuts, 10 tables SQL + audit + RLS, feature flag `tch.offlinesync.enabled` |
| **B — Domain pur** | 5 agrégats records composés (`identity + lifecycle + …`), 4 policies pures (Grant, Submission Technical 15 checks, Code Transition, Promotion) |
| **C — Crypto Ed25519** | `OfflineCryptoPort` + adapter JDK 17+, signature canonique v1 (`OfflineGrantPayloadSigner`), payload hash canonique v1 (`OfflineSubmissionPayloadHasher`), fail-fast en profile prod/staging |
| **D — Commands/Queries** | 9 commands + 6 queries + handlers + 8 ports out + mappers JPA |
| **E — Events + sales** | 3 events spec self-contained (TechValidated, AdminApproved, Processed), sales listener + `CreateTicketFromOfflineSubmissionCommand` + `OfflineSubmissionToTicketMapper` fonctionnel via `GetDrawByIdQuery`, listener retour offlinesync (stale check via `OfflineSyncPromotionPolicy`) |
| **F — LimitPolicy** | `OfflineLimitPolicy` model + `GetOfflineLimitPolicyQuery` (Phase F) |
| **G — REST** | `OfflineTenantController` (5 endpoints), `OfflineAdminController` (5 endpoints) + DTOs web + `@PreAuthorize` |
| **H — Jobs** | 4 schedulers (`GrantExpiration`, `StuckRecovery`, `OrphanedCode`, `SyncWindowClose`) + ShedLock + tenant binding + MDC |
| **Hardening Bloc 1+2** | Locks pessimistes (grant + code), payload hash recomputé serveur, signature canonique versionnée, fail-fast crypto prod, `decidedBy` audit dans `offline_submission_decision`, `getRequired` partout via `ProblemRest.notFound`, trusted operational context propagé |
| **#1 + #2** | Mapper Ticket fonctionnel + `drawId` pinné device + `GetDrawByIdQuery` |
| **#9** | Per-tenant policy via table `tenant_offline_policy` + admin endpoint `PUT /admin/policies/limits/offline` + fallback global |
| **#10 + #11** | Persist `offline_submission_line` + recover republish event (rebuild draft) |
| **#12** | Outbox `offline_event_outbox` + drainer scheduler ShedLock + replace `AfterCommit.run` côté offlinesync handlers |
| **Upcoming draws** | Grant retourne `List<OfflineUpcomingDrawSnapshot>` (lookahead configurable, default 3 jours) via `ListUpcomingDrawsTenantWideQuery` |

### 🟡 Partiellement livré

| Item | Statut |
|---|---|
| **`ApproveOfflineSubmissionCommandHandler` retry promotion** | persist `decision` + `markAdminApproved` OK, mais ne publie pas encore `OfflineSubmissionAdminApprovedEvent` via outbox → sales ne retry pas |
| **`ReplayOfflineSubmissionCommandHandler`** | squelette compile, ne ré-évalue pas la TechnicalPolicy en dry-run |
| **`GetOfflineDashboardQueryHandler`** | retourne uniquement `pendingReview` count ; reste à 0 |
| **`SyncAcceptedWindowCloseScheduler`** | impl JPQL fonctionnelle, mais bypass partiel du domain (mutation entity directe pour set `SYNC_FAILED`) — pas idéal |

---

## 2. Roadmap — à faire

### 🔴 Critique (avant pilote)

| # | Item | Estimation | Notes |
|---|---|---|---|
| R1 | **Tests E2E backend** (Phase I) | 2–3 j | Voir section 4 ci-dessous |
| R2 | **Sales-side outbox** pour `OfflineSubmissionProcessedEvent` | ~3h | Symétrie avec offlinesync side. Table dédiée ou réutiliser pattern. |
| R3 | **`ApproveOfflineSubmissionCommandHandler` publie via outbox** | ~1h | `OfflineSubmissionAdminApprovedEvent` avec nouveau `promotionAttemptId` + rebuild draft (idem recover) |
| R4 | **`DrawCutoffRule` dans le mapper offline** | ~30 min | Reject si `clientSoldAt > draw.cutoffAt` à la promotion (code `sales.offline.cutoff_breached`) |
| R5 | **`SyncAcceptedWindowCloseScheduler` via domain** | ~1h | Refactor pour utiliser un `MarkSubmissionsSyncFailedCommand` plutôt que mutation entity directe |

### 🟠 Important (avant go-live)

| # | Item | Estimation | Notes |
|---|---|---|---|
| R6 | **Outbox cleanup retention job** | ~1h | DELETE FROM `offline_event_outbox` WHERE `published_at < now - INTERVAL '7 days'`. Idem sales-side si #R2 fait. |
| R7 | **Replay dry-run réel** | ~2h | Refactor `OfflineSubmissionTechnicalPolicy` pour exposer un mode `dry-run` retournant un rapport JSON (stocké en `offline_submission_decision.report_json`) |
| R8 | **Dashboard complet** | ~2h | Active grant count, last 24h accepted/rejected counts, stuck submissions count |
| R9 | **Métriques Prometheus** sur schedulers + outbox | ~2h | `offlinesync_grants_expired_total`, `offlinesync_outbox_pending_count`, `offlinesync_submission_promoted_total` (labels: outcome) |
| R10 | **Audit logging admin endpoints** | ~1h | `@AuditLog` ou wrapper aspect pour traces Approve/Reject/Revoke grant + lookups |
| R11 | **Token rotation Ed25519** server | ~1j | Keyset `current`/`previous` + grant signe avec `current`, device verify avec n'importe lequel. Migration sans casser grants en vol. |

### 🟡 Nice to have

| # | Item | Estimation | Notes |
|---|---|---|---|
| R12 | **Outbox admin replay endpoint** | ~1h | `POST /admin/offline/outbox/{id}/replay` pour forcer un republish manuel |
| R13 | **`OfflineCodeGenerator` retry sur collision** | ~30 min | Wrap `codeWriter.save` dans try/catch `DataIntegrityViolationException` + retry 3× |
| R14 | **Bulk expire grants** | ~1h | Refactor `ExpireOfflineGrantCommand` → bulk UPDATE SQL pour gros volumes |
| R15 | **Audit table sur `offline_event_outbox`** | ~30 min | Si traceability outbox demandée. Actuellement `@NotAudited`. |
| R16 | **Cleanup `token_hash` legacy** sur `offline_grant` | ~30 min | Colonne NOT NULL inutilisée, fallback `""`. Drop ou réutilise pour stocker la signature hex. |
| R17 | **Reason cohérence approve** | 5 min | DTO `AdminReasonRequest.reason @NotBlank` mais command accepte reason null. Aligner. |
| R18 | **Cleanup `OfflineSubmissionForSalesView`** | déjà fait | ✅ retiré Phase E1 |
| R19 | **Mobile Flutter alignement** | Plan mobile séparé | Le device doit cacher `upcomingDraws[]`, pin `drawId` par vente, envoyer dans le payload |

### 🔵 Long-terme

- Multi-draw tickets (actuellement 1 draw / submission) — gros refactor `TicketContext`.
- Streaming sync (push device → server au fil de l'eau plutôt qu'en batches) — repensent l'API.
- Per-tenant cryptographic isolation (clé serveur par tenant au lieu d'une clé globale).
- Outbox génériquement réutilisable dans d'autres modules (extract en `common/` ou `platform/`).

---

## 3. Gestion des risques

> Convention : chaque risque a un **owner** (module touché), un **niveau** (🔴/🟠/🟡), une **stratégie de mitigation** active, et un **canari** (signal qui dit "ça casse").

### Concurrence

| Risque | Niveau | Mitigation actuelle | Canari |
|---|---|---|---|
| Double batch concurrent sur même grant → quota dépassé | 🔴 | `grantWriter.lockForUpdate(grantId)` au début du sync handler (PESSIMISTIC_WRITE) | `IllegalStateException("maxTicketCount exceeded")` |
| Course condition sur `code.reserve` (AVAILABLE → RESERVED) | 🔴 | `codeWriter.lockForReservation(codeId)` (PESSIMISTIC_WRITE) | `OptimisticLockException` catch → outcome `offlinesync.code.race_lost` |
| Drainer outbox multi-pod fire en // → double publish | 🟠 | ShedLock global + idempotence consumer via `ProcessedEventPort` | `processed_event.handler_key` dup count |
| Stuck submission jamais récupérée | 🟠 | `StuckSubmissionRecoveryScheduler` toutes les 10 min, threshold 15 min | Compteur `submissions PROMOTION_REQUESTED` non décroissant |
| Code RESERVED orphelin | 🟠 | `OrphanedCodeReservationScheduler` toutes les 15 min, threshold 30 min + `ReleaseOrphanedReservedCodeCommand` | Compteur `codes RESERVED > 30min` |

### Sécurité crypto

| Risque | Niveau | Mitigation actuelle | Canari |
|---|---|---|---|
| Ephemeral key en prod (mauvaise config) | 🔴 | Fail-fast au boot dans profile `prod`/`staging` (`IllegalStateException`) | Boot crash + log |
| Signature device forgée | 🔴 | Vérif Ed25519 strict + check device public key matche grant (check #9 + #10) | Tech rejection `offlinesync.signature.invalid` ou `.device_mismatch` |
| Payload hash forgé | 🔴 | Recompute serveur via `OfflineSubmissionPayloadHasher` (check #14) | Tech rejection `offlinesync.submission.payload_mismatch` |
| Signature non versionnée → impossible de rotater format | 🟡 | Format canonique `v1` explicite | À monitorer si on ajoute v2 |
| Pas de rotation clé serveur | 🟠 | Aucune (cf. R11) | Si secret compromis → invalidation manuelle de tous les grants actifs |

### Données

| Risque | Niveau | Mitigation actuelle | Canari |
|---|---|---|---|
| Double promotion (2 tickets pour 1 submission) | 🔴 | UNIQUE `(tenant_id, offline_submission_id)` sur `sales_ticket` + handler catch `DataIntegrityViolationException` → DUPLICATE outcome | `sales_ticket` unique violation log |
| Event de promotion perdu (crash pod entre commit et publish) | 🔴 | Outbox + drainer (commit guaranti, drainer at-least-once) | `offline_event_outbox.published_at IS NULL AND attempts > 5` |
| Stale promotion event applied | 🔴 | `OfflineSyncPromotionPolicy.evaluateReturn` ignore si `promotionAttemptId` ≠ courant | `submission.promotion.attemptId` reste cohérent post-recover |
| Submission duplicate / payload mismatch | 🟠 | UNIQUE `(tenant_id, client_submission_id)` + check hash → réponse `DUPLICATE` ou conflit | API rejection `offlinesync.submission.payload_mismatch` |
| Sync window close → submissions perdues | 🟠 | `SyncAcceptedWindowCloseScheduler` → mark `SYNC_FAILED` + raison explicite | Compteur `SYNC_FAILED` croissant |

### Operational

| Risque | Niveau | Mitigation actuelle | Canari |
|---|---|---|---|
| Outbox grossit sans cleanup | 🟡 | Aucune (cf. R6) | Row count `offline_event_outbox` |
| Schedulers actifs en tests `@SpringBootTest` | 🟡 | `@ConditionalOnProperty("tch.offlinesync.enabled")` désactive si flag off | Tests qui assert sur events imprévus |
| Tenant binding RLS leak | 🟠 | `JobContextBinder.bindTenant(...)` per-tenant dans schedulers, RLS strict sur écritures, `allow_platform_cross_tenant_select` pour reads ops | Logs "0 row" suspects |
| `ListUpcomingDrawsTenantWideQuery` retourne trop de draws (perf) | 🟡 | `limit` configurable (default 50) + window `lookaheadHours` borné | API response size |

### Cross-module

| Risque | Niveau | Mitigation actuelle | Canari |
|---|---|---|---|
| `core.sales` mapper non disponible (draw non résolu, lignes invalides) | 🟠 | `BUSINESS_REJECTED("sales.offline.draw_not_resolved")` + `business_rule_violation` | Compteur outcomes `BUSINESS_REJECTED` |
| `core.draw` cutoff dépassé entre vente et sync | 🟠 | (cf. R4) — à ajouter | Ticket créé pour draw déjà CLOSED |
| `core.limitpolicy` policy tenant manquante | 🟡 | Fallback global défaults | Si vouloir forcer per-tenant : check explicite dans le handler |

---

## 4. Tests poussés

> Phase I (tests E2E) jamais commencée. Voici le plan détaillé.

### Unitaires (existants)
- [x] `OfflineCodeTest` — transitions + invariant RESERVED↛AVAILABLE
- [x] `OfflineSyncPromotionPolicyTest` — apply / stale / null incoming
- [x] `Ed25519OfflineCryptoAdapterTest` — round-trip, tampered, key mismatch, malformed

### Unitaires à écrire ⭐

#### Domain
- [ ] `OfflineGrantTest` — issue/revoke/expire/recordValidatedTicket transitions + invariants window/quota
- [ ] `OfflineCodeBatchTest` — open/incrementConsumed/expire + `isExpired` boundary
- [ ] `OfflineSyncBatchTest` — counters → status computed
- [ ] `OfflineSubmissionTest` — 10 statuses state machine, transitions valides/invalides
- [ ] `OfflineGrantPolicyTest` — accept/reject par check (offline disabled, batch invalide, validity invalide)
- [ ] `OfflineSubmissionTechnicalPolicyTest` — **les 15 checks individuellement** : un test par check avec un input qui fait échouer ce check précis
- [ ] `OfflineCodeTransitionPolicyTest` — transition table exhaustive
- [ ] `OfflineSubmissionPayloadHasherTest` — hash deterministe / ordre lignes / nullable potentialPayout
- [ ] `OfflineGrantPayloadSignerTest` — round-trip avec adapter Ed25519

#### Application (handlers, mocks ports)
- [ ] `RequestOfflineGrantCommandHandlerTest` — happy path + policy reject + signature présente + upcoming draws câblé
- [ ] `RevokeOfflineGrantCommandHandlerTest` — revoke ACTIVE → REVOKED, revoke non-ACTIVE → IllegalState
- [ ] `ExpireOfflineGrantCommandHandlerTest`
- [ ] `SyncOfflineSalesCommandHandlerTest` — 6 scénarios :
  - Happy path 1 submission → TECH_VALIDATED + event in outbox
  - Duplicate client_submission_id same hash → DUPLICATE
  - Duplicate client_submission_id different hash → conflict `payload_mismatch`
  - Tech reject (signature invalide) → code `CONSUMED_REJECTED` + submission `TECH_REJECTED`
  - Replay batch (même client_batch_id) → DUPLICATE outcomes sans re-processing
  - Quota grant dépassé → rejection `offlinesync.grant.quota_exceeded`
- [ ] `ApproveOfflineSubmissionCommandHandlerTest` — `markAdminApproved` + decision persisted
- [ ] `RejectOfflineSubmissionCommandHandlerTest` — idem reject
- [ ] `RecoverStuckOfflineSubmissionCommandHandlerTest` — bump attempt + outbox event + rebuild draft contient drawId/lignes
- [ ] `ReleaseOrphanedReservedCodeCommandHandlerTest` — code `RESERVED → CONSUMED_REJECTED` + submission `SYNC_FAILED`
- [ ] `CreateTicketFromOfflineSubmissionCommandHandlerTest` (sales) — fast-path duplicate, mapper succeed, DataIntegrityViolation → DUPLICATE, draw not found → BUSINESS_REJECTED
- [ ] `GetOfflineLimitPolicyQueryHandlerTest` — tenant override vs global fallback

#### Infrastructure
- [ ] `OfflineEventOutboxDrainerSchedulerTest` — drain happy path + retry backoff sur deserialize error + idempotence si publié 2×
- [ ] `OfflineSubmissionPromotionEventListenerTest` (sales) — dispatch command + publish processed event
- [ ] `OfflineSubmissionProcessedEventListenerTest` (offlinesync) — apply Promoted / BusinessRejected / Duplicate / stale ignored

### Intégration (Testcontainers Postgres) ⭐⭐

> Doit valider RLS, contraintes uniques, transactions, FK, idempotence DB.

- [ ] **Migration test** : V100 + V101 + V103 + V104 + V105 appliquent sans erreur
- [ ] **RLS strict** : sans tenant context bindé, les `findAll` sur tables offline retournent `[]`
- [ ] **RLS isolation** : tenant A ne voit pas les rows de tenant B
- [ ] **Unique constraints** :
  - `(tenant_id, code)` sur `offline_code` → 2 inserts avec même code → violation
  - `(tenant_id, client_submission_id)` sur `offline_submission`
  - `(tenant_id, grant_id, client_batch_id)` sur `offline_sync_batch`
  - `(tenant_id, offline_submission_id)` sur `sales_ticket` (où non-null) → 2 tickets pour même submission → violation
- [ ] **CHECK constraints** : `offline_grant.valid_from < valid_until ≤ sync_accepted_until` → insert invalide rejeté
- [ ] **JPA optimistic locking** : 2 saves concurrent avec même version → OptimisticLockException
- [ ] **Pessimistic locking** :
  - `lockForUpdate(grantId)` bloque un second appelant
  - `lockForReservation(codeId)` idem

### End-to-end (`@SpringBootTest` + Testcontainers) ⭐⭐⭐

> Le test ultime : POS request grant → sync → ticket créé.

- [ ] **E2E Happy path** :
  1. `POST /tenant/offline/grants` avec contexte trusted → réponse contient signature + codes + upcoming draws
  2. `POST /tenant/offline/sync` avec 3 submissions (1 ligne chacune, payload signé device, drawId pinné depuis upcoming) → 3 outcomes ACCEPTED
  3. Wait for outbox drainer (5s) → events TechValidated publiés
  4. Sales listener invoque mapper → Tickets créés → `OfflineSubmissionProcessedEvent` publié
  5. Offlinesync listener applique outcome → submission PROMOTED + `created_ticket_id` set
  6. `GET /tenant/offline/submissions/{id}/status` retourne `PROMOTED` + `createdTicketId`
- [ ] **E2E Duplicate payload** : même `clientSubmissionId` + même hash → 2e call DUPLICATE, pas de nouvelle row
- [ ] **E2E Payload mismatch** : même `clientSubmissionId` + hash ≠ → conflit `offlinesync.submission.payload_mismatch`
- [ ] **E2E Sync window** :
  - Sync entre `validUntil` et `syncAcceptedUntil` → ACCEPTED
  - Sync après `syncAcceptedUntil` → REJECTED tech `offlinesync.grant.sync_window_closed`
- [ ] **E2E Quota dépassé** : `maxTicketCount=2`, submitter 3 submissions → 2 ACCEPTED + 1 rejection `offlinesync.grant.quota_exceeded`
- [ ] **E2E Grant revoked** : grant ACTIVE → revoke → submission après revoke → tech reject `offlinesync.grant.revoked`
- [ ] **E2E Stale promotion event ignored** : provoquer un retour avec ancien `promotionAttemptId` (mock sales) → submission inchangée + log `stale promotion attempt`
- [ ] **E2E Double promotion impossible** : forcer 2 listeners simultanés → 1 PROMOTED + 1 DUPLICATE via unique constraint
- [ ] **E2E Outbox crash simulation** : kill drainer entre commit et publish → relancer → event délivré au prochain tick
- [ ] **E2E Admin approve** : submission BUSINESS_REJECTED → admin approve → submission ADMIN_APPROVED + decision row + (TODO) sales retry
- [ ] **E2E Recover stuck** : submission PROMOTION_REQUESTED > 15 min → scheduler trigger → nouveau attempt + outbox event → sales retry
- [ ] **E2E Per-tenant policy override** : PUT `/admin/policies/limits/offline` avec `batchSize=20` → grant suivant a 20 codes (au lieu du global 50)

### Tests de charge / soak ⭐⭐⭐⭐

> À planifier en pré-prod sur env staging.

- [ ] **Concurrent batches** : 100 syncs simultanés sur même grant avec quota 50 → exactement 50 ACCEPTED, le reste REJECTED quota
- [ ] **Outbox throughput** : 10K events en attente → drainer 5s × batch 50 → vérifier vidage < 20 min sans backoff
- [ ] **Scheduler tenants** : 100 tenants actifs × 4 schedulers → tick complet < 30s
- [ ] **Crypto bench** : 1000 verify Ed25519 → < 1s (JDK natif)

### Sécurité ⭐⭐⭐

- [ ] **Token replay** : intercepter une submission valide, modifier `clientSoldAt` → signature invalide
- [ ] **Grant signature forgery** : signer avec une clé serveur factice → device verify échoue
- [ ] **Cross-tenant** : tenant A envoie un payload référant un code d'un grant tenant B → RLS bloque (404 ou rejection `offlinesync.code.not_found`)
- [ ] **Admin RBAC** : `AGENT` appelle `/admin/offline/submissions/{id}/approve` → 403

---

## 5. Indicateurs de santé (dashboard ops à brancher)

Métriques à exposer via Prometheus / actuator (cf. R9) :

```
offlinesync_grants_active{tenant}                       # gauge
offlinesync_grants_expired_total                        # counter
offlinesync_submissions_received_total{outcome}         # counter (ACCEPTED|REJECTED|DUPLICATE)
offlinesync_submissions_pending_review{tenant}          # gauge
offlinesync_submissions_stuck{tenant}                   # gauge
offlinesync_codes_orphaned_reservation                  # gauge
offlinesync_outbox_pending                              # gauge
offlinesync_outbox_drained_total{outcome}               # counter (published|failed)
offlinesync_signature_verify_seconds                    # histogram
offlinesync_promotion_round_trip_seconds                # histogram (sync → ticket created)
```

Alertes recommandées :
- `offlinesync_outbox_pending > 100 for 5min` → backlog, drainer probable down
- `offlinesync_submissions_stuck > 0 for 30min` → recovery non efficient
- `offlinesync_codes_orphaned_reservation > 0 for 1h` → investigate
- `offlinesync_signature_verify_seconds p99 > 100ms` → perf regression

---

## 6. Cycle de release & merge openspec

- `add-offlinesync-module/` = source de vérité actuelle.
- `add-offlinesync/` (legacy) = à archiver une fois `add-offlinesync-module` complètement aligné avec impl.
- Quand un risque migrate de "🔴 à faire" → "✅ livré" : mettre à jour cette ROADMAP **et** la section "TODO" du `DOMAIN_OFFLINESYNC.md`.
- À chaque PR significative : checkbox cocher dans `openspec/changes/add-offlinesync-module/tasks.md`.

---

## 7. Décisions architecturales clés (récap)

| Décision | Justification | Référence |
|---|---|---|
| 1 ticket = 1 draw | Cohérence `TicketContext.drawId` ; multi-draw = futur refactor | spec v2.1 |
| Device pin le `drawId` (pas résolveur serveur) | Le cashier voit la draw au moment de la vente ; pas de devinette serveur sur clientSoldAt | Discussion #2 |
| Outbox tenant-scoped (pas global) | Réduire le risque blast-radius si vol/corruption events | Discussion #12 |
| `getRequired` sur reader ports (vs `findById.orElseThrow` partout) | Convention Tchalanet → 404 RFC 7807 propre | `core/sales` precedent |
| Per-tenant policy override en base (table dédiée) | Permet customisation tenant sans redeploy ; fallback config si absent | Discussion #9 |
| Code RESERVED ne revient jamais à AVAILABLE | Audit fraude — un code "consommé" doit rester consommé même si tech reject | spec v2.1 |
| Signature canonique versionnée (v1) | Future-proof rotation format payload | Discussion crypto |
| Events self-contained (TicketDraft embarqué) | Sales ne re-query JAMAIS offlinesync → couplage faible | spec v2.1 |
| Lignes persistées en DB (`offline_submission_line`) | Recovery doit rebuild draft sans tenir le payload device | Discussion #11 |
| `decidedBy` requis sur admin commands | Compliance / traceability | Discussion audit logs |
