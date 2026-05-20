# Tchalanet — Sales / Print / Verify / Delivery TODO Pack

Pack de pilotage pour Claude / Copilot afin de terminer le gros dossier `core.sales` autour de :

- ventes `/sell`
- impression `/print/pdf`, `/print/escpos`, QR
- vérification publique de ticket
- ticket delivery email/SMS/WhatsApp
- vues DB P0 : `v_ticket_summary`, `v_ticket_print`, `v_draw_summary`

Ce pack n’est pas du code prêt à appliquer. C’est un plan d’exécution clair, découpé en tâches, avec les décisions d’architecture déjà validées.

## Décisions clés

```text
core.sales
  - /sell
  - /print/pdf
  - /print/escpos
  - TicketPrintView / TicketReceiptModel
  - GetPublicTicketVerificationRecordQuery

features.ticketverify
  - page/API publique de vérification
  - response publique masquée
  - noindex, rate-limit
  - mapping statut public

features.ticketdelivery
  - POST /api/v1/tenant/tickets/{ticketId}/delivery
  - channel = EMAIL | SMS | WHATSAPP
  - orchestre vers tchalanet-edge-service

tchalanet-edge-service
  - transport réel email/SMS/WhatsApp
  - templates, retry, logs, anti-spam
```

## Ordre recommandé

1. Créer/stabiliser les 3 vues DB.
2. Refactoriser `TicketPrintView` + ports/readers.
3. Corriger pipeline print PDF/ESC-POS/QR.
4. Sortir public verify vers `features.ticketverify`.
5. Ajouter `features.ticketdelivery` avec endpoint unique discriminant.
6. Nettoyer controllers/mappers sales.
7. Ajouter tests P0.

## Règle centrale

```text
Domain protège les invariants.
Application orchestre.
Infra persiste/projette/render.
Features exposent/orchestrent l’expérience, sans repos/JPA/entities.
```
