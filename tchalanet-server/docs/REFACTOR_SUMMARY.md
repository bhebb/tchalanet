# Récapitulatif technique — Refactorisation Tchalanet

Ce document résume les changements récents effectués dans le projet (refactorisation architecture hexagonale, cache, audit, batch, API publique tirages) et fournit un guide d'utilisation rapide.

Plan rapide

- Générer une checklist des changements.
- Décrire fonctionnellement chaque bloc (cache combiné, audit, batch, API publique tirages).
- Lister les fichiers principaux créés / modifiés et leurs emplacements.
- Mode d'emploi rapide : endpoints utiles + comment lancer un job.

---

## 1) Checklist rapide des changements appliqués

- Création d'un cache combiné local+remote : `CombinedCache`, `CombinedCacheManager`.
- Intégration Caffeine (local) + Redis (remote) via `CacheConfig` et `RedisConfig`.
- `CacheAdminController` pour administration des caches (list/clear/clear-all).
- Ajout d'une action d'audit dédiée `CACHE_CLEAR` et enregistrement des événements d'audit à chaque invalidation.
- `CacheKeyBuilder` : conventions de clés multi-tenant (préfixe `tch:{env}:{tenantId}:...`).
- API publique (tirages) : `PublicDrawsController`, use case `GetPublicDrawsSummaryUseCase`, cache service `PublicDrawsCacheService`.
- Implémentation V1 de `GetPublicDrawsSummaryUseCaseImpl` (lecture cache -> mock minimal -> write cache).
- Ports externes et adapters stubs : Keycloak, Meilisearch, Unleash (interfaces + impl. HTTP minima).
- Ajout Spring Batch : dépendance, `BatchConfig`, jobs/steps déclarés, Tasklet stubs invoquant use cases.
- `BatchJobController` (admin) pour déclencher un job manuellement depuis l'API (protegé SUPER_ADMIN).
- Conventions et utilitaires : clés de cache, TTL recommandés et schéma d'invalidation.

---

## 2) Description fonctionnelle par bloc

### Cache combiné (local + remote)

Objectif

- Offrir un cache local (Caffeine) pour de hautes performances côté instance.
- Offrir un cache remote (Redis) pour partage et persistance au-delà d'une instance.

Composants principaux

- `CombinedCache` : wrapper qui lit d'abord le cache local, puis le remote si absent, et ré-injecte la valeur locale (cache warm).
- `CombinedCacheManager` : fabrique `CombinedCache` pour chaque nom de cache en combinant `CaffeineCacheManager` et `RedisCacheManager`.
- `CacheConfig` : bean `caffeineCacheManager` + bean `cacheManager` (CombinedCacheManager).
- `RedisConfig` : bean `redisCacheManager` (serializer JSON générique).
- `CacheAdminController` : API d'administration (list, clear, clear-all). Les opérations d'effacement créent un event d'audit.

Notes opérationnelles

- Invalidation cross-instance : pour propager un clear à toutes les instances, il est recommandé d'ajouter un pub/sub Redis (à venir).
- Écriture synchrones sur Redis : simple et sûr pour V1; à optimiser si latences / QPS élevées.

### Audit

Objectif

- Traçabilité des actions critiques (cache clears, modifications métier, etc.).

Composants principaux

- Enum `AuditAction` étendu (`CACHE_CLEAR`).
- Use case `LogAuditEventUseCase` (déjà présent) utilisé par les contrôleurs pour persister les événements.
- `AuditEvent` JPA entity + repository (déjà présents) pour stockage.

Notes opérationnelles

- Les événements d'audit incluent le contexte (tenant, user) si `RequestContextHolder` est correctement rempli.

### Batch (V1)

Objectif

- Orchestrer traitements planifiés : fermer tirages arrivés, régler résultats, rafraîchir cache public, renouveler abonnements, purger anciens audits.

Composants principaux

- `spring-boot-starter-batch` (dépendance ajoutée dans `pom.xml`).
- `BatchConfig` : déclare jobs (closeDrawsJob, settleDrawsJob, refreshPublicCacheJob, renewSubscriptionsJob, purgeOldAuditEventsJob) et les steps associés.
- Tasklets : `CloseDrawsTasklet`, `SettleDrawsTasklet`, `RefreshPublicCacheTasklet`, `RenewSubscriptionsTasklet`, `PurgeOldAuditEventsTasklet` (stubs appelant les use cases correspondants).
- `BatchJobController` : endpoint admin pour déclencher un job manuellement (utilise `ApplicationContext` + réflexion pour robustesse à la compilation).

Notes opérationnelles

- Spring Batch crée/attend des tables meta (BATCH\_\*). Assure-toi que la DB contient ces tables (Spring Batch peut initialiser automatiquement ou via script SQL).
- Les Tasklets sont des adaptateurs simples vers les use cases; implémenter la logique métier réelle dans les use cases.

### API publique tirages / résultats (home public)

Objectif

- Fournir au front (BFF ou widget) un résumé pre-calculé des tirages : channels, status, next draw, derniers résultats.

Composants principaux

- `PublicDrawsController` (GET /api/public/draws/summary/{tenantId}).
- Domain records : `DrawSummary`, `ChannelSummary`.
- Use case port : `GetPublicDrawsSummaryUseCase` et impl `GetPublicDrawsSummaryUseCaseImpl`.
- `PublicDrawsCacheService` : wrapper cache pour ce résumé (utilise `CacheKeyBuilder`).
- Scheduler `PublicCacheRefreshScheduler` (cron) reliant `RefreshPublicDrawsCacheUseCase` (pré-remplissage des caches par tenant).

Notes opérationnelles

- TTL recommandés :
  - `next_draw` : 30–60s (TTL court)
  - `results` : 5–10 min (TTL plus long)
- Strategy V1 : precompute dans batch et stocker en Redis ; servir depuis cache. En cas de settle, evict ciblé.

---

## 3) Liste des fichiers principaux créés / modifiés

(chemins relatifs projet)

Cache

- src/main/java/com/tchalanet/server/common/cache/CombinedCache.java
- src/main/java/com/tchalanet/server/common/cache/CombinedCacheManager.java
- src/main/java/com/tchalanet/server/common/cache/CacheKeyBuilder.java
- src/main/java/com/tchalanet/server/common/config/CacheConfig.java
- src/main/java/com/tchalanet/server/common/config/RedisConfig.java

Audit

- src/main/java/com/tchalanet/server/common/audit/domain/model/AuditAction.java (ajout `CACHE_CLEAR`)
- (LogAuditEventUseCase reste utilisé)

API publique / draw

- src/main/java/com/tchalanet/server/draw/web/PublicDrawsController.java
- src/main/java/com/tchalanet/server/draw/domain/usecase/GetPublicDrawsSummaryUseCase.java
- src/main/java/com/tchalanet/server/draw/infra/usecase/impl/GetPublicDrawsSummaryUseCaseImpl.java
- src/main/java/com/tchalanet/server/draw/infra/cache/PublicDrawsCacheService.java
- src/main/java/com/tchalanet/server/draw/domain/model/DrawSummary.java
- src/main/java/com/tchalanet/server/draw/domain/model/ChannelSummary.java
- src/main/java/com/tchalanet/server/draw/domain/usecase/RefreshPublicDrawsCacheUseCase.java
- src/main/java/com/tchalanet/server/draw/infra/scheduler/PublicCacheRefreshScheduler.java

Batch

- src/main/java/com/tchalanet/server/common/config/BatchConfig.java
- src/main/java/com/tchalanet/server/common/batch/CloseDrawsTasklet.java
- src/main/java/com/tchalanet/server/common/batch/SettleDrawsTasklet.java
- src/main/java/com/tchalanet/server/common/batch/RefreshPublicCacheTasklet.java
- src/main/java/com/tchalanet/server/common/batch/RenewSubscriptionsTasklet.java
- src/main/java/com/tchalanet/server/common/batch/PurgeOldAuditEventsTasklet.java
- src/main/java/com/tchalanet/server/common/web/BatchJobController.java

External ports / adapters (stubs)

- src/main/java/com/tchalanet/server/external/ports/KeycloakUserProvisioningPort.java
- src/main/java/com/tchalanet/server/external/infra/KeycloakUserProvisioningHttpAdapter.java
- src/main/java/com/tchalanet/server/external/ports/SearchIndexPort.java
- src/main/java/com/tchalanet/server/external/infra/MeiliSearchIndexHttpAdapter.java
- src/main/java/com/tchalanet/server/external/ports/FeatureFlagPort.java
- src/main/java/com/tchalanet/server/external/infra/UnleashFeatureFlagHttpAdapter.java

Admin cache controller

- src/main/java/com/tchalanet/server/common/web/CacheAdminController.java

---

## 4) Mode d'emploi rapide (endpoints utiles + comment lancer un job)

Endpoints importants

- GET /api/platform/cache/list

  - Description : lister les caches connus par le `CacheManager`.
  - Accès : SUPER_ADMIN

- DELETE /api/platform/cache/clear/{cacheName}

  - Description : effacer la cache nommée. Enregistre un `AuditEvent` (action `CACHE_CLEAR`).
  - Accès : SUPER_ADMIN

- DELETE /api/platform/cache/clear-all

  - Description : effacer toutes les caches connues (itère sur `cacheManager.getCacheNames()` et clear). Audit event enregistré.
  - Accès : SUPER_ADMIN

- POST /api/admin/jobs/launch

  - Description : lancer manuellement un job Spring Batch déclaré (utile en dev et pour super-admin).
  - Body : JSON { "jobName": "closeDrawsJob", "triggeredBy": "ui" }
  - Réponse : { "status": "COMPLETED|STARTING|...", "id": "<executionId>" }
  - Accès : SUPER_ADMIN
  - Remarque : le controller utilise `ApplicationContext` + réflexion pour appeler `jobLauncher.run(job, params)` de façon robuste.

- GET /api/public/draws/summary/{tenantId}
  - Description : récupère le résumé public (channels, prochain tirage, derniers résultats) pour le tenant donné.
  - Accès : public (aucune auth requise) — peut être appelé par le front.
  - Retour : structure `DrawSummary` (voir le code pour le format exact).

Comment lancer un job en local (exemple)

1. Depuis l'API (recommandé pour tests manuels) :

```http
POST /api/admin/jobs/launch
Content-Type: application/json
Authorization: Bearer <token_super_admin>

{
  "jobName": "closeDrawsJob",
  "triggeredBy": "dev-ui"
}
```

2. Depuis la CLI (exécution Spring Batch via maven) :

```bash
# lancer l'application et appeler l'endpoint (curl example)
curl -X POST http://localhost:8080/api/admin/jobs/launch \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"jobName":"closeDrawsJob"}'
```

Notes :

- Le jobName doit correspondre au bean job déclaré dans `BatchConfig` (ex : `closeDrawsJob`, `settleDrawsJob`, `refreshPublicCacheJob`, `renewSubscriptionsJob`, `purgeOldAuditEventsJob`).
- Si tu veux déclencher des runs programmés, configure la scheduler/cron (ex : `@Scheduled`) qui appelle les use cases, ou execute les `JobLauncher` via des tâches planifiées.

---

## 5) Prochaines étapes recommandées (court terme)

- Implémenter pub/sub Redis pour invalidation cross-instance (urgent en multi-instance).
- Implémenter réellement `RefreshPublicDrawsCacheUseCase` et le job associé (pré-remplissage de cache par tenant).
- Connecter `SettleDrawsUseCase` pour evict/mettre à jour les caches concernés quand résultats arrivés.
- Ajouter tests d'intégration (Testcontainers : Redis + Postgres) pour valider CombinedCache + audit + job triggers.
- Ajouter TTLs distincts par clé dans `RedisConfig` (next draw vs results).

---

Si tu veux, je peux :

- A) générer un README détaillé dans le repo (je l'ai fait ici sous `docs/REFACTOR_SUMMARY.md`).
- B) implémenter maintenant la pub/sub Redis + test d'intégration minimal.
- C) ajouter la configuration TTLs par cache dans `RedisConfig`.

Dis quelle action prioritaire tu veux (A/B/C) et je m'en occupe.
