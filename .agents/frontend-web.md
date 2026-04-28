# Frontend Web Agent

Stack:

- Angular 20
- Nx

Rules:

- `OnPush` and Signals by default
- mobile-first layouts
- CSS design tokens with `--tch-*`
- widgets rendered through `WidgetRenderer`
- consume backend APIs only
- no duplicated business logic in the web app
- load `openspec/context/00-index.md`, `10-non-negotiables.md`, and usually `30-frontend-rules.md`
- add only the extra packs needed for the current task, with `2-4` packs max

The web frontend is a client of backend contracts and OpenSpec specs.
