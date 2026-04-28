# Tasks: Add Notification UI Patterns

## 1. Contracts

- [ ] Define notification summary response model.
- [ ] Define notification list item response model.
- [ ] Define notification action model.
- [ ] Define mapping from severity/kind to frontend presentation.
- [ ] Define i18n key namespaces for notifications.

## 2. PageModel integration

- [ ] Add optional `notifications` summary block to private/admin PageModel payloads.
- [ ] Do not embed full notification lists in PageModel.
- [ ] Ensure PageModel cache policy does not cache user-specific summary incorrectly.
- [ ] If summary is user-specific, resolve it after cache or mark the block as dynamic.

## 3. Frontend display rules

- [ ] Define inline form error behavior.
- [ ] Define `ApiNotice` rendering behavior.
- [ ] Define toast/snackbar behavior.
- [ ] Define drawer behavior.
- [ ] Define notification center behavior.
- [ ] Define critical banner behavior.

## 4. Components later

- [ ] Header badge component.
- [ ] Notification drawer component.
- [ ] Notification center page.
- [ ] Critical banner component.
- [ ] Toast adapter.
- [ ] ApiNotice renderer.

## 5. Accessibility and i18n

- [ ] Ensure severities are communicated beyond color.
- [ ] Use ARIA live regions for toasts and critical banners.
- [ ] Use i18n keys in snake_case with functional namespaces.
- [ ] Respect mobile-first breakpoints and theme tokens.
