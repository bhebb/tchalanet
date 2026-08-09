# Workflows CI/CD — Tchalanet

Référence des workflows GitHub Actions du repo : à quoi ils servent, quand ils
se déclenchent, et comment les lancer. Section détaillée en fin de doc pour la
**distribution mobile staging** (lancement avec URL API et autres valeurs).

## Conventions

- **Actions épinglées au SHA.** Toutes les actions tierces (non `actions/*`) sont
  épinglées au commit SHA avec un commentaire de version que Dependabot met à
  jour (ex. `uses: appleboy/ssh-action@0ff4204…d301dee2 # v1.2.5`). Ne jamais
  repasser une action tierce à un tag mutable.
- **Gate de merge.** Sur `server-pr.yml`, `spotless`, `checkstyle` et `PMD` sont
  **bloquants** ; `SpotBugs` reste advisory. PMD ne bloque que sur les
  règles de correctness (priorité 1-4) ; complexité et `UnusedPrivateMethod` sont
  advisory (priorité 5, cf. `tchalanet-server/pmd/ruleset.xml`).
- **Analyses coûteuses hors PR.** CodeQL, OWASP Dependency-Check et les E2E/full
  validations tournent le week-end ou manuellement pour éviter de brûler les
  minutes CI sur chaque PR. Trivy reste attaché au build des images de validation.
- **Tags d'images immuables.** Les déploiements utilisent `sha-<short_sha>` ;
  `latest` est interdit en staging/prod.

## Vue d'ensemble

| Workflow | Déclenchement | Rôle | Bloquant |
|---|---|---|---|
| `server-pr.yml` | toutes PR, travail lourd si `tchalanet-server/**` | build+test, spotless/checkstyle/PMD, SpotBugs advisory | ✅ gate |
| `web-pr.yml` | PR `tchalanet-web/**` | lint, test, tsc, build | ✅ |
| `mobile-pr.yml` | PR `tchalanet-mobile/**` | `flutter analyze` + `flutter test` | ✅ |
| `edge-pr.yml` | PR `tchalanet-edge-service/**` | lint, test, build | ✅ |
| `infra-check.yml` | PR `tchalanet-infra/**` | env vars + `docker compose config` | ✅ |
| `docs.yml` | push/PR docs + manuel | build docs public + déploiement public manuel Cloudflare Workers | ✅ |
| `codeql.yml` | samedi 00:00 ET + manuel | SAST Java + TS/JS | advisory |
| `full-validation.yml` | samedi deps 02:00 ET, dimanche E2E 02:00 ET + manuel | samedi OWASP ; dimanche IT, build images (+Trivy), deploy jetable, E2E, perf | 🌙 |
| `deploy-infra-runtime.yml` | manuel | plan→build→deploy API/edge (staging/prod) | 🚀 |
| `deploy-staging.yml` | manuel | create/destroy/recreate serveur Hetzner | 🚀 |
| `reusable-staging-core-infra.yml` | `workflow_call` | Traefik/Redis core | 🔁 |
| **`mobile-distribute-staging.yml`** | **manuel** | **build APK release + Firebase App Distribution** | 🚀 |

Les workflows PR sont filtrés par `paths` : ne tournent que si le slice concerné
change. `workflow_dispatch` est disponible sur la plupart pour un lancement manuel.

## Lancer un workflow manuel

**UI** : onglet *Actions* → choisir le workflow → *Run workflow* → remplir les
inputs → *Run*.

**CLI** (`gh`) :

```bash
gh workflow run <fichier-ou-nom> -f <input>=<valeur> ...
gh run watch            # suivre le run en cours
```

---

## Déploiement runtime (`deploy-infra-runtime.yml`)

Déploie l'API et/ou l'edge-service. Le job `plan` valide les entrées, interdit
`latest`, et bloque la prod si l'image n'a pas de déploiement staging réussi.

```bash
gh workflow run deploy-infra-runtime.yml \
  -f target_environment=staging \
  -f runtime_services=api-and-edge-service \
  -f build_api=true -f build_edge=true \
  -f force_recreate=true
```

Pour vérifier une VM recréée avant le basculement DNS, ajouter
`-f validation_local_traefik=true`; le smoke conserve le hostname TLS et
redirige temporairement les appels vers le Traefik local.

Prod = promotion d'un tag déjà validé en staging (`build_*` interdits en prod).

---

## Distribution mobile staging (`mobile-distribute-staging.yml`)

Construit un **APK Android release signé**, pointé sur le backend choisi et le
**vrai Firebase `tchalanet`** (le même projet que admin/web), puis le
publie sur **Firebase App Distribution**. Déclenchement **manuel uniquement**.

Ce workflow est réservé à QA/staging interne. Pour vendeurs, clients pilotes et
prod, viser Google Play (`internal`, `closed`, puis `production`) afin d'éviter
les installations APK manuelles et les explications "mode dev / sources
inconnues".

### Inputs (valeurs passées au lancement)

| Input | Défaut | Rôle |
|---|---|---|
| `api_base_url` | `https://api.stg.tchalanet.com/api/v1` | **Lien vers l'API** ciblée par le build (`--dart-define=API_BASE_URL`). Inclure `/api/v1`. |
| `terminal_email_domain` | `terminal.stg.tchalanet.com` | Domaine e-mail des logins terminal — **doit correspondre au seeding backend**. |
| `pos_device_binding` | `` (vide) | `POS_DEVICE_BINDING`. Laisser vide sauf si un credential device est requis. |
| `build_name` | `1.0.0-stg` | `versionName` lisible. Temporaire : doit devenir auto-généré depuis `pubspec.yaml` + canal. |
| `tester_groups` | `staging` | Groupes de testeurs App Distribution (alias séparés par virgule). |
| `release_notes` | `Staging build` | Notes affichées aux testeurs. |

> `FIREBASE_AUTH_EMULATOR` est forcé à `false` (vrai Firebase). C'est le but de ce
> workflow ; ne pas le rendre configurable pour éviter les builds incohérents.

### Lancer

**CLI :**

```bash
gh workflow run mobile-distribute-staging.yml \
  -f api_base_url="https://api.stg.tchalanet.com/api/v1" \
  -f terminal_email_domain="terminal.stg.tchalanet.com" \
  -f pos_device_binding="" \
  -f build_name="1.0.0-stg" \
  -f tester_groups="staging" \
  -f release_notes="Build staging $(date +%F)"
```

Tous les inputs ont un défaut : `gh workflow run mobile-distribute-staging.yml`
sans `-f` produit le build staging standard.

**UI :** *Actions* → *Mobile Distribute (staging)* → *Run workflow* → renseigner
au minimum `api_base_url`, laisser le reste par défaut → *Run*.

### Prérequis (à poser une fois, côté toi)

1. **Secrets repo** (Settings → Secrets and variables → Actions) :
   - `TCH_ANDROID_KEYSTORE_BASE64` — `base64 -i release.jks | pbcopy`
   - `TCH_ANDROID_KEYSTORE_PASSWORD`, `TCH_ANDROID_KEY_ALIAS`, `TCH_ANDROID_KEY_PASSWORD`
   - `FIREBASE_ADMIN_JSON_BASE64` (existe déjà) — le service account doit avoir le
     rôle **Firebase App Distribution Admin** dans `tchalanet`.
2. **Variable repo optionnelle** : `FIREBASE_ANDROID_APP_ID` si l'App ID Android
   change (défaut : celui de `google-services.json`).
3. **Serveur staging** en `RUNTIME_IDENTITY_PROVIDER=firebase` (pas `firebase-emulator`),
   sinon il rejette les vrais tokens produits par le build.
4. **Terminaux staging** provisionnés comme vrais utilisateurs Firebase dans
   `tchalanet`.
5. **Groupe de testeurs** `staging` créé dans la console App Distribution.

### Configuration Firebase App Distribution

Dans Firebase Console → projet `tchalanet` :

1. Ajouter l'app Android `com.tchalanet.mobile` si elle n'existe pas déjà.
2. Récupérer l'**App ID Android** (`1:...:android:...`) et le mettre dans la variable
   repo `FIREBASE_ANDROID_APP_ID` si différent du défaut du workflow.
3. Activer **App Distribution** et créer les groupes internes `staging`, puis si
   besoin `ops` et `qa`.
4. Ajouter les testeurs par email dans les groupes.
5. Créer ou réutiliser un service account avec le rôle
   **Firebase App Distribution Admin**. Encoder son JSON en base64 et le stocker
   dans `FIREBASE_ADMIN_JSON_BASE64`.
6. Vérifier que `tchalanet-mobile/android/app/google-services.json` correspond
   au même projet Firebase.

Le build mobile utilise le vrai Firebase, donc le backend ciblé doit aussi être
configuré sur le même projet et le même domaine terminal :

| Élément | Staging attendu |
|---|---|
| Firebase project | `tchalanet` |
| Android package | `com.tchalanet.mobile` |
| API | `https://api.stg.tchalanet.com/api/v1` |
| Terminal email domain | `terminal.stg.tchalanet.com` |
| Backend identity provider | `firebase` |

### Ce que fait le job

`checkout` → JDK 17 + Flutter 3.44 → matérialise le keystore + `android/key.properties`
depuis les secrets → `flutter pub get` → `flutter build apk --release` avec les
dart-defines ci-dessus → distribue l'APK via `firebase-tools` (CLI officielle,
auth par service account) → **efface les secrets du disque** (`always()`).

La signature release est pilotée par `android/app/build.gradle.kts` : il lit
`android/key.properties` (git-ignoré) et retombe sur la clé debug quand ce fichier
est absent, pour que `flutter run --release` fonctionne en local sans keystore.

### Distribuer vers un autre backend

Le workflow n'est pas limité au staging : passer un autre `api_base_url` (et le
`terminal_email_domain` correspondant) distribue un build pointé ailleurs, tant
que ce backend valide les tokens du **vrai** Firebase `tchalanet`.

> Note flotte prod : App Distribution reste un canal **beta**. Les invitations
> testeurs expirent après 30 jours si elles ne sont pas acceptées, et les releases
> App Distribution sont disponibles 150 jours dans Firebase. Pour une flotte de
> terminaux en production, préparer Google Play / Managed Google Play ou un MDM
> pour avoir installation professionnelle, mises à jour et contrôle de flotte.

### Versioning mobile cible

Objectif : ne plus devoir se souvenir de la dernière version au moment de lancer
une distribution.

Le workflow staging actuel utilise `github.run_number` comme `versionCode`. C'est
acceptable pour un seul workflow, mais insuffisant dès que staging, pilote et prod
ont chacun leur workflow : Android impose un `versionCode` strictement croissant
pour un même `applicationId` (`com.tchalanet.mobile`).

La cible recommandée :

1. Lire la base `version:` dans `tchalanet-mobile/pubspec.yaml`
   (`1.0.0+1` aujourd'hui).
2. Calculer un compteur Android mobile global, indépendant du workflow :
   - source simple : dernier tag `mobile/android-build-<number>` ;
   - prochain build : `number + 1`.
3. Générer automatiquement :
   - staging : `--build-name=<pubspec-version>-stg.<build>` ;
   - pilote : `--build-name=<pubspec-version>-pilot.<build>` ;
   - prod : `--build-name=<pubspec-version>` ;
   - tous canaux : `--build-number=<build>`.
4. Créer le tag uniquement après distribution réussie :
   - compteur : `mobile/android-build-000123` ;
   - canal : `mobile/staging/v1.0.0-stg.123+123`,
     `mobile/prod/v1.0.0+123`.
5. Garder `build_name` comme override d'urgence seulement, pas comme input à
   renseigner à chaque distribution.

À implémenter dans les workflows :

- Ajouter un job/step `Resolve mobile version` avant le build.
- Donner au workflow `permissions: contents: write` seulement si le job doit
  pousser les tags.
- Protéger le tag post-distribution avec `if: success()`.
- Écrire `build_name`, `build_number` et les tags dans le GitHub Actions summary.

### Préparer Google Play pour la prod

Firebase App Distribution reste le bon canal pour staging et QA interne rapide.
Pour la MEP fin août, préparer Google Play maintenant même si la publication
production reste manuelle au début :

1. Créer le compte Google Play Console de l'organisation et garder le même
   package Android `com.tchalanet.mobile`.
2. Choisir la stratégie de signature :
   - recommandé : **Play App Signing** avec upload key dédiée ;
   - garder le keystore CI actuel comme upload key ou planifier une rotation.
3. Préparer un workflow séparé `mobile-publish-play.yml`, manuel uniquement,
   qui build un **AAB** (`flutter build appbundle`) au lieu d'un APK.
4. Créer un service account Google Play Console avec droits limités aux releases,
   stocké plus tard dans un secret dédié, par exemple
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64`.
5. Utiliser des tracks progressifs : `internal` → `closed` → `production`.
6. Garder Firebase App Distribution pour les builds QA rapides ; utiliser Google
   Play pour les vendeurs/clients afin d'éviter l'installation APK manuelle.

Canaux recommandés :

| Canal | Artefact | Outil | Usage |
|---|---|---|---|
| `staging` | APK release | Firebase App Distribution | QA interne et recette rapide. |
| `pilot` | AAB | Google Play internal/closed | Premiers terminaux clients sans APK manuel. |
| `prod-internal` | AAB | Google Play Internal testing | Validation store avant prod. |
| `prod` | AAB | Google Play Production | MEP clients avec rollout progressif. |

Le workflow Play attendu doit refuser une prod si :

- `versionCode` n'est pas supérieur au dernier build taggé ;
- `api_base_url` ne pointe pas vers l'API prod ;
- `terminal_email_domain` n'est pas le domaine terminal prod ;
- `FIREBASE_AUTH_EMULATOR` n'est pas forcé à `false` ;
- la branche n'est pas `main` ou un tag release approuvé.

---

## Récap secrets & variables

| Nom | Type | Utilisé par |
|---|---|---|
| `FIREBASE_ADMIN_JSON_BASE64` | secret | deploys, distribution mobile |
| `TCH_ANDROID_KEYSTORE_BASE64` | secret | distribution mobile |
| `TCH_ANDROID_KEYSTORE_PASSWORD` | secret | distribution mobile |
| `TCH_ANDROID_KEY_ALIAS` | secret | distribution mobile |
| `TCH_ANDROID_KEY_PASSWORD` | secret | distribution mobile |
| `FIREBASE_ANDROID_APP_ID` | variable | distribution mobile (optionnel) |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` | secret futur | publication Google Play |
| `HCLOUD_TOKEN`, `SSH_PRIVATE_KEY`, `DOPPLER_TOKEN_STG`, … | secret | infra / deploy (voir chaque workflow) |

## Rôles outils externes pour la MEP mobile

| Outil | Rôle prod | Configuration à prévoir |
|---|---|---|
| Firebase Auth | Authentification mobile vendeur/POS. | Projet prod ou app prod dédiée, domaine terminal prod, backend `RUNTIME_IDENTITY_PROVIDER=firebase`, utilisateurs terminaux provisionnés. |
| Firebase App Distribution | Canal staging/QA interne rapide. | Groupes `staging`, `qa`, service account avec rôle App Distribution Admin. |
| Google Play Console | Distribution prod Android professionnelle. | Play App Signing, track `internal`, track `closed`, track `production`, service account release limité. |
| Doppler | Source des secrets runtime serveur/infra par environnement. | Projets/envs staging/prod, secrets backend Firebase/API/DB, tokens de déploiement synchronisés vers GitHub Actions si nécessaire. |
| GitHub Secrets/Variables | Secrets CI/CD et paramètres de build. | Keystore Android, Firebase SA, Google Play SA, app ids, API URLs, domaines terminaux. |
| Cloudflare | DNS/TLS/routage public web/API. | Zones prod, DNS `api`, `test/stg`, Workers/Pages, règles TLS/WAF/cache selon surface. |
| Grafana Cloud | Observabilité prod. | Dashboards API/edge/mobile-adjacent, alertes erreurs auth/sale/latence, logs si pipeline actif. |
