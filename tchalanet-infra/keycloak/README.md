# Keycloak (image custom)

Cette image Keycloak inclut:
- Un thème personnalisé: `themes/tchalanet`
- Un provider custom (protocol mapper): `tch-json-claim-mapper`
- Un mécanisme de génération de realm par environnement (via `scripts/keycloak/get-realm.sh`)

## Provider (tch-json-claim-mapper)
- Source: `keycloak/tchalanet-keycloak-provider/` (Java 21, Keycloak 26.6.2)
- Build: fait automatiquement au build de l’image (Dockerfile) via `./mvnw -DskipTests clean package`
- Déploiement: le JAR est copié dans l’image (répertoire `/opt/keycloak/providers`)
- Mapper ID: `tch-json-claim-mapper` (claim JSON `tch` dans ID/Access/UserInfo)

Important:
- Ne pas committer de JAR généré dans ce repo. Les binaires seront produits au build de l’image.
- Si vous devez tester manuellement, placez temporairement un JAR dans `keycloak/providers/` (voir ci-dessous) — il sera ignoré par git.
- Option: publier des versions du JAR (par ex. GitHub Releases) et documenter l’URL pour le pinning si nécessaire.

## Thème
- Dossier: `themes/tchalanet`
- Pages customisées: login.ftl, register.ftl, i18n en/fr/ht, CSS/Logo
- Pour activer: le realm généré utilise `loginTheme=tchalanet`.

## Realms (génération par env)
- Base: `keycloak/realms/realm.base.json`
- Generation:
```bash
# Variables optionnelles
export KC_LOGIN_THEME=tchalanet
export DEFAULT_LOCALE=fr
export SUPPORTED_LOCALES="en,fr,ht"
export TEST_USER_PASSWORD=Changeme1!

# Génère keycloak/realms/<realm>-realm.json (suffixe -realm.json)
./scripts/keycloak/get-realm.sh staging
```
- Overlay (optionnel): `keycloak/realms/overlays/<ENV>.json` (fusion simple, l’overlay gagne)
- Import auto: le Dockerfile copie uniquement les fichiers `*-realm.json` dans `/opt/keycloak/data/import/` (le template base n’est pas importé)
- Le script ajoute un client scope `tch` avec le mapper custom et l’attache aux `defaultClientScopes` des clients.
- Utilisateurs de test: 1 par rôle, login = nom du rôle en minuscules, mot de passe = `Changeme1!` (surchageable via `TEST_USER_PASSWORD`).
  - Le mot de passe doit satisfaire la `passwordPolicy` du realm (`length(10) and upperCase(1) and lowerCase(1) and digits(1)`).
  - `Changeme1!` est conforme : 10 chars, 1 majuscule (`C`), minuscules, 1 chiffre (`1`), 1 spécial.
  - Si tu changes la policy : aligne tous les seed users + les `.env.local` de `tchalanet-server/scripts/` et de `tchalanet-server/tests/e2e/`.

## Build & Run
```bash
# Builder l’image custom Keycloak
make -C tchalanet-infra build-keycloak

# Démarrer le noyau (Postgres + Keycloak)
make -C tchalanet-infra up-core ENV=staging
```

## Git ignore (JAR provider)
- Le dossier `keycloak/providers/` peut contenir des exemples/configs. Les JARs générés doivent être ignorés par git.
- Règle ajoutée: `keycloak/providers/.gitignore` avec `*.jar`
- Le module maven `tchalanet-keycloak-provider` ignore déjà `target/` via son propre `.gitignore`.

## Exemple de mapper (référence)
- Voir `keycloak/providers/mapper-config.json.example` pour une config JSON type du mapper `tch-json-claim-mapper`.

## Pinning d’une version du provider (optionnel)
- Si vous publiez le JAR sur un registre (ex: GitHub Releases), ajoutez ici l’URL et la procédure de pinning (wget/curl + COPY dans Dockerfile ou volume runtime). Par défaut, nous construisons le JAR lors du build de l’image.
