# Add offlinesync module — Design v2.1

## Architecture

`core.offlinesync` suit l’archétype core : `api/` + `internal/{domain,application,infra}`.

## Flow grant

```text
POS online
  -> POST /tenant/offline/grants
  -> ctx.trustedOperationalContextRequired()
  -> ResolvePosOperationContextQuery
  -> GetOfflineLimitPolicyQuery
  -> OfflineGrantPolicy
  -> create grant + code batch + codes
  -> response signed by server
```

## Flow sale offline local

```text
Flutter
  -> validate cached grant signature
  -> check validUntil
  -> reserve local code
  -> create local sale
  -> sign sale payload with device private key
  -> print offline ticket
```

## Flow sync

```text
POS online again
  -> POST /tenant/offline/sync
  -> ctx.trustedOperationalContextRequired()
  -> ResolvePosOperationContextQuery
  -> create OfflineSyncBatch
  -> for each submission:
       duplicate/hash check
       lock code AVAILABLE -> RESERVED
       verify grant/device/signature/windows/quota
       TECH_VALIDATED or TECH_REJECTED
       publish OfflineSubmissionTechValidatedEvent after commit
```

## Flow promotion

```text
OfflineSubmissionTechValidatedEvent
  -> sales listener
  -> CreateTicketFromOfflineSubmissionCommand
  -> DB unique (tenant_id, offline_submission_id)
  -> OfflineSubmissionProcessedEvent
  -> offlinesync listener
  -> apply outcome if promotionAttemptId current
```

## Transaction boundaries

- `SyncOfflineSalesCommandHandler` est transactionnel.
- Events publiés after-commit.
- Listener `sales` exécute sa propre transaction.
- Listener retour `offlinesync` exécute sa propre transaction.

## Idempotence

### Batch

```text
(tenant_id, grant_id, client_batch_id)
```

Même hash -> replay du résultat.
Hash différent -> conflit.

### Submission

```text
(tenant_id, client_submission_id)
```

Même payloadHash -> duplicate API.
Hash différent -> conflit.

### Event

```text
(tenant_id, handler_key, event_id)
```

### Ticket

```text
UNIQUE (tenant_id, offline_submission_id)
```

## Security

- Grant signé serveur.
- Submission signée device Ed25519.
- Device private key non exfiltrable côté Android si possible.
- Backend vérifie avec `devicePublicKey` stockée dans le grant.

## RLS

Toutes les tables `offline_*` sont tenant-scoped.
Les queries applicatives ne filtrent pas explicitement par `tenant_id` quand RLS est censée s’appliquer.
