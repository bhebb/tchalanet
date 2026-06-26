# Mapping Backend ↔ Web — Tchalanet

Source: sortie du script `extract_tchalanet_controller_mapping.py`.

Total endpoints: **185**.

## Résumé par surface

| Surface | Count |
|---|---|
| tenant_admin | 58 |
| platform_admin | 50 |
| tenant_runtime | 23 |
| platform_ops | 19 |
| public | 18 |
| pos_terminal | 14 |
| identity | 3 |

## Résumé par menu cible proposé

| Menu cible | Count |
|---|---|
| Superadmin > Référentiels | 25 |
| Public | 18 |
| Runtime tenant / bootstrap | 18 |
| POS / Terminal vendeur | 17 |
| Admin tenant > Tirages / Résultats | 15 |
| Admin tenant > Contrôles / Configuration | 13 |
| Admin tenant > Paramètres tenant | 12 |
| Superadmin > Opérations > Archives | 8 |
| Superadmin > Opérations > Résultats | 6 |
| Superadmin > Tenants | 5 |
| Admin tenant > Utilisateurs / Accès | 4 |
| Admin tenant > Promotions | 4 |
| Superadmin > Communication | 4 |
| Admin tenant > Notifications | 4 |
| Superadmin > Opérations > Tirages | 4 |
| Identité utilisateur | 3 |
| Superadmin > Opérations > Tâches planifiées | 3 |
| Superadmin > Référentiels > Thèmes | 3 |
| Superadmin > Référentiels > PageModel | 2 |
| Superadmin > Opérations > Cache | 2 |
| Superadmin > Vue d’ensemble | 2 |
| Admin tenant > Vendeurs | 2 |
| Superadmin > Accès | 2 |
| Admin tenant > Rapports | 2 |
| Superadmin > Opérations > Communication technique | 2 |
| Admin tenant > Archives | 1 |
| Superadmin > Opérations > Audit | 1 |
| Admin tenant > Tableau de bord | 1 |
| Superadmin > Opérations > Notifications test | 1 |
| Superadmin > Opérations > Sync identité | 1 |

## Doublons et chevauchements

- Doublons exacts `HTTP method + full_path`: **0**.

- Chevauchements de base path entre plusieurs controllers :

| Base path | Controllers | Endpoints |
|---|---|---|
| /admin/draw-results | DrawResultsController, TenantAdminDrawResultController | 4 |
| /tenant/tickets | TicketQueryController, TicketSalesController | 2 |

### Alias / duplications fonctionnelles notables

- `/tenant/seller-terminal/**` et `/tenant/terminal/**` exposent les mêmes actions `SellerTerminalMeController` (`me`, `changePin`). À garder temporairement seulement si compatibilité mobile/POS nécessaire.

- `/admin/draw-results` est partagé entre lecture core (`DrawResultsController`) et proposition manuelle tenant (`TenantAdminDrawResultController`). Pas un doublon exact, mais à présenter comme une seule page utilisateur “Résultats”.

- `/tenant/tickets` est partagé entre query (`GET`) et vente (`POST`) sur deux controllers. Pas un doublon exact, mais même ressource REST.


## Décisions d’architecture

- Le superadmin peut utiliser les endpoints `/admin/**` avec un tenant id/code dans l’en-tête; le filter construit alors le contexte tenant. Donc pas besoin de dupliquer toutes les actions admin en `/platform/**`.
- `/platform/**` reste pour les actions globales, référentiels globaux, tenants, superadmins et ops techniques.
- `/platform/ops/**` doit rester technique: batch, cache, archive, fetch provider/résultats, génération/open/close/apply multi-tenant.
- Côté UI, toujours afficher clairement le tenant actif quand un superadmin appelle un endpoint `/admin/**`.


## Mapping complet par menu cible


### Superadmin > Vue d’ensemble

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/dashboard | PlatformPageModelController.platformPageModel | platformPageModel | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/overview | PlatformAdminOverviewController.overview | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Tenants

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /platform/tenant-onboarding/preview | TenantProvisioningController.preview | Vente / ticket | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/tenant-onboarding/provision | TenantProvisioningController.provision | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/tenants | TenantAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/tenants | TenantAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/tenants/by-code | TenantAdminController.getByCode | getByCode | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Référentiels

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/catalog/games | GameAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/catalog/games | GameAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/draw-channels | PlatformDrawChannelController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/draw-channels | PlatformDrawChannelController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/draw-channels/{channelId}/games | PlatformDrawChannelGameController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/draw-channels/{channelId}/games | PlatformDrawChannelGameController.upsert | Modifier | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/draw-channels/{channelId}/games/bulk | PlatformDrawChannelGameController.bulkUpsert | bulkUpsert | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/i18n-overrides | PlatformI18nOverridesController.search | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/i18n-overrides | PlatformI18nOverridesController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/i18n-overrides/overview | PlatformI18nOverridesController.overview | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/i18n-overrides/resolve | PlatformI18nOverridesController.resolvePlatform | resolvePlatform | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/page-model-templates | PlatformPageModelTemplateController.search | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/page-model-templates | PlatformPageModelTemplateController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/page-model-templates/visible | PlatformPageModelTemplateController.visible | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/plans | PlanAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/plans | PlanAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/pricing | PricingAdminController.listActive | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/pricing | PricingAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/result-slots | ResultSlotAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/result-slots/active | ResultSlotAdminController.listActive | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/result-slots/{resultSlotId}/calendar | ResultSlotCalendarAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/result-slots/{resultSlotId}/calendar | ResultSlotCalendarAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/settings | PlatformSettingsController.search | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/settings | PlatformSettingsController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/settings/overview | PlatformSettingsController.overview | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Référentiels > Thèmes

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/catalog/theme-presets | ThemeAdminController.listActive | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/catalog/theme-presets | ThemeAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/catalog/theme-presets/overview | ThemeAdminController.overview | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Référentiels > PageModel

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/pagemodels | PageModelAdminController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/pagemodels | PageModelAdminController.create | Créer / soumettre | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Superadmin > Accès

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/super-admins | PlatformSuperAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/super-admins | PlatformSuperAdminController.create | Créer / soumettre | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Communication

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/contact-requests | PlatformContactRequestAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/public-content/news | PlatformPublicContentAdminController.list | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/public-content/news | PlatformPublicContentAdminController.upsert | Modifier | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/public-content/news/force-refresh | PlatformPublicContentAdminController.forceRefresh | forceRefresh | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Opérations > Tirages

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /platform/ops/draws/apply | DrawCalendarOpsController.apply | Opération tirage/résultat | Superadmin Ops technique/global | À vérifier: dépend du tenant context ou tenantCodes selon flow |
| POST | /platform/ops/draws/close-due | DrawCalendarOpsController.closeDue | Opération tirage/résultat | Superadmin Ops technique/global | Garder côté superadmin |
| POST | /platform/ops/draws/generate | DrawCalendarOpsController.generate | Opération tirage/résultat | Superadmin Ops technique/global | Garder côté superadmin |
| POST | /platform/ops/draws/open-today | DrawCalendarOpsController.openToday | Opération tirage/résultat | Superadmin Ops technique/global | Garder côté superadmin |

### Superadmin > Opérations > Résultats

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/ops/draw-results | DrawResultsOpsController.search | Consulter / lister | Superadmin Ops technique/global | Garder côté superadmin |
| GET | /platform/ops/draw-results/by-slot | DrawResultsOpsController.getBySlot | getBySlot | Superadmin Ops technique/global | Garder côté superadmin |
| POST | /platform/ops/draw-results/fetch | DrawResultsOpsController.fetch | Opération tirage/résultat | Superadmin Ops technique/global | Garder côté superadmin |
| POST | /platform/ops/draw-results/manual | DrawResultsOpsController.manual | Opération tirage/résultat | Superadmin Ops technique/global | À vérifier: dépend du tenant header/override; UI doit afficher tenant actif |
| POST | /platform/ops/draw-results/override | DrawResultsOpsController.override | Opération tirage/résultat | Superadmin Ops technique/global | À vérifier: dépend du tenant header/override; UI doit afficher tenant actif |
| POST | /platform/ops/draw-results/refresh | DrawResultsOpsController.refresh | Opération tirage/résultat | Superadmin Ops technique/global | À vérifier: dépend du tenant header/override; UI doit afficher tenant actif |

### Superadmin > Opérations > Tâches planifiées

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/ops/batch/executions | OpsBatchExecutionController.listExecutions | Consulter / lister | Superadmin Ops technique/global | Garder côté superadmin |
| GET | /platform/ops/batch/gates/:effective | OpsBatchGateController.getEffectiveGates | getEffectiveGates | Superadmin Ops technique/global | Garder côté superadmin |
| GET | /platform/ops/batch/jobs | OpsBatchJobController.listJobs | Consulter / lister | Superadmin Ops technique/global | Garder côté superadmin |

### Superadmin > Opérations > Cache

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| DELETE | /platform/ops/cache | CacheOpsController.clearAllCaches | Supprimer / désactiver / archiver | Superadmin Ops technique/global | Garder côté superadmin |
| GET | /platform/ops/cache | CacheOpsController.listCaches | Consulter / lister | Superadmin Ops technique/global | Garder côté superadmin |

### Superadmin > Opérations > Archives

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /platform/archive/objects/invalid | PlatformArchiveController.listInvalidObjects | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/archive/ops-summary | PlatformArchiveController.opsSummary | opsSummary | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/archive/partition-cleanup/execute | PlatformArchiveController.executePartitionCleanup | executePartitionCleanup | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/archive/partition-cleanup/plan | PlatformArchiveController.partitionCleanupPlan | partitionCleanupPlan | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/archive/restore/audit-log | PlatformArchiveController.restoreAuditLog | restoreAuditLog | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/archive/runs | PlatformArchiveController.listRuns | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |
| POST | /platform/archive/runs | PlatformArchiveController.triggerRun | triggerRun | Superadmin sans tenant par défaut | Garder côté superadmin |
| GET | /platform/archive/runs/failed | PlatformArchiveController.listFailedRuns | Consulter / lister | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Opérations > Audit

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /platform/audit/purge | AuditEventRestController.purgeExpiredAuditLogs | Supprimer / désactiver / archiver | Superadmin sans tenant par défaut | Garder côté superadmin |

### Superadmin > Opérations > Communication technique

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /platform/ops/communication/email-test | PlatformCommunicationOpsController.testEmail | testEmail | Superadmin Ops technique/global | Garder côté superadmin |
| POST | /platform/ops/communication/slack-test | PlatformCommunicationOpsController.testSlack | testSlack | Superadmin Ops technique/global | Garder côté superadmin |

### Superadmin > Opérations > Notifications test

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /platform/ops/notifications/test | OpsNotificationController.testNotification | testNotification | Superadmin Ops technique/global | Garder côté superadmin |

### Superadmin > Opérations > Sync identité

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /platform/ops/sync/identity/firebase-bootstrap-users | PlatformIdentitySyncOpsController.triggerFirebaseBootstrapSync | triggerFirebaseBootstrapSync | Superadmin Ops technique/global | Garder côté superadmin |

### Admin tenant > Tableau de bord

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/overview | TenantAdminOverviewController.overview | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Vendeurs

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/seller-terminals | SellerTerminalAdminController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/seller-terminals | SellerTerminalAdminController.create | Créer / soumettre | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Tirages / Résultats

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/draw-results | DrawResultsController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draw-results/last-days | DrawResultsController.listLastDays | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/draw-results/manual | TenantAdminDrawResultController.propose | propose | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draw-results/today | DrawResultsController.listToday | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draws | DrawQueryAdminController.listDraws | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draws/latest-with-results | DrawQueryAdminController.latestWithResults | latestWithResults | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draws/next | DrawQueryAdminController.nextDraws | nextDraws | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draws/today | DrawQueryAdminController.todayDraws | todayDraws | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/draws/upcoming | DrawQueryAdminController.upcomingDraws | upcomingDraws | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/tchala/approve | AdminTchalaController.approve | approve | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/tchala/delete | AdminTchalaController.deleteEntries | Supprimer / désactiver / archiver | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/tchala/import | AdminTchalaController.uploadImport | uploadImport | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/tchala/merge | AdminTchalaController.merge | merge | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/tchala/pending | AdminTchalaController.pending | pending | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/tchala/reject | AdminTchalaController.reject | reject | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Contrôles / Configuration

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/business-days | BusinessDayOverrideController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| PUT | /admin/business-days | BusinessDayOverrideController.upsert | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| PUT | /admin/commission/default-rate | TenantAdminCommissionController.setDefaultRate | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/commission/overview | TenantAdminCommissionController.overview | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/commission/sellers | TenantAdminCommissionController.listSellerCommissions | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/controls/odds | PricingOverrideAdminController.getTenantDefaultOdds | getTenantDefaultOdds | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/games | TenantGameAdminController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/games/catalog | TenantGameAdminController.catalog | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/policies/limits/assignments | LimitPolicyAdminController.listAssignments | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| PUT | /admin/policies/limits/assignments | LimitPolicyAdminController.upsertAssignment | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/policies/limits/rules | LimitPolicyAdminController.listAvailableRules | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/setup/draw-sales-matrix | AdminSetupController.drawSalesMatrix | drawSalesMatrix | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/setup/games-pricing | AdminSetupController.gamesPricing | gamesPricing | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Promotions

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/promotions/campaigns | PromotionCampaignAdminController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/promotions/campaigns | PromotionCampaignAdminController.create | Créer / soumettre | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate | PromotionCampaignAdminController.instantiateDefaultMaryajGratis | Lifecycle promotion | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/promotions/campaigns/{campaignId}/rules | PromotionRuleAdminController.addRule | addRule | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Paramètres tenant

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| PUT | /admin/tenant | AdminTenantController.updateIdentity | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/tenant-config | AdminTenantConfigController.getConfig | getConfig | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/tenant-config/communication | AdminTenantConfigController.getCommunication | getCommunication | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/tenant-config/document | AdminTenantConfigController.getDocument | getDocument | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| PUT | /admin/tenant-config/internal-settings | AdminTenantConfigController.updateSettings | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/tenant/address | AdminTenantController.getAddress | getAddress | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| PUT | /admin/tenant/address | AdminTenantController.upsertAddress | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| DELETE | /admin/theme | TenantThemeAdminController.deactivate | Supprimer / désactiver / archiver | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/theme | TenantThemeAdminController.get | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/theme/preset | TenantThemeAdminController.applyPreset | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/theme/presets | TenantThemeAdminController.listPresets | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| PATCH | /admin/theme/settings | TenantThemeAdminController.updateSettings | Modifier | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Utilisateurs / Accès

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/access-control/permissions | AccessControlAdminController.listPermissions | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/access-control/roles | AccessControlAdminController.listRoles | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/identity/users | IdentityUserAdminController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/identity/users | IdentityUserAdminController.create | Créer / soumettre | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Notifications

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/notifications | AdminNotificationController.list | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| POST | /admin/notifications | AdminNotificationController.create | Créer / soumettre | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/notifications/deliveries | AdminNotificationController.deliveries | deliveries | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |
| GET | /admin/notifications/summary | AdminNotificationController.summary | Consulter / lister | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Archives

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /admin/archive/audit | AdminArchiveController.getArchivedAudit | getArchivedAudit | Tenant admin; Superadmin possible avec header tenant | Garder; utilisable par admin tenant et superadmin avec tenant header |

### Admin tenant > Rapports

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /tenant/reports/sales-by-period-and-game | GetSalesReportByPeriodAndGameController.getSalesReport | getSalesReport | Runtime privé tenant / apps | Garder |
| GET | /tenant/reports/tenant-kpis | GetTenantKpisController.get | Consulter / lister | Runtime privé tenant / apps | Garder |

### Runtime tenant / bootstrap

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /tenant/dashboard | TenantPageModelController.tenantPageModel | tenantPageModel | Runtime privé tenant / apps | Garder |
| GET | /tenant/draw-channels | TenantDrawChannelController.list | Consulter / lister | Runtime privé tenant / apps | Garder |
| GET | /tenant/draw-channels/games | TenantDrawChannelController.gamesMap | gamesMap | Runtime privé tenant / apps | Garder |
| GET | /tenant/games/runtime | TenantGameRuntimeController.runtime | Consulter / lister | Runtime privé tenant / apps | Garder |
| GET | /tenant/me/capabilities | TenantCapabilityController.getMyCapabilities | getMyCapabilities | Runtime privé tenant / apps | Garder |
| GET | /tenant/me/notifications | TenantNotificationController.list | Consulter / lister | Runtime privé tenant / apps | Garder |
| POST | /tenant/me/notifications/archive | TenantNotificationController.archive | Supprimer / désactiver / archiver | Runtime privé tenant / apps | Garder |
| POST | /tenant/me/notifications/read | TenantNotificationController.markRead | markRead | Runtime privé tenant / apps | Garder |
| GET | /tenant/me/notifications/summary | TenantNotificationController.summary | Consulter / lister | Runtime privé tenant / apps | Garder |
| GET | /tenant/me/operational-context | CurrentOperationalContextController.current | current | Runtime privé tenant / apps | Garder |
| GET | /tenant/me/profile | CurrentUserProfileController.me | Consulter / lister | Runtime privé tenant / apps | Garder |
| PATCH | /tenant/me/profile | CurrentUserProfileController.updateProfile | Modifier | Runtime privé tenant / apps | Garder |
| POST | /tenant/me/profile/bootstrap | CurrentUserProfileController.bootstrap | bootstrap | Runtime privé tenant / apps | Garder |
| GET | /tenant/runtime | TenantRuntimeController.runtime | Consulter / lister | Runtime privé tenant / apps | Garder |
| GET | /tenant/runtime/state | PrivateBootstrapRuntimeController.privateState | privateState | Runtime privé tenant / apps | Garder |
| GET | /tenant/settings/resolve | TenantSettingsController.resolve | Consulter / lister | Runtime privé tenant / apps | Garder |
| GET | /tenant/subscription | SubscriptionController.getMySubscription | getMySubscription | Runtime privé tenant / apps | Garder |
| GET | /tenant/theme/runtime | PublicThemeRuntimeController.runtime | Consulter / lister | Runtime privé tenant / apps | Garder |

### POS / Terminal vendeur

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| GET | /tenant/cashier/draws/available | PosDrawsController.available | available | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| GET | /tenant/cashier/games/available | PosGamesController.available | available | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| GET | /tenant/cashier/home | PosHomeController.mobileHome | mobileHome | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| GET | /tenant/cashier/readiness | PosHomeController.readiness | readiness | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| GET | /tenant/cashier/tickets | PosTicketsController.list | Consulter / lister | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| POST | /tenant/cashier/tickets/preview | PosTicketsController.preview | Vente / ticket | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| POST | /tenant/cashier/tickets/sell | PosTicketsController.sell | Vente / ticket | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| GET | /tenant/cashier/tickets/stats | PosTicketsController.stats | Vente / ticket | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| POST | /tenant/cashier/tickets/verify | PosTicketsController.verify | Vente / ticket | POS seller-terminal | Garder POS; vocabulaire Cashier à renommer SellerTerminal plus tard |
| POST | /tenant/sales/preparations | SalePreparationController.prepare | Créer / soumettre | Runtime privé tenant / apps | Garder |
| GET | /tenant/seller-terminal/me | SellerTerminalMeController.me | Consulter / lister | POS seller-terminal | Garder |
| POST | /tenant/seller-terminal/me/change-pin | SellerTerminalMeController.changePin | Modifier | POS seller-terminal | Garder |
| GET | /tenant/seller-terminal/operational-context | CurrentOperationalContextController.current | current | POS seller-terminal | Garder |
| GET | /tenant/terminal/me | SellerTerminalMeController.me | Consulter / lister | POS seller-terminal | Garder |
| POST | /tenant/terminal/me/change-pin | SellerTerminalMeController.changePin | Modifier | POS seller-terminal | Garder |
| GET | /tenant/tickets | TicketQueryController.list | Consulter / lister | Runtime privé tenant / apps | Garder |
| POST | /tenant/tickets | TicketSalesController.sell | Vente / ticket | Runtime privé tenant / apps | Garder |

### Public

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /public/contact-requests | PublicContactRequestController.submit | Créer / soumettre | Public web/mobile | Garder |
| GET | /public/draw-results/history | PublicDrawResultController.history | history | Public web/mobile | Garder |
| GET | /public/draw-results/latest | PublicDrawResultController.latest | latest | Public web/mobile | Garder |
| GET | /public/i18n | PublicI18nRuntimeController.getBundle | getBundle | Public web/mobile | Garder |
| POST | /public/identity/reset-password | IdentityPasswordResetController.resetPassword | resetPassword | Public web/mobile | Garder |
| GET | /public/managers | PublicManagersController.resolve | Consulter / lister | Public web/mobile | Garder |
| GET | /public/news | PublicNewsController.listPublicNews | Consulter / lister | Public web/mobile | Garder |
| GET | /public/page | PublicPageModelController.resolve | Consulter / lister | Public web/mobile | Garder |
| GET | /public/security/backend-signing-keys | BackendPublicKeysController.listActiveKeys | Consulter / lister | Public web/mobile | Garder |
| GET | /public/settings | PublicSettingsRuntimeController.getPublicSettings | getPublicSettings | Public web/mobile | Garder |
| GET | /public/tchala/by-dream | PublicTchalaController.byDream | byDream | Public web/mobile | Garder |
| GET | /public/tchala/by-number | PublicTchalaController.byNumber | byNumber | Public web/mobile | Garder |
| GET | /public/tchala/search | PublicTchalaController.search | Consulter / lister | Public web/mobile | Garder |
| POST | /public/tchala/suggestions | PublicTchalaController.submitSuggestion | submitSuggestion | Public web/mobile | Garder |
| GET | /public/tchala/suggestions/status | PublicTchalaController.suggestionStatus | suggestionStatus | Public web/mobile | Garder |
| GET | /public/tenant/runtime | TenantRuntimeController.publicRuntime | publicRuntime | Public web/mobile | Garder |
| GET | /public/theme/runtime | PublicThemeRuntimeController.publicRuntime | publicRuntime | Public web/mobile | Garder |
| POST | /public/tickets/verify | TicketVerifyController.verify | Vente / ticket | Public web/mobile | Garder |

### Identité utilisateur

| Method | Path | Controller.method | Action UI | Usage | Décision |
|---|---|---|---|---|---|
| POST | /identity/me/complete-first-login | IdentityActivationController.completeFirstLogin | completeFirstLogin | Identité / first login | Garder |
| GET | /identity/users | IdentityUserCrudController.search | Consulter / lister | Identité / first login | Garder |
| POST | /identity/users | IdentityUserCrudController.create | Créer / soumettre | Identité / first login | Garder |

## Points à corriger / vérifier

1. **Platform Ops Provider**: aucun endpoint `/platform/ops/providers` dans l’extraction. Si la page existe côté web, il faut créer ou mapper vers les slots/résultats existants.
2. **Tenant context sur Platform Ops**: les actions `apply`, `refresh`, `override`, `manual` doivent être validées avec le modèle “superadmin + tenant header” ou être converties en multi-tenant explicite.
3. **Vocabulaire POS**: plusieurs endpoints gardent `cashier` dans le path/tag (`/tenant/cashier/**`). Pour V0 SELLER_TERMINAL, garder si compatibilité nécessaire, sinon prévoir alias/migration de vocabulaire.
4. **Archives**: endpoints backend `/platform/archive/**` existent, mais la nav indique `ops/archives`. Le web doit router `Opérations > Archives` vers les endpoints réels ou prévoir un proxy/service.
5. **Promotions**: endpoints `/admin/promotions/**` existent; menu admin tenant déjà correct. Ne pas placer dans Ops Platform.
