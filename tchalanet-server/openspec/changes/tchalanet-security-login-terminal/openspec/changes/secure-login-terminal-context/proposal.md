# OpenSpec Proposal — secure-login-terminal-context

## Summary

Implement a secure login and transaction context model for Web, Mobile and POS surfaces. The change introduces terminal/device binding, virtual phone terminals, operational context validation, plan/entitlement checks for phone sales, idempotency enforcement for sales, and a complete test plan.

## Problem

Tchalanet wants to offer a strong differentiator: tenants can sell from a phone or physical POS. Without terminal/device validation, a valid user token could be misused from an untrusted device, wrong outlet, wrong session or wrong terminal.

## Goals

- Support secure Angular Web admin login.
- Support secure Flutter POS login with device binding.
- Support secure Flutter phone sales with virtual terminal binding.
- Require trusted operational context for sensitive operations.
- Enforce plan/entitlement checks for phone sales.
- Enforce idempotency for ticket sales.
- Provide audit trail for sensitive actions.
- Provide test coverage for multi-tenant, multi-user, concurrency and retry cases.

## Non-goals

- Implement a custom authentication server.
- Replace Keycloak.
- Store biometric data server-side.
- Use SMS OTP on every login.
- Implement full offline sync in this change, except enforcing hooks and contracts.

## Architectural placement

- `common.context`: primitives only, existing `TchRequestContext` and operational context helpers.
- `platform.accesscontrol`: permission evaluation API and implementation.
- `platform.communication`: OTP/email/SMS delivery.
- `platform.audit`: audit trail.
- `platform.idempotence`: persistent idempotency records.
- `core.terminal`: terminal, assignment, activation, binding, context validation.
- `core.session`: session validation.
- `core.sales`: final sale transaction and idempotent ticket creation.
- `platform.entitlement`: effective plan capabilities consumed by terminal/sales.

## Security posture

A login identifies a user. A trusted terminal binding authorizes an operational surface. A valid session permits a transaction.
