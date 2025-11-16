# Scripts Utilitaires

Scripts utilitaires pour Docker et l'infrastructure.

## 📜 Scripts disponibles

### `health-check.sh`

**Objectif :** Vérifier la santé des services Docker

**Usage :**

```bash
./scripts/utils/health-check.sh [service]
```

**Actions :**

- Vérifie le statut des conteneurs
- Teste les healthchecks
- Retourne le statut

---

### `docker-pull-retry.sh`

**Objectif :** Pull images Docker avec retry automatique

**Usage :**

```bash
./scripts/utils/docker-pull-retry.sh <image:tag>
```

**Actions :**

- Tente de pull l'image
- Retry automatique en cas d'échec
- Timeout configurableExemple :\*\*

```bash
./scripts/utils/docker-pull-retry.sh postgres:18.0
./scripts/utils/docker-pull-retry.sh redis:8.2-alpine
```

---

### `check-envars.sh`

**Objectif :** Valider les variables d'environnement

**Usage :**

```bash
./scripts/utils/check-envars.sh [env]
```

**Actions :**

- Vérifie la présence des variables requises
- Détecte les variables vides
- Liste les variables manquantes

**Exemple :**

```bash
./scripts/utils/check-envars.sh staging
```

---

## 🔧 Cas d'usage

### Avant déploiement

```bash
# Vérifier les variables
./scripts/utils/check-envars.sh staging

# Pull toutes les images avec retry
./scripts/utils/docker-pull-retry.sh postgres:18.0
./scripts/utils/docker-pull-retry.sh redis:8.2-alpine
```

### Après déploiement

```bash
# Vérifier la santé
./scripts/utils/health-check.sh

# Healthcheck spécifique
./scripts/utils/health-check.sh postgres
```

## 📝 Conventions

- Scripts shell POSIX compatibles
- Exit codes standards (0=OK, 1=erreur)
- Messages clairs sur stdout/stderr
- Utilisables en CI/CD

## 🔗 Voir aussi

- [scripts/local/](../local/) - Scripts dev local
- [scripts/docker/](../docker/) - Scripts runtime
- [Makefile](../../Makefile) - Commandes Make
