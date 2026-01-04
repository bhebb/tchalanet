Domaine : Draw

1. Rôle du domaine
Responsabilité principale

Gérer le cycle de vie complet des tirages (draws) : planification, ouverture/fermeture des ventes, rattachement des résultats, et clôture financière (settlement).

Ce que le domaine fait

- Maintient le calendrier des tirages à partir des DrawChannel.
- Génère les Draw futurs (SCHEDULED).
- Pilote les transitions de statut : SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED (+ CANCELED).
- Stocke et met à jour les résultats canoniques dans DrawResult (global, non-tenant).
- Attache un résultat canonique à un tirage de tenant (draw.draw_result_id).
- Déclenche/contrôle les jobs batch et opérations ops (manual triggers).

Ce que le domaine ne fait pas

- Vente / validation des tickets (domaine ticket).
- Calcul des gains / paiements / ledger (domaine payout / ledger), même si le draw “SETTLED” en dépend.
- Gestion de la configuration de catalogue (game/tenant_game). DrawChannel est la configuration “draw-level”, mais la gouvernance catalogue/produit reste ailleurs.

2. Modèle métier (agrégats / entités)
2.1 Agrégats / entités principales

DrawChannel (tenant-scoped)

Représente un canal de tirage (ex: US_NY_NUM3_MID) avec un calendrier et un mapping provider.

Champs clés :

- tenant_id, code
- timezone, draw_time, days_of_week
- cutoff_sec
- mapping externe : external_provider, external_game_key, external_channel_code
- active, sort_order

Le DrawChannel définit “quand les draws doivent exister”.

Draw (tenant-scoped)

Représente un tirage “opérationnel” pour un tenant, support de vente + audit.

Champs clés :

- Identité : id, tenant_id, draw_channel_id

Clés temporelles :

- draw_date (date locale du channel, clé métier)
- scheduled_at (instant officiel du tirage)
- cutoff_at (instant de fermeture ventes)

Statuts : SCHEDULED | OPEN | CLOSED | RESULTED | SETTLED | CANCELED

Timestamps d’audit : opened_at, closed_at, resulted_at, settled_at, canceled_at

FK résultat : draw_result_id (nullable)

locked, system_generated, cancel_reason

Le Draw pilote “la vie commerciale et opérationnelle” d’un tirage.

DrawResult (canonique, non-tenant)

Vérité canonique des résultats par (channel_code, draw_date).

Champs clés :

- channel_code, draw_date (unique)
- numbers_main, numbers_extra, occurred_at
- quality (ex: SUSPECT, COMPLETE)
- status (ex: VALID, OVERRIDDEN, REVOKED si tu veux évoluer)
- source, source_hash, raw_payload, override_reason, fetched_at

Le DrawResult est “la vérité résultat” et sert à tous les tenants.

2.2 Value Objects / Enums

- DrawStatus : SCHEDULED, OPEN, CLOSED, RESULTED, SETTLED, CANCELED
- ResultQuality : SUSPECT, COMPLETE
- DrawSource / ResultSource : NY_PROVIDER, FL_PROVIDER, OPS_OVERRIDE, etc.

2.3 Invariants métier
Invariants temporels (Draw)

- scheduled_at et cutoff_at sont fixés à la génération.
- cutoff_at < scheduled_at obligatoire.
- draw_date = date locale du DrawChannel.timezone (cohérence calendrier).

Invariants de statut (state machine)

Transitions autorisées :

- SCHEDULED → OPEN
- OPEN → CLOSED
- CLOSED → RESULTED
- RESULTED → SETTLED
- SCHEDULED|OPEN|CLOSED → CANCELED

Chaque transition écrit son timestamp correspondant (opened_at, closed_at, etc.).

- SETTLED interdit sans draw_result_id.
- CANCELED interdit après SETTLED.

Invariants résultats (DrawResult)

- Écrire COMPLETE seulement, sauf force=true.
- Interdire COMPLETE → SUSPECT (jamais downgrade).
- Autoriser SUSPECT → COMPLETE.
- Les overrides doivent être traçables (override_reason, updated_by).

Multi-tenant

- Draw et DrawChannel sont tenant-scoped (RLS attendu).
- DrawResult est global (pas de RLS tenant).

3. Cas d’utilisation (ports d’entrée)
3.1 GenerateDrawsForRange

- But : générer les draws à venir d’un tenant à partir des DrawChannel.
- Inputs : tenantId, from, to, dryRun, force
- Output : stats (created, skipped, conflicts)

3.2 OpenDueDraws

- But : passer des draws SCHEDULED → OPEN selon policy d’ouverture.
- Inputs : now, limit, openHorizonHours, openLagHours, dryRun
- Output : count opened / skipped

3.3 CloseDueDraws

- But : passer des draws OPEN → CLOSED quand cutoff_at <= now.
- Inputs : now, limit, dryRun (+ option channelCodes/slot)
- Output : count closed

3.4 FetchExternalResults (global)

- But : récupérer les résultats externes et alimenter DrawResult canonique.
- Inputs : provider, draw_date, channel_codes, max_draws, force, dry_run, request_id, triggered_by
- Output : run_id, hashes, counts (parsing, upserts)

3.5 ApplyExternalResults

- But : attacher DrawResult aux Draw (tenant), passer CLOSED → RESULTED.
- Inputs : run_id OR (provider, draw_date, query_hash), channel_codes, force, dry_run
- Output : count attached / resulted / waiting

Policy MVP recommandée :

- Attacher et passer RESULTED seulement si DrawResult.quality == COMPLETE (sinon “waiting”).

3.6 SettleResultedDraws

- But : déclencher la clôture “draw-level” après que les paiements soient effectués (payout/ledger).
- Inputs : tenantId?, limit, dryRun (+ option channelCodes)
- Output : count settled

4. Ports de sortie (application.port.out)
4.1 DrawStorePort

- bulkInsert(List<NewDrawRow>)
- exists(tenantId, channelId, scheduledAt) (ou mieux : draw_date)

4.2 DrawLifecyclePort

- findOpenable(now, limit, openHorizonHours, openLagHours)
- bulkOpen(drawIds, openedAt)
- findDueToClose(now, limit)
- bulkClose(drawIds, closedAt)

4.3 DrawResultStorePort (global)

- upsertResult(DrawResultUpsert) selon policy qualité
- findByChannelCodesAndDate(channelCodes, drawDate)

4.4 ExternalFetchLogPort (tech)

- insert(fetchLogRow)
- findLast(provider, drawDate, queryHash) / findByRunId(runId)

5. Jobs batch & déclenchement (orchestration)
5.1 Principe général

- Generate/Open/Close/Apply/Settle sont tenant-scoped.
- FetchExternalResults est global (un seul run alimente tous les tenants).
- Les jobs sont déclenchés par :
  - schedulers “slots” (NY/FL)
  - endpoints ops (manual)
  - orchestrateur ops (fetch → apply)

5.2 Scheduling recommandé (slots)

Par slot (ex: NY MID) :

- close (cutoff-2m)
- fetch (T+5m)
- apply (T+8m)
- settle (T+15m)
- open = daily (matin) + catch-up optionnel.

6. Contrôle d’exécution : pause par job / pause globale / gate infra
6.1 Objectif

Pouvoir :

- arrêter un job précis (provider down, bug parsing, etc.)
- arrêter tous les jobs en cas de panne infra (DB/Redis/etc.)
- sans redémarrer l’application (pilotage par UI)

6.2 Mécanisme

Via app_setting (GLOBAL / namespace batch) :

- batch.enabled (BOOLEAN)
- batch.jobs.generate.enabled
- batch.jobs.open.enabled
- batch.jobs.close.enabled
- batch.jobs.fetch.enabled
- batch.jobs.apply.enabled
- batch.jobs.settle.enabled

Optionnel :

- batch.windows.* (STRING) pour ajuster les fenêtres runtime.

Chaque scheduler / ops trigger applique la logique :

- si batch.enabled=false → skip
- si batch.jobs.<job>.enabled=false → skip
- si infra down (health gate) → skip et/ou auto-disable

6.3 Gestion erreur infra (MVP)

Gate “infra health” : si DB indispo / batch repo indispo → ne lancer aucun job.
Option pro simple : après N échecs consécutifs, écrire batch.enabled=false (auto-stop) + log/alert.
Reprise : manuelle via UI (éviter flapping au début).

7. Intégration avec les autres domaines

- ticket : dépend de Draw (OPEN/CLOSED) pour autoriser la vente. associe les tickets à un draw donné.
- payout/ledger : dépend de DrawResult et de l’attachement Draw.draw_result_id. SETTLED ne doit être atteint qu’après ledger/payout idempotents.
- tenantconfig / catalogue : gère game, tenant_game ; draw_channel s’appuie sur ces entités.

8. Notes techniques
Packages recommandés (alignés avec ton code)

- core.draw.domain.model : Draw, DrawStatus, DrawResult (domain), transitions
- core.draw.application.command : commands + handlers (generate/open/close/apply/settle)
- core.draw.application.query : queries (upcoming, history, projections)
- core.draw.application.port.in/out
- core.draw.infra.persistence : JPA entities + repos + adapters
- core.draw.infra.batch : job starters + job configs (fetch/apply/settle)
- core.draw.infra.web.ops : endpoints ops
- common.settings : AppSettingsResolver + flags batch

