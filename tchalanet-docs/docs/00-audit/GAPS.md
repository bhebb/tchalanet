# Gaps — Ce qui manque pour MVP Phase 0

> Analyse au 2026-04-24. Référence : `openspec/context/` + audit existant.

---

## 1. Couverture MVP Phase 0

### Auth (Keycloak / IAM)

| Item                                                    | Statut  | Effort | Notes                                                                         |
| ------------------------------------------------------- | ------- | ------ | ----------------------------------------------------------------------------- |
| Keycloak 26 configuré en infra                          | PRÉSENT | —      | Docker Compose + workflow `realm-manage.yml`                                  |
| `AuthService` + `authGuard` + `roleGuard` Angular       | PRÉSENT | —      | `libs/shared/auth` complet                                                    |
| `AuthCallbackComponent` + callback OIDC                 | PRÉSENT | —      |                                                                               |
| JWT resource server Spring Security                     | PRÉSENT | —      | `spring-boot-starter-oauth2-resource-server`                                  |
| `TchContextFilter` (résolution tenant/scope depuis JWT) | PRÉSENT | —      | RLS flow opérationnel                                                         |
| IAM Roles/Permissions en base                           | PRÉSENT | —      | `V6__core_identity_access.sql` + seed V32                                     |
| Gestion utilisateurs tenant-admin (CRUD Keycloak)       | PARTIEL | M      | `TenantAdminUsersController` existe mais implémentation OpenSpec #82 en cours |
| Sync utilisateur Keycloak → `AppUserJpaEntity`          | PARTIEL | M      | `AppUserJpaEntity` existe, flow de sync à confirmer                           |

### Tirages (Draws)

| Item                                            | Statut         | Effort | Notes                                                                              |
| ----------------------------------------------- | -------------- | ------ | ---------------------------------------------------------------------------------- |
| `DrawJpaEntity` + schéma V7                     | PRÉSENT        | —      |                                                                                    |
| `DrawAdminController`                           | PRÉSENT        | —      | CRUD draws admin                                                                   |
| `DrawResultsController` + `DrawResultJpaEntity` | PRÉSENT        | —      |                                                                                    |
| `PublicDrawResultController`                    | PRÉSENT        | —      | Résultats publics                                                                  |
| Page Angular `/app/tirages`                     | PRÉSENT (stub) | M      | `DrawsPage` existe mais contenu à implémenter                                      |
| Scheduler tirage automatique                    | PARTIEL        | L      | `DrawCalendarOpsController` + `OpsBatch*` existent ; logique automation à vérifier |
| Résultats publics visibles web                  | PARTIEL        | S      | Route `/api/public/draws/results` présente ; widget front à connecter              |

### Vente de tickets

| Item                                            | Statut         | Effort | Notes                                                      |
| ----------------------------------------------- | -------------- | ------ | ---------------------------------------------------------- |
| `TicketEntity` + `TicketLineEntity` + schéma V9 | PRÉSENT        | —      |                                                            |
| `TicketController` + `SellTicketCommandHandler` | PRÉSENT        | —      | Core sales complet côté domain                             |
| `PublicTicketController`                        | PRÉSENT        | —      | Vérification ticket public                                 |
| `SalesSessionController`                        | PRÉSENT        | —      | Session POS / caissier                                     |
| `LimitPolicy` + `DrawExposure`                  | PRÉSENT        | —      | V14 + V19, `ApplySaleExposureHandlerTest`                  |
| Page Angular `/app/tickets`                     | PRÉSENT (stub) | M      | `TicketsPage` existe mais contenu minimal                  |
| Vente de ticket depuis le POS Flutter           | PARTIEL        | L      | App Flutter présente, intégration POS API à confirmer      |
| Ledger des transactions                         | PRÉSENT        | —      | `LedgerEntryJpaEntity` + handlers Deposit/Withdraw/Reverse |
| Paiement et payout                              | PRÉSENT        | —      | `PayoutJpaEntity` + `PayoutAdminController`                |

### Web public

| Item                         | Statut         | Effort | Notes                                                        |
| ---------------------------- | -------------- | ------ | ------------------------------------------------------------ |
| Shell Angular + routing      | PRÉSENT        | —      | `PublicShellComponent`, `PrivateShellComponent`              |
| Home page publique           | PRÉSENT        | —      | `HomePublicPage`, `PublicHomeController`                     |
| Plans / Pricing page         | PRÉSENT        | —      | `PlansPage`, `PlanAdminController`                           |
| Pages légales (Markdown)     | PRÉSENT        | —      | `MarkdownPageComponent` + routes                             |
| Vérification ticket public   | PRÉSENT (stub) | S      | Route `/verify` stub — `PublicTicketController` backend OK   |
| Page model runtime           | PARTIEL        | M      | `PageModelController` × 2 doublons, à nettoyer avant usage   |
| SEO / meta tags              | PARTIEL        | S      | `PublicStubPageComponent` avec titleKey/descKey — à vérifier |
| Résultats de tirages publics | PARTIEL        | S      | Widget à connecter à `PublicDrawResultController`            |
| News publiques               | PRÉSENT        | —      | `PublicNewsController` + `AdminNewsController`               |

---

## 2. Violations bloquantes

> Format : `fichier — convention violée — correction — priorité`

### BLOQUANT (empêche le démarrage ou viole des garanties système)

| #   | Fichier                                            | Violation                                                                     | Correction requise                                                                                                                 |
| --- | -------------------------------------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `V16__processed_event.sql` + `V16__stats_draw.sql` | Deux migrations Flyway avec le même numéro de version V16                     | Renommer `V16__stats_draw.sql` → `V46__stats_draw.sql` (numéro libre après V44) et ajuster si des dépendances de séquence existent |
| 2   | `V22__core_tenantconfig.sql` + `V22__tchala.sql`   | Deux migrations Flyway avec le même numéro de version V22                     | Renommer `V22__tchala.sql` → `V47__tchala.sql`                                                                                     |
| 3   | `UserPreferenceRestRepository.java`                | `@RepositoryRestResource(exported=true)` — expose les entités JPA directement | Supprimer ou remplacer par un controller standard                                                                                  |
| 4   | `PayoutRestRepository.java`                        | `@RepositoryRestResource(exported=true)`                                      | Supprimer                                                                                                                          |
| 5   | `features/pagemodel/PageModelRepository.java`      | `@RepositoryRestResource(exported=true)`                                      | Supprimer, migrer vers repository standard                                                                                         |

### IMPORTANT (violation de convention, non bloquant au démarrage)

| #   | Fichier                                                                                              | Violation                                                                                        | Correction requise                                                        |
| --- | ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------- |
| 6   | `LedgerEntry.java` (domain)                                                                          | `UUID.randomUUID()` dans le domain model                                                         | Remplacer par TypedId wrapper ou factory pattern                          |
| 7   | `LimitPolicyRuntimeService.java` (×2)                                                                | `@Autowired(required=false)` field injection                                                     | Migrer vers constructor injection avec `@Nullable` ou Optional            |
| 8   | `DrawMapper.java`                                                                                    | `@Autowired protected DrawChannelMapper` field injection                                         | Constructor injection                                                     |
| 9   | `LedgerRepositoryAdapter.java`                                                                       | `findByTenantIdAndOccurredAtBetween(tenantId.value(), ...)` — filtre tenant en Java, bypasse RLS | Vérifier si la table `ledger_entry` a une RLS policy — sinon en créer une |
| 10  | `TicketController.java`                                                                              | `return ResponseEntity.*` au lieu de `ApiResponse<T>`                                            | Migrer vers `ApiResponse`                                                 |
| 11  | `PublicTicketController.java`                                                                        | `return ResponseEntity.*`                                                                        | Migrer                                                                    |
| 12  | `TenantThemeController.java`                                                                         | `return ResponseEntity.noContent()`                                                              | Migrer                                                                    |
| 13  | `InsecureJwtDecoderConfig.java`                                                                      | `import javax.net.ssl.*` — code probablement de dev/debug                                        | Supprimer si non utilisé en prod, ou utiliser alternative moderne         |
| 14  | `features/pagemodel_backup/`                                                                         | Dead code (`_backup`)                                                                            | Supprimer le répertoire entier                                            |
| 15  | `features/pagemodel/PageModelController.java` + `features/pagemodelruntime/PageModelController.java` | Deux controllers pour la même route `/api/v1/public/pagemodel`                                   | Unifier en un seul, supprimer le doublon                                  |
| 16  | `features/tenantadmin/policies/TenantAdminPoliciesLimitsController.java` (×2)                        | Deux classes portant le même nom dans des sous-packages différents                               | Supprimer l'une, unifier                                                  |
| 17  | `PricingAdminController.java`                                                                        | `${tch.web.paths.admin:/api/v1/admin}/pricing` — valeur par défaut avec `/v1`                    | Configurer la valeur par défaut sans `/v1`                                |
| 18  | `features/pagemodelruntime/PageModelController.java`                                                 | Route hardcodée `/api/v1/public/pagemodel`                                                       | Utiliser la propriété de config                                           |
| 19  | `TenantGameView.java`, `UpdatePolicyRequest.java`, `DrawChannelResponse.java`                        | `@Data` Lombok                                                                                   | Remplacer par `@Value`, `@Builder`, ou `record`                           |
| 20  | `StatsEventLogEntity.java`, `StatsDrawEntity.java`, `StatsDailyEntity.java`                          | `@Data` Lombok sur des entités JPA                                                               | Remplacer par getters/setters explicites ou `@Getter`/`@Setter` séparés   |

### MINEUR (dette technique, pas de violation directe)

| #   | Observation                                                                                              | Impact                                 |
| --- | -------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| 21  | 13 tests pour 26 domaines core — couverture < 10%                                                        | Risque régressions sur refactoring     |
| 22  | QueryDSL présent mais non documenté dans CLAUDE.md                                                       | Incohérence documentation vs réalité   |
| 23  | `core/haiti` utilise UUID dans domain/application layer                                                  | Typed IDs non respectés sur ce domaine |
| 24  | `features/pagemodel/PageModelEntity.java` duplique probablement `core/pagemodel/PageModelJpaEntity.java` | Source of truth ambiguë                |

---

## 3. Dette technique

### Code dupliqué

- `PageModelController` : 2 implémentations pour la même route public pagemodel
- `TenantAdminPoliciesLimitsController` : 2 classes portant le même nom
- `PageModelEntity` dans features/ + `PageModelJpaEntity` dans core/ (probable doublon)

### Patterns incohérents

- Mix `ApiResponse<T>` / `ResponseEntity` dans les controllers (3 controllers hors convention)
- Mix typed IDs / UUID brut : `core/ledger` et `core/haiti` utilisent UUID dans des couches où ce n'est pas autorisé
- `LedgerRepositoryAdapter` bypasse RLS avec `findByTenantId*` — conception différente du reste du projet sans documentation

### Tests manquants critiques

- 0 test sur `core/sales` (handler `SellTicketCommand`) — domaine le plus critique
- 0 test sur `core/draw` — orchestration des tirages
- 0 test sur `core/payout` — gestion des gains
- 0 test sur `core/ledger` — toutes les transactions financières
- 0 test d'intégration (Testcontainers) sur aucun flux end-to-end

### Architecture en cours de migration

- `features/pagemodel` et `features/pagemodelruntime` coexistent — migration incomplète
- `features/pagemodel_backup` — backup non nettoyé après refactoring
