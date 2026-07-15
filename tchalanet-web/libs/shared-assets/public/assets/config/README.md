# Runtime Config

Each app loads one stable file at runtime:

- `runtime.public-portal.json`
- `runtime.admin-portal.json`
- `runtime.platform-portal.json`

Deployment must copy one profile-specific file to the stable name before serving assets.

Profiles:

- `*.local-ide.json` points to `/api/v1` through the local dev-server proxy.
- `*.dev-docker.json` points to `https://api.localtest.me/api/v1`.
- `*.local-ide-emulator.json` uses the same proxied API as `local-ide` with Firebase Auth emulator.
- `*.dev-docker-emulator.json` uses the same API as `dev-docker` with Firebase Auth emulator.
- `*.stg-cloudflare.json` points to `https://api.stg.tchalanet.com/api/v1`.
- `*.prod-cloudflare.json` points to `https://api.tchalanet.com/api/v1`.

Local profiles use Firebase directly by default. Use the emulator variants when needed:

```json
{
  "firebaseAuthEmulatorUrl": "http://localhost:9099"
}
```

Examples:

```sh
pnpm runtime:local-ide
pnpm runtime:local-ide-emulator
pnpm runtime:dev-docker
pnpm runtime:dev-docker-emulator
pnpm runtime:stg-cloudflare
pnpm runtime:prod-cloudflare
```

Angular `environment.ts` is only the fallback. Apps and libs read runtime config through
`@tch/shared-config`.

`public-portal` also uses `portalBaseUrls` after login to jump to the right deployed app:

```json
{
  "portalBaseUrls": {
    "admin-portal": "/admin",
    "platform-portal": "/platform"
  }
}
```

Staging currently uses:

```json
{
  "portalBaseUrls": {
    "admin-portal": "/admin",
    "platform-portal": "/platform"
  }
}
```
