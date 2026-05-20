# Spec — local-domain-routing

## Intent

Use explicit local domains so Web, API, Swagger, Keycloak, and mobile configs share a stable URL model.

## Local desktop domains

```text
auth.tchalanet.lan -> Keycloak
api.tchalanet.lan  -> Spring Boot API
app.tchalanet.lan  -> Angular Web
```

## Production domains

```text
auth.tchalanet.com -> Keycloak
api.tchalanet.com  -> API
app.tchalanet.com  -> Web
```

## Manjaro `/etc/hosts` local mapping

```text
127.0.0.1 auth.tchalanet.lan
127.0.0.1 api.tchalanet.lan
127.0.0.1 app.tchalanet.lan
```

## Mobile caveats

- Android emulator may use `10.0.2.2` for host machine access.
- Physical device cannot use the Manjaro `/etc/hosts` file.
- Physical device must use either LAN IP or local DNS resolving `*.tchalanet.lan` to the Manjaro host.

## Required caller configuration

### Web

```text
API_BASE_URL=http://api.tchalanet.lan/api/v1
KEYCLOAK_URL=http://auth.tchalanet.lan
KEYCLOAK_REALM=tchalanet-local
KEYCLOAK_CLIENT_ID=tchalanet-web
```

### Swagger

```text
Swagger origin: http://api.tchalanet.lan
OAuth client: tchalanet-swagger
Redirect: http://api.tchalanet.lan/swagger-ui/oauth2-redirect.html
```

### Flutter emulator fallback

```text
API_BASE_URL=http://10.0.2.2:8080/api/v1
KEYCLOAK_URL=http://10.0.2.2:8081
```

## Acceptance criteria

- Browser can open `http://auth.tchalanet.lan`.
- Swagger OAuth callback works from `api.tchalanet.lan`.
- Web can call API without CORS errors.
- Flutter has separate env profiles for emulator, LAN IP, and local DNS.
