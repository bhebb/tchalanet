## ADDED Requirements

### Requirement: Prérequis machine documentés

`QUICK-START.md` SHALL lister tous les prérequis nécessaires avant le premier
démarrage : Docker Desktop (Compose V2), `make`, `mkcert`, accès GHCR ou procédure
de build local de l'image Keycloak.

#### Scenario: Développeur sans mkcert

- **WHEN** un développeur suit `QUICK-START.md` sans `mkcert` installé
- **THEN** la section prérequis indique la commande exacte `brew install mkcert && mkcert -install`

#### Scenario: Développeur sans accès GHCR

- **WHEN** un développeur n'a pas de `GHCR_PAT` pour puller l'image Keycloak custom
- **THEN** `QUICK-START.md` indique la commande `make rebuild-keycloak ENV=dev`

---

### Requirement: Procédure de démarrage quotidien

`QUICK-START.md` SHALL fournir une procédure de démarrage quotidien en commandes
shell copiables, dans l'ordre exact : `make env-merge` → `make mkcert-local` →
`make networks` → `make up-all`.

#### Scenario: Démarrage quotidien nominal

- **WHEN** un développeur exécute `make up-all ENV=dev` après `make env-merge`
- **THEN** tous les services (postgres, redis, keycloak, traefik, unleash) sont
  `healthy` et l'API démarre sans erreur Flyway

#### Scenario: Démarrage API en IDE local (Option B)

- **WHEN** un développeur veut exécuter Spring Boot hors Docker
- **THEN** `QUICK-START.md` fournit les commandes `docker compose up -d traefik postgres redis keycloak`
  puis `./mvnw spring-boot:run -Dspring-boot.run.profiles=local-ide`

---

### Requirement: Checklist DoD de vérification

`QUICK-START.md` SHALL inclure une checklist de vérification post-démarrage avec
les commandes `curl` exactes pour valider chaque service.

#### Scenario: Vérification Keycloak

- **WHEN** le développeur exécute la commande de vérification Keycloak
- **THEN** `curl -s https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq .issuer`
  retourne `"https://auth.localtest.me/realms/tchalanet"`

#### Scenario: Vérification API health

- **WHEN** le développeur exécute la vérification API
- **THEN** `curl -s http://localhost:8083/api/v1/actuator/health | jq .status`
  retourne `"UP"`

#### Scenario: Vérification token Keycloak + appel authentifié

- **WHEN** le développeur exécute la commande token avec `super_admin`/`changeme`
- **THEN** un `access_token` JWT est retourné et un appel `GET /tenant/draws`
  avec ce token retourne HTTP 200

---

### Requirement: Traefik healthcheck fiable au boot

Le healthcheck Traefik SHALL utiliser l'entrypoint `web` (HTTP, port 80) pour
le ping, de sorte que le healthcheck soit positif indépendamment du chargement
des certificats TLS.

#### Scenario: Premier démarrage avec certs mkcert

- **WHEN** Traefik démarre pour la première fois et charge les certs mkcert
- **THEN** le healthcheck `traefik healthcheck --ping` retourne succès sans
  attendre que le provider TLS soit initialisé

#### Scenario: Démarrage normal

- **WHEN** Traefik démarre normalement
- **THEN** `docker ps` affiche `(healthy)` pour le conteneur Traefik dans les 15s

---

### Requirement: `setup-api-env.sh` génère des valeurs correctes

Le script `scripts/local/setup-api-env.sh` SHALL générer un fichier `.env` dont
les valeurs sont alignées avec `application-local-ide.yaml` et `envs/dev/.secrets`.

#### Scenario: DB et utilisateur corrects

- **WHEN** le script est exécuté avec `ENV=dev`
- **THEN** le `.env` généré contient `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tchalanet_db`
  et `SPRING_DATASOURCE_USERNAME=app_user`

#### Scenario: Realm Keycloak correct

- **WHEN** le script est exécuté avec `ENV=dev`
- **THEN** le `.env` contient l'issuer URI `https://auth.localtest.me/realms/tchalanet`
  (pas `realms/tchalanet-dev`)

#### Scenario: `ddl-auto` interdit absent

- **WHEN** le script est exécuté
- **THEN** le `.env` contient `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
  (jamais `update` ou `create`)
