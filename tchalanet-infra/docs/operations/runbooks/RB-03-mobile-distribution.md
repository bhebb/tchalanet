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

Firebase Auth est le même projet (`tchalanet-39115`) pour tous les envs — les rôles sont différenciés côté API.

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
firebase use tchalanet-39115
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
  --app 1:768000918177:android:5fc04b59928349269aa6e0 \
  --release-notes "Staging — $(git rev-parse --short HEAD) — $(date +%Y-%m-%d)" \
  --groups "internal-testers"
```

Le groupe `internal-testers` doit exister dans Firebase Console → App Distribution → Testers & Groups.

**Vérification :** les testeurs reçoivent un email Firebase avec un lien de téléchargement.

---

## Étape 3 — Via GitHub Actions (manuel)

Le workflow `mobile-distribute.yml` n'existe pas encore. Structure recommandée quand il sera créé :

```yaml
name: Mobile — Distribute Staging

on:
  workflow_dispatch:
    inputs:
      env:
        description: 'Environnement cible'
        required: true
        default: 'staging'
        type: choice
        options: [staging, prod]
      release_notes:
        description: 'Notes de release'
        required: false
        default: ''

jobs:
  build-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7

      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.x'
          channel: stable

      - name: Build APK
        working-directory: tchalanet-mobile
        run: |
          flutter build apk --release \
            --dart-define=API_BASE_URL=${{ vars.TCH_API_BASE_URL_STG }}/api/v1 \
            --dart-define=TERMINAL_EMAIL_DOMAIN=${{ vars.TCH_TERMINAL_EMAIL_DOMAIN_STG }} \
            --dart-define=POS_DEVICE_BINDING=

      - name: Upload to Firebase App Distribution
        uses: wzieba/Firebase-Distribution-Github-Action@v1
        with:
          appId: ${{ secrets.FIREBASE_ANDROID_APP_ID }}
          serviceCredentialsFileContent: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
          groups: internal-testers
          file: tchalanet-mobile/build/app/outputs/flutter-apk/app-release.apk
          releaseNotes: ${{ inputs.release_notes || github.sha }}
```

**GitHub Secrets requis :**
| Secret | Valeur |
|---|---|
| `FIREBASE_ANDROID_APP_ID` | `1:768000918177:android:5fc04b59928349269aa6e0` |
| `FIREBASE_SERVICE_ACCOUNT` | JSON du compte de service Firebase (rôle Firebase App Distribution Admin) |

**GitHub Variables (non sensibles) :**
| Variable | Valeur |
|---|---|
| `TCH_API_BASE_URL_STG` | `https://api.stg.tchalanet.com` |
| `TCH_TERMINAL_EMAIL_DOMAIN_STG` | `terminal.stg.tchalanet.com` |

---

## Ajouter un testeur

```bash
firebase appdistribution:testers:add \
  --app 1:768000918177:android:5fc04b59928349269aa6e0 \
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
