# Design: Simplified PageModel Runtime Contract

## Context

### Context packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/30-frontend-rules.md`

### Near-code references

- `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/pagemodel/FEATURE_PAGEMODEL.md`
- `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/pagemodel/api/model/PageModelDoc.java`
- `tchalanet-web/docs/conventions/pagemodel.md`
- `tchalanet-web/apps/tch-portal/src/app/shared/types/pagemodel.types.ts`

## 1. Boundary decision

The stored/resolved PageModel definition and the frontend runtime contract are different models.

```text
catalog template / core PageModel
  -> internal PageDefinition
  -> features.pagemodel runtime assembler
  -> external PageRuntimeResponse
  -> Angular renderer
```

`PageDefinition` is the architectural name for the internal model. The first implementation may
retain the existing Java name `PageModelDoc` to limit lifecycle-domain churn, provided it is never
serialized directly by runtime endpoints.

`PageRuntimeResponse` is owned by `features.pagemodel`. It is the only PageModel shape consumed by
Angular.

## 2. Runtime response

```ts
export interface PageRuntimeResponse {
  readonly meta: PageMeta;
  readonly theme?: PageThemeHint;
  readonly shell: PageShellRuntime;
  readonly content: PageContentRuntime;
  readonly dynamic?: PageDynamicPayload;
}
```

```ts
export interface PageMeta {
  readonly logicalId: string;
  readonly scope: 'public' | 'private';
  readonly slug: string;
  readonly schemaVersion: number;
}
```

Rules:

- Runtime JSON field names are camelCase.
- Runtime UI ids and widget ids use camelCase segments. Stable logical ids and i18n keys may retain
  their established dotted/snake_case forms.
- `theme` is an optional page hint/fallback. `ThemeApi`/theme bootstrap remains the runtime source
  of truth.
- Request-level `notices` and `services` live only on the existing `ApiResponse` envelope.
- `binding`, provider source names, `fileKey`, resource paths, and storage metadata are forbidden.
- Runtime errors are sanitized and may identify the affected widget, but never expose internal
  provider or storage details.
- Public and dashboard endpoints use the same core runtime shape. Role/scope resolution remains
  server-side.

### Metadata classification

The template envelope contains more attributes than the runtime needs. The assembler applies this
explicit classification:

| Attribute | Runtime | Reason |
|---|---:|---|
| `logicalId` | yes | Stable functional identity for diagnostics, caching, and renderer tests |
| `scope` | yes | Identifies the resolved surface scope |
| `slug` | yes | Stable page semantic name |
| `schemaVersion` | yes | Enables runtime compatibility checks |
| `code` | no | Template catalog/admin identifier; duplicates functional identity |
| `name` | no | Administrative display name, not translated page content |
| `label` | no | Administrative label; rendered copy must use content i18n keys |
| `schema` | no | Internal definition validation schema |
| `isDefault` | no | Template selection detail already resolved by the backend |
| `level` | no | Catalog ownership/override detail |
| `description`, `tenantId`, timestamps, persistence id | no | Admin/storage metadata |

These attributes remain available through catalog/admin contracts where needed. Runtime endpoints
must not serialize the full `PageModelTemplateView` or template envelope.

## 3. Typed shell

The shell is a discriminated union.

```ts
type PageShellRuntime = PublicShellRuntime | PrivateShellRuntime;

interface PublicShellRuntime {
  readonly type: 'public';
  readonly header: PublicHeaderRuntime;
  readonly footer: PublicFooterRuntime;
}

interface PrivateShellRuntime {
  readonly type: 'private';
  readonly topAppBar: TopAppBarRuntime;
  readonly navigationDrawer: NavigationDrawerRuntime;
}
```

The backend resolves JSON fragments before serialization. The frontend never reads shell content
from `dynamic.widgets`, never receives a shell `component`, and never infers that a private shell is
stored in a header field.

## 4. Navigation and actions

All navigable actions use one destination contract:

```ts
type NavigationDestination =
  | { readonly kind: 'route'; readonly value: string; readonly requiredRoles?: readonly string[] }
  | { readonly kind: 'url'; readonly value: string; readonly requiredRoles?: readonly string[] };
```

`ActionItem` uses camelCase and may contain `destination`. The runtime does not expose parallel
`path`, `href`, `route`, or `url` fields. Angular maps:

- `destination.kind === 'route'` to `routerLink`;
- `destination.kind === 'url'` to an external `href`.

## 5. Content and widget data

```ts
interface PageContentRuntime {
  readonly layout: PageLayout;
  readonly widgets: Readonly<Record<string, WidgetConfig>>;
}

interface WidgetConfig {
  readonly type: string;
  readonly props?: unknown;
}

interface PageDynamicPayload {
  readonly widgets?: Readonly<Record<string, unknown>>;
  readonly errors?: readonly WidgetRuntimeError[];
}
```

`WidgetConfig` contains render configuration only. Static fragment-backed widget props are resolved
into `props`. Dynamic business payloads remain in `dynamic.widgets[widgetId]` so configuration and
volatile data stay distinct.

The V1 layout vocabulary is limited to:

- rows;
- columns;
- span;
- widgets;
- labelKey.

Unknown widget types are contained: production may omit them or show a neutral fallback; development
may show a diagnostic fallback.

## 6. Backend assembly

`features.pagemodel` introduces a dedicated runtime assembler rather than mutating the internal
definition in place.

```text
resolve effective PageDefinition
  -> authorize surface from TchRequestContext
  -> resolve shell fragments
  -> resolve fragment-backed widget props
  -> execute dynamic providers
  -> map to PageRuntimeResponse
  -> enforce runtime contract guard
```

The runtime contract guard rejects or fails tests when serialized output contains forbidden keys
such as `binding`, `fileKey`, or snake_case contract fields.

Providers remain backend implementation details. They use QueryBus/catalog APIs and do not own
business invariants.

## 7. Frontend rendering

Each page route performs one PageModel runtime call for composition.

```text
Page container
  -> load PageRuntimeResponse
  -> PageShell(shell.type)
  -> PageModel(content, dynamic)
  -> WidgetHost(widgetId)
```

`WidgetHost` reads only:

- `content.widgets[widgetId]`;
- `dynamic.widgets[widgetId]`;
- the matching sanitized widget error.

Theme/bootstrap, i18n, auth/session, and settings/bootstrap remain valid separate runtime concerns.

## 8. Endpoint decision

Runtime routes describe frontend surfaces rather than the internal PageModel mechanism:

```http
GET /api/v1/public/page
GET /api/v1/tenant/dashboard
GET /api/v1/platform/dashboard
```

Rules:

- The public runtime exposes one public page only. The backend resolves `public.home`; the client
  cannot choose a public `logicalId`.
- The tenant dashboard is selected server-side from `TchRequestContext`.
- The platform dashboard is selected server-side for the authenticated platform actor.
- Runtime routes never accept PageModel logical ids as route parameters or allow arbitrary
  private-page selection. The resolved functional identity may still be returned in `meta.logicalId`.
- All three endpoints return `PageRuntimeResponse` inside the existing API envelope.
- Existing `/page-models` runtime routes are removed when Angular switches to the new routes; no
  dual runtime contract is maintained.

## 9. Migration strategy

1. Freeze the target runtime contract with backend serialization fixtures and matching web types.
2. Add the backend assembler and contract guard while keeping the internal definition unchanged.
3. Adapt shell and fragment resolution so runtime output is self-contained.
4. Switch Angular consumers atomically to the new routes and response shape.
5. Remove legacy runtime routes, runtime-only legacy DTOs, and frontend handling for `binding`,
   `fileKey`, snake_case, and
   shell data in dynamic widgets.
6. Update canonical near-code docs and supersede conflicting OpenSpec runtime-shape statements.

The runtime endpoint contract is intentionally breaking before go-live. A dual legacy/new payload
is avoided because it would preserve the complexity this change removes.

## 10. Risks and mitigations

- **Risk: internal definition leaks through generic serialization.** Mitigation: dedicated DTOs,
  assembler tests, and forbidden-key contract tests.
- **Risk: shell or widget fragments fail resolution.** Mitigation: contained sanitized runtime
  errors and focused fragment resolution tests.
- **Risk: frontend/backend drift.** Mitigation: shared JSON fixtures or generated contract samples
  validated in both slices.
- **Risk: dashboard navigation drawer disappears.** Mitigation: explicit private-shell contract and
  acceptance tests for each supported dashboard role.
- **Risk: old OpenSpec changes reintroduce the old envelope.** Mitigation: mark this contract as the
  superseding runtime-shape decision before applying dashboard/provider changes.
