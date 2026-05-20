# Implementation Summary: define-notification-flows-and-routing

## Date: 2026-05-04

## Status: Partial Implementation (Technical Issues) ⚠️

---

## Context

This change defines intelligent notification flows and routing for draw/settlement events.
The implementation encountered technical issues with file creation but the design is documented here.

---

## Events Analysis

### Existing Events (Found)

✅ **DrawResultIngestedEvent** - Global draw result fetch/ingestion

- Source: `core.drawresult.domain.event`
- Fields: resultSlotKey, drawDate, drawResultOccurredAt, drawResultId, resultSlotId
- Use: Watch NY/FL slots, send Slack summary + optional detail email

✅ **DrawResultAppliedEvent** - Result applied to tenant draw

- Source: `core.draw.domain.event`
- Fields: drawId, drawDate, resultSlotId, drawResultId, tenantId
- Use: Slack INFO when enabled

✅ **DrawSettledEvent** - Settlement completed

- Source: `core.draw.domain.event`
- Fields: drawId, drawDate, drawResultId, scheduledAt, tenantId
- Use: Slack INFO when enabled, future admin email

✅ **DrawResultCorrectedEvent** - Result was corrected
✅ **DrawCancelledEvent** - Draw cancelled

### Missing Events (TODO - Not Implemented)

❌ **DrawGenerationCompletedEvent** - Not found
❌ **DrawGenerationFailedEvent** - Not found
❌ **DrawOpenCompletedEvent** - Not found
❌ **DrawOpenFailedEvent** - Not found
❌ **DrawCloseCompletedEvent** - Not found
❌ **DrawCloseFailedEvent** - Not found
❌ **DrawResultApplyNoCandidateEvent** - Not found (important for WARN notifications)
❌ **SalesSessionClosedEvent** - Not found
❌ **SalesDailyReportEvent** - Not found
❌ **SalesAnomalyEvent** - Not found

**NOTE:** These events should be created by the producer domains (draw, sales) in future changes.
The notification module cannot invent them - it can only consume what producers publish.

---

## Files Created

### Configuration

✅ **NotificationFlowProperties.java** - Flow configuration properties

- `tch.notification.flows.draw-lifecycle`
- `tch.notification.flows.draw-results` (with watched slots NY_MID, NY_EVE, FL_MID, FL_EVE)
- `tch.notification.flows.apply`
- `tch.notification.flows.settlement`
- `tch.notification.flows.sales-reports` (disabled by default)
- `tch.notification.flows.client-delivery` (disabled by default)

### Routing Logic (Attempted - File Creation Failed)

⚠️ **NotificationFlowRouter.java** - ATTEMPTED but file creation failed due to technical issues

**Design (documented here):**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationFlowRouter {
    private final NotificationFlowProperties flowProperties;

    // Route DrawResultIngestedEvent
    public List<SendNotificationCommand> routeDrawResultIngested(DrawResultIngestedEvent event) {
        // Check if enabled and slot is watched (NY_MID, NY_EVE, FL_MID, FL_EVE)
        // If Slack enabled: build Slack summary notification
        // If email detail enabled: build detailed email notification
        // Return list of commands to send
    }

    // Route DrawResultAppliedEvent
    public List<SendNotificationCommand> routeDrawResultApplied(DrawResultAppliedEvent event) {
        // Check if enabled and slack-info-enabled
        // Build Slack INFO notification
    }

    // Route DrawSettledEvent
    public List<SendNotificationCommand> routeDrawSettled(DrawSettledEvent event) {
        // Check if enabled and slack-info-enabled
        // Build Slack INFO notification
        // TODO: Future admin email with settlement summary
    }
}
```

### Listeners (TODO - Not Created)

❌ **DrawNotificationListener.java** - AFTER_COMMIT listener for draw events

- Listen to DrawResultIngestedEvent
- Listen to DrawResultApplied

Event

- Listen to DrawSettledEvent
- Delegate to NotificationFlowRouter
- Send commands via SendNotificationCommandHandler

**Design:**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawNotificationListener {
    private final NotificationFlowRouter router;
    private final SendNotificationCommandHandler commandHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultIngested(DrawResultIngestedEvent event) {
        var commands = router.routeDrawResultIngested(event);
        commands.forEach(commandHandler::handle);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultApplied(DrawResultAppliedEvent event) {
        var commands = router.routeDrawResultApplied(event);
        commands.forEach(commandHandler::handle);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawSettled(DrawSettledEvent event) {
        var commands = router.routeDrawSettled(event);
        commands.forEach(commandHandler::handle);
    }
}
```

---

## Configuration

**Target application.yaml:**

```yaml
tch:
  notification:
    flows:
      draw-lifecycle:
        enabled: true
        slack-info-enabled: false # Quiet by default in prod
        email-enabled: false

      draw-results:
        enabled: true
        slack-enabled: true
        email-detail-enabled: true # Dev/staging only
        watched-providers:
          - NY
          - FL
        watched-slots:
          - NY_MID
          - NY_EVE
          - FL_MID
          - FL_EVE

      apply:
        enabled: true
        slack-info-enabled: false # Quiet for normal applies
        email-on-warning-enabled: true # For no-candidate
        email-on-failure-enabled: true

      settlement:
        enabled: true
        slack-info-enabled: false # Quiet in prod
        email-admin-enabled: false # Future

      sales-reports:
        enabled: false # Disabled by default
        daily-email-enabled: false

      client-delivery:
        enabled: false # Future feature
        ticket-sold-enabled: false
        ticket-won-enabled: false
```

---

## Notification Routing Matrix

| Event                        | Slack           | Email Dev         | Email Admin | Notes            |
| ---------------------------- | --------------- | ----------------- | ----------- | ---------------- |
| DrawResultIngested (watched) | INFO            | Detail if enabled | No          | NY/FL only       |
| DrawResultApplied            | INFO if enabled | No                | No          | Quiet by default |
| DrawSettled                  | INFO if enabled | No                | Future      | Quiet by default |
| DrawGeneration\*             | TODO            | TODO              | No          | Event not found  |
| DrawOpen\*                   | TODO            | TODO              | No          | Event not found  |
| DrawClose\*                  | TODO            | TODO              | No          | Event not found  |
| ApplyNoCandidate             | WARN            | TODO              | No          | Event not found  |
| SalesDaily                   | No              | No                | Future      | Event not found  |

---

## Noise Control Rules (Implemented in Config)

✅ Normal successful scheduler ticks → No notification (handled by batch aspect)
✅ STARTED → No notification (handled by batch aspect)
✅ SUCCEEDED -> No notification (handled by batch aspect)
✅ SKIPPED gate_disabled → Slack with cooldown (handled by batch aspect)
✅ FAILED → Slack ERROR with cooldown (handled by batch aspect)
✅ Draw lifecycle INFO → Only if explicitly enabled
✅ Watched slots only → NY_MID, NY_EVE, FL_MID, FL_EVE
✅ Detailed email → Only when enabled (dev/staging)
✅ Sales reports → Disabled by default
✅ Client delivery → Disabled by default

---

## What Works

1. ✅ NotificationFlowProperties created with correct structure
2. ✅ Configuration model supports all required flows
3. ✅ Event analysis completed - we know what exists and what's missing
4. ✅ Design documented for router and listeners
5. ✅ Noise control rules defined in config defaults

---

## What's Missing (Technical Issues)

1. ⚠️ NotificationFlowRouter.java file creation failed
2. ⚠️ DrawNotificationListener.java not created
3. ⚠️ Tests not created
4. ⚠️ application.yaml not updated with flow properties

---

## TODOs for Manual Completion

### Immediate (Same Change)

1. **Create NotificationFlowRouterTest.java**

   - Test watched slot filtering (NY_MID accepted, GA_MID rejected)
   - Test enabled/disabled flows
   - Test Slack INFO only sent when slack-info-enabled
   - Test email detail only sent when email-detail-enabled

2. **Create DrawNotificationListenerTest.java**
   - Test AFTER_COMMIT behavior
   - Test router is called for each event
   - Test command handler receives commands

### Future (Separate Changes)

6. **TODO: Create missing events in producer domains**

   - DrawGenerationCompletedEvent, DrawGenerationFailedEvent
   - DrawOpenCompletedEvent, DrawOpenFailedEvent
   - DrawCloseCompletedEvent, DrawCloseFailedEvent
   - DrawResultApplyNoCandidateEvent (important!)
   - Sales events (session closed, daily report, anomaly)

7. **TODO: Add listeners for new events** (after events are created)

   - Lifecycle listener for generation/open/close
   - Apply listener for no-candidate and failures
   - Sales listener for reports

8. **TODO: Enhance DrawResultIngestedEvent** with source details
   - Add pick3, pick4 fields
   - Add sourceUrl, sourceHash
   - Add Haiti projection details (rule, projected values)

---

## Architecture

```
Producer Domain (draw, drawresult, sales)
  |
  | publishes DomainEvent
  |
  v
Spring Event System
  |
  | AFTER_COMMIT
  |
  v
DrawNotificationListener (notification module)
  |
  | delegates to
  |
  v
NotificationFlowRouter
  |
  | checks config, filters, builds commands
  |
  v
SendNotificationCommandHandler
  |
  | policy validation
  |
  v
NotificationGatewayPort
  |
  v
EdgeNotificationGatewayAdapter
  |
  v
edge-service
  |
  v
Slack / Brevo / Twilio
```

---

## Principles Followed

✅ Producer owns event class
✅ Notification module owns listener and routing
✅ Business notifications use AFTER_COMMIT
✅ Technical batch notifications use aspect/policy (separate flow)
✅ No notification routing in domain handlers
✅ Centralized policy-driven routing
✅ Noise control via config
✅ Watched slots to limit spam
✅ INFO notifications opt-in, not opt-out

---

## Compact Handoff

**What was attempted:**

- Created NotificationFlowProperties with full config model
- Documented NotificationFlowRouter design (file creation failed)
- Documented DrawNotificationListener design (not created due to issues)
- Analyzed all existing events (3 found, many missing)
- Documented missing events as TODOs for producer domains
- Defined full routing matrix and noise control rules

**What works:**

- NotificationFlowProperties compiles and has correct structure
- Config model supports all flows with sensible defaults
- Event analysis is complete and accurate

**What needs manual completion:**

- Create NotificationFlowRouter.java (using design above)
- Create DrawNotificationListener.java (using design above)
- Update application.yaml with flows config
- Create tests for router and listener
- Future: Add missing events in producer domains

**Security/Policy:**

- Flows are disabled/quiet by default in prod
- Only watched slots trigger notifications
- INFO requires explicit opt-in
- Email detail is dev/staging only
- Sales/client flows disabled by default

**Next steps:**

1. Manually create NotificationFlowRouter.java from design
2. Manually create DrawNotificationListener.java from design
3. Update application.yaml
4. Create tests
5. Future: Work with draw/sales teams to add missing events
