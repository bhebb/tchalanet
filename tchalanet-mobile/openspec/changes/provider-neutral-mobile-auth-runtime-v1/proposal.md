# Change: Provider-neutral Mobile Auth Runtime V1

## Why

Mobile authentication was built around OIDC/PKCE and provider-specific JWT claims. This made the
identity provider authoritative for Tchalanet user, tenant, and roles instead of the backend
runtime, while the desired mobile UX requires native Firebase operator authentication.

## What

- Use the official FlutterFire Firebase Auth adapter for operator email/password authentication.
- Build `UserSession` from the canonical authenticated runtime response at `GET /runtime/private`.
- Use provider tokens only for authentication, bearer credentials, refresh, and technical expiry.
- Attach bearer credentials to the canonical private runtime endpoint.
- Align mobile auth documentation with backend-owned application identity.

## Non-goals

- No terminal-ID-as-user authentication; terminal binding remains a separate POS security step.
- No changes to terminal binding or operational context.
- No changes to backend authorization rules.
