# Refonte Super Admin Platform Sidenav

## Why

The Super Admin platform navigation is currently a flat technical list. It should reflect operational responsibilities: overview, tenants, referentials, operations, access, communication, and reports.

## What

- Return grouped Super Admin platform navigation from the private PageModel shell fragments.
- Keep the existing backend/frontend navigation contract and use its existing `children` support.
- Update the Angular fallback navigation, routes, translations, and sidebar rendering for grouped items, badges, and disabled states.
- Add clean placeholders for target routes that do not yet have a dedicated page.

## Impact

- Backend: PageModel JSON fragments for Super Admin private shell/sidebar.
- Frontend: private shell navigation fallback, sidebar UI component, platform routes, i18n bundles.

## Non-goals

- Dynamic badge counts.
- Fine-grained permission filtering beyond preserving contract fields.
- New business pages for every placeholder route.
