# OpenSpec Change — maryaj-gratis-pos-v1

## Status

Proposed

## Goal

Côté POS (cashier), afficher automatiquement la ligne « Maryaj gratuit »
générée par le serveur au preview, permettre la régénération avant
confirmation, et confirmer la vente par `preparationId` — le POS n'envoie
jamais les lignes au confirm.

## Why

Le backend (change serveur `maryaj-gratis-auto-selection-v1`) introduit un
flux de vente préparée : la promotion est décidée et matérialisée côté
serveur. Le POS **voit** la ligne offerte, il ne l'applique pas.

## Contrat API (figé côté serveur, 2026-06-10)

```http
POST /tenant/sales/preparations
  body = SellTicketRequest (drawId, drawChannelId, currency, lines, serviceOptions)
  headers = terminal device proof (comme /tenant/tickets)
  -> SalePreparationView { preparationId, status, expiresAt (TTL 10 min),
       currency, totalAmount, lines[], promotionLines[
         { lineRef, gameCode, betType, betOption, selection,
           payoutBaseAmount, regenerable, regenerationsRemaining } ],
       notices[] }

POST /tenant/sales/preparations/{preparationId}/promotion-lines/{lineRef}/regenerate
  -> SalePreparationView (promotionLines mises à jour ; lignes payantes
     inchangées côté client — la vue allégée ne les re-renvoie pas)

POST /tenant/sales/preparations/{preparationId}/confirm
  headers = terminal proof + Idempotency-Key
  body = aucun (preparationId en path, clé en header)
  -> ConfirmPreparedSaleResult { preparationId, ticketId, alreadyConfirmed, sale }
```

Erreurs à gérer : `sales.preparation.expired` (TTL dépassé -> re-préparer),
`sales.preparation.max_regenerations_reached`,
`sales.preparation.line_not_regenerable`,
`sales.preparation.already_confirmed` (clé différente).

## What (UX V1)

```text
Preview vente
  -> la ligne « Maryaj gratuit » apparaît automatiquement si campagne active
  -> bouton « Régénérer » visible si regenerable et regenerationsRemaining > 0
Confirmer -> confirm par preparationId (jamais les lignes)
Imprimer -> le reçu contient la ligne offerte avec les numéros du preview
```

## Non-goals

- Vente offline avec Maryaj gratuit (exclue V1 côté serveur).
- Choix manuel des numéros offerts.
- Affichage d'un historique de régénérations (remplacement simple).

## Dependencies

- Serveur : `tchalanet-server/openspec/changes/maryaj-gratis-auto-selection-v1`
  (slices 1-9 livrées ; reçu/events = slice 10).
- Terminal device proof client déjà en place (`terminal-device-proof-client`).
