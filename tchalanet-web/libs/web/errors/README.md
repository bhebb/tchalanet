# Web Errors

Reusable web error normalization and presentation helpers.

## Owns

- frontend-safe error copy resolution;
- `TchErrorViewModel` / `ErrorViewModel`;
- page/section/field ownership helpers;
- server field-error routing for Angular forms;
- API error presenter helpers from `ProblemDetail`, `HttpErrorResponse`, or `WebAppError`.

## Does Not Own

- shell feedback store or banner lifetime;
- reusable stateless UI components (`libs/ui/components`);
- backend contracts and HTTP normalizers (`libs/api`);
- feature API clients or retry decisions.

## Usage

```ts
const presented = presentApiError(err, key => translate.instant(key), {
  source: 'admin.draws',
  surface: 'section',
});

sectionError.set(presented.viewModel);
```

Field validation:

```ts
const fields = fieldErrorsFromApiError(err, 'profile.form');
const remaining = applyServerFieldErrors(form, fields, {
  'profile.email': 'email',
});
```

Rules:

- stable backend `code` translation first;
- category translation second;
- generic safe fallback last;
- never expose backend raw exception messages, SQL/provider details, or stack traces in UI copy;
- every rendered error must have one UI owner: shell, page, section, or field.
