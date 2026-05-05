# OpenSpec change: add-sales-read-views

## Why

Sales/print/draw public read flows ont besoin de projections stables sans gonfler les repositories domain ni faire des assemblages cross-domain dans les handlers.

## What

- Ajouter `v_ticket_summary`.
- Ajouter `v_ticket_print`.
- Ajouter `v_draw_summary`.
- Ajouter adapters JDBC/readers dédiés.
- Garder ces vues read-only.

## Tasks

- [ ] Flyway migration for views.
- [ ] `TicketSummaryReaderPort`.
- [ ] `JdbcTicketSummaryReaderAdapter`.
- [ ] `TicketPrintReaderPort`.
- [ ] `JdbcTicketPrintReaderAdapter`.
- [ ] `DrawSummaryReaderPort`.
- [ ] `JdbcDrawSummaryReaderAdapter`.
- [ ] Tests RLS.

## Acceptance

- List tickets uses `v_ticket_summary`.
- Print uses `v_ticket_print` + `ticket_line`.
- Draw summary queries use `v_draw_summary`.
- No command handler uses DB views for mutation.
