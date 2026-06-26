# UI Components

Reusable Angular UI components shared by Web shells and pages.

## Responsibilities

- render reusable UI behavior without owning PageModel or shell runtime resolution;
- consume navigation and action contracts from this library;
- consume global theme tokens under `--tch-*`;
- expose component-local customization under `--comp-*` with `--tch-*` fallbacks.

Runtime theme selection and DOM token application belong to `libs/ui/theme`. Shared SCSS primitives
and global Material overrides belong to `libs/ui/styles`.

The public API is exported from `src/index.ts`.

## Component inventory

| Selector | Class | Purpose |
|---|---|---|
| `tch-brand` | `TchBrand` | Logo + name link; accepts `ActionItem` or plain span |
| `tch-nav` | `TchNav` | Horizontal nav link list |
| `tch-overlay-nav` | `TchOverlayNav` | Mobile burger panel / overlay nav |
| `tch-sidebar-nav` | `TchSidebarNav` | Private shell sidebar nav |
| `tch-card` | `TchCard` | Surface card with `--comp-card-*` tokens |
| `tch-status-badge` | `TchStatusBadge` | Status badge chip |
| `tch-notice` | `TchNotice` | Inline notice / alert banner |
| `tch-empty-state` | `TchEmptyState` | Empty state with icon + message |
| `tch-error-panel` | `TchErrorPanel` | Error panel for data-load failures |
| `tch-page-error` | `TchPageError` | Full-page error (404, 500, etc.) |
| `tch-loading` | `TchLoading` | Loading spinner |
| `tch-admin-list-surface` | `AdminListSurface` | Shared list/table surface with debounced search, collapsible filters and slots |
| `tch-search-select` | `TchSearchSelect` | Single server-backed autocomplete selector |
| `tch-multi-search-select` | `TchMultiSearchSelect` | Multi server-backed autocomplete selector with chips |
| `tch-section-header` | `TchSectionHeader` | Section title + optional action |
| `tch-field-error` | `TchFieldError` | Form field error message |
| `tch-lang-switcher` | `TchLangSwitcher` | Language selector |
| `tch-lang-theme-group` | `TchLangThemeGroup` | Combined lang + theme controls |
| *(service)* | `TchBreakpointService` | Responsive tier signals (`handset/tablet/desktop`) |
| *(type)* | `ActionItem` | Navigation action contract (route or external) |

## Authoring pattern

Every component in this library:

- is a standalone Angular component with `ChangeDetectionStrategy.OnPush`;
- uses `input()` / `output()` signals, never `@Input`/`@Output`;
- declares `:host` with `--comp-<block>-*` variables falling back to `--tch-*`;
- uses BEM-like class names prefixed with `tch-` for generic blocks;
- has no dependency on feature stores, facades, auth, or PageModel;
- has no import from `libs/ui/theme` (tokens are consumed via CSS vars, not the store).

```ts
@Component({
  selector: 'tch-example',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="tch-example">{{ label() }}</div>`,
  styles: [`
    :host { --comp-example-fg: var(--tch-color-on-surface); }
    .tch-example { color: var(--comp-example-fg); }
  `],
})
export class TchExample {
  readonly label = input<string>('');
}
```

## Gaps (pending S2)

The following primitives are not yet in this library and are needed for public pages:
`TchButton`, `TchBadge`, `TchStatusChip`, `TchSkeleton`, `TchResultNumbers`, `TchDivider`.
See `openspec/changes/public-design-system-pagemodel-widgets/tasks.md` § Prérequis Design System.
