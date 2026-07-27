# RB-03 — Distribution mobile (Android staging)

**Scope V0 :** Android uniquement, distribution interne via Firebase App Distribution. iOS optionnel (non couvert ici).

**Quand utiliser ce runbook :** build et distribution d'une version staging à un testeur interne.

**Durée estimée :** 15-20 min (hors compilation Flutter première fois).

> **Contrainte spec :** les builds mobiles ne sont **jamais** déclenchés automatiquement. Toujours `workflow_dispatch` ou commande locale manuelle.

---

## Architecture de configuration

L'app Flutter reçoit sa configuration via `--dart-define` au moment du build — pas de fichier `.env` ni de config runtime fetchée.

| Variable | Staging | Production |
|---|---|---|
| `API_BASE_URL` | `https://api.stg.tchalanet.com/api/v1` | `https://api.tchalanet.com/api/v1` |
| `TERMINAL_EMAIL_DOMAIN` | `terminal.stg.tchalanet.com` | `terminal.tchalanet.com` |
| `POS_DEVICE_BINDING` | `e2e-cred-dev` (tests) ou vide | vide (activation T12) |

Firebase Auth est le même projet (`tchalanet`) pour tous les envs — les rôles sont différenciés côté API.

---

## Prérequis locaux

```bash
# Flutter SDK
flutter --version   # 3.x minimum

# Android toolchain
flutter doctor      # tout doit être vert pour Android

# Firebase CLI (pour upload App Distribution)
firebase --version  # 13+ recommandé

# Connexion au projet Firebase
firebase login
firebase use tchalanet
```

---

## Étape 1 — Build APK staging

```bash
cd tchalanet-mobile

flutter build apk \
  --release \
  --dart-define=API_BASE_URL=https://api.stg.tchalanet.com/api/v1 \
  --dart-define=TERMINAL_EMAIL_DOMAIN=terminal.stg.tchalanet.com \
  --dart-define=POS_DEVICE_BINDING=
```

Le fichier de sortie : `build/app/outputs/flutter-apk/app-release.apk`

> Pour les tests e2e internes (seed V217+), utiliser `--dart-define=POS_DEVICE_BINDING=e2e-cred-dev`.

---

## Étape 2 — Distribuer via Firebase App Distribution

```bash
firebase appdistribution:distribute \
  build/app/outputs/flutter-apk/app-release.apk \
  --app 1:1050094456835:android:afb4836a45c441769a3e36 \
  --release-notes "Staging — $(git rev-parse --short HEAD) — $(date +%Y-%m-%d)" \
  --groups "staging"
```

Le groupe `staging` doit exister dans Firebase Console → App Distribution → Testers & Groups.

**Vérification :** les testeurs reçoivent un email Firebase avec un lien de téléchargement.

---

## Étape 3 — Via GitHub Actions (manuel)

Le workflow réel est `.github/workflows/mobile-distribute-staging.yml`
(`Mobile Distribute (staging)`). Il est manuel uniquement et construit un APK
release signé, puis le pousse vers Firebase App Distribution.

```bash
gh workflow run mobile-distribute-staging.yml \
  -f api_base_url="https://api.stg.tchalanet.com/api/v1" \
  -f terminal_email_domain="terminal.stg.tchalanet.com" \
  -f pos_device_binding="" \
  -f build_name="1.0.0-stg" \
  -f tester_groups="staging" \
  -f release_notes="Staging $(git rev-parse --short HEAD)"
```

**GitHub Secrets requis :**
| Secret | Valeur |
|---|---|
| `FIREBASE_ADMIN_JSON_BASE64` | JSON base64 du compte de service Firebase avec rôle Firebase App Distribution Admin |
| `TCH_ANDROID_KEYSTORE_BASE64` | Keystore Android release encodé base64 |
| `TCH_ANDROID_KEYSTORE_PASSWORD` | Mot de passe du keystore |
| `TCH_ANDROID_KEY_ALIAS` | Alias de la clé release |
| `TCH_ANDROID_KEY_PASSWORD` | Mot de passe de la clé release |

**GitHub Variables (non sensibles) :**
| Variable | Valeur |
|---|---|
| `FIREBASE_ANDROID_APP_ID` | `1:1050094456835:android:afb4836a45c441769a3e36` si différent du défaut workflow |

Le backend ciblé doit être en `RUNTIME_IDENTITY_PROVIDER=firebase`, utiliser le
projet Firebase `tchalanet` et le même domaine terminal que le build
(`terminal.stg.tchalanet.com`).

---

## Préparer Google Play plus tard

Firebase App Distribution est conservé pour les builds QA/staging. Pour une
livraison client plus tard :

1. Créer le compte Google Play Console de l'organisation.
2. Garder le package Android `com.tchalanet.mobile`.
3. Activer Play App Signing et conserver une upload key dédiée.
4. Ajouter un workflow manuel séparé qui produit un AAB :
   `flutter build appbundle --release`.
5. Ajouter un secret futur `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` pour le
   service account Play Console.
6. Publier d'abord sur track `internal`, puis `closed`, puis `production`.

---

## Ajouter un testeur

```bash
firebase appdistribution:testers:add \
  --app 1:1050094456835:android:afb4836a45c441769a3e36 \
  testeur@email.com
```

Ou depuis Firebase Console → App Distribution → Testers & Groups → Add testers.

---

## Contraintes V0

| Ce qui est couvert | Ce qui n'est PAS couvert |
|---|---|
| Build APK Android release | Build iOS (pas de Mac runner CI) |
| Distribution Firebase App Distribution | Google Play / App Store |
| Config staging via dart-define | Signing keystore prod (à créer) |
| Manuel uniquement | Auto-build sur PR/push |

---

## Troubleshooting

| Symptôme | Cause probable | Action |
|---|---|---|
| `flutter doctor` erreurs Android | SDK ou licenses manquants | `flutter doctor --android-licenses` |
| APK build OK mais crashes au launch | `API_BASE_URL` non accessible | Vérifier que le serveur staging est up (`make smoke-staging`) |
| Firebase upload `401 Unauthorized` | `firebase login` expiré | `firebase login --reauth` |
| Testeur ne reçoit pas l'email | Pas dans le groupe | `firebase appdistribution:testers:add` |
| Build très lent en CI | Pas de cache Flutter | Utiliser `actions/cache` sur `.pub-cache` et `build/` |
