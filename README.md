
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
