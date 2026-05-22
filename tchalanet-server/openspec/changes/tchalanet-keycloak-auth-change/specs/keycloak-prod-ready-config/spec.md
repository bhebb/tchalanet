# Spec — keycloak-prod-ready-config

## Intent

Make Keycloak configuration production-ready while keeping local development easy and reproducible.

## Realm files

```text
base-realm.json
  common roles, groups, clients, scopes, mappers, policies
  no demo users

overlays/local.json
  local redirects
  local users
  test-friendly values

overlays/prod.json
  production redirects only
  no demo users
  hardened security settings
```

## Clients

### `tchalanet-web`

- Public client.
- Authorization Code + PKCE.
- Strict redirect URIs per environment.

### `tchalanet-mobile-pos`

- Public client.
- Authorization Code + PKCE.
- Mobile redirect scheme.
- Optional `offline_access`, controlled by policy.

### `tchalanet-swagger`

- Public client.
- Authorization Code + PKCE.
- Local/dev by default.
- If enabled in prod, must be restricted and audited.

### `tchalanet-api`

- Resource server audience.
- No direct user-facing login.

## Realm security

Prod-ready baseline:

```text
HTTPS-only external access
strict hostname
proxy headers configured
brute force protection
password policy
email verification
reset password flow
admin MFA readiness
strict redirect URIs
short access token TTL
controlled refresh/offline token TTL
SMTP configured outside local
```

## Keycloak container/proxy

Local:

```text
KC_HOSTNAME=auth.tchalanet.lan
KC_HTTP_ENABLED=true
KC_PROXY_HEADERS=xforwarded
```

Prod:

```text
KC_HOSTNAME=auth.tchalanet.com
KC_HTTP_ENABLED=false unless TLS is terminated upstream by approved config
KC_PROXY_HEADERS=xforwarded
```

## Provider maison

The custom provider is limited to token enrichment / custom claim mapping. User provisioning is handled by the backend through Keycloak Admin API adapters, not by the provider.

## Acceptance criteria

- Realm local imports without manual UI steps.
- Provider loads at Keycloak startup.
- Web, mobile, and Swagger clients can authenticate using PKCE.
- Prod overlay contains no demo users.
- Redirect URIs are environment-specific and strict.
