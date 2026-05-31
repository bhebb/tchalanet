# LAN Setup — Manjaro host + Mac client

Goal: run Tchalanet on a Manjaro computer and access it from a Mac on the same
local network through names such as:

- `https://api.tchalanet.lan`
- `https://auth.tchalanet.lan`
- `https://app.tchalanet.lan`
- `https://traefik.tchalanet.lan`

Use `tchalanet.lan` for a simple home/LAN setup. If your router already uses a
different local domain, adapt the examples.

## 1. Choose the Manjaro LAN IP

On the Manjaro host:

```bash
ip -4 addr
```

Pick the LAN address reachable from the Mac, for example:

```text
192.168.1.50
```

Prefer a DHCP reservation in the router so the Manjaro host keeps the same IP.

## 2. Configure DNS on the LAN

Best option: configure your router, Pi-hole, AdGuard Home, or dnsmasq with:

```text
*.tchalanet.lan -> 192.168.1.50
```

If wildcard DNS is not available, add explicit records:

```text
api.tchalanet.lan     -> 192.168.1.50
auth.tchalanet.lan    -> 192.168.1.50
app.tchalanet.lan     -> 192.168.1.50
traefik.tchalanet.lan -> 192.168.1.50
flags.tchalanet.lan   -> 192.168.1.50
mob.tchalanet.lan     -> 192.168.1.50
```

Fallback on the Mac, if you cannot edit LAN DNS:

```bash
sudo nano /etc/hosts
```

Add:

```text
192.168.1.50 api.tchalanet.lan auth.tchalanet.lan app.tchalanet.lan traefik.tchalanet.lan flags.tchalanet.lan mob.tchalanet.lan
```

Flush macOS DNS cache:

```bash
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder
```

## 3. Create trusted local certificates

On Manjaro, install `mkcert` and generate a certificate for the LAN names:

```bash
mkcert -install

cd /path/to/tchalanet/tchalanet-infra
mkdir -p traefik/certs

mkcert \
  -key-file traefik/certs/local-key.pem \
  -cert-file traefik/certs/local-cert.pem \
  "*.tchalanet.lan" \
  "tchalanet.lan" \
  "api.tchalanet.lan" \
  "auth.tchalanet.lan" \
  "app.tchalanet.lan" \
  "traefik.tchalanet.lan" \
  "flags.tchalanet.lan" \
  "mob.tchalanet.lan"
```

Export the mkcert root CA from Manjaro:

```bash
mkcert -CAROOT
```

Copy the `rootCA.pem` from that directory to the Mac, then on the Mac import it
into Keychain Access:

1. Open Keychain Access.
2. Import `rootCA.pem` into `System`.
3. Open the certificate details.
4. Set Trust to `Always Trust`.

Restart browsers after trusting the CA.

## 4. Configure Tchalanet LAN hosts

For a LAN profile, use these runtime values:

```dotenv
API_HOST=api.tchalanet.lan
FLAGS_HOST=flags.tchalanet.lan
APP_WEB_HOST=app.tchalanet.lan
TRAEFIK_HOST=traefik.tchalanet.lan

KC_HOSTNAME=auth.tchalanet.lan
KC_HOSTNAME_URL=https://auth.tchalanet.lan
KC_HOST=auth.tchalanet.lan
KC_BASE_URL_EXTERNAL_URL=https://auth.tchalanet.lan

TCH_KC_ISSUER=https://auth.tchalanet.lan/realms/tchalanet
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.tchalanet.lan/realms/tchalanet
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/tchalanet/protocol/openid-connect/certs
```

For quick testing, you can adapt `envs/dev/.env`, `envs/dev/keycloak.env`,
`envs/dev/compose.env`, and `traefik/env/dev.yaml`. For a durable setup, create
a separate `lan` environment so local laptop-only dev remains unchanged.

## 5. Start on Manjaro

From `tchalanet-infra`:

```bash
make env-merge ENV=dev
make render-traefik ENV=dev
make local-api-up ENV=dev
```

If you created a dedicated `lan` environment, use `ENV=lan` consistently and add
matching `envs/lan/*` and `traefik/env/lan.yaml` files before starting.

## 6. Verify from the Mac

DNS:

```bash
dig +short api.tchalanet.lan
dig +short auth.tchalanet.lan
```

TLS and API:

```bash
curl -k https://api.tchalanet.lan/api/v1/actuator/health
```

Expected response:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

Browser checks:

- `https://auth.tchalanet.lan`
- `https://api.tchalanet.lan/api/v1/swagger-ui`
- `https://traefik.tchalanet.lan`

## Troubleshooting

If the Mac cannot resolve the name, fix DNS first. `api.tchalanet.lan` must
resolve to the Manjaro LAN IP.

If the browser shows a certificate warning, import the Manjaro mkcert root CA
into the Mac System keychain and set it to `Always Trust`.

If the API starts but authentication fails, check that Keycloak issuer and API
issuer match exactly:

```bash
curl -k https://auth.tchalanet.lan/realms/tchalanet/.well-known/openid-configuration
```

The returned `issuer` must match:

```text
https://auth.tchalanet.lan/realms/tchalanet
```

If Docker containers cannot reach each other by LAN names, keep internal service
URLs on Docker service names where possible, for example `http://keycloak:8080`
for JWKS.
