-- Drop legacy view v_ticket_print
-- sales_ticket_print_header_v is the canonical view for ticket printing.
-- Public verification now uses direct joins.

DROP VIEW IF EXISTS v_ticket_print;
