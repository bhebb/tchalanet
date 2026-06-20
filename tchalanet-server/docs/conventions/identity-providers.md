# Identity Providers

## Invariant

```text
Firebase            = authentifie l'identité externe (TOUS les acteurs)
Tchalanet DB        = app_user, seller_terminal, memberships, rôles, permissions
TchRequestContext   = contexte canonique
PostgreSQL RLS      = isolation tenant finale
```

## Providers supportés

| `TCH_IDENTITY_PROVIDER` | Usage | Production |
| --- | --- | --- |
| `firebase` | Provider standard | Oui |
| `firebase-emulator` | Local IDE / tests fonctionnels | Non |
| `local-jwt` | E2E déterministe | Non |
| `local-perf` | Performance / RLS | Non |

`firebase-emulator`, `local-jwt` et `local-perf` sont interdits avec un profil production.

## Deux types d'acteurs — même pipeline Firebase

Tous les acteurs s'authentifient via Firebase. La distinction se fait côté Tchalanet :

| Acteur | Firebase UID mappé vers | TchActorType |
|---|---|---|
| Utilisateur humain (web, mobile admin) | `app_user.firebase_uid` | `APP_USER` |
| Terminal de vente POS | `seller_terminal.firebase_uid` | `SELLER_TERMINAL` |

`IdentityProviderApi.mapVerifiedToken()` détermine le type d'acteur en cherchant le `firebase_uid` d'abord dans `seller_terminal`, puis dans `app_user`.

## SellerTerminal — authentification Firebase

Un SellerTerminal s'authentifie avec :
- **email** : `<terminalCode>@<tenantCode>.tchalanet` (email fictif créé au provisioning)
- **password** : PIN à 6 chiffres

Le PIN est géré via Firebase Authentication password API. Il n'est jamais stocké en clair dans Tchalanet DB.

Voir flow complet : `tchalanet-docs/docs/02-functional/flows/seller-onboarding.md`

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

Firebase ne porte pas les permissions métier. Les rôles et permissions sont résolus depuis Tchalanet.
