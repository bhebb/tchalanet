# Conventions — tchalanet-edge-service

| Sujet | Fichier |
|---|---|
| Architecture | [`../ARCHITECTURE.md`](../ARCHITECTURE.md) |
| Auth inter-service (HMAC) | [`../internal-messages-hmac-curl.md`](../internal-messages-hmac-curl.md) |

**Conventions à créer quand les règles existent dans le code :**
- Routing Fastify (patterns routes, plugins)
- Error handling (format réponses erreur)
- Provider retry policy

> Règle : ne pas créer une convention pour une règle qui n'existe pas encore dans le code.
