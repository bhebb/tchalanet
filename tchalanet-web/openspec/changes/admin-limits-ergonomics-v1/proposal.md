# admin-limits-ergonomics-v1

## Why

The tenant admin limits screen is difficult to understand for an operator. It exposes dense rule rows and tab navigation inside a section that is already represented in the sidebar.

## What

- Add an `/admin/limits` overview page.
- Remove the inner tab navigation from the limits shell.
- Keep one page per sidebar route: global, seller terminal, draw channel, number.
- Present active assignments as business-readable cards instead of a dense table.
- Keep backend/BFF changes out of this first slice unless existing endpoints are insufficient.

## Impact

- Admin users get a clearer entry point for sales limits.
- Existing assignment endpoints remain in use.
- Sidenav remains the primary navigation surface.

## Non-goals

- No new backend overview endpoint in this slice.
- No full rewrite of the limit assignment dialog.
- No dynamic number exposure implementation.
