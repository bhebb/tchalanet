# Provider-Neutral Authentication Backend Inventory

## Summary

The backend is substantially ahead of the proposed plan. Authentication verification and durable
external identity mapping are already provider-neutral. The main remaining backend work is
provider-neutral provisioning, public contract cleanup, stable error contracts, and runtime naming.

## Implemented

| Capability | Current implementation |
|---|---|
| Provider-neutral verification API | `platform.identity.api.IdentityProviderApi` |
| External authenticated identity | `ExternalAuthenticatedUser` |
| Config-selected providers | Firebase, Firebase Emulator, Keycloak, local JWT, local perf |
| Provider adapters | `platform.identity.internal.{firebase,keycloak,local}` |
| Production guards | Firebase Emulator and local providers fail in production |
| Durable mapping | `app_user_external_identity` with provider, issuer, and external subject |
| Unknown identity default | Bootstrap mode defaults to `deny` |
| Tchalanet-owned authorization | Request context replaces token role hints with DB roles/permissions |
| Provider SDK boundary | `PlatformLayerGatesTest.providerSdksAreConfinedToIdentityAdapters` |
| Admin user API | `POST /admin/identity/users` |
| Firebase compensation | Created Firebase user is deleted after local transaction rollback |

## Provider Leaks

| Leak | Location | Action |
|---|---|---|
| OpenAPI bearer format and description say Firebase | `OpenApiConfig` | Fix in Phase 1 |
| Admin response exposes `keycloakSub` and `keycloakSyncStatus` | `TenantUserAdminResponse` | Fix in Phase 1 |
| Providers without managed provisioning need adapters | `UnsupportedIdentityProvisioningService` | Add adapter when required |
| Public/current-user models still use `KeycloakUserSub` | identity API and web models | Transitional cleanup |
| Bootstrap endpoint docs mention Keycloak | `CurrentUserProfileController` | Transitional cleanup |
| Unknown mapping uses generic `"User not provisioned"` response | `UserBootstrapFilterImpl` | Add stable problem code |

## Runtime Gaps

- Private runtime is canonically exposed as `/runtime/private`; `/tenant/runtime/bootstrap` remains
  as a compatibility alias and delegates to the same service.
- No dedicated `/runtime/pos` endpoint was found in the runtime feature. POS runtime belongs to the
  active terminal security work because it requires trusted terminal/outlet/session operational
  context, not only provider-neutral authentication.

## Provisioning Gap

`TenantUserAdministrationService` now depends on `IdentityProvisioningApi`. Firebase and Firebase
Emulator use `FirebaseUserProvisionService` behind that port and preserve rollback compensation.
Providers without a managed provisioning adapter fail explicitly through
`UnsupportedIdentityProvisioningService`; adding Keycloak or Clerk provisioning no longer requires
changing tenant user administration.

## Validation Notes

- Focused OpenAPI test passed.
- Focused identity admin, provisioning, runtime, and filter tests passed: 31 tests.
- Provider matrix passed: 63 tests covering Firebase live/emulator, Keycloak, local identity,
  production guards, resolution, and unknown mappings.
- Provider SDK confinement architecture gate passed in isolation.
- `./mvnw verify` was attempted and all identity tests passed; the reactor stopped in
  `tchalanet-platform` on the unrelated pre-existing
  `TenantConfigValidatorTest.communicationBlankCurrencyIsRejected` failure.
- The complete `PlatformLayerGatesTest` remains red on pre-existing unrelated violations:
  one `app -> features` dependency and 41 `features -> core.internal` dependencies.
