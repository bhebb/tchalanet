# Prompt task 01 — DB views

Implémente seulement les vues DB P0 et les readers/adapters nécessaires.

Créer :

- `v_ticket_summary`
- `v_ticket_print`
- `v_draw_summary`

Puis créer/adapter :

- `TicketSummaryReaderPort`
- `TicketPrintReaderPort`
- `DrawSummaryReaderPort`
- adapters JDBC/projection correspondants

Respecte RLS, `tenant_id`, `deleted_at`. Les vues sont read-only et ne doivent pas être utilisées par les command handlers.
