# OpenSpec change: add-ticket-delivery-feature

## Why

Mobile post-sale screen needs actions Email/SMS/WhatsApp in addition to Print. Sending is not printing; it is delivery through channels/providers/templates/retry.

## What

- Add `features.ticketdelivery`.
- Add endpoint unique with discriminant `channel`.
- Orchestrate Spring Boot validation + QueryBus + edge-service transport.

## Tasks

- [ ] Add `TicketDeliveryController`.
- [ ] Add `TicketDeliveryService`.
- [ ] Add `DeliverTicketRequest` / `DeliverTicketResponse`.
- [ ] Add `TicketDeliveryChannel` enum.
- [ ] Validate recipient per channel.
- [ ] Add `EdgeTicketDeliveryGateway`.
- [ ] Add edge internal contract draft.
- [ ] Add tests.

## Acceptance

- `POST /api/v1/tenant/tickets/{ticketId}/delivery` works for EMAIL MVP or returns clear unsupported for SMS/WHATSAPP if providers disabled.
- Print remains separate.
- Feature does not access JPA/repositories/entities.
- Spring Boot stays source of truth.
