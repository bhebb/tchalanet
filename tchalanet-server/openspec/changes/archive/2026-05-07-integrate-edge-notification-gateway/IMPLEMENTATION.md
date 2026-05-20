# Implementation Summary: integrate-edge-notification-gateway

## Date: 2026-05-04

## Status: Implementation Complete ✅

---

## Files Created

### Common Batch Notification

- ✅ `BatchNotificationPolicy.java` - Policy for should-send decisions and cooldown logic

### Core Notification Infrastructure

- ✅ `EdgeNotificationProperties.java` - Configuration properties for edge integration
- ✅ `EdgeNotificationConfig.java` - Spring configuration bean
- ✅ `EdgeHmacSigner.java` - HMAC-SHA256 signing for internal edge calls
- ✅ `EdgeNotificationGatewayAdapter.java` - Main adapter implementing NotificationGatewayPort

### Tests

- ✅ `BatchNotificationPolicyTest.java` - 8 test scenarios covering all policy rules
- ✅ `EdgeHmacSignerTest.java` - 4 test scenarios for deterministic HMAC signing
- ✅ `EdgeNotificationGatewayAdapterTest.java` - 5 test scenarios for adapter behavior

### Documentation

- ✅ `EDGE-NOTIFICATION-CONFIG.md` - Configuration guide and environment setup

---

## Files Modified

### Common Notification

- ✅ `NotificationGatewayPort.java` - Updated comment (Node → Edge)
- ✅ `NotificationTarget.java` - Made all fields nullable for batch notifications
- ✅ `SendNotificationPayload.java` - Made target nullable for batch notifications

### Batch Notification Service

- ✅ `BatchEventNotificationService.java` - Refactored to use BatchNotificationPolicy
  - Extracted shouldNotify logic to policy
  - Extracted cooldown logic to policy
  - Added enhanced payload mapping for edge contract
  - Added title, message, severity, channelKey to data map

### Configuration

- ✅ `application.yaml` - Replaced `notification.node.*` with `notification.edge.*`

---

## Files Deleted

- ✅ `NodeNotificationConfigProperties.java` - Replaced by EdgeNotificationProperties
- ✅ `NodeNotificationConfig.java` - Replaced by EdgeNotificationConfig
- ✅ `NodeNotificationGatewayAdapter.java` - Replaced by EdgeNotificationGatewayAdapter
- ✅ `NotificationHttpGatewayAdapter.java` - Removed old stub implementation

---

## Implementation Details

### Edge HTTP Contract

Request DTO matches edge-service `/internal/notifications/send`:

```json
{
  "eventId": "evt_...",
  "severity": "INFO|WARNING|ERROR",
  "title": "...",
  "message": "...",
  "recipients": [
    {
      "channel": "SLACK|EMAIL|SMS",
      "to": "user@example.com",       // for EMAIL/SMS
      "channelKey": "batch-draws"      // for SLACK
    }
  ],
  "context": { ... }
}
```

### HMAC Signing

All edge calls include headers:

- `X-Request-Id`: Correlation ID
- `Idempotency-Key`: Deduplication key
- `X-Tch-Timestamp`: ISO-8601 timestamp
- `X-Tch-Signature`: sha256=hex(HMAC(secret, timestamp + "." + rawJsonBody))

### Batch Notification Rules

| Status    | Code          | Action              | Cooldown |
| --------- | ------------- | ------------------- | -------- |
| STARTED   | -             | Silent (never send) | -        |
| SUCCEEDED | -             | Silent (never send) | -        |
| SKIPPED   | gate_disabled | Send                | 30 min   |
| SKIPPED   | other         | Silent              | -        |
| FAILED    | any           | Send                | 30 min   |

Cooldown fingerprint: `jobKey:tenantId:status:code`

---

## Configuration

```yaml
tch:
  notification:
    edge:
      enabled: true
      base-url: 'http://localhost:3000'
      notifications-path: '/internal/notifications/send'
      hmac-secret: '${EDGE_INTERNAL_HMAC_SECRET}'
      connect-timeout: 2s
      read-timeout: 5s
```

Environment variable required:

```bash
EDGE_INTERNAL_HMAC_SECRET=<secure-random-secret>
```

---

## Tests Coverage

### BatchNotificationPolicy

- ✅ STARTED never sends
- ✅ SUCCEEDED never sends
- ✅ SKIPPED with other code never sends
- ✅ SKIPPED with gate_disabled sends (when allowed)
- ✅ FAILED always sends (when allowed)
- ✅ Cooldown blocks repeat sends within 30 minutes
- ✅ Cooldown allows send after 30 minutes
- ✅ Cache unavailable allows send (fail-open)

### EdgeHmacSigner

- ✅ Produces deterministic signature
- ✅ Same input produces same signature
- ✅ Different secret produces different signature
- ✅ Different body produces different signature

### EdgeNotificationGatewayAdapter

- ✅ Sends Slack notification with channelKey
- ✅ Sends Email notification with to field
- ✅ Does not send when disabled
- ✅ Includes all HMAC headers
- ✅ Handles typed IDs correctly (converts to strings)

---

## Known Issues / Pre-existing Errors

The following compilation errors exist on branch `feature/draw-review` but are **not related to this change**:

- `GameJpaEntity` - missing setters (GameAdminService)
- `I18nOverrideEntity` - missing getters (I18nOverridesCatalogImpl)
- `GetNextDrawQuery` - duplicate class declaration

These should be fixed in a separate change.

---

## TODOs / Future Improvements

- [ ] Notification outbox/retry persistence (separate change)
- [ ] Draw result notification flows (separate change)
- [ ] Sales report notifications (separate change)
- [ ] Client ticket SMS/email flows (separate change)
- [ ] Redis-based cooldown for multi-instance deployments (optional improvement)
- [ ] Web/mobile notification endpoints (separate change)

---

## Verification Checklist

- ✅ All new files created
- ✅ Old Node files removed
- ✅ Configuration migrated from node._ to edge._
- ✅ NotificationTarget and SendNotificationPayload allow null target
- ✅ BatchEventNotificationService uses BatchNotificationPolicy
- ✅ HMAC signing implemented with Clock injection
- ✅ Edge DTO matches edge-service contract
- ✅ Unit tests cover all policy rules
- ✅ Documentation created
- ⚠️ Full test suite blocked by pre-existing compilation errors (unrelated)

---

## Risks

1. **Pre-existing compilation errors**: The branch has unrelated errors that prevent full compilation and test execution. These need to be fixed separately.

2. **Edge service availability**: If edge-service is down, notifications will fail. Current implementation logs errors but does not persist for retry (intentional - retry logic is a future enhancement).

3. **HMAC secret management**: Must be properly secured in production environments.

---

## Compact Handoff

**What was done:**

- Renamed Node → Edge notification integration
- Created EdgeNotificationGatewayAdapter with HMAC signing
- Refactored batch notifications to use BatchNotificationPolicy
- Added cooldown deduplication (30 minutes, cache-based)
- Made NotificationTarget nullable for technical notifications
- Created comprehensive unit tests
- Updated configuration (application.yaml + docs)

**What compiles:**

- All notification/batch files compile successfully
- Tests are syntactically correct

**What's blocked:**

- Full build blocked by pre-existing errors in GameJpaEntity and I18nOverrideEntity (unrelated domains)

**Next steps:**

1. Fix pre-existing compilation errors in catalog.game and catalog.i18n
2. Run full test suite
3. Test integration with running edge-service locally
4. Deploy and verify in staging environment
