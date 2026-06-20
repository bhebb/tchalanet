# Tester les modes d'authentification

## Firebase Auth Emulator

```bash
make -C ../tchalanet-infra local-ide-up ENV=dev

export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase-emulator
export FIREBASE_PROJECT_ID=demo-tchalanet-local
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

Token :

```bash
TOKEN="$(scripts/firebase-id-token.sh)"
```

## Firebase réel

```bash
export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase
export FIREBASE_PROJECT_ID=<project-id>
export FIREBASE_CREDENTIALS_PATH=/chemin/non-versionne/service-account.json
```

Token Email/Password :

```bash
export FIREBASE_AUTH_BASE_URL=https://identitytoolkit.googleapis.com/v1
export FIREBASE_API_KEY=<firebase-web-api-key>
export FIREBASE_EMAIL=<test-user-email>
export FIREBASE_PASSWORD=<test-user-password>
TOKEN="$(scripts/firebase-id-token.sh)"
```

## Local JWT déterministe

```bash
export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=local-jwt
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters

TOKEN="$(scripts/local-jwt-token.sh)"
```

## Smoke

```bash
curl -i http://localhost:8083/api/v1/runtime/private \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Request-Id: tch_req_auth_mode_smoke_0001"
```

Résultat attendu : `200` pour une identité liée et active; `403` si le token est
valide mais non lié à un `AppUser`.
