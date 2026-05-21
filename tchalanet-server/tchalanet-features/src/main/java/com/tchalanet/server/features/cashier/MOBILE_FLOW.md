# Mobile cashier flow

Guide pour coder l'app mobile vendeur contre l'API `features.cashier`. Toutes les routes
sont préfixées par `${TCH_BASE_URL}` (ex: `http://localhost:8083/api/v1`).

## 0. Pré-requis

- JWT cashier (Keycloak password grant ou refresh)
- IDs `outletId`, `terminalId` (provisionnés côté tenant_admin)
- `sessionId` après ouverture de session POS
- L'app envoie sur **chaque requête** :
  - `Authorization: Bearer <jwt>`
  - `X-Tch-Outlet-Id`, `X-Tch-Terminal-Id`, `X-Tch-Sales-Session-Id`
  - `Idempotency-Key` (UUID v4) **obligatoire** sur `POST /sell`

## 1. Login

```
POST {KEYCLOAK_TOKEN_URL}
  grant_type=password&client_id=...&username=...&password=...
→ { access_token, refresh_token, expires_in, ... }
```

Persiste `access_token` jusqu'à `expires_in`. À l'expiration, refresh ou re-login.

## 2. Validation du contexte opérationnel

Avant la première vente, valide que le vendeur peut utiliser ce terminal :

```
POST /tenant/cashier/operational-context/select
{
  "outletId":        "<outletId>",
  "terminalId":      "<terminalId>",
  "salesSessionId":  "<sessionId>"
}
→ 200  data: { terminalId, outletId, salesSessionId, source, trust }
```

Échec → 403 `seller_context.permission_denied` ou validation error → afficher un message
ops "contactez votre admin tenant".

L'app peut aussi lire l'état courant :

```
GET /tenant/cashier/operational-context/current
→ 200  data: {...}   ou 204 No Content
```

## 3. Session POS

```
GET  /tenant/cashier/session/current?terminalId=<terminalId>
→ 200 data:{sessionId, status:"OPEN", openedAt, openingFloat, ...}
  ou 204 No Content (pas de session ouverte)

POST /tenant/cashier/session/open
{ "outletId": "...", "terminalId": "...", "openingFloat": "100.00" }
→ 201 data:{sessionId, status:"OPEN", openedAt, openingFloat}

POST /tenant/cashier/session/close
{ "sessionId":"...", "closingAmount":"123.45", "reason":"end-of-shift" }
→ 200 data:{sessionId, status:"CLOSED", closedAt, closingAmount}
```

Stocke `sessionId` localement et utilise-le dans `X-Tch-Sales-Session-Id` sur les
requêtes suivantes.

## 4. Tirages vendables

```
GET /tenant/cashier/draws/available?lookaheadHours=24&limit=20
→ 200 data:[
  {
    drawId, drawChannelId, drawDate, resultSlotId, resultSlotKey,
    channelCode,   // ex "HT_TX_1000"
    channelLabel,  // ex "Haïti • Texas • 10:00 (11:00)"  ← déjà formaté
    gameCodes,     // ex ["HT_BOLET","HT_MARYAJ","HT_LOTO3","HT_LOTO4","HT_LOTO5"]
    status,        // "OPEN" | "SCHEDULED" | "CLOSED" — n'autoriser la vente que sur OPEN
    scheduledAt, cutoffAt
  }, ...
]
```

Filtre client-side sur `status == "OPEN"` pour le panier de vente. `cutoffAt` est
l'heure-limite côté serveur ; l'app peut désactiver la vente quand `now > cutoffAt - 60s`
pour une transition douce.

## 5. Construction du panier

Le panier est local au mobile : une liste de lignes `(gameCode, betType, selection,
betOption, stake)`. Voir [§ Selection / betOption matrix](#7-selection--betoption-matrix)
pour les formats valides.

### 5.1 Preview (validation read-only)

Appelle avant chaque "ajouter au panier" pour donner un feedback immédiat :

```
POST /tenant/cashier/tickets/preview
{
  "terminalId":     "<terminalId>",
  "drawId":         "<drawId>",
  "drawChannelId":  "<drawChannelId>",
  "currency":       "HTG",
  "lines": [
    {"gameCode":"HT_BOLET","betType":"MATCH_1_2D","selection":"11","betOption":null,"stake":"1.00"}
  ]
}
→ 200 data:{
  decision:            "ACCEPTABLE" | "REQUIRES_CHANGES" | "REJECTED_FINAL",
  issues:              [{code, severity, message, sellerInstruction, lineIndex, details}],
  actionAvailability:  {canSell, canPrint, canSendSms, canSendWhatsapp, canSendEmail, canCopy},
  sellerInstruction:   "..." | null,
  warning:             "..." | null
}
```

- `ACCEPTABLE` → activer le bouton "Vendre".
- `REQUIRES_CHANGES` → afficher `sellerInstruction`, garder le panier modifiable.
- `REJECTED_FINAL` → bloquer (ex: tirage fermé).

Cf. [§ Issue codes](#8-issue-codes-fréquents) pour la liste.

### 5.2 Vente

```
POST /tenant/cashier/tickets/sell
Idempotency-Key: <uuid-v4>
{
  "terminalId":     "...",
  "drawId":         "...",
  "drawChannelId":  "...",
  "currency":       "HTG",
  "lines": [ ... ]   // identique au panier validé via preview
}
→ 201 data:{
  outcome:            "ACCEPTED" | "REJECTED" | "PENDING_APPROVAL",
  ticketId, ticketCode, publicCode,    // tous null si REJECTED
  saleStatus:         "PLACED" | "APPROVED" | ...
  issues:             [...],
  backup: {                              // null si REJECTED
    displayCode:           "7EX7-0YME",
    verificationShortUrl:  "https://app.tchalanet.com/ticket/7EX70YME",
    shareableText:         "Ticket Tchalanet ...\nCode: 7EX7-0YME\n..."
  },
  actionAvailability: {...},
  sellerInstruction:  "..." | null
}
```

**Règles critiques** :

- L'`Idempotency-Key` doit être généré UNE FOIS pour le panier. Si la requête timeout,
  re-poste le même payload avec la même clé → la réponse stockée est rejouée
  (pas de double vente).
- Sur `ACCEPTED`, **affiche `backup.displayCode` au vendeur immédiatement** (avant tout
  appel print/send). C'est la garantie offline pour le client.
- Sur `REJECTED`, le ticket n'a pas été créé : `ticketId` est null.
- Sur `PENDING_APPROVAL`, ne pas afficher comme vendu — c'est un workflow tenant_admin
  (out of scope mobile cashier).

## 6. Reçu

### 6.1 Imprimer (PDF ou ESC/POS)

```
POST /tenant/cashier/tickets/{ticketId}/print
{
  "format":           "PDF" | "ESC_POS",
  "recordPrint":      true,           // false en reprint pour ne pas auditer doublon
  "deliveryOptions":  ["RETURN_FILE"],// ou + ["SMS","WHATSAPP","EMAIL"]
  "reprintReason":    null,           // string si re-print
  "buyerPhoneNumber": null,           // requis si SMS/WHATSAPP
  "buyerEmail":       null,           // requis si EMAIL
  "buyerLocale":      "fr"            // optionnel
}
→ 200 binary body
Headers:
  Content-Type: application/pdf  | application/octet-stream
  Content-Disposition: inline; filename="ticket-{ticketId}.pdf"
  Cache-Control: no-store
```

Stream directement vers la file d'impression locale (Bluetooth thermique, AirPrint, etc.).
Pour le visuel client, le PDF est le format de référence.

### 6.2 Envoyer (Slack/SMS/Email/WhatsApp — text-only)

```
POST /tenant/cashier/tickets/{ticketId}/send
{
  "terminalId":  "...",
  "channel":     "SLACK_INTERNAL" | "SMS" | "WHATSAPP" | "EMAIL" | "SLACK_TENANT_WEBHOOK",
  "channelKey":  "delivery",         // pour les channels Slack
  "to":          "+50937700000",     // pour SMS/WHATSAPP/EMAIL
  "locale":      "fr"
}
→ 202 data:{ ticketId, channel, recipient, queued:true, dedup:false }
```

L'envoi est **text-only** (pas de PDF en pièce jointe). Le mobile peut afficher
"envoyé" dès que `queued:true` — la livraison réelle passe par `tchalanet-edge-service`.

Dedup côté serveur : 60 secondes pour le même `(ticketId, channel, recipient)`. Re-poste
sans risquer le doublon.

## 7. Selection / betOption matrix

| Game     | BetType         | BetOption | Selection (exemple)      |
|----------|-----------------|-----------|--------------------------|
| BOLET    | `MATCH_1_2D`    | `null`    | 2 chiffres (`"11"`)      |
| MARYAJ   | `MARRIAGE_2D2D` | `1` Ordre exact / `2` Revers | paire (`"21-25"`) |
| LOTO 3   | `LOTTO3_3D`     | `1` Exact / `2` Box | 3 chiffres (`"012"`)   |
| LOTO 4   | `LOTTO4_PATTERN`| `1` Exact / `2` Box | 4 chiffres (`"1234"`)  |
|          |                 | `3` Front pair | **2 chiffres** (`"12"`) |
|          |                 | `4` Back pair  | **2 chiffres** (`"34"`) |
| LOTO 5   | `LOTTO5_PATTERN`| `1` Lot1+Lot2 / `2` Lot1+Lot3 / `3` Lot2+Lot3 | 5 chiffres (`"12345"`) |

⚠️ **Piège** : LOTO 4 front-pair / back-pair attendent **2 chiffres**, pas 4. L'UI doit
adapter le clavier d'entrée selon le betOption choisi sinon `SELECTION_INVALID`.

## 8. Issue codes fréquents

| Code                              | Sens | Action UI |
|-----------------------------------|------|-----------|
| `DRAW_NOT_OPEN`                   | Tirage fermé ou pas encore ouvert | Bloquer, suggérer un autre tirage |
| `SELECTION_INVALID`               | Format selection incompatible avec betType/Option | Corriger la ligne |
| `APPROVAL_REQUIRED`               | Montant > seuil — exposé comme `REQUIRES_CHANGES` côté POS | Réduire la mise OU envoyer pour approbation hors mobile |
| `EXPOSURE_LIMIT`                  | Sélection saturée pour le tirage | Choisir autre numéro |
| `STAKE_TOO_HIGH`                  | Mise dépasse la limite vendeur/outlet | Réduire |
| `EXPOSURE_CHANGED`                | Entre preview et sell, une autre vente a saturé la limite | Refaire preview |
| `DUPLICATE_IDEMPOTENCY_KEY_CONFLICT` | Même clé + payload différent | Régénérer la clé |
| `SESSION_CLOSED`                  | La session POS est fermée | Re-ouvrir une session |

## 9. Cancel (annulation dans la fenêtre)

```
POST /tenant/cashier/tickets/{ticketId}/cancel
{ "terminalId":"...", "reason":"customer change of mind" }
→ 200 data:{ ticketId, outcome:"CANCELLED", cancelledAt, issues:[] }
```

La fenêtre de cancel est de 3 minutes après la vente (config tenant). Au-delà →
`outcome=REJECTED` + `issue=CANCEL_WINDOW_EXPIRED`.

## 10. Consultation des tickets

```
GET /tenant/cashier/tickets?page=0&size=50&sort=createdAt,desc
→ 200 data:{ items:[{id,ticketCode,status,drawId,totalAmountCents,currency,placedAt},...],
             page, size, totalElements, totalPages, hasNext, hasPrevious }

GET /tenant/cashier/tickets/{ticketId}
→ 200 data:{ id, ticketCode, status, drawId, totalAmountCents, currency,
             placedAt, cancelledAt }
```

`sort` autorisé : `createdAt`, `totalAmount`, `ticketCode`.

## 11. Offline (mobile en zone sans réseau)

```
GET  /tenant/cashier/offline/grant/current?terminalId=...&deviceId=...
→ 200 data:{ grantId, codes:[...], validFrom, validUntil, maxTicketCount, maxTotalAmount }

POST /tenant/cashier/offline/submissions
{
  grantId, clientBatchId, batchPayloadHash,
  submissions: [
    { clientSubmissionId, offlineCode, drawId, clientSoldAt, totalStakeAmount, lines:[...], payloadHash, signature }
  ]
}
→ 202 data:{ syncBatchId, outcomes:[{clientSubmissionId, submissionId, outcome, rejectionCode, rejectionReason}] }
```

Le mobile doit signer chaque submission avec sa clé device (Ed25519). Le serveur
re-vérifie le `payloadHash` et la signature. Cf. `core.offlinesync` pour le détail
du protocole.

## 12. Erreurs HTTP communes

| Status | Cas | Action mobile |
|--------|------|----------------|
| 400    | Validation request body | Afficher `detail`, ne pas réessayer sans modif |
| 401    | JWT expiré ou invalide | Re-login |
| 403    | Permissions / context refusé | Vérifier role + operational context |
| 404    | Tirage / ticket inexistant | Recharger |
| 409    | Idempotency conflict (clé réutilisée avec payload différent) | Régénérer la clé + re-poster |
| 422    | Body OK mais business rule échoue | Lire `issues`, corriger panier |
| 500    | Erreur serveur | Retry exponentiel max 3 fois, surface error |

## 13. Sequence happy-path

1. Login → JWT
2. `POST /operational-context/select` (une fois par session app)
3. `GET /session/current` → si null, `POST /session/open`
4. `GET /draws/available` (poll toutes les ~30s)
5. Boucle vente :
   - `POST /tickets/preview` à chaque ligne ajoutée
   - `POST /tickets/sell` (Idempotency-Key fresh) à la confirmation
   - Afficher `backup.displayCode`
   - `POST /tickets/{id}/print` → flux vers imprimante
   - Optionnel : `POST /tickets/{id}/send` (SMS/Slack)
6. En fin de service : `POST /session/close`

## 14. Références côté serveur

- Controllers : `features.cashier.{tickets,session,draws,offline,operationalcontext}`
- Domaine : `core.sales.api.command.sell.SellTicketCommand`,
  `SaleAcceptanceEvaluator`, `TicketBackupAssembler`
- Tests E2E : `tchalanet-server/tests/e2e/tests/cashier/`
  (`test_happy_path.py`, `test_single_ticket.py`, `test_layouts.py`)
