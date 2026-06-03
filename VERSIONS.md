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
- Mobile (Flutter) : `tchalanet-mobile/pubspec.yaml`
- Version pnpm : `package.json#packageManager` + Corepack
- Edge service : `tchalanet-edge-service/package.json`

---

## 1) Backend (tchalanet-server)

- Java : 25 (défini dans `tchalanet-server/pom.xml`)
- Spring Boot : 4.0.3 (parent défini dans `tchalanet-server/pom.xml`)
- Build tool : Maven (wrapper présent : `./mvnw`)
- DB driver : PostgreSQL JDBC (version gérée via BOM ou dépendances Maven)
- Migration : Flyway (utilisé dans le POM)

### Bibliothèques backend (versions dans `tchalanet-server/pom.xml` properties)

| Bibliothèque          | Version |
| --------------------- | ------- |
| Lombok                | 1.18.42 |
| MapStruct             | 1.6.3   |
| QueryDSL              | 5.1.0   |
| Caffeine              | 3.2.3   |
| Keycloak admin        | 26.0.9  |
| ShedLock              | 6.6.0   |
| Testcontainers        | 1.21.4  |
| ArchUnit              | 1.4.1   |
| PDFBox                | 3.0.7   |
| ZXing                 | 3.5.3   |
| Springdoc OpenAPI     | 2.8.6   |
| Jackson 3 (tools.\*)  | 3.1.1   |
| json-schema-validator | 3.0.2   |

### Plugins Maven (versions dans `tchalanet-server/pom.xml` properties)

| Plugin           | Version    |
| ---------------- | ---------- |
| maven-compiler   | 3.15.0     |
| maven-enforcer   | 3.6.2      |
| maven-surefire   | 3.5.5      |
| maven-failsafe   | 3.5.5      |
| jacoco           | 0.8.14     |
| spotless         | 2.44.5     |
| maven-checkstyle | 3.6.0      |
| sonar-maven      | 5.3.0.6276 |

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
| flutter_appauth        | ^8.0.0  |
| flutter_lints          | ^6.0.0  |

---

## 3) Web (Nx / Angular)

- Angular : 21.x (voir `tchalanet-web/package.json`)
- Nx : workspace géré via `tchalanet-web/`
- Package manager : pnpm (voir `packageManager` dans `tchalanet-web/package.json`)

### Dépendances principales (versions dans `tchalanet-web/package.json`)

| Package                     | Version  | Purpose                        |
| --------------------------- | -------- | ------------------------------ |
| `@ngrx/store`               | ^21.1.0  | App state management           |
| `@ngrx/effects`             | ^21.1.0  | Side effects (HTTP, i18n)      |
| `@ngrx/router-store`        | ^21.1.0  | Router/store sync              |
| `@ngrx/store-devtools`      | ^21.1.0  | Dev tools                      |
| `@angular/material`         | ^21.2.13 | UI component library           |
| `@angular/cdk`              | ^21.2.13 | Component primitives           |
| `@ngx-translate/core`       | ^17.0.0  | i18n runtime                   |
| `@ngx-translate/http-loader`| ^17.0.0  | i18n HTTP loader               |
| `keycloak-angular`          | ^21.0.0  | Keycloak OIDC integration      |

---

## 5) Infra images

Runtime image tags are centralized in `tchalanet-infra/envs/common/compose.env`.

| Image/runtime      | Version/tag |
| ------------------ | ----------- |
| Traefik            | v3.7.0      |
| PostgreSQL         | 18.4        |
| Redis              | 8.6.3       |
| Keycloak upstream  | 26.6.2      |
| Tchalanet Keycloak | keycloak-26.6.2 |
| Node (edge-service) | 24 LTS (alpine) |

---

### Notes backend

- `tchalanet-server/pom.xml` exige Java >= 25 via maven-enforcer.
- Certains modules (ex: `tchalanet-infra/keycloak/tchalanet-keycloak-provider/pom.xml`) sont actuellement configurés pour Java 21.
  - Toute homogénéisation Java 25 doit passer par une PR dédiée + build complet.
