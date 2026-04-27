# CLAUDE.md — apps/tchalanet-web

> **Lire d'abord** : `../../CLAUDE.md` (règles transverses, secrets, OpenSpec)

---

## Stack frontend

| Item    | Valeur                                       |
| ------- | -------------------------------------------- |
| Angular | 20.2.4 — standalone uniquement               |
| Nx      | 21.4.1 (pnpm 10.19, Node 20.19)              |
| State   | NgRx store + effects + router-store          |
| UI      | Angular Material 20.2 · SCSS + CSS variables |
| i18n    | @ngx-translate (fr / en / ht)                |
| Tests   | Vitest (unit) · Playwright (E2E)             |

---

## Skills web (`apps/tchalanet-web/.claude/skills/`)

`angular-nx` · `frontend-web`

---

## Do ✅

- Composants `standalone: true` + `ChangeDetectionStrategy.OnPush`
- Control flow moderne : `@if`, `@for`, `@switch` (jamais `*ngIf`/`*ngFor`)
- Signaux (`signal()`, `computed()`, `effect()`) quand applicable
- CSS variables pour toutes les couleurs — mobile-first (480/768/1024)
- Clés i18n en `snake_case` avec namespaces, 3 locales en sync
- NgRx feature stores + façades pour isoler les composants
- Widget renderer pour le rendu des page models (`libs/ui/widget-renderer/`)

## Don't ❌

- NgModules · `*ngIf` / `*ngFor` · couleurs hardcodées
- Strings hardcodées dans les templates · logique métier dans les widgets
- Logique dupliquée entre pages et widgets

---

## Commandes Nx

```bash
pnpm install                        # install dépendances
nx serve tchalanet-web              # dev server (proxy → API)
nx test tchalanet-web               # tests unitaires Vitest
nx e2e tchalanet-web-e2e            # tests E2E Playwright
nx build tchalanet-web --prod       # build production
nx affected --target=test           # tests des libs impactées
```

---

## Structure libs

`libs/shared/` auth · api · config · types · facades · data-access
`libs/ui/` theme · styles · layout · widget-renderer
`libs/web/` pages · widgets · shell
`libs/i18n/` traductions (fr / en / ht)
