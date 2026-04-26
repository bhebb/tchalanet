## Why

L'audit `2026-04-25-draw-pipeline-audit.md` a révélé, au-delà des chantiers déjà ouverts (briefs 1, 3, 4, 5), un ensemble de trous résiduels dans le pipeline draw : violations de frontière (imports `*.infra.*` cross-domain, couplage bidirectionnel entre `core.draw` et `core.drawresult`), bug fonctionnel critique (`CreateDrawCommandHandler` ignore `cutoffSec` du channel et utilise `ZoneId.systemDefault()`), absence de verrou distribué sur les schedulers, magic numbers non documentés, statuts d'enum sérialisés en `String` brut, et code legacy (3 valeurs de `DrawResultStatus` jamais utilisées, champs morts dans `UpdateDrawCommand`). Sans ce ménage, le passage en multi-instance, l'observabilité et la maintenance deviennent fragiles.

## What Changes

- **[ApiResponse migration]** `DrawAdminController` retourne `ApiResponse<T>` sur tous les endpoints (list, create, update) ; suppression de l'endpoint `override-result` (responsabilité de `features.ops.DrawResultsOpsController`)
- **[ApiResponse migration]** `DrawResultsController` retourne `ApiResponse<TchPage<DrawResultResponse>>` sur les 3 endpoints (`/admin/draw-results`, `/today`, `/last-days`)
- **[createDraw refactor]** `DrawAdminController.createDraw` consomme directement le `DrawSummary` retourné par le handler (suppression du pattern "send + list + filter") ; appel `GetDrawByIdQuery` si nécessaire
- **[Bug fix scheduledAt/cutoffAt]** `CreateDrawCommandHandler` lit `cutoffSec` depuis `DrawChannelView` et la timezone depuis `ResultSlotView`, calcule `scheduledAt` via `OccurredAtResolver` et `cutoffAt = scheduledAt.minusSeconds(channel.cutoffSec())` (fin du hardcode `minusHours(1)` + `ZoneId.systemDefault()`)
- **[Code mort]** `UpdateDrawCommand` : suppression des champs `code` et `name` (ignorés par le handler)
- **[Magic numbers]** `DrawLifeCycleTickScheduler` : extraction de `batchSize`, `lookaheadHours`, `lagHours` vers `DrawProperties` (config externalisée), nommage explicite des paramètres `OpenDueDrawsCommand` / `CloseDueDrawsCommand` (record ou JavaDoc)
- **[Cross-domain enum]** `DrawSource` migre de `core.drawresult.domain.model` vers `common.types.enums.DrawSource` (suit le pattern existant : `AuditAction`, `BetType`, `ApprovalRole`, `UsLotteryProvider`, etc.) ; mise à jour des 5+ consommateurs cross-domain ; ArchUnit interdit l'import de l'ancienne localisation
- **[Cleanup enum]** **BREAKING** — `DrawResultStatus` réduit à 3 valeurs (`PROVISIONAL`, `FINAL`, `ERROR`) ; suppression de `VALID`, `OVERRIDDEN`, `INVALIDATED` ; migration Flyway si présence en DB ; suppression du constructeur "compat" `DrawResult` qui défaultait sur `VALID`
- **[Status as enum.name()]** `FetchExternalResultsWindowCommandHandler` et tout consommateur passe les statuts via `enum.name()` au lieu de `String` brut (`"PROVISIONAL"`, `"EXTERNAL"`, etc.)
- **[ExternalPick validation]** `FetchExternalResultsWindowCommandHandler` utilise `ExternalPick.of(...)` (validation longueur 3/4 chiffres) au lieu de `new ExternalPick(...)` ; en cas d'`InvalidExternalPickException`, log WARN + skip slot (ne fait pas planter le tick)
- **[Couplage bidirectionnel]** `RefreshExternalResultsWindowCommand` migre de `core.drawresult.application.command` vers `features.ops.application.command` ; le handler dans `features.ops` orchestre `Fetch` puis `Apply` via `CommandBus` ; `DrawResultsOpsController` met à jour son import
- **[Frontière infra]** `DrawResultsProperties` (propriétés partagées scheduler/cooldown/limits) déplacé en `common.config.draw.DrawResultsCommonProperties` ; `core.drawresult.infra.config.DrawResultsProperties` ne garde que les propriétés spécifiques au fetch (URLs, headers) ; `core.draw` n'importe plus rien depuis `core.drawresult.infra.*`
- **[Unification occurredAt]** Suppression de `ResultSlotTimes` (dans `core.drawresult.application.service`) ; `OverrideDrawResultCommandHandler`, `RecordManualDrawResultCommandHandler`, `ExternalResultsFetchTickScheduler` migrent vers `OccurredAtResolver` ; `OccurredAtResolver` devient l'unique implémentation
- **[Clock injecté]** `DrawSearchCriteria.of()` accepte un `Clock` ; audit transversal sur `core.draw` et `core.drawresult` pour remplacer tous les `LocalDate.now()` / `Instant.now()` / `LocalTime.now()` non-`Clock`
- **[Distributed locking]** Ajout de la dépendance `shedlock-spring` + `shedlock-provider-jdbc-template` ; configuration `LockProvider` JDBC + table `shedlock` (Flyway) ; annotation `@SchedulerLock` sur les 6 schedulers identifiés (`DrawLifeCycleTickScheduler.{generateNext7Days,openWindowed,closeWindowed}`, `ExternalResultsApplyTickScheduler.tickApply`, `ExternalResultsFetchTickScheduler.tickFetch`, `DrawSettleScheduler.tick`)
- **[Cleanup guards in-memory]** Suppression de `AtomicBoolean running` (`ExternalResultsApplyTickScheduler`) — devenu redondant avec `@SchedulerLock` ; conservation du cooldown applicatif `lastRunBySlot` (à migrer en SQL/Redis dans un follow-up)

## Capabilities

### New Capabilities

- `cross-domain-contracts`: Localisation et règles d'évolution des contrats partagés entre domaines — enums dans `common.types.enums.*` (pattern `AuditAction`, `BetType`, `ApprovalRole`), value types métier dans `common.contracts.*` ; qui possède un type partagé, comment le déplacer, comment garantir qu'aucun import cross-domain ne traverse `*.infra.*` ou `*.domain.model.*` non documenté en `common`
- `scheduler-distributed-locking`: Convention transverse pour les schedulers backend — verrouillage distribué via ShedLock obligatoire, paramètres `lockAtMostFor` / `lockAtLeastFor`, table `shedlock`, alternatives interdites (guards in-memory pour multi-instance)

### Modified Capabilities

- `draw-lifecycle`: Extension avec le calcul correct de `scheduledAt` / `cutoffAt` dans `CreateDrawCommandHandler` (utilisation de `cutoffSec` du channel et timezone du slot), externalisation des paramètres lifecycle (`batchSize`, `lookaheadHours`, `lagHours`) en config, suppression des champs morts de `UpdateDrawCommand`, `Clock` injecté dans `DrawSearchCriteria.of()`, `@SchedulerLock` sur les 3 schedulers lifecycle
- `draw-result-ingestion`: Extension avec la migration `RefreshExternalResultsWindow` de `core.drawresult` vers `features.ops`, `enum.name()` obligatoire pour les statuts sérialisés, `ExternalPick.of(...)` pour validation, `OccurredAtResolver` unique implémentation du calcul `occurredAt`, `DrawResultStatus` réduit à 3 valeurs, `@SchedulerLock` sur les schedulers de fetch/apply/settle

## Impact

- **Code modifié** : `DrawAdminController`, `DrawResultsController`, `CreateDrawCommandHandler`, `UpdateDrawCommand`, `DrawLifeCycleTickScheduler`, `OpenDueDrawsCommand`, `CloseDueDrawsCommand`, `DrawProperties`, `FetchExternalResultsWindowCommandHandler`, `ApplyExternalResultsWindowCommandHandler`, `ExternalResultsApplyTickScheduler`, `ExternalResultsFetchTickScheduler`, `DrawSettleScheduler`, `OverrideDrawResultCommandHandler`, `RecordManualDrawResultCommand`, `DrawResultsOpsController`, `DrawSearchCriteria`, `DrawResult` (constructeur compat retiré), tous les consommateurs de `DrawSource`
- **Code créé** : `common.types.enums.DrawSource`, `common.config.draw.DrawResultsCommonProperties`, `features.ops.application.command.RefreshExternalResultsWindowCommand` + handler, configuration `LockProvider` ShedLock, ArchUnit tests pour les frontières, specs `cross-domain-contracts/spec.md` et `scheduler-distributed-locking/spec.md`
- **Code supprimé** : `core.drawresult.domain.model.DrawSource`, `core.drawresult.application.service.ResultSlotTimes`, `core.drawresult.application.command.RefreshExternalResultsWindowCommand` + handler, `AtomicBoolean running` dans `ExternalResultsApplyTickScheduler`, valeurs `VALID` / `OVERRIDDEN` / `INVALIDATED` de `DrawResultStatus`, champs `code` / `name` de `UpdateDrawCommand`
- **Migration DB** : table `shedlock` (Flyway, schema standard ShedLock) ; éventuellement `UPDATE draw_result SET status=...` si valeurs legacy présentes en prod
- **Dépendances Maven** : ajout `net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-jdbc-template` (versions pinées via `VERSIONS.md`)
- **BREAKING** : `DrawResultStatus` perd 3 valeurs (impact sérialisation JSON / persistance) — vérification + migration Flyway requise
- **Tests** : unitaires `CreateDrawCommandHandlerTest` (3 cas cutoffSec), ArchUnit (DrawSource, frontières infra), intégration multi-instance ShedLock
- **Docs** : `docs/decisions/draw-pipeline-decisions.md` (décisions 15-18), `DOMAIN_DRAW.md` §14-§15, `DOMAIN_DRAWRESULT.md` §14-§15
- **Non scope** : sécurité `@PreAuthorize` (brief 1), règles métier FINAL/SETTLED (brief 3), cleanup `core.uslottery` YAML (P2), hardcode NY+FL dans `DrawSettleScheduler` (P2), invalidation cache draw (change séparé)
