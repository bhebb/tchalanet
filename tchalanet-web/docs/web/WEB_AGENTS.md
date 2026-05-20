# Web Agent Rules — Tchalanet

> Status: DRAFT v0.1

Ce document donne les règles minimales pour les agents IA qui modifient le frontend.

## 1. Règle centrale

```text
core connecte l'app.
features composent les écrans.
data-access parle au backend.
ui dessine.
shared aide.
```

## 2. Avant de créer un fichier

Choisir une seule maison :

```text
Global technique ? core
Écran/route/flow ? features
API/backend/state réutilisable ? data-access
Composant visuel pur ? ui
Utilitaire générique ? shared
```

## 3. Interdits

- Ne pas créer de nouveau top-level folder sans validation.
- Ne pas mettre `AuthService` dans `shared`.
- Ne pas mettre `HttpClient` dans un composant de feature si une lib `data-access` existe.
- Ne pas mettre des composants visuels dans `shared`.
- Ne pas faire dépendre `ui` de `data-access`.
- Ne pas faire dépendre `data-access` de `features`.
- Ne pas recoder des règles métier critiques côté frontend.

## 4. Feature

Par défaut :

```text
apps/tch-web/src/app/features/<scope>/<feature>
```

Ne pas créer `components/`, `pages/`, `state/` automatiquement. Rester plat au début.

## 5. Header / Footer / Sidebar

```text
visuel pur -> ui/layout
connecté auth/nav/tenant -> core/shell
```

## 6. PageModel

```text
API/models -> data-access/page-model
renderer -> ui/page-renderer
widgets -> ui/widgets
runtime public -> features/public/dynamic-page
admin editor -> features/platform/page-models
```

## 7. State

```text
local simple -> signal dans composant
écran -> feature store
API réutilisable -> data-access state
global -> core
```

## 8. Quand extraire une feature en lib Nx

Seulement si :

```text
partagée par plusieurs apps
grosse
stratégique
besoin de tests/build séparés
besoin de boundaries Nx propres
```
