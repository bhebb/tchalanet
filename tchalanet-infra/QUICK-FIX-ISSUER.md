# 🚀 Quick Fix - Keycloak Issuer Mismatch

## ⚡ TL;DR

```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra
chmod +x scripts/utils/refresh-keycloak.sh
./scripts/utils/refresh-keycloak.sh dev
```

Puis redémarrer le frontend :
```bash
npm run start:web
```

---

## 🎯 Le Problème en 1 Ligne

Keycloak retournait `http://localhost:8080` mais le frontend attendait `https://auth.localtest.me`.

## ✅ La Solution en 1 Ligne

Ajouter `KC_HOSTNAME_URL=https://auth.localtest.me` dans la config Keycloak.

---

## 📋 Vérifications Rapides

### 1. Issuer Keycloak
```bash
curl -sk https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq -r .issuer
```
✅ Attendu : `https://auth.localtest.me/realms/tchalanet`

### 2. Variable Keycloak
```bash
docker exec tchl-keycloak-dev env | grep KC_HOSTNAME_URL
```
✅ Attendu : `KC_HOSTNAME_URL=https://auth.localtest.me`

### 3. Fichier Environment Frontend
```bash
cat libs/shared/config/src/lib/environments/environment-local-ide.ts | grep authUrl
```
✅ Attendu : `authUrl: 'https://auth.localtest.me/realms/tchalanet'`

---

## 🔧 Fichiers Modifiés

1. ✅ `compose/docker-compose-keycloak.yml` - Ajout KC_HOSTNAME_URL
2. ✅ `envs/dev/keycloak.env` - KC_HOSTNAME_URL=https://auth.localtest.me
3. ✅ `envs/staging/keycloak.env` - KC_HOSTNAME_URL=https://auth.stg.tchalanet.com
4. ✅ `envs/prod/keycloak.env` - KC_HOSTNAME_URL=https://auth.tchalanet.com
5. ✅ `scripts/utils/refresh-keycloak.sh` - Nouveau script automatique

---

## 📚 Documentation Complète

- `tchalanet-infra/KEYCLOAK-ISSUER-CONFIG.md` - Comment fonctionne l'issuer
- `tchalanet-infra/WEB-KEYCLOAK-CONFIG.md` - Config frontend détaillée
- `tchalanet-infra/FIX-ISSUER-MISMATCH-SUMMARY.md` - Ce fix complet
- `tchalanet-docs/docs/web/keycloak-configuration.md` - Référence technique

---

## 🐛 Si ça ne marche toujours pas

1. Vider le cache navigateur (F12 → Application → Clear site data)
2. Vérifier les logs : `docker logs tchl-keycloak-dev`
3. Vérifier le realm client : https://auth.localtest.me → Admin Console → Clients → tchalanet-web
4. Valid Redirect URIs doit contenir : `http://localhost:4200/*` et `https://app.localtest.me/*`

---

**Date:** 2025-11-18  
**Status:** ✅ Configuration appliquée et documentée

