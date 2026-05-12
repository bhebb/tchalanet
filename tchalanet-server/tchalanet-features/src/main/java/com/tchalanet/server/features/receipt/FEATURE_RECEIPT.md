# Feature Receipt

> Feature tenant dédiée au rendu de reçus et documents exposés par API: PDF, ESC/POS et QR public.

## Rôle

- Exposer les endpoints binaires de reçu sous `/tenant/tickets/{ticketId}/...`.
- Orchestrer les composants techniques de rendu (`common.document`) et les projections de ticket fournies par `core.sales`.
- Rester une feature de rendu: elle ne livre pas de messages externes et ne modifie pas le cycle de vie ticket.

## Frontières

`features.receipt` peut dépendre de `core.sales` pour lire la projection de reçu et des helpers techniques `common.document`.

Le module ne doit pas:

- porter les invariants de vente, résultat ou règlement;
- écrire dans les repositories core;
- appeler d'autres features ou exposer un contrat interne HTTP pour les autres features;
- dupliquer la logique de communication email/SMS/WhatsApp/Slack.

## Routes

- `GET /tenant/tickets/{ticketId}/print.pdf`
- `GET /tenant/tickets/{ticketId}/print.escpos`
- `GET /tenant/tickets/{ticketId}/qr`

L'ancien endpoint base64 `GET /tenant/tickets/{ticketId}/print` est supprimé.
