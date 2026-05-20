# OFFLINESYNC — contrat REST v2.1

Les chemins sont déclarés sans `/api/v1`; le préfixe global est porté par `spring.mvc.servlet.path`.

## POS vendeur

### POST `/tenant/offline/grants`

Demande ou renouvelle un grant offline.

Sécurité :

```text
@PreAuthorize("hasPermission('offlinesync.grant.request')")
@AuditLog(action="OFFLINE_GRANT_REQUEST", entity="offline_grant")
```

Request :

```json
{
  "terminalId": "uuid",
  "deviceId": "android-device-id",
  "devicePublicKey": "base64-ed25519-public-key",
  "keyId": "device-key-v1",
  "renewExisting": false
}
```

Règles :

- `ctx.trustedOperationalContextRequired()` obligatoire.
- Validation POS via query dédiée.
- Pas de grant si offline côté serveur ou contexte non validé.

Response :

```json
{
  "grantId": "uuid",
  "codeBatchId": "uuid",
  "validFrom": "2026-05-18T15:00:00Z",
  "validUntil": "2026-05-18T19:00:00Z",
  "syncAcceptedUntil": "2026-05-25T19:00:00Z",
  "limits": {
    "maxTicketCount": 100,
    "maxTotalAmountCents": 500000
  },
  "serverSignature": "base64",
  "codes": [
    { "codeId": "uuid", "code": "TCH-9K2M-7Q" }
  ]
}
```

### GET `/tenant/offline/grants/current`

Retourne le grant actif ou renouvelable pour le contexte POS.

### POST `/tenant/offline/sync`

Synchronise un batch de ventes offline.

Sécurité :

```text
@PreAuthorize("hasPermission('offlinesync.sync')")
@AuditLog(action="OFFLINE_SYNC", entity="offline_sync_batch")
```

Request :

```json
{
  "grantId": "uuid",
  "codeBatchId": "uuid",
  "clientBatchId": "flutter-local-batch-id",
  "batchPayloadHash": "sha256",
  "deviceId": "android-device-id",
  "submissions": [
    {
      "clientSubmissionId": "local-sale-id",
      "offlineCode": "TCH-9K2M-7Q",
      "clientSoldAt": "2026-05-18T16:01:00Z",
      "payloadHash": "sha256",
      "signature": "base64-ed25519-signature",
      "totalStakeAmountCents": 5000,
      "currency": "HTG",
      "lines": []
    }
  ]
}
```

Response :

```json
{
  "syncBatchId": "uuid",
  "status": "PARTIALLY_ACCEPTED",
  "results": [
    {
      "clientSubmissionId": "local-sale-id",
      "result": "ACCEPTED_FOR_PROMOTION",
      "offlineSubmissionId": "uuid",
      "code": "offlinesync.accepted"
    },
    {
      "clientSubmissionId": "local-sale-id-2",
      "result": "DUPLICATE",
      "offlineSubmissionId": "original-uuid",
      "code": "offlinesync.duplicate"
    }
  ]
}
```

Résultats API possibles :

```text
ACCEPTED_FOR_PROMOTION
TECH_REJECTED
DUPLICATE
PAYLOAD_MISMATCH
BATCH_REJECTED
```

### GET `/tenant/offline/submissions/my`

Liste paginée des submissions du vendeur courant.

### GET `/tenant/offline/submissions/{offlineSubmissionId}/status`

Polling du résultat business après promotion.

## Admin tenant

### GET `/admin/offline/submissions`

Liste paginée filtrable.

Filtres :

```text
status
terminalId
outletId
sellerUserId
from
to
```

### POST `/admin/offline/submissions/{id}/approve`

Approve une submission en review et publie un nouvel event avec un nouveau `promotionAttemptId`.

### POST `/admin/offline/submissions/{id}/reject`

Rejette définitivement une submission.

### POST `/admin/offline/submissions/{id}/replay-dry-run`

Ne modifie pas l’état. Produit un rapport de diagnostic.

## Platform/Ops

### POST `/platform/ops/offline/grants/{id}/revoke`

Révocation forcée avec raison obligatoire.

### POST `/platform/ops/offline/jobs/recover-stuck-submissions`

Déclenche un job de recovery audité.
