# Fix: Fichiers i18n (assets) ne se chargent pas

## 🐛 Problème

Les fichiers de traduction i18n situés dans `libs/i18n/locales/` ne se chargent pas dans l'application web.

**Symptômes :**
- ❌ Erreur 404 sur `/assets/i18n/fr.json`
- ❌ Interface en anglais ou clés de traduction affichées
- ❌ Console du navigateur : Failed to load resource

## 🔍 Cause racine

L'option `assets` dans `project.json` ne fonctionne pas avec `@nx/vite` (même problème qu'avec les styles).

**Configuration qui ne fonctionne PAS :**
```json
// project.json
"assets": [
  {
    "glob": "**/*",
    "input": "libs/i18n/locales",
    "output": "/assets/i18n"
  }
]
```

Avec Webpack/Angular CLI, cette configuration copie automatiquement les fichiers. Avec Vite, elle est ignorée.

## ✅ Solution

**Copier manuellement les fichiers** de `libs/i18n/locales` vers `apps/tchalanet-web/public/assets/i18n`.

### Option 1 : Copie automatique au démarrage (recommandée)

Le script `start-dev.sh` copie automatiquement les fichiers au démarrage :

```bash
./apps/tchalanet-web/start-dev.sh
```

Le script exécute :
```bash
mkdir -p public/assets/i18n
cp -f ../../libs/i18n/locales/*.json public/assets/i18n/
```

### Option 2 : Script dédié

Un script `copy-i18n.sh` est disponible :

```bash
cd apps/tchalanet-web
./copy-i18n.sh
```

### Option 3 : Copie manuelle

```bash
cd /Users/bhebb/Documents/projets/tchalanet
cp libs/i18n/locales/*.json apps/tchalanet-web/public/assets/i18n/
```

## 📁 Structure des fichiers

```
apps/tchalanet-web/
  public/
    assets/
      i18n/              # ← Fichiers copiés ici
        fr.json
        en.json
        ht.json
        versions.json

libs/
  i18n/
    locales/             # ← Source originale
      fr.json
      en.json
      ht.json
      versions.json
```

## 🔄 Workflow de développement

### Au démarrage du serveur

```bash
./apps/tchalanet-web/start-dev.sh
```

Le script :
1. Nettoie le cache Vite
2. **Copie les fichiers i18n** ✅
3. Démarre le serveur Vite

### Après modification des traductions

Si tu modifies les fichiers dans `libs/i18n/locales/`, tu dois les recopier :

```bash
# Option 1 : Redémarrer le serveur
./apps/tchalanet-web/start-dev.sh

# Option 2 : Copier sans redémarrer
cd apps/tchalanet-web
./copy-i18n.sh
# Puis recharger la page (Cmd+R)
```

## 🎯 Pourquoi Vite ne copie pas automatiquement ?

Avec `@nx/vite`, les fichiers du dossier `public/` sont servis statiquement tels quels. Les fichiers en dehors de `public/` ne sont pas copiés automatiquement.

**Solutions envisagées :**
1. ❌ Plugin `nxCopyAssetsPlugin` - Ne fonctionne pas pour ce cas
2. ❌ Lien symbolique - Peut causer des problèmes sur certains systèmes
3. ✅ **Copie manuelle au démarrage** - Simple et fiable

## 📝 Configuration Git

Les fichiers copiés sont ignorés par Git :

```gitignore
# .gitignore
public/assets/i18n/*.json
```

Seuls les fichiers sources dans `libs/i18n/locales/` sont versionnés.

## ✅ Vérification

Pour vérifier que les fichiers sont accessibles :

### 1. Vérifier la copie

```bash
ls -la apps/tchalanet-web/public/assets/i18n/
# Doit afficher : fr.json, en.json, ht.json, versions.json
```

### 2. Tester l'accès HTTP

```bash
curl http://localhost:4200/assets/i18n/fr.json
# Doit retourner le contenu JSON
```

### 3. Dans le navigateur

Ouvre `http://localhost:4200/assets/i18n/fr.json`  
Tu devrais voir le contenu JSON.

## 🔧 En cas de problème

### Fichiers non trouvés (404)

```bash
# 1. Vérifier que les fichiers sources existent
ls libs/i18n/locales/

# 2. Copier manuellement
cp libs/i18n/locales/*.json apps/tchalanet-web/public/assets/i18n/

# 3. Redémarrer le serveur
pkill -f "nx serve"
./apps/tchalanet-web/start-dev.sh
```

### Interface reste en anglais

1. Vérifier que les fichiers sont copiés
2. Ouvrir la console du navigateur (F12)
3. Vérifier qu'il n'y a pas d'erreur 404
4. Recharger la page avec Cmd+Shift+R (hard refresh)

## 📚 Pour l'app mobile

L'app mobile `tchalanet-mobile` aura le même problème. Appliquer la même solution :

```bash
# Créer le script
cd apps/tchalanet-mobile
cp ../tchalanet-web/copy-i18n.sh .

# Modifier les chemins si nécessaire
# Exécuter
./copy-i18n.sh
```

## 🎯 Solution alternative (future)

Pour éviter la copie manuelle, on pourrait :

1. **Utiliser `vite-plugin-static-copy`** pour copier automatiquement
2. **Configurer un watcher** qui recopie les fichiers quand ils changent
3. **Publier i18n comme package npm** et l'importer

Pour l'instant, la copie au démarrage est la solution la plus simple et fiable.

---

**Date de fix :** 19 novembre 2025  
**Problème :** Assets i18n non copiés avec @nx/vite  
**Solution :** Copie manuelle au démarrage via script  
**Status :** ✅ Fonctionnel

