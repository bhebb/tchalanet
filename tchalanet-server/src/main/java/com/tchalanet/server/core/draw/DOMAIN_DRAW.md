# Domaine Core Draw

> Lifecycle complet d'un draw tenant : génération, ouverture/fermeture de la vente, application du résultat externe global, settlement des tickets, et risk management. Domaine **tenant-scoped** (RLS active). Source de vérité du flow draw côté backend.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Gérer le cycle de vie d'un `Draw` tenant — de sa génération depuis un `draw_channel` jusqu'à son settlement final — en orchestrant les transitions d'état, l'application du résultat externe et le risk management associé.

**Ce que le domaine fait**

- Génère les `Draw` à partir des `draw_channel` actifs (par tenant, sur un horizon glissant J → J+7).
- Gère la state machine commerciale : `SCHEDULED → OPEN → CLOSED`.
- Lie le `Draw` à son `draw_result` global (transition vers `RESULTED`).
- Orchestre le settlement des tickets via Spring Batch (transition vers `SETTLED`).
- Gère les annulations administratives (`CANCELED`) et l'archivage (`ARCHIVED`).
- Maintient l'exposition du tenant (`draw_exposure`) — risk management par scope.
- Publie les events tenants pour les domaines consommateurs (`sales`, `stats`, `cache`, `notifications`, `payout`).
- Expose un API admin et ops via controllers `/admin/draws` et `/platform/ops/draws`.

**Ce que le domaine ne fait pas**

- ❌ N'appelle jamais les providers externes (rôle de `core.uslottery`).
- ❌ N'écrit jamais dans `draw_result` (rôle de `core.drawresult`).
- ❌ Ne calcule pas les odds/payouts (rôle de `catalog.pricing`).
- ❌ Ne traite pas les paiements (rôle de `core.payout`, qui réagit à `DrawSettledEvent`).
- ❌ Ne projette pas les résultats Haïti (rôle de `core.haiti`, appelé par `core.drawresult`).
- ❌ N'autorise pas la vente (rôle de `core.sales`, qui lit l'état du draw).

---

## 2. Modèle métier (agrégats / entités)

### Agrégat principal : `Draw`

- **Identité** : `DrawId` (typed wrapper sur UUID).
- **Identité métier** : `UNIQUE(tenant_id, draw_channel_id, draw_date)`.
- **JPA Entity** : `BaseTenantEntity` (RLS active).

**Champs clés** :

| Champ                                                           | Type            | Sémantique                                     |
| --------------------------------------------------------------- | --------------- | ---------------------------------------------- |
| `tenantId`                                                      | `TenantId`      | Propriétaire (RLS)                             |
| `drawChannelId`                                                 | `DrawChannelId` | Canal de vente (catalog)                       |
| `drawDate`                                                      | `LocalDate`     | Date locale du channel (timezone du slot)      |
| `scheduledAt`                                                   | `Instant`       | Moment du tirage (via `OccurredAtResolver`)    |
| `cutoffAt`                                                      | `Instant`       | Moment où la vente s'arrête                    |
| `openedAt`, `closedAt`, `resultedAt`, `settledAt`, `canceledAt` | `Instant?`      | Timestamps des transitions                     |
| `status`                                                        | `DrawStatus`    | État courant                                   |
| `drawResultId`                                                  | `DrawResultId?` | FK vers `draw_result` (set à l'apply)          |
| `resultSource`                                                  | `DrawSource`    | `AUTO` (apply automatique) ou `OPS` (override) |
| `resultOverrideReason`, `resultOverriddenAt`                    |                 | Audit override                                 |
| `cancelReason`, `canceledAt`                                    |                 | Audit annulation                               |
| `systemGenerated`                                               | `boolean`       | `true` si scheduler, `false` si manuel         |
| `locked`                                                        | `boolean`       | Verrou anti-concurrence pendant settlement     |

### Agrégat secondaire : `DrawExposure`

- **Identité métier** : `UNIQUE(tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)`.
- **JPA Entity** : `BaseTenantEntity` (RLS active).
- **Mis à jour par** : listeners du domaine `sales` via fonction SQL `increment_draw_exposure(...)` (upsert atomique avec `last_event_id` pour idempotence).
- **Lu par** : dashboards risk, règles `core.limitpolicy`.

---

## 6. Règles métier non négociables

### 6.1 Règle PROVISIONAL / FINAL (apply vs settle)

- ✅ **Apply** (`CLOSED → RESULTED`) autorisé dès `draw_result.status IN (PROVISIONAL, FINAL)`.
- ✅ **Settle** (`RESULTED → SETTLED`) autorisé **uniquement si** `draw_result.status = FINAL`.
- ✅ **Watchdog** : Si `draw_result` reste `PROVISIONAL` plus de **30 minutes** après l'apply, alerte ops via `DrawProvisionalWatchdogScheduler`.

### 6.2 Règle Override après SETTLED

- ✅ **L'override est refusé** si **au moins un** `Draw` lié est `SETTLED` (payouts émis).
- ✅ Avant `SETTLED` : override autorisé, le `Draw` est mis à jour avec le nouveau résultat et l'événement `DrawResultAppliedEvent` est republié.

### 6.3 Règle `force=true`

- ✅ `force=true` bypasse les contrôles métier non critiques (`UNIQUE`, vérifications de statut).
- ✅ `force=true` **ne bypasse jamais** : règle 6.2 (override post-settled), RLS, authentification.
- ✅ `force=true` **exige** un `reason` non vide.
- ✅ `force=true` est **systématiquement audité** via `@AuditedForceCommand`.
- ✅ `force=true` est exposé **uniquement** sur `/platform/ops/**`.

---

## 14. Nouvelles fonctionnalités (Mai 2026)

### 14.1 DrawSearchCriteria étendu ✅

**Ajouts** :

- `Integer limitPerChannel` : limite le nombre de draws retournés par channel (pour `/next`).
- `Integer lookaheadHours` : fenêtre temporelle pour rechercher les prochains draws.
- `List<String> resultSlotKeys` : filtrage par slots pour `/latest-with-results`.

**Factory methods** :

- `DrawSearchCriteria.forNext(resultSlotId, lookaheadHours, limitPerChannel)`
- `DrawSearchCriteria.forLatestWithResults(resultSlotKeys)`

### 14.2 DrawSalesGuardPort ✅

**Rôle** : Valider les opérations draw contre l'état des ventes/payouts/settlements.

**Méthodes** :

- `assertCanCancel(DrawId, boolean force)`
- `assertCanArchive(DrawId, boolean force)`
- `assertCanCorrectAppliedResult(DrawId, DrawResultId, boolean force)`

**Implémentation actuelle** : `NoOpDrawSalesGuardAdapter` (⚠️ logs warnings, aucune validation réelle).

**TODO** : Implémenter `RealDrawSalesGuardAdapter` avec vraies validations.

### 14.3 CorrectAppliedDrawResult flow ✅

**Command** : `CorrectAppliedDrawResultCommand`

**Handler** : `CorrectAppliedDrawResultCommandHandler`

**Flow** :

1. Require drawId, correctedDrawResultId, reason, idempotencyKey
2. Lock/idempotency check via ProcessedEventPort
3. Load Draw
4. `salesGuard.assertCanCorrectAppliedResult(...)`
5. Vérifier draw a déjà drawResultId
6. Vérifier correctedDrawResultId différent de previous
7. `draw.overrideResult(correctedDrawResultId, now, reason)`
8. Save draw
9. Publish `DrawResultCorrectedEvent` AFTER_COMMIT
10. DrawResultEventListener écoute et envoie `MarkDrawResultOverriddenCommand`

**Event** : `DrawResultCorrectedEvent` (ajout `previousDrawResultId`, `correctedDrawResultId`, `reason`)

**Cross-domain** : `core.drawresult` écoute l'événement et marque le previous DrawResult comme `OVERRIDDEN`.

---

## 15. TODOs spécifiques au domaine

### P0 — Sécurité

- [x] Réactiver `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` dans `DrawAdminController`.

### P1 — Conformité conventions

- [x] Migrer `DrawAdminController` vers `ApiResponse<T>`.
- [x] Refactor `createDraw` — handler retourne directement le `DrawSummary`.
- [x] Corriger `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)` dans `DrawDomainEventListener`.
- [x] Créer `GetDrawByIdQuery` ✅ (remplace GetDrawQuery obsolète).
- [x] Supprimer `GetDrawQuery` et `GetDrawHandler` obsolètes ✅.
- [x] Supprimer `GetDrawResultQuery` et `GetDrawResultQueryHandler` obsolètes ✅.
- [x] Nommer les magic numbers dans les commands (`OpenDueDrawsCommand` batchSize/lookaheadHours/lagHours).
- [x] Implémenter règle 6.2 (refus override si Draw SETTLED).
- [x] Implémenter règle 6.3 (`force=true` avec reason obligatoire + audit).
- [x] Retirer `OverrideDrawResultCommand` de `DrawAdminController` (vit dans `features.ops`).
- [x] DrawMapper : noms de variables clarifiés (jpaEntity, drawAggregate, drawStatus) ✅.
- [ ] Supprimer paramètre `source` obsolète du constructeur Draw (ligne 49).

### P2 — Architecture

- [x] Itérer sur les providers configurés dans `tch.draw.settle.providers` dans `DrawSettleScheduler`.
- [x] Ajouter listener `DrawResultIngestedEvent` (accélérateur apply) ✅.
- [x] DrawEventListener créé avec idempotency + CommandBus ✅.
- [x] Utilisation distribuée via `ShedLock` sur les schedulers.
- [ ] Implémenter `RealDrawSalesGuardAdapter` (actuellement NoOp ⚠️).

### P3 — Observabilité

- [ ] Job ops `unlock-stale-draws` (cf. règle 6.4).
- [x] Watchdog `PROVISIONAL > 30 min` (cf. règle 6.1).
- [ ] Activer Envers pour audit complet.
- [ ] Sort de `ARCHIVED` (cron archivage ? ops only ? purge ?).

## 16. Pipeline final

Le pipeline runtime supporté est `generate -> open -> close -> fetch -> apply -> settle`.

- `generate/open/close` : schedulers tenant-scoped de `core.draw`, gates `DRAW_GENERATE`, `DRAW_OPEN`, `DRAW_CLOSE`.
- `fetch` : scheduler global de `core.drawresult`, gate `RESULTS_EXTERNAL_FETCH`, lit les `result_slot` actifs.
- `apply` : scheduler tenant-scoped de `core.draw`, gate `RESULTS_EXTERNAL_APPLY`, lie les draws fermés au `draw_result`.
- `settle` : scheduler tenant-scoped de `core.draw`, gate `DRAW_SETTLE`, providers issus de `tch.draw.settle.providers`.
- `watchdog` : `DrawProvisionalWatchdogScheduler`, gate `DRAW_WATCHDOG_PROVISIONAL`, alerte les draws `RESULTED` avec résultat `PROVISIONAL` trop ancien.

---

## 17. Analysis V1 (2026-05-05) — Flow Validation

### Critical Paths Validated

✅ **Generate Draws Flow**:

- Lookahead 7 days, per tenant
- Idempotent (existing draws skipped)
- No events published (batch operation)
- Typed IDs: DrawId, TenantId, DrawChannelId

✅ **Open/Close Draws Flow**:

- Time-based transitions (SCHEDULED→OPEN, OPEN→CLOSED)
- Bulk batch operations via DrawLifecyclePort
- No events published (state transitions only)

✅ **Apply Draw Result Flow**:

- Attach global DrawResult to tenant Draw
- Transition: CLOSED → RESULTED
- Publish DrawResultAppliedEvent after commit
- Listeners: ticket settlement job, draw stats

✅ **Settle Draw Flow**:

- Prerequisite: DrawResult.status == CONFIRMED
- Transition: RESULTED → SETTLED
- Publish DrawSettledEvent after commit
- Side-effect: Payout finalization

### Architecture Compliance

- ✅ Tenant-scoped: RLS enforced via tenant_id
- ✅ Pure domain: No Spring/JPA in domain layer
- ✅ Hexagonal: Ports for lifecycle, lookup, summary reading
- ✅ CQRS: Command handlers with @TchTx
- ✅ Events: AfterCommit pattern for cross-domain effects
- ✅ Typed IDs: DrawId, TenantId, DrawChannelId, ResultSlotId

### Notes

- Pessimistic locking candidate: SettleDrawCommandHandler (see comment for FOR UPDATE optimization)
- Cache TTL configurable: application-draw.yaml for last7m, todaym, nexts
- Scheduler windows: NY timezone (fetch 12-14h, 20-23h; apply 12-14:30h, 20-23:30h; settle 12-15h, 20-23:30h)
