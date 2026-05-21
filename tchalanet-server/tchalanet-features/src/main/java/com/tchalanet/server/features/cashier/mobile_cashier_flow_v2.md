# Mobile cashier flow — V1

Guide pour coder l'app mobile vendeur contre l'API `features.cashier`.
Toutes les routes sont préfixées par `${TCH_BASE_URL}`.

Exemple :

```text
http://localhost:8083/api/v1
```

## 0. Pré-requis

- JWT cashier obtenu via Keycloak.
- IDs `outletId`, `terminalId` provisionnés côté tenant admin.
- `sessionId` obtenu après ouverture de session POS.
- L'app envoie sur les requêtes protégées :
  - `Authorization: Bearer <jwt>`
  - `X-Tch-Outlet-Id`
  - `X-Tch-Terminal-Id`
  - `X-Tch-Sales-Session-Id` après ouverture de session
- `Idempotency-Key` est obligatoire uniquement sur `POST /tenant/cashier/tickets/sell`.

## 1. Login

```http
POST {KEYCLOAK_TOKEN_URL}
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=...&username=...&password=...
```

Réponse :

```json
{
  "access_token": "...",
  "refresh_token": "...",
  "expires_in": 300
}
```

Le mobile persiste `access_token` jusqu'à `expires_in`. À l'expiration, il tente un refresh token. Si le refresh échoue, il revient au login.

## 2. Profil vendeur

Le profil sert à charger les informations de l'utilisateur connecté et ses préférences d'affichage. Il ne remplace pas le contexte opérationnel POS.

### 2.1 Charger le profil

```http
GET /tenant/cashier/profile/current
Authorization: Bearer <jwt>
```

Réponse :

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
  "tenant": {
    "tenantId": "uuid",
    "tenantName": "Tchalanet Demo"
  },
  "defaults": {
    "receiptFormat": "PDF",
    "receiptLayout": "THERMAL_80MM",
    "currency": "HTG"
  }
}
```

UI mobile :

- Afficher `displayName`, `tenantName`, rôle et statut de session.
- Utiliser `locale` pour les libellés et le reçu.
- Utiliser `defaults.receiptFormat` et `defaults.receiptLayout` pour le bouton impression.
- Ne pas déduire la permission de vendre à partir du profil seulement : la vente dépend aussi du terminal, outlet et session.

### 2.2 Modifier le profil

```http
PATCH /tenant/cashier/profile/current
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "displayName": "Agent",
  "phoneNumber": "+50937700000",
  "locale": "fr",
  "timezone": "America/Port-au-Prince",
  "defaults": {
    "receiptFormat": "PDF",
    "receiptLayout": "THERMAL_80MM"
  }
}
```

Réponse :

```json
{
  "userId": "uuid",
  "displayName": "Agent",
  "phoneNumber": "+50937700000",
  "locale": "fr",
  "timezone": "America/Port-au-Prince",
  "defaults": {
    "receiptFormat": "PDF",
    "receiptLayout": "THERMAL_80MM",
    "currency": "HTG"
  },
  "updatedAt": "2026-05-21T13:00:00Z"
}
```

Règles :

- Le mobile ne peut pas modifier ses rôles, permissions, tenant, outlet ou terminal via le profil.
- Les préférences d'impression sont des préférences UI, pas une garantie que l'imprimante physique fonctionne.
- Si le backend refuse un champ non modifiable, afficher le message serveur et recharger le profil.

## 3. Validation du contexte opérationnel

Avant la première vente, le mobile valide que le vendeur peut utiliser ce terminal/outlet/session.

```http
POST /tenant/cashier/operational-context/select
Content-Type: application/json

{
  "outletId": "<outletId>",
  "terminalId": "<terminalId>",
  "salesSessionId": "<sessionId>"
}
```

Réponse :

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
```

Réponse possible :

```text
200 data:{...}
204 No Content
```

Échec : afficher un message opérationnel du type `Contactez votre administrateur`.

## 4. Session POS

```http
GET /tenant/cashier/session/current?terminalId=<terminalId>
```

Réponse :

```text
200 data:{sessionId, status:"OPEN", openedAt, openingFloat, ...}
204 No Content
```

Ouvrir une session :

```http
POST /tenant/cashier/session/open
Content-Type: application/json

{
  "outletId": "...",
  "terminalId": "...",
  "openingFloat": "100.00"
}
```

Fermer une session :

```http
POST /tenant/cashier/session/close
Content-Type: application/json

{
  "sessionId": "...",
  "closingAmount": "123.45",
  "reason": "end-of-shift"
}
```

Le mobile stocke `sessionId` localement et l'envoie ensuite dans `X-Tch-Sales-Session-Id`.

## 5. Tirages vendables

```http
GET /tenant/cashier/draws/available?lookaheadHours=24&limit=20
```

Réponse :

```json
[
  {
    "drawId": "uuid",
    "drawChannelId": "uuid",
    "drawDate": "2026-05-21",
    "resultSlotId": "uuid",
    "resultSlotKey": "HT_TX_1000",
    "channelCode": "HT_TX_1000",
    "channelLabel": "Haïti • Texas • 10:00",
    "gameCodes": ["HT_BOLET", "HT_MARYAJ", "HT_LOTO3", "HT_LOTO4", "HT_LOTO5"],
    "status": "OPEN",
    "scheduledAt": "2026-05-21T15:00:00Z",
    "cutoffAt": "2026-05-21T14:55:00Z"
  }
]
```

Règles UI :

- Autoriser la vente seulement sur `status == "OPEN"`.
- Utiliser `cutoffAt` pour désactiver progressivement la vente côté mobile, mais le serveur reste source de vérité.
- Recharger périodiquement la liste, par exemple toutes les 30 secondes.

## 6. Construction du panier

Le panier est local au mobile : liste de lignes `(gameCode, betType, selection, betOption, stake)`.

### 6.1 Règle idempotency mobile

- Générer un `Idempotency-Key` seulement au moment de confirmer la vente.
- Réutiliser la même clé uniquement pour retry du même payload après timeout/réseau instable.
- Si le panier change, générer une nouvelle clé.
- Si le backend retourne un conflit idempotency, ne pas forcer : refaire preview et confirmer à nouveau.

### 6.2 Preview

```http
POST /tenant/cashier/tickets/preview
Content-Type: application/json

{
  "terminalId": "<terminalId>",
  "drawId": "<drawId>",
  "drawChannelId": "<drawChannelId>",
  "currency": "HTG",
  "lines": [
    {
      "gameCode": "HT_BOLET",
      "betType": "MATCH_1_2D",
      "selection": "11",
      "betOption": null,
      "stake": "1.00"
    }
  ]
}
```

Réponse :

```json
{
  "decision": "ACCEPTABLE",
  "issues": [],
  "actionAvailability": {
    "canSell": true,
    "canPrint": true,
    "canSendSms": true,
    "canSendWhatsapp": true,
    "canSendEmail": true,
    "canCopy": true
  },
  "sellerInstruction": "Ce billet peut être vendu.",
  "warning": "Ce résultat est indicatif. D'autres ventes en cours peuvent modifier les limites disponibles."
}
```

Règles :

- `ACCEPTABLE` : activer le bouton `Vendre`.
- `REQUIRES_CHANGES` : garder le panier modifiable et afficher `sellerInstruction`.
- `REJECTED_FINAL` : bloquer, par exemple tirage fermé ou session fermée.
- `preview` ne réserve pas l'exposition. Un `sell` peut encore retourner `EXPOSURE_CHANGED`.

### 6.3 Vente

```http
POST /tenant/cashier/tickets/sell
Idempotency-Key: <uuid-v4>
Content-Type: application/json

{
  "terminalId": "...",
  "drawId": "...",
  "drawChannelId": "...",
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
  "actionAvailability": {
    "canPrint": true,
    "canSendSms": true,
    "canSendWhatsapp": true,
    "canSendEmail": true,
    "canCopy": true
  },
  "sellerInstruction": "Vente acceptée. Donnez le code au client ou imprimez/envoyez le ticket."
}
```

Réponse refusée :

```json
{
  "outcome": "REJECTED",
  "ticketId": null,
  "ticketCode": null,
  "publicCode": null,
  "issues": [
    {
      "code": "EXPOSURE_CHANGED",
      "severity": "ERROR",
      "message": "La limite a changé pendant la vente.",
      "sellerInstruction": "Réduisez la mise et réessayez.",
      "lineIndex": 0,
      "details": {
        "allowedRemaining": "1.00 HTG"
      }
    }
  ],
  "backup": null,
  "actionAvailability": {
    "canPrint": false,
    "canSendSms": false,
    "canSendWhatsapp": false,
    "canSendEmail": false,
    "canCopy": false
  },
  "sellerInstruction": "Vente refusée. Ajustez le panier et réessayez."
}
```

Règles critiques :

- Le cashier POS expose seulement `ACCEPTED` ou `REJECTED`.
- Un cas qui nécessiterait approbation admin est retourné comme `REJECTED` / `REQUIRES_CHANGES` avec issue `APPROVAL_REQUIRED`.
- Sur `ACCEPTED`, afficher `backup.displayCode` immédiatement, avant print ou send.
- Sur `REJECTED`, aucun ticket n'a été créé.

### 6.4 Après `ACCEPTED`

L'écran de succès doit afficher :

1. Le code client en grand : `backup.displayCode`.
2. Le lien : `backup.verificationShortUrl`.
3. Les boutons :
   - `Copier le code`
   - `Copier le lien`
   - `Copier le message`
4. Les actions optionnelles :
   - `Imprimer`
   - `Envoyer SMS`
   - `Envoyer WhatsApp`
   - `Envoyer Email`

Règle UX : print/send sont des bonus. La preuve client minimale est le code + lien de vérification.

## 7. Reçu

### 7.1 Imprimer

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
```

Réponse :

```text
200 binary body
Content-Type: application/pdf | application/octet-stream
Content-Disposition: inline; filename="ticket-{displayCode}.pdf"
Cache-Control: no-store
```

Règles :

- `print` rend un fichier seulement.
- `print` n'envoie pas SMS, WhatsApp ou email.
- Pour partager un reçu texte, utiliser `/send` ou les boutons copier.
- Le mobile stream directement le fichier vers la file d'impression locale.

### 7.2 Envoyer

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

Réponse :

```json
{
  "ticketId": "uuid",
  "channel": "SMS",
  "recipient": "+50937700000",
  "queued": true,
  "deduplicated": false
}
```

Règles :

- L'envoi V1 est text-only.
- Pas de PDF en pièce jointe.
- Pas de stockage fichier obligatoire.
- Le contenu envoyé doit être identique à `backup.shareableText` si `includeLines=true` et `includeVerificationLink=true`.
- Dedup serveur : 60 secondes pour le même `(ticketId, channel, recipient)`.

## 8. Selection / betOption matrix

| Game | BetType | BetOption | Selection |
|---|---|---:|---|
| BOLET | `MATCH_1_2D` | `null` | 2 chiffres, ex. `11` |
| BOLET | `MATCH_2_2D` | `null` | 2 chiffres, ex. `11` |
| BOLET | `MATCH_3_2D` | `null` | 2 chiffres, ex. `11` |
| MARYAJ | `MARRIAGE_2D2D` | `1` Ordre exact | paire, ex. `21-25` |
| MARYAJ | `MARRIAGE_2D2D` | `2` Revers / Double | paire, ex. `21-25` |
| LOTO 3 | `LOTTO3_3D` | `1` Exact | 3 chiffres, ex. `012` |
| LOTO 3 | `LOTTO3_3D` | `2` Box | 3 chiffres, ex. `012` |
| LOTO 4 | `LOTTO4_PATTERN` | `1` Exact | 4 chiffres, ex. `1234` |
| LOTO 4 | `LOTTO4_PATTERN` | `2` Box | 4 chiffres, ex. `1234` |
| LOTO 4 | `LOTTO4_PATTERN` | `3` Front pair | 2 chiffres, ex. `12` |
| LOTO 4 | `LOTTO4_PATTERN` | `4` Back pair | 2 chiffres, ex. `34` |
| LOTO 5 | `LOTTO5_PATTERN` | `1` 1er lot + 2e lot | 5 chiffres, ex. `12345` |
| LOTO 5 | `LOTTO5_PATTERN` | `2` 1er lot + 3e lot | 5 chiffres, ex. `12345` |
| LOTO 5 | `LOTTO5_PATTERN` | `3` Mixte 1er/2e/3e lot | 5 chiffres, ex. `34567` |

Pièges :

- LOTO 4 front pair / back pair attendent 2 chiffres, pas 4.
- LOTO 5 option 3 est le mixte : dernier chiffre du 1er lot + 2 chiffres du 2e lot + 2 chiffres du 3e lot.
- Le mobile ne doit pas afficher les noms techniques `LOTTO5_PATTERN` au vendeur. Il affiche les labels humains.

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

## 10. Cancel

```http
POST /tenant/cashier/tickets/{ticketId}/cancel
Content-Type: application/json

{
  "terminalId": "...",
  "reason": "customer change of mind"
}
```

Réponse :

```json
{
  "ticketId": "uuid",
  "outcome": "CANCELLED",
  "cancelledAt": "2026-05-21T13:00:00Z",
  "issues": []
}
```

Fenêtre MVP : 3 minutes après la vente. Au-delà : `REJECTED` + issue `CANCEL_WINDOW_EXPIRED`.

## 11. Consultation des tickets

```http
GET /tenant/cashier/tickets?page=0&size=50&sort=createdAt,desc
```

Réponse :

```json
{
  "items": [
    {
      "id": "uuid",
      "ticketCode": "TCK-...",
      "displayCode": "40CP-JBMR",
      "status": "PLACED",
      "drawId": "uuid",
      "totalAmount": "15.00 HTG",
      "placedAt": "2026-05-21T13:00:00Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

Détail :

```http
GET /tenant/cashier/tickets/{ticketId}
```

Tri autorisé : `createdAt`, `totalAmount`, `ticketCode`.

## 12. Offline

```http
GET /tenant/cashier/offline/grant/current?terminalId=...&deviceId=...
```

Réponse :

```json
{
  "grantId": "uuid",
  "codes": ["..."],
  "validFrom": "2026-05-21T12:00:00Z",
  "validUntil": "2026-05-21T18:00:00Z",
  "maxTicketCount": 100,
  "maxTotalAmount": "10000.00 HTG"
}
```

Soumettre les ventes offline :

```http
POST /tenant/cashier/offline/submissions
Content-Type: application/json

{
  "grantId": "uuid",
  "clientBatchId": "uuid",
  "batchPayloadHash": "sha256...",
  "submissions": [
    {
      "clientSubmissionId": "uuid",
      "offlineCode": "...",
      "drawId": "uuid",
      "clientSoldAt": "2026-05-21T13:00:00Z",
      "totalStakeAmount": "10.00 HTG",
      "lines": [],
      "payloadHash": "sha256...",
      "signature": "ed25519..."
    }
  ]
}
```

Réponse :

```json
{
  "syncBatchId": "uuid",
  "outcomes": [
    {
      "clientSubmissionId": "uuid",
      "submissionId": "uuid",
      "outcome": "ACCEPTED",
      "rejectionCode": null,
      "rejectionReason": null
    }
  ]
}
```

Le serveur re-vérifie le payload hash et la signature device.

## 13. Erreurs HTTP communes

| Status | Cas | Action mobile |
|---:|---|---|
| 400 | Validation request body | Afficher `detail`, ne pas réessayer sans modif |
| 401 | JWT expiré ou invalide | Refresh ou re-login |
| 403 | Permissions / contexte refusé | Vérifier rôle + operational context |
| 404 | Tirage / ticket inexistant | Recharger |
| 409 | Idempotency conflict | Générer nouvelle clé après modification panier |
| 422 | Body OK mais règle métier échoue | Lire `issues`, corriger panier |
| 500 | Erreur serveur | Retry exponentiel max 3 fois |

## 14. Sequence happy-path

1. Login → JWT.
2. `GET /tenant/cashier/profile/current`.
3. `GET /tenant/cashier/session/current`.
4. Si pas de session : `POST /tenant/cashier/session/open`.
5. `POST /tenant/cashier/operational-context/select`.
6. `GET /tenant/cashier/draws/available`.
7. Boucle vente :
   - créer panier local ;
   - `POST /tenant/cashier/tickets/preview` ;
   - si acceptable, `POST /tenant/cashier/tickets/sell` avec `Idempotency-Key` fresh ;
   - afficher `backup.displayCode` ;
   - copier code/lien/message ;
   - optionnel : print ;
   - optionnel : send.
8. En fin de service : `POST /tenant/cashier/session/close`.

## 15. Références côté serveur

- Controllers : `features.cashier.{tickets,session,draws,offline,operationalcontext,profile}`
- Domaine : `core.sales.api.command.sell.SellTicketCommand`, `SaleAcceptanceEvaluator`, `TicketBackupAssembler`
- Tests E2E : `tchalanet-server/tests/e2e/tests/cashier/`
