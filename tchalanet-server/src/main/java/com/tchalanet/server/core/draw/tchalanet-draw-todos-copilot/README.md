# Tchalanet — TODO Copilot pour terminer `core.draw`

But : terminer proprement le domaine `core.draw` après les décisions de refactor slot-driven.

Ce dossier ne contient **pas d'OpenSpec**. Ce sont des checklists pratiques pour Copilot / pair-coding.

## Règles globales à respecter

- Java 25 / Spring Boot 4.
- `core.draw` ne doit pas dépendre de `DrawChannelEntity` ni de JPA catalog.
- Le draw stocke `draw_channel_id`, pas `result_slot_id` directement.
- La liaison slot se fait via : `draw.draw_channel_id -> draw_channel.result_slot_id`.
- Pas de provider/channelCode/external code dans `core.draw` lifecycle/apply/settle.
- Timestamps métier en `Instant` dans le domaine.
- Timezone uniquement pour calculs/projections/read model.
- Writes via command handlers `@UseCase` + `@TchTx`.
- Events publiés après commit avec `AfterCommit.run(...)`.
- Cache invalidation après commit.
- Query summaries doivent éviter N+1.
- RLS reste la barrière de sécurité, mais les filtres tenant sont acceptés pour performance.

## Ordre recommandé de travail

1. `domain/01-domain.md`
2. `persistence/01-persistence-critical.md`
3. `handlers/01-command-handlers.md`
4. `queries/01-query-handlers-and-summary.md`
5. `events-batch-scheduler/01-events.md`
6. `events-batch-scheduler/02-batch-and-schedulers.md`
7. `web/01-web-and-api.md`
8. `ports/01-ports.md`
9. `cache/01-cache.md`
10. `final-checklist.md`

## Définition de terminé

`core.draw` est terminé quand :

- le domaine est pur et strict ;
- apply result est slot-driven ;
- summaries sont optimisées ;
- handlers sont transactionnels et audités quand nécessaire ;
- events/cache/batch sont après commit/idempotents ;
- endpoints admin/ops ne gardent pas de legacy create/update/override vague ;
- settlement est soit proprement limité, soit marqué deferred jusqu'à `core.sales`.
