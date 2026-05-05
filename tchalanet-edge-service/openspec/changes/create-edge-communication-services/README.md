# create-edge-communication-services

OpenSpec change for adding the first real communication services to `tchalanet-edge-service`.

## Copy location

Copy this folder to:

```text
openspec/changes/create-edge-communication-services/
```

## Files

```text
proposal.md
 tasks.md
 design.md
 claude-prompt.md
```

## Goal

Add:

- Slack notifications
- Brevo email notifications
- Twilio SMS notifications
- `POST /internal/notifications/send`
- manual curl test for Slack/email

Not included yet:

- WhatsApp sender
- ticket attachments
- Redis anti-spam
- templates
- rules
- feature management
