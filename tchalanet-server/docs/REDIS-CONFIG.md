# Configuration Redis Optionnelle

## Contexte

Redis est optionnel dans Tchalanet. L'application peut démarrer et fonctionner sans Redis, mais certaines fonctionnalités de cache seront désactivées.

## Profils Spring Boot

### 1. `local-ide` (Redis désactivé par défaut)

Usage normal pour le développement dans l'IDE sans Redis :

```bash
# IntelliJ Run Configuration ou application.properties
SPRING_PROFILES_ACTIVE=local-ide
```

**Services requis** :

- ✅ PostgreSQL (localhost:5432)
- ❌ Redis (optionnel)
- ✅ Keycloak via Traefik (https://auth.localtest.me)

**Démarrage infrastructure** :

```bash
make local-ide-up  # Démarre Traefik + Postgres + Keycloak (sans Redis)
```

### 2. `local-ide-redis` (Redis activé)

Pour tester avec Redis en local :

```bash
# IntelliJ Run Configuration
SPRING_PROFILES_ACTIVE=local-ide-redis
```

**Services requis** :

- ✅ PostgreSQL (localhost:5432)
- ✅ Redis (localhost:6379)
- ✅ Keycloak via Traefik (https://auth.localtest.me)

**Démarrage infrastructure** :

```bash
make local-ide-up-redis  # Démarre Traefik + Postgres + Keycloak + Redis
```

## Configuration Manuelle

Si vous voulez activer Redis sans changer de profil :

**Option 1** : Variables d'environnement

```bash
TCH_CACHE_REDIS_ENABLED=true
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=devredis
```

**Option 2** : application.yaml local

```yaml
tch:
  cache:
    redis:
      enabled: true

spring:
  autoconfigure:
    exclude: [] # Réactive l'autoconfiguration Redis
  data:
    redis:
      host: localhost
      port: 6379
      password: devredis
```

## Gestion des erreurs

### Erreur : `Connection refused: localhost/127.0.0.1:6379`

**Cause** : Redis est configuré mais pas démarré.

**Solutions** :

1. Démarrer Redis : `make local-ide-up-redis`
2. Utiliser le profil sans Redis : `SPRING_PROFILES_ACTIVE=local-ide`
3. Désactiver Redis : `TCH_CACHE_REDIS_ENABLED=false`

### Vérifier si Redis est démarré

```bash
# Vérifier le conteneur Docker
docker ps | grep redis

# Tester la connexion
redis-cli -h localhost -p 6379 -a devredis ping
# Devrait retourner : PONG
```

## Fonctionnalités impactées

Quand Redis est **désactivé** :

- ❌ Cache L2 (les requêtes iront directement en base)
- ❌ Cache distribué entre instances
- ✅ Application fonctionne normalement

Quand Redis est **activé** :

- ✅ Cache L2 actif (améliore les performances)
- ✅ Cache distribué entre instances
- ✅ Session partagée (si utilisée)

## FAQ

**Q: Puis-je démarrer l'app sans Redis en production ?**  
R: Non recommandé. Redis est désactivé par défaut uniquement pour faciliter le dev local. En production, Redis devrait toujours être actif.

**Q: Comment savoir si Redis est activé ?**  
R: Regardez les logs au démarrage :

```
INFO c.t.s.common.cache.RedisConfig : Creating LettuceConnectionFactory for Redis localhost:6379
```

Si cette ligne n'apparaît pas, Redis est désactivé.

**Q: Puis-je utiliser un Redis externe ?**  
R: Oui, configurez `SPRING_DATA_REDIS_HOST` et `SPRING_DATA_REDIS_PORT`.
