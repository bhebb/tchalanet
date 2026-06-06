# Design

## Extraction order

1. `api`: common transport contracts and HTTP infrastructure.
2. `shared-config`: settings runtime and feature flags.
3. `page-model`: PageModel contract, API, renderer, widget host contract.
4. `widgets`: registry and concrete props-driven widgets.
5. `web`: routes, shells, pages, containers, and role dashboards.

## Ownership

- `page-model` owns PageModel loading/rendering and the abstract widget-host boundary. It contains no
  concrete widgets and no shell.
- `widgets` owns concrete dynamic widgets and registry providers. It may depend on `page-model` and
  `ui/components`.
- Dashboards are routed pages/containers. They belong to `web/{cashier,tenant-admin,platform-admin}`,
  not to `page-model` or `widgets`.
- Public/private shells and routed public pages belong to `web`.

## Dependency direction

```text
api <- shared-config
api <- page-model <- widgets
api/shared-config/page-model/widgets/ui <- web <- app composition root
```
