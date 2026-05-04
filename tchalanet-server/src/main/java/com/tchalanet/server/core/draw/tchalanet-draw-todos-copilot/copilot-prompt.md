# Prompt Copilot — terminer `core.draw`

Tu travailles dans le projet Tchalanet, domaine `core.draw`.

Lis d'abord `README.md` puis traite les TODO dans l'ordre :

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

Contraintes fortes :

- Ne crée pas d'OpenSpec.
- Fais des petits commits logiques par zone.
- Ne réintroduis pas `DrawChannelEntity` dans `DrawJpaEntity`.
- Ne mets pas `result_slot_id` dans `draw`.
- Toute logique apply doit passer via `draw_channel.result_slot_id`.
- Pas de provider/channelCode dans lifecycle/apply/settlement.
- Pas de N+1 dans les summaries.
- Tout write handler doit être `@UseCase` + `@TchTx`.
- Events après commit seulement.
- `force` doit toujours être explicite, guardé et audité si action sensible.

Pour chaque fichier modifié :

1. explique brièvement le problème corrigé ;
2. applique le changement minimal ;
3. ajoute ou ajuste les tests si possible ;
4. indique les TODO restants si tu ne peux pas les résoudre sans plus de contexte.

Priorité : fermer tous les P0 avant les P1.
