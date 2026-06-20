# Configuration Redis Optionnelle

Redis est optionnel. L'application peut démarrer sans Redis; les caches Redis
sont alors désactivés.

## Local IDE sans Redis

```bash
SPRING_PROFILES_ACTIVE=local-ide
make -C ../tchalanet-infra local-ide-up ENV=dev
```

Services requis :

- PostgreSQL
- Firebase Auth Emulator

## Local IDE avec Redis

```bash
SPRING_PROFILES_ACTIVE=local-ide-redis
make -C ../tchalanet-infra local-ide-up-redis ENV=dev
```

Services requis :

- PostgreSQL
- Redis
- Firebase Auth Emulator
