# Feature TicketVerify

> Surface publique de vérification de ticket par code public.  
> Anonymous — pas d'authentification requise.  
> Rate-limitée par IP.

---

## Rôle

Permet à n'importe qui (client, vendeur, admin) de vérifier l'authenticité et l'état d'un ticket en connaissant son `publicCode`.  
Utilisé depuis le site public, l'app mobile (bouton "Vérifier"), et les terminaux POS (`POST /tenant/cashier/tickets/verify`).

---

## Endpoint

```http
GET /public/tickets/{publicCode}/verify?verificationCode=<code>
```

- `publicCode` : code Crockford 12 chars, ex: `40CP-JBMR` — extrait de l'URL ou du QR code
- `verificationCode` : code de vérification complémentaire (4-32 chars)
- Réponse cachée `no-store`, header `X-Robots-Tag: noindex, nofollow`
- Rate limit : configurable par IP via `PublicTicketRateLimitProperties`

---

## Réponse — `TicketVerifyResponse`

```json
{
  "publicCode": "40CP-JBMR",
  "displayCode": "40CP-JBMR",
  "status": "WON_CLAIMABLE",
  "totalAmount": { "amount": "15.00", "currency": "HTG" },
  "winningAmount": { "amount": "150.00", "currency": "HTG" },
  "placedAt": "2026-05-30T14:00:00Z",
  "outlet": { "name": "PDV Centre-Ville", "address": "..." },
  "draw": { "channelName": "HT_NY_MID", "channelLabel": "Haïti • New York Midi", "drawDate": "2026-05-30", "scheduledAt": "..." },
  "lines": [
    {
      "lineNumber": 1,
      "gameDisplayName": "Bolet",
      "betTypeLabel": "Match 1 2D",
      "selection": "11",
      "stake": { "amount": "15.00", "currency": "HTG" },
      "potentialPayout": { "amount": "150.00", "currency": "HTG" },
      "promotional": false,
      "promotionLabel": null
    }
  ]
}
```

---

## `CustomerTicketStatus` — états exposés publiquement

| Valeur | Signification |
|---|---|
| `AWAITING_RESULT` | Ticket vendu, tirage pas encore publié |
| `LOST` | Résultat connu — pas gagnant |
| `WON_CLAIMABLE` | Gagnant — paiement non encore effectué |
| `WON_PAID` | Gagnant — paiement enregistré |
| `CANCELLED` | Annulé dans la fenêtre (3 min) |
| `VOIDED` | Annulé administrativement |
| `CORRECTED` | Résultat corrigé après override |
| `EXPIRED` | Délai de réclamation dépassé |

Ces statuts sont une projection publique — ils ne correspondent pas 1:1 à `TicketSaleStatus` + `TicketResultStatus`.

---

## Rate limiting

`PublicTicketRateLimiter` : in-memory par IP (token bucket).  
`429 Too Many Requests` si dépassement.  
**TODO** : migrer vers Redis pour le multi-instance.

---

## Frontières

- Appelle `core.sales` via `QueryBus` — jamais directement les repositories
- Les DTOs ne contiennent pas d'UUID internes (`ticketId`, `drawId`, `tenantId`, `addressId`)
- L'outlet est masqué partiellement (`TicketVerifyOutletView`)
- Les lignes promotionnelles affichent `promotional: true` + `promotionLabel` mais pas les IDs promotion
- La vérification depuis le POS cashier passe par `POST /tenant/cashier/tickets/verify` (authentifiée) — retourne les actions disponibles (`EXECUTE_PAYOUT`, etc.)

---

## Références

- Statuts domaine : `core/sales/DOMAIN_SALES.md §CustomerTicketStatus`
- Flow de vente (génère le publicCode) : `tchalanet-docs/docs/02-functional/flows/sell-ticket.md`
- Flow de vérification : `tchalanet-docs/docs/02-functional/flows/verify-ticket.md`
- Payout après vérification : `tchalanet-docs/docs/02-functional/flows/payout-field-flow.md`
