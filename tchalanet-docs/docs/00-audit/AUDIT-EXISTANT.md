# Audit de l'existant — Tchalanet

> Généré le 2026-04-24. Source : analyse statique du code source.

---

## 1. Backend — tchalanet-server

### 1.1 Packages Java et rôle réel

| Couche      | Package                         | Contenu                                                                                                                                                                                                                |
| ----------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `common/`   | `com.tchalanet.server.common`   | Infrastructure transverse : RLS (`TchContextFilter`, `RlsAwareDataSource`), bootstrap tenant/user, types (`TchId`, `ApiResponse`), configuration Spring, batch utils                                                   |
| `catalog/`  | `com.tchalanet.server.catalog`  | Données de référence immuables : `drawchannel`, `game`, `i18n`, `pagemodeltemplate`, `plan`, `pricing`, `resultslot`, `settings`, `theme`, `tenant`                                                                    |
| `core/`     | `com.tchalanet.server.core`     | 26 domaines métier (hexagonal + CQRS) — voir §1.3                                                                                                                                                                      |
| `features/` | `com.tchalanet.server.features` | BFF / orchestration : `news`, `notifications`, `ops`, `pagemodel`, `pagemodel_backup`_, `pagemodelruntime`_, `platformadmin`, `privatedashboard`, `publicdraw`, `publichome_back`, `reporting`, `stats`, `tenantadmin` |

_\* Code mort / en cours de migration — voir §1.6_

---

### 1.2 Entités JPA existantes

**Catalog (11 entités)**

| Entité                    | Table probable        |
| ------------------------- | --------------------- |
| `DrawChannelEntity`       | `draw_channel`        |
| `DrawChannelGameEntity`   | `draw_channel_game`   |
| `GameJpaEntity`           | `game`                |
| `I18nOverrideEntity`      | `i18n_override`       |
| `PageModelTemplateEntity` | `page_model_template` |
| `PlanJpaEntity`           | `plan`                |
| `PricingOddsEntity`       | `pricing_odds`        |
| `ResultSlotJpaEntity`     | `result_slot`         |
| `SettingEntity`           | `setting`             |
| `TenantRegistryJpaEntity` | `tenant`              |
| `ThemePresetJpaEntity`    | `theme_preset`        |

**Core (29 entités)**

| Entité                                                                          | Domaine         |
| ------------------------------------------------------------------------------- | --------------- |
| `AppRoleEntity`, `AppRolePermissionEntity`, `PermissionEntity`                  | `accesscontrol` |
| `AddressJpaEntity`                                                              | `address`       |
| `AuditEventJpaEntity`, `TchRevisionEntity`                                      | `audit`         |
| `AutonomyPolicyRuleJpaEntity`                                                   | `autonomy`      |
| `DrawJpaEntity`                                                                 | `draw`          |
| `DrawResultJpaEntity`                                                           | `drawresult`    |
| `TchalaEntryJpaEntity`, `TchalaEntryNumberJpaEntity`                            | `haiti`         |
| `LedgerEntryJpaEntity`                                                          | `ledger`        |
| `LimitDefinitionJpaEntity`, `LimitAssignmentJpaEntity`, `DrawExposureJpaEntity` | `limitpolicy`   |
| `OutletEntity`                                                                  | `outlet`        |
| `PageModelJpaEntity`                                                            | `pagemodel`     |
| `PayoutJpaEntity`                                                               | `payout`        |
| `TerminalJpaEntity`                                                             | `pos`           |
| `TicketEntity`, `TicketLineEntity`                                              | `sales`         |
| `SalesSessionJpaEntity`, `SalesSessionTotalsJpaEntity`                          | `session`       |
| `SubscriptionJpaEntity`                                                         | `subscription`  |
| `TenantGameJpaEntity`                                                           | `tenantgame`    |
| `TenantThemeJpaEntity`                                                          | `tenanttheme`   |
| `TenantUserJpaEntity`                                                           | `tenantuser`    |
| `AppUserJpaEntity`, `UserPreferenceJpaEntity`                                   | `user`          |

**Features (5 entités)**

| Entité                                                       | Feature                           |
| ------------------------------------------------------------ | --------------------------------- |
| `NotificationEntity`                                         | `notifications/shared`            |
| `PageModelEntity`                                            | `pagemodel` (dupliquée avec core) |
| `StatsDailyEntity`, `StatsDrawEntity`, `StatsEventLogEntity` | `stats/aggregates`                |

---

### 1.3 Migrations Flyway

**42 migrations présentes (V1 → V51, non continues)**

| Version | Nom                                                                 | Catégorie            |
| ------- | ------------------------------------------------------------------- | -------------------- |
| V1      | extensions_and_functions                                            | Infrastructure       |
| V2      | core_tenant_and_address                                             | Schema + RLS partiel |
| V3      | core_settings                                                       | Schema               |
| V4      | core_outlet_table                                                   | Schema               |
| V5      | core_terminal                                                       | Schema               |
| V6      | core_identity_access                                                | Schema               |
| V7      | core_game_draw                                                      | Schema               |
| V8      | core_pos                                                            | Schema               |
| V9      | core_ticket                                                         | Schema               |
| V10     | core_theme                                                          | Schema               |
| V11     | core_audit                                                          | Schema               |
| V12     | feature_pagemodel                                                   | Schema               |
| V13     | core_payout                                                         | Schema               |
| V14     | draw_exposure                                                       | Schema               |
| V15     | ledger_entry                                                        | Schema               |
| **V16** | **processed_event**                                                 | Schema ⚠ CONFLIT     |
| **V16** | **stats_draw**                                                      | Schema ⚠ CONFLIT     |
| V17     | stats_event_log                                                     | Schema               |
| V18     | core_autonomy_policy_rule                                           | Schema               |
| V19     | core\_\_limit_definitions_and_assignments                           | Schema               |
| V20     | stats_daily                                                         | Schema               |
| V21     | core_billing                                                        | Schema               |
| **V22** | **core_tenantconfig**                                               | Schema ⚠ CONFLIT     |
| **V22** | **tchala**                                                          | Schema ⚠ CONFLIT     |
| V30–V38 | Seeds (tenant, plans, IAM, theme, draws, outlets, settings, tchala) | Data                 |
| V40     | rls_policies                                                        | RLS ✅               |
| V41     | rls_policies_mixed_globals                                          | RLS ✅               |
| V42     | spring_batch_schema                                                 | Schema               |
| V43     | audit_table                                                         | Schema               |
| V44     | idempotency                                                         | Schema               |
| V50     | core_address                                                        | Schema               |
| V51     | pagemodel_template_logicalid_unique                                 | Contrainte           |

> ⚠️ **BLOQUANT** — V16 et V22 ont chacun **deux fichiers de migration** portant le même numéro de version. Flyway refuse de démarrer dans ce cas.

---

### 1.4 Endpoints exposés

**65 controllers au total**

| Scope    | Controller                                  | Route principale                      |
| -------- | ------------------------------------------- | ------------------------------------- |
| catalog  | `PlatformDrawChannelController`             | `/api/platform/draw-channels`         |
| catalog  | `PlatformDrawChannelGameController`         | `/api/platform/draw-channels/games`   |
| catalog  | `TenantDrawChannelController`               | `/api/tenant/draw-channels`           |
| catalog  | `GameAdminController`                       | `/api/platform/games`                 |
| catalog  | `PlatformI18nOverridesController`           | `/api/platform/i18n`                  |
| catalog  | `PlatformPageModelTemplateController`       | `/api/platform/pagemodel-templates`   |
| catalog  | `PlanAdminController`                       | `/api/platform/plans`                 |
| catalog  | `PricingAdminController`                    | `/api/v1/admin/pricing` ⚠ v1 hardcodé |
| catalog  | `ResultSlotAdminController`                 | `/api/platform/result-slots`          |
| catalog  | `PlatformSettingsController`                | `/api/platform/settings`              |
| catalog  | `TenantSettingsController`                  | `/api/tenant/settings`                |
| catalog  | `ThemeAdminController`                      | `/api/platform/themes`                |
| common   | `CacheAdminController`                      | `/api/platform/cache`                 |
| core     | `AccessControlAdminController`              | `/api/platform/access-control`        |
| core     | `AuditEventRestController`                  | `/api/audit`                          |
| core     | `DrawAdminController`                       | `/api/draws`                          |
| core     | `DrawResultsController`                     | `/api/draw-results`                   |
| core     | `AdminTchalaController`                     | `/api/admin/tchala`                   |
| core     | `PublicTchalaController`                    | `/api/public/tchala`                  |
| core     | `OutletReportController`                    | `/api/outlets/report`                 |
| core     | `PageModelAdminController`                  | `/api/pagemodel`                      |
| core     | `PayoutAdminController`                     | `/api/payouts`                        |
| core     | `PublicTicketController`                    | `/api/public/tickets`                 |
| core     | `TicketController`                          | `/api/tickets`                        |
| core     | `SalesSessionController`                    | `/api/pos/sessions`                   |
| core     | `SalesSessionTotalsController`              | `/api/pos/sessions/totals`            |
| core     | `SubscriptionController`                    | `/api/subscriptions`                  |
| core     | `TenantAdminController`                     | `/api/tenant-admin`                   |
| core     | `TenantGameAdminController`                 | `/api/tenant/games`                   |
| core     | `TenantThemeController`                     | `/api/tenant/theme`                   |
| core     | `TenantUserAdminController`                 | `/api/tenant-admin/users`             |
| core     | `UserAdminController`                       | `/api/platform/users`                 |
| core     | `ProfileController`                         | `/api/me`                             |
| features | `AdminNewsController`                       | `/api/admin/news`                     |
| features | `PublicNewsController`                      | `/api/public/news`                    |
| features | `ListMyNotificationsController`             | `/api/notifications`                  |
| features | `MarkAllNotificationsReadController`        | `/api/notifications/read-all`         |
| features | `MarkNotificationReadController`            | `/api/notifications/{id}/read`        |
| features | `DrawCalendarOpsController`                 | `/api/ops/draws/calendar`             |
| features | `DrawResultsOpsController`                  | `/api/ops/draws/results`              |
| features | `OpsBatchExecutionController`               | `/api/ops/batch/executions`           |
| features | `OpsBatchGateController`                    | `/api/ops/batch/gate`                 |
| features | `OpsBatchJobController`                     | `/api/ops/batch/jobs`                 |
| features | `PageModelController` (x2!)                 | `/api/v1/public/pagemodel` ⚠ doublon  |
| features | `PageModelAdminController`                  | `/api/pagemodel-admin`                |
| features | `PlatformAdminI18nGlobalController`         | `/api/platform-admin/i18n`            |
| features | `PlatformAdminOverviewController`           | `/api/platform-admin/overview`        |
| features | `PlatformAdminSettingsGlobalController`     | `/api/platform-admin/settings`        |
| features | `PlatformAdminThemeController`              | `/api/platform-admin/theme`           |
| features | `PrivateDashboardController`                | `/api/dashboard`                      |
| features | `PublicDrawResultController`                | `/api/public/draws/results`           |
| features | `PublicHomeController`                      | `/api/public/home`                    |
| features | `GetOutletPerformanceReportController`      | `/api/reporting/outlets`              |
| features | `GetSalesReportByPeriodAndGameController`   | `/api/reporting/sales`                |
| features | `GetTenantKpisController`                   | `/api/reporting/kpis`                 |
| features | `TenantAdminI18nController`                 | `/api/tenant-admin/i18n`              |
| features | `TenantAdminIdentityController`             | `/api/tenant-admin/identity`          |
| features | `TenantAdminSettingsController`             | `/api/tenant-admin/settings`          |
| features | `TenantAdminConfigOverviewController`       | `/api/tenant-admin/config`            |
| features | `TenantAdminOutletsController`              | `/api/tenant-admin/outlets`           |
| features | `TenantAdminPoliciesLimitsController` (x2!) | ⚠ doublon de classe                   |
| features | `TenantAdminPoliciesAutonomyController`     | `/api/tenant-admin/policies/autonomy` |
| features | `TenantAdminPoliciesController`             | `/api/tenant-admin/policies`          |
| features | `TenantAdminTerminalsController`            | `/api/v1/tenant-admin/terminals` ⚠ v1 |
| features | `TenantAdminUsersController`                | `/api/tenant-admin/users`             |

---

### 1.5 Tests existants

**14 fichiers dans `src/test/`** (dont 1 utilitaire non-test)

| Fichier                                 | Type     | Couverture                                                                             |
| --------------------------------------- | -------- | -------------------------------------------------------------------------------------- |
| `ArchitectureTest.java`                 | ArchUnit | Isolation catalog interne, catalog/game ↔ core/tenantgame, ApiResponse sur controllers |
| `FeatureArchitectureTest.java`          | ArchUnit | Règles features layer                                                                  |
| `AddressCacheNamesTest.java`            | Unitaire | Cache names constants                                                                  |
| `PricingCacheNamesTest.java`            | Unitaire | Cache names constants                                                                  |
| `ResultSlotCatalogTest.java`            | Unitaire | Catalog logic                                                                          |
| `ResultSlotJpaAdapterTest.java`         | Unitaire | JPA adapter                                                                            |
| `BatchTchContextBinderTest.java`        | Unitaire | Batch context                                                                          |
| `TenantBootstrapLookupTest.java`        | Unitaire | Tenant lookup                                                                          |
| `TenantCodeToContextInfoCacheTest.java` | Unitaire | Cache lookup                                                                           |
| `TimeProviderTest.java`                 | Unitaire | Time utility                                                                           |
| `ApplySaleExposureHandlerTest.java`     | Unitaire | Limit policy                                                                           |
| `DrawCutoffRuleTest.java`               | Unitaire | Draw rule                                                                              |
| `TicketLinePreparationServiceTest.java` | Unitaire | Sales domain service                                                                   |

**Estimation couverture** : < 10% — 26 core domains avec 3 tests métier dédiés. Aucun test d'intégration (Testcontainers), aucun test de controller, aucun test de command handler.

---

### 1.6 Dépendances pom.xml — versions vs CLAUDE.md

| Dépendance                      | Version pom.xml     | Attendu CLAUDE.md                                               | Statut |
| ------------------------------- | ------------------- | --------------------------------------------------------------- | ------ |
| Spring Boot                     | 4.0.1               | 4.0.1                                                           | ✅     |
| Java                            | 25 (via toolchain)  | 25                                                              | ✅     |
| Lombok                          | présent             | attendu                                                         | ✅     |
| MapStruct                       | présent             | attendu                                                         | ✅     |
| Flyway                          | spring-boot-managed | via Boot 4                                                      | ✅     |
| QueryDSL                        | présent             | non mentionné                                                   | ℹ️     |
| Hibernate Envers                | présent             | non mentionné                                                   | ℹ️     |
| `spring-boot-starter-data-rest` | présent             | **interdit** (`@RepositoryRestResource` explicitement interdit) | ❌     |

---

## 2. Frontend — apps/ + libs/

### 2.1 Apps et libs Nx présentes

**4 apps** (+ e2e) — **30 projets Nx au total**

```
apps/
├─ tchalanet-web          ← SPA Angular 20
├─ tchalanet-web-e2e      ← Playwright E2E
├─ tchalanet-mobile       ← Flutter POS
└─ tchalanet-mobile-e2e

libs/
├─ shared/
│  ├─ auth                ← Keycloak OIDC (authGuard, roleGuard, AuthService)
│  ├─ api                 ← HTTP client, interceptors
│  ├─ config              ← environments, AppConfig
│  ├─ types               ← types TypeScript partagés
│  ├─ facades             ← facades NgRx
│  ├─ feature             ← feature clients (API clients par feature)
│  ├─ analytics
│  ├─ data-access/i18n    ← NgRx store i18n
│  ├─ data-access/page    ← NgRx store page + navigation
│  ├─ data-access/session ← NgRx store session
│  └─ utils/              ← layout, constants, i18n loader
├─ ui/
│  ├─ theme               ← CSS variables, design tokens
│  ├─ styles              ← styles globaux SCSS
│  ├─ layout              ← Shell layout, NotFoundComponent
│  ├─ widget-renderer     ← moteur de rendu page model
│  ├─ breadcrumb          ← fil d'Ariane
│  └─ plans               ← composant tarifs
└─ web/
   ├─ shell               ← PublicShellComponent, MarkdownPageComponent
   ├─ public-pages        ← pages publiques + PUBLIC_ROUTES
   ├─ private-pages       ← pages privées (dashboard, tickets, tirages, rapports)
   ├─ feature-home-public ← page d'accueil publique
   ├─ feature-home-private← page d'accueil privée
   ├─ widgets             ← widgets page model
   └─ constants           ← constantes
```

**i18n/** : 1 lib (traductions fr/en/ht)

### 2.2 Composants Angular existants

**Standalone : 46 composants** — 100% standalone, 0 NgModule ✅

Pages privées : `DashboardPage`, `DashboardContainerComponent`, `SuperAdminDashboardPage`, `TenantAdminDashboardPage`, `CashierDashboardPage`, `AdminPage`, `TicketsPage`, `DrawsPage`, `ReportsPage`, `ProfilePage`, `PrivateShellComponent`

Pages publiques : `HomePublicPage`, `PlansPage`, `FeaturesPage`, `PublicStubPageComponent`, `MarkdownPageComponent`

Auth : `AuthLoginComponent`, `AuthCallbackComponent`

### 2.3 État NgRx configuré

**3 feature stores actifs** avec actions / reducers / effects / selectors complets :

| Store                                 | Feature               | Contenu                             |
| ------------------------------------- | --------------------- | ----------------------------------- |
| `sessionFeature`                      | `data-access/session` | Session utilisateur, claims JWT     |
| `navAfterLoadFeature` + `pageFeature` | `data-access/page`    | Navigation page model, état pages   |
| `i18nFeature`                         | `data-access/i18n`    | Langue active, traductions chargées |

NgRx router-store : non vérifié — à confirmer dans `app.config.ts`.

### 2.4 Pages et routes existantes

**Routes publiques** (path `''`) :

- `/` — Home public
- `/pricing` — Plans
- `/features` — Features
- `/verify` — Vérification ticket (stub)
- `/ticket/:code` — Ticket public (stub)
- `/explanations`, `/official-reports`, `/security`, `/tchala` — Pages Markdown
- `/legal/regulation`, `/legal/responsible`, `/legal/privacy` — Légal
- `/support` — Support
- `/auth/login` — Login Keycloak
- `/auth/callback` — Callback OIDC
- `/404` — Not Found

**Routes privées** (path `app/`, protégé par `authGuard`) :

- `/app/dashboard` → redirige par rôle (super-admin / tenant-admin / cashier)
- `/app/tickets`
- `/app/tirages`
- `/app/rapports`
- `/app/gestion`
- `/app/profile`

---

## 3. Infrastructure

### 3.1 Services Docker Compose configurés

**9 services modulaires** dans `compose/` :

| Service         | Fichier                           | Usage               |
| --------------- | --------------------------------- | ------------------- |
| API Spring Boot | `docker-compose-api.yml`          | Backend principal   |
| Traefik v3.6.5  | `docker-compose-traefik.yml`      | Reverse proxy       |
| PostgreSQL 18.1 | `docker-compose-postgres.yml`     | Base de données     |
| Redis 8.4       | `docker-compose-redis.yml`        | Cache               |
| Keycloak 26     | `docker-compose-keycloak.yml`     | Auth                |
| Unleash 7.4     | `docker-compose-unleash.yml`      | Feature flags       |
| Unleash (host)  | `docker-compose-unleash-host.yml` | Unleash DB          |
| Doppler         | `docker-compose-doppler.yml`      | Secrets             |
| Projet          | `docker-compose-project.yml`      | Composition globale |

Envs : `common/` (versions partagées), `dev/`, `staging/`, `prod/`

### 3.2 CI/CD GitHub Actions

**11 workflows présents** :

| Workflow                | Rôle                           |
| ----------------------- | ------------------------------ |
| `build-and-publish.yml` | Build + push image GHCR        |
| `check-envs-pr.yml`     | Validation env vars sur PR     |
| `deploy-prod.yml`       | Déploiement production Hetzner |
| `docs.yml`              | Build + publish MkDocs         |
| `infra.yml`             | Déploiement infra              |
| `publish-images.yml`    | Publication images Docker      |
| `realm-manage.yml`      | Gestion realm Keycloak         |
| `scan-security.yml`     | Scan sécurité                  |
| `server_dev.yml`        | CI backend dev                 |
| `server_prod.yml`       | CI backend prod                |
| `web.yml`               | CI frontend                    |

Pipeline complet staging → prod via GHCR → Hetzner. Secrets via Doppler uniquement.

### 3.3 Variables d'environnement référencées

Gérées exclusivement via Doppler (pas de `.env` committé). Variables critiques : `DATABASE_URL`, `KEYCLOAK_*`, `REDIS_URL`, `MAILGUN_*`, `BIRD_*`, `UNLEASH_*`.

---

## 4. Vérification conventions CLAUDE.md

| Convention                                    | Statut           | Détail                                                                                                                                                             |
| --------------------------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Constructor injection (pas @Autowired champs) | ❌ **VIOLÉE**    | 4 fichiers : `LimitPolicyRuntimeService.java` (×2), `DrawMapper.java`                                                                                              |
| `jakarta.*` (pas `javax.*`)                   | ⚠️ **PARTIELLE** | 6 fichiers — `javax.sql.DataSource` (JSE, pas d'équivalent jakarta), `InsecureJwtDecoderConfig` (javax.net.ssl — code dev à supprimer)                             |
| Typed IDs (pas UUID brut hors persistence)    | ❌ **VIOLÉE**    | `LedgerEntry.java` : `UUID.randomUUID()` dans le domain model ; core `haiti` : UUID dans domain/application                                                        |
| Pas de `WHERE tenant_id` en Java              | ⚠️ **PARTIELLE** | `LedgerRepositoryAdapter` : `findByTenantIdAndOccurredAtBetween(tenantId.value(), ...)` — le ledger passe le tenantId explicitement au lieu de laisser RLS filtrer |
| `ddl-auto=validate`                           | ✅ **RESPECTÉE** | `application.yaml` : `ddl-auto: validate`                                                                                                                          |
| `ApiResponse<T>` sur controllers              | ⚠️ **PARTIELLE** | `TicketController`, `PublicTicketController`, `TenantThemeController` : utilisent `ResponseEntity` directement (3 controllers)                                     |
| Standalone components Angular                 | ✅ **RESPECTÉE** | 46 composants standalone, 0 NgModule                                                                                                                               |
| Control flow moderne `@if`/`@for`             | ✅ **RESPECTÉE** | 41 usages `@if`/`@for`, 0 `*ngIf`/`*ngFor`                                                                                                                         |
| RLS configuré                                 | ✅ **RESPECTÉE** | V40 + V41 : policies RLS définies                                                                                                                                  |
| `@RepositoryRestResource` absent              | ❌ **VIOLÉE**    | 3 fichiers : `UserPreferenceRestRepository`, `PayoutRestRepository`, `PageModelRepository`                                                                         |
| Pas de `/api/v1` hardcodé                     | ⚠️ **PARTIELLE** | `PageModelController` (pagemodelruntime + backup), `TenantAdminTerminalsController`                                                                                |
| `@Data` Lombok absent                         | ❌ **VIOLÉE**    | 6 fichiers : `TenantGameView`, `UpdatePolicyRequest`, `DrawChannelResponse` (core), `StatsEventLogEntity`, `StatsDrawEntity`, `StatsDailyEntity` (features/stats)  |
| Flyway sans conflits de version               | ❌ **BLOQUANT**  | V16 × 2 fichiers, V22 × 2 fichiers — empêche le démarrage                                                                                                          |
| Dead code absent                              | ❌ **VIOLÉE**    | `features/pagemodel_backup/`, `PageModelController` × 2 classes, `TenantAdminPoliciesLimitsController` × 2 classes                                                 |
