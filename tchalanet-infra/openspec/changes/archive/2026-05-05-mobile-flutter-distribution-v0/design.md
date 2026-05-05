# Design: mobile-flutter-distribution-v0

## Canaux de distribution

### Canal 1: Démo/dev rapide

```text
Android : APK signé dev/staging
Web preview : optionnel selon Flutter target
```

Usage : tests personnels, validation interne, démo non critique.

### Canal 2: Client pilote

```text
Android : Play Console internal/closed testing ou APK contrôlé
IOS : TestFlight si nécessaire
```

Usage : client/pilote identifié.

### Canal 3: Vendeurs/agents plus tard

```text
Phase 1 : Android first
Phase 2 : distribution par tenant/outlet
Phase 3 : MDM ou managed/private store si gros client
Phase 4 : iOS vendeur seulement si besoin métier
```

## Environnements Flutter

Définir trois environnements :

```text
dev
staging
prod
```

Configuration par `--dart-define` ou flavors :

```bash
flutter build apk --dart-define=APP_ENV=staging --dart-define=API_BASE_URL=https://api.stg.tchalanet.com
```

Plus tard, préférer flavors si la configuration devient plus large :

```text
android/app/src/dev
android/app/src/staging
android/app/src/prod
```

## Versioning

Chaque build mobile doit inclure :

```text
versionName
versionCode
commit SHA
APP_ENV
API_BASE_URL
```

Exemple :

```text
1.0.0+12-staging.shaabcd1234
```

## CI/CD

### Maintenant

Pas de workflow mobile automatique.

Les builds peuvent être faits localement :

```bash
flutter pub get
flutter test
flutter build apk --release --dart-define=APP_ENV=staging
```

### Plus tard

Créer `mobile-release.yml` manuel :

```yaml
on:
  workflow_dispatch:
    inputs:
      platform:
        type: choice
        options: [android, ios]
      env:
        type: choice
        options: [staging, prod]
      release_track:
        type: choice
        options: [internal, closed, testflight]
```

## Secrets mobile

Ne pas stocker secrets dans l'app mobile. L'app mobile ne doit contenir que :

```text
API base URL
Keycloak issuer/client public
feature display config non critique
```

Toute décision critique reste backend :

```text
permissions
vente
tickets
draws
settlement
tenant policy
```

## Sécurité

- Pas de credentials backend dans l'app.
- Pas de secret API dans l'app.
- Token OIDC via Keycloak.
- Refresh/offline tokens à décider plus tard selon UX vendeur.
- Offline mode local chiffré à traiter dans un OpenSpec métier/mobile séparé.

## Distribution vendeur

Pour les vendeurs, Android first :

```text
moins de friction hardware
plus compatible terminaux/POS
plus simple pour pilotes
```

Distribution initiale :

```text
APK contrôlé ou Play internal/closed testing
```

À éviter au début :

```text
App Store public
Play Store public
MDM complexe
multi-client white-label mobile
```

## Relation avec infra Hetzner

Mobile consomme :

```text
https://api.stg.tchalanet.com
https://auth.stg.tchalanet.com
https://edge.stg.tchalanet.com si besoin livraison/notifications/webhooks
```

Staging peut être disposable tant que l'app est en test interne. Ne pas distribuer à un client une app dépendant d'un staging jetable sans prévenir.
