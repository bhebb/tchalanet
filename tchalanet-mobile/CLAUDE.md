# Claude — tchalanet-mobile

## OpenSpec local

```text
tchalanet-mobile/openspec/
```

Toutes les changes mobile (Flutter, POS, flows vendeur) vivent ici.

Archiver via :

```bash
cd tchalanet-mobile
openspec archive <change-id> --yes
```

## Périmètre

Ce projet est **autonome**. Ne pas inspecter ni modifier `tchalanet-server`, `apps/tchalanet-web`, `tchalanet-edge-service` sauf demande explicite.

## Vérification contexte (obligatoire avant analyse ou édition)

```bash
pwd
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 3 -type d -name openspec
```

---

Scope:

- Standalone Flutter mobile app.
- Mobile UX for Tchalanet.
- Public views, dashboards, sell process, ticket scan/verify, notifications.

Stack:

- Flutter
- Dart
- Riverpod
- GoRouter
- Material 3

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

Output:

1. Mobile area changed
2. Files inspected
3. Files changed
4. API assumptions
5. Native/plugin/permission impact
6. Validation command
7. Handoff
