# Feature Cashier

> BFF vendeur/POS pour vendre, préparer un reçu imprimable et déclencher un envoi externe.

## Rôle

- Orchestrer les workflows écran caissier.
- Déléguer la vente à `core.sales` via `CommandBus`.
- Préparer les artefacts de reçu avec `core.sales` + `common.document`.
- Envoyer un reçu/message externe avec `common.communication`.

## Frontières

`features.cashier` ne calcule pas les invariants de vente, de limite, de payout ou de settlement.

Le module ne doit pas:

- appeler `features.receipt`;
- écrire directement dans les repositories core;
- appeler le edge-service directement;
- dupliquer la logique métier de `core.sales`.

## Routes

- `POST /tenant/cashier/sell`
- `POST /tenant/cashier/tickets/{ticketId}/send`
- `GET /tenant/cashier/games/available`

## Game options

`GET /tenant/cashier/games/available` fournit au POS les libellés vendeur des
jeux, bet types, bet options, descriptions et hints de saisie. Le POS envoie
toujours la saisie brute du vendeur; `core.sales` canonicalise avec
`betType + betOption`.

Le POS ne doit pas afficher les noms techniques d'enum (`LOTTO4_PATTERN`,
`MARRIAGE_2D2D`, etc.) ni des codes d'option bruts sans libellé.
