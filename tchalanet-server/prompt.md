1) 2) Domaine DRAW — Command Handlers (MVP)
      A. Scheduling / Ops
1) ScheduleDrawsCommand

But: générer les draws (J..J+N) pour un tenant à partir des draw_channel actifs.

Input

tenantId

fromDate (date locale, souvent aujourd’hui)

days (ex: 7)

Règles

ne créer que pour les channels actifs

respecter days_of_week

calculer scheduled_at = date locale + draw_time dans timezone

calculer cutoff_at = scheduled_at - cutoff_sec

idempotent via unique (tenant, channel, draw_date)

Ports OUT

DrawChannelRepositoryPort.findActiveByTenant(tenantId)

DrawRepositoryPort.upsertBatch(draws)

TimeProviderPort.now() (si fromDate implicite)

2) OpenDueDrawsCommand

But: passer SCHEDULED → OPEN pour les draws dont scheduled_at <= now < cutoff_at, locked=false.

Règles

lock pessimiste/optimiste pour éviter double-open en cluster

idempotent: si déjà OPEN, no-op

Ports OUT

DrawRepositoryPort.findOpenDue(tenantId, now, limit)

DrawRepositoryPort.markOpen(drawIds, openedAt)

LockPort.tryLockDraw(drawId) (optionnel si tu gères via DB locked)

3) CloseDueDrawsCommand

But: OPEN → CLOSED quand cutoff_at <= now.

Ports OUT

DrawRepositoryPort.findCloseDue(tenantId, now, limit)

DrawRepositoryPort.markClosed(drawIds, closedAt)

B. Results (global + attach)
4) UpsertDrawResultCommand

But: créer/met à jour un draw_result global (provider/slot/occurred_at unique).

Input

provider, providerSlot, occurredAt

source_result (Json)

haiti_result (Json)

raw_payload (Json)

status (PROVISIONAL/FINAL/ERROR)

source (API/MANUAL/IMPORT)

quality/sourceHash/overrideReason

Règles

idempotent par uq(provider, slot, occurred_at)

si FINAL, refuser de rétrograder vers PROVISIONAL (sauf override explicite ops)

valider la présence lot1..lot4 (check DB + validation app)

Ports OUT

DrawResultRepositoryPort.upsert(drawResult)

Ce handler vit dans draw car lié au cycle de tirage, mais son scope est “global”.

5) AttachResultToDrawCommand

But: relier un draw CLOSED à un draw_result (automatique ou ops).

Input

tenantId

drawId

drawResultId (ou provider+slot+occurredAt si tu veux lookup)

mode AUTO|OPS + reason

Règles

draw doit être CLOSED (MVP strict)

draw.locked must be false

si draw déjà a draw_result_id → no-op sauf mode OPS (override)

set draw.status = RESULTED, resulted_at = now, result_source, result_override_reason, result_overridden_at (si override)

Ports OUT

DrawRepositoryPort.getForUpdate(drawId) (row lock)

DrawResultRepositoryPort.get(drawResultId)

DrawRepositoryPort.attachResult(drawId, drawResultId, meta…)

6) ApplyResultsBatchCommand

But: pour un tenant, rattacher automatiquement les résultats aux draws CLOSED manquants.

Input

tenantId

date range (optionnel)

limit

Algorithm

sélectionner draws CLOSED AND draw_result_id IS NULL

pour chacun: déterminer provider + providerSlot attendu depuis draw_channel (code MID/EVE ou champ futur)

lookup draw_result par (provider, slot, occurredAt/date)

attach via AttachResultToDrawCommand (réutilisation interne)

Ports OUT

DrawRepositoryPort.findClosedMissingResult(tenantId, limit)

DrawChannelRepositoryPort.get(drawChannelId) (ou fetch join dans query)

DrawResultRepositoryPort.findMatch(provider, slot, window) (ex: occurred_at dans fenêtre)

C. Settlement (payout orchestration)
7) SettleDrawCommand

But: RESULTED → SETTLED + déclencher payout (tickets/payout/ledger).

Input

tenantId, drawId

Règles

draw must be RESULTED

draw_result must be FINAL (ou politique configurable; MVP: FINAL only)

settlement idempotent (si déjà SETTLED, no-op)

lock draw during settlement

Ports OUT

DrawRepositoryPort.getForUpdate(drawId)

DrawResultRepositoryPort.get(drawResultId)

TicketQueryPort.findTicketsByDraw(drawId) (depuis sales domain)

PayoutPort.settleTickets(draw, drawResult, tickets) (ou Command vers payout slice)

DrawRepositoryPort.markSettled(drawId, settledAt)

Ici tu vois l’intégration inter-domaines : draw orchestre, mais ne calcule pas l’argent tout seul.

3) Domaine DRAW — Query Handlers (API / UI)
1) ListDrawChannelsQuery

by tenantId, active=true

retourne code/name/time/provider + jeux associés (via draw_channel_game)

2) ListUpcomingDrawsQuery

draws SCHEDULED/OPEN sur X jours

pour home privée / opérateur

3) ListRecentResultsQuery (public)

draws RESULTED/SETTLED sur période (ex: 7 jours)

join draw_result pour afficher haiti_result (lot1..lot4)

4) GetDrawDetailsQuery

draw + channel + draw_result json + metadata

4) Domaine GAME — Handlers minimaux
   Commands

UpsertGameCommand (admin platform / seed, rarement via UI)

UpsertTenantGameCommand (enable/disable + odds/multipliers)

Queries

ListGamesQuery (global)

ListTenantGamesQuery (tenant)

5) Ports (interfaces) à définir maintenant
   Ports OUT (persistence)

GameRepositoryPort

TenantGameRepositoryPort

DrawChannelRepositoryPort

DrawChannelGameRepositoryPort

DrawRepositoryPort

DrawResultRepositoryPort

Méthodes clés (draw)

findActiveChannels(tenantId)

findClosedMissingResult(tenantId, limit)

findOpenDue(tenantId, now, limit)

findCloseDue(tenantId, now, limit)

getForUpdate(drawId) (SELECT … FOR UPDATE)

attachResult(drawId, drawResultId, meta)

markOpen/markClosed/markSettled

Méthodes clés (draw_result)

upsertByUniqueKey(provider, slot, occurredAt, …)

findByUniqueKey(provider, slot, occurredAt)

findMatch(provider, slot, from,to) (fenêtre)

Ports OUT (infra/time/lock)

TimeProviderPort (Instant now())

DrawLockPort (optionnel si tu utilises locked en DB)

ResultFetcherPort (NY/FL/GA) (plus tard)

Ports inter-domaines

TicketReaderPort / TicketQueryPort

PayoutOrchestratorPort (ou CommandGateway vers payout slice)

6) Adapters (infra) à préparer
   Persistence adapters

*JpaRepository (Spring Data)

*PersistenceAdapter implements *RepositoryPort

MapStruct mapper domain <-> JPA (si tu fais domain objects)

Web adapters

Controllers:

/ops/draws/schedule

/ops/draws/open-due

/ops/draws/close-due

/ops/draws/apply-results

/ops/draws/{id}/attach-result

/public/results (query)

/tenant/games (query)

7) Règles d’implémentation à respecter (anti-bugs)

Idempotence partout (ON CONFLICT, “already in state ⇒ no-op”).

Transactions :

Open/Close/Attach/Settle doivent être transactionnels.

Locking :

soit SELECT … FOR UPDATE (recommandé),

soit champ locked + update atomique (moins clean mais OK).

Post-settle :

MVP: attach/override interdit après SETTLED (sinon reversal).

8) Next concrete step (ce que je te propose pour le prochain message)

Je peux te produire un squelette code complet (interfaces + handlers + persistence adapters) pour :

ScheduleDrawsCommandHandler

OpenDueDrawsCommandHandler

CloseDueDrawsCommandHandler

UpsertDrawResultCommandHandler

ApplyResultsBatchCommandHandler

AttachResultToDrawCommandHandler

…avec signatures, transactions, et pseudo-queries JPA.



✅ Seed minimal (pour que l’app démarre)

Seed game (HT_*)

Seed tenant_game pour tchalanet (+ flags odds defaults)

Seed draw_channel HT_NY_MID / HT_NY_EVE / HT_FL_MID / HT_FL_EVE
(avec flags = mapping source + projection lots)

Objectif: pouvoir lister les channels et vendre sans erreur.

2) 🔌 Adapter l’ingestion “provider → draw_result”

Tu dois maintenant implémenter le pipeline qui remplit draw_result pour les channels HT.

À faire

Fetch: récupérer Pick3/Pick4 (NY/FL) comme avant

Transform: produire haiti_result (lot1/lot2/lot3/lot4)

Upsert draw_result: unique (draw_channel_id, draw_date)

Attach: mettre draw.draw_result_id quand dispo

Règle d’or

Le payout lit haiti_result uniquement

source_result + raw_payload = audit/debug

3) 🗓️ Jobs “Draw lifecycle” (OPEN/CLOSE/RESULTED)

Tu avais déjà les indexes pour OpenDue/CloseDue etc. Maintenant :

Planner (daily): créer les draw SCHEDULED (ex: J0..J+2) par channel

OpenDue: SCHEDULED → OPEN quand on atteint la fenêtre

CloseDue: OPEN → CLOSED à cutoff_at

ResultApply:

si résultat externe reçu → draw_result est prêt

draw CLOSED → RESULTED (set draw_result_id)

Settle: RESULTED → SETTLED après calcul payout (ou job séparé)

Objectif: “une table draw vivante” que le POS peut viser.

4) 🧾 Refactor Ticket (c’est le gros impact)

Tu dois aligner ticket / ticket_line avec le nouveau cœur :

Ticket

ticket.draw_id = draw HT (obligatoire)

terminal/session OK

TicketLine

ticket_line.game_code = HT_* (vendable)

bet_type devient inutile (souvent supprimable)

normaliser selection (padding, tri mariage)

index unique optionnel (ticket_id, game_code, selection) + upsert

Objectif: vendre sur draw HT, pas sur “US games”.

5) 💰 Payout v1 (ultra simple)

Implementer un PayoutService qui prend :

draw_id

draw_result.haiti_result

ticket_lines

et calcule :

WON/LOST

potential_payout / total_payout

puis ticket.status évolue
