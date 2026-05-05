# Copilot task prompt

Implémente uniquement la tâche demandée. Ne refactorise pas tout le module.

Contexte Tchalanet :

- `core.sales` garde `/sell` et `/print`.
- `features.ticketverify` reçoit la vérification publique.
- `features.ticketdelivery` reçoit l’envoi Email/SMS/WhatsApp via endpoint unique `/delivery` avec `channel`.
- Les vues DB P0 sont `v_ticket_summary`, `v_ticket_print`, `v_draw_summary`.
- Les vues sont read-only, jamais utilisées pour mutations.

Règles :

- Controllers minces.
- Query handlers sans side effects.
- Pas de JPA/repositories/entities depuis une feature.
- Pas de `PageRequest` dans application query.
- Pas de body `performedBy` pour actions sensibles.
- Public verify ne retourne aucun internal ID.
- QR print pointe vers `/ticket/{publicCode}`.

Format de réponse attendu :

- Fichiers à modifier.
- Code proposé.
- Tests à ajouter.
- Risques ou TODO restants.
