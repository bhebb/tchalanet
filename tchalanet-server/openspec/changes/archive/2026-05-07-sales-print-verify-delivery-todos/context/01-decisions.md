# Décisions validées

## 1. Vues DB P0

Créer 3 vues DB normales, non matérialisées :

```text
v_ticket_summary
v_ticket_print
v_draw_summary
```

Règles :

- read models uniquement ; jamais pour mutations métier, settlement/write, transitions ou invariants domain.
- respecter RLS, `tenant_id`, `deleted_at`.
- tester qu’un tenant A ne lit pas les données tenant B.
- pas de materialized view pour le moment.

### `v_ticket_summary`

Usage : listes tickets, dashboards, vues tenant/admin.

Champs attendus :

```text
ticket_id
tenant_id
ticket_code
public_code
sale_status
result_status
settlement_status
currency
total_amount
winning_amount
created_at
updated_at
terminal_id
terminal_label
outlet_id
outlet_name
draw_id
draw_date
scheduled_at
draw_channel_id
draw_channel_code
draw_channel_label
draw_timezone
```

### `v_ticket_print`

Usage : impression PDF/ESC-POS/QR, receipt interne.

Vue header enrichie. Lire `ticket_line` séparément par `ticket_id` pour éviter répétition header x lignes.

Champs attendus :

```text
ticket_id
tenant_id
ticket_code
public_code
sale_status
result_status
settlement_status
currency
total_amount
winning_amount
sold_at
terminal_id
terminal_label
session_id
outlet_id
outlet_name
outlet_city
draw_id
draw_date
scheduled_at
draw_channel_id
draw_channel_code
draw_channel_label
draw_time
draw_timezone
```

### `v_draw_summary`

Usage : PageModel/public draws/next draws/latest results/dashboard/ticket labels.

Champs attendus :

```text
draw_id
tenant_id
draw_date
status
scheduled_at
open_at
close_at
cutoff_at
resulted_at
settled_at
draw_channel_id
draw_channel_code
draw_channel_label
draw_time
draw_timezone
result_slot_id
result_slot_key
draw_result_id
result_status
result_occurred_at
source_hash
haiti_lot1
haiti_lot2
haiti_lot3
pick3
pick4
```

## 2. Boundaries verify / sell / print / delivery

```text
verify public  -> features.ticketverify
sell           -> core.sales
print          -> core.sales
delivery       -> features.ticketdelivery + edge-service transport
```

### `features.ticketverify`

- vertical slice, pas hexagonal lourd.
- appelle `core.sales` via `QueryBus`.
- possède controller public, normalization publicCode, noindex/rate-limit, masking, statuts publics.
- ne lit pas DB/JPA/entities directement.

### `/sell`

Reste dans `core.sales` : transactionnel, idempotency, session, limits, pricing, Ticket/TicketLine, events.

### `/print`

Reste dans `core.sales` : reçu officiel POS, PDF, ESC/POS, QR, `TicketPrintView`, `TicketReceiptModel`.

### `features.ticketdelivery`

Endpoint unique :

```text
POST /api/v1/tenant/tickets/{ticketId}/delivery
```

Body :

```json
{
  "channel": "EMAIL",
  "recipient": "client@example.com",
  "locale": "fr",
  "includePdf": false,
  "includeVerificationLink": true
}
```

`channel = EMAIL | SMS | WHATSAPP`.

Validation :

- EMAIL : email valide, PDF optionnel.
- SMS : téléphone E.164 recommandé, PDF interdit/ignoré, lien recommandé.
- WHATSAPP : téléphone E.164 recommandé, lien MVP, attachments plus tard.

`features.ticketdelivery` orchestre vers `tchalanet-edge-service`.
