# Backlog observations — entrées pour spec transverse

> Fichier de collecte. Rempli au fil des analyses de code et de docs.  
> Chaque item est une observation brute : glitch, incohérence, trou, ou dette.  
> Source pour le prochain spec transverse.

---

## Convention de nommage near-code (établie, à documenter formellement)

| Slice | Préfixe near-code | Statut |
|---|---|---|
| `tchalanet-core` | `DOMAIN_*.md` | ✅ En place |
| `tchalanet-catalog` | `CATALOG_*.md` | ✅ Renommé 2026-05-30 |
| `tchalanet-platform` | `PLATFORM_*.md` | ✅ En place |
| `tchalanet-features` | `FEATURE_*.md` | ✅ En place |

À ajouter dans `doc-policy.md` §Near-code docs et dans l'ARCHITECTURE.md de `tchalanet-server`.

---

## Audit platform — écarts corrigés (2026-05-30)

| Doc | Écart trouvé | Statut |
|---|---|---|
| `PLATFORM_TENANTCONFIG.md` | API décrivait `get(key)` / `getAll()` au lieu de `createTenant`, `getTenantById`, etc. — modèle TenantConfigView absent, TenantStatus/TenantType absents, interfaces secondaires (TenantZoneApi, TenantLocaleApi, TenantBusinessCalendarApi) absentes | ✅ Réécrit |
| `PLATFORM_TENANTTHEME.md` | Décrivait "overrides couleurs/logos" + `ThemeOverride.java` inexistant — API réelle : `applyTenantTheme(presetCode)`, `TenantThemeView` avec presetCode/version | ✅ Corrigé |
| `PLATFORM_PUBLICCONTENT.md` | Décrivait "CGU, mentions légales, `getContent(ContentKey)`" — API réelle : `listPublicHomeNews/TenantAdminDashboardNews/etc`, `PublicContentSurface` (4 valeurs), `PublicContentStatus` DRAFT/PUBLISHED/ARCHIVED | ✅ Réécrit |
| `PLATFORM_IDENTITY.md` | API `resolveUser`/`getCurrentUser` incorrect — réel : `bootstrapCurrentUser`, `getUserProfile`, `findAppUser`, `countTenantUsers`. `UserStatus`/`TenantUserStatus`/`AutonomyLevel`/`ClientSurface` tous absents | ✅ Corrigé |
| `PLATFORM_AUDIT.md` | `AuditAction` (80+ valeurs), `AuditEntityType`, `AuditActorType`, `@AuditLog` annotation absents | ✅ Corrigé |
| `PLATFORM_DOCUMENT.md` | `DocumentKind`/`DocumentFormat`/`PaperSize`/`LineStyle`/`AssetKind` enum values absents | ✅ Corrigé |
| `PLATFORM_ENTITLEMENT.md` | `EntitlementKeys` constants absents (4 features + 4 limites), `TenantPlanStatus` absent | ✅ Corrigé |
| `PLATFORM_TENANTGAME.md` | API `listActiveGames`/`getGameSettings` incorrect — réel : `enableTenantGame`/`disableTenantGame`/`resolveTenantGames`/`updateTenantGamePolicy` | ✅ Corrigé |

### Découverte : `catalog.tenant` non documenté

`TenantStatus` et `TenantType` vivent dans `tchalanet-catalog/catalog/tenant/api/model/` — pas dans `tchalanet-platform`.  
Ce package (`catalog.tenant`) n'a pas de `CATALOG_*.md` et n'était pas dans l'inventaire.

---

## Audit catalog — écarts corrigés (2026-05-30)

| Doc | Écart trouvé | Statut |
|---|---|---|
| `CATALOG_DRAWCHANNEL.md` | `DrawSource` n'avait que 2 valeurs (AUTO/OPS) au lieu de 8 (SYSTEM, AUTO, EXTERNAL, US_LOTTERY, NY_OPEN_DATA, FL_APIM, MANUAL, ADMIN_OVERRIDE) | ✅ Corrigé |
| `CATALOG_DRAWCHANNEL.md` | `findByCode` → nom réel `findByTenantAndCode` | ✅ Corrigé |
| `CATALOG_DRAWCHANNEL.md` | API incomplète : `listGamesByChannel`, `listChannelGames`, `search` manquants | ✅ Corrigé |
| `CATALOG_DRAWCHANNEL.md` | `DrawChannelCalendarRow` : `salesOpenTime` et `dependsOnChannelId` absents | ✅ Corrigé |
| `CATALOG_GAME.md` | `HT_MARYAJ_GRATUIT` (gamecode pour FREE_GAME_LINE promotion) absent | ✅ Corrigé |
| `CATALOG_GAME.md` | `stats()` et `listRecent(int)` absents de l'API publique | ✅ Corrigé |
| `CATALOG_PRICING.md` | `PricingView` record et `stats()` absents | ✅ Corrigé |
| `CATALOG_RESULTSLOT.md` | `getByKey` → nom réel `findByKey` | ✅ Corrigé |
| `CATALOG_RESULTSLOT.md` | `findById` et `stats()` absents | ✅ Corrigé |
| `CATALOG_RESULTSLOT.md` | `sourceCfg` et `projectionCfg` (champs critiques) absents de `ResultSlotView` | ✅ Corrigé |
| `CATALOG_RESULTSLOT.md` | `ResultSlotCalendarCatalog` et `ResultSlotCalendarOverrideView` (avec invariant XOR) absents | ✅ Corrigé |
| `CATALOG_THEME.md` | **Critique** : doc décrivait `platform.tenanttheme` (DRAFT/PUBLISHED/ARCHIVED) au lieu de `catalog.theme` (ThemePreset global) | ✅ Réécrit |

---

## Flows manquants (role-flows.md les référence, fichiers absents)

✅ Tous les flows sont créés — liens corrigés dans `role-flows.md`.

---

## DOMAIN_*.md manquants

### tchalanet-platform — aucun DOMAIN_*.md dans les 13 packages

| Package | Chemin |
|---|---|
| `platform.identity` | `tchalanet-platform/.../identity/` |
| `platform.tenantconfig` | `tchalanet-platform/.../tenantconfig/` |
| `platform.accesscontrol` | `tchalanet-platform/.../accesscontrol/` |
| `platform.idempotence` | `tchalanet-platform/.../idempotence/` |
| `platform.notification` | `tchalanet-platform/.../notification/` |
| `platform.communication` | `tchalanet-platform/.../communication/` |
| `platform.audit` | `tchalanet-platform/.../audit/` |
| `platform.address` | `tchalanet-platform/.../address/` |
| `platform.document` | `tchalanet-platform/.../document/` |
| `platform.entitlement` | `tchalanet-platform/.../entitlement/` |
| `platform.publiccontent` | `tchalanet-platform/.../publiccontent/` |
| `platform.tenantgame` | `tchalanet-platform/.../tenantgame/` |
| `platform.tenanttheme` | `tchalanet-platform/.../tenanttheme/` |

Tous ont des `PLATFORM_*.md` enrichis (infrastructure/config) mais pas de `DOMAIN_*.md` near-code (vocabulaire, états, invariants, events).

### tchalanet-core — en attente d'active changes

| Domaine | Bloqué par |
|---|---|
| `core.seller` | Change `tchalanet-commercial-network-v1` |
| `core.promotion` | Change `tchalanet-commercial-network-v1` + `tchalanet-promotion-integration-pack` |

### tchalanet-core — incohérence à corriger après spec

| Fichier | Problème |
|---|---|
| `core/terminal/DOMAIN_TERMINAL.md` | `TerminalStatus` dans le doc vs `TerminalState` dans le code — attente de `terminal-reorg-security` |

---

## Liens stale dans les docs existants

| Fichier | Ligne | Problème |
|---|---|---|
| `role-flows.md` | L41-44 | `seller-onboarding.md` et `terminal-binding.md` existent maintenant mais marqués `*(TODO)*` |
| `role-flows.md` | L63-65, L109 | Liens vers flows seller/terminal/session encore en forme `flow/` sans nom de fichier |
| `FEATURE_CASHIER.md` | Références | `print-ticket/` est un dossier (`00-index.md`, `01-backend.md`, etc.) — le lien devrait pointer vers `print-ticket/00-index.md` |

---

## Outils référencés non implémentés

| Outil | Référencé dans | Description |
|---|---|---|
| `pnpm docs:check` | `tchalanet-docs/docs/00-guidelines/doc-policy.md` | Validation automatique des conventions doc — jamais implémenté |

---

## Duplications / ambiguïtés à résoudre

| Observation | Localisation |
|---|---|
| `openspec/changes/add-offlinesync/` et `openspec/changes/add-offlinesync-module/` — deux changes pour le même domaine offlinesync | `openspec/changes/` |
| `DOMAIN_OFFLINESYNC.md` existe à 3 endroits : `changes/add-offlinesync/`, `changes/add-offlinesync-module/docs/`, `core/offlinesync/` — seul le near-code est canonique, les deux autres sont des artefacts de changes non nettoyés | `tchalanet-core/core/offlinesync/DOMAIN_OFFLINESYNC.md` = source |

---

## Features sans FEATURE_*.md

| Feature package | Fichier existant | Contenu actuel |
|---|---|---|
| `features.reporting` | `FEATURE_REPORTING.md` ✅ | À vérifier si à jour |
| `features.stats` | `FEATURE_STATS.md` ✅ | À vérifier si à jour |
| `features.publicdrawresults` | `FEATURE_PUBLICDRAWRESULTS.md` ✅ | À vérifier si à jour |
| `features.ops` | `FEATURE_OPS.md` ✅ | À vérifier si à jour |

---

## Couverture mobile docs

| Fichier | Statut |
|---|---|
| `tchalanet-mobile/docs/ARCHITECTURE.md` | Existe ✅ |
| `tchalanet-mobile/docs/conventions/README.md` | Existe ✅ |
| `tchalanet-mobile/docs/OFFLINE.md` | Existe ✅ |
| Conventions spécifiques mobile (state management, navigation, providers) | Stub seulement — TODO quand règles stabilisées dans le code |

---

## Audit tchalanet-infra (non terminé)

Phase 3 a supprimé les archives de `tchalanet-infra`. L'audit du contenu documentaire (est-ce que les docs existants sont à jour, utiles, ou à supprimer) n'a pas été fait.

---

## Observations mineures

- `FEATURE_TENANTADMIN_OFFLINE.md` existe mais est probablement un stub — vérifier contenu avant PR
- `tchalanet-web/.prettierignore` et `.prettierrc` modifiés sur `chore/ai-agent-setup` — vérifier si intentionnel ou vestige
- `SELLER_GUIDE.md` créé en FR — si l'app est bilingue (FR/HT), une version en créole haïtien sera nécessaire (V2)
