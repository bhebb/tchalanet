# TODO 01 — DB Views P0

## Objectif

Créer les read models DB stables :

```text
v_ticket_summary
v_ticket_print
v_draw_summary
```

## Fichiers probables

```text
tchalanet-server/src/main/resources/db/migration/V2026xxxx__create_ticket_draw_read_views.sql
```

## Tâches

### P0.1 — Créer `v_ticket_summary`

- [ ] Créer une vue normale, non materialized.
- [ ] Inclure tenant, statuts, montants, ticket code/public code.
- [ ] Inclure terminal label.
- [ ] Inclure outlet id/name.
- [ ] Inclure draw id/date/scheduledAt.
- [ ] Inclure drawChannel id/code/label/timezone.
- [ ] Filtrer `ticket.deleted_at is null`.
- [ ] Ne pas inclure les lignes.

### P0.2 — Créer `v_ticket_print`

- [ ] Créer une vue header enrichie.
- [ ] Ne pas joindre `ticket_line` dans la vue header.
- [ ] Inclure publicCode et champs nécessaires pour verify URL.
- [ ] Inclure terminal label, outlet name/city.
- [ ] Inclure draw + drawChannel labels/time/timezone.
- [ ] Filtrer `ticket.deleted_at is null`.

### P0.3 — Créer `v_draw_summary`

- [ ] Inclure draw lifecycle timestamps.
- [ ] Inclure drawChannel.
- [ ] Inclure resultSlot.
- [ ] Inclure drawResult si appliqué.
- [ ] Inclure Haiti/pick fields nécessaires au public/dashboard.
- [ ] Filtrer `draw.deleted_at is null` si applicable.

### P0.4 — RLS/tests

- [ ] Vérifier que les vues respectent RLS.
- [ ] Tester tenant A ne voit pas tenant B.
- [ ] Si PostgreSQL compatible, considérer `security_invoker = true` pour les vues.
- [ ] Ajouter tests SQL/integration si infrastructure existante.

## Notes

Les vues DB sont autorisées ici comme read models/projections optimisées. Elles ne doivent jamais être utilisées pour write/settlement/transition métier.
