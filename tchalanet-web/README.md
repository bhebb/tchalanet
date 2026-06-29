# Tchalanet Web

Workspace frontend Nx/Angular de Tchalanet.

La racine frontend est désormais `tchalanet-web/`.
Le workspace est découpé en apps déployables indépendamment:

- `apps/public-portal/`
- `apps/admin-portal/`
- `apps/platform-portal/`

## Commandes principales

Depuis `tchalanet-web/` :

```bash
pnpm install
pnpm lint
pnpm test
pnpm build:public-portal
pnpm build:admin-portal
pnpm build:platform-portal
```

## Lancer les apps en local

Choisir d'abord le profil runtime:

```bash
pnpm runtime:local-ide
```

Profils disponibles:

```bash
pnpm runtime:local-ide          # API http://localhost:8083/api/v1, Firebase réel
pnpm runtime:local-ide-emulator # API http://localhost:8083/api/v1, Firebase Auth emulator
pnpm runtime:dev-docker         # API https://api.localtest.me/api/v1, Firebase réel
pnpm runtime:dev-docker-emulator
pnpm runtime:stg-vercel
pnpm runtime:prod-vercel
```

Lancer une app:

```bash
pnpm serve:public-portal
pnpm serve:admin-portal
pnpm serve:platform-portal
```

Lancer les trois apps en même temps avec des ports séparés:

```bash
pnpm nx run public-portal:serve --port=4200
pnpm nx run admin-portal:serve --port=4201
pnpm nx run platform-portal:serve --port=4202
```

## Structure

- `apps/public-portal/` — portail public
- `apps/admin-portal/` — portail admin tenant
- `apps/platform-portal/` — portail platform/superadmin
- `libs/api/` — contrats et infrastructure HTTP transverses
- `libs/shared-config/` — settings runtime et feature flags
- `libs/shared-assets/` — assets publics partagés, runtime config, logos, i18n/assets communs
- `libs/core/auth/` — auth, login partagé, guards, access/entitlements
- `libs/core/i18n/` — internationalisation partagée
- `libs/ui/theme/` — thème runtime, presets et application des tokens
- `libs/ui/styles/` — primitives SCSS partagées et overrides globaux
- `libs/ui/components/` — composants UI réutilisables
- `libs/web/errors/` — présentation et normalisation UI des erreurs web
- `libs/web/shell/` — shells publics/privés/platform
- `libs/web/sandbox/` — sandbox de test theme/dev
- `libs/` — bibliothèques frontend partagées
- `openspec/` — OpenSpec projet-local web
- `docs/web/` — règles d’architecture frontend


## Architecture docs

**Important : Avant toute modification, lire :**

- `docs/web/WEB_DEV_ARCHITECTURE.md` — conventions de développement, structure cible, checklist, commande Nx recommandée
- `docs/ARCHITECTURE.md` — état actif et trajectoire d’extraction des libs
- `docs/web/WEB_AGENTS.md` — règles pour agents IA (placement, interdits, tags Nx)
- `docs/web/frontend-architecture-todo.md` — mapping détaillé, migration, rôles des libs

Autres documents :

- `docs/web/WEB_ARCHITECTURE.md` — modèle mental, familles, structure cible
- `docs/web/WEB_STATE_MANAGEMENT.md` — state placement rules
- `docs/web/WEB_NX_BOUNDARIES.md` — Nx tags et dépendances
- `docs/web/WEB_FEATURE_PLAYBOOK.md` — workflow feature
- `docs/web/WEB_PLACEMENT_GUIDE.md` — où placer chaque concept
- `docs/web/WEB_NAMING.md` — conventions de nommage frontend
- `openspec/context/90-web-rules.md` — règles normatives compactes
- `examples/` — snippets ESLint, feature template, state template

## Références officielles

- Angular Style Guide — https://angular.dev/style-guide
- Angular Workspace & Project File Structure — https://angular.dev/reference/configs/file-structure
- Nx Project Dependency Rules — https://nx.dev/docs/concepts/decisions/project-dependency-rules
- Nx Enforce Module Boundaries — https://nx.dev/docs/features/enforce-module-boundaries
- Nx Tag in Multiple Dimensions — https://nx.dev/docs/guides/enforce-module-boundaries/tag-multiple-dimensions

## Décision actuelle

Tchalanet Web peut rester dans Nx, mais avec un découpage léger :

- Les blocs transverses réutilisables peuvent être des libs Nx.
- Les features spécifiques à une app vivent dans l'app cible.
- Les features sont extraites en libs Nx seulement quand elles deviennent grosses, partagées ou stratégiques.
