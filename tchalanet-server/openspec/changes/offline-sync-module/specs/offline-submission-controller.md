# Spec — OfflineSubmissionController

## Package

`core.offlinesync.internal.infra.web`

## Endpoints

- `POST /tenant/offline-sales/submissions` — submit one or multiple offline sale payloads.
- `GET /admin/offline-sales/submissions` — list submissions for review/admin.
- `GET /admin/offline-sales/submissions/{id}` — details.
- `POST /admin/offline-sales/submissions/{id}/reject` — manual reject.
- `POST /admin/offline-sales/submissions/{id}/promote` — manual promotion if allowed.

## Rules

- Controller is thin.
- Controller maps request to command/query.
- Controller does not call repositories.
- Controller does not run sales validation directly.
- Controller uses `@CurrentContext TchRequestContext`.
