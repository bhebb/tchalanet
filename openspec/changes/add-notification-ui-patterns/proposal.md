# Change: Add Notification UI Patterns

## Summary

Define how the frontend should present feedback, notices, alerts, and persisted notifications across Tchalanet.

This change clarifies the distinction between:

- inline form errors,
- API notices,
- toast/snackbar feedback,
- notification drawer items,
- notification center pages,
- critical banners,
- PageModel notification summary.

## Motivation

Tchalanet will have many alert types: operational warnings, template update reviews, session alerts, terminal sync issues, payout decisions, draw/result errors, and provider outages. Without a clear UI taxonomy, every feature may invent its own feedback pattern.

The frontend needs stable rules so errors and notifications are displayed consistently across public, tenant, admin, and platform surfaces.

## Goals

- Define which feedback is persisted and which is transient.
- Define which feedback belongs in `ApiResponse.notices`.
- Define which feedback belongs in `core.notification`.
- Define how PageModel includes only notification summary, not full notification lists.
- Define frontend display patterns for severity/kind.
- Support mobile-first layouts and Tchalanet theming/i18n conventions.

## Non-Goals

- Do not implement frontend components in this change.
- Do not change the PageModel renderer architecture.
- Do not persist form validation errors.
- Do not replace backend `ProblemDetail` errors.

## UI Pattern Taxonomy

### Inline form errors

Used for field-specific validation and submission errors.

- Not persisted.
- Not sent through notification center.
- Usually derived from form validation or `ProblemDetail` field errors.

### API notices

Used for non-blocking notices in successful API responses.

- Not persisted by default.
- Comes from `ApiResponse.notices`.
- Examples: partial service degradation, warning about limits, safe fallback used.

### Toast/snackbar

Used for short-lived confirmation or transient status.

- Not persisted.
- UI-only or derived from successful action result.
- Examples: “Saved”, “Marked as read”, “Draft created”.

### Notification drawer

Used for persisted user/role actionable or operational notifications.

- Backed by `core.notification`.
- Paginated via notification API.
- Can contain action links.

### Notification center

Full page for notification history and filters.

- Backed by `core.notification`.
- Supports status/category/severity filters.

### Critical banner

Used for high-priority persisted notifications or major service status.

- Backed by `core.notification` summary or API service status.
- Severity usually `CRITICAL`.

### PageModel notification summary

The PageModel may include a lightweight summary only:

```json
{
  "notifications": {
    "unread_count": 3,
    "critical_count": 1,
    "action_required_count": 2,
    "has_action_required": true
  }
}
```

The PageModel must not embed a full list of notifications.

## Risks

- Persisting too much transient UI feedback creates noise.
- Loading full notifications inside PageModel makes PageModel less cacheable and user-specific.
- Overusing critical banners reduces trust.

## Rollout

1. Define backend contracts for summary and list endpoints.
2. Update PageModel providers to include summary only where appropriate.
3. Build frontend components progressively:
   - badge summary,
   - drawer,
   - center page,
   - critical banner.
4. Migrate ad-hoc alerts to the taxonomy.
