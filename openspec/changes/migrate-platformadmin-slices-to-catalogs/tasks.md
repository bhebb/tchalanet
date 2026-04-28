## 1. Enrichir PlatformI18nOverridesController (catalog/i18n/internal/web/)

- [ ] 1.1 Injecter `I18nOverridesCatalog` si pas déjà présent (vérifier : il est déjà injecté ✅)
- [ ] 1.2 Ajouter `GET /overview` avec `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` niveau méthode → appel `i18nOverridesCatalog.keyStats()`, retour `ApiResponse` avec record inline `I18nGlobalOverviewView(Instant generatedAt, Summary summary)` + `Summary(long totalKeys, long totalLocales, long totalOverrides)`
- [ ] 1.3 Ajouter `GET /resolve` avec `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` niveau méthode, `@RequestParam String locale`, `@RequestParam(required = false) TenantId tenantId` → brancher sur `resolveLocale(locale)` ou `resolveLocaleForTenant(locale, tenantId)`, retour `ApiResponse<Map<String, String>>`
- [ ] 1.4 Vérifier que l'endpoint existant `GET /resolve/{locale}` (contexte tenant) n'est pas impacté

## 2. Enrichir PlatformSettingsController (catalog/settings/internal/web/)

- [ ] 2.1 Ajouter `SettingsCatalog` dans les dépendances injectées (aux côtés de `SettingsAdminService`)
- [ ] 2.2 Ajouter `GET /overview` → appel `settingsCatalog.stats()`, retour `ApiResponse<SettingsCatalogStatsView>` (réutiliser le record existant)
- [ ] 2.3 Vérifier : `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau classe déjà présent ✅

## 3. Enrichir ThemeAdminController (catalog/theme/internal/web/)

- [ ] 3.1 Ajouter `ThemeCatalog` dans les dépendances injectées (aux côtés de `ThemePresetAdminService`)
- [ ] 3.2 Ajouter `GET /overview` → appel `themeCatalog.stats()`, retour `ApiResponse<ThemePresetStatsView>` (réutiliser le record existant)
- [ ] 3.3 Ajouter `GET /` → appel `themeCatalog.listActive()`, retour `ApiResponse<List<ThemePresetView>>`
- [ ] 3.4 Vérifier : `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau classe déjà présent ✅

## 4. Suppression des slices features/platformadmin

- [ ] 4.1 Supprimer `features/platformadmin/i18nglobal/PlatformAdminI18nGlobalController.java`
- [ ] 4.2 Supprimer le répertoire `features/platformadmin/i18nglobal/`
- [ ] 4.3 Supprimer `features/platformadmin/settingsglobal/PlatformAdminSettingsGlobalController.java`
- [ ] 4.4 Supprimer `features/platformadmin/settingsglobal/PlatformAdminSettingsGlobalOrchestrator.java`
- [ ] 4.5 Supprimer `features/platformadmin/settingsglobal/SettingsGlobalOverviewView.java`
- [ ] 4.6 Supprimer le répertoire `features/platformadmin/settingsglobal/`
- [ ] 4.7 Supprimer `features/platformadmin/theme/PlatformAdminThemeController.java`
- [ ] 4.8 Supprimer le répertoire `features/platformadmin/theme/`

## 5. Documentation

- [ ] 5.1 Créer `tchalanet-server/src/main/java/com/tchalanet/server/features/platformadmin/FEATURE_PLATFORM_ADMIN.md`
  - Documenter la slice `overview/` (composite cross-catalog — reste dans features)
  - Indiquer que `i18nglobal`, `settingsglobal`, `theme` ont été migrés vers `catalog/<bc>/internal/web/`
- [ ] 5.2 Vérifier que `tchalanet-server/docs/ARCHITECTURE.md` décrit correctement que `platformadmin` n'est plus qu'une feature `overview` composite

## 6. Validation

- [ ] 6.1 Exécuter `./mvnw verify` — build vert sans erreurs de compilation
- [ ] 6.2 Vérifier qu'aucune référence à `features/platformadmin/i18nglobal`, `settingsglobal`, `theme` ne subsiste dans le classpath
- [ ] 6.3 Vérifier les 3 nouveaux endpoints (`/overview`, `/resolve`) dans les controllers catalog
