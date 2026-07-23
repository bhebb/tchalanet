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
  **bloquants** ; `SpotBugs` et OWASP restent advisory. PMD ne bloque que sur les
  règles de correctness (priorité 1-4) ; complexité et `UnusedPrivateMethod` sont
  advisory (priorité 5, cf. `tchalanet-server/pmd/ruleset.xml`).
- **Tags d'images immuables.** Les déploiements utilisent `sha-<short_sha>` ;
  `latest` est interdit en staging/prod.

## Vue d'ensemble

| Workflow | Déclenchement | Rôle | Bloquant |
|---|---|---|---|
| `server-pr.yml` | PR `tchalanet-server/**` | build+test, spotless/checkstyle/PMD, OWASP | ✅ gate |
| `web-pr.yml` | PR `tchalanet-web/**` | lint, test, tsc, build | ✅ |
| `mobile-pr.yml` | PR `tchalanet-mobile/**` | `flutter analyze` + `flutter test` | ✅ |
| `edge-pr.yml` | PR `tchalanet-edge-service/**` | lint, test, build | ✅ |
| `infra-check.yml` | PR `tchalanet-infra/**` | env vars + `docker compose config` | ✅ |
| `docs.yml` | push/PR `tchalanet-docs/**` | `mkdocs build --strict` + deploy Pages | ✅ |
| `codeql.yml` | PR (server/web/edge) + hebdo | SAST Java + TS/JS | advisory |
| `full-validation.yml` | nightly 07:20 + manuel | IT, build images (+Trivy), deploy jetable, E2E, perf | 🌙 |
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

Prod = promotion d'un tag déjà validé en staging (`build_*` interdits en prod).

---

## Distribution mobile staging (`mobile-distribute-staging.yml`)

Construit un **APK Android release signé**, pointé sur le backend choisi et le
**vrai Firebase `tchalanet-39115`** (le même projet que admin/web), puis le
publie sur **Firebase App Distribution**. Déclenchement **manuel uniquement**.

### Inputs (valeurs passées au lancement)

| Input | Défaut | Rôle |
|---|---|---|
| `api_base_url` | `https://api.stg.tchalanet.com/api/v1` | **Lien vers l'API** ciblée par le build (`--dart-define=API_BASE_URL`). Inclure `/api/v1`. |
| `terminal_email_domain` | `terminal.tchalanet.local` | Domaine e-mail des logins terminal — **doit correspondre au seeding backend**. |
| `pos_device_binding` | `` (vide) | `POS_DEVICE_BINDING`. Laisser vide sauf si un credential device est requis. |
| `build_name` | `1.0.0-stg` | `versionName` lisible. Le `versionCode` est `github.run_number` (auto-incrément). |
| `tester_groups` | `staging` | Groupes de testeurs App Distribution (alias séparés par virgule). |
| `release_notes` | `Staging build` | Notes affichées aux testeurs. |

> `FIREBASE_AUTH_EMULATOR` est forcé à `false` (vrai Firebase). C'est le but de ce
> workflow ; ne pas le rendre configurable pour éviter les builds incohérents.

### Lancer

**CLI :**

```bash
gh workflow run mobile-distribute-staging.yml \
  -f api_base_url="https://api.stg.tchalanet.com/api/v1" \
  -f terminal_email_domain="terminal.tchalanet.local" \
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
     rôle **Firebase App Distribution Admin** dans `tchalanet-39115`.
2. **Variable repo optionnelle** : `FIREBASE_ANDROID_APP_ID` si l'App ID Android
   change (défaut : celui de `google-services.json`).
3. **Serveur staging** en `RUNTIME_IDENTITY_PROVIDER=firebase` (pas `firebase-emulator`),
   sinon il rejette les vrais tokens produits par le build.
4. **Terminaux staging** provisionnés comme vrais utilisateurs Firebase dans
   `tchalanet-39115`.
5. **Groupe de testeurs** `staging` créé dans la console App Distribution.

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
que ce backend valide les tokens du **vrai** Firebase `tchalanet-39115`.

> Note flotte prod : App Distribution reste un canal **beta** (invitations,
> builds expirant à 30 j). Pour une flotte de terminaux en production, évaluer
> Managed Google Play / MDM (auto-update forcé + mode kiosque).

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
| `HCLOUD_TOKEN`, `SSH_PRIVATE_KEY`, `DOPPLER_TOKEN_STG`, `NEON_API_KEY`, … | secret | infra / deploy (voir chaque workflow) |
