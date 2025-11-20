# Migration vers variables d'environnement Vite - Récapitulatif

## ✅ Ce qui a été fait

### 1. Fichiers `.env.*` créés

| Fichier | Mode Vite | Usage | API URL |
|---------|-----------|-------|---------|
| `.env` | (fallback) | Valeurs par défaut communes | - |
| `.env.development` | `development` | Développement local | `http://localhost:8083/api` |
| `.env.staging` | `staging` | Environnement de staging | `https://api.stg.tchalanet.com/api` |
| `.env.production` | `production` | Production | `https://api.tchalanet.com/api` |
| `.env.local.example` | - | Exemple pour overrides locaux | - |

### 2. Configuration Nx (`project.json`)

Chaque configuration (build + serve) a maintenant un paramètre `mode` :

```json
{
  "configurations": {
    "development": {
      "mode": "development"  // ✅ Charge .env.development
    },
    "staging": {
      "mode": "staging"      // ✅ Charge .env.staging
    },
    "production": {
      "mode": "production"   // ✅ Charge .env.production
    }
  }
}
```

### 3. `environment.ts` modifié

Au lieu de valeurs hardcodées, lit maintenant depuis `import.meta.env` :

```typescript
export const environment = {
  apiBase: import.meta.env.VITE_API_BASE || 'https://api.tchalanet.com/api',
  authUrl: import.meta.env.VITE_AUTH_URL || 'https://auth.tchalanet.com/realms/tchalanet',
  // ...
};
```

### 4. Types TypeScript (`vite-env.d.ts`)

Créé dans :
- `apps/tchalanet-web/src/vite-env.d.ts`
- `libs/shared/config/src/vite-env.d.ts`

Déclare toutes les variables `VITE_*` pour l'auto-complétion et la validation TypeScript.

### 5. Suppression des anciens systèmes

- ❌ `fileReplacements` supprimés de `project.json`
- ❌ Plugin Vite personnalisé supprimé (`vite-env-plugin.ts`)
- ✅ Fichiers `environment-*.ts` gardés (pour référence, peuvent être supprimés)

## 🚀 Utilisation

### Commandes

```bash
# Development (par défaut)
npx nx serve tchalanet-web
npx nx serve tchalanet-web --configuration=development

# Staging
npx nx serve tchalanet-web --configuration=staging
npx nx build tchalanet-web --configuration=staging

# Production
npx nx build tchalanet-web --configuration=production
```

### Comment ça fonctionne

1. Nx exécute avec `--configuration=X`
2. La configuration `X` dans `project.json` spécifie `"mode": "X"`
3. Vite charge automatiquement `.env.X`
4. Les variables `VITE_*` sont accessibles via `import.meta.env`
5. Au moment du build, Vite remplace `import.meta.env.VITE_*` par les valeurs

### Ordre de chargement des fichiers .env

Vite charge les fichiers dans cet ordre (du plus prioritaire au moins prioritaire) :

1. `.env.{mode}.local` (ignoré par git, pour overrides locaux)
2. `.env.{mode}` (ex: `.env.development`)
3. `.env.local` (ignoré par git, pour overrides locaux communs)
4. `.env` (fallback pour toutes les configurations)

## 📝 Variables disponibles

### Configuration Development (`.env.development`)

```bash
VITE_API_BASE=http://localhost:8083/api
VITE_AUTH_URL=https://auth.localtest.me/realms/tchalanet
VITE_FEATURE_KIND=memory  # ✅ Pas de serveur Unleash requis
```

### Configuration Staging (`.env.staging`)

```bash
VITE_API_BASE=https://api.stg.tchalanet.com/api
VITE_AUTH_URL=https://auth.stg.tchalanet.com/realms/tchalanet
VITE_FEATURE_KIND=unleash
VITE_FEATURE_URL=https://flags.stg.tchalanet.com/api/frontend
```

### Configuration Production (`.env.production`)

```bash
VITE_API_BASE=https://api.tchalanet.com/api
VITE_AUTH_URL=https://auth.tchalanet.com/realms/tchalanet
VITE_FEATURE_KIND=unleash
VITE_FEATURE_URL=https://flags.tchalanet.com/api/frontend
```

## 🔒 Sécurité

⚠️ **IMPORTANT** :
- Seules les variables préfixées par `VITE_` sont exposées au client
- **NE JAMAIS** mettre de secrets (API keys, tokens) dans les variables `VITE_*`
- Les fichiers `.env.local` et `.env.*.local` sont ignorés par git (ajoutés au `.gitignore`)

## 📚 Documentation

- **Configuration détaillée** : `ENVIRONMENT-CONFIG.md`
- **Documentation Vite** : https://vitejs.dev/guide/env-and-mode.html
- **Nx + Vite** : https://nx.dev/recipes/vite/configure-vite

## ✅ Avantages de cette approche

| Avant (fileReplacements) | Après (fichiers .env) |
|---------------------------|----------------------|
| ❌ Complexe avec Vite | ✅ Solution native Vite |
| ❌ Fichiers dupliqués | ✅ Un seul fichier `environment.ts` |
| ❌ Build parfois échoue | ✅ Fonctionne immédiatement |
| ❌ Difficile à déboguer | ✅ Simple et prévisible |

## 🧪 Tests

Pour vérifier que la bonne configuration est chargée :

```typescript
// Dans la console du navigateur
console.log('API Base:', import.meta.env.VITE_API_BASE);
console.log('Feature Kind:', import.meta.env.VITE_FEATURE_KIND);
console.log('Mode:', import.meta.env.MODE); // 'development', 'staging', ou 'production'
```

## 🔄 Migration des autres apps

Pour migrer `tchalanet-mobile` ou d'autres apps :

1. Créer les fichiers `.env.*` dans le dossier de l'app
2. Modifier `environment.ts` pour lire depuis `import.meta.env`
3. Ajouter `vite-env.d.ts` avec les types
4. Ajouter `"mode": "X"` dans les configurations de `project.json`
5. Tester avec `npx nx serve app-name --configuration=development`

---

**Date de migration** : 18 novembre 2025  
**Version Vite** : Compatible avec Vite 5+  
**Version Nx** : Compatible avec Nx 20+

