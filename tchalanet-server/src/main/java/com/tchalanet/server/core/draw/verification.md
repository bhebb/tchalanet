## 1) Flow "DrawChannel CRUD" (admin config)

**But**: Créer/modifier/désactiver un canal de tirage côté tenant.

**Classes à vérifier**

- Web
  - core.draw.infra.web.DrawChannelController
  - core.draw.infra.web.mapper.DrawChannelWebMapper
  - CreateDrawChannelRequest, UpdateDrawChannelRequest
  - DrawChannelResponse, DrawChannelSummaryResponse
- Application
  - CreateDrawChannelCommandHandler
  - UpdateDrawChannelCommandHandler
  - GetDrawChannelHandler
  - ListDrawChannelsHandler
  - ListActiveDrawChannelsHandler
- Ports
  - DrawChannelReaderPort
  - DrawChannelWriterPort
  - (si présent) DrawChannelQueryPort (attention aux doublons)
- Infra persistence
  - DrawChannelJpaEntity
  - JpaDrawChannelRepository (ou adapter)
  - Mapper entity↔domain (MapStruct ou manuel)
- ✅ Vérif rapide : active=false doit sortir de ListActive* mais rester dans List*.

## 2) Flow "Générer les draws" (calendar generation)

**But**: À partir des draw_channel, créer les draw pour un range (7–14 jours).

**Classes à vérifier**

- Scheduler
  - core.draw.infra.batch.DrawCalendarBatchScheduler
  - méthode generateNext7Days() (cron 05:00)
- Command + Handler
  - GenerateDrawsForRangeCommand
  - GenerateDrawsForRangeCommandHandler
- Ports
  - DrawChannelReaderPort (liste active)
  - DrawWriterPort (insert draws)
  - (souvent) DrawReaderPort pour vérifier existence / idempotence
- Domain
  - DrawChannel (days_of_week, draw_time, timezone, cutoff_sec)
  - Draw (scheduledAt/cutoffAt/status/source)
- ✅ Vérifs :
  - Idempotence : relancer generate ne doit pas dupliquer.
  - scheduled_at doit être en timestamptz cohérent avec timezone du channel.

## 3) Flow "Open due draws" (ouverture ventes)

**But**: Passer un draw en OPEN au moment voulu.

**Classes à vérifier**

- Scheduler
  - DrawCalendarBatchScheduler.openDueDraws() (actuellement fixedDelay PT5M)
- Command + Handler
  - OpenDueDrawsCommand
  - OpenDueDrawsCommandHandler
- Ports
  - DrawReaderPort (find openable ids/rows)
  - ou DrawLifecyclePort / DrawStorePort (selon ton design)
  - DrawWriterPort (update status)
- Projections (après refactor)
  - application.query.projection.OpenableDrawRow
- ✅ Vérifs :
  - requête DB : filtre sur fenêtre + LIMIT
  - transitions : SCHEDULED/PLANNED → OPEN autorisée.

## 4) Flow "Close due draws" (cutoff)

**But**: Fermer les ventes quand now >= cutoff.

**Classes à vérifier**

- Scheduler
  - DrawCalendarBatchScheduler.closeDueDraws() (actuellement fixedDelay PT1M)
- Command + Handler
  - CloseDueDrawsV1Command
  - CloseDueDrawsCommandHandlerV1
  - (⚠️ vérifier si une version "non V1" existe: doublon)
- Ports
  - DrawReaderPort / DrawLifecyclePort
  - projection DueToCloseRow
  - DrawWriterPort
- ✅ Vérifs :
  - transition : OPEN → CLOSED
  - pas de fermeture si locked=true (si tu veux cette règle v1)

## 5) Flow "Fetch results" (US Lottery) via Spring Batch

**But**: À heures fixes, récupérer un résultat externe et créer/mettre à jour draw_result.

**Classes à vérifier**

- Scheduler
  - core.draw.infra.batch.config.BatchJobScheduler
  - cron NY/FL (fetch + settle)
- Starter
  - DrawResultsJobStarter (params: ts, provider, channel_code, days_back, max_draws, dry_run, force)
- Job Config
  - FetchDrawResultsJobConfig
  - ItemReader<UUID> : FetchableDrawIdsReader
  - ItemProcessor : construit FetchAndApplyExternalResultCommand
  - ItemWriter : appelle handler (ou dry-run log)
- Use case
  - FetchAndApplyExternalResultCommand
  - FetchAndApplyExternalResultCommandHandler
- Ports
  - ExternalDrawResultPort ✅ (adapter uslottery)
  - DrawReaderPort
  - DrawWriterPort
  - DrawResultWriterPort
- ✅ Vérifs :
  - le handler construit DrawExternalQuery avec channelCode correct
  - "no result found" → log + no-op (pas de crash)
  - résultat trouvé → draw.applyResult + save draw_result + status RESULTED

## 6) Flow "Settle" (post-result)

**But**: Après RESULTED, settle = calcul payout/ledger/etc (même si v1 simple).

**Classes à vérifier**

- Starter
  - DrawSettleJobStarter
- Job Config
  - SettleDrawsJobConfig
  - SettleableDrawIdsReader
  - processor/writer
- Use case
  - SettleDrawsCommandHandler (ou équivalent)
- Ports vers payout/ledger si déjà branchés, sinon stub.
- ✅ Vérifs :
  - transition : RESULTED → SETTLED
  - refuse settle si pas de draw_result.

## 7) Flow "OPS endpoints" (manual trigger)

**But**: Forcer generate/fetch/settle via endpoints internes.

**Classes à vérifier**

- core.draw.infra.web.ops.DrawCalendarOpsController
  - déclenche generate/open/close (ou au moins generate)
- core.draw.infra.web.ops.DrawResultsOpsController
  - déclenche fetch job (force/dryRun/days_back/max_draws)
- ✅ Vérifs :
  - ça appelle bien JobStarter (pas le Job direct)
  - paramètres uniformes (keys identiques à scheduler)

## 8) Flow "Admin override result"

**But**: Admin override un résultat (manual/admin_override).

**Classes à vérifier**

- Web
  - DrawAdminController.overrideResult()
- Application
  - OverrideDrawResultCommand
  - OverrideDrawResultCommandHandler
- Ports
  - DrawResultWriterPort
  - DrawReaderPort / DrawWriterPort (si besoin de marquer draw status)
- ✅ Vérifs :
  - met draw_result.source=ADMIN_OVERRIDE, status OVERRIDDEN (ou VALID + flag)
  - conserve raw_payload si utile
  - audit fields overridden_by/at/reason.

## Ordre conseillé pour tes tests manuels

- DrawChannel list active ✅
- Generate next 7 days → crée des draws ✅
- Open due → passe OPEN ✅
- Close due → passe CLOSED ✅
- Fetch results (via OPS ou cron simulé) → crée draw_result + RESULTED ✅
- Settle → SETTLED ✅
- Override → remplace draw_result proprement ✅
