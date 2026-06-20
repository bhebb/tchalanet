# Tasks: Firebase cutover and Keycloak cleanup V1

## Phase 0 - Safety, inventory, and cutover policy

- [ ] Confirm owners for infra, server, web, mobile, and docs.
- [ ] Inventory every Keycloak runtime dependency, configuration property,
      public type, dependency, compose service, script, and test.
- [ ] Classify each Keycloak reference as executable code, configuration,
      infrastructure, test data, or documentation, then schedule its deletion.
- [ ] Confirm the Firebase onboarding/linking policy for email, phone-only, and
      explicitly linked users.
- [ ] Confirm the dedicated Firebase staging project and local emulator project
      ID.
- [ ] Confirm whether the local Firebase service-account JSON is real; if real,
      rotate it immediately and remove it from the workspace.
- [x] Add ignore rules for Firebase Admin credential files.
- [ ] Confirm that Git history, not dormant runtime code, is the reference for
      any future Keycloak adapter.

### Delivery check

- [x] No Firebase private credential is tracked by Git.
- [ ] The cutover matrix and linking policy are accepted.
- [ ] Every active Keycloak reference is scheduled for deletion.

## Phase 1 - Infrastructure cleanup

- [x] Remove Keycloak from the API compose `depends_on` chain.
- [x] Remove Keycloak issuer, audience, hostname, image, and bootstrap variables
      from standard production and staging configuration.
- [x] Remove Keycloak from standard `up`, `up-all`, staging, production, and
      local IDE targets.
- [x] Configure standard production and staging with
      `TCH_IDENTITY_PROVIDER=firebase`.
- [ ] Add Firebase Authentication Emulator to local infrastructure.
- [ ] Add explicit local-IDE commands/configuration for emulator mode and real
      Firebase staging mode.
- [ ] Seed deterministic emulator users needed by web, mobile, and backend
      integration tests.
- [ ] Configure deterministic local/E2E targets with
      `TCH_IDENTITY_PROVIDER=local-jwt`.
- [ ] Inject Firebase Admin credentials through the approved secret mechanism.
- [x] Delete Keycloak compose services, images, scripts, targets, variables,
      database, and bootstrap setup.
- [x] Update infrastructure operations and troubleshooting documentation.

### Delivery check

- [x] Standard production/staging compose configuration contains no Keycloak
      service or dependency.
- [ ] API boots successfully when no Keycloak host is resolvable.
- [ ] Local/E2E topology boots with `local-jwt` and exercises the API.
- [ ] Local IDE boots against Firebase Auth Emulator without Firebase private
      credentials.
- [ ] Local IDE can switch to real Firebase staging without code changes.
- [x] No Keycloak infra target or service remains.

## Phase 2 - Server runtime cleanup

- [x] Remove shared, dev, and local-IDE hardcoded Keycloak issuer/JWKS settings.
- [x] Remove Keycloak Spring Resource Server issuer URI, JWKS URI, audience,
      decoder, and realm configuration from standard profiles.
- [x] Remove Keycloak bootstrap and admin-client configuration from standard
      profiles.
- [x] Ensure provider-specific decoders and beans load only for their provider.
- [ ] Add explicit non-production `firebase-emulator` token verification mode.
- [ ] Accept unsigned emulator ID tokens only from the configured emulator host
      and expected local project ID.
- [ ] Reject emulator mode and emulator host configuration in production.
- [x] Remove Keycloak as a default value from provider guards and conditions.
- [x] Make production startup reject `keycloak`, `local-jwt`, and `local-perf`.
- [ ] Remove the absolute local Firebase credentials path and bundled secret
      fallback.
- [x] Replace Keycloak-specific OpenAPI OAuth configuration with bearer-token
      authentication.
- [ ] Add a boot test proving Firebase mode requires no Keycloak configuration.
- [ ] Add a boot test proving production provider guards fail closed.
- [ ] Add tests proving emulator tokens are accepted only in emulator mode.
- [ ] Add tests proving emulator tokens are rejected in Firebase and production
      modes.

### Delivery check

- [ ] Server boots in Firebase mode without Keycloak issuer, JWKS, realm, or
      admin-client properties.
- [ ] Swagger/OpenAPI accepts a bearer token without a Keycloak OAuth flow.
- [ ] Production cannot start with a local or unsupported provider.
- [ ] Production cannot start with Firebase Auth Emulator configuration.
- [ ] Firebase credentials are environment- or secret-mount-driven.

## Phase 3 - Server provider-neutral model and provisioning cleanup

- [ ] Replace `KeycloakUserSub` in public APIs, views, and domain models with a
      provider-neutral identity reference or remove it where unnecessary.
- [x] Remove direct Keycloak provisioning from tenant-user administration.
- [x] Remove Keycloak realm-role mirroring and resynchronization from the target
      user-management flow.
- [x] Delete Keycloak synchronization endpoints, listeners, services,
      background jobs, commands, and configuration.
- [x] Implement admin-provisioned AppUser onboarding with Firebase-first
      identity creation and durable linking.
- [ ] Support verified phone-only linking without requiring email.
- [ ] Prevent ambiguous email/phone matches from silently linking users.
- [ ] Audit bootstrap, link, unlink, disabled-user, and SUPER_ADMIN actions.
- [ ] Remove Keycloak external identity mappings from fixtures and active data
      setup.
- [x] Remove Keycloak adapter, admin client, dependencies, and tests.

### Delivery check

- [x] Tenant user creation and role changes do not require Keycloak.
- [ ] Firebase users resolve roles and permissions exclusively from Tchalanet.
- [ ] Phone-only and email-based linking policies are tested.
- [ ] Firebase tenant-user and SUPER_ADMIN flows exercise normal context and RLS.
- [ ] No provider-specific type leaks outside the identity adapter boundary.
- [x] No executable Keycloak synchronization path remains.
- [x] Standard Spring Resource Server configuration contains no Keycloak
      resource, issuer, JWKS, audience, realm, or decoder setting.

## Phase 4 - Web Firebase completion and Keycloak cleanup

- [ ] Remove `keycloak-angular`, `keycloak-js`, Keycloak providers, interceptors,
      runtime helpers, imports, comments, and tests.
- [ ] Restrict the Firebase auth interceptor to configured Tchalanet API URLs.
- [ ] Ensure application session initialization waits for Firebase auth state.
- [ ] Implement safe token refresh and a single retry policy for authentication
      challenges.
- [ ] Implement consistent logout and local session cleanup.
- [ ] Handle invalid, expired, revoked, disabled-user, 401, and 403 cases.
- [ ] Keep backend profile/context as the source of roles and permissions.
- [ ] Verify Firebase environment configuration for local and production builds.
- [ ] Add an explicit local switch between Firebase Auth Emulator and real
      Firebase staging.
- [ ] Add focused unit/integration tests for authentication and session flows.

### Delivery check

- [ ] Web obtains and attaches Firebase ID tokens only to Tchalanet API calls.
- [ ] Web contains no Keycloak runtime dependency or direct assumption.
- [ ] Logout, refresh, 401, and 403 behavior is tested.
- [ ] Web build and focused tests pass.
- [ ] Web authentication works against both emulator and real Firebase staging
      modes without code changes.

## Phase 5 - Mobile Firebase completion

- [ ] Add the supported Flutter Firebase authentication dependencies and
      platform configuration.
- [ ] Implement Firebase authentication and auth-state initialization.
- [ ] Attach Firebase ID tokens only to configured Tchalanet API URLs.
- [ ] Store session material using platform-appropriate secure storage.
- [ ] Implement token refresh, logout, and session cleanup.
- [ ] Handle invalid, expired, revoked, disabled-user, 401, and 403 cases.
- [ ] Load roles, tenant context, and operational context from Tchalanet only.
- [ ] Preserve offline submission semantics; authentication does not mark an
      offline operation as confirmed.
- [ ] Add focused mobile authentication and API-client tests.
- [ ] Document Android/iOS Firebase configuration and permission impact.
- [ ] Add an explicit local switch between Firebase Auth Emulator and real
      Firebase staging.

### Delivery check

- [ ] Mobile obtains and sends Firebase ID tokens only to Tchalanet API calls.
- [ ] Mobile contains no Keycloak runtime dependency or direct assumption.
- [ ] Mobile does not derive authorization or operational context from Firebase.
- [ ] `flutter analyze` and focused `flutter test` pass.
- [ ] Mobile authentication works against both emulator and real Firebase
      staging modes without code changes.

## Phase 6 - Published documentation cleanup

- [ ] Publish the Firebase production/staging configuration guide.
- [ ] Publish Firebase Auth Emulator setup, seeded-user, and troubleshooting
      guidance.
- [ ] Document the single local-IDE switch between emulator and real Firebase
      staging.
- [ ] Publish the local-jwt development/E2E configuration guide.
- [ ] Document admin-provisioned onboarding, first-login linking, and phone-only
      users.
- [ ] Document Firebase Admin secret injection, rotation, and incident response.
- [ ] Document web and mobile token refresh, logout, 401, and 403 behavior.
- [x] Remove Keycloak from standard setup, operations, web, and mobile guides.
- [x] Remove obsolete Keycloak documentation from `tchalanet-server/docs`.
- [x] Add or update canonical Firebase and local-jwt implementation guidance in
      `tchalanet-server/docs`.
- [ ] Update MkDocs navigation and links without duplicating component details.

### Delivery check

- [ ] A new contributor can configure Firebase or local-jwt from published
      documentation.
- [ ] Published standard-runtime documentation contains no Keycloak dependency.
- [ ] `tchalanet-server/docs` contains no current Keycloak setup or operations
      instructions.
- [ ] MkDocs build passes.

## Phase 7 - Complete Keycloak removal

- [ ] Remove all remaining Keycloak executable code and configuration.
- [ ] Remove all remaining Keycloak dependencies and test fixtures.
- [ ] Remove all current Keycloak documentation and scripts.
- [ ] Record only the provider-neutral extension contract for future providers.

### Delivery check

- [ ] Repository scans contain no active Keycloak runtime, test, config, infra,
      or documentation path.
- [ ] Firebase and local-jwt pass the provider-neutral contract tests.

## Phase 8 - Delivery validation

- [ ] Run server identity, access-control, context, architecture, and RLS tests.
- [ ] Run server Firebase tenant-user and SUPER_ADMIN integration tests.
- [ ] Run local-jwt E2E tests through normal permission and RLS paths.
- [ ] Run Firebase Auth Emulator web/mobile/backend integration tests.
- [ ] Run a focused real Firebase staging smoke test.
- [ ] Validate standard infra compose/configuration without Keycloak.
- [ ] Boot the API without any reachable Keycloak service.
- [ ] Run web authentication tests and production build.
- [ ] Run mobile authentication tests and analysis.
- [ ] Build the published documentation portal.
- [ ] Execute a Firebase token smoke test against profile and tenant endpoints.
- [ ] Verify invalid/revoked/disabled Firebase identities fail as designed.
- [ ] Verify production rejects Firebase Auth Emulator configuration.
- [ ] Scan executable runtime/configuration for unexpected Keycloak references.
- [ ] Prove no Keycloak synchronization endpoint, listener, service, or job
      remains.
- [ ] Prove standard Spring Resource Server configuration contains no Keycloak
      issuer/JWKS/audience/realm/decoder resource.
- [ ] Run OpenSpec strict validation.
- [ ] Run `git diff --check`.

### Suggested commands

```bash
./mvnw -pl tchalanet-app -am verify
pnpm nx test tch-portal
pnpm nx build tch-portal --configuration=production
flutter analyze
flutter test
venv/bin/mkdocs build --config-file mkdocs.yml
openspec validate firebase-cutover-keycloak-cleanup-v1 --strict
git diff --check
```

### Final acceptance

- [ ] Firebase authenticates production and staging users.
- [ ] Tchalanet owns authorization, tenant context, operational context, and
      audit.
- [ ] PostgreSQL RLS enforces tenant isolation for Firebase and local-jwt flows.
- [ ] Standard infra, server, web, and mobile runtime no longer requires
      Keycloak.
- [ ] Keycloak synchronization and Keycloak Spring Resource Server
      configuration are removed.
- [ ] Published documentation describes the supported runtime accurately.
- [ ] No authentication secret is committed or leaked to logs or clients.
