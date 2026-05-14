# Design: Web workspace and portal naming

## Product root vs frontend workspace root

The repository root is a product orchestration boundary. It groups several technical workspaces:

```text
tchalanet-server/   backend Maven/Spring
tchalanet-web/      frontend Nx/Angular
tchalanet-mobile/   Flutter mobile/POS
tchalanet-infra/    docker/infra/deployment
tchalanet-docs/     product and architecture docs
openspec/           change specifications
```

Nx files at the product root blur this boundary. Moving them under `tchalanet-web/` makes it clear that Nx owns only the web frontend workspace.

## Application naming

The main Angular app is named `tchalanet-portal` because it is the primary multi-role web portal.

It may contain:

- public landing and home pages;
- authentication and account flows;
- role-aware shell/navigation;
- cashier/operator pages;
- tenant dashboards;
- tenant admin pages;
- platform admin pages;
- reporting and settings.

The name intentionally avoids:

- `tch-web`: too generic;
- `dashboard`: too narrow;
- `admin`: excludes public/cashier/operator concerns;
- `backoffice`: excludes public/tenant portal concerns.

## Future apps

Do not split prematurely. Future applications may be created only when runtime, deployment, access model or UX constraints justify it.

Possible future apps:

```text
apps/tchalanet-portal      main multi-role portal
apps/tchalanet-kiosk       dedicated kiosk/terminal shell
apps/tchalanet-backoffice  separated admin-only surface, if needed
```

For now, only `tchalanet-portal` is required.

## Library structure

The first-pass library structure should support separation without over-fragmenting:

```text
libs/core                 app-wide Angular services, guards, interceptors, config
libs/shared               small shared utilities/pipes/directives
libs/ui                   design-system and reusable visual components
libs/auth                 auth flows and authorization helpers
libs/i18n                 translation infrastructure
libs/page-model           page model contracts/providers
libs/rendering-engine     dynamic rendering engine
libs/features/*           feature areas and route-level slices
```

Feature libs can remain shallow until the Rule of 3 justifies more folders.

## CI implication

All web/Nx commands should run with:

```yaml
working-directory: tchalanet-web
```

Example commands:

```bash
cd tchalanet-web
pnpm install
pnpm nx lint tchalanet-portal
pnpm nx test tchalanet-portal
pnpm nx build tchalanet-portal
```

## Documentation implication

`VERSIONS.md` must stop pointing to root `package.json` and `pnpm-lock.yaml` for web versions. The source of truth becomes `tchalanet-web/package.json` and `tchalanet-web/pnpm-lock.yaml`.
