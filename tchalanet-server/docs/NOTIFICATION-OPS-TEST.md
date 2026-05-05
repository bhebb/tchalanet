# Notification Use Cases - Ops Test Endpoint

## Overview

The server provides controlled notification use cases through command handlers. Web/mobile clients must use business-specific endpoints (like ticket delivery), not a generic send endpoint.

For ops/testing purposes, a SUPER_ADMIN-only endpoint is available.

## Ops Test Endpoint

**Endpoint:** `POST /api/v1/ops/notifications/test`

**Authorization:** SUPER_ADMIN role required

**Purpose:** Test notification delivery through edge-service manually

---

## Slack Notification

Send a test Slack notification to a configured channel.

### Request

```bash
curl -X POST http://localhost:8080/api/v1/ops/notifications/test \
  -H "Authorization: Bearer YOUR_SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SLACK",
    "channelKey": "batch-draws",
    "severity": "INFO",
    "title": "Tchalanet Server Test",
    "message": "Spring Boot successfully called edge-service via Slack.",
    "context": {
      "source": "ops-manual-test",
      "timestamp": "2026-05-04T19:00:00Z"
    }
  }'
```

### Response

```json
{
  "status": "SUCCESS",
  "data": {
    "success": true,
    "message": "Notification sent successfully",
    "idempotencyKey": "SYSTEM_MESSAGE_abc123-...",
    "channel": "SLACK"
  },
  "notices": [],
  "services": []
}
```

### Available Slack Channels

Configure in edge-service:
- `batch-draws`
- `ops-alerts`
- `platform-monitoring`

---

## Email Notification

Send a test email notification.

### Request

```bash
curl -X POST http://localhost:8080/api/v1/ops/notifications/test \
  -H "Authorization: Bearer YOUR_SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "EMAIL",
    "to": "admin@example.com",
    "severity": "INFO",
    "title": "Tchalanet Email Test",
    "message": "This is a test email sent from Spring Boot through edge-service and Brevo.",
    "context": {
      "environment": "staging",
      "test": "ops-email"
    }
  }'
```

### Response

```json
{
  "status": "SUCCESS",
  "data": {
    "success": true,
    "message": "Notification sent successfully",
    "idempotencyKey": "SYSTEM_MESSAGE_def456-...",
    "channel": "EMAIL"
  },
  "notices": [],
  "services": []
}
```

---

## SMS Notification

Send a test SMS notification.

### Request

```bash
curl -X POST http://localhost:8080/api/v1/ops/notifications/test \
  -H "Authorization: Bearer YOUR_SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SMS",
    "to": "+15145551234",
    "severity": "WARNING",
    "title": "Tchalanet SMS Test",
    "message": "Test SMS from Spring Boot via edge-service and Twilio.",
    "context": {
      "environment": "staging"
    }
  }'
```

### Response

```json
{
  "status": "SUCCESS",
  "data": {
    "success": true,
    "message": "Notification sent successfully",
    "idempotencyKey": "SYSTEM_MESSAGE_ghi789-...",
    "channel": "SMS"
  },
  "notices": [],
  "services": []
}
```

**Note:** Phone numbers must:
- Start with `+`
- Be 6-15 digits
- Use international format (e.g., `+1` for USA/Canada)

---

## Error Responses

### Validation Error (Missing channelKey for Slack)

```bash
curl -X POST http://localhost:8080/api/v1/ops/notifications/test \
  -H "Authorization: Bearer YOUR_SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SLACK",
    "severity": "INFO",
    "title": "Test",
    "message": "This will fail"
  }'
```

Response:
```json
{
  "status": "SUCCESS_WITH_WARNINGS",
  "data": {
    "success": false,
    "message": "Test failed: SLACK requires channelKey",
    "idempotencyKey": null,
    "channel": "SLACK"
  },
  "notices": [
    {
      "code": "TEST_ERROR",
      "message": "SLACK requires channelKey",
      "severity": "ERROR"
    }
  ],
  "services": []
}
```

### Unauthorized (Non-SUPER_ADMIN)

```bash
curl -X POST http://localhost:8080/api/v1/ops/notifications/test \
  -H "Authorization: Bearer REGULAR_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ ... }'
```

Response: `403 Forbidden` or `401 Unauthorized`

---

## Severity Levels

Supported severity levels:
- `INFO` - Informational message
- `WARNING` - Warning that needs attention
- `ERROR` - Error condition
- `CRITICAL` - Critical system failure

---

## Channel-Specific Requirements

| Channel   | Required Fields      | Optional Fields | Example                      |
|-----------|---------------------|-----------------|------------------------------|
| SLACK     | `channelKey`        | -               | `"batch-draws"`              |
| EMAIL     | `to` (email)        | -               | `"user@example.com"`         |
| SMS       | `to` (phone)        | -               | `"+15145551234"`             |
| WHATSAPP  | `to` (phone)        | -               | `"+15145551234"`             |

---

## Business Endpoints (Future)

Web/mobile clients should NOT use the ops test endpoint. They should use business-specific endpoints:

### Ticket Delivery (Future)
```
POST /api/v1/tickets/{ticketId}/send
POST /api/v1/tickets/{ticketId}/resend
```

### Admin Announcements (Future)
```
POST /api/v1/admin/announcements
```

### Sales Reports (Future)
```
POST /api/v1/sales/sessions/{sessionId}/report/send
```

These endpoints will:
- Validate permissions and ownership
- Apply business rules
- Select appropriate templates
- Determine recipient and channel
- Call `SendNotificationCommand` internally

---

## Architecture

```
OpsNotificationController (SUPER_ADMIN only)
  -> SendNotificationCommand
  -> SendNotificationCommandHandler
    -> NotificationPolicy (validates recipients)
    -> NotificationGatewayPort
    -> EdgeNotificationGatewayAdapter
    -> edge-service (Slack/Brevo/Twilio)
```

---

## Security Notes

1. **Ops endpoint is SUPER_ADMIN only** - Regular users cannot access
2. **Business endpoints will validate ownership** - Users can only send their own tickets
3. **Policy enforces channel rules** - Cannot bypass validation
4. **Edge service validates HMAC** - Internal calls are signed
5. **Idempotency keys prevent duplicates** - Safe to retry

---

## Testing Checklist

- [ ] Slack notification with valid channelKey
- [ ] Slack notification fails without channelKey
- [ ] Email notification with valid email
- [ ] Email notification fails with invalid email
- [ ] SMS notification with valid phone (+prefix)
- [ ] SMS notification fails without + prefix
- [ ] Non-SUPER_ADMIN cannot access endpoint
- [ ] Edge service receives correct HMAC headers
- [ ] Idempotency key is returned in response
- [ ] Context fields are included in edge payload

---

## Troubleshooting

### Slack notification not appearing

1. Check edge-service logs
2. Verify Slack webhook URL in edge config
3. Check channelKey matches configured channel
4. Verify edge-service is running

### Email not received

1. Check edge-service logs
2. Verify Brevo API key in edge config
3. Check spam folder
4. Verify "from" email is configured in edge

### SMS not received

1. Check edge-service logs
2. Verify Twilio credentials in edge config
3. Verify phone number format (+prefix)
4. Check Twilio account balance/limits

### 403 Forbidden

1. Verify token is valid
2. Check user has SUPER_ADMIN role
3. Verify authentication headers

### Edge service unavailable

1. Check edge-service is running: `curl http://localhost:3000/health`
2. Check network connectivity
3. Verify base URL in server config
4. Check edge service logs

