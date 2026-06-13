# Identity Provider Coupling Inventory

## Scope

Inventory for the provider-neutral identity V0 migration. This note records current coupling before
the authentication pipeline is changed.

## Direct coupling

| Area | Current dependency | Current responsibility | Decision |
|---|---|---|---|
| `app/config/security/SecurityConfig` | Spring Resource Server `Jwt`, Keycloak `realm_access` and flat `roles` claims | Builds Spring authorities and orders bootstrap/context filters | Wrap claim mapping, then remove provider roles as authorization source |
| `app/config/security/JwtDecoderConfig` | Keycloak-compatible issuer/JWKS through Spring `JwtDecoder` | Verifies issuer, signature, expiry, and configured audience | Keep temporarily; route behind configured provider verification later |
| `app/config/security/InsecureJwtDecoderConfig` | Keycloak issuer/JWKS | Local insecure-profile verification | Keep as transitional local-only adapter; remove from production path |
| `app/config/openapi/OpenApiConfig` | Keycloak OIDC endpoint conventions | Swagger OAuth configuration | Make provider/config driven |
| `platform.identity.internal.web.UserBootstrapFilterImpl` | Provider-neutral `ExternalAuthenticatedUser` | Maps authenticated subject to active `app_user` through durable external identity mapping | Active path consumes neutral identity and denies missing/inactive mappings |
| `platform.identity.internal.service.UserBootstrapFilter` | Duplicate Spring `Jwt`/`keycloak_sub` filter | Legacy bootstrap filter path | Removed; only the explicitly wired `IdentityBootstrapFilter` remains |
| `platform.identity.internal.service.keycloak..` | Keycloak Admin SDK | User provisioning, role mirroring, and bootstrap synchronization | Keep isolated as transitional Keycloak administration adapter |
| `platform.identity.internal.web..` admin/ops models and controllers | Keycloak sync concepts | Exposes Keycloak-specific administration operations/status | Keep temporarily; rename or remove after provider-neutral provisioning exists |
| `platform.identity.api` views/requests | `KeycloakUserSub` | Exposes Keycloak subject in public identity contracts | Deprecate and replace after external identity mapping is available |
| `common.context.auth.AuthContextExtractor` | Spring `Jwt` and provider-shaped claims | Extracts technical authentication/context claims | Preserve technical role, but remove provider-specific business authority assumptions |
| `common.types.id.KeycloakUserSub` | Keycloak-specific typed ID | Transitional provisioning and public API contract | Keep until remaining API naming coupling is removed; it is no longer persisted on `app_user` |
| `app_user_external_identity` | Provider-neutral external subject mapping | Stores Keycloak IDs and future Firebase subjects outside `app_user` | Keycloak ID is `external_subject` where `provider=KEYCLOAK`; `app_user` has no provider identifier |
| application YAML and infra compose/realm files | Keycloak issuer, JWKS, realm, admin client | Runtime authentication and local identity server | Keep for transition; remove Keycloak from required production V0 topology later |
| web/mobile clients | Keycloak/OIDC client assumptions | Obtain and refresh production access tokens | Migrate only after backend supports both paths |

## Risk areas

- Spring Resource Server currently verifies the token before identity bootstrap. The verified token
  is now mapped through `IdentityProviderApi` without a second decode; future providers must keep
  filter ordering and error behavior stable.
- `SecurityConfig` currently derives authorities from Keycloak realm roles. Those roles must stop
  being an authorization source before Firebase becomes active.
- Legacy Keycloak mappings are backfilled with issuer `legacy:keycloak`; the first verified login
  claims that mapping for the actual issuer. Concurrent first-login behavior needs operational
  monitoring.
- The public identity API and `TchRequestContext` still expose Keycloak-named subject fields.
- Keycloak administration is mixed with application-user provisioning and role mirroring.
- Seed data and operational scripts identify application users by `app_user.id`; provider subjects
  are seeded separately in `app_user_external_identity`.

## First migration boundary

The active boundary maps the already verified Keycloak JWT through a provider-neutral API and makes
the bootstrap filter consume `ExternalAuthenticatedUser`. Durable mappings live in
`app_user_external_identity`; legacy Keycloak subjects are backfilled and claimed for their actual
issuer on first verified login. Existing access-control, `TchRequestContext`, and RLS paths remain
unchanged.
