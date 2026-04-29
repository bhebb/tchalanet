# Tasks: Add Notification UI Patterns

## 1. Contracts

- [x] Define notification summary response model.
- [x] Define notification list item response model.
- [x] Define notification action model.
- [x] Define mapping from severity/kind to frontend presentation.
- [x] Define i18n key namespaces for notifications.

## 2. PageModel integration

- [x] Add optional `notifications` summary block to private/admin PageModel payloads.
- [x] Do not embed full notification lists in PageModel.
- [x] Ensure PageModel cache policy does not cache user-specific summary incorrectly.
- [x] If summary is user-specific, resolve it after cache or mark the block as dynamic.

## 3. Frontend display rules

- [x] Define inline form error behavior.
- [x] Define `ApiNotice` rendering behavior.
- [x] Define toast/snackbar behavior.
- [x] Define drawer behavior.
- [x] Define notification center behavior.
- [x] Define critical banner behavior.

## 4. Components later

- [ ] Header badge component.
- [ ] Notification drawer component.
- [ ] Notification center page.
- [ ] Critical banner component.
- [ ] Toast adapter.
- [ ] ApiNotice renderer.

## 5. Accessibility and i18n

- [x] Ensure severities are communicated beyond color.
- [x] Use ARIA live regions for toasts and critical banners.
- [x] Use i18n keys in snake_case with functional namespaces.
- [x] Respect mobile-first breakpoints and theme tokens.
