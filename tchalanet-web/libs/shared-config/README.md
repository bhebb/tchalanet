# Shared Config

Runtime configuration and feature-availability boundary.

## Owns

- runtime settings contracts, API client, mapping, and store;
- provider-agnostic feature flags;
- feature flag directive and route guard.

This library does not orchestrate auth, i18n, theme, or PageModel bootstrap. The application
composition root owns that cross-cutting orchestration.
