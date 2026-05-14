# Configuration des environnements avec Vite

Ce projet utilise les **variables d'environnement natives de Vite** via les fichiers `.env.*`.

## Fichiers d'environnement disponibles

- `.env.development` - Développement local (IDE)
- `.env.staging` - Environnement de staging
- `.env.production` - Production

## Utilisation

### En développement (local-ide)

```bash
# Par défaut, le mode development charge .env.development
npm run start:web
# ou
npx nx serve tchalanet-portal
# ou explicitement
npx nx serve tchalanet-portal --configuration=development
```

### Pour staging

```bash
npx nx serve tchalanet-portal --configuration=staging
# ou pour build
npx nx build tchalanet-portal --configuration=staging
```

### Pour production

```bash
npx nx build tchalanet-portal --configuration=production
```

```

Vite charge automatiquement le bon fichier `.env.*` selon le mode :
- `--configuration=development` → charge `.env.development`
- `--configuration=staging` → charge `.env.staging`
- `--configuration=production` → charge `.env.production`

Les variables sont accessibles via `import.meta.env.VITE_*` et sont remplacées au moment du build.

Le fichier `environment.ts` lit ces variables :

```typescript
export const environment = {
  apiBase: import.meta.env.VITE_API_BASE || 'https://api.tchalanet.com/api',
  authUrl: import.meta.env.VITE_AUTH_URL || 'https://auth.tchalanet.com/realms/tchalanet',
  // ...
};
```

## Variables disponibles
3. Par défaut : `production`
Toutes les variables doivent commencer par `VITE_` pour être exposées côté client :

### API
- `VITE_API_BASE` - URL base de l'API (ex: `http://localhost:8083/api`)
- `VITE_API_BASE_URL` - URL racine de l'API
- `VITE_APP_URL` - URL de l'application
## Logs
### Auth (Keycloak)
- `VITE_AUTH_URL` - URL du realm Keycloak
- `VITE_AUTH_CLIENT_ID` - Client ID Keycloak
>>> API target: http://localhost:8080
### Feature Flags
- `VITE_FEATURE_KIND` - `memory` ou `unleash`
- `VITE_FEATURE_URL` - URL du serveur Unleash
- `VITE_FEATURE_CLIENT_KEY` - Clé client Unleash
- `VITE_FEATURE_APP_NAME` - Nom de l'application
- `VITE_FEATURE_ENVIRONMENT` - Environnement (development/staging/production)
- `VITE_FEATURE_REFRESH` - Intervalle de rafraîchissement (secondes)
- `VITE_FEATURE_DEFAULT_VALUE` - Valeur par défaut des flags

### Analytics
- `VITE_ANALYTICS_PROVIDER` - Provider analytics (`ga` ou `none`)
- `VITE_GA_MEASUREMENT_ID` - ID Google Analytics
- `VITE_ANALYTICS_AUTO_TRACK` - Auto-tracking activé (`true`/`false`)

### Autres
- `VITE_API_VERSION` - Version de l'API
- `VITE_APP_VERSION` - Version de l'app
- `VITE_ERROR_VERSION` - Version error handling
- `VITE_TENANT` - Tenant par défaut
- `VITE_LANG` - Langue par défaut

## Configuration locale (.env.development)

```bash
VITE_API_BASE=http://localhost:8083/api
VITE_AUTH_URL=https://auth.localtest.me/realms/tchalanet
VITE_FEATURE_KIND=memory  # Pas de serveur Unleash requis
[Vite Env Plugin] Configuration: development

## Avantages de cette approche

✅ **Solution native Vite** - Pas de plugin custom  
✅ **Simple et maintenable** - Fichiers `.env` standard  
✅ **Fonctionne immédiatement** - Pas de configuration complexe  
✅ **Type-safe** - `import.meta.env` avec types TypeScript  
✅ **Sécurisé** - Seules les variables `VITE_*` sont exposées  

## Notes importantes

⚠️ Les variables d'environnement sont **remplacées au moment du build**  
⚠️ Ne JAMAIS mettre de secrets sensibles dans les variables `VITE_*`  
⚠️ Les fichiers `.env.local` sont ignorés par git (pour overrides locaux)  
