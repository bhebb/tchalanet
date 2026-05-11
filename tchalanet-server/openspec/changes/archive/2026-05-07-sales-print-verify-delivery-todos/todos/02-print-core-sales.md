# TODO 02 — Print dans core.sales

## Objectif

Stabiliser le pipeline print :

```text
TicketPrintView -> TicketVerificationUrlBuilder -> TicketReceiptFormatter -> ReceiptModel -> PDF/ESC-POS/QR
```

## Décisions

- Print reste dans `core.sales`.
- QR imprimé pointe vers la page publique `/ticket/{publicCode}`, pas une API `/api/v1/...`.
- `v_ticket_print` + `ticket_line` alimentent `TicketPrintView`.

## Fichiers/classes à revoir

```text
core.sales.application.query.handler.GetTicketPrintPdfQueryHandler
core.sales.application.query.handler.GetTicketPrintEscPosQueryHandler
core.sales.application.query.handler.GetTicketQrPngByPublicCodeQueryHandler
core.sales.application.query.handler.QrPayloadBuilder
common.print.pdf.TicketPdfBuilder
common.print.escpos.EscPosBuilder
common.qr.QrRenderer
```

## P0 — URL QR publique

- [ ] Remplacer `QrPayloadBuilder` par `TicketVerificationUrlBuilder`.
- [ ] Déplacer hors package `application.query.handler`, vers `core.sales.application.print` ou `core.sales.application.receipt`.
- [ ] Utiliser une config publique :

```yaml
tch:
  tickets:
    public:
      base-url: 'https://app.tchalanet.com'
      ticket-path-template: '/ticket/{code}'
```

- [ ] Construire `https://.../ticket/{publicCode}`.
- [ ] Normaliser `publicCode` : trim, uppercase, remove spaces/dashes.
- [ ] Ne plus utiliser `ApiProperties.basePath/apiVersion` pour QR public.

## P0 — Ports/readers

- [ ] Renommer/remplacer `TicketPrintViewPort` par `TicketPrintReaderPort`.
- [ ] Implémenter `JdbcTicketPrintReaderAdapter` via `v_ticket_print` + query `ticket_line`.
- [ ] Le reader retourne `TicketPrintView`, pas `Ticket` domain.
- [ ] Le reader ne doit pas formatter selon `Locale`; le formatter s’en charge.

## P0 — Handlers

- [ ] Annoter les handlers avec `@UseCase`.
- [ ] `GetTicketPrintPdfQueryHandler` utilise `TicketPrintReaderPort`.
- [ ] `GetTicketPrintEscPosQueryHandler` utilise `TicketPrintReaderPort`.
- [ ] Passer `Locale` dans `GetTicketPrintPdfQuery` / `GetTicketPrintEscPosQuery` si possible.
- [ ] Éviter `TchContextResolver` dans query handler si le controller peut mapper la locale.
- [ ] Ticket absent => 404 via `ProblemRest` ou exception mappée, pas `IllegalArgumentException`.

## P0 — QR PNG endpoint

- [ ] Supprimer/remplacer `GetTicketQrPngByPublicCodeQueryHandler`.
- [ ] Créer `GetTicketQrPngQuery(ticketId, sizePx)`.
- [ ] QR officiel interne se génère par `ticketId`, via `TicketPrintReaderPort`.
- [ ] Public verify n’a pas besoin d’un endpoint QR par publicCode.

## P1 — PDF builder

- [ ] Remplacer hauteur fixe `600` par hauteur dynamique selon nombre de lignes + QR.
- [ ] Ne jamais couper silencieusement les lignes (`if (y < 20) break` interdit pour reçu officiel).
- [ ] Tester accents FR/HT.
- [ ] Prévoir wrapping/truncation contrôlé.
- [ ] Garder largeur 80mm approx (`226pt`) si imprimante thermique/PDF ticket.

## P1 — ESC/POS builder

- [ ] Remplacer ASCII brut par ASCII folding pour accents.
- [ ] Ajouter `feed(3)` avant `cut()`.
- [ ] Tester que `qrEscPosRenderer` produit des commandes ESC/POS et non PNG.
- [ ] Garder les commandes init/align/bold/cut simples MVP.

## P1 — Tests

- [ ] PDF généré non vide.
- [ ] ESC/POS commence par init `ESC @`.
- [ ] ESC/POS contient cut.
- [ ] QR payload contient `/ticket/{publicCode}`.
- [ ] Aucun QR print ne pointe vers `/api/v1`.
- [ ] Accent folding ESC/POS : `Téléphone` -> `Telephone`.
