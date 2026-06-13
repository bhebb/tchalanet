# Identity Providers — Configuration et tests

## Statut

**NORMATIF**

Ce guide est le point d'entrée pour configurer et tester l'authentification externe du backend.

## Invariant

```text
Provider externe = authentifie l'identité externe
Tchalanet DB      = AppUser, memberships, rôles et permissions
TchRequestContext = contexte canonique
PostgreSQL RLS    = isolation tenant finale
```

Les rôles présents dans un token servent uniquement de hints de pré-routage Spring Security.
Avant l'exécution des handlers, Tchalanet remplace ces hints par les rôles et permissions chargés
depuis la base.

## Providers supportés

| Valeur `TCH_IDENTITY_PROVIDER` | Usage | Production |
|---|---|---|
| `firebase` | Provider cible V0 | Oui |
| `firebase-emulator` | Tests fonctionnels et intégration locale | Interdit |
| `local-jwt` | Développement et E2E | Interdit |
| `local-perf` | Performance et validation RLS | Interdit |

`firebase-emulator`, `local-jwt` et `local-perf` font échouer le démarrage si un profil actif
contient `prod` ou `production`.

## Modes local IDE

Le profil `local-ide` utilise Firebase Auth Emulator par défaut :

```bash
make -C ../tchalanet-infra up-firebase-emulator ENV=dev

export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase-emulator
export FIREBASE_PROJECT_ID=demo-tchalanet-local
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

L'émulateur Auth écoute sur `http://localhost:9099`. Il émet des tokens non signés. Le backend les
accepte uniquement lorsque `firebase-emulator` est explicitement actif hors production. Aucun
credential Firebase Admin n'est chargé dans ce mode.

Au démarrage `local-ide`, le bootstrap crée et lie dans Firebase Emulator les trois comptes seedés
dans Tchalanet :

| Utilisateur | Email | Mot de passe |
|---|---|---|
| `super_admin` | `super_admin@local` | `Changeme1!` |
| `admin` | `admin@local` | `Changeme1!` |
| `cashier` | `cashier@local` | `Changeme1!` |

Leurs UID Firebase sont les UUID déterministes des lignes `app_user`.

Pour tester la vraie instance Firebase staging depuis le même profil IDE :

```bash
export SPRING_PROFILES_ACTIVE=local-ide
export TCH_IDENTITY_PROVIDER=firebase
export FIREBASE_PROJECT_ID=tchalanet-39115
export FIREBASE_CREDENTIALS_PATH=/chemin/non-versionne/service-account.json
```

Le changement de mode ne nécessite aucune modification de code. L'émulateur sert aux tests
fonctionnels et d'intégration, pas à mesurer les performances de Firebase. Les tests de charge
backend utilisent `local-jwt`.

Web local :

```bash
# Firebase Auth Emulator, défaut development
pnpm nx serve tch-portal

# Vraie instance Firebase staging
pnpm nx serve tch-portal --configuration=real-firebase
```

## Obtenir un ID token pour Swagger et curl

Swagger expose uniquement un schéma HTTP bearer provider-neutral. Il n'effectue aucun login
Keycloak/OAuth. Le bouton **Authorize** attend un Firebase ID token brut; Swagger ajoute lui-même
le préfixe `Bearer`.

Avec Firebase Auth Emulator :

```bash
make -C ../tchalanet-infra up-firebase-emulator ENV=dev

export FIREBASE_EMAIL=swagger.admin@example.test
export FIREBASE_PASSWORD=dev-only-password
export FIREBASE_CREATE_USER=true
export FIREBASE_ID_TOKEN="$(scripts/firebase-id-token.sh)"
```

Après la première création, omettre `FIREBASE_CREATE_USER` pour les connexions suivantes :

```bash
unset FIREBASE_CREATE_USER
export FIREBASE_ID_TOKEN="$(scripts/firebase-id-token.sh)"
```

Avec la vraie instance Firebase staging, activer Email/Password pour l'utilisateur de test, puis
utiliser la Web API key publique du projet :

```bash
export FIREBASE_AUTH_BASE_URL=https://identitytoolkit.googleapis.com/v1
export FIREBASE_API_KEY=<firebase-web-api-key>
export FIREBASE_EMAIL=<test-user-email>
export FIREBASE_PASSWORD=<test-user-password>
export FIREBASE_ID_TOKEN="$(scripts/firebase-id-token.sh)"
```

Tester ensuite l'API directement :

```bash
curl -i http://localhost:8083/api/v1/tenant/runtime/bootstrap \
  -H "Authorization: Bearer ${FIREBASE_ID_TOKEN}" \
  -H "X-Request-Id: firebase-swagger-smoke-001"
```

L'identité Firebase doit correspondre à un `AppUser` préprovisionné ou autorisé par la politique
de bootstrap active. Un token valide sans mapping Tchalanet peut donc recevoir un `403`.

## Configuration commune

```bash
export TCH_IDENTITY_PROVIDER=firebase
export TCH_SECURITY_USER_BOOTSTRAP_ENABLED=true
export TCH_SECURITY_USER_BOOTSTRAP_MODE=admin-preprovisioned
```

Modes de bootstrap :

| Mode | Comportement |
|---|---|
| `deny` | Refuse toute identité externe inconnue. Défaut production monétaire. |
| `admin-preprovisioned` | Lie une première connexion à un `AppUser ACTIVE` créé par un admin. |
| `invite-only` | Lie un `AppUser INVITED`, puis le passe à `PENDING_APPROVAL`. |
| `controlled-auto` | Crée un `AppUser PENDING_APPROVAL` uniquement pour une allowlist email. |

`controlled-auto` ne crée jamais de membership, rôle ou permission. En production, il exige
explicitement :

```bash
export TCH_SECURITY_USER_BOOTSTRAP_CONTROLLED_AUTO_ALLOWED_EMAILS=user@example.com
export TCH_SECURITY_USER_BOOTSTRAP_CONTROLLED_AUTO_ALLOWED_DOMAINS=example.com
export TCH_SECURITY_USER_BOOTSTRAP_ALLOW_CONTROLLED_AUTO_IN_PRODUCTION=true
```

## Firebase — configuration backend

### 1. Configuration Firebase Console

1. Créer ou sélectionner le projet Firebase.
2. Ouvrir **Authentication > Sign-in method**.
3. Activer **Phone** et/ou les providers souhaités.
4. Pour Phone, configurer **Authentication > Settings > SMS region policy**.
   Les nouveaux projets n'autorisent aucune région par défaut. Autoriser explicitement le Canada
   et, lorsque requis, Haïti.
5. Pour le web, ajouter le domaine de l'application dans **Authorized domains**.
6. Créer un service account ou utiliser Application Default Credentials pour les contrôles de
   révocation/disabled-user.

### 2. Variables backend

```bash
export TCH_IDENTITY_PROVIDER=firebase
export FIREBASE_PROJECT_ID=tchalanet-dev
export FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-service-account.json
export FIREBASE_REVOCATION_CHECK_MODE=sensitive-only
export TCH_SECURITY_USER_BOOTSTRAP_MODE=admin-preprovisioned
```

`FIREBASE_CREDENTIALS_PATH` peut être omis lorsque l'environnement fournit des Application Default
Credentials.

Modes de révocation :

| Mode | Comportement |
|---|---|
| `off` | Vérifie signature/issuer/audience/expiry, sans appel Firebase Admin. |
| `sensitive-only` | Contrôle Firebase Admin uniquement pour les opérations classifiées sensibles. Défaut V0. |
| `always` | Effectue le contrôle Firebase Admin pour chaque requête authentifiée. |

La classification V0 est centralisée dans la chaîne Spring Security :

- toute requête authentifiée `POST`, `PUT`, `PATCH` ou `DELETE` est sensible, quel que soit son
  chemin ;
- toute requête portant `X-Tch-Tenant-Override` ou `X-Tenant-Id` est sensible, même en lecture ;
- une requête sensible est revérifiée avec `IdentityVerificationPolicy.SENSITIVE` avant le
  bootstrap `AppUser`, la création du `TchRequestContext` et l'exécution du handler ;
- un échec de révocation/disabled-user arrête la requête en `401`.

Cette règle conservatrice couvre notamment vente, payout, annulation, offline sync, changements
d'utilisateurs/rôles/permissions, terminal binding et override `SUPER_ADMIN`.

Ne jamais committer le JSON du service account.

### 3. Numéro canadien

Un numéro canadien est supporté. Il doit toujours utiliser le format E.164 :

```text
+14165551234
```

Règles :

- conserver `+1`, sans espace, parenthèse ou tiret ;
- préprovisionner exactement la même valeur dans `app_user.phone` ;
- vérifier que la SMS region policy Firebase autorise le Canada ;
- un vrai SMS consomme le quota Firebase et peut être throttlé.

Pour Haïti, le même principe s'applique avec `+509...`.

À la date de ce guide, le quota officiel Identity Platform sans instrument de facturation est de
**10 SMS de vérification par jour**. Utiliser les numéros fictifs Firebase pour les tests courants,
et confirmer le quota officiel au moment du go-live.

L'authentification par téléphone prouve la possession temporaire du numéro, pas l'identité civile.
Un numéro peut être transféré ou réattribué. Les permissions Tchalanet, le contexte opérationnel et
le binding/signing du terminal POS restent donc obligatoires et indépendants du token Firebase.

### 4. Préprovisionner l'utilisateur Tchalanet

Avec `admin-preprovisioned`, créer d'abord l'utilisateur via l'API admin :

```http
POST /api/v1/admin/identity/users
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "email": null,
  "phone": "+14165551234",
  "firstName": "Test",
  "lastName": "Canada",
  "role": "CASHIER",
  "outletId": "<outlet-id>",
  "terminalId": null
}
```

L'utilisateur doit être `ACTIVE`. Pour un `CASHIER`, `outletId` est obligatoire.

L'API crée d'abord l'identité Firebase active, puis l'`AppUser`, le membership, le rôle et le lien
durable `(FIREBASE, issuer, uid)`. Si la transaction Tchalanet échoue, un compte Firebase créé par
cette même requête est supprimé en compensation. Aucun rôle, tenant ou contexte opérationnel
n'est écrit dans Firebase.

À la connexion Firebase Phone :

1. Firebase vérifie le code SMS et émet un ID token.
2. Le backend retrouve directement l'`AppUser ACTIVE` par le lien durable créé au provisioning.
3. Les rôles, permissions, tenant et contexte RLS sont chargés depuis Tchalanet.

### 5. Tester sans envoyer de vrai SMS

Dans Firebase Console, configurer un **fictional phone number** et un code fixe. Firebase n'envoie
alors aucun SMS réel. Utiliser un numéro réservé au test, jamais le numéro réel d'une personne.

Le numéro fictif doit aussi être préprovisionné dans Tchalanet avec exactement le même format E.164.

### 6. Tester avec un vrai numéro canadien

Utiliser un vrai numéro uniquement pour un smoke test ponctuel :

1. Autoriser le Canada dans la SMS region policy.
2. Préprovisionner le numéro dans Tchalanet.
3. Se connecter depuis le client Firebase Web/Mobile/POS.
4. Récupérer le Firebase ID token côté client.
5. Appeler :

```bash
curl -i http://localhost:8083/api/v1/tenant/me/profile \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  -H "X-Request-Id: firebase-phone-smoke-001"
```

Le backend n'émet pas le code SMS et ne doit jamais recevoir le code SMS. Il reçoit seulement
l'ID token Firebase.

### 7. Cas Firebase à valider

| Cas | Résultat attendu |
|---|---|
| `AppUser ACTIVE` préprovisionné, téléphone identique | Première connexion liée, accès autorisé |
| Téléphone Firebase inconnu | `403 User not provisioned` |
| `AppUser SUSPENDED` | `403 User not active` |
| Mauvais project/audience/issuer | `401` token invalide |
| Token expiré ou malformed | `401` |
| Token révoqué ou utilisateur Firebase disabled sur opération sensible | `401` avec `sensitive-only` ou `always` |
| Rôle présent uniquement dans le token | Ignoré après résolution DB |
| Tenant fourni uniquement dans le body | Ignoré comme source d'autorité |

## Local JWT et local perf

Les deux providers utilisent un JWT HS256 signé localement, mais traversent le chemin normal :

```text
token local -> external identity -> AppUser -> tenant -> DB roles/permissions
            -> TchRequestContext -> PostgreSQL RLS
```

Configuration API :

```bash
export TCH_IDENTITY_PROVIDER=local-perf
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters
```

Configuration harness E2E :

```bash
export TCH_E2E_AUTH_PROVIDER=local-perf
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters
```

Après recréation de la DB avec les migrations courantes :

```bash
cd tchalanet-server/testing/e2e
python -m pytest tests/auth_context tests/multitenant/test_tenant_isolation.py -m "L2 or L3"
```

Les identités locales seedées sont `super_admin`, `admin` et `cashier`. `local-perf` ne contourne
ni les permissions ni RLS.

## Diagnostic rapide

| Symptôme | Vérification |
|---|---|
| API ne démarre pas en Firebase | `FIREBASE_PROJECT_ID`, credentials, provider actif |
| `401` Firebase | issuer/audience/signature/expiry, project ID, ID token et non refresh token |
| `401 Unsupported algorithm of none` avec un token émulateur | redémarrer l'API avec `TCH_IDENTITY_PROVIDER=firebase-emulator`; le mode `firebase` réel refuse obligatoirement les tokens non signés |
| `400 request_id.missing` | ajouter un header `X-Request-Id` unique à l'appel privé |
| `403 User not provisioned` | bootstrap mode, téléphone/email préprovisionné, mapping externe |
| `403 User not active` | statut local `AppUser` |
| Téléphone non lié | format E.164 exact et `firebase.sign_in_provider=phone` |
| SMS non envoyé | provider Phone activé, SMS region policy, quota/throttling |
| Rôle inattendu | vérifier `tenant_user_role`, `app_role` et permissions DB |
| Accès cross-tenant | arrêter la livraison et exécuter immédiatement la suite RLS |

## Références Firebase officielles

- Phone Auth Web : <https://firebase.google.com/docs/auth/web/phone-auth>
- Phone Auth Flutter : <https://firebase.google.com/docs/auth/flutter/phone-auth>
- Identity Platform quotas : <https://cloud.google.com/identity-platform/quotas>

Les quotas et règles Firebase peuvent changer. Les confirmer dans la documentation officielle au
moment du go-live.
