# Claude — tchalanet-mobile

Scope:

- Standalone Flutter mobile app.
- Mobile UX for Tchalanet.
- Public views, dashboards, sell process, ticket scan/verify, notifications.

Stack:

- Flutter
- Dart
- Material 3 if applicable

Rules:

- Mobile is outside the web Nx workspace.
- Do not import or depend on `tchalanet-web`.
- Do not invent backend endpoints.
- Backend remains source of truth.
- Reuse API contracts through generated clients or explicit Dart models.
- Optimize for touch, small screens, slow networks, and offline-aware flows.
- Keep platform-specific code isolated.
- Do not add native plugins without explicit approval.
- Explain Android/iOS permission changes.
- Do not store sensitive ticket/user data unencrypted.
- No hardcoded colors; use theme tokens/design system.
- i18n keys use snake_case functional namespaces.
- Public ticket verification must mask sensitive data.
- Sell flow must use backend validation and idempotency where applicable.

Possible mobile surfaces:

- publichome-style read-only public views
- dashboard summaries
- sellprocess/POS flow
- ticket scan/verify
- notifications

Boundary:

- Mobile can share API contracts.
- Mobile must not depend directly on web implementation.
- Mobile-specific UX can differ from web.

Output:

1. Mobile area changed
2. Files inspected
3. Files changed
4. API assumptions
5. Native/plugin/permission impact
6. Validation command
7. Handoff
