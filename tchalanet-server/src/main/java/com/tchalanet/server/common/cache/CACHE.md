# 🧠 Tchalanet — Architecture du Cache

_(Caffeine L1 + Redis L2 + TTL par Feature)_

---

## 🎯 Objectifs

Le système de cache de Tchalanet répond aux objectifs suivants :

- **Performance** : réduire la latence des requêtes critiques (home publique, tirages, thèmes, tenant config).
- **Scalabilité** : permettre à plusieurs instances du backend de partager le même cache distribué.
- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL technique plus court côté Caffeine (L1).
- **Extensibilité par domaine** : chaque feature module déclare ses propres caches et TTL via un mécanisme standardisé.
- **Tolérance aux pannes** : Redis peut être désactivé (dev/local), et l’application continue avec un fallback Caffeine only.

---

## 1. Architecture du Cache

Tchalanet utilise un cache à deux niveaux ("2-Tier Cache") :

```text
        ┌──────────────┐
        │  L1 Cache     │   Caffeine (in-memory, par instance)
        │  Ultra-rapide │   TTL technique court
        └──────────────┘
               │
Miss →         ▼
┌──────────────┐
│  L2 Cache     │   Redis (cache partagé)
│  TTL métier   │   Par cacheName (ex: news=24h)
└──────────────┘
        │
Miss →  ▼
┌──────────────┐
│  Backend DB   │  Postgres / Meilisearch / External providers
└──────────────┘
```

### Priorité de lecture

1. Caffeine (L1)
2. Redis (L2)
3. Backend (DB / Meili / providers externes)

Si Redis renvoie une valeur, elle est ré-hydratée dans Caffeine automatiquement.

### Écriture & Eviction

Les opérations suivantes sont appliquées sur les deux caches (L1 + L2) :

- `put()`
- `evict()`
- `clear()`

---

## 2. Mécanismes internes

### 2.1. `CombinedCacheManager` (cache manager principal)

La classe `CombinedCacheManager` orchestre L1 + L2 :

- Si Redis est activé (`tch.cache.redis.enabled=true`) → `CombinedCacheManager(Caffeine, Redis)`
- Sinon → `CaffeineCacheManager` seul

Déclaration (dans `CacheConfig`) :

```java
@Primary
@Bean
public CacheManager cacheManager(
    CaffeineCacheManager caffeineCacheManager,
    @Nullable CacheManager redisCacheManager) {

  if (redisCacheManager != null) {
    return new CombinedCacheManager(caffeineCacheManager, redisCacheManager);
  }
  return caffeineCacheManager;
}
```

### 2.2. `CombinedCache` (par `cacheName`)

Règles :

- `get()` : lit d’abord dans Caffeine (local), puis dans Redis (remote). Si Redis renvoie une valeur, elle est recopiée en L1.
- `put()` : écrit dans Caffeine + Redis (si présent).
- `evict()` : supprime dans Caffeine + Redis.
- `clear()` : vide Caffeine + Redis.

---

## 2.x – Catalogue des caches (v1 élargi)

### 2.x.1. Tenant

Objectif : éviter de recharger les paramètres du tenant à chaque requête (thèmes, limites, options, policies, etc.).

| CacheName         | Clé (Redis)                          | Contenu                      | TTL Redis (L2) | TTL Caffeine (L1) | Invalidation                    |
| ----------------- | ------------------------------------ | ---------------------------- | -------------- | ----------------- | ------------------------------- |
| `tenant_config`   | `tch:{env}:{tenantId}:tenant:config` | `TenantConfig` agrégé        | 10–60 min      | 1–5 min           | après update config tenant      |
| `tenant_theme`    | `tch:{env}:{tenantId}:theme:active`  | `TenantThemePayload`         | 10–60 min      | 1–5 min           | après changement de thème       |
| `tenant_limits`\* | `tch:{env}:{tenantId}:tenant:limits` | règles de limites / policies | 5–30 min       | 1–5 min           | après modification des policies |

\*optionnel pour la v1.

→ Ces caches peuvent être déclarés dans un `com.tchalanet.server.core.tenant.infra.config.TenantCacheConfig` (module tenant) via un `CacheSpecProvider`.

---

### 2.x.2. Outlet (points de vente / terminaux)

Objectif : un vendeur / terminal interroge souvent les mêmes données (outlet, terminal, config locale).

| CacheName            | Clé (Redis)                                  | Contenu                             | TTL Redis (L2) | TTL Caffeine (L1) | Invalidation                      |
| -------------------- | -------------------------------------------- | ----------------------------------- | -------------- | ----------------- | --------------------------------- |
| `tenant_outlet`      | `tch:{env}:{tenantId}:outlet:{outletId}`     | infos outlet (nom, statut, limites) | 10–30 min      | 2–5 min           | après update outlet               |
| `tenant_terminal`    | `tch:{env}:{tenantId}:terminal:{terminalId}` | config terminal, canaux, options    | 10–30 min      | 2–5 min           | après update terminal             |
| `tenant_outlet_tree` | `tch:{env}:{tenantId}:outlet:tree`           | arbre complet outlets/terminaux     | 5–15 min       | 1–3 min           | après grosses modifs de structure |

→ Déclarables dans un `OutletCacheConfig` / `PosCacheConfig` dans le module outlet / POS.

---

### 2.x.3. Draw / Draw Channel

Côté draws, on exploite des TTL très courts (semi temps-réel).

| CacheName              | Clé (Redis)                                       | Contenu                              | TTL Redis (L2) | TTL Caffeine (L1) | Invalidation                            |
| ---------------------- | ------------------------------------------------- | ------------------------------------ | -------------- | ----------------- | --------------------------------------- |
| `tenant_draws_summary` | `tch:{env}:{tenantId}:draws:summary`              | tirages du jour (tous canaux / jeux) | 30–60 s        | 5–15 s            | auto par TTL + après batch de résultat  |
| `tenant_draws_channel` | `tch:{env}:{tenantId}:draws:{channelCode}:{kind}` | tirages d’un canal (jour/soir, etc.) | 30–60 s        | 5–15 s            | idem summary                            |
| `tenant_draw_public`   | `tch:{env}:{tenantId}:draws:public:{date}`        | version prête pour la page publique  | 60–300 s       | 10–30 s           | après settlement / publication résultat |

Ces caches sont décrits dans `DrawCacheConfig` (ou équivalent) via `CacheSpecProvider`.

---

### 2.x.4. Permissions / Profil utilisateur

Objectif : éviter de recombiner les rôles/permissions à chaque requête, surtout en présence d’un mapping Keycloak → rôles internes → ACL. Sécurité oblige : TTL court et invalidation dès changement majeur.

| CacheName             | Clé (Redis)                                      | Contenu                                      | TTL Redis (L2) | TTL Caffeine (L1) | Invalidation                                |
| --------------------- | ------------------------------------------------ | -------------------------------------------- | -------------- | ----------------- | ------------------------------------------- |
| `user_permissions`    | `tch:{env}:{tenantId}:user:{userId}:permissions` | liste/graph de permissions effectives        | 5–15 min       | 1–3 min           | après changement de rôles/permissions       |
| `user_profile`        | `tch:{env}:{tenantId}:user:{userId}:profile`     | profil de base (nom, outlet, terminal, etc.) | 10–30 min      | 2–5 min           | après update profil utilisateur             |
| `tenant_roles_matrix` | `tch:{env}:{tenantId}:roles:matrix`              | mapping rôles → permissions de base          | 30–60 min      | 5–15 min          | après update des rôles ou de la matrice ACL |

Bonne pratique :

- `user_permissions` = résultat final consommé par les guards de sécurité (RoleGuard, policies, etc.).
- TTL court pour éviter de garder un utilisateur « over-granté » trop longtemps.

→ Ces caches peuvent être déclarés dans un `AuthCacheConfig` ou similaire.

---

### 2.x.5. News / Search

Récap des caches déjà définis pour les news et la recherche :

| CacheName             | Clé (Redis)                            | Contenu                            | TTL Redis (L2) | TTL Caffeine (L1) |
| --------------------- | -------------------------------------- | ---------------------------------- | -------------- | ----------------- |
| `news`                | `tch:{env}:-:news`                     | agrégation des actualités (RSS…)   | 24h            | 5–30 min          |
| `global_search_query` | `tch:{env}:-:search:query:{queryHash}` | résultat d’une requête Meilisearch | 5–30 min       | 1–5 min           |

→ Côté code, ces caches sont déclarés via des `CacheSpecProvider` dans les modules `news` et `search`.

---

### 2.x.6. Comment refléter ça dans le code

- **Clés Redis** : construites (ou à construire) via `CacheKeyBuilder` (`tenantConfigKey`, `tenantOutletKey`, `tenantTerminalKey`, `userPermissionsKey`, etc.).
- **TTL Redis (L2)** : définis par domaine via des `CacheSpecProvider` dans chaque module (tenant, outlet, draw, auth, news, search…).
- **TTL Caffeine (L1)** : global court (ex. 5 min) pour tous les caches. On pourra affiner par cache plus tard si nécessaire.

---

## 3. Déclaration des caches par Feature (`CacheSpecProvider`)

Pour rendre le système extensible, chaque feature peut déclarer ses caches + TTL métier en fournissant un bean `CacheSpecProvider`.

### 3.1. Contrat générique

```java
package com.tchalanet.server.common.cache;

import java.time.Duration;
import java.util.List;

public record CacheSpec(String name, Duration ttlL2) {}

public interface CacheSpecProvider {
  List<CacheSpec> cacheSpecs();
}
```

### 3.2. Exemple : feature **News**

```java
package com.tchalanet.server.features.news.config;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NewsCacheConfig {

  @Bean
  public CacheSpecProvider newsCacheSpecProvider() {
    return () ->
        List.of(
            // TTL L2 = 24h pour les news publiques
            new CacheSpec("news", Duration.ofHours(24))
        );
  }
}
```

### 3.3. Exemple : feature **Draw**

```java
package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DrawCacheConfig {

  @Bean
  public CacheSpecProvider drawCacheSpecProvider() {
    return () ->
        List.of(
            // Résumé des tirages par tenant, TTL L2 = 60s
            new CacheSpec("tenant_draws_summary", Duration.ofSeconds(60)),
            // Résumé par canal, TTL L2 = 30s
            new CacheSpec("tenant_draws_channel", Duration.ofSeconds(30))
        );
  }
}
```

---

## 4. Gestion du TTL — L1 vs L2

### 4.1. Redis (L2) : TTL métier

- Géré dans `RedisConfig`.
- TTL par cache via `CacheSpecProvider`.
- TTL par défaut (si aucune spec) = **60 minutes**.
- Redis applique un TTL strict, défini par les besoins métier.

### 4.2. Caffeine (L1) : TTL technique

- TTL global (ex : 5 minutes), configurable dans `CacheConfig`.
- But :
  - éviter de conserver en mémoire locale une valeur trop vieille ;
  - limiter le nombre d’accès réseau vers Redis.
- Toujours `TTL_L1 ≤ TTL_L2`.

```java
@Bean
public Caffeine<Object, Object> caffeine() {
  return Caffeine.newBuilder()
      .maximumSize(10_000)
      .expireAfterWrite(Duration.ofMinutes(5))
      .build();
}
```

---

## 5. `RedisConfig` : agrégation des TTL par cache

`RedisConfig` collecte tous les `CacheSpecProvider` et crée une configuration Redis dédiée par `cacheName` :

```java
@Bean
public CacheManager redisCacheManager(
    LettuceConnectionFactory connectionFactory,
    List<CacheSpecProvider> specProviders) {

  RedisCacheConfiguration defaultCfg =
      RedisCacheConfiguration.defaultCacheConfig()
          .entryTtl(Duration.ofMinutes(60))
          .serializeKeysWith(
              RedisSerializationContext.SerializationPair.fromSerializer(
                  new StringRedisSerializer()))
          .serializeValuesWith(
              RedisSerializationContext.SerializationPair.fromSerializer(
                  new GenericJackson2JsonRedisSerializer()));

  var perCacheCfg = new HashMap<String, RedisCacheConfiguration>();

  specProviders.stream()
      .flatMap(p -> p.cacheSpecs().stream())
      .forEach(spec ->
          perCacheCfg.put(
              spec.name(),
              defaultCfg.entryTtl(spec.ttlL2()))
      );

  return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(defaultCfg)
      .withInitialCacheConfigurations(perCacheCfg)
      .build();
}
```

---

## 6. Désactivation de Redis

Redis est **optionnel** :

```yaml
tch:
  cache:
    redis:
      enabled: true | false
```

- Si `false` :
  - `RedisConfig` est désactivé (pas de `redisCacheManager`).
  - L’application tourne uniquement sur Caffeine (L1).
- Si `true` :
  - `CombinedCacheManager` combine Caffeine (L1) + Redis (L2).

---

## 7. Liste officielle des caches Tchalanet (MVP1)

### 🔵 News

| CacheName | Description                    | TTL Redis | TTL Caffeine |
| --------- | ------------------------------ | --------: | -----------: |
| `news`    | Actualités (RSS, sectorielles) |       24h |     5–30 min |

### 🟣 Draw

| CacheName              | Description            | TTL Redis | TTL Caffeine |
| ---------------------- | ---------------------- | --------: | -----------: |
| `tenant_draws_summary` | Résumé tirages du jour |       60s |        5–15s |
| `tenant_draws_channel` | Résumé par canal       |       30s |        5–15s |

### 🟠 Tenant / Branding

| CacheName       | Description   | TTL Redis | TTL Caffeine |
| --------------- | ------------- | --------: | -----------: |
| `tenant_theme`  | Thème actif   | 10–60 min |      1–5 min |
| `tenant_config` | Config tenant | 10–60 min |      1–5 min |

### 🟢 Search

| CacheName             | Description                 | TTL Redis | TTL Caffeine |
| --------------------- | --------------------------- | --------: | -----------: |
| `global_search_query` | Queries Meilisearch hashées |  5–30 min |      1–5 min |

---

## 8. Bonnes pratiques pour les développeurs

### ✔ Utiliser `@Cacheable` avec le `cacheName` du domaine

```java
@Cacheable(cacheNames = "news")
public List<NewsItem> fetchNews() { ... }
```

### ✔ Toujours mettre la config de cache dans le module qui l’utilise

Exemples :

- Feature **Draw** → `DrawCacheConfig`
- Feature **News** → `NewsCacheConfig`

### ✔ TTL Redis = logique métier

Déterminer selon :

- fréquence de mise à jour ;
- cohérence acceptable ;
- charge induite sur la BD ou Meilisearch.

### ✔ TTL Caffeine = logique performance locale

- Toujours `TTL_L1 ≤ TTL_L2`.

### ✔ Toujours utiliser `CacheKeyBuilder` pour les clés paramétrées

Exemple :

```java
var key = cacheKeyBuilder.tenantDrawsSummaryKey(tenantId);
```

---

## 9. Fonctionnement global : séquence d’accès

1. Client → Service annoté `@Cacheable`
2. `CombinedCache.get()`

Détails :

1. **Caffeine (L1)**
   - hit → retour direct
   - miss → étape 2
2. **Redis (L2)**
   - hit → `Caffeine.put()` → retour
   - miss → étape 3
3. **Backend (DB, Meili, Provider externe)**
   - récupération des données
   - `put()` (Caffeine + Redis)
   - retour client

---

## 10. Résilience et cohérence

### En cas de panne Redis

- Caffeine continue de servir les requêtes.
- Il n’y a plus de partage cross-instances.
- Les valeurs expirent selon le TTL L1.

### En cas de scale-out (plusieurs pods)

- Redis assure la cohérence métier entre instances.
- Caffeine réduit la charge sur Redis.

---

## 📌 Conclusion

Ce système de cache :

- respecte l’architecture hexagonale ;
- donne à chaque domaine la responsabilité de définir ses propres TTL métier ;
- garantit des performances maximales ;
- reste simple à raisonner et à maintenir.

→ Exactement ce qu’il faut pour Tchalanet avant le lancement MVP + scale futur.
