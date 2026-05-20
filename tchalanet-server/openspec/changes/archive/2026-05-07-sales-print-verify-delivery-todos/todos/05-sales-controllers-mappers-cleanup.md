# TODO 05 — Sales controllers / mappers cleanup

## Objectif

Éviter un gros controller et corriger `TicketWebMapper`.

## Controllers cibles

```text
TicketSaleController
  POST /api/v1/tenant/tickets
  POST /api/v1/tenant/tickets/{id}/cancel
  POST /api/v1/tenant/tickets/{id}/approve
  POST /api/v1/tenant/tickets/{id}/reject

TicketQueryController
  GET /api/v1/tenant/tickets
  GET /api/v1/tenant/tickets/{id}

TicketPrintController
  GET /api/v1/tenant/tickets/{id}/print/pdf
  GET /api/v1/tenant/tickets/{id}/print/escpos
  GET /api/v1/tenant/tickets/{id}/qr

TicketAdminController
  POST /api/v1/admin/tickets/{id}/override-result
  GET /api/v1/admin/tickets/export
```

Public verify sort vers `features.ticketverify`.
Delivery sort vers `features.ticketdelivery`.

## P0 — `TicketWebMapper`

- [ ] `performedBy` toujours depuis `TchRequestContext`, jamais request body.
- [ ] `performedAt` depuis `Clock`/handler, pas request body sauf import spécial superadmin.
- [ ] Override result ne doit pas accepter `saleStatus`/`settlementStatus` arbitraires.
- [ ] Override accepte seulement : `resultStatus WON/LOST`, `totalPayout`, `reason`.
- [ ] Remplacer Spring `PageRequest` par `TchPageRequest` dans `ListTicketsQuery`.
- [ ] Status invalide en query param => 400, pas silently ignored.
- [ ] Ne pas réutiliser `TicketResponse` interne pour public verify.
- [ ] `terminalId` depuis body doit être validé contre session/context, sinon vient du contexte device/session.

## P0 — List query

- [ ] `ListTicketsQueryHandler` side-effect free.
- [ ] Pas d’audit direct dans query handler.
- [ ] Audit read-many si nécessaire au controller/aspect/QueryBus decorator.
- [ ] Utiliser `TicketSummaryReaderPort` + `v_ticket_summary`.

## P0 — Internal/public responses

- [ ] `TicketResponse` interne tenant/admin peut contenir IDs nécessaires.
- [ ] Public verify response ne contient jamais internal IDs.
- [ ] Print view interne ne doit pas être exposée comme public verify response.

## P1 — Split mapper

Si `TicketWebMapper` reste trop gros :

```text
TicketCommandWebMapper
TicketQueryWebMapper
TicketResponseWebMapper
TicketPrintWebMapper
```
