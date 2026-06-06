# PageModel renderer

Renders the resolved backend `PageRuntimeResponse` contract from `shared/types/pagemodel.types.ts`.
Engine-only: it walks the layout and delegates each widget to its component. Widgets receive only
their own config + resolved dynamic payload + id — never the whole page.

## Flow

`PageModelComponent` (`page-model.component.ts`)
→ iterates `content.layout.rows[].columns[].widgets[]`
→ one `WidgetHostComponent` (`widget-host.component.ts`) per widget id
→ host resolves the component from `WIDGET_REGISTRY` by the backend `type` string and binds
  `config` / `dynamic` / `widgetId`.

Dynamic payloads come from `dynamic.widgets[id]`; contained provider failures from
`dynamic.errors[]` (matched by `widgetId`). The web never receives `binding` or `fileKey`.

## Containment (a widget failure never blanks the page)

| Condition                                  | Result                       |
| ------------------------------------------ | ---------------------------- |
| missing `id` or `config.type`              | invalid-widget fallback      |
| `type` not in registry                     | unsupported-widget fallback  |
| `dynamic.errors` entry for the widget id   | widget-local error block     |
| component instantiation throws             | widget-local error block     |

## Widget registry (`widget-registry.ts`) — V1

| Backend `type`            | Component            | Dynamic source        |
| ------------------------- | ------------------- | --------------------- |
| `HeroWidget`              | `HeroWidget`        | json_file (props)     |
| `NewsTickerWidget`        | `NewsTickerWidget`  | `public_home` `{items}` |
| `FeatureGridWidget`       | `FeatureGridWidget` | json_file `{items}`   |
| `PlansWidget`             | `PlansWidget`       | `public_home` `{plans}` |
| `PublicDrawResultsWidget` | — (out of scope)    | unsupported fallback  |
| `CheckTicketWidget`       | — (out of scope)    | unsupported fallback  |
| `TchalaSearchWidget`      | — (out of scope)    | unsupported fallback  |

Adding a backend widget = register its component here; no other change.

## Translation & theme rules

- `LabelPipe` (`tchLabel`) renders the i18n value for a key, falling back to a stable
  key-derived label when the translation is missing — content is never hidden.
- Widgets style only via validated `--tch-*` tokens (with `--mat-sys-*` fallbacks). No hard-coded
  palette values.
