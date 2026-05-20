# Prompt task 04 — Feature ticketdelivery

Créer `features.ticketdelivery` comme vertical slice.

Endpoint :

```text
POST /api/v1/tenant/tickets/{ticketId}/delivery
```

Body :

```json
{
  "channel": "EMAIL | SMS | WHATSAPP",
  "recipient": "...",
  "locale": "fr",
  "includePdf": false,
  "includeVerificationLink": true
}
```

La feature doit :

- valider channel/recipient
- appeler `core.sales` via QueryBus pour `TicketPrintView`/receipt
- construire payload Edge
- appeler `tchalanet-edge-service` via gateway

Ne pas générer la vérité ticket dans la feature. Ne pas lire JPA.
