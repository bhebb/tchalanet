# Prompt task 05 — Sales cleanup

Nettoie web/events/generators/bridge sans changer les grandes décisions.

À faire :

- Split controllers sales.
- Corriger `TicketWebMapper` : no body `performedBy`, no arbitrary override statuses, `TchPageRequest`, invalid status -> 400.
- Ajouter idempotence `ProcessedEventPort` dans `SalesLedgerListener`.
- Ajouter DB unique + retry save pour `publicCode` et `ticketCode`.
- Corriger `SalesTicketAdminAdapter` : empty sessions -> zero stats, deletedAt filters, no-op methods fail fast ou supprimées.

Ne pas toucher public verify/delivery si déjà déplacés.
