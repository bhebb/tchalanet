# Distribution Android

Ce guide décrit la création et l'installation d'un fichier Android pour
Tchalanet Mobile. L'application est Android-first pour les terminaux vendeurs
et les appareils Android personnels.

## Deux types de fichier

| Usage | Fichier | Installation | Signature |
| --- | --- | --- | --- |
| Test local ou recette interne | APK debug | Installation directe sur l'appareil | Clé debug Android |
| Distribution opérateur / Play Store | APK ou AAB release | MDM, téléchargement contrôlé ou Play Store | Clé de release Tchalanet |

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
  --dart-define=POS_DEVICE=true
```

Le fichier créé est :

```text
build/app/outputs/flutter-apk/app-debug.apk
```

Le numéro `--build-number` doit augmenter à chaque fichier remis à un testeur.
Le `API_BASE_URL` doit pointer vers l'environnement prévu; il ne doit jamais
pointer vers une URL de développement sur un appareil remis à un vendeur.

## Installer sur un appareil Android

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

## Préparer la distribution release

La distribution release n'est pas encore activée: le fichier
`android/app/build.gradle.kts` utilise volontairement la signature debug afin
de permettre les essais locaux. Avant toute distribution opérateur ou store :

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
  --dart-define=POS_DEVICE=true

fvm flutter build appbundle --release \
  --build-name=<version> \
  --build-number=<numéro> \
  --dart-define=API_BASE_URL=https://<api-environnement>/api/v1 \
  --dart-define=POS_DEVICE=true
```

Les artefacts de release seront respectivement sous :

```text
build/app/outputs/flutter-apk/app-release.apk
build/app/outputs/bundle/release/app-release.aab
```

## Checklist de remise

- Version et build number renseignés et uniques.
- URL API de l'environnement contrôlée.
- Analyse et tests Flutter verts.
- Installation vérifiée sur un appareil Android réel.
- Connexion vendeur, changement de PIN si requis, tirages, vente et
  réimpression vérifiés.
- Fichier et somme de contrôle communiqués par un canal approuvé.
- Aucun secret, mot de passe, PIN ou jeton n'est inclus dans le fichier remis.
