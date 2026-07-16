# Proposal: Admin Passkey Login V1

## Summary

Add passkeys as an optional authentication method for `APP_USER` actors while preserving Firebase as
the issuer of the final ID token consumed by Tchalanet.

Passkeys improve mobile login ergonomics and phishing resistance. They do not replace Tchalanet
authorization, tenant resolution, or account-status checks.

## Current Decision

Implementation is intentionally deferred.

The current V0 remains username/email + password, browser password-manager support, password reset,
and mobile PWA installation guidance. This gives a usable mobile login path on iOS and Android
without introducing WebAuthn operational risk before the relying-party, Firebase adapter, recovery,
audit, and rate-limit decisions are closed.

This change records the desired passkey contract for a later implementation slice; it does not add
backend endpoints, credential storage, Firebase custom-token exchange, or web passkey ceremonies.

## Why

Tenant admins and platform admins primarily use phones. Username login and PWA installation reduce
friction, but repeated password entry remains inconvenient.

Passkeys allow the browser or authenticator to verify the user through a supported local mechanism
such as biometrics, device PIN, pattern, or a security key. Tchalanet never receives or stores that
local secret.

## Scope

- `APP_USER` only.
- Optional passkey enrollment from an authenticated private session.
- Optional passkey login from admin/platform login surfaces.
- Multiple registered WebAuthn credentials per `APP_USER`.
- Password login and recovery remain available in V1.
- Final login still produces a Firebase ID token and enters the existing Tchalanet access-context
  pipeline.
- Successful login continues to load `/runtime/private`.

## Identity Contract

- A WebAuthn credential belongs to an existing `APP_USER`.
- The credential is linked to the `APP_USER`'s existing Firebase identity.
- Passkey login must not create a second Firebase user or external identity.
- Firebase remains the final token issuer in V1.
- Tchalanet DB remains the source of truth for actor status, tenant, roles, and permissions.
- SellerTerminal authentication remains unchanged.

## Provider Strategy

At implementation time, choose one documented adapter.

### Native Firebase Passkey Adapter

Use only if Firebase officially supports production enrollment, authentication, credential
management, and linking to the existing Firebase user.

### Tchalanet WebAuthn Adapter

Tchalanet performs WebAuthn ceremonies and, after successful verification, issues a Firebase custom
token for the existing Firebase UID. The web client exchanges it through Firebase and receives the
normal Firebase ID token.

The application-facing contract must remain independent of the selected adapter.

## Decisions

- A user can register multiple credentials.
- A credential may be synced across devices or bound to a specific authenticator; Tchalanet must not
  assume that one credential equals one physical device.
- Enrollment requires a recently authenticated `APP_USER` session. A long-lived restored session
  alone is insufficient.
- `userVerification` is required.
- V1 uses `attestation = none`.
- Discoverable credentials are preferred for "Continue with a passkey"; username/email + password
  remains available.
- The RP ID and allowed origins must be chosen before implementation and treated as server
  configuration, not client input.
- Password fallback remains in V1; high-assurance policies are future work.

## RP ID And Origins To Decide Before Implementation

Credentials are bound to the Relying Party and allowed origins. The implementation plan must define:

- production RP ID;
- production origins;
- staging RP ID and origins;
- local development behavior;
- Cloudflare Pages preview behavior;
- whether all admin/platform surfaces share a common domain or use separate domains.

A stable common domain should be preferred when admins need the same credential across Tchalanet
surfaces. Preview domains such as `*.pages.dev` should not be treated as equivalent to production
credentials.

## Recovery And Support

Password reset recovers account access, but it is not by itself a complete passkey recovery policy.
Before implementation, decide whether recovery revokes all passkeys or preserves them after a
stronger reauthentication step.

Support may list and revoke credentials through authorized and audited flows. Support must never
create a passkey for a user or bypass the user's presence during enrollment.

## Non-Goals

- Implementation in this PR.
- Passkeys for SellerTerminal POS.
- Replacing Firebase ID tokens.
- Removing password login or recovery in V1.
- Mandatory hardware attestation.
- Treating a passkey as proof of one specific physical device.
