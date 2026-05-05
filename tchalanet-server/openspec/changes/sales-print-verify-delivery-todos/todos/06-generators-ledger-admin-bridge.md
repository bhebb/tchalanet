# TODO 06 — Generators / Ledger / Admin bridge

## Public/internal ticket codes

Générateurs validés MVP :

```text
CrockfordPublicCodeGenerator
TimeBasedTicketNumberGenerator
```

## P0 — Unicité

- [ ] `public_code` global unique en DB.
- [ ] `(tenant_id, ticket_code)` unique en DB.
- [ ] Les générateurs ne garantissent pas l’unicité seuls.
- [ ] Ajouter retry transactionnel autour du `save` sur violation de contrainte.
- [ ] Collision après max retries => 503 ou exception métier mappée.

## P0 — SalesLedgerListener

Actuel pattern after-commit OK, mais ajouter idempotence.

- [ ] Ajouter `ProcessedEventPort`.
- [ ] Consumer key : `ledger.record_ticket_sale`.
- [ ] `markProcessedIfAbsent(event.eventId(), CONSUMER)`.
- [ ] Ledger command/write idempotent par `source_event_id` ou `(tenantId,ticketId,entryType)`.

## P0 — SalesTicketAdminAdapter

Adapter bridge acceptable v1, mais :

- [ ] Si session list vide => retourner stats zéro immédiatement.
- [ ] Count queries doivent filtrer `deleted_at is null`.
- [ ] Clarifier RLS context requirement pour close-day stats.
- [ ] `refuseNewTickets` / `allowNewTickets` no-op : retirer ou fail fast `UnsupportedOperationException`.
- [ ] Plus tard, remplacer 6 counts par query groupée.
- [ ] Plus tard, ajouter montants close-day : total stake, voided amount, winning amount, paid amount, net sales.
