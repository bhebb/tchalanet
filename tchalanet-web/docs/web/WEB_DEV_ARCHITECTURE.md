## Guide de démarrage rapide

1. Initialiser le workspace (si besoin) :
  ```bash
  pnpm init # si package.json absent
  npx create-nx-workspace@latest . # si Nx pas encore initialisé
  pnpm install
  ```

2. Installer le plugin Angular pour Nx :
  ```bash
  pnpm add -D @nx/angular
  ```

3. Générer l’application principale (sans --changeDetection) :
  ```bash
  pnpm nx g @nx/angular:app --name=tch-portal --directory=apps/tch-portal --routing --style=scss --prefix=tch --standalone --unitTestRunner=vitest-angular --e2eTestRunner=playwright --tags=lottery,haiti,borlette
  ```

4. Démarrer le serveur de dev :
  ```bash
  pnpm nx serve tch-portal
  ```

5. Lancer les tests unitaires (Vitest) :
  ```bash
  pnpm nx test tch-portal
  ```

6. Lancer les tests end-to-end (Playwright) :
  ```bash
  pnpm nx e2e tch-portal-e2e
  ```

Respecte ensuite la structure cible et les conventions de la doc pour créer libs/pages/features.
> Angular/Nx web app.
or needs isolated boundaries and tests.

# Architecture de développement frontend — Tchalanet

> Version : 2026-05 — Architecture cible Nx/Angular

## 1. Objectif

Stabiliser l’architecture Angular Nx de `tchalanet-web` avant de développer massivement les pages public, admin, cashier/POS et superadmin.

Le but est de réduire le nombre de libs, clarifier les responsabilités, éviter les abstractions prématurées et poser une base cohérente avec l’architecture backend Tchalanet.

---

## 2. Décision de base

Démarrer avec un nombre limité de libs stables :

```text
libs/
  api/
  shared-auth/
  shared-i18n/
  shared-config/
  ui/
  page-model/
  widgets/
  web/
```

Ne pas créer une lib pour chaque petit composant, widget ou facade.
Une lib Nx doit exister seulement si elle porte une frontière claire, stable et utile.

---

## 3. Rôle des libs

Voir frontend-architecture-todo.md pour le détail des responsabilités de chaque lib.

Résumé :

- `api` : contrats backend/frontend, modèles, clients HTTP, interceptors
- `shared-auth` : auth, guards, login, secure storage
- `shared-i18n` : i18n, loader, switcher
- `shared-config` : env, feature flags, settings
- `ui` : design system, composants visuels, layout, feedback, actions, forms, status
- `page-model` : moteur PageModel, state, rendering, facade
- `widgets` : registry, widgets dynamiques, public/private/cashier/admin
- `web` : pages routées, shells, containers, assemblage écran

---

## 4. Convention Page / Container / Component / Widget

```text
Route -> Page -> Container(s) -> Component(s)
```

- Page : composant routé, suffixe `*.page.ts`, layout principal, peut injecter facade/store/router
- Container : interne à une Page, suffixe `*.container.ts`, orchestre une sous-zone logique
- Component : visuel, suffixe `*.component.ts`, reçoit `input()`, émet `output()`, stateless
- Widget : rendu dynamiquement par PageModel, suffixe `*.widget.ts`, reçoit des props
- Shell : structure globale d’une surface, suffixe `*.shell.ts`

---

## 5. Checklist développement

- Ne pas créer de nouvelle lib sans frontière claire et stable
- Placer les pages dans `web/` (public, private, cashier, admin, etc.)
- Placer les widgets dynamiques dans `widgets/`
- Placer les composants visuels réutilisables dans `ui/`
- Placer les contrats, modèles, clients HTTP dans `api/`
- Placer l’auth dans `shared-auth/`, l’i18n dans `shared-i18n/`, la config dans `shared-config/`
- Utiliser les tags Nx pour chaque lib :
  - `type:api|ui|web|widgets|page-model|shared-auth|shared-i18n|shared-config`
  - `scope:public|private|cashier|admin|platform|shared`
- Respecter les dépendances Nx :
  - `web` peut dépendre de `ui`, `widgets`, `api`, `shared-*`
  - `ui` ne dépend que de `shared-*`
  - `widgets` peut dépendre de `ui`, `api`, `shared-*`
  - `api` ne dépend que de `shared-*`
  - `shared-*` ne dépend que de code générique

---

## 6. Commande Nx recommandée pour une nouvelle app

```bash
pnpm nx g @nx/angular:app tch-portal \
  --directory=apps/tch-portal \
  --routing \
  --style=scss \
  --prefix=tch \
  --standalone \
  --unitTestRunner=vitest \
  --e2eTestRunner=playwright \
  --changeDetection=OnPush \
  --tags=lottery,haiti,borlette
```

---

## 7. Liens et docs complémentaires

- `frontend-architecture-todo.md` — plan détaillé, mapping, migration
- `WEB_ARCHITECTURE.md` — modèle mental, familles, structure cible
- `WEB_AGENTS.md` — règles pour agents IA
- `WEB_STATE_MANAGEMENT.md` — state placement
- `WEB_NX_BOUNDARIES.md` — Nx tags et dépendances
- `WEB_FEATURE_PLAYBOOK.md` — workflow feature
- `WEB_PLACEMENT_GUIDE.md` — où placer chaque concept
