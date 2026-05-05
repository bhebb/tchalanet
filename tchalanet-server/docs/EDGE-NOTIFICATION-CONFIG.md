# Edge Notification Gateway Configuration

## Overview

The Edge Notification Gateway integration allows `tchalanet-server` to send notifications through `tchalanet-edge-service` for Slack, Email, and SMS delivery.

## Configuration Properties

Add to `application.yml`:

```yaml
tch:
  notification:
    edge:
      enabled: true
      base-url: "http://localhost:3000"  # Local development
      # base-url: "http://tchalanet-edge-service:3000"  # Docker deployment
      notifications-path: "/internal/notifications/send"
      hmac-secret: "${EDGE_INTERNAL_HMAC_SECRET}"
      connect-timeout: 2s
      read-timeout: 5s
```

## Environment Variables

### EDGE_INTERNAL_HMAC_SECRET

**Required when `enabled=true`**

Shared secret for HMAC-SHA256 signing of internal edge calls.

- **Local development**: Set in `.env` file
- **Docker deployment**: Set in docker-compose or Kubernetes secrets
- **Production**: Use secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.)

Example `.env` entry:
```bash
EDGE_INTERNAL_HMAC_SECRET=your-very-long-random-secret-key-here
```

Generate a secure secret:
```bash
openssl rand -hex 32
```

## URL Configuration by Environment

| Environment | Base URL |
|-------------|----------|
| Local dev (edge started separately) | `http://localhost:3000` |
| Docker Compose | `http://tchalanet-edge-service:3000` |
| Kubernetes | `http://edge-service.default.svc.cluster.local:3000` |

## Batch Notifications

Batch technical notifications are automatically sent through the edge gateway:

- **STARTED**: Silent (never sent)
- **SUCCEEDED**: Silent (never sent)
- **SKIPPED**: Sent only when code = `gate_disabled`, subject to 30-minute cooldown
- **FAILED**: Always sent, subject to 30-minute cooldown

Cooldown prevents notification spam when scheduled jobs fail repeatedly.

## Security

All internal calls to edge-service include HMAC headers:

- `X-Request-Id`: Request correlation ID
- `Idempotency-Key`: Idempotency key for duplicate prevention
- `X-Tch-Timestamp`: ISO-8601 timestamp
- `X-Tch-Signature`: HMAC-SHA256 signature (sha256=hex)

The signature payload is: `timestamp + "." + rawJsonBody`

## Testing

Disable edge integration during tests:

```yaml
tch:
  notification:
    edge:
      enabled: false
```

## Migration from Node Naming

Old property prefix `tch.notification.node.*` has been renamed to `tch.notification.edge.*`.

Update any existing configuration files accordingly.

