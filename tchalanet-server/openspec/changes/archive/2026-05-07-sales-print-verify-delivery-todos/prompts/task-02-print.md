# Prompt task 02 — Print pipeline

Refactorise uniquement le pipeline print dans `core.sales`.

Objectifs :

- Remplacer `QrPayloadBuilder` par `TicketVerificationUrlBuilder`.
- QR print -> `/ticket/{publicCode}` via config publique, jamais `/api/v1`.
- Remplacer `TicketPrintViewPort` par `TicketPrintReaderPort`.
- PDF/ESC-POS handlers annotés `@UseCase`.
- Supprimer QR by publicCode ; créer QR by ticketId.
- PDF height dynamique.
- ESC/POS ASCII folding + feed avant cut.

Ne touche pas à `features.ticketverify` dans cette tâche.
