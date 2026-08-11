# Distribution Android

Ce guide décrit la création et l'installation d'un fichier Android pour
Tchalanet Mobile. L'application est Android-first pour les terminaux vendeurs
et les appareils Android personnels.

## Deux types de fichier

| Usage | Fichier | Installation | Signature |
| --- | --- | --- | --- |
| Test local ou recette interne | APK debug | Installation directe sur l'appareil | Clé debug Android |
| Staging/pilote | APK release | Firebase App Distribution | Clé de release Tchalanet |
| Production | AAB release | Google Play / Managed Google Play | Play App Signing + upload key Tchalanet |

Ne distribuez jamais un APK debug en dehors d'un environnement de test
contrôlé. Il est identifié comme une version de développement et sa signature
ne peut pas devenir la signature de production.

## Préparer une version de test installable

Depuis `tchalanet-mobile/` :

```bash
fvm flutter pub get
fvm flutter analyze
fvm flutter test
fvm flutter build apk --debug \
  --build-name=0.1.0 \
  --build-number=1 \
  --dart-define=API_BASE_URL=https://<api-environnement>/api/v1 \
  --dart-define=FIREBASE_AUTH_EMULATOR=false \
  --dart-define=TERMINAL_EMAIL_DOMAIN=<terminal-domain> \
  --dart-define=POS_DEVICE_BINDING=<binding-si-requis>
```

Le fichier créé est :

```text
build/app/outputs/flutter-apk/app-debug.apk
```

Le numéro `--build-number` doit augmenter à chaque fichier remis à un testeur.
Le `API_BASE_URL` doit pointer vers l'environnement prévu; il ne doit jamais
pointer vers une URL de développement sur un appareil remis à un vendeur.

## Versioning de distribution

Android impose un `versionCode` croissant pour chaque nouvelle version installée
ou publiée sous le même `applicationId` (`com.tchalanet.mobile`). Le workflow ne
doit donc pas demander à l'opérateur de se rappeler le dernier build.

État actuel :

- `pubspec.yaml` porte `version: 1.0.0+1`.
- Le workflow staging calcule `--build-name` depuis `pubspec.yaml` avec un
  suffixe `-stg.<build>`.
- `--build-number` est alimenté automatiquement par le workflow.
- L'opérateur ne saisit pas de numéro de version au lancement du workflow.

Cible avant MEP :

1. `versionName` vient automatiquement de `pubspec.yaml`.
2. Le canal ajoute seulement un suffixe lisible :
   - staging : `1.0.0-stg.<build>` ;
   - pilote : `1.0.0-pilot.<build>` ;
   - prod : `1.0.0`.
3. `versionCode` vient d'un compteur mobile global, pas du `run_number` d'un
   workflow particulier.
4. Le compteur est incrémenté seulement après une distribution réussie.
5. Le workflow crée des tags de traçabilité :
   - `mobile/android-build-000123` ;
   - `mobile/staging/v1.0.0-stg.123+123` ;
   - `mobile/prod/v1.0.0+123`.

Tant que cette automatisation n'est pas en place, vérifier manuellement que le
`--build-number` transmis est strictement supérieur au dernier build remis.

## Installer un APK sur un appareil Android

Cette méthode est réservée au développement local, à QA interne ou à un test
technique ponctuel. Elle ne doit pas devenir le parcours vendeur/client.

Pour les vendeurs et clients, utiliser Google Play `internal`, `closed` ou
`production` dès que le compte Play Console est prêt. Ils installent alors depuis
le Play Store, sans APK manuel ni explication "mode dev / sources inconnues".

1. Dans Android, autorisez temporairement l'installation depuis la source qui
   contient le fichier APK.
2. Copiez `app-debug.apk` sur l'appareil, puis ouvrez-le et confirmez
   l'installation.
3. Ouvrez Tchalanet, connectez le terminal vendeur et vérifiez la présence des
   tirages disponibles avant de démarrer une vente.
4. Après le test, désactivez de nouveau l'autorisation d'installation depuis
   cette source si l'appareil est géré.

Avec ADB pour un appareil branché :

```bash
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

`-r` met à jour l'application en conservant ses données. Pour repartir d'une
session propre, désinstallez l'application depuis Android avant l'installation.

## Canaux de distribution

| Canal | Outil | Artefact | Usage |
| --- | --- | --- | --- |
| `staging` | Firebase App Distribution | APK release | Recette interne rapide sur vrais appareils. |
| `pilot` | Google Play internal/closed | AAB | Premiers vendeurs/clients contrôlés sans APK manuel. |
| `prod-internal` | Google Play Internal testing | AAB | Validation store avant ouverture prod. |
| `prod` | Google Play Production | AAB | MEP client avec rollout progressif. |

Firebase App Distribution est pratique pour itérer vite, mais ce n'est pas le
canal final d'une flotte de production. Les invitations testeurs expirent après
30 jours si elles ne sont pas acceptées, et les releases App Distribution sont
disponibles 150 jours dans Firebase. Pour une MEP professionnelle, préparer
Google Play dès maintenant.

**Décision MEP :** ne pas demander aux vendeurs ou clients d'activer un mode
développeur, une autorisation APK ou "sources inconnues". Firebase App
Distribution reste réservé à l'équipe Tchalanet et à la QA interne.

Google Play apporte :

- installation et mise à jour par un canal officiel ;
- Android App Bundle (`.aab`) au lieu d'un APK transmis à la main ;
- Play App Signing ;
- tracks `internal`, `closed`, `production` ;
- rollout progressif et retour arrière plus propre.

Managed Google Play ou un MDM devient utile si Tchalanet fournit ou contrôle
des terminaux : auto-update forcé, restrictions appareil, mode kiosque, et
déploiement d'app privée à une organisation.

### Installation côté utilisateur

Firebase App Distribution peut encore demander un effort au testeur : accepter
l'invitation, passer par Firebase App Tester, puis autoriser Android à installer
un APK depuis cette source selon le téléphone. C'est acceptable pour QA/staging,
mais fragile pour des vendeurs ou clients non techniques.

Avec Google Play `internal`, `closed` ou `production`, l'utilisateur installe
depuis le Play Store. Il n'y a plus d'APK à envoyer ni d'explication "mode dev /
sources inconnues" pour une installation normale. Si les terminaux sont
contrôlés par Tchalanet, Managed Google Play ou un MDM permet de réduire encore
les actions côté vendeur.

## Préparer la distribution release Android

La signature release est maintenant pilotée par `android/key.properties`, créé
par le workflow depuis les secrets GitHub Actions. En local, sans ce fichier, le
build release retombe sur la signature debug pour faciliter les essais. Avant
toute distribution opérateur ou store :

1. Créer une clé Android de release détenue par l'organisation.
2. Stocker le mot de passe et la clé hors du dépôt, dans le coffre de secrets
   et/ou le système CI.
3. Configurer une `signingConfig` release qui lit ces secrets sans les écrire
   dans Git.
4. Construire un APK signé pour distribution directe, ou un AAB pour le Play
   Store.
5. Vérifier sur un appareil vierge: installation, connexion Firebase, chargement
   des tirages, préparation de vente, confirmation et impression.

Une fois la signature configurée, les commandes attendues seront :

```bash
fvm flutter build apk --release \
  --build-name=<version> \
  --build-number=<numéro> \
  --dart-define=API_BASE_URL=https://<api-environnement>/api/v1 \
  --dart-define=FIREBASE_AUTH_EMULATOR=false \
  --dart-define=TERMINAL_EMAIL_DOMAIN=<terminal-domain> \
  --dart-define=POS_DEVICE_BINDING=<binding-si-requis>

fvm flutter build appbundle --release \
  --build-name=<version> \
  --build-number=<numéro> \
  --dart-define=API_BASE_URL=https://<api-environnement>/api/v1 \
  --dart-define=FIREBASE_AUTH_EMULATOR=false \
  --dart-define=TERMINAL_EMAIL_DOMAIN=<terminal-domain> \
  --dart-define=POS_DEVICE_BINDING=<binding-si-requis>
```

Les artefacts de release seront respectivement sous :

```text
build/app/outputs/flutter-apk/app-release.apk
build/app/outputs/bundle/release/app-release.aab
```

## Préparer Google Play

À faire avant la MEP fin août :

1. Créer ou vérifier le compte Google Play Console de l'organisation.
2. Créer l'app Android avec le package définitif `com.tchalanet.mobile`.
3. Activer **Play App Signing**.
4. Définir l'upload key utilisée par CI.
5. Créer les tracks :
   - `internal` pour l'équipe Tchalanet ;
   - `closed` pour vendeurs/clients pilotes ;
   - `production` pour la MEP.
6. Créer un service account Google Play avec droits limités à la gestion des
   releases.
7. Stocker le JSON du service account en secret GitHub :
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64`.
8. Préparer un workflow manuel `mobile-publish-play.yml` :
   - build AAB release ;
   - versioning automatique ;
   - publication sur track `internal` par défaut ;
   - promotion explicite vers `closed` puis `production`.

Le workflow prod doit refuser une publication si :

- la branche n'est pas `main` ou un tag release approuvé ;
- `API_BASE_URL` ne pointe pas vers l'API prod ;
- `TERMINAL_EMAIL_DOMAIN` n'est pas le domaine terminal prod ;
- `FIREBASE_AUTH_EMULATOR` n'est pas `false` ;
- le `versionCode` n'est pas strictement supérieur au dernier build mobile.

## Garde-fous installabilité

Avant toute distribution Firebase ou Play, le workflow doit valider
l'artefact Android produit, puis prouver qu'Android peut l'installer.

Garde minimale obligatoire :

1. `flutter build apk --release` ou `flutter build appbundle --release`.
2. Validation de l'artefact avec `scripts/validate_android_artifact.sh` :
   - fichier présent et zip lisible ;
   - signature APK vérifiée par `apksigner` ;
   - `applicationId` attendu `com.tchalanet.mobile` ;
   - `versionCode` positif et `versionName` présent ;
   - activité launcher présente ;
   - refus des URLs locales (`localhost`, `127.*`, `10.0.2.2`) pour une build distribuée.
3. Installation smoke test sur émulateur Android avec `adb install`.
4. Distribution seulement si ces étapes passent.

Ces contrôles doivent rester avant l'étape Firebase App Distribution ou Google
Play. Si `adb install` échoue, l'artefact est considéré non distribuable même si
le build Flutter a réussi.

## Outils externes prod

| Outil | Rôle mobile/prod | Configuration à vérifier |
| --- | --- | --- |
| Firebase Auth | Connexion des vendeurs et validation des tokens côté API. | Projet/app prod, domaine terminal prod, backend en `RUNTIME_IDENTITY_PROVIDER=firebase`, utilisateurs terminaux provisionnés. |
| Firebase App Distribution | Staging et QA interne rapide. | Groupes `staging`, `qa`; service account avec rôle App Distribution Admin. |
| Google Play Console | Distribution prod Android. | Play App Signing, tracks `internal/closed/production`, service account release. |
| Doppler | Secrets runtime par environnement. | Secrets Firebase/backend/API par env, pas de secret embarqué dans l'app. |
| GitHub Secrets/Variables | Secrets CI et paramètres de build. | Keystore, Firebase SA, Google Play SA, app ids, API URLs, domaines terminaux. |
| Cloudflare | DNS/TLS/routage public. | DNS API/web prod, certificats, règles WAF/cache si nécessaires. |
| Grafana Cloud | Observabilité. | Dashboards et alertes API/mobile-adjacent : auth, prepare/confirm sale, erreurs 401/403/5xx, latence. |

## Checklist de remise

- Version et build number renseignés et uniques.
- URL API de l'environnement contrôlée.
- Analyse et tests Flutter verts.
- Installation vérifiée sur un appareil Android réel.
- Connexion vendeur, changement de PIN si requis, tirages, vente et
  réimpression vérifiés.
- Fichier et somme de contrôle communiqués par un canal approuvé.
- Aucun secret, mot de passe, PIN ou jeton n'est inclus dans le fichier remis.

## Sources externes utiles

- Firebase App Distribution FAQ :
  <https://firebase.google.com/docs/app-distribution/troubleshooting?platform=android>
- Google Play testing tracks :
  <https://support.google.com/googleplay/android-developer/answer/9845334>
- Google Play release rollout :
  <https://support.google.com/googleplay/android-developer/answer/9859348>
