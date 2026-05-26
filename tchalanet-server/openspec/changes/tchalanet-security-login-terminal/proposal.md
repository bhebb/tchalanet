## Why

Tchalanet must support POS and phone sales without allowing a valid user token to transact from an untrusted device, wrong terminal, wrong outlet, or wrong sales session. The existing terminal model is too thin for this security boundary, and the in-progress `core.terminal/terminal_binding.md` now defines the domain model needed to make terminal trust explicit.

## What Changes

- Introduce terminal/device trust as a first-class transaction boundary for POS, mobile, and phone sales.
- Model `core.terminal` around four lifecycle-separated aggregates: `Terminal`, `TerminalAssignment`, `TerminalDeviceBinding`, and `TerminalActivationChallenge`.
- Attach operational context during HTTP request context creation, but validate it late per sensitive use case.
- Require signed terminal/device binding, active assignment, active terminal, compatible outlet/session, idempotency, permission, entitlement, and audit for sensitive transactions.
- Resolve the business seller from the authenticated user, trusted outlet, and session before sale persistence.
- Add activation flows for physical POS pairing and virtual phone terminals.
- Integrate phone-sales and terminal quota checks through `platform.entitlement.api`, not by parsing plans in business code.
- Align the OpenSpec change with the near-code domain source of truth: `tchalanet-core/src/main/java/com/tchalanet/server/core/terminal/terminal_binding.md`.

## Capabilities

### New Capabilities

- `auth-context`: Canonical request context, operational context source classification, trusted-context helper, and idempotency key propagation.
- `terminal-security`: Terminal lifecycle, assignment, activation challenge, device binding, capability, and trust validation behavior.
- `transaction-security`: Sensitive transaction enforcement for ticket sales, payout/offline hooks, session compatibility, idempotency, entitlement, and audit.

### Modified Capabilities

- None.

## Impact

- Backend common context: `TchRequestContext`, `TchContextFilter`, operational context records/enums, and request header extraction.
- Backend platform APIs: `platform.accesscontrol`, `platform.audit`, `platform.communication`, `platform.idempotence`, and `platform.entitlement.api`.
- Backend core domains: `core.terminal`, `core.seller`, `core.session`, `core.sales`, payout/offline integration hooks.
- Persistence: terminal assignment, device binding, activation challenge, capabilities, tenant-scoped RLS, audit metadata, and indexes.
- API contracts: POS pairing, virtual phone activation, current operational context, terminal admin endpoints, and sale idempotency.
- Client contracts: Angular admin login/selection and Flutter POS/mobile signed binding headers.
