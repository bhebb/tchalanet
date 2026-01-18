# Domaine core.sales — Tickets & Ventes

> Gère le cycle de vie des tickets: émission, annulation, statut public, sync offline. Domaine critique argent.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/sales.md`

---

## 1. Rôle du domaine

- Émettre/canceller des tickets payés.
- Exposer statuts et vues tickets.
- Publier events métier (Issued/Cancelled/Settled).

**Ce que le domaine ne fait pas**

- Calcul des gains (payout).
- Écritures comptables (ledger).

---

## 2. Modèle & invariants

- `Ticket` aggregate: id, tenant, lines, amountPaid, status (ISSUED/CANCELLED/SETTLED/EXPIRED), publicCode.
- Invariants:
  - `CANCELLED` est terminal.
  - Annulation seulement si fenêtres/règles respectées.
  - `publicCode` signé → jamais mappé en ID interne public.

---

## 3. Use Cases (ports d’entrée)

- `IssueTicketCommandHandler`
- `CancelTicketCommandHandler`
- `VerifyTicketPublicQueryHandler`

---

## 4. Ports (out)

- `TicketRepoPort` (Reader/Writer)
- `TicketCodeGeneratorPort`
- `PrinterPort` (si besoin)

---

## 5. Mapping & DTOs

- MapStruct pour DTO `TicketResponse` / `IssueTicketRequest`.
- Controllers utilisent wrappers d’ID (TicketId, TenantId).

---

## 6. Événements

- Publier `TicketIssuedEvent`, `TicketCancelledEvent`, `TicketSettledEvent` via `AfterCommit`.
- Idempotence requise sur certaines commandes.

---

## 7. Intégrations

- `core.payout` ouvre les claims quand SETTLED.
- `core.ledger` consomme les events pour comptabiliser.

---

## 8. Notes techniques

- Multi-tenant strict; RLS appliqué via datasource.
- Idempotency-Key recommandé pour issue.
- Envers si audit.

---

## 9. Incohérences / TODO

- Confirmer fenêtres d’annulation exactes et limites (LimitPolicyFacade).
- Vérifier naming des statuses (ISSUED/OPEN, EXPIRED optionnel).
- S’assurer que public verify ne retourne pas d’ID interne.
