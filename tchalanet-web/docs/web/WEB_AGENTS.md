
# Règles pour agents IA — Tchalanet Web (2026)

Ce document définit les règles pour les agents IA qui modifient le frontend Angular/Nx.

## 1. Architecture cible

Respecter la structure suivante :

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

Ne jamais créer de micro-lib ou de nouveau dossier top-level sans justification claire et validation humaine.

## 2. Placement des fichiers

- Contrats, modèles, clients HTTP : `api/`
- Auth, guards, login : `shared-auth/`
- I18n, loader, switcher : `shared-i18n/`
- Config, env, feature flags : `shared-config/`
- Composants visuels réutilisables : `ui/`
- Moteur PageModel, rendering, state : `page-model/`
- Widgets dynamiques : `widgets/`
- Pages routées, shells, containers : `web/`

## 3. Conventions et suffixes

- Page routée : `*.page.ts`
- Container interne : `*.container.ts`
- Composant visuel : `*.component.ts`
- Widget dynamique : `*.widget.ts`
- Shell global : `*.shell.ts`

## 4. Tags Nx et dépendances

- Taguer chaque lib avec : `type:<lib>` et `scope:<domaine>`
- Respecter les dépendances Nx :
	- `web` peut dépendre de `ui`, `widgets`, `api`, `shared-*`
	- `ui` ne dépend que de `shared-*`
	- `widgets` peut dépendre de `ui`, `api`, `shared-*`
	- `api` ne dépend que de `shared-*`
	- `shared-*` ne dépend que de code générique

## 5. Interdits

- Ne pas créer de nouvelle lib sans frontière claire et stable
- Ne pas mettre de logique métier critique dans le frontend
- Ne pas faire dépendre `ui` de `data-access` ou `api`
- Ne pas faire dépendre `widgets` de `web`
- Ne pas mettre de composants visuels dans `shared-*`
- Ne pas mettre d’appel HTTP dans un composant UI

## 6. Quand extraire une feature en lib Nx

Seulement si :

- partagée par plusieurs apps
- grosse ou stratégique
- besoin de tests/build séparés
- besoin de boundaries Nx propres

## 7. Références

- `WEB_DEV_ARCHITECTURE.md` — conventions dev et agents
- `frontend-architecture-todo.md` — mapping détaillé
