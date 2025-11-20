# Configuration des ports - Frontend et Backend

## 🎯 Deux modes de développement

### Mode actuel : Local-IDE (Backend sur l'hôte)

C'est le mode **actuellement configuré** et recommandé pour le développement.

| Service | Localisation | Port | Raison |
|---------|--------------|------|--------|
| **Frontend Vite** | Hôte | 4200 | Dev server Angular |
| **Backend Spring Boot** | **Hôte** | **8083** | Exécution locale (hot reload) |
| **Keycloak** | Docker | 8082→8080 | Auth (via Docker) |
| **PostgreSQL** | Docker | 5432 | Base de données |
| **Redis** | Docker | 6379 | Cache |
| **Traefik** | Docker | 8080 | Reverse proxy |

### Mode alternatif : Full Docker (Backend dans Docker)

Si tu veux exécuter l'API dans Docker (via `docker-compose.override.yml`) :

| Service | Localisation | Port (hôte:container) | Raison |
|---------|--------------|----------------------|--------|
| **Frontend Vite** | Hôte | 4200 | Dev server Angular |
| **Backend Spring Boot** | **Docker** | **8081:8080** | Dans container Docker |
| **Keycloak** | Docker | 8082:8080 | Auth (via Docker) |
| **PostgreSQL** | Docker | 5432:5432 | Base de données |
| **Redis** | Docker | 6379:6379 | Cache |
| **Traefik** | Docker | 8080:8080 | Reverse proxy |

**Note :** Le fichier `docker-compose.override.yml` définit les mappings de ports pour le mode Full Docker.

---

## ⚙️ Configuration appliquée (Mode Local-IDE)

### Backend (Spring Boot)

**Fichier :** `tchalanet-server/src/main/resources/application-local-ide.yaml`

```yaml
server:
  port: 8083  # Port 8080 réservé à Traefik
```

### Frontend (Vite)

**Fichier :** `apps/tchalanet-web/vite.config.mts`

```typescript
const apiTarget = process.env['TCH_API_TARGET'] ?? 'http://localhost:8083';
```

Le proxy Vite redirige `/api` → `http://localhost:8083/api`

### Variables d'environnement

**Fichier :** `apps/tchalanet-web/.env.development`

```bash
VITE_API_BASE=/api  # Chemin relatif pour passer par le proxy Vite
```

## 🔄 Flux des requêtes

```
Browser
  ↓ http://localhost:4200/api/v1/configs/i18n
  
Vite Dev Server (port 4200)
  ↓ Proxy: /api → http://localhost:8083
  
Backend Spring Boot (port 8083)
  ✅ /api/v1/configs/i18n
```

## 🚀 Pour démarrer

### 1. Backend

```bash
cd tchalanet-server
export SPRING_PROFILES_ACTIVE=local-ide
./mvnw spring-boot:run
```

Le backend démarre sur **http://localhost:8083**

### 2. Frontend

```bash
cd apps/tchalanet-web
./start-dev.sh
# ou
npx nx serve tchalanet-web --configuration=development
```

Le frontend démarre sur **http://localhost:4200**  
Le proxy Vite redirige automatiquement `/api` vers `localhost:8083`

## ✅ Vérification

### Tester le backend directement

```bash
curl http://localhost:8083/api/v1/configs/i18n?lang=fr
```

### Tester via le proxy Vite

```bash
curl http://localhost:4200/api/v1/configs/i18n?lang=fr
```

Les deux commandes doivent retourner la même réponse.

## 🔧 Changer le port du backend

Si tu veux utiliser un autre port pour le backend :

### Option 1 : Variable d'environnement (temporaire)

```bash
# Frontend
TCH_API_TARGET=http://localhost:9000 npx nx serve tchalanet-web

# Backend
SERVER_PORT=9000 ./mvnw spring-boot:run
```

### Option 2 : Modifier les fichiers (permanent)

**Backend :** `application-local-ide.yaml`
```yaml
server:
  port: 9000
```

**Frontend :** `vite.config.mts`
```typescript
const apiTarget = process.env['TCH_API_TARGET'] ?? 'http://localhost:9000';
```

## 📝 Ports utilisés dans les autres environnements

### Staging

- **Backend :** `https://api.stg.tchalanet.com` (via Traefik/reverse proxy)
- **Frontend :** `https://app.stg.tchalanet.com`
- Pas de proxy Vite (URLs absolues)

### Production

- **Backend :** `https://api.tchalanet.com` (via Traefik/reverse proxy)
- **Frontend :** `https://app.tchalanet.com`
- Pas de proxy Vite (URLs absolues)

## 🎯 Pourquoi le port 8080 est réservé à Traefik

Traefik est le reverse proxy qui gère :
- Routage HTTP/HTTPS
- Certificats SSL
- Load balancing
- Routes vers Keycloak, Backend, Frontend

En local avec Docker, Traefik écoute sur le port 8080 pour l'administration et le routage.

## 🔍 Debugging

### Logs du proxy Vite

Dans le terminal du serveur Vite, tu verras :

```
[Vite Proxy] → GET /api/v1/configs/i18n?lang=fr => http://localhost:8083/api/v1/configs/i18n?lang=fr
[Vite Proxy] ← GET /api/v1/configs/i18n?lang=fr [200]
```

### Erreurs communes

| Erreur | Cause probable | Solution |
|--------|---------------|----------|
| `ECONNREFUSED 8083` | Backend pas démarré | Démarrer le backend |
| `ECONNREFUSED 8080` | Mauvaise config port | Vérifier `TCH_API_TARGET` |
| `404 Not Found` | Mauvais endpoint | Vérifier le controller |
| `CORS error` | URL absolue utilisée | Utiliser `/api` (relatif) |

---

**Date :** 19 novembre 2025  
**Port Backend local-ide :** 8083  
**Port Frontend dev :** 4200  
**Port Traefik :** 8080 (réservé)

