# Tchalanet Web Architecture Docs

Première version des règles d'architecture web Tchalanet.

Objectif : donner aux développeurs et agents IA un modèle mental simple, aligné avec Angular/Nx, sans sur-ingénierie.

## Documents

- `docs/web/WEB_ARCHITECTURE.md` — modèle mental, couches, responsabilités.
- `docs/web/WEB_STATE_MANAGEMENT.md` — règles Signals / SignalStore / NgRx.
- `docs/web/WEB_NX_BOUNDARIES.md` — tags Nx et règles de dépendances.
- `docs/web/WEB_FEATURE_PLAYBOOK.md` — comment ajouter une feature.
- `docs/web/WEB_PLACEMENT_GUIDE.md` — où placer auth, header, PageModel, widgets, i18n, etc.
- `docs/web/WEB_NAMING.md` — conventions de nommage frontend.
- `docs/web/WEB_AGENTS.md` — règles pour agents IA.
- `openspec/context/90-web-rules.md` — règles normatives compactes.
- `examples/` — snippets ESLint, feature template, state template.

## Références officielles

- Angular Style Guide — https://angular.dev/style-guide
- Angular Workspace & Project File Structure — https://angular.dev/reference/configs/file-structure
- Nx Project Dependency Rules — https://nx.dev/docs/concepts/decisions/project-dependency-rules
- Nx Enforce Module Boundaries — https://nx.dev/docs/features/enforce-module-boundaries
- Nx Tag in Multiple Dimensions — https://nx.dev/docs/guides/enforce-module-boundaries/tag-multiple-dimensions

## Décision actuelle

Tchalanet Web peut rester dans Nx, mais avec un découpage léger :

- Les blocs transverses réutilisables peuvent être des libs Nx.
- Les features spécifiques à `tch-web` peuvent rester dans `apps/tch-web/src/app/features` au début.
- Les features sont extraites en libs Nx seulement quand elles deviennent grosses, partagées ou stratégiques.
