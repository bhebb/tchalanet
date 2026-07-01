# VERSIONS.md — Tchalanet (Source de vérité)

Règle : aucune version (runtime/build/service) ne doit changer sans :

1. mise à jour de ce fichier
2. mise à jour du wrapper correspondant (Maven/Nx/pnpm)
3. mise à jour des images docker (compose/infra)
4. note dans le changelog si impact prod

---

## Sources de vérité (où lire/éditer)

- Backend runtime/build : `tchalanet-server/pom.xml` (+ `./mvnw`)
- Infra images/tags : `tchalanet-infra/envs/common/compose.env` + `compose/*`
- Web (Nx) : `tchalanet-web/package.json` + `tchalanet-web/pnpm-lock.yaml`
- Web runtime config : `tchalanet-web/apps/proxy.conf.cjs` + `tchalanet-web/libs/shared-assets/public/assets/config/runtime.*.json`
- Mobile (Flutter) : `tchalanet-mobile/pubspec.yaml`
- Version pnpm web : `tchalanet-web/package.json#packageManager` + Corepack
- Edge service : `tchalanet-edge-service/package.json`

---

## 1) Backend (tchalanet-server)

- Java : 25 (défini dans `tchalanet-server/pom.xml`)
- Spring Boot : 4.1.0 (parent défini dans `tchalanet-server/pom.xml`)
- Build tool : Maven (wrapper présent : `./mvnw`)
- DB driver : PostgreSQL JDBC (version gérée via BOM ou dépendances Maven)
- Migration : Flyway (utilisé dans le POM)

### Bibliothèques backend (versions dans `tchalanet-server/pom.xml` properties)

| Bibliothèque          | Version |
| --------------------- | ------- |
| Lombok                | 1.18.46 |
| MapStruct             | 1.6.3   |
| QueryDSL              | 5.1.0   |
| Caffeine              | 3.2.4   |
| Firebase Admin        | 9.9.0   |
| ShedLock              | 7.7.0   |
| Testcontainers        | 1.21.4  |
| ArchUnit              | 1.4.2   |
| PDFBox                | 3.0.7   |
| ZXing                 | 3.5.4   |
| Springdoc OpenAPI     | 3.0.3   |
| Jackson 3 (tools.\*)  | 3.1.2   |
| json-schema-validator | 3.0.5   |
| Hibernate             | 7.3.2.Final |
| Spring Modulith       | 2.1.0   |
| Mockito               | 5.23.0  |

### Plugins Maven (versions dans `tchalanet-server/pom.xml` properties)

| Plugin           | Version    |
| ---------------- | ---------- |
| maven-compiler   | 3.15.0     |
| maven-enforcer   | 3.6.2      |
| maven-surefire   | 3.5.6      |
| maven-failsafe   | 3.5.5      |
| jacoco           | 0.8.15     |
| spotless         | 3.7.0      |
| maven-checkstyle | 3.6.0      |
| sonar-maven      | 5.3.0.6276 |

### Capacités backend récentes

- Promotions V0 : moteur basé sur les effets `FREE_GAME_LINE`, `BOOST_ODDS`, `WAIVE_CHARGE`.
- Maryaj gratis V0 : configuration admin dédiée, modes d'attribution `FIXED`, `PER_PAID_AMOUNT`, `TIERED_PAID_AMOUNT`; validation vente de billet encore en cours.
- PageModel admin : navigation tenant avec `Maryaj gratis`, `Lignes gratuites` et `Toutes les promotions`.
- Migrations récentes promotions/admin : `V248`, `V249`, `V250`, `V251`, `V252`, `V253`.

---

## 2) Mobile (Flutter)

- Flutter : 3.44.0 (channel stable)
- Dart : 3.12.0 (bundled avec Flutter 3.44.0)
- App path : `tchalanet-mobile/`
- Target platform : Android (premier)
- Build tool : Flutter CLI + Gradle wrapper généré par Flutter
- Android application id : `com.tchalanet.mobile`
- Ancienne app Ionic/Capacitor : supprimée du workspace Nx par OpenSpec change `migrate-mobile-from-nx-ionic-to-flutter`

### Dépendances Flutter (versions dans `tchalanet-mobile/pubspec.yaml`)

| Package                | Version |
| ---------------------- | ------- |
| cupertino_icons        | ^1.0.9  |
| flutter_riverpod       | ^3.3.1  |
| go_router              | ^17.3.0 |
| dio                    | ^5.9.2  |
| flutter_secure_storage | ^10.3.1 |
| firebase_core          | ^4.10.0 |
| firebase_auth          | ^6.5.2  |
| flutter_lints          | ^6.0.0  |

---

## 3) Web (Nx / Angular)

- Angular : 22.0.4 (voir `tchalanet-web/package.json`)
- Angular Material/CDK : 22.0.x
- Nx : 23.1.0-beta.4
- TypeScript : 6.0.3
- Package manager : pnpm 11.9.0 (voir `packageManager` dans `tchalanet-web/package.json`)
- SSR packages : `@angular/ssr` ~22.0.4 + `express` 4.x pour `public-portal`

### Workspace web actif

- Apps Nx déployables : `public-portal`, `admin-portal`, `platform-portal`.
- App legacy : `tch-portal` supprimée du workspace actif.
- E2E : `web-e2e` couvre les apps web.
- Libs transverses : `api`, `core`, `ui`, `web`, `shared-assets`, `shared-config`, `page-model`, `notifications`, `widgets`.
- Assets partagés : `libs/shared-assets` pour logos, images et i18n réutilisables.
- Profils runtime web : `local-ide`, `local-ide-emulator`, `dev-docker`, `dev-docker-emulator`, `stg-vercel`, `prod-vercel`.
- Routes admin V0 prioritaires : `/app/admin/maryaj-gratis`, `/app/admin/promotions`, `/app/admin/company/appearance`.

### Dépendances principales (versions dans `tchalanet-web/package.json`)

| Package                     | Version  | Purpose                        |
| --------------------------- | -------- | ------------------------------ |
| `@ngrx/store`               | ^21.1.0  | App state management           |
| `@ngrx/effects`             | ^21.1.0  | Side effects (HTTP, i18n)      |
| `@ngrx/router-store`        | ^21.1.0  | Router/store sync              |
| `@ngrx/store-devtools`      | ^21.1.0  | Dev tools                      |
| `@angular/material`         | ^22.0.2  | UI component library           |
| `@angular/cdk`              | ^22.0.2  | Component primitives           |
| `@angular/fire`             | ^20.0.1  | Firebase Angular integration   |
| `@ngx-translate/core`       | ^17.0.0  | i18n runtime                   |
| `@ngx-translate/http-loader`| ^17.0.0  | i18n HTTP loader               |
| `keycloak-angular`          | ^22.0.0  | Keycloak OIDC integration      |
| `keycloak-js`               | ^26.2.4  | Keycloak JS adapter            |
| `@angular/ssr`              | ~22.0.4  | Angular SSR runtime            |
| `express`                   | ^4.21.2  | SSR node server                |
| `firebase`                  | ^12.14.0 | Firebase web SDK               |
| `marked`                    | ^18.0.5  | Markdown rendering             |

### Tooling web

| Package             | Version        |
| ------------------- | -------------- |
| `nx`                | 23.1.0-beta.4  |
| `@nx/angular`       | 23.1.0-beta.4  |
| `@angular/cli`      | ~22.0.4        |
| `@angular/build`    | ~22.0.4        |
| `typescript`        | ~6.0.3         |
| `vitest`            | ^4.0.8         |
| `@playwright/test`  | ^1.36.0        |

Known peer lag after Angular 22 upgrade:

- NgRx 21.x peer-declares Angular 21.
- `@angular/fire` 20.x peer-declares Angular 20 and pulls Angular 20 platform-browser-dynamic peers.
- Module federation tooling peer-declares TypeScript 4/5 while the workspace is on TypeScript 6.

---

## 4) Infra images

Runtime image tags are centralized in `tchalanet-infra/envs/common/compose.env`.

| Image/runtime      | Version/tag |
| ------------------ | ----------- |
| Traefik            | v3.7.0      |
| PostgreSQL         | 18.4        |
| Redis              | 8.6.3       |
| Node (edge-service) | 24 LTS (alpine) |

---

### Notes backend

- `tchalanet-server/pom.xml` exige Java >= 25 via maven-enforcer.
