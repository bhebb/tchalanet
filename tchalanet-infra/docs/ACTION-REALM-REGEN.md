# Action rapide - Régénération realm dev

## 🎯 Ce qui a été fait

✅ **Configuration Keycloak multi-environnements complète** :

1. **Fichiers d'environnement créés** :

   - `envs/dev/keycloak.env` (KC_HOSTNAME=auth.localtest.me)
   - `envs/staging/keycloak.env` (KC_HOSTNAME=auth.stg.tchalanet.com)
   - `envs/prod/keycloak.env` (KC_HOSTNAME=auth.tchalanet.com)

2. **Docker Compose Keycloak** :

   - `--hostname-url=https://${KC_HOSTNAME}` configuré

3. **Overlays realm** :

   - local.json, staging.json, prod.json déjà présents et corrects

4. **Protection Git** :

   - `.gitignore` créé dans `keycloak/realms/` pour ne pas commiter les realms générés

5. **Documentation** :
   - `KEYCLOAK-CONFIG-SUMMARY.md` : résumé complet
   - `KEYCLOAK-MULTI-ENV-CONFIG.md` : guide détaillé
   - `FIX-KEYCLOAK-ISSUER-MISMATCH.md` : explication du problème

## 🚀 Prochaine action IMMÉDIATE

### Pour dev (maintenant)

```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra

# 1. Merger les variables d'environnement
make env-merge ENV=dev

# 2. Régénérer le realm avec les nouvelles configs
make get-realm ENV=dev

# 3. Rebuilder et redémarrer Keycloak (avec l'image locale incluant le nouveau realm)
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak

# 4. Attendre 30-40 secondes puis tester
sleep 40
curl -s -k https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
# Doit retourner: "https://auth.localtest.me/realms/tchalanet"

# 5. Tester l'app web
open https://app.localtest.me
# Plus d'erreur "Issuer mismatch" !
```

## ✅ Checklist validation dev

- [ ] `make get-realm ENV=dev` s'exécute sans erreur
- [ ] Fichier `keycloak/realms/tchalanet-realm.json` est généré
- [ ] Keycloak démarre et est healthy (peut prendre 2-3 minutes)
- [ ] Issuer retourné = `https://auth.localtest.me/realms/tchalanet`
- [ ] Frontend Angular se connecte sans erreur "Issuer mismatch"
- [ ] Login fonctionne (super_admin / changeme)

## 📋 Pour staging/prod (plus tard)

Avant de déployer en staging ou prod :

```bash
# Staging
make env-merge ENV=staging
make get-realm ENV=staging
# Vérifier le fichier généré avant de déployer
cat keycloak/realms/tchalanet-realm.json | jq '.clients[] | select(.clientId=="tchalanet-web") | .redirectUris'

# Prod
make env-merge ENV=prod
make get-realm ENV=prod
# Vérifier le fichier généré avant de déployer
cat keycloak/realms/tchalanet-realm.json | jq '.clients[] | select(.clientId=="tchalanet-web") | .redirectUris'
```

## 🔍 Troubleshooting rapide

### Si `make get-realm` échoue

```bash
# Vérifier que le template existe
ls -la keycloak/realms/templates/realm.base.json

# Vérifier que l'overlay existe
ls -la keycloak/realms/overlays/dev.json

# Vérifier les variables d'environnement
cat envs/dev/.env.merged | grep KC_
```

### Si Keycloak ne démarre pas

```bash
# Voir les logs
docker logs tchl-keycloak-dev --tail=100

# Vérifier le volume
docker volume ls | grep keycloak

# Si problème de base de données, reset:
docker volume rm keycloak-data
docker compose -f compose/docker-compose-project.yml up -d keycloak
```

### Si l'issuer est toujours incorrect

```bash
# Vérifier que KC_HOSTNAME est bien passé au conteneur
docker exec tchl-keycloak-dev env | grep KC_HOSTNAME

# Si vide, recharger les env et recréer:
source envs/dev/.env.merged
docker compose -f compose/docker-compose-project.yml up -d --force-recreate keycloak
```

## 📖 Documentation complète

Pour plus de détails, consulter :

- **KEYCLOAK-CONFIG-SUMMARY.md** : ce document (résumé)
- **KEYCLOAK-MULTI-ENV-CONFIG.md** : guide complet multi-env
- **FIX-KEYCLOAK-ISSUER-MISMATCH.md** : explication technique

---

**Date** : 2025-11-16  
**Statut** : Configuration prête, à tester en dev puis déployer en staging/prod
