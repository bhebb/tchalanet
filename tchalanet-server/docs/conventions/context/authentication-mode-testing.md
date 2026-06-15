# Tester les modes d'authentification

## Statut

**NORMATIF**

Ce runbook teste les quatre modes de vérification d'identité supportés par le backend :
`keycloak`, `firebase-emulator`, `firebase` et `local-jwt`.

Le provisioning externe est supporté uniquement par Firebase. Keycloak est un adaptateur de
transition; `local-jwt` sert au développement et aux E2E.

## Smoke commun

Après avoir exporté les variables du mode choisi, démarrer l'API dans un terminal :

```bash
./mvnw -pl tchalanet-app -am spring-boot:run
```

Puis obtenir `TOKEN` et tester dans un autre terminal :

```bash
curl -i http://localhost:8083/api/v1/runtime/private \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Request-Id: tch_req_auth_mode_smoke_0001"
```

Résultat attendu : `200` pour une identité liée et active. Un token valide sans mapping Tchalanet
reçoit normalement `403`.

## 1. Keycloak

Keycloak doit être démarré. L'API doit utiliser le provider de transition `keycloak`.

```bash
make local-ide-up ENV=dev

export SPRING_PROFILES_ACTIVE=local-ide,insecure
export TCH_IDENTITY_PROVIDER=keycloak
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.localtest.me/realms/tchalanet
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/certs
```

Obtenir un token :

```bash
TOKEN="$(
  curl -k -sS -X POST \
    'https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode 'client_id=tchalanet-swagger' \
    --data-urlencode 'username=admin' \
    --data-urlencode 'password=Changeme1!' |
  jq -r '.access_token'
)"
```

## 2. Firebase Auth Emulator

```bash
make local-ide-up ENV=dev

export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase-emulator
export FIREBASE_PROJECT_ID=demo-tchalanet-local
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

Obtenir un token :

```bash
TOKEN="$(scripts/firebase-id-token.sh)"
# ou : scripts/firebase-id-token.sh --email cashier@local
```

## 3. Firebase réel

```bash
export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase
export FIREBASE_PROJECT_ID=tchalanet-39115
export FIREBASE_CREDENTIALS_PATH="$PWD/tchalanet-39115-firebase-adminsdk-fbsvc-62e904a236.json"
export FIREBASE_BOOTSTRAP_ENABLED=false
export FIREBASE_BOOTSTRAP_AUTO_RUN_ON_STARTUP=false
```

Obtenir un token Email/Password avec la Web API key publique :

```bash
export FIREBASE_AUTH_BASE_URL=https://identitytoolkit.googleapis.com/v1
export FIREBASE_API_KEY=<firebase-web-api-key>
export FIREBASE_EMAIL=<test-user-email>
export FIREBASE_PASSWORD=<test-user-password>
TOKEN="$(scripts/firebase-id-token.sh)"
```

Ne jamais committer le JSON du compte de service.

## 4. Local JWT

L'API et le script doivent utiliser exactement le même issuer et le même secret.

```bash
export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=local-jwt
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters

TOKEN="$(scripts/local-jwt-token.sh)"
# ou : scripts/local-jwt-token.sh --user super_admin
```

Les identités seedées sont `super_admin`, `admin` et `cashier`. `local-jwt` est interdit en
production et ne contourne ni les permissions DB ni PostgreSQL RLS.

## Matrice rapide

| Mode | Token | Provisioning externe | Production |
|---|---|---|---|
| `keycloak` | endpoint token Keycloak | Non, transition seulement | Transition |
| `firebase-emulator` | `scripts/firebase-id-token.sh` | Oui, émulateur | Interdit |
| `firebase` | `scripts/firebase-id-token.sh` avec API réelle | Oui | Oui |
| `local-jwt` | `scripts/local-jwt-token.sh` | Non | Interdit |

## Diagnostic

| Symptôme | Vérification |
|---|---|
| `400 request_id.missing` | Ajouter `X-Request-Id` |
| `401` | Provider actif, issuer, signature, expiration |
| `403 User not provisioned` | L'identité externe n'est pas liée à un `AppUser` |
| Token émulateur refusé | Utiliser `firebase-emulator` et définir `FIREBASE_AUTH_EMULATOR_HOST` |
| Token local refusé | Même `TCH_LOCAL_JWT_SECRET` côté API et script |
