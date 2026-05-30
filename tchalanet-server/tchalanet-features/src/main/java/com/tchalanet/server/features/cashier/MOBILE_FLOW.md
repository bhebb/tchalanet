# Mobile cashier flow — Guide d'intégration Flutter

Guide pour coder l'app mobile vendeur contre l'API `features.cashier`.
Toutes les routes sont préfixées par `${TCH_BASE_URL}` (ex: `http://localhost:8083/api/v1`).

---

## 0. Pré-requis

- JWT cashier (Keycloak password grant ou refresh)
- IDs `outletId`, `terminalId` provisionnés côté tenant_admin
- `sessionId` obtenu après ouverture de session POS
- L'app envoie sur **chaque requête protégée** :
  - `Authorization: Bearer <jwt>`
  - `X-Tch-Outlet-Id`
  - `X-Tch-Terminal-Id`
  - `X-Tch-Sales-Session-Id` (après ouverture de session)
  - `Idempotency-Key` (UUID v4) **obligatoire** uniquement sur `POST /sell`

---

## 1. Login

```http
POST {KEYCLOAK_TOKEN_URL}
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=...&username=...&password=...
```

```json
{
  "access_token": "...",
  "refresh_token": "...",
  "expires_in": 300
}
```

Persiste `access_token` jusqu'à `expires_in`. À l'expiration : refresh token ou re-login.

---

## 2. Profil vendeur

Charge les informations de l'utilisateur connecté et ses préférences. Ne remplace pas le contexte opérationnel POS.

```http
GET /tenant/cashier/profile/current
```

```json
{
  "userId": "uuid",
  "username": "agent01",
  "displayName": "Agent",
  "email": "agent@example.com",
  "phoneNumber": "+50937700000",
  "locale": "fr",
  "timezone": "America/Port-au-Prince",
  "roles": ["CASHIER"],
  "permissions": ["cashier.sell", "cashier.ticket.print"],
  "tenant": { "tenantId": "uuid", "tenantName": "Tchalanet Demo" },
  "defaults": {
    "receiptFormat": "PDF",
    "receiptLayout": "THERMAL_80MM",
    "currency": "HTG"
  }
}
```

```http
PATCH /tenant/cashier/profile/current
Content-Type: application/json

{
  "displayName": "Agent",
  "phoneNumber": "+50937700000",
  "locale": "fr",
  "timezone": "America/Port-au-Prince",
  "defaults": { "receiptFormat": "PDF", "receiptLayout": "THERMAL_80MM" }
}
```

Règles :
- Utiliser `locale` pour les libellés et le reçu.
- Utiliser `defaults.receiptFormat` et `defaults.receiptLayout` pour le bouton impression.
- Ne pas déduire la permission de vendre depuis le profil seul — ça dépend aussi du terminal, outlet et session.
- Les rôles, permissions, tenant, outlet et terminal ne sont pas modifiables via le profil.

---

## 3. Contexte opérationnel

Avant la première vente, valide que le vendeur peut utiliser ce terminal/outlet/session.

```http
POST /tenant/cashier/operational-context/select
Content-Type: application/json

{
  "outletId": "<outletId>",
  "terminalId": "<terminalId>",
  "salesSessionId": "<sessionId>"
}
```

```json
{
  "terminalId": "uuid",
  "outletId": "uuid",
  "salesSessionId": "uuid",
  "source": "ADMIN_SELECTION",
  "trust": "TRUSTED"
}
```

Lire l'état courant :

```http
GET /tenant/cashier/operational-context/current
→ 200 data:{...}  ou  204 No Content
```

Échec → 403 `seller_context.permission_denied` → afficher "Contactez votre administrateur".

---

## 4. Session POS

```http
GET /tenant/cashier/session/current?terminalId=<terminalId>
→ 200 data:{sessionId, status:"OPEN", openedAt, openingFloat, ...}
  ou 204 No Content

POST /tenant/cashier/session/open
{ "outletId": "...", "terminalId": "...", "openingFloat": "100.00" }
→ 201 data:{sessionId, status:"OPEN", openedAt, openingFloat}

POST /tenant/cashier/session/close
{ "sessionId":"...", "closingAmount":"123.45", "reason":"end-of-shift" }
→ 200 data:{sessionId, status:"CLOSED", closedAt, closingAmount}
```

Stocke `sessionId` localement et envoie dans `X-Tch-Sales-Session-Id` sur les requêtes suivantes.

---

## 5. Tirages vendables

```http
GET /tenant/cashier/draws/available?lookaheadHours=24&limit=20
```

```json
[
  {
    "drawId": "uuid",
    "drawChannelId": "uuid",
    "drawDate": "2026-05-21",
    "resultSlotKey": "HT_TX_1000",
    "channelCode": "HT_TX_1000",
    "channelLabel": "Haïti • Texas • 10:00 (11:00)",
    "gameCodes": ["HT_BOLET", "HT_MARYAJ", "HT_LOTO3", "HT_LOTO4", "HT_LOTO5"],
    "status": "OPEN",
    "scheduledAt": "2026-05-21T15:00:00Z",
    "cutoffAt": "2026-05-21T14:55:00Z"
  }
]
```

Règles :
- Autoriser la vente uniquement sur `status == "OPEN"`.
- `cutoffAt` : désactiver progressivement côté mobile (ex: `now > cutoffAt - 60s`), le serveur reste source de vérité.
- Recharger périodiquement (toutes les ~30s).

---

## 6. Panier et vente

Le panier est local au mobile : liste de lignes `(gameCode, betType, selection, betOption, stake)`.

### 6.1 Règle idempotency

- Générer un `Idempotency-Key` (UUID v4) **au moment de confirmer la vente** — pas avant.
- Si la requête timeout, re-poster le **même payload avec la même clé** → réponse stockée rejouée, pas de double vente.
- Si le panier change, générer une **nouvelle clé**.
- Si le serveur retourne un conflit idempotency, ne pas forcer — refaire preview et confirmer.

### 6.2 Preview (validation read-only)

Appeler avant chaque "ajouter au panier" pour feedback immédiat :

```http
POST /tenant/cashier/tickets/preview
Content-Type: application/json

{
  "terminalId": "<terminalId>",
  "drawId": "<drawId>",
  "drawChannelId": "<drawChannelId>",
  "currency": "HTG",
  "lines": [
    { "gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "11", "betOption": null, "stake": "1.00" }
  ]
}
```

```json
{
  "decision": "ACCEPTABLE",
  "issues": [],
  "actionAvailability": {
    "canSell": true, "canPrint": true, "canSendSms": true,
    "canSendWhatsapp": true, "canSendEmail": true, "canCopy": true
  },
  "sellerInstruction": "Ce billet peut être vendu.",
  "warning": null
}
```

- `ACCEPTABLE` → activer le bouton "Vendre".
- `REQUIRES_CHANGES` → garder le panier modifiable, afficher `sellerInstruction`.
- `REJECTED_FINAL` → bloquer (ex: tirage fermé, session fermée).

Preview ne réserve pas l'exposition — un sell peut encore retourner `EXPOSURE_CHANGED`.

### 6.3 Vente

```http
POST /tenant/cashier/tickets/sell
Idempotency-Key: <uuid-v4>
Content-Type: application/json

{
  "terminalId": "...", "drawId": "...", "drawChannelId": "...",
  "currency": "HTG",
  "lines": [ ... ]
}
```

Réponse acceptée :

```json
{
  "outcome": "ACCEPTED",
  "ticketId": "uuid",
  "ticketCode": "TCK-260521-125433-GV5G0P-0",
  "publicCode": "40CPJBMR",
  "saleStatus": "PLACED",
  "issues": [],
  "backup": {
    "displayCode": "40CP-JBMR",
    "verificationShortUrl": "https://app.tchalanet.com/ticket/40CP-JBMR",
    "shareableText": "Ticket Tchalanet valide\nCode: 40CP-JBMR\n..."
  },
  "actionAvailability": { "canPrint": true, "canSendSms": true, "canSendWhatsapp": true, "canSendEmail": true, "canCopy": true },
  "sellerInstruction": "Vente acceptée. Donnez le code au client ou imprimez/envoyez le ticket."
}
```

Réponse refusée :

```json
{
  "outcome": "REJECTED",
  "ticketId": null, "ticketCode": null, "publicCode": null,
  "issues": [
    {
      "code": "EXPOSURE_CHANGED",
      "severity": "ERROR",
      "message": "La limite a changé pendant la vente.",
      "sellerInstruction": "Réduisez la mise et réessayez.",
      "lineIndex": 0,
      "details": { "allowedRemaining": "1.00 HTG" }
    }
  ],
  "backup": null,
  "actionAvailability": { "canPrint": false, "canSendSms": false, "canSendWhatsapp": false, "canSendEmail": false, "canCopy": false },
  "sellerInstruction": "Vente refusée. Ajustez le panier et réessayez."
}
```

**Règles critiques** :
- Le cashier POS expose seulement `ACCEPTED` ou `REJECTED` — `PENDING_APPROVAL` est un workflow tenant_admin hors scope mobile.
- Sur `ACCEPTED` : **afficher `backup.displayCode` immédiatement**, avant tout appel print/send. C'est la garantie offline pour le client.
- Sur `REJECTED` : aucun ticket créé — `ticketId` est null.

### 6.4 Écran de succès (après ACCEPTED)

Afficher :
1. `backup.displayCode` en grand
2. `backup.verificationShortUrl` sous le code
3. Boutons : `Copier le code` · `Copier le lien` · `Copier le message`
4. Actions optionnelles : `Imprimer` · `Envoyer SMS` · `Envoyer WhatsApp` · `Envoyer Email`

> Print/send sont des bonus. La preuve minimale client est le code + lien de vérification.

---

## 7. Reçu

### 7.1 Imprimer (PDF ou ESC/POS)

```http
POST /tenant/cashier/tickets/{ticketId}/print
Content-Type: application/json

{
  "format": "PDF",
  "layout": "THERMAL_80MM",
  "recordPrint": true,
  "reprintReason": null,
  "buyerLocale": "fr"
}
→ 200 binary body
Content-Type: application/pdf | application/octet-stream
Content-Disposition: inline; filename="ticket-{displayCode}.pdf"
Cache-Control: no-store
```

- `print` rend un fichier uniquement — il n'envoie pas SMS/WhatsApp/Email.
- `recordPrint: false` sur reprint pour ne pas auditer le doublon.
- Streamer directement vers la file d'impression locale (Bluetooth thermique, AirPrint, etc.).

### 7.2 Envoyer (SMS / WhatsApp / Email)

```http
POST /tenant/cashier/tickets/{ticketId}/send
Content-Type: application/json

{
  "terminalId": "...",
  "channel": "SMS",
  "to": "+50937700000",
  "locale": "fr",
  "includeVerificationLink": true,
  "includeLines": true
}
```

```json
{ "ticketId": "uuid", "channel": "SMS", "recipient": "+50937700000", "queued": true, "deduplicated": false }
```

- Envoi V1 text-only — pas de PDF en pièce jointe.
- Afficher "envoyé" dès `queued:true`.
- Dedup serveur : 60s pour le même `(ticketId, channel, recipient)`.

---

## 8. Selection / betOption matrix

| Game | BetType | BetOption | Selection |
|---|---|---|---|
| BOLET | `MATCH_1_2D` | `null` | 2 chiffres, ex. `11` |
| BOLET | `MATCH_2_2D` | `null` | 2 chiffres, ex. `11` |
| BOLET | `MATCH_3_2D` | `null` | 2 chiffres, ex. `11` |
| MARYAJ | `MARRIAGE_2D2D` | `1` Ordre exact | paire, ex. `21-25` |
| MARYAJ | `MARRIAGE_2D2D` | `2` Revers / Double | paire, ex. `21-25` |
| LOTO 3 | `LOTTO3_3D` | `1` Exact | 3 chiffres, ex. `012` |
| LOTO 3 | `LOTTO3_3D` | `2` Box | 3 chiffres, ex. `012` |
| LOTO 4 | `LOTTO4_PATTERN` | `1` Exact | 4 chiffres, ex. `1234` |
| LOTO 4 | `LOTTO4_PATTERN` | `2` Box | 4 chiffres, ex. `1234` |
| LOTO 4 | `LOTTO4_PATTERN` | `3` Front pair | **2 chiffres**, ex. `12` |
| LOTO 4 | `LOTTO4_PATTERN` | `4` Back pair | **2 chiffres**, ex. `34` |
| LOTO 5 | `LOTTO5_PATTERN` | `1` 1er lot + 2e lot | 5 chiffres, ex. `12345` |
| LOTO 5 | `LOTTO5_PATTERN` | `2` 1er lot + 3e lot | 5 chiffres, ex. `12345` |
| LOTO 5 | `LOTTO5_PATTERN` | `3` Mixte 1er/2e/3e lot | 5 chiffres, ex. `34567` |

**Pièges** :
- LOTO 4 front pair / back pair attendent **2 chiffres**, pas 4 — adapter le clavier d'entrée selon betOption.
- LOTO 5 option 3 (mixte) : dernier chiffre du 1er lot + 2 chiffres du 2e lot + 2 chiffres du 3e lot.
- Ne jamais afficher les noms techniques (`LOTTO5_PATTERN`, `MATCH_1_2D`) au vendeur — afficher les labels humains.

---

## 9. Issue codes fréquents

| Code | Sens | Action UI |
|---|---|---|
| `DRAW_NOT_OPEN` | Tirage fermé ou pas encore ouvert | Bloquer, suggérer un autre tirage |
| `SELECTION_INVALID` | Format selection incompatible avec betType/option | Corriger la ligne |
| `APPROVAL_REQUIRED` | Montant > autonomie POS | Réduire la mise ou contacter admin |
| `EXPOSURE_LIMIT` | Sélection saturée pour le tirage | Choisir autre numéro ou réduire |
| `STAKE_TOO_HIGH` | Mise dépasse la limite vendeur/outlet | Réduire |
| `EXPOSURE_CHANGED` | Entre preview et sell, une autre vente a saturé la limite | Refaire preview |
| `DUPLICATE_IDEMPOTENCY_KEY_CONFLICT` | Même clé + payload différent | Générer nouvelle clé après modification panier |
| `SESSION_CLOSED` | Session POS fermée | Réouvrir une session |
| `CANCEL_WINDOW_EXPIRED` | Fenêtre 3min dépassée | Informer le client |

---

## 10. Annulation

```http
POST /tenant/cashier/tickets/{ticketId}/cancel
Content-Type: application/json

{ "terminalId": "...", "reason": "customer change of mind" }
```

```json
{ "ticketId": "uuid", "outcome": "CANCELLED", "cancelledAt": "2026-05-21T13:00:00Z", "issues": [] }
```

Fenêtre V1 : 3 minutes après la vente. Au-delà → `REJECTED` + `CANCEL_WINDOW_EXPIRED`.

---

## 11. Consultation des tickets

```http
GET /tenant/cashier/tickets?page=0&size=50&sort=createdAt,desc
```

```json
{
  "items": [
    { "id": "uuid", "ticketCode": "TCK-...", "displayCode": "40CP-JBMR",
      "status": "PLACED", "drawId": "uuid", "totalAmount": "15.00 HTG", "placedAt": "..." }
  ],
  "page": 0, "size": 50, "totalElements": 1, "totalPages": 1, "hasNext": false, "hasPrevious": false
}

GET /tenant/cashier/tickets/{ticketId}
```

Tri autorisé : `createdAt`, `totalAmount`, `ticketCode`.

---

## 12. Offline (mobile sans réseau)

```http
GET /tenant/cashier/offline/grant/current?terminalId=...&deviceId=...
```

```json
{
  "grantId": "uuid", "codes": ["..."],
  "validFrom": "2026-05-21T12:00:00Z", "validUntil": "2026-05-21T18:00:00Z",
  "maxTicketCount": 100, "maxTotalAmount": "10000.00 HTG"
}
```

Soumettre les ventes offline :

```http
POST /tenant/cashier/offline/submissions
Content-Type: application/json

{
  "grantId": "uuid", "clientBatchId": "uuid", "batchPayloadHash": "sha256...",
  "submissions": [
    {
      "clientSubmissionId": "uuid", "offlineCode": "...", "drawId": "uuid",
      "clientSoldAt": "...", "totalStakeAmount": "10.00 HTG",
      "lines": [], "payloadHash": "sha256...", "signature": "ed25519..."
    }
  ]
}
```

```json
{
  "syncBatchId": "uuid",
  "outcomes": [
    { "clientSubmissionId": "uuid", "submissionId": "uuid", "outcome": "ACCEPTED", "rejectionCode": null, "rejectionReason": null }
  ]
}
```

Le serveur re-vérifie le `payloadHash` et la signature device (Ed25519). Cf. `core.offlinesync` pour le protocole complet.

---

## 13. Erreurs HTTP communes

| Status | Cas | Action mobile |
|---:|---|---|
| 400 | Validation request body | Afficher `detail`, ne pas réessayer sans modif |
| 401 | JWT expiré ou invalide | Refresh ou re-login |
| 403 | Permissions / contexte refusé | Vérifier rôle + operational context |
| 404 | Tirage / ticket inexistant | Recharger |
| 409 | Idempotency conflict (clé réutilisée + payload différent) | Générer nouvelle clé après modification panier |
| 422 | Body OK mais règle métier échoue | Lire `issues`, corriger panier |
| 500 | Erreur serveur | Retry exponentiel max 3 fois, surface error |

---

## 14. Séquence happy-path

1. Login → JWT
2. `GET /profile/current` — charger préférences et locale
3. `GET /session/current?terminalId=...` — si 204, `POST /session/open`
4. `POST /operational-context/select` — confirmer terminal + outlet + session
5. `GET /draws/available` (poll ~30s)
6. **Boucle vente** :
   - Construire panier local
   - `POST /tickets/preview` → décision
   - Si `ACCEPTABLE` : `POST /tickets/sell` avec `Idempotency-Key` fresh
   - **Afficher `backup.displayCode` immédiatement**
   - Optionnel : `POST /tickets/{id}/print` · `POST /tickets/{id}/send`
7. En fin de service : `POST /session/close`

---

## 15. Références côté serveur

- Controllers : `features.cashier.{tickets,session,draws,offline,operationalcontext,profile}`
- Domaine : `core.sales.api.command.sell.SellTicketCommand`, `SaleAcceptanceEvaluator`, `TicketBackupAssembler`
- Tests E2E : `tchalanet-server/tests/e2e/tests/cashier/` (`test_happy_path.py`, `test_single_ticket.py`, `test_layouts.py`)
- Guide non-technique seller : [`SELLER_GUIDE.md`](./SELLER_GUIDE.md)
