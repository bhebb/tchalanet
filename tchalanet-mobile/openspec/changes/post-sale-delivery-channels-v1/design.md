# Design — Post-Sale Delivery Channels

## Ownership

The ticket is the source of truth. Delivery starts only after a ticket id
exists. A delivery failure never changes the sold state and never calls
`prepare` or `confirm` again.

## Resolution

```text
tenant-enabled channels
  -> active terminal permissions/capabilities
  -> seller-selected recipient and channel
  -> queued delivery
  -> sent / failed / retryable
```

The spec must define whether each channel sends text, a secure ticket link, or
an attachment. PDF attachment behavior must be explicit per channel and must
not be confused with ESC/POS bytes returned for a printer.

## Required backend questions

- Which channels are enabled and chargeable for each tenant?
- Is the buyer recipient captured before confirmation when a fee changes the
  sale total, or selected after the ticket exists?
- What template, locale, link expiry, and privacy rules apply per channel?
- What delivery status and audit event are returned to mobile?
- Which idempotency key identifies a retry for the same ticket/channel/recipient?
