# Cache — Analyse des écarts & proposition de TTL

> **Status**: AUDIT / PROPOSAL (non normatif)
> **Scope**: tchalanet-server (common / core / catalog / platform / app)
> **Date**: 2026-07-07
> **Référence normative**: [`cache.md`](./cache.md)
> **But**: mesurer l'écart entre la politique `cache.md` et l'implémentation réelle, puis
> proposer une politique de TTL cohérente, calée sur la **fréquence réelle de modification**
> de chaque entité.

---

## 0. TL;DR

1. La **politique** `cache.md` reste valide sur le fond. Le **document** est partiellement
   périmé (liens morts) et surtout **non appliqué** : nommage et déclaration des TTL divergent.
2. **~20 caches** (tout `catalog` + `platform.tenant`) n'ont **aucun `CacheSpecProvider`** et
   tournent sur les défauts génériques **5 min (L1) / 60 min (L2)** — en violation de `cache.md §8`.
   Résultat : des référentiels quasi-statiques (plans, thèmes, page models) expirent aussi vite
   qu'une donnée de séance, et des données de séance ne sont pas cadrées par leur cycle métier.
3. **Bug fonctionnel actif** : `catalog:drawchannel:calendar_rows` est mis en cache mais
   **jamais évincé** → calendriers de tirage périmés après édition d'un channel/game.
4. Le nommage réel (`catalog:plan:active_plans`, deux-points) contredit la convention
   documentée (`<scope>.<domain>.<resource>`, points). À trancher : aligner le code ou le doc.

---

## 1. Écarts entre `cache.md` et le code

| # | Règle `cache.md` | Réalité | Gravité |
|---|---|---|---|
| E1 | §8 « chaque cache **doit** être déclaré dans un `CacheSpecProvider` (ttlL1, ttlL2) » | ~20 caches sans provider → défauts 5 m / 60 m codés en dur dans `CacheRuntimeConfig` / `RedisCacheRuntimeConfig` | 🔴 |
| E2 | §6 nommage `<scope>.<domain>.<resource>` avec **points**, exemples `catalog.drawresult.by_id` | Code utilise des **deux-points** : `catalog:plan:active_plans` | 🟡 |
| E3 | §9 « éviction **après commit** (`beforeInvocation=false`) » | `@CacheEvict` sans `beforeInvocation` (défaut = false ✅) mais **exécuté dans la même transaction** — pas de hook after-commit réel. Si le commit échoue après l'éviction logique, incohérence possible. À vérifier au cas par cas. | 🟡 |
| E4 | §8 « `ttlL1 ≤ ttlL2` obligatoire » | Respecté par les providers. Mais les caches **sans** provider héritent L1=5 m / L2=60 m — OK. `CacheSpec.DEFAULT_TTL_L1`=10 m diverge du défaut `CacheRuntimeConfig`=5 m. Deux « défauts » différents. | 🟡 |
| E5 | Liens `architecture/cache-architecture.md`, `architecture/ops.md` | **Inexistants**. `docs/reference/REDIS-CONFIG.md` est **vide**. | 🟡 |
| E6 | §11 endpoints Ops audités | Présents (`CacheOpsController`, `CacheOpsResourceContributor`) ✅ | ✅ |

---

## 2. Inventaire complet des caches (état actuel)

Légende éviction : **✅** ciblée cohérente · **⚠️** `allEntries` (sur-éviction) · **❌** jamais évincé · **—** TTL only.

### 2.1 Caches AVEC `CacheSpecProvider` (TTL déclaré)

| Cache | Domaine | L1 | L2 | Éviction |
|---|---|---|---|---|
| `batch:global` (flag gate) | app/batch | 15 s | 2 m | — |
| `promotion.runtime_active` | core/promotion | 60 s | 15 m | evictor |
| `promotion.campaign_by_id` | core/promotion | 60 s | 2 h | evictor |
| `promotion.campaign_admin_list` | core/promotion | 60 s | 2 h | evictor |
| `infra.uslottery.provider_raw` | core/uslottery | 1 m | 5 m | — |
| `pricing.tenant_odds_list` | core/pricing | 10 m | 30 m | ⚠️ |
| `pricing.tenant_odds_by_variant` | core/pricing | 10 m | 30 m | ⚠️ |
| `draw.*` (next / latest / status …) | core/draw | 10 s | 60 s | evictor |
| `analytics.cashier_today` | core/analytics | 30 s | 2 m | — |
| `analytics.admin_overview` | core/analytics | 1 m | 5 m | — |
| `analytics.sales_summary` | core/analytics | 5 m | 30 m | — |
| `catalog:resultslot:calendar:v1:by_slot` | catalog/resultslot | 24 h | 24 h | ⚠️ |
| `news` external / internal | platform/publiccontent | 1 h | 1 h | events |
| `news` hidden | platform/publiccontent | 30 j | 30 j | events |
| `user_profile` | platform/accesscontrol | 20 m | 20 m | evictor |
| `role-permissions` | platform/accesscontrol | 45 m | 45 m | evictor |
| `entitlement.tenant_snapshot` | platform/entitlement | 5 m | 6 h | evictor |

### 2.2 Caches SANS provider → défaut **5 m / 60 m** (écart E1)

| Cache | Entité | Clé tenant ? | Éviction | Fréquence de modif réelle |
|---|---|---|---|---|
| `catalog:game:active_games` / `game_by_code` / `game_by_id` | Game (réf. global) | non (global) | ⚠️ allEntries | **très rare** |
| `catalog:plan:active_plans` / `plan_by_code` / `plan_by_id` | Plan (réf. global) | non (global) | ⚠️ allEntries | **très rare** |
| `catalog:theme:active_presets` / `preset_by_code` | ThemePreset | non (global) | ⚠️ allEntries | **très rare** |
| `catalog:resultslot:v2:active` / `by_key` / `by_id` | ResultSlot (définitions) | non (global) | ⚠️ allEntries | **très rare** |
| `catalog:pagemodeltemplate:by_id` / `by_logicalId` / `visible_list` / `search` | PageModelTemplate | non (global) | ⚠️ allEntries | **très rare** |
| `catalog:drawchannel:by_tenant` / `by_id` / `by_tenant_game_map` | DrawChannel (config tenant) | **oui** | ⚠️ allEntries | **rare** (par tenant) |
| `catalog:drawchannel:calendar_rows` | DrawChannel calendrier | **oui** | **❌ jamais** | **rare** |
| `catalog:settings:resolved_settings` | Settings résolus | **oui** | ⚠️ allEntries | **rare** |
| `catalog:i18n:resolved_by_locale` | I18n résolu | **oui** | ⚠️ allEntries | **rare** |
| `platform.tenant.*` (active_ids / by_code / by_id) | Tenant registry | mixte | à vérifier | **rare** |

---

## 3. Anomalies fonctionnelles (au-delà des TTL)

1. **🔴 `calendar_rows` jamais évincé** — `@Cacheable` dans `DrawChannelCatalogImpl#listCalendarRows`
   mais aucune `@CacheEvict` ne le cite. Les writes drawchannel évincent `BY_TENANT`, `BY_ID`,
   `BY_TENANT_GAME_MAP`, `BY_TENANT_BY_RESULT_SLOT_*` — **pas** `CALENDAR_ROWS`. Données de
   calendrier de tirage périmées jusqu'à expiration TTL après toute édition d'horaire / `enabled`.

2. **🟠 `by_tenant_by_result_slot_id` / `…provider_key`** — évincés partout mais **aucun
   `@Cacheable` ne les peuple**. Code mort (miroir inverse de l'anomalie 1).

3. **🟠 `DrawChannelGameAdminService`** n'évince que `{BY_TENANT, BY_ID, BY_TENANT_GAME_MAP}` —
   or c'est lui qui modifie le flag `enabled` filtré dans `calendar_rows` et `game_map`.

4. **🟡 Sur-éviction `allEntries` sur caches tenant-scopés** (`drawchannel`, `settings`, `i18n`) —
   un write d'un tenant vide les entrées de **tous** les tenants. Pas de réponse fausse, mais
   churn inutile en multi-tenant.

5. **🟡 Sérialisation Redis** (actif uniquement sous profil `local-ide-redis`) — voir risques
   `NullValue` (Optional vide non whitelisté) et `JsonNode` sous default typing `@class`.

---

## 4. Proposition : politique de TTL par fréquence de modification

Principe directeur : **le TTL suit le cycle de vie métier de l'entité, pas une valeur générique.**
Toutes ces entités ont une **éviction explicite sur write** → on peut allonger le TTL sans risque
de rester périmé (le TTL n'est qu'un filet de sécurité si une éviction est ratée).

### Tier A — Référentiels plateforme quasi-statiques
> Modifiés très rarement (admin), globaux, éviction sur write. TTL long = filet de sécurité.

**TTL cible : L1 = 30 min · L2 = 12 h**

- `catalog:plan:*` (Plan)
- `catalog:game:*` (Game)
- `catalog:theme:*` (ThemePreset)
- `catalog:pagemodeltemplate:*` (PageModelTemplate)
- `catalog:resultslot:v2:*` (définitions ResultSlot)
- `catalog:resultslot:calendar:v1:by_slot` (déjà 24 h — **OK, garder**)
- `platform.tenant.*` (registry)

### Tier B — Configuration par tenant, stable
> Modifiée rarement, scoping tenant dans la clé, éviction sur write.

**TTL cible : L1 = 15 min · L2 = 6 h**

- `catalog:drawchannel:by_tenant` / `by_id` / `by_tenant_game_map`
- `catalog:drawchannel:calendar_rows` (**+ corriger l'éviction manquante — bloquant**)
- `catalog:settings:resolved_settings`
- `catalog:i18n:resolved_by_locale`
- `entitlement.tenant_snapshot` (déjà 5 m / 6 h — **OK**)
- `user_profile`, `role-permissions` (déjà 20 m / 45 m — **OK**)
- `pricing.tenant_odds_*` (déjà 10 m / 30 m — peut monter à L2 = 2 h : les odds changent rarement)

### Tier C — Données de séance intra-journalières (draws)
> « Les draws changent dans la journée, après l'ouverture, jusqu'à la fermeture. »
> Volatilité liée aux **transitions d'état** (open → sales → cutoff → close), pas au temps.

**TTL cible : court (L1 = 10 s · L2 = 60 s — état actuel), MAIS éviction sur transition d'état.**

- `draw.*` : garder le TTL court actuel. Idéalement, **évincer sur événement**
  `DrawOpened` / `DrawClosed` / `ResultRecorded` plutôt que de dépendre du TTL 60 s (déjà des
  evictors : `DrawCacheEvictor`, `DrawResultCacheEvictor` — vérifier qu'ils couvrent toutes les
  transitions).
- `promotion.runtime_active` (60 s / 15 m — OK, promo active liée à la séance).

### Tier D — Ne PAS cacher (ou near-real-time uniquement)
> Change en permanence. Un cache y produit des réponses fausses ou une valeur nulle.

- **Liste des billets / tickets** — ne pas cacher.
- **Ticket lines** — ne pas cacher.
- **Audit info** — ne pas cacher (intégrité > perf).
- **Stats live** — `analytics.cashier_today` (30 s / 2 m) est un compromis near-real-time
  acceptable ; `admin_overview` (1 m / 5 m) et `sales_summary` (5 m / 30 m, historique) OK.
  Ne pas allonger : ce sont des agrégats vivants, pas des référentiels.

### Tableau de synthèse cible

| Tier | Entités | L1 | L2 | Déclencheur d'invalidation |
|---|---|---|---|---|
| A | plan, game, theme, pagemodel, resultslot-def, tenant | 30 m | 12 h | éviction sur write admin |
| A′ | resultslot-calendar (no-draw days) | 24 h | 24 h | éviction sur write (OK) |
| B | drawchannel, settings, i18n, entitlement, user/roles, pricing | 15 m | 6 h | éviction sur write tenant |
| C | draw status/next/latest, promo active | 10 s | 60 s | **événement de transition** + TTL |
| D | tickets, ticket lines, audit | — | — | **non caché** |

---

## 5. Actions recommandées (ordre de priorité)

1. **🔴 Corriger `calendar_rows`** : ajouter `CALENDAR_ROWS` à toutes les `@CacheEvict` drawchannel
   (extraire un `DrawChannelCacheNames.ALL[]` pour éviter la dérive). Nettoyer les deux caches
   morts `by_result_slot_*`.
2. **🔴 Créer les `CacheSpecProvider` manquants** (Tier A & B) pour sortir les ~20 caches catalog
   des défauts génériques et appliquer les TTL ci-dessus (résout E1).
3. **🟡 Trancher le nommage** (E2) : soit migrer les noms vers des points, soit mettre à jour
   `cache.md §6` pour entériner les deux-points. Recommandation : mettre à jour le doc (moins
   risqué que renommer des clés Redis en prod).
4. **🟡 Réparer/rétablir la doc** (E5) : recréer ou retirer les liens `cache-architecture.md` /
   `ops.md`, remplir ou supprimer `REDIS-CONFIG.md`, avancer « Last reviewed ».
5. **🟡 Unifier le défaut L1** (E4) : aligner `CacheRuntimeConfig` (5 m) et
   `CacheSpec.DEFAULT_TTL_L1` (10 m).
6. **🟡 Évaluer l'éviction ciblée par tenant** pour `drawchannel`/`settings`/`i18n` (remplacer
   `allEntries` par une clé/pattern tenant) si le churn devient mesurable.
7. **🟡 Fiabiliser la sérialisation Redis** avant activation L2 en prod (whitelist `NullValue`,
   traitement des `JsonNode`).
