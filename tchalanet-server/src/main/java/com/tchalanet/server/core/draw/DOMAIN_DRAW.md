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
| `resultSource`                                                  | `ResultSource`  | `AUTO` (apply automatique) ou `OPS` (override) |
| `resultOverrideReason`, `resultOverriddenAt`                    |                 | Audit override                                 |
| `cancelReason`, `canceledAt`                                    |                 | Audit annulation                               |
| `systemGenerated`                                               | `boolean`       | `true` si scheduler, `false` si manuel         |
| `locked`                                                        | `boolean`       | Verrou anti-concurrence pendant settlement     |

### Agrégat secondaire : `DrawExposure`

- **Identité métier** : `UNIQUE(tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)`.
- **JPA Entity** : `BaseTenantEntity` (RLS active).
- **Mis à jour par** : listeners du domaine `sales` via fonction SQL `increment_draw_exposure(...)` (upsert atomique avec `last_event_id` pour idempotence).
- **Lu par** : dashboards risk, règles `core.limitpolicy`.

> ⚠️ `DrawExposure` est écrit par `sales` mais vit dans le schéma de `draw` car agrégé par draw. Exception documentée au pattern "1 domaine = 1 schéma" (cf. ARCHITECTURE.md). Justifié : compteur factuel par draw, pas un agrégat métier de sales.

### État : `DrawStatus`

```
                                                        ┌──── CANCELED
                                                        │     (ops only)
        Generate     Open       Close       Apply       Settle
SCHEDULED ───────▶ OPEN ────▶ CLOSED ────▶ RESULTED ─▶ SETTLED ────▶ ARCHIVED
                                                                       (futur)
```

| État        | Sémantique                                       | Vente possible ? |
| ----------- | ------------------------------------------------ | ---------------- |
| `SCHEDULED` | Draw matérialisé, vente pas encore ouverte       | ❌               |
| `OPEN`      | Vente autorisée                                  | ✅               |
| `CLOSED`    | Cutoff atteint, plus de vente                    | ❌               |
| `RESULTED`  | `draw_result_id` rattaché, settlement en attente | ❌               |
| `SETTLED`   | Tickets traités, payouts émis                    | ❌               |
| `CANCELED`  | Annulé par ops, refund déclenché                 | ❌               |
| `ARCHIVED`  | Réservé futur (purge / archivage froid)          | ❌               |

### Invariants métier

- `OPEN` doit avoir `opened_at IS NOT NULL`.
- `CLOSED` doit avoir `closed_at IS NOT NULL` ET `closed_at >= opened_at`.
- `RESULTED` doit avoir `draw_result_id IS NOT NULL` ET `resulted_at IS NOT NULL`.
- `SETTLED` doit avoir `settled_at IS NOT NULL` ET `draw_result.status = FINAL` au moment du settlement.
- Transitions backwards interdites (sauf `→ CANCELED`).
- `CANCELED` est terminal (pas de retour en arrière).
- `cutoff_at < scheduled_at` (cutoff avant tirage).
- `locked=true` interdit toute autre opération transitionnelle sur le draw.

> Valeur métier clé :
> Le `Draw` est le pivot de la vente, du risk et du settlement. C'est l'objet auquel se rattachent les tickets, l'exposure, et les events tenants.

---

## 3. Architecture (75-core-rules.md)

### Package structure

```
core/draw/
├── api/                                # Contrat public (read minimal)
│   └── (futur — port read-only si sales/stats en a besoin)
├── domain/                             # Modèle pur, pas de Spring/JPA
│   ├── model/
│   │   ├── Draw.java                  # Aggregate
│   │   ├── DrawStatus.java            # Enum
│   │   ├── ResultSource.java          # AUTO/OPS
│   │   ├── DrawExposure.java          # Aggregate secondaire
│   │   └── DrawSummary.java           # Projection
│   ├── event/
│   │   ├── DrawClosedEvent.java
│   │   ├── DrawResultAppliedEvent.java
│   │   ├── DrawSettledEvent.java
│   │   └── DrawCanceledEvent.java
│   └── exception/
│       └── DrawTransitionException.java
├── application/
│   ├── command/
│   │   ├── model/                     # Commands (records)
│   │   │   ├── GenerateDrawsForRangeCommand.java
│   │   │   ├── OpenDueDrawsCommand.java
│   │   │   ├── CloseDueDrawsCommand.java
│   │   │   ├── ApplyExternalResultsWindowCommand.java
│   │   │   ├── SettleDrawCommand.java
│   │   │   ├── CancelDrawCommand.java
│   │   │   ├── CreateDrawCommand.java
│   │   │   └── UpdateDrawCommand.java
│   │   └── handler/                   # @UseCase + @TchTx
│   ├── query/
│   │   ├── model/
│   │   │   ├── ListDrawsQuery.java
│   │   │   ├── GetDrawByIdQuery.java
│   │   │   └── GetDrawBySlotAndDateQuery.java
│   │   └── handler/
│   └── port/
│       └── out/
│           ├── DrawWriterPort.java
│           ├── DrawReaderPort.java
│           ├── DrawResultReaderPort.java        # consumed from core.drawresult
│           └── TenantDrawCalendarQueryPort.java
└── infra/
    ├── persistence/
    │   ├── DrawJpaEntity.java
    │   ├── DrawJpaRepository.java
    │   ├── DrawExposureJpaEntity.java
    │   └── DrawExposureJpaRepository.java
    ├── event/
    │   └── DrawDomainEventListener.java          # @TransactionalEventListener(AFTER_COMMIT)
    ├── batch/
    │   ├── scheduler/
    │   │   ├── DrawLifeCycleTickScheduler.java   # Generate/Open/Close
    │   │   └── DrawSettleScheduler.java          # Spring Batch
    │   └── job/
    │       └── DrawSettleJobConfig.java
    ├── scheduler/
    │   └── ExternalResultsApplyTickScheduler.java
    └── web/
        ├── DrawAdminController.java              # /admin/draws
        ├── mapper/DrawAdminWebMapper.java
        └── model/                                # DTOs Request/Response
```

### Règles d'isolation

- ✅ `domain/` = pur (pas de Spring, pas de JPA, pas de Jackson).
- ✅ `application/` = handlers + ports out (interfaces).
- ✅ `infra/` = adapters (JPA, Spring Batch, scheduling, web).
- ✅ Aucune dépendance de `core.drawresult` ou `core.uslottery` vers `core.draw`.
- ✅ Aucune écriture cross-domain dans la même transaction (events after-commit).

---

## 4. API Publique (Commands & Queries via Bus)

### Commands (write — `CommandBus.send(...)`)

| Command                             | Trigger                              | Effet                                            | Idempotence                                         |
| ----------------------------------- | ------------------------------------ | ------------------------------------------------ | --------------------------------------------------- |
| `GenerateDrawsForRangeCommand`      | scheduler `generateNext7Days` ou ops | Crée les `Draw` manquants pour `(tenant, range)` | `UNIQUE(tenant, channel, date)`                     |
| `OpenDueDrawsCommand`               | scheduler `openWindowed` ou ops      | `SCHEDULED → OPEN` pour les draws dus            | Filtre `status='SCHEDULED'`                         |
| `CloseDueDrawsCommand`              | scheduler `closeWindowed` ou ops     | `OPEN → CLOSED` au cutoff                        | Filtre `status='OPEN'`                              |
| `ApplyExternalResultsWindowCommand` | scheduler `applyTick` ou ops         | `CLOSED → RESULTED` (lie au `draw_result`)       | Filtre `status='CLOSED' AND draw_result_id IS NULL` |
| `SettleDrawCommand`                 | batch `DRAW_SETTLE`                  | `RESULTED → SETTLED` (settlement tickets)        | `processed_event` + `draw.locked`                   |
| `CancelDrawCommand`                 | ops uniquement                       | `* → CANCELED` (refund)                          | Filtre `status NOT IN (CANCELED, SETTLED)`          |
| `CreateDrawCommand`                 | admin (manuel, exception)            | Insertion directe d'un draw                      | `UNIQUE(tenant, channel, date)`                     |
| `UpdateDrawCommand`                 | admin (correction)                   | Patch champs autorisés                           | Verrou optimiste (`version`)                        |

**Spec compliance** : D1 (write commands).

### Queries (read — `QueryBus.send(...)`)

| Query                         | Usage                                                                   |
| ----------------------------- | ----------------------------------------------------------------------- |
| `ListDrawsQuery`              | Liste paginée pour admin/ops                                            |
| `GetDrawByIdQuery`            | Lookup direct (à créer — TODO P1)                                       |
| `GetDrawBySlotAndDateQuery`   | Pour `sales` qui veut savoir si un draw OPEN existe (à créer — TODO P1) |
| `ListDrawsForSettlementQuery` | Batch settle : liste des draws RESULTED non settled                     |

**Spec compliance** : D2 (read queries).

### REST Controllers

#### `DrawAdminController` — `/admin/draws`

| Method | Endpoint            | HTTP | Returns                                  |
| ------ | ------------------- | ---- | ---------------------------------------- |
| GET    | `/admin/draws`      | 200  | `ApiResponse<List<DrawSummaryResponse>>` |
| POST   | `/admin/draws`      | 201  | `ApiResponse<DrawSummaryResponse>`       |
| PUT    | `/admin/draws/{id}` | 200  | `ApiResponse<DrawSummaryResponse>`       |

- ✅ `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`
- ✅ Returns `ApiResponse<T>`
- ✅ Error handling via `ProblemDetail`

> ⚠️ L'override de résultat **n'appartient pas** à ce controller. Il vit dans `features.ops.DrawResultsOpsController` et route vers `core.drawresult` via `OverrideDrawResultCommand`.

**Spec compliance** : D3 (admin endpoints) + `web_api.md` + `api_response.md`.

---

## 5. Events publiés

> Tous les events sont publiés via `AfterCommit.run(...)` dans le command handler.
> Tous les listeners cross-domain utilisent `@TransactionalEventListener(phase = AFTER_COMMIT)` et sont idempotents (cf. `idempotency.md` § Event Idempotence).

| Event                    | Quand                   | Tenanté | Consumers                                                            |
| ------------------------ | ----------------------- | ------- | -------------------------------------------------------------------- |
| `DrawClosedEvent`        | Draw passe à `CLOSED`   | ✅      | `sales` (refuse vente), `cache` (refresh)                            |
| `DrawResultAppliedEvent` | Draw passe à `RESULTED` | ✅      | `sales` (déclenche settlement), `stats`, `cache`                     |
| `DrawSettledEvent`       | Draw passe à `SETTLED`  | ✅      | `stats` (projection finale), `notifications`, `cache`, `core.payout` |
| `DrawCanceledEvent`      | Draw passe à `CANCELED` | ✅      | `sales` (refund), `stats`, `notifications`                           |

### Events consommés

| Event                     | Source            | Action                                                                                                                      |
| ------------------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `DrawResultIngestedEvent` | `core.drawresult` | Optionnel : déclenche un `ApplyExternalResultsWindowCommand` ciblé pour accélérer (le tick polling reste filet de sécurité) |

**Spec compliance** : D4 (events) + `event_model.md`.

---

## 6. Règles métier non négociables

### 6.1 Règle PROVISIONAL / FINAL (apply vs settle)

- ✅ **Apply** (`CLOSED → RESULTED`) autorisé dès `draw_result.status IN (PROVISIONAL, FINAL)`.
- ✅ **Settle** (`RESULTED → SETTLED`) autorisé **uniquement si** `draw_result.status = FINAL`.
- ⏸️ Si `draw_result` reste `PROVISIONAL` plus de **30 minutes** après l'apply, alerte ops (TODO P3).

**Rationale** : on bloque la vente le plus tôt possible (apply rapide) sans payer sur un résultat encore corrigeable.

### 6.2 Règle Override après SETTLED

- ❌ `OverrideDrawResultCommand` est **refusé** si **au moins un** `Draw` lié est `SETTLED`.
- ✅ Avant `SETTLED` : override autorisé, le `Draw` revient en `RESULTED` avec le nouveau résultat.
- 🚧 Cas "correction provider post-settlement" → traité **manuellement** par les ops (escalade, hors automatisation).

### 6.3 Règle `force=true`

- `force=true` bypasse les contrôles métier non critiques (`UNIQUE`, vérifications de statut).
- `force=true` **ne bypasse jamais** : règle 6.2 (override post-settled), RLS, authentification.
- `force=true` **exige** un `reason` non vide.
- `force=true` est **systématiquement audité** (`audit_log` avec actor + reason + before/after).
- `force=true` est exposé **uniquement** sur `/platform/ops/**`.

### 6.4 Règle `locked` (settlement)

- Posé par le batch `DRAW_SETTLE` au début du traitement.
- Retiré en fin de traitement (succès ou rollback).
- Un job qui trouve `locked=true` skip le draw.
- TODO P3 : job ops `unlock-stale-draws` (timeout 15 min).

### 6.5 Règle d'archivage (`ARCHIVED`)

- Non automatisé en MVP. Réservé pour cron de purge future.

**Spec compliance** : D5 (business rules).

---

## 7. Schedulers

| Scheduler                                      | Cron                           | Mode         | Commande envoyée                    |
| ---------------------------------------------- | ------------------------------ | ------------ | ----------------------------------- |
| `DrawLifeCycleTickScheduler.generateNext7Days` | `0 0 5 * * UTC` daily          | CommandBus   | `GenerateDrawsForRangeCommand`      |
| `DrawLifeCycleTickScheduler.openWindowed`      | `0 */30 * * * *`               | CommandBus   | `OpenDueDrawsCommand`               |
| `DrawLifeCycleTickScheduler.closeWindowed`     | `0 */15 * * * *`               | CommandBus   | `CloseDueDrawsCommand`              |
| `ExternalResultsApplyTickScheduler`            | `30 */5 * * * *` (offset +30s) | CommandBus   | `ApplyExternalResultsWindowCommand` |
| `DrawSettleScheduler`                          | `0 */5 * * * NY`               | Spring Batch | Job `DRAW_SETTLE`                   |

> **Pattern** : lifecycle court → CommandBus ; traitement de masse → Spring Batch.

Chaque scheduler vérifie sa `BatchGate` correspondante avant exécution. Voir `batch.md`.

**Spec compliance** : D6 (scheduling).

---

## 8. Cache

| Cache name                            | TTL   | Éviction                                        |
| ------------------------------------- | ----- | ----------------------------------------------- |
| `core.draw.latest::{tenant}`          | 30s   | sur `DrawClosedEvent`, `DrawResultAppliedEvent` |
| `core.draw.by_id::{drawId}`           | 5 min | sur `DrawSettledEvent`, `DrawCanceledEvent`     |
| `core.draw.calendar::{tenant}_{date}` | 5 min | sur transitions OPEN/CLOSED                     |

**Spec compliance** : D7 + `cache.md`.

---

## 9. Persistence

- Table `draw` — `BaseTenantEntity`, RLS active.
- Table `draw_exposure` — `BaseTenantEntity`, RLS active, mise à jour via fonction SQL `increment_draw_exposure`.
- Indexes partiels critiques :
  - `ix_draw_open_due` (filter `status='SCHEDULED' AND locked=false`)
  - `ix_draw_close_due` (filter `status='OPEN' AND locked=false`)
  - `ix_draw_closed_missing_result` (filter pour apply)
  - `ix_draw_resulted_to_settle` (filter pour settle)
- Audit : Envers (à activer — TODO P3).
- Migrations : Flyway, `ddl-auto=validate`.

Voir `jpa_entities.md`, `persistence.md`, `rls.md`.

---

## 10. Dépendances

**Dépend de** :

- `common.types.id` (`TenantId`, `DrawId`, `DrawChannelId`, `DrawResultId`)
- `common.types.BaseTenantEntity` (RLS + audit)
- `common.bus` (`CommandBus`, `QueryBus`)
- `common.tx.AfterCommit`
- `common.event.DomainEventPublisher`
- `common.idempotency.event.ProcessedEventPort`
- `catalog.drawchannel.DrawChannelCatalog` (lecture)
- `catalog.game.GameCatalog` (lecture)
- `catalog.tenant.TenantCatalog` (liste tenants actifs)
- `core.drawresult.DrawResultReaderPort` (read-only port consommé)

**Utilisé par** :

- `core.sales` (settlement via events)
- `core.payout` (réagit à `DrawSettledEvent`)
- `features.publicdraw` (exposition publique)
- `features.stats` (projections)
- `features.ops` (orchestration manuelle)

---

## 11. Notes techniques

### Typed IDs (`typed_ids.md`)

- ✅ `DrawId`, `TenantId`, `DrawChannelId`, `DrawResultId` wrappers.
- ✅ Controllers reçoivent UUID, convertis via Spring converter.
- ✅ Aucune fuite UUID brut hors persistence.

### Mapping (`api_response.md`, `web_api.md`)

- ✅ DTOs web (`DrawSummaryResponse`, `CreateDrawRequest`, `UpdateDrawRequest`) immuables.
- ✅ Mappers MapStruct (`DrawAdminWebMapper`).
- ✅ Controllers retournent `ApiResponse<T>`.
- ✅ Aucune entité JPA exposée.

### Transactions (`command_query_handlers.md`)

- ✅ Tous les command handlers sont annotés `@TchTx`.
- ✅ Events publiés via `AfterCommit.run(...)`.
- ✅ Aucune écriture cross-domain dans la même transaction.

### RLS (Row-Level Security)

- ✅ `draw` et `draw_exposure` sont tenant-scoped.
- ✅ Policies RLS appliquent l'isolation, le code n'ajoute jamais `WHERE tenant_id = ?`.

### Idempotence (`idempotency.md`)

- HTTP : `Idempotency-Key` non requis sur les endpoints draws (transitions protégées par state machine).
- Events : `ProcessedEventPort` avec `handler_key` stable (ex: `draw.settle`, `draw.apply`).

---

## 12. Séparation des responsabilités

### `core.draw` (lifecycle tenant + risk)

✅ Gère le lifecycle complet du `Draw` tenant.
✅ Maintient `draw_exposure` (compteurs risk).
✅ Publie 4 events tenants.
✅ Applique RLS.

### `core.drawresult` (ingestion globale)

✅ Ingère les résultats externes (global, pas de tenant).
✅ Persiste `draw_result` avec statut PROVISIONAL/FINAL/ERROR.
✅ Expose `DrawResultReaderPort` (read-only) à `core.draw`.

### `core.uslottery` (clients providers)

✅ Parle aux APIs HTTP NY/FL/GA/TX/TN.
✅ Cache provider raw.
✅ Aucune persistance, aucun event.

### `core.haiti` (projection métier)

✅ Projecteur pur (`HaitiResultProjector`).
✅ Appelé par `core.drawresult` pendant le fetch.

### `features.ops`

✅ Orchestration manuelle (fetch / refresh / override / manual / generate / open / close / apply).
✅ Aucune logique métier, route vers les commands.

---

## 13. Patterns appliqués

- ✅ **CQRS** (`command_query_handlers.md`)
- ✅ **Typed IDs** (`typed_ids.md`)
- ✅ **API Response** (`api_response.md`, `web_api.md`)
- ✅ **Cache Strategy** (`cache.md`)
- ✅ **Event Model after-commit** (`event_model.md`)
- ✅ **Idempotence à 2 couches** (`idempotency.md`)
- ✅ **RLS systématique** (`rls.md`)
- ✅ **Batch lifecycle vs masse** (`batch.md`) — CommandBus pour les transitions courtes, Spring Batch pour le settlement.

---

## 14. Conformité aux specs

| Spec         | Requirement                  | Status                                                          |
| ------------ | ---------------------------- | --------------------------------------------------------------- |
| D1           | Write commands (8 commandes) | ✅ Generate, Open, Close, Apply, Settle, Cancel, Create, Update |
| D2           | Read queries                 | ✅ List + à compléter (P1)                                      |
| D3           | Admin endpoints              | ⚠️ Migrer vers `ApiResponse<T>` (P1)                            |
| D4           | Events publiés (4)           | ⚠️ Listener pas en `AFTER_COMMIT` (P1)                          |
| D5           | Business rules               | ⚠️ Override post-SETTLED à implémenter (P1)                     |
| D6           | Schedulers (5)               | ✅                                                              |
| D7           | Cache                        | ✅                                                              |
| event_model  | After-commit + idempotence   | ⚠️ Listener à corriger (P1)                                     |
| api_response | `ApiResponse<T>` partout     | ⚠️ Migration en cours (P1)                                      |
| security     | `@PreAuthorize` partout      | ❌ Réactiver (P0)                                               |

---

## 15. TODOs spécifiques au domaine

### P0 — Sécurité

- [ ] Réactiver `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` dans `DrawAdminController`.

### P1 — Conformité conventions

- [ ] Migrer `DrawAdminController` vers `ApiResponse<T>`.
- [ ] Refactor `createDraw` — handler retourne directement le `DrawSummary` (supprimer le pattern "send command + filter list").
- [ ] Corriger `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)` dans `DrawDomainEventListener`.
- [ ] Créer `GetDrawByIdQuery` et `GetDrawBySlotAndDateQuery`.
- [ ] Nommer les magic numbers dans les commands (`OpenDueDrawsCommand` 5000/24/12 → champs typés).
- [ ] Implémenter règle 6.2 (refus override si Draw SETTLED).
- [ ] Implémenter règle 6.3 (`force=true` avec reason obligatoire + audit).
- [ ] Retirer `OverrideDrawResultCommand` de `DrawAdminController` (vit dans `features.ops`).

### P2 — Architecture

- [ ] Itérer sur tous les providers actifs dans `DrawSettleScheduler` (au lieu de NY+FL hardcoded).
- [ ] Ajouter listener `DrawResultIngestedEvent` (accélérateur apply).

### P3 — Observabilité

- [ ] Job ops `unlock-stale-draws` (cf. règle 6.4).
- [ ] Alerte ops si `PROVISIONAL > 30 min` (cf. règle 6.1).
- [ ] Activer Envers pour audit complet.
- [ ] Sort de `ARCHIVED` (cron archivage ? ops only ? purge ?).

---

> **Source of truth** : ce document est la référence backend pour le domaine `core.draw`.
> Pour la vue cross-apps (Web/Mobile/API), voir `tchalanet-docs/docs/02-functional/domains/draw.md`.
> Pour le pipeline complet inter-domaines, voir `tchalanet-docs/docs/02-functional/flows/draw-execution.md`.
