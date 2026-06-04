# PageModel Convention

> Status: DRAFT v0.2
> Scope: dynamic page payloads, widget renderer, actions
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

PageModel describes page structure. It does not carry i18n bundles, theme definitions, settings
payloads, or authorization rules. Runtime bootstrap loads PageModel alongside settings, i18n, and
theme as separate concerns.

**Type the real backend contract — do not invent a parallel widget vocabulary.** The web models the
backend `PageModelDoc` shape directly. Earlier proposals for an abstract normalized widget enum
(`HERO/ACTION_PANEL/…`) plus a "legacy mapping" registry are rejected: they drift from the backend.

## Real backend contract (`PageModelDoc`)

Source of truth: `core/pagemodel/api/model/PageModelDoc.java` (backend). The web contract type must
mirror it field-for-field.

```text
PageModelDoc {
  meta    { id, scope, slug, context, schema_version, langs[], default_lang }
  theme   { presetId, mode, density, overrides{} }
  shell   { header, sidenav, footer }            // each: { component, binding{mode,source}, nav, props }
  content {
    layout  { component, rows[ { id, label_key, columns[ { span, widgets: string[] } ] } ] }
    widgets Map<widgetId, { type, binding{mode,source}, props }>
  }
}
```

Critical shape facts:

- **Layout references widget *ids*** (strings). The widget definitions live in the separate
  `content.widgets` map, keyed by that id. The renderer joins them.
- **Dynamic payload is delivered separately** from the page document, in the response envelope:
  `PublicPageModelResponse { currentLang, langs, pageModel, dynamic }` where
  `dynamic = PageDynamicPayload { widgets: Map<widgetId, payload>, errors: WidgetDynamicError[] }`.
  A widget's runtime data is `dynamic.widgets[widgetId]`; its contained failure (if any) is the
  matching entry in `dynamic.errors`.
- `binding.mode` is `static | dynamic`; `binding.source` names the provider (`json_file`,
  `public_home`, `public_draw_results`, …). Static widgets render from `props` alone.
- Dashboard responses add `notifications` (`DashboardPageModelResponse`).

## Widget registry

Map the concrete backend `type` string straight to an Angular component via a registry
(`TCH_WIDGET_REGISTRY` multi-provider, factory `type → component`). The registry key **is** the
backend type string, e.g. `"HeroWidget"`, `"NewsTickerWidget"`, `"PlansWidget"`,
`"FeatureGridWidget"`. No normalization layer.

Each widget component receives only: its own `WidgetConfig` (`type`, `props`), its resolved
`dynamic.widgets[id]` payload, and its local error. It must **not** receive the whole page object.

### V1 supported widgets (this slice)

Public page, static-first: `HeroWidget`, `NewsTickerWidget`, `FeatureGridWidget`, `PlansWidget`
(+ shell header/footer). The seeded `public.home` also contains `PublicDrawResultsWidget`,
`CheckTicketWidget`, `TchalaSearchWidget` — these render the **unsupported-widget fallback** for now
and land in a later slice.

## Fallbacks (renderer must handle)

- **Unsupported type** (no registry entry) → contained fallback, page keeps rendering.
- **Invalid widget** (missing `id` or `type`) → invalid-widget fallback.
- **Widget-local error** (`dynamic.errors[id]`) → error rendered inside the widget only.
- A single widget failure must never blank the page.

## Text — i18n-first (norm)

Every string is an i18n key from the start (fr/en/ht), never hardcoded copy. PageModel carries keys
(`title_key`, `label_key`, `description_key`, …); the renderer resolves them through the i18n
runtime. Missing translation → render a stable key-derived fallback, keep the widget visible. See
[`i18n.md`](./i18n.md).

## Theme — validated tokens only

Widgets style exclusively through the validated `--tch-*` CSS variables (see [`theme.md`](./theme.md)).
`PageModelDoc.theme.presetId` selects a preset; the theme runtime materializes its tokens. Do not
hardcode colors and do not import legacy preset palettes.

## Gating

A PageModel may reference a feature-flag key, but never the flag value — the frontend runtime decides
rendering via `*tchFeature` / `featureGuard`. Paid/entitlement gating is separate from feature flags.
See [`settings.md`](./settings.md).

## Actions

Actions are descriptors; the renderer routes them to known handlers (`link`, `route`, `command`,
`externalLink`). Sensitive mutations are executed by owning endpoints, never by the PageModel
renderer. Unknown action types render disabled or are omitted per the widget contract.

> Domain note: "onboard seller" = create a `CASHIER` user (`role=CASHIER`, `outletId` required), not
> a `SELLER` role. Action forms target the owning endpoint (`POST /admin/identity/users`).

## Failure behavior

- public routes show a fallback page or error panel;
- private routes show a recoverable loading/error state;
- the app shell remains usable when possible.

## Anti-patterns

Do not:

- invent an abstract widget vocabulary that diverges from the backend `type` strings;
- pass the full page object into a widget;
- let arbitrary backend widget types instantiate Angular classes outside the registry;
- put business mutations in generic widget renderers;
- let PageModel decide auth, or carry translation maps / theme tokens / settings values;
- create a widget library before a real page needs the widget.
