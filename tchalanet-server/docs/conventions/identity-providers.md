# Identity Providers

## Invariant

```text
Provider externe = authentifie l'identité externe
Tchalanet DB      = AppUser, memberships, rôles et permissions
TchRequestContext = contexte canonique
PostgreSQL RLS    = isolation tenant finale
```

## Providers supportés

| `TCH_IDENTITY_PROVIDER` | Usage | Production |
| --- | --- | --- |
| `firebase` | Provider standard | Oui |
| `firebase-emulator` | Local IDE / tests fonctionnels | Non |
| `local-jwt` | E2E déterministe | Non |
| `local-perf` | Performance / RLS | Non |

`firebase-emulator`, `local-jwt` et `local-perf` sont interdits avec un profil
production.

## Local IDE

```bash
export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase-emulator
export FIREBASE_PROJECT_ID=demo-tchalanet-local
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

Obtenir un token :

```bash
scripts/firebase-id-token.sh --export
```

## Production

```bash
export TCH_IDENTITY_PROVIDER=firebase
export FIREBASE_PROJECT_ID=<project-id>
export FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
```

Firebase ne porte pas les permissions métier. Les rôles et permissions sont
résolus depuis Tchalanet.
