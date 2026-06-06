# Shared Config

Runtime configuration and feature-availability boundary.

## Owns

- runtime settings contracts, API client, mapping, and store;
- stable runtime API paths and bootstrap configuration values;
- provider-agnostic feature flags;
- feature flag directive and route guard.

This library does not orchestrate auth, i18n, theme, or PageModel bootstrap. It owns their stable
configuration values; the application composition root owns the orchestration.
