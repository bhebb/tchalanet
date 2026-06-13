# Design: Firebase cutover and Keycloak cleanup V1

## Authentication modes

| Environment | Standard provider | Notes |
|---|---|---|
| Production | `firebase` | Local and unsupported providers forbidden |
| Staging | `firebase` | Used for Firebase integration validation |
| Local IDE functional | `firebase-emulator` | Default local web/mobile/Firebase integration mode |
| Local IDE real Firebase | `firebase` | Explicit opt-in against the Firebase staging project |
| E2E/performance | `local-jwt` | Exercises normal authorization, context, and RLS |

Provider selection must fail closed. Production must reject `local-jwt`,
`local-perf`, `firebase-emulator`, and any unsupported provider.

## Local Firebase modes

Local IDE supports two explicit Firebase modes:

### Firebase Auth Emulator

- starts the Authentication Emulator from local infrastructure;
- uses a dedicated `demo-` prefixed local project ID;
- requires no Firebase service-account credential;
- supports deterministic users and phone/SMS codes without sending real SMS;
- is used for web, mobile, onboarding, and backend integration tests;
- is not used to measure Firebase production performance.

The emulator issues unsigned ID tokens. The backend accepts them only when
`firebase-emulator` is explicitly configured in a non-production profile and
the configured emulator host and project ID match the expected local values.

### Real Firebase staging

- does not start or connect to the local Authentication Emulator;
- uses the Firebase staging project configuration;
- verifies normally signed Firebase ID tokens;
- obtains server credentials through the approved local secret mechanism;
- is used to validate real Firebase behavior, latency, revocation, and provider
  integration before production.

Switching modes must require one explicit configuration value, for example:

```text
TCH_IDENTITY_PROVIDER=firebase-emulator
TCH_IDENTITY_PROVIDER=firebase
```

Provider-specific subordinate settings may differ, but must not require code
changes.

## Runtime flow

```text
Firebase ID token
  -> FirebaseIdentityProvider verifies token
  -> external identity mapping resolves AppUser
  -> Tchalanet resolves memberships, roles, permissions, and operational context
  -> TchRequestContext is created
  -> transaction-local RLS variables are bound
  -> PostgreSQL RLS enforces isolation
```

Firebase claims are identity evidence only. Tenant, role, permission, outlet,
terminal, seller session, and operational context remain server-owned.

## User provisioning and linking

The target onboarding model is admin-provisioned AppUsers:

1. An administrator creates an AppUser and memberships in Tchalanet.
2. The user authenticates with Firebase using an approved sign-in method.
3. On first successful login, Tchalanet links the verified Firebase subject to
   the pre-provisioned AppUser using an explicit linking policy.
4. Subsequent logins resolve through the durable external identity mapping.

The linking policy must support users without email. Phone-based users must be
linked using a verified phone number or an explicit administrator-issued link.
Email or phone matching must never silently merge two existing AppUsers.

Automatic creation of a new AppUser remains policy-controlled and audited.
Keycloak Admin API provisioning, realm-role synchronization, synchronization
endpoints, listeners, background jobs, adapters, and mappings are deleted.

## Provider-neutral boundary

The identity boundary remains provider-neutral. Firebase and local-jwt are the
only implemented providers for the current delivery.

No Keycloak compatibility code is retained. If Keycloak or another provider is
needed later, it must be reintroduced as a new reviewed change using Git history
only as reference.

## Server configuration

Provider-specific configuration is conditional:

- Firebase configuration is loaded only for `firebase`.
- Firebase Auth Emulator configuration is loaded only for
  `firebase-emulator`.
- Local JWT configuration is loaded only for `local-jwt`.

The standard Spring Resource Server configuration must not contain a Keycloak
issuer URI, Keycloak JWKS URI, realm, audience, or Keycloak decoder. Firebase and
local-jwt use their provider-specific verification adapters. OpenAPI exposes a
bearer-token security scheme; developers paste a Firebase or local JWT rather
than using a Keycloak OAuth authorization-code flow.

Firebase Admin credentials are injected using environment or secret mounts.
There is no absolute workstation path or bundled credential fallback.
Emulator mode must not load Firebase Admin private credentials.

## Web token handling

The Firebase interceptor attaches a bearer token only when the request URL
targets the configured Tchalanet API. It must not attach tokens to assets,
Firebase endpoints, telemetry exporters, or third-party services.

In local builds, the web application connects to the Authentication Emulator
only when emulator mode is explicitly enabled. Real-Firebase mode uses the
staging Firebase configuration without code changes.

The session layer:

- waits for Firebase auth initialization before resolving the current session;
- refreshes tokens before expiry or once after an authentication challenge;
- clears local session state on logout;
- maps backend 401 and 403 responses consistently;
- does not infer Tchalanet authorization from Firebase claims.

Firebase web configuration is public client configuration, but remains
environment-specific. Firebase service-account credentials are never present in
the web application.

## Mobile token handling

The Flutter mobile application authenticates through Firebase and stores session
material using platform-appropriate secure storage. It attaches a current
Firebase ID token only to configured Tchalanet API requests.

Local mobile builds connect to the Authentication Emulator only when emulator
mode is explicitly enabled. Android emulator, iOS simulator, physical-device,
and container host addressing differences are documented.

Mobile authorization state comes from the backend profile/context response.
Firebase claims do not define roles, tenant membership, outlet assignment,
terminal assignment, or operational context. Offline submissions remain pending
until accepted by Tchalanet.

The mobile session layer handles auth initialization, token refresh, logout,
revoked or disabled users, and backend 401/403 responses consistently with the
web client.

## Published documentation

The MkDocs portal links to the canonical component documentation for:

- Firebase production/staging configuration;
- local-jwt development and E2E configuration;
- admin-provisioned user onboarding and first-login linking;
- phone-only user handling;
- Firebase secret management;
- web and mobile troubleshooting.

Canonical implementation details are updated in `tchalanet-server/docs`.
The `tchalanet-docs` MkDocs portal links to those component documents where
appropriate. Keycloak is not presented as a supported runtime or migration
option.

## Infrastructure topology

Standard compose and Make targets contain:

- Traefik or equivalent reverse proxy;
- API;
- PostgreSQL;
- Redis;
- observability services where enabled.

They do not contain or depend on Keycloak.

Local infrastructure provides a Firebase Authentication Emulator service and
explicit commands/configuration for:

- starting local dependencies with Firebase Auth Emulator;
- running the API in local IDE against the emulator;
- running the API in local IDE against the real Firebase staging instance.

## Security controls

- Remove and rotate any real service-account credential found in the workspace.
- Add ignore rules preventing Firebase Admin credential files from being added.
- Reject unsafe identity providers in production at startup.
- Reject Firebase Auth Emulator host/configuration in production at startup.
- Keep authorization and RLS tests provider-independent.
- Audit external identity linking, bootstrap, unlinking, and SUPER_ADMIN use.

## Future providers

Future providers must implement the provider-neutral identity boundary and pass
the same AppUser, authorization, context, audit, and RLS tests. Removed Keycloak
code is not kept as dormant compatibility code; Git history may be consulted
when designing a future adapter.
