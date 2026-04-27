# 🚀 Quick Start - Développement Local

## Commandes Make simplifiées

### Démarrage complet (première fois)

```bash
cd tchalanet-infra

# Démarrer toute l'infrastructure
make up-all ENV=dev

# Dans un autre terminal : démarrer l'app Angular
cd ..
npm run start:web

# Ouvrir l'app
open https://app.localtest.me
```

### Rebuild après changements

```bash
cd tchalanet-infra

# Rebuilder Keycloak (avec régénération du realm)
make rebuild-keycloak ENV=dev

# Rebuilder l'API
make rebuild-api ENV=dev

# Rebuilder les deux
make rebuild-all ENV=dev
```

### Commandes utiles

```bash
# Voir l'état des conteneurs
make ps

# Voir les logs
make logs-api ENV=dev
make logs-keycloak ENV=dev

# Arrêter tout
make down-all ENV=dev

# Redémarrer un service
make up-keycloak ENV=dev
make up-api ENV=dev
```

## Tests rapides

```bash
# Backend API
curl -k https://api.localtest.me/actuator/health

# Keycloak issuer
curl -s -k https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'

# Frontend
open https://app.localtest.me
```

## Ce que fait chaque commande

### `make rebuild-keycloak`
1. Merge les variables d'environnement
2. Régénère le realm JSON
3. Build l'image Docker Keycloak locale (inclut le realm)
4. Redémarre le conteneur

### `make rebuild-api`
1. Compile le JAR Spring Boot
2. Build l'image Docker API locale
3. Redémarre le conteneur

### `make up-all`
1. Merge les variables d'environnement
2. Génère la config Traefik
3. Génère les certificats SSL locaux
4. Régénère le realm Keycloak
5. Démarre tous les services

## Workflow quotidien

```bash
# Matin
cd tchalanet-infra && make up-all ENV=dev
cd .. && npm run start:web &

# Après changement backend
make rebuild-api ENV=dev

# Après changement Keycloak
make rebuild-keycloak ENV=dev

# Soir
cd tchalanet-infra && make down-all ENV=dev
```

---

Pour plus de détails, voir :
- `BUILD-LOCAL-VS-PUBLISHED.md` - Guide complet build local
- `RECAP-COMPLETE-2025-11-16.md` - Résumé de toutes les corrections

