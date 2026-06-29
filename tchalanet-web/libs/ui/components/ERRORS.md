# Error UI Primitives

Reusable error components render presentation only. They do not inspect backend contracts,
`HttpErrorResponse`, `ProblemDetail`, or `ApiNotice`.

Error ownership and backend expectations are documented in:

```text
tchalanet-web/docs/conventions/error-management.md
```

## Components

| Selector | Class | Owner level | Placement | Purpose |
|---|---|---|---|---|
| `tch-error-panel` | `TchErrorPanel` | page/section | top | Framed error panel for blocking data-load or operation failures. |
| `tch-page-error` | `TchPageError` | page | full page | Route/page-level error such as not found or access denied. |
| `tch-section-error` | `TchSectionError` | section | top | Local block/widget/card warning or error. |
| `tch-field-error` | `TchFieldError` | field | inline | Form-control error text, including server errors attached to the control. |

## Rules

- Components receive already-normalized copy or Angular form controls.
- Components must not own API calls, stores, routing decisions, or retry policy.
- Shell feedback is shell-owned (`libs/web/shell`), not part of this UI lib.
- Feature/page code decides which component to render based on error ownership.
- Field errors should be attached to `FormControl.errors.server` by feature code before rendering
  `tch-field-error`.

## Styling

All error primitives follow the shared component conventions:

- class names use `tch-*` BEM-style blocks;
- component-local tokens use `--comp-*`;
- `--comp-*` values fall back to global `--tch-*` theme tokens;
- no hardcoded brand colors except defensive fallbacks where a global token may be missing.

## Examples

Page/section panel:

```html
<tch-error-panel
  [title]="pageErrorTitle"
  [message]="pageErrorMessage"
  [showRetry]="true"
  retryLabel="Reessayer"
  (retry)="load()"
/>
```

Section error:

```html
<tch-section-error
  severity="warn"
  [title]="commissionError.title"
  [message]="commissionError.message"
/>
```

Field error:

```html
<mat-form-field appearance="outline">
  <mat-label>Taux</mat-label>
  <input matInput type="number" formControlName="rate" />
</mat-form-field>
<tch-field-error [control]="form.controls.rate" />
```
