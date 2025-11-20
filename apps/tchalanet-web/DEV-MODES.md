# Modes de développement - Ports et configuration

## 🎯 Deux modes de développement

### Mode 1 : Local-IDE (Backend sur l'hôte)

**Backend Spring Boot** tourne **directement sur l'hôte** (pas dans Docker)

| Service | Où | Port | URL |
|---------|-----|------|-----|
| **Backend (Spring Boot)** | Hôte | 8083 | `http://localhost:8083` |
| **Frontend (Vite)** | Hôte | 4200 | `http://localhost:4200` |
| **Keycloak** | Docker | 8082→8080 | `http://localhost:8082` |
| **PostgreSQL** | Docker | 5432 | `localhost:5432` |
| **Redis** | Docker | 6379 | `localhost:6379` |
| **Traefik** | Docker | 8080 | `http://localhost:8080` |

**Configuration actuelle (✅ correcte) :**
```typescript
// vite.config.mts
const apiTarget = 'http://localhost:8083'; // Backend sur l'hôte
```

**Démarrage :**
```bash
# 1. Services Docker (sauf API)
cd tchalanet-infra
make up-all  # ou docker compose up postgres keycloak redis

# 2. Backend Spring Boot sur l'hôte
cd tchalanet-server
export SPRING_PROFILES_ACTIVE=local-ide
./mvnw spring-boot:run  # Port 8083

# 3. Frontend sur l'hôte
cd apps/tchalanet-web
./start-dev.sh  # Port 4200
```

---

### Mode 2 : Full Docker (Backend dans Docker)

**Backend Spring Boot** tourne **dans Docker**

| Service | Où | Port (hôte:container) | URL depuis hôte |
|---------|-----|----------------------|-----------------|
| **Backend (Spring Boot)** | Docker | 8081:8080 | `http://localhost:8081` |
| **Frontend (Vite)** | Hôte | 4200 | `http://localhost:4200` |
| **Keycloak** | Docker | 8082:8080 | `http://localhost:8082` |
| **PostgreSQL** | Docker | 5432:5432 | `localhost:5432` |
| **Redis** | Docker | 6379:6379 | `localhost:6379` |
| **Traefik** | Docker | 8080:8080 | `http://localhost:8080` |

**Configuration nécessaire (si tu veux utiliser ce mode) :**
```typescript
// vite.config.mts
const apiTarget = 'http://localhost:8081'; // Backend dans Docker
```

**Démarrage :**
```bash
# 1. Tous les services Docker (y compris API)
cd tchalanet-infra
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.override.yml \
               up -d

# 2. Frontend sur l'hôte
cd apps/tchalanet-web
TCH_API_TARGET=http://localhost:8081 ./start-dev.sh
```

---

## 📊 Mapping des ports dans docker-compose.override.yml

```yaml
services:
  api:
    ports:
      - "8081:8080"  # Hôte:8081 → Container:8080

  keycloak:
    ports:
      - "8082:8080"  # Hôte:8082 → Container:8080

  traefik:
    ports:
      - "8080:8080"  # Hôte:8080 → Container:8080
```

**Pourquoi ces ports ?**
- **8080** : Réservé à Traefik (reverse proxy)
- **8081** : API backend (dans Docker)
- **8082** : Keycloak (dans Docker)
- **8083** : API backend (sur l'hôte, mode local-ide)

---

## 🔄 Communication entre services

### Mode Local-IDE (actuel)

```
Frontend (hôte:4200)
  ↓ Proxy Vite
Backend Spring Boot (hôte:8083)
  ↓ Direct
PostgreSQL (docker:5432)
  ↓ Direct
Keycloak (docker:8082 → container:8080)
```

### Mode Full Docker

```
Frontend (hôte:4200)
  ↓ Proxy Vite
Backend Docker (hôte:8081 → container:8080)
  ↓ Network Docker (noms de service)
PostgreSQL (docker:5432)
Keycloak (docker:8082 → container:8080)
```

---

## ⚙️ Configuration actuelle du projet

### Frontend (`vite.config.mts`)

```typescript
const apiTarget = process.env['TCH_API_TARGET'] ?? 'http://localhost:8083';
```

**Par défaut :** Backend sur l'hôte (local-ide)  
**Override :** `TCH_API_TARGET=http://localhost:8081` pour backend Docker

### Backend (`application-local-ide.yaml`)

```yaml
server:
  port: 8083  # Port pour exécution sur l'hôte
```

### Backend Docker (`docker-compose.override.yml`)

```yaml
api:
  ports:
    - "8081:8080"  # Port exposé quand dans Docker
```

---

## 🚀 Recommandation : Mode Local-IDE

**Mode actuellement configuré et recommandé : Local-IDE**

✅ **Avantages :**
- Hot reload rapide du backend (pas besoin de rebuild Docker)
- Debugging facile avec IntelliJ/VSCode
- Logs directement dans le terminal
- Modifications de code instantanées

❌ **Inconvénients :**
- Nécessite Java/Maven installé
- Plus de terminaux ouverts

---

## 📝 Checklist selon le mode

### Mode Local-IDE (✅ Configuration actuelle)

- [x] Backend Spring Boot sur port **8083** (hôte)
- [x] Frontend Vite proxy vers `localhost:8083`
- [x] Keycloak Docker sur port **8082**
- [x] PostgreSQL Docker sur port **5432**
- [x] Variables : `SPRING_PROFILES_ACTIVE=local-ide`

### Mode Full Docker (si tu veux l'utiliser)

- [ ] Backend Docker sur port **8081** (hôte)
- [ ] Frontend Vite proxy vers `localhost:8081`
- [ ] Keycloak Docker sur port **8082**
- [ ] PostgreSQL Docker sur port **5432**
- [ ] Variables : `TCH_API_TARGET=http://localhost:8081`
- [ ] Build image Docker de l'API

---

## 🔧 Changer de mode

### Passer en mode Full Docker

```bash
# 1. Modifier le proxy Vite
export TCH_API_TARGET=http://localhost:8081

# 2. Démarrer tous les services Docker
cd tchalanet-infra
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.override.yml \
               up -d

# 3. Frontend
cd apps/tchalanet-web
./start-dev.sh
```

### Revenir en mode Local-IDE (actuel)

```bash
# 1. Par défaut (pas besoin de TCH_API_TARGET)

# 2. Services Docker (sans API)
cd tchalanet-infra
docker compose -f compose/docker-compose-postgres.yml \
               -f compose/docker-compose-keycloak.yml \
               -f compose/docker-compose-redis.yml \
               up -d

# 3. Backend sur l'hôte
cd tchalanet-server
export SPRING_PROFILES_ACTIVE=local-ide
./mvnw spring-boot:run

# 4. Frontend
cd apps/tchalanet-web
./start-dev.sh
```

---

## ✅ Configuration actuelle confirmée

**Mode :** Local-IDE  
**Backend :** Hôte sur port 8083  
**Frontend :** Hôte sur port 4200, proxy vers 8083  
**Status :** ✅ Correctement configuré

---

**Date :** 19 novembre 2025  
**Mode recommandé :** Local-IDE  
**Configuration :** Complète et cohérente

