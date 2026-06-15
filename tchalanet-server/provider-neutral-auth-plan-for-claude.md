# Tchalanet — Provider-Neutral Authentication & Provisioning Plan

## Status

Proposed implementation plan for Claude/Codex.

Goal: make authentication, provisioning, runtime bootstrap, tests, frontend and mobile provider-neutral so Tchalanet can run with:

1. **Keycloak** as authentication provider.
2. **Firebase live** as authentication provider.
3. **Firebase Auth Emulator** for integration/local tests.
4. **Local provider / no external provider** for services, local development and performance tests.
5. Later: **Clerk** or another provider without changing business features.

Core rule:

```text
External provider = authentication only.
Tchalanet = AppUser, roles, permissions, tenants, operational context, POS binding, RLS and authorization.
```

---

## 1. Non-negotiable architecture decisions

### 1.1 Provider is not the source of truth

Do not store business authorization in Firebase/Keycloak/Clerk tokens.

The provider token proves only:

```text
issuer
subject / uid
email snapshot
email verification if available
provider metadata
```

Tchalanet owns:

```text
app_user
app_user_external_identity
tenant membership
roles
permissions
outlet assignments
terminal assignments
sales session access
operational context
RLS tenant binding
audit identity
```

### 1.2 OpenAPI must be provider-neutral

Do not expose provider names in public API paths, DTO names or tags unless the endpoint is explicitly internal diagnostics.

Use:

```yaml
securitySchemes:
  bearerAuth:
    type: http
    scheme: bearer
    bearerFormat: JWT
```

Do not use:

```yaml
securitySchemes:
  firebaseAuth: ...
  keycloakAuth: ...
```

Endpoint names must remain Tchalanet-owned:

```text
GET  /api/v1/runtime/private
GET  /api/v1/runtime/pos
GET  /api/v1/tenant/me
POST /api/v1/admin/identity/users
POST /api/v1/admin/identity/users/{userId}/external-identities
POST /api/v1/admin/identity/users/{userId}/roles
POST /api/v1/admin/identity/users/{userId}/disable
```

### 1.3 Frontend should not create provider users directly in production

Allowed:

```text
Frontend -> provider login/logout/token acquisition
```

Not allowed in production:

```text
Frontend -> Firebase createUserWithEmailAndPassword()
Frontend -> Keycloak user creation
Frontend -> Clerk user creation
```

Provisioning must go through backend:

```text
Admin UI -> Tchalanet Identity API -> provider adapter -> AppUser mapping -> roles/tenant
```

Reason: avoid provider users with no `AppUser`, no tenant, no role and no `app_user_external_identity` mapping.

### 1.4 Runtime after login

After provider login, frontend must call Tchalanet runtime before navigating to private dashboards.

```text
Web login -> provider ID token -> GET /runtime/private -> defaultRoute
POS login -> POS credential/provider token -> GET /runtime/pos -> defaultRoute
```

Firebase/Keycloak must not decide private routing. Tchalanet decides based on internal AppUser and roles.

---

## 2. Target backend design

### 2.1 Package boundaries

Use provider-neutral APIs under `platform.identity.api` and hide provider implementation under `platform.identity.internal`.

```text
platform.identity.api
  IdentityProviderApi
  IdentityProvisioningApi
  ExternalAuthenticatedUser
  ExternalIdentityRef
  ExternalDirectoryUser
  IdentityVerificationPolicy
  IdentityProviderType
  AppUserView
  CurrentUserView

platform.identity.internal
  service/
    ExternalTokenAuthenticationService
    AppUserIdentityResolver
    AppUserProvisioningService
    ExternalIdentityLinkService
    ExternalIdentitySyncService
  firebase/
    FirebaseIdentityProvider
    FirebaseIdentityProvisioningAdapter
    FirebaseEmulatorIdentityProvider
  keycloak/
    KeycloakIdentityProvider
    KeycloakIdentityProvisioningAdapter
  local/
    LocalJwtIdentityProvider
    LocalIdentityProvisioningAdapter
  web/
    CurrentUserController
    IdentityAdminController
```

No `core` or `features` package may depend on Firebase, Keycloak or Clerk SDKs.

### 2.2 Provider-neutral identity interfaces

Create or align these contracts.

```java
public enum IdentityProviderType {
    FIREBASE,
    FIREBASE_EMULATOR,
    KEYCLOAK,
    LOCAL_JWT,
    CLERK
}
```

```java
public record ExternalIdentityRef(
    IdentityProviderType provider,
    String issuer,
    String externalSubject
) {}
```

```java
public record ExternalAuthenticatedUser(
    ExternalIdentityRef identity,
    String email,
    boolean emailVerified,
    String displayName,
    Map<String, Object> claims
) {}
```

```java
public interface IdentityProviderApi {
    IdentityProviderType providerType();
    ExternalAuthenticatedUser verifyBearerToken(String bearerToken, IdentityVerificationPolicy policy);
}
```

```java
public enum IdentityVerificationPolicy {
    STANDARD,
    SENSITIVE_OPERATION,
    TEST_ONLY
}
```

Provisioning interface:

```java
public interface IdentityProvisioningApi {
    IdentityProviderType providerType();
    ProvisionedExternalUser provisionUser(ProvisionExternalUserRequest request);
    void disableExternalUser(ExternalIdentityRef identity);
    Optional<ExternalDirectoryUser> findByEmail(String email);
    Optional<ExternalDirectoryUser> findBySubject(String externalSubject);
}
```

### 2.3 Database model

Add provider-neutral mapping table.

```sql
create table app_user_external_identity (
    id uuid primary key,
    app_user_id uuid not null references app_user(id),
    provider varchar(64) not null,
    issuer varchar(255) not null,
    external_subject varchar(255) not null,
    email_snapshot varchar(255),
    display_name_snapshot varchar(255),
    linked_at timestamptz not null,
    last_login_at timestamptz,
    status varchar(32) not null,
    unique(provider, issuer, external_subject)
);
```

Keep existing `app_user.keycloak_user_id` temporarily only for migration/backfill. Do not use it in new code.

Migration path:

```text
1. Add app_user_external_identity.
2. Backfill KEYCLOAK rows from app_user.keycloak_user_id.
3. Update resolver to use mapping table.
4. Mark keycloak_user_id deprecated.
5. Remove later after confidence.
```

### 2.4 Authentication filter flow

Current provider is selected by config.

```yaml
tch:
  identity:
    provider: firebase # firebase | firebase-emulator | keycloak | local-jwt
```

Request flow:

```text
Authorization: Bearer <token>
  -> ProviderTokenAuthenticationFilter
  -> IdentityProviderApi.verifyBearerToken(...)
  -> ExternalAuthenticatedUser
  -> AppUserIdentityResolver maps provider/issuer/subject -> AppUser
  -> roles/permissions/tenant loaded from Tchalanet
  -> Authentication principal built
  -> TchRequestContext created/bound
  -> RLS variables set by existing RLS bridge
```

Unknown external identity must be denied by default:

```text
Firebase token valid but no app_user_external_identity mapping -> 403 external_identity.not_linked
```

Do not auto-create AppUser during normal login unless explicitly configured for a controlled local/dev mode.

### 2.5 Provider configs

#### Firebase live

```yaml
tch:
  identity:
    provider: firebase
    firebase:
      project-id: tchalanet-39115
      issuer: https://securetoken.google.com/tchalanet-39115
      revocation-check:
        mode: sensitive-only # off | sensitive-only | always
```

Expected token:

```text
iss = https://securetoken.google.com/tchalanet-39115
aud = tchalanet-39115
sub = Firebase localId/uid
```

#### Firebase emulator

Docker compose service already exists:

```yaml
firebase-emulator:
  build:
    context: ../firebase-emulator
    dockerfile: Dockerfile
  image: tchl/firebase-auth-emulator:${FIREBASE_EMULATOR_IMAGE_TAG:-local}
  container_name: ${DOCKER_PREFIX:-tchl}-firebase-auth-emulator-${ENV:-dev}
  environment:
    CI: "true"
    FIREBASE_CLI_DISABLE_UPDATE_CHECK: "true"
    GCLOUD_PROJECT: ${FIREBASE_EMULATOR_PROJECT_ID:-demo-tchalanet-local}
  ports:
    - "9099:9099"
  networks:
    back: {}
  restart: unless-stopped
  healthcheck:
    test:
      [
        "CMD-SHELL",
        "node -e \"fetch('http://127.0.0.1:9099/').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))\""
      ]
    interval: 5s
    timeout: 3s
    retries: 20
    start_period: 20s
```

Backend config for emulator:

```yaml
tch:
  identity:
    provider: firebase-emulator
    firebase-emulator:
      project-id: demo-tchalanet-local
      emulator-host: http://firebase-emulator:9099
      issuer: https://securetoken.google.com/demo-tchalanet-local
```

For local host execution outside Docker:

```yaml
tch:
  identity:
    provider: firebase-emulator
    firebase-emulator:
      project-id: demo-tchalanet-local
      emulator-host: http://localhost:9099
```

Important: emulator mode must be forbidden in prod profile.

#### Keycloak authentication-only

```yaml
tch:
  identity:
    provider: keycloak
    keycloak:
      issuer-uri: https://auth.tchalanet.com/realms/tchalanet
      jwk-set-uri: https://auth.tchalanet.com/realms/tchalanet/protocol/openid-connect/certs
```

Use Keycloak only to verify token and get external subject.

Do not read Keycloak roles as Tchalanet roles in new code.

#### Local JWT / no external provider

```yaml
tch:
  identity:
    provider: local-jwt
    local-jwt:
      issuer: tchalanet-local
      secret: dev-only-secret
      allow-auto-link: true # dev/test only
```

Rules:

```text
Allowed in local/dev/test/perf only.
Forbidden in production.
Used for services, integration tests without provider, performance tests.
```

---

## 3. Provisioning strategy

### 3.1 User creation must be Tchalanet-first

Target API:

```http
POST /api/v1/admin/identity/users
```

Request:

```json
{
  "email": "cashier@test.com",
  "displayName": "Cashier Test",
  "tenantId": "tenant-id",
  "roles": ["CASHIER"],
  "provisioning": {
    "mode": "INVITE",
    "provider": "CURRENT"
  }
}
```

`CURRENT` means backend selects configured provider.

Backend flow:

```text
1. Create AppUser with status PROVISIONING.
2. Assign tenant membership and roles in Tchalanet.
3. Provision external provider user via IdentityProvisioningApi.
4. Insert app_user_external_identity(provider, issuer, externalSubject).
5. Mark AppUser ACTIVE.
6. Audit success.
```

Failure handling:

```text
If provider creation fails:
  AppUser remains PROVISIONING_FAILED or DRAFT.
  No login possible.
  Admin can retry.

If provider succeeds but mapping fails:
  Disable/delete provider user if adapter supports compensation.
  Mark AppUser PROVISIONING_FAILED.
  Audit with correlation id.
```

### 3.2 Manual link mode for tests

Add admin endpoint:

```http
POST /api/v1/admin/identity/users/{userId}/external-identities
```

Request:

```json
{
  "provider": "FIREBASE_EMULATOR",
  "issuer": "https://securetoken.google.com/demo-tchalanet-local",
  "externalSubject": "firebase-uid",
  "emailSnapshot": "test@test.com"
}
```

Useful when the test creates emulator user first, then links to an existing AppUser.

### 3.3 Sync is reconciliation, not source of truth

Sync provider users must not blindly create active AppUsers.

Allowed sync behavior:

```text
- detect provider users without AppUser mapping
- detect AppUsers with missing provider user
- update email/displayName snapshots
- report anomalies
- optionally create DRAFT candidates, not ACTIVE users
```

Endpoint:

```http
POST /api/v1/admin/identity/external-sync-runs
```

Result:

```json
{
  "runId": "...",
  "provider": "FIREBASE",
  "createdCandidates": 2,
  "missingMappings": 1,
  "disabledProviderUsers": 0,
  "anomalies": []
}
```

---

## 4. Runtime and frontend contract

### 4.1 Web private runtime

After Firebase/Keycloak login, web calls:

```http
GET /api/v1/runtime/private
Authorization: Bearer <provider-id-token>
```

Response:

```json
{
  "user": {
    "userId": "u_123",
    "displayName": "Admin Tenant",
    "email": "admin@test.com",
    "roles": ["TENANT_ADMIN"],
    "tenantId": "t_123",
    "tenantCode": "demo"
  },
  "defaultRoute": "/app/admin",
  "navigation": [],
  "shell": {},
  "permissions": []
}
```

Frontend flow:

```text
login provider
  -> get provider token
  -> GET /runtime/private
  -> store session from runtime
  -> navigate to defaultRoute
```

Do not navigate based on provider token claims.

### 4.2 POS runtime

POS is separate because it needs operational context.

```http
GET /api/v1/runtime/pos
Authorization: Bearer <pos-token-or-provider-token>
X-Tch-Terminal-Id: term_123
```

Response:

```json
{
  "user": {
    "userId": "u_cashier",
    "displayName": "Cashier 1",
    "roles": ["CASHIER"]
  },
  "operationalContext": {
    "tenantId": "t_123",
    "outletId": "out_123",
    "terminalId": "term_123",
    "salesSessionId": "sess_123",
    "trusted": true
  },
  "pos": {
    "deviceBindingStatus": "ACTIVE",
    "offlineMode": "AVAILABLE",
    "syncStatus": "OK",
    "printProfiles": ["RECEIPT_58MM", "RECEIPT_80MM"]
  },
  "defaultRoute": "/pos/sell"
}
```

### 4.3 POS authentication modes

Separate web auth from POS credentials.

Recommended V0:

```text
Web:
  Firebase email/password or Keycloak login
  -> /runtime/private

POS:
  phone + PIN credential owned by Tchalanet
  + terminal activation/binding
  -> /runtime/pos
```

Phone + PIN is not valid for admin web.

Credential types:

```text
EXTERNAL_IDENTITY  # Firebase, Keycloak, Clerk
POS_PIN            # Tchalanet credential, POS-only
DEVICE_BINDING     # terminal proof
ADMIN_IMPERSONATION
```

A POS user must not be able to sell unless all are valid:

```text
actor authenticated
AppUser active
role/permission allows sell
outlet assignment valid
terminal active/bound
sales session open
operational context trusted
device proof valid for sensitive actions
```

---

## 5. Frontend implementation plan

### 5.1 Split auth adapters from session/runtime

Do not keep one Keycloak-specific `AuthSessionService`.

Target structure:

```text
auth/
  auth-client.ts
  firebase-auth-client.ts
  keycloak-auth-client.ts
  local-auth-client.ts
  auth-provider.config.ts

session/
  web-auth-session.service.ts
  pos-auth-session.service.ts

runtime/
  runtime-client.ts
  private-runtime.model.ts
  pos-runtime.model.ts

identity-admin/
  identity-admin-client.ts
  identity-admin.models.ts
```

Provider-neutral interface:

```ts
export interface AuthClient {
  login(email: string, password: string): Promise<void>;
  logout(): Promise<void>;
  getIdToken(forceRefresh?: boolean): Promise<string | null>;
}
```

Firebase implementation uses AngularFire.

Keycloak implementation uses keycloak-js later, but only through `AuthClient`.

### 5.2 Web login flow

```ts
await authClient.login(email, password);
const runtime = await runtimeClient.loadPrivateRuntime();
sessionStore.setFromRuntime(runtime);
await router.navigateByUrl(runtime.defaultRoute ?? '/app');
```

### 5.3 Runtime client

```ts
@Injectable({ providedIn: 'root' })
export class RuntimeClient {
  private readonly backend = inject(TchBackendClient);

  loadPrivateRuntime(): Observable<PrivateRuntimeView> {
    return this.backend.get<PrivateRuntimeView>('/runtime/private', {
      suppressShellFeedback: true,
    });
  }

  loadPosRuntime(terminalId: string): Observable<PosRuntimeView> {
    return this.backend.get<PosRuntimeView>('/runtime/pos', {
      headers: { 'X-Tch-Terminal-Id': terminalId },
      suppressShellFeedback: true,
    });
  }
}
```

### 5.4 Identity admin client

No Firebase Admin SDK in frontend. No provider-specific user creation.

```ts
@Injectable({ providedIn: 'root' })
export class IdentityAdminClient {
  private readonly backend = inject(TchBackendClient);

  createUser(request: CreateAppUserRequest): Observable<AppUserView> {
    return this.backend.post<AppUserView>('/admin/identity/users', request);
  }

  linkExternalIdentity(userId: string, request: LinkExternalIdentityRequest): Observable<AppUserView> {
    return this.backend.post<AppUserView>(`/admin/identity/users/${userId}/external-identities`, request);
  }
}
```

### 5.5 Firebase emulator frontend config

Local frontend can use emulator.

```ts
import { connectAuthEmulator, getAuth } from '@angular/fire/auth';

provideAuth(() => {
  const auth = getAuth();
  if (environment.firebase.useEmulator) {
    connectAuthEmulator(auth, environment.firebase.emulatorUrl, { disableWarnings: true });
  }
  return auth;
});
```

Environment example:

```ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  authProvider: 'firebase-emulator',
  firebase: {
    useEmulator: true,
    emulatorUrl: 'http://localhost:9099',
    apiKey: 'demo-api-key',
    authDomain: 'demo-tchalanet-local.firebaseapp.com',
    projectId: 'demo-tchalanet-local',
    appId: 'demo-app-id',
  },
};
```

Production must use Firebase live or another live provider.

---

## 6. Mobile/POS implementation plan

### 6.1 Mobile auth split

Do not reuse web-only auth assumptions.

Target mobile services:

```text
AuthClient
  FirebaseMobileAuthClient
  PosPinAuthClient
  LocalTestAuthClient

PosSessionService
RuntimeClient
DeviceBindingService
OfflineSyncService
```

### 6.2 POS login with phone + PIN

Endpoint:

```http
POST /api/v1/pos/auth/login
```

Request:

```json
{
  "phone": "+15145550123",
  "pin": "1234",
  "terminalId": "term_123",
  "deviceId": "device_abc"
}
```

Response:

```json
{
  "accessToken": "short-lived-pos-token",
  "expiresInSeconds": 3600,
  "requiresDeviceBinding": false,
  "defaultRuntimePath": "/api/v1/runtime/pos"
}
```

The token must include channel/audience constraints:

```text
audience = tchalanet-pos
channel = POS
actorId = app_user_id
short TTL
not valid for admin web
```

### 6.3 POS actions still require operational proof

Selling/payout/offline grant/offline sync must not rely only on phone + PIN.

Required:

```text
POS auth token or provider token
device binding active
request signature / device proof for sensitive ops
trusted operational context
```

---

## 7. Test matrix

### 7.1 Backend test profiles

Create/standardize profiles:

```text
local-jwt-test
firebase-emulator-test
firebase-live-dev
keycloak-dev
```

### 7.2 Test cases

#### Local provider/no external provider

```text
Given local-jwt provider enabled
When token subject maps to AppUser
Then /runtime/private returns roles/defaultRoute
And RLS tenant context is bound
```

#### Firebase emulator

```text
Given firebase-auth-emulator running on 9099
And emulator user exists
And app_user_external_identity maps emulator uid to AppUser
When frontend/backend calls /runtime/private with emulator ID token
Then backend resolves AppUser and returns runtime
```

#### Firebase live

```text
Given Firebase live token for project tchalanet-39115
And mapping exists
When /runtime/private is called
Then runtime returns Tchalanet roles from DB, not Firebase custom claims
```

#### Keycloak authentication-only

```text
Given Keycloak token is valid
And KEYCLOAK external identity mapping exists
When /runtime/private is called
Then Tchalanet roles are loaded from DB
And Keycloak realm/resource roles are ignored for authorization
```

#### Unknown external identity

```text
Given provider token is valid
But no app_user_external_identity mapping exists
When /runtime/private is called
Then response is 403 external_identity.not_linked
```

#### Provider mismatch

```text
Given backend configured for firebase-emulator
When live Firebase token is used
Then response is 401 invalid_issuer_or_audience
```

#### POS runtime

```text
Given POS user authenticates with phone + PIN
And terminal binding is active
And outlet/session assignment is valid
When /runtime/pos is called
Then trusted operationalContext is returned
```

#### POS blocked without binding

```text
Given POS phone + PIN is valid
But terminal is not bound
When sell is attempted
Then request is rejected before sale creation
```

### 7.3 Docker compose validation

For emulator stack:

```bash
docker compose up -d firebase-emulator
curl http://localhost:9099/
```

Expected: HTTP 200/OK from emulator health endpoint.

Backend must be able to reach emulator by Docker network host:

```text
http://firebase-emulator:9099
```

Frontend local browser must use:

```text
http://localhost:9099
```

---

## 8. Implementation sequence for Claude

### Phase 1 — Inventory and guardrails

1. Search backend for direct references to Firebase, Keycloak, Clerk, `keycloak_user_id`, `realmAccess`, `resourceAccess`.
2. Search frontend for `Keycloak`, `keycloak-js`, provider-specific role parsing.
3. List all paths/DTOs/OpenAPI tags with provider names.
4. Add rule: business modules must not import provider SDKs.

Acceptance:

```text
Inventory document created.
Provider leaks identified.
No behavior changed yet.
```

### Phase 2 — Backend identity abstraction

1. Create `platform.identity.api` contracts.
2. Implement provider resolver by config.
3. Wrap current Keycloak behavior behind `KeycloakIdentityProvider`.
4. Add Firebase live provider.
5. Add Firebase emulator provider.
6. Add LocalJwt provider for local/test only.

Acceptance:

```text
Same security filter works with keycloak/firebase/firebase-emulator/local-jwt.
No controller knows provider type.
```

### Phase 3 — External identity mapping

1. Add `app_user_external_identity` migration.
2. Backfill Keycloak mappings from existing `app_user.keycloak_user_id`.
3. Implement `AppUserIdentityResolver` by `(provider, issuer, externalSubject)`.
4. Unknown mapping returns 403.

Acceptance:

```text
Provider token valid but no mapping -> 403.
Mapping exists -> AppUser resolved.
Roles loaded from Tchalanet DB.
```

### Phase 4 — Runtime endpoints

1. Implement/align `GET /runtime/private`.
2. Implement/align `GET /runtime/pos`.
3. Keep `/tenant/me` as lightweight current user endpoint if useful.
4. Runtime returns `defaultRoute`, roles, permissions, shell/navigation.

Acceptance:

```text
Frontend can login and redirect without reading provider roles.
POS can load operational runtime separately.
```

### Phase 5 — Provisioning API

1. Implement provider-neutral admin identity endpoints.
2. Add `IdentityProvisioningApi` adapters.
3. Add `CURRENT` provider mode.
4. Add compensation/retry status for partial failures.
5. Disable direct provider user creation from frontend production code.

Acceptance:

```text
Admin creates AppUser through Tchalanet.
Backend provisions provider user and mapping.
No orphan provider user is considered active.
```

### Phase 6 — Frontend provider-neutral refactor

1. Replace Keycloak-specific `AuthSessionService` with `AuthClient` + `WebAuthSessionService`.
2. Firebase implementation uses AngularFire.
3. Keycloak implementation remains optional adapter.
4. Login calls provider then `/runtime/private`.
5. Role/session state is built from runtime response, not token claims.
6. Add Firebase emulator config support.

Acceptance:

```text
Web works with Firebase live.
Web works with Firebase emulator locally.
No UI reads roles from provider token.
```

### Phase 7 — Mobile/POS split

1. Add/align POS auth client for phone + PIN.
2. Add POS runtime client.
3. Ensure sell/payout/offline actions require trusted operational context and device proof.
4. Do not reuse web admin auth rules for POS.

Acceptance:

```text
POS login is channel-specific.
POS runtime returns terminal/outlet/session state.
Phone + PIN cannot access admin web.
```

### Phase 8 — E2E matrix

Implement E2E tests for:

```text
local-jwt
firebase-emulator
firebase-live-dev manual smoke
keycloak-dev manual smoke
unknown mapping
POS runtime
POS blocked without binding
```

Acceptance:

```text
Provider can be switched by config without changing controllers, DTOs, frontend session logic or mobile runtime logic.
```

---

## 9. Definition of Done

Provider-neutral auth/provisioning is done when:

```text
- Backend can verify Keycloak, Firebase live, Firebase emulator and LocalJwt via the same IdentityProviderApi.
- Roles/permissions are always loaded from Tchalanet DB.
- app_user_external_identity is the only provider mapping used by new code.
- OpenAPI is bearerAuth/provider-neutral.
- Frontend login does provider auth then /runtime/private.
- Frontend does not create provider users directly in production.
- Admin user CRUD goes through /admin/identity/users.
- POS uses separate /runtime/pos and channel-specific credentials.
- Firebase emulator is supported for integration/local tests.
- LocalJwt/no-provider mode is available for service/perf tests and forbidden in prod.
- Adding Clerk later means adding a Clerk adapter, not rewriting business modules or frontend runtime flow.
```

---

## 10. Final instruction for Claude

Implement provider-neutral identity without leaking Firebase, Keycloak or Clerk into business modules, controllers, OpenAPI public names, frontend session state or mobile runtime logic.

The only provider-specific code must live behind adapters.

Tchalanet must remain the source of truth for AppUser, roles, permissions, tenant membership, POS operational context, RLS and audit.
