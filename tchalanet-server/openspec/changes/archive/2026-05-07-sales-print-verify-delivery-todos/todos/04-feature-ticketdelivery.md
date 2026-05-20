# TODO 04 — Feature Ticket Delivery

## Objectif

Créer `features.ticketdelivery` pour envoyer un ticket/reçu par email/SMS/WhatsApp depuis l’écran mobile post-sale.

## Décision

```text
Print = core.sales
Delivery = features.ticketdelivery
Transport = tchalanet-edge-service
```

Endpoint unique avec discriminant :

```text
POST /api/v1/tenant/tickets/{ticketId}/delivery
```

Body :

```json
{
  "channel": "EMAIL",
  "recipient": "client@example.com",
  "locale": "fr",
  "includePdf": false,
  "includeVerificationLink": true
}
```

## Structure recommandée

```text
features.ticketdelivery/
  web/
    TicketDeliveryController
  app/
    TicketDeliveryService
    TicketDeliveryMapper
  model/
    DeliverTicketRequest
    DeliverTicketResponse
    TicketDeliveryChannel
    TicketDeliveryStatus
```

Pas de repositories/JPA/entities.

## Flow

```text
TicketDeliveryController
  -> TicketDeliveryService
  -> QueryBus GetTicketPrintViewQuery / GetTicketReceiptQuery
  -> build delivery payload
  -> EdgeDeliveryGateway
  -> edge-service /internal/delivery
```

## P0 — Controller/service

- [ ] Créer `TicketDeliveryController`.
- [ ] Route : `POST /api/v1/tenant/tickets/{ticketId}/delivery`.
- [ ] Lire tenant/user/context via Spring/Tch context.
- [ ] Valider permission d’envoyer un ticket.
- [ ] Appeler service feature.
- [ ] Auditer la demande d’envoi selon politique audit.

## P0 — Model

```java
public enum TicketDeliveryChannel {
  EMAIL,
  SMS,
  WHATSAPP
}
```

```java
public record DeliverTicketRequest(
    TicketDeliveryChannel channel,
    String recipient,
    String locale,
    Boolean includePdf,
    Boolean includeVerificationLink
) {}
```

```java
public record DeliverTicketResponse(
    TicketDeliveryStatus status,
    TicketDeliveryChannel channel,
    String ticketCode,
    String publicCode,
    String message
) {}
```

## P0 — Validation par channel

- [ ] `EMAIL`: recipient email valide, PDF optionnel possible.
- [ ] `SMS`: recipient téléphone E.164 recommandé, PDF interdit/ignoré, link recommandé.
- [ ] `WHATSAPP`: recipient téléphone E.164 recommandé, link MVP, attachment plus tard.
- [ ] `includeVerificationLink` default true pour SMS/WhatsApp.
- [ ] Refuser channel inconnu avec 400.

## P0 — Edge gateway

Créer un gateway côté Spring Boot :

```text
EdgeTicketDeliveryGateway
```

Responsabilité : appeler `tchalanet-edge-service` internal endpoint.

- [ ] HMAC/internal auth si déjà prévu.
- [ ] requestId/idempotency key.
- [ ] Timeout court + erreurs propres.
- [ ] Spring Boot reste source de vérité.

## P1 — Edge service module

Dans `tchalanet-edge-service` :

```text
src/modules/delivery/
  delivery.routes.ts
  delivery.service.ts
  email.provider.ts
  sms.provider.ts later
  whatsapp.provider.ts later
  templates/ticket-receipt.*
```

- [ ] Email MVP.
- [ ] SMS/WhatsApp stub ou feature flag si provider absent.
- [ ] Logs de livraison.
- [ ] Retry technique.
- [ ] Anti-spam/idempotency delivery.

## Mobile receipt screen

Le mobile affiche :

```text
TicketReceiptScreen / SaleSuccessReceiptScreen
  [Print] [Email] [SMS] [WhatsApp] [Copier lien] [QR]
```

- [ ] Print appelle `/print/escpos` ou `/print/pdf`.
- [ ] Email/SMS/WhatsApp appellent `/delivery` avec `channel`.
- [ ] Copier lien utilise public verify URL.
