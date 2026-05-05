# Keycloak Development Guide

## Structure

```
keycloak/
├── Dockerfile                          # Build custom Keycloak image
├── tchalanet-keycloak-provider/        # Custom providers (Maven project)
│   ├── pom.xml
│   └── src/
├── realms/
│   ├── templates/
│   │   └── realm.base.json            # Template de base
│   └── overlays/
│       ├── dev.json                    # Overrides dev
│       ├── staging.json                # Overrides staging
│       └── prod.json                   # Overrides prod
└── themes/                             # Custom themes (optionnel)
```

## Workflow Development

### 1. Première installation

```bash
# Depuis la racine du projet
make local-ide-up
```

Cela va :

- ✅ Builder l'image Keycloak avec vos providers
- ✅ Générer le realm depuis le template
- ✅ Démarrer Traefik + Postgres + Keycloak

### 2. Modifier les providers

Quand vous modifiez le code dans `tchalanet-keycloak-provider/` :

```bash
# Rebuild l'image + restart Keycloak
make keycloak-rebuild
```

Ou manuellement :

```bash
# 1. Builder l'image
make keycloak-build

# 2. Redémarrer le conteneur
make keycloak-restart
```

### 3. Modifier le realm

Le realm est généré depuis :

- **Template de base** : `realms/templates/realm.base.json`
- **Overlay env-spécifique** : `realms/overlays/dev.json`

Après modification :

```bash
# Régénérer le realm
cd tchalanet-infra
make get-realm ENV=dev

# Redémarrer Keycloak pour recharger
make keycloak-restart
```

### 4. Debugging

```bash
# Voir les logs en temps réel
make keycloak-logs ENV=dev

# Entrer dans le conteneur
docker exec -it tchl-keycloak-dev bash

# Vérifier les providers installés
docker exec tchl-keycloak-dev ls -la /opt/keycloak/providers/
```

## Accès

- **Console Admin** : https://auth.localtest.me

  - Username : `admin`
  - Password : voir `.secrets` ou `KEYCLOAK_ADMIN_PASSWORD`

- **Realm Public** : https://auth.localtest.me/realms/tchalanet

- **API Endpoint** : https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration

## Variables d'environnement importantes

Dans `envs/dev/compose.env` :

```bash
KEYCLOAK_IMAGE=tchl/keycloak:local-dev  # Image locale
IMAGE_TAG=local-dev
KC_HOSTNAME=auth.localtest.me
```

Dans `envs/dev/.env` :

```bash
KC_REALM=tchalanet
KC_BOOTSTRAP_ADMIN_USERNAME=admin
TCH_KC_ISSUER=https://auth.localtest.me/realms/tchalanet
```

## Troubleshooting

### Keycloak ne démarre pas

```bash
# Vérifier les logs
make keycloak-logs ENV=dev

# Vérifier la santé de Postgres
docker exec tchl-postgres-dev pg_isready -U postgres

# Recréer complètement
make local-ide-down
make local-ide-up
```

### Provider non chargé

```bash
# Vérifier que le JAR est présent
docker exec tchl-keycloak-dev ls -la /opt/keycloak/providers/

# Rebuild complet
make keycloak-rebuild
```

### Realm non importé

```bash
# Vérifier le fichier realm généré
cat keycloak/realms/tchalanet-realm.json | jq '.realm'

# Régénérer
cd tchalanet-infra
make get-realm ENV=dev
make keycloak-restart
```

## Commandes utiles

```bash
# Depuis la racine du projet
make help                    # Voir toutes les commandes
make keycloak-build          # Builder l'image Keycloak
make keycloak-rebuild        # Rebuild + restart
make keycloak-logs           # Logs en temps réel
make keycloak-restart        # Redémarrer

# Depuis tchalanet-infra
make get-realm ENV=dev       # Générer le realm
make rebuild-keycloak        # Rebuild avec docker-compose.local-build.yml
```

## Production

Pour staging/prod, l'image est publiée sur ghcr.io :

```bash
# Build et push (CI/CD)
docker build -t ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2 .
docker push ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2
```

L'image en staging/prod utilise :

- `envs/common/compose.env` : `KEYCLOAK_IMAGE=ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2`
- Override dans `envs/staging/compose.env` si nécessaire
