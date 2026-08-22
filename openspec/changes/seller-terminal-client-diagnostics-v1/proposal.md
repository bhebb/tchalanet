# Change: seller-terminal-client-diagnostics-v1

## Why

When a seller terminal fails in the field, operators currently see the backend
request failure but not the mobile-side failure context. The Sunmi print issue is
the concrete example: staging logs showed `ticket.reprint.reason_required`, while
the terminal only displayed a generic unexpected error. Support needs a safe,
temporary way to see client errors for one affected seller terminal without
turning every POS device into a permanent telemetry source.

## What

- Add an explicit `platform.clientdiagnostics` capability for terminal-scoped
  client diagnostics.
- Expose the active diagnostics policy to the POS bootstrap/profile payload.
- Add a tenant-scoped batch endpoint for POS client diagnostic events.
- Capture mobile API, print, Flutter, and async errors while diagnostics is
  enabled.
- Add admin seller-terminal surfaces to enable/disable diagnostics and inspect
  recent diagnostic events.
- Keep retention short, payloads redacted, and activation time-bounded.

## Impact

- Backend: new `platform.clientdiagnostics` API, policy, ingestion, and persistence.
- Mobile: diagnostic event queue, redaction, and guarded error reporting.
- Web admin: seller-terminal diagnostics toggle and recent events view.
- Security: diagnostic events are tenant-scoped, permission-gated, redacted, and
  retained only for a short operational window.

## Non-goals

- Always-on client analytics.
- Full session replay, screenshots, PDF upload, or raw ticket payload capture.
- Replacing server logs, audit logs, or existing request IDs.
- Remote control of the POS device.
- Device management / MDM.
- Arbitrary remote log collection or remote log-level changes.
- Shipping a Sunmi printer adapter; this change only makes that class of client
  failure diagnosable.
