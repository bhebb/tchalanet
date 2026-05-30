# Fix: Styles SCSS ne se chargeaient pas avec @nx/vite

## 🐛 Problème

Les fichiers SCSS configurés dans `project.json` via l'option `styles: ["apps/tchalanet-web/src/styles.scss"]` ne se chargeaient pas avec `@nx/vite:build` et `@nx/vite:dev-server`.

**Symptômes :**
- ✅ Fonts externes (Google Fonts) se chargeaient
- ❌ Styles internes de l'application ne s'appliquaient pas
- ❌ Aucune erreur dans la console
- ❌ Aucune erreur dans les logs du serveur

## ✅ Solution

L'option `styles` dans `project.json` ne fonctionne pas correctement avec `@nx/vite`. Il faut **importer les fichiers SCSS directement dans `main.ts`**.

### Changements appliqués

#### 1. Suppression de l'option `styles` dans `project.json`

**Avant :**
```json
{
  "options": {
    "styles": ["apps/tchalanet-web/src/styles.scss"]
  }
}
```

**Après :**
```json
{
  "options": {
    // Option styles supprimée
  }
}
```

#### 2. Import direct dans `main.ts`

**Fichier :** `apps/tchalanet-web/src/main.ts`

```typescript
import 'zone.js';
// Import global styles
import './styles.scss';

import { bootstrapApplication } from '@angular/platform-browser';
// ...
```

### Configuration SCSS dans `vite.config.mts`

```typescript
css: {
  preprocessorOptions: {
    scss: {
      api: 'modern',
      // Chemins pour résoudre les imports SCSS
      includePaths: [
        path.resolve(__dirname, '../../'),
        path.resolve(__dirname, '../../libs'),
      ],
    },
  },
},
```

### Chemin relatif dans `styles.scss`

**Fichier :** `apps/tchalanet-web/src/styles.scss`

```scss
// Chemin relatif depuis apps/tchalanet-web/src/ vers libs/ui/styles/src/
@use '../../../libs/ui/styles/src/index.scss' as *;

@import 'https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700&display=swap';
```

## 🎯 Pourquoi ça fonctionne maintenant

1. **Import direct dans `main.ts`** → Vite sait qu'il doit traiter ce fichier SCSS
2. **Chemins relatifs** → Les imports `@use` dans SCSS sont résolus correctement
3. **includePaths configuré** → Sass peut trouver les dépendances dans `libs/`

## 📚 Méthode recommandée par Vite

Selon la documentation Vite, les styles globaux doivent être importés dans le point d'entrée JavaScript (dans notre cas `main.ts`) plutôt que configurés via des options de build.

**Référence :** https://vitejs.dev/guide/features.html#css

## ✅ Vérification

Pour vérifier que les styles se chargent :

```bash
# Dans la console du navigateur (F12)
console.log(document.styleSheets.length); // Devrait être > 0
```

Ou inspecter un élément et vérifier que les styles CSS personnalisés s'appliquent.

## 🔄 Pour d'autres apps Nx + Vite

Si tu as d'autres applications Nx qui utilisent Vite et ont le même problème :

1. Supprimer l'option `styles` de `project.json`
2. Importer les fichiers SCSS/CSS directement dans `main.ts` ou `main.tsx`
3. Configurer `includePaths` dans `vite.config.mts` si nécessaire

---

**Date de fix :** 18 novembre 2025  
**Version Nx :** 20.x  
**Version Vite :** 5.x  
**Problème :** Option `styles` dans `project.json` ignorée par `@nx/vite`  
**Solution :** Import direct dans `main.ts`

