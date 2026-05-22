# Utiliser e2e-cashier-morning.sh dans différents environnements

## Setup rapide

```bash
# 1. Partir du template
cp scripts/.env.template scripts/.env.local

# 2. Remplir les credentials réels dans .env.local
# (voir commentaires dans le fichier)

# 3. Lancer le test
ENV_FILE="scripts/.env.local" zsh scripts/e2e-cashier-morning.sh
```

## Environnements recommandés

### Local dev (localhost:8083)

```bash
# Créer et remplir .env.local
cp scripts/.env.template scripts/.env.local

# Ajouter les credentials réels :
# TCH_SUPER_ADMIN_USERNAME=superadmin
# TCH_SUPER_ADMIN_PASSWORD=...
# TCH_SELLER_USERNAME=cashier.demo
# TCH_SELLER_PASSWORD=...
# TCH_AUTH_CLIENT_ID=tchalanet-swagger
# TCH_AUTH_CLIENT_SECRET=... # seulement si Client authentication = ON dans Keycloak

# Lancer
ENV_FILE="scripts/.env.local" zsh scripts/e2e-cashier-morning.sh
```

### Staging (ex: https://api-staging.example.com)

```bash
# Créer .env.staging
cp scripts/.env.template scripts/.env.staging

# Adapter les valeurs :
# TCH_BASE_URL="https://api-staging.example.com/api/v1"
# TCH_AUTH_ISSUER_URI="https://auth-staging.example.com/realms/tchalanet"
# TCH_SELLER_USERNAME="staging-cashier"
# TCH_SELLER_PASSWORD=...
# TCH_GAME_PROFILES="BOLET,MARYAJ,LOTO3"
# TCH_SELECTION_PLAN="BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1"
# TCH_SELL_MODE="SINGLE_TICKET_MULTI_GAME"
# TCH_DRAW_LIMIT="20"

# Lancer
ENV_FILE="scripts/.env.staging" zsh scripts/e2e-cashier-morning.sh
```

### Production (https://api.example.com)

> **Recommandation** : Utiliser des **JWT pré-générés** plutôt que des credentials, pour plus de sécurité.

```bash
# Créer .env.prod
cp scripts/.env.template scripts/.env.prod

# Adapter et utiliser les JWT directement :
# TCH_BASE_URL="https://api.example.com/api/v1"
# TCH_SUPER_ADMIN_TOKEN="eyJ..."  # Token préalablement autorisé
# TCH_SELLER_TOKEN="eyJ..."       # Token préalablement autorisé
# TCH_SELECTION_PLAN="BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1"
# TCH_SELL_MODE="SINGLE_TICKET_MULTI_GAME"
# TCH_DRAW_LIMIT="10"

# Lancer
ENV_FILE="scripts/.env.prod" zsh scripts/e2e-cashier-morning.sh
```

## Utilisation en cron (scheduling)

### Cron local dev

```bash
# Ajouter à crontab -e (exécution à 07:00 tous les jours)
0 7 * * * cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && ENV_FILE="scripts/.env.local" /bin/zsh scripts/e2e-cashier-morning.sh
```

### Cron production (avec JWT secrets)

```bash
# Ajouter à crontab -e (exécution à 06:00 UTC tous les jours)
0 6 * * * cd /app/tchalanet-server && ENV_FILE="/secure/path/.env.prod" /bin/zsh scripts/e2e-cashier-morning.sh >> /var/log/tchalanet-e2e.log 2>&1
```

## Fichiers .env par environnement

```
scripts/
├── .env              # Local dev (à créer ou customiser)
├── .env.local        # Alternative local à .gitignore
├── .env.staging      # Staging (à .gitignore)
├── .env.prod         # Production (à .gitignore)
└── .env.template     # Template (versionnée dans git)
```

## Sauvegarde sécurisée des secrets

**Ne jamais commiter** les fichiers `.env` contenant des credentials réels dans git !

Ajouter à `.gitignore` (déjà fait si présent) :
```bash
scripts/.env
scripts/.env.local
scripts/.env.staging
scripts/.env.prod
```

Pour production, stocker les secrets dehors du repo :
- Vault (HashiCorp)
- AWS Secrets Manager
- Azure KeyVault
- Fichier local chiffré sur le serveur de cron

## Vérifier l'env courant

```bash
# Voir le chemin du fichier .env utilisé
echo $ENV_FILE

# Voir les variables chargées (masqué pour sécurité)
grep -E '^TCH_' scripts/.env.local | sed 's/=.*$/=***masked***/g' | cat
```

## Dépannage

### Erreur: "Variable obligatoire manquante: TCH_SUPER_ADMIN_USERNAME"

→ Le fichier `$ENV_FILE` n'existe pas ou ne contient pas les credentials. Vérifie :
```bash
ls -la $ENV_FILE
grep TCH_SUPER_ADMIN_USERNAME $ENV_FILE
```

### Erreur: "unauthorized_client"

→ Le client OAuth2 n'accepte pas `password` grant. Dans Keycloak, ouvre le realm `tchalanet`, client `tchalanet-swagger`, puis vérifie **Direct access grants = ON**. Si `TCH_AUTH_CLIENT_SECRET` est défini, vérifie aussi **Client authentication = ON** et recopie le secret depuis l’onglet Credentials.

### Erreur: "Cannot connect to API"

→ Le `TCH_BASE_URL` est incorrect ou le backend n'est pas accessible. Teste :
```bash
curl -I $(cat scripts/.env.local | grep TCH_BASE_URL | cut -d= -f2)/platform/ops/draws/open-today
```

## Variables surchargeables en ligne de commande

Tu peux aussi surcharger les variables au lancement sans toucher `.env` :

```bash
# Surcharger un single draw limit pour test rapide
ENV_FILE="scripts/.env.local" TCH_DRAW_LIMIT="1" zsh scripts/e2e-cashier-morning.sh

# Ou combiner
ENV_FILE="scripts/.env.staging" \
  TCH_SELLER_USERNAME="special-user" \
  TCH_SELLER_PASSWORD="special-pwd" \
  zsh scripts/e2e-cashier-morning.sh
```

## Pour les logs détaillés

```bash
# Afficher le log du dernier run
tail -f .tmp/e2e-morning.log

# Afficher les artefacts générés (PDF, JSON)
find .tmp/e2e-phase2-pdfs -type f -mtime -1 | head -20
```
