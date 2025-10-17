
# Tchalanet

Projet monorepo Tchalanet comprenant les applications web, mobile, le backend et la documentation.

## Structure du projet

- **tchalanet-web** : Application web Angular avec Nx
- **tchalanet-mobile** : Application mobile Ionic Angular
- **tchalanet-server** : Backend Spring Boot
- **tchalanet-docs** : Documentation technique (MkDocs)
- **tchalanet-infra** : Configuration d'infrastructure

## Démarrage rapide

### Web (Angular + Nx)



docker compose -f docker-compose.yml -f docker-compose.dev.yml down -v
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d pg
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d keycloak
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d api

ng new tchalanet-web --style scss --skip-git --prefix tchala --inline-style --inline-template --directory tchalanet-web
cd tchalanet-web

# Ng-Matero (Material)
npm i @angular/material @angular/cdk @ng-matero/extensions

# Tailwind + DaisyUI
npm i -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
npm i daisyui

# Auth Keycloak
npm i keycloak-js

# i18n
npm i @ngx-translate/core @ngx-translate/http-loader

# NgRx
ng add @ngrx/store@latest
ng add @ngrx/effects@latest
ng add @ngrx/store-devtools@latest
npm i @ngrx/router-store

# Mettre à jour le README avec les nouvelles commandes
cat >> README.md << 'EOF'

## Développement Frontend

### Application Web
```bash
# Démarrer l'application web en mode développement
npm run start:web

# Construire l'application web pour la production
npm run build:web

# Exécuter les tests unitaires
npm run test:web

# Exécuter les tests e2e
npm run e2e:web

# Démarrer l'application mobile en mode développement
npm run start:mobile

# Construire l'application mobile
npm run build:mobile

# Synchroniser avec Capacitor
npm run cap:sync

# Ouvrir dans Android Studio
npm run cap:android

# Ouvrir dans Xcode
npm run cap:ios

# Exécuter les tests unitaires
npm run test:mobile

# Exécuter les tests e2e
npm run e2e:mobile

```

nx g @nx/angular:lib --name=shared/types --directory=libs/shared/types --prefix=tchl --tags=scope:shared,type:types --standalone
nx g @nx/angular:lib --name=shared/util-i18n --directory=libs/shared/util-i18n --tags=scope:shared,type:util --standalone
nx g @nx/angular:lib --name=shared/ui-shell-material --directory=libs/shared/ui-shell-material --tags=scope:shared,type:ui --standalone
nx g @nx/angular:lib --name=web/feature-home-public --directory=libs/web/feature-home-public  --tags=scope:web,type:feature --standalone
nx g @nx/angular:lib --name=web/feature-home-private --directory=libs/web/feature-home-private  --tags=scope:web,type:feature --standalone
nx g @nx/angular:lib --name=shared/data-access --directory=libs/shared/data-access/page --prefix=tchp --tags=scope:shared,type:data-access --standalone
