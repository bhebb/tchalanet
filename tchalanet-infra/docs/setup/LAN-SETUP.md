# LAN Setup

La stack LAN expose Traefik, l'API, le web et l'edge-service. Firebase reste
externe. Pour une session LAN E2E/debug jetable, utilisez Firebase Auth
Emulator; pour `local-ide` standard, utilisez Firebase réel.

## Variables LAN E2E/debug

```bash
APP_CORS_ALLOWED_ORIGINS=http://localhost:*,https://app.localtest.me
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=<host-lan>:9099
```

## Démarrage

```bash
make local-ide-up-redis ENV=dev
```
