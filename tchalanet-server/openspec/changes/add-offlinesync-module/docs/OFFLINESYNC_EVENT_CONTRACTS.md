# OFFLINESYNC — contrats d’events v2.1

## Règles générales

- Les events consommés hors module vivent dans `core.offlinesync.api.event`.
- Les events sont des faits passés.
- Publication après commit uniquement.
- Consommation idempotente obligatoire.
- Les events vers `sales` sont self-contained.
- `sales` ne requête pas `offlinesync` pour compléter le payload.

## 1. OfflineSubmissionTechValidatedEvent

Émis par `offlinesync` quand une submission passe les validations techniques.

```java
package com.tchalanet.server.core.offlinesync.api.event;

public record OfflineSubmissionTechValidatedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSubmissionId offlineSubmissionId,
    PromotionAttemptId promotionAttemptId,
    OfflineGrantId grantId,
    OfflineCodeId offlineCodeId,
    String offlineCode,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    String deviceId,
    Instant clientSoldAt,
    Instant receivedAt,
    Money totalStakeAmount,
    Integer lineCount,
    String payloadHash,
    List<OfflineSubmissionLinePayload> lines
) implements DomainEvent {}
```

## 2. OfflineSubmissionAdminApprovedEvent

Émis par `offlinesync` quand un admin approuve une submission en review.

```java
public record OfflineSubmissionAdminApprovedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSubmissionId offlineSubmissionId,
    PromotionAttemptId promotionAttemptId,
    UserId approvedBy,
    String reason,
    OfflineSubmissionPromotionPayload payload
) implements DomainEvent {}
```

## 3. OfflineSubmissionProcessedEvent

Émis par `sales` après tentative de promotion.

```java
package com.tchalanet.server.core.offlinesync.api.event;

public record OfflineSubmissionProcessedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSubmissionId offlineSubmissionId,
    PromotionAttemptId promotionAttemptId,
    OfflinePromotionOutcome outcome,
    TicketId ticketId,
    String rejectionCode,
    String rejectionReason,
    String salesDiagnosticCode
) implements DomainEvent {}
```

`outcome` :

```text
PROMOTED
BUSINESS_REJECTED
NEEDS_ADMIN_REVIEW
```

## Idempotence côté sales

Le listener `sales` utilise un handler key stable :

```text
sales.offline-promotion
```

Clé idempotence event :

```text
(tenant, handler_key, event_id)
```

En plus, la DB protège la création ticket :

```sql
UNIQUE (tenant_id, offline_submission_id)
```

## Idempotence côté offlinesync retour

Le listener `offlinesync` :

1. ignore l’event si déjà traité ;
2. charge la submission ;
3. compare `promotionAttemptId` ;
4. ignore si obsolète ;
5. applique la transition ;
6. marque l’event traité.

Pseudo-code :

```java
if (processedEvent.alreadyProcessed(HANDLER_KEY, event.eventId().value())) {
  return;
}

var submission = reader.getRequired(event.offlineSubmissionId());

if (!submission.currentPromotionAttemptId().equals(event.promotionAttemptId())) {
  log.warn("Ignoring stale offline promotion result");
  processedEvent.markProcessed(HANDLER_KEY, event.eventId().value());
  return;
}

submission.applySalesOutcome(event.outcome(), event.ticketId(), event.rejectionCode(), event.rejectionReason());
writer.save(submission);
processedEvent.markProcessed(HANDLER_KEY, event.eventId().value());
```
