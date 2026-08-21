# Design: seller terminal client diagnostics

## Context

This is a cross-project change. The backend capability is
`platform.clientdiagnostics`: a transverse platform service with state and
lifecycle, exposing only `api/` to other modules. Mobile sends events only when
the policy says it may. Tenant admin enables diagnostics on a seller terminal for
a bounded period and inspects the collected events.

Relevant context packs:

- `openspec/context/05-version-guard.md`
- `tchalanet-server/openspec/context/10-non-negotiables.md`
- `tchalanet-server/openspec/context/79-request-context-rules.md`
- `tchalanet-mobile/docs/ARCHITECTURE.md`

## Backend module

Target structure:

```text
platform.clientdiagnostics
  api/
    ClientDiagnosticsApi
    model/
  internal/
    policy/
    ingestion/
    persistence/
    web/
```

The capability does not belong to `core.sellerterminal` or a `features` module:
it has no seller-terminal business invariant. It is operational application
state tied to a terminal lifecycle and request context.

## Policy model

A seller terminal can have an active diagnostics policy:

- `enabled`: boolean
- `expiresAt`: required when enabling
- `enabledBy`: admin/ops user
- `reason`: required free text
- `maxEvents`: maximum events accepted during the activation window
- `categories`: closed set: `API`, `CONNECTIVITY`, `SALE`, `PRINT`,
  `SCANNER`, `PRINTER_CONFIG`, `FLUTTER`, `ASYNC`, `DEVICE`

The policy is returned in the POS profile/bootstrap payload only for the current
authenticated terminal context:

```yaml
diagnostics:
  enabled: true
  expiresAt: "2026-08-20T18:00:00Z"
  maxEvents: 100
  categories:
    - API
    - CONNECTIVITY
    - SALE
    - PRINT
    - SCANNER
    - PRINTER_CONFIG
    - FLUTTER
    - ASYNC
    - DEVICE
```

`expiresAt` is mandatory. Backend SHALL enforce a maximum activation duration,
with no "until further notice" mode. The default target is max 24 hours, with UI
presets favoring much shorter windows such as 30 minutes, 1 hour, and a few
hours.

## Event model

The mobile client posts normalized batches, not raw logs.

HTTP endpoints:

- `POST /tenant/client-diagnostics/events`
- `GET /admin/seller-terminals/{sellerTerminalId}/diagnostics`
- `POST /admin/seller-terminals/{sellerTerminalId}/diagnostics`
- `DELETE /admin/seller-terminals/{sellerTerminalId}/diagnostics`
- `GET /admin/seller-terminals/{sellerTerminalId}/diagnostics/events`

The event schema is closed and whitelisted. Avoid generic fields such as
`payload`, `requestBody`, `responseBody`, unrestricted `metadata`, or raw
`stackTrace`.

Event fields:

- `eventId`
- `category`: `API`, `CONNECTIVITY`, `SALE`, `PRINT`, `SCANNER`,
  `PRINTER_CONFIG`, `FLUTTER`, `ASYNC`, `DEVICE`
- `occurredAtClient`
- `severity`: `ERROR` or `WARN`
- `operation`
- `errorCode`
- `message`
- `exceptionType`
- `requestId`
- `correlationId`
- `httpStatus`
- `endpointKey`
- `appVersion`
- `buildNumber`
- `platform`
- `deviceModel`
- `osVersion`
- `printerProvider`
- `printerService`
- `printerState`
- `stackFrames[]`

Server-derived fields:

- `tenantId`
- `sellerTerminalId`
- `sellerId`
- `receivedAtServer`
- `ingestionStatus`

The backend resolves terminal and tenant from request context. The client never
supplies tenant or terminal identifiers as authority.

## Whitelist and limits

Mobile sends only the whitelisted schema; backend validates again. Events must
not include:

- auth tokens, Firebase tokens, cookies, or headers
- passwords, secrets, private keys
- full sale payloads
- full PDF/receipt bytes
- unrestricted stack traces with arbitrary locals

Backend enforces:

- max event body size
- accepted category/severity/code formats
- idempotency on `(sellerTerminalId, clientEventId)`
- rate limits per terminal
- policy `maxEvents`
- retention expiry, normative default 7 days

Retention purge SHALL run as a thin scheduler/batch that delegates application
work to the capability service.

## Mobile capture points

The mobile POS should emit events for:

- API errors mapped by `ApiClient`, including `ProblemDetail.code` and request ID
- print failures in `PrinterService` and `printTicket`
- missing printer adapter or unsupported mode
- Flutter framework errors
- uncaught async errors in the root zone

For the Sunmi case, expected event examples:

- `PRINT_SERVER_REJECTED`, code `ticket.reprint.reason_required`
- `PRINT_ADAPTER_UNAVAILABLE`, provider `bluetooth_esc_pos`, mode `POS_DIRECT`
- `PRINT_UNEXPECTED_ERROR`, with sanitized stack

Mobile batching:

- errors enter a bounded local queue
- batches contain 5-20 events
- oldest events are dropped first when the queue is full
- retries are finite and best-effort
- diagnostics disabled or `expiresAt` passed destroys the queue

## Admin surface

Seller terminal detail/configuration owns terminal-specific diagnostics:

- diagnostics status: off, active until, expired
- enable action: duration, reason, level
- disable action
- recent events table with compact rows
- event detail drawer with sanitized whitelisted fields only

Keep the UI intentionally small: state, expiry, terminal/app summary, disable
button, recent events, and a sanitized detail view. It is not a log analytics
console.

## Permissions

Suggested permissions:

- `seller-terminal.diagnostics.read`
- `seller-terminal.diagnostics.manage`
- `client_diagnostics.write`

Cashier/mobile users may submit diagnostics only for their current terminal when
the server policy is active. They may not read or activate diagnostics.

If the V0 seller-terminal permission set is hardcoded, `client_diagnostics.write`
must be added explicitly to avoid POS devices receiving `403` on ingestion.

## Failure behavior

Diagnostic submission must never block POS flows. If the diagnostic endpoint
fails, mobile stores a small bounded queue and retries opportunistically while
the policy remains active. Expired policies stop capture and clear unsent events
that are no longer allowed.
