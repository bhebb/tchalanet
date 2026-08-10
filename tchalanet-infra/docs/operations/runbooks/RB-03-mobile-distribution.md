# RB-03 — Distribution mobile Android

**Scope V0 :** Android uniquement. iOS optionnel, non couvert ici.

**Quand utiliser ce runbook :** build et distribution d'une version mobile vers
staging, pilote ou production.

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

Firebase Auth peut être partagé au début (`tchalanet`) ou séparé par
environnement si le cutover Firebase prod est prêt. Dans tous les cas, le backend
ciblé doit valider les tokens du même projet Firebase que l'app distribuée.

---

## Canaux

| Canal | Outil | Artefact | Usage |
|---|---|---|---|
| `staging` | Firebase App Distribution | APK release | QA interne rapide. |
| `pilot` | Google Play internal/closed | AAB | Premiers terminaux clients/vendeurs sans APK manuel. |
| `prod-internal` | Google Play Internal testing | AAB | Validation store. |
| `prod` | Google Play Production | AAB | MEP client avec rollout progressif. |

Firebase App Distribution est un canal beta. Les invitations testeurs expirent
après 30 jours si elles ne sont pas acceptées, et les releases restent
disponibles 150 jours dans Firebase. Pour une flotte prod, privilégier Google
Play / Managed Google Play.

**Décision MEP :** ne pas demander aux vendeurs ou clients d'activer un mode
développeur, une autorisation APK ou "sources inconnues". Firebase App
Distribution reste réservé à l'équipe Tchalanet et à la QA interne. Les
vendeurs/clients passent par Google Play dès que le compte Play Console est prêt.

---

## Installation côté utilisateur

Firebase App Distribution reste utile pour QA/staging, mais son parcours est
réservé à des testeurs techniques :

- le testeur doit accepter l'invitation Firebase ;
- il peut devoir installer ou ouvrir Firebase App Tester ;
- selon le téléphone, Android peut demander d'autoriser l'installation depuis
  cette source, ce qui ressemble au parcours "mode dev / sources inconnues".

Google Play est le canal attendu pour un vendeur ou un client :

- en `internal` ou `closed`, le testeur passe par un lien d'opt-in puis installe
  depuis le Play Store ;
- en `production`, l'app est installée et mise à jour depuis le Play Store ;
- pas d'APK à transmettre à la main ;
- pas d'autorisation "sources inconnues" à expliquer à un vendeur.

Pour des terminaux fournis ou contrôlés par Tchalanet, viser Managed Google Play
ou un MDM après la V0 : installation privée, mises à jour contrôlées et moins
d'actions côté utilisateur.

---

## Versioning

Ne pas se fier à la mémoire humaine pour le dernier build Android.

État actuel du workflow staging :

- `versionName` vient de l'input `build_name`.
- `versionCode` vient de `github.run_number`.

Cible MEP :

- lire la base `version:` depuis `tchalanet-mobile/pubspec.yaml` ;
- calculer un `versionCode` mobile global depuis les tags
  `mobile/android-build-<number>` ;
- créer les tags seulement après distribution réussie ;
- garder `build_name` comme override d'urgence, pas comme input normal.

Tags recommandés :

```text
mobile/android-build-000123
mobile/staging/v1.0.0-stg.123+123
mobile/prod/v1.0.0+123
```

Android refuse les updates avec un `versionCode` inférieur ou déjà publié.
Un rollback mobile doit donc publier un nouveau build connu-bon avec un
`versionCode` supérieur.

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

## Étape 1 — Build APK staging / QA Firebase

```bash
cd tchalanet-mobile

flutter build apk \
  --release \
  --build-name=<version-staging> \
  --build-number=<build-number> \
  --dart-define=API_BASE_URL=https://api.stg.tchalanet.com/api/v1 \
  --dart-define=FIREBASE_AUTH_EMULATOR=false \
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

## Étape 4 — Préparer Google Play prod

Firebase App Distribution est conservé pour les builds QA/staging. Pour une
livraison client fin août, préparer Google Play avant la MEP :

1. Créer le compte Google Play Console de l'organisation.
2. Garder le package Android `com.tchalanet.mobile`.
3. Activer Play App Signing et conserver une upload key dédiée.
4. Ajouter un workflow manuel séparé qui produit un AAB :
   `flutter build appbundle --release`.
5. Ajouter un secret futur `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` pour le
   service account Play Console.
6. Publier d'abord sur track `internal`, puis `closed`, puis `production`.

Build AAB prod attendu :

```bash
cd tchalanet-mobile

flutter build appbundle \
  --release \
  --build-name=<version-prod> \
  --build-number=<build-number> \
  --dart-define=API_BASE_URL=https://api.tchalanet.com/api/v1 \
  --dart-define=FIREBASE_AUTH_EMULATOR=false \
  --dart-define=TERMINAL_EMAIL_DOMAIN=terminal.tchalanet.com \
  --dart-define=POS_DEVICE_BINDING=
```

Le workflow Play doit refuser une prod si :

- la branche n'est pas `main` ou un tag release approuvé ;
- l'API ne pointe pas vers `https://api.tchalanet.com/api/v1` ;
- le domaine terminal n'est pas `terminal.tchalanet.com` ;
- le `versionCode` n'est pas supérieur au dernier build mobile ;
- `FIREBASE_AUTH_EMULATOR` n'est pas forcé à `false`.

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
| Build APK Android release staging/pilote | Build iOS (pas de Mac runner CI) |
| Distribution Firebase App Distribution | App Store iOS |
| Préparation Google Play Android | Workflow Google Play à implémenter |
| Config via dart-define | Configuration runtime mobile distante |
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
