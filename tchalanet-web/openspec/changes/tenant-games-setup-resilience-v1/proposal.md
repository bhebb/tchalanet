# Tenant games setup resilience v1

## Why

The games setup page can receive a useful tenant-game list even when an informational setup
source is temporarily unavailable. The page must retain that data and show the degradation locally
instead of losing the response envelope.

## What

- Preserve `ApiResponse` notices for the games setup resource.
- Render targeted degradation notices on the games setup page.
- Keep the existing page-level error path for failures that prevent the required tenant-game list
  from loading.

## Non-goals

- Changing game activation, pricing, or limit business rules.
- Changing the separate limits or draw-sales-matrix pages.
