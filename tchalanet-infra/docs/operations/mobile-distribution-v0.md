# Mobile Distribution v0

## Stratégie

Distribution sobre, Android first, sans store public.

```
Canal 1 — test interne    : APK signé dev/staging, partage direct
Canal 2 — client pilote   : Play Console internal/closed testing ou APK contrôlé
Canal 3 — vendeurs/agents : Android first, iOS uniquement si client l'exige
```

**Règle** : ne jamais distribuer une app à un client dépendant d'un staging jetable (Hetzner disposable) sans en informer explicitement le destinataire.

---

## Décisions v0

| Décision                        | Choix                                    |
| ------------------------------- | ---------------------------------------- |
| Plateforme prioritaire vendeurs | Android                                  |
| iOS                             | TestFlight seulement si client/démo      |
| Store public                    | Non en v0                                |
| Pipeline CI mobile              | Manuel uniquement, pas d'automatique     |
| Configuration d'env             | `--dart-define` en v0, flavors plus tard |

---

## Configuration d'environnement Flutter

### Variables publiques (dart-define)

Ces variables ne contiennent aucun secret. Elles configurent les URLs et l'identité OAuth publique.

| Variable         | dev                                          | staging                                           | prod                                          |
| ---------------- | -------------------------------------------- | ------------------------------------------------- | --------------------------------------------- |
| `APP_ENV`        | `dev`                                        | `staging`                                         | `prod`                                        |
| `API_BASE_URL`   | `https://api.localtest.me`                   | `https://api.stg.tchalanet.com`                   | `https://api.tchalanet.com`                   |
| `AUTH_ISSUER`    | `https://auth.localtest.me/realms/tchalanet` | `https://auth.stg.tchalanet.com/realms/tchalanet` | `https://auth.tchalanet.com/realms/tchalanet` |
| `AUTH_CLIENT_ID` | `tchalanet-mobile`                           | `tchalanet-mobile`                                | `tchalanet-mobile`                            |

### Lecture dans le code Flutter

```dart
const appEnv     = String.fromEnvironment('APP_ENV',       defaultValue: 'dev');
const apiBaseUrl = String.fromEnvironment('API_BASE_URL',  defaultValue: 'https://api.localtest.me');
const authIssuer = String.fromEnvironment('AUTH_ISSUER',   defaultValue: 'https://auth.localtest.me/realms/tchalanet');
const authClient = String.fromEnvironment('AUTH_CLIENT_ID', defaultValue: 'tchalanet-mobile');
```

### Passage à flavors plus tard

Si la configuration s'élargit (icônes, noms d'app, GoogleServices, etc.), migrer vers flavors :

```
android/app/src/dev/
android/app/src/staging/
android/app/src/prod/
```

---

## Build APK local

### Prérequis

- Flutter SDK installé (voir `tchalanet-mobile/README.md` ou `flutter.dev`)
- Android Studio ou Android SDK avec émulateur / téléphone USB
- `flutter doctor` vert

### Test local dev

```bash
cd tchalanet-mobile

flutter pub get
flutter test

# Sur émulateur ou téléphone branché (USB debug activé)
flutter run \
  --dart-define=APP_ENV=dev \
  --dart-define=API_BASE_URL=https://api.localtest.me \
  --dart-define=AUTH_ISSUER=https://auth.localtest.me/realms/tchalanet \
  --dart-define=AUTH_CLIENT_ID=tchalanet-mobile
```

**Prérequis infra dev** : `make p0-up ENV=dev` dans `tchalanet-infra/`.

### Build APK staging

```bash
cd tchalanet-mobile

# Calcul du versionCode (nombre de commits depuis le début)
VERSION_CODE=$(git rev-list --count HEAD)
VERSION_NAME="1.0.0"
COMMIT_SHA=$(git rev-parse --short HEAD)
BUILD_LABEL="${VERSION_NAME}+${VERSION_CODE}-staging.${COMMIT_SHA}"

flutter build apk --release \
  --build-name="${VERSION_NAME}" \
  --build-number="${VERSION_CODE}" \
  --dart-define=APP_ENV=staging \
  --dart-define=API_BASE_URL=https://api.stg.tchalanet.com \
  --dart-define=AUTH_ISSUER=https://auth.stg.tchalanet.com/realms/tchalanet \
  --dart-define=AUTH_CLIENT_ID=tchalanet-mobile
```

L'APK est généré dans `build/app/outputs/flutter-apk/app-release.apk`.

Renommer avant distribution :

```bash
cp build/app/outputs/flutter-apk/app-release.apk \
   build/app/outputs/flutter-apk/tchalanet-${BUILD_LABEL}.apk
```

### Build APK prod (plus tard)

```bash
flutter build apk --release \
  --dart-define=APP_ENV=prod \
  --dart-define=API_BASE_URL=https://api.tchalanet.com \
  --dart-define=AUTH_ISSUER=https://auth.tchalanet.com/realms/tchalanet \
  --dart-define=AUTH_CLIENT_ID=tchalanet-mobile
```

---

## Versioning

Format : `versionName+versionCode-env.sha`

```
1.0.0+12-staging.abcd1234
```

| Champ         | Source                       |
| ------------- | ---------------------------- |
| `versionName` | Manuel dans `pubspec.yaml`   |
| `versionCode` | `git rev-list --count HEAD`  |
| `env`         | `APP_ENV` dart-define        |
| `sha`         | `git rev-parse --short HEAD` |

---

## Signing Android

Le keystore ne doit jamais être committé.

1. Générer un keystore :

   ```bash
   keytool -genkey -v -keystore tchalanet-release.jks \
     -alias tchalanet -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Stocker dans Doppler (projet `mobile`, config `prod`/`staging`) :

   ```
   ANDROID_KEYSTORE_BASE64
   ANDROID_KEY_ALIAS
   ANDROID_KEY_PASSWORD
   ANDROID_STORE_PASSWORD
   ```

3. En local, créer `android/key.properties` (ignoré par git) :

   ```properties
   storeFile=/chemin/vers/tchalanet-release.jks
   storePassword=...
   keyAlias=tchalanet
   keyPassword=...
   ```

4. Dans `android/app/build.gradle`, lire `key.properties` si présent.

---

## Distribution Android interne

### APK direct (court terme)

Partager l'APK signé via :

- Google Drive ou Notion partagé avec les testeurs
- `adb install tchalanet-staging.apk` pour les testeurs techniques

### Play Console internal testing (quand nécessaire)

1. Créer une fiche app sur Google Play Console
2. Upload AAB (Android App Bundle, préféré à APK pour le store) :
   ```bash
   flutter build appbundle --release ...
   ```
3. Utiliser le canal "Internal testing" → partager par email

---

## Distribution iOS — TestFlight

TestFlight uniquement si un client/démo iOS le demande explicitement.

Prérequis :

- Mac avec Xcode
- Apple Developer Program (99 USD/an)
- Certificat de distribution + provisioning profile

Build :

```bash
flutter build ipa --release \
  --dart-define=APP_ENV=staging \
  --dart-define=API_BASE_URL=https://api.stg.tchalanet.com \
  --dart-define=AUTH_ISSUER=https://auth.stg.tchalanet.com/realms/tchalanet \
  --dart-define=AUTH_CLIENT_ID=tchalanet-mobile
```

Upload via Xcode Organizer ou `xcrun altool`. Les testeurs reçoivent une invitation TestFlight.

Apple Business Program et Custom App Store reportés à post-v0.

---

## Relation avec l'infra

L'app mobile consomme uniquement des endpoints publics via Traefik :

```
https://api.stg.tchalanet.com    → API Spring Boot
https://auth.stg.tchalanet.com   → Keycloak OIDC
https://edge.stg.tchalanet.com   → Edge service (notifications/webhooks si besoin)
```

**Aucun secret backend dans l'app.** Les décisions critiques (permissions, ventes, tirages, règlement) restent backend.

Token : OIDC via Keycloak. Le client mobile est `publicClient: true` sans `clientSecret`.

### Staging jetable

Le staging Hetzner peut être détruit et recréé (`make staging-destroy` / `make staging-create`). Tant que l'app est en test interne, c'est acceptable. **Ne pas distribuer à un vrai client une app pointant sur staging sans en informer le destinataire.**

---

## CI/CD — État actuel

Pas de workflow automatique. Builds locaux uniquement.

### Brouillon `mobile-release.yml` (à créer plus tard)

Quand un premier client pilote arrive, créer un workflow GitHub Actions manuel :

```yaml
# .github/workflows/mobile-release.yml (brouillon — ne pas activer maintenant)
name: Mobile Release
on:
  workflow_dispatch:
    inputs:
      platform:
        description: 'Platform'
        type: choice
        options: [android, ios]
      env:
        description: 'Environment'
        type: choice
        options: [staging, prod]
      release_track:
        description: 'Release track'
        type: choice
        options: [internal, closed, testflight]
```

Contenu : checkout → Flutter setup → pub get → test → build → sign → upload.

Ce workflow n'est **pas créé maintenant** pour éviter les coûts CI et la complexité prématurée.

---

## Ce que l'app mobile ne fait pas

- Aucune décision de permission ou de rôle côté client
- Aucun secret API ou credential backend embarqué
- Aucune logique de settlement ou de tirage
- Aucune gestion de tenant policy
- Aucun mode offline chiffré en v0 (OpenSpec séparé si nécessaire)
