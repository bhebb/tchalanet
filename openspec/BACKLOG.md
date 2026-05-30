# Backlog observations — entrées pour spec transverse

> Fichier de collecte. Rempli au fil des analyses de code et de docs.  
> Chaque item est une observation brute : glitch, incohérence, trou, ou dette.  
> Source pour le prochain spec transverse.

---

## Convention de nommage near-code

✅ Établie, documentée dans `doc-policy.md` et dans l'ARCHITECTURE.md de `tchalanet-server` (TODO).

| Slice | Préfixe near-code | Couverture |
|---|---|---|
| `tchalanet-core` | `DOMAIN_*.md` | ✅ 17 packages couverts |
| `tchalanet-catalog` | `CATALOG_*.md` | ✅ 6 packages couverts |
| `tchalanet-platform` | `PLATFORM_*.md` | ✅ 13 packages couverts |
| `tchalanet-features` | `FEATURE_*.md` | ✅ 8 features couvertes |

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

✅ Tous les flows référencés dans `role-flows.md` sont créés.

---

## Audit core — écarts corrigés (2026-05-30)

| Doc | Écart trouvé | Statut |
|---|---|---|
| `DOMAIN_PAYOUT.md` | Contenu `# Domaine Ledger` (stub) en tête du fichier payout — deux docs fusionnés par erreur. Statuts incorrects (`PARTIALLY_PAID` inexistant). `PayoutClaimStatus`, `PayoutStatus`, `PayoutClaimSource` absents. Commandes (Block/Unblock/Execute/Approve/Reject) absentes. Queries réconciliation absentes | ✅ Réécrit |
| `DOMAIN_SESSION.md` | `SalesSessionStatus` = OPEN/CLOSED seulement — `CANCELLED` et `FINALIZED` présents dans le code mais absents du doc. `ValidateSalesSessionForOperationQuery` (query critique pour les opérations POS) absent | ✅ Corrigé |
| `DOMAIN_TERMINAL.md` | Doc mentionne uniquement `TerminalStatus` (internal, 6 valeurs) — `TerminalState` (api/, 5 valeurs, exposé dans `TerminalView`) absent et non documenté. Distinction interne/api non expliquée | ✅ Corrigé |
| `DOMAIN_DRAWRESULT.md` | Headers emoji (🎯, 🧱, 🔥…) incohérents avec la convention markdown du projet. Contenu correct mais formaté comme note de design plutôt que doc normatif | ✅ Converti + restructuré |

### tchalanet-core — bloqué par active changes

| Domaine | Bloqué par |
|---|---|
| `core.seller` | Change `tchalanet-commercial-network-v1` |
| `core.promotion` | Change `tchalanet-commercial-network-v1` + `tchalanet-promotion-integration-pack` |

### Stubs à enrichir quand domaine stabilisé

| Fichier | Niveau actuel | Action |
|---|---|---|
| `DOMAIN_OUTLET.md` | Stub (CRUD basique, pas d'enums, pas de statuts) | Enrichir : OutletStatus, OutletCapabilities, OutletSettingsView |
| `DOMAIN_ANALYTICS.md` | Stub (responsabilité décrite, implémentation TODO) | Enrichir à la livraison des KPI dashboards |
| `DOMAIN_AUTONOMY.md` | Stub (vision + runtime model, pas d'implémentation) | Enrichir quand autonomy intégré dans sell/payout workflow |
| `core/ledger/DOMAIN_LEDGER.md` | Stub — même contenu était dupliqué en tête de DOMAIN_PAYOUT.md (erreur corrigée). Domaine non encore implémenté | Enrichir quand ledger livré |

---

## Liens stale dans les docs existants

| Fichier | Problème |
|---|---|
| `FEATURE_CASHIER.md` | `print-ticket/` est un dossier — le lien devrait pointer vers `print-ticket/00-index.md` |

---

## Outils référencés non implémentés

| Outil | Référencé dans | Description |
|---|---|---|
| `pnpm docs:check` | `doc-policy.md` | Validation automatique — jamais implémenté |

---

## Openspec changes offlinesync — deux changes actives

`add-offlinesync` et `add-offlinesync-module` couvrent le même domaine.  
Les deux ont des tâches ouvertes — ce sont des specs actives, pas des archives.  
Le `DOMAIN_OFFLINESYNC.md` dans les changes est un doc de conception (pas un near-code dupliqué).  
Source canonique near-code : `tchalanet-core/.../core/offlinesync/DOMAIN_OFFLINESYNC.md`.  
**Action à la livraison** : vérifier si les deux changes peuvent être fusionnées avant merge.

---

## tchalanet-infra — gaps documentaires

Audit 2026-05-30 :

| Constat | Impact |
|---|---|
| Pas d'`docs/ARCHITECTURE.md` | Pas de vue globale de l'infra (services, réseau, volumes) |
| Pas de `docs/conventions/` | Pas de règles formalisées (nommage, env vars, volumes) |
| 15 docs opérationnels (~3800 lignes), datés nov 2025 | Probablement stales sur certains détails |
| Overlap entre `DEPLOYMENT.md` et `OPERATIONS.md` | Deux sources pour le même sujet |
| `DEMARRAGE.md` — état "100% opérationnel" nov 2025 | Potentiellement périmé |
| `mobile-distribution-v0.md` — "v0" dans le nom | À renommer ou archiver si supersédé |

**À créer :**
- `tchalanet-infra/docs/ARCHITECTURE.md` — services Docker, réseau Traefik, volumes, Keycloak, Doppler
- `tchalanet-infra/docs/conventions/` — conventions env vars, secrets, nommage services

---

## Features — contenu à vérifier

| Feature | Statut |
|---|---|
| `FEATURE_REPORTING.md` | Existe — contenu à vérifier |
| `FEATURE_STATS.md` | Existe — contenu à vérifier |
| `FEATURE_PUBLICDRAWRESULTS.md` | Existe — contenu à vérifier |
| `FEATURE_OPS.md` | Existe — contenu à vérifier |
| `FEATURE_TENANTADMIN_OFFLINE.md` | Existe — probablement stub |

---

## Docs obsolètes / à ranger (tchalanet-server/docs/)

| Fichier | Statut actuel | Action recommandée |
|---|---|---|
| `01_sell_handler_services_matrix.html` | HTML généré (probablement 2025) — pas de versioning | Vérifier si à jour ; si stale, archiver ou supprimer |
| `02_events_producers_consumers.html` | idem | idem |
| `03_batches_jobs_matrix.html` | idem | idem |
| `OFFLINE_MODE_FUNCTIONAL_TECHNICAL_DESIGN.md` | ✅ marqué SUPERSEDED (2026-05-30) | Conserver pour histoire, ne plus référencer comme source |
| `RFC_CORE_ARCHITECTURE_INTENSITE_VARIABLE.md` | ✅ marqué ARCHIVED (2026-05-30) | Conserver pour histoire |
| `PLATFORM_TEMPLATE.md` | Template platform — pas de CATALOG_TEMPLATE ni FEATURE_TEMPLATE | Créer templates manquants si besoin |

---

## Observations mineures

- `tchalanet-server/docs/ARCHITECTURE.md` — à mettre à jour avec la convention nommage near-code (DOMAIN/CATALOG/PLATFORM/FEATURE)
- `tchalanet-web/.prettierignore` et `.prettierrc` modifiés sur `chore/ai-agent-setup` — vérifier si intentionnel
- `SELLER_GUIDE.md` en FR uniquement — version créole haïtien à prévoir (V2 si app bilingue)
