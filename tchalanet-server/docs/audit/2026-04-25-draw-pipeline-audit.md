# Audit Pipeline Draw — État des lieux

**Date** : 2026-04-25
**Scope** : `catalog.game`, `catalog.resultslot`, `catalog.drawchannel`, `core.drawresult`, `core.draw` + domaines connexes (`core.uslottery`, `core.haiti`, `features.publicdraw`, `features.pagemodel.dynamic.providers.DrawsProvider`, `features.ops`)
**Méthode** : lecture code source réel uniquement — `DOMAIN_*.md` non pris en compte comme référence

---

## Domaine 1 : `catalog.game` (`com.tchalanet.server.catalog.game`)

### 1. Structure de packages

```
catalog/game/
├── api/
│   ├── GameCatalog.java          (interface read-only publique)
│   ├── GameView.java             (projection complète — record)
│   ├── GameStatsView.java        (stats globales)
│   └── GameSummaryView.java      (projection légère)
└── internal/
    ├── write/
    │   └── GameAdminService.java  (CRUD + cache eviction)
    ├── persistence/
    │   ├── GameJpaEntity.java
    │   └── GameJpaRepository.java
    ├── cache/
    │   └── GameCacheNames.java
    ├── mapper/
    │   └── GameMapper.java
    └── web/
        └── GameAdminController.java
```

Séparation `api/` vs `internal/` : présente et conforme au pattern catalog.

### 2. Entités JPA et tables

**`GameJpaEntity`** (`src/main/java/com/tchalanet/server/catalog/game/internal/persistence/GameJpaEntity.java`)

- Table : `game`
- Hérite de : `BaseEntity` (global, pas de tenant)
- UNIQUE : `uq_game_code` sur `code`
- Champs métier :
  - `code` (VARCHAR 32, NOT NULL, UNIQUE)
  - `name` (VARCHAR 128, NOT NULL)
  - `category` (VARCHAR 32, NOT NULL — commentaire dans le code : `HAITI`)
  - `combination` (VARCHAR 32, NOT NULL)
  - `minDigits`, `maxDigits` (int, NOT NULL)
  - `description` (nullable)
  - `active` (boolean, NOT NULL, default `true`)
  - `sortOrder` (int, NOT NULL, default `0`)
- `@Audited` : oui (Envers)

Pas d'enum de statut. Pas de machine à états.

### 3. Commands existantes

Pas de CommandBus dans `catalog.game`. Le write passe directement par `GameAdminService` depuis le controller.

- **create** : `GameAdminService.create(GameCreateRequest)` → `@Transactional` + `@CacheEvict` (3 caches)
- **update** : `GameAdminService.update(GameId, GameUpdateRequest)` → patch partiel (champs non-null)
- **deactivate** : `GameAdminService.deactivate(GameId)` → `active = false`
- **softDelete** : `GameAdminService.softDelete(GameId)` → `deletedAt = Instant.now()` + `active = false`

### 4. Queries existantes

Via `GameCatalog` (interface `api/`) :

- `listActive()` : tous avec `active=true AND deletedAt IS NULL`
- `findByCode(String)` : par code fonctionnel (filtre `deletedAt IS NULL`, retourne inactif)
- `findById(GameId)` : par ID (filtre `deletedAt IS NULL`)
- `stats()` : `GameStatsView` (count total + count actifs)
- `listRecent(int limit)` : top 10 par `updatedAt DESC`

Repository Spring Data :

- `findByActiveTrueAndDeletedAtIsNull()`
- `findByIdAndDeletedAtIsNull(UUID)`
- `findByCodeAndDeletedAtIsNull(String)`
- `findTop10ByDeletedAtIsNullOrderByUpdatedAtDesc()`

### 5. Ports (in/out)

- Port IN exposé : `GameCatalog` dans `api/` — consommé par `core.draw` (via `DrawChannelCatalog`)
- Pas de port OUT (aucune dépendance externe)

### 6. Schedulers / Batchs

Aucun.

### 7. Events

Aucun domain event publié (conforme règle catalog).

### 8. Controllers HTTP

**`GameAdminController`** (`src/main/java/com/tchalanet/server/catalog/game/internal/web/GameAdminController.java`)

- Path : `/platform/games`
- `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` : **actif**
- Retourne `ApiResponse<T>` : **oui** sur tous les endpoints
- Endpoints : `POST /`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/deactivate`

### 9. Cache

Noms de cache dans `GameCacheNames` :

- `catalog:game:active_games`
- `catalog:game:game_by_code`
- `catalog:game:game_by_id`

Éviction : `@CacheEvict(allEntries = true)` sur les 3 noms simultanément après chaque write.

### 10. Dépendances cross-domain

Uniquement `catalog.game.*` + `common.*` dans le controller. Conforme.

### 11. Anomalies détectées

- `GameAdminController.update()` reçoit un `UUID` brut (`@PathVariable UUID id`) converti via `GameId.of(id)` directement dans le controller — le typed ID wrapper aurait dû être dans la signature de la méthode.
- Le champ `category` n'est pas un enum mais un `String` avec commentaire `// HAITI`. Absence de contrainte CHECK en JPA.

---

## Domaine 2 : `catalog.resultslot` (`com.tchalanet.server.catalog.resultslot`)

### 1. Structure de packages

```
catalog/resultslot/
├── api/
│   ├── ResultSlotCatalog.java
│   ├── ResultSlotView.java
│   └── ResultSlotStatsView.java
└── internal/
    ├── write/
    │   └── ResultSlotAdminService.java
    ├── persistence/
    │   ├── ResultSlotJpaEntity.java
    │   └── ResultSlotJpaRepository.java
    ├── cache/
    │   └── ResultSlotCacheNames.java
    ├── mapper/
    │   └── ResultSlotMapper.java
    └── web/
        ├── model/
        │   ├── BaseResultSlotRequest.java
        │   ├── CreateResultSlotRequest.java
        │   └── UpdateResultSlotRequest.java
        └── ResultSlotAdminController.java
```

Séparation `api/` vs `internal/` : présente.

### 2. Entités JPA et tables

**`ResultSlotJpaEntity`** (`src/main/java/com/tchalanet/server/catalog/resultslot/internal/persistence/ResultSlotJpaEntity.java`)

- Table : `result_slot`
- Hérite de : `BaseEntity` (global, pas de tenant)
- UNIQUE : `slotKey` avec `unique = true` dans `@Column` (pas de `@UniqueConstraint` nommée)
- Champs métier :
  - `slotKey` (VARCHAR 32, NOT NULL, UNIQUE) — ex: `NY_MID`, `FL_EVE`
  - `provider` (VARCHAR 16, NOT NULL) — ex: `NY`, `FL`, `GA`, `TX`, `TN`
  - `timezone` (VARCHAR 64, NOT NULL)
  - `drawTime` (LocalTime, NOT NULL)
  - `daysOfWeek` (VARCHAR 32, NOT NULL)
  - `active` (boolean, NOT NULL, default `true`)
  - `sortOrder` (int, NOT NULL, default `0`)
  - `sourceCfg` (JSONB, NOT NULL)
  - `projectionCfg` (JSONB, NOT NULL) — contient les tokens Haiti (LOT1..LOT4)
  - `notes` (nullable)
  - `labelKey` (VARCHAR 256, nullable)
- `@Audited` : oui (Envers)

### 3. Commands existantes

Pas de CommandBus. Writes via `ResultSlotAdminService` depuis le controller.

### 4. Queries existantes

Via `ResultSlotCatalog` :

- `listActive()` : liste tous les slots actifs
- `findByKey(String slotKey)` : par clé fonctionnelle (`NY_MID`, etc.)
- `requireByKey(String slotKey)` : idem mais lève exception si absent
- `findById(ResultSlotId)`
- `stats()` : `ResultSlotStatsView`

### 5. Ports (in/out)

Port IN exposé : `ResultSlotCatalog` dans `api/` — consommé par :

- `core.drawresult.infra.scheduler.ExternalResultsFetchTickScheduler`
- `core.draw.infra.scheduler.ExternalResultsApplyTickScheduler`
- `core.drawresult.application.command.handler.FetchExternalResultsWindowCommandHandler`
- `core.drawresult.application.command.handler.OverrideDrawResultCommandHandler`
- `core.drawresult.application.command.handler.RecordManualDrawResultCommandHandler`
- `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler`

### 6. Schedulers / Batchs

Aucun.

### 7. Events

Aucun domain event (conforme catalog).

### 8. Controllers HTTP

**`ResultSlotAdminController`** (`src/main/java/com/tchalanet/server/catalog/resultslot/internal/web/ResultSlotAdminController.java`)

- Path : `/platform/result-slots`
- `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` : **actif**
- Retourne `ApiResponse<T>` : **oui** sur tous les endpoints
- Endpoints : `GET /active`, `GET /by-key/{slotKey}`, `POST /`, `PUT /{id}`, `DELETE /{id}`

### 9. Cache

AMBIGU : aucun fichier de noms de cache observé directement dans `catalog.resultslot`. `@Cacheable` présumé sur `listActive()` mais non confirmé à la lecture.

### 10. Dépendances cross-domain

`catalog.resultslot.api.*`, `catalog.resultslot.internal.*`, `common.*` uniquement. Conforme.

### 11. Anomalies détectées

- `ResultSlotView.timezone` est de type `ZoneId` — la valeur vient d'une `String` en DB, conversion via mapper non lue directement.
- Le champ `daysOfWeek` dans l'entité est stocké en `String` (format non spécifié — probablement `MON,TUE,...`). Pas d'enum. Pas de contrainte de validité visible au niveau JPA.

---

## Domaine 3 : `catalog.drawchannel` (`com.tchalanet.server.catalog.drawchannel`)

### 1. Structure de packages

```
catalog/drawchannel/
├── api/
│   ├── DrawChannelCatalog.java
│   └── model/
│       ├── DrawChannelView.java
│       ├── DrawChannelSummaryView.java
│       ├── DrawChannelGameView.java
│       ├── ChannelGamesView.java
│       ├── DrawChannelCalendarRow.java
│       └── DrawChannelSearchCriteria.java
└── internal/
    ├── write/
    │   └── DrawChannelAdminService.java
    ├── persistence/
    │   └── DrawChannelEntity.java
    ├── cache/
    │   └── DrawChannelCacheNames.java
    ├── mapper/
    │   └── DrawChannelMapper.java
    └── web/
        └── model/
            ├── CreateDrawChannelRequest.java
            └── UpdateDrawChannelRequest.java
```

Séparation `api/` vs `internal/` : présente.

AMBIGU : le controller HTTP correspondant à `DrawChannelAdminService` n'a pas été localisé directement. Un controller existe probablement dans `internal/web/` mais son nom exact est inconnu.

### 2. Entités JPA et tables

**`DrawChannelEntity`** (`src/main/java/com/tchalanet/server/catalog/drawchannel/internal/persistence/DrawChannelEntity.java`)

- Table : `draw_channel`
- Hérite de : `BaseTenantEntity` (tenant-scoped, RLS)
- Champs métier :
  - `code` (VARCHAR 64, NOT NULL)
  - `name` (VARCHAR 128, NOT NULL)
  - `timezone` (VARCHAR 64, NOT NULL)
  - `drawTime` (LocalTime, NOT NULL)
  - `cutoffSec` (int, NOT NULL, default `120`)
  - `daysOfWeek` (VARCHAR 32, NOT NULL)
  - `active` (boolean, NOT NULL, default `true`)
  - `sortOrder` (int, NOT NULL, default `0`)
  - `flags` (JSONB, NOT NULL)
  - `notes` (nullable)
  - `resultSlotId` (UUID, NOT NULL — FK vers `result_slot.id`)
- `@Audited` : oui (Envers)
- **Absence de `@UniqueConstraint` nommée dans la JPA entity pour `(tenantId, code)`** — peut exister uniquement en migration Flyway.

Note : `DrawJpaEntity` dans `core.draw.infra.persistence` référence `DrawChannelJpaEntity` via `@ManyToOne`. AMBIGU : `DrawChannelJpaEntity` n'a pas été trouvée dans `core.draw.infra.persistence` — soit c'est l'entité `DrawChannelEntity` de `catalog.drawchannel.internal.persistence`, soit une entité distincte dans `core.draw`.

### 3. Commands existantes

Pas de CommandBus. Writes via `DrawChannelAdminService` :

- `create(DrawChannelEntity)` / `createFromRequest(CreateDrawChannelRequest)` / `createFromView(DrawChannelView)` — 3 surcharges
- `update(UUID, DrawChannelEntity)` / `updateFromRequest(DrawChannelId, UpdateDrawChannelRequest)` / `updateFromView(DrawChannelId, DrawChannelView)` — 3 surcharges
- `updateFlagsFromRequest(DrawChannelId, UpdateDrawChannelFlagsRequest)`
- `softDelete(DrawChannelId)`

### 4. Queries existantes

Via `DrawChannelCatalog` :

- `listAll(TenantId, Boolean activeOnly)`
- `findById(TenantId, DrawChannelId)`
- `findByTenantAndCode(TenantId, String code)`
- `listGamesByChannel(TenantId, DrawChannelId)`
- `listChannelGames(TenantId)`
- `listCalendarRows(TenantId, Boolean activeOnly, Boolean enabledOnly)`
- `search(DrawChannelSearchCriteria, TchPageRequest)`

### 5. Ports (in/out)

Port IN exposé : `DrawChannelCatalog` — consommé par :

- `core.draw.application.command.handler.GenerateDrawsForRangeCommandHandler`
- `core.draw.application.command.handler.CreateDrawCommandHandler`
- `core.draw.infra.web.DrawAdminController`

### 6. Schedulers / Batchs

Aucun.

### 7. Events

Aucun domain event (conforme catalog).

### 8. Controllers HTTP

AMBIGU : controller non localisé directement.

### 9. Cache

Noms de cache dans `DrawChannelCacheNames` :

- `catalog:drawchannel:by_tenant`
- `catalog:drawchannel:by_id`
- `catalog:drawchannel:by_tenant_game_map`
- `catalog:drawchannel:calendar_rows`
- `catalog:drawchannel:by_tenant_by_result_slot_id`
- `catalog:drawchannel:by_tenant_by_result_slot_provider_key`

Éviction : `@CacheEvict(allEntries = true)` sur 5 caches simultanément dans `DrawChannelAdminService`.

### 10. Dépendances cross-domain

- `DrawChannelView` (`api/model/`) importe `com.tchalanet.server.core.drawresult.domain.model.DrawSource` — **import cross-domain depuis `catalog.*` vers `core.*.domain.model.*`**.
- `CreateDrawChannelRequest` importe aussi `DrawSource`. Double violation.

### 11. Anomalies détectées

- **Import `core.drawresult.domain.model.DrawSource` depuis `catalog.drawchannel`** : le catalog dépend d'un type du core. `DrawSource` devrait vivre dans `common/` ou dans un contrat partagé.
- `DrawChannelEntity` n'a pas de contrainte UNIQUE annotée en JPA pour `(tenantId, code)`.
- 3 variantes de `create` et `update` dans `DrawChannelAdminService` — surchargé, non unifié.
- `DrawChannelAdminService.update()` prend un `UUID` brut au lieu d'un `DrawChannelId`.

---

## Domaine 4 : `core.drawresult` (`com.tchalanet.server.core.drawresult`)

### 1. Structure de packages

```
core/drawresult/
├── domain/
│   ├── model/
│   │   ├── DrawResult.java           (record — agrégat)
│   │   ├── DrawResultRef.java
│   │   ├── DrawResultStatus.java     (enum)
│   │   └── DrawSource.java           (enum)
│   └── event/
│       └── DrawResultedAppliedEvent.java
├── application/
│   ├── command/
│   │   ├── model/
│   │   │   ├── FetchExternalResultsWindowCommand.java
│   │   │   ├── RefreshExternalResultsWindowCommand.java
│   │   │   ├── OverrideDrawResultCommand.java
│   │   │   └── RecordManualDrawResultCommand.java
│   │   └── handler/
│   │       ├── FetchExternalResultsWindowCommandHandler.java
│   │       ├── RefreshExternalResultsWindowCommandHandler.java
│   │       ├── OverrideDrawResultCommandHandler.java
│   │       └── RecordManualDrawResultCommandHandler.java
│   ├── query/
│   │   ├── model/
│   │   │   └── ListDrawResultsQuery.java
│   │   └── handler/
│   │       └── ListDrawResultsQueryHandler.java
│   ├── port/
│   │   └── out/
│   │       ├── DrawResultReaderPort.java
│   │       ├── DrawResultWriterPort.java
│   │       ├── ExternalResultsFetchPort.java
│   │       ├── ExternalDrawResultPort.java
│   │       ├── HaitiProjectionConfigPort.java
│   │       └── DrawResultsCriteria.java
│   └── service/
│       └── ResultSlotTimes.java
├── infra/
│   ├── persistence/
│   │   ├── DrawResultJpaEntity.java
│   │   ├── mapper/
│   │   │   └── DrawResultMapper.java
│   │   ├── adapter/
│   │   │   ├── DefaultHaitiProjectionConfigAdapter.java
│   │   │   ├── DrawResultJdbcReaderAdapter.java
│   │   │   └── DrawResultWriterJdbcAdapter.java
│   │   └── repo/
│   │       ├── DrawResultJdbcRepository.java
│   │       └── DrawResultJpaRepository.java
│   ├── scheduler/
│   │   └── ExternalResultsFetchTickScheduler.java
│   ├── config/
│   │   └── DrawResultsProperties.java
│   ├── util/
│   │   ├── ExternalPickMapper.java
│   │   └── SourceResultBuilder.java
│   ├── cache/
│   │   └── DrawResultCacheEvictor.java
│   └── web/
│       ├── DrawResultsController.java
│       ├── mapper/
│       │   └── DrawResultWebMapper.java
│       └── model/
│           ├── DrawResultResponse.java
│           ├── OverrideDrawResultRequest.java
│           └── RecordManualDrawResultRequest.java
```

Absence d'un dossier `api/` : les ports OUT de `application.port.out` sont consommés directement depuis `core.draw` sans isolation via `api/`.

### 2. Entités JPA et tables

**`DrawResultJpaEntity`** (`src/main/java/com/tchalanet/server/core/drawresult/infra/persistence/DrawResultJpaEntity.java`)

- Table : `draw_result`
- Hérite de : `BaseEntity` (global, **pas de tenant, pas de RLS**)
- Index : `ix_draw_result_status` sur `status`
- **Absence de `@UniqueConstraint` sur `(result_slot_id, occurred_at)`** dans l'annotation JPA — contrainte `uq_draw_result_slot_time` existe probablement uniquement en migration Flyway.
- Champs métier :
  - `resultSlotId` (UUID, NOT NULL)
  - `occurredAt` (Instant, NOT NULL)
  - `sourceResult` (JSONB, NOT NULL)
  - `haitiResult` (JSONB, NOT NULL)
  - `rawPayload` (JSONB, nullable)
  - `flags` (JSONB, NOT NULL)
  - `status` (VARCHAR 16, NOT NULL)
  - `quality` (VARCHAR 16)
  - `source` (VARCHAR 32)
  - `sourceHash` (VARCHAR 64, nullable)
  - `fetchedAt` (Instant, NOT NULL)
  - `overrideReason` (nullable)
- `@Audited` : oui (Envers)

**Enum `DrawResultStatus`** :

```
VALID, OVERRIDDEN, PROVISIONAL, FINAL, ERROR, INVALIDATED
```

AMBIGU : `VALID`, `OVERRIDDEN`, `INVALIDATED` sont présents dans l'enum mais sans usage visible dans le code principal et sans documentation.

**Enum `DrawSource`** :

```
SYSTEM, EXTERNAL, US_LOTTERY, NY_OPEN_DATA, FL_APIM, MANUAL, ADMIN_OVERRIDE
```

### 3. Commands existantes

| Command                               | Paramètres principaux                                                               | Handler                                      | Action                                                                                                             |
| ------------------------------------- | ----------------------------------------------------------------------------------- | -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `FetchExternalResultsWindowCommand`   | `tenantId?`, `baseDate`, `daysBack`, `slotKeys`, `force`, `dryRun`, `maxSlots`      | `FetchExternalResultsWindowCommandHandler`   | Fetch provider → projection Haiti → upsert `draw_result` avec status `"PROVISIONAL"`                               |
| `RefreshExternalResultsWindowCommand` | idem                                                                                | `RefreshExternalResultsWindowCommandHandler` | Orchestrateur : envoie `FetchExternalResultsWindowCommand` puis `ApplyExternalResultsWindowCommand` via CommandBus |
| `OverrideDrawResultCommand`           | `tenantId`, `slotKey`, `drawDate`, `pick3`, `pick4`, `reason`, `force`              | `OverrideDrawResultCommandHandler`           | Upsert avec `status=FINAL`, `source=ADMIN_OVERRIDE`, `quality=COMPLETE`                                            |
| `RecordManualDrawResultCommand`       | `tenantId`, `drawDate`, `slotKey`, `recordedBy`, `notes`, `pick3`, `pick4`, `force` | `RecordManualDrawResultCommandHandler`       | Crée avec `status=FINAL`, `source=MANUAL`, `quality=COMPLETE` + projection Haiti                                   |

Anomalie : `FetchExternalResultsWindowCommandHandler` passe le status en String `"PROVISIONAL"` et source en String `"EXTERNAL"` au lieu d'utiliser `DrawResultStatus.PROVISIONAL.name()` et `DrawSource.EXTERNAL.name()`. Les handlers `Override` et `Manual` utilisent `.name()`.

Anomalie : `RefreshExternalResultsWindowCommandHandler` orchestre 2 commandes cross-domaine (`core.drawresult` → `core.draw`) via CommandBus dans le même handler.

### 4. Queries existantes

| Query                  | Paramètres                                      | Handler                       | Retour                |
| ---------------------- | ----------------------------------------------- | ----------------------------- | --------------------- |
| `ListDrawResultsQuery` | `provider`, `slotKey`, `from`, `to`, `pageable` | `ListDrawResultsQueryHandler` | `TchPage<DrawResult>` |

### 5. Ports (in/out)

**Ports OUT** (dans `application.port.out`) :

- `DrawResultReaderPort` : `getById(DrawResultId)`, `findByCriteria(DrawResultsCriteria)`, `findByResultSlotIdAndOccurredAt(ResultSlotId, Instant)`
- `DrawResultWriterPort` : `upsert(...)` — signature à 12 paramètres positionnels
- `ExternalResultsFetchPort` : `fetchSlot(ResultSlotFetchQuery)` — implémenté par `core.uslottery`
- `HaitiProjectionConfigPort` : `getDefault()` — implémenté par `DefaultHaitiProjectionConfigAdapter`

**Absence de `api/`** : `DrawResultReaderPort` est dans `application.port.out/` mais consommé directement par `core.draw` — pas d'isolation via un dossier `api/`.

### 6. Schedulers / Batchs

**`ExternalResultsFetchTickScheduler`** (`src/main/java/com/tchalanet/server/core/drawresult/infra/scheduler/ExternalResultsFetchTickScheduler.java`)

- Cron : `${tch.draw.results.scheduler.fetch_cron:0 */5 * * * *}` (toutes les 5 min)
- Gate : `BatchJobKeys.RESULTS_EXTERNAL_FETCH`
- Logique `isDue()` : vérifie que `now ∈ [drawTime + minMinutes, drawTime + maxMinutes]` en timezone du slot
- Cooldown in-memory par slot : `ConcurrentHashMap<String, Instant>` avec `cooldownMinutes` depuis `DrawResultsProperties`
- Idempotence : cooldown applicatif in-memory (non distribué — perdu au restart) + contrainte SQL UNIQUE implicite via `writer.upsert()`
- Pas de ShedLock

### 7. Events

**Publié** :

- `DrawResultedAppliedEvent` défini dans `core.drawresult.domain.event` mais **publié depuis `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler`** via `AfterCommit.run()` + `DomainEventPublisher`. Inversion de responsabilité.

**Consommés** : aucun listener dans `core.drawresult` lui-même.

### 8. Controllers HTTP

**`DrawResultsController`** (`src/main/java/com/tchalanet/server/core/drawresult/infra/web/DrawResultsController.java`)

- Path : `/admin/draw-results`
- `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` : **actif**
- Retourne `TchPage<DrawResultResponse>` directement — **non encapsulé dans `ApiResponse<T>`** (non conforme)
- Endpoints : `GET /` (list), `GET /today`, `GET /last-days`

### 9. Cache

Aucun `@Cacheable` / `@CacheEvict` observé directement dans `core.drawresult`. `DrawResultCacheEvictor` présent mais logique non confirmée.

### 10. Dépendances cross-domain

- `catalog.resultslot.api.ResultSlotCatalog` — conforme (via interface api/)
- `core.haiti.application.port.out.HaitiLotteryPort` — conforme (via port)
- `core.draw.application.command.model.ApplyExternalResultsWindowCommand` depuis `RefreshExternalResultsWindowCommandHandler` — cross-domain `core.drawresult` → `core.draw`

### 11. Anomalies détectées

- **`DrawResult` record contient `ObjectMapper` comme champ `static final`** — instance Jackson dans un record domain pur.
- **`DrawResult` record a un constructeur de compatibilité** qui utilise `VALID` comme status — statut legacy non documenté.
- **`DrawResultStatus` enum** : `VALID`, `OVERRIDDEN`, `INVALIDATED` sans usage visible ni documentation.
- **`FetchExternalResultsWindowCommandHandler`** passe `"PROVISIONAL"` et `"EXTERNAL"` comme `String` bruts (incohérence avec les autres handlers qui utilisent `.name()`).
- **Cooldown in-memory non distribué** dans `ExternalResultsFetchTickScheduler`.

---

## Domaine 5 : `core.draw` (`com.tchalanet.server.core.draw`)

### 1. Structure de packages

```
core/draw/
├── domain/
│   ├── model/
│   │   ├── Draw.java              (agrégat — classe finale)
│   │   ├── DrawStatus.java        (enum)
│   │   ├── DrawStatusTransition.java
│   │   ├── DrawSummary.java       (projection — record)
│   │   └── DrawChannelSummary.java
│   └── event/
│       └── DrawSettledEvent.java
├── application/
│   ├── command/
│   │   ├── model/
│   │   │   ├── GenerateDrawsForRangeCommand.java
│   │   │   ├── OpenDueDrawsCommand.java
│   │   │   ├── CloseDueDrawsCommand.java
│   │   │   ├── ApplyExternalResultsWindowCommand.java
│   │   │   ├── SettleDrawCommand.java
│   │   │   ├── CreateDrawCommand.java
│   │   │   ├── UpdateDrawCommand.java
│   │   │   ├── RefreshDrawCacheCommand.java
│   │   │   ├── [*Result records correspondants]
│   │   └── handler/
│   │       ├── GenerateDrawsForRangeCommandHandler.java
│   │       ├── OpenDueDrawsCommandHandler.java
│   │       ├── CloseDueDrawsCommandHandler.java
│   │       ├── ApplyExternalResultsWindowCommandHandler.java
│   │       ├── CreateDrawCommandHandler.java
│   │       ├── UpdateDrawCommandHandler.java
│   │       └── SettleDrawsCommandHandler.java
│   ├── query/
│   │   ├── model/
│   │   │   ├── ListDrawsQuery.java
│   │   │   ├── GetDrawQuery.java
│   │   │   ├── GetDrawResultQuery.java
│   │   │   ├── GetNextDrawQuery.java
│   │   │   ├── GetNextDrawsQuery.java
│   │   │   └── DrawSearchCriteria.java
│   │   ├── handler/
│   │   │   ├── ListDrawsHandler.java
│   │   │   ├── GetDrawHandler.java
│   │   │   └── GetDrawResultQueryHandler.java
│   │   └── projection/
│   │       ├── OpenableDrawRow.java
│   │       ├── DueToCloseRow.java
│   │       ├── NewDrawRow.java
│   │       └── ExistingDrawKey.java
│   ├── port/
│   │   └── out/
│   │       ├── DrawLookupPort.java
│   │       ├── DrawLifecyclePort.java
│   │       ├── DrawApplyPort.java
│   │       ├── DrawBatchQueryPort.java
│   │       ├── DrawChannelGameWriterPort.java
│   │       ├── FindSettleableDrawIdsPort.java
│   │       └── TenantDrawCalendarQueryPort.java
│   ├── print/
│   │   ├── DrawChannelLabelResolver.java
│   │   └── DrawOccurrenceLabelResolver.java
│   └── util/
│       └── HaitiResultExtractors.java
├── infra/
│   ├── persistence/
│   │   ├── DrawJpaEntity.java
│   │   ├── mapper/
│   │   │   ├── DrawMapper.java
│   │   │   ├── DrawAdminWebMapper.java
│   │   │   ├── DrawChannelWebMapper.java
│   │   │   └── DrawWebMapper.java
│   │   ├── projection/
│   │   │   ├── DueToCloseProjection.java
│   │   │   └── OpenableDrawProjection.java
│   │   ├── adapter/
│   │   │   ├── DrawApplyJdbcAdapter.java
│   │   │   ├── DrawLifecycleJpaAdapter.java
│   │   │   ├── DrawLookupJdbcAdapter.java
│   │   │   ├── SettleableDrawIdsJpaAdapter.java
│   │   │   └── TenantDrawCalendarQueryAdapter.java
│   │   └── repo/
│   │       ├── DrawJpaRepository.java
│   │       ├── DrawLookupJdbcRepository.java
│   │       ├── DrawApplyJdbcRepository.java
│   │       ├── ApplyCandidateDrawJdbcRepository.java
│   │       ├── DrawBatchQueryRepository.java
│   │       ├── DrawDueToCloseProjection.java
│   │       ├── DrawOpenableProjection.java
│   │       └── FetchableDrawRow.java
│   ├── event/
│   │   └── DrawDomainEventListener.java
│   ├── batch/
│   │   └── results/settle/
│   │       ├── DrawSettleJobConfig.java
│   │       ├── SettleableDrawIdsReader.java
│   │       ├── SettleProcessor.java
│   │       └── SettleWriter.java
│   ├── cache/
│   │   ├── DrawCacheConfig.java
│   │   └── DrawCacheKeyBuilder.java
│   ├── config/
│   │   ├── DrawProperties.java
│   │   └── TchPropertiesConfig.java
│   ├── scheduler/
│   │   ├── ExternalResultsApplyTickScheduler.java
│   │   └── DrawSettleScheduler.java
│   └── web/
│       ├── DrawAdminController.java
│       └── model/
│           ├── DrawSummaryResponse.java
│           ├── DrawChannelResponse.java
│           ├── DrawChannelSummaryResponse.java
│           ├── CreateDrawRequest.java
│           ├── CreateDrawChannelRequest.java
│           ├── UpdateDrawRequest.java
│           └── UpdateDrawChannelRequest.java
```

### 2. Entités JPA et tables

**`DrawJpaEntity`** (`src/main/java/com/tchalanet/server/core/draw/infra/persistence/DrawJpaEntity.java`)

- Table : `draw`
- Hérite de : `BaseTenantEntity` (RLS active)
- UNIQUE : `uq_draw_tenant_channel_date` sur `(tenantId, draw_channel_id, draw_date)`
- Indexes : `ix_draw_tenant_date`, `ix_draw_tenant_scheduled`, `ix_draw_status_scheduled_at`, `ix_draw_status_cutoff_at`
- Champs métier :
  - `drawChannel` (`@ManyToOne DrawChannelJpaEntity`, LAZY, NOT NULL)
  - `drawDate` (LocalDate, NOT NULL)
  - `scheduledAt`, `cutoffAt` (Instant, NOT NULL)
  - `openedAt`, `closedAt`, `resultedAt`, `settledAt`, `canceledAt` (Instant, nullable)
  - `cancelReason` (String, nullable)
  - `status` (VARCHAR 16, NOT NULL) — `DrawStatus` enum
  - `drawResultId` (UUID, nullable — FK raw)
  - `systemGenerated` (boolean, NOT NULL, default `true`)
  - `locked` (boolean, NOT NULL, default `false`)
  - `resultSource` (VARCHAR 16)
  - `resultOverrideReason` (String, nullable)
  - `resultOverriddenAt` (Instant, nullable)
- `@Audited` : oui (Envers)

**Enum `DrawStatus`** : `SCHEDULED`, `OPEN`, `CLOSED`, `RESULTED`, `SETTLED`, `ARCHIVED`, `CANCELED`

**`DrawStatusTransition`** (state machine définie dans le code) :

```
SCHEDULED → {OPEN, CANCELED}
OPEN      → {CLOSED, CANCELED}
CLOSED    → {RESULTED, CANCELED}
RESULTED  → {SETTLED, CANCELED}
SETTLED   → {}   (terminal)
CANCELED  → {}   (terminal)
```

Note : `ARCHIVED` est absent de la state machine — transition non définie.

### 3. Commands existantes

| Command                                                                                              | Handler                                    | Action                                                        | Idempotence                                                                        |
| ---------------------------------------------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| `GenerateDrawsForRangeCommand(tenantId, from, to, dryRun, force)`                                    | `GenerateDrawsForRangeCommandHandler`      | Génère les `NewDrawRow` pour chaque date/channel, bulk insert | Contrainte SQL `UNIQUE(tenant, channel, date)` + préchargement des clés existantes |
| `OpenDueDrawsCommand(now, limit, openHorizonHours, openLagHours, dryRun)`                            | `OpenDueDrawsCommandHandler`               | Bulk update → OPEN                                            | Filtre DB `status='SCHEDULED' AND locked=false`                                    |
| `CloseDueDrawsCommand(now, limit, dryRun)`                                                           | `CloseDueDrawsCommandHandler`              | Bulk update → CLOSED                                          | Filtre DB `status='OPEN' AND locked=false`                                         |
| `ApplyExternalResultsWindowCommand(tenantId, baseDate, daysBack, slotKeys, force, dryRun, maxSlots)` | `ApplyExternalResultsWindowCommandHandler` | Attache draw_result aux draws CLOSED via `DrawApplyPort`      | `DrawApplyPort.attachResultBySlot()` + filtre status                               |
| `CreateDrawCommand(tenantId, channelCode, scheduledDate)`                                            | `CreateDrawCommandHandler`                 | Crée un draw en status SCHEDULED                              | Contrainte SQL UNIQUE                                                              |
| `UpdateDrawCommand(tenantId, drawId, scheduledDate, code, name)`                                     | `UpdateDrawCommandHandler`                 | Reschedule un draw SCHEDULED uniquement                       | Verrou statut (`DrawStatus.SCHEDULED` requis)                                      |
| `SettleDrawCommand(tenantId, drawId)`                                                                | `SettleDrawsCommandHandler`                | Settle un draw RESULTED                                       | `draw.locked` flag                                                                 |
| `RefreshDrawCacheCommand`                                                                            | (handler non confirmé)                     | Invalider le cache draw                                       | —                                                                                  |

Anomalie : `CreateDrawCommandHandler` utilise `ZoneId.systemDefault()` pour `scheduledAt` et `cutoffAt = scheduledAt.minusHours(1)` — magic number `1 heure` hardcodé, ignorant la timezone réelle du channel et le `cutoffSec` configuré.

Anomalie : `UpdateDrawCommand` contient les champs `code` et `name` mais `UpdateDrawCommandHandler` ne les utilise pas (uniquement `scheduledDate`).

### 4. Queries existantes

| Query                                             | Handler                     | Retour              |
| ------------------------------------------------- | --------------------------- | ------------------- |
| `ListDrawsQuery(tenantId, channelCode, from, to)` | `ListDrawsHandler`          | `List<DrawSummary>` |
| `GetDrawQuery(tenantId, drawId)`                  | `GetDrawHandler`            | `Draw`              |
| `GetDrawResultQuery(tenantId, drawId)`            | `GetDrawResultQueryHandler` | `DrawResult`        |
| `GetNextDrawQuery`                                | (handler à confirmer)       | `Draw`              |
| `GetNextDrawsQuery`                               | (handler à confirmer)       | `List<Draw>`        |

`DrawSearchCriteria.of()` utilise `LocalDate.now()` directement (sans Clock) dans des méthodes statiques.

### 5. Ports (in/out)

**Ports OUT** :

- `DrawLookupPort` : `findById(DrawId)`, `findByCriteria(DrawSearchCriteria)`, `findDrawIdBySlotId(TenantId, LocalDate, ResultSlotId)`
- `DrawLifecyclePort` : `findOpenable(...)`, `bulkOpen(...)`, `findDueToClose(...)`, `bulkClose(...)`, `bulkInsert(...)`, `save(Draw)`, `findExistingKeys(...)`
- `DrawApplyPort` : `attachResultBySlot(TenantId, LocalDate, ResultSlotId, DrawResultId, Instant, boolean)`
- `DrawBatchQueryPort`, `DrawChannelGameWriterPort`, `FindSettleableDrawIdsPort`, `TenantDrawCalendarQueryPort`

**Consomme depuis l'extérieur** :

- `DrawResultReaderPort` de `core.drawresult.application.port.out` (import direct sans isolation `api/`)

### 6. Schedulers / Batchs

**`DrawLifeCycleTickScheduler`** (`src/main/java/com/tchalanet/server/core/draw/infra/batch/scheduler/DrawLifeCycleTickScheduler.java`) :

- `generateNext7Days` : cron `0 0 5 * * *` UTC, gate `DRAW_GENERATE`, envoie `GenerateDrawsForRangeCommand`
- `openWindowed` : cron `0 */30 * * * *` UTC, gate `DRAW_OPEN`, fenêtre opérationnelle `America/New_York`, envoie `OpenDueDrawsCommand(now, 5000, 24, 12, false)` — **magic numbers 5000, 24, 12 hardcodés**
- `closeWindowed` : cron `0 */15 * * * *` UTC, gate `DRAW_CLOSE`, fenêtre opérationnelle, envoie `CloseDueDrawsCommand(now, 5000, false)` — **magic number 5000 hardcodé**

**`ExternalResultsApplyTickScheduler`** (`src/main/java/com/tchalanet/server/core/draw/infra/scheduler/ExternalResultsApplyTickScheduler.java`) :

- Cron : `${tch.draw.results.scheduler.apply_cron:30 */5 * * * *}` (offset +30s par rapport au fetch)
- Guard in-memory : `AtomicBoolean running` (non distribué)
- Envoie `ApplyExternalResultsWindowCommand` par tenant × slot

**`DrawSettleScheduler`** (`src/main/java/com/tchalanet/server/core/draw/infra/scheduler/DrawSettleScheduler.java`) :

- Cron : `0 */5 * * * *` zone `America/New_York`
- Vérifie fenêtre opérationnelle `BatchWindowsConfig.isInSettleDrawsWindow()`
- Démarre Spring Batch job `DRAW_SETTLE` pour providers **NY (maxDraws=700) et FL (maxDraws=900) seulement** — GA, TX, TN non couverts
- Gate par provider : `SWITCH_NY`, `SWITCH_FL`

**Spring Batch** (`infra/batch/results/settle/`) : `DrawSettleJobConfig` + `SettleableDrawIdsReader` + `SettleProcessor` + `SettleWriter`.

Pas de ShedLock sur aucun scheduler.

### 7. Events

**Publiés** :

- `DrawResultedAppliedEvent` (appartient à `core.drawresult.domain.event`) : publié depuis `ApplyExternalResultsWindowCommandHandler` via `AfterCommit.run()` + `DomainEventPublisher` — inversion de responsabilité
- `DrawSettledEvent` (`domain/event/DrawSettledEvent.java`) : point de publication non confirmé directement (handler settle non lu intégralement)

**`DrawDomainEventListener`** (`src/main/java/com/tchalanet/server/core/draw/infra/event/DrawDomainEventListener.java`) :

- Écoute `DrawResultedAppliedEvent` avec `@EventListener` (pas `@TransactionalEventListener(AFTER_COMMIT)`)
- `ProcessedEventPort` : non utilisé
- **Corps vide** : contient uniquement `log.info()` + `// TODO: invalidate caches`
- **Anomalie** : appelle `event.slotKey()` mais `DrawResultedAppliedEvent` expose `resultSlotId` (de type `ResultSlotId`), pas `slotKey` (de type `String`) — potentiel problème de compilation

### 8. Controllers HTTP

**`DrawAdminController`** (`src/main/java/com/tchalanet/server/core/draw/infra/web/DrawAdminController.java`)

- Path : `/admin/draws`
- `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` : **COMMENTÉ** — `// @PreAuthorize("hasAuthority('SUPER_ADMIN')") //todo remove testing`
- Retourne `ResponseEntity<List<DrawSummaryResponse>>` directement — **non conforme `ApiResponse<T>`**
- Expose `POST /{drawId}/override-result` → envoie `OverrideDrawResultCommand`
- `DrawAdminController.createDraw()` : envoie command + list query + filter inline

### 9. Cache

Caches définis dans `DrawCacheConfig` :

- `core.draw.latest::{tenant}` (TTL 30s)
- `core.draw.by_id::{drawId}` (TTL 5 min)
- `core.draw.calendar::{tenant}_{date}` (TTL 5 min)

**Le cache n'est pas invalidé** : `DrawDomainEventListener` contient uniquement `// TODO: invalidate caches`.

### 10. Dépendances cross-domain

- `catalog.drawchannel.api.*` — conforme
- `catalog.resultslot.api.*` — conforme
- `catalog.tenant.api.*` — conforme
- `core.drawresult.application.port.out.DrawResultReaderPort` — import vers `application.port.out` d'un autre domaine (pas via `api/`)
- `core.drawresult.domain.event.DrawResultedAppliedEvent` — import vers `domain.event` d'un autre domaine
- `core.drawresult.domain.model.DrawSource` — import vers `domain.model` d'un autre domaine
- `core.drawresult.infra.config.DrawResultsProperties` — **import vers `infra.config` d'un autre domaine** (violation stricte)
- `core.haiti.application.port.out.HaitiLotteryPort` — conforme (via port)

### 11. Anomalies détectées

- **`@PreAuthorize` commenté** dans `DrawAdminController`
- **`DrawDomainEventListener.onDrawResulted()` appelle `event.slotKey()`** — méthode inexistante sur `DrawResultedAppliedEvent`
- **`DrawSearchCriteria.of()` utilise `LocalDate.now()` sans Clock**
- **Magic numbers hardcodés** : 5000, 24, 12 dans `DrawLifeCycleTickScheduler`
- **`CreateDrawCommandHandler` : `ZoneId.systemDefault()` + `minusHours(1)` hardcodé** — ignore timezone du channel et `cutoffSec`
- **`UpdateDrawCommand` champs `code` et `name`** non utilisés par le handler
- **Cache non implémenté** (`DrawDomainEventListener` = TODOs)
- **`DrawSettleScheduler` hardcode NY+FL** — GA, TX, TN non couverts
- **`SettleDrawCommand.java`** existe mais aucun `SettleDrawCommandHandler` distinct n'a été localisé (le settle passe via Spring Batch)

---

## Domaines connexes

---

## Domaine 6 : `core.uslottery` (`com.tchalanet.server.core.uslottery`)

### 1. Structure de packages

Fichiers confirmés :

- `infra/config/UsLotteryProperties.java` — `@ConfigurationProperties(prefix = "tch.us-lottery")`
- `infra/config/UsLotteryConfig.java` — beans `RestClient` par provider (ny, fl, ga, tn, tx)

L'adaptateur implémentant `ExternalResultsFetchPort` n'a pas été localisé directement.

### 2. Entités JPA et tables

Aucune table persistée — domaine client HTTP pur.

### 3. Configuration providers

Configuration double (source de vérité dupliquée) :

- `src/main/resources/application.yaml` : providers `ny`, `fl`, `ga`, `tn` avec `games[]` + `draw_time` hardcodés
- `src/main/resources/application-uslottery.yaml` : providers `ny`, `fl`, `ga`, `tn`, `tx` avec `games[]` + `draw_time` hardcodés

Divergence : TX n'existe que dans `application-uslottery.yaml`. NY et FL ont des `app-token` différents entre les deux fichiers.

### 8. Controllers HTTP

Aucun controller HTTP — conforme.

### 11. Anomalies détectées

- Configuration dupliquée entre `application.yaml` et `application-uslottery.yaml` pour les providers US lottery.
- `draw_time` hardcodé dans YAML pour chaque game — doublon avec la table `result_slot`.
- TX provider `enabled: ${TCH_US_TX_LOTTERY_ENABLED:true}` dans `application-uslottery.yaml` mais absent de `application.yaml`.

---

## Domaine 7 : `core.haiti` (`com.tchalanet.server.core.haiti`)

### 1. Structure de packages

```
core/haiti/
├── application/
│   └── port/
│       └── out/
│           └── HaitiLotteryPort.java
└── domain/
    └── lottery/
        ├── exception/
        │   └── InvalidExternalPickException.java
        ├── model/
        │   ├── ExternalPick.java
        │   ├── HaitiLot.java           (enum: LOT1, LOT2, LOT3, LOT4)
        │   ├── HaitiProjectionConfig.java
        │   └── HaitiProjectionToken.java
        └── service/
            ├── HaitiResultProjector.java   (interface)
            └── DefaultHaitiResultProjector.java  (@Component)
```

### 2. Entités JPA et tables

Aucune table. Domaine projecteur pur.

### 8. Controllers HTTP

Aucun controller.

### 10. Dépendances cross-domain

Exporte `HaitiLotteryPort` consommé par `core.drawresult`. Conforme.

### 11. Anomalies détectées

- `ExternalPick` a deux chemins d'instanciation : `of()` (avec validation 3/4 chiffres) et le constructeur record par défaut (sans validation). `FetchExternalResultsWindowCommandHandler` utilise le constructeur direct, contournant la validation.
- L'implémentation du bean qui fournit `HaitiProjectionConfigPort.getDefault()` n'a pas été localisée. La config par défaut (mapping LOT1 → PICK3_FULL_3, etc.) est opaque.

---

## Domaine 8 : `features.publicdraw`

### 1. Structure de packages

Le sous-dossier `infra/web/` est confirmé mais aucun fichier `.java` n'a été localisé avec des noms conventionnels. Le feature est référencé dans `DOMAIN_DRAW.md` comme consommateur de `core.draw` et `core.drawresult`.

### 8. Controllers HTTP

AMBIGU : controller public attendu sur `/public/draws` ou `/tenant/draws` mais non confirmé.

### 11. Anomalies détectées

Impossible de confirmer la présence ou absence de `@PreAuthorize` sur ce controller. AMBIGU.

---

## Domaine 9 : `features.pagemodel.dynamic.providers.DrawsProvider`

### 1. Structure

Fichier : `src/main/java/com/tchalanet/server/features/pagemodel/dynamic/providers/DrawsProvider.java`

### 2. Logique

- Implémente `PageModelDynamicProvider`
- Supporte les sources `"results_by_game"` et `"draws"`
- Envoie `ListDrawResultsQuery(null, null, LocalDate.now().minusDays(7), LocalDate.now(), PageRequest.of(0, limit))`
- Retourne `Map<String, Object>` : `{drawnAt, name, results}` par draw
- `name` = `r.source().name()` (valeur de l'enum `DrawSource`, ex: `"EXTERNAL"`) — non UX

### 10. Dépendances cross-domain

- Importe `core.drawresult.application.query.model.ListDrawResultsQuery` directement
- Importe `core.drawresult.domain.model.DrawResult` directement
- Utilise `DrawResult.numbersMain()` — méthode legacy

### 11. Anomalies détectées

- `r.source().name()` retourne un nom d'enum technique, pas un label métier.
- `DrawResult.numbersMain()` est une méthode de rétrocompatibilité legacy.
- `LocalDate.now()` sans Clock.
- Import direct sur `core.drawresult.domain.model.DrawResult` depuis `features.*`.

---

## Domaine 10 : `features.ops`

### 1. Structure de packages

Le dossier `features.ops` existe (`infra/web/` confirmé) mais aucun fichier `.java` n'a été localisé avec des noms conventionnels.

### 8. Controllers HTTP

AMBIGU : un `DrawResultsOpsController` sur `/platform/ops/draw-results` est attendu selon la doc mais non confirmé.

### 11. Anomalies détectées

Si les ops controllers n'existent pas, les commandes `Refresh`, `Override`, `Manual` ne sont accessibles qu'en passant par les endpoints de `DrawAdminController` (qui n'a pas de `@PreAuthorize`) ou via les schedulers automatiques.

---

## Synthèse transverse

### A. Frontières domaine

**1. Imports `core.X.infra.*` depuis un autre domaine ?**

- `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler` importe `core.drawresult.infra.config.DrawResultsProperties` — **violation stricte** : couche `infra.config` d'un domaine consommée par un autre.

**2. Imports `core.X.domain.model.*` depuis `features.*` ou un autre `core.*` ?**

- `core.draw.domain.model.Draw` importe `core.drawresult.domain.model.DrawSource`
- `core.draw.infra.persistence.DrawJpaEntity` importe `core.drawresult.domain.model.DrawSource`
- `features.pagemodel.dynamic.providers.DrawsProvider` importe `core.drawresult.domain.model.DrawResult`
- `catalog.drawchannel.api.model.DrawChannelView` importe `core.drawresult.domain.model.DrawSource`
- `catalog.drawchannel.internal.web.model.CreateDrawChannelRequest` importe `core.drawresult.domain.model.DrawSource`

**3. Imports d'un repository JPA/JDBC depuis l'extérieur de son domaine ?**

Non détecté. Les repositories restent dans leurs propres domaines.

### B. Naming events

| Event                      | Package                        | Producer                                                                         | Consumer                                        | Mode publication                             |
| -------------------------- | ------------------------------ | -------------------------------------------------------------------------------- | ----------------------------------------------- | -------------------------------------------- |
| `DrawResultedAppliedEvent` | `core.drawresult.domain.event` | `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler` | `core.draw.infra.event.DrawDomainEventListener` | `AfterCommit.run()` + `DomainEventPublisher` |
| `DrawSettledEvent`         | `core.draw.domain.event`       | Handler settle (Spring Batch `SettleWriter` présumé)                             | `core.draw.infra.event.DrawDomainEventListener` | Non confirmé                                 |

`DrawResultedAppliedEvent` est publié par `core.draw` mais appartient au package `core.drawresult.domain.event` — inversion de responsabilité.

### C. Sécurité

| Controller                  | Path                     | @PreAuthorize                                                    |
| --------------------------- | ------------------------ | ---------------------------------------------------------------- |
| `DrawAdminController`       | `/admin/draws`           | **COMMENTÉ** (`// @PreAuthorize(...)` + `//todo remove testing`) |
| `DrawResultsController`     | `/admin/draw-results`    | **ACTIF**                                                        |
| `GameAdminController`       | `/platform/games`        | **ACTIF**                                                        |
| `ResultSlotAdminController` | `/platform/result-slots` | **ACTIF**                                                        |

**`DrawAdminController` sans auth** : tous les endpoints CRUD des draws, dont `POST /{drawId}/override-result`, sont accessibles sans authentification.

### D. Conformité ApiResponse

| Controller                  | Ce qui est retourné                                                                    |
| --------------------------- | -------------------------------------------------------------------------------------- |
| `DrawAdminController`       | `ResponseEntity<List<DrawSummaryResponse>>` — **non conforme**                         |
| `DrawResultsController`     | `TchPage<DrawResultResponse>` — **non conforme** (non encapsulé dans `ApiResponse<T>`) |
| `GameAdminController`       | `ApiResponse<T>` — conforme                                                            |
| `ResultSlotAdminController` | `ApiResponse<T>` — conforme                                                            |

### E. Schedulers & Idempotence

| Scheduler                                      | Cron                  | Mécanisme d'idempotence                                                         |
| ---------------------------------------------- | --------------------- | ------------------------------------------------------------------------------- |
| `DrawLifeCycleTickScheduler.generateNext7Days` | `0 0 5 * * *` UTC     | Contrainte SQL `UNIQUE(tenant, channel, date)`                                  |
| `DrawLifeCycleTickScheduler.openWindowed`      | `0 */30 * * * *` UTC  | Filtre DB `status='SCHEDULED' AND locked=false`                                 |
| `DrawLifeCycleTickScheduler.closeWindowed`     | `0 */15 * * * *` UTC  | Filtre DB `status='OPEN' AND locked=false`                                      |
| `ExternalResultsApplyTickScheduler`            | `30 */5 * * * *`      | `DrawApplyPort` filtre status + `AtomicBoolean` in-memory (non distribué)       |
| `DrawSettleScheduler`                          | `0 */5 * * * *` NY TZ | Spring Batch + `draw.locked` + gate par provider                                |
| `ExternalResultsFetchTickScheduler`            | `0 */5 * * * *`       | Contrainte SQL UNIQUE implicite + `ConcurrentHashMap` in-memory (non distribué) |

**Pas de ShedLock** sur aucun scheduler. En multi-instances : `generateNext7Days`, `openWindowed`, `closeWindowed` s'exécutent N fois simultanément (protection uniquement par contraintes DB). `ExternalResultsApplyTickScheduler` et `ExternalResultsFetchTickScheduler` ont des guards in-memory perdus au restart.

### F. Sources de vérité multiples

1. **Game draw times** : définis dans `application.yaml` (section `tch.us-lottery.providers.*.games[].draw_time`) ET dans `application-uslottery.yaml` — doublon. La table `result_slot.draw_time` est supposée être la source de vérité, mais le YAML contient les mêmes informations.
2. **`DrawSource` enum** : vit dans `core.drawresult.domain.model` mais est consommé par `core.draw.domain.model`, `core.draw.infra.persistence`, `catalog.drawchannel.api.model`, `catalog.drawchannel.internal.web.model` — type partagé qui n'est pas dans `common/`.
3. **`DrawResultStatus` enum** : contient `VALID`, `OVERRIDDEN`, `INVALIDATED` non documentés et sans usage visible dans le code principal.
4. **Providers hardcodés** : `DrawSettleScheduler` hardcode NY et FL avec leurs `maxDraws` respectifs ; les autres providers (GA, TX, TN) ne sont pas dans le settle.

### G. OccurredAtResolver

**9. Usages de `OccurredAtResolver`** :

- `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler` : `OccurredAtResolver.resolve(null, date, slot.drawTime(), slot.timezone(), clock)`
- `core.drawresult.application.command.handler.FetchExternalResultsWindowCommandHandler` : `OccurredAtResolver.resolve(p3.occurredAt() ou p4, date, slot.drawTime(), slot.timezone(), clock)`

**10. Calculs manuels `LocalDate.atTime(...).atZone(...).toInstant()`** :

- `src/main/java/com/tchalanet/server/core/drawresult/application/service/ResultSlotTimes.java` : `ZonedDateTime.of(drawDate, drawTime, tz).toInstant()` — utilitaire distinct qui duplique la logique d'`OccurredAtResolver`. Utilisé par `OverrideDrawResultCommandHandler` et `RecordManualDrawResultCommandHandler`.
- `src/main/java/com/tchalanet/server/core/draw/infra/scheduler/ExternalResultsFetchTickScheduler.java` (ligne ~94-99) : `nowZ.toLocalDate().atTime(slot.drawTime()).atZone(slot.timezone()).toInstant()` — calcul inline, n'utilise pas `OccurredAtResolver`.

3 implémentations distinctes du calcul `occurredAt` : `OccurredAtResolver`, `ResultSlotTimes`, et inline dans `ExternalResultsFetchTickScheduler`.

### H. Gaps fonctionnels

**11. TODOs significatifs** :

| Fichier                                      | Contenu du TODO                                                                                             |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `DrawAdminController.java` ligne 29          | `// @PreAuthorize("hasAuthority('SUPER_ADMIN')") //todo remove testing`                                     |
| `DrawDomainEventListener.java` lignes 22, 31 | `// TODO: invalidate caches for today/last-days/next draws`                                                 |
| `DrawSettleScheduler.java`                   | Hardcode NY+FL, GA/TX/TN non couverts (TODO P2 implicite)                                                   |
| `DOMAIN_DRAW.md` P1                          | `GetDrawByIdQuery` et `GetDrawBySlotAndDateQuery` — présents en code réel mais non listés comme implémentés |
| `DOMAIN_DRAWRESULT.md` P1                    | Renommage `DrawResultedAppliedEvent` → `DrawResultIngestedEvent` non effectué                               |

**12. Classes *Stub, *Fake, *Tmp, *Mock hors src/test/** :

Aucune détectée dans le périmètre analysé.

### I. Décisions draw-pipeline

**13. `docs/decisions/draw-pipeline-decisions.md`** :

Le fichier `docs/decisions/draw-pipeline-decisions.md` n'existe pas. Le répertoire `docs/decisions/` n'a pas été trouvé dans `tchalanet-server/docs/`.

---

## Conclusion exécutive — Top 5 chantiers urgents

**1. Sécurité P0 — `DrawAdminController` sans `@PreAuthorize`**

`DrawAdminController` (path `/admin/draws`) a son `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` commenté avec la note `//todo remove testing`. Tous les endpoints CRUD des draws (liste, création, mise à jour, override de résultat) sont accessibles sans authentification.

Fichier : `src/main/java/com/tchalanet/server/core/draw/infra/web/DrawAdminController.java`, ligne 29.

**2. Problème de compilation probable — `DrawDomainEventListener.onDrawResulted()` appelle `event.slotKey()`**

`DrawDomainEventListener` utilise `event.slotKey()` mais `DrawResultedAppliedEvent` expose `resultSlotId` (de type `ResultSlotId`), pas `slotKey` (de type `String`). Si ce code compile, une version décalée de l'event est en circulation. Si non, le listener produit une erreur à la compilation. Corps du listener : uniquement des `log.info()` + `// TODO: invalidate caches` — aucune action réelle.

Fichiers : `src/main/java/com/tchalanet/server/core/draw/infra/event/DrawDomainEventListener.java` ligne 19 ; `src/main/java/com/tchalanet/server/core/drawresult/domain/event/DrawResultedAppliedEvent.java`.

**3. Violations d'isolation entre `core.draw` et `core.drawresult` — 5 violations distinctes**

(a) `DrawSource` enum dans `core.drawresult.domain.model` importé par `core.draw.domain.model`, `core.draw.infra.persistence`, `catalog.drawchannel.api.model`.
(b) `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler` importe `core.drawresult.infra.config.DrawResultsProperties` (couche `infra.config` d'un autre domaine).
(c) `DrawResultedAppliedEvent` publié depuis `core.draw` mais appartient au package `core.drawresult.domain.event`.
(d) `RefreshExternalResultsWindowCommandHandler` (dans `core.drawresult`) envoie `ApplyExternalResultsWindowCommand` (commande de `core.draw`) — couplage bidirectionnel.
(e) `features.pagemodel.dynamic.providers.DrawsProvider` importe `core.drawresult.domain.model.DrawResult` directement.

**4. Cache draw non implémenté**

`DrawCacheConfig` définit 3 caches (`core.draw.latest::`, `core.draw.by_id::`, `core.draw.calendar::`). `DrawDomainEventListener` — seul point d'invalidation — contient uniquement des `// TODO`. Le cache est configuré mais jamais invalidé ; les données stale ne sont jamais évictées.

Fichier : `src/main/java/com/tchalanet/server/core/draw/infra/event/DrawDomainEventListener.java`.

**5. Settle incomplet — 3 providers sur 5 non couverts + duplication `occurredAt`**

`DrawSettleScheduler` démarre le job Spring Batch uniquement pour NY (maxDraws=700) et FL (maxDraws=900). GA, TX, TN n'ont pas de gate ni de déclenchement settle. Par ailleurs, 3 implémentations distinctes du calcul `occurredAt` coexistent : `OccurredAtResolver`, `ResultSlotTimes`, et inline dans `ExternalResultsFetchTickScheduler` — les handlers `OverrideDrawResultCommandHandler` et `RecordManualDrawResultCommandHandler` utilisent `ResultSlotTimes` au lieu d'`OccurredAtResolver`.

Fichiers : `src/main/java/com/tchalanet/server/core/draw/infra/scheduler/DrawSettleScheduler.java` ; `src/main/java/com/tchalanet/server/core/drawresult/application/service/ResultSlotTimes.java` ; `src/main/java/com/tchalanet/server/core/draw/infra/scheduler/ExternalResultsFetchTickScheduler.java`.
