# tch-lottery-proxy

Cloudflare Worker relay for the US-lottery result fetchers that get blocked when
called directly from the Hetzner staging server (datacenter/ASN-level bot
blocking on galottery.com, njlottery.com, calottery.com, ohiolottery.com,
palottery.pa.gov). The Worker re-issues the request from Cloudflare's network
with a normal browser `User-Agent`.

## Deploy (one-time)

```bash
cd tchalanet-infra/cloudflare-worker-proxy
npx wrangler login          # or export CLOUDFLARE_API_TOKEN
npx wrangler secret put PROXY_SHARED_SECRET   # paste a random value, e.g. openssl rand -base64 32
npx wrangler deploy
```

Note the deployed URL (`https://tch-lottery-proxy.<your-subdomain>.workers.dev`).

## Usage

```
GET https://tch-lottery-proxy.<subdomain>.workers.dev/?url=<url-encoded target>
Headers:
  X-Proxy-Secret: <the shared secret>
  X-Fwd-Accept: application/json          (optional - forwarded as Accept)
  X-Fwd-User-Agent: ...                   (optional - overrides the default browser UA)
```

Any request header prefixed `X-Fwd-` is forwarded to the target with the
prefix stripped. If no `X-Fwd-User-Agent` is sent, a standard browser
User-Agent is used by default.

Only hosts in the `ALLOWED_HOSTS` allowlist in `src/index.js` are proxyable -
add a host there before pointing a new provider at this Worker.

## Server-side wiring (tchalanet-server)

Once deployed, the `PROXY_SHARED_SECRET` and Worker URL become Doppler
secrets/env vars, and each blocked provider's `base-url` in
`application-uslottery.yaml` gets changed to route through this Worker
(exact wiring is a separate follow-up change, not done yet).
