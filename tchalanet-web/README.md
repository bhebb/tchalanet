# Tchalanet Web

Workspace frontend Nx/Angular de Tchalanet.

La racine frontend est désormais `tchalanet-web/`.
L’application principale s’appelle `tchalanet-portal` et vit dans
`apps/tchalanet-portal/`.

## Commandes principales

Depuis `tchalanet-web/` :

```bash
pnpm install
pnpm nx lint tchalanet-portal
pnpm nx test tchalanet-portal
pnpm nx build tchalanet-portal
pnpm nx serve tchalanet-portal
```

## Structure

- `apps/tchalanet-portal/` — portail web principal multi-rôle
- `apps/tchalanet-portal-e2e/` — tests end-to-end
- `libs/` — bibliothèques frontend partagées
- `openspec/` — OpenSpec projet-local web
- `docs/web/` — règles d’architecture frontend


## Architecture docs

**Important : Avant toute modification, lire :**

- `docs/web/WEB_DEV_ARCHITECTURE.md` — conventions de développement, structure cible, checklist, commande Nx recommandée
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
- Les features spécifiques à `tchalanet-portal` peuvent rester dans `apps/tchalanet-portal/src/app/features` au début.
- Les features sont extraites en libs Nx seulement quand elles deviennent grosses, partagées ou stratégiques.
