# Tchalanet — Stratégie de cache : état des lieux & proposition (document de revue)

> **Status**: PROPOSAL / REVIEW (non normatif — destiné à être challengé)
> **Scope**: tchalanet-server — couches `common`, `core`, `catalog`, `platform`, `app`
> **Date**: 2026-07-07
> **Normatif de référence**: [`cache.md`](./cache.md) · **Architecture**: [`common/cache/CACHE.md`](../../tchalanet-common/src/main/java/com/tchalanet/server/common/cache/CACHE.md) · **Audit détaillé**: [`cache-gap-analysis.md`](./cache-gap-analysis.md)
>
> Ce document est **auto-porteur** : il contient tous les faits nécessaires pour être
> revu sans accès au code. Objectif : décider **quelles entités cacher**, **avec quels TTL**,
> et **pourquoi**.

---

## 0. Résumé exécutif

- Cache à **deux niveaux** assemblé main : **L1 Caffeine** (in-process) + **L2 Redis** (partagé), via un `CombinedCacheManager`. Redis est **désactivé par défaut** (`tch.cache.redis.enabled=false`, en dur) → aujourd'hui la plupart des environnements tournent **en L1 seul**.
- **~20 caches n'ont aucun `CacheSpecProvider`** et retombent sur les défauts génériques **5 min (L1) / 60 min (L2)** — un référentiel quasi-statique expire alors aussi vite qu'une donnée de séance.
- **Bugs fonctionnels actifs** : `drawchannel:calendar_rows` caché mais **jamais évincé** ; `drawresult:*` évincé mais **jamais peuplé** ; `drawchannel:by_result_slot_*` évincé mais **jamais lu**.
- Le controller Ops sait **vider** un cache, **pas le désactiver** (il se re-remplit aussitôt). Aucun kill-switch par cache.
- Principe proposé : **le TTL suit le cycle de vie métier de l'entité**, et n'est qu'un filet de sécurité car l'invalidation réelle passe par l'éviction sur write.

---

## 1. Architecture réelle

```
@Cacheable → CombinedCacheManager (@Primary)
                ├─ L1 : CacheSpecAwareCaffeineCacheManager   (toujours actif)
                └─ L2 : RedisCacheManager                    (si tch.cache.redis.enabled=true)
```

- Lecture : L1 → miss → L2 → recopie en L1. Écriture / évict / clear se propagent aux deux niveaux.
- **Tolérance panne** : les erreurs L2 sont capturées (`log.warn` + evict), l'app dégrade silencieusement vers L1. Le backend **doit** fonctionner sans Redis (règle normative).
- **Redis activé uniquement** par le profil `local-ide-redis` ; `application.yaml` fixe `enabled: false` **sans variable d'environnement** → pas d'activation prod par simple env var aujourd'hui (à vérifier côté infra).

### 1.1 TTL par défaut (pièges)

| Source | L1 | L2 |
|---|---|---|
| `CacheRuntimeConfig` (défaut runtime) | **5 min** | — |
| `RedisCacheRuntimeConfig` (défaut runtime) | — | **60 min** |
| `CacheSpec.DEFAULT_TTL_L1 / L2` (si `CacheSpec.of` à 1 arg) | 10 min | 60 min |

⚠️ Deux « défauts L1 » divergents (5 vs 10 min) selon que le cache est déclaré ou non. Règle imposée partout : `ttlL1 ≤ ttlL2`.

### 1.2 Sérialisation Redis (L2)

- Clés : `StringRedisSerializer`.
- Valeurs : `GenericJacksonJsonRedisSerializer` (Jackson 3 / `tools.jackson`) avec **default typing `NON_FINAL_AND_RECORDS`** (propriété `@class`) et allowlist :
  `com.tchalanet.server.` · `java.time.` · `java.util.` · `java.math.` · `org.springframework.cache.interceptor.SimpleKey` · `tools.jackson.`
- **Risques de sérialisation** (ne surviennent que si Redis est actif) :
  1. **`NullValue`** (`org.springframework.cache.support`) **non whitelisté** → cacher un `Optional.empty()` peut échouer. **Mitigation existante** : `unless = "#result == null"` sur le tenant registry — à généraliser.
  2. **`JsonNode`** dans des records mis en cache (ex. `GameSummaryView.flags`) → default typing `@class` sur arbre polymorphe = fragile.
  3. Le serializer Redis **construit son propre `ObjectMapper`**, il ne réutilise pas celui de l'app → divergence de modules possible.

### 1.3 Administration Ops (`CacheOpsController`, SUPER_ADMIN, audité)

| Action | Effet |
|---|---|
| `GET /platform/ops/cache` | Lister les caches |
| `DELETE /platform/ops/cache/{name}` | **Vider** un cache (L1+L2) |
| `DELETE /platform/ops/cache/groups/{group}` | Vider un groupe (groupes **codés en dur** : seuls `plans` et `pricing`) |
| `DELETE /platform/ops/cache` | Tout vider |

**Limite clé** : *vider ≠ désactiver*. Un cache vidé se **re-remplit** à la lecture suivante. Le seul vrai « off » global est `tch.cache.redis.enabled=false` (L2 uniquement, toute l'app, au redémarrage). **Il n'existe aucun interrupteur par cache.**

> **Recommandation kill-switch** : ajouter un flag runtime par cache, vérifié via
> `@Cacheable(condition = "@cacheToggle.on('name')")`, piloté par les settings, pour
> désactiver un cache problématique sans redéploiement.

---

## 2. Écarts entre `cache.md` et le code

| # | Règle | Réalité | Gravité |
|---|---|---|---|
| E1 | §8 « chaque cache doit avoir un `CacheSpecProvider` » | ~20 caches sans provider → défauts 5/60 min | 🔴 |
| E2 | §6 nommage en points `catalog.plan.active_plans` | Code en deux-points `catalog:plan:active_plans` (+ legacy en points côté drawresult/tenant) | 🟡 corrigé dans `cache.md` (colon canonique) |
| E3 | §9 « éviction après commit » | `@CacheEvict` (défaut `beforeInvocation=false`) mais **dans la même transaction**, pas de hook after-commit réel | 🟡 |
| E4 | `ttlL1 ≤ ttlL2` | Respecté ; mais double défaut L1 (5 vs 10 min) | 🟡 corrigé (note ajoutée) |
| E5 | Liens doc | `cache-architecture.md` / `ops.md` inexistants ; `REDIS-CONFIG.md` vide | 🟡 corrigé (liens réels) |
| E6 | §11 Ops audité | Présent ✅ | ✅ |

---

## 3. Anomalies fonctionnelles (indépendantes des TTL)

1. **🔴 `catalog:drawchannel:calendar_rows`** — `@Cacheable` mais **aucune `@CacheEvict`** ne le cite. Calendriers de tirage périmés après édition d'un channel/game jusqu'à expiration TTL. Aggravé par `DrawChannelGameAdminService` qui n'évince que `{BY_TENANT, BY_ID, BY_TENANT_GAME_MAP}`.
2. **🟠 `catalog:drawchannel:by_result_slot_id` / `…provider_key`** — évincés partout, **jamais peuplés** (`@Cacheable` absent) → code mort.
3. **🟠 `core.drawresult.*`** — `DrawResultCacheEvictor` déclare 3 caches, **rien ne les alimente** → évince du vide.
4. **🟡 Sur-éviction `allEntries`** sur caches tenant-scopés (`drawchannel`, `settings`, `i18n`) → le write d'un tenant vide tous les tenants.

---

## 4. Inventaire complet des caches

Légende : **✅** éviction ciblée · **⚠️** `allEntries` · **❌** non évincé · **—** TTL only · **∅** pas de spec provider (défaut 5/60 min).

| Cache | Couche | Clé | Spec provider | L1 | L2 | Éviction |
|---|---|---|---|---|---|---|
| `catalog:plan:active_plans` / `plan_by_code` / `plan_by_id` | catalog | — / code / id | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:game:active_games` / `game_by_code` / `game_by_id` | catalog | — / code / id | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:theme:active_presets` / `preset_by_code` | catalog | — / code | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:resultslot:v2:active` / `by_key` / `by_id` | catalog | — / key / id | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:resultslot:calendar:v1:by_slot` | catalog | slotId | ✅ | 24 h | 24 h | ⚠️ |
| `catalog:pagemodeltemplate:by_id` / `by_logicalId` / `visible_list` / `search` | catalog | id / logicalId / const / page | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:drawchannel:by_tenant` / `by_id` / `by_tenant_game_map` | catalog | tenant(+…) | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:drawchannel:calendar_rows` | catalog | tenant:active:enabled | ∅ | 5 m | 60 m | **❌ BUG** |
| `catalog:settings:resolved_settings` | catalog | `t=…|ns=…` | ∅ | 5 m | 60 m | ⚠️ |
| `catalog:i18n:resolved_by_locale` | catalog | tenant:locale:scope:vis | ∅ | 5 m | 60 m | ⚠️ |
| `platform.tenant.cache.REGISTRY_BY_CODE` / `BY_ID` / `ACTIVE_TENANT_IDS` | platform | code / id / — | ∅ | 5 m | 60 m | ✅ (+`unless`) |
| `user_profile` | platform | userId | ✅ | 10 m | 20 m | ✅ evictor |
| `role-permissions` | platform | roleId | ✅ | 10 m | 45 m | ✅ par roleId |
| `entitlement.tenant_snapshot` | platform | tenantId | ✅ | 5 m | 6 h | ✅ evictor |
| `news` external / internal / hidden | platform | key | ✅ | 1 h / 30 j | idem | events |
| `core:pricing:tenant_odds_list` / `by_variant` | core | tenant(+variant) | ✅ | 10 m | 30 m | ⚠️ |
| `promotion.runtime_active` | core | tenant/ctx | ✅ | 60 s | 15 m | evictor |
| `promotion.campaign_by_id` / `campaign_admin_list` | core | id / filtre | ✅ | 60 s | 2 h | evictor |
| `draw.*` (summary/today/upcoming/next/latest) | core | critères | ✅ | 10 s | 60 s | evictor |
| `analytics.cashier_today` / `admin_overview` / `sales_summary` | core | tenant/plage | ✅ | 30 s–5 m | 2 m–30 m | — |
| `infra.uslottery.provider_raw` | core | provider | ✅ | 1 m | 5 m | — |
| `batch:global` (flag gate) | app | — | ✅ | 15 s | 2 m | — |
| **`core.drawresult.by_id` / `latest.by_slot` / `id.by_slot_occurred`** | core | — | ∅ | *(mort)* | *(mort)* | ❌ non peuplé |
| **limit** (policy) | core | — | — | **non caché** | | |
| **sellerterminal** | core | — | — | **non caché** | | |
| **subscription** | core | — | — | non caché (via entitlement) | | |
| **notification** | platform | — | — | **non caché (voulu)** | | |

---

## 5. Proposition : que cacher, quel TTL, pourquoi

Colonne **Cacher ?** = recommandation. **Pourquoi** = justification du TTL (long = immuable/rare + évincé sur write ; court = volatile / sensible à la fraîcheur ; off = change à chaque opération).

### 5.1 Référentiels plateforme quasi-statiques → **L1 30 min / L2 12 h**

| Entité · lecture | Cache | Clé | Cacher ? | L1 | L2 | Pourquoi |
|---|---|---|---|---|---|---|
| Plan | `catalog:plan:*` | — / code / id | ✅ | 30 m | 12 h | Global, édité très rarement, évincé sur write |
| Game | `catalog:game:*` | — / code / id | ✅ | 30 m | 12 h | Idem |
| ThemePreset | `catalog:theme:*` | — / code | ✅ | 30 m | 12 h | Idem |
| ResultSlot (définitions) | `catalog:resultslot:v2:*` | — / key / id | ✅ | 30 m | 12 h | Idem |
| ResultSlot calendar (no-draw days) | `catalog:resultslot:calendar:v1:by_slot` | slotId | ✅ garder | 24 h | 24 h | SUPER_ADMIN, change très rarement (OK actuel) |
| PageModelTemplate (by_id / logicalId / visible) | `catalog:pagemodeltemplate:*` | id / logicalId / const | ✅ | 30 m | 12 h | Rendu quasi-statique |
| PageModelTemplate (search paginé) | `…:search` | params page | 🟡 court/off | 2 m | 5 m | Explosion de clés, faible réutilisation |
| Tenant registry | `platform.tenant.cache.*` | code / id / — | ✅ | 15 m | 6 h | Rare, `unless` déjà présent |

### 5.2 Configuration par tenant, stable → **L1 15 min / L2 6 h**

| Entité · lecture | Cache | Clé | Cacher ? | L1 | L2 | Pourquoi |
|---|---|---|---|---|---|---|
| DrawChannel (by_tenant / by_id / game_map) | `catalog:drawchannel:*` | tenant(+…) | ✅ | 15 m | 6 h | Config tenant rare, évincé sur write |
| DrawChannel calendar_rows | `catalog:drawchannel:calendar_rows` | tenant:active:enabled | ✅ **+ corriger éviction** | 15 m | 6 h | **Bloquant** : éviction manquante |
| Settings résolus | `catalog:settings:resolved_settings` | `t=…|ns=…` | ✅ | 15 m | 6 h | Rare |
| I18n résolu | `catalog:i18n:resolved_by_locale` | tenant:locale:scope:vis | ✅ | 15 m | 6 h | Rare |
| Tenant theme runtime | `platform:tenanttheme:runtime` *(à créer)* | tenant:mode | ✅ **ajouter** | 15 m | 6 h | Lu à chaque page publique, change rarement — **ajouter `@CacheEvict` sur `TenantThemeAdminService.updateSettings`** |
| Tenant game runtime | `platform:tenantgame:runtime` *(à créer)* | tenant | ✅ **ajouter** | 15 m | 6 h | Lu à chaque session POS — **ajouter `@CacheEvict` sur `TenantGameAdminService.updateSettings/updateBetOptionConfig`** |
| Tenant game projection | `platform:tenantgame:projection` *(à créer)* | tenant | ✅ **ajouter** | 15 m | 6 h | Idem |
| Permissions (role→codes) | `role-permissions` | roleId | ✅ garder | 10 m | 45 m | Éviction ciblée fiable |
| User profile | `user_profile` | userId | ✅ | 10 m | 30 m *(↑)* | Stable, evictor sur changement de rôle |
| Entitlement snapshot | `entitlement.tenant_snapshot` | tenant | ✅ garder | 5 m | 6 h | OK actuel |
| Pricing odds | `core:pricing:*` | tenant(+variant) | ✅ | 10 m | 2 h *(↑ de 30 m)* | Cotes changent rarement |

### 5.3 Données de séance intra-journalières → **court + éviction événementielle**

| Entité · lecture | Cache | Clé | Cacher ? | L1 | L2 | Pourquoi |
|---|---|---|---|---|---|---|
| Draw (next / latest / today / status) | `draw.*` | critères | ✅ garder | 10 s | 60 s | « Change dans la journée après ouverture, jusqu'à fermeture » → **court**, idéalement évincer sur `DrawOpened/Closed` |
| DrawResult by_id | `core.drawresult.by_id` | resultId | ✅ **câbler** | 6 h | 24 h | Résultat publié **immuable** → **long** |
| DrawResult latest_by_slot | `core.drawresult.latest.by_slot` | slotId | ✅ **câbler** | 30 s | 2 m | « Dernier » bascule à la publication → **court** + éviction `ResultRecorded` |
| DrawResult id_by_slot_occurred | `core.drawresult.id.by_slot_occurred` | slot+date | ✅ **câbler** | 6 h | 24 h | Immuable une fois publié |
| Promotion active | `promotion.runtime_active` | tenant/ctx | ✅ garder | 60 s | 15 m | Lié à la séance |

### 5.4 À NE PAS cacher (ou near-real-time uniquement)

| Entité | Cacher ? | Pourquoi |
|---|---|---|
| **Limit — compteurs consommés** (reste vendable) | ❌ | Change à chaque vente → cacher = risque de **survente** (correctness > perf) |
| Limit — **définition** de LimitPolicy | ❌ (décision Phase 3.2) | La règle est lue **sur le chemin de décision** (`listActiveForTargets(scopes, now)`, mêlée à l'exposition), clé `now` = pas de hit, périmée = décision d'argent fausse → no-cache |
| **SellerTerminal — validation de vente / binding** | ❌ | Décision de **sécurité** (trust/terminal_binding) : doit être fraîche |
| SellerTerminal — profil `getMe` | ❌ (décision Phase 3.2) | `getMe` et `saleValidation` partagent `findById` → cacher exposerait la validation ; option handler-level en attente si read chaud mesuré |
| **Liste billets / ticket lines** | ❌ | Changent en permanence |
| **Stats live** (`analytics.cashier_today`) | 🟡 near-real-time OK (30 s / 2 m) | Agrégat vivant, pas un référentiel — ne pas allonger |
| **Audit info** | ❌ | Intégrité > performance |
| **Notification** (platform) | ❌ | Transactionnel / outbox — cacher créerait doublons/états faux |
| **Subscription** (core) | ❌ | Statut via `entitlement.tenant_snapshot` ; stats = JDBC live |
| **PageModel runtime** (core/features, ≠ `catalog:pagemodeltemplate`) | ❌ | Composition dynamique : dashboards admin (stats/health live), publicdrawresults (intra-journalier). Se compose déjà de caches (template/theme/games) → cacher l'assemblage masquerait la fraîcheur |

> **Audit couverture (2026-07-08)** : `accesscontrol` (role-permissions/user_profile), `entitlement`,
> `promotion`, `pricing` sont déjà cachés avec `CacheSpecProvider` (vérifié). Les non-cachés
> ci-dessus le sont **par design**.

---

## 6. Risques à considérer

1. **Éviction sur write = condition de tout allongement de TTL.** Allonger sans éviction fiable transforme un cache en source de réponses fausses (cf. `calendar_rows`).
2. **Clé multi-tenant obligatoire** pour tout cache tenant-scopé (tenant game/theme à créer) → sinon fuite inter-tenant.
3. **Sérialisation L2** (si Redis actif) : reprendre `unless=#result==null` (évite `NullValue`), éviter les `JsonNode` en valeur cachée, whitelister `org.springframework.cache.support` au besoin.
4. **`allEntries` inter-tenant** : churn sous multi-tenant ; envisager une éviction par clé/pattern tenant si mesurable.
5. **Câbler avant d'évincer** : `drawresult` doit avoir ses `@Cacheable` avant que son evictor ait un sens.
6. **Correctness des gates** : ne jamais cacher ce qui **autorise** (compteurs de limite, binding terminal) ; ne cacher que le **descriptif** (règle, profil), TTL court.

---

## 7. Questions ouvertes (à challenger)

1. Redis L2 est-il activé en prod ? (`application.yaml` = `false` en dur). Si non, tout le raisonnement L2 est théorique aujourd'hui.
2. Les référentiels `game/plan/theme/resultslot/pagemodel` sont-ils **réellement globaux** (mêmes données tous tenants) ? Si oui, l'absence de `tenantId` dans la clé est correcte ; sinon fuite.
3. Faut-il un **kill-switch par cache** (toggle runtime) plutôt que le simple flush Ops ?
4. TTL des draws : rester sur 60 s fixe ou passer à une invalidation **événementielle** (open/close/result) avec TTL de secours plus long ?
5. Migration des noms legacy (`core.drawresult.*`, `platform.tenant.cache.*`) vers le format `:` — vaut-il le risque (clés L2 persistées) ?

---

## 8. Plan d'action proposé (ordre)

1. 🔴 Corriger `calendar_rows` (ajouter à toutes les `@CacheEvict` drawchannel) ; nettoyer les caches morts (`by_result_slot_*`).
2. 🔴 Câbler `drawresult` (`@Cacheable` sur le reader) + créer son `CacheSpecProvider`.
3. 🔴 Créer les `CacheSpecProvider` manquants (plan, game, theme, resultslot, pagemodel, drawchannel, settings, i18n, tenant) avec les TTL §5.
4. ✅ Ajouter le cache tenant game/theme runtime (+ éviction sur leurs services admin).
5. ❌ limit + sellerterminal : **no-cache by design** (décision Phase 3.2) — reads sur les chemins de décision d'argent/sécurité, voir §5.4.
6. 🟡 Kill-switch runtime par cache + compléter les groupes Ops.
7. 🟡 Fiabiliser la sérialisation L2 avant activation prod.
