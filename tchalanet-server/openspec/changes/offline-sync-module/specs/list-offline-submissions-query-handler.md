# Spec — ListOfflineSubmissionsQueryHandler

## Responsibility

List offline submissions for admin/review dashboards.

## Rules

- Read-only.
- Returns `TchPage<OfflineSubmissionRow>`.
- Uses paging/sort allowlist.
- Does not expose JPA entities.
- RLS handles tenant scoping unless platform-admin query is explicit.
