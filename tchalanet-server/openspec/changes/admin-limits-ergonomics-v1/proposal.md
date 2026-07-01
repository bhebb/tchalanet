# Change: admin-limits-ergonomics-v1

## Why

The tenant admin limits overview needs a backend-owned business projection. The web app should not assemble the overview by calling rule catalog and assignment endpoints directly.

## What Changes

- Add a tenantadmin policies overview endpoint.
- Keep PageModel responsible for shell/navigation, not interactive limits data.
- Keep detailed limits mutation/read endpoints in the existing limit policy API.

## Impact

- New endpoint: `GET /admin/policies/overview`.
- Web overview can consume one stable BFF contract.
